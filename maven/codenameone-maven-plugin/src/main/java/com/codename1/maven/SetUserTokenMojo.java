package com.codename1.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.prefs.Preferences;

@Mojo(name = "set-user-token", requiresProject = false)
public class SetUserTokenMojo extends AbstractMojo {

    @Parameter(property = "token", required = true)
    private String token;

    @Parameter(property = "user", required = true)
    private String user;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (token == null || token.trim().isEmpty()) {
            throw new MojoFailureException("Missing required parameter: -Dtoken=<token>");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new MojoFailureException("Missing required parameter: -Duser=<email>");
        }

        Preferences prefs = Preferences.userRoot().node("/com/codename1/ui");
        prefs.put("token", token);
        prefs.put("user", user);
        try {
            // put() only mutates the in-memory node; force it to the backing
            // store now so a separate JVM (the build) reliably reads it back.
            prefs.flush();
        } catch (java.util.prefs.BackingStoreException ex) {
            throw new MojoExecutionException(
                    "Failed to persist Codename One credentials to the preferences backing store", ex);
        }

        getLog().info("Saved Codename One user token and user email to preferences node /com/codename1/ui");
    }
}
