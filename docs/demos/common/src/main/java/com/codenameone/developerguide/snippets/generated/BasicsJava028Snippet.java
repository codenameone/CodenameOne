package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.ui.layouts.*;

class BasicsJava028Snippet {
    void snippet() {
        Container cnt = new Container(new LayeredLayout());
        Button btn = new Button("Submit");
        LayeredLayout ll = (LayeredLayout)cnt.getLayout();
        cnt.add(btn);

        // tag::basics-java-028[]
        ll.setInsets(btn, "auto auto auto 5mm");
        // end::basics-java-028[]
    }
}
