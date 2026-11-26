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
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.marketing.Tokens;
import java.io.IOException;

/// The keys that sign what the server hands out and later takes back
/// unchanged: the bot guard's form tokens ([BotGuard]) and the theme preview
/// addresses (`content.Themes`).
///
/// They are derived from one secret, `bazaar.forms.signing-key` (the
/// environment's `BAZAAR_FORMS_SIGNING_KEY`), at least [#MIN_LENGTH]
/// characters. Every server behind a load balancer is given the same one,
/// and a restart keeps open forms and preview links working. Each use gets
/// a key of its own ([#key]), so a token signed for one is never accepted
/// by the other.
///
/// Without the setting -- or with one too short to be a secret -- the
/// server makes a random key when it starts and prints a `[config]` line
/// saying so: a warning on a development profile, an error on any other,
/// because forms then fail after every restart and across servers.
@Component
public class FormKeys {
    public static final String SETTING = "bazaar.forms.signing-key";
    public static final int MIN_LENGTH = 32;
    public static final String BOT_GUARD = "bot-guard";
    public static final String THEME_PREVIEW = "theme-preview";

    private final String master;
    private final boolean shared;

    @com.codename1.backend.annotations.Autowired
    public FormKeys(Config config) throws IOException {
        String configured = config.get(SETTING, "");
        String problem = problem(configured, config.isDevelopmentProfile(),
                config.getProfile());
        if (problem != null) {
            System.err.println("[config] " + problem);
            this.master = Tokens.token() + Tokens.token();
            this.shared = false;
        } else {
            this.master = configured.trim();
            this.shared = true;
        }
    }

    /// Keys from `master` as given; for tests.
    public FormKeys(String master) {
        this.master = master;
        this.shared = true;
    }

    /// What is wrong with `configured` on a profile, or null when it is a
    /// usable shared key.
    public static String problem(String configured, boolean development, String profile) {
        String key = configured == null ? "" : configured.trim();
        if (key.length() >= MIN_LENGTH) {
            return null;
        }
        String what = key.length() == 0 ? SETTING + " is not set"
                : SETTING + " is shorter than " + MIN_LENGTH + " characters";
        return development
                ? "warning: " + what + "; using a random key for this process (fine for "
                        + "development, but open forms and theme previews stop working on a "
                        + "restart)"
                : "error: " + what + " on the '" + profile + "' profile; using a random key "
                        + "for this process, so forms and theme previews fail after a restart "
                        + "and between servers. Set BAZAAR_FORMS_SIGNING_KEY, the same on "
                        + "every server";
    }

    /// Whether the key came from the configuration rather than this process.
    public boolean isShared() {
        return shared;
    }

    /// The key for one use, such as [#BOT_GUARD].
    public String key(String purpose) throws IOException {
        return Tokens.sign(master, "bazaar-form-key:" + purpose);
    }
}
