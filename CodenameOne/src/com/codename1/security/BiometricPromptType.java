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

/// What an application should call the device's biometric authentication in
/// its own UI -- the wording and icon of a "sign in with ..." button.
/// Returned from [Biometrics#getPromptType()].
///
/// It is one answer on every platform and OS release, so an application
/// needs no platform or version check to choose its wording.
public enum BiometricPromptType {

    /// Biometric authentication cannot be used right now: no hardware,
    /// nothing enrolled, or locked out. Offer the password only.
    NONE,

    /// Ask for a fingerprint (Touch ID on iOS).
    FINGERPRINT,

    /// Ask for the face (Face ID on iOS).
    FACE,

    /// Biometric authentication can be used and the device does not say
    /// which kind the user enrolled. Use neutral wording ("Use biometrics");
    /// the system prompt directs the user to the right sensor. An Android
    /// device with more than one kind of biometric hardware answers this,
    /// because Android reports that some biometric is enrolled and not
    /// which one.
    BIOMETRICS
}
