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

import com.codename1.location.Location;
import com.codename1.location.LocationManager;
import com.codename1.ui.geom.Point;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import com.codename1.maps.providers.MapProvider;
import com.codename1.maps.layers.AbstractLayer;
import com.codename1.maps.layers.PointsLayer;
import com.codename1.maps.providers.OpenStreetMapProvider;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Painter;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;


/**
 * All communication with the map and layers should be done in WGS84, it takes care of coordinates transformation.
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class MapComponent extends Container {

    protected Coord _center;
    protected int _zoom;
    protected MapProvider _map;
    protected Vector _layers;
    protected boolean _debugInfo = false;
    protected boolean _needTiles = true;
    private int draggedx, draggedy;
    private Vector _tiles;
    private Point _delta = null;

    /**
     * Empty constructor creates a map with OpenStreetMapProvider on the Last 
     * known Location of the LocationManager
     */
    public MapComponent() {
        this(new OpenStreetMapProvider());
        Location l = LocationManager.getLocationManager().getLastKnownLocation();
        Coord centerPosition = new Coord(l.getLatitude(), l.getLongtitude());
        _center = centerPosition.isProjected() ? centerPosition : _map.projection().fromWGS84(centerPosition);
        Painter bg = new Painter() {
            public void paint(Graphics g, Rectangle rect) {
                paintmap(g);
            }
        };
        getUnselectedStyle().setBgTransparency(255);
        getStyle().setBgTransparency(255);        
        getUnselectedStyle().setBgPainter(bg);
        getStyle().setBgPainter(bg);
        
    }

    /**
     * Constructor with a given provider
     * @param provider map provider
     */
    public MapComponent(MapProvider provider) {
        this(provider, new Coord(0, 0, true), 4, true);
    }

    /**
     * Constructor
     * 
     * @param provider map provider
     * @param centerPosition center position
     * @param zoomLevel zoom level
     */
    public MapComponent(MapProvider provider, Location centerPosition, int zoomLevel) {
        this(provider, centerPosition, zoomLevel, true);
    }

    /**
     * Constructor
     * 
     * @param provider map provider
     * @param centerPosition center position
     * @param zoomLevel zoom level
     * @param cacheEnabled is cache enabled
     */
    public MapComponent(MapProvider provider, Location centerPosition, int zoomLevel, boolean cacheEnabled) {
        this(provider, new Coord(centerPosition.getLatitude(), centerPosition.getLongtitude()), zoomLevel, cacheEnabled);
    }

    /**
     * Constructor
     * 
     * @param provider map provider
     * @param centerPosition center position
     * @param zoomLevel zoom level
     */
    public MapComponent(MapProvider provider, Coord centerPosition, int zoomLevel) {
        this(provider, centerPosition, zoomLevel, true);
    }

    /**
     * Constructor
     * 
     * @param provider map provider
     * @param centerPosition center position
     * @param zoomLevel zoom level
     * @param cacheEnabled is cache enabled
     */
    public MapComponent(MapProvider provider, Coord centerPosition, int zoomLevel, boolean cacheEnabled) {
        _map = new CacheProviderProxy(provider);
        _center = centerPosition.isProjected() ? centerPosition : _map.projection().fromWGS84(centerPosition);
        _zoom = zoomLevel;
        _layers = new Vector();
        setFocusable(!Display.getInstance().isTouchScreenDevice());
        if (Display.getInstance().isTouchScreenDevice()) {
            setLayout(new BorderLayout());
            Container buttonsbar = new Container(new FlowLayout(Component.RIGHT));
            Button out = new Button("-");
            out.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    zoomOut();
                    repaint();
                }
            });
            buttonsbar.addComponent(out);
            Button in = new Button("+");
            in.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    zoomIn();
                    repaint();
                }
            });
            buttonsbar.addComponent(in);
            addComponent(BorderLayout.SOUTH, buttonsbar);
        }
    }

    private void toggleDebug() {
        _debugInfo = !_debugInfo;
    }

    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize() {
        return new Dimension(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
    }

    /**
     * @inheritDoc
     */
    protected void focusGained() {
        setHandlesInput(true);
    }

    /**
     * @inheritDoc
     */
    public void pointerDragged(int x, int y) {
        super.pointerDragged(x, y);
        Coord scale = _map.scale(_zoom);
        int deltax = draggedx - x;
        int deltay = draggedy - y;
        _center = _center.translate(deltay * -scale.getLatitude(), deltax * scale.getLongitude());
        _needTiles = true;
        draggedx = x;
        draggedy = y;
        repaint();
    }

    /**
     * @inheritDoc
     */
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        draggedx = x;
        draggedy = y;
        x = x - getAbsoluteX();
        y = y - getAbsoluteY();
        
        Tile t = screenTile();
        
        Coord southWest = t.position(x - 20, t.dimension().getHeight() - y - 20);
        Coord c = Mercator.inverseMercator(southWest.getLatitude(), southWest.getLongitude());
        Coord northEast = t.position(x + 20, t.dimension().getHeight() - y + 20);
        c = Mercator.inverseMercator(northEast.getLatitude(), northEast.getLongitude());
        
        BoundingBox bbox = new BoundingBox(southWest, northEast);
        Enumeration e = _layers.elements();
        while (e.hasMoreElements()) {
            LayerWithZoomLevels layer = (LayerWithZoomLevels) e.nextElement();
            if(layer.layer instanceof PointsLayer){
                ((PointsLayer)layer.layer).fireActionEvent(bbox);
            }
        }

    }

    /**
     * @inheritDoc
     */
    public void keyPressed(int keyCode) {
        int oldZoom = _zoom;
        Coord oldCenter = _center;

        if (isLeftKey(keyCode)) {
            moveLeft();
        } else if (isRightKey(keyCode)) {
            moveRight();
        } else if (isDownKey(keyCode)) {
            moveDown();
        } else if (isUpKey(keyCode)) {
            moveUp();
        }
        if (!_map.projection().extent().contains(_center)) {
            _center = oldCenter;
        }
        if (isZoomInKey(keyCode)) {
            zoomIn();
        }
        if (isZoomOutKey(keyCode)) {
            zoomOut();
        }
        if (isZoomToLayersKey(keyCode)) {
            zoomToLayers();
        }
        super.keyPressed(keyCode);
        if (_center != oldCenter || _zoom != oldZoom) {
            _needTiles = true;
        }
        repaint();
    }

    private void paintmap(Graphics g) {
        g.translate(getX(), getY());
        if (_needTiles) {
            getTiles();
            _needTiles = false;
        }
        drawTiles(g);
        drawLayers(g);
        if (_debugInfo) {
            drawDebug(g);
        }
        drawPointer(g);
        drawAttribution(g, _map.attribution());
        g.translate(-getX(), -getY());
    }

    /**
     * @inheritDoc
     */
    private Tile screenTile() {
        Dimension componentDimension = new Dimension(getWidth(), getHeight());
        Coord southWest = _map.translate(_center, _zoom, -getWidth() / 2, -getHeight() / 2);
        Coord northEast = _map.translate(_center, _zoom, getWidth() / 2, getHeight() / 2);
        BoundingBox bbox = new BoundingBox(southWest, northEast);
        return new Tile(componentDimension, bbox, null);
    }

    private void getTiles() throws RuntimeException {
        _tiles = new Vector();
        Dimension tileSize = _map.tileSize();
        int posY = 0;
        _delta = null;
        while (posY - tileSize.getHeight() < getHeight()) {
            int posX = 0;
            while (posX - tileSize.getWidth() < getWidth()) {
                Tile tile;
                Coord cur = _map.translate(_center, _zoom, posX - getWidth() / 2, getHeight() / 2 - posY);
                if (!_map.projection().extent().contains(cur)) {
//                    tile = _unavailableTile;
                } else {
                    tile = _map.tileFor(_map.bboxFor(cur, _zoom));
                    //_map.tileFor(cur, _zoom);
                    //#mdebug
                    if (!tile.getBoundingBox().contains(cur)) {
                        throw new RuntimeException("Map returned bad tile (" + tile.getBoundingBox() + ") for: " + cur.toString());
                    }
                    //#enddebug
                    if (_delta == null) {
                        _delta = tile.pointPosition(cur);
                    }
                    tile.setsTileReadyListener(new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            repaint();
                        }
                    });
                    _tiles.addElement(new PositionedTile(new Point(posX, posY), tile));
                }
                posX += tileSize.getWidth();
            }
            posY += tileSize.getHeight();
        }
    }

    private void drawTiles(Graphics g) {
        if (_delta == null) {
            //#debug
            System.out.println("Delta is null!");
            return;
        }
        Enumeration e = _tiles.elements();
        g.translate(-_delta.getX(), -_delta.getY());
        while (e.hasMoreElements()) {
            PositionedTile pt = (PositionedTile) e.nextElement();
            pt.tile().paint(g, pt.position().getX(), pt.position().getY());
        }
        g.translate(_delta.getX(), _delta.getY());
    }

    private void drawAttribution(Graphics g, String attribution) {
        if (attribution == null) {
            return;
        }
        g.setColor(0);
        g.setFont(Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_SMALL));
        Font f = g.getFont();
        g.drawString(attribution, getWidth() - f.stringWidth(attribution) - 2, getHeight() - f.getHeight() - 2);
    }

    private void drawLayers(Graphics g) {
        Enumeration e = _layers.elements();
        Tile screenTile = screenTile();
        while (e.hasMoreElements()) {
            LayerWithZoomLevels layer = (LayerWithZoomLevels) e.nextElement();
            if (_zoom >= layer.minZoomLevel && _zoom <= layer.maxZoomLevel) {
                layer.layer.paint(g, screenTile);
            }
        }
    }

    private void drawPointer(Graphics g) {
        g.setColor(0xFF0000);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int halfSize = 5;
        g.drawRoundRect(centerX - halfSize, centerY - halfSize, 2 * halfSize, 2 * halfSize, halfSize, halfSize);
    }

    private void drawDebug(Graphics g) {
        g.setColor(0x000000);
        g.setFont(Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
        g.drawString(_map.projection().toWGS84(_center).toString(), 5, 5);
        g.drawString("Zoom:" + _zoom, 5, 5 + g.getFont().getHeight());
        for (int i = 0; i < _layers.size(); i++) {
            LayerWithZoomLevels lwzl = (LayerWithZoomLevels) _layers.elementAt(i);
            g.drawString("Layer " + lwzl.layer.getName(), 5, 5 + (i + 2) * g.getFont().getHeight());
        }
    }

    /**
     * Adds a layer to the map
     * @param layer to add
     */
    public void addLayer(AbstractLayer layer) {
        addLayer(layer, 0, _map.maxZoomLevel());
    }

    /**
     * Adds a layer to the map
     * @param layer to add
     * @param minZoomLevel min zoom level of this Layer
     * @param maxZoomLevel max zoom level of this Layer
     */
    public void addLayer(AbstractLayer layer, int minZoomLevel, int maxZoomLevel) {
        _layers.addElement(new LayerWithZoomLevels(layer, minZoomLevel, maxZoomLevel));
        repaint();
    }

    /**
     * Removes a Layer from the map
     * @param layer to remove
     */
    public void removeLayer(AbstractLayer layer) {
        int length = _layers.size();
        int no;
        for (no = 0; no < length; no++) {
            if (((LayerWithZoomLevels) _layers.elementAt(no)).layer == layer) {
                break;
            }
        }
        _layers.removeElementAt(no);
    }

    /**
     * Gets the map provider
     * @return the map provider
     */
    public MapProvider getProvider() {
        return _map;
    }

    /**
     * move the map 25% left
     */
    public void moveLeft() {
        Coord scale = _map.scale(_zoom);
        double partX = 1.0 * getWidth() / 4;
        _center = _center.translate(0, partX * -scale.getLongitude());
        _needTiles = true;
    }

    /**
     * move the map 25% right
     */
    public void moveRight() {
        Coord scale = _map.scale(_zoom);
        double partX = 1.0 * getWidth() / 4;
        _center = _center.translate(0, partX * scale.getLongitude());
        _needTiles = true;
    }

    /**
     * move the map 25% up
     */
    public void moveUp() {
        Coord scale = _map.scale(_zoom);
        double partY = 1.0 * getHeight() / 4;
        _center = _center.translate(partY * scale.getLatitude(), 0);
        _needTiles = true;
    }

    /**
     * move the map 25% down
     */
    public void moveDown() {
        Coord scale = _map.scale(_zoom);
        double partY = 1.0 * getHeight() / 4;
        _center = _center.translate(partY * -scale.getLatitude(), 0);
        _needTiles = true;
    }

    /**
     * zoom in the map one level if possible
     */
    public void zoomIn() {
        if (_zoom < _map.maxZoomLevel()) {
            _zoom += 1;
            _needTiles = true;
        }
    }

    /**
     * zoom out the map one level if possible
     */
    public void zoomOut() {
        if (_zoom > _map.minZoomLevel()) {
            _zoom -= 1;
            _needTiles = true;
        }
    }

    /**
     * Zoom the map the the giving bounding box
     * @param boundingBox to zoom to
     */
    public void zoomTo(BoundingBox boundingBox) {
        Dimension dimension = null;
        if (getWidth() == 0 || getHeight() == 0) {
            dimension = getPreferredSize();
        } else {
            dimension = new Dimension(getWidth(), getHeight());
        }
        final BoundingBox projectedBBOX = boundingBox.projected() ? boundingBox : _map.projection().fromWGS84(boundingBox);
        Tile tile = new Tile(dimension, projectedBBOX, null);
        _zoom = _map.maxZoomFor(tile);
        _center = tile.position(tile.dimension().getWidth() / 2, tile.dimension().getHeight() / 2);
        repaint();
    }

    /**
     * Zoom map to the center of the given coordinate with the given zoom level
     * @param coord center map to this coordinate
     * @param zoomLevel zoom map to this leve;
     */
    public void zoomTo(Coord coord, int zoomLevel) {
        _center = _map.projection().fromWGS84(coord);
        _zoom = zoomLevel;
    }

    /**
     * zoom map to largest zoom while all Layers are contained
     */
    public void zoomToLayers() {
        BoundingBox bbox = null;
        Enumeration e = _layers.elements();
        while (e.hasMoreElements()) {
            LayerWithZoomLevels layer = (LayerWithZoomLevels) e.nextElement();

            BoundingBox layerBbox = layer.layer.boundingBox();
            if (layerBbox == null) {
                continue;
            }
            if (bbox == null) {
                bbox = layerBbox;
            } else {
                bbox = bbox.extend(layerBbox);
            }
        }
        if (bbox != null) {
            zoomTo(bbox);
        }
    }

    /**
     * Gets the center of the map.
     * @return Coord in WGS84
     */
    public Coord center() {
        return _map.projection().toWGS84(_center);
    }

    /**
     * Returns true if this is a left keycode 
     * @param keyCode
     * @return true if this is a left keycode 
     */
    protected boolean isLeftKey(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        return game == Display.GAME_LEFT;
    }

    /**
     * Returns true if this is a right keycode 
     * @param keyCode
     * @return 
     */
    protected boolean isRightKey(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        return game == Display.GAME_RIGHT;
    }

    /**
     * Returns true if this is a down keycode 
     * @param keyCode
     * @return 
     */
    protected boolean isDownKey(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        return game == Display.GAME_DOWN;
    }

    /**
     * Returns true if this is a up keycode 
     * @param keyCode
     * @return 
     */
    protected boolean isUpKey(int keyCode) {
        int game = Display.getInstance().getGameAction(keyCode);
        return game == Display.GAME_UP;
    }

    /**
     * Returns true if this is a zoom in keycode 
     * @param keyCode
     * @return 
     */
    protected boolean isZoomInKey(int keyCode) {
        return keyCode == '1';
    }

    /**
     * Returns true if this is a zoom out keycode 
     * @param keyCode
     * @return 
     */
    protected boolean isZoomOutKey(int keyCode) {
        return keyCode == '3';
    }

    /**
     * Returns true if this is a zoom to layers keycode 
     * @param keyCode
     * @return 
     */
    protected boolean isZoomToLayersKey(int keyCode) {
        return keyCode == '5';
    }
}
class LayerWithZoomLevels {

    public AbstractLayer layer;
    public int minZoomLevel;
    public int maxZoomLevel;

    public LayerWithZoomLevels(AbstractLayer l, int min, int max) {
        layer = l;
        minZoomLevel = min;
        maxZoomLevel = max;
    }
}
