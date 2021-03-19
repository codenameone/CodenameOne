/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.invoker.*;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 * This mojo runs tests in the Codename One Test Runner.  This goal should be used in place of
 * the surefire plugin.
 * @author shannah
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class RunTestsMojo extends AbstractCN1Mojo {
    private static final int VERSION = 1;
    
    private File getMetaDataFile() {
        return new File(project.getBuild().getTestOutputDirectory()+ File.separator + "tests.dat");
    }

    private boolean isJavaSEProject() {
        return project.getArtifactId().endsWith("-javase");
    }

    private boolean isCommonProject() {
        return project.getArtifactId().endsWith("-common");
    }

    private File getCommonProjectBaseDir() {
        if (isJavaSEProject()) {
            return new File(project.getParent().getBasedir(), "common");
        }
        if (isCommonProject()) {
            return project.getBasedir();
        }
        throw new IllegalStateException("Cannot get common project in this context");
    }

    private boolean prepareTests() throws MojoExecutionException {


        // At this point, we are running inside the common project.
        try {
            List<File> paths = new ArrayList<File>();
            File cn1ProjectDir = getCN1ProjectDir();
            
            paths.add(new File(project.getBuild().getTestOutputDirectory()));
            paths.add(new File(project.getBuild().getOutputDirectory()));
            
            for (Artifact artifact : project.getArtifacts()) {
                File jar = getJar(artifact);
                if (jar != null) {
                    paths.add(jar);
                } else {
                    getLog().warn("Failed to resolve artifact "+artifact);
                }
            }
            Class[] testCases = findTestCases(paths.toArray(new File[paths.size()]));
            if (testCases.length == 0) {
                return false;
            }
            File metadataFile = getMetaDataFile();
            metadataFile.getParentFile().mkdir();
            DataOutputStream fo = new DataOutputStream(new FileOutputStream(metadataFile));
            
            Arrays.sort(testCases, new Comparator<Class>() {
                @Override
                public int compare(Class t, Class t1) {
                    return String.CASE_INSENSITIVE_ORDER.compare(t.getName(), t1.getName());
                }
            });
            
            fo.writeInt(VERSION);
            
            fo.writeInt(testCases.length);
            
            for(Class c : testCases) {
                fo.writeUTF(c.getName());
            }
            
            fo.close();
            return true;
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to prepare tests", ex);
        }
    }
    
    private Class[] findTestCases(File... classesDirectories) throws MalformedURLException, IOException {
        URL[] urls = new URL[classesDirectories.length];
        for(int iter = 0 ; iter < urls.length ; iter++) {
            try {
                urls[iter] = classesDirectories[iter].toURI().toURL();
            } catch (RuntimeException ex) {
                getLog().error("Failed to add class directory "+iter+" in directory list: "+Arrays.toString(urls));
                throw ex;
            }
        }
        URLClassLoader cl = new URLClassLoader(urls);
        
        // first directory is assumed to be the test classes directory
        File userClassesDirectory = classesDirectories[0];
        
        List<Class> classList = new ArrayList<Class>();
        findTestCasesInDir(userClassesDirectory.getAbsolutePath(), userClassesDirectory, cl, classList);

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
    

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        copyKotlinIncrementalCompileOutputToOutputDir();
        if (!prepareTests()) {
            getLog().info("No tests were found.");
            return;
        }
        Java java = createJava();
        Path cp = java.createClasspath();
        cp.add(new Path(antProject, project.getBuild().getTestOutputDirectory()));
        cp.add(new Path(antProject, project.getBuild().getOutputDirectory()));
        for (Artifact artifact : project.getArtifacts()) {
            if ("provided".equals(artifact.getScope())) {
                continue;
            }
            //if (artifact.getScope().equals("compile") || artifact.getScope().equals("system") || artifact.getScope().equals("test")) {
            File jar = getJar(artifact);
            if (jar != null) {
                cp.add(new Path(antProject, jar.getAbsolutePath()));
            } else {
                getLog().warn("Failed to resolve artifact: "+artifact);
            }
        }
        cp.add(new Path(antProject, getProjectInternalTmpJar().getAbsolutePath()));
        if (!isCefSetup()) {
            setupCef();
        }
        java.createJvmarg().setValue("-Dcef.dir="+System.getProperty("cef.dir", System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "cef"));
        java.setClassname("com.codename1.impl.javase.TestRunner");
        java.createArg().setValue(properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));
        java.setFork(true);
        int result = java.executeJava();
        if (result != 0) {
            throw new MojoExecutionException("Tests failed");
        }
        
        
    }
    
}
