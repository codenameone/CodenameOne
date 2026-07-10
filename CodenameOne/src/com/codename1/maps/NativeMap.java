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

import com.codename1.maps.spi.MapProvider;
import com.codename1.maps.spi.MapProviderRegistry;
import com.codename1.maps.vector.MapStyle;
import com.codename1.maps.vector.TileSource;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Point;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A native-rendered map. When the build wired in a native provider (Apple
/// MapKit, Google Maps, Bing, Huawei, ... selected via the `maps.provider`
/// build hint) and it is available on the device, `NativeMap` embeds that
/// provider's native view as a [com.codename1.ui.PeerComponent]. Otherwise -- on the simulator,
/// on devices without the selected provider, or when no provider was wired in
/// at all -- it transparently falls back to an embedded pure-vector
/// [MapView]. Either way it exposes the same [MapSurface] API, so application
/// code is identical.
///
/// The public API never names a provider; which one (if any) backs a given
/// build is decided entirely by build hints through [MapProviderRegistry].
public class NativeMap extends Container implements MapSurface {

    private static final Map INSTANCES = new HashMap();
    private static int idCounter = 1;

    private final int mapId;
    private MapProvider provider;
    private MapView fallback;
    private Component peer;
    private boolean peerInitialized;

    private LatLng initialCenter = new LatLng(0, 0);
    private double initialZoom = 2;
    private TileSource fallbackSource;
    private MapStyle fallbackStyle;

    private final List markers = new ArrayList();
    private final List tapListeners = new ArrayList();
    private final List longPressListeners = new ArrayList();
    private final List cameraListeners = new ArrayList();

    /// Creates a native map centered on the equator at a low zoom.
    public NativeMap() {
        this(new LatLng(0, 0), 2);
    }

    /// Creates a native map at the given initial camera.
    public NativeMap(LatLng center, double zoom) {
        this(center, zoom, null, null);
    }

    /// Creates a native map at the given initial camera, specifying the tile
    /// source and style used by the pure-vector [MapView] when no native
    /// provider is available. Useful for an offline or branded fallback
    /// basemap (and for deterministic tests).
    public NativeMap(LatLng center, double zoom, TileSource fallbackSource, MapStyle fallbackStyle) {
        synchronized (NativeMap.class) {
            mapId = idCounter++;
        }
        INSTANCES.put(Integer.valueOf(mapId), this);
        this.initialCenter = center;
        this.initialZoom = zoom;
        this.fallbackSource = fallbackSource;
        this.fallbackStyle = fallbackStyle;
        setLayout(new BorderLayout());
        provider = MapProviderRegistry.getProvider();
        if (provider != null && !safeAvailable(provider)) {
            provider = null;
        }
        if (provider == null) {
            // No native provider wired in (or unavailable at runtime) -> behave
            // as a pure-vector MapView immediately so the API works before the
            // component is ever shown.
            createFallback();
        }
        // When a provider IS present the native peer is created lazily in
        // initComponent() -- the standard Codename One peer lifecycle. Building
        // the peer here (detached from any form) and laying it out on show is
        // what crashed UIKit/MapKit, so we defer it until we are attached.
    }

    /// {@inheritDoc}
    @Override
    protected void initComponent() {
        super.initComponent();
        if (peerInitialized || provider == null) {
            return;
        }
        peerInitialized = true;
        // Create the native peer once the form is fully shown, not inline here:
        // building/laying it out during initComponent re-enters layout before the
        // component is wired to its form, which crashed the native peer on iOS.
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                installPeer();
            }
        });
    }

    private void installPeer() {
        Component peer = null;
        try {
            peer = provider.createPeer(this, mapId);
        } catch (Throwable t) {
            peer = null;
        }
        if (peer == null) {
            // The provider could not create a peer at runtime -> vector fallback.
            provider = null;
            createFallback();
            revalidateForm();
            return;
        }
        this.peer = peer;
        addComponent(BorderLayout.CENTER, peer);
        // Lay the peer out (give it a non-zero frame) so the native view is on
        // screen. The provider positions the camera at creation from the
        // initial center/zoom (see getInitialCenter/getInitialZoom).
        revalidateForm();
        replayMarkers();
    }

    /// Releases the native or web view backing this map and detaches it. Call
    /// this when you are finished with a `NativeMap` you will not show again so
    /// a provider that keeps a live view -- in particular the web provider's
    /// `BrowserComponent`, whose map JavaScript runs a continuous animation
    /// loop -- stops consuming CPU/GPU in the background instead of being left
    /// running. After `dispose()` the map should not be reused.
    public void dispose() {
        if (provider != null && peer != null) {
            try {
                provider.deinitialize(mapId);
            } catch (Throwable t) {
                // Best effort: a provider that fails to tear down must not
                // propagate out of dispose(), but surface it rather than hide it.
                t.printStackTrace();
            }
            removeComponent(peer);
            peer = null;
            revalidateForm();
        }
        INSTANCES.remove(Integer.valueOf(mapId));
    }

    /// The initial camera center. Package-private: build-injected providers
    /// read it to position the native map when its peer is created.
    LatLng getInitialCenter() {
        return initialCenter;
    }

    /// The initial camera zoom. Package-private (see [#getInitialCenter()]).
    double getInitialZoom() {
        return initialZoom;
    }

    private void revalidateForm() {
        Form form = getComponentForm();
        if (form != null) {
            form.revalidate();
        }
    }

    private void createFallback() {
        if (fallbackSource != null) {
            fallback = new MapView(fallbackSource, fallbackStyle == null ? MapStyle.light() : fallbackStyle);
        } else {
            fallback = new MapView();
        }
        fallback.setCenter(initialCenter);
        fallback.setZoom(initialZoom);
        addComponent(BorderLayout.CENTER, fallback);
    }

    /// Re-issues markers that were added before the native peer existed so they
    /// appear once the peer is created on attach.
    private void replayMarkers() {
        for (Object markerObj : markers) {
            Marker m = (Marker) markerObj;
            byte[] iconData = null;
            EncodedImage icon = m.getIcon();
            if (icon != null) {
                iconData = icon.getImageData();
            }
            long key = provider.addMarker(mapId, iconData, m.getPosition().getLatitude(),
                    m.getPosition().getLongitude(), m.getTitle(), m.getSnippet(),
                    m.getAnchorU(), m.getAnchorV());
            m.providerKey = Long.valueOf(key);
        }
    }

    private static boolean safeAvailable(MapProvider p) {
        try {
            return p.isAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isFallback() {
        return fallback != null;
    }

    // ---- MapSurface: camera ----------------------------------------------

    /// {@inheritDoc}
    @Override
    public CameraPosition getCameraPosition() {
        if (isFallback()) {
            return fallback.getCameraPosition();
        }
        return new CameraPosition(getCenter(), getZoom());
    }

    /// {@inheritDoc}
    @Override
    public void setCameraPosition(CameraPosition position) {
        if (isFallback()) {
            fallback.setCameraPosition(position);
            return;
        }
        provider.setCamera(mapId, position.getTarget().getLatitude(),
                position.getTarget().getLongitude(), (float) position.getZoom(),
                (float) position.getBearing(), (float) position.getTilt());
    }

    /// {@inheritDoc}
    @Override
    public void moveCamera(LatLng target, double zoom) {
        if (isFallback()) {
            fallback.moveCamera(target, zoom);
            return;
        }
        provider.setCamera(mapId, target.getLatitude(), target.getLongitude(), (float) zoom, 0, 0);
    }

    /// {@inheritDoc}
    @Override
    public double getZoom() {
        return isFallback() ? fallback.getZoom() : provider.getZoom(mapId);
    }

    /// {@inheritDoc}
    @Override
    public void setZoom(double zoom) {
        if (isFallback()) {
            fallback.setZoom(zoom);
            return;
        }
        provider.setCamera(mapId, provider.getLatitude(mapId), provider.getLongitude(mapId),
                (float) zoom, 0, 0);
    }

    /// {@inheritDoc}
    @Override
    public double getMinZoom() {
        return isFallback() ? fallback.getMinZoom() : provider.getMinZoom(mapId);
    }

    /// {@inheritDoc}
    @Override
    public double getMaxZoom() {
        return isFallback() ? fallback.getMaxZoom() : provider.getMaxZoom(mapId);
    }

    /// {@inheritDoc}
    @Override
    public LatLng getCenter() {
        if (isFallback()) {
            return fallback.getCenter();
        }
        return new LatLng(provider.getLatitude(mapId), provider.getLongitude(mapId));
    }

    /// {@inheritDoc}
    @Override
    public void setCenter(LatLng center) {
        if (isFallback()) {
            fallback.setCenter(center);
            return;
        }
        provider.setCamera(mapId, center.getLatitude(), center.getLongitude(),
                provider.getZoom(mapId), 0, 0);
    }

    /// {@inheritDoc}
    @Override
    public MapBounds getVisibleRegion() {
        if (isFallback()) {
            return fallback.getVisibleRegion();
        }
        LatLng nw = screenToLatLng(0, 0);
        LatLng se = screenToLatLng(getWidth(), getHeight());
        return new MapBounds(new LatLng(se.getLatitude(), nw.getLongitude()),
                new LatLng(nw.getLatitude(), se.getLongitude()));
    }

    /// {@inheritDoc}
    @Override
    public void fitBounds(MapBounds bounds, int paddingPixels) {
        if (isFallback()) {
            fallback.fitBounds(bounds, paddingPixels);
            return;
        }
        // Native providers center on the bounds; precise fit is provider work.
        provider.setCamera(mapId, bounds.getCenter().getLatitude(),
                bounds.getCenter().getLongitude(), provider.getZoom(mapId), 0, 0);
    }

    // ---- MapSurface: map objects -----------------------------------------

    /// {@inheritDoc}
    @Override
    public Marker addMarker(MarkerOptions options) {
        Marker m = options.build();
        if (isFallback()) {
            return fallback.addMarker(options);
        }
        byte[] iconData = null;
        EncodedImage icon = m.getIcon();
        if (icon != null) {
            iconData = icon.getImageData();
        }
        long key = provider.addMarker(mapId, iconData, m.getPosition().getLatitude(),
                m.getPosition().getLongitude(), m.getTitle(), m.getSnippet(),
                m.getAnchorU(), m.getAnchorV());
        m.providerKey = Long.valueOf(key);
        markers.add(m);
        return m;
    }

    /// {@inheritDoc}
    @Override
    public void removeMarker(Marker marker) {
        if (isFallback()) {
            fallback.removeMarker(marker);
            return;
        }
        if (marker.providerKey instanceof Long) {
            provider.removeElement(mapId, ((Long) marker.providerKey).longValue());
        }
        markers.remove(marker);
    }

    /// {@inheritDoc}
    @Override
    public Polyline addPolyline(Polyline polyline) {
        if (isFallback()) {
            return fallback.addPolyline(polyline);
        }
        long pathId = provider.beginPath(mapId);
        List pts = polyline.getPoints();
        for (Object ptObj : pts) {
            LatLng p = (LatLng) ptObj;
            provider.addToPath(mapId, pathId, p.getLatitude(), p.getLongitude());
        }
        long key = provider.finishPolyline(mapId, pathId, polyline.getStrokeColor(),
                polyline.getStrokeWidth());
        polyline.providerKey = Long.valueOf(key);
        return polyline;
    }

    /// {@inheritDoc}
    @Override
    public void removePolyline(Polyline polyline) {
        if (isFallback()) {
            fallback.removePolyline(polyline);
            return;
        }
        removeElement(polyline.providerKey);
    }

    /// {@inheritDoc}
    @Override
    public Polygon addPolygon(Polygon polygon) {
        if (isFallback()) {
            return fallback.addPolygon(polygon);
        }
        long pathId = provider.beginPath(mapId);
        List pts = polygon.getPoints();
        for (Object ptObj : pts) {
            LatLng p = (LatLng) ptObj;
            provider.addToPath(mapId, pathId, p.getLatitude(), p.getLongitude());
        }
        long key = provider.finishPolygon(mapId, pathId, polygon.getFillColor(),
                polygon.getStrokeColor(), polygon.getStrokeWidth());
        polygon.providerKey = Long.valueOf(key);
        return polygon;
    }

    /// {@inheritDoc}
    @Override
    public void removePolygon(Polygon polygon) {
        if (isFallback()) {
            fallback.removePolygon(polygon);
            return;
        }
        removeElement(polygon.providerKey);
    }

    /// {@inheritDoc}
    @Override
    public Circle addCircle(Circle circle) {
        if (isFallback()) {
            return fallback.addCircle(circle);
        }
        long key = provider.addCircle(mapId, circle.getCenter().getLatitude(),
                circle.getCenter().getLongitude(), circle.getRadiusMeters(),
                circle.getFillColor(), circle.getStrokeColor(), circle.getStrokeWidth());
        circle.providerKey = Long.valueOf(key);
        return circle;
    }

    /// {@inheritDoc}
    @Override
    public void removeCircle(Circle circle) {
        if (isFallback()) {
            fallback.removeCircle(circle);
            return;
        }
        removeElement(circle.providerKey);
    }

    /// {@inheritDoc}
    @Override
    public void clearMapObjects() {
        if (isFallback()) {
            fallback.clearMapObjects();
            return;
        }
        provider.removeAllElements(mapId);
        markers.clear();
    }

    private void removeElement(Object providerKey) {
        if (providerKey instanceof Long) {
            provider.removeElement(mapId, ((Long) providerKey).longValue());
        }
    }

    // ---- MapSurface: conversion + listeners ------------------------------

    /// {@inheritDoc}
    @Override
    public Point latLngToScreen(LatLng coord) {
        if (isFallback()) {
            return fallback.latLngToScreen(coord);
        }
        provider.calcScreenPosition(mapId, coord.getLatitude(), coord.getLongitude());
        return new Point(provider.getScreenX(mapId), provider.getScreenY(mapId));
    }

    /// {@inheritDoc}
    @Override
    public LatLng screenToLatLng(int x, int y) {
        if (isFallback()) {
            return fallback.screenToLatLng(x, y);
        }
        provider.calcLatLongPosition(mapId, x, y);
        return new LatLng(provider.getScreenLat(mapId), provider.getScreenLon(mapId));
    }

    /// {@inheritDoc}
    @Override
    public void addTapListener(MapTapListener l) {
        if (isFallback()) {
            fallback.addTapListener(l);
            return;
        }
        tapListeners.add(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeTapListener(MapTapListener l) {
        if (isFallback()) {
            fallback.removeTapListener(l);
            return;
        }
        tapListeners.remove(l);
    }

    /// {@inheritDoc}
    @Override
    public void addLongPressListener(MapTapListener l) {
        if (isFallback()) {
            fallback.addLongPressListener(l);
            return;
        }
        longPressListeners.add(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeLongPressListener(MapTapListener l) {
        if (isFallback()) {
            fallback.removeLongPressListener(l);
            return;
        }
        longPressListeners.remove(l);
    }

    /// {@inheritDoc}
    @Override
    public void addCameraChangeListener(CameraChangeListener l) {
        if (isFallback()) {
            fallback.addCameraChangeListener(l);
            return;
        }
        cameraListeners.add(l);
    }

    /// {@inheritDoc}
    @Override
    public void removeCameraChangeListener(CameraChangeListener l) {
        if (isFallback()) {
            fallback.removeCameraChangeListener(l);
            return;
        }
        cameraListeners.remove(l);
    }

    /// {@inheritDoc}
    @Override
    public boolean isNativeMap() {
        return !isFallback();
    }

    /// Whether the native map has finished its initial render. For the web
    /// provider this reflects the Google Maps `tilesloaded` event, so a caller
    /// (e.g. a screenshot test) can wait for the real map to paint instead of a
    /// fixed delay. Providers without an async-readiness signal report ready
    /// once their peer exists.
    public boolean isMapReady() {
        if (isFallback()) {
            return fallback.isMapReady();
        }
        if (provider instanceof WebMapProvider) {
            return ((WebMapProvider) provider).isReady(mapId);
        }
        return provider != null && peer != null;
    }

    /// {@inheritDoc} Delegates to the embedded vector map when this `NativeMap`
    /// fell back to it; a real native provider loads its own tiles, so false.
    @Override
    public boolean isLoadingTiles() {
        return isFallback() && fallback.isLoadingTiles();
    }

    /// {@inheritDoc}
    @Override
    public Component asComponent() {
        return this;
    }

    // ---- Native callbacks (invoked by build-injected provider code) ------

    /// Invoked from native code when the map is tapped.
    public static void fireTap(int mapId, int x, int y) {
        NativeMap map = lookup(mapId);
        if (map == null) {
            return;
        }
        LatLng geo = map.screenToLatLng(x, y);
        for (int i = 0; i < map.tapListeners.size(); i++) {
            ((MapTapListener) map.tapListeners.get(i)).mapTapped(map, geo, x, y);
        }
    }

    /// Invoked from native code when the map is long-pressed.
    public static void fireLongPress(int mapId, int x, int y) {
        NativeMap map = lookup(mapId);
        if (map == null) {
            return;
        }
        LatLng geo = map.screenToLatLng(x, y);
        for (int i = 0; i < map.longPressListeners.size(); i++) {
            ((MapTapListener) map.longPressListeners.get(i)).mapTapped(map, geo, x, y);
        }
    }

    /// Invoked from native code when a marker is tapped (`markerKey` is the
    /// value returned by [MapProvider#addMarker]).
    public static void fireMarkerClick(int mapId, long markerKey) {
        NativeMap map = lookup(mapId);
        if (map == null) {
            return;
        }
        for (int i = 0; i < map.markers.size(); i++) {
            Marker m = (Marker) map.markers.get(i);
            if (m.providerKey instanceof Long && ((Long) m.providerKey).longValue() == markerKey) {
                if (m.getOnClick() != null) {
                    m.getOnClick().actionPerformed(new ActionEvent(m));
                }
                return;
            }
        }
    }

    /// Invoked from native code when the camera settles after movement.
    public static void fireCameraChange(int mapId) {
        NativeMap map = lookup(mapId);
        if (map == null || map.cameraListeners.isEmpty()) {
            return;
        }
        CameraPosition pos = map.getCameraPosition();
        for (int i = 0; i < map.cameraListeners.size(); i++) {
            ((CameraChangeListener) map.cameraListeners.get(i)).cameraChanged(map, pos);
        }
    }

    private static NativeMap lookup(int mapId) {
        return (NativeMap) INSTANCES.get(Integer.valueOf(mapId));
    }
}
