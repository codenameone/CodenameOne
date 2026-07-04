package com.codenameone.developerguide.snippets.generated;

import com.codename1.components.*;
import com.codename1.ui.*;
import com.codename1.ui.table.TableLayout;

class BasicsJava022Snippet {
    void snippet() {
        // tag::basics-java-022[]
        TableLayout layout = new TableLayout(4, 3);
        layout.setGrowHorizontally(true);
        Form hi = new Form("Table Layout", layout);

        TableLayout.Constraint title = layout.createConstraint();
        title.setHorizontalSpan(3);
        title.setHorizontalAlign(Component.CENTER);
        hi.add(title, new Label("Invoice"));

        hi.add(new Label("Item"));
        hi.add(new Label("Qty"));
        hi.add(new Label("Total"));
        hi.add(new Label("Design"));
        hi.add(new Label("2"));
        hi.add(new Label("$120"));

        TableLayout.Constraint notes = layout.createConstraint();
        notes.setHorizontalSpan(2);
        notes.setHeightPercentage(40);
        hi.add(notes, new SpanLabel("Notes span two columns"));
        hi.add(new Button("Pay"));

        hi.show();
        // end::basics-java-022[]
    }
}
