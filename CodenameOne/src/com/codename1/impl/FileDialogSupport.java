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

package com.codename1.impl;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.filechooser.FileDialogOption;
import com.codename1.io.filechooser.FileDialogResult;
import com.codename1.io.filechooser.FileTypeFilter;
import com.codename1.io.filechooser.OpenFileRequest;
import com.codename1.io.filechooser.SaveFileRequest;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.util.SuccessCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// The Codename One rendered Open and Save dialogs used where a port has no
/// native one, and the save-completion step every port shares.
final class FileDialogSupport {
    private FileDialogSupport() {
    }

    /// Answers an open request through `CodenameOneImplementation#openFileChooser`,
    /// which every port already implements, natively where it can.
    static void showOpen(CodenameOneImplementation impl, final OpenFileRequest request,
                         final SuccessCallback<FileDialogResult> callback) {
        impl.openFileChooser(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Object src = evt == null ? null : evt.getSource();
                // A port may answer openFileChooser off the EDT; the API promises the EDT.
                if (src instanceof String) {
                    deliver(new FileDialogResult(request, new String[]{(String) src}, -1, null), callback);
                } else {
                    deliver(FileDialogResult.cancelled(request), callback);
                }
            }
        }, request.toAcceptString());
    }

    /// Shows the fallback save form: a name field, the filter list and the
    /// request's options, saving into its initial directory or app home.
    static void showSave(final CodenameOneImplementation impl, final SaveFileRequest request,
                         final SuccessCallback<FileDialogResult> callback) {
        if (!Display.getInstance().isEdt()) {
            // The form is modal and must be built on the EDT, which also puts
            // the cancellation below on the EDT the API promises.
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    showSave(impl, request, callback);
                }
            });
            return;
        }
        final FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = request.getInitialDirectory();
        if (dir == null || !fs.isDirectory(dir)) {
            dir = fs.getAppHomePath();
        }
        final String directory = dir;
        final Dialog d = new Dialog(request.getTitle() == null ? "Save" : request.getTitle());
        d.setLayout(new BorderLayout());
        Container body = new Container(BoxLayout.y());
        if (request.getMessage() != null) {
            body.add(new Label(request.getMessage()));
        }
        String name = request.getSuggestedName();
        final TextField nameField = new TextField(name == null ? "" : name);
        body.add(new Label("Name")).add(nameField);
        final FileTypeFilter[] filters = request.getFilters();
        final ComboBox<String> filterBox = new ComboBox<String>();
        if (filters.length > 1) {
            for (FileTypeFilter f : filters) {
                filterBox.addItem(f.getLabel());
            }
            filterBox.setSelectedIndex(request.getSelectedFilter());
            body.add(new Label("Format")).add(filterBox);
        }
        final FileDialogOption[] options = request.getOptions();
        final Object[] editors = new Object[options.length];
        for (int i = 0; i < options.length; i++) {
            FileDialogOption o = options[i];
            if (o.getType() == FileDialogOption.TYPE_CHECKBOX) {
                CheckBox cb = new CheckBox(o.getLabel());
                cb.setSelected("true".equals(o.getDefaultValue()));
                editors[i] = cb;
                body.add(cb);
            } else if (o.getType() == FileDialogOption.TYPE_CHOICE) {
                ComboBox<String> box = new ComboBox<String>((Object[]) o.getChoices());
                box.setSelectedItem(o.getDefaultValue());
                editors[i] = box;
                body.add(new Label(o.getLabel())).add(box);
            } else {
                TextField tf = new TextField(o.getDefaultValue() == null ? "" : o.getDefaultValue());
                editors[i] = tf;
                body.add(new Label(o.getLabel())).add(tf);
            }
        }
        if (request.getAccessory() != null) {
            body.add(request.getAccessory());
        }
        d.add(BorderLayout.CENTER, body);
        Button cancel = new Button("Cancel");
        Button save = new Button(request.getAcceptLabel() == null ? "Save" : request.getAcceptLabel());
        d.add(BorderLayout.SOUTH, FlowLayout.encloseRight(cancel, save));
        final boolean[] done = new boolean[1];
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                d.dispose();
            }
        });
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String n = nameField.getText().trim();
                if (n.length() == 0 || n.indexOf('/') > -1 || n.indexOf('\\') > -1) {
                    return;
                }
                int filter = filters.length > 1 ? filterBox.getSelectedIndex() : request.getSelectedFilter();
                n = withExtension(n, filter < 0 ? null : filters[filter]);
                String path = directory.endsWith("/") ? directory + n : directory + "/" + n;
                if (request.isConfirmOverwrite() && fs.exists(path)
                        && !Dialog.show("Replace", n + " already exists. Replace it?", "Replace", "Cancel")) {
                    return;
                }
                Map<String, String> values = new HashMap<String, String>();
                for (int i = 0; i < options.length; i++) {
                    values.put(options[i].getId(), editorValue(editors[i]));
                }
                done[0] = true;
                d.dispose();
                completeSave(request, new FileDialogResult(request, new String[]{path}, filter, values), callback);
            }
        });
        d.showPacked(BorderLayout.CENTER, true);
        if (!done[0]) {
            callback.onSucess(FileDialogResult.cancelled(request));
        }
    }

    private static String editorValue(Object editor) {
        if (editor instanceof CheckBox) {
            return ((CheckBox) editor).isSelected() ? "true" : "false";
        }
        if (editor instanceof ComboBox) {
            Object o = ((ComboBox) editor).getSelectedItem();
            return o == null ? null : o.toString();
        }
        if (editor instanceof TextField) {
            return ((TextField) editor).getText();
        }
        return null;
    }

    /// Appends the filter's first extension unless the name already carries one of its extensions.
    static String withExtension(String name, FileTypeFilter filter) {
        if (filter == null || filter.acceptsName(name)) {
            return name;
        }
        String[] ext = filter.getExtensions();
        return ext.length == 0 ? name : name + "." + ext[0];
    }

    /// Writes the request's content to the result's `FileSystemStorage` path off
    /// the EDT, then delivers the result on the EDT.
    static void completeSave(final SaveFileRequest request, final FileDialogResult result,
                             final SuccessCallback<FileDialogResult> callback) {
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (request.getContent() == null
                            && isSameFile(request.getSourceFile(), result.getPath())) {
                        // A plain copy onto its own source: opening the destination
                        // would truncate the source before it is read, and the file
                        // already holds the content. A content writer takes
                        // precedence over the source, so it still runs below.
                        deliver(result, callback);
                        return;
                    }
                    OutputStream out = FileSystemStorage.getInstance().openOutputStream(result.getPath());
                    try {
                        request.writeContent(result, out);
                    } finally {
                        out.close();
                    }
                } catch (IOException err) {
                    Log.e(err);
                    result.setError(err);
                }
                deliver(result, callback);
            }
        });
    }

    private static void deliver(final FileDialogResult result, final SuccessCallback<FileDialogResult> callback) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                callback.onSucess(result);
            }
        });
    }

    /// True when the save's source file and destination name the same path,
    /// ignoring a `file://` prefix on either.
    static boolean isSameFile(String source, String destination) {
        if (source == null || destination == null) {
            return false;
        }
        return stripFileScheme(source).equals(stripFileScheme(destination));
    }

    private static String stripFileScheme(String path) {
        String p = path;
        if (p.startsWith("file://")) {
            p = p.substring(7);
        }
        while (p.startsWith("//")) {
            p = p.substring(1);
        }
        return p;
    }
}
