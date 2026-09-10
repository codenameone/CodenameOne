/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exercises the parts of ArrayList and IdentityHashMap that were changed to stop
 * allocating: ArrayList no longer allocates a backing array until the first growth,
 * and IdentityHashMap's key and value iterators no longer build an Entry per step.
 *
 * Every line is compared against a real JDK run, so the JDK is the oracle rather
 * than a hand-written expectation.
 */
public class CollectionSemanticsApp {
    static void emit(String k, Object v) {
        System.out.println("CASE|" + k + "|" + v);
    }

    public static void main(String[] args) {
        // ---- an ArrayList that is never added to -------------------------------
        List<String> empty = new ArrayList<String>();
        emit("empty.size", empty.size());
        emit("empty.isEmpty", empty.isEmpty());
        emit("empty.contains", empty.contains("x"));
        emit("empty.indexOf", empty.indexOf("x"));
        emit("empty.iterHasNext", empty.iterator().hasNext());
        emit("empty.toArrayLen", empty.toArray().length);
        emit("empty.toString", empty.toString());
        empty.clear();
        emit("empty.afterClear", empty.size());
        try {
            empty.get(0);
            emit("empty.get0", "no throw");
        } catch (IndexOutOfBoundsException err) {
            emit("empty.get0", "IndexOutOfBounds");
        }

        // ---- first growth, and growth past it ----------------------------------
        List<Integer> grow = new ArrayList<Integer>();
        for (int i = 0; i < 40; i++) {
            grow.add(Integer.valueOf(i));
            if (i < 3 || i == 9 || i == 10 || i == 11 || i == 12 || i == 39) {
                emit("grow.size@" + i, grow.size() + ":" + grow.get(0) + ":" + grow.get(i));
            }
        }
        emit("grow.toString", grow.toString());
        emit("grow.indexOf37", grow.indexOf(Integer.valueOf(37)));

        // ---- add-at-front on a fresh list (the growAtFront path) ---------------
        List<String> front = new ArrayList<String>();
        front.add(0, "b");
        front.add(0, "a");
        front.add("c");
        emit("front.toString", front.toString());
        emit("front.size", front.size());

        // ---- insert into the middle of a fresh list (growForInsert) ------------
        List<String> mid = new ArrayList<String>();
        mid.add("x");
        mid.add("z");
        mid.add(1, "y");
        emit("mid.toString", mid.toString());

        // ---- ensureCapacity on a fresh list ------------------------------------
        ArrayList<String> ec = new ArrayList<String>();
        ec.ensureCapacity(100);
        ec.add("only");
        emit("ec.toString", ec.toString());

        // ---- remove down to empty and re-add -----------------------------------
        List<String> churn = new ArrayList<String>();
        churn.add("p");
        churn.add("q");
        churn.remove("p");
        churn.remove(0);
        emit("churn.emptyAgain", churn.size());
        churn.add("r");
        emit("churn.readd", churn.toString());

        // ---- IdentityHashMap: identity semantics, and all three views ----------
        String k1 = new String("dup");
        String k2 = new String("dup");
        IdentityHashMap<String, String> ihm = new IdentityHashMap<String, String>();
        ihm.put(k1, "first");
        ihm.put(k2, "second");
        emit("ihm.size", ihm.size());
        emit("ihm.get1", ihm.get(k1));
        emit("ihm.get2", ihm.get(k2));
        emit("ihm.containsKey1", ihm.containsKey(k1));

        // Null key and null value must survive the table's NULL_OBJECT sentinel in
        // BOTH directions -- this is what the key/value iterators read directly now.
        ihm.put(null, "nullkey");
        ihm.put("nullval", null);
        emit("ihm.getNullKey", ihm.get(null));
        emit("ihm.getNullVal", String.valueOf(ihm.get("nullval")));
        emit("ihm.sizeWithNulls", ihm.size());

        int keyNulls = 0, keyCount = 0;
        for (Iterator<String> it = ihm.keySet().iterator(); it.hasNext();) {
            String k = it.next();
            keyCount++;
            if (k == null) {
                keyNulls++;
            }
        }
        emit("ihm.keyCount", keyCount);
        emit("ihm.keyNulls", keyNulls);

        int valNulls = 0, valCount = 0;
        for (Iterator<String> it = ihm.values().iterator(); it.hasNext();) {
            String v = it.next();
            valCount++;
            if (v == null) {
                valNulls++;
            }
        }
        emit("ihm.valCount", valCount);
        emit("ihm.valNulls", valNulls);

        int entryCount = 0, entryKeyNulls = 0, entryValNulls = 0;
        for (Map.Entry<String, String> e : ihm.entrySet()) {
            entryCount++;
            if (e.getKey() == null) {
                entryKeyNulls++;
            }
            if (e.getValue() == null) {
                entryValNulls++;
            }
        }
        emit("ihm.entryCount", entryCount);
        emit("ihm.entryKeyNulls", entryKeyNulls);
        emit("ihm.entryValNulls", entryValNulls);

        // keySet().contains and removal through the key view
        Set<String> keys = ihm.keySet();
        emit("ihm.keysContainsK1", keys.contains(k1));
        emit("ihm.keysRemoveK1", keys.remove(k1));
        emit("ihm.sizeAfterRemove", ihm.size());

        // iterator removal
        IdentityHashMap<String, String> rem = new IdentityHashMap<String, String>();
        String r1 = new String("r1");
        String r2 = new String("r2");
        rem.put(r1, "1");
        rem.put(r2, "2");
        for (Iterator<String> it = rem.keySet().iterator(); it.hasNext();) {
            if (it.next() == r1) {
                it.remove();
            }
        }
        emit("ihm.afterIterRemove", rem.size() + ":" + rem.get(r2));

        // a map big enough to force a rehash, iterated by key
        IdentityHashMap<Object, Integer> big = new IdentityHashMap<Object, Integer>();
        Object[] held = new Object[500];
        for (int i = 0; i < held.length; i++) {
            held[i] = new Object();
            big.put(held[i], Integer.valueOf(i));
        }
        long sum = 0;
        int seen = 0;
        for (Object o : big.keySet()) {
            sum += big.get(o).intValue();
            seen++;
        }
        emit("ihm.bigSeen", seen);
        emit("ihm.bigSum", sum);

        System.out.println("DONE");
    }
}
