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
package com.codename1.impl.rive;

import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

import java.util.concurrent.atomic.AtomicInteger;

/// A component that shows a [RiveImage], fitted into its content area. This
/// is the only place a Rive animation renders on a GPU surface: when the
/// artboard has image meshes and the platform has a GPU backend
/// (`Display.isGpuSupported()`), the view holds a `com.codename1.gpu.RenderView`
/// laid out like any child. Otherwise, and wherever the image is drawn as an
/// icon, a background or in a `paint` method, it renders through `Graphics`.
///
/// The view hands the pointer events it receives to the image, in the image's
/// coordinates, and keeps drags on it from scrolling the parent.
///
/// The GPU surface is a native peer: a lightweight `Dialog` shown over it
/// appears under the surface and is not supported. Use an
/// `InteractionDialog` placed away from the view, or a view without meshes.
public class RiveView extends Container {
    private final RiveImage image;
    private final RenderView surface;
    private final AtomicInteger framesRendered = new AtomicInteger();

    /// A view of `image`.
    ///
    /// #### Parameters
    ///
    /// - `image`: the animation to show
    public RiveView(RiveImage image) {
        super(new BorderLayout());
        if (image == null) {
            throw new IllegalArgumentException("image is required");
        }
        this.image = image;
        if (image.usesGpu() && Display.getInstance().isGpuSupported()) {
            surface = new RenderView(new CountingRenderer(image.createRenderer(), framesRendered));
            addComponent(BorderLayout.CENTER, surface);
        } else {
            surface = null;
        }
    }

    /// The image this view shows.
    ///
    /// #### Returns
    ///
    /// the image
    public RiveImage getImage() {
        return image;
    }

    /// True while a live GPU surface shows the image.
    ///
    /// #### Returns
    ///
    /// true if the image is on a GPU surface
    public boolean isOnGpuSurface() {
        return surface != null && surface.getPeer() != null;
    }

    /// The frames the GPU surface has rendered, counted on the render thread.
    ///
    /// #### Returns
    ///
    /// the number of frames rendered so far, 0 without a surface
    public int getFramesRendered() {
        return framesRendered.get();
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        return new Dimension(image.getWidth() + s.getHorizontalPadding(),
                image.getHeight() + s.getVerticalPadding());
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (surface != null) {
            // the form hands pointer events to the deepest component under
            // them; marked this way they stay with this view
            ignorePointerEvents(surface);
        }
        Form f = getComponentForm();
        if (f != null) {
            f.registerAnimated(this);
        }
    }

    @Override
    protected void deinitialize() {
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimated(this);
        }
        super.deinitialize();
    }

    @Override
    public boolean animate() {
        boolean changed = image.animate();
        if (changed && isOnGpuSurface()) {
            renderSurface();
            return false;
        }
        return changed;
    }

    @Override
    public void paint(Graphics g) {
        if (isOnGpuSurface()) {
            renderSurface();
            super.paint(g);
            return;
        }
        Style s = getStyle();
        int w = getWidth() - s.getHorizontalPadding();
        int h = getHeight() - s.getVerticalPadding();
        if (w > 0 && h > 0) {
            image.drawFallback(g, getX() + s.getPaddingLeftNoRTL(), getY() + s.getPaddingTop(), w, h);
        }
        super.paint(g);
    }

    private void renderSurface() {
        image.prepareFrame(surface.getWidth(), surface.getHeight());
        surface.requestRender();
    }

    @Override
    public void pointerPressed(int x, int y) {
        forward(RiveImage.POINTER_PRESSED, x, y);
    }

    @Override
    public void pointerDragged(int x, int y) {
        // not passed on: a drag on the animation must not scroll the parent
        forward(RiveImage.POINTER_DRAGGED, x, y);
    }

    @Override
    public void pointerReleased(int x, int y) {
        forward(RiveImage.POINTER_RELEASED, x, y);
    }

    private void forward(int type, int x, int y) {
        Style s = getStyle();
        int left = getAbsoluteX() + s.getPaddingLeftNoRTL();
        int top = getAbsoluteY() + s.getPaddingTop();
        image.pointer(type, x - left, y - top,
                getWidth() - s.getHorizontalPadding(), getHeight() - s.getVerticalPadding());
        if (!isOnGpuSurface()) {
            repaint();
        }
    }

    private static void ignorePointerEvents(Component c) {
        c.setIgnorePointerEvents(true);
        if (c instanceof Container) {
            Container ct = (Container) c;
            for (int i = 0; i < ct.getComponentCount(); i++) {
                ignorePointerEvents(ct.getComponentAt(i));
            }
        }
    }

    /// Counts the frames the image's renderer draws.
    private static final class CountingRenderer implements Renderer {
        private final Renderer r;
        private final AtomicInteger frames;

        CountingRenderer(Renderer r, AtomicInteger frames) {
            this.r = r;
            this.frames = frames;
        }

        @Override
        public void onInit(GraphicsDevice device) {
            r.onInit(device);
        }

        @Override
        public void onResize(GraphicsDevice device, int width, int height) {
            r.onResize(device, width, height);
        }

        @Override
        public void onFrame(GraphicsDevice device) {
            r.onFrame(device);
            frames.incrementAndGet();
        }

        @Override
        public void onDispose(GraphicsDevice device) {
            r.onDispose(device);
        }
    }
}
