/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.testing.AbstractTest;
import static com.codename1.ui.CN.BASELINE;
import static com.codename1.ui.CN.BOTTOM;
import static com.codename1.ui.CN.CENTER;
import static com.codename1.ui.CN.RIGHT;
import static com.codename1.ui.CN.TOP;
import static com.codename1.ui.Component.LEFT;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;

import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.UIManager;

/**
 *
 * @author shannah
 */
public class LayoutTests extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        flowLayoutTests();
        layeredLayoutTests();
        boxLayoutTests();
        return true;
    }
    
    public void layeredLayoutTests() throws Exception {
        System.out.println("LayeredLayout tests");
        Container cnt = new Container(new LayeredLayout());
        Label l = new Label("Hello World");
        int prefW = l.getPreferredW();
        int prefH = l.getPreferredH();
        cnt.add(l);

        int cntW = 1000;
        int cntH = 1000;

        cnt.setWidth(cntW);
        cnt.setHeight(cntH);
        $(cnt).setPadding(0);
        $(cnt, l).setMargin(0);
        LayeredLayout ll = (LayeredLayout)cnt.getLayout();
        ll.setInsets(l, "0");

        cnt.revalidate();
        assertEqual(cntW, l.getWidth());
        assertEqual(cntH, l.getHeight());
        assertEqual(0, l.getX());
        assertEqual(0, l.getY());

        ll.setInsets(l, "auto");
        cnt.revalidate();

        // NOTE:  We give a relative error grace of 1.0 because of the way that 
        // Layered layout calculates insets.  Since it uses floating point arithmetic
        // internally, it is possible that widths will be off by +- 1.
        assertEqual(prefH, l.getHeight(), 1.0);
        assertEqual(prefW, l.getWidth(), 1.0);

        assertEqual((cntW-prefW)/2, l.getX(), 1.0);
        assertEqual((cntH-prefH)/2, l.getY(), 1.0);


        ll.setInsets(l, "auto 0 0 0");
        cnt.revalidate();
        assertEqual(prefH, l.getHeight(), 1.0);
        assertEqual(cntW, l.getWidth(), 1.0);
        assertEqual(cntW-l.getHeight(), l.getY(), 1.0);
        assertEqual(0, l.getX(), 1.0);

        ll.setInsets(l, "0 auto 0 0");
        cnt.revalidate();
        assertEqual(cntH, l.getHeight(), 1.0);
        assertEqual(prefW, l.getWidth(), 1.0);
        assertEqual(0, l.getY(), 1.0);
        assertEqual(0, l.getX(), 1.0);

        ll.setInsets(l, "0 0 auto 0");
        cnt.revalidate();
        assertEqual(prefH, l.getHeight(), 1.0);
        assertEqual(cntW, l.getWidth(), 1.0);
        assertEqual(0, l.getY(), 1.0);
        assertEqual(0, l.getX(), 1.0);

        ll.setInsets(l, "0 0 0 auto");
        cnt.revalidate();
        assertEqual(cntH, l.getHeight(), 1.0);
        assertEqual(prefW, l.getWidth(), 1.0);
        assertEqual(0, l.getY(), 1.0);
        assertEqual(cntW - prefW, l.getX(), 1.0);

        $(cnt).setPadding(10);
        cnt.revalidate();
        assertEqual(cntH - cnt.getStyle().getVerticalPadding(), l.getHeight(), 1.0);
        assertEqual(prefW, l.getWidth(), 1.0);
        assertEqual(cnt.getStyle().getPaddingTop(), l.getY(), 1.0);
        assertEqual(cntW - prefW - cnt.getStyle().getPaddingRightNoRTL(), l.getX(), 1.0);

        ll.setInsets(l, "auto");
        cnt.revalidate();
        assertEqual(prefH, l.getHeight(), 1.0);
        assertEqual(prefW, l.getWidth(), 1.0);
        assertEqual((cntW-prefW)/2, l.getX(), 1.0);
        assertEqual((cntH-prefH)/2, l.getY(), 1.0);

        $(cnt).setPadding(0, 10, 10, 0);
        cnt.revalidate();
        assertEqual(prefH, l.getHeight(), 1.0);
        assertEqual(prefW, l.getWidth(), 1.0);
        assertEqual((cntW-prefW-10)/2, l.getX(), 1.0);
        assertEqual((cntH-prefH-10)/2, l.getY(), 1.0);

        $(cnt).setPadding(0);
        ll.setInsets(l, "10mm");
        int inset = CN.convertToPixels(10);
        cnt.revalidate();
        assertEqual(cnt.getHeight() - 2*inset, l.getHeight(), 1.0);
        assertEqual(cnt.getWidth() - 2*inset, l.getWidth(), 1.0);
        assertEqual(inset, l.getX(), 1.0);
        assertEqual(inset, l.getY(), 1.0);

        ll.setInsets(l, "10%");
        int insetH = (int)Math.round(cnt.getWidth() * 0.1);
        int insetV = (int)Math.round(cnt.getHeight() * 0.1);
        cnt.revalidate();
        assertEqual(cnt.getHeight() - 2*insetV, l.getHeight(), 1.0);
        assertEqual(cnt.getWidth() - 2*insetH, l.getWidth(), 1.0);
        assertEqual(insetH, l.getX(), 1.0);
        assertEqual(insetV, l.getY(), 1.0);

        ll.setInsets(l, "10px");
        insetH = 10;
        insetV = 10;
        cnt.revalidate();
        assertEqual(cnt.getHeight() - 2*insetV, l.getHeight(), 1.0);
        assertEqual(cnt.getWidth() - 2*insetH, l.getWidth(), 1.0);
        assertEqual(insetH, l.getX(), 1.0);
        assertEqual(insetV, l.getY(), 1.0);

        Label l2 = new Label("Label 2");
        Label l3 = new Label("Label 3");
        $(l2, l3).setMargin(0);
        cnt.addAll(l2, l3);
        ll.setInsets(l, "0 auto auto 0"); // top left
        ll.setInsets(l2, "0");
        ll.setInsets(l3, "10mm auto 10mm auto");
        int l3Inset = CN.convertToPixels(10);
        ll.setReferenceComponentLeft(l2, l, 1f);
        ll.setReferenceComponentLeft(l3, l2);
        cnt.revalidate();
        assertEqual(0, l.getX(), 1.0);
        assertEqual(0, l.getY(), 1.0);
        assertEqual(prefW, l.getWidth(), 1.0);
        assertEqual(prefH, l.getHeight(), 1.0);
        assertEqual(l.getX() + l.getWidth(), l2.getX(), 1.0);
        assertEqual(0, l2.getY(), 1.0);
        assertEqual(cnt.getWidth() - l.getWidth(), l2.getWidth(), 1.0);
        assertEqual(cnt.getHeight(), l2.getHeight());

        assertEqual(l3.getPreferredW(), l3.getWidth(), 1.0);
        assertEqual(l3Inset, l3.getY());
        assertEqual(l2.getX() + (l2.getWidth() - l3.getWidth())/2, l3.getX(), 1.0);
        assertEqual(cnt.getHeight() - 2*inset, l3.getHeight());

        ll.setReferenceComponentLeft(l3, null);
        ll.setReferenceComponentRight(l3, l2, 1f);
        cnt.revalidate();
        assertEqual((l2.getX() - l3.getPreferredW())/2, l3.getX(), 1.0);
        assertEqual(l3.getPreferredW(), l3.getWidth(), 1.0);

        ll.setReferenceComponentLeft(l3, l2, 0);
        ll.setReferenceComponentRight(l3, l2, 0);
        ll.setReferenceComponentTop(l3, l2, 0);
        ll.setReferenceComponentBottom(l3, l2, 0);
        cnt.revalidate();
        assertEqual(l2.getX() + (l2.getWidth() - l3.getPreferredW())/2, l3.getX());
        assertEqual(l2.getY() + l3Inset, l3.getY(), 1.0);
        assertEqual(l2.getHeight() - 2 * l3Inset, l3.getHeight(), 1.0);
        assertEqual(l3.getPreferredW(), l3.getWidth(), 1.0);


        // Percent inset anchors tests
        ll.setInsets(l, "auto 50% auto auto");
        ll.setPercentInsetAnchorHorizontal(l, 1);
        cnt.revalidate();
        assertEqual(cnt.getWidth()/2, l.getX(), 1.0);
        assertEqual(l.getPreferredW(), l.getWidth(), 1.0);
        assertEqual(l.getPreferredH(), l.getHeight(), 1.0);
        assertEqual((cnt.getHeight() - l.getPreferredH())/2, l.getY(), 1.0);

        ll.setPercentInsetAnchorVertical(l, 1);
        cnt.revalidate();
        assertEqual(cnt.getWidth()/2, l.getX(), 1.0);
        assertEqual(l.getPreferredW(), l.getWidth(), 1.0);
        assertEqual(l.getPreferredH(), l.getHeight(), 1.0);
        assertEqual((cnt.getHeight() - l.getPreferredH())/2, l.getY(), 1.0);

        ll.setInsets(l, "50% 50% auto auto");
        cnt.revalidate();
        assertEqual(cnt.getWidth()/2, l.getX(), 1.0);
        assertEqual(l.getPreferredW(), l.getWidth(), 1.0);
        assertEqual(l.getPreferredH(), l.getHeight(), 1.0);
        assertEqual(cnt.getHeight()/2 - l.getPreferredH(), l.getY(), 1.0);

        ll.setPercentInsetAnchorVertical(l, 0);
        cnt.revalidate();
        assertEqual(cnt.getHeight()/2, l.getY(), 1.0);
            
    }
    
    public boolean flowLayoutTests() throws Exception {
        System.out.println("FlowLayout Tests");
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
    
    

    public boolean boxLayoutTests() throws Exception {
        System.out.println("BoxLayout Tests");
        BoxLayout l = new BoxLayout(BoxLayout.Y_AXIS);
        Container cnt = new Container(l);
        cnt.setRTL(false);
        int w = 500;
        int h = 500;

        cnt.setWidth(w);
        cnt.setHeight(h);

        Component child1 = createEmptyComponent(100, 100);
        Component child2 = createEmptyComponent(200, 50);
        cnt.add(child1).add(child2);

        $(child1, child2, cnt).setPadding(0).setMargin(0);
        cnt.layoutContainer();
        assertEqual(0, child1.getY(), "child1 should be aligned top");
        assertEqual(100, child2.getY(), "child 2 should be aligned top just after child1 ");
        assertEqual(0, child1.getX(), "Child1 not aligned left");
        assertEqual(w, child1.getWidth(), "Child1 not taking full width");
        assertEqual(0, child2.getX(), "Child2 not aligned left");
        assertEqual(w, child2.getWidth(), "Child2 not taking full width");

        l.setAlign(BOTTOM);
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();
        assertEqual(500, child2.getY() + child2.getHeight(), "Child2 should be aligned bottom");
        assertEqual(450, child1.getY() + child1.getHeight(), "child 1 should be aligned bottom just above child2");
        assertEqual(0, child1.getX(), "Child1 not aligned left");
        assertEqual(w, child1.getWidth(), "Child1 not taking full width");
        assertEqual(0, child2.getX(), "Child2 not aligned left");
        assertEqual(w, child2.getWidth(), "Child2 not taking full width");

        l.setAlign(CENTER);
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();
        assertEqual(325, child2.getY() + child2.getHeight(), "Child2 should be aligned bottom");
        assertEqual(175, child1.getY(), "child 1 should be aligned bottom just above child2");
        assertEqual(0, child1.getX(), "Child1 not aligned left");
        assertEqual(w, child1.getWidth(), "Child1 not taking full width");
        assertEqual(0, child2.getX(), "Child2 not aligned left");
        assertEqual(w, child2.getWidth(), "Child2 not taking full width");

        Component child3 = createEmptyComponent(500, 500);
        $(child3).setPadding(0).setMargin(0);
        cnt.add(child3);
        // This is a component to tip it over the edge.

        // NOTICE:  When the children fill the height of the container, the align property
        // ceases to have meaning.  We do NOT try to align the components once the container is filled.
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();
        assertEqual(0, child1.getY(), "Child1 should be aligned top");
        assertEqual(100, child2.getY(), "Child 2 should be aligned just after");
        assertEqual(150, child3.getY(), "Child 3 should be just after");

        assertEqual(0, child1.getX(), "Child1 not aligned left");
        assertEqual(w, child1.getWidth(), "Child1 not taking full width");
        assertEqual(0, child2.getX(), "Child2 not aligned left");
        assertEqual(w, child2.getWidth(), "Child2 not taking full width");

        l.setAlign(BOTTOM);
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();
        assertEqual(0, child1.getY(), "Child1 should be aligned top");
        assertEqual(100, child2.getY(), "Child 2 should be aligned just after");
        assertEqual(150, child3.getY(), "Child 3 should be just after");

        assertEqual(0, child1.getX(), "Child1 not aligned left");
        assertEqual(w, child1.getWidth(), "Child1 not taking full width");
        assertEqual(0, child2.getX(), "Child2 not aligned left");
        assertEqual(w, child2.getWidth(), "Child2 not taking full width");

        //Now test the x axis
        l = BoxLayout.x();
        cnt.setLayout(l);
        cnt.removeComponent(child3);
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();

        assertEqual(0, child1.getX(), "Child1 not aligned left");
        assertEqual(100, child1.getWidth(), "Child2 not taking preferred width");
        assertEqual(h, child1.getHeight(), "Child1 not taking full height");
        assertEqual(0, child1.getY(), "Child1 not aligning top");
        assertEqual(100, child2.getX(), "Child2 should be aligned next to child1");
        assertEqual(200, child2.getWidth(), "Child 2 not taking preferred width");
        assertEqual(h, child2.getHeight(), "Child 2 not taking full height of container");
        assertEqual(0, child2.getY(), "Child2 not aligning top");

        l.setAlign(CENTER);
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();

        assertEqual(100, child1.getX(), "Child1 not aligned left");
        assertEqual(100, child1.getWidth(), "Child2 not taking preferred width");
        assertEqual(h, child1.getHeight(), "Child1 not taking full height");
        assertEqual(0, child1.getY(), "Child1 not aligning top");
        assertEqual(200, child2.getX(), "Child2 should be aligned next to child1");
        assertEqual(200, child2.getWidth(), "Child 2 not taking preferred width");
        assertEqual(h, child2.getHeight(), "Child 2 not taking full height of container");
        assertEqual(0, child2.getY(), "Child2 not aligning top");

        l.setAlign(RIGHT);
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();

        assertEqual(200, child1.getX(), "Child1 not aligned left");
        assertEqual(100, child1.getWidth(), "Child2 not taking preferred width");
        assertEqual(h, child1.getHeight(), "Child1 not taking full height");
        assertEqual(0, child1.getY(), "Child1 not aligning top");
        assertEqual(300, child2.getX(), "Child2 should be aligned next to child1");
        assertEqual(200, child2.getWidth(), "Child 2 not taking preferred width");
        assertEqual(h, child2.getHeight(), "Child 2 not taking full height of container");
        assertEqual(0, child2.getY(), "Child2 not aligning top");

        return true;
    }

   

}
