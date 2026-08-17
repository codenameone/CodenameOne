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

        // A port that cannot express a minimum, or that delivers a smaller resize
        // before the constraint takes effect, must not lay the window out below it.
        w.sizeChangedInternal(100, 80);
        assertEquals(320, w.getWidth());
        assertEquals(240, w.getHeight());
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
