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
 * Typed error codes returned by {@link Biometrics} and {@link SecureStorage}
 * when an asynchronous operation fails. Callers branch on these codes via
 * {@link BiometricException#getError()} instead of string-matching error
 * messages, which makes localization and recovery logic straightforward.
 */
public enum BiometricError {
    /** Biometric hardware is not present, or is disabled by policy. */
    NOT_AVAILABLE,

    /** Hardware is present but the user has not enrolled any biometrics. */
    NOT_ENROLLED,

    /** Too many failed attempts; biometric prompt is temporarily disabled. */
    LOCKED_OUT,

    /**
     * Too many failed attempts and the user must unlock with their device
     * passcode or PIN before biometrics can be used again.
     */
    PERMANENTLY_LOCKED_OUT,

    /** The device has no passcode / PIN / pattern configured. */
    PASSCODE_NOT_SET,

    /** The user explicitly cancelled the prompt. */
    USER_CANCELED,

    /** The OS cancelled the prompt (app backgrounded, system pre-empted, etc.). */
    SYSTEM_CANCELED,

    /** Authentication completed but the user was not recognized. */
    AUTHENTICATION_FAILED,

    /**
     * A previously-stored {@link SecureStorage} entry can no longer be decrypted
     * because the user enrolled new biometrics or disabled device security since
     * the entry was written. Callers must re-prompt and re-write the entry.
     */
    KEY_REVOKED,

    /** Anything not covered by the more specific codes. */
    UNKNOWN
}
