package com.codenameone.developerguide.snippets.generated;

import com.codename1.components.*;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.TableLayout;

class BasicsJava021Snippet {
    void snippet() {
        // tag::basics-java-021[]
        Container table = TableLayout.encloseIn(2,
                new Label("First"),
                new Label("Second"),
                new Label("Third"),
                new Label("Fourth"),
                new Label("Fifth"));
        Form hi = new Form("TableLayout Enclose 2", new BorderLayout());
        hi.add(BorderLayout.CENTER, table);
        hi.show();
        // end::basics-java-021[]
    }
}
