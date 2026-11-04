/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.dart.transpiler.analyze;

import com.codename1.dart.transpiler.ast.Ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Whole-program model: every parsed user library plus lookup tables.
 * All user code lands in one Java package, so class names are global.
 */
public final class Program {

    public final List<Ast.Library> libraries = new ArrayList<Ast.Library>();
    public final Map<String, Ast.ClassDecl> classes = new LinkedHashMap<String, Ast.ClassDecl>();
    public final Map<String, Ast.EnumDecl> enums = new LinkedHashMap<String, Ast.EnumDecl>();
    /** Top-level function name -> owning library. */
    public final Map<String, Ast.Library> functionOwners = new LinkedHashMap<String, Ast.Library>();
    public final Map<String, Ast.FunctionDecl> functions = new LinkedHashMap<String, Ast.FunctionDecl>();
    /** Top-level variable name -> owning library. */
    public final Map<String, Ast.Library> topLevelVarOwners = new LinkedHashMap<String, Ast.Library>();
    /** Top-level setter name -> owning library (Dart {@code set x(v)} at library scope). */
    public final Map<String, Ast.Library> topLevelSetters = new LinkedHashMap<String, Ast.Library>();
    public final Map<String, Ast.FieldDecl> topLevelVars = new LinkedHashMap<String, Ast.FieldDecl>();

    /**
     * Every library declaring a given top-level var / function name. Unlike the
     * single-owner maps above (which only retain the last registration), these keep
     * all owners so a same-name collision across studies (e.g. {@code homeRoute} in
     * shrine/reply/rally routes.dart) can be resolved to the correct library via the
     * importing library's prefix import.
     */
    public final Map<String, List<Ast.Library>> topLevelVarOwnersByName =
            new LinkedHashMap<String, List<Ast.Library>>();
    public final Map<String, List<Ast.Library>> functionOwnersByName =
            new LinkedHashMap<String, List<Ast.Library>>();

    public final List<Ast.ClassDecl> extensions = new ArrayList<Ast.ClassDecl>();

    /** Top-level {@code typedef} name -> declaration, across all user libraries. */
    public final Map<String, Ast.TypedefDecl> typedefs = new LinkedHashMap<String, Ast.TypedefDecl>();

    /**
     * Union of every {@code import '...' as prefix;} prefix declared across all
     * user libraries. Because all user code compiles into one Java package with
     * global class / top-level names, a {@code prefix.member} access can be
     * resolved against the whole program regardless of which library declared it.
     */
    public final Set<String> importPrefixes = new HashSet<String>();

    /**
     * Every user class keyed by simple name, keeping ALL declarations that share a
     * name. In the single-package model two libraries may each declare (say) a
     * private {@code _FrontLayer} or a public {@code Backdrop}; {@link #classes}
     * only retains the last one, so same-library-preferred lookups consult this.
     */
    public final Map<String, List<Ast.ClassDecl>> classesByName =
            new LinkedHashMap<String, List<Ast.ClassDecl>>();

    /** Every user enum keyed by simple name, keeping all declarations that share it (see classesByName). */
    public final Map<String, List<Ast.EnumDecl>> enumsByName =
            new LinkedHashMap<String, List<Ast.EnumDecl>>();

    /**
     * Resolves a user enum by simple name as {@link #resolveClass} resolves a class: the
     * referencing library's own declaration, then one it imports, then the last registered.
     */
    public Ast.EnumDecl resolveEnum(String name, Ast.Library fromLibrary) {
        List<Ast.EnumDecl> byName = enumsByName.get(name);
        if (byName == null || byName.isEmpty()) {
            return null;
        }
        if (byName.size() > 1 && fromLibrary != null) {
            for (Ast.EnumDecl e : byName) {
                if (e.ownerLibrary == fromLibrary) {
                    return e;
                }
            }
            for (Ast.Directive d : fromLibrary.importDirectives) {
                // A combinator decides visibility: `import 'a.dart' hide Foo` makes a's
                // Foo invisible here, whatever order the imports come in.
                Ast.Library target = d.admits(name) ? resolveImportedLibrary(fromLibrary, d.uri) : null;
                if (target != null) {
                    for (Ast.EnumDecl e : byName) {
                        if (provides(target, name, e.ownerLibrary)) {
                            return e;
                        }
                    }
                }
            }
        }
        return byName.get(byName.size() - 1);
    }

    /**
     * The Dart package name(s) of the sources being transpiled (the pubspec name), so
     * {@code package:<name>/x.dart} resolves to the project's own x.dart.
     */
    public final Set<String> ownPackages = new HashSet<String>();

    /** {@code part of} files whose owning library has not been added yet. */
    private final List<Ast.Library> pendingParts = new ArrayList<Ast.Library>();

    public void add(Ast.Library lib) {
        if (lib.isPart()) {
            // A part is not a library of its own: its declarations join the owner's,
            // sharing its imports and its library privacy. Files arrive in any order,
            // so a part seen before its owner waits for it.
            for (Ast.Library owner : libraries) {
                if (ownsPart(owner, lib)) {
                    mergePart(owner, lib);
                    return;
                }
            }
            pendingParts.add(lib);
            return;
        }
        libraries.add(lib);
        register(lib, lib);
        for (java.util.Iterator<Ast.Library> it = pendingParts.iterator(); it.hasNext();) {
            Ast.Library part = it.next();
            if (ownsPart(lib, part)) {
                it.remove();
                mergePart(lib, part);
            }
        }
    }

    /**
     * Parts whose owner never arrived (the owner was not among the sources). They are
     * registered as libraries of their own so their declarations are not silently lost;
     * {@code JavaEmitter} calls this before it emits anything.
     */
    public void finishParts() {
        List<Ast.Library> orphans = new ArrayList<Ast.Library>(pendingParts);
        pendingParts.clear();
        for (Ast.Library part : orphans) {
            libraries.add(part);
            register(part, part);
        }
        separateLibraryPrivateMembers();
        separateLibraryPrivateTypedefs();
        restoreShadowedAliases();
        markShadowedCoreTypes();
        eraseExtensionTypes();
        expandRecordTypedefs();
        // `extends Iterable<E>` is `extends IterableBase<E>`: dart:core's Iterable is an
        // abstract base whose only abstract member is `iterator`, exactly IterableBase. The
        // Java dart.core.DartIterable is a concrete wrapper over a java.lang.Iterable, not a
        // base to extend (its iterator() is java.util's, it has no no-arg constructor), so
        // the class takes the dart.collection.IterableMixin protocol IterableBase maps to.
        for (Ast.Library lib : libraries) {
            for (Ast.ClassDecl c : lib.classes) {
                if (c.superclass != null && "Iterable".equals(c.superclass.name)
                        && c.superclass.resolved == null
                        && resolveClass("Iterable", c.ownerLibrary) == null) {
                    c.superclass.name = "IterableBase";
                }
                // `implements Iterable<E>` likewise: the class supplies every member itself,
                // and the IterableMixin interface is what a Java class can implement. A class
                // that already takes that interface (`with IterableMixin`, `extends
                // IterableBase`) would then repeat it, so the clause is dropped there.
                boolean applied = c.superclass != null && ("IterableBase".equals(c.superclass.name)
                        || "IterableMixin".equals(c.superclass.name));
                for (Ast.TypeRef m : c.mixins) {
                    applied |= "IterableMixin".equals(m.name) || "IterableBase".equals(m.name);
                }
                for (java.util.Iterator<Ast.TypeRef> it = c.interfaces.iterator(); it.hasNext();) {
                    Ast.TypeRef i = it.next();
                    if ("Iterable".equals(i.name) && i.resolved == null
                            && resolveClass("Iterable", c.ownerLibrary) == null) {
                        if (applied) {
                            it.remove();
                        } else {
                            i.name = "IterableBase";
                            applied = true;
                        }
                    }
                }
                addSetProtocol(c);
            }
        }
        Set<Ast.ClassDecl> done = new HashSet<Ast.ClassDecl>();
        for (Ast.Library lib : libraries) {
            for (Ast.ClassDecl c : lib.classes) {
                forwardSuperCtors(c, done);
            }
        }
        // Dart 2 let any class with no superclass, no mixins and no constructors be mixed
        // in (`class C with ListMixin<E>, DelegatingView<E>` where DelegatingView is an
        // `abstract class`). Such a class is used as a mixin, so it is emitted as one; it
        // must also be abstract, so nothing constructs it.
        for (Ast.Library lib : libraries) {
            for (Ast.ClassDecl c : lib.classes) {
                for (Ast.TypeRef m : c.mixins) {
                    Ast.ClassDecl mx = resolveClass(m.name, c.ownerLibrary);
                    if (mx != null && !mx.isMixin && mx.isAbstract && mx.superclass == null
                            && mx.mixins.isEmpty() && mx.ctors.isEmpty() && mx.extensionOn == null
                            && mx != c && (c.superclass != null || m != c.mixins.get(0))) {
                        // (In first place with no superclass the emitter already extends it.)
                        mx.isMixin = true;
                    }
                }
            }
        }
    }

    /**
     * `class Wrapper<E> extends Base<E> implements Set<E>` (or a mixin declared
     * `implements Set<E>`): dart:core's Set is a runtime class here, which a Java class that
     * already extends something cannot extend, so the class was neither a DartSet nor a
     * SetMixin and could be handed to no Set-typed slot. SetBase is added beside Set -- Set
     * stays, so the analyzer still reads the class as a Set -- and the emitter implements
     * its runtime interface, dart.collection.SetMixin, and bridges it through SetMixinView.
     * A class with no superclass and no mixin keeps extending the runtime class instead.
     */
    private void addSetProtocol(Ast.ClassDecl c) {
        if (c.superclass == null && c.mixins.isEmpty() && !c.isMixin) {
            return;
        }
        Ast.TypeRef set = null;
        for (Ast.TypeRef i : c.interfaces) {
            if ("SetBase".equals(i.name) || "SetMixin".equals(i.name)) {
                return;
            }
            if ("Set".equals(i.name) && i.resolved == null && i.args.size() == 1
                    && resolveClass("Set", c.ownerLibrary) == null) {
                set = i;
            }
        }
        if (set == null || set.arg(0).is("int") || set.arg(0).is("double")) {
            return;
        }
        for (Ast.TypeRef m : c.mixins) {
            if ("SetBase".equals(m.name) || "SetMixin".equals(m.name)) {
                return;
            }
        }
        if (c.superclass != null && ("SetBase".equals(c.superclass.name)
                || "SetMixin".equals(c.superclass.name))) {
            return;
        }
        Ast.TypeRef base = new Ast.TypeRef("SetBase");
        base.args.add(set.arg(0));
        c.interfaces.add(base);
    }

    /**
     * Gives a mixin application class ({@code class C = S with M;}) one constructor per
     * generative constructor of S, each taking S's parameters as {@code super.x} parameters
     * and calling the same-named S constructor. S is completed first when it is itself a
     * mixin application, so a chain forwards all the way down.
     */
    private void forwardSuperCtors(Ast.ClassDecl c, Set<Ast.ClassDecl> done) {
        if (!c.forwardSuperCtors || !done.add(c) || c.superclass == null) {
            return;
        }
        Ast.ClassDecl sup = resolveClass(c.superclass.name, c.ownerLibrary);
        if (sup == null) {
            return;   // a stub or JDK superclass: only its default constructor is reachable
        }
        forwardSuperCtors(sup, done);
        for (Ast.CtorDecl sc : sup.ctors) {
            if (sc.isFactory || (sc.name != null && sc.name.startsWith("_")
                    && sup.ownerLibrary != c.ownerLibrary)) {
                continue;
            }
            Ast.CtorDecl fc = new Ast.CtorDecl().at(c.file, c.line, c.col);
            fc.name = sc.name;
            fc.isConst = sc.isConst;
            for (Ast.Param sp : sc.params) {
                Ast.Param p = new Ast.Param().at(c.file, c.line, c.col);
                p.type = sp.type;
                p.name = sp.name;
                p.named = sp.named;
                p.required = sp.required;
                p.optional = sp.optional;
                p.isSuper = true;
                p.defaultValue = sp.defaultValue;
                fc.params.add(p);
            }
            Ast.SuperInit si = new Ast.SuperInit();
            si.namedCtor = sc.name;
            fc.superInit = si;
            c.ctors.add(fc);
        }
    }

    private boolean ownsPart(Ast.Library owner, Ast.Library part) {
        String partFile = normalizePath(part.fileName.replace('\\', '/'));
        for (String uri : owner.partUris) {
            if (partFile.equals(resolveRelative(owner, uri))) {
                return true;
            }
        }
        if (part.partOfUri != null) {
            return normalizePath(owner.fileName.replace('\\', '/')).equals(resolveRelative(part, part.partOfUri));
        }
        return part.partOfName != null && part.partOfName.equals(owner.libraryName);
    }

    /**
     * The parser reads an unprefixed {@code Queue} (or another dart:collection name) as the
     * built-in collection it aliases. A program class of that name declared in the library,
     * or imported into it without a prefix, is what Dart resolves the name to instead, so
     * {@code o is Queue} must test that class, never any List.
     */
    private void restoreShadowedAliases() {
        for (Ast.Library lib : libraries) {
            for (Ast.TypeRef t : lib.aliasedTypes) {
                if (t.aliasedFrom != null && !t.runtimeOnly && visibleUnprefixed(t.aliasedFrom, lib)) {
                    t.name = t.aliasedFrom;
                }
            }
        }
    }

    /**
     * A type written with a dart:core name ({@code String}, {@code int}, {@code List}, ...)
     * that a program class of that name declared in, or imported unprefixed into, the
     * library takes instead (dart:core hidden or prefixed, or the class declared locally).
     * It is marked {@code shadowsCore}, so {@code TypeRef.is("String")} -- which the
     * emitter asks everywhere to mean dart:core's String -- answers false for it.
     */
    private void markShadowedCoreTypes() {
        for (Ast.Library lib : libraries) {
            for (Ast.TypeRef t : lib.namedTypes) {
                if (t.name != null && !t.runtimeOnly && Ast.TypeRef.CORE_NAMES.contains(t.name)
                        && visibleUnprefixed(t.name, lib)) {
                    // Only the flag: {@code resolved} reaches a whole class declaration, which
                    // the reflective AST walks would then follow from every type reference.
                    t.shadowsCore = true;
                }
            }
        }
    }

    /**
     * Every type that names a Dart 3 extension type is its representation type at run time:
     * `List<Id>` is a `List<int>`, `x is Id` is `x is int`. Rewritten in place, through
     * chains (`extension type Admin(Id id)` is an int) and with the type arguments of a
     * generic one substituted (`Box<String>` over `List<T>` is `List<String>`). A class of the
     * same name wins, as in {@link #expandRecordTypedefs}.
     */
    private void eraseExtensionTypes() {
        Map<String, Ast.ClassDecl> ext = new java.util.HashMap<String, Ast.ClassDecl>();
        for (Ast.ClassDecl c : extensions) {
            if (c.isExtensionType && !classesByName.containsKey(c.name)) {
                ext.put(c.name, c);
            }
        }
        if (ext.isEmpty()) {
            return;
        }
        for (Ast.Library lib : libraries) {
            for (Ast.TypeRef t : lib.namedTypes) {
                for (int depth = 0; depth < 16 && t.name != null && ext.containsKey(t.name); depth++) {
                    Ast.ClassDecl et = ext.get(t.name);
                    if (t.extensionType == null) {
                        t.extensionType = et;
                    }
                    Ast.TypeRef rep = et.extensionOn;
                    Map<String, Ast.TypeRef> subst = new java.util.HashMap<String, Ast.TypeRef>();
                    for (int i = 0; i < et.typeParams.size(); i++) {
                        subst.put(et.typeParams.get(i), i < t.args.size() ? t.args.get(i) : Ast.TypeRef.DYNAMIC);
                    }
                    if (rep.args.isEmpty() && subst.containsKey(rep.name)) {
                        // `extension type Wrap<T>(T value)`: the representation is the argument.
                        Ast.TypeRef r = subst.get(rep.name);
                        t.name = r.name;
                        t.args = r.args;
                        t.funcParams = r.funcParams;
                        t.nullable = t.nullable || r.nullable;
                        continue;
                    }
                    List<Ast.TypeRef> args = new ArrayList<Ast.TypeRef>();
                    for (Ast.TypeRef a : rep.args) {
                        args.add(substRecordArg(a, subst));
                    }
                    t.name = rep.name;
                    t.args = args;
                    t.nullable = t.nullable || rep.nullable;
                }
            }
        }
    }

    /**
     * A record typedef (`typedef Pair = ({Op f, (int, Op) g})`) is replaced, wherever it is
     * named, by the record shape it aliases, so {@code p.f(1)} resolves the record's
     * fields like a record type written out would. A class of that name wins, as it
     * would in Dart's scope.
     */
    private void expandRecordTypedefs() {
        for (Ast.Library lib : libraries) {
            for (Ast.TypeRef t : lib.namedTypes) {
                Ast.TypedefDecl td = t.name == null ? null : typedefs.get(t.name);
                if (td == null || td.aliased == null || td.aliased.name == null
                        || !td.aliased.name.startsWith("Rec$") || td.aliased.funcParams != null
                        || classesByName.containsKey(t.name)) {
                    continue;
                }
                if (td.typeParams.size() != t.args.size() && !(t.args.isEmpty())) {
                    continue;
                }
                Map<String, Ast.TypeRef> subst = new java.util.HashMap<String, Ast.TypeRef>();
                for (int i = 0; i < td.typeParams.size(); i++) {
                    subst.put(td.typeParams.get(i), i < t.args.size() ? t.args.get(i) : Ast.TypeRef.DYNAMIC);
                }
                List<Ast.TypeRef> args = new ArrayList<Ast.TypeRef>();
                for (Ast.TypeRef a : td.aliased.args) {
                    args.add(substRecordArg(a, subst));
                }
                t.name = td.aliased.name;
                t.args = args;
            }
        }
    }

    private static Ast.TypeRef substRecordArg(Ast.TypeRef a, Map<String, Ast.TypeRef> subst) {
        if (a != null && a.name != null && a.args.isEmpty() && a.funcParams == null && subst.containsKey(a.name)) {
            Ast.TypeRef r = subst.get(a.name);
            if (a.nullable && !r.nullable) {
                Ast.TypeRef c = new Ast.TypeRef(r.name);
                c.args.addAll(r.args);
                c.nullable = true;
                return c;
            }
            return r;
        }
        return a;
    }

    private boolean visibleUnprefixed(String name, Ast.Library lib) {
        List<Ast.ClassDecl> byName = classesByName.get(name);
        if (byName == null) {
            return false;
        }
        for (Ast.ClassDecl c : byName) {
            if (c.ownerLibrary == lib) {
                return true;
            }
        }
        for (Ast.Directive d : lib.importDirectives) {
            Ast.Library target = d.admits(name) && d.prefix == null
                    ? resolveImportedLibrary(lib, d.uri) : null;
            if (target != null) {
                for (Ast.ClassDecl c : byName) {
                    if (provides(target, name, c.ownerLibrary)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void mergePart(Ast.Library owner, Ast.Library part) {
        owner.classes.addAll(part.classes);
        owner.enums.addAll(part.enums);
        owner.functions.addAll(part.functions);
        owner.topLevelVars.addAll(part.topLevelVars);
        owner.typedefs.addAll(part.typedefs);
        owner.aliasedTypes.addAll(part.aliasedTypes);
        owner.namedTypes.addAll(part.namedTypes);
        register(owner, part);
    }

    private static String resolveRelative(Ast.Library from, String uri) {
        String base = from.fileName.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        String dir = slash >= 0 ? base.substring(0, slash) : "";
        return normalizePath(dir.isEmpty() ? uri : dir + "/" + uri);
    }

    /** Registers {@code decls}' declarations in the lookup tables as {@code lib}'s. */
    private void register(Ast.Library lib, Ast.Library decls) {
        importPrefixes.addAll(lib.importPrefixes);
        for (Ast.ClassDecl c : decls.classes) {
            c.ownerLibrary = lib;
            if (c.extensionOn != null) {
                extensions.add(c);
            } else {
                classes.put(c.name, c);
                List<Ast.ClassDecl> byName = classesByName.get(c.name);
                if (byName == null) {
                    byName = new ArrayList<Ast.ClassDecl>();
                    classesByName.put(c.name, byName);
                }
                byName.add(c);
            }
        }
        for (Ast.EnumDecl e : decls.enums) {
            e.ownerLibrary = lib;
            enums.put(e.name, e);
            List<Ast.EnumDecl> byName = enumsByName.get(e.name);
            if (byName == null) {
                byName = new ArrayList<Ast.EnumDecl>();
                enumsByName.put(e.name, byName);
            }
            byName.add(e);
        }
        for (Ast.FunctionDecl f : decls.functions) {
            // A top-level setter shares its name with the getter/field it backs; keep the
            // read-side (getter/plain function) in the lookup maps and track setters apart.
            if (f.isSetter) {
                topLevelSetters.put(f.name, lib);
                continue;
            }
            functions.put(f.name, f);
            functionOwners.put(f.name, lib);
            addOwner(functionOwnersByName, f.name, lib);
        }
        for (Ast.FieldDecl v : decls.topLevelVars) {
            topLevelVars.put(v.name, v);
            topLevelVarOwners.put(v.name, lib);
            addOwner(topLevelVarOwnersByName, v.name, lib);
        }
        for (Ast.TypedefDecl t : decls.typedefs) {
            typedefs.put(t.name, t);
        }
    }

    /**
     * Resolves a user class by simple name, preferring a declaration in {@code fromLibrary}
     * when several libraries share the name (single-package name collision). Falls back to the
     * last-registered declaration ({@link #classes}) when there is no same-library match.
     */
    /**
     * Whether importing {@code lib} provides {@code owner}'s declaration of {@code name}:
     * {@code lib} is {@code owner}, or re-exports it through an {@code export} chain whose
     * every directive admits the name (a barrel file). Matching the imported library alone
     * missed a re-exported declaration, and resolution fell back to whichever same-named
     * declaration was registered last.
     */
    public boolean provides(Ast.Library lib, String name, Ast.Library owner) {
        return provides(lib, name, owner, new HashSet<Ast.Library>());
    }

    private boolean provides(Ast.Library lib, String name, Ast.Library owner, Set<Ast.Library> seen) {
        if (lib == null || owner == null || !seen.add(lib)) {
            return false;
        }
        if (lib == owner) {
            return true;
        }
        for (Ast.Directive d : lib.exportDirectives) {
            if (d.admits(name) && provides(resolveImportedLibrary(lib, d.uri), name, owner, seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@link #resolveClass(String, Ast.Library)} for {@code prefix.Name}: the declaration
     * the prefix's import provides, when it provides one. Lowering {@code a.Foo()} to a bare
     * {@code Foo()} lost the prefix, so {@code a.Foo()} and {@code b.Foo()} both followed
     * import order to the same class.
     */
    public Ast.ClassDecl resolveClass(String name, Ast.Library fromLibrary, String prefix) {
        if (prefix != null && fromLibrary != null) {
            String uri = fromLibrary.prefixImports.get(prefix);
            if (uri != null && uri.startsWith("dart:")) {
                // `io.WebSocket`: a dart: library declares no program class, so the name is
                // the runtime type even beside a program class of the same simple name.
                return null;
            }
            Ast.Library target = resolveImportedLibrary(fromLibrary, uri);
            List<Ast.ClassDecl> byName = classesByName.get(name);
            if (target != null && byName != null) {
                for (Ast.ClassDecl c : byName) {
                    if (provides(target, name, c.ownerLibrary)) {
                        return c;
                    }
                }
            }
        }
        return resolveClass(name, fromLibrary);
    }

    public Ast.ClassDecl resolveClass(String name, Ast.Library fromLibrary) {
        List<Ast.ClassDecl> byName = classesByName.get(name);
        if (byName == null || byName.isEmpty()) {
            return classes.get(name);
        }
        if (byName.size() > 1 && fromLibrary != null) {
            // 1. a class declared in the referencing library itself
            for (Ast.ClassDecl c : byName) {
                if (c.ownerLibrary == fromLibrary) {
                    return c;
                }
            }
            // 2. a class declared in a library the referencing library imports. Dart resolves
            //    an unqualified name against the file's imports, so `Backdrop` in main.dart
            //    (which imports pages/backdrop.dart) must be that Backdrop, never an unrelated
            //    same-name class in a study file main.dart never imports.
            for (Ast.Directive d : fromLibrary.importDirectives) {
                // Honour show/hide: `import 'a.dart' hide Foo; import 'b.dart' show Foo;`
                // is b's Foo even though a.dart comes first.
                // A prefixed import provides only `prefix.Name`, never the bare name: a
                // library wrapping `io.WebSocket` in its own WebSocket means its own.
                Ast.Library target = d.admits(name) && d.prefix == null
                        ? resolveImportedLibrary(fromLibrary, d.uri) : null;
                if (target != null) {
                    for (Ast.ClassDecl c : byName) {
                        if (provides(target, name, c.ownerLibrary)) {
                            return c;
                        }
                    }
                }
            }
        }
        return byName.get(byName.size() - 1);
    }

    private static void addOwner(Map<String, List<Ast.Library>> map, String name, Ast.Library lib) {
        List<Ast.Library> owners = map.get(name);
        if (owners == null) {
            owners = new ArrayList<Ast.Library>();
            map.put(name, owners);
        }
        if (!owners.contains(lib)) {
            owners.add(lib);
        }
    }

    /**
     * Resolves which library owns a top-level var named {@code name}, referenced from
     * {@code from} optionally through an import {@code prefix}. When several libraries
     * declare the name, prefer the one the prefix import points to, then the referencing
     * library itself, then a plainly-imported library, then the last registration.
     */
    public Ast.Library resolveTopLevelVarOwner(String name, Ast.Library from, String prefix) {
        return resolveOwner(name, topLevelVarOwnersByName.get(name), topLevelVarOwners.get(name), from, prefix);
    }

    /**
     * Whether a bare {@code name} in {@code from} can mean a top-level variable or function of
     * the program: one declared in {@code from} itself or provided by an unprefixed import.
     * The owner tables are whole-program, so without this a library that never imports
     * {@code package:path} would see its {@code context} getter.
     */
    public boolean topLevelMemberVisibleUnprefixed(String name, Ast.Library from) {
        List<Ast.Library> owners = new ArrayList<Ast.Library>();
        if (topLevelVarOwnersByName.get(name) != null) {
            owners.addAll(topLevelVarOwnersByName.get(name));
        }
        if (functionOwnersByName.get(name) != null) {
            owners.addAll(functionOwnersByName.get(name));
        }
        if (from == null || owners.isEmpty() || owners.contains(from)) {
            return true;
        }
        for (Ast.Directive d : from.importDirectives) {
            if (d.prefix != null || !d.admits(name)) {
                continue;
            }
            Ast.Library target = resolveImportedLibrary(from, d.uri);
            for (Ast.Library o : owners) {
                if (target != null && provides(target, name, o)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Same as {@link #resolveTopLevelVarOwner} for top-level functions / getters. */
    public Ast.Library resolveFunctionOwner(String name, Ast.Library from, String prefix) {
        return resolveOwner(name, functionOwnersByName.get(name), functionOwners.get(name), from, prefix);
    }

    private Ast.Library resolveOwner(String name, List<Ast.Library> owners, Ast.Library fallback,
                                     Ast.Library from, String prefix) {
        if (owners == null || owners.isEmpty()) {
            return fallback;
        }
        if (owners.size() == 1) {
            return owners.get(0);
        }
        if (prefix != null && from != null) {
            String uri = from.prefixImports.get(prefix);
            Ast.Library target = resolveImportedLibrary(from, uri);
            if (target != null) {
                for (Ast.Library o : owners) {
                    if (provides(target, name, o)) {
                        return o;
                    }
                }
            }
        }
        if (from != null) {
            if (owners.contains(from)) {
                return from;
            }
            for (Ast.Library o : owners) {
                for (Ast.Directive d : from.importDirectives) {
                    // An import that hides the name (or shows others only) does not
                    // provide it, whichever comes first.
                    if (d.admits(name) && provides(resolveImportedLibrary(from, d.uri), name, o)) {
                        return o;
                    }
                }
            }
        }
        return owners.get(owners.size() - 1);
    }

    /** Resolves a (relative) import uri against the importing library's directory to a user library. */
    public Ast.Library resolveImportedLibrary(Ast.Library from, String uri) {
        if (uri == null || from == null || uri.startsWith("dart:")) {
            return null;
        }
        String combined;
        if (uri.startsWith("package:")) {
            // package:<own>/x.dart is this project's lib/x.dart; any other package is
            // external (a stub or unsupported) and never a user library.
            int slash = uri.indexOf('/');
            if (slash < 0) {
                return null;
            }
            if (!ownPackages.contains(uri.substring("package:".length(), slash))) {
                // A pub package transpiled with the app: cn1:import-flutter-project copies
                // the files the app reaches to packages/<name>/, beside the app's own.
                String inPackage = normalizePath("packages/" + uri.substring("package:".length()));
                for (Ast.Library lib : libraries) {
                    if (normalizePath(lib.fileName.replace('\\', '/')).equals(inPackage)) {
                        return lib;
                    }
                }
                return null;
            }
            String rest = normalizePath(uri.substring(slash + 1));
            for (Ast.Library lib : libraries) {
                String f = normalizePath(lib.fileName.replace('\\', '/'));
                if (f.equals(rest) || f.equals("lib/" + rest)) {
                    return lib;
                }
            }
            return null;
        } else {
            combined = resolveRelative(from, uri);
        }
        for (Ast.Library lib : libraries) {
            if (normalizePath(lib.fileName.replace('\\', '/')).equals(combined)) {
                return lib;
            }
        }
        return null;
    }

    private static String normalizePath(String path) {
        String[] parts = path.split("/");
        List<String> out = new ArrayList<String>();
        for (String p : parts) {
            if (p.isEmpty() || p.equals(".")) {
                continue;
            }
            if (p.equals("..")) {
                if (!out.isEmpty() && !out.get(out.size() - 1).equals("..")) {
                    out.remove(out.size() - 1);
                } else {
                    out.add(p);
                }
            } else {
                out.add(p);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.size(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(out.get(i));
        }
        return sb.toString();
    }

    /**
     * Finds an extension member for the given receiver type name, as seen from library
     * {@code from}: Dart applies only an extension that library declares or imports
     * (directly, or through an export, and not removed by show/hide). Two libraries that each
     * declare an extension on String with the same member then each get their own; the first
     * registered answered for both. When none is visible -- an import this cannot resolve,
     * such as a package: uri -- the first match answers, as before.
     */
    public Ast.ClassDecl findExtension(String typeName, String member, boolean getter, Ast.Library from) {
        Ast.ClassDecl first = null;
        for (Ast.ClassDecl ext : extensions) {
            if (!ext.extensionOn.name.equals(typeName)) {
                continue;
            }
            for (Ast.MethodDecl m : ext.methods) {
                if (m.name.equals(member) && m.isGetter == getter && !m.isSetter) {
                    if (from == null || extensionVisible(ext, from)) {
                        return ext;
                    }
                    if (first == null) {
                        first = ext;
                    }
                    break;
                }
            }
        }
        return first;
    }

    /** Whether {@code from} declares {@code ext} or imports it. */
    public boolean extensionVisible(Ast.ClassDecl ext, Ast.Library from) {
        if (ext.ownerLibrary == from) {
            return true;
        }
        if (!ext.unnamedExtension && ext.name != null && ext.name.startsWith("_")) {
            // A private extension is visible only in its own library (parts included): no
            // import or export reaches it.
            return false;
        }
        for (Ast.Directive d : from.importDirectives) {
            if (exportsExtension(resolveImportedLibrary(from, d.uri), d, ext, new HashSet<Ast.Library>())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code lib}, reached through directive {@code via}, provides {@code ext}: it
     * declares it, or re-exports a library that does, every directive on the way admitting
     * the extension's name.
     */
    private boolean exportsExtension(Ast.Library lib, Ast.Directive via, Ast.ClassDecl ext,
                                     Set<Ast.Library> seen) {
        if (lib == null || (ext.unnamedExtension ? via.show != null : !via.admits(ext.name))) {
            return false;
        }
        if (!seen.add(lib)) {
            return false;
        }
        if (ext.ownerLibrary == lib) {
            return true;
        }
        for (Ast.Directive d : lib.exportDirectives) {
            if (exportsExtension(resolveImportedLibrary(lib, d.uri), d, ext, seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Java class name hosting a library's top-level functions: main.dart -> MainLib.
     * The FULL relative path is encoded (not just the basename) so libraries that share
     * a basename across directories — e.g. the six {@code app.dart} / seven
     * {@code routes.dart} files in the Flutter Gallery — get distinct classes instead of
     * colliding into one and losing members.
     */
    public static String libClassName(String fileName) {
        String base = fileName.replace('\\', '/');
        if (base.endsWith(".dart")) {
            base = base.substring(0, base.length() - 5);
        }
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (c == '_' || c == '-' || c == '.' || c == '/') {
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        if (sb.length() == 0) {
            sb.append("Lib0");
        }
        if (Character.isDigit(sb.charAt(0))) {
            sb.insert(0, '_');
        }
        return sb.append("Lib").toString();
    }
    /**
     * Dart library privacy: a member named {@code _x} declared in one library is a different
     * member from an {@code _x} declared in another, even on a subclass, so neither overrides
     * nor answers for the other. Java has no such scoping, so every library but the first to
     * declare a colliding private member name has that name renamed throughout its own
     * declarations ({@code _x$p1}); the name is private, so nothing outside the library can
     * spell it. Type, top-level and constructor names are left alone, as are stub libraries,
     * whose members map onto Java names.
     *
     * <p>A library that also declares the name at top level (or as a type) has only its
     * MEMBER references renamed, by resolution rather than spelling
     * ({@link #renamePrivateMembers}): member declarations, {@code o._x}, {@code #_x}, field
     * initializers and a bare {@code _x} that Dart resolves to a member of the enclosing
     * class. Its top-level {@code _x} keeps its name; top-level declarations are told apart
     * by library already.
     */
    /// For each library (by file name) whose private names were renamed: renamed name to the
    /// name as written. The generated classes of that library register it with the runtime
    /// ({@code Symbol.spell}), so an Invocation's memberName prints the Dart spelling; a
    /// `#_x` literal carries its own.
    public final Map<String, Map<String, String>> privateSpellings = new java.util.HashMap<String, Map<String, String>>();

    /// {@code candidate}, or it with {@code $} appended until no library spells it: a user
    /// identifier may itself be written {@code _x$p2}.
    private static String freshPrivateName(String candidate, Set<String> spelled) {
        String n = candidate;
        while (spelled.contains(n)) {
            n = n + "$";
        }
        return n;
    }

    private void separateLibraryPrivateMembers() {
        Map<String, List<Ast.Library>> declaring = new LinkedHashMap<String, List<Ast.Library>>();
        // Top-level names are excluded per library: renaming a member whose name a top-level
        // declaration of the SAME library also has would rename that declaration's references
        // too. Another library declaring only the member is still separated.
        Map<Ast.Library, Set<String>> excludedBy = new java.util.IdentityHashMap<Ast.Library, Set<String>>();
        Map<Ast.Library, Set<String>> enumEntriesBy = new java.util.IdentityHashMap<Ast.Library, Set<String>>();
        for (Ast.Library lib : libraries) {
            Set<String> excluded = new HashSet<String>();
            excludedBy.put(lib, excluded);
            for (Ast.ClassDecl c : lib.classes) {
                excluded.add(c.name);
            }
            Set<String> entries = new HashSet<String>();
            enumEntriesBy.put(lib, entries);
            for (Ast.EnumDecl e : lib.enums) {
                excluded.add(e.name);
                // Enum values keep their names (renamePrivate does not reach them); they are
                // reached through their enum, so another library's `_x` cannot meet them.
                excluded.addAll(e.entries);
                entries.addAll(e.entries);
            }
            for (Ast.TypedefDecl t : lib.typedefs) {
                excluded.add(t.name);
            }
            for (Ast.FunctionDecl f : lib.functions) {
                excluded.add(f.name);
            }
            for (Ast.FieldDecl f : lib.topLevelVars) {
                excluded.add(f.name);
            }
        }
        for (Ast.Library lib : libraries) {
            Set<String> own = new HashSet<String>();
            boolean stub = false;
            for (Ast.ClassDecl c : lib.classes) {
                stub |= c.javaName != null;
                for (Ast.FieldDecl f : c.fields) {
                    own.add(f.name);
                }
                for (Ast.MethodDecl m : c.methods) {
                    own.add(m.name);
                }
            }
            for (Ast.EnumDecl e : lib.enums) {
                stub |= e.javaName != null;
                for (Ast.FieldDecl f : e.fields) {
                    own.add(f.name);
                }
                for (Ast.MethodDecl m : e.methods) {
                    own.add(m.name);
                }
            }
            if (stub) {
                continue;
            }
            // Every private name the library spells as a member, a local or a `#_x` literal
            // takes part too, though it declares no member of that name: a dynamic `d._x` or
            // a `#_x` from this library must not reach (or equal) another library's `_x`, and
            // renaming the library's own spelling is what keeps it apart.
            collectPrivateNames(lib, own, java.util.Collections.newSetFromMap(
                    new java.util.IdentityHashMap<Object, Boolean>()));
            for (String n : own) {
                if (n != null && n.startsWith("_") && n.length() > 1) {
                    List<Ast.Library> l = declaring.get(n);
                    if (l == null) {
                        l = new ArrayList<Ast.Library>();
                        declaring.put(n, l);
                    }
                    l.add(lib);
                }
            }
        }
        Map<Ast.Library, Map<String, String>> renames = new java.util.IdentityHashMap<Ast.Library, Map<String, String>>();
        Map<Ast.Library, Map<String, String>> memberRenames = new java.util.IdentityHashMap<Ast.Library, Map<String, String>>();
        for (Map.Entry<String, List<Ast.Library>> en : declaring.entrySet()) {
            List<Ast.Library> l = en.getValue();
            if (l.size() < 2) {
                continue;
            }
            // A library that cannot rename the name at all (an enum value) keeps it; failing
            // that the first library does. Every other one is renamed: wholly when it has no
            // top-level of that name, otherwise its member references only.
            Ast.Library keeper = l.get(0);
            for (Ast.Library lib : l) {
                if (enumEntriesBy.get(lib).contains(en.getKey())) {
                    keeper = lib;
                    break;
                }
            }
            for (int i = 0; i < l.size(); i++) {
                Ast.Library lib = l.get(i);
                if (lib == keeper || enumEntriesBy.get(lib).contains(en.getKey())) {
                    continue;
                }
                if (excludedBy.get(lib).contains(en.getKey())) {
                    Map<String, String> m = memberRenames.get(lib);
                    if (m == null) {
                        m = new java.util.HashMap<String, String>();
                        memberRenames.put(lib, m);
                    }
                    m.put(en.getKey(), freshPrivateName(en.getKey() + "$p" + libraries.indexOf(lib), declaring.keySet()));
                    continue;
                }
                Map<String, String> m = renames.get(l.get(i));
                if (m == null) {
                    m = new java.util.HashMap<String, String>();
                    renames.put(l.get(i), m);
                }
                m.put(en.getKey(), freshPrivateName(en.getKey() + "$p" + libraries.indexOf(l.get(i)), declaring.keySet()));
            }
        }
        for (Map<Ast.Library, Map<String, String>> all : java.util.Arrays.asList(memberRenames, renames)) {
            for (Map.Entry<Ast.Library, Map<String, String>> en : all.entrySet()) {
                Map<String, String> sp = privateSpellings.get(en.getKey().fileName);
                if (sp == null) {
                    sp = new TreeMap<String, String>();
                    privateSpellings.put(en.getKey().fileName, sp);
                }
                for (Map.Entry<String, String> r : en.getValue().entrySet()) {
                    sp.put(r.getValue(), r.getKey());
                }
            }
        }
        for (Map.Entry<Ast.Library, Map<String, String>> en : memberRenames.entrySet()) {
            renamePrivateMembers(en.getKey(), en.getValue());
        }
        for (Map.Entry<Ast.Library, Map<String, String>> en : renames.entrySet()) {
            renamePrivate(en.getKey(), en.getValue(), java.util.Collections.newSetFromMap(
                    new java.util.IdentityHashMap<Object, Boolean>()));
        }
    }

    /**
     * Renames the member references to {@code names} in {@code lib}, leaving its top-level
     * declarations of those names and their references alone. A bare identifier is resolved
     * as Dart resolves it lexically: a local of that name wins, then a member the enclosing
     * class (or extension) itself declares, then the top-level; an inherited member is never
     * in lexical scope, so with a top-level of the name present it is reached only through
     * {@code this}. Locals are scoped lexically, as Dart scopes them: a local shadows the
     * member only inside the block (loop, catch clause, closure, case) that declares it and
     * from its declaration on, so the member is still the one reached before it, in sibling
     * blocks and in closures declared earlier.
     */
    private static void renamePrivateMembers(Ast.Library lib, Map<String, String> names) {
        Set<Object> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
        for (Ast.ClassDecl c : lib.classes) {
            Set<String> declared = new HashSet<String>();
            for (Ast.FieldDecl f : c.fields) {
                declared.add(f.name);
            }
            for (Ast.MethodDecl m : c.methods) {
                declared.add(m.name);
            }
            renameMembersIn(c, names, declared, new LocalScopes(), seen);
        }
        for (Ast.EnumDecl e : lib.enums) {
            Set<String> declared = new HashSet<String>();
            for (Ast.FieldDecl f : e.fields) {
                declared.add(f.name);
            }
            for (Ast.MethodDecl m : e.methods) {
                declared.add(m.name);
            }
            renameMembersIn(e, names, declared, new LocalScopes(), seen);
        }
        // Everything else (top-level functions and variables, typedefs) has no enclosing
        // class: a bare name there is never a member.
        renameMembersIn(lib, names, java.util.Collections.<String>emptySet(), new LocalScopes(), seen);
    }

    /// The local scopes open at a point of the walk, innermost last.
    private static final class LocalScopes {
        private final java.util.ArrayList<Set<String>> stack = new java.util.ArrayList<Set<String>>();

        void push() {
            stack.add(new HashSet<String>());
        }

        void pop() {
            stack.remove(stack.size() - 1);
        }

        void declare(String name) {
            if (name != null && !stack.isEmpty()) {
                stack.get(stack.size() - 1).add(name);
            }
        }

        boolean declares(String name) {
            for (Set<String> s : stack) {
                if (s.contains(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    /// Whether {@code o} opens a scope of its own for the locals it (or its body) declares.
    private static boolean opensScope(Object o) {
        return o instanceof Ast.Block || o instanceof Ast.MethodDecl || o instanceof Ast.CtorDecl
                || o instanceof Ast.FunctionDecl || o instanceof Ast.Lambda || o instanceof Ast.ForStmt
                || o instanceof Ast.ForInStmt || o instanceof Ast.CatchClause || o instanceof Ast.SwitchCase
                || o instanceof Ast.SwitchExprCase || o instanceof Ast.ForElement;
    }

    private static void renameMembersIn(Object o, Map<String, String> names, Set<String> inScope,
            LocalScopes locals, Set<Object> seen) {
        if (o == null || o instanceof String || o instanceof Ast.TypeRef || !seen.add(o)) {
            return;
        }
        if (o instanceof Iterable) {
            for (Object e : (Iterable<?>) o) {
                renameMembersIn(e, names, inScope, locals, seen);
            }
            return;
        }
        if (!(o instanceof Ast.Node) && !(o instanceof Ast.Args) && !(o instanceof Ast.NamedArg)) {
            return;
        }
        if (o instanceof Ast.IfStmt && ((Ast.IfStmt) o).casePattern != null) {
            // An if-case's pattern variables are in scope in its guard and then branch only.
            Ast.IfStmt is = (Ast.IfStmt) o;
            renameMembersIn(is.condition, names, inScope, locals, seen);
            locals.push();
            renameMembersIn(is.casePattern, names, inScope, locals, seen);
            renameMembersIn(is.caseGuard, names, inScope, locals, seen);
            renameMembersIn(is.thenStmt, names, inScope, locals, seen);
            locals.pop();
            renameMembersIn(is.elseStmt, names, inScope, locals, seen);
            return;
        }
        if (o instanceof Ast.LocalFunc) {
            // Its name is in scope in the enclosing block (and so in its own body).
            locals.declare(((Ast.LocalFunc) o).name);
        }
        boolean scoped = opensScope(o) || o instanceof Ast.LocalFunc;
        if (scoped) {
            locals.push();
        }
        try {
            renameMembersInNode(o, names, inScope, locals, seen);
        } finally {
            if (scoped) {
                locals.pop();
            }
        }
        // A variable is in scope from its declaration on (Dart rejects a use before it in the
        // same block, so its own initializer never meets it).
        if (o instanceof Ast.VarDeclStmt) {
            locals.declare(((Ast.VarDeclStmt) o).name);
        }
    }

    private static void renameMembersInNode(Object o, Map<String, String> names, Set<String> inScope,
            LocalScopes locals, Set<Object> seen) {
        if (o instanceof Ast.Param && !((Ast.Param) o).isThis && !((Ast.Param) o).isSuper) {
            locals.declare(((Ast.Param) o).name);
        } else if (o instanceof Ast.VariablePattern) {
            locals.declare(((Ast.VariablePattern) o).name);
        } else if (o instanceof Ast.ForInStmt) {
            locals.declare(((Ast.ForInStmt) o).varName);
        } else if (o instanceof Ast.CatchClause) {
            locals.declare(((Ast.CatchClause) o).exceptionVar);
            locals.declare(((Ast.CatchClause) o).stackVar);
        }
        String sym = privateSymbol(o);
        if (sym != null && names.containsKey(sym)) {
            renameSymbol((Ast.Call) o, names.get(sym));
        }
        String rename = null;
        if (o instanceof Ast.MethodDecl) {
            rename = ((Ast.MethodDecl) o).name;
        } else if (o instanceof Ast.FieldDecl) {
            // Only class and enum fields are reached with a class in scope; a top-level
            // variable is reached from the library walk, where inScope is empty.
            rename = inScope.isEmpty() ? null : ((Ast.FieldDecl) o).name;
        } else if (o instanceof Ast.PropertyGet) {
            rename = ((Ast.PropertyGet) o).target != null || memberInScope(((Ast.PropertyGet) o).name, inScope, locals)
                    ? ((Ast.PropertyGet) o).name : null;
        } else if (o instanceof Ast.Call) {
            Ast.Call c = (Ast.Call) o;
            rename = c.target != null || memberInScope(c.name, inScope, locals) ? c.name : null;
        } else if (o instanceof Ast.Ident) {
            rename = memberInScope(((Ast.Ident) o).name, inScope, locals) ? ((Ast.Ident) o).name : null;
        } else if (o instanceof Ast.FieldInit) {
            rename = ((Ast.FieldInit) o).field;
        } else if (o instanceof Ast.Param && (((Ast.Param) o).isThis || ((Ast.Param) o).isSuper)) {
            rename = ((Ast.Param) o).name;
        } else if (o instanceof Ast.PatternField) {
            rename = ((Ast.PatternField) o).name;
        }
        if (rename != null && names.containsKey(rename)) {
            String to = names.get(rename);
            if (o instanceof Ast.MethodDecl) {
                ((Ast.MethodDecl) o).name = to;
            } else if (o instanceof Ast.FieldDecl) {
                ((Ast.FieldDecl) o).name = to;
            } else if (o instanceof Ast.PropertyGet) {
                ((Ast.PropertyGet) o).name = to;
            } else if (o instanceof Ast.Call) {
                ((Ast.Call) o).name = to;
            } else if (o instanceof Ast.Ident) {
                ((Ast.Ident) o).name = to;
            } else if (o instanceof Ast.FieldInit) {
                ((Ast.FieldInit) o).field = to;
            } else if (o instanceof Ast.Param) {
                ((Ast.Param) o).name = to;
            } else {
                ((Ast.PatternField) o).name = to;
            }
        }
        for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(mod) || f.getType().isPrimitive() || f.getType() == String.class
                        || f.getType() == Ast.Library.class || f.getType() == Ast.ClassDecl.class
                        || f.getType() == Ast.EnumDecl.class || f.getType() == Ast.TypeRef.class) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    renameMembersIn(f.get(o), names, inScope, locals, seen);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    private static boolean memberInScope(String name, Set<String> inScope, LocalScopes locals) {
        return inScope.contains(name) && !locals.declares(name);
    }

    /// Renames a `#_x` literal to {@code to}, keeping the name as written for its spelling:
    /// the literal becomes {@code Symbol("_x$p2", "_x")}, so it equals only this library's
    /// `#_x` but prints as Dart prints it.
    private static void renameSymbol(Ast.Call c, String to) {
        Ast.StringLit lit = (Ast.StringLit) c.args.positional.get(0);
        String was = (String) lit.parts.get(0);
        lit.parts.set(0, to);
        if (c.args.positional.size() == 1) {
            Ast.StringLit spelled = new Ast.StringLit();
            spelled.parts.add(was);
            c.args.positional.add(spelled);
        }
    }

    /**
     * A private typedef ({@code typedef _Fn = ...}) or extension declared in two libraries
     * names two different things, but the {@link #typedefs} table is keyed by name alone and
     * an extension is emitted as a Java class of its own name, so the last one answered for
     * both. Classes, enums and mixins keep their names and are
     * told apart by library (see {@code JavaEmitter.javaClassName}); an alias leaves no
     * runtime trace, so every library but the first to declare the name simply has it
     * renamed ({@code _Fn$t2}) throughout its own declarations, as
     * {@link #separateLibraryPrivateMembers} does for members.
     */
    private void separateLibraryPrivateTypedefs() {
        // Extensions are reached as list elements; ClassDecl-typed fields are skipped below.
        Map<String, Ast.Library> first = new java.util.HashMap<String, Ast.Library>();
        Map<Ast.Library, Map<String, String>> renames = new java.util.IdentityHashMap<Ast.Library, Map<String, String>>();
        for (Ast.Library lib : libraries) {
            List<String> declared = new ArrayList<String>();
            for (Ast.TypedefDecl t : lib.typedefs) {
                declared.add(t.name);
            }
            for (Ast.ClassDecl c : lib.classes) {
                if (c.extensionOn != null && !c.unnamedExtension) {
                    declared.add(c.name);
                }
            }
            for (String name : declared) {
                if (name == null || !name.startsWith("_")) {
                    continue;
                }
                Ast.Library owner = first.get(name);
                if (owner == null) {
                    first.put(name, lib);
                } else if (owner != lib) {
                    Map<String, String> m = renames.get(lib);
                    if (m == null) {
                        m = new java.util.HashMap<String, String>();
                        renames.put(lib, m);
                    }
                    m.put(name, name + "$t" + libraries.indexOf(lib));
                }
            }
        }
        if (renames.isEmpty()) {
            return;
        }
        for (Map.Entry<Ast.Library, Map<String, String>> en : renames.entrySet()) {
            renameTypeNames(en.getKey(), en.getValue(), java.util.Collections.newSetFromMap(
                    new java.util.IdentityHashMap<Object, Boolean>()));
        }
        typedefs.clear();
        for (Ast.Library lib : libraries) {
            for (Ast.TypedefDecl t : lib.typedefs) {
                if (!typedefs.containsKey(t.name) || !t.name.startsWith("_")) {
                    typedefs.put(t.name, t);
                }
            }
        }
    }

    /** Renames type names (type references, typedef declarations, identifiers) under {@code o}. */
    private static void renameTypeNames(Object o, Map<String, String> names, Set<Object> seen) {
        if (o == null || o instanceof String || !seen.add(o)) {
            return;
        }
        if (o instanceof Iterable) {
            for (Object e : (Iterable<?>) o) {
                renameTypeNames(e, names, seen);
            }
            return;
        }
        if (!(o instanceof Ast.Node) && !(o instanceof Ast.Args) && !(o instanceof Ast.NamedArg)
                && !(o instanceof Ast.TypeRef)) {
            return;
        }
        boolean named = o instanceof Ast.TypeRef || o instanceof Ast.TypedefDecl || o instanceof Ast.Ident
                || o instanceof Ast.Call
                || o instanceof Ast.ClassDecl && ((Ast.ClassDecl) o).extensionOn != null;
        for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(mod) || f.getType().isPrimitive()
                        || f.getType() == Ast.Library.class || f.getType() == Ast.ClassDecl.class
                        || f.getType() == Ast.EnumDecl.class) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (f.getType() == String.class) {
                        if (named && "name".equals(f.getName()) && names.containsKey(v)) {
                            f.set(o, names.get(v));
                        }
                    } else {
                        renameTypeNames(v, names, seen);
                    }
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    /// The private names {@link #renamePrivate} would rename in {@code o}, into {@code out}.
    private static void collectPrivateNames(Object o, Set<String> out, Set<Object> seen) {
        if (o == null || o instanceof String || o instanceof Ast.TypeRef || !seen.add(o)) {
            return;
        }
        if (o instanceof Iterable) {
            for (Object e : (Iterable<?>) o) {
                collectPrivateNames(e, out, seen);
            }
            return;
        }
        if (!(o instanceof Ast.Node) && !(o instanceof Ast.Args) && !(o instanceof Ast.NamedArg)) {
            return;
        }
        String sym = privateSymbol(o);
        if (sym != null) {
            out.add(sym);
        }
        boolean typeName = o instanceof Ast.ClassDecl || o instanceof Ast.EnumDecl || o instanceof Ast.TypedefDecl
                || o instanceof Ast.Library;
        for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(mod) || f.getType().isPrimitive()
                        || f.getType() == Ast.Library.class || f.getType() == Ast.ClassDecl.class
                        || f.getType() == Ast.EnumDecl.class || f.getType() == Ast.TypeRef.class) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (f.getType() == String.class) {
                        String nm = f.getName();
                        if (!typeName && ("name".equals(nm) || "field".equals(nm)) && v != null
                                && ((String) v).startsWith("_")) {
                            out.add((String) v);
                        }
                    } else {
                        collectPrivateNames(v, out, seen);
                    }
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    /// The name of a `#_x` literal, or null.
    private static String privateSymbol(Object o) {
        if (!(o instanceof Ast.Call) || !((Ast.Call) o).symbolLiteral) {
            return null;
        }
        Ast.Call c = (Ast.Call) o;
        if (c.args.positional.size() != 1 || !(c.args.positional.get(0) instanceof Ast.StringLit)) {
            return null;
        }
        List<Object> parts = ((Ast.StringLit) c.args.positional.get(0)).parts;
        if (parts.size() != 1 || !(parts.get(0) instanceof String) || !((String) parts.get(0)).startsWith("_")) {
            return null;
        }
        return (String) parts.get(0);
    }

    private static void renamePrivate(Object o, Map<String, String> names, Set<Object> seen) {
        if (o == null || o instanceof String || o instanceof Ast.TypeRef || !seen.add(o)) {
            return;
        }
        String sym = privateSymbol(o);
        if (sym != null && names.containsKey(sym)) {
            renameSymbol((Ast.Call) o, names.get(sym));
        }
        if (o instanceof Iterable) {
            for (Object e : (Iterable<?>) o) {
                renamePrivate(e, names, seen);
            }
            return;
        }
        if (!(o instanceof Ast.Node) && !(o instanceof Ast.Args) && !(o instanceof Ast.NamedArg)) {
            return;
        }
        boolean typeName = o instanceof Ast.ClassDecl || o instanceof Ast.EnumDecl || o instanceof Ast.TypedefDecl
                || o instanceof Ast.Library;
        for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(mod) || f.getType().isPrimitive()
                        || f.getType() == Ast.Library.class || f.getType() == Ast.ClassDecl.class
                        || f.getType() == Ast.EnumDecl.class || f.getType() == Ast.TypeRef.class) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (f.getType() == String.class) {
                        String nm = f.getName();
                        if (!typeName && ("name".equals(nm) || "field".equals(nm)) && names.containsKey(v)) {
                            f.set(o, names.get(v));
                        }
                    } else {
                        renamePrivate(v, names, seen);
                    }
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

}
