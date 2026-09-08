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
import java.util.ArrayList;
import java.util.List;

/**
 * A fixed pool of connections to one SQLite database.
 *
 * Why a pool at all, when SQLite is compiled SQLITE_THREADSAFE=1 and a single
 * connection is already safe to share: serialized mode makes concurrent use SAFE
 * by serializing it, which means one connection gives no read concurrency at all.
 * Several connections against a WAL database do, because WAL lets readers proceed
 * while a writer is active.
 *
 * The pool is deliberately fixed-size and blocking rather than growing on demand:
 * an unbounded pool against a single file just moves the contention into SQLite
 * and makes the busy-timeout the thing that fails.
 */
public final class DbPool {
    private final List idle = new ArrayList();
    private final List all = new ArrayList();
    private boolean closed;

    private DbPool() {
    }

    /**
     * Opens `size` connections to `path` and puts the database in WAL mode.
     *
     * A ":memory:" database cannot be pooled - each connection would get its OWN
     * private database - so that is rejected rather than silently giving every
     * caller a different empty database.
     */
    public static DbPool open(String path, int size, int busyTimeoutMillis) throws IOException {
        if(path == null || ":memory:".equals(path)) {
            throw new IOException("An in-memory database cannot be pooled: each connection "
                    + "would get its own. Use Db.open(\":memory:\") directly.");
        }
        if(size < 1) {
            throw new IOException("Pool size must be at least 1");
        }
        DbPool pool = new DbPool();
        try {
            for(int iter = 0 ; iter < size ; iter++) {
                Db db = Db.open(path);
                db.setBusyTimeout(busyTimeoutMillis);
                if(iter == 0) {
                    // WAL is a property of the database file, not of the connection,
                    // so it only needs setting once - but every connection needs its
                    // own busy timeout.
                    db.enableWriteAheadLog();
                }
                pool.all.add(db);
                pool.idle.add(db);
            }
        } catch (IOException err) {
            pool.close();
            throw err;
        }
        return pool;
    }

    /**
     * Takes a connection, blocking until one is free. Always release it in a
     * finally, or prefer {@link #withConnection}, which cannot leak one.
     */
    public synchronized Db borrow() throws IOException {
        // Checked before the idle list, not only when it is empty. close() can run
        // while a borrower still holds a connection, and that borrower's finally
        // releases afterwards -- so the list can be non-empty after closing, and a
        // check that only guards the empty case hands out a closed connection.
        if(closed) {
            throw new IOException("Pool is closed");
        }
        while(idle.isEmpty()) {
            if(closed) {
                throw new IOException("Pool is closed");
            }
            try {
                wait();
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for a connection");
            }
        }
        return (Db)idle.remove(idle.size() - 1);
    }

    public synchronized void release(Db db) {
        if(db == null) {
            return;
        }
        // A release that arrives after close() belongs to a borrower that was still
        // running when the pool shut down. close() has already closed every
        // connection, so putting this one back would repopulate an idle list nobody
        // may draw from again.
        if(closed) {
            db.close();
            return;
        }
        idle.add(db);
        notifyAll();
    }

    /** Borrows a connection, runs body, and returns it however body ends. */
    public Object withConnection(Db.Work body) throws Exception {
        Db db = borrow();
        try {
            return body.run(db);
        } finally {
            release(db);
        }
    }

    /** Convenience: one transaction on a pooled connection. */
    public Object inTransaction(final Db.Work body) throws Exception {
        return withConnection(new Db.Work() {
            public Object run(Db db) throws Exception {
                return db.transaction(body);
            }
        });
    }

    public synchronized void close() {
        closed = true;
        for(int iter = 0 ; iter < all.size() ; iter++) {
            ((Db)all.get(iter)).close();
        }
        all.clear();
        idle.clear();
        notifyAll();
    }
}
