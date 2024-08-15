
CREATE TABLE public."user" (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    user_role_id bigint,
    user_session_status VARCHAR(20),
    user_last_login TIMESTAMP,
    user_last_change_password TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

--INSERT INTO public."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('ario_test', 'password123', 'ario.doe@example.com', 1, 'active', '2024-05-21 08:00:00', '2024-05-21 08:00:00', '2024-05-21 08:00:00', '2024-05-21 08:00:00');
--INSERT INTO public."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('jane_smith', 'securepassword', 'jane.smith@example.com', 2, 'active', '2024-05-22 10:30:00', '2024-05-22 10:30:00', '2024-05-22 10:30:00', '2024-05-22 10:30:00');
--INSERT INTO public."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('bob_johnson', 'p@ssw0rd', 'bob.johnson@example.com', 1, 'active', '2024-05-23 12:45:00', NULL, '2024-05-23 12:45:00', '2024-05-23 12:45:00');

INSERT INTO public."user" (id, username, "password", email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES(2, 'jane_smith', '$2a$10$AnSG7YgfV.xXG18TReFdx.4g.7aoDgn/YZYX1x4nIoagYpO/ctYyq', 'jane.smith@example.com', 2, 'active', '2024-05-22 10:30:00.000', '2024-05-22 10:30:00.000', '2024-05-22 10:30:00.000', '2024-05-22 10:30:00.000');
INSERT INTO public."user" (id, username, "password", email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES(3, 'bob_johnson', '$2a$10$Fyzx9YTta6v/hUIE2Ze4jOYR4tNbLW/Ea6YnYgvnZ38y4KXP/qIAe', 'bob.johnson@example.com', 1, 'active', '2024-05-23 12:45:00.000', NULL, '2024-05-23 12:45:00.000', '2024-05-23 12:45:00.000');
INSERT INTO public."user" (id, username, "password", email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES(1, 'ario_test', '$2a$10$db.qhjUpDeOgc249ziI2oepgmTMHKrT6YYv276Lh4mN1U7zvwcija', 'ario.doe@example.com', 1, 'ACTIVE', '2024-07-17 16:14:01.516', '2024-05-21 08:00:00.000', '2024-05-21 08:00:00.000', '2024-07-17 16:14:01.598');

CREATE TABLE public."role" (
    id SERIAL PRIMARY KEY,
    role varchar(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO public."role" (role, created_at, updated_at) VALUES
('admin', NOW(), NOW()),
('user', NOW(), NOW());

CREATE TABLE public.token_log (
    id SERIAL PRIMARY KEY,
    token varchar(255),
    status varchar(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE public.system_properties (
    id SERIAL PRIMARY KEY,
    key varchar(255),
    value varchar(255)
);

alter table public.system_properties
add column description varchar(255);

alter table public.system_properties
add column vgroup varchar(255);

INSERT INTO public.system_properties ("key", value, vgroup) VALUES('TOKEN_SECRET_KEY', '3VZ6lDdJZ/4k8dQ5HgO7w4XwZ8tTrHAlf4A5KJb/VZ8=', "gateway-services");
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('TOKEN_EXPIRATION', '3600', "gateway-services");
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('TOKEN_ACCESS_ACTIVE_STATUS', 'ACTIVE', "gateway-services");
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('TOKEN_ACCESS_INACTIVE_STATUS', 'INACTIVE', "gateway-services");
INSERT INTO public.system_properties ("key", value, vgroup) VALUES('API_EXPIRATION_TIME', '86400', "gateway-services");

ALTER TABLE public."user" ADD CONSTRAINT fk_role FOREIGN KEY (user_role_id) REFERENCES public."role" (id);

ALTER TABLE public.user_log
ADD CONSTRAINT fk_user
FOREIGN KEY (user_id)
REFERENCES public."user" (id);

drop table public.store_account;
create table public.store_account(
	id SERIAL primary key,
	store_name varchar(255),
	secretKey varchar(255),
	clientId varchar(255)
)

create table public.user_store_r(
	user_id bigint,
	store_id bigint
)

ALTER TABLE public.user_store_r
ADD CONSTRAINT fk_user_id
FOREIGN KEY (user_id)
REFERENCES public."user" (id);

ALTER TABLE public.user_store_r
ADD CONSTRAINT fk_store_id
FOREIGN KEY (store_id)
REFERENCES public.store_account (id);

drop table public.api_gateway;

select * from public.api_gateway ag ;
CREATE TABLE public.api_gateway (
    id SERIAL PRIMARY KEY,
    api_name varchar(255), -- Example API name
    api_identifier varchar(255),
    api_host varchar(255), -- https://localhost:8080
    api_path varchar(255), -- /examplePath1
    method varchar(20), --PUT, POST, GET, delete etc
    header varchar(255), --header: ax-request-id;Content-type, etc it will be splitted and inputed accordingly
    require_request_body boolean, -- yes or no
    require_request_param boolean, -- yes or no
    param varchar(255), -- param: phoneNumber;requestId; etch it will be splitted and inputed accordingly
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO public.api_gateway (api_name, api_identifier, api_host, api_path, method, header, require_request_body, require_request_param, param, created_at, updated_at) VALUES 
('Gateway-Example-1','gateway-example-1', 'https://localhost:8080', '/examplePath1', 'POST', 'ax-request-id;Content-Type', true, false, 'phoneNumber;requestId', NOW(), NOW()), 
('Gateway-Example-1','gateway-example-1', 'https://localhost:8081', '/examplePath2', 'GET', 'x-auth-token;Content-Type', false, true, 'userId;sessionId', NOW(), NOW()),
('Gateway-CatApi','gateway-catapi', 'https://api.thecatapi.com', '/v1/images/search', 'GET', 'x-api-key;Content-Type', false, false, null, NOW(), NOW());

--select * from public.gateway
