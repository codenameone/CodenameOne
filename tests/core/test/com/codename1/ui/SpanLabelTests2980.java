/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ui;

import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.testing.AbstractTest;
import static com.codename1.ui.CN.CENTER;
import static com.codename1.ui.CN.CENTER_BEHAVIOR_CENTER;
import static com.codename1.ui.CN.getCurrentForm;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.Layout;

public class SpanLabelTests2980 extends AbstractTest {

    private Layout layout;

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
    
    private void testBorderLayout() {
        System.out.println("Testing SpanLabel preferred size in BorderLayout.  https://github.com/codenameone/CodenameOne/issues/3000");
        //Button showPopUp = new Button("Show PopUp in Border Layout");
        Form f = new Form(BoxLayout.y());
        f.setName("testBorderLayout");
        //f.add(showPopUp);
        SpanLabel messageSpanLabel = new SpanLabel("Tap the following button to open the gallery. You should be able to select multiple images and videos. Tap the following button to open the gallery. You should be able to select multiple images and videos.");
        //showPopUp.addActionListener((e) -> {
        Runnable showPopup = () -> {
            
            messageSpanLabel.setName("messageSpanLabel");
            Container centerContainerOuter = new Container(new BorderLayout(CENTER_BEHAVIOR_CENTER));
            centerContainerOuter.add(CENTER, messageSpanLabel);

            Container layeredPane = getCurrentForm().getLayeredPane();
            layeredPane.setLayout(new LayeredLayout());        
            layeredPane.add(centerContainerOuter);
            layeredPane.setVisible(true);

            getCurrentForm().revalidate();     
        };
        //showPopUp.setName("showBorderLayout");
        f.show();
        waitForFormName("testBorderLayout");
        //clickButtonByName("showBorderLayout");
        showPopup.run();
        waitFor(500); // give time for click to take effect
        SpanLabel spanLabel = messageSpanLabel; //(SpanLabel)findByName("messageSpanLabel");
        Label l = new Label("Tap the following");

        assertTrue(spanLabel.getHeight() > l.getPreferredH() * 2, "Span Label height is too small.  Should be at least a few lines.");
        System.out.println("Finished SpanLabel BorderLayout test");

    }

    @Override
    public boolean runTest() throws Exception {
        if (layout == null) {
            Layout[] layouts = new Layout[]{
                new FlowLayout(),
                BoxLayout.x(),
                BoxLayout.y()
            };
            for (Layout l : layouts) {
                layout = l;
                runTest();
            }
            return true;
        }
        System.out.println("Laying out SpanLabel with layout " + layout);
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
        cnt.setWidth(label.getPreferredW());
        cnt.setHeight(CN.convertToPixels(1000));
        SpanLabel sl = new SpanLabel("Tap the following button to open the gallery. You should be able to select multiple images and videos.");

        sl.setName("TheSpanLabel");
        cnt.add(sl);
        cnt.add(new Button("Click Me"));
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();
        assertTrue(sl.getHeight() > label.getPreferredH() * 2, "Span Label height is too low for layout " + layout + ": was " + sl.getHeight() + " but should be at least " + (label.getPreferredH() * 2));

        SpanButton sb = new SpanButton("Tap the following button to open the gallery. You should be able to select multiple images and videos.");

        sb.setName("TheSpanButton");
        cnt.removeAll();
        cnt.add(sb);
        cnt.add(new Button("Click Me"));
        cnt.setShouldCalcPreferredSize(true);
        cnt.layoutContainer();
        assertTrue(sb.getHeight() > label.getPreferredH() * 2, "Span button height is too low for layout " + layout + ": was " + sb.getHeight() + " but should be at least " + (label.getPreferredH() * 2));

        
        testBorderLayout();
        
        return true;
    }

}
