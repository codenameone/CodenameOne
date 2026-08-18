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

package com.codename1.db;

import com.codename1.util.EasyThread;
import com.codename1.util.RunnableWithResultSync;

import java.io.IOException;

/// Confines a database and its cursors to a single thread.
///
/// A `Database` is not thread safe, and neither are the cursors it hands out. Wrapping one in this
/// class routes every call through one worker thread, so several application threads can share a
/// connection without coordinating.
///
/// ```java
/// Database db = new ThreadSafeDatabase(Database.openOrCreate("shared.db"));
/// ```
///
/// The cost is that every call is a thread handoff, so a tight loop over a large result set is
/// meaningfully slower than using a connection per thread. Prefer one database per thread when the
/// threads do not actually need to share state.
///
/// This class used to be deprecated, on the grounds that platform specific behaviour had defeated
/// it. That behaviour has since been fixed: the iOS port no longer closes SQLite handles from the
/// garbage collector thread, and it opens each connection in serialised mode rather than trying to
/// configure the whole process.
///
/// @author Shai Almog
public class ThreadSafeDatabase extends Database {
    private final Database underlying;
    private final EasyThread et;

    /// Guards against a second close. The worker is killed by the first one, so a synchronous
    /// hand-off afterwards would queue work nothing is left to run and block forever.
    ///
    /// Only ever read or written while holding `#dispatchLock`.
    private boolean closed;

    /// Serializes the closed check against the hand-off it guards.
    ///
    /// Testing the flag and queueing the work have to be one step. Otherwise two threads both see
    /// an open database, one of them closes it and kills the worker, and the other hands its task
    /// to a worker that is already gone and waits for a result that can never arrive. This class
    /// exists to be shared between threads, so that race is its main use case rather than an
    /// exotic one.
    ///
    /// Holding the lock across the hand-off costs nothing: the worker is a single thread and the
    /// hand-off is synchronous, so calls were already serialized. It is safe because no task
    /// queued here calls back into this wrapper -- every task body runs against `#underlying`.
    private final Object dispatchLock = new Object();

    /// Wraps the given database with a threadsafe version
    ///
    /// #### Parameters
    ///
    /// - `db`: the database
    public ThreadSafeDatabase(Database db) {
        underlying = db;
        et = EasyThread.start("Database");
    }

    /// Returns the underlying easy thread we can use to pipe tasks to the db thread
    ///
    /// #### Returns
    ///
    /// the easy thread object
    public EasyThread getThread() {
        return et;
    }


    @Override
    public void beginTransaction() throws IOException {
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                underlying.beginTransaction();
            }
        });
    }

    /// Refuses to hand work to a worker that has been killed.
    ///
    /// Guarding only close() was not enough: every other method still queued work and waited
    /// synchronously, so closing a cursor after its owning database had closed -- an ordinary
    /// cleanup order -- blocked forever instead of throwing.
    ///
    /// Call only while holding `#dispatchLock`, and queue the work it guards without releasing it.
    /// Refuses a hand-off to a worker that has already finished.
    ///
    /// `close()` from the worker itself asks it to stop once its queue drains, so a call that
    /// passed `#checkOpen()` a moment earlier can arrive after it has gone. EasyThread reports
    /// that rather than accepting work nothing will run; this turns it into the error the caller
    /// would have had a moment later.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the worker has stopped
    private void requireLiveWorker() throws IOException {
        if (et.isFinished()) {
            throw new IOException("This database has been closed");
        }
    }

    /// The same answer, for a refusal that arrives from the hand-off rather than from the check.
    ///
    /// Between reading that the worker is still accepting work and handing it some, the worker
    /// can decide to leave. EasyThread says so rather than queueing for a thread that has gone,
    /// and this is that refusal in the wrapper's own terms.
    ///
    /// #### Parameters
    ///
    /// - `refused`: what EasyThread raised
    ///
    /// #### Returns
    ///
    /// the error to throw
    private static IOException closedDuringHandoff(IllegalStateException refused) {
        return new IOException("This database has been closed", refused);
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("This database has been closed");
        }
    }

    private void invokeWithException(final RunnableWithIOException r) throws IOException {
        Object err;
        if (et.isThisIt()) {
            // Before the lock, not inside it. Already on the worker -- which getThread() hands out,
            // so a task running there can reach this -- handing work to that worker and waiting
            // for it is a deadlock on its own. Taking dispatchLock first makes it worse: another
            // thread can hold the lock while waiting for this worker, so this call would block on
            // the lock and that one on the worker, with neither able to move. The thread asking is
            // the thread that would do the work, so it does the work, and reads `closed` without
            // the lock -- there is one worker, so a close cannot be running on it concurrently.
            checkOpen();
            r.run();
            return;
        }
        synchronized (dispatchLock) {
            checkOpen();
            // A worker that has been asked to stop refuses new work rather than queueing it for
            // nobody. That can only be reached by a caller which passed the check above before
            // close() set the flag, and the answer it deserves is the one the check gives. The
            // refusal can also arrive from the hand-off itself, a moment later, which is why the
            // call below is wrapped as well.
            requireLiveWorker();
            try {
                err = et.run(new RunnableWithResultSync<Object>() {
                    @Override
                    @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
                    public Object run() {
                        try {
                            r.run();
                            return null;
                        } catch (Throwable err) {
                            // Throwable, not IOException. Anything escaping this callback is
                            // caught by the worker's own loop, which never delivers a result, so
                            // the caller waits forever holding dispatchLock and every later call
                            // queues behind it. An unchecked exception from the engine -- Android
                            // raises CursorIndexOutOfBoundsException for a bad column, for one --
                            // is enough to wedge the whole wrapper. Carrying it back keeps that a
                            // thrown error.
                            return err;
                        }
                    }
                });
            } catch (IllegalStateException refused) {
                throw closedDuringHandoff(refused);
            }
        }
        rethrow(err);
    }

    /// Re-raises whatever the worker carried back, preserving its kind.
    ///
    /// An unchecked exception is rethrown as itself so callers see the same failure they would
    /// from an unwrapped database; anything else that is not an IOException is wrapped, because
    /// this API promises IOException.
    private static void rethrow(Object err) throws IOException {
        if (err == null) {
            return;
        }
        if (err instanceof IOException) {
            throw (IOException) err;
        }
        if (err instanceof RuntimeException) {
            throw (RuntimeException) err;
        }
        if (err instanceof Error) {
            throw (Error) err;
        }
        throw new IOException(((Throwable) err).getMessage(), (Throwable) err);
    }

    private Object invokeWithException(final RunnableWithResponseOrIOException r) throws IOException {
        Object ret;
        if (et.isThisIt()) {
            // Before the lock; see the other invokeWithException for why both matter.
            checkOpen();
            return r.run();
        }
        synchronized (dispatchLock) {
            checkOpen();
            requireLiveWorker();
            try {
                ret = et.run(new RunnableWithResultSync<Object>() {
                    @Override
                    @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
                    public Object run() {
                        try {
                            return r.run();
                        } catch (Throwable err) {
                            // See invokeWithException above: an escape here wedges the wrapper.
                            return new Failure(err);
                        }
                    }
                });
            } catch (IllegalStateException refused) {
                throw closedDuringHandoff(refused);
            }
        }
        if (ret instanceof Failure) {
            rethrow(((Failure) ret).cause);
        }
        return ret;
    }

    /// Distinguishes a failure carried back from the worker from a result that happens to be a
    /// Throwable, which a query returning one as a value could otherwise be mistaken for.
    private static final class Failure {
        private final Throwable cause;

        Failure(Throwable cause) {
            this.cause = cause;
        }
    }

    @Override
    public void commitTransaction() throws IOException {
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                underlying.commitTransaction();
            }
        });
    }

    @Override
    public void rollbackTransaction() throws IOException {
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                underlying.rollbackTransaction();
            }
        });
    }

    @Override
    public boolean isInTransaction() {
        // beginTransaction() runs against the underlying database, so it moves that object's
        // flag, not this wrapper's. Without delegating, the wrapper always reported false --
        // including immediately after a successful begin -- and a caller trusting it would skip
        // the commit or rollback it still owed.
        //
        // Read under the dispatch lock, but without handing work to the worker. The lock is what
        // publishes the write: every transaction call runs inside it, so acquiring it here gives
        // the happens-before that a plain read of another thread's field does not, and without it
        // a polling thread could keep seeing stale state indefinitely.
        //
        // Deliberately not a dispatch. This is the one method on Database with no IOException, so
        // it reads as a cheap accessor and callers poll it from the event thread; queueing work
        // would park that thread behind whatever statement is running, which on Android is an
        // ANR. Taking the lock can still wait for an in-flight call, but only for as long as that
        // call holds it, and the answer is then current rather than stale.
        if (et.isThisIt()) {
            // Already on the worker, so the lock buys nothing and can cost everything: another
            // thread may hold it while waiting for this worker, and blocking here would leave the
            // two waiting on each other. The publishing this comment describes is not needed on
            // this path either -- the writes were made by this same thread.
            return underlying.isInTransaction();
        }
        synchronized (dispatchLock) {
            return underlying.isInTransaction();
        }
    }

    @Override
    public void changeKey(final DatabaseConfig config) throws IOException {
        // Without this the wrapper inherits the base implementation, which always reports
        // NOT_SUPPORTED, so a wrapped database could never be re-keyed however capable the
        // underlying port is.
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                underlying.changeKey(config);
            }
        });
    }

    @Override
    public void close() throws IOException {
        if (et.isThisIt()) {
            // Before the lock, for the reason invokeWithException gives: another thread can hold
            // dispatchLock while waiting for this worker, and blocking on it here would leave
            // neither able to move. One worker means no close can be running on it concurrently,
            // so the flag is read and set without the lock.
            if (closed) {
                return;
            }
            closed = true;
            try {
                underlying.close();
            } finally {
                // In a finally, because a close can fail -- the browser port reports a failed
                // OPFS flush that way -- and the wrapper is closed either way. Leaving the worker
                // alive there would keep the thread and everything it retains for the life of the
                // process, and getThread() would still hand callers a thread that services work
                // against a database that is gone.
                //
                // Asked to stop once its queue is empty, rather than killed. kill() stops the
                // worker after the current task -- which is this one -- and everything behind it
                // is dropped, including a call from a thread that took dispatchLock and passed
                // checkOpen before the flag above was set. That caller waits for an answer that
                // never comes.
                //
                // Draining instead runs those calls, which fail against the closed database, and
                // the thread then ends. One that arrives after the thread has gone is refused by
                // EasyThread rather than queued for nobody, and the refusal is turned into the
                // same "closed" IOException the check above raises.
                et.killWhenIdle();
            }
            return;
        }
        synchronized (dispatchLock) {
            if (closed) {
                // close() is idempotent by contract, and after the first call there is no worker
                // left to service the hand-off below.
                return;
            }
            // Flip the flag and shut down without releasing the lock, so a concurrent operation
            // either queues before this point or is rejected by checkOpen() after it. In between
            // it would hand work to a dead worker and wait on it forever.
            closed = true;
            // Synchronous on purpose. EasyThread.run(Runnable) is fire and forget, so this used to
            // return while the database was still open, and a delete() on the next line would race
            // it and fail with the file still in use.
            Object failure;
            try {
                failure = handOffClose();
            } catch (IllegalStateException refused) {
                // The worker has already stopped, which means another close got there first: the
                // worker's own path closes the database before it asks the thread to drain, and
                // this call arrived after the thread had gone. Every other entry point turns this
                // refusal into "this database has been closed", but close() is idempotent by
                // contract, so here it is not an error to report.
                //
                // Closed directly rather than assumed closed, because a refusal is all this can
                // observe and a worker can also stop without having closed anything --
                // getThread() is public, and killWhenIdle() on it is a call anybody can make.
                //
                // Waited for first, because a refusal does not mean the worker has gone.
                // EasyThread stops accepting the moment a stop is asked for, while the tasks it
                // already took keep running -- against this database. Closing here and then is
                // the concurrent use this wrapper exists to prevent, and it can leave an
                // operation that was already under way failing or half done. Once the worker has
                // left its loop there is nothing to race with, and closing an already closed
                // database does nothing.
                et.awaitFinished();
                underlying.close();
                return;
            }
            // The worker is shut down either way. A close that failed still leaves this wrapper
            // closed, so there is nothing to retry - but the caller has to be told, because on
            // some ports a failing close means the data never reached storage.
            //
            // Drained rather than killed, as on the worker's own path. Anything handed to the
            // worker through getThread() while the close above was running has been accepted, and
            // kill() would end the loop with those tasks still in it: a synchronous caller would
            // wait for a result that never comes, and an asynchronous one would lose its work
            // without being told.
            et.killWhenIdle();
            if (failure instanceof Failure) {
                rethrow(((Failure) failure).cause);
            }
        }
    }

    /// Closes the underlying database on the worker and reports what happened.
    ///
    /// Synchronous on purpose. EasyThread.run(Runnable) is fire and forget, so this used to
    /// return while the database was still open, and a delete() on the next line would race it
    /// and fail with the file still in use.
    ///
    /// #### Returns
    ///
    /// null, or a Failure carrying what the close threw
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: if the worker has already stopped and cannot take the work
    private Object handOffClose() {
        return et.run(new RunnableWithResultSync<Object>() {
            @Override
            public Object run() {
                try {
                    underlying.close();
                    return null;
                } catch (Exception err) {
                    return new Failure(err);
                } catch (Error err) {
                    // Wrapped, and caught in two clauses rather than as Throwable with an
                    // instanceof test, because PMD forbids both that and returning a caught
                    // local directly. Letting either escape is far worse than reporting it:
                    // the worker's own loop would swallow it without delivering a result,
                    // and this call is synchronous and holds dispatchLock, so close()
                    // would block forever and never shut the worker down.
                    return new Failure(err);
                }
            }
        });
    }

    @Override
    public void execute(final String sql) throws IOException {
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                underlying.execute(sql);
            }
        });
    }

    @Override
    public void execute(final String sql, final String[] params) throws IOException {
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                underlying.execute(sql, params);
            }
        });
    }

    @Override
    public Cursor executeQuery(final String sql, final String[] params) throws IOException {
        final Cursor[] curs = new Cursor[1];
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                curs[0] = underlying.executeQuery(sql, params);
            }
        });
        return new CursorWrapper(curs[0]);
    }

    @Override
    public Cursor executeQuery(final String sql) throws IOException {
        final Cursor[] curs = new Cursor[1];
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                curs[0] = underlying.executeQuery(sql);
            }
        });
        return new CursorWrapper(curs[0]);
    }

    @Override
    public Cursor executeQuery(final String sql, final Object... params) throws IOException {
        final Cursor[] curs = new Cursor[1];
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                curs[0] = underlying.executeQuery(sql, params);
            }
        });
        return new CursorWrapper(curs[0]);
    }

    @Override
    public void execute(final String sql, final Object... params) throws IOException {
        invokeWithException(new RunnableWithIOException() {
            @Override
            public void run() throws IOException {
                underlying.execute(sql, params);
            }
        });
    }

    interface RunnableWithIOException {
        // PMD Fix (UnnecessaryModifier): Interface methods are implicitly public.
        void run() throws IOException;
    }

    interface RunnableWithResponseOrIOException {
        // PMD Fix (UnnecessaryModifier): Interface methods are implicitly public.
        Object run() throws IOException;
    }

    private class RowWrapper implements RowExt {
        private final Row underlyingRow;

        public RowWrapper(Row underlyingRow) {
            this.underlyingRow = underlyingRow;
        }

        @Override
        public byte[] getBlob(final int index) throws IOException {
            return (byte[]) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingRow.getBlob(index);
                }
            });
        }

        @Override
        public double getDouble(final int index) throws IOException {
            return (Double) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingRow.getDouble(index);
                }
            });
        }

        @Override
        public float getFloat(final int index) throws IOException {
            return (Float) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingRow.getFloat(index);
                }
            });
        }

        @Override
        public int getInteger(final int index) throws IOException {
            return (Integer) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingRow.getInteger(index);
                }
            });
        }

        @Override
        public long getLong(final int index) throws IOException {
            return (Long) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingRow.getLong(index);
                }
            });
        }

        @Override
        public short getShort(final int index) throws IOException {
            return (Short) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingRow.getShort(index);
                }
            });
        }

        @Override
        public String getString(final int index) throws IOException {
            return (String) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingRow.getString(index);
                }
            });
        }


        @Override
        public boolean wasNull() throws IOException {
            return (Boolean) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return Database.wasNull(underlyingRow);
                }
            });
        }
    }

    /// Implements `CursorExt` as well as `Cursor`, because every cursor a Codename One port
    /// returns implements it and the wrapper is documented as transparent. Without it a wrapped
    /// cursor silently lost the capability: `Database#count(Cursor)` answered -1 even on Android,
    /// where the exact count is already known, and the extension calls bypassed the worker
    /// thread this class exists to funnel everything through.
    private class CursorWrapper implements CursorExt {
        private final Cursor underlyingCursor;

        public CursorWrapper(Cursor underlyingCursor) {
            this.underlyingCursor = underlyingCursor;
        }

        @Override
        public boolean first() throws IOException {
            return (Boolean) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.first();
                }
            });
        }

        @Override
        public boolean last() throws IOException {
            return (Boolean) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.last();
                }
            });
        }

        @Override
        public boolean next() throws IOException {
            return (Boolean) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.next();
                }
            });
        }

        @Override
        public boolean prev() throws IOException {
            return (Boolean) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.prev();
                }
            });
        }

        @Override
        public int getColumnIndex(final String columnName) throws IOException {
            return (Integer) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.getColumnIndex(columnName);
                }
            });
        }

        @Override
        public String getColumnName(final int columnIndex) throws IOException {
            return (String) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.getColumnName(columnIndex);
                }
            });
        }

        @Override
        public int getColumnCount() throws IOException {
            return (Integer) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.getColumnCount();
                }
            });
        }

        @Override
        public int getPosition() throws IOException {
            return (Integer) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.getPosition();
                }
            });
        }

        @Override
        public boolean position(final int row) throws IOException {
            return (Boolean) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.position(row);
                }
            });
        }

        @Override
        public void beforeFirst() throws IOException {
            invokeWithException(new RunnableWithIOException() {
                @Override
                public void run() throws IOException {
                    Database.beforeFirst(underlyingCursor);
                }
            });
        }

        @Override
        public int getCount() throws IOException {
            Integer count = (Integer) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return Integer.valueOf(Database.count(underlyingCursor));
                }
            });
            // -1 is the documented "cannot say cheaply" answer, so it is also the right answer if
            // the dispatch ever hands back nothing.
            return count == null ? -1 : count.intValue();
        }

        @Override
        public void close() throws IOException {
            if (et.isThisIt()) {
                // Before the lock, like every other entry point: another thread can hold
                // dispatchLock while waiting for this worker, so taking it here would leave the
                // two waiting on each other. The read of `closed` is safe for the same reason --
                // one worker, so no close can be running on it concurrently.
                if (closed) {
                    return;
                }
                underlyingCursor.close();
                return;
            }
            // Under the lock so the database cannot close between the check and the hand-off,
            // which would turn this no-op into a spurious "database has been closed". The monitor
            // is reentrant, so the nested acquisition inside invokeWithException is free.
            synchronized (dispatchLock) {
                if (closed) {
                    // Closing the database already invalidated its cursors, and cursor close is
                    // idempotent, so closing one afterwards is a no-op rather than an error.
                    // Closing the cursor after the database is an ordinary cleanup order.
                    return;
                }
                invokeWithException(new RunnableWithIOException() {
                    @Override
                    public void run() throws IOException {
                        underlyingCursor.close();
                    }
                });
            }
        }

        @Override
        public Row getRow() throws IOException {
            return new RowWrapper((Row) invokeWithException(new RunnableWithResponseOrIOException() {
                @Override
                public Object run() throws IOException {
                    return underlyingCursor.getRow();
                }
            }));
        }

    }
}
