/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;


import java.io.File;
import java.io.IOException;

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

import static com.codename1.maven.PathUtil.path;

/**
 * Compiles the project's CSS files, generating a theme.res file which is placed in the build/classes directory.
 *
 * **Notes**
 *
 * . This mojo is only run for application projects.  Libary projects support CSS but they don't compile them.
 *   They just package them in a cn1css artifact which will be merged into the CSS for application projects that
 *   use them.
 * . The project must have the "codename1.cssTheme" property defined as "true" in either the pom.xml or the
 *   codenameone_settings.properties.  Otherwise this mojo does nothing.
 * . The CSS from cn1libs in dependencies is merged with the project CSS (located at src/main/css/theme.css) into
 *   a build directory at target/css.  This merged file is then compiled to the output file in build/classes/theme.res.
 *
 * @author shannah
 */
@Mojo(name = "css", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, 
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CompileCSSMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        if (properties.getProperty("codename1.cssTheme", null) == null) {
            getLog().info("CSS themes not activated for this project.  Skipping CSS compilation");
            return;
        }
        
        File cssDirectory = findCSSDirectory(); // src/main/css
        if (cssDirectory == null || !cssDirectory.exists()) {
            getLog().warn("CSS compilation skipped because no CSS theme was found");
            return;
        }
        File themeResOutput = new File(project.getBuild().getOutputDirectory() + File.separator + "theme.res");
        // target/css
        File cssBuildDir = new File(project.getBuild().getDirectory() + File.separator + "css");
        cssBuildDir.mkdirs();

        // target/css/theme.css - the merged CSS file
        File mergeFile = new File(cssBuildDir, "theme.css");
        mergeFile.getParentFile().mkdirs();
        try {
            if (themeResOutput.exists() && getCSSSourcesModificationTime() < themeResOutput.lastModified()) {
                getLog().info("CSS sources unchanged since last compile.  Skipping CSS compilation");
                return;
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to check CSS file modification times", ex);
        }

        // Compile a comma-delimited list a CSS files that will be sent to the CSS compiler as inputs.
        // We look through all dependency artifacts with the cn1css classifier, and add their
        // theme.css to the input list.  (Codename One Library projects will include such an
        // artifact if they have CSS files).
        final StringBuilder inputs = new StringBuilder();
        
        project.getArtifacts().forEach(artifact->{
            if (artifact.hasClassifier() && "cn1css".equals(artifact.getClassifier())) {
                File zip = findArtifactFile(artifact);
                if (zip == null || !zip.exists()) {
                    return;
                }
                
                File extracted = new File(zip.getParentFile(), zip.getName()+"-extracted");
                getLog().debug("Checking for extracted CSS bundle "+extracted);
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
        File cssTheme = new File(cssDirectory, "theme.css");
        if (cssTheme.exists()) {
            if (inputs.length() > 0) {
                inputs.append(",");
            }
            inputs.append(cssTheme.getAbsolutePath());
        } else {
            if (inputs.length() > 0) {
                throw new MojoFailureException("Cannot compile CSS for this project.  The project does not include a theme.css file in "+cssTheme+", but it includes dependencies that require CSS.  Please add a CSS file at "+cssTheme);

            }
            getLog().info("Skipping CSS compilation because "+cssTheme+" does not exist");
            return;
        }



        // Run the CSS compiler which is contained inside the codenameone-designer jar
        // NOTE: The codenameone-designer.jar is a dependency of the codenameone-maven-plugin as
        // zip file (which is the designer jar with all dependencies).  We use this jar
        // rather than the central designer_1.jar located in the user's home directory to make it
        // easier to pin to a particular version.
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
        java.createArg().setFile(themeResOutput);
        
        java.createArg().setValue("-merge");
        java.createArg().setFile(mergeFile);
        int res = java.executeJava();
        if (res != 0) {
            throw new MojoExecutionException("An error occurred while compiling the CSS files.  Inputs: "+inputs+", output: " + new File(project.getBuild().getOutputDirectory() + File.separator + "theme.res") +", merge file: "+mergeFile);
        }
    }

    /**
     * Gets the source CSS directory (src/main/css).  The theme.css file should be inside this directory.
     * @return
     */
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
