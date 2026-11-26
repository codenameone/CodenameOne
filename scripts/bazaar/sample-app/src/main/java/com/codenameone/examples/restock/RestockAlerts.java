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
package com.codenameone.examples.restock;

import com.codename1.backend.Json;
import com.codename1.backend.WebClient;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Restock Alerts: shoppers leave their e-mail address on a sold-out product,
/// and the merchant tells them all at once when it is back.
///
/// This class is the whole app, written the way any third-party app would be:
/// it reaches Bazaar only through the public HTTP API, and it believes nothing
/// Bazaar sends until the signature checks ([Signatures]). [RestockEndpoints]
/// puts it on the web.
///
/// What it shows, in the order a store meets it:
///
/// 1. **Install** -- [#installUrl] sends the merchant to Bazaar's consent page
///    with a PKCE challenge; [#completeInstall] trades the code (and the
///    verifier) for an access token and keeps it.
/// 2. **Set up** -- with the token it subscribes to `inventory/low`, uploads an
///    admin product block, a storefront block and a checkout field, claims the
///    app proxy path `/apps/restock`, and uploads a checkout function.
/// 3. **Serve** -- signed webhooks, block data and the "Notify now" action,
///    the proxied signup form, and the embedded admin page.
/// 4. **Leave** -- `app/uninstalled` drops the token, `shop/redact` everything
///    else.
///
/// State is kept in memory to keep the sample short. A real app keeps the
/// tokens (encrypted) and the subscribers in a database.
@Component
public class RestockAlerts {
    /// The scopes the app asks for, and why: stock alerts, the UI, the
    /// checkout rule.
    public static final String SCOPES = "read_inventory write_extensions write_functions";
    /// Where the app proxy puts the signup form: `/s/{store}/apps/restock`.
    public static final String PROXY_SUBPATH = "restock";

    /// How a request reaches Bazaar. [WebHttp] is the real one.
    public interface Http {
        Answer send(String method, String url, List<String> headers, byte[] body)
                throws IOException;
    }

    /// What Bazaar answered.
    public static final class Answer {
        public final int status;
        public final String body;

        public Answer(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }
    }

    /// Plain HTTPS with modest timeouts.
    static final class WebHttp implements Http {
        @Override
        public Answer send(String method, String url, List<String> headers, byte[] body)
                throws IOException {
            WebClient client = new WebClient().setConnectTimeoutMillis(5000)
                    .setReadTimeoutMillis(10000);
            try {
                com.codename1.backend.Web.Result r = client.request(method, url,
                        new ArrayList<String>(headers), body);
                return new Answer(r.getStatus(), r.getBodyAsString());
            } finally {
                client.close();
            }
        }
    }

    /// One shopper waiting for one product.
    static final class Subscriber {
        final String productId;
        final String email;
        boolean notified;

        Subscriber(String productId, String email) {
            this.productId = productId;
            this.email = email;
        }
    }

    /// What the app keeps per store.
    static final class Shop {
        String token;
        final List<Subscriber> subscribers = new ArrayList<Subscriber>();
        final List<String> lowStock = new ArrayList<String>();
        final List<String> sent = new ArrayList<String>();
    }

    private final String bazaarUrl;
    private final String appUrl;
    private String clientId;
    private String clientSecret;
    private String signingKey;
    private Http http = new WebHttp();
    private final Map<String, Shop> shops = new LinkedHashMap<String, Shop>();
    /// OAuth `state` -> `{storeId, code verifier}` of installs in flight.
    private final Map<String, String[]> pending = new LinkedHashMap<String, String[]>();
    private final List<String> privacyLog = new ArrayList<String>();

    public RestockAlerts(@Value("${restock.bazaar-url:https://bazaar.example}") String bazaarUrl,
            @Value("${restock.app-url:https://restock.example}") String appUrl,
            @Value("${restock.client-id:}") String clientId,
            @Value("${restock.client-secret:}") String clientSecret) throws IOException {
        this.bazaarUrl = bazaarUrl;
        this.appUrl = appUrl;
        useCredentials(clientId, clientSecret);
    }

    /// The client id and secret Bazaar showed when the app was registered.
    /// Normally read from `restock.client-id` and `restock.client-secret`.
    public final synchronized void useCredentials(String id, String secret) throws IOException {
        this.clientId = id;
        this.clientSecret = secret;
        this.signingKey = secret == null || secret.length() == 0 ? null
                : Signatures.signingKey(secret);
    }

    /// Replaces how Bazaar is reached; the end-to-end test routes it in process.
    public synchronized void useHttp(Http http) {
        this.http = http;
    }

    private synchronized Shop shop(String storeId) {
        Shop s = shops.get(storeId);
        if (s == null) {
            s = new Shop();
            shops.put(storeId, s);
        }
        return s;
    }

    private synchronized String key() throws IOException {
        if (signingKey == null) {
            throw new IOException("Set restock.client-secret first");
        }
        return signingKey;
    }

    // =============================================================== install

    /// The address the merchant's browser goes to: Bazaar's consent page,
    /// carrying a fresh `state` and the S256 challenge of a fresh verifier.
    public synchronized String installUrl(String storeId) {
        String state = OAuth2Parameters.random(16);
        String verifier = OAuth2Parameters.random(32);
        pending.put(state, new String[] {storeId, verifier});
        Map<String, String> q = new LinkedHashMap<String, String>();
        q.put("store", storeId);
        q.put("client_id", clientId);
        q.put("scope", SCOPES);
        q.put("redirect_uri", appUrl + "/restock/callback");
        q.put("state", state);
        q.put("code_challenge", OAuth2Parameters.sha256(verifier));
        q.put("code_challenge_method", "S256");
        return OAuth2Parameters.append(bazaarUrl + "/account/apps/authorize", q);
    }

    /// Bazaar sent the merchant back with `code`: trade it for a token with
    /// the verifier only this app knows, keep the token, and set the store up.
    /// Answers the store id.
    public String completeInstall(String code, String state) throws IOException {
        String[] p;
        synchronized (this) {
            p = state == null ? null : pending.remove(state);
        }
        if (p == null || code == null) {
            throw new IOException("Unknown or reused state");
        }
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("code", code);
        form.put("code_verifier", p[1]);
        form.put("redirect_uri", appUrl + "/restock/callback");
        List<String> h = new ArrayList<String>();
        h.add("Content-Type: application/x-www-form-urlencoded");
        Answer a = http.send("POST", bazaarUrl + "/api/apps/oauth/token", h,
                Signatures.utf8(OAuth2Parameters.format(form)));
        if (a.status != 200) {
            throw new IOException("Bazaar refused the code: " + a.status + " " + a.body);
        }
        String token = (String) Json.parseObject(a.body).get("access_token");
        synchronized (this) {
            shop(p[0]).token = token;
        }
        setUp(p[0]);
        return p[0];
    }

    /// Calls the store's API with the app's token; throws unless it is a 2xx.
    private Answer api(String storeId, String method, String path, String json)
            throws IOException {
        String token;
        synchronized (this) {
            token = shop(storeId).token;
        }
        List<String> h = new ArrayList<String>();
        h.add("Authorization: Bearer " + token);
        h.add("Content-Type: application/json");
        // Pin the API version the app was written against.
        h.add("X-Bazaar-Api-Version: 2026-10");
        Answer a = http.send(method, bazaarUrl + "/api/v1/stores/" + storeId + path, h,
                json == null ? null : Signatures.utf8(json));
        if (a.status < 200 || a.status > 299) {
            throw new IOException(method + " " + path + " answered " + a.status + " " + a.body);
        }
        return a;
    }

    /// Everything the app adds to a store, right after the install.
    private void setUp(String storeId) throws IOException {
        // 1. Hear about stock running low. app/uninstalled and the privacy
        //    topics need no subscription: every app gets them.
        api(storeId, "POST", "/webhooks", "{\"topic\":\"inventory/low\",\"url\":\""
                + appUrl + "/restock/webhooks\"}");

        // 2. A block on the admin's product page. Its text is filled from the
        //    data URL; its button posts "notify" to the action URL.
        api(storeId, "POST", "/extensions", "{\"target\":\"admin.product.block\","
                + "\"title\":\"Restock Alerts\",\"tree\":{\"type\":\"stack\",\"children\":["
                + "{\"type\":\"text\",\"text\":\"Subscribers waiting: {{waiting}}\"},"
                + "{\"type\":\"button\",\"label\":\"Notify now\",\"action\":\"notify\"}]},"
                + "\"dataUrl\":\"" + appUrl + "/restock/blocks/data\","
                + "\"actionUrl\":\"" + appUrl + "/restock/blocks/action\"}");

        // 3. A note under the price on the storefront, pointing at the form.
        api(storeId, "POST", "/extensions", "{\"target\":\"storefront.block\","
                + "\"place\":\"product-below-price\",\"title\":\"Restock Alerts\",\"tree\":"
                + "{\"type\":\"stack\",\"children\":[{\"type\":\"heading\",\"text\":"
                + "\"Notify me when back in stock\"},{\"type\":\"text\",\"text\":"
                + "\"Sold out? Leave your e-mail at /apps/restock and we will tell you.\"}]}}");

        // 4. A field at checkout; the answer is kept on the order.
        api(storeId, "POST", "/extensions", "{\"target\":\"checkout.block\","
                + "\"title\":\"Restock Alerts\",\"tree\":{\"type\":\"field\",\"key\":"
                + "\"alerts\",\"label\":\"E-mail me when sold-out items return\","
                + "\"kind\":\"checkbox\"}}");

        // 5. /s/{store}/apps/restock/... is forwarded here, signed.
        api(storeId, "POST", "/app-proxy", "{\"subpath\":\"" + PROXY_SUBPATH
                + "\",\"url\":\"" + appUrl + "/restock/proxy/" + storeId + "\"}");

        // 6. A checkout rule Bazaar runs in its own sandbox: while stock is
        //    short, at most five of each item per order.
        api(storeId, "POST", "/functions", "{\"name\":\"Fair share\",\"target\":"
                + "\"validation\",\"source\":\"for l in lines {\\n  if l.quantity > 5 { "
                + "emit error(\\\"At most five of each item while stock is short\\\"); }\\n}\"}");
    }

    // ============================================================== webhooks

    /// A webhook: answers the HTTP status to reply with. 401 for a forged
    /// one, or one signed more than five minutes ago (a replay), so Bazaar
    /// never counts it as delivered.
    public int webhook(String topic, byte[] body, String signature, String timestamp) throws IOException {
        if (!Signatures.body(key(), body, signature, timestamp,
                System.currentTimeMillis() / 1000L)) {
            return 401;
        }
        Map payload = Json.parseObject(new String(body, "UTF-8"));
        String storeId = text(payload.get("storeId"), text(payload.get("store_id"), ""));
        synchronized (this) {
            if ("inventory/low".equals(topic)) {
                shop(storeId).lowStock.add(text(payload.get("variantId"), ""));
            } else if ("app/uninstalled".equals(topic)) {
                // The token is already revoked; forget it.
                shop(storeId).token = null;
            } else if ("shop/redact".equals(topic)) {
                // 48 hours after removal: delete everything about the store.
                shops.remove(storeId);
                privacyLog.add("shop/redact " + storeId);
            } else if ("customers/redact".equals(topic)
                    || "customers/data_request".equals(topic)) {
                // This app keeps e-mail addresses typed into its own form and
                // no Bazaar customer ids, so it has nothing keyed by
                // customer_id to hand over or erase. Record that it was asked.
                privacyLog.add(topic + " " + text(payload.get("customer_id"), ""));
            }
        }
        return 200;
    }

    // ================================================================ blocks

    /// The product block's data: `{"values":{"waiting":"3"}}`, or null when
    /// the call is not signed by Bazaar.
    public String blockData(byte[] body, String signature, String timestamp) throws IOException {
        if (!Signatures.body(key(), body, signature, timestamp,
                System.currentTimeMillis() / 1000L)) {
            return null;
        }
        Map call = Json.parseObject(new String(body, "UTF-8"));
        int waiting = waiting(text(call.get("storeId"), ""), text(call.get("resourceId"), ""));
        return "{\"values\":{\"waiting\":\"" + waiting + "\"}}";
    }

    /// "Notify now": e-mails everyone waiting for the product, once.
    public String blockAction(byte[] body, String signature, String timestamp) throws IOException {
        if (!Signatures.body(key(), body, signature, timestamp,
                System.currentTimeMillis() / 1000L)) {
            return null;
        }
        Map call = Json.parseObject(new String(body, "UTF-8"));
        if (!"notify".equals(call.get("action"))) {
            return "{\"ok\":false,\"message\":\"Unknown action\"}";
        }
        String storeId = text(call.get("storeId"), "");
        String productId = text(call.get("resourceId"), "");
        int count = 0;
        synchronized (this) {
            Shop s = shop(storeId);
            for (int iter = 0; iter < s.subscribers.size(); iter++) {
                Subscriber sub = s.subscribers.get(iter);
                if (!sub.notified && sub.productId.equals(productId)) {
                    sub.notified = true;
                    // A real app hands this to its mail provider.
                    s.sent.add(sub.email);
                    count++;
                }
            }
        }
        return "{\"ok\":true,\"message\":\"Notified " + count + " shopper"
                + (count == 1 ? "" : "s") + "\"}";
    }

    /// How many shoppers wait for `productId` in `storeId`.
    public synchronized int waiting(String storeId, String productId) {
        int n = 0;
        Shop s = shops.get(storeId);
        for (int iter = 0; s != null && iter < s.subscribers.size(); iter++) {
            if (!s.subscribers.get(iter).notified
                    && s.subscribers.get(iter).productId.equals(productId)) {
                n++;
            }
        }
        return n;
    }

    // ================================================================= proxy

    /// A page under `/s/{store}/apps/restock`: `page` is "" for the form and
    /// "join" for its submission; `params` is the decoded query, signature and
    /// all. Answers HTML, or null when the signature does not check.
    public String proxyPage(String storeId, String page, Map<String, String> params)
            throws IOException {
        if (!Signatures.proxy(params, key(), System.currentTimeMillis() / 1000L)) {
            return null;
        }
        String product = text(params.get("product"), "");
        if ("join".equals(page)) {
            String email = text(params.get("email"), "").trim();
            if (email.indexOf('@') < 1 || email.length() > 254 || product.length() == 0) {
                return html("Please give an e-mail address and a product.");
            }
            synchronized (this) {
                shop(storeId).subscribers.add(new Subscriber(product, email));
            }
            return html("Thanks! We will write to " + escape(email) + " when it is back.");
        }
        // The form posts back through the store, so it is signed again.
        return html("<form method=\"get\" action=\"/s/" + escape(params.get("shop"))
                + escape(params.get("path_prefix"))
                + "/join\"><input type=\"hidden\" name=\"product\" value=\"" + escape(product)
                + "\"><label>E-mail <input name=\"email\" type=\"email\" required></label>"
                + "<button>Notify me when back in stock</button></form>");
    }

    // ========================================================= embedded page

    /// The app's own admin page, opened by Bazaar with `session` in the
    /// query. Answers HTML, or null when the token does not verify.
    public String adminPage(String session) throws IOException {
        Map claims;
        String id;
        synchronized (this) {
            id = clientId;
        }
        try {
            claims = Signatures.session(session, key(), id);
        } catch (IOException refused) {
            return null;
        }
        String storeId = text(claims.get("dest"), "");
        int waiting = 0;
        int low;
        synchronized (this) {
            Shop s = shop(storeId);
            for (int iter = 0; iter < s.subscribers.size(); iter++) {
                waiting += s.subscribers.get(iter).notified ? 0 : 1;
            }
            low = s.lowStock.size();
        }
        return html("<h1>Restock Alerts</h1><p>Signed in as " + escape(text(claims.get("sub"),
                "")) + ".</p><p>" + waiting + " shoppers waiting, " + low
                + " low-stock alerts.</p>");
    }

    // ========================================================= for the tests

    /// Whether the app holds a token for `storeId`.
    public synchronized boolean installed(String storeId) {
        Shop s = shops.get(storeId);
        return s != null && s.token != null;
    }

    /// The variants Bazaar reported as low in `storeId`.
    public synchronized List<String> lowStock(String storeId) {
        return new ArrayList<String>(shop(storeId).lowStock);
    }

    /// The addresses "Notify now" wrote to in `storeId`.
    public synchronized List<String> sent(String storeId) {
        return new ArrayList<String>(shop(storeId).sent);
    }

    /// The privacy webhooks the app was sent, one line each.
    public synchronized List<String> privacyLog() {
        return new ArrayList<String>(privacyLog);
    }

    // =============================================================== helpers

    private static String text(Object o, String otherwise) {
        return o instanceof String ? (String) o : otherwise;
    }

    static String html(String body) {
        return "<!doctype html><meta charset=\"utf-8\"><title>Restock Alerts</title>" + body;
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c == '<') {
                b.append("&lt;");
            } else if (c == '>') {
                b.append("&gt;");
            } else if (c == '&') {
                b.append("&amp;");
            } else if (c == '"') {
                b.append("&quot;");
            } else if (c == '\'') {
                b.append("&#39;");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
