--liquibase formatted sql

--changeset garagepilot:005-fix-composite-unique-constraints dbms:postgresql
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_garage_email;
ALTER TABLE users ADD CONSTRAINT uk_users_garage_email UNIQUE (garage_id, email);

ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS uk_vehicles_garage_plate;
ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_garage_plate UNIQUE (garage_id, license_plate);

--rollback ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_garage_email;
--rollback ALTER TABLE users ADD CONSTRAINT uk_users_garage_email UNIQUE (garage_id);
--rollback ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS uk_vehicles_garage_plate;
--rollback ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_garage_plate UNIQUE (garage_id);
