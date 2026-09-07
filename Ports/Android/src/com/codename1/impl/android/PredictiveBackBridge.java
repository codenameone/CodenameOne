/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import android.app.Activity;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/// Registers the ahead-of-time back callback that Android 13 (API 33) introduced
/// and Android 16 made mandatory, so the system back action keeps reaching
/// Codename One.
///
/// An app that targets API 36 and runs on Android 16 or newer gets predictive
/// back turned on by default. For such an app the platform stops calling
/// `Activity.onBackPressed()`: back is delivered to an `OnBackInvokedCallback`
/// registered on the activity's `OnBackInvokedDispatcher`. With no callback
/// registered the system takes the action itself and pops the activity off the
/// task, which leaves the app instead of running the current form's back
/// command. Measured on an Android 16 emulator, both halves: with no callback
/// the launcher came forward and `onBackPressed()` was never called; with one,
/// the callback ran and the app stayed.
///
/// The registration has to go through reflection. The Android port compiles
/// against the android.jar in cn1-binaries, which predates API 33 and has
/// neither `android.window.OnBackInvokedDispatcher` nor
/// `android.window.OnBackInvokedCallback` in it; the same sources are compiled
/// again -- against the app's real compile SDK -- inside every generated Gradle
/// project, so the code has to satisfy both. The callback interface is
/// implemented with a `java.lang.reflect.Proxy` for the same reason. Everything
/// touched here is public SDK API, so no non-SDK-interface restriction applies.
///
/// Registering is safe on every device: below API 33 nothing is registered at
/// all, and on API 33 through 35 the manifest flag
/// `android:enableOnBackInvokedCallback` still defaults to false, which makes
/// the platform ignore the registered callback and keep calling
/// `onBackPressed()` -- measured on an Android 14 emulator with an app
/// targeting 36, where the callback never fired. So the two ACTIVITY-level
/// entry points never both fire for one press. The key-event path is a
/// different matter; see [#keyEventBackStarted()].
///
/// An app that has to fall back to the legacy behaviour on Android 16 can say
/// so with the build hint
/// `android.xapplication_attr=android:enableOnBackInvokedCallback="false"`,
/// which is a temporary escape hatch rather than a supported end state.
///
/// The IME keeps its own back handling and is unaffected: when the soft
/// keyboard is up the back key still arrives as `View.onKeyPreIme()`, which is
/// what [InPlaceEditView] uses to close an in-place editor, and the callback
/// registered here is not invoked for that press.
final class PredictiveBackBridge {
    private static final String TAG = "CodenameOne";

    /// `android.window.OnBackInvokedDispatcher`, absent from the compile SDK.
    private static final String DISPATCHER_CLASS = "android.window.OnBackInvokedDispatcher";

    /// `android.window.OnBackInvokedCallback`, absent from the compile SDK.
    private static final String CALLBACK_CLASS = "android.window.OnBackInvokedCallback";

    /// `OnBackInvokedDispatcher.PRIORITY_DEFAULT`. The default priority is what
    /// an app's own navigation registers with; higher priorities are reserved
    /// for overlays that must win against it.
    private static final int PRIORITY_DEFAULT = 0;

    /// API level that introduced the dispatcher.
    private static final int FIRST_SUPPORTED_SDK = 33;

    /// True while the key-event path is in the middle of a back gesture it is
    /// already feeding into Codename One. Written and read from the UI thread
    /// only, but kept behind synchronized accessors so a stale read on a
    /// device that dispatches from elsewhere cannot turn into a double pop.
    private static boolean keyEventBackInFlight;

    private PredictiveBackBridge() {
    }

    /// Records that the key-event path has taken the DOWN half of a system
    /// back gesture.
    ///
    /// Android 16 does not simply replace the key event with the callback. When
    /// a view in the window holds an input-method connection the platform
    /// delivers BOTH: the key event still reaches the view hierarchy, and
    /// `onBackInvoked` fires between its DOWN and UP, milliseconds apart. The
    /// Codename One canvas is such a view -- [AndroidAsyncView] answers
    /// `onCreateInputConnection` -- so this is the ordinary case, not a corner
    /// of it. Feeding Codename One from both paths pops two forms for one
    /// press, which is how a back on a form with a back command was seen to run
    /// the command AND then minimize the app from the form underneath.
    ///
    /// So the key path stays authoritative whenever it is running, exactly as
    /// it always was, and the callback stands down for that gesture.
    static synchronized void keyEventBackStarted() {
        keyEventBackInFlight = true;
    }

    /// Records the UP half, ending the gesture the key path owns.
    static synchronized void keyEventBackFinished() {
        keyEventBackInFlight = false;
    }

    /// Whether the callback (or the legacy `onBackPressed()`) should feed this
    /// back into Codename One.
    ///
    /// #### Returns
    ///
    /// false when the key-event path has this gesture, in which case the flag
    /// is also cleared: a DOWN whose UP never arrives would otherwise silence
    /// every later callback rather than just this one.
    static synchronized boolean claimBackForCallback() {
        if (keyEventBackInFlight) {
            keyEventBackInFlight = false;
            return false;
        }
        return true;
    }

    /// Registers `onBack` as the activity's back callback.
    ///
    /// #### Parameters
    ///
    /// - `activity`: the activity whose dispatcher receives the callback
    /// - `onBack`: run on the UI thread for every system back action the
    ///   activity receives
    ///
    /// #### Returns
    ///
    /// the registered callback, to be handed back to
    /// [#unregister(Activity, Object)], or null when nothing was registered
    /// because the platform is older than API 33 or the registration failed
    static Object register(Activity activity, final Runnable onBack) {
        if (android.os.Build.VERSION.SDK_INT < FIRST_SUPPORTED_SDK || activity == null || onBack == null) {
            return null;
        }
        try {
            Class callbackClass = Class.forName(CALLBACK_CLASS);
            Class dispatcherClass = Class.forName(DISPATCHER_CLASS);
            Object dispatcher = Activity.class.getMethod("getOnBackInvokedDispatcher", new Class[0])
                    .invoke(activity, new Object[0]);
            if (dispatcher == null) {
                return null;
            }
            Object callback = Proxy.newProxyInstance(
                    PredictiveBackBridge.class.getClassLoader(),
                    new Class[]{callbackClass},
                    new BackCallbackHandler(onBack));
            // The method is looked up on the public interface rather than on
            // the dispatcher's own class: the concrete implementation the
            // platform hands back is package private, and a Method taken from
            // it is not accessible to us.
            dispatcherClass.getMethod("registerOnBackInvokedCallback",
                            new Class[]{int.class, callbackClass})
                    .invoke(dispatcher, new Object[]{Integer.valueOf(PRIORITY_DEFAULT), callback});
            return callback;
        } catch (Throwable t) {
            // A failure here is not fatal: onBackPressed() still runs on every
            // platform that has not switched over, and losing back navigation
            // is not worth taking the process down for.
            Log.e(TAG, "Failed to register the predictive back callback", t);
            return null;
        }
    }

    /// Removes a callback previously returned by [#register(Activity, Runnable)].
    /// A null callback, or one belonging to a platform without the dispatcher,
    /// is ignored.
    ///
    /// #### Parameters
    ///
    /// - `activity`: the activity the callback was registered on
    /// - `callback`: the value [#register(Activity, Runnable)] returned
    static void unregister(Activity activity, Object callback) {
        if (callback == null || activity == null
                || android.os.Build.VERSION.SDK_INT < FIRST_SUPPORTED_SDK) {
            return;
        }
        try {
            Class callbackClass = Class.forName(CALLBACK_CLASS);
            Class dispatcherClass = Class.forName(DISPATCHER_CLASS);
            Object dispatcher = Activity.class.getMethod("getOnBackInvokedDispatcher", new Class[0])
                    .invoke(activity, new Object[0]);
            if (dispatcher == null) {
                return;
            }
            dispatcherClass.getMethod("unregisterOnBackInvokedCallback",
                            new Class[]{callbackClass})
                    .invoke(dispatcher, new Object[]{callback});
        } catch (Throwable t) {
            Log.e(TAG, "Failed to unregister the predictive back callback", t);
        }
    }

    /// Backs the `OnBackInvokedCallback` proxy. The interface declares a single
    /// no-argument `onBackInvoked()`; `equals`, `hashCode` and `toString` reach
    /// the handler as well and have to be answered here, because a proxy has no
    /// implementation of its own to fall back on and the dispatcher stores the
    /// callback in a collection.
    private static final class BackCallbackHandler implements InvocationHandler {
        private final Runnable onBack;

        BackCallbackHandler(Runnable onBack) {
            this.onBack = onBack;
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("onBackInvoked".equals(name)) {
                onBack.run();
                return null;
            }
            if ("equals".equals(name)) {
                return Boolean.valueOf(proxy == args[0]);
            }
            if ("hashCode".equals(name)) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if ("toString".equals(name)) {
                return "CodenameOneOnBackInvokedCallback";
            }
            return null;
        }
    }
}
