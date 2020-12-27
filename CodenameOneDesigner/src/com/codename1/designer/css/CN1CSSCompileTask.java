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
package com.codename1.designer.css;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Path;

/**
 *
 * @author shannah
 */
public class CN1CSSCompileTask extends Task {

    @Override
    public void execute() throws BuildException {
        
        String srcDirPath = getProject().getProperty("src.dir");
        if (srcDirPath == null || "".equals(srcDirPath)) {
            srcDirPath = "src";
        }
        
        File cssDir = new File(getProject().getBaseDir(), "css");
        if (!cssDir.exists()) {
            log("Skipping CSS task because no 'css' directory found in project.");
            return;
        }
        
        File srcDir = new File(getProject().getBaseDir(), srcDirPath);
        //System.out.println(getProject().getProperties());
        
        String runClasspath = getProject().getProperty("run.classpath");
        if (runClasspath == null || "".equals(runClasspath)) {
            runClasspath = "JavaSE.jar";
        }
        String[] classPath = runClasspath.split(":");
        String javaSEJarPath = null;
        for (String path : classPath) {
            if (path.endsWith("JavaSE.jar")) {
                javaSEJarPath = path;
                break;
            }
        }
        
        String codenameOneTempPath = System.getProperty("user.home") + File.separator + ".codenameone";
        
        String designerJarPath = cssDir.getAbsolutePath() + File.separator + "designer_1.jar";
        File designerJar = new File(designerJarPath);
        if (!designerJar.exists()) {
            designerJarPath = codenameOneTempPath + File.separator + "designer_1.jar";

            designerJar = new File(designerJarPath);
            if (!designerJar.exists()) {
                throw new BuildException("Could not find designer_1.jar file at path "+designerJar);
            }
        }
        
        File javaSEJar = new File(getProject().getBaseDir(), javaSEJarPath);
        if (!javaSEJar.exists()) {
            throw new BuildException("Could not find JavaSE.jar file at path "+javaSEJar);
        }
        
        
        File cssJar = new File(codenameOneTempPath, "cn1css.jar");
        
        
        if (!cssJar.exists()) {
            try {
                exportResource("cn1css.jar", cssJar);
            } catch (IOException ex) {
                throw new BuildException("Failed to export cn1css.jar to temp directory.", ex);
            }
        }
        
        try {
            String jarChecksum = getMD5Checksum(cssJar.getAbsolutePath());
            String newChecksum = getResourceAsString(getClass(), "cn1css.jar.MD5");
            if (!jarChecksum.equals(newChecksum)) {
                exportResource("cn1css.jar", cssJar);
            }
        } catch (IOException ex) {
            throw new BuildException("Failed to get checksum for "+cssJar, ex);
        }
        
        
        
        String[] cssFiles = cssDir.list((dir, name) -> {
            return name.endsWith(".css");
            
        });
        
        Map<String,String> checksums = loadChecksums();
        
        for (String cssFile : cssFiles) {
            try {
                File f = new File(cssDir, cssFile);
                File destFile = new File(srcDir, cssFile + ".res");
                if (destFile.exists() && f.lastModified() < destFile.lastModified()) {
                    log("Not compiling " + f + " because " + destFile + " has a newer modification time.");
                    
                    continue;
                }
                
                if (destFile.exists()) {
                    String checksum = getMD5Checksum(destFile.getAbsolutePath());
                    
                    String lastChecksum = checksums.get(cssFile);
                    
                    if (lastChecksum != null && !lastChecksum.equals(checksum)) {
                        throw new BuildException("The file "+destFile+" has been modified since it was last generated.  To avoid overwriting manual changes, please, delete this file and try to recompile.");
                    }
                    
                    
                }
                
                Java javaTask = (Java)getProject().createTask("java");
                Path cp = javaTask.createClasspath();
                //cp.add(new Path(getProject(), javaSEJar.getAbsolutePath()));
                cp.add(new Path(getProject(), designerJar.getAbsolutePath()));
                cp.add(new Path(getProject(), cssJar.getAbsolutePath()));
                
                javaTask.setClasspath(cp);
                javaTask.setFork(true);
                javaTask.setClassname("com.codename1.ui.css.CN1CSSCLI");
                javaTask.setFailonerror(true);
                String maxMemory = getProject().getProperty("cn1css.max.memory");
                if (maxMemory != null) {
                    javaTask.setMaxmemory("4096m");
                } else {
                    javaTask.setMaxmemory(maxMemory);
                }
                
                Argument arg = javaTask.createArg();
                arg.setValue(f.getAbsolutePath());
                
                Argument destArg = javaTask.createArg();
                destArg.setValue(destFile.getAbsolutePath());
                
                Argument swPipeline = javaTask.createJvmarg();
                swPipeline.setValue("-Dprism.order=sw");
                
                javaTask.execute();
                
                String checksum = getMD5Checksum(destFile.getAbsolutePath());
                checksums.put(cssFile, checksum);
                saveChecksums(checksums);
                

            } catch (Exception ex) {
                Logger.getLogger(CN1CSSCompileTask.class.getName()).log(Level.SEVERE, null, ex);
                throw new BuildException(ex.getMessage());
            }

        }

        super.execute(); //To change body of generated methods, choose Tools | Templates.
    }

    
    private static byte[] createChecksum(String filename) throws IOException  {
        try {
            InputStream fis =  new FileInputStream(filename);
            
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            
            fis.close();
            return complete.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
   }

   // see this How-to for a faster way to convert
   // a byte array to a HEX string
   private static String getMD5Checksum(String filename) throws IOException {
       byte[] b = createChecksum(filename);
       String result = "";

       for (int i=0; i < b.length; i++) {
           result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
       }
       return result;
   }
   
   private Map<String,String> loadChecksums() {
       File checkSums = getChecksumsFile();
       if (!checkSums.exists()){
           return new HashMap<String,String>();
       }
       HashMap<String,String> out = new HashMap<String,String>();
       try {
            Scanner scanner = new Scanner(checkSums);

            //now read the file line by line...
            int lineNum = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNum++;
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    out.put(parts[0], parts[1]);
                }
            }
            return out;
        } catch(Exception e) { 
            //handle this
            return out;
        }
       
   }
   
   private File getChecksumsFile() {
       return new File(getProject().getBaseDir(), ".cn1_css_checksums");
   }
   
   private void saveChecksums(Map<String,String> map) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileOutputStream(getChecksumsFile()))) {
            for (String key : map.keySet()) {
                out.println(key+":"+map.get(key));
            }
        }
       
   }
   
   static void exportResource(String resourceName, File targetFile) throws IOException {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        try {
            stream = CN1CSSCompileTask.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            resStreamOut = new FileOutputStream(targetFile);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            stream.close();
            resStreamOut.close();
        }

    }
   
   static String getResourceAsString(Class cls, String resourceName) throws IOException {
       return new Scanner(cls.getResourceAsStream(resourceName), "UTF-8").useDelimiter("\\A").next();
   }
}
