/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar;

import com.codename1.backend.DataSource;

/// A row of each of the markets, tax, risk and dispute tables for a shop,
/// so that a route naming one of them has a real object of that store to be
/// refused (MerchantIdorTest).
final class GlobalSeed {
    private GlobalSeed() {
    }

    static void seed(Shop s, String orderId, DataSource db) throws Exception {
        // A shipping profile and a routing rule, for the profile and routing
        // routes.
        db.execute("INSERT INTO shipping_profile (id, store_id, name, position) VALUES "
                + "(?, ?, 'IDOR profile', 0)", new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO routing_rule (id, store_id, kind, location_ids, position) "
                + "VALUES (?, ?, 'CLOSEST', '', 0)", new Object[] {Ids.next(), s.storeId});
        // Checkout settings and a checkout field, for the checkout settings
        // routes.
        db.execute("INSERT INTO checkout_settings (store_id, methods, tips_enabled, tip_presets, "
                + "updated_at) VALUES (?, 'klarna', 1, 'p1500', 0)", new Object[] {s.storeId});
        db.execute("INSERT INTO checkout_field (id, store_id, sort_order, label, kind, choices, "
                + "required, created_at) VALUES (?, ?, 0, 'IDOR field', 'text', '', 0, 0)",
                new Object[] {Ids.next(), s.storeId});
        // A data request, for the data request routes.
        db.execute("INSERT INTO data_request (id, store_id, customer_id, username, kind, status, "
                + "created_at, done_at, done_by) VALUES (?, ?, ?, 'idor@example.com', 'EXPORT', "
                + "'OPEN', 0, 0, '')", new Object[] {Ids.next(), s.storeId, Ids.next()});
        // A card reader and a card payment it took, for the card reader routes.
        String reader = Ids.next();
        db.execute("INSERT INTO card_reader (id, store_id, register_id, provider, provider_ref, "
                + "label, serial, status, created_at) VALUES (?, ?, '', 'simulated', ?, "
                + "'IDOR reader', 'SIM-IDOR', 'ACTIVE', 0)",
                new Object[] {reader, s.storeId, "tmr_sim_" + reader});
        db.execute("INSERT INTO card_payment (id, store_id, register_id, reader_id, client_id, "
                + "intent_ref, amount_cents, currency, status, message, card_label, order_id, "
                + "created_at, updated_at) VALUES (?, ?, '', ?, ?, ?, 100, 'USD', 'PENDING', '', "
                + "'', '', 0, 0)",
                new Object[] {Ids.next(), s.storeId, reader, Ids.next(), "sim_tpi_" + reader});
        // A bundle component and a publishing window, for the bundle and
        // schedule routes.
        db.execute("INSERT INTO bundle_component (id, store_id, product_id, variant_id, quantity, "
                + "position) VALUES (?, ?, ?, ?, 1, 0)",
                new Object[] {Ids.next(), s.storeId, Ids.next(), s.variantId});
        db.execute("INSERT INTO channel_schedule (id, store_id, product_id, channel, publish_at, "
                + "unpublish_at, visible, updated_at) VALUES (?, ?, ?, 'POS', 1, 0, 1, 0)",
                new Object[] {Ids.next(), s.storeId, s.productId});
        // A digital asset, a licence key and a download grant, for the
        // digital goods routes.
        db.execute("INSERT INTO digital_asset (id, store_id, variant_id, file_id, max_downloads, "
                + "expiry_days, created_at) VALUES (?, ?, ?, '', 5, 30, 0)",
                new Object[] {Ids.next(), s.storeId, s.variantId});
        db.execute("INSERT INTO license_key (id, store_id, variant_id, key_text, order_id, "
                + "line_id, assigned_at, created_at) VALUES (?, ?, ?, 'IDOR-KEY', '', '', 0, 0)",
                new Object[] {Ids.next(), s.storeId, s.variantId});
        db.execute("INSERT INTO download_grant (id, store_id, order_id, line_id, variant_id, "
                + "file_id, username, secret, expires_at, downloads, max_downloads, license_key, "
                + "created_at, revoked_at) VALUES (?, ?, ?, ?, ?, '', '', ?, 0, 0, 5, '', 0, 0)",
                new Object[] {Ids.next(), s.storeId, orderId, Ids.next(), s.variantId,
                    Ids.next()});
        // The online store's rows: a page, a post, a menu link, a redirect, a
        // metafield and a metaobject type with one entry.
        db.execute("INSERT INTO content_page (id, store_id, kind, handle, title, body, published, "
                + "updated_at) VALUES (?, ?, 'PAGE', 'idor', 'Idor', '<p>x</p>', 1, 0)",
                new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO blog_post (id, store_id, handle, title, author, tags, excerpt, "
                + "body, published, published_at, updated_at) VALUES "
                + "(?, ?, 'idor', 'Idor', 'Idor', '', '', '<p>x</p>', 1, 0, 0)",
                new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO menu_link (id, store_id, menu, parent_id, title, url, position) "
                + "VALUES (?, ?, 'main', '', 'Idor', '/', 0)", new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO url_redirect (id, store_id, from_path, to_path, hits, created_at) "
                + "VALUES (?, ?, '/s/idor/old', '/s/idor/new', 0, 0)",
                new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO metafield_def (id, store_id, owner_kind, field_key, name, "
                + "field_type, ref_kind, storefront, position, created_at) VALUES "
                + "(?, ?, 'PRODUCT', 'idor', 'Idor', 'text', '', 1, 0, 0)",
                new Object[] {Ids.next(), s.storeId});
        String type = Ids.next();
        db.execute("INSERT INTO metaobject_type (id, store_id, handle, name, created_at) VALUES "
                + "(?, ?, 'idor', 'Idor', 0)", new Object[] {type, s.storeId});
        db.execute("INSERT INTO metaobject_entry (id, store_id, type_id, handle, title, created_at) "
                + "VALUES (?, ?, ?, 'idor', 'Idor', 0)", new Object[] {Ids.next(), s.storeId, type});
        db.execute("INSERT INTO sales_channel (id, store_id, kind, name, status, refresh_token, "
                + "access_token, access_expires_at, webhook_secret, last_pull_at, pull_requested, "
                + "fail_count, next_attempt_at, last_error, ledger_cursor, created_at) VALUES "
                + "(?, ?, 'SIMULATED', 'Idor', 'PAUSED', 'x', '', 0, 'idor-secret-0123456789', 0, 0, 0, "
                + "0, '', 0, 0)", new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO market (id, store_id, code, name, currency, countries, locale, "
                + "rounding, adjust_bps, active, position, created_at) VALUES "
                + "(?, ?, 'idor', 'Idor', 'USD', 'CA', '', 'NONE', 0, 1, 0, 0)",
                new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO duty_rate (id, store_id, country, hs_prefix, rate_bps) VALUES "
                + "(?, ?, 'CA', '', 500)", new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO tax_exemption (id, store_id, username, certificate_id, regions, "
                + "expires_at, created_at) VALUES (?, ?, 'x@example.com', 'C1', '', 0, 0)",
                new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO dispute (id, store_id, order_id, event_id, reason, amount_cents, "
                + "currency, status, due_at, created_at, updated_at) VALUES "
                + "(?, ?, ?, ?, 'fraudulent', 100, 'USD', 'NEEDS_RESPONSE', ?, 0, 0)",
                new Object[] {Ids.next(), s.storeId, orderId, "evt-" + Ids.next(),
                    Long.valueOf(System.currentTimeMillis() + 86400000L)});
        db.execute("UPDATE order_risk SET status = 'HELD' WHERE order_id = ?",
                new Object[] {orderId});
        // The operations features: a saved report, a permission set, a chat.
        db.execute("INSERT INTO saved_report (id, store_id, name, spec, created_by, created_at) "
                + "VALUES (?, ?, 'Idor', 'dimension=day', 'x', 0)", new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO permission_set (id, store_id, name, grants, created_at) VALUES "
                + "(?, ?, 'Idor', 'orders=R', 0)", new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO chat_conversation (id, store_id, token, email, name, subject, status, "
                + "assignee, created_at, updated_at) VALUES (?, ?, ?, 'x@example.com', 'X', 'Hi', "
                + "'OPEN', '', 0, 0)", new Object[] {Ids.next(), s.storeId, "idor" + Ids.next()});
    }
}
