package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates sequencing animations.
 */
public class AnimationSynchronicityDemo implements Demo {
    @Override
    public String getTitle() {
        return "Animation Synchronicity";
    }

    @Override
    public String getDescription() {
        return "Sequences animations using AndWait and fade variants.";
    }

    @Override
    public void show(Form parent) {
        Form hi = new Form("Synchronicity", BoxLayout.y());
        Container effects = new Container(BoxLayout.y());
        Button run = new Button("Run Sequence");
        run.addActionListener((e) -> runSequence(effects));
        hi.addAll(effects, run);
        AnimationDemoUtil.show(hi, parent);
    }

    private void runSequence(Container effects) {
        // tag::animationSequence[]
        arrangeForInterlace(effects);
        effects.animateUnlayoutAndWait(800, 20);
        effects.animateLayoutFade(800, 20);
        // end::animationSequence[]
    }

    private void arrangeForInterlace(Container effects) {
        effects.removeAll();
        effects.add(new Label("Effect " + System.currentTimeMillis()));
        effects.revalidate();
    }
}
