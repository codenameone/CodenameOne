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
package com.codename1.gaming.level;

import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gaming.Scene;
import com.codename1.gaming.Sprite;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;

/// A turnkey `GameView` that plays a `GameLevel`: it wires the whole lifecycle so a
/// generated game scene can be a thin subclass that only adds behavior.
///
/// - **2D** levels are realized into the scene immediately.
/// - **board** levels are realized on the first sized frame, once an `IsoProjection`
///   can be fitted to the view.
/// - **3D** levels set up the perspective camera and the primary light, and build a
///   `Model` per element in `GameView#onSetup(com.codename1.gpu.GraphicsDevice)` (the
///   only place the GPU device exists); override `#buildModels(GraphicsDevice)` to
///   supply real meshes instead of the default per-element cube.
///
/// Game logic goes in `#onUpdate(double)` (a no-op by default) -- the scene's sprite
/// animations are advanced by the renderer, so do not call `Scene#update(double)`.
public class GameSceneView extends GameView {
    private final GameLevel level;
    private final AssetCatalog catalog;
    private final IsoProjection projection = new IsoProjection();
    private boolean boardRealized;
    private int score;
    private int lives = -1;

    public GameSceneView(GameLevel level, AssetCatalog catalog) {
        this.level = level;
        this.catalog = catalog;
        setClearColor(level.getInt("clearColor", 0xff101018));
        if (level.getMode() == GameLevel.MODE_2D) {
            level.realizeSprites(getScene(), catalog);
        }
    }

    public GameLevel getLevel() {
        return level;
    }

    public AssetCatalog getCatalog() {
        return catalog;
    }

    /// {@inheritDoc} Realizes a board level lazily once the view has a size, then
    /// defers to `#onUpdate(double)`.
    @Override
    protected void update(double deltaSeconds) {
        if (level.getMode() == GameLevel.MODE_BOARD && !boardRealized && getWidth() > 0 && getHeight() > 0) {
            int n = Math.max(level.getCols(), level.getRows());
            projection.fit(n, getWidth(), getHeight());
            level.realizeSprites(getScene(), catalog, projection);
            boardRealized = true;
        }
        onUpdate(deltaSeconds);
    }

    /// Override to advance game logic each frame. The default does nothing.
    protected void onUpdate(double deltaSeconds) {
    }

    /// The isometric projection used to place a board level (valid after the first
    /// sized frame).
    public IsoProjection getProjection() {
        return projection;
    }

    // ---- scene queries + game state -------------------------------------
    // So a generated scene (and your onUpdate) needs no hand-rolled boilerplate:
    // the editor wires named elements to fields and your logic uses these helpers.

    /// The `GameElement` a sprite was realized from -- its `Sprite#getUserData()` --
    /// or `null` if the sprite did not come from a level element. This is the bridge
    /// back to the typed `lives`/`value`/`speed` numbers you set in the editor.
    protected GameElement elementOf(Sprite sprite) {
        if (sprite == null) {
            return null;
        }
        Object data = sprite.getUserData();
        return data instanceof GameElement ? (GameElement) data : null;
    }

    /// The first sprite in the scene whose source element has the given `name` (the
    /// name typed in the Inspector), or `null`. The generated scene initializes a
    /// field per named element with this.
    protected Sprite findByName(String name) {
        if (name == null) {
            return null;
        }
        Scene scene = getScene();
        for (int i = 0; i < scene.size(); i++) {
            GameElement el = elementOf(scene.get(i));
            if (el != null && name.equals(el.getName())) {
                return scene.get(i);
            }
        }
        return null;
    }

    /// The first sprite stamped from the given asset id (e.g. `"player"`, `"coin"`),
    /// or `null`.
    protected Sprite findByAsset(String assetId) {
        if (assetId == null) {
            return null;
        }
        Scene scene = getScene();
        for (int i = 0; i < scene.size(); i++) {
            GameElement el = elementOf(scene.get(i));
            if (el != null && assetId.equals(el.getAssetId())) {
                return scene.get(i);
            }
        }
        return null;
    }

    /// Every sprite stamped from the given asset id, in z-order -- handy for a
    /// "collect all the coins" loop. Never `null` (empty when none match).
    protected java.util.List<Sprite> findAllByAsset(String assetId) {
        java.util.List<Sprite> matches = new java.util.ArrayList<Sprite>();
        if (assetId == null) {
            return matches;
        }
        Scene scene = getScene();
        for (int i = 0; i < scene.size(); i++) {
            GameElement el = elementOf(scene.get(i));
            if (el != null && assetId.equals(el.getAssetId())) {
                matches.add(scene.get(i));
            }
        }
        return matches;
    }

    /// Whether two sprites' bounding boxes overlap -- a null-safe shorthand for
    /// `Sprite#intersects(Sprite)`, the usual "did they touch?" collision test.
    protected boolean overlaps(Sprite a, Sprite b) {
        return a != null && b != null && a.intersects(b);
    }

    /// The running score (starts at zero). Add to it with `#addScore(int)`.
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    /// Adds `delta` to the score and returns the new total.
    public int addScore(int delta) {
        score += delta;
        return score;
    }

    /// Remaining lives, or `-1` until `#setLives(int)` is called. The generated scene
    /// seeds this from the player's `lives` property.
    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    /// Decrements the life count (never below zero) and returns what remains.
    public int loseLife() {
        if (lives > 0) {
            lives--;
        }
        return lives;
    }

    /// True once `#setLives(int)` has been given a count that has since reached zero --
    /// the standard "game over" test (false while lives are uninitialized).
    public boolean isGameOver() {
        return lives == 0;
    }

    /// {@inheritDoc} For a 3D level, configures the camera + light and builds the models.
    @Override
    protected void onSetup(GraphicsDevice device) {
        if (level.getMode() != GameLevel.MODE_3D) {
            return;
        }
        getCamera()
                .setPerspective(level.getFov(), level.getNear(), level.getFar())
                .setPosition(level.getEyeX(), level.getEyeY(), level.getEyeZ())
                .setTarget(level.getTargetX(), level.getTargetY(), level.getTargetZ());
        if (!level.lights().isEmpty()) {
            LevelLight l = level.lights().get(0);
            Light light = getLight();
            light.setDirection(l.getDirectionX(), l.getDirectionY(), l.getDirectionZ());
            light.setColor(l.getColor());
            light.setAmbientColor(l.getAmbientColor());
        }
        buildModels(device);
    }

    /// Builds the 3D models for the level. The default gives every element a unit cube
    /// (`com.codename1.gpu.Primitives#cube`) shaded with its asset's base color and
    /// placed/scaled from the element transform. Override to load real meshes (e.g. with
    /// `com.codename1.gpu.GltfLoader`) keyed off the element's asset id.
    protected void buildModels(GraphicsDevice device) {
        Mesh cube = Primitives.cube(device, 1f);
        for (int i = 0; i < level.elements().size(); i++) {
            GameElement el = level.elements().get(i);
            int color = 0xffcccccc;
            AssetDef def = catalog == null ? null : catalog.def(el.getAssetId());
            if (def != null) {
                color = def.getColor();
            }
            Material mat = new Material(Material.Type.LAMBERT).setColor(color);
            Model model = new Model(cube, mat);
            model.setPosition((float) el.getX(), (float) el.getY(), (float) el.getZ());
            model.setScale(el.getScaleX(), el.getScaleY(), el.getScaleZ());
            model.setRotation(0, el.getRotation(), 0);
            model.setUserData(el);
            addModel(model);
        }
    }
}
