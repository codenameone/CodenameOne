/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.codename1.designer.css;
 
//import android.content.res.AssetManager;
//import android.text.TextUtils;
//import android.util.Log;
 
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
/**
 * Implementation of a very basic HTTP server. The contents are loaded from the assets folder. This
 * server handles one request at a time. It only supports GET method.
 */
class SimpleWebServer implements Runnable {
 
    private static final String TAG = "SimpleWebServer";
    
    private Map<String,Router> routers = new HashMap<String,Router>();
    private List<Sniffer> sniffers = new ArrayList<Sniffer>();
    
    
    interface Router {
        public byte[] getContent();
    }
    
    interface Sniffer {
        public String getMimetype(String fileName);
    }
    
    public SimpleWebServer route(String fileName, Router router) {
        routers.put(fileName, router);
        return this;
    }
    
    public SimpleWebServer sniff(Sniffer sniffer) {
        sniffers.add(sniffer);
        return this;
    }
 
    /**
     * The port number we listen to
     */
    private int mPort;
    
    private final File docRoot;
 
 
    public boolean isRunning() {
        return mIsRunning;
    }
    
    /**
     * True if the server is running.
     */
    private boolean mIsRunning;
 
    /**
     * The {@link java.net.ServerSocket} that we listen to.
     */
    private ServerSocket mServerSocket;
 
    /**
     * WebServer constructor.
     */
    public SimpleWebServer(int port, File docRoot) {
        mPort = port;
        this.docRoot = docRoot;
        route("/ping", () -> {
            return "OK".getBytes();
        });

    }
 
    /**
     * This method starts the web server listening to the specified port.
     */
    public void start() {
        mIsRunning = true;
        new Thread(this).start();
    }
    
    public boolean waitForServer(int timeout) {
        long start = System.currentTimeMillis();
        long expires = start + timeout;
        while (System.currentTimeMillis() < expires) {
            if (ping()) {
                return true;
            }
            if (CN.isEdt()) {
                CN.invokeAndBlock(()->{
                    
                    Util.sleep(100);
                });
            } else {
                Util.sleep(100);
            }
        }
        return false;
    }
    
    public boolean ping() {
        if (!mIsRunning) {
            return false;
        }
        if (mServerSocket == null) {
            return false;
        }
        int port = mServerSocket.getLocalPort();
        try {
            URL u = new URL("http://localhost:"+port+"/ping");
            InputStream stream = u.openStream();
            byte[] buf = new byte[128];
            StringBuilder sb = new StringBuilder();
            int len = 0;
            while ((len = stream.read(buf)) >= 0) {
                if (len > 0) {
                    sb.append(new String(buf, 0, len));
                }
            }
            return sb.toString().trim().endsWith("OK");
        } catch (Exception ex) {
            return false;
        }
    }
 
    /**
     * This method stops the web server
     */
    public void stop() {
        try {
            mIsRunning = false;
            if (null != mServerSocket) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            Log.p("Error closing the server socket.");
            Log.e(e);
        }
    }
 
    public int getPort() {
        return mPort;
    }
 
    @Override
    public void run() {
        try {
            mServerSocket = new ServerSocket(mPort);
            if (mPort <= 0) {
                mPort = mServerSocket.getLocalPort();
            }
            while (mIsRunning) {
                Socket socket = mServerSocket.accept();
                handle(socket);
                socket.close();
            }
        } catch (SocketException e) {
            // The server was stopped; ignore.
        } catch (IOException e) {
            Log.p("Web server error.");
            Log.e(e);
            
        }
    }
 
    private static class TextUtils {
        static boolean isEmpty(String str) {
            return str == null || str.isEmpty();
        }
    }
    
    /**
     * Respond to a request from a client.
     *
     * @param socket The client socket.
     * @throws IOException
     */
    private void handle(Socket socket) throws IOException {
        BufferedReader reader = null;
        PrintStream output = null;
        try {
            String route = null;
 
            // Read HTTP headers and parse out the route.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (!TextUtils.isEmpty(line = reader.readLine())) {
                if (line.startsWith("GET /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = line.substring(start, end);
                    break;
                }
            }
 
            // Output stream that we send the response to
            output = new PrintStream(socket.getOutputStream());
            // Prepare the content to send.
            if (null == route) {
                writeServerError(output);
                return;
            }
            byte[] bytes = loadContent(route);
            if (null == bytes) {
                writeServerError(output);
                return;
            }
            
            // Send out the content.
            output.println("HTTP/1.0 200 OK");
            output.println("Content-Type: " + detectMimeType(route));
            output.println("Content-Length: " + bytes.length);
            output.println();
            output.write(bytes);
            output.flush();
        } finally {
            if (null != output) {
                output.close();
            }
            if (null != reader) {
                reader.close();
            }
        }
    }
 
    /**
     * Writes a server error response (HTTP/1.0 500) to the given output stream.
     *
     * @param output The output stream.
     */
    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }
 
    /**
     * Loads all the content of {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return The content of the file.
     * @throws IOException
     */
    private byte[] loadContent(String fileName) throws IOException {
        String absPath = fileName;
        if (!absPath.startsWith("/")) {
            absPath = "/" + absPath;
        }
        if (routers.containsKey(absPath)) {
            return routers.get(absPath).getContent();
        }
        InputStream input = null;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            File file = new File(docRoot, fileName);
            input = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int size;
            while (-1 != (size = input.read(buffer))) {
                output.write(buffer, 0, size);
            }
            output.flush();
            return output.toByteArray();
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (null != input) {
                input.close();
            }
        }
    }
 
    /**
     * Detects the MIME type from the {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return A MIME type.
     */
    private String detectMimeType(String fileName) {
        for (Sniffer sniffer : sniffers) {
            String mime = sniffer.getMimetype(fileName);
            if (mime != null) {
                return mime;
            }
        }
        if (TextUtils.isEmpty(fileName)) {
            return null;
        } else if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else {
            return "application/octet-stream";
        }
    }
 
}