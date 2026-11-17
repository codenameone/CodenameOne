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
package com.codename1.rbx.place;

import com.codename1.rbx.CFrame;
import com.codename1.rbx.Color3;
import com.codename1.rbx.NumberRange;
import com.codename1.rbx.QueryParams;
import com.codename1.rbx.Ray;
import com.codename1.rbx.UDim;
import com.codename1.rbx.UDim2;
import com.codename1.rbx.Vector2;
import com.codename1.rbx.Vector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads the binary place and model formats, `.rbxl` and `.rbxm`.
///
/// The file is a header and a run of chunks: `META` (file metadata),
/// `SSTR` (shared strings), one `INST` per class (its instances'
/// referents), one `PROP` per class and property (every instance's value,
/// column-wise), `PRNT` (the parent of each instance) and `END`. A chunk is
/// stored raw, LZ4 block compressed, or zstd compressed.
///
/// Integer columns are *interleaved*: for N values of W bytes, the first
/// bytes of all N come first, then all the second bytes, each value big
/// endian. Signed integers are zigzag encoded, referents are deltas of a
/// running sum, and floats are rotated left by one bit so the sign bit is
/// last.
public final class RbxlReader {
    private static final byte[] MAGIC = {'<', 'r', 'o', 'b', 'l', 'o', 'x', '!', (byte) 0x89, (byte) 0xff, 0x0d,
        0x0a, 0x1a, 0x0a};

    private final PlaceFile out = new PlaceFile();
    private final Map<Integer, String> classNames = new HashMap<Integer, String>();
    private final Map<Integer, int[]> classRefs = new HashMap<Integer, int[]>();
    private final Map<Integer, PlaceNode> nodes = new HashMap<Integer, PlaceNode>();
    private final List<byte[]> sharedStrings = new ArrayList<byte[]>();
    private final List<Object[]> refFixups = new ArrayList<Object[]>();

    private RbxlReader() {
    }

    public static boolean isBinary(byte[] d) {
        if (d.length < MAGIC.length) {
            return false;
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (d[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    public static PlaceFile read(byte[] d) {
        if (!isBinary(d)) {
            throw new IllegalArgumentException("Not a binary place file");
        }
        RbxlReader r = new RbxlReader();
        // magic(14) version(2) classes(4) instances(4) reserved(8)
        int p = MAGIC.length + 2 + 4 + 4 + 8;
        List<Integer> order = new ArrayList<Integer>();
        while (p + 16 <= d.length) {
            String name = new String(new char[] {(char) d[p], (char) d[p + 1], (char) d[p + 2], (char) d[p + 3]});
            int clen = le32(d, p + 4);
            int ulen = le32(d, p + 8);
            p += 16;
            checkChunkSizes(d.length - p, clen, ulen, d, p);
            byte[] body;
            if (clen == 0) {
                body = new byte[ulen];
                System.arraycopy(d, p, body, 0, ulen);
                p += ulen;
            } else {
                body = decompress(d, p, clen, ulen);
                p += clen;
            }
            Buf b = new Buf(body);
            if ("INST".equals(name)) {
                r.inst(b, order);
            } else if ("PROP".equals(name)) {
                r.prop(b);
            } else if ("PRNT".equals(name)) {
                r.prnt(b);
            } else if ("SSTR".equals(name)) {
                r.sstr(b);
            } else if (name.startsWith("END")) {
                break;
            } else if (!"META".equals(name) && !"SIGN".equals(name)) {
                r.out.readerNotes.add("Skipped unknown chunk " + name);
            }
        }
        for (Object[] f : r.refFixups) {
            ((PlaceNode) f[0]).put((String) f[1], "Ref", r.nodes.get((Integer) f[2]));
        }
        return r.out;
    }

    /// The largest chunk a place file may declare once decompressed. Real
    /// places stay far below this; the cap exists because `ulen` comes from
    /// untrusted bytes and sizes an allocation before anything is decoded.
    static final int MAX_CHUNK_BYTES = 64 * 1024 * 1024;

    /// LZ4 emits at most 255 output bytes per input byte of a match length
    /// extension, so a block can never expand past this ratio (plus slack).
    private static final int LZ4_MAX_RATIO = 255;

    /// Rejects a chunk header whose sizes cannot be honest, before any of
    /// them sizes an array: a tiny file claiming a 2 GB chunk would otherwise
    /// exhaust the server's heap on `new byte[ulen]`.
    static void checkChunkSizes(int remaining, int clen, int ulen, byte[] d, int p) {
        if (clen < 0 || ulen < 0 || clen > remaining || ulen > MAX_CHUNK_BYTES) {
            throw new IllegalArgumentException("Corrupt binary place file: bad chunk size");
        }
        if (clen == 0) {
            if (ulen > remaining) {
                throw new IllegalArgumentException("Corrupt binary place file: chunk overruns file");
            }
            return;
        }
        if (!isZstd(d, p, clen) && (long) ulen > (long) clen * LZ4_MAX_RATIO + 16) {
            // Zstandard has no small fixed ratio (an RLE block expands a few
            // bytes to 128 KB), so it is held to the absolute cap only.
            throw new IllegalArgumentException("Corrupt binary place file: impossible LZ4 ratio");
        }
    }

    private static boolean isZstd(byte[] d, int off, int len) {
        return len >= 4 && (d[off] & 0xff) == 0x28 && (d[off + 1] & 0xff) == 0xb5 && (d[off + 2] & 0xff) == 0x2f
                && (d[off + 3] & 0xff) == 0xfd;
    }

    private static byte[] decompress(byte[] d, int off, int len, int ulen) {
        if (len >= 4 && (d[off] & 0xff) == 0x28 && (d[off + 1] & 0xff) == 0xb5 && (d[off + 2] & 0xff) == 0x2f
                && (d[off + 3] & 0xff) == 0xfd) {
            return Zstd.decompress(d, off, len, ulen);
        }
        return Lz4.decompress(d, off, len, ulen);
    }

    private void inst(Buf b, List<Integer> order) {
        int classId = b.i32();
        String cls = b.str();
        boolean isService = b.u8() != 0;
        int count = b.i32();
        int[] refs = b.refs(count);
        classNames.put(Integer.valueOf(classId), cls);
        classRefs.put(Integer.valueOf(classId), refs);
        order.add(Integer.valueOf(classId));
        for (int i = 0; i < count; i++) {
            PlaceNode n = new PlaceNode(cls);
            n.referent = String.valueOf(refs[i]);
            nodes.put(Integer.valueOf(refs[i]), n);
        }
        if (isService) {
            for (int i = 0; i < count; i++) {
                nodes.get(Integer.valueOf(refs[i])).service = b.u8() != 0;
            }
        }
    }

    private void sstr(Buf b) {
        b.i32(); // version
        int count = b.i32();
        for (int i = 0; i < count; i++) {
            b.skip(16); // md5 of the value, unused
            sharedStrings.add(b.bytes(b.i32()));
        }
    }

    private void prnt(Buf b) {
        b.u8(); // version
        int count = b.i32();
        int[] child = b.refs(count);
        int[] parent = b.refs(count);
        for (int i = 0; i < count; i++) {
            PlaceNode c = nodes.get(Integer.valueOf(child[i]));
            if (c == null) {
                continue;
            }
            PlaceNode par = parent[i] < 0 ? null : nodes.get(Integer.valueOf(parent[i]));
            if (par == null) {
                out.roots.add(c);
            } else {
                par.add(c);
            }
        }
    }

    private void prop(Buf b) {
        Integer classId = Integer.valueOf(b.i32());
        String name = b.str();
        int type = b.u8();
        int[] refs = classRefs.get(classId);
        if (refs == null) {
            out.readerNotes.add("Property " + name + " for an undeclared class");
            return;
        }
        int n = refs.length;
        PlaceNode[] targets = new PlaceNode[n];
        for (int i = 0; i < n; i++) {
            targets[i] = nodes.get(Integer.valueOf(refs[i]));
        }
        String cls = classNames.get(classId);
        try {
            values(b, type, name, targets);
        } catch (IllegalArgumentException e) {
            out.readerNotes.add(e.getMessage() + " (" + cls + "." + name + ")");
        } catch (ArrayIndexOutOfBoundsException e) {
            out.readerNotes.add("Truncated " + cls + "." + name);
        }
    }

    private void values(Buf b, int type, String name, PlaceNode[] t) {
        int n = t.length;
        switch (type) {
            case 0x01: {
                // The binary format has one string type for text, scripts and
                // bytes alike; the property says which it is.
                String as = binaryStringType(name);
                for (PlaceNode node : t) {
                    byte[] raw = b.bytes(b.i32());
                    if ("BinaryString".equals(as)) {
                        node.put(name, as, raw);
                    } else {
                        node.put(name, as, RbxlxReader.utf8(raw, 0, raw.length));
                    }
                }
                return;
            }
            case 0x02:
                for (PlaceNode node : t) {
                    node.put(name, "bool", Boolean.valueOf(b.u8() != 0));
                }
                return;
            case 0x03: {
                int[] v = b.ints(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "int", Double.valueOf(v[i]));
                }
                return;
            }
            case 0x04: {
                float[] v = b.floats(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "float", Double.valueOf(v[i]));
                }
                return;
            }
            case 0x05:
                for (PlaceNode node : t) {
                    node.put(name, "double", Double.valueOf(Double.longBitsToDouble(b.le64())));
                }
                return;
            case 0x06: {
                float[] s = b.floats(n);
                int[] o = b.ints(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "UDim", new UDim(s[i], o[i]));
                }
                return;
            }
            case 0x07: {
                float[] xs = b.floats(n);
                float[] ys = b.floats(n);
                int[] xo = b.ints(n);
                int[] yo = b.ints(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "UDim2", new UDim2(xs[i], xo[i], ys[i], yo[i]));
                }
                return;
            }
            case 0x08:
                for (PlaceNode node : t) {
                    Vector3 o = new Vector3(b.f32(), b.f32(), b.f32());
                    node.put(name, "Ray", new Ray(o, new Vector3(b.f32(), b.f32(), b.f32())));
                }
                return;
            case 0x09:
                for (PlaceNode node : t) {
                    node.put(name, "Faces", Integer.valueOf(b.u8()));
                }
                return;
            case 0x0A:
                for (PlaceNode node : t) {
                    node.put(name, "Axes", Integer.valueOf(b.u8()));
                }
                return;
            case 0x0B: {
                int[] v = b.uints(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "BrickColor", Integer.valueOf(v[i]));
                }
                return;
            }
            case 0x0C: {
                float[] r = b.floats(n);
                float[] g = b.floats(n);
                float[] bl = b.floats(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "Color3", new Color3(r[i], g[i], bl[i]));
                }
                return;
            }
            case 0x0D: {
                float[] x = b.floats(n);
                float[] y = b.floats(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "Vector2", new Vector2(x[i], y[i]));
                }
                return;
            }
            case 0x0E: {
                float[] x = b.floats(n);
                float[] y = b.floats(n);
                float[] z = b.floats(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "Vector3", new Vector3(x[i], y[i], z[i]));
                }
                return;
            }
            case 0x10: {
                CFrame[] cf = cframes(b, n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "CoordinateFrame", cf[i]);
                }
                return;
            }
            case 0x12: {
                int[] v = b.uints(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "token", Integer.valueOf(v[i]));
                }
                return;
            }
            case 0x13: {
                int[] v = b.refs(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "Ref", null);
                    if (v[i] >= 0) {
                        refFixups.add(new Object[] {t[i], name, Integer.valueOf(v[i])});
                    }
                }
                return;
            }
            case 0x14: {
                for (PlaceNode node : t) {
                    node.put(name, "Vector3int16", new Vector3((short) b.le16(), (short) b.le16(), (short) b.le16()));
                }
                return;
            }
            case 0x15:
                for (PlaceNode node : t) {
                    int k = b.i32();
                    double[] v = new double[k * 3];
                    for (int j = 0; j < v.length; j++) {
                        v[j] = b.f32();
                    }
                    node.put(name, "NumberSequence", RbxlxReader.numberSequence(v));
                }
                return;
            case 0x16:
                for (PlaceNode node : t) {
                    int k = b.i32();
                    double[] v = new double[k * 5];
                    for (int j = 0; j < v.length; j++) {
                        v[j] = b.f32();
                    }
                    node.put(name, "ColorSequence", RbxlxReader.colorSequence(v));
                }
                return;
            case 0x17:
                for (PlaceNode node : t) {
                    node.put(name, "NumberRange", new NumberRange(b.f32(), b.f32()));
                }
                return;
            case 0x18: {
                float[] x0 = b.floats(n);
                float[] y0 = b.floats(n);
                float[] x1 = b.floats(n);
                float[] y1 = b.floats(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "Rect2D", new QueryParams.Rect(x0[i], y0[i], x1[i], y1[i]));
                }
                return;
            }
            case 0x19:
                for (PlaceNode node : t) {
                    if (b.u8() == 0) {
                        node.put(name, "PhysicalProperties", null);
                    } else {
                        node.put(name, "PhysicalProperties",
                                new QueryParams.PhysicalProperties(b.f32(), b.f32(), b.f32(), b.f32(), b.f32()));
                    }
                }
                return;
            case 0x1A: {
                byte[] r = b.bytes(n);
                byte[] g = b.bytes(n);
                byte[] bl = b.bytes(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "Color3uint8", Color3.rgb(r[i] & 0xff, g[i] & 0xff, bl[i] & 0xff));
                }
                return;
            }
            case 0x1B: {
                long[] v = b.longs(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "int64", Double.valueOf(v[i]));
                }
                return;
            }
            case 0x1C: {
                int[] v = b.uints(n);
                for (int i = 0; i < n; i++) {
                    byte[] s = v[i] >= 0 && v[i] < sharedStrings.size() ? sharedStrings.get(v[i]) : new byte[0];
                    t[i].put(name, "SharedString", s);
                }
                return;
            }
            case 0x1E: {
                int inner = b.u8();
                if (inner != 0x10) {
                    throw new IllegalArgumentException("OptionalCFrame of type " + inner);
                }
                CFrame[] cf = cframes(b, n);
                int boolType = b.u8();
                for (int i = 0; i < n; i++) {
                    boolean has = boolType != 0x02 || b.u8() != 0;
                    t[i].put(name, "OptionalCoordinateFrame", has ? cf[i] : null);
                }
                return;
            }
            case 0x1F: {
                byte[] raw = b.bytes(n * 16);
                for (int i = 0; i < n; i++) {
                    StringBuilder h = new StringBuilder(32);
                    for (int j = 0; j < 16; j++) {
                        int v = raw[j * n + i] & 0xff;
                        h.append(Character.forDigit(v >> 4, 16)).append(Character.forDigit(v & 15, 16));
                    }
                    t[i].put(name, "UniqueId", h.toString());
                }
                return;
            }
            case 0x20:
                for (PlaceNode node : t) {
                    String fam = b.str();
                    int weight = b.le16();
                    int style = b.u8();
                    String cached = b.str();
                    node.put(name, "Font", new String[] {fam, String.valueOf(weight),
                            style == 1 ? "Italic" : "Normal", cached});
                }
                return;
            case 0x21: {
                long[] v = b.longsRaw(n);
                for (int i = 0; i < n; i++) {
                    t[i].put(name, "SecurityCapabilities", Long.toString(v[i]));
                }
                return;
            }
            default:
                throw new IllegalArgumentException("Unsupported binary property type 0x" + Integer.toHexString(type));
        }
    }

    static String binaryStringType(String prop) {
        if ("Source".equals(prop)) {
            return "ProtectedString";
        }
        if ("AttributesSerialize".equals(prop) || "Tags".equals(prop) || "AttributesReplicate".equals(prop)
                || "SmoothGrid".equals(prop) || "PhysicsGrid".equals(prop) || "MaterialColors".equals(prop)
                || "PhysicalConfigData".equals(prop) || "ModelMeshData".equals(prop)) {
            return "BinaryString";
        }
        for (String c : PlaceSaver.CONTENT) {
            if (c.equals(prop)) {
                return "Content";
            }
        }
        return "string";
    }

    /// CFrames: per value a rotation id (0 = nine raw floats follow, else
    /// one of 24 axis-aligned rotations), then the positions as three
    /// interleaved float columns.
    private static CFrame[] cframes(Buf b, int n) {
        double[][] rot = new double[n][];
        for (int i = 0; i < n; i++) {
            int id = b.u8();
            if (id == 0) {
                double[] m = new double[9];
                for (int k = 0; k < 9; k++) {
                    m[k] = b.f32();
                }
                rot[i] = m;
            } else {
                rot[i] = specialRotation(id);
            }
        }
        float[] x = b.floats(n);
        float[] y = b.floats(n);
        float[] z = b.floats(n);
        CFrame[] out = new CFrame[n];
        for (int i = 0; i < n; i++) {
            double[] m = rot[i];
            out[i] = new CFrame(x[i], y[i], z[i], m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8]);
        }
        return out;
    }

    /// The axis-aligned rotation `id` names. `id - 1` is `6 * a + b`, where
    /// `a` and `b` are the directions (+X, +Y, +Z, -X, -Y, -Z) of the first
    /// two rows of the matrix; the third is their cross product.
    static double[] specialRotation(int id) {
        int k = id - 1;
        int a = k / 6;
        int c = k % 6;
        if (a > 5 || a % 3 == c % 3) {
            throw new IllegalArgumentException("Bad CFrame rotation id " + id);
        }
        double[] r0 = axis(a);
        double[] r1 = axis(c);
        double[] r2 = {r0[1] * r1[2] - r0[2] * r1[1], r0[2] * r1[0] - r0[0] * r1[2], r0[0] * r1[1] - r0[1] * r1[0]};
        return new double[] {r0[0], r0[1], r0[2], r1[0], r1[1], r1[2], r2[0], r2[1], r2[2]};
    }

    /// The inverse of [#specialRotation]: the id for an axis-aligned matrix,
    /// 0 when it is not one.
    static int rotationId(double[] m) {
        int a = axisOf(m[0], m[1], m[2]);
        int c = axisOf(m[3], m[4], m[5]);
        if (a < 0 || c < 0 || a % 3 == c % 3) {
            return 0;
        }
        double[] expect = specialRotation(6 * a + c + 1);
        for (int i = 6; i < 9; i++) {
            if (Math.abs(expect[i] - m[i]) > 1e-9) {
                return 0;
            }
        }
        return 6 * a + c + 1;
    }

    private static int axisOf(double x, double y, double z) {
        double[] v = {x, y, z};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(Math.abs(v[i]) - 1) < 1e-9 && Math.abs(v[(i + 1) % 3]) < 1e-9
                    && Math.abs(v[(i + 2) % 3]) < 1e-9) {
                return v[i] > 0 ? i : i + 3;
            }
        }
        return -1;
    }

    private static double[] axis(int a) {
        double[] v = new double[3];
        v[a % 3] = a < 3 ? 1 : -1;
        return v;
    }

    static int le32(byte[] d, int p) {
        return (d[p] & 0xff) | (d[p + 1] & 0xff) << 8 | (d[p + 2] & 0xff) << 16 | (d[p + 3] & 0xff) << 24;
    }

    /// A read cursor over one decompressed chunk.
    static final class Buf {
        final byte[] d;
        int p;

        Buf(byte[] d) {
            this.d = d;
        }

        int u8() {
            return d[p++] & 0xff;
        }

        void skip(int n) {
            p += n;
        }

        int le16() {
            int v = (d[p] & 0xff) | (d[p + 1] & 0xff) << 8;
            p += 2;
            return v;
        }

        int i32() {
            int v = le32(d, p);
            p += 4;
            return v;
        }

        long le64() {
            long lo = i32() & 0xffffffffL;
            long hi = i32() & 0xffffffffL;
            return lo | hi << 32;
        }

        float f32() {
            return Float.intBitsToFloat(i32());
        }

        byte[] bytes(int n) {
            if (n < 0 || p + n > d.length) {
                throw new ArrayIndexOutOfBoundsException(n);
            }
            byte[] o = new byte[n];
            System.arraycopy(d, p, o, 0, n);
            p += n;
            return o;
        }

        String str() {
            byte[] b = bytes(i32());
            return RbxlxReader.utf8(b, 0, b.length);
        }

        /// N interleaved big-endian 32-bit words.
        int[] uints(int n) {
            if (p + n * 4 > d.length) {
                throw new ArrayIndexOutOfBoundsException(n);
            }
            int[] o = new int[n];
            for (int i = 0; i < n; i++) {
                o[i] = (d[p + i] & 0xff) << 24 | (d[p + n + i] & 0xff) << 16 | (d[p + 2 * n + i] & 0xff) << 8
                        | (d[p + 3 * n + i] & 0xff);
            }
            p += n * 4;
            return o;
        }

        int[] ints(int n) {
            int[] o = uints(n);
            for (int i = 0; i < n; i++) {
                o[i] = (o[i] >>> 1) ^ -(o[i] & 1);
            }
            return o;
        }

        int[] refs(int n) {
            int[] o = ints(n);
            for (int i = 1; i < n; i++) {
                o[i] += o[i - 1];
            }
            return o;
        }

        float[] floats(int n) {
            int[] o = uints(n);
            float[] f = new float[n];
            for (int i = 0; i < n; i++) {
                int v = o[i];
                f[i] = Float.intBitsToFloat(v >>> 1 | v << 31);
            }
            return f;
        }

        long[] longsRaw(int n) {
            if (p + n * 8 > d.length) {
                throw new ArrayIndexOutOfBoundsException(n);
            }
            long[] o = new long[n];
            for (int i = 0; i < n; i++) {
                long v = 0;
                for (int j = 0; j < 8; j++) {
                    v = v << 8 | (d[p + j * n + i] & 0xff);
                }
                o[i] = v;
            }
            p += n * 8;
            return o;
        }

        long[] longs(int n) {
            long[] o = longsRaw(n);
            for (int i = 0; i < n; i++) {
                o[i] = (o[i] >>> 1) ^ -(o[i] & 1);
            }
            return o;
        }
    }
}
