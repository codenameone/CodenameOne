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
package com.codename1.gaming;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.Texture;
import com.codename1.ui.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Draws a `Scene` of `Sprite`s on the GPU, implementing the `com.codename1.gpu`
/// `com.codename1.gpu.Renderer` contract so it can be hosted in a
/// `com.codename1.gpu.RenderView` (which `GameView` does for you).
///
/// Each frame it sets up an orthographic camera that maps one world unit to one
/// pixel with the origin at the top left and y pointing down, then draws every
/// visible sprite as a textured quad: a shared unit quad mesh scaled/rotated/
/// translated by a per sprite model matrix, with a `com.codename1.gpu.Material.Type#SPRITE`
/// material (alpha blended, depth test off) whose texture is the sprite's image and
/// whose color is the sprite's tint. Images are uploaded to
/// `com.codename1.gpu.Texture`s lazily and cached.
///
/// Use it directly for a custom host, or let `GameView` create and drive one:
///
/// ```java
/// SpriteRenderer r = new SpriteRenderer();
/// r.getScene().add(mySprite);
/// RenderView view = new RenderView(r).setContinuous(true);
/// form.add(BorderLayout.CENTER, view);
/// ```
public class SpriteRenderer implements Renderer {
    /// Callbacks invoked by the renderer on the render thread (used by `GameView` to
    /// run game logic and to let the game allocate GPU resources once the device is
    /// ready).
    interface Updatable {
        /// Invoked once after the GPU device is created, before the first frame.
        void setup(GraphicsDevice device);

        /// Invoked every frame before the scene is drawn.
        void frame(double deltaSeconds);
    }

    private final Scene scene;
    private Updatable updatable;
    private int clearColor = 0xff000000;

    private final GameCamera gameCamera = new GameCamera();
    private final Camera camera = new Camera();
    private final Light light = new Light();
    private final List models = new ArrayList();
    private Mesh quad;
    private Material material;
    private RenderState spriteState2D;
    private RenderState spriteState3D;
    private Map textures;
    private int viewWidth;
    private int viewHeight;
    private long lastTime;
    private boolean hasLast;

    // reusable model-matrix scratch buffers (onFrame is single threaded)
    private final float[] scratchA = new float[16];
    private final float[] scratchB = new float[16];
    private final float[] model = new float[16];

    /// Creates a renderer with a fresh empty `Scene`.
    public SpriteRenderer() {
        this(new Scene());
    }

    /// Creates a renderer drawing the given scene.
    public SpriteRenderer(Scene scene) {
        this.scene = scene;
    }

    /// The scene this renderer draws; add and remove sprites here.
    public Scene getScene() {
        return scene;
    }

    /// The camera this renderer draws through. Leave it in its default 2D mode for a
    /// classic sprite game, or put it in `GameCamera#MODE_PERSPECTIVE` to render the
    /// sprites as billboards in a 3D world.
    public GameCamera getCamera() {
        return gameCamera;
    }

    /// The directional light used to shade lit 3D `Model`s. Configure it with
    /// `com.codename1.gpu.Light#setDirection(float, float, float)` etc.
    public Light getLight() {
        return light;
    }

    /// Adds a 3D model to be drawn (in perspective mode) alongside the sprites.
    public void addModel(Model model) {
        models.add(model);
    }

    /// Removes a previously added 3D model.
    public void removeModel(Model model) {
        models.remove(model);
    }

    /// The number of 3D models in this renderer.
    public int getModelCount() {
        return models.size();
    }

    /// The ARGB color the framebuffer is cleared to each frame.
    public void setClearColor(int argb) {
        this.clearColor = argb;
    }

    public int getClearColor() {
        return clearColor;
    }

    void setUpdatable(Updatable updatable) {
        this.updatable = updatable;
    }

    @Override
    public void onInit(GraphicsDevice device) {
        quad = Primitives.quad(device, 1f);
        // 2D: ignore depth entirely (draw order wins). 3D: still alpha blended with
        // no depth write, but depth-test against opaque 3D models so closer geometry
        // occludes the billboards.
        spriteState2D = RenderState.transparent().setDepthTest(false);
        spriteState3D = RenderState.transparent().setDepthTest(true);
        material = new Material(Material.Type.SPRITE).setRenderState(spriteState2D);
        textures = new HashMap();
        hasLast = false;
        if (updatable != null) {
            updatable.setup(device);
        }
    }

    @Override
    public void onResize(GraphicsDevice device, int width, int height) {
        viewWidth = width;
        viewHeight = height;
        device.setViewport(0, 0, width, height);
    }

    @Override
    public void onFrame(GraphicsDevice device) {
        long now = System.currentTimeMillis();
        double dt = hasLast ? (now - lastTime) / 1000.0 : 0;
        lastTime = now;
        hasLast = true;
        if (dt > 0.25) {
            dt = 0.25;
        }

        if (updatable != null) {
            updatable.frame(dt);
        }
        scene.update(dt);

        device.clear(clearColor, true, true);
        // configure the GPU camera from the GameCamera each frame so a moving 3D
        // camera (or a switch between 2D and perspective) takes effect immediately.
        gameCamera.apply(camera, viewWidth, viewHeight);
        device.setCamera(camera);
        device.setLight(light);
        boolean perspective = gameCamera.getMode() == GameCamera.MODE_PERSPECTIVE;

        // opaque 3D models first (they write depth), then alpha-blended sprites.
        int modelCount = models.size();
        for (int i = 0; i < modelCount; i++) {
            Model m = (Model) models.get(i);
            if (m.isVisible() && m.getMesh() != null) {
                device.draw(m.getMesh(), m.getMaterial(), m.modelMatrix());
            }
        }

        material.setRenderState(perspective ? spriteState3D : spriteState2D);
        scene.ensureSorted();
        int count = scene.size();
        for (int i = 0; i < count; i++) {
            Sprite s = scene.get(i);
            if (!s.isVisible() || s.getImage() == null) {
                continue;
            }
            Texture t = texture(device, s.getImage());
            material.setTexture(t).setColor(s.getColor());
            device.draw(quad, material, modelMatrix(s));
        }
    }

    @Override
    public void onDispose(GraphicsDevice device) {
        if (textures != null) {
            java.util.Iterator it = textures.values().iterator();
            while (it.hasNext()) {
                device.dispose((Texture) it.next());
            }
            textures.clear();
        }
        if (quad != null) {
            device.dispose(quad.getVertices());
            device.dispose(quad.getIndices());
            quad = null;
        }
    }

    private Texture texture(GraphicsDevice device, Image image) {
        Texture t = (Texture) textures.get(image);
        if (t == null) {
            t = device.createTexture(image);
            textures.put(image, t);
        }
        return t;
    }

    /// Builds the sprite's model matrix, dispatching on the camera mode.
    private float[] modelMatrix(Sprite s) {
        if (gameCamera.getMode() == GameCamera.MODE_PERSPECTIVE) {
            return billboardMatrix(s);
        }
        return orthoMatrix(s);
    }

    /// 2D path: translate the anchor to the origin, scale to the pixel size, rotate,
    /// then translate to the sprite's world position (pixel coordinates converted to
    /// the camera's centered, y-up space).
    private float[] orthoMatrix(Sprite s) {
        float w = s.getRenderWidth() * s.getScaleX();
        float h = s.getRenderHeight() * s.getScaleY();
        float worldX = (float) (s.getX() - scene.getCameraX()) - viewWidth / 2f;
        float worldY = viewHeight / 2f - (float) (s.getY() - scene.getCameraY());

        // anchor offset in unit-quad space so the anchor lands at the position
        float[] anchor = Matrix4.translation(0.5f - (float) s.getAnchorX(),
                (float) s.getAnchorY() - 0.5f, 0f);
        float[] scale = Matrix4.scaling(w, h, 1f);
        // screen rotation is clockwise; negate for the y-up world
        float[] rot = Matrix4.rotation((float) -Math.toRadians(s.getRotation()), 0f, 0f, 1f);
        float[] trans = Matrix4.translation(worldX, worldY, 0f);

        Matrix4.multiply(scale, anchor, scratchA);   // S * Ta
        Matrix4.multiply(rot, scratchA, scratchB);    // R * S * Ta
        Matrix4.multiply(trans, scratchB, model);     // T * R * S * Ta
        return model;
    }

    /// 3D path: orient a quad in world space so it always faces the camera (a
    /// billboard). The sprite's `Sprite#getX()`/`getY()`/`getZ()` is the world
    /// position and its size/scale are world units. The quad is anchored, scaled,
    /// rolled around the view axis, oriented by the camera billboard basis and then
    /// translated to the world position: `T * B * R * S * Ta`.
    private float[] billboardMatrix(Sprite s) {
        float w = s.getRenderWidth() * s.getScaleX();
        float h = s.getRenderHeight() * s.getScaleY();

        // y-up anchor offset (image y is top-down, world y is up)
        float[] anchor = Matrix4.translation(0.5f - (float) s.getAnchorX(),
                0.5f - (float) s.getAnchorY(), 0f);
        float[] scale = Matrix4.scaling(w, h, 1f);
        float[] roll = Matrix4.rotation((float) -Math.toRadians(s.getRotation()), 0f, 0f, 1f);
        float[] basis = gameCamera.getBillboardBasis();
        float[] trans = Matrix4.translation((float) s.getX(), (float) s.getY(), (float) s.getZ());

        Matrix4.multiply(scale, anchor, scratchA);    // S * Ta
        Matrix4.multiply(roll, scratchA, scratchB);    // R * S * Ta
        Matrix4.multiply(basis, scratchB, scratchA);   // B * R * S * Ta
        Matrix4.multiply(trans, scratchA, model);      // T * B * R * S * Ta
        return model;
    }
}
