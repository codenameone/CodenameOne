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

import com.codename1.rbx.BrickColor;
import com.codename1.rbx.CFrame;
import com.codename1.rbx.Color3;
import com.codename1.rbx.NumberRange;
import com.codename1.rbx.QueryParams;
import com.codename1.rbx.Sequence;
import com.codename1.rbx.UDim;
import com.codename1.rbx.UDim2;
import com.codename1.rbx.Vector2;
import com.codename1.rbx.Vector3;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The `AttributesSerialize` blob: a count, then per attribute its name, a
/// type byte and the value, all little endian and not interleaved.
final class Attributes {
    static final int STRING = 0x02;
    static final int BOOL = 0x03;
    static final int FLOAT = 0x05;
    static final int DOUBLE = 0x06;
    static final int UDIM = 0x09;
    static final int UDIM2 = 0x0A;
    static final int BRICKCOLOR = 0x0E;
    static final int COLOR3 = 0x0F;
    static final int VECTOR2 = 0x10;
    static final int VECTOR3 = 0x11;
    static final int CFRAME = 0x14;
    static final int NUMBERSEQUENCE = 0x17;
    static final int COLORSEQUENCE = 0x19;
    static final int NUMBERRANGE = 0x1B;
    static final int RECT = 0x1C;

    private Attributes() {
    }

    /// Decodes what it can. A type it does not know ends the decode, since
    /// the length of its value is unknown; the note names it.
    static Map<String, Object> decode(byte[] d, List<String> notes) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (d.length < 4) {
            return out;
        }
        RbxlReader.Buf b = new RbxlReader.Buf(d);
        int count = b.i32();
        for (int i = 0; i < count; i++) {
            String name = b.str();
            int type = b.u8();
            Object v = value(b, type);
            if (v == null) {
                notes.add("Attribute " + name + " has unsupported type 0x" + Integer.toHexString(type));
                return out;
            }
            out.put(name, v);
        }
        return out;
    }

    private static Object value(RbxlReader.Buf b, int type) {
        switch (type) {
            case STRING:
                return b.str();
            case BOOL:
                return Boolean.valueOf(b.u8() != 0);
            case FLOAT:
                return Double.valueOf(b.f32());
            case DOUBLE:
                return Double.valueOf(Double.longBitsToDouble(b.le64()));
            case UDIM:
                return new UDim(b.f32(), b.i32());
            case UDIM2: {
                double xs = b.f32();
                double xo = b.i32();
                double ys = b.f32();
                return new UDim2(xs, xo, ys, b.i32());
            }
            case BRICKCOLOR:
                return BrickColor.byNumber(b.i32());
            case COLOR3:
                return new Color3(b.f32(), b.f32(), b.f32());
            case VECTOR2:
                return new Vector2(b.f32(), b.f32());
            case VECTOR3:
                return new Vector3(b.f32(), b.f32(), b.f32());
            case CFRAME: {
                double x = b.f32();
                double y = b.f32();
                double z = b.f32();
                int id = b.u8();
                double[] m;
                if (id == 0) {
                    m = new double[9];
                    for (int k = 0; k < 9; k++) {
                        m[k] = b.f32();
                    }
                } else {
                    m = RbxlReader.specialRotation(id);
                }
                return new CFrame(x, y, z, m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8]);
            }
            case NUMBERSEQUENCE: {
                int k = b.i32();
                double[] v = new double[k * 3];
                for (int i = 0; i < k; i++) {
                    double env = b.f32();
                    v[i * 3] = b.f32();
                    v[i * 3 + 1] = b.f32();
                    v[i * 3 + 2] = env;
                }
                return RbxlxReader.numberSequence(v);
            }
            case COLORSEQUENCE: {
                int k = b.i32();
                double[] v = new double[k * 5];
                for (int i = 0; i < k; i++) {
                    double env = b.f32();
                    v[i * 5] = b.f32();
                    v[i * 5 + 1] = b.f32();
                    v[i * 5 + 2] = b.f32();
                    v[i * 5 + 3] = b.f32();
                    v[i * 5 + 4] = env;
                }
                return RbxlxReader.colorSequence(v);
            }
            case NUMBERRANGE:
                return new NumberRange(b.f32(), b.f32());
            case RECT:
                return new QueryParams.Rect(b.f32(), b.f32(), b.f32(), b.f32());
            default:
                return null;
        }
    }

    /// Encodes the attributes of an instance; a value of a type the format
    /// has no code for is skipped.
    static byte[] encode(Map<String, Object> attrs) {
        RbxlWriter.Out o = new RbxlWriter.Out();
        RbxlWriter.Out body = new RbxlWriter.Out();
        int count = 0;
        for (Map.Entry<String, Object> e : attrs.entrySet()) {
            RbxlWriter.Out one = new RbxlWriter.Out();
            one.str(e.getKey());
            if (encode(one, e.getValue())) {
                byte[] d = new byte[one.size()];
                System.arraycopy(one.bytes(), 0, d, 0, d.length);
                body.raw(d);
                count++;
            }
        }
        o.i32(count);
        byte[] d = new byte[body.size()];
        System.arraycopy(body.bytes(), 0, d, 0, d.length);
        o.raw(d);
        byte[] out = new byte[o.size()];
        System.arraycopy(o.bytes(), 0, out, 0, out.length);
        return out;
    }

    private static boolean encode(RbxlWriter.Out o, Object v) {
        if (v instanceof String) {
            o.u8(STRING);
            o.str((String) v);
        } else if (v instanceof Boolean) {
            o.u8(BOOL);
            o.u8(((Boolean) v).booleanValue() ? 1 : 0);
        } else if (v instanceof Double) {
            o.u8(DOUBLE);
            o.le64(Double.doubleToLongBits(((Double) v).doubleValue()));
        } else if (v instanceof UDim) {
            UDim u = (UDim) v;
            o.u8(UDIM);
            o.f32(u.scale);
            o.i32((int) u.offset);
        } else if (v instanceof UDim2) {
            UDim2 u = (UDim2) v;
            o.u8(UDIM2);
            o.f32(u.x.scale);
            o.i32((int) u.x.offset);
            o.f32(u.y.scale);
            o.i32((int) u.y.offset);
        } else if (v instanceof BrickColor) {
            o.u8(BRICKCOLOR);
            o.i32(((BrickColor) v).number);
        } else if (v instanceof Color3) {
            Color3 c = (Color3) v;
            o.u8(COLOR3);
            o.f32(c.r);
            o.f32(c.g);
            o.f32(c.b);
        } else if (v instanceof Vector2) {
            Vector2 p = (Vector2) v;
            o.u8(VECTOR2);
            o.f32(p.x);
            o.f32(p.y);
        } else if (v instanceof Vector3) {
            Vector3 p = (Vector3) v;
            o.u8(VECTOR3);
            o.f32(p.x);
            o.f32(p.y);
            o.f32(p.z);
        } else if (v instanceof CFrame) {
            CFrame c = (CFrame) v;
            o.u8(CFRAME);
            o.f32(c.x);
            o.f32(c.y);
            o.f32(c.z);
            double[] m = {c.r00, c.r01, c.r02, c.r10, c.r11, c.r12, c.r20, c.r21, c.r22};
            int id = RbxlReader.rotationId(m);
            o.u8(id);
            if (id == 0) {
                for (double x : m) {
                    o.f32(x);
                }
            }
        } else if (v instanceof NumberRange) {
            NumberRange r = (NumberRange) v;
            o.u8(NUMBERRANGE);
            o.f32(r.min);
            o.f32(r.max);
        } else if (v instanceof QueryParams.Rect) {
            QueryParams.Rect r = (QueryParams.Rect) v;
            o.u8(RECT);
            o.f32(r.min.x);
            o.f32(r.min.y);
            o.f32(r.max.x);
            o.f32(r.max.y);
        } else if (v instanceof Sequence) {
            Sequence s = (Sequence) v;
            o.u8(s.color ? COLORSEQUENCE : NUMBERSEQUENCE);
            o.i32(s.times.length);
            for (int i = 0; i < s.times.length; i++) {
                o.f32(s.envelopes[i]);
                o.f32(s.times[i]);
                Object kv = s.values[i];
                if (kv instanceof Color3) {
                    Color3 c = (Color3) kv;
                    o.f32(c.r);
                    o.f32(c.g);
                    o.f32(c.b);
                } else {
                    o.f32(kv instanceof Double ? ((Double) kv).doubleValue() : 0);
                }
            }
        } else {
            return false;
        }
        return true;
    }
}
