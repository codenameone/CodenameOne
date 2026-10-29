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
package com.codename1.flutter.plugins.cachednetworkimage;

import java.util.HashMap;
import java.util.Map;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.MemoryImage;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Image;
import com.codename1.flutter.widgets.SizedBox;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;

import dart.io.SocketException;
import dart.runtime.Funcs;
import dart.typed_data.Uint8List;

/**
 * package:cached_network_image's {@code CachedNetworkImage}. The widget fetches the
 * bytes itself (a {@code ConnectionRequest} on the network manager, with
 * {@code httpHeaders}) so it can build what the plugin builds: while loading, the
 * {@code progressIndicatorBuilder} (with a {@link DownloadProgress}) or else the
 * {@code placeholder}; on a failed connection, an HTTP error status, an empty body or
 * no display to load on, the {@code errorWidget} (with the error); once the bytes are
 * here, the picture, decoded as {@code Image.memory} with width, height, fit and the
 * {@code memCacheWidth}/{@code memCacheHeight} decode-size hints. Downloaded bytes are
 * kept in an in-memory map keyed by {@code cacheKey} (else the URL), so a repeat is
 * built straight away; nothing is written to disk. {@code fadeInDuration} is accepted
 * and not applied. With no builder given, the loading and error states are empty boxes.
 */
public class CachedNetworkImage extends StatefulWidget {
    /** Bytes of finished downloads, by cache key. */
    private static final Map<String, byte[]> CACHE = new HashMap<String, byte[]>();

    String imageUrl;
    Double width;
    Double height;
    BoxFit fit;
    Map<?, ?> httpHeaders;
    Long memCacheWidth;
    Long memCacheHeight;
    String cacheKey;
    Funcs.Func2<BuildContext, String, Widget> placeholder;
    Funcs.Func3<BuildContext, String, Object, Widget> errorWidget;
    Funcs.Func3<BuildContext, String, DownloadProgress, Widget> progressIndicatorBuilder;

    public CachedNetworkImage() {
    }

    public void imageUrl(String v) {
        this.imageUrl = v;
    }

    public void width(Double v) {
        this.width = v;
    }

    public void height(Double v) {
        this.height = v;
    }

    public void fit(BoxFit v) {
        this.fit = v;
    }

    /// Built while the image loads, when there is no progressIndicatorBuilder.
    public void placeholder(Funcs.Func2<BuildContext, String, Widget> v) {
        this.placeholder = v;
    }

    /// Built when the load fails, with the error.
    public void errorWidget(Funcs.Func3<BuildContext, String, Object, Widget> v) {
        this.errorWidget = v;
    }

    /// Sent with the image request, so an endpoint behind Authorization or a cookie
    /// serves the image instead of refusing a bare request.
    public void httpHeaders(Object v) {
        this.httpHeaders = v instanceof Map ? (Map<?, ?>) v : null;
    }

    public void memCacheWidth(Object v) {
        memCacheWidth = v instanceof Number ? Long.valueOf(((Number) v).longValue()) : null;
    }

    public void memCacheHeight(Object v) {
        memCacheHeight = v instanceof Number ? Long.valueOf(((Number) v).longValue()) : null;
    }

    /// Built while the image loads, in preference to the placeholder.
    public void progressIndicatorBuilder(Funcs.Func3<BuildContext, String, DownloadProgress, Widget> v) {
        this.progressIndicatorBuilder = v;
    }

    /** Accepted for source compatibility; see the class comment. */
    public void fadeInDuration(Object v) {
    }

    /// Keys the in-memory bytes instead of the URL.
    public void cacheKey(String v) {
        this.cacheKey = v;
    }

    String key() {
        return cacheKey != null ? cacheKey : (imageUrl == null ? "" : imageUrl);
    }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new Loader();
    }

    /// What can be answered without the network: the cached bytes (as byte[]), or the
    /// error a load that cannot start fails with; null when a download is needed.
    private Object immediate() {
        byte[] cached = CACHE.get(key());
        if (cached != null) {
            return cached;
        }
        if (imageUrl == null || imageUrl.length() == 0) {
            return new IllegalArgumentException("CachedNetworkImage has no imageUrl");
        }
        if (!Display.isInitialized()) {
            // nothing to download with; the error path is the answer a caller gets
            return new SocketException("No network: Codename One is not initialized");
        }
        return null;
    }

    /// The widget for one state: bytes, else error, else the loading builders.
    private Widget content(BuildContext context, byte[] bytes, Object error, long received, Long total) {
        String url = imageUrl == null ? "" : imageUrl;
        if (bytes != null) {
            Image i = new Image();
            i.image(new MemoryImage(Uint8List.fromBytes(bytes)));
            i.width(width);
            i.height(height);
            i.fit(fit);
            i.cacheWidth(memCacheWidth);
            i.cacheHeight(memCacheHeight);
            return i;
        }
        Widget built = null;
        if (error != null) {
            if (errorWidget != null) {
                built = errorWidget.call(context, url, error);
            }
        } else if (progressIndicatorBuilder != null) {
            built = progressIndicatorBuilder.call(context, url, new DownloadProgress(url, total, received));
        } else if (placeholder != null) {
            built = placeholder.call(context, url);
        }
        return built != null ? built : SizedBox.shrink(null, null);
    }

    /// What the widget builds the moment it is mounted, without mounting it: the
    /// cached picture, the error widget when the load cannot start (no display, no
    /// URL), else the loading widget.
    public Widget buildOnce(BuildContext context) {
        Object now = immediate();
        if (now instanceof byte[]) {
            return content(context, (byte[]) now, null, 0, null);
        }
        return content(context, null, now, 0, null);
    }

    /// The widget built while a download is {@code downloaded} bytes into {@code total}.
    public Widget buildLoading(BuildContext context, long downloaded, Long total) {
        return content(context, null, null, downloaded, total);
    }

    /// Stores bytes as a finished download of this widget's key.
    public void seedCache(dart.typed_data.Uint8List data) {
        CACHE.put(key(), data.toBytes());
    }

    private static final class Loader extends State<CachedNetworkImage> {
        private String loadedKey;
        private byte[] bytes;
        private Object error;
        private long received;
        private Long total;
        private ConnectionRequest request;
        private ActionListener<NetworkEvent> progressListener;

        @Override
        public void initState() {
            start();
        }

        @Override
        public void didUpdateWidget(CachedNetworkImage old) {
            if (!widget().key().equals(loadedKey)) {
                stop();
                start();
            }
        }

        @Override
        public void dispose() {
            stop();
        }

        private void stop() {
            if (progressListener != null) {
                NetworkManager.getInstance().removeProgressListener(progressListener);
                progressListener = null;
            }
            if (request != null) {
                request.kill();
                request = null;
            }
        }

        private void start() {
            final CachedNetworkImage w = widget();
            final String key = w.key();
            loadedKey = key;
            bytes = null;
            error = null;
            received = 0;
            total = null;
            final String url = w.imageUrl;
            Object now = w.immediate();
            if (now instanceof byte[]) {
                bytes = (byte[]) now;
                return;
            }
            if (now != null) {
                error = now;
                return;
            }
            final ConnectionRequest req = new ConnectionRequest(url, false);
            req.setDuplicateSupported(true);
            req.setFailSilently(true);
            if (w.httpHeaders != null) {
                for (Map.Entry<?, ?> e : w.httpHeaders.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        req.addRequestHeader(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                    }
                }
            }
            request = req;
            progressListener = new ActionListener<NetworkEvent>() {
                @Override
                public void actionPerformed(NetworkEvent evt) {
                    if (evt.getConnectionRequest() != req
                            || evt.getProgressType() != NetworkEvent.PROGRESS_TYPE_INPUT) {
                        return;
                    }
                    final long got = evt.getSentReceived();
                    final long len = evt.getLength();
                    CN.callSerially(new Runnable() {
                        @Override
                        public void run() {
                            if (request != req || !mounted()) {
                                return;
                            }
                            setState(new Funcs.VoidFunc0() {
                                @Override
                                public void call() {
                                    received = got;
                                    total = len > 0 ? Long.valueOf(len) : null;
                                }
                            });
                        }
                    });
                }
            };
            NetworkManager.getInstance().addProgressListener(progressListener);
            req.addResponseListener(new ActionListener<NetworkEvent>() {
                @Override
                public void actionPerformed(NetworkEvent evt) {
                    byte[] data = req.getResponseData();
                    if (data == null || data.length == 0) {
                        finish(req, key, null, new SocketException("Empty response from " + url));
                    } else {
                        finish(req, key, data, null);
                    }
                }
            });
            ActionListener<NetworkEvent> failed = new ActionListener<NetworkEvent>() {
                @Override
                public void actionPerformed(NetworkEvent evt) {
                    Object cause = evt.getError();
                    String msg = evt.getResponseCode() > 0
                            ? "HTTP request failed, statusCode: " + evt.getResponseCode() + ", " + url
                            : "Connection failed: " + url;
                    finish(req, key, null, cause != null ? cause : new SocketException(msg));
                }
            };
            req.addExceptionListener(failed);
            req.addResponseCodeListener(failed);
            NetworkManager.getInstance().addToQueue(req);
        }

        private void finish(final ConnectionRequest req, final String key, final byte[] data, final Object err) {
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    if (request != req || !mounted()) {
                        return;
                    }
                    if (progressListener != null) {
                        NetworkManager.getInstance().removeProgressListener(progressListener);
                        progressListener = null;
                    }
                    request = null;
                    if (data != null) {
                        CACHE.put(key, data);
                    }
                    setState(new Funcs.VoidFunc0() {
                        @Override
                        public void call() {
                            bytes = data;
                            error = err;
                        }
                    });
                }
            });
        }

        @Override
        public Widget build(BuildContext context) {
            return widget().content(context, bytes, error, received, total);
        }
    }
}
