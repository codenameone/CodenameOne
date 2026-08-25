/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

@Mojo(name="run")
public class CN1RunMojo extends AbstractCN1Mojo {

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
