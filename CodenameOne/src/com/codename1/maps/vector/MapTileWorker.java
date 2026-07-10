/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

import com.codename1.ui.CN;
import com.codename1.util.EasyThread;

/// Shared serial worker for map tile IO, decoding and raster preparation.
/// The front-end asks for jobs from the EDT, then receives only the small
/// completion callback back on the EDT.
final class MapTileWorker {

    private static EasyThread worker;

    private MapTileWorker() {
    }

    static void run(Runnable job) {
        thread().run(job);
    }

    static void callSerially(Runnable r) {
        CN.callSerially(r);
    }

    private static synchronized EasyThread thread() {
        if (worker == null) {
            worker = EasyThread.start("Map Tile Worker");
        }
        return worker;
    }
}
