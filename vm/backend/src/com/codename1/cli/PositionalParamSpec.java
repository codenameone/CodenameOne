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

/// The model of one positional parameter, written by the build from a
/// [CommandLine.Parameters].
public final class PositionalParamSpec extends ArgSpec {
    private final int indexFrom;
    private final int indexTo;

    /// Creates the model of a positional parameter.
    ///
    /// #### Parameters
    ///
    /// - `slot`: the number the generated binding knows this parameter by
    ///
    /// - `index`: the positions it takes, as `0`, `1..3` or `1..*`
    ///
    /// - `description`: the help text
    ///
    /// - `paramLabel`: the placeholder printed for it, such as `<file>`
    ///
    /// - `required`: whether a command line without it is an error
    ///
    /// - `arity`: how many values it takes, or empty for the default
    ///
    /// - `multiple`: true when the field is an array or a list
    ///
    /// - `defaultValue`: the value used when it is absent, or null
    ///
    /// - `split`: a separator that splits one value into several, or null
    ///
    /// - `hidden`: whether it is left out of the help
    public PositionalParamSpec(int slot, String index, String description, String paramLabel,
                               boolean required, String arity, boolean multiple,
                               String defaultValue, String split, boolean hidden) {
        super(slot, description, paramLabel, false, required, arity, multiple, defaultValue,
                split, hidden, multiple ? 0 : 1, multiple ? Integer.MAX_VALUE : 1);
        int[] range = range(index, multiple);
        this.indexFrom = range[0];
        this.indexTo = range[1];
    }

    /// The first position this parameter takes.
    public int indexFrom() {
        return indexFrom;
    }

    /// The last position this parameter takes, or `Integer.MAX_VALUE` for an
    /// open range.
    public int indexTo() {
        return indexTo;
    }

    @Override
    String label() {
        String l = paramLabel();
        return l != null && l.length() > 0 ? l : "<param" + slot() + ">";
    }

    static int[] range(String index, boolean multiple) {
        if (index == null || index.length() == 0) {
            return new int[] {0, multiple ? Integer.MAX_VALUE : 0};
        }
        int dots = index.indexOf("..");
        if (dots < 0) {
            int at = Integer.parseInt(index.trim());
            return new int[] {at, at};
        }
        int from = Integer.parseInt(index.substring(0, dots).trim());
        String to = index.substring(dots + 2).trim();
        return new int[] {from, "*".equals(to) ? Integer.MAX_VALUE : Integer.parseInt(to)};
    }
}
