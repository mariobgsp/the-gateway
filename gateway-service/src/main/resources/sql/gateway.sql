INSERT INTO public."user" (username, "password", email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES('jane_smith', '$2a$10$AnSG7YgfV.xXG18TReFdx.4g.7aoDgn/YZYX1x4nIoagYpO/ctYyq', 'jane.smith@example.com', 2, 'active', '2024-05-22 10:30:00.000', '2024-05-22 10:30:00.000', '2024-05-22 10:30:00.000', '2024-05-22 10:30:00.000');
INSERT INTO public."user" (username, "password", email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES('bob_johnson', '$2a$10$Fyzx9YTta6v/hUIE2Ze4jOYR4tNbLW/Ea6YnYgvnZ38y4KXP/qIAe', 'bob.johnson@example.com', 1, 'active', '2024-05-23 12:45:00.000', NULL, '2024-05-23 12:45:00.000', '2024-05-23 12:45:00.000');
INSERT INTO public."user" (username, "password", email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES('ario_test', '$2a$10$db.qhjUpDeOgc249ziI2oepgmTMHKrT6YYv276Lh4mN1U7zvwcija', 'ario.doe@example.com', 1, 'ACTIVE', '2024-07-17 16:14:01.516', '2024-05-21 08:00:00.000', '2024-05-21 08:00:00.000', '2024-07-17 16:14:01.598');

INSERT INTO public."role" (role, created_at, updated_at) VALUES
('admin', NOW(), NOW()),
('user', NOW(), NOW());

INSERT INTO public.system_properties ("key", value, vgroup) VALUES('TOKEN_SECRET_KEY', '3VZ6lDdJZ/4k8dQ5HgO7w4XwZ8tTrHAlf4A5KJb/VZ8=', 'gateway-services');
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('TOKEN_EXPIRATION', '3600', 'gateway-services');
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('TOKEN_ACCESS_ACTIVE_STATUS', 'ACTIVE', 'gateway-services');
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('TOKEN_ACCESS_INACTIVE_STATUS', 'INACTIVE', 'gateway-services');
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('API_EXPIRATION_TIME', '86400', 'gateway-services');
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('APP_NAME', 'gateway-services', 'gateway-services');

INSERT INTO public.api_gateway (api_name, api_identifier, api_host, api_path, method, header, require_request_body, require_request_param, param, created_at, updated_at) VALUES
('Gateway-Example-1','gateway-example-1', 'https://localhost:8080', '/examplePath1', 'POST', 'ax-request-id;Content-Type', true, false, 'phoneNumber;requestId', NOW(), NOW()),
('Gateway-Example-1','gateway-example-1', 'https://localhost:8081', '/examplePath2', 'GET', 'x-auth-token;Content-Type', false, true, 'userId;sessionId', NOW(), NOW()),
('Gateway-CatApi','gateway-catapi', 'https://api.thecatapi.com', '/v1/images/search', 'GET', 'x-api-key;Content-Type', false, false, null, NOW(), NOW());
