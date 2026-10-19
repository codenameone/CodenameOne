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
package dart.io;

import com.codename1.ui.Display;
import dart.async.Completer;
import dart.async.Future;
import dart.async.Stream;
import dart.convert.ByteCodec;
import dart.typed_data.Uint8List;

/**
 * Dart's {@code WebSocket} over {@link com.codename1.io.WebSocket}: a stream of the
 * messages received (a String per text frame, a Uint8List per binary frame) that the
 * app writes to with {@link #add}. Events reach listeners on the EDT.
 */
public class WebSocket extends Stream<Object> {
    public static final long connecting = 0;
    public static final long open = 1;
    public static final long closing = 2;
    public static final long closed = 3;

    private com.codename1.io.WebSocket socket;
    private volatile long readyState = connecting;
    private Long closeCode;
    private String closeReason;
    private final Completer<Object> done = new Completer<Object>();
    private WebSocket() {
    }

    public static Future<WebSocket> connect(String url) {
        return connect(url, null, null, null);
    }

    /** {@code WebSocket.connect(url, protocols:, headers:, compression:)}. */
    public static Future<WebSocket> connect(String url, Object protocols, Object headers, Object compression) {
        final Completer<WebSocket> result = new Completer<WebSocket>();
        if (!Display.isInitialized() || !com.codename1.io.WebSocket.isSupported()) {
            result.completeError(new SocketException("WebSockets are not supported here"));
            return result.future();
        }
        final WebSocket ws = new WebSocket();
        com.codename1.io.WebSocket s = com.codename1.io.WebSocket.build(url);
        if (protocols instanceof Iterable) {
            java.util.List<String> ps = new java.util.ArrayList<String>();
            for (Object p : (Iterable<?>) protocols) {
                ps.add(String.valueOf(p));
            }
            s.subprotocols(ps.toArray(new String[ps.size()]));
        }
        if (headers instanceof java.util.Map) {
            for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) headers).entrySet()) {
                s.header(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        s.onConnect(new com.codename1.io.WebSocket.ConnectHandler() {
            @Override
            public void onConnect(com.codename1.io.WebSocket w) {
                ws.readyState = open;
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        result.complete(ws);
                    }
                });
            }
        });
        s.onTextMessage(new com.codename1.io.WebSocket.TextHandler() {
            @Override
            public void onText(com.codename1.io.WebSocket w, final String message) {
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        ws.emit(message);
                    }
                });
            }
        });
        s.onBinaryMessage(new com.codename1.io.WebSocket.BinaryHandler() {
            @Override
            public void onBinary(com.codename1.io.WebSocket w, final byte[] message) {
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        ws.emit(Uint8List.fromBytes(message));
                    }
                });
            }
        });
        s.onClose(new com.codename1.io.WebSocket.CloseHandler() {
            @Override
            public void onClose(com.codename1.io.WebSocket w, final int statusCode, final String reason) {
                ws.readyState = closed;
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        ws.closeCode = Long.valueOf(statusCode);
                        ws.closeReason = reason;
                        if (!result.isCompleted()) {
                            result.completeError(new SocketException("WebSocket closed during connect: " + reason));
                        }
                        ws.finish();
                    }
                });
            }
        });
        s.onError(new com.codename1.io.WebSocket.ErrorHandler() {
            @Override
            public void onError(com.codename1.io.WebSocket w, final Exception ex) {
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        SocketException err = new SocketException(String.valueOf(ex.getMessage()));
                        if (!result.isCompleted()) {
                            ws.readyState = closed;
                            result.completeError(err);
                        } else {
                            ws.emitError(err);
                        }
                    }
                });
            }
        });
        ws.socket = s;
        s.connect();
        return result.future();
    }

    private static void onEdt(Runnable r) {
        if (Display.getInstance().isEdt()) {
            r.run();
        } else {
            Display.getInstance().callSerially(r);
        }
    }

    private void finish() {
        if (!done.isCompleted()) {
            emitDone();
            done.complete(null);
        }
    }

    /** Sends a String as a text frame, anything else as a binary frame. */
    public void add(Object data) {
        if (readyState != open) {
            return;
        }
        if (data instanceof String) {
            socket.send((String) data);
        } else {
            socket.send(ByteCodec.bytesOf(data));
        }
    }

    public void addUtf8Text(Object bytes) {
        add(dart.convert.Convert.utf8.decode(bytes));
    }

    public Future<Object> addStream(Stream<Object> stream) {
        return stream.forEach(new dart.runtime.Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                add(v);
            }
        });
    }

    public Future<Object> close() {
        return close(null, null);
    }

    public Future<Object> close(Long code) {
        return close(code, null);
    }

    public Future<Object> close(Long code, String reason) {
        if (readyState == open || readyState == connecting) {
            readyState = closing;
            closeCode = code;
            closeReason = reason;
            socket.close();
        }
        return done.future();
    }

    public Future<Object> done() {
        return done.future();
    }

    public long readyState() {
        return readyState;
    }

    public Long closeCode() {
        return closeCode;
    }

    public String closeReason() {
        return closeReason;
    }

    public String protocol() {
        return socket == null ? null : socket.getSelectedSubprotocol();
    }

    public String extensions() {
        return "";
    }

    public void pingInterval(Object interval) {
    }
}
