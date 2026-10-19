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

import java.util.HashMap;
import java.util.Map;

/// The outcome of a file dialog, delivered to the callback on the EDT.
public final class FileDialogResult {
    private final String[] paths;
    private final int selectedFilter;
    private final FileTypeFilter filter;
    private final Map<String, String> optionValues;
    private Throwable error;

    /// Creates a result; ports and the fallback dialogs build these.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request being answered
    /// - `paths`: the chosen paths, or null/empty when cancelled
    /// - `selectedFilter`: the filter the user ended on, or -1
    /// - `optionValues`: option id to value, or null for the defaults
    public FileDialogResult(FileDialogRequest<?> request, String[] paths, int selectedFilter,
                            Map<String, String> optionValues) {
        this.paths = paths == null ? new String[0] : paths;
        FileTypeFilter[] filters = request.getFilters();
        if (selectedFilter < 0 || selectedFilter >= filters.length) {
            selectedFilter = request.getSelectedFilter();
        }
        this.selectedFilter = selectedFilter;
        this.filter = selectedFilter < 0 ? null : filters[selectedFilter];
        Map<String, String> values = new HashMap<String, String>();
        for (FileDialogOption o : request.getOptions()) {
            values.put(o.getId(), o.getDefaultValue());
        }
        if (optionValues != null) {
            values.putAll(optionValues);
        }
        this.optionValues = values;
    }

    /// Creates the result of a dialog the user dismissed.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request being answered
    ///
    /// #### Returns
    ///
    /// a cancelled result
    public static FileDialogResult cancelled(FileDialogRequest<?> request) {
        return new FileDialogResult(request, null, -1, null);
    }

    /// True when the user dismissed the dialog, or a save failed before a file existed.
    ///
    /// #### Returns
    ///
    /// whether nothing was chosen
    public boolean isCancelled() {
        return paths.length == 0;
    }

    /// The chosen files. For a save, the one file written.
    ///
    /// #### Returns
    ///
    /// a copy of the paths; empty when cancelled
    public String[] getPaths() {
        String[] out = new String[paths.length];
        System.arraycopy(paths, 0, out, 0, paths.length);
        return out;
    }

    /// The first chosen file.
    ///
    /// #### Returns
    ///
    /// the path, or null when cancelled
    public String getPath() {
        return paths.length == 0 ? null : paths[0];
    }

    /// The index of the filter the user ended on.
    ///
    /// #### Returns
    ///
    /// the index, or -1 without filters
    public int getSelectedFilterIndex() {
        return selectedFilter;
    }

    /// The filter the user ended on; for a save this is the chosen format.
    ///
    /// #### Returns
    ///
    /// the filter, or null without filters
    public FileTypeFilter getSelectedFilter() {
        return filter;
    }

    /// The value of a `FileDialogOption`, or its default where the platform
    /// shows no options.
    ///
    /// #### Parameters
    ///
    /// - `id`: the option id
    ///
    /// #### Returns
    ///
    /// the value
    public String getOptionValue(String id) {
        return optionValues.get(id);
    }

    /// The failure of a save, or null. A failed save still reports the path it
    /// tried, so the partial file can be cleaned up.
    ///
    /// #### Returns
    ///
    /// the error
    public Throwable getError() {
        return error;
    }

    /// Records a save failure; used by ports.
    ///
    /// #### Parameters
    ///
    /// - `error`: the failure
    public void setError(Throwable error) {
        this.error = error;
    }
}
