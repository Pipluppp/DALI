package com.dali.ecommerce.location;

import com.dali.ecommerce.location.LocationService;
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
    public String getCitiesForProvince(@RequestParam("provinceId") Integer provinceId, Model model) {
        model.addAttribute("cities", locationService.getCitiesByProvinceId(provinceId));
        return "fragments/options :: city-options";
    }

    @GetMapping("/barangays")
    public String getBarangaysForCity(@RequestParam("cityId") Integer cityId, Model model) {
        model.addAttribute("barangays", locationService.getBarangaysByCityId(cityId));
        return "fragments/options :: barangay-options";
    }
}