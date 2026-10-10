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
package com.codename1.unitycompat.unityengine;

import java.util.ArrayList;

/// `UnityEngine.Animator`: runs the state machine of a
/// [RuntimeAnimatorController] and writes what its clips say into the
/// objects below it.
///
/// Once a frame, after every `Update` and before any `LateUpdate`, an
/// animator looks at the transitions out of the state it is in -- those
/// that leave any state first -- and takes the first whose conditions
/// hold, then moves its clip on by the frame's time and applies it.
///
/// A transition with a duration plays both states for that long. A number
/// is blended between them; a sprite, which cannot be, is the destination's
/// from halfway. While one transition runs no other starts.
///
/// A property a clip of the controller animates and the clip now playing
/// does not is written back to the value it had when the animator first
/// ran, in a state that writes defaults -- which is how Unity makes them.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Animator extends Behaviour {
    private RuntimeAnimatorController controller;
    private float speed = 1f;
    private float[] parameters = new float[0];
    private int state = -1;
    /// Seconds of the clip played since the state was entered.
    private float time;
    private int next = -1;
    private float nextTime;
    private float blendTime;
    private float blendLength;
    private boolean bound;
    private Transform[] targets = new Transform[0];
    private SpriteRenderer[] painted = new SpriteRenderer[0];
    private float[] defaults = new float[0];
    private Sprite[] defaultSprites = new Sprite[0];
    /// For each slot, whether the state playing drives it, and the same for
    /// the state a transition is going to.
    private float[] values = new float[0];
    private float[] nextValues = new float[0];
    private boolean[] driven = new boolean[0];
    private boolean[] nextDriven = new boolean[0];
    private Sprite[] shown = new Sprite[0];
    private Sprite[] nextShown = new Sprite[0];

    @Override
    public int $roles() {
        return TICKS;
    }

    @Override
    public Component $new() {
        return new Animator();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Animator a = (Animator) source;
        speed = a.speed;
        set_runtimeAnimatorController(a.controller);
    }

    /// What a scene file sets.
    public void $setup(RuntimeAnimatorController machine) {
        set_runtimeAnimatorController(machine);
    }

    public RuntimeAnimatorController get_runtimeAnimatorController() {
        return controller;
    }

    /// Changing the controller starts the new one from its default state,
    /// with its parameters as the controller declares them.
    public void set_runtimeAnimatorController(RuntimeAnimatorController value) {
        if (bound) {
            restore();
        }
        controller = value;
        state = -1;
        next = -1;
        time = 0f;
        bound = false;
        int n = value == null ? 0 : value.parameterCount;
        parameters = new float[n];
        for (int i = 0; i < n; i++) {
            parameters[i] = value.parameterDefaults[i];
        }
    }

    public float get_speed() {
        return speed;
    }

    public void set_speed(float value) {
        speed = value;
    }

    public static int StringToHash(String name) {
        return RuntimeAnimatorController.hash(name);
    }

    // ---------------------------------------------------------- parameters

    private int find(String name) {
        return controller == null ? -1 : controller.parameter(name);
    }

    private int find(int id) {
        return controller == null ? -1 : controller.parameter(id);
    }

    private void set(int index, float value) {
        if (index >= 0) {
            parameters[index] = value;
        }
    }

    private float get(int index) {
        return index >= 0 ? parameters[index] : 0f;
    }

    public void SetFloat(String name, float value) {
        set(find(name), value);
    }

    public void SetFloat(int id, float value) {
        set(find(id), value);
    }

    public float GetFloat(String name) {
        return get(find(name));
    }

    public float GetFloat(int id) {
        return get(find(id));
    }

    public void SetBool(String name, boolean value) {
        set(find(name), value ? 1f : 0f);
    }

    public void SetBool(int id, boolean value) {
        set(find(id), value ? 1f : 0f);
    }

    public boolean GetBool(String name) {
        return get(find(name)) != 0f;
    }

    public boolean GetBool(int id) {
        return get(find(id)) != 0f;
    }

    public void SetInteger(String name, int value) {
        set(find(name), value);
    }

    public void SetInteger(int id, int value) {
        set(find(id), value);
    }

    public int GetInteger(String name) {
        return (int) get(find(name));
    }

    public int GetInteger(int id) {
        return (int) get(find(id));
    }

    public void SetTrigger(String name) {
        set(find(name), 1f);
    }

    public void SetTrigger(int id) {
        set(find(id), 1f);
    }

    public void ResetTrigger(String name) {
        set(find(name), 0f);
    }

    public void ResetTrigger(int id) {
        set(find(id), 0f);
    }

    // -------------------------------------------------------------- states

    private void enter(int target, float normalized) {
        state = target;
        next = -1;
        time = target < 0 ? 0f : normalized * controller.length(target);
    }

    public void Play(String stateName) {
        Play(stateName, 0, 0f);
    }

    public void Play(String stateName, int layer) {
        Play(stateName, layer, 0f);
    }

    public void Play(String stateName, int layer, float normalizedTime) {
        int target = controller == null ? -1 : controller.state(stateName);
        if (target >= 0) {
            enter(target, normalizedTime > 0f ? normalizedTime : 0f);
        }
    }

    public void Play(int stateNameHash) {
        Play(stateNameHash, 0, 0f);
    }

    public void Play(int stateNameHash, int layer) {
        Play(stateNameHash, layer, 0f);
    }

    public void Play(int stateNameHash, int layer, float normalizedTime) {
        int target = controller == null ? -1 : controller.state(stateNameHash);
        if (target >= 0) {
            enter(target, normalizedTime > 0f ? normalizedTime : 0f);
        }
    }

    public void CrossFade(String stateName, float normalizedTransitionDuration) {
        CrossFade(stateName, normalizedTransitionDuration, 0);
    }

    public void CrossFade(String stateName, float normalizedTransitionDuration, int layer) {
        int target = controller == null ? -1 : controller.state(stateName);
        if (target < 0) {
            return;
        }
        if (state < 0 || !(normalizedTransitionDuration > 0f)) { // NOPMD LogicInversion
            enter(target, 0f);
            return;
        }
        next = target;
        nextTime = 0f;
        blendTime = 0f;
        blendLength = normalizedTransitionDuration * controller.length(state);
    }

    public AnimatorStateInfo GetCurrentAnimatorStateInfo(int layerIndex, AnimatorStateInfo ret) {
        ret.$clear();
        if (controller == null) {
            return ret;
        }
        int at = state < 0 ? controller.defaultState : state;
        if (at < 0 || at >= controller.stateCount) {
            return ret;
        }
        float length = controller.length(at);
        ret.nameHash = controller.stateHashes[at];
        ret.stateName = controller.stateNames[at];
        ret.length = length;
        ret.speed = controller.stateSpeeds[at];
        ret.normalizedTime = time / length;
        AnimationClip clip = controller.stateClips[at];
        ret.loop = clip != null && clip.loop;
        return ret;
    }

    public boolean IsInTransition(int layerIndex) {
        return next >= 0;
    }

    /// Puts back what the animator found and starts its machine again.
    public void Rebind() {
        set_runtimeAnimatorController(controller);
    }

    // ------------------------------------------------------------- running

    private boolean holds(float[] tests) {
        for (int i = 0; i + 2 < tests.length; i += 3) {
            int mode = (int) tests[i];
            int index = (int) tests[i + 1];
            float limit = tests[i + 2];
            float value = index >= 0 && index < parameters.length ? parameters[index] : 0f;
            boolean ok;
            switch (mode) {
                case RuntimeAnimatorController.IF:
                    ok = value != 0f;
                    break;
                case RuntimeAnimatorController.IF_NOT:
                    ok = value == 0f;
                    break;
                case RuntimeAnimatorController.GREATER:
                    ok = value > limit;
                    break;
                case RuntimeAnimatorController.LESS:
                    ok = value < limit;
                    break;
                case RuntimeAnimatorController.EQUALS:
                    ok = value == limit;
                    break;
                case RuntimeAnimatorController.NOT_EQUAL:
                    ok = value != limit;
                    break;
                default:
                    ok = false;
                    break;
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /// The first transition out of the state that may be taken in a frame
    /// that moves the state's clip from `before` to `after`, both as
    /// fractions of its length. Those that leave any state come first.
    private int choose(float before, float after) {
        RuntimeAnimatorController c = controller;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < c.transitionCount; i++) {
                if (pass == 0 ? c.from[i] != -1 : c.from[i] != state) {
                    continue;
                }
                int target = c.to[i] < 0 ? c.defaultState : c.to[i];
                if (pass == 0 && target == state && !c.toSelf[i]) {
                    continue;
                }
                float[] tests = c.conditions[i];
                boolean exits = c.hasExitTime[i];
                // Unity never takes a transition with neither a condition
                // nor an exit time.
                if (tests.length == 0 && !exits) {
                    continue;
                }
                if (exits) {
                    float at = c.exitTime[i];
                    boolean crossed;
                    if (at <= 1f) {
                        // Once a loop: when the whole number of exit times
                        // behind the clip goes up. A clip that does not
                        // loop counts loops all the same -- its time goes
                        // on past its end -- and that is what gets a
                        // state out when the transition into it lasted
                        // past its exit time: it leaves a loop later,
                        // where a test for the one crossing never would.
                        float b = before - at;
                        float a = after - at;
                        crossed = floor(a) > floor(b);
                    } else {
                        crossed = before < at && after >= at;
                    }
                    if (!crossed) {
                        continue;
                    }
                }
                if (holds(tests)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int floor(float v) {
        int whole = (int) v;
        return whole > v ? whole - 1 : whole;
    }

    @Override
    public void $tick(float dt) {
        RuntimeAnimatorController c = controller;
        if (c == null || c.stateCount == 0) {
            return;
        }
        if (!bound) {
            bind();
        }
        if (state < 0) {
            enter(c.defaultState, 0f);
        }
        float step = dt * speed;
        float length = c.length(state);
        float advance = step * c.stateSpeeds[state];
        float before = time / length;
        float moved = time + advance;
        float after = moved / length;
        if (next < 0) {
            int taken = choose(before, after);
            if (taken >= 0) {
                // The triggers a transition tested are spent by taking it.
                float[] tests = c.conditions[taken];
                for (int i = 0; i + 2 < tests.length; i += 3) {
                    int index = (int) tests[i + 1];
                    if (index >= 0 && index < c.parameterCount
                            && c.parameterTypes[index] == RuntimeAnimatorController.TRIGGER) {
                        parameters[index] = 0f;
                    }
                }
                int target = c.to[taken] < 0 ? c.defaultState : c.to[taken];
                float lasts = c.duration[taken];
                if (!c.fixedDuration[taken]) {
                    lasts = lasts * length;
                }
                float start = c.offset[taken] * c.length(target);
                if (lasts > 0f) {
                    next = target;
                    nextTime = start;
                    blendTime = 0f;
                    blendLength = lasts;
                } else {
                    events(c.stateClips[state], time, moved);
                    state = target;
                    time = start;
                    apply();
                    return;
                }
            }
        }
        events(c.stateClips[state], time, moved);
        time = moved;
        if (next >= 0) {
            float nextAdvance = step * c.stateSpeeds[next];
            nextTime = nextTime + nextAdvance;
            blendTime = blendTime + step;
            if (blendTime >= blendLength) {
                state = next;
                time = nextTime;
                next = -1;
            }
        }
        apply();
    }

    /// Calls the methods a clip names between two of its times.
    private void events(AnimationClip clip, float from, float until) {
        if (clip == null || clip.eventCount == 0 || !(until > from)) { // NOPMD LogicInversion
            return;
        }
        float length = clip.length > 0f ? clip.length : 1f;
        for (int i = 0; i < clip.eventCount; i++) {
            float at = clip.eventTimes[i];
            boolean due;
            if (clip.loop) {
                float b = (from - at) / length;
                float a = (until - at) / length;
                due = floor(a) > floor(b) || (from == 0f && at == 0f);
            } else {
                due = (from < at || (from == 0f && at == 0f)) && until >= at;
            }
            if (due) {
                ArrayList components = gameObject.components;
                for (int k = 0; k < components.size(); k++) { // NOPMD ForLoopCanBeForeach
                    java.lang.Object o = components.get(k);
                    if (o instanceof MonoBehaviour) {
                        ((MonoBehaviour) o).$invoke(clip.eventNames[i]);
                    }
                }
            }
        }
    }

    private static Transform resolve(Transform root, String path) {
        if (path.length() == 0) {
            return root;
        }
        Transform at = root;
        int from = 0;
        while (at != null && from <= path.length()) {
            int slash = path.indexOf('/', from);
            if (slash < 0) {
                slash = path.length();
            }
            at = at.Find(path.substring(from, slash));
            from = slash + 1;
        }
        return at;
    }

    private float read(int slot) {
        Transform t = targets[slot];
        SpriteRenderer r = painted[slot];
        switch (controller.slotKinds[slot]) {
            case AnimationClip.POSITION_X:
                return t.x;
            case AnimationClip.POSITION_Y:
                return t.y;
            case AnimationClip.POSITION_Z:
                return t.z;
            case AnimationClip.ROTATION_Z:
                return t.rotationZ;
            case AnimationClip.SCALE_X:
                return t.scaleX;
            case AnimationClip.SCALE_Y:
                return t.scaleY;
            case AnimationClip.SCALE_Z:
                return t.scaleZ;
            case AnimationClip.COLOR_R:
                return r == null ? 1f : r.red();
            case AnimationClip.COLOR_G:
                return r == null ? 1f : r.green();
            case AnimationClip.COLOR_B:
                return r == null ? 1f : r.blue();
            case AnimationClip.COLOR_A:
                return r == null ? 1f : r.alpha();
            case AnimationClip.RENDERER_ENABLED:
                return r == null || r.enabled ? 1f : 0f;
            case AnimationClip.ACTIVE:
                return t.gameObject.active ? 1f : 0f;
            case AnimationClip.FLIP_X:
                return r != null && r.flipX ? 1f : 0f;
            case AnimationClip.FLIP_Y:
                return r != null && r.flipY ? 1f : 0f;
            default:
                return 0f;
        }
    }

    private void write(int slot, float v) {
        Transform t = targets[slot];
        SpriteRenderer r = painted[slot];
        switch (controller.slotKinds[slot]) {
            case AnimationClip.POSITION_X:
                if (t.x != v) {
                    t.x = v;
                    t.invalidate();
                }
                break;
            case AnimationClip.POSITION_Y:
                if (t.y != v) {
                    t.y = v;
                    t.invalidate();
                }
                break;
            case AnimationClip.POSITION_Z:
                if (t.z != v) {
                    t.z = v;
                    t.invalidate();
                }
                break;
            case AnimationClip.ROTATION_Z:
                if (t.rotationZ != v) {
                    t.rotationZ = v;
                    t.invalidate();
                }
                break;
            case AnimationClip.SCALE_X:
                if (t.scaleX != v) {
                    t.scaleX = v;
                    t.invalidate();
                }
                break;
            case AnimationClip.SCALE_Y:
                if (t.scaleY != v) {
                    t.scaleY = v;
                    t.invalidate();
                }
                break;
            case AnimationClip.SCALE_Z:
                if (t.scaleZ != v) {
                    t.scaleZ = v;
                    t.invalidate();
                }
                break;
            case AnimationClip.COLOR_R:
                if (r != null && r.red() != v) {
                    r.$tint(v, r.green(), r.blue(), r.alpha());
                }
                break;
            case AnimationClip.COLOR_G:
                if (r != null && r.green() != v) {
                    r.$tint(r.red(), v, r.blue(), r.alpha());
                }
                break;
            case AnimationClip.COLOR_B:
                if (r != null && r.blue() != v) {
                    r.$tint(r.red(), r.green(), v, r.alpha());
                }
                break;
            case AnimationClip.COLOR_A:
                if (r != null && r.alpha() != v) {
                    r.$tint(r.red(), r.green(), r.blue(), v);
                }
                break;
            case AnimationClip.RENDERER_ENABLED:
                if (r != null) {
                    r.enabled = v > 0.5f;
                }
                break;
            case AnimationClip.ACTIVE:
                if (t.gameObject.active != v > 0.5f && t != gameObject.transform) { // NOPMD CompareObjectsWithEquals
                    t.gameObject.SetActive(v > 0.5f);
                }
                break;
            case AnimationClip.FLIP_X:
                if (r != null) {
                    r.flipX = v > 0.5f;
                }
                break;
            case AnimationClip.FLIP_Y:
                if (r != null) {
                    r.flipY = v > 0.5f;
                }
                break;
            default:
                break;
        }
    }

    /// Finds what each slot of the controller drives below this object and
    /// remembers the value each has now.
    private void bind() {
        RuntimeAnimatorController c = controller;
        int n = c.slotCount;
        targets = new Transform[n];
        painted = new SpriteRenderer[n];
        defaults = new float[n];
        defaultSprites = new Sprite[n];
        values = new float[n];
        nextValues = new float[n];
        driven = new boolean[n];
        nextDriven = new boolean[n];
        shown = new Sprite[n];
        nextShown = new Sprite[n];
        Transform root = gameObject.transform;
        for (int i = 0; i < n; i++) {
            Transform t = resolve(root, c.slotPaths[i]);
            targets[i] = t;
            if (t == null) {
                continue;
            }
            int kind = c.slotKinds[i];
            if (kind >= AnimationClip.COLOR_R && kind != AnimationClip.ACTIVE) {
                java.lang.Object r = t.gameObject.GetComponent(SpriteRenderer.class);
                painted[i] = r instanceof SpriteRenderer ? (SpriteRenderer) r : null;
            }
            if (kind == AnimationClip.SPRITE) {
                defaultSprites[i] = painted[i] == null ? null : painted[i].sprite;
            } else {
                defaults[i] = read(i);
            }
        }
        bound = true;
    }

    /// Writes back what [#bind()] found, before another controller is run.
    private void restore() {
        RuntimeAnimatorController c = controller;
        if (c == null || destroyed) {
            return;
        }
        for (int i = 0; i < c.slotCount && i < targets.length; i++) {
            Transform t = targets[i];
            if (t == null || t.destroyed) {
                continue;
            }
            if (c.slotKinds[i] == AnimationClip.SPRITE) {
                if (painted[i] != null) {
                    painted[i].sprite = defaultSprites[i];
                }
            } else {
                write(i, defaults[i]);
            }
        }
    }

    /// The values of a state's clip at a time, into `out` and `sprites`,
    /// with `set` saying which slots the clip drives.
    private void sample(int at, float seconds, float[] out, Sprite[] sprites, boolean[] set) {
        RuntimeAnimatorController c = controller;
        for (int i = 0; i < set.length; i++) {
            set[i] = false;
        }
        AnimationClip clip = c.stateClips[at];
        if (clip == null) {
            return;
        }
        float t = seconds;
        float length = clip.length;
        if (length > 0f) {
            if (clip.loop) {
                t = t % length;
                if (t < 0f) {
                    t += length;
                }
            } else if (t > length) {
                t = length;
            }
        } else {
            t = 0f;
        }
        int[] slots = c.stateSlots[at];
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            set[slot] = true;
            if (clip.kinds[i] == AnimationClip.SPRITE) {
                Sprite[] frames = clip.sprites[i];
                sprites[slot] = frames.length == 0 ? null : frames[AnimationClip.frame(clip.keys[i], t)];
            } else {
                out[slot] = AnimationClip.sample(clip.keys[i], t);
            }
        }
    }

    private void apply() {
        RuntimeAnimatorController c = controller;
        sample(state, time, values, shown, driven);
        float weight = 0f;
        if (next >= 0) {
            sample(next, nextTime, nextValues, nextShown, nextDriven);
            weight = blendLength > 0f ? blendTime / blendLength : 1f;
        }
        boolean writes = c.stateWritesDefaults[state];
        for (int i = 0; i < c.slotCount; i++) {
            Transform t = targets[i];
            if (t == null || t.destroyed) {
                continue;
            }
            boolean sprite = c.slotKinds[i] == AnimationClip.SPRITE;
            boolean has = driven[i];
            float v = has ? values[i] : defaults[i];
            Sprite s = has ? shown[i] : defaultSprites[i];
            if (next >= 0) {
                boolean nextHas = nextDriven[i];
                if (!has && !nextHas && !writes) {
                    continue;
                }
                float w = nextHas ? nextValues[i] : defaults[i];
                float span = w - v;
                float part = span * weight;
                v = v + part;
                if (weight >= 0.5f) {
                    s = nextHas ? nextShown[i] : defaultSprites[i];
                }
            } else if (!has && !writes) {
                continue;
            }
            if (sprite) {
                if (painted[i] != null) {
                    painted[i].sprite = s;
                }
            } else {
                write(i, v);
            }
        }
    }
}
