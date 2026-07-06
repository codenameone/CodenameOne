package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import static com.codename1.ui.ComponentSelector.$;

class ComponentSelectorJava005Snippet {
    void snippet(Container table) {
        // tag::component-selector-java-005[]
        $(".even", table)
            .selectPressedStyle()
            .setBgColor(0xcccccc)
            .setBgTransparency(255);
        // end::component-selector-java-005[]
    }
}
