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

    // This method now ONLY loads the page shell and initial data.
    @GetMapping("/shop")
    public String shop(Model model) {
        // Initial load of all products
        model.addAttribute("products", productRepository.findAll());
        // Load categories for the sidebar
        model.addAttribute("categories", productRepository.findDistinctCategories());
        return "shop";
    }

    // THIS IS THE NEW HTMX-DEDICATED ENDPOINT
    @GetMapping("/shop/products")
    public String searchAndFilterProducts(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "category", required = false) String category,
            Model model) {

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
        // This tells Thymeleaf to only render the 'product-list-fragment' part of the specified file
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