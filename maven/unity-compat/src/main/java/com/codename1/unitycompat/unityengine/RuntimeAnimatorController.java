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

/// `UnityEngine.RuntimeAnimatorController`: the state machine an
/// [Animator] runs -- its parameters, its states, each with the clip it
/// plays, and the transitions between them.
///
/// The scene compiler reads a `.controller` file and writes the calls that
/// build one of these. One controller is shared by every animator that
/// names it; what differs between two animators -- where each is in the
/// machine, what its parameters hold -- is in the [Animator].
///
/// Only the first layer of a controller is run, and a state plays one clip:
/// a blend tree plays its first.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class RuntimeAnimatorController extends Object {
    /// Parameter types, by the numbers a controller file uses.
    public static final int FLOAT = 1;
    public static final int INT = 3;
    public static final int BOOL = 4;
    public static final int TRIGGER = 9;

    /// Condition modes, by the numbers a controller file uses.
    static final int IF = 1;
    static final int IF_NOT = 2;
    static final int GREATER = 3;
    static final int LESS = 4;
    static final int EQUALS = 6;
    static final int NOT_EQUAL = 7;

    int parameterCount;
    String[] parameterNames = new String[4];
    int[] parameterHashes = new int[4];
    int[] parameterTypes = new int[4];
    float[] parameterDefaults = new float[4];

    int stateCount;
    String[] stateNames = new String[4];
    int[] stateHashes = new int[4];
    float[] stateSpeeds = new float[4];
    AnimationClip[] stateClips = new AnimationClip[4];
    boolean[] stateWritesDefaults = new boolean[4];
    /// For each state, the slot each curve of its clip drives.
    int[][] stateSlots = new int[4][];
    int defaultState;

    int transitionCount;
    /// The state a transition leaves, or -1 for one that leaves any state.
    int[] from = new int[8];
    int[] to = new int[8];
    boolean[] hasExitTime = new boolean[8];
    float[] exitTime = new float[8];
    float[] duration = new float[8];
    boolean[] fixedDuration = new boolean[8];
    float[] offset = new float[8];
    boolean[] toSelf = new boolean[8];
    /// Three to a condition: its mode, the index of its parameter, and the
    /// number it compares with.
    float[][] conditions = new float[8][];

    /// What the clips of all the states drive between them: each distinct
    /// pair of a path and a kind once.
    int slotCount;
    String[] slotPaths = new String[8];
    int[] slotKinds = new int[8];

    public RuntimeAnimatorController(String name) {
        this.name = name;
    }

    /// The hash Unity gives a name: its CRC-32.
    static int hash(String name) {
        int crc = 0xffffffff;
        for (int i = 0; i < name.length(); i++) {
            crc ^= name.charAt(i) & 0xff;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 1) != 0 ? (crc >>> 1) ^ 0xedb88320 : crc >>> 1;
            }
        }
        return ~crc;
    }

    public void $parameter(String parameterName, int type, float initial) {
        if (parameterCount == parameterNames.length) {
            int n = parameterCount * 2;
            String[] names = new String[n];
            int[] hashes = new int[n];
            int[] types = new int[n];
            float[] defaults = new float[n];
            System.arraycopy(parameterNames, 0, names, 0, parameterCount);
            System.arraycopy(parameterHashes, 0, hashes, 0, parameterCount);
            System.arraycopy(parameterTypes, 0, types, 0, parameterCount);
            System.arraycopy(parameterDefaults, 0, defaults, 0, parameterCount);
            parameterNames = names;
            parameterHashes = hashes;
            parameterTypes = types;
            parameterDefaults = defaults;
        }
        parameterNames[parameterCount] = parameterName;
        parameterHashes[parameterCount] = hash(parameterName);
        parameterTypes[parameterCount] = type;
        parameterDefaults[parameterCount] = initial;
        parameterCount++;
    }

    /// Adds a state and answers its index. A state with no clip holds
    /// whatever was there for a second at a time.
    public int $state(String stateName, float speed, AnimationClip clip, boolean writesDefaults) {
        if (stateCount == stateNames.length) {
            int n = stateCount * 2;
            String[] names = new String[n];
            int[] hashes = new int[n];
            float[] speeds = new float[n];
            AnimationClip[] clips = new AnimationClip[n];
            boolean[] writes = new boolean[n];
            int[][] slots = new int[n][];
            System.arraycopy(stateNames, 0, names, 0, stateCount);
            System.arraycopy(stateHashes, 0, hashes, 0, stateCount);
            System.arraycopy(stateSpeeds, 0, speeds, 0, stateCount);
            System.arraycopy(stateClips, 0, clips, 0, stateCount);
            System.arraycopy(stateWritesDefaults, 0, writes, 0, stateCount);
            System.arraycopy(stateSlots, 0, slots, 0, stateCount);
            stateNames = names;
            stateHashes = hashes;
            stateSpeeds = speeds;
            stateClips = clips;
            stateWritesDefaults = writes;
            stateSlots = slots;
        }
        stateNames[stateCount] = stateName;
        stateHashes[stateCount] = hash(stateName);
        stateSpeeds[stateCount] = speed;
        stateClips[stateCount] = clip;
        stateWritesDefaults[stateCount] = writesDefaults;
        int[] slots = new int[clip == null ? 0 : clip.curveCount];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = slot(clip.paths[i], clip.kinds[i]);
        }
        stateSlots[stateCount] = slots;
        return stateCount++;
    }

    private int slot(String path, int kind) {
        for (int i = 0; i < slotCount; i++) {
            if (slotKinds[i] == kind && slotPaths[i].equals(path)) {
                return i;
            }
        }
        if (slotCount == slotPaths.length) {
            String[] paths = new String[slotCount * 2];
            int[] kinds = new int[slotCount * 2];
            System.arraycopy(slotPaths, 0, paths, 0, slotCount);
            System.arraycopy(slotKinds, 0, kinds, 0, slotCount);
            slotPaths = paths;
            slotKinds = kinds;
        }
        slotPaths[slotCount] = path;
        slotKinds[slotCount] = kind;
        return slotCount++;
    }

    public void $defaultState(int state) {
        defaultState = state;
    }

    /// Adds a transition. `source` is -1 for one that leaves any state, and
    /// `target` -1 for one that leaves the machine, which lands on the
    /// default state. `tests` holds three numbers to a condition.
    public void $transition(int source, int target, boolean exits, float exitAt, float lasts, boolean fixed,
            float startsAt, boolean canReenter, float[] tests) {
        if (transitionCount == from.length) {
            int n = transitionCount * 2;
            int[] f = new int[n];
            int[] t = new int[n];
            boolean[] he = new boolean[n];
            float[] et = new float[n];
            float[] d = new float[n];
            boolean[] fd = new boolean[n];
            float[] o = new float[n];
            boolean[] ts = new boolean[n];
            float[][] c = new float[n][];
            System.arraycopy(from, 0, f, 0, transitionCount);
            System.arraycopy(to, 0, t, 0, transitionCount);
            System.arraycopy(hasExitTime, 0, he, 0, transitionCount);
            System.arraycopy(exitTime, 0, et, 0, transitionCount);
            System.arraycopy(duration, 0, d, 0, transitionCount);
            System.arraycopy(fixedDuration, 0, fd, 0, transitionCount);
            System.arraycopy(offset, 0, o, 0, transitionCount);
            System.arraycopy(toSelf, 0, ts, 0, transitionCount);
            System.arraycopy(conditions, 0, c, 0, transitionCount);
            from = f;
            to = t;
            hasExitTime = he;
            exitTime = et;
            duration = d;
            fixedDuration = fd;
            offset = o;
            toSelf = ts;
            conditions = c;
        }
        from[transitionCount] = source;
        to[transitionCount] = target;
        hasExitTime[transitionCount] = exits;
        exitTime[transitionCount] = exitAt;
        duration[transitionCount] = lasts;
        fixedDuration[transitionCount] = fixed;
        offset[transitionCount] = startsAt;
        toSelf[transitionCount] = canReenter;
        conditions[transitionCount] = tests;
        transitionCount++;
    }

    /// The index of the parameter a name or its hash stands for, or -1.
    int parameter(String parameterName) {
        for (int i = 0; i < parameterCount; i++) {
            if (parameterNames[i].equals(parameterName)) {
                return i;
            }
        }
        return -1;
    }

    int parameter(int id) {
        for (int i = 0; i < parameterCount; i++) {
            if (parameterHashes[i] == id) {
                return i;
            }
        }
        return -1;
    }

    int state(String stateName) {
        // A state may be named with its layer in front: "Base Layer.Run".
        int dot = stateName.lastIndexOf('.');
        String shortName = dot >= 0 ? stateName.substring(dot + 1) : stateName;
        for (int i = 0; i < stateCount; i++) {
            if (stateNames[i].equals(shortName)) {
                return i;
            }
        }
        return -1;
    }

    int state(int id) {
        for (int i = 0; i < stateCount; i++) {
            if (stateHashes[i] == id) {
                return i;
            }
        }
        return -1;
    }

    /// How long a state lasts at a speed of one.
    float length(int state) {
        AnimationClip clip = stateClips[state];
        return clip != null && clip.length > 0f ? clip.length : 1f;
    }

    public AnimationClip[] get_animationClips() {
        int n = 0;
        for (int i = 0; i < stateCount; i++) {
            if (stateClips[i] != null) {
                n++;
            }
        }
        AnimationClip[] out = new AnimationClip[n];
        n = 0;
        for (int i = 0; i < stateCount; i++) {
            if (stateClips[i] != null) {
                out[n++] = stateClips[i];
            }
        }
        return out;
    }
}
