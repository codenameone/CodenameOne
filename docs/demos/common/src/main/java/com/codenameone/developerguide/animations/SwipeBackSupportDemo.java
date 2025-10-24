package com.codenameone.developerguide.animations;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.SwipeBackSupport;
import com.codename1.util.LazyValue;
import com.codenameone.developerguide.Demo;

/**
 * Demonstrates SwipeBackSupport usage.
 */
public class SwipeBackSupportDemo implements Demo {
    @Override
    public String getTitle() {
        return "Swipe Back";
    }

    @Override
    public String getDescription() {
        return "Enables iOS-style swipe back navigation.";
    }

    @Override
    public void show(Form parent) {
        Form hi = new Form("Swipe Back", BoxLayout.y());
        Form dest = new Form("Destination");
        dest.getToolbar().addCommandToLeftBar("Back", null, e -> hi.showBack());
        // tag::swipeBackBindExample[]
        SwipeBackSupport.bindBack(dest, (args) -> hi);
        // end::swipeBackBindExample[]
        Button go = new Button("Open Destination");
        go.addActionListener(e -> dest.show());
        hi.add(go);
        AnimationDemoUtil.show(hi, parent);
    }

    private void bindBack(Form currentForm, LazyValue<Form> destination) {
        // tag::swipeBackBind[]
        SwipeBackSupport.bindBack(currentForm, destination);
        // end::swipeBackBind[]
    }
}
