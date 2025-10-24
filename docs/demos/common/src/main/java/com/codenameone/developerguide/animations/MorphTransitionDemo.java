package com.codenameone.developerguide.animations;

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.animations.MorphTransition;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates MorphTransition usage.
 */
public class MorphTransitionDemo implements Demo {
    @Override
    public String getTitle() {
        return "Morph Transition";
    }

    @Override
    public String getDescription() {
        return "Shows configuring MorphTransition between forms.";
    }

    @Override
    public void show(Form parent) {
        Form f = new Form("Morph Transition");
        MockDemo currentDemo = new MockDemo();
        int n = 1;
        Command backCommand = f.getToolbar().addCommandToLeftBar("Back", null, e -> {
            if (parent != null) {
                parent.showBack();
            }
        });
        runMorphTransitionSnippet(currentDemo, n, backCommand, f);
    }

    private void runMorphTransitionSnippet(MockDemo currentDemo, int n, Command backCommand, Form f) {
        // tag::morphTransition[]
        Form demoForm = new Form(currentDemo.getDisplayName());
        demoForm.setScrollable(false);
        demoForm.setLayout(new BorderLayout());
        Label demoLabel = new Label(currentDemo.getDisplayName());
        demoLabel.setIcon(currentDemo.getDemoIcon());
        demoLabel.setName("DemoLabel");
        demoForm.addComponent(BorderLayout.NORTH, demoLabel);
        demoForm.addComponent(BorderLayout.CENTER, wrapInShelves(n));
        // ...
        demoForm.setBackCommand(backCommand);
        demoForm.setTransitionOutAnimator(
            MorphTransition.create(3000).morph(
                currentDemo.getDisplayName(),
                "DemoLabel"));
        f.setTransitionOutAnimator(
            MorphTransition.create(3000).
                morph(currentDemo.getDisplayName(),
                "DemoLabel"));
        demoForm.show();
        // end::morphTransition[]
    }

    private Component wrapInShelves(int n) {
        return new Label("Shelf " + n);
    }

    private static class MockDemo {
        String getDisplayName() {
            return "Demo";
        }

        Image getDemoIcon() {
            return Image.createImage(10, 10);
        }
    }
}
