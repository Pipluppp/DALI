package com.dali.ecommerce.product;

import com.dali.ecommerce.cart.CartItem;
import com.dali.ecommerce.cart.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ProductController {

    private final ProductRepository productRepository;
    private final CartService cartService;

    public ProductController(ProductRepository productRepository, CartService cartService) {
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    private void addAvailableQuantitiesToModel(Model model, List<Product> products, Authentication authentication, HttpSession session) {
        List<CartItem> cartItems = cartService.getCartItems(authentication, session);
        Map<Integer, Integer> cartQuantities = cartItems.stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), CartItem::getQuantity, Integer::sum));

        Map<Integer, Integer> availableQuantities = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> {
                            int available = p.getProductQuantity() - cartQuantities.getOrDefault(p.getId(), 0);
                            return Math.max(0, available); // Ensure it's not negative
                        }
                ));

        model.addAttribute("availableQuantities", availableQuantities);
    }

    @GetMapping("/shop")
    public String shop(Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Initial load of all products
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        // Load categories for the sidebar
        model.addAttribute("categories", productRepository.findDistinctCategories());

        addAvailableQuantitiesToModel(model, products, authentication, session);
        return "shop";
    }

    @GetMapping("/shop/products")
    public String searchAndFilterProducts(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "category", required = false) String category,
            Model model, HttpSession session) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Product> products;
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasCategory = category != null && !category.trim().isEmpty();

        if (hasQuery && hasCategory) {
            products = productRepository.findByNameContainingIgnoreCaseAndCategory(query, category);
        } else if (hasQuery) {
            products = productRepository.findByNameContainingIgnoreCase(query);
        } else if (hasCategory) {
            products = productRepository.findByCategory(category);
        } else {
            products = productRepository.findAll();
        }

        model.addAttribute("products", products);
        addAvailableQuantitiesToModel(model, products, authentication, session);
        return "fragments/product-list :: product-list-fragment";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Integer id, Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            model.addAttribute("product", product);

            // Calculate how many more of this item can be added to the cart
            List<CartItem> cartItems = cartService.getCartItems(authentication, session);
            int quantityInCart = cartItems.stream()
                    .filter(item -> item.getProduct().getId().equals(id))
                    .mapToInt(CartItem::getQuantity)
                    .sum();

            int maxAllowedToAdd = product.getProductQuantity() - quantityInCart;
            model.addAttribute("maxAllowedToAdd", Math.max(0, maxAllowedToAdd));

            return "product-detail";
        } else {
            return "redirect:/shop";
        }
    }
}