package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for
 * https://github.com/codenameone/CodenameOne/issues/1592
 * -- Form.pointerDragged must not route drag events to a disabled
 * component. pointerPressed and pointerReleased already skip disabled
 * components; pointerDragged did not, which the 2015 reporter flagged as an
 * inconsistency.
 */
class DisabledPointerDraggedRoutingTest extends UITestBase {

    private static class DragCountingComponent extends Component {
        int dragCount;

        @Override
        public void pointerDragged(int x, int y) {
            dragCount++;
            super.pointerDragged(x, y);
        }

        @Override
        public void pointerDragged(int[] x, int[] y) {
            dragCount++;
            super.pointerDragged(x, y);
        }
    }

    @FormTest
    void enabledComponentReceivesDragEvents() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        DragCountingComponent c = new DragCountingComponent();
        c.setEnabled(true);
        form.add(BorderLayout.CENTER, c);
        form.revalidate();

        int x = c.getAbsoluteX() + 5;
        int y = c.getAbsoluteY() + 5;
        form.pointerPressed(x, y);
        form.pointerDragged(x + 30, y + 30);
        form.pointerReleased(x + 30, y + 30);

        assertTrue(c.dragCount > 0,
                "Sanity check: an enabled component must still receive drag events.");
    }

    @FormTest
    void disabledComponentDoesNotReceiveDragEvents() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        DragCountingComponent c = new DragCountingComponent();
        c.setEnabled(false);
        form.add(BorderLayout.CENTER, c);
        form.revalidate();

        int x = c.getAbsoluteX() + 5;
        int y = c.getAbsoluteY() + 5;
        form.pointerPressed(x, y);
        form.pointerDragged(x + 30, y + 30);
        form.pointerReleased(x + 30, y + 30);

        assertEquals(0, c.dragCount,
                "A disabled component must not receive pointerDragged events. "
                        + "pointerPressed and pointerReleased already gate on "
                        + "isEnabled(); pointerDragged must do the same. See #1592.");
    }
}
