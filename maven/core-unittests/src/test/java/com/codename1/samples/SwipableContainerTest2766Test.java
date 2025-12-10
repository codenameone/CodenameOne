package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.SwipeableContainer;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwipableContainerTest2766Test extends UITestBase {

    @FormTest
    public void swipeableContainersDoNotDragParentList() {
        Form form = new Form("Welcome", new BorderLayout());
        Container list = new Container(BoxLayout.y());
        for (int i = 0; i < 8; i++) {
            list.add(createListElement(i));
        }
        list.setScrollableY(true);
        form.add(BorderLayout.CENTER, list);
        SwipeableContainer footer = new SwipeableContainer(null, new SpanLabel("SOUTHSWIPE"), new SpanLabel("SOUTH CONTAINER"));
        form.add(BorderLayout.SOUTH, footer);
        form.show();
        DisplayTest.flushEdt();
        flushSerialCalls();

        SwipeableContainer target = (SwipeableContainer) list.getComponentAt(0);
        ensureSized(form, target);
        int initialScrollY = list.getScrollY();

        swipeLeft(target);
    }

    private SwipeableContainer createListElement(int index) {
        Label content = new Label("ListElement " + index + " with filler text to span multiple lines");
        content.getUnselectedStyle().setPadding(5, 5, 5, 5);
        content.setPreferredH(180);
        return new SwipeableContainer(null, new Label("SWIPE"), content);
    }

    private void swipeLeft(SwipeableContainer container) {
        int midY = container.getAbsoluteY() + container.getHeight() / 2;
        int startX = container.getAbsoluteX() + container.getWidth() - 5;
        int endX = container.getAbsoluteX() + container.getWidth() / 2;
        implementation.dispatchPointerPress(startX, midY);
        implementation.dispatchPointerDrag(endX, midY);
        implementation.dispatchPointerRelease(endX, midY);
    }

    private void ensureSized(Form form, com.codename1.ui.Component component) {
        for (int i = 0; i < 5 && (component.getWidth() <= 0 || component.getHeight() <= 0); i++) {
            form.revalidate();
            DisplayTest.flushEdt();
            flushSerialCalls();
        }
    }
}
