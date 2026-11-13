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
package com.codename1.cli;

/// What an option and a positional parameter have in common.
public abstract class ArgSpec {
    private final int slot;
    private final String description;
    private final String paramLabel;
    private final boolean flag;
    private final boolean required;
    private final int arityMin;
    private final int arityMax;
    private final boolean multiple;
    private final String defaultValue;
    private final String split;
    private final boolean hidden;

    ArgSpec(int slot, String description, String paramLabel, boolean flag, boolean required,
            String arity, boolean multiple, String defaultValue, String split, boolean hidden,
            int defaultArity, int defaultArityMax) {
        this.slot = slot;
        this.description = description == null ? "" : description;
        this.paramLabel = paramLabel;
        this.flag = flag;
        this.required = required;
        this.multiple = multiple;
        this.defaultValue = defaultValue == null || defaultValue.length() == 0
                ? null : defaultValue;
        this.split = split == null || split.length() == 0 ? null : split;
        this.hidden = hidden;
        if (arity == null || arity.length() == 0) {
            arityMin = defaultArity;
            arityMax = defaultArityMax;
        } else {
            int dots = arity.indexOf("..");
            if (dots < 0) {
                if ("*".equals(arity.trim())) {
                    arityMin = 0;
                    arityMax = Integer.MAX_VALUE;
                } else {
                    arityMin = Integer.parseInt(arity.trim());
                    arityMax = arityMin;
                }
            } else {
                arityMin = Integer.parseInt(arity.substring(0, dots).trim());
                String to = arity.substring(dots + 2).trim();
                arityMax = "*".equals(to) ? Integer.MAX_VALUE : Integer.parseInt(to);
            }
        }
    }

    /// The number the generated binding knows this argument by.
    public int slot() {
        return slot;
    }

    /// The help text.
    public String description() {
        return description;
    }

    /// The placeholder the help prints for the value, or null for the default.
    public String paramLabel() {
        return paramLabel;
    }

    /// Whether this is a boolean that takes no value.
    public boolean flag() {
        return flag;
    }

    /// Whether a command line without it is an error.
    public boolean required() {
        return required;
    }

    /// The fewest values one occurrence takes.
    public int arityMin() {
        return arityMin;
    }

    /// The most values one occurrence takes; `Integer.MAX_VALUE` when unbounded.
    public int arityMax() {
        return arityMax;
    }

    /// Whether the field holds several values: an array or a list.
    public boolean multiple() {
        return multiple;
    }

    /// The value used when the argument is absent, or null.
    public String defaultValue() {
        return defaultValue;
    }

    /// The separator that splits one value into several, or null.
    public String split() {
        return split;
    }

    /// Whether the help leaves it out.
    public boolean hidden() {
        return hidden;
    }

    abstract String label();
}
