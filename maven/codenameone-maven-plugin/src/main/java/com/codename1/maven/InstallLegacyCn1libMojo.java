/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;

import static com.codename1.maven.PathUtil.path;

/**
 * A goal that installs a legacy cn1lib as a dependency.  This will generate a Maven project for the cn1lib inside
 * the "cn1libs" directory of the root project (assuming the project structure follows that of the cn1app-archetype.
 *
 * @author shannah
 */
@Mojo(name = "install-cn1lib")
public class InstallLegacyCn1libMojo extends AbstractCN1Mojo {

    private File cn1libsDirectory;

    /**
     * The path to the .cn1lib file to install.
     */
    @Parameter(property="file", required=true)
    private File file;

    /**
     * The groupID to use for the generated project.  If omitted, it will use the same groupId as the project.
     */
    @Parameter(property="groupId", required = false)
    private String groupId;

    /**
     * The artifactId to use for the generated project.  If omitted, it will use ${project.artifactId}-${libName}, where ${libName}
     * is the name of the cn1lib file with out the .cn1lib extension.
     * module
     */
    @Parameter(property="artifactId", required = false)
    private String artifactId;

    /**
     * The version for the generated project.  If omitted, it will use the ${project.version}.
     */
    @Parameter(property="version", required = false)
    private String version;

    /**
     * A boolean flag indicating whether it should automatically update the pom.xml file with the dependency.
     * Default true.
     */
    @Parameter(property="updatePom", required=false, defaultValue = "true")
    private boolean updatePom;

    /**
     * A boolean flag indicating whether it should overwrite an existing project of the same name.  Default false.
     */
    @Parameter(property="overwrite", required=false, defaultValue = "false")
    private boolean overwrite;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            // To make things sane, this mojo should only be executed for the common module.
            return;
        }
        Cn1libInstaller installer = new Cn1libInstaller(project, getLog());
        installer.setOverwrite(overwrite);
        installer.setUpdatePom(updatePom);
        installer.setVersion(version);
        installer.setArtifactId(artifactId);
        installer.setGroupId(groupId);
        installer.setFile(file);
        installer.executeImpl();
    }


    

    
}
