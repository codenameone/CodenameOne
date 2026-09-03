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
import com.codename1.io.rest.*;
import com.codename1.xml.*;
import com.codename1.ui.tree.*;
import com.codename1.ui.table.*;
import com.codename1.db.*;
import com.codename1.io.gzip.*;
import com.codename1.util.*;
import com.codename1.system.*;
import com.codename1.annotations.*;
import com.codename1.io.services.*;
import java.util.*;


class IoJava096Snippet {


    Object context;
    String url = "https://example.com";
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
    String myUrl = "https://example.com";
    String baseUrl = "https://example.com";
    String token = "token";
    String myToken = "token";
    String password = "password";
    String user = "user";
    String email = "user@example.com";
    String fullPathToFile = "/path/to/file.txt";
    String bodyValueAsString = "{}";
    String petId = "1";
    Result result;
    ConnectionRequest request;
    java.io.Reader reader;
    java.io.Writer writer;
    java.io.InputStream input;
    java.io.OutputStream outputStream;
    
    void snippet() throws Exception {
        // tag::io-java-096[]
        Toolbar.setGlobalToolbar(true);
        Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
        FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_QUERY_BUILDER, s);
        Form hi = new Form("SQL Explorer", new BorderLayout());
        hi.getToolbar().addCommandToRightBar("", icon, (e) -> {
            TextArea query = new TextArea(3, 80);
            Command ok = new Command("Execute");
            Command cancel = new Command("Cancel");
            if(Dialog.show("Query", query, ok, cancel) == ok) {
                Database db = null;
                Cursor cur = null;
                try {
                    db = Display.getInstance().openOrCreate("MyDB.db");
                    String sql = query.getText().trim().toLowerCase();
                    boolean writeCte = sql.startsWith("with") &&
                            (sql.indexOf("insert") > -1 || sql.indexOf("update") > -1 ||
                             sql.indexOf("delete") > -1);
                    boolean returnsRows = !writeCte &&
                            (sql.startsWith("select") || sql.startsWith("with") ||
                             sql.startsWith("pragma") || sql.startsWith("explain") ||
                             sql.startsWith("values"));
                    if(returnsRows) {
                        cur = db.executeQuery(query.getText());
                        int columns = cur.getColumnCount();
                        hi.removeAll();
                        if(columns > 0) {
                            boolean next = cur.next();
                            if(next) {
                                ArrayList<String[]> data = new ArrayList<>();
                                String[] columnNames = new String[columns];
                                for(int iter = 0 ; iter < columns ; iter++) {
                                    columnNames[iter] = cur.getColumnName(iter);
                                }
                                while(next) {
                                    Row currentRow = cur.getRow();
                                    String[] currentRowArray = new String[columns];
                                    for(int iter = 0 ; iter < columns ; iter++) {
                                        currentRowArray[iter] = currentRow.getString(iter);
                                    }
                                    data.add(currentRowArray);
                                    next = cur.next();
                                }
                                Object[][] arr = new Object[data.size()][];
                                data.toArray(arr);
                                hi.add(BorderLayout.CENTER, new Table(new DefaultTableModel(columnNames, arr)));
                            } else {
                                hi.add(BorderLayout.CENTER, "Query returned no results");
                            }
                        } else {
                            hi.add(BorderLayout.CENTER, "Query returned no results");
                        }
                    } else {
                        db.execute(query.getText());
                        hi.add(BorderLayout.CENTER, "Query completed successfully");
                    }
                    hi.revalidate();
                } catch(IOException err) {
                    Log.e(err);
                    hi.removeAll();
                    hi.add(BorderLayout.CENTER, "Error: " + err);
                    hi.revalidate();
                } finally {
                    Util.cleanup(db);
                    Util.cleanup(cur);
                }
            }
        });
        hi.show();
        // end::io-java-096[]
    }
}
