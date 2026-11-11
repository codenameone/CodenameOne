/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.developerguide.snippets;

import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;

/// Snippets for choosing the virtual keyboard's Next or Done action, included
/// by the developer guide's virtual keyboard section.
public class KeyboardActionSnippets {
    void explicitActions() {
        // tag::keyboard-action-next-done[]
        TextField quantity = new TextField("", "Quantity", 6, TextArea.NUMERIC);
        TextField price = new TextField("", "Price", 8, TextArea.DECIMAL);

        // Numeric, with a Done listener, and still offering Next
        quantity.setDoneListener(e -> save());
        quantity.putClientProperty("nextButton", Boolean.TRUE);

        // Offers Done even if another field follows it
        price.putClientProperty("doneButton", Boolean.TRUE);
        // end::keyboard-action-next-done[]
    }

    void save() {
    }
}
