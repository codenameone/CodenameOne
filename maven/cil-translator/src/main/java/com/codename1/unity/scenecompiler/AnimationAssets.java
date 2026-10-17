/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.unity.scenecompiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Turns the two animation assets of a Unity project into Java source: an
/// animation clip (`.anim`) and an animator controller (`.controller`).
///
/// What comes out is a block of statements that builds the runtime's
/// object with plain calls. Nothing of the asset is left to be read on a
/// device, and nothing is found by name there: a state's clip, a
/// transition's target and the parameter a condition tests are all
/// numbers by the time the code is written.
final class AnimationAssets {
    private static final String RUNTIME = "com.codename1.unitycompat.unityengine.";

    /// Where a clip's sprites come from: the expression for the sprite a
    /// reference names, or null.
    interface Sprites {
        String sprite(String user, Object reference);
    }

    /// Where a state's clip comes from: the expression for the clip a
    /// reference names, or null.
    interface Clips {
        String clip(String user, Object reference);
    }

    private AnimationAssets() {
    }

    /// A number as Java source, an infinite slope included: that is how a
    /// curve says a key is stepped.
    static String real(Object o, String fallback) {
        String s = SceneCompiler.text(o);
        if (s.length() == 0) {
            return fallback + "f";
        }
        if ("Infinity".equalsIgnoreCase(s) || "inf".equalsIgnoreCase(s) || "+Infinity".equalsIgnoreCase(s)) {
            return "Float.POSITIVE_INFINITY";
        }
        if ("-Infinity".equalsIgnoreCase(s) || "-inf".equalsIgnoreCase(s)) {
            return "Float.NEGATIVE_INFINITY";
        }
        if ("NaN".equalsIgnoreCase(s)) {
            return "Float.NaN";
        }
        return SceneCompiler.f(o, fallback);
    }

    /// The keys of one curve as the four floats a key the runtime reads.
    static String keys(Object curve, String member) {
        StringBuilder sb = new StringBuilder();
        for (Object k : SceneCompiler.list(SceneCompiler.map(curve).get("m_Curve"))) {
            Map<String, Object> key = SceneCompiler.map(k);
            Object value = key.get("value");
            Object in = key.get("inSlope");
            Object out = key.get("outSlope");
            if (member != null) {
                value = SceneCompiler.map(value).get(member);
                in = SceneCompiler.map(in).get(member);
                out = SceneCompiler.map(out).get(member);
            }
            sb.append(sb.length() == 0 ? "" : ", ").append(real(key.get("time"), "0")).append(", ")
                    .append(real(value, "0")).append(", ").append(real(in, "0")).append(", ")
                    .append(real(out, "0"));
        }
        return sb.toString();
    }

    /// The kind of a curve on a float property, or -1.
    static int kind(int classId, String attribute) {
        if (classId == 212) {
            if ("m_Color.r".equals(attribute)) {
                return 7;
            }
            if ("m_Color.g".equals(attribute)) {
                return 8;
            }
            if ("m_Color.b".equals(attribute)) {
                return 9;
            }
            if ("m_Color.a".equals(attribute)) {
                return 10;
            }
            if ("m_Enabled".equals(attribute)) {
                return 11;
            }
            if ("m_FlipX".equals(attribute)) {
                return 13;
            }
            if ("m_FlipY".equals(attribute)) {
                return 14;
            }
        }
        if (classId == 1 && "m_IsActive".equals(attribute)) {
            return 12;
        }
        if (classId == 4 || classId == 224) {
            final String[] names = {"m_LocalPosition.x", "m_LocalPosition.y", "m_LocalPosition.z",
                "localEulerAnglesRaw.z", "m_LocalScale.x", "m_LocalScale.y", "m_LocalScale.z"};
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(attribute)) {
                    return i;
                }
            }
            if ("localEulerAngles.z".equals(attribute) || "m_LocalEulerAngles.z".equals(attribute)) {
                return 3;
            }
        }
        return -1;
    }

    private static void vector(StringBuilder sb, String v, Object curves, int firstKind, boolean onlyZ,
            String label, List<String> notes) {
        for (Object c : SceneCompiler.list(curves)) {
            Map<String, Object> curve = SceneCompiler.map(c);
            String path = SceneCompiler.string(SceneCompiler.text(curve.get("path")));
            final String[] members = {"x", "y", "z"};
            for (int i = 0; i < 3; i++) {
                String keys = keys(curve.get("curve"), members[i]);
                if (keys.length() == 0) {
                    continue;
                }
                if (onlyZ) {
                    if (i == 2) {
                        sb.append("            ").append(v).append(".$curve(").append(path).append(", 3, new float[] {")
                                .append(keys).append("});\n");
                    } else if (moves(curve.get("curve"), members[i])) {
                        notes.add(label + ": the clip turns '" + SceneCompiler.text(curve.get("path")) + "' about "
                                + members[i] + ", out of the plane a 2D scene is drawn in; only a turn about z"
                                + " is applied");
                    }
                    continue;
                }
                sb.append("            ").append(v).append(".$curve(").append(path).append(", ").append(firstKind + i)
                        .append(", new float[] {").append(keys).append("});\n");
            }
        }
    }

    /// Whether a member of a vector curve is ever anything but zero.
    private static boolean moves(Object curve, String member) {
        for (Object k : SceneCompiler.list(SceneCompiler.map(curve).get("m_Curve"))) {
            String value = SceneCompiler.number(SceneCompiler.map(SceneCompiler.map(k).get("value")).get(member), "0");
            if (Double.parseDouble(value) != 0d) {
                return true;
            }
        }
        return false;
    }

    /// The statements that build a clip into the variable `v`, each line
    /// indented to sit inside a method's `if`. `label` is the asset's
    /// path, for what is said about it.
    static String clip(String label, List<UnityYaml.Document> docs, String v, Sprites sprites, List<String> warnings,
            List<String> notes) {
        Map<String, Object> p = null;
        for (UnityYaml.Document d : docs) {
            if (d.properties != null && d.classId == 74) {
                p = d.properties;
            }
        }
        if (p == null) {
            warnings.add(label + ": the file holds no animation clip; a state that plays it shows nothing");
            return null;
        }
        Map<String, Object> settings = SceneCompiler.map(p.get("m_AnimationClipSettings"));
        double start = Double.parseDouble(SceneCompiler.number(settings.get("m_StartTime"), "0"));
        double stop = Double.parseDouble(SceneCompiler.number(settings.get("m_StopTime"), "0"));
        if (start != 0d) {
            notes.add(label + ": the clip starts at " + start + " seconds and not at zero; it is played from zero");
        }
        String rate = SceneCompiler.f(p.get("m_SampleRate"), "60");
        StringBuilder sb = new StringBuilder();
        sb.append("            ").append(RUNTIME).append("AnimationClip ").append(v).append(" = new ").append(RUNTIME)
                .append("AnimationClip(").append(SceneCompiler.string(SceneCompiler.text(p.get("m_Name"))))
                .append(", ").append((float) stop).append("f, ").append(rate).append(", ")
                .append("1".equals(SceneCompiler.text(settings.get("m_LoopTime")))).append(");\n");
        for (Object c : SceneCompiler.list(p.get("m_PPtrCurves"))) {
            Map<String, Object> curve = SceneCompiler.map(c);
            String attribute = SceneCompiler.text(curve.get("attribute"));
            if (!"m_Sprite".equals(attribute)) {
                warnings.add(label + ": the clip animates " + attribute + " of '" + SceneCompiler.text(curve.get("path"))
                        + "', a reference to an asset; only a sprite renderer's sprite is animated, and the curve"
                        + " was left out");
                continue;
            }
            StringBuilder times = new StringBuilder();
            StringBuilder shown = new StringBuilder();
            for (Object k : SceneCompiler.list(curve.get("curve"))) {
                Map<String, Object> key = SceneCompiler.map(k);
                String s = sprites.sprite("the clip " + label, key.get("value"));
                times.append(times.length() == 0 ? "" : ", ").append(SceneCompiler.f(key.get("time"), "0"));
                shown.append(shown.length() == 0 ? "" : ", ").append(s == null ? "null" : s);
            }
            sb.append("            ").append(v).append(".$sprites(")
                    .append(SceneCompiler.string(SceneCompiler.text(curve.get("path")))).append(", new float[] {")
                    .append(times).append("}, new ").append(RUNTIME).append("Sprite[] {").append(shown)
                    .append("});\n");
        }
        for (Object c : SceneCompiler.list(p.get("m_FloatCurves"))) {
            Map<String, Object> curve = SceneCompiler.map(c);
            String attribute = SceneCompiler.text(curve.get("attribute"));
            int classId = (int) Long.parseLong(SceneCompiler.integer(curve.get("classID"), "0"));
            int kind = kind(classId, attribute);
            if (kind < 0) {
                warnings.add(label + ": the clip animates " + attribute + " of '" + SceneCompiler.text(curve.get("path"))
                        + "' (a component of class " + classId + "), which is not a property the runtime animates;"
                        + " the curve was left out");
                continue;
            }
            sb.append("            ").append(v).append(".$curve(")
                    .append(SceneCompiler.string(SceneCompiler.text(curve.get("path")))).append(", ").append(kind)
                    .append(", new float[] {").append(keys(curve.get("curve"), null)).append("});\n");
        }
        vector(sb, v, p.get("m_PositionCurves"), 0, false, label, notes);
        vector(sb, v, p.get("m_ScaleCurves"), 4, false, label, notes);
        vector(sb, v, p.get("m_EulerCurves"), 3, true, label, notes);
        if (!SceneCompiler.list(p.get("m_RotationCurves")).isEmpty()) {
            warnings.add(label + ": the clip turns an object by quaternion keys, which are not read; record the"
                    + " rotation as Euler angles (the default for a clip made in the Animation window) and it is");
        }
        for (Object e : SceneCompiler.list(p.get("m_Events"))) {
            Map<String, Object> event = SceneCompiler.map(e);
            String function = SceneCompiler.text(event.get("functionName"));
            if (function.length() == 0) {
                continue;
            }
            sb.append("            ").append(v).append(".$event(").append(SceneCompiler.f(event.get("time"), "0"))
                    .append(", ").append(SceneCompiler.string(function)).append(");\n");
            if (SceneCompiler.text(event.get("data")).length() > 0
                    || Double.parseDouble(SceneCompiler.number(event.get("floatParameter"), "0")) != 0d
                    || Long.parseLong(SceneCompiler.integer(event.get("intParameter"), "0")) != 0
                    || SceneCompiler.fileId(event.get("objectReferenceParameter")).longValue() != 0) {
                notes.add(label + ": the animation event " + function + " carries an argument; the method is"
                        + " called without one, so only a method that takes none is found");
            }
        }
        return sb.toString();
    }

    /// The statements that build a controller into the variable `v`.
    static String controller(String label, List<UnityYaml.Document> docs, String v, Clips clips,
            List<String> warnings) {
        Map<Long, UnityYaml.Document> byId = new HashMap<Long, UnityYaml.Document>();
        Map<String, Object> p = null;
        for (UnityYaml.Document d : docs) {
            if (d.properties == null) {
                continue;
            }
            byId.put(Long.valueOf(d.fileId), d);
            if (d.classId == 91) {
                p = d.properties;
            }
        }
        if (p == null) {
            warnings.add(label + ": the file holds no animator controller; an animator that uses it does nothing");
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("            ").append(RUNTIME).append("RuntimeAnimatorController ").append(v).append(" = new ")
                .append(RUNTIME).append("RuntimeAnimatorController(")
                .append(SceneCompiler.string(SceneCompiler.text(p.get("m_Name")))).append(");\n");
        List<String> parameters = new ArrayList<String>();
        for (Object o : SceneCompiler.list(p.get("m_AnimatorParameters"))) {
            Map<String, Object> parameter = SceneCompiler.map(o);
            String name = SceneCompiler.text(parameter.get("m_Name"));
            int type = (int) Long.parseLong(SceneCompiler.integer(parameter.get("m_Type"), "1"));
            String initial = type == 1 ? SceneCompiler.f(parameter.get("m_DefaultFloat"), "0")
                    : type == 3 ? SceneCompiler.integer(parameter.get("m_DefaultInt"), "0") + "f"
                    : "1".equals(SceneCompiler.text(parameter.get("m_DefaultBool"))) ? "1f" : "0f";
            parameters.add(name);
            sb.append("            ").append(v).append(".$parameter(").append(SceneCompiler.string(name)).append(", ")
                    .append(type).append(", ").append(initial).append(");\n");
        }
        List<Object> layers = SceneCompiler.list(p.get("m_AnimatorLayers"));
        if (layers.isEmpty()) {
            return sb.toString();
        }
        if (layers.size() > 1) {
            warnings.add(label + ": the controller has " + layers.size() + " layers; only the first is played");
        }
        UnityYaml.Document machine = byId.get(SceneCompiler.fileId(SceneCompiler.map(layers.get(0))
                .get("m_StateMachine")));
        if (machine == null || machine.classId != 1107) {
            return sb.toString();
        }
        if (!SceneCompiler.list(machine.properties.get("m_ChildStateMachines")).isEmpty()) {
            warnings.add(label + ": the controller has sub-state machines, which are not supported; their states"
                    + " are never entered");
        }
        // Every state first, so that a transition can name one further on.
        Map<Long, Integer> stateIndex = new HashMap<Long, Integer>();
        List<UnityYaml.Document> states = new ArrayList<UnityYaml.Document>();
        for (Object o : SceneCompiler.list(machine.properties.get("m_ChildStates"))) {
            UnityYaml.Document state = byId.get(SceneCompiler.fileId(SceneCompiler.map(o).get("m_State")));
            if (state == null || state.classId != 1102) {
                continue;
            }
            Map<String, Object> s = state.properties;
            String name = SceneCompiler.text(s.get("m_Name"));
            Object motion = s.get("m_Motion");
            String clip = null;
            if (SceneCompiler.fileId(motion).longValue() != 0) {
                if (SceneCompiler.text(SceneCompiler.map(motion).get("guid")).length() == 0) {
                    warnings.add(label + ": the state " + name + " plays a blend tree, which is not supported; the"
                            + " state shows nothing");
                } else {
                    clip = clips.clip("the state " + name + " of " + label, motion);
                }
            }
            if ("1".equals(SceneCompiler.text(s.get("m_SpeedParameterActive")))) {
                warnings.add(label + ": the speed of the state " + name + " is multiplied by a parameter, which is"
                        + " not supported; it plays at its own speed");
            }
            stateIndex.put(Long.valueOf(state.fileId), Integer.valueOf(states.size()));
            states.add(state);
            sb.append("            ").append(v).append(".$state(").append(SceneCompiler.string(name)).append(", ")
                    .append(SceneCompiler.f(s.get("m_Speed"), "1")).append(", ").append(clip == null ? "null" : clip)
                    .append(", ").append(!"0".equals(SceneCompiler.text(s.get("m_WriteDefaultValues"))))
                    .append(");\n");
        }
        Integer first = stateIndex.get(SceneCompiler.fileId(machine.properties.get("m_DefaultState")));
        if (first != null) {
            sb.append("            ").append(v).append(".$defaultState(").append(first).append(");\n");
        }
        if (!SceneCompiler.list(machine.properties.get("m_EntryTransitions")).isEmpty()) {
            warnings.add(label + ": the controller has transitions out of Entry, which are not supported; it starts"
                    + " in its default state");
        }
        for (Object o : SceneCompiler.list(machine.properties.get("m_AnyStateTransitions"))) {
            transition(label, sb, v, -1, "Any State", byId.get(SceneCompiler.fileId(o)), stateIndex, parameters,
                    warnings);
        }
        for (int i = 0; i < states.size(); i++) {
            Map<String, Object> s = states.get(i).properties;
            for (Object o : SceneCompiler.list(s.get("m_Transitions"))) {
                transition(label, sb, v, i, SceneCompiler.text(s.get("m_Name")), byId.get(SceneCompiler.fileId(o)),
                        stateIndex, parameters, warnings);
            }
        }
        return sb.toString();
    }

    private static void transition(String label, StringBuilder sb, String v, int source, String sourceName,
            UnityYaml.Document d, Map<Long, Integer> stateIndex, List<String> parameters, List<String> warnings) {
        if (d == null || d.classId != 1101) {
            return;
        }
        Map<String, Object> t = d.properties;
        if ("1".equals(SceneCompiler.text(t.get("m_Mute")))) {
            return;
        }
        boolean exit = "1".equals(SceneCompiler.text(t.get("m_IsExit")));
        Integer target = stateIndex.get(SceneCompiler.fileId(t.get("m_DstState")));
        if (target == null && !exit) {
            warnings.add(label + ": a transition out of " + sourceName + " leads to a state machine or to a state"
                    + " that is not there; it was left out");
            return;
        }
        StringBuilder tests = new StringBuilder();
        for (Object o : SceneCompiler.list(t.get("m_Conditions"))) {
            Map<String, Object> condition = SceneCompiler.map(o);
            String name = SceneCompiler.text(condition.get("m_ConditionEvent"));
            int parameter = parameters.indexOf(name);
            if (parameter < 0) {
                warnings.add(label + ": a transition out of " + sourceName + " tests the parameter '" + name
                        + "', which the controller does not have; the transition is never taken");
                return;
            }
            tests.append(tests.length() == 0 ? "" : ", ")
                    .append(SceneCompiler.integer(condition.get("m_ConditionMode"), "1")).append("f, ")
                    .append(parameter).append("f, ").append(SceneCompiler.f(condition.get("m_EventTreshold"), "0"));
        }
        sb.append("            ").append(v).append(".$transition(").append(source).append(", ")
                .append(exit ? "-1" : target.toString()).append(", ")
                .append("1".equals(SceneCompiler.text(t.get("m_HasExitTime")))).append(", ")
                .append(SceneCompiler.f(t.get("m_ExitTime"), "1")).append(", ")
                .append(SceneCompiler.f(t.get("m_TransitionDuration"), "0")).append(", ")
                .append(!"0".equals(SceneCompiler.text(t.get("m_HasFixedDuration")))).append(", ")
                .append(SceneCompiler.f(t.get("m_TransitionOffset"), "0")).append(", ")
                .append(!"0".equals(SceneCompiler.text(t.get("m_CanTransitionToSelf")))).append(", new float[] {")
                .append(tests).append("});\n");
    }
}
