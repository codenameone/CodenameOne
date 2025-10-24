package com.codenameone.developerguide;

import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;

/**
 * Exercises the Hello World demo lifecycle and interactions.
 */
public class HelloWorldDemoTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        Form parent = new Form("Parent");
        parent.show();
        TestUtils.waitForFormTitle("Parent", 5000);

        Demo demo = new HelloWorldDemo();
        demo.show(parent);
        TestUtils.waitForFormTitle("Hello World", 5000);

        Form current = Display.getInstance().getCurrent();
        assertEqual("Hello World", current.getTitle());
        assertEqual("helloWorldForm", current.getName());

        Button button = (Button) TestUtils.findByName("helloButton");
        assertNotNull(button, "Hello button should be present.");
        assertEqual("Say Hello", button.getText());

        Thread dialogCloser = new Thread(() -> {
            TestUtils.waitFor(500);
            Display.getInstance().callSerially(() -> {
                if (Display.getInstance().getCurrent() instanceof Dialog) {
                    ((Dialog) Display.getInstance().getCurrent()).dispose();
                }
            });
        });
        dialogCloser.start();
        button.pressed();
        button.released();
        dialogCloser.join(2000);

        TestUtils.waitForFormTitle("Hello World", 5000);
        assertEqual("Hello World", Display.getInstance().getCurrent().getTitle());

        Command back = current.getBackCommand();
        assertNotNull(back, "Back command should be available.");
        back.actionPerformed(new ActionEvent(back));
        TestUtils.waitForFormTitle("Parent", 5000);
        assertEqual(parent, Display.getInstance().getCurrent());

        return true;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
}
