/*
 * Copyright (c) 2021, Codename One and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tools.ant.taskdefs.Zip;

@Mojo(name="attach-test-artifact")
public class AttachTestArtifactMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        Zip zip = (Zip)antProject.createTask("zip");
        File testOutputs = new File(project.getBuild().getTestOutputDirectory());
        if (!testOutputs.exists()) {
            testOutputs.mkdir();
        }
        File dummy = new File(testOutputs, project.getGroupId()+"__"+project.getArtifactId()+"__cn1tests");
        try {
            dummy.createNewFile();
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to create dummy test file", ex);
        }
        zip.setBasedir(testOutputs);
        File dest = new File(project.getBuild().getDirectory() + File.separator + project.getBuild().getFinalName()+"-tests.jar");
        zip.setDestFile(dest);
        zip.setCompress(false);
        zip.execute();
        projectHelper.attachArtifact(project, "jar", "tests", dest);
        
        
    }
    
}
