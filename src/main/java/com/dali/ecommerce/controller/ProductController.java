package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Product;
import com.dali.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // This method loads the FULL shop page initially
    @GetMapping("/shop")
    public String shop(Model model) {
        List<Product> products = productRepository.findAll();
        List<String> categories = productRepository.findDistinctCategories();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        return "shop";
    }

    // This NEW method handles HTMX search requests and returns ONLY the product list fragment
    @GetMapping("/shop/search")
    public String searchProducts(@RequestParam(value = "query", required = false) String query,
                                 @RequestParam(value = "category", required = false) String category,
                                 Model model) {
        List<Product> products;

        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasCategory = category != null && !category.isEmpty();

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

        // Return the fragment name
        return "fragments/product-list :: product-list-fragment";
    }


    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Integer id, Model model) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            model.addAttribute("product", productOptional.get());
            return "product-detail";
        } else {
            return "redirect:/shop"; // Or a 404 page
        }
    }
}