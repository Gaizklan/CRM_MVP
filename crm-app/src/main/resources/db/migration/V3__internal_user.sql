-- P04: usuarios internos (tabla de plataforma, no filtrada por Marca).
CREATE TABLE internal_user (
    id            UUID         PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    level         VARCHAR(16)  NOT NULL CHECK (level IN ('ADMIN', 'GLOBAL_USER', 'LOCAL_USER')),
    brand_id      UUID         REFERENCES brand (id),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT internal_user_brand_by_level CHECK (
        (level = 'LOCAL_USER' AND brand_id IS NOT NULL) OR
        (level <> 'LOCAL_USER' AND brand_id IS NULL))
);

CREATE TABLE internal_user_role (
    user_id UUID        NOT NULL REFERENCES internal_user (id) ON DELETE CASCADE,
    role    VARCHAR(32) NOT NULL CHECK (role IN ('CRM_MANAGER', 'PRODUCER', 'CONTENT_REVIEWER', 'DEVELOPER')),
    PRIMARY KEY (user_id, role)
);
