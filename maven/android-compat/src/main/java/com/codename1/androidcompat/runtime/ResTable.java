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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// Reads the binary resource table written by the build's
/// `com.codename1.android.rescompiler.TableWriter` and answers lookups by id.
///
/// The format is documented on that class; keep the two in step and bump
/// [#VERSION] on any change.
public final class ResTable {

    public static final int MAGIC = 0x434e3152;
    public static final int VERSION = 1;

    /// A style, array or plurals.
    public static final class Bag {
        public final int parent;
        public final int[] keys;
        public final ResValue[] values;

        Bag(int parent, int[] keys, ResValue[] values) {
            this.parent = parent;
            this.keys = keys;
            this.values = values;
        }

        /// The value for `key`, or null.
        public ResValue get(int key) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] == key) {
                    return values[i];
                }
            }
            return null;
        }
    }

    /// A file shipped under a flat name: an image, a font, a raw resource.
    public static final class FileRef {
        public final String name;

        FileRef(String name) {
            this.name = name;
        }
    }

    /// One resource and all its configured variants.
    public static final class Entry {
        public final int id;
        /// `type/name`.
        public final String name;
        final ResConfigSpec[] configs;
        final Object[] items;
        private int cachedGeneration = -1;
        private int cachedIndex = -1;

        Entry(int id, String name, ResConfigSpec[] configs, Object[] items) {
            this.id = id;
            this.name = name;
            this.configs = configs;
            this.items = items;
        }

        public String type() {
            return name.substring(0, name.indexOf('/'));
        }

        public String entryName() {
            return name.substring(name.indexOf('/') + 1);
        }

        /// The best variant for the device, or null when none matches.
        public Object best(DeviceConfig d) {
            if (cachedGeneration != d.generation) {
                int best = -1;
                for (int i = 0; i < configs.length; i++) {
                    if (!configs[i].matches(d)) {
                        continue;
                    }
                    if (best < 0 || configs[i].isBetterThan(configs[best], d)) {
                        best = i;
                    }
                }
                cachedIndex = best;
                cachedGeneration = d.generation;
            }
            return cachedIndex < 0 ? null : items[cachedIndex];
        }

        /// The configuration [#best(DeviceConfig)] picked.
        public ResConfigSpec bestConfig(DeviceConfig d) {
            best(d);
            return cachedIndex < 0 ? null : configs[cachedIndex];
        }
    }

    private final int packageId;
    private final Map<Integer, Entry> entries = new HashMap<Integer, Entry>();
    private final Map<String, Entry> byName = new HashMap<String, Entry>();

    private ResTable(int packageId) {
        this.packageId = packageId;
    }

    public int getPackageId() {
        return packageId;
    }

    public Entry get(int id) {
        return entries.get(Integer.valueOf(id));
    }

    /// Looks an entry up by `type/name`.
    public Entry get(String typeSlashName) {
        return byName.get(typeSlashName);
    }

    public static ResTable read(InputStream in) throws IOException {
        DataInputStream d = new DataInputStream(in);
        if (d.readInt() != MAGIC) {
            throw new IOException("Not a Codename One Android resource table");
        }
        int version = d.readInt();
        if (version != VERSION) {
            throw new IOException("Resource table version " + version + " is not " + VERSION
                    + "; rebuild the application against this runtime");
        }
        ResTable t = new ResTable(d.readInt());
        int ns = d.readInt();
        String[] strings = new String[ns];
        for (int i = 0; i < ns; i++) {
            byte[] b = new byte[d.readInt()];
            d.readFully(b);
            strings[i] = new String(b, "UTF-8");
        }
        int nc = d.readInt();
        ResConfigSpec[] configs = new ResConfigSpec[nc];
        for (int i = 0; i < nc; i++) {
            configs[i] = new ResConfigSpec(strings[d.readInt()]);
        }
        int nx = d.readInt();
        XmlNode[] xmls = new XmlNode[nx];
        for (int i = 0; i < nx; i++) {
            xmls[i] = readNode(d, strings);
        }
        int ne = d.readInt();
        for (int i = 0; i < ne; i++) {
            int id = d.readInt();
            String name = strings[d.readInt()];
            int nv = d.readInt();
            ResConfigSpec[] cfgs = new ResConfigSpec[nv];
            Object[] items = new Object[nv];
            for (int v = 0; v < nv; v++) {
                cfgs[v] = configs[d.readInt()];
                int kind = d.readByte();
                switch (kind) {
                    case 0:
                        items[v] = readValue(d, strings);
                        break;
                    case 1: {
                        int parent = d.readInt();
                        int n = d.readInt();
                        int[] keys = new int[n];
                        ResValue[] values = new ResValue[n];
                        for (int k = 0; k < n; k++) {
                            keys[k] = d.readInt();
                            values[k] = readValue(d, strings);
                        }
                        items[v] = new Bag(parent, keys, values);
                        break;
                    }
                    case 2: {
                        XmlNode x = xmls[d.readInt()];
                        x.setSource(name);
                        items[v] = x;
                        break;
                    }
                    case 3:
                        items[v] = new FileRef(strings[d.readInt()]);
                        break;
                    default:
                        throw new IOException("Corrupt resource table: item kind " + kind);
                }
            }
            Entry e = new Entry(id, name, cfgs, items);
            t.entries.put(Integer.valueOf(id), e);
            t.byName.put(name, e);
        }
        return t;
    }

    private static ResValue readValue(DataInputStream d, String[] strings) throws IOException {
        int type = d.readByte() & 0xff;
        int data = d.readInt();
        int s = d.readInt();
        return new ResValue(type, data, s < 0 ? null : strings[s]);
    }

    private static XmlNode readNode(DataInputStream d, String[] strings) throws IOException {
        String tag = strings[d.readInt()];
        int line = d.readInt();
        int na = d.readInt();
        int[] ids = new int[na];
        byte[] nss = new byte[na];
        String[] names = new String[na];
        ResValue[] values = new ResValue[na];
        for (int i = 0; i < na; i++) {
            ids[i] = d.readInt();
            nss[i] = d.readByte();
            names[i] = strings[d.readInt()];
            values[i] = readValue(d, strings);
        }
        int ti = d.readInt();
        int nch = d.readInt();
        XmlNode[] children = new XmlNode[nch];
        for (int i = 0; i < nch; i++) {
            children[i] = readNode(d, strings);
        }
        return new XmlNode(tag, line, ids, nss, names, values, ti < 0 ? null : strings[ti], children);
    }
}
