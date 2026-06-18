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
import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.ui.Display;

import java.util.HashMap;
import java.util.Map;

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

    // ---- opt-in 2D arcade behavior (off by default) ----
    private boolean arcadeBehavior;
    private boolean followCamera = true;
    private Sprite arcadePlayer;
    private double playerStartX;
    private double playerStartY;
    private boolean playerStartCaptured;
    private double pvy;
    private boolean grounded;
    private boolean jumpHeld;
    private final Map<String, Double> enemyDir = new HashMap<String, Double>();

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
        if (arcadeBehavior && level.getMode() == GameLevel.MODE_2D) {
            updateArcade(deltaSeconds);
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

    // ---- opt-in 2D arcade behavior ---------------------------------------
    // The editor's Live preview play-tests a level with built-in arcade behavior
    // (gravity, run/jump, tile collision, enemy patrol, pickups). That same
    // behavior is OFF by default at runtime so board/3D/your-own games are not
    // affected -- turn it on with setArcadeBehavior(true) and "what you preview is
    // what ships". Each piece is a protected hook you can override (or ignore the
    // lot and drive everything from onUpdate).

    /// Enables (or disables) the built-in 2D arcade behavior -- gravity, run/jump,
    /// tile collision, enemy patrol and pickups -- that the editor preview shows.
    /// Off by default. Has no effect on board or 3D levels.
    public GameSceneView setArcadeBehavior(boolean on) {
        this.arcadeBehavior = on;
        return this;
    }

    public boolean isArcadeBehavior() {
        return arcadeBehavior;
    }

    /// Whether the arcade behavior scrolls the scene camera to follow the player
    /// (on by default) -- this is what makes a parallax background scroll.
    public GameSceneView setFollowCamera(boolean on) {
        this.followCamera = on;
        return this;
    }

    /// Runs one arcade frame: player physics, enemy patrol, pickups, and the
    /// follow camera. Called automatically before `#onUpdate(double)` when
    /// `#setArcadeBehavior(boolean)` is on and the level is 2D.
    protected void updateArcade(double deltaSeconds) {
        Sprite player = arcadePlayer();
        if (player != null) {
            updatePlayer(player, deltaSeconds);
            checkPickups(player);
        }
        updateEnemies(deltaSeconds);
        if (player != null && followCamera) {
            int ts = Math.max(4, level.getTileSize());
            int camX = (int) Math.round(player.getX() - getWidth() / 2.0);
            int maxX = level.getCols() * ts - getWidth();
            if (camX < 0) {
                camX = 0;
            }
            if (maxX > 0 && camX > maxX) {
                camX = maxX;
            }
            getScene().setCamera(camX, 0);
        }
    }

    /// The player sprite the arcade behavior drives: an element flagged
    /// `player = true` wins, else the first `player`/`hero` asset. Cached; also
    /// seeds `#setLives(int)` from the player's `lives` property on first resolve.
    protected Sprite arcadePlayer() {
        if (arcadePlayer != null) {
            return arcadePlayer;
        }
        Scene scene = getScene();
        Sprite found = null;
        for (int i = 0; i < scene.size() && found == null; i++) {
            GameElement el = elementOf(scene.get(i));
            if (el != null && el.getBoolean("player", false)) {
                found = scene.get(i);
            }
        }
        if (found == null) {
            found = findByAsset("player");
        }
        if (found == null) {
            found = findByAsset("hero");
        }
        if (found != null) {
            arcadePlayer = found;
            if (!playerStartCaptured) {
                playerStartX = found.getX();
                playerStartY = found.getY();
                playerStartCaptured = true;
            }
            if (lives < 0) {
                GameElement el = elementOf(found);
                setLives(el == null ? 3 : el.getInt("lives", 3));
            }
        }
        return arcadePlayer;
    }

    /// Default player physics: run with Left/Right, jump on Up/Fire (edge-triggered),
    /// gravity from the level's `gravity` property, jump height from the player's
    /// `jumpHeight` property, stopped by solid tiles, respawning if it falls out.
    /// Override to change movement, or `#setArcadeBehavior(boolean)` off to opt out.
    protected void updatePlayer(Sprite player, double deltaSeconds) {
        int ts = Math.max(4, level.getTileSize());
        GameElement el = elementOf(player);
        AssetDef def = catalog == null || el == null ? null : catalog.def(el.getAssetId());
        double hw = (def == null ? ts : def.getWidth()) / 2.0;
        double hh = (def == null ? ts : def.getHeight()) / 2.0;
        double grav = level.getDouble("gravity", 9.8) * 132;
        double speed = level.getDouble("walkSpeed", 170);
        double jump = (el == null ? 110 : el.getInt("jumpHeight", 110)) * 4;

        boolean left = getInput().isGameKeyDown(Display.GAME_LEFT);
        boolean right = getInput().isGameKeyDown(Display.GAME_RIGHT);
        boolean up = getInput().isGameKeyDown(Display.GAME_UP) || getInput().isGameKeyDown(Display.GAME_FIRE);
        double pvx = ((right ? 1 : 0) - (left ? 1 : 0)) * speed;
        if (up && !jumpHeld && grounded) {
            pvy = -jump;
            grounded = false;
        }
        jumpHeld = up;
        pvy += grav * deltaSeconds;
        if (pvy > 1000) {
            pvy = 1000;
        }

        double nx = player.getX() + pvx * deltaSeconds;
        if (!isSolidAt(nx, player.getY(), hw, hh)) {
            player.setX(nx);
        }
        double ny = player.getY() + pvy * deltaSeconds;
        if (!isSolidAt(player.getX(), ny, hw, hh)) {
            player.setY(ny);
            grounded = false;
        } else {
            if (pvy > 0) {
                grounded = true;
            }
            pvy = 0;
        }
        double maxX = level.getCols() * ts - hw;
        if (player.getX() < hw) {
            player.setX(hw);
        }
        if (player.getX() > maxX) {
            player.setX(maxX);
        }
        if (player.getY() > level.getRows() * ts + hh * 3) {
            player.setPosition(playerStartX, playerStartY);
            pvy = 0;
        }
    }

    /// Default enemy behavior: every `#isEnemy(String)` sprite patrols horizontally
    /// at its `speed` property, turning at walls and level edges.
    protected void updateEnemies(double deltaSeconds) {
        int ts = Math.max(4, level.getTileSize());
        Scene scene = getScene();
        for (int i = 0; i < scene.size(); i++) {
            Sprite s = scene.get(i);
            GameElement el = elementOf(s);
            if (el == null || !isEnemy(el.getAssetId())) {
                continue;
            }
            Double d = enemyDir.get(el.getId());
            double dir = d == null ? 1.0 : d.doubleValue();
            AssetDef def = catalog == null ? null : catalog.def(el.getAssetId());
            double hw = (def == null ? ts : def.getWidth()) / 2.0;
            double hh = (def == null ? ts : def.getHeight()) / 2.0;
            double spd = el.getDouble("speed", 60);
            double nx = s.getX() + dir * spd * deltaSeconds;
            if (isSolidAt(nx, s.getY(), hw, hh) || nx < hw || nx > level.getCols() * ts - hw) {
                dir = -dir;
            } else {
                s.setX(nx);
            }
            enemyDir.put(el.getId(), Double.valueOf(dir));
        }
    }

    /// Default pickups: when the player overlaps a `#isCollectible(String)` sprite,
    /// `#onPickup(Sprite)` fires (scores and consumes it); when it overlaps an
    /// `#isEnemy(String)` sprite, `#onPlayerHit(Sprite)` fires (costs a life).
    protected void checkPickups(Sprite player) {
        int ts = Math.max(4, level.getTileSize());
        GameElement pel = elementOf(player);
        AssetDef pd = catalog == null || pel == null ? null : catalog.def(pel.getAssetId());
        double phw = (pd == null ? ts : pd.getWidth()) / 2.0;
        double phh = (pd == null ? ts : pd.getHeight()) / 2.0;
        Scene scene = getScene();
        for (int i = scene.size() - 1; i >= 0; i--) {
            Sprite s = scene.get(i);
            if (s == player) {   //NOPMD CompareObjectsWithEquals - sprite identity is intended
                continue;
            }
            GameElement el = elementOf(s);
            if (el == null) {
                continue;
            }
            String id = el.getAssetId();
            boolean coll = isCollectible(id);
            if (!coll && !isEnemy(id)) {
                continue;
            }
            AssetDef def = catalog == null ? null : catalog.def(id);
            double hw = (def == null ? ts : def.getWidth()) / 2.0;
            double hh = (def == null ? ts : def.getHeight()) / 2.0;
            boolean hit = Math.abs(player.getX() - s.getX()) < phw + hw
                    && Math.abs(player.getY() - s.getY()) < phh + hh;
            if (!hit) {
                continue;
            }
            if (coll) {
                if (onPickup(s)) {
                    scene.remove(s);
                }
            } else {
                onPlayerHit(s);
            }
        }
    }

    /// Called when the player touches a collectible. The default adds the item's
    /// `value` property to the score and returns `true` to consume it. Override for
    /// power-ups: read the asset id / properties, change state, and return whether
    /// to remove the item.
    protected boolean onPickup(Sprite item) {
        GameElement el = elementOf(item);
        addScore(el == null ? 0 : el.getInt("value", 10));
        return true;
    }

    /// Called when the player touches an enemy. The default costs a life and
    /// respawns the player at its start. Override for HP, knockback, invulnerability
    /// frames, checkpoints or a death screen.
    protected void onPlayerHit(Sprite enemy) {
        loseLife();
        if (arcadePlayer != null) {
            arcadePlayer.setPosition(playerStartX, playerStartY);
            pvy = 0;
        }
    }

    /// Whether an asset id is a collectible (the default arcade behavior scores and
    /// removes it). Defaults to `coin`/`gem`/`star`/`token`; override to add yours.
    protected boolean isCollectible(String assetId) {
        return assetId != null && (assetId.equals("coin") || assetId.equals("gem")
                || assetId.equals("star") || assetId.equals("token") || assetId.equals("coffee"));
    }

    /// Whether an asset id is an enemy (the default arcade behavior patrols it and
    /// costs a life on contact). Defaults to `slime`/`enemy*`/`npc*`; override yours.
    protected boolean isEnemy(String assetId) {
        return assetId != null && (assetId.equals("slime") || assetId.equals("enemy")
                || assetId.startsWith("enemy") || assetId.startsWith("npc")
                || assetId.equals("exception") || assetId.equals("bug"));
    }

    /// Whether a box centered at `(cx, cy)` with half-size `(hw, hh)` overlaps any
    /// solid tile (a non-empty cell of a visible tile layer). The collision query
    /// the default player/enemy physics uses; override for one-way platforms etc.
    protected boolean isSolidAt(double cx, double cy, double hw, double hh) {
        int ts = Math.max(4, level.getTileSize());
        int c0 = (int) Math.floor((cx - hw + 1) / ts);
        int c1 = (int) Math.floor((cx + hw - 1) / ts);
        int r0 = (int) Math.floor((cy - hh + 1) / ts);
        int r1 = (int) Math.floor((cy + hh - 1) / ts);
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                for (Layer l : level.layers()) {
                    // only tile layers in the play plane collide; a parallax background
                    // (clouds, mountains) is decoration, not a wall.
                    if (l.getKind() == Layer.KIND_TILE && l.isVisible()
                            && l.getParallaxX() == 1f && l.getParallaxY() == 1f && l.getTile(c, r) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        Map<String, Mesh> meshCache = new HashMap<String, Mesh>();
        for (int i = 0; i < level.elements().size(); i++) {
            GameElement el = level.elements().get(i);
            int color = 0xffcccccc;
            AssetDef def = catalog == null ? null : catalog.def(el.getAssetId());
            if (def != null) {
                color = def.getColor();
            }
            Mesh mesh = meshFor(device, el.getAssetId(), cube, meshCache);
            Material mat = new Material(Material.Type.LAMBERT).setColor(color);
            Model model = new Model(mesh, mat);
            model.setPosition((float) el.getX(), (float) el.getY(), (float) el.getZ());
            model.setScale(el.getScaleX(), el.getScaleY(), el.getScaleZ());
            model.setRotation(0, el.getRotation(), 0);
            model.setUserData(el);
            addModel(model);
        }
    }

    /// The mesh for an asset: a real glTF/glb mesh (`AssetDef#TYPE_MESH`) loaded from the
    /// catalog and cached per asset id, falling back to the shared unit cube when the
    /// asset has no mesh or it fails to load.
    private Mesh meshFor(GraphicsDevice device, String assetId, Mesh cube, Map<String, Mesh> cache) {
        byte[] glb = catalog == null ? null : catalog.meshData(assetId);
        if (glb == null) {
            return cube;
        }
        Mesh m = cache.get(assetId);
        if (m == null) {
            try {
                m = GltfLoader.load(device, glb);
            } catch (Exception ex) { //NOPMD - a bad mesh falls back to the cube
                m = cube;
            }
            cache.put(assetId, m);
        }
        return m;
    }
}
