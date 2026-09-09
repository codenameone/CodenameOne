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
package com.codename1.impl.android.referrer;

import android.content.Context;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.codename1.analytics.invite.InstallReferrerCallback;
import com.codename1.analytics.invite.InstallReferrerSource;
import com.codename1.analytics.invite.Invites;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.io.Log;
import com.codename1.io.Preferences;

/// Reads the Play Store install referrer, which is the deterministic half of
/// invite attribution on Android: the invite code makes the whole round trip
/// through the store and comes back verbatim, so nothing has to be matched or
/// guessed.
///
/// Compiled inside the generated application rather than into the port jar,
/// because it names `com.android.installreferrer`, which
/// `PlatformFeatureCatalog` adds only for an application that referenced the
/// invite package. `AndroidGradleBuilder` deletes this package for every other
/// application, and splices the registration call for the ones that kept it.
public class AndroidInstallReferrer implements InstallReferrerSource {
    // The referrer is retained by Google for the life of the install and
    // returns the same answer every time, so one successful read is enough
    // and the flag is what stops a service bind on every launch.
    private static final String PREF_ATTEMPTED = "cn1$invite$referrerAttempted";

    private boolean retried;

    @Override
    public boolean isSupported() {
        return AndroidNativeUtil.getContext() != null
                && !Preferences.get(PREF_ATTEMPTED, false);
    }

    @Override
    public void requestReferrer(InstallReferrerCallback callback) {
        Context context = AndroidNativeUtil.getContext();
        if (context == null) {
            callback.onUnavailable(Invites.REASON_UNSUPPORTED);
            return;
        }
        try {
            connect(InstallReferrerClient.newBuilder(context).build(), callback);
        } catch (Throwable t) {
            // A missing or broken store client must read as "no referral",
            // never as a crash: the application still works, it simply has no
            // invite behind it.
            Log.e(t);
            finish(callback, Invites.REASON_UNSUPPORTED);
        }
    }

    private void connect(final InstallReferrerClient client,
            final InstallReferrerCallback callback) {
        client.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                try {
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            deliver(client, callback);
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            // Transient. Exactly one retry: a loop here would
                            // bind the service repeatedly on a device that is
                            // never going to answer.
                            if (!retried) {
                                retried = true;
                                close(client);
                                requestReferrer(callback);
                                return;
                            }
                            // Transient, so it is NOT recorded as attempted.
                            // Burning the once-only flag here would make
                            // isSupported() false for ever, and a later
                            // Invites.flush() after the store recovered would
                            // skip the deterministic path and fall back to a
                            // statistical guess for a referrer we could have
                            // read exactly.
                            callback.onUnavailable(Invites.REASON_NO_MATCH);
                            break;
                        default:
                            // FEATURE_NOT_SUPPORTED is the ordinary answer on a
                            // device with no Play Store -- a sideload, an
                            // emulator without store services, another vendor's
                            // store. Terminal: this device will never have a
                            // referrer, so the flag is recorded and the bind is
                            // not attempted again.
                            finish(callback, Invites.REASON_UNSUPPORTED);
                            break;
                    }
                } catch (Throwable t) {
                    // Unknown failure: treated as transient, so a later flush
                    // can still read a referrer that is genuinely there.
                    Log.e(t);
                    callback.onUnavailable(Invites.REASON_NO_MATCH);
                } finally {
                    close(client);
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                // Deliberately not reconnecting. The one retry above is the
                // whole allowance; an automatic reconnect here is how a
                // background service bind loop starts.
            }
        });
    }

    private void deliver(InstallReferrerClient client, InstallReferrerCallback callback) {
        String referrer = "";
        long clickSeconds = 0;
        long beginSeconds = 0;
        try {
            ReferrerDetails details = client.getInstallReferrer();
            if (details != null) {
                referrer = details.getInstallReferrer();
                clickSeconds = details.getReferrerClickTimestampSeconds();
                beginSeconds = details.getInstallBeginTimestampSeconds();
            }
        } catch (Throwable t) {
            Log.e(t);
        }
        Preferences.set(PREF_ATTEMPTED, true);
        if (referrer == null || referrer.length() == 0) {
            callback.onUnavailable(Invites.REASON_NO_MATCH);
            return;
        }
        callback.onReferrer(referrer, clickSeconds, beginSeconds);
    }

    private void finish(InstallReferrerCallback callback, String reason) {
        Preferences.set(PREF_ATTEMPTED, true);
        callback.onUnavailable(reason);
    }

    private void close(InstallReferrerClient client) {
        try {
            client.endConnection();
        } catch (Throwable t) {
            Log.e(t);
        }
    }
}
