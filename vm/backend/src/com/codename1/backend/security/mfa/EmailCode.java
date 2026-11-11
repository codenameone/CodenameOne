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
package com.codename1.backend.security.mfa;

/// What an [EmailCodeRepository] holds for one user.
public final class EmailCode {
    private final boolean enabled;
    private final String codeHash;
    private final long expiresAt;
    private final int attempts;
    private final long sentAt;

    public EmailCode(boolean enabled, String codeHash, long expiresAt, int attempts,
            long sentAt) {
        this.enabled = enabled;
        this.codeHash = codeHash == null ? "" : codeHash;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
        this.sentAt = sentAt;
    }

    /// Whether the user signs in with a mailed code.
    public boolean isEnabled() {
        return enabled;
    }

    /// The hash of the code that is waiting to be entered; empty when none is.
    public String getCodeHash() {
        return codeHash;
    }

    /// When the waiting code stops working, epoch milliseconds.
    public long getExpiresAt() {
        return expiresAt;
    }

    /// How many times a code has been tried against the waiting one.
    public int getAttempts() {
        return attempts;
    }

    /// When the waiting code was mailed, epoch milliseconds.
    public long getSentAt() {
        return sentAt;
    }
}
