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
package com.codename1.ads;

import com.codename1.ads.mock.MockAdProvider;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;

class MockAdProviderTest extends UITestBase {
    private final List<String> events = new ArrayList<String>();

    @org.junit.jupiter.api.BeforeEach
    void resetEvents() { events.clear(); }

    private AdListener listener() {
        return new AdListener() {
            @Override public void onShown() { events.add("shown"); }
            @Override public void onImpression() { events.add("impression"); }
            @Override public void onDismissed() { events.add("dismissed"); }
        };
    }

    @FormTest
    void interstitialStaysVisibleUntilClosedAndRestoresPreviousForm() {
        Form previous = CN.getCurrentForm();
        MockAdProvider.install();
        InterstitialAd ad = new InterstitialAd("mock");
        ad.setAdListener(listener());
        ad.load();
        ad.show();
        Form showing = CN.getCurrentForm();
        assertNotSame(previous, showing);
        assertEquals("Mock advertisement", showing.getTitle());
        assertEquals(Arrays.asList("shown", "impression"), events);
        Button close = (Button) showing.getContentPane().getComponentAt(1);
        close.pressed();
        close.released();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression", "dismissed"), events);
        showing.getBackCommand().actionPerformed(new ActionEvent(showing));
        assertEquals(3, events.size(), "Closing twice must not repeat callbacks");
        ad.dispose();
    }

    @FormTest
    void rewardedBackCloseDeliversRewardBeforeDismissal() {
        Form previous = CN.getCurrentForm();
        MockAdProvider.install();
        RewardedAd ad = new RewardedAd("mock");
        ad.setAdListener(listener());
        ad.setOnUserEarnedRewardListener(reward -> events.add("reward"));
        ad.load();
        ad.show();
        Form showing = CN.getCurrentForm();
        assertEquals(Arrays.asList("shown", "impression"), events);
        showing.getBackCommand().actionPerformed(new ActionEvent(showing));
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression", "reward", "dismissed"), events);
        ad.dispose();
    }

    @FormTest
    void disposingVisibleAdRestoresFormWithoutRewardOrDismissal() {
        Form previous = CN.getCurrentForm();
        MockAdProvider.install();
        RewardedAd ad = new RewardedAd("mock");
        ad.setAdListener(listener());
        ad.setOnUserEarnedRewardListener(reward -> events.add("reward"));
        ad.load();
        ad.show();
        ad.dispose();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression"), events);
    }

    @FormTest
    void modalDialogCanBeDisposedAndNavigatedAwayFromInDismissalCallback() {
        MockAdProvider.install();
        Dialog dialog = new Dialog("Modal caller");
        Form destination = new Form("After dismissal");
        RewardedAd ad = new RewardedAd("mock");
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Timer watchdog = new Timer(true);
        boolean[] started = {false};
        ad.setAdListener(new AdListener() {
            @Override public void onDismissed() {
                events.add("dismissed");
                assertSame(dialog, CN.getCurrentForm());
                dialog.dispose();
                destination.show();
            }
        });
        ad.setOnUserEarnedRewardListener(reward -> events.add("reward"));
        dialog.addShowListener(evt -> {
            if (started[0]) {
                return;
            }
            started[0] = true;
            CN.callSerially(() -> {
                try {
                    ad.load();
                    ad.show();
                    Form showing = CN.getCurrentForm();
                    // Unblock the broken implementation so failure is an assertion,
                    // not a hung EDT. The fixed close delivers callbacks immediately.
                    watchdog.schedule(new TimerTask() {
                        @Override public void run() {
                            CN.callSerially(() -> {
                                if (!events.contains("dismissed")) {
                                    events.add("watchdog");
                                    dialog.dispose();
                                }
                            });
                        }
                    }, 1000);
                    showing.getBackCommand().actionPerformed(new ActionEvent(showing));
                } catch (Throwable t) {
                    failure.set(t);
                    dialog.dispose();
                }
            });
        });
        try {
            dialog.show();
            assertNull(failure.get(), "Dismissal callback must see the restored dialog");
            assertEquals(Arrays.asList("reward", "dismissed"), events);
            assertSame(destination, CN.getCurrentForm());
            // Reusing the same caller must still enter its original modal wait.
            // Queue disposal from onShow so only the modal event loop can run it
            // before show() returns.
            boolean[] disposedDuringShow = {false};
            dialog.addShowListener(evt -> CN.callSerially(() -> {
                disposedDuringShow[0] = true;
                dialog.dispose();
            }));
            dialog.show();
            assertTrue(disposedDuringShow[0], "A reused modal caller must still block until disposed");
        } finally {
            watchdog.cancel();
            ad.dispose();
            dialog.dispose();
        }
    }

    @FormTest
    void appOpenAdWithoutPreviousFormCanCloseGoBackOrDispose() {
        MockAdProvider.install();
        for (int action = 0; action < 3; action++) {
            events.clear();
            implementation.setCurrentForm(null);
            assertNull(CN.getCurrentForm());
            AppOpenAd ad = new AppOpenAd("mock");
            ad.setAdListener(listener());
            ad.load();
            ad.show();
            Form showing = CN.getCurrentForm();
            assertEquals("Mock advertisement", showing.getTitle());
            if (action == 0) {
                Button close = (Button) showing.getContentPane().getComponentAt(1);
                close.pressed();
                close.released();
            } else if (action == 1) {
                showing.getBackCommand().actionPerformed(new ActionEvent(showing));
            } else {
                ad.dispose();
            }
            assertNotNull(CN.getCurrentForm());
            assertNotSame(showing, CN.getCurrentForm(), "Dismissal must remove the ad even at startup");
            assertEquals(action == 2 ? Arrays.asList("shown", "impression")
                    : Arrays.asList("shown", "impression", "dismissed"), events);
            ad.dispose();
        }
    }

    @FormTest
    void modelessCallerStaysModelessAfterAdDisposal() {
        MockAdProvider.install();
        Dialog caller = new Dialog("Modeless caller");
        caller.showModeless();
        InterstitialAd ad = new InterstitialAd("mock");
        try {
            ad.load();
            ad.show();
            ad.dispose();
            assertSame(caller, CN.getCurrentForm());
            caller.dispose();
            boolean[] disposedDuringShow = {false};
            caller.addShowListener(evt -> CN.callSerially(() -> {
                disposedDuringShow[0] = true;
                caller.dispose();
            }));
            caller.show();
            assertFalse(disposedDuringShow[0], "A reused modeless caller must not wait for disposal");
        } finally {
            ad.dispose();
            caller.dispose();
        }
    }

    @FormTest
    void disposalDoesNotUndoApplicationNavigation() {
        MockAdProvider.install();
        InterstitialAd ad = new InterstitialAd("mock");
        ad.load();
        ad.show();
        Form destination = new Form("Application destination");
        destination.show();
        ad.dispose();
        assertSame(destination, CN.getCurrentForm());
    }

    @FormTest
    void disposalSplicesAdOutOfNestedDialogs() {
        MockAdProvider.install();
        Form application = CN.getCurrentForm();
        for (int depth = 1; depth <= 2; depth++) {
            events.clear();
            InterstitialAd ad = new InterstitialAd("mock");
            ad.setAdListener(listener());
            Dialog[] overlays = new Dialog[depth];
            try {
                ad.load();
                ad.show();
                for (int i = 0; i < depth; i++) {
                    overlays[i] = new Dialog("Application overlay " + i);
                    overlays[i].showModeless();
                }
                ad.dispose();
                ad.dispose();
                assertSame(overlays[depth - 1], CN.getCurrentForm(),
                        "Disposing an underlying ad must leave the top dialog visible");
                assertEquals(Arrays.asList("shown", "impression"), events);
                for (int i = depth - 1; i >= 0; i--) {
                    overlays[i].dispose();
                    assertSame(i == 0 ? application : overlays[i - 1], CN.getCurrentForm(),
                            "Dialog disposal must skip the removed ad and restore its caller");
                }
            } finally {
                for (int i = depth - 1; i >= 0; i--) {
                    if (overlays[i] != null) {
                        overlays[i].dispose();
                    }
                }
                ad.dispose();
                application.show();
            }
        }
    }

    @FormTest
    void disposalDoesNotUndoNavigationToAnUnrelatedDialog() {
        MockAdProvider.install();
        InterstitialAd ad = new InterstitialAd("mock");
        Form destination = new Form("Application destination");
        Dialog unrelated = new Dialog("Unrelated dialog");
        try {
            ad.load();
            ad.show();
            destination.show();
            unrelated.showModeless();
            ad.dispose();
            assertSame(unrelated, CN.getCurrentForm());
            unrelated.dispose();
            assertSame(destination, CN.getCurrentForm());
        } finally {
            ad.dispose();
            unrelated.dispose();
        }
    }

    @FormTest
    void workerShowThenDisposeDoesNotStrandQueuedDialog() throws Exception {
        MockAdProvider.install();
        Form previous = CN.getCurrentForm();
        InterstitialAd ad = new InterstitialAd("mock");
        ad.setAdListener(listener());
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread worker = new Thread(() -> {
            try {
                ad.load();
                ad.show();
                ad.dispose();
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "mock-ad-worker");
        try {
            // Keep the EDT here until the worker has queued both operations.
            // invokeAndBlock would pump the queue and hide the failing ordering.
            worker.start();
            worker.join(1000);
            assertFalse(worker.isAlive(), "Worker operations must not wait for the EDT");
            assertNull(failure.get());
            CountDownLatch drained = new CountDownLatch(1);
            CN.callSerially(drained::countDown);
            waitFor(drained, 1000);
            assertSame(previous, CN.getCurrentForm(), "Queued presentation must not resurrect a disposed ad");
            assertEquals(Arrays.asList("shown", "impression"), events);
        } finally {
            ad.dispose();
            previous.show();
        }
    }

    @FormTest
    void onShownCanDisposeSynchronouslyWithoutAnImpression() {
        MockAdProvider.install();
        Form previous = CN.getCurrentForm();
        InterstitialAd ad = new InterstitialAd("mock");
        ad.setAdListener(new AdListener() {
            @Override public void onShown() {
                events.add("shown");
                assertTrue(CN.isEdt());
                assertNotSame(previous, CN.getCurrentForm());
                ad.dispose();
            }
            @Override public void onImpression() { events.add("impression"); }
        });
        ad.load();
        ad.show();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown"), events);
    }

    @FormTest
    void edtDisposalCancelsAlreadyQueuedWorkerPresentation() throws Exception {
        MockAdProvider.install();
        Form previous = CN.getCurrentForm();
        InterstitialAd ad = new InterstitialAd("mock");
        ad.setAdListener(listener());
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread worker = new Thread(() -> {
            try {
                ad.load();
                ad.show();
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "mock-ad-queued-presentation");
        try {
            worker.start();
            worker.join(1000);
            assertFalse(worker.isAlive());
            assertNull(failure.get());
            // An EDT caller can dispose before the queued load/show get a turn.
            ad.dispose();
            CountDownLatch drained = new CountDownLatch(1);
            CN.callSerially(drained::countDown);
            waitFor(drained, 1000);
            assertSame(previous, CN.getCurrentForm());
            assertTrue(events.isEmpty(), "A disposed session must ignore pending presentation");
        } finally {
            ad.dispose();
            previous.show();
        }
    }

}
