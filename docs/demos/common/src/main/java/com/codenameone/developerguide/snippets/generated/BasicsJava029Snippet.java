package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.ui.layouts.*;

class BasicsJava029Snippet {
    void snippet() {
        Container cnt = new Container(new LayeredLayout());
        Button btn = new Button("Submit");
        LayeredLayout ll = (LayeredLayout)cnt.getLayout();
        cnt.add(btn);

        // tag::basics-java-029[]
        ll.setInsets(btn, "auto 5mm auto auto");
        // end::basics-java-029[]
    }
}
