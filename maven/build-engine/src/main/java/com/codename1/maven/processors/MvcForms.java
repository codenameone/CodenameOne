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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.*;

import org.objectweb.asm.Type;

import java.util.*;

/** Generates explicit scalar assignment, never reflective entity mass binding. */
final class MvcForms {
    private MvcForms() {}

    static String binding(ProcessorContext ctx, String type, String name, String local) {
        AnnotatedClass cls =
                RestControllerAnnotationProcessor.resolveClass(ctx, type.replace('.', '/'));
        if (cls == null || !cls.isPublic() || cls.isAbstract() || cls.isInterface())
            throw new IllegalArgumentException("Form must be a public concrete DTO: " + type);
        for (String annotation : cls.getAllAnnotationDescriptors())
            if (annotation.endsWith("/Entity;") || annotation.endsWith("/Table;"))
                throw new IllegalArgumentException(
                        "Bind a dedicated form DTO, not an ORM entity: " + type);
        boolean constructor = false;
        for (MethodInfo m : cls.getMethods())
            constructor |= m.isConstructor() && m.isPublic() && "()V".equals(m.getDescriptor());
        if (!constructor)
            throw new IllegalArgumentException(
                    "Form requires a public no-argument constructor: " + type);
        Map<String, String[]> fields = new TreeMap<String, String[]>();
        for (AnnotatedClass c = cls;
                c != null && !"java/lang/Object".equals(c.getInternalName());
                c = RestControllerAnnotationProcessor.resolveClass(ctx, c.getSuperInternalName())) {
            for (String annotation : c.getAllAnnotationDescriptors())
                if (annotation.endsWith("/Entity;") || annotation.endsWith("/Table;"))
                    throw new IllegalArgumentException(
                            "Bind a dedicated form DTO, not an ORM entity: " + type);
            for (FieldInfo f : c.getFields())
                if (f.isPublic()
                        && !f.isStatic()
                        && !f.isFinal()
                        && !fields.containsKey(f.getName()))
                    fields.put(
                            f.getName(),
                            new String[] {
                                Type.getType(f.getDescriptor()).getClassName(),
                                "." + f.getName() + " = ",
                                ";"
                            });
            for (MethodInfo m : c.getMethods())
                if (m.isPublic()
                        && !m.isStatic()
                        && m.getName().startsWith("set")
                        && m.getName().length() > 3
                        && Type.getArgumentTypes(m.getDescriptor()).length == 1
                        && Type.getReturnType(m.getDescriptor()).equals(Type.VOID_TYPE)) {
                    String field = java.beans.Introspector.decapitalize(m.getName().substring(3));
                    if (!fields.containsKey(field))
                        fields.put(
                                field,
                                new String[] {
                                    Type.getArgumentTypes(m.getDescriptor())[0].getClassName(),
                                    "." + m.getName() + "(",
                                    ");"
                                });
                }
        }
        if (fields.isEmpty())
            throw new IllegalArgumentException("Form has no writable scalar properties: " + type);
        StringBuilder s = new StringBuilder();
        s.append(cls.getSourceName())
                .append(' ')
                .append(local)
                .append(" = new ")
                .append(cls.getSourceName())
                .append("();\n")
                .append("com.codename1.backend.mvc.BindingResult ")
                .append(local)
                .append("Errors = new com.codename1.backend.mvc.BindingResult();\n")
                .append("try {\n");
        for (Map.Entry<String, String[]> entry : fields.entrySet()) {
            String field = entry.getKey(), t = entry.getValue()[0];
            String convert = conversion(t, "raw");
            if (convert == null)
                throw new IllegalArgumentException(
                        "Form property must be scalar: " + type + "." + field);
            s.append("{ String raw = request.param(").append(q(field)).append(");\n");
            boolean bool = "boolean".equals(t) || "java.lang.Boolean".equals(t);
            if (bool)
                s.append("if (raw == null && request.param(")
                        .append(q("_" + field))
                        .append(") != null) raw = \"false\";\n");
            boolean primitive = !t.startsWith("java.lang.");
            if (primitive) s.append("if (raw != null) {\n");
            s.append(local).append("Errors.submitted(").append(q(field)).append(", raw);\ntry {\n");
            if (!"java.lang.String".equals(t)) {
                if (t.startsWith("java.lang."))
                    s.append("if (raw == null || raw.length() == 0) ")
                            .append(local)
                            .append(entry.getValue()[1])
                            .append("null")
                            .append(entry.getValue()[2])
                            .append(" else {\n");
                else
                    s.append(
                            "if (raw == null || raw.length() == 0) throw new"
                                    + " IllegalArgumentException();\n");
            }
            s.append(local)
                    .append(entry.getValue()[1])
                    .append(convert)
                    .append(entry.getValue()[2])
                    .append('\n');
            if (t.startsWith("java.lang.") && !"java.lang.String".equals(t)) s.append("}\n");
            s.append("} catch (IllegalArgumentException invalid) { ")
                    .append(local)
                    .append("Errors.rejectValue(")
                    .append(q(field))
                    .append(", \"Invalid value\"); }\n");
            if (primitive) s.append("}\n");
            s.append("}\n");
        }
        s.append(
                        "} catch (IllegalArgumentException invalidForm) { return"
                                + " com.codename1.backend.HttpServer.Response.text(400, \"Malformed"
                                + " form\"); }\n")
                .append(
                        "catch (IllegalStateException invalidForm) { return"
                                + " com.codename1.backend.HttpServer.Response.text(400, \"Malformed"
                                + " form\"); }\n")
                .append("cn1Model.addAttribute(")
                .append(q(name))
                .append(", ")
                .append(local)
                .append(");\n")
                .append("cn1Model.addAttribute(")
                .append(q("BindingResult." + name))
                .append(", ")
                .append(local)
                .append("Errors);\n");
        return s.toString();
    }

    private static String conversion(String type, String raw) {
        if ("java.lang.String".equals(type)) return raw;
        if ("boolean".equals(type) || "java.lang.Boolean".equals(type))
            return "com.codename1.backend.mvc.FormValues.bool(" + raw + ")";
        if ("char".equals(type) || "java.lang.Character".equals(type))
            return "com.codename1.backend.mvc.FormValues.character(" + raw + ")";
        String[] prim = {"byte", "short", "int", "long", "float", "double"};
        String[] box = {"Byte", "Short", "Integer", "Long", "Float", "Double"};
        for (int i = 0; i < prim.length; i++)
            if (prim[i].equals(type) || ("java.lang." + box[i]).equals(type))
                return "java.lang." + box[i] + ".valueOf(" + raw + ")";
        return null;
    }

    static String q(String s) {
        return RestControllerAnnotationProcessor.quote(s);
    }
}
