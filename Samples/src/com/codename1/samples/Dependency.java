/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.samples;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dependency {

    /**
     * @return the projectDir
     */
    public File getProjectDir() {
        return projectDir;
    }

    /**
     * @param projectDir the projectDir to set
     */
    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }

    public Dependency(String libName) {
        this.name = libName;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public File getFile(SamplesContext context) {
        String fileName = name;
        if (!fileName.endsWith(".cn1lib")) {
            fileName += ".cn1lib";
        }
        return new File(context.getLibrariesDir(), fileName);
    }
    
    public URL getURL() {
        try {
            return new URL("https://github.com/codenameone/CodenameOneLibs/raw/master/cn1libs/"+name+".cn1lib");
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void update(SamplesContext context) throws IOException {
        if (HTTPUtil.requiresUpdate(getURL(), getFile(context))) {
            try {
                getFile(context).getParentFile().mkdirs();
                HTTPUtil.update(getURL(), getFile(context), new File(System.getProperty("java.io.tmpdir")), false, false);
            } catch (HttpsRequiredException|FingerprintChangedException ex) {
                throw new RuntimeException(ex);
            } 
        }
    }
    
    
    public int buildProject(SamplesContext context) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(context.getAnt());
        cmd.add("-f");
        cmd.add(new File(getProjectDir(), "build.xml").getAbsolutePath());
        cmd.add("jar");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process p = pb.start();

        int result = p.waitFor();
        return result;
    }
    
    
    private String name;
    private File projectDir;
    
}
