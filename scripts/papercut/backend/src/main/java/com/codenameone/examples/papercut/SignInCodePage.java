/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut;

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/// Where a sign-in from a browser ends: the authorization code, handed back.
///
/// The installed app asks for its code to be sent to a loopback address and
/// reads it off the redirect without following it. A page in a browser cannot
/// do that -- the browser follows the redirect itself and shows the page only
/// what it found at the end -- so the app running in one asks for the code to
/// be sent here, and what is at the end is the code.
///
/// Nothing is decided here and nothing is trusted: the code arrived in the
/// address, so repeating it tells the caller nothing it did not have, and it
/// is worth nothing without the PKCE verifier only the app that started the
/// sign-in holds. That is why this needs no sign-in of its own.
@RestController
public class SignInCodePage {
    @GetMapping(SecurityConfig.WEB_REDIRECT_PATH)
    public HttpServer.Response code(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error) throws Exception {
        Map<String, Object> answer = new LinkedHashMap<String, Object>();
        if (code != null) {
            answer.put("code", code);
        }
        if (state != null) {
            answer.put("state", state);
        }
        if (error != null) {
            answer.put("error", error);
        }
        return new HttpServer.Response(code == null ? 400 : 200, "application/json",
                Json.write(answer).getBytes("UTF-8"))
                // A code is good once, for a few minutes; nothing should keep it.
                .header("Cache-Control", "no-store")
                // And nothing should read it as anything but data: the values
                // come from the address, which anyone can write a link to.
                .header("X-Content-Type-Options", "nosniff");
    }
}
