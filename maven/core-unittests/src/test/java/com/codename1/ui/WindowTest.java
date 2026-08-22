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
import com.codename1.ui.animations.Animation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WindowEvent;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
    void overlappingPointerPressesInTwoWindowsEachGetTheirRelease() {
        implementation.setMultiWindowSupported(true);
        Window a = new Window("a", new BorderLayout());
        final PressCountingComponent ca = new PressCountingComponent();
        a.add(BorderLayout.CENTER, ca);
        a.setWindowSize(300, 200);
        a.show();

        Window b = new Window("b", new BorderLayout());
        final PressCountingComponent cb = new PressCountingComponent();
        b.add(BorderLayout.CENTER, cb);
        b.setWindowSize(300, 200);
        b.show();

        int[] px = new int[]{150};
        int[] py = new int[]{120};
        // Two contacts down in two windows at once -- the Linux port deliberately
        // tracks a touch sequence per window, so this is reachable on a touchscreen.
        // A single shared target let B's press erase A's, after which both releases
        // were dropped and both components stayed latched.
        Display.getInstance().windowPointerPressed(a.getWindowId(), px, py);
        Display.getInstance().windowPointerPressed(b.getWindowId(), px, py);
        Display.getInstance().windowPointerReleased(a.getWindowId(), px, py);
        Display.getInstance().windowPointerReleased(b.getWindowId(), px, py);
        DisplayTest.flushEdt();
        int ra = ca.released;
        int rb = cb.released;
        b.dispose();
        a.dispose();

        assertEquals(1, ra, "the first window's release must reach its component");
        assertEquals(1, rb, "and so must the second window's");
    }

    /// Counts pointer releases, so a test can prove each window's press was matched.
    private static final class PressCountingComponent extends Component {
        private int released;

        @Override
        public void pointerReleased(int x, int y) {
            released++;
        }
    }

    @FormTest
    void aPressInOneWindowDoesNotCancelAnothersLongPress() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window a = new Window("a", new BorderLayout());
        a.add(BorderLayout.CENTER, new Label("a"));
        a.setWindowSize(300, 200);
        a.show();
        Window b = new Window("b", new BorderLayout());
        b.add(BorderLayout.CENTER, new Label("b"));
        b.setWindowSize(300, 200);
        b.show();

        int[] px = new int[]{150};
        int[] py = new int[]{120};
        // Press in A, then in B. The long-press timer was a single set of fields, so
        // B's press replaced A's coordinates and clock, and releasing either one
        // cancelled the other's pending long press.
        Display.getInstance().windowPointerPressed(a.getWindowId(), px, py);
        Display.getInstance().windowPointerPressed(b.getWindowId(), px, py);
        Display.getInstance().windowPointerReleased(a.getWindowId(), px, py);
        DisplayTest.flushEdt();

        boolean bStillArmed = longPressArmedFor(b.getWindowId());
        b.dispose();
        a.dispose();

        assertTrue(bStillArmed,
                "releasing one window's contact must not cancel another window's "
                        + "pending long press");
    }

    /// Reads Display's per-window long-press table, which is private state with no
    /// public accessor.
    private static boolean longPressArmedFor(int windowId) throws Exception {
        // Same -1 keying for the main surface as keyRepeatArmedFor.
        java.lang.reflect.Field wf = Display.class.getDeclaredField("longPressWindows");
        java.lang.reflect.Field af = Display.class.getDeclaredField("longPressArmed");
        wf.setAccessible(true);
        af.setAccessible(true);
        int[] windows = (int[]) wf.get(Display.getInstance());
        boolean[] armed = (boolean[]) af.get(Display.getInstance());
        for (int iter = 0; iter < windows.length; iter++) {
            if (windows[iter] == windowId && armed[iter]) {
                return true;
            }
        }
        return false;
    }

    @FormTest
    void aBlockedWindowsPressDoesNotLeaveALongPressArmed() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window blocked = new Window("blocked", new BorderLayout());
        blocked.add(BorderLayout.CENTER, new Label("content"));
        blocked.setWindowSize(300, 200);
        blocked.show();

        Window modal = new Window("modal");
        modal.setModalityType(Window.MODALITY_APPLICATION);
        modal.show();

        int[] px = new int[]{150};
        int[] py = new int[]{120};
        // The timer is charged when the press is queued, before modality has had a
        // say, and the event dispatch thread fires longPointerPress directly without
        // re-checking -- so a context menu could open behind the modal for a press
        // the component never received.
        Display.getInstance().windowPointerPressed(blocked.getWindowId(), px, py);
        DisplayTest.flushEdt();
        boolean armed = longPressArmedFor(blocked.getWindowId());

        modal.dispose();
        blocked.dispose();
        assertFalse(armed,
                "a press rejected by modality must not leave its long press armed");
    }

    @FormTest
    void windowLevelPointerListenersFire() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("listeners", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();
        final int[] counts = new int[3];
        w.addPointerPressedListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                counts[0]++;
            }
        });
        w.addPointerDraggedListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                counts[1]++;
            }
        });
        w.addPointerReleasedListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                counts[2]++;
            }
        });

        // These dispatchers were never fired, so listeners attached to a Window did
        // nothing -- and material pull to refresh went with them, since Component
        // installs its refresh listeners on the top level.
        w.pointerPressed(150, 120);
        w.pointerDragged(150, 130);
        w.pointerReleased(150, 130);
        w.dispose();

        assertEquals(1, counts[0], "the window's pointer pressed listener must fire");
        assertEquals(1, counts[1], "and its dragged listener");
        assertEquals(1, counts[2], "and its released listener");
    }

    @FormTest
    void aPressInOneWindowDoesNotClearAnothersDragOccurred() {
        implementation.setMultiWindowSupported(true);
        final boolean[] seen = new boolean[1];
        Window a = new Window("a", new BorderLayout());
        a.add(BorderLayout.CENTER, new Component() {
            @Override
            public void pointerReleased(int x, int y) {
                seen[0] = Display.getInstance().hasDragOccured();
            }
        });
        a.setWindowSize(300, 200);
        a.show();
        Window b = new Window("b", new BorderLayout());
        b.add(BorderLayout.CENTER, new Label("b"));
        b.setWindowSize(300, 200);
        b.show();

        int[] px = new int[]{150};
        int[] py = new int[]{120};
        int[] py2 = new int[]{160};
        Display.getInstance().windowPointerPressed(a.getWindowId(), px, py);
        Display.getInstance().windowPointerDragged(a.getWindowId(), px, py2);
        DisplayTest.flushEdt();
        // B's press cleared the global flag after A had already dragged, so releasing
        // A made List and friends read hasDragOccured() as false and treat a
        // completed drag as a click.
        Display.getInstance().windowPointerPressed(b.getWindowId(), px, py);
        DisplayTest.flushEdt();
        Display.getInstance().windowPointerReleased(a.getWindowId(), px, py2);
        DisplayTest.flushEdt();
        b.dispose();
        a.dispose();

        // Read from inside A's own release dispatch, which is where List and
        // ContainerList consult it -- and the only context where the answer is
        // defined, now that the selector is restored when dispatch unwinds.
        assertTrue(seen[0],
                "a press in another window must not erase this window's drag state");
    }

    @FormTest
    void aDraggableComponentInAWindowGetsDragAndDropPrimed() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("dnd", new BorderLayout());
        Label draggable = new Label("drag me");
        draggable.setDraggable(true);
        w.add(BorderLayout.CENTER, draggable);
        w.setWindowSize(300, 200);
        w.show();

        // Component.pointerDragged checks dragAndDropInitialized and silently does
        // nothing without it, so drag and drop was unusable in a window.
        w.pointerPressed(150, 120);
        boolean primed = draggable.isDragAndDropInitialized();
        w.dispose();

        assertTrue(primed, "a press must prime drag and drop, as Form does");
    }

    @FormTest
    void multiTouchDragsStillNotifyWindowListeners() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("multi", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();
        final int[] drags = new int[1];
        w.addPointerDraggedListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                drags[0]++;
            }
        });

        // The listener block was added to the scalar overload only, so a gesture
        // stopped notifying the moment it became multi touch.
        w.pointerDragged(new int[]{150, 160}, new int[]{120, 130});
        int count = drags[0];
        w.dispose();

        assertEquals(1, count, "a multi touch drag must notify window listeners too");
    }

    @FormTest
    void aMinimizedWindowStopsQueueingRepaints() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("minimized", new BorderLayout());
        Label content = new Label("content");
        w.add(BorderLayout.CENTER, content);
        w.setWindowSize(300, 200);
        w.show();
        DisplayTest.flushEdt();

        w.hideNotify();
        // paintOpenWindows skips a window that is not showing while hasPendingPaints
        // still counts its queue, so anything queued here can never drain and the
        // event dispatch thread spins until the window is restored.
        content.repaint();
        w.repaint();
        boolean pendingWhileMinimized = Display.impl.hasPendingPaints();
        w.dispose();

        assertFalse(pendingWhileMinimized,
                "a minimized window must not queue paint work that cannot drain");
    }

    @FormTest
    void dragHistorySlotsAreReclaimedAfterEachGesture() throws Exception {
        implementation.setMultiWindowSupported(true);
        // Far more gestures than the table has entries. Slots were released only on
        // disposal, so a handful of long-lived windows exhausted it and a later
        // window could record neither drag state nor velocity.
        Window[] windows = new Window[10];
        for (int iter = 0; iter < windows.length; iter++) {
            windows[iter] = new Window("w" + iter, new BorderLayout());
            windows[iter].add(BorderLayout.CENTER, new Label("c"));
            windows[iter].setWindowSize(300, 200);
            windows[iter].show();
            int[] px = new int[]{150};
            int[] py = new int[]{120};
            Display.getInstance().windowPointerPressed(windows[iter].getWindowId(), px, py);
            Display.getInstance().windowPointerReleased(windows[iter].getWindowId(), px, py);
            DisplayTest.flushEdt();
        }

        java.lang.reflect.Field f = Display.class.getDeclaredField("dragHistoryWindows");
        f.setAccessible(true);
        int[] table = (int[]) f.get(Display.getInstance());
        int used = 0;
        for (int entry : table) {
            if (entry != 0) {
                used++;
            }
        }
        for (Window w : windows) {
            w.dispose();
        }

        // Only the main surface's permanent entry should remain held.
        assertEquals(1, used,
                "a finished gesture must hand its drag-history slot back");
    }

    @FormTest
    void aNestedPressSurvivesTheOuterReleaseTeardown() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("nested", new BorderLayout());
        final Window[] self = new Window[]{w};
        Component target = new Component() {
            @Override
            public void pointerReleased(int x, int y) {
                // Stands in for a handler that enters invokeAndBlock and has a fresh
                // press dispatched to the same window before it returns.
                self[0].pointerPressed(150, 120);
            }
        };
        w.add(BorderLayout.CENTER, target);
        w.setWindowSize(300, 200);
        w.show();

        w.pointerPressed(150, 120);
        w.pointerReleased(150, 120);
        // The replacement press must still be installed: tearing down by window
        // rather than by gesture erased it.
        boolean replacementHeld = w.getCurrentPointerPress() != null;
        w.dispose();

        assertTrue(replacementHeld,
                "a press installed during the outer release must survive its teardown");
    }

    @FormTest
    void anActivatedDragIsFinishedWhenReleasedInAWindow() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("dnd release", new BorderLayout());
        final int[] finished = new int[1];
        Label draggable = new Label("drag me") {
            @Override
            protected void dragFinishedImpl(int x, int y) {
                super.dragFinishedImpl(x, y);
                finished[0]++;
            }
        };
        draggable.setDraggable(true);
        w.add(BorderLayout.CENTER, draggable);
        w.setWindowSize(300, 200);
        w.show();

        w.pointerPressed(150, 120);
        // Enough movement to activate the drag rather than merely scroll.
        w.pointerDragged(150, 121);
        w.pointerDragged(160, 140);
        w.pointerDragged(170, 160);
        w.pointerReleased(170, 160);
        int count = finished[0];
        w.dispose();

        // Component hides the component when the drag activates and only
        // dragFinishedImpl restores it and runs the drop, so releasing through the
        // ordinary path left it invisible with the drop unfinished.
        assertEquals(1, count, "an activated drag must be finished, not merely released");
    }

    @FormTest
    void aBlockedWindowsKeyPressDoesNotLeaveTimersArmed() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window blocked = new Window("blocked keys", new BorderLayout());
        blocked.add(BorderLayout.CENTER, new Label("content"));
        blocked.setWindowSize(300, 200);
        blocked.show();

        Window modal = new Window("modal");
        modal.setModalityType(Window.MODALITY_APPLICATION);
        modal.show();

        // keyPressedImpl arms the repeat and long-key timers before modality has had
        // a say, and the paint loop fires them directly without re-checking, so
        // holding a key could drive a component behind the modal.
        // A positive key code, because the timers are only armed for a code that can
        // repeat -- with a negative one the test would pass without testing anything,
        // which is what the first version of it did.
        Display.getInstance().windowKeyPressed(blocked.getWindowId(), 65);
        DisplayTest.flushEdt();
        boolean armed = keyRepeatArmedFor(blocked.getWindowId());

        modal.dispose();
        blocked.dispose();
        assertFalse(armed,
                "a key press rejected by modality must not leave its timers armed");
    }

    /// Reads Display's per-window key repeat table, which has no public accessor.
    private static boolean keyRepeatArmedFor(int windowId) throws Exception {
        java.lang.reflect.Field wf = Display.class.getDeclaredField("keyRepeatWindows");
        java.lang.reflect.Field af = Display.class.getDeclaredField("keyRepeatArmed");
        wf.setAccessible(true);
        af.setAccessible(true);
        int[] windows = (int[]) wf.get(Display.getInstance());
        boolean[] armed = (boolean[]) af.get(Display.getInstance());
        // Display keys the main surface as -1 so that 0 can mean "unused". Comparing
        // against 0 here matched nothing, which is what made the first version of the
        // repeat test pass with the fix removed.
        int key = windowId == 0 ? -1 : windowId;
        for (int iter = 0; iter < windows.length; iter++) {
            if (windows[iter] == key && armed[iter]) {
                return true;
            }
        }
        return false;
    }

    @FormTest
    void anAutorepeatInAnotherWindowKeepsTheOriginalPressTarget() {
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

        Display.getInstance().keyPressed(-97);
        // The native ports forward every autorepeat as another press; this one
        // arrives while the other window has focus.
        Display.getInstance().windowKeyPressed(w.getWindowId(), -97);
        Display.getInstance().keyReleased(-97);
        DisplayTest.flushEdt();
        int released = mainKeys.released;
        w.dispose();

        assertEquals(1, released,
                "a repeat elsewhere must not steal the original press's target");
    }

    @FormTest
    void theWindowAnimationLockBehavesLikeAForms() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("anim lock", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();

        // An idle window must grant the lock, a second caller must be refused while
        // it is held, and releasing must not throw. The previous implementation
        // returned isAnimating() -- so it granted the lock only when something else
        // was already animating -- and released by handing null to flushAnimation,
        // which either invoked it on the spot or queued it for the event dispatch
        // thread to invoke: an NPE either way.
        boolean first = w.grabAnimationLock();
        boolean second = w.grabAnimationLock();
        w.releaseAnimationLock();
        boolean afterRelease = w.grabAnimationLock();
        w.releaseAnimationLock();
        DisplayTest.flushEdt();
        w.dispose();

        assertTrue(first, "an idle window must grant the lock");
        assertFalse(second, "a second caller must be refused while it is held");
        assertTrue(afterRelease, "and it must be grantable again after release");
    }

    @FormTest
    void anUnrelatedModalIsStillBlockedByAnApplicationModal() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window appModal = new Window("application modal");
        appModal.setModalityType(Window.MODALITY_APPLICATION);
        appModal.show();

        // Unowned, so not nested inside the first one. Being modal itself must not
        // exempt it from application modality -- the self check used to return for
        // any modal, which let this one accept input.
        Window unrelated = new Window("unrelated modal");
        unrelated.setModalityType(Window.MODALITY_APPLICATION);
        unrelated.show();
        TestWindowManager.FakeWindow unrelatedPeer = wm.getLastWindow();
        boolean blocked = !unrelatedPeer.isInputEnabled();

        // Disposed before the nested case: while it is up, the nested modal is
        // legitimately blocked *by it* -- an unrelated application modal blocks
        // everything outside its own chain, this window included. Leaving it open
        // made the first version of this test assert the opposite.
        unrelated.dispose();

        // A modal nested inside the first is still exempt from it.
        Window nested = new Window("nested modal");
        nested.setOwnerWindow(appModal);
        nested.setModalityType(Window.MODALITY_APPLICATION);
        nested.show();
        boolean nestedUsable = wm.getLastWindow().isInputEnabled();

        nested.dispose();
        appModal.dispose();

        assertTrue(blocked, "an unrelated modal must still be blocked by an application modal");
        assertTrue(nestedUsable, "but a modal nested inside it must stay usable");
    }

    @FormTest
    void aKeyReleasedAfterFocusMovedCancelsThePressingWindowsRepeat() throws Exception {
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

        // Pressed on the main surface, released while the other window has focus.
        // Cancelling by the releasing window left the main surface repeating every
        // 10ms with the key physically up.
        Display.getInstance().keyPressed(66);
        Display.getInstance().windowKeyReleased(w.getWindowId(), 66);
        DisplayTest.flushEdt();
        boolean stillArmed = keyRepeatArmedFor(0) || keyRepeatArmedFor(w.getWindowId());
        w.dispose();

        assertFalse(stillArmed,
                "releasing a key must cancel the repeat armed by its press");
    }

    @FormTest
    void aReleaseDuringAPressCallbackFindsItsAcceptedPress() {
        implementation.setMultiWindowSupported(true);
        final Window w = new Window("nested press", new BorderLayout());
        final int[] released = new int[1];
        final boolean[] reentered = new boolean[1];
        Component target = new Component() {
            @Override
            public void pointerPressed(int x, int y) {
                if (!reentered[0]) {
                    reentered[0] = true;
                    // Stands in for a callback entering a nested loop (showModal)
                    // during which the physical release is processed.
                    int[] px = new int[]{150};
                    int[] py = new int[]{120};
                    Display.getInstance().windowPointerReleased(w.getWindowId(), px, py);
                    DisplayTest.flushEdt();
                }
            }

            @Override
            public void pointerReleased(int x, int y) {
                released[0]++;
            }
        };
        w.add(BorderLayout.CENTER, target);
        w.setWindowSize(300, 200);
        w.show();
        w.setFocused(target);

        int[] px = new int[]{150};
        int[] py = new int[]{120};
        Display.getInstance().windowPointerPressed(w.getWindowId(), px, py);
        DisplayTest.flushEdt();
        int count = released[0];
        w.dispose();

        // Recording the press only after the callback returned meant a release
        // processed inside it saw no accepted press.
        assertEquals(1, count, "a release during the press callback must find its press");
    }

    @FormTest
    void hidingAWindowCancelsItsKeyTimers() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("hide timers", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();

        // A key handler can hide its own window. The window stays registered, so a
        // repeat armed by the press that got us here would keep firing into a tree
        // the user cannot see -- and the key-up may never arrive once the native
        // window has lost focus.
        Display.getInstance().windowKeyPressed(w.getWindowId(), 67);
        DisplayTest.flushEdt();
        w.hide();
        boolean armed = keyRepeatArmedFor(w.getWindowId());
        w.dispose();

        assertFalse(armed, "hiding a window must cancel the timers armed for it");
    }

    @FormTest
    void settingOnlyTheSizeLeavesPlacementToTheWindowManager() throws Exception {
        implementation.setMultiWindowSupported(true);
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("size only", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        // The documented pre-show call. Routing it through setWindowBounds handed the
        // port the placeholder (0,0) as though the application had chosen it.
        w.setWindowSize(420, 260);
        w.show();
        boolean positionSet = wm.getLastWindow().isPositionSet();
        w.dispose();

        assertFalse(positionSet,
                "setting only the size must leave the position unspecified");
    }

    @FormTest
    void hidingAWindowUnlatchesTheComponentThatTookThePress() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("latched", new BorderLayout());
        Button b = new Button("fire me");
        w.add(BorderLayout.CENTER, b);
        w.setWindowSize(300, 200);
        w.show();
        w.setFocused(b);

        // The scenario the hide cleanup exists for: the component takes the press and
        // the window goes away before the release. Dropping the records without
        // telling the component left it in STATE_PRESSED, still latched when the
        // window was shown again.
        // The test implementation maps a key code straight to its game action, so
        // GAME_FIRE is the code that reaches Button.pressed().
        b.keyPressed(Display.GAME_FIRE);
        int pressedState = b.getState();
        w.hide();
        int afterHide = b.getState();
        w.dispose();

        assertEquals(Button.STATE_PRESSED, pressedState, "the press must latch it first");
        assertTrue(afterHide != Button.STATE_PRESSED,
                "hiding the window must unlatch the component that took the press");
    }

    @FormTest
    void minimizingAWindowCancelsItsTimersToo() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("minimize timers", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();

        Display.getInstance().windowKeyPressed(w.getWindowId(), 68);
        DisplayTest.flushEdt();
        // Native minimization arrives through hideNotify, not hide(), and bypassed
        // the cleanup entirely -- the window stays registered either way.
        w.hideNotify();
        boolean armed = keyRepeatArmedFor(w.getWindowId());
        w.dispose();

        assertFalse(armed, "minimizing must cancel the timers armed for the window");
    }

    @FormTest
    void hidingDuringADragRestoresTheDraggedComponent() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("drag hide", new BorderLayout());
        Label draggable = new Label("drag me");
        draggable.setDraggable(true);
        w.add(BorderLayout.CENTER, draggable);
        w.setWindowSize(300, 200);
        w.show();

        w.pointerPressed(150, 120);
        w.pointerDragged(150, 121);
        w.pointerDragged(165, 145);
        w.pointerDragged(175, 165);
        // Component hides the dragged component when the drag activates; only
        // dragFinishedImpl restores it, and dragInitiated does not.
        w.hide();
        boolean visible = draggable.isVisible();
        boolean stillInitialized = draggable.isDragAndDropInitialized();
        w.dispose();

        assertTrue(visible, "hiding mid-drag must restore the dragged component");
        assertFalse(stillInitialized, "and must clear its drag-and-drop state");
    }

    @FormTest
    void losingFocusCancelsHeldInput() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("focus loss", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();

        Display.getInstance().windowKeyPressed(w.getWindowId(), 69);
        DisplayTest.flushEdt();
        // The user switches to another application while holding the key. The
        // key-up goes to whoever has focus now, so nothing else would ever stop
        // this window repeating.
        Display.getInstance().windowFocusChanged(w.getWindowId(), false);
        DisplayTest.flushEdt();
        boolean armed = keyRepeatArmedFor(w.getWindowId());
        w.dispose();

        assertFalse(armed, "losing focus must cancel input held in the window");
    }

    @FormTest
    void aPacketQueuedBeforeHideDoesNotRestartTheGesture() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("late packet", new BorderLayout());
        Button b = new Button("press me");
        w.add(BorderLayout.CENTER, b);
        w.setWindowSize(300, 200);
        w.show();

        // Queued, then the window is hidden before the event dispatch thread drains
        // it. Dispatching the press re-latches the component the hide just
        // unlatched, with no release coming -- the timer is armed at queue time, so
        // the observable damage is the component's state rather than the timer's.
        Display.getInstance().windowPointerPressed(w.getWindowId(),
                new int[]{150}, new int[]{120});
        w.hide();
        DisplayTest.flushEdt();
        boolean latched = b.getState() == Button.STATE_PRESSED;
        w.dispose();

        assertFalse(latched,
                "a packet queued before the hide must not re-latch the component");
    }

    @FormTest
    void aOneShotTimerBoundToAWindowStopsAfterFiring() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("one shot", new BorderLayout());
        w.add(BorderLayout.CENTER, new Label("content"));
        w.setWindowSize(300, 200);
        w.show();

        final int[] fired = new int[1];
        // A one-shot deregistered itself from the *current form* rather than from the
        // window it was bound to, so it stayed in the window's animation list and
        // fired again every interval, forever.
        com.codename1.ui.util.UITimer t =
                com.codename1.ui.util.UITimer.timer(1, false, w, new Runnable() {
                    @Override
                    public void run() {
                        fired[0]++;
                    }
                });
        // Driven directly rather than through the paint loop: what changed is which
        // top level the one-shot deregisters itself from, and the animation pass is
        // not what this needs to observe.
        int registeredBefore;
        int registeredAfter;
        try {
            java.lang.reflect.Field af = Window.class.getDeclaredField("animatableComponents");
            af.setAccessible(true);
            java.util.ArrayList<?> anims = (java.util.ArrayList<?>) af.get(w);
            registeredBefore = anims.size();
            java.lang.reflect.Method tick =
                    com.codename1.ui.util.UITimer.class.getDeclaredMethod("testEllapse");
            tick.setAccessible(true);
            Thread.sleep(3);
            tick.invoke(t);
            registeredAfter = ((java.util.ArrayList<?>) af.get(w)).size();
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        w.dispose();

        // Firing is what deregisters a one-shot. It used to deregister from the
        // current form instead, leaving it in the window's list to fire forever.
        assertEquals(1, fired[0], "the timer must have fired");
        assertEquals(1, registeredBefore, "it registers with the window it is bound to");
        assertEquals(0, registeredAfter, "and deregisters from that same window");
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

    @FormTest
    void captureUsesThePortsReadbackWhenItHasOneAndRendersWhenItDoesNot() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("shot", new BorderLayout());
        w.setWindowSize(300, 200);
        w.show();

        // A port that can read its own window back must be used: re-rendering the
        // hierarchy produces the content the window *should* be showing, so it cannot
        // tell a correct window from one whose raster and hierarchy disagree -- which
        // is the whole thing the windowed screenshot goldens exist to catch.
        Object readback = implementation.createImage(new int[64 * 32], 64, 32);
        wm.setCaptureResult(readback);
        Image shot = w.capture();
        assertNotNull(shot);
        assertEquals(1, wm.getCaptureCalls(), "the port must be asked first");
        assertEquals(64, shot.getWidth(),
                "capture() must hand back the port's readback, not a re-render at the "
                        + "window's size");
        assertEquals(32, shot.getHeight());

        // A port with no readback still owes a capture, so the re-render remains --
        // at the window's own size rather than the main display's.
        wm.setCaptureResult(null);
        Image rendered = w.capture();
        assertNotNull(rendered, "a port that cannot read back still owes a capture");
        assertEquals(w.getWidth(), rendered.getWidth());
        assertEquals(w.getHeight(), rendered.getHeight());

        w.dispose();
    }

    @FormTest
    void animatedComponentsRegisterWithTheWindowTheyLiveIn() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("animated", new BorderLayout());
        w.setWindowSize(400, 300);
        com.codename1.components.Switch sw = new com.codename1.components.Switch();
        w.add(BorderLayout.CENTER, sw);
        w.show();
        w.revalidate();

        // getComponentForm() is null by design inside a Window, so every component that
        // registered its animation through it threw on an ordinary interaction there --
        // a tapped Switch could not toggle at all. The registration has to resolve
        // through the top level instead.
        boolean before = sw.isValue();
        sw.pointerPressed(sw.getAbsoluteX() + 2, sw.getAbsoluteY() + 2);
        sw.pointerReleased(sw.getAbsoluteX() + 2, sw.getAbsoluteY() + 2);

        // The toggle completes on the animation, so the observable result here is that
        // the interaction was accepted and an animation was registered against the
        // window rather than throwing on the way.
        assertTrue(w.isWindowShowing());
        assertEquals(before, sw.isValue(),
                "the value flips when the animation finishes, not on release");

        // The guard around a registration matters as much as the registration. Several
        // of these sites sat inside an `if (getComponentForm() != null)`, so migrating
        // only the call left it unreachable in a Window -- a fix that changed nothing.
        // Toolbar was the worst of them: hideToolbar() compared the current Form with a
        // null one and took its early return, and the following showToolbar() then went
        // down its hidden branch and dereferenced that null form.
        // Hidden on a window that is not on screen, which is the path that used to
        // strand the toolbar: hideToolbar() took its early return and marked the
        // toolbar invisible, and showToolbar() then went down its hidden branch and
        // dereferenced the null form.
        Window off = new Window("offscreen", new BorderLayout());
        off.setWindowSize(400, 300);
        Toolbar tb = new Toolbar();
        off.setToolbar(tb);
        off.revalidate();
        tb.hideToolbar();
        assertFalse(tb.isVisible(),
                "a toolbar hidden while its window is off screen is hidden outright");
        tb.showToolbar();
        assertTrue(tb.isVisible(),
                "and showing it again must bring it back rather than throw");
        off.dispose();

        // ImageViewer registers the same way from its animated setZoom path, which is
        // an ordinary operation rather than an edge case.
        com.codename1.components.ImageViewer viewer =
                new com.codename1.components.ImageViewer(Image.createImage(32, 32));
        w.add(BorderLayout.NORTH, viewer);
        w.revalidate();
        viewer.setZoom(2f);
        assertNotNull(viewer.getImage(), "zooming in a window must not throw");

        w.dispose();
    }

    @FormTest
    void anInfiniteProgressAnimatesAndTearsDownInsideAWindow() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("busy", new BorderLayout());
        w.setWindowSize(300, 200);
        com.codename1.components.InfiniteProgress progress =
                new com.codename1.components.InfiniteProgress();
        w.add(BorderLayout.CENTER, progress);
        w.show();
        w.revalidate();

        // The spinner decided whether to animate by comparing Display.getCurrent() --
        // which only ever names a Form -- with its own getComponentForm(), null inside
        // a Window. That is false for every spinner in a window, so it registered
        // nothing and sat completely static.
        assertTrue(progress.animate(false),
                "an infinite progress in a shown window must animate");

        // And teardown resolved the form with a fallback to the current form, which
        // threw in a window-only application -- during Window.dispose(), before the
        // native peer and paint surface were released.
        w.dispose();
        assertTrue(w.isWindowDisposed(),
                "dispose must complete rather than throw on the way through teardown");
    }

    @FormTest
    void builtInComponentsInitialiseAndAnimateInsideAWindow() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("built ins", new BorderLayout());
        w.setWindowSize(400, 300);

        // AutoCompleteTextField registered its pointer listeners straight off
        // getComponentForm() in initComponent, so the first show() of a window
        // containing one threw before anything else could run.
        AutoCompleteTextField auto =
                new AutoCompleteTextField("alpha", "beta", "gamma");
        w.add(BorderLayout.NORTH, auto);

        // A Label with an animated icon registered through a local Form variable in
        // checkAnimation() -- the indirect form the first sweep's direct-call grep did
        // not match -- so the icon stayed frozen.
        Label animated = new Label(makeAnimatedImage());
        w.add(BorderLayout.CENTER, animated);

        w.show();
        w.revalidate();

        assertTrue(w.isWindowShowing(),
                "showing a window with built-in components must not throw");

        // The label's registration is silently skipped rather than throwing when it
        // resolves a null form, so "did not throw" proves nothing about it. The window's
        // own animation list is the observable: the icon animates only if the label is
        // in it.
        assertTrue(windowAnimates(w, animated),
                "a label with an animated icon must register with the window it lives "
                        + "in; resolving the form instead leaves the icon frozen");

        w.dispose();
    }

    /// True when the window has the given component in its animation list. Read
    /// reflectively because the list is private -- and it is the only observable that
    /// separates "registered with the window" from "silently skipped", which is what
    /// the null-form guard does.
    private static boolean windowAnimates(Window w, Object cmp) {
        try {
            java.lang.reflect.Field f =
                    Window.class.getDeclaredField("animatableComponents");
            f.setAccessible(true);
            return ((java.util.List) f.get(w)).contains(cmp);
        } catch (Exception err) {
            throw new IllegalStateException(err);
        }
    }

    @FormTest
    void selectingACalendarDayInsideAWindowDoesNotThrow() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("calendar", new BorderLayout());
        w.setWindowSize(400, 400);
        Calendar cal = new Calendar();
        w.add(BorderLayout.CENTER, cal);
        w.show();
        w.revalidate();

        // MonthView.actionPerformed() asked getComponentForm().isSingleFocusMode()
        // unconditionally, so every ordinary day selection updated the date, fired its
        // listeners, and then threw on the way out.
        final boolean[] fired = new boolean[1];
        cal.addActionListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                fired[0] = true;
            }
        });
        Button day = findFirstDayButton(cal);
        assertNotNull(day, "the month view should contain day buttons");
        day.pressed();
        day.released();

        assertTrue(fired[0], "selecting a day must fire its listeners");
        assertTrue(w.isWindowShowing(), "and must not throw on the way out");
        w.dispose();
    }

    @FormTest
    void groupedRadioButtonsAndTextAreasWorkInsideAWindow() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("radios", new BorderLayout());
        w.setWindowSize(400, 300);

        // initNamedGroup() stored the ButtonGroup as a client property on the form and
        // dereferenced it without a guard, so showing a window containing a grouped
        // radio button threw before the native window was even mapped.
        Container box = new Container(new com.codename1.ui.layouts.BoxLayout(
                com.codename1.ui.layouts.BoxLayout.Y_AXIS));
        RadioButton first = new RadioButton("first");
        RadioButton second = new RadioButton("second");
        first.setGroup("choice");
        second.setGroup("choice");
        box.add(first);
        box.add(second);
        w.add(BorderLayout.CENTER, box);
        w.show();
        w.revalidate();

        assertTrue(w.isWindowShowing(),
                "a window with a grouped radio button must show");

        // The group has to actually work, not merely not throw: selecting the second
        // must clear the first, which only happens if both joined the same group.
        first.setSelected(true);
        second.setSelected(true);
        assertTrue(second.isSelected());
        assertFalse(first.isSelected(),
                "both radio buttons must have joined the same named group");

        w.dispose();
    }

    @FormTest
    void aWindowTaggedPressReachesAnEditingTextAreaWithoutThrowing() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("editing", new BorderLayout());
        w.setWindowSize(400, 300);
        TextArea area = new TextArea("text");
        Button other = new Button("elsewhere");
        w.add(BorderLayout.NORTH, area);
        w.add(BorderLayout.SOUTH, other);
        w.show();
        w.revalidate();

        final boolean[] fired = new boolean[1];
        area.addActionListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                fired[0] = true;
            }
        });

        // The press listener is registered on the window, but its body resolved
        // getComponentForm() -- null there -- and did nothing. So the documented
        // pre-click action event never fired and the other component's handler could
        // observe an uncommitted value.
        implementation.setFocusedEditingText(area);
        assertTrue(area.isEditing(), "the area should be in editing state");

        // Tagged with the window's id: the untagged entry point is window 0, the main
        // surface, and the press would never reach this window at all.
        Display.getInstance().windowPointerPressed(w.getWindowId(),
                new int[]{other.getAbsoluteX() + 2},
                new int[]{other.getAbsoluteY() + 2});
        flushSerialCalls();

        // Deliberately not asserting that *this* listener fired the early event. The
        // press path fires an action event and sets suppressActionEvent by another
        // route as well, so both assertions pass against the un-fixed listener and
        // would prove nothing. What this does pin down is that a window-tagged press
        // reaches an editing text area in a window and is handled without throwing;
        // the listener body's own fix is covered by reading, and stated as such.
        assertTrue(fired[0], "the press must be handled and its action event delivered");
        assertTrue(w.isWindowShowing(), "and must not throw on the way");
        w.dispose();
    }

    @FormTest
    void editingATextFieldInsideAWindowReachesThePort() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("editor", new BorderLayout());
        w.setWindowSize(400, 300);
        TextField field = new TextField("hello");
        w.add(BorderLayout.CENTER, field);
        w.show();
        w.revalidate();

        // Display.editString() resolved getComponentForm() and returned outright when
        // it was null -- which it always is inside a Window. That guard rejected every
        // editor in a window before impl.editStringImpl() was reached, so none of the
        // port level editor routing could run however correct the ports were. The
        // windowed screenshot goldens could not see it either: a field that never
        // enters editing still renders.
        Display.getInstance().editString(field, 20, TextArea.ANY, "hello", 0);

        java.lang.reflect.Field active = com.codename1.testing.TestCodenameOneImplementation
                .class.getDeclaredField("activeTextEditor");
        active.setAccessible(true);
        assertSame(field, active.get(implementation),
                "editing a text field in a window must reach the port");

        w.dispose();
    }

    @FormTest
    void commandsAddedToAWindowReachThePort() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("commands", new BorderLayout());
        w.setWindowSize(300, 200);

        // Added before show(): the peer does not exist yet, so these have to be
        // published when it does rather than silently lost.
        Command before = new Command("Before");
        w.addCommand(before);
        w.show();

        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertNotNull(peer);

        // The command list used to be private bookkeeping that nothing consumed, so a
        // command added to a window was never displayed and never activated -- unlike
        // the identical call on a Form.
        assertEquals(1, wm.getPublishedCommands(peer).size(),
                "commands added before show() must reach the port once it exists");
        assertSame(before, wm.getPublishedCommands(peer).get(0));

        Command after = new Command("After");
        w.addCommand(after);
        assertEquals(2, wm.getPublishedCommands(peer).size(),
                "and a command added afterwards must be published too");

        w.removeCommand(before);
        assertEquals(1, wm.getPublishedCommands(peer).size());
        assertSame(after, wm.getPublishedCommands(peer).get(0));

        w.removeAllCommands();
        assertEquals(0, wm.getPublishedCommands(peer).size());

        w.dispose();
    }

    @FormTest
    void anAnimationInBothRegistriesRunsOncePerFrame() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("anim", new BorderLayout());
        w.setWindowSize(300, 200);
        w.show();

        final int[] ticks = new int[1];
        com.codename1.ui.animations.Animation a =
                new com.codename1.ui.animations.Animation() {
                    @Override
                    public boolean animate() {
                        ticks[0]++;
                        return false;
                    }

                    @Override
                    public void paint(com.codename1.ui.Graphics g) {
                    }
                };

        // A component can legitimately sit in both registries -- an explicitly animated
        // scrollable whose fading scrollbar is also running. Form skips entries already
        // handled by the public list; without the same exclusion the motion advances at
        // double speed and any side effect happens twice per frame.
        w.registerAnimated(a);
        w.registerAnimatedInternal(a);

        w.repaintAnimations();
        assertEquals(1, ticks[0],
                "an animation in both registries must run once per frame, not twice");

        w.dispose();
    }

    @FormTest
    void emblemValidationInstallsItsGlassPaneInsideAWindow() throws Exception {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("validate", new BorderLayout());
        w.setWindowSize(300, 200);
        TextField field = new TextField("");
        w.add(BorderLayout.CENTER, field);
        w.show();
        w.revalidate();
        assertNull(w.getGlassPane(), "no glass pane before validation runs");

        // setValid() is driven directly rather than through addConstraint: the full
        // constraint path pulls in listener wiring that wedges the event dispatch
        // thread in this harness, and the glass pane installation is what is under
        // test. It is package private, hence the reflective call.
        com.codename1.ui.validation.Validator v = new com.codename1.ui.validation.Validator();
        v.setValidationFailureHighlightMode(
                com.codename1.ui.validation.Validator.HighlightMode.EMBLEM);
        java.lang.reflect.Method setValid = com.codename1.ui.validation.Validator.class
                .getDeclaredMethod("setValid", Component.class, boolean.class);
        setValid.setAccessible(true);
        setValid.invoke(v, field, false);

        // The emblem is drawn by a glass pane, and the guard that installed it resolved
        // the form -- null inside a Window -- so EMBLEM validation showed nothing there.
        assertNotNull(w.getGlassPane(),
                "emblem validation must install its glass pane on the window");

        w.dispose();
    }

    @FormTest
    void aBlockedWindowStillRefusesItsCloseRequest() {
        implementation.setMultiWindowSupported(true);
        Window owner = new Window("owner", new BorderLayout());
        owner.setWindowSize(400, 300);
        owner.show();

        final int[] closes = new int[1];
        owner.addCloseListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                closes[0]++;
            }
        });

        Window modal = new Window("modal", new BorderLayout());
        modal.setWindowSize(200, 150);
        modal.setModalityType(Window.MODALITY_APPLICATION);
        modal.show();
        flushSerialCalls();

        // A close arrives outside the packed input queue, so it bypasses the modality
        // filter that guards every other event. The check moved onto the event dispatch
        // thread -- the modal stack is mutated there, so reading it from the port's
        // callback thread raced -- and this asserts the move kept the behaviour.
        Display.getInstance().windowCloseRequested(owner.getWindowId());
        flushSerialCalls();
        assertEquals(0, closes[0],
                "a window blocked by an application modal must not close");

        modal.dispose();
        flushSerialCalls();

        Display.getInstance().windowCloseRequested(owner.getWindowId());
        flushSerialCalls();
        assertEquals(1, closes[0],
                "and must close again once the modal is gone");

        owner.dispose();
    }

    @FormTest
    void aCommandBackedButtonNotifiesTheWindowsCommandListeners() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("cmd button", new BorderLayout());
        w.setWindowSize(300, 200);

        final int[] commandRuns = new int[1];
        Command cmd = new Command("Go") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                commandRuns[0]++;
            }
        };
        final int[] listenerSaw = new int[1];
        w.addCommandListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                listenerSaw[0]++;
            }
        });

        Button b = new Button(cmd);
        w.add(BorderLayout.CENTER, b);
        w.show();
        w.revalidate();

        // Button.fireActionEvent forwarded the post-command event through the form,
        // null in a window, so the window's command listeners never saw the activation.
        b.pressed();
        b.released();
        flushSerialCalls();

        assertEquals(1, commandRuns[0], "the command runs exactly once");
        assertEquals(1, listenerSaw[0],
                "and the window's command listeners must be notified once");

        w.dispose();
    }

    @FormTest
    void enablingTextSelectionInsideAWindowDoesNotThrow() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("selection", new BorderLayout());
        w.setWindowSize(300, 200);
        TextArea area = new TextArea("selectable");
        w.add(BorderLayout.CENTER, area);
        w.show();
        w.revalidate();

        // TextSelection is exposed on every TopLevelContainer, but setEnabled resolved
        // the root's form and dereferenced the null result, so enabling it threw in
        // every secondary window.
        TextSelection sel = w.getTextSelection();
        assertNotNull(sel);
        sel.setEnabled(true);
        assertTrue(sel.isEnabled(), "text selection must enable inside a window");

        sel.setEnabled(false);
        assertFalse(sel.isEnabled());

        w.dispose();
    }

    @FormTest
    void showingTheToolbarSearchBarInsideAWindowSwapsTheToolbar() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("search", new BorderLayout());
        w.setWindowSize(400, 300);
        Toolbar tb = new Toolbar();
        w.setToolbar(tb);
        w.show();
        w.revalidate();
        assertSame(tb, w.getToolbar());

        // showSearchBar() assigned getComponentForm() and immediately called
        // removeComponentFromForm on it, so activating the search command in a window
        // threw. A Window installs its toolbar in the title area, so the outgoing one
        // needs no separate detach -- only a Form does.
        tb.showSearchBar(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
            }
        });
        flushSerialCalls();

        assertNotSame(tb, w.getToolbar(),
                "showing the search bar must swap the window's toolbar for it");
        assertNotNull(w.getToolbar());

        w.dispose();
    }

    @FormTest
    void aTitleSetAfterAToolbarIsInstalledReachesIt() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("first", new BorderLayout());
        w.setWindowSize(400, 300);
        Toolbar tb = new Toolbar();
        w.setToolbar(tb);
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();

        // Once a toolbar is installed it draws the title, and the label setTitle used
        // to update is no longer in the hierarchy -- so the change was invisible.
        w.setTitle("second");

        Component shown = tb.getTitleComponent();
        assertTrue(shown instanceof Label, "the toolbar shows its title in a label");
        assertEquals("second", ((Label) shown).getText(),
                "an installed toolbar must show the window's new title");
        assertEquals("second", w.getTitle(),
                "and getTitle must read it back from the toolbar");
        assertEquals("second", peer.getTitle(),
                "while the native window title still follows too");

        w.dispose();
    }

    @FormTest
    void centeringOnAFormUsesTheMainWindowNotTheWorkArea() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        // The main window deliberately does not fill the work area, which is the case
        // that separates the two behaviours.
        wm.setMainWindowBounds(200, 100, 600, 400);

        Window w = new Window("centred", new BorderLayout());
        w.setWindowSize(200, 100);
        w.show();

        // A Form lives in the application's main native window, so centring over a Form
        // has to centre over that window. Falling through to centerOnDesktop() centred
        // on the monitor work area instead -- a different place whenever the main
        // window has been moved or does not fill the screen.
        w.centerOn(Display.getInstance().getCurrent());

        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertEquals(200 + (600 - 200) / 2, peer.getX(),
                "centred horizontally over the main window");
        assertEquals(100 + (400 - 100) / 2, peer.getY(),
                "centred vertically over the main window");

        w.dispose();
    }

    @FormTest
    void aCommandListInsideAWindowNotifiesItsCommandListeners() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("cmd list", new BorderLayout());
        w.setWindowSize(300, 200);

        final int[] commandRuns = new int[1];
        Command cmd = new Command("Pick") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                commandRuns[0]++;
            }
        };
        final int[] listenerSaw = new int[1];
        w.addCommandListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                listenerSaw[0]++;
            }
        });

        List<Command> list = new List<Command>(new Command[]{cmd});
        list.setCommandList(true);
        w.add(BorderLayout.CENTER, list);
        w.show();
        w.revalidate();

        // List.fireActionEvent invoked the command and then dispatched the follow-up
        // through the form, null in a window, so the window's command listeners never
        // saw the activation.
        list.setSelectedIndex(0);
        list.fireActionEvent();
        flushSerialCalls();

        assertEquals(1, commandRuns[0], "the command runs exactly once");
        assertEquals(1, listenerSaw[0],
                "and the window's command listeners must be notified once");

        w.dispose();
    }

    @FormTest
    void installingAToolbarKeepsTheWindowsExistingTitle() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("original", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();

        // getTitle() answers from the installed toolbar once there is one, so reading
        // it after this.toolbar has been replaced returns the incoming toolbar's empty
        // title -- which was then handed straight back to it, blanking the visible
        // title while the native window title still showed the real one.
        Toolbar first = new Toolbar();
        w.setToolbar(first);
        assertEquals("original", titleTextOf(first),
                "installing a toolbar must keep the window's existing title");
        assertEquals("original", w.getTitle());
        assertEquals("original", peer.getTitle(),
                "and the native title is unchanged");

        // Replacing one toolbar with another carries the title across too -- the same
        // read-before-assign, one step further on.
        w.setTitle("renamed");
        Toolbar second = new Toolbar();
        w.setToolbar(second);
        assertEquals("renamed", titleTextOf(second),
                "replacing a toolbar must carry the current title across");
        assertEquals("renamed", w.getTitle());

        w.dispose();
    }

    /// The text a toolbar is currently showing as its title, or null.
    private static String titleTextOf(Toolbar tb) {
        Component cmp = tb.getTitleComponent();
        return cmp instanceof Label ? ((Label) cmp).getText() : null;
    }

    /// The first day cell in a calendar's month view.
    /// Runs the animation manager until nothing is in progress and its post-animation
    /// queue has drained, which is where work handed to `AnimationManager#flushAnimation`
    /// during a layout animation ends up.
    private void pumpAnimations(Window w) {
        AnimationManager mgr = w.getAnimationManager();
        long deadline = System.currentTimeMillis() + 5000;
        while (mgr.isAnimating() && System.currentTimeMillis() < deadline) {
            mgr.updateAnimations();
            flushSerialCalls();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // updateAnimations() drains the post-animation queue only on a pass that finds
        // the animation list already empty, and the pass that empties it is not that
        // pass -- so a single trailing call is one short.
        for (int iter = 0; iter < 5; iter++) {
            mgr.updateAnimations();
            flushSerialCalls();
        }
    }

    /// Finds the button a `Command` was rendered into, anywhere under the top level.
    private static Button findButtonForCommand(Container c, Command cmd) {
        for (int iter = 0; iter < c.getComponentCount(); iter++) {
            Component cmp = c.getComponentAt(iter);
            if (cmp instanceof Button && ((Button) cmp).getCommand() == cmd) {
                return (Button) cmp;
            }
            if (cmp instanceof Container) {
                Button b = findButtonForCommand((Container) cmp, cmd);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    /// Finds the first button carrying a `Command`, which is how the test reaches a
    /// toolbar's back arrow without a public accessor for it.
    private static Button findFirstCommandButton(Container c) {
        for (int iter = 0; iter < c.getComponentCount(); iter++) {
            Component cmp = c.getComponentAt(iter);
            if (cmp instanceof Button && ((Button) cmp).getCommand() != null) {
                return (Button) cmp;
            }
            if (cmp instanceof Container) {
                Button b = findFirstCommandButton((Container) cmp);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    private static Button findFirstDayButton(Container c) {
        for (int iter = 0; iter < c.getComponentCount(); iter++) {
            Component cmp = c.getComponentAt(iter);
            if (cmp instanceof Button && ((Button) cmp).getText().length() > 0
                    && Character.isDigit(((Button) cmp).getText().charAt(0))) {
                return (Button) cmp;
            }
            if (cmp instanceof Container) {
                Button b = findFirstDayButton((Container) cmp);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    /// An image that reports itself as an animation, which is what drives the
    /// registration path under test.
    private static Image makeAnimatedImage() {
        return new Image(null) {
            @Override
            public boolean isAnimation() {
                return true;
            }

            @Override
            public boolean animate() {
                return false;
            }

            @Override
            public int getWidth() {
                return 8;
            }

            @Override
            public int getHeight() {
                return 8;
            }
        };
    }

    @FormTest
    void legacyPullToRefreshRegistersItsAnimationOnTheWindow() {
        implementation.setMultiWindowSupported(true);
        final AtomicInteger registered = new AtomicInteger();
        Window w = new Window("pull", new BorderLayout()) {
            @Override
            public void registerAnimated(Animation cmp) {
                registered.incrementAndGet();
                super.registerAnimated(cmp);
            }
        };
        Container scrollable = new Container(new BorderLayout());
        scrollable.setScrollableY(true);
        w.add(BorderLayout.CENTER, scrollable);
        w.show();

        LookAndFeel laf = UIManager.getInstance().getLookAndFeel();
        assertTrue(laf instanceof DefaultLookAndFeel,
                "This test drives the default look and feel's legacy pull-to-refresh path");
        try {
            // Also initializes the pull container the legacy path draws through.
            int threshold = laf.getPullToRefreshHeight();
            Graphics g = Image.createImage(60, 60).getGraphics();

            // First pass swaps the "pull down to refresh" label into the container.
            laf.drawPullToRefresh(g, scrollable, false);
            // Crossing the threshold swaps in "release to refresh", and that swap is
            // what registers the icon rotation animation on the top level. Before the
            // migration this went through getComponentForm(), which is null in a
            // Window, so the gesture threw on the EDT instead of animating.
            scrollable.setScrollY(-(threshold + 1));
            laf.drawPullToRefresh(g, scrollable, false);

            assertTrue(registered.get() > 0,
                    "Pull-to-refresh must register its animation on the Window that hosts it");
        } finally {
            // The look and feel keeps the pull container in a field shared by every
            // test in this JVM, so a failure here must still tear the window down or
            // it cascades into unrelated tests.
            w.dispose();
        }
    }

    @FormTest
    void dismissingTheSearchBarInsideAWindowRestoresTheToolbar() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("search", new BorderLayout());
        w.setWindowSize(400, 300);
        Toolbar tb = new Toolbar();
        w.setToolbar(tb);
        w.show();
        w.revalidate();

        tb.showSearchBar(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
            }
        });
        flushSerialCalls();
        Toolbar search = w.getToolbar();
        assertNotSame(tb, search, "the search bar should be installed at this point");

        try {
            // The search bar's back command resolved its host by casting getParent()
            // to Form. Inside a Window the toolbar hangs off the title area, so that
            // named the wrong type -- and because ParparVM does not check CHECKCAST,
            // on Mac Catalyst it was a native crash rather than a catchable one.
            Button back = findFirstCommandButton(search);
            assertNotNull(back, "the search bar installs a back command");
            back.getCommand().actionPerformed(
                    new com.codename1.ui.events.ActionEvent(back.getCommand()));
            flushSerialCalls();
            // Showing the search bar animates the layout, so the restore is queued
            // behind that animation rather than running inline.
            pumpAnimations(w);

            assertSame(tb, w.getToolbar(),
                    "dismissing the search bar must put the original toolbar back");
            assertFalse(tb.isHidden(),
                    "the restored toolbar must be visible again");
        } finally {
            w.dispose();
        }
    }


    @FormTest
    void addingAPermanentSideMenuCommandInsideAWindowWorks() {
        implementation.setMultiWindowSupported(true);
        boolean oldPermanent = Toolbar.isPermanentSideMenu();
        Toolbar.setPermanentSideMenu(true);
        Window w = new Window("menu", new BorderLayout());
        try {
            w.setWindowSize(400, 300);
            Toolbar tb = new Toolbar();
            w.setToolbar(tb);
            w.show();
            w.revalidate();

            // markInstalledOnWindow raises the initialized flag, so this call gets past
            // checkIfInitialized -- and then constructPermanentSideMenu assigned
            // getComponentForm() to a local and dereferenced it, which is null here.
            Command item = new Command("Item");
            tb.addCommandToLeftSideMenu(item);
            w.revalidate();

            assertNotNull(findButtonForCommand(w, item),
                    "the side menu command must end up in the window's own hierarchy");
        } finally {
            Toolbar.setPermanentSideMenu(oldPermanent);
            w.dispose();
        }
    }

    @FormTest
    void sideMenuCommandsOfAWindowToolbarReadTheWindowsCommands() {
        implementation.setMultiWindowSupported(true);
        boolean oldPermanent = Toolbar.isPermanentSideMenu();
        boolean oldOnTop = Toolbar.isOnTopSideMenu();
        Toolbar.setPermanentSideMenu(false);
        Toolbar.setOnTopSideMenu(false);
        Window w = new Window("menu", new BorderLayout());
        try {
            w.setWindowSize(400, 300);
            Toolbar tb = new Toolbar();
            w.setToolbar(tb);
            w.show();
            Command item = new Command("Item");
            w.addCommand(item);

            // Without a side menu container this read the command list off
            // getComponentForm() with no null check at all.
            ArrayList<Command> found = new ArrayList<Command>();
            for (Command c : tb.getSideMenuCommands()) {
                found.add(c);
            }
            assertTrue(found.contains(item),
                    "a window toolbar's side menu commands are the window's commands");
        } finally {
            Toolbar.setPermanentSideMenu(oldPermanent);
            Toolbar.setOnTopSideMenu(oldOnTop);
            w.dispose();
        }
    }

    @FormTest
    void movingAShownWindowInvalidatesItsCachedMonitor() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        java.util.List<TestWindowManager.FakeMonitor> two =
                new ArrayList<TestWindowManager.FakeMonitor>();
        two.add(new TestWindowManager.FakeMonitor(0, 0, 1440, 900, 1.0, 96, "primary"));
        two.add(new TestWindowManager.FakeMonitor(1440, 0, 2560, 1440, 2.0, 192, "second"));
        wm.setMonitors(two);

        Window w = new Window("mover", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertNotNull(peer);

        // Populate the cache while the window is still on the primary display.
        assertEquals(0, w.getMonitor().getIndex());
        assertEquals(1.0, w.getScale(), 0.0001);

        // Now it sits on the second display. A real port reports this through the
        // monitor-change callback, which is queued back to the event dispatch thread
        // -- so a move followed by a query in the same turn has to answer correctly
        // without it, or centerOnDesktop() sends the window back where it came from.
        peer.setMonitor(1);
        w.setWindowLocation(1500, 100);

        assertEquals(1, w.getMonitor().getIndex(),
                "a move must invalidate the cached monitor");
        assertEquals(2.0, w.getScale(), 0.0001,
                "and the scale that is answered from it");
        w.dispose();
    }

    @FormTest
    void aWindowMoveBetweenMonitorsIsNotADesktopTopologyEvent() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        java.util.List<TestWindowManager.FakeMonitor> two =
                new ArrayList<TestWindowManager.FakeMonitor>();
        two.add(new TestWindowManager.FakeMonitor(0, 0, 1440, 900, 1.0, 96, "primary"));
        two.add(new TestWindowManager.FakeMonitor(1440, 0, 2560, 1440, 2.0, 192, "second"));
        wm.setMonitors(two);

        final int[] fired = new int[1];
        Desktop.getInstance().addMonitorListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        });

        Window w = new Window("dragged", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertNotNull(peer);
        assertEquals(0, w.getMonitor().getIndex());

        // Dragging one window onto another display is not a change of topology.
        // addMonitorListener is documented for a monitor being attached, removed or
        // reconfigured, so firing it here made every move across a mixed-DPI desktop
        // re-run whatever display reconfiguration work the application does.
        try {
            peer.setMonitor(1);
            Display.getInstance().windowMonitorChanged(w.getWindowId());
            DisplayTest.flushEdt();

            assertEquals(0, fired[0],
                    "moving a window between monitors must not notify monitor listeners");
            assertEquals(1, w.getMonitor().getIndex(),
                    "but the window itself must follow the display it is now on");
            assertEquals(2.0, w.getScale(), 0.0001);

            // A real topology change still notifies.
            Display.getInstance().monitorsChanged();
            DisplayTest.flushEdt();
            assertEquals(1, fired[0], "an attach, removal or reconfiguration still notifies");
        } finally {
            // A window left undisposed by a failing assertion keeps the event dispatch
            // thread busy and times out whatever runs next, which buries the real
            // failure under an unrelated one.
            w.dispose();
        }
    }

    @FormTest
    void terminalEventsReportTheGeometryTheWindowActuallyHad() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("mover", new BorderLayout());
        w.setWindowBounds(new Rectangle(10, 20, 400, 300));
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertNotNull(peer);

        final java.util.List<Rectangle> disposedBounds = new ArrayList<Rectangle>();
        w.addWindowListener(new ActionListener<WindowEvent>() {
            @Override
            public void actionPerformed(WindowEvent evt) {
                if (evt.getType() == WindowEvent.Type.Disposed) {
                    disposedBounds.add(evt.getBounds());
                }
            }
        });

        // The user drags and resizes it natively. Nothing in the application asked for
        // this geometry, so it lives only in the peer.
        wm.setBounds(peer, 640, 480, 900, 700);
        w.dispose();

        assertEquals(1, disposedBounds.size(), "disposal must report exactly once");
        Rectangle r = disposedBounds.get(0);
        // Before the snapshot, disposal nulled the peer first and getWindowBounds()
        // fell back to the originally requested rectangle -- so a listener persisting
        // geometry across runs restored the window to where it was never left.
        assertEquals(640, r.getX(), "the final native position, not the requested one");
        assertEquals(480, r.getY());
        assertEquals(900, r.getWidth());
        assertEquals(700, r.getHeight());
    }

    @FormTest
    void anInteractionDialogShowsOnTheWindowItWasGiven() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        w.revalidate();

        com.codename1.components.InteractionDialog dlg =
                new com.codename1.components.InteractionDialog("in the window");
        dlg.setAnimateShow(false);
        try {
            // Without a host the dialog resolves Display.getCurrent() and lands in the
            // main form's layered pane -- so it appears on the main window while the
            // window that asked for it is merely dimmed.
            dlg.setTopLevelHost(w);
            dlg.show(10, 10, 10, 10);
            flushSerialCalls();

            assertSame(w, dlg.getTopLevelContainer(),
                    "the dialog must be attached to the window it was given");
            assertNull(dlg.getComponentForm(),
                    "and therefore to no form at all");
        } finally {
            dlg.dispose();
            w.dispose();
        }
    }

    @FormTest
    void anInteractionDialogWithNoHostStillUsesTheCurrentForm() {
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        com.codename1.components.InteractionDialog dlg =
                new com.codename1.components.InteractionDialog("on the form");
        dlg.setAnimateShow(false);
        try {
            // The historical behaviour, which every single-window application relies on.
            dlg.show(10, 10, 10, 10);
            flushSerialCalls();
            assertSame(main, dlg.getComponentForm(),
                    "an unhosted dialog must still land on the current form");
        } finally {
            dlg.dispose();
        }
    }

    @FormTest
    void sideMenuGeometryComesFromTheHostWindowNotTheDisplay() throws Exception {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        Toolbar tb = new Toolbar();
        w.setToolbar(tb);
        w.show();
        w.revalidate();

        Toolbar formToolbar = new Toolbar();
        main.setToolbar(formToolbar);
        main.revalidate();
        try {
            int displayWidth = Display.getInstance().getDisplayWidth();
            int displayHeight = Display.getInstance().getDisplayHeight();
            assertNotEquals(displayHeight, w.getHeight(),
                    "the window and the display have to differ or this proves nothing");

            // The side menu covers the surface its toolbar sits on, and every gesture
            // threshold is measured against that surface. Taken from Display, a
            // right-edge swipe in a narrow window was compared with the display's right
            // edge and could never activate, and landscape margins could exceed the
            // host width.
            assertEquals(w.getWidth(), invokeHostGeometry(tb, "hostWidth"),
                    "a toolbar in a window measures the window");
            assertEquals(w.getHeight(), invokeHostGeometry(tb, "hostHeight"));

            // The Form path has to stay exactly as it was.
            assertEquals(displayWidth, invokeHostGeometry(formToolbar, "hostWidth"),
                    "a toolbar in a form still measures the display");
            assertEquals(displayHeight, invokeHostGeometry(formToolbar, "hostHeight"));
        } finally {
            w.dispose();
        }
    }

    private static int invokeHostGeometry(Toolbar tb, String method) throws Exception {
        java.lang.reflect.Method m = Toolbar.class.getDeclaredMethod(method);
        m.setAccessible(true);
        return ((Integer) m.invoke(tb)).intValue();
    }

    /// The side menu's dialog, found by walking the window rather than through an
    /// accessor the toolbar does not expose.
    private static com.codename1.components.InteractionDialog findSideMenuDialog(Container c) {
        for (int iter = 0; iter < c.getComponentCount(); iter++) {
            Component cmp = c.getComponentAt(iter);
            if (cmp instanceof com.codename1.components.InteractionDialog) {
                return (com.codename1.components.InteractionDialog) cmp;
            }
            if (cmp instanceof Container) {
                com.codename1.components.InteractionDialog inner =
                        findSideMenuDialog((Container) cmp);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    @FormTest
    void aPopupForAWindowComponentOpensInThatWindow() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        Button anchor = new Button("anchor");
        w.add(BorderLayout.CENTER, anchor);
        w.show();
        w.revalidate();

        com.codename1.components.InteractionDialog dlg =
                new com.codename1.components.InteractionDialog("popup");
        dlg.setAnimateShow(false);
        try {
            // The popup is anchored to a component in the window, and its rectangle is
            // in that window's coordinate space. Resolving the current form instead
            // opened it over the main window at coordinates that mean nothing there.
            dlg.showPopupDialog(anchor);
            flushSerialCalls();

            assertSame(w, dlg.getTopLevelContainer(),
                    "a popup anchored in a window must open in that window");
            assertNull(dlg.getComponentForm());
        } finally {
            dlg.dispose();
            w.dispose();
        }
    }

    @FormTest
    void aModalityChangeMadeWhileMinimizedSurvivesRestoration() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        Window w = new Window("modal", new BorderLayout());
        w.setWindowSize(400, 300);
        w.setModalityType(Window.MODALITY_APPLICATION);
        w.show();
        flushSerialCalls();
        try {
            assertTrue(Display.getInstance().isWindowInputBlocked(0),
                    "an application modal window blocks the main window");

            // The platform minimizes it. A minimized window is still open and still
            // modal -- isModalFinished() says so itself.
            w.hideNotify();
            assertTrue(Display.getInstance().isWindowInputBlocked(0),
                    "minimizing a modal window does not end the modal");

            // Changing modality here released the old blocker and declined to take the
            // new one, and showNotify() never reacquires, so the window came back
            // visibly non-modal while getModalityType() still said otherwise.
            w.setModalityType(Window.MODALITY_APPLICATION);
            assertEquals(Window.MODALITY_APPLICATION, w.getModalityType());
            assertTrue(Display.getInstance().isWindowInputBlocked(0),
                    "a modality change while minimized must keep the window modal");

            w.showNotify();
            assertTrue(Display.getInstance().isWindowInputBlocked(0),
                    "and it must still be modal once restored");
        } finally {
            w.dispose();
        }
    }

    @FormTest
    void aPickerInsideAWindowOpensItsPopupThere() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        com.codename1.ui.spinner.Picker picker = new com.codename1.ui.spinner.Picker();
        picker.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        picker.setStrings("one", "two", "three");
        picker.setSelectedString("one");
        w.add(BorderLayout.CENTER, picker);
        w.show();
        w.revalidate();
        try {
            // The lightweight popup is the default wherever the platform has no native
            // picker, which is every desktop port. This threw "Attempt to show
            // interaction dialog while button is not on form" because it insisted on a
            // Form, making a standard component unusable in every secondary window.
            picker.pressed();
            picker.released();
            flushSerialCalls();

            com.codename1.components.InteractionDialog popup = findDialogIn(w);
            assertNotNull(popup, "the picker's popup must open inside the window");
        } finally {
            w.dispose();
        }
    }

    /// The first InteractionDialog anywhere under the given container.
    private static com.codename1.components.InteractionDialog findDialogIn(Container c) {
        for (int iter = 0; iter < c.getComponentCount(); iter++) {
            Component cmp = c.getComponentAt(iter);
            if (cmp instanceof com.codename1.components.InteractionDialog) {
                return (com.codename1.components.InteractionDialog) cmp;
            }
            if (cmp instanceof Container) {
                com.codename1.components.InteractionDialog inner = findDialogIn((Container) cmp);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    @FormTest
    void anOpenPickerInAWindowIsEditableAndCanBeStopped() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        com.codename1.ui.spinner.Picker picker = new com.codename1.ui.spinner.Picker();
        picker.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        picker.setStrings("one", "two", "three");
        picker.setSelectedString("one");
        w.add(BorderLayout.CENTER, picker);
        w.show();
        w.revalidate();
        try {
            picker.pressed();
            picker.released();
            flushSerialCalls();

            // registerAsInputDevice resolved a Form and so skipped every registration
            // inside a window: the popup opened but reported isEditing() false, which
            // is what window-level input-device replacement and stopEditing() both go
            // through -- so nothing could dismiss it.
            assertTrue(picker.isEditing(),
                    "an open picker in a window must report itself as editing");
            assertNotNull(w.getCurrentInputDevice(),
                    "and must register as the window's current input device");

            final boolean[] stopped = new boolean[1];
            picker.stopEditing(new Runnable() {
                @Override
                public void run() {
                    stopped[0] = true;
                }
            });
            flushSerialCalls();
            // The popup closes with a dispose animation, so the callback is queued
            // behind it rather than running inline.
            pumpAnimations(w);
            assertTrue(stopped[0], "stopEditing must close it and run the callback");
        } finally {
            w.dispose();
        }
    }

    @FormTest
    void aValidationErrorPopupAppearsInsideItsWindow() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        TextField field = new TextField("");
        w.add(BorderLayout.CENTER, field);
        w.show();
        w.revalidate();
        try {
            com.codename1.ui.validation.Validator v =
                    new com.codename1.ui.validation.Validator();
            v.setShowErrorMessageForFocusedComponent(true);
            v.addConstraint(field,
                    new com.codename1.ui.validation.LengthConstraint(3, "too short"));
            assertFalse(v.isValid(), "an empty field must fail the length constraint");

            // Driven the way the framework drives it. Going through setFocused() does
            // not work here: showing the window already focused its only focusable
            // child, so setFocused() short-circuits and nothing fires -- a test built
            // that way passes whatever the code does.
            //
            // The listener compared getComponentForm() with the current Form, and in a
            // window that is null against a non-null form, so it returned every time
            // and the configured popup never appeared.
            field.fireFocusGained();
            flushSerialCalls();
            pumpAnimations(w);

            assertNotNull(findDialogIn(w),
                    "the validation error popup must appear inside the window");
        } finally {
            w.dispose();
        }
    }

    @FormTest
    void geometryChangedOffTheEdtIsMarshalled() throws Exception {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        final Window w = new Window("marshalled", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertNotNull(peer);

        try {
            // The developer guide promises that moving a window from a background
            // thread is marshalled the way Form.show() is. Unmarshalled, this mutated
            // the pending geometry and the cached monitor while the event dispatch
            // thread was reading them, and drove the window manager concurrently with
            // the callbacks reporting the result.
            final boolean[] onEdt = new boolean[]{true};
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    onEdt[0] = Display.getInstance().isEdt();
                    w.setWindowLocation(120, 90);
                    w.setWindowSize(640, 480);
                }
            });
            t.start();
            t.join(5000);
            assertFalse(onEdt[0], "the mutation has to be made off the EDT to prove anything");

            // The point of the fix is that the work is *deferred*, so that is what is
            // asserted. Checking only the end state proves nothing: an unmarshalled
            // mutation reaches the same numbers, just on the wrong thread.
            Rectangle before = w.getWindowBounds();
            assertEquals(400, before.getWidth(),
                    "the background call must not have touched the window yet");
            assertEquals(300, before.getHeight());

            flushSerialCalls();

            Rectangle b = w.getWindowBounds();
            assertEquals(120, b.getX(), "the queued move must have been applied");
            assertEquals(90, b.getY());
            assertEquals(640, b.getWidth(), "and the queued resize with it");
            assertEquals(480, b.getHeight());
        } finally {
            w.dispose();
        }
    }

    @FormTest
    void aBackgroundResizeThenMoveKeepsBothChanges() throws Exception {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        final Window w = new Window("resize then move", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        assertNotNull(wm.getLastWindow());

        try {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    w.setWindowSize(800, 600);
                    w.setWindowLocation(10, 10);
                }
            });
            t.start();
            t.join(5000);
            flushSerialCalls();

            // setWindowLocation used to read the bounds before entering the event
            // dispatch thread, so it queued a full rectangle carrying the size from
            // *before* the queued resize -- the resize was applied and then silently
            // undone by the move.
            Rectangle b = w.getWindowBounds();
            assertEquals(10, b.getX());
            assertEquals(10, b.getY());
            assertEquals(800, b.getWidth(),
                    "the move must not carry a size read before the queued resize");
            assertEquals(600, b.getHeight());
        } finally {
            w.dispose();
        }
    }

    @FormTest
    void windowsCreatedConcurrentlyGetDistinctIds() throws Exception {
        implementation.setMultiWindowSupported(true);
        final int threads = 8;
        final int perThread = 25;
        final java.util.List<Integer> ids =
                java.util.Collections.synchronizedList(new ArrayList<Integer>());
        final java.util.List<Window> made =
                java.util.Collections.synchronizedList(new ArrayList<Window>());
        Thread[] workers = new Thread[threads];
        for (int iter = 0; iter < threads; iter++) {
            workers[iter] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < perThread; i++) {
                        Window w = new Window("concurrent");
                        made.add(w);
                        ids.add(Integer.valueOf(w.getWindowId()));
                    }
                }
            });
        }
        try {
            for (Thread t : workers) {
                t.start();
            }
            for (Thread t : workers) {
                t.join(10000);
            }

            // A constructor cannot be marshalled, so two background threads really do
            // allocate ids concurrently. A collision gives two native windows one id,
            // and windowById() returns the first match -- so every input and lifecycle
            // callback for the second would land on the first.
            assertEquals(threads * perThread, ids.size(), "every window must be built");
            java.util.Set<Integer> unique = new java.util.HashSet<Integer>(ids);
            assertEquals(ids.size(), unique.size(), "window ids must be unique");
        } finally {
            for (Window w : made) {
                w.dispose();
            }
        }
    }

    @FormTest
    void heldInputTimersStopAtAWindowThatBecameBlocked() throws Exception {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        flushSerialCalls();

        Window w = new Window("target", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        flushSerialCalls();

        java.lang.reflect.Method target = Display.class.getDeclaredMethod(
                "repeatTarget", int.class, Form.class);
        target.setAccessible(true);
        Window modal = new Window("modal", new BorderLayout());
        modal.setWindowSize(200, 150);
        try {
            assertSame(w, target.invoke(Display.getInstance(),
                    Integer.valueOf(w.getWindowId()), main),
                    "an unblocked window is a valid repeat target");

            // A handler can open an application modal from the very press that is
            // still being held. These timers are armed when the press is accepted and
            // fire from the paint loop, which calls keyRepeated and longPointerPress
            // directly -- so they never meet the modality filter the packed queue
            // applies, and the held press went on driving the window behind the modal.
            modal.setModalityType(Window.MODALITY_APPLICATION);
            modal.show();
            flushSerialCalls();

            assertNull(target.invoke(Display.getInstance(),
                    Integer.valueOf(w.getWindowId()), main),
                    "a window blocked by a modal must not receive repeats or long presses");
            assertNull(target.invoke(Display.getInstance(), Integer.valueOf(0), main),
                    "nor must the main form");
        } finally {
            modal.dispose();
            w.dispose();
        }
    }
}
