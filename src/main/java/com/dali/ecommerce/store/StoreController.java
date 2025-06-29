package com.dali.ecommerce.store;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class StoreController {

    private final StoreRepository storeRepository;

    public StoreController(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    // Main page load - fetches ALL stores
    @GetMapping("/stores")
    public String showStoresPage(Model model) {
        List<Store> stores = storeRepository.findAll();
        model.addAttribute("stores", stores);
        return "stores";
    }

    // HTMX search endpoint - fetches ALL matching stores
    @GetMapping("/stores/search")
    public String searchStores(@RequestParam(value = "query", required = false) String query, Model model) {
        List<Store> stores;
        if (query != null && !query.trim().isEmpty()) {
            stores = storeRepository.findByNameContainingIgnoreCase(query);
        } else {
            stores = storeRepository.findAll();
        }
        model.addAttribute("stores", stores);
        return "fragments/store-list :: store-list-fragment";
    }

    @GetMapping("/api/stores")
    @ResponseBody
    public List<Store> getAllStoresApi() {
        return storeRepository.findAll();
    }

    // Endpoint for the checkout store chooser
    @GetMapping("/stores/search-for-checkout")
    public String searchStoresForCheckout(@RequestParam(value = "query", required = false) String query, Model model, HttpSession session) {
        List<Store> stores;
        if (query != null && !query.trim().isEmpty()) {
            stores = storeRepository.findByNameContainingIgnoreCase(query);
        } else {
            stores = storeRepository.findAll();
        }
        model.addAttribute("stores", stores);

        Map<String, Object> checkoutDetails = (Map<String, Object>) session.getAttribute("checkoutDetails");
        if (checkoutDetails != null && checkoutDetails.get("storeId") != null) {
            model.addAttribute("selectedStoreId", checkoutDetails.get("storeId"));
        } else {
            model.addAttribute("selectedStoreId", null);
        }

        return "fragments/checkout-store-list :: store-list-fragment";
    }
}