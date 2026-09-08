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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Page paths and fragment identifiers, in the exact shapes the standard doclet
 * emits.
 *
 * <p>This is the compatibility surface of the whole generator. 304 distinct
 * {@code /javadoc/...} URLs are linked from the website content and the
 * developer guide, plus an unknown number from outside the project, and a
 * fragment is part of the URL. Anything this class spells differently from
 * javadoc is a link that used to work and now does not, which is why the
 * encoding below was read off real javadoc output rather than assumed:
 *
 * <pre>
 *   id="&lt;init&gt;()"                     constructors
 *   id="f"                                  fields, the bare name
 *   id="g(java.util.List)"                  type arguments erased away
 *   id="arr(byte[],int[][])"                arrays keep their brackets
 *   id="va(java.lang.String...)"            varargs keep the ellipsis...
 *   id="of(T...)" AND id="of(java.lang.Object[])"   ...but the erasure does not
 *   id="inner(p.A.B)"                       nested types are dot separated
 *   id="t(T)" AND id="t(java.lang.Object)"  a type variable gets both spellings
 * </pre>
 *
 * <p>Note the last one: javadoc emits two identifiers for a method with a type
 * variable parameter, the declared spelling and the erasure, so a link written
 * either way resolves. {@link #anchors} returns both for the same reason.
 */
final class Refs {

    private final Types types;

    Refs(Types types) {
        this.types = types;
    }

    /**
     * Every fragment identifier a member should answer to, the primary one first.
     *
     * @param member a field, constructor or method
     * @return one identifier for a field, and one or two for an executable
     */
    List<String> anchors(Element member) {
        if (!(member instanceof ExecutableElement executable)) {
            return List.of(member.getSimpleName().toString());
        }

        String name = member.getKind() == ElementKind.CONSTRUCTOR
                ? "<init>"
                : member.getSimpleName().toString();

        String declared = name + "(" + String.join(",", parameterTypes(executable, false)) + ")";
        String erased = name + "(" + String.join(",", parameterTypes(executable, true)) + ")";
        return declared.equals(erased) ? List.of(declared) : List.of(declared, erased);
    }

    private List<String> parameterTypes(ExecutableElement executable, boolean erase) {
        List<? extends VariableElement> parameters = executable.getParameters();
        List<String> out = new ArrayList<>(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            TypeMirror type = parameters.get(i).asType();
            if (erase) {
                type = types.erasure(type);
            }
            // The declared spelling keeps the ellipsis; the erased spelling does
            // not. javadoc gives Stream.of(T... values) both "of(T...)" and
            // "of(java.lang.Object[])", so writing the ellipsis into the erased
            // form loses the second address entirely.
            boolean varargs = !erase && executable.isVarArgs() && i == parameters.size() - 1;
            out.add(anchorType(type, varargs));
        }
        return out;
    }

    /** A single parameter type as javadoc spells it inside a fragment identifier. */
    private String anchorType(TypeMirror type, boolean varargs) {
        if (type instanceof ArrayType array) {
            return anchorType(array.getComponentType(), false) + (varargs ? "..." : "[]");
        }
        if (type instanceof DeclaredType declared && declared.asElement() instanceof TypeElement element) {
            // Reading the qualified name off the element rather than the mirror is
            // what erases the type arguments: List<String> becomes java.util.List,
            // and the nested p.A.B keeps its dots.
            return element.getQualifiedName().toString();
        }
        return type.toString();
    }

    /**
     * The site path of a documented type's page.
     *
     * <p>The directory form, not {@code Outer.Inner.html}, because Cloudflare
     * Pages will not serve a {@code .html} URL: its own html_handling redirects
     * {@code /x.html} to {@code /x} before any asset is considered, and the
     * site's redirect table separately maps {@code /*.html} to {@code /:splat/}.
     * Publishing the page at the extension was therefore a page nobody could
     * reach -- every API link 301'd away from it, and the alias it landed on
     * bounced the reader onto the production domain.
     *
     * <p>The javadoc spelling still works and still lands here: the redirect
     * table turns {@code /javadoc/com/codename1/ui/Label.html} into
     * {@code /javadoc/com/codename1/ui/Label/}, fragment intact, which is this
     * URL. That is checked end to end against the Pages runtime rather than
     * assumed; see scripts/website/check-javadoc-urls.sh.
     */
    static String typeUrl(TypeElement type) {
        PackageElement pkg = packageOf(type);
        String dir = pkg.isUnnamed() ? "" : pkg.getQualifiedName().toString().replace('.', '/') + "/";
        return "/javadoc/" + dir + fileName(type) + "/";
    }

    /** The site path of a package's summary page, in the same directory form. */
    static String packageUrl(PackageElement pkg) {
        return "/javadoc/" + pkg.getQualifiedName().toString().replace('.', '/') + "/package-summary/";
    }

    /** The content file a type's page is generated into, relative to the content root. */
    static String typeContentPath(TypeElement type) {
        PackageElement pkg = packageOf(type);
        String dir = pkg.isUnnamed() ? "" : pkg.getQualifiedName().toString().replace('.', '/') + "/";
        return dir + fileName(type) + ".md";
    }

    /** The content file a package's summary is generated into. */
    static String packageContentPath(PackageElement pkg) {
        return pkg.getQualifiedName().toString().replace('.', '/') + "/package-summary.md";
    }

    /**
     * A type's page file name: the simple name for a top level type, and the outer
     * names joined by dots for a nested one.
     */
    private static String fileName(TypeElement type) {
        StringBuilder name = new StringBuilder(type.getSimpleName().toString());
        Element parent = type.getEnclosingElement();
        while (parent instanceof TypeElement outer) {
            name.insert(0, outer.getSimpleName().toString() + ".");
            parent = outer.getEnclosingElement();
        }
        return name.toString();
    }

    /** The display name of a nested type, qualified by its outer types but not its package. */
    static String nestedDisplayName(TypeElement type) {
        return fileName(type);
    }

    static PackageElement packageOf(Element element) {
        Element current = element;
        while (current != null && !(current instanceof PackageElement)) {
            current = current.getEnclosingElement();
        }
        return (PackageElement) current;
    }

    /** The type a member belongs to, or null when the element is not a member. */
    static TypeElement enclosingType(Element member) {
        Element current = member.getEnclosingElement();
        while (current != null && !(current instanceof TypeElement)) {
            current = current.getEnclosingElement();
        }
        return (TypeElement) current;
    }
}
