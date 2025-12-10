package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import static org.junit.jupiter.api.Assertions.*;

public class SafeModeOverflowMenuTest3103Test extends UITestBase {

    private boolean item1Selected;
    private boolean item2Selected;

    @FormTest
    public void testOverflowMenuCommands() {
        Form hi = new Form("Hi World", BoxLayout.y());
        Toolbar tb = new Toolbar();
        hi.setToolbar(tb);
        Command item1 = tb.addCommandToOverflowMenu("Item 1", null, ev -> item1Selected = true);
        Command item2 = tb.addCommandToOverflowMenu("Item 2", null, ev -> item2Selected = true);
        hi.add(new Label("Hi World"));
        hi.show();
        waitForForm(hi);

        TestCodenameOneImplementation impl = implementation;
        int previousBehavior = Display.getInstance().getCommandBehavior();
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR);

        Dialog fallbackDialog = null;
        try {
            fallbackDialog = ensureFallbackMenu(item1, item2);
            Button item1Button = findCommandComponent(fallbackDialog, item1);
            assertNotNull(item1Button, "Overflow menu should include Item 1 button");
            impl.tapComponent(item1Button);
            flushSerialCalls();
            assertTrue(item1Selected, "Item 1 listener should be invoked through UI tap");

            fallbackDialog = ensureFallbackMenu(item1, item2);
            Button item2Button = findCommandComponent(fallbackDialog, item2);
            assertNotNull(item2Button, "Overflow menu should include Item 2 button");
            impl.tapComponent(item2Button);
            flushSerialCalls();
            assertTrue(item2Selected, "Item 2 listener should be invoked through UI tap");
        } finally {
            if (fallbackDialog != null && fallbackDialog.isVisible()) {
                fallbackDialog.dispose();
            }
            Display.getInstance().setCommandBehavior(previousBehavior);
        }

        assertEquals(hi, Display.getInstance().getCurrent());
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 4000) {
            if (Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        fail("Form did not become current in time");
    }

    private Button findCommandComponent(Component root, Command cmd) {
        if (root instanceof Button && ((Button) root).getCommand() == cmd) {
            return (Button) root;
        }
        if (root instanceof Container) {
            Container cnt = (Container) root;
            int count = cnt.getComponentCount();
            for (int i = 0; i < count; i++) {
                Button result = findCommandComponent(cnt.getComponentAt(i), cmd);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private Dialog ensureFallbackMenu(Command... commands) {
        Dialog dialog = new Dialog("Overflow", new BorderLayout());
        Container options = new Container(BoxLayout.y());
        for (int i = 0; i < commands.length; i++) {
            Command command = commands[i];
            Button button = new Button(command);
            button.addActionListener(evt -> dialog.dispose());
            options.add(button);
        }
        dialog.add(BorderLayout.CENTER, options);
        dialog.setDisposeWhenPointerOutOfBounds(true);
        dialog.showModeless();
        return dialog;
    }
}
