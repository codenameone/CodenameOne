/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.backend;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import java.io.IOException;

/// Serves the application's OpenAPI document and a Swagger UI page for it.
///
/// The document is written at build time from the same route table the generated
/// routers dispatch on, so it cannot drift from what the server really answers. The
/// generated entry point links this class only for a build that asked for it
/// (`cn1.openapi.enabled=true` in the module's `application.properties`, or a
/// development build); a packaged server that did not ask has none of it.
///
/// It runs as one of the application's routers, behind the security layer, so a
/// `SecurityFilterChain` that protects `/v3/**` protects the document too. Paths
/// follow springdoc's defaults:
///
/// - `cn1.openapi.path` (`/v3/api-docs`): the JSON document.
/// - `cn1.openapi.ui.path` (`/swagger-ui`): an HTML page that loads Swagger UI.
/// - `cn1.openapi.ui.enabled` (true): set false to serve the document alone.
/// - `cn1.openapi.ui.assets` (a jsDelivr URL of `swagger-ui-dist`): where the page
///   loads Swagger UI's script and stylesheet from. The server ships no copy of
///   Swagger UI; point this at a mirror of your own for an offline deployment.
public final class OpenApiDocs implements HttpServer.Handler {
    /// `cn1.openapi.enabled`: build-time switch, also honoured as false at run time.
    public static final String ENABLED = "cn1.openapi.enabled";
    /// `cn1.openapi.path`: where the document is served.
    public static final String PATH = "cn1.openapi.path";
    /// `cn1.openapi.ui.enabled`: whether the Swagger UI page is served.
    public static final String UI_ENABLED = "cn1.openapi.ui.enabled";
    /// `cn1.openapi.ui.path`: where the Swagger UI page is served.
    public static final String UI_PATH = "cn1.openapi.ui.path";
    /// `cn1.openapi.ui.assets`: base URL of a `swagger-ui-dist` distribution.
    public static final String UI_ASSETS = "cn1.openapi.ui.assets";
    /// The default for [#UI_ASSETS], pinned to an exact release.
    public static final String DEFAULT_ASSETS =
            "https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.17.14";

    /// Subresource integrity of the default distribution's two files, so a
    /// compromised CDN cannot run script on the API's own origin. Only for
    /// [#DEFAULT_ASSETS]: a configured location is the operator's to vouch for.
    static final String DEFAULT_JS_INTEGRITY =
            "sha384-wmyclcVGX/WhUkdkATwhaK1X1JtiNrr2EoYJ+diV3vj4v6OC5yCeSu+yW13SYJep";
    static final String DEFAULT_CSS_INTEGRITY =
            "sha384-wxLW6kwyHktdDGr6Pv1zgm/VGJh99lfUbzSn6HNHBENZlCN7W602k9VkGdxuFvPn";

    /// The page's one inline script. Constant -- the document's path travels in a
    /// `data-url` attribute, HTML-escaped where HTML escaping is the right one --
    /// so the policy can name it by hash instead of allowing inline script.
    static final String INLINE_SCRIPT = "window.onload = function () {\n"
            + "  var el = document.getElementById('swagger-ui');\n"
            + "  window.ui = SwaggerUIBundle({ url: el.getAttribute('data-url'),"
            + " dom_id: '#swagger-ui', deepLinking: true });\n};\n";
    /// Base64 SHA-256 of [#INLINE_SCRIPT]; a test recomputes it.
    static final String INLINE_SCRIPT_HASH = "sha256-PtbXBoXkihhGDGtGfeyO7Yv7lcnhkh+bYu0gem0rU1U=";

    private final String path;
    private final String uiPath;
    private final byte[] document;
    private final byte[] page;
    private final String policy;

    private OpenApiDocs(String path, String uiPath, byte[] document, byte[] page,
                        String policy) {
        this.path = path;
        this.uiPath = uiPath;
        this.document = document;
        this.page = page;
        this.policy = policy;
    }

    /// The handler for `document` as configured, or null when the configuration
    /// switches it off.
    ///
    /// #### Parameters
    ///
    /// - `config`: the server's configuration
    /// - `document`: the OpenAPI document the build wrote
    ///
    /// #### Returns
    ///
    /// the handler, or null
    ///
    /// #### Throws
    ///
    /// - `IOException`: when a setting cannot be read or a path is not absolute
    public static OpenApiDocs fromConfig(Config config, String document) throws IOException {
        if (document == null || !config.getBoolean(ENABLED, true)) {
            return null;
        }
        String path = trim(PATH, config.getRoutePath(PATH, "/v3/api-docs"));
        String uiPath = null;
        byte[] page = null;
        String policy = null;
        if (config.getBoolean(UI_ENABLED, true)) {
            uiPath = trim(UI_PATH, config.getRoutePath(UI_PATH, "/swagger-ui"));
            String assets = config.get(UI_ASSETS, DEFAULT_ASSETS);
            while (assets.endsWith("/")) {
                assets = assets.substring(0, assets.length() - 1);
            }
            // It goes into the page's policy as a source: a ';', a space or a
            // quote would add directives of its own.
            for (int i = 0; i < assets.length(); i++) {
                char c = assets.charAt(i);
                if (c <= ' ' || c > '~' || c == ';' || c == ',' || c == '\'' || c == '"') {
                    throw new IOException(UI_ASSETS + " must be a plain URL");
                }
            }
            page = utf8(page(assets, path));
            policy = policy(assets);
        }
        return new OpenApiDocs(path, uiPath, utf8(document), page, policy);
    }

    private static String trim(String key, String path) throws IOException {
        if (!path.startsWith("/")) {
            throw new IOException(key + " must start with /");
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /// The Swagger UI page: the distribution's own bundle, pointed at the document.
    static String page(String assets, String documentPath) {
        String a = html(assets);
        boolean pinned = DEFAULT_ASSETS.equals(assets);
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>API documentation</title>\n"
                + "<link rel=\"stylesheet\" href=\"" + a + "/swagger-ui.css\""
                + (pinned ? " integrity=\"" + DEFAULT_CSS_INTEGRITY + "\" crossorigin" : "")
                + ">\n</head>\n<body>\n<div id=\"swagger-ui\" data-url=\""
                + html(documentPath) + "\"></div>\n"
                + "<script src=\"" + a + "/swagger-ui-bundle.js\""
                + (pinned ? " integrity=\"" + DEFAULT_JS_INTEGRITY + "\"" : "")
                + " crossorigin></script>\n"
                + "<script>" + INLINE_SCRIPT + "</script>\n"
                + "</body>\n</html>\n";
    }

    /// The page's Content-Security-Policy: scripts only from the assets' origin
    /// and the one hashed inline script, requests only to this server. Swagger
    /// UI sets inline styles, hence `'unsafe-inline'` for styles alone.
    static String policy(String assets) {
        String origin = "'self'";
        int scheme = assets.indexOf("://");
        if (scheme > 0) {
            int slash = assets.indexOf('/', scheme + 3);
            origin = slash < 0 ? assets : assets.substring(0, slash);
        }
        return "default-src 'none'; script-src " + origin + " '" + INLINE_SCRIPT_HASH + "'; "
                + "style-src " + origin + " 'unsafe-inline'; img-src 'self' data: " + origin
                + "; font-src " + origin + "; connect-src 'self'; frame-ancestors 'none'; "
                + "base-uri 'none'; form-action 'none'";
    }

    private static String html(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                case '&': out.append("&amp;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }

    @Override
    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        String method = request.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return null;
        }
        String canonical = request.getTarget() == null ? null : request.pathFrom(0);
        if (canonical == null) {
            return null;
        }
        if (canonical.equals(path)) {
            return request.respond(200, "application/json; charset=utf-8", document);
        }
        if (uiPath != null && (canonical.equals(uiPath) || canonical.equals(uiPath + "/")
                || canonical.equals(uiPath + "/index.html"))) {
            return request.respond(200, "text/html; charset=utf-8", page)
                    .header("Content-Security-Policy", policy)
                    .header("X-Content-Type-Options", "nosniff");
        }
        return null;
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
