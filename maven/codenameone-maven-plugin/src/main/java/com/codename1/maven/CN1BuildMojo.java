package com.codename1.maven;

import com.codename1.ant.AntExecutor;
import com.codename1.ant.SortedProperties;
import com.codename1.builders.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.PatternFileSelector;
import org.apache.commons.vfs2.VFS;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import static com.codename1.maven.PathUtil.path;

/**
 * Mojo that uses the CodenameOneBuildClient to send builds to the CodenameOne build server.
 *
 * It also supports a few local build targets, such as "ios-source", which generates an Xcode project,
 * and "android-source", which generates an Android gradle project.
 */
@Mojo(name="build", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class CN1BuildMojo extends AbstractCN1Mojo {

    public static final String BUILD_TARGET_XCODE_PROJECT = Executor.BUILD_TARGET_XCODE_PROJECT;
    public static final String BUILD_TARGET_ANDROID_PROJECT = Executor.BUILD_TARGET_ANDROID_PROJECT;

    /**
     * The target platform.  E.g. javase, javascript, ios, android, win
     */
    @Parameter(property = "codename1.platform", required = true)
    private String platform;

    /**
     * The build target, corresponding to ANT build targets in build-template.xml.  E.g. javascript,
     * mac-os-x-desktop, windows-desktop, windows-device, ios-device, ios-device-release, android-device, war
     */
    @Parameter(property = "codename1.buildTarget", required = true, defaultValue = "${codename1.defaultBuildTarget}")
    private String buildTarget;

    /**
     * Flag to indicate whether to use an automated build or not.
     */
    @Parameter(property = "automated", defaultValue = "false")
    private boolean automated;

    /**
     * Flag of whether to open the xcode/android studio project.
     */
    @Parameter(property = "open", defaultValue = "false")
    private boolean open;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {

        File retrolambdaJar = getJar("net.orfjackal.retrolambda", "retrolambda");
        if (retrolambdaJar != null && retrolambdaJar.exists()) {
            System.setProperty("retrolambdaJarPath", retrolambdaJar.getAbsolutePath());
        } else {
            getLog().warn("Could not find retrolambda Jar from dependencies.  Falling back to default version 2.5.1 that may have issues building Android.");
        }

        String projectPlatform = project.getProperties().getProperty("codename1.projectPlatform");
        if (projectPlatform == null) {
            getLog().debug("Skipping build because codename1.projectPlatform property is not defined");
            return;
        }
        if (!projectPlatform.equals(platform)) {
            getLog().debug("Skipping build because codename1.projectPlatform doesn't match the given platform");
            return;
        }

        if (platform.contains("android")) {
            if (!BUILD_TARGET_ANDROID_PROJECT.equals(buildTarget)) {
                String apkName = project.getBuild().getFinalName() + ".apk";
                File apkFile = new File(project.getBuild().getDirectory() + File.separator + apkName);
                try {
                    if (apkFile.exists() && apkFile.lastModified() >= getSourcesModificationTime()) {
                        getLog().info("Sources have not been modified since APK at " + apkFile + " was created.  Skipping Android build");
                        return;
                    }
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to check sources modification time", ex);
                }
            }
        }

        try {
            createAntProject();
        } catch (IOException ex) {
            getLog().error("Failed to create and build ANT project", ex);
            throw new MojoFailureException("Failed to create and build ANT project", ex);
        } catch (LibraryPropertiesException ex) {
            getLog().error("Failed to merge properties from library "+ex.libName+".  " + ex.getMessage());
            throw new MojoExecutionException("Failed to merge properties from library "+ex.libName+".  " + ex.getMessage(), ex);
        }
    }

    /**
     * Merge a set of jars into a single jar file.
     * @param dest The destination jar file. Also the first source if it already exists.
     * @param src The source jar files to be merged into the destination.
     */
    private  void mergeJars(File dest, File... src) {
        Zip task = (Zip)antProject.createTask("zip");
        task.setDestFile(dest);
        task.setUpdate(true);
        for (File srcFile : src) {
            ZipFileSet fileset = new ZipFileSet();
            fileset.setProject(antProject);
            fileset.setSrc(srcFile);
            task.addZipfileset(fileset);
        }
        task.execute();
    }


    /**
     * The dependency scopes to include in the jar file that is sent to the build server.
     */
    private static String[] BUNDLE_ARTIFACT_SCOPES = new String[] { "compile" };

    /**
     * Artifact IDs that should not be sent to the build server.
     */
    private static String[] BUNDLE_ARTIFACT_ID_BLACKLIST = new String[] {"codenameone-core", "java-runtime"};

    private void createAntProject() throws IOException, LibraryPropertiesException, MojoExecutionException {
        File cn1dir = new File(project.getBuild().getDirectory() + File.separator + "codenameone");
        File antProject = new File(cn1dir, "antProject");

        antProject.mkdirs();
        File codenameOneSettings = new File(getCN1ProjectDir(), "codenameone_settings.properties");
        File icon = new File(getCN1ProjectDir(), "icon.png");
        if (icon.exists()) {
            FileUtils.copyFile(icon, new File(antProject, "icon.png"));
        } else {
            FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("codenameone-icon.png"), new File(antProject, "icon.png"));
        }

        File codenameOneSettingsCopy = new File(antProject, codenameOneSettings.getName());
        FileUtils.copyFile(codenameOneSettings, codenameOneSettingsCopy);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("buildxml-template.xml"), new File(antProject, "build.xml"));
        File distDir = new File(antProject, "dist");
        distDir.mkdirs();


        // Build a jar with all dependencies that we will send to the build server.
        File jarWithDependencies = new File(path(project.getBuild().getDirectory(), project.getBuild().getFinalName() + "-jar-with-dependencies.jar"));
        if (jarWithDependencies.exists()) {
            getLog().debug("Found jar file with dependencies at "+jarWithDependencies+". Will use that one unless it is out of date.");
            // Evidently pom.xml file has already built the jar file - we will use that one.  This allows
            // developers to override what is included in the jar file that is sent to the server.
            for (Artifact artifact : project.getAttachedArtifacts()) {
                File jar = artifact.getFile();
                if (jar.exists() && jar.lastModified() > jarWithDependencies.lastModified()) {
                    // One of the dependency jar files is newer... so we delete the dependencies jar file
                    // and will generate a new one.
                    getLog().debug("Jar file out of date.  Dependencies have changed. "+jarWithDependencies+". Deleting");
                    jarWithDependencies.delete();
                    break;
                }

            }
        }
        if (!jarWithDependencies.exists()) {
            getLog().info(jarWithDependencies + " not found.  Generating jar with dependencies now");
            for (Artifact artifact : project.getArtifacts()) {
                if (!contains(artifact.getScope(), BUNDLE_ARTIFACT_SCOPES)) {
                    getLog().debug("Not including jar for artifact " + artifact + " because it has scope " + artifact.getScope() + " and only scopes " + Arrays.toString(BUNDLE_ARTIFACT_SCOPES) + " are to be included in builds.");
                    continue;
                }

                if (artifact.getGroupId().equals("com.codenameone") && contains(artifact.getArtifactId(), BUNDLE_ARTIFACT_ID_BLACKLIST)) {
                    getLog().debug("Not including jar for artifact " + artifact + " because it is on the artifact blacklist. I.e. the server doesn't need this.  It will be provided on the server side");
                    continue;
                }
                File jar = getJar(artifact);
                if (jar == null || !jar.exists()) {
                    getLog().debug("Not including jar for artifact " + artifact + " because jar couldn't be found.");
                    continue;
                }
                getLog().debug("Adding artifact " + artifact + " to " + jarWithDependencies);
                mergeJars(jarWithDependencies, getJar(artifact));
            }

        }



        try {
            updateCodenameOne(false);
        } catch (MojoExecutionException ex) {
            getLog().error("Failed to update Codename One");
            throw new IOException("Failed to update Codename One", ex);
        }
        File antDistDir = new File(antProject, "dist");
        File antDistJar = new File(antDistDir, project.getBuild().getFinalName() + ".jar");
        antDistDir.mkdirs();
        FileUtils.copyFile(jarWithDependencies, antDistJar);
        Properties p = new Properties();
        p.setProperty("codenameone_settings.properties", codenameOneSettingsCopy.getAbsolutePath());
        p.setProperty("CodeNameOneBuildClient.jar", path(System.getProperty("user.home"), ".codenameone", "CodeNameOneBuildClient.jar"));
        p.setProperty("dist.jar", antDistJar.getAbsolutePath());
        if (automated) {
            p.setProperty("automated", "true");
        }
        getLog().info("Running ANT build target " + buildTarget);
        String logPasskey = UUID.randomUUID().toString();
        Properties cn1SettingsProps = new Properties();
        try (FileInputStream fis = new FileInputStream(codenameOneSettingsCopy)) {
            cn1SettingsProps.load(fis);
        }

        FileSystemManager fsManager = VFS.getManager();
        FileObject jarFile = fsManager.resolveFile( "jar:"+jarWithDependencies.getAbsolutePath() + "!/META-INF/codenameone" );
        if (jarFile != null) {
            FileObject[] appendedPropsFiles = jarFile.findFiles(new PatternFileSelector(".\\*\\/codenameone_library_appended.properties"));
            if (appendedPropsFiles != null) {
                for (FileObject appendedPropsFile : appendedPropsFiles) {
                    SortedProperties appendedProps = new SortedProperties();
                    try (InputStream appendedPropsIn = appendedPropsFile.getContent().getInputStream()) {
                        appendedProps.load(appendedPropsIn);
                    }

                    for (String propName : appendedProps.stringPropertyNames()) {
                        String propVal = appendedProps.getProperty(propName);
                        if (!cn1SettingsProps.containsKey(propName)) {
                            cn1SettingsProps.put(propName, propVal);
                        } else {
                            String existing = cn1SettingsProps.getProperty(propName);
                            if (!existing.contains(propVal)) {
                                cn1SettingsProps.setProperty(propName, existing + propVal);
                            }
                        }
                    }
                }
            }
            FileObject[] requiredPropsFiles = jarFile.findFiles(new PatternFileSelector(".\\*\\/codenameone_library_required.properties"));
            if (requiredPropsFiles != null) {
                for (FileObject requiredPropsFile : requiredPropsFiles) {
                    SortedProperties requiredProps = new SortedProperties();
                    try (InputStream appendedPropsIn = requiredPropsFile.getContent().getInputStream()) {
                        requiredProps.load(appendedPropsIn);
                    }

                    String artifactId = requiredPropsFile.getParent().getName().getBaseName();
                    String groupId = requiredPropsFile.getParent().getParent().getName().getBaseName();
                    String libraryName = groupId + ":" + artifactId;
                    cn1SettingsProps = mergeRequiredProperties(libraryName, requiredProps, cn1SettingsProps);
                }
            }

        }


        cn1SettingsProps.setProperty("codename1.arg.hyp.beamId", logPasskey);

        try (FileOutputStream fos = new FileOutputStream(codenameOneSettingsCopy)) {
            cn1SettingsProps.store(fos,"");

        }
        final Process[] proc = new Process[1];
        final boolean[] closingHypLog = new boolean[1];
        Thread hyperBeamThread = new Thread(()->{

            ProcessBuilder pb = new ProcessBuilder("hyp", "beam", logPasskey);
            pb.redirectErrorStream(true);
            try {
                proc[0] = pb.start();


                InputStream out = proc[0].getInputStream();


                byte[] buffer = new byte[4000];
                while (isAlive(proc[0])) {
                    int no = out.available();
                    if (no > 0) {
                        int n = out.read(buffer, 0, Math.min(no, buffer.length));
                        getLog().info(new String(buffer, 0, n));
                    }


                    try {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException e) {
                    }
                }

            } catch (Exception ex) {
                if (!closingHypLog[0]) {
                    getLog().warn("Failed to start hyperlog.  The build log will not stream to your console.  If the build fails, you can download the error log at https://cloud.codenameone.com/secure/index.html");
                    getLog().debug(ex);
                }

            }

        });
        File[] results = null;

        try {

            if (buildTarget.startsWith("local-") || BUILD_TARGET_XCODE_PROJECT.equals(buildTarget) || BUILD_TARGET_ANDROID_PROJECT.equals(buildTarget)) {
                automated = false;
                if (buildTarget.contains("android") || BUILD_TARGET_ANDROID_PROJECT.equals(buildTarget)) {
                    results = doAndroidLocalBuild(antProject, cn1SettingsProps, antDistJar);
                } else if (buildTarget.contains("ios") || BUILD_TARGET_XCODE_PROJECT.equals(buildTarget)) {
                    results = doIOSLocalBuild(antProject, cn1SettingsProps, antDistJar);
                } else {
                    throw new MojoExecutionException("Build target not supported "+buildTarget);
                }
            } else {
                if (automated) {
                    getLog().debug("Attempting to start hyper beam stream the build log to the console");
                    hyperBeamThread.start();
                }
                AntExecutor.executeAntTask(new File(antProject, "build.xml").getAbsolutePath(), buildTarget, p);
            }
        } finally {
            if (automated) {
                try {
                    closingHypLog[0] = true;
                    proc[0].destroyForcibly();
                } catch (Exception ex) {
                }
            }
        }

        if (automated) {
            getLog().info("Extracting server result");
            File result = new File(antDistDir, "result.zip");
            if (!result.exists()) {
                throw new IOException("Failed to find result.zip after automated build");
            }

            Expand unzip = (Expand)this.antProject.createTask("unzip");
            unzip.setSrc(result);
            File resultDir = new File(antDistDir, "result");
            resultDir.mkdir();
            unzip.setDest(resultDir);
            unzip.execute();
            for (File child : resultDir.listFiles()) {
                String name = child.getName();
                int dotpos = name.lastIndexOf(".");
                if (dotpos < 0) {
                    continue;
                }
                String extension = name.substring(dotpos);
                String base = name.substring(0, dotpos);
                File copyTo = new File(project.getBuild().getDirectory() + File.separator + project.getBuild().getFinalName() + extension);
                FileUtils.copyFile(child, copyTo);
                if (".war".equals(extension)) {
                    projectHelper.attachArtifact(project, "war", copyTo);
                } else if (".zip".equals(extension) && "javascript".equals(buildTarget)) {
                    projectHelper.attachArtifact(project, "zip", "webapp", copyTo);
                } else if (".dmg".equals(extension) && "mac-os-x-desktop".equals(buildTarget)) {
                    projectHelper.attachArtifact(project, "dmg", "mac-app", copyTo);

                } else if (".pkg".equals(extension) && "mac-os-x-desktop".equals(buildTarget)) {
                    projectHelper.attachArtifact(project, "pkg", "mac-app-installer", copyTo);

                }

            }
            FileUtils.deleteDirectory(resultDir);
            result.delete();
            afterBuild();
        }




    }

    private static boolean isAlive(Process proc) {
        try {
            proc.exitValue();
            return false;
        }
        catch (IllegalThreadStateException e) {
            return true;
        }
    }

    private File getGeneratedProjectSourceDirectory() {
        return new File(project.getBuild().getDirectory(), path("generated-sources", "codenameone", buildTarget, project.getBuild().getFinalName()));
    }

    private File[] doAndroidLocalBuild(File tmpProjectDir, Properties props, File distJar) throws MojoExecutionException {
        if (BUILD_TARGET_ANDROID_PROJECT.equals(buildTarget)) {

            File generatedProject = getGeneratedProjectSourceDirectory();
            getLog().info("Generating android gradle Project to "+generatedProject+"...");
            try {
                if (generatedProject.exists()) {
                    getLog().info("Android gradle project already exists.  Checking to see if it needs updating...");
                    if (getSourcesModificationTime() <= lastModifiedRecursive(generatedProject)) {
                        getLog().info("Sources have not changed.  Skipping android gradle project generation");
                        if (open) {
                            getLog().info("Opening workspace project "+getWorkspace(props, generatedProject));
                            openWorkspace(getWorkspace(props, generatedProject));
                        }
                        return new File[]{generatedProject};

                    }
                }

            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to find last modification time of "+generatedProject);
            }
        }
        File codenameOneJar = getJar("com.codenameone", "codenameone-core");

        AndroidGradleBuilder e = new AndroidGradleBuilder();
        e.setBuildTarget(buildTarget);
        e.setLogger(getLog());
        File buildDirectory = new File(tmpProjectDir, "dist" + File.separator + "android-build");
        e.setBuildDirectory(buildDirectory);

        e.setCodenameOneJar(codenameOneJar);

        e.setPlatform("android");

        BuildRequest r = new BuildRequest();
        r.setDisplayName(props.getProperty("codename1.displayName"));
        r.setPackageName(props.getProperty("codename1.packageName"));
        r.setMainClass(props.getProperty("codename1.mainName"));
        r.setVersion(props.getProperty("codename1.version"));
        String iconPath = props.getProperty("codename1.icon");
        File iconFile = new File(iconPath);
        if (!iconFile.isAbsolute()) {
            iconFile = new File(getCN1ProjectDir(), iconPath);
        }
        try {
            BufferedImage bi = ImageIO.read(iconFile);
            if(bi.getWidth() != 512 || bi.getHeight() != 512) {
                throw new MojoExecutionException("The icon must be a 512x512 pixel PNG image. It will be scaled to the proper sizes for devices");
            }
            r.setIcon(iconFile.getAbsolutePath());
        } catch (IOException ex) {
            throw new MojoExecutionException("Error reading the icon: the icon must be a 512x512 pixel PNG image. It will be scaled to the proper sizes for devices");
        }

        r.setVendor(props.getProperty("codename1.vendor"));
        r.setSubTitle(props.getProperty("codename1.secondaryTitle"));
        r.setType("android");
        r.setKeystoreAlias(props.getProperty("codename1.android.keystoreAlias"));
        String keystorePath = props.getProperty("codename1.android.keystore");
        if (keystorePath != null) {
            File keystoreFile = new File(keystorePath);
            if (!keystoreFile.isAbsolute()) {
                keystoreFile = new File(getCN1ProjectDir(), keystorePath);
            }
            if (keystoreFile.exists()) {
                try {
                    r.setCertificate(keystoreFile.getAbsolutePath());
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to load keystore file. ", ex);
                }
            }
        }
        r.setCertificatePassword(props.getProperty("codename1.android.keystorePassword"));
        for (Object k : props.keySet()) {
            String key = (String)k;
            if(key.startsWith("codename1.arg.")) {
                String value = props.getProperty(key);
                String currentKey = key.substring(14);
                if(currentKey.indexOf(' ') > -1) {
                    throw new MojoExecutionException("The build argument contains a space in the key: '" + currentKey + "'");
                }
                r.putArgument(currentKey, value);
            }
        }

        BuildRequest request = r;
        String incSources = request.getArg("build.incSources", null);
        request.setIncludeSource(true);



        String testBuild = request.getArg("build.unitTest", null);
        if(testBuild != null && testBuild.equals("1")) {
            e.setUnitTestMode(true);
        }

        try {
            boolean result = e.build(distJar, request);
            if (!result) {
                throw new MojoExecutionException("Android build failed");
            }
            // send the response to the server
            File[] results = e.getResults();

            for (File child : results) {
                if (child == null) continue;
                String name = child.getName();
                int dotpos = name.lastIndexOf(".");
                if (dotpos < 0) {
                    continue;
                }
                String extension = name.substring(dotpos);
                String base = name.substring(0, dotpos);
                File copyTo = new File(project.getBuild().getDirectory() + File.separator + project.getBuild().getFinalName() + extension);
                try {
                    FileUtils.copyFile(child, copyTo);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to copy APK to output directory", ex);
                }
                if (".apk".equals(extension)) {
                    projectHelper.attachArtifact(project, "apk", "android-app", copyTo);
                }
            }

            if (BUILD_TARGET_ANDROID_PROJECT.equals(buildTarget) && e.getGradleProjectDirectory() != null) {
                File gradleProject = e.getGradleProjectDirectory();
                File output = getGeneratedProjectSourceDirectory();
                output.getParentFile().mkdirs();
                try {
                    getLog().info("Copying Gradle Project to "+output);
                    FileUtils.copyDirectory(gradleProject, output);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to copy gradle project at "+gradleProject+" to "+output, ex);
                }

            }

            return results;

        } catch (BuildException ex) {
            throw new MojoExecutionException("Failed to build android app", ex);
        } finally {

            e.cleanup();
        }

    }
    private File getWorkspace(Properties props, File xcprojectRoot) {
        return new File(xcprojectRoot, props.getProperty("codename1.mainName")+".xcworkspace");
    }

    private void openWorkspace(File workspace) throws MojoExecutionException {
        try {
            ProcessBuilder pb = new ProcessBuilder("open", workspace.getAbsolutePath());
            Process p = pb.start();
            int result = p.waitFor();
            if (result != 0) {
                throw new MojoExecutionException("Failed to open project at "+workspace+".  Result code: "+result);
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to open project at "+workspace, ex);
        }
    }

    private File[] doIOSLocalBuild(File tmpProjectDir, Properties props, File distJar) throws MojoExecutionException {

        if (BUILD_TARGET_XCODE_PROJECT.equals(buildTarget)) {

            File generatedProject = getGeneratedProjectSourceDirectory();
            getLog().info("Generating Xcode Project to "+generatedProject+"...");
            try {
                if (generatedProject.exists()) {
                    getLog().info("Xcode project already exists.  Checking to see if it needs updating...");
                    if (getSourcesModificationTime() <= lastModifiedRecursive(generatedProject)) {
                        getLog().info("Sources have not changed.  Skipping Xcode project generation");
                        if (open) {
                            getLog().info("Opening workspace project "+getWorkspace(props, generatedProject));
                            openWorkspace(getWorkspace(props, generatedProject));
                        }
                        return new File[]{generatedProject};

                    }
                }

            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to find last modification time of "+generatedProject);
            }
        }

        File codenameOneJar = getJar("com.codenameone", "codenameone-core");

        IPhoneBuilder e = new IPhoneBuilder();
        e.setLogger(getLog());
        File buildDirectory = new File(tmpProjectDir, "dist" + File.separator + "ios-build");
        e.setBuildDirectory(buildDirectory);

        e.setCodenameOneJar(codenameOneJar);

        e.setPlatform("ios");

        BuildRequest r = new BuildRequest();
        r.setAppid(props.getProperty("codename1.ios.appid"));
        r.setDisplayName(props.getProperty("codename1.displayName"));
        r.setPackageName(props.getProperty("codename1.packageName"));
        r.setMainClass(props.getProperty("codename1.mainName"));
        r.setVersion(props.getProperty("codename1.version"));
        String iconPath = props.getProperty("codename1.icon");
        File iconFile = new File(iconPath);
        if (!iconFile.isAbsolute()) {
            iconFile = new File(getCN1ProjectDir(), iconPath);
        }
        try {
            BufferedImage bi = ImageIO.read(iconFile);
            if(bi.getWidth() != 512 || bi.getHeight() != 512) {
                throw new MojoExecutionException("The icon must be a 512x512 pixel PNG image. It will be scaled to the proper sizes for devices");
            }
            r.setIcon(iconFile.getAbsolutePath());
        } catch (IOException ex) {
            throw new MojoExecutionException("Error reading the icon: the icon must be a 512x512 pixel PNG image. It will be scaled to the proper sizes for devices");
        }

        r.setVendor(props.getProperty("codename1.vendor"));
        r.setSubTitle(props.getProperty("codename1.secondaryTitle"));
        r.setType("ios");


        for (Object k : props.keySet()) {
            String key = (String)k;
            if(key.startsWith("codename1.arg.")) {
                String value = props.getProperty(key);
                String currentKey = key.substring(14);
                if(currentKey.indexOf(' ') > -1) {
                    throw new MojoExecutionException("The build argument contains a space in the key: '" + currentKey + "'");
                }
                r.putArgument(currentKey, value);
            }
        }

        BuildRequest request = r;
        String incSources = request.getArg("build.incSources", null);
        request.setIncludeSource(true);



        String testBuild = request.getArg("build.unitTest", null);
        if(testBuild != null && testBuild.equals("1")) {
            e.setUnitTestMode(true);
        }

        try {
            boolean result = e.build(distJar, request);
            if (!result) {
                throw new MojoExecutionException("iOS build failed");
            }
            // send the response to the server
            File[] results = e.getResults();

            for (File child : results) {
                if (child == null) continue;
                String name = child.getName();
                int dotpos = name.lastIndexOf(".");
                if (dotpos < 0) {
                    continue;
                }
                String extension = name.substring(dotpos);
                String base = name.substring(0, dotpos);
                File copyTo = new File(project.getBuild().getDirectory() + File.separator + project.getBuild().getFinalName() + extension);
                try {
                    FileUtils.copyFile(child, copyTo);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to copy APK to output directory", ex);
                }
                if (".ipa".equals(extension)) {
                    projectHelper.attachArtifact(project, "ipa", "ios-app", copyTo);
                }
            }

            if (BUILD_TARGET_XCODE_PROJECT.equals(buildTarget) && e.getXcodeProjectDir() != null) {
                File xcodeProject = e.getXcodeProjectDir();
                File output = getGeneratedProjectSourceDirectory();
                output.getParentFile().mkdirs();
                try {
                    getLog().info("Copying Xcode Project to "+output);
                    FileUtils.copyDirectory(xcodeProject, output);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to copy xcode project at "+xcodeProject+" to "+output, ex);
                }
                if (open) {

                    getLog().info("Opening workspace project "+getWorkspace(props, output));
                    openWorkspace(getWorkspace(props, output));

                }
            }

            return results;

        } catch (BuildException ex) {
            throw new MojoExecutionException("Failed to build ios app", ex);
        } finally {

            e.cleanup();
        }

    }

    protected void afterBuild() {

    }

    private static class LibraryPropertiesException extends Exception {
        private String libName;
        LibraryPropertiesException(String libName, String message) {
            super(message);
            this.libName = libName;
        }
    }

    private static class VersionMismatchException extends LibraryPropertiesException {
        VersionMismatchException(String libName, String message) {
            super(libName, message);
        }
    }

    private static class PropertyConflictException extends LibraryPropertiesException {
        PropertyConflictException(String libName, String message) {
            super(libName, message);
        }
    }



    private SortedProperties mergeRequiredProperties(String libraryName, Properties libProps, Properties projectProps) throws LibraryPropertiesException {


        String javaVersion = (String)projectProps.getProperty("codename1.arg.java.version", "8");
        String javaVersionLib = (String)libProps.get("codename1.arg.java.version");
        if(javaVersionLib != null){
            int v1 = 5;
            if(javaVersion != null){
                v1 = Integer.parseInt(javaVersion);
            }
            int v2 = Integer.parseInt(javaVersionLib);
            //if the lib java version is bigger, this library cannot be used
            if(v1 < v2){
                throw new VersionMismatchException(libraryName, "Cannot use a cn1lib with java version "
                        + "greater then the project java version");
            }
        }
        //merge and save
        SortedProperties merged = new SortedProperties();
        merged.putAll(projectProps);
        Enumeration keys = libProps.propertyNames();
        while(keys.hasMoreElements()){
            String key = (String) keys.nextElement();
            if(!merged.containsKey(key)){
                merged.put(key, libProps.getProperty(key));
            }else{
                //if this property already exists with a different value the
                //install will fail
                if(!merged.get(key).equals(libProps.getProperty(key))){
                    throw new PropertyConflictException(libraryName, "Property " + key + " has a conflict");
                }
            }
        }
        return merged;

    }
}
