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
package com.codename1.androidcompat.runtime;

import com.codename1.ui.Display;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The application's `assets/` tree. Codename One resources are flat, so the
/// build ships each asset under a flat name and writes an index
/// ([#INDEX]: one `path<TAB>flatName` line per file) that maps paths back.
public final class Assets {

    public static final String INDEX = "/cn1_android_assets.idx";

    private static Map<String, String> index;

    private Assets() {
    }

    private static Map<String, String> index() {
        if (index == null) {
            index = new HashMap<String, String>();
            InputStream in = Display.getInstance().getResourceAsStream(Assets.class, INDEX);
            if (in != null) {
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    try {
                        String line;
                        while ((line = r.readLine()) != null) {
                            int tab = line.indexOf('\t');
                            if (tab > 0) {
                                index.put(line.substring(0, tab), line.substring(tab + 1));
                            }
                        }
                    } finally {
                        r.close();
                    }
                } catch (IOException e) {
                    com.codename1.io.Log.e(e);
                }
            }
        }
        return index;
    }

    private static String normalize(String path) {
        String p = path;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.startsWith("file:///android_asset/")) {
            p = p.substring("file:///android_asset/".length());
        }
        return p;
    }

    /// The flat resource name an asset path was shipped under, or null.
    public static String flatName(String path) {
        return index().get(normalize(path));
    }

    public static InputStream open(String path) throws IOException {
        String flat = flatName(path);
        InputStream in = flat == null ? null : Display.getInstance().getResourceAsStream(Assets.class, "/" + flat);
        if (in == null) {
            throw new java.io.FileNotFoundException(path);
        }
        return in;
    }

    /// The names directly inside directory `path` ("" for the root).
    public static String[] list(String path) {
        String dir = normalize(path);
        if (dir.length() > 0 && !dir.endsWith("/")) {
            dir = dir + "/";
        }
        List<String> out = new ArrayList<String>();
        for (String p : index().keySet()) {
            if (!p.startsWith(dir)) {
                continue;
            }
            String rest = p.substring(dir.length());
            int slash = rest.indexOf('/');
            String name = slash < 0 ? rest : rest.substring(0, slash);
            if (!out.contains(name)) {
                out.add(name);
            }
        }
        java.util.Collections.sort(out);
        return out.toArray(new String[out.size()]);
    }
}
