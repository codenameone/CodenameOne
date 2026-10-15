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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;

/// java.util.concurrent.CopyOnWriteArraySet.
public final class CopyOnWriteChecks {
    private CopyOnWriteChecks() {
    }

    private static List<Integer> some(Random random) {
        List<Integer> out = new ArrayList<Integer>();
        for (int i = random.nextInt(5); i > 0; i--) {
            out.add(random.nextInt(8) == 0 ? null : Integer.valueOf(random.nextInt(12)));
        }
        return out;
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("CopyOnWrite.aRandomSequenceOfOperationsMatchesTheJdk", () -> {
            // Every step's answer and the set after it, in insertion order
            // (which is what a copy-on-write set keeps), printed a hundred
            // steps to a line so a divergence names its step range.
            Random random = new Random(20261009L);
            CopyOnWriteArraySet<Integer> set = new CopyOnWriteArraySet<Integer>();
            int inconsistent = 0;
            StringBuilder trace = new StringBuilder();
            for (int step = 0; step < 5000; step++) {
                Integer value = random.nextInt(10) == 0 ? null : Integer.valueOf(random.nextInt(12));
                List<Integer> several = some(random);
                switch (random.nextInt(10)) {
                    case 0:
                    case 1:
                    case 2:
                        trace.append("add:").append(set.add(value));
                        break;
                    case 3:
                        trace.append("remove:").append(set.remove(value));
                        break;
                    case 4:
                        trace.append("contains:").append(set.contains(value));
                        trace.append(",containsAll:").append(set.containsAll(several));
                        break;
                    case 5:
                        trace.append("addAll:").append(set.addAll(several));
                        break;
                    case 6:
                        trace.append("removeAll:").append(set.removeAll(several));
                        break;
                    case 7:
                        trace.append("retainAll:").append(set.retainAll(several));
                        break;
                    case 8:
                        final int limit = random.nextInt(12);
                        trace.append("removeIf:").append(set.removeIf(x -> x != null && x.intValue() < limit));
                        break;
                    default:
                        if (random.nextInt(20) == 0) {
                            set.clear();
                            trace.append("clear");
                        }
                        break;
                }
                trace.append(' ').append(set).append(" h").append(set.hashCode()).append(" n").append(set.size())
                        .append(set.isEmpty() ? " e" : "").append(';');
                List<Integer> asList = new ArrayList<Integer>(set);
                if (!asList.equals(Arrays.asList(set.toArray()))
                        || !asList.equals(Arrays.asList(set.toArray(new Integer[0])))
                        || !set.equals(new HashSet<Integer>(asList))
                        || !new HashSet<Integer>(asList).equals(set)
                        || asList.size() != set.size()) {
                    inconsistent++;
                }
                if (step % 100 == 99) {
                    Bulk.p("steps" + (step - 99), trace.toString());
                    trace.setLength(0);
                }
            }
            Bulk.p("inconsistentSteps", inconsistent);
        });
        Bulk.check("CopyOnWrite.aCopyKeepsTheFirstOfEachElementInOrder", () -> {
            List<String> source = Arrays.asList("b", "a", "b", null, "c", "a", null);
            Bulk.p("copy", new ArrayList<String>(new CopyOnWriteArraySet<String>(source)));
        });
        Bulk.check("CopyOnWrite.anIteratorSeesTheSetAsItWasWhenItWasCreated", () -> {
            CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<String>(Arrays.asList("a", "b", "c"));
            List<String> seen = new ArrayList<String>();
            for (String s : set) {
                set.remove(s);
                set.add(s + s);
                seen.add(s);
            }
            Bulk.p("seen", seen);
            Bulk.p("after", new ArrayList<String>(set));
            final List<String> each = new ArrayList<String>();
            set.forEach(s -> {
                set.clear();
                each.add(s);
            });
            Bulk.p("forEach", each);
            Bulk.p("isEmpty", set.isEmpty());
        });
        Bulk.check("CopyOnWrite.anIteratorIsReadOnly", () -> {
            CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<String>(Arrays.asList("a"));
            Iterator<String> it = set.iterator();
            it.next();
            Bulk.pThrown("remove", UnsupportedOperationException.class, it::remove);
            Bulk.p("isEmpty", set.isEmpty());
        });
    }
}
