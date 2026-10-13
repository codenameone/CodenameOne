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

import java.io.IOException;
import com.codename1.backend.annotations.Isolation;
import com.codename1.backend.annotations.Propagation;

import com.codename1.backend.orm.EntityManager;

/// The transaction a `@Transactional` method runs in, bound to the calling
/// thread.
///
/// The build rewrites every `@Transactional` method into a call to
/// [#begin], the original body, then [#commit] -- or
/// [#afterThrow] with the rollback decision it worked out from the
/// annotation. Propagation, isolation, read-only and timeout arrive as constants.
/// This class binds the connection, completion hooks and scoped resources to
/// the physical transaction on the calling thread.
///
/// ## Joining
///
/// Nothing has to be handed the transaction. [DataSource#borrow] asks this
/// class first, and a thread inside a transaction gets the transaction's
/// connection back instead of a pooled one -- so the pool's own methods, an
/// [EntityManager]'s daos and a managed session all join it. The connection
/// is borrowed, and BEGIN sent, only when the transaction first needs it: a
/// `@Transactional` method that returns before touching the database costs
/// only an in-memory scope, with no database round trip.
///
/// A thread that has never begun a transaction does not even read the
/// thread-local: [#used] is set by the first [#begin] of the process,
/// on the thread that is then inside it, so every other thread reading a stale
/// `false` is correct -- none of them is in a transaction.
///
/// ## Propagation
///
/// Spring's seven, one for one; the constants below are the ordinals of
/// `com.codename1.backend.annotations.Propagation`. A method that joins a
/// transaction and fails with a rollback-worthy exception marks the whole
/// transaction rollback-only, and the method that began it then rolls back and
/// throws [TransactionException.UnexpectedRollback] rather than reporting
/// a commit that did not happen.
public final class Transactions {
    static final int REQUIRED = com.codename1.impl.backend.BackendAccess.REQUIRED;
    static final int SUPPORTS = com.codename1.impl.backend.BackendAccess.SUPPORTS;
    static final int MANDATORY = com.codename1.impl.backend.BackendAccess.MANDATORY;
    static final int REQUIRES_NEW = com.codename1.impl.backend.BackendAccess.REQUIRES_NEW;
    static final int NOT_SUPPORTED = com.codename1.impl.backend.BackendAccess.NOT_SUPPORTED;
    static final int NEVER = com.codename1.impl.backend.BackendAccess.NEVER;
    static final int NESTED = com.codename1.impl.backend.BackendAccess.NESTED;

    /// The physical transaction the thread is in, or null.
    private static final ThreadLocal CURRENT = new ThreadLocal();

    /// Whether any thread has ever begun one. Deliberately not volatile; see the
    /// class comment for why a stale read is a correct one.
    static boolean used;

    private static final int KIND_NONE = 0;
    private static final int KIND_JOINED = 1;
    private static final int KIND_NEW = 2;
    private static final int KIND_SAVEPOINT = 3;

    private Transactions() {
    }

    /// Final outcome. UNKNOWN means a commit or rollback failed and durability
    /// cannot be established; it must never be treated as a successful commit.
    public enum Outcome { COMMITTED, ROLLED_BACK, UNKNOWN }

    /// Hooks run synchronously in registration order, once per physical
    /// transaction (including a scope that never borrowed a connection).
    /// Override only the hooks needed. Before-commit failures prevent commit;
    /// after-commit failures are reported after all completion hooks run.
    /// Completion failures are logged and do not replace the original failure.
    public abstract static class Synchronization {
        public void beforeCommit(boolean readOnly) { }
        public void afterCommit() { }
        public void afterCompletion(Outcome outcome) { }
    }

    /// A programmatic transaction body. Any exception or Error rolls it back.
    public interface Work {
        Object run() throws Exception;
    }

    /// Runs a REQUIRED transaction with the engine's default isolation.
    public static Object execute(Work work) throws Exception {
        return execute(Propagation.REQUIRED, false, -1, Isolation.DEFAULT, work);
    }

    /// Runs a transaction with the same propagation and isolation rules as
    /// `@Transactional`. Unlike annotation rollback rules, every body failure rolls back.
    public static Object execute(Propagation propagation, boolean readOnly, int timeoutSeconds,
                                 Isolation isolation, Work work) throws Exception {
        if (propagation == null || work == null) {
            throw new IllegalArgumentException("Propagation and work are required");
        }
        Transaction tx = begin(propagation.ordinal(), readOnly, timeoutSeconds, isolation);
        Object result;
        try {
            result = work.run();
        } catch (Exception err) {
            afterThrow(tx, true);
            throw err;
        } catch (Error err) {
            afterThrow(tx, true);
            throw err;
        }
        commit(tx);
        return result;
    }

    /// Registers hooks on the current physical transaction. Registration is
    /// closed when completion begins. A rolled-back NESTED scope suppresses its
    /// before/after-commit hooks, but its completion hook still runs at physical end.
    public static void registerSynchronization(Synchronization synchronization) {
        Physical p = required();
        if (synchronization == null || p.completing) {
            throw new TransactionException.IllegalState("Cannot register synchronization during completion or null");
        }
        p.synchronizations.add(new Registration(synchronization));
    }

    /// Binds a non-null value to a non-null key; duplicate bindings are refused.
    /// Bindings belong to the physical transaction, not to a savepoint. They are
    /// cleared after all completion hooks; dispose values in afterCompletion.
    public static void bindResource(Object key, Object value) {
        Physical p = required();
        if (key == null || value == null || p.completed || p.resources.containsKey(key)) {
            throw new TransactionException.IllegalState("Invalid or already bound transaction resource");
        }
        p.resources.put(key, value);
    }

    /// Returns the current binding, or null outside a transaction or for a missing key.
    /// Bindings remain visible to completion callbacks until all have finished.
    public static Object getResource(Object key) {
        Physical p = used ? (Physical) CURRENT.get() : null;
        return p == null ? null : p.resources.get(key);
    }

    /// Removes a binding, returning its previous value (or null).
    public static Object unbindResource(Object key) {
        return required().resources.remove(key);
    }

    private static Physical required() {
        Physical p = used ? (Physical) CURRENT.get() : null;
        if (p == null) {
            throw new TransactionException.IllegalState("No transaction is active");
        }
        return p;
    }

    private static final class Registration {
        final Synchronization synchronization;
        boolean commitEligible = true;

        Registration(Synchronization synchronization) {
            this.synchronization = synchronization;
        }
    }

    /// One database transaction, which any number of joined methods share.
    static final class Physical {
        DataSource pool;
        Database db;
        final boolean readOnly;
        final Isolation isolation;
        final java.util.List synchronizations = new java.util.ArrayList();
        final java.util.Map resources = new java.util.HashMap();
        boolean completing;
        boolean completed;
        final long deadline;
        /// Set by a participant that failed: the outer commit must refuse loudly.
        boolean rollbackOnly;
        /// Set by setRollbackOnly() in the method that began the transaction: it
        /// chose to undo its own work, so the rollback is silent, as in Spring.
        boolean localRollback;
        /// The joined and nested calls running inside the one that began it,
        /// innermost last -- which is how setRollbackOnly() knows whose work it
        /// undoes.
        final java.util.ArrayList calls = new java.util.ArrayList();
        int savepoints;
        /// Savepoints a NESTED method took before the transaction had touched a
        /// database, in order, to be set right after its BEGIN. There is no pool
        /// to ask for a connection yet -- which one the transaction runs on is
        /// decided by the first statement -- and a savepoint at the very start of
        /// a transaction marks the same state BEGIN does, so setting it late
        /// changes nothing.
        java.util.List pendingSavepoints;
        com.codename1.orm.session.Session session;

        Physical(DataSource pool, boolean readOnly, int timeoutSeconds, Isolation isolation) {
            this.pool = pool;
            this.readOnly = readOnly;
            this.isolation = isolation;
            this.deadline = timeoutSeconds > 0
                    ? System.currentTimeMillis() + timeoutSeconds * 1000L : 0;
        }
    }

    /// What one call to [#begin] did, which the matching [#commit] or
    /// [#afterThrow] undoes. Opaque to the woven code, which only passes it
    /// back.
    static final class Transaction {
        final int kind;
        final Physical physical;
        /// The transaction to restore when this one ends: REQUIRES_NEW, NOT_SUPPORTED.
        final Physical suspended;
        final boolean suspends;
        final String savepoint;
        boolean completed;
        final int synchronizationStart;
        /// A NESTED call asked, by setRollbackOnly(), to undo its own work.
        boolean rollbackRequested;

        Transaction(int kind, Physical physical, Physical suspended, boolean suspends,
                    String savepoint) {
            this.kind = kind;
            this.physical = physical;
            this.suspended = suspended;
            this.suspends = suspends;
            this.savepoint = savepoint;
            this.synchronizationStart = physical == null ? 0 : physical.synchronizations.size();
        }

        /// Whether this call began the physical transaction it runs in.
        boolean isNewTransaction() {
            return kind == KIND_NEW;
        }
    }

    /// Whether the calling thread is inside a transaction.
    public static boolean isActive() {
        Physical p = used ? (Physical) CURRENT.get() : null;
        return p != null && !p.completed;
    }

    /// Whether the calling thread's transaction has been marked rollback-only.
    public static boolean isRollbackOnly() {
        Physical p = used ? (Physical) CURRENT.get() : null;
        if (p == null || p.completed) {
            return false;
        }
        Transaction top = innermost(p);
        return p.rollbackOnly || p.localRollback || (top != null && top.rollbackRequested);
    }

    private static Transaction innermost(Physical p) {
        return p.calls.isEmpty() ? null : (Transaction) p.calls.get(p.calls.size() - 1);
    }

    /// Marks the calling thread's transaction so it rolls back however its method
    /// ends -- the programmatic form of throwing, for a method that wants to
    /// return a value and still undo its work.
    public static void setRollbackOnly() {
        Physical p = used ? (Physical) CURRENT.get() : null;
        if (p == null || p.completed) {
            throw new TransactionException.IllegalState("setRollbackOnly() outside a "
                    + "transaction: there is nothing to roll back");
        }
        Transaction top = innermost(p);
        if (top == null) {
            // The method that began the transaction: it returns normally and
            // its work is undone, with nothing thrown.
            p.localRollback = true;
        } else if (top.kind == KIND_SAVEPOINT) {
            // A NESTED method: its own work goes, back to its savepoint, and
            // the transaction around it carries on -- which is what a savepoint
            // is for.
            top.rollbackRequested = true;
        } else {
            // A joined method: whoever began the transaction must learn that
            // what it thinks it committed was not saved.
            p.rollbackOnly = true;
        }
    }

    /// Starts what a `@Transactional` method asked for. Called by woven code.
    ///
    /// #### Parameters
    ///
    /// - `propagation`: one of the constants above
    ///
    /// - `readOnly`: the annotation's readOnly
    ///
    /// - `timeoutSeconds`: the annotation's timeout, zero or less for none
    static Transaction begin(int propagation, boolean readOnly, int timeoutSeconds) {
        return begin(propagation, readOnly, timeoutSeconds, Isolation.DEFAULT);
    }

    static Transaction begin(int propagation, boolean readOnly, int timeoutSeconds, Isolation isolation) {
        if (isolation == null) {
            throw new IllegalArgumentException("Isolation is required");
        }
        used = true;
        Transaction tx = open(propagation, readOnly, timeoutSeconds, isolation);
        if (tx.physical != null && (tx.kind == KIND_JOINED || tx.kind == KIND_SAVEPOINT)) {
            tx.physical.calls.add(tx);
        }
        return tx;
    }

    private static Transaction open(int propagation, boolean readOnly, int timeoutSeconds, Isolation isolation) {
        Physical current = (Physical) CURRENT.get();
        if (current != null && propagation != REQUIRES_NEW && propagation != NOT_SUPPORTED
                && propagation != NEVER) {
            if (current.completed) {
                throw new TransactionException.IllegalState("Transaction has completed; use REQUIRES_NEW for callback database work");
            }
            if (isolation != Isolation.DEFAULT && isolation != current.isolation) {
                throw new TransactionException.IllegalState("Isolation " + isolation
                        + " cannot join transaction with isolation " + current.isolation);
            }
        }
        if (isolation != Isolation.DEFAULT && (propagation == NOT_SUPPORTED || propagation == NEVER
                || (propagation == SUPPORTS && current == null))) {
            throw new TransactionException.IllegalState("Isolation requires a physical transaction");
        }
        switch (propagation) {
            case SUPPORTS:
                return current == null ? new Transaction(KIND_NONE, null, null, false, null)
                        : new Transaction(KIND_JOINED, current, null, false, null);
            case MANDATORY:
                if (current == null) {
                    throw new TransactionException.IllegalState("A MANDATORY transactional "
                            + "method was called with no transaction open. Call it from a "
                            + "@Transactional method, or change its propagation.");
                }
                return new Transaction(KIND_JOINED, current, null, false, null);
            case NEVER:
                if (current != null) {
                    throw new TransactionException.IllegalState("A NEVER transactional "
                            + "method was called inside a transaction.");
                }
                return new Transaction(KIND_NONE, null, null, false, null);
            case NOT_SUPPORTED:
                CURRENT.set(null);
                return new Transaction(KIND_NONE, null, current, true, null);
            case REQUIRES_NEW:
                return fresh(readOnly, timeoutSeconds, isolation, current, true);
            case NESTED:
                if (current == null) {
                    return fresh(readOnly, timeoutSeconds, isolation, null, false);
                }
                return nested(current);
            default:
                if (current != null) {
                    return new Transaction(KIND_JOINED, current, null, false, null);
                }
                return fresh(readOnly, timeoutSeconds, isolation, null, false);
        }
    }

    private static Transaction fresh(boolean readOnly, int timeoutSeconds, Isolation isolation, Physical suspended,
                                     boolean suspends) {
        Physical p = new Physical(null, readOnly, timeoutSeconds, isolation);
        CURRENT.set(p);
        return new Transaction(KIND_NEW, p, suspended, suspends, null);
    }

    private static Transaction nested(Physical current) {
        String name = "cn1_sp_" + (++current.savepoints);
        Database db = current.db;
        if (db == null) {
            // Nothing has run in the transaction yet, so there is no connection
            // and -- deliberately -- no process-wide default pool to borrow one
            // from: two servers in one process would hand this savepoint to
            // whichever started last. It is set when the first statement picks
            // the database.
            if (current.pendingSavepoints == null) {
                current.pendingSavepoints = new java.util.ArrayList();
            }
            current.pendingSavepoints.add(name);
            return new Transaction(KIND_SAVEPOINT, current, null, false, name);
        }
        try {
            flushSession(current);
            db.savepoint(name);
        } catch (IOException err) {
            // The outer transaction cannot commit after this, even if its method
            // catches the exception: the failed flush may have left some of its
            // statements applied (MySQL) or the transaction aborted (PostgreSQL),
            // and a commit would persist half of it or report success for work
            // the server threw away.
            current.rollbackOnly = true;
            throw new TransactionException("Could not set savepoint " + name + ": "
                    + err.getMessage(), err);
        }
        return new Transaction(KIND_SAVEPOINT, current, null, false, name);
    }

    /// Ends a call that returned normally. Commits when the call began the
    /// transaction, and does nothing more than restore state when it joined one.
    static void commit(Transaction tx) {
        if (tx == null || tx.completed) {
            return;
        }
        tx.completed = true;
        leave(tx);
        switch (tx.kind) {
            case KIND_SAVEPOINT:
                if (tx.rollbackRequested) {
                    discardNestedCommitHooks(tx);
                }
                if (dropPending(tx)) {
                    // Never set: the nested method did not touch the database.
                    return;
                }
                if (tx.rollbackRequested) {
                    rollBackToSavepoint(tx);
                    return;
                }
                try {
                    flushSession(tx.physical);
                    tx.physical.db.releaseSavepoint(tx.savepoint);
                } catch (IOException err) {
                    // The outer transaction is not over and may still commit, so
                    // it has to know this part could not be kept.
                    tx.physical.rollbackOnly = true;
                    throw new TransactionException("Could not release savepoint "
                            + tx.savepoint + ": " + err.getMessage(), err);
                }
                return;
            case KIND_NEW:
                try {
                    finishNew(tx.physical);
                } finally {
                    restore(tx);
                }
                return;
            default:
                restore(tx);
        }
    }

    /// Ends a call that threw. Rolls back -- the call's own transaction, its
    /// savepoint, or, when it joined one, by marking the shared transaction
    /// rollback-only -- when `rollback` is true, and otherwise treats the
    /// exception as a normal end, which is Spring's rule for a checked one.
    ///
    /// Never throws: the woven code rethrows the method's own exception after
    /// this returns, and a failure here would replace the cause with a symptom.
    /// A rollback that fails is reported on standard error and its connection
    /// closed, so the pool cannot hand out a session that is still inside a
    /// transaction.
    static void afterThrow(Transaction tx, boolean rollback) {
        if (tx == null || tx.completed) {
            return;
        }
        if (!rollback) {
            try {
                commit(tx);
            } catch (Throwable err) {
                System.err.println("A transaction ended by a checked exception could not "
                        + "commit: " + err);
            }
            return;
        }
        tx.completed = true;
        leave(tx);
        switch (tx.kind) {
            case KIND_JOINED:
                tx.physical.rollbackOnly = true;
                return;
            case KIND_SAVEPOINT:
                discardNestedCommitHooks(tx);
                if (dropPending(tx)) {
                    // Never set, so nothing ran after it: nothing to undo.
                    return;
                }
                rollBackToSavepoint(tx);
                return;
            case KIND_NEW:
                try {
                    rollbackPhysical(tx.physical);
                } finally {
                    restore(tx);
                }
                return;
            default:
                restore(tx);
        }
    }

    private static void discardNestedCommitHooks(Transaction tx) {
        for (int i = tx.synchronizationStart; i < tx.physical.synchronizations.size(); i++) {
            ((Registration) tx.physical.synchronizations.get(i)).commitEligible = false;
        }
    }

    /// Undoes a NESTED call's work, back to its savepoint; never throws.
    private static void rollBackToSavepoint(Transaction tx) {
        try {
            tx.physical.db.rollbackToSavepoint(tx.savepoint);
            tx.physical.db.releaseSavepoint(tx.savepoint);
            if (tx.physical.session != null) {
                // The rows it wrote since the savepoint are gone, so the
                // instances it still manages would be lying about them.
                tx.physical.session.clear();
            }
        } catch (Exception err) {
            tx.physical.rollbackOnly = true;
            System.err.println("Could not roll back to savepoint " + tx.savepoint
                    + "; the transaction will roll back instead: " + err);
        }
    }

    /// A joined or nested call has ended.
    private static void leave(Transaction tx) {
        if (tx.physical != null && (tx.kind == KIND_JOINED || tx.kind == KIND_SAVEPOINT)) {
            tx.physical.calls.remove(tx);
        }
    }

    /// Forgets a savepoint that was never set; true when it was one.
    private static boolean dropPending(Transaction tx) {
        java.util.List pending = tx.physical.pendingSavepoints;
        return pending != null && pending.remove(tx.savepoint);
    }

    /// Puts back what this call suspended, or clears what it began.
    private static void restore(Transaction tx) {
        if (tx.kind == KIND_NEW || tx.suspends) {
            CURRENT.set(tx.suspended);
        }
    }

    private static void finishNew(Physical p) {
        p.completing = true;
        checkCommitAllowed(p);
        if (p.completed) {
            return;
        }
        boolean commitAttempted = false;
        try {
            for (int i = 0; i < p.synchronizations.size(); i++) {
                Registration r = (Registration) p.synchronizations.get(i);
                if (r.commitEligible) {
                    r.synchronization.beforeCommit(p.readOnly);
                }
            }
            checkCommitAllowed(p);
            if (p.completed) {
                return;
            }
            if (p.session != null) {
                p.session.commitTransaction();
            }
            if (p.db != null) {
                commitAttempted = true;
                p.db.commitTransaction();
            }
        } catch (Error err) {
            rollbackPhysical(p, commitAttempted);
            throw err;
        } catch (TransactionException err) {
            rollbackPhysical(p, commitAttempted);
            throw err;
        } catch (Throwable err) {
            rollbackPhysical(p, commitAttempted);
            throw new TransactionException("Transaction could not commit: " + err.getMessage(), err);
        }
        complete(p, Outcome.COMMITTED);
    }

    private static void checkCommitAllowed(Physical p) {
        if (p.rollbackOnly) {
            rollbackPhysical(p);
            throw new TransactionException.UnexpectedRollback("A joined call marked the transaction rollback-only");
        }
        if (p.localRollback) {
            rollbackPhysical(p);
            return;
        }
        if (p.deadline != 0 && System.currentTimeMillis() > p.deadline) {
            rollbackPhysical(p);
            throw new TransactionException.TimedOut("The transaction ran past its timeout and was rolled back");
        }
    }

    private static void rollbackPhysical(Physical p) {
        rollbackPhysical(p, false);
    }

    private static void rollbackPhysical(Physical p, boolean uncertainCommit) {
        if (p.completed) {
            return;
        }
        p.completing = true;
        boolean broken = uncertainCommit;
        if (p.session != null) {
            try {
                if (p.session.isTransactionActive()) {
                    p.session.rollbackTransaction();
                }
            } catch (Throwable err) {
                System.err.println("Could not roll back the transaction's session: " + err);
            }
        }
        if (p.db != null) {
            try {
                if (p.db.isInTransaction()) {
                    p.db.rollbackTransaction();
                }
            } catch (Throwable err) {
                broken = true;
                System.err.println("Rollback failed; closing the connection: " + err);
            }
            if (broken) {
                p.db.close();
            }
        }
        complete(p, broken ? Outcome.UNKNOWN : Outcome.ROLLED_BACK);
    }

    private static void complete(Physical p, Outcome outcome) {
        p.completed = true;
        Throwable failure = null;
        try {
            closeSession(p);
            release(p);
            if (outcome == Outcome.COMMITTED) {
                for (int i = 0; i < p.synchronizations.size(); i++) {
                    Registration r = (Registration) p.synchronizations.get(i);
                    if (r.commitEligible) {
                        try {
                            r.synchronization.afterCommit();
                        } catch (Throwable err) {
                            if (failure == null) {
                                failure = err;
                            }
                        }
                    }
                }
            }
        } finally {
            try {
                for (int i = 0; i < p.synchronizations.size(); i++) {
                    try {
                        ((Registration) p.synchronizations.get(i)).synchronization.afterCompletion(outcome);
                    } catch (Throwable err) {
                        System.err.println("Transaction completion callback failed: " + err);
                    }
                }
            } finally {
                p.resources.clear();
                p.synchronizations.clear();
            }
        }
        if (failure != null) {
            throw new TransactionException("Transaction committed, but an after-commit callback failed", failure);
        }
    }

    private static void closeSession(Physical p) {
        if (p.session == null) {
            return;
        }
        com.codename1.orm.session.Session session = p.session;
        p.session = null;
        try {
            session.close();
        } catch (Throwable err) {
            System.err.println("Could not close the transaction's session: " + err);
        }
    }

    private static void release(Physical p) {
        Database db = p.db;
        DataSource pool = p.pool;
        p.db = null;
        if (db == null || pool == null) {
            return;
        }
        pool.releaseToPool(db);
    }

    private static void flushSession(Physical p) throws IOException {
        if (p.session != null && p.session.isTransactionActive()) {
            try {
                p.session.flush();
            } catch (RuntimeException err) {
                throw new IOException("Flushing the session failed: " + err.getMessage(), err);
            }
        }
    }

    /// The transaction's connection for `pool`, borrowing it and sending
    /// BEGIN the first time; null when the thread is not in a transaction or its
    /// transaction is on another database. Called by [DataSource#borrow].
    static Database joined(DataSource pool) throws IOException {
        if (!used) {
            return null;
        }
        Physical p = (Physical) CURRENT.get();
        if (p == null) {
            return null;
        }
        return materialize(p, pool);
    }

    private static Database materialize(Physical p, DataSource pool) throws IOException {
        if (p.completed) {
            throw new TransactionException.IllegalState("Transaction has completed; use REQUIRES_NEW for callback database work");
        }
        if (p.pool == null) {
            if (pool == null) {
                return null;
            }
            p.pool = pool;
        } else if (pool != null && p.pool != pool) { //NOPMD CompareObjectsWithEquals - pools and connections are compared by identity
            // Refused rather than answered with "no transaction": the caller would
            // then take an ordinary connection that commits each statement on its
            // own, and a method that failed afterwards would roll back only the
            // first database -- half of its work kept.
            throw new TransactionException.IllegalState("This thread's transaction is on "
                    + "another database, and one transaction cannot span two. Do the work "
                    + "on this one in a @Transactional(propagation = REQUIRES_NEW) method for "
                    + "a transaction of its own, or NOT_SUPPORTED to run it outside any.");
        }
        if (p.db != null) {
            return p.db;
        }
        if (p.deadline != 0 && System.currentTimeMillis() > p.deadline) {
            throw new IOException("The transaction ran past its timeout before it used the "
                    + "database");
        }
        Database db = null;
        try {
            db = p.pool.borrowFromPool();
            if ("sqlite".equals(db.dialect().getName())) {
                // The ORM relies on it and sets it outside a transaction, which is
                // the only place SQLite honours it: inside one it is ignored.
                db.execute("PRAGMA foreign_keys = ON", null);
            }
            db.beginTransaction(p.readOnly, p.isolation);
            if (p.pendingSavepoints != null) {
                for (int iter = 0 ; iter < p.pendingSavepoints.size() ; iter++) {
                    db.savepoint((String) p.pendingSavepoints.get(iter));
                }
                p.pendingSavepoints = null;
            }
        } catch (IOException err) {
            p.rollbackOnly = true;
            if (db != null) {
                try {
                    if (db.isInTransaction()) {
                        db.rollbackTransaction();
                    }
                } catch (IOException ignored) {
                    db.close();
                }
                p.pool.releaseToPool(db);
            }
            throw err;
        }
        p.db = db;
        return db;
    }

    /// Whether `db` is the connection of the calling thread's transaction on `pool`.
    static boolean isJoined(DataSource pool, Database db) {
        if (!used) {
            return false;
        }
        Physical p = (Physical) CURRENT.get();
        return p != null && p.db == db && p.pool == pool && db != null; //NOPMD CompareObjectsWithEquals - pools and connections are compared by identity
    }

    /// Whether the calling thread's transaction is on `pool`, or could be.
    static boolean isActiveOn(DataSource pool) {
        if (!used) {
            return false;
        }
        Physical p = (Physical) CURRENT.get();
        return p != null && (p.pool == null || p.pool == pool); //NOPMD CompareObjectsWithEquals - pools and connections are compared by identity
    }

    /// The managed session of the calling thread's transaction, opened on the
    /// transaction's connection the first time it is asked for and flushed and
    /// closed when the transaction ends.
    ///
    /// This is what an injected `Session` delegates to. Outside a
    /// transaction there is no session to give, and the refusal says where one
    /// comes from.
    static com.codename1.orm.session.Session session(EntityManager entities) {
        Physical p = used ? (Physical) CURRENT.get() : null;
        if (p == null) {
            throw new TransactionException.IllegalState("The injected Session belongs to a "
                    + "transaction, and none is open. Annotate the calling method "
                    + "@Transactional -- @Transactional(readOnly = true) for one that only "
                    + "reads -- or open a session of your own with "
                    + "EntityManager.openSession().");
        }
        if (p.session != null) {
            return p.session;
        }
        if (entities.dataSource() == null) {
            throw new TransactionException.IllegalState("A transaction's session needs an "
                    + "entity manager over a pool; this one is pinned to one connection.");
        }
        try {
            if (materialize(p, entities.dataSource()) == null) {
                throw new TransactionException.IllegalState("The transaction is on another "
                        + "database than this entity manager's.");
            }
        } catch (IOException err) {
            throw new TransactionException("Could not begin the transaction: "
                    + err.getMessage(), err);
        }
        com.codename1.orm.session.Session session = entities.openSession();
        // Joins: the adapter under it sees the thread's transaction on its pool
        // and pins that connection instead of sending a BEGIN of its own.
        session.beginTransaction();
        p.session = session;
        return session;
    }

    /// Called by the session adapter when a session that joined this thread's
    /// transaction rolls back: the rows are not the session's to keep, so the
    /// transaction they are in cannot commit either.
    /// Marks the calling thread's transaction, whatever pool it is on, as
    /// unable to commit; nothing when there is none.
    static void markRollbackOnly() {
        Physical p = used ? (Physical) CURRENT.get() : null;
        if (p != null) {
            p.rollbackOnly = true;
        }
    }

    static void markRollbackOnly(DataSource pool) {
        Physical p = used ? (Physical) CURRENT.get() : null;
        if (p != null && (p.pool == null || p.pool == pool)) { //NOPMD CompareObjectsWithEquals - pools and connections are compared by identity
            p.rollbackOnly = true;
        }
    }
}
