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

    /** Whether cutting here would leave a code span open. */
    private static boolean unbalancedCodeSpan(String text, int cut) {
        int ticks = 0;
        for (int i = 0; i < cut; i++) {
            if (text.charAt(i) == '`') {
                ticks++;
            }
        }
        return ticks % 2 != 0;
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
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (c == '`') {
                inCode = !inCode;
                continue;
            }
            if (!inCode && c == '.' && Character.isWhitespace(text.charAt(i + 1))) {
                return text.substring(0, i + 1);
            }
        }
        if (text.endsWith(".")) {
            return text;
        }
        if (text.length() <= 240) {
            return text;
        }
        // The same rule applies to the hard length cap.
        int cut = safeCut(text, 240);
        while (cut > 0 && unbalancedCodeSpan(text, cut)) {
            cut--;
        }
        return text.substring(0, cut).strip() + "...";
    }
}
