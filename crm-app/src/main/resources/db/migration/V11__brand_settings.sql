-- P12: configuración de negocio por Marca (documento JSON validado por la aplicación).
CREATE TABLE brand_settings (
    brand_id   UUID         PRIMARY KEY REFERENCES brand (id) ON DELETE CASCADE,
    settings   JSONB        NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);
