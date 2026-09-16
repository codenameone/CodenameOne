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
package java.lang;

/// @author shannah
///
/// THE VALUES LIVE ON THE THREAD, under a WEAK key, and both halves of that are
/// load bearing.
///
/// What this replaces was a `Map<Thread,T>` plus a `Set<Thread>` held HERE and
/// written by every thread that touched the variable, with no synchronization of
/// any kind -- so two threads calling `set` at the same instant both wrote into one
/// `java.util.HashMap`. That map is open addressed with linear probing, and a torn
/// insert leaves a probe sequence with no terminator: every later lookup of an
/// absent key walks the whole table and never stops. MEASURED on a Codename One
/// backend server, which sets a ThreadLocal once per request: 14 threads spinning
/// inside `java.util.HashMap.put` at 1450% CPU, the process answering nothing and
/// never recovering. It needs no virtual threads and no collector involvement --
/// any two platform threads sharing a ThreadLocal can do it.
///
/// Keying off the thread fixes that, and there is no lock here because a thread
/// only ever reads and writes its OWN table, which nothing else can reach.
///
/// THE KEY IS WEAK IN THE OTHER COPY, because the obvious version of that trade
/// leaks the other way. It is STRONG here, and Entry says why: this port's
/// java.lang.ref.Reference is a stub that answers null, so a weak key would
/// discard every live binding. The rest of the reasoning is the other copy's.
/// A long-lived worker thread that touches a short-lived ThreadLocal -- library or
/// request code that creates them dynamically -- would pin the ThreadLocal AND its
/// value until the thread died, however long ago the application dropped its last
/// reference. The old layout at least let an unreachable ThreadLocal take its
/// values with it. Holding the key weakly keeps that property and the new one:
/// nothing here keeps a ThreadLocal alive, and an entry whose key has been
/// collected is swept on the next access to this thread's table.
///
/// A LINEAR SCAN, not a hash table, and that is deliberate. A thread holds a
/// handful of these in any real program, the scan is over an array of entries with
/// no hashing and no probe sequence, and it is the sweep: every access already
/// walks the whole table, so purging cleared keys costs nothing extra. It also
/// cannot develop the pathology described above, which is what this class is
/// recovering from.
public class ThreadLocal<T> extends Object {

    /// One binding. The KEY is the referent, held weakly; the value is strong,
    /// because it is only reachable through an entry whose key is still alive.
    /// Package private rather than private: java.lang.Thread declares the array
    /// that holds these, and both classes live in java.lang.
    ///
    /// THE KEY IS STRONG HERE, and only here. The twin of this class in
    /// vm/JavaAPI holds it weakly, so a ThreadLocal the application has dropped
    /// does not survive on a long-lived thread. That cannot be done on this port:
    /// `java.lang.ref.Reference` in Ports/CLDC11 is a codavaj-generated stub whose
    /// `get()` returns null unconditionally and whose `clear()` does nothing, so a
    /// weak key reads as collected the instant it is stored. Every entry would be
    /// swept as stale on the next access -- `set` followed by `get` would answer
    /// `initialValue()`, and repeated `get` would recompute it every time.
    ///
    /// So the retention this port cannot avoid is the lesser fault, and it is the
    /// behaviour this class has always had. Give CLDC11 a working Reference and
    /// this should become the weak form the other copy uses.
    static final class Entry {
        final ThreadLocal key;
        Object value;
        boolean initialised;

        Entry(ThreadLocal key, Object value) {
            this.key = key;
            this.value = value;
        }

        /// Named for the WeakReference accessor the other copy inherits, so the two
        /// implementations read the same way at every use.
        ThreadLocal get() {
            return key;
        }
    }

    public ThreadLocal() {
        super();
    }

    protected T initialValue() {
        return null;
    }

    /// This thread's entry for this ThreadLocal, sweeping entries whose key has
    /// been collected on the way past. Returns null when there is no binding.
    ///
    /// `create` allocates the table and the entry rather than answering null, which
    /// is what separates `set` from `get` on an unbound variable.
    private Entry entryOfCurrentThread(boolean create) {
        Thread t = Thread.currentThread();
        Entry[] table = t.threadLocalValues;
        if(table == null) {
            if(!create) {
                return null;
            }
            table = new Entry[8];
            t.threadLocalValues = table;
        }
        Entry mine = null;
        int free = -1;
        for(int iter = 0 ; iter < table.length ; iter++) {
            Entry e = table[iter];
            if(e == null) {
                if(free < 0) {
                    free = iter;
                }
                continue;
            }
            Object key = e.get();
            if(key == null) {
                // UNREACHABLE while the key is strong -- see Entry -- and kept so
                // the two copies of this class stay line for line comparable, and so
                // that giving CLDC11 a real Reference is a one-line change here
                // rather than a re-derivation.
                table[iter] = null;
                if(free < 0) {
                    free = iter;
                }
                continue;
            }
            if(key == this) {
                mine = e;
            }
        }
        if(mine != null || !create) {
            return mine;
        }
        if(free < 0) {
            // Grown rather than probed: there is no hash and no displacement, so a
            // full table just needs more room.
            Entry[] grown = new Entry[table.length * 2];
            System.arraycopy(table, 0, grown, 0, table.length);
            free = table.length;
            table = grown;
            t.threadLocalValues = grown;
        }
        mine = new Entry(this, null);
        table[free] = mine;
        return mine;
    }

    public T get() {
        Entry e = entryOfCurrentThread(true);
        if(!e.initialised) {
            // The initial value is computed ONCE and then remembered, including
            // when it is null -- a ThreadLocal deliberately set to null must not be
            // re-initialised on every read, which is what the Set this replaces was
            // for.
            e.initialised = true;
            e.value = initialValue();
        }
        return (T)e.value;
    }

    public void set(T value) {
        Entry e = entryOfCurrentThread(true);
        e.initialised = true;
        e.value = value;
    }

    public void remove() {
        Thread t = Thread.currentThread();
        Entry[] table = t.threadLocalValues;
        if(table == null) {
            return;
        }
        for(int iter = 0 ; iter < table.length ; iter++) {
            Entry e = table[iter];
            if(e != null && e.get() == this) {
                table[iter] = null;
                return;
            }
        }
    }
}
