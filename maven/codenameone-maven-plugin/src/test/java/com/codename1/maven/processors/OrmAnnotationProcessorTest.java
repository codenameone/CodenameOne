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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Verifies the `@Entity` processor produces a structurally sound dao for a
/// simple POJO entity, and that the negative cases (missing @Id, relationship
/// fields) surface validation errors instead of silently emitting bad SQL.
public class OrmAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void generatesDaoWithExpectedShape() throws Exception {
        File classes = compileFixture(
                "com.example.User",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Entity(table=\"users\")\n"
                        + "public class User {\n"
                        + "    @Id(autoIncrement=true) public long id;\n"
                        + "    @Column(name=\"full_name\", nullable=false) public String name;\n"
                        + "    public int age;\n"
                        + "    @DbTransient public String tempCache;\n"
                        + "    public User() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);

        File daoFile = new File(classes, "com/example/UserCn1Dao.class");
        assertTrue("generated dao file should exist: " + daoFile, daoFile.exists());
        File bootstrapFile = new File(classes, "cn1app/DaoBootstrap.class");
        assertTrue("DaoBootstrap should exist", bootstrapFile.exists());

        Shape shape = readShape(daoFile);
        assertTrue("dao should implement com.codename1.orm.Dao",
                shape.interfaces.contains("com/codename1/orm/Dao"));
        assertTrue(shape.methodNames.contains("createTable"));
        assertTrue(shape.methodNames.contains("insert"));
        assertTrue(shape.methodNames.contains("update"));
        assertTrue(shape.methodNames.contains("delete"));
        assertTrue(shape.methodNames.contains("findById"));
        assertTrue(shape.methodNames.contains("findAll"));
        assertTrue(shape.methodNames.contains("find"));
        assertTrue(shape.methodNames.contains("dropTable"));
        assertTrue(shape.methodNames.contains("attach"));
    }

    @Test
    public void rejectsEntityMissingIdField() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.NoId",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "@Entity public class NoId {\n"
                                + "    public String name;\n"
                                + "    public NoId() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error when @Id is missing", ctx.hasErrors());
    }

    @Test
    public void rejectsEntityWithRelationshipField() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Order",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "@Entity public class Order {\n"
                                + "    @Id public long id;\n"
                                + "    public java.util.List<String> tags;\n"
                                + "    public Order() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error on relationship field", ctx.hasErrors());
    }

    @Test
    public void generatesAServerSideDaoWhenTheModuleIsABackendOne() throws Exception {
        // The SAME entity source as the client case. That is the whole claim of
        // sharing the annotations: one class, and the module it is compiled in
        // decides which database it is stored in.
        File classes = compileFixture(
                "com.example.Note",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Entity(table=\"notes\")\n"
                        + "public class Note {\n"
                        + "    @Id public long id;\n"
                        + "    @Column(nullable=false) public String title;\n"
                        + "    public Integer views;\n"
                        + "    public boolean pinned;\n"
                        + "    public java.util.Date created;\n"
                        + "    @DbTransient public String cache;\n"
                        + "    public Note() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertFalse("the backend flavour reported errors: " + ctx.getErrors(), ctx.hasErrors());

        // The names are NOT the client's, deliberately: a shared module's jar can
        // carry the client dao for this very class, and both have to be able to
        // sit on one classpath.
        File dao = new File(classes, "com/example/NoteCn1BackendDao.class");
        assertTrue("generated server-side dao should exist: " + dao, dao.exists());
        assertTrue("the client dao must NOT be generated in a backend module",
                !new File(classes, "com/example/NoteCn1Dao.class").exists());
        assertTrue("BackendDaoBootstrap should exist",
                new File(classes, "cn1app/BackendDaoBootstrap.class").exists());
        assertFalse("the client bootstrap must not be generated in a backend module",
                new File(classes, "cn1app/DaoBootstrap.class").exists());

        Shape shape = readShape(dao);
        assertTrue("a server-side dao is an EntityDefinition, which is what carries the "
                        + "column descriptions the runtime builds statements from",
                shape.superName.equals("com/codename1/backend/orm/EntityDefinition"));
        // No SQL in the generated class: the statements are the runtime's, built
        // per dialect, which is what lets ONE generated dao serve SQLite,
        // PostgreSQL and MySQL.
        assertTrue(shape.methodNames.contains("columns"));
        assertTrue(shape.methodNames.contains("get"));
        assertTrue(shape.methodNames.contains("set"));
        assertTrue(shape.methodNames.contains("newInstance"));
        assertTrue(shape.methodNames.contains("table"));
    }

    @Test
    public void refusesAGeneratedStringKeyOnTheServer() throws Exception {
        // No engine generates a string key: SQLite's AUTOINCREMENT is legal only
        // after INTEGER PRIMARY KEY and the other two count. Caught here rather
        // than at start-up, where the message is the server's own syntax error.
        File classes = compileFixture(
                "com.example.Session",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Entity public class Session {\n"
                        + "    @Id public String token;\n"
                        + "    public Session() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("a generated String key should be refused", ctx.hasErrors());
    }

    @Test
    public void generatesForAnEntityThatLivesInADependency() throws Exception {
        // The entity in a module BOTH halves of the application depend on, which
        // is where a shared one belongs -- and then the backend module's own
        // compiled classes hold no @Entity at all. Without the classpath scan the
        // server gets no dao and fails on its first query.
        File shared = compileFixture(
                "com.example.Shared",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Entity public class Shared {\n"
                        + "    @Id public long id;\n"
                        + "    public String name;\n"
                        + "    public Shared() {}\n"
                        + "}\n");
        File moduleClasses = tmp.newFolder("module");
        List<String> classpath = new ArrayList<String>(backendClasspath());
        classpath.add(shared.getAbsolutePath());
        ProcessorContext ctx = runProcessor(moduleClasses, classpath);
        assertFalse("errors: " + ctx.getErrors(), ctx.hasErrors());
        assertTrue("a dao should be generated for an entity read off the classpath",
                new File(moduleClasses, "com/example/SharedCn1BackendDao.class").exists());
    }

    @Test
    public void theEntryPointCompilesAgainstTheDaosGeneratedBeforeIt() throws Exception {
        // The packaging order, as a test rather than as a convention.
        //
        // cn1:backend-package compiles into its own class tree and generates
        // into it; the entry point it writes references cn1app.BackendDaoBootstrap
        // whenever the module has an @Entity, because that reference is what
        // keeps the generated daos in the binary. Run the controller processor
        // over a tree the entity processor has not touched and its own javac
        // fails on a class that is not there -- which is what native packaging
        // did for every project that had both.
        String entity = "package com.example;\n"
                + "import com.codename1.annotations.*;\n"
                + "@Entity public class Stored {\n"
                + "    @Id public long id;\n"
                + "    public String name;\n"
                + "    public Stored() {}\n"
                + "}\n";
        String controller = "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController public class Api {\n"
                + "    @GetMapping(\"/healthz\") public String health() { return \"ok\"; }\n"
                + "}\n";

        // Without the entity pass: the entry point cannot compile.
        File alone = tmp.newFolder("without-daos");
        java.util.Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Stored", entity);
        sources.put("com.example.Api", controller);
        JavaSourceCompiler.compile(sources, alone, filesOf(backendClasspath()));
        try {
            runControllers(alone, backendClasspath());
            fail("the entry point referenced a bootstrap that was never generated, so its "
                    + "compilation should have failed");
        } catch (ProcessingException expected) {
            assertTrue(expected.getMessage(),
                    expected.getMessage().contains("BackendDaoBootstrap")
                            || expected.getMessage().contains("cannot find symbol"));
        }

        // With it, in the order the packaging goal now uses.
        File together = tmp.newFolder("with-daos");
        JavaSourceCompiler.compile(sources, together, filesOf(backendClasspath()));
        ProcessorContext ctx = runProcessor(together, backendClasspath(), true);
        assertFalse("errors: " + ctx.getErrors(), ctx.hasErrors());
        assertTrue(new File(together, "cn1app/BackendDaoBootstrap.class").exists());
        runControllers(together, backendClasspath());
        assertTrue("the generated entry point should have compiled",
                new File(together, "com/example/BackendApplication.class").exists());
    }

    /** The controller processor alone, over an already-compiled tree. */
    private void runControllers(File classesDir, List<String> compileClasspath) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog(), null, null, null,
                Collections.<String>emptyList(), "UTF-8", compileClasspath);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
    }

    private static List<File> filesOf(List<String> paths) {
        List<File> out = new ArrayList<File>();
        for (String p : paths) out.add(new File(p));
        return out;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private File compileFixture(String fqn, String src) throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(fqn, src),
                classes,
                Arrays.asList(testClassesDir()));
        return classes;
    }

    private void runProcessorOrFail(File classesDir) throws Exception {
        ProcessorContext ctx = runProcessor(classesDir);
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("processor reported errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
            fail(sb.toString());
        }
    }

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        return runProcessor(classesDir, Collections.<String>emptyList());
    }

    /// Where the backend runtime sits, taken from a loaded class rather than
    /// from a path so it follows the test classpath. Its presence is ALSO what
    /// the processor detects the flavour from, so this exercises the detection
    /// rather than forcing it with a system property.
    private static List<String> backendClasspath() throws Exception {
        URL url = com.codename1.backend.Database.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return Collections.singletonList(new File(url.toURI()).getAbsolutePath());
    }

    private ProcessorContext runProcessor(File classesDir, List<String> compileClasspath)
            throws Exception {
        return runProcessor(classesDir, compileClasspath, false);
    }

    private ProcessorContext runProcessor(File classesDir, List<String> compileClasspath,
                                          boolean forceBackend) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        OrmAnnotationProcessor proc = new OrmAnnotationProcessor();
        if (forceBackend) {
            // What BackendPackageMojo does: that build compiles against the
            // JavaAPI with the runtime off the classpath, so detection cannot
            // answer for it.
            proc.setBackendFlavour(true);
        }
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog(), null, null, null,
                Collections.<String>emptyList(), "UTF-8", compileClasspath);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    private static File testClassesDir() throws Exception {
        URL url = OrmAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }

    private static Shape readShape(File classFile) throws Exception {
        final Shape shape = new Shape();
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                shape.superName = superName;
                if (interfaces != null) {
                    for (String i : interfaces) shape.interfaces.add(i);
                }
            }

            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name,
                                                                String descriptor, String signature,
                                                                String[] exceptions) {
                shape.methodNames.add(name);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return shape;
    }

    private static final class Shape {
        String superName;
        final Set<String> interfaces = new LinkedHashSet<String>();
        final Set<String> methodNames = new LinkedHashSet<String>();
    }
}
