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

/// Where [EmailCodeService] keeps who signs in with a mailed code, and the
/// hash of the code each is waiting to enter.
public interface EmailCodeRepository {
    /// What is held for `username`, or null.
    EmailCode find(String username);

    /// Turns the mailed code on or off for `username`. Turning it off drops
    /// any code that is waiting.
    void setEnabled(String username, boolean enabled);

    /// Keeps a new code for `username` in place of the one waiting, when the
    /// user signs in with a mailed code and the last was sent at or before
    /// `sentBefore` -- or after `sentAt`, which is a clock that was set back.
    /// Of several callers at once one is answered true, so one mail leaves.
    ///
    /// The attempts counted against the code it replaces are counted against
    /// this one too, unless that code had run out or been used: asking for
    /// another is not a way to more tries. And a code with `max` attempts
    /// counted is not replaced before it runs out, so the wait that follows
    /// them ends at a moment that asking again does not move.
    boolean saveCode(String username, String codeHash, long expiresAt, long sentAt,
            long sentBefore, int max);

    /// Counts one attempt against the waiting code. False when `max` have
    /// been counted already; of several callers at once, no more than `max`
    /// are answered true.
    boolean countAttempt(String username, int max);

    /// Uses the waiting code up, if it is still the one with `codeHash`. Of
    /// two callers with the same code, one is answered true. `signedIn` says
    /// the code was typed in and accepted, which also forgets when it was
    /// sent: the next sign-in is then mailed a code at once. A code cleared
    /// because its mail failed keeps the time, so a failing send is still
    /// spaced out.
    boolean clear(String username, String codeHash, boolean signedIn);
}
