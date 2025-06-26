package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;
import com.dali.ecommerce.repository.AccountRepository;
import com.dali.ecommerce.service.AddressService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AddressController {

    private final AddressService addressService;
    private final AccountRepository accountRepository;

    public AddressController(AddressService addressService, AccountRepository accountRepository) {
        this.addressService = addressService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/address/new")
    public String getAddressForm(Model model, @RequestParam(name="context", defaultValue="checkout") String context) {
        model.addAttribute("address", new Address());
        model.addAttribute("context", context); // Pass context to the form for the cancel button
        return "fragments/address-form :: address-form";
    }

    @PostMapping("/address/add")
    public String addNewAddress(@ModelAttribute("address") Address address,
                                Authentication authentication,
                                @RequestHeader("Referer") String referer) {
        String email = authentication.getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        addressService.addAddress(account, address);

        // Redirect back to the page that initiated the request
        return "redirect:" + referer;
    }

    @GetMapping("/address/link")
    public String getAddressLinkFragment(@RequestParam("context") String context, Model model) {
        model.addAttribute("context", context);
        return "fragments/address-link-fragment :: link";
    }
}