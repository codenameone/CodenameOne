package com.codename1.initializr.model;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class GeneratorModelLocalizationPackagingTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        byte[] zip = createProjectZip();
        Map<String, byte[]> entries = readZipEntries(zip);

        File classesRoot = Files.createTempDirectory("cn1-localization-packaging").toFile();
        try {
            compileDisplayClass(classesRoot);
            writeGeneratedLocalizationResources(entries, classesRoot);
            assertResourcesLoadFromDisplayClass(classesRoot);
        } finally {
            Util.cleanup(classesRoot.getAbsolutePath());
        }
        return true;
    }

    private static byte[] createProjectZip() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GeneratorModel.create(
                IDE.INTELLIJ,
                Template.BAREBONES,
                "PackagingProbeApp",
                "com.example.packaging"
        ).writeProjectZip(output);
        return output.toByteArray();
    }

    private static Map<String, byte[]> readZipEntries(byte[] zipData) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ByteArrayInputStream input = new ByteArrayInputStream(zipData);
        ZipInputStream zis = new ZipInputStream(input);
        try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    Util.copyNoClose(zis, bos, 8192);
                    entries.put(entry.getName(), bos.toByteArray());
                    bos.close();
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        } finally {
            zis.close();
            input.close();
        }
        return entries;
    }

    private static void compileDisplayClass(File classesRoot) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("JDK compiler is required to run packaging test");
        }
        File sourceRoot = new File(classesRoot, "src");
        File packageDir = new File(sourceRoot, "com/example/packaging");
        packageDir.mkdirs();

        File javaFile = new File(packageDir, "DisplayClass.java");
        String source = "package com.example.packaging; public class DisplayClass {}";
        Files.write(javaFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, java.util.Collections.singletonList(classesRoot));
            Boolean ok = compiler.getTask(null, fileManager, null, null, null,
                    fileManager.getJavaFileObjectsFromFiles(java.util.Collections.singletonList(javaFile))).call();
            if (!Boolean.TRUE.equals(ok)) {
                throw new IOException("Failed to compile display class for packaging test");
            }
        } finally {
            fileManager.close();
            Util.cleanup(sourceRoot.getAbsolutePath());
        }
    }

    private static void writeGeneratedLocalizationResources(Map<String, byte[]> entries, File classesRoot) throws IOException {
        writeResource(entries, classesRoot, "messages.properties");
        writeResource(entries, classesRoot, "messages_en.properties");
        for (ProjectOptions.PreviewLanguage language : ProjectOptions.PreviewLanguage.values()) {
            if (language == ProjectOptions.PreviewLanguage.ENGLISH) {
                continue;
            }
            writeResource(entries, classesRoot, "messages_" + language.bundleSuffix + ".properties");
        }
    }

    private static void writeResource(Map<String, byte[]> entries, File classesRoot, String resourceName) throws IOException {
        byte[] data = entries.get("common/src/main/resources/" + resourceName);
        if (data == null) {
            throw new IOException("Generated project is missing localization resource " + resourceName);
        }
        try (FileOutputStream fos = new FileOutputStream(new File(classesRoot, resourceName))) {
            fos.write(data);
        }
    }

    private void assertResourcesLoadFromDisplayClass(File classesRoot) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{classesRoot.toURI().toURL()}, null);
        try {
            Class<?> displayClass = Class.forName("com.example.packaging.DisplayClass", true, loader);
            assertNotNull(displayClass.getResourceAsStream("/messages.properties"), "Default bundle should resolve from display class");
            assertNotNull(displayClass.getResourceAsStream("/messages_en.properties"), "English bundle alias should resolve from display class");
            for (ProjectOptions.PreviewLanguage language : ProjectOptions.PreviewLanguage.values()) {
                if (language == ProjectOptions.PreviewLanguage.ENGLISH) {
                    continue;
                }
                String resource = "/messages_" + language.bundleSuffix + ".properties";
                InputStream in = displayClass.getResourceAsStream(resource);
                assertNotNull(in, "Bundle should resolve from display class: " + resource);
                if (in != null) {
                    in.close();
                }
            }
        } finally {
            loader.close();
        }
    }
}
