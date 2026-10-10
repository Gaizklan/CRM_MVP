-- P03: catálogo de Marcas (tenants). Toda entidad de negocio referenciará brand(id) en brand_id.
CREATE TABLE brand (
    id               UUID         PRIMARY KEY,
    code             VARCHAR(32)  NOT NULL UNIQUE,
    name             VARCHAR(120) NOT NULL,
    domain           VARCHAR(255) UNIQUE,
    default_language VARCHAR(10)  NOT NULL,
    status           VARCHAR(16)  NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
