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
package com.codename1.flutter.plugins.videoplayer;

import java.io.InputStream;

import com.codename1.flutter.FlutterAssets;
import com.codename1.flutter.foundation.ValueNotifier;
import com.codename1.io.Log;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.util.UITimer;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import dart.async.Completer;
import dart.async.Future;
import dart.core.DartUri;
import dart.core.Duration;
import dart.core.UnsupportedError;

/**
 * video_player's VideoPlayerController over a Codename One {@link Media} from
 * {@link MediaManager}. While playing, the position is polled every 250 ms with a
 * UITimer on the current form, and each change notifies the listeners.
 *
 * Accepted and not applied: httpHeaders (MediaManager opens a URL without them),
 * formatHint, captions and setPlaybackSpeed, which is recorded in the value but
 * not applied -- Media has no rate control. Without a display initialize fails as
 * the plugin does with no platform player, and the value keeps its defaults.
 */
public class VideoPlayerController extends ValueNotifier<VideoPlayerValue> {
    private final String dataSource;
    private final boolean asset;
    private Media media;
    private UITimer poll;
    private boolean disposed;

    VideoPlayerController(String dataSource, boolean asset) {
        super(new VideoPlayerValue());
        this.dataSource = dataSource;
        this.asset = asset;
    }

    public static VideoPlayerController networkUrl(DartUri url, VideoPlayerOptions options, Object httpHeaders,
            Object formatHint, Object closedCaptionFile) {
        return new VideoPlayerController(String.valueOf(url), false);
    }

    public static VideoPlayerController network(String dataSource, VideoPlayerOptions options, Object httpHeaders,
            Object formatHint, Object closedCaptionFile) {
        return new VideoPlayerController(dataSource, false);
    }

    public static VideoPlayerController file(dart.io.File file, VideoPlayerOptions options, Object httpHeaders,
            Object closedCaptionFile) {
        return new VideoPlayerController(file == null ? "" : file.path(), false);
    }

    public static VideoPlayerController asset(String dataSource, String pkg, VideoPlayerOptions options,
            Object closedCaptionFile) {
        return new VideoPlayerController(dataSource, true);
    }

    public String dataSource() {
        return dataSource;
    }

    public Future<Duration> position() {
        return Future.value(value().position());
    }

    Media media() {
        return media;
    }

    public Future<Object> initialize() {
        if (!Display.isInitialized()) {
            VideoPlayerValue v = value().copy();
            v.errorDescription = "no video player without a display";
            value(v);
            return Future.error(new UnsupportedError(v.errorDescription));
        }
        final Completer<Object> done = new Completer<Object>();
        Runnable onCompletion = new Runnable() {
            @Override
            public void run() {
                completed();
            }
        };
        AsyncResource<Media> res;
        try {
            if (asset) {
                FlutterAssets.Resolved r = FlutterAssets.open(VideoPlayerController.class, dataSource);
                InputStream in = r == null ? null : r.stream();
                if (in == null) {
                    return fail(done, "asset not found: " + dataSource);
                }
                res = MediaManager.createMediaAsync(in, mimeOf(dataSource), onCompletion);
            } else {
                res = MediaManager.createMediaAsync(dataSource, true, onCompletion);
            }
        } catch (Exception e) {
            return fail(done, String.valueOf(e.getMessage()));
        }
        res.ready(new SuccessCallback<Media>() {
            @Override
            public void onSucess(Media m) {
                if (disposed) {
                    m.cleanup();
                    return;
                }
                media = m;
                VideoPlayerValue v = value().copy();
                v.initialized = true;
                v.durationMs = Math.max(0, m.getDuration());
                Component c = m.getVideoComponent();
                if (c != null) {
                    Dimension d = c.getPreferredSize();
                    v.width = d.getWidth();
                    v.height = d.getHeight();
                }
                v.volume = m.getVolume() / 100.0;
                value(v);
                done.complete(null);
            }
        });
        res.except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable t) {
                fail(done, String.valueOf(t.getMessage()));
            }
        });
        return done.future();
    }

    private Future<Object> fail(Completer<Object> done, String why) {
        VideoPlayerValue v = value().copy();
        v.errorDescription = why;
        value(v);
        done.completeError(new UnsupportedError(why));
        return done.future();
    }

    private static String mimeOf(String path) {
        String p = path == null ? "" : path;
        if (p.regionMatches(true, Math.max(0, p.length() - 5), ".webm", 0, 5)) {
            return "video/webm";
        }
        if (p.regionMatches(true, Math.max(0, p.length() - 4), ".mov", 0, 4)) {
            return "video/quicktime";
        }
        return "video/mp4";
    }

    private void completed() {
        if (media == null) {
            return;
        }
        if (value().looping) {
            media.setTime(0);
            media.play();
            return;
        }
        stopPolling();
        VideoPlayerValue v = value().copy();
        v.playing = false;
        v.completed = true;
        v.positionMs = v.durationMs;
        value(v);
    }

    public Future<Object> play() {
        if (media != null) {
            media.play();
            VideoPlayerValue v = value().copy();
            v.playing = true;
            v.completed = false;
            value(v);
            startPolling();
        }
        return Future.value(null);
    }

    public Future<Object> pause() {
        if (media != null) {
            media.pause();
            stopPolling();
            VideoPlayerValue v = value().copy();
            v.playing = false;
            v.positionMs = media.getTime();
            value(v);
        }
        return Future.value(null);
    }

    public Future<Object> seekTo(Duration position) {
        long ms = position == null ? 0 : position.inMilliseconds();
        VideoPlayerValue v = value().copy();
        v.positionMs = Math.max(0, v.durationMs > 0 ? Math.min(ms, v.durationMs) : ms);
        if (media != null) {
            media.setTime((int) v.positionMs);
        }
        value(v);
        return Future.value(null);
    }

    public Future<Object> setLooping(boolean looping) {
        VideoPlayerValue v = value().copy();
        v.looping = looping;
        value(v);
        return Future.value(null);
    }

    public Future<Object> setVolume(double volume) {
        VideoPlayerValue v = value().copy();
        v.volume = Math.max(0, Math.min(1, volume));
        if (media != null) {
            media.setVolume((int) Math.round(v.volume * 100));
        }
        value(v);
        return Future.value(null);
    }

    public Future<Object> setPlaybackSpeed(double speed) {
        VideoPlayerValue v = value().copy();
        v.playbackSpeed = speed;
        value(v);
        return Future.value(null);
    }

    private void startPolling() {
        if (poll != null || !Display.isInitialized()) {
            return;
        }
        Form f = Display.getInstance().getCurrent();
        if (f == null) {
            return;
        }
        poll = new UITimer(new Runnable() {
            @Override
            public void run() {
                if (media == null) {
                    stopPolling();
                    return;
                }
                VideoPlayerValue v = value().copy();
                v.positionMs = media.getTime();
                v.playing = media.isPlaying();
                if (v.durationMs <= 0) {
                    v.durationMs = Math.max(0, media.getDuration());
                }
                value(v);
            }
        });
        poll.schedule(250, true, f);
    }

    private void stopPolling() {
        if (poll != null) {
            poll.cancel();
            poll = null;
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        stopPolling();
        if (media != null) {
            try {
                media.cleanup();
            } catch (RuntimeException e) {
                Log.e(e);
            }
            media = null;
        }
        super.dispose();
    }
}
