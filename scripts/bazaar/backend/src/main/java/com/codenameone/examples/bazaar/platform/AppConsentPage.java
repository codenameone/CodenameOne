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

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.security.CsrfToken;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codenameone.examples.bazaar.api.AppAuthorizeDto;
import com.codenameone.examples.bazaar.api.AppConsentDto;
import com.codenameone.examples.bazaar.api.AppRedirectDto;

import java.util.List;
import java.util.Map;

/// The consent page an app's install link opens, server-rendered under the
/// signed-in web pages (session, CSRF and sign-in redirect come with the
/// path): `GET /account/apps/authorize?store=<storeId>&client_id=...&scope=...
/// &redirect_uri=...&state=...&code_challenge=...&code_challenge_method=S256`.
///
/// It lists what the app asks for -- on an upgrade, only what is added -- and
/// posts the owner's decision back to the same path, where [Apps#authorize]
/// checks the whole request again before redirecting to the app. A request
/// that fails the check (an unknown client, an unregistered redirect address,
/// no PKCE) is shown as an error here and never redirected.
@RestController
public class AppConsentPage {
    public static final String PATH = "/account/apps/authorize";

    private final Apps apps;

    public AppConsentPage(Apps apps) {
        this.apps = apps;
    }

    @GetMapping(PATH)
    public HttpServer.Response show(HttpServer.Request request, CsrfToken csrf,
            @RequestParam(value = "store", required = false) String store,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code_challenge", required = false) String challenge,
            @RequestParam(value = "code_challenge_method", required = false) String method)
            throws Exception {
        AppAuthorizeDto ask = new AppAuthorizeDto(clientId, scope, redirectUri, state, challenge,
                method, false);
        AppConsentDto c;
        try {
            c = apps.consent(Caller.name(), store, ask);
        } catch (ResponseStatusException refused) {
            return page(refused.getStatus() == 400 ? 400 : 404, "This app cannot be installed",
                    "<p>" + esc(refused.getReason()) + "</p>");
        }
        StringBuilder b = new StringBuilder();
        b.append("<p><strong>").append(esc(c.appName())).append("</strong> by ")
                .append(esc(c.developer())).append(c.upgrade() ? " asks " + esc(c.storeName())
                        + " for more access." : " wants to be installed in "
                        + esc(c.storeName()) + ".").append("</p>");
        if (c.description().length() > 0) {
            b.append("<p>").append(esc(c.description())).append("</p>");
        }
        List<String> shown = c.upgrade() ? c.added() : c.scopes();
        b.append("<h2>").append(c.upgrade() ? "It also asks to" : "It will be able to")
                .append("</h2><ul>");
        for (int iter = 0; iter < shown.size(); iter++) {
            b.append("<li>").append(esc(describe(shown.get(iter)))).append("</li>");
        }
        if (shown.isEmpty()) {
            b.append("<li>Nothing it does not already have</li>");
        }
        b.append("</ul>");
        if (c.upgrade() && !c.granted().isEmpty()) {
            b.append("<p>It already has: ");
            for (int iter = 0; iter < c.granted().size(); iter++) {
                b.append(iter == 0 ? "" : ", ").append(esc(c.granted().get(iter)));
            }
            b.append(".</p>");
        }
        b.append("<form method=\"post\" action=\"").append(PATH).append("\">");
        hidden(b, csrf.getParameterName(), csrf.getToken());
        hidden(b, "store", store);
        hidden(b, "client_id", clientId);
        hidden(b, "scope", scope);
        hidden(b, "redirect_uri", redirectUri);
        hidden(b, "state", state);
        hidden(b, "code_challenge", challenge);
        hidden(b, "code_challenge_method", method);
        b.append("<button type=\"submit\" name=\"decision\" value=\"approve\">")
                .append(c.upgrade() ? "Grant access" : "Install app").append("</button> ")
                .append("<button type=\"submit\" name=\"decision\" value=\"deny\">Cancel</button>")
                .append("</form>");
        return page(200, (c.upgrade() ? "Update " : "Install ") + c.appName(), b.toString());
    }

    @PostMapping(PATH)
    public HttpServer.Response decide(HttpServer.Request request) throws Exception {
        Map<String, String> f = OAuth2Parameters.parse(request.getBody() == null ? ""
                : request.getBody());
        AppAuthorizeDto ask = new AppAuthorizeDto(f.get("client_id"), f.get("scope"),
                f.get("redirect_uri"), f.get("state"), f.get("code_challenge"),
                f.get("code_challenge_method"), "approve".equals(f.get("decision")));
        AppRedirectDto to;
        try {
            to = apps.authorize(Caller.name(), f.get("store"), ask);
        } catch (ResponseStatusException refused) {
            return page(refused.getStatus() == 400 ? 400 : 404, "This app cannot be installed",
                    "<p>" + esc(refused.getReason()) + "</p>");
        }
        return HttpServer.Response.text(303, "").header("Location", to.location())
                .header("Cache-Control", "no-store");
    }

    /// A scope in words.
    static String describe(String scope) {
        boolean write = scope.startsWith("write_");
        String what = scope.substring(scope.indexOf('_') + 1);
        return (write ? "See and change your " : "See your ") + what.replace('_', ' ')
                + " (" + scope + ")";
    }

    private static void hidden(StringBuilder b, String name, String value) {
        b.append("<input type=\"hidden\" name=\"").append(esc(name)).append("\" value=\"")
                .append(esc(value)).append("\">");
    }

    private static HttpServer.Response page(int status, String title, String body)
            throws java.io.IOException {
        String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<meta name=\"robots\" content=\"noindex\"><title>" + esc(title)
                + "</title><link rel=\"stylesheet\" href=\""
                + com.codenameone.examples.bazaar.web.ErrorPages.CSS
                + "\"></head><body>"
                + "<main class=\"narrow\"><h1>" + esc(title) + "</h1>" + body
                + "</main></body></html>";
        return new HttpServer.Response(status, "text/html; charset=utf-8",
                html.getBytes("UTF-8")).header("Cache-Control", "no-store")
                .header("X-Frame-Options", "DENY");
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 16);
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
