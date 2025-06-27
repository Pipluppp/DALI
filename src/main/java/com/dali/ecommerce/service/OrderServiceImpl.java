package com.dali.ecommerce.service;

import com.dali.ecommerce.model.*;
import com.dali.ecommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProductRepository productRepository; // To update stock

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository, AccountRepository accountRepository, AddressRepository addressRepository, CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.accountRepository = accountRepository;
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
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
        order.setStatus(OrderStatus.PROCESSING); // Use the enum

        double subtotal = cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
        // 2. Get the dynamically calculated shipping fee from the checkout session details.
        Number shippingFeeNumber = (Number) checkoutDetails.get("shippingFee");
        if (shippingFeeNumber == null) {
            // This is a safety check. This should not happen in a normal flow.
            throw new IllegalStateException("Shipping fee is missing from checkout details.");
        }
        double shippingFee = shippingFeeNumber.doubleValue();

        order.setTotalPrice(subtotal + shippingFee);

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItems.add(orderItem);

            // Decrease product stock
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

        // Clear user's cart
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

        // If order is being cancelled, restore product stock
        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            restoreStockForOrder(order);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        // TODO: Add email notification logic here later
        // emailService.sendOrderStatusUpdate(order.getAccount().getEmail(), order);
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found."));

        // Security Check: Ensure the user owns this order
        if (!order.getAccount().getEmail().equals(username)) {
            throw new SecurityException("User does not have permission to cancel this order.");
        }

        // Business Logic: Only allow cancellation if the order is still processing
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Order cannot be cancelled as it is already being fulfilled.");
        }

        restoreStockForOrder(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // TODO: Email notification
    }

    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int newQuantity = product.getProductQuantity() + item.getQuantity();
            product.setProductQuantity(newQuantity);
            productRepository.save(product);
        }
    }
}