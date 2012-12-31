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
import java.util.Enumeration;
import java.util.Vector;
import com.codename1.maps.providers.MapProvider;
import com.codename1.maps.layers.AbstractLayer;
import com.codename1.maps.layers.Layer;
import com.codename1.maps.layers.PointsLayer;
import com.codename1.maps.providers.OpenStreetMapProvider;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.MathUtil;


/**
 * All communication with the map and layers should be done in WGS84, it takes care of coordinates transformation.
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class MapComponent extends Container {

    private Coord _center;
    private int _zoom;
    private MapProvider _map;
    private Vector _layers;
    private boolean _debugInfo = false;
    private boolean _needTiles = true;
    private int draggedx, draggedy;
    private int pressedx, pressedy;
    private Vector _tiles;
    private Point _delta = null;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private boolean drawMapPointer = false;
    private double oldDistance = -1;
    private Image buffer = null;
    private boolean refreshLayers = false;
    private int scaleX = 0;
    private int scaleY = 0;
    private int translateX;
    private int translateY;
    
    
    private static Font attributionFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_SMALL);
    
    /**
     * Empty constructor creates a map with OpenStreetMapProvider on the Last 
     * known Location of the LocationManager
     */
    public MapComponent() {
        this(new OpenStreetMapProvider());
    }

    /**
     * Constructor with a given provider
     * @param provider map provider
     */
    public MapComponent(MapProvider provider) {
        this(provider, (Coord)null, 4, true);
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
        this(provider, new Coord(centerPosition.getLatitude(), centerPosition.getLongitude()), zoomLevel, cacheEnabled);
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
        if (cacheEnabled) {
            _map = new CacheProviderProxy(provider);
        } else {
            _map = provider;
        }

        if (centerPosition == null) {
            Location l = LocationManager.getLocationManager().getLastKnownLocation();
            if (l != null) {
                Coord p = new Coord(l.getLatitude(), l.getLongitude());
                _center = p.isProjected() ? p : _map.projection().fromWGS84(p);
            } else {
                _center = new Coord(0, 0, true);
            }
        }else{
            _center = centerPosition.isProjected() ? centerPosition : _map.projection().fromWGS84(centerPosition);        
        }
        
        _zoom = zoomLevel;
        _layers = new Vector();
        setFocusable(false);
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
        Painter bg = new Painter() {

            public void paint(Graphics g, Rectangle rect) {
                if (Display.getInstance().areMutableImagesFast()) {
                    if (buffer == null) {
                        buffer = Image.createImage(getWidth(), getHeight());
                    }
                    if (_needTiles || refreshLayers) {
                        paintmap(buffer.getGraphics());
                        refreshLayers = false;
                    }
                    g.translate(-translateX, -translateY);
                    if (scaleX > 0) {
                        g.drawImage(buffer, (getWidth() - scaleX) / 2, (getHeight() - scaleY) / 2, scaleX, scaleY);
                    } else {
                        g.drawImage(buffer, (getWidth() - buffer.getWidth()) / 2, (getHeight() - buffer.getHeight()) / 2);
                    }

                    g.translate(translateX, translateY);
                }else{
                    
                    g.translate(-translateX, -translateY);
                    if (scaleX > 0) {
                        g.translate(-(getWidth() - scaleX) / 2, -(getHeight() - scaleY) / 2);
                        g.scale((float)scaleX/(float)getWidth(), (float)scaleY/(float)getHeight());
                        paintmap(g);
                        g.resetAffine();
                        g.translate((getWidth() - scaleX) / 2, (getHeight() - scaleY) / 2);
                    }else{
                        paintmap(g);                    
                    }

                    g.translate(translateX, translateY);
                
                }
            }
        };
        getUnselectedStyle().setBgTransparency(255);
        getSelectedStyle().setBgTransparency(255);
        getUnselectedStyle().setBgPainter(bg);
        getSelectedStyle().setBgPainter(bg);
        drawMapPointer = UIManager.getInstance().isThemeConstant("drawMapPointerBool", false);        
    }

    @Override
    protected void laidOut() {
        super.laidOut();
        if(buffer != null){
            buffer.dispose();
        }
        buffer = null;
        repaint();
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
        
        translateX += (draggedx - x);
        translateY += (draggedy - y);
        draggedx = x;
        draggedy = y;
        repaint();
    }

    /**
     * @inheritDoc
     */
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);        
        pressedx = x;
        pressedy = y;
        draggedx = x;
        draggedy = y;
    }
    
    @Override
    public void pointerDragged(int[] x, int[] y) {
        if (x.length > 1) {
            double currentDis = distance(x, y);
            if(oldDistance == -1){
                oldDistance = currentDis;
                scaleX = getWidth();
                scaleY = getHeight();
            }
            if (Math.abs(currentDis - oldDistance) > 10f) {
                double scale = currentDis / oldDistance;
                if(scale > 1){
                    if(_zoom == getProvider().maxZoomLevel()){
                        return;
                    }
                }else{
                    if(_zoom == getProvider().minZoomLevel()){
                        return;
                    }                    
                }
                scaleX = (int) (scale * scaleX);
                scaleY = (int) (scale * scaleY);
                oldDistance = currentDis;
                repaint();
            }
        } else {
            super.pointerDragged(x, y);
        }
    }
    
    private double distance(int[] x, int[] y){
            int disx = x[0] - x[1];
            int disy = y[0] - y[1];
            return Math.sqrt(disx*disx + disy*disy);    
    }
    
    /**
     * @inheritDoc
     */
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        
        if(oldDistance != -1){
            double scale = (double)scaleX/(double)getWidth();
            if(scale > 1){
                if(scale < 1.2){
                    //do nothing
                }else if(scale < 1.6){
                    zoomIn();                
                }else if(scale < 2.0){
                    zoomIn();                
                    zoomIn();                
                }else if(scale < 2.4){
                    zoomIn();                
                    zoomIn();                
                    zoomIn();                                
                }else{
                    zoomIn();                
                    zoomIn();                
                    zoomIn();                                
                    zoomIn();                                                
                }
            }else{
                if(scale > 0.8){
                    //do nothing
                }else if(scale > 0.5){
                    zoomOut();                
                }else if(scale > 0.2){
                    zoomOut();                
                    zoomOut();                
                }else{
                    zoomOut();                
                    zoomOut();                
                    zoomOut();                                
                }
            }
            translateX = 0;        
            translateY = 0;        
            scaleX = 0;
            scaleY = 0;
            oldDistance = -1;
            if(buffer != null){
                buffer.dispose();
                buffer = null;
            }
            repaint();
            return;
        }
        Coord scale = _map.scale(_zoom);
        _center = _center.translate(translateY * -scale.getLatitude(), translateX * scale.getLongitude());
        _needTiles = true;
        translateX = 0;        
        translateY = 0;        
        
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
            if (layer.layer instanceof PointsLayer) {
                ((PointsLayer) layer.layer).fireActionEvent(bbox);
            }
        }
        repaint();
    }
    
    /**
     * Gets the Coord location on the map from a x, y position.
     * 
     * @param x
     * @param y
     * @return a Coord Object.
     */
    public Coord getCoordFromPosition(int x, int y) {
        x = x - getAbsoluteX();
        y = y - getAbsoluteY();
        Tile t = screenTile();
        Coord c = t.position(x, t.dimension().getHeight() - y);
        return _map.projection().toWGS84(c);
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
                if (_map.projection().extent().contains(cur)) {
                    tile = _map.tileFor(_map.bboxFor(cur, _zoom));
                    if (_delta == null) {
                        _delta = tile.pointPosition(cur);
                    }
                    tile.setsTileReadyListener(new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            refreshLayers = true;
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
        g.setFont(attributionFont);
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
        if(drawMapPointer) {
            g.setColor(0xFF0000);
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int halfSize = 5;
            g.drawRoundRect(centerX - halfSize, centerY - halfSize, 2 * halfSize, 2 * halfSize, halfSize, halfSize);
        }
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
    public void addLayer(Layer layer) {
        addLayer(layer, 0, _map.maxZoomLevel());
    }

    /**
     * Adds a layer to the map
     * @param layer to add
     * @param minZoomLevel min zoom level of this Layer
     * @param maxZoomLevel max zoom level of this Layer
     */
    public void addLayer(Layer layer, int minZoomLevel, int maxZoomLevel) {
        _layers.addElement(new LayerWithZoomLevels(layer, minZoomLevel, maxZoomLevel));
        refreshLayers = true;
        repaint();
    }

    /**
     * Removes a Layer from the map
     * @param layer to remove
     */
    public void removeLayer(Layer layer) {
        int length = _layers.size();
        int no;
        for (no = 0; no < length; no++) {
            if (((LayerWithZoomLevels) _layers.elementAt(no)).layer == layer) {
                break;
            }
        }
        _layers.removeElementAt(no);
        refreshLayers = true;
        repaint();
    }
    
    /**
     * Removes all layers from the map
     */
    public void removeAllLayers() {
        _layers.removeAllElements();
        refreshLayers = true;
        repaint();
    }
    
    /**
     * Returns layers count
     */
    public int getLayersConut(){
        return _layers.size();
    }

    /**
     * Returns Layer at index
     * 
     * @param index the index of the layer
     * @throws ArrayIndexOutOfBoundsException - if the index is out of range 
     * (index < 0 || index >= size())
     */
    public Layer getLayerAt(int index){
        Layer l = ((LayerWithZoomLevels) _layers.elementAt(index)).layer;
        return l;
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
     * 
     * @param boundingBox to zoom to
     * @throws IllegalArgumentException if the boundingBox is not wg84 format
     */
    public void zoomTo(BoundingBox boundingBox) {
        if (boundingBox.projected()) {
            throw new IllegalArgumentException("boundingBox should be wg84 format");
        }

        Dimension dimension = null;
        if (getWidth() == 0 || getHeight() == 0) {
            dimension = getPreferredSize();
        } else {
            dimension = new Dimension(getWidth(), getHeight());
        }
        final BoundingBox projectedBBOX = _map.projection().fromWGS84(boundingBox);
        Tile tile = new Tile(dimension, projectedBBOX, null);
        _zoom = _map.maxZoomFor(tile);
        _center = tile.position(tile.dimension().getWidth() / 2, tile.dimension().getHeight() / 2);
        _needTiles = true;
        repaint();
    }

    /**
     * Zoom map to the center of the given coordinate with the given zoom level
     * 
     * @param coord center map to this coordinate, coord should be in wg84 format
     * @param zoomLevel zoom map to this level;
     * @throws IllegalArgumentException if the coord is not wg84 format
     */
    public void zoomTo(Coord coord, int zoomLevel) {
        if (coord.isProjected()) {
            throw new IllegalArgumentException("coord should be wg84 format");
        }
        _center = _map.projection().fromWGS84(coord);
        _zoom = zoomLevel;
        _needTiles = true;
        repaint();
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
            if(bbox.projected()){
                bbox = _map.projection().toWGS84(bbox);
            }
            zoomTo(bbox);
        }
        _needTiles = true;
    }

    /**
     * Returns the center location of the map in WGS84 format.
     */
    public Coord getCenter() {
        return _map.projection().toWGS84(_center);
    }
   
    /**
     * Returns the current zoom level of the map.
     * 
     * @return zoom level
     */
    public int getZoomLevel(){
        return _zoom;
    }
    
    /**
     * Sets the current zoom level of the map.
     * 
     * @return zoom level
     */
    public void setZoomLevel(int zoom){
        if ( zoom <= getMaxZoomLevel() && zoom >= getMinZoomLevel()) {
            _zoom = zoom;
            _needTiles = true;
            repaint();
        }else{
            System.out.println("zoom level must be bigger then the min zoom "
                    + "level and smaller then the max zoom level");
        }
    }
    
    
    /**
     * Returns the max zoom level of the map
     * 
     * @return max zoom level
     */
    public int getMaxZoomLevel(){
        return _map.maxZoomLevel();
    }
    
    /**
     * Returns the min zoom level of the map
     * 
     * @return min zoom level
     */
    public int getMinZoomLevel(){
        return _map.minZoomLevel();
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
    
    /**
     * Returns the distance between 2 points in meters
     * 
     * @param latitude1
     * @param longitude1
     * @param latitude2
     * @param longitude2
     * 
     * @return distance in meters
     */
    public static long distance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latitudeSin = Math.sin(Math.toRadians(latitude2 - latitude1) / 2);
        double longitudeSin = Math.sin(Math.toRadians(longitude2 - longitude1) / 2);
        double a = latitudeSin * latitudeSin
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * longitudeSin * longitudeSin;
        double c = 2 * MathUtil.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (long) (6378137 * c);
    }    
    
    
    
    private void setLatitude(double latitude) {
        this.latitude = latitude;        
        setCoord(latitude, longitude);
    }

    private void setLongitude(double longitude) {
        this.longitude = longitude;
        setCoord(latitude, longitude);
    }

    private void setCoord(double latitude, double longitude){
        if(Double.isNaN(latitude) && Double.isNaN(longitude)){
            _center =  _map.projection().fromWGS84(new Coord(latitude, longitude));
            _needTiles = true;
            repaint();
        }
    }
        
    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[]{"latitude", "longitude", "zoom"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
        return new Class[]{Double.class, Double.class, Integer.class};
    }
    
    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if (name.equals("latitude")) {
            return new Double(_center.getLatitude());
        }
        if (name.equals("longitude")) {
            return new Double(_center.getLongitude());
        }
        if (name.equals("zoom")) {
            return new Integer(getZoomLevel());
        }
        return null;
    }
        
    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if (name.equals("latitude")) {
            setLatitude(((Double) value).doubleValue());
            return null;
        }
        if (name.equals("longitude")) {
            setLongitude(((Double) value).doubleValue());
            return null;
        }
        if (name.equals("zoom")) {
            setZoomLevel(((Integer) value).intValue());
            return null;
        }
        return super.setPropertyValue(name, value);
    }
    
    
    
}
class LayerWithZoomLevels {

    public Layer layer;
    public int minZoomLevel;
    public int maxZoomLevel;

    public LayerWithZoomLevels(Layer l, int min, int max) {
        layer = l;
        minZoomLevel = min;
        maxZoomLevel = max;
    }
}
