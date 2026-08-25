package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

@Mojo(name="run")
public class CN1RunMojo extends AbstractCN1Mojo {

    /**
     * What the nested build is invoked with.
     *
     * <p>cn1:run is a nested Maven build, and nothing of the outer command line
     * reached it. So {@code mvn cn1:run -Dcodename1.arg.desktop.titleBar=NATIVE}
     * -- or {@code -Dcodename1.mainName} -- was accepted, printed, and then
     * dropped: the inner build overlaid nothing, process-annotations stamped the
     * manifest for the file's entry point, and the simulator ran on values the
     * same command line would have changed for a device build.</p>
     *
     * <p>Everything in the {@code codename1} namespace, which is the rule
     * {@code overlayCommandLineBuildHints} applies -- except the platform, which
     * is set last because this goal IS the javase simulator and a stray
     * {@code -Dcodename1.platform} must not send the nested build elsewhere.</p>
     */
    static Properties nestedBuildProperties(Properties userProperties) {
        Properties props = new Properties();
        if (userProperties != null) {
            for (String key : userProperties.stringPropertyNames()) {
                if (key.startsWith("codename1.")) {
                    props.setProperty(key, userProperties.getProperty(key));
                }
            }
        }
        props.setProperty("codename1.platform", "javase");
        return props;
    }

    @Override
    protected String helpStep() {
        return "local_run";
    }

    @Override
    protected String helpAction() {
        return "mvn cn1:run";
    }

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

        JavaVersionUtil.requireRuntimeJavaVersion(JavaVersionUtil.MIN_RUNTIME_JAVA_VERSION,
                "run the Codename One simulator");



        InvocationRequest request = new DefaultInvocationRequest();
        //request.setPomFile( new File( "/path/to/pom.xml" ) );

        request.setGoals( Arrays.asList( "verify") );
        request.setProfiles(Arrays.asList("simulator"));
        Properties props = nestedBuildProperties(
                getSession() == null ? null : getSession().getUserProperties());
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
