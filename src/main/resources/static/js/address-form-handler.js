document.addEventListener('DOMContentLoaded', function() {
    const manilaCoords = [14.5995, 120.9842];
    let map;
    let marker;

    // A single, delegated event listener on the body for all clicks.6
    // This is more efficient and handles dynamically added elements.
    document.body.addEventListener('click', function(e) {
        if (!e.target) return;

        // Handle opening the map modal
        if (e.target.id === 'pinpoint-btn') {
            const modal = document.getElementById('map-modal');
            if (modal) {
                modal.style.display = 'flex';
                initializeMap(); // Initialize or update map view
            }
        }

        // Handle closing the map modal
        if (e.target.id === 'close-modal-btn') {
            const modal = document.getElementById('map-modal');
            if (modal) {
                modal.style.display = 'none';
            }
        }

        // Handle confirming the pin location
        if (e.target.id === 'confirm-pin-btn') {
            confirmPinLocation();
        }
    });


    /**
     * Initializes the Leaflet map if it doesn't exist,
     * or invalidates its size if it does to ensure proper rendering.
     */
    function initializeMap() {
        const mapContainer = document.getElementById('map');
        if (!mapContainer) return; // Guard: do nothing if map container isn't on the page

        // If map already exists, just invalidate size
        if (map) {
            setTimeout(() => map.invalidateSize(), 10);
            return;
        }

        // If map doesn't exist, create it
        map = L.map('map').setView(manilaCoords, 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
        marker = L.marker(manilaCoords, { draggable: true }).addTo(map);

        // Invalidate size after a short delay to ensure correct rendering
        setTimeout(() => map.invalidateSize(), 10);
    }

    /**
     * Captures the marker's coordinates, updates the hidden form fields,
     * provides visual feedback, and closes the modal.
     */
    function confirmPinLocation() {
        if (!marker) return;
        const modal = document.getElementById('map-modal');
        const position = marker.getLatLng();
        const latInput = document.getElementById('latitude');
        const lngInput = document.getElementById('longitude');
        const coordsDisplay = document.getElementById('coords-display');

        if (latInput && lngInput) {
            latInput.value = position.lat;
            lngInput.value = position.lng;
        }
        if (coordsDisplay) {
            coordsDisplay.textContent = `Pinned at: ${position.lat.toFixed(5)}, ${position.lng.toFixed(5)}`;
        }
        if (modal) {
            modal.style.display = 'none';
        }
    }
});