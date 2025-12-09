package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.FlipTransition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.spinner.Picker;
import static org.junit.jupiter.api.Assertions.*;

public class InterFormContainerSampleTest extends UITestBase {
    boolean actionFired = false;
    boolean destShown = false;

    @FormTest
    public void testInterFormContainer() {
        Button sharedButton = new Button("Shared Button");

        InterFormContainer cnt = new InterFormContainer(sharedButton);
        InterFormContainer cnt2 = new InterFormContainer(sharedButton);
        Toolbar.setGlobalToolbar(true);
        Form hi = new Form("Transitions", new BorderLayout());
        Container c = new Container(BoxLayout.y());
        hi.add(BorderLayout.CENTER, c);
        hi.add(BorderLayout.SOUTH, cnt);
        Style bg = hi.getContentPane().getUnselectedStyle();
        bg.setBgTransparency(255);
        bg.setBgColor(0xff0000);
        Button showTransition = new Button("Show");
        Picker pick = new Picker();
        pick.setStrings("Slide", "SlideFade", "Cover", "Uncover", "Fade", "Flip");
        pick.setSelectedString("Slide");
        TextField duration = new TextField("0", "Duration", 6, TextArea.NUMERIC);
        CheckBox horizontal = CheckBox.createToggle("Horizontal");
        pick.addActionListener((e) -> {
            String s = pick.getSelectedString().toLowerCase();
            horizontal.setEnabled(s.equals("slide") || s.indexOf("cover") > -1);
        });
        horizontal.setSelected(true);
        c.add(showTransition).
                add(pick).
                add(duration).
                add(horizontal);

        Form dest = new Form("Destination", new BorderLayout()) {
            @Override
            public void show() {
                destShown = true;
                super.show();
            }
        };
        sharedButton.addActionListener(e -> {
            if (sharedButton.getComponentForm() == hi) {
                dest.show();
            } else {
                hi.showBack();
            }
        });
        dest.add(BorderLayout.SOUTH, cnt2);
        bg = dest.getContentPane().getUnselectedStyle();
        bg.setBgTransparency(255);
        bg.setBgColor(0xff);
        Command backCmd = new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                hi.showBack();
            }
        };
        dest.setBackCommand(backCmd);
        if (dest.getToolbar() != null) {
            dest.getToolbar().addCommandToLeftBar(backCmd);
        }

        showTransition.addActionListener((e) -> {
            actionFired = true;
            // Disable transitions for unit test stability
            dest.show();
        });
        hi.show();

        // Assertions
        assertEquals(hi, Display.getInstance().getCurrent());
        assertTrue(hi.contains(sharedButton));

        // Simulate click on showTransition to show dest
        showTransition.pressed();
        showTransition.released();

        // Need to run serial calls to process the show() that happens in action listener
        // And maybe flush EDT
        // com.codename1.ui.DisplayTest.flushEdt(); // Causes timeout

        assertTrue(actionFired, "Action listener for showTransition should have been fired");
        assertTrue(destShown, "dest.show() should have been called");

        // In this case, we just fired an event that calls dest.show().
        assertEquals(dest, Display.getInstance().getCurrent());
        assertTrue(dest.contains(sharedButton));

        // Click back
        backCmd.actionPerformed(null);
        assertEquals(hi, Display.getInstance().getCurrent());
        assertTrue(hi.contains(sharedButton));
    }
}
