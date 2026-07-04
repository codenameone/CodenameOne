package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;

class ComponentSelectorJava004Snippet {
    void snippet(Component[] evenComponents) {
        // tag::component-selector-java-004[]
        for (Component c : evenComponents) {
            c.getStyle().setBgColor(0xcccccc);
        }
        // end::component-selector-java-004[]
    }
}
