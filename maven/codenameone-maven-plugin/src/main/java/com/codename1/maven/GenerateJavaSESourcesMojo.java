package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Expand;

import java.io.File;

import static com.codename1.maven.PathUtil.path;

@Mojo(name="generate-javase-sources", requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateJavaSESourcesMojo extends AbstractCN1Mojo {
    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File rootProjectDir = project.getParent().getBasedir();
        File generatedSourcesDir = new File(project.getBuild().getDirectory(), path("generated-sources", "cn1libs"));
        generatedSourcesDir.mkdirs();
        project.addCompileSourceRoot(generatedSourcesDir.getAbsolutePath());

        try {
            for (String element : project.getRuntimeClasspathElements()) {
                if (new File(element).getName().equals("nativese.zip")) {
                    Expand unzip = (Expand)antProject.createTask("unzip");
                    unzip.setSrc(new File(element));
                    unzip.setDest(generatedSourcesDir);
                    unzip.execute();

                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to get runtime classpath elements", ex);
        }


    }
}
