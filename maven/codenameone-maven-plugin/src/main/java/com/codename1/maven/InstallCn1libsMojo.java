/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import com.codename1.ant.SortedProperties;
import static com.codename1.maven.ProjectUtil.wrap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Expand;

/**
 *
 * @author shannah
 */
@Mojo(name = "installcn1libs", requiresDependencyResolution = ResolutionScope.TEST)
public class InstallCn1libsMojo extends AbstractCN1Mojo {
    
    
    
    
    /**
     * Extracts cn1lib artifact so that its contents can be accessed directly.  Extracted to {@link #getLibDirFor(org.apache.maven.artifact.Artifact) }
     * @param artifact
     * @return
     * @throws IOException 
     */
    private boolean extractArtifact(Artifact artifact) throws IOException {
        File cn1libFile = findArtifactFile(artifact);
        File destDir = getLibDirFor(artifact);
        getLog().info("Extracting cn1lib "+artifact+" to "+destDir);
        boolean requiresUpdate = !destDir.isDirectory() || destDir.lastModified() < cn1libFile.lastModified();
        if ("true".equals(System.getProperty("cn1.updateLibs", null))) {
            
            requiresUpdate = true;
        }
        if (!requiresUpdate) {
            getLog().info("no update required for "+artifact);
            return false;
        }
        
        if (destDir.exists()) {
            Delete del = (Delete)antProject.createTask("delete");
            del.setDir(destDir);
            del.execute();
        }
        
        Expand unzip  = (Expand)antProject.createTask("unzip");
        destDir.mkdirs();
        unzip.setDest(destDir);
        unzip.setSrc(cn1libFile);
        unzip.execute();
        
        // Extract the nativese stuff now
        
        File nativeSeZip = Cn1libUtil.getNativeSEJar(artifact);
        if (nativeSeZip != null && nativeSeZip.exists()) {
            unzip = (Expand)antProject.createTask("unzip");
            unzip.setDest(new File(nativeSeZip.getParentFile(), nativeSeZip.getName()+"-extracted"));
            unzip.setSrc(nativeSeZip);
            unzip.execute();
        }
        
        return true;
        
    }

    

    
    
    private long getLastModifiedCn1libDependencies() {
        long lastModified = 0;
        for (Artifact artifact : project.getDependencyArtifacts()) {
            lastModified = Math.max(getLastModified(artifact), lastModified);
        }
        return lastModified;
    }
    
    private boolean isProjectUpdateRequired() throws IOException {
        return getLastModifiedCn1libDependencies() > getLastProjectUpdate();
    }
    
    private long getLastProjectUpdate() throws IOException {
        String lastUpdated = getMavenProperties().getProperty("lastUpdated");
        if (lastUpdated == null) {
            return 0L;
        }
        try {
            return Long.parseLong(lastUpdated);
        } catch (Exception ex) {
            return 0L;
        }
        
    }
    
    
    @Override
    public void executeImpl() throws MojoExecutionException, MojoFailureException {
        setupCef();
        
        Exception[] err = new Exception[1];
        List<Artifact> cn1libArtifacts = new ArrayList<>();
        
        project.getArtifacts().forEach(artifact -> {
            File jarFile = findArtifactFile(artifact);
            
            if (Cn1libUtil.isCN1Lib(jarFile)) {
                cn1libArtifacts.add(artifact);
                 try {
                    if(extractArtifact(artifact)) {
                        
                    }
                    
                    
                } catch (IOException ex) {
                    err[0] = ex;
                }
            }
           
        });
        boolean forceUpdate = false;
        if ("true".equals(System.getProperty("cn1.updateLibs", null))) {
            forceUpdate = true;
        }
        try {
            if (forceUpdate || isProjectUpdateRequired()) {
                File classesDir = new File(project.getBuild().getOutputDirectory());
                delTree(classesDir);
                classesDir.mkdirs();
                getLog().info("Dependencies have changed.  Updating project properties and CSS");
                // The dependencies have changed
                boolean changed = false;
                for (Artifact artifact : project.getArtifacts()) {
                    if (mergeProjectProperties(artifact)) {
                        getLog().info("Merged properties for "+artifact+" into project properties");
                        changed = true;
                    }
                    getLog().info("UPdating project css for "+artifact);
                    updateProjectCSS(artifact);
                }
                
                if (changed) {
                    getLog().info("Project properties where updated.  Saving changes");
                    saveProjectProperties();
                    
                }
                saveProjectDependencies();
            }
        } catch (IOException ioe) {
            getLog().error("Failed to update project properties and CSS files with new dependencies from pom.xml file.");
            throw new MojoExecutionException("Failed to update project properties and CSS with new dependencies.", ioe);
        }
        
        
        if (err[0] != null) {
            throw new MojoExecutionException("Failed to extract cn1lib dependencies.", err[0]);
        }
        
    }
    
    
    
    /**
     * Merges artifact's codenameone_library_appended.properties and codenameone_library_required.properties with the 
     * projects's codenameone_settings.properties file.
     * 
     * Does not persist to disk.
     * 
     * @param artifact
     * @return True if any changes were made to the codenameone_settings properties.
     * @throws IOException 
     */
    private boolean mergeProjectProperties(Artifact artifact) throws IOException {
        boolean changed = false;
        if (mergeProjectRequiredProperties(artifact)) {
            changed = true;
        }
        if (mergeProjectAppendedProperties(artifact)) {
            changed = true;
        }
        return changed;
    }
    
    /**
     * Merges the lib's appended properties with the project properties. Does not persist to file system.
     * @param artifact 
     * @return True if changes were made to the project properties.s
     * @throws IOException 
     */
    private boolean mergeProjectAppendedProperties(Artifact artifact) throws IOException {
        Properties projectProps = getProjectProperties();
        Properties libProps = getLibraryAppendedProperties(artifact);
        
        Properties merged = projectProps;
        //merged.putAll(projectProps);
        Enumeration keys = libProps.propertyNames();
        boolean changed = false;
        while(keys.hasMoreElements()){
            String key = (String) keys.nextElement();
            if(!merged.containsKey(key)){
                merged.put(key, libProps.getProperty(key));
                changed = true;
            }else{
                String val = merged.getProperty(key);
                String libval = libProps.getProperty(key);
                if(!val.contains(libval)){
                    //append libval to the property
                    merged.put(key, val + libval);
                    changed = true;
                }
            }
        }
        return changed;
    }
    
    /**
     * Merges the lib's required properties with the project properties.  Does not persist.
     * @param artifact
     * @return True if project properties were changed.
     * @throws IOException 
     */
    private boolean mergeProjectRequiredProperties(Artifact artifact) throws IOException {
        
        SortedProperties projectProps = getProjectProperties();
        SortedProperties libProps = getLibraryRequiredProperties(artifact);
        
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
                throw new BuildException("Cannot use a cn1lib with java version "
                        + "greater then the project java version");
            }
        }
        //merge and save
        SortedProperties merged = projectProps;
       // merged.putAll(projectProps);
        Enumeration keys = libProps.propertyNames();
        boolean changed = false;
        while(keys.hasMoreElements()){
            String key = (String) keys.nextElement();
            if(!merged.containsKey(key)){
                merged.put(key, libProps.getProperty(key));
                changed = true;
            }else{
                //if this property already exists with a different value the 
                //install will fail
                if(!merged.get(key).equals(libProps.getProperty(key))){
                    throw new BuildException("Property " + key + " has a conflict");
                }
            }
        }
        return changed;
          
    }
    
    /**
     * Project's theme.css file
     * @return 
     */
    private File getProjectCSSFile() {
        return new File(getProjectCSSDir(), "theme.css");
    }
    
    
    /**
     * Project's theme.css file contents.
     * 
     * @return
     * @throws IOException 
     */
    private String getProjectCSSFileContents()throws IOException {
        File cssFile = getProjectCSSFile();
        byte[] buf = new byte[(int)cssFile.length()];
        int len;
        try (FileInputStream fis = new FileInputStream(cssFile)) {
            len = fis.read(buf);
        }
        return new String(buf, "UTF-8");
    }
    
    /**
     * Lib's theme.css file contents.
     * @param artifact
     * @return
     * @throws IOException 
     */
    private String getLibCSSFileContents(Artifact artifact)throws IOException {
        File cssFile = getLibCSSFile(artifact);
        byte[] buf = new byte[(int)cssFile.length()];
        int len;
        try (FileInputStream fis = new FileInputStream(cssFile)) {
            len = fis.read(buf);
        }
        return new String(buf, "UTF-8");
    }
    
    /**
     * CSS zip for library.
     * @param artifact
     * @return
     * @throws IOException 
     */
    private File getLibCSSZip(Artifact artifact) throws IOException {
        return new File(getLibDirFor(artifact), "META-INF" + File.separator + "cn1lib" + File.separator + "css.zip");
    }
    
    /**
     * Lib CSS directory (extracted from CSSZip).  This location is likely inside the local repository.
     * @param artifact
     * @return
     * @throws IOException 
     */
    private File getLibCSSDir(Artifact artifact) throws IOException {
        return new File(getLibCSSZip(artifact).getParentFile(), "css");
    }
    
    /**
     * Gets lib theme.css file from local repository.
     * @param artifact
     * @return
     * @throws IOException 
     */
    private File getLibCSSFile(Artifact artifact) throws IOException {
        return new File(getLibCSSDir(artifact), "theme.css");
    }
   
    /**
     * Extacts lib's CSS directory inside the local repository.
     * @param artifact
     * @throws IOException 
     */
    private void extractLibCSSDir(Artifact artifact) throws IOException {
        Expand unzip = (Expand)antProject.createTask("unzip");
        unzip.setSrc(getLibCSSZip(artifact));
        unzip.setDest(getLibCSSDir(artifact));
        unzip.execute();
        
    }
    
    /*
    // Originally we were attempting to programmatically add to the classpath by generating system scope dependencies
    // but this doesn't work... keeping this here for posterity as this may still be useful for something.
    private Dependency createSystemScopeDependency(String artifactId, String groupId, String version, File location) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId+"-jar");
        dependency.setVersion(version);
        dependency.setScope(Artifact.SCOPE_SYSTEM);
        dependency.setSystemPath(location.getAbsolutePath());
        dependency.setType("jar");
        dependency.setClassifier("jar");
        
        return dependency;
    }
    */
    
    
    
    
    

   
    
    /**
     * String with existing cn1lib dependencies in the project.  This helps us determine if any dependencies have changed
     * since we last updated the project.
     * @return
     * @throws IOException 
     */
    private String getExistingDependencies() throws IOException {
        return getMavenProperties().getProperty("dependencies", "");
    }
    
    /**
     * Gets the dependencies string from the current pom.xml file which can be compared with {@link #getExistingDependencies() } to 
     * determine if the project files need to be updated (i.e. dependencies have changed since last update).
     * @return
     * @throws IOException 
     */
    private String getPomDependencies() throws IOException {
        List<String> deps = new ArrayList<>();
        project.getDependencyArtifacts().forEach(artifact -> {
            File jarFile = findArtifactFile(artifact);
            if (Cn1libUtil.isCN1Lib(jarFile)) {
                deps.add(artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getVersion());
            }
        });
        
        Collections.sort(deps);
        StringBuilder sb = new StringBuilder();
        deps.forEach(dep -> {
            sb.append(" ").append(dep);
        });
        return sb.toString().trim();
        
    }
    
    /**
     * Saves the current pom dependencies into the {@link #getMavenProperties() } and saves to disk.
     * @throws IOException 
     */
    private void saveProjectDependencies() throws IOException {
        getMavenProperties().put("dependencies", getPomDependencies());
        getMavenProperties().put("lastUpdated", ""+System.currentTimeMillis());
        saveMavenProperties();
        
        
        
    }
    
    /**
     * Checks if dependencies have changed in the pom file since the last time the project files
     * were updated.
     * @return
     * @throws IOException 
     */
    public boolean checkDependenciesChanged() throws IOException {
        return !getPomDependencies().equals(getExistingDependencies());
    }
   
    /**
     * Appended properties file for library.
     * @param artifact
     * @return 
     */
    private File getLibraryAppendedPropertiesFile(Artifact artifact) {
        File dir = getLibDirFor(artifact);
        return new File(dir, "META-INF" + File.separator + "codenameone_library_appended.properties");
    }
    
    /**
     * Appended properties for library.
     * @param artifact
     * @return
     * @throws IOException 
     */
    private SortedProperties getLibraryAppendedProperties(Artifact artifact) throws IOException {
        SortedProperties out = new SortedProperties();
        File file = getLibraryAppendedPropertiesFile(artifact);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                out.load(fis);
            }
        }
        return out;
        
    }
    
    /**
     * Required properties file for library.
     * @param artifact
     * @return 
     */
    private File getLibraryRequiredPropertiesFile(Artifact artifact) {
        File dir = getLibDirFor(artifact);
        return new File(dir, "META-INF" + File.separator + "codenameone_library_required.properties");
    }
    
    /**
     * Required properties for library.
     * @param artifact
     * @return
     * @throws IOException 
     */
    private SortedProperties getLibraryRequiredProperties(Artifact artifact) throws IOException {
        SortedProperties out = new SortedProperties();
        File file = getLibraryRequiredPropertiesFile(artifact);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                out.load(fis);
            }
        }
        return out;
        
    }
    
   
    
    /**
     * Throws IOException indicating that CSS activation in project is required.
     * @param cn1lib
     * @throws IOException 
     */
    private void failActivateCSS(Artifact cn1lib) throws IOException {
        throw new IOException("The library "+cn1lib+" cannot be added to this project because it requires CSS.  Please activate CSS in your project before trying to add this library.  \nSee https://www.codenameone.com/manual/css.html for instructions on activating CSS");
    }
    
    /**
     * Updates project CSS with the lib's CSS.
     * @param artifact
     * @throws IOException 
     */
    private void updateProjectCSS(Artifact artifact) throws IOException {
        File cssZip = getLibCSSZip(artifact);
        if (!cssZip.exists()) {
            getLog().info(cssZip+" does not exist");
            getLog().info("Checking if is cn1lib "+artifact.getFile());
            if (!getLibDirFor(artifact).exists() && Cn1libUtil.isCN1Lib(artifact.getFile())) {
                getLog().info("Extracting artifact");
                extractArtifact(artifact);
            }
            if (!cssZip.exists()) {
                getLog().info(cssZip+" still doesn't exist");
                return;
            }
        }
        
        File libCssDir = getLibCSSDir(artifact);
        if (!libCssDir.exists()) {
            System.out.println("css dir "+libCssDir+" does not exist yet.  Extracting zip");
            extractLibCSSDir(artifact);
        }
        
        File libCSSFile = getLibCSSFile(artifact);
        if (!libCSSFile.exists()) {
            System.out.println("No theme.css "+libCSSFile+" found");
            return;
        }
        
        if (!"true".equals(getProjectProperties().getProperty("codename1.cssTheme"))) {
            failActivateCSS(artifact);
        }
        
        
        if (!getProjectCSSDir().exists()) {
            failActivateCSS(artifact);
        }
        File cssFile = getProjectCSSFile();
        if (!cssFile.exists()) {
            failActivateCSS(artifact);
        }
        File cssImpl = new File(project.getBuild().getDirectory() + File.separator + "css");
        String baseName = artifact.getGroupId()+"__"+artifact.getArtifactId();
        if (!cssImpl.exists()) {
            cssImpl.mkdirs();
        }
        
        File libCssImpl = new File(cssImpl, baseName);
        if (libCssImpl.exists()) {
            delTree(libCssImpl);
        }
        libCssImpl.mkdirs();
        
        Expand unzip = (Expand)antProject.createTask("unzip");
        unzip.setSrc(cssZip);
        unzip.setDest(libCssImpl);
        unzip.execute();
        
        
    }
   
    
    private String getCefPlatform() {
        if (isMac) return "mac";
        if (isWindows) return is64Bit ? "win64" : "win32";
        if (isUnix && is64Bit) return "linux64";
        return null;
    }
    
    private void setupCef() {
        String platform = getCefPlatform();
        if (platform == null) {
            getLog().warn("CEF not supported on this platform.  Not adding dependency");
            return;
        }
        File cefZip = getJar("com.codenameone", "codenameone-cef", platform);
        if (cefZip == null || !cefZip.exists()) {
            getLog().warn("codenameone-cef not found in dependencies.  Not adding CEF dependency");
            return;
        }
        File extractedDir = new File(cefZip.getParentFile(), cefZip.getName()+"-extracted");
        if (!extractedDir.exists() || extractedDir.lastModified() < cefZip.lastModified()) {
            if (extractedDir.exists()) {
                delTree(extractedDir);
            }
            Expand expand = (Expand)antProject.createTask("unzip");
            expand.setDest(extractedDir);
            expand.setSrc(cefZip);
            expand.execute();
        }
        
        project.getProperties().setProperty("cef.dir", extractedDir.getAbsolutePath());
        System.setProperty("cef.dir", extractedDir.getAbsolutePath());
        
    }
    
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isWindows = (OS.indexOf("win") >= 0);
    

    private static boolean isMac =  (OS.indexOf("mac") >= 0);
    private static final String ARCH = System.getProperty("os.arch");

    private static boolean isUnix = (OS.indexOf("nux") >= 0);
    private static final boolean is64Bit = is64Bit();
    private static final boolean is64Bit() {
        
        String model = System.getProperty("sun.arch.data.model",
                                          System.getProperty("com.ibm.vm.bitmode"));
        if (model != null) {
            return "64".equals(model);
        }
        if ("x86-64".equals(ARCH)
            || "ia64".equals(ARCH)
            || "ppc64".equals(ARCH) || "ppc64le".equals(ARCH)
            || "sparcv9".equals(ARCH)
            || "mips64".equals(ARCH) || "mips64el".equals(ARCH)
            || "amd64".equals(ARCH)
            || "aarch64".equals(ARCH)) {
            return true;
        }
        return false;
    }
    
}
