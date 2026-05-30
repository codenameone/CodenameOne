/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.maven;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Drives the OpenAPI codegen against an inline JSON spec to verify model +
/// `@RestClient` emission shape. Both the Java 17 records path and the Java 8
/// classes path are exercised.
class GenerateOpenApiMojoTest {

    private static final String SAMPLE_SPEC =
            "{"
            + "\"openapi\":\"3.0.0\","
            + "\"info\":{\"title\":\"Petstore\",\"version\":\"1.0\"},"
            + "\"paths\":{"
            + "  \"/pet/{petId}\":{"
            + "    \"get\":{"
            + "      \"tags\":[\"Pet\"],"
            + "      \"operationId\":\"getPetById\","
            + "      \"parameters\":[{\"name\":\"petId\",\"in\":\"path\",\"required\":true,"
            + "        \"schema\":{\"type\":\"integer\",\"format\":\"int64\"}}],"
            + "      \"responses\":{\"200\":{"
            + "        \"description\":\"ok\","
            + "        \"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Pet\"}}}"
            + "      }}"
            + "    }"
            + "  },"
            + "  \"/pets\":{"
            + "    \"get\":{"
            + "      \"tags\":[\"Pet\"],"
            + "      \"operationId\":\"findPets\","
            + "      \"responses\":{\"200\":{"
            + "        \"description\":\"ok\","
            + "        \"content\":{\"application/json\":{\"schema\":{"
            + "          \"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/Pet\"}"
            + "        }}}"
            + "      }}"
            + "    }"
            + "  }"
            + "},"
            + "\"components\":{\"schemas\":{"
            + "  \"Pet\":{"
            + "    \"type\":\"object\","
            + "    \"properties\":{"
            + "      \"id\":{\"type\":\"integer\",\"format\":\"int64\"},"
            + "      \"name\":{\"type\":\"string\"}"
            + "    }"
            + "  },"
            + "  \"Cat\":{"
            + "    \"type\":\"object\","
            + "    \"properties\":{"
            + "      \"id\":{\"type\":\"integer\",\"format\":\"int64\"},"
            + "      \"name\":{\"type\":\"string\"}"
            + "    }"
            + "  }"
            + "}}"
            + "}";

    @Test
    void emitsRecordsAndRestClientInterface(@TempDir Path tmp) throws Exception {
        Map<String, Object> doc = parse(SAMPLE_SPEC);
        File out = tmp.toFile();
        GenerateOpenApiMojo.Generator gen = new GenerateOpenApiMojo.Generator(
                doc, "com.example.petstore", out, /*overwrite*/ true,
                /*emitRecords*/ true, new SystemStreamLog());
        gen.run();

        File petJava = new File(out, "com/example/petstore/model/Pet.java");
        assertTrue(petJava.exists(), "expected Pet.java at " + petJava);
        String petSrc = readString(petJava);
        assertTrue(petSrc.contains("@Mapped"), "Pet should be @Mapped");
        assertTrue(petSrc.contains("public record Pet("), "Pet should be a record on Java 17 target");
        assertTrue(petSrc.contains("@JsonProperty(\"id\") Long id"),
                "Pet record should declare @JsonProperty(\"id\") Long id; was:\n" + petSrc);
        assertTrue(petSrc.contains("@JsonProperty(\"name\") String name"),
                "Pet record should declare name; was:\n" + petSrc);

        // Cat is structurally identical to Pet and should collapse to Pet --
        // i.e. Cat.java should NOT be emitted as a separate record.
        File catJava = new File(out, "com/example/petstore/model/Cat.java");
        assertFalse(catJava.exists(),
                "Cat is structurally identical to Pet -- expected schema unification to drop it");

        File petApi = new File(out, "com/example/petstore/PetApi.java");
        assertTrue(petApi.exists(), "expected PetApi.java at " + petApi);
        String apiSrc = readString(petApi);
        assertTrue(apiSrc.contains("@RestClient"), "PetApi should carry @RestClient");
        assertTrue(apiSrc.contains("public interface PetApi"), "PetApi must be an interface");
        assertTrue(apiSrc.contains("@GET(\"/pet/{petId}\")"),
                "getPetById method should carry @GET(\"/pet/{petId}\"); was:\n" + apiSrc);
        assertTrue(apiSrc.contains("@Path(\"petId\") Long petId"),
                "getPetById path param shape; was:\n" + apiSrc);
        assertTrue(apiSrc.contains("@Header(\"Authorization\") String bearerToken"),
                "bearerToken header shape; was:\n" + apiSrc);
        assertTrue(apiSrc.contains("OnComplete<Response<com.example.petstore.model.Pet>> callback"),
                "callback shape; was:\n" + apiSrc);
        assertTrue(apiSrc.contains("OnComplete<Response<java.util.List<com.example.petstore.model.Pet>>> callback"),
                "findPets list-of-Pet response; was:\n" + apiSrc);
        assertTrue(apiSrc.contains("static PetApi of(String baseUrl)"),
                "static of(...) factory must be emitted");
        assertTrue(apiSrc.contains("RestClients.create(PetApi.class, baseUrl)"),
                "of(...) factory should delegate to RestClients.create");
    }

    @Test
    void emitsClassesOnJava8Target(@TempDir Path tmp) throws Exception {
        Map<String, Object> doc = parse(SAMPLE_SPEC);
        File out = tmp.toFile();
        GenerateOpenApiMojo.Generator gen = new GenerateOpenApiMojo.Generator(
                doc, "com.example.petstore", out, true,
                /*emitRecords*/ false, new SystemStreamLog());
        gen.run();

        String petSrc = readString(new File(out, "com/example/petstore/model/Pet.java"));
        assertTrue(petSrc.contains("public class Pet {"), "Pet should be a class on Java 8 target");
        assertTrue(petSrc.contains("public Long id;"), "Pet class should declare Long id field");
        assertTrue(petSrc.contains("public String name;"), "Pet class should declare String name field");
        assertTrue(petSrc.contains("public Pet() {}"), "Pet class should have a public no-arg ctor");
    }

    @Test
    void respectsOverwriteFalseAndPreservesUserEdits(@TempDir Path tmp) throws Exception {
        Map<String, Object> doc = parse(SAMPLE_SPEC);
        File out = tmp.toFile();
        File apiDir = new File(out, "com/example/petstore");
        if (!apiDir.exists() && !apiDir.mkdirs()) throw new IOException("mkdirs");
        File apiFile = new File(apiDir, "PetApi.java");
        Files.write(apiFile.toPath(), "// hand-edited".getBytes(StandardCharsets.UTF_8));

        GenerateOpenApiMojo.Generator gen = new GenerateOpenApiMojo.Generator(
                doc, "com.example.petstore", out, /*overwrite*/ false,
                true, new SystemStreamLog());
        gen.run();

        String apiSrc = readString(apiFile);
        assertTrue(apiSrc.startsWith("// hand-edited"),
                "overwrite=false should preserve user edits; was:\n" + apiSrc);
    }

    @Test
    void parseJavaVersionHandlesShapes() {
        org.junit.jupiter.api.Assertions.assertEquals(8, GenerateOpenApiMojo.parseJavaVersion("1.8"));
        org.junit.jupiter.api.Assertions.assertEquals(8, GenerateOpenApiMojo.parseJavaVersion(null));
        org.junit.jupiter.api.Assertions.assertEquals(11, GenerateOpenApiMojo.parseJavaVersion("11"));
        org.junit.jupiter.api.Assertions.assertEquals(17, GenerateOpenApiMojo.parseJavaVersion("17"));
        org.junit.jupiter.api.Assertions.assertEquals(21, GenerateOpenApiMojo.parseJavaVersion("21-LTS"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static Map<String, Object> parse(String json) throws IOException {
        return new com.codename1.io.JSONParser().parseJSON(new StringReader(json));
    }

    private static String readString(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
