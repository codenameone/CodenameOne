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
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AppChargeDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.ops.Amounts;
import com.codenameone.examples.bazaar.ops.AppStoreText;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.webhook.Webhooks;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// App billing: what an app charges a store, and the store owner's answer.
///
/// An app asks with its token (one-time, or recurring every 30 days with an
/// optional trial); the charge waits as `PENDING` until the owner approves
/// or declines it, in the client or on the charge's web page
/// ([AppChargePage]). Approving records what the platform owes the app's
/// developer (`app_payout_entry`, due at once for a one-time charge and when
/// the trial ends for a recurring one). Removing the app cancels its
/// pending and active recurring charges. Every change of status is sent to
/// the app as `app_subscriptions/update`.
///
/// Depends on nothing of [Apps], which calls [#cancelAll] from its removal.
@Component
public class AppBilling {
    public static final String PENDING = "PENDING";
    public static final String ACTIVE = "ACTIVE";
    public static final String DECLINED = "DECLINED";
    public static final String CANCELLED = "CANCELLED";
    /// The most one charge may be, in minor units.
    static final long MAX_AMOUNT = 100000000L;

    private final DataSource db;
    private final StoreAccess access;
    private final Webhooks webhooks;
    private final AuditEventRepository audits;

    public AppBilling(DataSource db, StoreAccess access, Webhooks webhooks,
            AuditEventRepository audits) {
        this.db = db;
        this.access = access;
        this.webhooks = webhooks;
        this.audits = audits;
    }

    /// The web page where the owner decides on charge `id` of `storeId`.
    public static String confirmPath(String storeId, String id) {
        return AppChargePage.PATH + "/" + id + "?store=" + storeId;
    }

    // ================================================================== apps

    /// A charge an app asks for, through its key-API token.
    @Transactional(rollbackFor = IOException.class)
    public AppChargeDto create(StoreApiKeys.Key k, AppChargeDto in) throws IOException {
        if (in == null) {
            throw new ResponseStatusException(400, "No charge");
        }
        String name = in.name() == null ? "" : in.name().trim();
        if (name.length() == 0 || name.length() > 120) {
            throw new ResponseStatusException(400, "The name is 1 to 120 characters");
        }
        String kind = in.kind() == null ? "" : in.kind();
        if (!AppStoreText.ONE_TIME.equals(kind) && !AppStoreText.RECURRING.equals(kind)) {
            throw new ResponseStatusException(400, "The kind is ONE_TIME or RECURRING");
        }
        String currency = in.currency() == null ? "" : in.currency();
        if (!currency(currency)) {
            throw new ResponseStatusException(400, "The currency is three capital letters");
        }
        if (in.amount() <= 0 || in.amount() > MAX_AMOUNT) {
            throw new ResponseStatusException(400, "The amount is a positive number of "
                    + "minor units");
        }
        int trial = in.trialDays();
        if (trial < 0 || trial > AppStoreText.MAX_TRIAL_DAYS
                || trial > 0 && !AppStoreText.RECURRING.equals(kind)) {
            throw new ResponseStatusException(400, "A trial is 0 to "
                    + AppStoreText.MAX_TRIAL_DAYS + " days, on a recurring charge");
        }
        String back = in.returnUrl() == null ? "" : in.returnUrl().trim();
        if (back.length() > 500 || back.length() > 0
                && !back.regionMatches(true, 0, "https://", 0, 8)) {
            throw new ResponseStatusException(400, "The return URL is an https address");
        }
        String id = Ids.next();
        db.execute("INSERT INTO app_charge (id, app_id, installation_id, store_id, name, kind, "
                + "amount, currency, trial_days, status, return_url, decided_by, created_at, "
                + "decided_at, billing_starts_at, cancelled_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '', ?, 0, 0, 0)", new Object[] {id,
                    k.appId, k.id, k.storeId, name, kind, Long.valueOf(in.amount()), currency,
                    Integer.valueOf(trial), PENDING, back,
                    Long.valueOf(System.currentTimeMillis())});
        audit(k.storeId, k.owner(), "APP_CHARGE_CREATED", k.appId, name + " "
                + AppStoreText.terms(kind, in.amount(), currency, trial));
        return charge(id);
    }

    /// The app's own charges in the store.
    @Transactional(readOnly = true)
    public List<AppChargeDto> ofInstallation(StoreApiKeys.Key k) throws IOException {
        return list("c.installation_id = ?", k.id);
    }

    // ============================================================== merchant

    @Transactional(readOnly = true)
    public List<AppChargeDto> charges(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        return list("c.store_id = ?", storeId);
    }

    /// One of the store's charges, for its owner.
    @Transactional(readOnly = true)
    public AppChargeDto charge(String username, String storeId, String chargeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        return charge(inStore(storeId, chargeId));
    }

    /// The owner's answer to a pending charge.
    @Transactional(rollbackFor = IOException.class)
    public AppChargeDto decide(String username, String storeId, String chargeId,
            boolean approve) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        String id = inStore(storeId, chargeId);
        Map c = db.queryOne("SELECT c.*, a.developer AS developer FROM app_charge c "
                + "JOIN app a ON a.id = c.app_id WHERE c.id = ?", new Object[] {id});
        if (!PENDING.equals(Rows.text(c.get("status")))) {
            throw new ResponseStatusException(409, "The charge was already answered");
        }
        Map inst = db.queryOne("SELECT status FROM app_installation WHERE id = ?",
                new Object[] {Rows.text(c.get("installation_id"))});
        if (approve && (inst == null || !Apps.INSTALLED.equals(Rows.text(inst.get("status"))))) {
            throw new ResponseStatusException(409, "The app is not installed");
        }
        long now = System.currentTimeMillis();
        String next = approve ? ACTIVE : DECLINED;
        long starts = 0;
        if (approve) {
            starts = AppStoreText.RECURRING.equals(Rows.text(c.get("kind")))
                    ? AppStoreText.billingStarts(now, (int) Rows.number(c.get("trial_days")))
                    : now;
            db.execute("INSERT INTO app_payout_entry (id, charge_id, app_id, store_id, "
                    + "developer, amount, currency, due_at, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), id,
                        Rows.text(c.get("app_id")), storeId, Rows.text(c.get("developer")),
                        Long.valueOf(Rows.number(c.get("amount"))),
                        Rows.text(c.get("currency")), Long.valueOf(starts), Long.valueOf(now)});
        }
        db.execute("UPDATE app_charge SET status = ?, decided_by = ?, decided_at = ?, "
                + "billing_starts_at = ? WHERE id = ?", new Object[] {next, username,
                    Long.valueOf(now), Long.valueOf(starts), id});
        notifyApp(id);
        audit(storeId, username, approve ? "APP_CHARGE_APPROVED" : "APP_CHARGE_DECLINED",
                Rows.text(c.get("app_id")), Rows.text(c.get("name")));
        return charge(id);
    }

    /// Cancels an installation's pending charges and active recurring ones,
    /// as its app is removed; answers how many. A one-time charge already
    /// approved was paid for and stays as it is.
    @Transactional(rollbackFor = IOException.class)
    public int cancelAll(String installationId, String who) throws IOException {
        List rows = db.query("SELECT id, store_id, app_id, kind, status FROM app_charge "
                + "WHERE installation_id = ? AND status IN (?, ?)",
                new Object[] {installationId, PENDING, ACTIVE});
        int n = 0;
        long now = System.currentTimeMillis();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            if (ACTIVE.equals(Rows.text(r.get("status")))
                    && !AppStoreText.RECURRING.equals(Rows.text(r.get("kind")))) {
                continue;
            }
            String id = Rows.text(r.get("id"));
            db.execute("UPDATE app_charge SET status = ?, cancelled_at = ? WHERE id = ?",
                    new Object[] {CANCELLED, Long.valueOf(now), id});
            notifyApp(id);
            audit(Rows.text(r.get("store_id")), who, "APP_CHARGE_CANCELLED",
                    Rows.text(r.get("app_id")), id);
            n++;
        }
        return n;
    }

    // ================================================================ shared

    /// `app_subscriptions/update` to the charge's app, through its
    /// installation's hook, which is there whether the app is installed or not.
    private void notifyApp(String id) throws IOException {
        AppChargeDto c = charge(id);
        Map row = db.queryOne("SELECT installation_id, store_id FROM app_charge WHERE id = ?",
                new Object[] {id});
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("store_id", Rows.text(row.get("store_id")));
        payload.put("app_id", c.appId());
        payload.put("charge_id", c.id());
        payload.put("name", c.name());
        payload.put("kind", c.kind());
        payload.put("amount", Long.valueOf(c.amount()));
        payload.put("currency", c.currency());
        payload.put("trial_days", Integer.valueOf(c.trialDays()));
        payload.put("status", c.status());
        payload.put("billing_starts_at", Long.valueOf(c.billingStartsAt()));
        webhooks.appQueue(Rows.text(row.get("installation_id")),
                Webhooks.APP_SUBSCRIPTIONS_UPDATE, Json.write(payload));
    }

    private String inStore(String storeId, String chargeId) throws IOException {
        Map c = chargeId == null || chargeId.length() > 32 ? null
                : db.queryOne("SELECT store_id FROM app_charge WHERE id = ?",
                        new Object[] {chargeId});
        StoreAccess.requireIn(c == null ? null : Rows.text(c.get("store_id")), storeId,
                "charge");
        return chargeId;
    }

    private List<AppChargeDto> list(String where, String value) throws IOException {
        List rows = db.query(SELECT + where + " ORDER BY c.created_at DESC",
                new Object[] {value});
        List<AppChargeDto> out = new ArrayList<AppChargeDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            out.add(dto((Map) rows.get(iter)));
        }
        return out;
    }

    private static final String SELECT = "SELECT c.id AS id, c.app_id AS app_id, "
            + "a.name AS app_name, c.store_id AS store_id, c.name AS name, c.kind AS kind, "
            + "c.amount AS amount, c.currency AS currency, c.trial_days AS trial_days, "
            + "c.status AS status, c.return_url AS return_url, c.created_at AS created_at, "
            + "c.decided_at AS decided_at, c.billing_starts_at AS billing_starts_at "
            + "FROM app_charge c JOIN app a ON a.id = c.app_id WHERE ";

    private AppChargeDto charge(String id) throws IOException {
        return dto(db.queryOne(SELECT + "c.id = ?", new Object[] {id}));
    }

    private static AppChargeDto dto(Map r) {
        String id = Rows.text(r.get("id"));
        return new AppChargeDto(id, Rows.text(r.get("app_id")), Rows.text(r.get("app_name")),
                Rows.text(r.get("name")), Rows.text(r.get("kind")),
                Rows.number(r.get("amount")), Rows.text(r.get("currency")),
                (int) Rows.number(r.get("trial_days")), Rows.text(r.get("status")),
                Rows.text(r.get("return_url")),
                confirmPath(Rows.text(r.get("store_id")), id), Rows.number(r.get("created_at")),
                Rows.number(r.get("decided_at")), Rows.number(r.get("billing_starts_at")));
    }

    /// Three capital letters, and a currency the money rules know the digits of.
    static boolean currency(String code) {
        if (code.length() != 3) {
            return false;
        }
        for (int iter = 0; iter < 3; iter++) {
            char c = code.charAt(iter);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        return Amounts.digits(code) >= 0;
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
