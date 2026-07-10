package com.codename1.settings;

import com.codename1.settings.extensions.MavenDependency;
import com.codename1.settings.extensions.PomEditor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PomEditorTest {
    @Test
    public void addsDependencyToExistingDependenciesBlock() {
        String pom = "<project>\n    <dependencies>\n    </dependencies>\n</project>\n";
        MavenDependency dep = new MavenDependency("com.codenameone", "cn1-demo", "1.0");
        String updated = PomEditor.addDependency(pom, dep);
        assertTrue(updated.contains("<groupId>com.codenameone</groupId>"));
        assertTrue(updated.contains("<artifactId>cn1-demo</artifactId>"));
        assertEquals(updated, PomEditor.addDependency(updated, dep));
    }

    @Test
    public void removesOnlyTheSelectedDependency() {
        String pom = "<project>\n  <dependencies>\n"
                + "    <dependency>\n      <groupId>com.example</groupId>\n      <artifactId>keep</artifactId>\n    </dependency>\n"
                + "    <dependency>\n      <groupId>com.codenameone</groupId>\n      <artifactId>remove</artifactId>\n    </dependency>\n"
                + "  </dependencies>\n</project>\n";
        MavenDependency dep = new MavenDependency("com.codenameone", "remove", "1.0");
        String updated = PomEditor.removeDependency(pom, dep);
        assertTrue(updated.contains("<artifactId>keep</artifactId>"));
        assertTrue(!updated.contains("<artifactId>remove</artifactId>"));
        assertEquals(updated, PomEditor.removeDependency(updated, dep));
    }

    @Test
    public void dependencyCoordinatesMustAppearInTheSameBlock() {
        String pom = "<project><dependencies>"
                + "<dependency><groupId>com.example</groupId><artifactId>one</artifactId></dependency>"
                + "<dependency><groupId>com.other</groupId><artifactId>target</artifactId></dependency>"
                + "</dependencies></project>";

        assertFalse(PomEditor.containsDependency(pom,
                new MavenDependency("com.example", "target", "1.0")));
    }

    @Test
    public void addsToProjectDependenciesInsteadOfDependencyManagementOrProfiles() {
        String pom = "<project>\n"
                + "  <dependencyManagement><dependencies><dependency>"
                + "<groupId>com.managed</groupId><artifactId>managed</artifactId>"
                + "</dependency></dependencies></dependencyManagement>\n"
                + "  <dependencies>\n  </dependencies>\n"
                + "  <profiles><profile><dependencies>\n  </dependencies></profile></profiles>\n"
                + "</project>\n";
        MavenDependency dep = new MavenDependency("com.codenameone", "cn1-demo", "1.0");

        String updated = PomEditor.addDependency(pom, dep);

        int inserted = updated.indexOf("<artifactId>cn1-demo</artifactId>");
        assertTrue(inserted > updated.indexOf("</dependencyManagement>"));
        assertTrue(inserted < updated.indexOf("<profiles>"));
    }
}
