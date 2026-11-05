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
import java.util.List;

/// Renders a command's usage help, in picocli's layout.
final class Help {
    private static final int LABEL_COLUMN = 30;

    private Help() {
    }

    static String render(CommandSpec cmd, int width) {
        StringBuilder sb = new StringBuilder();
        List<OptionSpec> options = visibleOptions(cmd);
        StringBuilder synopsis = new StringBuilder("Usage: ").append(cmd.qualifiedName());
        StringBuilder flags = new StringBuilder();
        for (OptionSpec o : options) {
            String shortName = shortName(o);
            if (bare(o) && !o.required() && shortName != null) {
                insertSorted(flags, shortName.charAt(1));
            }
        }
        if (flags.length() > 0) {
            synopsis.append(" [-").append(flags).append(']');
        }
        for (OptionSpec o : options) {
            if (bare(o) && !o.required() && shortName(o) != null) {
                continue;
            }
            String one = bare(o) ? o.longestName() : o.longestName() + "=" + o.label();
            if (o.multiple()) {
                one = one + "...";
            }
            synopsis.append(' ').append(o.required() ? one : "[" + one + "]");
        }
        List<PositionalParamSpec> params = visiblePositionals(cmd);
        for (PositionalParamSpec p : params) {
            String one = p.label() + (p.multiple() ? "..." : "");
            boolean optional = !p.required() && (p.arityMin() == 0
                    || !p.multiple() && p.defaultValue() != null);
            synopsis.append(' ').append(optional ? "[" + one + "]" : one);
        }
        if (!cmd.subcommands().isEmpty()) {
            synopsis.append(" [COMMAND]");
        }
        wrap(sb, synopsis.toString(), 0, 7, width);
        String[] description = cmd.description();
        for (String line : description) {
            wrap(sb, line, 0, 0, width);
        }
        for (PositionalParamSpec p : params) {
            row(sb, "      " + p.label() + (p.multiple() ? "..." : ""),
                    withDefault(p.description(), p.defaultValue()), width);
        }
        for (OptionSpec o : options) {
            row(sb, optionLabel(o), withDefault(o.description(), o.defaultValue()), width);
        }
        List<CommandSpec> subs = cmd.subcommands();
        if (!subs.isEmpty()) {
            sb.append("Commands:\n");
            for (CommandSpec s : subs) {
                String[] d = s.description();
                row(sb, "  " + s.name(), d.length == 0 ? "" : d[0], width);
            }
        }
        return sb.toString();
    }

    /// Keeps the clustered flags of the synopsis in picocli's order: by letter,
    /// and the lower case one first where two differ only in case.
    private static void insertSorted(StringBuilder flags, char c) {
        int at = 0;
        while (at < flags.length() && order(flags.charAt(at)) <= order(c)) {
            at++;
        }
        flags.insert(at, c);
    }

    private static int order(char c) {
        boolean upper = c >= 'A' && c <= 'Z';
        char lower = upper ? (char) (c + 32) : c;
        return lower * 2 + (upper ? 1 : 0);
    }

    /// Whether an option is written without a value: a flag, unless its arity
    /// requires one (`arity = "1"` on a boolean).
    private static boolean bare(OptionSpec o) {
        return o.flag() && o.arityMin() == 0;
    }

    private static List<OptionSpec> visibleOptions(CommandSpec cmd) {
        List<OptionSpec> out = new ArrayList<OptionSpec>();
        List<OptionSpec> declared = cmd.options();
        for (OptionSpec d : declared) {
            if (!d.hidden()) {
                out.add(d);
            }
        }
        CommandLine.addStandardOptions(cmd, out);
        return out;
    }

    private static List<PositionalParamSpec> visiblePositionals(CommandSpec cmd) {
        List<PositionalParamSpec> out = new ArrayList<PositionalParamSpec>();
        List<PositionalParamSpec> declared = cmd.positionals();
        for (PositionalParamSpec d : declared) {
            if (!d.hidden()) {
                out.add(d);
            }
        }
        return out;
    }

    private static String shortName(OptionSpec o) {
        String[] names = o.names();
        for (String n : names) {
            if (n.length() == 2 && n.charAt(0) == '-') {
                return n;
            }
        }
        return null;
    }

    private static String optionLabel(OptionSpec o) {
        String shortName = shortName(o);
        StringBuilder sb = new StringBuilder();
        sb.append(shortName == null ? "      " : "  " + shortName + ", ");
        String[] names = o.names();
        boolean first = true;
        for (String n : names) {
            if (n.equals(shortName)) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            first = false;
            if (o.negatable() && n.startsWith("--")) {
                sb.append("--[no-]").append(n.substring(2));
            } else {
                sb.append(n);
            }
        }
        if (first && shortName != null) {
            sb.setLength(sb.length() - 2);
        }
        if (!bare(o)) {
            sb.append(first && shortName != null ? " " : "=").append(o.label());
        }
        return sb.toString();
    }

    private static String withDefault(String description, String defaultValue) {
        if (defaultValue == null) {
            return description;
        }
        String shown = CommandLine.resolveDefault(defaultValue);
        if (shown == null) {
            return description;
        }
        return description.length() == 0 ? "Default: " + shown
                : description + " Default: " + shown;
    }

    private static void row(StringBuilder sb, String label, String text, int width) {
        sb.append(label);
        if (text == null || text.length() == 0) {
            sb.append('\n');
            return;
        }
        if (label.length() + 2 > LABEL_COLUMN) {
            sb.append('\n');
            pad(sb, LABEL_COLUMN);
        } else {
            pad(sb, LABEL_COLUMN - label.length());
        }
        flow(sb, text, LABEL_COLUMN, LABEL_COLUMN, width);
    }

    private static void wrap(StringBuilder sb, String text, int firstIndent, int indent,
                             int width) {
        pad(sb, firstIndent);
        flow(sb, text, firstIndent, indent, width);
    }

    /// Appends `text`, wrapped at word boundaries to `width`, assuming the
    /// current line already holds `used` columns, and indents every
    /// continuation by `indent`.
    private static void flow(StringBuilder sb, String text, int used, int indent, int width) {
        int column = used;
        int limit = Math.max(width, indent + 20);
        String[] words = words(text);
        boolean lineStart = true;
        for (String w : words) {
            if (!lineStart && column + 1 + w.length() > limit) {
                sb.append('\n');
                pad(sb, indent);
                column = indent;
                lineStart = true;
            }
            if (!lineStart) {
                sb.append(' ');
                column++;
            }
            sb.append(w);
            column += w.length();
            lineStart = false;
        }
        sb.append('\n');
    }

    private static String[] words(String text) {
        List<String> out = new ArrayList<String>();
        int start = -1;
        for (int i = 0; i <= text.length(); i++) {
            boolean space = i == text.length() || text.charAt(i) == ' ';
            if (space) {
                if (start >= 0) {
                    out.add(text.substring(start, i));
                    start = -1;
                }
            } else if (start < 0) {
                start = i;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static void pad(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
    }
}
