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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// [Biometrics#getPromptType()] over what a port can report: one modality is
/// named, and anything else that can authenticate is generic.
class BiometricPromptTypeTest {

    private static BiometricPromptType of(final boolean can, final BiometricType... types) {
        return new Biometrics() {
            @Override
            public boolean canAuthenticate() {
                return can;
            }

            @Override
            public List<BiometricType> getAvailableBiometrics() {
                return types == null ? null : Arrays.asList(types);
            }
        }.getPromptType();
    }

    @Test
    void nothingUsableIsNone() {
        // The base class is what a port without biometrics hands out.
        assertEquals(BiometricPromptType.NONE, new Biometrics() { }.getPromptType());
        assertEquals(BiometricPromptType.NONE, of(false));
        // Hardware listed but locked out or not enrolled: still nothing to ask for.
        assertEquals(BiometricPromptType.NONE, of(false, BiometricType.FINGERPRINT));
    }

    @Test
    void singleModalityIsNamed() {
        assertEquals(BiometricPromptType.FINGERPRINT, of(true, BiometricType.FINGERPRINT));
        assertEquals(BiometricPromptType.FACE, of(true, BiometricType.FACE));
        // The authenticator class tags are not modalities.
        assertEquals(BiometricPromptType.FINGERPRINT,
                of(true, BiometricType.FINGERPRINT, BiometricType.STRONG));
        assertEquals(BiometricPromptType.FACE,
                of(true, BiometricType.FACE, BiometricType.WEAK, BiometricType.STRONG));
    }

    @Test
    void anythingElseIsGeneric() {
        // An Android device with both sensors: which one is enrolled is not reported.
        assertEquals(BiometricPromptType.BIOMETRICS,
                of(true, BiometricType.FINGERPRINT, BiometricType.FACE, BiometricType.STRONG));
        assertEquals(BiometricPromptType.BIOMETRICS, of(true, BiometricType.FACE, BiometricType.IRIS));
        assertEquals(BiometricPromptType.BIOMETRICS, of(true, BiometricType.FINGERPRINT, BiometricType.IRIS));
        assertEquals(BiometricPromptType.BIOMETRICS, of(true, BiometricType.IRIS));
        assertEquals(BiometricPromptType.BIOMETRICS, of(true));
        assertEquals(BiometricPromptType.BIOMETRICS, of(true, BiometricType.STRONG));
        assertEquals(BiometricPromptType.BIOMETRICS, of(true, (BiometricType[]) null));
    }
}
