/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author shannah
 */
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
