package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.animations.CommonTransitions;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates Container.replace() with transitions.
 */
public class ReplaceTransitionDemo implements Demo {
    @Override
    public String getTitle() {
        return "Replace Transition";
    }

    @Override
    public String getDescription() {
        return "Uses replaceAndWait() to swap components with transitions.";
    }

    @Override
    public void show(Form parent) {
        // tag::replaceTransition[]
        Form hi = new Form("Replace", new BoxLayout(BoxLayout.Y_AXIS));
        Button replace = new Button("Replace Pending");
        Label replaceDestiny = new Label("Destination Replace");
        hi.add(replace);
        replace.addActionListener((e) -> {
            replace.getParent().replaceAndWait(replace, replaceDestiny, CommonTransitions.createCover(CommonTransitions.SLIDE_VERTICAL, true, 800));
            replaceDestiny.getParent().replaceAndWait(replaceDestiny, replace, CommonTransitions.createUncover(CommonTransitions.SLIDE_VERTICAL, true, 800));
        });
        // end::replaceTransition[]
        AnimationDemoUtil.show(hi, parent);
    }
}
