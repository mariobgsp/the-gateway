INSERT INTO "role" (role, created_at, updated_at) VALUES
('admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO "user" (username, password, email, user_role_id, user_session_status, created_at, updated_at) VALUES
('ario_test', '$2a$10$s8CVccrkN0d/eryxRHMz2.2m.YBmYLzRBPedMY34b9l6xyL8I8ru6', 'ario.doe@example.com', 1, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('jane_smith', '$2a$10$s8CVccrkN0d/eryxRHMz2.2m.YBmYLzRBPedMY34b9l6xyL8I8ru6', 'jane.smith@example.com', 2, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO system_properties ("key", "value") VALUES
('TOKEN_SECRET_KEY', '3VZ6lDdJZ/4k8dQ5HgO7w4XwZ8tTrHAlf4A5KJb/VZ8='),
('TOKEN_EXPIRATION', '3600');

INSERT INTO api_gateway (api_name, api_identifier, api_host, api_path, method, header, require_request_body, require_request_param, param, status, created_at, updated_at) VALUES
('Gateway-Example-1', 'gateway-example-1', 'https://api.thecatapi.com', '/v1/images/search', 'GET', 'x-api-key;Content-Type', false, false, NULL, 'created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gateway-Example-2', 'gateway-example-2', 'https://api.thecatapi.com', '/v1/breeds', 'GET', 'Content-Type', false, false, NULL, 'published', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gateway-Cat-Api', 'gateway-catapi', 'https://api.thecatapi.com', '/v1/images/search', 'GET', 'x-api-key;Content-Type', false, true, 'limit;size', 'created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO store_account (store_name, client_id, secret_key) VALUES
('Store-Example-Satu', 'client_store1', 'gw_secret_placeholder_1'),
('Store-Example-Dua', 'client_store2', 'gw_secret_placeholder_2');

INSERT INTO user_store_r (user_id, store_id) VALUES (1, 1), (1, 2);
