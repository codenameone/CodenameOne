/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.maven;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Drives the proto3 parser + generator against an inline `.proto`
/// fixture to verify message / enum / service emission shape on both
/// the record (Java 17+) and class (Java 8) code paths.
class GenerateGrpcMojoTest {

    private static final String HELLOWORLD_PROTO =
            "// header comment\n"
            + "syntax = \"proto3\";\n"
            + "package helloworld;\n\n"
            + "/* request */\n"
            + "message HelloRequest {\n"
            + "    string name = 1;\n"
            + "    repeated string aliases = 2;\n"
            + "    Mood mood = 3;\n"
            + "}\n\n"
            + "message HelloReply {\n"
            + "    string message = 1;\n"
            + "    sint32 emoji_code = 2;\n"
            + "    HelloRequest echo = 3;\n"
            + "}\n\n"
            + "enum Mood {\n"
            + "    UNKNOWN = 0;\n"
            + "    HAPPY = 1;\n"
            + "    SAD = 2;\n"
            + "}\n\n"
            + "service Greeter {\n"
            + "    rpc SayHello (HelloRequest) returns (HelloReply);\n"
            + "}\n";

    @Test
    void emitsMessagesEnumsAndServiceAsRecords(@TempDir Path tmp) throws Exception {
        GenerateGrpcMojo.ProtoFile parsed = parse(HELLOWORLD_PROTO);
        File out = tmp.toFile();
        new GenerateGrpcMojo.Generator(parsed, "com.example.hello", out,
                /*overwrite*/ true, /*emitRecords*/ true, new SystemStreamLog()).run();

        String reqSrc = readString(out, "com/example/hello/HelloRequest.java");
        assertTrue(reqSrc.contains("@ProtoMessage"), "HelloRequest should be @ProtoMessage");
        assertTrue(reqSrc.contains("public record HelloRequest("),
                "HelloRequest should be a record on Java 17 target; was:\n" + reqSrc);
        assertTrue(reqSrc.contains("@ProtoField(tag = 1) String name"),
                "name field shape; was:\n" + reqSrc);
        assertTrue(reqSrc.contains("@ProtoField(tag = 2) List<String> aliases"),
                "repeated string -> List<String>; was:\n" + reqSrc);
        assertTrue(reqSrc.contains("com.example.hello.Mood mood"),
                "Mood resolved to FQN; was:\n" + reqSrc);

        String replySrc = readString(out, "com/example/hello/HelloReply.java");
        assertTrue(replySrc.contains("@ProtoField(tag = 2, wireType = ProtoField.WireKind.SINT, name = \"emoji_code\") int emojiCode"),
                "sint32 -> SINT wire kind; snake_case -> camelCase preserves name; was:\n" + replySrc);
        assertTrue(replySrc.contains("com.example.hello.HelloRequest echo"),
                "nested message resolved; was:\n" + replySrc);

        String moodSrc = readString(out, "com/example/hello/Mood.java");
        assertTrue(moodSrc.contains("@ProtoEnum"), "Mood should be @ProtoEnum");
        assertTrue(moodSrc.contains("public enum Mood"), "Mood should be an enum");
        assertTrue(moodSrc.contains("HAPPY(1)"), "Mood values include HAPPY(1); was:\n" + moodSrc);
        assertTrue(moodSrc.contains("public static Mood forNumber(int n)"),
                "forNumber lookup helper present");

        String svcSrc = readString(out, "com/example/hello/GreeterGrpc.java");
        assertTrue(svcSrc.contains("@GrpcClient(\"helloworld.Greeter\")"),
                "service path joins proto package + service name; was:\n" + svcSrc);
        assertTrue(svcSrc.contains("@Rpc(\"SayHello\")"), "rpc binding; was:\n" + svcSrc);
        assertTrue(svcSrc.contains("void sayHello(com.example.hello.HelloRequest request, "
                        + "@Header(\"Authorization\") String bearerToken, "
                        + "OnComplete<GrpcResponse<com.example.hello.HelloReply>> callback);"),
                "RPC method shape; was:\n" + svcSrc);
        assertTrue(svcSrc.contains("static GreeterGrpc of(String baseUrl)"),
                "static of(...) factory; was:\n" + svcSrc);
        assertTrue(svcSrc.contains("GrpcClients.create(GreeterGrpc.class, baseUrl)"),
                "of(...) delegates to GrpcClients.create");
    }

    @Test
    void emitsClassesOnJava8Target(@TempDir Path tmp) throws Exception {
        GenerateGrpcMojo.ProtoFile parsed = parse(HELLOWORLD_PROTO);
        File out = tmp.toFile();
        new GenerateGrpcMojo.Generator(parsed, "com.example.hello", out, true,
                /*emitRecords*/ false, new SystemStreamLog()).run();

        String reqSrc = readString(out, "com/example/hello/HelloRequest.java");
        assertTrue(reqSrc.contains("public class HelloRequest {"),
                "class form on Java 8 target");
        assertTrue(reqSrc.contains("public String name;"), "name as public field");
        assertTrue(reqSrc.contains("public HelloRequest() {}"), "no-arg ctor");
    }

    @Test
    void respectsOverwriteFalse(@TempDir Path tmp) throws Exception {
        GenerateGrpcMojo.ProtoFile parsed = parse(HELLOWORLD_PROTO);
        File out = tmp.toFile();
        File svcDir = new File(out, "com/example/hello");
        if (!svcDir.exists() && !svcDir.mkdirs()) throw new IOException("mkdirs");
        File svcFile = new File(svcDir, "GreeterGrpc.java");
        Files.write(svcFile.toPath(), "// hand-edited".getBytes(StandardCharsets.UTF_8));

        new GenerateGrpcMojo.Generator(parsed, "com.example.hello", out,
                /*overwrite*/ false, true, new SystemStreamLog()).run();
        String svcSrc = new String(Files.readAllBytes(svcFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(svcSrc.startsWith("// hand-edited"),
                "overwrite=false should preserve user edits");
    }

    @Test
    void streamingRpcRejected() {
        String src = "syntax = \"proto3\"; service S { rpc R (stream Req) returns (Resp); }\n";
        GenerateGrpcMojo.ProtoParseException ex = assertThrows(
                GenerateGrpcMojo.ProtoParseException.class,
                () -> new GenerateGrpcMojo.ProtoParser(src, "t.proto").parseFile());
        assertTrue(ex.getMessage().contains("Streaming RPCs"),
                "expected streaming rejection; got: " + ex.getMessage());
    }

    @Test
    void snakeCaseFieldNameConversion() {
        assertEquals("emojiCode", GenerateGrpcMojo.Generator.javaName("emoji_code"));
        assertEquals("alreadyCamel", GenerateGrpcMojo.Generator.javaName("alreadyCamel"));
        assertEquals("class_", GenerateGrpcMojo.Generator.javaName("class"),
                "reserved word should be suffixed with underscore");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static GenerateGrpcMojo.ProtoFile parse(String src) {
        return new GenerateGrpcMojo.ProtoParser(src, "t.proto").parseFile();
    }

    private static String readString(File root, String relative) throws IOException {
        File f = new File(root, relative);
        assertTrue(f.exists(), "expected file " + relative + " at " + f);
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
