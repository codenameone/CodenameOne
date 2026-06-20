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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
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
        engine.setRepaintCallback(new Runnable() {
            public void run() {
                repaint();
            }
        });
        setFocusable(true);
        getAllStyles().setBgTransparency(255);
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
    public CameraPosition getCameraPosition() {
        return new CameraPosition(engine.getCenter(), engine.getZoom());
    }

    /// {@inheritDoc}
    public void setCameraPosition(CameraPosition position) {
        engine.setCenter(position.getTarget());
        engine.setZoom(position.getZoom());
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    public void moveCamera(LatLng target, double zoom) {
        engine.setCenter(target);
        engine.setZoom(zoom);
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    public double getZoom() {
        return engine.getZoom();
    }

    /// {@inheritDoc}
    public void setZoom(double zoom) {
        engine.setZoom(zoom);
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    public double getMinZoom() {
        return engine.getMinZoom();
    }

    /// {@inheritDoc}
    public double getMaxZoom() {
        return engine.getMaxZoom();
    }

    /// {@inheritDoc}
    public LatLng getCenter() {
        return engine.getCenter();
    }

    /// {@inheritDoc}
    public void setCenter(LatLng center) {
        engine.setCenter(center);
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    public MapBounds getVisibleRegion() {
        return engine.getVisibleBounds();
    }

    /// {@inheritDoc}
    public void fitBounds(MapBounds bounds, int paddingPixels) {
        engine.setViewport(getWidth(), getHeight());
        engine.fitBounds(bounds, paddingPixels);
        repaint();
        fireCameraChanged();
    }

    // ---- MapSurface: map objects -----------------------------------------

    /// {@inheritDoc}
    public Marker addMarker(MarkerOptions options) {
        Marker m = options.build();
        markers.add(m);
        repaint();
        return m;
    }

    /// {@inheritDoc}
    public void removeMarker(Marker marker) {
        markers.remove(marker);
        repaint();
    }

    /// {@inheritDoc}
    public Polyline addPolyline(Polyline polyline) {
        polylines.add(polyline);
        repaint();
        return polyline;
    }

    /// {@inheritDoc}
    public void removePolyline(Polyline polyline) {
        polylines.remove(polyline);
        repaint();
    }

    /// {@inheritDoc}
    public Polygon addPolygon(Polygon polygon) {
        polygons.add(polygon);
        repaint();
        return polygon;
    }

    /// {@inheritDoc}
    public void removePolygon(Polygon polygon) {
        polygons.remove(polygon);
        repaint();
    }

    /// {@inheritDoc}
    public Circle addCircle(Circle circle) {
        circles.add(circle);
        repaint();
        return circle;
    }

    /// {@inheritDoc}
    public void removeCircle(Circle circle) {
        circles.remove(circle);
        repaint();
    }

    /// {@inheritDoc}
    public void clearMapObjects() {
        markers.clear();
        polylines.clear();
        polygons.clear();
        circles.clear();
        repaint();
    }

    // ---- MapSurface: conversion + listeners ------------------------------

    /// {@inheritDoc}
    public Point latLngToScreen(LatLng coord) {
        engine.setViewport(getWidth(), getHeight());
        return engine.latLngToScreen(coord);
    }

    /// {@inheritDoc}
    public LatLng screenToLatLng(int x, int y) {
        engine.setViewport(getWidth(), getHeight());
        return engine.screenToLatLng(x, y);
    }

    /// {@inheritDoc}
    public void addTapListener(MapTapListener l) {
        tapListeners.add(l);
    }

    /// {@inheritDoc}
    public void removeTapListener(MapTapListener l) {
        tapListeners.remove(l);
    }

    /// {@inheritDoc}
    public void addLongPressListener(MapTapListener l) {
        longPressListeners.add(l);
    }

    /// {@inheritDoc}
    public void removeLongPressListener(MapTapListener l) {
        longPressListeners.remove(l);
    }

    /// {@inheritDoc}
    public void addCameraChangeListener(CameraChangeListener l) {
        cameraListeners.add(l);
    }

    /// {@inheritDoc}
    public void removeCameraChangeListener(CameraChangeListener l) {
        cameraListeners.remove(l);
    }

    /// {@inheritDoc}
    public boolean isNativeMap() {
        return false;
    }

    /// {@inheritDoc}
    public Component asComponent() {
        return this;
    }

    // ---- Painting --------------------------------------------------------

    protected void paintBackground(Graphics g) {
        engine.setViewport(getWidth(), getHeight());
        g.translate(getX(), getY());
        engine.paint(g, 0, 0, getWidth(), getHeight());
        drawOverlays(g);
        g.translate(-getX(), -getY());
    }

    private void drawOverlays(Graphics g) {
        g.setAntiAliased(true);
        for (int i = 0; i < polygons.size(); i++) {
            drawPolygon(g, (Polygon) polygons.get(i));
        }
        for (int i = 0; i < circles.size(); i++) {
            drawCircle(g, (Circle) circles.get(i));
        }
        for (int i = 0; i < polylines.size(); i++) {
            drawPolyline(g, (Polyline) polylines.get(i));
        }
        for (int i = 0; i < markers.size(); i++) {
            drawMarker(g, (Marker) markers.get(i));
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
        } else {
            int r = 7;
            g.setColor(0xffffff);
            g.setAlpha(255);
            g.fillArc(p.getX() - r - 1, p.getY() - r - 1, (r + 1) * 2, (r + 1) * 2, 0, 360);
            g.setColor(0xe53935);
            g.fillArc(p.getX() - r, p.getY() - r, r * 2, r * 2, 0, 360);
        }
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
    public void pointerPressed(int x, int y) {
        lastX = x;
        lastY = y;
        dragDistance = 0;
    }

    /// {@inheritDoc}
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
    public void longPointerPress(int x, int y) {
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        LatLng geo = engine.screenToLatLng(lx, ly);
        for (int i = 0; i < longPressListeners.size(); i++) {
            ((MapTapListener) longPressListeners.get(i)).mapTapped(this, geo, lx, ly);
        }
    }

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
        for (int i = 0; i < tapListeners.size(); i++) {
            ((MapTapListener) tapListeners.get(i)).mapTapped(this, geo, lx, ly);
        }
    }

    private void fireCameraChanged() {
        if (cameraListeners.isEmpty()) {
            return;
        }
        CameraPosition pos = getCameraPosition();
        for (int i = 0; i < cameraListeners.size(); i++) {
            ((CameraChangeListener) cameraListeners.get(i)).cameraChanged(this, pos);
        }
    }

    protected com.codename1.ui.geom.Dimension calcPreferredSize() {
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        return new com.codename1.ui.geom.Dimension(w, h);
    }
}
