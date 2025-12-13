package com.codenameone.developerguide.animations;

import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates low level animation registration.
 */
public class LowLevelAnimationDemo implements Demo {
    @Override
    public String getTitle() {
        return "Low Level Animation";
    }

    @Override
    public String getDescription() {
        return "Shows overriding animate() and registering an animation.";
    }

    @Override
    public void show(Form parent) {
        Form myForm = new Form("Low Level", BoxLayout.y());
        StatusComponent status = new StatusComponent();
        myForm.add(status);
        status.onAddedToForm(myForm);
        AnimationDemoUtil.show(myForm, parent);
    }

    private static class StatusComponent extends Label {
        private boolean userStatusPending = true;
        void onAddedToForm(Form myForm) {
            // tag::registerAnimated[]
            myForm.registerAnimated(this);
            // end::registerAnimated[]
        }

        // tag::lowLevelAnimate[]
        private int spinValue;
        @Override
        public boolean animate() {
           if(userStatusPending) {
               spinValue++;
               super.animate();
               return true;
           }
           return super.animate();
        }
        // end::lowLevelAnimate[]
    }
}
