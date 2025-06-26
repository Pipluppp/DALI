document.addEventListener('DOMContentLoaded', function() {
    const manilaCoords = [14.5995, 120.9842];
    let map;
    // The draggable marker is no longer needed.

    // A single, delegated event listener on the body for all clicks.
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
     * A fixed pin is added to the center of the map.
     */
    function initializeMap() {
        const mapContainer = document.getElementById('map');
        if (!mapContainer) return; // Guard: do nothing if map container isn't on the page

        // If map already exists, just re-center and invalidate its size
        if (map) {
            map.setView(manilaCoords, 13);
            setTimeout(() => map.invalidateSize(), 10);
            return;
        }

        // If map doesn't exist, create it
        map = L.map('map').setView(manilaCoords, 13);

        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/attributions">CARTO</a>',
            subdomains: 'abcd',
            maxZoom: 20
        }).addTo(map);

        // --- NEW: Add a fixed center pin ---
        // Create the pin element dynamically using Leaflet's utility
        const centerPin = L.DomUtil.create('div', 'center-pin-container');
        centerPin.innerHTML = '<img src="https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png" alt="Pin">';

        // Style the container to be in the center and non-interactive
        centerPin.style.cssText = 'position:absolute; left:50%; top:50%; z-index:1000; transform:translate(-50%, -100%); pointer-events:none; width:25px; height:41px;';
        centerPin.querySelector('img').style.cssText = 'width:100%; height:100%;';

        // Add the pin to the map's container
        map.getContainer().appendChild(centerPin);

        // Invalidate size after a short delay to ensure correct rendering
        setTimeout(() => map.invalidateSize(), 10);
    }

    /**
     * Captures the map center's coordinates, updates the hidden form fields,
     * provides visual feedback, and closes the modal.
     */
    function confirmPinLocation() {
        // map must exist to confirm a location.
        if (!map) return;
        const modal = document.getElementById('map-modal');

        // --- UPDATED: Get the coordinates of the map's center ---
        const position = map.getCenter();

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