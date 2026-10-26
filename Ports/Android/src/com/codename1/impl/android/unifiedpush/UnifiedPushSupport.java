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
package com.codename1.impl.android.unifiedpush;

import android.app.Activity;
import android.content.Context;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.push.PushCallback;
import com.codename1.ui.Display;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.unifiedpush.android.connector.UnifiedPush;

/// Registers the application with a UnifiedPush distributor.
///
/// UnifiedPush is the push transport of Android builds without Google Play
/// services: a distributor app the user chose (ntfy, NextPush, Sunup and others)
/// keeps the one connection to a push server, and hands this application an
/// endpoint URL. Since version 2 of the specification that endpoint is a
/// standard Web Push (RFC 8030) resource, with the message encrypted to keys the
/// connector generates (RFC 8291) and the sender identified by VAPID (RFC 8292).
///
/// So the subscription is registered with the push service exactly as a
/// browser's is -- device key `cn1-web-` followed by the base64url subscription
/// JSON -- and the service delivers it with its Web Push sender and the
/// application's VAPID credential. No Codename One server needs to know that the
/// endpoint belongs to a phone. [CN1UnifiedPushService] receives the endpoint and
/// the decrypted messages.
///
/// The builder compiles this package only into an application that uses
/// `com.codename1.push.PushClient` and turned UnifiedPush on, together with the
/// connector library; the port jar never compiles it.
public final class UnifiedPushSupport {
    /// The connector's default registration instance.
    static final String INSTANCE = "default";

    private UnifiedPushSupport() {
    }

    /// Whether a UnifiedPush distributor is installed on this device.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context of the application
    ///
    /// #### Returns
    ///
    /// true when at least one distributor app is installed
    public static boolean isDistributorInstalled(Context context) {
        try {
            return !UnifiedPush.getDistributors(context).isEmpty();
        } catch (Throwable err) {
            return false;
        }
    }

    /// Registers with the distributor the user already linked, the system
    /// default one, or one the user picks, in that order.
    ///
    /// The endpoint arrives later, in [CN1UnifiedPushService]. Every failure --
    /// no distributor, the user declining to pick one, a VAPID key the connector
    /// rejects -- reaches the push callback as a registration error.
    ///
    /// #### Parameters
    ///
    /// - `activity`: the running activity; picking a distributor shows one
    /// - `vapidPublicKey`: the application's VAPID public key, base64url, or null
    public static void register(final Activity activity, final String vapidPublicKey) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    UnifiedPush.tryUseCurrentOrDefaultDistributor(activity,
                            new Function1<Boolean, Unit>() {
                                public Unit invoke(Boolean linked) {
                                    if (Boolean.TRUE.equals(linked)) {
                                        registerWithDistributor(activity, vapidPublicKey);
                                    } else if (isDistributorInstalled(activity)) {
                                        pickDistributor(activity, vapidPublicKey);
                                    } else {
                                        fail("No push service is available: Google Play services is "
                                                + "not installed and no UnifiedPush distributor "
                                                + "(for example ntfy) is installed");
                                    }
                                    return Unit.INSTANCE;
                                }
                            });
                } catch (Throwable err) {
                    fail("UnifiedPush registration failed: " + err);
                }
            }
        });
    }

    private static void pickDistributor(final Activity activity, final String vapidPublicKey) {
        UnifiedPush.tryPickDistributor(activity, new Function1<Boolean, Unit>() {
            public Unit invoke(Boolean picked) {
                if (Boolean.TRUE.equals(picked)) {
                    registerWithDistributor(activity, vapidPublicKey);
                } else {
                    fail("No UnifiedPush distributor was chosen");
                }
                return Unit.INSTANCE;
            }
        });
    }

    private static void registerWithDistributor(Context context, String vapidPublicKey) {
        try {
            String label = context.getApplicationInfo().loadLabel(context.getPackageManager())
                    .toString();
            String vapid = vapidPublicKey == null || vapidPublicKey.length() == 0
                    ? null : vapidPublicKey;
            UnifiedPush.register(context, INSTANCE, label, vapid);
        } catch (Throwable err) {
            fail("UnifiedPush registration failed: " + err);
        }
    }

    /// Removes the registration. The distributor confirms in
    /// [CN1UnifiedPushService#onUnregistered(String)].
    ///
    /// #### Parameters
    ///
    /// - `context`: any context of the application
    public static void unregister(Context context) {
        try {
            UnifiedPush.unregister(context, INSTANCE);
        } catch (Throwable err) {
            com.codename1.io.Log.e(err);
        }
    }

    /// Reports a registration failure to the application's push callback.
    ///
    /// #### Parameters
    ///
    /// - `message`: what went wrong
    static void fail(final String message) {
        com.codename1.io.Log.p(message);
        if (!Display.isInitialized()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                PushCallback callback = CodenameOneImplementation.getPushCallback();
                if (callback != null) {
                    callback.pushRegistrationError(message, 0);
                }
            }
        });
    }
}
