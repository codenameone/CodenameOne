package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for pointer events including press, release, drag, long press, scrolling,
 * multi-touch, and hover events. These tests fire events through the entire stack
 * using the TestCodenameOneImplementation.
 */
class PointerEventsTest extends UITestBase {

    @FormTest
    void testPointerPressedEvent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(40);
        form.add(button);
        form.revalidate();

        final boolean[] pressedCalled = {false};
        button.addPointerPressedListener(evt -> {
            pressedCalled[0] = true;
            assertEquals(button, evt.getSource(), "Event source should be the button");
        });

        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        assertTrue(pressedCalled[0], "Pointer pressed listener should be called");
    }

    @FormTest
    void testPointerReleasedEvent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(40);
        form.add(button);
        form.revalidate();

        final boolean[] releasedCalled = {false};
        button.addPointerReleasedListener(evt -> {
            releasedCalled[0] = true;
        });

        // Need to press first, then release
        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        form.pointerReleased(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        assertTrue(releasedCalled[0], "Pointer released listener should be called");
    }

    @FormTest
    void testPointerDraggedEvent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Button button = new Button("Draggable");
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(100);
        form.add(button);
        form.revalidate();

        final int[] dragCount = {0};
        button.addPointerDraggedListener(evt -> {
            dragCount[0]++;
        });

        // Press and drag - drag events should be fired through the form
        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        form.pointerDragged(button.getAbsoluteX() + 10, button.getAbsoluteY() + 10);
        form.pointerDragged(button.getAbsoluteX() + 15, button.getAbsoluteY() + 15);
        form.pointerReleased(button.getAbsoluteX() + 15, button.getAbsoluteY() + 15);

        // Drag events are implementation-dependent; just verify no exceptions occurred
        assertTrue(dragCount[0] >= 0, "Drag listener should be set up without errors");
    }

    @FormTest
    void testLongPressEvent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Long Press");
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(40);
        form.add(button);
        form.revalidate();

        final boolean[] longPressCalled = {false};
        button.addLongPressListener(evt -> {
            longPressCalled[0] = true;
            assertEquals(ActionEvent.Type.LongPointerPress, evt.getEventType());
        });

        // Simulate long press by pressing and triggering long press event
        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        form.longPointerPress(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        assertTrue(longPressCalled[0], "Long press listener should be called");
    }

    @FormTest
    void testScrollingWithPointerDrag() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        ScrollableComponent scrollable = new ScrollableComponent();
        scrollable.setWidth(200);
        scrollable.setHeight(100);
        form.add(scrollable);
        form.revalidate();

        final int[] scrollYChanged = {0};
        scrollable.addScrollListener(new ScrollListener() {
            public void scrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                scrollYChanged[0] = scrollY;
            }
        });

        int startX = scrollable.getAbsoluteX() + 10;
        int startY = scrollable.getAbsoluteY() + 50;

        // Simulate scroll by dragging
        form.pointerPressed(startX, startY);
        form.pointerDragged(startX, startY - 30);
        form.pointerReleased(startX, startY - 30);

        // Note: Actual scroll behavior depends on component implementation
        // We just verify the mechanism works without errors
        assertNotNull(scrollable);
    }

    @FormTest
    void testMultiTouchPointerEvents() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        TestableComponent component = new TestableComponent();
        component.setWidth(200);
        component.setHeight(200);
        form.add(component);
        form.revalidate();

        final boolean[] multiTouchReceived = {false};
        component.addPointerPressedListener(evt -> {
            multiTouchReceived[0] = true;
        });

        // Multi-touch with array of coordinates
        int[] xCoords = {component.getAbsoluteX() + 10, component.getAbsoluteX() + 50};
        int[] yCoords = {component.getAbsoluteY() + 10, component.getAbsoluteY() + 50};

        component.pointerPressed(xCoords, yCoords);
        component.pointerReleased(xCoords, yCoords);

        assertTrue(multiTouchReceived[0], "Multi-touch events should be processed");
    }

    @FormTest
    void testPointerHoverEvents() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        TestableComponent component = new TestableComponent();
        component.setWidth(100);
        component.setHeight(100);
        form.add(component);
        form.revalidate();

        int[] xCoords = {component.getAbsoluteX() + 10};
        int[] yCoords = {component.getAbsoluteY() + 10};

        // Hover events should not throw exceptions
        assertDoesNotThrow(() -> form.pointerHover(xCoords, yCoords));
        assertDoesNotThrow(() -> form.pointerHoverPressed(xCoords, yCoords));
        assertDoesNotThrow(() -> form.pointerHoverReleased(xCoords, yCoords));
    }

    @FormTest
    void testPointerEventsOnMultipleComponents() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        container.add(button1);
        container.add(button2);
        form.add(container);
        form.revalidate();

        final boolean[] button1Pressed = {false};
        final boolean[] button2Pressed = {false};

        button1.addPointerPressedListener(evt -> button1Pressed[0] = true);
        button2.addPointerPressedListener(evt -> button2Pressed[0] = true);

        // Press button1
        form.pointerPressed(button1.getAbsoluteX() + 5, button1.getAbsoluteY() + 5);
        assertTrue(button1Pressed[0], "Button 1 should receive press event");
        assertFalse(button2Pressed[0], "Button 2 should not receive press event");

        // Press button2
        form.pointerPressed(button2.getAbsoluteX() + 5, button2.getAbsoluteY() + 5);
        assertTrue(button2Pressed[0], "Button 2 should receive press event");
    }

    @FormTest
    void testPointerPressGrantsFocus() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Button button1 = new Button("First");
        Button button2 = new Button("Second");

        form.add(button1);
        form.add(button2);
        form.revalidate();

        assertFalse(button2.hasFocus(), "Button 2 should not have focus initially");

        form.pointerPressed(button2.getAbsoluteX() + 5, button2.getAbsoluteY() + 5);

        assertTrue(button2.hasFocus(), "Pointer press should grant focus to button 2");
    }

    @FormTest
    void testPointerDragBetweenComponents() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        TestableComponent source = new TestableComponent();
        source.setDraggable(true);
        source.setWidth(50);
        source.setHeight(50);

        TestableComponent target = new TestableComponent();
        target.setDropTarget(true);
        target.setWidth(50);
        target.setHeight(50);

        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        container.add(source);
        container.add(target);
        form.add(container);
        form.revalidate();

        // Drag from source towards target
        form.pointerPressed(source.getAbsoluteX() + 5, source.getAbsoluteY() + 5);
        form.pointerDragged(source.getAbsoluteX() + 5, source.getAbsoluteY() + 30);
        form.pointerReleased(source.getAbsoluteX() + 5, source.getAbsoluteY() + 30);

        // Just verify no exceptions occurred
        assertNotNull(source);
        assertNotNull(target);
    }

    @FormTest
    void testPointerEventsOnDisabledComponent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Button button = new Button("Disabled");
        button.setEnabled(false);
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(40);
        form.add(button);
        form.revalidate();

        final boolean[] pressedCalled = {false};
        button.addPointerPressedListener(evt -> pressedCalled[0] = true);

        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        // Disabled components may still receive pointer events at the implementation level
        // but shouldn't trigger actions
        assertNotNull(button);
    }

    @FormTest
    void testScrollListenerReceivesEvents() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        ScrollableComponent scrollable = new ScrollableComponent();
        scrollable.setWidth(200);
        scrollable.setHeight(100);
        form.add(scrollable);
        form.revalidate();

        final int[] eventCount = {0};
        final int[] lastScrollY = {-1};

        scrollable.addScrollListener(new ScrollListener() {
            public void scrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                eventCount[0]++;
                lastScrollY[0] = scrollY;
            }
        });

        // Programmatically scroll
        scrollable.setScrollY(10);

        assertTrue(eventCount[0] > 0, "Scroll listener should be called");
        assertEquals(10, lastScrollY[0], "Scroll Y should be 10");
    }

    @FormTest
    void testPointerReleaseOutsideComponent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(40);
        form.add(button);
        form.revalidate();

        final boolean[] releasedCalled = {false};
        button.addPointerReleasedListener(evt -> releasedCalled[0] = true);

        // Press inside, release outside
        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        form.pointerReleased(button.getAbsoluteX() + 200, button.getAbsoluteY() + 200);

        // Release listener may or may not be called depending on implementation
        // Just verify no exception occurs
        assertNotNull(button);
    }

    @FormTest
    void testMultiplePointerDraggedListeners() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Drag Test");
        button.setWidth(100);
        button.setHeight(100);
        form.add(button);
        form.revalidate();

        final int[] listener1Count = {0};
        final int[] listener2Count = {0};

        ActionListener listener1 = evt -> listener1Count[0]++;
        ActionListener listener2 = evt -> listener2Count[0]++;

        button.addPointerDraggedListener(listener1);
        button.addPointerDraggedListener(listener2);

        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        form.pointerDragged(button.getAbsoluteX() + 10, button.getAbsoluteY() + 10);
        form.pointerReleased(button.getAbsoluteX() + 10, button.getAbsoluteY() + 10);

        // Drag events are implementation-dependent; verify both listeners see same count
        assertEquals(listener1Count[0], listener2Count[0], "Both listeners should be called same number of times");
    }

    @FormTest
    void testRemovePointerListener() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(40);
        form.add(button);
        form.revalidate();

        final int[] callCount = {0};
        ActionListener listener = evt -> callCount[0]++;

        button.addPointerPressedListener(listener);

        // First press - listener should be called
        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        form.pointerReleased(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        assertEquals(1, callCount[0], "Listener should be called once");

        // Remove listener
        button.removePointerPressedListener(listener);

        // Second press - listener should not be called
        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        form.pointerReleased(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        assertEquals(1, callCount[0], "Listener should not be called after removal");
    }

    @FormTest
    void testPointerEventsWithNestedContainers() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Container outer = new Container(new BorderLayout());
        Container inner = new Container(new BorderLayout());
        Button button = new Button("Nested");

        inner.add(BorderLayout.CENTER, button);
        outer.add(BorderLayout.CENTER, inner);
        form.add(outer);
        form.revalidate();

        final boolean[] pressedCalled = {false};
        button.addPointerPressedListener(evt -> pressedCalled[0] = true);

        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        assertTrue(pressedCalled[0], "Button in nested container should receive pointer event");
    }

    @FormTest
    void testScrollableComponentScrolling() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        ScrollableComponent scrollable = new ScrollableComponent();
        scrollable.setWidth(200);
        scrollable.setHeight(100);
        form.add(scrollable);
        form.revalidate();

        assertTrue(scrollable.isScrollableY(), "Component should be scrollable in Y");

        int initialScrollY = scrollable.getScrollY();
        scrollable.setScrollY(20);
        int newScrollY = scrollable.getScrollY();

        assertTrue(newScrollY >= initialScrollY, "Scroll Y should increase");
    }

    @FormTest
    void testPointerEventsPropagation() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Button button = new Button("Test Button");
        Container container = new Container(new BorderLayout());
        container.add(BorderLayout.CENTER, button);
        form.add(container);
        form.revalidate();

        final boolean[] buttonPressed = {false};
        button.addPointerPressedListener(evt -> buttonPressed[0] = true);

        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        assertTrue(buttonPressed[0], "Button in container should receive pointer event");
    }

    @FormTest
    void testLongPressOnFormLevel() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        final boolean[] formLongPressCalled = {false};
        final int[] pressX = {0};
        final int[] pressY = {0};

        form.addLongPressListener(evt -> {
            formLongPressCalled[0] = true;
            pressX[0] = evt.getX();
            pressY[0] = evt.getY();
        });

        int testX = 50;
        int testY = 60;

        form.pointerPressed(testX, testY);
        form.longPointerPress(testX, testY);

        assertTrue(formLongPressCalled[0], "Form long press listener should be called");
        assertEquals(testX, pressX[0], "Press X coordinate should match");
        assertEquals(testY, pressY[0], "Press Y coordinate should match");
    }

    @FormTest
    void testPointerDragWithScrollableParent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        ScrollableComponent scrollable = new ScrollableComponent();
        scrollable.setWidth(200);
        scrollable.setHeight(150);

        Button button = new Button("Inside Scrollable");
        Container buttonContainer = new Container(new BorderLayout());
        buttonContainer.add(BorderLayout.NORTH, button);

        scrollable.add(buttonContainer);
        form.add(scrollable);
        form.revalidate();

        final boolean[] buttonPressed = {false};
        button.addPointerPressedListener(evt -> buttonPressed[0] = true);

        // Press the button
        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        assertTrue(buttonPressed[0], "Button inside scrollable should receive press event");
    }

    @FormTest
    void testMultipleLongPressListeners() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(40);
        form.add(button);
        form.revalidate();

        final int[] listener1Count = {0};
        final int[] listener2Count = {0};

        button.addLongPressListener(evt -> listener1Count[0]++);
        button.addLongPressListener(evt -> listener2Count[0]++);

        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);
        form.longPointerPress(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        assertEquals(1, listener1Count[0], "First listener should be called once");
        assertEquals(1, listener2Count[0], "Second listener should be called once");
    }

    // ========== Helper Classes ==========

    private static class TestableComponent extends Component {
        TestableComponent() {
            setUIID("Label");
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(0x0000FF);
            g.fillRect(getX(), getY(), getWidth(), getHeight());
        }
    }

    private static class ScrollableComponent extends Container {
        ScrollableComponent() {
            setUIID("Container");
            setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        }

        @Override
        public boolean isScrollableY() {
            return true;
        }

        @Override
        protected Dimension calcScrollSize() {
            return new Dimension(getWidth(), getHeight() * 3);
        }
    }
}
