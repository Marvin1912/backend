-- Adds the InfluxDB entity_id of the soil moisture sensor associated with a plant.
-- Nullable, no backfill: existing rows keep it unset until the user configures it via PUT /plants.
ALTER TABLE plants.plant
    ADD COLUMN soil_moisture_entity_id VARCHAR(255);
