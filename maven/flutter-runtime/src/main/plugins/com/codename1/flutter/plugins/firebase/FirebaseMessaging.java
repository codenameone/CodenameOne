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
 *
 * <p>A payload that is a JSON object is a data message: its entries are the message's
 * {@code data} (a {@code notification} object, or {@code title}/{@code body} entries,
 * make its {@code notification}; without them it is data-only and {@code notification}
 * is null). Any other payload is the notification body, with the text under
 * {@code data['payload']}. The {@code onBackgroundMessage} handler is called for a push
 * that arrives while the app is minimized or not yet showing a form; there is no
 * background isolate, so it runs on the app's thread before the message is delivered.
 * {@code requestPermission} answers {@code provisional} when provisional was asked for
 * and {@code authorized} otherwise (Codename One reports no denial); the other
 * permission flags are accepted and not applied.
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
        // Cold-start pushes after the first (which getInitialMessage hands over) wait
        // for the app's first onMessageOpenedApp listener; sent at once, no Dart
        // listener existed yet and the broadcast stream dropped them.
        OPENED.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                new dart.async.Timer(dart.core.Duration.zero, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        String[] held;
                        synchronized (FirebaseMessaging.class) {
                            held = HELD.toArray(new String[HELD.size()]);
                            HELD.clear();
                        }
                        for (String p : held) {
                            deliver(p, true);
                        }
                    }
                });
            }
        });
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
                if (!OPENED.hasListener()) {
                    HELD.add(payload);
                    return;
                }
            }
        }
        boolean opened = cold || com.codename1.ui.Display.getInstance().isMinimized();
        if (opened) {
            background(payload);
        }
        deliver(payload, opened);
    }

    private static Funcs.Func1<RemoteMessage, ?> backgroundHandler;

    /// Runs the onBackgroundMessage handler, if one is set, for a push received while
    /// the app was in the background. Answers whether a handler ran.
    public static boolean background(String payload) {
        Funcs.Func1<RemoteMessage, ?> h = backgroundHandler;
        if (h == null) {
            return false;
        }
        h.call(message(payload));
        return true;
    }

    private static final java.util.List<String> HELD = new java.util.ArrayList<String>();
    private static RemoteMessage initialMessage;
    private static boolean initialTaken;

    private static RemoteMessage message(String payload) {
        received++;
        DartMap<String, Object> data = new DartMap<String, Object>();
        String trimmed = payload == null ? "" : payload.trim();
        if (trimmed.startsWith("{")) {
            java.util.Map<String, Object> json = null;
            try {
                json = new com.codename1.io.JSONParser().parseJSON(new java.io.StringReader(trimmed));
            } catch (java.io.IOException e) {
                json = null;
            }
            if (json != null && !json.isEmpty()) {
                String title = null;
                String body = null;
                Object n = json.get("notification");
                if (n instanceof java.util.Map) {
                    Object t = ((java.util.Map<?, ?>) n).get("title");
                    Object b = ((java.util.Map<?, ?>) n).get("body");
                    title = t == null ? null : t.toString();
                    body = b == null ? null : b.toString();
                } else {
                    Object t = json.get("title");
                    Object b = json.get("body");
                    title = t == null ? null : t.toString();
                    body = b == null ? null : b.toString();
                }
                Object id = json.get("messageId");
                for (java.util.Map.Entry<String, Object> e : json.entrySet()) {
                    if (!"notification".equals(e.getKey())) {
                        data.put(e.getKey(), e.getValue());
                    }
                }
                RemoteNotification rn = title == null && body == null ? null : new RemoteNotification(title, body);
                return new RemoteMessage(id instanceof String ? (String) id : "cn1-" + received, data, rn);
            }
        }
        data.put("payload", payload);
        return new RemoteMessage("cn1-" + received, data, new RemoteNotification(null, payload));
    }

    public static void deliver(String payload, boolean opened) {
        RemoteMessage m = message(payload);
        (opened ? OPENED : MESSAGES).add(m);
    }

    /** Called when the push key is registered. */
    public static void tokenRegistered(String token) {
        settlePending(token, null);
        TOKENS.add(token);
    }

    /** Called when push registration fails: a getToken waiting on it fails too. */
    public static void registrationFailed(String error) {
        settlePending(null, error);
    }

    /// getToken calls waiting for the registration they started.
    private static final java.util.List<dart.async.Completer<String>> PENDING =
            new java.util.ArrayList<dart.async.Completer<String>>();

    private static void settlePending(String token, String error) {
        dart.async.Completer<String>[] waiting;
        synchronized (PENDING) {
            @SuppressWarnings("unchecked")
            dart.async.Completer<String>[] w = PENDING.toArray(new dart.async.Completer[PENDING.size()]);
            waiting = w;
            PENDING.clear();
        }
        for (dart.async.Completer<String> c : waiting) {
            if (error != null) {
                c.completeError(new dart.core.StateError("Push registration failed: " + error));
            } else {
                c.complete(token);
            }
        }
    }

    public static void onBackgroundMessage(Funcs.Func1<RemoteMessage, ?> handler) {
        // No background isolate: the handler runs on the app's thread (see background).
        backgroundHandler = handler;
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
        if (!Display.isInitialized() || src == null) {
            return Future.value(null);
        }
        String known = src.call();
        if (known != null) {
            return Future.value(known);
        }
        // A fresh install has no key until registration runs, and on platforms with no
        // permission prompt nothing else starts it: getToken is what obtains the token
        // in Firebase, so it registers and completes when registeredForPush answers.
        dart.async.Completer<String> c = new dart.async.Completer<String>();
        synchronized (PENDING) {
            PENDING.add(c);
        }
        Display.getInstance().registerPush();
        return c.future();
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
        lastStatus = Boolean.TRUE.equals(provisional) ? AuthorizationStatus.provisional
                : AuthorizationStatus.authorized;
        return Future.value(new NotificationSettings(lastStatus));
    }

    private static AuthorizationStatus lastStatus;

    public Future<NotificationSettings> getNotificationSettings() {
        AuthorizationStatus st = lastStatus;
        return Future.value(new NotificationSettings(st == null ? AuthorizationStatus.notDetermined : st));
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

    /// Codename One's Push topic API (FCM topics on Android), reached through a hook the
    /// import-generated entry point installs -- like tokenSource, and for the same reason:
    /// naming com.codename1.push here put it in every app that kept this package (an
    /// auth-only Firebase app included), and the Android builder then turned on FCM and
    /// required google-services.json. Arguments: the topic, and true to subscribe. With
    /// no hook (no firebase_messaging dependency) or a port without topics it is ignored,
    /// as it is for a native Codename One app.
    private static Funcs.VoidFunc2<String, Boolean> topicSink;

    public static void topicSink(Funcs.VoidFunc2<String, Boolean> sink) {
        topicSink = sink;
    }

    public Future<Object> subscribeToTopic(String topic) {
        return topic(topic, true);
    }

    public Future<Object> unsubscribeFromTopic(String topic) {
        return topic(topic, false);
    }

    private static Future<Object> topic(String topic, boolean subscribe) {
        Funcs.VoidFunc2<String, Boolean> sink = topicSink;
        if (sink != null) {
            try {
                sink.call(topic, Boolean.valueOf(subscribe));
            } catch (RuntimeException e) {
                return Future.error(e);
            }
        }
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
