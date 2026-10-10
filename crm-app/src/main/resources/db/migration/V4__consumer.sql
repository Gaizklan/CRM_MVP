-- P05: consumidores por Marca, atributos de perfil, preferencias tipadas e historial.
CREATE TABLE consumer (
    id            UUID         PRIMARY KEY,
    brand_id      UUID         NOT NULL REFERENCES brand (id),
    email         VARCHAR(254) NOT NULL,
    phone         VARCHAR(16),
    language      VARCHAR(10)  NOT NULL,
    segment       VARCHAR(32),
    status        VARCHAR(16)  NOT NULL CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN')),
    registered_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT consumer_email_unique_per_brand UNIQUE (brand_id, email)
);
CREATE INDEX consumer_brand_registered_idx ON consumer (brand_id, registered_at DESC);
CREATE INDEX consumer_brand_status_idx ON consumer (brand_id, status);

CREATE TABLE consumer_attribute (
    consumer_id UUID          NOT NULL REFERENCES consumer (id) ON DELETE CASCADE,
    name        VARCHAR(64)   NOT NULL,
    value       VARCHAR(1000) NOT NULL,
    PRIMARY KEY (consumer_id, name)
);

CREATE TABLE consumer_preference (
    consumer_id   UUID           NOT NULL REFERENCES consumer (id) ON DELETE CASCADE,
    pref_key      VARCHAR(64)    NOT NULL,
    pref_type     VARCHAR(16)    NOT NULL CHECK (pref_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'DATE')),
    string_value  VARCHAR(1000),
    number_value  NUMERIC(38, 10),
    boolean_value BOOLEAN,
    date_value    DATE,
    PRIMARY KEY (consumer_id, pref_key),
    CONSTRAINT consumer_preference_typed_value CHECK (
        (pref_type = 'STRING'  AND string_value  IS NOT NULL) OR
        (pref_type = 'NUMBER'  AND number_value  IS NOT NULL) OR
        (pref_type = 'BOOLEAN' AND boolean_value IS NOT NULL) OR
        (pref_type = 'DATE'    AND date_value    IS NOT NULL))
);

CREATE TABLE consumer_preference_history (
    id          UUID          PRIMARY KEY,
    brand_id    UUID          NOT NULL REFERENCES brand (id),
    consumer_id UUID          NOT NULL REFERENCES consumer (id) ON DELETE CASCADE,
    pref_key    VARCHAR(64)   NOT NULL,
    old_type    VARCHAR(16),
    old_value   VARCHAR(1000),
    new_type    VARCHAR(16),
    new_value   VARCHAR(1000),
    changed_by  VARCHAR(128)  NOT NULL,
    changed_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX consumer_preference_history_consumer_idx
    ON consumer_preference_history (brand_id, consumer_id, changed_at DESC);
