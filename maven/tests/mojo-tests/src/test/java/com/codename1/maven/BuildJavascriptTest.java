package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class BuildJavascriptTest {
    @Test
    public void testJavascriptBuild() throws IOException, MojoExecutionException {
        /*
        File projectDir = new File("target/tests/JavascriptBuild");
        FileUtils.deleteDirectory(projectDir);
        projectDir.getParentFile().mkdirs();
        ProjectUtil.generateHelloWorldProject(projectDir, "com.codename1.testjsbuild");
        ProjectUtil.executeGoal(new File(projectDir, ), "cn1:build", "className=com.codename1.tests.HelloForm");
        File helloFormJava = new File(projectDir, path("src", "main", "java", "com", "codename1", "tests", "HelloForm.java"));
        File helloFormGui = new File(projectDir, path("src", "main", "guibuilder", "com", "codename1", "tests", "HelloForm.gui"));
        // We don't want them created in the root project.
        // We want them in the common project
        */

    }
}
