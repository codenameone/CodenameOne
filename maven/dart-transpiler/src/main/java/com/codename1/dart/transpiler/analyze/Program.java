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

    public void add(Ast.Library lib) {
        libraries.add(lib);
        importPrefixes.addAll(lib.importPrefixes);
        for (Ast.ClassDecl c : lib.classes) {
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
        for (Ast.EnumDecl e : lib.enums) {
            enums.put(e.name, e);
        }
        for (Ast.FunctionDecl f : lib.functions) {
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
        for (Ast.FieldDecl v : lib.topLevelVars) {
            topLevelVars.put(v.name, v);
            topLevelVarOwners.put(v.name, lib);
            addOwner(topLevelVarOwnersByName, v.name, lib);
        }
        for (Ast.TypedefDecl t : lib.typedefs) {
            typedefs.put(t.name, t);
        }
    }

    /**
     * Resolves a user class by simple name, preferring a declaration in {@code fromLibrary}
     * when several libraries share the name (single-package name collision). Falls back to the
     * last-registered declaration ({@link #classes}) when there is no same-library match.
     */
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
            for (String uri : fromLibrary.imports) {
                Ast.Library target = resolveImportedLibrary(fromLibrary, uri);
                if (target != null) {
                    for (Ast.ClassDecl c : byName) {
                        if (c.ownerLibrary == target) {
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
        return resolveOwner(topLevelVarOwnersByName.get(name), topLevelVarOwners.get(name), from, prefix);
    }

    /** Same as {@link #resolveTopLevelVarOwner} for top-level functions / getters. */
    public Ast.Library resolveFunctionOwner(String name, Ast.Library from, String prefix) {
        return resolveOwner(functionOwnersByName.get(name), functionOwners.get(name), from, prefix);
    }

    private Ast.Library resolveOwner(List<Ast.Library> owners, Ast.Library fallback,
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
            if (target != null && owners.contains(target)) {
                return target;
            }
        }
        if (from != null) {
            if (owners.contains(from)) {
                return from;
            }
            for (Ast.Library o : owners) {
                for (String uri : from.imports) {
                    if (o == resolveImportedLibrary(from, uri)) {
                        return o;
                    }
                }
            }
        }
        return owners.get(owners.size() - 1);
    }

    /** Resolves a (relative) import uri against the importing library's directory to a user library. */
    public Ast.Library resolveImportedLibrary(Ast.Library from, String uri) {
        if (uri == null || from == null || uri.startsWith("dart:") || uri.startsWith("package:")) {
            return null;
        }
        String base = from.fileName.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        String dir = slash >= 0 ? base.substring(0, slash) : "";
        String combined = normalizePath(dir.isEmpty() ? uri : dir + "/" + uri);
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

    /** Finds an extension member for the given receiver type name. */
    public Ast.ClassDecl findExtension(String typeName, String member, boolean getter) {
        for (Ast.ClassDecl ext : extensions) {
            if (!ext.extensionOn.name.equals(typeName)) {
                continue;
            }
            for (Ast.MethodDecl m : ext.methods) {
                if (m.name.equals(member) && m.isGetter == getter && !m.isSetter) {
                    return ext;
                }
            }
        }
        return null;
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
