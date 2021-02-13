package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * A goal to generate a set of archetype projects from templates located in a specified directory (default ${basedir}/archetype-templates)
 * into an output directory (default $buildDir/generated-archetypes).  It can optionally generate them as a multi-module project so that
 * they can all be built from a root pom project.
 */
@Mojo(name="generate-archetype-projects")
public class GenerateArchetypeProjectsMojo extends AbstractCN1Mojo {

    /**
     * Output directory where the archetype projects should be written to.
     */
    @Parameter(property = "codename1.generateArchetypes.outputDir", defaultValue = "${project.build.directory}/generated-archetypes")
    private File generatedArchetypesOutputDir;

    /**
     * Templates directory where the archetype templates can be found.  These templates will be processed by the {@link GenerateArchetypeFromTemplateMojo}
     * goal so they should conform to that format.
     */
    @Parameter(property = "codename1.generateArchetypes.templatesDir", defaultValue = "${project.basedir}/archetype-templates")
    private File templatesDir;

    /**
     * Whether to overwrite existing projects if they are already there.  If this is false, and the project already exists, then the
     * goal will fail.
     */
    @Parameter(property = "codename1.generateArchetypes.overwrite", defaultValue = "false")
    private boolean overwrite;

    @Parameter(property = "codename1.generateArchetypes.parentGroupId", required = false, defaultValue = "com.codenameone")
    private String parentGroupId;

    @Parameter(property = "codename1.generateArchetypes.parentArtifactId", required = false, defaultValue = "maven-archetypes")
    private String parentArtifactId;

    @Parameter(property = "codename1.generateArchetypes.parentVersion", required = false, defaultValue = "${project.version}")
    private String parentVersion;

    @Parameter(property = "codename1.generateArchetypes.groupId", required = false)
    private String groupId;

    @Parameter(property = "codename1.generateArchetypes.artifactId", required = false)
    private String artifactId;

    @Parameter(property = "codename1.generateArchetypes.version", required = false, defaultValue = "${project.version}")
    private String version;





    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File[] templates = templatesDir.listFiles();
        if (templates != null) {
            for (File template : templates) {
                if (template.getName().endsWith(".java")) {
                    generateTemplate(template);
                }
            }
            if (groupId != null && artifactId != null && version != null) {
                generatedArchetypesOutputDir.mkdirs();
                generateParentProject(generatedArchetypesOutputDir);
            }
        }
    }

    private void generateTemplate(File template)  throws MojoExecutionException {

        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Collections.singletonList("com.codenameone:codenameone-maven-plugin:"+getSelf().getVersion()+":generate-archetype"));
        Properties props = new Properties();
        props.put("template", template.getAbsolutePath());
        if (overwrite) {
            props.put("overwrite", "true");
        }
        request.setProperties(props);
        generatedArchetypesOutputDir.mkdirs();
        request.setBaseDirectory(generatedArchetypesOutputDir);
        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Failed to generate project for template "+template, result.getExecutionException());
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Failed to generate archetype project for template "+template, e);
        }


    }

    private List<String> getModuleNames(File directory) {
        List<String> moduleNames = new ArrayList<String>();
        for (File child : directory.listFiles()) {
            if (child.isDirectory()) {
                File childPom = new File(child, "pom.xml");
                if (childPom.exists()) {
                    moduleNames.add(child.getName());
                }
            }
        }
        return moduleNames;
    }

    private void generateParentProject(File directory) throws MojoFailureException, MojoExecutionException{
        StringBuilder sb = new StringBuilder();





        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    \n" +
                "    <parent>\n" +
                "        <groupId>"+parentGroupId+"</groupId>\n" +
                "        <artifactId>"+parentArtifactId+"</artifactId>\n" +
                "        <version>"+parentVersion+"</version>\n" +
                "    </parent>\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>"+groupId+"</groupId>\n" +
                "    <artifactId>"+artifactId+"</artifactId>\n" +
                "    <version>"+version+"</version>\n" +
                "    <packaging>pom</packaging>\n" +
                "    \n" +
                "    <modules>\n");
        for (String module : getModuleNames(directory)) {
            sb.append("<module>").append(module).append("</module>\n");
        }
        sb.append("</modules>\n</project>");

        File pomFile = new File(generatedArchetypesOutputDir, "pom.xml");
        if (pomFile.exists() && !overwrite) {
            throw new MojoFailureException("Overwrite flag is not set and "+pomFile+" already exists");
        }
        try {
            FileUtils.writeStringToFile(pomFile, sb.toString(), "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write "+pomFile+".", ex);
        }

    }

    private boolean isSelf(Plugin plugin) {
        return plugin.getArtifactId().equals(ARTIFACT_ID) && plugin.getGroupId().equals(GROUP_ID);
    }


    private Plugin getSelf() {
        for (Plugin p : this.project.getBuildPlugins()) {
            if (isSelf(p)) {
                return p;
            }
        }
        return null;
    }

}
