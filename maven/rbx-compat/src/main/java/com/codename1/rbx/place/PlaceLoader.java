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
import com.codename1.rbx.ClassDb;
import com.codename1.rbx.Color3;
import com.codename1.rbx.Engine;
import com.codename1.rbx.Enums;
import com.codename1.rbx.Instance;
import com.codename1.rbx.QueryParams;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Turns a [PlaceFile] into live instances of an [Engine], and reports what
/// did not survive the trip in a [CompatReport].
///
/// Files name a few properties differently from the scripting API
/// (`Color3uint8` for `Color`, `size` for `Size`, `Health_XML`); those are
/// mapped. Values are converted by the type of the engine default, so an
/// enum token becomes the enum item and a BrickColor number a BrickColor.
public final class PlaceLoader {
    /// Engine extra holding `Class.Property` -> file type, so a save writes
    /// each property back under the type the file used.
    public static final String TYPES_KEY = "place.types";

    private static final String[][] ALIASES = {
        {"Color3uint8", "Color"}, {"size", "Size"}, {"shape", "Shape"}, {"Health_XML", "Health"},
        {"WorldPivotData", "WorldPivot"}, {"CustomPhysicalProperties", "CustomPhysicalProperties"},
    };

    private static final String[] IGNORED = {"formFactorRaw", "FormFactor", "SourceAssetId", "UniqueId",
        "HistoryId", "Capabilities", "DefinesCapabilities", "ScriptGuid", "LinkedSource", "ModelStreamingMode",
        "NeedsPivotMigration", "PhysicsGrid", "SmoothGrid", "MaterialColors", "ModelMeshData", "ModelMeshSize",
        "PhysicalConfigData", "InitialSize", "MeshContent", "TextureContent", "CollisionFidelity",
        "FluidFidelity", "AeroMeshData", "Velocity", "RotVelocity", "Elasticity", "Friction", "IsAutoJoint",
        "AttributesReplicate", "ScaleFactor", "AcquisitionGroupId", "ModelMeshCFrame"};

    private final Engine engine;
    private final CompatReport report = new CompatReport();
    private final Map<PlaceNode, Instance> made = new IdentityHashMap<PlaceNode, Instance>();
    private final Map<String, String> types;

    @SuppressWarnings("unchecked")
    private PlaceLoader(Engine engine) {
        this.engine = engine;
        Object t = engine.extra(TYPES_KEY);
        if (t instanceof Map) {
            types = (Map<String, String>) t;
        } else {
            types = new HashMap<String, String>();
            engine.extra(TYPES_KEY, types);
        }
    }

    /// Loads a place: top-level services merge into the engine's own, any
    /// other top-level object goes into the Workspace.
    public static CompatReport loadPlace(Engine e, PlaceFile f) {
        return load(e, f, null);
    }

    /// Loads a model under `parent` (null means the Workspace).
    public static CompatReport load(Engine e, PlaceFile f, Instance parent) {
        PlaceLoader l = new PlaceLoader(e);
        l.report.readerNotes.addAll(f.readerNotes);
        for (PlaceNode r : f.roots) {
            ClassDb.Desc d = ClassDb.get(r.className);
            if (parent == null && d != null && d.isService()) {
                Instance svc = e.service(r.className);
                l.made.put(r, svc);
                l.props(r, svc);
                l.children(r, svc);
            } else {
                Instance i = l.build(r);
                i.setParent(parent != null ? parent : e.getWorkspace());
            }
        }
        l.refs(f);
        return l.report;
    }

    /// Children of a service. The engine already made the singletons a
    /// place also saves (the Workspace's Camera and Terrain); the file's copy
    /// is merged into the existing one rather than added beside it.
    private void children(PlaceNode n, Instance into) {
        for (PlaceNode c : n.children) {
            Instance existing = "Camera".equals(c.className) || "Terrain".equals(c.className)
                    ? into.findFirstChildOfClass(c.className, false) : null;
            if (existing != null) {
                report.instances++;
                made.put(c, existing);
                props(c, existing);
                for (PlaceNode g : c.children) {
                    build(g).setParent(existing);
                }
                continue;
            }
            Instance ci = build(c);
            ci.setParent(into);
        }
    }

    /// Builds a subtree detached, then the caller parents its root, so
    /// scripts start once with their whole tree present.
    private Instance build(PlaceNode n) {
        report.instances++;
        ClassDb.Desc d = ClassDb.get(n.className);
        Instance i;
        if (d == null) {
            CompatReport.count(report.unknownClasses, n.className);
            i = engine.create("Folder");
        } else {
            i = engine.create(n.className);
        }
        made.put(n, i);
        if (i.isA("LuaSourceContainer")) {
            report.scripts++;
        }
        props(n, i);
        if (d != null && i.isA("LuaSourceContainer")) {
            ScriptScan.scan(i.getName(), i.str("Source"), report);
        }
        for (PlaceNode c : n.children) {
            build(c).setParent(i);
        }
        return i;
    }

    private static String alias(String p) {
        for (String[] a : ALIASES) {
            if (a[0].equals(p)) {
                return a[1];
            }
        }
        return p;
    }

    private static boolean ignored(String p) {
        for (String s : IGNORED) {
            if (s.equals(p)) {
                return true;
            }
        }
        return p.endsWith("SurfaceInput") || p.endsWith("ParamA") || p.endsWith("ParamB")
                || p.endsWith("Surface") && !p.startsWith("Top") && !p.startsWith("Bottom");
    }

    private void props(PlaceNode n, Instance i) {
        for (Map.Entry<String, PlaceValue> e : n.props.entrySet()) {
            String file = e.getKey();
            PlaceValue pv = e.getValue();
            types.put(n.className + "." + file, pv.type);
            if ("Name".equals(file)) {
                if (pv.value instanceof String) {
                    i.setName((String) pv.value);
                }
                continue;
            }
            if ("Tags".equals(file)) {
                tags(i, pv.value);
                continue;
            }
            if ("AttributesSerialize".equals(file)) {
                if (pv.value instanceof byte[]) {
                    Map<String, Object> a = Attributes.decode((byte[]) pv.value, report.badValues);
                    for (Map.Entry<String, Object> ae : a.entrySet()) {
                        i.setAttribute(ae.getKey(), ae.getValue());
                    }
                }
                continue;
            }
            if ("TimeOfDay".equals(file) && pv.value instanceof String) {
                i.putQuiet("ClockTime", Double.valueOf(clock((String) pv.value)));
                continue;
            }
            if ("BrickColor".equals(file) && pv.value instanceof Integer && i.isA("BasePart")
                    && !n.props.containsKey("Color3uint8")) {
                i.putQuiet("Color", BrickColor.byNumber(((Integer) pv.value).intValue()).color());
                continue;
            }
            String prop = alias(file);
            if (ignored(file)) {
                continue;
            }
            Object def = i.getDesc().defaultOf(prop);
            if (def == ClassDb.MISSING) { // NOPMD CompareObjectsWithEquals - sentinel
                CompatReport.count(report.unknownProperties, n.className + "." + file);
                continue;
            }
            if ("Ref".equals(pv.type)) {
                continue;
            }
            asset(n, prop, pv.value);
            Object v = convert(def, pv);
            if (v == BAD) { // NOPMD CompareObjectsWithEquals - sentinel
                report.badValues.add(n.className + "." + file + ": " + pv.type + " value " + pv.value);
            } else {
                i.putQuiet(prop, v);
            }
        }
    }

    private static final Object BAD = new Object();

    private static Object convert(Object def, PlaceValue pv) {
        Object v = pv.value;
        if (def instanceof Enums.EnumItem) {
            if (v instanceof Integer) {
                Enums.EnumItem it = ((Enums.EnumItem) def).type.byValue(((Integer) v).intValue());
                return it == null ? BAD : it;
            }
            return BAD;
        }
        if (def instanceof Double) {
            if (v instanceof Integer) {
                return Double.valueOf(((Integer) v).intValue());
            }
            return v instanceof Double ? v : BAD;
        }
        if (def instanceof BrickColor) {
            if (v instanceof Integer) {
                return BrickColor.byNumber(((Integer) v).intValue());
            }
            return v instanceof Color3 ? BrickColor.nearest((Color3) v) : BAD;
        }
        if (def instanceof String) {
            if (v instanceof byte[]) {
                byte[] b = (byte[]) v;
                return RbxlxReader.utf8(b, 0, b.length);
            }
            if (v instanceof String[]) {
                return ((String[]) v)[0];
            }
            return v instanceof String ? v : BAD;
        }
        if (def instanceof QueryParams.FontFace && v instanceof String[]) {
            return new QueryParams.FontFace(((String[]) v)[0]);
        }
        if (def == null || def == ClassDb.REF) { // NOPMD CompareObjectsWithEquals - sentinel
            return v;
        }
        if (v == null) {
            return def;
        }
        return def.getClass() == v.getClass() ? v : BAD;
    }

    private void asset(PlaceNode n, String prop, Object v) {
        if (!(v instanceof String)) {
            return;
        }
        String s = (String) v;
        if (s.startsWith("rbxassetid://") || s.startsWith("rbxasset://") || s.startsWith("rbxthumb://")
                || s.indexOf("roblox.com/asset") >= 0) {
            CompatReport.add(report.missingAssets, s, n.name() + "." + prop);
        }
    }

    private static void tags(Instance i, Object v) {
        String s;
        if (v instanceof byte[]) {
            byte[] b = (byte[]) v;
            s = RbxlxReader.utf8(b, 0, b.length);
        } else if (v instanceof String) {
            s = (String) v;
        } else {
            return;
        }
        int st = 0;
        for (int k = 0; k <= s.length(); k++) {
            if (k == s.length() || s.charAt(k) == '\0') {
                if (k > st) {
                    i.getEngine().addTag(i, s.substring(st, k));
                }
                st = k + 1;
            }
        }
    }

    static double clock(String t) {
        String[] parts = {"0", "0", "0"};
        int idx = 0;
        int st = 0;
        for (int k = 0; k <= t.length() && idx < 3; k++) {
            if (k == t.length() || t.charAt(k) == ':') {
                parts[idx++] = t.substring(st, k);
                st = k + 1;
            }
        }
        try {
            return Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim()) / 60
                    + Double.parseDouble(parts[2].trim()) / 3600;
        } catch (NumberFormatException e) {
            return 14;
        }
    }

    private void refs(PlaceFile f) {
        List<PlaceNode> all = f.all();
        for (PlaceNode n : all) {
            Instance i = made.get(n);
            if (i == null) {
                continue;
            }
            for (Map.Entry<String, PlaceValue> e : n.props.entrySet()) {
                PlaceValue pv = e.getValue();
                if (!"Ref".equals(pv.type) || pv.value == null) {
                    continue;
                }
                Instance target = made.get(pv.value);
                String prop = alias(e.getKey());
                if (target != null && i.getDesc().defaultOf(prop) != ClassDb.MISSING) { // NOPMD CompareObjectsWithEquals - sentinel
                    i.putQuiet(prop, target);
                }
            }
        }
    }
}
