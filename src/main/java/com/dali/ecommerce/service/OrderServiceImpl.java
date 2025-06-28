package com.dali.ecommerce.service;

import com.dali.ecommerce.model.*;
import com.dali.ecommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AccountRepository accountRepository;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderPickupRepository orderPickupRepository;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository, AccountRepository accountRepository, AddressRepository addressRepository, CartItemRepository cartItemRepository, StoreRepository storeRepository, ProductRepository productRepository, OrderHistoryRepository orderHistoryRepository, OrderPickupRepository orderPickupRepository) {
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

    @Override
    @Transactional
    public Order createOrder(String username, Map<String, Object> checkoutDetails) {
        Order order = createBaseOrder(username, checkoutDetails);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingStatus(ShippingStatus.PROCESSING);

        Order savedOrder = orderRepository.save(order);

        // Create OrderItems from the cart, same as in createPendingOrder
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
        savedOrder.setOrderItems(orderItems); // Associate the items with the saved order object

        createOrderHistoryEvent(savedOrder, ShippingStatus.PROCESSING, "Order placed successfully (COD). Awaiting delivery and payment.");
        processStockForPaidOrder(savedOrder.getOrderId());
        // Handle pickup details if necessary, though this path is mainly for delivery
        createOrderPickupDetails(savedOrder, checkoutDetails);
        return savedOrder;
    }

    @Override
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

    @Override
    @Transactional
    public void setPaymentTransactionId(Integer orderId, String transactionId) {
        Order order = findOrderById(orderId);
        order.setPaymentTransactionId(transactionId);
        orderRepository.save(order);
    }

    @Override
    public void processSuccessfulPayment(Integer orderId, String mayaCheckoutId) {
        boolean paymentRecorded = recordPayment(orderId, mayaCheckoutId);

        if (paymentRecorded) {
            try {
                this.processStockForPaidOrder(orderId);
            } catch (Exception e) {
                System.err.println("CRITICAL: Payment for order " + orderId + " was successful, but stock processing failed: " + e.getMessage());
                Order order = findOrderById(orderId);
                createOrderHistoryEvent(order, order.getShippingStatus(), "FULFILLMENT FAILED: Not enough stock. Admin review required.");
            }
        }
    }

    @Override
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
    protected boolean recordPayment(Integer orderId, String mayaCheckoutId) {
        if (mayaCheckoutId == null || mayaCheckoutId.trim().isEmpty()) {
            System.out.println("Webhook for order " + orderId + " skipped due to missing transaction ID.");
            return false;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            System.out.println("Webhook: Order " + orderId + " is already PAID. Ignoring webhook.");
            return false;
        }

        if (order.getPaymentTransactionId() != null && !Objects.equals(order.getPaymentTransactionId(), mayaCheckoutId)) {
            System.err.println("Webhook: Mismatched transaction ID for order " + orderId + ". Stored: " + order.getPaymentTransactionId() + ", Received: " + mayaCheckoutId);
            return false;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentTransactionId(mayaCheckoutId);

        OrderHistory lastEvent = order.getOrderHistory().stream().findFirst().orElse(null);
        if (lastEvent != null && lastEvent.getNotes().contains("Awaiting payment")) {
            lastEvent.setNotes("Payment confirmed via Maya Webhook. Order is now being processed.");
            orderHistoryRepository.save(lastEvent);
        }

        orderRepository.save(order);
        System.out.println("Webhook: Payment for order " + orderId + " successfully recorded as PAID.");
        return true;
    }

    @Override
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

    @Override
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
    @Override
    public Order findOrderById(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }

    @Override
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
            // If the payment was pending, also mark it as cancelled.
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

    @Override
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
        // If the payment was pending, also mark it as cancelled.
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