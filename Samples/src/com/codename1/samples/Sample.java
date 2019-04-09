/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import static com.codename1.samples.PropertiesUtil.loadProperties;
import static com.codename1.samples.PropertiesUtil.saveProperties;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 *
 * @author shannah
 */
public class Sample {

    
    public Sample(String name) {
        this.name = name;
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
    
    private String name;
    
    private String[] dependencies;
    
   
    
    private File getJavaFile(SamplesContext context) {
        return new File(context.getSrcDir(), getName() + File.separator + getName() + ".java");
    }
    
    private File getJavaFileInBuildDir(SamplesContext context) {
        return new File(getBuildSrcDir(context), "com" + File.separator + "codename1" + File.separator + "samples" + File.separator + getName() + File.separator + getName() + ".java");
    }
    
    private File getBuildSrcDir(SamplesContext context) {
        return new File(getBuildProjectDir(context), "src");
    }
    
    private File getBuildLibDir(SamplesContext context) {
        return new File(getBuildProjectDir(context), "lib");
        
    }
    
    private File getCSSDir(SamplesContext context) {
        return new File(getJavaFile(context).getParentFile(), "css");
    }
    
    private File getCSSDirInBuildDir(SamplesContext context) {
        return new File(getBuildProjectDir(context), "css");
    }
    
    private String getCompileClasspath(SamplesContext context) {
        StringBuilder sb = new StringBuilder();
        
        return sb.toString();
    }
    
    private File getBuildProjectDir(SamplesContext context) {
        return new File(context.getBuildDir(), getName());
    }
    
    
    
    public static void copyDir(File src, File dest) throws IOException {
        dest.mkdirs();
        if (!src.isDirectory()) {
            throw new IllegalArgumentException("copyDir expected 1st arg to a be a directory but received something else: "+src);
        }
        for (File child : src.listFiles()) {
            File destChild = new File(dest, child.getName());
            if (!child.isDirectory()) {
                copyFile(child, destChild);
            } else {
                copyDir(child, destChild);
            }
        }
    }
    
    private static void copyFile(File src, File dest) throws IOException {
        FileUtil.copy(src, dest);
    }
    
    private static long lastModifiedRecursive(File dir) throws IOException  {
        long mtime = dir.lastModified();
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                mtime = Math.max(lastModifiedRecursive(child), mtime);
            }
        }
        return mtime;
    }
    
    public void exportAsNetbeansProject(SamplesContext context, File destDir) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
        if (destDir.exists()) {
            throw new IOException("Destination directory "+destDir+" already exists.  Please choose a destination that doesn't exist yet");
            
        }
        copyDir(getBuildProjectDir(context), destDir);
        
    }
    
    private void copySampleProjectToBuildDir(SamplesContext context) throws IOException {
        copyDir(context.getSampleProjectTemplateDir(), getBuildProjectDir(context));
    }
    
    public File getPublicCodenameOneSettingsFile(SamplesContext context) {
        return new File(getJavaFile(context).getParentFile(), "codenameone_settings.properties");
    }
    
    public File getThemeCSSFile(SamplesContext context) {
        return new File(getCSSDir(context), "theme.css");
    }
    
    /**
     * Converts a project to CSS
     * @param context
     * @throws IOException 
     */
    public void activateCSS(SamplesContext context) throws IOException {
        getCSSDir(context).mkdirs();
        if (!getThemeCSSFile(context).exists()) {
            FileUtil.writeStringToFile("#Constants {\n" +
                "    includeNativeBool: true; \n" +
                "}",
                    getThemeCSSFile(context));
        }
        
        Properties props = loadProperties(getPublicCodenameOneSettingsFile(context));
        props.setProperty("codename1.cssTheme", "true");
        saveProperties(props, getPublicCodenameOneSettingsFile(context));
        
    }
    
    public boolean isCSSProject(SamplesContext context) throws IOException {
        return getThemeCSSFile(context).exists() 
                && "true".equals(loadProperties(getPublicCodenameOneSettingsFile(context)).getProperty("codename1.cssTheme"));
    }
    
    /**
     * Refreshes the CSS in a project that is already running.
     * @param context
     * @throws IOException 
     */
    public void refreshCSS(SamplesContext context) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
    }
    

    private void copyJavaFileToBuildDir(SamplesContext context) throws IOException {
        File dest = getJavaFileInBuildDir(context);
        dest.getParentFile().mkdirs();
        copyFile(getJavaFile(context), dest);
    }
    
    private void copyCSSToBuildDir(SamplesContext context) throws IOException {
        copyDir(getCSSDir(context), getCSSDirInBuildDir(context));
    }
    
    private void copyCodenameOneBuildClient(SamplesContext context) throws IOException {
        copyFile(context.getCodenameOneBuildClientJar(), new File(getBuildProjectDir(context), "CodeNameOneBuildClient.jar"));
    }
    
    /**
     * Synchronize any changes in the sample into the sample's project.
     * @param context
     * @throws IOException 
     */
    private void syncChangesToBuildDir(SamplesContext context) throws IOException, InterruptedException {
        context.getBuildDir().mkdirs();
        if (!getBuildProjectDir(context).exists()) {
            copySampleProjectToBuildDir(context);
            updateBuildProjectProperties(context);
            updateBuildProjectSettings(context);
            updateBuildProjectXml(context);
            copyCodenameOneBuildClient(context);
            updateCodenameOneSettings(context);
            
        }
        syncCodenameOneSettings(context);
        Properties settings = getAggregatedCodenameOneSettings(context);
        System.out.println("Settings: "+settings);
        installDependencies(context, settings);
        
        if (!getJavaFileInBuildDir(context).exists() || getJavaFileInBuildDir(context).lastModified() < getJavaFile(context).lastModified()) {
            copyJavaFileToBuildDir(context);
        }
        if (getCSSDir(context).exists()) {
            if (!getCSSDirInBuildDir(context).exists() || lastModifiedRecursive(getCSSDirInBuildDir(context)) < lastModifiedRecursive(getCSSDir(context))) {
                copyCSSToBuildDir(context);
            }
        }
        
    }
    
    private void syncCodenameOneSettings(SamplesContext context) throws IOException {
        if (isCodenameOneSettingsChanged(context)) {
            updateCodenameOneSettings(context);
        }
        
    }
    
    private Properties getAggregatedCodenameOneSettings(SamplesContext context) throws IOException {
        Properties props = new Properties();
        for (File f : new File[]{
            getSampleProjectCodenameOneSettingsFile(context),
            getPublicCodenameOneSettingsFile(context),
            context.getGlobalPrivateCodenameOneSettingsFile(),
            getPrivateCodenameOneSettingsFile(context)
            
        }) {
            if (f.exists()) {
                Properties tmp = loadProperties(f);
                for (String key : tmp.stringPropertyNames()) {
                    props.setProperty(key, tmp.getProperty(key));
                }
            }
        }
        return props;
    }
    
    private void updateCodenameOneSettings(SamplesContext context) throws IOException {
        Properties props = new Properties();
        for (File f : new File[]{
            getSampleProjectCodenameOneSettingsFile(context),
            getPublicCodenameOneSettingsFile(context),
            context.getGlobalPrivateCodenameOneSettingsFile(),
            getPrivateCodenameOneSettingsFile(context)
            
        }) {
            if (f.exists()) {
                Properties tmp = loadProperties(f);
                for (String key : tmp.stringPropertyNames()) {
                    props.setProperty(key, tmp.getProperty(key));
                }
            }
        }
        saveProperties(props, getSampleProjectCodenameOneSettingsFile(context));
        
    }
    
    private boolean isCodenameOneSettingsChanged(SamplesContext context) throws IOException {
        File projectCodenameOneSettings = getSampleProjectCodenameOneSettingsFile(context);
        for (File f : new File[]{
            getPublicCodenameOneSettingsFile(context),
            getPrivateCodenameOneSettingsFile(context),
            context.getGlobalPrivateCodenameOneSettingsFile()
        }) {
            if (f.exists() && f.lastModified() > projectCodenameOneSettings.lastModified()) {
                return true;
            }
        }
        return false;
    }

    
    /**
     * Gets the nbproject/project.properties file from the sample project
     * @param context
     * @return 
     */
    private File getSampleProjectPropertiesFile(SamplesContext context) {
        return new File(getBuildProjectDir(context), "nbproject" + File.separator + "project.properties");
    }
    
    /**
     * Gets the codenameone_settings.properties file from the sample project.
     * @param context
     * @return 
     */
    private File getSampleProjectCodenameOneSettingsFile(SamplesContext context) {
        return new File(getBuildProjectDir(context), "codenameone_settings.properties");
    }
    
    private Properties loadBuildProjectProperties(SamplesContext context) throws IOException {
        return loadProperties(getSampleProjectPropertiesFile(context));
    }
    
    /**
     * Loads the codenameone_settings.properties file from the sample project.
     * @param context
     * @return
     * @throws IOException 
     */
    private Properties loadSampleProjectCodenameOneSettings(SamplesContext context) throws IOException {
        return loadProperties(getSampleProjectCodenameOneSettingsFile(context));
    }
    
    
    
    private void saveBuildProjectProperties(SamplesContext context, Properties props) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getSampleProjectPropertiesFile(context))) {
            props.store(fos, "Updated properties");
        }
    }
    
    private void saveBuildProjectSettings(SamplesContext context, Properties props) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getSampleProjectCodenameOneSettingsFile(context))) {
            props.store(fos, "Updated settings");
        }
    }
    
    private void updateBuildProjectXml(SamplesContext context) throws IOException {
        File nbproject = new File(getBuildProjectDir(context), "nbproject");
        File projectXml = new File(nbproject, "project.xml");
        String text = FileUtil.readFileToString(projectXml);
        text = text.replace("<name>SampleProjectTemplate</name>", "<name>"+name+"</name>");
        FileUtil.writeStringToFile(text, projectXml);
    }
    
    private void updateBuildProjectProperties(SamplesContext context) throws IOException {
        Properties props = loadBuildProjectProperties(context);
        props.setProperty("application.title", getName());
        props.setProperty("dist.jar", "${dist.dir}"+File.separator+getName()+".jar");
        //project.CLDC11=../../Ports/CLDC11
        props.setProperty("project.CLDC11", context.getCldcProjectDir().getAbsolutePath());
        props.setProperty("project.CodenameOne", context.getCodenameOneProjectDir().getAbsolutePath());
        props.setProperty("project.JavaSE", context.getJavaSEProjectDir().getAbsolutePath());
        saveBuildProjectProperties(context, props);
        
    }
    
    private void updateBuildProjectSettings(SamplesContext context) throws IOException {
        Properties props = loadSampleProjectCodenameOneSettings(context);
        props.setProperty("codename1.mainName", getName());
        props.setProperty("codename1.displayName", getName());
        saveBuildProjectSettings(context, props);
    }
    
    public int run(SamplesContext context) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
        //ant -f /Users/shannah/cn1_files/dev/AppleMapsTest1213/build.xml -Dnb.internal.action.name=run run
        List<String> cmd = new ArrayList<>();
        cmd.add(context.getAnt());
        applyRunProperties(context, cmd);
        cmd.add("-f");
        cmd.add(new File(getBuildProjectDir(context), "build.xml").getAbsolutePath());
        cmd.add("-Dnb.internal.action.name=run");
        cmd.add("run");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process p = pb.start();
        setThreadLocalProcess(p);
        int result = p.waitFor();
       
        return result;
    }
    
    public File getPrivateConfigDir(SamplesContext context) {
        return new File(context.getConfigDir(), name);
    }
    
    public File getPrivateCodenameOneSettingsFile(SamplesContext context) {
        return new File(getPrivateConfigDir(context), "codenameone_settings.properties");
    }
    
    
            
    
    private void applyRunProperties(SamplesContext context, List<String> cmd) throws IOException {
        //Properties props = buildEffectiveCodenameOneSettings(context);
        //for (String key : props.stringPropertyNames()) {
        //    cmd.add("-D"+key+"="+props.getProperty(key));
        //}
    }
    
    
    
    public int runJavascript(SamplesContext context, ProcessCallback callback) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
        //ant -f /Users/shannah/cn1_files/dev/AppleMapsTest1213/build.xml -Dnb.internal.action.name=run run
        List<String> cmd = new ArrayList<>();
        cmd.add(context.getAnt());
        applyRunProperties(context, cmd);
    
        cmd.add("-f");
        cmd.add(new File(getBuildProjectDir(context), "build.xml").getAbsolutePath());
        cmd.add("clean");
        cmd.add("run-war");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        
        Process p = pb.start();
        setThreadLocalProcess(p);
        if (callback != null) {
            callback.processWillStart(p);
        }
        int result = p.waitFor();
       
        return result;
    }
    
    public int buildIOSDebug(SamplesContext context) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
        //ant -f /Users/shannah/cn1_files/dev/AppleMapsTest1213/build.xml -Dnb.internal.action.name=run run
        List<String> cmd = new ArrayList<>();
        cmd.add(context.getAnt());
        applyRunProperties(context, cmd);
    
        cmd.add("-f");
        cmd.add(new File(getBuildProjectDir(context), "build.xml").getAbsolutePath());
        cmd.add("build-for-ios-device");
        System.out.println("Running command: "+cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process p = pb.start();
        setThreadLocalProcess(p);
        int result = p.waitFor();
       
        return result;
    }
    
    public int buildAndroid(SamplesContext context) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
        //ant -f /Users/shannah/cn1_files/dev/AppleMapsTest1213/build.xml -Dnb.internal.action.name=run run
        List<String> cmd = new ArrayList<>();
        cmd.add(context.getAnt());
        applyRunProperties(context, cmd);
    
        cmd.add("-f");
        cmd.add(new File(getBuildProjectDir(context), "build.xml").getAbsolutePath());
        cmd.add("build-for-android-device");
        System.out.println("Running command: "+cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process p = pb.start();
        setThreadLocalProcess(p);
        int result = p.waitFor();
       
        return result;
    }
    
    public int sendWindowsDesktopBuild(SamplesContext context) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
        //ant -f /Users/shannah/cn1_files/dev/AppleMapsTest1213/build.xml -Dnb.internal.action.name=run run
        List<String> cmd = new ArrayList<>();
        cmd.add(context.getAnt());
        applyRunProperties(context, cmd);
    
        cmd.add("-f");
        cmd.add(new File(getBuildProjectDir(context), "build.xml").getAbsolutePath());
        cmd.add("build-for-windows-desktop");
        System.out.println("Running command: "+cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process p = pb.start();
        setThreadLocalProcess(p);
        int result = p.waitFor();
       
        return result;
    }
    
    public int sendMacDesktopBuild(SamplesContext context) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
        //ant -f /Users/shannah/cn1_files/dev/AppleMapsTest1213/build.xml -Dnb.internal.action.name=run run
        List<String> cmd = new ArrayList<>();
        cmd.add(context.getAnt());
        applyRunProperties(context, cmd);
    
        cmd.add("-f");
        cmd.add(new File(getBuildProjectDir(context), "build.xml").getAbsolutePath());
        cmd.add("build-for-mac-os-x-desktop");
        System.out.println("Running command: "+cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process p = pb.start();
        setThreadLocalProcess(p);
        int result = p.waitFor();
       
        return result;
    }
    
    
    public void save(SamplesContext ctx, String javaSourceContent) throws IOException {
        getJavaFile(ctx).getParentFile().mkdirs();
        FileUtil.writeStringToFile(javaSourceContent, getJavaFile(ctx));
    }
    
    public void openJavaSourceFile(SamplesContext ctx) throws IOException  {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().edit(getJavaFile(ctx));
        
        }
        
    }

    public boolean matchesSearch(SamplesContext ctx, String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return true;
        }
        try {
            String text = FileUtil.readFileToString(getJavaFile(ctx));
            if (getThemeCSSFile(ctx).exists()) {
                text += "\n" + FileUtil.readFileToString(getThemeCSSFile(ctx));
            }
            if (getPublicCodenameOneSettingsFile(ctx).exists()) {
                text += "\n" + FileUtil.readFileToString(getPublicCodenameOneSettingsFile(ctx));
            }
            for (String word : searchTerm.split(" ")) {
                if (!text.contains(word)) {
                    //System.out.println("Text does not contain "+word);
                    return false;
                }
                //System.out.println("Text contains "+word);
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    ThreadLocal<Process> threadLocalProcess = new ThreadLocal();
    
    public Process getThreadLocalProcess() {
        return threadLocalProcess.get();
    }
    
    private void setThreadLocalProcess(Process p) {
        threadLocalProcess.set(p);
    }

    
    public Dependencies getDependencies(SamplesContext context) throws IOException {
        Dependencies out = new Dependencies();
        try (FileInputStream fis = new FileInputStream(getJavaFile(context))) {
            Scanner scanner = new Scanner(fis, "UTF-8");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.startsWith("//require ")) {
                    return out;
                }
                String libName = line.substring(line.indexOf(" ")+1).trim();
                Dependency dep = new Dependency(libName);
                out.add(dep);
            }
        }
        return out;
    }
    
    private File getBuildProjectLib(SamplesContext context, Dependency dep) {
        return new File(getBuildLibDir(context), dep.getFile(context).getName());
    }
    
    public boolean installDependencies(SamplesContext context, Properties props) throws IOException, InterruptedException {
        boolean updated = false;
        for (Dependency dep : getDependencies(context)) {
            if (installDependency(context, dep, props)) {
                updated = true;
            }
        }
        if (updated) {
            List<String> cmd = new ArrayList<>();
            cmd.add(context.getAnt());
            applyRunProperties(context, cmd);

            cmd.add("-f");
            cmd.add(new File(getBuildProjectDir(context), "build.xml").getAbsolutePath());
            cmd.add("refresh-libs");
            cmd.add("clean");
            cmd.add("jar");
            System.out.println("Running command: "+cmd);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();
            Process p = pb.start();
            setThreadLocalProcess(p);
            int result = p.waitFor();
            if (result != 0) {
                throw new RuntimeException("Failed to build project");
            }

            
        }
        return updated;
        
    }
    
    public boolean installDependency(SamplesContext context, Dependency dep, Properties props) throws IOException, InterruptedException {
        File src = null;
        boolean updated = false;
        if (props.getProperty(dep.getName()+".projectDir", null) != null) {
            File projectDir = new File(props.getProperty(dep.getName()+".projectDir", null));
            if (projectDir.exists() && new File(projectDir, "build.xml").exists()) {
                dep.setProjectDir(projectDir);
                int res = dep.buildProject(context);
                if (res != 0) {
                    throw new RuntimeException("Failed to build library project "+dep.getName()+" at "+projectDir);
                }
                src = new File(projectDir, "dist" + File.separator + dep.getName()+".cn1lib");
            }
        }
        if (props.getProperty("cn1.library.path", null) != null) {
            for (String libPath : props.getProperty("cn1.library.path").split(File.pathSeparator)) {
                File libDir = new File(libPath);
                File f = new File(libDir, dep.getFile(context).getName());
                if (f.exists()) {
                    System.out.println("Using cn1lib at "+f);
                    src = f;
                }
            }
        }
        if (src == null) {
            src = dep.getFile(context);
            dep.update(context);
        }
        
        File dest = getBuildProjectLib(context, dep);
        
        if (!dest.exists() || src.lastModified() > dest.lastModified()) {
            dest.getParentFile().mkdirs();
            FileUtil.copy(src, dest);
            updated = true;
        }
        return updated;
        
    }
    
            
}
