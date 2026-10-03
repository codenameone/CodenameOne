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
package android.content;

import android.net.Uri;

/// Content providers are not available; queries answer nothing. Present so
/// code that touches the resolver for optional features still compiles.
public class ContentResolver {

    public static final String SCHEME_CONTENT = "content";
    public static final String SCHEME_FILE = "file";
    public static final String SCHEME_ANDROID_RESOURCE = "android.resource";


    public ContentResolver(Context context) {
    }

    public final java.io.InputStream openInputStream(Uri uri) throws java.io.FileNotFoundException {
        String s = uri.toString();
        try {
            if ("file".equals(uri.getScheme())) {
                return com.codename1.io.FileSystemStorage.getInstance().openInputStream(uri.getPath());
            }
            if (s.startsWith("file:") || s.startsWith("/")) {
                return com.codename1.io.FileSystemStorage.getInstance().openInputStream(uri.getPath());
            }
        } catch (java.io.IOException e) {
            throw new java.io.FileNotFoundException(s);
        }
        throw new java.io.FileNotFoundException("No content provider: " + s);
    }

    public final java.io.OutputStream openOutputStream(Uri uri) throws java.io.FileNotFoundException {
        try {
            return com.codename1.io.FileSystemStorage.getInstance().openOutputStream(uri.getPath());
        } catch (java.io.IOException e) {
            throw new java.io.FileNotFoundException(uri.toString());
        }
    }

    public final String getType(Uri uri) {
        return null;
    }

    public final void notifyChange(Uri uri, Object observer) {
    }
}
