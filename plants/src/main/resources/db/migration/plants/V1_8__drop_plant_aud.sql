-- Issue #190: remove Hibernate Envers auditing from the plants module.
-- The plant_aud table is no longer written to; safe to drop.
DROP TABLE IF EXISTS plants.plant_aud;
