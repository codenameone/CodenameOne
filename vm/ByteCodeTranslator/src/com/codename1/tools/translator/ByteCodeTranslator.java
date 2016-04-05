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

package com.codename1.tools.translator;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Shai Almog
 */
public class ByteCodeTranslator {
    public static enum OutputType { 
        
        OUTPUT_TYPE_IOS {
            @Override
            public String extension() {
                return "m";
            }
        },
        OUTPUT_TYPE_CSHARP {
            @Override
            public String extension() {
                return "cs";
            }
        
        };

        public abstract String extension();
    };
    public static OutputType output = OutputType.OUTPUT_TYPE_IOS;
    public static boolean verbose = true;
    
    ByteCodeTranslator() {
    }
    
    /**
     * Recursively parses the files in the hierarchy to the output directory
     */
    void execute(File[] sourceDirs, File outputDir) throws Exception {
        for(File f : sourceDirs) {
            execute(f, outputDir);
        }
    }
    
    void execute(File sourceDir, File outputDir) throws Exception {
        File[] directoryList = sourceDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isHidden() && !pathname.getName().startsWith(".") && pathname.isDirectory();
            }
        });
        File[] fileList = sourceDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isHidden() && !pathname.getName().startsWith(".") && !pathname.isDirectory();
            }
        });
        if(fileList != null) {
            for(File f : fileList) {
                if(f.getName().endsWith(".class")) {
                    Parser.parse(f);
                } else {
                    if(!f.isDirectory()) {
                        // copy the file to the dest dir
                        copy(new FileInputStream(f), new FileOutputStream(new File(outputDir, f.getName())));
                    }
                }
            }
        }
        if(directoryList != null) {
            for(File f : directoryList) {
                if(f.getName().endsWith(".bundle") || f.getName().endsWith(".xcdatamodeld")) {
                    copyDir(f, outputDir);
                    continue;
                }
                execute(f, outputDir);
            }
        }
    }
    
    private void copyDir(File source, File destDir) throws IOException {
        File destFile = new File(destDir, source.getName());
        destFile.mkdirs();
        File[] files = source.listFiles();
        for(File f : files) {
            if(f.isDirectory()) {
                copyDir(f, destFile);
            } else {
                copy(new FileInputStream(f), new FileOutputStream(new File(destFile, f.getName())));
            }
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {        
        if(args.length == 0) {
            new File("build/kitchen").mkdirs();
            args = new String[] {"ios", "/Users/shai/dev/CodenameOne/ByteCodeTranslator/tmp;/Users/shai/dev/cn1/vm/JavaAPI/build/classes;/Users/shai/dev/cn1/Ports/iOSPort/build/classes;/Users/shai/dev/cn1/Ports/iOSPort/nativeSources;/Users/shai/dev/cn1/CodenameOne/build/classes;/Users/shai/dev/codenameone-demos/KitchenSink/build/classes", 
                "build/kitchen", "KitchenSink", "com.codename1.demos.kitchen", "Kitchen Sink", "1.0", "ios", "none"};
        }
        
        if(args.length != 9) {
            System.out.println("We accept 9 arguments output type (ios, csharp), input directory, output directory, app name, package name, app dispaly name, version, type (ios/iphone/ipad) and additional frameworks");
            System.exit(1);
            return;
        }
        final String appName = args[3];
        final String appPackageName = args[4];
        final String appDisplayName = args[5];
        final String appVersion = args[6];
        final String appType = args[7];
        final String addFrameworks = args[8];
        // we accept 3 arguments output type, input directory & output directory
        if (System.getProperty("saveUnitTests", "false").equals("true")) {
            System.out.println("Generating Unit Tests");
            ByteCodeClass.setSaveUnitTests(true);
        }
        if(args[0].equalsIgnoreCase("csharp")) {
            output = OutputType.OUTPUT_TYPE_CSHARP;
        }
        String[] sourceDirectories = args[1].split(";");
        File[] sources = new File[sourceDirectories.length];
        for(int iter = 0 ; iter < sourceDirectories.length ; iter++) {
            sources[iter] = new File(sourceDirectories[iter]);
            if(!sources[iter].exists() && sources[iter].isDirectory()) {
                System.out.println("Source directory doesn't exist: " + sources[iter].getAbsolutePath());
                System.exit(2);
                return;
            }
        }
        File dest = new File(args[2]);
        if(!dest.exists() && dest.isDirectory()) {
            System.out.println("Source directory doesn't exist: " + dest.getAbsolutePath());
            System.exit(3);
            return;
        }
        
        ByteCodeTranslator b = new ByteCodeTranslator();
        if(output == OutputType.OUTPUT_TYPE_IOS) {
            File root = new File(dest, "dist");
            root.mkdirs();
            System.out.println("Root is: " + root.getAbsolutePath());
            File srcRoot = new File(root, appName + "-src");
            srcRoot.mkdirs();
            System.out.println("srcRoot is: " + srcRoot.getAbsolutePath() );
            
            File imagesXcassets = new File(srcRoot, "Images.xcassets");
            imagesXcassets.mkdirs();
            File  launchImageLaunchimage = new File(imagesXcassets, "LaunchImage.launchimage");
            launchImageLaunchimage.mkdirs();
            copy(ByteCodeTranslator.class.getResourceAsStream("/LaunchImages.json"), new FileOutputStream(new File(launchImageLaunchimage, "Contents.json")));

            File appIconAppiconset = new File(imagesXcassets, "AppIcon.appiconset");
            appIconAppiconset.mkdirs();
            copy(ByteCodeTranslator.class.getResourceAsStream("/Icons.json"), new FileOutputStream(new File(appIconAppiconset, "Contents.json")));
            
            
            File xcproj = new File(root, appName + ".xcodeproj");
            xcproj.mkdirs();
            File projectXCworkspace = new File(xcproj, "project.xcworkspace");
            projectXCworkspace.mkdirs();
            /*File xcsharedData = new File(projectXCworkspace, "xcshareddata");
            xcsharedData.mkdirs();*/
            
            b.execute(sources, srcRoot);

            File cn1Globals = new File(srcRoot, "cn1_globals.h");
            copy(ByteCodeTranslator.class.getResourceAsStream("/cn1_globals.h"), new FileOutputStream(cn1Globals));
            File cn1GlobalsM = new File(srcRoot, "cn1_globals.m");
            copy(ByteCodeTranslator.class.getResourceAsStream("/cn1_globals.m"), new FileOutputStream(cn1GlobalsM));
            File nativeMethods = new File(srcRoot, "nativeMethods.m");
            copy(ByteCodeTranslator.class.getResourceAsStream("/nativeMethods.m"), new FileOutputStream(nativeMethods));

            Parser.writeOutput(srcRoot);
            
            File templateInfoPlist = new File(srcRoot, appName + "-Info.plist");
            copy(ByteCodeTranslator.class.getResourceAsStream("/template/template/template-Info.plist"), new FileOutputStream(templateInfoPlist));
            
            File templatePch = new File(srcRoot, appName + "-Prefix.pch");
            copy(ByteCodeTranslator.class.getResourceAsStream("/template/template/template-Prefix.pch"), new FileOutputStream(templatePch));

            File xmlvm = new File(srcRoot, "xmlvm.h");
            copy(ByteCodeTranslator.class.getResourceAsStream("/xmlvm.h"), new FileOutputStream(xmlvm));
            
            File projectWorkspaceData = new File(projectXCworkspace, "contents.xcworkspacedata");
            copy(ByteCodeTranslator.class.getResourceAsStream("/template/template.xcodeproj/project.xcworkspace/contents.xcworkspacedata"), new FileOutputStream(projectWorkspaceData));
            replaceInFile(projectWorkspaceData, "KitchenSink", appName);
            
            
            File projectPbx = new File(xcproj, "project.pbxproj");
            copy(ByteCodeTranslator.class.getResourceAsStream("/template/template.xcodeproj/project.pbxproj"), new FileOutputStream(projectPbx));            
            
            String[] sourceFiles = srcRoot.list(new FilenameFilter() {
                @Override
                public boolean accept(File pathname, String string) {
                    return string.endsWith(".bundle") || string.endsWith(".xcdatamodeld") || !pathname.isHidden() && !string.startsWith(".") && !"Images.xcassets".equals(string);
                }
            });

            StringBuilder fileOneEntry = new StringBuilder();
            StringBuilder fileTwoEntry = new StringBuilder();
            StringBuilder fileListEntry = new StringBuilder();
            StringBuilder fileThreeEntry = new StringBuilder();
            StringBuilder frameworks = new StringBuilder();
            StringBuilder frameworks2 = new StringBuilder();
            StringBuilder resources = new StringBuilder();
            
            List<String> noArcFiles = new ArrayList<String>();
            noArcFiles.add("CVZBarReaderViewController.m");
            noArcFiles.add("OpenUDID.m");
            
            List<String> includeFrameworks = new ArrayList<String>();
            includeFrameworks.add("libiconv.dylib");
            //includeFrameworks.add("AdSupport.framework");
            includeFrameworks.add("AddressBookUI.framework");
            includeFrameworks.add("SystemConfiguration.framework");
            includeFrameworks.add("MapKit.framework");
            includeFrameworks.add("AudioToolbox.framework");
            includeFrameworks.add("libxml2.dylib");
            includeFrameworks.add("QuartzCore.framework");
            includeFrameworks.add("AddressBook.framework");
            includeFrameworks.add("libsqlite3.dylib");
            includeFrameworks.add("libsqlite3.0.dylib");
            includeFrameworks.add("GameKit.framework");
            includeFrameworks.add("Security.framework");
            includeFrameworks.add("StoreKit.framework");
            includeFrameworks.add("CoreMotion.framework");
            includeFrameworks.add("CoreLocation.framework");
            includeFrameworks.add("MessageUI.framework");
            includeFrameworks.add("MediaPlayer.framework");
            includeFrameworks.add("AVFoundation.framework");
            includeFrameworks.add("CoreVideo.framework");
            includeFrameworks.add("QuickLook.framework");
            //includeFrameworks.add("iAd.framework");
            includeFrameworks.add("CoreMedia.framework");
            includeFrameworks.add("libz.dylib");
            includeFrameworks.add("MobileCoreServices.framework");
            
            if(!addFrameworks.equalsIgnoreCase("none")) {
                includeFrameworks.addAll(Arrays.asList(addFrameworks.split(";")));
            }
            
            int currentValue = 0xF63EAAA;

            ArrayList<String> arr = new ArrayList<String>();
            arr.addAll(includeFrameworks);
            arr.addAll(Arrays.asList(sourceFiles));
            
            for(String file : arr) {
                fileListEntry.append("		0");
                currentValue++;
                String fileOneValue = Integer.toHexString(currentValue).toUpperCase();
                fileListEntry.append(fileOneValue);
                fileListEntry.append("18E9ABBC002F3D1D /* ");
                fileListEntry.append(file);
                fileListEntry.append(" */ = {isa = PBXFileReference; lastKnownFileType = ");
                fileListEntry.append(getFileType(file));
                if(file.endsWith(".framework") || file.endsWith(".dylib") || file.endsWith(".a")) {
                    fileListEntry.append("; name = \"");
                    fileListEntry.append(file);
                    if(file.endsWith(".dylib")) {
                        fileListEntry.append("\"; path = \"usr/lib/");
                        fileListEntry.append(file);
                        fileListEntry.append("\"; sourceTree = SDKROOT; };\n");
                    } else {
                        if(file.endsWith(".a")) {
                            fileListEntry.append("\"; path = \"");
                            fileListEntry.append(appName);
                            fileListEntry.append("-src/");
                            fileListEntry.append(file);
                            fileListEntry.append("\"; sourceTree = \"<group>\"; };\n");
                        } else {
                            fileListEntry.append("\"; path = System/Library/Frameworks/");
                            fileListEntry.append(file);
                            fileListEntry.append("; sourceTree = SDKROOT; };\n");
                        }
                    }
                } else {
                    fileListEntry.append("; path = \"");
                    if(file.endsWith(".m") || file.endsWith(".c") || file.endsWith(".cpp") || file.endsWith(".mm") || file.endsWith(".h") || 
                            file.endsWith(".bundle") || file.endsWith(".xcdatamodeld") || file.endsWith(".hh") || file.endsWith(".hpp") || file.endsWith(".xib")) {
                        fileListEntry.append(file);
                    } else {
                        fileListEntry.append(appName);
                        fileListEntry.append("-src/");
                        fileListEntry.append(file);
                    }
                    fileListEntry.append("\"; sourceTree = \"<group>\"; };\n");
                }
                currentValue++;
                fileOneEntry.append("		0");
                String referenceValue = Integer.toHexString(currentValue).toUpperCase();
                fileOneEntry.append(referenceValue);
                fileOneEntry.append("18E9ABBC002F3D1D /* ");
                fileOneEntry.append(file);
                fileOneEntry.append(" */ = {isa = PBXBuildFile; fileRef = 0");
                fileOneEntry.append(fileOneValue);
                fileOneEntry.append("18E9ABBC002F3D1D /* ");
                fileOneEntry.append(file);
                if(noArcFiles.contains(file)) {
                    fileOneEntry.append(" */; settings = {COMPILER_FLAGS = \"-fno-objc-arc\"; }; };\n");                
                } else {
                    fileOneEntry.append(" */; };\n");                
                }
                
                if(file.endsWith(".m") || file.endsWith(".c") || file.endsWith(".cpp") || file.endsWith(".hh") || file.endsWith(".hpp") || 
                        file.endsWith(".mm") || file.endsWith(".h") || file.endsWith(".bundle") || file.endsWith(".xcdatamodeld") || file.endsWith(".xib")) {
                    
                    // bundle also needs to be a runtime resource
                    if(file.endsWith(".bundle") || file.endsWith(".xcdatamodeld")) {
                        resources.append("\n				0");
                        resources.append(referenceValue);
                        resources.append("18E9ABBC002F3D1D /* ");
                        resources.append(file);
                        resources.append(" */,");                        
                    }
                    
                    fileTwoEntry.append("				0");
                    fileTwoEntry.append(fileOneValue);
                    fileTwoEntry.append("18E9ABBC002F3D1D /* ");
                    fileTwoEntry.append(file);
                    fileTwoEntry.append(" */,\n");

                    if(!file.endsWith(".h") && !file.endsWith(".hpp") && !file.endsWith(".hh") && !file.endsWith(".bundle") ) {
                        fileThreeEntry.append("				0");
                        fileThreeEntry.append(referenceValue);
                        fileThreeEntry.append("18E9ABBC002F3D1D /* ");
                        fileThreeEntry.append(file);
                        fileThreeEntry.append(" */,\n");
                    }
                } else {
                    if(file.endsWith(".a") || file.endsWith(".framework") || file.endsWith(".dylib") || file.endsWith("Info.plist") || file.endsWith(".pch")) {
                        frameworks.append("				0");
                        frameworks.append(referenceValue);
                        frameworks.append("18E9ABBC002F3D1D /* ");
                        frameworks.append(file);
                        frameworks.append(" */,\n");

                        frameworks2.append("				0");
                        frameworks2.append(fileOneValue);
                        frameworks2.append("18E9ABBC002F3D1D /* ");
                        frameworks2.append(file);
                        frameworks2.append(" */,\n");
                        
                        if(file.endsWith(".a")) {
                            fileTwoEntry.append("				0");
                            fileTwoEntry.append(fileOneValue);
                            fileTwoEntry.append("18E9ABBC002F3D1D /* ");
                            fileTwoEntry.append(file);
                            fileTwoEntry.append(" */,\n");

                            if(!file.endsWith(".h") && !file.endsWith(".bundle") && !file.endsWith(".xcdatamodeld")) {
                                fileThreeEntry.append("				0");
                                fileThreeEntry.append(referenceValue);
                                fileThreeEntry.append("18E9ABBC002F3D1D /* ");
                                fileThreeEntry.append(file);
                                fileThreeEntry.append(" */,\n");
                            }
                        }
                    } else {
                        // standard resource file
                        resources.append("\n				0");
                        resources.append(referenceValue);
                        resources.append("18E9ABBC002F3D1D /* ");
                        resources.append(file);
                        resources.append(" */,");
                    }
                }
            }
            
            if(!appType.equalsIgnoreCase("ios")) {
                String devFamily = "TARGETED_DEVICE_FAMILY = \"2\";";
                if(appType.equalsIgnoreCase("iphone")) {
                    devFamily = "TARGETED_DEVICE_FAMILY = \"1\";";
                } 
                replaceInFile(projectPbx, "template", appName, "**ACTUAL_FILES**", fileListEntry.toString(),
                        "**FILE_LIST**", fileOneEntry.toString(), "** FILE_LIST_2 **", fileTwoEntry.toString(),
                        "**FILES_3**", fileThreeEntry.toString(), "***FRAMEWORKS***", frameworks.toString(),
                        "***FRAMEWORKS2***", frameworks2.toString(), "TARGETED_DEVICE_FAMILY = \"1,2\";", devFamily,
                        "***RESOURCES***", resources.toString());
            } else {
                replaceInFile(projectPbx, "template", appName, "**ACTUAL_FILES**", fileListEntry.toString(),
                        "**FILE_LIST**", fileOneEntry.toString(), "** FILE_LIST_2 **", fileTwoEntry.toString(),
                        "**FILES_3**", fileThreeEntry.toString(), "***FRAMEWORKS***", frameworks.toString(),
                        "***FRAMEWORKS2***", frameworks2.toString(), "***RESOURCES***", resources.toString());
            }

            String bundleVersion = System.getProperty("bundleVersionNumber", appVersion);
            replaceInFile(templateInfoPlist, "com.codename1pkg", appPackageName, "${PRODUCT_NAME}", appDisplayName, "VERSION_VALUE", appVersion, "VERSION_BUNDLE_VALUE", bundleVersion);
        } else {
            b.execute(sources, dest);
            Parser.writeOutput(dest);
        }
    }
    
    private static String getFileType(String s) {
        if(s.endsWith(".framework")) {
            return "wrapper.framework";
        }
        if(s.endsWith(".a")) {
            return "archive.ar";
        }
        if(s.endsWith(".dylib")) {
            return "compiled.mach-o.dylib";
        }
        if(s.endsWith(".h")) {
            return "sourcecode.c.h";
        }
        if(s.endsWith(".pch")) {
            return "sourcecode.c.objc.preprocessed";
        }
        if(s.endsWith(".hh") || s.endsWith(".hpp")) {
            return "sourcecode.cpp.h";
        }
        if(s.endsWith(".plist")) {
            return "text.plist.xml";
        } 
        if(s.endsWith(".bundle") || s.endsWith("xcdatamodeld")) {
            return "wrapper.plug-in";
        }
        if(s.endsWith(".m") || s.endsWith(".c")) {
            return "sourcecode.c.objc";
        }
        if(s.endsWith(".xcassets")) {
            return "folder.assetcatalog";
        }
        if(s.endsWith(".mm") || s.endsWith(".cpp")) {
            return "sourcecode.cpp.objc";
        }
        if(s.endsWith(".xib")) {
            return "file.xib";
        }
        if(s.endsWith(".res") || s.endsWith(".ttf") ) {
            return "file";
        }
        if(s.endsWith(".png")) {
            return "image.png";
        }
        if(s.endsWith(".strings")) {
            return "text.plist.strings";
        }
        return "file";
    }
    
    private static void replaceInFile(File sourceFile, String... values) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
        byte[] data = new byte[(int)sourceFile.length()];
        dis.readFully(data);
        dis.close();
        FileWriter fios = new FileWriter(sourceFile);
        String str = new String(data);
        for(int iter = 0 ; iter < values.length ; iter += 2) {
            str = str.replace(values[iter], values[iter + 1]);
        }
        fios.write(str);
        fios.close();
    }
    

    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     * 
     * @param i source
     * @param o destination
     */
    public static void copy(InputStream i, OutputStream o) throws IOException {
        copy(i, o, 8192);
    }

    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2 large enoguh
     */
    public static void copy(InputStream i, OutputStream o, int bufferSize) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int size = i.read(buffer);
            while(size > -1) {
                o.write(buffer, 0, size);
                size = i.read(buffer);
            }
        } finally {
            cleanup(o);
            cleanup(i);
        }
    }

    /**
     * Closes the object (connection, stream etc.) without throwing any exception, even if the
     * object is null
     *
     * @param o Connection, Stream or other closeable object
     */
    public static void cleanup(Object o) {
        try {
            if(o instanceof OutputStream) {
                ((OutputStream)o).close();
                return;
            }
            if(o instanceof InputStream) {
                ((InputStream)o).close();
                return;
            }
        } catch(IOException err) {
            err.printStackTrace();
        }
    }
}
