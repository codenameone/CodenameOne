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

/// The model of one option, written by the build from an [CommandLine.Option].
public final class OptionSpec extends ArgSpec {
    private final String[] names;
    private final boolean negatable;
    private final boolean help;
    private final boolean versionHelp;

    /// Creates the model of an option.
    ///
    /// #### Parameters
    ///
    /// - `slot`: the number the generated binding knows this option by
    ///
    /// - `names`: every name, such as `-v` and `--verbose`
    ///
    /// - `description`: the help text
    ///
    /// - `paramLabel`: the placeholder printed for its value, such as `<file>`
    ///
    /// - `flag`: true for a boolean that takes no value
    ///
    /// - `required`: whether a command line without it is an error
    ///
    /// - `arity`: how many values one occurrence takes, as `1`, `0..1` or `1..*`;
    ///   empty for the default
    ///
    /// - `multiple`: true when the field is an array or a list
    ///
    /// - `defaultValue`: the value used when the option is absent, or null;
    ///   `${env:NAME}` reads an environment variable
    ///
    /// - `split`: a separator that splits one value into several, or null
    ///
    /// - `negatable`: whether `--no-<name>` sets a flag to false
    ///
    /// - `hidden`: whether it is left out of the help
    public OptionSpec(int slot, String[] names, String description, String paramLabel,
                      boolean flag, boolean required, String arity, boolean multiple,
                      String defaultValue, String split, boolean negatable, boolean hidden) {
        super(slot, description, paramLabel, flag, required, arity, multiple, defaultValue,
                split, hidden, flag ? 0 : 1, flag ? 0 : 1);
        this.names = names;
        this.negatable = negatable;
        this.help = false;
        this.versionHelp = false;
    }

    private OptionSpec(String[] names, String description, boolean versionHelp) {
        super(-1, description, null, true, false, "", false, null, null, false, 0, 0);
        this.names = names;
        this.negatable = false;
        this.help = !versionHelp;
        this.versionHelp = versionHelp;
    }

    static OptionSpec helpOption() {
        return new OptionSpec(new String[] {"-h", "--help"},
                "Show this help message and exit.", false);
    }

    static OptionSpec versionOption() {
        return new OptionSpec(new String[] {"-V", "--version"},
                "Print version information and exit.", true);
    }

    /// Every name of the option.
    public String[] names() {
        return names.clone();
    }

    /// The longest name, which the usage line and error messages print.
    public String longestName() {
        String best = names[0];
        for (int i = 1; i < names.length; i++) {
            if (names[i].length() > best.length()) {
                best = names[i];
            }
        }
        return best;
    }

    /// Whether `--no-<name>` sets the flag to false.
    public boolean negatable() {
        return negatable;
    }

    /// Whether this is the built-in `--help`.
    public boolean usageHelp() {
        return help;
    }

    /// Whether this is the built-in `--version`.
    public boolean versionHelp() {
        return versionHelp;
    }

    @Override
    String label() {
        String l = paramLabel();
        if (l != null && l.length() > 0) {
            return l;
        }
        String n = longestName();
        int i = 0;
        while (i < n.length() && n.charAt(i) == '-') {
            i++;
        }
        return "<" + n.substring(i) + ">";
    }
}
