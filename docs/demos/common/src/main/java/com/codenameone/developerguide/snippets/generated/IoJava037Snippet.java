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


class IoJava037Snippet {

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
    void snippet() throws Exception {
        // tag::io-java-037[]
        Form hi = new Form("Location", new BoxLayout(BoxLayout.Y_AXIS));
        hi.add("Pinpointing Location");
        Display.getInstance().callSerially(() -> {
            Location l = Display.getInstance().getLocationManager().getCurrentLocationSync();
            ConnectionRequest request = new ConnectionRequest("http://maps.googleapis.com/maps/api/geocode/json", false) {
                private String country;
                private String region;
                private String city;
                private String json;

                @Override
                protected void readResponse(InputStream input) throws IOException {
                        Result result = Result.fromContent(input, Result.JSON);
                        country = result.getAsString("/results/address_components[types='country']/long_name");
                        region = result.getAsString("/results/address_components[types='administrative_area_level_1']/long_name");
                        city = result.getAsString("/results/address_components[types='locality']/long_name");
                        json = result.toString();
                }

                @Override
                protected void postResponse() {
                    hi.removeAll();
                    hi.add(country);
                    hi.add(region);
                    hi.add(city);
                    hi.add(new SpanLabel(json));
                    hi.revalidate();
                }
            };
            request.setContentType("application/json");
            request.addRequestHeader("Accept", "application/json");
            request.addArgument("sensor", "true");
            request.addArgument("latlng", l.getLatitude() + "," + l.getLongitude());

            NetworkManager.getInstance().addToQueue(request);
        });
        hi.show();
        /* omitted */
        // end::io-java-037[]
    }
}
