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
package com.codename1.tools.javac;

import com.codename1.tools.javac.Token.Kind;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Attribution: resolves every name, types every expression, selects every method
 * and infers generic type arguments, recording on the tree what {@link Gen} needs.
 */
final class Attr {
    final Compiler compiler;
    final Symtab syms;
    final Types types;

    Attr(Compiler compiler) {
        this.compiler = compiler;
        this.syms = compiler.symtab;
        this.types = compiler.types;
    }

    private void error(Env env, int pos, String message) {
        compiler.error(env.unit, pos, message);
    }

    // ================================================================== type resolution

    Type attribType(Tree t, Env env) {
        Type result = attribTypeInner(t, env);
        t.type = result;
        return result;
    }

    private Type attribTypeInner(Tree t, Env env) {
        if (t == null) {
            return syms.objectType;
        }
        if (t instanceof Tree.ResolvedType) {
            return t.type;
        }
        if (t instanceof Tree.PrimitiveTypeTree) {
            return primitive(((Tree.PrimitiveTypeTree) t).tag);
        }
        if (t instanceof Tree.ArrayTypeTree) {
            Type elem = attribType(((Tree.ArrayTypeTree) t).elem, env);
            return new Type.ArrayType(elem);
        }
        if (t instanceof Tree.Wildcard) {
            Tree.Wildcard w = (Tree.Wildcard) t;
            if (w.boundKind == null) {
                return new Type.WildcardType(Type.WildcardType.UNBOUND, null);
            }
            Type b = attribType(w.bound, env);
            return new Type.WildcardType(w.boundKind == Kind.EXTENDS ? Type.WildcardType.EXTENDS
                    : Type.WildcardType.SUPER, b);
        }
        if (t instanceof Tree.IntersectionTypeTree) {
            List<Type> bounds = new ArrayList<Type>();
            for (Tree b : ((Tree.IntersectionTypeTree) t).bounds) {
                bounds.add(attribType(b, env));
            }
            return new Type.IntersectionType(bounds);
        }
        if (t instanceof Tree.TypeApply) {
            Tree.TypeApply ta = (Tree.TypeApply) t;
            Type base = attribType(ta.clazz, env);
            if (base.tag != Type.Tag.CLASS) {
                return base;
            }
            Type.ClassType ct = (Type.ClassType) base;
            if (ta.args.isEmpty()) {
                // Diamond: arguments are inferred at the instance creation.
                return ct;
            }
            List<Type> args = new ArrayList<Type>();
            for (Tree a : ta.args) {
                Type at = attribType(a, env);
                if (at.isPrimitive()) {
                    error(env, a.pos, "type argument cannot be of primitive type");
                    at = Type.ERROR;
                }
                args.add(at);
            }
            if (ct.sym.typeParams().size() != args.size()) {
                error(env, t.pos, "wrong number of type arguments; required " + ct.sym.typeParams().size());
                return ct.sym.erasure();
            }
            return new Type.ClassType(ct.sym, args, ct.outer);
        }
        if (t instanceof Tree.Ident) {
            String name = ((Tree.Ident) t).name;
            Type tv = findTypeVar(name, env);
            if (tv != null) {
                return tv;
            }
            ClassSymbol c = resolveClassName(name, env);
            if (c == null) {
                error(env, t.pos, "cannot find symbol\n  symbol: class " + name);
                return Type.ERROR;
            }
            ((Tree.Ident) t).sym = c;
            return classTypeIn(c, env);
        }
        if (t instanceof Tree.Select) {
            Tree.Select sel = (Tree.Select) t;
            Object q = resolveQualifier(sel.selected, env);
            if (q instanceof String) {
                String pkg = (String) q;
                ClassSymbol c = syms.lookup(pkg.isEmpty() ? sel.name : pkg + "/" + sel.name);
                if (c == null) {
                    error(env, t.pos, "cannot find symbol\n  symbol: class " + sel.name + "\n  location: package " + pkg.replace('/', '.'));
                    return Type.ERROR;
                }
                sel.sym = c;
                return c.erasure();
            }
            if (q instanceof Type) {
                Type outerType = (Type) q;
                if (outerType.tag != Type.Tag.CLASS) {
                    return Type.ERROR;
                }
                ClassSymbol member = findMemberClass(((Type.ClassType) outerType).sym, sel.name);
                if (member == null) {
                    error(env, t.pos, "cannot find symbol\n  symbol: class " + sel.name + "\n  location: " + outerType);
                    return Type.ERROR;
                }
                sel.sym = member;
                Type.ClassType outerCt = (Type.ClassType) outerType;
                return new Type.ClassType(member, new ArrayList<Type>(),
                        member.hasOuterInstance && outerCt.isParameterized() ? outerCt : null);
            }
            error(env, t.pos, "cannot find symbol\n  symbol: class " + sel.name);
            return Type.ERROR;
        }
        error(env, t.pos, "illegal type");
        return Type.ERROR;
    }

    /** The type a class name denotes in env: a generic inner class sees its outer's arguments. */
    private Type classTypeIn(ClassSymbol c, Env env) {
        return c.erasure();
    }

    static Type primitive(Kind k) {
        switch (k) {
            case BOOLEAN: return Type.BOOLEAN;
            case BYTE: return Type.BYTE;
            case SHORT: return Type.SHORT;
            case CHAR: return Type.CHAR;
            case INT: return Type.INT;
            case LONG: return Type.LONG;
            case FLOAT: return Type.FLOAT;
            case DOUBLE: return Type.DOUBLE;
            default: return Type.VOID;
        }
    }

    Type findTypeVar(String name, Env env) {
        for (Env e = env; e != null; e = e.outer) {
            if (e.enclMethod != null) {
                for (Type.TypeVar tv : e.enclMethod.typeParams) {
                    if (tv.name.equals(name)) {
                        return tv;
                    }
                }
            }
            if (e.classBoundary) {
                for (Type.TypeVar tv : e.enclClass.typeParams()) {
                    if (tv.name.equals(name)) {
                        return tv;
                    }
                }
            }
        }
        return null;
    }

    /** Resolves a simple class name in env, or null. */
    ClassSymbol resolveClassName(String name, Env env) {
        // Local classes in enclosing scopes.
        for (Env e = env; e != null; e = e.outer) {
            ClassSymbol local = e.localClassHere(name);
            if (local != null) {
                return local;
            }
        }
        // Member classes of enclosing classes (and their supertypes), innermost first.
        for (ClassSymbol c = env.enclClass; c != null; c = c.outer) {
            if (c.simpleName.equals(name) && !c.anonymous) {
                return c;
            }
            ClassSymbol member = findMemberClass(c, name);
            if (member != null) {
                return member;
            }
        }
        return resolveTopLevelName(name, env.unit);
    }

    ClassSymbol resolveTopLevelName(String name, Tree.CompilationUnit unit) {
        if (unit != null) {
            // Single-type imports.
            for (Tree.Import imp : unit.imports) {
                if (!imp.onDemand && !imp.isStatic && imp.name.endsWith("." + name) || !imp.onDemand && !imp.isStatic && imp.name.equals(name)) {
                    ClassSymbol c = classByQualifiedName(imp.name);
                    if (c != null) {
                        return c;
                    }
                }
                if (!imp.onDemand && imp.isStatic && imp.name.endsWith("." + name)) {
                    // import static a.B.Member: a member class named like this.
                    String owner = imp.name.substring(0, imp.name.length() - name.length() - 1);
                    ClassSymbol oc = classByQualifiedName(owner);
                    if (oc != null) {
                        ClassSymbol m = findMemberClass(oc, name);
                        if (m != null) {
                            return m;
                        }
                    }
                }
            }
            // Same package.
            String pkg = unit.packageName.replace('.', '/');
            ClassSymbol same = syms.lookup(pkg.isEmpty() ? name : pkg + "/" + name);
            if (same != null) {
                return same;
            }
            // On-demand imports. javac rejects a name two of them supply; a script also
            // gets default imports it never wrote, so pick instead: java.* first
            // (List means java.util.List, not com.codename1.ui.List), then import order.
            ClassSymbol javaMatch = null;
            ClassSymbol firstMatch = null;
            for (Tree.Import imp : unit.imports) {
                if (!imp.onDemand) {
                    continue;
                }
                ClassSymbol c = onDemandMatch(imp, name);
                if (c != null) {
                    if (firstMatch == null) {
                        firstMatch = c;
                    }
                    if (javaMatch == null && c.internalName.startsWith("java/")) {
                        javaMatch = c;
                    }
                }
            }
            if (javaMatch != null) {
                return javaMatch;
            }
            if (firstMatch != null) {
                return firstMatch;
            }
        }
        return syms.lookup("java/lang/" + name);
    }

    /** The class an on-demand import supplies for {@code name}: a member class, or a package's class. */
    private ClassSymbol onDemandMatch(Tree.Import imp, String name) {
        ClassSymbol owner = classByQualifiedName(imp.name);
        if (owner != null) {
            ClassSymbol m = findMemberClass(owner, name);
            if (m != null || imp.isStatic) {
                return m;
            }
        }
        if (imp.isStatic) {
            return null;
        }
        return syms.lookup(imp.name.replace('.', '/') + "/" + name);
    }

    /** {@code a.b.C.D}: the class it names (nested classes resolved through their outer class), or null. */
    ClassSymbol classByQualifiedName(String qualified) {
        String internal = qualified.replace('.', '/');
        ClassSymbol c = syms.lookup(internal);
        if (c != null) {
            return c;
        }
        int dot = qualified.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        ClassSymbol outer = classByQualifiedName(qualified.substring(0, dot));
        return outer == null ? null : findMemberClass(outer, qualified.substring(dot + 1));
    }

    /** A member class of c or any of its supertypes. */
    ClassSymbol findMemberClass(ClassSymbol c, String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<ClassSymbol>();
        return findMemberClass(c, name, seen);
    }

    private ClassSymbol findMemberClass(ClassSymbol c, String name, Set<ClassSymbol> seen) {
        if (c == null || !seen.add(c)) {
            return null;
        }
        ClassSymbol m = c.memberClasses().get(name);
        if (m != null) {
            return m;
        }
        Type sup = c.superclass();
        if (sup != null && sup.tag == Type.Tag.CLASS) {
            m = findMemberClass(((Type.ClassType) sup).sym, name, seen);
            if (m != null) {
                return m;
            }
        }
        for (Type i : c.interfaces()) {
            if (i.tag == Type.Tag.CLASS) {
                m = findMemberClass(((Type.ClassType) i).sym, name, seen);
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * What a qualifier names: a {@link Type} (a class), a {@link String} (a package,
     * internal form), or null when it is an expression.
     */
    /**
     * Reports a member's qualifier that names nothing, as javac does: a simple name
     * is an unknown variable ({@code Rest.get()} without the import), and a qualified
     * one is a member of a package that does not exist -- the qualifier's own
     * qualifier ({@code a.b.C.x} reports package {@code a.b}), at the dot before C.
     */
    private void unresolvedQualifier(Tree qual, String pkg, Env env) {
        if (qual instanceof Tree.Ident) {
            error(env, qual.pos, "cannot find symbol\n  symbol:   variable " + ((Tree.Ident) qual).name
                    + "\n  location: class " + env.enclClass.javaName());
            return;
        }
        int slash = pkg.lastIndexOf('/');
        String owner = slash < 0 ? pkg : pkg.substring(0, slash);
        int pos = qual instanceof Tree.Select && ((Tree.Select) qual).dotPos >= 0 ? ((Tree.Select) qual).dotPos : qual.pos;
        error(env, pos, "package " + owner.replace('/', '.') + " does not exist");
    }

    Object resolveQualifier(Tree t, Env env) {
        if (t instanceof Tree.Ident) {
            String name = ((Tree.Ident) t).name;
            if (name.equals("this") || name.equals("super")) {
                return null;
            }
            if (findVariable(name, env, false, t.pos) != null) {
                return null;
            }
            Type tv = findTypeVar(name, env);
            if (tv != null) {
                return tv;
            }
            ClassSymbol c = resolveClassName(name, env);
            if (c != null) {
                ((Tree.Ident) t).sym = c;
                t.type = c.erasure();
                return c.erasure();
            }
            return name;
        }
        if (t instanceof Tree.Select) {
            Tree.Select sel = (Tree.Select) t;
            if (sel.name.equals("this") || sel.name.equals("super") || sel.name.equals("class")) {
                return null;
            }
            Object q = resolveQualifier(sel.selected, env);
            if (q instanceof String) {
                String pkg = (String) q;
                String internal = pkg + "/" + sel.name;
                ClassSymbol c = syms.lookup(internal);
                if (c != null) {
                    sel.sym = c;
                    sel.staticRef = true;
                    t.type = c.erasure();
                    return c.erasure();
                }
                return internal;
            }
            if (q instanceof Type) {
                Type qt = (Type) q;
                if (qt.tag == Type.Tag.CLASS) {
                    ClassSymbol owner = ((Type.ClassType) qt).sym;
                    // A static field takes precedence over a member class in expression context.
                    if (findField(qt, sel.name) != null) {
                        return null;
                    }
                    ClassSymbol member = findMemberClass(owner, sel.name);
                    if (member != null) {
                        sel.sym = member;
                        sel.staticRef = true;
                        t.type = member.erasure();
                        return member.erasure();
                    }
                }
                return null;
            }
            return null;
        }
        if (t instanceof Tree.ResolvedType) {
            return t.type;
        }
        if (t instanceof Tree.TypeApply || t instanceof Tree.ArrayTypeTree || t instanceof Tree.PrimitiveTypeTree) {
            return attribType(t, env);
        }
        return null;
    }

    // ================================================================== variables and fields

    /** A field visible in type {@code site}: declared there or inherited. */
    VarSymbol findField(Type site, String name) {
        Type e = types.erased(site);
        if (e.tag != Type.Tag.CLASS) {
            return null;
        }
        return findField(((Type.ClassType) e).sym, name, new LinkedHashSet<ClassSymbol>());
    }

    private VarSymbol findField(ClassSymbol c, String name, Set<ClassSymbol> seen) {
        if (c == null || !seen.add(c)) {
            return null;
        }
        for (VarSymbol f : c.fields()) {
            if (f.name.equals(name)) {
                return f;
            }
        }
        for (Type i : c.interfaces()) {
            if (i.tag == Type.Tag.CLASS) {
                VarSymbol f = findField(((Type.ClassType) i).sym, name, seen);
                if (f != null) {
                    return f;
                }
            }
        }
        Type sup = c.superclass();
        if (sup != null && sup.tag == Type.Tag.CLASS) {
            return findField(((Type.ClassType) sup).sym, name, seen);
        }
        return null;
    }

    /** Result of resolving a simple variable name. */
    static final class VarRef {
        VarSymbol sym;
        /** For a field: the enclosing class whose instance (or static scope) holds it. */
        ClassSymbol site;
        /** An instance field reached from a context with no instance of {@link #site}. */
        boolean staticContext;
    }

    /**
     * Resolves a simple name to a local, a field of an enclosing class, or a
     * statically imported field. When {@code record} is set, a local reached
     * across a lambda or local-class boundary is recorded as captured.
     */
    VarRef findVariable(String name, Env env, boolean record, int pos) {
        List<Env> boundaries = new ArrayList<Env>();
        boolean noInstance = false;
        for (Env e = env; e != null; e = e.outer) {
            VarSymbol local = e.localHere(name);
            if (local != null) {
                if (record && !boundaries.isEmpty()) {
                    for (Env b : boundaries) {
                        if (b.lambda != null) {
                            if (b.lambda.captured == null) {
                                b.lambda.captured = new ArrayList<VarSymbol>();
                            }
                            if (!b.lambda.captured.contains(local)) {
                                b.lambda.captured.add(local);
                            }
                        } else if (b.classBoundary && (b.enclClass.local || b.enclClass.anonymous)) {
                            if (!b.enclClass.capturedVars.contains(local)) {
                                b.enclClass.capturedVars.add(local);
                            }
                        }
                    }
                    compiler.recordCapture(local, env.unit, pos, boundaries.get(0).lambda != null);
                }
                VarRef r = new VarRef();
                r.sym = local;
                return r;
            }
            if (e.isStatic) {
                noInstance = true;
            }
            if (e.lambda != null || e.classBoundary) {
                boundaries.add(e);
            }
            if (e.classBoundary) {
                VarSymbol f = findField(e.enclClass.thisType(), name);
                if (f != null) {
                    VarRef r = new VarRef();
                    r.sym = f;
                    r.site = e.enclClass;
                    r.staticContext = !f.isStatic() && noInstance;
                    if (record && !f.isStatic() && !noInstance) {
                        markThisCapture(env);
                    }
                    return r;
                }
                if (!e.enclClass.hasOuterInstance && !(e.enclClass.local || e.enclClass.anonymous)
                        || (e.enclClass.local || e.enclClass.anonymous) && !e.enclClass.hasOuterInstance) {
                    noInstance = true;
                }
            }
        }
        VarSymbol imported = findStaticImportField(name, env.unit);
        if (imported != null) {
            VarRef r = new VarRef();
            r.sym = imported;
            r.site = imported.owner;
            return r;
        }
        return null;
    }

    /** Using {@code this} (directly or through an outer instance) from env: lambdas in between capture it. */
    void markThisCapture(Env env) {
        for (Env e = env; e != null; e = e.outer) {
            if (e.lambda != null) {
                e.lambda.capturesThis = true;
            }
            if (e.classBoundary) {
                return;
            }
        }
    }

    private VarSymbol findStaticImportField(String name, Tree.CompilationUnit unit) {
        if (unit == null) {
            return null;
        }
        for (Tree.Import imp : unit.imports) {
            if (!imp.isStatic) {
                continue;
            }
            if (!imp.onDemand && imp.name.endsWith("." + name)) {
                ClassSymbol owner = classByQualifiedName(imp.name.substring(0, imp.name.length() - name.length() - 1));
                if (owner != null) {
                    VarSymbol f = findField(owner.erasure(), name);
                    if (f != null && f.isStatic()) {
                        return f;
                    }
                }
            }
        }
        for (Tree.Import imp : unit.imports) {
            if (imp.isStatic && imp.onDemand) {
                ClassSymbol owner = classByQualifiedName(imp.name);
                if (owner != null) {
                    VarSymbol f = findField(owner.erasure(), name);
                    if (f != null && f.isStatic()) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    // ================================================================== classes and methods

    /** Switch kinds recorded on {@link Tree.Switch#switchKind}. */
    static final int SW_INT = 1;
    static final int SW_STRING = 2;
    static final int SW_ENUM = 3;
    static final int SW_PATTERN = 4;

    /** The env of a class body: sees the enclosing scope (outer class, or a local class's method). */
    Env classEnv(ClassSymbol c) {
        Env outerEnv = c.declEnv != null ? c.declEnv : c.outer != null ? classEnv(c.outer) : null;
        Env e = new Env(outerEnv, c.unit, c, null);
        e.classBoundary = true;
        return e;
    }

    private Env initializerEnv(Env cenv, boolean isStatic) {
        Env e = new Env(cenv, cenv.unit, cenv.enclClass, null);
        e.isStatic = isStatic;
        e.methodBoundary = true;
        return e;
    }

    void attribClass(ClassSymbol c) {
        if (c.attributed) {
            return;
        }
        c.attributed = true;
        Tree.ClassDecl decl = c.decl;
        Env cenv = classEnv(c);
        checkClassHeader(c, cenv);
        for (Tree member : decl.members) {
            if (member instanceof Tree.VarDef) {
                Tree.VarDef v = (Tree.VarDef) member;
                VarSymbol f = v.sym;
                if (f == null) {
                    continue;
                }
                Env env = initializerEnv(cenv, f.isStatic());
                if ((f.flags & Symbol.ACC_ENUM) != 0) {
                    attribEnumConstant(c, v, env);
                } else if (v.init != null) {
                    currentFieldInit = v;
                    Type t;
                    try {
                        t = attribExpr(v.init, env, f.type);
                    } finally {
                        currentFieldInit = null;
                    }
                    checkAssignable(v.init, t, f.type, env);
                    if (f.constState == 0) {
                        f.constState = 2;
                        if (f.isFinal() && v.init.constant != null && isConstantType(f.type)) {
                            f.constValue = convertConstant(v.init.constant, f.type);
                        }
                    }
                }
            } else if (member instanceof Tree.MethodDecl) {
                attribMethod(((Tree.MethodDecl) member).sym, cenv);
            } else if (member instanceof Tree.Block) {
                Tree.Block b = (Tree.Block) member;
                Env env = initializerEnv(cenv, b.isStatic);
                attribStat(b, env);
            }
        }
        for (MethodSymbol m : c.methods) {
            if ((m.flags & Enter.SYNTH_DEFAULT_CTOR) != 0 && !c.isEnum() && !c.anonymous) {
                m.superCtor = resolveImplicitSuper(c, cenv, decl.pos);
            }
        }
        if (!c.isInterface() && !c.isAbstract() && !(c.isEnum() && !c.isFinal())) {
            checkAbstractImplemented(c, cenv);
        }
        for (ClassSymbol member : c.memberClasses.values()) {
            attribClass(member);
        }
    }

    private void checkClassHeader(ClassSymbol c, Env cenv) {
        Type sup = c.superclass();
        if (sup != null && sup.tag == Type.Tag.CLASS) {
            ClassSymbol s = ((Type.ClassType) sup).sym;
            if ((s.flags & Symbol.SEALED) != 0 && !permits(s, c)) {
                error(cenv, c.decl.pos, "class is not allowed to extend sealed class: " + s.javaName()
                        + " (as it is not listed in its 'permits' clause)");
            }
        }
        for (Type i : c.interfaces()) {
            if (i.tag == Type.Tag.CLASS) {
                ClassSymbol s = ((Type.ClassType) i).sym;
                if ((s.flags & Symbol.SEALED) != 0 && !permits(s, c)) {
                    error(cenv, c.decl.pos, "class is not allowed to extend sealed class: " + s.javaName()
                            + " (as it is not listed in its 'permits' clause)");
                }
            }
        }
    }

    /** The permitted direct subclasses of a sealed class: its permits clause, or its compilation unit's subclasses. */
    List<ClassSymbol> permittedSubclasses(ClassSymbol s) {
        if (!s.permitted.isEmpty() || s.decl == null) {
            return s.permitted;
        }
        List<ClassSymbol> out = new ArrayList<ClassSymbol>();
        for (ClassSymbol c : compiler.enter.sourceClasses) {
            if (c.unit != s.unit || c == s) {
                continue;
            }
            boolean direct = false;
            Type sup = c.superclass();
            if (sup != null && sup.tag == Type.Tag.CLASS && ((Type.ClassType) sup).sym == s) {
                direct = true;
            }
            for (Type i : c.interfaces()) {
                if (i.tag == Type.Tag.CLASS && ((Type.ClassType) i).sym == s) {
                    direct = true;
                }
            }
            if (direct) {
                out.add(c);
            }
        }
        s.permitted.addAll(out);
        return s.permitted;
    }

    private boolean permits(ClassSymbol sealed, ClassSymbol c) {
        if (c.anonymous && sealed.isEnum()) {
            return true;
        }
        return permittedSubclasses(sealed).contains(c);
    }

    private void checkAbstractImplemented(ClassSymbol c, Env cenv) {
        List<MethodSymbol> abstracts = new ArrayList<MethodSymbol>();
        collectAbstractMethods(c.thisType(), abstracts, new LinkedHashSet<ClassSymbol>());
        for (MethodSymbol m : abstracts) {
            MethodSymbol impl = findImplementation(c, m);
            if (impl == null) {
                String where = c.anonymous ? "<anonymous " + c.internalName.replace('/', '.').replace('$', '$') + ">" : c.simpleName;
                error(cenv, c.decl.pos, where + " is not abstract and does not override abstract method "
                        + m + " in " + m.owner.javaName());
                return;
            }
        }
    }

    private void collectAbstractMethods(Type t, List<MethodSymbol> out, Set<ClassSymbol> seen) {
        Type e = types.erased(t);
        if (e.tag != Type.Tag.CLASS) {
            return;
        }
        ClassSymbol c = ((Type.ClassType) e).sym;
        if (!seen.add(c)) {
            return;
        }
        for (MethodSymbol m : c.methods()) {
            if (m.isAbstract() && !m.isStatic()) {
                out.add(m);
            }
        }
        if (c.superclass() != null) {
            collectAbstractMethods(c.superclass(), out, seen);
        }
        for (Type i : c.interfaces()) {
            collectAbstractMethods(i, out, seen);
        }
    }

    /** A concrete method of c (or inherited, or an interface default) with m's erased signature. */
    private MethodSymbol findImplementation(ClassSymbol c, MethodSymbol m) {
        Set<ClassSymbol> seen = new LinkedHashSet<ClassSymbol>();
        for (ClassSymbol k = c; k != null; ) {
            for (MethodSymbol x : k.methods()) {
                if (x.name.equals(m.name) && !x.isAbstract() && !x.isStatic() && sameErasedParamsOrGeneric(x, m, c)) {
                    return x;
                }
            }
            Type sup = k.superclass();
            k = sup != null && sup.tag == Type.Tag.CLASS ? ((Type.ClassType) sup).sym : null;
        }
        // Interface defaults.
        List<ClassSymbol> itfs = new ArrayList<ClassSymbol>();
        for (ClassSymbol k = c; k != null; ) {
            collectInterfaceSyms(k, itfs, seen);
            Type sup = k.superclass();
            k = sup != null && sup.tag == Type.Tag.CLASS ? ((Type.ClassType) sup).sym : null;
        }
        for (ClassSymbol i : itfs) {
            for (MethodSymbol x : i.methods()) {
                if (x.name.equals(m.name) && (x.flags & Symbol.DEFAULT_METHOD) != 0 && sameErasedParamsOrGeneric(x, m, c)) {
                    return x;
                }
            }
        }
        return null;
    }

    private void collectInterfaceSyms(ClassSymbol k, List<ClassSymbol> out, Set<ClassSymbol> seen) {
        for (Type i : k.interfaces()) {
            if (i.tag == Type.Tag.CLASS && seen.add(((Type.ClassType) i).sym)) {
                out.add(((Type.ClassType) i).sym);
                collectInterfaceSyms(((Type.ClassType) i).sym, out, seen);
            }
        }
    }

    private String erasedParams(MethodSymbol m) {
        StringBuilder b = new StringBuilder();
        for (Type p : m.params) {
            b.append(Types.descriptor(types.erased(p)));
        }
        return b.toString();
    }

    /** Same parameters as seen from class c (generic supertypes substituted), or same erasure. */
    private boolean sameErasedParamsOrGeneric(MethodSymbol impl, MethodSymbol abs, ClassSymbol c) {
        if (impl.params.size() != abs.params.size()) {
            return false;
        }
        if (erasedParams(impl).equals(erasedParams(abs))) {
            return true;
        }
        java.util.Map<Type.TypeVar, Type> absMap = types.memberMapping(c.thisType(), abs.owner);
        java.util.Map<Type.TypeVar, Type> implMap = types.memberMapping(c.thisType(), impl.owner);
        for (int i = 0; i < impl.params.size(); i++) {
            Type a = types.erased(Type.substitute(abs.params.get(i), absMap));
            Type b = types.erased(Type.substitute(impl.params.get(i), implMap));
            if (!Types.descriptor(a).equals(Types.descriptor(b))) {
                return false;
            }
        }
        return true;
    }

    void attribMethod(MethodSymbol m, Env cenv) {
        Tree.MethodDecl d = m.decl;
        if (d == null || d.body == null) {
            return;
        }
        Env env = new Env(cenv, cenv.unit, cenv.enclClass, m);
        env.isStatic = m.isStatic();
        env.methodBoundary = true;
        for (VarSymbol p : m.paramSyms) {
            env.enterLocal(p);
        }
        Env body = env.dup();
        List<Tree> stats = d.body.stats;
        int first = 0;
        if (m.isConstructor()) {
            ClassSymbol c = cenv.enclClass;
            Tree.MethodCall explicit = explicitConstructorCall(d.body);
            if (explicit != null) {
                Env pro = body.dup();
                pro.ctorPrologue = true;
                attribStat(stats.get(0), pro);
                first = 1;
                if (c.isEnum() && explicit.superCall) {
                    error(env, explicit.pos, "call to super not allowed in enum constructor");
                }
                if (c.isRecord() && !d.compactConstructor && explicit.superCall) {
                    error(env, explicit.pos, "canonical constructor must not contain explicit constructor invocation");
                }
            } else if (!c.isEnum()) {
                m.superCtor = resolveImplicitSuper(c, cenv, d.pos);
            }
        }
        for (int i = first; i < stats.size(); i++) {
            attribStat(stats.get(i), body);
        }
        if (m.returnType.tag != Type.Tag.VOID && canCompleteNormally(d.body)) {
            error(env, d.body.endPos > 0 ? d.body.endPos : d.pos, "missing return statement");
        }
    }

    static Tree.MethodCall explicitConstructorCall(Tree.Block body) {
        if (body == null || body.stats.isEmpty() || !(body.stats.get(0) instanceof Tree.ExpressionStatement)) {
            return null;
        }
        Tree e = ((Tree.ExpressionStatement) body.stats.get(0)).expr;
        if (e instanceof Tree.MethodCall && "<init>".equals(((Tree.MethodCall) e).name)) {
            return (Tree.MethodCall) e;
        }
        return null;
    }

    /** The superclass's no-argument constructor an implicit {@code super()} calls. */
    private MethodSymbol resolveImplicitSuper(ClassSymbol c, Env cenv, int pos) {
        Type sup = c.superclass();
        if (sup == null || sup.tag != Type.Tag.CLASS) {
            return null;
        }
        Resolution r = resolveConstructor((Type.ClassType) sup, new ArrayList<Tree>(), new ArrayList<Type>(), cenv, pos, true);
        if (r == null) {
            ClassSymbol s = ((Type.ClassType) sup).sym;
            error(cenv, pos, "constructor " + s.simpleName + " in class " + s.javaName()
                    + " cannot be applied to given types;\n  required: " + describeCtorParams(s)
                    + "\n  found:    no arguments");
            return null;
        }
        ClassSymbol s = ((Type.ClassType) sup).sym;
        if (s.hasOuterInstance && !c.hasOuterInstance && !isSubclassOfAnyEnclosing(c, s.outer)) {
            error(cenv, pos, "no enclosing instance of type " + s.outer.javaName() + " is in scope");
        }
        return r.method;
    }

    private boolean isSubclassOfAnyEnclosing(ClassSymbol c, ClassSymbol target) {
        for (ClassSymbol k = c; k != null; k = k.outer) {
            if (types.isSubClass(k, target)) {
                return true;
            }
        }
        return false;
    }

    private String describeCtorParams(ClassSymbol s) {
        for (MethodSymbol m : s.methods()) {
            if (m.isConstructor()) {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < m.params.size(); i++) {
                    if (i > 0) {
                        b.append(',');
                    }
                    b.append(m.params.get(i));
                }
                return b.length() == 0 ? "no arguments" : b.toString();
            }
        }
        return "no arguments";
    }

    private void attribEnumConstant(ClassSymbol c, Tree.VarDef v, Env env) {
        Tree.NewClass nc = (Tree.NewClass) v.init;
        List<Type> argTypes = attribArgs(nc.args, env);
        Resolution r = resolveConstructor(c.erasure(), nc.args, argTypes, env, nc.pos, false);
        if (r != null) {
            nc.constructor = r.method;
            nc.varargsCall = r.varargs;
            completeArgs(nc.args, r, env);
        }
        nc.type = c.erasure();
        nc.clazz.type = c.erasure();
        nc.clazzSym = c;
        if (nc.body != null) {
            ClassSymbol anon = declareAnonymous(nc.body, env, c.erasure(), true);
            MethodSymbol ctor = anonConstructor(anon, r);
            nc.clazzSym = anon;
            nc.constructor = ctor;
            attribClass(anon);
        }
    }

    // ================================================================== local and anonymous classes

    /** Enters a local or anonymous class declared in env. */
    private ClassSymbol enterLocalClass(Tree.ClassDecl decl, Env env, String internalName) {
        Enter enter = compiler.enter;
        int before = enter.sourceClasses.size();
        ClassSymbol c = enter.enterClass(decl, env.unit, env.enclClass, internalName);
        c.declEnv = env;
        c.enclosingMethod = env.enclMethod;
        c.hasOuterInstance = decl.kind == Tree.ClassKind.CLASS && !env.isStatic && !inStaticContext(env);
        if (decl.kind != Tree.ClassKind.CLASS) {
            c.flags |= Symbol.ACC_STATIC;
        } else {
            c.flags &= ~(Symbol.ACC_STATIC | Symbol.ACC_PUBLIC | Symbol.ACC_PRIVATE | Symbol.ACC_PROTECTED);
        }
        if (!decl.anonymous) {
            env.enterLocalClass(c);
        }
        List<ClassSymbol> added = new ArrayList<ClassSymbol>(enter.sourceClasses.subList(before, enter.sourceClasses.size()));
        for (ClassSymbol k : added) {
            if (k != c || !decl.anonymous) {
                enter.enterHeader(k);
            }
        }
        return c;
    }

    private void enterLocalMembers(ClassSymbol c, int before) {
        Enter enter = compiler.enter;
        List<ClassSymbol> added = new ArrayList<ClassSymbol>(enter.sourceClasses.subList(before, enter.sourceClasses.size()));
        for (ClassSymbol k : added) {
            enter.enterMembers(k);
        }
    }

    /** True when env has no {@code this}: a static method, static initializer or static nested context. */
    boolean inStaticContext(Env env) {
        for (Env e = env; e != null; e = e.outer) {
            if (e.isStatic) {
                return true;
            }
            if (e.classBoundary) {
                return false;
            }
        }
        return true;
    }

    private void attribLocalClass(Tree.ClassDecl decl, Env env) {
        ClassSymbol encl = env.enclClass;
        encl.localCount++;
        int before = compiler.enter.sourceClasses.size();
        ClassSymbol c = enterLocalClass(decl, env, encl.internalName + "$" + encl.localCount + decl.name);
        enterLocalMembers(c, before);
        attribClass(c);
    }

    private ClassSymbol declareAnonymous(Tree.ClassDecl body, Env env, Type supertype, boolean enumConstant) {
        ClassSymbol encl = env.enclClass;
        // Anonymous classes are numbered per top-level-nested class, as javac does.
        encl.anonCount++;
        int before = compiler.enter.sourceClasses.size();
        body.anonymous = true;
        ClassSymbol c = enterLocalClass(body, env, encl.internalName + "$" + encl.anonCount);
        if (enumConstant) {
            c.hasOuterInstance = false;
        }
        if (supertype.tag == Type.Tag.CLASS && ((Type.ClassType) supertype).sym.isInterface()) {
            c.setSuperclass(syms.objectType);
            c.interfaces().add(supertype);
        } else {
            c.setSuperclass(supertype.tag == Type.Tag.CLASS ? supertype : syms.objectType);
        }
        c.thisType = null;
        enterLocalMembers(c, before);
        return c;
    }

    /** The constructor of an anonymous class: the superclass constructor's parameters, passed through. */
    private MethodSymbol anonConstructor(ClassSymbol anon, Resolution superCtor) {
        MethodSymbol ctor = new MethodSymbol("<init>", 0, anon);
        ctor.returnType = Type.VOID;
        if (superCtor != null) {
            for (Type p : superCtor.method.params) {
                ctor.params.add(types.erased(p));
            }
            ctor.superCtor = superCtor.method;
        }
        anon.methods.add(ctor);
        return ctor;
    }

    // ================================================================== statements

    void attribStats(List<Tree> stats, Env env) {
        for (Tree s : stats) {
            attribStat(s, env);
        }
    }

    void attribStat(Tree t, Env env) {
        if (t instanceof Tree.Block) {
            Env inner = env.dup();
            List<Tree> stats = ((Tree.Block) t).stats;
            for (int i = 0; i < stats.size(); i++) {
                attribStat(stats.get(i), inner);
            }
        } else if (t instanceof Tree.VarDef) {
            attribLocalVar((Tree.VarDef) t, env);
        } else if (t instanceof Tree.ClassDecl) {
            attribLocalClass((Tree.ClassDecl) t, env);
        } else if (t instanceof Tree.ExpressionStatement) {
            Tree e = ((Tree.ExpressionStatement) t).expr;
            attribExpr(e, env, null);
        } else if (t instanceof Tree.If) {
            Tree.If s = (Tree.If) t;
            attribCond(s.cond, env);
            Env thenEnv = env.dup();
            enterBindings(thenEnv, bindingsWhen(s.cond, true));
            attribStat(s.thenPart, thenEnv);
            if (s.elsePart != null) {
                Env elseEnv = env.dup();
                enterBindings(elseEnv, bindingsWhen(s.cond, false));
                attribStat(s.elsePart, elseEnv);
            }
            // Bindings flow into the enclosing scope when the other branch cannot complete.
            if (!canCompleteNormally(s.thenPart) && (s.elsePart == null || canCompleteNormally(s.elsePart))) {
                enterBindings(env, bindingsWhen(s.cond, false));
            } else if (s.elsePart != null && !canCompleteNormally(s.elsePart) && canCompleteNormally(s.thenPart)) {
                enterBindings(env, bindingsWhen(s.cond, true));
            }
        } else if (t instanceof Tree.WhileLoop) {
            Tree.WhileLoop s = (Tree.WhileLoop) t;
            attribCond(s.cond, env);
            Env body = loopEnv(env);
            enterBindings(body, bindingsWhen(s.cond, true));
            attribStat(s.body, body);
            if (!hasBreak(s.body)) {
                enterBindings(env, bindingsWhen(s.cond, false));
            }
        } else if (t instanceof Tree.DoLoop) {
            Tree.DoLoop s = (Tree.DoLoop) t;
            attribStat(s.body, loopEnv(env));
            attribCond(s.cond, env);
        } else if (t instanceof Tree.ForLoop) {
            Tree.ForLoop s = (Tree.ForLoop) t;
            Env loop = env.dup();
            for (Tree i : s.init) {
                attribStat(i, loop);
            }
            if (s.cond != null) {
                attribCond(s.cond, loop);
            }
            Env body = loopEnv(loop);
            if (s.cond != null) {
                enterBindings(body, bindingsWhen(s.cond, true));
            }
            attribStat(s.body, body);
            Env step = loop.dup();
            if (s.cond != null) {
                enterBindings(step, bindingsWhen(s.cond, true));
            }
            for (Tree st : s.step) {
                attribStat(st, step);
            }
        } else if (t instanceof Tree.ForEach) {
            attribForEach((Tree.ForEach) t, env);
        } else if (t instanceof Tree.Labeled) {
            Tree.Labeled s = (Tree.Labeled) t;
            Env inner = env.dup();
            inner.label = s.label;
            inner.breakable = true;
            attribStat(s.body, inner);
        } else if (t instanceof Tree.Switch) {
            attribSwitch((Tree.Switch) t, env, null);
        } else if (t instanceof Tree.Return) {
            attribReturn((Tree.Return) t, env);
        } else if (t instanceof Tree.Break) {
            Tree.Break b = (Tree.Break) t;
            if (env.switchExpression != null && b.label == null && !env.breakable) {
                error(env, t.pos, "attempting to break out of a switch expression");
            } else if (b.label == null && !env.breakable) {
                error(env, t.pos, "break outside switch or loop");
            } else if (b.label != null && !hasLabel(env, b.label)) {
                error(env, t.pos, "undefined label: " + b.label);
            }
        } else if (t instanceof Tree.Continue) {
            Tree.Continue c = (Tree.Continue) t;
            if (!env.continuable) {
                error(env, t.pos, "continue outside of loop");
            } else if (c.label != null && !hasLabel(env, c.label)) {
                error(env, t.pos, "undefined label: " + c.label);
            }
        } else if (t instanceof Tree.Yield) {
            Tree.Yield y = (Tree.Yield) t;
            if (env.switchExpression == null) {
                error(env, t.pos, "yield outside of switch expression");
                attribExpr(y.value, env, null);
            } else {
                Type pt = env.switchExpression.type;
                attribExpr(y.value, env, pt);
                if (env.yields != null) {
                    env.yields.add(y.value);
                }
            }
        } else if (t instanceof Tree.Throw) {
            Tree.Throw th = (Tree.Throw) t;
            Type et = attribExpr(th.expr, env, null);
            if (!et.isErroneous() && !types.isSubtype(types.erased(et), syms.type("java/lang/Throwable"))) {
                error(env, th.expr.pos, "incompatible types: " + et + " cannot be converted to Throwable");
            } else {
                checkThrown(et, env, t.pos);
            }
        } else if (t instanceof Tree.Try) {
            attribTry((Tree.Try) t, env);
        } else if (t instanceof Tree.Synchronized) {
            Tree.Synchronized s = (Tree.Synchronized) t;
            Type lt = attribExpr(s.lock, env, null);
            if (lt.isPrimitive() || lt.tag == Type.Tag.NULL) {
                error(env, s.lock.pos, "unexpected type\n  required: reference\n  found:    " + lt);
            }
            attribStat(s.body, env);
        } else if (t instanceof Tree.Assert) {
            Tree.Assert a = (Tree.Assert) t;
            attribCond(a.cond, env);
            if (a.detail != null) {
                attribExpr(a.detail, env, null);
            }
        } else if (t instanceof Tree.Empty) {
            // nothing
        } else if (t != null) {
            error(env, t.pos, "not a statement");
        }
    }

    private Env loopEnv(Env env) {
        Env e = env.dup();
        e.breakable = true;
        e.continuable = true;
        e.label = null;
        return e;
    }

    private static boolean hasLabel(Env env, String label) {
        for (Env e = env; e != null && !e.classBoundary && e.lambda == null; e = e.outer) {
            if (label.equals(e.label)) {
                return true;
            }
        }
        return false;
    }

    private void attribLocalVar(Tree.VarDef v, Env env) {
        Type t;
        checkLocalRedeclared(v.name, env, v.pos);
        if (v.vartype == null && v.declaredType == null) {
            if (v.init == null) {
                error(env, v.pos, "cannot infer type for local variable " + v.name + "\n  (cannot use 'var' on variable without initializer)");
                t = Type.ERROR;
            } else if (v.init instanceof Tree.Lambda || v.init instanceof Tree.MethodRef) {
                error(env, v.pos, "cannot infer type for local variable " + v.name + "\n  (lambda expression needs an explicit target-type)");
                t = Type.ERROR;
            } else if (v.init instanceof Tree.NewArray && ((Tree.NewArray) v.init).elemType == null) {
                error(env, v.pos, "cannot infer type for local variable " + v.name + "\n  (array initializer needs an explicit target-type)");
                t = Type.ERROR;
            } else {
                t = attribExpr(v.init, env, null);
                if (t.tag == Type.Tag.NULL) {
                    error(env, v.pos, "cannot infer type for local variable " + v.name + "\n  (variable initializer is 'null')");
                    t = Type.ERROR;
                } else if (t.tag == Type.Tag.VOID) {
                    error(env, v.pos, "cannot infer type for local variable " + v.name + "\n  (variable initializer is 'void')");
                    t = Type.ERROR;
                }
                t = upward(t);
            }
        } else {
            t = v.declaredType != null ? v.declaredType : attribType(v.vartype, env);
            if (v.init != null) {
                Type it = attribExpr(v.init, env, t);
                checkAssignable(v.init, it, t, env);
            }
        }
        VarSymbol sym = new VarSymbol(v.name, v.mods == null ? 0 : v.mods.flags & Symbol.ACC_FINAL, t, VarSymbol.Kind.LOCAL);
        sym.hasInitializer = v.init != null;
        if (v.init != null && v.init.constant != null && sym.isFinal() && isConstantType(t)) {
            sym.constValue = convertConstant(v.init.constant, t);
        }
        v.sym = sym;
        v.type = t;
        env.enterLocal(sym);
    }

    /** The type a {@code var} declaration gets: no captured wildcards or inference variables. */
    private Type upward(Type t) {
        t = types.resolveInference(t);
        if (t.tag == Type.Tag.INFERENCE) {
            return types.erased(t);
        }
        if (t.tag == Type.Tag.INTERSECTION) {
            return ((Type.IntersectionType) t).bounds.get(0);
        }
        return t;
    }

    void checkLocalRedeclared(String name, Env env, int pos) {
        for (Env e = env; e != null && !e.classBoundary; e = e.outer) {
            if (e.localHere(name) != null) {
                String where = env.enclMethod == null ? "initializer" : env.enclMethod.isConstructor()
                        ? "constructor " + env.enclMethod : "method " + env.enclMethod;
                error(env, pos, "variable " + name + " is already defined in " + where);
                return;
            }
        }
    }

    private void attribForEach(Tree.ForEach s, Env env) {
        Type et = attribExpr(s.expr, env, null);
        Type elem;
        Type erasedEt = types.erased(et);
        if (erasedEt.tag == Type.Tag.ARRAY) {
            Type rt = types.resolveInference(et);
            elem = rt.tag == Type.Tag.ARRAY ? ((Type.ArrayType) rt).elem : ((Type.ArrayType) erasedEt).elem;
        } else if (et.isErroneous()) {
            elem = Type.ERROR;
        } else {
            Type.ClassType it = types.asSuper(et, syms.require("java/lang/Iterable"));
            if (it == null) {
                error(env, s.expr.pos, "for-each not applicable to expression type\n  required: array or java.lang.Iterable\n  found:    " + et);
                elem = Type.ERROR;
            } else if (it.args.isEmpty()) {
                elem = syms.objectType;
            } else {
                elem = types.upperBound(it.args.get(0));
            }
        }
        Env loop = loopEnv(env);
        Tree.VarDef v = s.var;
        Type vt;
        if (v.vartype == null) {
            vt = upward(elem);
        } else {
            vt = attribType(v.vartype, env);
            if (!elem.isErroneous() && !types.isAssignable(elem, vt, true, null)) {
                error(env, v.pos, "incompatible types: " + elem + " cannot be converted to " + vt);
            }
        }
        checkLocalRedeclared(v.name, env, v.pos);
        VarSymbol sym = new VarSymbol(v.name, v.mods == null ? 0 : v.mods.flags & Symbol.ACC_FINAL, vt, VarSymbol.Kind.LOCAL);
        sym.hasInitializer = true;
        v.sym = sym;
        v.type = vt;
        // The element type as the iteration yields it (the cast Gen inserts goes from here to vt).
        s.type = elem;
        loop.enterLocal(sym);
        attribStat(s.body, loop);
    }

    private void attribReturn(Tree.Return r, Env env) {
        Env e = env;
        while (e != null && e.lambda == null && !e.methodBoundary) {
            e = e.outer;
        }
        if (e == null) {
            error(env, r.pos, "return outside method");
            return;
        }
        if (env.switchExpression != null && isOutside(env, e, env.switchExpression)) {
            error(env, r.pos, "attempting to return out of a switch expression");
        }
        if (e.lambda != null) {
            if (r.expr == null) {
                if (e.lambdaReturn != null && e.lambdaReturn.tag != Type.Tag.VOID && !Types.containsInference(e.lambdaReturn)) {
                    error(env, r.pos, "incompatible types: missing return value");
                }
                return;
            }
            Type pt = e.lambdaReturn;
            if (pt != null && pt.tag == Type.Tag.VOID) {
                attribExpr(r.expr, env, null);
                error(env, r.expr.pos, "incompatible types: unexpected return value");
                return;
            }
            boolean inferring = pt == null || Types.containsInference(pt);
            Type t = attribExpr(r.expr, env, inferring ? null : pt);
            if (!inferring) {
                checkAssignable(r.expr, t, pt, env);
            }
            if (e.lambdaReturns != null) {
                e.lambdaReturns.add(t);
            }
            return;
        }
        MethodSymbol m = e.enclMethod;
        if (m == null) {
            error(env, r.pos, "return outside method");
            return;
        }
        Type rt = m.returnType;
        if (r.expr == null) {
            if (rt.tag != Type.Tag.VOID) {
                error(env, r.pos, "incompatible types: missing return value");
            }
            return;
        }
        if (rt.tag == Type.Tag.VOID) {
            attribExpr(r.expr, env, null);
            error(env, r.expr.pos, "incompatible types: unexpected return value");
            return;
        }
        Type t = attribExpr(r.expr, env, rt);
        checkAssignable(r.expr, t, rt, env);
    }

    private static boolean isOutside(Env env, Env target, Tree.Switch sw) {
        // The switch expression env lies between env and the method/lambda env target.
        for (Env e = env; e != null && e != target; e = e.outer) {
            if (e.switchExpression == sw && (e.outer == null || e.outer.switchExpression != sw)) {
                return true;
            }
        }
        return false;
    }

    private void attribTry(Tree.Try t, Env env) {
        Env tryEnv = env.dup();
        List<Type> caught = new ArrayList<Type>();
        for (Tree.Catch c : t.catches) {
            Tree.VarDef p = c.param;
            if (p.unionTypes != null && !p.unionTypes.isEmpty()) {
                for (Tree u : p.unionTypes) {
                    caught.add(attribType(u, env));
                }
            } else {
                caught.add(attribType(p.vartype, env));
            }
        }
        tryEnv.caught = caught;
        for (Tree r : t.resources) {
            if (r instanceof Tree.VarDef) {
                attribLocalVar((Tree.VarDef) r, tryEnv);
                Tree.VarDef v = (Tree.VarDef) r;
                v.sym.flags |= Symbol.ACC_FINAL;
                checkAutoCloseable(v.sym.type, tryEnv, r.pos);
            } else {
                Type rt = attribExpr(r, tryEnv, null);
                checkAutoCloseable(rt, tryEnv, r.pos);
            }
        }
        Env body = tryEnv.dup();
        body.caught = caught;
        attribStat(t.body, body);
        int i = 0;
        for (Tree.Catch c : t.catches) {
            Tree.VarDef p = c.param;
            Type pt;
            if (p.unionTypes != null && !p.unionTypes.isEmpty()) {
                List<Type> alts = new ArrayList<Type>();
                for (Tree u : p.unionTypes) {
                    alts.add(u.type);
                }
                pt = types.lub(alts);
                i += alts.size();
            } else {
                pt = p.vartype.type;
                i++;
            }
            if (!pt.isErroneous() && !types.isSubtype(types.erased(pt), syms.type("java/lang/Throwable"))) {
                error(env, p.pos, "incompatible types: " + pt + " cannot be converted to Throwable");
            }
            Env cenv = env.dup();
            checkLocalRedeclared(p.name, env, p.pos);
            VarSymbol sym = new VarSymbol(p.name, (p.mods == null ? 0 : p.mods.flags & Symbol.ACC_FINAL)
                    | (p.unionTypes != null && !p.unionTypes.isEmpty() ? Symbol.ACC_FINAL : 0), pt, VarSymbol.Kind.EXCEPTION);
            sym.hasInitializer = true;
            p.sym = sym;
            p.type = pt;
            cenv.enterLocal(sym);
            attribStat(c.body, cenv);
        }
        if (t.finalizer != null) {
            attribStat(t.finalizer, env.dup());
        }
    }

    private void checkAutoCloseable(Type t, Env env, int pos) {
        if (t.isErroneous()) {
            return;
        }
        ClassSymbol ac = syms.require("java/lang/AutoCloseable");
        Type.ClassType view = types.asSuper(t, ac);
        if (view == null) {
            error(env, pos, "incompatible types: try-with-resources not applicable to variable type\n    ("
                    + t + " cannot be converted to AutoCloseable)");
            return;
        }
        // close() may throw: check what this type's close() declares.
        MethodSymbol close = findMethodByArity(types.erased(t), "close", 0);
        if (close != null) {
            for (Type th : close.thrown) {
                checkThrown(th, env, pos);
            }
        }
    }

    private MethodSymbol findMethodByArity(Type site, String name, int arity) {
        List<MethodSymbol> all = new ArrayList<MethodSymbol>();
        collectMethods(site, name, all, new LinkedHashSet<ClassSymbol>());
        for (MethodSymbol m : all) {
            if (m.params.size() == arity) {
                return m;
            }
        }
        return null;
    }

    // ================================================================== reachability

    /** A conservative "can complete normally" (JLS 14.22) used for missing-return and binding scope. */
    boolean canCompleteNormally(Tree t) {
        if (t == null) {
            return true;
        }
        if (t instanceof Tree.Block) {
            for (Tree s : ((Tree.Block) t).stats) {
                if (!canCompleteNormally(s)) {
                    return false;
                }
            }
            return true;
        }
        if (t instanceof Tree.Return || t instanceof Tree.Throw || t instanceof Tree.Break || t instanceof Tree.Continue
                || t instanceof Tree.Yield) {
            return false;
        }
        if (t instanceof Tree.If) {
            Tree.If s = (Tree.If) t;
            if (s.elsePart == null) {
                return true;
            }
            return canCompleteNormally(s.thenPart) || canCompleteNormally(s.elsePart);
        }
        if (t instanceof Tree.WhileLoop) {
            Tree.WhileLoop w = (Tree.WhileLoop) t;
            return !isTrue(w.cond) || hasBreak(w.body);
        }
        if (t instanceof Tree.DoLoop) {
            Tree.DoLoop d = (Tree.DoLoop) t;
            return !isTrue(d.cond) && (canCompleteNormally(d.body) || hasContinue(d.body)) || hasBreak(d.body);
        }
        if (t instanceof Tree.ForLoop) {
            Tree.ForLoop f = (Tree.ForLoop) t;
            return f.cond != null && !isTrue(f.cond) || hasBreak(f.body);
        }
        if (t instanceof Tree.Labeled) {
            Tree.Labeled l = (Tree.Labeled) t;
            return canCompleteNormally(l.body) || hasLabeledBreak(l.body, l.label);
        }
        if (t instanceof Tree.Switch) {
            Tree.Switch sw = (Tree.Switch) t;
            if (sw.isExpression) {
                return true;
            }
            boolean exhaustive = false;
            for (Tree.Case c : sw.cases) {
                if (c.isDefault || isUnconditionalCase(c, sw)) {
                    exhaustive = true;
                }
            }
            if (!exhaustive && !(sw.switchKind == SW_PATTERN || sw.switchKind == SW_ENUM && sw.arrows && sw.needsDefaultThrow)) {
                return true;
            }
            if (sw.switchKind == SW_PATTERN && !exhaustive && !sw.needsDefaultThrow) {
                return true;
            }
            for (Tree.Case c : sw.cases) {
                for (Tree s : c.stats) {
                    if (hasBreak(s)) {
                        return true;
                    }
                }
            }
            if (sw.cases.isEmpty()) {
                return true;
            }
            if (sw.arrows) {
                for (Tree.Case c : sw.cases) {
                    if (c.stats.isEmpty() || c.arrowExpr != null || canCompleteNormally(c.stats.get(c.stats.size() - 1))) {
                        return true;
                    }
                }
                return false;
            }
            Tree.Case last = sw.cases.get(sw.cases.size() - 1);
            if (last.stats.isEmpty()) {
                return true;
            }
            for (Tree s : last.stats) {
                if (!canCompleteNormally(s)) {
                    return false;
                }
            }
            return true;
        }
        if (t instanceof Tree.Try) {
            Tree.Try tr = (Tree.Try) t;
            if (tr.finalizer != null && !canCompleteNormally(tr.finalizer)) {
                return false;
            }
            if (canCompleteNormally(tr.body)) {
                return true;
            }
            for (Tree.Catch c : tr.catches) {
                if (canCompleteNormally(c.body)) {
                    return true;
                }
            }
            return false;
        }
        if (t instanceof Tree.Synchronized) {
            return canCompleteNormally(((Tree.Synchronized) t).body);
        }
        if (t instanceof Tree.ExpressionStatement) {
            Tree e = ((Tree.ExpressionStatement) t).expr;
            if (e instanceof Tree.Switch) {
                return true;
            }
            return true;
        }
        return true;
    }

    /** A type pattern that matches every (non-null) selector value. */
    static boolean isUnconditionalPattern(Tree l, Tree.Switch sw) {
        if (!(l instanceof Tree.BindingPattern) || l.type == null || sw.selector.type == null) {
            return false;
        }
        Type st = Types.erasure(sw.selector.type);
        Type pt = Types.erasure(l.type);
        if (st == null || pt == null || st.tag != Type.Tag.CLASS || pt.tag != Type.Tag.CLASS) {
            return pt != null && pt.tag == Type.Tag.CLASS && "java/lang/Object".equals(((Type.ClassType) pt).sym.internalName);
        }
        return ((Type.ClassType) pt).sym == ((Type.ClassType) st).sym
                || "java/lang/Object".equals(((Type.ClassType) pt).sym.internalName);
    }

    private boolean isUnconditionalCase(Tree.Case c, Tree.Switch sw) {
        if (c.guard != null || sw.selector.type == null) {
            return false;
        }
        for (Tree l : c.labels) {
            if (l instanceof Tree.BindingPattern) {
                Type pt = ((Tree.BindingPattern) l).var.type;
                if (pt != null && types.isSubtype(types.erased(sw.selector.type), types.erased(pt))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isTrue(Tree cond) {
        return cond != null && Boolean.TRUE.equals(cond.constant);
    }

    /** Does t contain a break that exits t itself (unlabeled, not nested in an inner loop/switch)? */
    static boolean hasBreak(Tree t) {
        return findBreak(t, null, 0);
    }

    static boolean hasLabeledBreak(Tree t, String label) {
        return findBreak(t, label, 0);
    }

    private static boolean findBreak(Tree t, String label, int depth) {
        if (t == null) {
            return false;
        }
        if (t instanceof Tree.Break) {
            Tree.Break b = (Tree.Break) t;
            return label == null ? b.label == null && depth == 0 : label.equals(b.label);
        }
        if (t instanceof Tree.Block) {
            for (Tree s : ((Tree.Block) t).stats) {
                if (findBreak(s, label, depth)) {
                    return true;
                }
            }
            return false;
        }
        if (t instanceof Tree.If) {
            return findBreak(((Tree.If) t).thenPart, label, depth) || findBreak(((Tree.If) t).elsePart, label, depth);
        }
        if (t instanceof Tree.WhileLoop) {
            return findBreak(((Tree.WhileLoop) t).body, label, depth + 1);
        }
        if (t instanceof Tree.DoLoop) {
            return findBreak(((Tree.DoLoop) t).body, label, depth + 1);
        }
        if (t instanceof Tree.ForLoop) {
            return findBreak(((Tree.ForLoop) t).body, label, depth + 1);
        }
        if (t instanceof Tree.ForEach) {
            return findBreak(((Tree.ForEach) t).body, label, depth + 1);
        }
        if (t instanceof Tree.Labeled) {
            return findBreak(((Tree.Labeled) t).body, label, depth);
        }
        if (t instanceof Tree.Switch) {
            if (((Tree.Switch) t).isExpression) {
                return false;
            }
            for (Tree.Case c : ((Tree.Switch) t).cases) {
                for (Tree s : c.stats) {
                    if (findBreak(s, label, depth + 1)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (t instanceof Tree.Try) {
            Tree.Try tr = (Tree.Try) t;
            if (findBreak(tr.body, label, depth) || findBreak(tr.finalizer, label, depth)) {
                return true;
            }
            for (Tree.Catch c : tr.catches) {
                if (findBreak(c.body, label, depth)) {
                    return true;
                }
            }
            return false;
        }
        if (t instanceof Tree.Synchronized) {
            return findBreak(((Tree.Synchronized) t).body, label, depth);
        }
        return false;
    }

    private static boolean hasContinue(Tree t) {
        if (t instanceof Tree.Continue) {
            return ((Tree.Continue) t).label == null;
        }
        if (t instanceof Tree.Block) {
            for (Tree s : ((Tree.Block) t).stats) {
                if (hasContinue(s)) {
                    return true;
                }
            }
        }
        if (t instanceof Tree.If) {
            return hasContinue(((Tree.If) t).thenPart) || ((Tree.If) t).elsePart != null && hasContinue(((Tree.If) t).elsePart);
        }
        if (t instanceof Tree.Try) {
            return hasContinue(((Tree.Try) t).body);
        }
        if (t instanceof Tree.Labeled) {
            return hasContinue(((Tree.Labeled) t).body);
        }
        if (t instanceof Tree.Switch && !((Tree.Switch) t).isExpression) {
            for (Tree.Case c : ((Tree.Switch) t).cases) {
                for (Tree s : c.stats) {
                    if (hasContinue(s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ================================================================== checked exceptions

    boolean isChecked(Type t) {
        Type e = types.erased(t);
        if (e.tag != Type.Tag.CLASS) {
            return false;
        }
        ClassSymbol c = ((Type.ClassType) e).sym;
        return !types.isSubClass(c, syms.require("java/lang/RuntimeException"))
                && !types.isSubClass(c, syms.require("java/lang/Error"))
                && types.isSubClass(c, syms.require("java/lang/Throwable"));
    }

    void checkThrown(Type exc, Env env, int pos) {
        if (exc == null || exc.isErroneous() || !isChecked(exc)) {
            return;
        }
        Type ee = types.erased(exc);
        for (Env e = env; e != null; e = e.outer) {
            if (e.caught != null) {
                for (Type c : e.caught) {
                    if (types.isSubtype(ee, types.erased(c))) {
                        return;
                    }
                }
            }
            if (e.lambda != null) {
                MethodSymbol sam = e.lambda.target;
                if (sam == null) {
                    return;
                }
                for (Type th : sam.thrown) {
                    if (th.tag == Type.Tag.TYPEVAR || types.isSubtype(ee, types.erased(th))) {
                        return;
                    }
                }
                error(env, pos, "unreported exception " + exc + "; must be caught or declared to be thrown");
                return;
            }
            if (e.methodBoundary) {
                if (e.enclMethod == null) {
                    // An initializer: instance initializers of anonymous classes may throw anything.
                    if (e.isStatic || !e.enclClass.anonymous) {
                        if (e.isStatic) {
                            error(env, pos, "unreported exception " + exc + "; must be caught or declared to be thrown");
                        }
                    }
                    return;
                }
                for (Type th : e.enclMethod.thrown) {
                    if (th.tag == Type.Tag.TYPEVAR || types.isSubtype(ee, types.erased(th))) {
                        return;
                    }
                }
                error(env, pos, "unreported exception " + exc + "; must be caught or declared to be thrown");
                return;
            }
            if (e.classBoundary) {
                return;
            }
        }
    }

    // ================================================================== expressions

    Type attribExpr(Tree t, Env env, Type pt) {
        Type result = attribExprInner(t, env, pt);
        if (result == null) {
            result = Type.ERROR;
        }
        t.type = result;
        return result;
    }

    private Type attribExprInner(Tree t, Env env, Type pt) {
        if (t instanceof Tree.Literal) {
            return attribLiteral((Tree.Literal) t);
        }
        if (t instanceof Tree.Parens) {
            Tree inner = ((Tree.Parens) t).expr;
            Type r = attribExpr(inner, env, pt);
            t.constant = inner.constant;
            return r;
        }
        if (t instanceof Tree.Ident) {
            return attribIdent((Tree.Ident) t, env);
        }
        if (t instanceof Tree.Select) {
            return attribSelect((Tree.Select) t, env);
        }
        if (t instanceof Tree.MethodCall) {
            return attribCall((Tree.MethodCall) t, env, pt);
        }
        if (t instanceof Tree.NewClass) {
            return attribNew((Tree.NewClass) t, env, pt);
        }
        if (t instanceof Tree.NewArray) {
            return attribNewArray((Tree.NewArray) t, env, pt);
        }
        if (t instanceof Tree.ArrayAccess) {
            Tree.ArrayAccess a = (Tree.ArrayAccess) t;
            Type at = attribExpr(a.array, env, null);
            Type it = attribExpr(a.index, env, Type.INT);
            checkIndex(a.index, it, env);
            Type rat = types.resolveInference(at);
            Type erasedAt = types.erased(rat);
            if (at.isErroneous()) {
                return Type.ERROR;
            }
            if (rat.tag == Type.Tag.ARRAY) {
                return ((Type.ArrayType) rat).elem;
            }
            if (erasedAt.tag == Type.Tag.ARRAY) {
                return ((Type.ArrayType) erasedAt).elem;
            }
            error(env, a.pos, "array required, but " + at + " found");
            return Type.ERROR;
        }
        if (t instanceof Tree.Assign) {
            return attribAssign((Tree.Assign) t, env);
        }
        if (t instanceof Tree.CompoundAssign) {
            return attribCompoundAssign((Tree.CompoundAssign) t, env);
        }
        if (t instanceof Tree.Unary) {
            return attribUnary((Tree.Unary) t, env);
        }
        if (t instanceof Tree.Binary) {
            return attribBinary((Tree.Binary) t, env);
        }
        if (t instanceof Tree.Conditional) {
            return attribConditional((Tree.Conditional) t, env, pt);
        }
        if (t instanceof Tree.InstanceOf) {
            return attribInstanceOf((Tree.InstanceOf) t, env);
        }
        if (t instanceof Tree.Cast) {
            return attribCast((Tree.Cast) t, env);
        }
        if (t instanceof Tree.Lambda) {
            return attribLambda((Tree.Lambda) t, env, pt);
        }
        if (t instanceof Tree.MethodRef) {
            return attribMethodRef((Tree.MethodRef) t, env, pt);
        }
        if (t instanceof Tree.ClassLiteral) {
            Tree.ClassLiteral cl = (Tree.ClassLiteral) t;
            Type ct = attribType(cl.clazz, env);
            if (ct.tag == Type.Tag.TYPEVAR) {
                error(env, t.pos, "cannot select from a type variable");
            }
            Type arg = ct.isPrimitive() || ct.tag == Type.Tag.VOID
                    ? ct.tag == Type.Tag.VOID ? syms.type("java/lang/Void") : syms.boxedType(ct) : types.erased(ct);
            ClassSymbol classSym = syms.require("java/lang/Class");
            return classSym.typeParams().isEmpty() ? classSym.erasure() : new Type.ClassType(classSym, Type.list(arg), null);
        }
        if (t instanceof Tree.Switch) {
            return attribSwitch((Tree.Switch) t, env, pt);
        }
        if (t instanceof Tree.ResolvedType) {
            return t.type;
        }
        if (t instanceof Tree.TypeApply || t instanceof Tree.ArrayTypeTree || t instanceof Tree.PrimitiveTypeTree) {
            error(env, t.pos, "illegal start of expression");
            return Type.ERROR;
        }
        if (t instanceof Tree.Erroneous) {
            return Type.ERROR;
        }
        error(env, t.pos, "illegal start of expression");
        return Type.ERROR;
    }

    private Type attribLiteral(Tree.Literal l) {
        l.constant = l.value;
        switch (l.kind) {
            case INT_LITERAL: return Type.INT;
            case LONG_LITERAL: return Type.LONG;
            case FLOAT_LITERAL: return Type.FLOAT;
            case DOUBLE_LITERAL: return Type.DOUBLE;
            case CHAR_LITERAL: return Type.CHAR;
            case STRING_LITERAL: return syms.stringType;
            case TRUE:
            case FALSE: return Type.BOOLEAN;
            default:
                l.constant = null;
                return Type.NULL;
        }
    }

    private void checkIndex(Tree index, Type it, Env env) {
        Type u = types.unboxedOrSelf(it);
        if (it.isErroneous()) {
            return;
        }
        if (u == null || !u.isIntegral() || u.tag == Type.Tag.LONG) {
            if (u != null && u.tag == Type.Tag.LONG) {
                error(env, index.pos, "incompatible types: possible lossy conversion from long to int");
            } else {
                error(env, index.pos, "incompatible types: " + it + " cannot be converted to int");
            }
        }
    }

    private Type attribIdent(Tree.Ident id, Env env) {
        String name = id.name;
        if (name.equals("this")) {
            if (inStaticContext(env)) {
                error(env, id.pos, "non-static variable this cannot be referenced from a static context");
                return Type.ERROR;
            }
            if (env.ctorPrologue) {
                error(env, id.pos, "cannot reference this before supertype constructor has been called");
            }
            markThisCapture(env);
            return env.enclClass.thisType();
        }
        if (name.equals("super")) {
            error(env, id.pos, "'.' expected");
            return Type.ERROR;
        }
        VarRef r = findVariable(name, env, true, id.pos);
        if (r == null) {
            // A class name used where a value is expected.
            // javac says the same when the name is a class used as a value.
            error(env, id.pos, "cannot find symbol\n  symbol:   variable " + name + "\n  location: class " + env.enclClass.javaName());
            return Type.ERROR;
        }
        VarSymbol v = r.sym;
        id.sym = v;
        if (v.kind == VarSymbol.Kind.FIELD) {
            id.site = r.site;
            if (r.staticContext) {
                error(env, id.pos, "non-static variable " + name + " cannot be referenced from a static context");
            } else if (!v.isStatic() && env.ctorPrologue && r.site == env.enclClass) {
                error(env, id.pos, "cannot reference " + name + " before supertype constructor has been called");
            }
            ensureConstant(v);
            id.constant = v.constValue;
            if (illegalForwardReference(v, env)) {
                error(env, id.pos, "illegal forward reference");
            }
            return fieldType(r.site != null ? r.site.thisType() : null, v);
        }
        id.constant = v.constValue;
        return v.type;
    }

    /** A field initializer reading a later field of the same class by simple name. */
    private boolean illegalForwardReference(VarSymbol v, Env env) {
        if (v.decl == null || v.owner != env.enclClass || env.enclMethod != null || !env.isStatic && v.isStatic()) {
            return false;
        }
        Env e = env;
        while (e != null && !e.methodBoundary) {
            if (e.lambda != null || e.classBoundary) {
                return false;
            }
            e = e.outer;
        }
        if (e == null || e.enclMethod != null || e.isStatic != v.isStatic()) {
            return false;
        }
        Tree current = currentFieldInit;
        if (current == null) {
            return false;
        }
        List<Tree> members = v.owner.decl.members;
        int vi = members.indexOf(v.decl);
        int ci = members.indexOf(current);
        return vi >= 0 && ci >= 0 && vi >= ci;
    }

    /** The field initializer being attributed (for forward-reference checks). */
    private Tree currentFieldInit;

    /** A field's type as seen through site's parameterization. */
    private Type fieldType(Type site, VarSymbol f) {
        if (site == null || f.owner == null || f.isStatic()) {
            return f.type;
        }
        java.util.Map<Type.TypeVar, Type> map = types.memberMapping(site, f.owner);
        return Type.substitute(f.type, map);
    }

    /** Computes a source field's constant value on demand (another class may need it first). */
    void ensureConstant(VarSymbol f) {
        if (f.constState != 0 || f.decl == null || !f.isFinal() || f.decl.init == null || !isConstantType(f.type)
                || !isConstantShaped(f.decl.init) || f.owner == null || f.owner.decl == null) {
            return;
        }
        f.constState = 1;
        compiler.quiet++;
        try {
            Env env = initializerEnv(classEnv(f.owner), f.isStatic());
            attribExpr(f.decl.init, env, f.type);
            if (f.decl.init.constant != null) {
                f.constValue = convertConstant(f.decl.init.constant, f.type);
            }
        } finally {
            compiler.quiet--;
            f.constState = 2;
        }
    }

    private static boolean isConstantShaped(Tree t) {
        if (t instanceof Tree.Literal || t instanceof Tree.Ident) {
            return true;
        }
        if (t instanceof Tree.Select) {
            return isConstantShaped(((Tree.Select) t).selected) || ((Tree.Select) t).selected instanceof Tree.Select;
        }
        if (t instanceof Tree.Parens) {
            return isConstantShaped(((Tree.Parens) t).expr);
        }
        if (t instanceof Tree.Unary) {
            Tree.Unary u = (Tree.Unary) t;
            return u.op != Token.Kind.PLUSPLUS && u.op != Token.Kind.SUBSUB && isConstantShaped(u.arg);
        }
        if (t instanceof Tree.Binary) {
            return isConstantShaped(((Tree.Binary) t).lhs) && isConstantShaped(((Tree.Binary) t).rhs);
        }
        if (t instanceof Tree.Conditional) {
            Tree.Conditional c = (Tree.Conditional) t;
            return isConstantShaped(c.cond) && isConstantShaped(c.truePart) && isConstantShaped(c.falsePart);
        }
        if (t instanceof Tree.Cast) {
            return isConstantShaped(((Tree.Cast) t).expr);
        }
        return false;
    }

    static boolean isConstantType(Type t) {
        return t.isPrimitive() && t.tag != Type.Tag.VOID
                || t.tag == Type.Tag.CLASS && ((Type.ClassType) t).sym.internalName.equals("java/lang/String");
    }

    private Type attribSelect(Tree.Select sel, Env env) {
        String name = sel.name;
        if (name.equals("this")) {
            Object q = resolveQualifier(sel.selected, env);
            if (!(q instanceof Type) || ((Type) q).tag != Type.Tag.CLASS) {
                error(env, sel.pos, "not an enclosing class");
                return Type.ERROR;
            }
            ClassSymbol target = ((Type.ClassType) q).sym;
            if (!isEnclosingInstance(env, target)) {
                if (inStaticContext(env)) {
                    error(env, sel.pos, "non-static variable this cannot be referenced from a static context");
                } else {
                    error(env, sel.pos, "not an enclosing class: " + target.simpleName);
                }
                return Type.ERROR;
            }
            sel.sym = target;
            markThisCapture(env);
            return target.thisType();
        }
        if (name.equals("super")) {
            error(env, sel.pos, "'.' expected");
            return Type.ERROR;
        }
        if (sel.selected instanceof Tree.Ident && "super".equals(((Tree.Ident) sel.selected).name)) {
            // super.field: a field of the superclass, read from this.
            if (inStaticContext(env)) {
                error(env, sel.pos, "non-static variable super cannot be referenced from a static context");
                return Type.ERROR;
            }
            markThisCapture(env);
            Type sup = types.supertype(env.enclClass.thisType());
            sel.selected.type = sup;
            VarSymbol f = sup == null ? null : findField(sup, name);
            if (f == null) {
                error(env, sel.pos, "cannot find symbol\n  symbol: variable " + name);
                return Type.ERROR;
            }
            sel.sym = f;
            return fieldType(sup, f);
        }
        Object q = resolveQualifier(sel.selected, env);
        if (q instanceof String) {
            unresolvedQualifier(sel.selected, (String) q, env);
            return Type.ERROR;
        }
        if (q instanceof Type) {
            Type qt = (Type) q;
            if (qt.isErroneous()) {
                return Type.ERROR;
            }
            sel.staticRef = true;
            if (qt.tag != Type.Tag.CLASS) {
                error(env, sel.pos, "cannot find symbol\n  symbol:   variable " + name + "\n  location: " + qt);
                return Type.ERROR;
            }
            VarSymbol f = findField(qt, name);
            if (f == null) {
                if (name.equals("length") && qt.tag == Type.Tag.ARRAY) {
                    return Type.INT;
                }
                error(env, sel.pos, "cannot find symbol\n  symbol:   variable " + name + "\n  location: class " + ((Type.ClassType) qt).sym.javaName());
                return Type.ERROR;
            }
            sel.sym = f;
            if (!f.isStatic()) {
                error(env, sel.pos, "non-static variable " + name + " cannot be referenced from a static context");
            }
            checkAccess(f.flags, f.owner, env, sel.pos, name);
            ensureConstant(f);
            sel.constant = f.constValue;
            return f.type;
        }
        Type st = attribExpr(sel.selected, env, null);
        if (st.isErroneous()) {
            return Type.ERROR;
        }
        Type rst = types.resolveInference(st);
        if (rst.isPrimitive()) {
            error(env, sel.pos, rst + " cannot be dereferenced");
            return Type.ERROR;
        }
        if (types.erased(rst).tag == Type.Tag.ARRAY) {
            if (name.equals("length")) {
                sel.sym = null;
                return Type.INT;
            }
            error(env, sel.pos, "cannot find symbol\n  symbol:   variable " + name + "\n  location: class " + rst);
            return Type.ERROR;
        }
        VarSymbol f = findField(rst, name);
        if (f == null) {
            error(env, sel.pos, "cannot find symbol\n  symbol:   variable " + name + "\n  location: "
                    + (sel.selected instanceof Tree.Ident && ((Tree.Ident) sel.selected).sym instanceof VarSymbol
                    ? "variable " + ((Tree.Ident) sel.selected).name + " of type " + rst : "class " + types.erased(rst)));
            return Type.ERROR;
        }
        sel.sym = f;
        checkAccess(f.flags, f.owner, env, sel.pos, name);
        ensureConstant(f);
        if (f.isStatic()) {
            sel.constant = f.constValue;
        }
        return fieldType(rst, f);
    }

    /** Is target the current class or an enclosing class with an instance reachable from env? */
    private boolean isEnclosingInstance(Env env, ClassSymbol target) {
        if (inStaticContext(env)) {
            return false;
        }
        for (ClassSymbol c = env.enclClass; c != null; c = c.outer) {
            if (c == target) {
                return true;
            }
            if (!c.hasOuterInstance) {
                return false;
            }
        }
        return false;
    }

    /** Private members are accessible within the same top-level class only. */
    private void checkAccess(int flags, ClassSymbol owner, Env env, int pos, String name) {
        if ((flags & Symbol.ACC_PRIVATE) == 0 || owner == null) {
            return;
        }
        if (outermost(owner) != outermost(env.enclClass)) {
            error(env, pos, name + " has private access in " + owner.javaName());
        }
    }

    static ClassSymbol outermost(ClassSymbol c) {
        while (c.outer != null) {
            c = c.outer;
        }
        return c;
    }

    private Type attribNewArray(Tree.NewArray na, Env env, Type pt) {
        Type elem;
        if (na.elemType != null) {
            elem = attribType(na.elemType, env);
        } else if (pt != null && types.resolveInference(pt).tag == Type.Tag.ARRAY) {
            elem = ((Type.ArrayType) types.resolveInference(pt)).elem;
        } else {
            error(env, na.pos, "illegal initializer for " + (pt == null ? "<none>" : pt.toString()));
            elem = Type.ERROR;
        }
        if (na.elems != null) {
            for (Tree e : na.elems) {
                Type et = attribExpr(e, env, elem);
                checkAssignable(e, et, elem, env);
            }
            return new Type.ArrayType(elem);
        }
        for (Tree d : na.dims) {
            Type dt = attribExpr(d, env, Type.INT);
            checkIndex(d, dt, env);
        }
        Type t = elem;
        for (int i = 0; i < na.dims.size() + na.extraDims; i++) {
            t = new Type.ArrayType(t);
        }
        if (elem.tag == Type.Tag.TYPEVAR || elem.tag == Type.Tag.CLASS && ((Type.ClassType) elem).isParameterized()) {
            error(env, na.pos, "generic array creation");
        }
        return t;
    }

    // ------------------------------------------------------------------ assignment

    /** Attributes an assignment target; reports when it is not a variable or is a final one. */
    private Type attribLhs(Tree lhs, Env env, boolean compound) {
        Tree inner = lhs;
        while (inner instanceof Tree.Parens) {
            inner = ((Tree.Parens) inner).expr;
        }
        Type t = attribExpr(inner, env, null);
        lhs.type = t;
        if (!(inner instanceof Tree.Ident || inner instanceof Tree.Select || inner instanceof Tree.ArrayAccess)) {
            error(env, lhs.pos, "unexpected type\n  required: variable\n  found:    value");
            return t;
        }
        Symbol sym = inner instanceof Tree.Ident ? ((Tree.Ident) inner).sym
                : inner instanceof Tree.Select ? ((Tree.Select) inner).sym : null;
        if (inner instanceof Tree.Select && ((Tree.Select) inner).sym == null && !(t.isErroneous())
                && "length".equals(((Tree.Select) inner).name)) {
            error(env, lhs.pos, "cannot assign a value to final variable length");
        }
        if (sym instanceof VarSymbol) {
            VarSymbol v = (VarSymbol) sym;
            v.assignCount++;
            if (v.isFinal()) {
                boolean ok = false;
                if (v.kind == VarSymbol.Kind.FIELD) {
                    // Final fields may be assigned in constructors/initializers of their own class (simple name or this.x).
                    boolean viaThis = inner instanceof Tree.Ident || inner instanceof Tree.Select
                            && ((Tree.Select) inner).selected instanceof Tree.Ident
                            && "this".equals(((Tree.Ident) ((Tree.Select) inner).selected).name);
                    Env m = env;
                    while (m != null && !m.methodBoundary && m.lambda == null && !m.classBoundary) {
                        m = m.outer;
                    }
                    ok = viaThis && m != null && m.methodBoundary && m.enclClass == v.owner && !compound
                            && (m.enclMethod == null ? m.isStatic == v.isStatic()
                            : m.enclMethod.isConstructor() && !v.isStatic());
                } else if (!v.hasInitializer && v.kind == VarSymbol.Kind.LOCAL && !compound) {
                    // A blank final local: Flow checks it is not assigned twice on one path.
                    ok = true;
                }
                if (!ok) {
                    String what = v.kind == VarSymbol.Kind.PARAM ? "final parameter " : "final variable ";
                    error(env, lhs.pos, "cannot assign a value to " + what + v.name);
                }
            }
            if (v.kind != VarSymbol.Kind.FIELD && isCapturedHere(v, env)) {
                error(env, lhs.pos, "local variables referenced from "
                        + (lambdaBetween(v, env) ? "a lambda expression" : "an inner class")
                        + " must be final or effectively final");
            }
        }
        return t;
    }

    /** Is local v declared outside the innermost lambda/local class around env? */
    private boolean isCapturedHere(VarSymbol v, Env env) {
        for (Env e = env; e != null; e = e.outer) {
            if (e.localHere(v.name) == v) {
                return false;
            }
            if (e.lambda != null || e.classBoundary) {
                return true;
            }
        }
        return false;
    }

    private boolean lambdaBetween(VarSymbol v, Env env) {
        for (Env e = env; e != null; e = e.outer) {
            if (e.localHere(v.name) == v) {
                return false;
            }
            if (e.lambda != null) {
                return true;
            }
            if (e.classBoundary) {
                return false;
            }
        }
        return false;
    }

    private Type attribAssign(Tree.Assign a, Env env) {
        Type lt = attribLhs(a.lhs, env, false);
        Type rt = attribExpr(a.rhs, env, lt);
        checkAssignable(a.rhs, rt, lt, env);
        return lt;
    }

    private Type attribCompoundAssign(Tree.CompoundAssign a, Env env) {
        Type lt = attribLhs(a.lhs, env, true);
        Type rt = attribExpr(a.rhs, env, null);
        if (lt.isErroneous() || rt.isErroneous()) {
            return lt;
        }
        Token.Kind op = binaryOf(a.op);
        if (op == Token.Kind.PLUS && isString(lt)) {
            if (rt.tag == Type.Tag.VOID) {
                error(env, a.rhs.pos, "'void' type not allowed here");
            }
            a.operandType = syms.stringType;
            return lt;
        }
        Type lu = types.unboxedOrSelf(lt);
        Type ru = types.unboxedOrSelf(rt);
        if (lu == null || ru == null) {
            error(env, a.pos, "bad operand types for binary operator '" + opName(op) + "'\n  first type:  " + lt + "\n  second type: " + rt);
            return lt;
        }
        Type operand = operandTypeFor(op, lu, ru);
        if (operand == null) {
            error(env, a.pos, "bad operand types for binary operator '" + opName(op) + "'\n  first type:  " + lt + "\n  second type: " + rt);
            return lt;
        }
        a.operandType = operand;
        return lt;
    }

    static Token.Kind binaryOf(Token.Kind compound) {
        switch (compound) {
            case PLUSEQ: return Token.Kind.PLUS;
            case SUBEQ: return Token.Kind.SUB;
            case STAREQ: return Token.Kind.STAR;
            case SLASHEQ: return Token.Kind.SLASH;
            case PERCENTEQ: return Token.Kind.PERCENT;
            case AMPEQ: return Token.Kind.AMP;
            case BAREQ: return Token.Kind.BAR;
            case CARETEQ: return Token.Kind.CARET;
            case LTLTEQ: return Token.Kind.LTLT;
            case GTGTEQ: return Token.Kind.GTGT;
            case GTGTGTEQ: return Token.Kind.GTGTGT;
            default: return compound;
        }
    }

    static String opName(Token.Kind op) {
        switch (op) {
            case PLUS: return "+";
            case SUB: return "-";
            case STAR: return "*";
            case SLASH: return "/";
            case PERCENT: return "%";
            case AMP: return "&";
            case BAR: return "|";
            case CARET: return "^";
            case LTLT: return "<<";
            case GTGT: return ">>";
            case GTGTGT: return ">>>";
            case LT: return "<";
            case GT: return ">";
            case LTEQ: return "<=";
            case GTEQ: return ">=";
            case EQEQ: return "==";
            case BANGEQ: return "!=";
            case AMPAMP: return "&&";
            case BARBAR: return "||";
            case BANG: return "!";
            case TILDE: return "~";
            case PLUSPLUS: return "++";
            case SUBSUB: return "--";
            default: return op.toString();
        }
    }

    boolean isString(Type t) {
        Type e = types.erased(t);
        return e.tag == Type.Tag.CLASS && ((Type.ClassType) e).sym == syms.stringSym;
    }

    /** The type a binary operator's (unboxed) operands are converted to, or null when not applicable. */
    private static Type operandTypeFor(Token.Kind op, Type l, Type r) {
        switch (op) {
            case PLUS:
            case SUB:
            case STAR:
            case SLASH:
            case PERCENT:
                if (l.isNumeric() && r.isNumeric()) {
                    return Types.binaryPromote(l, r);
                }
                return null;
            case LTLT:
            case GTGT:
            case GTGTGT:
                if (l.isIntegral() && r.isIntegral()) {
                    return Types.unaryPromote(l);
                }
                return null;
            case AMP:
            case BAR:
            case CARET:
                if (l.tag == Type.Tag.BOOLEAN && r.tag == Type.Tag.BOOLEAN) {
                    return Type.BOOLEAN;
                }
                if (l.isIntegral() && r.isIntegral()) {
                    return Types.binaryPromote(l, r);
                }
                return null;
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------ operators

    private Type attribUnary(Tree.Unary u, Env env) {
        if (u.op == Token.Kind.PLUSPLUS || u.op == Token.Kind.SUBSUB) {
            Type t = attribLhs(u.arg, env, true);
            Type un = types.unboxedOrSelf(t);
            if (!t.isErroneous() && (un == null || !un.isNumeric())) {
                error(env, u.pos, "bad operand type " + t + " for unary operator '" + opName(u.op) + "'");
                return Type.ERROR;
            }
            u.operandType = un;
            return t;
        }
        Type t = attribExpr(u.arg, env, null);
        if (t.isErroneous()) {
            return Type.ERROR;
        }
        Type un = types.unboxedOrSelf(t);
        switch (u.op) {
            case BANG:
                if (un == null || un.tag != Type.Tag.BOOLEAN) {
                    error(env, u.pos, "bad operand type " + t + " for unary operator '!'");
                    return Type.ERROR;
                }
                u.operandType = Type.BOOLEAN;
                if (u.arg.constant instanceof Boolean) {
                    u.constant = Boolean.valueOf(!((Boolean) u.arg.constant).booleanValue());
                }
                return Type.BOOLEAN;
            case TILDE:
                if (un == null || !un.isIntegral()) {
                    error(env, u.pos, "bad operand type " + t + " for unary operator '~'");
                    return Type.ERROR;
                }
                u.operandType = Types.unaryPromote(un);
                u.constant = Constants.unary(u.op, u.arg.constant, u.operandType);
                return u.operandType;
            default:
                if (un == null || !un.isNumeric()) {
                    error(env, u.pos, "bad operand type " + t + " for unary operator '" + opName(u.op) + "'");
                    return Type.ERROR;
                }
                u.operandType = Types.unaryPromote(un);
                u.constant = Constants.unary(u.op, u.arg.constant, u.operandType);
                return u.operandType;
        }
    }

    private Type attribBinary(Tree.Binary b, Env env) {
        Token.Kind op = b.op;
        Type lt = attribExpr(b.lhs, env, null);
        Type rt;
        if (op == Token.Kind.AMPAMP || op == Token.Kind.BARBAR) {
            Env renv = env.dup();
            enterBindings(renv, bindingsWhen(b.lhs, op == Token.Kind.AMPAMP));
            rt = attribExpr(b.rhs, renv, null);
        } else {
            rt = attribExpr(b.rhs, env, null);
        }
        if (lt.isErroneous() || rt.isErroneous()) {
            return Type.ERROR;
        }
        if (op == Token.Kind.PLUS && (isString(lt) || isString(rt))) {
            if (lt.tag == Type.Tag.VOID || rt.tag == Type.Tag.VOID) {
                error(env, b.pos, "'void' type not allowed here");
                return Type.ERROR;
            }
            b.operandType = syms.stringType;
            if (b.lhs.constant != null && b.rhs.constant != null) {
                b.constant = Constants.stringOf(b.lhs.constant, lt) + Constants.stringOf(b.rhs.constant, rt);
            }
            return syms.stringType;
        }
        Type lu = types.unboxedOrSelf(lt);
        Type ru = types.unboxedOrSelf(rt);
        switch (op) {
            case AMPAMP:
            case BARBAR:
                if (lu == null || ru == null || lu.tag != Type.Tag.BOOLEAN || ru.tag != Type.Tag.BOOLEAN) {
                    return badOperands(b, lt, rt, env);
                }
                b.operandType = Type.BOOLEAN;
                b.constant = Constants.binary(op, b.lhs.constant, b.rhs.constant, Type.BOOLEAN);
                return Type.BOOLEAN;
            case EQEQ:
            case BANGEQ: {
                boolean lPrim = lt.isPrimitive();
                boolean rPrim = rt.isPrimitive();
                if ((lPrim || rPrim) && lu != null && ru != null) {
                    if (lu.isNumeric() && ru.isNumeric()) {
                        b.operandType = Types.binaryPromote(lu, ru);
                    } else if (lu.tag == Type.Tag.BOOLEAN && ru.tag == Type.Tag.BOOLEAN) {
                        b.operandType = Type.BOOLEAN;
                    } else {
                        return badOperands(b, lt, rt, env);
                    }
                    b.constant = Constants.binary(op, b.lhs.constant, b.rhs.constant, b.operandType);
                    return Type.BOOLEAN;
                }
                if (lPrim || rPrim) {
                    if (lPrim && rt.tag == Type.Tag.NULL || rPrim && lt.tag == Type.Tag.NULL) {
                        return badOperands(b, lt, rt, env);
                    }
                    return badOperands(b, lt, rt, env);
                }
                if (!types.isCastable(lt, rt) && !types.isCastable(rt, lt)) {
                    error(env, b.pos, "incomparable types: " + lt + " and " + rt);
                    return Type.BOOLEAN;
                }
                b.operandType = syms.objectType;
                if (isString(lt) && isString(rt) && b.lhs.constant != null && b.rhs.constant != null) {
                    b.constant = Boolean.valueOf(b.lhs.constant.equals(b.rhs.constant) == (op == Token.Kind.EQEQ));
                }
                return Type.BOOLEAN;
            }
            case LT:
            case GT:
            case LTEQ:
            case GTEQ:
                if (lu == null || ru == null || !lu.isNumeric() || !ru.isNumeric()) {
                    return badOperands(b, lt, rt, env);
                }
                b.operandType = Types.binaryPromote(lu, ru);
                b.constant = Constants.binary(op, b.lhs.constant, b.rhs.constant, b.operandType);
                return Type.BOOLEAN;
            default: {
                if (lu == null || ru == null) {
                    return badOperands(b, lt, rt, env);
                }
                Type operand = operandTypeFor(op, lu, ru);
                if (operand == null) {
                    return badOperands(b, lt, rt, env);
                }
                b.operandType = operand;
                b.constant = Constants.binary(op, b.lhs.constant, b.rhs.constant, operand);
                if ((op == Token.Kind.SLASH || op == Token.Kind.PERCENT) && operand.isIntegral()
                        && b.rhs.constant != null && Constants.isZero(b.rhs.constant)) {
                    compiler.warning(env.unit, b.pos, "division by zero");
                }
                return operand;
            }
        }
    }

    private Type badOperands(Tree.Binary b, Type lt, Type rt, Env env) {
        error(env, b.pos, "bad operand types for binary operator '" + opName(b.op) + "'\n  first type:  " + lt + "\n  second type: " + rt);
        return Type.ERROR;
    }

    private Type attribConditional(Tree.Conditional c, Env env, Type pt) {
        attribCond(c.cond, env);
        Env tenv = env.dup();
        enterBindings(tenv, bindingsWhen(c.cond, true));
        Env fenv = env.dup();
        enterBindings(fenv, bindingsWhen(c.cond, false));
        boolean poly = pt != null && !pt.isPrimitive() && (isPolyShaped(c.truePart) || isPolyShaped(c.falsePart));
        Type tt = attribExpr(c.truePart, tenv, pt);
        Type ft = attribExpr(c.falsePart, fenv, pt);
        if (tt.isErroneous() || ft.isErroneous()) {
            return Type.ERROR;
        }
        Type result = conditionalType(c, tt, ft, pt, poly, env);
        if (c.cond.constant instanceof Boolean && c.truePart.constant != null && c.falsePart.constant != null
                && result != null && isConstantType(result)) {
            Object chosen = ((Boolean) c.cond.constant).booleanValue() ? c.truePart.constant : c.falsePart.constant;
            c.constant = convertConstant(chosen, result);
        }
        return result;
    }

    private static boolean isPolyShaped(Tree t) {
        while (t instanceof Tree.Parens) {
            t = ((Tree.Parens) t).expr;
        }
        return t instanceof Tree.Lambda || t instanceof Tree.MethodRef || t instanceof Tree.MethodCall
                || t instanceof Tree.NewClass || t instanceof Tree.Conditional || t instanceof Tree.Switch;
    }

    private Type conditionalType(Tree.Conditional c, Type tt, Type ft, Type pt, boolean poly, Env env) {
        if (tt.tag == Type.Tag.VOID || ft.tag == Type.Tag.VOID) {
            error(env, c.pos, "incompatible types: bad type in conditional expression\n    void cannot be converted to " + (pt == null ? "Object" : pt.toString()));
            return Type.ERROR;
        }
        Type tu = types.unboxedOrSelf(tt);
        Type fu = types.unboxedOrSelf(ft);
        if (types.isSameType(tt, ft) && !Types.containsInference(tt)) {
            return tt;
        }
        if (tu != null && fu != null && (tt.isPrimitive() || ft.isPrimitive() || pt == null || pt.isPrimitive())) {
            if (tu.tag == Type.Tag.BOOLEAN && fu.tag == Type.Tag.BOOLEAN) {
                return Type.BOOLEAN;
            }
            if (tu.isNumeric() && fu.isNumeric()) {
                if (tu.tag == fu.tag) {
                    return tu;
                }
                // byte/short/char with an int constant that fits.
                if (isNarrowTarget(tu) && fu.tag == Type.Tag.INT && c.falsePart.constant != null
                        && Types.fitsConstant(c.falsePart.constant, tu)) {
                    return tu;
                }
                if (isNarrowTarget(fu) && tu.tag == Type.Tag.INT && c.truePart.constant != null
                        && Types.fitsConstant(c.truePart.constant, fu)) {
                    return fu;
                }
                if (tu.tag == Type.Tag.BYTE && fu.tag == Type.Tag.SHORT || tu.tag == Type.Tag.SHORT && fu.tag == Type.Tag.BYTE) {
                    return Type.SHORT;
                }
                return Types.binaryPromote(tu, fu);
            }
        }
        if (poly || pt != null && !pt.isPrimitive() && !Types.containsInference(pt)) {
            if (pt != null) {
                return pt;
            }
        }
        if (tt.tag == Type.Tag.NULL) {
            return ft.isPrimitive() ? syms.boxedType(ft) : ft;
        }
        if (ft.tag == Type.Tag.NULL) {
            return tt.isPrimitive() ? syms.boxedType(tt) : tt;
        }
        List<Type> both = new ArrayList<Type>();
        both.add(tt);
        both.add(ft);
        return types.lub(both);
    }

    private static boolean isNarrowTarget(Type t) {
        return t.tag == Type.Tag.BYTE || t.tag == Type.Tag.SHORT || t.tag == Type.Tag.CHAR;
    }

    private Type attribInstanceOf(Tree.InstanceOf io, Env env) {
        Type et = attribExpr(io.expr, env, null);
        if (et.isPrimitive()) {
            error(env, io.expr.pos, "unexpected type\n  required: reference\n  found:    " + et);
            return Type.BOOLEAN;
        }
        if (io.pattern instanceof Tree.BindingPattern || io.pattern instanceof Tree.RecordPattern) {
            Type pt = attribPattern(io.pattern, et, env);
            if (io.pattern instanceof Tree.BindingPattern && !et.isErroneous() && !pt.isErroneous()
                    && types.isSubtype(types.erased(et), types.erased(pt)) && types.erased(et).tag != Type.Tag.NULL
                    && compiler.sourceLevel < 21) {
                // Java 21 allows an unconditional pattern in instanceof; Java 17 rejected it.
                error(env, io.pos, "expression type " + et + " is a subtype of pattern type " + pt);
            }
        } else {
            Type tt = attribType(io.pattern, env);
            if (!et.isErroneous() && !tt.isErroneous() && !types.isCastable(et, tt)) {
                error(env, io.pos, "incompatible types: " + et + " cannot be converted to " + tt);
            }
        }
        return Type.BOOLEAN;
    }

    private Type attribCast(Tree.Cast c, Env env) {
        Type ct = attribType(c.clazz, env);
        Type et = attribExpr(c.expr, env, ct);
        if (ct.isErroneous() || et.isErroneous()) {
            return ct;
        }
        if (c.expr instanceof Tree.Lambda || c.expr instanceof Tree.MethodRef) {
            return ct;
        }
        if (et.tag == Type.Tag.VOID || !types.isCastable(et, ct)) {
            error(env, c.pos, "incompatible types: " + et + " cannot be converted to " + ct);
            return ct;
        }
        if (c.expr.constant != null && isConstantType(ct)) {
            c.constant = Constants.cast(c.expr.constant, ct);
        }
        return ct;
    }

    // ------------------------------------------------------------------ conditions and bindings

    void attribCond(Tree cond, Env env) {
        Type t = attribExpr(cond, env, Type.BOOLEAN);
        if (t.isErroneous()) {
            return;
        }
        Type u = types.unboxedOrSelf(t);
        if (u == null || u.tag != Type.Tag.BOOLEAN) {
            error(env, cond.pos, "incompatible types: " + t + " cannot be converted to boolean");
        }
    }

    /** The pattern variables in scope where cond evaluated to {@code when}. */
    List<VarSymbol> bindingsWhen(Tree cond, boolean when) {
        List<VarSymbol> out = new ArrayList<VarSymbol>();
        collectBindings(cond, when, out);
        return out;
    }

    private void collectBindings(Tree t, boolean when, List<VarSymbol> out) {
        if (t instanceof Tree.Parens) {
            collectBindings(((Tree.Parens) t).expr, when, out);
        } else if (t instanceof Tree.Unary && ((Tree.Unary) t).op == Token.Kind.BANG) {
            collectBindings(((Tree.Unary) t).arg, !when, out);
        } else if (t instanceof Tree.Binary) {
            Tree.Binary b = (Tree.Binary) t;
            if (b.op == Token.Kind.AMPAMP && when || b.op == Token.Kind.BARBAR && !when) {
                collectBindings(b.lhs, when, out);
                collectBindings(b.rhs, when, out);
            }
        } else if (t instanceof Tree.InstanceOf && when) {
            patternBindings(((Tree.InstanceOf) t).pattern, out);
        }
    }

    static void patternBindings(Tree p, List<VarSymbol> out) {
        if (p instanceof Tree.BindingPattern) {
            VarSymbol v = ((Tree.BindingPattern) p).var.sym;
            if (v != null) {
                out.add(v);
            }
        } else if (p instanceof Tree.RecordPattern) {
            for (Tree n : ((Tree.RecordPattern) p).nested) {
                patternBindings(n, out);
            }
        }
    }

    void enterBindings(Env env, List<VarSymbol> vars) {
        for (VarSymbol v : vars) {
            if (env.localHere(v.name) == v) {
                continue;
            }
            env.enterLocal(v);
        }
    }

    /** Attributes a pattern matched against a value of type target; returns the pattern's type. */
    Type attribPattern(Tree p, Type target, Env env) {
        if (p instanceof Tree.BindingPattern) {
            Tree.VarDef v = ((Tree.BindingPattern) p).var;
            Type t = v.vartype == null ? upward(target) : attribType(v.vartype, env);
            if (v.vartype != null && !target.isErroneous() && !t.isErroneous() && !types.isCastable(target, t)) {
                error(env, p.pos, "incompatible types: " + target + " cannot be converted to " + t);
            }
            if (t.isPrimitive() && v.vartype != null && !target.isPrimitive()) {
                error(env, p.pos, "unexpected type\n  required: class or array\n  found:    " + t);
            }
            checkLocalRedeclared(v.name, env, v.pos);
            VarSymbol sym = new VarSymbol(v.name, v.mods == null ? 0 : v.mods.flags & Symbol.ACC_FINAL, t, VarSymbol.Kind.LOCAL);
            sym.hasInitializer = true;
            v.sym = sym;
            v.type = t;
            p.type = t;
            return t;
        }
        if (p instanceof Tree.RecordPattern) {
            Tree.RecordPattern rp = (Tree.RecordPattern) p;
            Type dt = attribType(rp.deconstructor, env);
            if (dt.tag != Type.Tag.CLASS || !((Type.ClassType) dt).sym.isRecord()) {
                if (!dt.isErroneous()) {
                    error(env, rp.pos, dt + " is not a record class");
                }
                p.type = Type.ERROR;
                return Type.ERROR;
            }
            Type.ClassType rt = (Type.ClassType) dt;
            ClassSymbol rec = rt.sym;
            rp.record = rec;
            if (rt.isRaw()) {
                Type.ClassType view = types.asSuper(target, rec);
                if (view != null && !view.args.isEmpty()) {
                    rt = view;
                }
            }
            if (!target.isErroneous() && !types.isCastable(target, rt)) {
                error(env, rp.pos, "incompatible types: " + target + " cannot be converted to " + rt);
            }
            List<VarSymbol> comps = rec.recordComponents;
            if (comps.size() != rp.nested.size()) {
                error(env, rp.pos, "incorrect number of nested patterns\n  required: " + comps.size() + "\n  found: " + rp.nested.size());
            }
            java.util.Map<Type.TypeVar, Type> map = types.memberMapping(rt, rec);
            for (int i = 0; i < rp.nested.size(); i++) {
                Type ct = i < comps.size() ? Type.substitute(comps.get(i).type, map) : Type.ERROR;
                if (ct.tag == Type.Tag.WILDCARD) {
                    ct = types.upperBound(ct);
                }
                attribPattern(rp.nested.get(i), ct, env);
            }
            p.type = rt;
            return rt;
        }
        return attribType(p, env);
    }

    // ------------------------------------------------------------------ assignability

    void checkAssignable(Tree tree, Type found, Type req, Env env) {
        if (found == null || req == null || found.isErroneous() || req.isErroneous()) {
            return;
        }
        Tree inner = tree;
        while (inner instanceof Tree.Parens) {
            inner = ((Tree.Parens) inner).expr;
        }
        if (inner instanceof Tree.Lambda || inner instanceof Tree.MethodRef) {
            return;
        }
        if (found.tag == Type.Tag.VOID) {
            error(env, tree.pos, "incompatible types: void cannot be converted to " + req);
            return;
        }
        if (types.isAssignable(types.resolveInference(found), req, true, tree.constant)) {
            return;
        }
        Type fu = types.unboxedOrSelf(found);
        if (found.isPrimitive() && req.isPrimitive() && found.isNumeric() && req.isNumeric()) {
            error(env, tree.pos, "incompatible types: possible lossy conversion from " + found + " to " + req);
        } else if (fu != null && fu.isNumeric() && req.isPrimitive() && req.isNumeric() && !found.isPrimitive()) {
            error(env, tree.pos, "incompatible types: " + found + " cannot be converted to " + req);
        } else {
            error(env, tree.pos, "incompatible types: " + types.resolveInference(found) + " cannot be converted to " + req);
        }
    }

    /** Converts a constant to the representation of type t (int-like as Integer, char as Character...). */
    static Object convertConstant(Object v, Type t) {
        return Constants.cast(v, t);
    }

    // ================================================================== method resolution and inference

    /** A selected method with the substitution that instantiates it at one call site. */
    static final class Resolution {
        MethodSymbol method;
        boolean varargs;
        java.util.Map<Type.TypeVar, Type> map;
        List<Type.InferenceVar> ivars = new ArrayList<Type.InferenceVar>();
        /** For a diamond: the class type with inference variables for its arguments. */
        Type.ClassType diamondType;
    }

    /** All methods named name that are members of site: declared or inherited, minus overridden ones. */
    List<MethodSymbol> collectMethods(Type site, String name) {
        List<MethodSymbol> out = new ArrayList<MethodSymbol>();
        collectMethods(site, name, out, new LinkedHashSet<ClassSymbol>());
        return out;
    }

    private void collectMethods(Type site, String name, List<MethodSymbol> out, Set<ClassSymbol> seen) {
        List<ClassSymbol> order = new ArrayList<ClassSymbol>();
        linearize(site, order, seen);
        if (order.isEmpty() || !seen.contains(syms.objectSym)) {
            if (seen.add(syms.objectSym)) {
                order.add(syms.objectSym);
            }
        }
        for (ClassSymbol k : order) {
            for (MethodSymbol m : k.methods()) {
                if (m.name.equals(name) && !m.isConstructor() && !isOverridden(out, m, site)) {
                    if (k == syms.objectSym && k != firstClass(order) && !m.isPublic() && !m.isProtected()) {
                        continue;
                    }
                    out.add(m);
                }
            }
        }
    }

    private static ClassSymbol firstClass(List<ClassSymbol> order) {
        return order.isEmpty() ? null : order.get(0);
    }

    /** Classes in member-lookup order: the class and its superclasses, then interfaces breadth first. */
    private void linearize(Type site, List<ClassSymbol> order, Set<ClassSymbol> seen) {
        if (site == null) {
            return;
        }
        Type r = types.resolveInference(site);
        if (r.tag == Type.Tag.TYPEVAR) {
            linearize(((Type.TypeVar) r).bound == null ? syms.objectType : ((Type.TypeVar) r).bound, order, seen);
            return;
        }
        if (r.tag == Type.Tag.INTERSECTION) {
            for (Type b : ((Type.IntersectionType) r).bounds) {
                linearize(b, order, seen);
            }
            return;
        }
        if (r.tag == Type.Tag.WILDCARD) {
            linearize(types.upperBound(r), order, seen);
            return;
        }
        Type e = types.erased(r);
        if (e.tag != Type.Tag.CLASS) {
            return;
        }
        List<ClassSymbol> itfs = new ArrayList<ClassSymbol>();
        for (ClassSymbol k = ((Type.ClassType) e).sym; k != null; ) {
            if (seen.add(k)) {
                order.add(k);
            }
            for (Type i : k.interfaces()) {
                if (i.tag == Type.Tag.CLASS) {
                    itfs.add(((Type.ClassType) i).sym);
                }
            }
            Type sup = k.superclass();
            k = sup != null && sup.tag == Type.Tag.CLASS ? ((Type.ClassType) sup).sym : null;
        }
        for (int i = 0; i < itfs.size(); i++) {
            ClassSymbol k = itfs.get(i);
            if (seen.add(k)) {
                order.add(k);
            }
            for (Type j : k.interfaces()) {
                if (j.tag == Type.Tag.CLASS && !itfs.contains(((Type.ClassType) j).sym)) {
                    itfs.add(((Type.ClassType) j).sym);
                }
            }
        }
    }

    /** Is m overridden (or hidden) by a method already collected, compared as instantiated in site? */
    private boolean isOverridden(List<MethodSymbol> collected, MethodSymbol m, Type site) {
        for (MethodSymbol x : collected) {
            if (x.params.size() != m.params.size() || !x.name.equals(m.name)) {
                continue;
            }
            if (sameSignatureIn(x, m, site)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameSignatureIn(MethodSymbol a, MethodSymbol b, Type site) {
        java.util.Map<Type.TypeVar, Type> ma = site == null ? new java.util.IdentityHashMap<Type.TypeVar, Type>() : types.memberMapping(site, a.owner);
        java.util.Map<Type.TypeVar, Type> mb = site == null ? new java.util.IdentityHashMap<Type.TypeVar, Type>() : types.memberMapping(site, b.owner);
        boolean sameErasure = true;
        boolean sameInstantiated = true;
        for (int i = 0; i < a.params.size(); i++) {
            String ea = Types.descriptor(types.erased(a.params.get(i)));
            String eb = Types.descriptor(types.erased(b.params.get(i)));
            if (!ea.equals(eb)) {
                sameErasure = false;
            }
            String ia = Types.descriptor(types.erased(Type.substitute(a.params.get(i), ma)));
            String ib = Types.descriptor(types.erased(Type.substitute(b.params.get(i), mb)));
            if (!ia.equals(ib)) {
                sameInstantiated = false;
            }
        }
        return sameErasure || sameInstantiated;
    }

    /** The i-th formal parameter type, expanded to the component type for a variable-arity invocation. */
    private Type formal(MethodSymbol m, int i, boolean varargs) {
        int n = m.params.size();
        if (varargs && i >= n - 1) {
            Type last = m.params.get(n - 1);
            Type rl = last;
            if (rl.tag == Type.Tag.ARRAY) {
                return ((Type.ArrayType) rl).elem;
            }
            return Type.ERROR;
        }
        return i < n ? m.params.get(i) : Type.ERROR;
    }

    /**
     * Checks one candidate in one phase (1 strict, 2 loose, 3 variable arity). Arguments
     * whose type is null are deferred (lambdas, or poly expressions with a single
     * candidate) and only checked for potential compatibility.
     */
    private Resolution tryCandidate(MethodSymbol m, Type site, List<Tree> args, List<Type> argTypes, int phase,
            List<Type> explicitTypeArgs, Env env, Type.ClassType diamondOf) {
        int n = args.size();
        int p = m.params.size();
        if (phase < 3) {
            if (n != p) {
                return null;
            }
        } else if (!m.isVarargs() || n < p - 1) {
            return null;
        }
        Resolution r = new Resolution();
        r.method = m;
        r.varargs = phase == 3;
        java.util.Map<Type.TypeVar, Type> map;
        if (diamondOf != null) {
            map = new java.util.IdentityHashMap<Type.TypeVar, Type>();
            List<Type> ivArgs = new ArrayList<Type>();
            for (Type.TypeVar tv : diamondOf.sym.typeParams()) {
                Type.InferenceVar iv = new Type.InferenceVar(tv);
                map.put(tv, iv);
                r.ivars.add(iv);
                ivArgs.add(iv);
            }
            r.diamondType = new Type.ClassType(diamondOf.sym, ivArgs, diamondOf.outer);
        } else if (site != null && (!m.isStatic() || m.isConstructor())) {
            map = types.memberMapping(site, m.owner);
        } else {
            map = new java.util.IdentityHashMap<Type.TypeVar, Type>();
        }
        if (explicitTypeArgs != null && !explicitTypeArgs.isEmpty() && explicitTypeArgs.size() == m.typeParams.size()) {
            for (int i = 0; i < m.typeParams.size(); i++) {
                map.put(m.typeParams.get(i), explicitTypeArgs.get(i));
            }
        } else {
            for (Type.TypeVar tv : m.typeParams) {
                Type.InferenceVar iv = new Type.InferenceVar(tv);
                map.put(tv, iv);
                r.ivars.add(iv);
            }
        }
        r.map = map;
        for (int i = 0; i < n; i++) {
            Type f = Type.substitute(formal(m, i, phase == 3), map);
            Type at = argTypes.get(i);
            Tree a = args.get(i);
            if (at == null) {
                if (!potentiallyCompatible(a, f, env)) {
                    return null;
                }
                continue;
            }
            if (at.isErroneous()) {
                continue;
            }
            // Invocation contexts allow no constant narrowing (unlike assignment): f(1) never picks f(char).
            if (!types.isAssignable(at, f, phase >= 2, null)) {
                return null;
            }
        }
        // A plain varargs call with an array in last position (phase 1/2) is not variable-arity.
        return r;
    }

    private static Tree strip(Tree t) {
        while (t instanceof Tree.Parens) {
            t = ((Tree.Parens) t).expr;
        }
        return t;
    }

    static boolean isLambdaLike(Tree t) {
        t = strip(t);
        if (t instanceof Tree.Lambda || t instanceof Tree.MethodRef) {
            return true;
        }
        if (t instanceof Tree.Conditional) {
            return isLambdaLike(((Tree.Conditional) t).truePart) || isLambdaLike(((Tree.Conditional) t).falsePart);
        }
        return false;
    }

    private static boolean isTargetSensitive(Tree t) {
        t = strip(t);
        if (t instanceof Tree.MethodCall) {
            return true;
        }
        if (t instanceof Tree.NewClass) {
            Tree c = ((Tree.NewClass) t).clazz;
            return c instanceof Tree.TypeApply && ((Tree.TypeApply) c).args.isEmpty();
        }
        if (t instanceof Tree.Conditional) {
            return isTargetSensitive(((Tree.Conditional) t).truePart) || isTargetSensitive(((Tree.Conditional) t).falsePart);
        }
        if (t instanceof Tree.Switch) {
            return true;
        }
        return false;
    }

    /** Could a deferred argument (lambda, method reference) be compatible with formal f? */
    private boolean potentiallyCompatible(Tree a, Type f, Env env) {
        a = strip(a);
        if (a instanceof Tree.Conditional) {
            Tree.Conditional c = (Tree.Conditional) a;
            return (!isLambdaLike(c.truePart) || potentiallyCompatible(c.truePart, f, env))
                    && (!isLambdaLike(c.falsePart) || potentiallyCompatible(c.falsePart, f, env));
        }
        if (!(a instanceof Tree.Lambda || a instanceof Tree.MethodRef)) {
            // A deferred poly expression (single candidate): checked once its target is known.
            return true;
        }
        Type rf = types.resolveInference(f);
        if (rf.tag == Type.Tag.INFERENCE || rf.tag == Type.Tag.TYPEVAR) {
            return false;
        }
        Type e = types.erased(rf);
        if (e.tag != Type.Tag.CLASS) {
            return false;
        }
        MethodSymbol sam = types.functionalMethod(((Type.ClassType) e).sym);
        if (sam == null) {
            return false;
        }
        if (a instanceof Tree.MethodRef) {
            return methodRefPotentiallyCompatible((Tree.MethodRef) a, sam, env);
        }
        Tree.Lambda l = (Tree.Lambda) a;
        if (l.params.size() != sam.params.size()) {
            return false;
        }
        boolean voidSam = sam.returnType.tag == Type.Tag.VOID;
        if (l.body instanceof Tree.Block) {
            boolean valueReturn = hasValueReturn(l.body);
            if (voidSam) {
                return !valueReturn;
            }
            return valueReturn || !canCompleteNormally(l.body);
        }
        Tree body = strip(l.body);
        boolean statementExpr = body instanceof Tree.MethodCall || body instanceof Tree.NewClass || body instanceof Tree.Assign
                || body instanceof Tree.CompoundAssign || body instanceof Tree.Unary
                && (((Tree.Unary) body).op == Token.Kind.PLUSPLUS || ((Tree.Unary) body).op == Token.Kind.SUBSUB);
        if (voidSam) {
            return statementExpr && !(l.body instanceof Tree.Parens);
        }
        if (body instanceof Tree.MethodCall && (l.params.isEmpty() || l.explicitParams)) {
            Type bt = speculativeType(l, sam, env);
            if (bt != null && bt.tag == Type.Tag.VOID) {
                return false;
            }
        }
        return true;
    }

    private boolean methodRefPotentiallyCompatible(Tree.MethodRef r, MethodSymbol sam, Env env) {
        if (r.name.equals("<init>")) {
            return true;
        }
        int n = sam.params.size();
        Object q = r.qualifierIsType ? null : quietQualifier(r.qualifier, env);
        Type site;
        boolean typeQualified;
        if (r.qualifierIsType || q instanceof Type) {
            site = q instanceof Type ? (Type) q : null;
            typeQualified = true;
        } else {
            site = null;
            typeQualified = false;
        }
        if (site == null) {
            return true;
        }
        for (MethodSymbol m : collectMethods(site, r.name)) {
            int p = m.params.size();
            boolean arityOk = p == n || m.isVarargs() && n >= p - 1;
            boolean unboundOk = typeQualified && !m.isStatic() && (p == n - 1 || m.isVarargs() && n - 1 >= p - 1);
            if (arityOk || unboundOk) {
                return true;
            }
        }
        return false;
    }

    private Object quietQualifier(Tree q, Env env) {
        compiler.quiet++;
        try {
            if (q instanceof Tree.Ident && ("this".equals(((Tree.Ident) q).name) || "super".equals(((Tree.Ident) q).name))) {
                return null;
            }
            return resolveQualifier(q, env);
        } finally {
            compiler.quiet--;
        }
    }

    private static boolean hasValueReturn(Tree t) {
        if (t instanceof Tree.Return) {
            return ((Tree.Return) t).expr != null;
        }
        if (t instanceof Tree.Lambda || t instanceof Tree.ClassDecl || t instanceof Tree.NewClass) {
            return false;
        }
        if (t instanceof Tree.Block) {
            for (Tree s : ((Tree.Block) t).stats) {
                if (hasValueReturn(s)) {
                    return true;
                }
            }
            return false;
        }
        if (t instanceof Tree.If) {
            return hasValueReturn(((Tree.If) t).thenPart) || ((Tree.If) t).elsePart != null && hasValueReturn(((Tree.If) t).elsePart);
        }
        if (t instanceof Tree.WhileLoop) {
            return hasValueReturn(((Tree.WhileLoop) t).body);
        }
        if (t instanceof Tree.DoLoop) {
            return hasValueReturn(((Tree.DoLoop) t).body);
        }
        if (t instanceof Tree.ForLoop) {
            return hasValueReturn(((Tree.ForLoop) t).body);
        }
        if (t instanceof Tree.ForEach) {
            return hasValueReturn(((Tree.ForEach) t).body);
        }
        if (t instanceof Tree.Labeled) {
            return hasValueReturn(((Tree.Labeled) t).body);
        }
        if (t instanceof Tree.Synchronized) {
            return hasValueReturn(((Tree.Synchronized) t).body);
        }
        if (t instanceof Tree.Try) {
            Tree.Try tr = (Tree.Try) t;
            if (hasValueReturn(tr.body) || tr.finalizer != null && hasValueReturn(tr.finalizer)) {
                return true;
            }
            for (Tree.Catch c : tr.catches) {
                if (hasValueReturn(c.body)) {
                    return true;
                }
            }
            return false;
        }
        if (t instanceof Tree.Switch && !((Tree.Switch) t).isExpression) {
            for (Tree.Case c : ((Tree.Switch) t).cases) {
                for (Tree s : c.stats) {
                    if (hasValueReturn(s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** The type of an expression-bodied lambda's body, attributed speculatively with errors suppressed. */
    private Type speculativeType(Tree.Lambda l, MethodSymbol sam, Env env) {
        if (containsClassOrLambda(l.body)) {
            return null;
        }
        compiler.quiet++;
        try {
            Tree.Lambda fake = new Tree.Lambda();
            Env le = env.dup();
            le.lambda = fake;
            le.breakable = false;
            le.continuable = false;
            for (Tree.VarDef p : l.params) {
                Type t = p.declaredType != null ? p.declaredType : p.vartype != null ? attribType(p.vartype, env) : null;
                if (t == null) {
                    return null;
                }
                VarSymbol v = new VarSymbol(p.name, 0, t, VarSymbol.Kind.PARAM);
                le.enterLocal(v);
            }
            return attribExpr(l.body, le, null);
        } finally {
            compiler.quiet--;
        }
    }

    private static boolean containsClassOrLambda(Tree t) {
        final boolean[] found = {false};
        new TreeScanner() {
            @Override
            void scan(Tree x) {
                if (x instanceof Tree.Lambda || x instanceof Tree.MethodRef || x instanceof Tree.ClassDecl
                        || x instanceof Tree.NewClass && ((Tree.NewClass) x).body != null || x instanceof Tree.Switch) {
                    found[0] = true;
                    return;
                }
                super.scan(x);
            }
        }.scan(t);
        return found[0];
    }

    /** Picks the applicable candidate, phase by phase; null (after reporting unless quiet) when none. */
    private Resolution selectMethod(List<MethodSymbol> candidates, Type site, String name, List<Tree> args,
            List<Type> argTypes, List<Type> typeArgs, Env env, int pos, boolean quiet, Type.ClassType diamondOf,
            boolean isCtor, ClassSymbol where) {
        for (int phase = 1; phase <= 3; phase++) {
            List<Resolution> applicable = new ArrayList<Resolution>();
            for (MethodSymbol m : candidates) {
                Resolution r = tryCandidate(m, site, args, argTypes, phase, typeArgs, env, diamondOf);
                if (r != null) {
                    applicable.add(r);
                }
            }
            if (applicable.isEmpty()) {
                continue;
            }
            if (applicable.size() == 1) {
                return applicable.get(0);
            }
            Resolution best = mostSpecific(applicable, args, phase == 3);
            if (best != null) {
                return best;
            }
            if (!quiet && !anyErroneous(argTypes)) {
                MethodSymbol a = applicable.get(0).method;
                MethodSymbol b = applicable.get(1).method;
                error(env, pos, "reference to " + (isCtor ? where.simpleName : name) + " is ambiguous\n  both "
                        + describe(a) + " and " + describe(b) + " match");
            }
            return applicable.get(0);
        }
        if (!quiet && !anyErroneous(argTypes)) {
            reportInapplicable(candidates, name, args, argTypes, env, pos, isCtor, where);
        }
        return null;
    }

    private static boolean anyErroneous(List<Type> argTypes) {
        for (Type t : argTypes) {
            if (t != null && t.isErroneous()) {
                return true;
            }
        }
        return false;
    }

    private String describe(MethodSymbol m) {
        return (m.isConstructor() ? "constructor " + m : "method " + m) + " in " + m.owner.javaName();
    }

    private void reportInapplicable(List<MethodSymbol> candidates, String name, List<Tree> args, List<Type> argTypes,
            Env env, int pos, boolean isCtor, ClassSymbol where) {
        StringBuilder found = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                found.append(',');
            }
            Type t = argTypes.get(i);
            found.append(t == null ? describeDeferred(args.get(i)) : t.tag == Type.Tag.NULL ? "<null>" : t.toString());
        }
        String foundText = args.isEmpty() ? "no arguments" : found.toString();
        if (candidates.isEmpty()) {
            if (isCtor) {
                error(env, pos, "cannot find symbol\n  symbol:   constructor " + where.simpleName + "(" + found + ")");
            } else {
                error(env, pos, "cannot find symbol\n  symbol:   method " + name + "(" + found + ")\n  location: "
                        + (where == null ? "class " + env.enclClass.javaName() : (where.isInterface() ? "interface " : "class ") + where.javaName()));
            }
            return;
        }
        if (candidates.size() == 1) {
            MethodSymbol m = candidates.get(0);
            // A lambda whose arity does not fit the functional interface: javac names the lambda.
            for (int i = 0; i < args.size() && i < m.params.size(); i++) {
                Tree a = strip(args.get(i));
                if (a instanceof Tree.Lambda) {
                    Type f = types.erased(m.params.get(i));
                    MethodSymbol sam = f.tag == Type.Tag.CLASS ? types.functionalMethod(((Type.ClassType) f).sym) : null;
                    if (sam != null && sam.params.size() != ((Tree.Lambda) a).params.size()) {
                        error(env, a.pos, "incompatible types: incompatible parameter types in lambda expression");
                        return;
                    }
                }
            }
            StringBuilder req = new StringBuilder();
            for (int i = 0; i < m.params.size(); i++) {
                if (i > 0) {
                    req.append(',');
                }
                req.append(m.params.get(i));
            }
            String kind = m.isConstructor() ? "constructor " + m.owner.simpleName + " in " + (m.owner.isEnum() ? "enum " : "class ")
                    + m.owner.javaName() : "method " + m.name + " in " + (m.owner.isInterface() ? "interface " : "class ") + m.owner.javaName();
            String reason;
            if (m.params.size() != args.size() && !m.isVarargs()) {
                reason = "actual and formal argument lists differ in length";
            } else {
                reason = "argument mismatch";
                for (int i = 0; i < args.size() && i < m.params.size(); i++) {
                    Type at = argTypes.get(i);
                    if (at != null && !types.isAssignable(at, types.erased(m.params.get(i)), true, args.get(i).constant)) {
                        Type pi = m.params.get(i);
                        if (at.isPrimitive() && pi.isPrimitive() && at.isNumeric() && pi.isNumeric()) {
                            reason = "argument mismatch; possible lossy conversion from " + at + " to " + pi;
                        } else {
                            reason = "argument mismatch; " + (at.tag == Type.Tag.NULL ? "<null>" : at.toString()) + " cannot be converted to " + pi;
                        }
                        break;
                    }
                }
            }
            error(env, pos, kind + " cannot be applied to given types;\n  required: "
                    + (req.length() == 0 ? "no arguments" : req.toString()) + "\n  found:    " + foundText + "\n  reason: " + reason);
            return;
        }
        error(env, pos, "no suitable " + (isCtor ? "constructor" : "method") + " found for "
                + (isCtor ? where.simpleName : name) + "(" + found + ")");
    }

    private static String describeDeferred(Tree t) {
        t = strip(t);
        if (t instanceof Tree.Lambda) {
            return "(" + ((Tree.Lambda) t).params.size() + " args)->{ }";
        }
        if (t instanceof Tree.MethodRef) {
            return "method reference";
        }
        return "<expression>";
    }

    private Resolution mostSpecific(List<Resolution> applicable, List<Tree> args, boolean varargs) {
        Resolution best = null;
        for (Resolution r1 : applicable) {
            boolean all = true;
            for (Resolution r2 : applicable) {
                if (r1 != r2 && !moreSpecific(r1.method, r2.method, args, varargs)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                if (best != null) {
                    // Two equally specific: the same signature inherited twice (prefer a concrete one).
                    if (best.method.isAbstract() && !r1.method.isAbstract()) {
                        best = r1;
                    }
                    continue;
                }
                best = r1;
            }
        }
        if (best == null) {
            // Same erased signature everywhere (abstract + default from different interfaces).
            String sig = erasedParams(applicable.get(0).method);
            boolean same = true;
            for (Resolution r : applicable) {
                if (!erasedParams(r.method).equals(sig)) {
                    same = false;
                }
            }
            if (same) {
                for (Resolution r : applicable) {
                    if (!r.method.isAbstract()) {
                        return r;
                    }
                }
                return applicable.get(0);
            }
        }
        return best;
    }

    private boolean moreSpecific(MethodSymbol m1, MethodSymbol m2, List<Tree> args, boolean varargs) {
        int n = Math.max(args.size(), Math.max(m1.params.size(), m2.params.size()));
        if (!varargs) {
            n = args.size();
        }
        for (int i = 0; i < n; i++) {
            Type f1 = formal(m1, i, varargs);
            Type f2 = formal(m2, i, varargs);
            if (f1.isErroneous() || f2.isErroneous()) {
                continue;
            }
            Tree arg = i < args.size() ? strip(args.get(i)) : null;
            if (arg != null && (arg instanceof Tree.Lambda || arg instanceof Tree.MethodRef)) {
                Type e1 = types.erased(f1);
                Type e2 = types.erased(f2);
                if (types.isSubtype(e1, e2)) {
                    continue;
                }
                if (types.isSubtype(e2, e1)) {
                    return false;
                }
                MethodSymbol s1 = e1.tag == Type.Tag.CLASS ? types.functionalMethod(((Type.ClassType) e1).sym) : null;
                MethodSymbol s2 = e2.tag == Type.Tag.CLASS ? types.functionalMethod(((Type.ClassType) e2).sym) : null;
                if (s1 == null || s2 == null) {
                    return false;
                }
                Type r1 = s1.returnType;
                Type r2 = s2.returnType;
                if (r2.tag == Type.Tag.VOID && r1.tag != Type.Tag.VOID) {
                    continue;
                }
                if (r1.isPrimitive() && !r2.isPrimitive() && r1.tag != Type.Tag.VOID && r2.tag != Type.Tag.VOID
                        && lambdaReturnsPrimitive(arg)) {
                    continue;
                }
                if (!r1.isPrimitive() && r2.isPrimitive() && r2.tag != Type.Tag.VOID && !lambdaReturnsPrimitive(arg)
                        && r1.tag != Type.Tag.VOID) {
                    continue;
                }
                return false;
            }
            Type e1 = f1.tag == Type.Tag.TYPEVAR ? types.erased(f1) : f1;
            Type e2 = f2.tag == Type.Tag.TYPEVAR ? types.erased(f2) : f2;
            if (e1.isPrimitive() && e2.isPrimitive()) {
                if (!Types.isPrimitiveWidening(e1, e2)) {
                    return false;
                }
            } else if (e1.isPrimitive() != e2.isPrimitive()) {
                // int vs Object in the loose phase: the primitive is more specific for a primitive argument.
                Type at = arg == null ? null : arg.type;
                if (at == null || at.isPrimitive() != e1.isPrimitive()) {
                    return false;
                }
            } else if (!types.isSubtype(types.erased(e1), types.erased(e2))) {
                return false;
            }
        }
        if (varargs && m1.params.size() < m2.params.size()) {
            return false;
        }
        return true;
    }

    private static boolean lambdaReturnsPrimitive(Tree arg) {
        if (arg instanceof Tree.Lambda) {
            Tree body = strip(((Tree.Lambda) arg).body);
            return body instanceof Tree.Literal && ((Tree.Literal) body).kind != Token.Kind.STRING_LITERAL
                    && ((Tree.Literal) body).kind != Token.Kind.NULL
                    || body instanceof Tree.Binary && ((Tree.Binary) body).op != Token.Kind.PLUS;
        }
        return false;
    }

    // ------------------------------------------------------------------ argument attribution

    /** Attributes the arguments that do not need a target type; deferred ones get a null slot. */
    private List<Type> attribArgsFor(List<Tree> args, Env env, boolean deferPoly) {
        List<Type> out = new ArrayList<Type>();
        for (Tree a : args) {
            if (isLambdaLike(a) || deferPoly && isTargetSensitive(a)) {
                out.add(null);
            } else {
                Type t = attribExpr(a, env, null);
                if (t.tag == Type.Tag.VOID) {
                    error(env, a.pos, "'void' type not allowed here");
                    t = Type.ERROR;
                }
                out.add(t);
            }
        }
        return out;
    }

    List<Type> attribArgs(List<Tree> args, Env env) {
        return attribArgsFor(args, env, false);
    }

    private static int arityMatches(List<MethodSymbol> candidates, int n) {
        int count = 0;
        for (MethodSymbol m : candidates) {
            int p = m.params.size();
            if (p == n || m.isVarargs() && n >= p - 1) {
                count++;
            }
        }
        return count;
    }

    /**
     * Completes a call once its method is chosen: attributes the deferred arguments
     * against their now-known formal types, adds the target type, and solves the
     * inference variables. Returns the instantiated return type.
     */
    private Type instantiate(Resolution r, List<Tree> args, List<Type> argTypes, Env env, Type pt, Type declaredRet) {
        MethodSymbol m = r.method;
        boolean va = r.varargs;
        // Bounds from the arguments already typed were recorded while checking applicability;
        // the target type comes next, before anything is solved.
        Type ret0 = Type.substitute(declaredRet, r.map);
        boolean ptAdded = addTargetBound(r, ret0, pt);
        partialSolve(r.ivars);
        for (int i = 0; i < args.size(); i++) {
            if (argTypes.get(i) != null || isLambdaLike(args.get(i))) {
                continue;
            }
            Type f = Type.substitute(formal(m, i, va), r.map);
            Type rf = types.resolveInference(f);
            Type at = attribExpr(args.get(i), env, hasUnsolved(rf) ? provisional(rf, true) : rf);
            argTypes.set(i, at);
            if (!at.isErroneous() && at.tag != Type.Tag.VOID) {
                if (!types.isAssignable(at, f, true, args.get(i).constant)) {
                    checkAssignable(args.get(i), at, types.resolveInference(f), env);
                }
            } else if (at.tag == Type.Tag.VOID) {
                error(env, args.get(i).pos, "'void' type not allowed here");
            }
        }
        Type ret = ret0;
        if (!ptAdded) {
            addTargetBound(r, ret, pt);
        }
        partialSolve(r.ivars);
        for (int i = 0; i < args.size(); i++) {
            Tree a = args.get(i);
            if (argTypes.get(i) != null || !isLambdaLike(a)) {
                continue;
            }
            Type f = Type.substitute(formal(m, i, va), r.map);
            solveFunctionalParams(f, r.ivars);
            Type at = attribExpr(a, env, types.resolveInference(f));
            argTypes.set(i, at);
        }
        solveAll(r.ivars);
        Type result = types.resolveInference(ret);
        return result;
    }

    /** Adds "return type compatible with the target" to the inference; false when there is nothing to add. */
    private boolean addTargetBound(Resolution r, Type ret, Type pt) {
        if (pt == null || pt.tag == Type.Tag.VOID || r.ivars.isEmpty() || !hasUnsolved(ret)) {
            return false;
        }
        Type target = types.resolveInference(pt);
        if (hasUnsolved(target)) {
            return false;
        }
        if (target.isPrimitive()) {
            if (ret.tag == Type.Tag.INFERENCE) {
                types.isSubtype(ret, syms.boxedType(target));
            }
        } else {
            types.isAssignable(ret, target, true, null);
        }
        return true;
    }

    /**
     * A target type for an argument whose formal still mentions unsolved inference
     * variables: each becomes a wildcard bounded by what is known of it, so a nested
     * generic call can still learn from the parts that are known.
     */
    private Type provisional(Type t, boolean top) {
        switch (t.tag) {
            case INFERENCE: {
                Type.InferenceVar iv = (Type.InferenceVar) t;
                if (iv.inst != null) {
                    return provisional(iv.inst, top);
                }
                Type upper = null;
                for (Type u : iv.upper) {
                    Type ru = types.resolveInference(u);
                    if (!hasUnsolved(ru) && ru.tag != Type.Tag.WILDCARD) {
                        upper = ru;
                    }
                }
                if (top) {
                    return upper;
                }
                return upper == null ? new Type.WildcardType(Type.WildcardType.UNBOUND, null)
                        : new Type.WildcardType(Type.WildcardType.EXTENDS, upper);
            }
            case CLASS: {
                Type.ClassType c = (Type.ClassType) t;
                if (!hasUnsolved(c)) {
                    return c;
                }
                List<Type> args = new ArrayList<Type>();
                for (Type a : c.args) {
                    Type p = provisional(a, false);
                    args.add(p == null ? new Type.WildcardType(Type.WildcardType.UNBOUND, null) : p);
                }
                return new Type.ClassType(c.sym, args, c.outer);
            }
            case WILDCARD: {
                Type.WildcardType w = (Type.WildcardType) t;
                if (w.bound == null || !hasUnsolved(w.bound)) {
                    return w;
                }
                Type b = provisional(w.bound, true);
                return b == null ? new Type.WildcardType(Type.WildcardType.UNBOUND, null) : new Type.WildcardType(w.kind, b);
            }
            case ARRAY: {
                Type e = provisional(((Type.ArrayType) t).elem, true);
                return e == null ? null : new Type.ArrayType(e);
            }
            default:
                return hasUnsolved(t) ? null : t;
        }
    }

    private boolean hasUnsolved(Type t) {
        return Types.containsInference(t);
    }

    /** Solves the inference variables a functional parameter's SAM parameter types mention. */
    private void solveFunctionalParams(Type f, List<Type.InferenceVar> ivars) {
        Type rf = types.resolveInference(f);
        if (rf.tag == Type.Tag.INFERENCE) {
            solve((Type.InferenceVar) rf, true);
            return;
        }
        Type e = types.erased(rf);
        if (e.tag != Type.Tag.CLASS) {
            return;
        }
        MethodSymbol sam = types.functionalMethod(((Type.ClassType) e).sym);
        if (sam == null) {
            return;
        }
        Type ground = groundType(rf);
        java.util.Map<Type.TypeVar, Type> map = types.memberMapping(ground, sam.owner);
        for (Type p : sam.params) {
            Type pp = Type.substitute(p, map);
            List<Type.InferenceVar> in = new ArrayList<Type.InferenceVar>();
            collectIvars(pp, in);
            for (Type.InferenceVar iv : in) {
                solve(iv, true);
            }
        }
    }

    private void collectIvars(Type t, List<Type.InferenceVar> out) {
        if (t == null) {
            return;
        }
        switch (t.tag) {
            case INFERENCE: {
                Type.InferenceVar iv = (Type.InferenceVar) t;
                if (iv.inst == null) {
                    if (!out.contains(iv)) {
                        out.add(iv);
                    }
                } else {
                    collectIvars(iv.inst, out);
                }
                break;
            }
            case CLASS:
                for (Type a : ((Type.ClassType) t).args) {
                    collectIvars(a, out);
                }
                break;
            case ARRAY:
                collectIvars(((Type.ArrayType) t).elem, out);
                break;
            case WILDCARD:
                collectIvars(((Type.WildcardType) t).bound, out);
                break;
            case INTERSECTION:
                for (Type b : ((Type.IntersectionType) t).bounds) {
                    collectIvars(b, out);
                }
                break;
            default:
                break;
        }
    }

    private void partialSolve(List<Type.InferenceVar> ivars) {
        boolean progress = true;
        while (progress) {
            progress = false;
            for (Type.InferenceVar iv : ivars) {
                if (iv.inst == null && solve(iv, false)) {
                    progress = true;
                }
            }
        }
    }

    private void solveAll(List<Type.InferenceVar> ivars) {
        partialSolve(ivars);
        for (Type.InferenceVar iv : ivars) {
            if (iv.inst == null) {
                solve(iv, true);
            }
        }
    }

    /** Instantiates one inference variable from its bounds; false when it must wait for others. */
    boolean solve(Type.InferenceVar iv, boolean force) {
        if (iv.inst != null) {
            return false;
        }
        for (Type t : iv.equal) {
            Type r = types.resolveInference(t);
            if (r == iv) {
                continue;
            }
            if (!hasUnsolved(r) || force) {
                iv.inst = noWildcard(r);
                if (hasUnsolved(iv.inst)) {
                    iv.inst = types.erased(iv.inst);
                }
                return true;
            }
        }
        List<Type> lowers = new ArrayList<Type>();
        boolean waiting = false;
        for (Type t : iv.lower) {
            Type r = types.resolveInference(t);
            if (r == iv) {
                continue;
            }
            if (hasUnsolved(r)) {
                waiting = true;
                if (force) {
                    lowers.add(types.erased(r));
                }
            } else {
                lowers.add(r);
            }
        }
        if (!lowers.isEmpty() && (!waiting || force)) {
            Type lub = noWildcard(types.lub(lowers));
            // A lub that misses an upper bound (List.of("a", sb) with target List<? extends CharSequence>):
            // take the upper bound every lower bound satisfies instead.
            for (Type u : iv.upper) {
                Type ru = types.resolveInference(u);
                if (ru == iv || hasUnsolved(ru) || ru.tag == Type.Tag.INFERENCE) {
                    continue;
                }
                ru = noWildcard(ru);
                if (!types.isSubtypeStrict(lub, ru)) {
                    boolean all = true;
                    for (Type l : lowers) {
                        if (!types.isSubtypeStrict(l, ru)) {
                            all = false;
                        }
                    }
                    if (all) {
                        lub = ru;
                    }
                }
            }
            iv.inst = lub;
            return true;
        }
        if (!force) {
            return false;
        }
        Type best = null;
        for (Type t : iv.upper) {
            Type r = types.resolveInference(t);
            if (r == iv || r.tag == Type.Tag.INFERENCE) {
                continue;
            }
            if (hasUnsolved(r)) {
                r = types.erased(r);
            }
            r = noWildcard(r);
            if (r.tag == Type.Tag.CLASS && ((Type.ClassType) r).sym == syms.objectSym && best != null) {
                continue;
            }
            if (best == null || types.isSubtypeStrict(r, best)) {
                best = r;
            }
        }
        if (best != null) {
            iv.inst = best;
            return true;
        }
        Type bound = iv.origin.bound;
        iv.inst = bound == null ? syms.objectType : types.erased(bound);
        return true;
    }

    private Type noWildcard(Type t) {
        if (t.tag == Type.Tag.WILDCARD) {
            Type.WildcardType w = (Type.WildcardType) t;
            return w.kind == Type.WildcardType.SUPER ? w.bound : types.upperBound(w);
        }
        if (t.tag == Type.Tag.NULL) {
            return syms.objectType;
        }
        return t;
    }

    /** A wildcard-parameterized functional interface's ground type (JLS 9.9). */
    Type groundType(Type t) {
        Type r = types.resolveInference(t);
        if (r.tag != Type.Tag.CLASS) {
            return r;
        }
        Type.ClassType c = (Type.ClassType) r;
        if (c.args.isEmpty()) {
            return c;
        }
        List<Type> args = new ArrayList<Type>();
        List<Type.TypeVar> params = c.sym.typeParams();
        boolean changed = false;
        for (int i = 0; i < c.args.size(); i++) {
            Type a = c.args.get(i);
            if (a.tag == Type.Tag.WILDCARD) {
                Type.WildcardType w = (Type.WildcardType) a;
                changed = true;
                if (w.kind == Type.WildcardType.UNBOUND) {
                    Type b = i < params.size() ? params.get(i).bound : null;
                    args.add(b == null || Types.containsInference(b) ? syms.objectType : types.erased(b));
                } else {
                    args.add(w.bound);
                }
            } else {
                args.add(a);
            }
        }
        return changed ? new Type.ClassType(c.sym, args, c.outer) : c;
    }

    // ------------------------------------------------------------------ calls

    private List<Type> attribTypeArgs(List<Tree> typeArgs, Env env) {
        List<Type> out = new ArrayList<Type>();
        for (Tree t : typeArgs) {
            out.add(attribType(t, env));
        }
        return out;
    }

    private Type attribCall(Tree.MethodCall call, Env env, Type pt) {
        if ("<init>".equals(call.name)) {
            return attribConstructorCall(call, env);
        }
        List<Type> typeArgs = attribTypeArgs(call.typeArgs, env);
        Type site;
        ClassSymbol where;
        boolean staticOnly = false;
        List<MethodSymbol> candidates;
        if (call.receiver == null && !call.superCall) {
            // Unqualified: the innermost enclosing class with a member of that name.
            candidates = null;
            site = null;
            where = null;
            boolean noInstance = false;
            for (Env e = env; e != null; e = e.outer) {
                if (e.isStatic) {
                    noInstance = true;
                }
                if (e.classBoundary) {
                    List<MethodSymbol> found = collectMethods(e.enclClass.thisType(), call.name);
                    if (!found.isEmpty()) {
                        candidates = found;
                        site = e.enclClass.thisType();
                        where = e.enclClass;
                        staticOnly = noInstance;
                        break;
                    }
                    if (!e.enclClass.hasOuterInstance) {
                        noInstance = true;
                    }
                }
            }
            if (candidates == null) {
                ClassSymbol owner = findStaticImportMethodOwner(call.name, env.unit);
                if (owner != null) {
                    candidates = new ArrayList<MethodSymbol>();
                    for (MethodSymbol m : collectMethods(owner.erasure(), call.name)) {
                        if (m.isStatic()) {
                            candidates.add(m);
                        }
                    }
                    site = owner.erasure();
                    where = owner;
                    staticOnly = true;
                    call.qualifier = owner;
                } else {
                    candidates = new ArrayList<MethodSymbol>();
                    where = env.enclClass;
                }
            }
            call.site = where;
        } else if (call.superCall) {
            // super.m(...)
            if (inStaticContext(env)) {
                error(env, call.pos, "non-static variable super cannot be referenced from a static context");
            }
            markThisCapture(env);
            Type sup = types.supertype(env.enclClass.thisType());
            site = sup == null ? syms.objectType : sup;
            where = ((Type.ClassType) types.erased(site)).sym;
            candidates = collectMethods(site, call.name);
            // Interface default methods reachable through super.m() in an interface's implementor.
            if (candidates.isEmpty()) {
                for (Type i : env.enclClass.interfaces()) {
                    candidates.addAll(collectMethods(i, call.name));
                }
            }
            call.qualifier = where;
        } else if (call.receiver instanceof Tree.Select && "super".equals(((Tree.Select) call.receiver).name)) {
            // X.super.m(): an interface's default method, or an enclosing class's superclass method.
            Tree.Select s = (Tree.Select) call.receiver;
            Type q = attribType(s.selected, env);
            if (q.tag != Type.Tag.CLASS) {
                return Type.ERROR;
            }
            ClassSymbol x = ((Type.ClassType) q).sym;
            markThisCapture(env);
            if (x.isInterface()) {
                site = q;
                where = x;
                call.superCall = true;
            } else {
                Type sup = x.superclass();
                site = sup == null ? syms.objectType : sup;
                where = ((Type.ClassType) types.erased(site)).sym;
                call.superCall = true;
                call.site = x;
            }
            s.type = site;
            candidates = collectMethods(site, call.name);
            call.qualifier = where;
        } else {
            Object q = resolveQualifier(call.receiver, env);
            if (q instanceof String) {
                unresolvedQualifier(call.receiver, (String) q, env);
                attribArgs(call.args, env);
                return Type.ERROR;
            }
            if (q instanceof Type) {
                site = (Type) q;
                staticOnly = true;
                call.receiver.type = site;
            } else {
                site = attribExpr(call.receiver, env, null);
            }
            if (site.isErroneous()) {
                attribArgs(call.args, env);
                return Type.ERROR;
            }
            Type rsite = types.resolveInference(site);
            if (rsite.isPrimitive()) {
                error(env, call.pos, rsite + " cannot be dereferenced");
                attribArgs(call.args, env);
                return Type.ERROR;
            }
            site = rsite;
            Type es = types.erased(site);
            where = es.tag == Type.Tag.CLASS ? ((Type.ClassType) es).sym : syms.objectSym;
            candidates = collectMethods(site, call.name);
        }
        int n = call.args.size();
        // With a single candidate the poly arguments are attributed against its formals
        // (deferred to instantiate()).
        boolean defer = arityMatches(candidates, n) == 1;
        List<Type> argTypes = attribArgsFor(call.args, env, defer);
        Resolution r = selectMethod(candidates, site, call.name, call.args, argTypes, typeArgs, env, call.pos, false,
                null, false, where);
        if (r == null) {
            attribDeferredQuietly(call.args, argTypes, env);
            return Type.ERROR;
        }
        MethodSymbol m = r.method;
        call.sym = m;
        call.varargsCall = r.varargs;
        if (staticOnly && !m.isStatic()) {
            error(env, call.pos, "non-static method " + m + " cannot be referenced from a static context");
        }
        if (call.receiver == null && !call.superCall && !m.isStatic()) {
            markThisCapture(env);
            if (env.ctorPrologue && where == env.enclClass) {
                error(env, call.pos, "cannot reference " + m.name + "(...) before supertype constructor has been called");
            }
        }
        if (call.superCall && m.isAbstract()) {
            error(env, call.pos, "abstract method " + m + " in " + m.owner.javaName() + " cannot be accessed directly");
        }
        checkAccess(m.flags, m.owner, env, call.pos, m.name + "(" + paramList(m) + ")");
        boolean arrayClone = m.name.equals("clone") && m.params.isEmpty() && site != null && types.erased(site).tag == Type.Tag.ARRAY;
        for (Type th : arrayClone ? new ArrayList<Type>() : m.thrown) {
            Type it = types.resolveInference(Type.substitute(th, r.map));
            checkThrown(it.tag == Type.Tag.INFERENCE ? types.erased(it) : it, env, call.pos);
        }
        Type declaredRet = m.returnType;
        Type result = instantiate(r, call.args, argTypes, env, pt, declaredRet);
        // Thrown types that mention method type variables are checked after inference.
        if (m.name.equals("getClass") && m.params.isEmpty() && call.receiver != null) {
            ClassSymbol cls = syms.require("java/lang/Class");
            if (!cls.typeParams().isEmpty()) {
                result = new Type.ClassType(cls, Type.list(new Type.WildcardType(Type.WildcardType.EXTENDS, types.erased(site))), null);
            }
        }
        if (m.name.equals("clone") && m.params.isEmpty() && types.erased(site).tag == Type.Tag.ARRAY) {
            result = site;
        }
        // A raw receiver (or unchecked call) yields the erased return type.
        if (site != null && types.erased(site).tag == Type.Tag.CLASS && site.tag == Type.Tag.CLASS
                && ((Type.ClassType) site).isRaw() && !m.isStatic()) {
            result = types.erased(result);
        }
        return result;
    }

    private String paramList(MethodSymbol m) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < m.params.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(m.params.get(i));
        }
        return b.toString();
    }

    /** After a failed resolution, attributes deferred arguments so their trees are typed (errors suppressed). */
    private void attribDeferredQuietly(List<Tree> args, List<Type> argTypes, Env env) {
        compiler.quiet++;
        try {
            for (int i = 0; i < args.size(); i++) {
                if (argTypes.get(i) == null && !isLambdaLike(args.get(i))) {
                    attribExpr(args.get(i), env, null);
                }
            }
        } finally {
            compiler.quiet--;
        }
    }

    private ClassSymbol findStaticImportMethodOwner(String name, Tree.CompilationUnit unit) {
        if (unit == null) {
            return null;
        }
        for (Tree.Import imp : unit.imports) {
            if (imp.isStatic && !imp.onDemand && imp.name.endsWith("." + name)) {
                ClassSymbol owner = classByQualifiedName(imp.name.substring(0, imp.name.length() - name.length() - 1));
                if (owner != null && hasStaticMethod(owner, name)) {
                    return owner;
                }
            }
        }
        for (Tree.Import imp : unit.imports) {
            if (imp.isStatic && imp.onDemand) {
                ClassSymbol owner = classByQualifiedName(imp.name);
                if (owner != null && hasStaticMethod(owner, name)) {
                    return owner;
                }
            }
        }
        return null;
    }

    private boolean hasStaticMethod(ClassSymbol owner, String name) {
        for (MethodSymbol m : collectMethods(owner.erasure(), name)) {
            if (m.isStatic()) {
                return true;
            }
        }
        return false;
    }

    /** this(...) or super(...) as a constructor's first statement. */
    private Type attribConstructorCall(Tree.MethodCall call, Env env) {
        ClassSymbol c = env.enclClass;
        if (env.enclMethod == null || !env.enclMethod.isConstructor() || !env.ctorPrologue) {
            error(env, call.pos, "call to " + (call.superCall ? "super" : "this") + " must be first statement in constructor");
        }
        Type.ClassType site;
        if (call.superCall) {
            Type sup = c.superclass();
            if (sup == null || sup.tag != Type.Tag.CLASS) {
                return Type.VOID;
            }
            site = (Type.ClassType) sup;
            if (call.receiver != null) {
                attribExpr(call.receiver, env, null);
            } else {
                ClassSymbol s = site.sym;
                if (s.hasOuterInstance && !isSubclassOfAnyEnclosing(c, s.outer) && !c.hasOuterInstance) {
                    error(env, call.pos, "no enclosing instance of type " + s.outer.javaName() + " is in scope");
                }
            }
        } else {
            site = c.thisType();
        }
        List<MethodSymbol> ctors = constructorsOf(site.sym);
        boolean defer = arityMatches(ctors, call.args.size()) == 1;
        List<Type> argTypes = attribArgsFor(call.args, env, defer);
        Resolution r = selectMethod(ctors, site, "<init>", call.args, argTypes, attribTypeArgs(call.typeArgs, env),
                env, call.pos, false, null, true, site.sym);
        if (r == null) {
            attribDeferredQuietly(call.args, argTypes, env);
            return Type.VOID;
        }
        call.sym = r.method;
        call.varargsCall = r.varargs;
        call.qualifier = site.sym;
        for (Type th : r.method.thrown) {
            checkThrown(th, env, call.pos);
        }
        instantiate(r, call.args, argTypes, env, null, Type.VOID);
        return Type.VOID;
    }

    List<MethodSymbol> constructorsOf(ClassSymbol c) {
        List<MethodSymbol> out = new ArrayList<MethodSymbol>();
        for (MethodSymbol m : c.methods()) {
            if (m.isConstructor()) {
                out.add(m);
            }
        }
        return out;
    }

    /** Resolves a constructor of site for args already attributed (used for implicit super() and enum constants). */
    Resolution resolveConstructor(Type.ClassType site, List<Tree> args, List<Type> argTypes, Env env, int pos, boolean quiet) {
        List<MethodSymbol> ctors = constructorsOf(site.sym);
        return selectMethod(ctors, site, "<init>", args, argTypes, null, env, pos, quiet, null, true, site.sym);
    }

    /** Completes argument attribution for a constructor resolved by {@link #resolveConstructor}. */
    void completeArgs(List<Tree> args, Resolution r, Env env) {
        List<Type> argTypes = new ArrayList<Type>();
        for (Tree a : args) {
            argTypes.add(a.type);
        }
        instantiate(r, args, argTypes, env, null, Type.VOID);
    }

    private Type attribNew(Tree.NewClass nc, Env env, Type pt) {
        Type ct;
        boolean diamond = nc.clazz instanceof Tree.TypeApply && ((Tree.TypeApply) nc.clazz).args.isEmpty();
        if (nc.outer != null) {
            Type ot = attribExpr(nc.outer, env, null);
            if (ot.isErroneous()) {
                attribArgs(nc.args, env);
                return Type.ERROR;
            }
            Tree name = nc.clazz instanceof Tree.TypeApply ? ((Tree.TypeApply) nc.clazz).clazz : nc.clazz;
            if (!(name instanceof Tree.Ident) || types.erased(ot).tag != Type.Tag.CLASS) {
                error(env, nc.pos, "illegal qualifier");
                return Type.ERROR;
            }
            ClassSymbol member = findMemberClass(((Type.ClassType) types.erased(ot)).sym, ((Tree.Ident) name).name);
            if (member == null) {
                error(env, nc.pos, "cannot find symbol\n  symbol: class " + ((Tree.Ident) name).name);
                return Type.ERROR;
            }
            ((Tree.Ident) name).sym = member;
            name.type = member.erasure();
            if (nc.clazz instanceof Tree.TypeApply && !diamond) {
                List<Type> targs = new ArrayList<Type>();
                for (Tree a : ((Tree.TypeApply) nc.clazz).args) {
                    targs.add(attribType(a, env));
                }
                ct = new Type.ClassType(member, targs, ot.tag == Type.Tag.CLASS ? (Type.ClassType) ot : null);
            } else {
                ct = member.erasure();
            }
            nc.clazz.type = ct;
            if (!member.hasOuterInstance) {
                error(env, nc.pos, "qualified new of static class");
            }
        } else {
            ct = attribType(nc.clazz, env);
        }
        if (ct.isErroneous()) {
            attribArgs(nc.args, env);
            if (nc.body != null) {
                compiler.quiet++;
                compiler.quiet--;
            }
            return Type.ERROR;
        }
        if (ct.tag != Type.Tag.CLASS) {
            error(env, nc.pos, "unexpected type");
            attribArgs(nc.args, env);
            return Type.ERROR;
        }
        Type.ClassType cct = (Type.ClassType) ct;
        ClassSymbol csym = cct.sym;
        if (nc.body == null) {
            if (csym.isAbstract() || csym.isInterface()) {
                error(env, nc.pos, csym.javaName() + " is abstract; cannot be instantiated");
            }
        }
        if (csym.isEnum() && (nc.body == null || !csym.isEnum())) {
            error(env, nc.pos, "enum classes may not be instantiated");
        }
        if (csym.hasOuterInstance && nc.outer == null) {
            if (inStaticContext(env) || !hasEnclosingInstanceOf(env.enclClass, csym.outer)) {
                if (csym.local) {
                    error(env, nc.pos, "non-static variable this cannot be referenced from a static context");
                } else {
                    error(env, nc.pos, "non-static variable this cannot be referenced from a static context");
                }
            } else {
                markThisCapture(env);
            }
        }
        // Instantiating a local class captures what it captures.
        for (VarSymbol v : new ArrayList<VarSymbol>(csym.capturedVars)) {
            findVariable(v.name, env, true, nc.pos);
        }
        Type.ClassType ctorSite = cct;
        boolean itf = csym.isInterface();
        if (itf) {
            ctorSite = syms.objectType;
            if (!nc.args.isEmpty()) {
                error(env, nc.pos, "anonymous class implements interface; cannot have arguments");
            }
        }
        List<MethodSymbol> ctors = constructorsOf(ctorSite.sym);
        boolean defer = arityMatches(ctors, nc.args.size()) == 1;
        List<Type> argTypes = attribArgsFor(nc.args, env, defer);
        Type.ClassType diamondOf = diamond && !itf ? cct : null;
        Resolution r = selectMethod(ctors, ctorSite, "<init>", nc.args, argTypes, attribTypeArgs(nc.typeArgs, env),
                env, nc.pos, false, diamondOf, true, ctorSite.sym);
        Type result = cct;
        if (r != null) {
            for (Type th : r.method.thrown) {
                checkThrown(th, env, nc.pos);
            }
            Type declaredRet = r.diamondType != null ? r.diamondType : Type.VOID;
            Type inst = instantiate(r, nc.args, argTypes, env, pt, declaredRet);
            if (r.diamondType != null) {
                result = inst;
            }
            nc.constructor = r.method;
            nc.varargsCall = r.varargs;
        } else {
            attribDeferredQuietly(nc.args, argTypes, env);
        }
        if (diamond && itf && pt != null) {
            Type.ClassType view = types.asSuper(types.resolveInference(pt), csym);
            if (view != null) {
                result = groundType(view);
            }
        }
        nc.clazzSym = csym;
        if (nc.body != null) {
            Type superType = result;
            if (superType.tag == Type.Tag.CLASS && ((Type.ClassType) superType).isParameterized()) {
                superType = groundType(superType);
            }
            ClassSymbol anon = declareAnonymous(nc.body, env, superType, false);
            MethodSymbol ctor = anonConstructor(anon, r);
            nc.clazzSym = anon;
            nc.constructor = ctor;
            if (anon.hasOuterInstance) {
                markThisCapture(env);
            }
            attribClass(anon);
            for (VarSymbol v : new ArrayList<VarSymbol>(anon.capturedVars)) {
                findVariable(v.name, env, true, nc.pos);
            }
            return anon.thisType();
        }
        return result;
    }

    private boolean hasEnclosingInstanceOf(ClassSymbol from, ClassSymbol target) {
        for (ClassSymbol c = from; c != null; c = c.outer) {
            if (types.isSubClass(c, target)) {
                return true;
            }
            if (!c.hasOuterInstance) {
                return false;
            }
        }
        return false;
    }

    // ================================================================== lambdas and method references

    private Type attribLambda(Tree.Lambda l, Env env, Type pt) {
        if (pt == null || pt.tag == Type.Tag.VOID) {
            error(env, l.pos, "lambda expression not expected here");
            attribLambdaBodyQuietly(l, env);
            return Type.ERROR;
        }
        Type target = groundType(pt);
        Type e = types.erased(target);
        MethodSymbol sam = e.tag == Type.Tag.CLASS ? types.functionalMethod(((Type.ClassType) e).sym) : null;
        if (sam == null) {
            if (!pt.isErroneous()) {
                error(env, l.pos, "incompatible types: " + pt + " is not a functional interface");
            }
            attribLambdaBodyQuietly(l, env);
            return Type.ERROR;
        }
        if (!sam.typeParams.isEmpty()) {
            error(env, l.pos, "invalid functional descriptor for lambda expression\n    method " + sam + " is generic");
            return Type.ERROR;
        }
        java.util.Map<Type.TypeVar, Type> map = types.memberMapping(target, sam.owner);
        List<Type> samParams = new ArrayList<Type>();
        for (Type p : sam.params) {
            samParams.add(Type.substitute(p, map));
        }
        Type samRet = Type.substitute(sam.returnType, map);
        if (samRet.tag == Type.Tag.WILDCARD) {
            samRet = types.upperBound(samRet);
        }
        if (l.params.size() != samParams.size()) {
            error(env, l.pos, "incompatible types: incompatible parameter types in lambda expression");
            return Type.ERROR;
        }
        Env le = env.dup();
        le.lambda = l;
        le.lambdaReturn = samRet;
        le.lambdaReturns = new ArrayList<Type>();
        le.breakable = false;
        le.continuable = false;
        le.switchExpression = null;
        le.yields = null;
        le.label = null;
        l.target = sam;
        l.targetType = target;
        l.type = target;
        if (l.boundVar != null) {
            le.enterLocal(l.boundVar);
        }
        for (int i = 0; i < l.params.size(); i++) {
            Tree.VarDef p = l.params.get(i);
            Type t;
            if (p.declaredType != null) {
                t = p.declaredType;
            } else if (p.vartype != null) {
                t = attribType(p.vartype, env);
            } else {
                t = types.resolveInference(samParams.get(i));
                if (Types.containsInference(t)) {
                    t = types.erased(t);
                }
                if (t.tag == Type.Tag.WILDCARD) {
                    t = types.upperBound(t);
                }
            }
            checkLocalRedeclared(p.name, env, p.pos);
            VarSymbol v = new VarSymbol(p.name, p.mods == null ? 0 : p.mods.flags & Symbol.ACC_FINAL, t, VarSymbol.Kind.PARAM);
            v.hasInitializer = true;
            p.sym = v;
            p.type = t;
            le.enterLocal(v);
        }
        boolean inferring = Types.containsInference(samRet);
        if (l.body instanceof Tree.Block) {
            attribStat(l.body, le);
            if (samRet.tag != Type.Tag.VOID && canCompleteNormally(l.body)) {
                error(env, l.pos, "incompatible types: bad return type in lambda expression\n    missing return value");
            }
        } else if (samRet.tag == Type.Tag.VOID) {
            Type bt = attribExpr(l.body, le, null);
            Tree body = strip(l.body);
            boolean statementExpr = body instanceof Tree.MethodCall || body instanceof Tree.NewClass
                    || body instanceof Tree.Assign || body instanceof Tree.CompoundAssign
                    || body instanceof Tree.Unary && (((Tree.Unary) body).op == Token.Kind.PLUSPLUS
                    || ((Tree.Unary) body).op == Token.Kind.SUBSUB);
            if (!statementExpr && !bt.isErroneous()) {
                error(env, l.body.pos, "incompatible types: bad return type in lambda expression\n    " + bt + " cannot be converted to void");
            }
        } else {
            Type bt = attribExpr(l.body, le, inferring ? null : samRet);
            if (bt.tag == Type.Tag.VOID) {
                error(env, l.body.pos, "incompatible types: bad return type in lambda expression\n    void cannot be converted to " + samRet);
            } else if (!inferring) {
                checkAssignable(l.body, bt, samRet, env);
            }
            le.lambdaReturns.add(bt);
        }
        if (inferring) {
            for (Type rt : le.lambdaReturns) {
                if (rt != null && !rt.isErroneous() && rt.tag != Type.Tag.NULL) {
                    types.isAssignable(rt, samRet, true, null);
                }
            }
        }
        env.enclClass.lambdas.add(l);
        return target;
    }

    private void attribLambdaBodyQuietly(Tree.Lambda l, Env env) {
        // Untyped context: the body is not checked (javac reports only the context error).
    }

    /** Rewrites a method reference into the equivalent lambda and attributes that. */
    private Type attribMethodRef(Tree.MethodRef r, Env env, Type pt) {
        if (pt == null || pt.tag == Type.Tag.VOID) {
            error(env, r.pos, "method reference not expected here");
            return Type.ERROR;
        }
        Type target = groundType(pt);
        Type e = types.erased(target);
        MethodSymbol sam = e.tag == Type.Tag.CLASS ? types.functionalMethod(((Type.ClassType) e).sym) : null;
        if (sam == null) {
            if (!pt.isErroneous()) {
                error(env, r.pos, "incompatible types: " + pt + " is not a functional interface");
            }
            return Type.ERROR;
        }
        java.util.Map<Type.TypeVar, Type> map = types.memberMapping(target, sam.owner);
        List<Type> samParams = new ArrayList<Type>();
        for (Type p : sam.params) {
            Type sp = types.resolveInference(Type.substitute(p, map));
            if (Types.containsInference(sp)) {
                sp = types.erased(sp);
            }
            if (sp.tag == Type.Tag.WILDCARD) {
                sp = types.upperBound(sp);
            }
            samParams.add(sp);
        }
        int n = samParams.size();
        Tree.Lambda l = new Tree.Lambda();
        l.pos = r.pos;
        l.explicitParams = true;
        List<Tree> argRefs = new ArrayList<Tree>();
        for (int i = 0; i < n; i++) {
            Tree.VarDef p = new Tree.VarDef();
            p.pos = r.pos;
            p.name = "$mref$" + i;
            p.declaredType = samParams.get(i);
            l.params.add(p);
            Tree.Ident id = new Tree.Ident();
            id.pos = r.pos;
            id.name = p.name;
            argRefs.add(id);
        }
        Tree body;
        Tree q = r.qualifier;
        boolean isSuper = q instanceof Tree.Ident && "super".equals(((Tree.Ident) q).name);
        if (r.name.equals("<init>")) {
            Type qt = attribType(q, env);
            if (qt.isErroneous()) {
                return Type.ERROR;
            }
            if (qt.tag == Type.Tag.ARRAY) {
                Tree.NewArray na = new Tree.NewArray();
                na.pos = r.pos;
                na.elemType = new Tree.ResolvedType(((Type.ArrayType) qt).elem, r.pos);
                na.dims.addAll(argRefs);
                body = na;
            } else {
                Tree.NewClass nc = new Tree.NewClass();
                nc.pos = r.pos;
                Type.ClassType cqt = (Type.ClassType) qt;
                if (cqt.isRaw()) {
                    Tree.TypeApply ta = new Tree.TypeApply();
                    ta.pos = r.pos;
                    ta.clazz = new Tree.ResolvedType(qt, r.pos);
                    nc.clazz = ta;
                } else {
                    nc.clazz = new Tree.ResolvedType(qt, r.pos);
                }
                nc.args.addAll(argRefs);
                body = nc;
            }
        } else {
            Tree.MethodCall call = new Tree.MethodCall();
            call.pos = r.pos;
            call.name = r.name;
            for (Tree ta : r.typeArgs) {
                call.typeArgs.add(ta);
            }
            Object qual = isSuper ? null : r.qualifierIsType ? attribType(q, env) : resolveQualifier(q, env);
            if (isSuper) {
                call.superCall = true;
                call.args.addAll(argRefs);
            } else if (qual instanceof Type) {
                Type qt = (Type) qual;
                if (qt.isErroneous()) {
                    return Type.ERROR;
                }
                // Static form first (T::m with all arguments), then unbound receiver (first argument is the receiver).
                boolean staticForm = hasApplicableMethod(qt, r.name, samParams, true, env);
                boolean unboundForm = n >= 1 && hasApplicableMethod(qt, r.name, samParams.subList(1, n), false, env);
                if (staticForm || !unboundForm) {
                    call.receiver = new Tree.ResolvedType(qt, r.pos);
                    call.args.addAll(argRefs);
                } else {
                    Tree recv = argRefs.get(0);
                    Type p0 = samParams.get(0);
                    if (!types.isSubtype(types.erased(p0), types.erased(qt))) {
                        Tree.Cast cast = new Tree.Cast();
                        cast.pos = r.pos;
                        cast.clazz = new Tree.ResolvedType(qt, r.pos);
                        cast.expr = recv;
                        recv = cast;
                    }
                    call.receiver = recv;
                    call.args.addAll(argRefs.subList(1, n));
                }
            } else if (qual instanceof String) {
                unresolvedQualifier(q, (String) qual, env);
                return Type.ERROR;
            } else {
                Type rt = attribExpr(q, env, null);
                if (rt.isErroneous()) {
                    return Type.ERROR;
                }
                if (rt.isPrimitive()) {
                    error(env, q.pos, rt + " cannot be dereferenced");
                    return Type.ERROR;
                }
                VarSymbol bound = new VarSymbol("$mref$recv", Symbol.ACC_FINAL, upward(rt), VarSymbol.Kind.PARAM);
                bound.hasInitializer = true;
                l.boundExpr = q;
                l.boundVar = bound;
                Tree.Ident recv = new Tree.Ident();
                recv.pos = r.pos;
                recv.name = bound.name;
                call.receiver = recv;
                call.args.addAll(argRefs);
            }
            body = call;
        }
        l.body = body;
        r.lambda = l;
        int errorsBefore = compiler.errorCount();
        Type t = attribLambda(l, env, pt);
        if (compiler.errorCount() > errorsBefore) {
            return Type.ERROR;
        }
        return t;
    }

    /** Is there a method named name in qt applicable to argument types args (static ones when wantStatic)? */
    private boolean hasApplicableMethod(Type qt, String name, List<Type> args, boolean wantStatic, Env env) {
        List<MethodSymbol> all = collectMethods(qt, name);
        List<MethodSymbol> filtered = new ArrayList<MethodSymbol>();
        for (MethodSymbol m : all) {
            if (m.isStatic() == wantStatic) {
                filtered.add(m);
            }
        }
        if (filtered.isEmpty()) {
            return false;
        }
        List<Tree> fakeArgs = new ArrayList<Tree>();
        for (Type a : args) {
            fakeArgs.add(new Tree.ResolvedType(a, 0));
        }
        compiler.quiet++;
        try {
            return selectMethod(filtered, qt, name, fakeArgs, new ArrayList<Type>(args), null, env, 0, true, null, false, null) != null;
        } finally {
            compiler.quiet--;
        }
    }

    // ================================================================== switch

    private Type attribSwitch(Tree.Switch sw, Env env, Type pt) {
        Type st = attribExpr(sw.selector, env, null);
        Type su = types.unboxedOrSelf(st);
        boolean hasPatterns = false;
        boolean hasNull = false;
        for (Tree.Case c : sw.cases) {
            hasNull |= c.hasNull;
            for (Tree l : c.labels) {
                if (l instanceof Tree.BindingPattern || l instanceof Tree.RecordPattern) {
                    hasPatterns = true;
                }
            }
        }
        int kind;
        if (st.isErroneous()) {
            kind = SW_PATTERN;
        } else if (!hasPatterns && !hasNull && su != null && (su.tag == Type.Tag.INT || su.tag == Type.Tag.CHAR
                || su.tag == Type.Tag.SHORT || su.tag == Type.Tag.BYTE)) {
            kind = SW_INT;
        } else if (!hasPatterns && !hasNull && isString(st)) {
            kind = SW_STRING;
        } else if (!hasPatterns && !hasNull && isEnumType(st)) {
            kind = SW_ENUM;
        } else if (st.isPrimitive()) {
            error(env, sw.selector.pos, "constant label of type " + st + " is not compatible with switch selector");
            kind = SW_INT;
        } else {
            kind = SW_PATTERN;
        }
        sw.switchKind = kind;
        Env swEnv = env.dup();
        if (sw.isExpression) {
            swEnv.switchExpression = sw;
            swEnv.yields = new ArrayList<Tree>();
            swEnv.breakable = false;
            swEnv.continuable = false;
            swEnv.label = null;
            sw.type = pt;
        } else {
            swEnv.breakable = true;
        }
        List<Object> seen = new ArrayList<Object>();
        boolean hasDefault = false;
        boolean unconditional = false;
        List<Tree> values = new ArrayList<Tree>();
        Env shared = swEnv.dup();
        Type selectorForLabels = kind == SW_INT ? su : st;
        for (Tree.Case c : sw.cases) {
            Env cenv = sw.arrows ? swEnv.dup() : shared;
            if (c.isDefault) {
                if (hasDefault) {
                    error(env, c.pos, "duplicate default label");
                }
                hasDefault = true;
            }
            for (Tree label : c.labels) {
                if (label instanceof Tree.BindingPattern || label instanceof Tree.RecordPattern) {
                    Type ptype = attribPattern(label, st, cenv);
                    List<VarSymbol> vars = new ArrayList<VarSymbol>();
                    patternBindings(label, vars);
                    enterBindings(cenv, vars);
                    if (c.guard == null && label instanceof Tree.BindingPattern && !ptype.isErroneous()
                            && types.isSubtype(types.erased(st), types.erased(ptype))) {
                        unconditional = true;
                    }
                    continue;
                }
                Object key = attribCaseConstant(label, kind, selectorForLabels, st, cenv);
                if (key != null) {
                    for (Object k : seen) {
                        if (k.equals(key)) {
                            error(env, label.pos, "duplicate case label");
                        }
                    }
                    seen.add(key);
                }
            }
            if (c.guard != null) {
                attribCond(c.guard, cenv);
                if (Boolean.FALSE.equals(c.guard.constant)) {
                    error(env, c.guard.pos, "this case label has a guard that is a constant expression with value 'false'");
                }
                enterBindings(cenv, bindingsWhen(c.guard, true));
            }
            if (c.arrowExpr != null) {
                if (sw.isExpression) {
                    attribExpr(c.arrowExpr, cenv, pt);
                    values.add(c.arrowExpr);
                } else {
                    attribExpr(c.arrowExpr, cenv, null);
                }
            } else {
                for (Tree s : c.stats) {
                    attribStat(s, cenv);
                }
                if (sw.isExpression && sw.arrows && !c.stats.isEmpty() && c.stats.get(0) instanceof Tree.Block
                        && canCompleteNormally(c.stats.get(0))) {
                    error(env, c.pos, "switch rule completes without providing a value\n  (switch rules in switch expressions should either provide a value or throw)");
                }
            }
        }
        if (sw.isExpression && swEnv.yields != null) {
            values.addAll(swEnv.yields);
        }
        boolean exhaustive = hasDefault || unconditional || isExhaustive(sw, st, kind);
        if (sw.isExpression || kind == SW_PATTERN || hasPatterns || hasNull) {
            if (!exhaustive) {
                error(env, sw.pos, sw.isExpression ? "the switch expression does not cover all possible input values"
                        : "the switch statement does not cover all possible input values");
            } else if (!hasDefault && !unconditional) {
                sw.needsDefaultThrow = true;
            }
        }
        if (!sw.isExpression) {
            return Type.VOID;
        }
        if (sw.cases.isEmpty()) {
            error(env, sw.pos, "switch expression does not have any case clauses");
            return Type.ERROR;
        }
        if (!sw.arrows) {
            Tree.Case last = sw.cases.get(sw.cases.size() - 1);
            if (last.stats.isEmpty() || canCompleteNormallyList(last.stats)) {
                error(env, sw.pos, "switch expression completes without providing a value\n  (switch expressions must either provide a value or throw for all possible input values)");
            }
        }
        return switchType(values, pt, env, sw);
    }

    private boolean canCompleteNormallyList(List<Tree> stats) {
        for (Tree s : stats) {
            if (!canCompleteNormally(s)) {
                return false;
            }
        }
        return true;
    }

    private Type switchType(List<Tree> values, Type pt, Env env, Tree.Switch sw) {
        if (values.isEmpty()) {
            return pt != null ? pt : syms.objectType;
        }
        List<Type> ts = new ArrayList<Type>();
        for (Tree v : values) {
            if (v.type != null && !v.type.isErroneous()) {
                ts.add(v.type);
            }
        }
        if (ts.isEmpty()) {
            return Type.ERROR;
        }
        if (pt != null && !pt.isPrimitive() && pt.tag != Type.Tag.VOID && !Types.containsInference(pt)) {
            for (Tree v : values) {
                checkAssignable(v, v.type, pt, env);
            }
            return pt;
        }
        boolean allSame = true;
        for (Type t : ts) {
            if (!types.isSameType(t, ts.get(0))) {
                allSame = false;
            }
        }
        if (allSame) {
            return ts.get(0);
        }
        boolean allBool = true;
        boolean allNum = true;
        for (Type t : ts) {
            Type u = types.unboxedOrSelf(t);
            allBool &= u != null && u.tag == Type.Tag.BOOLEAN;
            allNum &= u != null && u.isNumeric();
        }
        if (allBool) {
            return Type.BOOLEAN;
        }
        if (allNum) {
            Type r = types.unboxedOrSelf(ts.get(0));
            for (Type t : ts) {
                r = Types.binaryPromote(r, types.unboxedOrSelf(t));
            }
            if (pt != null && pt.isPrimitive()) {
                boolean fits = true;
                for (Tree v : values) {
                    if (!types.isAssignable(v.type, pt, true, v.constant)) {
                        fits = false;
                    }
                }
                if (fits) {
                    return pt;
                }
            }
            return r;
        }
        return types.lub(ts);
    }

    boolean isEnumType(Type t) {
        Type e = types.erased(t);
        return e.tag == Type.Tag.CLASS && ((Type.ClassType) e).sym.isEnum();
    }

    /** A case constant: its folded value (or enum constant symbol) for duplicate detection. */
    private Object attribCaseConstant(Tree label, int kind, Type selector, Type rawSelector, Env env) {
        if (kind == SW_ENUM || kind == SW_PATTERN && isEnumType(rawSelector) && label instanceof Tree.Ident) {
            ClassSymbol enumSym = ((Type.ClassType) types.erased(rawSelector)).sym;
            if (label instanceof Tree.Ident) {
                String name = ((Tree.Ident) label).name;
                VarSymbol f = findField(enumSym.erasure(), name);
                if (f == null || (f.flags & Symbol.ACC_ENUM) == 0) {
                    error(env, label.pos, "an enum switch case label must be the unqualified name of an enumeration constant");
                    label.type = Type.ERROR;
                    return null;
                }
                ((Tree.Ident) label).sym = f;
                label.type = enumSym.erasure();
                return f;
            }
            Type t = attribExpr(label, env, rawSelector);
            Symbol s = label instanceof Tree.Select ? ((Tree.Select) label).sym : null;
            if (!(s instanceof VarSymbol) || (s.flags & Symbol.ACC_ENUM) == 0) {
                error(env, label.pos, "an enum switch case label must be the unqualified name of an enumeration constant");
                return null;
            }
            if (!t.isErroneous() && !types.isSubtype(types.erased(t), types.erased(rawSelector))) {
                error(env, label.pos, "incompatible types: " + t + " cannot be converted to " + rawSelector);
            }
            return s;
        }
        Type t = attribExpr(label, env, selector);
        if (t.isErroneous()) {
            return null;
        }
        if (kind == SW_PATTERN) {
            // Constants in a pattern switch: compared by equals (or ==); the selector must be compatible.
            if (label instanceof Tree.Literal && ((Tree.Literal) label).kind == Token.Kind.NULL) {
                return null;
            }
            if (label.constant == null) {
                error(env, label.pos, "constant expression required");
                return null;
            }
            Type unboxedSel = types.unboxedOrSelf(rawSelector);
            if (unboxedSel != null && !types.isAssignable(t, unboxedSel, true, label.constant)
                    || unboxedSel == null && !types.isCastable(rawSelector, t.isPrimitive() ? syms.boxedType(t) : t)) {
                error(env, label.pos, "constant label of type " + t + " is not compatible with switch selector type " + rawSelector);
            }
            return label.constant;
        }
        if (label.constant == null) {
            error(env, label.pos, kind == SW_STRING ? "constant string expression required" : "constant expression required");
            return null;
        }
        if (!types.isAssignable(t, selector, false, label.constant)) {
            if (t.isPrimitive() && selector.isPrimitive()) {
                error(env, label.pos, "incompatible types: possible lossy conversion from " + t + " to " + selector);
            } else {
                error(env, label.pos, "constant label of type " + t + " is not compatible with switch selector type " + selector);
            }
            return null;
        }
        Object v = label.constant;
        if (kind == SW_INT) {
            label.constant = Integer.valueOf(Constants.intValue(v));
            return label.constant;
        }
        return v;
    }

    /** Exhaustive without default: every enum constant listed, or every permitted subclass of a sealed type covered. */
    private boolean isExhaustive(Tree.Switch sw, Type st, int kind) {
        if (kind == SW_INT || kind == SW_STRING) {
            Type u = types.unboxedOrSelf(st);
            return u != null && u.tag == Type.Tag.BOOLEAN;
        }
        Type e = types.erased(st);
        if (e.tag != Type.Tag.CLASS) {
            return false;
        }
        List<Type> covered = new ArrayList<Type>();
        List<VarSymbol> constants = new ArrayList<VarSymbol>();
        for (Tree.Case c : sw.cases) {
            for (Tree l : c.labels) {
                if (c.guard == null && (l instanceof Tree.BindingPattern || l instanceof Tree.RecordPattern && recordPatternTotal((Tree.RecordPattern) l))) {
                    if (l.type != null && !l.type.isErroneous()) {
                        covered.add(types.erased(l.type));
                    }
                } else if (l instanceof Tree.Ident && ((Tree.Ident) l).sym instanceof VarSymbol) {
                    constants.add((VarSymbol) ((Tree.Ident) l).sym);
                } else if (l instanceof Tree.Select && ((Tree.Select) l).sym instanceof VarSymbol) {
                    constants.add((VarSymbol) ((Tree.Select) l).sym);
                }
            }
        }
        return covers(((Type.ClassType) e).sym, covered, constants, new LinkedHashSet<ClassSymbol>());
    }

    /** A record pattern whose nested patterns all match unconditionally. */
    private boolean recordPatternTotal(Tree.RecordPattern rp) {
        if (rp.record == null) {
            return false;
        }
        List<VarSymbol> comps = rp.record.recordComponents;
        for (int i = 0; i < rp.nested.size() && i < comps.size(); i++) {
            Tree n = rp.nested.get(i);
            if (n instanceof Tree.BindingPattern) {
                Type nt = n.type;
                if (nt == null || !types.isSubtype(types.erased(comps.get(i).type), types.erased(nt))) {
                    if (!(comps.get(i).type.isPrimitive() && nt != null && nt.tag == comps.get(i).type.tag)) {
                        return false;
                    }
                }
            } else if (n instanceof Tree.RecordPattern) {
                if (!recordPatternTotal((Tree.RecordPattern) n)) {
                    return false;
                }
                if (!types.isSubtype(types.erased(comps.get(i).type), types.erased(n.type))) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean covers(ClassSymbol c, List<Type> covered, List<VarSymbol> constants, Set<ClassSymbol> seen) {
        for (Type t : covered) {
            if (t.tag == Type.Tag.CLASS && types.isSubClass(c, ((Type.ClassType) t).sym)) {
                return true;
            }
        }
        if (!seen.add(c)) {
            return false;
        }
        if (c.isEnum()) {
            for (VarSymbol f : c.fields()) {
                if ((f.flags & Symbol.ACC_ENUM) != 0 && !constants.contains(f)) {
                    return false;
                }
            }
            return true;
        }
        if ((c.flags & Symbol.SEALED) != 0 || c.decl == null && !c.permitted.isEmpty()) {
            List<ClassSymbol> subs = permittedSubclasses(c);
            if (subs.isEmpty()) {
                return false;
            }
            for (ClassSymbol s : subs) {
                if (!covers(s, covered, constants, seen)) {
                    return false;
                }
            }
            // An abstract sealed class is covered by its subclasses; a concrete one also needs itself.
            return c.isAbstract() || c.isInterface();
        }
        return false;
    }
}
