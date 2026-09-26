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

import com.codename1.backend.orm.EntityManager;

/**
 * The transaction a {@code @Transactional} method runs in, bound to the calling
 * thread.
 *
 * <p>The build rewrites every {@code @Transactional} method into a call to
 * {@link #begin}, the original body, then {@link #commit} -- or
 * {@link #afterThrow} with the rollback decision it worked out from the
 * annotation. The propagation, the read-only flag and the timeout arrive as
 * constants, so this class interprets nothing: it holds the one piece of state
 * that has to exist at run time, which is which connection the thread's
 * transaction is on.
 *
 * <h2>Joining</h2>
 *
 * <p>Nothing has to be handed the transaction. {@link DataSource#borrow} asks this
 * class first, and a thread inside a transaction gets the transaction's
 * connection back instead of a pooled one -- so the pool's own methods, an
 * {@link EntityManager}'s daos and a managed session all join it. The connection
 * is borrowed, and BEGIN sent, only when the transaction first needs it: a
 * {@code @Transactional} method that returns before touching the database costs
 * a thread-local write and nothing else.
 *
 * <p>A thread that has never begun a transaction does not even read the
 * thread-local: {@link #used} is set by the first {@link #begin} of the process,
 * on the thread that is then inside it, so every other thread reading a stale
 * {@code false} is correct -- none of them is in a transaction.
 *
 * <h2>Propagation</h2>
 *
 * <p>Spring's seven, one for one; the constants below are the ordinals of
 * {@code com.codename1.backend.annotations.Propagation}. A method that joins a
 * transaction and fails with a rollback-worthy exception marks the whole
 * transaction rollback-only, and the method that began it then rolls back and
 * throws {@link TransactionException.UnexpectedRollback} rather than reporting
 * a commit that did not happen.
 */
public final class Transactions {
    public static final int REQUIRED = 0;
    public static final int SUPPORTS = 1;
    public static final int MANDATORY = 2;
    public static final int REQUIRES_NEW = 3;
    public static final int NOT_SUPPORTED = 4;
    public static final int NEVER = 5;
    public static final int NESTED = 6;

    /** The physical transaction the thread is in, or null. */
    private static final ThreadLocal CURRENT = new ThreadLocal();

    /**
     * Whether any thread has ever begun one. Deliberately not volatile; see the
     * class comment for why a stale read is a correct one.
     */
    static boolean used;

    private static final int KIND_NONE = 0;
    private static final int KIND_JOINED = 1;
    private static final int KIND_NEW = 2;
    private static final int KIND_SAVEPOINT = 3;

    private Transactions() {
    }

    /** One database transaction, which any number of joined methods share. */
    static final class Physical {
        DataSource pool;
        Database db;
        final boolean readOnly;
        final long deadline;
        /** Set by a participant that failed: the outer commit must refuse loudly. */
        boolean rollbackOnly;
        /**
         * Set by setRollbackOnly() in the method that began the transaction: it
         * chose to undo its own work, so the rollback is silent, as in Spring.
         */
        boolean localRollback;
        /** How many joined or nested calls are running inside the one that began it. */
        int participants;
        int savepoints;
        /**
         * Savepoints a NESTED method took before the transaction had touched a
         * database, in order, to be set right after its BEGIN. There is no pool
         * to ask for a connection yet -- which one the transaction runs on is
         * decided by the first statement -- and a savepoint at the very start of
         * a transaction marks the same state BEGIN does, so setting it late
         * changes nothing.
         */
        java.util.List pendingSavepoints;
        com.codename1.orm.session.Session session;

        Physical(DataSource pool, boolean readOnly, int timeoutSeconds) {
            this.pool = pool;
            this.readOnly = readOnly;
            this.deadline = timeoutSeconds > 0
                    ? System.currentTimeMillis() + timeoutSeconds * 1000L : 0;
        }
    }

    /**
     * What one call to {@link #begin} did, which the matching {@link #commit} or
     * {@link #afterThrow} undoes. Opaque to the woven code, which only passes it
     * back.
     */
    public static final class Transaction {
        final int kind;
        final Physical physical;
        /** The transaction to restore when this one ends: REQUIRES_NEW, NOT_SUPPORTED. */
        final Physical suspended;
        final boolean suspends;
        final String savepoint;
        boolean completed;

        Transaction(int kind, Physical physical, Physical suspended, boolean suspends,
                    String savepoint) {
            this.kind = kind;
            this.physical = physical;
            this.suspended = suspended;
            this.suspends = suspends;
            this.savepoint = savepoint;
        }

        /** Whether this call began the physical transaction it runs in. */
        public boolean isNewTransaction() {
            return kind == KIND_NEW;
        }
    }

    /** Whether the calling thread is inside a transaction. */
    public static boolean isActive() {
        return used && CURRENT.get() != null;
    }

    /** Whether the calling thread's transaction has been marked rollback-only. */
    public static boolean isRollbackOnly() {
        Physical p = used ? (Physical)CURRENT.get() : null;
        return p != null && (p.rollbackOnly || p.localRollback);
    }

    /**
     * Marks the calling thread's transaction so it rolls back however its method
     * ends -- the programmatic form of throwing, for a method that wants to
     * return a value and still undo its work.
     */
    public static void setRollbackOnly() {
        Physical p = used ? (Physical)CURRENT.get() : null;
        if(p == null) {
            throw new TransactionException.IllegalState("setRollbackOnly() outside a "
                    + "transaction: there is nothing to roll back");
        }
        if(p.participants == 0) {
            // The method that began the transaction: it returns normally and
            // its work is undone, with nothing thrown.
            p.localRollback = true;
        } else {
            // A joined method: whoever began the transaction must learn that
            // what it thinks it committed was not saved.
            p.rollbackOnly = true;
        }
    }

    /**
     * Starts what a {@code @Transactional} method asked for. Called by woven code.
     *
     * @param propagation one of the constants above
     * @param readOnly the annotation's readOnly
     * @param timeoutSeconds the annotation's timeout, zero or less for none
     */
    public static Transaction begin(int propagation, boolean readOnly, int timeoutSeconds) {
        used = true;
        Transaction tx = open(propagation, readOnly, timeoutSeconds);
        if(tx.physical != null && (tx.kind == KIND_JOINED || tx.kind == KIND_SAVEPOINT)) {
            tx.physical.participants++;
        }
        return tx;
    }

    private static Transaction open(int propagation, boolean readOnly, int timeoutSeconds) {
        Physical current = (Physical)CURRENT.get();
        switch(propagation) {
            case SUPPORTS:
                return current == null ? new Transaction(KIND_NONE, null, null, false, null)
                        : new Transaction(KIND_JOINED, current, null, false, null);
            case MANDATORY:
                if(current == null) {
                    throw new TransactionException.IllegalState("A MANDATORY transactional "
                            + "method was called with no transaction open. Call it from a "
                            + "@Transactional method, or change its propagation.");
                }
                return new Transaction(KIND_JOINED, current, null, false, null);
            case NEVER:
                if(current != null) {
                    throw new TransactionException.IllegalState("A NEVER transactional "
                            + "method was called inside a transaction.");
                }
                return new Transaction(KIND_NONE, null, null, false, null);
            case NOT_SUPPORTED:
                CURRENT.set(null);
                return new Transaction(KIND_NONE, null, current, true, null);
            case REQUIRES_NEW:
                return fresh(readOnly, timeoutSeconds, current, true);
            case NESTED:
                if(current == null) {
                    return fresh(readOnly, timeoutSeconds, null, false);
                }
                return nested(current);
            default:
                if(current != null) {
                    return new Transaction(KIND_JOINED, current, null, false, null);
                }
                return fresh(readOnly, timeoutSeconds, null, false);
        }
    }

    private static Transaction fresh(boolean readOnly, int timeoutSeconds, Physical suspended,
                                     boolean suspends) {
        Physical p = new Physical(null, readOnly, timeoutSeconds);
        CURRENT.set(p);
        return new Transaction(KIND_NEW, p, suspended, suspends, null);
    }

    private static Transaction nested(Physical current) {
        String name = "cn1_sp_" + (++current.savepoints);
        Database db = current.db;
        if(db == null) {
            // Nothing has run in the transaction yet, so there is no connection
            // and -- deliberately -- no process-wide default pool to borrow one
            // from: two servers in one process would hand this savepoint to
            // whichever started last. It is set when the first statement picks
            // the database.
            if(current.pendingSavepoints == null) {
                current.pendingSavepoints = new java.util.ArrayList();
            }
            current.pendingSavepoints.add(name);
            return new Transaction(KIND_SAVEPOINT, current, null, false, name);
        }
        try {
            flushSession(current);
            db.savepoint(name);
        } catch (IOException err) {
            throw new TransactionException("Could not set savepoint " + name + ": "
                    + err.getMessage(), err);
        }
        return new Transaction(KIND_SAVEPOINT, current, null, false, name);
    }

    /**
     * Ends a call that returned normally. Commits when the call began the
     * transaction, and does nothing more than restore state when it joined one.
     */
    public static void commit(Transaction tx) {
        if(tx == null || tx.completed) {
            return;
        }
        tx.completed = true;
        leave(tx);
        switch(tx.kind) {
            case KIND_SAVEPOINT:
                if(dropPending(tx)) {
                    // Never set: the nested method did not touch the database.
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

    /**
     * Ends a call that threw. Rolls back -- the call's own transaction, its
     * savepoint, or, when it joined one, by marking the shared transaction
     * rollback-only -- when {@code rollback} is true, and otherwise treats the
     * exception as a normal end, which is Spring's rule for a checked one.
     *
     * <p>Never throws: the woven code rethrows the method's own exception after
     * this returns, and a failure here would replace the cause with a symptom.
     * A rollback that fails is reported on standard error and its connection
     * closed, so the pool cannot hand out a session that is still inside a
     * transaction.
     */
    public static void afterThrow(Transaction tx, boolean rollback) {
        if(tx == null || tx.completed) {
            return;
        }
        if(!rollback) {
            try {
                commit(tx);
            } catch (RuntimeException err) {
                System.err.println("A transaction ended by a checked exception could not "
                        + "commit: " + err);
            }
            return;
        }
        tx.completed = true;
        leave(tx);
        switch(tx.kind) {
            case KIND_JOINED:
                tx.physical.rollbackOnly = true;
                return;
            case KIND_SAVEPOINT:
                if(dropPending(tx)) {
                    // Never set, so nothing ran after it: nothing to undo.
                    return;
                }
                try {
                    tx.physical.db.rollbackToSavepoint(tx.savepoint);
                    tx.physical.db.releaseSavepoint(tx.savepoint);
                    if(tx.physical.session != null) {
                        // The rows it wrote since the savepoint are gone, so the
                        // instances it still manages would be lying about them.
                        tx.physical.session.clear();
                    }
                } catch (Exception err) {
                    tx.physical.rollbackOnly = true;
                    System.err.println("Could not roll back to savepoint " + tx.savepoint
                            + "; the transaction will roll back instead: " + err);
                }
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

    /** A joined or nested call has ended. */
    private static void leave(Transaction tx) {
        if(tx.physical != null && (tx.kind == KIND_JOINED || tx.kind == KIND_SAVEPOINT)) {
            tx.physical.participants--;
        }
    }

    /** Forgets a savepoint that was never set; true when it was one. */
    private static boolean dropPending(Transaction tx) {
        java.util.List pending = tx.physical.pendingSavepoints;
        return pending != null && pending.remove(tx.savepoint);
    }

    /** Puts back what this call suspended, or clears what it began. */
    private static void restore(Transaction tx) {
        if(tx.kind == KIND_NEW || tx.suspends) {
            CURRENT.set(tx.suspended);
        }
    }

    private static void finishNew(Physical p) {
        if(p.rollbackOnly) {
            rollbackPhysical(p);
            throw new TransactionException.UnexpectedRollback("The transaction was rolled "
                    + "back because a method that joined it failed. Its own method returned "
                    + "normally, so this is what tells its caller the work was not saved.");
        }
        if(p.localRollback) {
            rollbackPhysical(p);
            return;
        }
        if(p.deadline != 0 && System.currentTimeMillis() > p.deadline) {
            rollbackPhysical(p);
            throw new TransactionException.TimedOut("The transaction ran past its timeout "
                    + "and was rolled back.");
        }
        if(p.db == null) {
            // Never touched the database: nothing began, so nothing commits.
            closeSession(p);
            return;
        }
        try {
            if(p.session != null) {
                // Flushes the session's pending changes INTO the transaction
                // before it commits; the adapter it runs on joined this
                // transaction, so the session's own commit sends no COMMIT.
                p.session.commitTransaction();
            }
            p.db.commitTransaction();
        } catch (Exception err) {
            rollbackPhysical(p);
            throw new TransactionException("Commit failed; the transaction was rolled "
                    + "back: " + err.getMessage(), err);
        }
        closeSession(p);
        release(p, false);
    }

    private static void rollbackPhysical(Physical p) {
        boolean broken = false;
        if(p.session != null) {
            try {
                if(p.session.isTransactionActive()) {
                    p.session.rollbackTransaction();
                }
            } catch (Exception err) {
                System.err.println("Could not roll back the transaction's session: " + err);
            }
        }
        closeSession(p);
        if(p.db != null) {
            try {
                if(p.db.isInTransaction()) {
                    p.db.rollbackTransaction();
                }
            } catch (Exception err) {
                broken = true;
                System.err.println("Rollback failed; closing the connection so the pool "
                        + "cannot reuse it: " + err);
            }
            release(p, broken);
        }
    }

    private static void closeSession(Physical p) {
        if(p.session == null) {
            return;
        }
        com.codename1.orm.session.Session session = p.session;
        p.session = null;
        try {
            session.close();
        } catch (Exception err) {
            System.err.println("Could not close the transaction's session: " + err);
        }
    }

    private static void release(Physical p, boolean broken) {
        Database db = p.db;
        DataSource pool = p.pool;
        p.db = null;
        if(db == null || pool == null) {
            return;
        }
        if(broken) {
            db.close();
        }
        pool.releaseToPool(db);
    }

    private static void flushSession(Physical p) throws IOException {
        if(p.session != null && p.session.isTransactionActive()) {
            try {
                p.session.flush();
            } catch (RuntimeException err) {
                throw new IOException("Flushing the session failed: " + err.getMessage());
            }
        }
    }

    /**
     * The transaction's connection for {@code pool}, borrowing it and sending
     * BEGIN the first time; null when the thread is not in a transaction or its
     * transaction is on another database. Called by {@link DataSource#borrow}.
     */
    public static Database joined(DataSource pool) throws IOException {
        if(!used) {
            return null;
        }
        Physical p = (Physical)CURRENT.get();
        if(p == null) {
            return null;
        }
        return materialize(p, pool);
    }

    private static Database materialize(Physical p, DataSource pool) throws IOException {
        if(p.pool == null) {
            if(pool == null) {
                return null;
            }
            p.pool = pool;
        } else if(pool != null && p.pool != pool) {
            return null;
        }
        if(p.db != null) {
            return p.db;
        }
        if(p.deadline != 0 && System.currentTimeMillis() > p.deadline) {
            throw new IOException("The transaction ran past its timeout before it used the "
                    + "database");
        }
        Database db = p.pool.borrowFromPool();
        try {
            if("sqlite".equals(db.dialect().getName())) {
                // The ORM relies on it and sets it outside a transaction, which is
                // the only place SQLite honours it: inside one it is ignored.
                db.execute("PRAGMA foreign_keys = ON", null);
            }
            db.beginTransaction(p.readOnly);
            if(p.pendingSavepoints != null) {
                for(int iter = 0 ; iter < p.pendingSavepoints.size() ; iter++) {
                    db.savepoint((String)p.pendingSavepoints.get(iter));
                }
                p.pendingSavepoints = null;
            }
        } catch (IOException err) {
            try {
                if(db.isInTransaction()) {
                    db.rollbackTransaction();
                }
            } catch (IOException ignored) {
                db.close();
            }
            p.pool.releaseToPool(db);
            throw err;
        }
        p.db = db;
        return db;
    }

    /** Whether {@code db} is the connection of the calling thread's transaction on {@code pool}. */
    public static boolean isJoined(DataSource pool, Database db) {
        if(!used) {
            return false;
        }
        Physical p = (Physical)CURRENT.get();
        return p != null && p.db == db && p.pool == pool && db != null;
    }

    /** Whether the calling thread's transaction is on {@code pool}, or could be. */
    public static boolean isActiveOn(DataSource pool) {
        if(!used) {
            return false;
        }
        Physical p = (Physical)CURRENT.get();
        return p != null && (p.pool == null || p.pool == pool);
    }

    /**
     * The managed session of the calling thread's transaction, opened on the
     * transaction's connection the first time it is asked for and flushed and
     * closed when the transaction ends.
     *
     * <p>This is what an injected {@code Session} delegates to. Outside a
     * transaction there is no session to give, and the refusal says where one
     * comes from.
     */
    public static com.codename1.orm.session.Session session(EntityManager entities) {
        Physical p = used ? (Physical)CURRENT.get() : null;
        if(p == null) {
            throw new TransactionException.IllegalState("The injected Session belongs to a "
                    + "transaction, and none is open. Annotate the calling method "
                    + "@Transactional -- @Transactional(readOnly = true) for one that only "
                    + "reads -- or open a session of your own with "
                    + "EntityManager.openSession().");
        }
        if(p.session != null) {
            return p.session;
        }
        if(entities.dataSource() == null) {
            throw new TransactionException.IllegalState("A transaction's session needs an "
                    + "entity manager over a pool; this one is pinned to one connection.");
        }
        try {
            if(materialize(p, entities.dataSource()) == null) {
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

    /**
     * Called by the session adapter when a session that joined this thread's
     * transaction rolls back: the rows are not the session's to keep, so the
     * transaction they are in cannot commit either.
     */
    public static void markRollbackOnly(DataSource pool) {
        Physical p = used ? (Physical)CURRENT.get() : null;
        if(p != null && (p.pool == null || p.pool == pool)) {
            p.rollbackOnly = true;
        }
    }
}
