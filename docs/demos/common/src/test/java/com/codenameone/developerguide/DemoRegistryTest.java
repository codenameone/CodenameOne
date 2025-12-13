package com.codenameone.developerguide;

import com.codename1.testing.AbstractTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verifies the demo registry exposes the expected demos and is immutable.
 */
public class DemoRegistryTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        List<Demo> demos = DemoRegistry.getDemos();

        assertNotNull(demos, "Demo registry should never return null.");
        assertFalse(demos.isEmpty(), "Demo registry should contain at least one demo.");

        Set<String> titles = new HashSet<>();
        for (Demo demo : demos) {
            assertNotNull(demo.getTitle(), "Demo titles must not be null.");
            assertFalse(demo.getTitle().trim().isEmpty(), "Demo titles must not be empty.");
            assertNotNull(demo.getDescription(), "Demo descriptions must not be null.");
            titles.add(demo.getTitle());
        }

        assertTrue(titles.contains("Layout Animations"), "Layout Animations demo should be registered.");
        assertTrue(titles.contains("Slide Transitions"), "Slide Transitions demo should be registered.");
        assertTrue(titles.contains("Bubble Transition"), "Bubble Transition demo should be registered.");

        boolean immutable = false;
        try {
            demos.add(new Demo() {
                @Override
                public String getTitle() {
                    return "Should Not Add";
                }

                @Override
                public String getDescription() {
                    return "";
                }

                @Override
                public void show(com.codename1.ui.Form parent) {
                }
            });
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        assertTrue(immutable, "Demo registry should be immutable.");

        return true;
    }
}
