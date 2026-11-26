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
package com.codenameone.examples.bazaar.extensions;

import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.Jwt;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.WebClient;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AppSessionDto;
import com.codenameone.examples.bazaar.api.BlockValueDto;
import com.codenameone.examples.bazaar.api.ExtensionActionDto;
import com.codenameone.examples.bazaar.api.ExtensionActionResultDto;
import com.codenameone.examples.bazaar.api.ExtensionBlockDto;
import com.codenameone.examples.bazaar.api.FieldAnswerDto;
import com.codenameone.examples.bazaar.api.OrderFieldDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.ops.Blocks;
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.platform.StoreApiKeys;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.security.UrlGuard;
import com.codenameone.examples.bazaar.webhook.Webhooks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// App UI extensions: what installed apps uploaded ([ExtensionUploadApi])
/// and what Bazaar does with it. All of it is declarative -- a block tree
/// of [Blocks]' vocabulary -- and nothing an app wrote runs here.
///
/// - **Admin blocks** are rendered natively by the merchant app. A block
///   with a data URL has its `{{placeholders}}` filled by a signed POST to
///   the app, made here with a short timeout and cached for
///   [#DATA_CACHE_MILLIS]; a block whose app does not answer shows without
///   its data and says so (`error`).
/// - **Actions** are posted to the app's action URL by the server, signed,
///   with the product, order or customer they were pressed on.
/// - **Storefront blocks** print into the storefront's places as escaped
///   text; **checkout blocks** may ask for fields, whose answers are checked
///   by kind and kept on the order beside the store's own checkout fields.
///
/// Every call to an app is a POST whose `X-Bazaar-Signature` is
/// `sha256=<hex HMAC-SHA256 of the body>` keyed with the app's signing key,
/// exactly as its webhooks are; the body carries the store, the resource
/// and a `timestamp` (epoch milliseconds) so an app can refuse a replay.
///
/// Extensions belong to the installation that uploaded them: they are read
/// only while it is installed with `write_extensions` and are deleted when
/// the app is removed ([Apps#uninstall]).
@Component
public class Extensions {
    static final long DATA_CACHE_MILLIS = 30 * 1000L;
    static final int DATA_TIMEOUT_MILLIS = 800;
    static final int ACTION_TIMEOUT_MILLIS = 3000;
    static final int MAX_REPLY = 32 * 1024;
    static final int MAX_PER_APP = 30;
    private static final int CACHE_SIZE = 256;
    /// What a field answer's id starts with: `app:<extensionId>:<key>`.
    public static final String FIELD_PREFIX = "app:";

    /// What an app answered.
    public static final class Reply {
        public final int status;
        public final String contentType;
        public final byte[] body;

        public Reply(int status, String contentType, byte[] body) {
            this.status = status;
            this.contentType = contentType == null ? "" : contentType;
            this.body = body == null ? new byte[0] : body;
        }

        public String text() throws IOException {
            return new String(body, "UTF-8");
        }

        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    /// How an app is reached; answers its reply or throws. A transport must
    /// give up after `timeoutMillis` and refuse a body over `maxBytes`.
    public interface Transport {
        Reply send(String method, String url, List<String> headers, byte[] body,
                int timeoutMillis, int maxBytes) throws IOException;
    }

    /// HTTPS with every timeout set to the budget and the address checked
    /// against private networks when it is resolved.
    static final class HttpTransport implements Transport {
        @Override
        public Reply send(String method, String url, List<String> headers, byte[] body,
                int timeoutMillis, int maxBytes) throws IOException {
            String unsafe = UrlGuard.refuse(url, UrlGuard.DNS);
            if (unsafe != null) {
                throw new IOException(unsafe);
            }
            WebClient client = new WebClient().setConnectTimeoutMillis(timeoutMillis)
                    .setReadTimeoutMillis(timeoutMillis).setRequestTimeoutMillis(timeoutMillis);
            try {
                com.codename1.backend.Web.Result r = client.request(method, url,
                        new ArrayList<String>(headers), body);
                byte[] reply = r.getBody();
                if (reply != null && reply.length > maxBytes) {
                    throw new IOException("The app's answer is too long");
                }
                return new Reply(r.getStatus(), r.getHeader("Content-Type"), reply);
            } finally {
                client.close();
            }
        }
    }

    /// One stored extension.
    static final class Ext {
        String id;
        String storeId;
        String installationId;
        String appId;
        String appName;
        String target;
        String place;
        String title;
        String tree;
        String dataUrl;
        String actionUrl;
        long updatedAt;
    }

    private static final class Cached {
        final long at;
        final Map<String, String> values;

        Cached(long at, Map<String, String> values) {
            this.at = at;
            this.values = values;
        }
    }

    private final DataSource db;
    private final Apps apps;
    private final StoreAccess access;
    private Transport transport = new HttpTransport();
    private final Map<String, Cached> cache = new LinkedHashMap<String, Cached>();
    private long dataCalls;

    public Extensions(DataSource db, Apps apps, StoreAccess access) {
        this.db = db;
        this.apps = apps;
        this.access = access;
    }

    /// Replaces how apps are reached; for tests.
    public void useTransport(Transport transport) {
        this.transport = transport;
    }

    /// Data calls made so far (a cached answer is not one).
    public synchronized long dataCalls() {
        return dataCalls;
    }

    /// Forgets cached data; for tests.
    public synchronized void clearCache() {
        cache.clear();
    }

    Transport transport() {
        return transport;
    }

    /// Bumps the store's storefront version, so a cached page is not served
    /// with blocks it no longer has (the slots' ETag salt).
    public static void touch(DataSource db, String storeId) throws IOException {
        int n = db.execute("UPDATE app_slot_version SET version = version + 1 WHERE store_id = ?",
                new Object[] {storeId});
        if (n == 0) {
            db.execute("INSERT INTO app_slot_version (store_id, version) VALUES (?, 1)",
                    new Object[] {storeId});
        }
    }

    // ------------------------------------------------------------- upload

    private static String field(Map in, String name) {
        Object v = in.get(name);
        return v instanceof String ? ((String) v).trim() : "";
    }

    /// Checks and stores an extension `k`'s app uploaded; answers its row as
    /// the upload API shows it. Anything wrong is a 400 naming it.
    public Map<String, Object> upload(StoreApiKeys.Key k, Map in) throws IOException {
        String target = field(in, "target");
        if (!Blocks.isTarget(target)) {
            throw new ResponseStatusException(400, "The target is one of "
                    + join(Blocks.TARGETS));
        }
        String title = field(in, "title");
        if (title.length() > 80) {
            throw new ResponseStatusException(400, "The title is at most 80 characters");
        }
        Object tree = in.get("tree");
        if (!(tree instanceof Map)) {
            throw new ResponseStatusException(400, "Send the block tree as a JSON object");
        }
        String json = Json.write(tree);
        Blocks.Node root;
        try {
            root = Blocks.parse(json);
            Blocks.checkTarget(target, root);
        } catch (Blocks.Invalid bad) {
            throw new ResponseStatusException(400, bad.getMessage());
        }
        String place = field(in, "place");
        if (Blocks.STOREFRONT_BLOCK.equals(target) ? !Blocks.isPlace(place)
                : place.length() > 0) {
            throw new ResponseStatusException(400, "A storefront block names its place, one of "
                    + join(Blocks.PLACES) + "; no other target has one");
        }
        String dataUrl = address(in, "dataUrl");
        if (dataUrl.length() > 0 && (Blocks.area(target).length() == 0
                || Blocks.isAction(target))) {
            throw new ResponseStatusException(400, "Only an admin block fetches data");
        }
        String actionUrl = address(in, "actionUrl");
        boolean acts = !Blocks.actions(root).isEmpty();
        if (acts != (actionUrl.length() > 0)) {
            throw new ResponseStatusException(400, acts ? "Buttons with an action need an "
                    + "actionUrl" : "An actionUrl needs a button with an action");
        }
        Map count = db.queryOne("SELECT COUNT(*) AS n FROM app_extension WHERE "
                + "app_installation_id = ?", new Object[] {k.id});
        if (count != null && Rows.number(count.get("n")) >= MAX_PER_APP) {
            throw new ResponseStatusException(409, "An app may keep " + MAX_PER_APP
                    + " extensions in a store");
        }
        String id = Ids.next();
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO app_extension (id, store_id, app_installation_id, app_id, "
                + "target, place, title, tree, data_url, action_url, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {id, k.storeId,
                    k.id, k.appId, target, place, title, json, dataUrl, actionUrl,
                    Long.valueOf(now), Long.valueOf(now)});
        if (Blocks.STOREFRONT_BLOCK.equals(target)) {
            touch(db, k.storeId);
        }
        apps.audit(k.storeId, k.owner(), "APP_EXTENSION_UPLOADED", k.appId, target + " "
                + title);
        return dto(one(k, id));
    }

    private static String address(Map in, String name) {
        String url = field(in, name);
        if (url.length() == 0) {
            return "";
        }
        if (!Blocks.isHttps(url)) {
            throw new ResponseStatusException(400, "The " + name + " is a plain https address");
        }
        String unsafe = UrlGuard.refuseByName(url);
        if (unsafe != null) {
            throw new ResponseStatusException(400, unsafe);
        }
        return url;
    }

    static String join(String[] all) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < all.length; iter++) {
            b.append(iter == 0 ? "" : ", ").append(all[iter]);
        }
        return b.toString();
    }

    private static final String COLUMNS = "e.id AS id, e.store_id AS store_id, "
            + "e.app_installation_id AS app_installation_id, e.app_id AS app_id, "
            + "e.target AS target, e.place AS place, e.title AS title, e.tree AS tree, "
            + "e.data_url AS data_url, e.action_url AS action_url, e.updated_at AS updated_at, "
            + "e.created_at AS created_at, a.name AS app_name, i.scopes AS scopes";
    private static final String FROM = " FROM app_extension e JOIN app_installation i ON "
            + "i.id = e.app_installation_id JOIN app a ON a.id = e.app_id ";

    /// The extensions of `k`'s app in its store.
    public List<Map<String, Object>> mine(StoreApiKeys.Key k) throws IOException {
        List rows = db.query("SELECT " + COLUMNS + FROM + "WHERE e.store_id = ? AND "
                + "e.app_installation_id = ? ORDER BY e.created_at, e.id",
                new Object[] {k.storeId, k.id});
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(ext((Map) rows.get(iter))));
        }
        return out;
    }

    private Ext one(StoreApiKeys.Key k, String id) throws IOException {
        Map r = id == null || id.length() > 32 ? null : db.queryOne("SELECT " + COLUMNS + FROM
                + "WHERE e.id = ? AND e.store_id = ? AND e.app_installation_id = ?",
                new Object[] {id, k.storeId, k.id});
        if (r == null) {
            throw new ResponseStatusException(404, "No such extension");
        }
        return ext(r);
    }

    /// Removes one of `k`'s app's extensions.
    public void delete(StoreApiKeys.Key k, String id) throws IOException {
        Ext e = one(k, id);
        db.execute("DELETE FROM app_extension WHERE id = ?", new Object[] {e.id});
        if (Blocks.STOREFRONT_BLOCK.equals(e.target)) {
            touch(db, k.storeId);
        }
        apps.audit(k.storeId, k.owner(), "APP_EXTENSION_DELETED", k.appId, e.id);
    }

    private static Map<String, Object> dto(Ext e) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", e.id);
        m.put("target", e.target);
        m.put("place", e.place);
        m.put("title", e.title);
        m.put("dataUrl", e.dataUrl);
        m.put("actionUrl", e.actionUrl);
        try {
            m.put("tree", Json.parseObject(e.tree));
        } catch (IOException bad) {
            m.put("tree", null);
        }
        m.put("updatedAt", Long.valueOf(e.updatedAt));
        return m;
    }

    private static Ext ext(Map r) {
        Ext e = new Ext();
        e.id = Rows.text(r.get("id"));
        e.storeId = Rows.text(r.get("store_id"));
        e.installationId = Rows.text(r.get("app_installation_id"));
        e.appId = Rows.text(r.get("app_id"));
        e.appName = Rows.text(r.get("app_name"));
        e.target = Rows.text(r.get("target"));
        e.place = Rows.text(r.get("place"));
        e.title = Rows.text(r.get("title"));
        e.tree = Rows.text(r.get("tree"));
        e.dataUrl = Rows.text(r.get("data_url"));
        e.actionUrl = Rows.text(r.get("action_url"));
        e.updatedAt = Rows.number(r.get("updated_at"));
        return e;
    }

    // ------------------------------------------------------------- active

    /// The extensions of `targets` live at `storeId` now: of an installed app
    /// that still holds `write_extensions`, in upload order.
    List<Ext> active(String storeId, String... targets) throws IOException {
        StringBuilder in = new StringBuilder();
        Object[] args = new Object[targets.length + 2];
        args[0] = storeId;
        args[1] = Apps.INSTALLED;
        for (int iter = 0; iter < targets.length; iter++) {
            in.append(iter == 0 ? "?" : ", ?");
            args[iter + 2] = targets[iter];
        }
        List rows = db.query("SELECT " + COLUMNS + FROM + "WHERE e.store_id = ? AND i.status = ? "
                + "AND e.target IN (" + in + ") ORDER BY e.created_at, e.id", args);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<Ext> out = new ArrayList<Ext>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            if (StoreApiKeys.has(StoreApiKeys.scopes(Rows.text(r.get("scopes"))),
                    StoreApiKeys.WRITE_EXTENSIONS)) {
                out.add(ext(r));
            }
        }
        return out;
    }

    private static Blocks.Node tree(Ext e) {
        try {
            // Checked again on read: the row is trusted no more than an upload.
            Blocks.Node root = Blocks.parse(e.tree);
            Blocks.checkTarget(e.target, root);
            return root;
        } catch (Blocks.Invalid bad) {
            return null;
        }
    }

    // -------------------------------------------------------------- admin

    private static final String[] AREAS = {"product", "order", "customer"};

    private void staff(String username, String storeId, String area) throws IOException {
        if ("order".equals(area)) {
            access.requireStaff(username, storeId, StoreAccess.OWNER, StoreAccess.MANAGER,
                    StoreAccess.FULFILMENT);
        } else {
            access.requireStaff(username, storeId, StoreAccess.OWNER, StoreAccess.MANAGER);
        }
    }

    private void resource(String storeId, String area, String resourceId) throws IOException {
        String table = "product".equals(area) ? "product" : "order".equals(area) ? "store_order"
                : "customer";
        Map row = resourceId == null || resourceId.length() == 0 || resourceId.length() > 32
                ? null : db.queryOne("SELECT id FROM " + table + " WHERE id = ? AND store_id = ?",
                        new Object[] {resourceId, storeId});
        if (row == null) {
            throw new ResponseStatusException(404, "No such " + area);
        }
    }

    /// The blocks and actions of installed apps for the admin page of
    /// `resourceId`, each with its app's data. `area` is `product`, `order`
    /// or `customer`.
    public List<ExtensionBlockDto> blocks(String username, String storeId, String area,
            String resourceId) throws IOException {
        // Whose store it is comes first: staff of another store learn nothing,
        // not even that their request was malformed.
        access.requireStaff(username, storeId, StoreAccess.OWNER, StoreAccess.MANAGER,
                StoreAccess.CASHIER, StoreAccess.FULFILMENT);
        boolean known = false;
        for (int iter = 0; iter < AREAS.length; iter++) {
            known |= AREAS[iter].equals(area);
        }
        if (!known) {
            throw new ResponseStatusException(400, "The area is product, order or customer");
        }
        staff(username, storeId, area);
        resource(storeId, area, resourceId);
        List<Ext> all = active(storeId, "admin." + area + ".block", "admin." + area + ".action");
        List<ExtensionBlockDto> out = new ArrayList<ExtensionBlockDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Ext e = all.get(iter);
            if (tree(e) == null) {
                continue;
            }
            Map<String, String> values = Collections.emptyMap();
            String error = "";
            if (e.dataUrl.length() > 0) {
                try {
                    values = data(e, resourceId);
                } catch (IOException failed) {
                    error = Codes.cut(failed.getMessage() == null ? "The app did not answer"
                            : failed.getMessage(), 200);
                } catch (Blocks.Invalid bad) {
                    error = Codes.cut(bad.getMessage(), 200);
                }
            }
            out.add(new ExtensionBlockDto(e.id, e.installationId, e.appName, e.target, e.place,
                    e.tree, list(values), error));
        }
        return out;
    }

    private static List<BlockValueDto> list(Map<String, String> values) {
        List<BlockValueDto> out = new ArrayList<BlockValueDto>();
        for (Map.Entry<String, String> v : values.entrySet()) {
            out.add(new BlockValueDto(v.getKey(), v.getValue()));
        }
        return out;
    }

    /// The signed body every call to an app starts from.
    private static Map<String, Object> envelope(Ext e, String resourceId) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("extensionId", e.id);
        m.put("storeId", e.storeId);
        m.put("target", e.target);
        m.put("resourceId", resourceId);
        m.put("timestamp", Long.valueOf(System.currentTimeMillis()));
        return m;
    }

    private List<String> headers(Ext e, byte[] body) throws IOException {
        List<String> h = new ArrayList<String>();
        h.add("Content-Type: application/json");
        h.add("X-Bazaar-Extension: " + e.id);
        h.add("X-Bazaar-Extension-Target: " + e.target);
        h.add(Webhooks.SIGNATURE_HEADER + ": " + Webhooks.signature(apps.signingKeyOf(e.appId),
                body));
        return h;
    }

    private Map<String, String> data(Ext e, String resourceId) throws IOException, Blocks.Invalid {
        String key = e.id + ":" + e.updatedAt + ":" + resourceId;
        long now = System.currentTimeMillis();
        synchronized (this) {
            Cached c = cache.get(key);
            if (c != null && now - c.at < DATA_CACHE_MILLIS) {
                return c.values;
            }
            dataCalls++;
        }
        byte[] body = Json.write(envelope(e, resourceId)).getBytes("UTF-8");
        long started = System.currentTimeMillis();
        Reply r = transport.send("POST", e.dataUrl, headers(e, body), body, DATA_TIMEOUT_MILLIS,
                MAX_REPLY);
        if (System.currentTimeMillis() - started > DATA_TIMEOUT_MILLIS) {
            throw new IOException("The app did not answer within " + DATA_TIMEOUT_MILLIS + " ms");
        }
        if (!r.ok()) {
            throw new IOException("The app answered " + r.status);
        }
        if (r.body.length > MAX_REPLY) {
            throw new IOException("The app's answer is too long");
        }
        Map<String, String> values = Blocks.values(r.text());
        synchronized (this) {
            if (cache.size() >= CACHE_SIZE) {
                Iterator<String> oldest = cache.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
            cache.put(key, new Cached(now, values));
        }
        return values;
    }

    /// Presses `action` of extension `extensionId` on `resourceId`: posted to
    /// the app, signed, with the block's field answers checked by kind.
    public ExtensionActionResultDto action(String username, String storeId, String extensionId,
            ExtensionActionDto req) throws IOException {
        // Whose store it is comes first: staff of another store learn nothing,
        // not even that their request was malformed.
        access.requireStaff(username, storeId, StoreAccess.OWNER, StoreAccess.MANAGER,
                StoreAccess.CASHIER, StoreAccess.FULFILMENT);
        if (req == null || req.action() == null) {
            throw new ResponseStatusException(400, "Name the action");
        }
        Map r = extensionId == null || extensionId.length() > 32 ? null
                : db.queryOne("SELECT " + COLUMNS + FROM + "WHERE e.id = ? AND e.store_id = ? "
                        + "AND i.status = ?", new Object[] {extensionId, storeId, Apps.INSTALLED});
        Ext e = r == null ? null : ext(r);
        String area = e == null ? "" : Blocks.area(e.target);
        if (e == null || area.length() == 0 || !StoreApiKeys.has(StoreApiKeys.scopes(
                Rows.text(r.get("scopes"))), StoreApiKeys.WRITE_EXTENSIONS)) {
            access.requireStaff(username, storeId, StoreAccess.OWNER, StoreAccess.MANAGER);
            throw new ResponseStatusException(404, "No such extension");
        }
        staff(username, storeId, area);
        resource(storeId, area, req.resourceId());
        Blocks.Node root = tree(e);
        if (root == null || !Blocks.hasAction(root, req.action())) {
            throw new ResponseStatusException(400, "The extension has no such action");
        }
        Map<String, Object> answers = new LinkedHashMap<String, Object>();
        List<Blocks.Node> fields = Blocks.fields(root);
        for (int iter = 0; iter < fields.size(); iter++) {
            Blocks.Node f = fields.get(iter);
            try {
                answers.put(f.key, Blocks.answer(f, valueOf(req.values(), f.key)));
            } catch (Blocks.Invalid bad) {
                throw new ResponseStatusException(400, bad.getMessage());
            }
        }
        Map<String, Object> m = envelope(e, req.resourceId());
        m.put("action", req.action());
        m.put("values", answers);
        m.put("user", username);
        byte[] body = Json.write(m).getBytes("UTF-8");
        apps.audit(storeId, username, "APP_EXTENSION_ACTION", e.appId, e.id + " "
                + req.action() + " " + req.resourceId());
        try {
            Reply reply = transport.send("POST", e.actionUrl, headers(e, body), body,
                    ACTION_TIMEOUT_MILLIS, MAX_REPLY);
            String message = Blocks.message(reply.text());
            return new ExtensionActionResultDto(reply.ok(), message.length() > 0 ? message
                    : reply.ok() ? "Done" : "The app answered " + reply.status);
        } catch (IOException failed) {
            return new ExtensionActionResultDto(false, "The app did not answer");
        }
    }

    private static String valueOf(List<BlockValueDto> values, String key) {
        for (int iter = 0; values != null && iter < values.size(); iter++) {
            BlockValueDto v = values.get(iter);
            if (v != null && key.equals(v.key())) {
                return v.value();
            }
        }
        return "";
    }

    // ------------------------------------------------------------ session

    /// `installationId`'s app page with a session token: a JWT (HS256)
    /// signed with the app's signing key, `aud` its client id, good for
    /// [AppSessionTokens#TTL_SECONDS]. See [AppSessionTokens#verify].
    public AppSessionDto session(String username, String storeId, String installationId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.OWNER, StoreAccess.MANAGER);
        Map r = installationId == null || installationId.length() > 32 ? null
                : db.queryOne("SELECT a.client_id AS client_id, a.app_url AS app_url, "
                        + "a.signing_key AS signing_key, i.status AS status FROM app_installation i "
                        + "JOIN app a ON a.id = i.app_id WHERE i.id = ? AND i.store_id = ?",
                        new Object[] {installationId, storeId});
        if (r == null || !Apps.INSTALLED.equals(Rows.text(r.get("status")))) {
            throw new ResponseStatusException(404, "No such app");
        }
        String appUrl = Rows.text(r.get("app_url"));
        if (!Blocks.isHttps(appUrl)) {
            throw new ResponseStatusException(409, "The app has no https page");
        }
        String token = AppSessionTokens.issue(Rows.text(r.get("signing_key")),
                Rows.text(r.get("client_id")), storeId, username);
        String url = appUrl + (appUrl.indexOf('?') < 0 ? "?" : "&") + "shop=" + storeId
                + "&session=" + token;
        return new AppSessionDto(url, System.currentTimeMillis()
                + AppSessionTokens.TTL_SECONDS * 1000L);
    }

    // ---------------------------------------------------------- storefront

    /// The storefront or checkout blocks of `storeId` for shoppers. They
    /// carry no app data: a storefront page is shared and cached.
    public List<ExtensionBlockDto> shop(String storeId, String target) throws IOException {
        if (!Blocks.STOREFRONT_BLOCK.equals(target) && !Blocks.CHECKOUT_BLOCK.equals(target)) {
            throw new ResponseStatusException(400, "The target is storefront.block or "
                    + "checkout.block");
        }
        List<Ext> all = active(storeId, target);
        List<ExtensionBlockDto> out = new ArrayList<ExtensionBlockDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Ext e = all.get(iter);
            if (tree(e) != null) {
                out.add(new ExtensionBlockDto(e.id, "", e.appName, e.target, e.place, e.tree,
                        new ArrayList<BlockValueDto>(), ""));
            }
        }
        return out;
    }

    /// One storefront block for a server-rendered page: its place, title and
    /// lines with placeholders left empty, and the hosts of its images.
    public static final class Printed {
        public final String id;
        public final String place;
        public final String title;
        public final List<Blocks.Row> rows;

        Printed(String id, String place, String title, List<Blocks.Row> rows) {
            this.id = id;
            this.place = place;
            this.title = title;
            this.rows = rows;
        }
    }

    /// The storefront blocks of `storeId`, flattened for its pages.
    public List<Printed> storefront(String storeId) throws IOException {
        List<Ext> all = active(storeId, Blocks.STOREFRONT_BLOCK);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        List<Printed> out = new ArrayList<Printed>();
        for (int iter = 0; iter < all.size(); iter++) {
            Ext e = all.get(iter);
            Blocks.Node root = tree(e);
            if (root != null) {
                out.add(new Printed(e.id, e.place, e.title.length() > 0 ? e.title : e.appName,
                        Blocks.flatten(root, null)));
            }
        }
        return out;
    }

    // ----------------------------------------------------------- checkout

    /// The answers to the fields of `storeId`'s checkout blocks, from what
    /// the shopper sent as `app:<extensionId>:<key>`, each checked by kind
    /// (a 400 naming the field otherwise), for the order's fields. The label
    /// names the app, so the merchant knows whose question it was.
    public List<OrderFieldDto> checkoutAnswers(String storeId, List<FieldAnswerDto> given)
            throws IOException {
        List<Ext> all = active(storeId, Blocks.CHECKOUT_BLOCK);
        List<OrderFieldDto> out = new ArrayList<OrderFieldDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Ext e = all.get(iter);
            Blocks.Node root = tree(e);
            if (root == null) {
                continue;
            }
            List<Blocks.Node> fields = Blocks.fields(root);
            for (int f = 0; f < fields.size(); f++) {
                Blocks.Node field = fields.get(f);
                String id = FIELD_PREFIX + e.id + ":" + field.key;
                String value;
                try {
                    value = Blocks.answer(field, answerOf(given, id));
                } catch (Blocks.Invalid bad) {
                    throw new ResponseStatusException(400, bad.getMessage());
                }
                if (value.length() > 0) {
                    out.add(new OrderFieldDto(Codes.cut(e.appName + ": " + field.text, 120),
                            value));
                }
            }
        }
        return out;
    }

    /// One field of a checkout block, as the server-rendered checkout draws
    /// it: `id` is what [#checkoutAnswers] reads the answer under.
    public static final class FormField {
        public final String id;
        public final String label;
        public final String kind;
        public final boolean required;
        public final List<String> options;

        FormField(String id, String label, String kind, boolean required,
                List<String> options) {
            this.id = id;
            this.label = label;
            this.kind = kind;
            this.required = required;
            this.options = options;
        }
    }

    /// The fields of `storeId`'s checkout blocks, in the order
    /// [#checkoutAnswers] checks them, labelled with the app's name.
    public List<FormField> checkoutFields(String storeId) throws IOException {
        List<Ext> all = active(storeId, Blocks.CHECKOUT_BLOCK);
        List<FormField> out = new ArrayList<FormField>();
        for (int iter = 0; iter < all.size(); iter++) {
            Ext e = all.get(iter);
            Blocks.Node root = tree(e);
            if (root == null) {
                continue;
            }
            List<Blocks.Node> fields = Blocks.fields(root);
            for (int f = 0; f < fields.size(); f++) {
                Blocks.Node field = fields.get(f);
                out.add(new FormField(FIELD_PREFIX + e.id + ":" + field.key,
                        Codes.cut(e.appName + ": " + field.text, 120), field.kind,
                        field.required, new ArrayList<String>(field.items)));
            }
        }
        return out;
    }

    private static String answerOf(List<FieldAnswerDto> given, String id) {
        for (int iter = 0; given != null && iter < given.size(); iter++) {
            FieldAnswerDto g = given.get(iter);
            if (g != null && id.equals(g.fieldId())) {
                return g.value();
            }
        }
        return "";
    }

    // ------------------------------------------------------------- shared

    static String hex(byte[] b) {
        return Ids.hex(b);
    }

    static byte[] hmac(String key, String text) throws IOException {
        return Crypto.hmacSha256(key.getBytes("UTF-8"), text.getBytes("UTF-8"));
    }

    static boolean same(String a, String b) throws IOException {
        return a != null && b != null && Crypto.equalsConstantTime(a.getBytes("UTF-8"),
                b.getBytes("UTF-8"));
    }

    static Map verifyJwt(String token, String key) throws IOException {
        return Jwt.verify(token, key.getBytes("UTF-8"));
    }
}
