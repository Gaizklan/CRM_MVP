-- P10 (1/3): columnas cifradas de PII e índice ciego del email. V8 (Java) cifra los datos
-- existentes y V9 elimina las columnas en claro.
ALTER TABLE consumer ADD COLUMN email_ciphertext VARCHAR(512);
ALTER TABLE consumer ADD COLUMN email_hash       VARCHAR(64);
ALTER TABLE consumer ADD COLUMN phone_ciphertext VARCHAR(512);
