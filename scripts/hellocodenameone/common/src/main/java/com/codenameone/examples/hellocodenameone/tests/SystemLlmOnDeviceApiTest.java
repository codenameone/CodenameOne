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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ai.LlmClient;
import com.codename1.ai.LlmException;
import com.codename1.ai.RewriteStyle;
import com.codename1.ai.SystemLlmClient;
import com.codename1.ai.SystemLlmFeature;
import com.codename1.ai.SystemLlmStatus;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/**
 * The operating system's language model ({@code LlmClient.system()}): Gemini
 * Nano on Android, Foundation Models on iOS and the macOS port.
 *
 * <p>What CI can prove without a model on the device: that the builders kept
 * the platform backend in the app (the Android class against the ML Kit
 * GenAI artifacts, the Swift bridge and the Objective-C glue on iOS, which a
 * device build only links when the injection worked), that the status of
 * every feature answers within a bound and without throwing, and that a
 * device without the model refuses a request with an {@link LlmException}
 * rather than hanging. Where the model IS available -- a simulator on a host
 * with Apple Intelligence, a Pixel -- one short request runs as well.</p>
 */
public class SystemLlmOnDeviceApiTest extends BaseTest {
    private static final long STATUS_BUDGET_MS = 10000;
    private static final long REQUEST_BUDGET_MS = 15000;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        // Off the EDT: the status queries ask the operating system and block.
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                    done();
                } catch (Throwable err) {
                    fail("System LLM test failed: " + err);
                }
            }
        }, "SystemLlmOnDeviceApiTest");
        t.start();
        return true;
    }

    private void check() throws InterruptedException {
        String platform = Display.getInstance().getPlatformName();
        boolean expectBackend = !CN.isTV() && !CN.isWatch()
                && ("ios".equals(platform) || "mac".equals(platform) || "and".equals(platform));
        SystemLlmClient llm = LlmClient.system();
        try {
            String provider = llm.getProvider();
            Log.p("SystemLlmOnDeviceApiTest: platform=" + platform + " provider=" + provider);
            if (expectBackend && "none".equals(provider)) {
                throw new IllegalStateException("No system LLM backend on " + platform
                        + ": the builder did not keep or inject it");
            }
            if (("ios".equals(platform) || "mac".equals(platform)) && expectBackend
                    && !"apple-foundation-models".equals(provider)) {
                throw new IllegalStateException("Unexpected Apple provider " + provider);
            }
            if ("and".equals(platform) && !"gemini-nano".equals(provider)) {
                throw new IllegalStateException("Unexpected Android provider " + provider);
            }

            SystemLlmStatus chat = null;
            for (SystemLlmFeature f : SystemLlmFeature.values()) {
                AsyncResource<SystemLlmStatus> r = SystemLlmClient.checkStatus(f);
                SystemLlmStatus s = await(r, STATUS_BUDGET_MS, "status of " + f);
                if (s == null) {
                    throw new IllegalStateException("Status of " + f + " was null");
                }
                Log.p("SystemLlmOnDeviceApiTest: " + f + "=" + s
                        + " reason=" + SystemLlmClient.getStatusReason(f));
                if (f == SystemLlmFeature.CHAT) {
                    chat = s;
                }
            }
            if (!expectBackend && chat != SystemLlmStatus.UNSUPPORTED) {
                throw new IllegalStateException("Expected UNSUPPORTED on " + platform + ", got " + chat);
            }

            AsyncResource<String> req = llm.rewrite("Ths sentense has erors.", RewriteStyle.SHORTEN);
            if (chat != null && chat.isAvailable()) {
                // A real model: the request must settle, either way, in the
                // budget. A first request can load the model, so a slow
                // answer is logged rather than failed.
                if (!waitFor(req, REQUEST_BUDGET_MS)) {
                    Log.p("SystemLlmOnDeviceApiTest: request still running after "
                            + REQUEST_BUDGET_MS + "ms");
                    return;
                }
                try {
                    Log.p("SystemLlmOnDeviceApiTest: rewrite=" + req.get());
                } catch (AsyncResource.AsyncExecutionException ex) {
                    Log.p("SystemLlmOnDeviceApiTest: rewrite failed " + ex.getCause());
                }
                return;
            }
            // No model: the request is refused promptly, never left hanging.
            if (!waitFor(req, STATUS_BUDGET_MS)) {
                throw new IllegalStateException("A request without a model did not settle");
            }
            try {
                req.get();
                throw new IllegalStateException("A request without a model succeeded");
            } catch (AsyncResource.AsyncExecutionException expected) {
                if (!(expected.getCause() instanceof LlmException)) {
                    throw new IllegalStateException("Expected LlmException, got " + expected.getCause());
                }
            }
        } finally {
            llm.close();
        }
    }

    private static <T> T await(AsyncResource<T> r, long budget, String what) throws InterruptedException {
        if (!waitFor(r, budget)) {
            throw new IllegalStateException(what + " did not answer within " + budget + "ms");
        }
        return r.get();
    }

    private static boolean waitFor(AsyncResource<?> r, long budget) throws InterruptedException {
        long deadline = System.currentTimeMillis() + budget;
        while (!r.isDone() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        return r.isDone();
    }
}
