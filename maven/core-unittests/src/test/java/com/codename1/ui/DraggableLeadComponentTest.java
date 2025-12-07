package com.codename1.ui;

import com.codename1.components.SpanButton;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class DraggableLeadComponentTest extends UITestBase {

    private static final int DRAG_DELTA = 25;

    @FormTest
    void draggableContainerStartsDragWithoutLead() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button button = new Button("Normal Button, no lead");
        button.setPreferredSize(new Dimension(200, 48));
        Button other = new Button("Other");
        other.setPreferredSize(new Dimension(120, 48));
        Container draggableContainer = BorderLayout.centerCenterEastWest(button, other, null);
        draggableContainer.setDraggable(true);
        form.add(draggableContainer);
        form.revalidate();

        Component draggedComponent = performPressDragAndReadDragged(form, button);
        assertEquals(draggableContainer, draggedComponent, "Draggable containers should start dragging even without a lead component");
    }

    @FormTest
    void draggableContainerWithLeadButtonStartsDrag() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button button = new Button("Normal Button, lead");
        button.setPreferredSize(new Dimension(200, 48));
        Button other = new Button("Other");
        other.setPreferredSize(new Dimension(120, 48));
        Container draggableContainer = BorderLayout.centerCenterEastWest(button, other, null);
        draggableContainer.setDraggable(true);
        draggableContainer.setLeadComponent(button);
        form.add(draggableContainer);
        form.revalidate();

        Component draggedComponent = performPressDragAndReadDragged(form, button);
        assertEquals(draggableContainer, draggedComponent, "Lead component should initiate a drag on its container");
    }

    @FormTest
    void draggableContainerWithSpanButtonLeadStartsDrag() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        SpanButton spanButton = new SpanButton("SpanButton, lead");
        spanButton.setPreferredSize(new Dimension(200, 48));
        Button other = new Button("Other");
        other.setPreferredSize(new Dimension(120, 48));
        Container draggableContainer = BorderLayout.centerCenterEastWest(spanButton, other, null);
        draggableContainer.setDraggable(true);
        draggableContainer.setLeadComponent(other);
        form.add(draggableContainer);
        form.revalidate();

        Component draggedComponent = performPressDragAndReadDragged(form, other);
        assertEquals(draggableContainer, draggedComponent, "Containers with SpanButton children should still drag when a lead component is provided");
    }

    private Component performPressDragAndReadDragged(Form form, Component interactionComponent) {
        assertTrue(interactionComponent.getWidth() > 0, "Interaction component should be laid out with a width");
        assertTrue(interactionComponent.getHeight() > 0, "Interaction component should be laid out with a height");

        int startX = interactionComponent.getAbsoluteX() + Math.max(1, interactionComponent.getWidth() / 2);
        int startY = interactionComponent.getAbsoluteY() + Math.max(1, interactionComponent.getHeight() / 2);

        implementation.dispatchPointerPress(startX, startY);
        implementation.dispatchPointerDrag(startX + DRAG_DELTA, startY + DRAG_DELTA);
        implementation.dispatchPointerDrag(startX + (DRAG_DELTA * 2), startY + (DRAG_DELTA * 2));
        Component draggedComponent = form.getDraggedComponent();
        implementation.dispatchPointerRelease(startX + (DRAG_DELTA * 2), startY + (DRAG_DELTA * 2));
        return draggedComponent;
    }
}
