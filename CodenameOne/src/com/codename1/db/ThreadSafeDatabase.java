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
import com.codename1.util.RunnableWithResult;
import com.codename1.util.RunnableWithResultSync;
import com.codename1.util.SuccessCallback;
import java.io.IOException;

/**
 * Wraps all database calls in a single thread so they are all proxied thru that thread
 *
 * @author Shai Almog
 */
public class ThreadSafeDatabase extends Database {
    private Database underlying;
    private final EasyThread et;
    
    /**
     * Wraps the given database with a threadsafe version
     * @param db the database
     */
    public ThreadSafeDatabase(Database db) {
        underlying = db;
        et = EasyThread.start("Database");
    }

    /**
     * Returns the underlying easy thread we can use to pipe tasks to the db thread
     * @return the easy thread object
     */
    public EasyThread getThread() {
        return et;
    }
    
    
    @Override
    public void beginTransaction() throws IOException {
        invokeWithException(new RunnableWithIOException() {
            public void run() throws IOException {
                underlying.beginTransaction();
            }
        });
    }

    interface RunnableWithIOException {
        public void run() throws IOException;
    }

    interface RunnableWithResponseOrIOException {
        public Object run() throws IOException;
    }
    
    private void invokeWithException(final RunnableWithIOException r) throws IOException {
        IOException err = et.run(new RunnableWithResultSync<IOException>() {
            public IOException run() {
                try {
                    r.run();
                    return null;
                } catch(IOException err) {
                    return err;
                }
            }
        });
        if(err != null) {
            throw err;
        }
    }

    private Object invokeWithException(final RunnableWithResponseOrIOException r) throws IOException {
        Object ret = et.run(new RunnableWithResultSync<Object>() {
            public Object run() {
                try {
                    return r.run();
                } catch(IOException err) {
                    return err;
                }
            }
        });
        if(ret instanceof IOException) {
            throw (IOException)ret;
        }
        return ret;
    }
    
    @Override
    public void commitTransaction() throws IOException {
        invokeWithException(new RunnableWithIOException() {
            public void run() throws IOException {
                underlying.beginTransaction();
            }
        });
    }

    @Override
    public void rollbackTransaction() throws IOException {
        invokeWithException(new RunnableWithIOException() {
            public void run() throws IOException {
                underlying.rollbackTransaction();
            }
        });
    }

    @Override
    public void close() {
        // close should NEVER throw an exception...
        et.run(new Runnable() {
            public void run() {
                try {
                    underlying.close();
                } catch(IOException err) {
                    Log.e(err);
                }
            }
        });
    }

    @Override
    public void execute(final String sql) throws IOException {
        invokeWithException(new RunnableWithIOException() {
            public void run() throws IOException {
                underlying.execute(sql);
            }
        });
    }

    @Override
    public void execute(final String sql, final String[] params) throws IOException {
        invokeWithException(new RunnableWithIOException() {
            public void run() throws IOException {
                underlying.execute(sql, params);
            }
        });
    }

    private class RowWrapper implements Row {
        private Row underlyingRow;
        public RowWrapper(Row underlyingRow) {
            this.underlyingRow = underlyingRow;
        }
        
        public byte[] getBlob(final int index) throws IOException {
            return (byte[])invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingRow.getBlob(index);
                }
            });
        }

        public double getDouble(final int index) throws IOException {
            return (Double)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingRow.getDouble(index);
                }
            });
        }

        public float getFloat(final int index) throws IOException {
            return (Float)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingRow.getFloat(index);
                }
            });
        }

        public int getInteger(final int index) throws IOException {
            return (Integer)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingRow.getInteger(index);
                }
            });
        }

        public long getLong(final int index) throws IOException {
            return (Long)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingRow.getLong(index);
                }
            });
        }

        public short getShort(final int index) throws IOException {
            return (Short)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingRow.getShort(index);
                }
            });
        }

        public String getString(final int index) throws IOException {
            return (String)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingRow.getString(index);
                }
            });
        }
        
    }
    
    private class CursorWrapper implements Cursor {
        private Cursor underlyingCursor;
        public CursorWrapper(Cursor underlyingCursor) {
            this.underlyingCursor = underlyingCursor;
        }
        
        public boolean first() throws IOException {
            return (Boolean)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.first();
                }
            });
        }

        public boolean last() throws IOException {
            return (Boolean)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.last();
                }
            });
        }

        public boolean next() throws IOException {
            return (Boolean)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.next();
                }
            });
        }

        public boolean prev() throws IOException {
            return (Boolean)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.prev();
                }
            });
        }

        public int getColumnIndex(final String columnName) throws IOException {
            return (Integer)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.getColumnIndex(columnName);
                }
            });
        }

        public String getColumnName(final int columnIndex) throws IOException {
            return (String)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.getColumnName(columnIndex);
                }
            });
        }

        public int getColumnCount() throws IOException {
            return (Integer)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.getColumnCount();
                }
            });
        }

        public int getPosition() throws IOException {
            return (Integer)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.getPosition();
                }
            });
        }

        public boolean position(final int row) throws IOException {
            return (Boolean)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.position(row);
                }
            });
        }

        public void close() throws IOException {
            invokeWithException(new RunnableWithIOException() {
                public void run() throws IOException {
                    underlyingCursor.close();
                }
            });
        }

        public Row getRow() throws IOException {
            return new RowWrapper((Row)invokeWithException(new RunnableWithResponseOrIOException() {
                public Object run() throws IOException {
                    return underlyingCursor.getRow();
                }
            }));
        }
        
    }
    
    @Override
    public Cursor executeQuery(final String sql, final String[] params) throws IOException {
        final Cursor[] curs = new Cursor[1];
        invokeWithException(new RunnableWithIOException() {
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
            public void run() throws IOException {
                curs[0] = underlying.executeQuery(sql, params);
            }
        });
        return new CursorWrapper(curs[0]);
    }

    @Override
    public void execute(final String sql, final Object... params) throws IOException {
        invokeWithException(new RunnableWithIOException() {
            public void run() throws IOException {
                underlying.execute(sql, params);
            }
        });
    }    
}
