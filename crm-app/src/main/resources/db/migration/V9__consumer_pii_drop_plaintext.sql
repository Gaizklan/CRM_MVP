-- P10 (3/3): PII solo cifrada. Unicidad de email por Marca mediante el índice ciego.
ALTER TABLE consumer ALTER COLUMN email_ciphertext SET NOT NULL;
ALTER TABLE consumer ALTER COLUMN email_hash SET NOT NULL;
ALTER TABLE consumer DROP CONSTRAINT consumer_email_unique_per_brand;
ALTER TABLE consumer DROP COLUMN email;
ALTER TABLE consumer DROP COLUMN phone;
ALTER TABLE consumer ADD CONSTRAINT consumer_email_hash_unique_per_brand UNIQUE (brand_id, email_hash);
