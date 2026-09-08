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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Recovers the structure that the framework's documentation comments express as
 * markdown prose rather than as javadoc block tags.
 *
 * <p>Almost none of this codebase uses {@code @param} and {@code @return}. The
 * overwhelming majority writes a {@code #### Parameters} heading followed by a
 * bullet list, and a {@code #### Returns} heading followed by a paragraph --
 * measured over CodenameOne/src and Ports/CLDC11/src, 9068 occurrences of the
 * heading against 816 of the tag, and 7224 against 1036. The standard doclet
 * has no idea those are parameters: it renders them as an {@code <h6>} buried
 * inside the method description, which is why the generated pages carry no
 * parameter tables at all.
 *
 * <p>Parsing them back into structure is what lets the Hugo templates render a
 * real signature block. The convention is machine-regular, so this is reliable,
 * but it is still a convention rather than a language feature and the parser is
 * written defensively: only the six headings in {@link #STRUCTURAL} are claimed,
 * and every other heading -- {@code #### Example}, {@code #### Threading},
 * {@code #### Platform support} and a long tail of one-offs -- stays in the
 * description exactly where the author put it.
 */
public final class MarkdownSections {

    /**
     * The headings this parser treats as structure rather than prose.
     *
     * <p>Matched case insensitively against the heading text with any trailing
     * colon removed. Nothing is added here without checking what the sources
     * actually contain: a heading claimed by mistake silently disappears from
     * the description and reappears somewhere the author did not intend.
     */
    private static final List<String> STRUCTURAL =
            List.of("parameters", "returns", "throws", "see also", "deprecated", "since");

    /**
     * The headings that only mean anything on a method or a constructor.
     *
     * <p>A package and a type have no parameters, no return value and throw
     * nothing, so a heading of that name in one of their comments is prose about
     * something else. {@code com.codename1.db}'s package documentation has a
     * "Parameters" section describing how the database binds its arguments, and
     * claiming it lifted the whole section out of the page and then dropped it,
     * because a package page has nowhere to put a parameter table.
     *
     * <p>Deprecation and See-also stay structural everywhere: a package can be
     * deprecated, and does refer to other API.
     */
    private static final List<String> EXECUTABLE_ONLY =
            List.of("parameters", "returns", "throws");

    /** Parsed result: the prose that stays, plus whatever structure was lifted out of it. */
    public static final class Result {
        private final String description;
        private final List<NamedText> parameters;
        private final List<NamedText> exceptions;
        private final List<String> seeAlso;
        private final String returns;
        private final String deprecated;

        Result(String description, List<NamedText> parameters, List<NamedText> exceptions,
               List<String> seeAlso, String returns, String deprecated) {
            this.description = description;
            this.parameters = List.copyOf(parameters);
            this.exceptions = List.copyOf(exceptions);
            this.seeAlso = List.copyOf(seeAlso);
            this.returns = returns;
            this.deprecated = deprecated;
        }

        public String description() {
            return description;
        }

        public List<NamedText> parameters() {
            return parameters;
        }

        public List<NamedText> exceptions() {
            return exceptions;
        }

        public List<String> seeAlso() {
            return seeAlso;
        }

        /** Return description, or null when the comment documented none. */
        public String returns() {
            return returns;
        }

        /** Deprecation note, or null when the comment carried no deprecation section. */
        public String deprecated() {
            return deprecated;
        }
    }

    /** A bullet of the form {@code - `name`: text}, split into its two halves. */
    public record NamedText(String name, String text) {
    }

    private MarkdownSections() {
    }

    /**
     * Splits a markdown documentation body into prose and structure.
     *
     * @param markdown the raw comment body, exactly as the doclet received it
     * @return the parse, never null; a body with no structural headings yields a
     *         result whose description is the input and whose lists are empty
     */
    public static Result parse(String markdown) {
        return parse(markdown, true);
    }

    /**
     * @param executable whether the comment documents a method or constructor;
     *                   false for a package, a type or a field, whose comments
     *                   cannot carry parameters, a return value or exceptions
     */
    public static Result parse(String markdown, boolean executable) {
        if (markdown == null || markdown.isBlank()) {
            return new Result("", List.of(), List.of(), List.of(), null, null);
        }

        List<String> lines = List.of(markdown.split("\n", -1));
        List<String> prose = new ArrayList<>();
        List<NamedText> parameters = new ArrayList<>();
        List<NamedText> exceptions = new ArrayList<>();
        List<String> seeAlso = new ArrayList<>();
        String returns = null;
        String deprecated = null;

        boolean inFence = false;
        String fenceMarker = null;

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            String fence = fenceOpener(line);

            if (inFence) {
                prose.add(line);
                if (fence != null && fence.startsWith(fenceMarker)) {
                    inFence = false;
                    fenceMarker = null;
                }
                i++;
                continue;
            }
            if (fence != null) {
                inFence = true;
                fenceMarker = fence;
                prose.add(line);
                i++;
                continue;
            }

            int level = headingLevel(line);
            String key = level > 0 ? headingKey(line, level) : null;
            if (key == null || !STRUCTURAL.contains(key)
                    || (!executable && EXECUTABLE_ONLY.contains(key))) {
                prose.add(line);
                i++;
                continue;
            }

            int end = sectionEnd(lines, i + 1, level);
            String body = join(lines.subList(i + 1, end));
            switch (key) {
                case "parameters" -> parameters.addAll(bullets(body));
                case "throws" -> exceptions.addAll(bullets(body));
                case "see also" -> {
                    for (String item : bulletTexts(body)) {
                        seeAlso.add(item);
                    }
                }
                case "returns" -> {
                    if (returns == null && !body.isBlank()) {
                        returns = body.strip();
                    }
                }
                case "deprecated" -> {
                    if (deprecated == null) {
                        // A deprecation section with no body still marks the element
                        // deprecated, so an empty string is meaningfully different
                        // from the null that means "no section at all".
                        deprecated = body.strip();
                    }
                }
                // "since" is parsed only so that it is consumed rather than left in
                // the prose. Codename One does not publish availability metadata --
                // scripts/check-since-tags.sh rejects it in sources outright -- and a
                // guessed version is worse than no version, so the text is dropped.
                default -> {
                }
            }
            i = end;
        }

        return new Result(trimBlankEdges(prose), parameters, exceptions, seeAlso, returns, deprecated);
    }

    /**
     * The index one past the last line belonging to a section opened at the given
     * heading level: the next heading of the same or shallower level, or the end.
     * Headings inside fenced code are not headings.
     */
    private static int sectionEnd(List<String> lines, int from, int level) {
        boolean inFence = false;
        String fenceMarker = null;
        for (int i = from; i < lines.size(); i++) {
            String line = lines.get(i);
            String fence = fenceOpener(line);
            if (inFence) {
                if (fence != null && fence.startsWith(fenceMarker)) {
                    inFence = false;
                    fenceMarker = null;
                }
                continue;
            }
            if (fence != null) {
                inFence = true;
                fenceMarker = fence;
                continue;
            }
            int candidate = headingLevel(line);
            if (candidate > 0 && candidate <= level) {
                return i;
            }
        }
        return lines.size();
    }

    /**
     * The fence marker a line opens or closes, or null when the line is not a
     * fence.
     *
     * <p>A backtick fence's info string may not itself contain a backtick, so a
     * line that opens with a run and closes it again further along is an inline
     * code span, not a fence. {@code LocalNotification.setAlertSound} writes one
     * of exactly that shape, and treating it as an opener left the parser inside
     * a fence for the rest of the comment: the {@code #### Parameters} heading
     * below it was never recognised and the parameter lost its documentation.
     */
    private static String fenceOpener(String line) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("```")) {
            int run = 0;
            while (run < trimmed.length() && trimmed.charAt(run) == '`') {
                run++;
            }
            return trimmed.indexOf('`', run) >= 0 ? null : "```";
        }
        if (trimmed.startsWith("~~~")) {
            return "~~~";
        }
        return null;
    }

    /** ATX heading level, or 0 when the line is not an ATX heading. */
    private static int headingLevel(String line) {
        String trimmed = line.stripLeading();
        // More than three leading spaces would be an indented code block, and a
        // heading marker must be followed by a space to be a heading at all.
        if (line.length() - trimmed.length() > 3) {
            return 0;
        }
        int hashes = 0;
        while (hashes < trimmed.length() && trimmed.charAt(hashes) == '#') {
            hashes++;
        }
        if (hashes == 0 || hashes > 6) {
            return 0;
        }
        if (hashes == trimmed.length()) {
            return hashes;
        }
        return trimmed.charAt(hashes) == ' ' ? hashes : 0;
    }

    /** The comparable form of a heading's text: lower case, no trailing colon or hashes. */
    private static String headingKey(String line, int level) {
        String text = line.stripLeading().substring(level).strip();
        while (text.endsWith("#")) {
            text = text.substring(0, text.length() - 1).strip();
        }
        while (text.endsWith(":")) {
            text = text.substring(0, text.length() - 1).strip();
        }
        return text.toLowerCase(Locale.ROOT);
    }

    /** Splits a bullet list into {@code name} / {@code text} pairs. */
    private static List<NamedText> bullets(String body) {
        List<NamedText> out = new ArrayList<>();
        for (String item : bulletTexts(body)) {
            out.add(splitNamed(item));
        }
        return out;
    }

    /**
     * The raw text of each top level bullet, with continuation lines folded in.
     *
     * <p>A body that is not a list at all yields a single item holding the whole
     * body, so a hand written {@code #### Returns}-style paragraph under
     * {@code #### Parameters} is not silently dropped.
     */
    private static List<String> bulletTexts(String body) {
        List<String> out = new ArrayList<>();
        StringBuilder current = null;
        boolean sawBullet = false;
        boolean inFence = false;
        String fenceMarker = null;

        for (String line : body.split("\n", -1)) {
            String fence = fenceOpener(line);
            if (inFence) {
                if (current != null) {
                    current.append('\n').append(line);
                }
                if (fence != null && fence.startsWith(fenceMarker)) {
                    inFence = false;
                    fenceMarker = null;
                }
                continue;
            }

            String marker = bulletMarker(line);
            if (marker != null) {
                sawBullet = true;
                if (current != null) {
                    out.add(current.toString().strip());
                }
                current = new StringBuilder(line.stripLeading().substring(marker.length()).strip());
                if (fence != null) {
                    inFence = true;
                    fenceMarker = fence;
                }
                continue;
            }
            if (current != null) {
                current.append('\n').append(line);
                if (fence != null) {
                    inFence = true;
                    fenceMarker = fence;
                }
            }
        }
        if (current != null) {
            out.add(current.toString().strip());
        }
        if (!sawBullet && !body.isBlank()) {
            return List.of(body.strip());
        }
        return out;
    }

    /** The bullet marker a top level list item opens with, or null. */
    private static String bulletMarker(String line) {
        String trimmed = line.stripLeading();
        // Indentation beyond three spaces belongs to a nested list or a
        // continuation, both of which fold into the item already open.
        if (line.length() - trimmed.length() > 3) {
            return null;
        }
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
            return trimmed.substring(0, 2);
        }
        return null;
    }

    /**
     * Splits {@code `name`: text} into its halves.
     *
     * <p>The colon is only a separator when it follows something that looks like
     * an identifier, so a bullet that is a plain sentence containing a colon is
     * kept whole under an empty name rather than being cut in two.
     */
    static NamedText splitNamed(String item) {
        String text = item.strip();
        // A markdown reference link is how JEP 467 comments name a Java element,
        // and the renderer has already turned it into [`Name`](url) by the time
        // this sees it. com.codename1.nfc.NdefMessage.parse documents its
        // exception that way: without this the bullet produced a nameless entry
        // holding the prose, and the declared exception was then listed again
        // with no documentation at all.
        if (text.startsWith("[")) {
            int close = text.indexOf(']');
            if (close > 1) {
                String label = text.substring(1, close).replace("`", "").strip();
                String rest = text.substring(close + 1);
                if (rest.startsWith("(")) {
                    int url = matchingParen(rest);
                    if (url > 0) {
                        rest = rest.substring(url + 1);
                    }
                }
                int lastDot = label.lastIndexOf('.');
                String simple = lastDot < 0 ? label : label.substring(lastDot + 1);
                if (isIdentifierish(simple)) {
                    return new NamedText(label, stripSeparator(rest));
                }
            }
        }
        if (text.startsWith("`")) {
            int close = text.indexOf('`', 1);
            if (close > 1) {
                String name = text.substring(1, close);
                String rest = text.substring(close + 1).stripLeading();
                if (rest.startsWith(":")) {
                    rest = rest.substring(1).stripLeading();
                } else if (rest.startsWith("-")) {
                    rest = rest.substring(1).stripLeading();
                }
                if (isIdentifierish(name)) {
                    return new NamedText(name, stripRedundantTag(name, rest));
                }
            }
        }
        int colon = text.indexOf(':');
        if (colon > 0) {
            String name = text.substring(0, colon).strip();
            if (isIdentifierish(name)) {
                return new NamedText(name, stripRedundantTag(name, text.substring(colon + 1).stripLeading()));
            }
        }
        return new NamedText("", text);
    }

    /**
     * Removes a block tag the conversion left duplicated inside the bullet text.
     *
     * <p>1157 bullets across 184 files read {@code - `reverse`: @param reverse
     * creates a new transition...}: the name is given twice, once as the bullet
     * and again as a tag the markdown no longer needs. The standard pages show
     * that verbatim too, because to javadoc it is prose.
     *
     * <p>Conservative on purpose. The tag is only removed when it names the same
     * thing the bullet does, so a description that genuinely opens with an
     * at-sign is left alone.
     */
    private static String stripRedundantTag(String name, String text) {
        String rest = text.stripLeading();
        for (String tag : List.of("@param", "@throws", "@exception")) {
            String prefix = tag + " " + name;
            if (rest.startsWith(prefix)) {
                String after = rest.substring(prefix.length());
                if (after.isEmpty() || Character.isWhitespace(after.charAt(0))) {
                    return stripSeparator(after);
                }
            }
        }
        for (String tag : List.of("@return", "@returns")) {
            if (rest.startsWith(tag)) {
                String after = rest.substring(tag.length());
                if (after.isEmpty() || Character.isWhitespace(after.charAt(0))) {
                    return stripSeparator(after);
                }
            }
        }
        return text;
    }

    /** Drops the dash some of these use between the tag and the description. */
    private static String stripSeparator(String text) {
        String rest = text.stripLeading();
        if (rest.startsWith("- ")) {
            rest = rest.substring(2).stripLeading();
        }
        return rest;
    }

    /** The index of the parenthesis closing the one at position 0, or -1. */
    private static int matchingParen(String text) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Whether a candidate name could be a parameter or an exception type. */
    private static boolean isIdentifierish(String candidate) {
        if (candidate.isEmpty() || candidate.length() > 120) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(candidate.charAt(0))) {
            return false;
        }
        for (int i = 1; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.' && c != '<' && c != '>' && c != '[' && c != ']') {
                return false;
            }
        }
        return true;
    }

    private static String join(List<String> lines) {
        return String.join("\n", lines);
    }

    /** Joins prose lines and strips the blank lines a lifted section leaves behind. */
    private static String trimBlankEdges(List<String> lines) {
        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start).isBlank()) {
            start++;
        }
        while (end > start && lines.get(end - 1).isBlank()) {
            end--;
        }
        return join(lines.subList(start, end));
    }
}
