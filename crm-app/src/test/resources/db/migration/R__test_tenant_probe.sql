-- Solo tests: tabla de una entidad con Marca usada para verificar el aislamiento multimarca.
CREATE TABLE IF NOT EXISTS test_tenant_probe (
    id       UUID         PRIMARY KEY,
    brand_id UUID         NOT NULL REFERENCES brand (id),
    label    VARCHAR(64)  NOT NULL
);
