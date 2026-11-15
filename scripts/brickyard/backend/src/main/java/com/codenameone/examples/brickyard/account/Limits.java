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
package com.codenameone.examples.brickyard.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.api.LimitsDto;
import com.codenameone.examples.brickyard.api.ProfileDto;
import com.codenameone.examples.brickyard.domain.Account;
import com.codenameone.examples.brickyard.domain.AccountSettings;
import com.codenameone.examples.brickyard.repository.AccountSettingsRepository;
import java.io.IOException;

/// What the server keeps per account beyond the account row: avatar
/// colours, and the parental controls it enforces -- a monthly Bricks
/// spending limit checked on every purchase, and chat on or off checked on
/// every line sent. A player under [#CHILD_AGE] starts with chat off and a
/// limit of zero.
@Component
public class Limits {
    public static final int CHILD_AGE = 13;
    /// The spending window: thirty days, counted from the epoch.
    static final long MONTH = 30 * Clock.DAY;
    static final int MAX_AVATAR = 200;

    private final AccountSettingsRepository settings;
    private final Accounts accounts;

    public Limits(AccountSettingsRepository settings, Accounts accounts) {
        this.settings = settings;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public ProfileDto profile(String username) {
        Account a = accounts.account(username);
        AccountSettings s = settings.findById(username);
        return new ProfileDto(a.getDisplayName(), s == null ? "" : s.getAvatar());
    }

    /// Saves the avatar colours: up to 200 characters of six-digit hex
    /// colours separated by commas.
    @Transactional(rollbackFor = IOException.class)
    public void saveAvatar(String username, String avatar) {
        String v = avatar == null ? "" : avatar.trim();
        if (v.length() > MAX_AVATAR) {
            throw new ResponseStatusException(400, "Too many avatar colours");
        }
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            boolean ok = c == '#' || c == ',' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!ok) {
                throw new ResponseStatusException(400, "Avatar colours are #rrggbb values separated by commas");
            }
        }
        AccountSettings s = row(username);
        s.setAvatar(v);
        write(s);
    }

    @Transactional(readOnly = true)
    public LimitsDto limits(String username) {
        AccountSettings s = row(username);
        return new LimitsDto(s.getSpendLimit(), spentNow(s), s.getChatEnabled() != 0);
    }

    @Transactional(rollbackFor = IOException.class)
    public LimitsDto save(String username, LimitsDto request) {
        if (request == null) {
            throw new ResponseStatusException(400, "No limits");
        }
        AccountSettings s = row(username);
        s.setSpendLimit(request.spendLimit() < 0 ? -1 : Math.min(request.spendLimit(), 1000000));
        s.setChatEnabled(request.chatEnabled() ? 1 : 0);
        write(s);
        return limits(username);
    }

    /// Refuses (402) a purchase of `bricks` that would pass this month's
    /// limit, and otherwise counts it. Call inside the buying transaction.
    public void spend(String username, long bricks) {
        AccountSettings s = row(username);
        long spent = spentNow(s);
        if (s.getSpendLimit() >= 0 && spent + bricks > s.getSpendLimit()) {
            throw new ResponseStatusException(402, "That would pass the monthly spending limit");
        }
        s.setSpent(spent + bricks);
        s.setSpentMonth(Clock.now() / MONTH);
        write(s);
    }

    /// Refuses (403) chat for an account whose chat is off.
    public void requireChat(String username) {
        AccountSettings s = settings.findById(username);
        boolean on = s == null ? accounts.age(username) >= CHILD_AGE : s.getChatEnabled() != 0;
        if (!on) {
            throw new ResponseStatusException(403, "Chat is turned off for this account");
        }
    }

    private static long spentNow(AccountSettings s) {
        return s.getSpentMonth() == Clock.now() / MONTH ? s.getSpent() : 0;
    }

    /// The account's row, or the defaults for its age when it has none yet
    /// (an unsaved row: [#write] inserts it).
    private AccountSettings row(String username) {
        AccountSettings s = settings.findById(username);
        if (s == null) {
            boolean child = accounts.age(username) < CHILD_AGE;
            s = new AccountSettings();
            s.setId(username);
            s.setAvatar("");
            s.setSpendLimit(child ? 0 : -1);
            s.setChatEnabled(child ? 0 : 1);
            s.setSpentMonth(Clock.now() / MONTH);
        }
        return s;
    }

    private void write(AccountSettings s) {
        if (settings.findById(s.getId()) == null) {
            settings.insert(s);
        } else {
            settings.save(s);
        }
    }
}
