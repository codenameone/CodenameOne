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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.maven.artifact.Artifact;

public class Cn1libUtil {
    /**
     * Checks if the given jar file is a CN1lib.  CN1libs have a cn1lib section in
     * their manifest file, this is how they can be identified.
     * @param jar
     * @return
     * @throws IOException 
     */
    public static boolean isCN1Lib(File jar) {
        if (!jar.exists()) {
            return false;
        }
        try {
            JarFile jarFile = new JarFile(jar);
            
            Manifest mf = jarFile.getManifest();
            if (mf == null) {
                return false;
            }

            Attributes atts = mf.getAttributes("cn1lib");
            if (atts == null) {
                return false;
            }
            String version = atts.getValue("Version");
            return version != null;
        } catch (IOException ex) {
            return false;
        }
        
    }
    
    public static File getLibDirFor(Artifact artifact) {
        File artifactFile = artifact.getFile();
        
        if (artifactFile == null || !isCN1Lib(artifactFile)) {
            return null;
        }
        File artifactDir = new File(artifactFile.getParentFile(), artifactFile.getName()+"-extracted");
        return artifactDir;
    }
    
    public static File getNativeSEJar(Artifact artifact) {
        return getNativeJar(artifact, "javase");
       
    }
    
    public static File getNativeJar(Artifact artifact, String platform) {
        File libDir = getLibDirFor(artifact);
        if (libDir == null) {
            return null;
        }
        
        File metaInf = new File(libDir, "META-INF");
        if (!metaInf.exists()) {
            return null;
        }
        
        File cn1libDir =new File(metaInf, "cn1lib");
        if (!cn1libDir.exists()) {
            return null;
        }
        
        File nativeSeJar = new File(cn1libDir, "native"+platform+".zip");
        if (nativeSeJar.exists()) {
            return nativeSeJar;
        }
        return null;
       
    }
    
    public static File getNativeIOSJar(Artifact artifact) {
        return getNativeJar(artifact, "ios");
    }
    
    public static File getNativeAndroidJar(Artifact artifact) {
        return getNativeJar(artifact, "android");
    }
    
    public static File getNativeJavascriptJar(Artifact artifact) {
        return getNativeJar(artifact, "javascript");
    }
    
    
    public static List<File> getNativeSEEmbeddedJars(Artifact artifact) {
        List<File> out = new ArrayList<File>();
        File nativeSEJar = getNativeSEJar(artifact);
        if (nativeSEJar == null) {
            return out;
        }
        File extracted = new File(nativeSEJar.getParentFile(), nativeSEJar.getName()+"-extracted");
        if (extracted.exists()) {
            for (File child : extracted.listFiles()) {
                if (child.getName().endsWith(".jar")) {
                    out.add(child);
                }
            }
        }
        return out;
    }
}
