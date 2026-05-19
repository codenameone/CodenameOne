/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/**
 * Enumerates the biometric authentication modalities that may be available on a
 * device. Returned from {@link Biometrics#getAvailableBiometrics()}.
 *
 * <p>{@link #FINGERPRINT} and {@link #FACE} are populated on both iOS and
 * Android. {@link #IRIS} only appears on Android devices whose hardware
 * advertises {@code PackageManager.FEATURE_IRIS}. {@link #STRONG} and
 * {@link #WEAK} reflect Android's BiometricManager authenticator class tiers
 * (class 3 and class 2) and are only populated on Android API 30+.</p>
 */
public enum BiometricType {
    FINGERPRINT,
    FACE,
    IRIS,
    STRONG,
    WEAK
}
