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
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Native (operating system) drag and drop: the payload container, the target resolution the
 * ports rely on, and the gesture that hands a press to the platform.
 *
 * <p>The platform half is faked through {@code TestCodenameOneImplementation}, which records the
 * operation the framework prepared and started instead of talking to a window system. What is
 * under test here is everything above the port: which component a drag resolves to, what it
 * answers the operating system, and which callbacks fire.</p>
 */
class NativeDragAndDropTest extends UITestBase {

    /** A component that records every native drag callback it receives. */
    private static final class DropRecorder extends Container {
        final List<String> events = new ArrayList<String>();
        ClipboardContent dropped;
        int rejectAction = -1;

        @Override
        protected void nativeDragEnter(NativeDropEvent ev) {
            events.add("enter");
            if (rejectAction >= 0) {
                ev.accept(rejectAction);
            }
        }

        @Override
        protected void nativeDragOver(NativeDropEvent ev) {
            events.add("over");
            if (rejectAction >= 0) {
                ev.accept(rejectAction);
            }
        }

        @Override
        protected void nativeDragExit(NativeDropEvent ev) {
            events.add("exit");
        }

        @Override
        protected void nativeDrop(NativeDropEvent ev) {
            events.add("drop");
            dropped = ev.getContent();
        }
    }

    private DropRecorder addTarget(Form form) {
        DropRecorder target = new DropRecorder();
        target.setNativeDropTarget(true);
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, target);
        form.revalidate();
        return target;
    }

    /// Drives a drag the way the framework really does. CodenameOneImplementation wraps a
    /// single pointer into one-element arrays and Display dispatches *those*, and Form and
    /// Window implement that overload separately from the scalar one -- so a test that calls
    /// the scalar overload exercises a path no port takes.
    private static void drag(Form form, int x, int y) {
        form.pointerDragged(new int[]{x}, new int[]{y});
    }

    private static ClipboardContent textContent(String text) {
        return new ClipboardContent().setData(ClipboardContent.MIME_TEXT, text);
    }

    // ------------------------------------------------------------------------------------
    // ClipboardContent as a drag payload
    // ------------------------------------------------------------------------------------

    @Test
    void lazyRepresentationIsNotBuiltUntilItIsRead() {
        final int[] calls = {0};
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "report")
                .setDataProvider(ClipboardContent.MIME_FILE, new ClipboardDataProvider() {
                    public Object getClipboardData(String mimeType) {
                        calls[0]++;
                        return "/tmp/report.pdf";
                    }
                });

        assertTrue(content.hasMimeType(ClipboardContent.MIME_FILE),
                "a promised representation is advertised before it is built");
        assertEquals(0, calls[0], "advertising a representation must not build it");

        assertArrayEquals(new String[]{"/tmp/report.pdf"}, content.getFiles());
        assertEquals(1, calls[0], "reading it builds it");
        assertArrayEquals(new String[]{"/tmp/report.pdf"}, content.getFiles());
        assertEquals(1, calls[0], "reading it again reuses the value rather than writing the file twice");
    }

    @Test
    void fileListReadsBackWhicheverWayItWasStored() {
        ClipboardContent one = new ClipboardContent().setFiles(new String[]{"/tmp/a.txt"});
        assertEquals("/tmp/a.txt", one.getData(ClipboardContent.MIME_FILE),
                "a single file is stored as a plain string, which is what the ports expect");
        assertArrayEquals(new String[]{"/tmp/a.txt"}, one.getFiles());

        ClipboardContent many = new ClipboardContent().setFiles(new String[]{"/tmp/a.txt", "/tmp/b.txt"});
        assertArrayEquals(new String[]{"/tmp/a.txt", "/tmp/b.txt"}, many.getFiles());

        assertNull(new ClipboardContent().getFiles(), "no files means null rather than an empty array");
        assertNull(new ClipboardContent().setFiles(null).getFiles());
    }

    // On the event dispatch thread, because EventDispatcher defers a firing made from any other
    // thread and the listener would then not have run by the time the assertion below reads it.
    @FormTest
    void anOperationDefaultsToCopyAndReportsNothingUntilItCompletes() {
        NativeDragOperation op = new NativeDragOperation("hello");
        assertEquals(NativeDragOperation.ACTION_COPY, op.getAllowedActions());
        assertEquals("hello", op.getContent().getText(ClipboardContent.MIME_TEXT));
        assertEquals(NativeDragOperation.ACTION_NONE, op.getPerformedAction());

        final int[] completed = {-1};
        op.addCompletionListener(e -> completed[0] = ((NativeDragOperation) e.getSource()).getPerformedAction());
        op.fireCompleted(NativeDragOperation.ACTION_MOVE);
        assertEquals(NativeDragOperation.ACTION_MOVE, op.getPerformedAction());
        assertEquals(NativeDragOperation.ACTION_MOVE, completed[0],
                "a source that offered a move learns here, and only here, that it must delete its copy");
    }

    @Test
    void aMimeTypeIsNormalizedWithoutAskingTheLocale() {
        // Turkish and Azerbaijani fold I to a dotless i, so String.toLowerCase() turned IMAGE/PNG
        // into something that is not equal to image/png -- and every check against the
        // framework's own constants then failed, on the device's locale alone.
        java.util.Locale saved = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(new java.util.Locale("tr", "TR"));
            ClipboardContent content = new ClipboardContent()
                    .setData("IMAGE/PNG", new byte[]{1, 2, 3})
                    .setData("TEXT/URI-LIST", "https://codenameone.com");
            assertTrue(content.hasMimeType(ClipboardContent.MIME_PNG),
                    "a case-insensitive spelling of a MIME type is the same type in every locale");
            assertNotNull(content.getBytes(ClipboardContent.MIME_PNG));
            assertEquals("https://codenameone.com",
                    content.getText(ClipboardContent.MIME_URI_LIST));
            assertEquals(ClipboardContent.MIME_PNG, content.getMimeTypes()[0]);
        } finally {
            java.util.Locale.setDefault(saved);
        }
    }

    // ------------------------------------------------------------------------------------
    // Resolving the target and answering the operating system
    // ------------------------------------------------------------------------------------

    @FormTest
    void aDragOverATargetIsAcceptedAndDeliversEnterThenOver() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        int action = NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"),
                NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE);
        assertEquals(NativeDragOperation.ACTION_COPY, action,
                "a target that expresses no preference copies, which cannot destroy the source's data");

        NativeDragAndDrop.dragOver(0, x + 1, y + 1, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertEquals("enter", target.events.get(0));
        assertTrue(target.events.contains("over"));

        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
        assertEquals("exit", target.events.get(target.events.size() - 1));
    }

    @FormTest
    void aDragOverNothingIsRejected() {
        Form form = Display.getInstance().getCurrent();
        addTarget(form);
        form.setNativeDropTarget(false);

        // The title area is outside the target; nothing there accepts drops.
        int action = NativeDragAndDrop.dragEnter(0, 1, 1, textContent("hi"), NativeDragOperation.ACTION_COPY);
        assertEquals(NativeDragOperation.ACTION_NONE, action);
        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
    }

    @FormTest
    void aMimeFilterRefusesTheDragFromTheVeryFirstEvent() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        target.setAcceptedDropMimeTypes(ClipboardContent.MIME_FILE);

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY),
                "text is not a file, so the target never sees the drag at all");
        flushSerialCalls();
        assertTrue(target.events.isEmpty());

        ClipboardContent files = new ClipboardContent().setFiles(new String[]{"/tmp/a.txt"});
        assertEquals(NativeDragOperation.ACTION_COPY,
                NativeDragAndDrop.dragEnter(0, x, y, files, NativeDragOperation.ACTION_COPY));
        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
    }

    @FormTest
    void anActionTheTargetRefusesIsNotOffered() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        target.setAcceptedDropActions(NativeDragOperation.ACTION_MOVE);

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY),
                "a copy-only source and a move-only target have nothing in common");
        NativeDragAndDrop.dragExit(0);

        assertEquals(NativeDragOperation.ACTION_MOVE,
                NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"),
                        NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE));
        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
    }

    @FormTest
    void theDeepestTargetWins() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder outer = addTarget(form);
        DropRecorder inner = new DropRecorder();
        inner.setNativeDropTarget(true);
        outer.setLayout(new BorderLayout());
        outer.add(BorderLayout.CENTER, inner);
        form.revalidate();

        NativeDragAndDrop.drop(0, inner.getAbsoluteX() + 2, inner.getAbsoluteY() + 2,
                textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertTrue(inner.events.contains("drop"));
        assertFalse(outer.events.contains("drop"));
    }

    @FormTest
    void aTargetThatRefusesThisPayloadLetsAnAncestorHaveIt() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder outer = addTarget(form);
        DropRecorder inner = new DropRecorder();
        inner.setNativeDropTarget(true);
        inner.setAcceptedDropMimeTypes(ClipboardContent.MIME_FILE);
        outer.setLayout(new BorderLayout());
        outer.add(BorderLayout.CENTER, inner);
        form.revalidate();

        NativeDragAndDrop.drop(0, inner.getAbsoluteX() + 2, inner.getAbsoluteY() + 2,
                textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertFalse(inner.events.contains("drop"));
        assertTrue(outer.events.contains("drop"));
    }

    @FormTest
    void dropDeliversTheContentAndNotifiesTheListener() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        final NativeDropEvent[] seen = new NativeDropEvent[1];
        target.addNativeDropListener(e -> seen[0] = (NativeDropEvent) e);

        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "two files")
                .setFiles(new String[]{"/tmp/a.txt", "/tmp/b.txt"});
        int accepted = NativeDragAndDrop.drop(0, target.getAbsoluteX() + 5, target.getAbsoluteY() + 5,
                content, NativeDragOperation.ACTION_COPY);
        assertEquals(NativeDragOperation.ACTION_COPY, accepted);
        flushSerialCalls();

        assertNotNull(target.dropped);
        assertArrayEquals(new String[]{"/tmp/a.txt", "/tmp/b.txt"}, target.dropped.getFiles(),
                "a drop of several files arrives as several files");
        assertNotNull(seen[0]);
        assertEquals(ActionEvent.Type.NativeDrop, seen[0].getEventType());
        assertEquals("two files", seen[0].getText());
        assertFalse(seen[0].isLocal(), "a drag this application did not start is not local");
    }

    @FormTest
    void aRejectionInEnterSurvivesAnAlreadyQueuedOver() {
        Form form = Display.getInstance().getCurrent();
        final List<String> seen = new ArrayList<String>();
        // Decides in nativeDragEnter and nowhere else, which is the case the queued over event
        // used to undo.
        Container target = new Container() {
            @Override
            protected void nativeDragEnter(NativeDropEvent ev) {
                seen.add("enter");
                ev.reject();
            }

            @Override
            protected void nativeDrop(NativeDropEvent ev) {
                seen.add("drop");
            }
        };
        target.setNativeDropTarget(true);
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, target);
        form.revalidate();

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        // A second motion event before the queue drains, so its callback is queued behind the
        // enter that has not run yet.
        NativeDragAndDrop.dragOver(0, x + 1, y + 1, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.dragOver(0, x + 2, y + 2, textContent("hi"),
                        NativeDragOperation.ACTION_COPY),
                "the refusal made in nativeDragEnter must not be undone by an over event that "
                        + "was queued before it ran");
        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY));
        flushSerialCalls();
        assertFalse(seen.contains("drop"));
        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
    }

    @FormTest
    void aRejectionMadeWhileHoveringSurvivesTheDrop() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        // The target refuses from inside its callback, which is the only place it can change
        // its mind about a payload the declarative filters already let through.
        target.rejectAction = NativeDragOperation.ACTION_NONE;

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        // The port drops carrying the action it was handed before that callback ran -- its
        // answer is one drag event behind by construction -- so the refusal only survives if
        // the drop honours the target's own latest word rather than recomputing.
        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY),
                "a target that refused while hovering must not be handed the drop anyway");
        flushSerialCalls();
        assertFalse(target.events.contains("drop"),
                "and no drop event is delivered, which is what reject() promises");
    }

    @FormTest
    void anAcceptedActionThePlatformNoLongerProposesIsNotReported() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        // The target chose a move out of a drag that offered both.
        target.rejectAction = NativeDragOperation.ACTION_MOVE;

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"),
                NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE);
        flushSerialCalls();

        // The user let the modifier go before releasing, so the platform proposes only a copy.
        // Answering "moved" would tell it a move was performed -- and on a local drag that is
        // the word the source deletes its data on -- while the target agreed to no such thing.
        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY),
                "the accepted move is no longer performable, and nothing is the honest answer");
        flushSerialCalls();
        assertFalse(target.events.contains("drop"));
    }

    @FormTest
    void anAcceptedActionThePlatformStillProposesIsReported() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        target.rejectAction = NativeDragOperation.ACTION_MOVE;

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"),
                NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE);
        flushSerialCalls();

        assertEquals(NativeDragOperation.ACTION_MOVE,
                NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_MOVE),
                "the ordinary case: what the target chose is still what the release performs");
        flushSerialCalls();
        assertTrue(target.events.contains("drop"));
    }

    @FormTest
    void aDeferredDropIsNotJudgedByWhoeverIsHoveringNow() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;

        // A second drop hovering the same component while the first is still loading, and
        // refusing it. iOS supports exactly that overlap.
        target.rejectAction = NativeDragOperation.ACTION_NONE;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        // The ordinary entry point honours that refusal, which is right for the session doing
        // the hovering.
        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY));
        flushSerialCalls();
        assertFalse(target.events.contains("drop"));

        // The loading one brought its own decision, taken when the user released it, and must
        // not have it overruled by a drop that arrived since: the user performed this one.
        target.events.clear();
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertEquals(NativeDragOperation.ACTION_COPY,
                NativeDragAndDrop.deferredDrop(0, x, y, textContent("hi"),
                        NativeDragOperation.ACTION_COPY, NativeDragOperation.ACTION_COPY, false),
                "a drop that has been loading is no longer the hovering session, and the hover "
                        + "state it would be judged by may be another drop's entirely");
        flushSerialCalls();
        assertTrue(target.events.contains("drop"));
    }

    @FormTest
    void aModifierChangeDoesNotTurnAnUninterestedTargetIntoARefusal() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;

        // The enter is queued while the drag offers a move only, and before the event dispatch
        // thread runs it the user releases the modifier: the platform now offers a copy.
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_MOVE);
        NativeDragAndDrop.dragOver(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertEquals(NativeDragOperation.ACTION_COPY,
                NativeDragAndDrop.plannedDropAction(0, x, y, textContent("hi"),
                        NativeDragOperation.ACTION_COPY),
                "the target has no listener and refused nothing; a callback queued under the "
                        + "offer that has since been withdrawn must not answer for the new one");

        assertEquals(NativeDragOperation.ACTION_COPY,
                NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY));
        flushSerialCalls();
        assertTrue(target.events.contains("drop"));
    }

    @FormTest
    void aTargetNarrowingItsActionsWhileHoveredIsAnsweredWithWhatItStillTakes() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        target.setAcceptedDropActions(NativeDragOperation.ACTION_MOVE);
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        int both = NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE;

        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), both);
        flushSerialCalls();
        assertEquals(NativeDragOperation.ACTION_MOVE,
                NativeDragAndDrop.plannedDropAction(0, x, y, textContent("hi"), both));

        // The target changes its mind about what it will do, without the pointer moving.
        target.setAcceptedDropActions(NativeDragOperation.ACTION_COPY);
        NativeDragAndDrop.dragOver(0, x, y, textContent("hi"), both);
        flushSerialCalls();

        assertEquals(NativeDragOperation.ACTION_COPY,
                NativeDragAndDrop.plannedDropAction(0, x, y, textContent("hi"), both),
                "a target withdrawing an action is as much a withdrawal as the source doing it: "
                        + "keeping the move advertised had the release refused outright rather "
                        + "than settling for the copy the target still takes");
    }

    @FormTest
    void aTargetThatCannotPerformTheActionLetsAnAncestorHaveIt() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder outer = addTarget(form);
        outer.setAcceptedDropActions(NativeDragOperation.ACTION_COPY);
        DropRecorder inner = new DropRecorder();
        inner.setNativeDropTarget(true);
        inner.setAcceptedDropActions(NativeDragOperation.ACTION_MOVE);
        outer.setLayout(new BorderLayout());
        outer.add(BorderLayout.CENTER, inner);
        form.revalidate();

        // A copy-only drag over a move-only target nested in a copy-capable one. Choosing the
        // inner target on the content alone and only then finding it can do nothing swallowed
        // the drag: refusing on the action is the same kind of refusal as refusing on the MIME
        // type, and the walk has to treat it the same way.
        int x = inner.getAbsoluteX() + 2;
        int y = inner.getAbsoluteY() + 2;
        assertEquals(NativeDragOperation.ACTION_COPY,
                NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"),
                        NativeDragOperation.ACTION_COPY),
                "the copy-capable ancestor takes it");
        NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertTrue(outer.events.contains("drop"));
        assertFalse(inner.events.contains("drop"));
    }

    @FormTest
    void eachGestureRendersItsOwnDragImage() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            NativeDragOperation op = new NativeDragOperation("reused");
            source.setNativeDragOperation(op);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            Image first = op.getDragImage();
            assertNotNull(first);
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();

            // The same reusable operation, dragged again. The generated snapshot must not have
            // become the operation's permanent image: the component may look different now and
            // the press landed somewhere else.
            form.pointerPressed(x + 4, y + 4);
            drag(form, x + 200, y + 200);
            Image second = op.getDragImage();
            assertNotNull(second);
            assertNotSame(first, second,
                    "a framework-rendered preview belongs to its gesture, not to the operation");
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();

            // An image the application supplied is never replaced.
            Image supplied = Image.createImage(4, 4);
            op.setDragImage(supplied);
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            assertSame(supplied, op.getDragImage(), "an application's own image is left alone");
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void dropOnNothingReportsFailureSoThePortCanTellTheSource() {
        Form form = Display.getInstance().getCurrent();
        addTarget(form);
        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.drop(0, 1, 1, textContent("hi"), NativeDragOperation.ACTION_COPY));
        flushSerialCalls();
    }

    // ------------------------------------------------------------------------------------
    // The gesture: a press on a drag source becomes an operating system drag
    // ------------------------------------------------------------------------------------

    @FormTest
    void reusingAnOperationForgetsTheOutcomeOfTheLastDrag() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            NativeDragOperation op = new NativeDragOperation("reused")
                    .setAllowedActions(NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE);
            assertTrue(NativeDragAndDrop.startDrag(null, op));
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_MOVE);
            flushSerialCalls();
            assertEquals(NativeDragOperation.ACTION_MOVE, op.getPerformedAction());

            // The same instance is offered for every drag of its component, so the second drag
            // must not go on reporting the first one's result while it is still running.
            assertTrue(NativeDragAndDrop.startDrag(null, op));
            assertEquals(NativeDragOperation.ACTION_NONE, op.getPerformedAction(),
                    "a drag in flight has performed nothing yet, whatever the last one did");
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aCompletionOwedIsPaidBeforeTheSameOperationDragsAgain() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            final List<Integer> completions = new ArrayList<Integer>();
            NativeDragOperation op = new NativeDragOperation("reused")
                    .setAllowedActions(NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE);
            op.addCompletionListener(e -> completions.add(
                    Integer.valueOf(((NativeDragOperation) e.getSource()).getPerformedAction())));

            assertTrue(NativeDragAndDrop.startDrag(null, op));
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_MOVE);
            // Deliberately not flushed: the completion is queued, and the same instance is
            // dragged again before the event dispatch thread has run it -- which is what happens
            // when the press beginning the next drag is already ahead of it in the queue.
            assertTrue(NativeDragAndDrop.startDrag(null, op),
                    "the previous session is over, so the next drag is not refused");
            assertEquals(1, completions.size(),
                    "the outcome of the drag that ended is delivered before the operation is "
                            + "armed again, rather than during the drag that follows");
            assertEquals(NativeDragOperation.ACTION_MOVE, completions.get(0).intValue());
            assertEquals(NativeDragOperation.ACTION_NONE, op.getPerformedAction(),
                    "and the drag now running has performed nothing yet");

            flushSerialCalls();
            assertEquals(1, completions.size(), "the queued callback does not deliver it twice");
            assertEquals(NativeDragOperation.ACTION_NONE, op.getPerformedAction(),
                    "a late completion must not write the old drag's action onto the new one");

            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void oneOperationsPendingCompletionIsNotLostToAnother() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            final List<String> completions = new ArrayList<String>();
            NativeDragOperation first = new NativeDragOperation("first")
                    .setAllowedActions(NativeDragOperation.ACTION_MOVE);
            NativeDragOperation second = new NativeDragOperation("second");
            first.addCompletionListener(e -> completions.add("first:"
                    + ((NativeDragOperation) e.getSource()).getPerformedAction()));
            second.addCompletionListener(e -> completions.add("second:"
                    + ((NativeDragOperation) e.getSource()).getPerformedAction()));

            assertTrue(NativeDragAndDrop.startDrag(null, first));
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_MOVE);
            // A different operation starts and ends before the first one's completion has run --
            // an Android drag refused by the posted startDragAndDrop does exactly this, off the
            // event dispatch thread. Sharing one slot, the second overwrote the first.
            assertTrue(NativeDragAndDrop.startDrag(null, second));
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();

            assertTrue(completions.contains("first:" + NativeDragOperation.ACTION_MOVE),
                    "the first source is still owed the move it must delete its copy on, and "
                            + "nothing else starting can take that away from it");
            assertTrue(completions.contains("second:" + NativeDragOperation.ACTION_NONE));
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aReusedOperationAsksItsProvidersAgainForEachDrag() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            final int[] calls = {0};
            ClipboardContent content = new ClipboardContent()
                    .setDataProvider(ClipboardContent.MIME_FILE, new ClipboardDataProvider() {
                        public Object getClipboardData(String mimeType) {
                            calls[0]++;
                            return "/tmp/drag-" + calls[0] + ".pdf";
                        }
                    });
            // The instance a component installs once and is dragged with again and again.
            NativeDragOperation op = new NativeDragOperation(content);

            assertTrue(NativeDragAndDrop.startDrag(null, op));
            assertEquals("/tmp/drag-1.pdf", op.getContent().getFiles()[0]);
            assertEquals("/tmp/drag-1.pdf", op.getContent().getFiles()[0],
                    "within one drag the provider is asked once: a drop queries and then reads, "
                            + "and the promised file must not be written twice");
            assertEquals(1, calls[0]);
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();

            assertTrue(NativeDragAndDrop.startDrag(null, op));
            assertEquals("/tmp/drag-2.pdf", op.getContent().getFiles()[0],
                    "and the next drag gets its own: the file the first one wrote may have been "
                            + "cleaned up by now, and publishing that path again publishes nothing");
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aCompletionNamesTheComponentItsOwnDragBelongedTo() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container first = new Container();
            Container second = new Container();
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.NORTH, first);
            form.add(BorderLayout.SOUTH, second);
            form.revalidate();

            final List<Component> sources = new ArrayList<Component>();
            NativeDragOperation op = new NativeDragOperation("reused")
                    .setAllowedActions(NativeDragOperation.ACTION_MOVE);
            op.addCompletionListener(e ->
                    sources.add(((NativeDragOperation) e.getSource()).getSource()));

            assertTrue(NativeDragAndDrop.startDrag(first, op));
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_MOVE);
            // The same instance dragged from a different component before the completion has
            // run, which is what a source sharing one operation between rows looks like.
            assertTrue(NativeDragAndDrop.startDrag(second, op));

            assertEquals(1, sources.size());
            assertSame(first, sources.get(0),
                    "a source told to delete its copy must be told which drag did it: reporting "
                            + "the component now dragging would have the wrong one clean up");
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aCompletionListenerThatThrowsDoesNotWedgeEveryLaterDrag() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            NativeDragOperation op = new NativeDragOperation("reused")
                    .setAllowedActions(NativeDragOperation.ACTION_MOVE);
            op.addCompletionListener(e -> {
                throw new IllegalStateException("a listener may do anything");
            });

            assertTrue(NativeDragAndDrop.startDrag(null, op));
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_MOVE);
            // The owed completion is delivered from inside the next start, after the operation
            // has been made active -- so a listener throwing there left a drag that never began
            // looking like one still running.
            assertTrue(NativeDragAndDrop.startDrag(null, op),
                    "the drag still starts: application code that throws is logged, not allowed "
                            + "to take the framework with it");
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();

            NativeDragOperation next = new NativeDragOperation("after");
            assertTrue(NativeDragAndDrop.startDrag(null, next),
                    "and nothing is left active, so later drags are not refused for the life of "
                            + "the process");
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aSecondDragIsRefusedWhileOneIsStillRunning() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            NativeDragOperation first = new NativeDragOperation("first");
            NativeDragOperation second = new NativeDragOperation("second");
            assertTrue(NativeDragAndDrop.startDrag(null, first));
            assertFalse(NativeDragAndDrop.startDrag(null, second),
                    "one drag at a time; the second must not displace the first");
            assertSame(first, NativeDragAndDrop.getActiveDrag());

            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_MOVE);
            flushSerialCalls();
            assertEquals(NativeDragOperation.ACTION_MOVE, first.getPerformedAction(),
                    "the first source still learns its outcome, which is what tells it to delete");
            assertEquals(NativeDragOperation.ACTION_NONE, second.getPerformedAction());

            assertTrue(NativeDragAndDrop.startDrag(null, second),
                    "and once the session is over the next drag starts normally");
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void becomingADragSourceTellsThePort() {
        implementation.resetNativeDragState();
        try {
            Container cmp = new Container();
            assertEquals(0, implementation.getNativeDragSourceRegistrations());

            cmp.setNativeDragOperation(new NativeDragOperation("x"));
            assertEquals(1, implementation.getNativeDragSourceRegistrations(),
                    "a platform that needs a gesture recognizer installs it on the strength of "
                            + "this, so an application that never drags keeps its touch handling");

            cmp.setNativeDragSource(true);
            assertEquals(2, implementation.getNativeDragSourceRegistrations());

            cmp.setNativeDragOperation(null);
            cmp.setNativeDragSource(false);
            assertEquals(2, implementation.getNativeDragSourceRegistrations(),
                    "giving up on dragging is not a request for a recognizer");

            assertEquals(0, implementation.getNativeDropTargetRegistrations());
            cmp.setNativeDropTarget(true);
            assertEquals(1, implementation.getNativeDropTargetRegistrations());
            cmp.setNativeDropTarget(false);
            assertEquals(1, implementation.getNativeDropTargetRegistrations());
        } finally {
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDragOnANativeDragSourceIsHandedToThePlatform() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("dragged out"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            assertNotNull(implementation.getPreparedNativeDrag(),
                    "the press stages the payload so a platform that owns the gesture can ask for it");
            assertNull(implementation.getStartedNativeDrag(), "a press alone is not a drag");

            drag(form, x + 200, y + 200);
            assertNotNull(implementation.getStartedNativeDrag(), "moving far enough starts the session");
            assertSame(source, implementation.getStartedNativeDrag().getSource());
            assertSame(implementation.getStartedNativeDrag(), NativeDragAndDrop.getActiveDrag());

            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
            assertNull(NativeDragAndDrop.getActiveDrag());
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDragSourceInsideADraggableContainerIsStillStaged() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            // The form primes drag and drop on the pressed component and then again on its
            // nearest draggable ancestor. The drag source sits between the two, so the second
            // pass cannot find it -- and must not throw away what the first pass staged.
            Container draggableOuter = new Container(new BorderLayout());
            draggableOuter.setDraggable(true);
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("from the middle"));
            draggableOuter.add(BorderLayout.CENTER, source);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, draggableOuter);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            assertNotNull(implementation.getPreparedNativeDrag());

            drag(form, x + 200, y + 200);
            assertNotNull(implementation.getStartedNativeDrag());
            assertSame(source, implementation.getStartedNativeDrag().getSource());

            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDragSourceInTheTitleAreaIsStaged() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            // The title area has its own press branch, and it is the one branch that never
            // primed drag and drop -- so a Toolbar component given an operation could not be
            // dragged while the same component in the content pane could.
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("from the title"));
            source.setPreferredSize(new com.codename1.ui.geom.Dimension(40, 40));
            form.getTitleArea().add(BorderLayout.EAST, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 2;
            int y = source.getAbsoluteY() + 2;
            assertTrue(y < form.getContentPane().getAbsoluteY(),
                    "the fixture has to sit in the title area for this to test anything");

            form.pointerPressed(x, y);
            assertNotNull(implementation.getPreparedNativeDrag());
            drag(form, x + 200, y + 200);
            assertNotNull(implementation.getStartedNativeDrag());
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDisabledDragSourceIsNotDraggable() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("dragged out"));
            source.setEnabled(false);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            assertNull(implementation.getPreparedNativeDrag(),
                    "a Form primes drag and drop before its own isEnabled gate, so the check "
                            + "has to be here or a disabled control is draggable on the main "
                            + "surface and not in a window");

            drag(form, x + 200, y + 200);
            assertNull(implementation.getStartedNativeDrag());
            form.pointerReleased(x + 200, y + 200);

            // Enabled again, the very same component drags.
            source.setEnabled(true);
            form.pointerPressed(x, y);
            assertNotNull(implementation.getPreparedNativeDrag());
            drag(form, x + 200, y + 200);
            assertNotNull(implementation.getStartedNativeDrag());
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aStaleDragCallbackDoesNotSpeakForANewerOne() {
        Form form = Display.getInstance().getCurrent();
        // Decides from the payload rather than from a field, because the whole point is that
        // the stale callback runs later and must still carry *its own* drag's decision.
        Container target = new Container() {
            @Override
            protected void nativeDragEnter(NativeDropEvent ev) {
                ev.accept("first".equals(ev.getText())
                        ? NativeDragOperation.ACTION_MOVE : NativeDragOperation.ACTION_COPY);
            }
        };
        target.setNativeDropTarget(true);
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, target);
        form.revalidate();

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        // A drag arrives, settles on a move, and leaves -- with its callback still queued.
        NativeDragAndDrop.dragEnter(0, x, y, textContent("first"),
                NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE);
        NativeDragAndDrop.dragExit(0);

        // What the operating system would be told, read from between the stale callback and
        // the new drag's own. Nothing else can see the window: once the new drag's callback
        // runs it puts the right answer back, so an assertion after the queue drains would
        // pass whether or not the stale one had spoken.
        final int[] observed = { -1 };
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                observed[0] = NativeDragAndDrop.dragOver(0, x, y, textContent("second"),
                        NativeDragOperation.ACTION_COPY);
            }
        });

        // The second, copy-only drag enters the same component before the queue drains.
        NativeDragAndDrop.dragEnter(0, x, y, textContent("second"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertEquals(NativeDragOperation.ACTION_COPY, observed[0],
                "the first drag's move decision must not be handed to the second, copy-only drag");
        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
    }

    @FormTest
    void theGesturesOwnPreviewSurvivesTheStart() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setPreferredSize(new com.codename1.ui.geom.Dimension(40, 40));
            NativeDragOperation op = new NativeDragOperation("dragged by hand");
            source.setNativeDragOperation(op);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            // Pressed near one corner, not in the middle.
            int x = source.getAbsoluteX() + 3;
            int y = source.getAbsoluteY() + 4;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);

            assertNotNull(implementation.getStartedNativeDrag());
            assertEquals(3, op.getDragImageOffsetX(),
                    "the preview hangs from where the press actually landed; re-rendering it "
                            + "at the start replaces that with the component's centre and the "
                            + "image jumps out from under the pointer");
            assertEquals(4, op.getDragImageOffsetY());
        } finally {
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDragStartedInCodeStillGetsTheSourcesPreview() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setPreferredSize(new com.codename1.ui.geom.Dimension(40, 40));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            NativeDragOperation op = new NativeDragOperation("started in code");
            assertTrue(NativeDragAndDrop.startDrag(source, op));

            assertNotNull(op.getDragImage(),
                    "this entry point documents the source as providing the default preview, "
                            + "and without one Android snapshots the whole surface while JavaSE "
                            + "drags nothing at all");
            assertTrue(op.isDragImageGenerated(),
                    "and it is the framework's snapshot, not something the application supplied");

        } finally {
            // In the finally, not after the assertions: a session left active wedges every test
            // that follows, so a failure here would be reported as twenty.
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDropThatMaterializedNothingIsNotADrop() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;

        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        // Every representation failed to be read: a transferable that threw, or a one-shot
        // stream already spent. What arrives is a payload with nothing in it.
        int accepted = NativeDragAndDrop.drop(0, x, y, new ClipboardContent(),
                NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertEquals(NativeDragOperation.ACTION_NONE, accepted,
                "the source is told the transfer did not happen, because it did not");
        assertFalse(target.events.contains("drop"),
                "and a target that takes anything is not handed nothing and told it was a drop");
        assertTrue(target.events.contains("exit"),
                "the component that was hovering still hears that the drag left it");
    }

    @FormTest
    void aDropWhoseTargetMovedIsStillDeliveredToIt() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;

        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        // The tree is rebuilt while a slow item provider is still loading: the component that
        // accepted the drag is still there, but no longer where the release happened.
        form.removeComponent(target);
        Container filler = new Container();
        form.add(BorderLayout.CENTER, filler);
        Container elsewhere = new Container();
        elsewhere.setNativeDropTarget(false);
        form.add(BorderLayout.SOUTH, elsewhere);
        form.revalidate();
        // Put it back somewhere the release point does not reach.
        form.add(BorderLayout.NORTH, target);
        target.setPreferredSize(new com.codename1.ui.geom.Dimension(1, 1));
        form.revalidate();

        NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertTrue(target.events.contains("drop"),
                "the component that accepted this drag is still the one that accepted it; the "
                        + "payload goes to it rather than on the floor because the tree moved "
                        + "while the providers were loading");
        assertNotNull(target.dropped);
    }

    @FormTest
    void aDropReleasedAwayFromTheHoverIsNotHandedToTheHoveredTarget() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        Container elsewhere = new Container();
        elsewhere.add(new Label("not a drop target"));
        form.add(BorderLayout.SOUTH, elsewhere);
        form.revalidate();

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        // The pointer travels on to something that is not a target and is released there,
        // with no drag event in between -- a flick, or a port that reports the hover sparsely.
        NativeDragAndDrop.drop(0, elsewhere.getAbsoluteX() + 5, elsewhere.getAbsoluteY() + 5,
                textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertFalse(target.events.contains("drop"),
                "a release somewhere else is a release somewhere else: the recovery is for a "
                        + "tree that moved under a slow load, not for a pointer that moved");
        assertNull(target.dropped);
    }

    @FormTest
    void aTargetHiddenByItsAncestorIsNotHandedTheDelayedDrop() {
        Form form = Display.getInstance().getCurrent();
        Container holder = new Container(new BorderLayout());
        DropRecorder target = new DropRecorder();
        target.setNativeDropTarget(true);
        target.setPreferredSize(new com.codename1.ui.geom.Dimension(40, 40));
        holder.add(BorderLayout.CENTER, target);
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, holder);
        form.revalidate();

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertEquals("[enter]", target.events.toString());

        // Hidden by an ancestor while a slow provider was still loading. Its own flag still says
        // visible, so only walking up finds out.
        holder.setVisible(false);
        NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertFalse(target.events.contains("drop"),
                "hit testing does not descend into a hidden container, and the fallback has to "
                        + "agree with it: a drop the user can no longer see must not land");
        assertNull(target.dropped);
    }

    @FormTest
    void aTargetThatStoppedTakingPointerEventsIsNotHandedTheDelayedDrop() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;

        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertEquals("[enter]", target.events.toString());

        // Opted out while a slow item provider was still loading, so by the time the drop
        // arrives the walk no longer reaches it -- and neither may the fallback that answers
        // when the walk finds nothing.
        target.setIgnorePointerEvents(true);
        NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertFalse(target.events.contains("drop"),
                "a component that stopped taking pointer events is not a drop target, however "
                        + "recently it was hovering");
        assertNull(target.dropped);
    }

    @FormTest
    void aRefusedDropTellsTheComponentTheDragLeftIt() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        target.rejectAction = NativeDragOperation.ACTION_NONE;

        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertEquals("[enter]", target.events.toString());

        assertEquals(NativeDragOperation.ACTION_NONE,
                NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_COPY));
        flushSerialCalls();

        assertTrue(target.events.contains("exit"),
                "it refused the drop, so it gets no drop callback -- and the hover state is "
                        + "cleared here, so nothing later can tell it either: a component that "
                        + "clears its highlight on exit or drop would stay highlighted for good");
    }

    @FormTest
    void aDropThatLandsElsewhereTellsTheComponentItLeft() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder left = new DropRecorder();
        DropRecorder landed = new DropRecorder();
        left.setNativeDropTarget(true);
        landed.setNativeDropTarget(true);
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.NORTH, left);
        form.add(BorderLayout.SOUTH, landed);
        left.setPreferredSize(new com.codename1.ui.geom.Dimension(40, 40));
        landed.setPreferredSize(new com.codename1.ui.geom.Dimension(40, 40));
        form.revalidate();

        NativeDragAndDrop.dragEnter(0, left.getAbsoluteX() + 5, left.getAbsoluteY() + 5,
                textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertEquals("[enter]", left.events.toString());

        // Moved and released in one go, so the release resolves somewhere the drag never
        // hovered.
        NativeDragAndDrop.drop(0, landed.getAbsoluteX() + 5, landed.getAbsoluteY() + 5,
                textContent("hi"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertEquals("[enter, exit]", left.events.toString(),
                "the drag ended for the component it was over, and nothing else can tell it: "
                        + "the port's own cleanup finds the target already cleared");
        assertEquals("[drop]", landed.events.toString());
    }

    @FormTest
    void anEntryAfterASessionThatNeverExitedIsStillAnEntry() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;

        // A session that refuses and then ends without ever leaving the component. Ports have
        // paths that reach neither drop() nor dragExit() -- an Android drop the target refused,
        // an iOS session cancelled inside the surface -- so the framework can be left hovering.
        target.rejectAction = NativeDragOperation.ACTION_NONE;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("first"), NativeDragOperation.ACTION_COPY);
        flushSerialCalls();
        assertEquals("[enter]", target.events.toString());

        // The next session enters the same component.
        target.rejectAction = -1;
        target.events.clear();
        int answer = NativeDragAndDrop.dragEnter(0, x, y, textContent("second"),
                NativeDragOperation.ACTION_COPY);
        flushSerialCalls();

        assertEquals("[exit, enter]", target.events.toString(),
                "an entry is an entry: routed as a move over a target left behind by the last "
                        + "session, the component never hears about the new drag at all");
        assertEquals(NativeDragOperation.ACTION_COPY, answer,
                "and it must not inherit the refusal the ended session left, which is never "
                        + "recomputed because a refusal is a decision");

        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
    }

    /// A form that leaves the "grab a moving list" press to the dragStopFlag recovery, which
    /// is what `Form#resumeDragAfterScrolling(int, int)` documents overriding it for.
    private static final class NoResumeForm extends Form {
        @Override
        protected void initGlobalToolbar() {
        }

        @Override
        protected boolean resumeDragAfterScrolling(int x, int y) {
            return false;
        }
    }

    @FormTest
    void grabbingAScrollingContainerStopsItRatherThanDraggingOut() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        NoResumeForm form = new NoResumeForm();
        try {
            Container scroller = new Container(new BorderLayout());
            scroller.setScrollableY(true);
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("row"));
            scroller.add(BorderLayout.CENTER, source);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, scroller);
            form.show();
            flushSerialCalls();

            // A glide in progress, which is what makes this press a "stop the scroll" press:
            // Form defers the real pointerPressed to the first drag packet.
            scroller.draggedMotionY = Motion.createLinearMotion(0, 100, 1000);
            scroller.draggedMotionY.start();

            int x = source.getAbsoluteX() + 5;
            int y = source.getAbsoluteY() + 5;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            assertNull(implementation.getStartedNativeDrag(),
                    "grabbing a moving list stops it; handing the row to the operating system "
                            + "on that first packet makes the list impossible to stop");

            // The glide is over, and a deliberate drag from here still starts one.
            scroller.draggedMotionY = null;
            drag(form, x + 400, y + 400);
            assertNotNull(implementation.getStartedNativeDrag(),
                    "and the feature still works once the scroll has been taken over");

            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
        }
    }

    @FormTest
    void aDropAssembledLateReportsItsOwnActionsRatherThanTheNewDragS() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            DropRecorder hovered = new DropRecorder();
            DropRecorder target = new DropRecorder();
            hovered.setNativeDropTarget(true);
            target.setNativeDropTarget(true);
            hovered.setPreferredSize(new com.codename1.ui.geom.Dimension(40, 40));
            target.setPreferredSize(new com.codename1.ui.geom.Dimension(40, 40));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.NORTH, hovered);
            form.add(BorderLayout.SOUTH, target);
            form.revalidate();
            final NativeDropEvent[] seen = { null };
            target.addNativeDropListener(new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    if (ev.getEventType() == ActionEvent.Type.NativeDrop) {
                        seen[0] = (NativeDropEvent) ev;
                    }
                }
            });

            // A move-only drag of our own is hovering elsewhere by the time a copy-only drop
            // that arrived from another application finishes loading. It has overwritten what
            // the framework remembers of the earlier one.
            NativeDragAndDrop.dragEnter(0, hovered.getAbsoluteX() + 5, hovered.getAbsoluteY() + 5,
                    textContent("ours"), NativeDragOperation.ACTION_MOVE);
            flushSerialCalls();
            NativeDragAndDrop.drop(0, target.getAbsoluteX() + 5, target.getAbsoluteY() + 5,
                    textContent("theirs"), NativeDragOperation.ACTION_COPY,
                    NativeDragOperation.ACTION_COPY, false);
            flushSerialCalls();

            assertNotNull(seen[0]);
            assertEquals(NativeDragOperation.ACTION_COPY, seen[0].getAllowedActions(),
                    "the drop reports what its own drag offered, not what the drag that has "
                            + "since started offers");
            assertEquals(NativeDragOperation.ACTION_COPY, seen[0].getAcceptedAction(),
                    "and the copy it is performing is accepted rather than measured against a "
                            + "move-only mask and refused outright");
        } finally {
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDropAssembledLateIsNotLocalJustBecauseADragIsRunning() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            DropRecorder target = addTarget(form);
            final Boolean[] seen = { null };
            target.addNativeDropListener(new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    if (ev.getEventType() == ActionEvent.Type.NativeDrop) {
                        seen[0] = Boolean.valueOf(((NativeDropEvent) ev).isLocal());
                    }
                }
            });
            int x = target.getAbsoluteX() + 5;
            int y = target.getAbsoluteY() + 5;

            // A drag this application started, running while a drop that arrived from elsewhere
            // finally finishes loading -- which is what a slow item provider does on iOS.
            assertTrue(NativeDragAndDrop.startDrag(null,
                    new NativeDragOperation("ours, and still going")));
            NativeDragAndDrop.drop(0, x, y, textContent("theirs"),
                    NativeDragOperation.ACTION_COPY, NativeDragOperation.ACTION_COPY, false);
            flushSerialCalls();

            assertEquals(Boolean.FALSE, seen[0],
                    "the drop came from another application; asking which drag is running now "
                            + "answers about a different one, and a target telling reordering "
                            + "from importing would take foreign content as an internal move");
        } finally {
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void acceptingTheWholeSetIsNotAcceptingAnAction() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        int both = NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE;
        // accept(getAllowedActions()) is the obvious thing to write and means nothing: there is
        // no agreeing to two actions at once.
        target.rejectAction = both;

        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), both);
        flushSerialCalls();
        int answer = NativeDragAndDrop.dragOver(0, x, y, textContent("hi"), both);

        assertEquals(NativeDragOperation.ACTION_NONE, answer,
                "a set is not an action; stored as one it reaches the ports, which each read it "
                        + "their own way -- the iOS mapping picks the move out of copy-or-move");

        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
    }

    @FormTest
    void aGestureTheWindowHandsOverDoesNotLeaveADragStaged() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        implementation.setMultiWindowSupported(true);
        Window w = new Window("holds a drag source");
        try {
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("behind the dialog"));
            w.setLayout(new BorderLayout());
            w.add(BorderLayout.CENTER, source);
            w.show();
            flushSerialCalls();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            w.pointerPressed(x, y);
            assertNotNull(implementation.getPreparedNativeDrag(), "the press staged one");

            // What showing a dialog from a press handler does: the pointer changes hands and
            // no release ever arrives for the gesture that was in flight.
            w.pushPointerInputScope(new Container());
            w.pointerDragged(new int[]{x + 200}, new int[]{y + 200});

            assertNull(implementation.getStartedNativeDrag(),
                    "the component that staged this is behind whatever took the pointer, and "
                            + "the native hook runs before the cancellation is looked at");
        } finally {
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            w.dispose();
            flushSerialCalls();
            implementation.setMultiWindowSupported(false);
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aCancelledGestureDoesNotLeaveADragStaged() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("staged"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            assertNotNull(implementation.getPreparedNativeDrag());

            // What a platform that cancels a touch does: no release ever arrives.
            NativeDragAndDrop.gestureCancelled();
            drag(form, x + 200, y + 200);
            assertNull(implementation.getStartedNativeDrag(),
                    "the gesture the press belonged to is over, so nothing it staged may still "
                            + "be dragged");
        } finally {
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aPressAtTheSamePixelIsStillANewPress() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container first = new Container();
            first.setNativeDragOperation(new NativeDragOperation("the first press"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, first);
            form.revalidate();

            int x = first.getAbsoluteX() + 10;
            int y = first.getAbsoluteY() + 10;
            form.pointerPressed(x, y);

            // A second press at the very same pixel, with no release or cancellation between
            // them to clear what the first staged -- which is what a platform that drops a
            // gesture on the floor leaves behind. The component is given a different payload
            // first: identified by position, the second press inherits the first one's.
            NativeDragOperation second = new NativeDragOperation("the second press");
            first.setNativeDragOperation(second);
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);

            assertNotNull(implementation.getStartedNativeDrag());
            assertSame(second, implementation.getStartedNativeDrag(),
                    "a press is not its coordinates: the second press has its own payload");
        } finally {
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDragStartedInCodeSpendsWhatThePressStaged() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("staged by the press"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            assertNotNull(implementation.getPreparedNativeDrag(), "the press staged one");

            // A long press handler starting a drag of its own, which is what this entry point
            // is for.
            NativeDragOperation started = new NativeDragOperation("started in code");
            assertTrue(NativeDragAndDrop.startDrag(source, started));

            int cancelledBefore = implementation.getCancelledNativeDrags();
            form.pointerReleased(x, y);
            assertEquals(cancelledBefore, implementation.getCancelledNativeDrags(),
                    "the release must not cancel the port's staging underneath a session that "
                            + "is already running");
            assertSame(started, NativeDragAndDrop.getActiveDrag(),
                    "and the drag started in code is still the running one");
        } finally {
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aTargetCannotAcceptMoreThanItDeclared() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        // Declared copy-only, and its listener asks for a move anyway.
        target.setAcceptedDropActions(NativeDragOperation.ACTION_COPY);
        target.rejectAction = NativeDragOperation.ACTION_MOVE;
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        int both = NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE;

        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), both);
        flushSerialCalls();
        int answer = NativeDragAndDrop.dragOver(0, x, y, textContent("hi"), both);

        assertEquals(NativeDragOperation.ACTION_NONE, answer,
                "a move is the source deleting its copy, and this target said it does not do "
                        + "moves -- honouring the listener over the declaration that made the "
                        + "component eligible would destroy data on its word");

        NativeDragAndDrop.dragExit(0);
        flushSerialCalls();
    }

    @FormTest
    void theDropEventReportsWhatTheSourceAllowedAndWhatIsHappening() {
        Form form = Display.getInstance().getCurrent();
        DropRecorder target = addTarget(form);
        final NativeDropEvent[] seen = { null };
        target.addNativeDropListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                if (ev.getEventType() == ActionEvent.Type.NativeDrop) {
                    seen[0] = (NativeDropEvent) ev;
                }
            }
        });
        int x = target.getAbsoluteX() + 5;
        int y = target.getAbsoluteY() + 5;
        int both = NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE;

        // The source offers both and the target asks for the move, which is the case the
        // question is about: the mask and the choice are different answers.
        target.rejectAction = NativeDragOperation.ACTION_MOVE;
        NativeDragAndDrop.dragEnter(0, x, y, textContent("hi"), both);
        flushSerialCalls();
        NativeDragAndDrop.drop(0, x, y, textContent("hi"), NativeDragOperation.ACTION_MOVE);
        flushSerialCalls();

        assertNotNull(seen[0], "the drop has to reach the target for any of this to be asked");
        assertEquals(both, seen[0].getAllowedActions(),
                "getAllowedActions is what the *source* permits, and reporting the chosen "
                        + "action there makes a copy-or-move source look move-only");
        assertEquals(NativeDragOperation.ACTION_MOVE, seen[0].getAcceptedAction(),
                "and the action being performed is the one that was chosen, not the copy a "
                        + "source allowing both would default to");
    }

    @FormTest
    void aSecondFingerIsAPinchRatherThanADragToHandOver() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("row"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            form.pointerDragged(new int[]{x + 200, x + 260}, new int[]{y + 200, y + 40});
            assertNull(implementation.getStartedNativeDrag(),
                    "two pointers are a pinch or a two-finger scroll, not something to hand to "
                            + "the operating system as a drag");

            form.pointerReleased(x + 200, y + 200);
        } finally {
            // As everywhere else here: a session left active wedges every test after this one,
            // so a failure has to be reported as one failure.
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void theScalarDragOverloadStartsADragToo() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("row"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            // Not the overload the ports drive, but public API an application may call, and the
            // two must not diverge again.
            form.pointerDragged(x + 200, y + 200);
            assertNotNull(implementation.getStartedNativeDrag());
        } finally {
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aReleaseDoesNotDiscardWhatAReentrantPressStaged() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        int pointerType = implementation.getPointerType();
        try {
            final Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("dragged out"));
            Container clicked = new Container();
            clicked.add(new Label("release here"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.add(BorderLayout.NORTH, clicked);
            form.revalidate();

            final int sx = source.getAbsoluteX() + 10;
            final int sy = source.getAbsoluteY() + 10;
            int cx = clicked.getAbsoluteX() + 5;
            int cy = clicked.getAbsoluteY() + 5;
            final boolean[] reentered = new boolean[1];

            implementation.setPointerType(PointerEvent.TYPE_STYLUS);
            clicked.addStylusListener(ev -> {
                if (ev.getEventType() == ActionEvent.Type.PointerReleased) {
                    // The stylus callback is application code and may open a nested event
                    // loop -- a dialog -- inside which a whole new press is dispatched.
                    reentered[0] = true;
                    form.pointerPressed(sx, sy);
                }
            });

            form.pointerPressed(cx, cy);
            form.pointerReleased(cx, cy);

            assertTrue(reentered[0], "the stylus release reached the listener");
            assertTrue(NativeDragAndDrop.pointerDragged(sx + 200, sy + 200),
                    "the release belongs to the press that is ending, not to the one that "
                            + "started while its callback was running");
            assertNotNull(implementation.getStartedNativeDrag(),
                    "the new gesture keeps the operation its own press staged");
            assertSame(source, implementation.getStartedNativeDrag().getSource());
        } finally {
            // The gesture above really started a session, so end it here or the next test's
            // startDrag finds one already running.
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            NativeDragAndDrop.gestureCancelled();
            implementation.setPointerType(pointerType);
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aWindowReleaseDoesNotDiscardWhatAReentrantPressStaged() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        implementation.setMultiWindowSupported(true);
        int pointerType = implementation.getPointerType();
        final Window w = new Window("holds a drag source");
        try {
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("dragged out"));
            Container clicked = new Container();
            clicked.add(new Label("release here"));
            w.setLayout(new BorderLayout());
            w.add(BorderLayout.CENTER, source);
            w.add(BorderLayout.NORTH, clicked);
            w.show();
            flushSerialCalls();

            final int sx = source.getAbsoluteX() + 10;
            final int sy = source.getAbsoluteY() + 10;
            int cx = clicked.getAbsoluteX() + 5;
            int cy = clicked.getAbsoluteY() + 5;
            final boolean[] reentered = new boolean[1];

            implementation.setPointerType(PointerEvent.TYPE_STYLUS);
            clicked.addStylusListener(ev -> {
                if (ev.getEventType() == ActionEvent.Type.PointerReleased) {
                    reentered[0] = true;
                    w.pointerPressed(sx, sy);
                }
            });

            w.pointerPressed(cx, cy);
            w.pointerReleased(cx, cy);

            assertTrue(reentered[0], "the stylus release reached the listener");
            assertNotNull(implementation.getPreparedNativeDrag(),
                    "the window keeps the operation the reentrant press staged, exactly as it "
                            + "keeps that press's own teardown token");
        } finally {
            implementation.setPointerType(pointerType);
            NativeDragAndDrop.gestureCancelled();
            w.dispose();
            flushSerialCalls();
            implementation.setMultiWindowSupported(false);
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aClickOnANativeDragSourceDragsNothing() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("dragged out"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            form.pointerReleased(x, y);
            assertNull(implementation.getStartedNativeDrag());
            assertEquals(1, implementation.getCancelledNativeDrags(),
                    "the staged payload is dropped, so a later gesture elsewhere cannot start this drag");
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aPlatformThatOwnsTheGestureKeepsTheStagedOperationUntilItStarts() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        // startNativeDrag refuses, which is what a port whose operating system owns the drag
        // gesture looks like: iOS starts the session from its own long press and announces it
        // afterwards through dragSessionStarted().
        implementation.setNativeDragStartRefused(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            NativeDragOperation op = new NativeDragOperation("dragged out");
            source.setNativeDragOperation(op);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            assertNull(NativeDragAndDrop.getActiveDrag(),
                    "the port refused, so no session is running yet");
            // A second drag packet must not offer the same gesture again.
            drag(form, x + 220, y + 220);

            assertSame(op, NativeDragAndDrop.dragSessionStarted(),
                    "the staged operation is still there for the platform's own recognizer");
            assertSame(op, NativeDragAndDrop.getActiveDrag());
            assertNull(NativeDragAndDrop.dragSessionStarted(),
                    "and it is only handed over once");

            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
            assertEquals(NativeDragOperation.ACTION_COPY, op.getPerformedAction());
        } finally {
            implementation.setNativeDragStartRefused(false);
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
        }
    }

    @FormTest
    void aSecondPressDoesNotStealAGestureThePlatformHasBeenOffered() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        // The port refuses to start, which is what a platform whose own recognizer owns the
        // drag gesture looks like: the session begins later, out of that recognizer, and takes
        // whatever is staged.
        implementation.setNativeDragStartRefused(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container dragged = new Container();
            NativeDragOperation carried = new NativeDragOperation("the finger that is dragging");
            dragged.setNativeDragOperation(carried);
            Container touched = new Container();
            touched.add(new Label("pressed by the other finger"));
            touched.setNativeDragOperation(new NativeDragOperation("never dragged"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, dragged);
            form.add(BorderLayout.NORTH, touched);
            form.revalidate();

            int x = dragged.getAbsoluteX() + 10;
            int y = dragged.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            assertNull(NativeDragAndDrop.getActiveDrag(), "the port refused, so nothing runs yet");

            // A second finger comes down on another drag source while the first gesture waits
            // for the platform's recognizer.
            form.pointerPressed(touched.getAbsoluteX() + 5, touched.getAbsoluteY() + 5);

            assertSame(carried, NativeDragAndDrop.dragSessionStarted(),
                    "the session belongs to the gesture that was offered: taking the later "
                            + "press's operation exports the wrong component's payload, and "
                            + "reports a move against it");
            assertSame(dragged, NativeDragAndDrop.getActiveDrag().getSource());
        } finally {
            implementation.setNativeDragStartRefused(false);
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            NativeDragAndDrop.gestureCancelled();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aRefusedPressDoesNotRetargetASharedOperation() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        implementation.setNativeDragStartRefused(true);
        try {
            Form form = Display.getInstance().getCurrent();
            // One operation, handed to both components -- which is what a source that owns a
            // row of them does.
            NativeDragOperation shared = new NativeDragOperation("one payload, two components");
            Container dragged = new Container();
            dragged.setNativeDragOperation(shared);
            Container touched = new Container();
            touched.add(new Label("pressed by the other finger"));
            touched.setNativeDragOperation(shared);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, dragged);
            form.add(BorderLayout.NORTH, touched);
            form.revalidate();

            int x = dragged.getAbsoluteX() + 10;
            int y = dragged.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            form.pointerPressed(touched.getAbsoluteX() + 5, touched.getAbsoluteY() + 5);

            assertSame(shared, NativeDragAndDrop.dragSessionStarted());
            assertSame(dragged, shared.getSource(),
                    "the press that was refused the staging slot must not leave the operation "
                            + "pointing at its own component: a move completes against whatever "
                            + "the operation names, and that is whose data goes");
        } finally {
            implementation.setNativeDragStartRefused(false);
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
            flushSerialCalls();
            NativeDragAndDrop.gestureCancelled();
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void anOperationThatPermitsNothingNeverBecomesADrag() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            NativeDragOperation op = new NativeDragOperation("nothing may be done with me")
                    .setAllowedActions(NativeDragOperation.ACTION_NONE);
            source.setNativeDragOperation(op);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            assertNull(implementation.getStartedNativeDrag(),
                    "no receiver could accept it, so there is no drag to run");
            assertFalse(NativeDragAndDrop.startDrag(source, op),
                    "and asking for one directly is refused on the same terms");
            assertNull(NativeDragAndDrop.dragSessionStarted(),
                    "a platform whose own recognizer fires later gets the same answer -- a "
                            + "session that can never complete would wedge every drag after it");
            assertNull(NativeDragAndDrop.getActiveDrag());

            form.pointerReleased(x + 200, y + 200);
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aDeferredSessionGivesTheSourceBackTheVisibilityTheLightweightDragTook() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        // The port refuses the start, so the gesture carries on as a lightweight drag -- which
        // hides the source and carries its image -- until the platform's own recognizer fires.
        implementation.setNativeDragStartRefused(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setDraggable(true);
            NativeDragOperation op = new NativeDragOperation("dragged out");
            source.setNativeDragOperation(op);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            assertFalse(source.isVisible(),
                    "the lightweight drag took the gesture and hid the source it is carrying");

            assertSame(op, NativeDragAndDrop.dragSessionStarted());
            flushSerialCalls();
            assertTrue(source.isVisible(),
                    "the native session draws its own preview and never runs the lightweight "
                            + "drop, so nothing else would ever make the source visible again");

            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragStartRefused(false);
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
        }
    }

    @FormTest
    void aDraggedChildGetsItsVisibilityBackWhenItsAncestorIsTheSource() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            // A drag source is found by walking up from the press, so pressing the draggable
            // child stages the ancestor. The lightweight drag that a small motion starts still
            // belongs to the child.
            Container source = new Container(new BorderLayout());
            source.setNativeDragOperation(new NativeDragOperation("the ancestor is the source"));
            Container child = new Container();
            child.setDraggable(true);
            source.add(BorderLayout.CENTER, child);
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = child.getAbsoluteX() + 5;
            int y = child.getAbsoluteY() + 5;
            form.pointerPressed(x, y);
            drag(form, x + 1, y + 1);
            assertFalse(child.isVisible(),
                    "a motion too small to be a native drag starts the child's lightweight one, "
                            + "which hides the child while it carries its image");
            assertSame(child, form.getDraggedComponent());

            drag(form, x + 200, y + 200);
            assertSame(source, implementation.getStartedNativeDrag().getSource());
            assertTrue(child.isVisible(),
                    "the operating system owns the gesture now and no lightweight drop will "
                            + "ever run, so cancelling only the source left the child hidden");
            assertNull(form.getDraggedComponent());

            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();
        } finally {
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void aReleaseAfterARefusedStartDoesNotLeaveTheDragArmed() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(true);
        implementation.setNativeDragStartRefused(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Container source = new Container();
            source.setNativeDragOperation(new NativeDragOperation("dragged out"));
            form.setLayout(new BorderLayout());
            form.add(BorderLayout.CENTER, source);
            form.revalidate();

            int x = source.getAbsoluteX() + 10;
            int y = source.getAbsoluteY() + 10;
            form.pointerPressed(x, y);
            drag(form, x + 200, y + 200);
            form.pointerReleased(x + 200, y + 200);
            assertNull(NativeDragAndDrop.dragSessionStarted(),
                    "a gesture that ended cannot be turned into a drag by a later recognizer");
        } finally {
            implementation.setNativeDragStartRefused(false);
            implementation.setNativeDragAndDropSupported(false);
            implementation.resetNativeDragState();
        }
    }

    @FormTest
    void withoutPlatformSupportTheGestureIsLeftAlone() {
        implementation.resetNativeDragState();
        implementation.setNativeDragAndDropSupported(false);
        Form form = Display.getInstance().getCurrent();
        Container source = new Container();
        source.setNativeDragOperation(new NativeDragOperation("dragged out"));
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, source);
        form.revalidate();

        int x = source.getAbsoluteX() + 10;
        int y = source.getAbsoluteY() + 10;
        form.pointerPressed(x, y);
        drag(form, x + 200, y + 200);
        assertNull(implementation.getPreparedNativeDrag());
        assertNull(implementation.getStartedNativeDrag());
        form.pointerReleased(x + 200, y + 200);
    }
}
