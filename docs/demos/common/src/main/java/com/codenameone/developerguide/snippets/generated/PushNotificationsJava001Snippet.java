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


class PushNotificationsJava001Snippet {

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
    // tag::push-notifications-java-001[]
    public class MyApplication implements PushCallback {

        // /* omitted */

        /**
         * Invoked when the push notification occurs
         *
         * @param value the value of the push notification
         */
        public void push(String value) {
            System.out.println("Received push message: "+value);
        }

        /**
         * Invoked when push registration is complete to pass the device ID to the application.
         *
         * @param deviceId OS native push ID you should not use this value and instead use <code>Push.getPushKey()</code>
         * @see Push#getPushKey()
         */
        public void registeredForPush(String deviceId) {
            System.out.println("The Push ID for this device is "+Push.getPushKey());
        }

        /**
         * Invoked to indicate an error occurred during registration for push notification
         * @param error descriptive error string
         * @param errorCode an error code
         */
        public void pushRegistrationError(String error, int errorCode) {
            System.out.println("An error occurred during push registration.");
        }
    }
    // end::push-notifications-java-001[]
}
