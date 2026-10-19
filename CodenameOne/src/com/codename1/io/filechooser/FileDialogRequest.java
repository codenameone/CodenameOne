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

package com.codename1.io.filechooser;

import com.codename1.ui.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The settings shared by `OpenFileRequest` and `SaveFileRequest`.
///
/// Every setter returns the request, so a dialog is described in one chain.
/// Settings a platform cannot honour are ignored rather than failing, and
/// `com.codename1.ui.Display#isFileDialogFeatureSupported(int)` says up front
/// which ones apply. Platform specific tuning goes through `#hint(String, Object)`.
///
/// #### Parameters
///
/// - `<T>`: the concrete request type, so chained setters keep it
public abstract class FileDialogRequest<T extends FileDialogRequest<T>> {
    private String title;
    private String message;
    private String acceptLabel;
    private String initialDirectory;
    private final List<FileTypeFilter> filters = new ArrayList<FileTypeFilter>();
    private int selectedFilter;
    private final Map<String, Object> hints = new HashMap<String, Object>();
    private final List<FileDialogOption> options = new ArrayList<FileDialogOption>();
    private Component accessory;

    FileDialogRequest() {
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    /// Sets the window title. Ignored by the mobile pickers.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title
    ///
    /// #### Returns
    ///
    /// this request
    public T title(String title) {
        this.title = title;
        return self();
    }

    /// Sets an explanatory line shown in the dialog (`NSSavePanel.message` on
    /// macOS, the fallback dialog's header elsewhere).
    ///
    /// #### Parameters
    ///
    /// - `message`: the text
    ///
    /// #### Returns
    ///
    /// this request
    public T message(String message) {
        this.message = message;
        return self();
    }

    /// Replaces the text of the Open or Save button on desktops.
    ///
    /// #### Parameters
    ///
    /// - `label`: the button text
    ///
    /// #### Returns
    ///
    /// this request
    public T acceptLabel(String label) {
        this.acceptLabel = label;
        return self();
    }

    /// Sets the folder the dialog starts in, as a `FileSystemStorage` path.
    ///
    /// #### Parameters
    ///
    /// - `path`: the folder
    ///
    /// #### Returns
    ///
    /// this request
    public T initialDirectory(String path) {
        this.initialDirectory = path;
        return self();
    }

    /// Adds a file type filter. The first one added is selected unless
    /// `#selectedFilter(int)` says otherwise.
    ///
    /// #### Parameters
    ///
    /// - `filter`: the filter
    ///
    /// #### Returns
    ///
    /// this request
    public T addFilter(FileTypeFilter filter) {
        filters.add(filter);
        return self();
    }

    /// Selects the filter shown initially.
    ///
    /// #### Parameters
    ///
    /// - `index`: an index into the filters added so far
    ///
    /// #### Returns
    ///
    /// this request
    public T selectedFilter(int index) {
        this.selectedFilter = index;
        return self();
    }

    /// Sets a platform specific hint; see `FileDialogHints`. Unknown hints are
    /// ignored.
    ///
    /// #### Parameters
    ///
    /// - `key`: the hint name
    /// - `value`: its value
    ///
    /// #### Returns
    ///
    /// this request
    public T hint(String key, Object value) {
        hints.put(key, value);
        return self();
    }

    /// Adds a portable control to the dialog; see `FileDialogOption`.
    ///
    /// #### Parameters
    ///
    /// - `option`: the control
    ///
    /// #### Returns
    ///
    /// this request
    public T addOption(FileDialogOption option) {
        options.add(option);
        return self();
    }

    /// Embeds a Codename One component in the dialog, such as a format and
    /// quality panel for an export. Only shown where
    /// `FileDialogFeatures#ACCESSORY_COMPONENT` is supported (macOS and the
    /// desktop/simulator port); use `#addOption(FileDialogOption)` for
    /// controls that must appear on every desktop. The component is still
    /// reachable from the callback, so its state can be read after the dialog
    /// closes.
    ///
    /// #### Parameters
    ///
    /// - `accessory`: the component
    ///
    /// #### Returns
    ///
    /// this request
    public T accessory(Component accessory) {
        this.accessory = accessory;
        return self();
    }

    /// The window title, or null.
    ///
    /// #### Returns
    ///
    /// the title
    public String getTitle() {
        return title;
    }

    /// The explanatory message, or null.
    ///
    /// #### Returns
    ///
    /// the message
    public String getMessage() {
        return message;
    }

    /// The accept button text, or null for the platform default.
    ///
    /// #### Returns
    ///
    /// the label
    public String getAcceptLabel() {
        return acceptLabel;
    }

    /// The starting folder, or null.
    ///
    /// #### Returns
    ///
    /// the folder path
    public String getInitialDirectory() {
        return initialDirectory;
    }

    /// The filters in the order they were added.
    ///
    /// #### Returns
    ///
    /// a copy of the filters
    public FileTypeFilter[] getFilters() {
        return filters.toArray(new FileTypeFilter[filters.size()]);
    }

    /// The initially selected filter index, clamped to the filters present.
    ///
    /// #### Returns
    ///
    /// the index, or -1 when there are no filters
    public int getSelectedFilter() {
        if (filters.isEmpty()) {
            return -1;
        }
        if (selectedFilter < 0 || selectedFilter >= filters.size()) {
            return 0;
        }
        return selectedFilter;
    }

    /// Reads a hint.
    ///
    /// #### Parameters
    ///
    /// - `key`: the hint name
    ///
    /// #### Returns
    ///
    /// the value, or null
    public Object getHint(String key) {
        return hints.get(key);
    }

    /// Reads a boolean hint.
    ///
    /// #### Parameters
    ///
    /// - `key`: the hint name
    /// - `def`: the value when the hint is absent or not a boolean
    ///
    /// #### Returns
    ///
    /// the value
    public boolean getBooleanHint(String key, boolean def) {
        Object o = hints.get(key);
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        if (o instanceof String) {
            return "true".equalsIgnoreCase((String) o);
        }
        return def;
    }

    /// Reads a string hint.
    ///
    /// #### Parameters
    ///
    /// - `key`: the hint name
    ///
    /// #### Returns
    ///
    /// the value as a string, or null
    public String getStringHint(String key) {
        Object o = hints.get(key);
        return o == null ? null : o.toString();
    }

    /// The portable controls in the order they were added.
    ///
    /// #### Returns
    ///
    /// a copy of the options
    public FileDialogOption[] getOptions() {
        return options.toArray(new FileDialogOption[options.size()]);
    }

    /// The accessory component, or null.
    ///
    /// #### Returns
    ///
    /// the accessory
    public Component getAccessory() {
        return accessory;
    }

    /// Joins every filter's extensions and MIME types into the comma separated
    /// form `com.codename1.ui.Display#openFileChooser` takes.
    ///
    /// #### Returns
    ///
    /// the accept string, or null when nothing is filtered
    public String toAcceptString() {
        StringBuilder sb = new StringBuilder();
        for (FileTypeFilter f : filters) {
            for (String e : f.getExtensions()) {
                append(sb, e);
            }
            for (String m : f.getMimeTypes()) {
                append(sb, m);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void append(StringBuilder sb, String s) {
        if (sb.length() > 0) {
            sb.append(',');
        }
        sb.append(s);
    }
}
