package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import java.io.InputStream;
import java.io.OutputStream;

public class SocketTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        // We do not use createForm() because it registers a timer that calls done().
        // We want to control done() ourselves after socket interaction.
        Form form = new Form("Sockets", new BorderLayout());
        Label status = new Label("Connecting...");
        form.add(BorderLayout.CENTER, status);
        form.show();

        if (!Socket.isSupported()) {
            status.setText("Sockets not supported");
            done();
            return true;
        }

        Socket.connect("google.com", 80, new SocketConnection() {
            @Override
            public void connectionEstablished(InputStream is, OutputStream os) {
                Display.getInstance().callSerially(() -> {
                    status.setText("Connected. Sending request...");
                    form.revalidate();
                });
                try {
                    os.write("GET / HTTP/1.1\r\nHost: google.com\r\n\r\n".getBytes());
                    os.flush();
                } catch (Exception e) {
                    fail("Write failed: " + e.getMessage());
                }
            }

            @Override
            public void connectionError(int errorCode, String message) {
                 Display.getInstance().callSerially(() -> {
                    fail("Connection error: " + message);
                });
            }

            @Override
            public void messageReceived(InputStream is, OutputStream os) {
                // We received something, that's enough for coverage
                Display.getInstance().callSerially(() -> {
                     status.setText("Message received. Success.");
                     form.revalidate();
                     done();
                });
                // We do not assume we can close the socket gracefully here easily as SocketConnection
                // is managed by the implementation.
            }
        });

        return true;
    }
}
