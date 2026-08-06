/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardening;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ProGuard;

/**
 * Drives ProGuard 7.3.x programmatically over the class-only jar to rename it and
 * emit the mapping. Everything is expressed as a generated {@code .pro} config, the
 * best-understood ProGuard interface. Shrinking and optimization are always off --
 * ParparVM culls and R8 shrinks, and enabling them here only risks release-only
 * breakage.
 */
public final class ProGuardRunner {

    private ProGuardRunner() {
    }

    /**
     * Renames {@code classesJar} into {@code outJar} and writes {@code mappingFile}.
     *
     * @param libraryJars the app's compile-scope libraries and port jars, so overrides are not
     *                    misrenamed; the JRE is added automatically
     * @param keepRules   the assembled Tier 1-3 keep rules
     * @param dictionary  the {@link Cn1NameFactory} dictionary used for classes, members and packages
     */
    public static void rename(File classesJar, File outJar, File mappingFile,
                              List<File> libraryJars, List<String> keepRules, File dictionary,
                              File workDir) throws HardeningException {
        File config = new File(workDir, "cn1-hardening.pro");
        try {
            writeConfig(config, classesJar, outJar, mappingFile, libraryJars, keepRules, dictionary);
        } catch (IOException e) {
            throw new HardeningException("Could not write ProGuard configuration", e);
        }

        Configuration configuration = new Configuration();
        ConfigurationParser parser = null;
        try {
            parser = new ConfigurationParser(config, System.getProperties());
            parser.parse(configuration);
        } catch (Exception e) {
            throw new HardeningException("ProGuard configuration is invalid: " + e.getMessage(), e);
        } finally {
            close(parser);
        }

        try {
            new ProGuard(configuration).execute();
        } catch (Exception e) {
            throw new HardeningException("ProGuard failed while renaming the application: "
                    + e.getMessage(), e);
        }
        if (!outJar.isFile()) {
            throw new HardeningException("ProGuard did not produce an output jar");
        }
    }

    private static void writeConfig(File config, File classesJar, File outJar, File mappingFile,
                                    List<File> libraryJars, List<String> keepRules, File dictionary)
            throws IOException {
        FileOutputStream fo = new FileOutputStream(config);
        try {
            Writer w = new OutputStreamWriter(fo, Charset.forName("UTF-8"));
            w.write("-injars " + quote(classesJar) + "\n");
            w.write("-outjars " + quote(outJar) + "\n");
            for (File lib : runtimeLibraryJars()) {
                w.write("-libraryjars " + quote(lib) + "\n");
            }
            if (libraryJars != null) {
                for (File lib : libraryJars) {
                    if (lib != null && lib.exists()) {
                        w.write("-libraryjars " + quote(lib) + "\n");
                    }
                }
            }
            w.write("-printmapping " + quote(mappingFile) + "\n");
            w.write("-classobfuscationdictionary " + quote(dictionary) + "\n");
            w.write("-obfuscationdictionary " + quote(dictionary) + "\n");
            w.write("-packageobfuscationdictionary " + quote(dictionary) + "\n");
            for (String flag : BuiltinKeepRules.flags()) {
                w.write(flag + "\n");
            }
            if (keepRules != null) {
                for (String rule : keepRules) {
                    w.write(rule + "\n");
                }
            }
            w.flush();
        } finally {
            fo.close();
        }
    }

    /** rt.jar on a JDK 8 runtime, or every jmod on a JDK 9+ runtime. */
    static List<File> runtimeLibraryJars() {
        List<File> jars = new ArrayList<File>();
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            return jars;
        }
        File home = new File(javaHome);
        File rt = new File(home, "lib/rt.jar");
        if (rt.isFile()) {
            jars.add(rt);
            File jce = new File(home, "lib/jce.jar");
            if (jce.isFile()) {
                jars.add(jce);
            }
            return jars;
        }
        File jmods = new File(home, "jmods");
        File[] mods = jmods.listFiles();
        if (mods != null) {
            for (File m : mods) {
                if (m.getName().endsWith(".jmod")) {
                    jars.add(m);
                }
            }
        }
        return jars;
    }

    private static String quote(File f) {
        return "'" + f.getAbsolutePath() + "'";
    }

    private static void close(ConfigurationParser parser) {
        if (parser != null) {
            try {
                parser.close();
            } catch (IOException ignore) {
                // best effort
            }
        }
    }
}
