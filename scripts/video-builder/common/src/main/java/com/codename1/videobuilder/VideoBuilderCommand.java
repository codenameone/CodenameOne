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
package com.codename1.videobuilder;

import java.nio.file.Path;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Process-wide command parsed by the JavaSE launcher before CN1 initialization. */
public record VideoBuilderCommand(String name, Path script, String orientation,
                                  Path output, boolean stdio, int port) {
    private static VideoBuilderCommand active = new VideoBuilderCommand("help", null, "landscape", Path.of("output"), false, 0);
    private static PrintStream resultOut = System.out;

    public static VideoBuilderCommand get() { return active; }
    public static void set(VideoBuilderCommand command) { active = command; }
    public static void setOutput(PrintStream stream) { resultOut = stream; }
    public static void emit(String value) { resultOut.println(value); resultOut.flush(); }
    public boolean isInteractive() { return "preview".equals(name) || "help".equals(name); }

    public static VideoBuilderCommand parse(String[] args) {
        if (args == null || args.length == 0) return active;
        String name = args[0];
        Path script = null;
        String orientation = "landscape";
        Path output = Path.of("output");
        boolean stdio = false;
        int port = 0;
        int index = 1;
        if (!"mcp".equals(name) && index < args.length && !args[index].startsWith("--")) script = Path.of(args[index++]);
        while (index < args.length) {
            String option = args[index++];
            if ("--orientation".equals(option)) orientation = required(args, index++ - 1);
            else if ("--output".equals(option)) output = Path.of(required(args, index++ - 1));
            else if ("--stdio".equals(option)) stdio = true;
            else if ("--port".equals(option)) port = Integer.parseInt(required(args, index++ - 1));
            else throw new IllegalArgumentException("Unknown option: " + option);
        }
        if (!(orientation.equals("landscape") || orientation.equals("portrait") || orientation.equals("both"))) {
            throw new IllegalArgumentException("orientation must be landscape, portrait, or both");
        }
        if ((name.equals("validate") || name.equals("prepare") || name.equals("preview")
                || name.equals("render") || name.equals("remux") || name.equals("frame"))
                && script == null) {
            throw new IllegalArgumentException(name + " requires a video.json path");
        }
        return new VideoBuilderCommand(name, script, orientation, output, stdio, port);
    }

    private static String required(String[] args, int optionIndex) {
        int valueIndex = optionIndex + 1;
        if (valueIndex >= args.length) throw new IllegalArgumentException("Missing value for " + args[optionIndex]);
        return args[valueIndex];
    }
}
