/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.gpu;

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.layouts.BorderLayout;

/// A Codename One component that hosts a hardware accelerated 3D rendering
/// surface and drives an application supplied `Renderer`. It behaves like any
/// other component: add it to a `Form` or `Container`, give it a layout
/// constraint, and it participates in scrolling, transitions and z-ordering.
/// Internally it wraps a platform specific GPU peer (a `GLSurfaceView` on
/// Android, a WebGL canvas on the browser, an `MTKView` on iOS, a desktop GL
/// canvas in the simulator) using the same peer integration as
/// `BrowserComponent`.
///
/// When the running platform has no GPU backend, `isSupported()` returns false
/// and the view shows a placeholder instead of crashing. Always create the view
/// the same way; only the result of `isSupported()` differs per platform.
///
/// #### Example
///
/// ```java
/// RenderView view = new RenderView(new Renderer() {
///     Camera camera = new Camera();
///     Mesh cube;
///     Material material;
///
///     public void onInit(GraphicsDevice device) {
///         cube = Primitives.cube(device, 1f);
///         material = new Material(Material.Type.PHONG).setColor(0xff3366ff);
///     }
///
///     public void onResize(GraphicsDevice device, int w, int h) {
///         camera.setAspect((float) w / h);
///         device.setViewport(0, 0, w, h);
///     }
///
///     public void onFrame(GraphicsDevice device) {
///         device.clear(0xff101018, true, true);
///         device.setCamera(camera);
///         device.draw(cube, material, null);
///     }
///
///     public void onDispose(GraphicsDevice device) { }
/// });
/// view.setContinuous(true);
/// form.add(BorderLayout.CENTER, view);
/// ```
public class RenderView extends Container {
    private final Renderer renderer;
    private final Container placeholder;
    private PeerComponent internal;
    private boolean continuous;

    /// Creates a render view driven by the supplied renderer.
    ///
    /// #### Parameters
    ///
    /// - `renderer`: the callback that initializes and draws the scene
    public RenderView(Renderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer is required");
        }
        this.renderer = renderer;
        setLayout(new BorderLayout());
        placeholder = new Container();
        if (!Display.getInstance().isOpenGLSupported()) {
            placeholder.setLayout(new BorderLayout());
            placeholder.add(BorderLayout.CENTER, new Label("3D not supported"));
        }
        addComponent(BorderLayout.CENTER, placeholder);
    }

    /// Returns the renderer driving this view.
    public Renderer getRenderer() {
        return renderer;
    }

    /// Returns true if the current platform provides a 3D backend. Equivalent to
    /// `Display.getInstance().isOpenGLSupported()`.
    public boolean isSupported() {
        return Display.getInstance().isOpenGLSupported();
    }

    /// Returns true if the view continuously renders frames.
    public boolean isContinuous() {
        return continuous;
    }

    /// Controls whether the view renders continuously (an animation loop) or
    /// only when `requestRender()` is called (on demand). On demand is the
    /// default and conserves battery for static scenes.
    ///
    /// #### Parameters
    ///
    /// - `continuous`: true to render every frame
    ///
    /// #### Returns
    ///
    /// this view for chaining
    public RenderView setContinuous(boolean continuous) {
        this.continuous = continuous;
        if (internal != null) {
            Display.getInstance().glSetContinuous(internal, continuous);
        }
        return this;
    }

    /// Requests that a single frame be rendered. Has no effect when the view is
    /// in continuous mode or when 3D is unsupported.
    public void requestRender() {
        if (internal != null) {
            Display.getInstance().glRequestRender(internal);
        }
    }

    /// Returns the underlying native peer once created, or null before the view
    /// has been added to the UI or on unsupported platforms.
    public PeerComponent getPeer() {
        return internal;
    }

    protected void initComponent() {
        super.initComponent();
        if (internal == null && Display.getInstance().isOpenGLSupported()) {
            PeerComponent c = Display.getInstance().createGLPeer(this);
            if (c != null) {
                internal = c;
                removeComponent(placeholder);
                addComponent(BorderLayout.CENTER, internal);
                Display.getInstance().glSetContinuous(internal, continuous);
                Container parent = getParent();
                if (parent != null) {
                    parent.revalidate();
                } else {
                    revalidate();
                }
            }
        }
    }
}
