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
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                login.setAccessToken(token(provider));
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

        public void nativeLogout() {
            setAccessToken(null);
        }

        public boolean isUserLoggedIn() {
            return getAccessToken() != null;
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

        public void nativeLogout() {
            setAccessToken(null);
        }

        public boolean isUserLoggedIn() {
            return getAccessToken() != null;
        }

        protected boolean validateToken(String token) {
            return token != null && token.startsWith("mock-");
        }
    }
}
