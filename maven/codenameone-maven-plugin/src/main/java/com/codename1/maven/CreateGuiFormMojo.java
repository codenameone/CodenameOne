/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A goal to generate a GUI form.
 * @author shannah
 */
@Mojo(name = "create-gui-form")
public class CreateGuiFormMojo extends AbstractCN1Mojo {

    /**
     * The fully-qualified class name of the form to generate.
     */
    @Parameter(property="className", required = true)
    private String className;

    /**
     * Use autolayout mode.
     */
    @Parameter(property="autoLayout", required = false, defaultValue = "true")
    private boolean autoLayout;

    /**
     * The guiType.  Default is "Form".  "Container", and "Dialog" also supported values.
     */
    @Parameter(property="guiType", required = true, defaultValue = "Form")
    private String guiType;

    /**
     * Validates a class name.
     * @param className
     * @throws MojoExecutionException
     */
    private void validateClassName(String className) throws MojoExecutionException {
        String[] classNameParts = className.split("\\.");
        int len = classNameParts.length;
        if (len < 2) {
            throw new MojoExecutionException("GUIBuilder Forms cannot be in the root namespace.  Specify a package.  E.g. -DclassName=com.example.MyForm");
        }
        Pattern p = Pattern.compile("^[a-z][a-z0-9A-Z]*$");
        for (int i = 0; i < len - 1; i++) {
            if (!classNameParts[i].matches("^[a-z][a-z0-9A-Z]*$")) {
                throw new MojoExecutionException("className package component " + classNameParts[i] + " does not match the required regular expression ^[a-z][a-z0-9A-Z]*$");
            }
        }
        if (!classNameParts[len - 1].matches("^[A-Z][a-z0-9A-Z]*$")) {
            throw new MojoExecutionException("className validation failed.  Name must conform to regular expression ^[A-Z][a-z0-9A-Z]*$");
        }
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {

        if (getCN1ProjectDir() == null) {
            getLog().debug("Skipping create-gui-form because this is not a CN1 project");
            return;
        }
        try {
            if (!getCN1ProjectDir().getCanonicalFile().equals(project.getBasedir().getCanonicalFile())) {
                getLog().debug("Skipping create-gui-form because this is not a CN1 project");
                return;
            }
        } catch (IOException ex) {

            getLog().error("Error trying to convert to canonical paths", ex);
            return;
        }

        validateClassName(className);
        File guibuilderDir = new File(getCN1ProjectDir(),  "src" + File.separator + "main" + File.separator + "guibuilder");

        String path = className.replace(".", File.separator);

        File guiFile = new File(guibuilderDir, path + ".gui");
        if (guiFile.exists()) {
            throw new MojoExecutionException("GUI File already exists at "+guiFile);
        }
        File guiFileDir = guiFile.getParentFile();
        guiFileDir.mkdirs();

        File javaSrcRoot = new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "java");
        File javaFile = new File(javaSrcRoot, path + ".java");
        if (javaFile.exists()) {
            throw new MojoExecutionException("Java source file already exists at "+javaFile);
        }
        File javaFileDir = javaFile.getParentFile();
        javaFileDir.mkdirs();

        String fileName = className.contains(".") ? className.substring(className.lastIndexOf(".") + 1) : className;

        String javaSource = "package " + className.substring(0, className.lastIndexOf(".")) + ";\n"
                + "public class " + fileName + " extends com.codename1.ui." + getGUIType() + " {\n"
                + "    public " + fileName + "() {\n"
                + "        this(com.codename1.ui.util.Resources.getGlobalResources());\n"
                + "    }\n"
                + "    \n"
                + "    public " + fileName + "(com.codename1.ui.util.Resources resourceObjectInstance) {\n"
                + "        initGuiBuilderComponents(resourceObjectInstance);\n"
                + "    }\n"
                + "    \n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "    private void initGuiBuilderComponents(com.codename1.ui.util.Resources resourceObjectInstance) {\n"
                + "    }\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "}\n";
        String xmlGUISource;

        if (getGUIType().equalsIgnoreCase("Container")) {
            xmlGUISource = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n"
                    + "<component type=\"Container\" layout=\"" + getLayout() + "\" name=\"" + fileName + "\"" + getAutoLayout() + "></component>";
        } else {
            xmlGUISource = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n"
                    + "<component type=\"" + getGUIType() + "\" layout=\"" + getLayout() + "\" title=\"" + fileName + "\" name=\"" + fileName + "\"" + getAutoLayout() + "></component>";
        }
        try {
            getLog().info("Writing "+guiFile);
            FileUtils.writeStringToFile(guiFile, xmlGUISource);
            getLog().info("Writing "+javaFile);
            FileUtils.writeStringToFile(javaFile, javaSource);
        } catch (IOException ex) {
            getLog().error("Failed to write source files");
            throw new MojoExecutionException("Failed to write source files", ex);
        }
        
        getLog().info("2 files created successfully.  Open the gui file in the gui builder using \n"
                + "mvn cn1:guibuilder -DclassName="+className);

    }

    protected String getGUIType() {
        return guiType;
    }

    protected String getAutoLayout() {
        if (isAutoLayout()) {
            return " autoLayout=\"true\"";
        }
        return "";
    }

    protected String getLayout() {
        return isAutoLayout() ? "LayeredLayout" : "FlowLayout";
    }

    private boolean isAutoLayout() {
        return autoLayout;
    }

}
