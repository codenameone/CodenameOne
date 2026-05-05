package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StickyHeaderContainerTest extends UITestBase {

    private static final int FRAME_WIDTH = 200;
    private static final int FRAME_HEIGHT = 600;
    private static final int HEADER_HEIGHT = 50;
    private static final int CONTENT_HEIGHT = 200;
    private static final int SECTION_STRIDE = HEADER_HEIGHT + CONTENT_HEIGHT;

    @FormTest
    void addSectionRejectsNullHeader() {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        assertThrows(IllegalArgumentException.class, () -> sticky.addSection(null, new Container()));
    }

    @FormTest
    void sectionsAreRegisteredInOrder() {
        StickyHeaderContainer sticky = build(3);
        assertEquals(3, sticky.getStickyHeaders().size());
        assertEquals(6, sticky.getScrollContainer().getComponentCount(),
                "scroller should hold one entry per header and content");
    }

    @FormTest
    void initialStateIsInactive() {
        StickyHeaderContainer sticky = build(3);
        assertEquals(-1, sticky.getActiveSectionIndex());
        assertEquals(0, sticky.getStickyHost().getComponentCount());
    }

    @FormTest
    void firstSectionPinsAfterScroll() {
        StickyHeaderContainer sticky = build(3);
        Component first = sticky.getStickyHeaders().get(0);

        sticky.setScrollPosition(10);
        sticky.updateSticky();

        assertEquals(0, sticky.getActiveSectionIndex());
        assertEquals(1, sticky.getStickyHost().getComponentCount());
        assertSame(first, sticky.getStickyHost().getComponentAt(0),
                "the same header instance must be moved into the sticky host");
    }

    @FormTest
    void secondSectionTakesOverPastBoundary() {
        StickyHeaderContainer sticky = build(3);
        Component second = sticky.getStickyHeaders().get(1);

        sticky.setScrollPosition(SECTION_STRIDE + 10);
        sticky.updateSticky();

        assertEquals(1, sticky.getActiveSectionIndex());
        assertSame(second, sticky.getStickyHost().getComponentAt(0));
    }

    @FormTest
    void scrollingBackToTopDeactivates() {
        StickyHeaderContainer sticky = build(3);

        sticky.setScrollPosition(50);
        sticky.updateSticky();
        assertEquals(0, sticky.getActiveSectionIndex());

        sticky.setScrollPosition(0);
        sticky.updateSticky();

        assertEquals(-1, sticky.getActiveSectionIndex());
        assertEquals(0, sticky.getStickyHost().getComponentCount());
    }

    @FormTest
    void clearSectionsResetsState() {
        StickyHeaderContainer sticky = build(3);
        sticky.setScrollPosition(50);
        sticky.updateSticky();
        assertEquals(0, sticky.getActiveSectionIndex());

        sticky.clearSections();

        assertEquals(0, sticky.getStickyHeaders().size());
        assertEquals(0, sticky.getScrollContainer().getComponentCount());
        assertEquals(-1, sticky.getActiveSectionIndex());
        assertEquals(0, sticky.getStickyHost().getComponentCount());
    }

    @FormTest
    void headerOnlySectionRegisters() {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        Container header = sized("solo", FRAME_WIDTH, HEADER_HEIGHT);
        sticky.addSection(header);
        sticky.setWidth(FRAME_WIDTH);
        sticky.setHeight(FRAME_HEIGHT);
        sticky.layoutContainer();
        sticky.getScrollContainer().layoutContainer();

        assertEquals(1, sticky.getStickyHeaders().size());
        assertEquals(1, sticky.getScrollContainer().getComponentCount(),
                "header-only section adds a single child to the scroller");
    }

    @FormTest
    void transitionStyleDefaultsToSlide() {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        assertEquals(StickyHeaderContainer.TRANSITION_SLIDE, sticky.getTransitionStyle());
    }

    @FormTest
    void transitionStyleSetterAcceptsKnownValues() {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_FADE);
        assertEquals(StickyHeaderContainer.TRANSITION_FADE, sticky.getTransitionStyle());
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_NONE);
        assertEquals(StickyHeaderContainer.TRANSITION_NONE, sticky.getTransitionStyle());
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_SLIDE);
        assertEquals(StickyHeaderContainer.TRANSITION_SLIDE, sticky.getTransitionStyle());
    }

    @FormTest
    void transitionStyleRejectsUnknownValue() {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        assertThrows(IllegalArgumentException.class, () -> sticky.setTransitionStyle(42));
    }

    @FormTest
    void transitionDurationRejectsNegative() {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        assertThrows(IllegalArgumentException.class, () -> sticky.setTransitionDurationMillis(-1));
    }

    @FormTest
    void pushProgressTracksScrollPositionWithinWindow() {
        StickyHeaderContainer sticky = build(3);

        // Pin section 0 well clear of the next boundary: no overlap yet.
        sticky.setScrollPosition(100);
        sticky.updateSticky();
        assertEquals(0, sticky.getActiveSectionIndex());
        assertFalse(sticky.isTransitionInProgress(),
                "no overlap when next header is far below the slot");
        assertEquals(0f, sticky.getTransitionProgress(), 0.001f);

        // Section 1's header sits at Y = SECTION_STRIDE (250). It enters
        // the push window when scrollY > SECTION_STRIDE - HEADER_HEIGHT
        // (i.e. the header's top is within the slot height).
        sticky.setScrollPosition(SECTION_STRIDE - HEADER_HEIGHT + 10);
        sticky.updateSticky();
        assertEquals(0, sticky.getActiveSectionIndex(),
                "the swap shouldn't happen until next.relY <= 0");
        assertTrue(sticky.isTransitionInProgress(),
                "expected push to be in flight inside the window");
        assertEquals(10f / HEADER_HEIGHT, sticky.getTransitionProgress(), 0.001f);

        sticky.setScrollPosition(SECTION_STRIDE - HEADER_HEIGHT + 25);
        sticky.updateSticky();
        assertEquals(25f / HEADER_HEIGHT, sticky.getTransitionProgress(), 0.001f);

        sticky.setScrollPosition(SECTION_STRIDE - HEADER_HEIGHT + 40);
        sticky.updateSticky();
        assertEquals(40f / HEADER_HEIGHT, sticky.getTransitionProgress(), 0.001f);

        // Cross the boundary and section 1 takes over; no new overlap with
        // section 2 yet, so the push offset resets to 0.
        sticky.setScrollPosition(SECTION_STRIDE + 10);
        sticky.updateSticky();
        assertEquals(1, sticky.getActiveSectionIndex());
        assertFalse(sticky.isTransitionInProgress());
        assertEquals(0f, sticky.getTransitionProgress(), 0.001f);
    }

    @FormTest
    void slidePushShiftsStickyHostUp() {
        StickyHeaderContainer sticky = build(3);
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_SLIDE);

        sticky.setScrollPosition(100);
        sticky.updateSticky();
        int baseY = sticky.getStickyHost().getY();

        sticky.setScrollPosition(SECTION_STRIDE - HEADER_HEIGHT + 30);
        sticky.updateSticky();
        // pushOffset = 30 → host shifts up by 30.
        assertEquals(baseY - 30, sticky.getStickyHost().getY(),
                "slide style must shift the host up by the push offset");
        assertEquals(255, sticky.getStickyHost().getStyle().getOpacity(),
                "slide style keeps the host fully opaque");
        assertTrue(sticky.getStickyHost().isVisible(),
                "slide style keeps the host visible during the push");
    }

    @FormTest
    void noneStyleKeepsStickyHostInPlaceDuringOverlap() {
        StickyHeaderContainer sticky = build(3);
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_NONE);

        sticky.setScrollPosition(100);
        sticky.updateSticky();
        int baseY = sticky.getStickyHost().getY();
        assertTrue(sticky.getStickyHost().isVisible(),
                "host stays visible when there is no overlap");

        // Inside the push window: NONE keeps the host pinned in place
        // and fully opaque so the rising section in the scroller below
        // does not bleed through where the slot used to be.
        sticky.setScrollPosition(SECTION_STRIDE - HEADER_HEIGHT + 20);
        sticky.updateSticky();
        assertTrue(sticky.getStickyHost().isVisible(),
                "NONE must keep the host visible during the overlap");
        assertEquals(baseY, sticky.getStickyHost().getY(),
                "NONE must not shift the host while overlapping");
        assertEquals(255, sticky.getStickyHost().getStyle().getOpacity(),
                "NONE keeps the host fully opaque");

        // Past the boundary: new section is pinned, host shows again.
        sticky.setScrollPosition(SECTION_STRIDE + 10);
        sticky.updateSticky();
        assertEquals(1, sticky.getActiveSectionIndex());
        assertTrue(sticky.getStickyHost().isVisible(),
                "after the swap the host is visible with the new header");
    }

    @FormTest
    void fadeStyleSlidesAndFadesStickyHostWithPush() {
        StickyHeaderContainer sticky = build(3);
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_FADE);

        sticky.setScrollPosition(100);
        sticky.updateSticky();
        int baseY = sticky.getStickyHost().getY();
        assertEquals(255, sticky.getStickyHost().getStyle().getOpacity(),
                "fully opaque outside the push window");

        sticky.setScrollPosition(SECTION_STRIDE - HEADER_HEIGHT + 25);
        sticky.updateSticky();
        // pushOffset = 25 of 50 → host slides up by 25 AND alpha = 127
        // (or 128 by rounding). The combined slide+fade gives the rising
        // header room to rise into the slot from below while the pinned
        // header dissolves out the top.
        int alpha = sticky.getStickyHost().getStyle().getOpacity();
        assertTrue(alpha > 100 && alpha < 160,
                "fade alpha should be roughly half-way through, was " + alpha);
        assertEquals(baseY - 25, sticky.getStickyHost().getY(),
                "FADE must shift the host up by the push offset");
        assertTrue(sticky.getStickyHost().isVisible());

        // After the swap: full opacity again on the new header,
        // host returns to the base Y.
        sticky.setScrollPosition(SECTION_STRIDE + 10);
        sticky.updateSticky();
        assertEquals(1, sticky.getActiveSectionIndex());
        assertEquals(255, sticky.getStickyHost().getStyle().getOpacity());
        assertEquals(baseY, sticky.getStickyHost().getY());
    }

    @FormTest
    void shortReverseScrollDoesNotFlipActiveSection() {
        // 4 sections so that scrollDimension > viewport height + the
        // boundary positions used below; otherwise non-tensile scrollers
        // clamp scrollY into a range that hides this hysteresis window.
        StickyHeaderContainer sticky = build(4);

        sticky.setScrollPosition(SECTION_STRIDE + 5);
        sticky.updateSticky();
        assertEquals(1, sticky.getActiveSectionIndex());

        // Tiny reverse bounce of 1 pixel past the boundary — well
        // inside the hysteresis window — must not deactivate section 1.
        // Without hysteresis an inertial bounce here would teleport the
        // pinned header back into the scroller and re-pin it on the
        // next forward bounce, producing a visible jitter.
        sticky.setScrollPosition(SECTION_STRIDE - 1);
        sticky.updateSticky();
        assertEquals(1, sticky.getActiveSectionIndex(),
                "small inertial bounces must not flip the active section");

        // A larger reverse scroll past the hysteresis window does
        // deactivate as before.
        sticky.setScrollPosition(SECTION_STRIDE - 20);
        sticky.updateSticky();
        assertEquals(0, sticky.getActiveSectionIndex(),
                "scrolling well back past the boundary deactivates");
    }

    @FormTest
    void scrollingPastSeveralBoundariesSettlesOnLast() {
        StickyHeaderContainer sticky = build(4);

        sticky.setScrollPosition(SECTION_STRIDE * 3 + 10);
        sticky.updateSticky();

        assertEquals(3, sticky.getActiveSectionIndex());
        assertSame(sticky.getStickyHeaders().get(3),
                sticky.getStickyHost().getComponentAt(0));
    }

    private static StickyHeaderContainer build(int sectionCount) {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        zero(sticky);
        zero(sticky.getScrollContainer());
        zero(sticky.getStickyHost());
        for (int i = 0; i < sectionCount; i++) {
            Container header = sized("H" + i, FRAME_WIDTH, HEADER_HEIGHT);
            Container content = sized("C" + i, FRAME_WIDTH, CONTENT_HEIGHT);
            sticky.addSection(header, content);
        }
        sticky.setWidth(FRAME_WIDTH);
        sticky.setHeight(FRAME_HEIGHT);
        sticky.layoutContainer();
        sticky.getScrollContainer().layoutContainer();
        return sticky;
    }

    private static Container sized(final String name, final int w, final int h) {
        Container c = new Container() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(w, h);
            }
        };
        c.setName(name);
        Style s = c.getAllStyles();
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        return c;
    }

    private static void zero(Container c) {
        Style s = c.getAllStyles();
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
    }
}
