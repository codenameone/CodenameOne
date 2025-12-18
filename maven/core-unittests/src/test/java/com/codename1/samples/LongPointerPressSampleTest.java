package com.codename1.samples;

import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class LongPointerPressSampleTest extends UITestBase {

    boolean longPressTriggered = false;
    boolean pointerPressedTriggered = false;

    @FormTest
    public void testLongPointerPressSample() {
        Form hi = new Form("Hi World", new BorderLayout());

        Container centerCnt = new Container(BoxLayout.y()) {
            @Override
            public void longPointerPress(int x, int y) {
                longPressTriggered = true;
            }

            @Override
            public void pointerPressed(int x, int y) {
                super.pointerPressed(x, y);
                pointerPressedTriggered = true;
            }
        };
        centerCnt.setFocusable(true);
        hi.add(BorderLayout.CENTER, centerCnt);
        hi.show();
        waitForFormTitle("Hi World");

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        int x = centerCnt.getAbsoluteX() + 10;
        int y = centerCnt.getAbsoluteY() + 10;

        impl.dispatchPointerPress(x, y);
        flushSerialCalls();
        assertTrue(pointerPressedTriggered, "pointerPressed should be triggered");

        centerCnt.longPointerPress(x, y);
        flushSerialCalls();
        assertTrue(longPressTriggered, "longPointerPress should be triggered");

        impl.dispatchPointerRelease(x, y);
    }

    private void waitForFormTitle(String title) {
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 5000) {
            Form f = CN.getCurrentForm();
            if (f != null && title.equals(f.getTitle())) {
                return;
            }
            try { Thread.sleep(50); } catch(Exception e){}
        }
    }
}
