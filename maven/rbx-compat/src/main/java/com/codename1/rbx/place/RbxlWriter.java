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
import com.codename1.rbx.Sequence;
import com.codename1.rbx.UDim;
import com.codename1.rbx.UDim2;
import com.codename1.rbx.Vector2;
import com.codename1.rbx.Vector3;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Writes a [PlaceFile] in the binary format, `.rbxl` / `.rbxm`; the layout
/// [RbxlReader] documents. Chunks are stored uncompressed.
public final class RbxlWriter {
    private static final Map<String, Integer> TYPE_IDS = new HashMap<String, Integer>();

    static {
        String[] names = {"string", "bool", "int", "float", "double", "UDim", "UDim2", "Ray", "Faces", "Axes",
            "BrickColor", "Color3", "Vector2", "Vector3", null, "CoordinateFrame", null, "token", "Ref",
            "Vector3int16", "NumberSequence", "ColorSequence", "NumberRange", "Rect2D", "PhysicalProperties",
            "Color3uint8", "int64", "SharedString", null, "OptionalCoordinateFrame", "UniqueId", "Font",
            "SecurityCapabilities"};
        for (int i = 0; i < names.length; i++) {
            if (names[i] != null) {
                TYPE_IDS.put(names[i], Integer.valueOf(i + 1));
            }
        }
        TYPE_IDS.put("ProtectedString", Integer.valueOf(1));
        TYPE_IDS.put("Content", Integer.valueOf(1));
        TYPE_IDS.put("ContentId", Integer.valueOf(1));
        TYPE_IDS.put("BinaryString", Integer.valueOf(1));
    }

    private final Map<PlaceNode, Integer> ids = new IdentityHashMap<PlaceNode, Integer>();
    private final List<byte[]> shared = new ArrayList<byte[]>();
    private final ByteArrayOutputStream file = new ByteArrayOutputStream();

    private RbxlWriter() {
    }

    public static byte[] write(PlaceFile f) {
        RbxlWriter w = new RbxlWriter();
        List<PlaceNode> all = f.all();
        Map<String, List<PlaceNode>> byClass = new LinkedHashMap<String, List<PlaceNode>>();
        for (PlaceNode n : all) {
            w.ids.put(n, Integer.valueOf(w.ids.size()));
            List<PlaceNode> l = byClass.get(n.className);
            if (l == null) {
                l = new ArrayList<PlaceNode>();
                byClass.put(n.className, l);
            }
            l.add(n);
        }
        Out h = new Out();
        h.raw(new byte[] {'<', 'r', 'o', 'b', 'l', 'o', 'x', '!', (byte) 0x89, (byte) 0xff, 0x0d, 0x0a, 0x1a,
                0x0a});
        h.le16(0);
        h.i32(byClass.size());
        h.i32(all.size());
        h.raw(new byte[8]);
        w.file.write(h.bytes(), 0, h.size());

        Out meta = new Out();
        meta.i32(1);
        meta.str("ExplicitAutoJoints");
        meta.str("true");
        w.chunk("META", meta);

        int classId = 0;
        List<Out> props = new ArrayList<Out>();
        for (Map.Entry<String, List<PlaceNode>> e : byClass.entrySet()) {
            List<PlaceNode> nodes = e.getValue();
            Out c = new Out();
            c.i32(classId);
            c.str(e.getKey());
            boolean svc = false;
            for (PlaceNode n : nodes) {
                svc |= n.service;
            }
            c.u8(svc ? 1 : 0);
            c.i32(nodes.size());
            int[] refs = new int[nodes.size()];
            for (int i = 0; i < refs.length; i++) {
                refs[i] = w.ids.get(nodes.get(i)).intValue();
            }
            c.refs(refs);
            if (svc) {
                for (PlaceNode n : nodes) {
                    c.u8(n.service ? 1 : 0);
                }
            }
            w.chunk("INST", c);
            w.classProps(classId, nodes, props);
            classId++;
        }
        if (!w.shared.isEmpty()) {
            Out s = new Out();
            s.i32(0);
            s.i32(w.shared.size());
            for (byte[] v : w.shared) {
                s.raw(new byte[16]);
                s.i32(v.length);
                s.raw(v);
            }
            w.chunk("SSTR", s);
        }
        for (Out p : props) {
            w.chunk("PROP", p);
        }
        Out pr = new Out();
        pr.u8(0);
        pr.i32(all.size());
        int[] child = new int[all.size()];
        int[] parent = new int[all.size()];
        for (int i = 0; i < child.length; i++) {
            PlaceNode n = all.get(i);
            child[i] = w.ids.get(n).intValue();
            parent[i] = n.parent == null ? -1 : w.ids.get(n.parent).intValue();
        }
        pr.refs(child);
        pr.refs(parent);
        w.chunk("PRNT", pr);
        Out end = new Out();
        end.raw(new byte[] {'<', '/', 'r', 'o', 'b', 'l', 'o', 'x', '>'});
        w.chunk("END\0", end);
        return w.file.toByteArray();
    }

    private void chunk(String name, Out body) {
        Out h = new Out();
        for (int i = 0; i < 4; i++) {
            h.u8(name.charAt(i));
        }
        h.i32(0);
        h.i32(body.size());
        h.i32(0);
        file.write(h.bytes(), 0, h.size());
        file.write(body.bytes(), 0, body.size());
    }

    /// One PROP chunk per property any instance of the class carries. An
    /// instance without it is written with the type's zero value, since a
    /// column has one entry per instance.
    private void classProps(int classId, List<PlaceNode> nodes, List<Out> chunks) {
        Map<String, String> types = new LinkedHashMap<String, String>();
        for (PlaceNode n : nodes) {
            for (Map.Entry<String, PlaceValue> e : n.props.entrySet()) {
                if (!types.containsKey(e.getKey()) && TYPE_IDS.containsKey(e.getValue().type)) {
                    types.put(e.getKey(), e.getValue().type);
                }
            }
        }
        for (Map.Entry<String, String> e : types.entrySet()) {
            String type = e.getValue();
            Object[] vals = new Object[nodes.size()];
            for (int i = 0; i < vals.length; i++) {
                PlaceValue pv = nodes.get(i).props.get(e.getKey());
                vals[i] = pv != null && TYPE_IDS.get(pv.type).equals(TYPE_IDS.get(type)) ? pv.value : zero(type);
            }
            Out c = new Out();
            c.i32(classId);
            c.str(e.getKey());
            int id = TYPE_IDS.get(type).intValue();
            c.u8(id);
            column(c, id, vals);
            chunks.add(c);
        }
    }

    private static Object zero(String type) {
        switch (type) {
            case "string":
            case "ProtectedString":
            case "Content":
            case "ContentId":
            case "UniqueId":
            case "SecurityCapabilities":
                return "";
            case "BinaryString":
            case "SharedString":
                return new byte[0];
            case "bool":
                return Boolean.FALSE;
            case "token":
            case "BrickColor":
            case "Faces":
            case "Axes":
                return Integer.valueOf(0);
            case "Color3":
            case "Color3uint8":
                return new Color3(0, 0, 0);
            case "Vector3":
            case "Vector3int16":
                return Vector3.ZERO;
            case "Vector2":
                return Vector2.ZERO;
            case "CoordinateFrame":
                return CFrame.IDENTITY;
            case "UDim":
                return new UDim(0, 0);
            case "UDim2":
                return new UDim2(0, 0, 0, 0);
            case "Ray":
                return new Ray(Vector3.ZERO, Vector3.ZERO);
            case "NumberRange":
                return new NumberRange(0, 0);
            case "NumberSequence":
                return Sequence.number(0);
            case "ColorSequence":
                return Sequence.color(new Color3(0, 0, 0));
            case "Rect2D":
                return new QueryParams.Rect(0, 0, 0, 0);
            case "Font":
                return new String[] {"", "400", "Normal", ""};
            default:
                return type.endsWith("int") || "float".equals(type) || "double".equals(type) || "int64".equals(type)
                        ? Double.valueOf(0) : null;
        }
    }

    private static double d(Object v) {
        return v instanceof Double ? ((Double) v).doubleValue() : 0;
    }

    private static int in(Object v) {
        return v instanceof Integer ? ((Integer) v).intValue() : 0;
    }

    private void column(Out c, int type, Object[] v) {
        int n = v.length;
        switch (type) {
            case 0x01:
                for (Object o : v) {
                    if (o instanceof byte[]) {
                        byte[] b = (byte[]) o;
                        c.i32(b.length);
                        c.raw(b);
                    } else {
                        c.str(o instanceof String ? (String) o : "");
                    }
                }
                return;
            case 0x02:
                for (Object o : v) {
                    c.u8(Boolean.TRUE.equals(o) ? 1 : 0);
                }
                return;
            case 0x03: {
                int[] a = new int[n];
                for (int i = 0; i < n; i++) {
                    a[i] = (int) d(v[i]);
                }
                c.ints(a);
                return;
            }
            case 0x04: {
                float[] a = new float[n];
                for (int i = 0; i < n; i++) {
                    a[i] = (float) d(v[i]);
                }
                c.floats(a);
                return;
            }
            case 0x05:
                for (Object o : v) {
                    c.le64(Double.doubleToLongBits(d(o)));
                }
                return;
            case 0x06: {
                float[] s = new float[n];
                int[] o = new int[n];
                for (int i = 0; i < n; i++) {
                    UDim u = v[i] instanceof UDim ? (UDim) v[i] : new UDim(0, 0);
                    s[i] = (float) u.scale;
                    o[i] = (int) u.offset;
                }
                c.floats(s);
                c.ints(o);
                return;
            }
            case 0x07: {
                float[] xs = new float[n];
                float[] ys = new float[n];
                int[] xo = new int[n];
                int[] yo = new int[n];
                for (int i = 0; i < n; i++) {
                    UDim2 u = v[i] instanceof UDim2 ? (UDim2) v[i] : new UDim2(0, 0, 0, 0);
                    xs[i] = (float) u.x.scale;
                    ys[i] = (float) u.y.scale;
                    xo[i] = (int) u.x.offset;
                    yo[i] = (int) u.y.offset;
                }
                c.floats(xs);
                c.floats(ys);
                c.ints(xo);
                c.ints(yo);
                return;
            }
            case 0x08:
                for (Object o : v) {
                    Ray r = o instanceof Ray ? (Ray) o : new Ray(Vector3.ZERO, Vector3.ZERO);
                    c.f32(r.origin.x);
                    c.f32(r.origin.y);
                    c.f32(r.origin.z);
                    c.f32(r.direction.x);
                    c.f32(r.direction.y);
                    c.f32(r.direction.z);
                }
                return;
            case 0x09:
            case 0x0A:
                for (Object o : v) {
                    c.u8(in(o));
                }
                return;
            case 0x0B:
            case 0x12: {
                int[] a = new int[n];
                for (int i = 0; i < n; i++) {
                    a[i] = in(v[i]);
                }
                c.uints(a);
                return;
            }
            case 0x0C: {
                float[][] ch = new float[3][n];
                for (int i = 0; i < n; i++) {
                    Color3 col = v[i] instanceof Color3 ? (Color3) v[i] : new Color3(0, 0, 0);
                    ch[0][i] = (float) col.r;
                    ch[1][i] = (float) col.g;
                    ch[2][i] = (float) col.b;
                }
                for (float[] a : ch) {
                    c.floats(a);
                }
                return;
            }
            case 0x0D: {
                float[][] ch = new float[2][n];
                for (int i = 0; i < n; i++) {
                    Vector2 p = v[i] instanceof Vector2 ? (Vector2) v[i] : Vector2.ZERO;
                    ch[0][i] = (float) p.x;
                    ch[1][i] = (float) p.y;
                }
                for (float[] a : ch) {
                    c.floats(a);
                }
                return;
            }
            case 0x0E:
                vectors(c, v);
                return;
            case 0x10:
                cframes(c, v);
                return;
            case 0x13: {
                int[] a = new int[n];
                for (int i = 0; i < n; i++) {
                    Integer id = v[i] instanceof PlaceNode ? ids.get(v[i]) : null;
                    a[i] = id == null ? -1 : id.intValue();
                }
                c.refs(a);
                return;
            }
            case 0x14:
                for (Object o : v) {
                    Vector3 p = o instanceof Vector3 ? (Vector3) o : Vector3.ZERO;
                    c.le16((int) p.x);
                    c.le16((int) p.y);
                    c.le16((int) p.z);
                }
                return;
            case 0x15:
            case 0x16:
                for (Object o : v) {
                    Sequence s = o instanceof Sequence ? (Sequence) o
                            : type == 0x15 ? Sequence.number(0) : Sequence.color(new Color3(0, 0, 0));
                    c.i32(s.times.length);
                    for (int k = 0; k < s.times.length; k++) {
                        c.f32(s.times[k]);
                        Object kv = s.values[k];
                        if (kv instanceof Color3) {
                            Color3 col = (Color3) kv;
                            c.f32(col.r);
                            c.f32(col.g);
                            c.f32(col.b);
                        } else {
                            c.f32(d(kv));
                        }
                        c.f32(s.envelopes[k]);
                    }
                }
                return;
            case 0x17:
                for (Object o : v) {
                    NumberRange r = o instanceof NumberRange ? (NumberRange) o : new NumberRange(0, 0);
                    c.f32(r.min);
                    c.f32(r.max);
                }
                return;
            case 0x18: {
                float[][] ch = new float[4][n];
                for (int i = 0; i < n; i++) {
                    QueryParams.Rect r = v[i] instanceof QueryParams.Rect ? (QueryParams.Rect) v[i]
                            : new QueryParams.Rect(0, 0, 0, 0);
                    ch[0][i] = (float) r.min.x;
                    ch[1][i] = (float) r.min.y;
                    ch[2][i] = (float) r.max.x;
                    ch[3][i] = (float) r.max.y;
                }
                for (float[] a : ch) {
                    c.floats(a);
                }
                return;
            }
            case 0x19:
                for (Object o : v) {
                    if (o instanceof QueryParams.PhysicalProperties) {
                        QueryParams.PhysicalProperties p = (QueryParams.PhysicalProperties) o;
                        c.u8(1);
                        c.f32(p.density);
                        c.f32(p.friction);
                        c.f32(p.elasticity);
                        c.f32(p.frictionWeight);
                        c.f32(p.elasticityWeight);
                    } else {
                        c.u8(0);
                    }
                }
                return;
            case 0x1A: {
                byte[][] ch = new byte[3][n];
                for (int i = 0; i < n; i++) {
                    Color3 col = v[i] instanceof Color3 ? (Color3) v[i] : new Color3(0, 0, 0);
                    ch[0][i] = (byte) channel(col.r);
                    ch[1][i] = (byte) channel(col.g);
                    ch[2][i] = (byte) channel(col.b);
                }
                for (byte[] a : ch) {
                    c.raw(a);
                }
                return;
            }
            case 0x1B: {
                long[] a = new long[n];
                for (int i = 0; i < n; i++) {
                    long x = (long) d(v[i]);
                    a[i] = (x << 1) ^ (x >> 63);
                }
                c.longs(a);
                return;
            }
            case 0x1C: {
                int[] a = new int[n];
                for (int i = 0; i < n; i++) {
                    a[i] = sharedIndex(v[i] instanceof byte[] ? (byte[]) v[i] : new byte[0]);
                }
                c.uints(a);
                return;
            }
            case 0x1E: {
                c.u8(0x10);
                Object[] cf = new Object[n];
                for (int i = 0; i < n; i++) {
                    cf[i] = v[i] instanceof CFrame ? v[i] : CFrame.IDENTITY;
                }
                cframes(c, cf);
                c.u8(0x02);
                for (Object o : v) {
                    c.u8(o instanceof CFrame ? 1 : 0);
                }
                return;
            }
            case 0x1F: {
                byte[] raw = new byte[n * 16];
                for (int i = 0; i < n; i++) {
                    String h = v[i] instanceof String ? (String) v[i] : "";
                    for (int j = 0; j < 16 && j * 2 + 1 < h.length(); j++) {
                        raw[j * n + i] = (byte) Integer.parseInt(h.substring(j * 2, j * 2 + 2), 16);
                    }
                }
                c.raw(raw);
                return;
            }
            case 0x20:
                for (Object o : v) {
                    String[] f = o instanceof String[] ? (String[]) o : new String[] {"", "400", "Normal", ""};
                    c.str(f[0]);
                    c.le16(parseInt(f[1], 400));
                    c.u8("Italic".equals(f[2]) ? 1 : 0);
                    c.str(f[3]);
                }
                return;
            case 0x21: {
                long[] a = new long[n];
                for (int i = 0; i < n; i++) {
                    a[i] = v[i] instanceof String && ((String) v[i]).length() > 0 ? Long.parseLong((String) v[i]) : 0;
                }
                c.longs(a);
                return;
            }
            default:
                throw new IllegalArgumentException("Unwritable type " + type);
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private int sharedIndex(byte[] b) {
        for (int i = 0; i < shared.size(); i++) {
            if (Arrays.equals(shared.get(i), b)) {
                return i;
            }
        }
        shared.add(b);
        return shared.size() - 1;
    }

    private static int channel(double c) {
        long v = Math.round(c * 255);
        return (int) Math.max(0, Math.min(255, v));
    }

    private static void vectors(Out c, Object[] v) {
        int n = v.length;
        float[][] ch = new float[3][n];
        for (int i = 0; i < n; i++) {
            Vector3 p = v[i] instanceof Vector3 ? (Vector3) v[i] : Vector3.ZERO;
            ch[0][i] = (float) p.x;
            ch[1][i] = (float) p.y;
            ch[2][i] = (float) p.z;
        }
        for (float[] a : ch) {
            c.floats(a);
        }
    }

    private static void cframes(Out c, Object[] v) {
        Object[] pos = new Object[v.length];
        for (int i = 0; i < v.length; i++) {
            CFrame f = v[i] instanceof CFrame ? (CFrame) v[i] : CFrame.IDENTITY;
            double[] m = {f.r00, f.r01, f.r02, f.r10, f.r11, f.r12, f.r20, f.r21, f.r22};
            int id = RbxlReader.rotationId(m);
            c.u8(id);
            if (id == 0) {
                for (double x : m) {
                    c.f32(x);
                }
            }
            pos[i] = new Vector3(f.x, f.y, f.z);
        }
        vectors(c, pos);
    }

    /// A growable little-endian byte buffer with the format's column writers.
    static final class Out {
        private byte[] b = new byte[256];
        private int n;

        int size() {
            return n;
        }

        byte[] bytes() {
            return b;
        }

        private void room(int k) {
            if (n + k > b.length) {
                byte[] nb = new byte[Math.max(b.length * 2, n + k)];
                System.arraycopy(b, 0, nb, 0, n);
                b = nb;
            }
        }

        void u8(int v) {
            room(1);
            b[n++] = (byte) v;
        }

        void raw(byte[] d) {
            room(d.length);
            System.arraycopy(d, 0, b, n, d.length);
            n += d.length;
        }

        void le16(int v) {
            u8(v);
            u8(v >> 8);
        }

        void i32(int v) {
            u8(v);
            u8(v >> 8);
            u8(v >> 16);
            u8(v >> 24);
        }

        void le64(long v) {
            i32((int) v);
            i32((int) (v >>> 32));
        }

        void f32(double v) {
            i32(Float.floatToIntBits((float) v));
        }

        void str(String s) {
            try {
                byte[] d = s.getBytes("UTF-8");
                i32(d.length);
                raw(d);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 is required", e);
            }
        }

        void uints(int[] v) {
            int k = v.length;
            room(k * 4);
            for (int i = 0; i < k; i++) {
                b[n + i] = (byte) (v[i] >>> 24);
                b[n + k + i] = (byte) (v[i] >>> 16);
                b[n + 2 * k + i] = (byte) (v[i] >>> 8);
                b[n + 3 * k + i] = (byte) v[i];
            }
            n += k * 4;
        }

        void ints(int[] v) {
            int[] z = new int[v.length];
            for (int i = 0; i < v.length; i++) {
                z[i] = (v[i] << 1) ^ (v[i] >> 31);
            }
            uints(z);
        }

        void refs(int[] v) {
            int[] d = new int[v.length];
            for (int i = 0; i < v.length; i++) {
                d[i] = i == 0 ? v[0] : v[i] - v[i - 1];
            }
            ints(d);
        }

        void floats(float[] v) {
            int[] r = new int[v.length];
            for (int i = 0; i < v.length; i++) {
                int bits = Float.floatToIntBits(v[i]);
                r[i] = bits << 1 | bits >>> 31;
            }
            uints(r);
        }

        void longs(long[] v) {
            int k = v.length;
            room(k * 8);
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < 8; j++) {
                    b[n + j * k + i] = (byte) (v[i] >>> (56 - 8 * j));
                }
            }
            n += k * 8;
        }
    }
}
