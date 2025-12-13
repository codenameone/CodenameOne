package com.codename1.samples;

import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.ui.*;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.*;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SpanLabelTest2980Test extends UITestBase {

    @FormTest
    public void testSpanLabelLayouts() {
        Layout[] layouts = new Layout[]{
            new FlowLayout(),
            BoxLayout.x(),
            BoxLayout.y()
        };
        for (Layout l : layouts) {
            runTest(l);
        }
    }

    private void runTest(Layout layout) {
        Label label = new Label("Tap the following");

        Container cnt = new Container(layout) {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(label.getPreferredW(), CN.convertToPixels(1000));
            }

            @Override
            public int getWidth() {
                return label.getPreferredW();
            }

            @Override
            public int getHeight() {
                return CN.convertToPixels(1000);
            }
        };
        cnt.setScrollableX(false);
        cnt.setScrollableY(false);
        // We set size to simulate constraint
        cnt.setWidth(label.getPreferredW());
        cnt.setHeight(CN.convertToPixels(1000));

        SpanLabel sl = new SpanLabel("Tap the following button to open the gallery. You should be able to select multiple images and videos.");
        sl.setName("TheSpanLabel");
        cnt.add(sl);
        cnt.add(new Button("Click Me"));
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();

        assertTrue(sl.getHeight() > label.getPreferredH() * 2, "Span Label height is too low for layout "+layout+": was "+sl.getHeight()+" but should be at least "+(label.getPreferredH() * 2));

        SpanButton sb = new SpanButton("Tap the following button to open the gallery. You should be able to select multiple images and videos.");
        sb.setName("TheSpanButton");
        cnt.removeAll();
        cnt.add(sb);
        cnt.add(new Button("Click Me"));
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();

        assertTrue(sb.getHeight() > label.getPreferredH() * 2, "Span button height is too low for layout "+layout+": was "+sb.getHeight()+" but should be at least "+(label.getPreferredH() * 2));
    }
}
