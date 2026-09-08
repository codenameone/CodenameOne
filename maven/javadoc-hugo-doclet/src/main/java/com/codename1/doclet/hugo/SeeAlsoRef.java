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

/**
 * One entry of a {@code #### See also} list, split into the reference it names
 * and whatever prose follows it.
 *
 * <p>These are not {@code @see} block tags. They are markdown bullets, so the
 * doclet receives them as text and nothing has resolved them: the reference has
 * to be parsed here. Measured over CodenameOne/src and Ports/CLDC11/src, the
 * list is 1180 local member references ({@code #drawRoundRect}), 426 qualified
 * ones ({@code Display#supportsNativeImageCache()}), 238 bare type names, and a
 * tail of 50 that carry trailing prose or are prose outright.
 *
 * <p>Both member forms routinely carry a parameter list, and roughly a third of
 * the local ones carry a trailing sentence, as in
 * {@code #isShapeSupported(java.lang.Object) to determine if the graphics
 * context supports drawing}. Splitting on the first space is wrong for a
 * signature containing one, so the split tracks parenthesis depth.
 */
final class SeeAlsoRef {

    private final String type;
    private final String member;
    private final List<String> parameters;
    private final boolean hasParameterList;
    private final String label;

    private SeeAlsoRef(String type, String member, List<String> parameters,
                       boolean hasParameterList, String label) {
        this.type = type;
        this.member = member;
        this.parameters = parameters;
        this.hasParameterList = hasParameterList;
        this.label = label;
    }

    /** The type half, empty when the reference is local to the current type. */
    String type() {
        return type;
    }

    /** The member half, empty when the reference names a type. */
    String member() {
        return member;
    }

    /** The parameter types as written, empty when none were given. */
    List<String> parameters() {
        return parameters;
    }

    /**
     * Whether the reference wrote a parameter list at all.
     *
     * <p>Distinct from an empty list: {@code #clear()} names the no-argument
     * overload specifically, while {@code #clear} names the method and leaves
     * the choice open.
     */
    boolean hasParameterList() {
        return hasParameterList;
    }

    /** The trailing prose, empty when the entry was only a reference. */
    String label() {
        return label;
    }

    /** Whether this entry looks like a reference at all rather than a sentence. */
    boolean isReference() {
        return !type.isEmpty() || !member.isEmpty();
    }

    /** The reference as written, without the trailing prose. */
    String text() {
        StringBuilder out = new StringBuilder(type);
        if (!member.isEmpty()) {
            out.append('#').append(member);
            if (hasParameterList) {
                out.append('(').append(String.join(", ", parameters)).append(')');
            }
        }
        return out.toString();
    }

    static SeeAlsoRef parse(String entry) {
        String text = entry == null ? "" : entry.strip();
        if (text.isEmpty()) {
            return new SeeAlsoRef("", "", List.of(), false, "");
        }

        // A markdown link is already a link; leave it whole for the caller to pass
        // through rather than trying to read a Java reference out of it.
        if (text.startsWith("[") || text.contains("](")) {
            return new SeeAlsoRef("", "", List.of(), false, text);
        }

        int split = referenceEnd(text);
        String reference = text.substring(0, split);
        String label = text.substring(split).strip();

        String typePart = reference;
        String memberPart = "";
        int hash = reference.indexOf('#');
        if (hash >= 0) {
            typePart = reference.substring(0, hash);
            memberPart = reference.substring(hash + 1);
        }

        List<String> parameters = new ArrayList<>();
        boolean hasList = false;
        int open = memberPart.indexOf('(');
        if (open >= 0) {
            hasList = true;
            int close = memberPart.lastIndexOf(')');
            String inside = close > open ? memberPart.substring(open + 1, close) : "";
            memberPart = memberPart.substring(0, open);
            for (String parameter : splitParameters(inside)) {
                if (!parameter.isBlank()) {
                    parameters.add(parameter.strip());
                }
            }
        }

        if (!isReferenceLike(typePart) || !isReferenceLike(memberPart)) {
            return new SeeAlsoRef("", "", List.of(), false, text);
        }
        return new SeeAlsoRef(typePart.strip(), memberPart.strip(), parameters, hasList, label);
    }

    /** The index where the reference stops and any trailing prose begins. */
    private static int referenceEnd(String text) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth = Math.max(0, depth - 1);
            } else if (Character.isWhitespace(c) && depth == 0) {
                return i;
            }
        }
        return text.length();
    }

    /** Splits a parameter list on commas that are not inside nested generics. */
    private static List<String> splitParameters(String inside) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < inside.length(); i++) {
            char c = inside.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth = Math.max(0, depth - 1);
            }
            if (c == ',' && depth == 0) {
                out.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        out.add(current.toString());
        return out;
    }

    /** Whether a half of the reference could be a Java name rather than prose. */
    private static boolean isReferenceLike(String candidate) {
        String text = candidate.strip();
        if (text.isEmpty()) {
            return true;
        }
        if (!Character.isJavaIdentifierStart(text.charAt(0))) {
            return false;
        }
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.' && c != '<' && c != '>'
                    && c != '[' && c != ']') {
                return false;
            }
        }
        return true;
    }
}
