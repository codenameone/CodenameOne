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

/**
 * Collections reached through interface-typed references.
 *
 * The declared type at the call site is java.util.List, not ArrayList, so a
 * linker that resolves against the declared owner rather than the receiver runs
 * AbstractList's method -- which throws. Every real application does this in
 * its first ten lines, and it is invisible to a probe that declares the
 * concrete type.
 */
public class CollectionProbe {
    public static void main(String[] a) {
        java.util.List<String> l = new ArrayList<String>();
        l.add("b"); l.add("a"); l.add("c");
        Collections.sort(l);
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("k", 7);
        Set<String> s = new HashSet<String>();
        s.add("x"); s.add("x");
        Iterator<String> it = l.iterator();
        StringBuilder walked = new StringBuilder();
        while (it.hasNext()) { walked.append(it.next()); }
        Collection<String> c = l;
        System.out.println("PROBE CollectionProbe: list=" + l + " size=" + l.size()
            + " map=" + m.get("k") + " set=" + s.size() + " walked=" + walked
            + " contains=" + c.contains("a") + " removed=" + l.remove("a") + " now=" + l);
        new Form("Collection").show();
    }
}
