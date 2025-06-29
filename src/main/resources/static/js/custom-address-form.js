
(function() {

    // Configuration for how the dropdowns should look and behave.
    const choicesConfig = {
        searchEnabled: true,
        shouldSort: false,
        itemSelectText: 'Select',
        position: 'bottom',
    };

    /**
     * A function that takes a select element and turns it into a
     * searchable dropdown, but only if it hasn't been done before.
     * @param {HTMLElement} selectElement The <select> element to enhance.
     */
    function enhanceDropdown(selectElement) {
        // If the element doesn't exist or has already been initialized, do nothing.
        if (!selectElement || selectElement.dataset.choicesInitialized === 'true') {
            return;
        }

        // Initialize the searchable dropdown.
        new Choices(selectElement, {
            ...choicesConfig,
            placeholderValue: selectElement.querySelector('option[value=""]').textContent || 'Select...'
        });

        // Mark the element as initialized so we don't do it again.
        selectElement.dataset.choicesInitialized = 'true';
    }

    /**
     * This function runs after any HTMX swap and enhances any new
     * address dropdowns that have appeared in the swapped content.
     * @param {HTMLElement} targetContainer The element that received the new HTML.
     */
    function processSwappedContent(targetContainer) {
        // Find all potential address dropdowns inside the new content.
        // This works even if the targetContainer is the select element itself.
        if (targetContainer.matches('#province-select, #city-select, #barangay-select')) {
            enhanceDropdown(targetContainer);
        } else {
            targetContainer.querySelectorAll('#province-select, #city-select, #barangay-select').forEach(enhanceDropdown);
        }
    }

    // --- Main Logic ---
    // Listen for the HTMX event that fires after content has been swapped into the page.
    document.body.addEventListener('htmx:afterSwap', function(event) {
        processSwappedContent(event.detail.target);
    });

})();