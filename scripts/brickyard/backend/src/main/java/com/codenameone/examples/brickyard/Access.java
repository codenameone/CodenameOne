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
import com.codenameone.examples.brickyard.domain.Experience;

/// Who may do what, checked in code beside the route matchers.
public final class Access {
    private Access() {
    }

    /// Refuses everyone but an engine server: scores, badges and data stores
    /// are the game's to write, never a player client's.
    public static void requireEngine() {
        if (!Caller.hasRole(Roles.ENGINE)) {
            throw new ResponseStatusException(403, "Only an engine server may do that");
        }
    }

    public static boolean isOwner(Experience e) {
        return e.getOwner().equals(Caller.name());
    }

    public static void requireOwner(Experience e) {
        if (!isOwner(e)) {
            throw new ResponseStatusException(403, "Only the experience's creator may do that");
        }
    }

    public static void requireEngineOrOwner(Experience e) {
        if (!Caller.hasRole(Roles.ENGINE) && !isOwner(e)) {
            throw new ResponseStatusException(403, "Only an engine server or the creator may do that");
        }
    }

    public static boolean isStaff() {
        return Caller.hasRole(Roles.MODERATOR) || Caller.hasRole(Roles.ADMIN);
    }
}
