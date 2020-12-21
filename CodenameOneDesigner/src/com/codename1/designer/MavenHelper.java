/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.designer;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author shannah
 */
public class MavenHelper {

    public static boolean isMavenProject(File inputFile)  {
        return new File(getProjectDir(inputFile), "pom.xml").exists();
    }

    public static File getProjectDir(File start) {
        File f = new File(start, "codenameone_settings.properties");

        while (!f.exists() && f.getParentFile().getParentFile() != null) {
            f = new File(f.getParentFile().getParentFile(), "codenameone_settings.properties");
            if (f.exists()) {
                return f.getParentFile();
            }
        }
        return f.exists() ? f.getParentFile() : null;

    }
    
    public static File getCSSDir(File start) {
        File projectDir = getProjectDir(start);
        if (isMavenProject(start)) {
            return new File(projectDir, "src" + File.separator + "main" + File.separator + "css");
        } else {
            return new File(projectDir, "css");
        }
        
    }
}
