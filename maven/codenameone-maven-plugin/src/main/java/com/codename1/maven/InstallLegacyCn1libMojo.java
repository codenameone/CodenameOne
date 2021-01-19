/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.tools.ant.taskdefs.Expand;

/**
 *
 * @author shannah
 */
@Mojo(name = "install-cn1lib")
public class InstallLegacyCn1libMojo extends AbstractCN1Mojo {

    private File localProjectRepository;
    
    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        String cn1libPath = System.getProperty("file", null);
        if (cn1libPath == null) {
            throw new MojoExecutionException("Please specify path to cnlib using -Dfile=/path/to/mylib.cn1lib");
        }
        File cn1libFile = new File(cn1libPath);
        if (!cn1libFile.exists()) {
            throw new MojoExecutionException("The file "+cn1libFile+" cannot be found.");
        }
        
        if (!cn1libFile.getName().endsWith(".cn1lib")) {
            throw new MojoExecutionException("File must have .cn1lib extension");
        }
        
        File repository = new File(project.getBasedir(), "cn1libs");
        if (!repository.exists()) {
            repository.mkdirs();
        }
        localProjectRepository = repository;
        
        File generatedSources = new File(project.getBuild().getDirectory() + File.separator + "generated-sources");
        generatedSources.mkdirs();
        
        String libName = cn1libFile.getName().substring(0, cn1libFile.getName().lastIndexOf("."));
        File libOutputDir = new File(generatedSources, libName);
        if (libOutputDir.exists()) {
            try {
                FileUtils.deleteDirectory(libOutputDir);
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to delete directory.", ex);
            }
        }
        libOutputDir.mkdirs();
        
        File libOutputJars = new File(libOutputDir, "jars");
        Expand unzip = (Expand)antProject.createTask("unzip");
        unzip.setSrc(cn1libFile);
        unzip.setDest(libOutputJars);
        unzip.execute();
        
        
        
        String groupId = project.getGroupId() + ".cn1libs";
        String artifactId = project.getArtifactId() + "-" +libName;
        
        File seJar = new File(libOutputJars, "nativese.zip");
        if (seJar.exists()) {
            installFile(seJar, groupId, artifactId, project.getVersion(), "javase");
        }
        
        File androidJar = new File(libOutputJars, "nativeand.zip");
        if (androidJar.exists()) {
            installFile(androidJar, groupId, artifactId, project.getVersion(), "android");
        }
        
        File iosJar = new File(libOutputJars, "nativeios.zip");
        if (iosJar.exists()) {
            installFile(iosJar, groupId, artifactId, project.getVersion(), "ios");
        }
        
        File jsJar = new File(libOutputJars, "nativejavascript.zip");
        if (jsJar.exists()) {
            installFile(jsJar, groupId, artifactId, project.getVersion(), "javascript");
        }
        
        File mainJar = new File(libOutputJars, "main.zip");
        if (mainJar.exists()) {
            installFile(mainJar, groupId, artifactId, project.getVersion(), "common");
        }
        
        File winJar = new File(libOutputJars, "main.zip");
        if (winJar.exists()) {
            installFile(winJar, groupId, artifactId, project.getVersion(), "win");
        }
        
        
        
        
        
        String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "\n" +
            "  <groupId>"+groupId+"</groupId>\n" +
            "  <artifactId>"+artifactId+"</artifactId>\n" +
            "  <version>"+project.getVersion()+"</version>\n" +
            "  <packaging>pom</packaging>\n" +
                "  <name>"+libName+"</name>\n" +
            "  <dependencies>\n" +
                (mainJar.exists()? (
                 "<dependency>\n"
                + "     <groupId>"+groupId+"</groupId>\n"
                + "     <artifactId>"+artifactId+"</artifactId>\n"
                + "     <version>"+project.getVersion()+"</version>\n"
                + "     <classifier>common</classifier>\n"
                + "     <type>jar</type>\n"
                + "</dependency>\n") : "") 
                +"</dependencies>\n"
                +"<profiles>\n"
                + (seJar.exists() ? profileXml("javase", groupId, artifactId) : "")
                + (androidJar.exists() ? profileXml("android", groupId, artifactId) : "")
                + (iosJar.exists() ? profileXml("ios", groupId, artifactId): "")
                + (jsJar.exists() ? profileXml("javascript", groupId, artifactId) : "")
                + (winJar.exists() ? profileXml("win", groupId, artifactId) : "")
                + "</profiles>\n"
                + "</project>"
                ;
        
        File pomFile = new File(libOutputDir, "pom.xml");
        try {
            FileUtils.writeStringToFile(pomFile, pomXml, "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException("Cannot write pom.xml file", ex);
        }
        
        installFile(pomFile, groupId, artifactId, project.getVersion(), null, "pom");
        

        
        
    }
    
    private String profileXml(String platform, String groupId, String artifactId) {
        return  "  <profile>\n"
                + "    <id>"+platform+"</id>"
                + "     <activation>"
                + "         <property><name>codename1.platform</name><value>"+platform+"</value></property>\n"
                + "     </activation>"
                + "     <dependencies>"
                + "         <dependency>"
                + "             <groupId>"+groupId+"</groupId>\n"
                + "             <artifactId>"+artifactId+"</artifactId>\n"
                + "             <version>"+project.getVersion()+"</artifactId>\n"
                + "             <type>jar</type>\n"
                + "             <classifier>"+platform+"</classifier>\n"
                + "         </dependency>\n"
                + "     </dependencies>\n"
                + "  </profile>";
    }
    
     private void installFile(File file, String groupId, String artifactId, String version, String classifier) throws MojoExecutionException {
         installFile(file, groupId, artifactId, version, classifier, "jar");
     }
    
    private void installFile(File file, String groupId, String artifactId, String version, String classifier, String packaging) throws MojoExecutionException {
        InvocationRequest request = new DefaultInvocationRequest();
        //request.setPomFile( new File( "/path/to/pom.xml" ) );
        
        request.setGoals( Collections.singletonList( "install:install-file" ) );
        
        Properties props = new Properties();
        props.setProperty("file", file.getAbsolutePath());
        props.setProperty("groupId", groupId);
        props.setProperty("artifactId", artifactId);
        props.setProperty("packaging", packaging);
        props.setProperty("version", version);
        if (classifier != null) props.setProperty("classifier", classifier);
        props.setProperty("localRepositoryPath", localProjectRepository.getAbsolutePath());
                
        
        
        request.setProperties(props);
        
        Invoker invoker = new DefaultInvoker();
        try {
            invoker.execute( request );
        } catch (MavenInvocationException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
                    
        }
    }
    
    
    
}
