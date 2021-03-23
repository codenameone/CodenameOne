/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.net.URL;

/**
 * A mojo that updates Codename One.
 * @author shannah
 */
@Mojo(name = "update")
public class UpdateCodenameOneMojo extends AbstractCN1Mojo {


    @Parameter(property="newVersion", defaultValue = "")
    private String newVersion;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        updateCodenameOne(true);

        String existingCn1Version = project.getModel().getProperties().getProperty("cn1.version");
        String existingCn1PluginVersion = project.getModel().getProperties().getProperty("cn1.plugin.version");
        boolean isAutoVersion = false;
        if (newVersion == null || newVersion.isEmpty()) {
            if (!existingCn1Version.endsWith("-SNAPSHOT")) {
                // As long as the existing version is not a snapshot, we'll update to the latest in Maven
                // by default.
                newVersion = "LATEST";
                isAutoVersion = true;
            }
        }

        if ("LATEST".equals(newVersion)) {
            try {
                newVersion = findLatestVersionOnMavenCentral();
            } catch (Exception ex) {
                getLog().error("Failed to find latest version from Maven central", ex);
            }
        }


        getLog().info("Existing cn1.version="+existingCn1Version);
        getLog().info("Existing cn1.plugin.version="+existingCn1PluginVersion);
        if (newVersion != null && !newVersion.isEmpty() && (!newVersion.equals(existingCn1Version) || !newVersion.equals(existingCn1PluginVersion))) {

            getLog().info("Attempting to update project to version " + newVersion);
            MavenXpp3Reader pomReader = new MavenXpp3Reader();
            Model model = null;
            File pomFile = new File(project.getParent().getBasedir(), "pom.xml");
            try (FileInputStream fis = new FileInputStream(pomFile)) {
                model = pomReader.read(new InputStreamReader(fis, "UTF-8"), false);
            } catch (Exception ex) {
                getLog().error("Failed to load pom.xml file from parent project", ex);
                throw new MojoExecutionException("Failed to read pom.xml file", ex);
            }
            boolean changed = false;
            if (!isAutoVersion || !existingCn1Version.endsWith("-SNAPSHOT")) {
                if (!existingCn1Version.equals(newVersion)) {
                    getLog().info("Setting cn1.version=" + newVersion);
                    model.getProperties().setProperty("cn1.version", newVersion);
                    changed = true;
                } else {
                    getLog().info("cn1.version already up to date.  Not changing");
                }
            } else {
                getLog().warn("Not updating cn1.version because current version is a snapshot.  To update cn1.version property run mvn cn1:update -DnewVersion=XXXX");
            }
            if (!isAutoVersion || !existingCn1PluginVersion.endsWith("-SNAPSHOT")) {
                if (!existingCn1PluginVersion.equals(newVersion)) {
                    getLog().info("Setting cn1.plugin.version=" + newVersion);
                    model.getProperties().setProperty("cn1.plugin.version", newVersion);
                    changed = true;
                } else {
                    getLog().info("cn1.plugin.version already up to date. Not changing.");
                }
            } else {
                getLog().warn("Not updating cn1.plugin.version because current version is a snapshot.  To update cn1.plugin.version property, run mvn cn1:update -DnewVersion=XXX");
            }

            if (changed) {
                try {
                    FileUtils.copyFile(pomFile, new File(pomFile.getParentFile(), "pom.xml.bak"));
                } catch (Exception ex) {
                    throw new MojoExecutionException("Failed to back up pom.xml file", ex);
                }
                try (FileOutputStream fos = new FileOutputStream(pomFile)) {
                    MavenXpp3Writer pomWriter = new MavenXpp3Writer();
                    getLog().info("Updating "+pomFile+" with new cn1.version and cn1.plugin.version properties");

                    pomWriter.write(fos, model);


                } catch (IOException e) {
                    getLog().error("Failed to write changes to the pom file", e);
                    throw new MojoExecutionException("Failed to write canges to the pom file.", e);
                }
            }



        } else {
            if (newVersion == null || newVersion.isEmpty()) {
                getLog().warn("Not updating pom.xml file because it is currently set to use a SNAPSHOT version of Codename One.");
                getLog().info("To update to a newer version of CN1 in maven use the -DnewVersion property.");
                getLog().info("e.g. -DnewVersion=LATEST to update to the latest version in Maven central");
                getLog().info("or -DnewVersion=7.0.12, for example");
            } else {
                getLog().info("Maven version already up to date.  Not updating pom.xml file");
            }
        }






 
    }

    private String findLatestVersionOnMavenCentral() throws IOException, XmlPullParserException {
        URL mavenMetadata = new URL("https://repo1.maven.org/maven2/com/codenameone/codenameone-maven-plugin/maven-metadata.xml");
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        try (Reader input = new InputStreamReader(mavenMetadata.openStream(), "UTF-8")) {
            Metadata metadata = reader.read(input, false);
            return metadata.getVersioning().getLatest();
        }



    }
    
}
