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

    private Container adContent(Form form) {
        return form.getContentPane();
    }

    private void drainEdt() {
        CountDownLatch drained = new CountDownLatch(1);
        CN.callSerially(() -> CN.callSerially(drained::countDown));
        waitFor(drained, 1000);
    }

    private void assertNoAd(Form form) {
        assertSame(form, CN.getCurrentForm());
    }

    private AdListener listener() {
        return new AdListener() {
            @Override public void onShown() { events.add("shown"); }
            @Override public void onImpression() { events.add("impression"); }
            @Override public void onDismissed() { events.add("dismissed"); }
        };
    }

    @FormTest
    void interstitialFormStaysVisibleUntilClosed() {
        Form previous = CN.getCurrentForm();
        Command previousBack = new Command("Application back");
        previous.setBackCommand(previousBack);
        MockAdProvider.install();
        InterstitialAd ad = new InterstitialAd("mock");
        ad.setAdListener(listener());
        ad.load();
        ad.show();
        Form showing = CN.getCurrentForm();
        assertNotSame(previous, showing);
        assertFalse(showing instanceof Dialog);
        assertEquals(implementation.getDisplayWidth(), showing.getWidth());
        assertEquals(implementation.getDisplayHeight(), showing.getHeight());
        assertEquals("Mock advertisement", ((Label) adContent(showing).getComponentAt(0)).getText());
        assertEquals(Arrays.asList("shown", "impression"), events);
        Command closeCommand = showing.getBackCommand();
        Button close = (Button) adContent(showing).getComponentAt(1);
        close.pressed();
        close.released();
        drainEdt();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression", "dismissed"), events);
        closeCommand.actionPerformed(new ActionEvent(showing));
        drainEdt();
        assertNoAd(previous);
        assertSame(previousBack, previous.getBackCommand());
        assertEquals(3, events.size(), "Closing twice must not repeat callbacks");
        ad.dispose();
        drainEdt();
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
        drainEdt();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression", "reward", "dismissed"), events);
        ad.dispose();
        drainEdt();
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
        drainEdt();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression"), events);
    }

    @FormTest
    void adRestoresModalCallerBeforeDismissalCallback() {
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
                drainEdt();
                destination.show();
            }
        });
        ad.setOnUserEarnedRewardListener(reward -> events.add("reward"));
        boolean[] started = {false};
        dialog.addShowListener(evt -> {
            if (started[0]) {
                return;
            }
            started[0] = true;
            CN.callSerially(() -> {
                try {
                    ad.load();
                    ad.show();
                    CN.getCurrentForm().getBackCommand().actionPerformed(new ActionEvent(dialog));
                    drainEdt();
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    dialog.dispose();
                    drainEdt();
                }
            });
        });
        try {
            dialog.show();
            assertNull(failure.get());
            assertEquals(Arrays.asList("reward", "dismissed"), events);
            assertSame(destination, CN.getCurrentForm());
        } finally {
            ad.dispose();
            drainEdt();
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
            assertEquals("Mock advertisement", ((Label) adContent(showing).getComponentAt(0)).getText());
            if (action == 0) {
                Button close = (Button) adContent(showing).getComponentAt(1);
                close.pressed();
                close.released();
                drainEdt();
            } else if (action == 1) {
                showing.getBackCommand().actionPerformed(new ActionEvent(showing));
                drainEdt();
            } else {
                ad.dispose();
                drainEdt();
            }
            assertNotNull(CN.getCurrentForm());
            assertNotSame(showing, CN.getCurrentForm());
            assertEquals(action == 2 ? Arrays.asList("shown", "impression")
                    : Arrays.asList("shown", "impression", "dismissed"), events);
            ad.dispose();
            drainEdt();
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
            drainEdt();
            assertSame(caller, CN.getCurrentForm());
            caller.dispose();
            drainEdt();
            boolean[] disposedDuringShow = {false};
            caller.addShowListener(evt -> CN.callSerially(() -> {
                disposedDuringShow[0] = true;
                caller.dispose();
                drainEdt();
            }));
            caller.show();
            assertFalse(disposedDuringShow[0], "A reused modeless caller must not wait for disposal");
        } finally {
            ad.dispose();
            drainEdt();
            caller.dispose();
            drainEdt();
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
        drainEdt();
        assertSame(destination, CN.getCurrentForm());
    }

    @FormTest
    void disposedAdReturnsToCallerAfterNestedDialogsClose() {
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
                drainEdt();
                ad.dispose();
                drainEdt();
                assertSame(overlays[depth - 1], CN.getCurrentForm(),
                        "Disposing an underlying ad must leave the top dialog visible");
                assertEquals(Arrays.asList("shown", "impression"), events);
                for (int i = depth - 1; i >= 0; i--) {
                    overlays[i].dispose();
                    drainEdt();
                    assertSame(i == 0 ? application : overlays[i - 1], CN.getCurrentForm(),
                            "Dialog disposal must restore its original caller");
                }
            } finally {
                for (int i = depth - 1; i >= 0; i--) {
                    if (overlays[i] != null) {
                        overlays[i].dispose();
                        drainEdt();
                    }
                }
                ad.dispose();
                drainEdt();
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
            drainEdt();
            assertSame(unrelated, CN.getCurrentForm());
            unrelated.dispose();
            drainEdt();
            assertSame(destination, CN.getCurrentForm());
        } finally {
            ad.dispose();
            drainEdt();
            unrelated.dispose();
            drainEdt();
        }
    }

    @FormTest
    void workerShowThenDisposeRestoresCaller() throws Exception {
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
            drainEdt();
            assertSame(previous, CN.getCurrentForm(), "Queued disposal must restore the original form");
            assertNoAd(previous);
            assertEquals(Arrays.asList("shown", "impression"), events);
        } finally {
            ad.dispose();
            drainEdt();
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
                assertEquals(2, adContent(CN.getCurrentForm()).getComponentCount());
                ad.dispose();
            }
            @Override public void onImpression() { events.add("impression"); }
        });
        ad.load();
        ad.show();
        drainEdt();
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
            drainEdt();
            assertSame(previous, CN.getCurrentForm());
            assertNoAd(previous);
            assertTrue(events.isEmpty(), "A disposed session must ignore pending presentation");
        } finally {
            ad.dispose();
            drainEdt();
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
            Form showing = CN.getCurrentForm();
            Command firstClose = showing.getBackCommand();
            second.show();
            assertEquals(Arrays.asList("rejected"), events);
            assertTrue(second.isLoaded(), "Rejected presentation must not consume the loaded ad");
            assertSame(firstClose, showing.getBackCommand());
            assertSame(showing, CN.getCurrentForm());
            first.dispose();
            drainEdt();
            assertSame(applicationBack, host.getBackCommand());
            second.show();
            assertEquals(Arrays.asList("rejected", "shown"), events);
            CN.getCurrentForm().getBackCommand().actionPerformed(new ActionEvent(host));
            drainEdt();
            assertEquals(Arrays.asList("rejected", "shown", "dismissed"), events);
            assertSame(applicationBack, host.getBackCommand());
            assertNoAd(host);
        } finally {
            second.dispose();
            drainEdt();
            first.dispose();
            drainEdt();
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
            Form showing = CN.getCurrentForm();
            Command firstClose = showing.getBackCommand();
            Container firstContent = adContent(CN.getCurrentForm());
            second.show();
            second.dispose();
            drainEdt();
            assertSame(firstClose, showing.getBackCommand());
            assertSame(firstContent, adContent(CN.getCurrentForm()));
            firstClose.actionPerformed(new ActionEvent(host));
            drainEdt();
            assertSame(applicationBack, host.getBackCommand());
            assertNoAd(host);
        } finally {
            second.dispose();
            drainEdt();
            first.dispose();
            drainEdt();
        }
    }

    @FormTest
    void adFormKeepsTypingAndFocusTraversalAwayFromHostControls() {
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
            Form showing = CN.getCurrentForm();
            Component close = adContent(showing).getComponentAt(1);
            assertSame(close, showing.getFocused());
            showing.keyPressed('a');
            showing.keyReleased('a');
            for (int key : new int[]{Display.GAME_UP, Display.GAME_DOWN, Display.GAME_LEFT, Display.GAME_RIGHT}) {
                showing.keyPressed(key);
                showing.keyReleased(key);
                assertSame(close, showing.getFocused());
            }
            assertNull(showing.getNextComponent(close), "Tab must not reach a covered control");
            assertNull(showing.getPreviousComponent(close), "Shift-Tab must not reach a covered control");
            assertEquals(0, typed[0]);
            ad.dispose();
            drainEdt();
            assertSame(input, host.getFocused());
            assertTrue(input.isFocusable());
            assertTrue(underlying.isFocusable());
            assertFalse(label.isFocusable());
        } finally {
            ad.dispose();
            drainEdt();
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
            Form showing = CN.getCurrentForm();
            showing.keyPressed(Display.GAME_FIRE);
            showing.keyReleased(Display.GAME_FIRE);
            drainEdt();
            assertEquals(Arrays.asList("shown", "impression", "dismissed"), events);
            assertNoAd(host);
            assertSame(underlying, host.getFocused());
            assertSame(defaultCommand, host.getDefaultCommand());
        } finally {
            ad.dispose();
            drainEdt();
        }
    }

    @FormTest
    void fullScreenAdBlocksHostKeyAndGameKeyListeners() {
        MockAdProvider.install();
        Form host = CN.getCurrentForm();
        host.addKeyListener('x', evt -> events.add("shortcut"));
        host.addKeyListener(Display.GAME_FIRE, evt -> events.add("enter"));
        host.addGameKeyListener(Display.GAME_FIRE, evt -> events.add("game-fire"));
        InterstitialAd ad = new InterstitialAd("mock");
        try {
            ad.load();
            ad.show();
            Form showing = CN.getCurrentForm();
            showing.keyPressed('x');
            showing.keyReleased('x');
            showing.keyPressed(Display.GAME_FIRE);
            showing.keyReleased(Display.GAME_FIRE);
            drainEdt();
            assertTrue(events.isEmpty(), "Ad input must not invoke covered form shortcuts: " + events);
            assertSame(host, CN.getCurrentForm());
            host.keyReleased('x');
            host.keyReleased(Display.GAME_FIRE);
            assertEquals(Arrays.asList("shortcut", "enter", "game-fire"), events);
        } finally {
            ad.dispose();
            drainEdt();
        }
    }

}
