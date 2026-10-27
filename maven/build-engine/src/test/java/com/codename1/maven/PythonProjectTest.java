/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import com.codename1.build.SystemStreamLog;
import com.codename1.builders.BuildException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// [PythonProjectImporter] and [PythonProjectBuilder]: what an import copies
/// and records, and what a build writes, keeps and removes.
public class PythonProjectTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File project;
    private File common;
    private File python;

    @Before
    public void project() throws Exception {
        project = tmp.newFolder("textstats");
        write(new File(project, "pyproject.toml"), "[project]\nname = \"textstats\"\n"
                + "dependencies = [\"attrs>=23\", 'python-dateutil ; python_version>\"3\"']\n\n"
                + "[project.scripts]\ntextstats = \"textstats.cli:main\"\n");
        write(new File(project, "requirements.txt"), "# pinned\nPyYAML==6.0.1\n-r other.txt\n");
        write(new File(project, "src/textstats/__init__.py"), "");
        write(new File(project, "src/textstats/cli.py"), "def main():\n    print('ran')\n    return 3\n");
        write(new File(project, "src/textstats/data.txt"), "not code");
        write(new File(project, "src/textstats/__pycache__/cli.cpython-313.pyc"), "x");
        write(new File(project, ".venv/lib/site.py"), "x = 1\n");
        write(new File(project, "tests/test_cli.py"), "x = 1\n");
        common = tmp.newFolder("app", "common");
        python = new File(common, "src/main/python");
        write(new File(common, "codenameone_settings.properties"), "codename1.packageName=com.acme\n");
    }

    @Test
    public void importsTheSourceLayoutAndTheConsoleScript() throws Exception {
        PythonProjectImporter.Result r = importer().importProject(project, common, "com.acme", "Stats");
        assertEquals("only the .py files of src/", 2, r.copiedFiles);
        assertTrue(new File(python, "textstats/cli.py").isFile());
        assertFalse(new File(python, "textstats/data.txt").exists());
        assertFalse(new File(python, "textstats/__pycache__").exists());
        assertFalse(new File(python, "tests").exists());
        assertEquals("textstats.cli:main", r.mainModule);
        assertEquals(Arrays.asList("PyYAML", "attrs", "python-dateutil"), r.requirements);
        String settings = read(new File(common, "codenameone_settings.properties"));
        assertTrue(settings, settings.startsWith("codename1.packageName=com.acme\n"));
        assertTrue(settings, settings.contains("codename1.python.mainModule=textstats.cli:main\n"));
        String main = read(new File(common, "src/main/java/com/acme/Stats.java"));
        assertTrue(main, main.contains("extends Lifecycle"));
        assertTrue(main, main.contains("PythonModules.runMain()"));
    }

    @Test
    public void reimportRemovesWhatTheProjectNoLongerHas() throws Exception {
        write(new File(project, "src/textstats/old.py"), "x = 1\n");
        importer().importProject(project, common, null, null);
        assertTrue(new File(python, "textstats/old.py").isFile());
        write(new File(python, "mine.py"), "y = 2\n");
        assertTrue(new File(project, "src/textstats/old.py").delete());
        PythonProjectImporter.Result r = importer().importProject(project, common, null, null);
        assertEquals(1, r.removedFiles);
        assertFalse(new File(python, "textstats/old.py").exists());
        assertTrue("a file the import did not write is kept", new File(python, "mine.py").isFile());
        assertFalse("no main class without a package and class name",
                new File(common, "src/main/java").exists());
    }

    @Test
    public void flatProjectsFindTheirEntryModule() throws Exception {
        File flat = tmp.newFolder("flat");
        write(new File(flat, "app.py"), "print(1)\n");
        assertEquals("app", PythonProjectImporter.mainModule(flat, null));
        write(new File(flat, "tool/__init__.py"), "");
        write(new File(flat, "tool/__main__.py"), "print(2)\n");
        assertEquals("tool.__main__", PythonProjectImporter.mainModule(flat, null));
        File lib = tmp.newFolder("lib");
        write(new File(lib, "util.py"), "x = 1\n");
        assertNull(PythonProjectImporter.mainModule(lib, null));
    }

    @Test
    public void notAPythonProject() throws Exception {
        try {
            importer().importProject(tmp.newFolder("empty"), common, null, null);
            fail();
        } catch (BuildException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("no Python sources"));
        }
    }

    @Test
    public void buildWritesOnlyWhatChangedAndRemovesWhatIsGone() throws Exception {
        File src = tmp.newFolder("py");
        File out = tmp.newFolder("gen");
        write(new File(src, "main.py"), "import helper\nprint(helper.twice(2))\n");
        write(new File(src, "helper.py"), "def twice(x):\n    return 2 * x\n");
        assertTrue(builder(src, out, null).run());
        File registry = new File(out, "com/codename1/generated/python/PythonModules.java");
        assertTrue(read(registry), read(registry).contains(".exec(\"__main__\")"));
        File helper = javaFor(out, "helper");
        assertTrue(helper.isFile());
        long stamp = helper.lastModified() - 10000;
        assertTrue(helper.setLastModified(stamp));
        write(new File(src, "main.py"), "import helper\nprint(helper.twice(3))\n");
        builder(src, out, null).run();
        assertEquals("an unchanged module is not rewritten", stamp, helper.lastModified());
        assertTrue(new File(src, "helper.py").delete());
        write(new File(src, "main.py"), "print(1)\n");
        builder(src, out, null).run();
        assertFalse(helper.exists());
        assertTrue(new File(src, "main.py").delete());
        assertFalse(builder(src, out, null).run());
        assertFalse("the output of a removed project is removed", registry.exists());
    }

    @Test
    public void functionEntryPointIsCalled() throws Exception {
        File src = tmp.newFolder("py");
        File out = tmp.newFolder("gen");
        write(new File(src, "pkg/__init__.py"), "");
        write(new File(src, "pkg/cli.py"), "def main():\n    return 0\n");
        builder(src, out, "pkg.cli:main").run();
        String entry = read(javaFor(out, PythonProjectBuilder.ENTRY_MODULE));
        assertTrue(entry, entry.contains("main"));
        assertTrue(read(new File(out, "com/codename1/generated/python/PythonModules.java")).contains("__main__"));
    }

    @Test
    public void unsupportedCodeNamesTheFileAndLine() throws Exception {
        File src = tmp.newFolder("py");
        write(new File(src, "main.py"), "x = 1\ny = eval('x')\n");
        try {
            builder(src, tmp.newFolder("gen"), null).run();
            fail();
        } catch (BuildException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("src/main/python/main.py:2:"));
        }
    }

    private static File javaFor(File out, String module) {
        return new File(out, "com/codename1/generated/python/"
                + com.codename1.python.transpiler.api.PythonTranspiler.className(module) + ".java");
    }

    private static PythonProjectImporter importer() {
        return new PythonProjectImporter(new SystemStreamLog());
    }

    private static PythonProjectBuilder builder(File src, File out, String main) {
        return new PythonProjectBuilder(src, out, main, new SystemStreamLog());
    }

    private static void write(File f, String text) throws IOException {
        Files.createDirectories(f.getParentFile().toPath());
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
