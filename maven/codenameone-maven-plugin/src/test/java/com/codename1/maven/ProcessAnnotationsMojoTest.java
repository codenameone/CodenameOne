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

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// Drives `ProcessAnnotationsMojo` itself rather than a processor in isolation.
///
/// The distinction is the whole point of this class. The per-processor tests
/// build a class index and call `processClass` on every entry by hand, so they
/// pass whether or not the Mojo would ever have dispatched that class. That is
/// how a class annotated *only on a method* -- the documented static-factory
/// `@Route` form -- came to be silently skipped in real builds while the test
/// suite stayed green.
public class ProcessAnnotationsMojoTest {

    private static final String ROUTES_PATH =
            "com/codename1/router/generated/Routes.class";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /// The regression: a class whose only annotation is on a static factory
    /// method must still reach the processor when the Mojo drives the run.
    @Test
    public void dispatchesClassAnnotatedOnlyOnAMethod() throws Exception {
        File classes = compile("com.example.Routes",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "public class Routes {\n"
                        + "  @Route(\"/home\")\n"
                        + "  public static Form home() { return new Form(); }\n"
                        + "}\n");

        runMojo(classes);

        assertTrue("a method-level @Route must produce a route table; the Mojo "
                        + "skipped the class entirely",
                new File(classes, ROUTES_PATH).exists());
    }

    /// The class-level form has always worked; assert it still does, so the
    /// dispatch widening cannot regress it.
    @Test
    public void stillDispatchesClassLevelAnnotations() throws Exception {
        File classes = compile("com.example.ProfileForm",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "@Route(\"/users\")\n"
                        + "public class ProfileForm extends Form {\n"
                        + "  public ProfileForm() {}\n"
                        + "}\n");

        runMojo(classes);

        assertTrue(new File(classes, ROUTES_PATH).exists());
    }

    /// A class carrying no annotation anywhere must remain cheap to skip -- the
    /// empty-set check is what keeps the scan from dispatching every class in
    /// the project to every processor.
    @Test
    public void unannotatedClassHasNoDescriptors() throws Exception {
        File classes = compile("com.example.Plain",
                "package com.example;\n"
                        + "public class Plain {\n"
                        + "  public void doNothing() {}\n"
                        + "}\n");

        AnnotatedClass cls = scan(classes).get("com/example/Plain");
        assertNotNull(cls);
        assertTrue(cls.getAllAnnotationDescriptors().isEmpty());
    }

    /// The descriptor union must see every element kind a processor can declare
    /// interest in, not just the class and its methods.
    @Test
    public void descriptorUnionCoversMethodParameterAndField() throws Exception {
        File classes = compile("com.example.Mixed",
                "package com.example;\n"
                        + "import com.codename1.annotations.Route;\n"
                        + "import com.codename1.annotations.RouteParam;\n"
                        + "import com.codename1.annotations.JsonIgnore;\n"
                        + "import com.codename1.ui.Form;\n"
                        + "public class Mixed {\n"
                        + "  @JsonIgnore public String hidden;\n"
                        + "  @Route(\"/users/:id\")\n"
                        + "  public static Form profile(@RouteParam(\"id\") String id) {\n"
                        + "    return new Form();\n"
                        + "  }\n"
                        + "}\n");

        AnnotatedClass cls = scan(classes).get("com/example/Mixed");
        assertNotNull(cls);
        Set<String> all = cls.getAllAnnotationDescriptors();

        assertTrue("method annotation missing from the union",
                all.contains("Lcom/codename1/annotations/Route;"));
        assertTrue("parameter annotation missing from the union",
                all.contains("Lcom/codename1/annotations/RouteParam;"));
        assertTrue("field annotation missing from the union",
                all.contains("Lcom/codename1/annotations/JsonIgnore;"));
        assertTrue("the class itself carries no annotation, so the class-level "
                        + "map must stay empty -- that is the condition the old "
                        + "dispatch gate tripped over",
                cls.getClassAnnotations().isEmpty());
    }

    /// Widening the dispatch means processors now see classes they never used
    /// to. Several declare interest in member-level annotations (`@Id`,
    /// `@Column`, `@JsonProperty`, `@Bind`) that could previously never match,
    /// because the old gate only ever tested class-level annotations. Each of
    /// them early-returns when its class-level marker is absent, so a class
    /// carrying only member annotations must process cleanly rather than
    /// failing the build.
    @Test
    public void memberAnnotationsWithoutTheClassMarkerAreIgnored() throws Exception {
        File classes = compile("com.example.Loose",
                "package com.example;\n"
                        + "import com.codename1.annotations.Id;\n"
                        + "import com.codename1.annotations.Column;\n"
                        + "public class Loose {\n"
                        + "  @Id public long id;\n"
                        + "  @Column(name = \"n\") public String name;\n"
                        + "}\n");

        // The Mojo throws MojoFailureException when a processor reports an
        // error, so completing normally is the assertion.
        runMojo(classes);
    }

    // ------------------------------------------------------------------
    // Harness
    // ------------------------------------------------------------------

    private File compile(String fqn, String source) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(fqn, source),
                classes,
                Arrays.asList(testClassesDir()));
        return classes;
    }

    private Map<String, AnnotatedClass> scan(File classes) throws Exception {
        return ClassScanner.scan(classes);
    }

    /// Runs the real Mojo against a compiled output directory. `executeImpl` is
    /// what the Maven lifecycle ultimately calls, and it loads processors
    /// through `ServiceLoader` exactly as a real build does.
    private void runMojo(File classes) throws Exception {
        ProcessAnnotationsMojo mojo = new ProcessAnnotationsMojo();
        mojo.outputDirectory = classes;
        mojo.stubSourceDirectory = tmp.newFolder();
        mojo.setLog(new SystemStreamLog());
        mojo.executeImpl();
    }

    private static File testClassesDir() throws Exception {
        URL url = ProcessAnnotationsMojoTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
