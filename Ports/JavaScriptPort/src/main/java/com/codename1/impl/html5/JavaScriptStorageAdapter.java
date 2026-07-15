/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
