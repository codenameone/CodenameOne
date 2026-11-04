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
import com.codename1.maven.annotations.ProcessorContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// Finds the application's calls to the typed `Json` methods, so the build
/// writes a codec for each class they name.
///
/// `Json.read(body, Release.class)` names its class with the literal right
/// before the call: the class is the last argument, so its `LDC` is the last
/// instruction before the `INVOKESTATIC`. `Json.write(release)` names nothing,
/// and the type of what is written is taken from the instruction that put it on
/// the stack -- a method's return type, a field's, a cast, or for a local what
/// was stored in it and its declared type in the debug table. A site that says nothing still makes the build
/// generate the registry, and a class some other use gave a codec works there.
final class JsonCallSites {
    private static final String JSON = "com/codename1/backend/Json";

    private JsonCallSites() {
    }

    /// Hands every typed call site of the application's own classes to `codecs`,
    /// and reports a class the build cannot write a codec for at its call.
    static void scan(final ProcessorContext ctx, final BackendJsonCodecs codecs) {
        for (final AnnotatedClass cls : new ArrayList<AnnotatedClass>(
                ctx.getClassIndex().values())) {
            File file = cls.getClassFile();
            if (file == null || !file.isFile() || ctx.isDependencyClass(cls)
                    || cls.isFromBackendLibrary()) {
                continue;
            }
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                ctx.error(cls, "Cannot read " + file + ": " + e.getMessage());
                continue;
            }
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, final String name, String desc,
                                                 String signature, String[] exceptions) {
                    return new Sites(ctx, codecs, cls, name);
                }
            }, ClassReader.SKIP_FRAMES);
        }
    }

    /// One method: the type the last instruction left on the stack, and the
    /// writes of a local whose type only the debug table at the end can say.
    private static final class Sites extends MethodVisitor {
        private final ProcessorContext ctx;
        private final BackendJsonCodecs codecs;
        private final AnnotatedClass cls;
        private final String method;
        /// The class the last instruction pushed, as a binary name, or null.
        private String last;
        /// The local the last instruction loaded, or -1.
        private int lastLocal = -1;
        private final List<Integer> writtenLocals = new ArrayList<Integer>();
        private final Map<Integer, List<String>> localTypes = new HashMap<Integer, List<String>>();

        Sites(ProcessorContext ctx, BackendJsonCodecs codecs, AnnotatedClass cls, String method) {
            super(Opcodes.ASM9);
            this.ctx = ctx;
            this.codecs = codecs;
            this.cls = cls;
            this.method = method;
        }

        private void pushed(String type) {
            last = type;
            lastLocal = -1;
        }

        @Override
        public void visitLdcInsn(Object value) {
            pushed(value instanceof Type && ((Type) value).getSort() == Type.OBJECT
                    ? ((Type) value).getClassName() : null);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (opcode == Opcodes.ASTORE && last != null) {
                // What the slot was given: the type a class compiled without its
                // debug table still says, through the cast that came before.
                local(var, last);
            }
            pushed(null);
            if (opcode == Opcodes.ALOAD) {
                lastLocal = var;
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            pushed(opcode == Opcodes.CHECKCAST ? objectType(Type.getObjectType(type)) : null);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            pushed(opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC
                    ? objectType(Type.getType(desc)) : null);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc,
                                    boolean itf) {
            if (opcode == Opcodes.INVOKESTATIC && JSON.equals(owner)) {
                if (("read".equals(name) || "readList".equals(name))
                        && "(Ljava/lang/String;Ljava/lang/Class;)".equals(params(desc))
                        || "convert".equals(name)
                        && "(Ljava/lang/Object;Ljava/lang/Class;)".equals(params(desc))) {
                    use(last, true, name);
                } else if ("write".equals(name)
                        && "(Ljava/lang/Object;)Ljava/lang/String;".equals(desc)) {
                    if (lastLocal >= 0) {
                        writtenLocals.add(Integer.valueOf(lastLocal));
                    } else if (last != null) {
                        use(last, false, name);
                    }
                }
            }
            pushed(objectType(Type.getReturnType(desc)));
        }

        @Override
        public void visitInsn(int opcode) {
            pushed(null);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            pushed(null);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            pushed(null);
        }

        @Override
        public void visitLabel(Label label) {
            pushed(null);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc,
                                           org.objectweb.asm.Handle bsm, Object... args) {
            pushed(null);
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start,
                                       Label end, int index) {
            String type = objectType(Type.getType(desc));
            if (type != null) {
                local(index, type);
            }
        }

        private void local(int index, String type) {
            List<String> types = localTypes.get(Integer.valueOf(index));
            if (types == null) {
                types = new ArrayList<String>();
                localTypes.put(Integer.valueOf(index), types);
            }
            if (!types.contains(type)) {
                types.add(type);
            }
        }

        @Override
        public void visitEnd() {
            for (Integer local : writtenLocals) {
                List<String> types = localTypes.get(local);
                if (types != null) {
                    // A slot reused for another variable names every type it held;
                    // a codec too many is harmless.
                    for (String type : types) {
                        use(type, false, "write");
                    }
                }
            }
        }

        private void use(String type, boolean read, String call) {
            if (!read && (type == null || type.startsWith("java."))) {
                // Json writes the JDK's shapes itself, and an Object says nothing.
                return;
            }
            String why = codecs.typedUse(type, read);
            if (why != null) {
                ctx.error(cls, "Json." + call + " in " + cls.getSourceName() + "." + method
                        + " " + (read ? "reads " : "writes ")
                        + BackendJsonCodecs.source(type) + ", which cannot be "
                        + (read ? "read" : "written") + " as JSON: " + why);
            }
        }
    }

    private static String params(String desc) {
        return desc.substring(0, desc.indexOf(')') + 1);
    }

    /// The binary name of `type` when it is a class, else null.
    private static String objectType(Type type) {
        return type.getSort() == Type.OBJECT ? type.getClassName() : null;
    }
}
