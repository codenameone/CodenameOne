/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The data files the application's Python packages ship beside their
/// modules -- a JSON table, a font, a CMap -- read-only under `/cn1python/`,
/// where every module's `__file__` points.
///
/// The build packs them into one resource at the root of the application,
/// `/cn1python.dat`, because device resources are flat: a package's own
/// directory tree would not survive. The format is a count followed by that
/// many entries, each a UTF path relative to the root, a length and the
/// bytes (`java.io.DataOutputStream`). It is read once, on the first access
/// under the root; an application without package data has no resource and
/// an empty tree.
public final class PyPackageData {
    /// Must match `Emitter.PACKAGE_DATA_ROOT`.
    public static final String ROOT = "/cn1python/";
    /// The resource the build writes.
    public static final String RESOURCE = "/cn1python.dat";

    private static Map<String, byte[]> files;
    private static Map<String, List<String>> dirs;

    private PyPackageData() {
    }

    /// Whether `path` is under the package data root.
    public static boolean covers(String path) {
        return path.startsWith(ROOT) || ROOT.substring(0, ROOT.length() - 1).equals(path);
    }

    private static String key(String path) {
        String k = path.length() <= ROOT.length() ? "" : path.substring(ROOT.length());
        while (k.endsWith("/")) {
            k = k.substring(0, k.length() - 1);
        }
        return k;
    }

    private static synchronized void load() {
        if (files != null) {
            return;
        }
        Map<String, byte[]> f = new LinkedHashMap<String, byte[]>();
        Map<String, List<String>> d = new HashMap<String, List<String>>();
        d.put("", new ArrayList<String>());
        InputStream in = PyPackageData.class.getResourceAsStream(RESOURCE);
        if (in != null) {
            try (DataInputStream data = new DataInputStream(in)) {
                int count = data.readInt();
                for (int i = 0; i < count; i++) {
                    String path = data.readUTF();
                    byte[] bytes = new byte[data.readInt()];
                    data.readFully(bytes);
                    f.put(path, bytes);
                }
            } catch (IOException e) {
                RuntimeException corrupt = Py.error(Py.OS_ERROR, "corrupt " + RESOURCE + ": " + e.getMessage());
                corrupt.initCause(e);
                throw corrupt;
            }
        }
        // Indexed outside the try: its implicit catch (Throwable) would cover
        // the casts of d.get, which ParparVM does not check.
        for (String path : f.keySet()) {
            String child = path;
            while (true) {
                int slash = child.lastIndexOf('/');
                String parent = slash < 0 ? "" : child.substring(0, slash);
                String name = child.substring(slash + 1);
                List<String> list = d.get(parent);
                boolean known = list != null;
                if (!known) {
                    list = new ArrayList<String>();
                    d.put(parent, list);
                }
                if (!list.contains(name)) {
                    list.add(name);
                }
                // The root is always listed, so a walk ends there.
                if (known) {
                    break;
                }
                child = parent;
            }
        }
        dirs = d;
        files = f;
    }

    /// The bytes of the file at `path`, or null when there is none.
    public static byte[] read(String path) {
        load();
        return files.get(key(path));
    }

    public static boolean exists(String path) {
        load();
        String k = key(path);
        return files.containsKey(k) || dirs.containsKey(k);
    }

    public static boolean isDirectory(String path) {
        load();
        return dirs.containsKey(key(path));
    }

    /// The names in directory `path`, or null when it is not one.
    public static String[] list(String path) {
        load();
        List<String> l = dirs.get(key(path));
        return l == null ? null : l.toArray(new String[l.size()]);
    }

    public static long size(String path) {
        byte[] b = read(path);
        return b == null ? 0 : b.length;
    }
}
