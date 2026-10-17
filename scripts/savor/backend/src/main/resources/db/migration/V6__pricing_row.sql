-- The one row of prices, there from the start, so that saving prices only
-- ever updates it. Created on the first save instead, two admins saving
-- together before anyone had would both insert it, and one of them fail.
-- An updated_at of 0 says nobody has set prices yet: the settings' are the
-- ones in force until somebody does.
INSERT INTO pricing (id, base_cents, per_km_cents, per_minute_cents, minimum_cents,
    fee_percent, commission_percent, surge, comfort, xl, updated_at, updated_by)
VALUES (1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, '');
