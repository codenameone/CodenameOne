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
package com.codenameone.examples.dash;

import com.codename1.maps.LatLng;
import com.codenameone.examples.dash.rules.Track;
import com.codenameone.examples.dash.rules.CityRoute;
import com.codenameone.examples.dash.run.Levels;
import com.codenameone.examples.dash.run.Landmarks;
import com.codename1.maps.routing.Route;
import com.codename1.maps.routing.RouteCallback;
import com.codename1.maps.routing.RouteLeg;
import com.codename1.maps.routing.RouteRequest;
import com.codename1.maps.routing.RouteService;
import com.codename1.maps.routing.RouteStep;
import com.codename1.maps.routing.Routing;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codenameone.examples.dash.run.RunView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// A city run: two taps on the street map, the route between them, and the
/// run down it with the minimap lit as it goes. The route comes from a
/// stand-in service, so the test never leans on the public routing server.
public class CityRunTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        final RouteService real = Routing.getService();
        Routing.setService(new Canned());
        Landmarks.setSource(new CannedPlaces());
        try {
            signIn(PLAYER);
            waitForForm("home");
            click("runButton");
            waitForForm("levels");
            touch("cityButton");
            waitForForm("city");
            until(() -> findByName("cityMap") != null && findByName("cityMap").getWidth() > 0, 20000, "the map");
            // Give the street tiles a moment to arrive for the screenshot.
            waitFor(4000);
            Component map = findByName("cityMap");
            int x = map.getAbsoluteX();
            int y = map.getAbsoluteY();
            touch(x + map.getWidth() / 4, y + map.getHeight() * 2 / 3);
            waitFor(500);
            touch(x + map.getWidth() * 3 / 4, y + map.getHeight() / 4);
            until(() -> ((Button) findByName("runCity")).isEnabled(), 20000, "the route");
            waitFor(2500);
            shot("city-route");
            click("runCity");
            waitForForm("run");
            final RunView view = track();
            while (view.driver().run().tick() < 60 * 12 && !driveFor(view, 200)) {
                // the bot steers for the first stretch
            }
            assertTrue(has("minimap"), "the route's minimap");
            assertTrue(has("street"), "the street the runner is on");
            shot("city-run", view);
            // Just short of the first landmark, with its board in view.
            CityRoute route = Levels.cityRoute(view.driver().run().level().id);
            assertTrue(route != null && route.landmarks().size() >= 3, "landmarks along the route");
            int mark = -1;
            for (CityRoute.Sign sign : route.signs()) {
                if (sign.kind >= 0 && sign.metres > 120) {
                    mark = sign.metres;
                    break;
                }
            }
            final int ahead = mark - 45;
            long give = System.currentTimeMillis() + 60000;
            while (view.driver().run().runner(view.driver().me()).z / Track.UNIT < ahead
                    && System.currentTimeMillis() < give && !driveFor(view, 100)) {
                // the bot steers up to the landmark
            }
            assertTrue(!view.driver().run().over(), "still running at the landmark");
            shot("city-landmark", view);
            finish(view, 60 * 900);
            awaitResult("the result");
            shot("city-result");
            click("done");
            return true;
        } finally {
            Routing.setService(real);
            Landmarks.setSource(new Landmarks.Overpass("https://overpass-api.de/api/interpreter"));
        }
    }

    /// Three streets from wherever the start is: east, a left turn north,
    /// and a right turn east again to the finish.
    static final class Canned implements RouteService {
        @Override
        public String getId() {
            return "canned";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void findRoutes(RouteRequest request, final RouteCallback callback) {
            LatLng a = request.getOrigin();
            // East, north, east again, ending where the finish was tapped so
            // the line on the map meets both pins.
            LatLng d = request.getDestination();
            double share = 520.0 / (520 + 460);
            double midLon = a.getLongitude() + (d.getLongitude() - a.getLongitude()) * share;
            LatLng b = new LatLng(a.getLatitude(), midLon);
            LatLng c = new LatLng(d.getLatitude(), midLon);
            List<RouteStep> steps = new ArrayList<RouteStep>();
            steps.add(new RouteStep("Head east", "Cannon Street", 520, 400, a, Arrays.asList(a, b)));
            steps.add(new RouteStep("Turn left", "Moorgate", 380, 300, b, Arrays.asList(b, c)));
            steps.add(new RouteStep("Turn right", "London Wall", 460, 350, c, Arrays.asList(c, d)));
            steps.add(new RouteStep("Arrive", "London Wall", 0, 0, d, Arrays.asList(d)));
            final Route r = new Route(Arrays.asList(a, b, c, d),
                    Arrays.asList(new RouteLeg("", 1360, 1050, steps)), 1360, 1050, "");
            CN.callSerially(() -> callback.routesFound(Arrays.asList(r)));
        }
    }

    /// Real places of the City of London, set along the canned route.
    static final class CannedPlaces implements Landmarks.Source {
        private static final Object[][] PLACES = {
            {0.12, CityRoute.WORSHIP, "St Mary-le-Bow"},
            {0.3, CityRoute.MUSEUM, "Bank of England Museum"},
            {0.48, CityRoute.MONUMENT, "Guildhall"},
            {0.62, CityRoute.STATION, "Moorgate Station"},
            {0.8, CityRoute.PARK, "Finsbury Circus Gardens"},
        };

        @Override
        public void find(List<LatLng> path, Landmarks.Callback done) {
            List<Landmarks.Place> out = new ArrayList<Landmarks.Place>();
            for (Object[] p : PLACES) {
                double t = (Double) p[0] * (path.size() - 1);
                int i = Math.min(path.size() - 2, (int) t);
                double f = t - i;
                LatLng a = path.get(i);
                LatLng b = path.get(i + 1);
                LatLng at = new LatLng(a.getLatitude() + (b.getLatitude() - a.getLatitude()) * f,
                        a.getLongitude() + (b.getLongitude() - a.getLongitude()) * f);
                out.add(new Landmarks.Place(at, (Integer) p[1], (String) p[2]));
            }
            CN.callSerially(() -> done.found(out));
        }
    }
}
