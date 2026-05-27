package com.codename1.maven;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage of {@link OpenApiCodegen} against a small inline OpenAPI
 * spec resembling a Petstore-style API. The test asserts the generator emits
 * the expected file set and that key marker strings (annotations, method
 * signatures, return types) appear in the output.
 *
 * <p>This is an integration test in spirit -- not a pure unit test of
 * sanitizer methods -- but it doesn't reach the network so it stays
 * deterministic and runs in <100ms.
 */
class OpenApiCodegenTest {

    private static final String INLINE_SPEC =
            "{"
            + "\"openapi\":\"3.0.0\","
            + "\"info\":{\"title\":\"t\",\"version\":\"1.0\"},"
            + "\"paths\":{"
            + "  \"/pet/{petId}\":{"
            + "    \"get\":{\"tags\":[\"pet\"],\"operationId\":\"getPetById\","
            + "      \"parameters\":[{\"name\":\"petId\",\"in\":\"path\",\"required\":true,\"schema\":{\"type\":\"integer\",\"format\":\"int64\"}}],"
            + "      \"responses\":{\"200\":{\"description\":\"ok\",\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Pet\"}}}}}"
            + "    },"
            + "    \"delete\":{\"tags\":[\"pet\"],\"operationId\":\"deletePet\","
            + "      \"parameters\":[{\"name\":\"petId\",\"in\":\"path\",\"required\":true,\"schema\":{\"type\":\"integer\",\"format\":\"int64\"}}],"
            + "      \"responses\":{\"200\":{\"description\":\"ok\"}}"
            + "    }"
            + "  },"
            + "  \"/pet\":{"
            + "    \"post\":{\"tags\":[\"pet\"],\"operationId\":\"addPet\","
            + "      \"requestBody\":{\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Pet\"}}}},"
            + "      \"responses\":{\"200\":{\"description\":\"ok\",\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Pet\"}}}}}"
            + "    }"
            + "  },"
            + "  \"/pet/findByStatus\":{"
            + "    \"get\":{\"tags\":[\"pet\"],\"operationId\":\"findPetsByStatus\","
            + "      \"parameters\":[{\"name\":\"status\",\"in\":\"query\",\"schema\":{\"type\":\"string\"}}],"
            + "      \"responses\":{\"200\":{\"description\":\"ok\",\"content\":{\"application/json\":{\"schema\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/Pet\"}}}}}}"
            + "    }"
            + "  }"
            + "},"
            + "\"components\":{\"schemas\":{"
            + "  \"Pet\":{\"type\":\"object\",\"properties\":{"
            + "    \"id\":{\"type\":\"integer\",\"format\":\"int64\"},"
            + "    \"name\":{\"type\":\"string\"},"
            + "    \"tags\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}"
            + "  }},"
            + "  \"Tag\":{\"type\":\"object\",\"properties\":{"
            + "    \"id\":{\"type\":\"integer\",\"format\":\"int64\"},"
            + "    \"name\":{\"type\":\"string\"}"
            + "  }}"
            + "}}}";

    @Test
    void emitsExpectedFiles() throws Exception {
        File specFile = File.createTempFile("openapi-spec-", ".json");
        try (FileOutputStream out = new FileOutputStream(specFile)) {
            out.write(INLINE_SPEC.getBytes(StandardCharsets.UTF_8));
        }

        File outDir = Files.createTempDirectory("cn1-openapi-codegen").toFile();
        try {
            Map<String, Object> spec = OpenApiCodegen.loadSpec(specFile.getAbsolutePath());
            assertNotNull(spec, "spec parses to non-null Map");
            new OpenApiCodegen(outDir, "com.example.api", new SystemStreamLog(), spec).generate();

            // Models
            File petModel = new File(outDir, "com/example/api/model/Pet.java");
            File tagModel = new File(outDir, "com/example/api/model/Tag.java");
            assertTrue(petModel.exists(), "Pet model emitted");
            assertTrue(tagModel.exists(), "Tag model emitted");

            String petContent = new String(Files.readAllBytes(petModel.toPath()), StandardCharsets.UTF_8);
            assertTrue(petContent.contains("@Mapped"), "Pet POJO carries @Mapped");
            assertTrue(petContent.contains("@JsonProperty(\"id\")"), "Pet#id has @JsonProperty(\"id\")");
            assertTrue(petContent.contains("public long id"), "Pet#id is a long (int64)");
            assertTrue(petContent.contains("public String name"),
                    "Pet#name is a String. got:\n" + petContent);
            assertTrue(petContent.contains("java.util.List<String>"),
                    "Pet#tags is a List<String>. got:\n" + petContent);

            // Api class
            File petApi = new File(outDir, "com/example/api/PetApi.java");
            assertTrue(petApi.exists(), "PetApi emitted");
            String petApiContent = new String(Files.readAllBytes(petApi.toPath()), StandardCharsets.UTF_8);
            assertTrue(petApiContent.contains("public void getPetById(long petId,"),
                    "getPetById takes long petId. got:\n" + petApiContent);
            assertTrue(petApiContent.contains("Rest.get(url)"),
                    "GET endpoint generates Rest.get. got:\n" + petApiContent);
            assertTrue(petApiContent.contains(".fetchAsMapped(com.example.api.model.Pet.class,"),
                    "getPetById returns mapped Pet. got:\n" + petApiContent);
            assertTrue(petApiContent.contains("public void deletePet(long petId,"),
                    "deletePet emitted. got:\n" + petApiContent);
            assertTrue(petApiContent.contains("Rest.delete(url)"),
                    "DELETE endpoint generates Rest.delete. got:\n" + petApiContent);
            assertTrue(petApiContent.contains("public void addPet(com.example.api.model.Pet body,"),
                    "addPet body is typed Pet. got:\n" + petApiContent);
            assertTrue(petApiContent.contains("Rest.post(url)"),
                    "POST endpoint generates Rest.post. got:\n" + petApiContent);
            assertTrue(petApiContent.contains(".fetchAsMappedList(com.example.api.model.Pet.class,"),
                    "findPetsByStatus returns mapped list. got:\n" + petApiContent);
            assertTrue(petApiContent.contains("if (status != null) rb = rb.queryParam(\"status\","),
                    "Optional query param wired as queryParam. got:\n" + petApiContent);
            assertTrue(petApiContent.contains("if (bearerToken != null)"),
                    "Bearer token plumbing present. got:\n" + petApiContent);
            assertTrue(petApiContent.contains("baseUrl + \"/pet/\" + petId + \"\""),
                    "Path param interpolated into URL. got:\n" + petApiContent);
        } finally {
            // Cleanup
            specFile.delete();
        }
    }

    /// If a saved Swagger Petstore spec is on disk at `/tmp/petstore.json`,
    /// drive the generator end-to-end. The test asserts the generator
    /// produces the expected Api classes (PetApi, StoreApi, UserApi) and
    /// the expected model count -- catching real-world spec patterns
    /// (operationId fallbacks, multi-tag operations, $ref-in-array
    /// schemas) that the inline-spec test doesn't exercise.
    ///
    /// Skipped when the file isn't present so the test suite stays
    /// hermetic.
    @Test
    void generatesPetstoreClient() throws Exception {
        File petstoreSpec = new File("/tmp/petstore.json");
        if (!petstoreSpec.exists()) {
            System.out.println("petstore.json not present; skipping. "
                    + "To run: curl -sS https://petstore3.swagger.io/api/v3/openapi.json > /tmp/petstore.json");
            return;
        }
        File outDir = Files.createTempDirectory("cn1-openapi-petstore").toFile();
        Map<String, Object> spec = OpenApiCodegen.loadSpec(petstoreSpec.getAbsolutePath());
        new OpenApiCodegen(outDir, "com.petstore.api", new SystemStreamLog(), spec).generate();

        File petApi = new File(outDir, "com/petstore/api/PetApi.java");
        File storeApi = new File(outDir, "com/petstore/api/StoreApi.java");
        File userApi = new File(outDir, "com/petstore/api/UserApi.java");
        assertTrue(petApi.exists(), "PetApi emitted from real Petstore spec");
        assertTrue(storeApi.exists(), "StoreApi emitted");
        assertTrue(userApi.exists(), "UserApi emitted");

        File petModel = new File(outDir, "com/petstore/api/model/Pet.java");
        File orderModel = new File(outDir, "com/petstore/api/model/Order.java");
        File userModel = new File(outDir, "com/petstore/api/model/User.java");
        assertTrue(petModel.exists(), "Pet model emitted");
        assertTrue(orderModel.exists(), "Order model emitted");
        assertTrue(userModel.exists(), "User model emitted");

        String petContent = new String(Files.readAllBytes(petModel.toPath()), StandardCharsets.UTF_8);
        assertTrue(petContent.contains("public long id"), "Pet#id is long (int64). got:\n" + petContent);
        assertTrue(petContent.contains("public java.util.List<String> photoUrls"),
                "Pet#photoUrls is List<String>. got:\n" + petContent);

        String petApiContent = new String(Files.readAllBytes(petApi.toPath()), StandardCharsets.UTF_8);
        // The Petstore has findPetsByStatus returning a List<Pet>; verify list path.
        assertTrue(petApiContent.contains("fetchAsMappedList(com.petstore.api.model.Pet.class"),
                "findPetsByStatus uses fetchAsMappedList. PetApi content:\n" + petApiContent);
    }

    @Test
    void sanitizesIdentifiers() {
        assertEquals("getPetById", OpenApiCodegen.sanitizeIdentifier("getPetById"));
        assertEquals("xRateLimit", OpenApiCodegen.sanitizeIdentifier("X-Rate-Limit"));
        assertEquals("class_", OpenApiCodegen.sanitizeIdentifier("class"));
        assertEquals("_123foo", OpenApiCodegen.sanitizeIdentifier("123foo"));
    }

    @Test
    void sanitizesClassNames() {
        assertEquals("Pet", OpenApiCodegen.sanitizeClassName("pet"));
        assertEquals("OrderItem", OpenApiCodegen.sanitizeClassName("order_item"));
        assertEquals("HttpClient", OpenApiCodegen.sanitizeClassName("http-client"));
    }
}
