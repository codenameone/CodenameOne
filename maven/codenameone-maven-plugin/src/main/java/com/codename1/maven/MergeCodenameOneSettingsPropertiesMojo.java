package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * A mojo that is run during the builds of cn1app projects which combines the
 * codenameone_settings.properties with the codenameone_library_appended.properties and
 * codenameone_library_required.properties files of all cn1libs, and writes them to
 * target/classes/codenameone_settings.properties, which will be used when sending builds
 * to the build server.
 */
@Mojo(name="merge-cn1-settings-properties", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class MergeCodenameOneSettingsPropertiesMojo extends AbstractCN1Mojo {
    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {

    }
}
