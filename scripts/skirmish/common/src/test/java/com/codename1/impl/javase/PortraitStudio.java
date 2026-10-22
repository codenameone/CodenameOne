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
package com.codename1.impl.javase;

import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.Model;
import com.codename1.gpu.Camera;
import com.codename1.gpu.Light;
import com.codename1.testing.junit.CodenameOneTest;
import com.codename1.testing.junit.RunOnEdt;
import com.codenameone.examples.skirmish.battle.Models;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/// Renders the character art the menus use -- card portraits and hero poses
/// -- from the same glTF models the battle draws, with the simulator's
/// software rasterizer. It is a tool, not a check: it runs only when asked,
///
/// ```
/// mvn -pl common test-compile surefire:test -Dtest=PortraitStudio \
///     -Dskirmish.portraits="$PWD/common/src/main/resources"
/// ```
///
/// and writes `card_<card>.png` and `hero_<model>.png` there. Re-run it after
/// changing a model or a look in [Models]. It lives in the JavaSE port's
/// package because the software device is package private.
@CodenameOneTest
@EnabledIfSystemProperty(named = "skirmish.portraits", matches = ".+")
class PortraitStudio {
    private static final String[][] CARDS = {
        {"squire", "1H_Melee_Attack_Chop", "0.35"},
        {"twin-archers", "1H_Ranged_Shoot", "0.5"},
        {"ogre", "2H_Melee_Attack_Chop", "0.35"},
        {"bat-swarm", "1H_Melee_Attack_Chop", "0.35"},
        {"sharpshooter", "2H_Ranged_Shoot", "0.5"},
        {"spearmen", "1H_Melee_Attack_Chop", "0.35"},
        {"drake", "Spellcast_Shoot", "0.45"},
        {"imp", "Unarmed_Melee_Attack_Punch_A", "0.3"},
        // A building has no animation: posed as loaded, framed whole.
        {"imp-den", null, "0"},
    };
    private static final String[] HEROES = {"knight", "barbarian", "mage", "rogue_hooded", "skeleton_warrior"};

    @Test
    @RunOnEdt
    void render() throws Exception {
        File dir = new File(System.getProperty("skirmish.portraits"));
        for (String[] c : CARDS) {
            Models.Look look = Models.of(c[0]);
            BufferedImage img = shoot(look.model, c[1], Float.parseFloat(c[2]), 256, 320, 25f, c[1] != null);
            ImageIO.write(img, "png", new File(dir, "card_" + c[0] + ".png"));
        }
        for (String h : HEROES) {
            BufferedImage img = shoot(h, "Cheer", 0.55f, 480, 600, 18f, false);
            ImageIO.write(img, "png", new File(dir, "hero_" + h + ".png"));
        }
    }

    /// One character at `seconds` into `clip`, filling a `w` x `h` frame.
    private static BufferedImage shoot(String model, String clip, float seconds, int w, int h,
                                       float yaw, boolean bust) {
        int ss = 2; // render at twice the size, then downsample for clean edges
        JavaSESoftwareDevice d = new JavaSESoftwareDevice();
        d.resize(w * ss, h * ss);
        AnimatedModel m = new AnimatedModel(d, Models.scene(model));
        if (clip != null) {
            m.play(clip, false);
            m.setClipTime(seconds * m.getScene().findAnimation(clip).getDuration());
        }
        m.pose();
        float[] b = bounds(m);
        float height = b[4] - b[1];
        float cy = bust ? b[1] + height * 0.55f : b[1] + height * 0.5f;
        float frame = bust ? height * 0.85f : height * 1.08f;
        float fov = 30f;
        float dist = frame / 2f / (float) Math.tan(Math.toRadians(fov / 2));
        double a = Math.toRadians(yaw);
        Camera cam = new Camera().setPerspective(fov, 0.1f, 100f).setAspect((float) w / h)
                .setPosition((float) Math.sin(a) * dist, cy + frame * 0.12f, (float) Math.cos(a) * dist)
                .setTarget(0f, cy, 0f).setUp(0f, 1f, 0f);
        d.setCamera(cam);
        d.setLight(new Light().setDirection(-0.5f, -0.7f, -0.6f).setColor(0xffffffff).setAmbientColor(0xff9098a8));
        d.setViewport(0, 0, w * ss, h * ss);
        d.clear(0x00000000, true, true);
        float[] identity = com.codename1.gpu.Matrix4.identity();
        for (Model p : m.getParts()) {
            if (p.isVisible()) {
                d.draw(p.getMesh(), p.getMaterial(), identity);
            }
        }
        return downsample(d.getImage(), ss);
    }

    private static float[] bounds(AnimatedModel m) {
        float[] b = {1e9f, 1e9f, 1e9f, -1e9f, -1e9f, -1e9f};
        for (Model p : m.getParts()) {
            if (!p.isVisible()) {
                continue;
            }
            float[] v = p.getMesh().getVertices().getData();
            for (int i = 0; i + 2 < v.length; i += 8) {
                for (int k = 0; k < 3; k++) {
                    b[k] = Math.min(b[k], v[i + k]);
                    b[k + 3] = Math.max(b[k + 3], v[i + k]);
                }
            }
        }
        return b;
    }

    private static BufferedImage downsample(BufferedImage src, int ss) {
        int w = src.getWidth() / ss;
        int h = src.getHeight() / ss;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                long a = 0, r = 0, g = 0, bl = 0;
                for (int j = 0; j < ss; j++) {
                    for (int i = 0; i < ss; i++) {
                        int p = src.getRGB(x * ss + i, y * ss + j);
                        int pa = p >>> 24;
                        a += pa;
                        r += ((p >> 16) & 0xff) * pa;
                        g += ((p >> 8) & 0xff) * pa;
                        bl += (p & 0xff) * pa;
                    }
                }
                int n = ss * ss;
                int oa = (int) (a / n);
                int div = (int) Math.max(1, a);
                out.setRGB(x, y, (oa << 24) | ((int) (r / div) << 16) | ((int) (g / div) << 8) | (int) (bl / div));
            }
        }
        return out;
    }
}
