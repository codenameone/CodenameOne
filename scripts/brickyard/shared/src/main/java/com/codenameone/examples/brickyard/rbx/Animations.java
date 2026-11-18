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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Keyframe animation. An `Animation.AnimationId` names a KeyframeSequence
/// that ships inside the place (by the sequence's `AssetId` attribute, its
/// name, or the numeric tail of the id): nothing is downloaded. Tracks write
/// `Motor6D.Transform` from the poses whose names match the motor's Part1.
/// Characters with no playing track get a procedural walk.
final class Animations {
    private Animations() {
    }

    static Instance resolve(Engine e, String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        String tail = id;
        int cut = Math.max(id.lastIndexOf('/'), id.lastIndexOf('='));
        if (cut >= 0) {
            tail = id.substring(cut + 1);
        }
        List<Instance> all = e.getGame().descendants();
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance s = all.get(i);
            if (!s.isA("KeyframeSequence")) {
                continue;
            }
            Object aid = s.getAttribute("AssetId");
            if (id.equals(aid) || tail.equals(aid) || tail.equals(s.getName()) || id.equals(s.getName())) {
                return s;
            }
        }
        return null;
    }

    static double length(Instance seq) {
        double t = 0;
        List<Instance> kf = seq.getChildren();
        for (int i = 0; i < kf.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (kf.get(i).isA("Keyframe")) {
                t = Math.max(t, kf.get(i).num("Time"));
            }
        }
        return t;
    }

    /// Pose CFrames by part name at time `t`, interpolated between keyframes.
    static Map<String, CFrame> sample(Instance seq, double t) {
        List<Instance> kfs = new ArrayList<Instance>();
        List<Instance> kids = seq.getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (kids.get(i).isA("Keyframe")) {
                kfs.add(kids.get(i));
            }
        }
        java.util.Collections.sort(kfs, new java.util.Comparator<Instance>() {
            @Override
            public int compare(Instance a, Instance b) {
                return Double.compare(a.num("Time"), b.num("Time"));
            }
        });
        Map<String, CFrame> out = new HashMap<String, CFrame>();
        if (kfs.isEmpty()) {
            return out;
        }
        Instance a = kfs.get(0);
        Instance b = a;
        for (int i = 0; i < kfs.size(); i++) {
            if (kfs.get(i).num("Time") <= t) {
                a = kfs.get(i);
                b = i + 1 < kfs.size() ? kfs.get(i + 1) : a;
            }
        }
        double span = b.num("Time") - a.num("Time");
        double f = span <= 0 ? 0 : (t - a.num("Time")) / span;
        Map<String, CFrame> pa = poses(a);
        Map<String, CFrame> pb = poses(b);
        for (Map.Entry<String, CFrame> en : pa.entrySet()) {
            CFrame to = pb.get(en.getKey());
            out.put(en.getKey(), to == null ? en.getValue() : en.getValue().lerp(to, f));
        }
        return out;
    }

    static Map<String, CFrame> poses(Instance kf) {
        Map<String, CFrame> out = new HashMap<String, CFrame>();
        List<Instance> d = kf.descendants();
        for (int i = 0; i < d.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (d.get(i).isA("Pose")) {
                out.put(d.get(i).getName(), (CFrame) d.get(i).prop("CFrame"));
            }
        }
        return out;
    }

    static List<Instance> motors(Instance model) {
        List<Instance> out = new ArrayList<Instance>();
        List<Instance> d = model.descendants();
        for (int i = 0; i < d.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (d.get(i).isA("Motor6D")) {
                out.add(d.get(i));
            }
        }
        return out;
    }

    static void reset(Instance model) {
        List<Instance> ms = motors(model);
        for (int i = 0; i < ms.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            ms.get(i).putQuiet("Transform", CFrame.IDENTITY);
        }
    }

    /// Advances the tracks and writes the motor transforms.
    static void step(Instance model, List<Instance> tracks, double dt) {
        Engine e = model.getEngine();
        boolean any = false;
        Map<String, CFrame> pose = new HashMap<String, CFrame>();
        for (int i = 0; i < tracks.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance t = tracks.get(i);
            if (!t.bool("IsPlaying")) {
                continue;
            }
            Instance seq = resolve(e, ((Instance) t.prop("Animation")) == null ? null
                    : ((Instance) t.prop("Animation")).str("AnimationId"));
            double len = t.num("Length");
            double tp = t.num("TimePosition") + dt * t.num("Speed");
            if (tp >= len) {
                if (t.bool("Looped")) {
                    tp = len <= 0 ? 0 : tp % len;
                    t.fireEvent("DidLoop");
                } else {
                    tp = len;
                    t.putQuiet("TimePosition", Args.n(tp));
                    Characters.stopTrack(t);
                    continue;
                }
            }
            t.putQuiet("TimePosition", Args.n(tp));
            if (seq != null) {
                pose.putAll(sample(seq, tp));
                any = true;
            }
        }
        List<Instance> ms = motors(model);
        if (any) {
            for (int i = 0; i < ms.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                Instance m = ms.get(i);
                Instance p1 = m.ref("Part1");
                CFrame c = p1 == null ? null : pose.get(p1.getName());
                m.putQuiet("Transform", c == null ? CFrame.IDENTITY : c);
            }
            return;
        }
        procedural(model, ms);
    }

    /// The default walk: arms and legs swing with speed.
    static void procedural(Instance model, List<Instance> ms) {
        Instance hum = model.findFirstChildOfClass("Humanoid", false);
        if (hum == null) {
            return;
        }
        Vector3 dir = (Vector3) hum.prop("MoveDirection");
        double moving = Math.min(1, dir.magnitude());
        Engine e = model.getEngine();
        double phase = e.getScheduler().now() * hum.num("WalkSpeed") / 2.5;
        double swing = Math.sin(phase) * 0.9 * moving;
        for (int i = 0; i < ms.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance m = ms.get(i);
            String n = m.getName();
            double a;
            if ("Left Shoulder".equals(n) || "Right Hip".equals(n)) {
                a = swing;
            } else if ("Right Shoulder".equals(n) || "Left Hip".equals(n)) {
                a = -swing;
            } else {
                continue;
            }
            m.putQuiet("Transform", CFrame.rotX(a));
        }
    }
}
