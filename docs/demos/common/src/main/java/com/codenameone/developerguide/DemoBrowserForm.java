package com.codenameone.developerguide;

import com.codename1.components.MultiButton;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Parent form that lists all demos and allows launching them.
 */
public class DemoBrowserForm extends Form {

    public DemoBrowserForm() {
        super("Developer Guide Demos", new BorderLayout());
        getToolbar().setTitleCentered(false);
        Container listContainer = new Container(BoxLayout.y());
        listContainer.setName("demoList");
        listContainer.setScrollableY(true);

        int index = 0;
        for (Demo demo : DemoRegistry.getDemos()) {
            MultiButton demoButton = new MultiButton(demo.getTitle());
            demoButton.setTextLine2(demo.getDescription());
            demoButton.setName("demoButton-" + index++);
            demoButton.addActionListener(e -> demo.show(DemoBrowserForm.this));
            listContainer.add(demoButton);
        }

        add(BorderLayout.CENTER, listContainer);
    }
}
