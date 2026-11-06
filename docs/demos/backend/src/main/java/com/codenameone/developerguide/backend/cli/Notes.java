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
package com.codenameone.developerguide.backend.cli;

import com.codename1.cli.CommandLine.Command;
import com.codename1.cli.CommandLine.Option;
import com.codename1.cli.CommandLine.Parameters;
import com.codename1.cli.CommandLine.ParentCommand;
import com.codename1.cli.CommandLine.Spec;
import com.codename1.cli.CommandSpec;

import java.util.List;

// tag::cli-group[]
@Command(name = "notes", mixinStandardHelpOptions = true, version = "notes 1.0",
        description = "Keeps notes in a file.",
        subcommands = {Notes.Add.class, Notes.Show.class})
public class Notes {
    @Option(names = {"-f", "--file"}, paramLabel = "<file>",
            description = "Where the notes live.", defaultValue = "${env:NOTES_FILE:-notes.txt}")
    String file;

    @Command(name = "add", description = "Adds a note.")
    public static class Add implements Runnable {
        @ParentCommand
        Notes parent;

        @Option(names = "--tag", split = ",", description = "Tags, comma separated.")
        List<String> tags;

        @Parameters(paramLabel = "<text>", description = "The words of the note.")
        List<String> words;

        @Override
        public void run() {
            System.out.println("adding to " + parent.file + ": " + String.join(" ", words));
        }
    }

    @Command(name = "show", aliases = "ls", description = "Lists the notes.")
    public static class Show implements Runnable {
        @Option(names = "--sort", description = "NEWEST or OLDEST.",
                defaultValue = "NEWEST")
        Order sort;

        @Spec
        CommandSpec spec;

        @Override
        public void run() {
            System.out.println(spec.qualifiedName() + " sorted " + sort);
        }
    }

    public enum Order { NEWEST, OLDEST }
}
// end::cli-group[]
