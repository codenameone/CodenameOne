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
package com.codename1.flutter.platform;

/**
 * Mirrors Android's {@code io.flutter.plugin.common.EventChannel}: the
 * "platform side" of a Flutter event channel, matched to the Dart side's
 * {@code package:flutter/services.dart} {@code EventChannel} (transpiled to
 * {@code com.codename1.flutter.services.EventChannel}) by {@link #name}
 * through the shared {@link com.codename1.flutter.FlutterChannelRegistry}.
 *
 * <p>{@link StreamHandler#onListen} runs when the Dart side's stream gets
 * its first listener; {@link StreamHandler#onCancel} runs when its last
 * listener cancels. Both may be called repeatedly over the channel's
 * lifetime -- Dart may call {@code receiveBroadcastStream()} again, listen,
 * and let it be cancelled, any number of times.</p>
 */
public final class EventChannel {

    private final String name;

    public EventChannel(String name) {
        this.name = name;
    }

    /** Registers the handler that starts/stops event delivery for this channel. */
    public void setStreamHandler(StreamHandler handler) {
        com.codename1.flutter.FlutterChannelRegistry.setStreamHandler(name, handler);
    }

    /** Starts and stops delivering events to a Dart-side listener. */
    public interface StreamHandler {
        /** The Dart side's stream got its first listener. */
        void onListen(Object arguments, EventSink events);

        /** The Dart side's stream lost its last listener. */
        void onCancel(Object arguments);
    }

    /** Pushes events (or the end of the stream) to every current Dart-side listener. */
    public interface EventSink {
        void success(Object event);

        void error(String errorCode, String errorMessage, Object errorDetails);

        void endOfStream();
    }
}
