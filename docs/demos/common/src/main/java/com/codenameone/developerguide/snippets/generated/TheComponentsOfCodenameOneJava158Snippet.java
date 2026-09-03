/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
import com.codename1.components.ToastBar.Status;
import com.codename1.maps.layers.*;
import com.codename1.charts.*;
import com.codename1.ui.validation.*;
import com.codename1.xml.*;
import com.codename1.charts.util.*;
import com.codename1.javascript.*;
import com.codename1.ui.tree.*;
import com.codename1.ui.table.*;
import com.codename1.contacts.*;
import java.util.*;


class TheComponentsOfCodenameOneJava158Snippet {


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
        // tag::the-components-of-codename-one-java-158[]
        MultiButton twoLinesNoIcon = new MultiButton("MultiButton");
        twoLinesNoIcon.setTextLine2("Line 2");
        MultiButton oneLineIconEmblem = new MultiButton("Icon + Emblem");
        oneLineIconEmblem.setIcon(icon);
        oneLineIconEmblem.setEmblem(emblem);
        MultiButton twoLinesIconEmblem = new MultiButton("Icon + Emblem");
        twoLinesIconEmblem.setIcon(icon);
        twoLinesIconEmblem.setEmblem(emblem);
        twoLinesIconEmblem.setTextLine2("Line 2");

        MultiButton twoLinesIconEmblemHorizontal = new MultiButton("Icon + Emblem");
        twoLinesIconEmblemHorizontal.setIcon(icon);
        twoLinesIconEmblemHorizontal.setEmblem(emblem);
        twoLinesIconEmblemHorizontal.setTextLine2("Line 2 Horizontal");
        twoLinesIconEmblemHorizontal.setHorizontalLayout(true);

        MultiButton twoLinesIconCheckBox = new MultiButton("CheckBox");
        twoLinesIconCheckBox.setIcon(icon);
        twoLinesIconCheckBox.setCheckBox(true);
        twoLinesIconCheckBox.setTextLine2("Line 2");

        MultiButton fourLinesIcon = new MultiButton("With Icon");
        fourLinesIcon.setIcon(icon);
        fourLinesIcon.setTextLine2("Line 2");
        fourLinesIcon.setTextLine3("Line 3");
        fourLinesIcon.setTextLine4("Line 4");

        hi.add(oneLineIconEmblem).
                add(twoLinesNoIcon).
                add(twoLinesIconEmblem).
                add(twoLinesIconEmblemHorizontal).
                add(twoLinesIconCheckBox).
                add(fourLinesIcon);
        // end::the-components-of-codename-one-java-158[]
    }

    Image icon;
    Image emblem;

}
