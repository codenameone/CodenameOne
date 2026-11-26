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
package com.codenameone.examples.bazaar.docs;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.pdf.ui.PdfViewer;
import com.codename1.printing.Printer;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.NativeDragAndDrop;
import com.codename1.ui.NativeDragOperation;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Ui;
import com.codenameone.examples.bazaar.ui.Windows;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// PDF documents in the app: the ones the server renders (invoices, packing
/// slips, labels, purchase orders, credit notes) and the ones the app makes
/// itself (the pick list).
///
/// A server document is fetched like any other API call -- the session's
/// authorizer puts the access token on every request under `/api`, a raw
/// request included -- and shown in `PdfViewer`, Codename One's pure Java
/// renderer, so the preview looks the same everywhere. Under it: Print where
/// the platform prints, Share where it has a share sheet, Save always, and on
/// a desktop the document itself, dragged out of the window onto the desktop
/// or into a mail. On a desktop the preview is a window of its own, beside
/// the screen it came from.
public final class Pdfs {
    /// What a fetch answers with.
    public interface Got {
        void got(byte[] pdf);
    }

    private Pdfs() {
    }

    /// Fetches `path` (starting `/api/`) and previews it as `fileName`.
    public static void open(final String title, final String fileName, String path) {
        fetch(path, pdf -> preview(title, fileName, pdf));
    }

    /// Fetches `path` and prints it straight away, without a preview.
    public static void print(final String fileName, String path) {
        fetch(path, pdf -> {
            String file = write(fileName, pdf);
            if (file != null) {
                print(file);
            }
        });
    }

    /// Fetches a document the server renders. A failure is told to the user.
    public static void fetch(String path, final Got got) {
        String base = Session.serverUrl();
        if (base == null) {
            Ui.say("Not connected to a server");
            return;
        }
        ConnectionRequest request = new ConnectionRequest(base + path, false) {
            private byte[] data;

            @Override
            protected void readResponse(InputStream input) throws IOException {
                data = Util.readInputStream(input);
            }

            @Override
            protected void postResponse() {
                if (data == null || data.length == 0) {
                    Ui.say("The document is empty");
                    return;
                }
                got.got(data);
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                Ui.fail(code, code == 404 ? "That document does not exist"
                        : code == 403 ? "You may not see that document"
                        : "The document could not be fetched");
            }

            @Override
            protected void handleException(Exception err) {
                Log.e(err);
                Ui.say("The document could not be fetched");
            }
        };
        request.setFailSilently(true);
        request.setDuplicateSupported(true);
        request.addRequestHeader("Accept", "application/pdf");
        NetworkManager.getInstance().addToQueue(request);
    }

    /// Shows `pdf`: in a window of its own on a desktop, as a screen
    /// elsewhere.
    public static void preview(String title, final String fileName, final byte[] pdf) {
        final PdfViewer viewer;
        try {
            viewer = new PdfViewer(pdf);
        } catch (IOException bad) {
            Log.e(bad);
            Ui.say("The document could not be read");
            return;
        }
        viewer.setName("pdf-viewer");
        final String file = write(fileName, pdf);
        Container content = new Container(new BorderLayout());
        content.setName("pdf-preview");
        content.add(BorderLayout.CENTER, viewer);
        content.add(BorderLayout.SOUTH, actions(fileName, file));
        String heading = Lang.tr(title);
        Window window = Windows.open(heading, content, 150f, 190f);
        if (window != null) {
            return;
        }
        final Form previous = CN.getCurrentForm();
        Form form = Ui.form(heading, "pdf-form");
        if (previous != null) {
            Ui.back(form, previous);
        }
        form.add(BorderLayout.CENTER, content);
        Layouts.dress(form);
        form.show();
    }

    /// Print, Share and Save, and the file to drag out where a drag can leave
    /// the app.
    private static Container actions(final String fileName, final String file) {
        Container row = new Container(new FlowLayout(com.codename1.ui.Component.CENTER));
        row.setUIID("DocActions");
        if (file == null) {
            return row;
        }
        if (Printer.isPrintingSupported()) {
            Button print = Ui.secondary(Lang.tr("Print"), "pdf-print", e -> print(file));
            FontImage.setMaterialIcon(print, FontImage.MATERIAL_PRINT, 3.2f);
            row.add(print);
        }
        if (CN.isNativeShareSupported()) {
            Button share = Ui.secondary(Lang.tr("Share"), "pdf-share", e ->
                    Display.getInstance().share(null, file, "application/pdf"));
            FontImage.setMaterialIcon(share, FontImage.MATERIAL_SHARE, 3.2f);
            row.add(share);
        }
        Button save = Ui.secondary(Lang.tr("Save"), "pdf-save", e ->
                Ui.say(Lang.tr("Saved {0}", file)));
        FontImage.setMaterialIcon(save, FontImage.MATERIAL_SAVE, 3.2f);
        row.add(save);
        if (NativeDragAndDrop.isDragOutsideApplicationSupported()) {
            // The document itself, to drop on the desktop, into a mail or a
            // file manager: the drag offers the saved file.
            Label drag = new Label(fileName, "DocDrag");
            drag.setShouldLocalize(false);
            drag.setName("pdf-drag");
            FontImage.setMaterialIcon(drag, FontImage.MATERIAL_DRAG_INDICATOR, 3.2f);
            drag.setNativeDragOperation(NativeDragOperation.createFileDrag(new String[] {file}));
            row.add(drag);
        }
        return row;
    }

    /// Sends a saved PDF to the platform's print dialog.
    public static void print(String file) {
        if (!Printer.isPrintingSupported()) {
            Ui.say("Printing is not available here");
            return;
        }
        Printer.printPDF(file, result -> {
            if (result.getStatus() == com.codename1.printing.PrintResult.STATUS_FAILED) {
                Ui.say("Printing failed");
            }
        });
    }

    /// Writes `pdf` into the app's home as `fileName` and answers its path,
    /// or null when it could not be written.
    public static String write(String fileName, byte[] pdf) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath() + safe(fileName);
        OutputStream out = null;
        try {
            out = fs.openOutputStream(path);
            out.write(pdf);
        } catch (IOException failed) {
            Log.e(failed);
            Ui.say("The file could not be written");
            return null;
        } finally {
            Util.cleanup(out);
        }
        return path;
    }

    /// A file name with nothing in it but letters, digits, dots and dashes.
    static String safe(String name) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; name != null && iter < name.length(); iter++) {
            char c = name.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '-' || c == '_';
            out.append(ok ? c : '-');
        }
        return out.length() == 0 ? "document.pdf" : out.toString();
    }
}
