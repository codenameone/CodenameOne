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

/// End-to-end test for `ProtoMessageAnnotationProcessor`. Compiles
/// a `@ProtoMessage` Pet fixture, runs the processor, asserts the
/// generated codec source carries the expected
/// `ProtoWriter` / `ProtoReader` calls and the bootstrap registers
/// the codec.
public class ProtoMessageAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void emitsCodecAndBootstrapForPet() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Pet",
                "package com.example;\n"
                        + "import com.codename1.annotations.grpc.ProtoField;\n"
                        + "import com.codename1.annotations.grpc.ProtoMessage;\n"
                        + "import java.util.List;\n"
                        + "@ProtoMessage public class Pet {\n"
                        + "    @ProtoField(tag = 1) public long id;\n"
                        + "    @ProtoField(tag = 2) public String name;\n"
                        + "    @ProtoField(tag = 3) public List<String> tags;\n"
                        + "    @ProtoField(tag = 4) public Owner owner;\n"
                        + "    public Pet() {}\n"
                        + "}\n");
        sources.put("com.example.Owner",
                "package com.example;\n"
                        + "import com.codename1.annotations.grpc.ProtoField;\n"
                        + "import com.codename1.annotations.grpc.ProtoMessage;\n"
                        + "@ProtoMessage public class Owner {\n"
                        + "    @ProtoField(tag = 1) public String email;\n"
                        + "    public Owner() {}\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));

        ProcessorContext ctx = runProcessor(classes);
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("processor reported errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
            fail(sb.toString());
        }

        // Codecs land at <pkg>.<Type>ProtoCodec, bootstrap at cn1app.ProtoBootstrap.
        assertTrue("expected PetProtoCodec.class",
                new File(classes, "com/example/PetProtoCodec.class").exists());
        assertTrue("expected OwnerProtoCodec.class",
                new File(classes, "com/example/OwnerProtoCodec.class").exists());
        assertTrue("expected ProtoBootstrap.class",
                new File(classes, "cn1app/ProtoBootstrap.class").exists());

        String petCodecSrc = generateCodecSourceForFixture(classes, "com.example.Pet");
        assertTrue("codec emits writeInt64 for id; was:\n" + petCodecSrc,
                petCodecSrc.contains("out.writeInt64(1, value.id);"));
        assertTrue("codec emits writeString for name",
                petCodecSrc.contains("out.writeString(2, value.name);"));
        assertTrue("codec emits writeStringList for tags",
                petCodecSrc.contains("out.writeStringList(3, value.tags);"));
        assertTrue("codec emits writeMessage with OwnerProtoCodec.INSTANCE for owner",
                petCodecSrc.contains("out.writeMessage(4, value.owner, com.example.OwnerProtoCodec.INSTANCE);"));

        // Read path dispatches on tag.
        assertTrue("read emits switch on field number",
                petCodecSrc.contains("switch (_field) {"));
        assertTrue("read uses readMessage with the Owner codec",
                petCodecSrc.contains("in.readMessage(com.example.OwnerProtoCodec.INSTANCE)"));

        String bootSrc = generateBootstrapSourceForFixture(classes);
        assertTrue("bootstrap registers Pet codec; was:\n" + bootSrc,
                bootSrc.contains("com.example.PetProtoCodec.register();"));
        assertTrue("bootstrap registers Owner codec",
                bootSrc.contains("com.example.OwnerProtoCodec.register();"));
    }

    @Test
    public void rejectsZeroTag() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Bad",
                        "package com.example;\n"
                                + "import com.codename1.annotations.grpc.*;\n"
                                + "@ProtoMessage public class Bad {\n"
                                + "    @ProtoField(tag = 0) public int n;\n"
                                + "    public Bad() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected error on tag=0", ctx.hasErrors());
    }

    @Test
    public void rejectsDuplicateTags() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Dup",
                        "package com.example;\n"
                                + "import com.codename1.annotations.grpc.*;\n"
                                + "@ProtoMessage public class Dup {\n"
                                + "    @ProtoField(tag = 1) public int a;\n"
                                + "    @ProtoField(tag = 1) public int b;\n"
                                + "    public Dup() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected error on duplicate tag", ctx.hasErrors());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        ProtoMessageAnnotationProcessor proc = new ProtoMessageAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    private String generateCodecSourceForFixture(File classesDir, String fqn) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        ProtoMessageAnnotationProcessor proc = new ProtoMessageAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        // Resolve pending references the way finish() would (but skip the
        // compile step so we get the raw source back).
        java.lang.reflect.Method r = ProtoMessageAnnotationProcessor.class
                .getDeclaredMethod("resolveReferenceKind",
                        Class.forName("com.codename1.maven.processors.ProtoMessageAnnotationProcessor$ProtoFieldInfo"),
                        ProcessorContext.class);
        r.setAccessible(true);
        java.lang.reflect.Field accepted = ProtoMessageAnnotationProcessor.class
                .getDeclaredField("messages");
        accepted.setAccessible(true);
        java.util.TreeMap<?, ?> map = (java.util.TreeMap<?, ?>) accepted.get(proc);
        Object target = map.get(fqn);
        java.lang.reflect.Field fields = target.getClass().getDeclaredField("fields");
        fields.setAccessible(true);
        for (Object f : (java.util.List<?>) fields.get(target)) {
            r.invoke(proc, f, ctx);
        }
        java.lang.reflect.Method gen = ProtoMessageAnnotationProcessor.class
                .getDeclaredMethod("generateCodecSource",
                        Class.forName("com.codename1.maven.processors.ProtoMessageAnnotationProcessor$ProtoClass"));
        gen.setAccessible(true);
        return (String) gen.invoke(null, target);
    }

    private String generateBootstrapSourceForFixture(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        ProtoMessageAnnotationProcessor proc = new ProtoMessageAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        java.lang.reflect.Field accepted = ProtoMessageAnnotationProcessor.class
                .getDeclaredField("messages");
        accepted.setAccessible(true);
        java.util.TreeMap<?, ?> map = (java.util.TreeMap<?, ?>) accepted.get(proc);
        java.lang.reflect.Method m = ProtoMessageAnnotationProcessor.class
                .getDeclaredMethod("generateBootstrapSource", Iterable.class);
        m.setAccessible(true);
        return (String) m.invoke(null, map.values());
    }

    private static File testClassesDir() throws Exception {
        URL url = ProtoMessageAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
