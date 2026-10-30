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
package com.codename1.flutter.plugins.videocompress;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Dimension;

import dart.async.Future;
import dart.core.UnsupportedError;
import dart.typed_data.Uint8List;

/**
 * video_compress's VideoCompress. Codename One's core has no video transcoder, so
 * compressVideo does NOT compress: it answers the MediaInfo of the original file,
 * logs a warning saying so, and reports progress 100 at once. An app that uploads
 * the result therefore uploads the original. Real compression needs a native
 * interface per platform (AVAssetExportSession, MediaCodec). The thumbnail calls
 * fail with an UnsupportedError for the same reason as video_thumbnail.
 * deleteOrigin is never honoured -- the original is the result.
 */
public final class VideoCompress {
    private static final ObservableBuilder<Double> PROGRESS = new ObservableBuilder<Double>();

    /** The progress observable, a static getter in Dart. */
    public static final ObservableBuilder<Double> compressProgress$ = PROGRESS;

    /** Nothing is ever being compressed. */
    public static final boolean isCompressing = false;

    private VideoCompress() {
    }

    public static Future<MediaInfo> compressVideo(String path, VideoQuality quality, Boolean deleteOrigin,
            Long startTime, Long duration, Boolean includeAudio, Long frameRate) {
        if (Display.isInitialized()) {
            // Log needs the platform; headless the warning has nowhere to go
            Log.p("video_compress: no transcoder in Codename One; answering the original file uncompressed: " + path,
                    Log.WARNING);
        }
        MediaInfo info = info(path);
        PROGRESS.next(Double.valueOf(100));
        return Future.value(info);
    }

    public static Future<MediaInfo> getMediaInfo(String path) {
        return Future.value(info(path));
    }

    private static MediaInfo info(String path) {
        MediaInfo info = new MediaInfo(path);
        if (!Display.isInitialized() || path == null) {
            return info;
        }
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (fs.exists(path)) {
                info.filesize = Long.valueOf(fs.getLength(path));
            }
            Media m = MediaManager.createMedia(path, true);
            if (m != null) {
                info.duration = Double.valueOf(Math.max(0, m.getDuration()));
                Component c = m.getVideoComponent();
                if (c != null) {
                    Dimension d = c.getPreferredSize();
                    info.width = Long.valueOf(d.getWidth());
                    info.height = Long.valueOf(d.getHeight());
                }
                m.cleanup();
            }
        } catch (Exception e) {
            Log.e(e);
        }
        return info;
    }

    public static Future<dart.io.File> getFileThumbnail(String path, Long quality, Long position) {
        return Future.error(new UnsupportedError("video_compress: Codename One cannot extract a video frame without native code"));
    }

    public static Future<Uint8List> getByteThumbnail(String path, Long quality, Long position) {
        return Future.error(new UnsupportedError("video_compress: Codename One cannot extract a video frame without native code"));
    }

    public static Future<Object> cancelCompression() {
        return Future.value(null);
    }

    /** Nothing is ever cached, so there is nothing to delete. */
    public static Future<Boolean> deleteAllCache() {
        return Future.value(Boolean.TRUE);
    }
}
