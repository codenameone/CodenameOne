package com.codenameone.developerguide.snippets.generated;

import com.codename1.components.*;
import com.codename1.ui.*;
import com.codename1.ui.table.TableLayout;

class BasicsJava021Snippet {
    void snippet() {
        // tag::basics-java-021[]
        Container table = TableLayout.encloseIn(3,
                new Label("First"),
                new Label("Second"),
                new Label("Third grows"));
        Form hi = new Form("TableLayout.encloseIn()");
        hi.add(table);
        hi.show();
        // end::basics-java-021[]
    }
}
