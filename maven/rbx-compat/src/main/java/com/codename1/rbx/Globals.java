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

import com.codename1.luau.Coroutine;
import com.codename1.luau.Interp;
import com.codename1.luau.JavaFunction;
import com.codename1.luau.LuaClosure;
import com.codename1.luau.LuaError;
import com.codename1.luau.LuaFunction;
import com.codename1.luau.LuaTable;

/// Installs the engine globals: `game`, `workspace`, `Instance`, the
/// datatype constructors, `task`, `wait`/`spawn`/`delay`, `tick`, `time`,
/// `warn`, `print` (to the engine output) and `require`.
final class Globals {
    private Globals() {
    }

    /// `require`, written in Luau so a module may yield while it loads.
    private static final String PRELUDE = ""
            + "local cache, loading = {}, {}\n"
            + "local loadmodule = __loadmodule\n"
            + "__loadmodule = nil\n"
            + "function require(m)\n"
            + "  if typeof(m) ~= 'Instance' then error('require: only ModuleScripts are supported; asset ids are not', 2) end\n"
            + "  if not m:IsA('ModuleScript') then error('Attempted to call require with invalid argument(s).', 2) end\n"
            + "  local c = cache[m]\n"
            + "  if c then return c[1] end\n"
            + "  if loading[m] == coroutine.running() then error('Requested module was required recursively', 2) end\n"
            + "  while loading[m] do task.wait() end\n"
            + "  c = cache[m]\n"
            + "  if c then return c[1] end\n"
            + "  loading[m] = coroutine.running()\n"
            + "  local ok, r = pcall(loadmodule(m))\n"
            + "  loading[m] = nil\n"
            + "  if not ok then error('Requested module experienced an error while loading: ' .. tostring(r), 2) end\n"
            + "  if r == nil then error('Module code did not return exactly one value', 2) end\n"
            + "  cache[m] = {r}\n"
            + "  return r\n"
            + "end\n"
            + "return function(cb, done, ...) done(pcall(cb, ...)) end\n";

    private static final class Fn extends JavaFunction {
        private final Engine e;
        private final int id;

        Fn(Engine e, String name, int id) {
            super(name);
            this.e = e;
            this.id = id;
        }

        @Override
        public Object[] call(Interp in, Object[] a) {
            return Globals.call(e, id, name(), in, a);
        }
    }

    private static void fn(Engine e, LuaTable t, String name, int id) {
        t.rawset(name, new Fn(e, name, id));
    }

    private static LuaTable lib(LuaTable g, String name) {
        LuaTable t = new LuaTable();
        g.rawset(name, t);
        return t;
    }

    static void install(Engine e) {
        Interp in = e.in;
        LuaTable g = in.getGlobals();
        g.rawset("game", e.game);
        g.rawset("Game", e.game);
        g.rawset("workspace", e.getWorkspace());
        g.rawset("Workspace", e.getWorkspace());
        g.rawset("Enum", Enums.ROOT);
        g.rawset("_VERSION", "Luau");
        g.rawset("shared", new LuaTable());

        LuaTable inst = lib(g, "Instance");
        fn(e, inst, "new", 0);
        fn(e, inst, "fromExisting", 1);
        LuaTable v3 = lib(g, "Vector3");
        fn(e, v3, "new", 10);
        fn(e, v3, "FromNormalId", 11);
        fn(e, v3, "FromAxis", 12);
        v3.rawset("zero", Vector3.ZERO);
        v3.rawset("one", Vector3.ONE);
        v3.rawset("xAxis", Vector3.X_AXIS);
        v3.rawset("yAxis", Vector3.Y_AXIS);
        v3.rawset("zAxis", Vector3.Z_AXIS);
        LuaTable v2 = lib(g, "Vector2");
        fn(e, v2, "new", 20);
        v2.rawset("zero", Vector2.ZERO);
        v2.rawset("one", new Vector2(1, 1));
        v2.rawset("xAxis", new Vector2(1, 0));
        v2.rawset("yAxis", new Vector2(0, 1));
        LuaTable cf = lib(g, "CFrame");
        fn(e, cf, "new", 30);
        fn(e, cf, "Angles", 31);
        fn(e, cf, "fromEulerAnglesXYZ", 31);
        fn(e, cf, "fromOrientation", 32);
        fn(e, cf, "fromEulerAnglesYXZ", 32);
        fn(e, cf, "fromAxisAngle", 33);
        fn(e, cf, "lookAt", 34);
        fn(e, cf, "fromMatrix", 35);
        fn(e, cf, "lookAlong", 36);
        cf.rawset("identity", CFrame.IDENTITY);
        LuaTable c3 = lib(g, "Color3");
        fn(e, c3, "new", 40);
        fn(e, c3, "fromRGB", 41);
        fn(e, c3, "fromHSV", 42);
        fn(e, c3, "fromHex", 43);
        fn(e, c3, "toHSV", 44);
        LuaTable ud = lib(g, "UDim");
        fn(e, ud, "new", 50);
        LuaTable ud2 = lib(g, "UDim2");
        fn(e, ud2, "new", 51);
        fn(e, ud2, "fromScale", 52);
        fn(e, ud2, "fromOffset", 53);
        LuaTable bc = lib(g, "BrickColor");
        fn(e, bc, "new", 60);
        fn(e, bc, "random", 61);
        fn(e, bc, "palette", 62);
        for (String n : new String[] {"White", "Gray", "DarkGray", "Black", "Red", "Yellow", "Green", "Blue"}) {
            fn(e, bc, n, 63);
        }
        LuaTable ti = lib(g, "TweenInfo");
        fn(e, ti, "new", 70);
        LuaTable ray = lib(g, "Ray");
        fn(e, ray, "new", 71);
        LuaTable nr = lib(g, "NumberRange");
        fn(e, nr, "new", 72);
        LuaTable ns = lib(g, "NumberSequence");
        fn(e, ns, "new", 73);
        LuaTable cs = lib(g, "ColorSequence");
        fn(e, cs, "new", 74);
        LuaTable nsk = lib(g, "NumberSequenceKeypoint");
        fn(e, nsk, "new", 75);
        LuaTable csk = lib(g, "ColorSequenceKeypoint");
        fn(e, csk, "new", 76);
        LuaTable rnd = lib(g, "Random");
        fn(e, rnd, "new", 77);
        LuaTable rp = lib(g, "RaycastParams");
        fn(e, rp, "new", 78);
        LuaTable op = lib(g, "OverlapParams");
        fn(e, op, "new", 79);
        LuaTable pp = lib(g, "PhysicalProperties");
        fn(e, pp, "new", 80);
        LuaTable rg = lib(g, "Region3");
        fn(e, rg, "new", 81);
        LuaTable dt = lib(g, "DateTime");
        fn(e, dt, "now", 82);
        fn(e, dt, "fromUnixTimestamp", 83);
        fn(e, dt, "fromUnixTimestampMillis", 84);
        LuaTable rect = lib(g, "Rect");
        fn(e, rect, "new", 85);
        LuaTable pw = lib(g, "PathWaypoint");
        fn(e, pw, "new", 86);
        LuaTable fnt = lib(g, "Font");
        fn(e, fnt, "new", 87);
        fn(e, fnt, "fromEnum", 88);
        fn(e, fnt, "fromName", 87);

        LuaTable task = lib(g, "task");
        fn(e, task, "wait", 100);
        fn(e, task, "spawn", 101);
        fn(e, task, "defer", 102);
        fn(e, task, "delay", 103);
        fn(e, task, "cancel", 104);
        fn(e, task, "synchronize", 105);
        fn(e, task, "desynchronize", 105);
        fn(e, g, "wait", 106);
        fn(e, g, "spawn", 107);
        fn(e, g, "delay", 108);
        fn(e, g, "Spawn", 107);
        fn(e, g, "Delay", 108);
        fn(e, g, "Wait", 106);
        fn(e, g, "tick", 110);
        fn(e, g, "time", 111);
        fn(e, g, "elapsedTime", 111);
        fn(e, g, "ElapsedTime", 111);
        fn(e, g, "warn", 112);
        fn(e, g, "print", 113);
        fn(e, g, "typeof", 114);
        fn(e, g, "settings", 115);
        fn(e, g, "UserSettings", 115);
        fn(e, g, "version", 116);
        fn(e, g, "__loadmodule", 117);
        fn(e, g, "loadstring", 118);
        fn(e, g, "printidentity", 119);

        LuaClosure pre = in.load(PRELUDE, "=prelude", g);
        e.invoker = (LuaFunction) in.call(pre)[0];
    }

    private static Color3 color(Object[] a, int i, String fn) {
        Object v = Args.at(a, i);
        if (v instanceof Color3) {
            return (Color3) v;
        }
        throw Args.bad(a, i, fn, "Color3");
    }

    private static String enumName(Object v, String def) {
        if (v instanceof Enums.EnumItem) {
            return ((Enums.EnumItem) v).name;
        }
        return def;
    }

    static Object[] call(Engine e, int id, String name, Interp in, Object[] a) {
        switch (id) {
            case 0: {
                Instance i = e.scriptCreate(Args.str(a, 0, "Instance.new"));
                Object p = Args.at(a, 1);
                if (p instanceof Instance) {
                    i.setParent((Instance) p);
                }
                return RbxType.ret(i);
            }
            case 1:
                return RbxType.ret(Args.inst(a, 0, "fromExisting").cloneTree());
            case 10:
                return RbxType.ret(new Vector3(Args.optNum(a, 0, 0), Args.optNum(a, 1, 0), Args.optNum(a, 2, 0)));
            case 11: {
                Enums.EnumItem n = Args.at(a, 0) instanceof Enums.EnumItem ? (Enums.EnumItem) a[0] : null;
                int v = n == null ? 5 : n.value;
                double[][] dirs = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {-1, 0, 0}, {0, -1, 0}, {0, 0, -1}};
                return RbxType.ret(new Vector3(dirs[v][0], dirs[v][1], dirs[v][2]));
            }
            case 12: {
                String ax = enumName(Args.at(a, 0), "X");
                return RbxType.ret("X".equals(ax) ? Vector3.X_AXIS : "Y".equals(ax) ? Vector3.Y_AXIS : Vector3.Z_AXIS);
            }
            case 20:
                return RbxType.ret(new Vector2(Args.optNum(a, 0, 0), Args.optNum(a, 1, 0)));
            case 30:
                return RbxType.ret(newCFrame(a));
            case 31:
                return RbxType.ret(CFrame.angles(Args.optNum(a, 0, 0), Args.optNum(a, 1, 0), Args.optNum(a, 2, 0)));
            case 32:
                return RbxType.ret(CFrame.orientation(Args.optNum(a, 0, 0), Args.optNum(a, 1, 0), Args.optNum(a, 2, 0)));
            case 33:
                return RbxType.ret(CFrame.fromAxisAngle(Args.vec3(a, 0, name), Args.num(a, 1, name)));
            case 34: {
                Vector3 up = Args.at(a, 2) instanceof Vector3 ? (Vector3) a[2] : Vector3.Y_AXIS;
                return RbxType.ret(CFrame.lookAt(Args.vec3(a, 0, name), Args.vec3(a, 1, name), up));
            }
            case 35: {
                Vector3 vx = Args.vec3(a, 1, name);
                Vector3 vy = Args.vec3(a, 2, name);
                Vector3 vz = Args.at(a, 3) instanceof Vector3 ? (Vector3) a[3] : vx.cross(vy).unit();
                return RbxType.ret(CFrame.fromMatrix(Args.vec3(a, 0, name), vx, vy, vz));
            }
            case 36: {
                Vector3 at = Args.vec3(a, 0, name);
                Vector3 up = Args.at(a, 2) instanceof Vector3 ? (Vector3) a[2] : Vector3.Y_AXIS;
                return RbxType.ret(CFrame.lookAt(at, at.add(Args.vec3(a, 1, name)), up));
            }
            case 40:
                return RbxType.ret(new Color3(Args.optNum(a, 0, 0), Args.optNum(a, 1, 0), Args.optNum(a, 2, 0)));
            case 41:
                return RbxType.ret(new Color3(Args.optNum(a, 0, 0) / 255, Args.optNum(a, 1, 0) / 255,
                        Args.optNum(a, 2, 0) / 255));
            case 42:
                return RbxType.ret(Color3.hsv(Args.num(a, 0, name), Args.num(a, 1, name), Args.num(a, 2, name)));
            case 43:
                try {
                    return RbxType.ret(Color3.hex(Args.str(a, 0, name)));
                } catch (NumberFormatException ex) {
                    throw LuaError.rt("Unable to convert characters to hex value"); // NOPMD PreserveStackTrace - a script sees the message only; LuaError carries no cause
                }
            case 44: {
                double[] h = color(a, 0, name).toHsv();
                return new Object[] {Args.n(h[0]), Args.n(h[1]), Args.n(h[2])};
            }
            case 50:
                return RbxType.ret(new UDim(Args.optNum(a, 0, 0), Args.optNum(a, 1, 0)));
            case 51:
                if (Args.at(a, 0) instanceof UDim) {
                    return RbxType.ret(new UDim2((UDim) a[0], Args.at(a, 1) instanceof UDim ? (UDim) a[1] : new UDim(0, 0)));
                }
                return RbxType.ret(new UDim2(Args.optNum(a, 0, 0), Args.optNum(a, 1, 0), Args.optNum(a, 2, 0),
                        Args.optNum(a, 3, 0)));
            case 52:
                return RbxType.ret(new UDim2(Args.optNum(a, 0, 0), 0, Args.optNum(a, 1, 0), 0));
            case 53:
                return RbxType.ret(new UDim2(0, Args.optNum(a, 0, 0), 0, Args.optNum(a, 1, 0)));
            case 60: {
                Object v = Args.at(a, 0);
                if (v instanceof String) {
                    return RbxType.ret(BrickColor.byName((String) v));
                }
                if (v instanceof Color3) {
                    return RbxType.ret(BrickColor.nearest((Color3) v));
                }
                if (a.length >= 3) {
                    return RbxType.ret(BrickColor.nearest(new Color3(Args.num(a, 0, name), Args.num(a, 1, name),
                            Args.num(a, 2, name))));
                }
                return RbxType.ret(BrickColor.byNumber(Args.integer(a, 0, name)));
            }
            case 61:
                return RbxType.ret(BrickColor.at((int) (e.sched.now() * 1000) + (int) e.getFrames()));
            case 62:
                return RbxType.ret(BrickColor.at(Args.integer(a, 0, name)));
            case 63: {
                String n = name;
                String mapped = "White".equals(n) ? "White" : "Gray".equals(n) ? "Medium stone grey"
                        : "DarkGray".equals(n) ? "Dark stone grey" : "Black".equals(n) ? "Black"
                        : "Red".equals(n) ? "Bright red" : "Yellow".equals(n) ? "Bright yellow"
                        : "Green".equals(n) ? "Dark green" : "Bright blue";
                return RbxType.ret(BrickColor.byName(mapped));
            }
            case 70:
                return RbxType.ret(new TweenInfo(Args.optNum(a, 0, 1), enumName(Args.at(a, 1), "Quad"),
                        enumName(Args.at(a, 2), "Out"), (int) Args.optNum(a, 3, 0), Args.bool(a, 4), Args.optNum(a, 5, 0)));
            case 71:
                return RbxType.ret(new Ray(Args.vec3(a, 0, name), Args.vec3(a, 1, name)));
            case 72: {
                double lo = Args.num(a, 0, name);
                return RbxType.ret(new NumberRange(lo, Args.optNum(a, 1, lo)));
            }
            case 73:
                return RbxType.ret(newSequence(a, false, name));
            case 74:
                return RbxType.ret(newSequence(a, true, name));
            case 75:
                return RbxType.ret(new Sequence.Keypoint(false, Args.num(a, 0, name), Args.n(Args.num(a, 1, name)),
                        Args.optNum(a, 2, 0)));
            case 76:
                return RbxType.ret(new Sequence.Keypoint(true, Args.num(a, 0, name), color(a, 1, name), 0));
            case 77:
                return RbxType.ret(new RbxRandom(Args.at(a, 0) == null ? (long) (e.sched.now() * 1e6) + e.getFrames()
                        : (long) Args.num(a, 0, name)));
            case 78:
                return RbxType.ret(new QueryParams(false));
            case 79:
                return RbxType.ret(new QueryParams(true));
            case 80:
                return RbxType.ret(new QueryParams.PhysicalProperties(Args.optNum(a, 0, 0.7), Args.optNum(a, 1, 0.3),
                        Args.optNum(a, 2, 0.5), Args.optNum(a, 3, 1), Args.optNum(a, 4, 1)));
            case 81:
                return RbxType.ret(new QueryParams.Region3(Args.vec3(a, 0, name), Args.vec3(a, 1, name)));
            case 82:
                return RbxType.ret(new QueryParams.DateTime(System.currentTimeMillis()));
            case 83:
                return RbxType.ret(new QueryParams.DateTime((long) (Args.num(a, 0, name) * 1000)));
            case 84:
                return RbxType.ret(new QueryParams.DateTime((long) Args.num(a, 0, name)));
            case 85:
                return RbxType.ret(new QueryParams.Rect(Args.optNum(a, 0, 0), Args.optNum(a, 1, 0),
                        Args.optNum(a, 2, 0), Args.optNum(a, 3, 0)));
            case 86:
                return RbxType.ret(new QueryParams.Waypoint(Args.vec3(a, 0, name),
                        Args.at(a, 1) instanceof Enums.EnumItem ? ((Enums.EnumItem) a[1]).name : "Walk"));
            case 87:
                return RbxType.ret(new QueryParams.FontFace(Args.str(a, 0, name)));
            case 88:
                return RbxType.ret(new QueryParams.FontFace(enumName(Args.at(a, 0), "SourceSans")));
            case 100:
            case 106:
                return e.sched.sleep(Args.optNum(a, 0, 0));
            case 101:
            case 107: {
                Object f = Args.at(a, 0);
                Object[] rest = rest(a, 1);
                if (f instanceof Coroutine) {
                    e.sched.resume((Coroutine) f, rest);
                    return RbxType.ret(f);
                }
                LuaFunction lf = Args.fn(a, 0, name);
                if (id == 107) {
                    return RbxType.ret(e.sched.deferFn(lf, new Object[] {Args.n(0), Args.n(e.sched.now())}));
                }
                return RbxType.ret(e.sched.spawn(lf, rest));
            }
            case 102: {
                Object f = Args.at(a, 0);
                if (f instanceof Coroutine) {
                    e.sched.defer((Coroutine) f, rest(a, 1));
                    return RbxType.ret(f);
                }
                return RbxType.ret(e.sched.deferFn(Args.fn(a, 0, name), rest(a, 1)));
            }
            case 103:
            case 108: {
                double t = Args.optNum(a, 0, 0);
                return RbxType.ret(e.sched.delay(t, Args.fn(a, 1, name), rest(a, 2)));
            }
            case 104: {
                Object c = Args.at(a, 0);
                if (!(c instanceof Coroutine)) {
                    throw Args.bad(a, 0, name, "thread");
                }
                e.sched.cancel((Coroutine) c);
                return null;
            }
            case 105:
                return null;
            case 110:
                return RbxType.ret(Args.n(System.currentTimeMillis() / 1000.0));
            case 111:
                return RbxType.ret(Args.n(e.sched.now()));
            case 112:
            case 113: {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < a.length; i++) {
                    if (i > 0) {
                        b.append(' ');
                    }
                    b.append(com.codename1.luau.Utf8.toJava(in.tostring(a[i])));
                }
                e.sched.print(id == 112 ? "warn" : "output", b.toString());
                return null;
            }
            case 114: {
                Object v = Args.at(a, 0);
                if (v instanceof RbxType) {
                    return RbxType.ret(((RbxType) v).typeName());
                }
                if (v instanceof Coroutine) {
                    return RbxType.ret("thread");
                }
                return RbxType.ret(Interp.typeName(v));
            }
            case 115:
                return RbxType.ret(new LuaTable());
            case 116:
                return RbxType.ret("0.1.0");
            case 117:
                return RbxType.ret(e.loadModule(Args.inst(a, 0, "require")));
            case 118: {
                if (!e.service("ServerScriptService").bool("LoadStringEnabled")) {
                    return new Object[] {null, "loadstring() is not available"};
                }
                try {
                    return RbxType.ret(in.load(Args.str(a, 0, name), Args.optStr(a, 1, "loadstring"),
                            in.getGlobals()));
                } catch (LuaError ex) {
                    return new Object[] {null, ex.getMessage()};
                }
            }
            case 119:
                e.sched.print("output", "Current identity is 2");
                return null;
            default:
                throw LuaError.rt(name + " is not implemented");
        }
    }

    private static Object[] rest(Object[] a, int from) {
        if (a.length <= from) {
            return new Object[0];
        }
        Object[] r = new Object[a.length - from];
        System.arraycopy(a, from, r, 0, r.length);
        return r;
    }

    private static CFrame newCFrame(Object[] a) {
        int n = a.length;
        if (n == 0) {
            return CFrame.IDENTITY;
        }
        if (a[0] instanceof Vector3) {
            Vector3 p = (Vector3) a[0];
            if (n >= 2 && a[1] instanceof Vector3) {
                return CFrame.lookAt(p, (Vector3) a[1], Vector3.Y_AXIS);
            }
            return CFrame.at(p.x, p.y, p.z);
        }
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = Args.num(a, i, "CFrame.new");
        }
        if (n == 3) {
            return CFrame.at(v[0], v[1], v[2]);
        }
        if (n == 7) {
            return CFrame.fromQuat(v[0], v[1], v[2], v[3], v[4], v[5], v[6]);
        }
        if (n == 12) {
            return new CFrame(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11]);
        }
        throw LuaError.rt("Invalid number of arguments: " + n);
    }

    private static Sequence newSequence(Object[] a, boolean color, String name) {
        Object first = Args.at(a, 0);
        if (first instanceof LuaTable) {
            LuaTable t = (LuaTable) first;
            int len = t.length();
            if (len < 2) {
                throw LuaError.rt(name + ": requires at least 2 keypoints");
            }
            double[] times = new double[len];
            Object[] vals = new Object[len];
            double[] env = new double[len];
            for (int i = 0; i < len; i++) {
                Object k = t.rawgeti(i + 1);
                if (!(k instanceof Sequence.Keypoint)) {
                    throw LuaError.rt(name + ": expected keypoints");
                }
                Sequence.Keypoint kp = (Sequence.Keypoint) k;
                times[i] = kp.time;
                vals[i] = kp.value;
                env[i] = kp.envelope;
            }
            return new Sequence(color, times, vals, env);
        }
        if (color) {
            Color3 c0 = color(a, 0, name);
            Color3 c1 = Args.at(a, 1) instanceof Color3 ? (Color3) a[1] : c0;
            return Sequence.colors(c0, c1);
        }
        double n0 = Args.num(a, 0, name);
        return Sequence.numbers(n0, Args.optNum(a, 1, n0));
    }
}
