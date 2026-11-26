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
package com.codenameone.examples.bazaar.web.security;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.SecurityFilter;
import com.codenameone.examples.bazaar.marketing.Tokens;
import java.io.IOException;

/// Bot protection for the web forms a script would hammer -- sign-in and
/// placing an order -- with no third-party CAPTCHA:
///
/// - **A honeypot.** The form carries a `website` field people never see
///   (`.hp`, off screen and out of the tab order); a form filler fills it.
/// - **Timing.** The page carries `_ft`, the time it was drawn signed with a
///   key from [FormKeys] ([#issue]). A post without it, with a
///   forged one, sooner than `bazaar.bot.min-fill-ms` (1.5 seconds) after
///   the page, or later than six hours, is refused: a script that posts
///   without loading the page, or the instant it loads, fails.
/// - **The throttle**, which the chains already apply to both posts.
///
/// A refused post answers 400 with a page saying to try again; nothing is
/// signed in or charged. The key comes from the configuration, so a form
/// left open over a restart, or posted to another server, still passes.
@Component
public class BotGuard {
    public static final String TOKEN = "_ft";
    public static final String HONEYPOT = "website";
    static final long MAX_AGE = 6L * 3600L * 1000L;

    private final String key;
    private long minFillMillis;

    public BotGuard(Config config, FormKeys keys) throws IOException {
        this.key = keys.key(FormKeys.BOT_GUARD);
        this.minFillMillis = parse(config.get("bazaar.bot.min-fill-ms", "1500"));
    }

    private static long parse(String s) {
        long out = 0;
        for (int iter = 0; s != null && iter < s.length() && iter < 9; iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return 1500;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }

    /// For tests: how soon after the page a post may come.
    public void setMinFillMillis(long millis) {
        this.minFillMillis = Math.max(0, millis);
    }

    /// The token a page puts in its form now.
    public String issue() throws IOException {
        String t = Long.toString(System.currentTimeMillis());
        return t + "." + Tokens.sign(key, t);
    }

    /// Why a post with these fields is a bot's, or null when it passes.
    public String refusal(String token, String honeypot, long now) throws IOException {
        if (honeypot != null && honeypot.length() > 0) {
            return "honeypot";
        }
        int dot = token == null ? -1 : token.indexOf('.');
        if (dot <= 0 || dot > 15) {
            return "no token";
        }
        String t = token.substring(0, dot);
        for (int iter = 0; iter < t.length(); iter++) {
            if (t.charAt(iter) < '0' || t.charAt(iter) > '9') {
                return "bad token";
            }
        }
        if (!Tokens.verify(key, t, token.substring(dot + 1))) {
            return "bad token";
        }
        long age = now - Long.parseLong(t);
        if (age < minFillMillis) {
            return "too fast";
        }
        return age > MAX_AGE ? "expired" : null;
    }

    static boolean guarded(HttpServer.Request request) {
        if (!"POST".equals(request.getMethod())) {
            return false;
        }
        String p = request.pathFrom(0);
        if ("/auth/signin".equals(p) || "/auth/signup".equals(p)) {
            return true;
        }
        return p != null && p.startsWith("/s/") && p.endsWith("/checkout/place");
    }

    /// The filter for the web app chain.
    public SecurityFilter filter() {
        return (request, chain) -> {
            if (guarded(request)) {
                String token;
                String trap;
                try {
                    token = request.param(TOKEN);
                    trap = request.param(HONEYPOT);
                } catch (RuntimeException malformed) {
                    token = null;
                    trap = null;
                }
                if (refusal(token, trap, System.currentTimeMillis()) != null) {
                    SecurityExchange ex = SecurityExchange.current();
                    if (ex != null) {
                        ex.setResponseHeader("Cache-Control", "no-store");
                    }
                    return HttpServer.Response.text(400, "That form was sent too quickly or "
                            + "from an old page. Go back, reload the page and try again.");
                }
            }
            return chain.doFilter(request);
        };
    }
}
