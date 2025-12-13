package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates setHidden() usage.
 */
public class HiddenComponentDemo implements Demo {
    @Override
    public String getTitle() {
        return "Hidden Components";
    }

    @Override
    public String getDescription() {
        return "Shows how to hide components with animateLayout.";
    }

    @Override
    public void show(Form parent) {
        Form hi = new Form("Hidden", BoxLayout.y());
        // tag::hiddenComponent[]
        Button toHide = new Button("Will Be Hidden");
        Button hide = new Button("Hide It");
        hide.addActionListener((e) -> {
            hide.setEnabled(false);
            boolean t = !toHide.isHidden();
            toHide.setHidden(t);
            toHide.getParent().animateLayoutAndWait(200);
            toHide.setVisible(!t);
            hide.setEnabled(true);
        });
        // end::hiddenComponent[]
        hi.addAll(toHide, hide);
        AnimationDemoUtil.show(hi, parent);
    }
}
