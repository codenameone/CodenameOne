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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.objectweb.asm.Type;

/// The test pass: what makes a `@BackendTest` class runnable without reflection.
///
/// For each test class it writes, next to the test classes:
///
/// - **`<Test>Cn1TestContext`**, in the test's package, implementing
///   `com.codename1.impl.backend.test.TestContext`: the settings to start with and
///   the straight-line code that fills the test's `@Autowired`, `@LocalServerPort`
///   and `@MockitoBean` fields;
/// - **a test wiring**, in the main build's entry package, shared by the classes
///   whose contexts have the same key: the application's beans, resolved again
///   with the context's `@TestConfiguration` beans added and its mocks in place of
///   what they replace -- the same [BackendWiringWriter] output the application
///   ships with, plus a lookup by name for the injections above.
///
/// It also rewrites the test classes the way the main build rewrites beans, so a
/// private field gets a public setter instead of being reached by reflection.
///
/// For a compiled run it writes `<Test>Cn1TestRunner` per test class -- the
/// lifecycle and test calls JUnit would make, in the test's package so it may call
/// package-private methods -- and `cn1app.BackendTestMain` to run them all.
///
/// The application's classes, stand-ins and adapters are the main build's and are
/// not regenerated; [BackendBeans#forTests] keeps every name it computes equal to
/// the one the main build used.
final class BackendTestGenerator {
    static final String BACKEND_TEST = "Lcom/codename1/backend/test/BackendTest;";
    static final String TEST_CONFIGURATION = "Lcom/codename1/backend/test/TestConfiguration;";
    static final String MOCKITO_BEAN = "Lcom/codename1/backend/test/MockitoBean;";
    static final String LOCAL_SERVER_PORT = "Lcom/codename1/backend/test/LocalServerPort;";

    static final String JUNIT_TEST = "Lorg/junit/jupiter/api/Test;";
    static final String JUNIT_BEFORE_EACH = "Lorg/junit/jupiter/api/BeforeEach;";
    static final String JUNIT_AFTER_EACH = "Lorg/junit/jupiter/api/AfterEach;";
    static final String JUNIT_BEFORE_ALL = "Lorg/junit/jupiter/api/BeforeAll;";
    static final String JUNIT_AFTER_ALL = "Lorg/junit/jupiter/api/AfterAll;";
    static final String JUNIT_DISABLED = "Lorg/junit/jupiter/api/Disabled;";
    static final String JUNIT_NESTED = "Lorg/junit/jupiter/api/Nested;";

    /// The compiled run's entry point.
    static final String MAIN_CLASS = "cn1app.BackendTestMain";

    private static final String[] WEB_ENVIRONMENTS = {"MOCK", "RANDOM_PORT", "DEFINED_PORT", "NONE"};

    private final ProcessorContext ctx;
    private final Set<String> mainClasses;
    private final Set<String> testClasses;
    private final File mainOut;
    private final boolean compiled;
    private final Map<String, String> sources = new TreeMap<String, String>();

    /// What the main build recorded about its wiring.
    private String entryPackage;
    private final List<BackendWiringWriter.Router> routers = new ArrayList<BackendWiringWriter.Router>();
    private final Map<String, String> sockets = new LinkedHashMap<String, String>();
    private final List<String[]> routes = new ArrayList<String[]>();

    /// The runner of each compiled test class, for the main.
    private final List<String> runners = new ArrayList<String>();
    /// Test classes a compiled run skips, and why.
    private final Map<String, String> skippedClasses = new TreeMap<String, String>();
    /// Helper class -> method key -> source, for [#call].
    private final Map<String, Map<String, String>> accessHelpers = new TreeMap<String, Map<String, String>>();

    /// One test class's configuration.
    private static final class Spec {
        AnnotatedClass test;
        final Set<String> configurations = new TreeSet<String>();
        final List<String> properties = new ArrayList<String>();
        String profile = "test";
        int webEnvironment;
        /// {field, internal type}
        final List<String[]> mocks = new ArrayList<String[]>();
        String key;
        /// The wiring this class's context starts; set once its key's beans are resolved.
        String wiringBinary;
        BackendBeans model;
    }

    private BackendTestGenerator(ProcessorContext ctx, Set<String> mainClasses,
                                 Set<String> testClasses, File mainOut, boolean compiled,
                                 java.util.function.Predicate<String> selection) {
        this.ctx = ctx;
        this.mainClasses = mainClasses;
        this.testClasses = testClasses;
        this.mainOut = mainOut;
        this.compiled = compiled;
        this.selection = selection;
    }

    /// Which test classes, by binary name, the compiled run runs: the ones the JVM
    /// run discovers, so the two runs exercise one set. Asked `Class#method` too,
    /// for each test of a selected class. Null runs every class.
    private final java.util.function.Predicate<String> selection;

    /// Runs the test pass over `ctx`, whose class index holds both the
    /// application's classes and the test classes, and whose output directory is
    /// where the test classes are. Errors go to `ctx`.
    ///
    /// @return the number of test classes it generated a context or runner for
    static int generate(ProcessorContext ctx, Set<String> mainClasses, Set<String> testClasses,
                        File mainOut, boolean compiled) throws ProcessingException {
        return generate(ctx, mainClasses, testClasses, mainOut, compiled, null);
    }

    /// As above, with the compiled run limited to the test classes `selection` accepts.
    static int generate(ProcessorContext ctx, Set<String> mainClasses, Set<String> testClasses,
                        File mainOut, boolean compiled, java.util.function.Predicate<String> selection)
            throws ProcessingException {
        BackendTestGenerator g = new BackendTestGenerator(ctx, mainClasses, testClasses, mainOut,
                compiled, selection);
        return g.run();
    }

    private int run() throws ProcessingException {
        List<Spec> specs = new ArrayList<Spec>();
        for (String name : new TreeSet<String>(testClasses)) {
            AnnotatedClass cls = ctx.lookup(name);
            if (cls != null && !cls.isAbstract() && backendTestOf(cls) != null) {
                Spec spec = spec(cls);
                if (spec != null) {
                    specs.add(spec);
                }
            }
        }
        if (ctx.hasErrors()) {
            return 0;
        }
        if (!specs.isEmpty()) {
            readWiringRecord();
            resolveContexts(specs);
            if (ctx.hasErrors()) {
                return 0;
            }
            for (Spec spec : specs) {
                String name = contextBinary(spec.test.getBinaryName());
                if (claim(name, spec.test)) {
                    sources.put(name, context(spec));
                }
            }
            if (ctx.hasErrors()) {
                return 0;
            }
        }
        int count = specs.size();
        if (compiled) {
            Map<String, Spec> byTest = new LinkedHashMap<String, Spec>();
            for (Spec spec : specs) {
                byTest.put(spec.test.getInternalName(), spec);
            }
            for (String name : new TreeSet<String>(testClasses)) {
                AnnotatedClass cls = ctx.lookup(name);
                if (cls == null || (selection != null && !selection.test(cls.getBinaryName()))) {
                    // Not a class the JVM run discovers -- a helper with an @Test
                    // method, an IntegrationSpec the includes leave out -- so not one
                    // the compiled run runs either.
                    continue;
                }
                if (runner(cls, byTest.get(name))) {
                    count++;
                }
            }
            accessHelperSources();
            sources.put(MAIN_CLASS, mainSource());
        }
        if (ctx.hasErrors() || sources.isEmpty()) {
            return 0;
        }
        compile();
        return count;
    }

    // ---------------------------------------------------------------- specs

    private Spec spec(AnnotatedClass cls) {
        Spec spec = new Spec();
        spec.test = cls;
        AnnotatedClass declaring = backendTestOf(cls);
        AnnotationValues a = declaring.getClassAnnotation(BACKEND_TEST);
        Object env = a.get("webEnvironment");
        String envName = BackendBeans.enumName(env, "MOCK");
        for (int i = 0; i < WEB_ENVIRONMENTS.length; i++) {
            if (WEB_ENVIRONMENTS[i].equals(envName)) {
                spec.webEnvironment = i;
            }
        }
        spec.profile = a.getStringOrDefault("profile", "test").trim();
        Object props = a.get("properties");
        if (props instanceof List) {
            for (Object o : (List<?>) props) {
                String p = String.valueOf(o);
                int eq = p.indexOf('=');
                if (eq <= 0) {
                    ctx.error(cls, "@BackendTest property \"" + p + "\" is not key=value.");
                    return null;
                }
                spec.properties.add(p.substring(0, eq).trim());
                spec.properties.add(p.substring(eq + 1).trim());
            }
        }
        Object listed = a.get("classes");
        if (listed instanceof List) {
            for (Object o : (List<?>) listed) {
                if (o instanceof Type) {
                    String internal = ((Type) o).getInternalName();
                    AnnotatedClass config = ctx.lookup(internal);
                    if (config == null || config.getClassAnnotation(TEST_CONFIGURATION) == null) {
                        ctx.error(cls, "@BackendTest(classes) lists " + internal.replace('/', '.')
                                + ", which is not a @TestConfiguration of this module's tests.");
                        return null;
                    }
                    spec.configurations.add(internal);
                }
            }
        }
        // Static nested @TestConfiguration classes apply to their test, as in Spring
        // -- the class's own, and those of the base class that carries @BackendTest.
        for (AnnotatedClass owner : declaring == cls
                ? Collections.singletonList(cls) : java.util.Arrays.asList(cls, declaring)) {
            String prefix = owner.getInternalName() + "$";
            for (String name : new TreeSet<String>(testClasses)) {
                AnnotatedClass nested = ctx.lookup(name);
                // Directly nested only, as Spring: OuterTest$Nested$Config is the
                // nested test's configuration, not the outer test's.
                if (name.startsWith(prefix) && name.indexOf('$', prefix.length()) < 0
                        && nested != null
                        && nested.getClassAnnotation(TEST_CONFIGURATION) != null
                        && !spec.configurations.contains(name)) {
                    spec.configurations.add(name);
                }
            }
        }
        for (AnnotatedClass c : hierarchy(cls)) {
            for (FieldInfo f : c.getFields()) {
                if (f.getAnnotation(MOCKITO_BEAN) != null) {
                    Type t = Type.getType(f.getDescriptor());
                    if (t.getSort() != Type.OBJECT) {
                        ctx.error(cls, "@MockitoBean " + f.getName() + " is a " + t.getClassName()
                                + "; only an object type can be mocked.");
                        return null;
                    }
                    spec.mocks.add(new String[] {f.getName(), t.getInternalName()});
                }
            }
        }
        StringBuilder key = new StringBuilder();
        key.append(spec.configurations).append('|').append(spec.properties).append('|')
           .append(spec.profile).append('|').append(spec.webEnvironment).append('|');
        List<String> mockTypes = new ArrayList<String>();
        for (String[] m : spec.mocks) {
            mockTypes.add(m[1]);
        }
        Collections.sort(mockTypes);
        key.append(mockTypes);
        spec.key = key.toString();
        return spec;
    }

    /// The class in `cls`'s hierarchy that carries `@BackendTest` -- `cls` itself or
    /// the nearest superclass, since the annotation is inherited -- or null.
    private AnnotatedClass backendTestOf(AnnotatedClass cls) {
        for (AnnotatedClass c : hierarchy(cls)) {
            if (c.getClassAnnotation(BACKEND_TEST) != null) {
                return c;
            }
        }
        return null;
    }

    /// The class and its superclasses that the build can see, subclass first.
    private List<AnnotatedClass> hierarchy(AnnotatedClass cls) {
        List<AnnotatedClass> out = new ArrayList<AnnotatedClass>();
        AnnotatedClass c = cls;
        while (c != null && out.size() < 32) {
            out.add(c);
            String sup = c.getSuperInternalName();
            c = sup == null || "java/lang/Object".equals(sup) ? null : ctx.lookup(sup);
        }
        return out;
    }

    // --------------------------------------------------------- wiring record

    private void readWiringRecord() throws ProcessingException {
        File record = new File(mainOut, RestControllerAnnotationProcessor.WIRING_RESOURCE);
        if (!record.isFile()) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(record.toPath(), StandardCharsets.UTF_8);
        } catch (IOException err) {
            throw new ProcessingException("Could not read " + record + ": " + err.getMessage(), err);
        }
        for (String line : lines) {
            String[] f = line.split("\t", -1);
            if (f.length >= 2 && "package".equals(f[0])) {
                entryPackage = f[1];
            } else if (f.length >= 3 && "router".equals(f[0])) {
                routers.add(new BackendWiringWriter.Router(f[1], f[2]));
            } else if (f.length >= 3 && "socket".equals(f[0])) {
                sockets.put(f[1], f[2]);
            } else if (f.length >= 5 && "route".equals(f[0])) {
                routes.add(new String[] {f[1], f[2], f[3], f[4]});
            }
        }
    }

    // -------------------------------------------------------------- contexts

    private void resolveContexts(List<Spec> specs) throws ProcessingException {
        Map<String, List<Spec>> byKey = new LinkedHashMap<String, List<Spec>>();
        for (Spec spec : specs) {
            List<Spec> group = byKey.get(spec.key);
            if (group == null) {
                group = new ArrayList<Spec>();
                byKey.put(spec.key, group);
            }
            group.add(spec);
        }
        Set<String> wiringNames = new LinkedHashSet<String>();
        for (List<Spec> group : byKey.values()) {
            Spec first = group.get(0);
            Set<String> scope = new LinkedHashSet<String>(mainClasses);
            scope.addAll(first.configurations);
            Set<String> weaveable = new LinkedHashSet<String>(first.configurations);
            for (Spec spec : group) {
                for (AnnotatedClass c : hierarchy(spec.test)) {
                    if (testClasses.contains(c.getInternalName())) {
                        weaveable.add(c.getInternalName());
                    }
                }
            }
            BackendBeans model = BackendBeans.forTests(ctx, scope, first.configurations,
                    first.mocks, weaveable, entryPackage);
            if (ctx.hasErrors()) {
                return;
            }
            // The test classes' own fields, which the beans pass does not know:
            // the port, the mocks and the clients a test asks for.
            for (Spec spec : group) {
                for (AnnotatedClass c : hierarchy(spec.test)) {
                    if (!testClasses.contains(c.getInternalName())) {
                        continue;
                    }
                    for (FieldInfo f : c.getFields()) {
                        if (f.isStatic() || f.isFinal()) {
                            continue;
                        }
                        if (f.getAnnotation(LOCAL_SERVER_PORT) != null
                                || f.getAnnotation(MOCKITO_BEAN) != null
                                || f.getAnnotation(BackendBeans.AUTOWIRED) != null) {
                            BackendWeaver.Plan plan = model.plans.get(c.getInternalName());
                            if (plan == null) {
                                plan = new BackendWeaver.Plan(c.getInternalName(), null);
                                model.plans.put(c.getInternalName(), plan);
                            }
                            plan.injectFields.put(f.getName(), f.getDescriptor());
                        }
                    }
                }
            }
            model.weaveTests(ctx.getOutputClassDir());
            String pkg = entryPackage != null ? entryPackage : model.entryPackage == null ? ""
                    : model.entryPackage;
            String base = first.test.getBinaryName();
            base = base.substring(base.lastIndexOf('.') + 1).replace('$', '_') + "Cn1TestWiring";
            String simple = base;
            for (int n = 2; !wiringNames.add(simple); n++) {
                simple = base + n;
            }
            String binary = BackendBeans.qualify(pkg, simple);
            sources.put(binary, new BackendWiringWriter(model).write(pkg, simple, routers, sockets,
                    routes, true));
            for (Spec spec : group) {
                spec.wiringBinary = binary;
                spec.model = model;
            }
        }
    }

    /// `pkg.Name` to `pkg.NameCn1TestContext`, a nested class's `$` written `_`.
    /// Generated class -> the test class it was named after, for [#claim].
    private final Map<String, String> generatedNames = new TreeMap<String, String>();

    /// Records that `generated` is named after `test`, or reports a build error
    /// when another test class already has that name. `$` becomes `_` in a
    /// generated name, so a nested `Outer.Inner` and a top-level `Outer_Inner` in
    /// one package would otherwise both write `Outer_InnerCn1TestContext`, and one
    /// would silently load the other's context. The JVM extension derives the same
    /// name to find a context, so the scheme stays and the collision is refused.
    private boolean claim(String generated, AnnotatedClass test) {
        String previous = generatedNames.get(generated);
        if (previous == null) {
            generatedNames.put(generated, test.getBinaryName());
            return true;
        }
        if (previous.equals(test.getBinaryName())) {
            return true;
        }
        ctx.error(test, test.getSourceName() + " and " + previous.replace('$', '.')
                + " would both generate " + generated + "; rename one of the two test classes.");
        return false;
    }

    static String contextBinary(String testBinary) {
        int dot = testBinary.lastIndexOf('.');
        String pkg = dot < 0 ? "" : testBinary.substring(0, dot + 1);
        return pkg + testBinary.substring(dot + 1).replace('$', '_') + "Cn1TestContext";
    }

    private String context(Spec spec) {
        String binary = contextBinary(spec.test.getBinaryName());
        int dot = binary.lastIndexOf('.');
        String pkg = dot < 0 ? "" : binary.substring(0, dot);
        String simple = binary.substring(dot + 1);
        String testType = spec.test.getSourceName();
        StringBuilder sb = new StringBuilder();
        if (pkg.length() > 0) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("// Generated from @BackendTest ").append(spec.test.getBinaryName())
          .append(". Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(simple)
          .append(" implements com.codename1.impl.backend.test.TestContext {\n\n");
        sb.append("    public String key() {\n        return ").append(quote(spec.key))
          .append(";\n    }\n\n");
        BackendSettings settings = BackendSettings.resolve(ctx);
        sb.append("    public String[] compiledSettings() {\n        return ")
          .append(array(settings.flat())).append(";\n    }\n\n");
        List<String> profileSettings = new ArrayList<String>();
        Properties profileFile = RestControllerAnnotationProcessor.profileApplicationProperties(
                ctx, spec.profile);
        if (profileFile != null) {
            for (String k : new TreeSet<String>(profileFile.stringPropertyNames())) {
                profileSettings.add(k);
                profileSettings.add(profileFile.getProperty(k));
            }
        }
        sb.append("    public String[] profileSettings() {\n        return ")
          .append(array(profileSettings)).append(";\n    }\n\n");
        sb.append("    public String[] properties() {\n        return ")
          .append(array(spec.properties)).append(";\n    }\n\n");
        sb.append("    public String profile() {\n        return ").append(quote(spec.profile))
          .append(";\n    }\n\n");
        sb.append("    public int webEnvironment() {\n        return ").append(spec.webEnvironment)
          .append(";\n    }\n\n");
        sb.append("    public boolean requiresDataSource() {\n        return ")
          .append(spec.model.needsDatabase).append(";\n    }\n\n");
        sb.append("    public void prepare() {\n");
        if (ctx.lookup(OrmAnnotationProcessor.BACKEND_BOOTSTRAP_BINARY.replace('.', '/')) != null
                || RestControllerAnnotationProcessor.resolveClass(ctx,
                OrmAnnotationProcessor.BACKEND_BOOTSTRAP_BINARY.replace('.', '/')) != null) {
            // What the generated main does first: the daos register here.
            sb.append("        new ").append(OrmAnnotationProcessor.BACKEND_BOOTSTRAP_BINARY)
              .append("();\n");
        }
        sb.append("    }\n\n");
        sb.append("    public com.codename1.impl.backend.BackendApplication createApplication() {\n")
          .append("        return new ").append(spec.wiringBinary).append("();\n    }\n\n");
        List<String> mockNames = new ArrayList<String>();
        for (BackendBeans.Bean b : spec.model.beans) {
            if (b.mockType != null) {
                mockNames.add(b.name);
            }
        }
        sb.append("    public String[] mockBeans() {\n        return ").append(array(mockNames))
          .append(";\n    }\n\n");
        sb.append("    public void inject(Object test, com.codename1.impl.backend.test.TestEnvironment "
                + "environment) throws Exception {\n");
        sb.append("        if (!(test instanceof ").append(testType).append(")) {\n")
          .append("            throw new IllegalArgumentException(\"Not a ").append(testType)
          .append(": \" + test);\n        }\n");
        sb.append("        ").append(testType).append(" t = (").append(testType).append(") test;\n");
        Set<String> injectedNames = new LinkedHashSet<String>();
        for (AnnotatedClass c : hierarchy(spec.test)) {
            for (FieldInfo f : c.getFields()) {
                if (f.isStatic() || f.isFinal()) {
                    continue;
                }
                String value = injection(spec, c, f);
                if (value == null) {
                    continue;
                }
                if (!injectedNames.add(f.getName())) {
                    // The rule the main build applies to beans, for the same reason:
                    // a field is injected through a public setter named after it, so
                    // two of one name in a hierarchy are one virtual method -- the
                    // subclass's field was set twice and the superclass's never.
                    ctx.error(c, c.getSourceName() + "." + f.getName() + " has the name of "
                            + "another injected field of " + spec.test.getSourceName()
                            + "'s class hierarchy, and the build injects a field through a "
                            + "setter named after it. Rename one.");
                    continue;
                }
                sb.append("        t.").append(BackendWeaver.INJECT_PREFIX).append(f.getName())
                  .append('(').append(value).append(");\n");
            }
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    /// The expression a test field is filled with, or null when it is not injected.
    private String injection(Spec spec, AnnotatedClass owner, FieldInfo f) {
        Type t = Type.getType(f.getDescriptor());
        String where = owner.getSourceName() + "." + f.getName();
        if (f.getAnnotation(LOCAL_SERVER_PORT) != null) {
            if (t.getSort() != Type.INT) {
                ctx.error(owner, "@LocalServerPort " + where + " is a " + t.getClassName()
                        + "; a port is an int.");
                return null;
            }
            return "environment.port()";
        }
        boolean mock = f.getAnnotation(MOCKITO_BEAN) != null;
        AnnotationValues autowired = f.getAnnotation(BackendBeans.AUTOWIRED);
        if (!mock && autowired == null) {
            return null;
        }
        if (t.getSort() != Type.OBJECT) {
            ctx.error(owner, where + " is a " + t.getClassName() + ", which no bean is.");
            return null;
        }
        String type = t.getInternalName();
        String source = t.getClassName().replace('$', '.');
        if (!mock) {
            if ("com/codename1/backend/test/MockMvc".equals(type)) {
                return "environment.mockMvc()";
            }
            if ("com/codename1/backend/test/TestRestTemplate".equals(type)) {
                return "environment.restTemplate()";
            }
            if ("com/codename1/backend/Backend".equals(type)) {
                return "environment.backend()";
            }
            if (BackendBeans.CONFIG_TYPE.equals(type)) {
                return "environment.backend().getConfig()";
            }
            if (BackendBeans.DATASOURCE_TYPE.equals(type)) {
                return "environment.backend().getDataSource()";
            }
            if (BackendBeans.ENTITIES_TYPE.equals(type)) {
                return "environment.backend().getEntityManager()";
            }
        }
        AnnotationValues qualifier = f.getAnnotation(BackendBeans.QUALIFIER);
        String wanted = qualifier == null ? null : qualifier.getStringOrDefault("value", "");
        List<BackendBeans.Bean> candidates = new ArrayList<BackendBeans.Bean>();
        for (BackendBeans.Bean b : spec.model.beans) {
            if (b.types.contains(type) && (!mock || b.mockType != null)
                    && (wanted == null || wanted.length() == 0 || wanted.equals(b.name))) {
                candidates.add(b);
            }
        }
        BackendBeans.Bean chosen = null;
        if (candidates.size() == 1) {
            chosen = candidates.get(0);
        } else {
            for (BackendBeans.Bean b : candidates) {
                if (b.primary) {
                    chosen = chosen == null ? b : null;
                }
            }
        }
        if (chosen == null) {
            boolean required = autowired == null || autowired.getBoolOrDefault("required", true);
            if (candidates.isEmpty() && !required) {
                return null;
            }
            ctx.error(owner, where + " needs a " + source + ", and "
                    + (candidates.isEmpty() ? "no bean of this test's application has that type."
                    : candidates.size() + " beans could fill it; name one with @Qualifier or mark "
                    + "one @Primary."));
            return null;
        }
        return "(" + source + ") environment.bean(" + quote(chosen.name) + ")";
    }

    // --------------------------------------------------------------- runners

    /// Writes the compiled runner of one test class; false when it has no tests.
    private boolean runner(AnnotatedClass cls, Spec spec) {
        if (cls.isAbstract() || cls.isInterface() || cls.isSynthetic()) {
            return false;
        }
        List<AnnotatedClass> chain = methodHierarchy(cls);
        List<MethodInfo> tests = new ArrayList<MethodInfo>();
        Overrides seen = new Overrides();
        java.util.IdentityHashMap<MethodInfo, AnnotatedClass> owners =
                new java.util.IdentityHashMap<MethodInfo, AnnotatedClass>();
        for (AnnotatedClass c : chain) {
            for (MethodInfo m : c.getMethods()) {
                if (m.getAnnotation(JUNIT_TEST) != null) {
                    // -Dtest=Class#method narrows the JVM run to that method; the
                    // compiled run runs the same ones.
                    if (!seen.covers(c, m) && (selection == null
                            || selection.test(cls.getBinaryName() + "#" + m.getName()))) {
                        tests.add(m);
                        owners.put(m, c);
                    }
                }
                // An override without @Test is not a test, and it hides the
                // superclass's: JUnit runs neither. Collected anyway, the
                // generated call would dispatch to the override.
                seen.declare(c, m);
            }
        }
        if (tests.isEmpty()) {
            return false;
        }
        String testType = cls.getSourceName();
        String binary = cls.getBinaryName();
        String runnerBinary = binary.replace('$', '_') + "Cn1TestRunner";
        if (!claim(runnerBinary, cls)) {
            return false;
        }
        int dot = runnerBinary.lastIndexOf('.');
        String pkg = dot < 0 ? "" : runnerBinary.substring(0, dot);
        String simple = runnerBinary.substring(dot + 1);
        String skip = null;
        if (cls.getClassAnnotation(JUNIT_DISABLED) != null) {
            skip = "disabled: " + cls.getClassAnnotation(JUNIT_DISABLED).getStringOrDefault("value", "");
        } else if (cls.getClassAnnotation(JUNIT_NESTED) != null || isInner(cls)) {
            skip = "a @Nested or inner test class does not run in a compiled test";
        } else if (spec != null && !spec.mocks.isEmpty()) {
            skip = "@MockitoBean needs Mockito, which runs only on the JVM";
        } else if (!hasNoArgConstructor(cls)) {
            skip = "the class has no constructor without arguments";
        }
        StringBuilder sb = new StringBuilder();
        if (pkg.length() > 0) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("// Generated from the tests of ").append(binary).append(". Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(simple).append(" {\n");
        sb.append("    private static final String CLS = ").append(quote(binary)).append(";\n\n");
        sb.append("    private ").append(simple).append("() {\n    }\n\n");
        sb.append("    public static void run() {\n");
        if (skip != null) {
            for (MethodInfo m : tests) {
                sb.append("        com.codename1.impl.backend.test.TestRun.skipped(CLS, ")
                  .append(quote(m.getName())).append(", ").append(quote(skip)).append(");\n");
            }
            sb.append("    }\n}\n");
            sources.put(runnerBinary, sb.toString());
            skippedClasses.put(binary, skip);
            runners.add(runnerBinary);
            return true;
        }
        List<MethodInfo> beforeAll = lifecycle(chain, JUNIT_BEFORE_ALL, true, true, owners);
        List<MethodInfo> afterAll = lifecycle(chain, JUNIT_AFTER_ALL, true, false, owners);
        sb.append("        boolean ready = true;\n");
        sb.append("        String notReady = \"@BeforeAll failed\";\n");
        for (MethodInfo m : beforeAll) {
            // An assumption that does not hold aborts the class, as JUnit aborts the
            // container: its tests are skipped, not failed, so an environment-gated
            // class has the same result in both runs.
            sb.append("        if (ready) {\n            try {\n                ")
              .append(call(owners.get(m), m, pkg, testType)).append(";\n")
              .append("            } catch (Throwable err) {\n")
              .append("                if (com.codename1.impl.backend.test.TestRun.isAbort(err)) {\n")
              .append("                    notReady = ").append(quote("@BeforeAll " + m.getName() + " aborted: "))
              .append(" + err.getMessage();\n")
              .append("                } else {\n")
              .append("                    com.codename1.impl.backend.test.TestRun.lifecycleFailed(CLS, ")
              .append(quote("@BeforeAll " + m.getName())).append(", err);\n")
              .append("                }\n")
              .append("                ready = false;\n            }\n        }\n");
        }
        int index = 0;
        for (MethodInfo m : tests) {
            AnnotationValues disabled = m.getAnnotation(JUNIT_DISABLED);
            if (disabled != null) {
                sb.append("        com.codename1.impl.backend.test.TestRun.skipped(CLS, ")
                  .append(quote(m.getName())).append(", ")
                  .append(quote("disabled: " + disabled.getStringOrDefault("value", "")))
                  .append(");\n");
            } else {
                sb.append("        if (ready) {\n            t").append(index).append("();\n")
                  .append("        } else {\n")
                  .append("            com.codename1.impl.backend.test.TestRun.skipped(CLS, ")
                  .append(quote(m.getName())).append(", notReady);\n        }\n");
            }
            index++;
        }
        for (MethodInfo m : afterAll) {
            sb.append("        try {\n            ").append(call(owners.get(m), m, pkg, testType))
              .append(";\n        } catch (Throwable err) {\n")
              .append("            com.codename1.impl.backend.test.TestRun.lifecycleFailed(CLS, ")
              .append(quote("@AfterAll " + m.getName())).append(", err);\n        }\n");
        }
        sb.append("    }\n\n");
        List<MethodInfo> beforeEach = lifecycle(chain, JUNIT_BEFORE_EACH, false, true, owners);
        List<MethodInfo> afterEach = lifecycle(chain, JUNIT_AFTER_EACH, false, false, owners);
        index = 0;
        for (MethodInfo m : tests) {
            checkCallable(cls, m, false);
            sb.append("    private static void t").append(index++).append("() {\n");
            sb.append("        long started = com.codename1.impl.backend.test.TestRun.started();\n");
            sb.append("        ").append(testType).append(" test = null;\n");
            sb.append("        Throwable failure = null;\n");
            sb.append("        try {\n");
            sb.append("            test = new ").append(testType).append("();\n");
            if (spec != null) {
                sb.append("            ").append(contextBinary(binary)).append(" context = new ")
                  .append(contextBinary(binary)).append("();\n");
                sb.append("            context.inject(test, com.codename1.impl.backend.test.TestContexts"
                        + ".acquire(context));\n");
            }
            for (MethodInfo b : beforeEach) {
                sb.append("            ").append(call(owners.get(b), b, pkg, "test")).append(";\n");
            }
            sb.append("            ").append(call(owners.get(m), m, pkg, "test")).append(";\n");
            sb.append("        } catch (Throwable err) {\n            failure = err;\n        }\n");
            if (!afterEach.isEmpty()) {
                sb.append("        if (test != null) {\n");
                for (MethodInfo a : afterEach) {
                    sb.append("            try {\n                ").append(call(owners.get(a), a, pkg, "test"))
                      .append(";\n            } catch (Throwable err) {\n")
                      // A real teardown failure fails the test even after an
                      // assumption aborted it, as JUnit reports it.
                      .append("                if (failure == null || (com.codename1.impl.backend.test.TestRun"
                              + ".isAbort(failure) && !com.codename1.impl.backend.test.TestRun.isAbort(err))) {\n")
                      .append("                    failure = err;\n                }\n")
                      .append("            }\n");
                }
                sb.append("        }\n");
            }
            sb.append("        com.codename1.impl.backend.test.TestRun.finished(CLS, ")
              .append(quote(m.getName())).append(", started, failure);\n    }\n\n");
        }
        sb.append("}\n");
        sources.put(runnerBinary, sb.toString());
        runners.add(runnerBinary);
        return true;
    }

    /// The lifecycle methods of a kind across the hierarchy: superclass first for
    /// a "before", subclass first for an "after", as JUnit orders them.
    private List<MethodInfo> lifecycle(List<AnnotatedClass> chain, String annotation,
                                       boolean isStatic, boolean superFirst,
                                       java.util.Map<MethodInfo, AnnotatedClass> owners) {
        List<MethodInfo> out = new ArrayList<MethodInfo>();
        Overrides seen = new Overrides();
        for (AnnotatedClass c : chain) {
            List<MethodInfo> level = new ArrayList<MethodInfo>();
            for (MethodInfo m : c.getMethods()) {
                if (m.getAnnotation(annotation) == null) {
                    // As for tests: an unannotated override or hiding method is
                    // what JUnit sees, and it is not a lifecycle method.
                    seen.declare(c, m);
                    continue;
                }
                boolean covered = seen.covers(c, m);
                seen.declare(c, m);
                if (!covered) {
                    owners.put(m, c);
                    if (m.isStatic() != isStatic) {
                        ctx.error(c, m.getName() + " is " + (isStatic ? "not " : "")
                                + "static; " + annotation.substring(annotation.lastIndexOf('/') + 1,
                                annotation.length() - 1) + " methods are "
                                + (isStatic ? "static." : "instance methods."));
                        continue;
                    }
                    checkCallable(c, m, isStatic);
                    level.add(m);
                }
            }
            if (superFirst) {
                out.addAll(0, level);
            } else {
                out.addAll(level);
            }
        }
        return out;
    }

    /// The class, its superclasses and every interface they implement, for the
    /// methods JUnit collects: classes subclass first, then the interfaces, whose
    /// default `@Test` and lifecycle methods a test class inherits. Interfaces come
    /// last because a class's method overrides theirs, and so that a "before" from
    /// an interface runs ahead of the class's, as JUnit orders them.
    private List<AnnotatedClass> methodHierarchy(AnnotatedClass cls) {
        List<AnnotatedClass> out = new ArrayList<AnnotatedClass>(hierarchy(cls));
        Set<String> visited = new LinkedHashSet<String>();
        java.util.ArrayDeque<String> pending = new java.util.ArrayDeque<String>();
        for (AnnotatedClass c : out) {
            pending.addAll(c.getInterfaceInternalNames());
        }
        List<AnnotatedClass> interfaces = new ArrayList<AnnotatedClass>();
        while (!pending.isEmpty() && visited.size() < 64) {
            String name = pending.removeFirst();
            AnnotatedClass i = visited.add(name) ? ctx.lookup(name) : null;
            if (i != null) {
                interfaces.add(i);
                pending.addAll(i.getInterfaceInternalNames());
            }
        }
        out.addAll(interfaces);
        return out;
    }

    /// The methods declared lower in a hierarchy, by signature, and what they
    /// override or hide from JUnit when they carry no annotation of their own.
    private static final class Overrides {
        private final Map<String, List<String>> packages = new java.util.HashMap<String, List<String>>();

        /// Records `m`, declared by `owner`: any method but a private one, a
        /// constructor or a synthetic bridge can override or hide.
        void declare(AnnotatedClass owner, MethodInfo m) {
            if (m.isPrivate() || m.isConstructor() || m.isSynthetic()) {
                return;
            }
            String signature = m.getName() + m.getDescriptor();
            List<String> pkgs = packages.get(signature);
            if (pkgs == null) {
                pkgs = new ArrayList<String>();
                packages.put(signature, pkgs);
            }
            pkgs.add(packageOf(owner.getBinaryName()));
        }

        /// Whether a method recorded so far overrides or hides `m`, declared higher
        /// up by `owner`. A public or protected one is covered by any; a
        /// package-private one only from its own package -- Java's rule, so a
        /// same-signature method in a subclass elsewhere does not override it, and
        /// JUnit still runs the base class's test.
        boolean covers(AnnotatedClass owner, MethodInfo m) {
            List<String> pkgs = packages.get(m.getName() + m.getDescriptor());
            if (pkgs == null || m.isPrivate()) {
                return false;
            }
            if (m.isPublic() || m.isProtected() || owner.isInterface()) {
                return true;
            }
            return pkgs.contains(packageOf(owner.getBinaryName()));
        }
    }

    /// The call to `m`, declared by `owner`, from a runner in package `runnerPkg`:
    /// `target.m()`, or `Type.m()` for a static one. A method that is not public
    /// and is declared in another package -- a JUnit base class's package-private
    /// or protected test -- cannot be called from there, so it goes through a
    /// helper generated in its own package; JUnit reaches it reflectively, which
    /// the compiled run cannot.
    private String call(AnnotatedClass owner, MethodInfo m, String runnerPkg, String target) {
        String ownerPkg = packageOf(owner.getBinaryName());
        if (m.isPublic() || ownerPkg.equals(runnerPkg)) {
            return (m.isStatic() ? owner.getSourceName() : target) + "." + m.getName() + "()";
        }
        String helper = (ownerPkg.length() == 0 ? "" : ownerPkg + ".")
                + owner.getBinaryName().substring(owner.getBinaryName().lastIndexOf('.') + 1)
                        .replace('$', '_') + "Cn1TestAccess";
        java.util.Map<String, String> calls = accessHelpers.get(helper);
        if (calls == null) {
            calls = new TreeMap<String, String>();
            accessHelpers.put(helper, calls);
        }
        String body = m.isStatic()
                ? "    public static void " + m.getName() + "() throws Throwable {\n        "
                        + owner.getSourceName() + "." + m.getName() + "();\n    }\n"
                : "    public static void " + m.getName() + "(" + owner.getSourceName()
                        + " target) throws Throwable {\n        target." + m.getName() + "();\n    }\n";
        calls.put(m.getName() + (m.isStatic() ? "/static" : ""), body);
        return helper + "." + m.getName() + "(" + (m.isStatic() ? "" : target) + ")";
    }

    private static String packageOf(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        return dot < 0 ? "" : binaryName.substring(0, dot);
    }

    /// The access helpers [#call] asked for, as their sources.
    private void accessHelperSources() {
        for (java.util.Map.Entry<String, java.util.Map<String, String>> e : accessHelpers.entrySet()) {
            String binary = e.getKey();
            int dot = binary.lastIndexOf('.');
            StringBuilder sb = new StringBuilder();
            if (dot > 0) {
                sb.append("package ").append(binary, 0, dot).append(";\n\n");
            }
            sb.append("// Calls the compiled test runners cannot make from their own package. Do not edit.\n");
            sb.append("@com.codename1.backend.annotations.Generated\n");
            sb.append("public final class ").append(binary.substring(dot + 1)).append(" {\n");
            sb.append("    private ").append(binary.substring(dot + 1)).append("() {\n    }\n\n");
            for (String body : e.getValue().values()) {
                sb.append(body).append('\n');
            }
            sb.append("}\n");
            sources.put(binary, sb.toString());
        }
    }

    /// A test or lifecycle method the runner can call: not private, taking nothing.
    private void checkCallable(AnnotatedClass owner, MethodInfo m, boolean isStatic) {
        if (m.isPrivate()) {
            ctx.error(owner, owner.getSourceName() + "." + m.getName() + " is private; a test "
                    + "method is package-private or public, as JUnit requires.");
        }
        if (!m.getDescriptor().startsWith("()")) {
            ctx.error(owner, owner.getSourceName() + "." + m.getName() + " takes parameters, "
                    + "which a compiled test cannot supply.");
        }
    }

    private boolean isInner(AnnotatedClass cls) {
        for (FieldInfo f : cls.getFields()) {
            if (f.getName().startsWith("this$")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNoArgConstructor(AnnotatedClass cls) {
        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() && "()V".equals(m.getDescriptor()) && !m.isPrivate()) {
                return true;
            }
        }
        return false;
    }

    private String mainSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("package cn1app;\n\n");
        sb.append("// Generated: runs every compiled backend test. Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class BackendTestMain {\n");
        sb.append("    private BackendTestMain() {\n    }\n\n");
        sb.append("    public static void main(String[] args) {\n");
        for (String r : runners) {
            sb.append("        ").append(r).append(".run();\n");
        }
        sb.append("        com.codename1.impl.backend.test.TestContexts.shutdown();\n");
        sb.append("        int failures = com.codename1.impl.backend.test.TestRun.finish();\n");
        sb.append("        System.exit(failures == 0 ? 0 : 1);\n");
        sb.append("    }\n}\n");
        return sb.toString();
    }

    // -------------------------------------------------------------- helpers

    private void compile() throws ProcessingException {
        List<File> cp = new ArrayList<File>();
        cp.add(ctx.getOutputClassDir());
        for (String element : ctx.getCompileClasspath()) {
            cp.add(new File(element));
        }
        try {
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException err) {
            throw new ProcessingException("Could not compile the generated test sources: "
                    + err.getMessage(), err);
        }
        ctx.getLog().info("cn1: generated " + sources.size() + " backend test class(es)"
                + (skippedClasses.isEmpty() ? "" : "; a compiled run skips "
                + skippedClasses.keySet()));
    }

    private static String array(List<String> values) {
        StringBuilder sb = new StringBuilder("new String[] {");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(quote(values.get(i)));
        }
        return sb.append('}').toString();
    }

    private static String quote(String value) {
        return BackendSources.quote(value);
    }
}
