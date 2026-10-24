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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.builders.BuildException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/// A small Room application compiled against the real compatibility
/// runtime, put through the remap step (which runs the Room generator) and
/// then run on the JVM from the relocated classes alone.
public class RoomGeneratorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final List<String> warnings = new ArrayList<String>();

    private final com.codename1.build.Log log = new com.codename1.build.Log() {
        public void debug(CharSequence c) { }
        public void debug(CharSequence c, Throwable e) { }
        public void debug(Throwable e) { }
        public void info(CharSequence c) { }
        public void info(CharSequence c, Throwable e) { }
        public void info(Throwable e) { }
        public void warn(CharSequence c) {
            warnings.add(c.toString());
        }
        public void warn(CharSequence c, Throwable e) { }
        public void warn(Throwable e) { }
        public void error(CharSequence c) { }
        public void error(CharSequence c, Throwable e) { }
        public void error(Throwable e) { }
        public boolean isDebugEnabled() { return false; }
        public boolean isInfoEnabled() { return false; }
        public boolean isWarnEnabled() { return false; }
        public boolean isErrorEnabled() { return false; }
    };

    private static Map<String, String> appSources() {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("app/Priority.java", "package app;\npublic enum Priority { LOW, HIGH }\n");
        s.put("app/Meta.java", "package app;\n"
                + "public class Meta {\n"
                + "    public String author;\n"
                + "    public Integer rev;\n"
                + "}\n");
        s.put("app/Converters.java", "package app;\n"
                + "import androidx.room.TypeConverter;\n"
                + "public class Converters {\n"
                + "    @TypeConverter public static Long fromDate(java.util.Date d) {\n"
                + "        return d == null ? null : Long.valueOf(d.getTime());\n"
                + "    }\n"
                + "    @TypeConverter public static java.util.Date toDate(Long v) {\n"
                + "        return v == null ? null : new java.util.Date(v.longValue());\n"
                + "    }\n"
                + "}\n");
        s.put("app/Note.java", "package app;\n"
                + "import androidx.room.*;\n"
                + "@Entity(tableName = \"notes\", indices = {@Index(value = {\"title\"}, unique = true)})\n"
                + "public class Note {\n"
                + "    @PrimaryKey(autoGenerate = true) private long id;\n"
                + "    @ColumnInfo(name = \"title\") private String mTitle;\n"
                + "    private boolean done;\n"
                + "    public Priority priority;\n"
                + "    public java.util.Date created;\n"
                + "    @Embedded(prefix = \"meta_\") public Meta meta;\n"
                + "    @Ignore public String scratch;\n"
                + "    public Note() {}\n"
                + "    @Ignore public Note(String t) { mTitle = t; priority = Priority.LOW; }\n"
                + "    public long getId() { return id; }\n"
                + "    public void setId(long id) { this.id = id; }\n"
                + "    public String getTitle() { return mTitle; }\n"
                + "    public void setTitle(String t) { mTitle = t; }\n"
                + "    public boolean isDone() { return done; }\n"
                + "    public void setDone(boolean d) { done = d; }\n"
                + "}\n");
        s.put("app/TitleCount.java", "package app;\n"
                + "public final class TitleCount {\n"
                + "    public final String title;\n"
                + "    public final int n;\n"
                + "    public TitleCount(String title, int n) { this.title = title; this.n = n; }\n"
                + "}\n");
        s.put("app/BaseDao.java", "package app;\n"
                + "import androidx.room.*;\n"
                + "public interface BaseDao<T> {\n"
                + "    @Insert long insert(T t);\n"
                + "    @Update int update(T t);\n"
                + "    @Delete void delete(T t);\n"
                + "}\n");
        s.put("app/NoteDao.java", "package app;\n"
                + "import androidx.room.*;\n"
                + "import androidx.lifecycle.LiveData;\n"
                + "import java.util.List;\n"
                + "@Dao\n"
                + "public interface NoteDao extends BaseDao<Note> {\n"
                + "    @Insert(onConflict = OnConflictStrategy.IGNORE) long[] insertAll(List<Note> notes);\n"
                + "    @Upsert long upsert(Note n);\n"
                + "    @Query(\"SELECT * FROM notes WHERE id = :id\") Note byId(long id);\n"
                + "    @Query(\"SELECT * FROM notes WHERE id IN (:ids) ORDER BY id\") List<Note> byIds(List<Long> ids);\n"
                + "    @Query(\"SELECT * FROM notes WHERE priority = :p ORDER BY id\") List<Note> byPriority(Priority p);\n"
                + "    @Query(\"SELECT title FROM notes ORDER BY title\") List<String> titles();\n"
                + "    @Query(\"SELECT COUNT(*) FROM notes WHERE done = :done\") int count(boolean done);\n"
                + "    @Query(\"SELECT title, COUNT(*) AS n FROM notes GROUP BY title ORDER BY title\")"
                + " List<TitleCount> counts();\n"
                + "    @Query(\"SELECT * FROM notes ORDER BY id\") LiveData<List<Note>> live();\n"
                + "    @Query(\"UPDATE notes SET done = 1 WHERE title = :title\") int markDone(String title);\n"
                + "    @Query(\"DELETE FROM notes\") void clear();\n"
                + "    @RawQuery(observedEntities = Note.class) List<Note> raw(androidx.sqlite.db.SupportSQLiteQuery q);\n"
                + "}\n");
        s.put("app/BulkDao.java", "package app;\n"
                + "import androidx.room.*;\n"
                + "@Dao\n"
                + "public abstract class BulkDao {\n"
                + "    @Insert public abstract void insert(Note... notes);\n"
                + "    @Query(\"DELETE FROM notes\") public abstract int clear();\n"
                + "    @Transaction public void replaceAll(Note... notes) {\n"
                + "        clear();\n"
                + "        insert(notes);\n"
                + "    }\n"
                + "}\n");
        s.put("app/AppDb.java", "package app;\n"
                + "import androidx.room.*;\n"
                + "@Database(entities = {Note.class}, version = 1)\n"
                + "@TypeConverters({Converters.class})\n"
                + "public abstract class AppDb extends RoomDatabase {\n"
                + "    public abstract NoteDao notes();\n"
                + "    public abstract BulkDao bulk();\n"
                + "}\n");
        s.put("app/AppDb2.java", "package app;\n"
                + "import androidx.room.*;\n"
                + "@Database(entities = {Note.class}, version = 2)\n"
                + "@TypeConverters({Converters.class})\n"
                + "public abstract class AppDb2 extends RoomDatabase {\n"
                + "    public abstract NoteDao notes();\n"
                + "}\n");
        s.put("android/database/sqlite/RoomDriver.java", DRIVER);
        return s;
    }

    /// Runs inside the relocated application; in this package for the
    /// JDBC-backed context, as the runtime's own tests are.
    private static final String DRIVER = "package android.database.sqlite;\n"
            + "import android.content.Context;\n"
            + "import android.content.ContextWrapper;\n"
            + "import android.database.DatabaseErrorHandler;\n"
            + "import androidx.lifecycle.Observer;\n"
            + "import androidx.room.Room;\n"
            + "import androidx.room.migration.Migration;\n"
            + "import androidx.sqlite.db.SimpleSQLiteQuery;\n"
            + "import androidx.sqlite.db.SupportSQLiteDatabase;\n"
            + "import app.*;\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.Arrays;\n"
            + "import java.util.List;\n"
            + "public final class RoomDriver {\n"
            + "    static void check(boolean b, String what) {\n"
            + "        if (!b) { throw new AssertionError(what); }\n"
            + "    }\n"
            + "    static void eq(Object a, Object b, String what) {\n"
            + "        check(a == null ? b == null : a.equals(b), what + \": expected \" + a + \" got \" + b);\n"
            + "    }\n"
            + "    static Context context(final String dir) {\n"
            + "        return new ContextWrapper(null) {\n"
            + "            @Override\n"
            + "            public SQLiteDatabase openOrCreateDatabase(String name, int mode,\n"
            + "                    SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler eh) {\n"
            + "                try {\n"
            + "                    return SQLiteDatabase.wrap(new JdbcDatabase(\"jdbc:sqlite:\" + dir + \"/\" + name),\n"
            + "                            name, factory, eh);\n"
            + "                } catch (java.io.IOException e) {\n"
            + "                    throw new SQLiteException(e.getMessage());\n"
            + "                }\n"
            + "            }\n"
            + "        };\n"
            + "    }\n"
            + "    static Note note(String t, Priority p) {\n"
            + "        Note n = new Note(t);\n"
            + "        n.priority = p;\n"
            + "        return n;\n"
            + "    }\n"
            + "    public static String run(String dir) {\n"
            + "        Context ctx = context(dir);\n"
            // Live queries run on the query executor, as in AndroidX; a direct
            // one keeps every value in order on this thread.
            + "        AppDb db = Room.databaseBuilder(ctx, AppDb.class, \"app.db\").allowMainThreadQueries()\n"
            + "                .setQueryExecutor(new java.util.concurrent.Executor() {\n"
            + "                    public void execute(Runnable r) { r.run(); }\n"
            + "                }).build();\n"
            + "        NoteDao dao = db.notes();\n"
            + "        check(dao == db.notes(), \"dao is cached\");\n"
            + "        final List<Integer> sizes = new ArrayList<Integer>();\n"
            + "        Observer<List<Note>> obs = new Observer<List<Note>>() {\n"
            + "            public void onChanged(List<Note> v) { sizes.add(Integer.valueOf(v.size())); }\n"
            + "        };\n"
            + "        dao.live().observeForever(obs);\n"
            + "        Note a = note(\"a\", Priority.HIGH);\n"
            + "        a.meta = new Meta();\n"
            + "        a.meta.author = \"ann\";\n"
            + "        a.meta.rev = 3;\n"
            + "        a.created = new java.util.Date(1234L);\n"
            + "        long id = dao.insert(a);\n"
            + "        eq(Long.valueOf(1), Long.valueOf(id), \"first id\");\n"
            + "        long[] ids = dao.insertAll(Arrays.asList(note(\"b\", Priority.LOW), note(\"a\", Priority.LOW)));\n"
            + "        eq(Long.valueOf(2), Long.valueOf(ids[0]), \"second id\");\n"
            + "        eq(Long.valueOf(-1), Long.valueOf(ids[1]), \"ignored duplicate\");\n"
            + "        Note back = dao.byId(1);\n"
            + "        eq(\"a\", back.getTitle(), \"title\");\n"
            + "        eq(Priority.HIGH, back.priority, \"enum\");\n"
            + "        eq(Long.valueOf(1234L), Long.valueOf(back.created.getTime()), \"converter\");\n"
            + "        eq(\"ann\", back.meta.author, \"embedded\");\n"
            + "        eq(Integer.valueOf(3), back.meta.rev, \"embedded int\");\n"
            + "        check(dao.byId(2).meta == null, \"all-null embedded stays null\");\n"
            + "        check(dao.byId(99) == null, \"missing row\");\n"
            + "        eq(Arrays.asList(\"a\", \"b\"), dao.titles(), \"titles\");\n"
            + "        eq(Integer.valueOf(2), Integer.valueOf(dao.byIds(Arrays.asList(Long.valueOf(1), Long.valueOf(2),\n"
            + "                Long.valueOf(7))).size()), \"IN list\");\n"
            + "        eq(\"b\", dao.byPriority(Priority.LOW).get(0).getTitle(), \"enum parameter\");\n"
            + "        eq(Integer.valueOf(1), Integer.valueOf(dao.markDone(\"b\")), \"write query\");\n"
            + "        eq(Integer.valueOf(1), Integer.valueOf(dao.count(true)), \"boolean parameter\");\n"
            + "        check(dao.byId(2).isDone(), \"boolean column\");\n"
            + "        back.setTitle(\"c\");\n"
            + "        eq(Integer.valueOf(1), Integer.valueOf(dao.update(back)), \"update\");\n"
            + "        eq(\"c\", dao.counts().get(1).title, \"constructor pojo\");\n"
            + "        Note up = dao.byId(2);\n"
            + "        up.setTitle(\"bb\");\n"
            + "        eq(Long.valueOf(-1), Long.valueOf(dao.upsert(up)), \"upsert updates\");\n"
            + "        long dId = dao.upsert(note(\"d\", Priority.LOW));\n"
            + "        eq(\"d\", dao.byId(dId).getTitle(), \"upsert inserts\");\n"
            + "        eq(Integer.valueOf(3), Integer.valueOf(dao.raw(new SimpleSQLiteQuery(\"SELECT * FROM notes\")).size()), \"raw\");\n"
            + "        dao.delete(dao.byId(dId));\n"
            + "        eq(Arrays.asList(\"bb\", \"c\"), dao.titles(), \"delete\");\n"
            + "        db.bulk().replaceAll(note(\"x\", Priority.LOW), note(\"y\", Priority.LOW), note(\"z\", Priority.LOW));\n"
            + "        eq(Arrays.asList(\"x\", \"y\", \"z\"), dao.titles(), \"transaction\");\n"
            + "        dao.live().removeObserver(obs);\n"
            + "        String live = sizes.toString();\n"
            + "        db.close();\n"
            + "        final List<String> ran = new ArrayList<String>();\n"
            + "        AppDb2 db2 = Room.databaseBuilder(ctx, AppDb2.class, \"app.db\").allowMainThreadQueries()\n"
            + "                .addMigrations(new Migration(1, 2) {\n"
            + "                    public void migrate(SupportSQLiteDatabase d) { ran.add(\"1-2\"); }\n"
            + "                }).build();\n"
            + "        eq(Arrays.asList(\"x\", \"y\", \"z\"), db2.notes().titles(), \"migrated data\");\n"
            + "        eq(Arrays.asList(\"1-2\"), ran, \"migration ran\");\n"
            + "        db2.close();\n"
            + "        return live;\n"
            + "    }\n"
            + "}\n";

    @Test
    public void generatedRoomAppRunsFromRelocatedClasses() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = appSources();
        File jdbc = new File("../android-compat/src/test/java/android/database/sqlite/JdbcDatabase.java");
        assertTrue("android-compat test sources are needed: " + jdbc.getAbsolutePath(), jdbc.isFile());
        sources.put("android/database/sqlite/JdbcDatabase.java",
                new String(Files.readAllBytes(jdbc.toPath()), StandardCharsets.UTF_8));
        File jar = compatJar();
        compile(sources, classes, jar);
        new AndroidRemapper(classes, jar, null, log).run();
        assertTrue(new File(classes, "app/AppDb_Impl.class").isFile());
        assertTrue(new File(classes, "app/NoteDao_Impl.class").isFile());
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Class<?> driver = loader.loadClass(AndroidRemapper.TARGET.replace('/', '.')
                    + "android.database.sqlite.RoomDriver");
            Method run = driver.getMethod("run", String.class);
            String live;
            try {
                live = (String) run.invoke(null, tmp.newFolder("db").getAbsolutePath());
            } catch (InvocationTargetException e) {
                throw new AssertionError(e.getCause().toString(), e.getCause());
            }
            // One value on registration, one per write outside the
            // transaction (the IGNOREd insert still notifies), one for it.
            assertTrue(live, live.startsWith("[0, 1, "));
            assertTrue(live, live.endsWith(", 3]"));
        } finally {
            loader.close();
        }
    }

    @Test
    public void suspendFunctionFailsTheBuildNamingIt() throws Exception {
        assertRefused("app/KDao.java", "package app;\n"
                + "import androidx.room.*;\n"
                + "@Dao public interface KDao {\n"
                + "    @Query(\"SELECT * FROM notes\") Object all(kotlin.coroutines.Continuation<? super java.util.List<Note>> c);\n"
                + "}\n", "app.KDao.all()", "suspend");
    }

    @Test
    public void relationFailsTheBuildNamingIt() throws Exception {
        assertRefused("app/WithRel.java", "package app;\n"
                + "import androidx.room.*;\n"
                + "public class WithRel {\n"
                + "    @Embedded public Note note;\n"
                + "    @Relation(parentColumn = \"id\", entityColumn = \"id\") public java.util.List<Note> others;\n"
                + "}\n", "app.WithRel.others", "@Relation");
    }

    private void assertRefused(String name, String src, String where, String what) throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = appSources();
        sources.remove("android/database/sqlite/RoomDriver.java");
        sources.put(name, src);
        if (name.contains("KDao")) {
            sources.put("kotlin/coroutines/Continuation.java",
                    "package kotlin.coroutines;\npublic interface Continuation<T> {}\n");
            sources.put("app/AppDb.java", sources.get("app/AppDb.java").replace("public abstract BulkDao bulk();",
                    "public abstract BulkDao bulk();\n    public abstract KDao k();"));
        } else {
            sources.put("app/RelDao.java", "package app;\nimport androidx.room.*;\n@Dao public interface RelDao {\n"
                    + "    @Query(\"SELECT * FROM notes\") java.util.List<WithRel> all();\n}\n");
            sources.put("app/AppDb.java", sources.get("app/AppDb.java").replace("public abstract BulkDao bulk();",
                    "public abstract BulkDao bulk();\n    public abstract RelDao rel();"));
        }
        File jar = compatJar();
        compile(sources, classes, jar);
        try {
            new AndroidRemapper(classes, jar, null, log).run();
            fail("expected the build to fail");
        } catch (BuildException expected) {
            String m = expected.getMessage();
            assertTrue(m, m.contains(where));
            assertTrue(m, m.contains(what));
        }
    }

    /// The runtime jar on the test class path, zipped when the reactor hands
    /// over its classes directory.
    private File compatJar() throws IOException {
        File f;
        try {
            f = new File(Class.forName("androidx.room.Room").getProtectionDomain().getCodeSource().getLocation()
                    .toURI());
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (f.isFile()) {
            return f;
        }
        File out = tmp.newFile("android-compat.jar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(out));
        try {
            List<File> all = new ArrayList<File>();
            collect(f, all);
            String root = f.getAbsolutePath() + File.separator;
            for (File c : all) {
                zip.putNextEntry(new ZipEntry(c.getAbsolutePath().substring(root.length())
                        .replace(File.separatorChar, '/')));
                zip.write(Files.readAllBytes(c.toPath()));
                zip.closeEntry();
            }
        } finally {
            zip.close();
        }
        return out;
    }

    private static void collect(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    collect(f, out);
                } else {
                    out.add(f);
                }
            }
        }
    }

    /// Compiles against the runtime and the core only, the way an
    /// application's Android sources are, with parameter names kept.
    private void compile(Map<String, String> sources, File out, File jar) throws Exception {
        File src = tmp.newFolder();
        List<String> args = new ArrayList<String>(Arrays.asList("-source", "1.8", "-target", "1.8", "-parameters",
                "-nowarn", "-encoding", "UTF-8", "-d", out.getAbsolutePath(), "-cp",
                jar.getAbsolutePath() + File.pathSeparator + coreJar()));
        for (Map.Entry<String, String> e : sources.entrySet()) {
            File f = new File(src, e.getKey());
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), e.getValue().getBytes(StandardCharsets.UTF_8));
            args.add(f.getAbsolutePath());
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
        int rc = javac.run(null, null, err, args.toArray(new String[0]));
        assertEquals(err.toString("UTF-8"), 0, rc);
    }

    private static String coreJar() throws Exception {
        return new File(com.codename1.db.Database.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getAbsolutePath();
    }
}
