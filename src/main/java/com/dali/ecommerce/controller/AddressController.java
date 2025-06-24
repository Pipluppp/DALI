package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;
import com.dali.ecommerce.repository.AccountRepository;
import com.dali.ecommerce.service.AddressService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class AddressController {

    private final AddressService addressService;
    private final AccountRepository accountRepository;

    public AddressController(AddressService addressService, AccountRepository accountRepository) {
        this.addressService = addressService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/address/new")
    public String getAddressForm(Model model) {
        model.addAttribute("address", new Address());
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
}