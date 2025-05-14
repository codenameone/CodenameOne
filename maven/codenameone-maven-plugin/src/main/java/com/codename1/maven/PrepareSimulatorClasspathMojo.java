/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;

import static com.codename1.maven.PathUtil.path;

/**
 * A mojo that should be called sometime before simulator runs.  It sets properties
 * that the simulator requires to run properly, including:
 * codename1.mainClass
 * codename1.css.compiler.args.input
 * codename1.css.compiler.args.output
 * codename1.css.compiler.args.output
 *
 * @author shannah
 */
@Mojo(name = "prepare-simulator-classpath", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PrepareSimulatorClasspathMojo extends AbstractCN1Mojo {

    private boolean filterByName(Artifact artifact) {
        if (GROUP_ID.equals(artifact.getGroupId())) {
            if ("java-runtime".equals(artifact.getArtifactId())) {
                return false;
            }
        }
        return true;
    }

    private String prepareClasspath() {
        StringBuilder sb = new StringBuilder();
        try {
            for (String el : project.getRuntimeClasspathElements()) {
                if (sb.length() > 0) {
                    sb.append(File.pathSeparator);
                }
                sb.append(el);
            }
        } catch (Exception ex) {
            getLog().error("Failed to get runtime classpath elementes", ex);
        }
        return sb.toString();
    }


    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        getLog().info("Preparing Simulator Classpath");
        Properties props = project.getModel().getProperties();
        if (props == null) {
            props = new Properties();
            project.getModel().setProperties(props);
        }
        //props.setProperty("exec.args", properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));
        project.getModel().addProperty("codename1.mainClass", properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));
        // Setup CEF directory
        if (!isCefSetup()) {
            getLog().debug("CEF Not set up yet.  Setting it up now");
            setupCef();
        } else {
            getLog().debug("CEF is already set up. cef.dir="+System.getProperty("cef.dir"));
        }
        project.getProperties().setProperty("cef.dir", System.getProperty("cef.dir"));
        
        File designerJar = getDesignerJar();
        
        if (designerJar != null && designerJar.exists()) {
            project.getModel().addProperty("codename1.designer.jar", designerJar.getAbsolutePath());
        } else {
            throw new MojoExecutionException("Can't find designer jar");
        }
        
        File cssFile = new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "css" + File.separator + "theme.css");
        File resFile = new File(getCN1ProjectDir(), "target" + File.separator + "classes" + File.separator + "theme.res");
        File mergeFile = new File(getCN1ProjectDir(), "target" + File.separator + "css" + File.separator + "theme.css");

        final StringBuilder inputs = new StringBuilder();

        project.getArtifacts().forEach(artifact->{
            if (artifact.hasClassifier() && "cn1css".equals(artifact.getClassifier())) {
                File zip = findArtifactFile(artifact);
                if (zip == null || !zip.exists()) {
                    return;
                }

                File extracted = new File(zip.getParentFile(), zip.getName()+"-extracted");
                getLog().debug("Checking for extracted CSS bundle "+extracted);
                if (extracted.exists() && artifact.isSnapshot() && getLastModified(artifact) > extracted.lastModified()) {
                    try {
                        FileUtils.deleteDirectory(extracted);
                    } catch (IOException ex){
                        getLog().error(ex);
                    }
                }
                if (!extracted.exists()) {
                    getLog().debug("CSS bundle "+zip+" not extracted yet.  Extracting to "+extracted);
                    // This is a cn1css artifact, which is a zip file.
                    // We extract it so that we can access the files directly.
                    Expand expand = (Expand)antProject.createTask("unzip");
                    expand.setSrc(zip);
                    expand.setDest(extracted);
                    expand.execute();

                }
                if (extracted.exists()) {
                    File extractedCssDir = new File(extracted, path("META-INF","codenameone", artifact.getGroupId(), artifact.getArtifactId(), "css"));
                    if (extractedCssDir.exists()) {
                        // We expect that the cn1css artifact has a theme.css file at its root
                        // If found, we add it to the list of inputs.
                        File theme = new File(extractedCssDir, "theme.css");
                        if (theme.exists()) {
                            if (inputs.length() > 0) {
                                inputs.append(",");
                            }
                            inputs.append(theme.getAbsolutePath());
                        }
                    }

                } else {
                    getLog().debug("CSS bundle extraction must have failed for "+zip+" because after extraction it still doesn't exist at "+extracted);
                }
            }
        });

        // The project's theme.css file is added to the input list last so that it will result in it
        // being last in the merged theme.css file (i.e. the application project CSS can override the
        // CSS in dependent libraries.

        if (cssFile.exists()) {
            if (inputs.length() > 0) {
                inputs.append(",");
            }
            inputs.append(cssFile.getAbsolutePath());
        }


        if (cssFile.exists()) {
            project.getModel().addProperty("codename1.css.compiler.args.input", inputs.toString());
            project.getModel().addProperty("codename1.css.compiler.args.output", resFile.getAbsolutePath());
            project.getModel().addProperty("codename1.css.compiler.args.merge", mergeFile.getAbsolutePath());
        } else {
            project.getModel().addProperty("codename1.css.compiler.args.input", "");
            project.getModel().addProperty("codename1.css.compiler.args.output", "");
            project.getModel().addProperty("codename1.css.compiler.args.merge", "");
        }
        if ("true".equals(project.getProperties().getProperty("cn1.class.path.required"))) {
            project.getModel().addProperty("cn1.class.path", prepareClasspath());
        }

        Properties simulatorProperties = new Properties();
        File simulatorPropertiesFile = new File(getCN1ProjectDir(), path("target", "codenameone", "simulator.properties"));
        if (!simulatorPropertiesFile.getParentFile().exists()) {
            simulatorPropertiesFile.getParentFile().mkdirs();
        }


        try {
            StringBuilder compileClasspath = new StringBuilder();
            for (String el : project.getCompileClasspathElements()) {
                if (compileClasspath.length() > 0) {
                    compileClasspath.append(File.pathSeparator);
                }
                compileClasspath.append(el);
            }
           simulatorProperties.setProperty("cn1.maven.compileClasspathElements", compileClasspath.toString());

        } catch (Exception ex){}

        try (FileOutputStream fos = new FileOutputStream(simulatorPropertiesFile)) {
            simulatorProperties.store(fos, "Updated simulator properties in PrepareSimulatorClasspathMojo");
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write simulator.properties file", ex);
        }
        
    }

    protected File findCSSDirectory() {
        for (String dir : project.getCompileSourceRoots()) {
            File dirFile = new File(dir);
            File cssSibling = new File(dirFile.getParentFile(), "css");
            File themeCss = new File(cssSibling, "theme.css");
            if (themeCss.exists()) {
                return cssSibling;
            }

        }
        return null;
    }
    
    
}
