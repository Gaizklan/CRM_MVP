-- P11: outbox transaccional de eventos de dominio (tabla de plataforma).
CREATE TABLE domain_event_outbox (
    id           UUID          PRIMARY KEY,
    seq          BIGSERIAL     NOT NULL UNIQUE,
    brand_id     UUID          NOT NULL REFERENCES brand (id),
    event_type   VARCHAR(255)  NOT NULL,
    payload      JSONB         NOT NULL,
    occurred_at  TIMESTAMPTZ   NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts     INT           NOT NULL DEFAULT 0,
    last_error   VARCHAR(2000)
);
CREATE INDEX domain_event_outbox_pending_idx ON domain_event_outbox (seq) WHERE published_at IS NULL;

-- P11: eventos de usuario (crm-tracking). consumer_id es una referencia lógica al módulo de
-- consumidores (sin FK entre módulos).
CREATE TABLE user_event (
    id              UUID        PRIMARY KEY,
    brand_id        UUID        NOT NULL REFERENCES brand (id),
    consumer_id     UUID        NOT NULL,
    type            VARCHAR(32) NOT NULL CHECK (type IN ('LOGIN', 'LOGOUT', 'SERVICE_CLICK',
                        'EXPERIENCE_CHANGED', 'EMAIL_SENT', 'EMAIL_OPENED', 'EMAIL_CLICKED',
                        'PREFERENCES_CHANGED')),
    occurred_at     TIMESTAMPTZ NOT NULL,
    attributes      JSONB       NOT NULL DEFAULT '{}'::jsonb,
    source_event_id UUID        UNIQUE,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX user_event_consumer_type_time_idx
    ON user_event (brand_id, consumer_id, type, occurred_at DESC);
CREATE INDEX user_event_brand_type_time_idx ON user_event (brand_id, type, occurred_at DESC);
