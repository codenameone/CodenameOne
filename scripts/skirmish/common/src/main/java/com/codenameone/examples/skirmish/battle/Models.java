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
package com.codenameone.examples.skirmish.battle;

import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.GltfScene;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.forge.GlbInspector;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// The 3D look of every card: which model plays it, how big it stands, which
/// clip is its attack and whether it flies.
///
/// This table is the one place to re-skin a unit. The models are KayKit
/// characters and buildings by Kay Lousberg (CC0), packed into
/// `src/main/resources/model_*.glb` by `tools/pack-models.py`; drop in any other
/// skinned glTF with an `Idle`, `Running_A`, `Death_A` and an attack clip and
/// name it here.
// tag::skirmish-unit-looks[]
public final class Models {
    /// How one card looks on the field.
    public static final class Look {
        public final String model;
        /// Height in tiles for a character; footprint width in tiles for a
        /// building.
        public final float size;
        public final String attack;
        public final float hover;
        public final int tint;
        public final String idle;
        public final String run;
        public final String death;

        Look(String model, float size, String attack, float hover, int tint) {
            this(model, size, attack, hover, tint, "Idle", "Running_A", "Death_A");
        }

        Look(String model, float size, String attack, float hover, int tint, String idle, String run,
                String death) {
            this.model = model;
            this.size = size;
            this.attack = attack;
            this.hover = hover;
            this.tint = tint;
            this.idle = idle;
            this.run = run;
            this.death = death;
        }
    }

    private static final Map<String, Look> LOOKS = new HashMap<String, Look>();
    private static final Map<String, GltfScene> SCENES = new HashMap<String, GltfScene>();

    static {
        look("squire", "knight", 1.96f, "1H_Melee_Attack_Chop", 0f, 0xffffffff);
        look("twin-archers", "rogue_hooded", 1.67f, "1H_Ranged_Shoot", 0f, 0xffffffff);
        look("ogre", "barbarian", 3.04f, "2H_Melee_Attack_Chop", 0f, 0xffffffff);
        look("bat-swarm", "skeleton_rogue", 1.16f, "1H_Melee_Attack_Chop", 1.6f, 0xffb8c8ff);
        look("sharpshooter", "rogue", 1.89f, "2H_Ranged_Shoot", 0f, 0xffffffff);
        look("spearmen", "skeleton_warrior", 1.74f, "1H_Melee_Attack_Chop", 0f, 0xffffffff);
        look("drake", "mage", 2.03f, "Spellcast_Shoot", 1.8f, 0xffffffff);
        look("imp", "skeleton_minion", 1.38f, "Unarmed_Melee_Attack_Punch_A", 0f, 0xffffffff);
        look("imp-den", "crypt", 3.77f, null, 0f, 0xffffffff);
    }

    private Models() {
    }

    private static void look(String card, String model, float size, String attack, float hover, int tint) {
        LOOKS.put(card, new Look(model, size, attack, hover, tint));
    }

    /// The look of `card`, or null for a spell or a tower (drawn by the view).
    public static Look of(String card) {
        return LOOKS.get(card);
    }

    /// The look of a card from a Forge pack: its own model, bundled or
    /// uploaded, its size and tint, and the clips its author mapped. A card
    /// with no model of its own falls back to a bundled one by kind.
    public static Look of(CardDef card) {
        Look builtin = LOOKS.get(card.id);
        if (builtin != null || card.kind == CardDef.SPELL || card.kind == CardDef.TOWER) {
            return builtin;
        }
        String model = card.model();
        if (model == null || model.length() == 0) {
            model = card.kind == CardDef.BUILDING ? "crypt" : card.isFlying() ? "skeleton_rogue" : "knight";
        }
        float size = (card.kind == CardDef.BUILDING ? 3.5f : 1.9f) * Math.max(25, card.scalePercent()) / 100f;
        int tint = card.tint() < 0 ? 0xffffffff : 0xff000000 | card.tint();
        String clips = card.clips();
        String attack = GlbInspector.clip(clips, "attack");
        if (attack == null && card.kind != CardDef.BUILDING) {
            attack = card.range() > 1500 ? "1H_Ranged_Shoot" : "1H_Melee_Attack_Chop";
        }
        return new Look(model, size, attack, card.isFlying() ? 1.6f : 0f, tint,
                orElse(GlbInspector.clip(clips, "idle"), "Idle"), orElse(GlbInspector.clip(clips, "run"), "Running_A"),
                orElse(GlbInspector.clip(clips, "death"), "Death_A"));
    }

    private static String orElse(String v, String fallback) {
        return v == null ? fallback : v;
    }

    /// Whether `name` is one of the models that ship with the game.
    public static boolean isBundled(String name) {
        return CN.getResourceAsStream("/model_" + name + ".glb") != null;
    }

    /// The parsed model `name`, loaded once: `/model_<name>.glb` for a bundled
    /// one, or an uploaded file, `asset:<sha256>`, from [ForgeFiles].
    public static synchronized GltfScene scene(String name) {
        GltfScene s = SCENES.get(name);
        if (s == null) {
            InputStream in = name.startsWith("asset:") ? ForgeFiles.open(name.substring(6))
                    : CN.getResourceAsStream("/model_" + name + ".glb");
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
}
// end::skirmish-unit-looks[]
