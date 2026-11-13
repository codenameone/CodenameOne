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
package com.codename1.annotations.processing;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.tools.Diagnostic;

/// Writes the builder of every record `@Entity` while the application
/// compiles, so the application's own code can call it: `NoteBuilder` for
/// `Note`, in the same package. `create()` starts from default component
/// values, `of(note)` copies every component, each setter replaces one and
/// `build()` calls the canonical constructor.
///
/// Everything else the ORM generates is written after compilation, from the
/// classes; a builder cannot wait for that, because the code that names it is
/// compiled in this same pass. Built for Java 8 so it loads in any javac, which
/// is why records are recognised by the name of their kind.
public class RecordBuilderProcessor extends AbstractProcessor {
    private static final String ENTITY = "com.codename1.annotations.Entity";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(ENTITY);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        TypeElement entity = processingEnv.getElementUtils().getTypeElement(ENTITY);
        if (entity == null) {
            return false;
        }
        for (Element element : round.getElementsAnnotatedWith(entity)) {
            if ("RECORD".equals(element.getKind().name())) {
                write((TypeElement) element);
            }
        }
        return false;
    }

    private void write(TypeElement record) {
        if (!reachable(record)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "A record entity cannot be private: its builder could not reach it", record);
            return;
        }
        PackageElement pkg = processingEnv.getElementUtils().getPackageOf(record);
        String packageName = pkg.isUnnamed() ? "" : pkg.getQualifiedName().toString();
        String binary = processingEnv.getElementUtils().getBinaryName(record).toString();
        String builder = (packageName.isEmpty() ? binary : binary.substring(packageName.length() + 1)) + "Builder";
        String type = record.getQualifiedName().toString();
        // A generic record's builder takes the erased types, as a raw use of
        // the record would.
        boolean erase = !record.getTypeParameters().isEmpty();
        List<String[]> components = new ArrayList<String[]>();
        for (Element member : record.getEnclosedElements()) {
            // A record declares exactly one instance field per component, in
            // component order, and may declare no other.
            if (member.getKind() == ElementKind.FIELD && !member.getModifiers().contains(Modifier.STATIC)) {
                TypeMirror t = ((VariableElement) member).asType();
                if (erase) {
                    t = processingEnv.getTypeUtils().erasure(t);
                }
                components.add(new String[] {member.getSimpleName().toString(), source(t)});
            }
        }
        String visibility = publicType(record) ? "public " : "";
        StringBuilder sb = new StringBuilder();
        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
        sb.append("// Generated by the Codename One annotation processor. Do not edit.\n")
          .append("@SuppressWarnings({\"all\"})\n")
          .append(visibility).append("final class ").append(builder).append(" {\n");
        for (String[] c : components) {
            sb.append("    private ").append(c[1]).append(' ').append(c[0]).append(";\n");
        }
        sb.append("\n    private ").append(builder).append("() {\n    }\n\n")
          .append("    /// A builder with every component at its default.\n")
          .append("    public static ").append(builder).append(" create() {\n")
          .append("        return new ").append(builder).append("();\n    }\n\n")
          .append("    /// A builder holding every component of `source`.\n")
          .append("    public static ").append(builder).append(" of(").append(type).append(" source) {\n")
          .append("        ").append(builder).append(" b = new ").append(builder).append("();\n");
        for (String[] c : components) {
            sb.append("        b.").append(c[0]).append(" = source.").append(c[0]).append("();\n");
        }
        sb.append("        return b;\n    }\n\n");
        for (String[] c : components) {
            sb.append("    public ").append(builder).append(' ').append(c[0]).append('(').append(c[1]).append(" value) {\n")
              .append("        this.").append(c[0]).append(" = value;\n        return this;\n    }\n\n");
        }
        sb.append("    public ").append(type).append(" build() {\n")
          .append("        return new ").append(type).append('(');
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(components.get(i)[0]);
        }
        sb.append(");\n    }\n}\n");
        String name = packageName.isEmpty() ? builder : packageName + "." + builder;
        try {
            Writer out = processingEnv.getFiler().createSourceFile(name, record).openWriter();
            try {
                out.write(sb.toString());
            } finally {
                out.close();
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Could not write " + name + ": " + e.getMessage(), record);
        }
    }

    /// `type` as source names it, without its type-use annotations. `toString()`
    /// would keep them, and on some JDKs puts one in front of a qualified name,
    /// `@N java.util.List`, which is not source at all.
    private static String source(TypeMirror type) {
        TypeKind kind = type.getKind();
        if (kind.isPrimitive()) {
            return kind.name().toLowerCase(java.util.Locale.ROOT);
        }
        if (kind == TypeKind.ARRAY) {
            return source(((ArrayType) type).getComponentType()) + "[]";
        }
        if (kind == TypeKind.TYPEVAR) {
            return ((TypeVariable) type).asElement().getSimpleName().toString();
        }
        if (kind == TypeKind.WILDCARD) {
            WildcardType w = (WildcardType) type;
            return w.getExtendsBound() != null ? "? extends " + source(w.getExtendsBound())
                    : w.getSuperBound() != null ? "? super " + source(w.getSuperBound()) : "?";
        }
        if (kind == TypeKind.DECLARED) {
            DeclaredType declared = (DeclaredType) type;
            StringBuilder sb = new StringBuilder(((TypeElement) declared.asElement()).getQualifiedName());
            List<? extends TypeMirror> arguments = declared.getTypeArguments();
            if (!arguments.isEmpty()) {
                sb.append('<');
                for (int i = 0; i < arguments.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(source(arguments.get(i)));
                }
                sb.append('>');
            }
            return sb.toString();
        }
        return type.toString();
    }

    /// Whether the record and every type enclosing it are public.
    private static boolean publicType(Element type) {
        for (Element e = type; e instanceof TypeElement; e = e.getEnclosingElement()) {
            if (!e.getModifiers().contains(Modifier.PUBLIC)) {
                return false;
            }
        }
        return true;
    }

    /// Whether code in the record's package can name it.
    private static boolean reachable(Element type) {
        for (Element e = type; e instanceof TypeElement; e = e.getEnclosingElement()) {
            if (e.getModifiers().contains(Modifier.PRIVATE)) {
                return false;
            }
        }
        return true;
    }
}
