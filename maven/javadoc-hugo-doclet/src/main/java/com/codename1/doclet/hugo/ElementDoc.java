/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.doclet.hugo;

import java.util.ArrayList;
import java.util.List;

/** One element's documentation, after tags and markdown sections have been merged. */
final class ElementDoc {

    String description = "";
    final List<MarkdownSections.NamedText> parameters = new ArrayList<>();
    final List<MarkdownSections.NamedText> exceptions = new ArrayList<>();
    final List<String> seeAlso = new ArrayList<>();
    String returns;
    boolean deprecated;
    String deprecatedText = "";
    boolean hidden;

    boolean isEmpty() {
        return description.isBlank() && parameters.isEmpty() && exceptions.isEmpty()
                && seeAlso.isEmpty() && returns == null && !deprecated;
    }

    /** Adds a parameter unless one of that name is already documented. */
    void addParameter(MarkdownSections.NamedText parameter) {
        for (MarkdownSections.NamedText existing : parameters) {
            if (existing.name().equals(parameter.name())) {
                return;
            }
        }
        parameters.add(parameter);
    }

    /** Adds an exception unless one of that type is already documented. */
    void addException(MarkdownSections.NamedText exception) {
        for (MarkdownSections.NamedText existing : exceptions) {
            if (existing.name().equals(exception.name())) {
                return;
            }
        }
        exceptions.add(exception);
    }

    /** The documented text for a parameter, or null when it is undocumented. */
    String parameterText(String name) {
        for (MarkdownSections.NamedText parameter : parameters) {
            if (parameter.name().equals(name)) {
                return parameter.text();
            }
        }
        return null;
    }
}
