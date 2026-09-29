-- Buyer extends the common BaseEntity (id, createdAt, updatedAt), but V1 only gave the buyers
-- table a created_at column. This backfills the missing updated_at column so the schema
-- matches what Hibernate's schema validator expects from the entity.
ALTER TABLE buyers ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();