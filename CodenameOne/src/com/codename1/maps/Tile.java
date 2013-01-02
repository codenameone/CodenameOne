/*
 * Copyright (c) 2010, 2011 Itiner.pl. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Itiner designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Itiner in the LICENSE.txt file that accompanied this code.
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
 */
package com.codename1.maps;

import com.codename1.ui.geom.Dimension;
import com.codename1.maps.BoundingBox;
import com.codename1.maps.Coord;
import com.codename1.ui.geom.Point;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.UIManager;

/**
 * This class represents a single tile on a map.
 * a map is been constructed from a few tiles that are been tiled on next to the 
 * other.
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class Tile {
    
    private Dimension dimension;
    private BoundingBox bbox;
    private Image tileImage;
    private static Image tileLoadingImage;    
    private static String tileLoadingText = "Loading...";
    private ActionListener listener;
    private static Font f = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);
    private static boolean paintLoading = false;
    
    /**
     * Creates a new Tile.
     * 
     * @param dimension the tile Dimensions (usually 256x256)
     * @param getBoundingBox the bounding box this tile is showing
     * @param image the map image or null.
     */
    public Tile(Dimension dimension, BoundingBox boundingBox, Image image) {
        this.dimension = dimension;
        bbox = boundingBox;
        tileImage = image;
        tileLoadingImage = UIManager.getInstance().getThemeImageConstant("mapTileLoadingImage");
        tileLoadingText = UIManager.getInstance().getThemeConstant("mapTileLoadingText", "Loading...");
    }
    
    /**
     * Returns the x, y point of the given coordinate relative to this tile
     * @param point a coordinate to translate to x, y
     * @return a Point object relative to this tile
     */
    public Point pointPosition(Coord point) {
        int x = position(dimension.getWidth(), point.getLongitude(),
                bbox.getSouthWest().getLongitude(), bbox.getNorthEast().getLongitude());
        int y = position(dimension.getHeight(), point.getLatitude(),
                bbox.getSouthWest().getLatitude(), bbox.getNorthEast().getLatitude());
        //
        return new Point(x, dimension.getHeight() - y);
    }
    
    private int position(int dx, double x, double x1, double x2) {
        return (int) (dx * (x - x1) / (x2 - x1));
    }
    
    private double coord(double percent, double x1, double x2) {
        return x1 + percent * (x2 - x1);
    }
    
    /**
     * Returns the Coordinate of the given x, y position on the tile
     * @param posX
     * @param posY
     * @return a Coordinate that was created from the given x, y position
     */
    public Coord position(int posX, int posY) {
        double longitude = coord(1.0 * posX / dimension.getWidth(), bbox.getSouthWest().getLongitude(), bbox.getNorthEast().getLongitude());
        double latitude = coord(1.0 * posY / dimension.getHeight(), bbox.getSouthWest().getLatitude(), bbox.getNorthEast().getLatitude());
        return new Coord(latitude, longitude, bbox.getSouthWest().isProjected());
    }
    
    /**
     * Gets the tile dimension
     * @return the tile dimension
     */
    public Dimension dimension() {
        return dimension;
    }
    
    /**
     * Gets the tile bounding box.
     * @return the tile bounding box.
     */
    public BoundingBox getBoundingBox() {
        return bbox;
    }
    
    /**
     * Paints the tile on the Graphics Object
     * @param g Graphics object to paint on.
     * @return true if painting succeeded.
     */
    public boolean paint(Graphics g) {
        if (tileImage != null) {
            g.drawImage(tileImage, 0, 0);
            return true;
        }
        return false;
    }
    
    /**
     * Paints the tile on the Graphics Object translated to the given x, y, 
     * This method paints the tile image if available or will call 
     * paintTileLoading
     * 
     * @param g Graphics object to paint on.
     * @param x translate to x before painting
     * @param y translate to y before painting
     */
    public void paint(Graphics g, int x, int y) {
        g.translate(x, y);
        if (!paint(g) && paintLoading) {
            paintTileLoading(g);
        }
        g.translate(-x, -y);
    }
    
    /**
     * This method paints a "tile loading" on the Graphics if 
     * boolean paint(Graphics g) returned false.
     * @param g Graphics object to paint on.
     */
    public void paintTileLoading(Graphics g) {
        if (tileLoadingImage == null) {
            paintLoadingText(g);
        } else {
            paintLoadingImage(g);
        }        
    }

    private void paintLoadingText(Graphics g) {
        g.setColor(0x707070);
        g.fillRect(0, 0, dimension().getWidth(), dimension().getHeight());
        g.setColor(0xFFFFFF);
        g.setFont(f);
        Font f = g.getFont();
        int strWidth = f.stringWidth(tileLoadingText);
        g.drawString(tileLoadingText, (dimension().getWidth() - strWidth) / 2, (dimension().getHeight() - f.getHeight()) / 2);
    }
    
    private void paintLoadingImage(Graphics g) {
        for (int y = 0; y < dimension().getHeight(); y += tileLoadingImage.getHeight()) {
            for (int x = 0; x < dimension().getWidth(); x += tileLoadingImage.getWidth()) {
                g.drawImage(tileLoadingImage, x, y);
            }
        }
    }
    
    /**
     * This flag indicates if the Tile should paint a Loading image or Text or
     * simply not do any painting if a map image is not ready for painting
     * 
     * @param if true a Loading rect is displayed when map image is being 
     * downloaded
     */ 
    public static void setPaintLoading(boolean toPaint){
        paintLoading = toPaint;
    }
    
    /**
     * Sets a static image that will be drawn on the map if the tile image is 
     * not available yet.
     * 
     * @param tileLoadingImage 
     */
    public static void setTileLoadingImage(Image tileLoadingImage) {
        Tile.tileLoadingImage = tileLoadingImage;
    }
    
    /**
     * Sets a static text to paint.
     * This will be used if the map if the tile image is not available yet and
     * the tileLoadingImage is null
     * @param tileLoadingText 
     */
    public static void setTileLoadingText(String tileLoadingText) {
        Tile.tileLoadingText = tileLoadingText;
    }
    
    /**
     * Sets a Listener to be notified when the tile is fireReady to be painted
     * @param listener 
     */
    public void setsTileReadyListener(ActionListener listener) {
        this.listener = listener;
    }
    
    /**
     * inform the TileReadyListener that this tile is ready to be painted
     */
    protected void fireReady() {
        if (listener != null) {
            listener.actionPerformed(null);
        }
    }
    
    /**
     * @inheritDoc
     */
    public String toString() {
        return getClass().getName() + " dimension: " + dimension + " bbox: " + bbox;
    }
}
