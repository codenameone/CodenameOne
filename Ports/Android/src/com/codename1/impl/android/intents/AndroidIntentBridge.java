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
package com.codename1.impl.android.intents;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.intents.IntentCompletion;
import com.codename1.intents.IntentResult;
import com.codename1.intents.IntentSource;
import com.codename1.intents.Intents;
import com.codename1.intents.spi.IntentBridge;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/// Android `IntentBridge`.
///
/// #### What Android can and cannot do here
///
/// Android has no contract by which an assistant invokes an application capability and receives
/// a typed result back, so `isVoiceInvocationSupported()` answers **false** and the framework
/// documents phrases, system disambiguation and spoken results as iOS-only. Saying otherwise
/// would be the kind of promise that only fails on a user's device.
///
/// What Android does deliver is real and useful: launcher shortcuts, dynamic shortcuts the
/// system learns to suggest from donation, and genuinely headless execution -- the last of which
/// is actually easier here than on iOS, because the port already boots without an Activity for
/// background fetch.
///
/// Indexing maps onto long-lived dynamic shortcuts rather than a separate search index. That is
/// Google's own current recommendation for surfacing app content, and it needs no additional
/// dependency, which keeps the "zero cost when unused" promise intact for every app that does
/// not use this package.
public class AndroidIntentBridge implements IntentBridge {

    private static final String TAG = "CN1Intents";
    /// The scheme the generated shortcuts and the trampoline agree on.
    public static final String SCHEME = "cn1intent";
    private static final int MAX_SHORTCUTS = 10;
    /// API levels named as literals rather than through `Build.VERSION_CODES`, because the port
    /// compiles against an older `android.jar` than these releases; the constants do not exist
    /// there even though the runtime values are fixed and public.
    private static final int API_LONG_LIVED = 29;
    private static final int API_PUSH_DYNAMIC = 30;
    /// ShortcutManager.getShortcuts(int) and the cached-shortcut concept both arrive here.
    private static final int API_MATCH_FLAGS = 30;
    /// How long a parked foreground request waits for the app to actually appear.
    private static final long FOREGROUND_WAIT_MILLIS = 15000L;
    /// Stamped on every shortcut published for an indexed entity, so clearIndex can tell this
    /// framework's rows from the application's own without guessing.
    /// What the launcher will show of a label before it truncates anyway.
    private static final int MAX_LABEL_CHARS = 40;

    private static final String INDEXED_PREFIX = "cn1entity:";

    /// Stamped on a donated shortcut, so it cannot collide with the immutable manifest shortcut
    /// the build published for the same intent.
    private static final String DONATED_PREFIX = "cn1donated:";

    private static final long FOREGROUND_POLL_MILLIS = 50L;

    /// How long requestForeground waits for the Activity it launched. Deliberately shorter than
    /// CN1IntentService's BACKSTOP_MARGIN_SECONDS: the service must not give up on the latch
    /// while this is still waiting.
    private static final long FOREGROUND_LAUNCH_WAIT_MILLIS = 3000L;

    private final List<String> indexed = new ArrayList<String>();
    /// A cold-start request waiting for the declaration table to exist.
    private static final List<String> PARKED = new ArrayList<String>();

    public boolean areIntentsSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
    }

    public boolean isHeadlessExecutionSupported() {
        // AndroidImplementation.startContext boots Display with a Service context, which is the
        // same path background fetch already uses in production -- but only where something can
        // ask it to. Every entry point on this platform is a shortcut, and shortcuts do not
        // exist below N_MR1, so answering true there described a capability with no way to be
        // invoked. An application reads these to decide what to offer, and would have offered a
        // headless action that nothing could ever start.
        return areIntentsSupported();
    }

    public boolean isVoiceInvocationSupported() {
        return false;
    }

    public boolean isIndexingSupported() {
        return areIntentsSupported();
    }

    /// Called by the framework once the generated dispatcher installs itself, which is the
    /// first moment the declaration table exists.
    ///
    /// That makes it the right place to settle a request that arrived before it did. A
    /// build-time static shortcut carries no nonce -- there is no runtime at build time to mint
    /// one -- so on a cold start the trampoline cannot yet tell whether the request is one this
    /// application published or one an arbitrary app fabricated. Rejecting outright would break
    /// every static shortcut, so the request is parked and judged here instead, against the same
    /// policy an unauthenticated request is always held to.
    public void registerIntents(String declarationsJson) {
        String parked;
        synchronized (PARKED) {
            parked = PARKED.isEmpty() ? null : PARKED.remove(0);
            PARKED.clear();
        }
        if (parked == null) {
            return;
        }
        if (!CN1IntentTrampolineActivity.isSafeForUntrustedCallers(parked)) {
            Log.w(TAG, "Refusing the parked unauthenticated request for \"" + parked + "\"");
            return;
        }
        // Parameters were dropped at the door, so this runs exactly as declared -- including
        // its headless flag, which is only knowable now. Parking one as foreground made the
        // first tap after process death visibly open the app for an intent that declared
        // headless=true, and the trampoline had already foregrounded by then.
        com.codename1.intents.IntentDeclaration decl = Intents.getDeclaration(parked);
        boolean headless = decl != null && decl.runsHeadless();
        if (!headless) {
            // This runs from registerIntents, which the stub calls while the app instance is
            // still being built -- before init()/start() have produced a Form. Dispatching a
            // non-headless handler here would hand it the same absent window the foreground
            // queue exists to wait for, so it joins that queue rather than taking a shortcut
            // around it. Parameters were dropped at the door, so there are none to carry.
            parkForegroundRequest(parked, null);
            // And something has to open a window for it. The trampoline reached the service
            // rather than an Activity because the shortcut's URI said headless -- a cached or
            // pinned shortcut minted before an update that made this declaration foreground, or
            // gave it a route. The declaration is the authority and it says otherwise, so the
            // queue this just joined is waiting for a window nobody asked for: the service
            // finishes, the waiter polls out its bound, and the shortcut does nothing at all.
            // The service's own migration path launches for exactly this reason; this one is
            // the same case reached through the parked door.
            requestForegroundStatic();
            return;
        }
        // The completion is what CN1IntentService.wakeRuntime waits on. Without it the service
        // that started this runtime has no idea when the handler finished, and tearing the
        // runtime down underneath a handler that was still working loses whatever it was doing.
        int budget = decl == null ? Intents.getDefaultTimeout() : decl.getTimeoutSeconds();
        parkedBudgetSeconds = budget;
        Intents.dispatchInvocation(parked, null, IntentSource.SHORTCUT, headless,
                new ParkedCompletion());
    }

    /// Signals the service waiting on the parked invocation. Named and static for the same
    /// reason as ForegroundWaiter: an anonymous class would hold the bridge instance, and this
    /// outlives the call that created it.
    private static final class ParkedCompletion implements IntentCompletion {
        @Override
        public void onIntentResult(IntentResult r) {
            synchronized (PARKED) {
                parkedFinished = true;
                PARKED.notifyAll();
            }
        }
    }

    /// Set once the parked invocation reports its outcome. Guarded by PARKED.
    private static boolean parkedFinished;
    /// The declared budget of whatever registerIntents dispatched, for the service to wait out.
    private static volatile int parkedBudgetSeconds;

    /// Waits for the request `registerIntents` dispatched, so the service that booted the
    /// runtime for it does not tear that runtime down while the handler is still running.
    ///
    /// Returns when the handler completes, when its declared budget expires, or immediately if
    /// nothing was dispatched.
    ///
    /// #### Parameters
    ///
    /// - `marginSeconds`: extra time beyond the declared budget, matching the service's own
    ///   backstop
    static void awaitParkedCompletion(int marginSeconds) throws InterruptedException {
        long deadline;
        synchronized (PARKED) {
            if (parkedBudgetSeconds == 0) {
                // registerIntents ran and dispatched nothing -- refused, or nothing was parked.
                return;
            }
            deadline = System.currentTimeMillis()
                    // Widened before the addition: two ints whose sum can exceed what an int
                    // holds, and a negative deadline makes this return immediately.
                    + ((long) parkedBudgetSeconds + marginSeconds) * 1000L;
            while (!parkedFinished) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    Log.w(TAG, "A parked intent did not complete within its budget");
                    return;
                }
                PARKED.wait(remaining);
            }
            // Reset so a second cold start in the same process does not see a stale answer.
            parkedFinished = false;
            parkedBudgetSeconds = 0;
        }
    }

    /// Requests that must not run until the app is actually in front, with the parameters the
    /// trampoline accepted.
    ///
    /// A non-headless handler is allowed to touch a `Form` -- that is precisely what the flag
    /// means -- so dispatching it the moment a launcher tap arrives runs it against a window
    /// that does not exist yet, or against whatever the app happened to be showing when it was
    /// backgrounded. The trampoline brings the app forward and leaves the work here.
    private static final List<String[]> FOREGROUND = new ArrayList<String[]>();

    /// Records a foreground request the trampoline has just asked the launcher to bring the app
    /// forward for, and starts waiting for the window that request needs.
    ///
    /// The waiting lives here rather than in a port lifecycle callback on purpose. A hook in
    /// `CodenameOneActivity.onResume` would be a hard reference from every Android app to this
    /// package, which is exactly the cost this feature promises not to impose on an app that
    /// never declares an intent.
    static void parkForegroundRequest(String intentId, String paramsJson) {
        if (intentId == null) {
            return;
        }
        // Each request carries its own expiry rather than the waiter holding one deadline for
        // the queue: two taps a few seconds apart must not have the first one's timeout throw
        // away the second.
        String expiry = String.valueOf(System.currentTimeMillis() + FOREGROUND_WAIT_MILLIS);
        synchronized (FOREGROUND) {
            FOREGROUND.add(new String[]{intentId, paramsJson, expiry});
            if (waiting) {
                // One waiter is enough, and more than one is actively wrong: they would race to
                // drain the same queue and each would apply its own idea of when to give up.
                return;
            }
            waiting = true;
        }
        startWaiter();
    }

    /// True while a waiter thread is alive. Guarded by FOREGROUND.
    private static boolean waiting;

    /// Waits, off any UI thread, until the app has a window and then drains the queue.
    ///
    /// Bounded per request: if the app never comes forward -- the user dismissed it, the launch
    /// was refused -- that request is dropped rather than firing into whatever the app is
    /// showing minutes later. A stale capability running unannounced is worse than one that did
    /// not run.
    private static void startWaiter() {
        Thread t = new Thread(new ForegroundWaiter(), "CN1IntentForeground");
        t.setDaemon(true);
        t.start();
    }

    /// Polls for the window a parked request needs.
    ///
    /// Named and static rather than anonymous for the same reason CN1IntentService's Latch is:
    /// an anonymous inner class here holds a reference to its enclosing instance, and this
    /// outlives the bridge call that started it.
    private static final class ForegroundWaiter implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    if (isForegrounded()) {
                        deliverPendingForegroundRequests();
                        // Anything parked while that was running keeps this waiter alive.
                        // Exiting on "I just emptied it" instead loses the request: the parking
                        // thread saw waiting==true and declined to start a waiter of its own,
                        // and a moment later there is none.
                        if (finishIfIdle()) {
                            return;
                        }
                        continue;
                    }
                    if (dropExpired()) {
                        return;
                    }
                    Thread.sleep(FOREGROUND_POLL_MILLIS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                synchronized (FOREGROUND) {
                    waiting = false;
                }
            }
        }
    }

    /// True when there is a window a non-headless handler can legitimately touch: the runtime
    /// is up, an Activity is attached, and a Form is actually showing.
    private static boolean isForegrounded() {
        if (!Display.isInitialized() || Display.getInstance().getCurrent() == null
                || AndroidImplementation.getActivity() == null) {
            return false;
        }
        // A backgrounded app still has a current Form and a non-null Activity, so those two
        // alone said "foreground" the instant a tap arrived -- before launchMainActivity had
        // resumed anything. The handler then ran against a window the user cannot see, which is
        // the exact thing being non-headless is supposed to guarantee against. isMinimized is
        // the port's own answer to that question, fed by CodenameOneActivity.onStop/onResume.
        return !Display.getInstance().isMinimized();
    }

    /// Clears `waiting` and reports true only when the queue is genuinely empty, both decided
    /// under one lock so a request cannot arrive between the two.
    private static boolean finishIfIdle() {
        synchronized (FOREGROUND) {
            if (FOREGROUND.isEmpty()) {
                waiting = false;
                return true;
            }
            return false;
        }
    }

    /// Whether a parked request's wait has run out.
    ///
    /// One definition, because the waiter is not the only thing that drains the queue and the
    /// two disagreeing is how a stale request gets run. An unparseable stamp counts as expired:
    /// this code writes it, so a value that cannot be read is a bug, and running a request that
    /// cannot be dated is the worse of the two answers.
    private static boolean hasExpired(String[] request, long now) {
        try {
            return Long.parseLong(request[2]) <= now;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /// Drops requests whose wait has run out. Returns true when nothing is left to wait for,
    /// having cleared `waiting` in the same critical section so a request parked in between
    /// cannot find a waiter that is about to stop existing.
    private static boolean dropExpired() {
        long now = System.currentTimeMillis();
        synchronized (FOREGROUND) {
            for (int i = FOREGROUND.size() - 1; i >= 0; i--) {
                String[] request = FOREGROUND.get(i);
                if (hasExpired(request, now)) {
                    Log.w(TAG, "The app did not come forward; dropping \"" + request[0] + "\"");
                    FOREGROUND.remove(i);
                }
            }
            if (FOREGROUND.isEmpty()) {
                waiting = false;
                return true;
            }
            return false;
        }
    }

    /// Runs everything parked for the foreground, **if there is a foreground to run it in**.
    ///
    /// The precondition is checked here rather than assumed by the caller. The generated stub
    /// calls this while the app instance is being built, which is before the lifecycle start
    /// has created the first Form -- so a caller-trusts-its-own-timing design would dispatch a
    /// non-headless handler into a process with no window, which is the thing this whole queue
    /// exists to prevent. Anything not runnable yet stays queued for the waiter.
    ///
    /// Draining twice is harmless and dropping the work is not, which is why this is idempotent
    /// on an empty queue rather than guarded by a flag.
    public static void deliverPendingForegroundRequests() {
        if (!isForegrounded()) {
            // Not yet. Make sure somebody is watching for the window, then leave it queued.
            synchronized (FOREGROUND) {
                if (FOREGROUND.isEmpty() || waiting) {
                    return;
                }
                waiting = true;
            }
            startWaiter();
            return;
        }
        // Expiry is checked here as well as in the waiter, because the waiter is not guaranteed
        // to have run. Android can suspend or throttle it while the app sits in the background,
        // and this method is also called from the Activity's own startup -- so a request whose
        // fifteen seconds ran out long ago could be delivered on an unrelated later opening of
        // the app, which is exactly the stale invocation the expiry exists to prevent.
        List<String[]> drained = new ArrayList<String[]>();
        synchronized (FOREGROUND) {
            if (FOREGROUND.isEmpty()) {
                return;
            }
            long now = System.currentTimeMillis();
            for (int i = 0; i < FOREGROUND.size(); i++) {
                String[] request = FOREGROUND.get(i);
                if (hasExpired(request, now)) {
                    Log.w(TAG, "Not running \"" + request[0] + "\": its wait ran out before the "
                            + "app came forward");
                    continue;
                }
                drained.add(request);
            }
            FOREGROUND.clear();
        }
        if (drained.isEmpty()) {
            return;
        }
        for (String[] request : drained) {
            try {
                // Judged now rather than when it was queued. A request parked on a cold start
                // was queued before the declarations existed, so nothing could yet say whether
                // the shortcut that produced it still describes something safe to run on one
                // tap -- and a donation outlives the policy it was made under.
                com.codename1.intents.IntentDeclaration decl =
                        Intents.getDeclaration(request[0]);
                if (decl != null
                        && !CN1IntentTrampolineActivity.isStillPermittedOnOneTap(decl)) {
                    Log.w(TAG, "Refusing \"" + request[0] + "\": the shortcut predates a "
                            + "declaration change that no longer allows it to run on a single "
                            + "tap");
                    continue;
                }
                Intents.dispatchInvocation(request[0], CN1IntentService.parse(request[1]),
                        IntentSource.SHORTCUT, false, null);
            } catch (Throwable t) {
                Log.w(TAG, "Could not run a foreground intent request", t);
            }
        }
    }

    /// Records a request that arrived before the declarations existed. Only the id is kept:
    /// an unauthenticated request never gets to choose parameter values.
    static void parkUntrustedRequest(String intentId) {
        if (intentId == null) {
            return;
        }
        synchronized (PARKED) {
            // One is enough. A second tap before the runtime is up is the user pressing twice,
            // not two things to run.
            PARKED.clear();
            PARKED.add(intentId);
        }
    }

    public boolean requestForeground() {
        if (isForegrounded()) {
            // Already forward: an in-app invoke, or a shortcut that resumed a live app. Nothing
            // to launch and nothing to wait for, so this stays a fast path -- which matters,
            // because Intents.invoke is synchronous and its caller may be the EDT.
            return true;
        }
        if (!requestForegroundStatic()) {
            return false;
        }
        // startActivity() only *posts* the launch. It returns long before the Activity has
        // resumed, and the framework navigates the moment this method does -- so the routed
        // Form was built against the service's context and then torn down by the stopContext()
        // that follows the handler, with the Activity attaching to a runtime that no longer had
        // it. Waiting here is what makes "true" mean the app is forward rather than merely
        // asked to be.
        //
        // Bounded well under CN1IntentService's backstop margin: overrunning it would have the
        // service stop the context in the middle of the navigation this wait exists to protect.
        // A launch slower than that degrades to the previous behaviour rather than hanging.
        long deadline = System.currentTimeMillis() + FOREGROUND_LAUNCH_WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (isForegrounded()) {
                return true;
            }
            try {
                Thread.sleep(FOREGROUND_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        Log.w(TAG, "The app did not come forward in time for a routed intent result");
        return false;
    }

    /// Brings the app forward. Static because it reads nothing but the application context, and
    /// because CN1IntentService needs it without holding a bridge.
    static boolean requestForegroundStatic() {
        Context ctx = context();
        if (ctx == null) {
            return false;
        }
        try {
            Intent launch = ctx.getPackageManager()
                    .getLaunchIntentForPackage(ctx.getPackageName());
            if (launch == null) {
                return false;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            ctx.startActivity(launch);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Could not bring the app forward", t);
            return false;
        }
    }

    public void donate(String intentId, String paramsJson) {
        if (!areIntentsSupported()) {
            return;
        }
        Context ctx = context();
        if (ctx == null || intentId == null) {
            return;
        }
        try {
            // A shortcut outlives the process, but a parameterization lives only in memory. So
            // the shortcut has to carry what the build-time registry can actually run: the base
            // intent, with the bound values merged in. Donating the runtime id would produce a
            // shortcut that works until the app is killed and reports an unknown intent after.
            com.codename1.intents.DynamicIntent dyn = com.codename1.intents.Intents
                    .getDynamicIntent(intentId);
            String targetId = intentId;
            String effectiveParams = paramsJson;
            String label = null;
            if (dyn != null) {
                targetId = dyn.getBaseIntentId();
                label = dyn.getTitle();
                effectiveParams = com.codename1.intents.IntentSerializer.mergeParams(
                        dyn.getBoundParameters(), paramsJson);
            }
            if (label == null || label.length() == 0) {
                // An ordinary declared intent has a title, and that title is what the static
                // catalogue already shows for it. Falling through to the id here would put
                // "log_workout" on the launcher beside a "Log a workout" the build wrote.
                com.codename1.intents.IntentDeclaration decl =
                        com.codename1.intents.Intents.getDeclaration(targetId);
                label = decl == null ? intentId : decl.getTitle();
            }
            // A donation carrying no values of its own has nothing to publish that is not
            // already on the launcher -- *if* the launcher has it. The build's static shortcut
            // runs exactly the same thing, so a second entry under another id would put one
            // label there twice, and what the donation is actually saying (the user just did
            // this) is the usage report.
            //
            // Only when that shortcut exists, which is why this asks. The build omits one for
            // an intent declared discoverable=false -- whose documented meaning is precisely
            // that it appears *after* a donation -- and for anything past the static quota,
            // which the build's own log says remains donatable. Reporting usage for an id the
            // launcher does not have is a no-op, so those donations did nothing at all.
            if (dyn == null && !carriesValues(effectiveParams) && hasManifestShortcut(ctx, intentId)) {
                reportUsage(ctx, intentId);
                return;
            }
            // Anything else is published under an id this framework owns. The raw intent id is
            // the *manifest* shortcut's id, and Android does not let a runtime API modify an
            // immutable manifest shortcut -- so this used to be rejected outright, silently,
            // leaving the parameterless static shortcut in place. For an intent with optional
            // parameters that meant the tap ran the declared defaults instead of the values
            // the user had actually chosen, which is the whole content of a donation.
            // The same capacity question indexing asks. pushDynamicShortcut evicts the least
            // recently used dynamic shortcut once the cap is reached, and with the quota full
            // of manifest shortcuts and ones the application published itself, the thing
            // evicted would be one of the application's -- a launcher action removed in order
            // to record that the user did something. A donation is a hint; it is not worth
            // that. Slots this framework already owns are counted as available, so replacing
            // an earlier donation of its own still works.
            if (publishableSlots(ctx) < 1) {
                Log.i(TAG, "Not donating \"" + intentId + "\": this app's own shortcuts already "
                        + "fill the launcher's quota, and recording this one would evict one of "
                        + "them");
                return;
            }
            pushShortcut(ctx, DONATED_PREFIX + intentId, label, label,
                    uriFor(targetId, effectiveParams, ctx), null, true);
        } catch (Throwable t) {
            Log.w(TAG, "Could not donate " + intentId, t);
        }
    }

    public void index(String entitiesJson, Map<String, byte[]> images) {
        // The images map is the only place an entity's thumbnail exists -- the JSON carries the
        // name it was registered under, not the bytes.
        //
        // Carried through the loop as a parameter rather than staged on the instance: two
        // background threads indexing at once would otherwise have the second overwrite the
        // field between the first parsing its JSON and reading its blobs, so the first would
        // publish the other request's thumbnails or none.
        if (!isIndexingSupported()) {
            return;
        }
        Context ctx = context();
        if (ctx == null || entitiesJson == null) {
            return;
        }
        // Deliberately shallow parsing: the payload is a known shape produced by the framework's
        // own serializer, and pulling in a JSON dependency for the port would cost every app.
        List<String[]> entries = CN1IntentJson.entities(entitiesJson);
        int budget = publishableSlots(ctx);
        int published = 0;
        for (String[] entry : entries) {
            if (published >= budget) {
                // The launcher caps how many it will show. Truncating silently would look like
                // indexing randomly failing, so it is reported once -- and the two reasons read
                // differently to whoever has to act on them.
                if (budget == 0) {
                    Log.i(TAG, "Not indexing: this app's own shortcuts already fill the "
                            + "launcher's quota, and publishing content here would evict one "
                            + "of them");
                } else {
                    Log.i(TAG, "Indexed the first " + budget
                            + " items; Android limits how many shortcuts an app may publish");
                }
                break;
            }
            String uid = entry[0];
            String title = entry[1];
            String subtitle = entry[2];
            try {
                String imageName = entry.length > 3 ? entry[3] : null;
                String openUri = SCHEME + "://open?uid=" + Uri.encode(uid);
                String nonce = CN1IntentNonce.get(ctx);
                if (nonce != null) {
                    openUri += "&n=" + Uri.encode(nonce);
                }
                // Indexing publishes content the user has not necessarily touched.
                String shortcutId = shortcutIdFor(uid);
                pushShortcut(ctx, shortcutId, title, subtitle, Uri.parse(openUri),
                        imageFor(images, imageName), false);
                synchronized (indexed) {
                    if (!indexed.contains(shortcutId)) {
                        indexed.add(shortcutId);
                    }
                }
                published++;
            } catch (Throwable t) {
                Log.w(TAG, "Could not index " + uid, t);
            }
        }
    }

    public void removeFromIndex(String idsJson) {
        if (!isIndexingSupported()) {
            return;
        }
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        List<String> uids = CN1IntentJson.refs(idsJson);
        if (uids.isEmpty()) {
            return;
        }
        List<String> shortcutIds = new ArrayList<String>();
        for (String uid : uids) {
            shortcutIds.add(shortcutIdFor(uid));
        }
        removeShortcuts(ctx, shortcutIds);
        synchronized (indexed) {
            indexed.removeAll(shortcutIds);
        }
    }

    public void clearIndex(String entityType) {
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        // Asks the platform what is actually published rather than trusting this instance's
        // memory. Shortcuts outlive the process, so after a restart the in-memory list is empty
        // while the launcher still shows every previously indexed item -- and a clear that
        // quietly removed nothing is worse than one that fails loudly.
        List<String> drop = publishedIds(ctx, entityType);
        synchronized (indexed) {
            for (String uid : indexed) {
                if (matchesType(uid, entityType) && !drop.contains(uid)) {
                    drop.add(uid);
                }
            }
            indexed.removeAll(drop);
        }
        if (!drop.isEmpty()) {
            removeShortcuts(ctx, drop);
        }
    }

    /// The ids this app has published that belong to `entityType`, or all of them when null.
    @TargetApi(Build.VERSION_CODES.N_MR1)
    private List<String> publishedIds(Context ctx, String entityType) {
        List<String> out = new ArrayList<String>();
        if (!areIntentsSupported()) {
            return out;
        }
        // Only the platform call is guarded. A cast inside a catch(Throwable) block reads as
        // relying on ClassCastException, which ParparVM never throws -- and the repo's gate
        // rejects the shape wherever it appears, so the iteration stays outside.
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return out;
        }
        for (ShortcutInfo info : allPublished(manager)) {
            String id = info.getId();
            if (id != null && matchesType(id, entityType) && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /// Every shortcut this app has published that the system still knows about.
    ///
    /// The dynamic set alone is not that. Indexed shortcuts are long-lived, so the system keeps
    /// a cached copy after `pushDynamicShortcut` evicts one to stay under the limit, and the
    /// user may have pinned others -- both remain visible and tappable. Reading only the dynamic
    /// list therefore left deleted content on screen after a process restart, since the
    /// in-memory list of what this instance published is gone by then too.
    @TargetApi(Build.VERSION_CODES.N_MR1)
    @SuppressWarnings("unchecked")
    private static List<ShortcutInfo> allPublished(ShortcutManager manager) {
        List<ShortcutInfo> out = new ArrayList<ShortcutInfo>();
        if (Build.VERSION.SDK_INT >= API_MATCH_FLAGS) {
            // getShortcuts(int) reports every kind in one call, but postdates the android.jar
            // this port compiles against. FLAG_MATCH_DYNAMIC | FLAG_MATCH_PINNED |
            // FLAG_MATCH_CACHED = 2 | 4 | 8; named as literals for the same reason.
            Object all = invokeReturning(manager, "getShortcuts", new Class[]{int.class},
                    new Object[]{Integer.valueOf(2 | 4 | 8)});
            if (all instanceof List) {
                for (Object o : (List<Object>) all) {
                    if (o instanceof ShortcutInfo) {
                        out.add((ShortcutInfo) o);
                    }
                }
                return out;
            }
        }
        // Older platforms: the two lists that exist there. Cached shortcuts are a concept from
        // the same release as getShortcuts(int), so nothing is missed by their absence here.
        try {
            addAll(out, manager.getDynamicShortcuts());
            addAll(out, manager.getPinnedShortcuts());
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the published shortcuts", t);
        }
        return out;
    }

    private static void addAll(List<ShortcutInfo> out, List<ShortcutInfo> in) {
        if (in != null) {
            out.addAll(in);
        }
    }

    /// How many shortcuts this framework may still publish, whether indexing or donating.
    ///
    /// Asked of the platform rather than assumed. getMaxShortcutCountPerActivity() is the
    /// *combined* static and dynamic quota, and the manifest shortcuts the build wrote are
    /// already spending part of it -- so a fixed ten was simply wrong on a device that allows
    /// five, and the surplus pushes were rejected one at a time with nothing said. The build
    /// reserves room by emitting fewer static shortcuts than the smallest quota; this is the
    /// other half, spending only what is actually left.
    @TargetApi(Build.VERSION_CODES.N_MR1)
    private static int publishableSlots(Context ctx) {
        // The cast stays outside the guard, as it does in publishedIds: a cast inside a
        // catch(Throwable) block reads as relying on ClassCastException, which ParparVM does
        // not throw -- and the repo's gate rejects the shape wherever it appears.
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return MAX_SHORTCUTS;
        }
        int quota;
        List<ShortcutInfo> manifest;
        List<ShortcutInfo> dynamic;
        try {
            quota = manager.getMaxShortcutCountPerActivity();
            manifest = manager.getManifestShortcuts();
            dynamic = manager.getDynamicShortcuts();
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the shortcut quota", t);
            return MAX_SHORTCUTS;
        }
        // Dynamic slots the application is using for its own shortcuts count against the quota
        // just as manifest ones do, and they are not ours to take: pushDynamicShortcut evicts
        // the least recently used dynamic shortcut when the cap is reached, so indexing enough
        // entities would have quietly removed a launcher action published by native code or
        // another library. Only the slots this framework already owns are reusable, because
        // replacing one of those is what indexing is supposed to do.
        int taken = manifest == null ? 0 : manifest.size();
        if (dynamic != null) {
            for (ShortcutInfo info : dynamic) {
                if (!isOurShortcut(info.getId())) {
                    taken++;
                }
            }
        }
        // Slots this framework already holds are not in `taken`, so they are counted as
        // available here -- publishing over one of those is what indexing does. What is left
        // after that is genuinely nothing, and forcing a budget of one anyway would have
        // pushShortcut evict the least recently used dynamic shortcut, which at that point can
        // only be one belonging to the application. Publishing nothing is the correct answer:
        // indexing content must not cost a launcher action this framework never published.
        int left = quota - taken;
        if (left < 0) {
            left = 0;
        }
        return left < MAX_SHORTCUTS ? left : MAX_SHORTCUTS;
    }

    /// Whether this shortcut id is one this framework published.
    ///
    /// Both stamps, because both spend the same quota: an indexed entity and a donation.
    private static boolean isOurShortcut(String id) {
        return id != null && (id.startsWith(INDEXED_PREFIX) || id.startsWith(DONATED_PREFIX));
    }

    /// Whether a donation payload names any value at all.
    private static boolean carriesValues(String paramsJson) {
        if (paramsJson == null) {
            return false;
        }
        String trimmed = paramsJson.trim();
        return trimmed.length() > 0 && !"{}".equals(trimmed);
    }

    /// Whether the build published a manifest shortcut for this intent.
    ///
    /// Asked of the platform rather than inferred from the declaration: the build drops a
    /// shortcut for more reasons than the declaration shows -- not discoverable, past the
    /// static quota, destructive -- and this only needs to know whether the launcher has one.
    @TargetApi(Build.VERSION_CODES.N_MR1)
    private static boolean hasManifestShortcut(Context ctx, String intentId) {
        // Cast outside the guard: inside a catch(Throwable) it reads as relying on
        // ClassCastException, which ParparVM does not throw.
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return false;
        }
        // Only the platform call is guarded, and the iteration stays outside it: a for-each
        // over List<ShortcutInfo> compiles to a CHECKCAST, and a handler around one reads as
        // relying on ClassCastException. allPublished is written this way for the same reason.
        List<ShortcutInfo> manifest;
        try {
            manifest = manager.getManifestShortcuts();
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the manifest shortcuts", t);
            return false;
        }
        if (manifest == null) {
            return false;
        }
        for (ShortcutInfo info : manifest) {
            if (intentId.equals(info.getId())) {
                return true;
            }
        }
        return false;
    }

    /// Reports that a shortcut was used, without publishing anything.
    ///
    /// The ranking signal is the part of a donation that applies to a shortcut the build
    /// already published, and reportShortcutUsed accepts a manifest shortcut's id.
    @TargetApi(Build.VERSION_CODES.N_MR1)
    private static void reportUsage(Context ctx, String shortcutId) {
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return;
        }
        try {
            manager.reportShortcutUsed(shortcutId);
        } catch (Throwable t) {
            Log.w(TAG, "Could not report usage for " + shortcutId, t);
        }
    }

    /// The launcher-visible id an indexed entity is published under.
    ///
    /// The entity uid alone was used, and "does it contain a colon" was taken to mean "this
    /// framework published it". The shortcut namespace is the whole application's, though, not
    /// this framework's: an app that also publishes shortcuts from native code or another
    /// library, under an id as ordinary as `chat:compose`, had it removed and disabled by a
    /// clearIndex() that promises to clear only what was indexed through Intents. Ownership has
    /// to be something this framework stamps, not a character anyone might use.
    ///
    /// The uid keeps travelling unprefixed in the tap URI, because that is the content
    /// reference the framework resolves, not an Android identifier.
    private static String shortcutIdFor(String uid) {
        return INDEXED_PREFIX + uid;
    }

    /// Whether a published shortcut is an entity this framework indexed, and of this type.
    private static boolean matchesType(String shortcutId, String entityType) {
        if (!shortcutId.startsWith(INDEXED_PREFIX)) {
            return false;
        }
        if (entityType == null) {
            return true;
        }
        return shortcutId.startsWith(INDEXED_PREFIX + entityType + ":");
    }

    public void completeInvocation(String token, String resultJson, Map<String, byte[]> images) {
        // Nothing is waiting on a token here. Android has no continuation to resume: an
        // invocation either ran in the foreground, where the app itself shows the outcome, or in
        // CN1IntentService, which surfaces the spoken line itself.
    }

    // ------------------------------------------------------------------
    // Shortcut plumbing
    // ------------------------------------------------------------------

    @TargetApi(Build.VERSION_CODES.N_MR1)
    /// Publishes a shortcut, and reports usage only when the app is telling the system the
    /// user just did this.
    ///
    /// `reportShortcutUsed` is a ranking signal meaning "invoked": calling it while indexing
    /// content told the launcher that every published entity had been used, which competes with
    /// genuinely used shortcuts for placement and can evict them. Indexing publishes content;
    /// donation reports an action.
    private void pushShortcut(Context ctx, String id, String shortLabel, String longLabel,
                               Uri data, byte[] png, boolean report) {
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return;
        }
        // Re-publishing does not undo a disable. removeFromIndex disables the id so a cached
        // long-lived or pinned copy stops being surfaced, and Android keeps it disabled until
        // something says otherwise -- so an entity that is removed and later indexed again
        // would come back inert, or be refused outright. Harmless when the id was never
        // disabled, which is the ordinary case.
        try {
            manager.enableShortcuts(Arrays.asList(id));
        } catch (Throwable t) {
            // An id the system has never seen is not an error worth reporting.
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, data);
        intent.setClass(ctx, CN1IntentTrampolineActivity.class);

        ShortcutInfo.Builder b = new ShortcutInfo.Builder(ctx, id)
                .setShortLabel(safeLabel(shortLabel, id))
                .setLongLabel(safeLabel(longLabel, shortLabel))
                .setIntent(intent);
        if (Build.VERSION.SDK_INT >= API_LONG_LIVED) {
            // Long-lived is what lets the system keep suggesting a shortcut after the app drops
            // it from the dynamic set, which is the whole point of donating. Reflective because
            // the compile-time android.jar predates the method; a device that has it uses it,
            // and one that does not simply gets an ordinary dynamic shortcut.
            invokeQuietly(b, "setLongLived", new Class[]{boolean.class},
                    new Object[]{Boolean.TRUE});
        }
        try {
            // The entity's own thumbnail when it has one, so a list of indexed content is
            // distinguishable rather than a column of identical app icons.
            Icon icon = null;
            if (png != null && png.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(png, 0, png.length);
                if (bitmap != null) {
                    icon = Icon.createWithBitmap(bitmap);
                }
            }
            if (icon == null) {
                icon = Icon.createWithResource(ctx, ctx.getApplicationInfo().icon);
            }
            b.setIcon(icon);
        } catch (Throwable ignored) {
            // An icon is optional; a shortcut without one is still usable.
        }
        ShortcutInfo info = b.build();
        // pushDynamicShortcut makes room by evicting the least-used shortcut instead of failing
        // once the app is at the platform cap, which is what a donation wants. Same story as
        // above: reflective, with the older API as the fallback rather than an error.
        if (Build.VERSION.SDK_INT < API_PUSH_DYNAMIC
                || !invokeQuietly(manager, "pushDynamicShortcut",
                        new Class[]{ShortcutInfo.class}, new Object[]{info})) {
            try {
                manager.addDynamicShortcuts(Arrays.asList(info));
            } catch (IllegalArgumentException e) {
                // Thrown once the app is at the platform's shortcut cap. Losing a suggestion is
                // not worth failing the caller's action over.
                Log.i(TAG, "At the shortcut limit; not publishing " + id);
            }
        }
        if (report) {
            try {
                manager.reportShortcutUsed(id);
            } catch (Throwable ignored) {
                // Usage reporting is a hint to the launcher, never load bearing.
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void removeShortcuts(Context ctx, List<String> ids) {
        ShortcutManager manager = (ShortcutManager) ctx.getSystemService(ShortcutManager.class);
        if (manager == null) {
            return;
        }
        try {
            manager.removeDynamicShortcuts(ids);
            // Removing a long-lived shortcut is not enough on its own: the system keeps a cached
            // copy it can still surface, so it also has to be disabled or the user goes on
            // seeing content the app has said is gone.
            manager.disableShortcuts(ids);
        } catch (Throwable t) {
            Log.w(TAG, "Could not remove shortcuts", t);
        }
    }

    /// Calls a method that may not exist on this platform version and returns its result, or
    /// null when it was unavailable.
    private static Object invokeReturning(Object target, String method, Class[] signature,
                                           Object[] args) {
        try {
            return target.getClass().getMethod(method, signature).invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    /// Calls a method that may not exist on this device's platform version. Returns false when
    /// it was unavailable, so the caller can fall back rather than treating absence as failure.
    private static boolean invokeQuietly(Object target, String method, Class[] signature,
                                          Object[] args) {
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(method, signature);
            m.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String safeLabel(String preferred, String fallback) {
        String s = preferred != null && preferred.length() > 0 ? preferred : fallback;
        if (s == null) {
            return "";
        }
        // The launcher truncates hard; trimming here keeps the visible text predictable.
        if (s.length() <= MAX_LABEL_CHARS) {
            return s;
        }
        // Cut on a character rather than on a code unit. A supplementary character -- an emoji
        // in a title -- is a surrogate pair, and a cut that lands between the halves leaves a
        // lone surrogate: the launcher shows a replacement glyph, or refuses the label. One
        // code unit earlier is a whole character, and one character shorter than a limit is not
        // something anyone notices.
        int end = MAX_LABEL_CHARS;
        if (Character.isHighSurrogate(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    /// Builds the shortcut URI. The headless flag rides along so the trampoline can route a
    /// cold-start tap without the declaration table, which does not exist yet at that point.
    /// The bytes for a name the serializer embedded, from this request's own map.
    private static byte[] imageFor(Map<String, byte[]> images, String name) {
        if (name == null || images == null) {
            return null;
        }
        return images.get(name);
    }

    private static Uri uriFor(String intentId, String paramsJson, Context ctx) {
        String uri = SCHEME + "://run?id=" + Uri.encode(intentId);
        if (paramsJson != null && paramsJson.length() > 0) {
            uri += "&p=" + Uri.encode(paramsJson);
        }
        com.codename1.intents.IntentDeclaration decl =
                com.codename1.intents.Intents.getDeclaration(intentId);
        // runsHeadless: this URI outlives the process, so a routed intent claiming headless
        // here is what a cold tap later believes before any declaration is loaded. The static
        // shortcut generator writes the flag by the same rule.
        if (decl != null && decl.runsHeadless()) {
            uri += "&h=1";
        }
        // Marks the URI as one this application published, which is what lets the trampoline
        // run it without the restrictions an unauthenticated caller is held to.
        String nonce = CN1IntentNonce.get(ctx);
        if (nonce != null) {
            uri += "&n=" + Uri.encode(nonce);
        }
        return Uri.parse(uri);
    }

    private static Context context() {
        Context ctx = AndroidImplementation.getContext();
        return ctx == null ? null : ctx.getApplicationContext();
    }
}
