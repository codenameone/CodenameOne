package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StickyHeaderContainerTest extends UITestBase {

    private static final int FRAME_WIDTH = 200;
    private static final int FRAME_HEIGHT = 600;
    private static final int HEADER_HEIGHT = 50;
    private static final int CONTENT_HEIGHT = 200;
    private static final int SECTION_STRIDE = HEADER_HEIGHT + CONTENT_HEIGHT;

    @org.junit.jupiter.api.AfterEach
    void resetAnimationTime() {
        AnimationTime.reset();
    }

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
    void slideTransitionStartsOnActiveChange() {
        StickyHeaderContainer sticky = build(3);
        sticky.setTransitionDurationMillis(200);

        AnimationTime.setTime(1000L);
        sticky.setScrollPosition(10);
        sticky.updateSticky();
        // First activation has no outgoing snapshot so no transition runs.
        assertTrue(sticky.getTransitionProgress() == 1f);

        sticky.setScrollPosition(SECTION_STRIDE + 10);
        sticky.updateSticky();
        // Second activation starts a transition because there is an outgoing
        // header to swap.
        assertTrue(sticky.isTransitionInProgress(), "expected transition after section swap");
        assertEquals(0f, sticky.getTransitionProgress(), 0.001f);

        AnimationTime.setTime(1100L);
        assertEquals(0.5f, sticky.getTransitionProgress(), 0.05f);

        AnimationTime.setTime(1300L);
        assertTrue(sticky.getTransitionProgress() >= 1f);
    }

    @FormTest
    void noneStyleSkipsTransitionAnimation() {
        StickyHeaderContainer sticky = build(3);
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_NONE);

        sticky.setScrollPosition(10);
        sticky.updateSticky();
        sticky.setScrollPosition(SECTION_STRIDE + 10);
        sticky.updateSticky();

        assertEquals(1, sticky.getActiveSectionIndex());
        assertTrue(sticky.getTransitionProgress() == 1f,
                "TRANSITION_NONE must not start an animation");
    }

    @FormTest
    void scrollingPastSeveralBoundariesSettlesOnLast() {
        StickyHeaderContainer sticky = build(4);
        sticky.setTransitionDurationMillis(0);

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
