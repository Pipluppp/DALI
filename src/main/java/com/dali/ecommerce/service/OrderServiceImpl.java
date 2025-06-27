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
    private final OrderHistoryRepository orderHistoryRepository; // Inject new repository

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository, AccountRepository accountRepository, AddressRepository addressRepository, CartItemRepository cartItemRepository, ProductRepository productRepository, OrderHistoryRepository orderHistoryRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.accountRepository = accountRepository;
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderHistoryRepository = orderHistoryRepository; // Add to constructor
    }

    @Override
    @Transactional
    public Order createOrder(String username, Map<String, Object> checkoutDetails) {
        Account account = accountRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<CartItem> cartItems = cartItemRepository.findByAccountAccountId(account.getAccountId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create an order with an empty cart.");
        }

        Integer addressId = (Integer) checkoutDetails.get("addressId");
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        Order order = new Order();
        order.setAccount(account);
        order.setAddress(address);
        order.setDeliveryMethod((String) checkoutDetails.get("deliveryMethod"));
        order.setPaymentMethod((String) checkoutDetails.get("paymentMethod"));
        order.setStatus(OrderStatus.PROCESSING);

        double subtotal = cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
        Number shippingFeeNumber = (Number) checkoutDetails.get("shippingFee");
        if (shippingFeeNumber == null) {
            throw new IllegalStateException("Shipping fee is missing from checkout details.");
        }
        double shippingFee = shippingFeeNumber.doubleValue();

        order.setTotalPrice(subtotal + shippingFee);

        Order savedOrder = orderRepository.save(order);

        // Create initial history event
        createOrderHistoryEvent(savedOrder, OrderStatus.PROCESSING, "Order placed by customer.");

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItems.add(orderItem);

            Product product = cartItem.getProduct();
            int newQuantity = product.getProductQuantity() - cartItem.getQuantity();
            if (newQuantity < 0) {
                throw new IllegalStateException("Not enough stock for product: " + product.getName());
            }
            product.setProductQuantity(newQuantity);
            productRepository.save(product);
        }

        orderItemRepository.saveAll(orderItems);
        savedOrder.setOrderItems(orderItems);

        cartItemRepository.deleteByAccountAccountId(account.getAccountId());

        return savedOrder;
    }

    @Override
    public Order findOrderById(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }

    @Override
    @Transactional
    public void updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            restoreStockForOrder(order);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        // Create history event for admin update
        String notes = "Order status updated to '" + newStatus.name() + "' by DALI Admin.";
        createOrderHistoryEvent(order, newStatus, notes);
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found."));

        if (!order.getAccount().getEmail().equals(username)) {
            throw new SecurityException("User does not have permission to cancel this order.");
        }

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Order cannot be cancelled as it is already being fulfilled.");
        }

        restoreStockForOrder(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Create history event for user cancellation
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