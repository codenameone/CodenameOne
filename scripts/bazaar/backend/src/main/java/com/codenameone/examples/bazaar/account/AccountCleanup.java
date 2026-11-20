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
package com.codenameone.examples.bazaar.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.repository.LiveTicketRepository;
import com.codenameone.examples.bazaar.repository.PayoutAccountRepository;
import com.codenameone.examples.bazaar.repository.PhoneCodeRepository;
import com.codenameone.examples.bazaar.repository.PreferenceRepository;
import com.codenameone.examples.bazaar.repository.PushDeviceRepository;

/// What closing or restricting an account does to the rows other parts of the
/// server keep under its name.
///
/// These are questions and statements over whole sets of rows -- everything
/// of one account in a table -- and not the work of the services that own
/// those tables, which is why they are gathered here. Each deletion is a bulk
/// statement, so whatever the session had loaded before it is stale
/// afterwards and must be read again before it is used. It has no
/// transaction of its own: [Accounts] calls it inside one.
@Component
public class AccountCleanup {
    private final PreferenceRepository preferences;
    private final PayoutAccountRepository payoutAccounts;
    private final PhoneCodeRepository codes;
    private final LiveTicketRepository tickets;
    private final PushDeviceRepository pushDevices;

    public AccountCleanup(PreferenceRepository preferences, PayoutAccountRepository payoutAccounts,
            PhoneCodeRepository codes, LiveTicketRepository tickets, PushDeviceRepository pushDevices) {
        this.preferences = preferences;
        this.payoutAccounts = payoutAccounts;
        this.codes = codes;
        this.tickets = tickets;
        this.pushDevices = pushDevices;
    }

    /// Deletes what an account leaves in the tables that are keyed by it and
    /// belong to nobody's `forget`: its preferences, where it is paid, a code
    /// it was texted, the tickets it held for the live connection, and the
    /// phones it was sent notifications on.
    public void removeEverythingOf(String username) {
        preferences.deleteByUsername(username);
        payoutAccounts.deleteByUsername(username);
        codes.deleteByUsername(username);
        tickets.deleteByUsername(username);
        pushDevices.deleteByUsername(username);
    }
}
