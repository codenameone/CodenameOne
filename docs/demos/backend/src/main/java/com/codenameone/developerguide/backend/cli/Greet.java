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

// tag::cli-command[]
import com.codename1.cli.CommandLine.Command;
import com.codename1.cli.CommandLine.Option;
import com.codename1.cli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "greet", mixinStandardHelpOptions = true, version = "greet 1.0",
        description = "Prints a greeting.")
public class Greet implements Callable<Integer> {
    @Option(names = {"-c", "--count"}, description = "How many times.", defaultValue = "1")
    private int count;

    @Option(names = {"-l", "--loud"}, description = "Shout it.")
    private boolean loud;

    @Parameters(paramLabel = "<name>", description = "Who to greet.")
    private String name;

    @Override
    public Integer call() {
        String line = loud ? "HELLO, " + name + "!" : "Hello, " + name;
        for (int i = 0; i < count; i++) {
            System.out.println(line);
        }
        return 0;
    }
}
// end::cli-command[]
