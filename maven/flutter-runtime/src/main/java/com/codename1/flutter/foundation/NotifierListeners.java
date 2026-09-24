/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.foundation;

import dart.runtime.Funcs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The listener lists of ChangeNotifier mixins, which cannot hold them in a field (a
 * stub mixin becomes a Java interface).
 *
 * <p>This was a static IdentityHashMap that owned every notifier ever touched: reading
 * the list created an entry, emptying it kept the entry, and the strong key kept an
 * otherwise-unreachable model -- and through leftover listener closures, its widget
 * tree -- alive for good. Flutter keeps the list in the notifier, so it goes when the
 * notifier does. Here an entry exists only while it holds listeners, and it is keyed by
 * a weak identity reference, purged once its notifier has been collected.</p>
 */
public final class NotifierListeners {

    /** A notifier by identity, held weakly. */
    private static final class Key {
        final WeakReference<Object> ref;
        final int hash;

        Key(Object o) {
            ref = new WeakReference<Object>(o);
            hash = System.identityHashCode(o);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key)) {
                return false;
            }
            Object mine = ref.get();
            return mine != null && mine == ((Key) other).ref.get();
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private final Map<Key, List<Funcs.VoidFunc0>> lists = new HashMap<Key, List<Funcs.VoidFunc0>>();
    private int addsSincePurge;

    /** The list for {@code notifier}, or null when it has no listeners. */
    List<Funcs.VoidFunc0> get(Object notifier) {
        return lists.get(new Key(notifier));
    }

    /** The list for {@code notifier}, created if needed, to add to. */
    List<Funcs.VoidFunc0> forAdding(Object notifier) {
        if (++addsSincePurge >= 64) {
            addsSincePurge = 0;
            purge();
        }
        Key k = new Key(notifier);
        List<Funcs.VoidFunc0> l = lists.get(k);
        if (l == null) {
            l = new ArrayList<Funcs.VoidFunc0>();
            lists.put(k, l);
        }
        return l;
    }

    /** Removes one listener, and the entry with its last one. */
    void remove(Object notifier, Funcs.VoidFunc0 listener) {
        Key k = new Key(notifier);
        List<Funcs.VoidFunc0> l = lists.get(k);
        if (l != null) {
            l.remove(listener);
            if (l.isEmpty()) {
                lists.remove(k);
            }
        }
    }

    void clear(Object notifier) {
        // Emptied as well as dropped: a notification already running holds this list
        // and checks each listener against it before calling, so a notifier disposed
        // by one of its own listeners must stop there.
        List<Funcs.VoidFunc0> l = lists.remove(new Key(notifier));
        if (l != null) {
            l.clear();
        }
    }

    /** How many notifiers currently have an entry; for tests. */
    int size() {
        return lists.size();
    }

    /** Drops the entries whose notifier has been collected. */
    void purge() {
        Iterator<Key> it = lists.keySet().iterator();
        while (it.hasNext()) {
            if (it.next().ref.get() == null) {
                it.remove();
            }
        }
    }
}
