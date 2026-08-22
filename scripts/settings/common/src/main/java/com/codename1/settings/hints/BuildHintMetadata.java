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
package com.codename1.settings.hints;

import java.util.Collections;
import java.util.List;

public final class BuildHintMetadata {
    private final String name;
    private final String description;
    private final BuildHintType type;
    private final String platform;
    private final List<String> values;
    private final String defaultValue;
    private final String annotation;

    public BuildHintMetadata(String name, String description, BuildHintType type, String platform) {
        this(name, description, type, platform, null, null, null);
    }

    /**
     * @param values the closed value domain, or null when the hint is free-form
     * @param defaultValue the builder's own default, or null when it has none
     * @param annotation the annotation attribute that sets this hint, e.g.
     *                   {@code @Ios(pods)}, or null when it has none
     */
    public BuildHintMetadata(String name, String description, BuildHintType type, String platform,
                             List<String> values, String defaultValue, String annotation) {
        this.name = name;
        this.description = description == null ? "" : description.trim();
        this.type = type == null ? BuildHintType.TEXT : type;
        this.platform = platform == null ? "general" : platform;
        this.values = values == null || values.isEmpty()
                ? Collections.<String>emptyList() : Collections.unmodifiableList(values);
        this.defaultValue = defaultValue;
        this.annotation = annotation;
    }

    /** The accepted values, or empty when the hint is free-form. */
    public List<String> values() {
        return values;
    }

    /** The builder's own default, or null. */
    public String defaultValue() {
        return defaultValue;
    }

    /**
     * The annotation attribute that sets this hint, or null when the hint has no
     * checked form yet. Editing such a hint here is not wrong, but the annotation
     * is the form the compiler validates.
     */
    public String annotation() {
        return annotation;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public BuildHintType type() {
        return type;
    }

    public String platform() {
        return platform;
    }

    public boolean matches(String query) {
        if (query == null || query.trim().length() == 0) {
            return true;
        }
        String q = query.toLowerCase();
        return name.toLowerCase().contains(q)
                || description.toLowerCase().contains(q)
                || platform.toLowerCase().contains(q)
                || type.name().toLowerCase().contains(q);
    }
}
