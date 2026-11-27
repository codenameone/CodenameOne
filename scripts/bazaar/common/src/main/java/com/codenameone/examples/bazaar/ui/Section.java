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
package com.codenameone.examples.bazaar.ui;

import com.codename1.ui.Component;
import java.util.function.Supplier;

/// One place of a mode: an entry in the phone's tab bar and in the wide
/// screen's sidebar, and the content shown when it is picked.
///
/// The content is made by `content` each time the section is opened, so a
/// section that fetches from the server fetches again when it comes back into
/// view. Register one with [Sections#register(String, Section)].
public final class Section {
    private final String id;
    private final String title;
    private final String group;
    private final char icon;
    private final Supplier<Component> content;

    /// @param id stable id: the component name of its tab is `tab-` and this
    /// @param title what it is called, a key of the l10n bundles
    /// @param icon a `FontImage.MATERIAL_*` glyph
    /// @param content makes what the section shows
    public Section(String id, String title, char icon, Supplier<Component> content) {
        this(id, title, "", icon, content);
    }

    /// @param group the sidebar heading it is listed under on a wide screen;
    ///     empty for none
    public Section(String id, String title, String group, char icon,
            Supplier<Component> content) {
        this.id = id;
        this.title = title;
        this.group = group == null ? "" : group;
        this.icon = icon;
        this.content = content;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String group() {
        return group;
    }

    public char icon() {
        return icon;
    }

    /// The section's content, made afresh.
    public Component create() {
        return content.get();
    }

    /// The same place with other content: what a feature screen registers in
    /// place of a placeholder.
    public Section with(Supplier<Component> other) {
        return new Section(id, title, group, icon, other);
    }
}
