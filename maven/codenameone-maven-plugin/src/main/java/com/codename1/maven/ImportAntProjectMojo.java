/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import static com.codename1.maven.Cn1libMojo.CN1LIB_VERSION;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

/**
 * Imports an existing legacy ANT project into the current project.  Typical workflow would be to
 * first generate a project using archetype:generate, then run cn1:import-ant-project -Dcn1.sourceProject=/path/to/legacy-project
 * 
 * This leaves the legacy project untouched.
 * 
 * NOTE: This will overwrite all current settings in the maven project - effectively destroying it.
 * 
 * 
 * As a safety feature, this goal will fail if the maven project has an existing src directory.  Before running 
 * this goal, you need to delete or move the src directory.
 * @author shannah
 */
@Mojo(name = "import-ant-project")
public class ImportAntProjectMojo extends AbstractCN1Mojo {

    /**
     * The file system repository (located ad $basedir/repository where cn1libs
     * are installed.
     */
    private File repository;
    
    /**
     * A flag that is set to indicate that the pom file has already been backed up.
     */
    private boolean pomBackedUp;
    
    /**
     * The version of Kotlin to use for kotlin projects.
     */
    public static final String KOTLIN_VERSION="1.3.72";
    
    /**
     * The source ANT project.
     */
    private File sourceProject;
    
    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        
        String sourceProjectPath = project.getProperties().getProperty("cn1.sourceProject", System.getProperty("cn1.sourceProject"));
        if (sourceProjectPath == null) {
            getLog().error("Please specify the path to the ANT project you wish to import using -Dcn1.sourceProject=/path/to/project");
            throw new MojoExecutionException("Missing cn1.sourceProject parameter");
        }
        
        sourceProject = new File(sourceProjectPath);
        
        if (!sourceProject.exists()) {
            throw new MojoExecutionException("Source project must point to the root directory of a Codename One ant project.  The directory supplied doesn't exist. "+sourceProject);
        }
        
        // Just a safety feature:  If the current project has a "src" directory, then this goal will fail
        // Force them to delete or move the project src directory in order to run this goal.
        for (String root : project.getCompileSourceRoots()) {
            if (new File(root).exists()) {
                getLog().error("Before you can run the import-ant-project goal, you must delete or rename the source directory in this project.");
                getLog().error("Found existing source directory at "+root);
                throw new MojoExecutionException("Project import failed");
            }
        }
        
        // Find all cn1libs in the legacy project - convert them to maven format, and install them 
        // in file system repository inside the project.
        repository = new File(project.getBasedir() + File.separator + "repository");
        File tmpDir = new File(project.getBuild().getDirectory() + File.separator + "cn1libs");
        File libs = new File(sourceProject, "lib");
        for (File cn1lib : libs.listFiles()) {
            if (!cn1lib.getName().endsWith(".cn1lib")) {
                continue;
            }
            migrateLib(cn1lib, tmpDir, project.getVersion());
        }
        
        // Now migrate all of the source files.
        try {
            migrateSources();
        } catch (Exception ex) {
            getLog().error("Failed to migrate sources from "+sourceProject+" to "+project.getBasedir());
            throw new MojoExecutionException("Failed to migrate sources", ex);
        }
        
        // If it is a kotlin project, we need to activate kotlin.
        if (isKotlinProject()) {
            addKotlinDependencies();
        }
    }
    
    /**
     * Checks if the project (the legacy ant project) is a kotlin project.  Projects are deemed to be 
     * kotlin projects if they include the kotlin-runtime.cn1lib
     * @return 
     */
    private boolean isKotlinProject() {
        return new File(sourceProject + File.separator + "lib" + File.separator + "kotlin-runtime.cn1lib").exists();
    }
    
    /**
     * Replaces the first instance of patttern in inputString with replacement - Not using regex.
     * @param inputString
     * @param pattern
     * @param replacement
     * @return 
     */
    private String replaceFirst(String inputString, String pattern, String replacement) {
        int patternLen = pattern.length();
        int index = inputString.indexOf(pattern);
        if (index >= 0) {
            return inputString.substring(0, index) + replacement + inputString.substring(index+patternLen);
        }
        return inputString;
    }
    
    /**
     * Kotlin dependencies are added by simply adding the cn1.kotlin=true system property
     * to builds (or explicitly invoke the "kotlin" profile.  We will use the .mvn/jvm.config
     * file to set the cn1.kotlin system property by default.
     */
    private void addKotlinDependencies() throws MojoExecutionException {
        File mvnDir = new File(project.getBasedir() + File.separator + ".mvn");
        if (!mvnDir.exists()) {
            mvnDir.mkdir();
        }
        File jvmConfig = new File(mvnDir, "jvm.config");
        if (!jvmConfig.exists()) {
            try {
                jvmConfig.createNewFile();
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to add .mvn/jvm.config file", ex);
            }
        }
        try {
            String contents = FileUtils.readFileToString(jvmConfig);
            if (contents.contains("-Dcn1.kotlin=")) {
                contents = contents.replaceAll("-Dcn1\\.kotlin=(true|false)", "");
            }
            contents += " -Dcn1.kotlin=true";
            FileUtils.writeStringToFile(jvmConfig, contents);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to update .mvn/jvm.config file", ex);
        }

    }
    
    /**
     * Migrate all sources from the legacy project into the maven project.
     */
    private void migrateSources() throws Exception {
        File javaSources = new File(project.getCompileSourceRoots().get(0));
        File antSrcDir = new File(sourceProject, "src");
        if (antSrcDir.exists()) {
            
            File kotlinSources = new File(javaSources.getParentFile(), "kotlin");
            javaSources.mkdirs();
            if (javaSources.exists()) {
                Copy copy = (Copy)antProject.createTask("copy");
                copy.setOverwrite(true);
                copy.setTodir(javaSources);
                FileSet javaSourcesFilter = new FileSet();
                javaSourcesFilter.setProject(antProject);
                javaSourcesFilter.setDir(antSrcDir);
                javaSourcesFilter.setIncludes("**/*.java");
                copy.addFileset(javaSourcesFilter);
                copy.execute();
            }
            if (isKotlinProject()) {
                kotlinSources.mkdirs();
                if (kotlinSources.exists()) {
                    Copy copy = (Copy)antProject.createTask("copy");
                    copy.setOverwrite(true);
                    copy.setTodir(kotlinSources);
                    FileSet kotlinSourcesFilter = new FileSet();
                    kotlinSourcesFilter.setProject(antProject);
                    kotlinSourcesFilter.setDir(antSrcDir);
                    kotlinSourcesFilter.setIncludes("**/*.kt");
                    copy.addFileset(kotlinSourcesFilter);
                    copy.execute();
                }
            }

            File resourcesDir = new File(javaSources.getParentFile(), "resources");
            resourcesDir.mkdirs();
            if (resourcesDir.exists()) {
                Copy copy = (Copy)antProject.createTask("copy");
                copy.setOverwrite(true);
                copy.setTodir(resourcesDir);
                FileSet resourcesFilter = new FileSet();
                resourcesFilter.setProject(antProject);
                resourcesFilter.setDir(antSrcDir);
                resourcesFilter.setIncludes("**");
                resourcesFilter.setExcludes("**/*.kt,**/*.java");
                copy.addFileset(resourcesFilter);
                copy.execute();
            }
            
            File antCSSDirectory = new File(sourceProject, "css");
            if (antCSSDirectory.exists()) {
                File targetCSSDirectory = new File(javaSources.getParentFile(), "css");
                targetCSSDirectory.mkdirs();
                Copy copy = (Copy)antProject.createTask("copy");
                copy.setOverwrite(true);
                copy.setTodir(targetCSSDirectory);
                FileSet resourcesFilter = new FileSet();
                resourcesFilter.setProject(antProject);
                resourcesFilter.setDir(antCSSDirectory);
                resourcesFilter.setIncludes("**");
                copy.addFileset(resourcesFilter);
                copy.execute();
            }
            
            File antNativeDirectory = new File(sourceProject, "native");
            File nativeDirectory = new File(project.getBasedir() + File.separator + "native");
            if (antNativeDirectory.exists()) {
                if (nativeDirectory.exists()) {
                    FileUtils.deleteDirectory(nativeDirectory);

                }
                FileUtils.copyDirectory(antNativeDirectory, nativeDirectory);
                
            }
            
            
            File srcFile = new File(sourceProject, "codenameone_settings.properties");
            File destFile = new File(project.getBasedir() + File.separator + srcFile.getName());
            
            FileUtils.copyFile(srcFile, destFile);
            
            
            
            
            
        }
        File antResDirectory = new File(sourceProject, "res");
        File resDirectory = new File(javaSources.getParentFile(), "res");

        File guibuilderDirectory = new File(javaSources.getParentFile(), "guibuilder");
        if (antResDirectory.exists()) {
            FileUtils.copyDirectory(antResDirectory, resDirectory, true);
            File guibuilder  = new File(resDirectory, "guibuilder");
            if (guibuilder.exists()) {
                FileUtils.copyDirectory(guibuilder, guibuilderDirectory, true);
                FileUtils.deleteDirectory(guibuilder);
            } else {
                guibuilderDirectory.mkdirs();
            }
        } else {
            resDirectory.mkdirs();
            guibuilderDirectory.mkdirs();
        }
        
        
        File antTestDirectory = new File(sourceProject, "test");
        File javaTestSources = new File(project.getBasedir() + File.separator + "src" + File.separator + "test" + File.separator + "java");
        javaTestSources.mkdirs();
        if (antTestDirectory.exists()) {
            Copy copy = (Copy)antProject.createTask("copy");
            copy.setTodir(javaTestSources);
            FileSet fs = new FileSet();
            fs.setProject(antProject);
            fs.setDir(antTestDirectory);
            fs.setIncludes("**/*.java");
            copy.addFileset(fs);
            copy.execute();
            
            File kotlinTestSources = new File(javaTestSources.getParentFile(), "kotlin");
            kotlinTestSources.mkdirs();
            copy = (Copy)antProject.createTask("copy");
            copy.setTodir(kotlinTestSources);
            fs = new FileSet();
            fs.setProject(antProject);
            fs.setDir(antTestDirectory);
            fs.setIncludes("**/*.kt");
            copy.addFileset(fs);
            copy.execute();
            
            File testResources = new File(javaTestSources.getParentFile(), "resources");
            testResources.mkdirs();
            copy = (Copy)antProject.createTask("copy");
            copy.setTodir(testResources);
            fs = new FileSet();
            fs.setProject(antProject);
            fs.setDir(antTestDirectory);
            fs.setExcludes("**/*.kt,**/*.java");
            copy.addFileset(fs);
            copy.execute();
            
            
        }
        
        
        
        
    }
    
    
    private void migrateLib(File cn1lib, File targetDir, String version) throws MojoExecutionException {
        if (cn1lib.getName().equals("kotlin-runtime.cn1lib")) {
            getLog().info("Skipping migration of "+cn1lib+".  Will add official kotlin maven dependencies instead.");
            return;
        }
        File outputDir = new File(targetDir, cn1lib.getName().substring(0, cn1lib.getName().lastIndexOf(".")));
        if (outputDir.exists()) {
            getLog().info("Skipping migration of "+cn1lib+".  The output directory already exists");
            return;
        }
        
        
        
        Expand unzip = (Expand)antProject.createTask("unzip");
        unzip.setSrc(cn1lib);
        unzip.setDest(outputDir);
        unzip.execute();
        
        File metaInf = new File(outputDir, "META-INF");
        if (!metaInf.exists()) {
            metaInf.mkdir();
        }
        Manifest manifest = new Manifest();
        Attributes cn1Attributes = manifest.getAttributes("cn1lib");
        if (cn1Attributes == null) {
            cn1Attributes = new Attributes();
            manifest.getEntries().put("cn1lib", cn1Attributes);
        }
        cn1Attributes.putValue("Version", CN1LIB_VERSION);
        
        File manifestFile = new File(metaInf, "MANIFEST.MF");
        try (FileOutputStream fos = new FileOutputStream(manifestFile)) {
            manifest.write(fos);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to create manifest file", ex);
        }
        
        for (File child : outputDir.listFiles()) {
            File metaInfChild = new File(metaInf, "cn1lib" + File.separator + child.getName());
            
            if (child.getName().endsWith(".zip") || child.getName().endsWith(".properties")) {
                metaInfChild.getParentFile().mkdirs();
                child.renameTo(metaInfChild);
            } else {
                continue;
            }
            if (child.getName().equals("main.zip")) {
                unzip = (Expand)antProject.createTask("unzip");
                unzip.setSrc(metaInfChild);
                unzip.setDest(outputDir);
                unzip.execute();
                metaInfChild.delete();
            }
            if (child.getName().equals("stubs.zip")) {
                File sourcesZip = new File(targetDir, outputDir.getName() + "-" + version + "-sources.jar");
                metaInfChild.renameTo(sourcesZip);
            }
            if (child.getName().equals("nativese.zip")) {
                metaInfChild.renameTo(new File(metaInf, "cn1lib" + File.separator + "nativejavase.zip"));
            }
            if (child.getName().equals("nativeand.zip")) {
                metaInfChild.renameTo(new File(metaInf, "cn1lib" + File.separator + "nativeandroid.zip"));
            }
            
        }
        File outputFile = new File(targetDir, outputDir.getName() + "-" + version + ".jar");
        Zip zip = (Zip)antProject.createTask("zip");
        zip.setBasedir(outputDir);
        zip.setDestFile(outputFile);
        zip.execute();
        delTree(outputDir);
        
        InvocationRequest request = new DefaultInvocationRequest();
        //request.setPomFile( new File( "/path/to/pom.xml" ) );
        request.setGoals( Collections.singletonList( "install:install-file" ) );
        request.setBaseDirectory(project.getBasedir());
        Properties props = new Properties();
        props.setProperty("file", outputFile.getAbsolutePath());
        props.setProperty("groupId", project.getGroupId()+".cn1libs");
        props.setProperty("artifactId", outputDir.getName());
        props.setProperty("version", version);
        props.setProperty("packaging", "cn1lib");
        props.setProperty("localRepositoryPath", repository.getAbsolutePath());
        request.setProperties(props);
        if (!repository.exists()) {
            getLog().info("Creating local file system repository to store mavenized cn1libs at "+repository);
            repository.mkdirs();
        }

        
        Invoker invoker = new DefaultInvoker();
        try {
            getLog().info("Installing cn1lib "+outputDir+" into local repository");
            invoker.execute( request );
        } catch (MavenInvocationException ex) {
            getLog().error("Failed to install file "+outputFile);
            throw new MojoExecutionException(ex.getMessage(), ex);
                    
        }
        
        outputFile.delete();
        
        // Add the repository
        
        File pomFile = new File(project.getBasedir() + File.separator + "pom.xml");
        if (!pomFile.exists()) {
            throw new MojoExecutionException("Cannot add filesystem repository to pom file because the pom file "+pomFile+" could not be found.");
        }
        try {
           
            String contents = FileUtils.readFileToString(pomFile, "UTF-8");
            boolean changed = false;
            if (!contents.contains("<url>file://${project.basedir}/repository</url>")) {
                changed = true;
                contents = contents.replace("<repositories>", "<repositories>\n<repository>\n" +
"        <id>project-repository</id>\n" +
"        <url>file://${project.basedir}/repository</url>\n" +
"    </repository>\n");
            }
            
            
            if (!contents.contains("<artifactId>"+outputDir.getName()+"</artifactId>")) {
                changed = true;
                contents = contents.replaceFirst("</dependencies>", "    <dependency>\n"
                    + "                <groupId>"+project.getGroupId()+".cn1libs</groupId>\n"
                    + "                <artifactId>"+outputDir.getName()+"</artifactId>\n"
                    + "                <version>"+version+"</version>"
                    + "            </dependency>\n"
                    + "        </dependencies>");
            }
            
            if (changed) {
                
                if (!pomBackedUp) {
                    pomBackedUp = true;
                    File pomBackup = new File(pomFile.getAbsolutePath()+"."+System.currentTimeMillis()+".bak");
                    getLog().info("Backing up pom.xml to "+pomBackup);
                    FileUtils.copyFile(pomFile, pomBackup , true);
                }
                getLog().info("Adding dependency for library "+outputDir.getName()+" to pom.xml");
                FileUtils.writeStringToFile(pomFile, contents, "UTF-8");
            }
            
            
            
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to read pom file", ex);
        }
        
        
    }
    
}
