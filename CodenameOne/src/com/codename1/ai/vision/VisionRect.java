/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Immutable normalized rectangle using a top-left origin. */
public final class VisionRect {
    public static final VisionRect EMPTY = new VisionRect(0, 0, 0, 0);

    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public VisionRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
