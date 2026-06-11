-- Enforce the single-row invariant on the nutrition profile.
-- Collapse any accidental duplicates, pin the surviving row to the fixed id 1,
-- and forbid any other id so concurrent inserts can no longer create a second row.
DELETE FROM nutrition.profile
WHERE id <> (SELECT MIN(id) FROM nutrition.profile);

UPDATE nutrition.profile SET id = 1 WHERE id <> 1;

ALTER TABLE nutrition.profile ALTER COLUMN id DROP DEFAULT;

ALTER TABLE nutrition.profile ADD CONSTRAINT profile_single_row CHECK (id = 1);
