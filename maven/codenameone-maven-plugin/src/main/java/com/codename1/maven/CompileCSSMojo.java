/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;


import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;

/**
 *
 * @author shannah
 */
@Mojo(name = "css", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, 
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CompileCSSMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        /*
        <java jar="${user.home}/.codenameone/designer_1.jar" fork="true" failonerror="true">
            <jvmarg value="-Dcli=true"/>
            <arg value="-css"/>
            <arg file="css/theme.css"/>
            <arg file="src/theme.res"/>
        </java>
        */
        
        if (properties.getProperty("codename1.cssTheme", null) == null) {
            getLog().info("CSS themes not activated for this project.  Skipping CSS compilation");
            return;
        }
        
        File cssDirectory = findCSSDirectory();
        if (cssDirectory == null || !cssDirectory.exists()) {
            getLog().warn("CSS compilation skipped because no CSS theme was found");
            return;
        }
        
        final StringBuilder inputs = new StringBuilder();
        
        project.getArtifacts().forEach(artifact->{
            if (artifact.hasClassifier() && "cn1css".equals(artifact.getClassifier())) {
                File zip = findArtifactFile(artifact);
                if (zip == null || !zip.exists()) {
                    return;
                }
                
                File extracted = new File(zip.getParentFile(), zip.getName()+"-extracted");
                if (!extracted.exists()) {
                    Expand expand = (Expand)antProject.createTask("unzip");
                    expand.setSrc(zip);
                    expand.setDest(extracted);
                    expand.execute();
                }
                if (extracted.exists()) {
                    File theme = new File(extracted, "theme.css");
                    if (theme.exists()) {
                        if (inputs.length() > 0) {
                            inputs.append(",");
                        }
                        inputs.append(theme.getAbsolutePath());
                    }
                }
            }
        });
        
        File cssTheme = new File(cssDirectory, "theme.css");
        if (cssTheme.exists()) {
            if (inputs.length() > 0) {
                inputs.append(",");
            }
            inputs.append(cssTheme.getAbsolutePath());
        }
        
        File cssImplDir = new File(project.getBuild().getDirectory() + File.separator + "css");
        cssImplDir.mkdirs();
        File mergeFile = new File(cssImplDir, "theme.css");
        
        Copy copy = (Copy)antProject.createTask("copy");
        copy.setTodir(cssImplDir);
        FileSet fileset = new FileSet();
        fileset.setProject(antProject);
        fileset.setDir(cssDirectory);
        copy.addFileset(fileset);
        copy.execute();
        
        Java java = createJava();
        java.setDir(getCN1ProjectDir());
        java.setJar(getDesignerJar());
        java.setFork(true);
        java.setFailonerror(true);
        setupCef();
        String cefDir = System.getProperty("cef.dir", System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "cef");
        java.createJvmarg().setValue("-Dcli=true");
        java.createJvmarg().setValue("-Dcef.dir="+cefDir);
        java.createArg().setValue("-css");
        java.createArg().setValue("-input");
        java.createArg().setValue(inputs.toString());
        
        java.createArg().setValue("-output");
        java.createArg().setFile(new File(project.getBuild().getOutputDirectory() + File.separator + "theme.res"));
        
        java.createArg().setValue("-merge");
        java.createArg().setFile(mergeFile);
        java.executeJava();
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
    
    protected File findThemeRes() {
        for (String dir : project.getCompileSourceRoots()) {
            File dirFile = new File(dir);
            File resources = new File(dirFile.getParentFile(), "resources");
            File themeRes = new File(resources, "theme.res");
            if (themeRes.exists()) {
                return themeRes;
            }
            themeRes = new File(dirFile, "theme.res");
            if (themeRes.exists()) {
                return themeRes;
            }
        }
        return null;
    }
    
    
    
    
    
}
