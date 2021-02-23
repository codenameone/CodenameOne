package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Collections;
import java.util.Properties;

import static com.codename1.maven.PathUtil.path;
import static org.junit.Assert.assertEquals;

public class ProjectTemplateTest {
    private File project;

    @BeforeEach
    private void setUp() throws Exception {
        System.out.println("In Setup");
        project = File.createTempFile("project", "test");
        project.delete();
        project.mkdir();

        File src = new File(project, "src");
        src.mkdir();

        File pkg = new File(src, "__packagePath__");
        pkg.mkdir();

        File main = new File(pkg, "__mainName__.java");
        String mainContents = "package ${packageName};\n\npublic class ${mainName} {\n  public void hello(){}\n}\n";
        FileUtils.writeStringToFile(main, mainContents, "UTF-8");

        File cn1Settings = new File(project, "codenameone_settings.properties");
        FileUtils.writeStringToFile(cn1Settings, "", "UTF-8");

    }



    @AfterEach
    private void tearDown() throws Exception {
        if (project.exists()) {
            FileUtils.deleteDirectory(project);
        }
    }

    @Test
    public void processContentTest() throws Exception {
        System.out.println("in processContentTest");
        Properties props = new Properties();
        props.setProperty("mainName", "MyExample");
        props.setProperty("packageName", "com.example");
        ProjectTemplate tpl = new ProjectTemplate(project, props);

        String content = FileUtils.readFileToString(new File(project, path("src", "__packagePath__", "__mainName__.java")), "UTF-8");
        String newContent = tpl.processContent(content);
        String expected = "package com.example;\n\npublic class MyExample {\n  public void hello(){}\n}\n";
        assertEquals(expected, newContent);
    }

    @Test
    public void processFileNameTest() throws Exception {
        System.out.println("in processContentTest");
        Properties props = new Properties();
        props.setProperty("mainName", "MyExample");
        props.setProperty("packageName", "com.example");
        ProjectTemplate tpl = new ProjectTemplate(project, props);
        File mainFile = new File(project, path("src", "__packagePath__", "__mainName__.java"));
        File newMainFile = tpl.processFileName(mainFile);

        File expectedNewMain = new File(project, path("src", "__packagePath__", "MyExample.java"));
        assertEquals(expectedNewMain, newMainFile);
        assert(mainFile.exists()); // processFileName shouldn't actually make any changes to the file system.
        assert(!newMainFile.exists());

        File packageDir = new File(project, path("src", "__packagePath__"));
        File newPackageDir = tpl.processFileName(packageDir);
        File expectedNewPackageDir = new File(project, path("src", "com", "example"));
        assertEquals(expectedNewPackageDir, newPackageDir);
        assert(packageDir.exists());
        assert(!newPackageDir.exists());

    }

    @Test
    public void processFilesTest() throws Exception {
        Properties props = new Properties();
        props.setProperty("mainName", "MyExample");
        props.setProperty("packageName", "com.example");
        ProjectTemplate tpl = new ProjectTemplate(project, props);
        tpl.processFiles();
        File mainFile = new File(project, path("src", "__packagePath__", "__mainName__.java"));
        File expectedNewMain = new File(project, path("src", "com", "example", "MyExample.java"));
        assert(expectedNewMain.exists());
        assert(!mainFile.exists());
        String content = FileUtils.readFileToString(expectedNewMain, "UTF-8");

        String expected = "package com.example;\n\npublic class MyExample {\n  public void hello(){}\n}\n";
        assertEquals(expected, content);

        File cn1SettingsFile = new File(project, "codenameone_settings.properties");
        Properties cn1Settings = new Properties();
        cn1Settings.load(new FileInputStream(cn1SettingsFile));
        assertEquals("com.example", cn1Settings.getProperty("codename1.packageName"));
        assertEquals("MyExample", cn1Settings.getProperty("codename1.mainName"));


    }

    @Test
    public void convertToTemplateTest() throws Exception {
        // Lets start by converting *from* a template
        processFilesTest();
        Properties props = new Properties();
        ProjectTemplate tpl = new ProjectTemplate(project, props);
        tpl.convertToTemplate("com.example", "MyExample");
        File mainFile = new File(project, path("src", "__packagePath__", "__mainName__.java"));
        File expectedNewMain = new File(project, path("src", "com", "example", "MyExample.java"));
        assert(!expectedNewMain.exists());
        assert(mainFile.exists());
        String content = FileUtils.readFileToString(mainFile, "UTF-8");

        String expected = "package ${packageName};\n\npublic class ${mainName} {\n  public void hello(){}\n}\n";
        assertEquals(expected, content);

        File cn1SettingsFile = new File(project, "codenameone_settings.properties");
        Properties cn1Settings = new Properties();
        cn1Settings.load(new FileInputStream(cn1SettingsFile));
        assertEquals("${packageName}", cn1Settings.getProperty("codename1.packageName"));
        assertEquals("${mainName}", cn1Settings.getProperty("codename1.mainName"));

    }
}
