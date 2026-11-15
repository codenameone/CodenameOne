/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.fieldline.map;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapBounds;
import com.codename1.maps.MapView;
import com.codename1.maps.Marker;
import com.codename1.maps.MarkerOptions;
import com.codename1.maps.Polyline;
import com.codename1.maps.vector.MapStyle;
import com.codename1.maps.vector.MvtTileSource;
import com.codename1.maps.vector.TileSource;
import com.codename1.ui.CN;
import com.codename1.ui.Font;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.MathUtil;
import com.codenameone.examples.fieldline.ui.Look;

import java.util.ArrayList;
import java.util.List;

/// The map, as the screens use it.
///
/// `MapView` draws OpenStreetMap vector tiles with no key and no account, in a
/// light or a dark style to match the app. Everything put on it is drawn here,
/// in code: the numbered stops of a route, the people on a crew map, and the
/// route itself in the colours of `WlRoute` from `theme.css`, whose text
/// colour is the line and background colour its edge.
public final class Maps {
    public static final int CITY_ZOOM = 13;
    public static final int STREET_ZOOM = 15;
    private static TileSource tiles;

    private Maps() {
    }

    /// Draws every map from these tiles from now on. The tests use the handful
    /// bundled with them, so a screenshot does not depend on a tile server.
    public static void useTiles(TileSource source) {
        tiles = source;
    }

    public static MapView create(LatLng center, int zoom) {
        MapView map = new MapView(tiles == null ? MvtTileSource.openFreeMap() : tiles,
                Look.dark() ? MapStyle.dark() : MapStyle.light());
        map.setName("map");
        // The map is the screen here, not an item on a page that scrolls, so
        // the wheel is for zooming it.
        map.setWheelZoom(true);
        map.moveCamera(center, zoom);
        return map;
    }

    /// A stop on a technician's route: a round pin in `color` with the stop's
    /// number in it, a ring when the stop is done, a halo when it is the one
    /// under way.
    public static Marker stop(MapView map, LatLng at, String label, int color, boolean done,
            boolean current, String title, ActionListener<ActionEvent> onClick) {
        MarkerOptions options = new MarkerOptions(at).icon(numbered(label, color, done, current))
                .anchor(0.5f, 0.5f).title(title);
        if (onClick != null) {
            options.onClick(onClick);
        }
        return map.addMarker(options);
    }

    /// A technician on the crew map: their initials in their colour.
    public static Marker person(MapView map, LatLng at, String initials, int color,
            String title, ActionListener<ActionEvent> onClick) {
        return stop(map, at, initials, color, false, true, title, onClick);
    }

    private static EncodedImage numbered(String label, int color, boolean done,
            boolean current) {
        int side = CN.convertToPixels(current ? 8.5f : 7f);
        int halo = current ? side / 6 : 0;
        int edge = Math.max(2, side / 12);
        Image image = Image.createImage(side, side, 0);
        Graphics g = image.getGraphics();
        g.setAntiAliased(true);
        g.setAntiAliasedText(true);
        if (current) {
            g.setColor(color);
            g.setAlpha(70);
            g.fillArc(0, 0, side, side, 0, 360);
            g.setAlpha(255);
        }
        int inner = side - 2 * halo;
        g.setColor(0xffffff);
        g.fillArc(halo, halo, inner, inner, 0, 360);
        g.setColor(done ? 0x9aa3ae : color);
        g.fillArc(halo + edge, halo + edge, inner - 2 * edge, inner - 2 * edge, 0, 360);
        Font font = Font.createTrueTypeFont(Font.NATIVE_MAIN_BOLD, Font.NATIVE_MAIN_BOLD)
                .derive(inner * 0.42f, Font.STYLE_BOLD);
        g.setFont(font);
        g.setColor(0xffffff);
        String text = done ? "\u2713" : label;
        g.drawString(text, (side - font.stringWidth(text)) / 2, (side - font.getHeight()) / 2);
        return EncodedImage.createFromImage(image, false);
    }

    /// A route on the map: the line and the edge drawn under it.
    public static final class Line {
        private final Polyline edge;
        private final Polyline line;

        Line(Polyline edge, Polyline line) {
            this.edge = edge;
            this.line = line;
        }

        public void remove(MapView map) {
            map.removePolyline(edge);
            map.removePolyline(line);
        }
    }

    /// Draws `points` as the route and returns it, to remove it by.
    public static Line route(MapView map, List<LatLng> points) {
        Style style = UIManager.getInstance().getComponentStyle("WlRoute");
        int width = Math.max(4, CN.convertToPixels(1.1f));
        // The edge first, so that the line is drawn over it.
        Polyline edge = stroke(points, style.getBgColor(), width + Math.max(2, width / 2));
        Polyline line = stroke(points, style.getFgColor(), width);
        map.addPolyline(edge);
        map.addPolyline(line);
        return new Line(edge, line);
    }

    private static Polyline stroke(List<LatLng> points, int color, int width) {
        LatLng[] line = new LatLng[points.size()];
        points.toArray(line);
        return new Polyline(line).setStrokeColor(color).setStrokeWidth(width);
    }

    /// Moves the camera so that all of `points` are in view.
    public static void show(MapView map, LatLng... points) {
        frame(map, 0, 0, points);
    }

    /// Moves the camera so that all of `points` are in the part of the map
    /// that is not covered: `top` pixels of it are under something at the top,
    /// and `bottom` pixels under the sheet.
    public static void frame(MapView map, int top, int bottom, LatLng... points) {
        List<LatLng> all = new ArrayList<LatLng>();
        for (LatLng point : points) {
            if (point != null) {
                all.add(point);
            }
        }
        if (all.isEmpty()) {
            return;
        }
        int height = map.getHeight();
        if (height > 0 && top + bottom > 0) {
            // The camera fits what it is given to the whole map. So it is given
            // more: the points, and a point far enough above and below them
            // that the points themselves land in the clear part.
            double north = all.get(0).getLatitude();
            double south = north;
            for (LatLng point : all) {
                north = Math.max(north, point.getLatitude());
                south = Math.min(south, point.getLatitude());
            }
            double clear = Math.max(0.3, 1d - (top + bottom) / (double) height);
            double span = Math.max(north - south, 0.002) / clear;
            double middle = all.get(0).getLongitude();
            all.add(new LatLng(north + span * top / height, middle));
            all.add(new LatLng(south - span * bottom / height, middle));
        }
        if (all.size() == 1) {
            map.moveCamera(all.get(0), STREET_ZOOM);
        } else {
            // Room for a marker's own size at the edge, on a screen of any density.
            map.fitBounds(MapBounds.fromCoordinates(all), CN.convertToPixels(10, true));
        }
    }

    /// The distance between two points in meters, along the surface.
    public static double meters(LatLng a, LatLng b) {
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.getLongitude() - a.getLongitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * 6371000d * MathUtil.asin(Math.min(1d, Math.sqrt(h)));
    }
}
