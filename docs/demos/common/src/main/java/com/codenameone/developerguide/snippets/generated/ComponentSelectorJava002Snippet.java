/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

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
