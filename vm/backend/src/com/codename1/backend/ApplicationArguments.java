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
package com.codename1.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The program's arguments, as Spring Boot parses them: `--name=value` and
/// `--name` are options, which may repeat, and everything else is a
/// non-option argument. A lone `--` is an argument, not an option.
public final class ApplicationArguments {
    private final String[] source;
    private final Map<String, List<String>> options = new LinkedHashMap<String, List<String>>();
    private final List<String> nonOptions = new ArrayList<String>();

    /// Parses `args`.
    /// @param args the arguments; null is taken as none
    public ApplicationArguments(String... args) {
        source = args == null ? new String[0] : copy(args);
        for (String arg : source) {
            if (arg != null && arg.startsWith("--") && arg.length() > 2) {
                int eq = arg.indexOf('=');
                String name = eq < 0 ? arg.substring(2) : arg.substring(2, eq);
                List<String> values = options.get(name);
                if (values == null) {
                    values = new ArrayList<String>();
                    options.put(name, values);
                }
                if (eq >= 0) {
                    values.add(arg.substring(eq + 1));
                }
            } else {
                nonOptions.add(arg);
            }
        }
    }

    private static String[] copy(String[] args) {
        String[] out = new String[args.length];
        System.arraycopy(args, 0, out, 0, args.length);
        return out;
    }

    /// The arguments as given.
    public String[] getSourceArgs() {
        return copy(source);
    }

    /// The names of the options, in the order they first appear.
    public Set<String> getOptionNames() {
        return Collections.unmodifiableSet(options.keySet());
    }

    /// Whether `--name` was given, with or without a value.
    public boolean containsOption(String name) {
        return options.containsKey(name);
    }

    /// The values given to `--name`, empty for an option without one, or null
    /// when it was not given at all.
    public List<String> getOptionValues(String name) {
        List<String> values = options.get(name);
        return values == null ? null : Collections.unmodifiableList(values);
    }

    /// The arguments that are not options, in order.
    public List<String> getNonOptionArgs() {
        return Collections.unmodifiableList(nonOptions);
    }
}
