/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Goal to open the gui builder.
 * @author shannah
 */
@Mojo(name = "guibuilder")
public class OpenGuiBuilderMojo extends AbstractCN1Mojo {
    private File guibuilderInput = new File(System.getProperty("user.home") + File.separator + ".guiBuilder" + File.separator + "guibuilder.input");

    @Parameter(property="className", required=true)
    private String className;
    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        try {
            

            File sourceFile = findSourceFile(className);
            if (!sourceFile.exists()) {
                throw new MojoExecutionException("Cannot find source file "+sourceFile);
            }
            File guiFile = findGuiFile(className);
            if (!guiFile.exists()) {
                throw new MojoExecutionException("Cannot find gui fuild "+guiFile);
            }
            
            File resourceFile = getProjectResourceFile();
            if (!resourceFile.exists()) {
                if (isCSSProject()) {
                    // If it is a CSS project, we may simply need to compile the CSS since the theme.res
                    // will be located in the compiled classes directory
                    InvocationRequest request = new DefaultInvocationRequest();
                    //request.setPomFile( new File( "/path/to/pom.xml" ) );
                    request.setGoals( Collections.singletonList( "cn1:css" ) );
                    

                    Invoker invoker = new DefaultInvoker();
                    try {
                        getLog().info("theme.res file not found.  Trying to compile CSS to generate it now");
                        invoker.execute( request );
                    } catch (MavenInvocationException ex) {
                        getLog().error("Failed to compile CSS");
                        throw new MojoExecutionException(ex.getMessage(), ex);

                    }
                    if (!resourceFile.exists()) {
                        throw new MojoExecutionException("Still cannot find resource file at "+resourceFile+" even after compiling project CSS");
                                
                    }
                } else {
                    throw new MojoExecutionException("Cannot find project resource file "+resourceFile);
                }
            }
            openInGuiBuilder(project.getName(), sourceFile, resourceFile, guiFile);
        } catch (IOException ex) {
            getLog().error("Failed to open form");
            throw new MojoExecutionException("Failed to open form", ex);
        }
    }
    
    private boolean isCSSProject() {
        return ("true".equals(properties.getProperty("codename1.cssTheme", null)));
    }
 
    private File getProjectResourceFile() {
        if (isCSSProject()) {
            return new File(project.getBuild().getOutputDirectory() + File.separator + "theme.res");
        }
        return new File(project.getBasedir() + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "theme.res");
    }
    
    private File getGuiBuilderSourcesDir() {
        return new File(project.getBasedir() + File.separator + "src" + File.separator + "main" + File.separator + "guibuilder");
    }
    
    private File findGuiFile(String fullyQualifiedClassName) {
        return new File(getGuiBuilderSourcesDir(), fullyQualifiedClassName.replace(".", File.separator) + ".gui");
    }
    
    private File findSourceFile(String fullyQualifiedClassName) {
        return new File(
                project.getBasedir() + File.separator + "src" + File.separator + 
                        "main" + File.separator + "java" + File.separator + 
                        fullyQualifiedClassName.replace(".", File.separator) + ".java"
        );
    }
    
    private File getGuiBuilderJar() {
        File home = new File(System.getProperty("user.home"));
        File codenameone = new File(home, ".codenameone");
        File settingsJar = new File(codenameone, "guibuilder.jar");
        
        return settingsJar;
    }
     
    
    
    static String xmlize(String s) {
        s = s.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        s = s.replace("\"", "&quot;");
        int charCount = s.length();
        for(int iter = 0 ; iter < charCount ; iter++) {
            char c = s.charAt(iter);
            if(c > 127) {
                // we need to localize the string...
                StringBuilder b = new StringBuilder();
                for(int counter = 0 ; counter < charCount ; counter++) {
                    c = s.charAt(counter);
                    if(c > 127) {
                        b.append("&#x");
                        b.append(Integer.toHexString(c));
                        b.append(";");
                    } else {
                        b.append(c);
                    }
                }
                return b.toString();
            }
        }
        return s;
    }
    
    public void openInGuiBuilder(String projectName, final File javaSourceFile, File projectResourceFile, File guiFile) throws IOException, MojoExecutionException {
        try {
            File guiBuilderDirectory = new File(System.getProperty("user.home") + File.separator + ".guiBuilder");
            File cn1Directory = new File(System.getProperty("user.home") + File.separator + ".cn1");
            File codenameOneDirectory = new File(System.getProperty("user.home") + File.separator + ".codenameone");
            guiBuilderDirectory.mkdirs();
            cn1Directory.mkdirs();
            codenameOneDirectory.mkdirs();
            if(!guiBuilderDirectory.exists()) {
                throw new FileNotFoundException("Couldn't find or create the GUI builder directory within your home directory specifically: " + guiBuilderDirectory.getAbsolutePath());
                
            }
            if(!cn1Directory.exists()) {
                throw new FileNotFoundException("Couldn't find or create the cn1 directory within your home directory specifically: " + cn1Directory.getAbsolutePath());
                
            }
            if(!codenameOneDirectory.exists()) {
                throw new FileNotFoundException("Couldn't find or create the codename1 directory within your home directory specifically: " + codenameOneDirectory.getAbsolutePath());
            }
            String connectionId = UUID.randomUUID().toString();
            final File runningFile = new File(System.getProperty("user.home") + File.separator + ".guiBuilder" + File.separator +  connectionId);
            final File outputFile = new File(System.getProperty("user.home") + File.separator + ".guiBuilder" + File.separator +  connectionId + ".ouput");
            runningFile.getParentFile().mkdirs();
            FileOutputStream r = new FileOutputStream(runningFile);
            r.write(0);
            r.close();
            FileOutputStream fos = new FileOutputStream(guibuilderInput);
            String formName = javaSourceFile.getName();
            formName = formName.substring(0, formName.length() - 5);
            fos.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<con name=\"" + xmlize(projectName) +
                    "\" supportsEvents=\"false" + // The we disable the events panel in maven because event handler generation doesn't yet work properly.
                    "\" formName=\"" + xmlize(formName) +
                    "\"  file=\"" + xmlize(guiFile.toURI().toURL().toExternalForm()) +
                    "\" javaFile=\"" + xmlize(javaSourceFile.toURI().toURL().toExternalForm()) +
                    "\" resFile=\"" + xmlize(projectResourceFile.toURI().toURL().toExternalForm()) +
                    "\" outputFile=\"" + xmlize(outputFile.toURI().toURL().toExternalForm()) +
                    "\" running=\"" + xmlize(runningFile.toURI().toURL().toExternalForm()) +
                    "\" />\n").getBytes());
            fos.close();
            launchGuiBuilderApp();

            new Thread() {
                private long lastModified = 0;
                public void run() {
                    while(runningFile.exists()) {
                        if(outputFile.exists() && lastModified != outputFile.lastModified()) {
                            try {
                                Thread.sleep(100);
                            } catch(InterruptedException e) {}
                            try {
                                FileInputStream fis = new FileInputStream(outputFile);
                                byte[] data = new byte[(int)outputFile.length()];
                                fis.read(data);
                                fis.close();
                                lastModified = outputFile.lastModified();
                                String d = new String(data);
                                if(d.endsWith("DataChangeEvent")) {
                                    gotoSourceFileLine(javaSourceFile, "void " + d, "\n    public void " + d + "(com.codename1.ui.Component cmp, int type, int index) {\n    }\n");
                                } else {
                                    if(d.endsWith("Command")) {
                                        gotoSourceFileLine(javaSourceFile, "void " + d, "\n    public void " + d + "(com.codename1.ui.events.ActionEvent ev, Command cmd) {\n    }\n");
                                    } else {
                                        if(d.endsWith("ListModel")) {
                                            gotoSourceFileLine(javaSourceFile, "ListModel " + d, "\n    public com.codename1.ui.list.ListModel " + d + "() {\n    }\n");
                                        } else {
                                            gotoSourceFileLine(javaSourceFile, "void " + d, "\n    public void " + d + "(com.codename1.ui.events.ActionEvent ev) {\n    }\n");
                                        }
                                    }
                                }
                                outputFile.delete();
                            } catch(IOException err) {
                                err.printStackTrace();
                            }
                        }
                        try {
                            Thread.sleep(1000);
                        } catch(InterruptedException e) {}
                    }
                }
            }.start();

        } catch(IOException err) {
            handleGUIBuilderError(err, "Error launching GUI builder: " + err);
        }
     
    }
       
         /**
     * Opens the source file, brings the IDE to the foreground. If methodSig doesn't exist it inserts the method
     * prototype at the end of the source file before the last curly bracket
     */
    public  void gotoSourceFileLine(File javaSource, String methodSig, String methodPrototype) {
        
    }

    public  void handleGUIBuilderError(Exception err, String message) {
        
    }

    /**
     * Launches the actual GUI builder jar executable
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    public  void launchGuiBuilderApp() throws MojoExecutionException{
        
        updateCodenameOne(false, getGuiBuilderJar());
        Java java = createJava();
        java.setFork(true);
        if ("true".equals(System.getProperty("spawn", "true"))) {
            java.setSpawn(true);
        }
        java.setJar(getGuiBuilderJar());
        java.executeJava();
    }
}
