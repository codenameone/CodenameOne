package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for
 * <a href="https://github.com/codenameone/CodenameOne/issues/1399">#1399</a>
 * -- a scrollable Container whose content is smaller than the container itself
 * should still produce scroll feedback when the user explicitly opts in via
 * {@link Component#setAlwaysTensile(boolean)} (the workaround documented in
 * the 2015 issue thread). The Y axis has always honoured {@code alwaysTensile}
 * in {@link Container#isScrollableY()}; the X axis did not.
 */
class ScrollableAlwaysTensileTest extends UITestBase {

    @FormTest
    void containerIsNotScrollableYByDefaultWhenContentFits() {
        Container c = new Container(BoxLayout.y());
        c.setScrollableY(true);
        Label small = new Label("hi");
        small.setPreferredW(40);
        small.setPreferredH(20);
        c.add(small);
        c.setWidth(400);
        c.setHeight(800);
        c.layoutContainer();

        assertFalse(c.isScrollableY(),
                "By design, when content fits the container is not 'scrollable' "
                        + "and produces no scroll feedback. The documented workaround "
                        + "for #1399 is setAlwaysTensile(true).");
    }

    @FormTest
    void alwaysTensileMakesScrollableYTrueWhenContentFits() {
        Container c = new Container(BoxLayout.y());
        c.setScrollableY(true);
        c.setAlwaysTensile(true);
        Label small = new Label("hi");
        small.setPreferredW(40);
        small.setPreferredH(20);
        c.add(small);
        c.setWidth(400);
        c.setHeight(800);
        c.layoutContainer();

        assertTrue(c.isScrollableY(),
                "With setAlwaysTensile(true) and scrollableY=true, the container "
                        + "must report as scrollable so paintScrollbars() and "
                        + "pointerDragged() run during tensile feedback.");
    }

    @FormTest
    void alwaysTensileMakesScrollableXTrueWhenContentFits() {
        // The 2015 fix in Container.isScrollableY() that added an
        // isAlwaysTensile() check was never mirrored to isScrollableX(). The
        // documented #1399 workaround therefore had no effect on the X axis:
        // setScrollableX(true) + setAlwaysTensile(true) with narrow content
        // still reported isScrollableX() == false, so no scrollbar paint and
        // no drag activation. After the fix both axes behave the same.
        Container c = new Container(new com.codename1.ui.layouts.FlowLayout());
        c.setScrollableX(true);
        c.setAlwaysTensile(true);
        Label small = new Label("hi");
        small.setPreferredW(40);
        small.setPreferredH(20);
        c.add(small);
        c.setWidth(800);
        c.setHeight(200);
        c.layoutContainer();

        assertTrue(c.isScrollableX(),
                "With setAlwaysTensile(true) and scrollableX=true, the container "
                        + "must report as scrollable on the X axis just like "
                        + "isScrollableY() already does, otherwise the documented "
                        + "#1399 workaround has no effect on horizontal scrollables.");
    }

    @FormTest
    void isScrollableXStillFalseWhenAlwaysTensileFalse() {
        // Sanity check: the alwaysTensile override must not regress the
        // non-alwaysTensile case. When content fits and alwaysTensile is off,
        // isScrollableX() must still report false.
        Container c = new Container(new com.codename1.ui.layouts.FlowLayout());
        c.setScrollableX(true);
        Label small = new Label("hi");
        small.setPreferredW(40);
        small.setPreferredH(20);
        c.add(small);
        c.setWidth(800);
        c.setHeight(200);
        c.layoutContainer();

        assertFalse(c.isScrollableX());
    }
}
