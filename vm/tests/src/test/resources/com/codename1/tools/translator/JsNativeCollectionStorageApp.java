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
import java.util.*;

public class JsNativeCollectionStorageApp {
    static int result;
    public static void main(String[] args) {
        int score = 0;
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 400; i++) list.add(i);
        list.subList(10, 390).clear();
        list.addAll(3, list);
        if (list.size() == 40 && list.get(3) == 0 && list.get(39) == 399) score |= 1;
        Integer[] array = list.toArray(new Integer[45]);
        list.clear();
        if (array[39] == 399 && array[40] == null && list.isEmpty()) score |= 2;

        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        Hashtable<Integer, Integer> table = new Hashtable<Integer, Integer>();
        for (int i = 0; i < 500; i++) { map.put(i, i * 3); table.put(i, i * 3); }
        for (int i = 0; i < 400; i++) { map.remove(i); table.remove(i); }
        if (map.size() == 100 && table.size() == 100 && map.get(499) == 1497 && table.get(499) == 1497) score |= 4;

        LinkedHashMap<Integer, Integer> linked = new LinkedHashMap<Integer, Integer>(2, 0.75f, true);
        for (int i = 0; i < 100; i++) linked.put(i, i);
        linked.get(0);
        if (linked.keySet().iterator().next() == 1 && linked.size() == 100) score |= 8;

        Object[] keys = new Object[300];
        IdentityHashMap<Object, Integer> identities = new IdentityHashMap<Object, Integer>();
        for (int i = 0; i < keys.length; i++) { keys[i] = new Object(); identities.put(keys[i], i); }
        for (int i = 0; i < 150; i++) identities.remove(keys[i]);
        if (identities.size() == 150 && identities.get(keys[299]) == 299 && !identities.containsKey(new Object())) score |= 16;

        ArrayDeque<Integer> deque = new ArrayDeque<Integer>();
        for (int i = 0; i < 300; i++) deque.addLast(i);
        for (int i = 0; i < 200; i++) deque.removeFirst();
        for (int i = 300; i < 600; i++) deque.addLast(i);
        deque.addFirst(199);
        if (deque.removeFirst() == 199 && deque.removeLast() == 599 && deque.size() == 399) score |= 32;

        StringBuilder text = new StringBuilder("caf\u00e9\u00ff");
        String before = text.toString();
        text.append('\u1234').append("xyz");
        text.delete(5, text.length());
        char[] chars = new char[5];
        text.getChars(0, 5, chars, 0);
        if (before.equals(text.toString()) && chars[4] == 255 && text.charAt(3) == 233) score |= 64;
        StringBuilder self = new StringBuilder("abc");
        self.append(self, 0, 3).insert(1, "\u00ff");
        String immutable = self.toString();
        self.setLength(0);
        self.setLength(8);
        if (immutable.equals("a\u00ffbcabc") && self.charAt(6) == 0) score |= 128;
        // HashSet keeps its table natively on the C targets; the JS port binds each of
        // those natives to a pure-Java twin. Equality by value, a null element,
        // removal, removal through the iterator and clear all go through them.
        HashSet<Object> set = new HashSet<Object>();
        for (int i = 0; i < 300; i++) set.add(Integer.valueOf(i));
        for (int i = 0; i < 200; i++) set.remove(Integer.valueOf(i));
        boolean dup = set.add(Integer.valueOf(250));
        set.add(null);
        set.add(new String(new char[] {'k', 'e', 'y'}));
        int seen = 0;
        for (Iterator<Object> it = set.iterator(); it.hasNext(); ) {
            Object o = it.next();
            seen++;
            if (o instanceof Integer && ((Integer) o).intValue() % 2 == 0) it.remove();
        }
        boolean shape = !dup && seen == 102 && set.size() == 52 && set.contains(null)
                && set.contains("key") && set.contains(Integer.valueOf(299)) && !set.contains(Integer.valueOf(298))
                && !set.contains(Integer.valueOf(5));
        set.clear();
        if (shape && set.isEmpty() && set.add("again") && set.size() == 1) score |= 256;
        result = score;
        System.out.println("RESULT=" + result);
    }
}
