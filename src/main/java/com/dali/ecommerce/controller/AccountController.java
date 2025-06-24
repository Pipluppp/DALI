package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Order;
import com.dali.ecommerce.repository.AccountRepository;
import com.dali.ecommerce.repository.OrderRepository;
import com.dali.ecommerce.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final OrderRepository orderRepository;

    public AccountController(AccountRepository accountRepository, AccountService accountService, OrderRepository orderRepository) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("account", new Account());
        return "register";
    }

    @PostMapping("/register")
    public String registerUserAccount(@ModelAttribute("account") Account account, RedirectAttributes redirectAttributes) {
        try {
            accountService.registerNewUser(account);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
        redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Order> orders = orderRepository.findByAccountAccountIdOrderByCreatedAtDesc(account.getAccountId());

        model.addAttribute("account", account);
        model.addAttribute("orders", orders);
        model.addAttribute("hasOrders", !orders.isEmpty());

        return "profile";
    }
}