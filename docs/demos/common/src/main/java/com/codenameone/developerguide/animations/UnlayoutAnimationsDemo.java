package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.developerguide.Demo;

/**
 * Demo for animateUnlayout().
 */
public class UnlayoutAnimationsDemo implements Demo {
    @Override
    public String getTitle() {
        return "Unlayout Animations";
    }

    @Override
    public String getDescription() {
        return "Demonstrates animateUnlayout*() APIs.";
    }

    @Override
    public void show(Form parent) {
        // tag::unlayoutAnimation[]
        Form hi = new Form("Layout Animations", new BoxLayout(BoxLayout.Y_AXIS));
        Button fall = new Button("Fall");
        fall.addActionListener((e) -> {
            if (hi.getContentPane().getComponentCount() == 1) {
                fall.setText("Rise");
                for (int iter = 0; iter < 10; iter++) {
                    Label b = new Label("Label " + iter);
                    b.setWidth(fall.getWidth());
                    b.setHeight(fall.getHeight());
                    b.setY(-fall.getHeight());
                    hi.add(b);
                }
                hi.animateLayout(20000);
            } else {
                fall.setText("Fall");
                for (int iter = 1; iter < hi.getContentPane().getComponentCount(); iter++) { // <1>
                    Component c = hi.getContentPane().getComponentAt(iter);
                    c.setY(-fall.getHeight()); // <2>
                }
                hi.animateUnlayoutAndWait(20000, 255); // <3>
                hi.removeAll(); // <4>
                hi.add(fall);
                hi.revalidate();
            }
        });
        hi.add(fall);
        // end::unlayoutAnimation[]
        AnimationDemoUtil.show(hi, parent);
    }

    private void animateUnlayoutWithCallback(Form hi, Button fall) {
        // tag::unlayoutCallback[]
        hi.animateUnlayout(20000, 255, () -> {
            hi.removeAll();
            hi.add(fall);
            hi.revalidate();
        });
        // end::unlayoutCallback[]
    }
}
