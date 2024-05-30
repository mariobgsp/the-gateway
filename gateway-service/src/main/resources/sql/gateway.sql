create schema if not exists "user";

CREATE TABLE "user"."user" (
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

INSERT INTO "user"."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('ario_test', 'password123', 'ario.doe@example.com', 1, 'active', '2024-05-21 08:00:00', '2024-05-21 08:00:00', '2024-05-21 08:00:00', '2024-05-21 08:00:00');
INSERT INTO "user"."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('jane_smith', 'securepassword', 'jane.smith@example.com', 2, 'active', '2024-05-22 10:30:00', '2024-05-22 10:30:00', '2024-05-22 10:30:00', '2024-05-22 10:30:00');
INSERT INTO "user"."user" (username, password, email, user_role_id, user_session_status, user_last_login, user_last_change_password, created_at, updated_at) VALUES ('bob_johnson', 'p@ssw0rd', 'bob.johnson@example.com', 1, 'active', '2024-05-23 12:45:00', NULL, '2024-05-23 12:45:00', '2024-05-23 12:45:00');

CREATE TABLE "user"."role" (
    id SERIAL PRIMARY KEY,
    role varchar(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO "user"."role" (role, created_at, updated_at) VALUES
('admin', NOW(), NOW()),
('user', NOW(), NOW());

CREATE TABLE "user".user_log (
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

CREATE TABLE "user".system_properties (
    id SERIAL PRIMARY KEY,
    key varchar(255),
    value varchar(255)
);

alter table "user".system_properties
add column description varchar(255);

INSERT INTO "user".system_properties ("key", value) VALUES('admin_secret_key', '3VZ6lDdJZ/4k8dQ5HgO7w4XwZ8tTrHAlf4A5KJb/VZ8=');
INSERT INTO "user".system_properties ("key", value) VALUES('user_secret_key', 'Xfh7NxjW2D5BfgZzWfZpSj9KZv7OeVvL5jQy4UO4lGg=');
INSERT INTO "user".system_properties ("key", value) VALUES('user_session_time', '600');
INSERT INTO "user".system_properties ("key", value) VALUES('admin_session_time', '1800');
INSERT INTO "user".system_properties ("key", value) VALUES('login_activity_name', 'LOGIN');
INSERT INTO "user".system_properties ("key", value) VALUES('active_session_status', 'ACTIVE');
INSERT INTO "user".system_properties ("key", value) VALUES('failed_session_status', 'FAILED-LOGIN');
INSERT INTO "user".system_properties ("key", value) VALUES('inactive_session_status', 'INACTIVE');
INSERT INTO "user".system_properties ("key", value) VALUES('logout_activity_name', 'LOGOUT');

ALTER TABLE "user"."user"
ADD CONSTRAINT fk_role
FOREIGN KEY (user_role_id)
REFERENCES "user"."role" (id);

ALTER TABLE "user".user_log
ADD CONSTRAINT fk_user
FOREIGN KEY (user_id)
REFERENCES "user"."user" (id);