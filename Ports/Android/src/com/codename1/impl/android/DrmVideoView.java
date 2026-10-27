/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android;

import android.content.Context;
import android.media.MediaDrm;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.codename1.media.DrmConfig;
import com.codename1.media.DrmScheme;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// A video view that plays a CENC-protected stream (Widevine or ClearKey) through
/// the framework `MediaPlayer` DRM API (API 26).
///
/// It extends `VideoView` only so `AndroidImplementation.Video` can host it
/// unchanged: every playback method `Video` calls is overridden here, and
/// `VideoView`'s own player is never opened (its URI stays null, so its surface
/// callback returns early). The view is the surface; the player is ours.
///
/// The player is created on a private looper thread, which is the thread
/// `MediaPlayer` delivers its events on. `onDrmInfo` must call `prepareDrm` and
/// provide the key response before it returns, and the license request is a
/// network round trip -- on the main thread that would throw
/// `NetworkOnMainThreadException`. Listener callbacks are reposted to the main
/// thread, where `VideoView` would have delivered them.
///
/// The framework player has no DASH extractor: it plays CENC progressive or
/// fragmented MP4 and HLS with fMP4 segments. `AndroidImplementation` refuses an
/// `.mpd` URL up front rather than failing in prepare; an application that needs
/// DASH with Widevine on Android uses a Media3 based player instead.
class DrmVideoView extends VideoView {
    static final UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
    static final UUID CLEARKEY_UUID = new UUID(0xE2719D58A985B3C9L, 0x781AB030AF78D30EL);

    private final Uri uri;
    private final Map<String, String> headers;
    private final DrmConfig drm;
    private final Handler main = new Handler(Looper.getMainLooper());
    // One looper thread for the life of the view; quit by stopPlayback.
    private HandlerThread playerThread;
    private Handler playerHandler;
    // Written on the player thread and read on the main thread (and the reverse
    // for the flags), so they are atomics rather than plain fields.
    private final AtomicReference<MediaPlayer> playerRef = new AtomicReference<MediaPlayer>();
    private final AtomicBoolean prepared = new AtomicBoolean();
    private final AtomicBoolean startWhenPrepared = new AtomicBoolean();
    private final AtomicBoolean released = new AtomicBoolean();
    // A seek asked for before the player was prepared, or the position the
    // player had reached when its surface went away; -1 for none. VideoView
    // keeps the same thing in mSeekWhenPrepared.
    private final AtomicInteger pendingSeek = new AtomicInteger(-1);
    private final AtomicInteger videoWidth = new AtomicInteger();
    private final AtomicInteger videoHeight = new AtomicInteger();
    private MediaController controller;
    private MediaPlayer.OnCompletionListener completionListener;
    private MediaPlayer.OnErrorListener errorListener;

    DrmVideoView(Context ctx, Uri uri, Map<String, String> headers, DrmConfig drm, int startPosition) {
        super(ctx);
        this.uri = uri;
        this.headers = headers;
        this.drm = drm;
        if (startPosition > 0) {
            pendingSeek.set(startPosition);
        }
        // The player lives exactly as long as the surface, as VideoView's does:
        // a view that is removed without cleanup() still lets go of its decoder
        // and its DRM session (a scarce resource with hardware-backed Widevine)
        // when its surface is destroyed, and a reparented view reopens where it
        // was. Re-attaching means a new license request, which is cheap.
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(final SurfaceHolder holder) {
                open(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                closePlayer(true);
            }
        });
    }

    /// Maps a scheme to the UUID `MediaDrm` knows it by.
    static UUID uuidFor(DrmScheme scheme) {
        if (scheme == DrmScheme.WIDEVINE) {
            return WIDEVINE_UUID;
        }
        if (scheme == DrmScheme.CLEARKEY) {
            return CLEARKEY_UUID;
        }
        return null;
    }

    /// Answers whether this device's framework player can play the scheme.
    static boolean isSupported(DrmScheme scheme) {
        UUID id = uuidFor(scheme);
        if (id == null || Build.VERSION.SDK_INT < 26) {
            return false;
        }
        try {
            return MediaDrm.isCryptoSchemeSupported(id);
        } catch (Throwable t) {
            return false;
        }
    }

    private void open(final SurfaceHolder holder) {
        if (released.get()) {
            return;
        }
        if (playerThread == null) {
            playerThread = new HandlerThread("CN1DrmVideo");
            playerThread.start();
            playerHandler = new Handler(playerThread.getLooper());
        }
        playerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!released.get() && playerRef.get() == null) {
                    createPlayer(holder);
                }
            }
        });
    }

    /// Releases the player on its own thread. From surfaceDestroyed this waits
    /// (briefly) for that to finish, because the surface must not be used once
    /// the callback returns.
    private void closePlayer(boolean wait) {
        if (playerHandler == null) {
            return;
        }
        final CountDownLatch done = new CountDownLatch(1);
        playerHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    MediaPlayer p = playerRef.getAndSet(null);
                    if (p != null) {
                        if (prepared.get() && pendingSeek.get() < 0) {
                            try {
                                pendingSeek.set(p.getCurrentPosition());
                            } catch (IllegalStateException ignore) {
                                // the position is lost with the player
                            }
                        }
                        try {
                            p.setDisplay(null);
                            if (Build.VERSION.SDK_INT >= 26) {
                                p.releaseDrm();
                            }
                        } catch (Throwable ignore) {
                            // no DRM session was opened
                        }
                        p.release();
                    }
                    prepared.set(false);
                } finally {
                    done.countDown();
                }
            }
        });
        if (wait) {
            try {
                if (!done.await(1, TimeUnit.SECONDS)) {
                    com.codename1.io.Log.p("DrmVideoView: the player did not release its surface within 1s");
                }
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void createPlayer(SurfaceHolder holder) {
        try {
            final MediaPlayer p = new MediaPlayer();
            playerRef.set(p);
            p.setDisplay(holder);
            p.setScreenOnWhilePlaying(true);
            p.setOnDrmInfoListener(new MediaPlayer.OnDrmInfoListener() {
                @Override
                public void onDrmInfo(MediaPlayer mp, MediaPlayer.DrmInfo info) {
                    try {
                        acquireLicense(mp, info);
                    } catch (Throwable t) {
                        com.codename1.io.Log.e(t);
                        postError(MediaPlayer.MEDIA_ERROR_UNKNOWN, MediaPlayer.MEDIA_ERROR_IO);
                    }
                }
            });
            p.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, final int width, final int height) {
                    videoWidth.set(width);
                    videoHeight.set(height);
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            if (width > 0 && height > 0) {
                                getHolder().setFixedSize(width, height);
                            }
                            requestLayout();
                        }
                    });
                }
            });
            p.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    prepared.set(true);
                    int seek = pendingSeek.getAndSet(-1);
                    if (seek > 0) {
                        mp.seekTo(seek);
                    }
                    if (startWhenPrepared.get()) {
                        mp.start();
                    }
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            if (controller != null) {
                                controller.setEnabled(true);
                            }
                        }
                    });
                }
            });
            p.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(final MediaPlayer mp) {
                    startWhenPrepared.set(false);
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            if (completionListener != null) {
                                completionListener.onCompletion(mp);
                            }
                        }
                    });
                }
            });
            p.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    postError(what, extra);
                    return true;
                }
            });
            p.setDataSource(getContext(), uri, headers);
            p.prepareAsync();
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
            postError(MediaPlayer.MEDIA_ERROR_UNKNOWN, MediaPlayer.MEDIA_ERROR_IO);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // VideoView sizes itself from its own player's video size, which is
        // never set here; keep the aspect ratio of ours instead.
        int vw = videoWidth.get();
        int vh = videoHeight.get();
        int width = getDefaultSize(vw, widthMeasureSpec);
        int height = getDefaultSize(vh, heightMeasureSpec);
        if (vw > 0 && vh > 0) {
            if (vw * height > width * vh) {
                height = width * vh / vw;
            } else if (vw * height < width * vh) {
                width = height * vw / vh;
            }
        }
        setMeasuredDimension(width, height);
    }

    private void acquireLicense(MediaPlayer mp, MediaPlayer.DrmInfo info) throws Exception {
        UUID scheme = uuidFor(drm.getScheme());
        boolean offered = false;
        for (UUID u : info.getSupportedSchemes()) {
            if (u.equals(scheme)) {
                offered = true;
                break;
            }
        }
        if (!offered) {
            throw new IllegalStateException("The stream is not packaged for " + drm.getScheme());
        }
        mp.prepareDrm(scheme);
        byte[] initData = info.getPssh().get(scheme);
        MediaDrm.KeyRequest req = mp.getKeyRequest(null, initData, "video/mp4",
                MediaDrm.KEY_TYPE_STREAMING, null);
        byte[] response = post(drm.getLicenseUrl(), req.getData(), drm.getLicenseHeaders());
        if (released.get() || playerRef.get() != mp) {
            // The view was closed while the license was in flight; this player is
            // being released and must not go on preparing against a dead surface.
            throw new IllegalStateException("The player was closed during license acquisition");
        }
        mp.provideKeyResponse(null, response);
    }

    private static byte[] post(String url, byte[] body, Map<String, String> headers) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setRequestProperty("Content-Type", "application/octet-stream");
            for (Map.Entry<String, String> h : headers.entrySet()) {
                c.setRequestProperty(h.getKey(), h.getValue());
            }
            OutputStream os = c.getOutputStream();
            try {
                os.write(body);
            } finally {
                os.close();
            }
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new java.io.IOException("License server answered " + code);
            }
            InputStream is = c.getInputStream();
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            } finally {
                is.close();
            }
        } finally {
            c.disconnect();
        }
    }

    private void postError(final int what, final int extra) {
        main.post(new Runnable() {
            @Override
            public void run() {
                if (errorListener != null) {
                    errorListener.onError(playerRef.get(), what, extra);
                }
            }
        });
    }

    private void onPlayer(final Runnable r) {
        if (playerHandler != null) {
            playerHandler.post(r);
        }
    }

    @Override
    public void setMediaController(MediaController mc) {
        if (controller != null) {
            controller.hide();
        }
        controller = mc;
        if (mc != null) {
            mc.setMediaPlayer(this);
            View anchor = getParent() instanceof View ? (View) getParent() : this;
            mc.setAnchorView(anchor);
            mc.setEnabled(prepared.get());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && prepared.get() && controller != null) {
            if (controller.isShowing()) {
                controller.hide();
            } else {
                controller.show();
            }
        }
        return false;
    }

    @Override
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener l) {
        completionListener = l;
    }

    @Override
    public void setOnErrorListener(MediaPlayer.OnErrorListener l) {
        errorListener = l;
    }

    @Override
    public void start() {
        startWhenPrepared.set(true);
        onPlayer(new Runnable() {
            @Override
            public void run() {
                MediaPlayer p = playerRef.get();
                if (p != null && prepared.get()) {
                    p.start();
                }
            }
        });
    }

    @Override
    public void pause() {
        startWhenPrepared.set(false);
        onPlayer(new Runnable() {
            @Override
            public void run() {
                MediaPlayer p = playerRef.get();
                if (p != null && prepared.get() && p.isPlaying()) {
                    p.pause();
                }
            }
        });
    }

    @Override
    public void stopPlayback() {
        released.set(true);
        closePlayer(false);
        if (playerHandler != null) {
            playerHandler.post(new Runnable() {
                @Override
                public void run() {
                    playerThread.quitSafely();
                }
            });
        }
    }

    @Override
    public boolean canPause() {
        return prepared.get();
    }

    @Override
    public boolean canSeekBackward() {
        return prepared.get();
    }

    @Override
    public boolean canSeekForward() {
        return prepared.get();
    }

    @Override
    public int getCurrentPosition() {
        MediaPlayer p = playerRef.get();
        if (p == null || !prepared.get()) {
            return 0;
        }
        try {
            return p.getCurrentPosition();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    @Override
    public int getDuration() {
        MediaPlayer p = playerRef.get();
        if (p == null || !prepared.get()) {
            return -1;
        }
        try {
            return p.getDuration();
        } catch (IllegalStateException e) {
            return -1;
        }
    }

    @Override
    public boolean isPlaying() {
        MediaPlayer p = playerRef.get();
        if (p == null || !prepared.get()) {
            return false;
        }
        try {
            return p.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public void seekTo(final int msec) {
        if (!prepared.get()) {
            pendingSeek.set(msec);
        }
        onPlayer(new Runnable() {
            @Override
            public void run() {
                MediaPlayer p = playerRef.get();
                if (p != null && prepared.get()) {
                    p.seekTo(msec);
                } else {
                    pendingSeek.set(msec);
                }
            }
        });
    }
}
