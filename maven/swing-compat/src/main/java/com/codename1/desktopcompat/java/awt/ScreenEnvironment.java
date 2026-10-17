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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.geom.AffineTransform;

/// The one graphics environment, with its one device and configuration.
final class ScreenEnvironment extends GraphicsEnvironment {

    private final Device device = new Device();

    @Override
    public GraphicsDevice[] getScreenDevices() {
        return new GraphicsDevice[]{device};
    }

    @Override
    public GraphicsDevice getDefaultScreenDevice() {
        return device;
    }

    @Override
    public String[] getAvailableFontFamilyNames() {
        String[] own = {Font.DIALOG, Font.DIALOG_INPUT, Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF};
        String[] added = com.codename1.desktopcompat.rt.FontFiles.families();
        String[] all = new String[own.length + added.length];
        System.arraycopy(own, 0, all, 0, own.length);
        System.arraycopy(added, 0, all, own.length, added.length);
        return all;
    }

    @Override
    public Graphics2D createGraphics(com.codename1.desktopcompat.java.awt.image.BufferedImage img) {
        if (img == null) {
            throw new NullPointerException("BufferedImage cannot be null");
        }
        return img.createGraphics();
    }

    /// The display.
    static final class Device extends GraphicsDevice {

        private final Configuration configuration = new Configuration(this);

        @Override
        public int getType() {
            return TYPE_RASTER_SCREEN;
        }

        @Override
        public String getIDstring() {
            return "Display0";
        }

        @Override
        public GraphicsConfiguration[] getConfigurations() {
            return new GraphicsConfiguration[]{configuration};
        }

        @Override
        public GraphicsConfiguration getDefaultConfiguration() {
            return configuration;
        }
    }

    /// The display's one configuration: its bounds are the screen size in
    /// logical pixels, and its transform is the identity because a
    /// logical pixel is already 1/96 inch.
    static final class Configuration extends GraphicsConfiguration {

        private final GraphicsDevice device;

        Configuration(GraphicsDevice device) {
            this.device = device;
        }

        @Override
        public GraphicsDevice getDevice() {
            return device;
        }

        @Override
        public Rectangle getBounds() {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            return new Rectangle(0, 0, d.width, d.height);
        }

        @Override
        public AffineTransform getDefaultTransform() {
            return new AffineTransform();
        }

        @Override
        public AffineTransform getNormalizingTransform() {
            return new AffineTransform();
        }
    }
}
