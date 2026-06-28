package com.codename1.ui;

import com.codename1.junit.EdtTest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/// Regression tests for issue #5305: a {@link ScrollListener} that reacts to a
/// scroll by scrolling the same component back into view (e.g. via
/// {@link Container#scrollComponentToVisible}) used to recurse infinitely and
/// throw a {@link StackOverflowError}.
///
/// The fix in {@link Component#setScrollY(int)} / {@link Component#setScrollX(int)}
/// does two things:
///   1. updates the {@code scrollX}/{@code scrollY} field *before* notifying
///      listeners, so a listener reading {@code getScrollY()} during its callback
///      sees the new position and stops recomputing the same target; and
///   2. only fires the scroll event when the (clamped) value actually changed,
///      so once the position settles the listener is no longer re-invoked.
///
/// This class lives in the {@code com.codename1.ui} package so it can drive the
/// protected {@code setScrollX}/{@code setScrollY} setters directly.
class ScrollListenerReentrancyTest extends UITestBase {

    /// A bare container with smooth scrolling + tensile drag enabled skips the
    /// clamp block in the setters, giving deterministic scroll values without
    /// needing a laid-out, scrollable viewport.
    private static Container unclampedContainer() {
        // make sure no earlier test left the global smooth-scroll kill switch on
        Component.setDisableSmoothScrolling(false);
        Container c = new Container();
        c.setSmoothScrolling(true);
        c.setTensileDragEnabled(true);
        return c;
    }

    @EdtTest
    void testNoEventFiredWhenScrollYDoesNotChange() {
        Container c = unclampedContainer();
        int[] count = {0};
        c.addScrollListener((sx, sy, osx, osy) -> count[0]++);

        // already at 0 -> must not fire
        c.setScrollY(0);
        assertEquals(0, count[0], "setScrollY to the current value must not fire an event");

        // real change -> fires once with the new value
        c.setScrollY(50);
        assertEquals(1, count[0], "a real scroll change must fire exactly once");
        assertEquals(50, c.getScrollY());

        // repeat with same value -> suppressed (this is the loop-breaker for #5305)
        c.setScrollY(50);
        assertEquals(1, count[0], "repeating the same scrollY must not fire again");

        // another real change -> fires again
        c.setScrollY(0);
        assertEquals(2, count[0], "scrolling back to a different value must fire");
        assertEquals(0, c.getScrollY());
    }

    @EdtTest
    void testNoEventFiredWhenScrollXDoesNotChange() {
        Container c = unclampedContainer();
        int[] count = {0};
        c.addScrollListener((sx, sy, osx, osy) -> count[0]++);

        c.setScrollX(0);
        assertEquals(0, count[0], "setScrollX to the current value must not fire an event");

        c.setScrollX(30);
        assertEquals(1, count[0]);
        assertEquals(30, c.getScrollX());

        c.setScrollX(30);
        assertEquals(1, count[0], "repeating the same scrollX must not fire again");

        c.setScrollX(0);
        assertEquals(2, count[0]);
    }

    /// The scroll field must already hold the new value while the listener runs,
    /// otherwise a listener reading {@code getScrollY()} (as scrollComponentToVisible
    /// does) would see a stale value and recompute the same target forever.
    @EdtTest
    void testScrollFieldUpdatedBeforeListenerRuns() {
        Container c = unclampedContainer();
        int[] observed = {-1};
        c.addScrollListener((sx, sy, osx, osy) -> observed[0] = c.getScrollY());

        c.setScrollY(77);
        assertEquals(77, observed[0],
                "getScrollY() inside the listener must return the new position, not the old one");
    }

    /// Faithful reproduction of the reported pattern using only the setter: a
    /// listener that pins the component to a fixed scroll offset whenever the
    /// component reports a different position. Before the fix this recursed until
    /// the stack overflowed; now it converges after a single correction.
    @EdtTest
    void testReentrantPinningListenerConverges() {
        Container c = unclampedContainer();
        final int pin = 50;
        int[] count = {0};
        c.addScrollListener((sx, sy, osx, osy) -> {
            count[0]++;
            if (count[0] > 100) {
                throw new IllegalStateException("runaway scroll recursion (#5305 regressed)");
            }
            // react to the component's OWN reported position, exactly like
            // scrollComponentToVisible does via getScrollY()
            if (c.getScrollY() != pin) {
                c.setScrollY(pin);
            }
        });

        c.setScrollY(200);

        assertEquals(pin, c.getScrollY(), "listener should have pinned the scroll position");
        assertTrue(count[0] <= 3,
                "a pinning listener must converge, not recurse (callbacks=" + count[0] + ")");
    }

    /// End-to-end reproduction through the exact API chain from the issue's stack
    /// trace: scrollComponentToVisible -> scrollRectToVisible -> setScrollY. Smooth
    /// scrolling is disabled so the scroll happens synchronously (as in the report)
    /// rather than being deferred to the animation thread.
    @FormTest
    void testScrollComponentToVisibleListenerDoesNotOverflow() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        scrollable.setScrollableY(true);
        scrollable.setSmoothScrolling(false);

        Component target = null;
        for (int i = 0; i < 30; i++) {
            Button b = new Button("Item " + i);
            b.setPreferredSize(new Dimension(200, 100));
            scrollable.add(b);
            if (i == 25) {
                target = b;
            }
        }
        final Component pinned = target;

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // precondition: the content must actually overflow the viewport, otherwise
        // there is nothing to scroll and the test would pass vacuously.
        assertTrue(scrollable.getScrollDimension().getHeight() > scrollable.getHeight(),
                "precondition: container must be scrollable (content taller than viewport)");

        int[] count = {0};
        scrollable.addScrollListener((sx, sy, osx, osy) -> {
            count[0]++;
            if (count[0] > 200) {
                throw new IllegalStateException("runaway scroll recursion (#5305 regressed)");
            }
            // the tutorial use case: keep the highlighted component in view on
            // every scroll event
            scrollable.scrollComponentToVisible(pinned);
        });

        // bring the target into view (fires the listener, which re-pins it)
        scrollable.scrollComponentToVisible(pinned);
        // then jump back to the top so the listener has to drag it into view again
        scrollable.setScrollY(0);

        assertTrue(count[0] < 50,
                "scrollComponentToVisible listener must converge (callbacks=" + count[0] + ")");
        assertTrue(scrollable.getScrollY() > 0,
                "the target should have been scrolled back into view");
    }
}
