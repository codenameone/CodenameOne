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
import com.codename1.cil.metadata.CilAssembly.FieldDef;
import com.codename1.cil.metadata.CilAssembly.FieldRef;
import com.codename1.cil.metadata.CilAssembly.MethodDef;
import com.codename1.cil.metadata.CilAssembly.MethodRef;
import com.codename1.cil.metadata.CilAssembly.TypeDef;
import com.codename1.cil.metadata.CilType;
import com.codename1.cil.metadata.MetadataReader;
import com.codename1.cil.metadata.MethodBody.Instr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// The static state of generic types.
///
/// Generics are erased, so ``Cache`1`` is one JVM class whatever it is a
/// cache of. C# gives every constructed type its own static fields and runs
/// the static constructor once for each: `Cache<int>.Value` and
/// `Cache<string>.Value` are two variables. One Java static for both hands
/// one manager the other's instance in the commonest shape there is,
/// `Singleton<T>`, and on ParparVM the cast that follows is unchecked.
///
/// So the static fields of a generic type are not Java statics. They are
/// the instance fields of a *context*: an object of the generated class
/// `Type$$S`, of which there is one per constructed type. Which one is
/// settled where the type arguments are known:
///
/// - Code that names a closed type -- `Cache<int>.Value` anywhere outside
///   the generic type -- reads the context from a static field of a class
///   generated for that one constructed type, `Type$$S$<arguments>`. That
///   class's own initializer makes the context and runs the C# static
///   constructor on it, so the JVM's once-per-class rule is .NET's
///   once-per-constructed-type rule and nothing is looked up at run time.
/// - A static method of the generic type takes its context as one more
///   parameter, after every other; so does a constructor, which keeps it in
///   a field of the object for the instance methods to read.
/// - Code that names a type built from its own type variables, as
///   `Outer<T>.Inner` naming `Outer<T>` does, asks its own context: the
///   context class declares one method per such type, and each
///   `Type$$S$<arguments>` answers it with the context of the type those
///   arguments make of it. A type that needs one for that reason alone has
///   a context too, with no fields in it.
/// - A generic *method* that names a type built from its own type variables
///   takes a context of its own in the same way (`Type$$M<n>`).
///
/// A context also answers `typeof(T)`, and the class a type variable
/// travels as to a generic method of the runtime, so `FindObjectOfType<T>()`
/// inside `Singleton<T>` finds a `T`. It answers, too, for the constructed
/// types its code builds from its own variables and that erasure would
/// otherwise make one: `typeof(List<T>)`, and the closed type of a
/// delegate it makes, `Action<T>` (`Delegate.$typeKey`). Each is a
/// constant of the `Type$$S$<arguments>` class, so making one costs no
/// more than a call. Code without a context still cannot tell them apart
/// (see `Delegate.$typeKey` for what that leaves).
///
/// None of this touches a type without type parameters, or a generic type
/// with no static state that names none that has: when no generic type of
/// the program declares a static field, nothing is even scanned.
///
/// What has no context is an error, never a shared variable: an instance
/// method of a generic *struct* (a struct is built without a constructor
/// call, so nothing could hand it one), and a virtual generic method that
/// would need a parameter its overrides and its interface do not have.
final class GenericStatics implements Opcodes {
    /// The static field of a `Type$$S$<arguments>` class that holds the context.
    static final String INSTANCE = "$I";
    /// How many contexts deep one constructed type may reach for another
    /// before the family is taken to be endless, as
    /// `class G<T> { static int n; int f() { return G<List<T>>.n; } }` is.
    private static final int DEPTH_LIMIT = 24;

    /// A reference to something that has a context: a generic type, or a
    /// generic method when `method` is set, with the arguments the
    /// reference gives it. Those may still name type variables of the code
    /// the reference is in.
    static final class Use {
        final TypeDef type;
        final MethodDef method;
        final CilType[] typeArgs;
        final CilType[] methodArgs;

        Use(TypeDef type, MethodDef method, CilType[] typeArgs, CilType[] methodArgs) {
            this.type = type;
            this.method = method;
            this.typeArgs = typeArgs == null ? new CilType[0] : typeArgs;
            this.methodArgs = methodArgs == null ? new CilType[0] : methodArgs;
        }

        /// True when an argument names a type variable of a method.
        boolean namesMethodVariable() {
            return mentions(typeArgs, CilType.Kind.MVAR) || mentions(methodArgs, CilType.Kind.MVAR);
        }

        /// True when an argument names any type variable: the context is
        /// not known until the code around it knows its own.
        boolean open() {
            return namesMethodVariable() || mentions(typeArgs, CilType.Kind.VAR)
                    || mentions(methodArgs, CilType.Kind.VAR);
        }

        Use substitute(CilType[] types, CilType[] methods) {
            return new Use(type, method, substitute(typeArgs, types, methods), substitute(methodArgs, types, methods));
        }

        private static CilType[] substitute(CilType[] in, CilType[] types, CilType[] methods) {
            CilType[] out = new CilType[in.length];
            for (int i = 0; i < in.length; i++) {
                out[i] = in[i].substitute(types, methods);
            }
            return out;
        }

        String arguments() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < typeArgs.length; i++) {
                sb.append(i > 0 ? "," : "").append(typeArgs[i]);
            }
            if (method != null) {
                sb.append(';');
                for (int i = 0; i < methodArgs.length; i++) {
                    sb.append(i > 0 ? "," : "").append(methodArgs[i]);
                }
            }
            return sb.toString();
        }

        String key() {
            return (method == null ? type.fullName() : method.toString()) + "<" + arguments() + ">";
        }

        /// As C# would write it, for a message.
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(type.fullName());
            sb.append('<');
            for (int i = 0; i < typeArgs.length; i++) {
                sb.append(i > 0 ? "," : "").append(typeArgs[i]);
            }
            sb.append('>');
            if (method != null) {
                sb.append("::").append(method.name).append('<');
                for (int i = 0; i < methodArgs.length; i++) {
                    sb.append(i > 0 ? "," : "").append(methodArgs[i]);
                }
                sb.append('>');
            }
            return sb.toString();
        }
    }

    private static boolean mentions(CilType[] types, CilType.Kind kind) {
        if (types != null) {
            for (CilType t : types) {
                if (mentions(t, kind)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// True when a type names a type variable of a method.
    static boolean namesMethodVariable(CilType type) {
        return mentions(type, CilType.Kind.MVAR);
    }

    private static boolean mentions(CilType type, CilType.Kind kind) {
        if (type == null) {
            return false;
        }
        return type.kind == kind || mentions(type.element, kind) || mentions(type.args, kind);
    }

    private final Translator host;
    private boolean analyzed;
    private final Set<TypeDef> keyedTypes = new HashSet<TypeDef>();
    private final Set<MethodDef> keyedMethods = new HashSet<MethodDef>();
    private final Map<Object, Map<String, Use>> dependencies = new HashMap<Object, Map<String, Use>>();
    private final Map<Object, Map<String, CilType>> openTypes = new HashMap<Object, Map<String, CilType>>();
    /// The arguments each generated `...$<arguments>` class stands for, to
    /// notice two that were given one name.
    private final Map<String, String> closedClasses = new HashMap<String, String>();
    private int depth;
    private Use outermost;

    GenericStatics(Translator host) {
        this.host = host;
    }

    // --------------------------------------------------------------- analysis

    private static boolean isStaticStorage(FieldDef f) {
        return f.isStatic() && !f.isLiteral() && f.rva == 0;
    }

    private static boolean hasStaticState(TypeDef type) {
        for (FieldDef f : type.fields) {
            if (isStaticStorage(f)) {
                return true;
            }
        }
        return staticConstructor(type) != null;
    }

    static MethodDef staticConstructor(TypeDef type) {
        for (MethodDef m : type.methods) {
            if (".cctor".equals(m.name)) {
                return m;
            }
        }
        return null;
    }

    /// The classes the C# compiler writes to keep delegates it made once:
    /// `<>c`, with the object every lambda that captures nothing is a
    /// method of, and `<>O`, with the delegates of static method groups.
    /// What they keep is the same for every constructed type unless the
    /// code around them has a context to tell them apart by, so by
    /// themselves they do not make a type one that needs a context -- or
    /// every generic class with a lambda in it would pay for one.
    private static boolean isDelegateCache(TypeDef type) {
        return (type.name.startsWith("<>c") && !type.name.startsWith("<>c__DisplayClass"))
                || type.name.startsWith("<>O");
    }

    private static boolean isCandidate(TypeDef type) {
        return type.genericParamCount > 0 && !type.isEnum() && !type.isDelegate() && !type.isInterface();
    }

    private void analyze() {
        if (analyzed) {
            return;
        }
        analyzed = true;
        List<TypeDef> types = new ArrayList<TypeDef>();
        List<MethodDef> methods = new ArrayList<MethodDef>();
        boolean any = false;
        for (CilAssembly assembly : host.universe.assemblies()) {
            for (TypeDef type : assembly.types()) {
                if (!host.names.isTranslated(type.fullName()) || type.isInterface()) {
                    continue;
                }
                if (isCandidate(type)) {
                    types.add(type);
                    if (hasStaticState(type) && !isDelegateCache(type)) {
                        keyedTypes.add(type);
                        any = true;
                    }
                }
                for (MethodDef m : type.methods) {
                    if (m.genericParamCount > 0 && m.hasBody()) {
                        methods.add(m);
                    }
                }
            }
        }
        if (!any) {
            return;
        }
        // Whatever names a type that has a context, through its own type
        // variables, needs a context to find it by: until nothing changes.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (TypeDef type : types) {
                if (keyedTypes.contains(type)) {
                    continue;
                }
                if (isDelegateCache(type) && hasStaticState(type) && type.enclosing != null
                        && (keyedTypes.contains(type.enclosing) || declaresKeyedMethod(type.enclosing))) {
                    keyedTypes.add(type);
                    changed = true;
                    continue;
                }
                for (MethodDef m : type.methods) {
                    if (namesOpenUse(m, false)) {
                        keyedTypes.add(type);
                        changed = true;
                        break;
                    }
                }
            }
            for (MethodDef m : methods) {
                if (!keyedMethods.contains(m) && namesOpenUse(m, true)) {
                    if (m.isVirtual() || m.isAbstract()) {
                        throw new TranslationException(m + ": a virtual generic method, or one that implements an"
                                + " interface, uses " + firstOpenUse(m, true) + ", whose static state belongs to"
                                + " each constructed type; the method would need a parameter its overrides do not"
                                + " have. Make the method non-virtual, or keep that state outside the generic type");
                    }
                    keyedMethods.add(m);
                    changed = true;
                }
            }
        }
    }

    private boolean declaresKeyedMethod(TypeDef type) {
        for (MethodDef m : type.methods) {
            if (keyedMethods.contains(m)) {
                return true;
            }
        }
        return false;
    }

    private boolean namesOpenUse(MethodDef m, boolean ofMethod) {
        return firstOpenUse(m, ofMethod) != null;
    }

    private Use firstOpenUse(MethodDef m, boolean ofMethod) {
        for (Use u : uses(m)) {
            if (u.open() && u.namesMethodVariable() == ofMethod && !isSelf(u, m.owner, ofMethod ? m : null)) {
                return u;
            }
        }
        return null;
    }

    /// Every reference in a method body to something that has a context.
    private List<Use> uses(MethodDef m) {
        List<Use> out = new ArrayList<Use>();
        if (!m.hasBody()) {
            return out;
        }
        CilAssembly assembly = m.owner.assembly;
        for (Instr ins : m.body().instructions) {
            String op = ins.op;
            if ("ldsfld".equals(op) || "ldsflda".equals(op) || "stsfld".equals(op)) {
                add(out, fieldUse0(assembly.fieldRef(ins.operand)));
            } else if ("call".equals(op) || "callvirt".equals(op) || "newobj".equals(op) || "ldftn".equals(op)
                    || "ldvirtftn".equals(op)) {
                MethodRef ref = assembly.methodRef(ins.operand);
                add(out, typeUse0(ref));
                add(out, methodUse0(ref));
            }
        }
        return out;
    }

    private static void add(List<Use> out, Use u) {
        if (u != null) {
            out.add(u);
        }
    }

    private static CilType[] identity(int count, boolean ofMethod) {
        CilType[] out = new CilType[count];
        for (int i = 0; i < count; i++) {
            out[i] = ofMethod ? CilType.mvar(i) : CilType.var(i);
        }
        return out;
    }

    private static boolean isIdentity(CilType[] args, CilType.Kind kind) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].kind != kind || args[i].index != i) {
                return false;
            }
        }
        return true;
    }

    /// True when a use inside `type` (and `method`, for a method's own
    /// context) names that very thing with its own variables: the context
    /// the code already has.
    boolean isSelf(Use u, TypeDef type, MethodDef method) {
        return u.type == type && u.method == method // NOPMD CompareObjectsWithEquals
                && isIdentity(u.typeArgs, CilType.Kind.VAR) && isIdentity(u.methodArgs, CilType.Kind.MVAR);
    }

    /// The arguments `target` has in a reference that names `from`, which
    /// is `target` or a type deriving from it: C# may name a subclass where
    /// it means an inherited static.
    private CilType[] argumentsOf(CilType from, TypeDef target) {
        CilType at = from;
        for (int i = 0; i < 64 && at != null; i++) {
            TypeDef def = host.universe.definitionOf(at);
            if (def == null) {
                break;
            }
            CilType[] args = at.kind == CilType.Kind.GENERICINST ? at.args : null;
            if (def == target) { // NOPMD CompareObjectsWithEquals
                return args != null ? args : identity(target.genericParamCount, false);
            }
            at = def.baseType == null ? null : def.baseType.substitute(args, null);
        }
        throw new TranslationException("cannot tell which " + target.fullName() + " a reference through " + from
                + " means");
    }

    private Use fieldUse0(FieldRef ref) {
        if (keyedTypes.isEmpty() || host.universe.definitionOf(ref.declaringType) == null) {
            return null;
        }
        FieldDef f = host.universe.resolve(ref);
        if (f == null || !isStaticStorage(f) || !keyedTypes.contains(f.owner)) {
            return null;
        }
        return new Use(f.owner, null, argumentsOf(ref.declaringType, f.owner), null);
    }

    private Use typeUse0(MethodRef ref) {
        if (keyedTypes.isEmpty() || host.universe.definitionOf(ref.declaringType) == null) {
            return null;
        }
        MethodDef m = host.universe.resolve(ref);
        if (m == null || !takesTypeContext(m)) {
            return null;
        }
        return new Use(m.owner, null, argumentsOf(ref.declaringType, m.owner), null);
    }

    private Use methodUse0(MethodRef ref) {
        if (keyedMethods.isEmpty() || host.universe.definitionOf(ref.declaringType) == null) {
            return null;
        }
        MethodDef m = host.universe.resolve(ref);
        if (m == null || !keyedMethods.contains(m)) {
            return null;
        }
        return new Use(m.owner, m, m.owner.genericParamCount == 0 ? null : argumentsOf(ref.declaringType, m.owner),
                ref.methodArgs != null ? ref.methodArgs : identity(m.genericParamCount, true));
    }

    // ---------------------------------------------------------------- queries

    /// True for a generic type whose static state is kept per constructed
    /// type, in a context.
    boolean hasContext(TypeDef type) {
        analyze();
        return keyedTypes.contains(type);
    }

    /// True for a generic method that takes a context of its own.
    boolean hasContext(MethodDef method) {
        analyze();
        return keyedMethods.contains(method);
    }

    /// True for a method that is handed its type's context as a parameter:
    /// a static method, and a constructor of a class.
    boolean takesTypeContext(MethodDef m) {
        analyze();
        return keyedTypes.contains(m.owner)
                && (m.isStatic() || (".ctor".equals(m.name) && !m.owner.isValueType()));
    }

    /// The context a static field lives in, or null for an ordinary static.
    Use fieldUse(FieldRef ref) {
        analyze();
        return fieldUse0(ref);
    }

    /// The context a call must pass for the type of the method it names:
    /// set for a static method or a constructor of a type that has one.
    Use typeUse(MethodRef ref) {
        analyze();
        return typeUse0(ref);
    }

    /// The context a call must pass for the generic method it names.
    Use methodUse(MethodRef ref) {
        analyze();
        return methodUse0(ref);
    }

    /// The class of a type's contexts.
    String contextClass(TypeDef type) {
        return host.names.className(type.fullName()) + "$$S";
    }

    /// The class of a generic method's contexts.
    String contextClass(MethodDef method) {
        return host.names.className(method.owner.fullName()) + "$$M" + method.owner.methods.indexOf(method);
    }

    String contextClass(Use u) {
        return u.method == null ? contextClass(u.type) : contextClass(u.method);
    }

    /// The field of an object of a generic class that holds its context.
    /// Named after the class, because a class deriving from it may have a
    /// context -- and such a field -- of its own.
    String contextField(TypeDef type) {
        return "$s$" + host.names.className(type.fullName()).replace('/', '_');
    }

    /// What the hidden parameters add to a method's descriptor: its type's
    /// context, then its own.
    String hiddenParameters(MethodDef m) {
        analyze();
        if (keyedTypes.isEmpty()) {
            return "";
        }
        String out = takesTypeContext(m) ? "L" + contextClass(m.owner) + ";" : "";
        return keyedMethods.contains(m) ? out + "L" + contextClass(m) + ";" : out;
    }

    static String withHidden(String descriptor, String hidden) {
        if (hidden.length() == 0) {
            return descriptor;
        }
        int at = descriptor.lastIndexOf(')');
        return descriptor.substring(0, at) + hidden + descriptor.substring(at);
    }

    /// The types built from a type's own variables that its code names:
    /// what its context has to be able to answer for.
    Map<String, Use> dependencies(TypeDef type) {
        Map<String, Use> found = dependencies.get(type);
        if (found == null) {
            found = new LinkedHashMap<String, Use>();
            for (MethodDef m : type.methods) {
                for (Use u : uses(m)) {
                    if (u.open() && !u.namesMethodVariable() && !isSelf(u, type, null)) {
                        found.put(u.key(), u);
                    }
                }
            }
            checkNames(found, type.fullName());
            dependencies.put(type, found);
        }
        return found;
    }

    Map<String, Use> dependencies(MethodDef method) {
        Map<String, Use> found = dependencies.get(method);
        if (found == null) {
            found = new LinkedHashMap<String, Use>();
            for (Use u : uses(method)) {
                if (u.namesMethodVariable() && !isSelf(u, method.owner, method)) {
                    found.put(u.key(), u);
                }
            }
            checkNames(found, method.toString());
            dependencies.put(method, found);
        }
        return found;
    }

    private static void checkNames(Map<String, Use> found, String where) {
        Set<String> names = new HashSet<String>();
        for (Use u : found.values()) {
            if (!names.add(getter(u))) {
                throw new TranslationException(where + ": two of the generic types it names have one name once"
                        + " shortened (" + getter(u) + ")");
            }
        }
    }

    /// The constructed types built from a type's own variables whose closed
    /// form its code needs at run time, by the method of its context that
    /// answers each: see [#keyGetter(CilType)] and [#typeOfGetter(CilType)].
    /// A type's own are those that name no variable of a method; a
    /// method's are every one its body names.
    Map<String, CilType> openTypes(TypeDef type) {
        Map<String, CilType> found = openTypes.get(type);
        if (found == null) {
            found = new LinkedHashMap<String, CilType>();
            for (MethodDef m : type.methods) {
                scanOpenTypes(m, false, found);
            }
            openTypes.put(type, found);
        }
        return found;
    }

    Map<String, CilType> openTypes(MethodDef method) {
        Map<String, CilType> found = openTypes.get(method);
        if (found == null) {
            found = new LinkedHashMap<String, CilType>();
            scanOpenTypes(method, true, found);
            openTypes.put(method, found);
        }
        return found;
    }

    private void scanOpenTypes(MethodDef m, boolean ofMethod, Map<String, CilType> out) {
        if (!m.hasBody()) {
            return;
        }
        CilAssembly assembly = m.owner.assembly;
        for (Instr ins : m.body().instructions) {
            CilType t = null;
            boolean delegate = false;
            if ("newobj".equals(ins.op)) {
                MethodRef ref = assembly.methodRef(ins.operand);
                TypeDef def = host.universe.definitionOf(ref.declaringType);
                if (def != null && def.isDelegate() && ref.declaringType.kind == CilType.Kind.GENERICINST) {
                    t = ref.declaringType;
                    delegate = true;
                }
            } else if ("ldtoken".equals(ins.op)
                    && MetadataReader.tokenTable(ins.operand) == MetadataReader.TYPE_SPEC) {
                CilType x = assembly.typeFromToken(ins.operand);
                if (x.kind == CilType.Kind.GENERICINST) {
                    t = x;
                }
            }
            if (t == null || !(mentions(t, CilType.Kind.VAR) || mentions(t, CilType.Kind.MVAR))
                    || !ofMethod && mentions(t, CilType.Kind.MVAR)) {
                continue;
            }
            out.put(delegate ? keyGetter(t) : typeOfGetter(t), t);
        }
    }

    /// The method of a context that answers the closed type of a delegate
    /// type its code makes, as the string constant `Delegate.$typeKey`
    /// holds -- the one code naming the closed type writes, so the two
    /// are one interned object.
    static String keyGetter(CilType open) {
        return "$k" + mangle(open.toString());
    }

    /// The method of a context that answers `typeof` of a constructed type
    /// its code names.
    static String typeOfGetter(CilType open) {
        return "$y" + mangle(open.toString());
    }

    static String openTypeDescriptor(String getter) {
        return getter.startsWith("$k") ? "()Ljava/lang/Object;" : "()" + Translator.TYPE_DESCRIPTOR;
    }

    /// The method of a context that answers for a type its code names.
    static String getter(Use u) {
        return "$d" + mangle(u.key());
    }

    /// The method of a type's context that answers `typeof` of a type
    /// argument; of a method's context, of one of the method's own.
    static String typeGetter(int index) {
        return "$t" + index;
    }

    /// A name that is legal in a class file and still says something, with
    /// the hash of the whole text after it: sanitizing alone would give
    /// `A.B` and `A_B` one name.
    private static String mangle(String text) {
        String plain = Names.sanitize(text);
        int from = Math.max(0, plain.length() - 40);
        return plain.substring(from) + "_" + Integer.toHexString(text.hashCode());
    }

    // --------------------------------------------------------------- emission

    /// The class of a generic type's contexts: its static fields as
    /// instance fields, and a question for each thing a context has to
    /// answer.
    void emitContextClass(TypeDef type) {
        String name = contextClass(type);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_5, ACC_PUBLIC | ACC_ABSTRACT | ACC_SUPER, name, null, Names.OBJECT, null);
        for (FieldDef f : type.fields) {
            if (isStaticStorage(f)) {
                cw.visitField(ACC_PUBLIC, Names.sanitize(f.name), host.names.descriptor(f.type), null, null)
                        .visitEnd();
            }
        }
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, Names.OBJECT, "<init>", "()V", false);
        for (FieldDef f : type.fields) {
            // A field of struct type always holds an object, as it always
            // holds a value in C#.
            if (isStaticStorage(f) && host.names.isStruct(f.type)) {
                String c = host.names.classRef(f.type);
                host.index.requireMethod(c, "<init>", "()V", type.fullName());
                mv.visitVarInsn(ALOAD, 0);
                mv.visitTypeInsn(NEW, c);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, c, "<init>", "()V", false);
                mv.visitFieldInsn(PUTFIELD, name, Names.sanitize(f.name), host.names.descriptor(f.type));
            }
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        emitQuestions(cw, type.genericParamCount, dependencies(type), openTypes(type));
        cw.visitEnd();
        host.putClass(name, cw.toByteArray());
    }

    void emitContextClass(MethodDef method) {
        String name = contextClass(method);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_5, ACC_PUBLIC | ACC_ABSTRACT | ACC_SUPER, name, null, Names.OBJECT, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, Names.OBJECT, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        emitQuestions(cw, method.genericParamCount, dependencies(method), openTypes(method));
        cw.visitEnd();
        host.putClass(name, cw.toByteArray());
    }

    private void emitQuestions(ClassWriter cw, int typeParameters, Map<String, Use> uses,
            Map<String, CilType> open) {
        for (String g : open.keySet()) {
            cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, g, openTypeDescriptor(g), null, null).visitEnd();
        }
        for (int i = 0; i < typeParameters; i++) {
            cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, typeGetter(i), "()" + Translator.TYPE_DESCRIPTOR, null, null)
                    .visitEnd();
        }
        for (Use u : uses.values()) {
            cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, getter(u), "()L" + contextClass(u) + ";", null, null)
                    .visitEnd();
        }
    }

    /// The class that holds the one context of a constructed type, or of a
    /// constructed generic method, written the first time something names
    /// it. Its initializer is where the C# static constructor runs.
    String closedClass(Use u, String neededBy) {
        if (u.open()) {
            throw new TranslationException(neededBy + ": " + u + " still names a type variable");
        }
        String base = contextClass(u);
        String arguments = u.arguments();
        String name = base + "$" + mangle(arguments);
        String known = closedClasses.get(name);
        if (known != null) {
            if (!known.equals(arguments)) {
                throw new TranslationException(neededBy + ": " + u + " and the same type of " + known
                        + " have one class name once shortened (" + name + ")");
            }
            return name;
        }
        closedClasses.put(name, arguments);
        if (depth == 0) {
            outermost = u;
        }
        depth++;
        if (depth > DEPTH_LIMIT) {
            throw new TranslationException(neededBy + ": " + outermost + " reaches an endless family of"
                    + " constructed types (" + u.type.fullName() + " names itself with a larger type argument"
                    + " each time), and one of them has static state; a variable per constructed type cannot be"
                    + " laid out ahead of time for a family with no end. Keep that state outside the generic"
                    + " type");
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_5, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, name, null, base, null);
        String descriptor = "L" + base + ";";
        cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, INSTANCE, descriptor, null, null).visitEnd();

        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, base, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // The context exists before the static constructor runs: that
        // constructor is handed it, and whatever it calls that names this
        // constructed type reads the field and finds it.
        mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, name);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, name, INSTANCE, descriptor);
        if (u.method == null && staticConstructor(u.type) != null) {
            mv.visitFieldInsn(GETSTATIC, name, INSTANCE, descriptor);
            mv.visitMethodInsn(INVOKESTATIC, host.names.className(u.type.fullName()), STATIC_CONSTRUCTOR,
                    "(" + descriptor + ")V", false);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        CilType[] own = u.method == null ? u.typeArgs : u.methodArgs;
        for (int i = 0; i < own.length; i++) {
            mv = cw.visitMethod(ACC_PUBLIC, typeGetter(i), "()" + Translator.TYPE_DESCRIPTOR, null, null);
            mv.visitCode();
            host.emitTypeOf(mv, own[i], neededBy);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        Map<String, CilType> types = u.method == null ? openTypes(u.type) : openTypes(u.method);
        for (Map.Entry<String, CilType> e : types.entrySet()) {
            CilType closed = e.getValue().substitute(u.typeArgs, u.methodArgs);
            mv = cw.visitMethod(ACC_PUBLIC, e.getKey(), openTypeDescriptor(e.getKey()), null, null);
            mv.visitCode();
            if (e.getKey().startsWith("$k")) {
                mv.visitLdcInsn(closed.toString());
            } else {
                host.emitTypeOf(mv, closed, neededBy);
            }
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        Map<String, Use> uses = u.method == null ? dependencies(u.type) : dependencies(u.method);
        for (Use open : uses.values()) {
            Use closed = open.substitute(u.typeArgs, u.methodArgs);
            String d = "L" + contextClass(closed) + ";";
            mv = cw.visitMethod(ACC_PUBLIC, getter(open), "()" + d, null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, closedClass(closed, neededBy), INSTANCE, d);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        host.putClass(name, cw.toByteArray());
        depth--;
        return name;
    }

    /// What the static constructor of a type with a context is called: it
    /// is an ordinary static method that takes the context, run by each
    /// constructed type's class and not by the erased class's own
    /// initializer, which would run it once for all of them.
    static final String STATIC_CONSTRUCTOR = "$cctor";
}
