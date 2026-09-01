/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.social;

import com.codename1.io.AccessToken;
import com.codenameone.devruntime.DeviceRuntimeMocks;
import com.codename1.ui.Display;

/**
 * Social login that succeeds without anybody logging in.
 *
 * <p>Debugging a social flow on a device otherwise means real credentials, a
 * provider console entry naming this app's bundle id and signing certificate,
 * and a human typing a password on a phone. A runtime hosting somebody else's
 * program can arrange none of that, and none of it is what the developer is
 * trying to test -- what they are testing is what their code does after the
 * callback fires.</p>
 *
 * <p>In this package because that is where the seam is: a provider's
 * implementation registers itself with {@code implClass} and the port's own
 * {@code FacebookImpl} does exactly this, so {@code getInstance()} returns the
 * mock through the framework's own mechanism rather than through anything the
 * runtime bolts on. The constructors and the callback proxy are package-private
 * too, which is the other reason a subclass has to live here.</p>
 *
 * <p><b>No real identity is involved.</b> The token is fabricated, it
 * authenticates against nothing, and a server that accepts it is broken. The
 * runtime says so on screen the first time a pushed program logs in.</p>
 *
 * @author Shai Almog
 */
public final class DeviceRuntimeSocialMocks {
    private DeviceRuntimeSocialMocks() {
    }

    /// Registers the mocks as the providers' implementations.
    public static void install() {
        FacebookConnect.setImplClass(Facebook.class);
        GoogleConnect.setImplClass(Google.class);
    }

    /**
     * Forgets what the last pushed program did.
     *
     * <p>A provider is a singleton and the framework caches it, so without this
     * the next program starts already logged in as the previous one -- and
     * holds a callback belonging to a runtime that has since been detached, so
     * a later login would call into a dead program before reaching the live
     * one. {@code doLogout()} is the API's own way to clear it: token, native
     * state and the preference it was persisted under.</p>
     */
    public static void reset() {
        generation++;
        reset(FacebookConnect.getInstance());
        reset(GoogleConnect.getInstance());
    }

    private static void reset(Login login) {
        if (login instanceof Facebook) {
            ((Facebook) login).remember(null);
        } else if (login instanceof Google) {
            ((Google) login).remember(null);
        }
        try {
            login.doLogout();
        } catch (Throwable alreadyGone) {
            // Nothing was logged in, which is the outcome either way.
        }
        login.setAccessToken(null);
        // And the persisted copy. getAccessToken() re-reads from Storage
        // whenever its field is null, so clearing the field alone hands the
        // next program the previous one's token back -- doLogout deletes the
        // Preferences entry but not this one.
        com.codename1.io.Storage.getInstance()
                .deleteStorageFile(login.getClass().getName() + "AccessToken");
        login.setCallback(null);
    }

    /**
     * Which pushed program the current logins belong to.
     *
     * <p>A login completes on a later pass of the event thread, so a program
     * can be replaced between asking and being answered. Without this the
     * queued completion issues and stores a token *after* the reset, and the
     * new program starts logged in as the old one -- the very thing the reset
     * exists to prevent.</p>
     */
    private static int generation;

    /// A token that reads as fake, including in a log somebody pastes later.
    static AccessToken token(String provider) {
        return new AccessToken("mock-" + provider.toLowerCase() + "-token-not-valid-anywhere",
                String.valueOf(System.currentTimeMillis() + 3600000L));
    }

    /**
     * Completes a login on the event thread.
     *
     * <p>A real provider calls back there, and pushed code touches its UI from
     * the callback -- delivering on the caller's thread is the sort of
     * difference that works in a simulator and fails on a device.</p>
     */
    static void succeed(final Login login, final String provider) {
        DeviceRuntimeMocks.warnOnce(provider + " login");
        final int asked = generation;
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (asked != generation) {
                    // The program that asked is gone. Answering it now would
                    // log the *next* program in, and call a callback the
                    // detached runtime owns.
                    return;
                }
                AccessToken issued = token(provider);
                if (login instanceof Facebook) {
                    ((Facebook) login).remember(issued);
                } else if (login instanceof Google) {
                    ((Google) login).remember(issued);
                }
                login.setAccessToken(issued);
                login.callback.loginSuccessful();
            }
        });
    }

    /** Facebook, answered locally. */
    public static final class Facebook extends FacebookConnect {
        public boolean isFacebookSDKSupported() {
            return true;
        }

        public boolean isNativeLoginSupported() {
            return true;
        }

        public void login() {
            succeed(this, "Facebook");
        }

        public void nativelogin() {
            succeed(this, "Facebook");
        }

    /**
     * What a provider mock has to answer once it claims native login.
     *
     * <p>Claiming it is not free: {@code FacebookConnect.getAccessToken()} falls
     * through to {@code getToken()} when no token is stored, and the base
     * class's {@code getToken()} and {@code nativeIsLoggedIn()} both throw. A
     * mock that overrode only the login methods therefore worked until the
     * first logged-out call and then failed inside the framework, which is a
     * confusing place for a mock to fail.</p>
     *
     * <p>The token is held here rather than read back through the provider,
     * because reading it means calling the provider's own
     * {@code getAccessToken()} -- the method this exists to keep honest.</p>
     */
    private AccessToken current;

    void remember(AccessToken token) {
        current = token;
    }

    public AccessToken getAccessToken() {
        return current;
    }

    public String getToken() {
        return current == null ? null : current.getToken();
    }

    public boolean nativeIsLoggedIn() {
        return current != null;
    }

    public boolean isUserLoggedIn() {
        return current != null;
    }

    public void nativeLogout() {
        current = null;
        setAccessToken(null);
    }

    protected boolean validateToken(String token) {
        // Valid for this session and no longer: saying otherwise would send
        // pushed code round a refresh loop it cannot win.
        return token != null && token.startsWith("mock-");
    }

    }

    /** Google, the same way. */
    public static final class Google extends GoogleConnect {
        public boolean isNativeLoginSupported() {
            return true;
        }

        public void login() {
            succeed(this, "Google");
        }

        public void nativelogin() {
            succeed(this, "Google");
        }

    /**
     * What a provider mock has to answer once it claims native login.
     *
     * <p>Claiming it is not free: {@code FacebookConnect.getAccessToken()} falls
     * through to {@code getToken()} when no token is stored, and the base
     * class's {@code getToken()} and {@code nativeIsLoggedIn()} both throw. A
     * mock that overrode only the login methods therefore worked until the
     * first logged-out call and then failed inside the framework, which is a
     * confusing place for a mock to fail.</p>
     *
     * <p>The token is held here rather than read back through the provider,
     * because reading it means calling the provider's own
     * {@code getAccessToken()} -- the method this exists to keep honest.</p>
     */
    private AccessToken current;

    void remember(AccessToken token) {
        current = token;
    }

    public AccessToken getAccessToken() {
        return current;
    }

    public String getToken() {
        return current == null ? null : current.getToken();
    }

    public boolean nativeIsLoggedIn() {
        return current != null;
    }

    public boolean isUserLoggedIn() {
        return current != null;
    }

    public void nativeLogout() {
        current = null;
        setAccessToken(null);
    }

    protected boolean validateToken(String token) {
        // Valid for this session and no longer: saying otherwise would send
        // pushed code round a refresh loop it cannot win.
        return token != null && token.startsWith("mock-");
    }

    }
}
