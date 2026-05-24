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
package com.codename1.ai;

import java.util.Map;

/// Trivial `{placeholder}` substitution. Designed for the common
/// "build a prompt from a handful of fields" pattern without pulling
/// in a templating library. For anything more sophisticated (loops,
/// conditionals) just compose strings directly.
///
/// ```
/// String prompt = PromptTemplate.of(
///     "You are an expert {role}. Reply in {style}."
/// ).put("role", "tax accountant").put("style", "bullet points").build();
/// ```
public final class PromptTemplate {
    private final String template;
    private final java.util.HashMap<String, String> values = new java.util.HashMap<String, String>();

    private PromptTemplate(String template) {
        if (template == null) {
            throw new IllegalArgumentException("template is required");
        }
        this.template = template;
    }

    public static PromptTemplate of(String template) {
        return new PromptTemplate(template);
    }

    public PromptTemplate put(String key, String value) {
        values.put(key, value == null ? "" : value);
        return this;
    }

    public PromptTemplate putAll(Map<String, String> map) {
        if (map != null) {
            values.putAll(map);
        }
        return this;
    }

    /// Renders the final string. Unknown placeholders are left
    /// intact (`{like_this}`) so they're easy to spot in test
    /// output — silently dropping them tends to hide bugs.
    public String build() {
        StringBuilder out = new StringBuilder(template.length() + 32);
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int end = template.indexOf('}', i + 1);
                if (end > i) {
                    String key = template.substring(i + 1, end);
                    String v = values.get(key);
                    if (v != null) {
                        out.append(v);
                        i = end + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /// Convenience: render and wrap as a [ChatMessage] with USER role.
    public ChatMessage asUser() {
        return ChatMessage.user(build());
    }

    /// Convenience: render and wrap as a [ChatMessage] with SYSTEM role.
    public ChatMessage asSystem() {
        return ChatMessage.system(build());
    }
}
