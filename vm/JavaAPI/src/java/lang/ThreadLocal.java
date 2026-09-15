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

import java.util.HashMap;

/// @author shannah
///
/// THE VALUES LIVE ON THE THREAD, not in a map owned by this object, and that is
/// the whole of the design. What it replaces was a `Map<Thread,T>` plus a
/// `Set<Thread>` held HERE and written by every thread that touched the variable,
/// with no synchronization of any kind -- so two threads calling `set` at the same
/// instant both wrote into one `java.util.HashMap`.
///
/// That map is open addressed with linear probing, and a torn insert leaves a table
/// whose probe sequence has no terminator: every later lookup of an absent key walks
/// the whole table and never stops. MEASURED on a Codename One backend server, which
/// sets a ThreadLocal once per request: 14 threads spinning inside
/// `java.util.HashMap.put` at 1450% CPU, the process answering nothing and never
/// recovering. It needs no virtual threads and no collector involvement -- any two
/// platform threads sharing a ThreadLocal can do it.
///
/// It also LEAKED. Nothing ever removed an entry for a thread that had died, so a
/// server with a thread per connection accumulated one live entry per connection for
/// the life of the process, and the ThreadLocal kept both the Thread and its value
/// reachable. Keying off the Thread instead means the whole table dies with the
/// thread that owns it.
///
/// There is no lock here and none is needed: a thread only ever reads and writes its
/// OWN map, which nothing else can reach.
public class ThreadLocal<T> extends Object {

    public ThreadLocal() {
        super();
    }

    protected T initialValue() {
        return null;
    }

    /// The calling thread's table, created on first use so a thread that never
    /// touches a ThreadLocal pays nothing for one.
    private static HashMap tableOfCurrentThread() {
        Thread t = Thread.currentThread();
        HashMap table = t.threadLocalValues;
        if(table == null) {
            table = new HashMap();
            t.threadLocalValues = table;
        }
        return table;
    }

    public T get() {
        HashMap table = tableOfCurrentThread();
        // containsKey rather than a null test, so a ThreadLocal deliberately set to
        // null is not re-initialised on every read -- which is what the Set this
        // replaces was for.
        if(!table.containsKey(this)) {
            table.put(this, initialValue());
        }
        return (T)table.get(this);
    }

    public void set(T value) {
        tableOfCurrentThread().put(this, value);
    }

    public void remove() {
        tableOfCurrentThread().remove(this);
    }
}
