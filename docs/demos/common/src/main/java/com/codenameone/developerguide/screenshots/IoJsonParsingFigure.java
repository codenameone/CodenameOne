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

package com.codenameone.developerguide.screenshots;

import com.codename1.components.MultiButton;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

/// A list built by parsing a packaged JSON document.
class IoJsonParsingFigure implements GuideFigure {

    @Override
    public String id() {
        return "json-parsing";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it. The document it reads is packaged with
    /// the demo rather than fetched, which is what makes the picture the same
    /// every time it is taken, and it is byte for byte the array the chapter
    /// prints a few lines above and tells the reader to save under this name.
    /// That the array has no "root" key is the point of the first annotation:
    /// JSONParser synthesises one when the document's root is a list.
    @Override
    @SuppressWarnings("unchecked")
    public Form build() {
        // tag::io-java-032[]
        Form hi = new Form("JSON Parsing", new BoxLayout(BoxLayout.Y_AXIS));
        JSONParser json = new JSONParser();
        try (Reader r = new InputStreamReader(
                Display.getInstance().getResourceAsStream(getClass(), "/anapioficeandfire.json"), "UTF-8")) {
            Map<String, Object> data = json.parseJSON(r);
            java.util.List<Map<String, Object>> content =
                    (java.util.List<Map<String, Object>>) data.get("root"); // <1>
            for (Map<String, Object> obj : content) { // <2>
                String url = (String) obj.get("url");
                String name = (String) obj.get("name");
                java.util.List<String> titles = (java.util.List<String>) obj.get("titles"); // <3>
                if (name == null || name.length() == 0) {
                    java.util.List<String> aliases = (java.util.List<String>) obj.get("aliases");
                    if (aliases != null && aliases.size() > 0) {
                        name = aliases.get(0);
                    }
                }
                MultiButton mb = new MultiButton(name);
                if (titles != null && titles.size() > 0) {
                    mb.setTextLine2(titles.get(0));
                }
                mb.addActionListener((e) -> Display.getInstance().execute(url));
                hi.add(mb);
            }
        } catch (IOException err) {
            Log.e(err);
        }
        hi.show();
        // end::io-java-032[]
        return hi;
    }
}
