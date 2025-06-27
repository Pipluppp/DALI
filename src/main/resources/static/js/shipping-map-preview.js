document.addEventListener('DOMContentLoaded', function () {
    const viewMapBtn = document.getElementById('view-shipping-map-btn');
    const shippingMapModal = document.getElementById('shipping-map-modal');
    const closeShippingMapBtn = document.getElementById('close-shipping-map-btn');

    let shippingMap;
    let mapLayers = []; // Store layers (markers, lines, circles) to remove them later

    // Function to open the modal
    function openModal() {
        if (shippingMapModal) {
            shippingMapModal.style.display = 'flex';
            initializeShippingMap();
        }
    }

    // Function to close the modal
    function closeModal() {
        if (shippingMapModal) {
            shippingMapModal.style.display = 'none';
        }
    }

    // Add event listeners
    if (viewMapBtn) {
        viewMapBtn.addEventListener('click', openModal);
    }
    if (closeShippingMapBtn) {
        closeShippingMapBtn.addEventListener('click', closeModal);
    }

    // Close modal if user clicks outside of the content
    window.addEventListener('click', function (event) {
        if (event.target === shippingMapModal) {
            closeModal();
        }
    });

    function initializeShippingMap() {
        const mapContainer = document.getElementById('shipping-map');
        if (!mapContainer || !viewMapBtn) return;

        // Get coordinates from the button's data attributes
        const warehouseLat = parseFloat(viewMapBtn.dataset.warehouseLat);
        const warehouseLon = parseFloat(viewMapBtn.dataset.warehouseLon);
        const userLat = parseFloat(viewMapBtn.dataset.userLat);
        const userLon = parseFloat(viewMapBtn.dataset.userLon);
        const userAddress = viewMapBtn.dataset.userAddress;

        // Check if user coordinates are valid
        if (isNaN(userLat) || isNaN(userLon)) {
            mapContainer.innerHTML = '<p style="text-align: center; padding: 20px;">Your address does not have pinpointed coordinates. Please edit the address and use the "Pinpoint address" feature.</p>';
            if(shippingMap) {
                shippingMap.remove();
                shippingMap = null;
            }
            return;
        } else {
            // If the map was showing the error message, clear it.
            // This is important for when the modal is reopened after an error.
            if (mapContainer.querySelector('p')) {
                mapContainer.innerHTML = '';
            }
        }

        const warehouseCoords = [warehouseLat, warehouseLon];
        const userCoords = [userLat, userLon];

        // If map is already initialized, clear old layers
        if (shippingMap) {
            mapLayers.forEach(layer => shippingMap.removeLayer(layer));
            mapLayers = []; // Reset the array
        } else {
            // Initialize the map for the first time
            shippingMap = L.map(mapContainer);
            L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                attribution: '© OpenStreetMap contributors © CARTO',
            }).addTo(shippingMap);
        }

        // --- Add new layers and store them ---

        // Add Warehouse Marker using a generic warehouse icon from a CDN
        const warehouseIcon = L.icon({
            iconUrl: 'https://cdn-icons-png.flaticon.com/512/3448/3448339.png',
            iconSize: [40, 40],
            iconAnchor: [20, 40],
            popupAnchor: [0, -40]
        });
        const warehouseMarker = L.marker(warehouseCoords, { icon: warehouseIcon })
            .bindPopup('<b>DALI Warehouse</b><br>Shipping Origin');
        mapLayers.push(warehouseMarker);

        // Add User Address Marker
        const userMarker = L.marker(userCoords)
            .bindPopup(`<b>Your Address</b><br>${userAddress}`);
        mapLayers.push(userMarker);

        // Draw a line between the two points
        const line = L.polyline([warehouseCoords, userCoords], { color: '#b21984', weight: 3, dashArray: '5, 10' });
        mapLayers.push(line);

        // Calculate distance for circle radius
        const distance = L.latLng(warehouseCoords).distanceTo(userCoords); // distance in meters

        // Draw a circle from warehouse to user
        const circle = L.circle(warehouseCoords, {
            color: 'pink',
            fillColor: '#b21984',
            fillOpacity: 0.2,
            radius: distance
        });
        mapLayers.push(circle);

        // Add all layers to the map
        mapLayers.forEach(layer => layer.addTo(shippingMap));

        // Fit map to show both markers
        const bounds = L.latLngBounds(warehouseCoords, userCoords);
        shippingMap.fitBounds(bounds.pad(0.2)); // pad adds some margin

        // Invalidate size to ensure it renders correctly inside the modal
        setTimeout(() => shippingMap.invalidateSize(), 10);
    }
});