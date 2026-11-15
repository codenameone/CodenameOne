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
package com.codenameone.examples.brickyard.play;

import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BoxLayout;

/// Runs a place. A stand-in until the player is written: it shows the title
/// and goes back to where it was opened from.
public final class PlaySession {
    private PlaySession() {
    }

    /// Starts playing `place`, the published place file of `experienceId`.
    public static void start(String experienceId, String title, byte[] place) {
        final Form previous = com.codename1.ui.CN.getCurrentForm();
        Form f = new Form(title, BoxLayout.y());
        f.setName("play");
        f.add(new Label(title));
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
