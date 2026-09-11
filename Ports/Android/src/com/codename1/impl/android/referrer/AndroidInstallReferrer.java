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

    /// The installation the flag above belongs to. See
    /// [#forgetAflagRestoredFromAnotherInstallation].
    private static final String PREF_INSTALL_TIME = "cn1$invite$referrerInstallTime";

    private boolean retried;

    // Whether the framework has been given its one answer FOR THIS EXCHANGE.
    // The SPI promises exactly one call, and the disconnect handler below can
    // arrive after a real answer as easily as instead of one -- ending a
    // connection is itself what fires it.
    //
    // Reset by requestReferrer, and that reset is load bearing. Invites keeps
    // one source instance and calls it again on a later flush; left set from a
    // transient failure, this suppressed the retry's answer while deliver()
    // had already recorded the read as attempted, so an exact code was read
    // and thrown away and no relaunch could ask for it again.
    private boolean answered;

    // Which attempt is current. Every bind captures this and answers only
    // while it still matches.
    //
    // One counter rather than a flag per attempt, because two different things
    // supersede a listener and both have to be caught: the retry below, whose
    // close() fires the OLD listener's disconnect, and a later flush, which
    // starts a whole new exchange that a lingering listener from the previous
    // one would otherwise answer.
    private int attemptSeq;

    @Override
    public boolean isSupported() {
        Context context = AndroidNativeUtil.getContext();
        if (context == null) {
            return false;
        }
        forgetAflagRestoredFromAnotherInstallation(context);
        return !Preferences.get(PREF_ATTEMPTED, false);
    }

    /// Clears the one-shot flag when it came from a DIFFERENT installation.
    ///
    /// Android's auto-backup is on by default -- `AndroidGradleBuilder` leaves
    /// `android:allowBackup` alone -- so a reinstall or a device migration
    /// restores this app's files, this flag among them. Restored, it says the
    /// referrer has already been read, and the new installation never asks: its
    /// own Play referrer, which is the one exact answer this whole path
    /// exists for, is thrown away before anything looks at it.
    ///
    /// `firstInstallTime` is what separates the two. It survives an app
    /// UPDATE, so an ordinary upgrade is not mistaken for a new install, and a
    /// restore into a new installation carries the OLD value in preferences
    /// while the package manager reports the new one. Unknown means this code
    /// is running for the first time on an install that predates it, which is
    /// not evidence of anything and stamps rather than clears.
    ///
    /// #### What this does NOT cover
    ///
    /// Only the flag this class owns. A restore also brings back the invite
    /// records themselves -- the resolved attribution above all -- so a device
    /// migrated from another one can still report the previous installation's
    /// inviter as its own. Fixing that needs a core entry point meaning "this
    /// is a new installation, forget the last one but stay attributable", and
    /// the one public method that comes close, `Invites.reset()`, is the
    /// erasure: it writes a terminal marker, which would leave the new install
    /// permanently unattributable -- worse than the problem. iOS has the same
    /// exposure through device transfer and no equivalent signal here at all.
    /// Left as a deliberate gap rather than guessed at.
    private void forgetAflagRestoredFromAnotherInstallation(Context context) {
        try {
            long current = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).firstInstallTime;
            if (current <= 0L) {
                return;
            }
            long known = Preferences.get(PREF_INSTALL_TIME, 0L);
            if (known == 0L) {
                Preferences.set(PREF_INSTALL_TIME, current);
                return;
            }
            if (known != current) {
                Preferences.set(PREF_INSTALL_TIME, current);
                Preferences.set(PREF_ATTEMPTED, false);
            }
        } catch (Throwable t) {
            // A package manager that cannot describe this app's own package is
            // not a state to guess in: leaving the flag alone keeps the
            // ordinary behaviour rather than re-reading a referrer that may
            // genuinely have been consumed.
            com.codename1.io.Log.e(t);
        }
    }

    @Override
    public void requestReferrer(InstallReferrerCallback callback) {
        // A NEW exchange, so both guards start clean. The internal retry does
        // not come through here -- it calls attempt() directly -- because
        // resetting `retried` there would turn one allowance into a loop.
        answered = false;
        retried = false;
        attempt(callback);
    }

    private void attempt(InstallReferrerCallback callback) {
        attemptSeq++;
        Context context = AndroidNativeUtil.getContext();
        if (context == null) {
            unavailable(attemptSeq, callback, Invites.REASON_UNSUPPORTED);
            return;
        }
        try {
            connect(InstallReferrerClient.newBuilder(context).build(), callback);
        } catch (Throwable t) {
            // A missing or broken store client must read as "no referral",
            // never as a crash: the application still works, it simply has no
            // invite behind it.
            Log.e(t);
            finish(attemptSeq, callback, Invites.REASON_UNSUPPORTED);
        }
    }

    private void connect(final InstallReferrerClient client,
            final InstallReferrerCallback callback) {
        // Captured, not read at callback time. The retry below ends this
        // connection, which fires this listener's own disconnect, and a later
        // flush starts a whole new exchange -- a listener that read the field
        // when it fired would inherit whichever attempt is current and answer
        // for it.
        final int issued = attemptSeq;
        client.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                try {
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            deliver(issued, client, callback);
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            // Transient. Exactly one retry: a loop here would
                            // bind the service repeatedly on a device that is
                            // never going to answer.
                            if (!retried) {
                                retried = true;
                                // The sequence advances BEFORE the close, and
                                // that order is the whole guard. Ending a
                                // connection is what fires its own listener's
                                // disconnect, so closing while this attempt is
                                // still current lets that disconnect answer
                                // the exchange the retry was about to make --
                                // with "no referral", for a store that had not
                                // been asked yet. attempt() advances it again,
                                // which only skips a number.
                                attemptSeq++;
                                close(client);
                                attempt(callback);
                                return;
                            }
                            // Transient, so it is NOT recorded as attempted.
                            // Burning the once-only flag here would make
                            // isSupported() false for ever, and a later
                            // Invites.flush() after the store recovered would
                            // skip the deterministic path and fall back to a
                            // statistical guess for a referrer we could have
                            // read exactly.
                            unavailable(issued, callback, Invites.REASON_NO_MATCH);
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED:
                            // The SAME transient state the disconnect callback
                            // reports, arriving through the response code
                            // instead -- and it is a separate path, so
                            // handling one and not the other left this one
                            // falling into the terminal default below. That
                            // burnt the once-only flag and permanently refused
                            // another read, for an invited install whose exact
                            // Play referrer was still there on the next
                            // connection.
                            unavailable(issued, callback, Invites.REASON_NO_MATCH);
                            break;
                        default:
                            // FEATURE_NOT_SUPPORTED is the ordinary answer on a
                            // device with no Play Store -- a sideload, an
                            // emulator without store services, another vendor's
                            // store. Terminal: this device will never have a
                            // referrer, so the flag is recorded and the bind is
                            // not attempted again.
                            finish(issued, callback, Invites.REASON_UNSUPPORTED);
                            break;
                    }
                } catch (Throwable t) {
                    // Unknown failure: treated as transient, so a later flush
                    // can still read a referrer that is genuinely there.
                    Log.e(t);
                    unavailable(issued, callback, Invites.REASON_NO_MATCH);
                } finally {
                    close(client);
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                // Still deliberately not reconnecting. The one retry above is
                // the whole allowance; an automatic reconnect here is how a
                // background service bind loop starts.
                //
                // But the exchange has to END, and this was the one path that
                // left it open. A service that drops before
                // onInstallReferrerSetupFinished() ever runs answered nothing,
                // so Invites kept its lookup outstanding and its deferred flag
                // set: the application's listener was never told anything, and
                // nothing retried until the next cold launch.
                //
                // Reported as transient, which is what it is -- the once-only
                // flag stays unburnt, so a later flush can still read a
                // referrer that was there the whole time.
                unavailable(issued, callback, Invites.REASON_NO_MATCH);
            }
        });
    }

    private void deliver(int issued, InstallReferrerClient client,
            InstallReferrerCallback callback) {
        String referrer = "";
        long clickSeconds = 0;
        long beginSeconds = 0;
        boolean threw = false;
        try {
            ReferrerDetails details = client.getInstallReferrer();
            if (details != null) {
                referrer = details.getInstallReferrer();
                clickSeconds = details.getReferrerClickTimestampSeconds();
                beginSeconds = details.getInstallBeginTimestampSeconds();
            }
        } catch (Throwable t) {
            // The connection came up and the read failed -- a RemoteException
            // from the service, most often. That is the same kind of transient
            // failure as a bind that never succeeded, and it is not evidence
            // about whether a referrer exists.
            threw = true;
            Log.e(t);
        }
        if (threw) {
            // Deliberately NOT recorded as attempted. Burning the once-only
            // flag here makes isSupported() false for ever, so a later
            // Invites.flush() skips the deterministic path entirely and a
            // statistical no-match settles the install as organic -- for a
            // referrer that was there all along and simply could not be read
            // this once.
            unavailable(issued, callback, Invites.REASON_NO_MATCH);
            return;
        }
        if (referrer == null || referrer.length() == 0) {
            // Read successfully and there is no invite behind this install.
            // Definitive, so the flag is burnt: asking again cannot change it.
            //
            // Only when THIS exchange still owns the answer -- see burn().
            if (unavailable(issued, callback, Invites.REASON_NO_MATCH)) {
                Preferences.set(PREF_ATTEMPTED, true);
            }
            return;
        }
        // The handoff FIRST, the flag after.
        //
        // Burning the flag before handing the referrer over meant a process
        // killed in between lost the exact code for ever: the next launch saw
        // isSupported() false and settled the invited install as no-match.
        // Invites persists the code inside this call when it runs on the EDT,
        // which is the common case.
        //
        // It is not a guarantee, and saying so is the point: the framework
        // marshals onto the EDT, so when this callback arrives on a binder
        // thread the persist is queued rather than done, and a process killed
        // inside that window still loses it. The window goes from "always" to
        // "the callSerially latency", which is the most the SPI shape allows
        // without the port knowing what the framework did with the value.
        if (referrer(issued, callback, referrer, clickSeconds, beginSeconds)) {
            Preferences.set(PREF_ATTEMPTED, true);
        }
    }

    /// The one-shot flag is burnt by the exchange that ANSWERED, and only by
    /// it.
    ///
    /// A bind that outlives the retry interval leaves its callback pending
    /// while a later checkForInvite() starts a fresh exchange. When the first
    /// one finally lands it is superseded -- `issued != attemptSeq` -- and both
    /// delivery methods below drop it on purpose, because the newer exchange
    /// owns the outcome. Burning the flag anyway performed the one side effect
    /// that cannot be undone: if the newer exchange then failed transiently,
    /// every later launch saw isSupported() as false and the exact Play
    /// referrer was gone, for an install that really did have one.
    private void finish(int issued, InstallReferrerCallback callback, String reason) {
        if (unavailable(issued, callback, reason)) {
            Preferences.set(PREF_ATTEMPTED, true);
        }
    }

    /// Reports "no referral", at most once.
    ///
    /// Every terminal path goes through here so the disconnect handler can
    /// close an exchange nobody else closed without risking a second answer
    /// for one that somebody did.
    private boolean unavailable(int issued, InstallReferrerCallback callback, String reason) {
        if (answered || issued != attemptSeq) {
            return false;
        }
        answered = true;
        callback.onUnavailable(reason);
        return true;
    }

    private boolean referrer(int issued, InstallReferrerCallback callback, String value,
            long clickSeconds, long beginSeconds) {
        if (answered || issued != attemptSeq) {
            return false;
        }
        answered = true;
        callback.onReferrer(value, clickSeconds, beginSeconds);
        return true;
    }

    private void close(InstallReferrerClient client) {
        try {
            client.endConnection();
        } catch (Throwable t) {
            Log.e(t);
        }
    }
}
