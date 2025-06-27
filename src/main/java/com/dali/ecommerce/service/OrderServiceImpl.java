package com.dali.ecommerce.service;

import com.dali.ecommerce.model.*;
import com.dali.ecommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        // This method is now for COD or other immediate processing orders
        Order order = createBaseOrder(username, checkoutDetails);
        order.setStatus(OrderStatus.PROCESSING);
        Order savedOrder = orderRepository.save(order);

        createOrderHistoryEvent(savedOrder, OrderStatus.PROCESSING, "Order placed successfully (COD).");

        processOrderItemsAndStock(savedOrder, username);

        return savedOrder;
    }

    @Override
    @Transactional
    public Order createPendingOrder(String username, Map<String, Object> checkoutDetails) {
        // This method creates an order that awaits payment confirmation
        Order order = createBaseOrder(username, checkoutDetails);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        Order savedOrder = orderRepository.save(order);

        createOrderHistoryEvent(savedOrder, OrderStatus.PENDING_PAYMENT, "Awaiting payment from gateway.");

        // We do NOT process items or clear the cart yet. That happens upon webhook confirmation.
        // We do need to save the order items to the order, however.
        Account account = accountRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("User not found"));
        List<CartItem> cartItems = cartItemRepository.findByAccountAccountId(account.getAccountId());
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
    public void processSuccessfulPayment(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        // Ensure we only process it once
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
            createOrderHistoryEvent(order, OrderStatus.PROCESSING, "Payment confirmed via Maya. Order is now being processed.");

            // Now, process the stock and clear the user's cart
            processOrderItemsAndStock(order, order.getAccount().getEmail());
        }
    }

    @Override
    @Transactional
    public void failOrderPayment(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            createOrderHistoryEvent(order, OrderStatus.CANCELLED, "Payment failed or was cancelled by the user.");
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
        if (shippingFeeNumber == null) {
            throw new IllegalStateException("Shipping fee is missing from checkout details.");
        }
        double shippingFee = shippingFeeNumber.doubleValue();

        Order order = new Order();
        order.setAccount(account);
        order.setAddress(address);
        order.setDeliveryMethod((String) checkoutDetails.get("deliveryMethod"));
        order.setPaymentMethod((String) checkoutDetails.get("paymentMethod"));
        order.setTotalPrice(subtotal + shippingFee);

        return order;
    }

    private void processOrderItemsAndStock(Order order, String username) {
        Account account = accountRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<CartItem> cartItems = cartItemRepository.findByAccountAccountId(account.getAccountId());
        if (cartItems.isEmpty() && (order.getOrderItems() == null || order.getOrderItems().isEmpty())) {
            throw new IllegalStateException("Cannot process an order with an empty cart.");
        }

        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            // This case handles COD where order items aren't set in the base method
            orderItems = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItems.add(orderItem);
            }
            orderItemRepository.saveAll(orderItems);
            order.setOrderItems(orderItems);
        }

        // Decrement stock
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            int newQuantity = product.getProductQuantity() - orderItem.getQuantity();
            if (newQuantity < 0) {
                throw new IllegalStateException("Not enough stock for product: " + product.getName());
            }
            product.setProductQuantity(newQuantity);
            productRepository.save(product);
        }

        // Clear cart
        cartItemRepository.deleteByAccountAccountId(account.getAccountId());
    }

    @Override
    public Order findOrderById(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }

    @Override
    @Transactional
    public void updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = findOrderById(orderId);

        // Prevent updating from a terminal state or pending state
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Cannot update order from its current state: " + order.getStatus());
        }

        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            restoreStockForOrder(order);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        String notes = "Order status updated to '" + newStatus.name() + "' by DALI Admin.";
        createOrderHistoryEvent(order, newStatus, notes);
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId, String username) {
        Order order = findOrderById(orderId);

        if (!order.getAccount().getEmail().equals(username)) {
            throw new SecurityException("User does not have permission to cancel this order.");
        }

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Order cannot be cancelled as it is already being fulfilled or is in a final state.");
        }

        restoreStockForOrder(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        createOrderHistoryEvent(order, OrderStatus.CANCELLED, "Order cancelled by customer.");
    }

    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int newQuantity = product.getProductQuantity() + item.getQuantity();
            product.setProductQuantity(newQuantity);
            productRepository.save(product);
        }
    }

    private void createOrderHistoryEvent(Order order, OrderStatus status, String notes) {
        OrderHistory historyEvent = new OrderHistory();
        historyEvent.setOrder(order);
        historyEvent.setStatus(status);
        historyEvent.setNotes(notes);
        historyEvent.setEventTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(historyEvent);
    }
}