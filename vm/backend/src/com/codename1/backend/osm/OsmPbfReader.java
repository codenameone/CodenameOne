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

import com.codename1.backend.FileIo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/// Streams the nodes and ways out of an OpenStreetMap `.osm.pbf` file.
///
/// The format is a sequence of blobs, each a length-prefixed `BlobHeader`
/// followed by a `Blob` holding a zlib-compressed (or raw) `PrimitiveBlock`.
/// Field numbers follow `fileformat.proto` and `osmformat.proto` of the
/// OSM-binary project. Relations are skipped: nothing built from an extract
/// here needs them.
///
/// Every length and index read from the file is checked before it is used,
/// so a truncated or hostile file fails with an [IOException] rather than an
/// out-of-bounds read.
final class OsmPbfReader {
    private static final int MAX_HEADER = 64 * 1024;
    private static final int MAX_BLOB = 32 * 1024 * 1024;

    /// Receives elements as they are decoded. The tag array is reused between
    /// calls: keys at even indexes, values at odd ones, `tagCount` pairs.
    interface Sink {
        boolean wantsNodes();

        boolean wantsWays();

        void node(long id, int latE7, int lonE7, String[] tags, int tagCount);

        void way(long id, long[] refs, int refCount, String[] tags, int tagCount);
    }

    private OsmPbfReader() {
    }

    /// Reads the whole file, handing every node and way to `sink`.
    static void read(String path, Sink sink) throws IOException {
        int fd = FileIo.openRead(path);
        if (fd < 0) {
            throw new IOException("OpenStreetMap extract does not exist: " + path);
        }
        try {
            long[] info = new long[4];
            if (FileIo.stat(fd, info) != 0 || info[2] != 0) {
                throw new IOException("Not a readable file: " + path);
            }
            long size = info[0];
            long pos = 0;
            Block block = new Block(sink);
            byte[] four = new byte[4];
            while (pos < size) {
                readFully(fd, four, 4, pos);
                pos += 4;
                int headerLength = (four[0] & 0xff) << 24 | (four[1] & 0xff) << 16
                        | (four[2] & 0xff) << 8 | (four[3] & 0xff);
                if (headerLength <= 0 || headerLength > MAX_HEADER) {
                    throw new IOException("Corrupt blob header in " + path);
                }
                byte[] header = new byte[headerLength];
                readFully(fd, header, headerLength, pos);
                pos += headerLength;
                String type = null;
                long dataSize = -1;
                Proto h = new Proto(header, 0, headerLength);
                while (h.next()) {
                    if (h.field == 1 && h.wire == 2) {
                        type = h.string();
                    } else if (h.field == 3 && h.wire == 0) {
                        dataSize = h.varint();
                    } else {
                        h.skip();
                    }
                }
                if (type == null || dataSize < 0 || dataSize > MAX_BLOB || pos + dataSize > size) {
                    throw new IOException("Corrupt blob header in " + path);
                }
                byte[] blob = new byte[(int) dataSize];
                readFully(fd, blob, (int) dataSize, pos);
                pos += dataSize;
                if ("OSMHeader".equals(type)) {
                    checkHeader(unpack(blob));
                } else if ("OSMData".equals(type)) {
                    block.parse(unpack(blob));
                }
                // Any other blob type is unknown to the spec's readers too, and
                // is skipped as it says.
            }
        } finally {
            FileIo.close(fd);
        }
    }

    private static void checkHeader(byte[] data) throws IOException {
        Proto p = new Proto(data, 0, data.length);
        while (p.next()) {
            if (p.field == 4 && p.wire == 2) {
                String feature = p.string();
                if (!"OsmSchema-V0.6".equals(feature) && !"DenseNodes".equals(feature)) {
                    throw new IOException("The extract requires an unsupported feature: " + feature);
                }
            } else {
                p.skip();
            }
        }
    }

    private static byte[] unpack(byte[] blob) throws IOException {
        Proto p = new Proto(blob, 0, blob.length);
        long rawSize = -1;
        int zStart = -1;
        int zLength = 0;
        while (p.next()) {
            if (p.field == 1 && p.wire == 2) {
                int len = p.length();
                byte[] raw = new byte[len];
                System.arraycopy(blob, p.pos, raw, 0, len);
                return raw;
            } else if (p.field == 2 && p.wire == 0) {
                rawSize = p.varint();
            } else if (p.field == 3 && p.wire == 2) {
                zLength = p.length();
                zStart = p.pos;
                p.pos += zLength;
            } else if (p.wire == 2 && p.field >= 4 && p.field <= 7) {
                throw new IOException("The extract uses a blob compression other than zlib;"
                        + " re-encode it with zlib (osmium cat -o out.osm.pbf in.osm.pbf)");
            } else {
                p.skip();
            }
        }
        if (zStart < 0 || rawSize < 0 || rawSize > MAX_BLOB) {
            throw new IOException("Corrupt blob in the extract");
        }
        byte[] out = new byte[(int) rawSize];
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(blob, zStart, zLength);
            int done = 0;
            while (done < out.length) {
                int n = inflater.inflate(out, done, out.length - done);
                if (n <= 0) {
                    // Nothing produced is the end either way; looping again
                    // without new input would spin.
                    break;
                }
                done += n;
            }
            if (done != out.length) {
                throw new IOException("Truncated blob in the extract");
            }
        } catch (DataFormatException err) {
            IOException wrapped = new IOException("Corrupt zlib data in the extract: " + err.getMessage());
            wrapped.initCause(err);
            throw wrapped;
        } finally {
            inflater.end();
        }
        return out;
    }

    private static void readFully(int fd, byte[] out, int length, long position) throws IOException {
        int done = 0;
        while (done < length) {
            int n = FileIo.pread(fd, out, done, length - done, position + done);
            if (n <= 0) {
                throw new IOException("Unexpected end of the extract");
            }
            done += n;
        }
    }

    /// One `PrimitiveBlock`: its string table, coordinate grid, and groups.
    private static final class Block {
        private final Sink sink;
        private String[] strings = new String[0];
        private String[] tags = new String[32];
        private long[] refs = new long[256];
        private long granularity;
        private long latOffset;
        private long lonOffset;

        Block(Sink sink) {
            this.sink = sink;
        }

        void parse(byte[] data) throws IOException {
            granularity = 100;
            latOffset = 0;
            lonOffset = 0;
            int stStart = -1;
            int stLength = 0;
            // Groups are parsed after the scalars, which may follow them.
            int[] groups = new int[16];
            int groupCount = 0;
            Proto p = new Proto(data, 0, data.length);
            while (p.next()) {
                if (p.field == 1 && p.wire == 2) {
                    stLength = p.length();
                    stStart = p.pos;
                    p.pos += stLength;
                } else if (p.field == 2 && p.wire == 2) {
                    int len = p.length();
                    if (groupCount + 2 > groups.length) {
                        int[] grown = new int[groups.length * 2];
                        System.arraycopy(groups, 0, grown, 0, groupCount);
                        groups = grown;
                    }
                    groups[groupCount++] = p.pos;
                    groups[groupCount++] = len;
                    p.pos += len;
                } else if (p.field == 17 && p.wire == 0) {
                    granularity = p.varint();
                } else if (p.field == 19 && p.wire == 0) {
                    latOffset = p.varint();
                } else if (p.field == 20 && p.wire == 0) {
                    lonOffset = p.varint();
                } else {
                    p.skip();
                }
            }
            if (granularity <= 0) {
                throw new IOException("Corrupt coordinate granularity in the extract");
            }
            strings = stStart < 0 ? new String[0] : stringTable(data, stStart, stLength);
            for (int i = 0; i < groupCount; i += 2) {
                group(data, groups[i], groups[i + 1]);
            }
        }

        private static String[] stringTable(byte[] data, int start, int length) throws IOException {
            int count = 0;
            Proto p = new Proto(data, start, length);
            while (p.next()) {
                if (p.field == 1 && p.wire == 2) {
                    count++;
                }
                p.skip();
            }
            String[] out = new String[count];
            p = new Proto(data, start, length);
            int i = 0;
            while (p.next()) {
                if (p.field == 1 && p.wire == 2) {
                    out[i++] = p.string();
                } else {
                    p.skip();
                }
            }
            return out;
        }

        private void group(byte[] data, int start, int length) throws IOException {
            Proto p = new Proto(data, start, length);
            while (p.next()) {
                if (p.field == 1 && p.wire == 2 && sink.wantsNodes()) {
                    int len = p.length();
                    node(data, p.pos, len);
                    p.pos += len;
                } else if (p.field == 2 && p.wire == 2 && sink.wantsNodes()) {
                    int len = p.length();
                    dense(data, p.pos, len);
                    p.pos += len;
                } else if (p.field == 3 && p.wire == 2 && sink.wantsWays()) {
                    int len = p.length();
                    way(data, p.pos, len);
                    p.pos += len;
                } else {
                    p.skip();
                }
            }
        }

        private int latE7(long raw) {
            return (int) ((latOffset + granularity * raw) / 100);
        }

        private int lonE7(long raw) {
            return (int) ((lonOffset + granularity * raw) / 100);
        }

        private String string(long index) throws IOException {
            if (index < 0 || index >= strings.length) {
                throw new IOException("String index outside the block's table");
            }
            return strings[(int) index];
        }

        private void ensureTags(int pairs) {
            if (pairs * 2 > tags.length) {
                tags = new String[pairs * 4];
            }
        }

        private void node(byte[] data, int start, int length) throws IOException {
            long id = 0;
            long lat = 0;
            long lon = 0;
            int kStart = -1;
            int kLength = 0;
            int vStart = -1;
            int vLength = 0;
            Proto p = new Proto(data, start, length);
            while (p.next()) {
                if (p.field == 1 && p.wire == 0) {
                    id = Proto.zigzag(p.varint());
                } else if (p.field == 2 && p.wire == 2) {
                    kLength = p.length();
                    kStart = p.pos;
                    p.pos += kLength;
                } else if (p.field == 3 && p.wire == 2) {
                    vLength = p.length();
                    vStart = p.pos;
                    p.pos += vLength;
                } else if (p.field == 8 && p.wire == 0) {
                    lat = Proto.zigzag(p.varint());
                } else if (p.field == 9 && p.wire == 0) {
                    lon = Proto.zigzag(p.varint());
                } else {
                    p.skip();
                }
            }
            int count = pairTags(data, kStart, kLength, vStart, vLength);
            sink.node(id, latE7(lat), lonE7(lon), tags, count);
        }

        private int pairTags(byte[] data, int kStart, int kLength, int vStart, int vLength)
                throws IOException {
            if (kStart < 0) {
                return 0;
            }
            Proto k = new Proto(data, kStart, kLength);
            Proto v = new Proto(data, vStart < 0 ? kStart : vStart, vStart < 0 ? 0 : vLength);
            int count = 0;
            while (k.more()) {
                if (!v.more()) {
                    throw new IOException("Tag keys and values differ in count");
                }
                ensureTags(count + 1);
                tags[count * 2] = string(k.varint());
                tags[count * 2 + 1] = string(v.varint());
                count++;
            }
            return count;
        }

        private void dense(byte[] data, int start, int length) throws IOException {
            Proto ids = null;
            Proto lats = null;
            Proto lons = null;
            Proto kv = null;
            Proto p = new Proto(data, start, length);
            while (p.next()) {
                if (p.wire == 2 && (p.field == 1 || p.field == 8 || p.field == 9 || p.field == 10)) {
                    int len = p.length();
                    Proto packed = new Proto(data, p.pos, len);
                    p.pos += len;
                    if (p.field == 1) {
                        ids = packed;
                    } else if (p.field == 8) {
                        lats = packed;
                    } else if (p.field == 9) {
                        lons = packed;
                    } else {
                        kv = packed;
                    }
                } else {
                    p.skip();
                }
            }
            if (ids == null) {
                return;
            }
            if (lats == null || lons == null) {
                throw new IOException("Dense nodes without coordinates");
            }
            long id = 0;
            long lat = 0;
            long lon = 0;
            while (ids.more()) {
                if (!lats.more() || !lons.more()) {
                    throw new IOException("Dense node columns differ in length");
                }
                id += Proto.zigzag(ids.varint());
                lat += Proto.zigzag(lats.varint());
                lon += Proto.zigzag(lons.varint());
                int count = 0;
                if (kv != null) {
                    while (kv.more()) {
                        long key = kv.varint();
                        if (key == 0) {
                            break;
                        }
                        if (!kv.more()) {
                            throw new IOException("Dense node tag without a value");
                        }
                        ensureTags(count + 1);
                        tags[count * 2] = string(key);
                        tags[count * 2 + 1] = string(kv.varint());
                        count++;
                    }
                }
                sink.node(id, latE7(lat), lonE7(lon), tags, count);
            }
        }

        private void way(byte[] data, int start, int length) throws IOException {
            long id = 0;
            int kStart = -1;
            int kLength = 0;
            int vStart = -1;
            int vLength = 0;
            int refCount = 0;
            Proto p = new Proto(data, start, length);
            while (p.next()) {
                if (p.field == 1 && p.wire == 0) {
                    id = p.varint();
                } else if (p.field == 2 && p.wire == 2) {
                    kLength = p.length();
                    kStart = p.pos;
                    p.pos += kLength;
                } else if (p.field == 3 && p.wire == 2) {
                    vLength = p.length();
                    vStart = p.pos;
                    p.pos += vLength;
                } else if (p.field == 8 && p.wire == 2) {
                    int len = p.length();
                    Proto r = new Proto(data, p.pos, len);
                    p.pos += len;
                    long ref = 0;
                    while (r.more()) {
                        ref += Proto.zigzag(r.varint());
                        if (refCount == refs.length) {
                            long[] grown = new long[refs.length * 2];
                            System.arraycopy(refs, 0, grown, 0, refCount);
                            refs = grown;
                        }
                        refs[refCount++] = ref;
                    }
                } else {
                    p.skip();
                }
            }
            int count = pairTags(data, kStart, kLength, vStart, vLength);
            sink.way(id, refs, refCount, tags, count);
        }
    }

    /// A bounds-checked protobuf cursor over a slice of a byte array.
    static final class Proto {
        private final byte[] data;
        private final int limit;
        int pos;
        int field;
        int wire;

        Proto(byte[] data, int start, int length) throws IOException {
            if (start < 0 || length < 0 || start + length > data.length || start + length < 0) {
                throw new IOException("Field outside its message");
            }
            this.data = data;
            this.pos = start;
            this.limit = start + length;
        }

        boolean more() {
            return pos < limit;
        }

        boolean next() throws IOException {
            if (pos >= limit) {
                return false;
            }
            long key = varint();
            field = (int) (key >>> 3);
            wire = (int) (key & 7);
            return true;
        }

        long varint() throws IOException {
            long v = 0;
            int shift = 0;
            while (true) {
                if (pos >= limit || shift > 63) {
                    throw new IOException("Truncated varint");
                }
                int b = data[pos++] & 0xff;
                v |= (long) (b & 0x7f) << shift;
                if ((b & 0x80) == 0) {
                    return v;
                }
                shift += 7;
            }
        }

        static long zigzag(long v) {
            return (v >>> 1) ^ -(v & 1);
        }

        int length() throws IOException {
            long len = varint();
            if (len < 0 || len > limit - pos) {
                throw new IOException("Field length past the end of its message");
            }
            return (int) len;
        }

        String string() throws IOException {
            int len = length();
            String s;
            try {
                s = new String(data, pos, len, "UTF-8");
            } catch (UnsupportedEncodingException err) {
                IOException wrapped = new IOException(err.getMessage());
                wrapped.initCause(err);
                throw wrapped;
            }
            pos += len;
            return s;
        }

        void skip() throws IOException {
            switch (wire) {
                case 0:
                    varint();
                    break;
                case 1:
                    advance(8);
                    break;
                case 2:
                    advance(length());
                    break;
                case 5:
                    advance(4);
                    break;
                default:
                    throw new IOException("Unsupported protobuf wire type " + wire);
            }
        }

        private void advance(int n) throws IOException {
            if (n > limit - pos) {
                throw new IOException("Field past the end of its message");
            }
            pos += n;
        }
    }
}
