package com.codename1.dart.transpiler.analyze;

import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.parser.AstBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The hand-written runtime API as seen from Dart: classes, enums and
 * top-level functions declared in signature-stub .dart files (parsed with
 * the same front end as user code). Every entry carries its Java name via
 * the stub's {@code @JavaName} annotation.
 */
public final class StubRegistry {

    public final Map<String, Ast.ClassDecl> classes = new LinkedHashMap<String, Ast.ClassDecl>();
    public final Map<String, Ast.EnumDecl> enums = new LinkedHashMap<String, Ast.EnumDecl>();
    public final Map<String, Ast.FunctionDecl> functions = new LinkedHashMap<String, Ast.FunctionDecl>();

    /** Loads the embedded stub set (fallback when the classpath has none). */
    public static StubRegistry loadEmbedded(Diagnostics diags) {
        StubRegistry r = new StubRegistry();
        r.loadResource("/com/codename1/dart/stubs/flutter_material.dart", diags);
        return r;
    }

    /**
     * Loads stubs from META-INF/dart/*.dart inside the given jars or
     * directories (the runtime dependencies of the app being transpiled).
     * Falls back to the embedded stub set when nothing contributes.
     */
    public static StubRegistry loadFromClasspath(java.util.List<java.io.File> entries, Diagnostics diags) {
        StubRegistry r = new StubRegistry();
        for (java.io.File entry : entries) {
            try {
                if (entry.isDirectory()) {
                    java.io.File dir = new java.io.File(entry, "META-INF/dart");
                    java.io.File[] files = dir.listFiles();
                    if (files != null) {
                        for (java.io.File f : files) {
                            if (f.getName().endsWith(".dart")) {
                                byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
                                r.load(f.getName(), new String(data, StandardCharsets.UTF_8), diags);
                            }
                        }
                    }
                } else if (entry.getName().endsWith(".jar") && entry.exists()) {
                    java.util.zip.ZipFile zip = new java.util.zip.ZipFile(entry);
                    try {
                        java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries();
                        while (en.hasMoreElements()) {
                            java.util.zip.ZipEntry ze = en.nextElement();
                            if (!ze.isDirectory() && ze.getName().startsWith("META-INF/dart/")
                                    && ze.getName().endsWith(".dart")) {
                                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                                InputStream in = zip.getInputStream(ze);
                                byte[] chunk = new byte[8192];
                                int n;
                                while ((n = in.read(chunk)) > 0) {
                                    buf.write(chunk, 0, n);
                                }
                                r.load(entry.getName() + "!" + ze.getName(),
                                        new String(buf.toByteArray(), StandardCharsets.UTF_8), diags);
                            }
                        }
                    } finally {
                        zip.close();
                    }
                }
            } catch (IOException e) {
                diags.error(entry.getName(), 0, 0, "E0903", "Failed scanning for Dart stubs: " + e);
            }
        }
        if (r.classes.isEmpty() && r.functions.isEmpty()) {
            return loadEmbedded(diags);
        }
        return r;
    }

    public void loadResource(String resource, Diagnostics diags) {
        InputStream in = StubRegistry.class.getResourceAsStream(resource);
        if (in == null) {
            diags.error(resource, 0, 0, "E0901", "Missing embedded stub resource: " + resource);
            return;
        }
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) {
                buf.write(chunk, 0, n);
            }
            String src = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            load(resource, src, diags);
        } catch (IOException e) {
            diags.error(resource, 0, 0, "E0902", "Failed reading stub resource: " + e);
        }
    }

    public void load(String name, String source, Diagnostics diags) {
        AstBuilder builder = new AstBuilder(diags);
        Ast.Library lib = builder.parse(name, source);
        for (Ast.ClassDecl c : lib.classes) {
            classes.put(c.name, c);
        }
        for (Ast.EnumDecl e : lib.enums) {
            enums.put(e.name, e);
        }
        for (Ast.FunctionDecl f : lib.functions) {
            functions.put(f.name, f);
        }
    }

    public boolean isStubClass(String dartName) {
        return classes.containsKey(dartName);
    }

    public boolean isStubEnum(String dartName) {
        return enums.containsKey(dartName);
    }

    /** Walks the stub superclass chain looking for a member. */
    public Ast.MethodDecl findMethod(String className, String member, boolean getter) {
        Ast.ClassDecl c = classes.get(className);
        while (c != null) {
            for (Ast.MethodDecl m : c.methods) {
                if (m.name.equals(member) && m.isGetter == getter && !m.isSetter) {
                    return m;
                }
            }
            c = c.superclass != null ? classes.get(c.superclass.name) : null;
        }
        return null;
    }

    /** The unnamed constructor of a stub class (or null). */
    public Ast.CtorDecl ctorOf(String className) {
        Ast.ClassDecl c = classes.get(className);
        return c == null ? null : c.defaultCtor();
    }
}
