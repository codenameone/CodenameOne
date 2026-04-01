/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

public final class JavaScriptNativeImageAdapter {
    private JavaScriptNativeImageAdapter() {
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
    }

    public interface ImageTarget {
        void drawLoadedImage(int x, int y, int width, int height);
        void drawMutableSurface(int x, int y, int width, int height);
        void tileLoadedImage(int x, int y, int width, int height);
        void tileMutableSurface(int x, int y, int width, int height);
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

    public static void draw(ImageModel image, ImageTarget target, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (image.hasLoadedImage()) {
            target.drawLoadedImage(x, y, width, height);
        } else if (image.hasMutableSurface()) {
            target.drawMutableSurface(x, y, width, height);
        }
    }

    public static void tile(ImageModel image, ImageTarget target, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (image.hasLoadedImage()) {
            target.tileLoadedImage(x, y, width, height);
        } else if (image.hasMutableSurface()) {
            target.tileMutableSurface(x, y, width, height);
        }
    }
}
