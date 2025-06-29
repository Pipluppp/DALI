package com.dali.ecommerce.address;

import com.dali.ecommerce.location.LocationService;
import com.dali.ecommerce.account.Account;
import com.dali.ecommerce.account.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AddressController {

    private final AddressService addressService;
    private final AccountRepository accountRepository;
    private final LocationService locationService;

    public AddressController(AddressService addressService, AccountRepository accountRepository, LocationService locationService) {
        this.addressService = addressService;
        this.accountRepository = accountRepository;
        this.locationService = locationService;
    }

    @GetMapping("/address/new")
    public String getAddressForm(Model model, @RequestParam(name="context", defaultValue="checkout") String context) {
        model.addAttribute("address", new Address());
        model.addAttribute("provinces", locationService.getAllProvinces());
        model.addAttribute("context", context);
        return "fragments/address-form :: address-form";
    }

    @PostMapping("/address/add")
    public String addNewAddress(@ModelAttribute Address address,
                                @RequestParam("provinceId") Integer provinceId,
                                @RequestParam("cityId") Integer cityId,
                                @RequestParam("barangayId") Integer barangayId,
                                Authentication authentication,
                                @RequestHeader("Referer") String referer) {
        String email = authentication.getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        addressService.addAddress(account, address, provinceId, cityId, barangayId);

        // Redirect back to the page that initiated the request
        return "redirect:" + referer;
    }

    @GetMapping("/address/link")
    public String getAddressLinkFragment(@RequestParam("context") String context, Model model) {
        model.addAttribute("context", context);
        return "fragments/address-link-fragment :: link";
    }

    @GetMapping("/address/edit/{id}")
    public String getEditAddressForm(@PathVariable("id") Integer addressId,
                                     @RequestParam("context") String context,
                                     Authentication authentication,
                                     Model model) {
        // Fetch the address securely
        Address address = addressService.findByIdAndAccount(addressId, authentication.getName());

        model.addAttribute("address", address);
        model.addAttribute("context", context);

        // Pre-populate location dropdowns
        model.addAttribute("provinces", locationService.getAllProvinces());
        if (address.getProvince() != null) {
            model.addAttribute("cities", locationService.getCitiesByProvinceId(address.getProvince().getId()));
        }
        if (address.getCity() != null) {
            model.addAttribute("barangays", locationService.getBarangaysByCityId(address.getCity().getId()));
        }
        return "fragments/address-form :: address-form";
    }

    @PostMapping("/address/update/{id}")
    public String updateAddress(@PathVariable("id") Integer addressId,
                                @ModelAttribute Address addressDetails,
                                @RequestParam("provinceId") Integer provinceId,
                                @RequestParam("cityId") Integer cityId,
                                @RequestParam("barangayId") Integer barangayId,
                                Authentication authentication,
                                @RequestHeader("Referer") String referer) {

        addressService.updateAddress(addressId, authentication.getName(), addressDetails, provinceId, cityId, barangayId);
        return "redirect:" + referer;
    }
}