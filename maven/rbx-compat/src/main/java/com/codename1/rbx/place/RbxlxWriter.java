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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Writes a [PlaceFile] in the XML format, `.rbxlx` / `.rbxmx`. Every value is
/// written under the type it was read with, so Studio reads the result as
/// it would its own file.
public final class RbxlxWriter {
    private final StringBuilder out = new StringBuilder(); // NOPMD AvoidStringBufferField - one writer per document, dropped after write()
    private final Map<PlaceNode, String> refs = new IdentityHashMap<PlaceNode, String>();
    private final List<byte[]> shared = new ArrayList<byte[]>();
    private final List<String> sharedKeys = new ArrayList<String>();

    private RbxlxWriter() {
    }

    public static String write(PlaceFile f) {
        RbxlxWriter w = new RbxlxWriter();
        for (PlaceNode n : f.all()) {
            w.refs.put(n, "RBX" + w.refs.size());
        }
        w.out.append("<roblox version=\"4\">\n");
        w.out.append("\t<Meta name=\"ExplicitAutoJoints\">true</Meta>\n");
        w.out.append("\t<External>null</External>\n\t<External>nil</External>\n");
        for (PlaceNode r : f.roots) {
            w.item(r, 1);
        }
        if (!w.shared.isEmpty()) {
            w.out.append("\t<SharedStrings>\n");
            for (int i = 0; i < w.shared.size(); i++) {
                w.out.append("\t\t<SharedString md5=\"").append(w.sharedKeys.get(i)).append("\">");
                w.out.append(PlaceBase64.encode(w.shared.get(i))).append("</SharedString>\n");
            }
            w.out.append("\t</SharedStrings>\n");
        }
        w.out.append("</roblox>\n");
        return w.out.toString();
    }

    public static byte[] writeBytes(PlaceFile f) {
        try {
            return write(f).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is required", e);
        }
    }

    private void indent(int d) {
        for (int i = 0; i < d; i++) {
            out.append('\t');
        }
    }

    private void item(PlaceNode n, int d) {
        indent(d);
        out.append("<Item class=\"");
        Xml.escape(out, n.className);
        out.append("\" referent=\"").append(refs.get(n)).append("\">\n");
        indent(d + 1);
        out.append("<Properties>\n");
        for (Map.Entry<String, PlaceValue> e : n.props.entrySet()) {
            indent(d + 2);
            value(e.getKey(), e.getValue());
            out.append('\n');
        }
        indent(d + 1);
        out.append("</Properties>\n");
        for (PlaceNode c : n.children) {
            item(c, d + 1);
        }
        indent(d);
        out.append("</Item>\n");
    }

    private void open(String type, String name) {
        out.append('<').append(type).append(" name=\"");
        Xml.escape(out, name);
        out.append("\">");
    }

    private void tag(String n, double v) {
        out.append('<').append(n).append('>').append(num(v)).append("</").append(n).append('>');
    }

    private void tag(String n, String v) {
        out.append('<').append(n).append('>');
        Xml.escape(out, v);
        out.append("</").append(n).append('>');
    }

    static String num(double v) {
        if (Double.isNaN(v)) {
            return "NAN";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "INF" : "-INF";
        }
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }

    private void value(String name, PlaceValue pv) {
        String type = pv.type;
        Object v = pv.value;
        open(type, name);
        switch (type) {
            case "ProtectedString": {
                String s = (String) v;
                if (s.indexOf("]]>") < 0 && s.indexOf('\r') < 0) {
                    out.append("<![CDATA[").append(s).append("]]>");
                } else {
                    Xml.escape(out, s);
                }
                break;
            }
            case "Content":
            case "ContentId":
                if (v == null || ((String) v).length() == 0) {
                    out.append("<null></null>");
                } else {
                    tag("url", (String) v);
                }
                break;
            case "BinaryString":
                out.append(PlaceBase64.encode((byte[]) v));
                break;
            case "SharedString":
                out.append(sharedKey((byte[]) v));
                break;
            case "bool":
                out.append(Boolean.TRUE.equals(v) ? "true" : "false");
                break;
            case "int":
            case "int64":
            case "float":
            case "double":
                out.append(num(((Double) v).doubleValue()));
                break;
            case "token":
            case "BrickColor":
                out.append(((Integer) v).intValue());
                break;
            case "Faces":
                tag("faces", ((Integer) v).intValue());
                break;
            case "Axes":
                tag("axes", ((Integer) v).intValue());
                break;
            case "Color3": {
                Color3 c = (Color3) v;
                tag("R", c.r);
                tag("G", c.g);
                tag("B", c.b);
                break;
            }
            case "Color3uint8": {
                Color3 c = (Color3) v;
                long packed = 0xff000000L | (long) channel(c.r) << 16 | channel(c.g) << 8 | channel(c.b);
                out.append(packed);
                break;
            }
            case "Vector3":
            case "Vector3int16":
                vec((Vector3) v);
                break;
            case "Vector2":
            case "Vector2int16": {
                Vector2 p = (Vector2) v;
                tag("X", p.x);
                tag("Y", p.y);
                break;
            }
            case "CoordinateFrame":
                cframe((CFrame) v);
                break;
            case "OptionalCoordinateFrame":
                if (v != null) {
                    out.append("<CFrame>");
                    cframe((CFrame) v);
                    out.append("</CFrame>");
                }
                break;
            case "UDim": {
                UDim u = (UDim) v;
                tag("S", u.scale);
                tag("O", u.offset);
                break;
            }
            case "UDim2": {
                UDim2 u = (UDim2) v;
                tag("XS", u.x.scale);
                tag("XO", u.x.offset);
                tag("YS", u.y.scale);
                tag("YO", u.y.offset);
                break;
            }
            case "Ray": {
                Ray r = (Ray) v;
                out.append("<origin>");
                vec(r.origin);
                out.append("</origin><direction>");
                vec(r.direction);
                out.append("</direction>");
                break;
            }
            case "Rect2D": {
                QueryParams.Rect r = (QueryParams.Rect) v;
                out.append("<min>");
                tag("X", r.min.x);
                tag("Y", r.min.y);
                out.append("</min><max>");
                tag("X", r.max.x);
                tag("Y", r.max.y);
                out.append("</max>");
                break;
            }
            case "NumberRange": {
                NumberRange r = (NumberRange) v;
                out.append(num(r.min)).append(' ').append(num(r.max)).append(' ');
                break;
            }
            case "NumberSequence":
            case "ColorSequence":
                sequence((Sequence) v);
                break;
            case "PhysicalProperties":
                if (v == null) {
                    tag("CustomPhysics", "false");
                } else {
                    QueryParams.PhysicalProperties pp = (QueryParams.PhysicalProperties) v;
                    tag("CustomPhysics", "true");
                    tag("Density", pp.density);
                    tag("Friction", pp.friction);
                    tag("Elasticity", pp.elasticity);
                    tag("FrictionWeight", pp.frictionWeight);
                    tag("ElasticityWeight", pp.elasticityWeight);
                }
                break;
            case "Ref":
                out.append(v == null ? "null" : refs.containsKey(v) ? refs.get(v) : "null");
                break;
            case "Font": {
                String[] f = (String[]) v;
                out.append("<Family>");
                tag("url", f[0]);
                out.append("</Family>");
                tag("Weight", f[1]);
                tag("Style", f[2]);
                if (f[3].length() > 0) {
                    out.append("<CachedFaceId>");
                    tag("url", f[3]);
                    out.append("</CachedFaceId>");
                }
                break;
            }
            default:
                Xml.escape(out, String.valueOf(v));
        }
        out.append("</").append(type).append('>');
    }

    private static int channel(double c) {
        long v = Math.round(c * 255);
        return (int) Math.max(0, Math.min(255, v));
    }

    private void vec(Vector3 p) {
        tag("X", p.x);
        tag("Y", p.y);
        tag("Z", p.z);
    }

    private void cframe(CFrame c) {
        tag("X", c.x);
        tag("Y", c.y);
        tag("Z", c.z);
        tag("R00", c.r00);
        tag("R01", c.r01);
        tag("R02", c.r02);
        tag("R10", c.r10);
        tag("R11", c.r11);
        tag("R12", c.r12);
        tag("R20", c.r20);
        tag("R21", c.r21);
        tag("R22", c.r22);
    }

    private void sequence(Sequence s) {
        for (int i = 0; i < s.times.length; i++) {
            out.append(num(s.times[i])).append(' ');
            Object v = s.values[i];
            if (v instanceof Color3) {
                Color3 c = (Color3) v;
                out.append(num(c.r)).append(' ').append(num(c.g)).append(' ').append(num(c.b)).append(' ');
            } else if (v instanceof Double) {
                out.append(num(((Double) v).doubleValue())).append(' ');
            }
            out.append(num(s.envelopes[i])).append(' ');
        }
    }

    /// Shared strings are keyed by content; equal values share one entry.
    private String sharedKey(byte[] v) {
        for (int i = 0; i < shared.size(); i++) {
            if (java.util.Arrays.equals(shared.get(i), v)) {
                return sharedKeys.get(i);
            }
        }
        String key = "s" + shared.size();
        shared.add(v);
        sharedKeys.add(key);
        return key;
    }
}
