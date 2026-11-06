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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The model of one command: its name, help text, options, positional
/// parameters and subcommands.
///
/// The build writes one for every `@Command` class from its annotations, so an
/// application normally never constructs one. A command can receive its own
/// through a field annotated [CommandLine.Spec], to print its usage or read the
/// name it was invoked by.
public final class CommandSpec {
    private final String name;
    private final String[] description;
    private final String[] version;
    private final boolean standardHelpOptions;
    private final CommandBinding binding;
    private final List<OptionSpec> options = new ArrayList<OptionSpec>();
    private final List<PositionalParamSpec> positionals = new ArrayList<PositionalParamSpec>();
    private final List<CommandSpec> subcommands = new ArrayList<CommandSpec>();
    private String[] aliases = new String[0];
    private CommandSpec parent;

    /// Creates the model of a command.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name the command is invoked by
    ///
    /// - `description`: the lines of help text printed under the usage line
    ///
    /// - `version`: the lines `--version` prints, or an empty array
    ///
    /// - `standardHelpOptions`: whether `-h`/`--help` and `-V`/`--version` are added
    ///
    /// - `binding`: the generated code that creates and runs the command, or
    ///   null for a group that only holds subcommands
    public CommandSpec(String name, String[] description, String[] version,
                       boolean standardHelpOptions, CommandBinding binding) {
        this.name = name;
        this.description = description == null ? new String[0] : description;
        this.version = version == null ? new String[0] : version;
        this.standardHelpOptions = standardHelpOptions;
        this.binding = binding;
    }

    /// Adds an option, and answers this spec.
    public CommandSpec addOption(OptionSpec option) {
        options.add(option);
        return this;
    }

    /// Adds a positional parameter, and answers this spec.
    public CommandSpec addPositional(PositionalParamSpec positional) {
        positionals.add(positional);
        return this;
    }

    /// Adds a subcommand, and answers this spec.
    public CommandSpec addSubcommand(CommandSpec sub) {
        sub.parent = this;
        subcommands.add(sub);
        return this;
    }

    /// Sets the other names the command answers to, and answers this spec.
    public CommandSpec aliases(String... names) {
        aliases = names == null ? new String[0] : names;
        return this;
    }

    /// The name the command is invoked by.
    public String name() {
        return name;
    }

    /// The other names the command answers to.
    public String[] aliases() {
        return aliases.clone();
    }

    /// The name with the names of every enclosing command before it, as the
    /// usage line prints it: `git remote add`.
    public String qualifiedName() {
        return parent == null ? name : parent.qualifiedName() + " " + name;
    }

    /// The lines of help text.
    public String[] description() {
        return description.clone();
    }

    /// The lines `--version` prints. A subcommand with none inherits its parent's.
    public String[] version() {
        if (version.length == 0 && parent != null) {
            return parent.version();
        }
        return version.clone();
    }

    /// Whether `-h`/`--help` and `-V`/`--version` are recognized.
    public boolean standardHelpOptions() {
        return standardHelpOptions;
    }

    /// The options, in declaration order.
    public List<OptionSpec> options() {
        return Collections.unmodifiableList(options);
    }

    /// The positional parameters, in declaration order.
    public List<PositionalParamSpec> positionals() {
        return Collections.unmodifiableList(positionals);
    }

    /// The subcommands, in declaration order.
    public List<CommandSpec> subcommands() {
        return Collections.unmodifiableList(subcommands);
    }

    /// The enclosing command, or null for the top level.
    public CommandSpec parent() {
        return parent;
    }

    /// The generated code behind this command, or null for a group of
    /// subcommands with no body of its own.
    public CommandBinding binding() {
        return binding;
    }

    /// The subcommand invoked by `token`, by name or alias, or null.
    public CommandSpec subcommand(String token) {
        for (CommandSpec sub : subcommands) {
            if (sub.name.equals(token)) {
                return sub;
            }
            for (int j = 0; j < sub.aliases.length; j++) {
                if (sub.aliases[j].equals(token)) {
                    return sub;
                }
            }
        }
        return null;
    }
}
