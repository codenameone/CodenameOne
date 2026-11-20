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

import com.codename1.io.CSVWriter;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.NativeDragAndDrop;
import com.codename1.ui.NativeDropEvent;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Spreadsheet files in and out.
///
/// Export writes the file into the app's home and offers the platform's share
/// sheet where there is one, so it can be mailed or saved to a drive. Import
/// takes the text pasted into a box, or -- where the platform has native drag
/// and drop -- a `.csv` file dropped onto that box. Codename One has no file
/// open dialog of its own, which is why import is paste or drop.
final class Files {
    interface Read {
        void read(String text);
    }

    private Files() {
    }

    static void saveCsv(String name, String[][] rows) {
        CSVWriter writer = new CSVWriter(',');
        writer.setEscapeFormulas(true);
        save(name, writer.toString(rows), "text/csv");
    }

    static void save(String name, String text, String mime) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath() + name;
        OutputStream out = null;
        try {
            out = fs.openOutputStream(path);
            out.write(text.getBytes("UTF-8"));
        } catch (IOException failed) {
            Log.e(failed);
            Ui.say("The file could not be written");
            return;
        } finally {
            Util.cleanup(out);
        }
        if (CN.isNativeShareSupported()) {
            CN.share(null, path, mime);
        } else {
            Ui.say(Lang.tr("Saved {0}", path));
        }
    }

    /// Asks for CSV text: pasted, or a dropped file where drops work.
    static void askCsv(String title, final Read read) {
        Container body = new Container(new BorderLayout());
        final TextArea box = new TextArea(8, 40);
        box.setUIID("SvField");
        box.setName("csv-paste");
        box.setHint(Lang.tr("Paste the spreadsheet's rows here"));
        Container top = new Container(BoxLayout.y());
        top.add(Ui.text(Lang.tr("The first row names the columns, as in an exported file."),
                "SvMuted"));
        if (NativeDragAndDrop.isSupported()) {
            Label drop = new Label(Lang.tr("Or drop a .csv file here"), "MeDrop");
            drop.setName("csv-drop");
            drop.setNativeDropTarget(true);
            drop.addNativeDropListener(e -> {
                if (!(e instanceof NativeDropEvent)) {
                    return;
                }
                String[] files = ((NativeDropEvent) e).getFiles();
                if (files != null && files.length > 0) {
                    String text = readFile(files[0]);
                    if (text != null) {
                        box.setText(text);
                    }
                } else if (((NativeDropEvent) e).getText() != null) {
                    box.setText(((NativeDropEvent) e).getText());
                }
            });
            top.add(drop);
        }
        body.add(BorderLayout.NORTH, top);
        body.add(BorderLayout.CENTER, box);
        com.codename1.ui.Command ok = com.codename1.ui.Command.create(Lang.tr("Import"), null, null);
        com.codename1.ui.Command cancel = com.codename1.ui.Command.create(Lang.tr("Cancel"), null,
                null);
        if (Dialog.show(Lang.tr(title), body, cancel, ok) == ok && box.getText().trim().length() > 0) {
            read.read(box.getText());
        }
    }

    static String readFile(String path) {
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(path);
            return new String(Util.readInputStream(in), "UTF-8");
        } catch (IOException failed) {
            Log.e(failed);
            Ui.say("That file could not be read");
            return null;
        } finally {
            Util.cleanup(in);
        }
    }
}
