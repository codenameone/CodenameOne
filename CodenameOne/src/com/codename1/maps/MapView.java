/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps;

import com.codename1.maps.vector.MapStyle;
import com.codename1.maps.vector.MvtTileSource;
import com.codename1.maps.vector.TileSource;
import com.codename1.maps.vector.VectorMapEngine;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Point;
import com.codename1.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

/// A pure-vector map component: it renders entirely through the Codename One
/// [Graphics] API (the built-in [VectorMapEngine]) and never embeds a native
/// peer, so it composes cleanly with the rest of the UI -- dialogs, lists and
/// overlays draw over it without the clipping limitations of a native view.
///
/// `MapView` works identically on every platform including the simulator and
/// the web. By default it shows the free, keyless **OpenFreeMap** vector
/// basemap (real OpenStreetMap data) so it renders real maps with zero
/// configuration and no API key; point it at any other
/// [com.codename1.maps.vector.TileSource] (a keyed MVT endpoint, a raster
/// source such as [com.codename1.maps.vector.RasterTileSource#openStreetMap()],
/// or a bundled offline tileset) as needed. For a native-rendered map (Apple
/// MapKit, Google Maps, ...) use [NativeMap], which falls back to this
/// component when no native provider is wired in.
public class MapView extends Container implements MapSurface {

    private final VectorMapEngine engine;
    private Font markerFont;

    private final List markers = new ArrayList();
    private final List polylines = new ArrayList();
    private final List polygons = new ArrayList();
    private final List circles = new ArrayList();

    private final List tapListeners = new ArrayList();
    private final List longPressListeners = new ArrayList();
    private final List cameraListeners = new ArrayList();

    private int lastX;
    private int lastY;
    private int dragDistance;
    private boolean pinching;
    private double pinchStartZoom;
    private long lastTapTime;
    private int lastTapX;
    private int lastTapY;

    /// Creates a map showing the free, keyless OpenFreeMap vector basemap (real
    /// OpenStreetMap data) centered on the equator at a low zoom.
    public MapView() {
        this(MvtTileSource.openFreeMap(), MapStyle.light());
    }

    /// Creates a map backed by `source` with the default light style.
    public MapView(TileSource source) {
        this(source, MapStyle.light());
    }

    /// Creates a map backed by `source` and styled by `style` (the style is
    /// only consulted for vector sources).
    public MapView(TileSource source, MapStyle style) {
        engine = new VectorMapEngine(source, style);
        engine.setCenter(new LatLng(0, 0));
        engine.setZoom(2);
        engine.setPixelRatio(devicePixelRatio());
        engine.setRepaintCallback(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
        setFocusable(true);
        getAllStyles().setBgTransparency(255);
    }

    /// The device pixel ratio used to scale tile rendering, derived from the
    /// display density. A z-level tile is 256 logical pixels; on a high-density
    /// screen it must cover proportionally more physical pixels (the standard
    /// slippy-map convention: mdpi = 1, hdpi = 1.5, xhdpi = 2, xxhdpi = 3, ...)
    /// otherwise the viewport spans many tiles and the map shows too much area.
    private static double devicePixelRatio() {
        switch (Display.getInstance().getDeviceDensity()) {
            case Display.DENSITY_HIGH:
                return 1.5;
            case Display.DENSITY_VERY_HIGH:
                return 2.0;
            case Display.DENSITY_HD:
                return 3.0;
            case Display.DENSITY_560:
                return 3.5;
            case Display.DENSITY_2HD:
                return 4.0;
            case Display.DENSITY_4K:
                return 4.0;
            default:
                // very-low / low / medium / desktop -> no scaling.
                return 1.0;
        }
    }

    /// {@inheritDoc}
    @Override
    public boolean isLoadingTiles() {
        return engine.hasPendingTiles();
    }

    /// The underlying vector engine, for advanced configuration (tile cache,
    /// source and style swapping).
    public VectorMapEngine getEngine() {
        return engine;
    }

    /// Replaces the tile source.
    public MapView setTileSource(TileSource source) {
        engine.setSource(source);
        repaint();
        return this;
    }

    /// Replaces the style.
    public MapView setStyle(MapStyle style) {
        engine.setStyle(style);
        repaint();
        return this;
    }

    // ---- MapSurface: camera ----------------------------------------------

    /// {@inheritDoc}
    @Override
    public CameraPosition getCameraPosition() {
        return new CameraPosition(engine.getCenter(), engine.getZoom());
    }

    /// {@inheritDoc}
    @Override
    public void setCameraPosition(CameraPosition position) {
        engine.setCenter(position.getTarget());
        engine.setZoom(position.getZoom());
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    @Override
    public void moveCamera(LatLng target, double zoom) {
        engine.setCenter(target);
        engine.setZoom(zoom);
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    @Override
    public double getZoom() {
        return engine.getZoom();
    }

    /// {@inheritDoc}
    @Override
    public void setZoom(double zoom) {
        engine.setZoom(zoom);
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    @Override
    public double getMinZoom() {
        return engine.getMinZoom();
    }

    /// {@inheritDoc}
    @Override
    public double getMaxZoom() {
        return engine.getMaxZoom();
    }

    /// {@inheritDoc}
    @Override
    public LatLng getCenter() {
        return engine.getCenter();
    }

    /// {@inheritDoc}
    @Override
    public void setCenter(LatLng center) {
        engine.setCenter(center);
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    @Override
    public MapBounds getVisibleRegion() {
        return engine.getVisibleBounds();
    }

    /// {@inheritDoc}
    @Override
    public void fitBounds(MapBounds bounds, int paddingPixels) {
        engine.setViewport(getWidth(), getHeight());
        engine.fitBounds(bounds, paddingPixels);
        repaint();
        fireCameraChanged();
    }

    // ---- MapSurface: map objects -----------------------------------------

    /// {@inheritDoc}
    @Override
    public Marker addMarker(MarkerOptions options) {
        Marker m = options.build();
        markers.add(m);
        repaint();
        return m;
    }

    /// {@inheritDoc}
    @Override
    public void removeMarker(Marker marker) {
        markers.remove(marker);
        repaint();
    }

    /// {@inheritDoc}
    @Override
    public Polyline addPolyline(Polyline polyline) {
        polylines.add(polyline);
        repaint();
        return polyline;
    }

    /// {@inheritDoc}
    @Override
    public void removePolyline(Polyline polyline) {
        polylines.remove(polyline);
        repaint();
    }

    /// {@inheritDoc}
    @Override
    public Polygon addPolygon(Polygon polygon) {
        polygons.add(polygon);
        repaint();
        return polygon;
    }

    /// {@inheritDoc}
    @Override
    public void removePolygon(Polygon polygon) {
        polygons.remove(polygon);
        repaint();
    }

    /// {@inheritDoc}
    @Override
    public Circle addCircle(Circle circle) {
        circles.add(circle);
        repaint();
        return circle;
    }

    /// {@inheritDoc}
    @Override
    public void removeCircle(Circle circle) {
        circles.remove(circle);
        repaint();
    }

    /// {@inheritDoc}
    @Override
    public void clearMapObjects() {
        markers.clear();
        polylines.clear();
        polygons.clear();
        circles.clear();
        repaint();
    }

    // ---- MapSurface: conversion + listeners ------------------------------

    /// {@inheritDoc}
    @Override
    public Point latLngToScreen(LatLng coord) {
        engine.setViewport(getWidth(), getHeight());
        return engine.latLngToScreen(coord);
    }

    /// {@inheritDoc}
    @Override
    public LatLng screenToLatLng(int x, int y) {
        engine.setViewport(getWidth(), getHeight());
        return engine.screenToLatLng(x, y);
    }

    /// {@inheritDoc}
    @Override
    public void addTapListener(MapTapListener l) {
        tapListeners.add(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeTapListener(MapTapListener l) {
        tapListeners.remove(l);
    }

    /// {@inheritDoc}
    @Override
    public void addLongPressListener(MapTapListener l) {
        longPressListeners.add(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeLongPressListener(MapTapListener l) {
        longPressListeners.remove(l);
    }

    /// {@inheritDoc}
    @Override
    public void addCameraChangeListener(CameraChangeListener l) {
        cameraListeners.add(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeCameraChangeListener(CameraChangeListener l) {
        cameraListeners.remove(l);
    }

    /// {@inheritDoc}
    @Override
    public boolean isNativeMap() {
        return false;
    }

    /// {@inheritDoc}
    @Override
    public Component asComponent() {
        return this;
    }

    // ---- Painting --------------------------------------------------------

    @Override
    protected void paintBackground(Graphics g) {
        engine.setViewport(getWidth(), getHeight());
        g.translate(getX(), getY());
        engine.paint(g, 0, 0, getWidth(), getHeight());
        drawOverlays(g);
        g.translate(-getX(), -getY());
    }

    private void drawOverlays(Graphics g) {
        g.setAntiAliased(true);
        for (Object polygonObj : polygons) {
            drawPolygon(g, (Polygon) polygonObj);
        }
        for (Object circleObj : circles) {
            drawCircle(g, (Circle) circleObj);
        }
        for (Object polylineObj : polylines) {
            drawPolyline(g, (Polyline) polylineObj);
        }
        for (Object markerObj : markers) {
            drawMarker(g, (Marker) markerObj);
        }
    }

    private void drawPolyline(Graphics g, Polyline pl) {
        if (!pl.isVisible() || pl.getPoints().size() < 2) {
            return;
        }
        GeneralPath path = buildPath(pl.getPoints(), false);
        g.setColor(pl.getStrokeColor());
        g.setAlpha(pl.getStrokeAlpha());
        g.drawShape(path, new Stroke(pl.getStrokeWidth(), Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4f));
        g.setAlpha(255);
    }

    private void drawPolygon(Graphics g, Polygon pg) {
        if (!pg.isVisible() || pg.getPoints().size() < 3) {
            return;
        }
        GeneralPath path = buildPath(pg.getPoints(), true);
        int fill = pg.getFillColor();
        int fa = (fill >>> 24) & 0xff;
        g.setColor(fill & 0xffffff);
        g.setAlpha(fa == 0 ? 255 : fa);
        g.fillShape(path);
        if (pg.getStrokeWidth() > 0) {
            g.setColor(pg.getStrokeColor());
            g.setAlpha(255);
            g.drawShape(path, new Stroke(pg.getStrokeWidth(), Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4f));
        }
        g.setAlpha(255);
    }

    private void drawCircle(Graphics g, Circle c) {
        if (!c.isVisible()) {
            return;
        }
        Point center = engine.latLngToScreen(c.getCenter());
        LatLng north = new LatLng(c.getCenter().getLatitude() + c.getRadiusMeters() / 111320.0,
                c.getCenter().getLongitude());
        Point np = engine.latLngToScreen(north);
        int r = (int) Math.abs(center.getY() - np.getY());
        if (r < 1) {
            r = 1;
        }
        int fill = c.getFillColor();
        int fa = (fill >>> 24) & 0xff;
        g.setColor(fill & 0xffffff);
        g.setAlpha(fa == 0 ? 255 : fa);
        g.fillArc(center.getX() - r, center.getY() - r, r * 2, r * 2, 0, 360);
        if (c.getStrokeWidth() > 0) {
            g.setColor(c.getStrokeColor());
            g.setAlpha(255);
            g.drawArc(center.getX() - r, center.getY() - r, r * 2, r * 2, 0, 360);
        }
        g.setAlpha(255);
    }

    private void drawMarker(Graphics g, Marker m) {
        if (!m.isVisible()) {
            return;
        }
        Point p = engine.latLngToScreen(m.getPosition());
        EncodedImage icon = m.getIcon();
        if (icon != null) {
            int w = icon.getWidth();
            int h = icon.getHeight();
            int dx = p.getX() - (int) (w * m.getAnchorU());
            int dy = p.getY() - (int) (h * m.getAnchorV());
            g.drawImage(icon, dx, dy);
            return;
        }
        // Default marker: the standard Material Design map pin glyph, anchored
        // at the marker's tip (anchor defaults to 0.5, 1.0 -- bottom center).
        Font pin = markerFont();
        String glyph = String.valueOf(FontImage.MATERIAL_PLACE);
        int gw = pin.stringWidth(glyph);
        int gh = pin.getHeight();
        int gx = p.getX() - (int) (gw * m.getAnchorU());
        int gy = p.getY() - (int) (gh * m.getAnchorV());
        int prevAlpha = g.getAlpha();
        g.setFont(pin);
        g.setAlpha(255);
        g.setColor(0x8e1c16);
        g.drawString(glyph, gx - 1, gy);
        g.drawString(glyph, gx + 1, gy);
        g.drawString(glyph, gx, gy - 1);
        g.drawString(glyph, gx, gy + 1);
        g.setColor(0xe53935);
        g.drawString(glyph, gx, gy);
        g.setAlpha(prevAlpha);
    }

    private Font markerFont() {
        if (markerFont == null) {
            float size = CN.convertToPixels(7f);
            if (size < 24) {
                size = 24;
            }
            markerFont = FontImage.getMaterialDesignFont().derive(size, Font.STYLE_PLAIN);
        }
        return markerFont;
    }

    private GeneralPath buildPath(List points, boolean close) {
        GeneralPath path = new GeneralPath();
        for (int i = 0; i < points.size(); i++) {
            Point sp = engine.latLngToScreen((LatLng) points.get(i));
            if (i == 0) {
                path.moveTo(sp.getX(), sp.getY());
            } else {
                path.lineTo(sp.getX(), sp.getY());
            }
        }
        if (close) {
            path.closePath();
        }
        return path;
    }

    // ---- Gestures --------------------------------------------------------

    /// {@inheritDoc}
    @Override
    public void pointerPressed(int x, int y) {
        lastX = x;
        lastY = y;
        dragDistance = 0;
    }

    /// {@inheritDoc}
    @Override
    public void pointerDragged(int x, int y) {
        int dx = x - lastX;
        int dy = y - lastY;
        lastX = x;
        lastY = y;
        dragDistance += Math.abs(dx) + Math.abs(dy);
        engine.panPixels(dx, dy);
        repaint();
    }

    /// {@inheritDoc}
    @Override
    public void pointerReleased(int x, int y) {
        if (pinching) {
            pinching = false;
            fireCameraChanged();
            return;
        }
        if (dragDistance < 10) {
            int lx = x - getAbsoluteX();
            int ly = y - getAbsoluteY();
            long now = System.currentTimeMillis();
            if (now - lastTapTime < 300 && Math.abs(x - lastTapX) < 30 && Math.abs(y - lastTapY) < 30) {
                lastTapTime = 0;
                engine.zoomAround(engine.getZoom() + 1, lx, ly);
                repaint();
                fireCameraChanged();
            } else {
                lastTapTime = now;
                lastTapX = x;
                lastTapY = y;
                handleTap(lx, ly);
            }
        } else {
            fireCameraChanged();
        }
    }

    /// {@inheritDoc}
    @Override
    public void longPointerPress(int x, int y) {
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        LatLng geo = engine.screenToLatLng(lx, ly);
        for (Object lpListener : longPressListeners) {
            ((MapTapListener) lpListener).mapTapped(this, geo, lx, ly);
        }
    }

    @Override
    protected boolean pinch(float scale) {
        if (!pinching) {
            pinching = true;
            pinchStartZoom = engine.getZoom();
        }
        double nz = pinchStartZoom + MathUtil.log(scale) / MathUtil.log(2);
        engine.zoomAround(nz, getWidth() / 2, getHeight() / 2);
        repaint();
        return true;
    }

    private void handleTap(int lx, int ly) {
        // Hit-test markers first (top-most wins).
        for (int i = markers.size() - 1; i >= 0; i--) {
            Marker m = (Marker) markers.get(i);
            if (!m.isVisible() || m.getOnClick() == null) {
                continue;
            }
            Point p = engine.latLngToScreen(m.getPosition());
            int w = m.getIcon() != null ? m.getIcon().getWidth() : 16;
            int h = m.getIcon() != null ? m.getIcon().getHeight() : 16;
            int left = p.getX() - (int) (w * m.getAnchorU());
            int top = p.getY() - (int) (h * m.getAnchorV());
            if (lx >= left && lx <= left + w && ly >= top && ly <= top + h) {
                m.getOnClick().actionPerformed(new ActionEvent(m, lx, ly));
                return;
            }
        }
        LatLng geo = engine.screenToLatLng(lx, ly);
        for (Object tapListener : tapListeners) {
            ((MapTapListener) tapListener).mapTapped(this, geo, lx, ly);
        }
    }

    private void fireCameraChanged() {
        if (cameraListeners.isEmpty()) {
            return;
        }
        CameraPosition pos = getCameraPosition();
        for (Object camListener : cameraListeners) {
            ((CameraChangeListener) camListener).cameraChanged(this, pos);
        }
    }

    @Override
    protected com.codename1.ui.geom.Dimension calcPreferredSize() {
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        return new com.codename1.ui.geom.Dimension(w, h);
    }
}
