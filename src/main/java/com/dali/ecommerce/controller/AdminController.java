package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Product;
import com.dali.ecommerce.repository.ProductRepository;
import com.dali.ecommerce.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    public AdminController(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @GetMapping("/login")
    public String showAdminLoginPage() {
        return "admin-login";
    }

    @GetMapping
    public String adminHome() {
        return "admin-home";
    }

    @GetMapping("/inventory")
    public String showInventoryPage(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("categories", productRepository.findDistinctCategories());
        return "admin-inventory";
    }

    @GetMapping("/inventory/products")
    public String searchAndFilterInventory(
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
        return "fragments/admin-inventory-product-list :: product-list-fragment";
    }

    @GetMapping("/product/{id}")
    public String adminProductDetail(@PathVariable("id") Integer id, Model model) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            model.addAttribute("product", productOptional.get());
            return "admin-product-detail.html";
        } else {
            return "redirect:/admin/inventory";
        }
    }

    @PostMapping("/inventory/update")
    public String updateStock(@RequestParam("productId") Integer productId,
                              @RequestParam("quantity") Integer quantity,
                              @RequestHeader("Referer") String referer,
                              RedirectAttributes redirectAttributes) {
        try {
            productService.updateProductStock(productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Stock updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating stock: " + e.getMessage());
        }
        return "redirect:" + referer;
    }


    @GetMapping("/orders")
    public String showOrdersPage() {
        return "admin-orders";
    }
}