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
package com.codenameone.examples.dash.run;

import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.Model;
import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.GltfScene;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// Which model draws which runner, and the parsed glTF files, loaded once.
///
/// A runner id is what the shop sells (`runner-dash`, `runner-nova`, ...);
/// each maps to a bundled `/model_<name>.glb`. Add a runner by adding a
/// catalog item on the server and a line to [#modelFor].
public final class Models {
    private static final Map<String, GltfScene> SCENES = new HashMap<String, GltfScene>();
    /// The models a bot or an unknown runner wears, in turn.
    private static final String[] CROWD = {"knight", "skeleton_rogue", "rogue_hooded", "skeleton_warrior"};

    private Models() {
    }

    /// The bundled model for `runnerId`; `fallback` picks one for a bot.
    public static String modelFor(String runnerId, int fallback) {
        if ("runner-dash".equals(runnerId)) {
            return "rogue";
        }
        if ("runner-nova".equals(runnerId)) {
            return "mage";
        }
        if ("runner-kite".equals(runnerId)) {
            return "barbarian";
        }
        return CROWD[Math.abs(fallback) % CROWD.length];
    }

    /// The parsed model `name`, from `/model_<name>.glb`.
    public static synchronized GltfScene scene(String name) {
        GltfScene s = SCENES.get(name);
        if (s == null) {
            InputStream in = CN.getResourceAsStream("/model_" + name + ".glb");
            if (in == null) {
                throw new IllegalStateException("missing model " + name);
            }
            try {
                s = GltfLoader.loadScene(in);
            } catch (java.io.IOException err) {
                Log.e(err);
                throw new IllegalStateException("unreadable model " + name);
            }
            SCENES.put(name, s);
        }
        return s;
    }

    /// The axis-aligned bounds of the model's rest pose:
    /// `minX, minY, minZ, maxX, maxY, maxZ`.
    static float[] bounds(AnimatedModel m) {
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
        return b;
    }

    /// `name` when `m` has that clip, else null.
    static String clip(AnimatedModel m, String name) {
        return m.getScene().findAnimation(name) != null ? name : null;
    }
}
