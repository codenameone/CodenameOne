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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Interp;
import com.codenameone.examples.brickyard.luau.LuaError;
import com.codenameone.examples.brickyard.luau.LuaTable;
import java.util.ArrayList;
import java.util.List;

/// `RaycastParams` and `OverlapParams` (mutable, as in the engine), plus the
/// small immutable value types the spatial and time APIs use.
public final class QueryParams extends RbxType implements Engine.RaycastFilter {
    private final boolean overlap;
    private final List<Instance> filter = new ArrayList<Instance>();
    private boolean include;
    private boolean ignoreWater;
    private boolean respectCanCollide;
    private String group = "Default";
    private int maxParts;

    public QueryParams(boolean overlap) {
        this.overlap = overlap;
    }

    @Override
    public String typeName() {
        return overlap ? "OverlapParams" : "RaycastParams";
    }

    @Override
    public boolean accepts(Instance part) {
        if (!part.bool("CanQuery")) {
            return false;
        }
        if (respectCanCollide && !part.bool("CanCollide")) {
            return false;
        }
        boolean listed = false;
        for (int i = 0; i < filter.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance f = filter.get(i);
            if (part == f || part.isDescendantOf(f)) { // NOPMD CompareObjectsWithEquals - tree identity
                listed = true;
                break;
            }
        }
        return include ? listed : !listed;
    }

    public int getMaxParts() {
        return maxParts;
    }

    private static final Method ADD = new Method("AddToFilter", 0);

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "FilterDescendantsInstances":
                return Instance.list(filter);
            case "FilterType":
                return Enums.item("RaycastFilterType", include ? "Include" : "Exclude");
            case "IgnoreWater":
                return Boolean.valueOf(ignoreWater);
            case "RespectCanCollide":
                return Boolean.valueOf(respectCanCollide);
            case "CollisionGroup":
                return group;
            case "MaxParts":
                return Args.n(maxParts);
            case "AddToFilter":
                return ADD;
            default:
                return super.get(in, k);
        }
    }

    @Override
    public void set(Interp in, String k, Object v) {
        switch (k) {
            case "FilterDescendantsInstances": {
                if (!(v instanceof LuaTable)) {
                    throw LuaError.rt("Unable to assign property FilterDescendantsInstances. table expected");
                }
                filter.clear();
                addAll((LuaTable) v);
                return;
            }
            case "FilterType":
                include = v instanceof Enums.EnumItem && ("Include".equals(((Enums.EnumItem) v).name)
                        || "Whitelist".equals(((Enums.EnumItem) v).name));
                return;
            case "IgnoreWater":
                ignoreWater = Interp.truthy(v);
                return;
            case "RespectCanCollide":
                respectCanCollide = Interp.truthy(v);
                return;
            case "CollisionGroup":
                group = v instanceof String ? (String) v : "Default";
                return;
            case "MaxParts":
                maxParts = v instanceof Double ? ((Double) v).intValue() : 0;
                return;
            default:
                super.set(in, k, v);
        }
    }

    private void addAll(LuaTable t) {
        for (int i = 1; i <= t.length(); i++) {
            Object o = t.rawgeti(i);
            if (o instanceof Instance) {
                filter.add((Instance) o);
            }
        }
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        Object v = Args.at(a, 1);
        if (v instanceof Instance) {
            filter.add((Instance) v);
        } else if (v instanceof LuaTable) {
            addAll((LuaTable) v);
        }
        return null;
    }

    /// `PhysicalProperties`.
    public static final class PhysicalProperties extends RbxType {
        public final double density;
        public final double friction;
        public final double elasticity;
        public final double frictionWeight;
        public final double elasticityWeight;

        public PhysicalProperties(double density, double friction, double elasticity, double fw, double ew) {
            this.density = density;
            this.friction = friction;
            this.elasticity = elasticity;
            this.frictionWeight = fw;
            this.elasticityWeight = ew;
        }

        @Override
        public String typeName() {
            return "PhysicalProperties";
        }

        @Override
        public Object get(Interp in, String k) {
            switch (k) {
                case "Density":
                    return Args.n(density);
                case "Friction":
                    return Args.n(friction);
                case "Elasticity":
                    return Args.n(elasticity);
                case "FrictionWeight":
                    return Args.n(frictionWeight);
                case "ElasticityWeight":
                    return Args.n(elasticityWeight);
                default:
                    return super.get(in, k);
            }
        }
    }

    /// `Region3`: an axis-aligned box.
    public static final class Region3 extends RbxType {
        public final Vector3 min;
        public final Vector3 max;

        public Region3(Vector3 a, Vector3 b) {
            min = new Vector3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
            max = new Vector3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
        }

        @Override
        public String typeName() {
            return "Region3";
        }

        @Override
        public Object get(Interp in, String k) {
            if ("CFrame".equals(k)) {
                Vector3 c = min.add(max).scale(0.5);
                return CFrame.at(c.x, c.y, c.z);
            }
            if ("Size".equals(k)) {
                return max.sub(min);
            }
            return super.get(in, k);
        }
    }

    /// `Rect`.
    public static final class Rect extends RbxType {
        public final Vector2 min;
        public final Vector2 max;

        public Rect(double x0, double y0, double x1, double y1) {
            min = new Vector2(x0, y0);
            max = new Vector2(x1, y1);
        }

        @Override
        public String typeName() {
            return "Rect";
        }

        @Override
        public Object get(Interp in, String k) {
            switch (k) {
                case "Min":
                    return min;
                case "Max":
                    return max;
                case "Width":
                    return Args.n(max.x - min.x);
                case "Height":
                    return Args.n(max.y - min.y);
                default:
                    return super.get(in, k);
            }
        }
    }

    /// `DateTime`.
    public static final class DateTime extends RbxType {
        public final long millis;

        public DateTime(long millis) {
            this.millis = millis;
        }

        @Override
        public String typeName() {
            return "DateTime";
        }

        private static final Method ISO = new Method("ToIsoDate", 0);

        @Override
        public Object get(Interp in, String k) {
            if ("UnixTimestamp".equals(k)) {
                return Args.n(Math.floor(millis / 1000.0));
            }
            if ("UnixTimestampMillis".equals(k)) {
                return Args.n(millis);
            }
            if ("ToIsoDate".equals(k)) {
                return ISO;
            }
            return super.get(in, k);
        }

        @Override
        protected Object[] invoke(int id, Interp in, Object[] a) {
            long days = floorDiv(millis, 86_400_000L);
            long ms = millis - days * 86_400_000L;
            long z = days + 719468;
            long era = floorDiv(z, 146097);
            long doe = z - era * 146097;
            long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
            long y = yoe + era * 400;
            long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
            long mp = (5 * doy + 2) / 153;
            long d = doy - (153 * mp + 2) / 5 + 1;
            long m = mp < 10 ? mp + 3 : mp - 9;
            if (m <= 2) {
                y++;
            }
            return ret(y + "-" + two(m) + "-" + two(d) + "T" + two(ms / 3_600_000) + ":" + two(ms / 60_000 % 60)
                    + ":" + two(ms / 1000 % 60) + "Z");
        }

        private static long floorDiv(long a, long b) {
            long q = a / b;
            return (a % b != 0 && (a < 0) != (b < 0)) ? q - 1 : q;
        }

        private static String two(long v) {
            return v < 10 ? "0" + v : String.valueOf(v);
        }
    }

    /// `PathWaypoint`.
    public static final class Waypoint extends RbxType {
        public final Vector3 position;
        public final String action;

        public Waypoint(Vector3 position, String action) {
            this.position = position;
            this.action = action;
        }

        @Override
        public String typeName() {
            return "PathWaypoint";
        }

        @Override
        public Object get(Interp in, String k) {
            if ("Position".equals(k)) {
                return position;
            }
            if ("Action".equals(k)) {
                return Enums.item("PathWaypointAction", action);
            }
            if ("Label".equals(k)) {
                return "";
            }
            return super.get(in, k);
        }
    }

    /// `Font` (a font face by family name).
    public static final class FontFace extends RbxType {
        public final String family;

        public FontFace(String family) {
            this.family = family;
        }

        @Override
        public String typeName() {
            return "Font";
        }

        @Override
        public Object get(Interp in, String k) {
            if ("Family".equals(k)) {
                return family;
            }
            if ("Bold".equals(k)) {
                return Boolean.valueOf(family.indexOf("Bold") >= 0);
            }
            return super.get(in, k);
        }
    }
}
