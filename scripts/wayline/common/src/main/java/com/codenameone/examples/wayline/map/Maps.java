/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codenameone.examples.wayline.map;

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
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.MathUtil;
import com.codenameone.examples.wayline.ui.Look;

import java.util.ArrayList;
import java.util.List;

/// The map, as the screens use it.
///
/// `MapView` draws OpenStreetMap vector tiles with no key and no account, in a
/// light or a dark style to match the app. Everything put on it is drawn here,
/// in code, in colours from `theme.css`: the pickup and drop-off pins
/// (`WlMarkerPickup`, `WlMarkerDropoff`), the car (`WlMarkerCar`) and the route
/// (`WlRoute`). In each of those styles the text colour is the shape and the
/// background colour is its edge.
public final class Maps {
    public static final int CITY_ZOOM = 13;
    public static final int STREET_ZOOM = 15;
    /// The car is drawn once for each of this many directions, and the
    /// drawings kept.
    private static final int HEADINGS = 36;

    private static TileSource tiles;
    private static final EncodedImage[] CARS = new EncodedImage[HEADINGS];
    /// Whether [#CARS] were drawn for the dark look.
    private static boolean carsDark;

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

    /// Where the ride starts: a disc with a dot in it.
    public static Marker pickup(MapView map, LatLng at) {
        return marker(map, at, pin("WlMarkerPickup", true));
    }

    /// Where the ride ends: the same, square.
    public static Marker dropoff(MapView map, LatLng at) {
        return marker(map, at, pin("WlMarkerDropoff", false));
    }

    private static Marker marker(MapView map, LatLng at, EncodedImage icon) {
        return map.addMarker(new MarkerOptions(at).icon(icon).anchor(0.5f, 0.5f));
    }

    private static EncodedImage pin(String uiid, boolean round) {
        Style style = UIManager.getInstance().getComponentStyle(uiid);
        int side = CN.convertToPixels(5.5f);
        int edge = Math.max(2, side / 7);
        int dot = Math.max(2, side / 4);
        Image image = Image.createImage(side, side, 0);
        Graphics g = image.getGraphics();
        g.setAntiAliased(true);
        g.setColor(style.getBgColor());
        fill(g, round, 0, side);
        g.setColor(style.getFgColor());
        fill(g, round, edge, side - 2 * edge);
        g.setColor(style.getBgColor());
        fill(g, round, (side - dot) / 2, dot);
        return EncodedImage.createFromImage(image, false);
    }

    private static void fill(Graphics g, boolean round, int at, int side) {
        if (round) {
            g.fillArc(at, at, side, side, 0, 360);
        } else {
            g.fillRoundRect(at, at, side, side, side / 4, side / 4);
        }
    }

    /// A car seen from above, pointing along `heading` -- degrees clockwise
    /// from north. A map marker cannot be turned, so the car is drawn turned.
    static EncodedImage car(double heading) {
        boolean dark = Look.dark();
        if (dark != carsDark) {
            carsDark = dark;
            for (int iter = 0; iter < HEADINGS; iter++) {
                CARS[iter] = null;
            }
        }
        int step = (int) Math.round(heading * HEADINGS / 360d) % HEADINGS;
        if (step < 0) {
            step += HEADINGS;
        }
        if (CARS[step] == null) {
            CARS[step] = drawCar(step * 360d / HEADINGS);
        }
        return CARS[step];
    }

    private static EncodedImage drawCar(double heading) {
        Style style = UIManager.getInstance().getComponentStyle("WlMarkerCar");
        // Square, and wide enough for the car's length at any angle.
        int side = CN.convertToPixels(9f);
        double length = side * 0.78;
        double width = side * 0.40;
        Image image = Image.createImage(side, side, 0);
        Graphics g = image.getGraphics();
        g.setAntiAliased(true);
        double angle = Math.toRadians(heading);
        // The body with its corners cut, as across (x) and along (y), in
        // halves of the width and the length; first the edge, then the body
        // inside it, then the two windows.
        double[] bodyX = {-0.62, 0.62, 1, 1, 0.7, -0.7, -1, -1};
        double[] bodyY = {1, 1, 0.72, -0.8, -1, -1, -0.8, 0.72};
        g.setColor(style.getBgColor());
        shape(g, bodyX, bodyY, width / 2 + side * 0.035, length / 2 + side * 0.035, angle, side);
        g.setColor(style.getFgColor());
        shape(g, bodyX, bodyY, width / 2, length / 2, angle, side);
        g.setColor(style.getBgColor());
        shape(g, new double[] {-0.62, 0.62, 0.74, -0.74}, new double[] {0.5, 0.5, 0.14, 0.14},
                width / 2, length / 2, angle, side);
        shape(g, new double[] {-0.7, 0.7, 0.6, -0.6}, new double[] {-0.42, -0.42, -0.7, -0.7},
                width / 2, length / 2, angle, side);
        return EncodedImage.createFromImage(image, false);
    }

    /// Fills the outline `(x, y)`, scaled and turned about the middle of a
    /// square image `side` across.
    private static void shape(Graphics g, double[] x, double[] y, double scaleX, double scaleY,
            double angle, int side) {
        int[] screenX = new int[x.length];
        int[] screenY = new int[x.length];
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        for (int iter = 0; iter < x.length; iter++) {
            double across = x[iter] * scaleX;
            double along = y[iter] * scaleY;
            // Along is up the screen when the heading is north.
            screenX[iter] = (int) Math.round(side / 2d + across * cos + along * sin);
            screenY[iter] = (int) Math.round(side / 2d + across * sin - along * cos);
        }
        g.fillPolygon(screenX, screenY, x.length);
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

    /// The compass heading from `a` to `b`, in degrees.
    public static double heading(LatLng a, LatLng b) {
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double dLng = Math.toRadians(b.getLongitude() - a.getLongitude());
        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
        double degrees = Math.toDegrees(MathUtil.atan2(y, x));
        return degrees < 0 ? degrees + 360 : degrees;
    }
}
