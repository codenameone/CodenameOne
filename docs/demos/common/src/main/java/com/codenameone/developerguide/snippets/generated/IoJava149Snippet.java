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


class IoJava149Snippet {


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
    
    // tag::io-java-149[]
    public class MyApplication {
        private Form current;

        public void init(Object context) {
            try {
                Resources theme = Resources.openLayered("/theme");
                UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        public void start() {
            if(current != null){
                current.show();
                return;
            }
            final Form soc = new Form("Socket Test");
            Button btn = new Button("Create Server");
            Button connect = new Button("Connect");
            final TextField host = new TextField("127.0.0.1");
            // the handle is the only way to close the listening socket, so a second
            // tap would otherwise leave the first accept thread running for good
            Socket.StopListening[] listening = new Socket.StopListening[1];
            btn.addActionListener((evt) -> {
                if(listening[0] != null) {
                    listening[0].stop();
                }
                soc.addComponent(new Label("Listening: " + Socket.getHostOrIP()));
                soc.revalidate();
                listening[0] = Socket.listen(5557, SocketListenerCallback.class);
            });
            connect.addActionListener((evt) -> {
                Socket.connect(host.getText(), 5557, new SocketConnection() {
                    @Override
                    public void connectionError(int errorCode, String message) {
                        System.out.println("Error");
                    }

                    @Override
                    public void connectionEstablished(InputStream is, OutputStream os) {
                        try {
                            int counter = 1;
                            while(isConnected()) {
                                os.write(("Hi: " + counter).getBytes());
                                counter++;
                                Thread.sleep(2000);
                            }
                        } catch(Exception err) {
                            err.printStackTrace();
                        }
                    }
                });
            });
            soc.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            soc.addComponent(btn);
            soc.addComponent(connect);
            soc.addComponent(host);
            soc.show();
        }

        public static class SocketListenerCallback extends SocketConnection {
            private Label connectionLabel;

            @Override
            public void connectionError(int errorCode, String message) {
                System.out.println("Error");
            }

            private void updateLabel(final String t) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if(connectionLabel == null) {
                            connectionLabel = new Label(t);
                            Display.getInstance().getCurrent().addComponent(connectionLabel);
                        } else {
                            connectionLabel.setText(t);
                        }
                        Display.getInstance().getCurrent().revalidate();
                    }
                });
            }

            @Override
            public void connectionEstablished(InputStream is, OutputStream os) {
                try {
                    byte[] buffer = new byte[8192];
                    while(isConnected()) {
                        int size = is.read(buffer, 0, 8192);
                        if(size == -1) {
                            return;
                        }
                        if(size > 0) {
                            updateLabel(new String(buffer, 0, size));
                        }
                    }
                } catch(Exception err) {
                    err.printStackTrace();
                }
            }
        }

        public void stop() {
            current = Display.getInstance().getCurrent();
        }

        public void destroy() {
        }
    }
    // end::io-java-149[]
}
