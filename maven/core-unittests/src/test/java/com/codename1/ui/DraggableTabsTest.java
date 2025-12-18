package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestUtils;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DraggableTabsTest extends UITestBase {

    @FormTest
    void draggingTabHeaderReordersTabs() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(new BorderLayout());

        Tabs tabs = new Tabs();
        Label tabOne = new Label("Tab 1");
        Label tabTwo = new Label("Tab 2");
        Label tabThree = new Label("Tab 3");
        Label tabFour = new Label("Tab 4");
        tabs.addTab("T1", tabOne);
        tabs.addTab("T2", tabTwo);
        tabs.addTab("T3", tabThree);
        tabs.addTab("T4", tabFour);

        enableTabDragging(tabs);

        form.add(BorderLayout.CENTER, tabs);
        form.revalidate();

        Container headers = tabs.getTabsContainer();
        Component firstHeader = headers.getComponentAt(0);
        Component thirdHeader = headers.getComponentAt(2);

        int startX = firstHeader.getAbsoluteX() + firstHeader.getWidth() / 2;
        int startY = firstHeader.getAbsoluteY() + firstHeader.getHeight() / 2;
        int dragX = startX + 10;
        int dragY = startY + 5;
        int targetX = thirdHeader.getAbsoluteX() + thirdHeader.getWidth() / 2;
        int targetY = thirdHeader.getAbsoluteY() + thirdHeader.getHeight() / 2;

        implementation.dispatchPointerPress(startX, startY);
        implementation.setHasDragStarted(true);
        flushSerialCalls();
        for(int iter = startX ; iter <= targetX ; iter++) {
            implementation.dispatchPointerDrag(iter, startY);
        }
        implementation.dispatchPointerDrag(targetX, targetY);
        implementation.dispatchPointerRelease(targetX, targetY);
        flushSerialCalls();

        assertEquals(4, tabs.getTabCount());
        assertEquals("T2", tabs.getTabTitle(0));
        assertEquals("T1", tabs.getTabTitle(1));
        assertEquals("T3", tabs.getTabTitle(2));
        assertEquals("T4", tabs.getTabTitle(3));
        assertSame(tabTwo, tabs.getTabComponentAt(0));
        assertSame(tabOne, tabs.getTabComponentAt(1));
        assertSame(tabThree, tabs.getTabComponentAt(2));
        assertSame(tabFour, tabs.getTabComponentAt(3));
    }

    private void enableTabDragging(final Tabs tabs) {
        final Container tabsContainer = tabs.getTabsContainer();
        tabsContainer.setDropTarget(true);
        for (final Component header : tabsContainer) {
            header.setDraggable(true);
            header.addDropListener(e -> {
                e.consume();
                int x = e.getX();
                int y = e.getY();
                int sourceIndex = tabsContainer.getComponentIndex(header);
                if (sourceIndex < 0) {
                    return;
                }
                Component destination = tabsContainer.getComponentAt(x, y);
                if (destination == header) {
                    return;
                }
                int destIndex = tabsContainer.getComponentIndex(destination);
                if (destIndex < 0 || destIndex == sourceIndex) {
                    return;
                }
                String title = tabs.getTabTitle(sourceIndex);
                Component content = tabs.getTabComponentAt(sourceIndex);
                tabs.removeTabAt(sourceIndex);
                if (destIndex > sourceIndex) {
                    tabs.insertTab(title, null, content, destIndex - 1);
                } else {
                    tabs.insertTab(title, null, content, destIndex);
                }
                tabsContainer.revalidate();
            });
        }
    }
}
