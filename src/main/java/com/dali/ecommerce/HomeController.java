package com.dali.ecommerce;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        // You can add attributes to the model here to pass data to the view
        // For example: model.addAttribute("products", productService.getFeaturedProducts());
        return "home";
    }
}