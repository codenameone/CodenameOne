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

import com.codename1.io.Log;
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
    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("This database has been closed");
        }
    }

    private void invokeWithException(final RunnableWithIOException r) throws IOException {
        IOException err;
        synchronized (dispatchLock) {
            checkOpen();
            err = et.run(new RunnableWithResultSync<IOException>() {
                @Override
                @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
                public IOException run() {
                    try {
                        r.run();
                        return null;
                    } catch (IOException err) {
                        return err;
                    }
                }
            });
        }
        if (err != null) {
            throw err;
        }
    }

    private Object invokeWithException(final RunnableWithResponseOrIOException r) throws IOException {
        Object ret;
        synchronized (dispatchLock) {
            checkOpen();
            ret = et.run(new RunnableWithResultSync<Object>() {
                @Override
                @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
                public Object run() {
                    try {
                        return r.run();
                    } catch (IOException err) {
                        return err;
                    }
                }
            });
        }
        if (ret instanceof IOException) {
            throw (IOException) ret;
        }
        return ret;
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
        // Read directly rather than through the worker. This is the one method on Database with
        // no IOException, so it reads as a cheap accessor and callers poll it from the event
        // thread; routing it through the dispatch would block that thread behind whatever
        // statement is currently running, which on Android is an ANR. The value is a single
        // boolean written by the same worker that runs every transaction call, so the worst a
        // racing read sees is the state from immediately before or after a transition -- which
        // is all a caller on another thread could ever know anyway.
        return underlying.isInTransaction();
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
    public void close() {
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
            et.run(new RunnableWithResultSync<Object>() {
                @Override
                public Object run() {
                    try {
                        underlying.close();
                    } catch (IOException err) {
                        // close() cannot report a failure through its signature, so log it rather
                        // than dropping it silently.
                        Log.e(err);
                    }
                    return null;
                }
            });
            et.kill();
        }
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
