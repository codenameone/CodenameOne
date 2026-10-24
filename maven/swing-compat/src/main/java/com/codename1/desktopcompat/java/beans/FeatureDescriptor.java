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
package com.codename1.desktopcompat.java.beans;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/// What every bean descriptor has in common: a name, a display name, a short
/// description, and a table of named values a tool or a library may attach.
public class FeatureDescriptor {

    private String name;
    private String displayName;
    private String shortDescription;
    private boolean expert;
    private boolean hidden;
    private boolean preferred;
    private final Map<String, Object> values = new HashMap<String, Object>();

    public FeatureDescriptor() {
        // Named by its setters.
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName == null ? getName() : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getShortDescription() {
        return shortDescription == null ? getDisplayName() : shortDescription;
    }

    public void setShortDescription(String text) {
        this.shortDescription = text;
    }

    public boolean isExpert() {
        return expert;
    }

    public void setExpert(boolean expert) {
        this.expert = expert;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    public void setValue(String attributeName, Object value) {
        if (attributeName == null || value == null) {
            throw new NullPointerException();
        }
        values.put(attributeName, value);
    }

    public Object getValue(String attributeName) {
        return attributeName == null ? null : values.get(attributeName);
    }

    public Enumeration<String> attributeNames() {
        return Collections.enumeration(values.keySet());
    }
}
