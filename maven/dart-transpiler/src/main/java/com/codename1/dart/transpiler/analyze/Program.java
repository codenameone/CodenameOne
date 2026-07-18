package com.codename1.dart.transpiler.analyze;

import com.codename1.dart.transpiler.ast.Ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public final Map<String, Ast.FieldDecl> topLevelVars = new LinkedHashMap<String, Ast.FieldDecl>();

    public final List<Ast.ClassDecl> extensions = new ArrayList<Ast.ClassDecl>();

    public void add(Ast.Library lib) {
        libraries.add(lib);
        for (Ast.ClassDecl c : lib.classes) {
            if (c.extensionOn != null) {
                extensions.add(c);
            } else {
                classes.put(c.name, c);
            }
        }
        for (Ast.EnumDecl e : lib.enums) {
            enums.put(e.name, e);
        }
        for (Ast.FunctionDecl f : lib.functions) {
            functions.put(f.name, f);
            functionOwners.put(f.name, lib);
        }
        for (Ast.FieldDecl v : lib.topLevelVars) {
            topLevelVars.put(v.name, v);
            topLevelVarOwners.put(v.name, lib);
        }
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

    /** Java class name hosting a library's top-level functions: main.dart -> MainLib. */
    public static String libClassName(String fileName) {
        String base = fileName;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        if (base.endsWith(".dart")) {
            base = base.substring(0, base.length() - 5);
        }
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (c == '_' || c == '-' || c == '.') {
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
