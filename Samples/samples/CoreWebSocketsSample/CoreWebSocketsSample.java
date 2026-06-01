package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.io.WebSocket;
import com.codename1.io.WebSocketState;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.io.IOException;

/// Mirrors WebSocketsSample but uses the core `com.codename1.io.WebSocket`
/// API -- no cn1lib dependency, no `//require` directive, no subclassing.
/// Each callback is wired as a lambda via the fluent setters.
public class CoreWebSocketsSample {

    private static final String SERVER_URL = "wss://weblite.ca/ws/cn1-websocket-demo/chat";

    private Resources theme;
    private WebSocket sock;
    private Container chatContainer;

    public void init(Object context) {
        try {
            theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (!WebSocket.isSupported()) {
            Dialog.show("WebSocket", "This port does not support WebSocket", "OK", null);
            return;
        }

        sock = WebSocket.build(SERVER_URL)
                .onConnect(w -> Log.p("WebSocket connected"))
                .onTextMessage((w, msg) -> Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (chatContainer == null) {
                            return;
                        }
                        SpanLabel label = new SpanLabel();
                        label.setText(msg);
                        chatContainer.addComponent(label);
                        chatContainer.animateHierarchy(100);
                    }
                }))
                .onBinaryMessage((w, bin) -> Log.p("Received " + bin.length + " bytes of binary"))
                .onClose((w, code, reason) -> Log.p("Closed: " + code + " / " + reason))
                .onError((w, ex) -> Log.e(ex))
                .connect();

        showLogin();
    }

    private void showLogin() {
        Form f = new Form("Login");
        f.addComponent(new Label("Name: "));
        final TextField nameField = new TextField();
        f.addComponent(nameField);
        f.addComponent(new Button(new Command("Login") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (sock.getReadyState() == WebSocketState.OPEN) {
                    sock.send(nameField.getText());
                    showChat();
                } else {
                    Dialog.show("Not Connected", "Socket state: " + sock.getReadyState(), "OK", null);
                }
            }
        }));
        f.show();
    }

    private void showChat() {
        Form f = new Form("Chat");
        f.setLayout(new BorderLayout());

        Container south = new Container();
        final TextField tf = new TextField();
        Button send = new Button(new Command("Send") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (sock.getReadyState() == WebSocketState.OPEN) {
                    sock.send(tf.getText());
                    tf.setText("");
                } else {
                    Dialog.show("Not Connected", "Socket is " + sock.getReadyState(), "OK", null);
                    showLogin();
                }
            }
        });

        chatContainer = new Container();
        chatContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        south.addComponent(tf);
        south.addComponent(send);
        f.addComponent(BorderLayout.SOUTH, south);
        f.addComponent(BorderLayout.CENTER, chatContainer);
        f.setFormBottomPaddingEditingMode(true);
        f.show();
    }

    public void stop() {
    }

    public void destroy() {
        if (sock != null) {
            sock.close();
        }
    }
}
