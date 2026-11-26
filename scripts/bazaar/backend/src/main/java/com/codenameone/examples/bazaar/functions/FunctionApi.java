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
package com.codenameone.examples.bazaar.functions;

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
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.platform.StoreApiKeys;
import com.codenameone.examples.bazaar.web.security.UrlGuard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An app's checkout functions, on its access token with `write_functions`:
///
/// | Route | Does |
/// |---|---|
/// | `GET functions` | the app's functions in this store, with their last error |
/// | `POST functions` | uploads `{name, target, source}` or `{name, target, url}` |
/// | `POST functions/{id}/activate`, `.../deactivate` | turns one on or off |
/// | `DELETE functions/{id}` | removes one |
///
/// A script is compiled and verified here, once; a program that does not
/// compile is a 400 naming the line. A function is the installation's own:
/// another app, or the store's key, never sees it.
@RestController
public class FunctionApi {
    private static final String BASE = "/api/v1/stores/{storeId}/functions";
    static final int MAX_FUNCTIONS = 20;

    private final StoreApiKeys keys;
    private final DataSource db;
    private final Apps apps;
    private final CheckoutFunctions functions;

    public FunctionApi(StoreApiKeys keys, DataSource db, Apps apps, CheckoutFunctions functions) {
        this.keys = keys;
        this.db = db;
        this.apps = apps;
        this.functions = functions;
    }

    private static HttpServer.Response json(Object value) {
        return HttpServer.Response.json(200, Json.write(value)).header("Cache-Control", "no-store");
    }

    private StoreApiKeys.Key app(String storeId) throws IOException {
        return keys.requireApp(storeId, StoreApiKeys.WRITE_FUNCTIONS);
    }

    @GetMapping(BASE)
    public HttpServer.Response list(@PathVariable("storeId") String storeId) throws Exception {
        return json(listOf(app(storeId)));
    }

    private List<Map<String, Object>> listOf(StoreApiKeys.Key k) throws IOException {
        List rows = db.query("SELECT id AS id, name AS name, target AS target, kind AS kind, "
                + "url AS url, active AS active, created_at AS created_at, "
                + "updated_at AS updated_at FROM checkout_function WHERE store_id = ? AND "
                + "app_installation_id = ? ORDER BY created_at, id",
                new Object[] {k.storeId, k.id});
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto((Map) rows.get(iter)));
        }
        return out;
    }

    private Map<String, Object> dto(Map r) {
        String id = Rows.text(r.get("id"));
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("name", Rows.text(r.get("name")));
        m.put("target", Rows.text(r.get("target")));
        m.put("kind", Rows.text(r.get("kind")));
        m.put("url", Rows.text(r.get("url")));
        m.put("active", Boolean.valueOf(Rows.number(r.get("active")) == 1));
        m.put("createdAt", Long.valueOf(Rows.number(r.get("created_at"))));
        m.put("updatedAt", Long.valueOf(Rows.number(r.get("updated_at"))));
        m.put("failures", Long.valueOf(functions.failures(id)));
        String last = functions.lastError(id);
        m.put("lastError", last == null ? "" : last);
        return m;
    }

    private static String field(Map in, String name) {
        Object v = in.get(name);
        return v instanceof String ? ((String) v).trim() : "";
    }

    @PostMapping(BASE)
    public HttpServer.Response upload(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        StoreApiKeys.Key k = app(storeId);
        Map in = Json.parseObject(request.getBody() == null ? "{}" : request.getBody());
        if (in == null) {
            throw new ResponseStatusException(400, "Send the function as a JSON object");
        }
        String name = field(in, "name");
        if (name.length() == 0 || name.length() > 80) {
            throw new ResponseStatusException(400, "Name the function, in at most 80 characters");
        }
        Target target = Target.named(field(in, "target"));
        if (target == null) {
            throw new ResponseStatusException(400, "The target is one of " + Target.names());
        }
        Object source = in.get("source");
        String url = field(in, "url");
        String kind;
        String script = "";
        String program = "";
        if (source instanceof String && url.length() == 0) {
            script = (String) source;
            try {
                program = FunctionCompiler.compile(script, target).encode();
            } catch (FunctionException bad) {
                throw new ResponseStatusException(400, bad.getMessage());
            }
            kind = CheckoutFunctions.SCRIPT;
        } else if (source == null && url.length() > 0) {
            if (!url.startsWith("https://") || url.length() > 500) {
                throw new ResponseStatusException(400, "A remote function is an https address");
            }
            String unsafe = UrlGuard.refuseByName(url);
            if (unsafe != null) {
                throw new ResponseStatusException(400, unsafe);
            }
            kind = CheckoutFunctions.REMOTE;
        } else {
            throw new ResponseStatusException(400, "Send either source or url");
        }
        Map count = db.queryOne("SELECT COUNT(*) AS n FROM checkout_function WHERE "
                + "app_installation_id = ?", new Object[] {k.id});
        if (count != null && Rows.number(count.get("n")) >= MAX_FUNCTIONS) {
            throw new ResponseStatusException(409, "An app may keep " + MAX_FUNCTIONS
                    + " functions in a store");
        }
        Object active = in.get("active");
        boolean on = !(active instanceof Boolean) || ((Boolean) active).booleanValue();
        String id = Ids.next();
        long now = System.currentTimeMillis();
        db.execute("INSERT INTO checkout_function (id, store_id, app_installation_id, app_id, "
                + "name, target, kind, script, program, url, active, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {id, storeId,
                    k.id, k.appId, name, target.name, kind, script, program, url,
                    Integer.valueOf(on ? 1 : 0), Long.valueOf(now), Long.valueOf(now)});
        apps.audit(storeId, k.owner(), "APP_FUNCTION_UPLOADED", k.appId, target.name + " "
                + name);
        return json(dto(row(k, id)));
    }

    private Map row(StoreApiKeys.Key k, String id) throws IOException {
        Map r = db.queryOne("SELECT id AS id, name AS name, target AS target, kind AS kind, "
                + "url AS url, active AS active, created_at AS created_at, "
                + "updated_at AS updated_at FROM checkout_function WHERE id = ? AND "
                + "store_id = ? AND app_installation_id = ?",
                new Object[] {id, k.storeId, k.id});
        if (r == null) {
            throw new ResponseStatusException(404, "No such function");
        }
        return r;
    }

    private HttpServer.Response turn(String storeId, String id, boolean on) throws IOException {
        StoreApiKeys.Key k = app(storeId);
        row(k, id);
        db.execute("UPDATE checkout_function SET active = ?, updated_at = ? WHERE id = ?",
                new Object[] {Integer.valueOf(on ? 1 : 0),
                    Long.valueOf(System.currentTimeMillis()), id});
        apps.audit(storeId, k.owner(), on ? "APP_FUNCTION_ACTIVATED" : "APP_FUNCTION_DEACTIVATED",
                k.appId, id);
        return json(dto(row(k, id)));
    }

    @PostMapping(BASE + "/{functionId}/activate")
    public HttpServer.Response activate(@PathVariable("storeId") String storeId,
            @PathVariable("functionId") String functionId) throws Exception {
        return turn(storeId, functionId, true);
    }

    @PostMapping(BASE + "/{functionId}/deactivate")
    public HttpServer.Response deactivate(@PathVariable("storeId") String storeId,
            @PathVariable("functionId") String functionId) throws Exception {
        return turn(storeId, functionId, false);
    }

    @DeleteMapping(BASE + "/{functionId}")
    public HttpServer.Response remove(@PathVariable("storeId") String storeId,
            @PathVariable("functionId") String functionId) throws Exception {
        StoreApiKeys.Key k = app(storeId);
        row(k, functionId);
        db.execute("DELETE FROM checkout_function WHERE id = ?", new Object[] {functionId});
        apps.audit(storeId, k.owner(), "APP_FUNCTION_DELETED", k.appId, functionId);
        return json(listOf(k));
    }
}
