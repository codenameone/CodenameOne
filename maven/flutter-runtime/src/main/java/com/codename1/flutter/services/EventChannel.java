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
package com.codename1.flutter.services;

import com.codename1.flutter.FlutterChannelRegistry;
import com.codename1.flutter.foundation.FlutterError;
import com.codename1.flutter.platform.MissingPluginException;
import com.codename1.flutter.platform.PlatformException;
import dart.async.Stream;

/**
 * The transpilation target of {@code package:flutter/services.dart}'s
 * {@code EventChannel} -- the Dart side of a platform event channel, matched
 * to {@link com.codename1.flutter.platform.EventChannel} by {@link #name}
 * through {@link FlutterChannelRegistry}.
 *
 * <p>Each {@link #receiveBroadcastStream()} call returns a fresh
 * {@link dart.async.Stream}, exactly like the real API: calling it twice
 * gives two independent listen sessions, each running the platform
 * {@code StreamHandler.onListen}/{@code onCancel} pair on its own first
 * listener / last cancel, as described on {@link dart.async.Stream}'s class
 * comment.</p>
 */
public final class EventChannel {

    private final String name;

    public EventChannel(String name) {
        this.name = name;
    }

    /** {@code codec} is accepted for source compatibility and ignored, the same as {@link MethodChannel}'s. */
    public EventChannel(String name, Object codec) {
        this(name);
    }

    public Stream<Object> receiveBroadcastStream() {
        return receiveBroadcastStream(null);
    }

    public Stream<Object> receiveBroadcastStream(Object arguments) {
        return new PlatformEventStream(name, arguments);
    }

    /**
     * One listen session of a platform event channel: runs the registered
     * {@code StreamHandler.onListen} on its first listener and
     * {@code onCancel} on its last cancel, forwarding every
     * {@code EventSink} call to the current listener set in between.
     */
    private static final class PlatformEventStream extends Stream<Object> {
        private final String channel;
        private final Object arguments;
        private boolean listening;

        PlatformEventStream(String channel, Object arguments) {
            this.channel = channel;
            this.arguments = arguments;
        }

        @Override
        protected void onFirstListener() {
            com.codename1.flutter.platform.EventChannel.StreamHandler h =
                    FlutterChannelRegistry.streamHandler(channel);
            if (h == null) {
                // No platform handler registered: real Flutter's
                // receiveBroadcastStream awaits methodChannel.invokeMethod(
                // 'listen', arguments), which throws MissingPluginException
                // when nothing answers it, and reports that through
                // FlutterError.reportError -- never delivered to the stream
                // itself (services/platform_channel.dart's
                // receiveBroadcastStream, read against Flutter 3.35: the
                // catch around invokeMethod only calls
                // FlutterError.reportError, never controller.addError). The
                // Dart listener therefore stays live but silent, matching
                // that -- no error event here, ever.
                FlutterError.reportError(new MissingPluginException(
                        "No implementation found for method listen on channel " + channel));
                return;
            }
            listening = true;
            try {
                h.onListen(arguments, new SinkImpl());
            } catch (Throwable t) {
                // Same source, same catch: a platform handler that throws
                // during activation is reported exactly like a missing one,
                // not delivered to the stream.
                FlutterError.reportError(t);
            }
        }

        @Override
        protected void onNoListeners() {
            if (!listening) {
                return;
            }
            listening = false;
            com.codename1.flutter.platform.EventChannel.StreamHandler h =
                    FlutterChannelRegistry.streamHandler(channel);
            if (h != null) {
                try {
                    h.onCancel(arguments);
                } catch (Throwable t) {
                    // receiveBroadcastStream's onCancel has the identical
                    // try/catch around invokeMethod('cancel', ...), reporting
                    // through FlutterError.reportError rather than swallowing
                    // it -- there is no longer a listener to deliver it to
                    // anyway.
                    FlutterError.reportError(t);
                }
            }
        }

        /** The {@code EventSink} handed to the platform handler's {@code onListen}. */
        private final class SinkImpl implements com.codename1.flutter.platform.EventChannel.EventSink {
            @Override
            public void success(Object event) {
                emit(FlutterChannelRegistry.toDart(event));
            }

            @Override
            public void error(String errorCode, String errorMessage, Object errorDetails) {
                PlatformException e = new PlatformException();
                e.code(errorCode);
                e.message(errorMessage);
                e.details(FlutterChannelRegistry.toDart(errorDetails));
                emitError(e);
            }

            @Override
            public void endOfStream() {
                emitDone();
            }
        }
    }
}
