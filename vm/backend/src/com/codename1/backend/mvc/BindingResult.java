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
package com.codename1.backend.mvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Conversion errors, submitted values, and application validation for one form.
public final class BindingResult {
    private final Map<String, String> values = new LinkedHashMap<String, String>();
    private final Map<String, List<String>> errors = new LinkedHashMap<String, List<String>>();

    public void submitted(String field, String value) {
        values.put(field, value);
    }

    public Object fieldValue(String field, Object fallback) {
        return values.containsKey(field) ? values.get(field) : fallback;
    }

    public void reject(String message) {
        rejectValue("", message);
    }

    public void rejectValue(String field, String message) {
        List<String> list = errors.get(field);
        if (list == null) {
            list = new ArrayList<String>();
            errors.put(field, list);
        }
        list.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getFieldErrors(String field) {
        List<String> list = errors.get(field);
        return list == null ? Collections.<String>emptyList() : Collections.unmodifiableList(list);
    }

    public String messages(String field) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
            if (!"*".equals(field) && !entry.getKey().equals(field)) {
                continue;
            }
            for (String message : entry.getValue()) {
                if (text.length() > 0) {
                    text.append("; ");
                }
                text.append(message);
            }
        }
        return text.toString();
    }
}
