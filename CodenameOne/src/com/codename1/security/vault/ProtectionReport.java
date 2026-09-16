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
package com.codename1.security.vault;

/// What a store, a key or a vault actually provides, one [Protection] at a time.
///
/// Three answers rather than two. `UNKNOWN` is what a port returns when the platform will not
/// tell it -- a browser has no way to learn whether a `CryptoKey` ended up in a secure element,
/// and the honest report of that is not `NO` (which would understate an authenticator that does
/// use one) and certainly not `YES`. Callers that need a guarantee must treat `UNKNOWN` as "not
/// provided"; callers that are only describing the state to a user can say so.
///
/// A report describes what was **observed**, not what an API exists for. A port that finds
/// `crypto.subtle` present but cannot complete a round trip through it reports
/// [Protection#ENCRYPTED_AT_REST] as `NO`.
public final class ProtectionReport {

    /// The protection is provided.
    public static final int YES = 1;

    /// The protection is not provided.
    public static final int NO = 0;

    /// The platform cannot say. Treat as not provided when a guarantee is required.
    public static final int UNKNOWN = -1;

    /// One answer per [Protection], indexed by ordinal. An array rather than a map because
    /// `EnumMap` is not part of the class library the core compiles against, and because the set
    /// is fixed and tiny.
    private final int[] answers;

    private ProtectionReport(int[] answers) {
        this.answers = answers;
    }

    /// Starts building a report. Every protection not explicitly set answers [#UNKNOWN], which is
    /// the correct default for a port that has not considered the question.
    public static Builder builder() {
        return new Builder();
    }

    /// A report that answers [#NO] to everything. What the fallback returns on a platform with no
    /// secure store at all.
    public static ProtectionReport none() {
        Builder b = new Builder();
        for (Protection p : Protection.values()) {
            b.set(p, NO);
        }
        return b.build();
    }

    /// A report that answers [#UNKNOWN] to everything. What to return when the store could not be
    /// reached at all, as distinct from a store that was reached and provides nothing.
    public static ProtectionReport unknown() {
        return new Builder().build();
    }

    /// The answer for one protection: [#YES], [#NO] or [#UNKNOWN].
    ///
    /// #### Parameters
    ///
    /// - `protection`: the protection to ask about
    ///
    /// #### Returns
    ///
    /// one of the three answers, never an exception for an unconsidered protection
    public int answer(Protection protection) {
        if (protection == null) {
            return UNKNOWN;
        }
        int index = protection.ordinal();
        return index < answers.length ? answers[index] : UNKNOWN;
    }

    /// Whether this report provides the protection, with `UNKNOWN` counting as no.
    ///
    /// The method to call when a policy has to be enforced. Use [#answer(Protection)] when the
    /// difference between "no" and "cannot say" matters to the caller, which it does when the
    /// answer is being shown to a user.
    public boolean provides(Protection protection) {
        return answer(protection) == YES;
    }

    /// Whether every requirement in `required` is answered [#YES].
    ///
    /// #### Parameters
    ///
    /// - `required`: the protections a caller insists on, may be null or empty
    ///
    /// #### Returns
    ///
    /// true when all of them are provided
    public boolean satisfies(Protection[] required) {
        if (required == null) {
            return true;
        }
        for (Protection protection : required) {
            if (!provides(protection)) {
                return false;
            }
        }
        return true;
    }

    /// The first requirement this report does not provide, for an error message that names the
    /// thing that was missing rather than saying the request was refused.
    ///
    /// #### Parameters
    ///
    /// - `required`: the protections a caller insists on, may be null
    ///
    /// #### Returns
    ///
    /// the unmet protection, or null when everything is provided
    public Protection firstUnmet(Protection[] required) {
        if (required == null) {
            return null;
        }
        for (Protection protection : required) {
            if (protection == null) {
                // A null is not an unmet requirement, and it must not become one: provides(null)
                // is false, so returning it here handed the caller this method's success sentinel
                // -- null means "everything is provided" -- for an array that had not been
                // checked past that point. {null, ENCRYPTED_AT_REST} therefore reported every
                // requirement met, and SecureStorage wrote into a store with no encryption at all.
                continue;
            }
            if (!provides(protection)) {
                return protection;
            }
        }
        return null;
    }

    /// A human readable line per protection, for diagnostics and for the "what protects this"
    /// screen an application that cares about this will end up writing.
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        Protection[] all = Protection.values();
        for (int iter = 0; iter < all.length; iter++) {
            if (iter > 0) {
                b.append(", ");
            }
            b.append(all[iter].name()).append('=');
            switch (answer(all[iter])) {
                case YES:
                    b.append("yes");
                    break;
                case NO:
                    b.append("no");
                    break;
                default:
                    b.append("unknown");
                    break;
            }
        }
        return b.toString();
    }

    /// Accumulates the answers for a [ProtectionReport].
    public static final class Builder {
        private final int[] answers = new int[Protection.values().length];

        Builder() {
            for (int iter = 0; iter < answers.length; iter++) {
                answers[iter] = UNKNOWN;
            }
        }

        /// Records one answer. `value` outside the three constants is stored as [#UNKNOWN] rather
        /// than accepted, so a port that passes a boolean by mistake cannot claim a protection.
        public Builder set(Protection protection, int value) {
            if (protection != null) {
                answers[protection.ordinal()] = (value == YES || value == NO) ? value : UNKNOWN;
            }
            return this;
        }

        /// Records `yes` or `no`. There is no boolean spelling of `UNKNOWN` on purpose: a port
        /// that does not know must say so through [#set(Protection, int)].
        public Builder set(Protection protection, boolean value) {
            return set(protection, value ? YES : NO);
        }

        /// Builds the immutable report.
        public ProtectionReport build() {
            int[] copy = new int[answers.length];
            System.arraycopy(answers, 0, copy, 0, answers.length);
            return new ProtectionReport(copy);
        }
    }
}
