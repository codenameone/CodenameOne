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
package com.codenameone.examples.bazaar.catalog;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.ChannelScheduleDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Scheduled publishing per sales channel. A product can have a window on
/// each channel -- the storefront ([#ONLINE]), the registers ([#POS]) or a
/// connected marketplace (its `sales_channel` id) -- opening at `publishAt`
/// and closing at `unpublishAt` (0 for an open end). [#job] flips each row's
/// `visible` flag as the clock passes a bound; everything that lists or sells
/// reads that flag. A product with no row for a channel follows its status
/// alone, as it always did.
///
/// A window narrows an ACTIVE product; it never publishes a draft.
@Component
public class Schedules {
    public static final String ONLINE = "ONLINE";
    public static final String POS = "POS";

    private final DataSource db;

    public Schedules(DataSource db) {
        this.db = db;
    }

    /// Whether a window holds `now`.
    public static boolean open(long publishAt, long unpublishAt, long now) {
        return (publishAt <= 0 || now >= publishAt) && (unpublishAt <= 0 || now < unpublishAt);
    }

    /// A product's windows, one per channel that has one.
    @Transactional(readOnly = true)
    public List<ChannelScheduleDto> of(String productId) throws IOException {
        List<ChannelScheduleDto> out = new ArrayList<ChannelScheduleDto>();
        List rs = db.query("SELECT channel, publish_at, unpublish_at, visible FROM channel_schedule "
                + "WHERE product_id = ? ORDER BY channel", new Object[] {productId});
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            out.add(new ChannelScheduleDto(Rows.text(r.get("channel")), "",
                    Rows.number(r.get("publish_at")), Rows.number(r.get("unpublish_at")),
                    Rows.number(r.get("visible")) == 1));
        }
        return out;
    }

    /// The channels a store can schedule on: the storefront, the registers
    /// and each connected marketplace, as id and name.
    @Transactional(readOnly = true)
    public List<String[]> channels(String storeId) throws IOException {
        List<String[]> out = new ArrayList<String[]>();
        out.add(new String[] {ONLINE, "Online store"});
        out.add(new String[] {POS, "Point of sale"});
        List rs = db.query("SELECT id, name FROM sales_channel WHERE store_id = ? ORDER BY name",
                new Object[] {storeId});
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            out.add(new String[] {Rows.text(r.get("id")), Rows.text(r.get("name"))});
        }
        return out;
    }

    /// A product's window on every channel of its store, in [#channels]
    /// order, named; a channel with no window answers 0 for both bounds and
    /// visible.
    @Transactional(readOnly = true)
    public List<ChannelScheduleDto> windows(String storeId, String productId) throws IOException {
        List<ChannelScheduleDto> had = of(productId);
        List<String[]> all = channels(storeId);
        List<ChannelScheduleDto> out = new ArrayList<ChannelScheduleDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            String[] ch = all.get(iter);
            ChannelScheduleDto found = null;
            for (int h = 0; h < had.size(); h++) {
                if (had.get(h).channel().equals(ch[0])) {
                    found = had.get(h);
                }
            }
            out.add(found == null ? new ChannelScheduleDto(ch[0], ch[1], 0, 0, true)
                    : new ChannelScheduleDto(ch[0], ch[1], found.publishAt(), found.unpublishAt(),
                        found.visible()));
        }
        return out;
    }

    /// Sets a product's window on a channel; both bounds 0 removes it. The
    /// flag is set for `now` at once, so a window that is already open or
    /// closed takes effect without waiting for the job.
    @Transactional(rollbackFor = IOException.class)
    public void set(String storeId, String productId, String channel, long publishAt,
            long unpublishAt, long now) throws IOException {
        boolean known = ONLINE.equals(channel) || POS.equals(channel);
        if (!known && channel != null && channel.length() <= 32) {
            known = db.queryOne("SELECT id FROM sales_channel WHERE id = ? AND store_id = ?",
                    new Object[] {channel, storeId}) != null;
        }
        if (!known) {
            throw new ResponseStatusException(404, "No such channel");
        }
        if (publishAt < 0 || unpublishAt < 0) {
            throw new ResponseStatusException(400, "A time is not before 1970");
        }
        if (publishAt > 0 && unpublishAt > 0 && unpublishAt <= publishAt) {
            throw new ResponseStatusException(400, "The product must come off after it goes on");
        }
        Map had = db.queryOne("SELECT id, visible FROM channel_schedule WHERE product_id = ? AND "
                + "channel = ?", new Object[] {productId, channel});
        if (publishAt == 0 && unpublishAt == 0) {
            if (had != null) {
                db.execute("DELETE FROM channel_schedule WHERE id = ?", new Object[] {had.get("id")});
                touchListings(productId, channel);
            }
            return;
        }
        int visible = open(publishAt, unpublishAt, now) ? 1 : 0;
        if (had == null) {
            db.execute("INSERT INTO channel_schedule (id, store_id, product_id, channel, publish_at, "
                    + "unpublish_at, visible, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[] {Ids.next(), storeId, productId, channel, Long.valueOf(publishAt),
                        Long.valueOf(unpublishAt), Integer.valueOf(visible), Long.valueOf(now)});
        } else {
            db.execute("UPDATE channel_schedule SET publish_at = ?, unpublish_at = ?, visible = ?, "
                    + "updated_at = ? WHERE id = ?", new Object[] {Long.valueOf(publishAt),
                        Long.valueOf(unpublishAt), Integer.valueOf(visible), Long.valueOf(now),
                        had.get("id")});
        }
        touchListings(productId, channel);
    }

    /// The products of a store hidden on `channel` right now.
    @Transactional(readOnly = true)
    public Set<String> hidden(String storeId, String channel) throws IOException {
        Set<String> out = new HashSet<String>();
        List rs = db.query("SELECT product_id FROM channel_schedule WHERE store_id = ? AND "
                + "channel = ? AND visible = 0", new Object[] {storeId, channel});
        for (int iter = 0; iter < rs.size(); iter++) {
            out.add(Rows.text(((Map) rs.get(iter)).get("product_id")));
        }
        return out;
    }

    /// `products` less those hidden on `channel` right now, in order.
    @Transactional(readOnly = true)
    public List<com.codenameone.examples.bazaar.domain.Product> visible(String storeId,
            String channel, List<com.codenameone.examples.bazaar.domain.Product> products)
            throws IOException {
        Set<String> hide = hidden(storeId, channel);
        if (hide.isEmpty()) {
            return products;
        }
        List<com.codenameone.examples.bazaar.domain.Product> out =
                new ArrayList<com.codenameone.examples.bazaar.domain.Product>();
        for (int iter = 0; iter < products.size(); iter++) {
            if (!hide.contains(products.get(iter).getId())) {
                out.add(products.get(iter));
            }
        }
        return out;
    }

    /// When a window of the store last changed or flipped, 0 for never: a
    /// list built from the products' own times must move with it too.
    @Transactional(readOnly = true)
    public long lastChange(String storeId) throws IOException {
        Map r = db.queryOne("SELECT MAX(updated_at) AS t FROM channel_schedule WHERE store_id = ?",
                new Object[] {storeId});
        return r == null ? 0 : Rows.number(r.get("t"));
    }

    /// Whether a product is hidden on `channel` right now.
    @Transactional(readOnly = true)
    public boolean isHidden(String productId, String channel) throws IOException {
        return db.queryOne("SELECT id FROM channel_schedule WHERE product_id = ? AND channel = ? "
                + "AND visible = 0", new Object[] {productId, channel}) != null;
    }

    /// Whether the product a variant belongs to is hidden on `channel`.
    @Transactional(readOnly = true)
    public boolean isVariantHidden(String variantId, String channel) throws IOException {
        return db.queryOne("SELECT cs.id FROM channel_schedule cs JOIN variant v ON v.product_id = "
                + "cs.product_id WHERE v.id = ? AND cs.channel = ? AND cs.visible = 0",
                new Object[] {variantId, channel}) != null;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000, lock = "bazaar-channel-schedule")
    public void job() {
        try {
            tick(System.currentTimeMillis());
        } catch (IOException | RuntimeException failed) {
            System.out.println("Bazaar: the publishing schedule did not run: " + failed);
        }
    }

    /// Flips every window the clock has crossed since the last run; answers
    /// how many rows changed. A marketplace listing of a product that flipped
    /// is marked for the next sync, which pushes it as unavailable (or
    /// available again).
    @Transactional(rollbackFor = IOException.class)
    public int tick(long now) throws IOException {
        Long at = Long.valueOf(now);
        List rs = db.query("SELECT id, product_id, channel, publish_at, unpublish_at, visible FROM "
                + "channel_schedule WHERE (visible = 0 AND (publish_at = 0 OR publish_at <= ?) AND "
                + "(unpublish_at = 0 OR unpublish_at > ?)) OR (visible = 1 AND ((publish_at > ?) "
                + "OR (unpublish_at > 0 AND unpublish_at <= ?)))", new Object[] {at, at, at, at});
        int changed = 0;
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            int visible = open(Rows.number(r.get("publish_at")), Rows.number(r.get("unpublish_at")),
                    now) ? 1 : 0;
            changed += db.execute("UPDATE channel_schedule SET visible = ?, updated_at = ? WHERE "
                    + "id = ? AND visible <> ?", new Object[] {Integer.valueOf(visible), at,
                        r.get("id"), Integer.valueOf(visible)});
            touchListings(Rows.text(r.get("product_id")), Rows.text(r.get("channel")));
        }
        return changed;
    }

    private void touchListings(String productId, String channel) throws IOException {
        if (ONLINE.equals(channel) || POS.equals(channel)) {
            return;
        }
        db.execute("UPDATE channel_listing SET urgent = 1 WHERE channel_id = ? AND variant_id IN "
                + "(SELECT id FROM variant WHERE product_id = ?)", new Object[] {channel, productId});
    }
}
