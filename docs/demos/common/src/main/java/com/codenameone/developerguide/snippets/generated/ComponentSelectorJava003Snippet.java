package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import static com.codename1.ui.ComponentSelector.$;

class ComponentSelectorJava003Snippet {
    void snippet(Container table) {
        // tag::component-selector-java-003[]
        $(".even", table)
            .setBgColor(0xcccccc)
            .setBgTransparency(255);
        // end::component-selector-java-003[]
    }
}
