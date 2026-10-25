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
package javafx.stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.codename1.fxcompat.runtime.FilePicker;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// Asks the user for a file.
///
/// Opening goes through the platform's own document picker, limited to
/// the extensions of the filters, and the call waits for the answer as
/// in JavaFX. Three things differ from a desktop:
///
/// - the picker answers one file, so [#showOpenMultipleDialog(Window)]
///   answers a list of one;
/// - a device has no dialog that names a new file. [#showSaveDialog(Window)]
///   answers the file of the initial name, or `untitled` with the first
///   extension of the selected filter, in the initial directory when one
///   was given and otherwise in the directory the application may write
///   to. It does not ask, and it never answers `null`;
/// - the title, and the initial directory when opening, are recorded
///   and not shown: the platform's picker decides where it starts.
public final class FileChooser {

    private final StringProperty title = new SimpleStringProperty(this, "title");
    private final ObjectProperty<File> initialDirectory = new SimpleObjectProperty<File>(this, "initialDirectory");
    private final ObjectProperty<String> initialFileName = new SimpleObjectProperty<String>(this,
            "initialFileName");
    private final ObjectProperty<ExtensionFilter> selectedExtensionFilter = new SimpleObjectProperty<ExtensionFilter>(
            this, "selectedExtensionFilter");
    private final ObservableList<ExtensionFilter> extensionFilters = FXCollections.observableArrayList();

    /// A named set of file name patterns, each `*.` and an extension;
    /// `*.*` and `*` stand for any file.
    public static final class ExtensionFilter {

        private final String description;
        private final List<String> extensions;

        /// Creates a filter from a description and its patterns.
        public ExtensionFilter(String description, String... extensions) {
            this(description, list(extensions));
        }

        /// Creates a filter from a description and a list of patterns.
        public ExtensionFilter(String description, List<String> extensions) {
            if (description == null) {
                throw new NullPointerException("Description must not be null");
            }
            if (description.length() == 0) {
                throw new IllegalArgumentException("Description must not be empty");
            }
            if (extensions == null) {
                throw new NullPointerException("Extensions must not be null");
            }
            if (extensions.isEmpty()) {
                throw new IllegalArgumentException("At least one extension must be defined");
            }
            ArrayList<String> copy = new ArrayList<String>();
            for (int i = 0; i < extensions.size(); i++) {
                String e = extensions.get(i);
                if (e == null) {
                    throw new NullPointerException("Extension must not be null");
                }
                if (e.length() == 0) {
                    throw new IllegalArgumentException("Extension must not be empty");
                }
                copy.add(e);
            }
            this.description = description;
            this.extensions = Collections.unmodifiableList(copy);
        }

        private static List<String> list(String[] extensions) {
            if (extensions == null) {
                throw new NullPointerException("Extensions must not be null");
            }
            ArrayList<String> out = new ArrayList<String>();
            for (int i = 0; i < extensions.length; i++) {
                out.add(extensions[i]);
            }
            return out;
        }

        /// Returns the description.
        public String getDescription() {
            return description;
        }

        /// Returns the patterns, which cannot be changed.
        public List<String> getExtensions() {
            return extensions;
        }
    }

    /// Creates a chooser with no filter.
    public FileChooser() {
    }

    /// Returns the title.
    public final String getTitle() {
        return title.get();
    }

    /// Sets the title.
    public final void setTitle(String value) {
        title.set(value);
    }

    /// The title of the dialog.
    public final StringProperty titleProperty() {
        return title;
    }

    /// Returns the directory to start in.
    public final File getInitialDirectory() {
        return initialDirectory.get();
    }

    /// Sets the directory to start in.
    public final void setInitialDirectory(File value) {
        initialDirectory.set(value);
    }

    /// The directory to start in.
    public final ObjectProperty<File> initialDirectoryProperty() {
        return initialDirectory;
    }

    /// Returns the name a file to save starts with.
    public final String getInitialFileName() {
        return initialFileName.get();
    }

    /// Sets the name a file to save starts with.
    public final void setInitialFileName(String value) {
        initialFileName.set(value);
    }

    /// The name a file to save starts with.
    public final ObjectProperty<String> initialFileNameProperty() {
        return initialFileName;
    }

    /// Returns the filters, to be added to.
    public ObservableList<ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    /// Returns the filter in use.
    public final ExtensionFilter getSelectedExtensionFilter() {
        return selectedExtensionFilter.get();
    }

    /// Sets the filter in use.
    public final void setSelectedExtensionFilter(ExtensionFilter value) {
        selectedExtensionFilter.set(value);
    }

    /// The filter in use.
    public final ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        return selectedExtensionFilter;
    }

    /// Asks for a file to open and returns it, or `null` when the user
    /// cancelled.
    public File showOpenDialog(Window ownerWindow) {
        String path = FilePicker.open(accept());
        return path == null ? null : new File(path);
    }

    /// Asks for files to open and returns them, or `null` when the user
    /// cancelled. The list holds the one file the platform's picker
    /// answers.
    public List<File> showOpenMultipleDialog(Window ownerWindow) {
        File one = showOpenDialog(ownerWindow);
        if (one == null) {
            return null;
        }
        ArrayList<File> out = new ArrayList<File>();
        out.add(one);
        return Collections.unmodifiableList(out);
    }

    /// Returns the file to save to; see the class for which that is.
    public File showSaveDialog(Window ownerWindow) {
        String name = getInitialFileName();
        if (name == null || name.length() == 0) {
            name = "untitled" + suffix();
        }
        File dir = getInitialDirectory();
        return dir != null ? new File(dir, name) : new File(FilePicker.home(), name);
    }

    /// The filter that applies: the selected one, or the first.
    private ExtensionFilter filter() {
        ExtensionFilter selected = getSelectedExtensionFilter();
        if (selected == null && !extensionFilters.isEmpty()) {
            selected = extensionFilters.get(0);
        }
        return selected;
    }

    /// The extension a file saved under the applying filter gets, with
    /// its dot, or nothing for a filter of any file.
    private String suffix() {
        ExtensionFilter f = filter();
        if (f == null) {
            return "";
        }
        String e = extension(f.getExtensions().get(0));
        return e == null ? "" : "." + e;
    }

    /// The extensions of every filter, separated by commas, as the
    /// platform's picker takes them; `null` when any filter takes any
    /// file, or there is no filter.
    private String accept() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < extensionFilters.size(); i++) {
            List<String> patterns = extensionFilters.get(i).getExtensions();
            for (int j = 0; j < patterns.size(); j++) {
                String e = extension(patterns.get(j));
                if (e == null) {
                    return null;
                }
                if (out.length() > 0) {
                    out.append(',');
                }
                out.append(e);
            }
        }
        return out.length() == 0 ? null : out.toString();
    }

    /// The extension a pattern names, or `null` for one that takes any
    /// file.
    private static String extension(String pattern) {
        int dot = pattern.lastIndexOf('.');
        String e = dot < 0 ? pattern : pattern.substring(dot + 1);
        return e.length() == 0 || e.indexOf('*') >= 0 ? null : e;
    }
}
