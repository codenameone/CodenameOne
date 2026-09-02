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
import com.codename1.ui.events.ActionEvent;
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
            form.pointerDragged(x + 200, y + 200);
            Image first = op.getDragImage();
            assertNotNull(first);
            NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_COPY);
            flushSerialCalls();

            // The same reusable operation, dragged again. The generated snapshot must not have
            // become the operation's permanent image: the component may look different now and
            // the press landed somewhere else.
            form.pointerPressed(x + 4, y + 4);
            form.pointerDragged(x + 200, y + 200);
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
            form.pointerDragged(x + 200, y + 200);
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

            form.pointerDragged(x + 200, y + 200);
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

            form.pointerDragged(x + 200, y + 200);
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
            form.pointerDragged(x + 200, y + 200);
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

            form.pointerDragged(x + 200, y + 200);
            assertNull(implementation.getStartedNativeDrag());
            form.pointerReleased(x + 200, y + 200);

            // Enabled again, the very same component drags.
            source.setEnabled(true);
            form.pointerPressed(x, y);
            assertNotNull(implementation.getPreparedNativeDrag());
            form.pointerDragged(x + 200, y + 200);
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
            form.pointerDragged(x + 200, y + 200);
            assertNull(NativeDragAndDrop.getActiveDrag(),
                    "the port refused, so no session is running yet");
            // A second drag packet must not offer the same gesture again.
            form.pointerDragged(x + 220, y + 220);

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
            form.pointerDragged(x + 200, y + 200);
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
            form.pointerDragged(x + 200, y + 200);
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
            form.pointerDragged(x + 200, y + 200);
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
        form.pointerDragged(x + 200, y + 200);
        assertNull(implementation.getPreparedNativeDrag());
        assertNull(implementation.getStartedNativeDrag());
        form.pointerReleased(x + 200, y + 200);
    }
}
