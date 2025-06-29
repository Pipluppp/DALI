package com.dali.ecommerce.cart;

import com.dali.ecommerce.account.Account;
import com.dali.ecommerce.account.AccountRepository;
import com.dali.ecommerce.product.Product;
import com.dali.ecommerce.product.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private static final String SESSION_CART = "sessionCart";

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository, AccountRepository accountRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
    }

    private Optional<Account> findAccountByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return accountRepository.findByEmail(email.trim());
    }

    private Optional<Account> resolveAuthenticatedAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || (authentication instanceof AnonymousAuthenticationToken)) {
            return Optional.empty();
        }
        return findAccountByEmail(authentication.getName());
    }

    @Transactional
    public void addToCart(Integer productId, int quantity, Authentication authentication, HttpSession session) {
        Optional<Account> accountOpt = resolveAuthenticatedAccount(authentication);

        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            Optional<CartItem> existingItem = cartItemRepository.findByAccountAccountIdAndProductId(account.getAccountId(), productId);
            if (existingItem.isPresent()) {
                CartItem cartItem = existingItem.get();
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
                cartItemRepository.save(cartItem);
            } else {
                Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
                CartItem newItem = new CartItem();
                newItem.setAccount(account);
                newItem.setProduct(product);
                newItem.setQuantity(quantity);
                cartItemRepository.save(newItem);
            }
        } else {
            Map<Integer, Integer> cart = getSessionCart(session);
            cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
            session.setAttribute(SESSION_CART, cart);
        }
    }

    public List<CartItem> getCartItems(Authentication authentication, HttpSession session) {
        Optional<Account> accountOpt = resolveAuthenticatedAccount(authentication);

        if (accountOpt.isPresent()) {
            return cartItemRepository.findByAccountAccountId(accountOpt.get().getAccountId());
        } else {
            Map<Integer, Integer> sessionCart = getSessionCart(session);
            return sessionCart.entrySet().stream()
                    .map(entry -> {
                        Product product = productRepository.findById(entry.getKey()).orElse(null);
                        if (product == null) return null;
                        CartItem item = new CartItem();
                        item.setProduct(product);
                        item.setQuantity(entry.getValue());
                        return item;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    @Transactional
    public void updateQuantity(Integer productId, int quantity, Authentication authentication, HttpSession session) {
        if (quantity <= 0) {
            removeFromCart(productId, authentication, session);
            return;
        }

        Optional<Account> accountOpt = resolveAuthenticatedAccount(authentication);
        if (accountOpt.isPresent()) {
            cartItemRepository.findByAccountAccountIdAndProductId(accountOpt.get().getAccountId(), productId)
                    .ifPresent(item -> {
                        item.setQuantity(quantity);
                        cartItemRepository.save(item);
                    });
        } else {
            Map<Integer, Integer> cart = getSessionCart(session);
            if (cart.containsKey(productId)) {
                cart.put(productId, quantity);
                session.setAttribute(SESSION_CART, cart);
            }
        }
    }

    @Transactional
    public void removeFromCart(Integer productId, Authentication authentication, HttpSession session) {
        Optional<Account> accountOpt = resolveAuthenticatedAccount(authentication);
        if (accountOpt.isPresent()) {
            cartItemRepository.findByAccountAccountIdAndProductId(accountOpt.get().getAccountId(), productId)
                    .ifPresent(cartItemRepository::delete);
        } else {
            Map<Integer, Integer> cart = getSessionCart(session);
            cart.remove(productId);
            session.setAttribute(SESSION_CART, cart);
        }
    }

    @Transactional
    public void clearCart(Authentication authentication, HttpSession session) {
        Optional<Account> accountOpt = resolveAuthenticatedAccount(authentication);
        if (accountOpt.isPresent()) {
            cartItemRepository.deleteByAccountAccountId(accountOpt.get().getAccountId());
        } else {
            session.removeAttribute(SESSION_CART);
        }
    }

    public double getCartTotal(List<CartItem> cartItems) {
        return cartItems.stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public int getCartItemCount(Authentication authentication, HttpSession session) {
        Optional<Account> accountOpt = resolveAuthenticatedAccount(authentication);

        if (accountOpt.isPresent()) {
            return cartItemRepository.findByAccountAccountId(accountOpt.get().getAccountId()).stream()
                    .mapToInt(CartItem::getQuantity)
                    .sum();
        } else {
            return getSessionCart(session).values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    @Transactional
    public void mergeSessionCartWithDbCart(HttpSession session, String username) {
        Map<Integer, Integer> sessionCart = getSessionCart(session);
        if (sessionCart.isEmpty()) return;

        Optional<Account> accountOpt = findAccountByEmail(username);
        if (accountOpt.isEmpty()) {
            return;
        }
        Account account = accountOpt.get();

        sessionCart.forEach((productId, quantity) -> {
            Optional<CartItem> existingItem = cartItemRepository.findByAccountAccountIdAndProductId(account.getAccountId(), productId);
            if (existingItem.isPresent()) {
                CartItem dbItem = existingItem.get();
                dbItem.setQuantity(dbItem.getQuantity() + quantity); // Merge quantities
                cartItemRepository.save(dbItem);
            } else {
                productRepository.findById(productId).ifPresent(product -> {
                    CartItem newItem = new CartItem();
                    newItem.setAccount(account);
                    newItem.setProduct(product);
                    newItem.setQuantity(quantity);
                    cartItemRepository.save(newItem);
                });
            }
        });
        session.removeAttribute(SESSION_CART);
    }

    private Map<Integer, Integer> getSessionCart(HttpSession session) {
        Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute(SESSION_CART);
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute(SESSION_CART, cart);
        }
        return cart;
    }
}