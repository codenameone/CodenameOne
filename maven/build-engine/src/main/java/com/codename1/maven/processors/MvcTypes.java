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

import com.codename1.maven.annotations.AnnotatedClass;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.*;

/** Resolves property and parent signatures in the context of a declared model type. */
final class MvcTypes {
    final AnnotatedClass cls;
    final Map<String, String> bindings = new LinkedHashMap<String, String>();
    final List<StringBuilder> parents = new ArrayList<StringBuilder>();

    MvcTypes(AnnotatedClass cls, String owner) {
        this.cls = cls;
        final List<String> arguments = arguments(owner);
        if (cls.getSignature() == null) {
            if (cls.getSuperInternalName() != null)
                parents.add(new StringBuilder(cls.getSuperInternalName().replace('/', '.')));
            for (String parent : cls.getInterfaceInternalNames())
                parents.add(new StringBuilder(parent.replace('/', '.')));
        } else {
            new SignatureReader(cls.getSignature())
                    .accept(
                            new SignatureVisitor(Opcodes.ASM9) {
                                private int index;

                                @Override
                                public void visitFormalTypeParameter(String name) {
                                    bindings.put(
                                            name,
                                            index < arguments.size()
                                                    ? arguments.get(index)
                                                    : "java.lang.Object");
                                    index++;
                                }

                                @Override
                                public SignatureVisitor visitClassBound() {
                                    return ignored();
                                }

                                @Override
                                public SignatureVisitor visitInterfaceBound() {
                                    return ignored();
                                }

                                @Override
                                public SignatureVisitor visitSuperclass() {
                                    return parent();
                                }

                                @Override
                                public SignatureVisitor visitInterface() {
                                    return parent();
                                }

                                private SignatureVisitor parent() {
                                    StringBuilder out = new StringBuilder();
                                    parents.add(out);
                                    return new JavaType(out, bindings);
                                }
                            });
        }
    }

    String member(Type erased, String signature, boolean method) {
        if (signature == null) return RestClientAnnotationProcessor.javaTypeFor(erased, null);
        final StringBuilder out = new StringBuilder();
        final Map<String, String> scope = new LinkedHashMap<String, String>(bindings);
        SignatureReader reader = new SignatureReader(signature);
        if (!method) reader.acceptType(new JavaType(out, scope));
        else
            reader.accept(
                    new SignatureVisitor(Opcodes.ASM9) {
                        // A method type parameter shadows one with the same name on the class.
                        @Override
                        public void visitFormalTypeParameter(String name) {
                            scope.put(name, "java.lang.Object");
                        }

                        @Override
                        public SignatureVisitor visitClassBound() {
                            return ignored();
                        }

                        @Override
                        public SignatureVisitor visitInterfaceBound() {
                            return ignored();
                        }

                        @Override
                        public SignatureVisitor visitParameterType() {
                            return ignored();
                        }

                        @Override
                        public SignatureVisitor visitExceptionType() {
                            return ignored();
                        }

                        @Override
                        public SignatureVisitor visitReturnType() {
                            return new JavaType(out, scope);
                        }
                    });
        return out.toString();
    }

    private static SignatureVisitor ignored() {
        return new SignatureVisitor(Opcodes.ASM9) {};
    }

    private static List<String> arguments(String type) {
        List<String> result = new ArrayList<String>();
        int start = type.indexOf('<');
        if (start < 0) return result;
        int depth = 0;
        start++;
        for (int i = start; i < type.length(); i++) {
            char c = type.charAt(i);
            if ((c == ',' || c == '>') && depth == 0) {
                result.add(type.substring(start, i).trim());
                start = i + 1;
                if (c == '>') break;
            } else if (c == '<') depth++;
            else if (c == '>') depth--;
        }
        return result;
    }

    private static final class JavaType extends SignatureVisitor {
        private final StringBuilder out;
        private final Map<String, String> bindings;
        private final String suffix;
        private boolean arguments;

        JavaType(StringBuilder out, Map<String, String> bindings) {
            this(out, bindings, "");
        }

        JavaType(StringBuilder out, Map<String, String> bindings, String suffix) {
            super(Opcodes.ASM9);
            this.out = out;
            this.bindings = bindings;
            this.suffix = suffix;
        }

        @Override
        public void visitBaseType(char descriptor) {
            out.append(Type.getType(String.valueOf(descriptor)).getClassName()).append(suffix);
        }

        @Override
        public void visitTypeVariable(String name) {
            String type = bindings.get(name);
            out.append(type == null ? "java.lang.Object" : type).append(suffix);
        }

        @Override
        public SignatureVisitor visitArrayType() {
            return new JavaType(out, bindings, "[]" + suffix);
        }

        @Override
        public void visitClassType(String name) {
            out.append(name.replace('/', '.'));
        }

        @Override
        public void visitInnerClassType(String name) {
            closeArguments();
            out.append('.').append(name);
        }

        private void argument() {
            out.append(arguments ? ',' : '<');
            arguments = true;
        }

        @Override
        public void visitTypeArgument() {
            argument();
            out.append('?');
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            argument();
            if (wildcard == EXTENDS) out.append("? extends ");
            else if (wildcard == SUPER) out.append("? super ");
            return new JavaType(out, bindings);
        }

        private void closeArguments() {
            if (arguments) {
                out.append('>');
                arguments = false;
            }
        }

        @Override
        public void visitEnd() {
            closeArguments();
            out.append(suffix);
        }
    }
}
