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
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Component;
import com.codename1.util.SuccessCallback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// A cross-platform [MapProvider] that hosts a JavaScript map SDK (Google Maps
/// JS, Azure Maps, ...) inside a [BrowserComponent]. Because it relies only on
/// a web view it renders on every platform that has a browser, which makes it
/// the natural `"web"` entry in a provider fallback chain and the only way to
/// surface SDKs that ship no native component (e.g. Azure Maps).
///
/// The map's initial camera comes from the host [NativeMap] (its center/zoom),
/// baked straight into the page so the map opens on the right region without a
/// follow-up call. Use [#google(String)] for a ready-made Google Maps page, or
/// the constructor with a custom HTML template containing the tokens
/// `{key}`, `{lat}`, `{lon}` and `{zoom}`.
public class WebMapProvider implements MapProvider {

    private final String id;
    private final String apiKey;
    private final String htmlTemplate;
    private final Map peers = new HashMap();
    /// mapIds whose web map has fired its first 'tilesloaded' (see createPeer's
    /// JS bridge). A plain Set read so isReady(int) is a cheap, non-blocking
    /// check -- never a synchronous JS round-trip on the EDT.
    private final Set readyMapIds = new HashSet();

    /// Creates a web provider.
    ///
    /// #### Parameters
    ///
    /// - `id`: the provider id used in the fallback chain (e.g. `"web"`)
    ///
    /// - `apiKey`: the SDK key substituted for `{key}` (may be empty for
    /// keyless SDKs)
    ///
    /// - `htmlTemplate`: a full HTML document with `{key}`/`{lat}`/`{lon}`/`{zoom}` tokens
    public WebMapProvider(String id, String apiKey, String htmlTemplate) {
        this.id = id;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.htmlTemplate = htmlTemplate;
    }

    /// A Google Maps JavaScript SDK provider for the given API key (the key
    /// must have the *Maps JavaScript API* enabled). Its id is `"web"` -- not
    /// `"google"` -- so it slots in as the cross-platform *web* fallback after
    /// the native `"google"` provider in a chain such as
    /// `setProviderOrder("google", "web", "vector")`, rather than colliding
    /// with it.
    public static WebMapProvider google(String apiKey) {
        return new WebMapProvider("web", apiKey, GOOGLE_HTML);
    }

    /// {@inheritDoc}
    @Override
    public String getId() {
        return id;
    }

    /// {@inheritDoc}
    @Override
    public boolean isAvailable() {
        // Needs a real key (not the empty default and not an unsubstituted
        // build placeholder); any platform with a browser can then render it.
        return apiKey.length() > 0 && apiKey.indexOf('{') < 0;
    }

    /// {@inheritDoc}
    @Override
    public Component createPeer(NativeMap host, int mapId) {
        LatLng center = host.getInitialCenter();
        double lat = center == null ? 0 : center.getLatitude();
        double lon = center == null ? 0 : center.getLongitude();
        int zoom = (int) Math.round(host.getInitialZoom());
        String html = replace(htmlTemplate, "{key}", apiKey);
        html = replace(html, "{lat}", Double.toString(lat));
        html = replace(html, "{lon}", Double.toString(lon));
        html = replace(html, "{zoom}", Integer.toString(zoom));
        BrowserComponent bc = new BrowserComponent();
        bc.setPage(html, "https://maps.example/");
        peers.put(Integer.valueOf(mapId), bc);
        // Bridge the map's readiness to Java once, event-style, instead of
        // polling it with a synchronous executeAndReturnString (which routes
        // through invokeAndBlock on iOS and is far too costly to call in a
        // loop). The HTML sets window.cn1mapReady on the SDK's 'tilesloaded'
        // event; an in-page interval (cheap, runs in the browser) watches that
        // flag and calls back into Java exactly once, flipping a plain Set entry.
        final Integer readyKey = Integer.valueOf(mapId);
        bc.addJSCallback(
                "window.cn1NotifyMapReady=function(){callback.onSuccess('ready');};"
                + "(function(){var t=setInterval(function(){"
                + "if(window.cn1mapReady===true){clearInterval(t);window.cn1NotifyMapReady();}"
                + "},200);})();",
                new SuccessCallback<BrowserComponent.JSRef>() {
                    public void onSucess(BrowserComponent.JSRef value) {
                        readyMapIds.add(readyKey);
                    }
                });
        return bc;
    }

    /// Whether the web map for `mapId` has finished painting its tiles, i.e. the
    /// SDK has fired its first `tilesloaded` event. Lets a caller wait for the
    /// real render rather than a fixed delay. Cheap and non-blocking: reads a
    /// Set updated by the one-shot JS->Java readiness bridge in createPeer.
    public boolean isReady(int mapId) {
        return readyMapIds.contains(Integer.valueOf(mapId));
    }

    /// {@inheritDoc}
    @Override
    public void deinitialize(int mapId) {
        readyMapIds.remove(Integer.valueOf(mapId));
        BrowserComponent bc = (BrowserComponent) peers.remove(Integer.valueOf(mapId));
        if (bc != null) {
            try {
                // Replace the live map page with a blank one so the SDK's
                // requestAnimationFrame / tile-loading loop stops; otherwise a
                // disposed web map keeps animating in the background and starves
                // the main thread.
                bc.setPage("<!DOCTYPE html><html><body></body></html>", "https://maps.example/");
            } catch (Throwable t) {
                // Best effort tear-down.
                return;
            }
        }
    }

    /// {@inheritDoc}
    @Override
    public void setCamera(int mapId, double lat, double lon, float zoom, float bearing, float tilt) {
        exec(mapId, "if(window.cn1map){window.cn1map.setCenter({lat:" + lat + ",lng:" + lon
                + "});window.cn1map.setZoom(" + ((int) zoom) + ");}");
    }

    /// {@inheritDoc}
    @Override
    public long addMarker(int mapId, byte[] icon, double lat, double lon,
                          String title, String snippet, float anchorU, float anchorV) {
        exec(mapId, "if(window.cn1map&&window.google){new google.maps.Marker({position:{lat:"
                + lat + ",lng:" + lon + "},map:window.cn1map});}");
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public void setMapType(int mapId, int type) {
        String t = type == MAP_TYPE_SATELLITE ? "satellite"
                : type == MAP_TYPE_HYBRID ? "hybrid"
                : type == MAP_TYPE_TERRAIN ? "terrain" : "roadmap";
        exec(mapId, "if(window.cn1map){window.cn1map.setMapTypeId('" + t + "');}");
    }

    private void exec(int mapId, String js) {
        BrowserComponent bc = (BrowserComponent) peers.get(Integer.valueOf(mapId));
        if (bc != null) {
            try {
                bc.execute(js);
            } catch (Throwable t) {
                // best-effort scripting; a not-yet-loaded page just ignores it
                return;
            }
        }
    }

    // The web SDK has no cheap synchronous bridge for these; they return
    // neutral values. The screenshot/display path does not depend on them.

    /// {@inheritDoc}
    @Override
    public double getLatitude(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public double getLongitude(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public float getZoom(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public float getMaxZoom(int mapId) {
        return 21;
    }

    /// {@inheritDoc}
    @Override
    public float getMinZoom(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public long beginPath(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public void addToPath(int mapId, long pathId, double lat, double lon) {
    }

    /// {@inheritDoc}
    @Override
    public long finishPolyline(int mapId, long pathId, int strokeColor, int strokeWidth) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public long finishPolygon(int mapId, long pathId, int fillColor, int strokeColor, int strokeWidth) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public long addCircle(int mapId, double lat, double lon, double radiusMeters,
                          int fillColor, int strokeColor, int strokeWidth) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public void removeElement(int mapId, long elementId) {
    }

    /// {@inheritDoc}
    @Override
    public void removeAllElements(int mapId) {
    }

    /// {@inheritDoc}
    @Override
    public void calcScreenPosition(int mapId, double lat, double lon) {
    }

    /// {@inheritDoc}
    @Override
    public int getScreenX(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public int getScreenY(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public void calcLatLongPosition(int mapId, int x, int y) {
    }

    /// {@inheritDoc}
    @Override
    public double getScreenLat(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public double getScreenLon(int mapId) {
        return 0;
    }

    /// {@inheritDoc}
    @Override
    public void setShowMyLocation(int mapId, boolean show) {
    }

    /// {@inheritDoc}
    @Override
    public void setRotateGestureEnabled(int mapId, boolean enabled) {
    }

    private static String replace(String src, String token, String value) {
        int idx;
        String s = src;
        while ((idx = s.indexOf(token)) >= 0) {
            s = s.substring(0, idx) + value + s.substring(idx + token.length());
        }
        return s;
    }

    private static final String GOOGLE_HTML =
            "<!DOCTYPE html><html><head>"
            + "<meta name=\"viewport\" content=\"initial-scale=1,maximum-scale=1,user-scalable=no\">"
            + "<style>html,body,#map{height:100%;width:100%;margin:0;padding:0}</style></head>"
            + "<body><div id=\"map\"></div><script>"
            + "function initMap(){window.cn1map=new google.maps.Map(document.getElementById('map'),"
            + "{center:{lat:{lat},lng:{lon}},zoom:{zoom},disableDefaultUI:true,"
            + "gestureHandling:'none',clickableIcons:false});"
            // Expose a readiness flag once the map has actually painted its
            // tiles, so callers can wait for the real render instead of guessing
            // a fixed delay (the screenshot test polls isReady(int)).
            + "google.maps.event.addListenerOnce(window.cn1map,'tilesloaded',function(){window.cn1mapReady=true;});}"
            + "</script>"
            + "<script async src=\"https://maps.googleapis.com/maps/api/js?key={key}&callback=initMap\"></script>"
            + "</body></html>";
}
