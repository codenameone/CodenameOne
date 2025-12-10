package com.codename1.samples;

import com.codename1.ui.ButtonGroup;
import com.codename1.ui.ComponentGroup;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class RadioButtonLeadComponentTest3105Test extends UITestBase {

    @FormTest
    public void testRadioButtonLeadComponent() {
        Form f = new Form("Lead component breaks ComponentGroup", BoxLayout.y());

        ButtonGroup group = new ButtonGroup();
        ComponentGroup cgroup = new ComponentGroup();
        Label editFieldLabel = new Label("");
        Container visibleField = BorderLayout.centerCenterEastWest(cgroup, editFieldLabel, null);
        RadioButton r1 = new RadioButton("R1");
        group.add(r1);
        cgroup.add(r1);
        RadioButton r2 = new RadioButton("R2");
        group.add(r2);
        cgroup.add(r2);
        f.add(visibleField);

        ButtonGroup group2 = new ButtonGroup();
        ComponentGroup cgroup2 = new ComponentGroup();
        Label editFieldLabel2 = new Label("EDIT2");
        Container visibleField2 = BorderLayout.centerCenterEastWest(cgroup2, editFieldLabel2, null);
        RadioButton r3 = new RadioButton("R3");
        group2.add(r3);
        cgroup2.add(r3);
        RadioButton r4 = new RadioButton("R4");
        group2.add(r4);
        cgroup2.add(r4);
        f.add(visibleField2);

        //What causes the problem:
        cgroup2.setBlockLead(true);
        visibleField2.setLeadComponent(cgroup2);

        f.show();

        // Test logic: Verify selection works even with lead component set
        // Case 1: Normal group
        assertFalse(r1.isSelected());
        assertFalse(r2.isSelected());
        click(r1);
        assertTrue(r1.isSelected());
        click(r2);
        assertTrue(r2.isSelected());
        assertFalse(r1.isSelected());

        // Case 2: Group with Lead Component
        assertFalse(r3.isSelected());
        assertFalse(r4.isSelected());

        // Clicking directly on radio button
        click(r3);
        assertTrue(r3.isSelected());

        click(r4);
        assertTrue(r4.isSelected());
        assertFalse(r3.isSelected());
    }

    private void click(RadioButton rb) {
        rb.pointerPressed(0, 0);
        rb.pointerReleased(0, 0);
        DisplayTest.flushEdt();
    }
}
