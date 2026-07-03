-- Move the "Ernährungsplan & Einkaufsliste" meal-plan/shopping-list content from a static
-- classpath JSON resource into fully normalized relational tables, seeded with the current
-- content of nutrition/src/main/resources/nutrition/meal-plan.json.

CREATE TABLE nutrition.meal_plan
(
    id                     BIGINT        PRIMARY KEY CHECK (id = 1),
    eyebrow                VARCHAR(500)  NOT NULL,
    title                  VARCHAR(500)  NOT NULL,
    description            TEXT          NOT NULL,
    shopping_list_title    VARCHAR(500)  NOT NULL,
    shopping_list_note     VARCHAR(500)  NOT NULL,
    shopping_list_callout  TEXT,
    footer_note            TEXT          NOT NULL,
    creation_date          TIMESTAMP     NOT NULL,
    last_modified          TIMESTAMP     NOT NULL
);

CREATE TABLE nutrition.meal_plan_stat
(
    id            UUID          PRIMARY KEY,
    meal_plan_id  BIGINT        NOT NULL REFERENCES nutrition.meal_plan(id) ON DELETE CASCADE,
    label         VARCHAR(255)  NOT NULL,
    value         VARCHAR(255)  NOT NULL,
    sort_order    INTEGER       NOT NULL,
    creation_date TIMESTAMP     NOT NULL,
    last_modified TIMESTAMP     NOT NULL
);

CREATE INDEX idx_meal_plan_stat_meal_plan_id ON nutrition.meal_plan_stat(meal_plan_id);

CREATE TABLE nutrition.meal_plan_changelog_entry
(
    id            UUID          PRIMARY KEY,
    meal_plan_id  BIGINT        NOT NULL REFERENCES nutrition.meal_plan(id) ON DELETE CASCADE,
    tag           VARCHAR(255)  NOT NULL,
    was           VARCHAR(500),
    entry_text    TEXT          NOT NULL,
    sort_order    INTEGER       NOT NULL,
    creation_date TIMESTAMP     NOT NULL,
    last_modified TIMESTAMP     NOT NULL
);

CREATE INDEX idx_meal_plan_changelog_entry_meal_plan_id ON nutrition.meal_plan_changelog_entry(meal_plan_id);

CREATE TABLE nutrition.meal_plan_section
(
    id              UUID          PRIMARY KEY,
    meal_plan_id    BIGINT        NOT NULL REFERENCES nutrition.meal_plan(id) ON DELETE CASCADE,
    title           VARCHAR(500)  NOT NULL,
    note            VARCHAR(500)  NOT NULL,
    totals_label    VARCHAR(255),
    totals_kcal     VARCHAR(255),
    totals_protein  VARCHAR(255),
    callout         TEXT,
    sort_order      INTEGER       NOT NULL,
    creation_date   TIMESTAMP     NOT NULL,
    last_modified   TIMESTAMP     NOT NULL
);

CREATE INDEX idx_meal_plan_section_meal_plan_id ON nutrition.meal_plan_section(meal_plan_id);

CREATE TABLE nutrition.meal_plan_row
(
    id                    UUID          PRIMARY KEY,
    meal_plan_section_id  UUID          NOT NULL REFERENCES nutrition.meal_plan_section(id) ON DELETE CASCADE,
    meal                  VARCHAR(255)  NOT NULL,
    details               TEXT          NOT NULL,
    qty                   VARCHAR(255)  NOT NULL,
    kcal                  VARCHAR(255)  NOT NULL,
    protein               VARCHAR(255)  NOT NULL,
    sort_order            INTEGER       NOT NULL,
    creation_date         TIMESTAMP     NOT NULL,
    last_modified         TIMESTAMP     NOT NULL
);

CREATE INDEX idx_meal_plan_row_meal_plan_section_id ON nutrition.meal_plan_row(meal_plan_section_id);

CREATE TABLE nutrition.meal_plan_shopping_category
(
    id            UUID          PRIMARY KEY,
    meal_plan_id  BIGINT        NOT NULL REFERENCES nutrition.meal_plan(id) ON DELETE CASCADE,
    title         VARCHAR(500)  NOT NULL,
    sort_order    INTEGER       NOT NULL,
    creation_date TIMESTAMP     NOT NULL,
    last_modified TIMESTAMP     NOT NULL
);

CREATE INDEX idx_meal_plan_shopping_category_meal_plan_id ON nutrition.meal_plan_shopping_category(meal_plan_id);

CREATE TABLE nutrition.meal_plan_shopping_item
(
    id                              UUID          PRIMARY KEY,
    meal_plan_shopping_category_id UUID          NOT NULL REFERENCES nutrition.meal_plan_shopping_category(id) ON DELETE CASCADE,
    name                            VARCHAR(255)  NOT NULL,
    brand                           VARCHAR(500),
    badge                           VARCHAR(10)   CHECK (badge IN ('ok', 'warn') OR badge IS NULL),
    badge_text                      VARCHAR(500),
    qty                             VARCHAR(255)  NOT NULL,
    sort_order                      INTEGER       NOT NULL,
    creation_date                   TIMESTAMP     NOT NULL,
    last_modified                   TIMESTAMP     NOT NULL
);

CREATE INDEX idx_meal_plan_shopping_item_category_id ON nutrition.meal_plan_shopping_item(meal_plan_shopping_category_id);

CREATE TABLE nutrition.meal_plan_source
(
    id            UUID          PRIMARY KEY,
    meal_plan_id  BIGINT        NOT NULL REFERENCES nutrition.meal_plan(id) ON DELETE CASCADE,
    label         VARCHAR(500)  NOT NULL,
    url           VARCHAR(1000) NOT NULL,
    sort_order    INTEGER       NOT NULL,
    creation_date TIMESTAMP     NOT NULL,
    last_modified TIMESTAMP     NOT NULL
);

CREATE INDEX idx_meal_plan_source_meal_plan_id ON nutrition.meal_plan_source(meal_plan_id);

-- Seed data: header row (singleton, id = 1)
INSERT INTO nutrition.meal_plan
    (id, eyebrow, title, description, shopping_list_title, shopping_list_note, shopping_list_callout, footer_note,
     creation_date, last_modified)
VALUES
    (1,
     'Version 3 — Ei & Magerquark aus dem Frühstück ins Abendessen verschoben',
     'Ernährungsplan & Einkaufsliste',
     'Fettabbau & Muskelerhalt (Cut) · Whey reduziert, Magerquark als Hauptproteinquelle, Ei kontrolliert statt ad-hoc.',
     '4 · Einkaufsliste für Lidl (1 Woche)',
     '4× Kantine Mo–Do · 3× Selbstkochen Fr–So · 40 g Whey/Tag',
     'Whey Protein wird bei Lidl nicht durchgängig geführt — separat online oder in der Drogerie besorgen. Alle anderen Positionen entsprechen den Produkten, die bereits im Tracking als Lidl-Artikel hinterlegt sind (Freshona, Milbona, Alesto, Bio-Eigenmarken); Basmatireis- und Magerquark-Nährwerte wurden ergänzend recherchiert, da sie bisher nicht im Tracking vorkamen.',
     'Nährwerte für Haferflocken, Haferdrink, Whey, Himbeeren, Hähnchenbrust, Rinderhüftsteak, Dinkel-Penne, Kaisergemüse und Ei stammen aus der bestehenden Lebensmitteldatenbank des Nutrition-Trackings. Magerquark, Basmatireis und Banane waren dort nicht erfasst und wurden recherchiert:',
     now(), now());

-- Seed data: stats (4)
INSERT INTO nutrition.meal_plan_stat (id, meal_plan_id, label, value, sort_order, creation_date, last_modified)
VALUES
    ('d74eba94-2b41-4fd9-8fa4-2d642c8e3f2d', 1, 'Tagesbudget (Ø)', '2.416 kcal', 0, now(), now()),
    ('4fb904f4-d0d7-44b0-8274-6446fcf52661', 1, 'Protein', '~184 g', 1, now(), now()),
    ('ef84bcbb-9e4b-4540-bc3b-3622b8bd6c70', 1, 'Kohlenhydrate', '~291 g', 2, now(), now()),
    ('499070e8-df59-417f-a225-109e0dc75d96', 1, 'Fett', '~52 g', 3, now(), now());

-- Seed data: changelog entries (9)
INSERT INTO nutrition.meal_plan_changelog_entry (id, meal_plan_id, tag, was, entry_text, sort_order, creation_date, last_modified)
VALUES
    ('0f4fe0df-5aeb-4104-a196-43d1ea498e3c', 1, 'Whey', '80 g/Tag (2×40 g)',
     '→ 40 g/Tag (je 20 g zu Frühstück & Nachmittagssnack) — halbiert, wie gewünscht.', 0, now(), now()),
    ('66fe1cf6-d562-4239-9ffd-c97a58d33b0f', 1, 'Magerquark', NULL,
     'neu: 300 g/Tag (100 g Frühstück + 200 g Nachmittag) übernimmt den Proteinanteil, den der Whey abgibt.', 1, now(), now()),
    ('897ea972-8e0f-42d9-bbaa-012ad7e0a631', 1, 'Ei', NULL,
     'fest auf 1 Ei/Tag im Frühstück begrenzt, statt wie im Tracking unregelmäßig 1–2 Eier über mehrere Mahlzeiten verteilt.', 2, now(), now()),
    ('5a703cc6-bbcd-4d4a-9e57-3b217e8c9cf3', 1, 'Korrektur', NULL,
     'Abendessen ist an allen 7 Tagen identisch berechnet. Der alte Plan hatte für dieselbe Zutatenliste zwei unterschiedliche Werte (858 vs. 926 kcal) — das war ein Rechenfehler, nicht real erreichbar.', 3, now(), now()),
    ('b1a9b6e1-276a-4c96-adfb-608375471d60', 1, 'Olivenöl', NULL,
     '10 g neu bei Mittag (Wochenende) & Abendessen ausgewiesen, um das Fettziel realistisch zu erreichen, statt es implizit wegzulassen.', 4, now(), now()),
    ('21fd6402-7495-4ee6-ae1d-5935dec49f80', 1, 'Hähnchen-Engpass', NULL,
     'nur 1.200 g statt 1.400 g Hähnchenbrust verfügbar → Portion beim Abendessen auf 170 g/Tag reduziert (statt 200 g). Ausgleich über +50 g Magerquark im Nachmittagssnack (150 g → 200 g), das deckt den Protein- & Kalorienverlust fast exakt — Wochenschnitt bleibt praktisch unverändert.', 5, now(), now()),
    ('91005971-02da-4bb8-9603-7fe7b0654f4d', 1, 'Frühstück', '+ 100 g Magerquark, 1 Ei',
     'Magerquark und Ei aus dem Frühstück entfernt — Frühstück besteht jetzt nur noch aus Haferflocken, Haferdrink, Whey, TK-Himbeeren und Mandeln.', 6, now(), now()),
    ('78e798cb-a8c6-43cd-87df-050587c0f57a', 1, 'Ei', '1 Ei/Tag im Frühstück',
     '→ 1 Ei/Tag im Abendessen (alle 7 Tage) — Ei wird nur noch zu Mittag/Abendbrot am Wochenende bzw. zum Abendbrot an Wochentagen gegessen, nicht mehr morgens.', 7, now(), now()),
    ('9c92c50b-a83b-4e84-9efc-e52a349e748a', 1, 'Magerquark', '100 g Frühstück + 200 g Nachmittag',
     '→ 200 g Nachmittag + 100 g Abendessen — nicht mehr im Frühstück, sondern auf Nachmittag & Abendessen verteilt. Tagesmenge bleibt bei 300 g.', 8, now(), now());

-- Seed data: sections (3)
INSERT INTO nutrition.meal_plan_section
    (id, meal_plan_id, title, note, totals_label, totals_kcal, totals_protein, callout, sort_order, creation_date, last_modified)
VALUES
    ('5b21f5d0-5dd3-4653-8941-8920f7231447', 1, '1 · Tagesstruktur (täglich gleich)',
     'Frühstück & Nachmittag identisch an allen 7 Tagen', NULL, NULL, NULL,
     'Der Whey-Shake wird jetzt kleiner (20 g statt 40 g) und durch Magerquark ergänzt — pro 100 g liefert Magerquark 12 g Protein bei nur 66 kcal und 0,2 g Fett, macht das Cut-Ziel also nicht teurer. Ei und ein Teil des Magerquarks (100 g) sind aus dem Frühstück verschwunden und wandern ins Abendessen: das Frühstück besteht dadurch nur noch aus Haferflocken, Haferdrink, Whey, TK-Himbeeren und Mandeln. Magerquark ist damit auf Nachmittag (200 g) und Abendessen (100 g) verteilt, das Ei gibt es nur noch zu Mittag/Abendbrot am Wochenende bzw. zum Abendbrot an Wochentagen. Die Nachmittags-Portion Magerquark ist weiterhin bewusst größer als rechnerisch nötig (200 g statt 150 g) — das federt den Hähnchen-Engpass beim Abendessen ab.',
     0, now(), now()),
    ('41de111d-8e9e-4067-98ed-6d6ccba19c07', 1, '2 · Wochentage (Montag – Donnerstag)',
     'Kantine am Mittag bleibt Richtwert', 'Tagesgesamt', '2.407 kcal', '182,2 g', NULL,
     1, now(), now()),
    ('edc7ee00-af02-4c76-850c-38315022f2b7', 1, '3 · Wochenende (Freitag – Sonntag)',
     'Mittag wird selbst zubereitet', 'Tagesgesamt', '2.428 kcal', '186,5 g',
     'Zielabweichung über die Woche: im Schnitt 2.416 kcal / 184,0 g Protein / 291,0 g Kohlenhydrate / 52,3 g Fett gegen ein Ziel von 2.422 kcal / 188 g / 296 g / 54 g — trotz reduzierter Hähnchenmenge weiterhin unter 2 % Abweichung, dank der Magerquark-Kompensation.',
     2, now(), now());

-- Seed data: rows (10) — section 1 has 2, section 2 has 4, section 3 has 4
INSERT INTO nutrition.meal_plan_row (id, meal_plan_section_id, meal, details, qty, kcal, protein, sort_order, creation_date, last_modified)
VALUES
    ('b3818af7-c8f5-4f8b-9c13-e6e1a9d7e9a1', '5b21f5d0-5dd3-4653-8941-8920f7231447', 'Frühstück',
     'Haferflocken, Haferdrink, Whey, TK-Himbeeren, Mandeln', '90g/200ml/20g/50g/10g', '519', '28,0 g', 0, now(), now()),
    ('b9420d05-8454-4f21-8e33-3d3dfbbbef01', '5b21f5d0-5dd3-4653-8941-8920f7231447', 'Nachmittag',
     'Whey, Magerquark, Banane', '20g/200g/1 Stk', '314', '40,3 g', 1, now(), now()),

    ('fdf766c6-9571-4aa8-a8d1-9f0f7c5a80c4', '41de111d-8e9e-4067-98ed-6d6ccba19c07', 'Frühstück',
     'siehe Tagesstruktur', '—', '519', '28,0 g', 0, now(), now()),
    ('b321d7fb-5d46-4d9d-8f52-158e14ea958c', '41de111d-8e9e-4067-98ed-6d6ccba19c07', 'Mittag (Kantine)',
     'Mageres Protein, komplexe Carbs, Gemüse (Richtwert)', '1 Portion', '650', '40,0 g', 1, now(), now()),
    ('632eac1a-476f-4ea4-a68f-2c842e62236c', '41de111d-8e9e-4067-98ed-6d6ccba19c07', 'Nachmittag',
     'siehe Tagesstruktur', '—', '314', '40,3 g', 2, now(), now()),
    ('83d82b3a-a2ab-41a7-94e9-fb022ebdc54a', '41de111d-8e9e-4067-98ed-6d6ccba19c07', 'Abendessen',
     'Hähnchenbrustfilet, Dinkel-Penne, Kaisergemüse, Olivenöl, Magerquark, Ei', '170g/120g/200g/10g/100g/1 Stk', '923', '73,9 g', 3, now(), now()),

    ('3faffc1d-51db-47b5-91ab-aafa9c63caae', 'edc7ee00-af02-4c76-850c-38315022f2b7', 'Frühstück',
     'siehe Tagesstruktur', '—', '519', '28,0 g', 0, now(), now()),
    ('cda168ce-aeec-4f84-ad78-ca5e168a6b86', 'edc7ee00-af02-4c76-850c-38315022f2b7', 'Mittagessen',
     'Rinderhüftsteak, Basmatireis, Kaisergemüse, Olivenöl', '145g/100g/200g/10g', '672', '44,3 g', 1, now(), now()),
    ('db5bd1f0-af65-4cd4-81b1-f33e263f2d26', 'edc7ee00-af02-4c76-850c-38315022f2b7', 'Nachmittag',
     'siehe Tagesstruktur', '—', '314', '40,3 g', 2, now(), now()),
    ('1a26687c-cdda-4083-9a1c-3fa4381e15d5', 'edc7ee00-af02-4c76-850c-38315022f2b7', 'Abendessen',
     'Hähnchenbrustfilet, Dinkel-Penne, Kaisergemüse, Olivenöl, Magerquark, Ei', '170g/120g/200g/10g/100g/1 Stk', '923', '73,9 g', 3, now(), now());

-- Seed data: shopping categories (6)
INSERT INTO nutrition.meal_plan_shopping_category (id, meal_plan_id, title, sort_order, creation_date, last_modified)
VALUES
    ('10682dcd-1f76-4db0-8044-ea3a17c7354a', 1, 'Fleisch & Fisch', 0, now(), now()),
    ('6ee27873-bf8b-4cff-a67e-365ce2bad325', 1, 'Milchprodukte & Eier', 1, now(), now()),
    ('bcba0a32-0a0d-4384-a4e0-bde9466ee0df', 1, 'Proteinpulver', 2, now(), now()),
    ('2d23c32b-e833-4ea0-b9a7-b0b8d21d699e', 1, 'Gemüse & Obst (TK & frisch)', 3, now(), now()),
    ('22ca2d83-8380-4ede-acd3-80468f5e47d0', 1, 'Kohlenhydrate & Getreide', 4, now(), now()),
    ('f7d4100f-bd31-4347-8bd0-8efb90599126', 1, 'Fette, Nüsse & Drinks', 5, now(), now());

-- Seed data: shopping items (14) — 2 + 2 + 1 + 3 + 3 + 3
INSERT INTO nutrition.meal_plan_shopping_item
    (id, meal_plan_shopping_category_id, name, brand, badge, badge_text, qty, sort_order, creation_date, last_modified)
VALUES
    ('4b076c0c-fc17-4b28-8679-d3dcaf7945b7', '10682dcd-1f76-4db0-8044-ea3a17c7354a', 'Hähnchenbrustfilet',
     'frisch, Kühltheke', 'warn', 'nur 1.200 g verfügbar', '1.200 g', 0, now(), now()),
    ('ee9037fa-7e11-4690-9176-e5e22fc8d503', '10682dcd-1f76-4db0-8044-ea3a17c7354a', 'Frisches Rinderhüftsteak',
     'Lidl', NULL, NULL, '435 g', 1, now(), now()),

    ('2ca3cb46-7881-49d8-a66a-53ee5c6aff04', '6ee27873-bf8b-4cff-a67e-365ce2bad325', 'Magerquark',
     'Milbona (Lidl), 4× 500 g + 1× 250 g Tuben', NULL, NULL, '2.100 g', 0, now(), now()),
    ('544eb690-80c3-42a3-a52b-14e3ef43e2bc', '6ee27873-bf8b-4cff-a67e-365ce2bad325', 'Eier',
     '10er-Packung', NULL, NULL, '7 Stk', 1, now(), now()),

    ('df29412b-e700-4c3f-ae5f-5651f2838e06', 'bcba0a32-0a0d-4384-a4e0-bde9466ee0df', 'Whey Protein Konzentrat Erdbeere',
     'Rühls Bestes', 'warn', 'nicht bei Lidl', '280 g', 0, now(), now()),

    ('a374534f-6632-4403-8c27-72cfb5bf8e0f', '2d23c32b-e833-4ea0-b9a7-b0b8d21d699e', 'Kaisergemüse TK',
     'Freshona (Lidl)', NULL, NULL, '2.000 g', 0, now(), now()),
    ('979e8e8b-9fd1-4625-b766-c27e98a37461', '2d23c32b-e833-4ea0-b9a7-b0b8d21d699e', 'TK-Himbeeren',
     'Freshona (Lidl)', NULL, NULL, '350 g', 1, now(), now()),
    ('effb7f70-7259-4015-9aba-775d97f9a9b2', '2d23c32b-e833-4ea0-b9a7-b0b8d21d699e', 'Bananen',
     'frisch', NULL, NULL, '7 Stk', 2, now(), now()),

    ('32953ae9-2980-494e-8b85-bb4b894e3ba6', '22ca2d83-8380-4ede-acd3-80468f5e47d0', 'Haferflocken',
     'Bio Hafervollkornflocken Kleinblatt, Lidl', NULL, NULL, '630 g', 0, now(), now()),
    ('1b0a8d11-56ef-4585-84b5-f1b2ae09a616', '22ca2d83-8380-4ede-acd3-80468f5e47d0', 'Dinkel-Penne',
     'Bio, Lidl/Bioland', NULL, NULL, '840 g', 1, now(), now()),
    ('ae4cb5b4-0069-4aa5-95d8-defbd35a6c72', '22ca2d83-8380-4ede-acd3-80468f5e47d0', 'Basmatireis',
     'Lidl, roh', NULL, NULL, '300 g', 2, now(), now()),

    ('4a74e7ee-447b-4d37-999d-59eb8ee16dff', 'f7d4100f-bd31-4347-8bd0-8efb90599126', 'Olivenöl',
     'Lidl', NULL, NULL, '100 ml', 0, now(), now()),
    ('26e8a7df-5fe7-4cb1-9425-198ea98120b8', 'f7d4100f-bd31-4347-8bd0-8efb90599126', 'Mandeln, blanchiert & gehackt',
     'Alesto (Lidl)', NULL, NULL, '70 g', 1, now(), now()),
    ('0b552292-396f-4fd0-bafd-4a34d4ce7c50', 'f7d4100f-bd31-4347-8bd0-8efb90599126', 'Bio Haferdrink',
     'Lidl, 2 Packungen', NULL, NULL, '1,4 L', 2, now(), now());

-- Seed data: footer sources (3)
INSERT INTO nutrition.meal_plan_source (id, meal_plan_id, label, url, sort_order, creation_date, last_modified)
VALUES
    ('df31c398-b885-4689-bdcd-d1ff2291a7a6', 1, 'Magerquark (Milbona/Milsani, fatsecret.de)',
     'https://www.fatsecret.de/Kalorien-Ern%C3%A4hrung/milsani/magerquark/100g', 0, now(), now()),
    ('df4bf910-7545-4aab-ac31-f1a10d4958ff', 1, 'Basmatireis Lidl, roh (fatsecret.de)',
     'https://www.fatsecret.de/Kalorien-Ern%C3%A4hrung/lidl/basmati-reis/100g', 1, now(), now()),
    ('8763cd1a-4096-4115-8ff5-b3476a0821e5', 1, 'Banane, frisch (yazio.com)',
     'https://www.yazio.com/de/kalorientabelle/banane-frisch.html', 2, now(), now());
