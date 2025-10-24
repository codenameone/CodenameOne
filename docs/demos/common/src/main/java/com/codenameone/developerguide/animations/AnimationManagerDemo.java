package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates interaction with the AnimationManager.
 */
public class AnimationManagerDemo implements Demo {
    @Override
    public String getTitle() {
        return "Animation Manager";
    }

    @Override
    public String getDescription() {
        return "Shows how AnimationManager queues operations.";
    }

    @Override
    public void show(Form parent) {
        Form hi = new Form("Animation Manager", BoxLayout.y());
        Container cnt = new Container(BoxLayout.y());
        Button myButton = new Button("Animated");
        cnt.add(myButton);
        hi.add(cnt);
        AnimationDemoUtil.show(hi, parent);
        // Methods below exist only so their code can be included in the guide.
    }

    private void demoProblem(Container cnt, Button myButton) {
        // tag::animationManagerProblem[]
        cnt.add(myButton);
        int componentCount = cnt.getComponentCount();
        cnt.animateLayout(300);
        cnt.removeComponent(myButton);
        if(componentCount == cnt.getComponentCount()) {
            // this will happen...
        }
        // end::animationManagerProblem[]
    }

    private void demoWait(Container cnt, Button myButton) {
        // tag::animationManagerWait[]
        cnt.add(myButton);
        int componentCount = cnt.getComponentCount();
        cnt.animateLayoutAndWait(300);
        cnt.removeComponent(myButton);
        if(componentCount == cnt.getComponentCount()) {
            // this probably won't happen...
        }
        // end::animationManagerWait[]
    }

    private void demoFlush(Container cnt, Button myButton) {
        // tag::animationManagerFlush[]
        cnt.add(myButton);
        int componentCount = cnt.getComponentCount();
        cnt.animateLayout(300);
        cnt.getAnimationManager().flushAnimation(() -> {
            cnt.removeComponent(myButton);
            if(componentCount == cnt.getComponentCount()) {
                // this shouldn't happen...
            }
        });
        // end::animationManagerFlush[]
    }
}
