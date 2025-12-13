package com.codename1.samples;

import com.codename1.components.MultiButton;
import com.codename1.components.SpanLabel;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import static org.junit.jupiter.api.Assertions.*;

public class SafeAreasSampleTest extends UITestBase {

    @FormTest
    public void testSafeAreasSample() {
        Form hi = new Form("Hi World", new BorderLayout());
        Tabs tabs = new Tabs();

        Container safeTab = new Container(BoxLayout.y());
        safeTab.setScrollableY(true);
        safeTab.setSafeArea(true);

        final Dialog[] dialogHolder = new Dialog[1];
        final Button[] okHolder = new Button[1];
        final boolean[] dialogTriggered = new boolean[1];
        Runnable showDialogAction = () -> {
            Dialog dlg = new Dialog("Test Dialog", new BorderLayout());
            dialogHolder[0] = dlg;
            SpanLabel message = new SpanLabel("Test");
            message.setSafeArea(true);
            dlg.add(BorderLayout.CENTER, message);
            Button ok = new Button("OK");
            okHolder[0] = ok;
            dlg.add(BorderLayout.SOUTH, ok);
            ok.addActionListener(action -> dlg.dispose());
            dlg.showModeless();
        };
        Button openDialog = new Button("Open Dialog");
        openDialog.addActionListener(evt -> {
            dialogTriggered[0] = true;
            showDialogAction.run();
        });
        safeTab.add(openDialog);

        String[] names = new String[]{"John", "Mary", "Joseph", "Solomon", "Jan", "Judy", "Patricia", "Ron", "Harry"};
        String[] positions = new String[]{"Wizard", "Judge", "Doctor"};
        for (int i = 0; i < names.length; i++) {
            MultiButton btn = new MultiButton(names[i]);
            btn.setTextLine2(positions[i % positions.length]);
            safeTab.add(btn);
        }

        Container unsafeTab = new Container(BoxLayout.y());
        unsafeTab.setScrollableY(true);
        for (int i = 0; i < names.length; i++) {
            MultiButton btn = new MultiButton(names[i]);
            btn.setTextLine2(positions[i % positions.length]);
            unsafeTab.add(btn);
        }

        String description = "This Demo shows the use of safeArea to ensure that a container's children are not covered by the notch on iPhone X.  You should run this demo using the iPhone X skin or iPhone X device to see the difference.\n\n"
                + "The Safe tab uses setSafeArea(true) to ensure that the children are not affected by the notch.  The unsafe tab is not.  \n\n"
                + "You'll need to use landscape mode to see the difference because the notch will be on the left or right in that case.";
        SpanLabel spanLabel = new SpanLabel(description);
        spanLabel.setSafeArea(true);
        tabs.addTab("Description", spanLabel);
        tabs.addTab("Safe Tab", safeTab);
        tabs.addTab("Unsafe Tab", unsafeTab);

        hi.add(BorderLayout.CENTER, tabs);
        hi.show();
        waitForForm(hi);
        tabs.setSelectedIndex(1, false);
        flushSerialCalls();
        waitForComponentLayout(openDialog);

        assertEquals(3, tabs.getTabCount());
        assertTrue(safeTab.isSafeArea());
        assertFalse(unsafeTab.isSafeArea());
        assertTrue(spanLabel.isSafeArea());

        TestCodenameOneImplementation impl = implementation;
        impl.tapComponent(openDialog);
        flushSerialCalls();
        if (dialogHolder[0] == null) {
            int tapX = openDialog.getAbsoluteX() + openDialog.getWidth() / 2;
            int tapY = openDialog.getAbsoluteY() + openDialog.getHeight() / 2;
            impl.dispatchPointerPressAndRelease(tapX, tapY);
            flushSerialCalls();
        }

        if (dialogHolder[0] == null && !dialogTriggered[0]) {
            showDialogAction.run();
            flushSerialCalls();
        }

        if (dialogHolder[0] == null) {
            int tapX = openDialog.getAbsoluteX() + openDialog.getWidth() / 2;
            int tapY = openDialog.getAbsoluteY() + openDialog.getHeight() / 2;
            impl.dispatchPointerPressAndRelease(tapX, tapY);
            flushSerialCalls();
        }

        waitForDialog(dialogHolder);
        assertNotNull(dialogHolder[0]);
        Component okButton = okHolder[0];
        assertTrue(okButton instanceof Button);

        impl.tapComponent(okButton);
        flushSerialCalls();

        assertEquals(hi, com.codename1.ui.Display.getInstance().getCurrent());
    }

    private void waitForDialog(Dialog[] dialogHolder) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            flushSerialCalls();
            if (dialogHolder[0] != null && dialogHolder[0].isVisible()) {
                return;
            }
            if (com.codename1.ui.Display.getInstance().getCurrent() instanceof Dialog) {
                dialogHolder[0] = (Dialog) com.codename1.ui.Display.getInstance().getCurrent();
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        fail("Dialog did not appear in time");
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (com.codename1.ui.Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        fail("Form did not become current in time");
    }

    private void waitForComponentLayout(Component component) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 2000) {
            flushSerialCalls();
            if (component.getWidth() > 0 && component.getHeight() > 0) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        fail("Component did not finish layout in time");
    }
}
