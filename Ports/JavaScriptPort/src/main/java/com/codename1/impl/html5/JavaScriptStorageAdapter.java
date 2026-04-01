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
 * Minimal storage adapter surface extracted from {@link HTML5Implementation}.
 */
public final class JavaScriptStorageAdapter {
    private JavaScriptStorageAdapter() {
    }

    public interface Backend {
        void removeItem(String key) throws IOException;
        OutputStream openOutputStream(String key) throws IOException;
        InputStream openInputStream(String key) throws IOException;
        Object getItem(String key) throws IOException;
        int getSize(String key) throws IOException;
        String[] keys() throws IOException;
    }

    public static void deleteStorageFile(Backend backend, String name) throws IOException {
        backend.removeItem(JavaScriptRuntimeFacade.wrapStorageKey(name));
    }

    public static OutputStream createStorageOutputStream(Backend backend, String name) throws IOException {
        return backend.openOutputStream(JavaScriptRuntimeFacade.wrapStorageKey(name));
    }

    public static InputStream createStorageInputStream(Backend backend, String name) throws IOException {
        return backend.openInputStream(JavaScriptRuntimeFacade.wrapStorageKey(name));
    }

    public static boolean storageFileExists(Backend backend, String name) throws IOException {
        return backend.getItem(JavaScriptRuntimeFacade.wrapStorageKey(name)) != null;
    }

    public static int getStorageEntrySize(Backend backend, String name) throws IOException {
        return backend.getSize(JavaScriptRuntimeFacade.wrapStorageKey(name));
    }

    public static String[] listStorageEntries(Backend backend) throws IOException {
        return JavaScriptRuntimeFacade.unwrapStorageEntries(backend.keys());
    }
}
