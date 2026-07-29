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
import com.codename1.surfaces.*;
import com.codename1.wearable.*;
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


class WearablesJava001Snippet {

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
    Label stepsLabel;
    int stepCount = 0;
    void showWorkout(String id) {
    }
    String beginWorkout() {
        return "w1";
    }
    void snippet() throws Exception {
        // tag::wearables-java-001[]
        Form f = new Form(BoxLayout.y());
        if (CN.isWatch()) {
            // Compact, single-column layout suited to a small round/square screen
            f.add(new Label("Hi Watch"));
            f.getToolbar().setVisible(false);
        } else {
            // Full phone/tablet layout
            f.add(new SpanLabel("Welcome to the full size application"));
        }
        f.show();
        // end::wearables-java-001[]

        // tag::wearables-java-002[]
        // On the phone: publish the value the watch should show whenever it next wakes.
        WearableConnection.putData(new WearableMessage("/steps")
                .put("count", stepCount)
                .put("goalReached", stepCount >= 10000));
        // end::wearables-java-002[]

        // tag::wearables-java-003[]
        // On the watch: react to it. Register from init(), not from a form -- a value that
        // arrived while the app was starting is replayed only to listeners that exist by then.
        WearableConnection.addDataListener(new WearableDataListener() {
            public void dataChanged(WearableMessage data) {
                stepsLabel.setText("" + data.getInt("count", 0));
            }

            public void dataRemoved(String path) {
                stepsLabel.setText("--");
            }
        });
        // end::wearables-java-003[]

        // tag::wearables-java-004[]
        // Ask the phone something and use the answer. Only works while both apps are awake,
        // so check first and fall back to what you already replicated.
        if (WearableConnection.isReachable()) {
            WearableConnection.sendMessage(new WearableMessage("/workout/start"),
                    new WearableReplyHandler() {
                        public void replyReceived(WearableMessage reply) {
                            showWorkout(reply.getString("id", null));
                        }

                        public void replyFailed(String message) {
                            Log.p("Could not start the workout: " + message);
                        }
                    });
        }
        // end::wearables-java-004[]

        // tag::wearables-java-005[]
        // Answer the watch. Reply quickly and do slow work afterwards -- the sender is waiting.
        WearableConnection.addMessageListener(new WearableMessageListener() {
            public WearableMessage messageReceived(WearableMessage message, boolean expectsReply) {
                if ("/workout/start".equals(message.getPath())) {
                    return new WearableMessage("/workout/start").put("id", beginWorkout());
                }
                return null;
            }
        });
        // end::wearables-java-005[]

        // tag::wearables-java-006[]
        // A complication is a widget in a watch family, published from the same timeline.
        WidgetKind steps = new WidgetKind("steps")
                .setDisplayName("Steps")
                .addSupportedSize(WidgetSize.WATCH_CIRCULAR)
                .addSupportedSize(WidgetSize.WATCH_RECTANGULAR);
        // end::wearables-java-006[]
    }
}
