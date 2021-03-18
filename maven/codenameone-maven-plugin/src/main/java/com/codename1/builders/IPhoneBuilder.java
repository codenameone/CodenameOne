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
package com.codename1.builders;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Shai Almog
 * @author Steve Hannah
 */
public class IPhoneBuilder extends Executor {
    private boolean useMetal;
    private boolean enableGalleryMultiselect;
    private boolean enableWKWebView, disableUIWebView;
    private String pod = "/usr/local/bin/pod";
    private int podTimeout = 300000; // 5 minutes
    private int xcodeVersion;
    private String codesignAllocate;
    private static final String GOOGLE_SIGNIN_TUTORIAL_URL = "http://www.codenameone.com/...";
    private File resultDir;
    private File pushCertificate, notificationServiceProvisioningProfileTemp;
    private boolean includePush;
    private File tmpFile;
    private File ipaFile;
    private File icon57;
    private File icon512;

    private String provisioningProfileName, developmentTeam;

    private File dsym;

    private boolean runPods=false;
    private String certificateName;
    private boolean photoLibraryUsage;
    private String buildVersion;
    private String origMainClass; // generate unit tests will change the request.getMainClass() to the unit test executor
    private boolean usesLocalNotifications;
                                  // so we need to store the main class name for later here.
    // Map will be used for Xcode 8 privacy usage descriptions.  Don't need it yet
    // so leaving it commented out.
    private Map<String,String> privacyUsageDescriptions = new HashMap<String,String>();
    
    final static int majorOSVersion;
    final static int minorOSVersion;
    final static String osVersion;
    static {
        osVersion = System.getProperty("os.version");
        StringTokenizer versionTok = new StringTokenizer(osVersion, ".");
        majorOSVersion = Integer.parseInt(versionTok.nextToken());
        minorOSVersion = Integer.parseInt(versionTok.nextToken());
    }
    

    
    public void cleanup() {
        super.cleanup();

        
    }

    private static String maxVersionString(String commaDelimitedVersions) {
        String[] versions = commaDelimitedVersions.split(",");
        String currMax = "0.0";
        for (String version : versions) {
            version = version.trim();
            if (version.length() == 0) {
                continue;
            }
            if (compareVersionStrings(version, currMax) > 0) {
                currMax = version;
            }
        }
        
        return currMax;
    }
    
    private static int compareVersionStrings(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        int len = Math.max(p1.length, p2.length);
        for (int i=0; i<len; i++) {
            int iPart1 = p1.length > i ? Integer.parseInt(p1[i]) : 0;
            int iPart2 = p2.length > i ? Integer.parseInt(p2[i]) : 0;
            if (iPart1 != iPart2) {
                return iPart1 < iPart2 ? -1 : 1;
            }
        }
        return 0;
    }
    
    @Override
    protected String getDeviceIdCode() {
        return "\"\"";
    }
    
    /**
     * Static libs that don't include the LC_VERSION_MIN_XXX run instructions seem
     * to cause IPATool to crash.  This occurs for .a archives compiled with Xcode before version 7.
     * We should validate it here so that the error message is sensical. (It will fail in ipatool in the
     * export step but the error won't make any sense..
     * @param file
     * @return 
     */
    private boolean validateLC_MIN_VERSION(File file) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("otool", "-lv", file.getAbsolutePath());
        Process p = pb.start();
        InputStream is = p.getInputStream();
        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("LC_VERSION_MIN_")) {
                return true;
            }
        }
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(IPhoneBuilder.class.getName()).log(Level.SEVERE, null, ex);
            log(ex.getMessage());
            
        }
        return false;
    }
    
    private double versionToDouble(String version) {
        StringBuilder majorVersion = new StringBuilder();
        StringBuilder minorVersion = new StringBuilder();
        boolean majorComplete = false;
        for (char c : version.toCharArray()) {
            if (!Character.isDigit(c)) {
                majorComplete = true;
                continue;
            }
            if (majorComplete) {
                minorVersion.append(c);
            } else {
                majorVersion.append(c);
            }
        }
        if (majorVersion.length() == 0) {
            majorVersion.append("0");
        }
        if (minorVersion.length() == 0) {
            minorVersion.append("0");
        }
        return Double.parseDouble(majorVersion + "." + minorVersion);
    }
    
    /**
     * Strips non-null values from an array of strings.
     * @param params
     * @return 
     */
    private String[] nonNull(String... params) {
        ArrayList<String> out = new ArrayList<String>();
        for (String p : params) {
            if (p != null) {
                out.add(p);
            }
        }
        return out.toArray(new String[out.size()]);
    }
    
    /**
     * Gets the Xcode.app file corresponding to a given xcodebuildPath
     * @param xcodebuildPath
     * @return 
     */
    private File getXcodeAppDir(String xcodebuildPath) {
        File f = new File(xcodebuildPath);
        while (f != null) {
            if ("Contents".equals(f.getName())) {
                f = f.getParentFile();
                if (f != null) {
                    return f;
                }
                throw new IllegalArgumentException("Provided xcodeBuildPath "+xcodebuildPath+" not in Xcode.app bundle");
            }
            f = f.getParentFile();
        }
        throw new IllegalArgumentException("Provided xcodeBUildPath "+xcodebuildPath+" not in Xcode.app bundle");
    }
    
    private File getResDir() {
        return new File(tmpFile, "res");
    }
    
    private File getBuildinRes() {
        return new File(tmpFile, "btres");
    }
    
    private String minDeploymentTargets = "6.0";
    private void addMinDeploymentTarget(String target) {
        minDeploymentTargets += ","+target;
    }
    
    private String getDeploymentTarget(BuildRequest request){
        StringBuilder sb = new StringBuilder();
        sb.append(minDeploymentTargets);
        if (request.getArg("ios.pods.platform", null) != null) {
            sb.append(",");
            sb.append(request.getArg("ios.pods.platform", ""));
        }
        if (request.getArg("ios.deployment_target", null) != null) {
            sb.append(",");
            sb.append(request.getArg("ios.deployment_target", ""));
        }
        if (request.getArg("ios.minDeploymentTarget", null) != null) {
            sb.append(",");
            sb.append(request.getArg("ios.minDeploymentTarget", ""));
        }
        return maxVersionString(sb.toString());
        
               
    }
    
    private static String append(String str, String separator, String append) {
        if (!str.trim().endsWith(separator)) {
            str += separator;
        }
        return str + append;
    }
    
    private int getDeploymentTargetInt(BuildRequest request) {
        String target = getDeploymentTarget(request);
        if (target.indexOf(".") > 0) {
            target = target.substring(0, target.indexOf("."));
        }
        return Integer.parseInt(target);
    }
    
    @Override
    public boolean build(File sourceZip, BuildRequest request) throws BuildException {
        tmpFile = tmpDir = getBuildDirectory();
        useMetal = "true".equals(request.getArg("ios.metal", "false"));
        try {
            log("Pods version: " + execString(new File("."), pod, "--version"));
        } catch (Exception ex) {
            error("Please install Cocoapods in order to generate Xcode projects.  E.g. 'sudo gem install cocoapods'.  See https://cocoapods.org/", ex);
            throw new BuildException("Please install Cocoapods in order to generate Xcode projects.  E.g. 'sudo gem install cocoapods'.  See https://cocoapods.org/");
        }
        log("Request Args: ");
        log("-----------------");
        for (String arg : request.getArgs()) {
            log(arg+"="+request.getArg(arg, null));
        }
        log("-------------------");

        origMainClass = request.getMainClass();

        buildVersion = request.getVersion();
        if(request.getArg("ios.twoDigitVersion", "false").equals("true")) {
            try {
                float version = Float.parseFloat(buildVersion);
                int intVersion = Math.round(100 * version);
                int lsb = intVersion % 100;
                buildVersion = "" + (intVersion / 100) + ".";
                if(lsb == 0) {
                    buildVersion += "00";
                } else {
                    if(lsb < 10) {
                        buildVersion += "0" + lsb;
                    } else {
                        buildVersion += lsb;
                    }
                }
            } catch(Exception err) {
            }
        }
        
        for (String arg : request.getArgs()) {
            if (arg.startsWith("ios.NS") && arg.endsWith("UsageDescription")) {
                if (arg.toUpperCase().contains("PHOTOLIBRARY")) {
                    photoLibraryUsage = true;
                }
                privacyUsageDescriptions.put(arg.substring(arg.lastIndexOf(".")+1), request.getArg(arg, null));
            }
        }
        
        String homeDir = System.getProperty("user.home");
        String xcodebuild;
        String iosPods = request.getArg("ios.pods", "");
        enableGalleryMultiselect = "true".equals(request.getArg("ios.enableGalleryMultiselect", "false"));
        if (enableGalleryMultiselect) {
            if (!iosPods.contains("QBImagePickerController") && photoLibraryUsage) {
                if (!iosPods.endsWith(",")) {
                    iosPods += ",";
                }
                iosPods += "QBImagePickerController ~> 3.4";
                addMinDeploymentTarget("8.0");
            }
        }
        enableWKWebView = "true".equals(request.getArg("ios.useWKWebView", "true"));
        if (enableWKWebView) {
            addMinDeploymentTarget("8.0");
        }
        disableUIWebView = enableWKWebView && "true".equals(request.getArg("ios.noUIWebView", "true"));

        boolean bicodeHandle = true;
        String xcodePath = System.getenv("XCODE_PATH");
        if (xcodePath == null) {
            xcodePath = "/Applications/Xcode.app";
        }
        xcodebuild = "xcodebuild";
        String iosSDK = request.getArg("ios.sdk", "13.2");
        xcodebuild = "xcodebuild";
        xcodeVersion = getXcodeVersion(xcodebuild);
        if (xcodeVersion <= 0) {
            xcodeVersion = 10;
        }

        codesignAllocate = xcodebuild.replace(
                "/Contents/Developer/usr/bin/xcodebuild", 
                "/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/codesign_allocate"
        );
        
        
        

        String facebookAppId = request.getArg("facebook.appId", null);
        if(!new File(pod).exists()) {
            pod = "/usr/bin/pod";
            if(!new File(pod).exists()) {
                log("You need to install cocoapods to proceed, to install cocoapods on your mac issue this command in the terminal: sudo gem install cocoapods --pre\n"
                        + "followed by: sudo gem install xcodeproj");
                return false;
            }
        }
        
        boolean usePodsForFacebook = !request.getArg("ios.facebook.usePods", "true").equals("false") && facebookAppId != null && facebookAppId.length() > 0;
        if (usePodsForFacebook) {
            String fbPodsVersion = request.getArg("ios.facebook.version", "~>5.6.0");
            addMinDeploymentTarget("10.0");
            iosPods += (((iosPods.length() > 0) ? ",":"") + "FBSDKCoreKit "+fbPodsVersion+",FBSDKLoginKit "+fbPodsVersion+",FBSDKShareKit "+fbPodsVersion);
        }
        
        runPods = true;
        
        
        String googleAdUnitId = request.getArg("ios.googleAdUnitId", request.getArg("google.adUnitId", null));
        boolean usePodsForGoogleAds = runPods && googleAdUnitId != null && googleAdUnitId.length() > 0;
        if (usePodsForGoogleAds) {
            iosPods += (((iosPods.length() > 0) ? ",":"") + "Firebase/Core,Firebase/AdMob");
            addMinDeploymentTarget("7.0");
        }
        if (enableGalleryMultiselect && photoLibraryUsage) {
            addMinDeploymentTarget("8.0");
        }
        if (enableWKWebView) {
            addMinDeploymentTarget("8.0");
        }
        
        if (request.getArg("ios.sdk", null) == null && System.getProperty("ios.sdk", null) != null) {
            iosSDK = System.getProperty("ios.sdk", iosSDK);
        }
        System.out.println("Xcode version is "+xcodeVersion);
        String iosMode = request.getArg("ios.themeMode", "auto");
        
        tmpFile = getBuildDirectory();
        if (tmpFile == null) {
            throw new IllegalStateException("Build directory must be set before running build.");
        }
        if (tmpFile.exists()) {
            delTree(tmpFile);
        }
        tmpFile.mkdirs();

        File classesDir = new File(tmpFile, "classes");
        classesDir.mkdirs();
        File resDir = new File(tmpFile, "res");
        resDir.mkdirs();
        File buildinRes = new File(tmpFile, "btres");
        buildinRes.mkdirs();
        pushCertificate = new File(tmpFile, "PushCertificate.p12");

        // fill classes dir from JAR and proper ports
        try {
            unzip(sourceZip, classesDir, resDir, resDir, buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to unzip source Zip file.", ex);
        }
        
        
        
        // We allow devs to add local podspecs inside a folder called "podspecs".  This will
        // be tarred by unzip() into a file named podspecs.tar so that folder hierarchies can be preserved
        // We must now go through and extract this tar file into a separate directory so that we can copy them
        // into the project folder after ByteCodeTranslator has created the Xcode project.
        
        // Look for frameworks
        for (File child : resDir.listFiles()) {
            if (child.getName().endsWith(".framework.zip")) {
                log("Found framework "+child+". Attempting extract it and generate podspec for it");
                try {
                    if (!exec(resDir, "unzip", child.getName(), "-d", new File(tmpDir, "dist").getAbsolutePath())) {
                        log("Failed to unzip " + child.getName());
                        return false;
                    }
                } catch (Exception ex) {
                    throw new BuildException("Failed to extract bundled framework "+child, ex);
                }
                String frameworkBase = child.getName().substring(0, child.getName().lastIndexOf(".framework.zip"));
                File frameworkFile = new File(new File(tmpDir, "dist"), frameworkBase+".framework");
                if (!frameworkFile.exists()) {
                    log("Cannot find framework file "+frameworkFile+" after extracting "+child+".  Please ensure that the framework is located in the top level of the zip file.");
                    return false;
                }
                
                File podspecFile = new File(resDir, frameworkBase+".podspec");
                StringBuilder podspecContents = new StringBuilder()
                        .append("Pod::Spec.new do |s|\n" +
                        "  s.name                    = \""+frameworkBase+"\"\n" +
                        "  s.version                 = \"1.0.0\"\n" +
                        "  s.summary                 = \""+frameworkBase+" framework\"\n" +
                        "  s.description             = \"This spec specifies a vendored framework.\"\n" +
                        "  s.platform                = :ios\n" +
                        "  s.homepage                = \"https://www.codenameone.com\"\n" +
                        "  s.source                  = {:path => \".\"}\n" +
                        "  s.author                  = \"Codename One\"\n" +
                        "  s.vendored_frameworks     = \""+frameworkBase+".framework\"\n" +
                        "end");
                log("Writing podspec "+podspecFile+" with contents:\n"+podspecContents.toString());
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(podspecFile);
                    fos.write(podspecContents.toString().getBytes("UTF-8"));
                } catch (IOException ex) {
                    throw new BuildException("Failed to write th podxspec file for bundled framework "+child, ex);
                } finally {
                    if (fos != null) {
                        try {fos.close();} catch (Throwable t){}
                    }
                }
                child.delete();
                
                iosPods = append(iosPods, ",", frameworkBase);
                
                
            }
        }
        
        File podSpecs = new File(tmpFile, "podspecs");
        podSpecs.mkdirs();
        try {
            for (File dir : new File[]{classesDir, resDir, buildinRes}) {
                for (File child : dir.listFiles()) {
                    if (child.getName().endsWith(".podspec")) {
                        Files.move(child.toPath(), new File(podSpecs, child.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    if ("podspecs.tar".equals(child.getName())) {
                        if (!exec(tmpFile, "tar", "xvf", child.getAbsolutePath(), "-C", podSpecs.getAbsolutePath())) {
                            log("Failed to extract podspecs tar file " + child.getAbsolutePath() + " to podspecs dir " + podSpecs.getAbsolutePath());
                            return false;
                        }

                        child.delete();
                    }

                }
            }
        } catch (Exception ex) {
            throw new BuildException("An error occurred while attempting to install bundled podspecs", ex);
        }
        
        File googleServicePlistFile = new File(resDir, "GoogleService-Info.plist");
        String googleClientId = null;
        boolean useGoogleSignIn = false;
        if (googleServicePlistFile.exists()) {
            googleServicePlist = new GoogleServicePlist();
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            Document doc;
            try {
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                doc = dBuilder.parse(googleServicePlistFile);
            } catch (Exception ex) {
                throw new BuildException("Failed to parse google services Plist File", ex);
            }
            Element dict = (Element)doc.getElementsByTagName("dict").item(0);
            NodeList childNodes = dict.getChildNodes();
            int len = childNodes.getLength();
            for (int i=0; i<len; i++) {
                Node n = childNodes.item(i);
                if (n instanceof Element) {
                    Element e = (Element)n;
                    if ("key".equals(e.getTagName()) && "CLIENT_ID".equals(e.getTextContent().trim())) {
                        Element nextEl = getNextElement(childNodes, i);
                        
                        if (nextEl != null && "string".equals(nextEl.getTagName())) {
                            googleClientId = nextEl.getTextContent().trim();
                            googleServicePlist.clientId = googleClientId;
                        }
                    } else if ("key".equals(e.getTagName()) && "BUNDLE_ID".equals(e.getTextContent().trim())) {
                        Element nextEl = getNextElement(childNodes, i);
                        if (nextEl != null && "string".equals(nextEl.getTagName())) {
                            String bid = nextEl.getTextContent().trim();
                            if (bid == null || !bid.equals(request.getPackageName())) {
                                System.out.println("Bundle ID="+request.getPackageName()+"; GoogleService BUNDLE_ID="+bid);
                                System.out.println("GoogleService-Info.plist file bundle ID does not match the App ID.  See "+GOOGLE_SIGNIN_TUTORIAL_URL+" for instructions on setting up GoogleSignIn");
                                log("GoogleService-Info.plist file bundle ID does not match the App ID.  See "+GOOGLE_SIGNIN_TUTORIAL_URL+" for instructions on setting up GoogleSignIn");
                                
                                return false;
                            }
                        }
                    } else if ("key".equals(e.getTagName()) && "IS_SIGNIN_ENABLED".equals(e.getTextContent().trim())) {
                        Element nextEl = getNextElement(childNodes, i);
                        if ("true".equals(nextEl.getTagName())) {
                            useGoogleSignIn = true;
                            googleServicePlist.useSignIn = true;
                        }
                    } else if ("key".equals(e.getTagName()) && "REVERSED_CLIENT_ID".equals(e.getTextContent().trim())) {
                        Element nextEl = getNextElement(childNodes, i);
                        
                        if (nextEl != null && "string".equals(nextEl.getTagName())) {
                            //googleClientId = nextEl.getTextContent().trim();
                            googleServicePlist.reverseClientId = nextEl.getTextContent().trim();
                        }
                    }
                }
            }
            
            
            
        }
        
        if (googleClientId == null && useGoogleSignIn) {
            log("GoogleService-Info.plist file specifies that GoogleSignIn should be used but it doesn't provide a client ID.  Likely the GoogleService-Info.plist file is not valid.  See "+GOOGLE_SIGNIN_TUTORIAL_URL+" for instructions on setting up GoogleSignIn");
            System.out.println("Fail 2");
            return false;
        }
        if (googleClientId == null) {
            googleClientId = request.getArg("ios.gplus.clientId", null);
            if (googleClientId != null) {
                useGoogleSignIn = true;
            }
        }
        
                
        if (useGoogleSignIn) {
            iosPods += (((iosPods.length() > 0) ? ",":"") + "GoogleSignIn ~>5.0.0");
            addMinDeploymentTarget("8.0");
        }

        try {
            if (!retrolambda(new File(System.getProperty("user.dir")), request, classesDir)) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Error occurred while running retrolambda on project classes", ex);
        }


        try {
            scanClassesForPermissions(classesDir, new Executor.ClassScanner() {
                @Override
                public void usesClass(String cls) {
                    if (!usesLocalNotifications && cls.indexOf("com/codename1/notifications/LocalNotification") == 0) {
                        usesLocalNotifications = true;
                    }
                }

                @Override
                public void usesClassMethod(String cls, String method) {

                }
            });
        } catch (Exception ex) {
            throw new BuildException("Failed to scan project classes for permissions.");
        }
        
        System.out.println("Local Notifications "+(usesLocalNotifications?"enabled":"disabled"));
        log("Local Notifications "+(usesLocalNotifications?"enabled":"disabled"));
        try {
            unzip(getResourceAsStream("/iOSPort.jar"), classesDir, buildinRes, buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to extract the iOSPort jar", ex);
        }

        
        // Check to make sure that static libraries include the LC_VERSION_MIN_XXX run commands
        // so that ipatool doesn't choke. 
        // See https://stackoverflow.com/questions/47816371/getting-ios-development-build-error#
        // And http://thomask.sdf.org/blog/2015/09/15/xcode-7s-new-linker-rules.html
        boolean foundImproperStaticLibs = false;
        try {
            for (File f : buildinRes.listFiles()) {
                if (f.getName().endsWith(".a")) {
                    if (!validateLC_MIN_VERSION(f)) {
                        log("WARNING: The static library " + f.getName() + " is missing the LC_MIN_VERSION_IPHONEOS run command which is required by the Xcode build tools.  This generally means that it was compiled with an older version of Xcode which didn't include this command.  Unfortunately, Xcode 7 now requires this command to be embedded into all static libraries.  Please recompile this library with Xcode 7 or higher.  If this library has been embedded as part of a cn1lib, you will need to update the cn1lib with the newly compiled static library.  You may also want to look at changing the library to use Cocoapods instead of embedding the static lib directly.");
                        foundImproperStaticLibs = true;

                    }
                }
            }
        } catch (Exception ex) {
            throw new BuildException("Exception while trying to verify static libraries", ex);
        }
        if (foundImproperStaticLibs && "true".equals(request.getArg("ios.failOnWarning", "false"))) {
            // For now, we'll make the default behaviour such that we don't automatically fail when a static
            // lib doesn't have LC_VERSION_MIN because it is possible that compilation will still work.  E.g.
            // libzbar.a in the cn1-codescan library and little monkey QR reader doesn't include this 
            // and it doesn't seem to cause export to fail (need to test this).
            log("Cancelling build due to static library warnings.  Set ios.failOnWarning build hint to 'false' to ignore these warnings.");
            return false;
        }
        

        try {
            unzip(getResourceAsStream("/nativeios.jar"), classesDir, buildinRes, buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to extract nativeios.jar",ex);
        }

        if(request.getArg("noExtraResources", "false").equals("true")) {
            new File(buildinRes, "CN1Resource.res").delete();
            new File(buildinRes, "IPhoneTheme.res").delete();
            new File(buildinRes, "iOS7Theme.res").delete();
        } 


        if (useMetal) {
            try {
                File CN1ES2compat = new File(buildinRes, "CN1ES2compat.h");
                replaceInFile(CN1ES2compat, "//#define CN1_USE_METAL", "#define CN1_USE_METAL");
                copy(new File(buildinRes, "MainWindowMETAL.xib"), new File(buildinRes, "MainWindow.xib"));
                copy(new File(buildinRes, "CodenameOne_METALViewController.xib"), new File(buildinRes, "CodenameOne_GLViewController.xib"));
            } catch (Exception ex) {
                throw new BuildException("Failed to inject Metal controllers", ex);
            }
        } else {
            new File(buildinRes, "MainWindowMETAL.xib").delete();
            new File(buildinRes, "CodenameOne_METALViewController.xib").delete();
        }


        final String moPubAdUnitId = request.getArg("ios.mopubId", null);
        final String moPubTabletAdUnitId = request.getArg("ios.mopubTabletId", moPubAdUnitId);
        if(moPubAdUnitId != null && moPubAdUnitId.length() > 0) {
            try {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.h");
                unzip(getResourceAsStream("/MoPubSDK_ios.zip"), classesDir, buildinRes, buildinRes);
                replaceInFile(CodenameOne_GLViewController, "//#define INCLUDE_MOPUB", "#define INCLUDE_MOPUB");
                replaceInFile(CodenameOne_GLViewController, "#define MOPUB_AD_UNIT", "#define MOPUB_AD_UNIT @\"" + moPubAdUnitId + "\"");
                replaceInFile(CodenameOne_GLViewController, "#define MOPUB_AD_SIZE", "#define MOPUB_AD_SIZE " + request.getArg("ios.mopubAdSize", "MOPUB_BANNER_SIZE"));
                replaceInFile(CodenameOne_GLViewController, "#define MOPUB_TABLET_AD_UNIT", "#define MOPUB_TABLET_AD_UNIT @\"" + moPubTabletAdUnitId + "\"");
                replaceInFile(CodenameOne_GLViewController, "#define MOPUB_TABLET_AD_SIZE", "#define MOPUB_TABLET_AD_SIZE " + request.getArg("ios.mopubTabletAdSize", "MOPUB_LEADERBOARD_SIZE"));

            } catch (Exception ex) {
                throw new BuildException("Failed to inject MoPubSDK");
            }
        }
        
        String microphoneCallback = "";
        if(request.getArg("ios.headphoneCallback", "false").equals("true")) {
            try {
                File headphoneDetectorM = new File(buildinRes, "HeadphonesDetector.m");
                File headphoneDetectorH = new File(buildinRes, "HeadphonesDetector.h");
                replaceInFile(headphoneDetectorM, "//#define DETECT_HEADPHONE", "#define DETECT_HEADPHONE");
                replaceInFile(headphoneDetectorH, "//#define DETECT_HEADPHONE2", "#define DETECT_HEADPHONE2");
                microphoneCallback =
                        "    public void headphonesDisconnected() {\n"
                                + "        i.headphonesDisconnected();\n"
                                + "    }\n\n"
                                + "    public void headphonesConnected() {\n"
                                + "        i.headphonesConnected();\n"
                                + "    }\n\n";
            } catch (Exception ex) {
                throw new BuildException("Failed to add microphone callbacks", ex);
            }
        }
        
        File launchStoryboard = new File(buildinRes, "LaunchScreen-Default.storyboard");
        if (xcodeVersion < 9) {
            launchStoryboard.delete();
        }
        
        File glAppDelegate = new File(buildinRes, "CodenameOne_GLAppDelegate.m");
        String integrateFacebook = "";
        

            
        if(facebookAppId != null && facebookAppId.length() > 0) {
            try {
                if (usePodsForFacebook) {

                } else {
                    String facebookFile = "/facebook-ios-sdk-4.12.zip";

                    unzip(getResourceAsStream(facebookFile), classesDir, buildinRes, buildinRes);
                }
                integrateFacebook = "        com.codename1.social.FacebookImpl.init(com.codename1.impl.ios.IOSImplementation.nativeInstance);\n"
                        + "        Display.getInstance().setProperty(\"facebook_app_id\", \"" + facebookAppId + "\");\n";
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define INCLUDE_FACEBOOK_CONNECT", "#define INCLUDE_FACEBOOK_CONNECT");
                if (usePodsForFacebook) {
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define USE_FACEBOOK_CONNECT_PODS", "#define USE_FACEBOOK_CONNECT_PODS");
                }

                String defaultPermissions = "\"public_profile\", \"email\", \"user_friends\"";
                String permissions = request.getArg("ios.facebook_permissions", request.getArg("and.facebook_permissions", defaultPermissions));
                StringTokenizer t = new StringTokenizer(permissions, " ,\n\r\t");
                permissions = "";
                permissions += "@" + t.nextToken();
                while (t.hasMoreTokens()) {
                    permissions += ", @" + t.nextToken();
                }

                replaceInFile(new File(buildinRes, "FacebookImpl.m"), "@\"basic_info\"", permissions);
            } catch (Exception ex) {
                throw new BuildException("Failed to add facebook api", ex);
            }


        }

        
        String integrateGoogleConnect = "";
        if (useGoogleSignIn) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define INCLUDE_GOOGLE_CONNECT", "#define INCLUDE_GOOGLE_CONNECT");
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define GOOGLE_SIGNIN", "#define GOOGLE_SIGNIN");

                integrateGoogleConnect = "        com.codename1.social.GoogleImpl.init(com.codename1.impl.ios.IOSImplementation.nativeInstance);\n"
                        + "        Display.getInstance().setProperty(\"ios.gplus.clientId\", \"" + googleClientId + "\");\n";
            } catch (IOException ex) {
                throw new BuildException("Failed to inject google signin support", ex);
            }
        } 

        
        boolean enableBackgroundFetch = request.getArg("ios.background_modes", "").contains("fetch");
        if (enableBackgroundFetch) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define INCLUDE_CN1_BACKGROUND_FETCH", "#define INCLUDE_CN1_BACKGROUND_FETCH");
            } catch (IOException ex) {
                throw new BuildException("Failed to add background fetch support", ex);
            }

        }
        
        if(request.getArg("ios.usePrintf","false").equals("true")) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "#define CN1Log(str,...) NSLog(str,##__VA_ARGS__)", "#define CN1Log(str,...) printf([[NSString stringWithFormat:str,##__VA_ARGS__] UTF8String])");
            } catch (IOException ex) {
                throw new BuildException("Failed to process ios.usePrintf build hint");
            }
        }
        
        boolean disableSignalHandler = request.getArg("ios.convertSignalsToExceptions", "true").equals("false");
        if (disableSignalHandler) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLAppDelegate.m"), "installSignalHandlers();", "//installSignalHandlers();");
            } catch (IOException ex) {
                throw new BuildException("Failed to process ios.convertSignalsToExceptions build hint", ex);
            }
        }
        
        
        boolean enableBackgroundLocation = request.getArg("ios.background_modes", "").contains("location");
        if (enableBackgroundLocation) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_ENABLE_BACKGROUND_LOCATION", "#define CN1_ENABLE_BACKGROUND_LOCATION");
            } catch (IOException ex) {
                throw new BuildException("Failed to process ios.background_modes location build hint", ex);
            }
        }
        
        if (enableGalleryMultiselect) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define ENABLE_GALLERY_MULTISELECT", "#define ENABLE_GALLERY_MULTISELECT");
            } catch (IOException ex) {
                throw new BuildException("Failed to enabled gallery multiselect support", ex);
            }
        }
        if (enableWKWebView) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define ENABLE_WKWEBVIEW", "#define ENABLE_WKWEBVIEW");
            } catch (IOException ex) {
                throw new BuildException("Failure while enabing WKWebView support", ex);
            }
        }
        if (disableUIWebView) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define NO_UIWEBVIEW", "#define NO_UIWEBVIEW");
            } catch (IOException ex) {
                throw new BuildException("Failure while disabling UIWebView support", ex);
            }
        }
        
        if (xcodeVersion >= 9) {
            try {
                for (String privacyKey : privacyUsageDescriptions.keySet()) {
                    String defKey = "INCLUDE_" + privacyKey.replace("UsageDescription", "_USAGE").substring(2).toUpperCase();
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define " + defKey, "#define " + defKey);

                }
                if (request.getArg("ios.locationUsageDescription", null) != null) {
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define INCLUDE_LOCATION_USAGE", "#define INCLUDE_LOCATION_USAGE");
                }
            } catch (IOException ex) {
                throw new BuildException("Failed to add privacy usage descriptions", ex);
            }
        } else {

            photoLibraryUsage = true;
            String[] defines = {"INCLUDE_CONTACTS_USAGE", "INCLUDE_CALENDARS_USAGE", "INCLUDE_CAMERA_USAGE",
                "INCLUDE_FACEID_USAGE", "INCLUDE_LOCATION_USAGE", "INCLUDE_MICROPHONE_USAGE", "INCLUDE_MOTION_USAGE",
                "INCLUDE_PHOTOLIBRARYADD_USAGE", "INCLUDE_PHOTOLIBRARY_USAGE", "INCLUDE_REMINDERS_USAGE", 
                "INCLUDE_SIRI_USAGE", "INCLUDE_SPEECHRECOGNITION_USAGE", "INCLUDE_NFCREADER_USAGE"
            };
            try {
                for (String defKey : defines) {
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define " + defKey, "#define " + defKey);
                }
            } catch (IOException ex) {
                throw new BuildException("Failed to process usage descriptions", ex);
            }
        }
        
        if ("true".equals(request.getArg("ios.blockScreenshotsOnEnterBackground", "false"))) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_BLOCK_SCREENSHOTS_ON_ENTER_BACKGROUND", "#define CN1_BLOCK_SCREENSHOTS_ON_ENTER_BACKGROUND");
            } catch (IOException ex) {
                throw new BuildException("Failure while processing ios.blockScreenshotsOnEnterBackground build hint", ex);
            }
        }
        
        String applicationDidEnterBackground = request.getArg("ios.applicationDidEnterBackground", null);
        if(applicationDidEnterBackground != null) {
            try {
                replaceInFile(glAppDelegate, "//----application_will_resign_active", applicationDidEnterBackground);
            } catch (IOException ex) {
                throw new BuildException("Failure while processing ios.applicationDidEnterBackground build hint", ex);
            }
        }
        


        try {
            if (request.getArg("ios.lowMemCamera", "false").equals("true")) {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceInFile(CodenameOne_GLViewController, "//#define LOW_MEM_CAMERA", "#define LOW_MEM_CAMERA");
            }

            if (request.getArg("ios.enableStatusBar7", "true").equals("false")) {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceInFile(CodenameOne_GLViewController, "int statusbarHeight = 20;", "int statusbarHeight = 0;");
            }

            if (request.getArg("ios.enableAutoplayVideo", "false").equals("false")) {
                File iosNative = new File(buildinRes, "IOSNative.m");
                replaceInFile(iosNative, "#define AUTO_PLAY_VIDEO", "//#define AUTO_PLAY_VIDEO");
            }

            if (request.getArg("ios.background_modes", "").contains("fetch")) {
                replaceInFile(new File(buildinRes, "CodenameOne_GLAppDelegate.m"), "//#define INCLUDE_CN1_BACKGROUND_FETCH", "#define INCLUDE_CN1_BACKGROUND_FETCH");
            }
        } catch (IOException ex) {
            throw new BuildException("Failure while trying to inject build hints into sources.", ex);
        }
        
        String viewDidLoad = request.getArg("ios.viewDidLoad", null);
        String adPadding = request.getArg("ios.googleAdUnitIdPadding", "");
        if(googleAdUnitId != null && googleAdUnitId.length() > 0) {
            if(adPadding.length() == 0) {
                adPadding = "        Display.getInstance().setProperty(\"adPaddingBottom\", \"9\");\n"; 
            } else {
                adPadding = "        Display.getInstance().setProperty(\"adPaddingBottom\", \"" + adPadding + "\");\n"; 
            }
        }
        
        File stubSource = new File(tmpFile, "stub");
        stubSource.mkdirs();
        try {
            generateUnitTestFiles(request, stubSource);
        } catch (Exception ex) {
            throw new BuildException("Failed to generate Unit Test Files", ex);
        }

        String newStorage = "";
        if(request.getArg("ios.newStorageLocation", "true").equals("true")) {
            newStorage = "        Display.getInstance().setProperty(\"iosNewStorage\", \"true\");\n";
        }

        String didEnterBackground =  "        stopped = true;\n"
                + "        final long bgTask = com.codename1.impl.ios.IOSImplementation.beginBackgroundTask();\n"
                + "        Display.getInstance().callSerially(new Runnable() { \n"
                + "            public void run(){ \n"
                + "                i.stop();\n"
                + "                com.codename1.impl.ios.IOSImplementation.endBackgroundTask(bgTask);"
                + "            }\n"
                + "        });\n";




        try (OutputStream stubSourceStream = new FileOutputStream(new File(stubSource, request.getMainClass() + "Stub.java"))) {
            String stubSourceCode = "package " + request.getPackageName() + ";\n\n"
                    + "import com.codename1.ui.*;\n"
                    + "import com.codename1.push.PushCallback;\n\n"
                    + "import com.codename1.system.*;\n\n"
                    + "public class " + request.getMainClass() + "Stub extends com.codename1.impl.ios.Lifecycle implements Runnable {\n"
                    //+ "    public static final String BUILD_KEY = \"" + xorEncode(getBuildKey()) + "\";\n"
                    + "    public static final String PACKAGE_NAME = \"" + request.getPackageName() + "\";\n"
                    //+ "    public static final String BUILT_BY_USER = \"" + xorEncode(request.getUserName()) + "\";\n"
                    + "    public static final String APPLICATION_VERSION = \"" + buildVersion + "\";\n"
                    + "    public static final String APPLICATION_NAME = \"" + request.getDisplayName()+ "\";\n"
                    + "    private " + request.getMainClass() + " i = new "+request.getMainClass()+"();\n"
                    + "    private boolean initialized = false;\n"
                    + "    private boolean stopped = false;\n";

                stubSourceCode += decodeFunction();
                stubSourceCode += "    public void run() {\n"
                    + "        Display.getInstance().setProperty(\"package_name\", PACKAGE_NAME);\n"
                    + "        Display.getInstance().setProperty(\"AppVersion\", APPLICATION_VERSION);\n"
                    + "        Display.getInstance().setProperty(\"AppName\", APPLICATION_NAME);\n"
                    + newStorage
                    + adPadding
                    + integrateFacebook
                    + integrateGoogleConnect

                    + "        if(!initialized) {\n"
                    + "            initialized = true;\n"
                    + "            i.init(this);\n"
                    + createStartInvocation(request, "i")
                    + "        } else {\n"
                    + createStartInvocation(request, "i")
                    + "        }\n"
                    + "    }\n\n"
                    + "    public void applicationDidEnterBackground() {\n"
                    + didEnterBackground
                    + "    }\n\n"
                    + "    public void applicationWillEnterForeground() {\n"
                    + "         if(stopped) {\n"
                    + "             stopped = false;\n"
                    + "             Display.getInstance().callSerially(this);"
                    + "         }\n"
                    + "    }\n\n"
                    + "    public void applicationDidBecomeActive() {\n"
                    + "    }\n\n"
                    + microphoneCallback
                    + "    public boolean shouldApplicationHandleURL(String url, String caller) {\n"
                    + "        if(i instanceof com.codename1.system.URLCallback) {"
                    + "            return ((com.codename1.system.URLCallback)i).shouldApplicationHandleURL(url, caller);\n"
                    + "        }\n"
                    + "        return true;\n"
                    + "    }\n\n"
                    + "    public void applicationWillTerminate() {\n"
                    + "        if(!stopped) {\n"
                    + "            i.stop();\n"
                    + "            stopped = true;\n"
                    + "        }\n"
                    + "        i.destroy();\n"
                    + "    }\n\n"
                    + "    public static void main(String[] argv) {\n"
                    + "        if(!(argv != null && argv.length > 0 && argv[0].equals(\"ignoreNative\"))) {\n"
                    + registerNativeImplementationsAndCreateStubs(new URLClassLoader(new URL[]{codenameOneJar.toURI().toURL()}), stubSource, classesDir)
                    + "        }\n"
                    + "        " + request.getMainClass() + "Stub stub = new " + request.getMainClass() + "Stub();\n"
                    + "        com.codename1.impl.ios.IOSImplementation.setMainClass(stub.i);\n"
                    + "        com.codename1.impl.ios.IOSImplementation.setIosMode(\"" + iosMode + "\");\n"
                    + "        Display.init(stub);\n"

                    + "    }\n"
                    + "}\n";

            stubSourceStream.write(stubSourceCode.getBytes());
        } catch (IOException ex) {
            throw new BuildException("Failed to write stub source", ex);
        }
        
        Class[] nativeInterfaces = getNativeInterfaces();
        if(nativeInterfaces != null && nativeInterfaces.length > 0) {
            for(Class currentNative : nativeInterfaces) {
                File folder = new File(stubSource, currentNative.getPackage().getName().replace('.', File.separatorChar));
                folder.mkdirs();
                File javaFile = new File(folder, currentNative.getSimpleName() + "ImplCodenameOne.java");
                
                String javaImplSourceFile = "package " + currentNative.getPackage().getName() + ";\n\n"
                        + "import com.codename1.ui.PeerComponent;\n\n"
                        + "public class " + currentNative.getSimpleName() + "ImplCodenameOne {\n"
                        + "    private long nativePeer;\n\n"
                        + "    public " + currentNative.getSimpleName() + "ImplCodenameOne() {\n"
                        + "        nativePeer = initializeNativePeer();\n"
                        + "    }\n\n"
                        + "    public void finalize() {\n"
                        + "        releaseNativePeerInstance(nativePeer);\n"
                        + "    }\n\n"
                        + "    private static native long initializeNativePeer();\n\n"
                        + "    private static native void releaseNativePeerInstance(long peer);\n\n";
                
                String prefixForNewVM = "";
                String postfixForNewVM = "";
                String prefix2ForNewVM = "";
                String newVMEnterNativeCode = "";
                String newVMExitNativeCode = "";
                String newVMInclude = "";

                newVMInclude = "\n#include \"cn1_globals.h\"\n";
                newVMEnterNativeCode = "    POOL_BEGIN();\n    enteringNativeAllocations();\n";
                newVMExitNativeCode = "    finishedNativeAllocations();\n    POOL_END();\n";
                prefixForNewVM = "CODENAME_ONE_THREAD_STATE";
                prefix2ForNewVM = "CODENAME_ONE_THREAD_STATE, ";
                postfixForNewVM = "_R_long";

                String classNameWithUnderscores = currentNative.getName().replace('.', '_');
                String mSourceFile = "#include \"xmlvm.h\"\n"
                        + "#include \"java_lang_String.h\"\n"
                        + "#import \"CodenameOne_GLViewController.h\"\n"
                        + "#import <UIKit/UIKit.h>\n"
                        + "#import \"" + classNameWithUnderscores + "Impl.h\"\n" + newVMInclude
                        + "#include \"" + classNameWithUnderscores + "ImplCodenameOne.h\"\n\n"
                        + "JAVA_LONG " + classNameWithUnderscores + "ImplCodenameOne_initializeNativePeer__" + postfixForNewVM + "(" + prefixForNewVM + ") {\n"
                        + "    " + classNameWithUnderscores + "Impl* i = [[" + classNameWithUnderscores + "Impl alloc] init];\n"
                        + "    return i;\n"
                        + "}\n\n"
                        + "void " + classNameWithUnderscores + "ImplCodenameOne_releaseNativePeerInstance___long(" + prefix2ForNewVM + "JAVA_LONG l) {\n"
                        + "    " + classNameWithUnderscores + "Impl* i = (" + classNameWithUnderscores + "Impl*)l;\n"
                        + "    [i release];\n"
                        + "}\n\n"
                        + "extern NSData* arrayToData(JAVA_OBJECT arr);\n"
                        + "extern NSString* toNSString(" + prefix2ForNewVM + "JAVA_OBJECT str);\n"
                        + "extern JAVA_OBJECT nsDataToByteArr(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToBooleanArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToCharArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToShortArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToIntArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToLongArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToFloatArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToDoubleArray(NSData *data);\n\n"
                        + "void xmlvm_init_native_"+ classNameWithUnderscores + "ImplCodenameOne() {}\n\n";

                for(Method m : currentNative.getMethods()) {
                    String name = m.getName();
                    if(name.equals("hashCode") || name.equals("equals") || name.equals("toString")) {
                        continue;
                    }
                    
                    Class returnType = m.getReturnType();
                    
                    mSourceFile += typeToXMLVMName(returnType) + " " + currentNative.getName().replace('.', '_') + "ImplCodenameOne_" + 
                            name + "__";
                    String mFileArgs;
                    String mFileBody;

                    mFileArgs = "(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me";
                    mFileBody = "    " + classNameWithUnderscores + "Impl* ptr = (" + classNameWithUnderscores +
                        "Impl*)get_field_" + classNameWithUnderscores + "ImplCodenameOne_nativePeer(me);\n";

                    
                    if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                        mFileBody += "    " + typeToXMLVMName(returnType) + " returnValue = " + convertToJavaMethod(returnType);
                    }
                    mFileBody += "[ptr " + name;
                    
                    if(returnType.getName().equals("com.codename1.ui.PeerComponent")) {
                        javaImplSourceFile += "    public native long " + name + "(";
                    } else {
                        javaImplSourceFile += "    public native " + getSimpleNameWithJavaLang(returnType) + " " + name + "(";
                    }
                    Class[] params = m.getParameterTypes();
                    if(params != null && params.length > 0) {
                        for(int iter = 0 ; iter < params.length ; iter++) {
                            if(params[iter].getName().equals("com.codename1.ui.PeerComponent")) {
                                params[iter] = Long.TYPE;
                            }
                        }
                        javaImplSourceFile += getSimpleNameWithJavaLang(params[0]) + " param0";
                        for(int iter = 1 ; iter < params.length ; iter++) {
                            javaImplSourceFile += ", " + getSimpleNameWithJavaLang(params[iter]) + " param" + iter;
                        }
                                                
                        for(int iter = 0 ; iter < params.length ; iter++) {
                            mSourceFile += "_" + typeToXMLVMJavaName(params[iter]);
                            mFileArgs += ", " + typeToXMLVMName(params[iter]) + " param" + iter;
                            if(iter == 0) {
                                mFileBody += ":" + convertToObjectiveCMethod(params[iter]) + "param0" + convertToClosing(params[iter]); 
                            } else {
                                mFileBody += " param" + iter + ":" + convertToObjectiveCMethod(params[iter]) + "param" + iter + convertToClosing(params[iter]); 
                            }
                        }
                    }

                    if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                        if(returnType.getName().endsWith("PeerComponent")) {
                            mSourceFile += "_R_long";
                        } else {
                            mSourceFile += "_R_" + typeToXMLVMJavaName(returnType);
                        }
                    }

                    if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                        mSourceFile += mFileArgs + ") {\n" + newVMEnterNativeCode +
                                mFileBody + "]" + convertToClosing(returnType) + ";\n" + newVMExitNativeCode 
                                + "    return returnValue;\n}\n\n";                        
                    } else {
                        mSourceFile += mFileArgs + ") {\n" + newVMEnterNativeCode +
                                mFileBody + "]" + convertToClosing(returnType) + ";\n" + newVMExitNativeCode 
                                + "}\n\n";                        
                    }
                    javaImplSourceFile += ");\n";
                }
                
                javaImplSourceFile += "}\n";
                
                
                try (FileOutputStream out = new FileOutputStream(javaFile)) {
                    out.write(javaImplSourceFile.getBytes());
                    out.close();
                } catch (IOException ex) {
                    throw new BuildException("Error while generating native interface stub for "+currentNative, ex);
                }
                File mFile = new File(resDir, "native_" + currentNative.getName().replace('.', '_') + "ImplCodenameOne.m");

                try (FileOutputStream out = new FileOutputStream(mFile)) {
                    out.write(mSourceFile.getBytes());
                    out.close();
                } catch (IOException ex) {
                    throw new BuildException("Error while generating native interface stub for "+currentNative, ex);
                }
            }
        }

        try {
            if (!execWithFiles(stubSource, stubSource, ".java", "javac", "-classpath",
                    classesDir.getAbsolutePath(),
                    "-d", classesDir.getAbsolutePath())) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failure occurred while compiling native interface stubs", ex);
        }

        
        try {
            if (!generateIcons(request)) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failed to generate icons", ex);
        }

        try {
            if (!generateLaunchScreen(request)) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failed to generate launch screen");
        }

        resultDir = new File(tmpFile, "result");
        resultDir.mkdirs();




        includePush = request.getArg("ios.includePush", "false").equalsIgnoreCase("true");
        boolean includePushEnt= includePush;

        if ((request.getPushCertificate() != null || includePush) || usesLocalNotifications) {
            try {
                File appDelH = new File(buildinRes, "CodenameOne_GLAppDelegate.h");
                DataInputStream dis = new DataInputStream(new FileInputStream(appDelH));
                byte[] data = new byte[(int) appDelH.length()];
                dis.readFully(data);
                dis.close();
                FileWriter fios = new FileWriter(appDelH);
                String str = new String(data);
                str = str.replace("//#define CN1_INCLUDE_NOTIFICATIONS", "#define CN1_INCLUDE_NOTIFICATIONS");
                fios.write(str);
                fios.close();

                File iosNative = new File(buildinRes, "IOSNative.m");
                dis = new DataInputStream(new FileInputStream(iosNative));
                data = new byte[(int) iosNative.length()];
                dis.readFully(data);
                dis.close();
                fios = new FileWriter(iosNative);
                str = new String(data);
                str = str.replace("//#define CN1_INCLUDE_NOTIFICATIONS2", "#define CN1_INCLUDE_NOTIFICATIONS2");
                fios.write(str);
                fios.close();
            } catch (IOException ex) {
                log("Failed to Update Objective-C source files to activate notifications flag");
                throw new BuildException("Failed to update Objective-C source files to activate notifications flag", ex);
            }
        }

        if(!(request.getPushCertificate() != null || includePush)) {
            try {
                // special workaround for issue Apple is having with push notification missing from
                // the entitlements
                DataInputStream dis = new DataInputStream(new FileInputStream(glAppDelegate));
                byte[] data = new byte[(int) glAppDelegate.length()];
                dis.readFully(data);
                dis.close();
                FileWriter fios = new FileWriter(glAppDelegate);
                String str = new String(data);
                str = str.replace("#define INCLUDE_CN1_PUSH", "");
                fios.write(str);
                fios.close();

                File iosNative = new File(buildinRes, "IOSNative.m");
                dis = new DataInputStream(new FileInputStream(iosNative));
                data = new byte[(int) iosNative.length()];
                dis.readFully(data);
                dis.close();
                fios = new FileWriter(iosNative);
                str = new String(data);
                str = str.replace("#define INCLUDE_CN1_PUSH2", "//#define INCLUDE_CN1_PUSH2");
                fios.write(str);
                fios.close();
            } catch (IOException ex) {
                throw new BuildException("Failed to update Objective-C source files to activate push notification flag", ex);
            }

        } else {
            if(request.getArg("ios.enableBadgeClear", "true").equals("false")) {
                try {
                    replaceInFile(glAppDelegate, "[UIApplication sharedApplication].applicationIconBadgeNumber = 0;", "//[UIApplication sharedApplication].applicationIconBadgeNumber = 0;");
                    replaceInFile(glAppDelegate, "[[UIApplication sharedApplication] cancelAllLocalNotifications];", "//[[UIApplication sharedApplication] cancelAllLocalNotifications];");
                } catch (IOException ex) {
                    throw new BuildException("Failed to remove badge notifications from objective-c soruce files", ex);
                }
            }
        }



        String releaseString = "Release";
        try {
            File iosNative = new File(buildinRes, "IOSNative.m");



            String glAppDelegeateHeader = request.getArg("ios.glAppDelegateHeader", null);
            if (glAppDelegeateHeader != null && glAppDelegeateHeader.length() > 0) {
                replaceInFile(glAppDelegate, "//GL_APP_DELEGATE_INCLUDE", glAppDelegeateHeader);
            }

            String glAppDelegeateBody = request.getArg("ios.glAppDelegateBody", null);
            if (glAppDelegeateBody != null && glAppDelegeateBody.length() > 0) {
                replaceInFile(glAppDelegate, "//GL_APP_DELEGATE_BODY", glAppDelegeateBody);
            }

            String openURLInject = request.getArg("ios.openURLInject", null);
            if (openURLInject != null && openURLInject.length() > 0) {
                replaceInFile(glAppDelegate, "//openURLMarkerEntry", openURLInject);
            }


            String beforeFinishLaunching = request.getArg("ios.beforeFinishLaunching", null);
            if (beforeFinishLaunching != null) {
                replaceInFile(glAppDelegate, "//beforeDidFinishLaunchingWithOptionsMarkerEntry", beforeFinishLaunching);
            }

            String afterFinishLaunching = request.getArg("ios.afterFinishLaunching", null);
            if (afterFinishLaunching != null) {
                replaceInFile(glAppDelegate, "//afterDidFinishLaunchingWithOptionsMarkerEntry", afterFinishLaunching);
            }

            // one of: UIActionSheetStyleAutomatic, UIActionSheetStyleDefault, UIActionSheetStyleBlackTranslucent (default), UIActionSheetStyleBlackOpaque
            String actionSheetStyle = request.getArg("ios.actionSheetStyle", null);
            if (actionSheetStyle != null) {
                replaceInFile(iosNative, "[actionSheet setActionSheetStyle:UIActionSheetStyleBlackTranslucent", "[actionSheet setActionSheetStyle:" + actionSheetStyle);

            }

            String zbarFlash = request.getArg("ios.zbar_flash", "true");
            if (zbarFlash.equals("false")) {
                // remove the flash behavior from zbar
                replaceAllInFile(iosNative, "//ZBAR_CONFIGURATIONS", "reader.readerView.torchMode = AVCaptureTorchModeOff;");
            }

            if (request.getArg("ios.keyboardOpen", "true").equals("true")) {
                File CodenameOne_GLViewController_m = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceInFile(CodenameOne_GLViewController_m, "BOOL vkbAlwaysOpen = NO;", "BOOL vkbAlwaysOpen = YES;");
            }
            if (request.getArg("ios.associatedDomains", null) != null) {
                // If the user has provided the ios.associatedDomains build hint, then we will need to
                // enable handling for these events.
                // We keep it off by default in case it interferes.
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceInFile(CodenameOne_GLViewController_h, "//#define CN1_HANDLE_UNIVERSAL_LINKS", "#define CN1_HANDLE_UNIVERSAL_LINKS");
            }


            if (request.getArg("ios.locationUsageDescription", null) != null) {
                // Remove location warning message for iOS8...  This is sort of developer documentation
                // so that they know what to do when location fails silently on iOS 8
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceInFile(CodenameOne_GLViewController_h, "#define IOS8_LOCATION_WARNING", "//#define IOS8_LOCATION_WARNING");
            }

            if (request.getArg("ios.background_modes", "").contains("location")) {
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceInFile(CodenameOne_GLViewController_h, "#define CN1_REQUEST_LOCATION_AUTH requestWhenInUseAuthorization", "#define CN1_REQUEST_LOCATION_AUTH requestAlwaysAuthorization");

            }
        } catch (Exception ex) {
            throw new BuildException("Failure while injecting code from build hints", ex);
        }
        if(googleAdUnitId != null && googleAdUnitId.length() > 0) {

            try {
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceAllInFile(CodenameOne_GLViewController_h, "//ADD_VARIABLES", "@public\n    GADBannerView *googleBannerView;\n");
                if (usePodsForGoogleAds) {
                    replaceAllInFile(CodenameOne_GLViewController_h, "//ADD_INCLUDE", "#import <GoogleMobileAds/GoogleMobileAds.h>\n");
                } else {
                    replaceAllInFile(CodenameOne_GLViewController_h, "//ADD_INCLUDE", "#import \"GADBannerView.h\"\n");
                }

                File CodenameOne_GLViewController_m = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceAllInFile(CodenameOne_GLViewController_m, "//replaceViewDidAppear", "[self addGoogleAds];\n");

                replaceAllInFile(CodenameOne_GLViewController_m, "//WILL_ROTATE_TO_INTERFACE_MARKER", "if(googleBannerView != nil) {\n" +
                        "        [googleBannerView removeFromSuperview];\n" +
                        "        [googleBannerView release];\n" +
                        "        googleBannerView = nil;\n" +
                        "    }\n");
                replaceAllInFile(CodenameOne_GLViewController_m, "//DID_ROTATE_FROM_INTERFACE_MARKER", "[self addGoogleAds];\n");
                replaceAllInFile(CodenameOne_GLViewController_m, "//INJECT_METHODS_MARKER", "-(void) addGoogleAds {\n" +
                        "    if(googleBannerView != nil) {\n" +
                        "        [googleBannerView removeFromSuperview];\n" +
                        "        [googleBannerView release];\n" +
                        "        googleBannerView = nil;\n" +
                        "    }\n" +
                        "    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];\n" +
                        "    bool isPortrait = (orientation == UIInterfaceOrientationPortrait || orientation == UIInterfaceOrientationPortraitUpsideDown);\n" +
                        "    GADAdSize adSize = kGADAdSizeSmartBannerPortrait;\n" +
                        "    if(!isPortrait) {\n" +
                        "        adSize = kGADAdSizeSmartBannerLandscape;\n" +
                        "    }\n" +
                        "    googleBannerView = [[GADBannerView alloc] initWithAdSize:adSize];\n" +
                        "    googleBannerView.adUnitID = @\"" + googleAdUnitId + "\";\n" +
                        "    googleBannerView.rootViewController = self;\n" +
                        "    [self.view addSubview:googleBannerView];\n" +
                        "    GADRequest *request = [GADRequest request];\n" +
                        "    request.testDevices = [NSArray arrayWithObjects:@\"" +
                        request.getArg("ios.googleAdUnitTestDevice", "97cfc76e5efbc6dfa7eb2e6857b613a0") + "\", nil];\n" +
                        "    [googleBannerView loadRequest:request];\n" +
                        "    CGRect r =CGRectMake([CodenameOne_GLViewController instance].view.bounds.size.width / 2 - googleBannerView.bounds.size.width / 2,\n" +
                        "                         [CodenameOne_GLViewController instance].view.bounds.size.height - googleBannerView.bounds.size.height,\n" +
                        "                         CGSizeFromGADAdSize(adSize).width, CGSizeFromGADAdSize(adSize).height);\n" +
                        "    [googleBannerView setFrame:r];\n" +
                        "}");
            } catch (Exception ex) {
                throw new BuildException("Failed to inject google ads", ex);
            }
        }
        try {
            if (viewDidLoad != null) {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceAllInFile(CodenameOne_GLViewController, "//replaceViewDidLoad", viewDidLoad);
            }

            if (request.getArg("ios.viewDidLoadInclude", null) != null) {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceAllInFile(CodenameOne_GLViewController, "#import \"CodenameOne_GLViewController.h\"", "#import \"CodenameOne_GLViewController.h\"\n" + request.getArg("ios.viewDidLoadInclude", ""));
            }
        } catch (Exception ex) {
            throw new BuildException("Failed to inject indo vidwDidLoad", ex);
        }
        
         {
            
            //if(isNewVM) {
            String addLibs = request.getArg("ios.add_libs", null);
            if(addLibs != null) {
                addLibs = addLibs.replace(',', ';').replace(':', ';');
                if (addLibs.startsWith(";")) {
                    addLibs = addLibs.substring(1);
                }
            }

            try {
                if (!runPods && googleAdUnitId != null && googleAdUnitId.length() > 0) {
                    unzip(getResourceAsStream("/google-play-services_lib-ios.zip"), classesDir, buildinRes, buildinRes);
                    if (addLibs == null || addLibs.length() == 0) {
                        addLibs = "AdSupport.framework;SystemConfiguration.framework;StoreKit.framework;CoreTelephony.framework";
                    } else {
                        addLibs = addLibs + ";AdSupport.framework;SystemConfiguration.framework;StoreKit.framework;CoreTelephony.framework";
                    }
                }

                if ((includePush || usesLocalNotifications) && xcodeVersion >= 9) {
                    if (addLibs == null) {
                        addLibs = "UserNotifications.framework";
                    } else {
                        addLibs += ";UserNotifications.framework";
                    }
                }

                if (request.getArg("ios.useJavascriptCore", "false").equalsIgnoreCase("true")) {
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_USE_JAVASCRIPTCORE", "#define CN1_USE_JAVASCRIPTCORE");
                    if (addLibs == null) {
                        addLibs = "JavascriptCore.framework";
                    } else {
                        addLibs += ";JavascriptCore.framework";
                    }
                }
                if (request.getArg("ios.useAVKit", "true").equalsIgnoreCase("true")) {

                    replaceInFile(new File(buildinRes, "IOSNative.m"), "//#define CN1_USE_AVKIT", "#define CN1_USE_AVKIT");
                }

                if (enableWKWebView) {
                    if (addLibs == null) {
                        addLibs = "WebKit.framework";
                    } else {
                        addLibs += ";WebKit.framework";
                    }
                }

                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = "CoreImage.framework;QuartzCore.framework";
                } else {
                    if (addLibs.indexOf("CoreImage.framework") < 0) {
                        addLibs += ";CoreImage.framework";
                    }
                    if (addLibs.indexOf("QuartzCore.framework") < 0) {
                        addLibs += ";QuartzCore.framework";
                    }
                }
            } catch (Exception ex) {
                throw new BuildException("Failed to process build hints", ex);
            }

            File userDir = new File(System.getProperty("user.dir"));
             String parparVMCompilerJar = null;
            try {
                File parparVMCompilerJarFile = getResourceAsFile("/parparvm-compiler.jar", ".jar");
                parparVMCompilerJar = parparVMCompilerJarFile.getAbsolutePath();
            } catch (IOException ex) {
                throw new BuildException("Failed to extract parparvm-compiler.jar", ex);
            }




            try {
                unzip(getResourceAsStream("/parparvm-java-api.jar"), classesDir, classesDir, classesDir);
            } catch (IOException ex) {
                throw new BuildException("Failed to load JavaAPI.jar");
            }

            String optimizerOn = request.getArg("ios.optimizer", "on");
            HashMap<String, String> env = new HashMap<String, String>();
            env.put("optimizer", optimizerOn);
            if(request.getArg("ios.superfastBuild", "false").equals("true")) {
                env.put("concatenateFiles", "true");
            }

            String fieldNullChecks = Boolean.valueOf(request.getArg("ios.fieldNullChecks", "false")) ? "true":"false";

            // includeNullChecks enables null checks on everything else (methods, arrays, etc..)
            String includeNullChecks = Boolean.valueOf(request.getArg("ios.includeNullChecks", "true")) ? "true":"false";
            String bundleVersionNumber = request.getArg("ios.bundleVersion", buildVersion);


            if (enableGalleryMultiselect && photoLibraryUsage) {
                addMinDeploymentTarget("8.0");
            }
            if (enableWKWebView) {
                addMinDeploymentTarget("8.0");
            }

            debug("iosDeploymentTargetMajorVersionInt="+getDeploymentTargetInt(request));

            debug("Building using addLibs="+addLibs);
            try {
                if (!exec(userDir, env, 420000, "java", "-DsaveUnitTests=" + isUnitTestMode(), "-DfieldNullChecks=" + fieldNullChecks, "-DINCLUDE_NPE_CHECKS=" + includeNullChecks, "-DbundleVersionNumber=" + bundleVersionNumber, "-Xmx384m",
                        "-jar", parparVMCompilerJar, "ios",
                        classesDir.getAbsolutePath() + ";" + resDir.getAbsolutePath() + ";" +
                                buildinRes.getAbsolutePath(),
                        tmpFile.getAbsolutePath(),
                        request.getMainClass(),
                        request.getPackageName(),
                        request.getDisplayName(),
                        buildVersion,
                        request.getArg("ios.project_type", "ios"), // one of: ios, iphone, ipad
                        addLibs)) {
                    return false;
                }
            } catch (Exception ex) {
                throw new BuildException("Failure while trying to run ByteCodeTranslator of ParparVM", ex);
            }
            try {
                String orientations = request.getArg("ios.interface_orientation", null);
                if (orientations != null && orientations.split(":").length < 4) {
                    orientations = orientations.toLowerCase();
                    File infoPlist = new File(tmpFile, "dist/" + request.getMainClass() + "-src/" + request.getMainClass() + "-Info.plist");
                    if (!orientations.contains("uiinterfaceorientationportrait")) {
                        replaceInFile(infoPlist, "<string>UIInterfaceOrientationPortrait</string>", "");
                    }
                    if (!orientations.contains("uiinterfaceorientationportraitupsidedown")) {
                        replaceInFile(infoPlist, "<string>UIInterfaceOrientationPortraitUpsideDown</string>", "");
                    }
                    if (!orientations.contains("uiinterfaceorientationlandscapeleft")) {
                        replaceInFile(infoPlist, "<string>UIInterfaceOrientationLandscapeLeft</string>", "");
                    }
                    if (!orientations.contains("uiinterfaceorientationlandscaperight")) {
                        replaceInFile(infoPlist, "<string>UIInterfaceOrientationLandscapeRight</string>", "");
                    }
                }

                if ("true".equals(request.getArg("ios.prerendered_icon", "false"))) {
                    log("Replacing prerendered Icon");
                    File infoPlist = new File(tmpFile, "dist/" + request.getMainClass() + "-src/" + request.getMainClass() + "-Info.plist");
                    replaceAllInFile(infoPlist, "<key>UIPrerenderedIcon</key>[^<]*<false/>", "<key>UIPrerenderedIcon</key><true/>");
                }


                if(runPods || !request.getArg("ios.buildType", "debug").equals("debug") || request.getArg("ios.force64", "false").equals("true")) {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");

                    //Note:  Changed this replace to work with cocoapods but it is possible, and even likely,
                    // that the change will work for all builds.  I made it "only" for the cocoapods version
                    // to prevent inadvertent breaking of versioned builds etc...
                    if (runPods) {
                        replaceAllInFile(pbx, "ARCHS = [^;]+;", "ARCHS = \"\\$(ARCHS_STANDARD_INCLUDING_64_BIT)\";");
                    } else {
                        replaceInFile(pbx, "ARCHS = armv7;", "ARCHS = \"armv7 arm64\";");
                    }
                }


                if(bicodeHandle) {
                    String minTargetVersion = request.getArg("ios.minDeploymentTarget", "6.0");
                    if(minTargetVersion.equals("6.0")) {
                        if (xcodeVersion >= 9) {
                            minTargetVersion = "7.0";
                        }
                        if (enableGalleryMultiselect && photoLibraryUsage) {
                            minTargetVersion = "8.0";
                        }
                        if (enableWKWebView) {
                            minTargetVersion = "8.0";
                        }
                    }
                    addMinDeploymentTarget(minTargetVersion);
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    replaceInFile(pbx, "IPHONEOS_DEPLOYMENT_TARGET = 5.1.1;", "IPHONEOS_DEPLOYMENT_TARGET = "+getDeploymentTarget(request)+";");
                    // this is based on the response here: http://stackoverflow.com/questions/32504355/error-itms-90339-this-bundle-is-invalid-the-info-plist-contains-an-invalid-ke
                    if(request.getArg("ios.bitcode", "false").equals("true")) {
                        replaceInFile(pbx, "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;",
                                "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;\n");
                    } else {
                        replaceInFile(pbx, "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;",
                                "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;\n"
                            + "				ENABLE_BITCODE = NO;\n");
                    }
                }

                if (useMetal) {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    replaceInFile(pbx, "CLANG_ENABLE_MODULES = NO;", "CLANG_ENABLE_MODULES = YES;");
                }
            } catch (Exception ex) {
                throw new BuildException("Failed to update infoplist file", ex);
            }
            if (runPods) {
                try {
                    List<File> podSpecFileList = new ArrayList<File>();
                    for (File podSpec : podSpecs.listFiles()) {
                        if (podSpec.getName().startsWith(".")) {
                            continue;
                        }
                        File distDir = new File(tmpFile, "dist");
                        File targetF = new File(distDir, podSpec.getName());
                        Files.move(podSpec.toPath(), targetF.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        podSpecFileList.add(targetF);

                    }

                    // Generate the NotificationServiceExtension
                    String notificationServiceExtensionStr = "";
                    if (xcodeVersion >= 9 &&
                            "true".equals(request.getArg("ios.useNotificationServiceExtension", "false"))) {

                        String notificationServiceExtensionName = request.getMainClass() + "NotificationServiceExtension";
                        // Make the notificationServiceExtensionFiles
                        File nseFolder = new File(tmpFile, "dist/"+notificationServiceExtensionName);
                        nseFolder.mkdir();
                        File nseInfoPlist = new File(nseFolder, "Info.plist");
                        PrintWriter writer = new PrintWriter(nseInfoPlist, "UTF-8");
                        writer.println(
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                                        "<plist version=\"1.0\">\n" +
                                        "<dict>\n" +
                                        "	<key>CFBundleDevelopmentRegion</key>\n" +
                                        "	<string>$(DEVELOPMENT_LANGUAGE)</string>\n" +
                                        "	<key>CFBundleDisplayName</key>\n" +
                                        "	<string>"+notificationServiceExtensionName+"</string>\n" +
                                        "	<key>CFBundleExecutable</key>\n" +
                                        "	<string>$(EXECUTABLE_NAME)</string>\n" +
                                        "	<key>CFBundleIdentifier</key>\n" +
                                        "	<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>\n" +
                                        "	<key>CFBundleInfoDictionaryVersion</key>\n" +
                                        "	<string>6.0</string>\n" +
                                        "	<key>CFBundleName</key>\n" +
                                        "	<string>$(PRODUCT_NAME)</string>\n" +
                                        "	<key>CFBundlePackageType</key>\n" +
                                        "	<string>XPC!</string>\n" +
                                        "	<key>CFBundleShortVersionString</key>\n" +
                                        "	<string>1.0</string>\n" +
                                        "	<key>CFBundleVersion</key>\n" +
                                        "	<string>1</string>\n" +
                                        "	<key>NSExtension</key>\n" +
                                        "	<dict>\n" +
                                        "		<key>NSExtensionPointIdentifier</key>\n" +
                                        "		<string>com.apple.usernotifications.service</string>\n" +
                                        "		<key>NSExtensionPrincipalClass</key>\n" +
                                        "		<string>NotificationService</string>\n" +
                                        "	</dict>\n" +
                                        "</dict>\n" +
                                        "</plist>");
                        writer.close();

                        File notificationServiceH = new File(nseFolder, "NotificationService.h");
                        writer = new PrintWriter(notificationServiceH, "UTF-8");
                        writer.println(
                                "#import <UserNotifications/UserNotifications.h>\n" +
                                        "\n" +
                                        "@interface NotificationService : UNNotificationServiceExtension\n" +
                                        "\n" +
                                        "@end"
                        );

                        writer.close();

                        File notificationServiceM = new File(nseFolder, "NotificationService.m");
                        writer = new PrintWriter(notificationServiceM, "UTF-8");
                        writer.println(
                                "//\n" +
                                        "//  NotificationService.m\n" +
                                        "//  NotificationExt2\n" +
                                        "//\n" +
                                        "//  Created by Steve Hannah on 2018-06-19.\n" +
                                        "//  Copyright (c) 2018 CodenameOne. All rights reserved.\n" +
                                        "//\n" +
                                        "\n" +
                                        "#import \"NotificationService.h\"\n" +
                                        "\n" +
                                        "@interface NotificationService ()\n" +
                                        "\n" +
                                        "@property (nonatomic, strong) void (^contentHandler)(UNNotificationContent *contentToDeliver);\n" +
                                        "@property (nonatomic, strong) UNMutableNotificationContent *bestAttemptContent;\n" +
                                        "@property (nonatomic, strong) NSURLSession *session;\n" +
                                        "\n" +
                                        "@end\n" +
                                        "\n" +
                                        "@implementation NotificationService\n" +
                                        "\n" +
                                        "- (void)didReceiveNotificationRequest:(UNNotificationRequest *)request withContentHandler:(void (^)(UNNotificationContent * _Nonnull))contentHandler {\n" +
                                        "    self.contentHandler = contentHandler;\n" +
                                        "    self.bestAttemptContent = [request.content mutableCopy];\n" +
                                        "    \n" +
                                        "    NSDictionary *userInfo = request.content.userInfo;\n" +
                                        "    if (userInfo == nil)\n" +
                                        "    {\n" +
                                        "        [self contentComplete];\n" +
                                        "        return;\n" +
                                        "    }\n" +
                                        "    NSString *mediaUrl = [userInfo objectForKey:@\"media-url\"];\n" +
                                        "    \n" +
                                        "    if (mediaUrl == nil) {\n" +
                                        "        [self contentComplete];\n" +
                                        "        return;\n" +
                                        "    }\n" +
                                        "    [self loadAttachmentForUrlString:mediaUrl\n" +
                                        "                   completionHandler:^(UNNotificationAttachment *attachment) {\n" +
                                        "                       if (attachment) {\n" +
                                        "                           self.bestAttemptContent.attachments = [NSArray arrayWithObject:attachment];\n" +
                                        "                       }\n" +
                                        "                       [self contentComplete];\n" +
                                        "                   }];\n" +
                                        "\n" +
                                        "}\n" +
                                        "\n" +
                                        "- (void)contentComplete\n" +
                                        "{\n" +
                                        "    [self.session invalidateAndCancel];\n" +
                                        "    self.contentHandler(self.bestAttemptContent);\n" +
                                        "}\n" +
                                        "\n" +
                                        "- (void)loadAttachmentForUrlString:(NSString *)urlString\n" +
                                        "                 completionHandler:(void (^)(UNNotificationAttachment *))completionHandler\n" +
                                        "{\n" +
                                        "    __block UNNotificationAttachment *attachment = nil;\n" +
                                        "    __block NSURL *attachmentURL = [NSURL URLWithString:urlString];\n" +
                                        "    \n" +
                                        "    NSString *fileExt = [@\".\" stringByAppendingString:[urlString pathExtension]];\n" +
                                        "    \n" +
                                        "    \n" +
                                        "    NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];\n" +
                                        "    \n" +
                                        "    NSURLSessionDownloadTask *task = [session downloadTaskWithURL:attachmentURL\n" +
                                        "                                                completionHandler: ^(NSURL *temporaryFileLocation, NSURLResponse *response, NSError *error) {\n" +
                                        "                                                    if (error != nil)\n" +
                                        "                                                    {\n" +
                                        "                                                        NSLog(@\"%@\", error.localizedDescription);\n" +
                                        "                                                    }\n" +
                                        "                                                    else\n" +
                                        "                                                    {\n" +
                                        "                                                        NSFileManager *fileManager = [NSFileManager defaultManager];\n" +
                                        "                                                        NSURL *localURL = [NSURL fileURLWithPath:[temporaryFileLocation.path\n" +
                                        "                                                                                                  stringByAppendingString:fileExt]];\n" +
                                        "                                                        [fileManager moveItemAtURL:temporaryFileLocation\n" +
                                        "                                                                             toURL:localURL\n" +
                                        "                                                                             error:&error];\n" +
                                        "                                                        \n" +
                                        "                                                        NSError *attachmentError = nil;\n" +
                                        "                                                        attachment = [UNNotificationAttachment attachmentWithIdentifier:[attachmentURL lastPathComponent]\n" +
                                        "                                                                                                                    URL:localURL\n" +
                                        "                                                                                                                options:nil\n" +
                                        "                                                                                                                  error:&attachmentError];\n" +
                                        "                                                        if (attachmentError)\n" +
                                        "                                                        {\n" +
                                        "                                                            NSLog(@\"%@\", attachmentError.localizedDescription);\n" +
                                        "                                                        }\n" +
                                        "                                                    }\n" +
                                        "                                                    completionHandler(attachment);\n" +
                                        "                                                }];\n" +
                                        "    \n" +
                                        "    [task resume];\n" +
                                        "}\n" +
                                        "\n" +
                                        "- (void)serviceExtensionTimeWillExpire {\n" +
                                        "    // Called just before the extension will be terminated by the system.\n" +
                                        "    // Use this as an opportunity to deliver your \"best attempt\" at modified content, otherwise the original push payload will be used.\n" +
                                        "    [self contentComplete];\n" +
                                        "}\n" +
                                        "\n" +
                                        "@end"
                        );
                        writer.close();


                        String buildSettingsStr = "CLANG_ANALYZER_NONNULL = YES;\n" +
                                "				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;\n" +
                                "				CLANG_CXX_LANGUAGE_STANDARD = \"gnu++14\";\n" +
                                "				CLANG_ENABLE_MODULES = YES;\n" +
                                "				CLANG_ENABLE_OBJC_ARC = YES;\n" +
                                "				CLANG_ENABLE_OBJC_WEAK = YES;\n" +
                                "				CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;\n" +
                                "				CLANG_WARN_COMMA = YES;\n" +
                                "				CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;\n" +
                                "				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;\n" +
                                "				CLANG_WARN_EMPTY_BODY = YES;\n" +
                                "				CLANG_WARN_ENUM_CONVERSION = YES;\n" +
                                "				CLANG_WARN_INFINITE_RECURSION = YES;\n" +
                                "				CLANG_WARN_INT_CONVERSION = YES;\n" +
                                "				CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;\n" +
                                "				CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;\n" +
                                "				CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;\n" +
                                "				CLANG_WARN_RANGE_LOOP_ANALYSIS = YES;\n" +
                                "				CLANG_WARN_STRICT_PROTOTYPES = YES;\n" +
                                "				CLANG_WARN_SUSPICIOUS_MOVE = YES;\n" +
                                "				CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;\n" +
                                "				CLANG_WARN_UNREACHABLE_CODE = YES;\n" +
                                "				CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;";

                        Map<String,String> buildSettingsMap = new HashMap<String,String>();
                        String[] lines = buildSettingsStr.split("\n");
                        for (String line : lines) {
                            if (line.trim().isEmpty()) {
                                continue;
                            }
                            String key = line.substring(0, line.indexOf("=")).trim();
                            String val = line.substring(line.indexOf("=")+1).trim();
                            if (val.endsWith(";")) {
                                val = val.substring(val.length()-1);
                            }
                            buildSettingsMap.put(key, val);

                        }
                        buildSettingsMap.put("PRODUCT_BUNDLE_IDENTIFIER", request.getPackageName()+".NotificationServiceExtension");
                        buildSettingsMap.put("PRODUCT_NAME", "$(TARGET_NAME)");
                        buildSettingsMap.put("PROVISIONING_PROFILE", "$(NS_PROVISIONING_PROFILE)");
                        buildSettingsMap.put("CODE_SIGN_ENTITLEMENTS", "$(NS_CODE_SIGN_ENTITLEMENTS)");
                        buildSettingsMap.put("LD_RUNPATH_SEARCH_PATHS", "$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks");
                        buildSettingsMap.put("INFOPLIST_FILE", notificationServiceExtensionName+"/Info.plist");

                        // We are using the notification service extension so that we can support rich push notifications

                        notificationServiceExtensionStr = "\nservice_target = xcproj.new_target(:app_extension, '"+notificationServiceExtensionName+"', :ios, '10.0')\n"
                                + "xcproj.targets.find{|e|e.name=='"+request.getMainClass()+"'}.build_configurations.each{|e| \n"
                                + "  e.build_settings['PROVISIONING_PROFILE']='$(APP_PROVISIONING_PROFILE)'\n"
                                + "  e.build_settings['CODE_SIGN_ENTITLEMENTS']='$(APP_CODE_SIGN_ENTITLEMENTS)'\n"
                                + "}\n"
                                + "service_target.frameworks_build_phase.add_file_reference(xcproj.files.find{|e|e.path.include? 'UserNotifications.framework'})\n"
                                + "service_group = xcproj.new_group('"+notificationServiceExtensionName+"')\n"
                                + "infoPlist = '"+nseInfoPlist.getAbsolutePath()+"'\n"
                                + "notificationServiceH = '"+notificationServiceH.getAbsolutePath()+"'\n"
                                + "notificationServiceM = '"+notificationServiceM.getAbsolutePath()+"'\n"
                                + "service_group.new_file(infoPlist)\n"
                                + "service_group.new_file(notificationServiceH)\n"
                                + "fileref = service_group.new_file(notificationServiceM)\n"
                                + "service_target.add_file_references([fileref])\n"
                                + "xcproj.targets.find{|e|e.name==main_class_name}.add_dependency(service_target)\n"
                                + "fileref = xcproj.groups.find{|e| e.display_name=='Products'}.new_file('"+notificationServiceExtensionName+".appex', \"BUILT_PRODUCTS_DIR\")\n"
                                + "embed_phase=xcproj.targets.find{|e| e.name=='"+request.getMainClass()+"'}.new_copy_files_build_phase('Embed App Extensions')\n"
                                + "embed_phase.build_action_mask = \"2147483647\"\n"
                                + "embed_phase.dst_subfolder_spec = \"13\"\n"
                                + "embed_phase.run_only_for_deployment_postprocessing=\"0\"\n"
                                + "embed_phase.add_file_reference(fileref)\n"
                                + "service_target.build_configurations.each{|e| \n";
                        for (String buildSettingKey : buildSettingsMap.keySet()) {
                            notificationServiceExtensionStr += "  e.build_settings['"+buildSettingKey+"'] = \"" + buildSettingsMap.get(buildSettingKey)+"\"\n";
                        }

                        notificationServiceExtensionStr +=
                                "}\n"
                                        + "xcproj.save(project_file)\n";


                        log("Adding NotificationServiceExtension: "+notificationServiceExtensionStr);

                    } else {

                        log("Not adding NotificationServiceExtension");
                    }


                    String deploymentTargetStr = "";


                    String targetStr = request.getArg("ios.deployment_target", xcodeVersion >= 9 ? "7.0" : "6.0");

                    if (enableGalleryMultiselect && photoLibraryUsage && getMajorVersionInt(targetStr, 6) < 8) {
                        targetStr = "8.0";
                    }
                    if (enableWKWebView && getMajorVersionInt(targetStr, 6) < 8) {
                        targetStr = "8.0";
                    }
                    addMinDeploymentTarget(targetStr);
                    deploymentTargetStr = "begin\n"
                            + "  xcproj.targets.each do |target|\n"
                            + "    target.build_configurations.each do |config|\n"
                            + "      config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '" + getDeploymentTarget(request) + "'\n"
                            + "    end\n"
                            + "  end\n"
                            + "  xcproj.save\n"
                            + "rescue => e\n"
                            + "  puts \"Error during updating deployment target: #{$!}\"\n"
                            + "  puts \"Backtrace:\\n\\t#{e.backtrace.join(\"\\n\\t\")}\"\n"
                            + "  puts 'An error occurred updating deployment target, but the build still might work...'\n"
                            + "end\n";


                    String createSchemesScript = "#!/usr/bin/env ruby\n" +
                            "require 'xcodeproj'\n" +
                            "main_class_name = \"" + request.getMainClass() + "\"\n" +
                            "project_file = \"" +
                                tmpDir.getAbsolutePath() + "/dist/" +
                                request.getMainClass() + ".xcodeproj\"\n" +
                            "xcproj = Xcodeproj::Project.open(project_file)\n" +
                            "begin\n"
                            + "  xcproj.recreate_user_schemes\n"
                            + "rescue => e\n"
                            + "  puts \"Error during processing: #{$!}\"\n"
                            + "  puts \"Backtrace:\\n\\t#{e.backtrace.join(\"\\n\\t\")}\"\n"
                            + "  puts 'An error occurred recreating schemes, but the build still might work...'\n"
                            + "end\n"
                            + deploymentTargetStr
                            + notificationServiceExtensionStr;
                    File hooksDir = new File(tmpFile, "hooks");
                    hooksDir.mkdir();
                    File fixSchemesFile = new File(hooksDir, "fix_xcode_schemes.rb");
                    this.createFile(fixSchemesFile, createSchemesScript.getBytes("UTF-8"));
                    exec(hooksDir, "echo", "chmod", "0755", fixSchemesFile.getAbsolutePath());
                    exec(hooksDir, "chmod", "0755", fixSchemesFile.getAbsolutePath());
                    exec(hooksDir, "echo", fixSchemesFile.getAbsolutePath());
                    if (!exec(hooksDir, fixSchemesFile.getAbsolutePath())) {
                        log("Failed to fix xcode project schemes.  Make sure you have Cocoapods installed. ");
                        return false;
                    }

                    if (!exec(new File(tmpFile, "dist"), podTimeout, pod, "init")) {
                        log("Failed to run "+pod+" init.  Make sure you have Cocoapods installed.");
                        return false;
                    }
                    File podFile = new File(new File(tmpFile, "dist"), "Podfile");
                    if (!podFile.exists()) {
                        log("Failed to create the PodFile at " + podFile);
                        return false;
                    }
                    String podFileContents = "target '" + request.getMainClass() + "' do\n";
                    String[] pods = iosPods.split("[,;]");
                    for (String podLib : pods) {
                        podLib = podLib.trim();
                        if (podLib.isEmpty()) {
                            continue;
                        }
                        String podLibName = podLib;
                        String podLibVersion = "";
                        if (podLibName.contains(" ")) {
                            podLibName = podLib.substring(0, podLib.indexOf(" ")).trim();
                            podLibVersion = podLib.substring(podLib.indexOf(" ") + 1).trim();
                        }
                        String podSpecPath = "";
                        for (File f : podSpecFileList) {
                            if (f.getName().equals(podLibName + ".podspec")) {
                                podSpecPath = ", :path => '.'";
                                break;
                            }
                        }
                        podFileContents += "    pod  '" + podLibName + "'" + (!podLibVersion.equals("") ? (", '" + podLibVersion + "'") : "") + podSpecPath + "\n";
                    }
                    podFileContents += "end\n";

                    podFileContents = "platform :ios, '" + getDeploymentTarget(request) + "'\n" + podFileContents;

                    if (!"false".equals(request.getArg("ios.pods.use_frameworks!", "false"))) {
                        podFileContents = "use_frameworks!\n" + podFileContents;
                    }
                    if (request.getArg("ios.pods.sources", null) != null) {
                        String[] podSources = request.getArg("ios.pods.sources", null).split("[;,]");
                        for (String podSource : podSources) {
                            podSource = podSource.trim();
                            if (podSource.length() == 0) {
                                continue;
                            }

                            podFileContents = "source '" + podSource + "'\n" + podFileContents;
                        }
                    }

                    String buildSettings = "";
                    String buildSettingsPrefix = "ios.pods.build.";
                    for (String key : request.getArgs()) {
                        if (key.startsWith(buildSettingsPrefix)) {
                            if (buildSettings.length() == 0) {
                                buildSettings += "\n";
                            }
                            buildSettings += "      config.build_settings['" + key.substring(buildSettingsPrefix.length()) + "'] = \"" + request.getArg(key, "") + "\"\n";
                            ;
                        }
                    }


                    if (useMetal) {
                        buildSettings += "      config.build_settings['CLANG_ENABLE_MODULES'] = \"YES\"\n";
                    }


                    podFileContents += "\n\npost_install do |installer|\n" +
                            "  installer.pods_project.targets.each do |target|\n" +
                            "    target.build_configurations.each do |config|\n" +
                            "      config.build_settings['EXPANDED_CODE_SIGN_IDENTITY'] = \"\"\n" +
                            "      config.build_settings['CODE_SIGNING_REQUIRED'] = \"NO\"\n" +
                            "      config.build_settings['CODE_SIGNING_ALLOWED'] = \"NO\"\n" +
                            "      config.build_settings['ENABLE_STRICT_OBJC_MSGSEND'] = \"NO\"\n" +
                            buildSettings +
                            "    end\n" +
                            "  end\n" +
                            "end";

                    this.createFile(podFile, podFileContents.getBytes("UTF-8"));

                    File userHome = new File(System.getProperty("user.home"));
                    File masterRepo = new File(userHome, ".cocoapods/repos/master");
                    if (!masterRepo.exists()) {
                        log("Master Repo doesn't exist.  Running pod setup to create it");
                        exec(new File(tmpFile, "dist"), podTimeout * 3, pod, "setup");
                        if (!masterRepo.exists()) {
                            log("Failed to create master repo.  This might spell trouble...");
                        }

                    }
                    if (!exec(new File(tmpFile, "dist"), podTimeout, pod, "install")) {
                        // Perhaps we need to update the master repo
                        log("Failed to exec cocoapods.  Trying to update master repo...");
                        if (!exec(new File(tmpFile, "dist"), podTimeout * 3, pod, "repo", "update")) {
                            log("Failed to update cocoapods master repo.  Trying to clean up spec repos");
                            if (!exec(new File(tmpFile, "dist"), podTimeout * 3, pod, "repo", "update")) {
                                log("Failed to update cocoapods master repo event after cleaning spec repos.");
                                return false;
                            }
                        }

                        if (!exec(new File(tmpFile, "dist"), podTimeout, pod, "install")) {
                            log("Cocoapods failed even after updating master repo");
                            log("Trying to cleanup spec repos");
                            if (!exec(new File(tmpFile, "dist"), podTimeout, pod, "install")) {
                                log("Cocoapods failed even after cleaning up spec repos.");
                                return false;
                            }
                        }
                    }
                } catch (Exception ex) {
                    throw new BuildException("Failed to generate PodFile", ex);
                }
            }

            try {


                if (request.getArg("ios.buildType", "debug").equals("debug") &&
                        request.getArg("ios.no_strip", "false").equalsIgnoreCase("true")) {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    replaceAllInFile(pbx, "COPY_PHASE_STRIP = YES;", "COPY_PHASE_STRIP = NO;");
                    replaceAllInFile(pbx, "STRIP_STYLE = all;", "STRIP_STYLE = debugging;");
                    replaceAllInFile(pbx, "SEPARATE_STRIP = YES;", "SEPARATE_STRIP = NO;");

                    releaseString = "Debug";
                }
                if ("YES".equals(request.getArg("ios.pods.build.CLANG_ENABLE_MODULES", null))) {
                    // Needed this for WebRTC.  For some reason cocoapods was not updating these build settings.
                    // After several hours of fighting cocoapods, we'll just skip that here and brute force it.
                    // Perhaps revisit this in the future.
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    if ("YES".equals(request.getArg("ios.pods.build.CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES", null))) {
                        replaceAllInFile(pbx, "CLANG_ENABLE_MODULES = NO;", "CLANG_ENABLE_MODULES = YES; CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES = YES;");
                    } else {
                        replaceAllInFile(pbx, "CLANG_ENABLE_MODULES = NO;", "CLANG_ENABLE_MODULES = YES;");
                    }
                }

                if (googleAdUnitId != null && googleAdUnitId.length() > 0 || moPubAdUnitId != null && moPubAdUnitId.length() > 0) {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    replaceAllInFile(pbx, "SDKROOT = iphoneos;", "OTHER_LDFLAGS = \"-ObjC\";\n				SDKROOT = iphoneos;");

                } else {
                    if (request.getArg("ios.objC", "false").equals("true")) {
                        File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                        replaceAllInFile(pbx, "SDKROOT = iphoneos;", "OTHER_LDFLAGS = \"-ObjC\";\n				SDKROOT = iphoneos;");

                    }
                }

                injectToPlist(tmpFile, resDir, request);

            } catch (Exception ex) {
                throw new BuildException("Failed to inject into plist");
            }

            
            String buildSubdir = runPods ? "/dist/build/Build/Products/" : "/dist/build/";


            String projectFlag = "-workspace" ;
            String projectFlagValue = request.getMainClass() + ".xcworkspace";
            String targetFlag = "-scheme";
            String targetFlagValue = request.getMainClass() ;
            String derivedDataPathFlag = "-derivedDataPath";
            String derivedDataPathValue = "build" ;
            buildSubdir = "/dist/build/Build/Products/";
            try {
                if (!xcode7BuildMode(request, xcodebuild, projectFlag,
                        projectFlagValue,
                        targetFlag,
                        targetFlagValue,
                        derivedDataPathFlag,
                        derivedDataPathValue,
                        releaseString,
                        certificateName,
                        iosSDK)) {
                    return false;
                }
            } catch (Exception ex) {
                throw new BuildException("Failed during Xcode build", ex);
            }
            
        }

        if ("xcode".equals(getBuildTarget()) || getBuildTarget() == null) {
            xcodeProjectDir = new File(tmpFile, "dist");
            return true;
        }

        ipaFile = new File(resultDir.getAbsolutePath() + "/" + request.getMainClass() + ".ipa");



        return true;
    }

    private File xcodeProjectDir;

    public File getXcodeProjectDir() {
        return xcodeProjectDir;
    }

    private boolean xcode7BuildMode(BuildRequest request, String xcodebuild, 
            String projectFlag,
            String projectFlagValue,
            String targetFlag,
            String targetFlagValue,
            String derivedDataPathFlag,
            String derivedDataPathValue, 
            String releaseString,
            String certificateName,
            String iosSDK) throws Exception {
        debug("Starting xcode7BuildMode");
        debug("Starting xcode7BuildMode");
        if(projectFlagValue.endsWith("/")) {
            projectFlagValue = projectFlagValue.substring(0, projectFlagValue.length() - 1);
        }
        
        String homeDir = System.getProperty("user.home");
        
        File mp = null;
        File nsmp = null;
        String ppUID = null;
        String nsppUID = null;

        
        String teamId = request.getArg("ios.teamId", "");
        if (request.getArg("ios.buildType", "debug").equals("debug")) {
            teamId = request.getArg("ios.debug.teamId", teamId);
        } else {
            teamId = request.getArg("ios.release.teamId", teamId);
        }
        if(teamId.length() > 0) {
            teamId = "<key>teamID</key><string>" + teamId + "</string>";
        }
        
        String method = "development";
        if(!request.getArg("ios.buildType", "debug").equals("debug")) {
            method = "app-store";
        } 
        // can be one of: app-store, enterprise, ad-hoc, development
        method = request.getArg("ios.distributionMethod", method);
        if (request.getArg("ios.buildType", "debug").equals("debug")) {
            method = request.getArg("ios.debug.distributionMethod", method);
        } else {
            method = request.getArg("ios.release.distributionMethod", method);
        }
        
        String provisioningProfilesDict = "";
        if (xcodeVersion >= 9) {
            provisioningProfilesDict = "<key>provisioningProfiles</key>\n" +
                "    <dict>\n" +
                "        <key>"+request.getPackageName()+"</key>\n" +
                "        <string>"+provisioningProfileName+"</string>\n" +
                "    </dict>";
        }
        
        
        String iCloudKeys = "";
        if ("true".equals(request.getArg("ios.icloud.CloudDocuments", "false"))) {
            if (xcodeVersion >= 9) {
                String icloudContainerType = request.getArg("ios.buildType", "debug").equals("debug") ? 
                        "Development" : "Production";
                iCloudKeys += "\n        <key>iCloudContainerEnvironment</key>\n" +
                        "        <string>"+icloudContainerType+"</string>\n";
                        
            }
        }
        
        String exportOptionsPlist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                "<plist version=\"1.0\">\n" +
                "<dict>\n" +
                provisioningProfilesDict +
                teamId +
                iCloudKeys +
                "        <key>compileBitcode</key>\n" +
                "        <" + request.getArg("ios.bitcode", "false") + "/>\n" +
                "        <key>uploadBitcode</key>\n" +
                "        <" + request.getArg("ios.bitcode", "false") + "/>\n" +
                "        <key>method</key>\n" +
                "        <string>" + method + "</string>\n" +
                "        <key>uploadSymbols</key>\n" +
                "        <false/>\n" +
                "</dict>\n" +
                "</plist>";
        File ep = createTempFile("export", ".plist");
        debug("Export Options: "+exportOptionsPlist);
        createFile(ep, exportOptionsPlist.getBytes("UTF-8"));
        
        
        
        ep.deleteOnExit();
        File distDir = new File(tmpFile, "dist");
        String projectFile = projectFlagValue;
        if(!new File(distDir, projectFlagValue).exists()) {
            projectFile = request.getMainClass() + ".xcodeproj/project.xcworkspace";
        }
        
        // Allow users to force debug builds target arm64 if they want a 64 bit build
        String debugArchs = request.getArg("ios.buildType", "debug").equals("debug") ? "ARCHS=arm64":null;
        String onlyActiveArchs = request.getArg("ios.buildType", "debug").equals("debug") ? "ONLY_ACTIVE_ARCH=NO" : null;
        
        if (debugArchs != null && "armv7".equals(request.getArg("ios.debug.archs", null))) {
            debugArchs = "ARCHS=armv7";
        }
        

        
        
        File entitlementsFile = generateEntitlements(request, method);
        File nsEntitlementsFile = null;
        
        if (nsppUID != null) {
            nsEntitlementsFile = generateNSEntitlements(request, method);
        }

        return true;

    } 
    
    


    
    private String convertToJavaMethod(Class type) {
        if(type.isArray()) {
            type = type.getComponentType();
            if(Integer.class == type || Integer.TYPE == type) {
                return "nsDataToIntArray(";
            }
            if(Long.class == type || Long.TYPE == type) {
                return "nsDataToLongArray(";
            }
            if(Byte.class == type || Byte.TYPE == type) {
                return "nsDataToByteArr(";
            }
            if(Short.class == type || Short.TYPE == type) {
                return "nsDataToShortArray(";
            }
            if(Character.class == type || Character.TYPE == type) {
                return "nsDataToCharArray(";
            }
            if(Boolean.class == type || Boolean.TYPE == type) {
                return "nsDataToBooleanArray(";
            }
            if(Float.class == type || Float.TYPE == type) {
                return "nsDataToFloatArray(";
            }
            if(Double.class == type || Double.TYPE == type) {
                return "nsDataToDoubleArray(";
            }
        }
        if(String.class == type) {
            return "fromNSString(CN1_THREAD_GET_STATE_PASS_ARG ";
        }
        return "";
    }
    
    private String convertToClosing(Class type) {
        if(type.isArray()) {
            return ")";
        }
        if(String.class == type) {
            return ")";
        }
        return "";
    }
    
    private String convertToObjectiveCMethod(Class type) {
        if(type.isArray()) {
            return "arrayToData(";
        }
        if(String.class == type) {
            return "toNSString(CN1_THREAD_GET_STATE_PASS_ARG ";
        }
        return "";
    }
    
    private String getSimpleNameWithJavaLang(Class c) {
        if(c.isPrimitive()) {
            return c.getSimpleName();
        }
        if(c.isArray()) {
            return getSimpleNameWithJavaLang(c.getComponentType()) + "[]";
        }
        if(c.getClass().getName().startsWith("java.lang.")) {
            return c.getName();
        }
        return c.getSimpleName();
    }
    
    private String typeToXMLVMJavaName(Class type) {
        if(type.isArray()) {
            return getSimpleNameWithJavaLang(type.getComponentType()).replace('.', '_') + "_1ARRAY";
        }
        return getSimpleNameWithJavaLang(type).replace('.', '_');
    }
    private String typeToXMLVMName(Class type) {
        if(type.getName().equals("com.codename1.ui.PeerComponent")) {
            return "JAVA_LONG";
        }
        if(Integer.class == type || Integer.TYPE == type) {
            return "JAVA_INT";
        }
        if(Long.class == type || Long.TYPE == type) {
            return "JAVA_LONG";
        }
        if(Byte.class == type || Byte.TYPE == type) {
            return "JAVA_BYTE";
        }
        if(Short.class == type || Short.TYPE == type) {
            return "JAVA_SHORT";
        }
        if(Character.class == type || Character.TYPE == type) {
            return "JAVA_CHAR";
        }
        if(Boolean.class == type || Boolean.TYPE == type) {
            return "JAVA_BOOLEAN";
        }
        if(Void.class == type || Void.TYPE == type) {
            return "void";
        }
        if(Float.class == type || Float.TYPE == type) {
            return "JAVA_FLOAT";
        }
        if(Double.class == type || Double.TYPE == type) {
            return "JAVA_DOUBLE";
        }
        // array/string
        return "JAVA_OBJECT";
    }
    


    @Override
    public File[] getResults() {
        if(dsym != null) {
            return new File[] {ipaFile, icon57, icon512, dsym, xcodeProjectDir};
        } else {
            return new File[] {ipaFile, icon57, icon512, xcodeProjectDir};
        }
    }
    
    protected String generatePeerComponentCreationCode(String methodCallString) {
        return "PeerComponent.create(new long[] {" + methodCallString + "})";
    }


    @Override
    protected String convertPeerComponentToNative(String param) {
        return "((long[])" + param + ".getNativePeer())[0]";
    }
    
    @Override
    protected String getImplSuffix() {
        return "ImplCodenameOne";
    }

    protected boolean deriveGlobalInstrumentClasspath() {
        return true;
    }
    
    private GoogleServicePlist googleServicePlist;

    private class GoogleServicePlist {
        String reverseClientId;
        String clientId;
        boolean useSignIn;
        
    }
    
    private void injectToPlist(File tmpFile, File resDir, BuildRequest request) throws IOException {
        File buildinRes = new File(tmpFile, "btres");
        File mat = new File(buildinRes, "material-design-font.ttf");
        if(mat.exists()) {
            copy(new File(buildinRes, "material-design-font.ttf"), new File(resDir, "material-design-font.ttf"));
        }
        File[] fontFiles = resDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File file, String string) {
                return string.toLowerCase().endsWith(".ttf");
            }
        });
        
        String facebook = request.getArg("facebook.appId", null);
        String googleClientId = request.getArg("ios.gplus.clientId", null);
        boolean includeGooglePlus = !(googleServicePlist != null && googleServicePlist.useSignIn) && googleClientId != null;
        String statusBarWhite = request.getArg("ios.statusBarFG", null);

        File infoPlist = new File(tmpFile, "build/xcode/sys/" + request.getMainClass() + "-Info.plist");
        if(!infoPlist.exists()) {
            infoPlist = new File(tmpFile, "dist/" + request.getMainClass() + "-src/" + request.getMainClass() + "-Info.plist");
        }

        
        String lang = request.getArg("ios.devLocale", null);
        if(lang != null) {
            replaceAllInFile(infoPlist, "<string>English</string>", "<string>"  + lang + "</string>");
        }
        
        
        // nothing to inject here? move along
        String inject = request.getArg("ios.plistInject", "<key>CFBundleShortVersionString</key> 	<string>" + buildVersion +"</string>");
        
        String applicationQueriesSchemes = request.getArg("ios.applicationQueriesSchemes", null);
        if(applicationQueriesSchemes != null && applicationQueriesSchemes.length() > 0) {
            inject += "<key>LSApplicationQueriesSchemes</key>\n <array>\n";
            for(String s : applicationQueriesSchemes.split(",")) {
                inject += "<string>" + s + "</string>\n";
            }
            if (facebook != null) {
                inject += "  <string>fbauth2</string>\n";
            }
            if (googleClientId != null) {
                inject += "  <string>gplus</string>\n";
            }
            inject += "</array>";
        }
        
        
        // Some stuff for the switch to Xcode 8, but we don't need it yet
        for (String privacyKey : privacyUsageDescriptions.keySet()) {
            if (!inject.contains(privacyKey)) {
                if (privacyKey.toLowerCase().contains("location")) {
                    // We add location usage descriptions after when we deal with the ios.locationUsageDescription
                    // build hint.
                    continue;
                }
                String val = privacyUsageDescriptions.get(privacyKey);
                if (!"false".equals(val)) {
                    inject += "\n<key>"+privacyKey+"</key><string>"+val+"</string>";
                }
            }
        
        }
        
        boolean multitasking = "true".equals(request.getArg("ios.multitasking", "true"));
        if(request.getArg("ios.generateSplashScreens", "false").equals(
            "true")) {
            multitasking = false;
        }
        
        
        if (!multitasking || xcodeVersion < 9) {
            if (inject.indexOf("UIRequiresFullScreen") < 0) {
                // Temporary workaround to disable iPad multitasking support.
                // Ultimately we need to migrate to storyboards to support multitasking on iPad
                // http://stackoverflow.com/questions/32559724/ipad-multitasking-support-requires-these-orientations
                inject += "\n<key>UIRequiresFullScreen</key><true/>\n";
            }
        }
        if (!"true".equals(request.getArg("ios.generateSplashScreens", "false"))) {
            if (!inject.contains("UILaunchStoryboardName")) {
                inject += "\n<key>UILaunchStoryboardName</key><string>"+request.getArg("ios.launchStoryboardName", "LaunchScreen")+"</string>";
            }
        }
        
        //if(request.getArg("ios.background_modes", "").contains("music")) {
        //    inject += "<key>UIBackgroundModes</key><array><string>audio</string> </array>";
        //}
        
        /*if((fontFiles == null || fontFiles.length == 0) && inject == null && statusBarWhite == null && facebook == null) {
            return;
        }*/
        if(request.getArg("ios.fileSharingEnabled", "false").equals("true")) {
            inject += "\n	<key>UIFileSharingEnabled</key>\n	<true/>\n";
        }
        if(inject.indexOf("CFBundleShortVersionString") < 0) {
            inject += "\n<key>CFBundleShortVersionString</key> 	<string>" + buildVersion +"</string>";
        }
        String locationUsageDescription = null;
        if (xcodeVersion >= 9) {
            if ( (locationUsageDescription = request.getArg("ios.locationUsageDescription", null)) != null ){
                String key = "NSLocationWhenInUseUsageDescription";
                if(!inject.contains(key)) {
                    inject += "\n<key>"+key+"</key> 	<string>"+locationUsageDescription+"</string>";
                }
                if (request.getArg("ios.background_modes", "").contains("location")) {
                    key = "NSLocationAlwaysUsageDescription";
                    locationUsageDescription = request.getArg("ios.NSLocationAlwaysUsageDescription", locationUsageDescription);
                    if(!inject.contains(key)) {
                        inject += "\n<key>"+key+"</key> 	<string>"+locationUsageDescription+"</string>";
                    }
                    
                    key = "NSLocationAlwaysAndWhenInUseUsageDescription";
                    locationUsageDescription = request.getArg("ios.NSLocationAlwaysAndWhenInUseUsageDescription", locationUsageDescription);
                    if(!inject.contains(key)) {
                        inject += "\n<key>"+key+"</key> 	<string>"+locationUsageDescription+"</string>";
                    }
                    

                }


            }
        } else {
            if ( (locationUsageDescription = request.getArg("ios.locationUsageDescription", null)) != null ){
                String key = "NSLocationWhenInUseUsageDescription";
                if (request.getArg("ios.background_modes", "").contains("location")) {
                    key = "NSLocationAlwaysUsageDescription";
                }

                if(!inject.contains(key)) {
                    inject += "\n<key>"+key+"</key> 	<string>"+locationUsageDescription+"</string>";
                }
            }
        }
        String backgroundModesStr = request.getArg("ios.background_modes", null);
        if (includePush) {
            if (backgroundModesStr == null || !backgroundModesStr.contains("remote-notification")) {
                if (backgroundModesStr == null) {
                    backgroundModesStr = "";
                } else {
                    backgroundModesStr += ",";
                }
                backgroundModesStr += "remote-notification";
            }
        }

        if (backgroundModesStr != null) {
            String[] backgroundModes = backgroundModesStr.split(",");
            if (!inject.contains("UIBackgroundModes")) {
                inject += "\n<key>UIBackgroundModes</key><array>";
                for (String mode : backgroundModes) {
                    if (mode.trim().isEmpty()) {
                        continue;
                    }
                    if (mode.trim().equals("music")) {
                        mode = "audio";
                    }
                    inject += "<string>"+mode.trim()+"</string>\n";
                }
                inject += "</array>";
            } else {
                throw new IOException("You cannot use both ios.background_modes build hint and use UIBackgroundModes in the ios.plistInject build hint.  Choose one or the other");
                
            }
        }

        BufferedReader infoReader = new BufferedReader(new FileReader(infoPlist));
        StringBuilder b = new StringBuilder();
        String line = infoReader.readLine();
        while(line != null) {
            // here we inject everything we need
            if(line.indexOf("</dict>") > -1) {
                if(inject != null) {
                    b.append(inject);
                }
                if(facebook != null || includeGooglePlus || (googleServicePlist != null && googleServicePlist.useSignIn)) {
                    if (facebook != null) {
                        b.append("	<key>FacebookAppID</key>\n");
                        b.append("	<string>");
                        b.append(facebook);
                        b.append("</string>");

                        b.append("	<key>FacebookDisplayName</key>\n");
                        b.append("	<string>");
                        b.append(request.getDisplayName());
                        b.append("</string>");
                        if (!inject.contains("LSApplicationQueriesSchemes")) {
                            b.append("<key>LSApplicationQueriesSchemes</key>\n" +
                                "	<array>\n" +
                                "		<string>fbauth2</string>\n<string>gplus</string>" +
                                "	</array>");
                        }
                        
                    }

                    b.append("	<key>CFBundleURLTypes</key>\n");
                    b.append("	<array><dict>");
                    b.append("	    <key>CFBundleURLSchemes</key>\n");
                    b.append("	    <array>");
                    if (facebook != null) {
                        b.append("<string>fb");
                        b.append(facebook);
                        b.append("</string>");
                    }
                    b.append(request.getArg("ios.urlSchemes", request.getArg("ios.urlScheme", "")));
                    b.append("</array>\n");
                    b.append("</dict>");
                    if (includeGooglePlus) {
                        b.append("<dict>\n" +
    "			<key>CFBundleTypeRole</key>\n" +
    "			<string>Editor</string>\n" +
    "			<key>CFBundleURLName</key>\n" +
    "			<string>"+request.getPackageName()+"</string>\n" +
    "			<key>CFBundleURLSchemes</key>\n" +
    "			<array>\n" +
    "				<string>"+request.getPackageName()+"</string>\n" +
    "			</array>\n" +
    "		</dict>");
                    }
                    if (googleServicePlist != null && googleServicePlist.useSignIn) {
                        b.append("<dict>\n" +
    "			<key>CFBundleTypeRole</key>\n" +
    "			<string>Editor</string>\n" +
    "			<key>CFBundleURLName</key>\n" +
    "			<string>"+request.getPackageName()+"</string>\n" +
    "			<key>CFBundleURLSchemes</key>\n" +
    "			<array>\n" +
    "				<string>"+request.getPackageName()+"</string>\n" +
    "			</array>\n" +
    "		</dict>");
                        b.append("<dict>\n" +
    "			<key>CFBundleTypeRole</key>\n" +
    "			<string>Editor</string>\n" +
    "			<key>CFBundleURLName</key>\n" +
    "			<string>"+request.getPackageName()+"</string>\n" +
    "			<key>CFBundleURLSchemes</key>\n" +
    "			<array>\n" +
    "				<string>"+googleServicePlist.reverseClientId+"</string>\n" +
    "			</array>\n" +
    "		</dict>");
                    } else if (googleClientId != null) {
                        
                            b.append("<dict>\n" +
        "			<key>CFBundleTypeRole</key>\n" +
        "			<string>Editor</string>\n" +
        "			<key>CFBundleURLName</key>\n" +
        "			<string>"+request.getPackageName()+"</string>\n" +
        "			<key>CFBundleURLSchemes</key>\n" +
        "			<array>\n" +
        "				<string>"+createReverseGoogleClientId(googleClientId)+"</string>\n" +
        "			</array>\n" +
        "		</dict>");
                    }
                    b.append("</array>\n");
                } else {
                    String scheme = request.getArg("ios.urlSchemes", request.getArg("ios.urlScheme", null));
                    if(scheme != null && scheme.length() > 0) {
                        b.append("	<key>CFBundleURLTypes</key>\n");
                        b.append("	<array><dict><key>CFBundleURLSchemes</key><array>");
                        b.append(request.getArg("ios.urlSchemes", request.getArg("ios.urlScheme", "")));
                        b.append("</array></dict></array>\n");
                    }
                }
                if(statusBarWhite != null) {
                    b.append("	<key>UIViewControllerBasedStatusBarAppearance</key>\n");
                    b.append("	<false/>\n");
                    b.append("	<key>UIStatusBarStyle</key>");
                    b.append("	<string>");
                    b.append(statusBarWhite);
                    b.append("</string>");
                }
                if(fontFiles != null && fontFiles.length > 0) {
                    b.append("    <key>UIAppFonts</key>\n    <array>\n");
                    for(File f : fontFiles) {
                        b.append("        <string>");
                        b.append(f.getName());
                        b.append("</string>\n");
                    }
                    b.append("    </array>\n");
                }
            }
            b.append(line);
            b.append('\n');
            line = infoReader.readLine();
        }
        infoReader.close();
        
        FileOutputStream fo = new FileOutputStream(infoPlist);
        fo.write(b.toString().getBytes());
        fo.close();
    }

    
    
    private String entitlementsString;
    private File generateEntitlements(BuildRequest request,  String method) throws IOException {
        File entitlementsFile = createTempFile("Entitlements", ".plist");
        entitlementsFile.deleteOnExit();


        String iCloudKeys = "";
        if ("true".equals(request.getArg("ios.icloud.CloudDocuments", "false"))) {
            iCloudKeys = "	<key>com.apple.developer.icloud-services</key>\n" +
                "	<array>\n" +
                "		<string>CloudDocuments</string>\n" +
                "	</array>\n";
        }
        
        String keychainAccessGroups = "";
        if (request.getArg("ios.keychainAccessGroup", null) != null) {
            String[] accessGroups = request.getArg("ios.keychainAccessGroup", "").split(" ");
            StringBuilder sb = new StringBuilder();
            for (String grp : accessGroups) {
                sb.append("<string>").append(grp.trim()).append("</string>\n");
            }
            keychainAccessGroups = sb.toString();
        }
        
        String associatedDomains = "";
        if (request.getArg("ios.associatedDomains", null) != null) {
            String[] domains = request.getArg("ios.associatedDomains", null).split(",");
            String domainsStr = "";
            for (String domain : domains) {
                domain = domain.trim();
                if (domain.isEmpty()) continue;
                domainsStr += "<string>"+domain+"</string>\n";
            }
            associatedDomains = "<key>com.apple.developer.associated-domains</key>\n" +
                "	<array>\n" + domainsStr +
                "	</array>";
        }

        String getTaskAllow = request.getArg("ios.buildType", "debug").equals("debug") ? "<key>get-task-allow</key><true/>\n" : "";
        
        String accessWifi = "";
        if(request.getArg("ios.accessWifi", "false").equals("true")) {
            accessWifi = "<key>com.apple.developer.networking.wifi-info</key><true/>\n";
        }
        String appleSignin = "true".equals(request.getArg("ios.entitlements.applesignin", "false")) ?
                ("<key>com.apple.developer.applesignin</key>\n" +
"	<array>\n" +
"		<string>Default</string>\n" +
"	</array>") : "";
        
        
        FileOutputStream entitlementsOutput = new FileOutputStream(entitlementsFile);
        String appId = request.getAppid();
        if (appId == null) {
            throw new IllegalStateException("The appID is not set.  Please set app ID via the codename1.ios.appid of the codenameone_settings.properties file");
        }
        String ent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<plist version=\"1.0\">\n"
                + "   <dict>\n"
                + "       <key>application-identifier</key>\n"
                + "       <string>" + request.getAppid().trim() + "</string>\n"
                + "       <key>keychain-access-groups</key>\n"
                + "       <array>\n"
                + "           <string>" + request.getAppid().trim() + "</string>\n"
                + keychainAccessGroups
                + "       </array>\n"
                + appleSignin
                + iCloudKeys
                + getTaskAllow
                + associatedDomains
                + accessWifi
                + "   </dict>\n"
                + "</plist>\n";
        log("Entitlements: "+ent);

        entitlementsOutput.write(ent.getBytes());
        entitlementsOutput.close();
        entitlementsString = ent;
        return entitlementsFile;
    }
    
    // Notification service extension entitlements string
    private String nsEntitlementsString;
    /** 
     * Generates notification service extension entitlement
     * @param request
     * @param method
     * @return
     * @throws IOException 
     */
    private File generateNSEntitlements(BuildRequest request, String method) throws IOException {
        File entitlementsFile = createTempFile("Entitlements", ".plist");
        entitlementsFile.deleteOnExit();
        
       
        

        
        FileOutputStream entitlementsOutput = new FileOutputStream(entitlementsFile);
        String ent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<plist version=\"1.0\">\n"
                + "   <dict>\n"
                + "       <key>application-identifier</key>\n"
                + "       <string>" + request.getAppid().trim() + ".NotificationServiceExtension</string>\n"
                + "   </dict>\n"
                + "</plist>\n";

        
        entitlementsOutput.write(ent.getBytes());
        entitlementsOutput.close();
        nsEntitlementsString = ent;
        return entitlementsFile;
    }
    
    

    
    /**
     * 
     * @param xcodeBuild path to xcodebuild executable
     * @return The version of xcode.  This will only return an integer.  E.g. 7 for Xcode 7
     * @throws Exception 
     */
    private int getXcodeVersion(String xcodeBuild) {
        try {
            String result = execString(tmpFile, xcodeBuild, "-version");
            log("Result is "+result);
            System.out.println("Result is "+result);
            Scanner scanner = new Scanner(result);
            scanner.useDelimiter("\n");
            while (scanner.hasNext()) {
                String line = scanner.next();
                if (line.startsWith("Xcode")) {
                    log("Xcode version line matching pattern: "+line);
                    String[] parts = line.split(" ");
                    if (parts.length < 2) {
                        log("Xcode version line did not contain version number.");
                    }
                    if (parts[1].indexOf(".") >= 0) {
                        parts[1] = parts[1].substring(0, parts[1].indexOf(".")).trim();
                    }
                    return Integer.parseInt(parts[1]);
                }
            }
        } catch (Exception ex) {
            log("Problem getting Xcode version: "+ex.getMessage());
            ex.printStackTrace();
            return -1;
        }
        log("Did not find any lines in Xcode version that matched the patterns we were looking for.  Returning version -1");
        return -1;
    }

    private int getMajorVersionInt(String versionStr, int defaultVal) {
        int pos;
        if ((pos = versionStr.indexOf(".")) != -1) {
            try {
                return Integer.parseInt(versionStr.substring(0, pos));
            } catch (Throwable ex){}
        } else {
            try {
                return Integer.parseInt(versionStr);
            } catch (Throwable ex){}
        }
        return defaultVal;
    }
    

    
    private static Element getNextElement(NodeList l, int currPos) {
        int len = l.getLength();
        Element nextEl = null;
        for (int j=currPos+1; j<len; j++) {
            Node nextN = l.item(j);
            if (nextN instanceof Element) {
                nextEl = (Element)nextN;
                break;
            }
        }
        return nextEl;
    }

    
    private File getIconDirectory(BuildRequest request) {

        File iconDirectory = new File(tmpFile, "dist/" + request.getMainClass() + "-src/Images.xcassets/AppIcon.appiconset");
        if (!iconDirectory.exists()) {
            iconDirectory.mkdirs();
        }
        return iconDirectory;

    }
    
    private void copyIcon(String name, File srcDir, File destDir) throws IOException {
        copy(new File(srcDir, name), new File(destDir, name));
    }
    
    private void copyIcons(File srcDir, File destDir, String... icons) throws IOException {
        for (String icon : icons) {
            copyIcon(icon, srcDir, destDir);
        }
    }
    
    private boolean generateIcons(BuildRequest request) throws Exception {

        File iconDirectory = getIconDirectory(request);
        File resDir = getResDir();
        
        BufferedImage iconImage = ImageIO.read(new ByteArrayInputStream(request.getIcon()));
        icon512 = new File(iconDirectory, "iTunesArtwork");
        createFile(icon512, request.getIcon());
        icon57 = new File(iconDirectory, "Icon.png");
        createIconFile(icon57, iconImage, 57, 57);
        createIconFile(new File(iconDirectory, "iPhoneNotification@2x.png"), iconImage, 40, 40);
        createIconFile(new File(iconDirectory, "iPhoneNotification@3x.png"), iconImage, 60, 60);
        createIconFile(new File(iconDirectory, "iPhoneSpotlight.png"), iconImage, 29, 29);
        createIconFile(new File(iconDirectory, "iPhoneSpotlight@2x.png"), iconImage, 58, 58);
        createIconFile(new File(iconDirectory, "iPhoneSpotlight@3x.png"), iconImage, 87, 87);
        createIconFile(new File(iconDirectory, "iPhone7Spotlight@2x.png"), iconImage, 80, 80);
        createIconFile(new File(iconDirectory, "iPhone7Spotlight@3x.png"), iconImage, 120, 120);
        createIconFile(new File(iconDirectory, "iPhoneApp.png"), iconImage, 57, 57);
        createIconFile(new File(iconDirectory, "iPhoneApp@2x.png"), iconImage, 114, 114);
        createIconFile(new File(iconDirectory, "iPhone7App@2x.png"), iconImage, 120, 120);
        createIconFile(new File(iconDirectory, "iPhone7App@3x.png"), iconImage, 180, 180);
        createIconFile(new File(iconDirectory, "iPadNotifications.png"), iconImage, 20, 20);
        createIconFile(new File(iconDirectory, "iPadNotification@2x.png"), iconImage, 40, 40);
        createIconFile(new File(iconDirectory, "iPadSettings.png"), iconImage, 29, 29);
        createIconFile(new File(iconDirectory, "iPadSettings@2x.png"), iconImage, 58, 58);
        createIconFile(new File(iconDirectory, "iPadSpotlight7.png"), iconImage, 40, 40);
        createIconFile(new File(iconDirectory, "iPadSpotlight7@2x.png"), iconImage, 80, 80);
        createIconFile(new File(iconDirectory, "iPadSpotlight.png"), iconImage, 50, 50);
        createIconFile(new File(iconDirectory, "iPadSpotlight@2x.png"), iconImage, 100, 100);
        createIconFile(new File(iconDirectory, "iPadApp.png"), iconImage, 72, 72);
        createIconFile(new File(iconDirectory, "iPadApp@2x.png"), iconImage, 144, 144);
        createIconFile(new File(iconDirectory, "iPadApp7.png"), iconImage, 76, 76);
        createIconFile(new File(iconDirectory, "iPadApp7@2x.png"), iconImage, 152, 152);
        createIconFile(new File(iconDirectory, "iPadPro@2x.png"), iconImage, 167, 167);
        createIconFile(new File(iconDirectory, "AppStore.png"), iconImage, 1024, 1024);
        

        copy(icon512, new File(resDir, icon512.getName()));
        copy(icon57, new File(resDir, icon57.getName()));
        copyIcons(iconDirectory, resDir, 
                "iPhoneNotification@2x.png",
                "iPhoneNotification@3x.png",
                "iPhoneSpotlight.png",
                "iPhoneSpotlight@2x.png",
                "iPhoneSpotlight@3x.png",
                "iPhone7Spotlight@2x.png",
                "iPhone7Spotlight@3x.png",
                "iPhoneApp.png",
                "iPhoneApp@2x.png",
                "iPhone7App@2x.png",
                "iPhone7App@3x.png",
                "iPadNotifications.png",
                "iPadNotification@2x.png",
                "iPadSettings.png",
                "iPadSettings@2x.png",
                "iPadSpotlight7.png",
                "iPadSpotlight7@2x.png",
                "iPadSpotlight.png",
                "iPadSpotlight@2x.png",
                "iPadApp.png",
                "iPadApp@2x.png",
                "iPadApp7.png",
                "iPadApp7@2x.png",
                "iPadPro@2x.png",
                "AppStore.png");
        
        
        return true;
    }
    
    private boolean generateIconsPre5(BuildRequest request) throws Exception {
        File iconDirectory = getIconDirectory(request);
        File resDir = getResDir();
        
        BufferedImage iconImage = ImageIO.read(new ByteArrayInputStream(request.getIcon()));
        icon512 = new File(iconDirectory, "iTunesArtwork");
        createFile(icon512, request.getIcon());
        icon57 = new File(iconDirectory, "Icon.png");
        createIconFile(icon57, iconImage, 57, 57);
        createIconFile(new File(iconDirectory, "Icon7.png"), iconImage, 60, 60);
        createIconFile(new File(iconDirectory, "Icon@2x.png"), iconImage, 114, 114);
        createIconFile(new File(iconDirectory, "Icon7@2x.png"), iconImage, 120, 120);
        createIconFile(new File(iconDirectory, "Icon-72.png"), iconImage, 72, 72);
        createIconFile(new File(iconDirectory, "Icon-76.png"), iconImage, 76, 76);
        createIconFile(new File(iconDirectory, "Icon-152.png"), iconImage, 152, 152);
        createIconFile(new File(iconDirectory, "Icon-Small-50.png"), iconImage, 50, 50);
        createIconFile(new File(iconDirectory, "Icon-Small.png"), iconImage, 29, 29);
        createIconFile(new File(iconDirectory, "Icon-Small@2x.png"), iconImage, 58, 58);
        createIconFile(new File(iconDirectory, "Icon@3x.png"), iconImage, 87, 87);
        createIconFile(new File(iconDirectory, "Icon7@3x.png"), iconImage, 180, 180);
        createIconFile(new File(iconDirectory, "Icon-167.png"), iconImage, 167, 167);
        createIconFile(new File(iconDirectory, "Icon-1024.png"), iconImage, 1024, 1024);
        

        copy(icon512, new File(resDir, icon512.getName()));
        copy(icon57, new File(resDir, icon57.getName()));
        copy(new File(iconDirectory, "Icon7.png"), new File(resDir, "Icon7.png"));
        copy(new File(iconDirectory, "Icon@2x.png"), new File(resDir, "Icon@2x.png"));
        copy(new File(iconDirectory, "Icon7@2x.png"), new File(resDir, "Icon7@2x.png"));
        copy(new File(iconDirectory, "Icon-72.png"), new File(resDir, "Icon-72.png"));
        copy(new File(iconDirectory, "Icon-76.png"), new File(resDir, "Icon-76.png"));
        copy(new File(iconDirectory, "Icon-152.png"), new File(resDir, "Icon-152.png"));
        copy(new File(iconDirectory, "Icon-Small-50.png"), new File(resDir, "Icon-Small-50.png"));
        copy(new File(iconDirectory, "Icon-Small.png"), new File(resDir, "Icon-Small.png"));
        copy(new File(iconDirectory, "Icon-Small@2x.png"), new File(resDir, "Icon-Small@2x.png"));
        copy(new File(iconDirectory, "Icon@3x.png"), new File(resDir, "Icon@3x.png"));
        copy(new File(iconDirectory, "Icon7@3x.png"), new File(resDir, "Icon7@3x.png"));
        copy(new File(iconDirectory, "Icon-167.png"), new File(resDir, "Icon-167.png"));
        
        return true;
    }
    
    private File getScreenshotDir(BuildRequest request) {

        File screenshotDirectory = new File(tmpFile, "dist/" + request.getMainClass() + "-src/Images.xcassets/LaunchImage.launchimage");
        if (!screenshotDirectory.exists()) {
            screenshotDirectory.mkdirs();
        }
        return screenshotDirectory;

    }
    
    private boolean generateLaunchScreen(BuildRequest request) throws Exception {
        File buildinRes = getBuildinRes();
        File resDir = getResDir();
        File iconDirectory = getIconDirectory(request);
        
        
        if (xcodeVersion >= 9) {
            File launchFgImage = new File(resDir, "Launch.Foreground.png");
            if (!launchFgImage.exists()) {
                copy(new File(iconDirectory, "iPadApp7@2x.png"), launchFgImage);
            }
            
            File defaultLaunchStoryBoard = new File(buildinRes, "LaunchScreen-Default.storyboard");
            File launchStoryBoard = new File(buildinRes, "LaunchScreen.storyboard");
            if (!launchStoryBoard.exists()) {
                copy(defaultLaunchStoryBoard, launchStoryBoard);
            }
            defaultLaunchStoryBoard.delete();
        }
        return true;
    }

    
    private static String createReverseGoogleClientId(String clientId) {
        String[] parts = clientId.split("\\.");
        return join(reverse(parts), ".");
    }
    
    private static String[] reverse(String[] input) {
        int len = input.length;
        String[] output = new String[len];
        for (int i=0; i<len; i++) {
            output[i] = input[len-i-1];
        }
        return output;
    }
    
    private static String join(String[] strs, String sep) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (String str : strs) {
            if (first) {
                first = false;
            } else {
                out.append(sep);
            }
            out.append(str);
        
        }
        return out.toString();
    }


/**
 * Helper class for finding and downloading the NotificationServiceExtension provisioning
 * profile from S3, which would have been generated and uploaded by the certificate wizard.
 *
 * <p>IMPORTANT: The hashing functions used for generating the S3 keys and URLs for where
 * the certificates are stored match those in the certificate wizard.  If you make changes to
 * the hash functions or naming conventions in the Certificate wizard, you must make corresponding
 * changes here.  See the PushCertificateService class in the AppleCertServiceRestCLI project.</p>
 */
private static class NotificationServiceExtensionProvisioningProfileHelper {
    private final IPhoneBuilder context;
    private final BuildRequest request;
    private final static String S3_BUCKET_NAME = "codename-one-push-certificates";
    private final static String NOTIFICATION_SERVICE_EXTENSION_MTIME_HASH_SALT = "1234567890-QWERTYUIO";
    private final byte[] appProvisioningProfileData;
    private final String appBundleId;
    private byte[] notificationServiceExtensionProvisioningProfileData;

    private String overrideUrl;

    public NotificationServiceExtensionProvisioningProfileHelper(IPhoneBuilder context, BuildRequest request, String appBundleId, byte[] appProvisioningProfileData) {
        this.context = context;
        this.request = request;
        this.appProvisioningProfileData = appProvisioningProfileData;
        this.appBundleId = appBundleId;
    }

    /**
     * Override the URL where the provisioning profile is stored with this explicit URL.  This
     * will allow developers to generate their own provisioning profile if they want, without
     * using the certificate wizard.  They would upload it themselves to their own server,
     * and provide the URL here.
     * @param url
     */
    public void setOverrideUrl(String url) {
        this.overrideUrl = url;
    }

    public String getUrl() throws IOException {
        if (overrideUrl != null) {
            return overrideUrl;
        }
        return "https://" + S3_BUCKET_NAME + ".s3.amazonaws.com/" + java.net.URLEncoder.encode(getKey(), "UTF-8");
    }

    public String getMtimeUrl() throws IOException {
        return "https://" + S3_BUCKET_NAME + ".s3.amazonaws.com/" + java.net.URLEncoder.encode(getMtimeKey(), "UTF-8");
    }

    public String getKey() throws IOException {
        return getKeyPrefix() + getProvisioningProfileHash();
    }

    public String getMtimeKey() throws IOException {
        String typePrefix = "mtime-dev-";
        if (request.isProduction()) {
            typePrefix = "mtime-prod-";
        }
        return typePrefix + getKeyPrefix()+sha1(appBundleId+NOTIFICATION_SERVICE_EXTENSION_MTIME_HASH_SALT);
    }
    private String getKeyPrefix() {
        String prefix = appBundleId + "NotificationServiceExtension-";
        return prefix;
    }

    private static String sha1(String str) throws IOException {
        return sha1(str.getBytes("UTF-8"));
    }

    private static String sha1(byte[] bytes) throws IOException {
        java.security.MessageDigest digest = null;
        try {
            digest = java.security.MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(IPhoneBuilder.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
        digest.reset();
        digest.update(bytes);
        byte[] result = digest.digest();

        return java.util.Base64.getEncoder().encodeToString(result);
    }

    public String getProvisioningProfileHash()  throws IOException {
        return sha1(appProvisioningProfileData);

    }


    public boolean tryDownloadProvisioningProfile(File dest) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)new URL(getUrl()).openConnection();
        conn.setInstanceFollowRedirects(true);
        if (conn.getResponseCode() != 200) {
            context.log("Failed to dowload NotificationServiceExtension provisioning profile from "+getUrl()+" but it was not found.  Response code "+conn.getResponseCode()+" : "+conn.getResponseMessage());
            return false;
        }
        System.out.println("Trying to download provisioning profile from "+getUrl());
        FileOutputStream baos = new FileOutputStream(dest);
        InputStream input = null;
        try {
            input = conn.getInputStream();
            copy(input, baos);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t){}
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (Throwable t) {}
            }
        }
        return true;

    }


    private byte[] readFile(File f) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            copy(fis, baos);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Throwable t){}
            }
        }
        return baos.toByteArray();
    }

}

            
}
