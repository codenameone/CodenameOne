/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.splat.play;

import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.Model;
import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.GltfScene;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// Every model the field is drawn with, and which one plays what.
///
/// The operators are MakeHuman characters animated with Quaternius' Universal
/// Animation Library (`tools/make-operators.py`), the markers Kenney's Blaster Kit
/// and the cover Quaternius' Toon Shooter and Kenney's Survival Kit, all CC0,
/// packed into `src/main/resources/model_*.glb` by the scripts in `tools/` (see
/// `ART.md`). Re-skin the game here: swap a name, or drop in any other skinned
/// glTF with the same clip names.
// tag::splat-models[]
public final class Models {
    /// The operators each team fields, in slot order.
    static final String[][] OPERATORS = {
        {"op-swat", "op-worker", "op-farmer", "op-punk", "op-ranger", "op-hoodie"},
        {"op-dad", "op-suit", "op-hoodie", "op-ranger", "op-punk", "op-worker"},
    };
    /// Clips every operator carries (`tools/make-operators.py` retargets them
    /// from Quaternius' Universal Animation Library onto the MakeHuman rig).
    static final String IDLE = "Idle";
    /// Standing, marker up and aimed: the pose a player holds.
    static final String AIM = "Aim";
    /// The aim pose pitched up and down, mixed into the spine by look pitch.
    static final String AIM_UP = "AimUp";
    static final String AIM_DOWN = "AimDown";
    static final String WALK = "Walk";
    static final String RUN = "Run";
    static final String CROUCH = "CrouchIdle";
    static final String CROUCH_WALK = "CrouchWalk";
    static final String DEATH = "Death";
    static final String HIT = "HitChest";
    static final String HIT2 = "Knockback";
    static final String ROLL = "Roll";
    /// Shaking the head: "it was a bounce", and the paint wiped off.
    static final String DISPUTE = "No";
    static final String DANCE = "Dance";
    /// The bone a marker is held by, the head, and where the aim layer starts.
    static final String HAND = "hand_r";
    static final String HEAD = "head";
    static final String SPINE = "spine_01";

    private static final Map<String, GltfScene> SCENES = new HashMap<String, GltfScene>();

    private Models() {
    }

    /// The operator for the `slot`th player of `team`.
    static String operator(int team, int slot) {
        String[] list = OPERATORS[team & 1];
        return list[Math.abs(slot) % list.length];
    }

    /// The model of marker `id` (`pump`, `semi`, `ramp`).
    static String marker(String id) {
        return "mk-" + id;
    }

    /// The parsed `/model_<name>.glb`, loaded once.
    public static synchronized GltfScene scene(String name) {
        GltfScene s = SCENES.get(name);
        if (s == null) {
            InputStream in = CN.getResourceAsStream("/model_" + name + ".glb");
            if (in == null) {
                throw new IllegalArgumentException("No model " + name);
            }
            try {
                s = GltfLoader.loadScene(in);
            } catch (java.io.IOException e) {
                Log.e(e);
                throw new IllegalStateException("Could not read model " + name);
            } finally {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                    // read already
                }
            }
            SCENES.put(name, s);
        }
        return s;
    }

    /// A new instance of `name`.
    static AnimatedModel create(GraphicsDevice d, String name) {
        AnimatedModel m = new AnimatedModel(d, scene(name));
        m.setMaterialType(Material.Type.PBR);
        return m;
    }

    /// The bounds of the current pose, node transforms applied:
    /// `minX, minY, minZ, maxX, maxY, maxZ`.
    static float[] bounds(AnimatedModel m) {
        m.pose();
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
            -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        Model[] parts = m.getParts();
        for (int p = 0; p < parts.length; p++) {
            if (!parts[p].isVisible()) {
                continue;
            }
            float[] d = parts[p].getMesh().getVertices().getData();
            for (int i = 0; i + 2 < d.length; i += 8) {
                for (int k = 0; k < 3; k++) {
                    b[k] = Math.min(b[k], d[i + k]);
                    b[k + 3] = Math.max(b[k + 3], d[i + k]);
                }
            }
        }
        if (b[0] > b[3]) {
            return new float[] {-0.5f, 0f, -0.5f, 0.5f, 1f, 0.5f};
        }
        return b;
    }
}
// end::splat-models[]
