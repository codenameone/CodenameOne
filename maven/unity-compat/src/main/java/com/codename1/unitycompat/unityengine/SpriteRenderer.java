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

import UnityEngine.Color;

/// `UnityEngine.SpriteRenderer`: draws a [Sprite] at its object's
/// transform, tinted and optionally mirrored.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class SpriteRenderer extends Renderer {
    private final Color color = new Color();
    Sprite sprite;
    boolean flipX;
    boolean flipY;
    /// The tint as ARGB, worked out when it changes and not per frame.
    private int argb = 0xffffffff;

    public SpriteRenderer() {
        color.r = 1f;
        color.g = 1f;
        color.b = 1f;
        color.a = 1f;
    }

    /// What a scene file sets.
    public void $tint(float r, float g, float b, float a) {
        color.r = r;
        color.g = g;
        color.b = b;
        color.a = a;
        argb = Camera.argb(r, g, b, a);
    }

    @Override
    public Component $new() {
        return new SpriteRenderer();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        SpriteRenderer r = (SpriteRenderer) source;
        $tint(r.color.r, r.color.g, r.color.b, r.color.a);
        sprite = r.sprite;
        flipX = r.flipX;
        flipY = r.flipY;
    }

    public Color get_color(Color ret) {
        ret.$assign(color);
        return ret;
    }

    public void set_color(Color value) {
        $tint(value.r, value.g, value.b, value.a);
    }

    public Sprite get_sprite() {
        return sprite;
    }

    public void set_sprite(Sprite value) {
        sprite = value;
    }

    public boolean get_flipX() {
        return flipX;
    }

    public void set_flipX(boolean value) {
        flipX = value;
    }

    public boolean get_flipY() {
        return flipY;
    }

    public void set_flipY(boolean value) {
        flipY = value;
    }

    int argb() {
        return argb;
    }

    float red() {
        return color.r;
    }

    float green() {
        return color.g;
    }

    float blue() {
        return color.b;
    }

    float alpha() {
        return color.a;
    }
}
