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
import com.codename1.util.Base64;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads the XML place and model formats, `.rbxlx` and `.rbxmx`.
public final class RbxlxReader {
    private final PlaceFile out = new PlaceFile();
    private final Map<String, PlaceNode> byRef = new HashMap<String, PlaceNode>();
    private final List<Object[]> refs = new ArrayList<Object[]>();
    private final List<Object[]> shared = new ArrayList<Object[]>();
    private final Map<String, byte[]> sharedStrings = new HashMap<String, byte[]>();

    private RbxlxReader() {
    }

    public static PlaceFile read(byte[] data) {
        return read(utf8(data, 0, data.length));
    }

    public static PlaceFile read(String xml) {
        Xml root = Xml.parse(xml);
        if (!"roblox".equals(root.name)) {
            throw new IllegalArgumentException("Not a place file: the root element is <" + root.name + ">");
        }
        RbxlxReader r = new RbxlxReader();
        for (Xml c : root.children) {
            if ("Item".equals(c.name)) {
                r.out.roots.add(r.item(c, null));
            } else if ("SharedStrings".equals(c.name)) {
                for (Xml s : c.children) {
                    r.sharedStrings.put(s.attrs.get("md5"), base64(s.text.toString()));
                }
            }
        }
        for (Object[] f : r.refs) {
            PlaceNode n = (PlaceNode) f[0];
            n.put((String) f[1], "Ref", r.byRef.get((String) f[2]));
        }
        for (Object[] f : r.shared) {
            PlaceNode n = (PlaceNode) f[0];
            byte[] v = r.sharedStrings.get((String) f[2]);
            if (v == null) {
                r.out.readerNotes.add("SharedString " + f[2] + " of " + n.className + "." + f[1] + " is missing");
                v = new byte[0];
            }
            n.put((String) f[1], "SharedString", v);
        }
        return r.out;
    }

    private PlaceNode item(Xml x, PlaceNode parent) {
        PlaceNode n = new PlaceNode(x.attrs.containsKey("class") ? x.attrs.get("class") : "Folder");
        n.referent = x.attrs.get("referent");
        if (n.referent != null) {
            byRef.put(n.referent, n);
        }
        if (parent != null) {
            parent.add(n);
        }
        for (Xml c : x.children) {
            if ("Properties".equals(c.name)) {
                for (Xml p : c.children) {
                    property(n, p);
                }
            } else if ("Item".equals(c.name)) {
                item(c, n);
            }
        }
        return n;
    }

    private void property(PlaceNode n, Xml p) {
        String name = p.attrs.get("name");
        if (name == null) {
            return;
        }
        String t = p.text.toString();
        try {
            Object v = decode(p, t);
            if (v == REF) {
                if (!"null".equals(t.trim()) && t.trim().length() > 0) {
                    refs.add(new Object[] {n, name, t.trim()});
                }
                n.put(name, "Ref", null);
            } else if (v == SHARED) {
                n.put(name, "SharedString", new byte[0]);
                shared.add(new Object[] {n, name, t.trim()});
            } else {
                n.put(name, p.name, v);
            }
        } catch (NumberFormatException e) {
            out.readerNotes.add("Bad " + p.name + " value for " + n.className + "." + name);
        } catch (IllegalArgumentException e) {
            out.readerNotes.add(e.getMessage() + " (" + n.className + "." + name + ")");
        }
    }

    private static final Object REF = new Object();
    private static final Object SHARED = new Object();

    private static Object decode(Xml p, String t) {
        String type = p.name;
        switch (type) {
            case "string":
            case "ProtectedString":
                return t;
            case "Content":
            case "ContentId":
                return content(p, t);
            case "BinaryString":
                return base64(t);
            case "SharedString":
                return SHARED;
            case "bool":
                return Boolean.valueOf("true".equalsIgnoreCase(t.trim()));
            case "int":
            case "int64":
            case "float":
            case "double":
                return Double.valueOf(num(t));
            case "token":
            case "BrickColor":
                return Integer.valueOf((int) Long.parseLong(t.trim()));
            case "Faces":
                return Integer.valueOf(intChild(p, "faces", t));
            case "Axes":
                return Integer.valueOf(intChild(p, "axes", t));
            case "Color3":
                if (p.child("R") == null) {
                    return Color3.fromInt((int) Long.parseLong(t.trim()) & 0xffffff);
                }
                return new Color3(f(p, "R"), f(p, "G"), f(p, "B"));
            case "Color3uint8":
                return Color3.fromInt((int) Long.parseLong(t.trim()) & 0xffffff);
            case "Vector3":
            case "Vector3int16":
                return new Vector3(f(p, "X"), f(p, "Y"), f(p, "Z"));
            case "Vector2":
            case "Vector2int16":
                return new Vector2(f(p, "X"), f(p, "Y"));
            case "CoordinateFrame":
                return cframe(p);
            case "OptionalCoordinateFrame": {
                Xml c = p.child("CFrame");
                return c == null ? null : cframe(c);
            }
            case "UDim":
                return new UDim(f(p, "S"), f(p, "O"));
            case "UDim2":
                return new UDim2(f(p, "XS"), f(p, "XO"), f(p, "YS"), f(p, "YO"));
            case "Ray":
                return new Ray(vec(p.child("origin")), vec(p.child("direction")));
            case "Rect2D": {
                Xml a = p.child("min");
                Xml b = p.child("max");
                return new QueryParams.Rect(f(a, "X"), f(a, "Y"), f(b, "X"), f(b, "Y"));
            }
            case "NumberRange": {
                double[] v = nums(t);
                return new NumberRange(v.length > 0 ? v[0] : 0, v.length > 1 ? v[1] : v.length > 0 ? v[0] : 0);
            }
            case "NumberSequence":
                return numberSequence(nums(t));
            case "ColorSequence":
                return colorSequence(nums(t));
            case "PhysicalProperties":
                if (!"true".equalsIgnoreCase(p.childText("CustomPhysics"))) {
                    return null;
                }
                return new QueryParams.PhysicalProperties(f(p, "Density"), f(p, "Friction"), f(p, "Elasticity"),
                        f(p, "FrictionWeight"), f(p, "ElasticityWeight"));
            case "Ref":
                return REF;
            case "Font":
                return new String[] {content(p.child("Family"), ""), trimmed(p.childText("Weight"), "400"),
                    trimmed(p.childText("Style"), "Normal"), content(p.child("CachedFaceId"), "")};
            case "UniqueId":
            case "SecurityCapabilities":
                return t.trim();
            default:
                throw new IllegalArgumentException("Unsupported property type " + type);
        }
    }

    private static String trimmed(String s, String def) {
        return s == null ? def : s.trim();
    }

    /// `Content` is a `<url>`, a `<null>` or, in older files, bare text.
    private static String content(Xml p, String t) {
        if (p == null) {
            return t;
        }
        String u = p.childText("url");
        if (u != null) {
            return u;
        }
        if (p.child("null") != null) {
            return "";
        }
        String h = p.childText("hash");
        return h != null ? h : p.text.toString().trim();
    }

    private static int intChild(Xml p, String n, String t) {
        String c = p.childText(n);
        return (int) Long.parseLong((c != null ? c : t).trim());
    }

    static double num(String s) {
        String t = s.trim();
        if ("INF".equals(t) || "inf".equals(t) || "+INF".equals(t)) {
            return Double.POSITIVE_INFINITY;
        }
        if ("-INF".equals(t) || "-inf".equals(t)) {
            return Double.NEGATIVE_INFINITY;
        }
        if ("NAN".equalsIgnoreCase(t) || "-NAN".equalsIgnoreCase(t)) {
            return Double.NaN;
        }
        return t.length() == 0 ? 0 : Double.parseDouble(t);
    }

    private static double f(Xml p, String n) {
        String t = p == null ? null : p.childText(n);
        return t == null ? 0 : num(t);
    }

    private static Vector3 vec(Xml p) {
        return new Vector3(f(p, "X"), f(p, "Y"), f(p, "Z"));
    }

    private static CFrame cframe(Xml p) {
        return new CFrame(f(p, "X"), f(p, "Y"), f(p, "Z"), f(p, "R00"), f(p, "R01"), f(p, "R02"), f(p, "R10"),
                f(p, "R11"), f(p, "R12"), f(p, "R20"), f(p, "R21"), f(p, "R22"));
    }

    private static double[] nums(String t) {
        List<String> parts = new ArrayList<String>();
        int i = 0;
        int n = t.length();
        while (i < n) {
            while (i < n && t.charAt(i) <= ' ') {
                i++;
            }
            int st = i;
            while (i < n && t.charAt(i) > ' ') {
                i++;
            }
            if (st < i) {
                parts.add(t.substring(st, i));
            }
        }
        double[] out = new double[parts.size()];
        for (int k = 0; k < out.length; k++) {
            out[k] = num(parts.get(k));
        }
        return out;
    }

    static Sequence numberSequence(double[] v) {
        int k = v.length / 3;
        double[] times = new double[k];
        Object[] vals = new Object[k];
        double[] env = new double[k];
        for (int i = 0; i < k; i++) {
            times[i] = v[i * 3];
            vals[i] = Double.valueOf(v[i * 3 + 1]);
            env[i] = v[i * 3 + 2];
        }
        return new Sequence(false, times, vals, env);
    }

    static Sequence colorSequence(double[] v) {
        int k = v.length / 5;
        double[] times = new double[k];
        Object[] vals = new Object[k];
        double[] env = new double[k];
        for (int i = 0; i < k; i++) {
            times[i] = v[i * 5];
            vals[i] = new Color3(v[i * 5 + 1], v[i * 5 + 2], v[i * 5 + 3]);
            env[i] = v[i * 5 + 4];
        }
        return new Sequence(true, times, vals, env);
    }

    static byte[] base64(String t) {
        StringBuilder b = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c > ' ') {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            return new byte[0];
        }
        try {
            return Base64.decode(b.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is required", e);
        }
    }

    static String utf8(byte[] d, int off, int len) {
        try {
            return new String(d, off, len, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is required", e);
        }
    }
}
