/**
 * FINAL, ROBUST VERSION
 * This script enhances address dropdowns and prevents form submission bugs.
 * It runs a universal check after EVERY htmx swap to find and fix any
 * address forms on the page, covering all cases (Add, Edit, and dynamic loads).
 */
(function() {

    const choicesConfig = {
        searchEnabled: true,
        shouldSort: false,
        itemSelectText: 'Select',
        position: 'bottom',
    };

    function enhanceDropdown(selectElement) {
        // If the element doesn't exist or is already enhanced, do nothing.
        if (!selectElement || selectElement.dataset.choicesInitialized === 'true') {
            return;
        }
        new Choices(selectElement, {
            ...choicesConfig,
            placeholderValue: selectElement.querySelector('option[value=""]').textContent || 'Select...'
        });
        // Mark it as done.
        selectElement.dataset.choicesInitialized = 'true';
    }

    function preventDoubleSubmission(formElement) {
        // If the form doesn't exist or already has the fix, do nothing.
        if (!formElement || formElement.dataset.submitListenerAdded === 'true') {
            return;
        }
        formElement.addEventListener('submit', function() {
            const button = formElement.querySelector('button[type="submit"]');
            if (button) {
                button.disabled = true;
                button.textContent = 'Saving...';
            }
        });
        // Mark it as done.
        formElement.dataset.submitListenerAdded = 'true';
    }

    /**
     * The main controller function.
     * Scans the ENTIRE document for any address forms and dropdowns
     * that have not yet been processed.
     */
    function initializeAllAddressForms() {
        // Find all address dropdowns on the page.
        document.querySelectorAll('#province-select, #city-select, #barangay-select').forEach(enhanceDropdown);

        // Find all address forms on the page.
        document.querySelectorAll('.address-form').forEach(preventDoubleSubmission);
    }

    // --- Main Event Listeners ---

    // 1. Run once when the page first loads.
    document.addEventListener('DOMContentLoaded', initializeAllAddressForms);

    // 2. Run again after EVERY successful HTMX swap.
    document.body.addEventListener('htmx:afterSwap', initializeAllAddressForms);

})();