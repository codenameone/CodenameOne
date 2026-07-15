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
package com.codename1.impl.html5;

public final class JavaScriptNativeImageAdapter {
    private JavaScriptNativeImageAdapter() {
    }

    public enum SurfaceKind {
        NONE,
        LOADED_IMAGE,
        MUTABLE_SURFACE
    }

    public interface ImageModel {
        int getExplicitWidth();
        int getExplicitHeight();
        boolean hasLoadedImage();
        int getLoadedImageWidth();
        int getLoadedImageHeight();
        boolean hasMutableSurface();
        int getMutableSurfaceWidth();
        int getMutableSurfaceHeight();
        Object getPatternCache();
        void setPatternCache(Object patternCache);
    }

    public interface DrawTarget {
        void drawLoadedImage(int x, int y, int width, int height);
        void drawMutableSurface(int x, int y, int width, int height);
    }

    public interface TileTarget {
        Object createLoadedImagePattern();
        Object createMutableSurfacePattern();
        void paintPattern(Object pattern, int x, int y, int width, int height);
    }

    public interface PixelReadTarget {
        void readLoadedImage();
        void readMutableSurface();
    }

    public static SurfaceKind resolveSurfaceKind(ImageModel image) {
        if (image.hasLoadedImage()) {
            return SurfaceKind.LOADED_IMAGE;
        }
        if (image.hasMutableSurface()) {
            return SurfaceKind.MUTABLE_SURFACE;
        }
        return SurfaceKind.NONE;
    }

    public static int resolveWidth(ImageModel image) {
        if (image.getExplicitWidth() > 0) {
            return image.getExplicitWidth();
        }
        if (image.hasLoadedImage()) {
            return image.getLoadedImageWidth();
        }
        if (image.hasMutableSurface()) {
            return image.getMutableSurfaceWidth();
        }
        return 10;
    }

    public static int resolveHeight(ImageModel image) {
        if (image.getExplicitHeight() > 0) {
            return image.getExplicitHeight();
        }
        if (image.hasLoadedImage()) {
            return image.getLoadedImageHeight();
        }
        if (image.hasMutableSurface()) {
            return image.getMutableSurfaceHeight();
        }
        return 10;
    }

    public static void invalidatePatternCache(ImageModel image) {
        image.setPatternCache(null);
    }

    public static void draw(ImageModel image, DrawTarget target, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        switch (resolveSurfaceKind(image)) {
            case LOADED_IMAGE:
                target.drawLoadedImage(x, y, width, height);
                break;
            case MUTABLE_SURFACE:
                target.drawMutableSurface(x, y, width, height);
                break;
            default:
                break;
        }
    }

    public static void tile(ImageModel image, TileTarget target, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Object pattern = image.getPatternCache();
        if (pattern != null) {
            target.paintPattern(pattern, x, y, width, height);
            return;
        }
        switch (resolveSurfaceKind(image)) {
            case LOADED_IMAGE:
                pattern = target.createLoadedImagePattern();
                break;
            case MUTABLE_SURFACE:
                pattern = target.createMutableSurfacePattern();
                break;
            default:
                pattern = null;
                break;
        }
        if (pattern != null) {
            image.setPatternCache(pattern);
            target.paintPattern(pattern, x, y, width, height);
        }
    }

    public static void readPixels(ImageModel image, PixelReadTarget target) {
        switch (resolveSurfaceKind(image)) {
            case LOADED_IMAGE:
                target.readLoadedImage();
                break;
            case MUTABLE_SURFACE:
                target.readMutableSurface();
                break;
            default:
                break;
        }
    }
}
