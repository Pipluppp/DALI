package com.dali.ecommerce.location;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/cities")
    public String getCitiesForProvince(@RequestParam(required = false) Integer provinceId, Model model) {
        if (provinceId == null) {
            return "fragments/options :: disabled-city-select";
        }
        model.addAttribute("cities", locationService.getCitiesByProvinceId(provinceId));
        return "fragments/options :: city-select";
    }

    @GetMapping("/barangays")
    public String getBarangaysForCity(@RequestParam(required = false) Integer cityId, Model model) {
        if (cityId == null) {
            return "fragments/options :: disabled-barangay-select";
        }
        model.addAttribute("barangays", locationService.getBarangaysByCityId(cityId));
        return "fragments/options :: barangay-select";
    }
}