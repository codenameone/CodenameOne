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


class TheComponentsOfCodenameOneJava208Snippet {


    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    
    // tag::the-components-of-codename-one-java-208[]
      Form hi = new Form("Autocomplete", new BoxLayout(BoxLayout.Y_AXIS));
    public void showForm() {
      final DefaultListModel<String> options = new DefaultListModel<>();
      AutoCompleteTextField ac = new AutoCompleteTextField(options) {
          @Override
          protected boolean filter(String text) {
              if(text.length() == 0) {
                  // an emptied field must not keep showing the last query's matches
                  options.removeAll();
                  return false;
              }
              String[] l = searchLocations(text);
              if(l == null || l.length == 0) {
                  // otherwise the popup keeps showing the previous query's matches
                  options.removeAll();
                  return false;
              }

              options.removeAll();
              for(String s : l) {
                  options.addItem(s);
              }
              return true;
          }

      };
      ac.setMinimumElementsShownInPopup(5);
      hi.add(ac);
      hi.add(new SpanLabel("This demo requires a valid google API key to be set below "
               + "you can get this key for the webservice (not the native key) by following the instructions here: "
               + "https://developers.google.com/places/web-service/get-api-key"));
      hi.add(apiKey);
      hi.getToolbar().addCommandToRightBar("Get Key", null, e -> Display.getInstance().execute("https://developers.google.com/places/web-service/get-api-key"));
      hi.show();
    }

    TextField apiKey = new TextField();

    String[] searchLocations(String text) {
        try {
            if(text.length() > 0) {
                ConnectionRequest r = new ConnectionRequest();
                r.setPost(false);
                r.setUrl("https://maps.googleapis.com/maps/api/place/autocomplete/json");
                r.addArgument("key", apiKey.getText());
                r.addArgument("input", text);
                NetworkManager.getInstance().addToQueueAndWait(r);
                Map<String,Object> result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
                String[] res = Result.fromContent(result).getAsStringArray("//description");
                return res;
            }
        } catch(Exception err) {
            Log.e(err);
        }
        return null;
    }
    // end::the-components-of-codename-one-java-208[]
}
