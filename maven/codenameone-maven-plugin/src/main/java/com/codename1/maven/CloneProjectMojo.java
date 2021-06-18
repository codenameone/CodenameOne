package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Collections;
import java.util.Properties;

import static com.codename1.maven.PathUtil.path;

/**
 * Generates a legacy .cn1lib file.
 * @author shannah
 */
@Mojo(name = "clone")
public class CloneProjectMojo extends AbstractCN1Mojo {

    @Parameter(property="artifactId", required = false, defaultValue = "")
    private String artifactId;

    @Parameter(property="groupId", required = false, defaultValue = "")
    private String groupId;

    @Parameter(property="version", defaultValue = "1.0-SNAPSHOT")
    private String version;

    @Parameter(property="gui", defaultValue = "false")
    private boolean gui;


    private String artifactIdToMainName(String artifactId) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : artifactId.toCharArray()) {
            if (sb.length() == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (!Character.isLetterOrDigit(c)){
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private boolean showGUIPrompt() {

        if (!EventQueue.isDispatchThread()) {
            try {
                final boolean[] result = new boolean[1];
                EventQueue.invokeAndWait(() -> {
                    result[0] = showGUIPrompt();
                });
                return result[0];
            } catch (Exception ex) {
                return false;
            }
        }

        JTextField tfArtifactId = new JTextField();
        JTextField tfGroupId = new JTextField();
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        tfArtifactId.setText(artifactId);
        if (artifactId == null || artifactId.isEmpty()) {
            tfArtifactId.setText(project.getParent().getArtifactId());
        }
        tfArtifactId.setToolTipText("Enter artifact ID for cloned project");
        tfArtifactId.setColumns(30);
        tfGroupId.setText(groupId);
        if (groupId == null || groupId.isEmpty()) {
            tfGroupId.setText(project.getGroupId());
        }
        tfGroupId.setColumns(30);
        tfGroupId.setToolTipText("Enter Group ID for cloned project");

        panel.add(new JLabel("Group ID: "));
        panel.add(tfGroupId);
        panel.add(new JLabel("Artifact ID:"));
        panel.add(tfArtifactId);

        int result = JOptionPane.showOptionDialog(null, panel, "Enter New Project Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
        if (result == JOptionPane.OK_OPTION) {
            artifactId = tfArtifactId.getText();
            groupId = tfGroupId.getText();
            return true;
        }

        return false;

    }


    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {

        if (!isCN1ProjectDir()) return;

        if (gui) {
            showGUIPrompt();
        }

        if (artifactId == null || artifactId.isEmpty()) {
            throw new MojoFailureException("artifactId is a required parameter.");
        }

        if (groupId == null || groupId.isEmpty()) {
            throw new MojoFailureException("groupId is a required parameter.");
        }

        File generateAppProjectProps = new File(getCN1ProjectDir().getParentFile(), "generate-app-project.rpf");

        if (!generateAppProjectProps.exists()) {
            // Need to generate rpf file with properties for project template
            Properties cn1Props = new Properties();
            try (FileInputStream fis = new FileInputStream(new File(getCN1ProjectDir(), "codenameone_settings.properties"))) {
                cn1Props.load(fis);
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to load codenameone_settings.properties.", ex);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("template.type=maven\n");
            sb.append("template.mainName=").append(cn1Props.getProperty("codename1.mainName")).append("\n");
            sb.append("template.packageName=").append(cn1Props.getProperty("codename1.packageName")).append("\n");
            sb.append("\n");
            if (new File(getCN1ProjectDir(), "pom.xml").exists()) {
                sb.append("[dependencies]\n");
                sb.append("====\n");
                try {
                    writeDependencies(sb, new File(getCN1ProjectDir(), "pom.xml"));
                } catch (IOException ex) {
                    throw new MojoFailureException("Failed to write generate-app-project.rpf while extracting depencies from common pom.xml file", ex);
                }
                sb.append("====\n\n");
            } else {
                throw new MojoFailureException("Cannot find common pom.xml file");
            }
            if (new File(getCN1ProjectDir().getParentFile(), "pom.xml").exists()) {
                sb.append("[parentDependencies]\n");
                sb.append("====\n");
                try {
                    writeDependencies(sb, new File(getCN1ProjectDir().getParentFile(), "pom.xml"));
                } catch (IOException ex) {
                    throw new MojoFailureException("Failed to write generate-app-project.rpf while extracting depencies from root pom.xml file", ex);
                }
                sb.append("====\n");
            }
            try {
                FileUtils.writeStringToFile(generateAppProjectProps, sb.toString(), "UTF-8");
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to write "+generateAppProjectProps+".", ex);
            }
        }

        String mainName = artifactIdToMainName(artifactId);

        File outputDirectory = new File(path(project.getBuild().getDirectory(), "generated-sources", "cn1-cloned-projects"));
        outputDirectory.mkdirs();
        InvocationRequest request = new DefaultInvocationRequest();
        //request.setPomFile( new File( "/path/to/pom.xml" ) );
        String pluginVersion = "LATEST";
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(getClass().getResourceAsStream("/META-INF/maven/com.codenameone/codenameone-maven-plugin/pom.xml"));
            pluginVersion = model.getVersion();
        } catch (Exception ex) {
            getLog().warn("Attempted to read archetype version from embedded pom.xml file but failed", ex);
        }

        request.setGoals( Collections.singletonList( "com.codenameone:codenameone-maven-plugin:"+pluginVersion+":generate-app-project" ) );
        Properties props = new Properties();
        props.setProperty("archetypeGroupId", "com.codenameone");
        props.setProperty("archetypeArtifactId", "cn1app-archetype");
        props.setProperty("archetypeVersion", pluginVersion);
        props.setProperty("artifactId", artifactId);
        props.setProperty("groupId", groupId);
        props.setProperty("version", version);
        props.setProperty("mainName", mainName);
        props.setProperty("interactiveMode", "false");
        props.setProperty("sourceProject", getCN1ProjectDir().getParentFile().getAbsolutePath());
        props.setProperty("cn1Version", pluginVersion);
        request.setProperties(props);
        if (getLog().isErrorEnabled()) {
            request.setShowErrors(true);
        }
        if (getLog().isDebugEnabled()) {
            request.setDebug(true);
        }




        Invoker invoker = new DefaultInvoker();
        invoker.setWorkingDirectory(outputDirectory);

        try {
            InvocationResult result = invoker.execute( request );
            if (result.getExitCode() != 0) {
                throw new MojoFailureException("Failed to generate project.  Exit code "+result.getExitCode());
            }
        } catch (MavenInvocationException ex) {
            getLog().error("Failed to clone project");
            throw new MojoExecutionException(ex.getMessage(), ex);

        }
        getLog().info("Project created at "+outputDirectory+File.separator+artifactId);
    }

    private void writeDependencies(StringBuilder sb, File pom) throws IOException {

        if (!pom.exists()) {
            throw new IOException("Cannot write dependencies because "+pom+" does not exist");
        }

        Model model;
        try (FileInputStream fis = new FileInputStream(pom)){
            MavenXpp3Reader reader = new MavenXpp3Reader();
            model = reader.read(fis);
        } catch (Exception ex) {
            throw new IOException("Failed to read dummy pom.xml file while injecting dependencies into "+pom, ex);
        }


        String dummyModelStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>link.sharpe</groupId>\n" +
                "    <artifactId>mavenproject1</artifactId>\n" +
                "    <version>1.0-SNAPSHOT</version>\n" +
                "    <dependencies>\n" +
                "    </dependencies>\n" +
                "</project>";
        Model dummyModel;
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            dummyModel = reader.read(new CharArrayReader(dummyModelStr.toCharArray()));
        } catch (Exception ex) {
            throw new IOException("Failed to read dummy pom.xml", ex);
        }
        for(Dependency dep : model.getDependencies()) {
            if (dep.getArtifactId().equals("codenameone-core") && dep.getGroupId().equals("com.codenameone")) continue;
            dummyModel.addDependency(dep);
        }
        MavenXpp3Writer writer = new MavenXpp3Writer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(baos, dummyModel);

        String dummyPomStr = new String(baos.toByteArray(), "UTF-8");
        int startPos = dummyPomStr.indexOf("<dependencies>");
        if (startPos >= 0 ) startPos += +"<dependencies>".length();
        if (startPos >= 0) {
            int endPos = dummyPomStr.indexOf("</dependencies>");
            if (endPos < 0) {
                throw new IOException("Malformed pom.xml generated for dependencies.  Could not find closing dependencies tag.");
            }
            sb.append(dummyPomStr.substring(startPos, endPos)).append("\n");
        }


    }
}
