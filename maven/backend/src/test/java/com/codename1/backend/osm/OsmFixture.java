/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.osm;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

/// Writes a small `.osm.pbf` town for the tests: a 3x3 grid of streets 0.001
/// degrees apart at (10, 10), a one-way avenue, a footpath to a node no car
/// can reach, a cafe, an address and the town's place node.
final class OsmFixture {
    private OsmFixture() {
    }

    static final String CAFE = "Caf\u00e9 Central";

    static void write(String path, boolean ways, String blobCompression) throws IOException {
        List strings = new ArrayList();
        strings.add("");
        // Dense nodes: grid 1..9, footpath end 11, tagged 30..32.
        long[] ids = {1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 30, 31, 32};
        double[][] at = new double[ids.length][];
        for (int i = 0; i < 9; i++) {
            at[i] = new double[] {10 + 0.001 * (i / 3), 10 + 0.001 * (i % 3)};
        }
        at[9] = new double[] {10.002, 10.004};
        at[10] = new double[] {10.0011, 10.0011};
        at[11] = new double[] {10.0001, 10.0012};
        at[12] = new double[] {10.001, 10.001};
        String[][] nodeTags = new String[ids.length][];
        nodeTags[10] = new String[] {"amenity", "cafe", "name", CAFE};
        nodeTags[11] = new String[] {"addr:housenumber", "12", "addr:street", "First Street",
            "addr:city", "Testville"};
        nodeTags[12] = new String[] {"place", "town", "name", "Testville"};
        Out dense = new Out();
        Out idCol = new Out();
        Out latCol = new Out();
        Out lonCol = new Out();
        Out kv = new Out();
        long lastId = 0;
        long lastLat = 0;
        long lastLon = 0;
        for (int i = 0; i < ids.length; i++) {
            long lat = Math.round(at[i][0] * 1e7);
            long lon = Math.round(at[i][1] * 1e7);
            idCol.svarint(ids[i] - lastId);
            latCol.svarint(lat - lastLat);
            lonCol.svarint(lon - lastLon);
            lastId = ids[i];
            lastLat = lat;
            lastLon = lon;
            if (nodeTags[i] != null) {
                for (String s : nodeTags[i]) {
                    kv.varint(index(strings, s));
                }
            }
            kv.varint(0);
        }
        dense.bytes(1, idCol);
        dense.bytes(8, latCol);
        dense.bytes(9, lonCol);
        dense.bytes(10, kv);
        Out nodeGroup = new Out();
        nodeGroup.bytes(2, dense);

        Out wayGroup = new Out();
        way(wayGroup, strings, 100, new long[] {1, 2, 3}, "highway", "residential", "name", "First Street");
        way(wayGroup, strings, 101, new long[] {4, 5, 6}, "highway", "residential", "name", "Second Street");
        way(wayGroup, strings, 102, new long[] {7, 8, 9}, "highway", "residential", "name", "Third Street");
        way(wayGroup, strings, 103, new long[] {1, 4, 7}, "highway", "residential", "name", "West Avenue");
        way(wayGroup, strings, 104, new long[] {2, 5, 8}, "highway", "residential", "name", "Middle Avenue",
                "oneway", "yes");
        way(wayGroup, strings, 105, new long[] {3, 6, 9}, "highway", "residential", "name", "East Avenue");
        way(wayGroup, strings, 106, new long[] {9, 11}, "highway", "footway", "name", "Garden Path");

        Out st = new Out();
        for (int i = 0; i < strings.size(); i++) {
            st.bytes(1, ((String) strings.get(i)).getBytes("UTF-8"));
        }
        Out block = new Out();
        block.bytes(1, st);
        block.bytes(2, nodeGroup);
        if (ways) {
            block.bytes(2, wayGroup);
        }

        Out header = new Out();
        header.bytes(4, "OsmSchema-V0.6".getBytes("UTF-8"));
        header.bytes(4, "DenseNodes".getBytes("UTF-8"));

        ByteArrayOutputStream file = new ByteArrayOutputStream();
        blob(file, "OSMHeader", header.toByteArray(), "zlib");
        blob(file, "OSMData", block.toByteArray(), blobCompression);
        FileOutputStream fos = new FileOutputStream(path);
        try {
            fos.write(file.toByteArray());
        } finally {
            fos.close();
        }
    }

    private static void way(Out group, List strings, long id, long[] refs, String... tags) throws IOException {
        Out w = new Out();
        w.tag(1, 0);
        w.varint(id);
        Out keys = new Out();
        Out vals = new Out();
        for (int i = 0; i < tags.length; i += 2) {
            keys.varint(index(strings, tags[i]));
            vals.varint(index(strings, tags[i + 1]));
        }
        w.bytes(2, keys);
        w.bytes(3, vals);
        Out r = new Out();
        long last = 0;
        for (long ref : refs) {
            r.svarint(ref - last);
            last = ref;
        }
        w.bytes(8, r);
        group.bytes(3, w);
    }

    private static int index(List strings, String s) {
        int i = strings.indexOf(s);
        if (i < 0) {
            strings.add(s);
            i = strings.size() - 1;
        }
        return i;
    }

    private static void blob(ByteArrayOutputStream file, String type, byte[] raw, String compression)
            throws IOException {
        Out blob = new Out();
        if ("raw".equals(compression)) {
            blob.bytes(1, raw);
        } else if ("lz4".equals(compression)) {
            blob.tag(2, 0);
            blob.varint(raw.length);
            blob.bytes(6, raw);
        } else {
            Deflater d = new Deflater();
            d.setInput(raw);
            d.finish();
            byte[] buf = new byte[raw.length + 1024];
            int n = d.deflate(buf);
            d.end();
            byte[] z = new byte[n];
            System.arraycopy(buf, 0, z, 0, n);
            blob.tag(2, 0);
            blob.varint(raw.length);
            blob.bytes(3, z);
        }
        byte[] b = blob.toByteArray();
        Out h = new Out();
        h.bytes(1, type.getBytes("UTF-8"));
        h.tag(3, 0);
        h.varint(b.length);
        byte[] hb = h.toByteArray();
        file.write(hb.length >>> 24);
        file.write(hb.length >>> 16);
        file.write(hb.length >>> 8);
        file.write(hb.length);
        file.write(hb, 0, hb.length);
        file.write(b, 0, b.length);
    }

    /// A protobuf writer.
    static final class Out extends ByteArrayOutputStream {
        void varint(long v) {
            while ((v & ~0x7fL) != 0) {
                write((int) ((v & 0x7f) | 0x80));
                v >>>= 7;
            }
            write((int) v);
        }

        void svarint(long v) {
            varint((v << 1) ^ (v >> 63));
        }

        void tag(int field, int wire) {
            varint((long) field << 3 | wire);
        }

        void bytes(int field, byte[] b) {
            tag(field, 2);
            varint(b.length);
            write(b, 0, b.length);
        }

        void bytes(int field, Out o) {
            bytes(field, o.toByteArray());
        }
    }
}
