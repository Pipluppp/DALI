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
    private final ProductRepository productRepository;
    private final OrderHistoryRepository orderHistoryRepository;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository, AccountRepository accountRepository, AddressRepository addressRepository, CartItemRepository cartItemRepository, ProductRepository productRepository, OrderHistoryRepository orderHistoryRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.accountRepository = accountRepository;
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderHistoryRepository = orderHistoryRepository;
    }

    @Override
    @Transactional
    public Order createOrder(String username, Map<String, Object> checkoutDetails) {
        Order order = createBaseOrder(username, checkoutDetails);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingStatus(ShippingStatus.PROCESSING);
        Order savedOrder = orderRepository.save(order);
        createOrderHistoryEvent(savedOrder, ShippingStatus.PROCESSING, "Order placed successfully (COD). Awaiting delivery and payment.");
        processStockForPaidOrder(savedOrder.getOrderId());
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
                // By calling a public @Transactional method on the same bean, Spring creates a new transaction.
                this.processStockForPaidOrder(orderId);
            } catch (Exception e) {
                // If stock processing fails, the payment is still recorded as PAID.
                // We now log this critical failure and add a history note for the admin.
                System.err.println("CRITICAL: Payment for order " + orderId + " was successful, but stock processing failed: " + e.getMessage());
                Order order = findOrderById(orderId);
                createOrderHistoryEvent(order, order.getShippingStatus(), "FULFILLMENT FAILED: Not enough stock. Admin review required.");
            }
        }
    }

    @Transactional
    protected boolean recordPayment(Integer orderId, String mayaCheckoutId) {
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

        if (!order.getOrderHistory().isEmpty()) {
            OrderHistory lastEvent = order.getOrderHistory().get(0);
            if (lastEvent.getNotes().contains("Awaiting payment")) {
                lastEvent.setNotes("Payment confirmed via Maya. Order is now being processed.");
                orderHistoryRepository.save(lastEvent);
            }
        }

        orderRepository.save(order); // Commits this small transaction.
        System.out.println("Webhook: Payment for order " + orderId + " successfully recorded as PAID.");
        return true;
    }

    @Override
    @Transactional
    public void processStockForPaidOrder(Integer orderId) {
        Order order = findOrderById(orderId);
        Account account = order.getAccount();

        System.out.println("Fulfillment: Starting stock and cart processing for order " + orderId);

        // Decrement stock
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            int newQuantity = product.getProductQuantity() - orderItem.getQuantity();
            if (newQuantity < 0) {
                throw new IllegalStateException("Not enough stock for product ID " + product.getId() + " (" + product.getName() + ")");
            }
            product.setProductQuantity(newQuantity);
            productRepository.save(product);
        }

        // Clear cart
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
        if (order.getShippingStatus() == ShippingStatus.DELIVERED || order.getShippingStatus() == ShippingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update order from its current terminal state: " + order.getShippingStatus());
        }
        if (newStatus == ShippingStatus.CANCELLED && order.getShippingStatus() != ShippingStatus.CANCELLED) {
            if(order.getPaymentStatus() == PaymentStatus.PAID || "Cash on delivery (COD)".equals(order.getPaymentMethod())) {
                restoreStockForOrder(order);
            }
        }
        order.setShippingStatus(newStatus);
        createOrderHistoryEvent(order, newStatus, "Order status updated to '" + newStatus.name() + "' by DALI Admin.");
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
        order.setShippingStatus(ShippingStatus.CANCELLED);
        createOrderHistoryEvent(order, ShippingStatus.CANCELLED, "Order cancelled by customer.");
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