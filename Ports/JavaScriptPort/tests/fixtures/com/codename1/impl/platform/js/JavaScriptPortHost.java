/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.platform.js;

/**
 * Minimal host bridge contract for JavaScript port smoke fixtures running through ParparVM.
 */
public final class JavaScriptPortHost {
    private JavaScriptPortHost() {
    }

    public static native int bootstrap(int apiVersion);

    public static native int resourceThemeChecksum(int resourceId);

    public static native int networkFetchStatus(int requestId);

    public static native int storageWriteRead(int key, int value);

    public static native int databaseWriteRead(int key, int value);

    public static native int browserNavigateAndEval(int pageId);

    public static native int mediaPlayAndQuery(int mediaId);

    public static native int dispatchPointer(int x, int y);
}
