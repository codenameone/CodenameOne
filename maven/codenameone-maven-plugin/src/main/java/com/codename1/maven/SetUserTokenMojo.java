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

        getLog().info("Saved Codename One user token and user email to preferences node /com/codename1/ui");
    }
}
