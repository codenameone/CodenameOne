package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import static com.codename1.ui.ComponentSelector.$;

class ComponentSelectorJava006Snippet {
    void snippet(Container table) {
        // tag::component-selector-java-006[]
        $(".even:pressed", table)
            .setBgColor(0xcccccc)
            .setBgTransparency(255);
        // end::component-selector-java-006[]
    }
}
