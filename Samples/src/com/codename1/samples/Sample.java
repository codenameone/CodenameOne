/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

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
        for (File child : dir.listFiles()) {
            mtime = Math.max(lastModifiedRecursive(child), mtime);
        }
        return mtime;
    }
    
    private void copySampleProjectToBuildDir(SamplesContext context) throws IOException {
        copyDir(context.getSampleProjectTemplateDir(), getBuildProjectDir(context));
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
    
    private void syncChangesToBuildDir(SamplesContext context) throws IOException {
        context.getBuildDir().mkdirs();
        if (!getBuildProjectDir(context).exists()) {
            copySampleProjectToBuildDir(context);
            updateBuildProjectProperties(context);
            updateBuildProjectSettings(context);
            copyCodenameOneBuildClient(context);
            
        }
        if (!getJavaFileInBuildDir(context).exists() || getJavaFileInBuildDir(context).lastModified() < getJavaFile(context).lastModified()) {
            copyJavaFileToBuildDir(context);
        }
        if (getCSSDir(context).exists()) {
            if (!getCSSDirInBuildDir(context).exists() || lastModifiedRecursive(getCSSDirInBuildDir(context)) < lastModifiedRecursive(getCSSDir(context))) {
                copyCSSToBuildDir(context);
            }
        }
        
    }
    
    private File getSampleProjectPropertiesFile(SamplesContext context) {
        return new File(getBuildProjectDir(context), "nbproject" + File.separator + "project.properties");
    }
    
    private File getSampleProjectSettingsFile(SamplesContext context) {
        return new File(getBuildProjectDir(context), "codenameone_settings.properties");
    }
    
    private Properties loadBuildProjectProperties(SamplesContext context) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(getSampleProjectPropertiesFile(context))) {
            props.load(fis);
        }
        return props;
    }
    
    private Properties loadBuildProjectSettings(SamplesContext context) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(getSampleProjectSettingsFile(context))) {
            props.load(fis);
        }
        return props;
    }
    
    private void saveBuildProjectProperties(SamplesContext context, Properties props) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getSampleProjectPropertiesFile(context))) {
            props.store(fos, "Updated properties");
        }
    }
    
    private void saveBuildProjectSettings(SamplesContext context, Properties props) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getSampleProjectSettingsFile(context))) {
            props.store(fos, "Updated settings");
        }
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
        Properties props = loadBuildProjectSettings(context);
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
        int result = p.waitFor();
       
        return result;
    }
    
    public File getConfigDir(SamplesContext context) {
        return new File(context.getConfigDir(), name);
    }
    
    public File getRunPropertiesFile(SamplesContext context) {
        return new File(getConfigDir(context), "codenameone_settings.properties");
    }
    
    public Properties getRunProperties(SamplesContext context) throws IOException {
        Properties props = new Properties();
        Properties globals = context.getGlobalBuildProperties();
        for (String key : globals.stringPropertyNames()) {
            props.put(key, globals.getProperty(key));
        }
        File configFile = getRunPropertiesFile(context);
        if (configFile.exists()) {
            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                p.load(fis);
            }
            for (String key : p.stringPropertyNames()) {
                props.put(key, p.getProperty(key));
            }
        }
        return props;
    }
            
    
    private void applyRunProperties(SamplesContext context, List<String> cmd) throws IOException {
        Properties props = getRunProperties(context);
        for (String key : props.stringPropertyNames()) {
            cmd.add("-D"+key+"="+props.getProperty(key));
        }
    }
    
    public int runJavascript(SamplesContext context) throws IOException, InterruptedException {
        syncChangesToBuildDir(context);
        //ant -f /Users/shannah/cn1_files/dev/AppleMapsTest1213/build.xml -Dnb.internal.action.name=run run
        List<String> cmd = new ArrayList<>();
        cmd.add(context.getAnt());
        applyRunProperties(context, cmd);
    
        cmd.add("-f");
        cmd.add(new File(getBuildProjectDir(context), "build.xml").getAbsolutePath());
        cmd.add("run-war");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process p = pb.start();
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

    
    
            
}
