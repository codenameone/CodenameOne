/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static com.codename1.maven.PathUtil.path;

/**
 * A goal that installs a legacy cn1lib as a dependency.  This will generate a Maven project for the cn1lib inside
 * the "cn1libs" directory of the root project (assuming the project structure follows that of the cn1app-archetype.
 *
 * @author shannah
 */

public class Cn1libInstaller {

    private File cn1libsDirectory;

    /**
     * The path to the .cn1lib file to install.
     */
    private File file;

    /**
     * The groupID to use for the generated project.  If omitted, it will use the same groupId as the project.
     */
    private String groupId;

    /**
     * The artifactId to use for the generated project.  If omitted, it will use ${project.artifactId}-${libName}, where ${libName}
     * is the name of the cn1lib file with out the .cn1lib extension.
     * module
     */
    private String artifactId;

    /**
     * The version for the generated project.  If omitted, it will use the ${project.version}.
     */
    private String version;

    /**
     * A boolean flag indicating whether it should automatically update the pom.xml file with the dependency.
     * Default true.
     */
    private boolean updatePom;

    /**
     * A boolean flag indicating whether it should overwrite an existing project of the same name.  Default false.
     */
    private boolean overwrite;



    private MavenProject _project;
    private Project antProject;


    private Log log;

    private Log getLog() {
        return log;
    }


    private void setupAntProject() {
        antProject = new Project();
        if (getProjectBasedir() != null) {
            antProject.setBaseDir(getProjectBasedir());
        } else {
            antProject.setBaseDir(new File("."));
        }
        antProject.setDefaultInputStream(System.in);

        InputHandler handler = new DefaultInputHandler();
        antProject.setProjectReference(handler);
        antProject.setInputHandler(handler);


        antProject.init();
    }

    /**
     * Creates a new Cn1libInstaller
     * @param project The MavenProject of the target app's common project.
     * @param log The log.
     */
    public Cn1libInstaller(MavenProject project, Log log) {
        Properties props = project.getProperties();
        file = props.containsKey("file") ? new File(project.getProperties().getProperty("file")) : null;
        groupId = props.getProperty("groupId");
        artifactId = props.getProperty("artifactId");
        version = props.getProperty("version");
        updatePom = !"false".equals(props.getProperty("updatePom"));
        overwrite = "true".equals(props.getProperty("overwrite"));
        this._project = project;
        this.log = log;
        setupAntProject();

    }

    /**
     * Creates a new Cn1libInstaller
     * @param basedir The directory of the target app's "common" project.
     * @param baseGroupId The groupId of the target app's root module.
     * @param baseArtifactId The artifactId of the target app's root module.
     * @param baseVersion The version of the target app's root module.
     * @param log The log.
     */
    public Cn1libInstaller(File basedir, String baseGroupId, String baseArtifactId, String baseVersion, Log log) {
        this.log = log;
        this.basedir = basedir;
        this.baseGroupId = baseGroupId;
        this.baseArtifactId = baseArtifactId;
        this.baseVersion = baseVersion;
        setupAntProject();

    }


    private File basedir;
    private File getProjectBasedir() {
        if (_project != null) {
            return _project.getBasedir();
        }
        return basedir;
    }

    public void setProjectBasedir(File basedir) {
        this.basedir = basedir;
    }


    private String getProjectBuildDirectory() {
        if (_project != null) {
            return _project.getBuild().getDirectory();
        }
        return new File(getProjectBasedir(), "target").getAbsolutePath();
    }


    private String baseGroupId, baseArtifactId, baseVersion;

    private String getBaseGroupId() {
        if (_project != null) {
            return _project.getParent().getGroupId();
        }
        return baseGroupId;
    }

    public void setBaseGroupId(String groupId) {
        this.baseGroupId = groupId;
    }

    private String getBaseArtifactId() {
        if (_project != null) {
            return _project.getParent().getArtifactId();
        }
        return baseArtifactId;
    }

    public void setBaseArtifactId(String artifactId) {
        this.baseArtifactId = artifactId;
    }

    private String getBaseVersion() {
        if (_project != null) {
            return _project.getParent().getVersion();
        }
        return baseVersion;
    }

    public void setBaseVersion(String baseVersion) {
        this.baseVersion = baseVersion;
    }

    private void checkProps() {
        if (_project != null) {
            return;
        }

        if (this.baseVersion == null) {
            throw new IllegalStateException("baseVersion not set.  Should be set to the version of the parent app project.");
        }

        if (this.baseArtifactId == null) {
            throw new IllegalStateException("baseArtifactId not set.  Should be set to the aftifactId of the parent app project");
        }

        if (this.baseGroupId == null) {
            throw new IllegalStateException("baseGroupId not set.  Should be set to the groupId of the parent app project");
        }

        if (this.basedir == null) {
            throw new IllegalStateException("basedir is not set.  Should be set to the common project directory");
        }

    }


    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        checkProps();
        File canonicalFile;
        try {
            canonicalFile = getProjectBasedir().getCanonicalFile();
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to get canonical file for project basedir", ex);
        }

        if (!canonicalFile.getName().equals("common")) {
            throw new MojoFailureException("install-cn1lib goal can only be run from the standard maven project format.  The project directory name is expected to be 'common' but found '"+canonicalFile.getName()+"'");

        }

        if (canonicalFile.getParentFile() == null) {
            throw new MojoFailureException("Cannot run install-cn1lib goal because the project structure is invalid. Cannot find parent directory of project");
        }

        File parentPom = new File(canonicalFile.getParentFile(), "pom.xml");
        if (!parentPom.exists()) {
            throw new MojoFailureException(("Cannot run install-cn1lib goal because the project structure is invalid. Cannot find file "+parentPom));
        }

        if (!file.exists()) {
            throw new MojoExecutionException("The file "+ file +" cannot be found.");
        }
        
        if (!file.getName().endsWith(".cn1lib")) {
            throw new MojoExecutionException("File must have .cn1lib extension");
        }
        
        File cn1libsDirectory = new File(canonicalFile.getParentFile(), "cn1libs");
        if (!cn1libsDirectory.exists()) {
            cn1libsDirectory.mkdirs();
        }
        this.cn1libsDirectory = cn1libsDirectory;
        String libName = file.getName().substring(0, file.getName().lastIndexOf("."));

        File cn1libDirectory = new File(cn1libsDirectory, libName);
        if (cn1libDirectory.exists()) {
            if (overwrite) {
                try {
                    FileUtils.deleteDirectory(cn1libDirectory);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to delete existing "+cn1libDirectory, ex);
                }
            } else {
                throw new MojoFailureException("Directory "+cn1libDirectory+" already exists.  Add the -Doverwrite=true or delete this directory and try again.");
            }
        }
        cn1libDirectory.mkdir();
        File cn1libJars = new File(cn1libDirectory, "jars");


        
        File generatedSources = new File(getProjectBuildDirectory() + File.separator + "generated-sources");
        generatedSources.mkdirs();
        

        File libOutputDir = new File(generatedSources, libName);
        if (libOutputDir.exists()) {
            try {
                FileUtils.deleteDirectory(libOutputDir);
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to delete directory.", ex);
            }
        }
        libOutputDir.mkdirs();
        
        File libOutputJars = new File(libOutputDir, "jars");
        Expand unzip = (Expand)antProject.createTask("unzip");
        unzip.setSrc(file);
        unzip.setDest(libOutputJars);
        unzip.execute();
        
        
        
        String groupId = this.groupId;
        if (groupId == null) {
            groupId = getBaseGroupId();
        }
        String artifactId = this.artifactId;
        if (artifactId == null) {
            artifactId = getBaseArtifactId() + "-" +libName;
        }
        String version = this.version;
        if (version == null) {
            version = getBaseVersion();
        }
        
        File seJar = new File(libOutputJars, "nativese.zip");
        if (seJar.exists()) {
            //installFile(seJar, groupId, artifactId, version, "javase");
            try {
                FileUtils.copyFile(seJar, new File(cn1libJars, seJar.getName()));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+seJar+" to "+cn1libJars, ex);
            }
        }
        
        File androidJar = new File(libOutputJars, "nativeand.zip");
        if (androidJar.exists()) {
            //installFile(androidJar, groupId, artifactId, version, "android");
            try {
                FileUtils.copyFile(androidJar, new File(cn1libJars, androidJar.getName()));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+androidJar+" to "+cn1libJars, ex);
            }
        }
        
        File iosJar = new File(libOutputJars, "nativeios.zip");
        if (iosJar.exists()) {
            //installFile(iosJar, groupId, artifactId, version, "ios");
            try {
                FileUtils.copyFile(iosJar, new File(cn1libJars, iosJar.getName()));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+iosJar+" to "+cn1libJars, ex);
            }
        }
        
        File jsJar = new File(libOutputJars, "nativejavascript.zip");
        if (jsJar.exists()) {
            //installFile(jsJar, groupId, artifactId, version, "javascript");
            try {
                FileUtils.copyFile(jsJar, new File(cn1libJars, jsJar.getName()));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+jsJar+" to "+cn1libJars, ex);
            }
        }
        
        File mainJar = new File(libOutputJars, "main.zip");

        // No main jar exists.  Let's generate it
        File mainTmp = new File(libOutputJars, "main");
        mainTmp.mkdir();
        File metaDir = new File(mainTmp, path("META-INF", "codenameone", groupId, artifactId));
        metaDir.mkdirs();
        File appendedProps = new File(libOutputJars, "codenameone_library_appended.properties");
        File requiredProps = new File(libOutputJars, "codenameone_library_required.properties");
        if (appendedProps.exists()) {
            try {
                FileUtils.copyFile(appendedProps, new File(metaDir, appendedProps.getName()));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+appendedProps, ex);
            }
        }
        if (requiredProps.exists()) {
            try {
                FileUtils.copyFile(appendedProps, new File(metaDir, requiredProps.getName()));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+requiredProps, ex);
            }
        }
        Zip zip = (Zip)antProject.createTask("zip");
        zip.setBasedir(mainTmp);
        zip.setDestFile(mainJar);
        zip.setCompress(true);
        zip.setUpdate(mainJar.exists());
        zip.execute();


        if (mainJar.exists()) {
            //installFile(mainJar, groupId, artifactId, version, "common");
            try {
                FileUtils.copyFile(mainJar, new File(cn1libJars, mainJar.getName()));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+mainJar+" to "+cn1libJars, ex);
            }
        }
        
        File winJar = new File(libOutputJars, "nativewin.zip");
        if (winJar.exists()) {
            //installFile(winJar, groupId, artifactId, version, "win");
            try {
                FileUtils.copyFile(winJar, new File(cn1libJars, winJar.getName()));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+winJar+" to "+cn1libJars, ex);
            }
        }
        
        
        File cssJar = new File(libOutputJars, "css.zip");
        if (cssJar.exists()) {
            // We can't pass the cssJar as is beause the css directory must
            // be rebased to the META-INF/codenameone/groupId/artifactId/css directory
            // so it won't conflict with the css in other artifacts
            File cssTmp = new File(libOutputJars, path("META-INF", "codenameone", groupId, artifactId, "css"));
            cssTmp.getParentFile().mkdirs();
            Expand unzipCss = (Expand)antProject.createTask("unzip");
            unzipCss.setSrc(cssJar);
            unzipCss.setDest(cssTmp);
            unzipCss.execute();

            Zip zipCss = (Zip)antProject.createTask("zip");
            zipCss.setBasedir(cssTmp);
            File rebasedCssJar = new File(cssJar.getParentFile(), "css-rebased.zip");
            zipCss.setDestFile(rebasedCssJar);
            zipCss.execute();
            //installFile(rebasedCssJar, groupId, artifactId, version, "cn1css", "zip");
            cssJar = rebasedCssJar;

            try {
                FileUtils.copyFile(cssJar, new File(cn1libJars, "css.zip"));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to copy "+cssJar+" to "+cn1libJars, ex);
            }

        }
        
        
        String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
                "<parent>" +
                "  <artifactId>" + getBaseArtifactId() + "-cn1libs</artifactId>\n" +
                "  <groupId>" + getBaseGroupId() + "</groupId>\n" +
                "  <version>" + getBaseVersion() + "</version>\n" +
                "</parent>\n" +

            "\n" +
            "  <groupId>"+groupId+"</groupId>\n" +
            "  <artifactId>"+artifactId+"</artifactId>\n" +
            "  <version>"+version+"</version>\n" +
            "  <packaging>pom</packaging>\n" +
                "  <name>"+libName+"</name>\n" +
            "  <dependencies>\n" +
                (mainJar.exists()? (
                 "<dependency>\n"
                + "     <groupId>"+groupId+"</groupId>\n"
                + "     <artifactId>"+artifactId+"</artifactId>\n"
                + "     <version>"+version+"</version>\n"
                + "     <classifier>common</classifier>\n"
                + "     <type>jar</type>\n"
                + "</dependency>\n") : "")
                + (cssJar.exists()? (
                      "<dependency>\n" +
                              "  <groupId>"+groupId+"</groupId>\n" +
                              "  <artifactId>"+artifactId+"</artifactId>\n" +
                              "  <version>"+version+"</version>\n" +
                              "  <classifier>cn1css</classifier>\n" +
                              "  <type>zip</type>\n" +
                              "</dependency>\n"
                ) : "")
                +"</dependencies>\n"
                +"<profiles>\n"
                + (seJar.exists() ? profileXml("javase", groupId, artifactId, version) : "")
                + (androidJar.exists() ? profileXml("android", groupId, artifactId, version) : "")
                + (iosJar.exists() ? profileXml("ios", groupId, artifactId, version): "")
                + (jsJar.exists() ? profileXml("javascript", groupId, artifactId, version) : "")
                + (winJar.exists() ? profileXml("win", groupId, artifactId, version) : "")
                + "</profiles>\n" +
                "<build>\n" +
                "  <plugins>\n" +
                "    <plugin>\n" +
                "      <groupId>org.codehaus.mojo</groupId>\n" +
                "      <artifactId>build-helper-maven-plugin</artifactId>\n" +
                "      <version>1.7</version>\n" +
                "      <executions>\n" +
                "          <execution>\n" +
                "            <id>attach-artifacts</id>\n" +
                "            <phase>package</phase>\n" +
                "            <goals>\n" +
                "              <goal>attach-artifact</goal>\n" +
                "            </goals>\n" +
                "            <configuration>\n" +

                "              <artifacts>\n" +
                (mainJar.exists() ? ("<artifact>\n" +
                        "<file>${basedir}/jars/"+mainJar.getName()+"</file>\n" +
                        "<type>jar</type>\n" +
                        "<classifier>common</classifier>\n" +
                        "</artifact>\n") : "") +
                (seJar.exists() ? ("<artifact>\n" +
                        "<file>${basedir}/jars/"+seJar.getName()+"</file>\n" +
                        "<type>jar</type>\n" +
                        "<classifier>javase</classifier>\n" +
                        "</artifact>\n") : "") +
                (androidJar.exists() ? ("<artifact>\n" +
                        "<file>${basedir}/jars/"+androidJar.getName()+"</file>\n" +
                        "<type>jar</type>\n" +
                        "<classifier>android</classifier>\n" +
                        "</artifact>\n") : "") +
                (iosJar.exists() ? ("<artifact>\n" +
                        "<file>${basedir}/jars/"+iosJar.getName()+"</file>\n" +
                        "<type>jar</type>\n" +
                        "<classifier>ios</classifier>\n" +
                        "</artifact>\n") : "") +
                (jsJar.exists() ? ("<artifact>\n" +
                        "<file>${basedir}/jars/"+jsJar.getName()+"</file>\n" +
                        "<type>jar</type>\n" +
                        "<classifier>javascript</classifier>\n" +
                        "</artifact>\n") : "") +
                (winJar.exists() ? ("<artifact>\n" +
                        "<file>${basedir}/jars/"+winJar.getName()+"</file>\n" +
                        "<type>jar</type>\n" +
                        "<classifier>win</classifier>\n" +
                        "</artifact>\n") : "") +
                (cssJar.exists() ? ("<artifact>\n" +
                        "<file>${basedir}/jars/css.zip</file>\n" +
                        "<type>zip</type>\n" +
                        "<classifier>cn1css</classifier>\n" +
                        "</artifact>\n") : "") +

                "              </artifacts>\n" +
                "            </configuration>\n" +
                "          </execution>\n" +
                "        </executions>\n" +
                "      </plugin>\n" +
                "    </plugins>\n" +
                "   </build>\n"
                + "</project>"
                ;
        
        File pomFile = new File(cn1libDirectory, "pom.xml");
        try {
            FileUtils.writeStringToFile(pomFile, pomXml, "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException("Cannot write pom.xml file", ex);
        }
        
        //installFile(pomFile, groupId, artifactId, project.getVersion(), null, "pom");
        

        String dependencyString = "<dependency>\n" +
                "  <artifactId>"+artifactId+"</artifactId>\n" +
                "  <groupId>"+groupId+"</groupId>\n" +
                "  <version>"+version+"</version>\n" +
                "  <type>pom</type>\n" +
                "</dependency>\n";




        if (updatePom) {
            // We will update the Pom files now.  The "common" project should have a <dependency> added
            // and the parent project should have the <module> added.

            File projectPom = new File(getProjectBasedir(), "pom.xml");
            getLog().info("Attempting to update "+projectPom+" with the the dependency:\n"+dependencyString);
            String pomContents;
            try {
                pomContents = FileUtils.readFileToString(projectPom, "UTF-8");
            }  catch (IOException ex) {
                throw new MojoExecutionException("Failed to read pom file for updating dependencies", ex);
            }
            boolean writeChanges = false;
            if (pomContents.contains("<artifactId>"+artifactId+"</artifactId>")) {
                // The artifact is already listed in the pom file
                getLog().warn("The artifact "+artifactId+" is already referenced in the project Pom file at "+projectPom+". To avoid conflicts we are not adding the dependency.  You should manually update your dependency to the following:\n" +
                        dependencyString);


            } else {
                String marker = "<!-- INJECT DEPENDENCIES -->";
                if (!pomContents.contains(marker)) {
                    getLog().warn("The project pom does not include an dependency injection marker to indicate where dependencies should be added to the pom file.\n" +
                            "Not adding the dependency for the cnlib "+ file +".\n" +
                            "Please add the following dependency to your project pom file at "+projectPom+":\n" +
                            dependencyString);
                } else {
                    pomContents = pomContents.replace(marker, dependencyString + "\n" + marker);
                    try {
                        FileUtils.writeStringToFile(projectPom, pomContents, "UTF-8");
                    } catch (IOException ex) {
                        throw new MojoExecutionException("Failed to write changes to "+projectPom+".  Please add the dependency manually by adding the following snippet to the build/dependencies section of your pom.xml file: \n" +
                                dependencyString, ex);
                    }
                    getLog().info("Successfully injected the dependency into "+projectPom+"\n"+dependencyString);

                }
            }

            File parentPomFile = new File(cn1libsDirectory, "pom.xml");


            String injectedXml = "<!-- Profile injected by install-cn1lib goal for "+libName+" cn1lib -->\n" +
                    "<profile>\n" +
                    "  <id>"+file.getName()+"-cn1lib</id>\n" +
                    "  <activation>\n" +
                    "    <file><exists>${basedir}/"+libName+"/pom.xml</exists></file>\n" +
                    "  </activation>" +
                    "  <modules><module>"+libName+"</module></modules>\n" +
                    "</profile>\n";

            String parentPomContents;
            if (parentPomFile.exists()) {
                try {
                    parentPomContents = FileUtils.readFileToString(parentPomFile, "UTF-8");
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to read " + parentPomFile + " for injecting the cn1lib profile.  Please insert this profile manually:\n" +
                            injectedXml, ex);
                }
            } else {
                parentPomContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <parent>  <artifactId>"+getBaseArtifactId()+"</artifactId>\n" +
                        "        <groupId>"+getBaseGroupId()+"</groupId>\n" +
                        "        <version>"+getBaseVersion()+"</version>\n" +
                        "    </parent>\n" +
                        "\n" +
                        "    <groupId>"+getBaseGroupId()+"</groupId>\n" +
                        "    <artifactId>"+getBaseArtifactId()+"-cn1libs</artifactId>\n" +
                        "    <version>"+getBaseVersion()+"</version>\n" +
                        "    <packaging>pom</packaging>\n" +
                        "    <name>"+getBaseArtifactId()+"-cn1libs</name>\n" +
                        "    <profiles>\n" +
                        "    </profiles>\n" +
                        "</project>";
            }
            if (parentPomContents.contains("<module>cn1libs/"+libName+"</module>")) {
                getLog().warn("The module "+libName+" is already referenced inside "+parentPomFile+".  Please review that pom file and, if necessary, add the following snippet manaullay:\n" +
                        injectedXml);
            } else {
                if (!parentPomContents.contains("</profiles>")) {
                    if (!parentPomContents.contains("</project>")) {
                        throw new MojoExecutionException("During attempt to inject <profiles> section into "+parentPomFile+" could not find a closing </project> tag.");
                    }
                    parentPomContents = parentPomContents.replace("</project>", "<profiles>\n</profiles>\n</project>");
                }
                parentPomContents = parentPomContents.replace("</profiles>", injectedXml+"\n" + "</profiles>");
                getLog().info("Attempting to inject the following snippet into the <profiles> section of your pom.xml file at "+parentPomFile+":\n" +
                        injectedXml);
                try {
                    FileUtils.writeStringToFile(parentPomFile, parentPomContents, "UTF-8");

                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to update the pom file "+parentPomFile+".  Attempting to update with the following content:\n" +
                            parentPomFile);
                }
            }


        }


        
    }


    
    private String profileXml(String platform, String groupId, String artifactId, String version) {
        return  "  <profile>\n"
                + "    <id>"+platform+"</id>"
                + "     <activation>"
                + "         <property><name>codename1.platform</name><value>"+platform+"</value></property>\n"
                + "     </activation>"
                + "     <dependencies>"
                + "         <dependency>"
                + "             <groupId>"+groupId+"</groupId>\n"
                + "             <artifactId>"+artifactId+"</artifactId>\n"
                + "             <version>"+version+"</version>\n"
                + "             <type>jar</type>\n"
                + "             <classifier>"+platform+"</classifier>\n"
                + "         </dependency>\n"
                + "     </dependencies>\n"
                + "  </profile>";
    }
    
     private void installFile(File file, String groupId, String artifactId, String version, String classifier) throws MojoExecutionException {
         installFile(file, groupId, artifactId, version, classifier, "jar");
     }
    
    private void installFile(File file, String groupId, String artifactId, String version, String classifier, String packaging) throws MojoExecutionException {
        InvocationRequest request = new DefaultInvocationRequest();
        //request.setPomFile( new File( "/path/to/pom.xml" ) );
        
        request.setGoals( Collections.singletonList( "install:install-file" ) );
        
        Properties props = new Properties();
        props.setProperty("file", file.getAbsolutePath());
        props.setProperty("groupId", groupId);
        props.setProperty("artifactId", artifactId);
        props.setProperty("packaging", packaging);
        props.setProperty("version", version);
        props.setProperty("interactiveMode", "false");
        if (classifier != null) props.setProperty("classifier", classifier);
        props.setProperty("localRepositoryPath", cn1libsDirectory.getAbsolutePath());
                
        
        
        request.setProperties(props);
        
        Invoker invoker = new DefaultInvoker();
        try {
            invoker.execute( request );
        } catch (MavenInvocationException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
                    
        }
    }


    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void setUpdatePom(boolean updatePom) {
        this.updatePom = updatePom;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
