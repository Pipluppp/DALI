-- Drop tables if they exist to ensure a clean slate on each run.
-- This is useful for development.
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
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

-- Table 3: accounts
CREATE TABLE accounts (
                          account_id          SERIAL PRIMARY KEY,
                          account_first_name  VARCHAR(255),
                          account_last_name   VARCHAR(255),
                          account_email       VARCHAR(255) UNIQUE NOT NULL,
                          password_hash       VARCHAR(255) NOT NULL,
                          phone_number        VARCHAR(50)
);

-- Table 8: addresses
CREATE TABLE addresses (
                           address_id      SERIAL PRIMARY KEY,
                           account_id      INTEGER NOT NULL REFERENCES accounts(account_id),
                           province        VARCHAR(255),
                           city            VARCHAR(255),
                           barangay        VARCHAR(255),
                           additional_info VARCHAR(1024),
                           phone_number    VARCHAR(50),
                           created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           is_default      BOOLEAN DEFAULT FALSE
);