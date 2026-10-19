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
        Set<Ast.ClassDecl> done = new HashSet<Ast.ClassDecl>();
        for (Ast.Library lib : libraries) {
            for (Ast.ClassDecl c : lib.classes) {
                forwardSuperCtors(c, done);
            }
        }
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

    private void mergePart(Ast.Library owner, Ast.Library part) {
        owner.classes.addAll(part.classes);
        owner.enums.addAll(part.enums);
        owner.functions.addAll(part.functions);
        owner.topLevelVars.addAll(part.topLevelVars);
        owner.typedefs.addAll(part.typedefs);
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
            Ast.Library target = resolveImportedLibrary(fromLibrary, fromLibrary.prefixImports.get(prefix));
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
                Ast.Library target = d.admits(name) ? resolveImportedLibrary(fromLibrary, d.uri) : null;
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
            if (slash < 0 || !ownPackages.contains(uri.substring("package:".length(), slash))) {
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
}
