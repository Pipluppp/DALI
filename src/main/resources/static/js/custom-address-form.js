/**
 * This script turns our standard address dropdowns into searchable ones.
 * It's designed to work with HTMX, so it will automatically apply
 * the searchable style to any new dropdowns that HTMX loads.
 */
(function() {
    // This object will hold our searchable dropdown instances.
    let addressChoices = {
        province: null,
        city: null,
        barangay: null
    };

    // Configuration for how the dropdowns should look and behave.
    const choicesConfig = {
        searchEnabled: true,
        shouldSort: false, // Use the order from your server
        itemSelectText: 'Select',
         position: 'bottom', // <--- ADD THIS LINE

    };

    /**
     * This is the core function. It finds the address dropdowns on the page,
     * destroys any old searchable instances, and creates new ones.
     * This gets called when the page loads and after HTMX swaps content.
     */
    function initializeAddressDropdowns() {
        const provinceEl = document.getElementById('province-select');
        const cityEl = document.getElementById('city-select');
        const barangayEl = document.getElementById('barangay-select');

        // Destroy old instances to prevent memory leaks or duplicate controls
        if (addressChoices.province) addressChoices.province.destroy();
        if (addressChoices.city) addressChoices.city.destroy();
        if (addressChoices.barangay) addressChoices.barangay.destroy();

        // Initialize a new searchable dropdown for each <select> element that exists on the page
        if (provinceEl) {
            addressChoices.province = new Choices(provinceEl, { ...choicesConfig, placeholderValue: 'Select Province' });
        }
        if (cityEl) {
            addressChoices.city = new Choices(cityEl, { ...choicesConfig, placeholderValue: 'Select City / Municipality' });
        }
        if (barangayEl) {
            addressChoices.barangay = new Choices(barangayEl, { ...choicesConfig, placeholderValue: 'Select Barangay' });
        }
    }

    // --- Main Logic ---

    // 1. Initialize dropdowns when the Add/Edit form first appears
    // We do this by listening for when HTMX swaps content into our target divs.
    document.body.addEventListener('htmx:afterSwap', function(event) {
        const targetId = event.detail.target.id;

        // Check if the swapped content is part of our address form
        if (targetId === 'add-address-target' ||
            targetId === 'address-list-container' ||
            targetId.startsWith('address-block-') ||
            targetId === 'city-select-wrapper' ||
            targetId === 'barangay-select-wrapper') {
            
            // If it is, run our function to initialize the searchable dropdowns.
            initializeAddressDropdowns();
        }
    });

})();