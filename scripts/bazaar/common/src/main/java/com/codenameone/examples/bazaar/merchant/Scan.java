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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.codescan.CodeScanner;
import com.codename1.codescan.ScanResult;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// Barcodes in: the camera scanner on devices that have one, and on desktops
/// a USB scanner, which types the code into the focused field and presses
/// Enter -- a "keyboard wedge". Every code field is therefore an ordinary text
/// field that acts on Enter, with a scan button beside it where scanning works.
final class Scan {
    private Scan() {
    }

    /// A field a scanner can type into; `entered` runs on Enter (may be null).
    static TextField field(String name, final Runnable entered) {
        TextField f = new TextField("", Lang.tr("SKU or barcode"), 16, TextField.ANY);
        f.setUIID("SvField");
        f.getHintLabel().setUIID("SvFieldHint");
        f.setName(name);
        f.setDoneListener(e -> {
            if (entered != null) {
                entered.run();
            }
        });
        return f;
    }

    /// Whether this device can scan with its camera.
    static boolean supported() {
        return !CN.isDesktop() && CodeScanner.getInstance() != null;
    }

    /// A button that scans into `into` and then runs `then` (may be null).
    /// Disabled where there is no camera scanner; the field still takes a
    /// keyboard-wedge scanner.
    static Button button(String name, final TextField into, final Runnable then) {
        Button b = new Button(Lang.tr("Scan"), "SvSecondary");
        b.setName(name);
        Ui.icon(b, FontImage.MATERIAL_QR_CODE_SCANNER, 3.2f);
        if (!supported()) {
            b.setEnabled(false);
            return b;
        }
        b.addActionListener(e -> CodeScanner.getInstance().scanBarCode(new ScanResult() {
            @Override
            public void scanCompleted(String contents, String formatName, byte[] rawBytes) {
                CN.callSerially(() -> {
                    into.setText(contents == null ? "" : contents.trim());
                    if (then != null) {
                        then.run();
                    }
                });
            }

            @Override
            public void scanCanceled() {
            }

            @Override
            public void scanError(int errorCode, String message) {
                CN.callSerially(() -> Ui.say("The code could not be read"));
            }
        }));
        return b;
    }
}
