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
import com.codename1.home.*;
import com.codename1.home.commissioning.*;
import com.codename1.util.AsyncResource;

class SmartHomeJava005Snippet {


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
    String scannedCode = "MT:Y.K9042C00KA0648G00";
    void snippet() throws Exception {
        // tag::smart-home-java-005[]
        Commissioner commissioner = SmartHome.getInstance().getCommissioner();
        if (!commissioner.isSupported()) {
            commissioner.openEcosystemApp();
            return;
        }

        // Validated here, in your own words, rather than failing inside the
        // operating system's sheet with wording about the app.
        SetupPayload payload;
        try {
            payload = SetupPayload.parse(scannedCode);
        } catch (IllegalArgumentException badCode) {
            label.setText(badCode.getMessage());
            return;
        }

        commissioner.commission(new CommissioningRequest()
                        .setSetupPayload(payload)
                        .setSuggestedName("Kettle"))
                .onResult((result, err) -> {
                    if (err != null) {
                        Log.e(err);
                        return;
                    }
                    if (result.wasCommissionedToThisApp()) {
                        SmartHome.getInstance().refresh();
                    } else {
                        // Added to the user's home, and this app cannot
                        // address it. Say that, rather than showing an empty
                        // device screen.
                        label.setText("Added " + result.getAccessoryName()
                                + " to your home");
                    }
                });
        // end::smart-home-java-005[]
    }
}
