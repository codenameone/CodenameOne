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
package com.codename1.android.rescompiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Every resource name the build knows and the id it was given, for both the
/// framework package (`android`, ids `0x01......`) and the package being
/// compiled.
///
/// The framework half is not compiled by an application build. It is read
/// from the symbols file the compatibility runtime was built with
/// (`META-INF/android-compat/framework.symbols`), because the runtime's
/// `android.R` constants were inlined into its classes when it was compiled
/// and an application has to agree with those exact numbers.
public final class SymbolTable {

    public static final String FRAMEWORK = "android";
    public static final int FRAMEWORK_PACKAGE_ID = 0x01;
    public static final int APP_PACKAGE_ID = 0x7f;
    /// The application's namespace: its own resources and, in an application
    /// build, the libraries' (see `ResourceCompiler.Request#library`).
    public static final String APP = "app";
    /// The AndroidX and Material resources the runtime ships.
    public static final int LIBRARY_PACKAGE_ID = 0x7e;

    /// `type/name` -> id, per package.
    private final Map<String, Map<String, Integer>> ids = new LinkedHashMap<String, Map<String, Integer>>();
    /// Qualified attr name (`android:text`, `app:colorPrimary`) -> definition.
    private final Map<String, AttrDef> attrs = new LinkedHashMap<String, AttrDef>();
    /// View tag -> class, for the framework's view classes (`LinearLayout` ->
    /// `android.widget.LinearLayout`). Only the classes the compatibility
    /// runtime implements are listed, so an unsupported widget in a layout is
    /// reported against the layout rather than as a javac error in generated
    /// code.
    private final Map<String, String> views = new TreeMap<String, String>();
    /// Styleable name -> ordered qualified attr names, per package.
    private final Map<String, Map<String, List<String>>> styleables = new LinkedHashMap<String, Map<String, List<String>>>();

    public void put(String pkg, ResType type, String name, int id) {
        Map<String, Integer> m = ids.get(pkg);
        if (m == null) {
            m = new TreeMap<String, Integer>();
            ids.put(pkg, m);
        }
        m.put(type.tag + "/" + name, id);
    }

    /// The id of `type/name` in `pkg`, or 0.
    public int get(String pkg, ResType type, String name) {
        Map<String, Integer> m = ids.get(pkg);
        if (m == null) {
            return 0;
        }
        Integer v = m.get(type.tag + "/" + name);
        return v == null ? 0 : v;
    }

    public Map<String, Integer> entries(String pkg) {
        Map<String, Integer> m = ids.get(pkg);
        return m == null ? Collections.<String, Integer>emptyMap() : m;
    }

    public void putAttr(String qualifiedName, AttrDef def) {
        attrs.put(qualifiedName, def);
    }

    public AttrDef attr(String qualifiedName) {
        return attrs.get(qualifiedName);
    }

    public void putStyleable(String pkg, String name, List<String> qualifiedAttrs) {
        Map<String, List<String>> m = styleables.get(pkg);
        if (m == null) {
            m = new TreeMap<String, List<String>>();
            styleables.put(pkg, m);
        }
        m.put(name, qualifiedAttrs);
    }

    public Map<String, List<String>> styleables(String pkg) {
        Map<String, List<String>> m = styleables.get(pkg);
        return m == null ? Collections.<String, List<String>>emptyMap() : m;
    }

    public void putView(String tag, String className) {
        views.put(tag, className);
    }

    public Map<String, String> views() {
        return java.util.Collections.unmodifiableMap(views);
    }

    /// The id of an attr given its qualified name, or 0.
    public int attrId(String qualifiedName) {
        int colon = qualifiedName.indexOf(':');
        if (colon < 0) {
            return 0;
        }
        return get(qualifiedName.substring(0, colon), ResType.ATTR, qualifiedName.substring(colon + 1));
    }

    // ---------------------------------------------------------------- io

    /// Writes the symbols of `pkg`: one line per resource, attr formats and
    /// enum/flag values, and the styleables. Text, sorted, so the file diffs
    /// cleanly and two builds of the same sources write identical bytes.
    public void write(String pkg, Writer w) throws IOException {
        w.write("# cn1-android-symbols 1\n");
        for (Map.Entry<String, Integer> e : entries(pkg).entrySet()) {
            w.write("res " + e.getKey() + " 0x" + Integer.toHexString(e.getValue()) + "\n");
        }
        for (Map.Entry<String, AttrDef> e : attrs.entrySet()) {
            if (!e.getKey().startsWith(pkg + ":")) {
                continue;
            }
            AttrDef d = e.getValue();
            StringBuilder sb = new StringBuilder("attr ").append(e.getKey()).append(' ')
                    .append(AttrDef.formatsToString(d.formats));
            for (Map.Entry<String, Integer> en : d.enums.entrySet()) {
                sb.append(" enum:").append(en.getKey()).append('=').append(en.getValue());
            }
            for (Map.Entry<String, Integer> fl : d.flags.entrySet()) {
                sb.append(" flag:").append(fl.getKey()).append("=0x").append(Integer.toHexString(fl.getValue()));
            }
            w.write(sb.append('\n').toString());
        }
        if (pkg.equals(FRAMEWORK)) {
            for (Map.Entry<String, String> e : views.entrySet()) {
                w.write("view " + e.getKey() + " " + e.getValue() + "\n");
            }
        }
        for (Map.Entry<String, List<String>> e : styleables(pkg).entrySet()) {
            StringBuilder sb = new StringBuilder("styleable ").append(e.getKey());
            for (String a : e.getValue()) {
                sb.append(' ').append(a);
            }
            w.write(sb.append('\n').toString());
        }
    }

    /// Reads a symbols file written by [#write(String,Writer)] for `pkg`.
    public void read(String pkg, InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            String[] parts = line.split(" ");
            if (parts[0].equals("res") && parts.length == 3) {
                String[] tn = parts[1].split("/", 2);
                ResType t = ResType.fromTag(tn[0]);
                if (t != null) {
                    put(pkg, t, tn[1], (int) Long.parseLong(parts[2].substring(2), 16));
                }
            } else if (parts[0].equals("attr") && parts.length >= 3) {
                AttrDef d = new AttrDef(parts[1], parts[2].equals("any") ? 0 : AttrDef.parseFormats(parts[2]));
                for (int i = 3; i < parts.length; i++) {
                    String p = parts[i];
                    int eq = p.indexOf('=');
                    if (p.startsWith("enum:")) {
                        d.enums.put(p.substring(5, eq), parseInt(p.substring(eq + 1)));
                    } else if (p.startsWith("flag:")) {
                        d.flags.put(p.substring(5, eq), parseInt(p.substring(eq + 1)));
                    }
                }
                putAttr(parts[1], d);
            } else if (parts[0].equals("view") && parts.length == 3) {
                putView(parts[1], parts[2]);
            } else if (parts[0].equals("styleable") && parts.length >= 2) {
                List<String> l = new ArrayList<String>();
                for (int i = 2; i < parts.length; i++) {
                    l.add(parts[i]);
                }
                putStyleable(pkg, parts[1], l);
            }
        }
    }

    static int parseInt(String s) {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return (int) Long.parseLong(s.substring(2), 16);
        }
        return Integer.parseInt(s);
    }
}
