package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.table.*;

class TheComponentsOfCodenameOneJava072Snippet {
    void snippet() {
        // tag::the-components-of-codename-one-java-072[]
        Form hi = new Form("Table", new BorderLayout());
        TableModel model = new DefaultTableModel(
                new String[] {"Col 1", "Col 2", "Col 3"},
                new Object[][] {
                    {"Row 1", "Row A", 1},
                    {"Row 2", "Row B", 4},
                    {"Row 3", "Row C", 7.5},
                    {"Row 4", "Row D", 2.24},
                });
        Table table = new Table(model);
        table.setSortSupported(true);
        hi.add(BorderLayout.CENTER, table);
        hi.add(BorderLayout.NORTH, new Button("Button"));
        hi.show();
        // end::the-components-of-codename-one-java-072[]
    }
}
