package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Store;
import com.dali.ecommerce.repository.StoreRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class StoreController {

    private final StoreRepository storeRepository;

    public StoreController(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    // Loads the full page shell with initial data
    @GetMapping("/stores")
    public String showStoresPage(Model model) {
        List<Store> stores = storeRepository.findAll();
        model.addAttribute("stores", stores);
        return "stores";
    }

    // HTMX endpoint for searching stores
    @GetMapping("/stores/search")
    public String searchStores(@RequestParam(value = "query", required = false) String query, Model model) {
        List<Store> stores;
        if (query != null && !query.trim().isEmpty()) {
            stores = storeRepository.findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(query, query);
        } else {
            stores = storeRepository.findAll();
        }
        model.addAttribute("stores", stores);
        return "fragments/store-list :: store-list-fragment";
    }
}