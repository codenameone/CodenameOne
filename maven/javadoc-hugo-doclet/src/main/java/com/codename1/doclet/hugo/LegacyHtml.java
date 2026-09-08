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
package com.codename1.doclet.hugo;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deals with the HTML left in comments that were converted to markdown.
 *
 * <p>Goldmark renders with {@code unsafe = false}, so raw HTML in a comment body
 * is dropped rather than rendered, and dropped silently: the build logs a
 * warning per page and the content simply is not there. 13 of the 2024 core
 * sources still carry some, and what disappeared included a whole {@code <ul>}
 * of tuning values in
 * {@code com.codename1.gaming.physics.box2d.common.Settings}.
 *
 * <p>Turning goldmark's {@code unsafe} on is not the answer, and not only
 * because it would change how 45 pages of existing blog content render. Half of
 * this HTML is not markup at all -- {@code com.codename1.util.regex.RE}
 * documents a substitution string as {@code <a href="$0">$0</a>} and
 * {@code PushBuilder} writes {@code <metadata>;<body>} to mean one value
 * followed by another. Rendering those is how the standard pages lose them:
 * the browser eats {@code <metadata>} as an unknown element and shows a bare
 * semicolon.
 *
 * <p>So the two cases are separated. A structural tag carrying no attributes
 * becomes its markdown equivalent, which covers 129 of the 140 tags in the API
 * and renders them properly. Everything else is escaped, which makes it visible
 * exactly as written -- better than both the old pipeline, which dropped it, and
 * the standard doclet, which mangles it.
 */
final class LegacyHtml {

    /** Tags that are real markup here, all of which appear without attributes. */
    private static final Map<String, String> BLOCK = Map.of(
            "p", "\n\n",
            "ul", "\n\n",
            "ol", "\n\n",
            "pre", "\n\n",
            "br", "\n",
            "li", "\n- ");

    private static final Map<String, String> INLINE = Map.of(
            "b", "**",
            "strong", "**",
            "i", "*",
            "em", "*");

    private static final Set<String> HEADINGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

    private LegacyHtml() {
    }

    /**
     * Converts the structural HTML in a markdown body and escapes the rest.
     *
     * <p>Fenced blocks and code spans are copied through untouched: HTML inside
     * one is an example being shown, and goldmark already keeps it literal.
     */
    static String convert(String markdown) {
        if (markdown == null || markdown.indexOf('<') < 0) {
            return markdown;
        }

        StringBuilder out = new StringBuilder(markdown.length() + 32);
        int i = 0;
        int n = markdown.length();
        while (i < n) {
            char c = markdown.charAt(i);

            int fence = fenceLength(markdown, i);
            if (fence > 0) {
                int end = markdown.indexOf(markdown.substring(i, i + fence), i + fence);
                int stop = end < 0 ? n : end + fence;
                out.append(markdown, i, stop);
                i = stop;
                continue;
            }
            if (c == '`') {
                int ticks = 0;
                while (i + ticks < n && markdown.charAt(i + ticks) == '`') {
                    ticks++;
                }
                String delimiter = "`".repeat(ticks);
                int end = markdown.indexOf(delimiter, i + ticks);
                int stop = end < 0 ? n : end + ticks;
                out.append(markdown, i, stop);
                i = stop;
                continue;
            }
            if (c != '<') {
                out.append(c);
                i++;
                continue;
            }

            // A bare "<" is arithmetic, not markup: "a < b" must stay as written.
            if (!opensATag(markdown, i)) {
                out.append(c);
                i++;
                continue;
            }
            int close = markdown.indexOf('>', i);
            if (close < 0) {
                out.append(c);
                i++;
                continue;
            }
            String raw = markdown.substring(i, close + 1);
            String replacement = replacementFor(raw);
            if (replacement == null) {
                // Not structural markup: show it as the author wrote it.
                out.append("&lt;").append(raw, 1, raw.length());
            } else {
                out.append(replacement);
            }
            i = close + 1;
        }
        return out.toString();
    }

    /** Whether the "<" at this index begins something tag shaped. */
    private static boolean opensATag(String text, int index) {
        int next = index + 1;
        if (next < text.length() && text.charAt(next) == '/') {
            next++;
        }
        return next < text.length() && Character.isLetter(text.charAt(next));
    }

    /** The markdown for a tag, or null when it should be escaped instead. */
    private static String replacementFor(String raw) {
        String inner = raw.substring(1, raw.length() - 1).trim();
        if (inner.endsWith("/")) {
            inner = inner.substring(0, inner.length() - 1).trim();
        }
        boolean closing = inner.startsWith("/");
        if (closing) {
            inner = inner.substring(1).trim();
        }
        // An attribute means the tag is doing something this cannot reproduce --
        // <a href="$0"> is the documented example, not a link to follow.
        if (inner.isEmpty() || inner.chars().anyMatch(Character::isWhitespace)) {
            return null;
        }
        String name = inner.toLowerCase(Locale.ROOT);

        if (HEADINGS.contains(name)) {
            // Levelled down deliberately: a heading inside a comment must not
            // outrank the page structure the templates put around it.
            return closing ? "\n" : "\n\n#### ";
        }
        String inline = INLINE.get(name);
        if (inline != null) {
            return inline;
        }
        String block = BLOCK.get(name);
        if (block == null) {
            return null;
        }
        if (closing) {
            // </br> is not valid HTML but is written anyway, and it means the same
            // single break as <br>. </li> closes an item that the next "- " opens.
            if ("br".equals(name)) {
                return "\n";
            }
            return "li".equals(name) ? "" : "\n\n";
        }
        return block;
    }

    /** The length of a code fence opening at this position, or 0. */
    private static int fenceLength(String text, int index) {
        if (index > 0 && text.charAt(index - 1) != '\n') {
            return 0;
        }
        char c = text.charAt(index);
        if (c != '`' && c != '~') {
            return 0;
        }
        int run = 0;
        while (index + run < text.length() && text.charAt(index + run) == c) {
            run++;
        }
        return run >= 3 ? run : 0;
    }
}
