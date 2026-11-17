/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;

/// The staff roles and what each may do. ADMIN may do everything; a
/// MODERATOR handles reports, sanctions, appeals and unpublishing; SUPPORT
/// reads the queues and the audit log and refunds purchases. Every staff
/// route checks one of these in code, so a route without a matcher in
/// `SecurityConfig` is still refused to a player.
public final class Staff {
    /// Reports, sanctions, appeals and unpublishing.
    public static final String MODERATE = "MODERATE";
    /// Reading the report queue, sanctions, the audit log and fraud flags.
    public static final String VIEW = "VIEW";
    /// Refunding a store purchase.
    public static final String REFUND = "REFUND";
    /// Everything else: Bricks grants, roles, featuring, KPIs, payouts, quests, jobs.
    public static final String ADMINISTER = "ADMINISTER";

    private Staff() {
    }

    public static boolean may(String permission) {
        if (Caller.hasRole(Roles.ADMIN)) {
            return true;
        }
        boolean mod = Caller.hasRole(Roles.MODERATOR);
        boolean support = Caller.hasRole(Roles.SUPPORT);
        if (MODERATE.equals(permission)) {
            return mod;
        }
        if (VIEW.equals(permission)) {
            return mod || support;
        }
        if (REFUND.equals(permission)) {
            return support;
        }
        return false;
    }

    public static void require(String permission) {
        if (!may(permission)) {
            throw new ResponseStatusException(403, "Your staff role does not allow that");
        }
    }
}
