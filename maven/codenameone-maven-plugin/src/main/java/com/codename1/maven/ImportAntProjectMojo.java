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
 *
 * @author shannah
 */
@Mojo(name = "import-ant-project")
public class ImportAntProjectMojo extends AbstractCN1Mojo {

    private File repository;
    private boolean pomBackedUp;
    public static final String KOTLIN_VERSION="1.3.72";
    
    
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
        for (String root : project.getCompileSourceRoots()) {
            if (new File(root).exists()) {
                getLog().error("Before you can run the import-ant-project goal, you must delete or rename the source directory in this project.");
                getLog().error("Found existing source directory at "+root);
                throw new MojoExecutionException("Project import failed");
            }
        }
        repository = new File(project.getBasedir() + File.separator + "repository");
        File tmpDir = new File(project.getBuild().getDirectory() + File.separator + "cn1libs");
        File libs = new File(sourceProject, "lib");
        for (File cn1lib : libs.listFiles()) {
            if (!cn1lib.getName().endsWith(".cn1lib")) {
                continue;
            }
            migrateLib(cn1lib, tmpDir, "1.0-SNAPSHOT");
        }
        try {
            migrateSources();
        } catch (Exception ex) {
            getLog().error("Failed to migrate sources from "+sourceProject+" to "+project.getBasedir());
            throw new MojoExecutionException("Failed to migrate sources", ex);
        }
        
        if (isKotlinProject()) {
            addKotlinDependencies();
        }
    }
    
    
    private boolean isKotlinProject() {
        return new File(sourceProject + File.separator + "lib" + File.separator + "kotlin-runtime.cn1lib").exists();
    }
    
    private String replaceFirst(String inputString, String pattern, String replacement) {
        int patternLen = pattern.length();
        int index = inputString.indexOf(pattern);
        if (index >= 0) {
            return inputString.substring(0, index) + replacement + inputString.substring(index+patternLen);
        }
        return inputString;
    }
    
    private void addKotlinDependencies() throws MojoExecutionException {
        File pomFile = new File(project.getBasedir() + File.separator + "pom.xml");
        if (!pomFile.exists()) {
            throw new MojoExecutionException("Cannot add kotlin dependencies to pom file "+pomFile+" because the file could not be found.");
        }
        try {
           
            String contents = FileUtils.readFileToString(pomFile, "UTF-8");
            boolean changed = false;
            
            
            if (!contents.contains("<artifactId>kotlin-stdlib</artifactId>")) {
                changed = true;
                
                contents = replaceFirst(contents, "</dependencies>", "<dependency>\n" +
"		        <groupId>org.jetbrains.kotlin</groupId>\n" +
"		        <artifactId>kotlin-stdlib</artifactId>\n" +
"		        <version>${kotlin.version}</version>\n" +
"		    </dependency>"
                    + "        </dependencies>");
            }
            if (!contents.contains("<kotlin.version>")) {
                changed = true;
                contents = replaceFirst(contents, "<properties>", "<properties>\n"
                        + "<kotlin.version>"+KOTLIN_VERSION+"</kotlin.version>\n"
                                + "");
            }
            
            if (!contents.contains("<artifactId>kotlin-maven-plugin</artifactId>")) {
                changed = true;
                contents = replaceFirst(contents, "</plugin>", "</plugin>\n"
                + "<plugin>\n" +
"			            <groupId>org.jetbrains.kotlin</groupId>\n" +
"			            <artifactId>kotlin-maven-plugin</artifactId>\n" +
"			            <version>${kotlin.version}</version>\n" +
"			            <executions>\n" +
"			                <execution>\n" +
"			                    <id>compile</id>\n" +
"			                    <goals>\n" +
"			                        <goal>compile</goal>\n" +
"			                    </goals>\n" +
"			                    <configuration>\n" +
"			                        <sourceDirs>\n" +
"			                            <sourceDir>${project.basedir}/src/main/kotlin</sourceDir>\n" +
"			                            <sourceDir>${project.basedir}/src/main/java</sourceDir>\n" +
"			                        </sourceDirs>\n" +
"			                    </configuration>\n" +
"			                </execution>\n" +
"			                <execution>\n" +
"			                    <id>test-compile</id>\n" +
"			                    <goals> <goal>test-compile</goal> </goals>\n" +
"			                    <configuration>\n" +
"			                        <sourceDirs>\n" +
"			                            <sourceDir>${project.basedir}/src/test/kotlin</sourceDir>\n" +
"			                            <sourceDir>${project.basedir}/src/test/java</sourceDir>\n" +
"			                        </sourceDirs>\n" +
"			                    </configuration>\n" +
"			                </execution>\n" +
"			            </executions>\n" +
"			        </plugin>\n" +
"			        <plugin>\n" +
"			            <groupId>org.apache.maven.plugins</groupId>\n" +
"			            <artifactId>maven-compiler-plugin</artifactId>\n" +
"			            <version>3.5.1</version>\n" +
"			            <executions>\n" +
"			                <!-- Replacing default-compile as it is treated specially by maven -->\n" +
"			                <execution>\n" +
"			                    <id>default-compile</id>\n" +
"			                    <phase>none</phase>\n" +
"			                </execution>\n" +
"			                <!-- Replacing default-testCompile as it is treated specially by maven -->\n" +
"			                <execution>\n" +
"			                    <id>default-testCompile</id>\n" +
"			                    <phase>none</phase>\n" +
"			                </execution>\n" +
"			                <execution>\n" +
"			                    <id>java-compile</id>\n" +
"			                    <phase>compile</phase>\n" +
"			                    <goals>\n" +
"			                        <goal>compile</goal>\n" +
"			                    </goals>\n" +
"			                </execution>\n" +
"			                <execution>\n" +
"			                    <id>java-test-compile</id>\n" +
"			                    <phase>test-compile</phase>\n" +
"			                    <goals>\n" +
"			                        <goal>testCompile</goal>\n" +
"			                    </goals>\n" +
"			                    <configuration>\n" +
"			                        <skip>${maven.test.skip}</skip>\n" +
"			                    </configuration>\n" +
"			                </execution>\n" +
"			            </executions>\n" +
"			        </plugin>");
            }
            
            if (changed) {
                
                if (!pomBackedUp) {
                    pomBackedUp = true;
                    File pomBackup = new File(pomFile.getAbsolutePath()+"."+System.currentTimeMillis()+".bak");
                    getLog().info("Backing up pom.xml to "+pomBackup);
                    FileUtils.copyFile(pomFile, pomBackup , true);
                }
                getLog().info("Kotlin dependencies to pom.xml");
                FileUtils.writeStringToFile(pomFile, contents, "UTF-8");
            }
            
            
            
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to read pom file", ex);
        }
    }
    
    private void migrateSources() throws Exception {
        File antSrcDir = new File(sourceProject, "src");
        if (antSrcDir.exists()) {
            File javaSources = new File(project.getCompileSourceRoots().get(0));
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
                    + "            <dependency>\n" +
                      "                <groupId>org.jetbrains</groupId>\n" +
                      "                <artifactId>annotations</artifactId>\n" +
                      "                <version>13.0</version>\n" +
                      "            </dependency>"
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
