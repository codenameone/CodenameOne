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
package com.codename1.retrace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone retrace CLI. Reads a crash trace on stdin and prints the structured
 * frames, so a developer can symbolicate a report without the cloud service.
 *
 * <p>This is the entry point wired as the shaded jar's {@code Main-Class}. Mapping
 * and synthetics de-obfuscation (via {@code MappingFile}/{@code SyntheticIndex})
 * are layered on as those pieces land in this module; today it parses and prints
 * the ParparVM trace domain, which is the format on-device crash reports carry on
 * the C targets.
 */
public final class RetraceMain {

    private RetraceMain() {
    }

    public static void main(String[] args) throws Exception {
        // Optional mappings: --mapping <file> may repeat (device-nearest first, e.g. R8 then
        // the cross-platform mapping). The trace is read from stdin.
        MappingChain chain = loadMappings(args);

        StringBuilder in = new StringBuilder();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(System.in, Charset.forName("UTF-8")));
        String line;
        while ((line = r.readLine()) != null) {
            in.append(line).append('\n');
        }
        List<Frame> frames = ParparVmTraceParser.parse(in.toString());
        if (frames.isEmpty()) {
            System.err.println("No ParparVM frames recognized in the input.");
            return;
        }
        for (Frame f : frames) {
            Frame out = chain.isEmpty() ? f : chain.retrace(f);
            System.out.println("    " + out);
        }
    }

    private static MappingChain loadMappings(String[] args) throws Exception {
        List<MappingFile> files = new ArrayList<MappingFile>();
        for (int i = 0; i < args.length - 1; i++) {
            if ("--mapping".equals(args[i])) {
                FileReader fr = new FileReader(new File(args[i + 1]));
                try {
                    files.add(MappingFile.parse(fr));
                } finally {
                    fr.close();
                }
            }
        }
        return new MappingChain(files);
    }
}
