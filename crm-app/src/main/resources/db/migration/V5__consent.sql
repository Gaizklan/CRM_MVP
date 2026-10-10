-- P06: consentimientos opt-in/opt-out por canal, con registro de auditoría inmutable.
CREATE TABLE consumer_consent (
    consumer_id UUID        NOT NULL REFERENCES consumer (id) ON DELETE CASCADE,
    channel     VARCHAR(16) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'MMS', 'PUSH', 'SUBSCRIPTIONS', 'CRM_PROGRAM')),
    brand_id    UUID        NOT NULL REFERENCES brand (id),
    granted     BOOLEAN     NOT NULL,
    source      VARCHAR(16) NOT NULL CHECK (source IN ('WEB', 'EMAIL', 'ADMIN', 'IMPORT')),
    changed_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_id, channel)
);
CREATE INDEX consumer_consent_brand_channel_idx ON consumer_consent (brand_id, channel, granted);

CREATE TABLE consumer_consent_log (
    id          UUID         PRIMARY KEY,
    brand_id    UUID         NOT NULL REFERENCES brand (id),
    consumer_id UUID         NOT NULL REFERENCES consumer (id),
    channel     VARCHAR(16)  NOT NULL,
    granted     BOOLEAN      NOT NULL,
    source      VARCHAR(16)  NOT NULL CHECK (source IN ('WEB', 'EMAIL', 'ADMIN', 'IMPORT')),
    actor       VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX consumer_consent_log_consumer_idx ON consumer_consent_log (brand_id, consumer_id, occurred_at DESC);

-- El registro de auditoría es solo de inserción.
CREATE FUNCTION consumer_consent_log_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'consumer_consent_log es un registro de auditoría inmutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER consumer_consent_log_no_update_delete
    BEFORE UPDATE OR DELETE ON consumer_consent_log
    FOR EACH ROW EXECUTE FUNCTION consumer_consent_log_immutable();
