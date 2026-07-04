/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codenameone.inputvalidation.gestures;

import com.codename1.io.FileSystemStorage;
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.io.OutputStream;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

/// Drives a fixed sequence of input-event tests on a single Form. Each step waits
/// for its expected gesture (tap, drag, long-press) and logs structured CN1IV:*
/// markers that the platform driver script asserts against. The state machine
/// auto-advances on either success or timeout so a broken gesture fails fast
/// without blocking the rest of the suite.
public final class GestureSuite {
    private static final long DEFAULT_STEP_TIMEOUT_MS = 8000L;
    private static final long SUITE_EXIT_DELAY_MS = 1500L;

    private final GestureStep[] steps;
    private final Form form;
    private final Label statusLabel;
    private final Container targetArea;
    private int index = -1;
    private UITimer activeTimeout;

    public GestureSuite() {
        this.steps = new GestureStep[] {
                new TapStep(),
                new DragStep(),
                new LongPressStep()
        };
        this.form = new Form("Input Validation", new BorderLayout());
        this.statusLabel = new Label("Initializing");
        this.statusLabel.setName("cn1iv-status");
        Container top = new Container(BoxLayout.y());
        top.add(this.statusLabel);
        this.targetArea = new Container(new BorderLayout());
        this.targetArea.setName("cn1iv-target");
        this.form.add(BorderLayout.NORTH, top);
        this.form.add(BorderLayout.CENTER, this.targetArea);
    }

    public void start() {
        log("CN1IV:SUITE:STARTED platform=" + CN.getPlatformName()
                + " w=" + Display.getInstance().getDisplayWidth()
                + " h=" + Display.getInstance().getDisplayHeight());
        this.form.show();
        CN.callSerially(this::advance);
    }

    private void advance() {
        cancelTimeout();
        this.index++;
        if (this.index >= this.steps.length) {
            finishSuite();
            return;
        }
        final GestureStep step = this.steps[this.index];
        this.statusLabel.setText("Step " + (this.index + 1) + "/" + this.steps.length
                + ": " + step.name());
        this.targetArea.removeAll();
        step.install(this.targetArea, new GestureStep.Callback() {
            @Override
            public void onDetected(String details) {
                log("CN1IV:EVENT:" + step.name() + (details == null ? "" : ":" + details));
                CN.callSerially(GestureSuite.this::advance);
            }
        });
        this.targetArea.revalidate();
        log("CN1IV:READY:" + step.name());
        this.activeTimeout = UITimer.timer((int) DEFAULT_STEP_TIMEOUT_MS, false, this.form, () -> {
            log("CN1IV:TIMEOUT:" + step.name());
            advance();
        });
    }

    private void cancelTimeout() {
        if (this.activeTimeout != null) {
            this.activeTimeout.cancel();
            this.activeTimeout = null;
        }
    }

    private void finishSuite() {
        log("CN1IV:SUITE:FINISHED");
        UITimer.timer((int) SUITE_EXIT_DELAY_MS, false, this.form, () -> {
            try {
                Display.getInstance().exitApplication();
            } catch (Throwable ignored) {
            }
        });
    }

    private static final StringBuilder EVENT_LOG = new StringBuilder();

    /// Console output routes through NSLog -> unified logging on iOS, which
    /// DROPS messages under burst pressure (CI observed interleaved event
    /// lines missing from both `log stream` and `log show`). The full event
    /// transcript is therefore also rewritten to a file in the app home on
    /// every line; the platform driver reads it from the app container after
    /// the run. The console line stays as a live-progress channel.
    private static void log(String line) {
        System.out.println(line);
        EVENT_LOG.append(line).append('\n');
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            OutputStream os = fs.openOutputStream(fs.getAppHomePath() + "cn1iv-events.log");
            os.write(EVENT_LOG.toString().getBytes("UTF-8"));
            os.close();
        } catch (Exception err) {
            // the console channel remains as the fallback
        }
    }
}
