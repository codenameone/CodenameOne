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
import com.codename1.cli.CommandLine.ITypeConverter;
import com.codename1.cli.CommandLine.Option;

// tag::cli-converter[]
@Command(name = "listen", description = "Opens a port range.")
public class Ports implements Runnable {
    /// A first and last port.
    public static class Range {
        final int first;
        final int last;

        Range(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

    /// Turns "8000-8010" into a range; anything it throws becomes a usage error.
    public static class RangeConverter implements ITypeConverter<Range> {
        @Override
        public Range convert(String value) {
            int dash = value.indexOf('-');
            if (dash < 0) {
                int port = Integer.parseInt(value);
                return new Range(port, port);
            }
            return new Range(Integer.parseInt(value.substring(0, dash)),
                    Integer.parseInt(value.substring(dash + 1)));
        }
    }

    @Option(names = "--ports", converter = RangeConverter.class, required = true,
            description = "A port, or a range such as 8000-8010.")
    Range ports;

    @Override
    public void run() {
        System.out.println("ports " + ports.first + " to " + ports.last);
    }
}
// end::cli-converter[]
