-- A store's checkout settings: the processor payment method types offered
-- beside cards (comma separated), and the tip presets ("p1500" is 15%,
-- "f500" a fixed 5.00, comma separated) when tips are on.
CREATE TABLE checkout_settings (
    store_id VARCHAR(32) NOT NULL PRIMARY KEY,
    methods VARCHAR(400) NOT NULL,
    tips_enabled SMALLINT NOT NULL,
    tip_presets VARCHAR(200) NOT NULL,
    updated_at BIGINT NOT NULL
);
