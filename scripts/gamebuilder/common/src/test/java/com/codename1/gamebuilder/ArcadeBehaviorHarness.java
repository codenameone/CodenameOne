/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gamebuilder;

import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.gaming.Sprite;
import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gaming.level.GameSceneView;
import com.codename1.gaming.level.Layer;
import com.codename1.ui.Display;

/// Drives the runtime `GameSceneView` opt-in arcade behavior headlessly (no editor) and
/// asserts the ported physics actually run in core: gravity + tile landing, coin pickup
/// (default `onPickup`), enemy patrol, the follow camera, and an `onPickup` override.
public final class ArcadeBehaviorHarness {
    private static int failures;

    /// A GameSceneView that turns arcade behavior on and exposes the protected step +
    /// queries so the harness can drive and inspect it.
    static class TestView extends GameSceneView {
        boolean consumeCoins = true;

        TestView(GameLevel level, AssetCatalog catalog) {
            super(level, catalog);
            setArcadeBehavior(true);
        }

        // fixed viewport so the follow-camera math is deterministic and the level
        // (832px wide) is wider than the view, so scrolling is observable
        @Override
        public int getWidth() {
            return 400;
        }

        @Override
        public int getHeight() {
            return 300;
        }

        void step(double dt) {
            updateArcade(dt);
        }

        Sprite player() {
            return findByAsset("player");
        }

        Sprite enemy() {
            return findByAsset("slime");
        }

        int coinCount() {
            return findAllByAsset("coin").size();
        }

        @Override
        protected boolean onPickup(Sprite item) {
            if (!consumeCoins) {
                addScore(1);
                return false;   // a "power-up" that is not removed
            }
            return super.onPickup(item);
        }
    }

    public static void main(String[] args) throws Exception {
        Display.init(null);
        GameLevel level = StarterPacks.newLevel(GameLevel.MODE_2D);
        AssetCatalog catalog = StarterPacks.loadCatalog();
        int ts = level.getTileSize();
        int cols = level.getCols();
        int rows = level.getRows();
        double floorTop = (rows - 2) * ts;

        // a solid floor across the bottom -- on a play-plane (parallax 1) tile layer,
        // since a parallax background layer is decoration and does not collide
        Layer terrain = solidLayer(level);
        for (int c = 0; c < cols; c++) {
            terrain.putTile(c, rows - 2, "grass");
        }

        level.addElement(new GameElement("pl", "player").setName("player").setLayer("Actors")
                .setPosition(3 * ts, (rows - 7) * ts).setProperty("lives", 3));
        level.addElement(new GameElement("co", "coin").setLayer("Actors")
                .setPosition(3 * ts, (rows - 4) * ts).setProperty("value", 25));
        level.addElement(new GameElement("sl", "slime").setName("slime").setLayer("Actors")
                .setPosition(10 * ts, (rows - 3) * ts).setProperty("speed", 60));

        TestView view = new TestView(level, catalog);
        double slimeStart = view.enemy().getX();

        // run ~2s of frames: the player falls, lands, sweeps up the coin on the way
        for (int f = 0; f < 120; f++) {
            view.step(1.0 / 60.0);
        }

        Sprite player = view.player();
        check(player != null, "player sprite realized");
        check(player.getY() > floorTop - ts && player.getY() <= floorTop,
                "player fell and landed on the floor (y=" + (int) player.getY() + ", floorTop=" + (int) floorTop + ")");
        check(view.getScore() >= 25, "coin scored its value (score=" + view.getScore() + ")");
        check(view.coinCount() == 0, "collected coin removed from the scene");
        check(view.getLives() == 3, "lives seeded from the player property");
        check(Math.abs(view.enemy().getX() - slimeStart) > 1, "slime patrolled (dx="
                + (int) (view.enemy().getX() - slimeStart) + ")");

        // follow camera tracks the player horizontally (view is 400px, level 832px).
        // remove the enemy first so moving the player does not trip onPlayerHit.
        view.getScene().remove(view.enemy());
        player.setX(40);
        view.step(0);
        int camLeft = view.getScene().getCameraX();
        player.setX(cols * ts / 2.0);
        view.step(0);
        int camMid = view.getScene().getCameraX();
        player.setX(cols * ts - 40);
        view.step(0);
        int camRight = view.getScene().getCameraX();
        check(camLeft == 0, "camera clamps at the left edge (camX=" + camLeft + ")");
        check(camMid > camLeft && camRight > camMid, "camera scrolls right with the player ("
                + camLeft + " -> " + camMid + " -> " + camRight + ")");
        check(camRight == cols * ts - 400, "camera clamps at the right edge (camX=" + camRight + ")");

        // override: a non-consuming pickup (power-up) stays in the scene
        GameLevel level2 = StarterPacks.newLevel(GameLevel.MODE_2D);
        for (int c = 0; c < level2.getCols(); c++) {
            solidLayer(level2).putTile(c, level2.getRows() - 2, "grass");
        }
        level2.addElement(new GameElement("pl", "player").setName("player").setLayer("Actors")
                .setPosition(3 * ts, (level2.getRows() - 4) * ts));
        level2.addElement(new GameElement("co", "coin").setLayer("Actors")
                .setPosition(3 * ts, (level2.getRows() - 3) * ts));
        TestView view2 = new TestView(level2, catalog);
        view2.consumeCoins = false;
        for (int f = 0; f < 60; f++) {
            view2.step(1.0 / 60.0);
        }
        check(view2.coinCount() == 1, "onPickup override kept the item (power-up not consumed)");
        check(view2.getScore() > 0, "onPickup override scored");

        // ---- parallax: a layer's factor flows onto its realized sprites + JSON ----
        GameLevel pl = StarterPacks.newLevel(GameLevel.MODE_2D);
        Layer bg = null;
        Layer fg = null;
        for (Layer l : pl.layers()) {
            if (l.getKind() == Layer.KIND_TILE) {
                if (bg == null) {
                    bg = l;
                } else if (fg == null) {
                    fg = l;
                }
            }
        }
        if (fg == null) {
            fg = bg;
        }
        bg.setParallax(0.3f, 0.5f);
        bg.putTile(2, 2, "grass");
        TestView pv = new TestView(pl, catalog);
        Sprite bgSprite = null;
        for (int i = 0; i < pv.getScene().size(); i++) {
            Sprite s = pv.getScene().get(i);
            if (s.getUserData() == bg) {
                bgSprite = s;
                break;
            }
        }
        check(bgSprite != null && Math.abs(bgSprite.getParallaxX() - 0.3f) < 1e-4f,
                "background layer parallax flows onto its sprites (px=" + (bgSprite == null ? "?" : bgSprite.getParallaxX()) + ")");
        check(bgSprite != null && Math.abs(bgSprite.getParallaxY() - 0.5f) < 1e-4f, "parallaxY flows onto sprites");

        GameLevel reloaded = GameLevel.load(pl.toJson());
        Layer rbg = null;
        for (Layer l : reloaded.layers()) {
            if (l.getName().equals(bg.getName())) {
                rbg = l;
                break;
            }
        }
        check(rbg != null && Math.abs(rbg.getParallaxX() - 0.3f) < 1e-4f
                && Math.abs(rbg.getParallaxY() - 0.5f) < 1e-4f, "layer parallax survives a JSON round-trip");

        System.out.println("[Arcade] failures=" + failures);
        System.out.println(failures == 0 ? "[Arcade] RESULT OK" : "[Arcade] RESULT FAIL");
        System.exit(failures == 0 ? 0 : 1);
    }

    /// The first play-plane (parallax 1) tile layer -- where a solid floor must go,
    /// since a parallax background layer is decoration and does not collide.
    private static Layer solidLayer(GameLevel level) {
        for (Layer l : level.layers()) {
            if (l.getKind() == Layer.KIND_TILE && l.getParallaxX() == 1f && l.getParallaxY() == 1f) {
                return l;
            }
        }
        return level.layers().get(0);
    }

    private static void check(boolean cond, String msg) {
        System.out.println((cond ? "  ok   " : "  FAIL ") + msg);
        if (!cond) {
            failures++;
        }
    }
}
