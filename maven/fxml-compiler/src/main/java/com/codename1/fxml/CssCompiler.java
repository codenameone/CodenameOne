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
package com.codename1.fxml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.codename1.compat.jdk.ResourceNames;
import com.codename1.fxml.css.CssDeclaration;
import com.codename1.fxml.css.CssDeclarations;
import com.codename1.fxml.css.CssProperties;
import com.codename1.fxml.css.CssSelector;
import com.codename1.fxml.css.CssSheetData;
import com.codename1.fxml.css.CssValueParser;

/// Compiles one JavaFX style sheet into the table the device reads
/// ([CssSheetData]).
///
/// #### What is an error and what is a warning
///
/// A style sheet written for the desktop names properties this layer does
/// not have and values it cannot draw, and JavaFX itself only logs those.
/// So a declaration that cannot be used -- an unknown property, a value
/// that does not parse, a selector form that is not supported -- is a
/// WARNING with its file and line, and is left out of the table. A sheet
/// whose structure is broken -- a block that is never closed, a `}` with no
/// block, declarations with no selector -- is an ERROR: nothing after that
/// point can be trusted to mean what the author wrote.
///
/// #### At-rules
///
/// `@import` is resolved against the sheet's own path and recorded; the
/// device reads the imported table first. `@font-face` is recorded as a
/// family and the font file it names; the device loads the file when it
/// reads the sheet, and the family is one more name of that font. Any
/// other at-rule is skipped with a warning.
public final class CssCompiler {

    private final String file;
    private final String path;
    private final String text;
    private final Messages messages;
    private final Set<String> knownSheets;
    private final CssValueParser parser = new CssValueParser();

    private final List<String> imports = new ArrayList<String>();
    private final List<String> fontFamilies = new ArrayList<String>();
    private final List<String> fontSources = new ArrayList<String>();
    private final List<CssSelector> selectors = new ArrayList<CssSelector>();
    private final List<Integer> selectorBlocks = new ArrayList<Integer>();
    private final List<CssDeclaration[]> blocks = new ArrayList<CssDeclaration[]>();

    private CssCompiler(String file, String path, String text, Set<String> knownSheets, Messages messages) {
        this.file = file;
        this.path = path;
        // An address in a value -- the picture of a background -- is
        // written from where the sheet is.
        int directory = path == null ? -1 : path.lastIndexOf('/');
        parser.setBase(directory < 0 ? "" : path.substring(0, directory + 1));
        this.text = CssDeclarations.stripComments(text);
        this.knownSheets = knownSheets;
        this.messages = messages;
    }

    /// Compiles a style sheet. Answers `null`, with the reason recorded as
    /// an error, for a sheet whose structure is broken.
    ///
    /// #### Parameters
    ///
    /// - `file`: the file as messages name it
    ///
    /// - `path`: the resource path of the sheet, without a leading slash,
    ///   which its imports are relative to
    ///
    /// - `text`: the content of the sheet
    ///
    /// - `knownSheets`: the resource paths of every style sheet of the
    ///   application, to warn about an import of none of them; `null` to
    ///   skip that check
    ///
    /// - `messages`: where warnings and errors go
    public static CssSheetData compile(String file, String path, String text, Set<String> knownSheets,
            Messages messages) {
        CssCompiler c = new CssCompiler(file, path, text, knownSheets, messages);
        if (!c.parse()) {
            return null;
        }
        int[] blockOf = new int[c.selectorBlocks.size()];
        for (int i = 0; i < blockOf.length; i++) {
            blockOf[i] = c.selectorBlocks.get(i).intValue();
        }
        return new CssSheetData(c.imports.toArray(new String[0]), c.fontFamilies.toArray(new String[0]),
                c.fontSources.toArray(new String[0]), c.selectors.toArray(new CssSelector[0]), blockOf,
                c.blocks.toArray(new CssDeclaration[0][]));
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    /// The index of the first of `stops` at or after `from` that is outside
    /// a string and outside parentheses, or -1.
    private int scan(int from, String stops) {
        char quote = 0;
        int depth = 0;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth <= 0 && stops.indexOf(c) >= 0) {
                return i;
            }
        }
        return -1;
    }

    /// The `}` that closes the block opened at `open`, nested blocks
    /// skipped, or -1.
    private int closing(int open) {
        int depth = 0;
        int i = open;
        while (true) {
            int at = scan(i, "{}");
            if (at < 0) {
                return -1;
            }
            if (text.charAt(at) == '{') {
                depth++;
            } else if (--depth == 0) {
                return at;
            }
            i = at + 1;
        }
    }

    private boolean parse() {
        int i = 0;
        int n = text.length();
        while (i < n) {
            while (i < n && isSpace(text.charAt(i))) {
                i++;
            }
            if (i >= n) {
                break;
            }
            int start = i;
            if (text.charAt(i) == '}') {
                messages.error(file, text, i, "'}' without a block to close");
                return false;
            }
            int stop = scan(i, text.charAt(i) == '@' ? "{;" : "{}");
            if (stop < 0) {
                if (text.charAt(i) == '@') {
                    messages.error(file, text, start, "'" + head(start) + "' is not ended by ';' or a block");
                } else {
                    messages.error(file, text, start, "'" + head(start) + "' is not followed by a '{' block");
                }
                return false;
            }
            if (text.charAt(stop) == '}') {
                messages.error(file, text, stop, "'}' without a block to close, after '" + head(start) + "'");
                return false;
            }
            int end = stop;
            if (text.charAt(stop) == '{') {
                end = closing(stop);
                if (end < 0) {
                    messages.error(file, text, stop, "the block of '" + text.substring(start, stop).trim()
                            + "' is never closed");
                    return false;
                }
            }
            if (text.charAt(start) == '@') {
                atRule(start, stop, end);
            } else {
                rule(start, stop, end);
            }
            i = end + 1;
        }
        return true;
    }

    private String head(int start) {
        int end = start;
        while (end < text.length() && end - start < 40 && text.charAt(end) != '\n') {
            end++;
        }
        return text.substring(start, end).trim();
    }

    private void rule(int start, int open, int close) {
        String list = text.substring(start, open);
        if (list.trim().length() == 0) {
            messages.warning(file, text, start, "a block without a selector; it is ignored");
            return;
        }
        List<CssSelector> parsed = new ArrayList<CssSelector>();
        int from = 0;
        while (from <= list.length()) {
            int comma = list.indexOf(',', from);
            if (comma < 0) {
                comma = list.length();
            }
            String one = list.substring(from, comma).trim();
            CssSelector s = one.length() == 0 ? null : CssSelector.parse(one);
            if (s == null) {
                messages.warning(file, text, start + from, "the selector '" + one
                        + "' is not supported (type, .class, #id, :state, descendant and '>' are); it is ignored");
            } else {
                parsed.add(s);
            }
            from = comma + 1;
        }
        List<CssDeclaration> declarations = new ArrayList<CssDeclaration>();
        CssDeclarations.parse(text, open + 1, close, parser, new CssDeclarations.Reporter() {
            @Override
            public void warning(int offset, String message) {
                messages.warning(file, text, offset, message);
            }
        }, declarations);
        if (parsed.isEmpty() || declarations.isEmpty()) {
            return;
        }
        Integer block = Integer.valueOf(blocks.size());
        blocks.add(declarations.toArray(new CssDeclaration[0]));
        for (CssSelector s : parsed) {
            selectors.add(s);
            selectorBlocks.add(block);
        }
    }

    private void atRule(int start, int stop, int end) {
        int nameEnd = start + 1;
        while (nameEnd < stop && !isSpace(text.charAt(nameEnd))) {
            nameEnd++;
        }
        String name = CssProperties.lower(text.substring(start, nameEnd));
        if ("@import".equals(name) && text.charAt(stop) == ';') {
            String target = address(text.substring(nameEnd, stop).trim());
            String resolved = target == null ? null : resolve(target);
            if (resolved == null) {
                messages.warning(file, text, start, "'" + text.substring(start, stop).trim()
                        + "' does not name a style sheet of the application; it is ignored");
                return;
            }
            if (knownSheets != null && !knownSheets.contains(resolved)) {
                messages.warning(file, text, start, "the imported style sheet " + resolved
                        + " is not among the application's desktop resources; it will not be found");
            }
            imports.add(resolved);
            return;
        }
        if ("@font-face".equals(name) && text.charAt(stop) == '{') {
            fontFace(start, stop, end);
            return;
        }
        messages.warning(file, text, start, name + " is not supported; it is ignored");
    }

    private void fontFace(int start, int open, int close) {
        String family = null;
        String source = null;
        int i = open + 1;
        while (i < close) {
            int semi = scan(i, ";}");
            int stop = semi < 0 || semi > close ? close : semi;
            String declaration = text.substring(i, stop);
            int colon = declaration.indexOf(':');
            if (colon > 0) {
                String property = CssProperties.lower(declaration.substring(0, colon).trim());
                String value = declaration.substring(colon + 1).trim();
                if ("font-family".equals(property)) {
                    family = unquote(value);
                } else if ("src".equals(property)) {
                    String target = address(value);
                    source = target == null ? null : resolve(target);
                }
            }
            i = stop + 1;
        }
        if (family == null || source == null) {
            messages.warning(file, text, start,
                    "@font-face needs a font-family and a src that is a file of the application; it is ignored");
            return;
        }
        fontFamilies.add(family);
        fontSources.add(source);
    }

    private static String unquote(String s) {
        String t = s.trim();
        if (t.length() >= 2 && (t.charAt(0) == '"' || t.charAt(0) == '\'') && t.charAt(t.length() - 1) == t.charAt(0)) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    /// The address in `"x"`, `'x'`, `url(x)` or `url("x")`; the first one
    /// when a `src` lists several. `null` for anything else.
    private static String address(String value) {
        String v = value.trim();
        if (v.length() > 4 && "url(".equals(CssProperties.lower(v.substring(0, 4)))) {
            int close = v.indexOf(')');
            if (close < 0) {
                return null;
            }
            v = v.substring(4, close);
        } else {
            int end = v.indexOf(' ');
            if (end > 0 && v.charAt(0) != '"' && v.charAt(0) != '\'') {
                v = v.substring(0, end);
            }
        }
        v = unquote(v);
        return v.length() == 0 ? null : v;
    }

    /// The resource path an address in this sheet names, or `null` for one
    /// that is not a file of the application (an `http:` address).
    private String resolve(String address) {
        if (address.indexOf(':') >= 0) {
            return null;
        }
        if (address.startsWith("/")) {
            return ResourceNames.normalize(address);
        }
        int slash = path.lastIndexOf('/');
        return ResourceNames.normalize(slash < 0 ? address : path.substring(0, slash + 1) + address);
    }
}
