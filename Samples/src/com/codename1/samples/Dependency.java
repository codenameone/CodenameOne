/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author shannah
 */
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
