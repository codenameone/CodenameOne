package com.codename1.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.prefs.Preferences;

@Mojo(name = "print-user-token", requiresProject = false)
public class PrintUserTokenMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Preferences prefs = Preferences.userRoot().node("/com/codename1/ui");
        String token = prefs.get("token", null);
        if (token == null || token.isEmpty()) {
            throw new MojoFailureException("No user token found in preferences at /com/codename1/ui");
        }
        System.out.println(token);
    }
}
