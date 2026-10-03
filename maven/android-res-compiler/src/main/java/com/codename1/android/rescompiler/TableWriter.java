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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Writes the binary resource table the runtime's `ResTable` reads.
///
/// Big-endian `DataOutputStream` layout, every reference to text an index into
/// one string pool:
///
/// ```
/// int magic 'CN1R', int version, int packageId
/// int nStrings  { int byteLength, UTF-8 bytes }
/// int nConfigs  { int string }                  canonical qualifier strings
/// int nXml      { node }                        compiled XML files
/// int nEntries  { int id, int string("type/name"), int nVariants { int config, item } }
///
/// item  := byte 0 value                         simple
///        | byte 1 int parent, int n { int key, value }   bag
///        | byte 2 int xmlIndex                  compiled XML file
///        | byte 3 int string                    file shipped under a flat name
/// value := byte type, int data, int string (-1 when absent)
/// node  := int tag, int line, int nAttrs { int attrId, byte ns, int name, value },
///          int text (-1), int nChildren { node }
/// ```
///
/// Kept in sync by hand with `com.codename1.androidcompat.runtime.ResTable`;
/// the format version is bumped on any change.
public final class TableWriter {

    public static final int MAGIC = 0x434e3152;
    public static final int VERSION = 1;

    private final int packageId;
    private final List<String> strings = new ArrayList<String>();
    private final Map<String, Integer> stringIndex = new HashMap<String, Integer>();
    private final List<String> configs = new ArrayList<String>();
    private final Map<String, Integer> configIndex = new HashMap<String, Integer>();
    private final List<XmlTree> xmls = new ArrayList<XmlTree>();

    public TableWriter(int packageId) {
        this.packageId = packageId;
    }

    public void write(List<Resource> resources, OutputStream out) throws IOException {
        // Body first, so every string and config is interned before the
        // pools are written ahead of it.
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(body);
        b.writeInt(resources.size());
        for (Resource r : resources) {
            b.writeInt(r.id);
            b.writeInt(str(r.type.tag + "/" + r.name));
            b.writeInt(r.variants.size());
            for (Resource.Variant v : r.variants) {
                b.writeInt(config(v.config.canonical()));
                writeItem(b, v.item);
            }
        }
        ByteArrayOutputStream xmlBytes = new ByteArrayOutputStream();
        DataOutputStream x = new DataOutputStream(xmlBytes);
        x.writeInt(xmls.size());
        for (XmlTree t : xmls) {
            writeNode(x, t);
        }
        x.flush();
        b.flush();

        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(MAGIC);
        d.writeInt(VERSION);
        d.writeInt(packageId);
        Charset utf8 = Charset.forName("UTF-8");
        d.writeInt(strings.size());
        for (String s : strings) {
            byte[] bytes = s.getBytes(utf8);
            d.writeInt(bytes.length);
            d.write(bytes);
        }
        d.writeInt(configs.size());
        for (String c : configs) {
            d.writeInt(str(c));
        }
        d.write(xmlBytes.toByteArray());
        d.write(body.toByteArray());
        d.flush();
    }

    private void writeItem(DataOutputStream b, Resource.Item item) throws IOException {
        if (item instanceof Resource.Simple) {
            b.writeByte(0);
            writeValue(b, ((Resource.Simple) item).value);
        } else if (item instanceof Resource.Bag) {
            Resource.Bag bag = (Resource.Bag) item;
            b.writeByte(1);
            b.writeInt(bag.parentId);
            // An unresolvable key is a compile error reported by the compiler,
            // so every entry here has a real key.
            b.writeInt(bag.keys.length);
            for (int i = 0; i < bag.keys.length; i++) {
                b.writeInt(bag.keys[i]);
                writeValue(b, bag.values[i]);
            }
        } else if (item instanceof Resource.Xml) {
            b.writeByte(2);
            xmls.add(((Resource.Xml) item).compiled);
            b.writeInt(xmls.size() - 1);
        } else if (item instanceof Resource.FileRes) {
            b.writeByte(3);
            b.writeInt(str(((Resource.FileRes) item).flatName));
        } else {
            throw new IOException("Unknown item " + item);
        }
    }

    private void writeValue(DataOutputStream b, Value v) throws IOException {
        b.writeByte(v.type);
        b.writeInt(v.data);
        b.writeInt(v.string == null ? -1 : str(v.string));
    }

    private void writeNode(DataOutputStream x, XmlTree t) throws IOException {
        x.writeInt(str(t.tag));
        x.writeInt(t.line);
        x.writeInt(t.attrs.size());
        for (XmlTree.Attr a : t.attrs) {
            x.writeInt(a.attrId);
            x.writeByte(a.ns);
            x.writeInt(str(a.name));
            writeValue(x, a.value);
        }
        x.writeInt(t.text == null ? -1 : str(t.text));
        x.writeInt(t.children.size());
        for (XmlTree c : t.children) {
            writeNode(x, c);
        }
    }

    private int str(String s) {
        Integer i = stringIndex.get(s);
        if (i == null) {
            i = strings.size();
            strings.add(s);
            stringIndex.put(s, i);
        }
        return i;
    }

    private int config(String c) {
        Integer i = configIndex.get(c);
        if (i == null) {
            i = configs.size();
            configs.add(c);
            configIndex.put(c, i);
            str(c);
        }
        return i;
    }
}
