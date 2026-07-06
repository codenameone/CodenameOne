package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.ui.table.*;
import static com.codename1.ui.ComponentSelector.$;

class ComponentSelectorJava002Snippet {
    void snippet() {
        int numRows = 4;
        int numCols = 3;
        String[][] data = {
            {"Name", "Team", "Role"},
            {"Janet", "Core", "Lead"},
            {"Sam", "Design", "UI"},
            {"Ravi", "QA", "Tester"},
        };

        // tag::component-selector-java-002[]
        TableLayout tl = new TableLayout(numRows, numCols);
        Container table = new Container(tl);
        int rowNum = 0;
        for (String[] row : data) {
            int colNum = 0;
            for (String cell : row) {
                table.add(
                    tl.createConstraint(rowNum, colNum),
                    $(new Button(cell))
                        .setUIID("Label")
                        .addTags(rowNum % 2 == 0 ? "even" : "odd")
                        .asComponent()
                );
                colNum++;
            }
            rowNum++;
        }
        $(".even", table)
            .setBgColor(0xcccccc)
            .setBgTransparency(255);
        // end::component-selector-java-002[]
    }
}
