// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;

import com.codename1.io.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

/**
 * The example for the second scheme with domain handling is a more
 * complex example and is taken from the parent project CEF. Please
 * see CEF: "cefclient/scheme_test.cpp" for futher details
 */
public class ClientSchemeHandler extends CefResourceHandlerAdapter {
    public static final String scheme = "client";
    public static final String domain = "tests";

    private byte[] data_;
    private String mime_type_;
    private int offset_ = 0;

    public ClientSchemeHandler() {
        super();
    }

    @Override
    public synchronized boolean processRequest(CefRequest request, CefCallback callback) {
        boolean handled = false;
        String url = request.getURL();
        
        if (url.indexOf("/streams/") != -1) {
            File f = new File("/Users/shannah/tmp/test.mp3");
            try (FileInputStream fis = new FileInputStream(f)){
                data_ = new byte[(int)f.length()];
                fis.read(data_);
                mime_type_ = "audio/mp3";
                handled = true;
            } catch (IOException ex) {
                Log.e(ex);
            }
        }
        /*
        if (url.indexOf("/streams/") != -1) {
            System.out.println("Processing "+url);
            String streamId = url.substring(url.lastIndexOf("/")+1);
            StreamWrapper w = BrowserPanel.getStreamRegistry().getStream(streamId);
            data_ = new byte[(int)w.getLength()];
            mime_type_ = w.getMimeType();
            if (mime_type_ == null) {
                mime_type_ = "audio/wav";
            }
            try {
                w.getStream().read(data_);
                handled = true;
            } catch (IOException ex) {
                Log.e(ex);
            } finally {
                try {
                    w.getStream().close();
                } catch (Throwable t){}
                w.setStream(new ByteArrayInputStream(data_));
                //BrowserPanel.getStreamRegistry().removeStream(w);
            }
            
            
            
        }
*/
        if (url.indexOf("handler.html") != -1) {
            // Build the response html
            String html;
            html = "<html><head><title>Client Scheme Handler</title></head>"
                    + "<body bgcolor=\"white\">"
                    + "This contents of this page page are served by the "
                    + "ClientSchemeHandler class handling the client:// protocol."
                    + "<br/>You should see an image:"
                    + "<br/><img src=\"client://tests/logo.png\"><pre>";

            // Output a string representation of the request
            html += request.toString();

            html += "</pre><br/>Try the test form:"
                    + "<form method=\"POST\" action=\"handler.html\">"
                    + "<input type=\"text\" name=\"field1\">"
                    + "<input type=\"text\" name=\"field2\">"
                    + "<input type=\"submit\">"
                    + "</form></body></html>";

            data_ = html.getBytes();

            handled = true;
            // Set the resulting mime type
            mime_type_ = "text/html";
        } else if (url.endsWith(".png")) {
            handled = loadContent(url.substring(url.lastIndexOf('/') + 1));
            mime_type_ = "image/png";
        } else if (url.endsWith(".html")) {
            handled = loadContent(url.substring(url.lastIndexOf('/') + 1));
            mime_type_ = "text/html";
            if (!handled) {
                String html = "<html><head><title>Error 404</title></head>";
                html += "<body><h1>Error 404</h1>";
                html += "File  " + url.substring(url.lastIndexOf('/') + 1) + " ";
                html += "does not exist</body></html>";
                data_ = html.getBytes();
                handled = true;
            }
        }

        if (handled) {
            // Indicate the headers are available.
            callback.Continue();
            return true;
        }

        return false;
    }

    @Override
    public void getResponseHeaders(
            CefResponse response, IntRef response_length, StringRef redirectUrl) {
        response.setMimeType(mime_type_);
        response.setStatus(200);

        // Set the resulting response length
        response_length.set(data_.length);
        System.out.println("Response length: "+data_.length);
    }

    @Override
    public synchronized boolean readResponse(
            byte[] data_out, int bytes_to_read, IntRef bytes_read, CefCallback callback) {
        boolean has_data = false;

        if (offset_ < data_.length) {
            // Copy the next block of data into the buffer.
            int transfer_size = Math.min(bytes_to_read, (data_.length - offset_));
            System.arraycopy(data_, offset_, data_out, 0, transfer_size);
            offset_ += transfer_size;
            System.out.println("Read "+transfer_size+".  Offset now "+offset_);
            bytes_read.set(transfer_size);
            has_data = true;
        } else {
            offset_ = 0;
            bytes_read.set(0);
        }

        return has_data;
    }

    private boolean loadContent(String resName) {
        InputStream inStream = getClass().getResourceAsStream(resName);
        if (inStream != null) {
            try {
                ByteArrayOutputStream outFile = new ByteArrayOutputStream();
                int readByte = -1;
                while ((readByte = inStream.read()) >= 0) outFile.write(readByte);
                data_ = outFile.toByteArray();
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }
}
