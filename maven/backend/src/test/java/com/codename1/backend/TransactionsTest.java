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
package com.codename1.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The runtime half of {@code @Transactional}: what the woven code calls, driven
 * here the way it drives it -- begin, the body, then commit or afterThrow with
 * the decision the build worked out -- against a real SQLite file, because a
 * transaction is only proven by what a SECOND connection can see.
 */
class TransactionsTest {
    private DataSource pool;

    @BeforeEach
    void open(@TempDir File dir) throws IOException {
        // A file, not :memory:, so the pool can hold a second connection and the
        // tests can look at the table from outside the transaction.
        pool = DataSource.open(new File(dir, "tx.db").getAbsolutePath(), 4, 5000, 10000);
        pool.execute("CREATE TABLE t (v TEXT)", null);
    }

    @AfterEach
    void close() {
        pool.close();
    }

    private int rows() throws IOException {
        Map row = pool.queryOne("SELECT COUNT(*) AS n FROM t", null);
        return ((Number)row.get("n")).intValue();
    }

    private void insert(String v) throws IOException {
        pool.execute("INSERT INTO t (v) VALUES (?)", new Object[] {v});
    }

    @Test
    @DisplayName("REQUIRED commits what its body did")
    void requiredCommits() throws Exception {
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("a");
        insert("b");
        Transactions.commit(tx);
        assertFalse(Transactions.isActive());
        assertEquals(2, rows());
    }

    @Test
    @DisplayName("a rollback undoes every statement the body ran through the pool")
    void rollbackUndoes() throws Exception {
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("a");
        Transactions.afterThrow(tx, true);
        assertEquals(0, rows());
    }

    @Test
    @DisplayName("statements inside the transaction share one connection, the pool's others see nothing yet")
    void oneConnectionIsolated() throws Exception {
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("a");
        Database inside = pool.borrow();
        Database again = pool.borrow();
        assertSame(inside, again, "a thread in a transaction was handed a second connection");
        pool.release(inside);
        pool.release(again);
        Transactions.commit(tx);
        assertEquals(1, rows());
    }

    @Test
    @DisplayName("a joined method that fails marks the outer transaction, which then refuses to commit")
    void joinedFailureIsUnexpectedRollback() throws Exception {
        Transactions.Transaction outer = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("a");
        Transactions.Transaction inner = Transactions.begin(Transactions.REQUIRED, false, -1);
        assertFalse(inner.isNewTransaction());
        insert("b");
        Transactions.afterThrow(inner, true);
        assertTrue(Transactions.isRollbackOnly());
        assertThrows(TransactionException.UnexpectedRollback.class,
                () -> Transactions.commit(outer));
        assertEquals(0, rows());
    }

    @Test
    @DisplayName("REQUIRES_NEW commits on its own connection even when the outer one rolls back")
    void requiresNewIsIndependent() throws Exception {
        Transactions.Transaction outer = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("outer");
        Transactions.Transaction inner = Transactions.begin(Transactions.REQUIRES_NEW, true, -1);
        assertTrue(inner.isNewTransaction());
        // SQLite: the outer transaction holds the write lock, so the inner one
        // could not write -- read instead, which proves it is a different
        // connection that cannot see the outer's uncommitted row.
        Map seen = pool.queryOne("SELECT COUNT(*) AS n FROM t", null);
        assertEquals(0, ((Number)seen.get("n")).intValue(),
                "REQUIRES_NEW saw the suspended transaction's uncommitted row");
        Transactions.commit(inner);
        Transactions.afterThrow(outer, true);
        assertEquals(0, rows());
    }

    @Test
    @DisplayName("NESTED rolls back to its savepoint and leaves the outer transaction able to commit")
    void nestedSavepoint() throws Exception {
        Transactions.Transaction outer = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("kept");
        Transactions.Transaction nested = Transactions.begin(Transactions.NESTED, false, -1);
        insert("undone");
        Transactions.afterThrow(nested, true);
        assertFalse(Transactions.isRollbackOnly());
        Transactions.commit(outer);
        assertEquals(1, rows());
    }

    @Test
    @DisplayName("NESTED before the transaction has touched a database sets its savepoint on the first statement")
    void nestedBeforeAnyStatement() throws Exception {
        // No process-wide default pool: the savepoint waits for the statement
        // that decides which database the transaction is on.
        Transactions.Transaction outer = Transactions.begin(Transactions.REQUIRED, false, -1);
        Transactions.Transaction nested = Transactions.begin(Transactions.NESTED, false, -1);
        insert("undone");
        Transactions.afterThrow(nested, true);
        insert("kept");
        Transactions.commit(outer);
        assertEquals(1, rows(), "the late savepoint did not undo exactly the nested insert");

        outer = Transactions.begin(Transactions.REQUIRED, false, -1);
        nested = Transactions.begin(Transactions.NESTED, false, -1);
        Transactions.commit(nested);                  // never touched the database
        insert("after");
        Transactions.commit(outer);
        assertEquals(2, rows());

        outer = Transactions.begin(Transactions.REQUIRED, false, -1);
        nested = Transactions.begin(Transactions.NESTED, false, -1);
        Transactions.afterThrow(nested, true);        // nothing ran, nothing to undo
        assertFalse(Transactions.isRollbackOnly());
        insert("last");
        Transactions.commit(outer);
        assertEquals(3, rows());
    }

    @Test
    @DisplayName("setRollbackOnly in the method that began the transaction rolls back silently")
    void localRollbackOnlyIsSilent() throws Exception {
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("a");
        Transactions.setRollbackOnly();
        assertTrue(Transactions.isRollbackOnly());
        Transactions.commit(tx);                     // no UnexpectedRollback
        assertFalse(Transactions.isActive());
        assertEquals(0, rows());

        // From a joined method it is the participant's failure, and the method
        // that began the transaction must be told its work was not saved.
        Transactions.Transaction outer = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("b");
        Transactions.Transaction inner = Transactions.begin(Transactions.REQUIRED, false, -1);
        Transactions.setRollbackOnly();
        Transactions.commit(inner);
        assertThrows(TransactionException.UnexpectedRollback.class,
                () -> Transactions.commit(outer));
        assertEquals(0, rows());
    }

    @Test
    @DisplayName("MANDATORY refuses to run alone and NEVER refuses to run inside one")
    void mandatoryAndNever() throws Exception {
        assertThrows(TransactionException.IllegalState.class,
                () -> Transactions.begin(Transactions.MANDATORY, false, -1));
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        assertThrows(TransactionException.IllegalState.class,
                () -> Transactions.begin(Transactions.NEVER, false, -1));
        Transactions.commit(tx);
    }

    @Test
    @DisplayName("NOT_SUPPORTED suspends the transaction and restores it afterwards")
    void notSupportedSuspends() throws Exception {
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("a");
        Transactions.Transaction none = Transactions.begin(Transactions.NOT_SUPPORTED, false, -1);
        assertFalse(Transactions.isActive());
        Transactions.commit(none);
        assertTrue(Transactions.isActive());
        Transactions.commit(tx);
        assertEquals(1, rows());
    }

    @Test
    @DisplayName("a checked exception commits unless the build decided otherwise")
    void checkedExceptionCommits() throws Exception {
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("a");
        Transactions.afterThrow(tx, false);
        assertEquals(1, rows());
    }

    @Test
    @DisplayName("a transaction that never touches the database borrows no connection")
    void lazyBegin() throws Exception {
        int idle = pool.getIdleCount();
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        assertEquals(idle, pool.getIdleCount());
        Transactions.commit(tx);
        assertEquals(idle, pool.getIdleCount());
    }

    @Test
    @DisplayName("the programmatic inTransaction joins the thread's transaction instead of nesting")
    void inTransactionJoins() throws Exception {
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        pool.inTransaction(new DataSource.Work() {
            public Object run(Database db) throws Exception {
                db.execute("INSERT INTO t (v) VALUES (?)", new Object[] {"joined"});
                return null;
            }
        });
        Transactions.afterThrow(tx, true);
        assertEquals(0, rows(), "the joined work committed on its own");
    }

    @Test
    @DisplayName("each thread has its own transaction")
    void perThread() throws Exception {
        Transactions.Transaction tx = Transactions.begin(Transactions.REQUIRED, false, -1);
        insert("a");
        final Database[] other = new Database[1];
        final Database mine = pool.borrow();
        pool.release(mine);
        Thread t = new Thread(() -> {
            try {
                other[0] = pool.borrow();
                pool.release(other[0]);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        });
        t.start();
        t.join();
        assertNotSame(mine, other[0], "another thread was handed this thread's transaction");
        Transactions.commit(tx);
    }
}
