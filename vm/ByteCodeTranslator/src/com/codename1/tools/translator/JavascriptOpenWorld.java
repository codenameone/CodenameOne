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
package com.codename1.tools.translator;

import java.util.ArrayList;
import java.util.List;

/**
 * Open-world JavaScript output: a host bundle that code translated LATER can link
 * against (the Playground runs user classes compiled in the browser inside the
 * VM that is already running).
 *
 * <p>A normal bundle is closed-world: classes and methods nobody calls are culled,
 * virtual-dispatch tables only list the ids some call site uses, monomorphic call
 * sites are devirtualized, field entries nobody reads are dropped, and generated
 * function names are minified. Each of those is sound only when every caller is in
 * the bundle. With {@code -Dparparvm.js.openWorld=<prefixes>} the classes whose
 * internal name starts with one of the comma-separated prefixes are kept whole
 * (every method, field and dispatch entry, canonical function names) and the
 * whole-bundle facts that late code could falsify are not used. A prefix starting
 * with {@code !} excludes, e.g. {@code java/,com/codename1/,!com/codename1/tools/}.
 * Everything outside the kept set is still culled normally.
 */
public final class JavascriptOpenWorld {
    private static List<String> keep;
    private static List<String> exclude;
    private static boolean configured;

    private JavascriptOpenWorld() {
    }

    private static void configure() {
        if (configured) {
            return;
        }
        configured = true;
        String spec = System.getProperty("parparvm.js.openWorld");
        if (spec != null) {
            setPrefixes(spec);
        }
    }

    /** Configures programmatically (null disables). Prefixes use '/' separators. */
    public static void setPrefixes(String spec) {
        configured = true;
        if (spec == null) {
            keep = null;
            exclude = null;
            return;
        }
        keep = new ArrayList<String>();
        exclude = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= spec.length(); i++) {
            if (i == spec.length() || spec.charAt(i) == ',') {
                String p = spec.substring(start, i).trim();
                start = i + 1;
                if (p.length() == 0) {
                    continue;
                }
                if (p.charAt(0) == '!') {
                    exclude.add(sanitize(p.substring(1)));
                } else {
                    keep.add(sanitize(p));
                }
            }
        }
    }

    private static String sanitize(String prefix) {
        return prefix.replace('/', '_').replace('.', '_').replace('$', '_');
    }

    public static boolean isEnabled() {
        configure();
        return keep != null;
    }

    /** Is this class (sanitized {@code java_lang_String} or internal {@code java/lang/String}) kept whole? */
    public static boolean keepsClass(String name) {
        configure();
        if (keep == null || name == null) {
            return false;
        }
        String s = sanitize(name);
        for (String e : exclude) {
            if (s.startsWith(e)) {
                return false;
            }
        }
        for (String k : keep) {
            if (s.startsWith(k)) {
                return true;
            }
        }
        return false;
    }

    /** Is this generated function identifier ({@code cn1_<class>_<method>...}) one a kept class defines? */
    public static boolean keepsFunction(String identifier) {
        if (!isEnabled() || !identifier.startsWith("cn1_")) {
            return false;
        }
        return keepsClass(identifier.substring(4));
    }
}
