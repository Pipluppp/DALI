package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Order;
import com.dali.ecommerce.repository.AccountRepository;
import com.dali.ecommerce.repository.OrderRepository;
import com.dali.ecommerce.service.AccountService;
import com.dali.ecommerce.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.dali.ecommerce.model.Address;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;




import java.util.List;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

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
        if  (bindingResult.hasErrors()) {
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

    /*@GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        // This string "forgot-password" MUST match your HTML file name.
        return "forgot-password";
    }*/

    @GetMapping("/account/address/form")
    public String getProfileAddressForm(Model model) {
        model.addAttribute("address", new Address());
        // This will return the content of a NEW file you will create: 'profile-address-form.html'
        return "fragments/profile-address-form :: profile-address-form";
    }

    @GetMapping("/account/address/link")
    public String getProfileAddAddressLink() {
        // This will return the content of a NEW file you will create: 'profile-add-address-link.html'
        return "fragments/profile-add-address-link :: profile-add-address-link";
    }


    @PostMapping("/account/address/add")
    public String saveNewAddressFromProfile(@ModelAttribute Address address, Authentication authentication) {

        return "redirect:/profile";
    }

    @GetMapping("/profile/password/form")
    public String getChangePasswordForm() {
        // This correctly returns the path to the HTML fragment.
        System.out.println(">>>>>>>>> AccountController: getChangePasswordForm() was called! <<<<<<<<<");
        return "fragments/change-password-form :: change-password-form";
        
    }

    @GetMapping("/profile/password/link")
    public String getChangePasswordLink() {
    // This serves the content of 'fragments/change-password-link.html'
        return "fragments/change-password-link :: change-password-link";
    }

    @PostMapping("/profile/change-password")
    public String processChangePassword(Authentication authentication,
                                    @RequestParam("currentPassword") String currentPassword,
                                    @RequestParam("newPassword") String newPassword,
                                    @RequestParam("confirmPassword") String confirmPassword,
                                    Model model) { // Note: We use Model, not RedirectAttributes

    // Check if the new passwords match
    if (!newPassword.equals(confirmPassword)) {
        // If they don't match, add an error message to the model...
        model.addAttribute("passwordChangeError", "New passwords do not match.");
        // ...and return the form fragment again, which will display the error.
        return "fragments/change-password-form :: change-password-form";
    }

    try {
        // Call your existing service method. This logic is correct.
        accountService.changeUserPassword(authentication.getName(), currentPassword, newPassword);
    } catch (Exception e) {
        // If the service throws an error (e.g., wrong password)...
        model.addAttribute("passwordChangeError", e.getMessage());
        // ...return the form fragment again to display the error.
        return "fragments/change-password-form :: change-password-form";
    }

    // If successful, return a different fragment that just contains a success message.
    return "fragments/password-change-success :: success-message";
    }

    


}