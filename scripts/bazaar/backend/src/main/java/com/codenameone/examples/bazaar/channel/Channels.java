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
package com.codenameone.examples.bazaar.channel;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.ChannelDto;
import com.codenameone.examples.bazaar.api.ChannelListingDto;
import com.codenameone.examples.bazaar.api.ChannelSetupDto;
import com.codenameone.examples.bazaar.api.SyncLogDto;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A store's connected marketplaces, as the back office manages them:
/// connect an account, list variants on it, read the sync log, sync now.
/// Reading needs the products area; changing it, write access to it.
/// Tokens and secrets go in and never come out.
@Component
public class Channels {
    static final int MAX_CHANNELS = 10;
    static final int MAX_LISTINGS = 2000;
    static final int LOG_ROWS = 100;

    private final DataSource db;
    private final StoreAccess access;
    private final Connectors connectors;
    private final ChannelSync sync;

    public Channels(DataSource db, StoreAccess access, Connectors connectors, ChannelSync sync) {
        this.db = db;
        this.access = access;
        this.connectors = connectors;
        this.sync = sync;
    }

    @Transactional(readOnly = true)
    public List<ChannelDto> list(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, false, StoreAccess.MANAGERS);
        return dtos(storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<ChannelDto> connect(String username, String storeId, ChannelSetupDto in)
            throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, true, StoreAccess.MANAGERS);
        String kind = in == null ? null : in.kind();
        if (!Connectors.known(kind)) {
            throw new ResponseStatusException(400, "A channel is SIMULATED or EBAY");
        }
        String name = in.name() == null ? "" : in.name().trim();
        if (name.length() == 0 || name.length() > 120) {
            throw new ResponseStatusException(400, "Give the channel a name");
        }
        String refresh = in.refreshToken() == null ? "" : in.refreshToken().trim();
        if (refresh.length() > 4000 || connectors.live(kind) && refresh.length() == 0) {
            throw new ResponseStatusException(400, "Paste the refresh token the marketplace's "
                    + "consent page gave");
        }
        String secret = in.webhookSecret() == null ? "" : in.webhookSecret().trim();
        if (secret.length() < 16 || secret.length() > 128) {
            throw new ResponseStatusException(400, "The notification secret is 16 to 128 characters");
        }
        Map n = db.queryOne("SELECT COUNT(*) AS n FROM sales_channel WHERE store_id = ?",
                new Object[] {storeId});
        if (Rows.number(n.get("n")) >= MAX_CHANNELS) {
            throw new ResponseStatusException(400, "A store connects at most " + MAX_CHANNELS
                    + " channels");
        }
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO sales_channel (id, store_id, kind, name, status, refresh_token, "
                + "access_token, access_expires_at, webhook_secret, last_pull_at, pull_requested, "
                + "fail_count, next_attempt_at, last_error, ledger_cursor, created_at) VALUES "
                + "(?, ?, ?, ?, ?, ?, '', 0, ?, 0, 1, 0, 0, '', ?, ?)", new Object[] {Ids.next(),
                    storeId, kind, name, ChannelSync.ACTIVE, refresh.length() == 0 ? "simulated"
                    : refresh, secret, Long.valueOf(now), Long.valueOf(now)});
        return dtos(storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<ChannelDto> remove(String username, String storeId, String channelId)
            throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, true, StoreAccess.MANAGERS);
        require(storeId, channelId);
        Object[] id = {channelId};
        db.execute("DELETE FROM channel_listing WHERE channel_id = ?", id);
        db.execute("DELETE FROM channel_sync_log WHERE channel_id = ?", id);
        db.execute("DELETE FROM channel_fulfillment WHERE channel_id = ?", id);
        // channel_order stays: the orders it made are still the store's.
        db.execute("DELETE FROM sales_channel WHERE id = ?", id);
        return dtos(storeId);
    }

    @Transactional(readOnly = true)
    public List<ChannelListingDto> listings(String username, String storeId, String channelId)
            throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, false, StoreAccess.MANAGERS);
        require(storeId, channelId);
        return listingDtos(channelId);
    }

    /// Lists a variant of the store on a channel; its external SKU defaults
    /// to the variant's SKU.
    @Transactional(rollbackFor = IOException.class)
    public List<ChannelListingDto> listVariant(String username, String storeId, String channelId,
            ChannelListingDto in) throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, true, StoreAccess.MANAGERS);
        require(storeId, channelId);
        String variantId = in == null || in.variantId() == null ? "" : in.variantId();
        Map v = variantId.length() == 0 || variantId.length() > 32 ? null : db.queryOne(
                "SELECT v.sku, p.store_id FROM variant v JOIN product p ON p.id = v.product_id "
                + "WHERE v.id = ?", new Object[] {variantId});
        if (v == null || !storeId.equals(Rows.text(v.get("store_id")))) {
            throw new ResponseStatusException(404, "No such variant");
        }
        String sku = in.externalSku() == null || in.externalSku().trim().length() == 0
                ? Rows.text(v.get("sku")) : in.externalSku().trim();
        if (sku.length() == 0 || sku.length() > 64) {
            throw new ResponseStatusException(400, "A listed variant needs a SKU the marketplace "
                    + "knows it by");
        }
        if (db.queryOne("SELECT id FROM channel_listing WHERE channel_id = ? AND (variant_id = ? OR "
                + "external_sku = ?)", new Object[] {channelId, variantId, sku}) != null) {
            throw new ResponseStatusException(409, "That variant or SKU is already listed there");
        }
        Map n = db.queryOne("SELECT COUNT(*) AS n FROM channel_listing WHERE channel_id = ?",
                new Object[] {channelId});
        if (Rows.number(n.get("n")) >= MAX_LISTINGS) {
            throw new ResponseStatusException(400, "A channel lists at most " + MAX_LISTINGS
                    + " variants");
        }
        String external = in.externalId() == null ? "" : in.externalId().trim();
        db.execute("INSERT INTO channel_listing (id, store_id, channel_id, variant_id, external_sku, "
                + "external_id, etag, version, pushed_state, pushed_qty, pushed_price_cents, seen_state, "
                + "seen_at, urgent, status, fail_count, next_attempt_at, last_error, updated_at) VALUES "
                + "(?, ?, ?, ?, ?, ?, '', 0, '', 0, 0, '', 0, 1, ?, 0, 0, '', ?)", new Object[] {
                    Ids.next(), storeId, channelId, variantId, sku, ChannelSync.cut(external, 64),
                    ChannelSync.PENDING, Long.valueOf(System.currentTimeMillis())});
        return listingDtos(channelId);
    }

    @Transactional(readOnly = true)
    public List<SyncLogDto> log(String username, String storeId, String channelId)
            throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, false, StoreAccess.MANAGERS);
        require(storeId, channelId);
        return logDtos(channelId);
    }

    /// Runs the channel now, ignoring its schedule and backoff.
    public List<SyncLogDto> syncNow(String username, String storeId, String channelId)
            throws IOException {
        access.requireArea(username, storeId, Perms.PRODUCTS, true, StoreAccess.MANAGERS);
        require(storeId, channelId);
        sync.run(channelId, System.currentTimeMillis(), true);
        return logDtos(channelId);
    }

    private void require(String storeId, String channelId) throws IOException {
        Map c = channelId == null || channelId.length() > 32 ? null : db.queryOne(
                "SELECT store_id FROM sales_channel WHERE id = ?", new Object[] {channelId});
        if (c == null || !storeId.equals(Rows.text(c.get("store_id")))) {
            throw new ResponseStatusException(404, "No such channel");
        }
    }

    private List<ChannelDto> dtos(String storeId) throws IOException {
        List rows = db.query("SELECT * FROM sales_channel WHERE store_id = ? ORDER BY created_at, id",
                new Object[] {storeId});
        List<ChannelDto> out = new ArrayList<ChannelDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String id = Rows.text(r.get("id"));
            Map n = db.queryOne("SELECT COUNT(*) AS n FROM channel_listing WHERE channel_id = ?",
                    new Object[] {id});
            Map e = db.queryOne("SELECT COUNT(*) AS n FROM channel_listing WHERE channel_id = ? AND "
                    + "status = ?", new Object[] {id, ChannelSync.ERROR});
            String kind = Rows.text(r.get("kind"));
            out.add(new ChannelDto(id, kind, Rows.text(r.get("name")), Rows.text(r.get("status")),
                    connectors.live(kind), (int) Rows.number(n.get("n")), (int) Rows.number(e.get("n")),
                    Rows.number(r.get("last_pull_at")), Rows.text(r.get("last_error")),
                    ChannelWebhook.PATH + "?channel=" + id));
        }
        return out;
    }

    private List<ChannelListingDto> listingDtos(String channelId) throws IOException {
        List rows = db.query("SELECT l.*, v.title AS vtitle, p.title AS ptitle FROM channel_listing l "
                + "JOIN variant v ON v.id = l.variant_id JOIN product p ON p.id = v.product_id WHERE "
                + "l.channel_id = ? ORDER BY l.external_sku", new Object[] {channelId});
        List<ChannelListingDto> out = new ArrayList<ChannelListingDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String title = Rows.text(r.get("ptitle"));
            String vt = Rows.text(r.get("vtitle"));
            out.add(new ChannelListingDto(Rows.text(r.get("id")), Rows.text(r.get("variant_id")),
                    vt.length() == 0 || "Default".equals(vt) ? title : title + " - " + vt,
                    Rows.text(r.get("external_sku")), Rows.text(r.get("external_id")),
                    Rows.text(r.get("status")), (int) Rows.number(r.get("pushed_qty")),
                    Rows.number(r.get("pushed_price_cents")), Rows.number(r.get("version")),
                    Rows.text(r.get("last_error"))));
        }
        return out;
    }

    private List<SyncLogDto> logDtos(String channelId) throws IOException {
        List rows = db.query("SELECT * FROM channel_sync_log WHERE channel_id = ? ORDER BY created_at "
                + "DESC, id DESC LIMIT " + LOG_ROWS, new Object[] {channelId});
        List<SyncLogDto> out = new ArrayList<SyncLogDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new SyncLogDto(Rows.text(r.get("id")), Rows.text(r.get("action")),
                    Rows.text(r.get("ref")), Rows.text(r.get("outcome")), Rows.text(r.get("message")),
                    Rows.number(r.get("created_at"))));
        }
        return out;
    }
}
