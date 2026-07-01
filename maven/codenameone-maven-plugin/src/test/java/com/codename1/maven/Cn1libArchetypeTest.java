package com.codename1.maven;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Cn1libArchetypeTest {

    @Test
    void linuxModuleIsPartOfCn1libArchetypeAndPlatformSelector() throws Exception {
        String metadata = archetypeResource("META-INF/maven/archetype-metadata.xml");
        assertTrue(metadata.contains("dir=\"linux\""), metadata);
        assertTrue(metadata.contains("<directory>src/main/c</directory>"), metadata);

        String rootPom = archetypeResource("archetype-resources/pom.xml");
        assertTrue(rootPom.contains("<module>linux</module>"), rootPom);

        String libPom = archetypeResource("archetype-resources/lib/pom.xml");
        assertTrue(libPom.contains("<id>linux</id>"), libPom);
        assertTrue(libPom.contains("<artifactId>${cn1lib.name}-linux</artifactId>"), libPom);

        String linuxPom = archetypeResource("archetype-resources/linux/pom.xml");
        assertTrue(linuxPom.contains("<directory>src/main/c</directory>"), linuxPom);
        assertTrue(linuxPom.contains("<directory>src/main/resources</directory>"), linuxPom);
    }

    private static String archetypeResource(String path) throws Exception {
        File file = new File("../cn1lib-archetype/src/main/resources", path);
        assertTrue(file.isFile(), "Missing archetype resource " + file.getAbsolutePath());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
