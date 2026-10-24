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

import com.codename1.build.Log;
import com.codename1.builders.BuildException;
import com.codename1.maven.RoomTypes.ClassSig;
import com.codename1.maven.RoomTypes.JType;
import com.codename1.maven.RoomTypes.MethodSig;
import com.codename1.maven.annotations.JavaSourceCompiler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// The build-time half of AndroidX Room on the compatibility runtime: what
/// Room's annotation processor does on Android, done from the compiled
/// classes. For every `@Database` class it writes `Name_Impl` (the schema,
/// the DAO getters, the converter instances) and for every DAO it returns
/// `Dao_Impl` (each `@Query`, `@Insert`, `@Update`, `@Delete`, `@Upsert`,
/// `@RawQuery` and `@Transaction` method as plain code), as Java source
/// compiled against the application's classes and the runtime jar. Nothing
/// it writes uses reflection.
///
/// It runs before the classes are relocated, so the source names the real
/// `androidx` and `android` types. Anything Room accepts that this does not
/// fails the build with the class and member named: a construct is never
/// skipped silently.
final class RoomGenerator {

    static final String DATABASE = "Landroidx/room/Database;";
    static final String DATABASE_RELOCATED = "L" + AndroidRemapper.TARGET + "androidx/room/Database;";
    private static final String DAO = "Landroidx/room/Dao;";
    private static final String ENTITY = "Landroidx/room/Entity;";
    private static final String PRIMARY_KEY = "Landroidx/room/PrimaryKey;";
    private static final String COLUMN_INFO = "Landroidx/room/ColumnInfo;";
    private static final String IGNORE = "Landroidx/room/Ignore;";
    private static final String EMBEDDED = "Landroidx/room/Embedded;";
    private static final String RELATION = "Landroidx/room/Relation;";
    private static final String QUERY = "Landroidx/room/Query;";
    private static final String RAW_QUERY = "Landroidx/room/RawQuery;";
    private static final String INSERT = "Landroidx/room/Insert;";
    private static final String UPDATE = "Landroidx/room/Update;";
    private static final String DELETE = "Landroidx/room/Delete;";
    private static final String UPSERT = "Landroidx/room/Upsert;";
    private static final String TRANSACTION = "Landroidx/room/Transaction;";
    private static final String TYPE_CONVERTER = "Landroidx/room/TypeConverter;";
    private static final String TYPE_CONVERTERS = "Landroidx/room/TypeConverters;";
    private static final String PROVIDED_CONVERTER = "Landroidx/room/ProvidedTypeConverter;";
    private static final String FTS3 = "Landroidx/room/Fts3;";
    private static final String FTS4 = "Landroidx/room/Fts4;";
    private static final String DATABASE_VIEW = "Landroidx/room/DatabaseView;";
    private static final String[] NOT_NULL = {"Landroidx/annotation/NonNull;", "Lorg/jetbrains/annotations/NotNull;",
        "Ljavax/annotation/Nonnull;", "Landroid/support/annotation/NonNull;"};
    private static final String RT = "com.codename1.androidcompat.runtime.RoomSupport";
    private static final String[] LIST_TYPES = {"java/util/List", "java/util/ArrayList", "java/util/Collection",
        "java/lang/Iterable"};
    private static final String[] SET_TYPES = {"java/util/Set", "java/util/HashSet", "java/util/LinkedHashSet"};
    /// Return types Room maps through an integration this runtime lacks.
    private static final String[][] REACTIVE = {
        {"kotlinx/coroutines/flow/Flow", "Kotlin Flow"},
        {"io/reactivex/Flowable", "RxJava"}, {"io/reactivex/Observable", "RxJava"},
        {"io/reactivex/Single", "RxJava"}, {"io/reactivex/Maybe", "RxJava"},
        {"io/reactivex/Completable", "RxJava"},
        {"io/reactivex/rxjava3/core/Flowable", "RxJava"}, {"io/reactivex/rxjava3/core/Observable", "RxJava"},
        {"io/reactivex/rxjava3/core/Single", "RxJava"}, {"io/reactivex/rxjava3/core/Maybe", "RxJava"},
        {"io/reactivex/rxjava3/core/Completable", "RxJava"},
        {"com/google/common/util/concurrent/ListenableFuture", "Guava ListenableFuture"},
        {"androidx/paging/PagingSource", "Paging"}, {"androidx/paging/DataSource$Factory", "Paging"},
        {"java/util/Optional", "java.util.Optional"}, {"com/google/common/base/Optional", "Guava Optional"},
        {"java/util/Map", "multimap"}, {"java/util/HashMap", "multimap"}, {"java/util/LinkedHashMap", "multimap"},
        {"java/util/TreeMap", "multimap"}, {"androidx/collection/LongSparseArray", "multimap"},
        {"androidx/collection/ArrayMap", "multimap"}};

    private final File classesDir;
    private final List<File> extraDirs;
    private final File compatJar;
    private final Log log;
    private final Map<String, ClassNode> app = new LinkedHashMap<String, ClassNode>();
    private final Map<String, ClassNode> lib = new HashMap<String, ClassNode>();
    private ZipFile compatZip;
    private int uid;

    RoomGenerator(File classesDir, List<File> extraDirs, File compatJar, Log log) {
        this.classesDir = classesDir;
        this.extraDirs = extraDirs;
        this.compatJar = compatJar;
        this.log = log;
    }

    /// A Room construct the build cannot translate, with where it is.
    static final class RoomError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RoomError(String message) {
            super(message);
        }
    }

    /// Writes and compiles the implementations; answers how many databases
    /// there were.
    int generate() throws BuildException {
        try {
            load(classesDir);
            for (File d : extraDirs) {
                load(d);
            }
            List<ClassNode> dbs = new ArrayList<ClassNode>();
            for (ClassNode n : app.values()) {
                if (ann(n.visibleAnnotations, n.invisibleAnnotations, DATABASE) != null) {
                    dbs.add(n);
                }
            }
            if (dbs.isEmpty()) {
                return 0;
            }
            Map<String, String> sources = new LinkedHashMap<String, String>();
            Set<String> daos = new HashSet<String>();
            for (ClassNode db : dbs) {
                new DbGen(db).generate(sources, daos);
            }
            List<File> cp = new ArrayList<File>();
            cp.add(classesDir);
            cp.addAll(extraDirs);
            if (compatJar != null) {
                cp.add(compatJar);
            }
            File core = coreJar();
            if (core != null) {
                cp.add(core);
            }
            try {
                JavaSourceCompiler.compile(sources, classesDir, cp);
            } catch (IOException e) {
                throw new BuildException("Room: the generated implementations did not compile; this is a bug in"
                        + " the Room build step, not in the application.\n" + e.getMessage(), e);
            }
            log.info("Room: generated " + dbs.size() + " database and " + daos.size() + " DAO implementation(s)");
            return dbs.size();
        } catch (RoomError e) {
            throw new BuildException("Room: " + e.getMessage());
        } catch (IOException e) {
            throw new BuildException("Room code generation failed: " + e.getMessage(), e);
        } finally {
            if (compatZip != null) {
                try {
                    compatZip.close();
                } catch (IOException ignored) {
                    // Read-only; nothing to lose.
                }
            }
        }
    }

    /// The Codename One core jar the build engine itself runs against: the
    /// runtime's classes refer to it.
    private static File coreJar() {
        java.security.CodeSource cs = com.codename1.db.Database.class.getProtectionDomain().getCodeSource();
        if (cs == null || cs.getLocation() == null) {
            return null;
        }
        try {
            return new File(cs.getLocation().toURI());
        } catch (java.net.URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    /// The generated implementation of a database or DAO class, by Room's
    /// naming: nested names joined with `_`, then `_Impl`.
    static String implName(String internal) {
        return internal.replace('$', '_') + "_Impl";
    }

    // ------------------------------------------------------------ loading

    private void load(File dir) throws IOException {
        List<File> files = new ArrayList<File>();
        collect(dir, files);
        for (File f : files) {
            ClassNode n = new ClassNode();
            new ClassReader(Files.readAllBytes(f.toPath())).accept(n, ClassReader.SKIP_FRAMES);
            if (n.name.startsWith(AndroidRemapper.TARGET)) {
                continue;
            }
            app.put(n.name, n);
        }
    }

    private static void collect(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, out);
            } else if (f.getName().endsWith(".class")) {
                out.add(f);
            }
        }
    }

    private ClassNode node(String internal) {
        ClassNode n = app.get(internal);
        if (n != null) {
            return n;
        }
        if (lib.containsKey(internal)) {
            return lib.get(internal);
        }
        n = null;
        try {
            if (compatJar != null && compatJar.isFile()) {
                if (compatZip == null) {
                    compatZip = new ZipFile(compatJar);
                }
                ZipEntry e = compatZip.getEntry(internal + ".class");
                if (e != null) {
                    InputStream in = compatZip.getInputStream(e);
                    try {
                        n = new ClassNode();
                        new ClassReader(in).accept(n, ClassReader.SKIP_CODE);
                    } finally {
                        in.close();
                    }
                }
            }
        } catch (IOException e) {
            n = null;
        }
        lib.put(internal, n);
        return n;
    }

    private boolean isEnum(String internal) {
        ClassNode n = node(internal);
        return n != null && (n.access & Opcodes.ACC_ENUM) != 0;
    }

    private boolean isSubclass(String cls, String base) {
        String c = cls;
        int guard = 0;
        while (c != null && guard++ < 64) {
            if (c.equals(base)) {
                return true;
            }
            ClassNode n = node(c);
            c = n == null ? null : n.superName;
        }
        return false;
    }

    // ------------------------------------------------------------ annotations

    static AnnotationNode ann(List<AnnotationNode> visible, List<AnnotationNode> invisible, String desc) {
        if (visible != null) {
            for (AnnotationNode a : visible) {
                if (a.desc.equals(desc)) {
                    return a;
                }
            }
        }
        if (invisible != null) {
            for (AnnotationNode a : invisible) {
                if (a.desc.equals(desc)) {
                    return a;
                }
            }
        }
        return null;
    }

    private static AnnotationNode ann(ClassNode n, String desc) {
        return ann(n.visibleAnnotations, n.invisibleAnnotations, desc);
    }

    private static AnnotationNode ann(FieldNode n, String desc) {
        return ann(n.visibleAnnotations, n.invisibleAnnotations, desc);
    }

    private static AnnotationNode ann(MethodNode n, String desc) {
        return ann(n.visibleAnnotations, n.invisibleAnnotations, desc);
    }

    private static Object val(AnnotationNode a, String name) {
        if (a == null || a.values == null) {
            return null;
        }
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            if (name.equals(a.values.get(i))) {
                return a.values.get(i + 1);
            }
        }
        return null;
    }

    private static String str(AnnotationNode a, String name, String def) {
        Object v = val(a, name);
        return v instanceof String ? (String) v : def;
    }

    private static int intv(AnnotationNode a, String name, int def) {
        Object v = val(a, name);
        return v instanceof Integer ? ((Integer) v).intValue() : def;
    }

    private static boolean bool(AnnotationNode a, String name, boolean def) {
        Object v = val(a, name);
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : def;
    }

    private static List<Object> list(AnnotationNode a, String name) {
        Object v = val(a, name);
        if (v instanceof List) {
            List<Object> out = new ArrayList<Object>();
            for (Object o : (List<?>) v) {
                out.add(o);
            }
            return out;
        }
        return Collections.emptyList();
    }

    private static List<String> types(AnnotationNode a, String name) {
        List<String> out = new ArrayList<String>();
        for (Object o : list(a, name)) {
            if (o instanceof Type) {
                out.add(((Type) o).getInternalName());
            }
        }
        return out;
    }

    private static String typeVal(AnnotationNode a, String name) {
        Object v = val(a, name);
        return v instanceof Type ? ((Type) v).getInternalName() : null;
    }

    // ------------------------------------------------------------ names

    static String pkg(String internal) {
        int i = internal.lastIndexOf('/');
        return i < 0 ? "" : internal.substring(0, i).replace('/', '.');
    }

    static String simple(String internal) {
        int i = Math.max(internal.lastIndexOf('/'), internal.lastIndexOf('$'));
        return internal.substring(i + 1);
    }

    static String fqn(String internal) {
        return internal.replace('/', '.');
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20 || c > 0x7e) {
                sb.append(String.format("\\u%04x", Integer.valueOf(c)));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    static String asciiLower(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return sb.toString();
    }

    private static String where(ClassNode n, MethodNode m) {
        return fqn(n.name) + "." + m.name + "()";
    }

    private static String sourceFile(ClassNode n) {
        return n.sourceFile == null ? "" : " (" + n.sourceFile + ")";
    }

    // ------------------------------------------------------------ converters

    /// A `@TypeConverter` method.
    static final class Conv {
        String owner;
        String name;
        boolean isStatic;
        JType from;
        JType to;
    }

    /// How one Java type is read from a column and bound to a statement.
    abstract static class Codec {
        abstract String read(String c, String i);

        abstract String bind(String v);

        abstract String affinity();
    }

    static boolean isNative(JType t) {
        if (t.isPrimitive()) {
            return !t.isVoid();
        }
        if (t.dims == 1 && t.kind == 'B') {
            return true;
        }
        return RoomTypes.unboxed(t) != 0 || t.isClass("java/lang/String");
    }

    /// Primitives, their boxes, `String` and `byte[]`.
    static final class NativeCodec extends Codec {
        final JType t;

        NativeCodec(JType t) {
            this.t = t;
        }

        static String readPrim(char k, String c, String i) {
            switch (k) {
                case 'Z': return "(" + c + ".getInt(" + i + ") != 0)";
                case 'B': return "((byte) " + c + ".getShort(" + i + "))";
                case 'C': return "((char) " + c + ".getInt(" + i + "))";
                case 'S': return c + ".getShort(" + i + ")";
                case 'I': return c + ".getInt(" + i + ")";
                case 'J': return c + ".getLong(" + i + ")";
                case 'F': return c + ".getFloat(" + i + ")";
                case 'D': return c + ".getDouble(" + i + ")";
                default: throw new IllegalArgumentException(String.valueOf(k));
            }
        }

        @Override
        String read(String c, String i) {
            if (t.isPrimitive()) {
                return readPrim(t.kind, c, i);
            }
            if (t.isArray()) {
                return "(" + c + ".isNull(" + i + ") ? null : " + c + ".getBlob(" + i + "))";
            }
            if (t.isClass("java/lang/String")) {
                return "(" + c + ".isNull(" + i + ") ? null : " + c + ".getString(" + i + "))";
            }
            char k = RoomTypes.unboxed(t);
            return "(" + c + ".isNull(" + i + ") ? null : " + RoomTypes.boxName(k) + ".valueOf("
                    + readPrim(k, c, i) + "))";
        }

        @Override
        String bind(String v) {
            if (t.isPrimitive()) {
                if (t.kind == 'Z') {
                    return RT + ".bindBoolean(" + v + ")";
                }
                if (t.kind == 'C') {
                    return "Integer.valueOf(" + v + ")";
                }
                return RoomTypes.boxName(t.kind) + ".valueOf(" + v + ")";
            }
            if (t.isClass("java/lang/Boolean")) {
                return RT + ".bindBoolean(" + v + ")";
            }
            if (t.isClass("java/lang/Character")) {
                return RT + ".bindChar(" + v + ")";
            }
            return v;
        }

        @Override
        String affinity() {
            if (t.isArray()) {
                return "BLOB";
            }
            if (t.isClass("java/lang/String")) {
                return "TEXT";
            }
            char k = t.isPrimitive() ? t.kind : RoomTypes.unboxed(t);
            return k == 'F' || k == 'D' ? "REAL" : "INTEGER";
        }
    }

    /// An enum, by constant name, Room's built-in converter.
    static final class EnumCodec extends Codec {
        final JType t;

        EnumCodec(JType t) {
            this.t = t;
        }

        @Override
        String read(String c, String i) {
            return RT + ".enumByName(" + t.rawSrc() + ".values(), " + c + ".isNull(" + i + ") ? null : " + c
                    + ".getString(" + i + "))";
        }

        @Override
        String bind(String v) {
            return RT + ".enumName(" + v + ")";
        }

        @Override
        String affinity() {
            return "TEXT";
        }
    }

    /// A type the application's converters turn into a column type.
    static final class ConvCodec extends Codec {
        final JType t;
        final Conv toColumn;
        final Conv fromColumn;

        ConvCodec(JType t, Conv toColumn, Conv fromColumn) {
            this.t = t;
            this.toColumn = toColumn;
            this.fromColumn = fromColumn;
        }

        static String call(Conv c, String arg) {
            String target = c.isStatic ? RoomTypes.sourceName(c.owner)
                    : "__db.getTypeConverter(" + RoomTypes.sourceName(c.owner) + ".class)";
            return target + "." + c.name + "(" + arg + ")";
        }

        @Override
        String read(String c, String i) {
            if (fromColumn == null) {
                throw new RoomError("Cannot figure out how to read " + t.src() + " from a cursor: no @TypeConverter"
                        + " converts a column value to it.");
            }
            String col = new NativeCodec(fromColumn.from).read(c, i);
            String conv = call(fromColumn, col);
            if (fromColumn.from.isPrimitive() && !t.isPrimitive()) {
                return "(" + c + ".isNull(" + i + ") ? null : " + conv + ")";
            }
            return conv;
        }

        @Override
        String bind(String v) {
            if (toColumn == null) {
                throw new RoomError("Cannot figure out how to save " + t.src() + " into the database: no"
                        + " @TypeConverter converts it to a column value.");
            }
            String bound = new NativeCodec(toColumn.to).bind(call(toColumn, v));
            if (toColumn.to.isPrimitive() && !t.isPrimitive()) {
                return "(" + v + " == null ? null : (Object) " + bound + ")";
            }
            return bound;
        }

        @Override
        String affinity() {
            JType col = toColumn != null ? toColumn.to : fromColumn.from;
            return new NativeCodec(col).affinity();
        }
    }

    // ------------------------------------------------------------ model

    /// A field of an entity or a query result class.
    static final class Prop {
        String field;
        JType type;
        String column;
        String owner;
        boolean embedded;
        String prefix = "";
        Pojo emb;
        String getter;
        String setter;
        boolean fieldReadable;
        boolean fieldWritable;
        Codec codec;
        AnnotationNode columnInfo;
        AnnotationNode primaryKey;
        boolean notNullAnnotated;
    }

    /// A class whose fields map to columns.
    static final class Pojo {
        String cls;
        JType type;
        final List<Prop> props = new ArrayList<Prop>();
        final List<Prop> ctorParams = new ArrayList<Prop>();
        int leafCount;
    }

    /// A column, through the embedded objects that lead to it.
    static final class Leaf {
        String column;
        Prop prop;
        int index;
    }

    /// An `@Entity` and its table.
    static final class Entity {
        String cls;
        String table;
        Pojo pojo;
        final List<Leaf> leaves = new ArrayList<Leaf>();
        final List<Leaf> pk = new ArrayList<Leaf>();
        Leaf autoGenerate;
        final List<String> ddl = new ArrayList<String>();
    }

    private final Map<String, Pojo> pojos = new HashMap<String, Pojo>();

    private Pojo pojo(String cls, String pkg, List<Conv> convs, Set<String> visiting) {
        String key = cls + "|" + pkg;
        Pojo cached = pojos.get(key);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(cls)) {
            throw new RoomError(fqn(cls) + ": recursive @Embedded");
        }
        ClassNode n = app.get(cls);
        if (n == null) {
            throw new RoomError(fqn(cls) + " is used as an entity or query result but is not an application class");
        }
        if ((n.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0) {
            throw new RoomError(fqn(cls) + sourceFile(n) + ": an entity or query result class cannot be abstract");
        }
        if (!accessible(n.access, cls, pkg)) {
            throw new RoomError(fqn(cls) + sourceFile(n) + " must be public to be used by a DAO in package " + pkg);
        }
        AnnotationNode entity = ann(n, ENTITY);
        Set<String> ignored = new HashSet<String>();
        for (Object o : list(entity, "ignoredColumns")) {
            ignored.add(String.valueOf(o));
        }
        List<ClassNode> chain = new ArrayList<ClassNode>();
        for (String c = cls; c != null; ) {
            ClassNode cn = app.get(c);
            if (cn == null) {
                break;
            }
            chain.add(0, cn);
            c = cn.superName;
        }
        Pojo p = new Pojo();
        p.cls = cls;
        p.type = JType.cls(cls);
        for (ClassNode cn : chain) {
            for (FieldNode f : cn.fields) {
                if ((f.access & (Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT | Opcodes.ACC_SYNTHETIC)) != 0
                        || ann(f, IGNORE) != null) {
                    continue;
                }
                if (ann(f, RELATION) != null) {
                    throw new RoomError(fqn(cn.name) + "." + f.name + sourceFile(cn)
                            + ": @Relation is not supported by the Codename One Room build; query the related"
                            + " rows with a second @Query");
                }
                Prop prop = new Prop();
                prop.field = f.name;
                prop.owner = cn.name;
                prop.columnInfo = ann(f, COLUMN_INFO);
                prop.primaryKey = ann(f, PRIMARY_KEY);
                for (String nn : NOT_NULL) {
                    if (ann(f, nn) != null) {
                        prop.notNullAnnotated = true;
                    }
                }
                String col = str(prop.columnInfo, "name", "[field-name]");
                prop.column = "[field-name]".equals(col) ? f.name : col;
                if (ignored.contains(prop.column)) {
                    continue;
                }
                prop.type = RoomTypes.parseField(f.signature != null ? f.signature : f.desc);
                if (prop.type.hasTypeVariables()) {
                    throw new RoomError(fqn(cn.name) + "." + f.name + sourceFile(cn)
                            + ": a field of a type variable cannot be mapped to a column");
                }
                AnnotationNode embedded = ann(f, EMBEDDED);
                if (embedded != null) {
                    if (prop.type.kind != 'L' || prop.type.isArray()) {
                        throw new RoomError(fqn(cn.name) + "." + f.name + ": @Embedded needs a class type");
                    }
                    prop.embedded = true;
                    prop.prefix = str(embedded, "prefix", "");
                    prop.emb = pojo(prop.type.name, pkg, convs, visiting);
                } else {
                    prop.codec = codec(prop.type, convs);
                    if (prop.codec == null) {
                        throw new RoomError(fqn(cn.name) + "." + f.name + sourceFile(cn)
                                + ": Cannot figure out how to save this field into database. You can consider"
                                + " adding a type converter for it. (type " + prop.type.src() + ")");
                    }
                }
                prop.fieldReadable = accessible(f.access, cn.name, pkg);
                prop.fieldWritable = prop.fieldReadable && (f.access & Opcodes.ACC_FINAL) == 0;
                findAccessors(chain, prop, f.desc, pkg);
                p.props.add(prop);
            }
        }
        chooseConstructor(n, p, pkg);
        for (Prop prop : p.props) {
            p.leafCount += prop.embedded ? prop.emb.leafCount : 1;
        }
        visiting.remove(cls);
        pojos.put(key, p);
        return p;
    }

    private static boolean accessible(int access, String owner, String pkg) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            return true;
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return false;
        }
        return pkg(owner).equals(pkg);
    }

    /// Room's accessor names: `getX`/`isX`/`x()` and `setX`/`x(v)`, for the
    /// field name and for it without an `m` or `_` prefix.
    private static void findAccessors(List<ClassNode> chain, Prop prop, String desc, String pkg) {
        List<String> names = new ArrayList<String>();
        names.add(prop.field);
        String f = prop.field;
        if (f.length() > 1 && f.charAt(0) == 'm' && Character.isUpperCase(f.charAt(1))) {
            names.add(Character.toLowerCase(f.charAt(1)) + f.substring(2));
        }
        if (f.length() > 1 && f.charAt(0) == '_') {
            names.add(f.substring(1));
        }
        List<String> getters = new ArrayList<String>();
        List<String> setters = new ArrayList<String>();
        for (String n : names) {
            getters.add("get" + cap(n));
            if (desc.equals("Z") || desc.equals("Ljava/lang/Boolean;")) {
                getters.add("is" + cap(n));
                getters.add("has" + cap(n));
            }
            getters.add(n);
            setters.add("set" + cap(n));
            if (n.length() > 2 && n.startsWith("is") && Character.isUpperCase(n.charAt(2))) {
                setters.add("set" + n.substring(2));
            }
            setters.add(n);
        }
        for (int i = chain.size() - 1; i >= 0; i--) {
            ClassNode cn = chain.get(i);
            for (MethodNode m : cn.methods) {
                if ((m.access & Opcodes.ACC_STATIC) != 0 || !accessible(m.access, cn.name, pkg)) {
                    continue;
                }
                if (prop.getter == null && getters.contains(m.name) && m.desc.equals("()" + desc)) {
                    prop.getter = m.name;
                }
                if (prop.setter == null && setters.contains(m.name) && m.desc.startsWith("(" + desc + ")")) {
                    prop.setter = m.name;
                }
            }
        }
    }

    private static List<String> paramNames(MethodNode m, int count) {
        if (m.parameters != null && m.parameters.size() == count) {
            List<String> out = new ArrayList<String>();
            for (ParameterNode p : m.parameters) {
                out.add(p.name);
            }
            return out;
        }
        if (m.localVariables == null) {
            return null;
        }
        Type[] args = Type.getArgumentTypes(m.desc);
        List<String> out = new ArrayList<String>();
        int slot = (m.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (Type a : args) {
            String name = null;
            for (LocalVariableNode lv : m.localVariables) {
                if (lv.index == slot) {
                    name = lv.name;
                    break;
                }
            }
            if (name == null) {
                return null;
            }
            out.add(name);
            slot += a.getSize();
        }
        return out;
    }

    private static boolean settable(Prop p) {
        return p.setter != null || p.fieldWritable;
    }

    /// A public no-argument constructor with setters, or the smallest
    /// constructor whose parameters name the fields nothing else can set.
    private static void chooseConstructor(ClassNode n, Pojo p, String pkg) {
        Set<Prop> need = new HashSet<Prop>();
        for (Prop prop : p.props) {
            if (!settable(prop)) {
                need.add(prop);
            }
        }
        List<Prop> best = null;
        for (MethodNode m : n.methods) {
            if (!m.name.equals("<init>") || (m.access & Opcodes.ACC_SYNTHETIC) != 0 || ann(m, IGNORE) != null
                    || !accessible(m.access, n.name, pkg)) {
                continue;
            }
            Type[] args = Type.getArgumentTypes(m.desc);
            List<Prop> params = new ArrayList<Prop>();
            if (args.length > 0) {
                List<String> names = paramNames(m, args.length);
                if (names == null) {
                    continue;
                }
                for (int i = 0; i < args.length; i++) {
                    Prop match = null;
                    for (Prop prop : p.props) {
                        String f = prop.field;
                        String bare = f.length() > 1 && f.charAt(0) == 'm' && Character.isUpperCase(f.charAt(1))
                                ? Character.toLowerCase(f.charAt(1)) + f.substring(2) : f;
                        if ((f.equals(names.get(i)) || bare.equals(names.get(i)))
                                && prop.type.desc().equals(args[i].getDescriptor())) {
                            match = prop;
                        }
                    }
                    if (match == null) {
                        params = null;
                        break;
                    }
                    params.add(match);
                }
            }
            if (params == null || !params.containsAll(need)) {
                continue;
            }
            if (best == null || params.size() < best.size()) {
                best = params;
            }
        }
        if (best == null) {
            throw new RoomError(fqn(n.name) + sourceFile(n) + ": Entities and POJOs must have a usable public"
                    + " constructor. You can have an empty constructor or a constructor whose parameters match the"
                    + " fields (by name and type)" + (need.isEmpty() ? "" : "; these fields have no setter: "
                    + names(need)));
        }
        p.ctorParams.addAll(best);
    }

    private static String names(Set<Prop> props) {
        List<String> out = new ArrayList<String>();
        for (Prop p : props) {
            out.add(p.field);
        }
        Collections.sort(out);
        return out.toString();
    }

    private static void flatten(Pojo p, String prefix, List<Leaf> out) {
        for (Prop prop : p.props) {
            if (prop.embedded) {
                flatten(prop.emb, prefix + prop.prefix, out);
            } else {
                Leaf l = new Leaf();
                l.column = prefix + prop.column;
                l.prop = prop;
                l.index = out.size();
                out.add(l);
            }
        }
    }

    private Codec codec(JType t, List<Conv> convs) {
        if (isNative(t)) {
            return new NativeCodec(t);
        }
        Conv to = null;
        Conv from = null;
        for (Conv c : convs) {
            if (c.from.equals(t) && isNative(c.to) && (to == null || (from != null && c.to.equals(from.from)))) {
                to = c;
            }
            if (c.to.equals(t) && isNative(c.from) && (from == null || (to != null && c.from.equals(to.to)))) {
                from = c;
            }
        }
        if (to != null || from != null) {
            return new ConvCodec(t, to, from);
        }
        if (t.kind == 'L' && t.dims == 0 && isEnum(t.name)) {
            return new EnumCodec(t);
        }
        return null;
    }

    private String tableName(String cls) {
        ClassNode n = app.get(cls);
        AnnotationNode e = n == null ? null : ann(n, ENTITY);
        if (e == null) {
            throw new RoomError(fqn(cls) + " is not an @Entity");
        }
        String t = str(e, "tableName", "");
        return t.isEmpty() ? simple(cls) : t;
    }

    private Entity entity(String cls, List<Conv> convs) {
        ClassNode n = app.get(cls);
        if (n == null || ann(n, ENTITY) == null) {
            throw new RoomError(fqn(cls) + " is listed in @Database(entities) but is not an @Entity class");
        }
        if (ann(n, FTS3) != null || ann(n, FTS4) != null) {
            throw new RoomError(fqn(cls) + sourceFile(n) + ": FTS entities are not supported by the Codename One"
                    + " Room build");
        }
        AnnotationNode ea = ann(n, ENTITY);
        Entity e = new Entity();
        e.cls = cls;
        e.table = tableName(cls);
        e.pojo = pojo(cls, pkg(cls), convs, new HashSet<String>());
        flatten(e.pojo, "", e.leaves);
        List<Object> declared = list(ea, "primaryKeys");
        for (Leaf l : e.leaves) {
            if (l.prop.primaryKey != null) {
                e.pk.add(l);
                if (bool(l.prop.primaryKey, "autoGenerate", false)) {
                    e.autoGenerate = l;
                }
            }
        }
        // @PrimaryKey on an @Embedded field: every column of it.
        collectEmbeddedPk(e.pojo, e.leaves, new int[1], e.pk);
        if (!declared.isEmpty()) {
            if (!e.pk.isEmpty()) {
                throw new RoomError(fqn(cls) + ": You cannot have multiple primary keys defined in an Entity");
            }
            for (Object o : declared) {
                Leaf l = leaf(e, String.valueOf(o));
                if (l == null) {
                    throw new RoomError(fqn(cls) + ": primary key column " + o + " is not a column of the entity");
                }
                e.pk.add(l);
            }
        }
        if (e.pk.isEmpty()) {
            throw new RoomError(fqn(cls) + sourceFile(n) + ": An entity must have at least 1 field annotated with"
                    + " @PrimaryKey");
        }
        if (e.autoGenerate != null) {
            JType t = e.autoGenerate.prop.type;
            char k = t.isPrimitive() ? t.kind : RoomTypes.unboxed(t);
            if (e.pk.size() != 1 || !(k == 'I' || k == 'J')) {
                throw new RoomError(fqn(cls) + "." + e.autoGenerate.prop.field + ": If a primary key is annotated"
                        + " with autoGenerate, its type must be int, Integer, long or Long and it must be the only"
                        + " primary key");
            }
        }
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + e.table + "` (");
        List<String> indexed = new ArrayList<String>();
        for (int i = 0; i < e.leaves.size(); i++) {
            Leaf l = e.leaves.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('`').append(l.column).append("` ").append(affinity(l));
            if (l == e.autoGenerate) {
                sql.append(" PRIMARY KEY AUTOINCREMENT");
            }
            if (l.prop.type.isPrimitive() || l.prop.notNullAnnotated || e.pk.contains(l)) {
                sql.append(" NOT NULL");
            }
            int collate = intv(l.prop.columnInfo, "collate", 1);
            String[] collations = {null, null, "BINARY", "NOCASE", "RTRIM", "LOCALIZED", "UNICODE"};
            if (collate > 1 && collate < collations.length) {
                sql.append(" COLLATE ").append(collations[collate]);
            }
            String def = str(l.prop.columnInfo, "defaultValue", "[value-unspecified]");
            if (!"[value-unspecified]".equals(def)) {
                sql.append(" DEFAULT ").append(def);
            }
            if (bool(l.prop.columnInfo, "index", false)) {
                indexed.add(l.column);
            }
        }
        if (e.autoGenerate == null) {
            sql.append(", PRIMARY KEY(");
            for (int i = 0; i < e.pk.size(); i++) {
                sql.append(i > 0 ? ", " : "").append('`').append(e.pk.get(i).column).append('`');
            }
            sql.append(')');
        }
        String[] actions = {null, "NO ACTION", "RESTRICT", "SET NULL", "SET DEFAULT", "CASCADE"};
        for (Object o : list(ea, "foreignKeys")) {
            AnnotationNode fk = (AnnotationNode) o;
            String parent = tableName(typeVal(fk, "entity"));
            sql.append(", FOREIGN KEY(").append(columns(list(fk, "childColumns"))).append(") REFERENCES `")
                    .append(parent).append("`(").append(columns(list(fk, "parentColumns"))).append(") ON UPDATE ")
                    .append(actions[clamp(intv(fk, "onUpdate", 1))]).append(" ON DELETE ")
                    .append(actions[clamp(intv(fk, "onDelete", 1))]);
            if (bool(fk, "deferred", false)) {
                sql.append(" DEFERRABLE INITIALLY DEFERRED");
            }
        }
        sql.append(')');
        e.ddl.add(sql.toString());
        for (Object o : list(ea, "indices")) {
            AnnotationNode ix = (AnnotationNode) o;
            List<Object> cols = list(ix, "value");
            String name = str(ix, "name", "");
            if (name.isEmpty()) {
                StringBuilder nm = new StringBuilder("index_" + e.table);
                for (Object c : cols) {
                    nm.append('_').append(c);
                }
                name = nm.toString();
            }
            e.ddl.add("CREATE " + (bool(ix, "unique", false) ? "UNIQUE " : "") + "INDEX IF NOT EXISTS `" + name
                    + "` ON `" + e.table + "` (" + columns(cols) + ")");
        }
        for (String c : indexed) {
            e.ddl.add("CREATE INDEX IF NOT EXISTS `index_" + e.table + "_" + c + "` ON `" + e.table + "` (`" + c
                    + "`)");
        }
        return e;
    }

    private static void collectEmbeddedPk(Pojo p, List<Leaf> leaves, int[] next, List<Leaf> pk) {
        for (Prop prop : p.props) {
            if (prop.embedded) {
                int start = next[0];
                collectEmbeddedPk(prop.emb, leaves, next, pk);
                if (prop.primaryKey != null) {
                    for (int i = start; i < start + prop.emb.leafCount; i++) {
                        pk.add(leaves.get(i));
                    }
                }
            } else {
                next[0]++;
            }
        }
    }

    private static int clamp(int action) {
        return action < 1 || action > 5 ? 1 : action;
    }

    private static String columns(List<Object> cols) {
        StringBuilder sb = new StringBuilder();
        for (Object c : cols) {
            sb.append(sb.length() > 0 ? ", " : "").append('`').append(c).append('`');
        }
        return sb.toString();
    }

    private static String affinity(Leaf l) {
        int a = intv(l.prop.columnInfo, "typeAffinity", 1);
        switch (a) {
            case 2: return "TEXT";
            case 3: return "INTEGER";
            case 4: return "REAL";
            case 5: return "BLOB";
            default: return l.prop.codec.affinity();
        }
    }

    private static Leaf leaf(Entity e, String column) {
        for (Leaf l : e.leaves) {
            if (l.column.equals(column)) {
                return l;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ database

    /// One `@Database` class and what it generates.
    private final class DbGen {
        final ClassNode db;
        final AnnotationNode da;
        final List<Conv> convs = new ArrayList<Conv>();
        final List<String> converterClasses = new ArrayList<String>();
        final Map<String, Entity> entities = new LinkedHashMap<String, Entity>();

        DbGen(ClassNode db) {
            this.db = db;
            this.da = ann(db, DATABASE);
        }

        void generate(Map<String, String> sources, Set<String> daosDone) {
            if (!isSubclass(db.name, "androidx/room/RoomDatabase")) {
                throw new RoomError(fqn(db.name) + sourceFile(db) + ": a @Database class must extend RoomDatabase");
            }
            if ((db.access & Opcodes.ACC_ABSTRACT) == 0) {
                throw new RoomError(fqn(db.name) + sourceFile(db) + ": a @Database class must be abstract");
            }
            if (!list(da, "autoMigrations").isEmpty()) {
                throw new RoomError(fqn(db.name) + sourceFile(db) + ": @AutoMigration is not supported by the"
                        + " Codename One Room build; write the Migration and pass it to addMigrations");
            }
            if (!list(da, "views").isEmpty()) {
                throw new RoomError(fqn(db.name) + sourceFile(db) + ": @DatabaseView is not supported by the"
                        + " Codename One Room build");
            }
            List<String> entityClasses = types(da, "entities");
            if (entityClasses.isEmpty()) {
                throw new RoomError(fqn(db.name) + ": @Database must list at least one entity");
            }
            addConverters(ann(db, TYPE_CONVERTERS));
            for (String ec : entityClasses) {
                ClassNode en = app.get(ec);
                if (en != null) {
                    addConverters(ann(en, TYPE_CONVERTERS));
                    if (ann(en, DATABASE_VIEW) != null) {
                        throw new RoomError(fqn(ec) + ": @DatabaseView is not supported by the Codename One Room"
                                + " build");
                    }
                }
            }
            List<MethodNode> daoGetters = new ArrayList<MethodNode>();
            List<String> daoTypes = new ArrayList<String>();
            for (String c = db.name; c != null && !c.equals("androidx/room/RoomDatabase"); ) {
                ClassNode cn = app.get(c);
                if (cn == null) {
                    break;
                }
                for (MethodNode m : cn.methods) {
                    if ((m.access & Opcodes.ACC_ABSTRACT) == 0) {
                        continue;
                    }
                    Type ret = Type.getReturnType(m.desc);
                    ClassNode dn = ret.getSort() == Type.OBJECT ? app.get(ret.getInternalName()) : null;
                    if (Type.getArgumentTypes(m.desc).length != 0 || dn == null || ann(dn, DAO) == null) {
                        throw new RoomError(where(cn, m) + sourceFile(cn) + ": an abstract method of a @Database"
                                + " class must take no parameters and return a @Dao type");
                    }
                    boolean dup = false;
                    for (MethodNode seen : daoGetters) {
                        dup |= seen.name.equals(m.name) && seen.desc.equals(m.desc);
                    }
                    if (!dup) {
                        daoGetters.add(m);
                        daoTypes.add(dn.name);
                        addConverters(ann(dn, TYPE_CONVERTERS));
                    }
                }
                c = cn.superName;
            }
            for (String ec : entityClasses) {
                entities.put(ec, entity(ec, convs));
            }
            for (String dt : daoTypes) {
                if (daosDone.add(dt)) {
                    String src = new DaoGen(this, app.get(dt)).generate();
                    sources.put(fqn(implName(dt)), src);
                }
            }
            sources.put(fqn(implName(db.name)), source(daoGetters, daoTypes));
        }

        private void addConverters(AnnotationNode tc) {
            for (String cls : types(tc, "value")) {
                if (converterClasses.contains(cls)) {
                    continue;
                }
                converterClasses.add(cls);
                ClassNode cn = app.get(cls);
                if (cn == null) {
                    throw new RoomError("@TypeConverters names " + fqn(cls) + ", which is not an application class");
                }
                for (MethodNode m : cn.methods) {
                    if (ann(m, TYPE_CONVERTER) == null) {
                        continue;
                    }
                    MethodSig s = RoomTypes.parseMethod(m.signature != null ? m.signature : m.desc);
                    if (s.params.size() != 1 || s.ret.isVoid() || (m.access & Opcodes.ACC_PUBLIC) == 0) {
                        throw new RoomError(where(cn, m) + sourceFile(cn) + ": a @TypeConverter must be public, take"
                                + " one parameter and return a value");
                    }
                    Conv c = new Conv();
                    c.owner = cls;
                    c.name = m.name;
                    c.isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
                    c.from = s.params.get(0);
                    c.to = s.ret;
                    convs.add(c);
                }
            }
        }

        String tableOf(String cls) {
            Entity e = entities.get(cls);
            return e == null ? null : e.table;
        }

        private String source(List<MethodNode> getters, List<String> daoTypes) {
            StringBuilder s = new StringBuilder();
            String pkg = pkg(db.name);
            if (!pkg.isEmpty()) {
                s.append("package ").append(pkg).append(";\n\n");
            }
            String impl = simple(implName(db.name));
            s.append("/** Generated by the Codename One Room build step. */\n");
            s.append("@SuppressWarnings({\"unchecked\", \"rawtypes\", \"deprecation\"})\n");
            s.append("public final class ").append(impl).append(" extends ").append(RoomTypes.sourceName(db.name))
                    .append(" {\n");
            for (int i = 0; i < getters.size(); i++) {
                s.append("    private ").append(RoomTypes.sourceName(daoTypes.get(i))).append(" _dao").append(i)
                        .append(";\n");
            }
            s.append("\n    public ").append(impl).append("() {\n        super();\n    }\n\n");
            s.append("    @Override\n    protected int roomSchemaVersion() {\n        return ")
                    .append(intv(da, "version", 1)).append(";\n    }\n\n");
            s.append("    @Override\n    protected String[] roomCreateStatements() {\n        return new String[] {\n");
            List<String> tables = new ArrayList<String>();
            for (Entity e : entities.values()) {
                tables.add(e.table);
                for (String d : e.ddl) {
                    s.append("            ").append(quote(d)).append(",\n");
                }
            }
            s.append("        };\n    }\n\n");
            s.append("    @Override\n    protected String[] roomTableNames() {\n        return new String[] {");
            for (String t : tables) {
                s.append(quote(t)).append(", ");
            }
            s.append("};\n    }\n\n");
            s.append("    @Override\n    protected Object roomCreateTypeConverter(Class<?> c) {\n");
            for (String cls : converterClasses) {
                ClassNode cn = app.get(cls);
                boolean instance = false;
                for (Conv c : convs) {
                    instance |= c.owner.equals(cls) && !c.isStatic;
                }
                if (!instance || ann(cn, PROVIDED_CONVERTER) != null) {
                    continue;
                }
                boolean ctor = false;
                for (MethodNode m : cn.methods) {
                    ctor |= m.name.equals("<init>") && m.desc.equals("()V") && (m.access & Opcodes.ACC_PUBLIC) != 0;
                }
                if (!ctor) {
                    throw new RoomError(fqn(cls) + ": a type converter class with instance methods needs a public"
                            + " no-argument constructor, or @ProvidedTypeConverter and Builder.addTypeConverter");
                }
                String n = RoomTypes.sourceName(cls);
                s.append("        if (c == ").append(n).append(".class) {\n            return new ").append(n)
                        .append("();\n        }\n");
            }
            s.append("        return null;\n    }\n");
            for (int i = 0; i < getters.size(); i++) {
                MethodNode m = getters.get(i);
                String t = RoomTypes.sourceName(daoTypes.get(i));
                s.append("\n    @Override\n    public ").append(t).append(' ').append(m.name).append("() {\n")
                        .append("        if (_dao").append(i).append(" == null) {\n            _dao").append(i)
                        .append(" = new ").append(fqn(implName(daoTypes.get(i)))).append("(this);\n        }\n")
                        .append("        return _dao").append(i).append(";\n    }\n");
            }
            s.append("}\n");
            return s.toString();
        }
    }

    // ------------------------------------------------------------ DAO

    /// A DAO method as the DAO type sees it, type variables resolved.
    static final class DaoMethod {
        ClassNode owner;
        MethodNode m;
        MethodSig sig;
        boolean isAbstract;
    }

    private void collectMethods(String cls, Map<String, JType> binding, Map<String, DaoMethod> out,
                                Set<String> concrete) {
        ClassNode n = app.get(cls);
        if (n == null) {
            return;
        }
        for (MethodNode m : n.methods) {
            if (m.name.startsWith("<") || (m.access & (Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC
                    | Opcodes.ACC_BRIDGE | Opcodes.ACC_PRIVATE)) != 0) {
                continue;
            }
            MethodSig s = RoomTypes.parseMethod(m.signature != null ? m.signature : m.desc);
            MethodSig r = new MethodSig();
            r.typeParams.addAll(s.typeParams);
            StringBuilder key = new StringBuilder(m.name).append('(');
            for (JType p : s.params) {
                JType q = p.subst(binding);
                r.params.add(q);
                key.append(q.desc());
            }
            key.append(')');
            r.ret = s.ret.subst(binding);
            for (JType x : s.exceptions) {
                r.exceptions.add(x.subst(binding));
            }
            if (r.exceptions.isEmpty() && m.exceptions != null) {
                for (String x : m.exceptions) {
                    r.exceptions.add(JType.cls(x));
                }
            }
            String k = key.toString();
            if (concrete.contains(k) || out.containsKey(k)) {
                continue;
            }
            boolean abs = (m.access & Opcodes.ACC_ABSTRACT) != 0;
            DaoMethod dm = new DaoMethod();
            dm.owner = n;
            dm.m = m;
            dm.sig = r;
            dm.isAbstract = abs;
            if (!abs) {
                concrete.add(k);
                if (ann(m, TRANSACTION) == null) {
                    continue;
                }
            }
            out.put(k, dm);
        }
        List<JType> supers = new ArrayList<JType>();
        if (n.signature != null) {
            ClassSig cs = RoomTypes.parseClass(n.signature);
            supers.add(cs.superclass);
            supers.addAll(cs.interfaces);
        } else {
            if (n.superName != null) {
                supers.add(JType.cls(n.superName));
            }
            for (String i : n.interfaces) {
                supers.add(JType.cls(i));
            }
        }
        for (JType st : supers) {
            JType bound = st.subst(binding);
            ClassNode sn = app.get(bound.name);
            if (sn == null) {
                continue;
            }
            Map<String, JType> b2 = new HashMap<String, JType>();
            if (sn.signature != null) {
                List<String> formal = RoomTypes.parseClass(sn.signature).typeParams;
                for (int i = 0; i < formal.size() && i < bound.args.size(); i++) {
                    b2.put(formal.get(i), bound.arg(i));
                }
            }
            collectMethods(bound.name, b2, out, concrete);
        }
    }

    /// One DAO and its implementation.
    private final class DaoGen {
        final DbGen db;
        final ClassNode dao;
        final String pkg;
        final StringBuilder helpers = new StringBuilder();
        final Map<String, String> readers = new HashMap<String, String>();
        final Map<String, String> binders = new HashMap<String, String>();
        int methodCount;

        DaoGen(DbGen db, ClassNode dao) {
            this.db = db;
            this.dao = dao;
            this.pkg = pkg(dao.name);
        }

        String generate() {
            boolean iface = (dao.access & Opcodes.ACC_INTERFACE) != 0;
            if (!iface && (dao.access & Opcodes.ACC_ABSTRACT) == 0) {
                throw new RoomError(fqn(dao.name) + sourceFile(dao) + ": a @Dao must be an interface or an abstract"
                        + " class");
            }
            if (dao.signature != null && !RoomTypes.parseClass(dao.signature).typeParams.isEmpty()) {
                throw new RoomError(fqn(dao.name) + sourceFile(dao) + ": a @Dao used by a database cannot be"
                        + " generic; declare an interface that binds its type arguments");
            }
            Map<String, DaoMethod> methods = new LinkedHashMap<String, DaoMethod>();
            collectMethods(dao.name, new HashMap<String, JType>(), methods, new HashSet<String>());
            StringBuilder s = new StringBuilder();
            if (!pkg.isEmpty()) {
                s.append("package ").append(pkg).append(";\n\n");
            }
            String impl = simple(implName(dao.name));
            String daoSrc = RoomTypes.sourceName(dao.name);
            s.append("/** Generated by the Codename One Room build step. */\n");
            s.append("@SuppressWarnings({\"unchecked\", \"rawtypes\", \"deprecation\"})\n");
            s.append("public final class ").append(impl).append(iface ? " implements " : " extends ").append(daoSrc)
                    .append(" {\n");
            s.append("    private final androidx.room.RoomDatabase __db;\n\n");
            s.append("    public ").append(impl).append("(androidx.room.RoomDatabase __db) {\n");
            if (!iface) {
                s.append("        ").append(superCall()).append('\n');
            }
            s.append("        this.__db = __db;\n    }\n");
            for (DaoMethod dm : methods.values()) {
                s.append(method(dm, daoSrc, iface));
            }
            s.append(helpers);
            s.append("}\n");
            return s.toString();
        }

        private String superCall() {
            String noArg = null;
            String withDb = null;
            for (MethodNode m : dao.methods) {
                if (!m.name.equals("<init>") || (m.access & Opcodes.ACC_PRIVATE) != 0) {
                    continue;
                }
                Type[] a = Type.getArgumentTypes(m.desc);
                if (a.length == 0) {
                    noArg = "super();";
                } else if (a.length == 1 && a[0].getSort() == Type.OBJECT
                        && isSubclassOrSuper(a[0].getInternalName())) {
                    withDb = "super((" + RoomTypes.sourceName(a[0].getInternalName()) + ") __db);";
                }
            }
            if (withDb != null) {
                return withDb;
            }
            if (noArg != null) {
                return noArg;
            }
            throw new RoomError(fqn(dao.name) + sourceFile(dao) + ": an abstract @Dao class needs a constructor that"
                    + " takes no arguments or the database");
        }

        private boolean isSubclassOrSuper(String type) {
            return isSubclass(db.db.name, type);
        }

        private String method(DaoMethod dm, String daoSrc, boolean iface) {
            MethodNode m = dm.m;
            String w = where(dm.owner, m) + sourceFile(dm.owner);
            if (!dm.sig.typeParams.isEmpty()) {
                throw new RoomError(w + ": generic DAO methods are not supported");
            }
            List<JType> params = dm.sig.params;
            if (!params.isEmpty() && params.get(params.size() - 1).isClass("kotlin/coroutines/Continuation")) {
                throw new RoomError(w + ": Kotlin suspend DAO functions are not supported by the Codename One Room"
                        + " build; declare a blocking function and call it from a background thread");
            }
            for (String[] r : REACTIVE) {
                JType ret = dm.sig.ret;
                if (ret.isClass(r[0]) || (ret.isClass("androidx/lifecycle/LiveData") && !ret.args.isEmpty()
                        && ret.arg(0).isClass(r[0]))) {
                    throw new RoomError(w + ": " + r[1] + " return types are not supported by the Codename One"
                            + " Room build; return the value, a List or LiveData");
                }
            }
            String[] kinds = {QUERY, RAW_QUERY, INSERT, UPDATE, DELETE, UPSERT};
            AnnotationNode found = null;
            for (String k : kinds) {
                AnnotationNode a = ann(m, k);
                if (a != null) {
                    if (found != null) {
                        throw new RoomError(w + ": a DAO method can have only one of @Query, @RawQuery, @Insert,"
                                + " @Update, @Delete and @Upsert");
                    }
                    found = a;
                }
            }
            boolean tx = ann(m, TRANSACTION) != null;
            if (found == null && dm.isAbstract) {
                throw new RoomError(w + ": an abstract DAO method must be annotated with @Query, @RawQuery,"
                        + " @Insert, @Update, @Delete or @Upsert (a Kotlin interface function with a body compiles"
                        + " to an abstract method; use an abstract class)");
            }
            StringBuilder decl = new StringBuilder();
            StringBuilder args = new StringBuilder();
            StringBuilder formals = new StringBuilder();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    args.append(", ");
                    formals.append(", ");
                }
                formals.append("final ").append(params.get(i).src()).append(" _p").append(i);
                args.append("_p").append(i);
            }
            String throwsClause = "";
            if (!dm.sig.exceptions.isEmpty()) {
                StringBuilder t = new StringBuilder(" throws ");
                for (int i = 0; i < dm.sig.exceptions.size(); i++) {
                    t.append(i > 0 ? ", " : "").append(dm.sig.exceptions.get(i).src());
                }
                throwsClause = t.toString();
            }
            JType ret = dm.sig.ret;
            String retSrc = ret.src();
            boolean isVoid = ret.isVoid();
            decl.append("\n    @Override\n    public ").append(retSrc).append(' ').append(m.name).append('(')
                    .append(formals).append(')').append(throwsClause).append(" {\n");
            boolean liveData = ret.isClass("androidx/lifecycle/LiveData");
            String body;
            if (found == null) {
                // A concrete @Transaction method: the application's body,
                // in a transaction.
                String target = iface ? daoSrc + ".super." : "super.";
                body = "        " + (isVoid ? "" : "return ") + target + m.name + "(" + args + ");\n";
            } else if (found.desc.equals(QUERY)) {
                body = query(dm, w, str(found, "value", ""), tx && liveData);
            } else if (found.desc.equals(RAW_QUERY)) {
                body = rawQuery(dm, w, found, tx && liveData);
            } else {
                body = write(dm, w, found);
            }
            if (!tx || liveData) {
                return decl + body + "    }\n";
            }
            int n = methodCount++;
            String helper = "__tx" + n;
            helpers.append("\n    private ").append(retSrc).append(' ').append(helper).append('(').append(formals)
                    .append(')').append(throwsClause).append(" {\n").append(body).append("    }\n");
            StringBuilder s = new StringBuilder(decl);
            s.append("        __db.beginTransaction();\n        try {\n");
            if (isVoid) {
                s.append("            ").append(helper).append('(').append(args).append(");\n")
                        .append("            __db.setTransactionSuccessful();\n");
            } else {
                s.append("            ").append(retSrc).append(" _r = ").append(helper).append('(').append(args)
                        .append(");\n            __db.setTransactionSuccessful();\n            return _r;\n");
            }
            s.append("        } finally {\n            __db.endTransaction();\n        }\n    }\n");
            return s.toString();
        }

        // ---------------------------------------------------- @Query

        private String query(DaoMethod dm, String w, String sql, boolean liveTx) {
            List<Object> parts = parseSql(sql, w);
            List<String> names = paramNames(dm.m, dm.sig.params.size());
            Map<String, Integer> byName = new HashMap<String, Integer>();
            List<String> distinct = new ArrayList<String>();
            int positional = 0;
            for (Object p : parts) {
                if (p instanceof String[]) {
                    String n = ((String[]) p)[0];
                    if (n == null) {
                        positional++;
                    } else if (!distinct.contains(n)) {
                        distinct.add(n);
                    }
                }
            }
            if (positional > 0 && !distinct.isEmpty()) {
                throw new RoomError(w + ": a query cannot mix ? and :name parameters");
            }
            if (names != null) {
                for (int i = 0; i < names.size(); i++) {
                    byName.put(names.get(i), Integer.valueOf(i));
                }
            } else if (!distinct.isEmpty()) {
                if (distinct.size() == dm.sig.params.size()) {
                    log.warn("Room: " + w + " was compiled without parameter names; binding " + distinct
                            + " to its parameters in order. Compile with -parameters (Kotlin: -java-parameters)"
                            + " so the names are kept.");
                    for (int i = 0; i < distinct.size(); i++) {
                        byName.put(distinct.get(i), Integer.valueOf(i));
                    }
                } else {
                    throw new RoomError(w + " was compiled without parameter names, so :" + distinct.get(0)
                            + " cannot be bound. Compile with -parameters (Kotlin: -java-parameters)");
                }
            }
            StringBuilder build = new StringBuilder();
            build.append("StringBuilder _sql = new StringBuilder();\n");
            build.append("java.util.ArrayList<Object> _args = new java.util.ArrayList<Object>();\n");
            int nextPositional = 0;
            for (Object p : parts) {
                if (p instanceof String) {
                    build.append("_sql.append(").append(quote((String) p)).append(");\n");
                    continue;
                }
                String n = ((String[]) p)[0];
                int idx;
                if (n == null) {
                    idx = nextPositional++;
                    if (idx >= dm.sig.params.size()) {
                        throw new RoomError(w + ": the query has more ? placeholders than the method has parameters");
                    }
                } else {
                    Integer i = byName.get(n);
                    if (i == null) {
                        throw new RoomError(w + ": Cannot find method parameters for :" + n);
                    }
                    idx = i.intValue();
                }
                build.append(bindParam(dm.sig.params.get(idx), "_p" + idx, w));
            }
            String kind = firstWord(sql);
            Set<String> tables = tables(sql);
            JType ret = dm.sig.ret;
            if (kind.equals("INSERT") || kind.equals("UPDATE") || kind.equals("DELETE") || kind.equals("REPLACE")) {
                return writeQuery(w, kind, ret, build.toString(), tables);
            }
            return read(w, ret, build.toString(), "__sqlCursor", tables, liveTx);
        }

        private String bindParam(JType t, String v, String w) {
            boolean array = t.isArray() && !(t.dims == 1 && t.kind == 'B');
            boolean collection = false;
            for (String l : LIST_TYPES) {
                collection |= t.isClass(l);
            }
            for (String l : SET_TYPES) {
                collection |= t.isClass(l);
            }
            if (array || collection) {
                JType e = array ? t.element() : t.args.isEmpty() ? JType.cls("java/lang/Object") : t.arg(0);
                if (e.isPrimitive()) {
                    if ("JISDF".indexOf(e.kind) < 0) {
                        throw new RoomError(w + ": a " + t.src() + " parameter cannot be expanded; use a List");
                    }
                    return RT + ".appendIn(_sql, _args, " + v + ");\n";
                }
                Codec c = codec(e, db.convs);
                if (c == null) {
                    throw new RoomError(w + ": Query method parameters should either be a type that can be"
                            + " converted into a database column or a List / Array that contains such type ("
                            + t.src() + ")");
                }
                String n = "_n" + (uid++);
                String x = "_x" + (uid++);
                return "int " + n + " = 0;\nif (" + v + " != null) {\n    for (" + e.src() + " " + x + " : " + v
                        + ") {\n        _sql.append(" + n + "++ == 0 ? \"?\" : \",?\");\n        _args.add("
                        + c.bind(x) + ");\n    }\n}\n";
            }
            Codec c = codec(t, db.convs);
            if (c == null) {
                throw new RoomError(w + ": Query method parameters should either be a type that can be converted"
                        + " into a database column or a List / Array that contains such type (" + t.src() + ")");
            }
            return "_sql.append(\"?\");\n_args.add(" + c.bind(v) + ");\n";
        }

        private String writeQuery(String w, String kind, JType ret, String build, Set<String> tables) {
            char k = ret.isPrimitive() ? ret.kind : RoomTypes.unboxed(ret);
            boolean insertId = kind.equals("INSERT") || kind.equals("REPLACE");
            StringBuilder s = new StringBuilder();
            s.append(indent(build, "        "));
            s.append("        __db.beginTransaction();\n        try {\n");
            String run;
            if (insertId && (k == 'J' || ret.isVoid())) {
                run = RT + ".insert(__db, _sql.toString(), _args.toArray())";
            } else if (ret.isVoid() || k == 'I') {
                run = RT + ".updateDelete(__db, _sql.toString(), _args)";
            } else {
                throw new RoomError(w + ": a " + kind + " query can return void, int" + (insertId ? " or long" : ""));
            }
            if (ret.isVoid()) {
                s.append("            ").append(run).append(";\n");
            } else {
                s.append("            ").append(k == 'J' ? "long" : "int").append(" _r = ").append(run).append(";\n");
            }
            s.append(notify(tables, "            "));
            s.append("            __db.setTransactionSuccessful();\n");
            if (!ret.isVoid()) {
                s.append("            return _r;\n");
            }
            s.append("        } finally {\n            __db.endTransaction();\n        }\n");
            return s.toString();
        }

        private String notify(Set<String> tables, String ind) {
            if (tables.isEmpty()) {
                return "";
            }
            StringBuilder s = new StringBuilder(ind + "__db.getInvalidationTracker().notifyTablesChanged(");
            int i = 0;
            for (String t : tables) {
                s.append(i++ > 0 ? ", " : "").append(quote(t));
            }
            return s.append(");\n").toString();
        }

        private String rawQuery(DaoMethod dm, String w, AnnotationNode a, boolean liveTx) {
            if (dm.sig.params.size() != 1 || !(dm.sig.params.get(0).isClass("androidx/sqlite/db/SupportSQLiteQuery")
                    || dm.sig.params.get(0).isClass("androidx/sqlite/db/SimpleSQLiteQuery"))) {
                throw new RoomError(w + ": a @RawQuery method takes one SupportSQLiteQuery");
            }
            Set<String> tables = new LinkedHashSet<String>();
            for (String e : types(a, "observedEntities")) {
                String t = db.tableOf(e);
                if (t == null) {
                    throw new RoomError(w + ": observedEntities names " + fqn(e) + ", not an entity of the database");
                }
                tables.add(t);
            }
            return read(w, dm.sig.ret, "", "__rawCursor", tables, liveTx);
        }

        /// A read: build the SQL, run it, map the rows to `ret`.
        private String read(String w, JType ret, String build, String cursorKind, Set<String> tables,
                            boolean liveTx) {
            if (ret.isClass("androidx/lifecycle/LiveData")) {
                if (ret.args.isEmpty()) {
                    throw new RoomError(w + ": LiveData needs a type argument");
                }
                if (tables.isEmpty()) {
                    throw new RoomError(w + ": Observable query return type (LiveData) used but the query"
                            + " returns no tables (cannot find any table to observe)");
                }
                JType inner = ret.arg(0);
                String in = inner.src(true);
                StringBuilder s = new StringBuilder();
                s.append("        return __db.getInvalidationTracker().createLiveData(new String[] {");
                int i = 0;
                for (String t : tables) {
                    s.append(i++ > 0 ? ", " : "").append(quote(t));
                }
                s.append("}, ").append(liveTx).append(", new java.util.concurrent.Callable<").append(in)
                        .append(">() {\n            @Override\n            public ").append(in)
                        .append(" call() throws Exception {\n");
                s.append(indent(build + rows(w, inner, cursorKind), "                "));
                s.append("            }\n        });\n");
                return s.toString();
            }
            if (ret.isVoid()) {
                throw new RoomError(w + ": a SELECT query must return a value");
            }
            return indent(build + rows(w, ret, cursorKind), "        ");
        }

        private String cursorExpr(String kind) {
            return kind.equals("__rawCursor") ? "__db.query(_p0)" : RT + ".query(__db, _sql.toString(), _args)";
        }

        /// Statements that run the query and return `ret` from its rows.
        private String rows(String w, JType ret, String kind) {
            if (ret.isClass("android/database/Cursor")) {
                return "return " + cursorExpr(kind) + ";\n";
            }
            StringBuilder s = new StringBuilder();
            s.append("android.database.Cursor _c = ").append(cursorExpr(kind)).append(";\ntry {\n");
            boolean list = false;
            for (String l : LIST_TYPES) {
                list |= ret.isClass(l);
            }
            boolean set = false;
            for (String l : SET_TYPES) {
                set |= ret.isClass(l);
            }
            boolean array = ret.isArray() && !(ret.dims == 1 && ret.kind == 'B');
            if (list || set || array) {
                JType e = array ? ret.element() : ret.args.isEmpty() ? null : ret.arg(0);
                if (e == null) {
                    throw new RoomError(w + ": a raw " + ret.src() + " cannot be mapped; give it a type argument");
                }
                String coll = (set ? "java.util.LinkedHashSet<" : "java.util.ArrayList<") + e.src(true) + ">";
                s.append("    ").append(coll).append(" _r = new ").append(coll).append("();\n");
                s.append(indent(rowSetup(w, e), "    "));
                s.append("    while (_c.moveToNext()) {\n        _r.add(").append(row(w, e)).append(");\n    }\n");
                if (!array) {
                    s.append("    return _r;\n");
                } else if (e.isPrimitive()) {
                    s.append("    ").append(ret.src()).append(" _a = new ").append(e.src()).append("[_r.size()];\n")
                            .append("    for (int _i = 0; _i < _a.length; _i++) {\n        _a[_i] = _r.get(_i);\n")
                            .append("    }\n    return _a;\n");
                } else {
                    s.append("    return _r.toArray(new ").append(e.rawSrc()).append("[_r.size()]);\n");
                }
            } else {
                s.append(indent(rowSetup(w, ret), "    "));
                s.append("    if (_c.moveToFirst()) {\n        return ").append(row(w, ret)).append(";\n    }\n");
                s.append("    return ").append(defaultValue(ret)).append(";\n");
            }
            s.append("} finally {\n    _c.close();\n}\n");
            return s.toString();
        }

        private String rowSetup(String w, JType t) {
            if (codec(t, db.convs) != null) {
                return "";
            }
            return "int[] _ix = " + reader(w, t) + "_index(_c);\n";
        }

        private String row(String w, JType t) {
            Codec c = codec(t, db.convs);
            if (c != null) {
                return c.read("_c", "0");
            }
            return reader(w, t) + "(_c, _ix)";
        }

        /// The name of the reader of `t`'s rows, written once per DAO.
        private String reader(String w, JType t) {
            if (t.kind != 'L' || t.isArray() || !t.args.isEmpty()) {
                throw new RoomError(w + ": Not sure how to convert a Cursor to this method's return type ("
                        + t.src() + ")");
            }
            String name = readers.get(t.name);
            if (name != null) {
                return name;
            }
            if (app.get(t.name) == null) {
                throw new RoomError(w + ": Not sure how to convert a Cursor to this method's return type ("
                        + t.src() + "): it is neither a column type nor an application class");
            }
            Pojo p = pojo(t.name, pkg, db.convs, new HashSet<String>());
            name = "__read_" + simple(t.name) + "_" + (uid++);
            readers.put(t.name, name);
            List<Leaf> leaves = new ArrayList<Leaf>();
            flatten(p, "", leaves);
            StringBuilder h = new StringBuilder();
            h.append("\n    private int[] ").append(name).append("_index(android.database.Cursor _c) {\n")
                    .append("        return new int[] {");
            for (int i = 0; i < leaves.size(); i++) {
                h.append(i > 0 ? ", " : "").append(RT).append(".columnIndex(_c, ").append(quote(leaves.get(i).column))
                        .append(')');
            }
            h.append("};\n    }\n");
            h.append("\n    private ").append(p.type.src()).append(' ').append(name)
                    .append("(android.database.Cursor _c, int[] _ix) {\n");
            StringBuilder body = new StringBuilder();
            String v = build(body, p, new int[1]);
            body.append("return ").append(v).append(";\n");
            h.append(indent(body.toString(), "        "));
            h.append("    }\n");
            helpers.append(h);
            return name;
        }

        /// Statements creating a `p` from the row; answers its variable.
        private String build(StringBuilder out, Pojo p, int[] next) {
            String var = "_o" + (uid++);
            Map<Prop, String> locals = new HashMap<Prop, String>();
            List<String> after = new ArrayList<String>();
            for (Prop prop : p.props) {
                boolean viaCtor = p.ctorParams.contains(prop);
                if (prop.embedded) {
                    int start = next[0];
                    String ev = "_e" + (uid++);
                    out.append(prop.emb.type.src()).append(' ').append(ev).append(" = null;\n");
                    StringBuilder cond = new StringBuilder();
                    for (int k = start; k < start + prop.emb.leafCount; k++) {
                        cond.append(cond.length() > 0 ? " || " : "").append("(_ix[").append(k).append("] != -1 && !_c")
                                .append(".isNull(_ix[").append(k).append("]))");
                    }
                    out.append("if (").append(cond.length() == 0 ? "true" : cond).append(") {\n");
                    StringBuilder inner = new StringBuilder();
                    String iv = build(inner, prop.emb, next);
                    out.append(indent(inner.toString(), "    "));
                    out.append("    ").append(ev).append(" = ").append(iv).append(";\n}\n");
                    locals.put(prop, ev);
                    if (!viaCtor) {
                        after.add(assign(var, prop, ev));
                    }
                } else {
                    int k = next[0]++;
                    String read = prop.codec.read("_c", "_ix[" + k + "]");
                    if (viaCtor) {
                        String lv = "_v" + (uid++);
                        out.append(prop.type.src()).append(' ').append(lv).append(" = _ix[").append(k)
                                .append("] == -1 ? ").append(defaultValue(prop.type)).append(" : ").append(read)
                                .append(";\n");
                        locals.put(prop, lv);
                    } else {
                        after.add("if (_ix[" + k + "] != -1) {\n    " + assign(var, prop, read) + "\n}");
                    }
                }
            }
            out.append(p.type.src()).append(' ').append(var).append(" = new ").append(p.type.src()).append('(');
            for (int i = 0; i < p.ctorParams.size(); i++) {
                out.append(i > 0 ? ", " : "").append(locals.get(p.ctorParams.get(i)));
            }
            out.append(");\n");
            for (String a : after) {
                out.append(a).append('\n');
            }
            return var;
        }

        private String assign(String var, Prop prop, String expr) {
            if (prop.setter != null) {
                return var + "." + prop.setter + "(" + expr + ");";
            }
            return var + "." + prop.field + " = " + expr + ";";
        }

        private String getter(String obj, Prop prop, String cls) {
            if (prop.getter != null) {
                return obj + "." + prop.getter + "()";
            }
            if (prop.fieldReadable) {
                return obj + "." + prop.field;
            }
            throw new RoomError(fqn(cls) + "." + prop.field + ": Cannot find getter for field (it is private and"
                    + " has no accessible getX()/isX() method)");
        }

        // ---------------------------------------------------- writes

        /// The binder of an entity's columns, written once per DAO.
        private String binder(Entity e) {
            String name = binders.get(e.cls);
            if (name != null) {
                return name;
            }
            Pojo p = pojo(e.cls, pkg, db.convs, new HashSet<String>());
            name = "__bind_" + simple(e.cls) + "_" + (uid++);
            binders.put(e.cls, name);
            int auto = e.autoGenerate == null ? -1 : e.autoGenerate.index;
            Set<Integer> pkIdx = new HashSet<Integer>();
            for (Leaf l : e.pk) {
                pkIdx.add(Integer.valueOf(l.index));
            }
            StringBuilder h = new StringBuilder();
            h.append("\n    private Object[] ").append(name).append('(').append(p.type.src()).append(" _o) {\n")
                    .append("        Object[] _a = new Object[").append(e.leaves.size()).append("];\n");
            StringBuilder body = new StringBuilder();
            emitBind(body, p, "_o", new int[1], null, auto, new int[1]);
            h.append(indent(body.toString(), "        "));
            h.append("        return _a;\n    }\n");
            h.append("\n    private Object[] ").append(name).append("_pk(").append(p.type.src()).append(" _o) {\n")
                    .append("        Object[] _a = new Object[").append(e.pk.size()).append("];\n");
            body = new StringBuilder();
            emitBind(body, p, "_o", new int[1], pkIdx, -1, new int[1]);
            h.append(indent(body.toString(), "        "));
            h.append("        return _a;\n    }\n");
            helpers.append(h);
            return name;
        }

        private void emitBind(StringBuilder out, Pojo p, String obj, int[] next, Set<Integer> only, int auto,
                              int[] slot) {
            for (Prop prop : p.props) {
                if (prop.embedded) {
                    String ev = "_e" + (uid++);
                    out.append(prop.emb.type.src()).append(' ').append(ev).append(" = ")
                            .append(getter(obj, prop, p.cls)).append(";\n");
                    out.append("if (").append(ev).append(" != null) {\n");
                    StringBuilder inner = new StringBuilder();
                    int[] innerSlot = {slot[0]};
                    emitBind(inner, prop.emb, ev, next, only, auto, innerSlot);
                    out.append(indent(inner.toString(), "    ")).append("}\n");
                    slot[0] = innerSlot[0];
                    continue;
                }
                int k = next[0]++;
                if (only != null && !only.contains(Integer.valueOf(k))) {
                    continue;
                }
                int s = slot[0]++;
                String v = "_v" + (uid++);
                out.append(prop.type.src()).append(' ').append(v).append(" = ").append(getter(obj, prop, p.cls))
                        .append(";\n");
                if (k == auto && prop.type.isPrimitive()) {
                    out.append("_a[").append(s).append("] = ").append(v).append(" == 0 ? null : (Object) ")
                            .append(prop.codec.bind(v)).append(";\n");
                } else {
                    out.append("_a[").append(s).append("] = ").append(prop.codec.bind(v)).append(";\n");
                }
            }
        }

        private String insertSql(Entity e, int conflict) {
            String[] words = {"ABORT", "REPLACE", "ROLLBACK", "ABORT", "FAIL", "IGNORE"};
            StringBuilder s = new StringBuilder("INSERT OR " + words[conflict < 0 || conflict > 5 ? 3 : conflict]
                    + " INTO `" + e.table + "` (");
            StringBuilder q = new StringBuilder();
            for (int i = 0; i < e.leaves.size(); i++) {
                s.append(i > 0 ? "," : "").append('`').append(e.leaves.get(i).column).append('`');
                q.append(i > 0 ? "," : "").append('?');
            }
            return s.append(") VALUES (").append(q).append(')').toString();
        }

        private String pkWhere(Entity e) {
            StringBuilder s = new StringBuilder(" WHERE ");
            for (int i = 0; i < e.pk.size(); i++) {
                s.append(i > 0 ? " AND " : "").append('`').append(e.pk.get(i).column).append("` = ?");
            }
            return s.toString();
        }

        private String updateSql(Entity e, int conflict) {
            String[] words = {"ABORT", "REPLACE", "ROLLBACK", "ABORT", "FAIL", "IGNORE"};
            StringBuilder s = new StringBuilder("UPDATE OR " + words[conflict < 0 || conflict > 5 ? 3 : conflict]
                    + " `" + e.table + "` SET ");
            for (int i = 0; i < e.leaves.size(); i++) {
                s.append(i > 0 ? "," : "").append('`').append(e.leaves.get(i).column).append("` = ?");
            }
            return s.append(pkWhere(e)).toString();
        }

        /// @Insert, @Update, @Delete and @Upsert.
        private String write(DaoMethod dm, String w, AnnotationNode a) {
            String kind = a.desc.equals(INSERT) ? "insert" : a.desc.equals(UPDATE) ? "update"
                    : a.desc.equals(DELETE) ? "delete" : "upsert";
            String partial = typeVal(a, "entity");
            if (partial != null && !partial.equals("java/lang/Object")) {
                throw new RoomError(w + ": partial entities (@" + cap(kind) + "(entity = ...)) are not supported"
                        + " by the Codename One Room build; pass the entity itself");
            }
            int conflict = intv(a, "onConflict", 3);
            List<JType> params = dm.sig.params;
            if (params.isEmpty()) {
                throw new RoomError(w + ": a @" + cap(kind) + " method must have at least one parameter");
            }
            List<Entity> ents = new ArrayList<Entity>();
            List<Integer> shapes = new ArrayList<Integer>();
            for (JType p : params) {
                int shape = 0;
                JType e = p;
                if (p.isArray()) {
                    shape = 1;
                    e = p.element();
                } else {
                    for (String l : LIST_TYPES) {
                        if (p.isClass(l) && !p.args.isEmpty()) {
                            shape = 1;
                            e = p.arg(0);
                        }
                    }
                    for (String l : SET_TYPES) {
                        if (p.isClass(l) && !p.args.isEmpty()) {
                            shape = 1;
                            e = p.arg(0);
                        }
                    }
                }
                Entity ent = e.kind == 'L' && e.dims == 0 ? db.entities.get(e.name) : null;
                if (ent == null) {
                    throw new RoomError(w + ": Type of the parameter must be a class annotated with @Entity of this"
                            + " database or a collection/array of it (" + p.src() + ")");
                }
                ents.add(ent);
                shapes.add(Integer.valueOf(shape));
            }
            JType ret = dm.sig.ret;
            boolean ids = kind.equals("insert") || kind.equals("upsert");
            String retKind;
            char rk = ret.isPrimitive() ? ret.kind : RoomTypes.unboxed(ret);
            if (ret.isVoid()) {
                retKind = "void";
            } else if (ids && params.size() == 1 && shapes.get(0).intValue() == 0 && rk == 'J') {
                retKind = "id";
            } else if (ids && params.size() == 1 && shapes.get(0).intValue() == 1
                    && (ret.dims == 1 && (ret.kind == 'J' || (ret.kind == 'L' && ret.name.equals("java/lang/Long")))
                    || ((ret.isClass("java/util/List") || ret.isClass("java/util/Collection")
                    || ret.isClass("java/util/ArrayList")) && !ret.args.isEmpty()
                    && ret.arg(0).isClass("java/lang/Long")))) {
                retKind = "ids";
            } else if (!ids && rk == 'I') {
                retKind = "count";
            } else {
                throw new RoomError(w + ": " + (ids ? "an @Insert or @Upsert method can return void, long (one"
                        + " entity) or long[] / List<Long> (a collection)"
                        : "an @Update or @Delete method can return void or int") + "; it returns " + ret.src());
            }
            StringBuilder s = new StringBuilder();
            s.append("        __db.beginTransaction();\n        try {\n");
            if (retKind.equals("id")) {
                s.append("            long _id = -1;\n");
            } else if (retKind.equals("ids")) {
                s.append("            java.util.ArrayList<Long> _ids = new java.util.ArrayList<Long>();\n");
            } else if (retKind.equals("count")) {
                s.append("            int _n = 0;\n");
            }
            Set<String> tables = new LinkedHashSet<String>();
            for (int i = 0; i < params.size(); i++) {
                Entity e = ents.get(i);
                tables.add(e.table);
                String b = binder(e);
                String x = "_p" + i;
                String stmt;
                if (kind.equals("insert")) {
                    String call = RT + ".insert(__db, " + quote(insertSql(e, conflict)) + ", " + b + "(%s))";
                    stmt = retKind.equals("id") ? "_id = " + call + ";" : retKind.equals("ids")
                            ? "_ids.add(Long.valueOf(" + call + "));" : call + ";";
                } else if (kind.equals("upsert")) {
                    String y = "_u" + (uid++);
                    stmt = "long " + y + ";\ntry {\n    " + y + " = " + RT + ".insert(__db, "
                            + quote(insertSql(e, 3)) + ", " + b + "(%s));\n} catch (RuntimeException _ex) {\n"
                            + "    if (!" + RT + ".isUniqueViolation(_ex)) {\n        throw _ex;\n    }\n    "
                            + RT + ".updateDelete(__db, " + quote(updateSql(e, 3)) + ", " + RT + ".concat(" + b
                            + "(%s), " + b + "_pk(%s)));\n    " + y + " = -1;\n}\n"
                            + (retKind.equals("id") ? "_id = " + y + ";" : retKind.equals("ids")
                            ? "_ids.add(Long.valueOf(" + y + "));" : "");
                } else if (kind.equals("update")) {
                    String call = RT + ".updateDelete(__db, " + quote(updateSql(e, conflict)) + ", " + RT + ".concat("
                            + b + "(%s), " + b + "_pk(%s)))";
                    stmt = retKind.equals("count") ? "_n += " + call + ";" : call + ";";
                } else {
                    String call = RT + ".updateDelete(__db, " + quote("DELETE FROM `" + e.table + "`" + pkWhere(e))
                            + ", " + b + "_pk(%s))";
                    stmt = retKind.equals("count") ? "_n += " + call + ";" : call + ";";
                }
                if (shapes.get(i).intValue() == 0) {
                    s.append(indent(stmt.replace("%s", x), "            "));
                    s.append('\n');
                } else {
                    String el = "_e" + (uid++);
                    JType et = JType.cls(e.cls);
                    s.append("            if (").append(x).append(" != null) {\n                for (")
                            .append(et.src()).append(' ').append(el).append(" : ").append(x).append(") {\n");
                    s.append(indent(stmt.replace("%s", el), "                    "));
                    s.append("\n                }\n            }\n");
                }
            }
            s.append(notify(tables, "            "));
            s.append("            __db.setTransactionSuccessful();\n");
            if (retKind.equals("id")) {
                s.append("            return _id;\n");
            } else if (retKind.equals("count")) {
                s.append("            return _n;\n");
            } else if (retKind.equals("ids")) {
                if (ret.isArray()) {
                    s.append("            ").append(ret.src()).append(" _a = new ").append(ret.element().src())
                            .append("[_ids.size()];\n            for (int _i = 0; _i < _a.length; _i++) {\n")
                            .append("                _a[_i] = _ids.get(_i);\n            }\n")
                            .append("            return _a;\n");
                } else {
                    s.append("            return _ids;\n");
                }
            }
            s.append("        } finally {\n            __db.endTransaction();\n        }\n");
            return s.toString();
        }

        private Set<String> tables(String sql) {
            Set<String> out = new LinkedHashSet<String>();
            Map<String, String> known = new HashMap<String, String>();
            for (Entity e : db.entities.values()) {
                known.put(asciiLower(e.table), e.table);
            }
            int i = 0;
            while (i < sql.length()) {
                char c = sql.charAt(i);
                if (Character.isLetter(c) || c == '_') {
                    int j = i;
                    while (j < sql.length() && (Character.isLetterOrDigit(sql.charAt(j)) || sql.charAt(j) == '_')) {
                        j++;
                    }
                    String t = known.get(asciiLower(sql.substring(i, j)));
                    if (t != null && (i == 0 || sql.charAt(i - 1) != ':')) {
                        out.add(t);
                    }
                    i = j;
                } else if (c == '`' || c == '"' || c == '[') {
                    char close = c == '[' ? ']' : c;
                    int j = sql.indexOf(close, i + 1);
                    if (j < 0) {
                        break;
                    }
                    String t = known.get(asciiLower(sql.substring(i + 1, j)));
                    if (t != null) {
                        out.add(t);
                    }
                    i = j + 1;
                } else if (c == '\'') {
                    int j = sql.indexOf('\'', i + 1);
                    i = j < 0 ? sql.length() : j + 1;
                } else {
                    i++;
                }
            }
            return out;
        }
    }

    // ------------------------------------------------------------ SQL

    /// The SQL as literal strings and parameters (`String[] {name}`, a null
    /// name for `?`).
    static List<Object> parseSql(String sql, String w) {
        List<Object> out = new ArrayList<Object>();
        StringBuilder lit = new StringBuilder();
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'' || c == '"' || c == '`') {
                int j = sql.indexOf(c, i + 1);
                if (j < 0) {
                    throw new RoomError(w + ": unterminated quote in query");
                }
                lit.append(sql, i, j + 1);
                i = j + 1;
            } else if (c == ':' && i + 1 < sql.length()
                    && (Character.isLetter(sql.charAt(i + 1)) || sql.charAt(i + 1) == '_')
                    && (i == 0 || sql.charAt(i - 1) != ':')) {
                int j = i + 1;
                while (j < sql.length() && (Character.isLetterOrDigit(sql.charAt(j)) || sql.charAt(j) == '_')) {
                    j++;
                }
                if (lit.length() > 0) {
                    out.add(lit.toString());
                    lit.setLength(0);
                }
                out.add(new String[] {sql.substring(i + 1, j)});
                i = j;
            } else if (c == '?') {
                int j = i + 1;
                while (j < sql.length() && Character.isDigit(sql.charAt(j))) {
                    j++;
                }
                if (j > i + 1) {
                    throw new RoomError(w + ": numbered ?NNN parameters are not supported; use :name");
                }
                if (lit.length() > 0) {
                    out.add(lit.toString());
                    lit.setLength(0);
                }
                out.add(new String[] {null});
                i = j;
            } else {
                lit.append(c);
                i++;
            }
        }
        if (lit.length() > 0) {
            out.add(lit.toString());
        }
        return out;
    }

    static String firstWord(String sql) {
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c) || c == '(') {
                i++;
            } else if (sql.startsWith("--", i)) {
                int j = sql.indexOf('\n', i);
                i = j < 0 ? sql.length() : j + 1;
            } else if (sql.startsWith("/*", i)) {
                int j = sql.indexOf("*/", i);
                i = j < 0 ? sql.length() : j + 2;
            } else {
                break;
            }
        }
        int j = i;
        while (j < sql.length() && Character.isLetter(sql.charAt(j))) {
            j++;
        }
        String w = sql.substring(i, j);
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < w.length(); k++) {
            char c = w.charAt(k);
            sb.append(c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c);
        }
        return sb.toString();
    }

    static String defaultValue(JType t) {
        if (!t.isPrimitive()) {
            return "null";
        }
        switch (t.kind) {
            case 'Z': return "false";
            case 'C': return "(char) 0";
            case 'B': return "(byte) 0";
            case 'S': return "(short) 0";
            case 'J': return "0L";
            case 'F': return "0f";
            case 'D': return "0d";
            default: return "0";
        }
    }

    static String indent(String code, String ind) {
        if (code.isEmpty()) {
            return code;
        }
        StringBuilder sb = new StringBuilder();
        for (String line : code.split("\n", -1)) {
            if (line.isEmpty()) {
                sb.append('\n');
            } else {
                sb.append(ind).append(line).append('\n');
            }
        }
        // split keeps the empty piece after the last newline
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
