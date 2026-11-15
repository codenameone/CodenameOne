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

import com.codename1.luau.Interp;
import com.codename1.luau.LuaError;
import com.codename1.luau.LuaFunction;
import com.codename1.luau.LuaTable;
import java.util.ArrayList;
import java.util.List;

/// Behaviour of the scene classes: parts, models, the world queries,
/// attachments, cameras, values, remotes, bindables, effects, tools, GUI and
/// tweens. Services are in [ServiceBehaviors], characters in [Characters].
final class Behaviors {
    private Behaviors() {
    }

    static void install() {
        ClassDb.attach("DataModel", DATA_MODEL);
        ClassDb.attach("PVInstance", PV);
        ClassDb.attach("Model", MODEL);
        ClassDb.attach("WorldRoot", WORLD);
        ClassDb.attach("Workspace", WORKSPACE);
        ClassDb.attach("BasePart", PART);
        ClassDb.attach("Attachment", ATTACHMENT);
        ClassDb.attach("Camera", CAMERA);
        ClassDb.attach("BaseRemoteEvent", REMOTE_EVENT);
        ClassDb.attach("RemoteFunction", REMOTE_FUNCTION);
        ClassDb.attach("BindableEvent", BINDABLE_EVENT);
        ClassDb.attach("BindableFunction", BINDABLE_FUNCTION);
        ClassDb.attach("Sound", SOUND);
        ClassDb.attach("ParticleEmitter", PARTICLES);
        ClassDb.attach("Explosion", EXPLOSION);
        ClassDb.attach("Tool", TOOL);
        ClassDb.attach("Seat", SEAT);
        ClassDb.attach("VehicleSeat", SEAT);
        ClassDb.attach("TweenBase", TWEEN);
        ClassDb.attach("GuiObject", GUI);
        ClassDb.attach("TextBox", TEXT_BOX);
        ClassDb.attach("Lighting", LIGHTING);
        ClassDb.attach("Script", SCRIPT);
        ClassDb.attach("ModuleScript", SCRIPT);
        ClassDb.attach("Trail", TRAIL);
        ClassDb.attach("ValueBase", VALUE);
        ServiceBehaviors.install();
        Characters.install();
    }

    static Object[] ret(Object v) {
        return new Object[] {v};
    }

    // ------------------------------------------------------------------ data model

    static final Behavior DATA_MODEL = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "GetService":
                    return ret(self.engine.service(Args.str(a, 1, m)));
                case "FindService":
                    return ret(self.engine.findService(Args.str(a, 1, m)));
                case "BindToClose":
                    self.event("Close").connect(Args.fn(a, 1, m), true);
                    return null;
                case "IsLoaded":
                    return ret(Boolean.TRUE);
                default:
                    return PASS_CALL;
            }
        }

        @Override
        public Object get(Instance self, Interp in, String key) {
            if ("Loaded".equals(key)) {
                return self.event("Loaded");
            }
            return PASS;
        }
    };

    // ------------------------------------------------------------------ pivots and models

    /// Every part in `root` (itself included when it is one).
    static List<Instance> parts(Instance root) {
        List<Instance> out = new ArrayList<Instance>();
        if (root.isA("BasePart")) {
            out.add(root);
        }
        List<Instance> d = root.descendants();
        for (int i = 0; i < d.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (d.get(i).isA("BasePart")) {
                out.add(d.get(i));
            }
        }
        return out;
    }

    static CFrame cf(Instance p) {
        Object v = p.prop("CFrame");
        return v instanceof CFrame ? (CFrame) v : CFrame.IDENTITY;
    }

    static Vector3 size(Instance p) {
        Object v = p.prop("Size");
        return v instanceof Vector3 ? (Vector3) v : Vector3.ONE;
    }

    static CFrame pivot(Instance self) {
        if (self.isA("BasePart")) {
            return cf(self);
        }
        Instance pp = self.ref("PrimaryPart");
        if (pp != null) {
            return cf(pp);
        }
        Object wp = self.prop("WorldPivot");
        List<Instance> ps = parts(self);
        if (ps.isEmpty()) {
            return wp instanceof CFrame ? (CFrame) wp : CFrame.IDENTITY;
        }
        if (self.storedProps().containsKey("WorldPivot")) {
            return (CFrame) wp;
        }
        Object[] box = bounds(ps);
        return (CFrame) box[0];
    }

    /// {center CFrame, size} of an axis-aligned box around the parts.
    static Object[] bounds(List<Instance> ps) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            CFrame c = cf(ps.get(i));
            Vector3 h = size(ps.get(i)).scale(0.5);
            double ex = Math.abs(c.r00) * h.x + Math.abs(c.r01) * h.y + Math.abs(c.r02) * h.z;
            double ey = Math.abs(c.r10) * h.x + Math.abs(c.r11) * h.y + Math.abs(c.r12) * h.z;
            double ez = Math.abs(c.r20) * h.x + Math.abs(c.r21) * h.y + Math.abs(c.r22) * h.z;
            minX = Math.min(minX, c.x - ex);
            maxX = Math.max(maxX, c.x + ex);
            minY = Math.min(minY, c.y - ey);
            maxY = Math.max(maxY, c.y + ey);
            minZ = Math.min(minZ, c.z - ez);
            maxZ = Math.max(maxZ, c.z + ez);
        }
        if (ps.isEmpty()) {
            return new Object[] {CFrame.IDENTITY, Vector3.ZERO};
        }
        return new Object[] {CFrame.at((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2),
            new Vector3(maxX - minX, maxY - minY, maxZ - minZ)};
    }

    static void pivotTo(Instance self, CFrame target) {
        if (self.isA("BasePart")) {
            self.put("CFrame", target);
            return;
        }
        CFrame from = pivot(self);
        CFrame delta = target.mul(from.inverse());
        List<Instance> ps = parts(self);
        for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = ps.get(i);
            p.put("CFrame", delta.mul(cf(p)));
        }
        if (self.storedProps().containsKey("WorldPivot")) {
            self.put("WorldPivot", target);
        }
    }

    static final Behavior PV = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("GetPivot".equals(m)) {
                return ret(pivot(self));
            }
            if ("PivotTo".equals(m)) {
                pivotTo(self, Args.cframe(a, 1, m));
                return null;
            }
            return PASS_CALL;
        }
    };

    static final Behavior MODEL = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "GetBoundingBox": {
                    Object[] b = bounds(parts(self));
                    return new Object[] {b[0], b[1]};
                }
                case "GetExtentsSize":
                    return ret(bounds(parts(self))[1]);
                case "MoveTo": {
                    Vector3 p = Args.vec3(a, 1, m);
                    CFrame cur = pivot(self);
                    Object[] b = bounds(parts(self));
                    double halfH = ((Vector3) b[1]).y / 2;
                    double lift = ((CFrame) b[0]).y - halfH - cur.y;
                    Vector3 dst = p;
                    if (self.engine.getPhysics() != null) {
                        Object[] hit = self.engine.getPhysics().raycast(self.engine, p.add(new Vector3(0, 500, 0)),
                                new Vector3(0, -1000, 0), notIn(self));
                        if (hit != null) {
                            Vector3 hp = (Vector3) hit[1];
                            dst = new Vector3(p.x, Math.max(p.y, hp.y), p.z);
                        }
                    }
                    pivotTo(self, cur.rotation().translate(new Vector3(dst.x, dst.y - lift, dst.z)));
                    return null;
                }
                case "SetPrimaryPartCFrame": {
                    if (self.ref("PrimaryPart") == null) {
                        throw LuaError.rt("Model:SetPrimaryPartCFrame() failed because no PrimaryPart has been set");
                    }
                    pivotTo(self, Args.cframe(a, 1, m));
                    return null;
                }
                case "GetPrimaryPartCFrame": {
                    Instance pp = self.ref("PrimaryPart");
                    return ret(pp == null ? null : cf(pp));
                }
                case "TranslateBy": {
                    pivotTo(self, pivot(self).translate(Args.vec3(a, 1, m)));
                    return null;
                }
                case "GetScale": {
                    Object s = self.getAttribute("__scale");
                    return ret(s == null ? Args.n(1) : s);
                }
                case "ScaleTo": {
                    double to = Args.num(a, 1, m);
                    Object cur = self.getAttribute("__scale");
                    double f = to / (cur instanceof Double ? ((Double) cur).doubleValue() : 1);
                    CFrame pv = pivot(self);
                    List<Instance> ps = parts(self);
                    for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        Instance p = ps.get(i);
                        CFrame c = cf(p);
                        Vector3 off = pv.pointToObject(c.position()).scale(f);
                        Vector3 w = pv.pointToWorld(off);
                        p.put("CFrame", c.withPosition(w.x, w.y, w.z));
                        p.put("Size", size(p).scale(f));
                    }
                    self.setAttribute("__scale", Args.n(to));
                    return null;
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    static Engine.RaycastFilter notIn(final Instance root) {
        return new Engine.RaycastFilter() {
            @Override
            public boolean accepts(Instance p) {
                return p != root && !p.isDescendantOf(root) && p.bool("CanQuery"); // NOPMD CompareObjectsWithEquals - tree identity
            }
        };
    }

    static final Engine.RaycastFilter ALL = new Engine.RaycastFilter() {
        @Override
        public boolean accepts(Instance p) {
            return p.bool("CanQuery");
        }
    };

    // ------------------------------------------------------------------ world queries

    /// A physics hit ({part, position, normal, material}) as the script's
    /// RaycastResult. Slot 3 is the material, not a distance: casting it to
    /// one threw on every raycast that hit, so the distance is measured here.
    static Object result(Object[] hit, Vector3 origin) {
        if (hit == null) {
            return null;
        }
        if (!(hit[0] instanceof Instance) || !(hit[1] instanceof Vector3) || !(hit[2] instanceof Vector3)) {
            return null;
        }
        Vector3 at = (Vector3) hit[1];
        return new RaycastResult((Instance) hit[0], at, (Vector3) hit[2], Args.n(at.sub(origin).magnitude()));
    }

    static final Behavior WORLD = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.engine;
            Engine.PhysicsWorld w = e.getPhysics();
            switch (m) {
                case "Raycast": {
                    Engine.RaycastFilter f = Args.at(a, 3) instanceof QueryParams ? (QueryParams) a[3] : ALL;
                    if (w == null) {
                        return ret(null);
                    }
                    Vector3 from = Args.vec3(a, 1, m);
                    return ret(result(w.raycast(e, from, Args.vec3(a, 2, m), f), from));
                }
                case "Spherecast":
                case "Blockcast":
                case "Shapecast": {
                    if (w == null) {
                        return ret(null);
                    }
                    boolean sphere = "Spherecast".equals(m);
                    Vector3 origin = sphere ? Args.vec3(a, 1, m) : Args.cframe(a, 1, m).position();
                    Vector3 dir = Args.vec3(a, 3, m);
                    Engine.RaycastFilter f = Args.at(a, 4) instanceof QueryParams ? (QueryParams) a[4] : ALL;
                    return ret(result(w.raycast(e, origin, dir, f), origin));
                }
                case "FindPartOnRay":
                case "FindPartOnRayWithIgnoreList": {
                    Object r = Args.at(a, 1);
                    if (!(r instanceof Ray)) {
                        throw Args.bad(a, 1, m, "Ray");
                    }
                    Ray ray = (Ray) r;
                    Engine.RaycastFilter f = ALL;
                    if (Args.at(a, 2) instanceof Instance) {
                        f = notIn((Instance) a[2]);
                    } else if (Args.at(a, 2) instanceof LuaTable) {
                        QueryParams q = new QueryParams(false);
                        q.set(in, "FilterDescendantsInstances", a[2]);
                        f = q;
                    }
                    Object[] hit = w == null ? null : w.raycast(e, ray.origin, ray.direction, f);
                    if (hit == null) {
                        return new Object[] {null, ray.origin.add(ray.direction), Vector3.ZERO, null};
                    }
                    return new Object[] {hit[0], hit[1], hit[2], ((Instance) hit[0]).prop("Material")};
                }
                case "GetPartBoundsInBox": {
                    Engine.RaycastFilter f = Args.at(a, 3) instanceof QueryParams ? (QueryParams) a[3] : ALL;
                    return ret(w == null ? new LuaTable() : Instance.list(w.partsInBox(e, Args.cframe(a, 1, m),
                            Args.vec3(a, 2, m), f)));
                }
                case "GetPartBoundsInRadius": {
                    Vector3 c = Args.vec3(a, 1, m);
                    double r = Args.num(a, 2, m);
                    Engine.RaycastFilter f = Args.at(a, 3) instanceof QueryParams ? (QueryParams) a[3] : ALL;
                    if (w == null) {
                        return ret(new LuaTable());
                    }
                    List<Instance> box = w.partsInBox(e, CFrame.at(c.x, c.y, c.z), new Vector3(r * 2, r * 2, r * 2), f);
                    List<Instance> out = new ArrayList<Instance>();
                    for (int i = 0; i < box.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        Instance p = box.get(i);
                        Vector3 h = size(p).scale(0.5);
                        Vector3 local = cf(p).pointToObject(c);
                        double dx = Math.max(Math.abs(local.x) - h.x, 0);
                        double dy = Math.max(Math.abs(local.y) - h.y, 0);
                        double dz = Math.max(Math.abs(local.z) - h.z, 0);
                        if (dx * dx + dy * dy + dz * dz <= r * r) {
                            out.add(p);
                        }
                    }
                    return ret(Instance.list(out));
                }
                case "GetPartsInPart": {
                    Instance p = Args.inst(a, 1, m);
                    Engine.RaycastFilter f = Args.at(a, 2) instanceof QueryParams ? (QueryParams) a[2] : ALL;
                    if (w == null) {
                        return ret(new LuaTable());
                    }
                    List<Instance> l = w.partsInBox(e, cf(p), size(p), f);
                    l.remove(p);
                    return ret(Instance.list(l));
                }
                case "FindPartsInRegion3": {
                    Object r = Args.at(a, 1);
                    if (!(r instanceof QueryParams.Region3) || w == null) {
                        return ret(new LuaTable());
                    }
                    QueryParams.Region3 rg = (QueryParams.Region3) r;
                    Vector3 c = rg.min.add(rg.max).scale(0.5);
                    Engine.RaycastFilter f = Args.at(a, 2) instanceof Instance ? notIn((Instance) a[2]) : ALL;
                    return ret(Instance.list(w.partsInBox(e, CFrame.at(c.x, c.y, c.z), rg.max.sub(rg.min), f)));
                }
                case "BulkMoveTo": {
                    LuaTable ps = Args.table(a, 1, m);
                    LuaTable cfs = Args.table(a, 2, m);
                    for (int i = 1; i <= ps.length(); i++) {
                        Object p = ps.rawgeti(i);
                        Object c = cfs.rawgeti(i);
                        if (p instanceof Instance && c instanceof CFrame) {
                            ((Instance) p).put("CFrame", c);
                        }
                    }
                    return null;
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior WORKSPACE = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("GetServerTimeNow".equals(m)) {
                return ret(Args.n(System.currentTimeMillis() / 1000.0));
            }
            if ("GetRealPhysicsFPS".equals(m)) {
                return ret(Args.n(60));
            }
            return PASS_CALL;
        }

        @Override
        public Object get(Instance self, Interp in, String key) {
            if ("Terrain".equals(key)) {
                Instance t = self.findFirstChildOfClass("Terrain", false);
                if (t == null) {
                    t = self.engine.create("Terrain");
                    t.setName("Terrain");
                    t.putQuiet("Anchored", Boolean.TRUE);
                    t.putQuiet("Size", new Vector3(2048, 1, 2048));
                    t.putQuiet("Transparency", Args.n(1));
                    t.putQuiet("CanCollide", Boolean.FALSE);
                    t.setParent(self);
                }
                return t;
            }
            return PASS;
        }
    };

    // ------------------------------------------------------------------ parts

    static double mass(Instance p) {
        if (p.bool("Massless")) {
            return 0;
        }
        Vector3 s = size(p);
        Object pp = p.prop("CustomPhysicalProperties");
        double density = pp instanceof QueryParams.PhysicalProperties ? ((QueryParams.PhysicalProperties) pp).density
                : Materials.density(p);
        double vol = s.x * s.y * s.z;
        Object shape = p.prop("Shape");
        if (shape instanceof Enums.EnumItem) {
            String n = ((Enums.EnumItem) shape).name;
            if ("Ball".equals(n)) {
                vol *= Math.PI / 6;
            } else if ("Cylinder".equals(n)) {
                vol *= Math.PI / 4;
            }
        }
        return vol * density;
    }

    static final Behavior PART = new Behavior() {
        @Override
        public Object get(Instance self, Interp in, String key) {
            switch (key) {
                case "Position":
                    return cf(self).position();
                case "Orientation": {
                    double[] e = cf(self).eulerYXZ();
                    return new Vector3(Math.toDegrees(e[0]), Math.toDegrees(e[1]), Math.toDegrees(e[2]));
                }
                case "Rotation": {
                    double[] e = cf(self).eulerXYZ();
                    return new Vector3(Math.toDegrees(e[0]), Math.toDegrees(e[1]), Math.toDegrees(e[2]));
                }
                case "BrickColor":
                    return BrickColor.nearest((Color3) self.prop("Color"));
                case "Velocity":
                    return self.prop("AssemblyLinearVelocity");
                case "RotVelocity":
                    return self.prop("AssemblyAngularVelocity");
                case "Mass":
                case "AssemblyMass":
                    return Args.n(mass(self));
                case "AssemblyRootPart":
                    return self;
                case "ExtentsSize":
                    return size(self);
                case "PivotOffset":
                    return CFrame.IDENTITY;
                case "ResizeIncrement":
                    return Args.n(1);
                default:
                    return PASS;
            }
        }

        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            switch (key) {
                case "Position": {
                    if (!(v instanceof Vector3)) {
                        throw LuaError.rt("Unable to assign property Position. Vector3 expected, got " + Interp.typeName(v));
                    }
                    Vector3 p = (Vector3) v;
                    self.put("CFrame", cf(self).withPosition(p.x, p.y, p.z));
                    return true;
                }
                case "Orientation":
                case "Rotation": {
                    if (!(v instanceof Vector3)) {
                        throw LuaError.rt("Unable to assign property " + key + ". Vector3 expected");
                    }
                    Vector3 o = (Vector3) v;
                    CFrame c = cf(self);
                    CFrame r = "Orientation".equals(key)
                            ? CFrame.orientation(Math.toRadians(o.x), Math.toRadians(o.y), Math.toRadians(o.z))
                            : CFrame.angles(Math.toRadians(o.x), Math.toRadians(o.y), Math.toRadians(o.z));
                    self.put("CFrame", r.withPosition(c.x, c.y, c.z));
                    return true;
                }
                case "BrickColor": {
                    if (!(v instanceof BrickColor)) {
                        throw LuaError.rt("Unable to assign property BrickColor. BrickColor expected");
                    }
                    self.put("Color", ((BrickColor) v).color());
                    return true;
                }
                case "Velocity":
                    self.put("AssemblyLinearVelocity", self.coerce("AssemblyLinearVelocity", v));
                    return true;
                case "RotVelocity":
                    self.put("AssemblyAngularVelocity", self.coerce("AssemblyAngularVelocity", v));
                    return true;
                case "Size": {
                    if (!(v instanceof Vector3)) {
                        throw LuaError.rt("Unable to assign property Size. Vector3 expected, got " + Interp.typeName(v));
                    }
                    Vector3 s = (Vector3) v;
                    self.put("Size", new Vector3(Math.max(0.001, s.x), Math.max(0.001, s.y), Math.max(0.001, s.z)));
                    return true;
                }
                case "PivotOffset":
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "GetMass":
                    return ret(Args.n(mass(self)));
                case "ApplyImpulse":
                case "ApplyImpulseAtPosition": {
                    double ms = Math.max(mass(self), 1e-6);
                    Vector3 j = Args.vec3(a, 1, m);
                    Vector3 v = (Vector3) self.prop("AssemblyLinearVelocity");
                    self.put("AssemblyLinearVelocity", v.add(j.scale(1 / ms)));
                    return null;
                }
                case "ApplyAngularImpulse": {
                    double ms = Math.max(mass(self), 1e-6);
                    Vector3 j = Args.vec3(a, 1, m);
                    Vector3 v = (Vector3) self.prop("AssemblyAngularVelocity");
                    self.put("AssemblyAngularVelocity", v.add(j.scale(1 / ms)));
                    return null;
                }
                case "GetTouchingParts": {
                    Engine.PhysicsWorld w = self.engine.getPhysics();
                    if (w == null) {
                        return ret(new LuaTable());
                    }
                    List<Instance> l = w.partsInBox(self.engine, cf(self), size(self).add(new Vector3(0.02, 0.02, 0.02)), ALL);
                    l.remove(self);
                    return ret(Instance.list(l));
                }
                case "GetConnectedParts":
                    return ret(Instance.list(Joints.connected(self, Args.bool(a, 1))));
                case "GetRootPart":
                    return ret(self);
                case "GetJoints":
                    return ret(Instance.list(Joints.of(self)));
                case "BreakJoints": {
                    List<Instance> js = Joints.of(self);
                    for (int i = 0; i < js.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        js.get(i).destroy();
                    }
                    return null;
                }
                case "CanCollideWith": {
                    Instance o = Args.inst(a, 1, m);
                    return ret(Boolean.valueOf(self.bool("CanCollide") && o.bool("CanCollide")
                            && ServiceBehaviors.groupsCollide(self.engine, self.str("CollisionGroup"), o.str("CollisionGroup"))));
                }
                case "SetNetworkOwner":
                case "SetNetworkOwnershipAuto":
                    return null;
                case "GetNetworkOwner":
                    return ret(null);
                case "IsGrounded":
                    return ret(Boolean.valueOf(self.bool("Anchored")));
                case "GetVelocityAtPosition": {
                    Vector3 p = Args.vec3(a, 1, m);
                    Vector3 v = (Vector3) self.prop("AssemblyLinearVelocity");
                    Vector3 w = (Vector3) self.prop("AssemblyAngularVelocity");
                    return ret(v.add(w.cross(p.sub(cf(self).position()))));
                }
                case "Resize": {
                    Object n = Args.at(a, 1);
                    double d = Args.num(a, 2, m);
                    if (!(n instanceof Enums.EnumItem)) {
                        throw Args.bad(a, 1, m, "NormalId");
                    }
                    int f = ((Enums.EnumItem) n).value;
                    double[][] dirs = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {-1, 0, 0}, {0, -1, 0}, {0, 0, -1}};
                    Vector3 axis = new Vector3(Math.abs(dirs[f][0]), Math.abs(dirs[f][1]), Math.abs(dirs[f][2]));
                    Vector3 s = size(self).add(axis.scale(d));
                    CFrame c = cf(self);
                    Vector3 shift = c.vectorToWorld(new Vector3(dirs[f][0], dirs[f][1], dirs[f][2]).scale(d / 2));
                    self.put("Size", s);
                    self.put("CFrame", c.translate(shift));
                    return ret(Boolean.TRUE);
                }
                case "SubtractAsync":
                case "UnionAsync":
                case "IntersectAsync": {
                    // Solid modelling is approximated: the result is a union
                    // part with this part's shape, so scripts keep running.
                    Instance u = self.engine.create("UnionOperation");
                    for (String k : new String[] {"CFrame", "Size", "Color", "Material", "Anchored", "Transparency"}) {
                        u.putQuiet(k, self.prop(k));
                    }
                    self.engine.unsupported("BasePart", m + " (approximated)");
                    return ret(u);
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ attachments

    static CFrame attachmentWorld(Instance att) {
        Instance p = att.getParent();
        CFrame local = (CFrame) att.prop("CFrame");
        if (p != null && p.isA("BasePart")) {
            return cf(p).mul(local);
        }
        if (p != null && p.isA("Attachment")) {
            return attachmentWorld(p).mul(local);
        }
        return local;
    }

    static final Behavior ATTACHMENT = new Behavior() {
        @Override
        public Object get(Instance self, Interp in, String key) {
            switch (key) {
                case "Position":
                    return ((CFrame) self.prop("CFrame")).position();
                case "Orientation": {
                    double[] e = ((CFrame) self.prop("CFrame")).eulerYXZ();
                    return new Vector3(Math.toDegrees(e[0]), Math.toDegrees(e[1]), Math.toDegrees(e[2]));
                }
                case "WorldCFrame":
                    return attachmentWorld(self);
                case "WorldPosition":
                    return attachmentWorld(self).position();
                case "WorldAxis":
                    return attachmentWorld(self).right();
                case "WorldSecondaryAxis":
                    return attachmentWorld(self).up();
                default:
                    return PASS;
            }
        }

        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            CFrame c = (CFrame) self.prop("CFrame");
            switch (key) {
                case "Position": {
                    Vector3 p = Args.vec3(new Object[] {v}, 0, "Position");
                    self.put("CFrame", c.withPosition(p.x, p.y, p.z));
                    return true;
                }
                case "Orientation": {
                    Vector3 o = Args.vec3(new Object[] {v}, 0, "Orientation");
                    self.put("CFrame", CFrame.orientation(Math.toRadians(o.x), Math.toRadians(o.y),
                            Math.toRadians(o.z)).withPosition(c.x, c.y, c.z));
                    return true;
                }
                case "WorldCFrame":
                case "WorldPosition": {
                    Instance p = self.getParent();
                    CFrame base = p != null && p.isA("BasePart") ? cf(p) : CFrame.IDENTITY;
                    CFrame w = v instanceof CFrame ? (CFrame) v
                            : attachmentWorld(self).withPosition(((Vector3) v).x, ((Vector3) v).y, ((Vector3) v).z);
                    self.put("CFrame", base.inverse().mul(w));
                    return true;
                }
                default:
                    return false;
            }
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("GetConstraints".equals(m)) {
                List<Instance> out = new ArrayList<Instance>();
                List<Instance> all = self.engine.getWorkspace().descendants();
                for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                    Instance c = all.get(i);
                    if (c.isA("Constraint") && (c.ref("Attachment0") == self || c.ref("Attachment1") == self)) { // NOPMD CompareObjectsWithEquals - reference identity
                        out.add(c);
                    }
                }
                return ret(Instance.list(out));
            }
            return PASS_CALL;
        }
    };

    // ------------------------------------------------------------------ camera

    static final Behavior CAMERA = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            CFrame c = (CFrame) self.prop("CFrame");
            Vector2 vp = (Vector2) self.prop("ViewportSize");
            double fov = Math.toRadians(self.num("FieldOfView"));
            double tanY = Math.tan(fov / 2);
            double aspect = vp.x / Math.max(1, vp.y);
            switch (m) {
                case "WorldToScreenPoint":
                case "WorldToViewportPoint": {
                    Vector3 local = c.pointToObject(Args.vec3(a, 1, m));
                    double depth = -local.z;
                    if (depth == 0) {
                        depth = 1e-6;
                    }
                    double nx = local.x / (depth * tanY * aspect);
                    double ny = local.y / (depth * tanY);
                    double sx = (nx + 1) / 2 * vp.x;
                    double sy = (1 - ny) / 2 * vp.y;
                    boolean on = depth > 0 && nx >= -1 && nx <= 1 && ny >= -1 && ny <= 1;
                    return new Object[] {new Vector3(sx, sy, depth), Boolean.valueOf(on)};
                }
                case "ScreenPointToRay":
                case "ViewportPointToRay": {
                    double sx = Args.num(a, 1, m);
                    double sy = Args.num(a, 2, m);
                    double depth = Args.optNum(a, 3, 1);
                    double nx = sx / vp.x * 2 - 1;
                    double ny = 1 - sy / vp.y * 2;
                    Vector3 dir = c.vectorToWorld(new Vector3(nx * tanY * aspect, ny * tanY, -1)).unit();
                    return ret(new Ray(c.position().add(dir.scale(depth)), dir));
                }
                case "ZoomToExtents":
                case "Interpolate":
                    return null;
                case "GetPartsObscuringTarget":
                    return ret(new LuaTable());
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ values

    static final Behavior VALUE = new Behavior() {
        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            if ("Value".equals(key) && (self.isA("IntValue") || self.isA("IntConstrainedValue"))) {
                Double d = Interp.toNumber(v);
                if (d == null) {
                    throw LuaError.rt("Unable to assign property Value. int64 expected, got " + Interp.typeName(v));
                }
                double n = Math.floor(d.doubleValue());
                if (self.isA("IntConstrainedValue")) {
                    n = Math.max(self.num("MinValue"), Math.min(self.num("MaxValue"), n));
                }
                self.put("Value", Args.n(n));
                return true;
            }
            if ("Value".equals(key) && self.isA("DoubleConstrainedValue")) {
                Double d = Interp.toNumber(v);
                if (d == null) {
                    throw LuaError.rt("Unable to assign property Value. double expected");
                }
                self.put("Value", Args.n(Math.max(self.num("MinValue"), Math.min(self.num("MaxValue"), d.doubleValue()))));
                return true;
            }
            return false;
        }
    };

    // ------------------------------------------------------------------ remotes and bindables

    static final Behavior REMOTE_EVENT = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.engine;
            switch (m) {
                case "FireServer":
                    if (e.getRole() == Engine.SERVER) {
                        throw LuaError.rt("FireServer can only be called from the client");
                    }
                    Network.fireServer(e, self, rest(a, 1));
                    return null;
                case "FireClient": {
                    if (e.getRole() == Engine.CLIENT) {
                        throw LuaError.rt("FireClient can only be called from the server");
                    }
                    Instance p = Args.inst(a, 1, m);
                    if (!p.isA("Player")) {
                        throw LuaError.rt("FireClient: player argument must be a Player object");
                    }
                    Network.fireClient(e, self, p, rest(a, 2));
                    return null;
                }
                case "FireAllClients":
                    if (e.getRole() == Engine.CLIENT) {
                        throw LuaError.rt("FireAllClients can only be called from the server");
                    }
                    Network.fireAllClients(e, self, rest(a, 1));
                    return null;
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior REMOTE_FUNCTION = new Behavior() {
        @Override
        public Object get(Instance self, Interp in, String key) {
            if ("OnServerInvoke".equals(key) || "OnClientInvoke".equals(key)) {
                throw LuaError.rt(key + " is a callback member of RemoteFunction; you can only set the callback value, get is not available");
            }
            return PASS;
        }

        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            if ("OnServerInvoke".equals(key) || "OnClientInvoke".equals(key)) {
                if (v != null && !(v instanceof LuaFunction)) {
                    throw LuaError.rt(key + " must be a function");
                }
                self.putQuiet(key, v);
                return true;
            }
            return false;
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.engine;
            switch (m) {
                case "InvokeServer":
                    return Network.invokeServer(e, self, rest(a, 1));
                case "InvokeClient": {
                    Instance p = Args.inst(a, 1, m);
                    return Network.invokeClient(e, self, p, rest(a, 2));
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior BINDABLE_EVENT = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("Fire".equals(m)) {
                self.event("Event").fire(rest(a, 1));
                return null;
            }
            return PASS_CALL;
        }
    };

    static final Behavior BINDABLE_FUNCTION = new Behavior() {
        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            if ("OnInvoke".equals(key)) {
                self.putQuiet(key, v);
                return true;
            }
            return false;
        }

        @Override
        public Object get(Instance self, Interp in, String key) {
            if ("OnInvoke".equals(key)) {
                throw LuaError.rt("OnInvoke is a callback member of BindableFunction; you can only set the callback value");
            }
            return PASS;
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("Invoke".equals(m)) {
                Object cb = self.prop("OnInvoke");
                if (!(cb instanceof LuaFunction)) {
                    // The engine queues until a callback is set; with nothing
                    // ever set that waits forever, which is never wanted.
                    throw LuaError.rt("BindableFunction:Invoke() has no OnInvoke callback");
                }
                return self.engine.invoke((LuaFunction) cb, rest(a, 1));
            }
            return PASS_CALL;
        }
    };

    static Object[] rest(Object[] a, int from) {
        if (a.length <= from) {
            return new Object[0];
        }
        Object[] r = new Object[a.length - from];
        System.arraycopy(a, from, r, 0, r.length);
        return r;
    }

    // ------------------------------------------------------------------ sound and effects

    static final Behavior SOUND = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "Play":
                    self.put("TimePosition", Args.n(0));
                    self.put("Playing", Boolean.TRUE);
                    self.put("IsPlaying", Boolean.TRUE);
                    self.fireEvent("Played", self.prop("SoundId"));
                    return null;
                case "Resume":
                    self.put("Playing", Boolean.TRUE);
                    self.put("IsPlaying", Boolean.TRUE);
                    self.fireEvent("Resumed", self.prop("SoundId"));
                    return null;
                case "Pause":
                    self.put("Playing", Boolean.FALSE);
                    self.put("IsPlaying", Boolean.FALSE);
                    self.fireEvent("Paused", self.prop("SoundId"));
                    return null;
                case "Stop":
                    self.put("Playing", Boolean.FALSE);
                    self.put("IsPlaying", Boolean.FALSE);
                    self.put("TimePosition", Args.n(0));
                    self.fireEvent("Stopped", self.prop("SoundId"));
                    return null;
                default:
                    return PASS_CALL;
            }
        }

        @Override
        public void ancestryChanged(Instance self) {
            if (self.getParent() == null && self.bool("PlayOnRemove")) {
                self.engine.effect(self, "PlayOnRemove", null);
            }
        }
    };

    static final Behavior PARTICLES = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("Emit".equals(m)) {
                self.engine.effect(self, "Emit", Args.n(Args.optNum(a, 1, 16)));
                return null;
            }
            if ("Clear".equals(m)) {
                self.engine.effect(self, "Clear", null);
                return null;
            }
            return PASS_CALL;
        }
    };

    static final Behavior TRAIL = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("Clear".equals(m)) {
                self.engine.effect(self, "Clear", null);
                return null;
            }
            return PASS_CALL;
        }
    };

    static final Behavior EXPLOSION = new Behavior() {
        @Override
        public void ancestryChanged(final Instance self) {
            if (self.host != null || !self.isDescendantOf(self.engine.getWorkspace())) {
                return;
            }
            self.host = Boolean.TRUE;
            self.engine.later(0, new Runnable() {
                @Override
                public void run() {
                    explode(self);
                }
            });
        }
    };

    static void explode(Instance ex) {
        Engine e = ex.engine;
        Vector3 c = (Vector3) ex.prop("Position");
        double r = ex.num("BlastRadius");
        double pressure = ex.num("BlastPressure");
        e.effect(ex, "Explode", null);
        List<Instance> all = parts(e.getWorkspace());
        List<Instance> hitHumanoids = new ArrayList<Instance>();
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = all.get(i);
            Vector3 d = cf(p).position().sub(c);
            double dist = d.magnitude();
            if (dist > r) {
                continue;
            }
            ex.fireEvent("Hit", p, Args.n(dist));
            if (!p.bool("Anchored") && pressure > 0) {
                double ms = Math.max(mass(p), 1e-3);
                Vector3 push = d.unit().scale(pressure / 10000.0 * (1 - dist / Math.max(r, 1e-3)) / Math.max(ms / 10, 1));
                p.put("AssemblyLinearVelocity", ((Vector3) p.prop("AssemblyLinearVelocity")).add(push));
            }
            Instance model = p.getParent();
            if (model != null && ex.num("DestroyJointRadiusPercent") > 0
                    && dist <= r * ex.num("DestroyJointRadiusPercent")) {
                Instance h = model.findFirstChildOfClass("Humanoid", false);
                if (h != null && !hitHumanoids.contains(h) && model.findFirstChildOfClass("ForceField", false) == null) {
                    hitHumanoids.add(h);
                    h.put("Health", Args.n(0));
                }
            }
        }
        e.later(1, new Runnable() {
            @Override
            public void run() {
                if (!ex.isDestroyed()) {
                    ex.destroy();
                }
            }
        });
    }

    // ------------------------------------------------------------------ tools and seats

    static final Behavior TOOL = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("Activate".equals(m)) {
                if (self.bool("Enabled")) {
                    self.fireEvent("Activated");
                }
                return null;
            }
            if ("Deactivate".equals(m)) {
                self.fireEvent("Deactivated");
                return null;
            }
            return PASS_CALL;
        }

        @Override
        public void ancestryChanged(Instance self) {
            Instance p = self.getParent();
            boolean equipped = p != null && p.findFirstChildOfClass("Humanoid", false) != null;
            boolean was = Boolean.TRUE.equals(self.host);
            if (equipped && !was) {
                self.host = Boolean.TRUE;
                Characters.gripTool(self, p);
                self.fireEvent("Equipped", ServiceBehaviors.mouse(self.engine));
            } else if (!equipped && was) {
                self.host = Boolean.FALSE;
                self.fireEvent("Unequipped");
            }
        }
    };

    static final Behavior SEAT = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("Sit".equals(m)) {
                Characters.sit(Args.inst(a, 1, m), self);
                return null;
            }
            return PASS_CALL;
        }
    };

    // ------------------------------------------------------------------ scripts

    static final Behavior SCRIPT = new Behavior() {
        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            if ("Source".equals(key) && (self.engine == null || !self.engine.inCommand())) {
                // Scripts cannot rewrite code at run time in the engine this is
                // compatible with; the studio writes Source through put(), and
                // its command bar (where an editor's plugins run) may set it.
                throw LuaError.rt("The current thread cannot write 'Source' (lacking capability PluginOrOpenCloud)");
            }
            return false;
        }
    };

    // ------------------------------------------------------------------ lighting

    static final Behavior LIGHTING = new Behavior() {
        @Override
        public Object get(Instance self, Interp in, String key) {
            if ("TimeOfDay".equals(key)) {
                double t = self.num("ClockTime");
                int h = (int) Math.floor(t);
                int mi = (int) Math.floor((t - h) * 60);
                int s = (int) Math.floor(((t - h) * 60 - mi) * 60);
                return two(h) + ":" + two(mi) + ":" + two(s);
            }
            return PASS;
        }

        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            if ("TimeOfDay".equals(key)) {
                String s = v instanceof String ? (String) v : "14:00:00";
                double t = 0;
                double mul = 1;
                int start = 0;
                for (int i = 0; i <= s.length(); i++) {
                    if (i == s.length() || s.charAt(i) == ':') {
                        try {
                            t += Double.parseDouble(s.substring(start, i)) * mul;
                        } catch (NumberFormatException e) {
                            break;
                        }
                        mul /= 60;
                        start = i + 1;
                    }
                }
                self.put("ClockTime", Args.n(((t % 24) + 24) % 24));
                return true;
            }
            if ("ClockTime".equals(key)) {
                Double d = Interp.toNumber(v);
                self.put("ClockTime", Args.n(d == null ? 14 : ((d.doubleValue() % 24) + 24) % 24));
                return true;
            }
            return false;
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "GetSunDirection":
                    return ret(sunDirection(self.num("ClockTime"), self.num("GeographicLatitude")));
                case "GetMoonDirection":
                    return ret(sunDirection(self.num("ClockTime"), self.num("GeographicLatitude")).negateV());
                case "GetMinutesAfterMidnight":
                    return ret(Args.n(self.num("ClockTime") * 60));
                case "SetMinutesAfterMidnight":
                    self.put("ClockTime", Args.n(((Args.num(a, 1, m) / 60) % 24 + 24) % 24));
                    return null;
                default:
                    return PASS_CALL;
            }
        }

        @Override
        public void changed(Instance self, String prop) {
            self.fireEvent("LightingChanged", Boolean.FALSE);
        }
    };

    /// The direction towards the sun for a clock time, as a unit vector.
    static Vector3 sunDirection(double clock, double latitude) {
        double hourAngle = (clock - 12) / 24 * Math.PI * 2;
        double lat = Math.toRadians(latitude) * 0.5;
        double y = Math.cos(hourAngle) * Math.cos(lat);
        double x = -Math.sin(hourAngle);
        double z = Math.cos(hourAngle) * Math.sin(lat);
        return new Vector3(x, y, z).unit();
    }

    static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    // ------------------------------------------------------------------ tweens

    static final Behavior TWEEN = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Tweens.State s = (Tweens.State) self.host;
            switch (m) {
                case "Play":
                    Tweens.play(self.engine, self, s);
                    return null;
                case "Pause":
                    Tweens.pause(self, s);
                    return null;
                case "Cancel":
                    Tweens.cancel(self.engine, self, s);
                    return null;
                default:
                    return PASS_CALL;
            }
        }

        @Override
        public Object get(Instance self, Interp in, String key) {
            if ("TweenInfo".equals(key)) {
                return ((Tweens.State) self.host).info;
            }
            return PASS;
        }
    };

    static void tweens(Engine e, double dt) {
        Tweens.step(e, dt);
        Characters.animate(e, dt);
    }

    // ------------------------------------------------------------------ GUI

    static final Behavior GUI = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "TweenPosition":
                    return ret(Boolean.valueOf(Tweens.guiTween(self, Args.at(a, 1), null, a, 2)));
                case "TweenSize":
                    return ret(Boolean.valueOf(Tweens.guiTween(self, null, Args.at(a, 1), a, 2)));
                case "TweenSizeAndPosition":
                    return ret(Boolean.valueOf(Tweens.guiTween(self, Args.at(a, 2), Args.at(a, 1), a, 3)));
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior TEXT_BOX = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "CaptureFocus":
                    self.host = Boolean.TRUE;
                    if (self.bool("ClearTextOnFocus")) {
                        self.put("Text", "");
                    }
                    self.fireEvent("Focused");
                    self.engine.effect(self, "Focus", Boolean.TRUE);
                    return null;
                case "ReleaseFocus":
                    self.host = Boolean.FALSE;
                    self.fireEvent("FocusLost", Boolean.valueOf(Args.bool(a, 1)), null);
                    self.engine.effect(self, "Focus", Boolean.FALSE);
                    return null;
                case "IsFocused":
                    return ret(Boolean.valueOf(Boolean.TRUE.equals(self.host)));
                default:
                    return PASS_CALL;
            }
        }
    };

    /// Per-frame client work: GUI layout and render-step bindings.
    static void renderStep(Engine e, double dt) {
        GuiLayout.layout(e);
        ServiceBehaviors.renderBindings(e, dt);
    }

    static void purchaseFinished(Engine e, Instance player, String kind, long id, boolean purchased) {
        ServiceBehaviors.purchaseFinished(e, player, kind, id, purchased);
    }
}
