/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase;

import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/// Real OpenGL surface for the JavaSE simulator, backed by JOGL. It hosts a
/// `GLJPanel` -- a lightweight Swing component that renders to an offscreen
/// framebuffer and blits into the Swing paint tree -- so it layers, clips and
/// snapshots exactly like every other Codename One peer while still running on
/// the GPU.
///
/// Constructing this class touches JOGL classes and initializes a GL context, so
/// it can throw `GLException` (no usable GL) or, when the JOGL jars are absent,
/// `NoClassDefFoundError`. The JavaSE port instantiates it inside a `try/catch`
/// and falls back to the software renderer, so neither failure breaks the
/// simulator.
class JavaSEJoglSurface implements JavaSEGpuSurface {
    private final GLJPanel panel;
    private final JavaSEGLDevice device = new JavaSEGLDevice();
    private final Renderer renderer;
    private boolean initialized;
    private int lastW = -1;
    private int lastH = -1;
    private Timer timer;

    JavaSEJoglSurface(RenderView view) {
        this.renderer = view.getRenderer();
        // Use the legacy GL2 (OpenGL 2.1) profile: on macOS the GL2ES2 profile
        // resolves to a core context that rejects GLSL 1.20, whereas GL2 gives a
        // 2.1 compatibility context whose GLSL 1.20 accepts the shared GLSL ES
        // shaders (after JavaSEGLDevice prepends the #version directive). GL2 is
        // available on every desktop GL stack; if it is not, construction throws
        // and the port falls back to the software renderer.
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDepthBits(24);
        caps.setAlphaBits(8);
        panel = new GLJPanel(caps);
        panel.addGLEventListener(new GLEventListener() {
            public void init(GLAutoDrawable d) {
                device.setGL(d.getGL().getGL2ES2());
                try {
                    renderer.onInit(device);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                initialized = true;
                lastW = -1;
            }

            public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
                device.setGL(d.getGL().getGL2ES2());
                if (!initialized) {
                    return;
                }
                lastW = w;
                lastH = h;
                try {
                    renderer.onResize(device, w, h);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            public void display(GLAutoDrawable d) {
                device.setGL(d.getGL().getGL2ES2());
                int w = d.getSurfaceWidth();
                int h = d.getSurfaceHeight();
                if ((w != lastW || h != lastH) && w > 0 && h > 0) {
                    lastW = w;
                    lastH = h;
                    try {
                        renderer.onResize(device, w, h);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                try {
                    renderer.onFrame(device);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            public void dispose(GLAutoDrawable d) {
                device.setGL(d.getGL().getGL2ES2());
                try {
                    renderer.onDispose(device);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                device.disposePrograms();
            }
        });
    }

    public JComponent getComponent() {
        return panel;
    }

    public void setContinuous(boolean continuous) {
        if (continuous) {
            if (timer == null) {
                timer = new Timer(16, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        panel.repaint();
                    }
                });
            }
            timer.start();
        } else if (timer != null) {
            timer.stop();
        }
    }

    public void requestRender() {
        panel.repaint();
    }

    public void disposeSurface() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        try {
            panel.destroy();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
