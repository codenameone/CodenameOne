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

import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.GameCamera;
import com.codename1.gaming.GameView;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.geom.Dimension;

/// A character turning slowly on the spot, playing one of its clips: how the
/// card editor shows a model before it goes into a battle.
public final class ModelPreview extends GameView {
    private final String model;
    private final int tint;
    private String clip;
    private AnimatedModel shown;
    private float yaw;

    /// `model` is a bundled name or `asset:<sha256>` of a downloaded file.
    public ModelPreview(String model, int tint, String clip) {
        this.model = model;
        this.tint = tint;
        this.clip = clip;
        setClearColor(0xff2b3440);
    }

    /// Plays `name` from now on.
    public void play(String name) {
        clip = name;
        if (shown != null && name != null) {
            shown.play(name, true);
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        int s = Math.min(CN.convertToPixels(55), CN.getDisplayWidth() - CN.convertToPixels(8));
        return new Dimension(s, s * 3 / 4);
    }

    @Override
    protected void onSetup(GraphicsDevice d) {
        getLight().setDirection(-0.35f, -1f, -0.6f).setColor(0xfffff4e0).setAmbientColor(0xff8a8f9a);
        GameCamera cam = getCamera();
        cam.setPerspective(35f, 1f, 50f);
        cam.setPosition(0f, 1.6f, 5.2f);
        cam.setTarget(0f, 0.9f, 0f);
        try {
            shown = new AnimatedModel(d, Models.scene(model));
        } catch (RuntimeException e) {
            Log.e(e);
            return;
        }
        float[] b = Actor.bounds(shown);
        float h = Math.max(0.01f, b[4] - b[1]);
        float s = 2f / h;
        shown.setScale(s);
        shown.setPosition(0f, -b[1] * s, 0f);
        if (tint != 0xffffffff) {
            shown.setTint(tint);
        }
        if (clip != null) {
            shown.play(clip, true);
        }
        shown.addTo(this);
    }

    @Override
    protected void update(double dt) {
        if (shown == null) {
            return;
        }
        yaw += (float) dt * 30f;
        shown.setRotation(0f, yaw, 0f);
        shown.update((float) dt);
    }
}
