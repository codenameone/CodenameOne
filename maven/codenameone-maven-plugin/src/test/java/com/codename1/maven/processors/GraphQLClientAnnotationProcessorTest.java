/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// End-to-end test for `GraphQLClientAnnotationProcessor`. Compiles a
/// `@GraphQLClient` fixture with a query and a subscription, runs the
/// processor, and asserts the generated impl chains
/// `GraphQL.execute` / `GraphQL.subscribe` and that the bootstrap
/// registers the interface with `GraphQLClients`.
public class GraphQLClientAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void emitsImplAndBootstrap() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.sw.HeroNameData",
                "package com.example.sw;\n"
                        + "import com.codename1.annotations.Mapped;\n"
                        + "@Mapped public class HeroNameData {\n"
                        + "    public String hero;\n"
                        + "    public HeroNameData() {}\n"
                        + "}\n");
        sources.put("com.example.sw.StarWarsApi",
                "package com.example.sw;\n"
                        + "import com.codename1.annotations.graphql.*;\n"
                        + "import com.codename1.annotations.rest.Header;\n"
                        + "import com.codename1.io.graphql.GraphQLResponse;\n"
                        + "import com.codename1.io.graphql.GraphQLSubscription;\n"
                        + "import com.codename1.util.OnComplete;\n"
                        + "@GraphQLClient(\"https://api/graphql\")\n"
                        + "public interface StarWarsApi {\n"
                        + "    @Query(\"query HeroName($episode: String) { hero(episode: $episode) { name } }\")\n"
                        + "    void heroName(@Var(\"episode\") String episode,\n"
                        + "                  @Header(\"Authorization\") String bearerToken,\n"
                        + "                  OnComplete<GraphQLResponse<HeroNameData>> callback);\n"
                        + "    @Subscription(\"subscription OnReview { reviewAdded { stars } }\")\n"
                        + "    GraphQLSubscription onReview(@Header(\"Authorization\") String bearerToken,\n"
                        + "                                 GraphQLSubscription.Handler<HeroNameData> handler);\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classes);
        if (ctx.hasErrors()) failProcessor(ctx);

        assertTrue("expected StarWarsApiImpl.class",
                new File(classes, "com/example/sw/StarWarsApiImpl.class").exists());
        assertTrue("expected GraphQLClientBootstrap.class",
                new File(classes, "cn1app/GraphQLClientBootstrap.class").exists());

        String implSrc = generateImplSourceForFixture(classes);
        assertTrue("impl should call GraphQL.execute; was:\n" + implSrc,
                implSrc.contains("com.codename1.io.graphql.GraphQL.execute"));
        assertTrue("impl should call GraphQL.subscribe; was:\n" + implSrc,
                implSrc.contains("return com.codename1.io.graphql.GraphQL.subscribe"));
        assertTrue("impl should build the variables map",
                implSrc.contains("_vars.put(\"episode\""));
        assertTrue("impl should embed the operation document",
                implSrc.contains("hero(episode: $episode)"));
        assertTrue("impl should reference the response data class",
                implSrc.contains("com.example.sw.HeroNameData.class"));

        String bootSrc = generateBootstrapSourceForFixture(classes);
        assertTrue("bootstrap should register StarWarsApi; was:\n" + bootSrc,
                bootSrc.contains("GraphQLClients.register(com.example.sw.StarWarsApi.class"));
        assertTrue("bootstrap should instantiate StarWarsApiImpl",
                bootSrc.contains("new com.example.sw.StarWarsApiImpl(endpoint)"));
    }

    @Test
    public void rejectsMethodWithoutOperation() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.x.Data",
                "package com.example.x;\n"
                        + "import com.codename1.annotations.Mapped;\n"
                        + "@Mapped public class Data { public Data() {} }\n");
        sources.put("com.example.x.Bad",
                "package com.example.x;\n"
                        + "import com.codename1.annotations.graphql.GraphQLClient;\n"
                        + "import com.codename1.io.graphql.GraphQLResponse;\n"
                        + "import com.codename1.util.OnComplete;\n"
                        + "@GraphQLClient\n"
                        + "public interface Bad {\n"
                        + "    void unmarked(OnComplete<GraphQLResponse<Data>> cb);\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected error on method without @Query/@Mutation/@Subscription", ctx.hasErrors());
    }

    @Test
    public void rejectsGraphQLClientOnConcreteClass() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Bad",
                        "package com.example;\n"
                                + "import com.codename1.annotations.graphql.GraphQLClient;\n"
                                + "@GraphQLClient public class Bad {}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected error on @GraphQLClient applied to a class", ctx.hasErrors());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        GraphQLClientAnnotationProcessor proc = new GraphQLClientAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    private String generateImplSourceForFixture(File classesDir) throws Exception {
        GraphQLClientAnnotationProcessor proc = primedProcessor(classesDir);
        java.lang.reflect.Field accepted = GraphQLClientAnnotationProcessor.class
                .getDeclaredField("accepted");
        accepted.setAccessible(true);
        java.util.TreeMap<?, ?> map = (java.util.TreeMap<?, ?>) accepted.get(proc);
        Object api = map.values().iterator().next();
        java.lang.reflect.Method m = GraphQLClientAnnotationProcessor.class
                .getDeclaredMethod("generateImplSource",
                        Class.forName("com.codename1.maven.processors."
                                + "GraphQLClientAnnotationProcessor$GraphQLApi"));
        m.setAccessible(true);
        return (String) m.invoke(null, api);
    }

    private String generateBootstrapSourceForFixture(File classesDir) throws Exception {
        GraphQLClientAnnotationProcessor proc = primedProcessor(classesDir);
        java.lang.reflect.Field accepted = GraphQLClientAnnotationProcessor.class
                .getDeclaredField("accepted");
        accepted.setAccessible(true);
        java.util.TreeMap<?, ?> map = (java.util.TreeMap<?, ?>) accepted.get(proc);
        java.lang.reflect.Method m = GraphQLClientAnnotationProcessor.class
                .getDeclaredMethod("generateBootstrapSource", Iterable.class);
        m.setAccessible(true);
        return (String) m.invoke(null, map.values());
    }

    private GraphQLClientAnnotationProcessor primedProcessor(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        GraphQLClientAnnotationProcessor proc = new GraphQLClientAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        return proc;
    }

    private static void failProcessor(ProcessorContext ctx) {
        StringBuilder sb = new StringBuilder("GraphQL processor reported errors:\n");
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
        fail(sb.toString());
    }

    private static File testClassesDir() throws Exception {
        URL url = GraphQLClientAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
