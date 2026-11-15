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

import com.codename1.luau.LuaError;
import com.codename1.luau.LuaFunction;
import com.codename1.luau.LuaTable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// TweenService tweens: interpolation of numbers, booleans, vectors, colors,
/// UDims and CFrames, with the easing, repeat, reverse and delay of a
/// [TweenInfo].
final class Tweens {
    private Tweens() {
    }

    /// The running state of one tween, kept in `Instance.host`.
    static final class State {
        final TweenInfo info;
        final Instance target;
        final Map<String, Object> goals = new LinkedHashMap<String, Object>();
        final Map<String, Object> starts = new LinkedHashMap<String, Object>();
        double elapsed;
        double delayLeft;
        int repeats;
        boolean reverse;
        boolean playing;
        LuaFunction callback;

        State(TweenInfo info, Instance target) {
            this.info = info;
            this.target = target;
        }
    }

    @SuppressWarnings("unchecked")
    static List<Instance> active(Engine e) {
        Object o = e.extra("tweens");
        if (o == null) {
            o = new ArrayList<Instance>();
            e.extra("tweens", o);
        }
        return (List<Instance>) o;
    }

    /// `TweenService:Create(instance, info, goals)`.
    static Instance create(Engine e, Instance target, TweenInfo info, LuaTable goals) {
        Instance t = e.create("Tween");
        t.setName("Tween");
        t.putQuiet("Instance", target);
        t.putQuiet("TweenInfo", info);
        State s = new State(info, target);
        Object[] kv = new Object[2];
        Object k = null;
        while (goals.next(k, kv)) {
            k = kv[0];
            Object v = kv[1];
            if ("Position".equals(k) && v instanceof Vector3 && target.isA("BasePart")) {
                // A part's Position is its CFrame's translation, not a
                // property of its own: tween the CFrame to the goal, keeping
                // the rotation, which is what moving a part's Position does.
                Vector3 p = (Vector3) v;
                CFrame f = Behaviors.cf(target);
                s.goals.put("CFrame", new CFrame(p.x, p.y, p.z, f.r00, f.r01, f.r02, f.r10, f.r11, f.r12,
                        f.r20, f.r21, f.r22));
                continue;
            }
            if (!(k instanceof String) || !target.hasProp((String) k)) {
                throw LuaError.rt("TweenService:Create property named '" + k + "' cannot be tweened due to type "
                        + "mismatch (property is a 'nil', but given type is '" + com.codename1.luau.Interp.typeName(v) + "')");
            }
            s.goals.put((String) k, target.coerce((String) k, v));
        }
        t.host = s;
        return t;
    }

    static void play(Engine e, Instance t, State s) {
        if (s.playing) {
            return;
        }
        Object st = t.prop("PlaybackState");
        boolean resume = st instanceof Enums.EnumItem && "Paused".equals(((Enums.EnumItem) st).name);
        if (!resume) {
            s.elapsed = 0;
            s.repeats = 0;
            s.reverse = false;
            s.delayLeft = s.info.delay;
            captureStarts(s);
        }
        // A newer tween on the same property wins, as in the engine this
        // mirrors: the older one is cancelled.
        List<Instance> act = active(e);
        for (int i = act.size() - 1; i >= 0; i--) {
            Instance o = act.get(i);
            State os = (State) o.host;
            if (os.target == s.target) { // NOPMD CompareObjectsWithEquals - reference identity
                for (String k : s.goals.keySet()) {
                    if (os.goals.containsKey(k)) {
                        cancel(e, o, os);
                        break;
                    }
                }
            }
        }
        s.playing = true;
        t.put("PlaybackState", Enums.item("PlaybackState", s.delayLeft > 0 ? "Delayed" : "Playing"));
        act.add(t);
    }

    static void captureStarts(State s) {
        s.starts.clear();
        for (String k : s.goals.keySet()) {
            s.starts.put(k, s.target.prop(k));
        }
    }

    static void pause(Instance t, State s) {
        if (!s.playing) {
            return;
        }
        s.playing = false;
        active(t.getEngine()).remove(t);
        t.put("PlaybackState", Enums.item("PlaybackState", "Paused"));
    }

    static void cancel(Engine e, Instance t, State s) {
        s.playing = false;
        active(e).remove(t);
        s.elapsed = 0;
        t.put("PlaybackState", Enums.item("PlaybackState", "Cancelled"));
        t.fireEvent("Completed", Enums.item("PlaybackState", "Cancelled"));
        if (s.callback != null) {
            e.getScheduler().spawn(s.callback, new Object[] {Enums.item("TweenStatus", "Canceled")});
        }
    }

    static void step(Engine e, double dt) {
        List<Instance> act = active(e);
        if (act.isEmpty()) {
            return;
        }
        Instance[] snapshot = act.toArray(new Instance[0]);
        for (Instance t : snapshot) {
            State s = (State) t.host;
            if (!s.playing || s.target.isDestroyed()) {
                act.remove(t);
                continue;
            }
            if (s.delayLeft > 0) {
                s.delayLeft -= dt;
                if (s.delayLeft > 0) {
                    continue;
                }
                t.put("PlaybackState", Enums.item("PlaybackState", "Playing"));
            }
            s.elapsed += dt;
            double dur = Math.max(s.info.time, 1e-9);
            double f = Math.min(1, s.elapsed / dur);
            apply(s, s.reverse ? 1 - f : f);
            if (f < 1) {
                continue;
            }
            if (s.info.reverses && !s.reverse) {
                s.reverse = true;
                s.elapsed = 0;
                continue;
            }
            if (s.info.repeatCount < 0 || s.repeats < s.info.repeatCount) {
                s.repeats++;
                s.reverse = false;
                s.elapsed = 0;
                s.delayLeft = s.info.delay;
                continue;
            }
            s.playing = false;
            act.remove(t);
            t.put("PlaybackState", Enums.item("PlaybackState", "Completed"));
            t.fireEvent("Completed", Enums.item("PlaybackState", "Completed"));
            if (s.callback != null) {
                e.getScheduler().spawn(s.callback, new Object[] {Enums.item("TweenStatus", "Completed")});
            }
        }
    }

    static void apply(State s, double f) {
        double a = s.info.ease(f);
        for (Map.Entry<String, Object> g : s.goals.entrySet()) {
            Object from = s.starts.get(g.getKey());
            s.target.put(g.getKey(), lerp(from, g.getValue(), a));
        }
    }

    /// Interpolates two values of one engine type; a type that cannot be
    /// interpolated snaps at the end.
    static Object lerp(Object a, Object b, double t) {
        if (a instanceof Double && b instanceof Double) {
            double x = ((Double) a).doubleValue();
            return Args.n(x + (((Double) b).doubleValue() - x) * t);
        }
        if (a instanceof Vector3 && b instanceof Vector3) {
            return ((Vector3) a).lerp((Vector3) b, t);
        }
        if (a instanceof CFrame && b instanceof CFrame) {
            return ((CFrame) a).lerp((CFrame) b, t);
        }
        if (a instanceof Color3 && b instanceof Color3) {
            Color3 x = (Color3) a;
            Color3 y = (Color3) b;
            return new Color3(x.r + (y.r - x.r) * t, x.g + (y.g - x.g) * t, x.b + (y.b - x.b) * t);
        }
        if (a instanceof Vector2 && b instanceof Vector2) {
            Vector2 x = (Vector2) a;
            Vector2 y = (Vector2) b;
            return new Vector2(x.x + (y.x - x.x) * t, x.y + (y.y - x.y) * t);
        }
        if (a instanceof UDim && b instanceof UDim) {
            return udim((UDim) a, (UDim) b, t);
        }
        if (a instanceof UDim2 && b instanceof UDim2) {
            UDim2 x = (UDim2) a;
            UDim2 y = (UDim2) b;
            return new UDim2(udim(x.x, y.x, t), udim(x.y, y.y, t));
        }
        return t >= 1 ? b : a;
    }

    static UDim udim(UDim a, UDim b, double t) {
        return new UDim(a.scale + (b.scale - a.scale) * t, a.offset + (b.offset - a.offset) * t);
    }

    /// `GuiObject:TweenPosition/TweenSize/TweenSizeAndPosition`; the
    /// arguments from `at` are easing direction, style, time, override and a
    /// callback.
    static boolean guiTween(Instance gui, Object pos, Object size, Object[] a, int at) {
        String dir = enumName(Args.at(a, at), "Out");
        String style = enumName(Args.at(a, at + 1), "Quad");
        double time = Args.optNum(a, at + 2, 1);
        boolean override = Args.bool(a, at + 3);
        Object cb = Args.at(a, at + 4);
        Engine e = gui.getEngine();
        List<Instance> act = active(e);
        for (int i = act.size() - 1; i >= 0; i--) {
            State os = (State) act.get(i).host;
            if (os.target == gui) { // NOPMD CompareObjectsWithEquals - reference identity
                if (!override) {
                    return false;
                }
                cancel(e, act.get(i), os);
            }
        }
        LuaTable goals = new LuaTable();
        if (pos instanceof UDim2) {
            goals.rawset("Position", pos);
        }
        if (size instanceof UDim2) {
            goals.rawset("Size", size);
        }
        Instance t = create(e, gui, new TweenInfo(time, style, dir, 0, false, 0), goals);
        State s = (State) t.host;
        if (cb instanceof LuaFunction) {
            s.callback = (LuaFunction) cb;
        }
        play(e, t, s);
        return true;
    }

    static String enumName(Object v, String def) {
        if (v instanceof Enums.EnumItem) {
            return ((Enums.EnumItem) v).name;
        }
        if (v instanceof String) {
            return (String) v;
        }
        return def;
    }
}
