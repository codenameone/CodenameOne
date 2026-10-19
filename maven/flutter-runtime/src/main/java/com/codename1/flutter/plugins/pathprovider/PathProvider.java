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
package com.codename1.flutter.plugins.pathprovider;

import dart.async.Future;
import dart.io.Directory;

/// path_provider's top-level functions, mapped onto Codename One's app storage:
/// the documents and support directories live under the app's private home
/// (`FileSystemStorage.getAppHomePath`), the temporary and cache ones in its
/// caches directory. Directories that only Android exposes answer null, as
/// path_provider does on a platform without them.
public final class PathProvider {
    private PathProvider() {
    }

    private static Future<Directory> under(Directory base, String name) {
        Directory d = name == null ? base : new Directory(base.path() + "/" + name);
        if (!d.existsSync()) {
            d.createSync(Boolean.TRUE);
        }
        return Future.value(d);
    }

    public static Future<Directory> getApplicationDocumentsDirectory() {
        return under(Directory.current, null);
    }

    public static Future<Directory> getApplicationSupportDirectory() {
        return under(Directory.current, "support");
    }

    public static Future<Directory> getLibraryDirectory() {
        return under(Directory.current, "library");
    }

    public static Future<Directory> getTemporaryDirectory() {
        return under(Directory.systemTemp, null);
    }

    public static Future<Directory> getApplicationCacheDirectory() {
        return under(Directory.systemTemp, null);
    }

    public static Future<Directory> getDownloadsDirectory() {
        return under(Directory.current, "downloads");
    }

    public static Future<Directory> getExternalStorageDirectory() {
        return Future.value(null);
    }

    public static Future<Object> getExternalCacheDirectories() {
        return Future.value(null);
    }

    public static Future<Object> getExternalStorageDirectories(Object type) {
        return Future.value(null);
    }
}
