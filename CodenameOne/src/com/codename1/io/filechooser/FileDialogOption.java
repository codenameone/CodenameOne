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

import java.util.ArrayList;
import java.util.List;

/// A declarative control added to a native file dialog: a checkbox, a choice
/// list or a text field.
///
/// This is the portable way to customize a dialog. Each desktop maps it to its
/// own widgets (an accessory view on macOS, `IFileDialogCustomize` on Windows,
/// `gtk_file_chooser_add_choice` on Linux, a Swing accessory in the simulator),
/// and the fallback chooser renders it with Codename One components. Mobile
/// pickers have a fixed UI and ignore options; check
/// `FileDialogFeatures#OPTIONS` before relying on them. The value the user
/// chose is read back from `FileDialogResult#getOptionValue(String)`.
public final class FileDialogOption {
    /// A checkbox. Its value is `"true"` or `"false"`.
    public static final int TYPE_CHECKBOX = 1;
    /// A choice list. Its value is the selected choice.
    public static final int TYPE_CHOICE = 2;
    /// A free text field.
    public static final int TYPE_TEXT = 3;

    private final String id;
    private final int type;
    private final String label;
    private final List<String> choices = new ArrayList<String>();
    private String defaultValue;

    private FileDialogOption(String id, int type, String label, String defaultValue) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.defaultValue = defaultValue;
    }

    /// Creates a checkbox option.
    ///
    /// #### Parameters
    ///
    /// - `id`: the key its value is reported under
    /// - `label`: the checkbox text
    /// - `checked`: the initial state
    ///
    /// #### Returns
    ///
    /// the option
    public static FileDialogOption checkbox(String id, String label, boolean checked) {
        return new FileDialogOption(id, TYPE_CHECKBOX, label, checked ? "true" : "false");
    }

    /// Creates a choice list option. The first choice is selected initially.
    ///
    /// #### Parameters
    ///
    /// - `id`: the key its value is reported under
    /// - `label`: the label shown beside the list
    /// - `choices`: the entries
    ///
    /// #### Returns
    ///
    /// the option
    public static FileDialogOption choice(String id, String label, String... choices) {
        FileDialogOption o = new FileDialogOption(id, TYPE_CHOICE, label, null);
        for (String c : choices) {
            o.choices.add(c);
        }
        if (!o.choices.isEmpty()) {
            o.defaultValue = o.choices.get(0);
        }
        return o;
    }

    /// Creates a text field option.
    ///
    /// #### Parameters
    ///
    /// - `id`: the key its value is reported under
    /// - `label`: the label shown beside the field
    /// - `value`: the initial text
    ///
    /// #### Returns
    ///
    /// the option
    public static FileDialogOption text(String id, String label, String value) {
        return new FileDialogOption(id, TYPE_TEXT, label, value);
    }

    /// Changes the initial value; for a choice it must be one of the choices.
    ///
    /// #### Parameters
    ///
    /// - `value`: the initial value
    ///
    /// #### Returns
    ///
    /// this option
    public FileDialogOption defaultValue(String value) {
        this.defaultValue = value;
        return this;
    }

    /// The key the value is reported under.
    ///
    /// #### Returns
    ///
    /// the id
    public String getId() {
        return id;
    }

    /// One of `#TYPE_CHECKBOX`, `#TYPE_CHOICE` or `#TYPE_TEXT`.
    ///
    /// #### Returns
    ///
    /// the type
    public int getType() {
        return type;
    }

    /// The text shown for the control.
    ///
    /// #### Returns
    ///
    /// the label
    public String getLabel() {
        return label;
    }

    /// The entries of a choice option, empty for other types.
    ///
    /// #### Returns
    ///
    /// a copy of the choices
    public String[] getChoices() {
        return choices.toArray(new String[choices.size()]);
    }

    /// The initial value.
    ///
    /// #### Returns
    ///
    /// the initial value
    public String getDefaultValue() {
        return defaultValue;
    }
}
