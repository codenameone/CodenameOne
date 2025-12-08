package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.GridLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port of the DropListenerSample demo into a regression test that fires drag and drop
 * events through the full Codename One stack using {@link TestCodenameOneImplementation}.
 */
class DropListenerSampleTest extends UITestBase {

    @FormTest
    void dropListenerReceivesDropEventsAndDelegatesToTarget() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new GridLayout(1, 2));

        TrackingContainer draggable = new TrackingContainer();
        draggable.setPreferredSize(new Dimension(80, 80));
        draggable.setDraggable(true);

        TrackingDropTarget dropTarget = new TrackingDropTarget();
        dropTarget.setPreferredSize(new Dimension(80, 80));
        dropTarget.setDropTarget(true);

        form.add(draggable);
        form.add(dropTarget);
        form.revalidate();

        assertTrue(draggable.getWidth() > 0 && draggable.getHeight() > 0,
                "Draggable component should have layout dimensions");
        assertTrue(dropTarget.getWidth() > 0 && dropTarget.getHeight() > 0,
                "Drop target should have layout dimensions");

        final int[] dropListenerCount = {0};
        final ActionEvent[] lastEvent = {null};
        draggable.addDropListener(evt -> {
            dropListenerCount[0]++;
            lastEvent[0] = evt;
        });

        int startX = draggable.getAbsoluteX() + draggable.getWidth() / 2;
        int startY = draggable.getAbsoluteY() + draggable.getHeight() / 2;
        int targetX = dropTarget.getAbsoluteX() + dropTarget.getWidth() / 2;
        int targetY = dropTarget.getAbsoluteY() + dropTarget.getHeight() / 2;

        implementation.dispatchPointerPress(startX, startY);
        implementation.dispatchPointerDrag(startX + draggable.getWidth() / 4, startY);
        implementation.dispatchPointerDrag(startX + draggable.getWidth() / 3, startY + draggable.getHeight() / 6);
        implementation.dispatchPointerDrag((startX + targetX) / 2, (startY + targetY) / 2);
        implementation.dispatchPointerDrag(targetX, targetY);
        implementation.dispatchPointerDrag(targetX, targetY + dropTarget.getHeight() / 4);
        implementation.dispatchPointerRelease(targetX, targetY);

        assertTrue(dropListenerCount[0] > 0, "Drop listener should be invoked when dropping on a target");
        assertNotNull(lastEvent[0], "Drop event should be captured");
        assertEquals(dropTarget, lastEvent[0].getComponent(), "Drop listener should receive the drop target component");
        assertEquals(draggable, lastEvent[0].getSource(), "Source component should be the draggable container");
        assertTrue(dropTarget.getDragOverCount() > 0, "Drag over callbacks should occur on the drop target");
        assertEquals(1, dropTarget.getDropCount(), "Drop target should be notified exactly once");
        assertEquals(draggable, dropTarget.getLastDragged(), "Drop target should receive the dragged component instance");
    }

    private static class TrackingContainer extends Container {
        TrackingContainer() {
            getStyle().setBgTransparency(255);
        }
    }

    private static class TrackingDropTarget extends Container {
        private int dropCount;
        private Component lastDragged;
        private int dragOverCount;

        TrackingDropTarget() {
            getStyle().setBgTransparency(255);
        }

        @Override
        protected boolean draggingOver(Component dragged, int x, int y) {
            dragOverCount++;
            return super.draggingOver(dragged, x, y);
        }

        @Override
        public void drop(Component dragged, int x, int y) {
            dropCount++;
            lastDragged = dragged;
        }

        int getDropCount() {
            return dropCount;
        }

        Component getLastDragged() {
            return lastDragged;
        }

        int getDragOverCount() {
            return dragOverCount;
        }
    }
}
