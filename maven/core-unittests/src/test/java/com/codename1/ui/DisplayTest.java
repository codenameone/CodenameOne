package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DisplayTest extends UITestBase {

    public static void flushEdt() {
        final Display display = Display.getInstance();
        if (display.isEdt()) {
            display.flushEdt();
            return;
        }

        display.callSeriallyAndWait(new Runnable() {
            public void run() {
                display.flushEdt();
            }
        });
    }

    @AfterEach
    void resetStatics() {
        Container.blockOverdraw = false;
        Component.revalidateOnStyleChange = true;
    }

    @Test
    void testConvertToPixelsHandlesVariousUnits() {
        Display display = Display.getInstance();

        assertEquals(Math.round(2f * Font.getDefaultFont().getHeight()), display.convertToPixels(2f, Style.UNIT_TYPE_REM));
        assertEquals(Math.round(25f / 100f * CN.getDisplayHeight()), display.convertToPixels(25f, Style.UNIT_TYPE_VH));
        assertEquals(Math.round(40f / 100f * CN.getDisplayWidth()), display.convertToPixels(40f, Style.UNIT_TYPE_VW));
        assertEquals(Math.round(10f / 100f * Math.min(CN.getDisplayWidth(), CN.getDisplayHeight())),
                display.convertToPixels(10f, Style.UNIT_TYPE_VMIN));
        assertEquals(Math.round(60f / 100f * Math.max(CN.getDisplayWidth(), CN.getDisplayHeight())),
                display.convertToPixels(60f, Style.UNIT_TYPE_VMAX));
        assertEquals(display.convertToPixels(2.5f), display.convertToPixels(2.5f, Style.UNIT_TYPE_DIPS));
        assertEquals(540, display.convertToPixels(50f, Style.UNIT_TYPE_SCREEN_PERCENTAGE, true));
        assertEquals(960, display.convertToPixels(50f, Style.UNIT_TYPE_SCREEN_PERCENTAGE, false));
        assertEquals(7, display.convertToPixels(7f, (byte) 99));
    }

    @Test
    void testSetPropertyHandlesSpecialKeys() {
        Display display = Display.getInstance();

        display.setProperty("AppArg", "launch");
        assertEquals("launch", display.getProperty("AppArg", ""));

        display.setProperty("blockOverdraw", "ignored");
        assertTrue(Container.blockOverdraw);

        display.setProperty("blockCopyPaste", "true");
        assertTrue(implementation.isBlockCopyAndPaste());

        display.setProperty("Component.revalidateOnStyleChange", "false");
        assertFalse(Component.revalidateOnStyleChange);

        display.setProperty("Component.revalidateOnStyleChange", "TRUE");
        assertTrue(Component.revalidateOnStyleChange);
    }

    @Test
    void testDebugRunnable() {
        Display display = Display.getInstance();
        boolean oldEnable = display.isEnableAsyncStackTraces();
        try {
            display.setEnableAsyncStackTraces(true);
            assertTrue(display.isEnableAsyncStackTraces());

            final boolean[] executed = {false};
            display.callSeriallyAndWait(new Runnable() {
                public void run() {
                    executed[0] = true;
                }
            });
            assertTrue(executed[0]);

            // Testing exception propagation behavior is tricky as it just logs.
            // But running callSerially with async traces enabled exercises DebugRunnable construction and run.

        } finally {
            display.setEnableAsyncStackTraces(oldEnable);
        }
    }
}
