package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.ui.layouts.mig.MigLayout;

class BasicsJava035Snippet {
    void snippet() {
        // tag::basics-java-035[]
        Form hi = new Form("MigLayout",
                new MigLayout("wrap 2", "[right][grow]", "[]10[]10[]"));
        hi.add(new Label("First name"));
        hi.add(new TextField("", "First name"), "growx");
        hi.add(new Label("Last name"));
        hi.add(new TextField("", "Last name"), "growx");
        hi.add(new Label("Phone"));
        hi.add(new TextField("", "Phone"), "growx");
        hi.add(new Button("OK"), "span 2, align right");
        hi.show();
        // end::basics-java-035[]
    }
}
