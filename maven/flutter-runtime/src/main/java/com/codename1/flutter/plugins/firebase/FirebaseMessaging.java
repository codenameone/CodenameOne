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
package com.codename1.flutter.plugins.firebase;

import com.codename1.ui.Display;

import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;
import dart.core.DartMap;
import dart.runtime.Funcs;

/**
 * firebase_messaging over Codename One push. {@link #getToken} answers the device's
 * push key once {@code Display.registerPush} has run. Messages reach the Dart streams
 * through {@link #deliver}, which the app's {@code PushCallback.push} forwards to;
 * a message that arrives while the app is not running is not replayed.
 */
public class FirebaseMessaging {
    /** {@code FirebaseMessaging.instance}. */
    public static final FirebaseMessaging instance = new FirebaseMessaging();

    /** {@code FirebaseMessaging.onMessage}. */
    public static final Stream<RemoteMessage> onMessage;
    /** {@code FirebaseMessaging.onMessageOpenedApp}. */
    public static final Stream<RemoteMessage> onMessageOpenedApp;

    private static final StreamController<RemoteMessage> MESSAGES = StreamController.broadcast();
    private static final StreamController<RemoteMessage> OPENED = StreamController.broadcast();
    private static final StreamController<String> TOKENS = StreamController.broadcast();
    private static int received;

    static {
        onMessage = MESSAGES.stream();
        onMessageOpenedApp = OPENED.stream();
    }

    FirebaseMessaging() {
    }

    /** Called with a push payload; plain text becomes the notification body. */
    /// Codename One's push callback, as the imported entry point forwards it. The
    /// callback does not say whether a tap opened the app; a push arriving while the
    /// app is minimized (the tap resumes it) or before it has started (the tap
    /// launched it) is one the user opened, so it goes to onMessageOpenedApp.
    ///
    /// A push that arrives before the first form is shown launched the app. No Dart
    /// listener can be on the broadcast stream yet, so it is held for
    /// getInitialMessage, which is how firebase_messaging hands over a cold-start tap
    /// (and it is not also sent to onMessageOpenedApp, as on Flutter).
    public static void pushReceived(String payload) {
        boolean cold = !com.codename1.ui.Display.isInitialized()
                || com.codename1.ui.Display.getInstance().getCurrent() == null;
        if (cold) {
            synchronized (FirebaseMessaging.class) {
                if (!initialTaken && initialMessage == null) {
                    initialMessage = message(payload);
                    return;
                }
            }
        }
        deliver(payload, cold || com.codename1.ui.Display.getInstance().isMinimized());
    }

    private static RemoteMessage initialMessage;
    private static boolean initialTaken;

    private static RemoteMessage message(String payload) {
        received++;
        DartMap<String, Object> data = new DartMap<String, Object>();
        data.put("payload", payload);
        return new RemoteMessage("cn1-" + received, data, new RemoteNotification(null, payload));
    }

    public static void deliver(String payload, boolean opened) {
        RemoteMessage m = message(payload);
        (opened ? OPENED : MESSAGES).add(m);
    }

    /** Called when the push key is registered. */
    public static void tokenRegistered(String token) {
        TOKENS.add(token);
    }

    public static void onBackgroundMessage(Funcs.Func1<RemoteMessage, ?> handler) {
        // background isolates do not exist here; foreground delivery uses onMessage
    }

    /// Where getToken reads the push key from, installed by the import-generated entry
    /// point. This class must not name com.codename1.push itself: the Android builder
    /// turns push on (and then demands google-services.json) for any app whose classes
    /// reference that package, and this runtime ships in every imported app -- the
    /// Flutter gallery, with no Firebase at all, stopped building. The entry point
    /// references Push only when the app depends on firebase_messaging.
    private static Funcs.Func0<String> tokenSource;

    public static void tokenSource(Funcs.Func0<String> source) {
        tokenSource = source;
    }

    public Future<String> getToken(String vapidKey) {
        Funcs.Func0<String> src = tokenSource;
        return Future.value(Display.isInitialized() && src != null ? src.call() : null);
    }

    public Future<String> getAPNSToken() {
        return getToken(null);
    }

    public Stream<String> onTokenRefresh() {
        return TOKENS.stream();
    }

    public Future<Object> deleteToken() {
        return Future.value(null);
    }

    public Future<NotificationSettings> requestPermission(Boolean alert, Boolean announcement, Boolean badge,
            Boolean carPlay, Boolean criticalAlert, Boolean provisional, Boolean sound) {
        if (Display.isInitialized()) {
            Display.getInstance().registerPush();
        }
        return Future.value(new NotificationSettings(AuthorizationStatus.authorized));
    }

    public Future<NotificationSettings> getNotificationSettings() {
        return Future.value(new NotificationSettings(AuthorizationStatus.authorized));
    }

    /** The message whose tap launched the app, once; null afterwards, as on Flutter. */
    public Future<RemoteMessage> getInitialMessage() {
        RemoteMessage m;
        synchronized (FirebaseMessaging.class) {
            m = initialMessage;
            initialMessage = null;
            initialTaken = true;
        }
        return Future.value(m);
    }

    public Future<Object> subscribeToTopic(String topic) {
        return Future.value(null);
    }

    public Future<Object> unsubscribeFromTopic(String topic) {
        return Future.value(null);
    }

    public Future<Object> setForegroundNotificationPresentationOptions(Boolean alert, Boolean badge,
            Boolean sound) {
        return Future.value(null);
    }

    public Future<Object> setAutoInitEnabled(boolean enabled) {
        return Future.value(null);
    }
}
