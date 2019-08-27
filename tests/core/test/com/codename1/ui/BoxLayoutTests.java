/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.testing.AbstractTest;
import static com.codename1.ui.CN.BOTTOM;
import static com.codename1.ui.CN.CENTER;
import static com.codename1.ui.CN.RIGHT;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;

/**
 *
 * @author shannah
 */
public class BoxLayoutTests extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
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

    private Component createEmptyComponent(int width, int height) {
        return new Component() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(width, height);
            }

        };

    }
}
