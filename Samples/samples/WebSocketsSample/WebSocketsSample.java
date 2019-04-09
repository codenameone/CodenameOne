//require cn1-websockets
package com.codename1.samples;


import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.io.websocket.WebSocket;
import com.codename1.io.websocket.WebSocketState;
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
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketsSample {

    private Form current;
    private Resources theme;
    WebSocket sock;
    Container chatContainer;

    //public static final String SERVER_URL="ws://translation.weblite.ca:8080/cn1-websockets-demo/chat";
    public static final String SERVER_URL="wss://weblite.ca/ws/cn1-websocket-demo/chat";
    
    
    public void init(Object context) {
        try {
            theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
        } catch(IOException e){
            e.printStackTrace();
        }
        // Pro users - uncomment this code to get crash reports sent to you automatically
        /*Display.getInstance().addEdtErrorHandler(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                evt.consume();
                Log.p("Exception in AppName version " + Display.getInstance().getProperty("AppVersion", "Unknown"));
                Log.p("OS " + Display.getInstance().getPlatformName());
                Log.p("Error " + evt.getSource());
                Log.p("Current Form " + Display.getInstance().getCurrent().getName());
                Log.e((Throwable)evt.getSource());
                Log.sendLog();
            }
        });*/
        
        
        
    }
    
    void showLogin() {
        Form f = new Form("Login");
        f.addComponent(new Label("Name: "));
        final TextField nameField = new TextField();
        f.addComponent(nameField);
        f.addComponent(new Button(new Command("Login"){ 

            @Override
            public void actionPerformed(ActionEvent evt) {
                System.out.println("Hello");
                if (sock.getReadyState() == WebSocketState.OPEN) {
                    System.out.println("Open");
                    sock.send(nameField.getText());
                    showChat();
                } else {
                    System.out.println("Closed");
                    Dialog.show("Dialog", "The socket is not open: "+sock.getReadyState(), "OK", null);
                    sock.reconnect();
                }
            }
               
        }));
        f.show();
    }
    
    void showChat() {
        Form f= new Form("Chat");
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
                    Dialog.show("", "The socket is not open", "OK", null);
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
    
    private void testTimeouts() {
        WebSocket ws = new WebSocket("http://10.0.1.77/ws") {
            @Override
            protected void onError(Exception ex) {
                Log.e(ex);
            }

            @Override
            protected void onOpen() {
                Log.p("Opened");
            }

            @Override
            protected void onClose(int statusCode, String reason) {
                Log.p("Closed "+statusCode+", "+reason);
            }

            @Override
            protected void onMessage(String message) {
                
            }

            @Override
            protected void onMessage(byte[] message) {
                
            }
        };
        System.out.println("Trying to connect");
        ws.connect(5000);
        Form f = new Form("Hello");
        f.add(new Label("World"));
        f.show();
    }
    
    public void start() {
        if (false) {
            testTimeouts();
            return;
        }
        System.out.println("About to start socket");
        
        sock = new WebSocket(SERVER_URL) {

            @Override
            protected void onOpen() {
                System.out.println("In onOpen");
                System.out.println("Ready state: "+sock.getReadyState());
            }

            @Override
            protected void onClose(int statusCode, String reason) {
                System.out.println("Closing: "+sock.getReadyState());
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                         showLogin();
                    }
                });
               
            }

            @Override
            protected void onMessage(final String message) {
                System.out.println("Received message "+message);
                System.out.println("Ready state: "+sock.getReadyState());
                Display.getInstance().callSerially(new Runnable() {

                    public void run() {
                        if (chatContainer == null) {
                            return;
                        }
                        SpanLabel label = new SpanLabel();
                        label.setText(message);
                        chatContainer.addComponent(label);
                        chatContainer.animateHierarchy(100);
                    }
                    
                });
            }

            @Override
            protected void onError(Exception ex) {
                
                if (sock == null) {
                    System.out.println("Error while socket is null: "+ex.getMessage());
                } else {
                    
                    System.out.println("Ready state: "+sock.getReadyState());
                    System.out.println("in onError "+ex.getMessage());
                    //Log.e(ex);
                }
            }

             @Override
             protected void onMessage(byte[] message) {
                 System.out.println("Received bytes "+message.length);
                 System.out.println(Arrays.toString(message));
                 
             }
            
        }.autoReconnect(10000);
        
        System.out.println("Sending connect");
        System.out.println("Ready State: "+sock.getReadyState());
        sock.connect();
        try {
            sock.send("Test Message");
        } catch (Throwable t) {
            t.printStackTrace();
        }
        showLogin();
        
       
        
        
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }
    
    public void destroy() {
    }

}
