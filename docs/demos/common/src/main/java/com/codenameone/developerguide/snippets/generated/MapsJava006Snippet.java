package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.maps.routing.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;


class MapsJava006Snippet {

    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    LatLng origin = new LatLng(38.8977, -77.0365);
    LatLng destination = new LatLng(38.8894, -77.0352);
    final MapView map = new MapView();
    void snippet() throws Exception {
        // tag::maps-java-006[]
        RouteRequest request = new RouteRequest(origin, destination)
                .addWaypoint(new LatLng(38.8899, -77.0091))
                .setTravelMode(TravelMode.DRIVING);

        Routing.findRoute(request, new RouteCallback() {
            @Override
            public void routesFound(java.util.List routes) {
                Route best = (Route)routes.get(0);
                map.addPolyline(best.toPolyline().setStrokeColor(0xff5722).setStrokeWidth(6));
                map.fitBounds(best.getBounds(), CN.convertToPixels(4));
                label.setText((int)(best.getDistanceMeters() / 1000) + " km, "
                        + (int)(best.getDurationSeconds() / 60) + " min");
            }

            @Override
            public void routeFailed(String message, Throwable error) {
                label.setText(message);
            }
        });
        // end::maps-java-006[]
    }
}
