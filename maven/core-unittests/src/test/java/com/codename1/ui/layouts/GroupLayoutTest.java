package com.codename1.ui.layouts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.GroupLayout;
import com.codename1.ui.layouts.LayoutStyle;
import org.junit.jupiter.api.Assertions;

public class GroupLayoutTest extends UITestBase {

    @FormTest
    public void testGapSpring() {
        Container cnt = new Container();
        GroupLayout layout = new GroupLayout(cnt);
        cnt.setLayout(layout);

        Label l1 = new Label("Label 1");
        Label l2 = new Label("Label 2");

        // Explicit gap
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .add(l1)
                .add(20, 20, 20) // This creates GapSpring
                .add(l2)
        );

        layout.setVerticalGroup(
            layout.createSequentialGroup().add(l1).add(l2)
        );

        cnt.setWidth(500);
        cnt.setHeight(100);
        cnt.layoutContainer();

        Assertions.assertTrue(l2.getX() >= l1.getX() + l1.getWidth() + 20, "Gap should be at least 20");
    }

    @FormTest
    public void testPaddingSpring() {
        Container cnt = new Container();
        GroupLayout layout = new GroupLayout(cnt);
        cnt.setLayout(layout);

        Label l1 = new Label("Label 1");
        Label l2 = new Label("Label 2");

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        // This creates a PaddingSpring internally
        hGroup.add(l1)
              .addPreferredGap(l1, l2, LayoutStyle.RELATED)
              .add(l2);

        layout.setHorizontalGroup(hGroup);
        layout.setVerticalGroup(layout.createSequentialGroup().add(l1).add(l2));

        // Trigger layout calculation which invokes PaddingSpring methods
        cnt.setWidth(500);
        cnt.setHeight(100);
        cnt.layoutContainer();

        // Check if layout happened (positions changed)
        // PaddingSpring should add gap.
        // l1 should be at left margin.
        // l2 should be at l1.x + l1.w + gap.
        Assertions.assertTrue(l2.getX() > l1.getX() + l1.getWidth(), "Gap should exist between l1 and l2");
    }
}
