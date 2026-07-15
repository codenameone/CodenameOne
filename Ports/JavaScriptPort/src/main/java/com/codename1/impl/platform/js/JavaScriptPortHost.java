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
package com.codename1.impl.platform.js;

/**
 * Host-native bridge for ParparVM-backed JavaScript port bootstrap and smoke/runtime probes.
 *
 * <p>The initial methods are intentionally narrow and map onto the existing worker
 * host-call protocol used by the ParparVM JavaScript runtime tests. They provide
 * a stable bridge surface while the broader JavaScript port runtime is integrated.</p>
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
