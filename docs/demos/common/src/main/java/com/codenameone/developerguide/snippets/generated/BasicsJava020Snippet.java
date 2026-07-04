package com.codenameone.developerguide.snippets.generated;

import com.codename1.components.*;
import com.codename1.ui.*;
import com.codename1.ui.table.TableLayout;

class BasicsJava020Snippet {
    void snippet() {
        // tag::basics-java-020[]
        Form hi = new Form("TableLayout", new TableLayout(2, 2));
        hi.add(new Label("First"));
        hi.add(new Label("Second"));
        hi.add(new Label("Third"));
        hi.add(new Label("Fourth"));
        hi.add(new Label("Fifth"));
        hi.show();
        // end::basics-java-020[]
    }
}
