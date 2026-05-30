package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for
 * <a href="https://github.com/codenameone/CodenameOne/issues/1363">#1363</a>
 * — {@link Component#getPreferredSize()} must give each Component its own
 * {@link Dimension} instance even when their {@code calcPreferredSize()}
 * implementations all return a shared one (for example, when each entry of a
 * {@code ContainerList} returns the same renderer component's preferred-size
 * field).
 */
class ComponentPreferredSizeIsolationTest extends UITestBase {

    private static final class FixedSizeComponent extends Component {
        private final Dimension shared;

        FixedSizeComponent(Dimension shared) {
            this.shared = shared;
        }

        @Override
        protected Dimension calcPreferredSize() {
            return shared;
        }
    }

    /**
     * Two components whose {@code calcPreferredSize()} returns the same
     * {@link Dimension} reference must end up with independent preferred-size
     * instances after the framework caches the result. Otherwise mutating one
     * component's preferred size (e.g. via the {@code sameWidth}/{@code
     * sameHeight} alignment path or via a layout) silently mutates the other.
     */
    @FormTest
    void twoComponentsSharingACalcDimensionGetIndependentPreferredSizes() {
        Dimension shared = new Dimension(40, 20);
        FixedSizeComponent a = new FixedSizeComponent(shared);
        FixedSizeComponent b = new FixedSizeComponent(shared);

        Dimension sizeA = a.getPreferredSize();
        Dimension sizeB = b.getPreferredSize();

        assertEquals(40, sizeA.getWidth());
        assertEquals(20, sizeA.getHeight());
        assertEquals(40, sizeB.getWidth());
        assertEquals(20, sizeB.getHeight());

        assertNotSame(sizeA, sizeB,
                "Each component must own its preferred-size Dimension; sharing the "
                        + "instance returned by calcPreferredSize causes #1363.");
        assertNotSame(shared, sizeA,
                "The cached preferred-size must not retain a reference to the "
                        + "Dimension returned from calcPreferredSize, otherwise mutating "
                        + "the source (e.g. a renderer's own preferredSize field) "
                        + "silently mutates this component's cached value.");
    }

    /**
     * Mutating the {@link Dimension} that {@code calcPreferredSize()} returned
     * must not silently change the cached preferred size of any component that
     * has already been measured. This is the failure mode the 2015 reporter
     * pinpointed in {@code ContainerList.Entry}: the renderer's own
     * {@code preferredSize} field is reused across entries, so when the
     * renderer was re-laid-out for the next entry every previously-measured
     * entry's "cached" size silently changed too.
     */
    @FormTest
    void mutatingCalcSourceDoesNotAlterCachedPreferredSize() {
        Dimension shared = new Dimension(40, 20);
        FixedSizeComponent c = new FixedSizeComponent(shared);

        Dimension cached = c.getPreferredSize();
        assertEquals(40, cached.getWidth());
        assertEquals(20, cached.getHeight());

        // Simulate the renderer being re-measured for the next entry.
        shared.setWidth(999);
        shared.setHeight(888);

        Dimension after = c.getPreferredSize();
        assertEquals(40, after.getWidth(),
                "Cached preferred width changed underneath us because the framework "
                        + "kept a reference to the Dimension returned by calcPreferredSize.");
        assertEquals(20, after.getHeight(),
                "Cached preferred height changed underneath us because the framework "
                        + "kept a reference to the Dimension returned by calcPreferredSize.");
    }
}
