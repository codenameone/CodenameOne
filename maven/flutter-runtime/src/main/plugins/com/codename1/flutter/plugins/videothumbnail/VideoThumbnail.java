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
package com.codename1.flutter.plugins.videothumbnail;

import dart.async.Future;
import dart.core.UnsupportedError;
import dart.typed_data.Uint8List;

/**
 * video_thumbnail's VideoThumbnail. Codename One's core has no portable way to
 * decode a frame out of a video -- MediaManager plays media and Capture records it,
 * and neither hands back a picture -- so both calls complete with an
 * UnsupportedError naming that gap instead of answering a blank image. Frame
 * extraction needs a native interface per platform (AVAssetImageGenerator,
 * MediaMetadataRetriever).
 */
public final class VideoThumbnail {
    static final String GAP = "video_thumbnail: Codename One cannot extract a video frame without native code";

    private VideoThumbnail() {
    }

    public static Future<Uint8List> thumbnailData(String video, Object headers, ImageFormat imageFormat,
            Long maxHeight, Long maxWidth, Long timeMs, Long quality) {
        return Future.error(new UnsupportedError(GAP));
    }

    public static Future<String> thumbnailFile(String video, Object headers, String thumbnailPath,
            ImageFormat imageFormat, Long maxHeight, Long maxWidth, Long timeMs, Long quality) {
        return Future.error(new UnsupportedError(GAP));
    }
}
