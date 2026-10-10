-- P07: catálogo de servicios suscribibles por Marca y suscripciones de consumidores.
CREATE TABLE subscription_service (
    id          UUID          PRIMARY KEY,
    brand_id    UUID          NOT NULL REFERENCES brand (id),
    code        VARCHAR(64)   NOT NULL,
    name        VARCHAR(120)  NOT NULL,
    description VARCHAR(1000),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT subscription_service_code_unique_per_brand UNIQUE (brand_id, code)
);

CREATE TABLE subscription (
    id              UUID        PRIMARY KEY,
    brand_id        UUID        NOT NULL REFERENCES brand (id),
    consumer_id     UUID        NOT NULL REFERENCES consumer (id) ON DELETE CASCADE,
    service_id      UUID        NOT NULL REFERENCES subscription_service (id),
    subscribed_at   TIMESTAMPTZ NOT NULL,
    unsubscribed_at TIMESTAMPTZ,
    CONSTRAINT subscription_dates CHECK (unsubscribed_at IS NULL OR unsubscribed_at >= subscribed_at)
);
-- Como mucho una suscripción activa por consumidor y servicio.
CREATE UNIQUE INDEX subscription_one_active
    ON subscription (consumer_id, service_id) WHERE unsubscribed_at IS NULL;
CREATE INDEX subscription_brand_service_active_idx
    ON subscription (brand_id, service_id) WHERE unsubscribed_at IS NULL;
