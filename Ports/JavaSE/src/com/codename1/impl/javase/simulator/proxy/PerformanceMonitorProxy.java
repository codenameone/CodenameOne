/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.impl.javase.simulator.proxy;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.CodenameOneImplementationDecorator;
import com.codename1.impl.javase.PerformanceMonitor;
import com.codename1.impl.javase.simulator.tools.SimulatorTools;
import com.codename1.ui.Component;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Decorator feeding the simulator's performance monitor from the
 * implementation's drawing, font and image-creation entry points. Works
 * against any backend because it only observes the call parameters and the
 * opaque return values - it never inspects the native image objects beyond
 * asking the delegate for their dimensions.
 *
 * <p>Image RAM accounting tracks the native images returned by the create
 * methods with weak references; when an image is garbage collected its bytes
 * are subtracted from the monitor's RAM gauge. This replaces the historical
 * finalizer-based tracking that lived in JavaSEPort.</p>
 */
public class PerformanceMonitorProxy extends CodenameOneImplementationDecorator {
    private final ReferenceQueue<Object> collectedImages = new ReferenceQueue<Object>();
    private final Set<TrackedImage> trackedImages
            = Collections.synchronizedSet(new HashSet<TrackedImage>());

    private static class TrackedImage extends WeakReference<Object> {
        private final int bytes;

        TrackedImage(Object image, ReferenceQueue<Object> queue, int bytes) {
            super(image, queue);
            this.bytes = bytes;
        }
    }

    public PerformanceMonitorProxy(CodenameOneImplementation delegate) {
        super(delegate);
    }

    private static PerformanceMonitor perf() {
        return SimulatorTools.getPerformanceMonitor();
    }

    /**
     * Registers a newly created native image with the RAM gauge and arranges
     * for the bytes to be subtracted when it is garbage collected.
     */
    private void trackImage(Object nativeImage) {
        PerformanceMonitor pm = perf();
        if (pm != null && nativeImage != null) {
            int bytes = delegate.getImageWidth(nativeImage) * delegate.getImageHeight(nativeImage) * 4;
            pm.addImageRAM(bytes);
            trackedImages.add(new TrackedImage(nativeImage, collectedImages, bytes));
        }
        drainCollectedImages();
    }

    private void drainCollectedImages() {
        PerformanceMonitor pm = perf();
        TrackedImage ref;
        while ((ref = (TrackedImage) collectedImages.poll()) != null) {
            trackedImages.remove(ref);
            if (pm != null) {
                pm.removeImageRAM(ref.bytes);
            }
        }
    }

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        Object i = delegate.createImage(rgb, width, height);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.printToLog("Created RGB image width: " + width + " height: " + height
                    + " size (bytes) " + (width * height * 4));
        }
        trackImage(i);
        return i;
    }

    @Override
    public Object createImage(String path) throws IOException {
        Object i = delegate.createImage(path);
        PerformanceMonitor pm = perf();
        if (pm != null && i != null) {
            int w = delegate.getImageWidth(i);
            int h = delegate.getImageHeight(i);
            pm.printToLog("Created path image " + path + " width: " + w + " height: " + h
                    + " size (bytes) " + (w * h * 4));
        }
        trackImage(i);
        return i;
    }

    @Override
    public Object createImage(InputStream in) throws IOException {
        Object i = delegate.createImage(in);
        PerformanceMonitor pm = perf();
        if (pm != null && i != null) {
            int w = delegate.getImageWidth(i);
            int h = delegate.getImageHeight(i);
            pm.printToLog("Created InputStream image width: " + w + " height: " + h
                    + " size (bytes) " + (w * h * 4));
        }
        trackImage(i);
        return i;
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        Object i = delegate.createImage(bytes, offset, len);
        PerformanceMonitor pm = perf();
        if (pm != null && i != null) {
            int w = delegate.getImageWidth(i);
            int h = delegate.getImageHeight(i);
            pm.printToLog("Created data image width: " + w + " height: " + h
                    + " data size (bytes) " + bytes.length
                    + " unpacked size (bytes) " + (w * h * 4));
        }
        trackImage(i);
        return i;
    }

    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.printToLog("Created mutable image width: " + width + " height: " + height
                    + " size (bytes) " + (width * height * 4));
        }
        Object i = delegate.createMutableImage(width, height, fillColor);
        trackImage(i);
        return i;
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.printToLog("Scaling image from width: " + delegate.getImageWidth(nativeImage)
                    + " height: " + delegate.getImageHeight(nativeImage)
                    + " to width: " + width + " height: " + height
                    + " size (bytes) " + (width * height * 4));
        }
        Object i = delegate.scale(nativeImage, width, height);
        if (i != nativeImage) {
            trackImage(i);
        }
        return i;
    }

    @Override
    public void drawingEncodedImage(EncodedImage img) {
        delegate.drawingEncodedImage(img);
        PerformanceMonitor pm = perf();
        if (pm != null && !img.isLocked()) {
            pm.printToLog("Drawing unlocked image: " + img.getImageName());
        }
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        delegate.setClip(graphics, x, y, width, height);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.setClip(x, y, width, height);
        }
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        delegate.clipRect(graphics, x, y, width, height);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.clipRect(x, y, width, height);
        }
    }

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        delegate.drawLine(graphics, x1, y1, x2, y2);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.drawLine(x1, y1, x2, y2);
        }
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int w, int h) {
        delegate.fillRect(graphics, x, y, w, h);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.fillRect(x, y, w, h);
        }
    }

    @Override
    public void clearRect(Object graphics, int x, int y, int width, int height) {
        delegate.clearRect(graphics, x, y, width, height);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.clearRect(x, y, width, height);
        }
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        delegate.drawRect(graphics, x, y, width, height);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.drawRect(x, y, width, height);
        }
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.drawRoundRect(graphics, x, y, width, height, arcWidth, arcHeight);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.fillRoundRect(graphics, x, y, width, height, arcWidth, arcHeight);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        delegate.fillArc(graphics, x, y, width, height, startAngle, arcAngle);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.fillArc(x, y, width, height, startAngle, arcAngle);
        }
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        delegate.drawArc(graphics, x, y, width, height, startAngle, arcAngle);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.drawArc(x, y, width, height, startAngle, arcAngle);
        }
    }

    @Override
    public void setColor(Object graphics, int RGB) {
        delegate.setColor(graphics, RGB);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.setColor(RGB);
        }
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        delegate.setAlpha(graphics, alpha);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.setAlpha(alpha);
        }
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        delegate.drawString(graphics, str, x, y);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.drawString(str, x, y);
        }
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        delegate.drawImage(graphics, img, x, y);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.drawImage(img, x, y);
        }
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y, int w, int h) {
        delegate.drawImage(graphics, img, x, y, w, h);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.drawImage(img, x, y, w, h);
        }
    }

    @Override
    public void fillTriangle(Object graphics, int x1, int y1, int x2, int y2, int x3, int y3) {
        delegate.fillTriangle(graphics, x1, y1, x2, y2, x3, y3);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.fillTriangle(x1, y1, x2, y2, x3, y3);
        }
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        delegate.drawRGB(graphics, rgbData, offset, x, y, w, h, processAlpha);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.drawRGB(rgbData, offset, x, y, w, h, processAlpha);
        }
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        int w = delegate.stringWidth(nativeFont, str);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.stringWidth(nativeFont, str);
        }
        return w;
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        int w = delegate.charWidth(nativeFont, ch);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.charWidth(nativeFont, ch);
        }
        return w;
    }

    @Override
    public void beforeComponentPaint(Component c, Graphics g) {
        delegate.beforeComponentPaint(c, g);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.beforeComponentPaint(c);
        }
    }

    @Override
    public void afterComponentPaint(Component c, Graphics g) {
        delegate.afterComponentPaint(c, g);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.afterComponentPaint(c);
        }
    }

    @Override
    public void nothingWithinComponentPaint(Component c) {
        delegate.nothingWithinComponentPaint(c);
        PerformanceMonitor pm = perf();
        if (pm != null) {
            pm.nothingWithinComponentPaint(c);
        }
    }
}
