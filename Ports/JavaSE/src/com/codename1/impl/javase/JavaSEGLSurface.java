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

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/// AWT surface that hosts the JavaSE software 3D renderer. The component is
/// wrapped as a Codename One native peer; each time it is painted it drives one
/// frame of the application `Renderer` and blits the resulting image. In
/// continuous mode a Swing timer requests repaints to form an animation loop.
class JavaSEGLSurface extends JComponent {
    private final RenderView view;
    private final Renderer renderer;
    private final JavaSESoftwareDevice device = new JavaSESoftwareDevice();
    private boolean initialized;
    private int lastW = -1;
    private int lastH = -1;
    private Timer timer;

    JavaSEGLSurface(RenderView view) {
        this.view = view;
        this.renderer = view.getRenderer();
        setOpaque(false);
    }

    void setContinuous(boolean continuous) {
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

    void requestRender() {
        view.repaint();
    }

    void disposeSurface() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        try {
            renderer.onDispose(device);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        device.resize(w, h);
        if (!initialized) {
            try {
                renderer.onInit(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            initialized = true;
            lastW = -1;
        }
        if (w != lastW || h != lastH) {
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
        BufferedImage img = device.getImage();
        if (img != null) {
            g.drawImage(img, 0, 0, null);
        }
    }
}
