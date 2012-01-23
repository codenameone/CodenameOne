/*
 * Copyright (c) 2012, Codename One. All rights reserved.
 */
package com.codename1.designer;

import com.codename1.server.EchoServlet;
import com.codename1.server.LivePreviewServlet;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Local HTTP server currently based on Jetty providing proxy functionality for devices
 * thus allowing faster connection without cloud round trip for devices residing within
 * the same IP range.
 *
 * @author Shai Almog
 */
public class LocalServer {
    private static int port = -1;
    
    
    public static int getPort() {
        if(port == -1) {
            int currentPort = 9000;
            while(true) {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(currentPort);
                    ss.setReuseAddress(true);
                } catch (Throwable e) {
                    currentPort++;
                    continue;
                } finally {
                    try {
                        ss.close();
                    } catch (Throwable e) {
                        /* should not be thrown */
                    }
                }
                port = currentPort;
                break;
            }
        }
        return port;
    }
    
    private static boolean started;
    public static void startServer(final JComponent root) {
        if(started) {
            return;
        }
        started = true;
        new Thread() {
            public void run() {
                try {
                    Server server = new Server(getPort());

                    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                    context.setContextPath("/");
                    server.setHandler(context);

                    context.addServlet(new ServletHolder(new EchoServlet()),"/echo");
                    context.addServlet(new ServletHolder(new LivePreviewServlet()),"/preview");

                    server.start();
                    server.join();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(root, "Error in HTTP Local Server: " + ex, "Local HTTP Server Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.start();
    }
}
