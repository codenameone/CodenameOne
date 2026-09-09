/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.services;

import com.codename1.ui.Display;
import dart.async.Future;

/**
 * System clipboard access, mirroring Flutter's {@code Clipboard}. Backed by
 * CN1's {@code Display.copyToClipboard}/{@code getPasteDataFromClipboard}.
 */
public abstract class Clipboard {

    private Clipboard() {
    }

    public static Future<Object> setData(ClipboardData data) {
        if (data != null && Display.isInitialized()) {
            Display.getInstance().copyToClipboard(data.text());
        }
        return Future.value((Object) null);
    }

    public static Future<Object> getData(String format) {
        Object contents = Display.isInitialized() ? Display.getInstance().getPasteDataFromClipboard() : null;
        return Future.value((Object) new ClipboardData(contents == null ? null : contents.toString()));
    }
}
