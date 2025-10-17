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

package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides a similar API to {@code URL} making it almost into a "drop in" replacement.
 * It is placed in a different package because it is incompatible to {@code URL} by definition.  It is useful
 * in getting some simple code to work without too many changes
 *
 * @author Shai Almog
 */
public class URL {
    private URI u;

    public URL(java.lang.String url) throws URISyntaxException {
        u = new URI(url);
    }

    URL(URI u) {
        this.u = u;
    }

    public java.lang.String getQuery() {
        return u.getQuery();
    }

    public java.lang.String getPath() {
        return u.getPath();
    }

    public java.lang.String getUserInfo() {
        return u.getUserInfo();
    }

    public java.lang.String getAuthority() {
        return u.getAuthority();
    }

    public int getPort() {
        return u.getPort();
    }

    public int getDefaultPort() {
        if (u.toASCIIString().startsWith("https")) {
            return 443;
        }
        return 80;
    }

    public java.lang.String getProtocol() {
        String s = u.toASCIIString();
        if (s.startsWith("https")) {
            return "https";
        }
        if (s.startsWith("file")) {
            return "file";
        }
        return "http";
    }

    public java.lang.String getHost() {
        return u.getHost();
    }

    public java.lang.String getFile() {
        return u.toASCIIString();
    }

    public boolean equals(java.lang.Object o) {
        return o instanceof URL && ((URL) o).u.equals(o);
    }

    public synchronized int hashCode() {
        return u.hashCode();
    }

    public boolean sameFile(URL u) {
        return equals(u);
    }

    public java.lang.String toString() {
        return u.toASCIIString();
    }

    public java.lang.String toExternalForm() {
        return u.toASCIIString();
    }

    public java.net.URI toURI() throws java.net.URISyntaxException {
        return u;
    }

    public URLConnection openConnection() throws java.io.IOException {
        if ("file".equals(getProtocol())) {
            return new FileURLConnection(new File(u));
        }
        return new HttpURLConnection(u.toASCIIString());
    }

    public final java.io.InputStream openStream() throws java.io.IOException {
        return openConnection().getInputStream();
    }


    public abstract class URLConnection {
        final Map<String, String> properties = new HashMap<String, String>();
        int connectTimeout;
        int readTimeout;
        boolean doInput = true;
        boolean doOutput;

        public abstract void connect() throws java.io.IOException;

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public URL getURL() {
            return URL.this;
        }

        public abstract int getContentLength();

        public long getContentLengthLong() {
            return getContentLength();
        }

        public abstract java.lang.String getContentType();

        public abstract java.lang.String getHeaderField(java.lang.String s);

        public abstract java.util.Map<java.lang.String, java.util.List<java.lang.String>> getHeaderFields();

        public abstract java.io.InputStream getInputStream() throws java.io.IOException;

        public abstract java.io.OutputStream getOutputStream() throws java.io.IOException;

        /**
         * Sets the request property, replacing existing property with same key, if it already exists.
         *
         * @param key   The key of the request property.
         * @param value The value of the request property.
         * @since 7.0
         */
        public void setRequestProperty(String key,
                                       String value) {
            properties.put(key, value);

        }

        public boolean getDoInput() {
            return doInput;
        }

        public void setDoInput(boolean i) {
            doInput = i;
        }

        public boolean getDoOutput() {
            return doOutput;
        }

        public void setDoOutput(boolean i) {
            doOutput = i;
        }
    }

    private class FileURLConnection extends URLConnection {
        private File file;

        private FileURLConnection(File f) {
            this.file = f;
        }

        @Override
        public void connect() throws IOException {
            if (!file.exists()) {
                throw new IOException("Failed to connect to file because it doesn't exist");
            }
        }

        @Override
        public int getContentLength() {
            return (int) file.length();
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public String getHeaderField(String s) {
            return null;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return new HashMap();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return FileSystemStorage.getInstance().openInputStream(file.getAbsolutePath());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return FileSystemStorage.getInstance().openOutputStream(file.getAbsolutePath());
        }

    }

    public class HttpURLConnection extends URLConnection {
        private String url;
        private Object connection;
        private CodenameOneImplementation impl;
        private String requestMethod = "GET";

        HttpURLConnection(String url) {
            this.url = url;
            impl = Util.getImplementation();
        }

        @Override
        public void connect() throws IOException {
            connection = impl.connect(url, doInput, doOutput);
            impl.setHttpMethod(connection, requestMethod);
            if (properties != null && !properties.isEmpty()) {
                for (String key : properties.keySet()) {
                    impl.setHeader(connection, key, properties.get(key));
                }
            }
        }

        public void setRequestMethod(String method) {
            requestMethod = method;
            if (connection != null) {
                try {
                    impl.setHttpMethod(connection, requestMethod);
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Failed to set request method " + method + ". " + ex.getMessage());
                }
            }
        }

        @Override
        public int getContentLength() {
            return impl.getContentLength(connection);
        }

        @Override
        public String getContentType() {
            return getHeaderField("Content-Type");
        }

        @Override
        public String getHeaderField(String s) {
            try {
                return impl.getHeaderField(s, connection);
            } catch (IOException err) {
                return null;
            }
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            try {
                String[] dat = impl.getHeaderFieldNames(connection);
                Map<String, List<String>> response = new HashMap<String, List<String>>();
                for (String s : dat) {
                    String[] vals = impl.getHeaderFields(s, connection);
                    ArrayList<String> a = new ArrayList<String>();
                    response.put(s, a);
                    for (String c : vals) {
                        a.add(c);
                    }
                }
                return response;
            } catch (IOException err) {
                return null;
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (connection == null) {
                connect();
            }
            return impl.openInputStream(connection);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            if (connection == null) {
                connect();
            }
            return impl.openOutputStream(connection);
        }


    }
}
