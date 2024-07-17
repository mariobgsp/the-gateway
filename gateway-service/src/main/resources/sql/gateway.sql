
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

INSERT INTO public."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('ario_test', 'password123', 'ario.doe@example.com', 1, 'active', '2024-05-21 08:00:00', '2024-05-21 08:00:00', '2024-05-21 08:00:00', '2024-05-21 08:00:00');
INSERT INTO public."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('jane_smith', 'securepassword', 'jane.smith@example.com', 2, 'active', '2024-05-22 10:30:00', '2024-05-22 10:30:00', '2024-05-22 10:30:00', '2024-05-22 10:30:00');
INSERT INTO public."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('bob_johnson', 'p@ssw0rd', 'bob.johnson@example.com', 1, 'active', '2024-05-23 12:45:00', NULL, '2024-05-23 12:45:00', '2024-05-23 12:45:00');

CREATE TABLE public."role" (
    id SERIAL PRIMARY KEY,
    role varchar(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO public."role" (role, created_at, updated_at) VALUES
('admin', NOW(), NOW()),
('user', NOW(), NOW());

CREATE TABLE public.user_log (
    id SERIAL PRIMARY KEY,
    user_id bigint,
    user_activity_name varchar(255),
    user_session_status varchar(20),
    user_session varchar(255),
    user_token varchar(255),
    error_message varchar(255),
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

INSERT INTO public.system_properties ("key", value) VALUES('admin_secret_key', '3VZ6lDdJZ/4k8dQ5HgO7w4XwZ8tTrHAlf4A5KJb/VZ8=');
INSERT INTO public.system_properties ("key", value) VALUES('user_secret_key', 'Xfh7NxjW2D5BfgZzWfZpSj9KZv7OeVvL5jQy4UO4lGg=');
INSERT INTO public.system_properties ("key", value) VALUES('user_session_time', '600');
INSERT INTO public.system_properties ("key", value) VALUES('admin_session_time', '1800');
INSERT INTO public.system_properties ("key", value) VALUES('login_activity_name', 'LOGIN');
INSERT INTO public.system_properties ("key", value) VALUES('active_session_status', 'ACTIVE');
INSERT INTO public.system_properties ("key", value) VALUES('failed_session_status', 'FAILED-LOGIN');
INSERT INTO public.system_properties ("key", value) VALUES('inactive_session_status', 'INACTIVE');
INSERT INTO public.system_properties ("key", value) VALUES('logout_activity_name', 'LOGOUT');

ALTER TABLE public."user" ADD CONSTRAINT fk_role FOREIGN KEY (user_role_id) REFERENCES public."role" (id);

ALTER TABLE public.user_log
ADD CONSTRAINT fk_user
FOREIGN KEY (user_id)
REFERENCES public."user" (id);

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

CREATE TABLE public.gateway (
    id SERIAL PRIMARY KEY,
    api_name varchar(255), -- Example API name
    api_host varchar(255), -- https://localhost:8080
    api_path varchar(255), -- /examplePath1
    method varchar(20), --PUT, POST, GET, delete etc
    header varchar(255), --header: ax-request-id;Content type, etc it will be splitted and inputed accordingly
    is_request_body varchar(10), -- yes or no
    is_request_param varchar(10), -- yes or no
    param varchar(255), -- param: phoneNumber;requestId; etch it will be splitted and inputed accordingly
    store_id bigint,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE public.gateway
ADD CONSTRAINT fk_store_id
FOREIGN KEY (store_id)
REFERENCES public.store_account (id);

INSERT INTO public.gateway (api_name, api_host, api_path, method, header, is_request_body, is_request_param, param, created_at, updated_at) VALUES
('Example API 1', 'https://localhost:8080', '/examplePath1', 'POST', 'ax-request-id;Content-Type', 'yes', 'no', 'phoneNumber;requestId', NOW(), NOW()),
('Example API 2', 'https://localhost:8081', '/examplePath2', 'GET', 'auth-token;Content-Type', 'no', 'yes', 'userId;sessionId', NOW(), NOW());

--select * from public.gateway
