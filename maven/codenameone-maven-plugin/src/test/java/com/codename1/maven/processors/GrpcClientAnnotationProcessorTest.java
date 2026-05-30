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

/// End-to-end test for `GrpcClientAnnotationProcessor`. Compiles a
/// `@GrpcClient` Greeter fixture, runs both the proto and gRPC
/// processors, asserts the generated impl chains
/// `GrpcWeb.invokeUnary` with the right service / method names and
/// codec references.
public class GrpcClientAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void emitsImplAndBootstrapForGreeter() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.hello.HelloRequest",
                "package com.example.hello;\n"
                        + "import com.codename1.annotations.grpc.*;\n"
                        + "@ProtoMessage public class HelloRequest {\n"
                        + "    @ProtoField(tag = 1) public String name;\n"
                        + "    public HelloRequest() {}\n"
                        + "}\n");
        sources.put("com.example.hello.HelloReply",
                "package com.example.hello;\n"
                        + "import com.codename1.annotations.grpc.*;\n"
                        + "@ProtoMessage public class HelloReply {\n"
                        + "    @ProtoField(tag = 1) public String message;\n"
                        + "    public HelloReply() {}\n"
                        + "}\n");
        sources.put("com.example.hello.GreeterGrpc",
                "package com.example.hello;\n"
                        + "import com.codename1.annotations.grpc.*;\n"
                        + "import com.codename1.annotations.rest.Header;\n"
                        + "import com.codename1.io.grpc.GrpcResponse;\n"
                        + "import com.codename1.util.OnComplete;\n"
                        + "@GrpcClient(\"helloworld.Greeter\")\n"
                        + "public interface GreeterGrpc {\n"
                        + "    @Rpc(\"SayHello\")\n"
                        + "    void sayHello(HelloRequest req,\n"
                        + "                  @Header(\"Authorization\") String bearerToken,\n"
                        + "                  OnComplete<GrpcResponse<HelloReply>> cb);\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));

        // Run the proto processor first so the codecs exist in the
        // classpath before the gRPC processor's impl source compiles.
        ProcessorContext protoCtx = runProtoProcessor(classes);
        if (protoCtx.hasErrors()) failProcessor("proto", protoCtx);

        ProcessorContext grpcCtx = runGrpcProcessor(classes);
        if (grpcCtx.hasErrors()) failProcessor("gRPC", grpcCtx);

        assertTrue("expected GreeterGrpcImpl.class",
                new File(classes, "com/example/hello/GreeterGrpcImpl.class").exists());
        assertTrue("expected GrpcClientBootstrap.class",
                new File(classes, "cn1app/GrpcClientBootstrap.class").exists());

        String implSrc = generateImplSourceForFixture(classes);
        assertTrue("impl should call GrpcWeb.invokeUnary; was:\n" + implSrc,
                implSrc.contains("com.codename1.io.grpc.GrpcWeb.invokeUnary"));
        assertTrue("impl should pass the gRPC service path",
                implSrc.contains("\"helloworld.Greeter\""));
        assertTrue("impl should pass the gRPC method name",
                implSrc.contains("\"SayHello\""));
        assertTrue("impl should reference request codec INSTANCE",
                implSrc.contains("com.example.hello.HelloRequestProtoCodec.INSTANCE"));
        assertTrue("impl should reference response codec INSTANCE",
                implSrc.contains("com.example.hello.HelloReplyProtoCodec.INSTANCE"));

        String bootSrc = generateBootstrapSourceForFixture(classes);
        assertTrue("bootstrap should register GreeterGrpc; was:\n" + bootSrc,
                bootSrc.contains("GrpcClients.register(com.example.hello.GreeterGrpc.class"));
        assertTrue("bootstrap should instantiate GreeterGrpcImpl",
                bootSrc.contains("new com.example.hello.GreeterGrpcImpl(baseUrl)"));
    }

    @Test
    public void rejectsMissingRpcAnnotation() throws Exception {
        File classes = tmp.newFolder("classes");
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.x.Req",
                "package com.example.x;\n"
                        + "import com.codename1.annotations.grpc.*;\n"
                        + "@ProtoMessage public class Req { public Req() {} }\n");
        sources.put("com.example.x.Resp",
                "package com.example.x;\n"
                        + "import com.codename1.annotations.grpc.*;\n"
                        + "@ProtoMessage public class Resp { public Resp() {} }\n");
        sources.put("com.example.x.MissingRpc",
                "package com.example.x;\n"
                        + "import com.codename1.annotations.grpc.GrpcClient;\n"
                        + "import com.codename1.io.grpc.GrpcResponse;\n"
                        + "import com.codename1.util.OnComplete;\n"
                        + "@GrpcClient(\"x.MissingRpc\")\n"
                        + "public interface MissingRpc {\n"
                        + "    void unmarked(Req r, OnComplete<GrpcResponse<Resp>> cb);\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runGrpcProcessor(classes);
        assertTrue("expected error on method without @Rpc", ctx.hasErrors());
    }

    @Test
    public void rejectsGrpcClientOnConcreteClass() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Bad",
                        "package com.example;\n"
                                + "import com.codename1.annotations.grpc.GrpcClient;\n"
                                + "@GrpcClient(\"x.Bad\") public class Bad {}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runGrpcProcessor(classes);
        assertTrue("expected error on @GrpcClient applied to a class", ctx.hasErrors());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private ProcessorContext runProtoProcessor(File classesDir) throws Exception {
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

    private ProcessorContext runGrpcProcessor(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        GrpcClientAnnotationProcessor proc = new GrpcClientAnnotationProcessor();
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
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        GrpcClientAnnotationProcessor proc = new GrpcClientAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        java.lang.reflect.Field accepted = GrpcClientAnnotationProcessor.class
                .getDeclaredField("accepted");
        accepted.setAccessible(true);
        java.util.TreeMap<?, ?> map = (java.util.TreeMap<?, ?>) accepted.get(proc);
        Object api = map.values().iterator().next();
        java.lang.reflect.Method m = GrpcClientAnnotationProcessor.class
                .getDeclaredMethod("generateImplSource",
                        Class.forName("com.codename1.maven.processors.GrpcClientAnnotationProcessor$GrpcApi"));
        m.setAccessible(true);
        return (String) m.invoke(null, api);
    }

    private String generateBootstrapSourceForFixture(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        GrpcClientAnnotationProcessor proc = new GrpcClientAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        java.lang.reflect.Field accepted = GrpcClientAnnotationProcessor.class
                .getDeclaredField("accepted");
        accepted.setAccessible(true);
        java.util.TreeMap<?, ?> map = (java.util.TreeMap<?, ?>) accepted.get(proc);
        java.lang.reflect.Method m = GrpcClientAnnotationProcessor.class
                .getDeclaredMethod("generateBootstrapSource", Iterable.class);
        m.setAccessible(true);
        return (String) m.invoke(null, map.values());
    }

    private static void failProcessor(String which, ProcessorContext ctx) {
        StringBuilder sb = new StringBuilder(which + " processor reported errors:\n");
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
        fail(sb.toString());
    }

    private static File testClassesDir() throws Exception {
        URL url = GrpcClientAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
