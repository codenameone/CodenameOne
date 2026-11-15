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
package com.codenameone.examples.brickyard.moderation;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.domain.Sanction;
import com.codenameone.examples.brickyard.repository.SanctionRepository;

/// Whether an account is banned or muted now. The routes that spend, chat or
/// meet other players ask here; reading stays open so a banned player can
/// still see why and appeal.
@Component
public class Sanctions {
    public static final String WARN = "WARN";
    public static final String TEMP_BAN = "TEMP_BAN";
    public static final String PERM_BAN = "PERM_BAN";
    public static final String MUTE = "MUTE";

    private final SanctionRepository sanctions;

    public Sanctions(SanctionRepository sanctions) {
        this.sanctions = sanctions;
    }

    public boolean has(String username, String kind) {
        long now = Clock.now();
        for (Sanction s : sanctions.of(username)) {
            if (s.getLifted() == 0 && s.getUntilAt() > now && s.getKind().equals(kind)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBanned(String username) {
        return has(username, TEMP_BAN) || has(username, PERM_BAN);
    }

    public void requireNotBanned(String username) {
        if (isBanned(username)) {
            throw new ResponseStatusException(403, "This account is banned");
        }
    }

    public void requireCanChat(String username) {
        requireNotBanned(username);
        if (has(username, MUTE)) {
            throw new ResponseStatusException(403, "This account is muted");
        }
    }
}
