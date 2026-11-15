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
package com.codename1.rbx;

import com.codename1.luau.LuaTable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The byte format of multiplayer traffic: the ops a [ReplicaServer] sends
/// and a [ReplicaClient] answers, and the values they carry.
///
/// An instance travels as the network id the server gave it; a service as
/// its negative index in [#SERVICES]. A value of a type with no encoding
/// here (sequences, ranges, physical properties) travels as nil, so the
/// client keeps the class default -- [#encodable] tells which.
public final class Wire {
    /// Services whose contents every client sees. ServerScriptService and
    /// ServerStorage are deliberately absent: server code never leaves it.
    static final String[] SERVICES = {"Workspace", "Players", "Lighting", "ReplicatedStorage",
        "ReplicatedFirst", "StarterGui", "StarterPack", "StarterPlayer", "Teams", "SoundService",
        "TextChatService"};

    static final int CREATE = 1;
    static final int PROP = 2;
    static final int ATTR = 3;
    static final int PARENT = 4;
    static final int DESTROY = 5;
    static final int EVENT = 6;
    static final int INVOKE = 7;
    static final int RESULT = 8;
    static final int LOCAL = 9;
    static final int MOVE = 10;
    static final int CHAT = 11;
    static final int TAG = 12;

    private static final int T_NIL = 0;
    private static final int T_FALSE = 1;
    private static final int T_TRUE = 2;
    private static final int T_NUM = 3;
    private static final int T_STR = 4;
    private static final int T_V3 = 5;
    private static final int T_CF = 6;
    private static final int T_C3 = 7;
    private static final int T_UDIM2 = 8;
    private static final int T_UDIM = 9;
    private static final int T_V2 = 10;
    private static final int T_ENUM = 11;
    private static final int T_REF = 12;
    private static final int T_BRICK = 13;
    private static final int T_TABLE = 14;
    private static final int T_INT = 15;
    private static final int MAX_DEPTH = 16;

    private Wire() {
    }

    /// Maps instances to network ids and back.
    interface Ids {
        int id(Instance i);

        Instance instance(int id);
    }

    /// Whether `v` survives the trip.
    static boolean encodable(Object v) {
        return v == null || v instanceof Boolean || v instanceof Number || v instanceof String
            || v instanceof Vector3 || v instanceof CFrame || v instanceof Color3 || v instanceof UDim2
            || v instanceof UDim || v instanceof Vector2 || v instanceof Enums.EnumItem || v instanceof Instance
            || v instanceof BrickColor || v instanceof LuaTable;
    }

    static int serviceRef(Engine e, Instance i) {
        for (int k = 0; k < SERVICES.length; k++) {
            if (e.findService(SERVICES[k]) == i) { // NOPMD CompareObjectsWithEquals - service identity
                return -(k + 1);
            }
        }
        return 0;
    }

    static Instance service(Engine e, int ref) {
        int k = -ref - 1;
        return k >= 0 && k < SERVICES.length ? e.service(SERVICES[k]) : null;
    }

    /// Writes one ref: 0 nil, negative a service, positive a network id.
    static void ref(DataOutputStream out, Engine e, Ids ids, Instance i) throws IOException {
        if (i == null) {
            out.writeInt(0);
            return;
        }
        int s = serviceRef(e, i);
        out.writeInt(s != 0 ? s : ids.id(i));
    }

    static Instance ref(DataInputStream in, Engine e, Ids ids) throws IOException {
        int r = in.readInt();
        if (r == 0) {
            return null;
        }
        return r < 0 ? service(e, r) : ids.instance(r);
    }

    static void value(DataOutputStream out, Engine e, Ids ids, Object v) throws IOException {
        value(out, e, ids, v, 0);
    }

    private static void value(DataOutputStream out, Engine e, Ids ids, Object v, int depth) throws IOException {
        if (v == null) {
            out.writeByte(T_NIL);
        } else if (v instanceof Boolean) {
            out.writeByte(((Boolean) v).booleanValue() ? T_TRUE : T_FALSE);
        } else if (v instanceof Integer) {
            out.writeByte(T_INT);
            out.writeInt(((Integer) v).intValue());
        } else if (v instanceof Number) {
            out.writeByte(T_NUM);
            out.writeDouble(((Number) v).doubleValue());
        } else if (v instanceof String) {
            out.writeByte(T_STR);
            out.writeUTF((String) v);
        } else if (v instanceof Vector3) {
            Vector3 p = (Vector3) v;
            out.writeByte(T_V3);
            nums(out, p.x, p.y, p.z);
        } else if (v instanceof CFrame) {
            CFrame c = (CFrame) v;
            out.writeByte(T_CF);
            nums(out, c.x, c.y, c.z, c.r00, c.r01, c.r02, c.r10, c.r11, c.r12, c.r20, c.r21, c.r22);
        } else if (v instanceof Color3) {
            Color3 c = (Color3) v;
            out.writeByte(T_C3);
            nums(out, c.r, c.g, c.b);
        } else if (v instanceof UDim2) {
            UDim2 u = (UDim2) v;
            out.writeByte(T_UDIM2);
            nums(out, u.x.scale, u.x.offset, u.y.scale, u.y.offset);
        } else if (v instanceof UDim) {
            out.writeByte(T_UDIM);
            nums(out, ((UDim) v).scale, ((UDim) v).offset);
        } else if (v instanceof Vector2) {
            out.writeByte(T_V2);
            nums(out, ((Vector2) v).x, ((Vector2) v).y);
        } else if (v instanceof Enums.EnumItem) {
            out.writeByte(T_ENUM);
            out.writeUTF(((Enums.EnumItem) v).type.name);
            out.writeUTF(((Enums.EnumItem) v).name);
        } else if (v instanceof BrickColor) {
            out.writeByte(T_BRICK);
            out.writeInt(((BrickColor) v).number);
        } else if (v instanceof Instance) {
            out.writeByte(T_REF);
            ref(out, e, ids, (Instance) v);
        } else if (v instanceof LuaTable && depth < MAX_DEPTH) {
            table(out, e, ids, (LuaTable) v, depth);
        } else {
            out.writeByte(T_NIL);
        }
    }

    private static void table(DataOutputStream out, Engine e, Ids ids, LuaTable t, int depth) throws IOException {
        out.writeByte(T_TABLE);
        List<Object> keys = new ArrayList<Object>();
        List<Object> vals = new ArrayList<Object>();
        Object[] kv = new Object[2];
        Object k = null;
        while (t.next(k, kv)) {
            k = kv[0];
            keys.add(kv[0]);
            vals.add(kv[1]);
        }
        out.writeInt(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            value(out, e, ids, keys.get(i), depth + 1);
            value(out, e, ids, vals.get(i), depth + 1);
        }
    }

    private static void nums(DataOutputStream out, double... v) throws IOException {
        for (double d : v) {
            out.writeDouble(d);
        }
    }

    static Object value(DataInputStream in, Engine e, Ids ids) throws IOException {
        int t = in.readByte();
        switch (t) {
            case T_NIL:
                return null;
            case T_FALSE:
                return Boolean.FALSE;
            case T_TRUE:
                return Boolean.TRUE;
            case T_INT:
                return Integer.valueOf(in.readInt());
            case T_NUM:
                return Double.valueOf(in.readDouble());
            case T_STR:
                return in.readUTF();
            case T_V3:
                return new Vector3(in.readDouble(), in.readDouble(), in.readDouble());
            case T_CF: {
                double[] m = new double[12];
                for (int i = 0; i < 12; i++) {
                    m[i] = in.readDouble();
                }
                return new CFrame(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10], m[11]);
            }
            case T_C3:
                return new Color3(in.readDouble(), in.readDouble(), in.readDouble());
            case T_UDIM2:
                return new UDim2(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble());
            case T_UDIM:
                return new UDim(in.readDouble(), in.readDouble());
            case T_V2:
                return new Vector2(in.readDouble(), in.readDouble());
            case T_ENUM:
                return Enums.item(in.readUTF(), in.readUTF());
            case T_BRICK:
                return BrickColor.byNumber(in.readInt());
            case T_REF:
                return ref(in, e, ids);
            case T_TABLE: {
                LuaTable tab = new LuaTable();
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    Object k = value(in, e, ids);
                    Object v = value(in, e, ids);
                    if (k != null) {
                        tab.rawset(k, v);
                    }
                }
                return tab;
            }
            default:
                throw new IOException("Unknown value type " + t);
        }
    }

    static void values(DataOutputStream out, Engine e, Ids ids, Object[] v) throws IOException {
        out.writeInt(v == null ? 0 : v.length);
        if (v != null) {
            for (Object o : v) {
                value(out, e, ids, o);
            }
        }
    }

    static Object[] values(DataInputStream in, Engine e, Ids ids) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > 4096) {
            throw new IOException("Bad argument count " + n);
        }
        Object[] r = new Object[n];
        for (int i = 0; i < n; i++) {
            r[i] = value(in, e, ids);
        }
        return r;
    }

    /// A message under construction.
    static final class Writer {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(bytes);
        int ops;

        DataOutputStream op(int code) throws IOException {
            ops++;
            out.writeByte(code);
            return out;
        }

        byte[] done() {
            return bytes.toByteArray();
        }
    }

    static DataInputStream reader(byte[] msg) {
        return new DataInputStream(new ByteArrayInputStream(msg));
    }
}
