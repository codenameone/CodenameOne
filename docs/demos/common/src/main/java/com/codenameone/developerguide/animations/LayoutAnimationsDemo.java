package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.developerguide.Demo;

/**
 * Demo for layout animations.
 */
public class LayoutAnimationsDemo implements Demo {
    @Override
    public String getTitle() {
        return "Layout Animations";
    }

    @Override
    public String getDescription() {
        return "Demonstrates animateLayout() when adding components.";
    }

    @Override
    public void show(Form parent) {
        // tag::layoutAnimations[]
        Form hi = new Form("Layout Animations", new BoxLayout(BoxLayout.Y_AXIS));
        Button fall = new Button("Fall"); // <1>
        fall.addActionListener((e) -> {
            for (int iter = 0; iter < 10; iter++) {
                Label b = new Label("Label " + iter);
                b.setWidth(fall.getWidth());
                b.setHeight(fall.getHeight());
                b.setY(-fall.getHeight());
                hi.add(b);
            }
            hi.animateLayout(20000); // <2>
        });
        hi.add(fall);
        // end::layoutAnimations[]
        AnimationDemoUtil.show(hi, parent);
    }
}
