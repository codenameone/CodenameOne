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
package com.codenameone.playground;

import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Renderer;

import java.util.function.Consumer;

/// A {@link com.codename1.gpu.Renderer} from two lambdas, for driving a GPU
/// `RenderView` without declaring a `Renderer` class (which Playground code can
/// also do directly):
///
/// ```java
/// RenderView view = new RenderView(GpuScripting.renderer(
///     device -> { /* onInit: build meshes/materials */ },
///     device -> { /* onFrame: clear, set camera, draw */ }));
/// ```
///
/// `onResize` defaults to setting the full viewport; `onDispose` is a no-op.
public final class GpuScripting {
    private GpuScripting() {
    }

    /// Builds a {@link Renderer} that forwards `onInit` / `onFrame` to the given
    /// lambdas (each receives the {@link GraphicsDevice}). Either may be null.
    public static Renderer renderer(final Consumer<GraphicsDevice> onInit, final Consumer<GraphicsDevice> onFrame) {
        return new Renderer() {
            @Override
            public void onInit(GraphicsDevice device) {
                if (onInit != null) {
                    onInit.accept(device);
                }
            }

            @Override
            public void onResize(GraphicsDevice device, int width, int height) {
                device.setViewport(0, 0, width, height);
            }

            @Override
            public void onFrame(GraphicsDevice device) {
                if (onFrame != null) {
                    onFrame.accept(device);
                }
            }

            @Override
            public void onDispose(GraphicsDevice device) {
            }
        };
    }
}
