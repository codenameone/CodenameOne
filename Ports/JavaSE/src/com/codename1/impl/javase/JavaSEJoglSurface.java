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
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/// Real OpenGL surface for the JavaSE simulator, backed by JOGL.
///
/// Unlike a live on-screen `GLJPanel`, this renders the scene to an *offscreen*
/// FBO and reads the pixels back into a `BufferedImage` that is blitted during the
/// component's own `paintComponent`. That makes it a plain lightweight `JComponent`
/// that paints an image -- exactly like the software fallback -- so Codename One
/// captures, clips, layers, scales and (critically) rotates it through its normal
/// peer-compositing path. A live `GLJPanel` could not be composited that way: in
/// the rotated landscape skin the heavyweight GL panel and CN1's buffered copy
/// desynced and flickered. Rendering offscreen and compositing the read-back image
/// keeps the GL frame perfectly in step with every CN1 repaint.
///
/// Constructing this class touches JOGL classes and initializes a GL context, so
/// it can throw `GLException` (no usable GL) or, when the JOGL jars are absent,
/// `NoClassDefFoundError`. The JavaSE port instantiates it inside a `try/catch`
/// and falls back to the software renderer, so neither failure breaks the
/// simulator.
class JavaSEJoglSurface extends JComponent implements JavaSEGpuSurface {
    private final RenderView view;
    private final Renderer renderer;
    private final JavaSEGLDevice device = new JavaSEGLDevice();
    private final GLProfile profile;
    private final GLOffscreenAutoDrawable drawable;
    private final AWTGLReadBufferUtil readback;
    private BufferedImage frame;
    private int lastW = -1;
    private int lastH = -1;
    private Timer timer;

    JavaSEJoglSurface(RenderView view) {
        this.view = view;
        this.renderer = view.getRenderer();
        setOpaque(false);

        // Use the legacy GL2 (OpenGL 2.1) profile: on macOS the GL2ES2 profile
        // resolves to a core context that rejects GLSL 1.20, whereas GL2 gives a
        // 2.1 compatibility context whose GLSL 1.20 accepts the shared GLSL ES
        // shaders (after JavaSEGLDevice prepends the #version directive). GL2 is
        // available on every desktop GL stack; if it is not, construction throws
        // and the port falls back to the software renderer.
        profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDepthBits(24);
        caps.setAlphaBits(8);
        caps.setOnscreen(false);
        caps.setFBO(true);
        GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
        drawable = factory.createOffscreenAutoDrawable(null, caps, null, 1, 1);
        drawable.addGLEventListener(new GLEventListener() {
            public void init(GLAutoDrawable d) {
                device.setGL(d.getGL().getGL2ES2());
                try {
                    renderer.onInit(device);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
                device.setGL(d.getGL().getGL2ES2());
                try {
                    renderer.onResize(device, w, h);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            public void display(GLAutoDrawable d) {
                device.setGL(d.getGL().getGL2ES2());
                try {
                    renderer.onFrame(device);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                try {
                    frame = readback.readPixelsToBufferedImage(d.getGL(), true);
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
        readback = new AWTGLReadBufferUtil(profile, true);
        // Realize the GL context now so a missing/unusable GL throws here (and the
        // port falls back to the software renderer) rather than mid-render.
        drawable.display();

        // The surface sits on top of the CN1 canvas, so native mouse events land on
        // it and never reach CN1's pointer pipeline -- leaving on-screen game
        // controls (and any CN1 component under the surface) dead. Forward them to
        // the canvas, reusing its coordinate conversion, so CN1 dispatches them (and
        // fires Form pointer listeners) normally.
        MouseAdapter forwarder = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                forward(e);
            }

            public void mouseReleased(MouseEvent e) {
                forward(e);
            }

            public void mouseDragged(MouseEvent e) {
                forward(e);
            }

            public void mouseMoved(MouseEvent e) {
                forward(e);
            }

            private void forward(MouseEvent e) {
                JavaSEPort port = JavaSEPort.instance;
                if (port == null || port.canvas == null) {
                    return;
                }
                JavaSEPort.C canvas = port.canvas;
                MouseEvent ce = SwingUtilities.convertMouseEvent(JavaSEJoglSurface.this, e, canvas);
                switch (e.getID()) {
                    case MouseEvent.MOUSE_PRESSED:
                        canvas.mousePressed(ce);
                        break;
                    case MouseEvent.MOUSE_RELEASED:
                        canvas.mouseReleased(ce);
                        break;
                    case MouseEvent.MOUSE_DRAGGED:
                        canvas.mouseDragged(ce);
                        break;
                    case MouseEvent.MOUSE_MOVED:
                        canvas.mouseMoved(ce);
                        break;
                    default:
                        break;
                }
            }
        };
        addMouseListener(forwarder);
        addMouseMotionListener(forwarder);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setContinuous(boolean continuous) {
        if (continuous) {
            if (timer == null) {
                timer = new Timer(16, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        view.repaint();
                    }
                });
            }
            timer.start();
        } else if (timer != null) {
            timer.stop();
        }
    }

    public void requestRender() {
        view.repaint();
    }

    public void disposeSurface() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        try {
            drawable.destroy();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        // Resize the offscreen FBO to match the component, then render one frame
        // synchronously and blit the read-back image. Rendering here (rather than on
        // a GL animator thread) keeps the GL output in lock-step with CN1's paint, so
        // the composited frame never tears or flickers, including in landscape.
        if (w != lastW || h != lastH) {
            lastW = w;
            lastH = h;
            try {
                drawable.setSurfaceSize(w, h);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        try {
            drawable.display();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        if (frame != null) {
            g.drawImage(frame, 0, 0, null);
        }
    }
}
