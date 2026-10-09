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

/// `UnityEngine.ParticleSystemRenderer`: draws the particles of the
/// system beside it, each as one sprite that faces the camera.
///
/// The sprite is the main texture of the renderer's material, whole,
/// tinted by the material's colour; a particle's size is its width. The
/// other render modes -- stretched, mesh, the horizontal and vertical
/// billboards -- and the material's shader and blending are not read:
/// particles are drawn as sprites are, blended over what is behind them.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are what generated code calls
public final class ParticleSystemRenderer extends Renderer {
    private Sprite sprite;
    private int argb = 0xffffffff;
    private float least;
    private float most = 0.5f;
    private ParticleSystem system;

    /// What a scene file sets: the material's texture and colour, and the
    /// least and most a particle may be, as parts of the view's height.
    public void $setup(Sprite shown, int color, float minSize, float maxSize) {
        sprite = shown;
        argb = color;
        least = minSize;
        most = maxSize;
    }

    @Override
    public Component $new() {
        return new ParticleSystemRenderer();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        ParticleSystemRenderer r = (ParticleSystemRenderer) source;
        $setup(r.sprite, r.argb, r.least, r.most);
    }

    @Override
    public int $roles() {
        return DRAWS;
    }

    @Override
    public void $draw(DrawView view) {
        if (!enabled) {
            return;
        }
        if (system == null) {
            java.lang.Object s = gameObject.GetComponent(ParticleSystem.class);
            if (!(s instanceof ParticleSystem)) {
                return;
            }
            system = (ParticleSystem) s;
        }
        system.draw(view, sprite, argb, least, most, sortingOrder, sortingLayerID);
    }
}
