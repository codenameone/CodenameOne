package com.codenameone.developerguide.advancedtopics;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.MultiButton;
import java.util.Arrays;
import java.util.Collections;

// tag::rearrangeableItems[]
public class RearrangeableItemsDemo {
    public void show() {
        Form hi = new Form("Rearrangeable Items", new BorderLayout());
        String[] buttons = {"A Game of Thrones", "A Clash Of Kings",  "A Storm Of Swords",
            "A Feast For Crows", "A Dance With Dragons", "The Winds of Winter", "A Dream of Spring" };

        Container box = new Container(BoxLayout.y());
        box.setScrollableY(true);
        box.setDropTarget(true);
        java.util.List<String> got = Arrays.asList(buttons);
        Collections.shuffle(got);
        for (String current : got) {
            MultiButton mb = new MultiButton(current);
            box.add(mb);
            mb.setDraggable(true);
        }

        hi.add(BorderLayout.NORTH, "Arrange The Titles").add(BorderLayout.CENTER, box);
        hi.show();
    }
}
// end::rearrangeableItems[]
