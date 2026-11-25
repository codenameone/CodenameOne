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

import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.apikey.ApiKeyGenerator;
import com.codename1.backend.security.apikey.GeneratedApiKey;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AppAuthorizeDto;
import com.codenameone.examples.bazaar.api.AppConsentDto;
import com.codenameone.examples.bazaar.api.AppDto;
import com.codenameone.examples.bazaar.api.AppInstallationDto;
import com.codenameone.examples.bazaar.api.AppRedirectDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.webhook.Webhooks;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Apps: separate services, in any language, that a store's owner installs.
///
/// An app is registered by a developer (any signed-in account) with the
/// addresses it may be sent back to and the most it may ask for. A store's
/// owner installs it through OAuth's authorization-code flow:
///
/// 1. The app sends the owner to the consent page with its `client_id`, the
///    scopes it wants, a `redirect_uri` it registered, a `state` and a PKCE
///    `code_challenge` (S256 only).
/// 2. The owner approves ([#authorize]); the browser returns to the app with a
///    code good once, for ten minutes, for exactly that request.
/// 3. The app trades the code, its client secret and the PKCE verifier for an
///    access token at the token endpoint ([#exchange]).
///
/// The token (`bzk_app_...`) is an API key as far as the security chain is
/// concerned: [StoreApiKeys] finds it by its hash and holds it to the same
/// store and scope checks a key is held to, against the installation's grant.
/// Asking again for more scopes shows only what is added and issues a new
/// token, which retires the old one. Removing the app clears the token's hash,
/// so it stops working with the next request, turns its webhooks off and
/// queues `app/uninstalled`; `shop/redact` follows 48 hours later
/// ([#sendShopRedacts]) unless the app is installed again first.
///
/// Secrets, tokens and codes are kept as SHA-256 only. A webhook cannot be
/// signed with a hash, so an app's webhooks are signed with a key derived
/// from its secret (`HMAC-SHA256(secret, "bazaar-webhooks")`, in hex), which
/// the app can compute and the database never sees the secret behind.
///
/// Everything a store's apps do to it is written to the store's audit log.
@Component
public class Apps {
    public static final String DRAFT = "DRAFT";
    public static final String LISTED = "LISTED";
    public static final String INSTALLED = "INSTALLED";
    public static final String UNINSTALLED = "UNINSTALLED";
    /// How long an authorization code is good for.
    static final long CODE_MILLIS = 10 * 60 * 1000L;
    /// How long after removal a store's data must be deleted: `shop/redact`.
    public static final long SHOP_REDACT_MILLIS = 48L * 60 * 60 * 1000;
    static final int MAX_APPS = 25;
    private static final String SECRET_PREFIX = "bzs_";
    private static final String SIGNING_CONTEXT = "bazaar-webhooks";

    private final DataSource db;
    private final StoreAccess access;
    private final StoreRepository stores;
    private final Webhooks webhooks;
    private final AuditEventRepository audits;
    private final ApiKeyGenerator tokens = new ApiKeyGenerator(StoreApiKeys.APP_TOKEN_PREFIX);

    public Apps(DataSource db, StoreAccess access, StoreRepository stores, Webhooks webhooks,
            AuditEventRepository audits) {
        this.db = db;
        this.access = access;
        this.stores = stores;
        this.webhooks = webhooks;
        this.audits = audits;
    }

    /// The webhook signing key of an app whose client secret is `secret`.
    public static String signingKey(String secret) throws IOException {
        return Ids.hex(Crypto.hmacSha256(secret.getBytes("UTF-8"),
                SIGNING_CONTEXT.getBytes("UTF-8")));
    }

    // ============================================================ developers

    @Transactional(readOnly = true)
    public List<AppDto> mine(String username) throws IOException {
        signedIn(username);
        List rows = db.query("SELECT * FROM app WHERE developer = ? ORDER BY created_at",
                new Object[] {username});
        List<AppDto> out = new ArrayList<AppDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            out.add(dto((Map) rows.get(iter), ""));
        }
        return out;
    }

    /// Registers an app; the answer is the only place its client secret appears.
    @Transactional(rollbackFor = IOException.class)
    public AppDto register(String username, AppDto wanted) throws IOException {
        signedIn(username);
        Map count = db.queryOne("SELECT COUNT(*) AS n FROM app WHERE developer = ?",
                new Object[] {username});
        if (count != null && Rows.number(count.get("n")) >= MAX_APPS) {
            throw new ResponseStatusException(400, "A developer registers at most " + MAX_APPS
                    + " apps");
        }
        Fields f = fields(wanted);
        // Short, because it names the app's metafield namespace.
        String id = Ids.hex(Crypto.randomBytes(6));
        String clientId = Ids.hex(Crypto.randomBytes(16));
        String secret = SECRET_PREFIX + OAuth2Parameters.random(32);
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO app (id, developer, client_id, secret_hash, signing_key, name, "
                + "description, icon_url, app_url, webhook_url, redirect_uris, scopes, status, "
                + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[] {id, username, clientId, OAuth2Parameters.sha256(secret),
                    signingKey(secret), f.name, f.description, f.icon, f.appUrl, f.webhookUrl,
                    f.redirects, f.scopes, f.status, Long.valueOf(now), Long.valueOf(now)});
        audit("", username, "APP_REGISTERED", id, f.name);
        return dto(app(id), secret);
    }

    @Transactional(rollbackFor = IOException.class)
    public AppDto update(String username, String appId, AppDto wanted) throws IOException {
        signedIn(username);
        Map row = appId == null ? null : app(appId);
        if (row == null || !username.equals(Rows.text(row.get("developer")))) {
            throw new ResponseStatusException(404, "No such app");
        }
        Fields f = fields(wanted);
        db.execute("UPDATE app SET name = ?, description = ?, icon_url = ?, app_url = ?, "
                + "webhook_url = ?, redirect_uris = ?, scopes = ?, status = ?, updated_at = ? "
                + "WHERE id = ?", new Object[] {f.name, f.description, f.icon, f.appUrl,
                    f.webhookUrl, f.redirects, f.scopes, f.status,
                    Long.valueOf(System.currentTimeMillis()), appId});
        audit("", username, "APP_UPDATED", appId, f.name + " " + f.status);
        return dto(app(appId), "");
    }

    private static final class Fields {
        String name;
        String description;
        String icon;
        String appUrl;
        String webhookUrl;
        String redirects;
        String scopes;
        String status;
    }

    private static Fields fields(AppDto in) {
        if (in == null) {
            throw new ResponseStatusException(400, "Describe the app");
        }
        Fields f = new Fields();
        f.name = Codes.cut(in.name(), 80);
        if (f.name.length() == 0) {
            throw new ResponseStatusException(400, "Name the app");
        }
        f.description = Codes.cut(in.description(), 1000);
        f.icon = Codes.cut(in.iconUrl(), 500);
        if (f.icon.length() > 0 && !web(f.icon)) {
            throw new ResponseStatusException(400, "The icon is an http or https address");
        }
        f.appUrl = Codes.cut(in.appUrl(), 500);
        if (!web(f.appUrl)) {
            throw new ResponseStatusException(400, "The app URL is an http or https address");
        }
        f.webhookUrl = Codes.cut(in.webhookUrl(), 500);
        if (f.webhookUrl.length() == 0) {
            f.webhookUrl = f.appUrl;
        }
        // The server posts to this address, so it is held to what a store's
        // own webhook is.
        String unsafe = web(f.webhookUrl)
                ? com.codenameone.examples.bazaar.web.security.UrlGuard.refuseByName(f.webhookUrl)
                : "The webhook URL is an http or https address";
        if (unsafe != null) {
            throw new ResponseStatusException(400, unsafe);
        }
        StringBuilder redirects = new StringBuilder();
        List<String> asked = in.redirectUris() == null ? new ArrayList<String>()
                : in.redirectUris();
        for (int iter = 0; iter < asked.size(); iter++) {
            String r = asked.get(iter) == null ? "" : asked.get(iter).trim();
            if (!web(r) || r.indexOf('#') >= 0 || r.indexOf(',') >= 0) {
                throw new ResponseStatusException(400, "A redirect URI is an http or https "
                        + "address with no fragment");
            }
            redirects.append(redirects.length() == 0 ? "" : "\n").append(r);
        }
        if (redirects.length() == 0 || asked.size() > 10 || redirects.length() > 2000) {
            throw new ResponseStatusException(400, "Give one to ten redirect URIs");
        }
        f.redirects = redirects.toString();
        List<String> scopes = checkScopes(in.scopes(), null);
        if (scopes.isEmpty()) {
            throw new ResponseStatusException(400, "Ask for at least one scope");
        }
        f.scopes = join(scopes);
        f.status = in.status() == null || in.status().length() == 0 ? DRAFT : in.status();
        if (!DRAFT.equals(f.status) && !LISTED.equals(f.status)) {
            throw new ResponseStatusException(400, "The status is DRAFT or LISTED");
        }
        return f;
    }

    private static boolean web(String url) {
        return url != null && url.length() <= 500 && url.indexOf(' ') < 0
                && (url.regionMatches(true, 0, "https://", 0, 8)
                    || url.regionMatches(true, 0, "http://", 0, 7));
    }

    /// `asked` as known scopes, each once; with `allowed`, each one of those too.
    private static List<String> checkScopes(List<String> asked, List<String> allowed) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; asked != null && iter < asked.size(); iter++) {
            String s = asked.get(iter) == null ? "" : asked.get(iter).trim();
            if (s.length() == 0) {
                continue;
            }
            if (!StoreApiKeys.SCOPES.contains(s)) {
                throw new ResponseStatusException(400, "There is no scope " + Codes.cut(s, 40));
            }
            if (allowed != null && !allowed.contains(s)) {
                throw new ResponseStatusException(400, "The app did not register the scope " + s);
            }
            if (!out.contains(s)) {
                out.add(s);
            }
        }
        return out;
    }

    // =============================================================== consent

    /// What the owner is asked: the app, the store and the scopes, with
    /// those already granted told apart from those approving would add.
    /// Checked in full, as [#authorize] checks it again.
    @Transactional(readOnly = true)
    public AppConsentDto consent(String username, String storeId, AppAuthorizeDto request)
            throws IOException {
        Request r = check(username, storeId, request);
        Store s = stores.findById(storeId);
        return new AppConsentDto(Rows.text(r.app.get("id")), Rows.text(r.app.get("name")),
                Rows.text(r.app.get("description")), Rows.text(r.app.get("icon_url")),
                Rows.text(r.app.get("developer")), storeId, s == null ? "" : s.getName(),
                r.scopes, r.granted, r.added, r.installed);
    }

    /// The owner's decision. Approving records a one-time code for exactly
    /// this request; refusing records nothing. Either way the answer is the
    /// app's own redirect address, which was checked against its registration
    /// before anything was answered -- an unknown client or address is a 400
    /// here, never a redirect.
    @Transactional(rollbackFor = IOException.class)
    public AppRedirectDto authorize(String username, String storeId, AppAuthorizeDto request)
            throws IOException {
        Request r = check(username, storeId, request);
        Map<String, String> q = new LinkedHashMap<String, String>();
        if (!request.approve()) {
            q.put("error", "access_denied");
        } else {
            String code = OAuth2Parameters.random(32);
            long now = System.currentTimeMillis();
            db.execute("DELETE FROM app_grant WHERE expires_at < ?",
                    new Object[] {Long.valueOf(now - CODE_MILLIS)});
            db.execute("INSERT INTO app_grant (code_hash, app_id, store_id, username, scopes, "
                    + "redirect_uri, code_challenge, expires_at, used_at, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)", new Object[] {
                        OAuth2Parameters.sha256(code), Rows.text(r.app.get("id")), storeId,
                        username, join(r.scopes), r.redirect, r.challenge,
                        Long.valueOf(now + CODE_MILLIS), Long.valueOf(now)});
            q.put("code", code);
            q.put("store_id", storeId);
        }
        if (request.state() != null && request.state().length() > 0) {
            q.put("state", Codes.cut(request.state(), 500));
        }
        return new AppRedirectDto(OAuth2Parameters.append(r.redirect, q));
    }

    /// A checked authorization request.
    private static final class Request {
        Map app;
        String redirect;
        String challenge;
        List<String> scopes;
        List<String> granted = new ArrayList<String>();
        List<String> added = new ArrayList<String>();
        boolean installed;
    }

    private Request check(String username, String storeId, AppAuthorizeDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        if (in == null) {
            throw new ResponseStatusException(400, "No authorization request");
        }
        Map app = in.clientId() == null || in.clientId().length() > 64 ? null
                : db.queryOne("SELECT * FROM app WHERE client_id = ?",
                        new Object[] {in.clientId()});
        // A draft is for its own developer to try in a store of theirs.
        if (app == null || !LISTED.equals(Rows.text(app.get("status")))
                && !username.equals(Rows.text(app.get("developer")))) {
            throw new ResponseStatusException(400, "There is no such app");
        }
        Request r = new Request();
        r.app = app;
        r.redirect = in.redirectUri() == null ? "" : in.redirectUri();
        if (!StoreApiKeys.scopes(Rows.text(app.get("redirect_uris")).replace('\n', ','))
                .contains(r.redirect)) {
            throw new ResponseStatusException(400, "The redirect URI is not one the app "
                    + "registered");
        }
        if (!"S256".equals(in.codeChallengeMethod())) {
            throw new ResponseStatusException(400, "PKCE is required, with S256");
        }
        r.challenge = in.codeChallenge() == null ? "" : in.codeChallenge();
        if (r.challenge.length() < 43 || r.challenge.length() > 128) {
            throw new ResponseStatusException(400, "The code challenge is 43 to 128 characters");
        }
        List<String> registered = StoreApiKeys.scopes(Rows.text(app.get("scopes")));
        Set<String> asked = OAuth2Parameters.scopes(in.scope());
        r.scopes = asked.isEmpty() ? registered
                : checkScopes(new ArrayList<String>(asked), registered);
        Map inst = installation(Rows.text(app.get("id")), storeId);
        r.installed = inst != null && INSTALLED.equals(Rows.text(inst.get("status")));
        if (r.installed) {
            r.granted = StoreApiKeys.scopes(Rows.text(inst.get("scopes")));
        }
        for (int iter = 0; iter < r.scopes.size(); iter++) {
            if (!r.granted.contains(r.scopes.get(iter))) {
                r.added.add(r.scopes.get(iter));
            }
        }
        return r;
    }

    // ================================================================= token

    /// Thrown for a token request that fails, with OAuth's error code.
    public static final class TokenError extends Exception {
        public final int status;
        public final String error;

        TokenError(int status, String error, String description) {
            super(description);
            this.status = status;
            this.error = error;
        }
    }

    /// Trades a code for an access token. The code is spent by the first
    /// attempt whatever its outcome, so a wrong PKCE verifier cannot be
    /// followed by a right one. Answers OAuth's token response.
    public Map<String, Object> exchange(String clientId, String clientSecret, String code,
            String verifier, String redirectUri) throws IOException, TokenError {
        Map app = clientId == null || clientId.length() > 64 ? null : appByClient(clientId);
        if (app == null || clientSecret == null || !OAuth2Parameters.equalsConstantTime(
                OAuth2Parameters.sha256(clientSecret), Rows.text(app.get("secret_hash")))) {
            throw new TokenError(401, "invalid_client", "The client is not known");
        }
        Map grant = code == null || code.length() > 100 ? null
                : spend(OAuth2Parameters.sha256(code));
        if (grant == null || Rows.number(grant.get("expires_at")) < System.currentTimeMillis()
                || !Rows.text(app.get("id")).equals(Rows.text(grant.get("app_id")))) {
            throw new TokenError(400, "invalid_grant", "The code is not valid");
        }
        if (!Rows.text(grant.get("redirect_uri")).equals(redirectUri)) {
            throw new TokenError(400, "invalid_grant", "The redirect URI does not match");
        }
        if (verifier == null || verifier.length() < 43 || verifier.length() > 128
                || !OAuth2Parameters.equalsConstantTime(OAuth2Parameters.sha256(verifier),
                        Rows.text(grant.get("code_challenge")))) {
            throw new TokenError(400, "invalid_grant", "The code verifier does not match");
        }
        return install(app, grant);
    }

    @Transactional(rollbackFor = IOException.class)
    private Map spend(String codeHash) throws IOException {
        int spent = db.execute("UPDATE app_grant SET used_at = ? WHERE code_hash = ? "
                + "AND used_at = 0", new Object[] {Long.valueOf(System.currentTimeMillis()),
                    codeHash});
        return spent == 1 ? db.queryOne("SELECT * FROM app_grant WHERE code_hash = ?",
                new Object[] {codeHash}) : null;
    }

    @Transactional(readOnly = true)
    private Map appByClient(String clientId) throws IOException {
        return db.queryOne("SELECT * FROM app WHERE client_id = ?", new Object[] {clientId});
    }

    @Transactional(rollbackFor = IOException.class)
    private Map<String, Object> install(Map app, Map grant) throws IOException {
        String appId = Rows.text(app.get("id"));
        String storeId = Rows.text(grant.get("store_id"));
        String by = Rows.text(grant.get("username"));
        List<String> scopes = StoreApiKeys.scopes(Rows.text(grant.get("scopes")));
        Map inst = installation(appId, storeId);
        boolean upgrade = inst != null && INSTALLED.equals(Rows.text(inst.get("status")));
        if (upgrade) {
            // Re-consent adds to the grant; it never quietly takes away.
            List<String> had = StoreApiKeys.scopes(Rows.text(inst.get("scopes")));
            for (int iter = 0; iter < scopes.size(); iter++) {
                if (!had.contains(scopes.get(iter))) {
                    had.add(scopes.get(iter));
                }
            }
            scopes = had;
        }
        GeneratedApiKey made = tokens.generate(StoreApiKeys.APP_OWNER_PREFIX + appId,
                scopes.toArray(new String[scopes.size()]));
        long now = System.currentTimeMillis();
        String id;
        if (inst == null) {
            id = Ids.next();
            db.execute("INSERT INTO app_installation (id, app_id, store_id, scopes, token_hash, "
                    + "token_last_four, status, installed_by, installed_at, uninstalled_at, "
                    + "shop_redact_due_at, shop_redact_sent_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0)", new Object[] {id, appId,
                        storeId, join(scopes), made.getApiKey().getHash(),
                        made.getApiKey().getLastFour(), INSTALLED, by, Long.valueOf(now)});
        } else {
            id = Rows.text(inst.get("id"));
            // A new token every time: the one an earlier consent produced
            // stops working now. Installing again within 48 hours of removal
            // also calls off the pending shop/redact.
            db.execute("UPDATE app_installation SET scopes = ?, token_hash = ?, "
                    + "token_last_four = ?, status = ?, installed_by = ?, installed_at = ?, "
                    + "uninstalled_at = 0, shop_redact_due_at = 0 WHERE id = ?",
                    new Object[] {join(scopes), made.getApiKey().getHash(),
                        made.getApiKey().getLastFour(), INSTALLED, by,
                        upgrade ? inst.get("installed_at") : Long.valueOf(now), id});
        }
        webhooks.appActivate(storeId, id, Rows.text(app.get("webhook_url")),
                Rows.text(app.get("signing_key")));
        audit(storeId, by, upgrade ? "APP_SCOPES_GRANTED" : "APP_INSTALLED", appId,
                Rows.text(app.get("name")) + ": " + join(scopes));
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("access_token", made.getPlaintext());
        out.put("token_type", "bearer");
        out.put("scope", OAuth2Parameters.scopes(scopes));
        out.put("store_id", storeId);
        return out;
    }

    // ============================================================== merchant

    @Transactional(readOnly = true)
    public List<AppInstallationDto> installed(String username, String storeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        List rows = db.query("SELECT i.id AS id, i.app_id AS app_id, a.name AS name, "
                + "a.icon_url AS icon_url, a.app_url AS app_url, i.scopes AS scopes, "
                + "i.status AS status, i.installed_by AS installed_by, "
                + "i.installed_at AS installed_at, i.uninstalled_at AS uninstalled_at "
                + "FROM app_installation i JOIN app a ON a.id = i.app_id WHERE i.store_id = ? "
                + "ORDER BY i.installed_at DESC", new Object[] {storeId});
        List<AppInstallationDto> out = new ArrayList<AppInstallationDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new AppInstallationDto(Rows.text(r.get("id")), Rows.text(r.get("app_id")),
                    Rows.text(r.get("name")), Rows.text(r.get("icon_url")),
                    Rows.text(r.get("app_url")), StoreApiKeys.scopes(Rows.text(r.get("scopes"))),
                    Rows.text(r.get("status")), Rows.text(r.get("installed_by")),
                    Rows.number(r.get("installed_at")), Rows.number(r.get("uninstalled_at"))));
        }
        return out;
    }

    /// Removes an app from a store: the token is withdrawn at once (its hash
    /// is cleared), `app/uninstalled` is queued while the hooks are turned
    /// off, and `shop/redact` falls due in 48 hours.
    @Transactional(rollbackFor = IOException.class)
    public List<AppInstallationDto> uninstall(String username, String storeId,
            String installationId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        Map inst = installationId == null || installationId.length() > 32 ? null
                : db.queryOne("SELECT * FROM app_installation WHERE id = ?",
                        new Object[] {installationId});
        StoreAccess.requireIn(inst == null ? null : Rows.text(inst.get("store_id")), storeId,
                "app");
        if (INSTALLED.equals(Rows.text(inst.get("status")))) {
            long now = System.currentTimeMillis();
            db.execute("UPDATE app_installation SET status = ?, token_hash = '', "
                    + "uninstalled_at = ?, shop_redact_due_at = ?, shop_redact_sent_at = 0 "
                    + "WHERE id = ?", new Object[] {UNINSTALLED, Long.valueOf(now),
                        Long.valueOf(now + SHOP_REDACT_MILLIS), installationId});
            webhooks.appDeactivate(installationId);
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("store_id", storeId);
            payload.put("app_id", Rows.text(inst.get("app_id")));
            payload.put("uninstalled_at", Long.valueOf(now));
            webhooks.appQueue(installationId, Webhooks.APP_UNINSTALLED, Json.write(payload));
            audit(storeId, username, "APP_UNINSTALLED", Rows.text(inst.get("app_id")),
                    "Token revoked");
        }
        return installed(username, storeId);
    }

    // =============================================================== privacy

    /// `customers/data_request` to every app installed in the store: a
    /// customer asked for their data, and each app must send what it holds.
    public void customerDataRequest(String storeId, String customerId, String requestId)
            throws IOException {
        privacy(storeId, Webhooks.CUSTOMERS_DATA_REQUEST, customerId, requestId);
    }

    /// `customers/redact` to every app installed in the store: the customer's
    /// personal data was erased here, and each app must erase its copy.
    public void customerRedact(String storeId, String customerId) throws IOException {
        privacy(storeId, Webhooks.CUSTOMERS_REDACT, customerId, "");
    }

    private void privacy(String storeId, String topic, String customerId, String requestId)
            throws IOException {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("store_id", storeId);
        payload.put("customer_id", customerId);
        if (requestId.length() > 0) {
            payload.put("data_request_id", requestId);
        }
        // Only installed apps' mandatory hooks are on, so emit reaches them alone.
        int sent = webhooks.emit(storeId, topic, Json.write(payload));
        if (sent > 0) {
            audit(storeId, "", "APP_PRIVACY_WEBHOOK", customerId, topic + " to " + sent
                    + " app(s)");
        }
    }

    /// Sends `shop/redact` to every app removed 48 hours ago and not
    /// installed again since.
    @Scheduled(fixedDelay = 600000, initialDelay = 60000, lock = "bazaar-app-redact")
    public void shopRedacts() throws IOException {
        sendShopRedacts(System.currentTimeMillis());
    }

    /// [#shopRedacts] as of `now`; answers how many were queued. Public so
    /// that tests can run the clock forward.
    @Transactional(rollbackFor = IOException.class)
    public int sendShopRedacts(long now) throws IOException {
        List rows = db.query("SELECT id AS id, store_id AS store_id, app_id AS app_id "
                + "FROM app_installation WHERE status = ? AND shop_redact_due_at > 0 "
                + "AND shop_redact_due_at <= ? AND shop_redact_sent_at = 0",
                new Object[] {UNINSTALLED, Long.valueOf(now)});
        int sent = 0;
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String id = Rows.text(r.get("id"));
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("store_id", Rows.text(r.get("store_id")));
            payload.put("app_id", Rows.text(r.get("app_id")));
            webhooks.appQueue(id, Webhooks.SHOP_REDACT, Json.write(payload));
            db.execute("UPDATE app_installation SET shop_redact_sent_at = ? WHERE id = ?",
                    new Object[] {Long.valueOf(now), id});
            audit(Rows.text(r.get("store_id")), "", "APP_SHOP_REDACT",
                    Rows.text(r.get("app_id")), "shop/redact queued");
            sent++;
        }
        return sent;
    }

    // ================================================================ shared

    /// The signing key of an installation's app, for its own subscriptions.
    @Transactional(readOnly = true)
    public String signingKeyOf(String appId) throws IOException {
        Map row = app(appId);
        return row == null ? "" : Rows.text(row.get("signing_key"));
    }

    /// Writes `action` to the store's audit log, as `who`.
    public void audit(String storeId, String who, String action, String subject, String detail)
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

    private Map app(String id) throws IOException {
        return db.queryOne("SELECT * FROM app WHERE id = ?", new Object[] {id});
    }

    private Map installation(String appId, String storeId) throws IOException {
        return db.queryOne("SELECT * FROM app_installation WHERE app_id = ? AND store_id = ?",
                new Object[] {appId, storeId});
    }

    private static void signedIn(String username) {
        if (username == null || username.length() == 0) {
            throw new ResponseStatusException(401, "Sign in first");
        }
    }

    private static AppDto dto(Map r, String secret) {
        return new AppDto(Rows.text(r.get("id")), Rows.text(r.get("client_id")),
                Rows.text(r.get("name")), Rows.text(r.get("description")),
                Rows.text(r.get("icon_url")), Rows.text(r.get("app_url")),
                Rows.text(r.get("webhook_url")),
                StoreApiKeys.scopes(Rows.text(r.get("redirect_uris")).replace('\n', ',')),
                StoreApiKeys.scopes(Rows.text(r.get("scopes"))), Rows.text(r.get("status")),
                Rows.text(r.get("developer")), Rows.number(r.get("created_at")), secret);
    }

    static String join(List<String> items) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < items.size(); iter++) {
            b.append(iter == 0 ? "" : ",").append(items.get(iter));
        }
        return b.toString();
    }
}
