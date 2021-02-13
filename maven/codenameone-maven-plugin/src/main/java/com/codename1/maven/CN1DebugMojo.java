package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

@Mojo(name="debug")
public class CN1DebugMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File commonDir = getCN1ProjectDir();
        if (commonDir == null) {
            return;
        }
        File rootMavenProjectDir = commonDir.getParentFile();
        File javaSEDir = new File(rootMavenProjectDir, "javase");
        if (!javaSEDir.exists()) {
            return;
        }



        InvocationRequest request = new DefaultInvocationRequest();
        //request.setPomFile( new File( "/path/to/pom.xml" ) );

        request.setGoals( Arrays.asList( "verify") );
        request.setProfiles(Arrays.asList("debug-simulator"));
        Properties props = new Properties();
        props.setProperty("codename1.platform", "javase");

        props.setProperty("jpda.address", System.getProperty("jpda.address"));


        request.setProperties(props);
        request.setBaseDirectory(rootMavenProjectDir);

        Invoker invoker = new DefaultInvoker();
        try {
            invoker.execute( request );
        } catch (MavenInvocationException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);

        }
    }
}
