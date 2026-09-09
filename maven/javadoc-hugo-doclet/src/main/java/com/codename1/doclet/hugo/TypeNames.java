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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/** Renders types the way a reader wants to read them, and links them where we publish them. */
final class TypeNames {

    private final CommentRenderer.Links links;

    TypeNames(CommentRenderer.Links links) {
        this.links = links;
    }

    /**
     * A type reference for the templates: the source-like label, and the URL of
     * the page documenting its raw type.
     *
     * <p>Only the raw type carries a link. {@code Map<String, List<Integer>>}
     * renders whole and links to Map, rather than being decomposed into three
     * separately linked fragments -- the extra links are not worth the template
     * complexity, and the label still says exactly what the type is.
     */
    Map<String, Object> reference(TypeMirror type) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("label", label(type));
        out.put("url", url(type));
        return out;
    }

    /** A source-like rendering using simple names: {@code Map<String, List<Integer>>}. */
    String label(TypeMirror type) {
        if (type == null) {
            return "";
        }
        switch (type.getKind()) {
            case ARRAY:
                return label(((ArrayType) type).getComponentType()) + "[]";
            case DECLARED: {
                DeclaredType declared = (DeclaredType) type;
                String name = declared.asElement() instanceof TypeElement element
                        ? Refs.nestedDisplayName(element)
                        : declared.asElement().getSimpleName().toString();
                List<? extends TypeMirror> arguments = declared.getTypeArguments();
                if (arguments.isEmpty()) {
                    return name;
                }
                List<String> rendered = new ArrayList<>(arguments.size());
                for (TypeMirror argument : arguments) {
                    rendered.add(label(argument));
                }
                return name + "<" + String.join(", ", rendered) + ">";
            }
            case WILDCARD: {
                WildcardType wildcard = (WildcardType) type;
                if (wildcard.getExtendsBound() != null) {
                    return "? extends " + label(wildcard.getExtendsBound());
                }
                if (wildcard.getSuperBound() != null) {
                    return "? super " + label(wildcard.getSuperBound());
                }
                return "?";
            }
            case TYPEVAR:
                return ((TypeVariable) type).asElement().getSimpleName().toString();
            default:
                return type.toString();
        }
    }

    /** The page documenting a type's raw form, or null when we do not publish it. */
    String url(TypeMirror type) {
        TypeMirror current = type;
        while (current instanceof ArrayType array) {
            current = array.getComponentType();
        }
        if (current instanceof DeclaredType declared && declared.asElement() instanceof TypeElement element) {
            return links.url(element);
        }
        return null;
    }

    /**
     * The modifiers a declaration would actually write.
     *
     * <p>The model reports the implicit ones too, and writing them out produces
     * source no compiler would take: an enum is always final, an interface and
     * an annotation are always abstract, and a nested one is always static.
     */
    static String declarationModifiers(Element type) {
        String all = modifiers(type);
        return switch (type.getKind()) {
            case ENUM -> strip(all, "final", "static");
            case INTERFACE, ANNOTATION_TYPE -> strip(all, "abstract", "static");
            case RECORD -> strip(all, "final", "static");
            default -> all;
        };
    }

    private static String strip(String modifiers, String... implicit) {
        List<String> kept = new ArrayList<>();
        for (String word : modifiers.split(" ")) {
            if (!word.isEmpty() && !List.of(implicit).contains(word)) {
                kept.add(word);
            }
        }
        return String.join(" ", kept);
    }

    /** The declared modifiers in the order the language writes them. */
    static String modifiers(Element element) {
        List<String> ordered = new ArrayList<>();
        for (Modifier modifier : List.of(Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE,
                Modifier.ABSTRACT, Modifier.DEFAULT, Modifier.STATIC, Modifier.FINAL,
                Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.SYNCHRONIZED, Modifier.NATIVE,
                Modifier.STRICTFP)) {
            if (element.getModifiers().contains(modifier)) {
                ordered.add(modifier.toString());
            }
        }
        return String.join(" ", ordered);
    }

    /** The {@code <T, U extends V>} clause of a generic declaration, or an empty string. */
    String typeParameters(List<? extends javax.lang.model.element.TypeParameterElement> parameters) {
        if (parameters.isEmpty()) {
            return "";
        }
        List<String> rendered = new ArrayList<>(parameters.size());
        for (javax.lang.model.element.TypeParameterElement parameter : parameters) {
            StringBuilder text = new StringBuilder(parameter.getSimpleName().toString());
            List<String> bounds = new ArrayList<>();
            for (TypeMirror bound : parameter.getBounds()) {
                String rendering = label(bound);
                // Every type variable has java.lang.Object as an implicit bound, and
                // writing it out turns every generic signature into noise.
                if (!"Object".equals(rendering)) {
                    bounds.add(rendering);
                }
            }
            if (!bounds.isEmpty()) {
                text.append(" extends ").append(String.join(" & ", bounds));
            }
            rendered.add(text.toString());
        }
        return "<" + String.join(", ", rendered) + ">";
    }

    /**
     * A summary with its markdown reduced to the words it was marking up.
     *
     * <p>The search results list is plain text -- the page escapes what it is
     * given rather than rendering it, which is right for a value that came out
     * of a comment -- so a summary still carrying markdown shows its own source:
     * 622 of the 2096 types displayed backticks, emphasis markers or a whole
     * link destination in the results list.
     *
     * <p>The page-side summaries stay markdown, because Hugo renders those.
     */
    static String plainSummary(String markdown) {
        String text = summary(markdown);
        if (text.isEmpty()) {
            return text;
        }
        // ![alt](url) and [label](url) both reduce to the text a reader sees.
        // Scanned rather than matched with a regex: a member URL ends in a
        // signature, so its destination contains balanced parentheses and
        // stopping at the first ")" left one behind -- AdError arrived in the
        // index as "AdListener.onFailedToLoad(AdError))".
        text = unlink(text);

        // Everything below rewrites markup, and a code span contains none: its
        // content is literal by definition. JSONWriter.ArrayBuilder documents
        // itself as "Fluent builder for `[ ..., ..., ... ]`", and treating those
        // brackets as reference shorthand left the search result reading
        // "Fluent builder for  ..., ..., ... ." So each span is lifted out,
        // stripped of its delimiters, and put back untouched afterwards.
        List<String> spans = new ArrayList<>();
        StringBuilder masked = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != '`') {
                masked.append(c);
                i++;
                continue;
            }
            int ticks = 0;
            while (i + ticks < text.length() && text.charAt(i + ticks) == '`') {
                ticks++;
            }
            String fence = "`".repeat(ticks);
            int end = text.indexOf(fence, i + ticks);
            if (end < 0) {
                masked.append(c);
                i++;
                continue;
            }
            // NUL cannot appear in a doc comment that compiles, so it cannot
            // collide with the text being masked.
            masked.append('\0').append(spans.size()).append('\0');
            spans.add(text.substring(i + ticks, end));
            i = end + ticks;
        }
        text = masked.toString();

        // A reference link written as [Type] names that type.
        text = text.replaceAll("\\[([^\\]]*)\\]", "$1");
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replaceAll("(?<![A-Za-z0-9])[*_]([^*_]+)[*_](?![A-Za-z0-9])", "$1");
        // A marker with no partner left is not emphasis, it is a stray asterisk.
        text = text.replace("**", "");

        for (int span = 0; span < spans.size(); span++) {
            text = text.replace("\0" + span + "\0", spans.get(span));
        }
        return text.strip();
    }

    /**
     * Replaces every [label](destination) with its label, parentheses balanced.
     *
     * <p>Code spans are copied through whole. Brackets inside one are literal
     * text, not a reference: JSONWriter.ArrayBuilder documents itself as
     * "Fluent builder for `[ ..., ..., ... ]`", and stripping the brackets there
     * left the search result reading "Fluent builder for  ..., ..., ... ."
     */
    private static String unlink(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '`') {
                int ticks = 0;
                while (i + ticks < text.length() && text.charAt(i + ticks) == '`') {
                    ticks++;
                }
                String fence = "`".repeat(ticks);
                int end = text.indexOf(fence, i + ticks);
                int stop = end < 0 ? text.length() : end + ticks;
                out.append(text, i, stop);
                i = stop;
                continue;
            }
            if (c != '[' && !(c == '!' && i + 1 < text.length() && text.charAt(i + 1) == '[')) {
                out.append(c);
                i++;
                continue;
            }
            int open = c == '!' ? i + 1 : i;
            int close = text.indexOf(']', open);
            if (close < 0 || close + 1 >= text.length() || text.charAt(close + 1) != '(') {
                out.append(c);
                i++;
                continue;
            }
            int depth = 0;
            int j = close + 1;
            while (j < text.length()) {
                if (text.charAt(j) == '(') {
                    depth++;
                } else if (text.charAt(j) == ')') {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                }
                j++;
            }
            if (j >= text.length()) {
                out.append(c);
                i++;
                continue;
            }
            out.append(text, open + 1, close);
            i = j + 1;
        }
        return out.toString();
    }

    /**
     * A cut index that never falls between the halves of a surrogate pair.
     *
     * <p>Cutting one in half produces a lone surrogate, which is not
     * representable in UTF-8: writing it out fails with
     * UnmappableCharacterException rather than producing a broken file, so the
     * whole generation aborts on one emoji in one comment.
     */
    private static int safeCut(String text, int limit) {
        return Character.isHighSurrogate(text.charAt(limit - 1)) ? limit - 1 : limit;
    }

    /**
     * Whether the full stop at this index belongs to an abbreviation.
     *
     * <p>"(e.g. a bad ad unit id)" is one sentence, and AdError's constants were
     * being cut at the "g." -- leaving the summary ending mid-parenthesis in the
     * package table and in search.
     */
    private static boolean isAbbreviation(String text, int dot) {
        int start = dot;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) {
            start--;
        }
        String word = text.substring(start, dot).toLowerCase(Locale.ROOT);
        // "e.g", "i.e" and friends, plus any single letter: the "e." of "e.g."
        // is itself a full stop followed by a space in some house styles.
        return word.length() <= 1 || ABBREVIATIONS.contains(word);
    }

    private static final java.util.Set<String> ABBREVIATIONS = java.util.Set.of(
            "e.g", "i.e", "etc", "cf", "vs", "approx", "resp", "al", "fig", "no");

    /** Whether every parenthesis opened before this point has been closed. */
    private static boolean parenthesesBalanced(String text, int end) {
        int depth = 0;
        for (int i = 0; i < end; i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
        }
        return depth <= 0;
    }

    /** Whether a delimiter appears an odd number of times, so a span is left open. */
    private static boolean unbalanced(String text, String delimiter) {
        int count = 0;
        int at = text.indexOf(delimiter);
        while (at >= 0) {
            count++;
            at = text.indexOf(delimiter, at + delimiter.length());
        }
        return count % 2 != 0;
    }

    /**
     * The first sentence of a description, for summary tables.
     *
     * <p>Deliberately crude: it stops at the first sentence end, and it stops at a
     * blank line so that a description opening with a code fence or a list does
     * not drag the whole block into a table cell.
     */
    static String summary(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String text = description.strip();
        int blank = text.indexOf("\n\n");
        if (blank > 0) {
            text = text.substring(0, blank);
        }
        text = text.replace('\n', ' ').strip();
        // A full stop inside a code span does not end a sentence. Cutting there
        // leaves the span unterminated, and the summary is rendered as markdown
        // on the package page and shown raw in search: JSONWriter.ArrayBuilder
        // opens "Fluent builder for `[ ..., ..., ... ]`." and was cut mid span.
        boolean inCode = false;
        boolean inBold = false;
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (c == '`') {
                inCode = !inCode;
                continue;
            }
            // A full stop inside "**...**" is inside the span, and cutting there
            // leaves the emphasis open: BrowserNavigationCallback and Dictionary
            // both open with a bold note whose first sentence ends inside it.
            if (!inCode && c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                inBold = !inBold;
                i++;
                continue;
            }
            if (!inCode && !inBold && c == '.' && Character.isWhitespace(text.charAt(i + 1))
                    && !isAbbreviation(text, i) && parenthesesBalanced(text, i + 1)) {
                return text.substring(0, i + 1);
            }
        }
        if (text.endsWith(".")) {
            return text;
        }
        if (text.length() <= 240) {
            return text;
        }
        // The hard cap can land inside a span too, and walking back until it does
        // not is the wrong repair: BrowserNavigationCallback opens with a bold
        // note that runs past the cap, so backing out of it would leave nothing.
        // Closing what the cut opened keeps the text and the markup valid.
        String cropped = text.substring(0, safeCut(text, 240)).strip() + "...";
        if (unbalanced(cropped, "**")) {
            cropped += "**";
        }
        if (unbalanced(cropped, "`")) {
            cropped += "`";
        }
        return cropped;
    }
}
