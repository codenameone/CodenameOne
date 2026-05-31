package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the interactive (desktop) scrollbar logic added to {@link Component}: grabbing
 * and dragging the thumb, clicking the track to page, clamping, and that the whole feature
 * is inert when the {@code interactiveScrollBool} theme constant is off (mobile behavior).
 *
 * <p>The tests inject the painted thumb/track geometry directly via
 * {@link Component#setVerticalScrollBounds(int, int, int, int, int, int, int, int)} (the
 * same call the look and feel makes during paint) so the assertions don't depend on the
 * unit-test theme giving the scrollbar a particular pixel width.</p>
 */
class InteractiveScrollbarTest extends UITestBase {

    private static final int GUTTER = 14;

    private void setInteractiveScroll(boolean on) {
        Hashtable props = new Hashtable();
        props.put("@interactiveScrollBool", on ? "true" : "false");
        UIManager.getInstance().addThemeProps(props);
        assertEquals(on, UIManager.getInstance().getLookAndFeel().isInteractiveScroll());
    }

    /**
     * Builds a vertically scrollable container that overflows its viewport and is laid out
     * inside the current form so absolute coordinates and the scroll dimension are real.
     */
    private Container scrollableContainer(int contentHeight) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        form.removeAll();
        // BoxLayout (not BorderLayout, which forces scrollableY off) so the tall child keeps
        // its preferred height and the container overflows
        Container sc = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        sc.setScrollableY(true);
        sc.setSmoothScrolling(false);
        Label tall = new Label("");
        tall.setPreferredSize(new Dimension(100, contentHeight));
        sc.add(tall);
        form.add(BorderLayout.CENTER, sc);
        form.revalidate();
        assertTrue(sc.getHeight() > 0, "container must be laid out with a height");
        assertTrue(sc.getScrollDimension().getHeight() > sc.getHeight(), "content must overflow");
        return sc;
    }

    private void injectVerticalThumb(Container sc, int thumbY, int thumbH) {
        int trackX = sc.getWidth() - GUTTER;
        sc.setVerticalScrollBounds(trackX, thumbY, GUTTER, thumbH, trackX, 0, GUTTER, sc.getHeight());
    }

    @FormTest
    void thumbGrabAndDragScrollsDown() {
        setInteractiveScroll(true);
        Container sc = scrollableContainer(8000);
        int thumbH = sc.getHeight() / 4;
        injectVerticalThumb(sc, 0, thumbH);

        int px = sc.getAbsoluteX() + sc.getWidth() - GUTTER / 2;
        int py = sc.getAbsoluteY() + thumbH / 2;

        sc.pointerPressed(px, py);
        assertTrue(sc.isInteractiveScrollThumbGrabbed(), "press on the thumb should start a grab");

        int before = sc.getScrollY();
        sc.pointerDragged(px, py + 300);
        assertTrue(sc.getScrollY() > before, "dragging the thumb down should increase scrollY");

        sc.pointerReleased(px, py + 300);
        assertFalse(sc.isInteractiveScrollThumbGrabbed(), "release should clear the grab");
    }

    @FormTest
    void thumbDragClampsAtBottom() {
        setInteractiveScroll(true);
        Container sc = scrollableContainer(8000);
        int thumbH = sc.getHeight() / 4;
        injectVerticalThumb(sc, 0, thumbH);

        int px = sc.getAbsoluteX() + sc.getWidth() - GUTTER / 2;
        int py = sc.getAbsoluteY() + thumbH / 2;
        sc.pointerPressed(px, py);
        // drag far past the bottom of the track
        sc.pointerDragged(px, py + 100000);
        sc.pointerReleased(px, py + 100000);

        int max = sc.getScrollDimension().getHeight() - sc.getHeight();
        assertEquals(max, sc.getScrollY(), "thumb drag must clamp at the maximum scroll position");
    }

    @FormTest
    void trackClickBelowThumbPagesDown() {
        setInteractiveScroll(true);
        Container sc = scrollableContainer(8000);
        int thumbH = sc.getHeight() / 5;
        injectVerticalThumb(sc, 0, thumbH);

        // click the track well below the thumb (which sits at the top)
        int px = sc.getAbsoluteX() + sc.getWidth() - GUTTER / 2;
        int py = sc.getAbsoluteY() + sc.getHeight() - 2;

        int before = sc.getScrollY();
        sc.pointerPressed(px, py);
        assertFalse(sc.isInteractiveScrollThumbGrabbed(), "a track click is not a thumb grab");
        assertTrue(sc.getScrollY() > before, "clicking the track below the thumb should page down");
        sc.pointerReleased(px, py);
    }

    @FormTest
    void disabledIsInert() {
        setInteractiveScroll(false);
        Container sc = scrollableContainer(8000);
        int thumbH = sc.getHeight() / 4;
        injectVerticalThumb(sc, 0, thumbH);

        int px = sc.getAbsoluteX() + sc.getWidth() - GUTTER / 2;
        int py = sc.getAbsoluteY() + thumbH / 2;

        sc.pointerPressed(px, py);
        assertFalse(sc.isInteractiveScrollThumbGrabbed(),
                "with interactiveScrollBool off, pressing the gutter must not grab the thumb");
        sc.pointerReleased(px, py);
    }
}
