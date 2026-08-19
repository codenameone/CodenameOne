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
package com.codename1.impl.android.intents;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.security.SecureRandom;

/// The shared secret that distinguishes a shortcut this application published from a URI some
/// other app fabricated.
///
/// The trampoline has to be exported so the launcher can start a shortcut, and Android offers no
/// reliable way to identify who started an activity: `getReferrer()` returns a caller-supplied
/// extra in preference to anything the platform attributes, and `getCallingPackage()` is null
/// unless the caller used `startActivityForResult`. So authenticity travels in the URI instead.
///
/// The nonce is generated once, stored in the application's private preferences, and embedded in
/// every shortcut this application publishes at runtime. Another application cannot read it --
/// private preferences are not world-readable, and the launcher does not expose a shortcut's
/// intent extras to third parties -- so it cannot forge a trusted URI.
///
/// Build-time static shortcuts carry no nonce, because there is no runtime at build time. They
/// are handled by the trampoline's untrusted path, which is bounded to intents the launcher
/// already offers anyway.
final class CN1IntentNonce {

    private static final String TAG = "CN1Intents";
    private static final String PREFS = "cn1intents";
    private static final String KEY = "nonce";
    private static String cached;
    /// Held rather than constructed per call: a SecureRandom instantiated for a single use is
    /// both wasteful and, on some platforms, worse-seeded than one that has been reused.
    private static final SecureRandom RANDOM = new SecureRandom();

    private CN1IntentNonce() {
    }

    /// The nonce for this install, minting one on first use.
    static synchronized String get(Context ctx) {
        if (cached != null) {
            return cached;
        }
        if (ctx == null) {
            return null;
        }
        try {
            SharedPreferences prefs = ctx.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String existing = prefs.getString(KEY, null);
            if (existing == null || existing.length() == 0) {
                byte[] bytes = new byte[24];
                RANDOM.nextBytes(bytes);
                StringBuilder sb = new StringBuilder(bytes.length * 2);
                for (int i = 0; i < bytes.length; i++) {
                    sb.append(Character.forDigit((bytes[i] >> 4) & 0xf, 16));
                    sb.append(Character.forDigit(bytes[i] & 0xf, 16));
                }
                existing = sb.toString();
                // commit() rather than apply() so the answer is known here, and the answer is
                // acted on. A nonce that was not written is a secret that dies with the
                // process, and the shortcuts stamped with it do not: a donated or indexed
                // shortcut is durable, so after the next launch mints a different nonce the
                // launcher is left holding entries the trampoline refuses as unauthenticated.
                // Caching it anyway made that outcome certain instead of merely possible.
                if (!prefs.edit().putString(KEY, existing).commit()) {
                    Log.w(TAG, "Could not persist the intent nonce; not publishing shortcuts "
                            + "that would be refused after a restart");
                    return null;
                }
            }
            cached = existing;
            return cached;
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the intent nonce", t);
            return null;
        }
    }

    /// True when `candidate` is this install's nonce. A missing or empty candidate is never a
    /// match, so the absence of a nonce can only ever mean untrusted.
    static boolean matches(Context ctx, String candidate) {
        if (candidate == null || candidate.length() == 0) {
            return false;
        }
        String actual = get(ctx);
        if (actual == null || actual.length() != candidate.length()) {
            return false;
        }
        // Constant-time over the compared bytes; the length is already public.
        int diff = 0;
        for (int i = 0; i < actual.length(); i++) {
            diff |= actual.charAt(i) ^ candidate.charAt(i);
        }
        return diff == 0;
    }
}
