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
package com.codenameone.examples.bazaar.platform;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AppListingDecisionDto;
import com.codenameone.examples.bazaar.api.AppListingDto;
import com.codenameone.examples.bazaar.api.AppReviewDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.ops.AppStoreText;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.security.Ascii;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The app directory: listings, the platform's review of them, and stores'
/// reviews of apps.
///
/// A developer submits one listing per app; it waits as `PENDING` until a
/// platform admin approves it, rejects it with a reason, or (once approved)
/// delists it with one. Resubmitting puts it back in the queue, so an
/// approved listing that its developer changes is off the directory until it
/// is looked at again. Only `APPROVED` listings are answered to anyone else.
///
/// A store may review an app it has, or once had, installed: one review per
/// store, which writing again replaces. A platform admin may hide a review;
/// the average counts visible ones only.
@Component
public class AppDirectory {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String DELISTED = "DELISTED";
    /// Listings in one page of the directory.
    public static final int PAGE = 20;
    static final int MAX_SCREENSHOTS = 5;

    private final DataSource db;
    private final StoreAccess access;
    private final AuditEventRepository audits;

    public AppDirectory(DataSource db, StoreAccess access, AuditEventRepository audits) {
        this.db = db;
        this.access = access;
        this.audits = audits;
    }

    // ============================================================ developers

    @Transactional(readOnly = true)
    public AppListingDto mine(String username, String appId) throws IOException {
        Map app = ownApp(username, appId);
        Map row = db.queryOne("SELECT * FROM app_listing WHERE app_id = ?",
                new Object[] {Rows.text(app.get("id"))});
        if (row == null) {
            throw new ResponseStatusException(404, "The app has no listing yet");
        }
        return dto(row, app);
    }

    /// Submits the listing for review; the handle is made from the name the
    /// first time and kept after that, so a listing's address never moves.
    @Transactional(rollbackFor = IOException.class)
    public AppListingDto submit(String username, String appId, AppListingDto in)
            throws IOException {
        Map app = ownApp(username, appId);
        if (in == null) {
            throw new ResponseStatusException(400, "No listing");
        }
        String name = required(in.name(), 80, "name");
        String tagline = required(in.tagline(), 140, "tagline");
        String description = required(in.description(), 4000, "description");
        String category = in.category() == null ? "" : in.category();
        if (!AppStoreText.CATEGORIES.contains(category)) {
            throw new ResponseStatusException(400, "The category is one of "
                    + AppStoreText.CATEGORIES);
        }
        String icon = url(in.iconUrl(), "icon URL", false);
        List<String> shots = in.screenshots() == null ? new ArrayList<String>()
                : in.screenshots();
        if (shots.size() > MAX_SCREENSHOTS) {
            throw new ResponseStatusException(400, "At most " + MAX_SCREENSHOTS
                    + " screenshots");
        }
        StringBuilder joined = new StringBuilder();
        for (int iter = 0; iter < shots.size(); iter++) {
            joined.append(iter == 0 ? "" : "\n").append(url(shots.get(iter), "screenshot", true));
        }
        String pricing = optional(in.pricing(), 200);
        String support = url(in.supportUrl(), "support URL", true);
        String privacy = url(in.privacyUrl(), "privacy policy URL", true);
        String search = Ascii.lower(name + " " + tagline + " " + description);
        long now = System.currentTimeMillis();
        String id = Rows.text(app.get("id"));
        Map row = db.queryOne("SELECT * FROM app_listing WHERE app_id = ?", new Object[] {id});
        if (row == null) {
            String handle = AppStoreText.handle(name);
            if (db.queryOne("SELECT id FROM app_listing WHERE handle = ?",
                    new Object[] {handle}) != null) {
                handle = Codes.cut(handle, 50) + "-" + id;
            }
            db.execute("INSERT INTO app_listing (id, app_id, handle, developer, name, tagline, "
                    + "description, category, icon_url, screenshots, pricing, support_url, "
                    + "privacy_url, search_text, status, reason, reviewed_by, submitted_at, "
                    + "reviewed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '', '', "
                    + "?, 0)", new Object[] {Ids.next(), id, handle, username, name, tagline,
                        description, category, icon, joined.toString(), pricing, support,
                        privacy, search, PENDING, Long.valueOf(now)});
        } else {
            db.execute("UPDATE app_listing SET name = ?, tagline = ?, description = ?, "
                    + "category = ?, icon_url = ?, screenshots = ?, pricing = ?, "
                    + "support_url = ?, privacy_url = ?, search_text = ?, status = ?, "
                    + "submitted_at = ? WHERE app_id = ?", new Object[] {name, tagline,
                        description, category, icon, joined.toString(), pricing, support,
                        privacy, search, PENDING, Long.valueOf(now), id});
        }
        audit("", username, "APP_LISTING_SUBMITTED", id, name);
        return mine(username, appId);
    }

    // ================================================================ admins

    @Transactional(readOnly = true)
    public List<AppListingDto> queue(String status) throws IOException {
        String s = status == null || status.length() == 0 ? PENDING : status;
        List rows = db.query("SELECT * FROM app_listing WHERE status = ? ORDER BY submitted_at",
                new Object[] {s});
        return dtos(rows);
    }

    @Transactional(rollbackFor = IOException.class)
    public AppListingDto decide(String admin, String listingId, AppListingDecisionDto d)
            throws IOException {
        Map row = listingId == null || listingId.length() > 32 ? null
                : db.queryOne("SELECT * FROM app_listing WHERE id = ?", new Object[] {listingId});
        if (row == null) {
            throw new ResponseStatusException(404, "No such listing");
        }
        String action = d == null || d.action() == null ? "" : d.action();
        String reason = d == null ? "" : optional(d.reason(), 500);
        String current = Rows.text(row.get("status"));
        String next;
        if ("APPROVE".equals(action)) {
            next = APPROVED;
            reason = "";
        } else if ("REJECT".equals(action)) {
            next = REJECTED;
        } else if ("DELIST".equals(action)) {
            if (!APPROVED.equals(current)) {
                throw new ResponseStatusException(409, "Only an approved listing is delisted");
            }
            next = DELISTED;
        } else {
            throw new ResponseStatusException(400, "The action is APPROVE, REJECT or DELIST");
        }
        if (!APPROVED.equals(next) && reason.trim().length() == 0) {
            throw new ResponseStatusException(400, "Say why, for the developer");
        }
        db.execute("UPDATE app_listing SET status = ?, reason = ?, reviewed_by = ?, "
                + "reviewed_at = ? WHERE id = ?", new Object[] {next, reason, admin,
                    Long.valueOf(System.currentTimeMillis()), listingId});
        audit("", admin, "APP_LISTING_" + next, Rows.text(row.get("app_id")), reason);
        return dto(db.queryOne("SELECT * FROM app_listing WHERE id = ?",
                new Object[] {listingId}), null);
    }

    @Transactional(readOnly = true)
    public List<AppReviewDto> allReviews(String listingId) throws IOException {
        Map row = listingId == null ? null
                : db.queryOne("SELECT app_id FROM app_listing WHERE id = ?",
                        new Object[] {listingId});
        if (row == null) {
            throw new ResponseStatusException(404, "No such listing");
        }
        return reviewsOf(Rows.text(row.get("app_id")), true);
    }

    @Transactional(rollbackFor = IOException.class)
    public AppReviewDto hide(String admin, String reviewId) throws IOException {
        Map r = reviewId == null || reviewId.length() > 32 ? null
                : db.queryOne("SELECT * FROM app_review WHERE id = ?", new Object[] {reviewId});
        if (r == null) {
            throw new ResponseStatusException(404, "No such review");
        }
        db.execute("UPDATE app_review SET hidden = 1, hidden_by = ? WHERE id = ?",
                new Object[] {admin, reviewId});
        audit(Rows.text(r.get("store_id")), admin, "APP_REVIEW_HIDDEN",
                Rows.text(r.get("app_id")), reviewId);
        return review(reviewId);
    }

    // ================================================================ public

    /// One page of approved listings, best rated first, then by name.
    @Transactional(readOnly = true)
    public List<AppListingDto> search(String q, String category, int page) throws IOException {
        StringBuilder sql = new StringBuilder("SELECT * FROM app_listing WHERE status = ?");
        List<Object> args = new ArrayList<Object>();
        args.add(APPROVED);
        if (category != null && category.length() > 0) {
            sql.append(" AND category = ?");
            args.add(category);
        }
        String words = q == null ? "" : Ascii.lower(Codes.cut(q.trim(), 100));
        if (words.length() > 0) {
            // LIKE's own wildcards are taken out rather than escaped: escaping
            // differs between the databases, and nobody searches for them.
            StringBuilder plain = new StringBuilder();
            for (int iter = 0; iter < words.length(); iter++) {
                char c = words.charAt(iter);
                if (c != '%' && c != '_' && c != '\\') {
                    plain.append(c);
                }
            }
            sql.append(" AND search_text LIKE ?");
            args.add("%" + plain + "%");
        }
        sql.append(" ORDER BY name");
        List<AppListingDto> all = dtos(db.query(sql.toString(), args.toArray()));
        // Best rated first; a stable sort keeps the names in order within a rating.
        List<AppListingDto> sorted = new ArrayList<AppListingDto>(all);
        java.util.Collections.sort(sorted, (a, b) -> b.ratingTenths() - a.ratingTenths());
        int from = Math.max(0, page) * PAGE;
        List<AppListingDto> out = new ArrayList<AppListingDto>();
        for (int iter = from; iter < sorted.size() && iter < from + PAGE; iter++) {
            out.add(sorted.get(iter));
        }
        return out;
    }

    /// An approved listing by its handle; 404 for any other.
    @Transactional(readOnly = true)
    public AppListingDto listing(String handle) throws IOException {
        Map row = handle == null || handle.length() > 64 ? null
                : db.queryOne("SELECT * FROM app_listing WHERE handle = ? AND status = ?",
                        new Object[] {handle, APPROVED});
        if (row == null) {
            throw new ResponseStatusException(404, "No such app");
        }
        return dto(row, null);
    }

    @Transactional(readOnly = true)
    public List<AppReviewDto> reviews(String handle) throws IOException {
        return reviewsOf(listing(handle).appId(), false);
    }

    // ============================================================== merchant

    /// Writes the store's review of an app; a second one replaces the first.
    @Transactional(rollbackFor = IOException.class)
    public AppReviewDto review(String username, String storeId, AppReviewDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        String appId = in == null || in.appId() == null ? "" : in.appId();
        Map inst = appId.length() == 0 || appId.length() > 32 ? null
                : db.queryOne("SELECT id FROM app_installation WHERE app_id = ? AND store_id = ?",
                        new Object[] {appId, storeId});
        if (inst == null) {
            throw new ResponseStatusException(403, "Only a store that installed an app "
                    + "reviews it");
        }
        if (in.rating() < 1 || in.rating() > 5) {
            throw new ResponseStatusException(400, "The rating is 1 to 5");
        }
        String body = optional(in.body(), 2000);
        long now = System.currentTimeMillis();
        Map had = db.queryOne("SELECT id FROM app_review WHERE app_id = ? AND store_id = ?",
                new Object[] {appId, storeId});
        String id;
        if (had == null) {
            id = Ids.next();
            db.execute("INSERT INTO app_review (id, app_id, store_id, username, rating, body, "
                    + "hidden, hidden_by, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, 0, '', ?, ?)", new Object[] {id, appId, storeId,
                        username, Integer.valueOf(in.rating()), body, Long.valueOf(now),
                        Long.valueOf(now)});
        } else {
            // Rewriting keeps a hidden review hidden: it is the same review.
            id = Rows.text(had.get("id"));
            db.execute("UPDATE app_review SET username = ?, rating = ?, body = ?, "
                    + "updated_at = ? WHERE id = ?", new Object[] {username,
                        Integer.valueOf(in.rating()), body, Long.valueOf(now), id});
        }
        audit(storeId, username, "APP_REVIEWED", appId, in.rating() + "/5");
        return review(id);
    }

    // ================================================================ shared

    private List<AppReviewDto> reviewsOf(String appId, boolean hiddenToo) throws IOException {
        List rows = db.query("SELECT r.id AS id, r.app_id AS app_id, s.name AS store_name, "
                + "r.rating AS rating, r.body AS body, r.hidden AS hidden, "
                + "r.created_at AS created_at FROM app_review r JOIN store s ON s.id = r.store_id "
                + "WHERE r.app_id = ?" + (hiddenToo ? "" : " AND r.hidden = 0")
                + " ORDER BY r.updated_at DESC", new Object[] {appId});
        List<AppReviewDto> out = new ArrayList<AppReviewDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            out.add(reviewDto((Map) rows.get(iter)));
        }
        return out;
    }

    private AppReviewDto review(String id) throws IOException {
        return reviewDto(db.queryOne("SELECT r.id AS id, r.app_id AS app_id, "
                + "s.name AS store_name, r.rating AS rating, r.body AS body, "
                + "r.hidden AS hidden, r.created_at AS created_at FROM app_review r "
                + "JOIN store s ON s.id = r.store_id WHERE r.id = ?", new Object[] {id}));
    }

    private static AppReviewDto reviewDto(Map r) {
        return new AppReviewDto(Rows.text(r.get("id")), Rows.text(r.get("app_id")),
                Rows.text(r.get("store_name")), (int) Rows.number(r.get("rating")),
                Rows.text(r.get("body")), Rows.number(r.get("hidden")) != 0,
                Rows.number(r.get("created_at")));
    }

    private Map ownApp(String username, String appId) throws IOException {
        if (username == null || username.length() == 0) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        Map app = appId == null || appId.length() > 32 ? null
                : db.queryOne("SELECT * FROM app WHERE id = ?", new Object[] {appId});
        if (app == null || !username.equals(Rows.text(app.get("developer")))) {
            throw new ResponseStatusException(404, "No such app");
        }
        return app;
    }

    private List<AppListingDto> dtos(List rows) throws IOException {
        List<AppListingDto> out = new ArrayList<AppListingDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            out.add(dto((Map) rows.get(iter), null));
        }
        return out;
    }

    private AppListingDto dto(Map r, Map app) throws IOException {
        String appId = Rows.text(r.get("app_id"));
        Map a = app != null ? app : db.queryOne("SELECT client_id, app_url FROM app WHERE id = ?",
                new Object[] {appId});
        Map stats = db.queryOne("SELECT COUNT(*) AS n, SUM(rating) AS total FROM app_review "
                + "WHERE app_id = ? AND hidden = 0", new Object[] {appId});
        long n = stats == null ? 0 : Rows.number(stats.get("n"));
        long total = stats == null || stats.get("total") == null ? 0
                : Rows.number(stats.get("total"));
        List<String> shots = new ArrayList<String>();
        String joined = Rows.text(r.get("screenshots"));
        int from = 0;
        while (joined.length() > 0 && from <= joined.length()) {
            int nl = joined.indexOf('\n', from);
            String one = nl < 0 ? joined.substring(from) : joined.substring(from, nl);
            if (one.length() > 0) {
                shots.add(one);
            }
            if (nl < 0) {
                break;
            }
            from = nl + 1;
        }
        String clientId = a == null ? "" : Rows.text(a.get("client_id"));
        String appUrl = a == null ? "" : Rows.text(a.get("app_url"));
        return new AppListingDto(Rows.text(r.get("id")), appId, Rows.text(r.get("handle")),
                clientId, Rows.text(r.get("name")), Rows.text(r.get("tagline")),
                Rows.text(r.get("description")), Rows.text(r.get("category")),
                Rows.text(r.get("icon_url")), shots, Rows.text(r.get("pricing")),
                Rows.text(r.get("support_url")), Rows.text(r.get("privacy_url")), appUrl,
                Rows.text(r.get("developer")), Rows.text(r.get("status")),
                Rows.text(r.get("reason")), AppStoreText.tenths(total, n), (int) n,
                Rows.number(r.get("submitted_at")), Rows.number(r.get("reviewed_at")));
    }

    private static String required(String value, int max, String what) {
        String v = value == null ? "" : value.trim();
        if (v.length() == 0 || v.length() > max) {
            throw new ResponseStatusException(400, "The " + what + " is 1 to " + max
                    + " characters");
        }
        return v;
    }

    private static String optional(String value, int max) {
        String v = value == null ? "" : value.trim();
        if (v.length() > max) {
            throw new ResponseStatusException(400, "At most " + max + " characters");
        }
        return v;
    }

    /// An `https` address, or empty when `optional`.
    private static String url(String value, String what, boolean optional) {
        String v = value == null ? "" : value.trim();
        if (v.length() == 0 && optional) {
            return "";
        }
        if (v.length() > 500 || v.indexOf(' ') >= 0 || v.indexOf('"') >= 0
                || !v.regionMatches(true, 0, "https://", 0, 8)) {
            throw new ResponseStatusException(400, "The " + what + " is an https address");
        }
        return v;
    }

    private void audit(String storeId, String who, String action, String subject, String detail)
            throws IOException {
        AuditEvent e = new AuditEvent();
        e.setId(Ids.next());
        e.setStoreId(storeId);
        e.setUsername(Codes.cut(who, 190));
        e.setAction(action);
        e.setSubject(Codes.cut(subject, 64));
        e.setDetail(Codes.cut(detail, 500));
        e.setCreatedAt(Instant.now());
        audits.insert(e);
    }
}
