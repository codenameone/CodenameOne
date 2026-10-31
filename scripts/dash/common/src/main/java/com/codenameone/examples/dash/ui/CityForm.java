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
package com.codenameone.examples.dash.ui;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.Marker;
import com.codename1.maps.MarkerOptions;
import com.codename1.maps.Polyline;
import com.codename1.maps.routing.Route;
import com.codename1.maps.routing.RouteCallback;
import com.codename1.maps.routing.RouteRequest;
import com.codename1.maps.routing.RouteStep;
import com.codename1.maps.routing.RouteLeg;
import com.codename1.maps.routing.Routing;
import com.codename1.maps.routing.TravelMode;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.dash.rules.CityRoute;
import com.codenameone.examples.dash.run.CityPath;
import com.codenameone.examples.dash.run.Levels;
import java.util.ArrayList;
import java.util.List;

/// Runs real streets. The player taps a start and a finish on a street map,
/// a routing service finds the way between them, and the route becomes a
/// level: each street a stretch of track, each turn a corner taken on its
/// side, the street names called out on the way ([CityRoute]).
///
/// The map is Codename One's own [MapView] over OpenStreetMap data, and the
/// route comes from [Routing], whose default service is OSRM's public demo
/// server: fine for trying this, not for shipping it (see the guide). The
/// server never sees the map. The run's result carries the route, and the
/// server builds the same level from it to check the run.
public final class CityForm {
    /// Where the map opens: a city centre with a grid of named streets.
    static final LatLng START = new LatLng(51.5136, -0.0890);
    /// Steps shorter than this are folded into the street before them: a
    /// slip road or a kink in a junction is not a street to run.
    private static final int MIN_LEG = 40;

    private LatLng from;
    private LatLng to;
    private Marker fromMarker;
    private Marker toMarker;
    private Polyline line;
    private CityRoute route;
    private CityPath path;
    private final MapView map = new MapView();
    private final SpanLabel status = new SpanLabel();
    private final Button run;

    private CityForm() {
        run = Ui.primary("Run it", "runCity", e -> start());
        run.setMaterialIcon(FontImage.MATERIAL_DIRECTIONS_RUN, 4f);
    }

    public static void show() {
        new CityForm().open();
    }

    private void open() {
        Form f = new Form("City run", new BorderLayout());
        f.setName("city");
        f.setUIID("GameForm");
        f.getToolbar().setUIID("GameToolbar");
        f.getToolbar().getTitleComponent().setUIID("GameTitle");
        Ui.back(f, LevelsForm::show);
        map.setName("cityMap");
        map.moveCamera(START, 15);
        map.addTapListener((surface, at, x, y) -> tapped(at));
        status.setUIID("Container");
        status.setTextUIID("CityStatus");
        status.setName("cityStatus");
        Container card = new Container(BoxLayout.y());
        card.setUIID("CityCard");
        card.setSafeArea(true);
        card.add(status);
        card.add(run);
        Container layers = new Container(new LayeredLayout());
        layers.add(map);
        Container over = new Container(new BorderLayout());
        // OpenStreetMap's licence asks for this wherever its data is shown.
        over.add(BorderLayout.NORTH, FlowLayout.encloseRight(
                new com.codename1.ui.Label("(c) OpenStreetMap contributors", "MapCredit")));
        over.add(BorderLayout.SOUTH, card);
        layers.add(over);
        f.add(BorderLayout.CENTER, layers);
        say("Tap the map where the run starts.");
        run.setEnabled(false);
        f.show();
    }

    /// The first tap is the start, the second the finish; a third starts
    /// again from the spot tapped.
    private void tapped(LatLng at) {
        if (from == null || to != null) {
            clear();
            from = at;
            fromMarker = map.addMarker(new MarkerOptions(at).label("Start"));
            say("Now tap where it ends.");
        } else {
            to = at;
            toMarker = map.addMarker(new MarkerOptions(at).label("Finish"));
            find();
        }
    }

    /// Asks for the route between the two points, as given by
    /// [#pick(LatLng, LatLng)] in a test.
    void pick(LatLng a, LatLng b) {
        clear();
        tapped(a);
        tapped(b);
    }

    private void clear() {
        if (fromMarker != null) {
            map.removeMarker(fromMarker);
        }
        if (toMarker != null) {
            map.removeMarker(toMarker);
        }
        if (line != null) {
            map.removePolyline(line);
        }
        from = null;
        to = null;
        fromMarker = null;
        toMarker = null;
        line = null;
        route = null;
        run.setEnabled(false);
    }

    private void find() {
        say("Finding the way...");
        final LatLng a = from;
        final LatLng b = to;
        Routing.findRoute(new RouteRequest(a, b).setTravelMode(TravelMode.WALKING).setSteps(true),
                new RouteCallback() {
                    @Override
                    public void routesFound(List routes) {
                        if (a != from || b != to) {
                            // Superseded by newer taps.
                            return;
                        }
                        if (routes == null || routes.isEmpty()) {
                            say("No way through from there. Try two points on streets.");
                            return;
                        }
                        found((Route) routes.get(0));
                    }

                    @Override
                    public void routeFailed(String message, Throwable error) {
                        if (a == from && b == to) {
                            say("The route service did not answer. Try again in a moment.");
                        }
                    }
                });
    }

    private void found(Route r) {
        CityRoute c = toRoute(r);
        if (c == null) {
            say("That is too short a run. Pick a finish further away.");
            return;
        }
        route = c;
        path = CityPath.of(points(r));
        line = map.addPolyline(r.toPolyline().setStrokeColor(0xd4ff3a).setStrokeWidth(CN.convertToPixels(1.2f)));
        map.fitBounds(r.getBounds(), CN.convertToPixels(14));
        int turns = 0;
        for (CityRoute.Leg l : c.legs()) {
            if (l.turn != CityRoute.STRAIGHT) {
                turns++;
            }
        }
        say(c.place + "\n" + (c.metres() >= 1000 ? c.metres() / 100 / 10.0 + " km" : c.metres() + " m")
                + ", " + c.legs().size() + (c.legs().size() == 1 ? " street, " : " streets, ")
                + turns + (turns == 1 ? " turn" : " turns"));
        run.setEnabled(true);
        run.getComponentForm().revalidate();
    }

    private void start() {
        if (route == null) {
            return;
        }
        Levels.Playable p = Levels.city(route, path);
        // The route's own seed: everyone on this route runs the same track.
        RunForm.solo(p, p.level.contentHash(), "city", null, CityForm::show);
    }

    private void say(String text) {
        status.setText(text);
        Component c = status.getParent();
        if (c != null && c.getComponentForm() != null) {
            c.getComponentForm().revalidate();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<LatLng> points(Route r) {
        return (List<LatLng>) r.getPoints();
    }

    /// Reduces a routed path to the streets and turns a run needs: steps
    /// on the same street run together, short ones fold into the one before,
    /// and each turn is read off the geometry where two streets meet.
    /// Null when nothing is left to run.
    static CityRoute toRoute(Route r) {
        List<RouteStep> steps = new ArrayList<RouteStep>();
        for (Object leg : r.getLegs()) {
            for (Object s : ((RouteLeg) leg).getSteps()) {
                RouteStep step = (RouteStep) s;
                if (step.getDistanceMeters() >= 1) {
                    steps.add(step);
                }
            }
        }
        if (steps.isEmpty()) {
            return null;
        }
        List<int[]> lengths = new ArrayList<int[]>();
        List<String> names = new ArrayList<String>();
        RouteStep prev = null;
        for (RouteStep s : steps) {
            String name = s.getRoadName() == null ? "" : s.getRoadName();
            int metres = (int) Math.round(s.getDistanceMeters());
            int turn = prev == null ? CityRoute.STRAIGHT : turn(prev, s);
            int last = lengths.size() - 1;
            if (last >= 0 && (metres < MIN_LEG || name.equals(names.get(last)) && turn == CityRoute.STRAIGHT)) {
                lengths.get(last)[0] += metres;
            } else {
                lengths.add(new int[] {metres, turn});
                names.add(name);
            }
            prev = s;
        }
        String first = firstNamed(names, false);
        String lastName = firstNamed(names, true);
        String place = first.length() == 0 ? "City run"
                : lastName.length() == 0 || lastName.equals(first) ? first : first + " to " + lastName;
        if (place.length() > CityRoute.MAX_NAME) {
            place = first.length() > 0 ? first : "City run";
        }
        CityRoute c = new CityRoute(place);
        int legs = Math.min(lengths.size(), CityRoute.MAX_LEGS);
        for (int i = 0; i < legs; i++) {
            int[] l = lengths.get(i);
            // Past the cap, the rest of the way is the last street, run on.
            int metres = l[0];
            if (i == legs - 1) {
                for (int j = legs; j < lengths.size(); j++) {
                    metres += lengths.get(j)[0];
                }
            }
            c.leg(Math.min(metres, 100000), l[1], names.get(i));
        }
        return c.metres() < 100 ? null : c;
    }

    private static String firstNamed(List<String> names, boolean fromEnd) {
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(fromEnd ? names.size() - 1 - i : i);
            if (n.length() > 0) {
                return n;
            }
        }
        return "";
    }

    /// Left, right or straight on from `a` into `b`: the angle between the
    /// last stretch of one and the first of the other, on a local flat
    /// projection. Within 30 degrees is straight on.
    static int turn(RouteStep a, RouteStep b) {
        List pa = a.getPoints();
        List pb = b.getPoints();
        if (pa == null || pb == null || pa.size() < 2 || pb.size() < 2) {
            return CityRoute.STRAIGHT;
        }
        LatLng a0 = (LatLng) pa.get(pa.size() - 2);
        LatLng a1 = (LatLng) pa.get(pa.size() - 1);
        LatLng b0 = (LatLng) pb.get(0);
        LatLng b1 = (LatLng) pb.get(1);
        double k = Math.cos(Math.toRadians(a1.getLatitude()));
        double ax = (a1.getLongitude() - a0.getLongitude()) * k;
        double ay = a1.getLatitude() - a0.getLatitude();
        double bx = (b1.getLongitude() - b0.getLongitude()) * k;
        double by = b1.getLatitude() - b0.getLatitude();
        double la = Math.sqrt(ax * ax + ay * ay);
        double lb = Math.sqrt(bx * bx + by * by);
        if (la == 0 || lb == 0 || (ax * bx + ay * by) / (la * lb) > 0.866) {
            return CityRoute.STRAIGHT;
        }
        // East is x, north is y: a positive cross product turns
        // anticlockwise, which is to the left.
        return ax * by - ay * bx > 0 ? CityRoute.LEFT : CityRoute.RIGHT;
    }
}
