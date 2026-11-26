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
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.ops.Blocks;
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.web.SiteUrls;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The storefront app proxy: `/s/{handle}/apps/{subpath}/...` is fetched
/// from the URL the app registered (`ExtensionUploadApi`) and answered on
/// the store's own address.
///
/// The forwarded request is a GET carrying the shopper's query plus `shop`,
/// `path_prefix`, `timestamp` and `signature` ([ProxySignature]); no cookie,
/// authorization or other header of the shopper's is passed on, and none of
/// the app's comes back but its content type. The answer must arrive within
/// [#TIMEOUT_MILLIS], be at most [#MAX_BYTES] and be HTML, plain text or
/// JSON; anything else is a 502. HTML is served under a sandbox policy, so
/// an app's page can run no script with the store's origin.
@Controller
public class AppProxyPages {
    public static final int TIMEOUT_MILLIS = 3000;
    public static final int MAX_BYTES = 256 * 1024;
    private static final int MAX_QUERY = 2000;
    private static final int MAX_SEGMENT = 80;
    static final String[] CONTENT_TYPES = {"text/html", "text/plain", "application/json"};

    private final DataSource db;
    private final StoreRepository stores;
    private final Apps apps;
    private final Extensions extensions;

    public AppProxyPages(DataSource db, StoreRepository stores, Apps apps,
            Extensions extensions) {
        this.db = db;
        this.stores = stores;
        this.apps = apps;
        this.extensions = extensions;
    }

    @GetMapping("/s/{handle}/apps/{app}")
    public HttpServer.Response root(@PathVariable("handle") String handle,
            @PathVariable("app") String app, HttpServer.Request request) throws IOException {
        return forward(handle, app, "", request);
    }

    @GetMapping("/s/{handle}/apps/{app}/{a}")
    public HttpServer.Response one(@PathVariable("handle") String handle,
            @PathVariable("app") String app, @PathVariable("a") String a,
            HttpServer.Request request) throws IOException {
        return forward(handle, app, "/" + a, request);
    }

    @GetMapping("/s/{handle}/apps/{app}/{a}/{b}")
    public HttpServer.Response two(@PathVariable("handle") String handle,
            @PathVariable("app") String app, @PathVariable("a") String a,
            @PathVariable("b") String b, HttpServer.Request request) throws IOException {
        return forward(handle, app, "/" + a + "/" + b, request);
    }

    @GetMapping("/s/{handle}/apps/{app}/{a}/{b}/{c}")
    public HttpServer.Response three(@PathVariable("handle") String handle,
            @PathVariable("app") String app, @PathVariable("a") String a,
            @PathVariable("b") String b, @PathVariable("c") String c,
            HttpServer.Request request) throws IOException {
        return forward(handle, app, "/" + a + "/" + b + "/" + c, request);
    }

    private static HttpServer.Response plain(int status, String text) throws IOException {
        return new HttpServer.Response(status, "text/plain; charset=utf-8", text.getBytes("UTF-8"))
                .header("Cache-Control", "no-store").header("X-Content-Type-Options", "nosniff");
    }

    private HttpServer.Response forward(String handle, String subpath, String rest,
            HttpServer.Request request) throws IOException {
        if (!Blocks.isKey(subpath) || !safeRest(rest)) {
            return plain(404, "Not found");
        }
        Store store = handle == null || handle.length() > 64 ? null : stores.findByHandle(handle);
        if (store == null) {
            return plain(404, "Not found");
        }
        Map proxy = db.queryOne("SELECT p.url AS url, p.app_id AS app FROM app_proxy p JOIN "
                + "app_installation i ON i.id = p.app_installation_id WHERE p.store_id = ? AND "
                + "p.subpath = ? AND i.status = ?",
                new Object[] {store.getId(), subpath, Apps.INSTALLED});
        if (proxy == null) {
            return plain(404, "Not found");
        }
        Map<String, String> params = query(request.getTarget());
        if (params == null) {
            return plain(400, "Bad query");
        }
        params.put("shop", handle);
        params.put("path_prefix", "/apps/" + subpath);
        params.put("timestamp", Long.toString(System.currentTimeMillis() / 1000L));
        params.remove("signature");
        String signature = ProxySignature.sign(params, apps.signingKeyOf(Rows.text(proxy.get("app"))));
        params.put("signature", signature);
        String url = target(Rows.text(proxy.get("url")), rest, params);
        Extensions.Reply reply;
        try {
            List<String> headers = new ArrayList<String>();
            headers.add("Accept: text/html, text/plain, application/json");
            reply = extensions.transport().send("GET", url, headers, null, TIMEOUT_MILLIS,
                    MAX_BYTES);
        } catch (IOException unreachable) {
            return plain(502, "The app did not answer");
        }
        if (reply.body.length > MAX_BYTES) {
            return plain(502, "The app's answer is too long");
        }
        String type = allowed(reply.contentType);
        if (type == null) {
            return plain(502, "The app answered with a type the store does not serve");
        }
        int status = reply.status >= 200 && reply.status < 500 ? reply.status : 502;
        HttpServer.Response out = new HttpServer.Response(status, type + "; charset=utf-8",
                reply.body).header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-store").header("Referrer-Policy", "no-referrer");
        if ("text/html".equals(type)) {
            out.header("Content-Security-Policy", "sandbox allow-forms allow-popups; "
                    + "default-src 'none'; img-src https:; style-src 'unsafe-inline' https:; "
                    + "form-action https:; frame-ancestors 'self'");
        }
        return out;
    }

    /// The allowed media type `contentType` starts with, or null.
    static String allowed(String contentType) {
        if (contentType == null) {
            return null;
        }
        String t = contentType.trim();
        for (String c : CONTENT_TYPES) {
            if (t.regionMatches(true, 0, c, 0, c.length())
                    && (t.length() == c.length() || t.charAt(c.length()) == ';'
                    || t.charAt(c.length()) == ' ')) {
                return c;
            }
        }
        return null;
    }

    private static boolean safeRest(String rest) {
        if (rest.length() > 3 * MAX_SEGMENT) {
            return false;
        }
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_' || c == '.' || c == '/';
            if (!ok) {
                return false;
            }
        }
        return rest.indexOf("..") < 0;
    }

    /// The registered url with `rest` appended to its path and the signed
    /// query in place of any it had.
    static String target(String base, String rest, Map<String, String> params) {
        int q = base.indexOf('?');
        String path = q < 0 ? base : base.substring(0, q);
        if (path.endsWith("/") && rest.startsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        StringBuilder b = new StringBuilder(path).append(rest);
        char sep = '?';
        for (Map.Entry<String, String> e : params.entrySet()) {
            b.append(sep).append(SiteUrls.encode(e.getKey())).append('=')
                    .append(SiteUrls.encode(e.getValue()));
            sep = '&';
        }
        return b.toString();
    }

    /// The decoded query of a request target, or null when it is malformed
    /// or too long. A repeated name keeps its last value.
    static Map<String, String> query(String targetText) throws IOException {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (targetText == null) {
            return out;
        }
        int q = targetText.indexOf('?');
        if (q < 0) {
            return out;
        }
        String s = targetText.substring(q + 1);
        if (s.length() > MAX_QUERY) {
            return null;
        }
        int start = 0;
        while (start <= s.length()) {
            int amp = s.indexOf('&', start);
            int end = amp < 0 ? s.length() : amp;
            if (end > start) {
                String pair = s.substring(start, end);
                int eq = pair.indexOf('=');
                String name = decode(eq < 0 ? pair : pair.substring(0, eq));
                String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
                if (name == null || value == null) {
                    return null;
                }
                if (name.length() > 0 && out.size() < 40) {
                    out.put(name, value);
                }
            }
            if (amp < 0) {
                break;
            }
            start = amp + 1;
        }
        return out;
    }

    private static String decode(String s) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '+') {
                b.write(' ');
            } else if (c == '%') {
                if (i + 2 >= s.length()) {
                    return null;
                }
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    return null;
                }
                b.write(hi * 16 + lo);
                i += 2;
            } else if (c < 128) {
                b.write(c);
            } else {
                return null;
            }
        }
        return new String(b.toByteArray(), "UTF-8");
    }
}
