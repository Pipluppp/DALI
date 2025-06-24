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
        order.setStatus("Processing");

        double subtotal = cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
        double shippingFee = 100.0; // Updated to match design
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
}