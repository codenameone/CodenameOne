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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestWindowManager;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowTest extends UITestBase {

    @FormTest
    void unsupportedPlatformThrowsOnConstruction() {
        // the default: no window manager, which is what every mobile port reports
        assertFalse(Desktop.isSupported(),
                "Desktop windowing should be off unless a test turns it on");
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                new Window("nope");
            }
        }, "Constructing a Window without a windowing system must throw, not degrade");
    }

    @FormTest
    void unsupportedPlatformStillAnswersDesktopQueriesSafely() {
        assertEquals(0, Desktop.getInstance().getWindows().length,
                "getWindows() must be empty rather than null where there are no windows");
        assertNull(Desktop.getInstance().getFocusedWindow());
        assertEquals(1, Desktop.getInstance().getMonitors().length,
                "A platform with no windowing system still reports its single display");
        assertNotNull(Desktop.getInstance().getPrimaryMonitor());
    }

    @FormTest
    void showCreatesExactlyOneNativeWindowAndDisposeReleasesIt() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("Inspector", new BorderLayout());
        w.setWindowSize(640, 480);
        w.show();

        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertNotNull(peer, "show() should have created a native window");
        assertEquals(1, wm.getWindows().size(), "show() must not create a second window");
        assertTrue(peer.isVisible());
        assertEquals("Inspector", peer.getTitle());
        assertEquals(1, Desktop.getInstance().getWindows().length);

        w.dispose();
        assertTrue(peer.isDisposed());
        assertFalse(peer.isVisible());
        assertEquals(0, Desktop.getInstance().getWindows().length,
                "A disposed window must leave the desktop registry");

        // disposing twice is harmless
        w.dispose();
        assertEquals(1, wm.getWindows().size());
    }

    @FormTest
    void titleAndBoundsReachTheNativeWindow() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("first");
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();

        w.setTitle("second");
        assertEquals("second", peer.getTitle());

        w.setWindowBounds(new com.codename1.ui.geom.Rectangle(10, 20, 300, 200));
        assertEquals(10, peer.getX());
        assertEquals(20, peer.getY());
        assertEquals(300, peer.getWidth());
        assertEquals(200, peer.getHeight());
        w.dispose();
    }

    @FormTest
    void componentsInAWindowResolveTheWindowNotAForm() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("host", new BorderLayout());
        Label content = new Label("hello");
        w.add(BorderLayout.CENTER, content);
        w.show();

        assertSame(w, content.getTopLevelContainer(),
                "A component in a Window must resolve that Window as its top level");
        assertNull(content.getComponentForm(),
                "getComponentForm() keeps its meaning and is null inside a Window");
        assertSame(w.getContentPane(), content.getParent(),
                "add() on a Window should reach the content pane, as it does on a Form");
        w.dispose();
    }

    @FormTest
    void formStillResolvesItselfAsTopLevel() {
        Form f = new Form("main", new BorderLayout());
        Label content = new Label("hello");
        f.add(BorderLayout.CENTER, content);
        f.show();
        flushSerialCalls();

        assertSame(f, content.getTopLevelContainer());
        assertSame(f, content.getComponentForm(),
                "The Form path must be completely unaffected");
    }

    @FormTest
    void closeRequestHonoursTheCloseOperation() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("closable");
        w.setCloseOperation(Window.HIDE_ON_CLOSE);
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();

        w.closeRequested();
        assertFalse(peer.isDisposed(), "HIDE_ON_CLOSE must not destroy the window");
        assertFalse(peer.isVisible());

        w.setCloseOperation(Window.DISPOSE_ON_CLOSE);
        w.closeRequested();
        assertTrue(peer.isDisposed());
    }

    @FormTest
    void aCloseListenerCanVetoTheClose() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("vetoed");
        w.show();
        final AtomicInteger calls = new AtomicInteger();
        w.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                calls.incrementAndGet();
                evt.consume();
            }
        });

        w.closeRequested();
        assertEquals(1, calls.get());
        assertFalse(wm.getLastWindow().isDisposed(),
                "Consuming the close event must veto the close");
        w.dispose();
    }

    @FormTest
    void windowChromeReachesTheNativeWindow() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("chrome");
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();

        assertTrue(peer.isDecorated(), "windows are decorated by default");
        w.setDecorated(false);
        assertFalse(peer.isDecorated());

        w.setResizable(false);
        assertFalse(peer.isResizable());

        w.setAlwaysOnTop(true);
        assertTrue(peer.isAlwaysOnTop());

        w.requestWindowFocus();
        assertTrue(peer.isFocusRequested());
        w.dispose();
    }

    @FormTest
    void modalityMarksTheNativeWindow() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("modal");
        w.show();
        assertEquals(Window.MODALITY_NONE, w.getModalityType());

        w.setModalityType(Window.MODALITY_APPLICATION);
        assertEquals(Window.MODALITY_APPLICATION, w.getModalityType());
        assertTrue(wm.getLastWindow().isModal());
        w.dispose();
    }

    @FormTest
    void windowsGetIndependentIds() {
        implementation.setMultiWindowSupported(true);
        Window a = new Window("a");
        Window b = new Window("b");
        a.show();
        b.show();

        assertEquals(2, Desktop.getInstance().getWindows().length);
        assertSame(a, Desktop.getInstance().windowById(a.getWindowId()));
        assertSame(b, Desktop.getInstance().windowById(b.getWindowId()));
        assertTrue(a.getWindowId() != b.getWindowId(),
                "Each window needs its own id, since events are routed by it");
        a.dispose();
        b.dispose();
    }

    @FormTest
    void showInitializesTheHierarchy() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("Preferences", new BorderLayout());
        Label added = new Label("before show");
        w.add(BorderLayout.CENTER, added);
        assertFalse(added.isInitialized(),
                "nothing should be initialized before the window is shown");

        w.show();

        // Without this a Window is the one top level whose children never receive
        // initComponent(), so look and feel binding and peer attachment never happen.
        assertTrue(added.isInitialized(),
                "show() must initialize the hierarchy the way setCurrent() does for a Form");
        assertTrue(w.isInitialized());
        w.dispose();
        assertFalse(added.isInitialized(), "dispose() must deinitialize it again");
    }

    @FormTest
    void aFailedNativeWindowIsReportedRatherThanShown() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        wm.setCreateFails(true);
        final Window w = new Window("too many");
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                w.show();
            }
        }, "A window the platform could not create must not become a phantom window");
        assertEquals(0, Desktop.getInstance().getWindows().length,
                "a window that failed to open must not be registered");
    }

    @FormTest
    void modalityIsAcquiredByShowAndReleasedByDispose() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("modal");
        w.setModalityType(Window.MODALITY_APPLICATION);
        w.show();

        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertTrue(peer.isModal(),
                "a window shown with a modality type blocks, whether or not showModal was used");
        assertEquals(1, peer.getModalCalls());

        w.dispose();
        assertFalse(peer.isModal(),
                "the native modal flag must be dropped: on Windows it disables the main "
                        + "window, and leaving it set makes the application unusable");
        assertEquals(2, peer.getModalCalls(),
                "the flag has to be set and cleared exactly once each");
    }

    @FormTest
    void closeListenersFireOnceForOneClose() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("closes");
        final AtomicInteger closes = new AtomicInteger();
        w.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                closes.incrementAndGet();
            }
        });
        w.show();
        w.closeRequested();

        // dispose() used to fire them a second time, so one user close ran a
        // listener's save or cleanup work twice.
        assertEquals(1, closes.get(), "one close must notify a close listener once");
        assertTrue(w.isWindowDisposed());
    }

    @FormTest
    void anOwnedWindowIsDisposedWithItsOwner() {
        implementation.setMultiWindowSupported(true);
        Window owner = new Window("owner");
        owner.show();
        Window child = new Window("child");
        child.setOwnerWindow(owner);
        child.show();
        assertEquals(2, Desktop.getInstance().getWindows().length);

        owner.dispose();

        assertTrue(child.isWindowDisposed(),
                "an owned window cannot outlive its owner: the platform would leave it "
                        + "open with nothing behind it");
        assertEquals(0, Desktop.getInstance().getWindows().length);
    }

    @FormTest
    void theMinimumSizeReachesThePortAndClampsAResize() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("clamped", new BorderLayout());
        w.setWindowSize(800, 600);
        w.setMinimumWindowSize(new com.codename1.ui.geom.Dimension(320, 240));
        w.show();

        assertEquals(320, wm.getLastWindow().getMinimumWidth(),
                "the constraint has to reach the port, which is where it can be enforced");
        assertEquals(240, wm.getLastWindow().getMinimumHeight());

        // The framework deliberately does not re-clamp what the port reports. The
        // minimum is native geometry and includes the platform's chrome, while a
        // resize reports content dimensions, so clamping one against the other mixes
        // two coordinate spaces -- on a decorated window it laid the hierarchy out
        // larger than the canvas it is drawn into, clipping controls and putting hit
        // testing out of step with what is on screen. The window lays out to what it
        // was actually given; enforcing the minimum belongs to the platform that owns
        // the frame, which is why the assertions above matter.
        w.sizeChangedInternal(100, 80);
        assertEquals(100, w.getWidth());
        assertEquals(80, w.getHeight());
        w.dispose();
    }

    @FormTest
    void keysReachTheWindowsFocusedComponent() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("keys", new BorderLayout());
        final AtomicInteger pressed = new AtomicInteger();
        Button b = new Button("target") {
            @Override
            public void keyPressed(int keyCode) {
                super.keyPressed(keyCode);
                pressed.incrementAndGet();
            }
        };
        w.add(BorderLayout.CENTER, b);
        w.show();
        w.setFocused(b);

        // Container's inherited handler only forwards to a lead component, so without
        // Window dispatching keys itself the focused component never sees one.
        w.keyPressed('a');
        assertEquals(1, pressed.get(), "the focused component must receive the key");

        final AtomicInteger listened = new AtomicInteger();
        w.addKeyListener('b', new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                listened.incrementAndGet();
            }
        });
        w.keyReleased('b');
        assertEquals(1, listened.get(), "addKeyListener must fire in a window too");
        w.dispose();
    }

    @FormTest
    void hidingAModalWindowStopsItBlocking() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("modal");
        w.setModalityType(Window.MODALITY_APPLICATION);
        w.setCloseOperation(Window.HIDE_ON_CLOSE);
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertTrue(peer.isModal());

        w.closeRequested();

        // The user can no longer reach it, so it must not go on blocking what is
        // behind it -- natively or in the framework.
        assertFalse(w.isWindowShowing());
        assertFalse(peer.isModal(),
                "a hidden modal window must release the block it holds");
        w.dispose();
    }

    @FormTest
    void modalityTellsThePortWhatItBlocks() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window owner = new Window("owner");
        owner.show();
        TestWindowManager.FakeWindow ownerPeer = wm.getLastWindow();

        Window child = new Window("child");
        child.setOwnerWindow(owner);
        child.setModalityType(Window.MODALITY_WINDOW);
        child.show();
        TestWindowManager.FakeWindow childPeer = wm.getLastWindow();

        // A port applies modality by disabling the blocked window, so window scoped
        // modality naming the main window would make an unrelated part of the
        // application unusable.
        assertFalse(childPeer.isModalApplicationWide());
        assertSame(ownerPeer, childPeer.getModalOwner());
        assertSame(ownerPeer, childPeer.getOwner(),
                "the owner has to reach createWindow, or no platform knows the window "
                        + "should stay above it");
        owner.dispose();
    }

    @FormTest
    void aNarrowerModalDoesNotLiftABroaderOne() {
        implementation.setMultiWindowSupported(true);
        Window appModal = new Window("application modal");
        appModal.setModalityType(Window.MODALITY_APPLICATION);
        appModal.show();

        Window child = new Window("window modal");
        child.setOwnerWindow(appModal);
        child.setModalityType(Window.MODALITY_WINDOW);
        child.show();

        // The window modal on top blocks only its owner. Consulting just the newest
        // blocker would answer "not blocked" for the main form and for every unrelated
        // window, silently letting input back in while an application modal is still up.
        // The wheel entry point is the one that reports the answer synchronously.
        Display d = Display.getInstance();
        assertTrue(d.windowMouseWheelEvent(0, 5, 5, 0, 120, false, 0),
                "the main form stays blocked while an application modal is registered");

        child.dispose();
        assertTrue(d.windowMouseWheelEvent(0, 5, 5, 0, 120, false, 0),
                "and stays blocked once the narrower one is gone");

        appModal.dispose();
        assertFalse(d.windowMouseWheelEvent(0, 5, 5, 0, 120, false, 0),
                "input returns once no modal window is registered");
    }

    @FormTest
    void theOwnerCannotBeChangedOnceTheWindowExists() {
        implementation.setMultiWindowSupported(true);
        Window a = new Window("a");
        a.show();
        final Window b = new Window("b");
        b.show();
        final Window child = new Window("child");
        child.setOwnerWindow(a);
        child.show();

        // Native ownership is fixed when the window is created, and the port was told
        // to block a specific owner. Repointing the field would strand the modal
        // blocker on the previous owner and leave the platform relation on it too.
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                child.setOwnerWindow(b);
            }
        });
        a.dispose();
        b.dispose();
    }

    @FormTest
    void minimizingAModalWindowDoesNotEndItsModality() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("modal");
        w.setModalityType(Window.MODALITY_APPLICATION);
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();

        // The platform minimizing the window also clears nativeVisible. Reading that
        // as "the modal is over" would end the wait and drop the block, so restoring
        // the window would put a modal back on screen with input flowing behind it.
        w.hideNotify();
        assertTrue(peer.isModal(), "a minimized modal window is still modal");

        w.showNotify();
        assertTrue(w.isWindowShowing());
        assertTrue(peer.isModal());
        w.dispose();
        assertFalse(peer.isModal());
    }

    @FormTest
    void aMoveIsReportedToWindowListeners() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("moves");
        w.show();
        final AtomicInteger moves = new AtomicInteger();
        w.addWindowListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (((com.codename1.ui.events.WindowEvent) evt).getType()
                        == com.codename1.ui.events.WindowEvent.Type.Moved) {
                    moves.incrementAndGet();
                }
            }
        });

        // Only a monitor change was reported before, so an ordinary move within one
        // display never reached a listener and nothing could persist a position.
        Display.getInstance().windowMoved(w.getWindowId());
        flushSerialCalls();
        assertEquals(1, moves.get());
        w.dispose();
    }

    @FormTest
    void thePortIsToldWhetherAPositionWasChosen() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window placed = new Window("placed");
        // Negative is an ordinary coordinate: a monitor left of or above the primary
        // display has a negative origin, so it cannot double as "no position given".
        placed.setWindowBounds(new com.codename1.ui.geom.Rectangle(-1400, -200, 300, 200));
        placed.show();
        assertTrue(wm.getLastWindow().isPositionSet());
        assertEquals(-1400, wm.getLastWindow().getX());
        placed.dispose();

        Window unplaced = new Window("unplaced");
        unplaced.show();
        assertFalse(wm.getLastWindow().isPositionSet(),
                "a window that named no position must be placed by the platform");
        unplaced.dispose();
    }

    @FormTest
    void aFormOwnedWindowIsNotConfusedWithAnUnownedOne() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window unowned = new Window("unowned");
        unowned.show();
        assertNull(wm.getLastWindow().getOwner());
        assertFalse(wm.getLastWindow().isOwnedByMainWindow(),
                "an unowned window must not become a child of the main window");
        unowned.dispose();

        Window ownedByForm = new Window("owned by the form");
        ownedByForm.setOwnerWindow(Display.getInstance().getCurrent());
        ownedByForm.show();
        assertNull(wm.getLastWindow().getOwner(),
                "the main form has no window peer");
        assertTrue(wm.getLastWindow().isOwnedByMainWindow(),
                "but the port still has to be told it owns this window");
        ownedByForm.dispose();
    }

    @FormTest
    void draggingANonScrollableChildDoesNotRecurse() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("drags", new BorderLayout());
        Label plain = new Label("not scrollable");
        w.add(BorderLayout.CENTER, plain);
        w.show();

        // A drag bubbles up looking for something scrollable and used to stop only at
        // a Form. A Window dispatches drags to the pressed child itself, so bubbling
        // past it came straight back and recursed until the stack ran out.
        w.pointerPressed(10, 10);
        w.pointerDragged(12, 14);
        w.pointerReleased(12, 14);
        w.dispose();
    }

    @FormTest
    void aPressInTheTitleAreaReachesIt() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("titled", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        final AtomicInteger pressed = new AtomicInteger();
        Button chrome = new Button("close");
        chrome.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                pressed.incrementAndGet();
            }
        });
        w.getTitleArea().add(BorderLayout.EAST, chrome);
        w.show();
        w.revalidate();

        // The title area is a sibling of the content pane, so hit testing that always
        // started at the content pane could never reach a button drawn as chrome.
        int x = chrome.getAbsoluteX() + chrome.getWidth() / 2;
        int y = chrome.getAbsoluteY() + chrome.getHeight() / 2;
        w.pointerPressed(x, y);
        w.pointerReleased(x, y);
        assertEquals(1, pressed.get(),
                "a component in the title area has to receive presses");
        w.dispose();
    }

    @FormTest
    void anUnownedWindowModalBlocksNothing() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("unowned modal");
        w.setModalityType(Window.MODALITY_WINDOW);
        w.show();

        // Window modality blocks the owning window. There is none, so it blocks
        // nothing -- treating that as main-form ownership would block the main form
        // on a window that never claimed it.
        assertFalse(Display.getInstance().windowMouseWheelEvent(0, 5, 5, 0, 120, false, 0),
                "the main form is not the owner, so it must not be blocked");
        w.dispose();
    }

    @FormTest
    void showingAChildFirstStillEstablishesTheRealOwner() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window owner = new Window("owner");
        Window child = new Window("child");
        child.setOwnerWindow(owner);

        // The owner has never been shown, so it has no peer. Creating the child now
        // would fix the wrong native owner permanently, since every port establishes
        // the relation at creation.
        child.show();

        assertNotNull(wm.getLastWindow().getOwner(),
                "the owner's native window has to exist before the child's");
        assertFalse(wm.getLastWindow().isOwnedByMainWindow());
        owner.dispose();
    }

    @FormTest
    void nativeBlockingFollowsTheWholeModalStack() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window plain = new Window("plain");
        plain.show();
        TestWindowManager.FakeWindow plainPeer = wm.getLastWindow();

        Window appModal = new Window("application modal");
        appModal.setModalityType(Window.MODALITY_APPLICATION);
        appModal.show();
        TestWindowManager.FakeWindow appPeer = wm.getLastWindow();

        // Application modality blocks every other window natively, not just the main
        // one: a blocked window's own title bar is outside the framework's filter.
        assertFalse(wm.isMainWindowInputEnabled());
        assertFalse(plainPeer.isInputEnabled());
        assertTrue(appPeer.isInputEnabled(), "the modal window itself stays usable");

        Window inner = new Window("window modal");
        inner.setOwnerWindow(appModal);
        inner.setModalityType(Window.MODALITY_WINDOW);
        inner.show();
        inner.dispose();

        // Releasing the inner modal must not re-enable what the outer one still
        // blocks. A port counting its own depth got this wrong.
        assertFalse(wm.isMainWindowInputEnabled(),
                "the application modal is still up");
        assertFalse(plainPeer.isInputEnabled());

        appModal.dispose();
        assertTrue(wm.isMainWindowInputEnabled(), "nothing blocks any more");
        assertTrue(plainPeer.isInputEnabled());
        plain.dispose();
    }

    @FormTest
    void aWindowShownUnderAnApplicationModalIsBlockedNatively() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window appModal = new Window("application modal");
        appModal.setModalityType(Window.MODALITY_APPLICATION);
        appModal.show();

        // Shown *after* the modal, so it registers no blocker of its own and
        // acquireModal() does nothing for it. Ports enable a native window by
        // default, so without a resync at registration its title bar stayed live
        // -- focusable, movable, closable -- under a modal meant to block it.
        Window later = new Window("opened while blocked");
        later.show();
        TestWindowManager.FakeWindow laterPeer = wm.getLastWindow();
        assertFalse(laterPeer.isInputEnabled(),
                "a window opened under an application modal must start blocked");

        appModal.dispose();
        assertTrue(laterPeer.isInputEnabled(), "the modal is gone");
        later.dispose();
    }

    @FormTest
    void aWindowCannotOwnItself() {
        implementation.setMultiWindowSupported(true);
        final Window w = new Window("self owned");
        // show() creates an unshown owner's native window first, so a cycle here
        // recurses until the stack runs out before either peer exists -- and a
        // StackOverflowError names none of the windows involved.
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                w.setOwnerWindow(w);
            }
        });
        w.dispose();
    }

    @FormTest
    void aCycleThroughTheOwnerChainIsRejected() {
        implementation.setMultiWindowSupported(true);
        final Window a = new Window("a");
        final Window b = new Window("b");
        final Window c = new Window("c");
        b.setOwnerWindow(a);
        c.setOwnerWindow(b);
        // a -> c would close the loop a -> c -> b -> a. Only a walk of the whole
        // chain sees it; comparing against the immediate owner does not.
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                a.setOwnerWindow(c);
            }
        });
        c.dispose();
        b.dispose();
        a.dispose();
    }

    @FormTest
    void aGestureIsDispatchedToItsOwnWindow() {
        implementation.setMultiWindowSupported(true);
        final int[] mainPinches = new int[1];
        final int[] windowPinches = new int[1];

        Form main = new Form("main", new BorderLayout());
        main.add(BorderLayout.CENTER, new PinchCountingComponent(mainPinches));
        main.show();

        Window w = new Window("windowed", new BorderLayout());
        w.add(BorderLayout.CENTER, new PinchCountingComponent(windowPinches));
        w.setWindowSize(300, 200);
        w.show();

        // Aimed at the middle of the content, not the corner: the window's title
        // area covers the top rows and would answer the hit test instead.
        Display.getInstance().windowMagnifyGesture(w.getWindowId(), 150, 120, 1.5f);
        Display.getInstance().windowMagnifyGesture(0, 150, 120, 1.5f);
        // Disposed before asserting: a window left open by a failing assertion is
        // painted for the rest of the class and times out every later test.
        w.dispose();

        assertEquals(1, windowPinches[0], "the gesture belongs to the window it arrived on");
        assertEquals(1, mainPinches[0], "window 0 is still the main surface");
    }

    @FormTest
    void aGestureOverABlockedWindowIsDropped() {
        implementation.setMultiWindowSupported(true);
        final int[] pinches = new int[1];
        Window blocked = new Window("blocked", new BorderLayout());
        blocked.add(BorderLayout.CENTER, new PinchCountingComponent(pinches));
        blocked.setWindowSize(300, 200);
        blocked.show();

        Window appModal = new Window("application modal");
        appModal.setModalityType(Window.MODALITY_APPLICATION);
        appModal.show();

        // Gestures are filtered like every other input event: pinching a window a
        // modal is blocking has to do nothing, the same way clicking it does.
        Display.getInstance().windowMagnifyGesture(blocked.getWindowId(), 150, 120, 1.5f);
        int whileBlocked = pinches[0];

        appModal.dispose();
        Display.getInstance().windowMagnifyGesture(blocked.getWindowId(), 150, 120, 1.5f);
        int afterRelease = pinches[0];
        blocked.dispose();

        assertEquals(0, whileBlocked, "a blocked window must not see the gesture");
        assertEquals(1, afterRelease, "and resume once nothing blocks it");
    }

    /// Counts the pinches it is handed, so a test can tell which tree a gesture
    /// reached.
    private static final class PinchCountingComponent extends Component {
        private final int[] counter;

        PinchCountingComponent(int[] counter) {
            this.counter = counter;
        }

        @Override
        public boolean pinch(float scale) {
            counter[0]++;
            return true;
        }
    }

    @FormTest
    void aKeyReleaseIsNotStolenByAClickInAnotherWindow() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        final KeyCountingComponent mainKeys = new KeyCountingComponent();
        main.add(BorderLayout.CENTER, mainKeys);
        main.show();
        main.setFocused(mainKeys);

        Window w = new Window("other", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();

        // Press a key on the main form, then click the window before releasing it.
        // The two sequences tracked one shared target while there was only ever one
        // form; with windows this interleaving is ordinary, and the click used to
        // overwrite the key's target so the release never arrived -- leaving the
        // component latched in its pressed state.
        Display.getInstance().keyPressed(-90);
        int[] px = new int[]{150};
        int[] py = new int[]{120};
        Display.getInstance().windowPointerPressed(w.getWindowId(), px, py);
        Display.getInstance().windowPointerReleased(w.getWindowId(), px, py);
        Display.getInstance().keyReleased(-90);
        DisplayTest.flushEdt();
        w.dispose();

        assertEquals(1, mainKeys.pressed, "the press reached the main form");
        assertEquals(1, mainKeys.released,
                "and so must the release, despite the click on another window");
    }

    /// Counts the key events it receives, so a test can prove a release was matched
    /// to the component that saw the press.
    private static final class KeyCountingComponent extends Component {
        private int pressed;
        private int released;

        @Override
        public void keyPressed(int code) {
            pressed++;
        }

        @Override
        public void keyReleased(int code) {
            released++;
        }

        @Override
        public boolean isFocusable() {
            return true;
        }
    }

    @FormTest
    void repeatedMonitorReportsCollapseIntoOneNotification() {
        implementation.setMultiWindowSupported(true);
        final int[] fired = new int[1];
        Desktop.getInstance().addMonitorListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        });

        // One physical display change is reported many times over: Windows
        // broadcasts WM_DISPLAYCHANGE to every top level window, and GTK fires
        // geometry, work-area and scale-factor notifications separately per monitor.
        for (int iter = 0; iter < 5; iter++) {
            Display.getInstance().monitorsChanged();
        }
        DisplayTest.flushEdt();
        assertEquals(1, fired[0], "five reports of one change must notify once");

        // A later change is a new change, not a duplicate of the one already drained.
        Display.getInstance().monitorsChanged();
        DisplayTest.flushEdt();
        assertEquals(2, fired[0]);
    }

    @FormTest
    void overlappingKeyPressesAcrossWindowsEachReachTheirOwnTarget() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        final KeyCountingComponent mainKeys = new KeyCountingComponent();
        main.add(BorderLayout.CENTER, mainKeys);
        main.show();
        main.setFocused(mainKeys);

        Window w = new Window("other", new BorderLayout());
        final KeyCountingComponent windowKeys = new KeyCountingComponent();
        w.add(BorderLayout.CENTER, windowKeys);
        w.setWindowSize(300, 200);
        w.show();
        w.setFocused(windowKeys);

        // Hold a key on the main form, press a different key in the window before
        // releasing it, then release both. One target for the whole keyboard is not
        // enough: the second press overwrote the first, so the first release matched
        // nothing and cleared the field, and the second release then matched nothing
        // either -- latching a component in each window.
        Display.getInstance().keyPressed(-91);
        Display.getInstance().windowKeyPressed(w.getWindowId(), -92);
        Display.getInstance().keyReleased(-91);
        Display.getInstance().windowKeyReleased(w.getWindowId(), -92);
        DisplayTest.flushEdt();
        w.dispose();

        assertEquals(1, mainKeys.pressed);
        assertEquals(1, windowKeys.pressed);
        assertEquals(1, mainKeys.released,
                "the main form's release must survive a press in another window");
        assertEquals(1, windowKeys.released,
                "and so must the window's own");
    }

    @FormTest
    void theMainFormReportsTheMonitorItIsActuallyOn() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        // Two displays, with the application's main window on the second one.
        java.util.List<TestWindowManager.FakeMonitor> two =
                new java.util.ArrayList<TestWindowManager.FakeMonitor>();
        two.add(new TestWindowManager.FakeMonitor(0, 0, 1440, 900, 1.0, 96, "primary"));
        two.add(new TestWindowManager.FakeMonitor(1440, 0, 2560, 1440, 2.0, 192, "second"));
        wm.setMonitors(two);
        wm.setMainWindowMonitor(1);

        Form main = new Form("main");
        main.show();

        Monitor m = Desktop.getInstance().getMonitorFor(main);
        assertEquals(1, m.getIndex(),
                "a Form has no window peer, but its monitor is still answerable");
        assertFalse(m.isPrimary(),
                "reporting the primary monitor here gave the wrong work area and scale");
    }

    @FormTest
    void aKeyReleaseArrivingOnAnotherWindowStillReachesThePressTarget() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        final KeyCountingComponent mainKeys = new KeyCountingComponent();
        main.add(BorderLayout.CENTER, mainKeys);
        main.show();
        main.setFocused(mainKeys);

        Window w = new Window("other", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();

        // Press on the main form, then have the *window* report the release. That is
        // what a desktop window system does: key-up goes to whatever holds focus at
        // the time, so it names the window the user moved to rather than the one the
        // key went down in. Recording the press target is not enough on its own --
        // it has to be where the release is delivered, not merely something the
        // packet is checked against.
        Display.getInstance().keyPressed(-93);
        Display.getInstance().windowKeyReleased(w.getWindowId(), -93);
        DisplayTest.flushEdt();
        w.dispose();

        assertEquals(1, mainKeys.pressed);
        assertEquals(1, mainKeys.released,
                "the release belongs to the component that saw the press");
    }

    @FormTest
    void aDisabledComponentInAWindowIsNotActivatable() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("disabled", new BorderLayout());
        final int[] fired = new int[1];
        Button b = new Button("nope");
        b.setEnabled(false);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        });
        w.add(BorderLayout.CENTER, b);
        w.setWindowSize(300, 200);
        w.show();

        // Button.pointerPressed has no enabled check of its own -- it relies on the
        // top level never calling it -- so dispatching unconditionally let a disabled
        // button enter its pressed state and fire on release.
        w.pointerPressed(150, 120);
        w.pointerReleased(150, 120);
        int firedCount = fired[0];
        boolean stillReleased = b.getState() == Button.STATE_DEFAULT;
        w.dispose();

        assertEquals(0, firedCount, "a disabled button must not fire inside a window");
        assertTrue(stillReleased, "and must not be left in a pressed state");
    }

    @FormTest
    void aPressDraggedOutOfAButtonInAWindowIsCancelled() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("drag out", new BorderLayout());
        final int[] fired = new int[1];
        Button b = new Button("press me");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        });
        w.add(BorderLayout.CENTER, b);
        w.setWindowSize(300, 200);
        w.show();

        // Press on the button, drag well clear of it, release there. Form cancels the
        // press through its awaiting-release list; the window never consumed that list
        // because Button registered through getComponentForm(), which is null here.
        w.pointerPressed(150, 120);
        w.pointerDragged(2, 2);
        w.pointerReleased(2, 2);
        int firedCount = fired[0];
        w.dispose();

        assertEquals(0, firedCount,
                "releasing outside the button must not fire its action");
    }

    @FormTest
    void aReleaseFinishingAPressSurvivesAModalOpenedByThatPress() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("presser", new BorderLayout());
        final KeyCountingComponent keys = new KeyCountingComponent();
        w.add(BorderLayout.CENTER, keys);
        w.setWindowSize(300, 200);
        w.show();
        w.setFocused(keys);

        Display.getInstance().windowKeyPressed(w.getWindowId(), -94);
        DisplayTest.flushEdt();

        // The press handler opens an application modal, so the release arrives with
        // its own window blocked. Dropping it strands the component in its pressed
        // state for good and never clears the recorded target, so the *next* release
        // matches the wrong thing. Modality is there to stop new interaction, not to
        // abandon a gesture already under way.
        Window modal = new Window("modal");
        modal.setModalityType(Window.MODALITY_APPLICATION);
        modal.show();

        Display.getInstance().windowKeyReleased(w.getWindowId(), -94);
        DisplayTest.flushEdt();
        int released = keys.released;

        // A press that never happened stays blocked: this one leaves no record.
        Display.getInstance().windowKeyPressed(w.getWindowId(), -95);
        DisplayTest.flushEdt();
        int pressedWhileBlocked = keys.pressed;

        modal.dispose();
        w.dispose();

        assertEquals(1, released,
                "the release completing an accepted press must reach its target");
        assertEquals(1, pressedWhileBlocked,
                "but a new press on a blocked window must still be dropped");
    }

    @FormTest
    void focusChangesInAWindowRunTheFocusLifecycle() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("focus", new BorderLayout());
        final FocusCountingComponent a = new FocusCountingComponent();
        final FocusCountingComponent b = new FocusCountingComponent();
        Container box = new Container(new BorderLayout());
        box.add(BorderLayout.NORTH, a);
        box.add(BorderLayout.SOUTH, b);
        w.add(BorderLayout.CENTER, box);
        w.setWindowSize(300, 200);
        w.show();

        // Toggling the focus flag and repainting is not the same as running the
        // lifecycle: components build real behaviour on these notifications --
        // TextArea enables its input handling in focusGainedInternal -- so without
        // them an arrow key traversed away from a field instead of moving its caret.
        w.setFocused(a);
        w.setFocused(b);
        int aGained = a.gained;
        int aLost = a.lost;
        int bGained = b.gained;
        w.dispose();

        assertEquals(1, aGained, "the first component must be told it gained focus");
        assertEquals(1, aLost, "and told when it loses it");
        assertEquals(1, bGained, "and the second must be told it gained it");
    }

    /// Counts the focus notifications it receives.
    private static final class FocusCountingComponent extends Component {
        private int gained;
        private int lost;

        @Override
        public boolean isFocusable() {
            return true;
        }

        @Override
        public void fireFocusGained() {
            super.fireFocusGained();
            gained++;
        }

        @Override
        public void fireFocusLost() {
            super.fireFocusLost();
            lost++;
        }
    }

    @FormTest
    void aLongPressInAWindowReachesThePressedComponent() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("long press", new BorderLayout());
        final int[] longPresses = new int[1];
        Button b = new Button("hold me") {
            @Override
            public void longPointerPress(int x, int y) {
                longPresses[0]++;
            }
        };
        w.add(BorderLayout.CENTER, b);
        w.setWindowSize(300, 200);
        w.show();

        w.pointerPressed(150, 120);
        w.longPointerPress(150, 120);
        int count = longPresses[0];
        w.dispose();

        assertEquals(1, count,
                "Component's implementation only fires the window's own listeners, so "
                        + "a long press reached nothing inside a window");
    }

    @FormTest
    void aLongKeyPressInAWindowReachesTheFocusedComponent() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("long key", new BorderLayout());
        final int[] longKeys = new int[1];
        Component target = new Component() {
            @Override
            public boolean isFocusable() {
                return true;
            }

            @Override
            protected void longKeyPress(int keyCode) {
                longKeys[0]++;
            }
        };
        w.add(BorderLayout.CENTER, target);
        w.setWindowSize(300, 200);
        w.show();
        w.setFocused(target);

        // Display dispatches a long key press to the top level and Component's
        // implementation is empty, so without an override it reached nothing -- the
        // keyboard twin of the long-press defect.
        w.longKeyPress(-95);
        int count = longKeys[0];
        w.dispose();

        assertEquals(1, count, "a long key press must reach the focused component");
    }

    @FormTest
    void theUtilityWindowFlagReachesThePort() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("palette");
        w.setUtilityWindow(true);
        w.show();
        assertTrue(wm.getLastWindow().isUtility(),
                "setUtilityWindow only stored a field before; nothing reached the port");
        w.dispose();
    }

    @FormTest
    void anIconifiedWindowStopsBeingPainted() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("iconified", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.show();
        assertTrue(w.isWindowShowing());

        // What a port reports when the platform minimizes the window. Container's
        // implementation is inert, which would leave it counted as visible: still
        // painted, and its animations still keeping the event dispatch thread awake.
        w.hideNotify();
        assertFalse(w.isWindowShowing(),
                "a minimized window must stop counting as visible");

        w.showNotify();
        assertTrue(w.isWindowShowing(), "restoring must resume painting");
        w.dispose();
    }

    @FormTest
    void aResizeDropsPaintWorkQueuedAgainstTheOldSize() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("resizes", new BorderLayout());
        Label l = new Label("content");
        w.add(BorderLayout.CENTER, l);
        w.show();
        w.markPainted();
        l.repaint();

        w.sizeChangedInternal(900, 700);

        // Those rectangles were computed against the old geometry. A port that
        // reallocates its buffer on resize would paint them into a fresh, larger one
        // and leave the rest unpainted -- which is exactly what a capture caught.
        assertFalse(w.hasPaintedOnce(),
                "frames painted at the old size do not count once the window resized");
        assertEquals(900, w.getWidth());
        w.dispose();
    }

    @FormTest
    void hidingAWindowStopsItPinningPaintWork() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("hides", new BorderLayout());
        Label l = new Label("content");
        w.add(BorderLayout.CENTER, l);
        w.show();
        w.hide();

        // A hidden window is never painted, so anything queued on its surface would
        // never drain -- and an undrained queue keeps the event dispatch thread awake.
        assertFalse(w.isVisible(),
                "hiding must mark the hierarchy invisible so its components stop enqueuing");
        l.repaint();
        assertFalse(Display.impl.hasPendingPaints(),
                "a hidden window must not leave paint work that nothing will ever drain");
        w.dispose();
    }
}
