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
import com.codename1.ui.Form;
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

    // How long a thrown map takes to lose all but a third of its speed, and how
    // long a zoom takes to close all but a third of the way to where it is
    // going. Both motions are decays rather than fixed-length tweens, so one
    // that is added to while it runs -- a second notch of the wheel -- simply
    // carries on from where it is.
    private static final double GLIDE_MILLIS = 325;
    private static final double ZOOM_MILLIS = 90;
    // The longest step one frame may take. A frame that was held up must not
    // then throw the map across the gap in one move.
    private static final int LONGEST_STEP_MILLIS = 100;
    // The part of a drag its release is judged by.
    private static final int THROW_WINDOW_MILLIS = 120;
    // A finger that rested this long before lifting put the map down.
    private static final int THROW_REST_MILLIS = 80;
    private static final int SAMPLES = 8;

    private final long[] sampleTime = new long[SAMPLES];
    private final int[] sampleX = new int[SAMPLES];
    private final int[] sampleY = new int[SAMPLES];
    private int sampleCount;
    // Pixels a millisecond.
    private double glideX;
    private double glideY;
    private boolean gliding;
    private boolean zooming;
    private double zoomTarget;
    private int zoomFocusX;
    private int zoomFocusY;
    private long lastStep;
    private boolean moving;
    private boolean wheelZoom;
    // Where the pointer was last seen over the map, for a gesture that says
    // how much to zoom and not where.
    private int pointerX;
    private int pointerY;
    private boolean pointerSeen;
    private int pinchX;
    private int pinchY;
    private boolean pinchPlaced;
    private int pinchLastX;
    private int pinchLastY;

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
    static double devicePixelRatio() {
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

    /// Whether every tile visible in the current viewport has finished
    /// loading/decoding/rendering. Unlike [#isLoadingTiles()], this actively
    /// computes the visible tile set and requests missing tiles, so it is a
    /// deterministic readiness probe even before the first paint has run.
    public boolean isMapReady() {
        engine.setViewport(getWidth(), getHeight());
        return engine.hasRenderedVisibleTiles();
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
        settle();
        engine.setCenter(position.getTarget());
        engine.setZoom(position.getZoom());
        repaint();
        fireCameraChanged();
    }

    /// {@inheritDoc}
    @Override
    public void moveCamera(LatLng target, double zoom) {
        settle();
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
        settle();
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
        settle();
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
        settle();
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
        engine.paintTiles(g, 0, 0, getWidth(), getHeight());
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
        // Street and place names go over shapes, so a route does not hide the
        // names of the roads it follows, and under the pins.
        engine.paintLabels(g);
        for (Object markerObj : markers) {
            drawMarker(g, (Marker) markerObj);
        }
        // Draw labels last so routes and later pins cannot paint over them.
        List labelBounds = new ArrayList();
        for (Object markerObj : markers) {
            drawMarkerLabel(g, (Marker) markerObj, labelBounds);
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

    private void drawMarkerLabel(Graphics g, Marker marker, List occupied) {
        String label = marker.getLabel();
        if (!marker.isVisible() || label == null || label.trim().length() == 0) {
            return;
        }
        Point point = engine.latLngToScreen(marker.getPosition());
        int px = point.getX();
        int py = point.getY();
        if (px < 0 || py < 0 || px > getWidth() || py > getHeight()) {
            return;
        }
        Font font = Font.getDefaultFont();
        int padding = Math.max(2, CN.convertToPixels(1));
        int available = getWidth() - padding * 4;
        if (available <= 0 || getHeight() < font.getHeight() + padding * 2) {
            return;
        }
        // Keep long place names inside the viewport without changing the model.
        if (font.stringWidth(label) > available) {
            String ellipsis = "...";
            int end = label.length();
            while (end > 0 && font.stringWidth(label.substring(0, end) + ellipsis) > available) {
                end--;
            }
            if (end == 0) {
                return;
            }
            label = label.substring(0, end) + ellipsis;
        }
        int width = font.stringWidth(label) + padding * 2;
        int height = font.getHeight() + padding * 2;
        int x = Math.max(0, Math.min(px - width / 2, getWidth() - width));
        // Prefer below the pin's tip, away from its icon; flip at the bottom edge.
        int y = py + padding;
        if (y + height > getHeight()) {
            int iconHeight = marker.getIcon() == null ? markerFont().getHeight() : marker.getIcon().getHeight();
            y = Math.max(0, py - (int) (iconHeight * marker.getAnchorV()) - height - padding);
        }
        // Nearby stops (including a round trip's start and destination) must
        // not paint their names on top of each other. Try rows below/above.
        int preferredY = y;
        boolean placed = false;
        for (int offset = 0; offset < getHeight(); offset += height + padding) {
            if (markerLabelFits(x, preferredY + offset, width, height, occupied)) {
                y = preferredY + offset;
                placed = true;
                break;
            }
            if (offset > 0 && markerLabelFits(x, preferredY - offset, width, height, occupied)) {
                y = preferredY - offset;
                placed = true;
                break;
            }
        }
        if (!placed) {
            return;
        }
        occupied.add(new int[]{x, y, width, height});
        Font previousFont = g.getFont();
        int previousAlpha = g.getAlpha();
        int previousColor = g.getColor();
        g.setAlpha(255);
        g.setColor(0xffffff);
        g.fillRoundRect(x, y, width, height, padding * 2, padding * 2);
        g.setFont(font);
        g.setColor(0x222222);
        g.drawString(label, x + padding, y + padding);
        g.setFont(previousFont);
        g.setAlpha(previousAlpha);
        g.setColor(previousColor);
    }

    private boolean markerLabelFits(int x, int y, int width, int height, List occupied) {
        if (y < 0 || y + height > getHeight()) {
            return false;
        }
        for (Object item : occupied) {
            int[] box = (int[]) item;
            if (x < box[0] + box[2] && x + width > box[0]
                    && y < box[1] + box[3] && y + height > box[1]) {
                return false;
            }
        }
        return true;
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
        // A finger put down on a moving map stops it there.
        boolean wasMoving = gliding || zooming;
        settle();
        if (wasMoving) {
            fireCameraChanged();
        }
        lastX = x;
        lastY = y;
        dragDistance = 0;
        sampleCount = 0;
        sample(x, y);
        seen(x, y);
    }

    /// Whether the wheel zooms the map about the pointer instead of panning it.
    public boolean isWheelZoom() {
        return wheelZoom;
    }

    /// Makes the wheel zoom the map about the pointer, the way a map that fills
    /// its window is expected to answer, instead of panning it.
    ///
    /// Panning is the default because a map is as often one item on a page
    /// that scrolls: there the wheel belongs to the page, and a map that took
    /// it to zoom would stop the page under the pointer wherever the two met.
    /// A wheel at the end of the zoom range is passed on either way.
    ///
    /// #### Parameters
    ///
    /// - `wheelZoom`: true to zoom with the wheel, false to pan with it
    public void setWheelZoom(boolean wheelZoom) {
        this.wheelZoom = wheelZoom;
    }

    /// {@inheritDoc}
    @Override
    public void pointerHover(int[] x, int[] y) {
        if (x.length > 0) {
            seen(x[0], y[0]);
        }
        super.pointerHover(x, y);
    }

    private void seen(int x, int y) {
        pointerX = x - getAbsoluteX();
        pointerY = y - getAbsoluteY();
        pointerSeen = true;
    }

    private void sample(int x, int y) {
        if (sampleCount == SAMPLES) {
            System.arraycopy(sampleTime, 1, sampleTime, 0, SAMPLES - 1);
            System.arraycopy(sampleX, 1, sampleX, 0, SAMPLES - 1);
            System.arraycopy(sampleY, 1, sampleY, 0, SAMPLES - 1);
            sampleCount--;
        }
        sampleTime[sampleCount] = System.currentTimeMillis();
        sampleX[sampleCount] = x;
        sampleY[sampleCount] = y;
        sampleCount++;
    }

    // ---- Motion that outlasts the gesture --------------------------------

    // Stops the map where it is. Whoever moves the camera next -- a finger, or
    // the application -- is not to be argued with by a glide still running.
    private void settle() {
        gliding = false;
        zooming = false;
    }

    private void move() {
        lastStep = System.currentTimeMillis();
        if (!moving) {
            Form form = getComponentForm();
            if (form != null) {
                form.registerAnimated(this);
                moving = true;
            }
        }
        repaint();
    }

    // Starts, or redirects, a zoom towards `target` that keeps the point under
    // the given pixel where it is.
    private void zoomTowards(double target, int focusX, int focusY) {
        gliding = false;
        zoomTarget = Math.max(engine.getMinZoom(), Math.min(engine.getMaxZoom(), target));
        zoomFocusX = focusX;
        zoomFocusY = focusY;
        zooming = true;
        move();
    }

    /// {@inheritDoc}
    @Override
    public boolean animate() {
        boolean other = super.animate();
        if (!gliding && !zooming) {
            if (moving) {
                moving = false;
                Form form = getComponentForm();
                if (form != null) {
                    form.deregisterAnimated(this);
                }
            }
            return other;
        }
        long now = System.currentTimeMillis();
        long elapsed = Math.min(LONGEST_STEP_MILLIS, now - lastStep);
        if (elapsed <= 0) {
            return other;
        }
        lastStep = now;
        if (zooming) {
            double zoom = engine.getZoom();
            double left = zoomTarget - zoom;
            double next = zoomTarget - left * MathUtil.exp(-elapsed / ZOOM_MILLIS);
            if (Math.abs(zoomTarget - next) < 0.004) {
                next = zoomTarget;
                zooming = false;
            }
            engine.zoomAround(next, zoomFocusX, zoomFocusY);
        }
        if (gliding) {
            double keep = MathUtil.exp(-elapsed / GLIDE_MILLIS);
            // The distance covered while the speed fell from what it was to
            // what it is, not the speed at either end times the time.
            double reach = GLIDE_MILLIS * (1 - keep);
            LatLng before = engine.getCenter();
            engine.panPixels(glideX * reach, glideY * reach);
            glideX *= keep;
            glideY *= keep;
            LatLng after = engine.getCenter();
            boolean stuck = Double.compare(before.getLatitude(), after.getLatitude()) == 0
                    && Double.compare(before.getLongitude(), after.getLongitude()) == 0;
            if (stuck || glideX * glideX + glideY * glideY < slowest() * slowest()) {
                gliding = false;
            }
        }
        if (!gliding && !zooming) {
            // The camera has come to rest: this is the end of the gesture that
            // set it going, which is when the listeners hear of a drag too.
            fireCameraChanged();
        }
        return true;
    }

    // The speed, in pixels a millisecond, under which a glide is over: about
    // one logical pixel a frame.
    private double slowest() {
        return 0.03 * engine.getPixelRatio();
    }

    /// {@inheritDoc}
    @Override
    protected void deinitialize() {
        settle();
        moving = false;
        super.deinitialize();
    }

    /// {@inheritDoc}
    ///
    /// A wheel pans the camera, and tells the listeners about it -- which is the half a
    /// synthetic drag could never deliver, because the gesture it emulated had no end that
    /// meant "the pan is finished".
    @Override
    protected boolean mouseWheel(com.codename1.ui.events.WheelEvent ev) {
        if (ev.getDeltaX() == 0 && ev.getDeltaY() == 0) {
            return false;
        }
        seen(ev.getX(), ev.getY());
        if (wheelZoom) {
            if (ev.getDeltaY() == 0) {
                return false;
            }
            // Two hundred logical pixels of wheel to a zoom level: half a level
            // for a notch of a mouse wheel, and a trackpad, which reports a few
            // pixels at a time, zooms as far as the fingers travel.
            double from = zooming ? zoomTarget : engine.getZoom();
            double target = from + ev.getDeltaY() / (200 * engine.getPixelRatio());
            target = Math.max(engine.getMinZoom(), Math.min(engine.getMaxZoom(), target));
            if (Double.compare(target, from) == 0) {
                // At the end of the range there is nothing to do with it, and
                // what cannot move passes the wheel on.
                return false;
            }
            zoomTowards(target, pointerX, pointerY);
            return true;
        }
        settle();
        // Web Mercator stops at its latitude limits, and panPixels clamps the centre back
        // to where it already was. Claiming the wheel for a pan that did not happen traps
        // scrolling on a page that holds a map: what cannot move passes the wheel on, the
        // same rule the scrollers and the image viewer follow.
        //
        // Judged on the axis with the larger delta, and only that one. A trackpad swipe
        // always carries a little of the other axis, so a map held against the top of Web
        // Mercator still drifts east or west under a downward swipe -- and counting that as
        // a pan means the page under the map stops scrolling at exactly the latitude a
        // reader is most likely to be at. The whole gesture goes or stays, so a centre that
        // moved only on the subordinate axis is put back.
        boolean verticalGesture = Math.abs(ev.getDeltaY()) >= Math.abs(ev.getDeltaX());
        LatLng before = engine.getCenter();
        engine.panPixels(ev.getDeltaX(), ev.getDeltaY());
        LatLng after = engine.getCenter();
        boolean moved = verticalGesture
                ? Double.compare(before.getLatitude(), after.getLatitude()) != 0
                : Double.compare(before.getLongitude(), after.getLongitude()) != 0;
        if (!moved) {
            engine.setCenter(before);
            return false;
        }
        repaint();
        fireCameraChanged();
        return true;
    }

    /// {@inheritDoc}
    @Override
    public void pointerDragged(int x, int y) {
        int dx = x - lastX;
        int dy = y - lastY;
        lastX = x;
        lastY = y;
        dragDistance += Math.abs(dx) + Math.abs(dy);
        sample(x, y);
        seen(x, y);
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
                // Glided to rather than jumped to: a map that is suddenly a
                // different map leaves the eye to find its place again. The
                // listeners hear when it gets there.
                zoomTowards(engine.getZoom() + 1, lx, ly);
            } else {
                lastTapTime = now;
                lastTapX = x;
                lastTapY = y;
                handleTap(lx, ly);
            }
        } else if (!thrown()) {
            fireCameraChanged();
        }
    }

    // Lets a map that was moving when it was let go carry on, slowing: a drag
    // that stops dead under a lifting finger is the one thing about a map that
    // feels like software. The speed is that of the last tenth of a second, so
    // a drag that ended slowly is not thrown by how it began.
    private boolean thrown() {
        if (sampleCount < 2) {
            return false;
        }
        int last = sampleCount - 1;
        long now = System.currentTimeMillis();
        if (now - sampleTime[last] > THROW_REST_MILLIS) {
            return false;
        }
        int first = last;
        while (first > 0 && sampleTime[last] - sampleTime[first - 1] <= THROW_WINDOW_MILLIS) {
            first--;
        }
        long took = sampleTime[last] - sampleTime[first];
        if (took <= 0) {
            return false;
        }
        double vx = (sampleX[last] - sampleX[first]) / (double) took;
        double vy = (sampleY[last] - sampleY[first]) / (double) took;
        // Slower than a sixth of a logical pixel a millisecond is a map being
        // put down, not thrown.
        double least = 0.16 * engine.getPixelRatio();
        if (vx * vx + vy * vy < least * least) {
            return false;
        }
        glideX = vx;
        glideY = vy;
        gliding = true;
        move();
        return true;
    }

    /// {@inheritDoc}
    ///
    /// Remembers where the fingers are, so that the zoom that follows keeps
    /// the ground between them between them.
    @Override
    protected boolean pinch(int[] x, int[] y) {
        if (x.length > 1) {
            pinchX = (x[0] + x[1]) / 2 - getAbsoluteX();
            pinchY = (y[0] + y[1]) / 2 - getAbsoluteY();
            pinchPlaced = true;
        }
        return false;
    }

    /// {@inheritDoc}
    ///
    /// The end of a pinch that leaves no pointer behind: a trackpad's, which
    /// never presses or releases anything. Without it the map stayed mid-pinch,
    /// and the next pinch began from the zoom the last one had started at.
    @Override
    protected void pinchReleased(int x, int y) {
        if (!pinching) {
            return;
        }
        pinching = false;
        // A finger still down carries on as a drag from where it is, and is
        // neither a tap nor a throw when it lifts.
        lastX = x;
        lastY = y;
        dragDistance = Math.max(dragDistance, 10);
        sampleCount = 0;
        fireCameraChanged();
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
        settle();
        // Between the fingers; for a gesture that has none on the screen, under
        // the pointer; and failing both, the middle of the map.
        int fx = getWidth() / 2;
        int fy = getHeight() / 2;
        if (pinchPlaced) {
            fx = pinchX;
            fy = pinchY;
        } else if (pointerSeen) {
            fx = pointerX;
            fy = pointerY;
        }
        pinchPlaced = false;
        if (!pinching) {
            pinching = true;
            pinchStartZoom = engine.getZoom();
            pinchLastX = fx;
            pinchLastY = fy;
        }
        double nz = pinchStartZoom + MathUtil.log(scale) / MathUtil.log(2);
        // Zoomed about where the fingers were and then carried to where they
        // are, so two fingers that travel as they spread take the map along.
        engine.zoomAround(nz, pinchLastX, pinchLastY);
        engine.panPixels(fx - pinchLastX, fy - pinchLastY);
        pinchLastX = fx;
        pinchLastY = fy;
        sampleCount = 0;
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
