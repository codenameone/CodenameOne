/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Minimal network adapter surface extracted from {@link HTML5Implementation}.
 */
public final class JavaScriptNetworkAdapter {
    private JavaScriptNetworkAdapter() {
    }

    public interface UrlTransformer {
        String transform(String url);
    }

    public interface ConnectionFactory<T> {
        T create(String url, boolean read, boolean write, int timeout) throws IOException;
    }

    public interface Connection {
        void setHeader(String key, String value);
        void setHttpMethod(String method) throws IOException;
        int getContentLength();
        OutputStream openOutputStream() throws IOException;
        InputStream openInputStream() throws IOException;
        void cleanup();
        void setPostRequest(boolean postRequest);
        int getResponseCode() throws IOException;
        String getResponseMessage() throws IOException;
        String getHeaderField(String name) throws IOException;
        String[] getHeaderFieldNames() throws IOException;
        String[] getHeaderFields(String name) throws IOException;
    }

    public interface FileOutputStreamProvider {
        OutputStream openFileOutputStream(String file) throws IOException;
    }

    public static <T> T connect(String url, boolean read, boolean write, int timeout, UrlTransformer transformer, ConnectionFactory<T> factory) throws IOException {
        if (transformer != null) {
            url = transformer.transform(url);
        }
        return factory.create(url, read, write, timeout);
    }

    public static OutputStream openOutputStream(Object connection, FileOutputStreamProvider fileProvider) throws IOException {
        if (connection instanceof String) {
            return fileProvider.openFileOutputStream((String) connection);
        }
        return ((Connection) connection).openOutputStream();
    }

    public static InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            throw new RuntimeException("openInputStream for file types not supported");
        }
        return ((Connection) connection).openInputStream();
    }

    public static void cleanup(Object connection) {
        if (connection instanceof Connection) {
            ((Connection) connection).cleanup();
        }
    }

    public static void setPostRequest(Object connection, boolean postRequest) {
        ((Connection) connection).setPostRequest(postRequest);
    }

    public static void setHeader(Object connection, String key, String value) {
        ((Connection) connection).setHeader(key, value);
    }

    public static void setHttpMethod(Object connection, String method) throws IOException {
        ((Connection) connection).setHttpMethod(method);
    }

    public static int getContentLength(Object connection) {
        return ((Connection) connection).getContentLength();
    }

    public static int getResponseCode(Object connection) throws IOException {
        return ((Connection) connection).getResponseCode();
    }

    public static String getResponseMessage(Object connection) throws IOException {
        return ((Connection) connection).getResponseMessage();
    }

    public static String getHeaderField(String name, Object connection) throws IOException {
        return ((Connection) connection).getHeaderField(name);
    }

    public static String[] getHeaderFieldNames(Object connection) throws IOException {
        return ((Connection) connection).getHeaderFieldNames();
    }

    public static String[] getHeaderFields(String name, Object connection) throws IOException {
        return ((Connection) connection).getHeaderFields(name);
    }
}
