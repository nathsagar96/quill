ALTER TABLE users
    ADD COLUMN enabled            BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verification_token UUID;
