/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.audioplayers;

import com.codename1.flutter.FlutterAssets;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;

/**
 * audioplayers' {@code AssetSource}. The path is relative to the AudioCache prefix,
 * {@code assets/}, as in the plugin; the asset is the flattened resource the build wrote
 * for that key (see {@link FlutterAssets#resourceName}).
 */
public class AssetSource extends Source {
    static final String PREFIX = "assets/";
    private final String path;

    public AssetSource(String path) {
        this(path, null);
    }

    public AssetSource(String path, String mimeType) {
        super(mimeType);
        this.path = path;
    }

    public String path() {
        return path;
    }

    String assetKey() {
        return path.startsWith(PREFIX) ? path : PREFIX + path;
    }

    @Override
    Media open(Runnable onComplete) throws IOException {
        InputStream in = Display.getInstance().getResourceAsStream(AssetSource.class,
                FlutterAssets.resourceName(assetKey()));
        if (in == null) {
            throw new IOException("audioplayers: no asset " + assetKey());
        }
        return MediaManager.createMedia(in, guessMime(path, mimeType()), onComplete);
    }
}
