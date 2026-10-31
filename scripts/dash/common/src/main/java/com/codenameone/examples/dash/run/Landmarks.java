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
package com.codenameone.examples.dash.run;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.maps.LatLng;
import com.codename1.ui.CN;
import com.codenameone.examples.dash.rules.CityRoute;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The named places a route passes: landmarks, churches, museums, parks,
/// stations and monuments within a short walk of it, each placed by how far
/// along the route it stands so a city run can show it beside the track.
///
/// The default source asks OpenStreetMap's data through the public Overpass
/// API, which is rate limited and has no service guarantee; point
/// [#setSource] at your own Overpass server before you ship. Tests set a
/// source of their own.
public final class Landmarks {
    /// A place found near the route.
    public static final class Place {
        public final LatLng at;
        public final int kind;
        public final String name;

        public Place(LatLng at, int kind, String name) {
            this.at = at;
            this.kind = kind;
            this.name = name;
        }
    }

    /// Finds places near a path. Answers on the EDT, an empty list when
    /// there is nothing or the lookup failed: landmarks are a nicety, never a
    /// reason a run cannot start.
    public interface Source {
        void find(List<LatLng> path, Callback done);
    }

    public interface Callback {
        void found(List<Place> places);
    }

    /// The most landmarks a run shows, and how far apart they stand at least.
    static final int MAX = 12;
    private static final int SPACING = 60;
    private static final int RADIUS = 70;
    private static final int MAX_QUERY_POINTS = 40;

    private static Source source = new Overpass("https://overpass-api.de/api/interpreter");

    private Landmarks() {
    }

    public static void setSource(Source s) {
        source = s;
    }

    /// Looks up the places along `path` and adds the best of them to
    /// `route`, then calls `done` with how many were added.
    public static void addTo(final CityRoute route, final List<LatLng> path, final Runnable done) {
        source.find(path, places -> {
            place(route, path, places);
            done.run();
        });
    }

    /// Projects each place onto the path and keeps those far enough apart,
    /// in route order. Scaled from the path's own length to the route's, as
    /// the routed distance and the drawn line differ a little.
    static void place(CityRoute route, List<LatLng> path, List<Place> places) {
        if (path.size() < 2 || places.isEmpty()) {
            return;
        }
        double[] along = new double[path.size()];
        for (int i = 1; i < path.size(); i++) {
            along[i] = along[i - 1] + metres(path.get(i - 1), path.get(i));
        }
        double scale = along[along.length - 1] <= 0 ? 0 : route.metres() / along[along.length - 1];
        List<int[]> spots = new ArrayList<int[]>();
        for (int p = 0; p < places.size(); p++) {
            LatLng at = places.get(p).at;
            double best = Double.MAX_VALUE;
            double where = 0;
            for (int i = 1; i < path.size(); i++) {
                LatLng a = path.get(i - 1);
                LatLng b = path.get(i);
                double seg = along[i] - along[i - 1];
                double t = seg <= 0 ? 0 : project(a, b, at);
                double ex = a.getLatitude() + (b.getLatitude() - a.getLatitude()) * t;
                double ey = a.getLongitude() + (b.getLongitude() - a.getLongitude()) * t;
                double d = metres(at, new LatLng(ex, ey));
                if (d < best) {
                    best = d;
                    where = along[i - 1] + seg * t;
                }
            }
            if (best <= RADIUS * 1.5) {
                spots.add(new int[] {(int) (where * scale), p});
            }
        }
        // Route order, then keep each one only if it is clear of the last.
        for (int i = 1; i < spots.size(); i++) {
            int[] x = spots.get(i);
            int j = i - 1;
            while (j >= 0 && spots.get(j)[0] > x[0]) {
                spots.set(j + 1, spots.get(j));
                j--;
            }
            spots.set(j + 1, x);
        }
        int last = -SPACING;
        int added = 0;
        for (int i = 0; i < spots.size() && added < MAX; i++) {
            int[] s = spots.get(i);
            if (s[0] - last < SPACING) {
                continue;
            }
            Place pl = places.get(s[1]);
            try {
                route.landmark(s[0], pl.kind, pl.name);
                last = s[0];
                added++;
            } catch (IllegalArgumentException e) {
                // A name that cleans to nothing; skip it.
            }
        }
    }

    /// Where `p` falls along `a`-`b`, from 0 to 1, in a flat local frame.
    private static double project(LatLng a, LatLng b, LatLng p) {
        double k = Math.cos(Math.toRadians(a.getLatitude()));
        double bx = (b.getLongitude() - a.getLongitude()) * k;
        double by = b.getLatitude() - a.getLatitude();
        double px = (p.getLongitude() - a.getLongitude()) * k;
        double py = p.getLatitude() - a.getLatitude();
        double len = bx * bx + by * by;
        if (len <= 0) {
            return 0;
        }
        double t = (px * bx + py * by) / len;
        return t < 0 ? 0 : t > 1 ? 1 : t;
    }

    /// The distance between two points in metres, flat-earth: good for the
    /// few kilometres a run covers.
    public static double metres(LatLng a, LatLng b) {
        double k = Math.cos(Math.toRadians((a.getLatitude() + b.getLatitude()) / 2));
        double dx = (b.getLongitude() - a.getLongitude()) * k * 111320.0;
        double dy = (b.getLatitude() - a.getLatitude()) * 110540.0;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /// OpenStreetMap's named places near a line, through an Overpass server.
    public static final class Overpass implements Source {
        private final String url;

        public Overpass(String url) {
            this.url = url;
        }

        @Override
        public void find(List<LatLng> path, final Callback done) {
            StringBuilder line = new StringBuilder();
            int step = Math.max(1, (path.size() + MAX_QUERY_POINTS - 1) / MAX_QUERY_POINTS);
            for (int i = 0; i < path.size(); i += step) {
                append(line, path.get(i));
            }
            append(line, path.get(path.size() - 1));
            String around = "(around:" + RADIUS + "," + line + ")";
            String q = "[out:json][timeout:15];("
                    + "nwr" + around + "[name][tourism~\"attraction|museum|gallery|viewpoint\"];"
                    + "nwr" + around + "[name][historic];"
                    + "nwr" + around + "[name][amenity=place_of_worship];"
                    + "nwr" + around + "[name][railway=station];"
                    + "nwr" + around + "[name][leisure=park];"
                    + ");out center 80;";
            final List<Place> out = new ArrayList<Place>();
            ConnectionRequest r = new ConnectionRequest() {
                @Override
                protected void readResponse(InputStream input) throws IOException {
                    read(JSONParser.parseJSON(com.codename1.io.Util.readInputStream(input)), out);
                }

                @Override
                protected void postResponse() {
                    done.found(out);
                }

                @Override
                protected void handleErrorResponseCode(int code, String message) {
                    CN.callSerially(() -> done.found(new ArrayList<Place>()));
                }

                @Override
                protected void handleException(Exception err) {
                    CN.callSerially(() -> done.found(new ArrayList<Place>()));
                }
            };
            r.setUrl(url);
            r.setPost(true);
            r.addArgument("data", q);
            r.setFailSilently(true);
            NetworkManager.getInstance().addToQueue(r);
        }

        private static void append(StringBuilder sb, LatLng p) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(round(p.getLatitude())).append(',').append(round(p.getLongitude()));
        }

        private static String round(double v) {
            return String.valueOf(Math.round(v * 100000) / 100000.0);
        }

        @SuppressWarnings("unchecked")
        static void read(Map<String, Object> json, List<Place> out) {
            Object els = json.get("elements");
            if (!(els instanceof List)) {
                return;
            }
            for (Object o : (List<Object>) els) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<String, Object> e = (Map<String, Object>) o;
                Object c = e.get("center");
                Map<String, Object> pos = c instanceof Map ? (Map<String, Object>) c : e;
                Object tags = e.get("tags");
                if (!(tags instanceof Map) || !(pos.get("lat") instanceof Number)
                        || !(pos.get("lon") instanceof Number)) {
                    continue;
                }
                Map<String, Object> t = (Map<String, Object>) tags;
                Object name = t.get("name");
                if (!(name instanceof String)) {
                    continue;
                }
                out.add(new Place(new LatLng(((Number) pos.get("lat")).doubleValue(),
                        ((Number) pos.get("lon")).doubleValue()), kind(t), (String) name));
            }
        }

        private static int kind(Map<String, Object> t) {
            if ("place_of_worship".equals(t.get("amenity"))) {
                return CityRoute.WORSHIP;
            }
            Object tourism = t.get("tourism");
            if ("museum".equals(tourism) || "gallery".equals(tourism)) {
                return CityRoute.MUSEUM;
            }
            if ("park".equals(t.get("leisure"))) {
                return CityRoute.PARK;
            }
            if ("station".equals(t.get("railway"))) {
                return CityRoute.STATION;
            }
            if (t.get("historic") != null) {
                return CityRoute.MONUMENT;
            }
            return CityRoute.LANDMARK;
        }
    }
}
