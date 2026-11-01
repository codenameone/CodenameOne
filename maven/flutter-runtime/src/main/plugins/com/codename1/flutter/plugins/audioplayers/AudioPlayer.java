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

import com.codename1.media.Media;
import com.codename1.ui.Display;

import java.util.Timer;
import java.util.TimerTask;

import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;
import dart.core.Duration;
import dart.typed_data.Uint8List;

/**
 * audioplayers' {@code AudioPlayer}, over one Codename One Media per source
 * ({@code MediaManager.createMedia}). Position is polled every 200 ms while playing;
 * {@code ReleaseMode.loop} restarts the sound when it ends. Balance, playback rate, the
 * audio context and the player mode are accepted and not applied. With no display there
 * is no player: the state still follows the calls, so an app's UI logic runs, but nothing
 * sounds and no position is reported.
 */
public class AudioPlayer {
    private static int ids;
    private String playerId;
    private Media media;
    private Source source;
    private PlayerState state = PlayerState.stopped;
    private ReleaseMode releaseMode = ReleaseMode.release;
    private double volume = 1.0;
    private Timer ticker;
    private final StreamController<Object> complete = StreamController.broadcast();
    private final StreamController<Duration> positions = StreamController.broadcast();
    private final StreamController<Duration> durations = StreamController.broadcast();
    private final StreamController<PlayerState> states = StreamController.broadcast();

    public AudioPlayer() {
        // Players are made on the EDT, so the counter needs no lock.
        ids++;
        this.playerId = "cn1-player-" + ids;
    }

    /** The {@code playerId} constructor argument. */
    public void playerId(String id) {
        if (id != null) {
            playerId = id;
        }
    }

    public String playerId() {
        return playerId;
    }

    public PlayerState state() {
        return state;
    }

    public Source source() {
        return source;
    }

    public double volume() {
        return volume;
    }

    public ReleaseMode releaseMode() {
        return releaseMode;
    }

    public Stream<Object> onPlayerComplete() {
        return complete.stream();
    }

    public Stream<Duration> onPositionChanged() {
        return positions.stream();
    }

    public Stream<Duration> onDurationChanged() {
        return durations.stream();
    }

    public Stream<PlayerState> onPlayerStateChanged() {
        return states.stream();
    }

    private void state(PlayerState s) {
        if (state != s) {
            state = s;
            states.add(s);
        }
    }

    private static Duration ms(long millis) {
        return Duration.ofMicroseconds(millis * 1000L);
    }

    public Future<Object> setSource(Source s) {
        if (state == PlayerState.disposed) {
            return Future.error(new dart.core.StateError("AudioPlayer has been disposed"));
        }
        releaseMedia();
        source = s;
        if (s == null || !Display.isInitialized()) {
            return Future.value(null);
        }
        try {
            media = s.open(new Runnable() {
                @Override
                public void run() {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            ended();
                        }
                    });
                }
            });
            media.setVolume((int) Math.round(volume * 100));
            int d = media.getDuration();
            if (d > 0) {
                durations.add(ms(d));
            }
        } catch (java.io.IOException e) {
            media = null;
            return Future.error(e);
        }
        return Future.value(null);
    }

    public Future<Object> setSourceUrl(String url, String mimeType) {
        return setSource(new UrlSource(url, mimeType));
    }

    public Future<Object> setSourceDeviceFile(String path, String mimeType) {
        return setSource(new DeviceFileSource(path, mimeType));
    }

    public Future<Object> setSourceAsset(String path, String mimeType) {
        return setSource(new AssetSource(path, mimeType));
    }

    public Future<Object> setSourceBytes(Uint8List bytes, String mimeType) {
        return setSource(new BytesSource(bytes, mimeType));
    }

    public Future<Object> play(Source s, Double volume, Double balance, Object ctx, Duration position,
            Object mode) {
        if (volume != null) {
            setVolume(volume.doubleValue());
        }
        Future<Object> set = setSource(s);
        if (set.completedError() != null) {
            return set;
        }
        if (position != null) {
            seek(position);
        }
        return resume();
    }

    public Future<Object> resume() {
        if (media != null) {
            media.play();
            startTicker();
        }
        state(PlayerState.playing);
        return Future.value(null);
    }

    public Future<Object> pause() {
        if (media != null) {
            media.pause();
        }
        stopTicker();
        state(PlayerState.paused);
        return Future.value(null);
    }

    public Future<Object> stop() {
        if (media != null) {
            media.pause();
            media.setTime(0);
        }
        stopTicker();
        state(PlayerState.stopped);
        return Future.value(null);
    }

    public Future<Object> release() {
        stopTicker();
        releaseMedia();
        state(PlayerState.stopped);
        return Future.value(null);
    }

    public Future<Object> seek(Duration position) {
        if (media != null && position != null) {
            media.setTime((int) position.inMilliseconds());
            positions.add(position);
        }
        return Future.value(null);
    }

    public Future<Object> setVolume(double v) {
        volume = Math.max(0, Math.min(1, v));
        if (media != null) {
            media.setVolume((int) Math.round(volume * 100));
        }
        return Future.value(null);
    }

    public Future<Object> setBalance(double balance) {
        return Future.value(null);
    }

    public Future<Object> setPlaybackRate(double rate) {
        return Future.value(null);
    }

    public Future<Object> setReleaseMode(ReleaseMode mode) {
        releaseMode = mode == null ? ReleaseMode.release : mode;
        return Future.value(null);
    }

    public Future<Object> setPlayerMode(PlayerMode mode) {
        return Future.value(null);
    }

    public Future<Duration> getDuration() {
        if (media == null) {
            return Future.value(null);
        }
        int d = media.getDuration();
        return Future.value(d < 0 ? null : ms(d));
    }

    public Future<Duration> getCurrentPosition() {
        return Future.value(media == null ? null : ms(Math.max(0, media.getTime())));
    }

    public Future<Object> dispose() {
        release();
        state(PlayerState.disposed);
        complete.close();
        positions.close();
        durations.close();
        states.close();
        return Future.value(null);
    }

    private void ended() {
        if (state == PlayerState.disposed) {
            return;
        }
        if (releaseMode == ReleaseMode.loop && media != null) {
            media.setTime(0);
            media.play();
            return;
        }
        stopTicker();
        if (releaseMode == ReleaseMode.release) {
            releaseMedia();
        }
        state(PlayerState.completed);
        complete.add(null);
    }

    private void releaseMedia() {
        if (media != null) {
            media.cleanup();
            media = null;
        }
    }

    private void startTicker() {
        stopTicker();
        ticker = new Timer();
        ticker.schedule(new TimerTask() {
            @Override
            public void run() {
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (media != null && state == PlayerState.playing) {
                            positions.add(ms(Math.max(0, media.getTime())));
                        }
                    }
                });
            }
        }, 200, 200);
    }

    private void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }
}
