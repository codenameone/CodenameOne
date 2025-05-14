/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import static com.codename1.maven.PathUtil.path;

/**
 * Mojo used in the compliance check.  This uses proguard strip out all unused classes, then check the remaining
 * classes to ensure that they don't use any APIs that aren't available in Codename One.
 *
 * @author shannah
 */
@Mojo(name = "compliance-check", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.TEST)
public class ComplianceCheckMojo extends AbstractCN1Mojo {

    private File complianceOutputFile;
    @Override
    public void executeImpl() throws MojoExecutionException, MojoFailureException {
        if ("true".equals(System.getProperty("skipComplianceCheck", "false"))) {
            return;
        }
        if ("true".equals(project.getProperties().getProperty("skipComplianceCheck", "false"))) {
            return;
        }
        if ("true".equals(System.getProperty("reloadClasses", "false"))) {
            return;
        }
        if ("true".equals(project.getProperties().getProperty("reloadClasses", "false"))) {
            return;
        }
        if (!isCN1ProjectDir()) {
            return;
        }
        complianceOutputFile = new File(path(project.getBuild().getDirectory(), "codenameone", "compliance_check.txt"));
        getLog().info("Running compliance check against Codename One Java Runtime API");
        getLog().info("See https://www.codenameone.com/javadoc/ for supported Classes and Methods");

        if (!hasChangedSinceLastCheck()) {
            getLog().info("Sources haven't changed since the last compliance check. Skipping check");
            return;
        }


        // Kotlin incrementable compilation seems to store its output in a different directory.
        // We need to copy it into the classes directory for proguard to work.
        copyKotlinIncrementalCompileOutputToOutputDir();

        // Run proguard.
        runProguard();
        complianceOutputFile.getParentFile().mkdirs();
        try {
            FileUtils.writeStringToFile(complianceOutputFile, "Completed compliance check on " + project.getName(), "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write compliance file");
        }
    }

    private boolean hasChangedSinceLastCheck() {

        if (!complianceOutputFile.exists()) {
            return true;
        }
        try {
            if (getSourcesModificationTime(true) > complianceOutputFile.lastModified()) {
                return true;
            }
        } catch (IOException ex) {
            getLog().error("Failed to check sources modification time for compliance check", ex);
        }

        return false;

    }

    /**
     * Runs proguard on the compiled classes directory in two passes.  The first pass disables warnings
     * and strips out all unused code.  The second pass runs the remaining code against the java-runtime library
     * and will result in an error if there is any API usage that isn't supported.
     * @throws MojoExecutionException
     */
    private void runProguard() throws MojoExecutionException {
        runProguard(0); // First pass strips code without warnings
        runProguard(1); // Second pass checks the stripped jar generated in the first pass to make sure there are 
                        // no warnings - which would result from missing classes
    }

    /**
     * Runs a single pass of proguard.
     * @param passNum The pass number.  0 or 1.  0 if this is the first pass.  1 if this is the second pass.
     *                In pass 0, it disables warnings and strips out all code that isn't used.  In pass 1, it
     *                enables warnings and checks remaining code against the java-runtime lib.
     * @throws MojoExecutionException
     */
    private void runProguard(int passNum) throws MojoExecutionException{
        Java java = createJava();
        Path classPath = java.createClasspath();
        classPath.setProject(antProject);
        for (File jar : getProguardJars()) {
            classPath.add(new Path(antProject, jar.getAbsolutePath()));
        }
        getLog().info("Proguard classpath: "+classPath);
        java.setClasspath(classPath);
        java.setClassname(proguardMainClass);
        java.setFailonerror(true);
        java.setFork(true);
        
        // The following proguard options are used for the first pass
        //        -verbose
        //            -dontobfuscate
        //            -libraryjars lib/CLDC11.jar:lib/CodenameOne.jar
        //            -injars build/tmp:lib/impl/cls
        //            -outjars build/tmp.jar
        //            -keep class ${codename1.packageName}.${codename1.mainName} {
        //            *;
        //            }
        //            -dontwarn **


        java.createArg().setValue("-dontobfuscate");
        if (passNum == 1) {
            //java.createArg().setValue("-verbose");
            java.createArg().setValue("-dontnote");
        } else {
            java.createArg().setValue("-dontnote");
            java.createArg().setValue("-dontwarn");  // First pass disables warnings because
                                                    // we aren't interested in warnings yet.  Just want to strip
                                                    // unused
        }

        // -library jars is the allowed APIs that they build against.
        // We build against java-runtime (formerly CLDC11.jar) and CodenameOne which
        // has the core Codename One api.
        java.createArg().setValue("-libraryjars");
        Path libraryJarsPath = new Path(antProject, getJavaRuntimeJar().getAbsolutePath());
        libraryJarsPath.add(new Path(antProject, getCodenameOneJar().getAbsolutePath()));
        java.createArg().setPath(libraryJarsPath);
        getLog().debug("Compliance check -libraryjars="+libraryJarsPath);
        File complianceCheckJar =  new File(project.getBuild().getDirectory() + File.separator + "compliance-check.jar");
        java.createArg().setValue("-keepattributes");
        java.createArg().setValue("Signature");

        // We need these options so that we are able to reference package-private stuff from user-space source.
        // This happens when testing (e.g. creating class in the com.codename1.ui package so that it can
        // access package-private stuff
        java.createArg().setValue("-dontskipnonpubliclibraryclasses");
        java.createArg().setValue("-dontskipnonpubliclibraryclassmembers");
        java.createArg().setValue("-dontoptimize");

        if (passNum == 0) {
            // The -injars parameter of proguard is the list of jars/directories that
            // we want to operate on.  We will operate on all dependencies, stripping out unused
            // code and place the result into a complianceCheck jar file.  This jar file
            // will be checked for consistency on the second pass.
            Path inJars = new Path(antProject, project.getBuild().getOutputDirectory());

            project.getArtifacts().forEach(artifact -> {
                getLog().info("artifact "+artifact);
                if (artifact.getGroupId().equals("com.codenameone") && artifact.getArtifactId().equals("codenameone-core")) {
                    return;
                }
                if (artifact.getGroupId().equals("com.codenameone") && artifact.getArtifactId().equals("java-runtime")) {
                    return;
                }
                if (artifact.getScope().equals("compile") || artifact.getScope().equals("system") || artifact.getScope().equals("test")) {
                    File jar = getJar(artifact);
                    if (jar != null) {
                        getLog().info("Adding to injars: " + jar);
                        inJars.add(new Path(antProject, getJar(artifact).getAbsolutePath()+"(!META-INF/**)"));
                    } else {
                        getLog().warn("No jar found for artifact "+artifact+".  This might cause problems for the compliance check");
                    }
                }
            });
            getLog().info("injars = "+inJars);
            java.createArg().setValue("-injars");
            java.createArg().setPath(inJars);

            java.createArg().setValue("-outjars");
            java.createArg().setPath(new Path(antProject, complianceCheckJar.getAbsolutePath()));
        } else if (passNum == 1) {
            // On pass 2 we only have one input: the compliance check jar that we generated in the first pass
            java.createArg().setValue("-injars");
            java.createArg().setPath(new Path(antProject, complianceCheckJar.getAbsolutePath()));
        }

        // The -keep parameter specifies which classes we need to keep.  These are used as a starting
        // point for proguard to crawl through the code and find out what is used.
        // For application projects, the starting point is just the main class.
        // For library projects, we keep all classes in the immediate project.

        if (properties != null && properties.getProperty("codename1.mainName") != null && !properties.getProperty("codename1.mainName").isEmpty()) {

            String keep = "class "+properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName")+" {\n" +
                "            *;\n" +
                "            }";
            //getLog().info("Addin -keep directive "+keep);
            java.createArg().setValue("-keep");
            java.createArg().setValue(keep);
            getLog().info("Keeping "+keep);


        } else {

            List<String> keeps = new ArrayList<String>();
            for (String sourceRoot : project.getCompileSourceRoots()) {
                File sourceRootFile = new File(sourceRoot);
                findClassesInDirectory(sourceRootFile.getAbsolutePath(), sourceRootFile, keeps);
            }
            getLog().info("Keep classes: "+keeps);
            for (String keepClass : keeps) {
                String keep = "class "+keepClass+" {\n" +
        "            *;\n" +
        "            }";
                //getLog().info("Addin -keep directive "+keep);
                java.createArg().setValue("-keep");
                java.createArg().setValue(keep);
            }
        }
        if (passNum == 0) {
            getLog().debug("Compliance check pass 0");
            // In the first pass we don't want any warnings or errors.
            // So let's pile on here anything necessary to make that happen.
            java.createArg().setValue("-dontwarn **");
            //java.createArg().setValue("**");
            java.createArg().setValue("-ignorewarnings");
        }
        if (getLog().isDebugEnabled()) {
            java.createArg().setValue("-verbose");
        }

        int result = java.executeJava();

        if (result != 0) {
            // The result will be non-zero if proguard ran into a problem of any kind, including
            // if there are APIs used that aren't in either the CodenameOne jar or the java-runtime jar.
            throw new MojoExecutionException("Compliance check failed");
        }

    }
    
    private void findClassesInDirectory(String sourceRootAbsolutePath, File file, List<String> out) {
        String fileName = file.getAbsolutePath();
        if (fileName.endsWith(".java") || fileName.endsWith(".kt")) {
            String className = fileName.substring(0, fileName.lastIndexOf("."))
                    .substring(sourceRootAbsolutePath.length())
                    .replace('/', '.')
                    .replace('\\', '.');
            if (className.startsWith(".")) {
                className = className.substring(1);
            }
            out.add(className);
        } else if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                findClassesInDirectory(sourceRootAbsolutePath, child, out);
            }
        }
    }

    /**
     * Gets the java-runtime jar (formerly CLDC11) which defines the supported API.
     * @return
     */
    private File getJavaRuntimeJar() {
        for (Artifact artifact : project.getArtifacts()) {
            if (JAVA_RUNTIME_ARTIFACT_ID.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        for (Artifact artifact : pluginArtifacts) {
            if (JAVA_RUNTIME_ARTIFACT_ID.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        throw new RuntimeException(JAVA_RUNTIME_ARTIFACT_ID + " not found in dependencies");
        
    }

    /**
     * Gets the codename one jar.
     * @return
     */
    private File getCodenameOneJar() {
        String codenameOneCoreId = "codenameone-core";
        for (Artifact artifact : project.getArtifacts()) {
            if (codenameOneCoreId.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        for (Artifact artifact : pluginArtifacts) {
            if (codenameOneCoreId.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        throw new RuntimeException(codenameOneCoreId + " not found in dependencies");
    }

    /**
     * Gets the proguard jars.
     * @return
     * @throws MojoExecutionException
     */
    private List<File> getProguardJars() throws MojoExecutionException {

        List<Artifact> proguardArtifacts = new ArrayList<Artifact>();
        int proguardArtifactDistance = -1;
        // This should be solved in Maven 2.1
        //Starting in v. 7.0.0., proguard got split up in proguard-base and proguard-core,
        //both of which need to be on the classpath.
        Artifact proguardBase = null;
        for (Artifact artifact : pluginArtifacts) {
            if (artifact.getArtifactId().equals("proguard-base")) {
                proguardBase = artifact;
                break;
            }
        }

        //getLog().info("Proguard dependencies are "+getArtifactsDependencies(proguardBase));
        for (Artifact artifact : pluginArtifacts) {
            getLog().debug("pluginArtifact: " + artifact.getFile());

            final String artifactId = artifact.getArtifactId();
            if (!(artifactId.equals("java-runtime") && artifact.getGroupId().equals("com.codenameone"))) {
                int distance = artifact.getDependencyTrail().size();
                getLog().debug("proguard DependencyTrail: " + distance);

                /*
				 *  Check if artifact has been defined twice - eg. no proguardVersion given but dependency for proguard
				 *  defined in plugin config
                 */
                for (Artifact existingArtifact : proguardArtifacts) {
                    if (existingArtifact.getArtifactId().equals(artifactId)) {
                        getLog().warn("Dependency for proguard defined twice! This may lead to unexpected results: "
                                + existingArtifact.getArtifactId() + ":" + existingArtifact.getVersion()
                                + " | "
                                + artifactId + ":" + artifact.getVersion());
                        break;
                    }
                }

                proguardArtifacts.add(artifact);

            }
        }
        if (!proguardArtifacts.isEmpty()) {
            List<File> resList = new ArrayList<File>(proguardArtifacts.size());
            for (Artifact p : proguardArtifacts) {
                getLog().debug("proguardArtifact: " + p.getFile());
                resList.add(p.getFile().getAbsoluteFile());
            }
            return resList;
        }
        getLog().info("proguard jar not found in pluginArtifacts");

        ClassLoader cl;
        cl = getClass().getClassLoader();
        // cl = Thread.currentThread().getContextClassLoader();
        String classResource = "/" + proguardMainClass.replace('.', '/') + ".class";
        URL url = cl.getResource(classResource);
        if (url == null) {
            throw new MojoExecutionException(
                    "Obfuscation failed ProGuard (" + proguardMainClass + ") not found in classpath");
        }
        String proguardJar = url.toExternalForm();
        if (proguardJar.startsWith("jar:file:")) {
            proguardJar = proguardJar.substring("jar:file:".length());
            proguardJar = proguardJar.substring(0, proguardJar.indexOf('!'));
        } else {
            throw new MojoExecutionException("Unrecognized location (" + proguardJar + ") in classpath");
        }
        return Collections.singletonList(new File(proguardJar));
    }

    private String proguardMainClass = "proguard.ProGuard";
}
