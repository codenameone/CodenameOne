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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bazaar.domain.Profile;
import com.codenameone.examples.bazaar.repository.ProfileRepository;

/// The shopper's language picker: a form on every storefront and account
/// page posts `lang` (a code, or `auto` to forget the choice) and `back`, the
/// page to return to. The choice is the `lang` cookie, which outranks the
/// market's locale and `Accept-Language` ([StoreText#chosen]).
///
/// It is an ordinary form post, so the picker works without script. Like the
/// consent form it carries no token (storefront pages are shared-cached and
/// hold none), so a post from another site is refused instead
/// ([Storefront#sameSite]); a cookie naming a language is all it changes.
@Controller
public class LanguageChoice {
    /// A year, like the consent cookie.
    static final int MAX_AGE = 365 * 24 * 3600;

    private final boolean secureCookies;
    private final ProfileRepository profiles;

    public LanguageChoice(@Value("${bazaar.web.hsts:true}") boolean secureCookies,
            ProfileRepository profiles) {
        this.secureCookies = secureCookies;
        this.profiles = profiles;
    }

    @PostMapping("/lang")
    public HttpServer.Response choose(HttpServer.Request request) {
        Storefront.sameSite(request);
        String lang = param(request, "lang");
        String cookie = StoreText.supported(lang)
                ? StoreText.COOKIE + "=" + lang + "; Path=/; Max-Age=" + MAX_AGE
                : StoreText.COOKIE + "=; Path=/; Max-Age=0";
        remember(lang);
        return HttpServer.Response.text(303, "")
                .header("Location", StoreText.back(param(request, "back")))
                .header("Set-Cookie", cookie + "; SameSite=Lax; HttpOnly"
                        + (secureCookies ? "; Secure" : ""))
                .header("Cache-Control", "no-store");
    }

    /// A signed-in shopper's choice is the account's language too, which the
    /// store's mail to them follows ([com.codenameone.examples.bazaar.notify.ShopperLanguage]).
    private void remember(String lang) {
        String me = CheckoutPages.signedIn();
        if (me == null || !StoreText.supported(lang)) {
            return;
        }
        try {
            Profile p = profiles.findById(me);
            if (p != null && !lang.equals(p.getLanguage())) {
                p.setLanguage(lang);
                profiles.save(p);
            }
        } catch (RuntimeException failed) {
            // The cookie is the choice; the account's copy is a convenience.
            System.err.println("[lang] the account's language was not saved: " + failed);
        }
    }

    private static String param(HttpServer.Request request, String name) {
        try {
            return request.param(name);
        } catch (RuntimeException malformed) {
            return null;
        }
    }
}
