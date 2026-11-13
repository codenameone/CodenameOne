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

/// The code the build generates for one `@Command` class, through which the
/// parser creates the command, sets its fields and runs it without reflection.
///
/// Applications do not implement this; the build writes one implementation per
/// command and fails if a command cannot be bound.
public interface CommandBinding {
    /// The model of the command, with its subcommands.
    CommandSpec spec();

    /// A new instance of the command.
    Object create() throws Exception;

    /// Converts the text of one value of the argument in `slot` to the type of
    /// its field. A failure is reported as an invalid value.
    Object convert(int slot, String text) throws Exception;

    /// Stores a converted value in the field of `slot`. For an array or a list
    /// field, `value` is a `java.util.List` of converted elements.
    void set(Object command, int slot, Object value) throws Exception;

    /// Hands the command the instance of its enclosing command, for a
    /// [CommandLine.ParentCommand] field, and its own model, for a
    /// [CommandLine.Spec] field.
    void inject(Object command, Object parent, CommandSpec spec) throws Exception;

    /// Whether the command has a body: it is a `Runnable` or a `Callable`.
    boolean runnable();

    /// Runs the command and answers its exit code.
    int call(Object command) throws Exception;
}
