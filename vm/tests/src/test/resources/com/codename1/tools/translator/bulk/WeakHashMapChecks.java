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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;

/// java.util.WeakHashMap while every key is strongly held, which is the
/// only time two runs are comparable: what a collector clears, and when, is
/// not something either promises.
public final class WeakHashMapChecks {
    private WeakHashMapChecks() {
    }

    /// Equal by value and colliding on purpose, so that a bucket holds several keys.
    private static final class WeakHashMapKey implements Comparable<WeakHashMapKey> {
        final int id;

        WeakHashMapKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof WeakHashMapKey && ((WeakHashMapKey) o).id == id;
        }

        @Override
        public int hashCode() {
            return id % 7;
        }

        @Override
        public int compareTo(WeakHashMapKey o) {
            return Integer.compare(id, o.id);
        }

        @Override
        public String toString() {
            return "k" + id;
        }
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("WeakHashMap.aRandomSequenceOfOperationsMatchesTheJdk", () -> {
            Random random = new Random(20260917L);
            // Strong references to every key ever used: nothing may be cleared.
            List<WeakHashMapKey> held = new ArrayList<WeakHashMapKey>();
            for (int i = 0; i < 40; i++) {
                held.add(new WeakHashMapKey(i));
            }
            WeakHashMap<WeakHashMapKey, String> map = new WeakHashMap<WeakHashMapKey, String>(4);
            StringBuilder trace = new StringBuilder();
            for (int step = 0; step < 4000; step++) {
                WeakHashMapKey key = held.get(random.nextInt(held.size()));
                // An equal key that is another object: found by equals, not identity.
                WeakHashMapKey equal = new WeakHashMapKey(key.id);
                String value = random.nextInt(8) == 0 ? null : "v" + step;
                switch (random.nextInt(6)) {
                    case 0:
                    case 1:
                        trace.append("put:").append(map.put(key, value));
                        break;
                    case 2:
                        trace.append("remove:").append(map.remove(equal));
                        break;
                    case 3:
                        trace.append("get:").append(map.get(equal));
                        trace.append(",containsKey:").append(map.containsKey(equal));
                        break;
                    case 4:
                        trace.append("containsValue:").append(map.containsValue(value));
                        break;
                    default:
                        if (random.nextInt(200) == 0) {
                            map.clear();
                            trace.append("clear");
                        }
                        break;
                }
                trace.append(" n").append(map.size()).append(map.isEmpty() ? " e" : "").append(';');
                if (step % 200 == 199) {
                    Bulk.p("steps" + (step - 199), trace.toString());
                    trace.setLength(0);
                }
            }
            Bulk.p("sorted", new TreeMap<WeakHashMapKey, String>(map));
            Bulk.p("keySetSize", map.keySet().size());
            Bulk.p("values", new ArrayList<String>(new TreeMap<WeakHashMapKey, String>(map).values()));
            // What the key set view iterates, against the JDK's: sorted, so the
            // two hash orders cannot differ.
            Bulk.p("keySet", Bulk.sorted(map.keySet()));
        });
        Bulk.check("WeakHashMap.theNullKeyIsAKeyLikeAnyOther", () -> {
            WeakHashMap<Object, String> map = new WeakHashMap<Object, String>();
            Bulk.p("put1", map.put(null, "a"));
            Bulk.p("put2", map.put(null, "b"));
            Bulk.p("get", map.get(null));
            Bulk.p("containsKey", map.containsKey(null));
            Bulk.p("size", map.size());
            Bulk.p("remove", map.remove(null));
            Bulk.p("containsKeyAfter", map.containsKey(null));
            Bulk.p("isEmpty", map.isEmpty());
        });
        Bulk.check("WeakHashMap.theConstructorsRejectWhatTheJdkRejects", () -> {
            Bulk.pThrown("capacityNegative", IllegalArgumentException.class,
                    () -> new WeakHashMap<Object, Object>(-1, 1));
            Bulk.pThrown("loadFactorZero", IllegalArgumentException.class,
                    () -> new WeakHashMap<Object, Object>(4, 0));
            Bulk.pThrown("loadFactorNegative", IllegalArgumentException.class,
                    () -> new WeakHashMap<Object, Object>(4, -1));
            try {
                new WeakHashMap<Object, Object>(4, Float.NaN);
                Bulk.p("nan", "none");
            } catch (IllegalArgumentException e) {
                Bulk.p("nan", "IllegalArgumentException");
                Bulk.p("nanCause", e.getCause());
            }
            Map<String, String> source = new TreeMap<String, String>();
            source.put("a", "1");
            source.put("b", "2");
            Bulk.p("copy", Bulk.sortedMap(new WeakHashMap<String, String>(source)));
            Bulk.p("copyEquals", source.equals(new WeakHashMap<String, String>(source)));
        });
        Bulk.check("WeakHashMap.anEntryDoesNotKeepItsKeyAlive", () -> {
            // A collector is asked, not ordered, so the check gives it time
            // and makes no claim about whether the dropped key went: only
            // that what remains is consistent either way.
            WeakHashMap<Object, String> map = new WeakHashMap<Object, String>();
            Object kept = new Object();
            map.put(kept, "kept");
            map.put(new Object(), "dropped");
            for (int i = 0; i < 50 && map.size() > 1; i++) {
                System.gc();
                Thread.sleep(20);
            }
            Bulk.p("getKept", map.get(kept));
            int size = map.size();
            Bulk.p("sizeInRange", size >= 1 && size <= 2);
            boolean consistent = true;
            if (size == 1) {
                consistent = map.keySet().iterator().next() == kept && !map.containsValue("dropped");
            }
            Bulk.p("consistent", consistent);
        });
    }
}
