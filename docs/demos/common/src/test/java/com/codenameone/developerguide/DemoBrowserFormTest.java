package com.codenameone.developerguide;

import com.codename1.components.MultiButton;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;

/**
 * Ensures the demo browser lists and describes the registered demos.
 */
public class DemoBrowserFormTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        DemoBrowserForm form = new DemoBrowserForm();
        form.show();
        TestUtils.waitForFormTitle("Developer Guide Demos", 5000);

        assertEqual("Developer Guide Demos", Display.getInstance().getCurrent().getTitle());

        Component content = form.getContentPane().getComponentAt(0);
        assertTrue(content instanceof Container, "Demo list should be wrapped in a container.");

        Container list = (Container) content;
        assertEqual("demoList", list.getName());
        assertEqual(DemoRegistry.getDemos().size(), list.getComponentCount());

        for (int i = 0; i < list.getComponentCount(); i++) {
            Component component = list.getComponentAt(i);
            assertTrue(component instanceof MultiButton, "Expected demo option to be a MultiButton.");

            MultiButton button = (MultiButton) component;
            Demo demo = DemoRegistry.getDemos().get(i);
            assertEqual(demo.getTitle(), button.getTextLine1());
            assertEqual(demo.getDescription(), button.getTextLine2());
            assertEqual("demoButton-" + i, button.getName());
        }

        return true;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
}
