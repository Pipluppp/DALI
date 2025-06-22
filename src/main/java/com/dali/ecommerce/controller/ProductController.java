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

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) String category, Model model) {
        List<Product> products;
        if (category != null && !category.isEmpty()) {
            products = productRepository.findByCategory(category);
            model.addAttribute("selectedCategory", category);
        } else {
            products = productRepository.findAll();
        }

        List<String> categories = productRepository.findDistinctCategories();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        return "shop";
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