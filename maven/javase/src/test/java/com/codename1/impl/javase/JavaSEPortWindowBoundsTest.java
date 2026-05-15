package com.codename1.impl.javase;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Guards against regressions in the simulator's window-bounds persistence.
 * The bug that prompted these tests: picking Simulate -> Larger Text ->
 * Extra Extra Extra Large collapsed the frame, the resulting tiny geometry
 * was written to prefs, and every subsequent launch restored the unusable
 * window. The helpers tested here are the choke points that must reject
 * those values on both write and read.
 */
public class JavaSEPortWindowBoundsTest {

    @Test
    public void isUsableWindowBoundsRejectsNull() {
        assertFalse(JavaSEPort.isUsableWindowBounds(null));
    }

    @Test
    public void isUsableWindowBoundsRejectsCollapsedFrame() {
        assertFalse(JavaSEPort.isUsableWindowBounds(new Rectangle(0, 0, 0, 0)));
        assertFalse(JavaSEPort.isUsableWindowBounds(new Rectangle(0, 0, 1, 1)));
        assertFalse(JavaSEPort.isUsableWindowBounds(new Rectangle(0, 0, 50, 50)));
    }

    @Test
    public void isUsableWindowBoundsRejectsJustBelowFloor() {
        // 99 sits just under MIN_PERSISTED_WINDOW_DIMENSION (100). Verifies the
        // floor is enforced strictly, not as "approximately".
        assertFalse(JavaSEPort.isUsableWindowBounds(new Rectangle(0, 0, 99, 200)));
        assertFalse(JavaSEPort.isUsableWindowBounds(new Rectangle(0, 0, 200, 99)));
    }

    @Test
    public void isUsableWindowBoundsRejectsAbsurdSize() {
        // A corrupt or overflowing pref could produce arbitrarily large values.
        // Both dimensions are checked independently.
        assertFalse(JavaSEPort.isUsableWindowBounds(new Rectangle(0, 0, 40000, 800)));
        assertFalse(JavaSEPort.isUsableWindowBounds(new Rectangle(0, 0, 800, 40000)));
        assertFalse(JavaSEPort.isUsableWindowBounds(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)));
    }

    @Test
    public void isUsableWindowBoundsAcceptsReasonableOnDefaultScreen() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle screen = gd.getDefaultConfiguration().getBounds();
        Rectangle window = new Rectangle(screen.x + 50, screen.y + 50, 800, 600);
        assertTrue(JavaSEPort.isUsableWindowBounds(window),
                "a normally placed 800x600 window on the default screen must be accepted");
    }

    @Test
    public void isUsableWindowBoundsRejectsFarOffScreen() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        // No reachable display extends out this far; the user could never
        // recover this window with the mouse.
        Rectangle window = new Rectangle(-100000, -100000, 800, 600);
        assertFalse(JavaSEPort.isUsableWindowBounds(window));
    }

    @Test
    public void isUsableWindowBoundsRejectsSliverOnScreen() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle screen = gd.getDefaultConfiguration().getBounds();
        // Window is mostly off the right edge of the screen, leaving only ~10px
        // visible. The old intersects() check accepted this; the new floor of
        // 100px visible width must reject it.
        Rectangle window = new Rectangle(screen.x + screen.width - 10, screen.y + 50, 800, 600);
        assertFalse(JavaSEPort.isUsableWindowBounds(window));
    }

    @Test
    public void parsePersistedBoundsAcceptsWellFormed() {
        Rectangle r = JavaSEPort.parsePersistedBounds("10,20,800,600");
        assertNotNull(r);
        assertEquals(10, r.x);
        assertEquals(20, r.y);
        assertEquals(800, r.width);
        assertEquals(600, r.height);
    }

    @Test
    public void parsePersistedBoundsToleratesWhitespace() {
        // Defensive against hand-edited prefs or future formatting tweaks; the
        // current writer does not insert spaces but readers should not be
        // fragile about it.
        Rectangle r = JavaSEPort.parsePersistedBounds("  10 , 20 , 800 , 600  ");
        assertNotNull(r);
        assertEquals(10, r.x);
        assertEquals(600, r.height);
    }

    @Test
    public void parsePersistedBoundsAcceptsNegativeOrigin() {
        // Multi-monitor setups can legitimately place a window at negative
        // coordinates relative to the primary display.
        Rectangle r = JavaSEPort.parsePersistedBounds("-1200,-300,800,600");
        assertNotNull(r);
        assertEquals(-1200, r.x);
        assertEquals(-300, r.y);
    }

    @Test
    public void parsePersistedBoundsRejectsNull() {
        assertNull(JavaSEPort.parsePersistedBounds(null));
    }

    @Test
    public void parsePersistedBoundsRejectsEmpty() {
        assertNull(JavaSEPort.parsePersistedBounds(""));
    }

    @Test
    public void parsePersistedBoundsRejectsWrongFieldCount() {
        assertNull(JavaSEPort.parsePersistedBounds("10,20,30"));
        assertNull(JavaSEPort.parsePersistedBounds("10,20,30,40,50"));
        assertNull(JavaSEPort.parsePersistedBounds("10"));
    }

    @Test
    public void parsePersistedBoundsRejectsNonNumeric() {
        // A NumberFormatException leaking out of init() would abort the entire
        // simulator startup; the parser must swallow it and return null.
        assertNull(JavaSEPort.parsePersistedBounds("not,a,number,here"));
        assertNull(JavaSEPort.parsePersistedBounds("10,20,800,abc"));
        assertNull(JavaSEPort.parsePersistedBounds("10.5,20,800,600"));
    }

    @Test
    public void roundTripWriteAndParse() {
        // Mirrors the save format used by saveBounds(): "x,y,width,height".
        Rectangle saved = new Rectangle(150, 75, 1024, 768);
        String written = saved.x + "," + saved.y + "," + saved.width + "," + saved.height;
        Rectangle restored = JavaSEPort.parsePersistedBounds(written);
        assertEquals(saved, restored);
    }
}
