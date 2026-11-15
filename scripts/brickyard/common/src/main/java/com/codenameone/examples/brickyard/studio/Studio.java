/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.studio;

import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BoxLayout;

/// The creator's editor. A stand-in until the editor is written.
public final class Studio {
    private Studio() {
    }

    /// Opens `experienceId` for editing, or a new experience when it is null.
    public static void open(String experienceId) {
        final Form previous = com.codename1.ui.CN.getCurrentForm();
        Form f = new Form(experienceId == null ? "New experience" : "Studio", BoxLayout.y());
        f.setName("studio");
        f.add(new Label(experienceId == null ? "New experience" : experienceId));
        f.getToolbar().setBackCommand(new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (previous != null) {
                    previous.showBack();
                }
            }
        });
        f.show();
    }
}
