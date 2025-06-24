-- Drop tables if they exist to ensure a clean slate on each run.
-- This is useful for development.
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS stores CASCADE;

-- Create the 'stores' table
CREATE TABLE stores (
                        store_id        SERIAL PRIMARY KEY,
                        store_name      VARCHAR(255) NOT NULL,
                        store_location  VARCHAR(255)
);

-- Create the 'products' table
CREATE TABLE products (
                          product_id          SERIAL PRIMARY KEY,
                          product_name        VARCHAR(255) NOT NULL,
                          product_description TEXT,
                          product_price       NUMERIC(10, 2) NOT NULL, -- Use NUMERIC for currency
                          product_category    VARCHAR(255),
                          product_quantity    INTEGER NOT NULL,
                          image               VARCHAR(255)
);