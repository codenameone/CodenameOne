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
package com.codename1.cil.translate;

import com.codename1.cil.metadata.CilAssembly;
import com.codename1.cil.metadata.CilType;
import com.codename1.cil.metadata.Universe;
import java.util.Set;

/// How CIL names and types are spelled on the JVM. Every class the
/// translator writes and every reference it emits goes through here, so a
/// definition and its uses cannot disagree.
///
/// Two kinds of type exist. A type from an assembly being translated keeps
/// its namespace as its package. Every other type is one the runtime library
/// provides, written in Java, and lives under [#RUNTIME] with its namespace
/// lower-cased. Generics are erased: ``List`1`` is one class, `List_1`, and a
/// type variable is `Object`.
final class Names {
    static final String RUNTIME = "com/codename1/unitycompat/";
    /// The package of a translated type that has no namespace.
    static final String GLOBAL = "global/";
    static final String OBJECT = "java/lang/Object";
    static final String STRING = "java/lang/String";
    static final String INTEROP = RUNTIME + "system/Interop";
    static final String DELEGATE = RUNTIME + "system/Delegate";
    static final String EXCEPTION = RUNTIME + "system/Exception";
    static final String MD_ARRAY = RUNTIME + "system/MdArray";
    /// What every struct implements, for runtime code that has to copy or
    /// reset one without knowing its class.
    static final String STRUCT = RUNTIME + "system/Struct";

    final Universe universe;
    private final Set<CilAssembly> translated;

    Names(Universe universe, Set<CilAssembly> translated) {
        this.universe = universe;
        this.translated = translated;
    }

    boolean isTranslated(String fullName) {
        CilAssembly.TypeDef def = universe.find(fullName);
        return def != null && translated.contains(def.assembly);
    }

    static String sanitize(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean plain = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'
                    || c == '$';
            sb.append(plain ? c : '_');
        }
        return sb.toString();
    }

    private static String lowerAscii(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return sb.toString();
    }

    /// JVM internal name of a named CIL type.
    String className(String fullName) {
        if ("System.Object".equals(fullName) || "System.ValueType".equals(fullName)
                || "System.Enum".equals(fullName) || "System.Array".equals(fullName)) {
            return OBJECT;
        }
        if ("System.String".equals(fullName)) {
            return STRING;
        }
        String[] nesting = fullName.split("/");
        String outer = nesting[0];
        int dot = outer.lastIndexOf('.');
        String namespace = dot < 0 ? "" : outer.substring(0, dot);
        StringBuilder sb = new StringBuilder();
        if (isTranslated(fullName)) {
            if (namespace.length() > 0) {
                sb.append(sanitizePath(namespace)).append('/');
            } else {
                // Java cannot import a class of the unnamed package, and the
                // generated scene code and the hand-written runtime both have
                // to name these. Most Unity scripts declare no namespace.
                sb.append(GLOBAL);
            }
        } else {
            sb.append(RUNTIME);
            if (namespace.length() > 0) {
                sb.append(sanitizePath(lowerAscii(namespace))).append('/');
            }
        }
        sb.append(sanitize(outer.substring(dot + 1)));
        for (int i = 1; i < nesting.length; i++) {
            sb.append('$').append(sanitize(nesting[i]));
        }
        return sb.toString();
    }

    private static String sanitizePath(String namespace) {
        String[] parts = namespace.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(sanitize(parts[i]));
        }
        return sb.toString();
    }

    /// The class of static helpers that stands in for a built-in type's own
    /// methods. `int`, `string` and `object` are JVM primitives and JDK
    /// classes, which cannot be given methods, so `x.ToString()` on an `int`
    /// becomes `Int32_.ToString(x)`.
    static String helperClass(String fullName) {
        return RUNTIME + "system/" + sanitize(fullName.substring(fullName.lastIndexOf('.') + 1)) + "_";
    }

    static boolean hasHelperClass(String fullName) {
        return fullName != null && (CilType.builtin(fullName) != null || fullName.equals("System.Array")
                || fullName.equals("System.ValueType") || fullName.equals("System.Enum"));
    }

    boolean isEnum(CilType type) {
        if (type.kind != CilType.Kind.VALUETYPE) {
            return false;
        }
        CilAssembly.TypeDef def = universe.find(type.name);
        return def != null && def.isEnum();
    }

    /// An enum is stored and passed as the integer under it; everything else
    /// is itself.
    CilType norm(CilType type) {
        if (type.kind == CilType.Kind.VALUETYPE) {
            CilAssembly.TypeDef def = universe.find(type.name);
            if (def != null && def.isEnum()) {
                return def.enumUnderlyingType();
            }
        }
        return type;
    }

    /// A value type that is an object on the JVM: a struct, as opposed to a
    /// primitive or an enum.
    boolean isStruct(CilType type) {
        if (type.kind == CilType.Kind.GENERICINST) {
            return isStruct(type.element);
        }
        return type.kind == CilType.Kind.VALUETYPE && !isEnum(type);
    }

    /// The [Val] kind a value of this type has on the evaluation stack.
    int kind(CilType type) {
        CilType t = norm(type);
        switch (t.kind) {
            case BOOLEAN:
            case CHAR:
            case I1:
            case U1:
            case I2:
            case U2:
            case I4:
            case U4:
            case I:
            case U:
                return Val.I4;
            case I8:
            case U8:
                return Val.I8;
            case R4:
                return Val.R4;
            case R8:
                return Val.R8;
            default:
                return Val.REF;
        }
    }

    /// The `java.lang` class a primitive is boxed as.
    static String boxClass(CilType primitive) {
        switch (primitive.kind) {
            case BOOLEAN:
                return "java/lang/Boolean";
            case CHAR:
            case U2:
                return "java/lang/Character";
            case I1:
            case U1:
                return "java/lang/Byte";
            case I2:
                return "java/lang/Short";
            case I8:
            case U8:
                return "java/lang/Long";
            case R4:
                return "java/lang/Float";
            case R8:
                return "java/lang/Double";
            default:
                return "java/lang/Integer";
        }
    }

    /// What `instanceof` tests for a type: its class, or the box of a
    /// primitive.
    String instanceClass(CilType type) {
        CilType t = norm(type);
        return t.isPrimitive() ? boxClass(t) : classRef(t);
    }

    static boolean isTypeVariable(CilType type) {
        return type.kind == CilType.Kind.VAR || type.kind == CilType.Kind.MVAR;
    }

    /// JVM internal name for a reference to the class of a struct or class
    /// type, or the array descriptor of an array type: what `checkcast`,
    /// `instanceof` and `anewarray` take.
    String classRef(CilType type) {
        switch (type.kind) {
            case STRING:
                return STRING;
            case OBJECT:
            case VAR:
            case MVAR:
                return OBJECT;
            case CLASS:
            case VALUETYPE:
                return className(type.name);
            case GENERICINST:
                return className(type.element.name);
            case SZARRAY:
                return descriptor(type);
            case ARRAY:
                return mdArrayClass(type.element);
            default:
                throw new TranslationException("type " + type + " has no class of its own");
        }
    }

    String descriptor(CilType type) {
        CilType t = norm(type);
        switch (t.kind) {
            case VOID:
                return "V";
            case BOOLEAN:
                return "Z";
            case CHAR:
            case U2:
                return "C";
            case I1:
            case U1:
                return "B";
            case I2:
                return "S";
            case I4:
            case U4:
            case I:
            case U:
                return "I";
            case I8:
            case U8:
                return "J";
            case R4:
                return "F";
            case R8:
                return "D";
            case STRING:
                return "Ljava/lang/String;";
            case OBJECT:
            case VAR:
            case MVAR:
                return "Ljava/lang/Object;";
            case CLASS:
            case VALUETYPE:
                return "L" + className(t.name) + ";";
            case GENERICINST:
                return "L" + className(t.element.name) + ";";
            case SZARRAY:
                return "[" + descriptor(t.element);
            case ARRAY:
                return "L" + mdArrayClass(t.element) + ";";
            default:
                throw new TranslationException("type " + type + " cannot be expressed on the JVM yet");
        }
    }

    /// The runtime class of an array of two or more dimensions, `T[,]` and
    /// up. Such an array is one flat JVM array -- of the primitive, where the
    /// element is one -- with its dimensions beside it, and there is one
    /// final class per kind of flat array. The rank is not part of the class:
    /// an `int[,]` and an `int[,,]` are both an `MdArrayInt`.
    String mdArrayClass(CilType element) {
        CilType e = norm(element);
        String kind;
        switch (e.kind) {
            case BOOLEAN:
                kind = "Boolean";
                break;
            case I1:
            case U1:
                kind = "Byte";
                break;
            case CHAR:
            case U2:
                kind = "Char";
                break;
            case I2:
                kind = "Short";
                break;
            case I4:
            case U4:
            case I:
            case U:
                kind = "Int";
                break;
            case I8:
            case U8:
                kind = "Long";
                break;
            case R4:
                kind = "Float";
                break;
            case R8:
                kind = "Double";
                break;
            default:
                kind = "Object";
                break;
        }
        return MD_ARRAY + kind;
    }

    /// What the flat array of a `T[,]` holds, as a descriptor: the primitive,
    /// or `Object` for every reference and struct.
    String mdElementDescriptor(CilType element) {
        CilType e = norm(element);
        return e.isPrimitive() ? descriptor(e) : "L" + OBJECT + ";";
    }

    /// A by-reference parameter. A struct is already an object, so a
    /// reference to one is that object. A reference to anything else is a
    /// one-element-or-more array and the index into it, two JVM parameters,
    /// which covers `ref x`, `out x` and `ref array[i]` alike.
    String parameterDescriptor(CilType type) {
        if (type.kind != CilType.Kind.BYREF) {
            return descriptor(type);
        }
        if (isStruct(type.element)) {
            return descriptor(type.element);
        }
        return "[" + descriptor(type.element) + "I";
    }

    String methodDescriptor(CilAssembly.MethodSig sig) {
        return methodDescriptor(sig, null);
    }

    /// True for a method declared to return a struct. Such a method takes one
    /// more parameter than it declares, after all the others: the object to
    /// write its result into. It returns a reference the caller may read but
    /// must not keep -- that object, or one nothing else can reach.
    ///
    /// The point is that returning a struct then allocates nothing. `a + b`
    /// on two vectors is a call, and a frame of a game is thousands of them;
    /// an object per call measured fourteen times slower than the same
    /// arithmetic on bare floats under ParparVM, and this about one and a half.
    ///
    /// The caller always passes an object nothing else refers to, never the
    /// variable the result is on its way to, so a callee may write to it at
    /// any point without disturbing an argument it has yet to read.
    ///
    /// A method declared to return a type variable is not one of these, even
    /// where the variable stands for a struct: erased code cannot know.
    boolean returnsStruct(CilAssembly.MethodSig sig) {
        return sig.returnType.kind != CilType.Kind.VOID && !isTypeVariable(sig.returnType)
                && isStruct(sig.returnType);
    }

    /// `receiver`, when given, is prepended as a first parameter: the shape of
    /// a helper-class method standing in for an instance method.
    String methodDescriptor(CilAssembly.MethodSig sig, String receiver) {
        StringBuilder sb = new StringBuilder("(");
        if (receiver != null) {
            sb.append(receiver);
        }
        for (CilType p : sig.params) {
            sb.append(parameterDescriptor(p));
        }
        if (sig.returnType.kind == CilType.Kind.BYREF) {
            throw new TranslationException("a method returning by reference is not supported");
        }
        if (returnsStruct(sig)) {
            sb.append(descriptor(sig.returnType));
        }
        return sb.append(')').append(descriptor(sig.returnType)).toString();
    }

    /// What a method of a helper class is called after its C# name when it
    /// takes an enum: `$` and the enum's name, once per such parameter. An
    /// enum is its integer on the JVM, so `Split(char[], int)` and
    /// `Split(char[], StringSplitOptions)` would be one descriptor and the
    /// wrong one of them would be called without a word.
    String enumSuffix(CilAssembly.MethodSig sig) {
        StringBuilder sb = null;
        for (CilType p : sig.params) {
            if (isEnum(p)) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                String n = p.typeName();
                sb.append('$').append(sanitize(n.substring(n.lastIndexOf('.') + 1)));
            }
        }
        return sb == null ? "" : sb.toString();
    }

    /// The JVM name of a method. The three `System.Object` virtuals map onto
    /// their `java.lang.Object` counterparts so that a C# override is the Java
    /// override too, and string concatenation, hash maps and the debugger see
    /// what the C# author wrote.
    String methodName(String name, CilAssembly.MethodSig sig, boolean ownerIsStruct) {
        if (".ctor".equals(name)) {
            return ownerIsStruct ? "$ctor" : "<init>";
        }
        if (".cctor".equals(name)) {
            return "<clinit>";
        }
        String objectMethod = objectVirtual(name, sig);
        return objectMethod != null ? objectMethod : sanitize(name);
    }

    static String objectVirtual(String name, CilAssembly.MethodSig sig) {
        if (!sig.hasThis) {
            return null;
        }
        if ("ToString".equals(name) && sig.params.length == 0 && sig.returnType.kind == CilType.Kind.STRING) {
            return "toString";
        }
        if ("GetHashCode".equals(name) && sig.params.length == 0 && sig.returnType.kind == CilType.Kind.I4) {
            return "hashCode";
        }
        if ("Equals".equals(name) && sig.params.length == 1 && sig.params[0].kind == CilType.Kind.OBJECT
                && sig.returnType.kind == CilType.Kind.BOOLEAN) {
            return "equals";
        }
        return null;
    }
}
