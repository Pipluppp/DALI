// Can't do it in HTMX due to the LeafletJS functionality in checkout, had to create checkout's own JS
document.addEventListener('DOMContentLoaded', () => {
    const deliveryOptions = document.querySelectorAll('input[name="deliveryMethod"]');
    const storeSelectorSection = document.getElementById('store-pickup-selector');

    // This is the container where HTMX replaces the list of stores.
    const storeListContainer = document.getElementById('store-radio-list-container');

    // Sets the 'required' attribute on store radios based on the selected delivery method.
    function setStoreRadioRequiredStatus() {
        if (!storeSelectorSection) return;

        const selectedDelivery = document.querySelector('input[name="deliveryMethod"]:checked');
        const isPickup = selectedDelivery && selectedDelivery.value === 'Pickup Delivery';

        const storeRadios = storeSelectorSection.querySelectorAll('input[name="storeId"]');
        storeRadios.forEach(radio => {
            radio.required = isPickup;
        });
    }

    // Toggles the visibility of the entire pickup section.
    function toggleStoreSelectorVisibility() {
        if (!storeSelectorSection) return;

        const selectedDelivery = document.querySelector('input[name="deliveryMethod"]:checked');
        const isPickup = selectedDelivery && selectedDelivery.value === 'Pickup Delivery';

        storeSelectorSection.style.display = isPickup ? 'block' : 'none';
    }

    // Main handler function to run when a delivery method is changed.
    function handleDeliveryChange() {
        toggleStoreSelectorVisibility();
        setStoreRadioRequiredStatus();
    }

    // Attach event listener to run the handler when a delivery option is chosen.
    deliveryOptions.forEach(option => option.addEventListener('change', handleDeliveryChange));

    // If the store list is dynamically replaced by HTMX, re-apply the logic.
    if (storeListContainer) {
        // The 'htmx:afterSwap' event fires after new content is swapped in.
        storeListContainer.addEventListener('htmx:afterSwap', () => {
            setStoreRadioRequiredStatus();
        });
    }

    // Run the handler on initial page load to set the correct state.
    handleDeliveryChange();
});