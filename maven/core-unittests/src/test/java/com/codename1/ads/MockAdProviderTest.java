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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextField;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;

class MockAdProviderTest extends UITestBase {
    private final List<String> events = new ArrayList<String>();

    @org.junit.jupiter.api.BeforeEach
    void resetEvents() { events.clear(); }

    private Container adLayer(Form form) {
        return form.getFormLayeredPane(MockAdProvider.class, true);
    }

    private Container adOverlay(Form form) {
        return (Container) adLayer(form).getComponentAt(0);
    }

    private void assertNoAd(Form form) {
        assertEquals(0, adLayer(form).getComponentCount());
    }

    private AdListener listener() {
        return new AdListener() {
            @Override public void onShown() { events.add("shown"); }
            @Override public void onImpression() { events.add("impression"); }
            @Override public void onDismissed() { events.add("dismissed"); }
        };
    }

    @FormTest
    void interstitialOverlayStaysVisibleUntilClosed() {
        Form previous = CN.getCurrentForm();
        Command previousBack = new Command("Application back");
        previous.setBackCommand(previousBack);
        MockAdProvider.install();
        InterstitialAd ad = new InterstitialAd("mock");
        ad.setAdListener(listener());
        ad.load();
        ad.show();
        Form showing = CN.getCurrentForm();
        assertSame(previous, showing);
        assertEquals(showing.getWidth(), adOverlay(showing).getWidth());
        assertEquals(showing.getHeight(), adOverlay(showing).getHeight());
        assertEquals("Mock advertisement", ((Label) adOverlay(showing).getComponentAt(0)).getText());
        assertEquals(Arrays.asList("shown", "impression"), events);
        Command closeCommand = showing.getBackCommand();
        Button close = (Button) adOverlay(showing).getComponentAt(1);
        close.pressed();
        close.released();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression", "dismissed"), events);
        closeCommand.actionPerformed(new ActionEvent(showing));
        assertNoAd(showing);
        assertSame(previousBack, showing.getBackCommand());
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
    void overlayLeavesModalCallerAvailableToDismissalCallback() {
        MockAdProvider.install();
        Dialog dialog = new Dialog("Modal caller");
        Form destination = new Form("After dismissal");
        RewardedAd ad = new RewardedAd("mock");
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        ad.setAdListener(new AdListener() {
            @Override public void onDismissed() {
                events.add("dismissed");
                assertSame(dialog, CN.getCurrentForm());
                assertNoAd(dialog);
                dialog.dispose();
                destination.show();
            }
        });
        ad.setOnUserEarnedRewardListener(reward -> events.add("reward"));
        dialog.addShowListener(evt -> CN.callSerially(() -> {
            try {
                ad.load();
                ad.show();
                dialog.getBackCommand().actionPerformed(new ActionEvent(dialog));
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                dialog.dispose();
            }
        }));
        try {
            dialog.show();
            assertNull(failure.get());
            assertEquals(Arrays.asList("reward", "dismissed"), events);
            assertSame(destination, CN.getCurrentForm());
        } finally {
            ad.dispose();
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
            assertEquals("Mock advertisement", ((Label) adOverlay(showing).getComponentAt(0)).getText());
            if (action == 0) {
                Button close = (Button) adOverlay(showing).getComponentAt(1);
                close.pressed();
                close.released();
            } else if (action == 1) {
                showing.getBackCommand().actionPerformed(new ActionEvent(showing));
            } else {
                ad.dispose();
            }
            assertNotNull(CN.getCurrentForm());
            assertSame(showing, CN.getCurrentForm());
            assertNoAd(showing);
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
    void disposalRemovesOverlayWithoutChangingNestedDialogs() {
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
                            "Dialog disposal must restore its original caller");
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
    void workerShowThenDisposeRemovesQueuedOverlay() throws Exception {
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
            assertSame(previous, CN.getCurrentForm(), "Queued presentation must not change the current form");
            assertNoAd(previous);
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
                assertSame(previous, CN.getCurrentForm());
                assertEquals(1, adLayer(previous).getComponentCount());
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
            assertNoAd(previous);
            assertTrue(events.isEmpty(), "A disposed session must ignore pending presentation");
        } finally {
            ad.dispose();
            previous.show();
        }
    }

    @FormTest
    void overlappingAdIsRejectedAndCanRetryAfterFirstIsDisposed() {
        MockAdProvider.install();
        Form host = CN.getCurrentForm();
        Command applicationBack = new Command("Application back");
        host.setBackCommand(applicationBack);
        InterstitialAd first = new InterstitialAd("first");
        AppOpenAd second = new AppOpenAd("second");
        second.setAdListener(new AdListener() {
            @Override public void onShowFailed(AdError error) { events.add("rejected"); }
            @Override public void onShown() { events.add("shown"); }
            @Override public void onDismissed() { events.add("dismissed"); }
        });
        try {
            first.load();
            second.load();
            first.show();
            Command firstClose = host.getBackCommand();
            second.show();
            assertEquals(Arrays.asList("rejected"), events);
            assertTrue(second.isLoaded(), "Rejected presentation must not consume the loaded ad");
            assertSame(firstClose, host.getBackCommand());
            assertEquals(1, adLayer(host).getComponentCount());
            first.dispose();
            assertSame(applicationBack, host.getBackCommand());
            second.show();
            assertEquals(Arrays.asList("rejected", "shown"), events);
            host.getBackCommand().actionPerformed(new ActionEvent(host));
            assertEquals(Arrays.asList("rejected", "shown", "dismissed"), events);
            assertSame(applicationBack, host.getBackCommand());
            assertNoAd(host);
        } finally {
            second.dispose();
            first.dispose();
        }
    }

    @FormTest
    void disposingRejectedAdLeavesVisibleAdAndBackCommandAlone() {
        MockAdProvider.install();
        Form host = CN.getCurrentForm();
        Command applicationBack = new Command("Application back");
        host.setBackCommand(applicationBack);
        InterstitialAd first = new InterstitialAd("first");
        InterstitialAd second = new InterstitialAd("second");
        try {
            first.load();
            second.load();
            first.show();
            Command firstClose = host.getBackCommand();
            Container firstOverlay = adOverlay(host);
            second.show();
            second.dispose();
            assertSame(firstClose, host.getBackCommand());
            assertSame(firstOverlay, adOverlay(host));
            firstClose.actionPerformed(new ActionEvent(host));
            assertSame(applicationBack, host.getBackCommand());
            assertNoAd(host);
        } finally {
            second.dispose();
            first.dispose();
        }
    }

    @FormTest
    void overlayKeepsTypingAndFocusTraversalAwayFromHostControls() {
        MockAdProvider.install();
        Form host = CN.getCurrentForm();
        int[] typed = {0};
        TextField input = new TextField() {
            @Override public void keyPressed(int key) { typed[0]++; }
        };
        Button underlying = new Button("Underlying action");
        Label label = new Label("Not focusable");
        host.addAll(input, underlying, label);
        host.revalidate();
        input.requestFocus();
        assertSame(input, host.getFocused());
        InterstitialAd ad = new InterstitialAd("mock");
        try {
            ad.load();
            ad.show();
            Component close = adOverlay(host).getComponentAt(1);
            assertSame(close, host.getFocused());
            host.keyPressed('a');
            host.keyReleased('a');
            for (int key : new int[]{Display.GAME_UP, Display.GAME_DOWN, Display.GAME_LEFT, Display.GAME_RIGHT}) {
                host.keyPressed(key);
                host.keyReleased(key);
                assertSame(close, host.getFocused());
            }
            assertNull(host.getNextComponent(close), "Tab must not reach a covered control");
            assertNull(host.getPreviousComponent(close), "Shift-Tab must not reach a covered control");
            assertEquals(0, typed[0]);
            ad.dispose();
            assertSame(input, host.getFocused());
            assertTrue(input.isFocusable());
            assertTrue(underlying.isFocusable());
            assertFalse(label.isFocusable());
        } finally {
            ad.dispose();
        }
    }

    @FormTest
    void keyboardCloseDoesNotAlsoTriggerHostDefaultCommand() {
        MockAdProvider.install();
        Form host = CN.getCurrentForm();
        Button underlying = new Button("Underlying action");
        underlying.addActionListener(evt -> events.add("underlying"));
        host.add(underlying);
        host.revalidate();
        underlying.requestFocus();
        Command defaultCommand = new Command("Default action") {
            @Override public void actionPerformed(ActionEvent evt) { events.add("default"); }
        };
        host.setDefaultCommand(defaultCommand);
        InterstitialAd ad = new InterstitialAd("mock");
        ad.setAdListener(listener());
        try {
            ad.load();
            ad.show();
            host.keyPressed(Display.GAME_FIRE);
            host.keyReleased(Display.GAME_FIRE);
            CountDownLatch drained = new CountDownLatch(1);
            CN.callSerially(drained::countDown);
            waitFor(drained, 1000);
            assertEquals(Arrays.asList("shown", "impression", "dismissed"), events);
            assertNoAd(host);
            assertSame(underlying, host.getFocused());
            assertSame(defaultCommand, host.getDefaultCommand());
        } finally {
            ad.dispose();
        }
    }

}
