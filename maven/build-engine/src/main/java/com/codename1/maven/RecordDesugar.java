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
package com.codename1.maven;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

/// Replaces the two `invokedynamic` families javac emits for records and
/// pattern switches with plain methods of the class itself.
///
/// A record's `equals`, `hashCode` and `toString` are one `invokedynamic`
/// each, bootstrapped by `java.lang.runtime.ObjectMethods`; a `switch` over
/// types or over an enum with patterns is one bootstrapped by
/// `java.lang.runtime.SwitchBootstraps`. Neither bootstrap class exists on a
/// device, and both build their answer from method handles, which do not
/// exist there either. Everything they need is in the call site's static
/// arguments, though, so each site becomes a call of a private static
/// synthetic method generated here with the site's own descriptor.
///
/// The results are the JDK's: `equals` compares primitives by value (floats
/// and doubles by their bits, as `Float.compare` does) and references with
/// `Objects.equals`; `hashCode` is `31 * h + hash(component)` from zero;
/// `toString` is `Name[a=1, b=x]`.
///
/// A switch label this cannot express -- a primitive type, a constant of a
/// shape javac does not write today -- leaves its call site as it was, which
/// the compliance check then reports.
final class RecordDesugar extends ClassVisitor {
    private static final String OBJECT_METHODS = "java/lang/runtime/ObjectMethods";
    private static final String SWITCHES = "java/lang/runtime/SwitchBootstraps";
    private static final String BUILDER = "java/lang/StringBuilder";

    private final List<Site> sites = new ArrayList<Site>();
    private String className;
    private String simpleName;
    private boolean isInterface;

    RecordDesugar(ClassVisitor next) {
        super(Opcodes.ASM9, next);
    }

    /// One replaced call site and the method that stands in for it.
    private static final class Site {
        final String method;
        final String kind;
        final String descriptor;
        final Object[] arguments;

        Site(String method, String kind, String descriptor, Object[] arguments) {
            this.method = method;
            this.kind = kind;
            this.descriptor = descriptor;
            this.arguments = arguments;
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        className = name;
        simpleName = name.substring(name.lastIndexOf('/') + 1);
        isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (name.equals(className) && innerName != null) {
            // What Class.getSimpleName answers for a nested or local record.
            simpleName = innerName;
        }
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (mv == null) {
            return null;
        }
        return new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap,
                    Object... arguments) {
                String kind = kind(name, descriptor, bootstrap, arguments);
                if (kind == null) {
                    super.visitInvokeDynamicInsn(name, descriptor, bootstrap, arguments);
                    return;
                }
                String method = "cn1$" + kind + "$" + sites.size();
                sites.add(new Site(method, kind, descriptor, arguments));
                super.visitMethodInsn(Opcodes.INVOKESTATIC, className, method, descriptor, isInterface);
            }
        };
    }

    @Override
    public void visitEnd() {
        for (Site site : sites) {
            MethodVisitor mv = super.visitMethod(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    site.method, site.descriptor, null, null);
            if (mv == null) {
                continue;
            }
            mv.visitCode();
            if ("equals".equals(site.kind)) {
                recordEquals(mv, site);
            } else if ("hashCode".equals(site.kind)) {
                recordHashCode(mv, site);
            } else if ("toString".equals(site.kind)) {
                recordToString(mv, site);
            } else if ("typeSwitch".equals(site.kind)) {
                typeSwitch(mv, site);
            } else {
                enumSwitch(mv, site);
            }
            mv.visitEnd();
        }
        super.visitEnd();
    }

    /// Which of the five this call site is, or null for one that is left
    /// alone.
    private String kind(String name, String descriptor, Handle bootstrap, Object[] arguments) {
        if (isInterface) {
            // A private static method of an interface is legal, but no
            // record is an interface and javac puts no switch in one's
            // abstract methods; a default method's switch is reported.
            return null;
        }
        String owner = bootstrap.getOwner();
        Type[] parameters = Type.getArgumentTypes(descriptor);
        Type result = Type.getReturnType(descriptor);
        if (OBJECT_METHODS.equals(owner) && "bootstrap".equals(bootstrap.getName())) {
            if (arguments.length < 2 || !(arguments[0] instanceof Type) || !(arguments[1] instanceof String)) {
                return null;
            }
            for (int i = 2; i < arguments.length; i++) {
                if (!(arguments[i] instanceof Handle) || ((Handle) arguments[i]).getTag() != Opcodes.H_GETFIELD) {
                    return null;
                }
            }
            if ("equals".equals(name) && parameters.length == 2 && result.getSort() == Type.BOOLEAN) {
                return "equals";
            }
            if ("hashCode".equals(name) && parameters.length == 1 && result.getSort() == Type.INT) {
                return "hashCode";
            }
            if ("toString".equals(name) && parameters.length == 1 && result.getSort() == Type.OBJECT) {
                return "toString";
            }
            return null;
        }
        if (!SWITCHES.equals(owner) || parameters.length != 2 || parameters[1].getSort() != Type.INT
                || result.getSort() != Type.INT || !reference(parameters[0])) {
            return null;
        }
        if ("typeSwitch".equals(bootstrap.getName())) {
            for (Object label : arguments) {
                boolean type = label instanceof Type && reference((Type) label);
                if (!type && !(label instanceof String) && !(label instanceof Integer)
                        && enumConstant(label) == null) {
                    return null;
                }
            }
            return "typeSwitch";
        }
        if ("enumSwitch".equals(bootstrap.getName())) {
            for (Object label : arguments) {
                boolean type = label instanceof Type && reference((Type) label);
                if (!type && !(label instanceof String)) {
                    return null;
                }
            }
            return "enumSwitch";
        }
        return null;
    }

    /// The enum's internal name and the constant's name when `label` is a
    /// constant of a sealed hierarchy's enum, which javac writes as a
    /// dynamic constant: `EnumDesc.of(ClassDesc.of("p.E"), "NAME")`, each
    /// call made through `ConstantBootstraps.invoke`. Null for anything
    /// else, including the same idea spelled another way.
    private static String[] enumConstant(Object label) {
        Object[] outer = invoked(label, "java/lang/Enum$EnumDesc", "of", 2);
        if (outer == null || !(outer[1] instanceof String)) {
            return null;
        }
        Object[] inner = invoked(outer[0], "java/lang/constant/ClassDesc", "of", 1);
        if (inner == null || !(inner[0] instanceof String)) {
            return null;
        }
        return new String[] {((String) inner[0]).replace('.', '/'), (String) outer[1]};
    }

    /// The arguments of `owner.name(...)` when `constant` is that call made
    /// by `ConstantBootstraps.invoke` with `count` arguments, else null.
    private static Object[] invoked(Object constant, String owner, String name, int count) {
        if (!(constant instanceof ConstantDynamic)) {
            return null;
        }
        ConstantDynamic dynamic = (ConstantDynamic) constant;
        Handle bootstrap = dynamic.getBootstrapMethod();
        if (!"java/lang/invoke/ConstantBootstraps".equals(bootstrap.getOwner())
                || !"invoke".equals(bootstrap.getName())
                || dynamic.getBootstrapMethodArgumentCount() != count + 1) {
            return null;
        }
        Object called = dynamic.getBootstrapMethodArgument(0);
        if (!(called instanceof Handle)) {
            return null;
        }
        Handle handle = (Handle) called;
        if (handle.getTag() != Opcodes.H_INVOKESTATIC || !owner.equals(handle.getOwner())
                || !name.equals(handle.getName())) {
            return null;
        }
        Object[] arguments = new Object[count];
        for (int i = 0; i < count; i++) {
            arguments[i] = dynamic.getBootstrapMethodArgument(i + 1);
        }
        return arguments;
    }

    private static boolean reference(Type type) {
        return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
    }

    private static Handle component(Site site, int index) {
        Object handle = site.arguments[index + 2];
        // Checked by kind() before the site was recorded.
        return handle instanceof Handle ? (Handle) handle : null;
    }

    private static int components(Site site) {
        return site.arguments.length - 2;
    }

    private static void field(MethodVisitor mv, Handle handle) {
        mv.visitFieldInsn(Opcodes.GETFIELD, handle.getOwner(), handle.getName(), handle.getDesc());
    }

    /// `(LR;Ljava/lang/Object;)Z`. The other record is cast again for every
    /// component rather than kept in a local, so that every jump to the
    /// "different" exit arrives with the same two locals and an empty stack.
    private void recordEquals(MethodVisitor mv, Site site) {
        Type record = Type.getArgumentTypes(site.descriptor)[0];
        String name = record.getInternalName();
        Label different = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.INSTANCEOF, name);
        mv.visitJumpInsn(Opcodes.IFEQ, different);
        for (int i = 0; i < components(site); i++) {
            Handle handle = component(site, i);
            if (handle == null) {
                continue;
            }
            int sort = Type.getType(handle.getDesc()).getSort();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            field(mv, handle);
            bits(mv, sort);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, name);
            field(mv, handle);
            bits(mv, sort);
            if (sort == Type.LONG || sort == Type.DOUBLE) {
                mv.visitInsn(Opcodes.LCMP);
                mv.visitJumpInsn(Opcodes.IFNE, different);
            } else if (sort == Type.OBJECT || sort == Type.ARRAY) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "equals",
                        "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                mv.visitJumpInsn(Opcodes.IFEQ, different);
            } else {
                mv.visitJumpInsn(Opcodes.IF_ICMPNE, different);
            }
        }
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(different);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(4, 2);
    }

    /// A float or a double on the stack becomes the bits two equal values
    /// share, which is how the JDK compares a record's: a NaN equals a NaN
    /// and the two zeros differ.
    private static void bits(MethodVisitor mv, int sort) {
        if (sort == Type.FLOAT) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "floatToIntBits", "(F)I", false);
        } else if (sort == Type.DOUBLE) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "doubleToLongBits", "(D)J", false);
        }
    }

    /// `(LR;)I`, with no branch in it.
    private void recordHashCode(MethodVisitor mv, Site site) {
        mv.visitInsn(Opcodes.ICONST_0);
        for (int i = 0; i < components(site); i++) {
            Handle handle = component(site, i);
            if (handle == null) {
                continue;
            }
            int sort = Type.getType(handle.getDesc()).getSort();
            mv.visitIntInsn(Opcodes.BIPUSH, 31);
            mv.visitInsn(Opcodes.IMUL);
            if (sort == Type.BOOLEAN) {
                // Boolean.hashCode: 1231 for true and 1237 for false.
                mv.visitIntInsn(Opcodes.SIPUSH, 1237);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                field(mv, handle);
                mv.visitIntInsn(Opcodes.BIPUSH, 6);
                mv.visitInsn(Opcodes.IMUL);
                mv.visitInsn(Opcodes.ISUB);
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                field(mv, handle);
                bits(mv, sort);
                if (sort == Type.LONG || sort == Type.DOUBLE) {
                    mv.visitInsn(Opcodes.DUP2);
                    mv.visitIntInsn(Opcodes.BIPUSH, 32);
                    mv.visitInsn(Opcodes.LUSHR);
                    mv.visitInsn(Opcodes.LXOR);
                    mv.visitInsn(Opcodes.L2I);
                } else if (sort == Type.OBJECT || sort == Type.ARRAY) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "hashCode",
                            "(Ljava/lang/Object;)I", false);
                }
            }
            mv.visitInsn(Opcodes.IADD);
        }
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(6, 1);
    }

    /// `(LR;)Ljava/lang/String;`
    private void recordToString(MethodVisitor mv, Site site) {
        Object joined = site.arguments[1];
        String text = joined instanceof String ? (String) joined : "";
        String[] names = text.length() == 0 ? new String[0] : text.split(";");
        mv.visitTypeInsn(Opcodes.NEW, BUILDER);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, BUILDER, "<init>", "()V", false);
        append(mv, simpleName + "[");
        for (int i = 0; i < components(site); i++) {
            Handle handle = component(site, i);
            if (handle == null) {
                continue;
            }
            String label = i < names.length ? names[i] : handle.getName();
            append(mv, (i == 0 ? "" : ", ") + label + "=");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            field(mv, handle);
            int sort = Type.getType(handle.getDesc()).getSort();
            String type;
            if (sort == Type.BOOLEAN) {
                type = "Z";
            } else if (sort == Type.CHAR) {
                type = "C";
            } else if (sort == Type.LONG) {
                type = "J";
            } else if (sort == Type.FLOAT) {
                type = "F";
            } else if (sort == Type.DOUBLE) {
                type = "D";
            } else if (sort == Type.OBJECT || sort == Type.ARRAY) {
                type = "Ljava/lang/Object;";
            } else {
                type = "I";
            }
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, BUILDER, "append", "(" + type + ")L" + BUILDER + ";", false);
        }
        append(mv, "]");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, BUILDER, "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 1);
    }

    private static void append(MethodVisitor mv, String text) {
        mv.visitLdcInsn(text);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, BUILDER, "append",
                "(Ljava/lang/String;)L" + BUILDER + ";", false);
    }

    /// `(LT;I)I`: the index of the first label at or after the second
    /// argument that the first one matches, the number of labels when none
    /// does, and -1 for null. Every jump lands with the same two locals and
    /// an empty stack.
    private void typeSwitch(MethodVisitor mv, Site site) {
        Label start = nullCheck(mv);
        Label next = start;
        for (int i = 0; i < site.arguments.length; i++) {
            Object label = site.arguments[i];
            if (label instanceof Integer) {
                // A constant matches the Integer and the Character of that
                // value, as the JDK's does.
                next = constant(mv, next, i, ((Integer) label).intValue(), "java/lang/Integer", "intValue", "()I");
                next = constant(mv, next, i, ((Integer) label).intValue(), "java/lang/Character", "charValue",
                        "()C");
                continue;
            }
            Label after = begin(mv, next, i);
            String[] constant = enumConstant(label);
            if (constant != null) {
                // An enum has one instance of each constant, so being of the
                // enum's type and having the name is being that constant.
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitTypeInsn(Opcodes.INSTANCEOF, constant[0]);
                mv.visitJumpInsn(Opcodes.IFEQ, after);
                mv.visitLdcInsn(constant[1]);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Enum");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "name", "()Ljava/lang/String;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",
                        false);
            } else if (label instanceof Type) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitTypeInsn(Opcodes.INSTANCEOF, ((Type) label).getInternalName());
            } else {
                mv.visitLdcInsn(label);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",
                        false);
            }
            mv.visitJumpInsn(Opcodes.IFEQ, after);
            mv.visitLdcInsn(Integer.valueOf(i));
            mv.visitInsn(Opcodes.IRETURN);
            next = after;
        }
        end(mv, next, site.arguments.length);
    }

    /// `(LE;I)I`, where a label is the name of a constant or a type.
    private void enumSwitch(MethodVisitor mv, Site site) {
        Label start = nullCheck(mv);
        Label next = start;
        for (int i = 0; i < site.arguments.length; i++) {
            Object label = site.arguments[i];
            Label after = begin(mv, next, i);
            if (label instanceof Type) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitTypeInsn(Opcodes.INSTANCEOF, ((Type) label).getInternalName());
                mv.visitJumpInsn(Opcodes.IFEQ, after);
            } else {
                // The selector's static type may be an interface the enum
                // implements, so it is asked whether it is an enum first.
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/Enum");
                mv.visitJumpInsn(Opcodes.IFEQ, after);
                mv.visitLdcInsn(label);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Enum");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Enum", "name", "()Ljava/lang/String;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",
                        false);
                mv.visitJumpInsn(Opcodes.IFEQ, after);
            }
            mv.visitLdcInsn(Integer.valueOf(i));
            mv.visitInsn(Opcodes.IRETURN);
            next = after;
        }
        end(mv, next, site.arguments.length);
    }

    private static Label nullCheck(MethodVisitor mv) {
        Label start = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitJumpInsn(Opcodes.IFNONNULL, start);
        mv.visitInsn(Opcodes.ICONST_M1);
        mv.visitInsn(Opcodes.IRETURN);
        return start;
    }

    /// Opens the test of label `index` at `here`, skipping it when the
    /// switch restarts after it; answers where a failed test continues.
    private static Label begin(MethodVisitor mv, Label here, int index) {
        Label after = new Label();
        mv.visitLabel(here);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitLdcInsn(Integer.valueOf(index));
        mv.visitJumpInsn(Opcodes.IF_ICMPGT, after);
        return after;
    }

    private static Label constant(MethodVisitor mv, Label here, int index, int value, String box, String unbox,
            String descriptor) {
        Label after = begin(mv, here, index);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitTypeInsn(Opcodes.INSTANCEOF, box);
        mv.visitJumpInsn(Opcodes.IFEQ, after);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitTypeInsn(Opcodes.CHECKCAST, box);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, box, unbox, descriptor, false);
        mv.visitLdcInsn(Integer.valueOf(value));
        mv.visitJumpInsn(Opcodes.IF_ICMPNE, after);
        mv.visitLdcInsn(Integer.valueOf(index));
        mv.visitInsn(Opcodes.IRETURN);
        return after;
    }

    private static void end(MethodVisitor mv, Label here, int count) {
        mv.visitLabel(here);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitLdcInsn(Integer.valueOf(count));
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(2, 2);
    }
}
