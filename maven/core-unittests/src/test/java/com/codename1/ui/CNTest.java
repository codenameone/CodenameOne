package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.Style;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CNTest extends UITestBase {

    @FormTest
    void delegatesDisplayState() {
        Display display = Display.getInstance();
        Form current = display.getCurrent();
        current.removeAll();
        current.setTitle("CN Proxy Test");
        CN.setProperty("proxyKey", "proxyValue");

        assertSame(current, CN.getCurrentForm());
        assertEquals(display.getDisplayWidth(), CN.getDisplayWidth());
        assertEquals(display.getDisplayHeight(), CN.getDisplayHeight());
        assertEquals("proxyValue", display.getProperty("proxyKey", null));
        assertEquals(display.convertToPixels(3, true), CN.convertToPixels(3, true));
        assertEquals(display.convertToPixels(2.5f, Style.UNIT_TYPE_DIPS, true), CN.convertToPixels(2.5f, Style.UNIT_TYPE_DIPS, true));
        assertEquals(display.getDeviceDensity(), CN.getDeviceDensity());
        assertEquals(display.isPortrait(), CN.isPortrait());
    }

    @FormTest
    void bookmarkAndSerialCallsExecute() {
        AtomicInteger invoked = new AtomicInteger();
        CN.setBookmark(invoked::incrementAndGet);
        CN.restoreToBookmark();
        assertEquals(1, invoked.get(), "Bookmark runnable should be invoked on restore");

        invoked.set(0);
        CN.callSerially(invoked::incrementAndGet);
        flushSerialCalls();
        assertEquals(1, invoked.get(), "callSerially should enqueue runnable on EDT");

        RuntimeException thrown = assertThrows(RuntimeException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                CN.callSeriallyAndWait(new Runnable() {
                    @Override
                    public void run() {
                        invoked.incrementAndGet();
                    }
                });
            }
        }, "callSeriallyAndWait should not be allowed on the EDT");
        assertTrue(thrown.getMessage().contains("MUST NOT"), "Exception message should indicate EDT restriction");
        assertEquals(0, invoked.get(), "Runnable must not execute when callSeriallyAndWait is invoked on EDT");
    }

    @FormTest
    void invokeBlocksAndReturnsResult() {
        AtomicInteger invoked = new AtomicInteger();
        CN.invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertEquals(1, invoked.get(), "invokeAndBlock should execute runnable synchronously");

        final AtomicInteger resultHolder = new AtomicInteger();
        Integer result = CN.invokeWithoutBlockingWithResultSync(resultHolder::incrementAndGet);
        assertEquals(1, resultHolder.get());
        assertEquals(Integer.valueOf(1), result);
    }
}
