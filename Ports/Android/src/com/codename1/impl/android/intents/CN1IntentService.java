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

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || intent.getDataString() == null) {
            return;
        }
        Uri data = Uri.parse(intent.getDataString());
        final String id = data.getQueryParameter("id");
        if (id == null) {
            return;
        }
        if (!Display.isInitialized()) {
            shouldStopContext = true;
            AndroidImplementation.startContext(this);
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
            if (shouldStopContext) {
                AndroidImplementation.stopContext(this);
            }
        }
    }

    /// How long to wait before giving up on a completion that never came.
    ///
    /// Derived from what the intent declared rather than fixed: an intent allowed more than the
    /// default would otherwise have its runtime torn down while its handler was still running,
    /// and the caller told nothing.
    private static int backstopSeconds(String intentId) {
        com.codename1.intents.IntentDeclaration decl = Intents.getDeclaration(intentId);
        int declared = decl == null ? Intents.getDefaultTimeout() : decl.getTimeoutSeconds();
        if (declared < 1) {
            declared = DEFAULT_BUDGET_SECONDS;
        }
        return declared + BACKSTOP_MARGIN_SECONDS;
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

        boolean await(int seconds, TimeUnit unit) throws InterruptedException {
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

    private static Map<String, Object> parse(String json) {
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            return JSONParser.parseJSON(json);
        } catch (Throwable t) {
            Log.w(TAG, "Could not parse intent parameters", t);
            return null;
        }
    }
}
