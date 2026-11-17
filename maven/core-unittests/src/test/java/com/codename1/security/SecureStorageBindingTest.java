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
package com.codename1.security;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// The biometric key is bound to the enrolled biometrics, always.
///
/// From 2022 the FingerprintScanner cn1lib, and from its import the core Android
/// storage, created the key WITHOUT `setUserAuthenticationRequired` on API 33+ to
/// side-step a "Key user not authenticated" error. Such a key decrypts with no
/// authentication and is never invalidated by a new enrolment: whoever knows the
/// device PIN could enrol a fingerprint and unlock every entry, while the prompt
/// still looked like protection. Every test passed, because the round trip works
/// the same with an unbound key. This holds the binding in the source.
class SecureStorageBindingTest {
    private static final String ANDROID =
            "../../Ports/Android/src/com/codename1/impl/android/AndroidSecureStorage.java";

    @Test
    void bindingIsAlwaysOn() {
        SecureStorage storage = new SecureStorage() {
        };
        assertTrue(storage.isStrictBiometricBinding());
        storage.setStrictBiometricBinding(false);
        assertTrue(storage.isStrictBiometricBinding());
    }

    @Test
    void theAndroidBiometricKeyIsBoundUnconditionally() throws IOException {
        String source = code(read(new File(ANDROID)));
        assertTrue(source.indexOf("setUserAuthenticationRequired(false)") < 0,
                "no key in the biometric storage may be created unbound");
        int at = source.indexOf("private boolean createKey()");
        assertTrue(at > 0, "createKey not found in " + ANDROID);
        String body = source.substring(at, source.indexOf("keyGenerator.init(", at));
        int builder = body.indexOf("new KeyGenParameterSpec.Builder(KEY_ID");
        int bind = body.indexOf("setUserAuthenticationRequired(true)");
        assertTrue(builder > 0 && bind > builder,
                "createKey must bind the biometric key: " + body);
        String between = body.substring(builder, bind);
        assertTrue(between.indexOf("if (") < 0 && between.indexOf("else") < 0
                        && body.substring(0, builder).indexOf("if (") < 0,
                "the binding must not depend on a condition (API level, opt-in): " + body);
        assertTrue(body.indexOf("setInvalidatedByBiometricEnrollment(true)") > 0,
                "createKey must revoke the key on a new enrolment: " + body);
    }

    @Test
    void anUnboundKeyFromAnEarlierReleaseIsNotUsed() throws IOException {
        String source = code(read(new File(ANDROID)));
        int at = source.indexOf("private KeyLookup lookupKey()");
        assertTrue(at > 0, "lookupKey not found in " + ANDROID);
        String body = source.substring(at, source.indexOf("private KeyLookup replaceKey(", at));
        assertTrue(body.indexOf("bindingOf(k)") > 0 && body.indexOf("BINDING_NONE") > 0,
                "lookupKey must refuse a key the keystore does not bind: " + body);
    }

    /// The source with comments removed, so prose cannot satisfy or trip a check.
    private static String code(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"') {
                    j += s.charAt(j) == '\\' ? 2 : 1;
                }
                out.append(s, i, Math.min(j + 1, s.length()));
                i = j + 1;
            } else if (s.startsWith("//", i)) {
                int nl = s.indexOf('\n', i);
                i = nl < 0 ? s.length() : nl;
            } else if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String read(File f) throws IOException {
        assertTrue(f.isFile(), "source not found: " + f.getAbsolutePath());
        byte[] raw = new byte[(int) f.length()];
        InputStream in = new FileInputStream(f);
        try {
            int read = 0;
            while (read < raw.length) {
                int n = in.read(raw, read, raw.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
        } finally {
            in.close();
        }
        return new String(raw, "UTF-8");
    }
}
