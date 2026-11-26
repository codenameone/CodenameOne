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

import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.DeleteMapping;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.ops.Blocks;
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.platform.StoreApiKeys;
import com.codenameone.examples.bazaar.web.security.UrlGuard;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// An app's UI extensions and storefront proxy, on its access token with
/// `write_extensions`:
///
/// | Route | Does |
/// |---|---|
/// | `GET extensions` | the app's extensions in this store |
/// | `POST extensions` | uploads `{target, title, place, tree, dataUrl, actionUrl}` |
/// | `DELETE extensions/{id}` | removes one |
/// | `POST app-proxy` | sets `{subpath, url}`: `/s/{handle}/apps/{subpath}` forwards there |
/// | `DELETE app-proxy` | removes the proxy |
///
/// A tree that does not validate ([Blocks]) is a 400 naming what is wrong.
/// Another app, or the store's key, never sees an app's extensions.
@RestController
public class ExtensionUploadApi {
    private static final String BASE = "/api/v1/stores/{storeId}/extensions";
    private static final String PROXY = "/api/v1/stores/{storeId}/app-proxy";

    private final StoreApiKeys keys;
    private final Extensions extensions;
    private final DataSource db;
    private final Apps apps;

    public ExtensionUploadApi(StoreApiKeys keys, Extensions extensions, DataSource db,
            Apps apps) {
        this.keys = keys;
        this.extensions = extensions;
        this.db = db;
        this.apps = apps;
    }

    private static HttpServer.Response json(Object value) {
        return HttpServer.Response.json(200, Json.write(value)).header("Cache-Control", "no-store");
    }

    private StoreApiKeys.Key app(String storeId) throws IOException {
        return keys.requireApp(storeId, StoreApiKeys.WRITE_EXTENSIONS);
    }

    private static Map body(HttpServer.Request request) throws IOException {
        Map in = Json.parseObject(request.getBody() == null ? "{}" : request.getBody());
        if (in == null) {
            throw new ResponseStatusException(400, "Send a JSON object");
        }
        return in;
    }

    @GetMapping(BASE)
    public HttpServer.Response list(@PathVariable("storeId") String storeId) throws Exception {
        return json(extensions.mine(app(storeId)));
    }

    @PostMapping(BASE)
    public HttpServer.Response upload(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        StoreApiKeys.Key k = app(storeId);
        return json(extensions.upload(k, body(request)));
    }

    @DeleteMapping(BASE + "/{extensionId}")
    public HttpServer.Response remove(@PathVariable("storeId") String storeId,
            @PathVariable("extensionId") String extensionId) throws Exception {
        StoreApiKeys.Key k = app(storeId);
        extensions.delete(k, extensionId);
        return json(extensions.mine(k));
    }

    @PostMapping(PROXY)
    public HttpServer.Response proxy(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        StoreApiKeys.Key k = app(storeId);
        Map in = body(request);
        Object s = in.get("subpath");
        Object u = in.get("url");
        String subpath = s instanceof String ? (String) s : "";
        String url = u instanceof String ? (String) u : "";
        if (!Blocks.isKey(subpath)) {
            throw new ResponseStatusException(400, "The subpath is 1 to " + Blocks.MAX_KEY
                    + " lower case letters, digits, '-' or '_'");
        }
        if (!Blocks.isHttps(url)) {
            throw new ResponseStatusException(400, "The proxy url is a plain https address");
        }
        String unsafe = UrlGuard.refuseByName(url);
        if (unsafe != null) {
            throw new ResponseStatusException(400, unsafe);
        }
        Map taken = db.queryOne("SELECT app_installation_id AS i FROM app_proxy WHERE "
                + "store_id = ? AND subpath = ?", new Object[] {storeId, subpath});
        if (taken != null && !k.id.equals(Rows.text(taken.get("i")))) {
            throw new ResponseStatusException(409, "Another app uses that subpath");
        }
        db.execute("DELETE FROM app_proxy WHERE app_installation_id = ?", new Object[] {k.id});
        db.execute("INSERT INTO app_proxy (id, store_id, app_installation_id, app_id, subpath, "
                + "url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(),
                    storeId, k.id, k.appId, subpath, url,
                    Long.valueOf(System.currentTimeMillis())});
        apps.audit(storeId, k.owner(), "APP_PROXY_SET", k.appId, subpath + " " + url);
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("subpath", subpath);
        m.put("url", url);
        return json(m);
    }

    @DeleteMapping(PROXY)
    public HttpServer.Response removeProxy(@PathVariable("storeId") String storeId)
            throws Exception {
        StoreApiKeys.Key k = app(storeId);
        db.execute("DELETE FROM app_proxy WHERE app_installation_id = ?", new Object[] {k.id});
        apps.audit(storeId, k.owner(), "APP_PROXY_REMOVED", k.appId, "");
        return json(new LinkedHashMap<String, Object>());
    }
}
