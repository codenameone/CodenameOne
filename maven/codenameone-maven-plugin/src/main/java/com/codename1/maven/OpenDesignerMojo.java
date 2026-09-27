/*
 * Copyright (c) 2020, Codename One and/or its affiliates. All rights reserved.
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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;

/**
 * Opens the designer.
 */
@Mojo(name = "designer")
public class OpenDesignerMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        updateCodenameOne(false, getDesignerJar());
        Java java = createJava();
        java.setFork(true);
        java.setSpawn(true);
        java.setJar(getDesignerJar());
        java.createArg().setFile(getResourceFile());
        java.executeJava();
    }
 
    
    
     
    private File getResourceFile() {
        return new File(project.getProperties().getProperty("cn1.resourceFile", project.getCompileSourceRoots().get(0) + File.separator + "theme.res"));
        
        
    }
}
