/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/// `mvn cn1:feedback -Dnote="..."` records friction with Codename One -- a confusing
/// error, a missing API, a step that took too long -- as a Markdown file in the
/// project's `.cn1/feedback` directory. Agents are asked to log one whenever
/// something gets in their way, so the developer has a list to read or send.
///
/// Nothing leaves the machine: the note, the time, and the plugin, Java and OS
/// versions are written, and no source code. Sending it is the developer's call.
@Mojo(name = "feedback", requiresProject = false)
public class CN1FeedbackMojo extends AbstractCN1Mojo {
    /// What got in the way, in a sentence or two.
    @Parameter(property = "note")
    private String note;

    @Override
    protected String helpAction() {
        return "mvn cn1:feedback -Dnote=\"...\"";
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isFirstProjectOfTheBuild()) {
            return;
        }
        if (note == null || note.trim().isEmpty()) {
            throw new MojoFailureException("Say what got in the way: mvn cn1:feedback -Dnote=\"...\"");
        }
        File common = getCN1ProjectDir();
        File root = common != null && common.getParentFile() != null ? common.getParentFile()
                : new File(System.getProperty("user.dir"));
        try {
            File written = write(new File(root, ".cn1/feedback"), note.trim(),
                    com.codename1.maven.help.ToolingHelp.pluginVersion(), new Date());
            getLog().info("Feedback saved to " + written + " (it stays on this machine)");
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not save the feedback: " + ex.getMessage(), ex);
        }
    }

    /// Writes one note as its own file, named by the UTC time so the files sort, and
    /// never overwrites an earlier one written in the same second.
    static File write(File dir, String note, String pluginVersion, Date when) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        SimpleDateFormat name = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT);
        name.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        String base = name.format(when);
        File out = new File(dir, base + ".md");
        for (int i = 2; out.exists(); i++) {
            out = new File(dir, base + "-" + i + ".md");
        }
        String text = "# Codename One feedback\n\n"
                + note + "\n\n"
                + "- Time: " + iso.format(when) + "\n"
                + "- Plugin: " + pluginVersion + "\n"
                + "- Java: " + System.getProperty("java.version") + "\n"
                + "- OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n";
        Files.write(out.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return out;
    }
}
