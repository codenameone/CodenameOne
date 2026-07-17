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
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattNotificationListener;
import com.codename1.bluetooth.le.BlePeripheral;

class BluetoothJava003Snippet {

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
    BlePeripheral peripheral;
    void updateHeartRateLabel(int bpm) {
    }
    void snippet() throws Exception {
        // tag::bluetooth-java-003[]
        GattCharacteristic measurement = peripheral.getCharacteristic(
                BluetoothUuid.fromShort(0x180D),
                BluetoothUuid.fromShort(0x2A37));
        GattNotificationListener listener = (characteristic, data) -> {
            // fires on the EDT for every notification while subscribed
            updateHeartRateLabel(data[1] & 0xFF);
        };
        measurement.subscribe(listener).onResult((armed, err) -> {
            if (err != null) {
                // the CCCD write was rejected or the link dropped
            }
        });

        // later, e.g. when leaving the form:
        measurement.unsubscribe(listener);
        // end::bluetooth-java-003[]
    }
}
