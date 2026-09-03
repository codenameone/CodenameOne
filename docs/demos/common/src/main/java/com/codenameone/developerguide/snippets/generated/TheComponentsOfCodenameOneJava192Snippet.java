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


class TheComponentsOfCodenameOneJava192Snippet {


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
        // tag::the-components-of-codename-one-java-192[]
        Image duke;
        try {
            duke = Image.createImage("/duke.png");
        } catch(IOException err) {
            Log.e(err);
            return; // without the icon there is nothing to show
        }
        int fiveMM = Display.getInstance().convertToPixels(5);
        final Image finalDuke = duke.scaledWidth(fiveMM);
        Toolbar.setGlobalToolbar(true);
        Form hi = new Form("Search", BoxLayout.y());
        hi.add(new InfiniteProgress());
        Display.getInstance().scheduleBackgroundTask(()-> {
            // this will take a while...
            Contact[] cnts = Display.getInstance().getAllContacts(true, true, true, true, false, false);
            Display.getInstance().callSerially(() -> {
                hi.removeAll();
                for(Contact c : cnts) {
                    MultiButton m = new MultiButton();
                    m.setTextLine1(c.getDisplayName());
                    m.setTextLine2(c.getPrimaryPhoneNumber());
                    Image pic = c.getPhoto();
                    if(pic != null) {
                        m.setIcon(pic.fill(finalDuke.getWidth(), finalDuke.getHeight()));
                    } else {
                        m.setIcon(finalDuke);
                    }
                    hi.add(m);
                }
                // the query may have been typed while the contacts were loading
                filterContacts(hi, currentSearch);
                hi.revalidate();
            });
        });

        hi.getToolbar().addSearchCommand(e -> {
            currentSearch = (String)e.getSource();
            filterContacts(hi, currentSearch);
        }, 4);

        hi.show();

        // end::the-components-of-codename-one-java-192[]
    }



    // tag::the-components-of-codename-one-java-192-filter[]
    String currentSearch;

    void filterContacts(Form hi, String text) {
        if(text == null || text.length() == 0) {
            // clear search
            for(Component cmp : hi.getContentPane()) {
                cmp.setHidden(false);
                cmp.setVisible(true);
            }
            hi.getContentPane().animateLayout(150);
            return;
        }
        text = text.toLowerCase();
        for(Component cmp : hi.getContentPane()) {
            if(!(cmp instanceof MultiButton)) {
                // the loading indicator is still there until the contacts arrive
                continue;
            }
            MultiButton mb = (MultiButton)cmp;
            String line1 = mb.getTextLine1();
            String line2 = mb.getTextLine2();
            boolean show = line1 != null && line1.toLowerCase().indexOf(text) > -1 ||
                    line2 != null && line2.toLowerCase().indexOf(text) > -1;
            mb.setHidden(!show);
            mb.setVisible(show);
        }
        hi.getContentPane().animateLayout(150);
    }
    // end::the-components-of-codename-one-java-192-filter[]
}
