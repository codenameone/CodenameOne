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
package com.codenameone.examples.bastion;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.JournalEntry;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.ledger.Ledger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The ledger's three rules, and the audit chain's one.
@BackendTest
class LedgerTest {
    @Autowired
    private Ledger ledger;
    @Autowired
    private Audit audit;
    @Autowired
    private DataSource db;

    private static String key() throws Exception {
        return "test-" + Ids.next();
    }

    private LedgerAccount funded(String owner, long cents) throws Exception {
        LedgerAccount account = ledger.open(owner, "USD", AccountKinds.CHECKING, "Checking");
        if (cents > 0) {
            LedgerAccount cash = ledger.system("USD", AccountKinds.SYS_CASH);
            ledger.post(key(), "DEPOSIT", owner, "seed", Arrays.asList(
                    new Leg(cash.getId(), -cents), new Leg(account.getId(), cents)));
        }
        return account;
    }

    private long balance(LedgerAccount account) throws Exception {
        return ledger.owned(account.getOwner(), account.getId()).getBalance();
    }

    private List<String> books(String... owners) throws Exception {
        List<String> all = new ArrayList<String>(Arrays.asList(owners));
        all.add(AccountKinds.BANK);
        return ledger.verify(all);
    }

    @Test
    void aTransferMovesMoneyAndTheBooksBalance() throws Exception {
        LedgerAccount alice = funded("ledger-a1", 10000);
        LedgerAccount bob = funded("ledger-b1", 0);
        ledger.post(key(), "TRANSFER", "ledger-a1", "rent", Arrays.asList(
                new Leg(alice.getId(), -2500), new Leg(bob.getId(), 2500)));
        assertEquals(7500, balance(alice));
        assertEquals(2500, balance(bob));
        assertEquals(new ArrayList<String>(), books("ledger-a1", "ledger-b1"));
    }

    @Test
    void anUnbalancedEntryIsRefused() throws Exception {
        LedgerAccount alice = funded("ledger-a2", 10000);
        LedgerAccount bob = funded("ledger-b2", 0);
        assertThrows(IllegalStateException.class, () -> ledger.post(key(), "TRANSFER",
                "ledger-a2", "", Arrays.asList(new Leg(alice.getId(), -100),
                        new Leg(bob.getId(), 99))));
        assertEquals(10000, balance(alice));
        assertEquals(0, balance(bob));
    }

    @Test
    void anOverdraftMovesNothingAtAll() throws Exception {
        LedgerAccount alice = funded("ledger-a3", 1000);
        LedgerAccount bob = funded("ledger-b3", 0);
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> ledger.post(key(), "TRANSFER", "ledger-a3", "", Arrays.asList(
                        new Leg(bob.getId(), 1001), new Leg(alice.getId(), -1001))));
        assertEquals(409, refused.getStatus());
        // bob's leg sorts first and was applied before alice's was refused; the
        // rollback has to have taken it back.
        assertEquals(0, balance(bob));
        assertEquals(1000, balance(alice));
        assertEquals(new ArrayList<String>(), books("ledger-a3", "ledger-b3"));
    }

    @Test
    void aFrozenAccountNeitherPaysNorReceives() throws Exception {
        LedgerAccount alice = funded("ledger-a4", 1000);
        LedgerAccount bob = funded("ledger-b4", 0);
        ledger.freeze(bob.getId(), true);
        assertThrows(ResponseStatusException.class, () -> ledger.post(key(), "TRANSFER",
                "ledger-a4", "", Arrays.asList(new Leg(alice.getId(), -10),
                        new Leg(bob.getId(), 10))));
        assertEquals(1000, balance(alice));
    }

    @Test
    void aRetryMovesMoneyOnceAndAReusedKeyIsRefused() throws Exception {
        LedgerAccount alice = funded("ledger-a5", 1000);
        LedgerAccount bob = funded("ledger-b5", 0);
        String key = key();
        List<Leg> legs = Arrays.asList(new Leg(alice.getId(), -300), new Leg(bob.getId(), 300));
        JournalEntry first = ledger.post(key, "TRANSFER", "ledger-a5", "", legs);
        JournalEntry again = ledger.post(key, "TRANSFER", "ledger-a5", "", legs);
        assertEquals(first.getId(), again.getId());
        assertEquals(700, balance(alice));
        ResponseStatusException reused = assertThrows(ResponseStatusException.class,
                () -> ledger.post(key, "TRANSFER", "ledger-a5", "", Arrays.asList(
                        new Leg(alice.getId(), -301), new Leg(bob.getId(), 301))));
        assertEquals(422, reused.getStatus());
        assertEquals(700, balance(alice));
    }

    @Test
    void concurrentSpendsCannotOverdraw() throws Exception {
        final LedgerAccount alice = funded("ledger-a6", 500);
        final LedgerAccount bob = funded("ledger-b6", 0);
        final AtomicInteger moved = new AtomicInteger();
        Runnable[] calls = new Runnable[8];
        for (int iter = 0; iter < calls.length; iter++) {
            calls[iter] = () -> {
                try {
                    ledger.post(key(), "TRANSFER", "ledger-a6", "", Arrays.asList(
                            new Leg(alice.getId(), -100), new Leg(bob.getId(), 100)));
                    moved.incrementAndGet();
                } catch (Exception refused) {
                    // Insufficient funds, or a busy database: either way
                    // nothing moved, which the balances below confirm.
                }
            };
        }
        Race.together(calls);
        assertTrue(moved.get() <= 5, moved.get() + " spends of 100 from 500");
        assertEquals(500 - 100L * moved.get(), balance(alice));
        assertEquals(100L * moved.get(), balance(bob));
        assertEquals(new ArrayList<String>(), books("ledger-a6", "ledger-b6"));
    }

    @Test
    void theAuditChainNoticesAnEditedRow() throws Exception {
        audit.append("audit-1", "TEST", "one", "first");
        audit.append("audit-1", "TEST", "two", "second");
        assertEquals(new ArrayList<String>(), audit.verify());
        db.execute("UPDATE audit_event SET detail = 'rewritten' WHERE actor = ? AND subject = ?",
                new Object[] {"audit-1", "one"});
        List<String> problems = audit.verify();
        assertFalse(problems.isEmpty());
        assertTrue(problems.get(0).contains("was changed"), problems.toString());
        db.execute("UPDATE audit_event SET detail = 'first' WHERE actor = ? AND subject = ?",
                new Object[] {"audit-1", "one"});
        assertEquals(new ArrayList<String>(), audit.verify());
    }
}
