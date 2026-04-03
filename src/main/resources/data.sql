-- =====================
-- FX Rates
-- =====================
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('NGN_USD', 0.00065, NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('USD_GBP', 0.79,    NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('USD_EUR', 0.92,    NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('USD_NGN', 1538.0,  NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('EUR_GBP', 0.86,    NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('GBP_EUR', 1.16,    NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('EUR_USD', 1.087,   NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('GBP_USD', 1.265,   NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('NGN_GBP', 0.00051, NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('USD_USD', 1.00,    NOW());
INSERT INTO fx_rates (currency_pair, rate, last_updated) VALUES ('NGN_NGN', 1.00,    NOW());

-- =====================
-- Liquidity Pools
-- =====================
INSERT INTO liquidity_pool (currency, available_balance) VALUES ('USD', 5000000.00);
INSERT INTO liquidity_pool (currency, available_balance) VALUES ('GBP', 3000000.00);
INSERT INTO liquidity_pool (currency, available_balance) VALUES ('EUR', 4000000.00);
INSERT INTO liquidity_pool (currency, available_balance) VALUES ('NGN', 2000000000.00);

-- =====================
-- Banks (SWIFT network nodes)
-- =====================
INSERT INTO bank (bic, name, country, currency) VALUES ('GTBINGLA', 'Guaranty Trust Bank',       'Nigeria',        'NGN');
INSERT INTO bank (bic, name, country, currency) VALUES ('ZENITHNG', 'Zenith Bank',                'Nigeria',        'NGN');
INSERT INTO bank (bic, name, country, currency) VALUES ('CITIVUS33', 'Citibank N.A. (USD Hub)',   'United States',  'USD');
INSERT INTO bank (bic, name, country, currency) VALUES ('CHASUS33',  'JPMorgan Chase',            'United States',  'USD');
INSERT INTO bank (bic, name, country, currency) VALUES ('BARCGB22',  'Barclays Bank UK',          'United Kingdom', 'GBP');
INSERT INTO bank (bic, name, country, currency) VALUES ('HSBCGB2L',  'HSBC UK',                   'United Kingdom', 'GBP');
INSERT INTO bank (bic, name, country, currency) VALUES ('DEUTDEDB',  'Deutsche Bank',             'Germany',        'EUR');
INSERT INTO bank (bic, name, country, currency) VALUES ('BNPAFRPP',  'BNP Paribas',               'France',         'EUR');

-- =====================
-- Nostro / Vostro Accounts
-- (ownerBic holds an account at correspondentBic)
-- =====================
-- GTBank's USD nostro at Citibank
INSERT INTO nostro_vostro_account (id, owner_bic, correspondent_bic, currency, balance, account_type)
    VALUES (RANDOM_UUID(), 'GTBINGLA', 'CITIVUS33', 'USD', 2000000.00, 'NOSTRO');

-- GTBank's GBP nostro at Barclays
INSERT INTO nostro_vostro_account (id, owner_bic, correspondent_bic, currency, balance, account_type)
    VALUES (RANDOM_UUID(), 'GTBINGLA', 'BARCGB22', 'GBP', 1000000.00, 'NOSTRO');

-- Zenith's USD nostro at JPMorgan
INSERT INTO nostro_vostro_account (id, owner_bic, correspondent_bic, currency, balance, account_type)
    VALUES (RANDOM_UUID(), 'ZENITHNG', 'CHASUS33', 'USD', 1500000.00, 'NOSTRO');

-- Citibank's GBP nostro at Barclays
INSERT INTO nostro_vostro_account (id, owner_bic, correspondent_bic, currency, balance, account_type)
    VALUES (RANDOM_UUID(), 'CITIVUS33', 'BARCGB22', 'GBP', 3000000.00, 'NOSTRO');

-- Citibank's EUR nostro at Deutsche Bank
INSERT INTO nostro_vostro_account (id, owner_bic, correspondent_bic, currency, balance, account_type)
    VALUES (RANDOM_UUID(), 'CITIVUS33', 'DEUTDEDB', 'EUR', 2500000.00, 'NOSTRO');

-- JPMorgan's GBP nostro at HSBC
INSERT INTO nostro_vostro_account (id, owner_bic, correspondent_bic, currency, balance, account_type)
    VALUES (RANDOM_UUID(), 'CHASUS33', 'HSBCGB2L', 'GBP', 2000000.00, 'NOSTRO');

-- Barclays' EUR nostro at BNP Paribas
INSERT INTO nostro_vostro_account (id, owner_bic, correspondent_bic, currency, balance, account_type)
    VALUES (RANDOM_UUID(), 'BARCGB22', 'BNPAFRPP', 'EUR', 1800000.00, 'NOSTRO');

-- Vostro mirror: Citibank holds GTBank's funds (vostro from Citi's perspective)
INSERT INTO nostro_vostro_account (id, owner_bic, correspondent_bic, currency, balance, account_type)
    VALUES (RANDOM_UUID(), 'CITIVUS33', 'GTBINGLA', 'NGN', 500000000.00, 'VOSTRO');
