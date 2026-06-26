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
package com.codename1.impl.javase.simulator.host;

import com.codename1.io.Properties;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * The parsed contents of a simulator .skin file: the device frame images for
 * both orientations, the screen region within each frame, the touch hotspot
 * map (soft buttons painted into skin_map.png) and the safe-area insets.
 *
 * <p>This is a pure data model independent of the backend implementation -
 * the simulator chrome draws the frame around whichever surface the active
 * backend provides (a Swing panel or a native heavyweight canvas).</p>
 */
public class DeviceSkin {
    private BufferedImage portraitSkin;
    private BufferedImage landscapeSkin;
    private BufferedImage header;
    private BufferedImage headerLandscape;
    private boolean roundedSkin;
    private Rectangle safeAreaPortrait;
    private Rectangle safeAreaLandscape;
    private Map<Point, Integer> portraitHotspots;
    private Map<Point, Integer> landscapeHotspots;
    private Rectangle portraitScreenCoordinates;
    private Rectangle landscapeScreenCoordinates;

    /**
     * @return true once a skin file has been loaded into this model
     */
    public boolean isLoaded() {
        return portraitSkin != null;
    }

    /**
     * @param portrait true for the portrait orientation
     * @return the device frame image for the given orientation
     */
    public BufferedImage getSkin(boolean portrait) {
        return portrait ? portraitSkin : landscapeSkin;
    }

    /**
     * @param portrait true for the portrait orientation
     * @return the soft-button hotspot map for the given orientation
     */
    public Map<Point, Integer> getHotspots(boolean portrait) {
        return portrait ? portraitHotspots : landscapeHotspots;
    }

    /**
     * @param portrait true for the portrait orientation
     * @return the screen region within the device frame for the given orientation
     */
    public Rectangle getScreenCoordinates(boolean portrait) {
        return portrait ? portraitScreenCoordinates : landscapeScreenCoordinates;
    }

    /**
     * @param portrait true for the portrait orientation
     * @return the safe area for the given orientation, or null when the skin
     * defines none
     */
    public Rectangle getSafeArea(boolean portrait) {
        return portrait ? safeAreaPortrait : safeAreaLandscape;
    }

    public BufferedImage getPortraitSkin() {
        return portraitSkin;
    }

    public void setPortraitSkin(BufferedImage portraitSkin) {
        this.portraitSkin = portraitSkin;
    }

    public BufferedImage getLandscapeSkin() {
        return landscapeSkin;
    }

    public void setLandscapeSkin(BufferedImage landscapeSkin) {
        this.landscapeSkin = landscapeSkin;
    }

    public BufferedImage getHeader() {
        return header;
    }

    public void setHeader(BufferedImage header) {
        this.header = header;
    }

    public BufferedImage getHeaderLandscape() {
        return headerLandscape;
    }

    public void setHeaderLandscape(BufferedImage headerLandscape) {
        this.headerLandscape = headerLandscape;
    }

    public boolean isRoundedSkin() {
        return roundedSkin;
    }

    public void setRoundedSkin(boolean roundedSkin) {
        this.roundedSkin = roundedSkin;
    }

    public void setHotspots(Map<Point, Integer> portraitHotspots, Map<Point, Integer> landscapeHotspots) {
        this.portraitHotspots = portraitHotspots;
        this.landscapeHotspots = landscapeHotspots;
    }

    public void setScreenCoordinates(Rectangle portrait, Rectangle landscape) {
        this.portraitScreenCoordinates = portrait;
        this.landscapeScreenCoordinates = landscape;
    }

    public void setSafeAreas(Rectangle portrait, Rectangle landscape) {
        this.safeAreaPortrait = portrait;
        this.safeAreaLandscape = landscape;
    }

    /**
     * Parses a skin_map.png pixel map: black pixels mark the screen region,
     * white pixels are blank and any other color is looked up in the skin
     * properties (c&lt;hex&gt; or x&lt;hex&gt; keys) as a soft-button key code
     * for the hotspot map.
     *
     * @param map the skin_map image
     * @param props the skin properties
     * @param coordinates receives the hotspot pixel-to-keycode mapping
     * @param screenPosition receives the bounding box of the screen region
     */
    public static void parseSkinMap(BufferedImage map, Properties props, Map<Point, Integer> coordinates, Rectangle screenPosition) {
        int[] buffer = new int[map.getWidth() * map.getHeight()];
        map.getRGB(0, 0, map.getWidth(), map.getHeight(), buffer, 0, map.getWidth());
        int screenX1 = Integer.MAX_VALUE;
        int screenY1 = Integer.MAX_VALUE;
        int screenX2 = 0;
        int screenY2 = 0;
        for (int iter = 0; iter < buffer.length; iter++) {
            int pixel = buffer[iter];
            // white pixels are blank
            if (pixel != 0xffffffff) {
                int x = iter % map.getWidth();
                int y = iter / map.getWidth();

                // black pixels represent the screen region
                if (pixel == 0xff000000) {
                    if (x < screenX1) {
                        screenX1 = x;
                    }
                    if (y < screenY1) {
                        screenY1 = y;
                    }
                    if (x > screenX2) {
                        screenX2 = x;
                    }
                    if (y > screenY2) {
                        screenY2 = y;
                    }
                } else {
                    String prop = "c" + Integer.toHexString(0xffffff & pixel);
                    String val = props.getProperty(prop);
                    int code = 0;
                    if (val == null) {
                        val = props.getProperty("x" + Integer.toHexString(pixel));
                        if (val == null) {
                            continue;
                        }
                        code = Integer.parseInt(val, 16);
                    } else {
                        code = Integer.parseInt(val);
                    }
                    coordinates.put(new Point(x, y), code);
                }
            }
        }
        screenPosition.x = screenX1;
        screenPosition.y = screenY1;
        screenPosition.width = screenX2 - screenX1 + 1;
        screenPosition.height = screenY2 - screenY1 + 1;
    }
}
