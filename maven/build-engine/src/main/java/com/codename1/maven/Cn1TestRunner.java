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
package com.codename1.maven;

import com.codename1.build.AntSupport;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.Log;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Runs a project's Codename One unit tests -- the classes implementing
/// `com.codename1.testing.UnitTest` -- in the JavaSE port's `TestRunner`.
///
/// The body of the Maven plugin's `test` goal and the Gradle plugin's `cn1Test`
/// task: find the tests, record them in `tests.dat` beside the test classes
/// (where the runner reads them from the classpath), and fork the runner.
public final class Cn1TestRunner {
    private static final int VERSION = 1;
    /// The runner's main class in the JavaSE port.
    public static final String RUNNER_MAIN_CLASS = "com.codename1.impl.javase.TestRunner";

    private final Log log;

    /// A runner logging to `log`.
    public Cn1TestRunner(Log log) {
        this.log = log;
    }

    /// Finds the tests under `testClassesDir` and writes `tests.dat` there.
    ///
    /// @param classpath everything the test classes need to load
    /// @return false when there are no tests
    public boolean prepare(File testClassesDir, List<File> classpath) throws BuildExecutionException {
        return prepare(java.util.Collections.singletonList(testClassesDir), testClassesDir, classpath);
    }

    /// Finds the tests under every one of `testClassesDirs` -- Gradle compiles
    /// Java and Kotlin tests into separate directories -- and writes `tests.dat`
    /// into `metadataDir`, which must be on the runner's classpath.
    ///
    /// @param classpath everything the test classes need to load
    /// @return false when there are no tests
    public boolean prepare(List<File> testClassesDirs, File metadataDir, List<File> classpath)
            throws BuildExecutionException {
        try {
            List<File> paths = new ArrayList<File>(testClassesDirs);
            paths.addAll(classpath);
            Class[] testCases = findTestCases(testClassesDirs, paths.toArray(new File[paths.size()]));
            if (testCases.length == 0) {
                return false;
            }
            File metadataFile = new File(metadataDir, "tests.dat");
            metadataFile.getParentFile().mkdirs();
            DataOutputStream fo = new DataOutputStream(new FileOutputStream(metadataFile));
            try {
                Arrays.sort(testCases, new Comparator<Class>() {
                    @Override
                    public int compare(Class t, Class t1) {
                        return String.CASE_INSENSITIVE_ORDER.compare(t.getName(), t1.getName());
                    }
                });
                fo.writeInt(VERSION);
                fo.writeInt(testCases.length);
                for (Class c : testCases) {
                    fo.writeUTF(c.getName());
                }
            } finally {
                fo.close();
            }
            return true;
        } catch (Exception ex) {
            throw new BuildExecutionException("Failed to prepare tests", ex);
        }
    }

    /// Forks the test runner.
    ///
    /// @param java the task to fork with, already set up for logging
    /// @param classpath the test classes, the classes, and every runtime jar
    ///        including the JavaSE port
    /// @param mainClass the application's main class, fully qualified
    /// @param reportsDir where the JUnit XML reports go
    /// @return the runner's exit status
    public static int run(Project antProject, Java java, List<File> classpath, String mainClass, File reportsDir) {
        Path cp = java.createClasspath();
        for (File f : classpath) {
            cp.add(new Path(antProject, f.getAbsolutePath()));
        }
        java.setClassname(RUNNER_MAIN_CLASS);
        java.createArg().setValue(mainClass);
        java.createArg().setValue("-junitXML");
        reportsDir.mkdirs();
        java.setDir(reportsDir);
        java.setFork(true);
        return java.executeJava();
    }

    /// [run(Project, Java, List, String, File)] with a task logging to `log`.
    public int run(File baseDir, List<File> classpath, String mainClass, File reportsDir) {
        Project ant = AntSupport.newProject(baseDir);
        return run(ant, AntSupport.createJava(ant, log, AntSupport.LEVEL_INFO), classpath, mainClass, reportsDir);
    }

    private Class[] findTestCases(List<File> testDirectories, File... classesDirectories)
            throws MalformedURLException, IOException {
        URL[] urls = new URL[classesDirectories.length];
        for(int iter = 0 ; iter < urls.length ; iter++) {
            try {
                urls[iter] = classesDirectories[iter].toURI().toURL();
            } catch (RuntimeException ex) {
                log.error("Failed to add class directory "+iter+" in directory list: "+Arrays.toString(urls));
                throw ex;
            }
        }
        URLClassLoader cl = new URLClassLoader(urls);
        
        List<Class> classList = new ArrayList<Class>();
        for (File testDirectory : testDirectories) {
            findTestCasesInDir(testDirectory.getAbsolutePath(), testDirectory, cl, classList);
        }

        Class[] arr = new Class[classList.size()];
        classList.toArray(arr);
        return arr;
    }
     
     private void findTestCasesInDir(String baseDir, File directory, URLClassLoader cl, List<Class> classList) throws IOException {
        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || (file.getName().endsWith(".class") && file.getName().indexOf('$') < 0)
                        || file.getName().endsWith(".jar");
            }
        });
        if (files == null) {
            return;
        }
        for(File f : files) {
            if(f.isDirectory()) {
                findTestCasesInDir(baseDir, f, cl, classList);
            } else {
                String fileName = f.getAbsolutePath();
                if(fileName.endsWith(".jar")) {
                    FileInputStream zipFile = new FileInputStream(fileName);
                    ZipInputStream zip = new ZipInputStream(zipFile);
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        //System.out.println("Extracting: " +entry);
                        if (entry.isDirectory()) {
                            continue;
                        }

                        String entryName = entry.getName();
                        if (entryName.endsWith(".class") && entryName.indexOf('$') < 0) {
                            String className = entryName.substring(baseDir.length() + 1, entryName.length() - 6);
                            className = className.replace('/', '.');
                            isTestCase(cl, className, classList);
                        } 
                    }
                    zip.close();
                } else {
                    String className = fileName.substring(baseDir.length() + 1, fileName.length() - 6);
                    className = className.replace(File.separatorChar, '.');
                    isTestCase(cl, className, classList);
                }
            }
        }
    }
     private boolean impl(Class cls) {
        for(Class current : cls.getInterfaces()) {
            if(current.getName().equals("com.codename1.testing.UnitTest")) {
                return true;
            }
        }
        Class parent = cls.getSuperclass();
        if(parent == Object.class || parent == null) {
            return false;
        }
        return impl(parent);
    }
    
    private boolean isTestCase(ClassLoader cl, String className, List<Class> classList) {
        try {
            Class cls = cl.loadClass(className);
            if(Modifier.isAbstract(cls.getModifiers())) {
                return false;
            }
            if(impl(cls)) {
                classList.add(cls);
                return true;
            }
        } catch(Throwable t) {
        }
        return false;
    }
    

}
