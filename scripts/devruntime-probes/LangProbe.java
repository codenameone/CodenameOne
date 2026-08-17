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
import com.codename1.ui.*;
import java.util.*;
public class LangProbe {
    static String sw(String s) { switch (s) { case "a": return "A"; case "b": return "B"; default: return "?"; } }
    static int sum(int... xs) { int t = 0; for (int x : xs) t += x; return t; }
    public static void main(String[] a) {
        StringBuilder r = new StringBuilder();
        java.util.List<String> l = new ArrayList<String>();
        l.add("x"); l.add("y");
        Map<String,Integer> m = new HashMap<String,Integer>();
        m.put("k", 7);
        int[][] grid = new int[2][3];
        grid[1][2] = 9;
        r.append("list=").append(l).append(" map=").append(m.get("k"));
        r.append(" grid=").append(grid[1][2]).append(" sw=").append(sw("b"));
        r.append(" var=").append(sum(1,2,3));
        Object o = l;
        r.append(" inst=").append(o instanceof java.util.List);
        long big = 1L << 40; double d = 3.5;
        r.append(" long=").append(big).append(" d=").append(d);
        char c = "hello".charAt(1);
        r.append(" ch=").append(c).append(" sub=").append("hello".substring(1,3));
        Collections.sort(l, new Comparator<String>() {
            public int compare(String p, String q) { return q.compareTo(p); }
        });
        r.append(" sorted=").append(l);
        System.out.println("PROBE LangProbe: " + r);
        new Form("Lang").show();
    }
}
