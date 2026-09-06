-- Adds the soil moisture threshold (in %) below which a plant is considered to need water,
-- and a per-plant switch to enable/disable that check independently of the threshold value
-- (e.g. while a sensor is temporarily broken/uninstalled). Both nullable, no backfill:
-- a null soil_moisture_check_enabled is treated as "enabled" by the application, and a null
-- soil_moisture_threshold means the moisture check is skipped in favor of the existing
-- watering_frequency schedule alone.
ALTER TABLE plants.plant
    ADD COLUMN soil_moisture_threshold DOUBLE PRECISION;
ALTER TABLE plants.plant
    ADD COLUMN soil_moisture_check_enabled BOOLEAN DEFAULT true;
