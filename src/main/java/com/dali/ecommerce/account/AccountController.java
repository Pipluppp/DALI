package com.dali.ecommerce.account;

import com.dali.ecommerce.order.Order;
import com.dali.ecommerce.order.OrderRepository;
import com.dali.ecommerce.cart.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final OrderRepository orderRepository;
    private final UserDetailsService userDetailsService;
    private final CartService cartService;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AccountController(AccountRepository accountRepository,
                             AccountService accountService,
                             OrderRepository orderRepository,
                             @Qualifier("customUserDetailsService") UserDetailsService userDetailsService,
                             CartService cartService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.orderRepository = orderRepository;
        this.userDetailsService = userDetailsService;
        this.cartService = cartService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("account", new Account());
        return "register";
    }

    @PostMapping("/register")
    public String registerUserAccount(@ModelAttribute("account") @Valid Account account,
                                      BindingResult bindingResult,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("account", account);
            return "register";
        }

        Account registeredUser;
        try {
            registeredUser = accountService.registerNewUser(account);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }

        // Automatically log the user in after successful registration
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(registeredUser.getEmail());
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            // Merge the session cart with the new user's DB cart
            cartService.mergeSessionCartWithDbCart(request.getSession(), registeredUser.getEmail());

            // Redirect to home page as a logged-in user
            return "redirect:/";
        } catch (Exception e) {
            // If auto-login fails for any reason, fall back to the old flow
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/login";
        }
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

    @GetMapping("/profile/details/edit")
    public String getProfileDetailsEditForm(Model model, Authentication authentication) {
        Account account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("account", account);
        return "fragments/profile-details-form :: details-form";
    }

    @GetMapping("/profile/details/view")
    public String getProfileDetailsView(Model model, Authentication authentication) {
        Account account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("account", account);
        return "fragments/profile-details-view :: details-view";
    }

    @PostMapping("/profile/details/update")
    public String updateProfileDetails(@ModelAttribute Account profileUpdates, Authentication authentication, Model model) {
        accountService.updateUserProfile(authentication.getName(), profileUpdates);

        // Fetch the updated account to pass to the view fragment
        Account updatedAccount = accountService.findByEmail(authentication.getName());
        model.addAttribute("account", updatedAccount);

        // Return the view fragment to show the updated, non-editable details
        return "fragments/profile-details-view :: details-view";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordPage() {
        return "change-password";
    }


    @PostMapping("/profile/change-password")
    public String processChangePasswordRedirect(Authentication authentication,
                                                @RequestParam("currentPassword") String currentPassword,
                                                @RequestParam("newPassword") String newPassword,
                                                @RequestParam("confirmPassword") String confirmPassword,
                                                RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/profile/change-password";
        }

        try {
            accountService.changeUserPassword(authentication.getName(), currentPassword, newPassword);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/profile/change-password";
        }

        // On success, redirect back to the main profile page with a success message.
        // The user will need to log in again, which is the secure default.
        redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully! Please log in again for your security.");
        return "redirect:/login";
    }
}