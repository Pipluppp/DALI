package com.dali.ecommerce.order;

import com.dali.ecommerce.account.Account;
import com.dali.ecommerce.account.AccountRepository;
import com.dali.ecommerce.address.Address;
import com.dali.ecommerce.address.AddressRepository;
import com.dali.ecommerce.cart.CartItem;
import com.dali.ecommerce.cart.CartItemRepository;
import com.dali.ecommerce.product.Product;
import com.dali.ecommerce.product.ProductRepository;
import com.dali.ecommerce.store.Store;
import com.dali.ecommerce.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AccountRepository accountRepository;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderPickupRepository orderPickupRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, AccountRepository accountRepository, AddressRepository addressRepository, CartItemRepository cartItemRepository, StoreRepository storeRepository, ProductRepository productRepository, OrderHistoryRepository orderHistoryRepository, OrderPickupRepository orderPickupRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.accountRepository = accountRepository;
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.orderHistoryRepository = orderHistoryRepository;
        this.orderPickupRepository = orderPickupRepository;
    }

    @Transactional
    public Order createOrder(String username, Map<String, Object> checkoutDetails) {
        Order order = createBaseOrder(username, checkoutDetails);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingStatus(ShippingStatus.PROCESSING);

        Order savedOrder = orderRepository.save(order);

        List<CartItem> cartItems = cartItemRepository.findByAccountAccountId(savedOrder.getAccount().getAccountId());
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setOrderItems(orderItems);

        createOrderHistoryEvent(savedOrder, ShippingStatus.PROCESSING, "Order placed successfully (COD). Awaiting delivery and payment.");
        processStockForPaidOrder(savedOrder.getOrderId());
        createOrderPickupDetails(savedOrder, checkoutDetails);
        return savedOrder;
    }

    @Transactional
    public Order createPendingOrder(String username, Map<String, Object> checkoutDetails) {
        Order order = createBaseOrder(username, checkoutDetails);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingStatus(ShippingStatus.PROCESSING);
        Order savedOrder = orderRepository.save(order);
        createOrderHistoryEvent(savedOrder, ShippingStatus.PROCESSING, "Order created. Awaiting payment from gateway.");
        List<CartItem> cartItems = cartItemRepository.findByAccountAccountId(savedOrder.getAccount().getAccountId());
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setOrderItems(orderItems);
        createOrderPickupDetails(savedOrder, checkoutDetails);
        return savedOrder;
    }

    @Transactional
    public void setPaymentTransactionId(Integer orderId, String transactionId) {
        Order order = findOrderById(orderId);
        order.setPaymentTransactionId(transactionId);
        orderRepository.save(order);
    }

    @Transactional
    public void confirmPaymentOnSuccessRedirect(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            System.out.println("Payment for order " + orderId + " is already marked as PAID. Skipping confirmation.");
            return;
        }

        order.setPaymentStatus(PaymentStatus.PAID);

        OrderHistory lastEvent = order.getOrderHistory().stream().findFirst().orElse(null);
        if (lastEvent != null && lastEvent.getNotes().contains("Awaiting payment")) {
            lastEvent.setNotes("Payment confirmed via user redirect. Order is now being processed.");
            orderHistoryRepository.save(lastEvent);
        } else {
            createOrderHistoryEvent(order, order.getShippingStatus(), "Payment confirmed via user redirect.");
        }

        orderRepository.save(order);
        System.out.println("Payment for order " + orderId + " successfully confirmed via redirect.");

        try {
            this.processStockForPaidOrder(orderId);
        } catch (Exception e) {
            System.err.println("CRITICAL: Payment for order " + orderId + " was confirmed, but stock processing failed: " + e.getMessage());
            createOrderHistoryEvent(order, order.getShippingStatus(), "FULFILLMENT FAILED: Not enough stock. Admin review required.");
            throw e; // Re-throw to let the controller handle the redirect
        }
    }

    @Transactional
    public void processStockForPaidOrder(Integer orderId) {
        Order order = findOrderById(orderId);
        Account account = order.getAccount();

        System.out.println("Fulfillment: Starting stock and cart processing for order " + orderId);

        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            int newQuantity = product.getProductQuantity() - orderItem.getQuantity();
            if (newQuantity < 0) {
                throw new IllegalStateException("Not enough stock for product ID " + product.getId() + " (" + product.getName() + ")");
            }
            product.setProductQuantity(newQuantity);
            productRepository.save(product);
        }

        cartItemRepository.deleteByAccountAccountId(account.getAccountId());
        System.out.println("Fulfillment: Stock and cart processing completed for order " + orderId);
    }

    @Transactional
    public void failOrderPayment(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        if (order.getShippingStatus() != ShippingStatus.CANCELLED) {
            order.setShippingStatus(ShippingStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.CANCELLED); // Set payment status to cancelled
            createOrderHistoryEvent(order, ShippingStatus.CANCELLED, "Payment failed or was cancelled by the user.");
            orderRepository.save(order);
        }
    }

    private Order createBaseOrder(String username, Map<String, Object> checkoutDetails) {
        Account account = accountRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Integer addressId = (Integer) checkoutDetails.get("addressId");
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        double subtotal = cartItemRepository.findByAccountAccountId(account.getAccountId()).stream()
                .mapToDouble(CartItem::getSubtotal).sum();
        Number shippingFeeNumber = (Number) checkoutDetails.get("shippingFee");
        double shippingFee = shippingFeeNumber.doubleValue();
        Order order = new Order();
        order.setAccount(account);
        order.setAddress(address);
        order.setDeliveryMethod((String) checkoutDetails.get("deliveryMethod"));
        order.setPaymentMethod((String) checkoutDetails.get("paymentMethod"));
        order.setTotalPrice(subtotal + shippingFee);
        return order;
    }

    private void createOrderPickupDetails(Order savedOrder, Map<String, Object> checkoutDetails) {
        if ("Pickup Delivery".equals(savedOrder.getDeliveryMethod())) {
            Integer storeId = (Integer) checkoutDetails.get("storeId");
            if (storeId == null) {
                throw new IllegalStateException("Store ID is required for Pickup Delivery but was not found in checkout details.");
            }
            Store pickupStore = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("Selected pickup store with ID " + storeId + " not found."));

            OrderPickup orderPickup = new OrderPickup();
            orderPickup.setOrder(savedOrder);
            orderPickup.setStore(pickupStore);
            orderPickupRepository.save(orderPickup);

            savedOrder.setOrderPickup(orderPickup);
        }
    }

    public Order findOrderById(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }

    @Transactional
    public void updateShippingStatus(Integer orderId, ShippingStatus newStatus) {
        Order order = findOrderById(orderId);
        if (order.getPaymentStatus() == PaymentStatus.PENDING && !"Cash on delivery (COD)".equals(order.getPaymentMethod())) {
            throw new IllegalStateException("Cannot update shipping for an order with PENDING online payment.");
        }

        if (order.getShippingStatus() == ShippingStatus.COLLECTED || order.getShippingStatus() == ShippingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update order from its current terminal state: " + order.getShippingStatus());
        }

        if (newStatus == ShippingStatus.CANCELLED && order.getShippingStatus() != ShippingStatus.CANCELLED) {
            if(order.getPaymentStatus() == PaymentStatus.PAID || "Cash on delivery (COD)".equals(order.getPaymentMethod())) {
                restoreStockForOrder(order);
            }
            if (order.getPaymentStatus() == PaymentStatus.PENDING) {
                order.setPaymentStatus(PaymentStatus.CANCELLED);
            }
        }
        order.setShippingStatus(newStatus);

        String historyNotes;
        if (newStatus == ShippingStatus.CANCELLED) {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                historyNotes = "Order cancelled by DALI Admin. Customer has paid, refund is required.";
            } else {
                historyNotes = "Order cancelled by DALI Admin.";
            }
        } else if (newStatus == ShippingStatus.COLLECTED) {
            historyNotes = "Order collected by customer from " + order.getOrderPickup().getStore().getName() + ".";
        } else if (newStatus == ShippingStatus.DELIVERED && "Pickup Delivery".equals(order.getDeliveryMethod())) {
            historyNotes = "Order has arrived at " + order.getOrderPickup().getStore().getName() + " and is ready for customer pickup.";
        }
        else {
            historyNotes = "Order status updated to '" + newStatus.name() + "' by DALI Admin.";
        }
        createOrderHistoryEvent(order, newStatus, historyNotes);

        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Integer orderId, String username) {
        Order order = findOrderById(orderId);
        if (!order.getAccount().getEmail().equals(username)) {
            throw new SecurityException("User does not have permission to cancel this order.");
        }
        if (order.getShippingStatus() != ShippingStatus.PROCESSING) {
            throw new IllegalStateException("Order cannot be cancelled as it is already being fulfilled.");
        }
        if(order.getPaymentStatus() == PaymentStatus.PAID || "Cash on delivery (COD)".equals(order.getPaymentMethod())) {
            restoreStockForOrder(order);
        }
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }
        order.setShippingStatus(ShippingStatus.CANCELLED);

        String historyNotes;
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            historyNotes = "Order cancelled by customer. Refund is required.";
        } else {
            historyNotes = "Order cancelled by customer.";
        }
        createOrderHistoryEvent(order, ShippingStatus.CANCELLED, historyNotes);

        orderRepository.save(order);
    }

    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setProductQuantity(product.getProductQuantity() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private void createOrderHistoryEvent(Order order, ShippingStatus status, String notes) {
        OrderHistory historyEvent = new OrderHistory();
        historyEvent.setOrder(order);
        historyEvent.setStatus(status);
        historyEvent.setNotes(notes);
        historyEvent.setEventTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(historyEvent);
    }
}