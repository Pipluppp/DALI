package com.dali.ecommerce.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class PasswordResetController {

    private final AccountService accountService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetController(AccountService accountService, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.accountService = accountService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        Account account = accountService.findByEmail(email);
        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "No account found with that email address.");
            return "redirect:/forgot-password";
        }

        String token = UUID.randomUUID().toString();
        accountService.createPasswordResetTokenForUser(account, token);
        emailService.sendPasswordResetEmail(account.getEmail(), token);

        redirectAttributes.addFlashAttribute("message", "A password reset link has been sent to your email.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        Account account = accountService.findByResetPasswordToken(token);
        if (account == null) {
            model.addAttribute("error", "Invalid or expired password reset token.");
            return "redirect:/login";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword, // Get the confirmation password
                                       RedirectAttributes redirectAttributes) {

        // 1. Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            redirectAttributes.addAttribute("token", token); // Pass the token back so the link still works
            return "redirect:/reset-password";
        }

        // 2. Find the account by token
        Account account = accountService.findByResetPasswordToken(token);
        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired password reset token.");
            return "redirect:/login";
        }

        try {

            accountService.resetUserPassword(token, newPassword);

        } catch (IllegalArgumentException e) {
            // If validation fails, redirect back with the error message
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("token", token); // Pass token back
            return "redirect:/reset-password";
        }

        redirectAttributes.addFlashAttribute("message", "You have successfully reset your password. Please log in.");
        return "redirect:/login";
    }
}