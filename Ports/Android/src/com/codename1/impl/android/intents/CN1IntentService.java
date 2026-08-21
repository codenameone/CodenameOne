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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.intents.IntentCompletion;
import com.codename1.intents.IntentDeclaration;
import com.codename1.intents.IntentResult;
import com.codename1.intents.IntentSource;
import com.codename1.intents.Intents;
import com.codename1.io.JSONParser;
import com.codename1.ui.Display;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/// Runs a headless intent with no Activity in sight.
///
/// This is the one place Android is genuinely easier than iOS: `AndroidImplementation.startContext`
/// boots `Display` against a Service context, and the port is already written for that mode --
/// background fetch has shipped on exactly this path for years. So a headless intent here is a
/// service that starts the runtime, runs the handler, surfaces whatever the handler wanted said,
/// and stops the runtime again.
///
/// The service is not exported. Nothing outside the application may ask it to run a capability;
/// taps arrive through the trampoline activity, which is the only exported door.
public class CN1IntentService extends IntentService {

    private static final String TAG = "CN1Intents";
    private static final String SCHEME = "cn1intentsvc";
    /// Added to the intent's own budget. The framework enforces the real deadline and answers
    /// on expiry; this is only the backstop for a completion that never arrives at all, so it
    /// has to outlast the deadline rather than pre-empt it.
    private static final int BACKSTOP_MARGIN_SECONDS = 5;
    private static final int DEFAULT_BUDGET_SECONDS = 25;

    private boolean shouldStopContext;

    public CN1IntentService() {
        super("com.codename1.impl.android.intents.CN1IntentService");
    }

    /// Starts a headless run from a context that has no runtime of its own.
    public static void run(Context ctx, String intentId, String paramsJson) {
        try {
            Intent svc = new Intent(ctx, CN1IntentService.class);
            String uri = SCHEME + "://run?id=" + Uri.encode(intentId);
            if (paramsJson != null) {
                uri += "&p=" + Uri.encode(paramsJson);
            }
            svc.setData(Uri.parse(uri));
            ctx.startService(svc);
        } catch (Throwable t) {
            Log.w(TAG, "Could not start the intent service", t);
        }
    }

    /// Runs an intent inside a process whose runtime is already up.
    public static void runInProcess(String intentId, String paramsJson) {
        Intents.dispatchInvocation(intentId, parse(paramsJson), IntentSource.SHORTCUT, false, null);
    }

    /// Boots the runtime without running anything, so a request parked at a cold start gets
    /// judged and dispatched by `AndroidIntentBridge.registerIntents`.
    ///
    /// This exists for the one case the trampoline cannot decide by itself: a build-time static
    /// shortcut declaring `headless=true`. It carries no nonce -- there was no runtime at build
    /// time to mint one -- so the id has to be parked, and parking it used to mean foregrounding
    /// the app to get a runtime, which is exactly what a headless intent promises not to do.
    public static void wake(Context ctx) {
        try {
            Intent svc = new Intent(ctx, CN1IntentService.class);
            svc.setData(Uri.parse(SCHEME + "://wake"));
            ctx.startService(svc);
        } catch (Throwable t) {
            Log.w(TAG, "Could not start the intent service", t);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || intent.getDataString() == null) {
            return;
        }
        Uri data = Uri.parse(intent.getDataString());
        final String id = data.getQueryParameter("id");
        if (id == null) {
            wakeRuntime();
            return;
        }
        // An IntentService reuses one instance for every queued start, so a flag left true by
        // an earlier request would make a later one tear down a context it never started --
        // deinitializing the Activity's runtime if the app came forward in between.
        shouldStopContext = false;
        if (!Display.isInitialized()) {
            shouldStopContext = true;
            AndroidImplementation.startContext(this);
        }
        installDispatcher();
        // The headless flag arrived in the URI because at the moment of the tap there were no
        // declarations to consult. There are now, and they are the authority: a shortcut minted
        // before an app update can carry h=1 for an intent that has since become non-headless,
        // and running that handler here gives it no Activity and no visible Form. Hand it to
        // the foreground path instead of honouring a flag the app no longer agrees with.
        IntentDeclaration decl = Intents.getDeclaration(id);
        // The trampoline applies this too, but it cannot on a cold start: the declarations are
        // not installed yet when a tap arrives, so a trusted shortcut minted before an update
        // that marked the intent destructive passed a check that had nothing to check against.
        // Here they exist. A donation is durable and the policy it was made under is not.
        if (decl != null && !CN1IntentTrampolineActivity.isStillPermittedOnOneTap(decl)) {
            Log.w(TAG, "Refusing \"" + id + "\": the shortcut predates a declaration change "
                    + "that no longer allows it to run on a single tap");
            if (shouldStopContext && AndroidImplementation.getActivity() == null) {
                AndroidImplementation.stopContext(this);
            }
            return;
        }
        if (decl != null && !decl.runsHeadless()) {
            Log.w(TAG, "Shortcut for \"" + id + "\" asked for headless, but the declaration is "
                    + "not; running it in the foreground instead");
            AndroidIntentBridge.parkForegroundRequest(id, data.getQueryParameter("p"));
            // Released *before* the launch, and the order is the whole fix. stopContext takes
            // one of two paths and both are destructive once an Activity has registered: with
            // contexts remaining it calls instance.deinitialize(), which tears down the UI
            // resources of the Activity that just started. requestForegroundStatic only posts
            // the launch, so stopping afterwards was a race with the Activity's own startContext
            // -- a fast launch lost its window, a slow one did not, and nothing in between said
            // which had happened.
            //
            // Letting go first makes this deterministic: the service is the only context here
            // (Display was not initialized, which is why it was started), so this is a clean
            // teardown of a runtime nothing is using yet. The parked request is a static field
            // and outlives it; the Activity boots its own runtime and drains the queue through
            // registerIntents. Nothing needs the runtime in between -- the launch reads only the
            // package manager.
            if (shouldStopContext) {
                AndroidImplementation.stopContext(this);
                shouldStopContext = false;
            }
            AndroidIntentBridge.requestForegroundStatic();
            return;
        }
        try {
            Latch latch = new Latch();
            Intents.dispatchInvocation(id, parse(data.getQueryParameter("p")),
                    IntentSource.SHORTCUT, true, latch);
            // The service must outlive the handler, so this waits -- but never forever. The
            // framework enforces its own per-intent deadline; this is the backstop for the case
            // where the completion itself never arrives.
            if (!latch.await(backstopSeconds(id), TimeUnit.SECONDS)) {
                Log.w(TAG, "Intent " + id + " did not complete within its budget");
            }
            report(latch.result());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            Log.e(TAG, "Intent " + id + " failed", t);
        } finally {
            // Not when an Activity has taken the runtime over. A headless handler may return a
            // route, and the framework then brings the app forward and navigates before this
            // runs -- so by here the Activity may hold the runtime this service started, and
            // stopContext would reach its non-empty branch and deinitialize the UI it just
            // built. Holding the claim instead leaves one stale entry in activeContexts, which
            // costs a full deinitialize the app would otherwise get on its last context; that
            // is the smaller harm by a wide margin, and the process is on its way out anyway.
            if (shouldStopContext && AndroidImplementation.getActivity() == null) {
                AndroidImplementation.stopContext(this);
            }
        }
    }

    /// Installs the build-time generated dispatcher, which nothing else does on this path.
    ///
    /// `AndroidImplementation.startContext` boots Display and no more. The generated
    /// `new cn1app.IntentBootstrap()` is spliced into the main Activity's resume, and a headless
    /// shortcut starting a dead process launches this service and nothing else -- so the
    /// dispatcher stayed null, every dispatch queued waiting for it, and the service timed out
    /// and tore the runtime down having run nothing.
    ///
    /// By name because a port class cannot reference a per-application generated one. The
    /// builder emits a keep rule for it when the app uses intents, so R8 leaves the name alone.
    private static void installDispatcher() {
        if (!Intents.getDeclarations().isEmpty()) {
            return;
        }
        try {
            Class.forName("cn1app.IntentBootstrap").newInstance();
        } catch (ClassNotFoundException e) {
            // An app that declares no @AppIntent has no bootstrap, which is not an error: it can
            // still index content and donate.
        } catch (Throwable t) {
            Log.w(TAG, "Could not install the intent dispatcher", t);
        }
    }

    /// Starts the runtime and holds the service open long enough for the framework to install
    /// its dispatcher and run whatever was parked.
    ///
    /// The wait is on the parked work actually leaving the queue rather than on a fixed sleep,
    /// with a bound for the case where the request is refused and nothing ever runs.
    private void wakeRuntime() {
        shouldStopContext = false;
        if (Display.isInitialized()) {
            // Already up, so registerIntents has already run; nothing was waiting on this.
            return;
        }
        shouldStopContext = true;
        try {
            AndroidImplementation.startContext(this);
            installDispatcher();
            long deadline = System.currentTimeMillis()
                    + (DEFAULT_BUDGET_SECONDS + BACKSTOP_MARGIN_SECONDS) * 1000L;
            while (System.currentTimeMillis() < deadline
                    && Intents.getDeclarations().isEmpty()) {
                Thread.sleep(50);
            }
            // registerIntents dispatches the parked request on the framework's own thread, so
            // this has to wait for that handler rather than for a fixed interval. A fixed sleep
            // meant any handler slower than it had the only runtime it has torn down from
            // underneath it, halfway through, with its declared budget ignored.
            AndroidIntentBridge.awaitParkedCompletion(BACKSTOP_MARGIN_SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            Log.e(TAG, "Could not start the runtime for a parked request", t);
        } finally {
            // Same rule as run(): what registerIntents dispatched may have brought the app
            // forward, and stopping this context with an Activity registered deinitializes the
            // UI that Activity just built.
            if (shouldStopContext && AndroidImplementation.getActivity() == null) {
                AndroidImplementation.stopContext(this);
            }
        }
    }

    /// How long to wait before giving up on a completion that never came.
    ///
    /// Derived from what the intent declared rather than fixed: an intent allowed more than the
    /// default would otherwise have its runtime torn down while its handler was still running,
    /// and the caller told nothing.
    private static long backstopSeconds(String intentId) {
        com.codename1.intents.IntentDeclaration decl = Intents.getDeclaration(intentId);
        int declared = decl == null ? Intents.getDefaultTimeout() : decl.getTimeoutSeconds();
        if (declared < 1) {
            declared = DEFAULT_BUDGET_SECONDS;
        }
        // Widened before the addition. Both operands are ints and the declared budget can be
        // anything the build accepts, so within five seconds of Integer.MAX_VALUE this went
        // negative -- await returned at once and the service tore down the runtime with the
        // handler still in it.
        return (long) declared + BACKSTOP_MARGIN_SECONDS;
    }

    /// Shows what the handler wanted said.
    ///
    /// Android has no assistant to speak it, so a spoken line becomes a toast rather than being
    /// dropped silently -- otherwise a headless intent would give the user no sign it ran at all.
    private void report(IntentResult result) {
        if (result == null) {
            return;
        }
        String message = result.isFailed() ? result.getErrorMessage() : result.getDialog();
        if (message == null || message.length() == 0) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new ShowToast(getApplicationContext(), message));
    }

    /// Blocks the service thread until the handler answers.
    ///
    /// A named class rather than an anonymous one: it captures nothing from the service, so
    /// making that explicit keeps it from holding a reference to the enclosing instance for the
    /// lifetime of the invocation.
    private static final class Latch implements IntentCompletion {
        private final CountDownLatch done = new CountDownLatch(1);
        private IntentResult result;

        public void onIntentResult(IntentResult r) {
            result = r;
            done.countDown();
        }

        boolean await(long seconds, TimeUnit unit) throws InterruptedException {
            return done.await(seconds, unit);
        }

        IntentResult result() {
            return result;
        }
    }

    /// Shows the handler's spoken line, since Android has no assistant to say it.
    private static final class ShowToast implements Runnable {
        private final Context ctx;
        private final String message;

        ShowToast(Context ctx, String message) {
            this.ctx = ctx;
            this.message = message;
        }

        public void run() {
            try {
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {
                // A toast is a courtesy; losing it must not fail the invocation.
            }
        }
    }

    /// Shared with the bridge's foreground queue, which decodes the same payload.
    static Map<String, Object> parse(String json) {
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            return com.codename1.intents.IntentSerializer.parsePayload(json);
        } catch (Throwable t) {
            Log.w(TAG, "Could not parse intent parameters", t);
            return null;
        }
    }
}
