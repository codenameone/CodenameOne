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

package com.codename1.rive.schema;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Generates the Rive object model from rive-runtime's generated C++ base
/// headers (MIT, https://github.com/rive-app/rive-runtime), read from the
/// pinned copy in `src/main/schema/rive-runtime-generated.zip`.
///
/// Two modes, each run by a module's build into its `target/generated-sources`:
///
/// - `runtime <zip> <hand-written source root> <output root>`: for every type a
///   `<Name>Base.java` (keys, fields, defaults, generic property access) and,
///   when the source root has no hand-written `<Name>.java`, a trivial
///   `<Name>.java` placeholder. Used by `maven/rive-runtime`.
/// - `transcoder <zip> <output root>`: `RiveSchema.java`, the build-time table of
///   runtime classes and wire types. Used by `maven/rive-transcoder`.
///
/// Nothing generated is committed. A placeholder becomes hand-written logic by
/// adding `<Name>.java` to the runtime's sources; the generator then skips it.
public final class RiveSchemaGenerator {
    static final String GEN_ROOT = "include/rive/generated/";
    static final String GENERATOR = "maven/rive-schema-gen";

    private static final String HEADER = "/*\n"
            + " * MIT License\n"
            + " *\n"
            + " * Copyright (c) 2020 Rive\n"
            + " *\n"
            + " * Permission is hereby granted, free of charge, to any person obtaining a copy\n"
            + " * of this software and associated documentation files (the \"Software\"), to deal\n"
            + " * in the Software without restriction, including without limitation the rights\n"
            + " * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n"
            + " * copies of the Software, and to permit persons to whom the Software is\n"
            + " * furnished to do so, subject to the following conditions:\n"
            + " *\n"
            + " * The above copyright notice and this permission notice shall be included in all\n"
            + " * copies or substantial portions of the Software.\n"
            + " *\n"
            + " * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n"
            + " * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n"
            + " * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n"
            + " * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n"
            + " * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n"
            + " * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n"
            + " * SOFTWARE.\n"
            + " *\n"
            + " * Java rewrite of rive-runtime for Codename One.\n"
            + " * Codename One modifications Copyright (c) 2026 Codename One.\n"
            + " */\n";

    private static final Pattern CLASS_RE = Pattern.compile(
            "^class (\\w+)Base\\s*:\\s*public (\\w+)(?:,\\s*public (\\w+))?", Pattern.MULTILINE);
    private static final Pattern TYPEKEY_RE = Pattern.compile("static const uint16_t typeKey = (\\d+);");
    private static final Pattern PROP_RE = Pattern.compile("static const uint16_t (\\w+)PropertyKey = (\\d+);");
    private static final Pattern MEMBER_RE = Pattern.compile(
            "^  ([A-Za-z_][\\w:<> ]*?) m_(\\w+)(?: = ([^;]+))?;", Pattern.MULTILINE);
    private static final Pattern CASE_RE = Pattern.compile("case (\\w+)Base::(\\w+)PropertyKey:");
    private static final Pattern RETURN_RE = Pattern.compile("return (\\w+)Type::id;");
    private static final Pattern NEW_RE = Pattern.compile("return new (\\w+)\\(\\);");
    private static final Pattern METHOD_RE = Pattern.compile(
            "^\\s+(?:public|protected)[\\w\\s<>\\[\\]]*?\\s(\\w+)\\(", Pattern.MULTILINE);
    private static final Pattern SNAKE_RE = Pattern.compile("(?<=[a-z0-9])([A-Z])");

    /// Luau scripting is out of scope: those types are read and skipped. The
    /// exception is ScriptedInterpolator, which upstream's build without
    /// WITH_RIVE_SCRIPTING still instantiates as a linear interpolator, so a
    /// keyframe that names one keeps playing instead of being dropped.
    private static final String[] SCRIPTED_KEPT = {"ScriptedInterpolator"};

    private static final String[] COPY_ROOTS = {"DataConverter", "DataBind", "FormulaToken",
        "StringTemplateArgument", "DataConverterGroupItem"};

    private static final Map<String, String> JAVA_TYPE = new HashMap<String, String>();
    private static final Map<String, String> FAMILY = new HashMap<String, String>();
    private static final Map<String, Integer> FIELD_ID = new HashMap<String, Integer>();

    static {
        String[][] table = {
            {"CoreDouble", "float", "Double", "2"},
            {"CoreUint", "int", "Uint", "0"},
            {"CoreId", "int", "Uint", "0"},
            {"CoreInt", "int", "Uint", "0"},
            {"CoreColor", "int", "Color", "3"},
            {"CoreBool", "boolean", "Bool", "4"},
            {"CoreString", "String", "String", "1"},
            {"CoreBytes", "byte[]", "Bytes", "1"},
        };
        for (String[] row : table) {
            JAVA_TYPE.put(row[0], row[1]);
            FAMILY.put(row[0], row[2]);
            FIELD_ID.put(row[0], Integer.valueOf(row[3]));
        }
    }

    /// One property of a type.
    static final class Prop {
        String name;
        int key;
        String coreType;
        String defaultValue;
    }

    /// One type: its key, parent and properties, in header order.
    static final class Type {
        String name;
        int typeKey;
        String parent;
        String dir;
        final List<Prop> props = new ArrayList<Prop>();
    }

    /// The schema read from the zip.
    static final class Schema {
        final Map<String, Type> types = new TreeMap<String, Type>();
        final Set<String> concrete = new TreeSet<String>();
    }

    private final Schema schema;
    private final Map<String, Type> keep = new TreeMap<String, Type>();
    private final Set<String> concrete = new TreeSet<String>();
    private final File handWritten;
    private final Map<String, Set<String>> handMethodCache = new HashMap<String, Set<String>>();

    RiveSchemaGenerator(Schema schema, File handWritten) {
        this.schema = schema;
        this.handWritten = handWritten;
        for (Type t : schema.types.values()) {
            if (!excluded(t)) {
                keep.put(t.name, t);
            }
        }
        for (String n : schema.concrete) {
            if (keep.containsKey(n)) {
                concrete.add(n);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 4 && "runtime".equals(args[0])) {
            File src = new File(args[2], "com/codename1/impl/rive");
            File out = new File(args[3], "com/codename1/impl/rive");
            RiveSchemaGenerator g = new RiveSchemaGenerator(load(new File(args[1])), src);
            Map<String, String> files = g.runtimeFiles();
            writeAll(out, files);
        } else if (args.length == 3 && "transcoder".equals(args[0])) {
            RiveSchemaGenerator g = new RiveSchemaGenerator(load(new File(args[1])), null);
            Map<String, String> files = new TreeMap<String, String>();
            files.put("RiveSchema.java", g.schemaClass());
            writeAll(new File(args[2], "com/codename1/rive/transcoder"), files);
        } else {
            throw new IllegalArgumentException("usage: runtime <zip> <src root> <out root> | "
                    + "transcoder <zip> <out root>");
        }
    }

    private static void writeAll(File dir, Map<String, String> files) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        // A placeholder that became hand-written since the last run must not
        // linger here as a second copy of the class.
        File[] old = dir.listFiles();
        if (old != null) {
            for (File f : old) {
                if (f.getName().endsWith(".java") && !files.containsKey(f.getName()) && !f.delete()) {
                    throw new IOException("cannot delete stale " + f);
                }
            }
        }
        for (Map.Entry<String, String> e : files.entrySet()) {
            File f = new File(dir, e.getKey());
            byte[] bytes = e.getValue().getBytes(StandardCharsets.UTF_8);
            // Leave an unchanged file alone so incremental compiles stay incremental.
            if (f.isFile() && java.util.Arrays.equals(Files.readAllBytes(f.toPath()), bytes)) {
                continue;
            }
            Files.write(f.toPath(), bytes);
        }
    }

    // ---------------------------------------------------------------- reading

    static Schema load(File zip) throws IOException {
        Map<String, String> headers = new TreeMap<String, String>();
        InputStream fin = Files.newInputStream(zip.toPath());
        try {
            ZipInputStream in = new ZipInputStream(fin);
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (!e.isDirectory() && e.getName().startsWith(GEN_ROOT)) {
                    headers.put(e.getName().substring(GEN_ROOT.length()), readText(in));
                }
            }
        } finally {
            fin.close();
        }
        String registry = headers.get("core_registry.hpp");
        if (registry == null) {
            throw new IOException(zip + " has no " + GEN_ROOT + "core_registry.hpp");
        }
        Map<String, String> fieldIds = registryFieldIds(registry);
        Schema s = new Schema();
        for (Map.Entry<String, String> h : headers.entrySet()) {
            String path = h.getKey();
            if (!path.endsWith("_base.hpp")) {
                continue;
            }
            String text = h.getValue();
            Matcher cm = CLASS_RE.matcher(text);
            if (!cm.find()) {
                continue;
            }
            Type t = new Type();
            t.name = cm.group(1);
            String parent = cm.group(2);
            Matcher tk = TYPEKEY_RE.matcher(text);
            if (!tk.find()) {
                throw new IOException(path + " has no typeKey");
            }
            t.typeKey = Integer.parseInt(tk.group(1));
            t.parent = parent.endsWith("Base") && !"ColorChannelsBase".equals(parent)
                    ? parent.replace("Base", "") : parent;
            int slash = path.lastIndexOf('/');
            t.dir = slash < 0 ? "." : path.substring(0, slash);
            Map<String, String[]> members = new HashMap<String, String[]>();
            Matcher mm = MEMBER_RE.matcher(text);
            while (mm.find()) {
                String d = mm.group(3) == null ? "" : mm.group(3).trim();
                members.put(mm.group(2), new String[] {mm.group(1).trim(), d});
            }
            Matcher pm = PROP_RE.matcher(text);
            while (pm.find()) {
                Prop p = new Prop();
                p.name = pm.group(1);
                p.key = Integer.parseInt(pm.group(2));
                String[] m = members.get(cap(p.name));
                p.defaultValue = m == null ? "" : m[1];
                p.coreType = fieldIds.get(t.name + "::" + p.name);
                t.props.add(p);
            }
            s.types.put(t.name, t);
        }
        int at = registry.indexOf("static Core* makeCoreInstance");
        String make = registry.substring(at);
        make = make.substring(0, make.indexOf("\n  }\n"));
        Matcher nm = NEW_RE.matcher(make);
        while (nm.find()) {
            s.concrete.add(nm.group(1));
        }
        return s;
    }

    private static String readText(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        return normalize(new String(bo.toByteArray(), StandardCharsets.UTF_8));
    }

    /// Universal newlines, as the original generator's text-mode reads had.
    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

    /// `Class::property` to the core type of its wire field.
    private static Map<String, String> registryFieldIds(String registry) {
        String body = registry.substring(registry.indexOf("static int propertyFieldId(int propertyKey)"));
        body = body.substring(0, body.indexOf("\n  }\n"));
        Map<String, String> ids = new HashMap<String, String>();
        List<String> pending = new ArrayList<String>();
        for (String line : body.split("\n", -1)) {
            Matcher m = CASE_RE.matcher(line);
            if (m.find()) {
                pending.add(m.group(1) + "::" + m.group(2));
                continue;
            }
            m = RETURN_RE.matcher(line);
            if (m.find()) {
                String core = m.group(1);
                if (!FIELD_ID.containsKey(core)) {
                    throw new IllegalStateException("unknown field type " + core);
                }
                for (String k : pending) {
                    ids.put(k, core);
                }
                pending.clear();
            }
        }
        return ids;
    }

    // ------------------------------------------------------------- generating

    private static boolean excluded(Type t) {
        for (String k : SCRIPTED_KEPT) {
            if (k.equals(t.name)) {
                return false;
            }
        }
        return "scripted".equals(t.dir) || t.name.startsWith("Script");
    }

    static String snake(String name) {
        return SNAKE_RE.matcher(name).replaceAll("_$1").toUpperCase(Locale.ROOT);
    }

    static String cap(String n) {
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static String javaDefault(Prop p) {
        String jt = JAVA_TYPE.get(p.coreType);
        String d = p.defaultValue;
        if (d.length() == 0) {
            if ("float".equals(jt)) {
                return "0f";
            }
            if ("int".equals(jt)) {
                return "0";
            }
            if ("boolean".equals(jt)) {
                return "false";
            }
            if ("String".equals(jt)) {
                return "\"\"";
            }
            return "null";
        }
        if ("kEmptyId".equals(d)) {
            return "-1";
        }
        if ("Core::invalidPropertyKey".equals(d)) {
            return "0";
        }
        if ("String".equals(jt)) {
            return d.replace("\\x24", "$");
        }
        if ("float".equals(jt)) {
            return d.endsWith("f") ? d : d + "f";
        }
        return d;
    }

    private static List<Prop> typedProps(Type t) {
        List<Prop> out = new ArrayList<Prop>();
        for (Prop p : t.props) {
            if (p.coreType != null) {
                out.add(p);
            }
        }
        return out;
    }

    /// Names of the methods the hand-written `cls.java` declares, if there is one.
    private Set<String> handMethods(String cls) throws IOException {
        Set<String> cached = handMethodCache.get(cls);
        if (cached != null) {
            return cached;
        }
        Set<String> out = new HashSet<String>();
        File f = new File(handWritten, cls + ".java");
        if (f.isFile()) {
            String text = normalize(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            Matcher m = METHOD_RE.matcher(text);
            while (m.find()) {
                out.add(m.group(1));
            }
        }
        handMethodCache.put(cls, out);
        return out;
    }

    /// Methods declared by hand in an ancestor of `name`: a generated getter or
    /// setter with one of these names overrides it.
    private Set<String> inheritedHandMethods(String name) throws IOException {
        Set<String> out = new HashSet<String>();
        Type t = keep.get(name);
        String parent = t == null ? null : t.parent;
        while (parent != null && keep.containsKey(parent)) {
            out.addAll(handMethods(parent));
            parent = keep.get(parent).parent;
        }
        out.addAll(handMethods("Component"));
        out.addAll(handMethods("Core"));
        return out;
    }

    private static boolean isCopyRoot(String n) {
        for (String r : COPY_ROOTS) {
            if (r.equals(n)) {
                return true;
            }
        }
        return false;
    }

    private boolean copyFamily(String name) {
        String n = name;
        while (keep.containsKey(n)) {
            if (isCopyRoot(n)) {
                return true;
            }
            n = keep.get(n).parent;
        }
        return false;
    }

    // Upstream's generated XBase::copy and clone(): data binding clones its
    // converters (and their binds, items, tokens, arguments) per bind.
    private void genCopy(StringBuilder out, String name, List<Prop> props) {
        if (!copyFamily(name)) {
            return;
        }
        boolean root = isCopyRoot(name);
        // A type with no properties of its own inherits copyFrom: an override
        // that only called super would be PMD's UselessOverridingMethod.
        if (root || !props.isEmpty()) {
            out.append("    /// Copies this type's properties from `source`, which is of this type.\n");
            if (!root) {
                out.append("    @Override\n");
            }
            out.append("    protected void copyFrom(Core source) {\n");
            if (!root) {
                out.append("        super.copyFrom(source);\n");
            }
            if (!props.isEmpty()) {
                out.append("        ").append(name).append("Base o = (").append(name).append("Base) source;\n");
                for (Prop p : props) {
                    out.append("        m").append(cap(p.name)).append(" = o.m").append(cap(p.name)).append(";\n");
                }
            }
            out.append("    }\n\n");
        }
        boolean isConcrete = concrete.contains(name);
        if (root && !isConcrete) {
            out.append("    /// A copy of this object, or null for a type that cannot be built.\n");
            out.append("    public Core makeCopy() {\n        return null;\n    }\n\n");
        }
        if (isConcrete) {
            out.append(root ? "    /// A copy of this object.\n" : "    @Override\n");
            out.append("    public Core makeCopy() {\n        ").append(name).append(" c = new ").append(name)
                    .append("();\n        c.copyFrom(this);\n        return c;\n    }\n\n");
        }
    }

    String baseClass(String name) throws IOException {
        Type info = keep.get(name);
        Set<String> inherited = inheritedHandMethods(name);
        List<Prop> props = typedProps(info);
        StringBuilder out = new StringBuilder(HEADER);
        out.append("package com.codename1.impl.rive;\n\n");
        out.append("/// Generated from rive-runtime's `").append(name).append("Base` by `").append(GENERATOR)
                .append("`;\n");
        out.append("/// do not edit. Behaviour belongs in `").append(name).append("`.\n");
        out.append("public abstract class ").append(name).append("Base extends ").append(info.parent).append(" {\n");
        out.append("    public static final int TYPE_KEY = ").append(info.typeKey).append(";\n");
        for (Prop p : props) {
            out.append("    public static final int ").append(snake(p.name)).append("_PROPERTY_KEY = ")
                    .append(p.key).append(";\n");
        }
        out.append("\n");
        for (Prop p : props) {
            out.append("    protected ").append(JAVA_TYPE.get(p.coreType)).append(" m").append(cap(p.name))
                    .append(" = ").append(javaDefault(p)).append(";\n");
        }
        out.append("\n    @Override\n    public int coreType() {\n        return TYPE_KEY;\n    }\n\n");
        genCopy(out, name, props);
        out.append("    @Override\n    public boolean isTypeOf(int typeKey) {\n"
                + "        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);\n    }\n");
        for (Prop p : props) {
            String jt = JAVA_TYPE.get(p.coreType);
            String c = cap(p.name);
            String getter = ("boolean".equals(jt) ? "is" : "get") + c;
            if ("boolean".equals(jt) && c.startsWith("Is") && c.length() > 2 && Character.isUpperCase(c.charAt(2))) {
                getter = "is" + c.substring(2);
            }
            String ov = inherited.contains(getter) ? "    @Override\n" : "";
            out.append("\n").append(ov).append("    public ").append(jt).append(" ").append(getter)
                    .append("() {\n        return m").append(c).append(";\n    }\n");
            if ("byte[]".equals(jt)) {
                out.append("\n    protected void decode").append(c).append("(byte[] value) {\n        m").append(c)
                        .append(" = value;\n    }\n");
                continue;
            }
            // float: compare bits, an exact "unchanged" test (SpotBugs FE_*)
            String cmp;
            if ("float".equals(jt)) {
                cmp = "Float.floatToIntBits(m" + c + ") == Float.floatToIntBits(value)";
            } else if ("String".equals(jt)) {
                cmp = "(m" + c + " == null ? value == null : m" + c + ".equals(value))";
            } else {
                cmp = "m" + c + " == value";
            }
            ov = inherited.contains("set" + c) ? "    @Override\n" : "";
            out.append("\n").append(ov).append("    public void set").append(c).append("(").append(jt)
                    .append(" value) {\n        if (").append(cmp).append(") {\n            return;\n        }\n")
                    .append("        m").append(c).append(" = value;\n        ").append(p.name)
                    .append("Changed();\n    }\n");
            out.append("\n    protected void ").append(p.name).append("Changed() {\n    }\n");
        }
        Map<String, List<Prop>> fams = new TreeMap<String, List<Prop>>();
        for (Prop p : props) {
            String fam = FAMILY.get(p.coreType);
            List<Prop> l = fams.get(fam);
            if (l == null) {
                l = new ArrayList<Prop>();
                fams.put(fam, l);
            }
            l.add(p);
        }
        for (Map.Entry<String, List<Prop>> e : fams.entrySet()) {
            String fam = e.getKey();
            List<Prop> ps = e.getValue();
            String jt = "Uint".equals(fam) ? "int" : JAVA_TYPE.get(ps.get(0).coreType);
            out.append("\n    @Override\n    public boolean deserialize").append(fam).append("(int propertyKey, ")
                    .append(jt).append(" value) {\n        switch (propertyKey) {\n");
            for (Prop p : ps) {
                String c = cap(p.name);
                out.append("            case ").append(snake(p.name)).append("_PROPERTY_KEY:\n");
                if ("Bytes".equals(fam)) {
                    out.append("                decode").append(c).append("(value);\n                return true;\n");
                } else {
                    out.append("                m").append(c).append(" = value;\n                return true;\n");
                }
            }
            out.append("            default:\n                return super.deserialize").append(fam)
                    .append("(propertyKey, value);\n        }\n    }\n");
            if ("Bytes".equals(fam)) {
                continue;
            }
            out.append("\n    @Override\n    public boolean set").append(fam).append("Property(int propertyKey, ")
                    .append(jt).append(" value) {\n        switch (propertyKey) {\n");
            for (Prop p : ps) {
                out.append("            case ").append(snake(p.name)).append("_PROPERTY_KEY:\n                set")
                        .append(cap(p.name)).append("(value);\n                return true;\n");
            }
            out.append("            default:\n                return super.set").append(fam)
                    .append("Property(propertyKey, value);\n        }\n    }\n");
            out.append("\n    @Override\n    public ").append(jt).append(" get").append(fam)
                    .append("Property(int propertyKey) {\n        switch (propertyKey) {\n");
            for (Prop p : ps) {
                out.append("            case ").append(snake(p.name)).append("_PROPERTY_KEY:\n                return m")
                        .append(cap(p.name)).append(";\n");
            }
            out.append("            default:\n                return super.get").append(fam)
                    .append("Property(propertyKey);\n        }\n    }\n");
        }
        out.append("}\n");
        return out.toString();
    }

    static String placeholder(String name) {
        return HEADER + "package com.codename1.impl.rive;\n\n"
                + "/// Rive `" + name + "`, with no behaviour beyond its stored properties.\n"
                + "/// Generated by `" + GENERATOR + "`; add a hand-written `" + name + "`\n"
                + "/// to the runtime's sources to give it behaviour.\n"
                + "public class " + name + " extends " + name + "Base {\n}\n";
    }

    /// The generated runtime sources, file name to text.
    Map<String, String> runtimeFiles() throws IOException {
        Map<String, String> files = new LinkedHashMap<String, String>();
        for (String name : keep.keySet()) {
            files.put(name + "Base.java", baseClass(name));
            if (!new File(handWritten, name + ".java").isFile()) {
                files.put(name + ".java", placeholder(name));
            }
        }
        return files;
    }

    private List<Type> byTypeKey(Iterable<String> names, Map<String, Type> from) {
        List<Type> out = new ArrayList<Type>();
        for (String n : names) {
            out.add(from.get(n));
        }
        Collections.sort(out, new Comparator<Type>() {
            @Override
            public int compare(Type a, Type b) {
                return a.typeKey < b.typeKey ? -1 : a.typeKey == b.typeKey ? 0 : 1;
            }
        });
        return out;
    }

    private boolean isAsset(String name) {
        String n = name;
        while (n != null) {
            if ("FileAsset".equals(n) || "FileAssetContents".equals(n)) {
                return true;
            }
            Type t = keep.get(n);
            n = t == null ? null : t.parent;
        }
        return false;
    }

    String schemaClass() {
        StringBuilder out = new StringBuilder(HEADER);
        out.append("package com.codename1.rive.transcoder;\n\n");
        out.append("/// Generated by `").append(GENERATOR).append("`; do not edit. The runtime\n");
        out.append("/// class of every object type a `.riv` file can hold, and the wire type of\n");
        out.append("/// every property key. Build time only: nothing here ships in an app.\n");
        out.append("public final class RiveSchema {\n");
        out.append("    public static final int FIELD_UINT = 0;\n    public static final int FIELD_STRING = 1;\n");
        out.append("    public static final int FIELD_DOUBLE = 2;\n    public static final int FIELD_COLOR = 3;\n");
        out.append("    public static final int FIELD_BOOL = 4;\n    public static final int FIELD_BYTES = 5;\n");
        out.append("    public static final int FIELD_UNKNOWN = -1;\n\n");
        out.append("    private RiveSchema() {\n    }\n\n");
        out.append("    /// The simple name of the `com.codename1.impl.rive` class for `typeKey`,\n");
        out.append("    /// or null for a type the runtime skips.\n");
        out.append("    public static String className(int typeKey) {\n        switch (typeKey) {\n");
        for (Type t : byTypeKey(concrete, keep)) {
            out.append("            case ").append(t.typeKey).append(":\n                return \"").append(t.name)
                    .append("\";\n");
        }
        out.append("            default:\n                return null;\n        }\n    }\n\n");
        out.append("    /// The rive-runtime name of `typeKey`, for build notes, or null.\n");
        out.append("    public static String typeName(int typeKey) {\n        switch (typeKey) {\n");
        for (Type t : byTypeKey(schema.types.keySet(), schema.types)) {
            out.append("            case ").append(t.typeKey).append(":\n                return \"").append(t.name)
                    .append("\";\n");
        }
        out.append("            default:\n                return null;\n        }\n    }\n\n");
        out.append("    /// Whether `typeKey` is a file asset or an asset's contents, which belong\n");
        out.append("    /// to the whole file rather than to an artboard.\n");
        out.append("    public static boolean isAsset(int typeKey) {\n        switch (typeKey) {\n");
        for (Type t : byTypeKey(concrete, keep)) {
            if (isAsset(t.name)) {
                out.append("            case ").append(t.typeKey).append(":\n");
            }
        }
        out.append("                return true;\n            default:\n                return false;\n"
                + "        }\n    }\n\n");
        out.append("    /// The wire type of `propertyKey`, or FIELD_UNKNOWN.\n");
        out.append("    public static int propertyFieldId(int propertyKey) {\n        switch (propertyKey) {\n");
        // Every type's keys, skipped ones included: their properties still have
        // to be stepped over, and the header's table of contents does not list
        // keys the editor expects every runtime to know.
        Map<Integer, Set<Integer>> by = new TreeMap<Integer, Set<Integer>>();
        for (Type t : schema.types.values()) {
            for (Prop p : t.props) {
                if (p.coreType == null) {
                    continue;
                }
                Integer fid = "CoreBytes".equals(p.coreType) ? Integer.valueOf(5) : FIELD_ID.get(p.coreType);
                Set<Integer> keys = by.get(fid);
                if (keys == null) {
                    keys = new TreeSet<Integer>();
                    by.put(fid, keys);
                }
                keys.add(Integer.valueOf(p.key));
            }
        }
        for (Map.Entry<Integer, Set<Integer>> e : by.entrySet()) {
            for (Integer k : e.getValue()) {
                out.append("            case ").append(k).append(":\n");
            }
            out.append("                return ").append(e.getKey()).append(";\n");
        }
        out.append("            default:\n                return FIELD_UNKNOWN;\n        }\n    }\n}\n");
        return out.toString();
    }
}
