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
            until(() -> view.driver().run().tick() > 60 * 12, 30000, "the run");
            assertTrue(has("minimap"), "the route's minimap");
            shot("city-run");
            finish(view, 60 * 900);
            awaitResult("the result");
            shot("city-result");
            click("done");
            return true;
        } finally {
            Routing.setService(real);
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
}
