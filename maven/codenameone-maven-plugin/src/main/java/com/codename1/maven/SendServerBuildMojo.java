/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import com.codename1.ant.CodeNameOneBuildTask;
import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

/**
 *
 * @author shannah
 */
@Mojo(name="build-old", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, 
        defaultPhase = LifecyclePhase.COMPILE)
@Execute(phase = LifecyclePhase.PACKAGE)
public class SendServerBuildMojo extends AbstractCN1Mojo {
    
  
    private File jarFile;
    private String displayName;
    private String mainClassName;
    private String packageName;
    private float version = 1.0f;
    private File icon;
    private String targetType;
    private String vendor;
    private String subtitle;
    private File pushCertificate;
    private File certificate;
    private String certPassword;
    private String keystoreAlias;
    private File provisioningProfile;
    private boolean includeSource;
    private String appid = "";
    private boolean appStoreBuild;
    private boolean automated;
    private File resultFile;
    private boolean production;
    private String buildArgs;
    
    private String targetPlatform;
    
    
    private String p(String key) {
        
        return project.getProperties().getProperty("codename1."+key, properties.getProperty("codename1."+key));
    }
    
    private float p(String key, float defaultVal) {
        return Float.parseFloat(project.getProperties().getProperty("codename1."+key, properties.getProperty("codename1."+key, ""+defaultVal)));
    }
    
    
    private boolean isUWPTarget(String target) {
        return target.indexOf("windows") > -1 && target.indexOf("desktop_windows") < 0;
    }
    
    
    protected void initVars() {
        
        displayName = p("displayName");
        mainClassName = p("mainName");
        packageName = p("packageName");
        targetType = p("targetType");
        targetPlatform = p("targetPlatform");
        version=p("version", 1f);
        icon=createFile(p("icon"));
        vendor = p("vendor");
        subtitle = p("secondaryTitle");
        String tmp = null;
        tmp = p("production");
        production = "true".equals(tmp);
        tmp = p("appStoreBuild");
        appStoreBuild = "true".equals(tmp);
        
        if (targetType.indexOf("android") > -1) {
            tmp = p("android.keystore");
        } else if (targetType.indexOf("iphone") > -1) {
            if (production) {
                tmp = p("ios.release.certificate");
                if (tmp == null || tmp.isEmpty()) {
                    tmp = p("ios.certificate");
                }
            } else {
                tmp = p("ios.debug.certificate");
                if (tmp == null || tmp.isEmpty()) {
                    tmp = p("ios.certificate");
                }
            }
        } else if (targetType.indexOf("desktop_macosx") > -1) {
            tmp = p("desktop.mac.certificate");
        } else if (isUWPTarget(targetType)) {
            tmp = p("windows.certificate");
        } else {
            tmp = p("certificate");
        }
        if (tmp != null && !tmp.isEmpty()) {
            certificate = createFile(tmp);
        }
        if (targetType.indexOf("android") > -1) {
            tmp = p("android.keystorePassword");
        } else if (targetType.indexOf("iphone") > -1) {
            if (production) {
                tmp = p("ios.release.certificatePassword");
                if (tmp == null || tmp.isEmpty()) {
                    tmp = p("ios.certificatePassword");
                }
            } else {
                tmp = p("ios.debug.certificatePassword");
                if (tmp == null || tmp.isEmpty()) {
                    tmp = p("ios.certificatePassword");
                }
            }
        } else if (targetType.indexOf("desktop_macosx") > -1) {
            tmp = p("desktop.mac.certificatePassword");
        } else if (isUWPTarget(targetType)) {
            tmp = p("windows.certificatePassword");
        } else {
            tmp = p("certificatePassword");
        }
        certPassword = tmp;
        if (targetType.indexOf("iphone") > -1) {
            if (production) {
                tmp = p("ios.release.provision");
            } else {
                tmp = p("ios.debug.provision");
            }
            if (tmp == null || tmp.isEmpty()) {
                tmp = p("ios.provision");
            }
        } else {
            tmp = p("provision");
        }
        if (tmp != null && !tmp.isEmpty()) {
            provisioningProfile = createFile(tmp);
        }
        appid = p("appId");
        if (appid == null || appid.isEmpty()) {
            appid = p("ios.appid");
        }
        tmp = p("automated");
        automated = "true".equals(tmp);
        
        if (targetType.indexOf("android") > -1) {
            tmp = p("android.keystoreAlias");
        } else {
            tmp = p("keystoreAlias");
        }
        if (tmp != null) {
            keystoreAlias = tmp;
        }
        
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        initVars();
        if (targetPlatform == null) {
            throw new MojoExecutionException("No targetPlatform defined.  Please set the codename1.targetPlatform property");
        }
        /*
         * <codeNameOne 
            jarFile="${dist.jar}"
            displayName="${codename1.displayName}"
            packageName = "${codename1.packageName}"
            mainClassName = "${codename1.mainName}"
            version="${codename1.version}"
            icon="${codename1.icon}"
            vendor="${codename1.vendor}"
            subtitle="${codename1.secondaryTitle}"
            
            targetType="iphone"
            certificate="${codename1.ios.debug.certificate}"
            certPassword="${codename1.ios.debug.certificatePassword}"
            provisioningProfile="${codename1.ios.debug.provision}"
            appid="${codename1.ios.appid}"
            automated="${automated}"
            />
         */
        CodeNameOneBuildTask buildTask = new CodeNameOneBuildTask();
        buildTask.setRootDir(getCN1ProjectDir());
        buildTask.setProject(antProject);
        buildTask.setJarFile(buildJarFile());
        buildTask.setAppStoreBuild(appStoreBuild);
        if (appid != null) buildTask.setAppid(appid);
        buildTask.setAutomated(automated);
        buildTask.setBuildArgs(buildArgs);
        buildTask.setCertPassword(certPassword);
        buildTask.setCertificate(certificate);
        buildTask.setDisplayName(displayName);
        buildTask.setIcon(icon);
        buildTask.setKeystoreAlias(keystoreAlias);
        buildTask.setMainClassName(mainClassName);
        buildTask.setDisplayName(displayName);
        buildTask.setPackageName(packageName);
        buildTask.setProduction(production);
        buildTask.setProvisioningProfile(provisioningProfile);
        buildTask.setPushCertificate(pushCertificate);
        buildTask.setSubtitle(subtitle);
        buildTask.setTargetType(targetType);
        buildTask.setVendor(vendor);
        buildTask.setVersion(version);
        
        buildTask.execute();
        
        
        
    }
    
    private File createFile(String path) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        }
        else {
            return new File(getCN1ProjectDir(), path);
        }
    }
    
    File buildJarFile() {
        File dir = new File(this.outputDirectory +"/"+ this.finalName + "-"+targetType+"-"+(production?"release":"debug"));
        if (dir.exists()) {
            delTree(dir);
        }
        dir.mkdirs();
        Copy copy = (Copy)antProject.createTask("copy");
        copy.setTodir(dir);
        FileSet fileset = new FileSet();
        fileset.setProject(antProject);
        fileset.setDir(new File(project.getBuild().getOutputDirectory()));
        fileset.setIncludes("**");
        copy.addFileset(fileset);
        copy.execute();
        
        
        File projectNativeDir = new File(getProjectNativeDir(), targetPlatform);
        if (projectNativeDir.exists()) {
            copy = (Copy)antProject.createTask("copy");
            copy.setTodir(dir);
            fileset = new FileSet();
            fileset.setProject(antProject);
            fileset.setDir(projectNativeDir);
            fileset.setIncludes("**");
            copy.addFileset(fileset);
            copy.execute();
        }

        project.getArtifacts().forEach(art->{
            if (art.getGroupId().equals("com.codenameone")) {
                if (art.getArtifactId().equals("codenameone-core") || art.getArtifactId().equals("codenameone-javase") || art.getArtifactId().equals("codenameone-java-runtime")) {
                    return;
                }
            }
            if (isCompileDependency(art)) {
                Expand expand = (Expand)antProject.createTask("unzip");
                expand.setSrc(art.getFile());
                expand.setDest(dir);
                expand.execute();
            } 
            
            if (!Cn1libUtil.isCN1Lib(art.getFile())) {
                return;
            }

            getLibsNativeJarsForPlatform(targetPlatform).forEach(zipFile -> {
                Expand expand = (Expand)antProject.createTask("unzip");
                expand.setSrc(zipFile);
                expand.setDest(dir);
                expand.execute();
            });
        });
        
        
        
        File artifact = new File(this.outputDirectory +"/"+ this.finalName + "-"+targetType+"-"+(production?"release":"debug")+".jar");
        if (artifact.exists()) {
            artifact.delete();
        }
        
        File metaInf = new File(dir, "META-INF");
        if (metaInf.exists()) {
            delTree(metaInf);
        }
        
        Zip zip = (Zip)antProject.createTask("zip");
        zip.setDestFile(artifact);
        zip.setBasedir(dir);
        zip.execute();
        
        delTree(dir);
        
        return artifact;
        
    }
    
    private boolean isCompileDependency(Artifact artifact) {
        return artifact.getScope().equals("compile") || artifact.getScope().equals("system");
        
    }
}
