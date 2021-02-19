package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static com.codename1.maven.PathUtil.path;

/**
 * Generates a Maven cn1lib project (using the cn1lib-archetype) using a provided ANT cn1lib project as a template.
 * This is to assist in migrating Ant projects to Maven projects.
 *
 *
 */
@Mojo(name="generate-cn1lib-project", requiresProject = false)
public class GenerateCn1libProjectMojo extends AbstractMojo {

    @Parameter(property = "sourceProject")
    private File sourceProject;

    @Parameter(property="artifactId")
    private String artifactId;

    @Parameter(property="groupId")
    private String groupId;

    @Parameter(property="version", defaultValue = "1.0-SNAPSHOT")
    private String version;

    private void generateProject() throws MojoExecutionException{
        InvocationRequest request = new DefaultInvocationRequest();
        //request.setPomFile( new File( "/path/to/pom.xml" ) );

        request.setGoals( Collections.singletonList( "archetype:generate" ) );
        String[] propsArr = {
                "interactiveMode=false",
            "archetypeArtifactId=cn1lib-archetype",
            "archetypeGroupId=com.codenameone",
            "archetypeVersion=LATEST",
            "artifactId="+artifactId,
                "groupId="+groupId,
                "version="+version
        };
        Properties props = new Properties();
        for (String prop : propsArr) {
            int eqpos = prop.indexOf("=");
            if (eqpos > 0) {
                props.setProperty(prop.substring(0, eqpos), prop.substring(eqpos+1));
            } else if (eqpos < 0) {
                props.setProperty(prop, "true");
            }
        }


        request.setProperties(props);

        Invoker invoker = new DefaultInvoker();
        try {
            invoker.execute( request );
        } catch (MavenInvocationException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);

        }
    }

    private File targetProjectDir() {
        return new File(artifactId);
    }



    private File targetCommonDir() {
        return new File(targetProjectDir(), "common");
    }

    private File targetIosDir() {
        return new File(targetProjectDir(), "ios");
    }

    private File targetAndroidDir() {
        return new File(targetProjectDir(), "android");
    }

    private File targetJavascriptDir() {
        return new File(targetProjectDir(), "javascript");
    }

    private File targetJavaseDir() {
        return new File(targetProjectDir(), "javase");
    }

    private File targetWinDir() {
        return new File(targetProjectDir(), "win");
    }

    private File targetSrcDir() {
        return new File(targetCommonDir(), "src");
    }

    private File targetSrcDir(String type) {
        return new File(targetSrcDir(), path("main", type));
    }

    private File sourceSrcDir() {
        return new File(sourceProject, "src");
    }

    private File sourceNativeDir() {
        return new File(sourceProject, "native");
    }

    private File sourceNativeDir(String type) {
        return new File(sourceNativeDir(), type);
    }

    private void copyPropertiesFiles() throws IOException {

        for (File child : sourceProject.listFiles()) {
            if (child.getName().endsWith(".properties")) {
                FileUtils.copyFile(child, new File(targetCommonDir(), child.getName()));
            }
        }
    }

    private boolean hasFilesWithSuffix(File root, String suffix) {
        if (root.isDirectory()) {
            for (File child : root.listFiles()) {
                if (child.getName().endsWith(suffix)) {
                    return true;
                }
            }
            for (File child : root.listFiles()) {
                if (child.isDirectory()) {
                    if (hasFilesWithSuffix(child, suffix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void copyIosFiles() {
        if (sourceNativeDir("ios").exists()) {
            File srcDir = new File(targetIosDir(), path("src", "main", "objectivec"));
            File resDir = new File(targetIosDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("ios"));
                files.setIncludes("**/*.m, **/*.c, **/*.h");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("ios"));
                files.setExcludes("**/*.m, **/*.c, **/*.h");
                copy.addFileset(files);

                copy.execute();
            }

        }

    }

    private void copyAndroidFiles() {
        if (sourceNativeDir("android").exists()) {
            File srcDir = new File(targetAndroidDir(), path("src", "main", "java"));
            File resDir = new File(targetAndroidDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("android"));
                files.setIncludes("**/*.java");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("android"));
                files.setExcludes("**/*.java");
                copy.addFileset(files);

                copy.execute();
            }

        }

    }

    private void copyJavascriptFiles() {
        if (sourceNativeDir("javascript").exists()) {
            File srcDir = new File(targetJavascriptDir(), path("src", "main", "javascript"));
            File resDir = new File(targetJavascriptDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("javascript"));
                files.setIncludes("**/*.js");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("javascript"));
                files.setExcludes("**/*.js");
                copy.addFileset(files);

                copy.execute();
            }

        }
    }

    private void copyWinFiles() {
        if (sourceNativeDir("win").exists()) {
            File srcDir = new File(targetWinDir(), path("src", "main", "csharp"));
            File resDir = new File(targetWinDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("win"));
                files.setIncludes("**/*.cs");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("win"));
                files.setExcludes("**/*.cs");
                copy.addFileset(files);

                copy.execute();
            }

        }
    }

    private File sourceCSSDir() {
        return new File(sourceProject, "css");
    }

    private void copyCSSFiles() {
        if (sourceCSSDir().exists()) {
            File srcDir = targetSrcDir("css");

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceCSSDir());
                files.setIncludes("**");
                copy.addFileset(files);

                copy.execute();
            }
        }
    }

    private void copyJavaseFiles() {
        if (sourceNativeDir("javase").exists()) {
            File srcDir = new File(targetJavaseDir(), path("src", "main", "java"));
            File resDir = new File(targetJavaseDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("javase"));
                files.setIncludes("**/*.java");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);

                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("javase"));
                files.setExcludes("**/*.java");
                copy.addFileset(files);

                copy.execute();


            }

            // If there are jar files in the resources directory, we should issue a warning that
            // they should replace these with dependencies in the pom.xml

            for (File child : resDir.listFiles()) {
                if (child.getName().endsWith(".jar")) {
                    getLog().warn("Found jar file '"+child.getName()+"' in the native/javase directory.  This has been copied to "+child+", but you should " +
                            "remove this file and replace it with the equivalent Maven dependency inside your "+new File(targetJavaseDir(), "pom.xml")+" file.");
                }
            }

        }
    }

    private void copySourceFiles() {
        {
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetSrcDir("java"));

            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceSrcDir());
            files.setIncludes("**/*.java");
            copy.addFileset(files);

            copy.execute();
        }

        {
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetSrcDir("resources"));

            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceSrcDir());
            files.setExcludes("**/*.kt, **/*.java, **/*.mirah");
            copy.addFileset(files);

            copy.execute();
        }


        if (hasFilesWithSuffix(sourceSrcDir(), ".kt")){
            targetSrcDir("kotlin").mkdirs();
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetSrcDir("kotlin"));

            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceSrcDir());
            files.setIncludes("**/*.kt");
            copy.addFileset(files);

            copy.execute();
        }
        if (hasFilesWithSuffix(sourceSrcDir(), ".mirah")){
            targetSrcDir("mirah").mkdirs();
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetSrcDir("mirah"));

            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceSrcDir());
            files.setIncludes("**/*.mirah");
            copy.addFileset(files);

            copy.execute();
        }

    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            generateProject();
            copyPropertiesFiles();
            copySourceFiles();
            copyAndroidFiles();
            copyIosFiles();
            copyJavascriptFiles();
            copyWinFiles();
            copyJavaseFiles();
            copyCSSFiles();

        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to copy files", ex);
        }

    }

    private Project antProject;
    private Project antProject() {
        if (antProject == null) {
            antProject = new Project();
            antProject.setBaseDir(sourceProject);

            antProject.setDefaultInputStream(System.in);

            InputHandler handler = new DefaultInputHandler();
            antProject.setProjectReference(handler);
            antProject.setInputHandler(handler);


            antProject.init();
        }
        return antProject;

    }

}
