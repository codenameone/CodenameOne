-- The one row of prices, there from the start, so that saving prices only
-- ever updates it. Created on the first save instead, two admins saving
-- together before anyone had would both insert it, and one of them fail.
-- An updated_at of 0 says nobody has set prices yet: the settings' are the
-- ones in force until somebody does.
INSERT INTO pricing (id, base_cents, per_km_cents, surge, small_order_cents,
    small_order_fee_cents, fee_percent, tax_percent, commission_percent, updated_at, updated_by)
VALUES (1, 0, 0, 1, 0, 0, 0, 0, 0, 0, '');
