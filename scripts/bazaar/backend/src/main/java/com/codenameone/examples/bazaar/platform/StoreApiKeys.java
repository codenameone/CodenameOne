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
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.SecurityContext;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.apikey.ApiKey;
import com.codename1.backend.security.apikey.ApiKeyAuthenticationToken;
import com.codename1.backend.security.apikey.ApiKeyGenerator;
import com.codename1.backend.security.apikey.ApiKeyRepository;
import com.codename1.backend.security.apikey.GeneratedApiKey;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.ApiKeyDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/// A store's API keys: made by an owner, good for one store and the scopes
/// chosen, kept as a SHA-256 only, and withdrawn at once when revoked.
///
/// This is the [ApiKeyRepository] the API chain looks presented keys up in
/// (`SecurityConfig`). A key signs a request in as `apikey:<id>` with its
/// scopes as `SCOPE_x` authorities and nothing else: no role, so every
/// staff route (`hasRole(MERCHANT)`) refuses it, and a filter keeps it to the
/// key API under `/api/v1`. [#require] is what each route of that API asks:
/// the key, its store and the scope the route needs.
///
/// A key is 256 random bits, so a plain SHA-256 is as hard to reverse as the
/// key is to guess, and a lookup by that hash is what every request costs.
/// When it was last used is written at most once a minute per key, so a busy
/// integration does not turn every read into a write.
@Component
public class StoreApiKeys implements ApiKeyRepository {
    public static final String PREFIX = "bzk_";
    public static final String READ_PRODUCTS = "read_products";
    public static final String WRITE_PRODUCTS = "write_products";
    public static final String READ_ORDERS = "read_orders";
    public static final String WRITE_ORDERS = "write_orders";
    public static final String READ_CUSTOMERS = "read_customers";
    public static final String WRITE_CUSTOMERS = "write_customers";
    public static final String READ_INVENTORY = "read_inventory";
    public static final String WRITE_INVENTORY = "write_inventory";
    public static final String READ_DISCOUNTS = "read_discounts";
    public static final String WRITE_DISCOUNTS = "write_discounts";
    public static final String READ_FULFILLMENTS = "read_fulfillments";
    public static final String WRITE_FULFILLMENTS = "write_fulfillments";
    public static final String READ_METAFIELDS = "read_metafields";
    public static final String WRITE_METAFIELDS = "write_metafields";
    public static final String READ_ANALYTICS = "read_analytics";
    /// Every scope a key or an app may be given. A `write_x` scope also
    /// grants `read_x` ([#has]).
    public static final List<String> SCOPES = Collections.unmodifiableList(Arrays.asList(
            READ_PRODUCTS, WRITE_PRODUCTS, READ_ORDERS, WRITE_ORDERS, READ_CUSTOMERS,
            WRITE_CUSTOMERS, READ_INVENTORY, WRITE_INVENTORY, READ_DISCOUNTS, WRITE_DISCOUNTS,
            READ_FULFILLMENTS, WRITE_FULFILLMENTS, READ_METAFIELDS, WRITE_METAFIELDS,
            READ_ANALYTICS));
    /// What a key's owner name starts with: no account name has a colon.
    static final String OWNER_PREFIX = "apikey:";
    /// What an app installation's token signs in as: `app:<installationId>`.
    public static final String APP_OWNER_PREFIX = "app:";
    /// What an app installation's access token starts with. It is a key
    /// prefix too, so the same filter accepts it and asks [#findByHash].
    public static final String APP_TOKEN_PREFIX = PREFIX + "app_";
    /// How many live keys one store may hold.
    static final int MAX_KEYS = 25;
    private static final long TOUCH_MILLIS = 60000L;

    private final DataSource db;
    private final StoreAccess access;
    private final ApiKeyGenerator generator = new ApiKeyGenerator(PREFIX);

    public StoreApiKeys(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    /// A key a request presented, as the key API sees it.
    public static final class Key {
        /// The key's id, or the app installation's.
        public final String id;
        public final String storeId;
        public final List<String> scopes;
        /// The app the token was issued to, or null for a store's own key.
        public final String appId;

        Key(String id, String storeId, List<String> scopes, String appId) {
            this.id = id;
            this.storeId = storeId;
            this.scopes = scopes;
            this.appId = appId;
        }

        /// Who a record this request makes is made by: `apikey:<id>` or
        /// `app:<installationId>`.
        public String owner() {
            return (appId == null ? OWNER_PREFIX : APP_OWNER_PREFIX) + id;
        }
    }

    /// Whether `granted` covers `scope`: a `write_x` covers `read_x` too.
    public static boolean has(List<String> granted, String scope) {
        if (granted.contains(scope)) {
            return true;
        }
        return scope.startsWith("read_") && granted.contains("write_" + scope.substring(5));
    }

    // ------------------------------------------------------------- lookups

    @Override
    public ApiKey findByHash(String hash) {
        try {
            Map row = db.queryOne("SELECT id AS id, store_id AS store_id, scopes AS scopes, "
                    + "key_hash AS key_hash, prefix AS prefix, last_four AS last_four, "
                    + "revoked_at AS revoked_at, last_used_at AS last_used_at "
                    + "FROM store_api_key WHERE key_hash = ?", new Object[] {hash});
            if (row == null) {
                return appToken(hash);
            }
            String id = Rows.text(row.get("id"));
            long now = System.currentTimeMillis();
            boolean revoked = Rows.number(row.get("revoked_at")) > 0;
            if (!revoked && now - Rows.number(row.get("last_used_at")) > TOUCH_MILLIS) {
                db.execute("UPDATE store_api_key SET last_used_at = ? WHERE id = ?",
                        new Object[] {Long.valueOf(now), id});
            }
            return new ApiKey(id, OWNER_PREFIX + id, scopes(Rows.text(row.get("scopes"))),
                    Rows.text(row.get("key_hash")), Rows.text(row.get("prefix")),
                    Rows.text(row.get("last_four")), revoked);
        } catch (IOException failed) {
            // Refused, not let through: the filter answers 401 for null.
            return null;
        }
    }

    /// An app installation's access token, looked up by the same hash: an
    /// installation that was removed has no hash any more, so its token is
    /// unknown at once. The installation's scopes are read on every request,
    /// so a narrowed grant takes effect without a new token.
    private ApiKey appToken(String hash) throws IOException {
        Map row = db.queryOne("SELECT id AS id, scopes AS scopes, token_last_four AS last_four "
                + "FROM app_installation WHERE token_hash = ? AND status = ?",
                new Object[] {hash, Apps.INSTALLED});
        if (row == null) {
            return null;
        }
        String id = Rows.text(row.get("id"));
        return new ApiKey(id, APP_OWNER_PREFIX + id, scopes(Rows.text(row.get("scopes"))), hash,
                APP_TOKEN_PREFIX, Rows.text(row.get("last_four")), false);
    }

    /// The key this request was signed in with, its store and the scope the
    /// route needs: 401 for a request that was not made with a key, 404 for
    /// a store that is not the key's (as for a store that does not exist),
    /// and 403 for a key without the scope. An app installation's token is
    /// held to exactly the same checks, against the installation's grant.
    public Key require(String storeId, String scope) throws IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication a = context == null ? null : context.getAuthentication();
        if (!(a instanceof ApiKeyAuthenticationToken)) {
            throw new ResponseStatusException(401, "This API takes an API key");
        }
        ApiKey key = ((ApiKeyAuthenticationToken) a).getApiKey();
        String keyStore;
        String appId = null;
        List<String> granted;
        if (key.getOwner().startsWith(APP_OWNER_PREFIX)) {
            Map row = db.queryOne("SELECT store_id AS store_id, app_id AS app_id, "
                    + "scopes AS scopes, status AS status FROM app_installation WHERE id = ?",
                    new Object[] {key.getId()});
            if (row == null || !Apps.INSTALLED.equals(Rows.text(row.get("status")))) {
                throw new ResponseStatusException(401, "The access token is not valid");
            }
            keyStore = Rows.text(row.get("store_id"));
            appId = Rows.text(row.get("app_id"));
            granted = scopes(Rows.text(row.get("scopes")));
        } else {
            Map row = db.queryOne("SELECT store_id AS store_id, revoked_at AS revoked_at "
                    + "FROM store_api_key WHERE id = ?", new Object[] {key.getId()});
            if (row == null || Rows.number(row.get("revoked_at")) > 0) {
                throw new ResponseStatusException(401, "The API key is not valid");
            }
            keyStore = Rows.text(row.get("store_id"));
            granted = key.getScopes();
        }
        if (storeId == null || !keyStore.equals(storeId)) {
            throw new ResponseStatusException(404, "No such store");
        }
        if (scope != null && !has(granted, scope)) {
            throw new ResponseStatusException(403, (appId == null ? "The API key" : "The app")
                    + " lacks the scope " + scope);
        }
        return new Key(key.getId(), keyStore, granted, appId);
    }

    /// [#require], for a route only an app's token may call.
    public Key requireApp(String storeId, String scope) throws IOException {
        Key k = require(storeId, scope);
        if (k.appId == null) {
            throw new ResponseStatusException(403, "Only an app's access token may do this");
        }
        return k;
    }

    // ------------------------------------------------------------- owners

    @Transactional(readOnly = true)
    public List<ApiKeyDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        List rows = db.query("SELECT id AS id, name AS name, scopes AS scopes, prefix AS prefix, "
                + "last_four AS last_four, created_by AS created_by, created_at AS created_at, "
                + "last_used_at AS last_used_at, revoked_at AS revoked_at FROM store_api_key "
                + "WHERE store_id = ? ORDER BY created_at DESC", new Object[] {storeId});
        List<ApiKeyDto> out = new ArrayList<ApiKeyDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new ApiKeyDto(Rows.text(r.get("id")), Rows.text(r.get("name")),
                    scopes(Rows.text(r.get("scopes"))),
                    Rows.text(r.get("prefix")) + "..." + Rows.text(r.get("last_four")),
                    Rows.text(r.get("created_by")), Rows.number(r.get("created_at")),
                    Rows.number(r.get("last_used_at")), Rows.number(r.get("revoked_at")), ""));
        }
        return out;
    }

    /// Makes a key; the answer is the only place its secret ever appears.
    @Transactional(rollbackFor = IOException.class)
    public ApiKeyDto create(String username, String storeId, ApiKeyDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        String name = wanted == null || wanted.name() == null ? "" : wanted.name().trim();
        if (name.length() == 0) {
            throw new ResponseStatusException(400, "Name the key");
        }
        List<String> scopes = new ArrayList<String>();
        List<String> asked = wanted.scopes() == null ? new ArrayList<String>() : wanted.scopes();
        for (int iter = 0; iter < asked.size(); iter++) {
            String s = asked.get(iter) == null ? "" : asked.get(iter).trim();
            if (!SCOPES.contains(s)) {
                throw new ResponseStatusException(400, "There is no scope " + Codes.cut(s, 40));
            }
            if (!scopes.contains(s)) {
                scopes.add(s);
            }
        }
        if (scopes.isEmpty()) {
            throw new ResponseStatusException(400, "Give the key at least one scope");
        }
        Map live = db.queryOne("SELECT COUNT(*) AS n FROM store_api_key WHERE store_id = ? "
                + "AND revoked_at = 0", new Object[] {storeId});
        if (live != null && Rows.number(live.get("n")) >= MAX_KEYS) {
            throw new ResponseStatusException(400, "Revoke a key before making another");
        }
        String id = Ids.next();
        GeneratedApiKey made = generator.generate(OWNER_PREFIX + id,
                scopes.toArray(new String[scopes.size()]));
        ApiKey key = made.getApiKey();
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO store_api_key (id, store_id, name, scopes, key_hash, prefix, "
                + "last_four, created_by, created_at, last_used_at, revoked_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)", new Object[] {id, storeId,
                    Codes.cut(name, 80), join(scopes), key.getHash(), key.getPrefix(),
                    key.getLastFour(), username, Long.valueOf(now)});
        return new ApiKeyDto(id, Codes.cut(name, 80), scopes,
                key.getPrefix() + "..." + key.getLastFour(), username, now, 0, 0,
                made.getPlaintext());
    }

    @Transactional(rollbackFor = IOException.class)
    public List<ApiKeyDto> revoke(String username, String storeId, String keyId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER);
        Map row = keyId == null ? null : db.queryOne("SELECT store_id AS store_id FROM "
                + "store_api_key WHERE id = ?", new Object[] {keyId});
        StoreAccess.requireIn(row == null ? null : Rows.text(row.get("store_id")), storeId,
                "API key");
        db.execute("UPDATE store_api_key SET revoked_at = ? WHERE id = ? AND revoked_at = 0",
                new Object[] {Long.valueOf(System.currentTimeMillis()), keyId});
        return list(username, storeId);
    }

    static List<String> scopes(String stored) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        while (stored != null && start < stored.length()) {
            int comma = stored.indexOf(',', start);
            int end = comma < 0 ? stored.length() : comma;
            String s = stored.substring(start, end).trim();
            if (s.length() > 0) {
                out.add(s);
            }
            start = end + 1;
        }
        return out;
    }

    private static String join(List<String> scopes) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < scopes.size(); iter++) {
            if (iter > 0) {
                b.append(',');
            }
            b.append(scopes.get(iter));
        }
        return b.toString();
    }
}
