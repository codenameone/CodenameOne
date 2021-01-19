package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Properties;
import java.io.File;
import com.codename1.maven.ProjectUtil;

public class CreateGuiFormMojoTest {

    private String path(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(File.separator);
            }
            sb.append(part);
        }
        return sb.toString();
    }

    @Test
    void testCreateGuiForm() throws Exception {

        File projectDir = new File("target/tests/HelloWorld");
        FileUtils.deleteDirectory(projectDir);
        projectDir.getParentFile().mkdirs();
        ProjectUtil.generateHelloWorldProject(projectDir, "com.codename1.tests");
        ProjectUtil.executeGoal(projectDir, "cn1:create-gui-form", "className=com.codename1.tests.HelloForm");
        File helloFormJava = new File(projectDir, path("src", "main", "java", "com", "codename1", "tests", "HelloForm.java"));
        File helloFormGui = new File(projectDir, path("src", "main", "guibuilder", "com", "codename1", "tests", "HelloForm.gui"));
        Assert.assertTrue(helloFormJava.exists());
        Assert.assertTrue(helloFormGui.exists());

        // Now make sure it compiles
        ProjectUtil.executeGoal(projectDir, "package");

    }
}
