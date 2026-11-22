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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.DigitalAssetDto;
import com.codenameone.examples.bazaar.api.DigitalUploadDto;
import com.codenameone.examples.bazaar.api.DownloadGrantDto;
import com.codenameone.examples.bazaar.api.LicenseKeysDto;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.SiteUrls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Digital goods: what a digital variant delivers, and what a paid order
/// line lets its buyer download.
///
/// A digital variant's `digital_asset` names its file and the limits each
/// sale gets (how many downloads, for how many days). When an order is paid
/// every line of a digital variant gets one `download_grant`, keyed by a
/// random secret, and one licence key per unit when the variant has a pool.
/// The buyer's link is `/download/{grantId}.{expiresAt}.{signature}`, the
/// signature an HMAC of the id and expiry under the grant's own secret
/// ([Tokens#sign]). A link that is forged, expired, used up, revoked or for
/// a grant that does not exist is answered one way, 404, so a guess learns
/// nothing.
///
/// Issuing is idempotent (a line has at most one grant), so every path that
/// marks an order paid may call [#issue]. Refunding a line in full revokes its
/// grant; cancelling the order revokes them all.
@Component
public class Downloads {
    /// Downloads a grant allows when the merchant sets none.
    public static final int DEFAULT_MAX_DOWNLOADS = 5;
    /// Days a link lives when the merchant sets none.
    public static final int DEFAULT_EXPIRY_DAYS = 30;
    /// Free keys at or below which the store is told its pool runs low.
    public static final int LOW_KEYS = 5;
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final int MAX_KEYS_PER_PASTE = 5000;

    private final DataSource db;
    private final CatalogFiles files;
    private final StoreAccess access;
    private final VariantRepository variants;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final SiteUrls urls;

    /// Field-injected: the order service tells the store's events, which
    /// call this class, so constructor dependencies would be a cycle.
    @Autowired
    private Orders orderService;
    @Autowired
    private Emails emails;

    public Downloads(DataSource db, CatalogFiles files, StoreAccess access,
            VariantRepository variants, StoreOrderRepository orders, OrderLineRepository lines,
            SiteUrls urls) {
        this.db = db;
        this.files = files;
        this.access = access;
        this.variants = variants;
        this.orders = orders;
        this.lines = lines;
        this.urls = urls;
    }

    // ------------------------------------------------------------ merchant

    /// The download a variant of `storeId` delivers.
    @Transactional(readOnly = true)
    public DigitalAssetDto asset(String username, String storeId, String variantId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Variant v = variant(storeId, variantId);
        return assetDto(v.getId());
    }

    /// Uploads or replaces the variant's file and sets its limits.
    public DigitalAssetDto setAsset(String username, String storeId, String variantId,
            DigitalUploadDto upload) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Variant v = variant(storeId, variantId);
        if (upload == null) {
            throw new ResponseStatusException(400, "Say what to change");
        }
        String fileId = null;
        if (upload.data() != null && upload.data().length() > 0) {
            fileId = files.storeBase64(storeId, CatalogFiles.DOWNLOAD, upload.name(),
                    upload.contentType(), upload.data());
        }
        return saveAsset(storeId, v.getId(), fileId, upload.maxDownloads(),
                upload.expiryDays());
    }

    /// Records a stored `fileId` (or null to keep the file) and the limits
    /// (zero keeps or defaults each) for a variant of the caller's store.
    @Transactional(rollbackFor = IOException.class)
    public DigitalAssetDto saveAsset(String storeId, String variantId, String fileId,
            int maxDownloads, int expiryDays) throws IOException {
        if (maxDownloads < 0 || maxDownloads > 1000 || expiryDays < 0 || expiryDays > 3650) {
            throw new ResponseStatusException(400, "Downloads are 1 to 1000 and days 1 to 3650");
        }
        Map row = db.queryOne("SELECT id, file_id, max_downloads, expiry_days FROM digital_asset "
                + "WHERE variant_id = ?", new Object[] {variantId});
        if (row == null) {
            db.execute("INSERT INTO digital_asset (id, store_id, variant_id, file_id, "
                    + "max_downloads, expiry_days, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new Object[] {Ids.next(), storeId, variantId, fileId == null ? "" : fileId,
                        Integer.valueOf(maxDownloads > 0 ? maxDownloads : DEFAULT_MAX_DOWNLOADS),
                        Integer.valueOf(expiryDays > 0 ? expiryDays : DEFAULT_EXPIRY_DAYS),
                        Long.valueOf(System.currentTimeMillis())});
        } else {
            String old = Rows.text(row.get("file_id"));
            db.execute("UPDATE digital_asset SET file_id = ?, max_downloads = ?, expiry_days = ? "
                    + "WHERE id = ?", new Object[] {fileId == null ? old : fileId,
                        Integer.valueOf(maxDownloads > 0 ? maxDownloads
                            : (int) Rows.number(row.get("max_downloads"))),
                        Integer.valueOf(expiryDays > 0 ? expiryDays
                            : (int) Rows.number(row.get("expiry_days"))),
                        Rows.text(row.get("id"))});
            // A replaced file stays while a grant still names it: a buyer who
            // paid for the old version keeps it.
            if (fileId != null && old.length() > 0 && !old.equals(fileId)
                    && empty(db.query("SELECT id FROM download_grant WHERE file_id = ?",
                        new Object[] {old}))) {
                files.remove(old);
            }
        }
        return assetDto(variantId);
    }

    /// Adds pasted keys to the variant's pool, one per line, skipping blank
    /// lines and keys the pool already has.
    public DigitalAssetDto addKeys(String username, String storeId, String variantId,
            LicenseKeysDto keys) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Variant v = variant(storeId, variantId);
        List<String> pasted = keyLines(keys == null ? null : keys.keys());
        if (pasted.size() > MAX_KEYS_PER_PASTE) {
            throw new ResponseStatusException(400, "At most " + MAX_KEYS_PER_PASTE
                    + " keys at a time");
        }
        insertKeys(storeId, v.getId(), pasted);
        return assetDto(v.getId());
    }

    @Transactional(rollbackFor = IOException.class)
    private void insertKeys(String storeId, String variantId, List<String> pasted)
            throws IOException {
        if (db.queryOne("SELECT id FROM digital_asset WHERE variant_id = ?",
                new Object[] {variantId}) == null) {
            saveAsset(storeId, variantId, null, 0, 0);
        }
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < pasted.size(); iter++) {
            String k = pasted.get(iter);
            if (db.queryOne("SELECT id FROM license_key WHERE variant_id = ? AND key_text = ?",
                    new Object[] {variantId, k}) != null) {
                continue;
            }
            db.execute("INSERT INTO license_key (id, store_id, variant_id, key_text, order_id, "
                    + "line_id, assigned_at, created_at) VALUES (?, ?, ?, ?, '', '', 0, ?)",
                    new Object[] {Ids.next(), storeId, variantId, k, Long.valueOf(now + iter)});
        }
    }

    /// The non-blank, distinct lines of `text`, each trimmed and at most 200
    /// characters.
    static List<String> keyLines(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) {
            return out;
        }
        int start = 0;
        while (start <= text.length()) {
            int nl = text.indexOf('\n', start);
            String line = text.substring(start, nl < 0 ? text.length() : nl).trim();
            if (line.length() > 200) {
                throw new ResponseStatusException(400, "A licence key is at most 200 characters");
            }
            if (line.length() > 0 && !out.contains(line)) {
                out.add(line);
            }
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }
        return out;
    }

    /// An order's grants as its merchant sees them: no links.
    @Transactional(readOnly = true)
    public List<DownloadGrantDto> ofOrder(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        StoreOrder o = orderId == null || orderId.length() > 32 ? null : orders.findById(orderId);
        if (o == null || !storeId.equals(o.getStoreId())) {
            throw new ResponseStatusException(404, "No such order");
        }
        return grants(orderId, false);
    }

    /// Stops a grant's link from working.
    @Transactional(rollbackFor = IOException.class)
    public DownloadGrantDto revoke(String username, String storeId, String grantId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Map row = grantId == null || grantId.length() > 32 ? null
                : db.queryOne("SELECT id, store_id, order_id FROM download_grant WHERE id = ?",
                    new Object[] {grantId});
        if (row == null || !storeId.equals(Rows.text(row.get("store_id")))) {
            throw new ResponseStatusException(404, "No such download");
        }
        db.execute("UPDATE download_grant SET revoked_at = ? WHERE id = ? AND revoked_at = 0",
                new Object[] {Long.valueOf(System.currentTimeMillis()), grantId});
        List<DownloadGrantDto> all = grants(Rows.text(row.get("order_id")), false);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(grantId)) {
                return all.get(iter);
            }
        }
        throw new ResponseStatusException(404, "No such download");
    }

    // ------------------------------------------------------------- shopper

    /// The signed-in buyer's downloads for an order of theirs, with links.
    @Transactional(readOnly = true)
    public List<DownloadGrantDto> mine(String username, String orderId) throws IOException {
        StoreOrder o = orderId == null || orderId.length() > 32 ? null : orders.findById(orderId);
        if (o == null || username == null || !username.equals(o.getUsername())) {
            throw new ResponseStatusException(404, "No such order");
        }
        return grants(orderId, true);
    }

    /// An order's grants with their links, for its mail and pages.
    @Transactional(readOnly = true)
    public List<DownloadGrantDto> forOrder(String orderId) throws IOException {
        return grants(orderId, true);
    }

    private List<DownloadGrantDto> grants(String orderId, boolean links) throws IOException {
        List<DownloadGrantDto> out = new ArrayList<DownloadGrantDto>();
        List rows = db.query("SELECT id, order_id, line_id, file_id, secret, expires_at, "
                + "downloads, max_downloads, revoked_at, created_at FROM download_grant "
                + "WHERE order_id = ? ORDER BY created_at, id", new Object[] {orderId});
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        Map<String, OrderLine> byId = new HashMap<String, OrderLine>();
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            byId.put(ls.get(iter).getId(), ls.get(iter));
        }
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String id = Rows.text(r.get("id"));
            String lineId = Rows.text(r.get("line_id"));
            OrderLine line = byId.get(lineId);
            String title = line == null ? "" : line.getTitle()
                    + (line.getVariantTitle() == null || line.getVariantTitle().length() == 0
                        ? "" : " - " + line.getVariantTitle());
            String fileId = Rows.text(r.get("file_id"));
            Object[] meta = fileId.length() == 0 ? null : files.meta(fileId);
            long expires = Rows.number(r.get("expires_at"));
            int used = (int) Rows.number(r.get("downloads"));
            int max = (int) Rows.number(r.get("max_downloads"));
            boolean revoked = Rows.number(r.get("revoked_at")) != 0;
            String url = "";
            if (links && meta != null && !revoked && expires > now && used < max) {
                url = urls.absolute(null, path(token(id, expires, Rows.text(r.get("secret")))));
            }
            out.add(new DownloadGrantDto(id, orderId, lineId, title,
                    meta == null ? "" : Rows.text(meta[2]), url, expires, used, max, revoked,
                    keysOf(lineId)));
        }
        return out;
    }

    private List<String> keysOf(String lineId) throws IOException {
        List<String> out = new ArrayList<String>();
        List rows = db.query("SELECT key_text FROM license_key WHERE line_id = ? "
                + "ORDER BY created_at, id", new Object[] {lineId});
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            out.add(Rows.text(((Map) rows.get(iter)).get("key_text")));
        }
        return out;
    }

    // ----------------------------------------------------------- lifecycle

    /// An order was placed or paid: marks its digital lines delivered (which
    /// takes a held payment, as shipping does) and issues their grants once
    /// it is paid. Answers how many grants were new.
    public int deliver(String orderId) throws IOException {
        StoreOrder o = orders.findById(orderId);
        if (o == null || Orders.CANCELLED.equals(o.getStatus())) {
            return 0;
        }
        String money = o.getFinancial();
        if (!Orders.PAID.equals(money) && !Orders.AUTHORIZED.equals(money)
                && !Orders.PARTIALLY_REFUNDED.equals(money)) {
            return 0;
        }
        Map<String, Integer> open = new HashMap<String, Integer>();
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine line = ls.get(iter);
            int left = line.getQuantity() - line.getRefundedQty() - line.getFulfilledQty();
            if (left > 0 && isDigital(line.getVariantId())) {
                open.put(line.getId(), Integer.valueOf(left));
            }
        }
        if (!open.isEmpty()) {
            orderService.recordFulfilled(orderId, open, "system");
        }
        return issue(orderId);
    }

    /// Issues a grant for every paid digital line that has none yet, and a
    /// licence key per unit when the variant has a pool. Answers how many
    /// grants were new. Does nothing for an order that is not paid.
    public int issue(String orderId) throws IOException {
        StoreOrder o = orders.findById(orderId);
        if (o == null || Orders.CANCELLED.equals(o.getStatus())
                || !Orders.PAID.equals(o.getFinancial())
                    && !Orders.PARTIALLY_REFUNDED.equals(o.getFinancial())) {
            return 0;
        }
        int made = 0;
        List<String> pools = new ArrayList<String>();
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine line = ls.get(iter);
            int units = line.getQuantity() - line.getRefundedQty();
            if (units <= 0 || !isDigital(line.getVariantId())) {
                continue;
            }
            Map asset = db.queryOne("SELECT file_id, max_downloads, expiry_days FROM "
                    + "digital_asset WHERE variant_id = ?", new Object[] {line.getVariantId()});
            if (asset == null) {
                continue;
            }
            if (!insertGrant(o, line, asset)) {
                continue;
            }
            made++;
            if (claimKeys(o.getStoreId(), line.getVariantId(), orderId, line.getId(), units) > 0
                    && !pools.contains(line.getVariantId())) {
                pools.add(line.getVariantId());
            }
        }
        for (int iter = 0; iter < pools.size(); iter++) {
            warnIfLow(o.getStoreId(), pools.get(iter));
        }
        return made;
    }

    /// Inserts the line's grant; false when it already has one.
    private boolean insertGrant(StoreOrder o, OrderLine line, Map asset) throws IOException {
        if (db.queryOne("SELECT id FROM download_grant WHERE line_id = ?",
                new Object[] {line.getId()}) != null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long days = Math.max(1, Rows.number(asset.get("expiry_days")));
        try {
            db.execute("INSERT INTO download_grant (id, store_id, order_id, line_id, variant_id, "
                    + "file_id, username, secret, expires_at, downloads, max_downloads, "
                    + "license_key, created_at, revoked_at) VALUES "
                    + "(?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, '', ?, 0)", new Object[] {Ids.next(),
                        o.getStoreId(), o.getId(), line.getId(), line.getVariantId(),
                        Rows.text(asset.get("file_id")), o.getUsername() == null ? ""
                            : o.getUsername(), Ids.next() + Ids.next(),
                        Long.valueOf(now + days * DAY),
                        Integer.valueOf((int) Math.max(1, Rows.number(asset.get("max_downloads")))),
                        Long.valueOf(now)});
        } catch (IOException | RuntimeException taken) {
            // The unique line index: another path issued it meanwhile.
            if (db.queryOne("SELECT id FROM download_grant WHERE line_id = ?",
                    new Object[] {line.getId()}) != null) {
                return false;
            }
            throw taken;
        }
        return true;
    }

    /// Takes `units` free keys of a variant's pool for an order line, each by
    /// a conditional update that only a free key passes, so two sales never
    /// get the same key. Answers how many it took (fewer when the pool ran
    /// out; the merchant is warned and can add keys).
    public int claimKeys(String storeId, String variantId, String orderId, String lineId,
            int units) throws IOException {
        int taken = 0;
        int misses = 0;
        while (taken < units && misses < 50) {
            // The oldest few free keys; a key another sale took meanwhile
            // fails the conditional update below and the next is tried.
            List candidates = db.query("SELECT id FROM license_key WHERE variant_id = ? AND "
                    + "order_id = '' ORDER BY created_at, id", new Object[] {variantId});
            if (empty(candidates)) {
                break;
            }
            Map free = (Map) candidates.get(Math.min(misses, candidates.size() - 1));
            int n = db.execute("UPDATE license_key SET order_id = ?, line_id = ?, assigned_at = ? "
                    + "WHERE id = ? AND order_id = '' AND store_id = ?", new Object[] {orderId,
                        lineId, Long.valueOf(System.currentTimeMillis()),
                        Rows.text(free.get("id")), storeId});
            if (n == 1) {
                taken++;
            } else {
                misses++;
            }
        }
        return taken;
    }

    private void warnIfLow(String storeId, String variantId) {
        try {
            long free = Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM license_key WHERE "
                    + "variant_id = ? AND order_id = ''", new Object[] {variantId}).get("n"));
            if (free <= LOW_KEYS) {
                Variant v = variants.findById(variantId);
                String sku = v == null || v.getSku() == null || v.getSku().length() == 0
                        ? (v == null ? variantId : v.getTitle()) : v.getSku();
                emails.lowStock(storeId, sku + " (licence keys)", (int) free, LOW_KEYS);
            }
        } catch (IOException | RuntimeException failed) {
            System.err.println("licence key alert " + variantId + ": " + failed);
        }
    }

    /// Revokes the grant of every line refunded in full.
    public void revokeRefunded(String orderId) throws IOException {
        List<OrderLine> ls = lines.findByOrderId(orderId);
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine line = ls.get(iter);
            if (line.getRefundedQty() > 0 && line.getRefundedQty() >= line.getQuantity()) {
                db.execute("UPDATE download_grant SET revoked_at = ? WHERE line_id = ? AND "
                        + "revoked_at = 0", new Object[] {Long.valueOf(now), line.getId()});
            }
        }
        StoreOrder o = orders.findById(orderId);
        if (o != null && Orders.REFUNDED.equals(o.getFinancial())) {
            revokeAll(orderId);
        }
    }

    /// Revokes every grant of an order.
    public void revokeAll(String orderId) throws IOException {
        db.execute("UPDATE download_grant SET revoked_at = ? WHERE order_id = ? AND revoked_at = 0",
                new Object[] {Long.valueOf(System.currentTimeMillis()), orderId});
    }

    /// True when the order has a line of a digital variant.
    @Transactional(readOnly = true)
    public boolean hasDigital(String orderId) throws IOException {
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            if (isDigital(ls.get(iter).getVariantId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDigital(String variantId) throws IOException {
        Variant v = variantId == null || variantId.length() == 0 ? null
                : variants.findById(variantId);
        return v != null && v.isDigital();
    }

    // ------------------------------------------------------------- serving

    /// What a valid link serves.
    public static final class Served {
        public final String fileId;
        public final String name;

        Served(String fileId, String name) {
            this.fileId = fileId;
            this.name = name;
        }
    }

    /// Counts one download of the grant a link names and answers its file;
    /// 404 for a link that is malformed, forged, expired, revoked, used up
    /// or whose file is gone.
    public Served consume(String token) throws IOException {
        Map row = row(token);
        if (row == null) {
            throw gone();
        }
        String fileId = Rows.text(row.get("file_id"));
        Object[] meta = fileId.length() == 0 ? null : files.meta(fileId);
        if (meta == null) {
            throw gone();
        }
        if (db.execute("UPDATE download_grant SET downloads = downloads + 1 WHERE id = ? AND "
                + "downloads < max_downloads AND revoked_at = 0 AND expires_at > ?",
                new Object[] {Rows.text(row.get("id")),
                    Long.valueOf(System.currentTimeMillis())}) != 1) {
            throw gone();
        }
        return new Served(fileId, Rows.text(meta[2]));
    }

    private static boolean empty(List rows) {
        return rows == null || rows.isEmpty();
    }

    private static ResponseStatusException gone() {
        return new ResponseStatusException(404, "This download link has expired or was revoked");
    }

    /// The grant row a token names when it is well formed, signed with the
    /// grant's secret, unexpired, unrevoked and not used up; null otherwise.
    private Map row(String token) throws IOException {
        if (token == null || token.length() > 200) {
            return null;
        }
        int a = token.indexOf('.');
        int b = a < 0 ? -1 : token.indexOf('.', a + 1);
        if (a <= 0 || a > 32 || b <= a + 1 || b == token.length() - 1) {
            return null;
        }
        String id = token.substring(0, a);
        long expires = digits(token.substring(a + 1, b));
        if (expires <= System.currentTimeMillis()) {
            return null;
        }
        Map r = db.queryOne("SELECT id, file_id, secret, expires_at, downloads, max_downloads, "
                + "revoked_at FROM download_grant WHERE id = ?", new Object[] {id});
        if (r == null || Rows.number(r.get("revoked_at")) != 0
                || Rows.number(r.get("expires_at")) != expires
                || Rows.number(r.get("downloads")) >= Rows.number(r.get("max_downloads"))) {
            return null;
        }
        if (!Tokens.verify(Rows.text(r.get("secret")), id + "." + expires,
                token.substring(b + 1))) {
            return null;
        }
        return r;
    }

    private static long digits(String s) {
        if (s.length() == 0 || s.length() > 15) {
            return -1;
        }
        long v = 0;
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    /// The token of a grant.
    static String token(String id, long expires, String secret) throws IOException {
        return id + "." + expires + "." + Tokens.sign(secret, id + "." + expires);
    }

    /// The path a token opens.
    public static String path(String token) {
        return "/download/" + token;
    }

    // ------------------------------------------------------------- helpers

    private Variant variant(String storeId, String variantId) throws IOException {
        Variant v = variantId == null || variantId.length() > 32 ? null
                : variants.findById(variantId);
        if (v == null || !storeId.equals(v.getStoreId())) {
            throw new ResponseStatusException(404, "No such variant");
        }
        return v;
    }

    private DigitalAssetDto assetDto(String variantId) throws IOException {
        Map row = db.queryOne("SELECT file_id, max_downloads, expiry_days FROM digital_asset "
                + "WHERE variant_id = ?", new Object[] {variantId});
        long free = Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM license_key WHERE "
                + "variant_id = ? AND order_id = ''", new Object[] {variantId}).get("n"));
        long sold = Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM license_key WHERE "
                + "variant_id = ? AND order_id <> ''", new Object[] {variantId}).get("n"));
        if (row == null) {
            return new DigitalAssetDto(variantId, "", "", "", 0, DEFAULT_MAX_DOWNLOADS,
                    DEFAULT_EXPIRY_DAYS, (int) free, (int) sold);
        }
        String fileId = Rows.text(row.get("file_id"));
        Object[] meta = fileId.length() == 0 ? null : files.meta(fileId);
        return new DigitalAssetDto(variantId, meta == null ? "" : fileId,
                meta == null ? "" : Rows.text(meta[2]), meta == null ? "" : Rows.text(meta[3]),
                meta == null ? 0 : Rows.number(meta[4]),
                (int) Rows.number(row.get("max_downloads")),
                (int) Rows.number(row.get("expiry_days")), (int) free, (int) sold);
    }
}
