package com.codenameone.developerguide;

import com.codename1.testing.AbstractTest;

import java.util.List;

/**
 * Verifies the demo registry exposes the expected demos and is immutable.
 */
public class DemoRegistryTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        List<Demo> demos = DemoRegistry.getDemos();

        assertNotNull(demos, "Demo registry should never return null.");
        assertFalse(demos.isEmpty(), "Demo registry should contain at least one demo.");
        assertEqual(2, demos.size(), "Demo registry should contain the sample demos.");

        Demo hello = demos.get(0);
        assertEqual("Hello World", hello.getTitle());
        assertEqual("Shows a button that pops up a welcome dialog.", hello.getDescription());

        Demo counter = demos.get(1);
        assertEqual("Counter", counter.getTitle());
        assertEqual("Interactive counter with increment/decrement controls and a slider.", counter.getDescription());

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
