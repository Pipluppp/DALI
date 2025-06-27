document.addEventListener('DOMContentLoaded', function () {
    const mapContainer = document.getElementById('stores-map');
    if (mapContainer) {
        // 1. Initialize map centered on the Philippines
        const map = L.map('stores-map').setView([12.8797, 121.7740], 6); // General PH view

        // 2. Add a tile layer (the map background)
        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/attributions">CARTO</a>',
            subdomains: 'abcd',
            maxZoom: 20
        }).addTo(map);

        // 3. Fetch store data from our new API endpoint
        fetch('/api/stores')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(stores => {
                const markers = [];
                // 4. Loop through stores and create markers
                stores.forEach(store => {
                    // Check if latitude and longitude are valid numbers
                    if (store.latitude != null && store.longitude != null) {
                        const marker = L.marker([store.latitude, store.longitude])
                            .bindPopup(`<b>${store.name}</b>`); // Add a popup with the store's name
                        markers.push(marker);
                    }
                });

                // 5. Add markers to a feature group and add to map
                if (markers.length > 0) {
                    const featureGroup = L.featureGroup(markers).addTo(map);
                    // 6. Adjust map view to fit all markers
                    map.fitBounds(featureGroup.getBounds().pad(0.1)); // pad adds a little space around the edges
                }
            })
            .catch(error => {
                console.error('Error fetching or processing store data:', error);
                // Optionally display an error message on the map itself
                mapContainer.innerHTML = '<p style="text-align:center; padding: 20px;">Could not load store locations.</p>';
            });
    }
});