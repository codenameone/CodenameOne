package com.codenameone.developerguide.animations;

import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;

/**
 * Utility helpers shared by the animation demos.
 */
final class AnimationDemoUtil {
    private AnimationDemoUtil() {
    }

    static void show(Form form, Form parent) {
        if (parent != null) {
            Toolbar toolbar = form.getToolbar();
            toolbar.setBackCommand(toolbar.addCommandToLeftBar("Back", null, e -> parent.showBack()));
        }
        form.show();
    }
}
