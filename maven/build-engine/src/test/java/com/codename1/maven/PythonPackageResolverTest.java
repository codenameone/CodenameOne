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
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// [PythonRequirement], [PythonPackageResolver] and [PythonExtensionStubs]
/// against an index served from memory.
public class PythonPackageResolverTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final Map<String, byte[]> served = new HashMap<String, byte[]>();
    private final Map<String, JSONObject> releases = new HashMap<String, JSONObject>();

    private void publish(String name, String version, String file, byte[] data) {
        String url = "https://files.example/" + file;
        served.put(url, data);
        JSONObject rel = releases.get(name);
        if (rel == null) {
            rel = new JSONObject();
            releases.put(name, rel);
        }
        JSONArray files = rel.optJSONArray(version);
        if (files == null) {
            files = new JSONArray();
            rel.put(version, files);
        }
        files.put(new JSONObject().put("filename", file).put("url", url).put("size", data.length)
                .put("digests", new JSONObject().put("sha256", PythonPackageResolver.sha256(data))));
    }

    private PythonPackageResolver resolver() {
        return new PythonPackageResolver(new SystemStreamLog(), url -> {
            String prefix = "https://pypi.org/pypi/";
            if (url.startsWith(prefix) && url.endsWith("/json")) {
                String name = url.substring(prefix.length(), url.length() - 5);
                JSONObject rel = releases.get(name);
                if (rel == null) {
                    throw new IOException(url + " answered 404");
                }
                return new JSONObject().put("releases", rel).toString().getBytes(StandardCharsets.UTF_8);
            }
            byte[] b = served.get(url);
            if (b == null) {
                throw new IOException(url + " answered 404");
            }
            return b;
        });
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    public void versionsOrderAsPep440() {
        String[] ordered = {"1.0.dev1", "1.0a1", "1.0b2", "1.0rc1", "1.0", "1.0.post1", "1.1", "2!0.1"};
        for (int i = 1; i < ordered.length; i++) {
            assertTrue(ordered[i - 1] + " < " + ordered[i], PythonRequirement.compare(ordered[i - 1], ordered[i]) < 0);
        }
        assertEquals(0, PythonRequirement.compare("1.0", "1.0.0"));
    }

    @Test
    public void specifiersAndMarkers() {
        PythonRequirement r = PythonRequirement.parse("Foo_Bar[x] >=1.2, !=1.3.*, <2 ; python_version >= '3.8'");
        assertEquals("foo-bar", r.name);
        assertEquals(Arrays.asList("x"), r.extras);
        assertTrue(r.accepts("1.2"));
        assertFalse(r.accepts("1.3.4"));
        assertFalse(r.accepts("2.0"));
        assertFalse("no pre-release unless asked", r.accepts("1.9b1"));
        assertTrue(PythonRequirement.parse("x ~=1.4.2").accepts("1.4.9"));
        assertFalse(PythonRequirement.parse("x ~=1.4.2").accepts("1.5"));
        assertTrue(r.applies(Collections.<String>emptyList()));
        assertFalse(PythonRequirement.parse("pywin32 ; sys_platform == 'win32'").applies(Collections.<String>emptyList()));
        PythonRequirement extra = PythonRequirement.parse("socks ; extra == 'socks'");
        assertFalse(extra.applies(Collections.<String>emptyList()));
        assertTrue(extra.applies(Arrays.asList("socks")));
    }

    @Test
    public void resolvesTransitivelyAndStubsCompiledModules() throws Exception {
        Map<String, String> app = new LinkedHashMap<String, String>();
        app.put("app/__init__.py", "from fastlib import fast_sum\n");
        app.put("app-1.0.dist-info/METADATA", "Name: app\nVersion: 1.0\nRequires-Dist: fastlib>=2\n"
                + "Requires-Dist: requests\nRequires-Dist: winonly; sys_platform == 'win32'\n\nbody\n");
        publish("app", "1.0", "app-1.0-py3-none-any.whl", zip(app));
        publish("app", "1.1b1", "app-1.1b1-py3-none-any.whl", zip(app));

        Map<String, String> fast = new LinkedHashMap<String, String>();
        fast.put("fastlib/__init__.py", "from ._core import fast_sum, Vector\nfrom . import _io as io\n"
                + "def total(xs):\n    return io.read(xs) + io.SIZE\n"
                + "try:\n    from ._speedups import escape\nexcept ImportError:\n    def escape(s):\n        return s\n");
        fast.put("fastlib/_core.cpython-313-x86_64-linux-gnu.so", "ELF");
        fast.put("fastlib/_io.cpython-313-x86_64-linux-gnu.so", "ELF");
        fast.put("fastlib/_speedups.cpython-313-x86_64-linux-gnu.so", "ELF");
        fast.put("fastlib-2.1.dist-info/METADATA", "Name: fastlib\nVersion: 2.1\n");
        publish("fastlib", "1.9", "fastlib-1.9-py3-none-any.whl", zip(fast));
        publish("fastlib", "2.1", "fastlib-2.1-cp313-cp313-manylinux_2_17_x86_64.whl", zip(fast));

        File common = tmp.newFolder("common");
        PythonPackageResolver.Result r = resolver().resolve(Arrays.asList("app"), common);
        assertEquals(Arrays.asList("app==1.0", "fastlib==2.1"), r.installed);
        assertEquals(Arrays.asList("requests"), r.provided);
        assertEquals(Arrays.asList("fastlib._speedups"), r.optionalNativeModules);
        assertEquals("com.codename1.python.ext.FastlibCore", r.nativeModules.get("fastlib._core"));
        assertEquals("com.codename1.python.ext.FastlibIo", r.nativeModules.get("fastlib._io"));

        File pkgs = new File(common, "src/main/python-packages");
        assertTrue(new File(pkgs, "fastlib/__init__.py").isFile());
        assertFalse(new File(pkgs, "app-1.0.dist-info").exists());
        String lock = new String(Files.readAllBytes(new File(pkgs, "python.lock").toPath()), StandardCharsets.UTF_8);
        assertTrue(lock, lock.contains("fastlib==2.1 file=fastlib-2.1-cp313-cp313-manylinux_2_17_x86_64.whl"));

        String core = new String(Files.readAllBytes(new File(common,
                "src/main/java/com/codename1/python/ext/FastlibCore.java").toPath()), StandardCharsets.UTF_8);
        assertTrue(core, core.contains("new NativeModule(\"fastlib._core\")"));
        assertTrue(core, core.contains(".function(\"Vector\"") && core.contains(".function(\"fast_sum\""));
        String io = new String(Files.readAllBytes(new File(common,
                "src/main/java/com/codename1/python/ext/FastlibIo.java").toPath()), StandardCharsets.UTF_8);
        assertTrue(io, io.contains(".function(\"SIZE\"") && io.contains(".function(\"read\""));

        // A filled-in stub survives a second import, which installs from the lock.
        Files.write(new File(common, "src/main/java/com/codename1/python/ext/FastlibCore.java").toPath(),
                "// mine\n".getBytes(StandardCharsets.UTF_8));
        publish("fastlib", "2.2", "fastlib-2.2-cp313-cp313-manylinux_2_17_x86_64.whl", zip(fast));
        PythonPackageResolver.Result again = resolver().resolve(Arrays.asList("app"), common);
        assertTrue(again.installed.contains("fastlib==2.1"));
        assertFalse(again.stubsWritten.contains("com.codename1.python.ext.FastlibCore"));
        assertEquals("// mine\n", new String(Files.readAllBytes(new File(common,
                "src/main/java/com/codename1/python/ext/FastlibCore.java").toPath()), StandardCharsets.UTF_8));
        assertEquals(PythonExtensionStubs.readIndex(new File(pkgs, PythonExtensionStubs.INDEX)), r.nativeModules);
    }

    @Test
    public void rejectsATamperedDownload() throws Exception {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("lib/__init__.py", "x = 1\n");
        publish("lib", "1.0", "lib-1.0-py3-none-any.whl", zip(m));
        served.put("https://files.example/lib-1.0-py3-none-any.whl", "evil".getBytes(StandardCharsets.UTF_8));
        try {
            resolver().resolve(Arrays.asList("lib"), tmp.newFolder("c"));
            fail();
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("SHA-256"));
        }
    }

    @Test
    public void conflictingRequirementsFail() throws Exception {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("lib/__init__.py", "x = 1\n");
        publish("lib", "1.0", "lib-1.0-py3-none-any.whl", zip(m));
        try {
            resolver().resolve(Arrays.asList("lib>=2"), tmp.newFolder("c"));
            fail();
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("No release of lib"));
        }
    }

    @Test
    public void requirementSpecsKeepVersions() {
        assertEquals(Arrays.asList("requests[socks]>=2", "attrs", "numpy==2.1"),
                PythonProjectImporter.requirementSpecs(
                        "[project]\nname = \"x\"\ndependencies = [\"requests[socks]>=2\", 'attrs']\n",
                        "# c\nnumpy==2.1  # pinned\n-r other.txt\n"));
    }
}
