/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.codename1.util.AsyncResource;

/// The one place outside an optional feature package that talks to Google Play
/// services, the Play Store or Firebase.
///
/// Codename One applications run unmodified on Android builds that ship without
/// Google's apps -- GrapheneOS, CalyxOS, /e/OS, LineageOS without GApps, Amazon
/// and Huawei devices -- and on GrapheneOS where Play services is installed as an
/// ordinary sandboxed app that the user may remove at any time. On those devices
/// the Play services client libraries are still inside the APK, so nothing fails
/// to load; what fails is the service behind them. A `GoogleApiClient` never
/// connects, a `Task` completes with `API_UNAVAILABLE`, and a connection
/// resolution shows the "Get Google Play services" prompt. Every entry point
/// therefore asks [#isPlayServicesAvailable(Context)] first and falls back or
/// reports itself unsupported.
///
/// Nothing here names a Google type at compile time. Everything is reached by
/// name, so the class compiles into the port jar and loads on a device where the
/// libraries were never bundled. `scripts/check-android-gms-isolation.py` holds
/// the rest of the port to that: a class that names a Google Play type must be
/// listed there, and must be one that guards its calls this way.
///
/// Package visibility: from API 30 `PackageManager` only answers for packages the
/// manifest declares in `<queries>`. The builder declares both packages below for
/// every application, so these lookups see them.
public final class GoogleServices {
    /// The package of Google Play services.
    public static final String PLAY_SERVICES_PACKAGE = "com.google.android.gms";

    /// The package of the Google Play Store.
    public static final String PLAY_STORE_PACKAGE = "com.android.vending";

    private static final String LEGACY_MARKET_PACKAGE = "com.google.market";

    private static final String API_AVAILABILITY =
            "com.google.android.gms.common.GoogleApiAvailability";

    private static final String INTEGRITY_FACTORY =
            "com.google.android.play.core.integrity.IntegrityManagerFactory";

    private GoogleServices() {
    }

    /// Whether Google Play services is installed, enabled and usable here.
    ///
    /// The package must be present and enabled -- a user can disable it, and
    /// GrapheneOS lets them uninstall it. When the application bundles the Play
    /// services base library its own verdict is asked as well, so a version too
    /// old for the bundled client, or a service still being set up, answers false
    /// rather than handing the caller a client that will never connect.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context of the application
    ///
    /// #### Returns
    ///
    /// true only when calls into Play services can succeed
    public static boolean isPlayServicesAvailable(Context context) {
        if (context == null || !isPackageEnabled(context, PLAY_SERVICES_PACKAGE)) {
            return false;
        }
        try {
            Class<?> availability = Class.forName(API_AVAILABILITY);
            Object instance = availability.getMethod("getInstance").invoke(null);
            Object result = availability.getMethod("isGooglePlayServicesAvailable", Context.class)
                    .invoke(instance, context);
            // ConnectionResult.SUCCESS is 0. Anything else -- SERVICE_MISSING,
            // SERVICE_DISABLED, SERVICE_VERSION_UPDATE_REQUIRED, SERVICE_INVALID --
            // is a client that would sit waiting or ask the user to fix the device.
            return result instanceof Integer && ((Integer) result).intValue() == 0;
        } catch (ClassNotFoundException notBundled) {
            // The application never bundled the base library, so nothing in it will
            // call the service through a client; the installed package is the answer.
            return true;
        } catch (Throwable err) {
            return false;
        }
    }

    /// Whether the Google Play Store is installed and enabled.
    ///
    /// Play Core APIs -- in-app review, Play Integrity -- are served by the store,
    /// not by Play services, so they ask this instead.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context of the application
    ///
    /// #### Returns
    ///
    /// true when the Play Store app is present and enabled
    public static boolean isPlayStoreInstalled(Context context) {
        return context != null && (isPackageEnabled(context, PLAY_STORE_PACKAGE)
                || isPackageEnabled(context, LEGACY_MARKET_PACKAGE));
    }

    /// Whether `packageName` is installed and enabled.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context of the application
    /// - `packageName`: the package to look up
    ///
    /// #### Returns
    ///
    /// true when it is installed and not disabled
    public static boolean isPackageEnabled(Context context, String packageName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return info != null && info.enabled;
        } catch (PackageManager.NameNotFoundException absent) {
            return false;
        } catch (Throwable err) {
            return false;
        }
    }

    /// Subscribes to or unsubscribes from a Firebase Cloud Messaging topic.
    ///
    /// Topics are an FCM feature. Without Play services, or with another push
    /// transport registered, there is nothing to subscribe on the device and the
    /// application's server has to do the fan-out; this says so in the log.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context of the application
    /// - `methodName`: `subscribeToTopic` or `unsubscribeFromTopic`
    /// - `topic`: the topic name
    static void firebaseTopic(Context context, String methodName, String topic) {
        if (!isPlayServicesAvailable(context)) {
            com.codename1.io.Log.p("Google Play services is not available; topic '" + topic
                    + "' subscription must be handled server side");
            return;
        }
        try {
            Class<?> cls = Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            Object instance = cls.getMethod("getInstance").invoke(null);
            cls.getMethod(methodName, String.class).invoke(instance, topic);
        } catch (ClassNotFoundException notAvailable) {
            com.codename1.io.Log.p("Firebase Cloud Messaging is not available; topic '" + topic
                    + "' subscription must be handled server side");
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
        }
    }

    /// Whether the Play Integrity library is bundled and the Play Store that
    /// serves it is installed.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context of the application
    ///
    /// #### Returns
    ///
    /// true when a token request can be attempted
    static boolean isPlayIntegritySupported(Context context) {
        try {
            Class.forName(INTEGRITY_FACTORY);
        } catch (Throwable t) {
            return false;
        }
        return isPlayStoreInstalled(context);
    }

    /// Requests a Play Integrity token for `nonce`, completing `result`.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context of the application
    /// - `nonce`: the request nonce
    /// - `result`: completed with the token or an error
    static void requestIntegrityToken(Context context, final String nonce,
            final AsyncResource<String> result) {
        if (context != null && !isPlayStoreInstalled(context)) {
            // Answered here with a reason a developer can act on, rather than as
            // Play Core's numeric error code from a service that is not there.
            result.error(new UnsupportedOperationException(
                    "Google Play Integrity needs the Google Play Store, which is not installed"));
            return;
        }
        try {
            Class factory = Class.forName("com.google.android.play.core.integrity.IntegrityManagerFactory");
            Object manager = factory.getMethod("create", Context.class).invoke(null, context);
            Class requestClass = Class.forName("com.google.android.play.core.integrity.IntegrityTokenRequest");
            Object builder = requestClass.getMethod("builder").invoke(null);
            builder = builder.getClass().getMethod("setNonce", String.class).invoke(builder, nonce);
            Object request = builder.getClass().getMethod("build").invoke(builder);
            Class managerClass = Class.forName("com.google.android.play.core.integrity.IntegrityManager");
            Object task = managerClass.getMethod("requestIntegrityToken", requestClass).invoke(manager, request);

            Class taskClass = Class.forName("com.google.android.gms.tasks.Task");
            Class onSuccessClass = Class.forName("com.google.android.gms.tasks.OnSuccessListener");
            Class onFailureClass = Class.forName("com.google.android.gms.tasks.OnFailureListener");
            final Class responseClass = Class.forName("com.google.android.play.core.integrity.IntegrityTokenResponse");

            Object successListener = java.lang.reflect.Proxy.newProxyInstance(
                    onSuccessClass.getClassLoader(), new Class[] { onSuccessClass },
                    new java.lang.reflect.InvocationHandler() {
                        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                            try {
                                Object response = args[0];
                                Object token = responseClass.getMethod("token").invoke(response);
                                // Tested rather than cast into the catch below: a
                                // wrong type here is a bad token rather than a
                                // failed call, and a reflective call's answer is
                                // exactly the kind of value worth testing.
                                if (token instanceof String) {
                                    result.complete((String) token);
                                } else {
                                    result.error(new IllegalStateException(
                                            "integrity token was not a string"));
                                }
                            } catch(Throwable t) {
                                result.error(t);
                            }
                            return null;
                        }
                    });
            Object failureListener = java.lang.reflect.Proxy.newProxyInstance(
                    onFailureClass.getClassLoader(), new Class[] { onFailureClass },
                    new java.lang.reflect.InvocationHandler() {
                        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                            Throwable err = (args != null && args.length > 0 && args[0] instanceof Throwable)
                                    ? (Throwable) args[0] : new RuntimeException("Play Integrity request failed");
                            result.error(err);
                            return null;
                        }
                    });
            taskClass.getMethod("addOnSuccessListener", onSuccessClass).invoke(task, successListener);
            taskClass.getMethod("addOnFailureListener", onFailureClass).invoke(task, failureListener);
        } catch(ClassNotFoundException notBundled) {
            result.error(new UnsupportedOperationException(
                    "Google Play Integrity is not bundled. Enable the android.playIntegrity build hint."));
        } catch(Throwable t) {
            result.error(t);
        }
    }
}
