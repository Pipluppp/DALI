document.addEventListener('DOMContentLoaded', function () {
    const mapContainer = document.getElementById('stores-map');
    const storeListPanel = document.querySelector('.store-list-panel'); // The static parent for the click listener
    let map;
    const markerMap = new Map(); // Use a Map to store markers by store ID

    // --- Custom Pink Marker Icon using SVG ---
    const pinkIcon = L.divIcon({
        html: `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" fill="#b21984" width="32px" height="32px"><path d="M12 0C7.8 0 4 3.8 4 8.5c0 5.1 8 15.5 8 15.5s8-10.4 8-15.5C20 3.8 16.2 0 12 0zm0 12a3 3 0 110-6 3 3 0 010 6z"/></svg>`,
        className: '', // remove default background and border
        iconSize: [32, 32],
        iconAnchor: [16, 32], // Point of the icon which will correspond to marker's location
        popupAnchor: [0, -32] // Point from which the popup should open relative to the iconAnchor
    });


    function initializeMap() {
        if (!mapContainer) return;

        // 1. Initialize map centered on Manila with a closer zoom
        map = L.map('stores-map').setView([14.5995, 120.9842], 11);

        // 2. Add a tile layer
        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/attributions">CARTO</a>',
            subdomains: 'abcd',
            maxZoom: 20
        }).addTo(map);

        // 3. Fetch store data
        fetch('/api/stores')
            .then(response => {
                if (!response.ok) throw new Error('Network response was not ok');
                return response.json();
            })
            .then(stores => {
                const markers = [];
                // 4. Clear old markers from the map before adding new ones
                markerMap.forEach(marker => marker.remove());
                markerMap.clear();

                // 5. Loop through stores and create markers
                stores.forEach(store => {
                    if (store.latitude != null && store.longitude != null) {
                        const marker = L.marker([store.latitude, store.longitude], { icon: pinkIcon })
                            .bindPopup(`<b>${store.name}</b>`);

                        markers.push(marker);
                        markerMap.set(store.id, marker); // Store marker by its ID
                    }
                });

                // 6. Add markers to a feature group and fit bounds
                if (markers.length > 0) {
                    const featureGroup = L.featureGroup(markers).addTo(map);
                    // Don't fit bounds on initial load to keep the Manila view, but this is useful for search
                    // map.fitBounds(featureGroup.getBounds().pad(0.1));
                }
            })
            .catch(error => {
                console.error('Error fetching or processing store data:', error);
                mapContainer.innerHTML = '<p style="text-align:center; padding: 20px;">Could not load store locations.</p>';
            });
    }

    function handleStoreClick(event) {
        const storeItem = event.target.closest('.store-item-clickable');
        if (!storeItem) return; // Exit if the click was not on a store item

        const lat = parseFloat(storeItem.dataset.lat);
        const lng = parseFloat(storeItem.dataset.lng);
        const storeId = parseInt(storeItem.dataset.storeId, 10);

        if (map && !isNaN(lat) && !isNaN(lng)) {
            // Pan the map to the selected location
            map.flyTo([lat, lng], 15); // flyTo provides a smooth animation, 15 is a close zoom level

            // Open the corresponding marker's popup
            if (markerMap.has(storeId)) {
                const marker = markerMap.get(storeId);
                // A short delay allows the pan animation to start before the popup opens
                setTimeout(() => {
                    marker.openPopup();
                }, 500);
            }
        }
    }

    // Initialize the map on page load
    initializeMap();

    // Add a single click listener to the static parent panel
    if (storeListPanel) {
        storeListPanel.addEventListener('click', handleStoreClick);
    }
});