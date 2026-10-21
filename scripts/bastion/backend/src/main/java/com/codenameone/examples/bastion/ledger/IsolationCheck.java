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
package com.codenameone.examples.bastion.ledger;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import java.io.IOException;
import java.util.Map;

/// Refuses to start on a MySQL server that runs at REPEATABLE READ, its
/// default.
///
/// The ledger, the audit chain and the order book each serialize their
/// writers on one row -- a balance, `audit_head`, a market -- and then read
/// what the writer before them committed. At REPEATABLE READ, InnoDB answers
/// those reads from the snapshot the transaction took at its first read,
/// which can predate the lock: two appends then both chain onto the same
/// audit row, and a cancel releases funds a concurrent fill already moved.
/// READ COMMITTED reads what was committed when each statement runs, as
/// PostgreSQL does by default; SQLite serializes every writer and needs
/// nothing.
///
/// The level is the server's (or the account's) setting, not the
/// application's, so it is checked rather than set:
/// `SET GLOBAL transaction_isolation = 'READ-COMMITTED'`, or
/// `transaction-isolation = READ-COMMITTED` in the server's configuration.
@Component
public class IsolationCheck {
    private final DataSource db;

    public IsolationCheck(DataSource db) {
        this.db = db;
    }

    @PostConstruct
    public void check() throws IOException {
        if (!"mysql".equals(db.dialect().getName())) {
            return;
        }
        Map row = db.queryOne("SELECT @@transaction_isolation AS level", null);
        Object level = row == null ? null : row.get("level");
        if (!"READ-COMMITTED".equals(String.valueOf(level))) {
            throw new IllegalStateException("Bastion needs MySQL at READ COMMITTED, and this "
                    + "server runs at " + level + ": SET GLOBAL transaction_isolation = "
                    + "'READ-COMMITTED' (see IsolationCheck)");
        }
    }
}
