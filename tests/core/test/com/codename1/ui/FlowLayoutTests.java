/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.testing.AbstractTest;
import static com.codename1.ui.CN.BASELINE;
import static com.codename1.ui.CN.BOTTOM;
import static com.codename1.ui.CN.TOP;
import static com.codename1.ui.Component.CENTER;
import static com.codename1.ui.Component.LEFT;
import static com.codename1.ui.Component.RIGHT;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.geom.Dimension;

import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIManager;

/**
 *
 * @author shannah
 */
public class FlowLayoutTests extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        boolean rtl = UIManager.getInstance().getLookAndFeel().isRTL();
        try {
            UIManager.getInstance().getLookAndFeel().setRTL(false);
            FlowLayout fl = new FlowLayout();
            Container cnt = new Container(fl);
            cnt.setRTL(false);

            Label lbl = new Label("Test");
            $(cnt, lbl).selectAllStyles().setPadding(0).setMargin(0);
            lbl.setRTL(false);
            cnt.add(lbl);
            int cntWidth = 500;
            int cntHeight = 500;
            int labelPreferredWidth = lbl.getPreferredW();
            int labelPreferredHeight = lbl.getPreferredH();
            cnt.setWidth(cntWidth);
            cnt.setHeight(cntHeight);

            // Start with NO margin, NO padding, default FlowLayout, with single child label also with no padding or margin
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "Label should be laid out with its preferred height");
            assertEqual(0, lbl.getX(), "Label should have x=0 in default FlowLayout");
            assertEqual(0, lbl.getY(), "Label should have y=0 in default FlowLayout");

            // Now try with Change to RTL.  FlowLayout should respect RTL so that left padding/margin/align is interpreted
            // as the opposite.
            cnt.setRTL(true);
            lbl.setRTL(true);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "Label should be laid out with its preferred width in RTL");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "Label should be laid out with its preferred height in RTL");
            assertEqual(cntWidth - lbl.getWidth(), lbl.getX(), "Label should be aligned right default FlowLayout in RTL");
            assertEqual(0, lbl.getY(), "Label should have y=0 in default FlowLayout in RTL");

            // Now add left Margin to the label.  This should be applied to right side in RTL
            lbl.getStyle().setMarginLeft(10);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "LM10: Label should be laid out with its preferred width in RTL ");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "LM10: Label should be laid out with its preferred height in RTL");
            assertEqual(cntWidth - lbl.getWidth() - 10, lbl.getX(), "LM10: Label should be aligned right default FlowLayout in RTL");
            assertEqual(0, lbl.getY(), "LM10: Label should have y=0 in default FlowLayout in RTL");

            // Now change to LTR
            cnt.setRTL(false);
            lbl.setRTL(false);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "LM10: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "LM10: Label should be laid out with its preferred height");
            assertEqual(10, lbl.getX(), "LM10: Label should be aligned left with 10 offset default FlowLayout");
            assertEqual(0, lbl.getY(), "LM10: Label should have y=0 in default FlowLayout");

            // Now add left padding to the container.  In RTL this should be applied on right
            cnt.getStyle().setPaddingLeft(12);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "CP12,LM10: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "CP12,LM10: Label should be laid out with its preferred height");
            assertEqual(22, lbl.getX(), "CP12,LM10: Label should be aligned left with 22 offset default FlowLayout");
            assertEqual(0, lbl.getY(), "CP12,LM10: Label should have y=0 in default FlowLayout");

            // Now change back to RTL
            cnt.setRTL(true);
            lbl.setRTL(true);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "CP12,LM10: Label should be laid out with its preferred width in RTL");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "CP12,LM10: Label should be laid out with its preferred height in RTL");
            assertEqual(cntWidth - lbl.getWidth() - 22, lbl.getX(), "CP12,LM10: Label should be aligned right with 22 offset default FlowLayout in RTL");
            assertEqual(0, lbl.getY(), "CP12,LM10: Label should have y=0 in default FlowLayout in RTL");

            // Now change padding to right.  in RTL this is applied to left.  This shouldn't affect the layout at all
            cnt.getStyle().setPaddingLeft(0);
            cnt.getStyle().setPaddingRight(12);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "CP12,LM10: Label should be laid out with its preferred width in RTL");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "CP12,LM10: Label should be laid out with its preferred height in RTL");
            assertEqual(cntWidth - lbl.getWidth() - 10, lbl.getX(), "CP12,LM10: Label should be aligned left with 22 offset default FlowLayout in RTL");
            assertEqual(0, lbl.getY(), "CP12,LM10: Label should have y=0 in default FlowLayout in RTL");

            // Now in LTR
            cnt.setRTL(false);
            lbl.setRTL(false);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "CP12,LM10: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "CP12,LM10: Label should be laid out with its preferred height");
            assertEqual(10, lbl.getX(), "CP12,LM10: Label should be aligned left with 22 offset default FlowLayout");
            assertEqual(0, lbl.getY(), "CP12,LM10: Label should have y=0 in default FlowLayout");

            // Now add some top padding
            cnt.getStyle().setPaddingTop(5);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "TP5,CP12,LM10: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "TP5,CP12,LM10: Label should be laid out with its preferred height");
            assertEqual(10, lbl.getX(), "TP5,CP12,LM10: Label should be aligned left with 22 offset default FlowLayout");
            assertEqual(5, lbl.getY(), "TP5,CP12,LM10: Label should have y=5 in default FlowLayout");

            // And in RTL
            cnt.setRTL(true);
            lbl.setRTL(true);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "TP5,CP12,LM10: Label should be laid out with its preferred width in RTL");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "TP5,CP12,LM10: Label should be laid out with its preferred height in RTL");
            assertEqual(cntWidth - 10 - lbl.getWidth(), lbl.getX(), "TP5,CP12,LM10: Label should be aligned right with 10 offset default FlowLayout in RTL");
            assertEqual(5, lbl.getY(), "TP5,CP12,LM10: Label should have y=5 in default FlowLayout in RTL");

            // Now let's test center alignment with a single child.
            // All margins and padding back to ZERO and LTR
            $(cnt, lbl).selectAllStyles().setPadding(0).setMargin(0).setRTL(false);
            fl.setAlign(CENTER);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "LTR,Center: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "LTR Center: Label should be laid out with its preferred height");
            assertEqual((cntWidth - lbl.getWidth()) / 2, lbl.getX(), "LTR Center: Label should be aligned  center");
            assertEqual(0, lbl.getY(), "LTR Center: Label should have y=0 in default FlowLayout");

            // Now let's add some margin to the child, and padding to the parent
            // In this case, it should align center in the *inner* bounds of the container (i.e. inside the padding box).
            // But because the child has a left margin of 10, it should actually be 10 to the right of absolute center of this box.
            lbl.getStyle().setMarginLeft(10);
            cnt.getStyle().setPaddingLeft(5);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "LTR,P5,M10, Center: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "LTR,P5,M10, Center: Label should be laid out with its preferred height");
            assertEqual((cnt.getInnerWidth() - lbl.getOuterWidth()) / 2 + 15, lbl.getX(), "LTR,P5,M10, Center: Label should be aligned center");
            assertEqual(0, lbl.getY(), "LTR,P5,M10, Center: Label should have y=0 in default FlowLayout");

            // Now check RTL
            $(cnt, lbl).setRTL(true);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "RTL,P5,M10, Center: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "RTL,P5,M10, Center: Label should be laid out with its preferred height");
            // Expected value:  We subtract LTR expected value from the container width - which should take us to the right edge
            // of the label - so we additionally subtract the label width to take us to the left edge.
            assertEqual(cntWidth - ((cnt.getInnerWidth() - lbl.getOuterWidth()) / 2 + 15) - lbl.getWidth(), lbl.getX(), "RTL,P5,M10, Center: Label should be aligned center");
            assertEqual(0, lbl.getY(), "RTL,P5,M10, Center: Label should have y=0 in default FlowLayout");

            // Now let's test right alignment with a single child.
            fl.setAlign(RIGHT);
            $(cnt, lbl).selectAllStyles().setPadding(0).setMargin(0).setRTL(false);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "LTR,Right: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "LTR,Right: Label should be laid out with its preferred height");
            assertEqual(cntWidth - lbl.getWidth(), lbl.getX(), "LTR,Right: Label should be aligned right");
            assertEqual(0, lbl.getY(), "LTR,Right: Label should have y=0 in default FlowLayout");

            // Now in RTL
            $(cnt, lbl).setRTL(true);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(labelPreferredWidth, lbl.getWidth(), "RTL,Right: Label should be laid out with its preferred width");
            assertEqual(labelPreferredHeight, lbl.getHeight(), "RTL,Right: Label should be laid out with its preferred height");
            assertEqual(0, lbl.getX(), "RTL,Right: Label should be aligned right");
            assertEqual(0, lbl.getY(), "RTL,Right: Label should have y=0 in default FlowLayout");

            // Now add some padding to the left.  This should have no effect on this test because we are aligned right.
            cnt.getStyle().setPaddingLeft(10);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(0, lbl.getX(), "RTL,P10,Right: Label should be aligned right");

            $(lbl, cnt).setRTL(false);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(cntWidth - lbl.getWidth(), lbl.getX(), "LTR,Right: Label should be aligned right");

            // Now let's test vertical alignment
            $(cnt, lbl).selectAllStyles().setPadding(0).setMargin(0);
            fl.setValign(BOTTOM);
            $(cnt, lbl).setRTL(false);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(cntHeight - lbl.getHeight(), lbl.getY(), "Should be aligned bottom");

            // RTL should have no effect on the vertical alignment
            $(cnt, lbl).setRTL(true);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(cntHeight - lbl.getHeight(), lbl.getY(), "Should be aligned bottom");

            //Add some padding to the bottom
            cnt.getStyle().setPaddingBottom(10);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(cntHeight - lbl.getHeight() - 10, lbl.getY(), "Should be aligned bottom");

            // Add bottom margin to label.  This should effectively move it up.
            lbl.getStyle().setMarginBottom(12);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(cntHeight - lbl.getHeight() - 22, lbl.getY(), "Should be aligned bottom");

            // Vertical align center.
            fl.setValign(CENTER);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual((cnt.getInnerHeight() - lbl.getOuterHeight()) / 2, lbl.getY(), "Should be valigned middle");

            // Now valign by row.  With only a single component, this should cause the children to be rendered aligned TOP
            fl.setValignByRow(true);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(0, lbl.getY(), "Should be valigned top, when valignbyrow is true");

            // Check valign=BOTTOM now.  Should be same as center when aligning by row, since it is its own row.
            fl.setValign(BOTTOM);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(0, lbl.getY(), "Should be valigned top, when valignbyrow is true");

            // Baseline should be same as others since there is only a single child.
            fl.setValign(BASELINE);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(0, lbl.getY(), "Should be valigned top, when valignbyrow is true");

            // Now let's try laying out a 2nd child.
            Component spacer1 = createEmptyComponent(100, 100);
            $(spacer1, lbl, cnt).setPadding(0).setMargin(0).setRTL(false);
            cnt.add(spacer1);
            fl.setValign(TOP);
            fl.setAlign(LEFT);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(0, lbl.getY(), "Should be valigned top, when valignbyrow is true");
            assertEqual(0, spacer1.getY(), "Spacer should be aligned to top");
            assertEqual(0, lbl.getX(), "Label should be aligned to left edge");
            assertEqual(lbl.getWidth(), spacer1.getX(), "Spacer should be aligned to right edge of label");
            assertEqual(lbl.getPreferredW(), lbl.getWidth(), "Label should be rendered to its preferred width");
            assertEqual(lbl.getPreferredH(), lbl.getHeight(), "Lable should be rendered to its preferred height");
            assertEqual(100, spacer1.getWidth(), "Spacer should be its preferred width");
            assertEqual(100, spacer1.getHeight(), "Spacer should be its preferred height");

            // Let's try aligning bottom by row with the two of them.
            fl.setValign(BOTTOM);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            int rowH = Math.max(100, lbl.getOuterHeight());
            assertEqual(rowH - lbl.getHeight(), lbl.getY(), "Label should be aligned with the bottom of its row.");
            assertEqual(rowH - 100, spacer1.getY(), "Spacer shoudl be aligned with the bottom of its row");

            // Now let's valign center
            fl.setValign(CENTER);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual((rowH - lbl.getHeight()) / 2, lbl.getY(), "Label should be aligned with the bottom of its row.");
            assertEqual((rowH - 100) / 2, spacer1.getY(), "Spacer shoudl be aligned with the bottom of its row");

            // Now same thing with RTL
            $(cnt, lbl, spacer1).setRTL(true);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual((rowH - lbl.getHeight()) / 2, lbl.getY(), "Label should be aligned with the bottom of its row.");
            assertEqual((rowH - 100) / 2, spacer1.getY(), "Spacer shoudl be aligned with the bottom of its row");

            // Now align top left but still RTL.  Make sure everything is Kosher
            fl.setValign(TOP);
            fl.setAlign(LEFT);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEqual(0, lbl.getY(), "Should be valigned top, when valignbyrow is true");
            assertEqual(0, spacer1.getY(), "Spacer should be aligned to top");
            assertEqual(cntWidth - lbl.getWidth(), lbl.getX(), "Label should be aligned to left edge");
            assertEqual(cntWidth - lbl.getWidth() - spacer1.getWidth(), spacer1.getX(), "Spacer should be aligned to right edge of label");
            assertEqual(lbl.getPreferredW(), lbl.getWidth(), "Label should be rendered to its preferred width");
            assertEqual(lbl.getPreferredH(), lbl.getHeight(), "Lable should be rendered to its preferred height");
            assertEqual(100, spacer1.getWidth(), "Spacer should be its preferred width");
            assertEqual(100, spacer1.getHeight(), "Spacer should be its preferred height");

        } finally {
            UIManager.getInstance().getLookAndFeel().setRTL(rtl);
        }

        return true;

    }

    private Component createEmptyComponent(int width, int height) {
        return new Component() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(width, height);
            }

        };
    }

}
