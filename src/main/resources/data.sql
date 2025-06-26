-- This script seeds the database with initial data.
-- It is run automatically by Spring Boot on application startup.

-- =================================================================
--  Seed the 'stores' table
-- =================================================================
INSERT INTO stores (store_name, store_location) VALUES
                                                    ('DALI Everyday Grocery - Pureza', 'Pureza, Sampaloc, City Of Manila'),
                                                    ('DALI Convenience Store - Katipunan', 'Katipunan Ave, Quezon City'),
                                                    ('DALI Supermart - Makati CBD', 'Ayala Avenue, Makati City')
    ON CONFLICT (store_id) DO NOTHING;


-- =================================================================
--  Seed the 'products' table
-- =================================================================
INSERT INTO products (product_name, product_description, product_price, product_category, product_quantity, image) VALUES
                                                                                                                       ('Frozen Pork Shoulder 500g', 'Versatile and affordable choice for your everyday meals. Perfect for classic dishes like adobo, sinigang, or hearty stews. Tender, flavorful, and conveniently packed.', 199.00, 'Pork', 55, 'frozen-pork-shoulder.png'),
                                                                                                                       ('Beef Cubes for Stew 500g', 'Ideal for slow-cooking, caldereta, or mechado. These tender beef cubes will make your stews rich and flavorful.', 280.00, 'Beef', 40, 'beef-cubes.png'),
                                                                                                                       ('Pork Liempo (Belly) 500g', 'The perfect cut for grilling or frying to a crispy perfection. Marinate it for an unforgettable inihaw na liempo.', 225.50, 'Pork', 75, 'pork-liempo.png'),
                                                                                                                       ('Fresh Chicken Drumsticks 1kg', 'Juicy and meaty, perfect for your favorite fried chicken recipe, grilling, or tinola. A family favorite.', 250.00, 'Chicken', 80, 'chicken-drumstick.png'),
                                                                                                                       ('Choice Ground Beef 500g', 'Versatile ground beef for making burgers, spaghetti sauce, or picadillo. Lean and full of flavor.', 275.00, 'Beef', 60, 'ground-beef.png'),
                                                                                                                       ('Whole Dressed Chicken (1.2kg)', 'A whole chicken ready for roasting, lechon manok style, or for a hearty chicken soup. A versatile centerpiece for any meal.', 290.00, 'Chicken', 30, 'whole-chicken.png')
    ON CONFLICT (product_id) DO NOTHING;

-- =================================================================
--  Seed location data
-- =================================================================
INSERT INTO provinces (province_id, province_name) VALUES (1, 'Cavite') ON CONFLICT (province_name) DO NOTHING;
-- Reset sequence to avoid conflicts if manually inserting
SELECT setval('provinces_province_id_seq', (SELECT MAX(province_id) FROM provinces));

INSERT INTO cities (city_id, province_id, city_name) VALUES (1, 1, 'Imus') ON CONFLICT (province_id, city_name) DO NOTHING;
SELECT setval('cities_city_id_seq', (SELECT MAX(city_id) FROM cities));

INSERT INTO barangays (city_id, barangay_name) VALUES
                                                   (1, 'ALAPAN I-A'), (1, 'ALAPAN I-B'), (1, 'ALAPAN I-C'),
                                                   (1, 'ALAPAN II-A'), (1, 'ALAPAN II-B'),
                                                   (1, 'ANABU I-A'), (1, 'ANABU I-B'), (1, 'ANABU I-C'), (1, 'ANABU I-D'),
                                                   (1, 'ANABU II-A'), (1, 'ANABU II-B'), (1, 'ANABU II-C'), (1, 'ANABU II-D'), (1, 'ANABU II-E'), (1, 'ANABU II-F')
    ON CONFLICT (city_id, barangay_name) DO NOTHING;
SELECT setval('barangays_barangay_id_seq', (SELECT MAX(barangay_id) FROM barangays));


-- =================================================================
--  Seed the 'admin_accounts' table
-- =================================================================
-- Password for this user is 'password'
INSERT INTO admin_accounts (store_id, account_email, password_hash) VALUES
                                                                        (1, 'admin@dali.com', '$2a$10$XAwzdPJuPap2hfdGXWuF0u/mM0rZ8PIEHox9ySOgXrBoP4JibYXPC'),
                                                                        (2, 'admin2@dali.com', '$2a$10$XAwzdPJuPap2hfdGXWuF0u/mM0rZ8PIEHox9ySOgXrBoP4JibYXPC')
    ON CONFLICT (account_email) DO NOTHING;