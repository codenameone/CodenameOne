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

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;

import java.util.LinkedHashMap;
import java.util.Map;

/// The apps' token endpoint, `POST /api/apps/oauth/token`: an authorization
/// code, the client's credentials and the PKCE verifier in, an access token
/// out ([Apps#exchange]). Takes a form (`grant_type=authorization_code`,
/// `client_id`, `client_secret`, `code`, `code_verifier`, `redirect_uri`) as
/// OAuth does, or the same fields as a JSON object. Nobody is signed in to
/// call it; the client secret is the credential.
@RestController
public class AppOAuth {
    public static final String TOKEN_PATH = "/api/apps/oauth/token";

    private final Apps apps;

    public AppOAuth(Apps apps) {
        this.apps = apps;
    }

    @PostMapping(TOKEN_PATH)
    public HttpServer.Response token(HttpServer.Request request) throws Exception {
        String body = request.getBody() == null ? "" : request.getBody();
        Map fields;
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            fields = Json.parseObject(trimmed);
        } else {
            fields = OAuth2Parameters.parse(body);
        }
        if (fields == null || !"authorization_code".equals(text(fields, "grant_type"))) {
            return error(400, "unsupported_grant_type", "Only authorization_code is supported");
        }
        try {
            return HttpServer.Response.json(200, Json.write(apps.exchange(
                    text(fields, "client_id"), text(fields, "client_secret"),
                    text(fields, "code"), text(fields, "code_verifier"),
                    text(fields, "redirect_uri"))))
                    .header("Cache-Control", "no-store").header("Pragma", "no-cache");
        } catch (Apps.TokenError e) {
            return error(e.status, e.error, e.getMessage());
        }
    }

    private static String text(Map fields, String name) {
        Object v = fields.get(name);
        return v instanceof String ? (String) v : null;
    }

    private static HttpServer.Response error(int status, String code, String description) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("error", code);
        out.put("error_description", description);
        return HttpServer.Response.json(status, Json.write(out)).header("Cache-Control",
                "no-store");
    }
}
