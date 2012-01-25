/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.designer;

import com.codename1.ui.util.EditableResources;
import java.awt.Desktop;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Static class to contain code generation logic
 *
 * @author Shai Almog
 */
public class GenerateHelper {
    static final String[] GENERATED_PROJECT_FOLDER_NAMES = {
        "/GeneratedProject/nbproject",
        "/GeneratedProject/nbproject/private",
        "/GeneratedProject/src",
        "/GeneratedProject/src/generated",
        "/GeneratedProject/src/userclasses",
    };
    static final String[] GENERATED_PROJECT_FILE_NAMES = {
        "/GeneratedProject/build.xml",
        "/GeneratedProject/CLDC11.jar",
        "/GeneratedProject/CodeNameOneBuildClient.jar",
        "/GeneratedProject/JavaSE.jar",
        "/GeneratedProject/icon.png",
        "/GeneratedProject/nbproject/build-impl.xml",
        "/GeneratedProject/nbproject/genfiles.properties",
        "/GeneratedProject/nbproject/project.properties",
        "/GeneratedProject/nbproject/project.xml",
        "/GeneratedProject/nbproject/private/private.properties",
        "/GeneratedProject/src/userclasses/StateMachine.j"
    };


    private void replaceStringInFile(File destinationFile, String sourceValue, String newValue) throws IOException {
        DataInputStream i = new DataInputStream(new FileInputStream(destinationFile));
        byte[] b = new byte[(int)destinationFile.length()];
        i.readFully(b);
        i.close();
        String val = new String(b);
        val = val.replaceAll(sourceValue, newValue);

        Writer out = new FileWriter(destinationFile);
        out.write(val);
        out.close();
    }

    private void replaceStringInFiles(String oldString, String newString, File... files) throws IOException {
        for(File f : files) {
            replaceStringInFile(f, oldString, newString);
        }
    }

    Properties generateNetbeansProject(ResourceEditorView v, JComponent mainPanel, EditableResources loadedResources, File loadedFile) {
        try {
            if(loadedResources == null) {
                return null;
            }
            if(loadedResources.getUIResourceNames() == null || loadedResources.getUIResourceNames().length == 0) {
                JOptionPane.showMessageDialog(mainPanel, "This feature is designed for use with the GUI Builder", 
                        "Add A UI Form", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            
            GenerateNetBeansProjectDialog generateDialog = new GenerateNetBeansProjectDialog(mainPanel, loadedResources);
            if(generateDialog.canceled()) {
                return null;
            }

            File destFolder = new File(generateDialog.getDestinationDirectory());
            if(!destFolder.exists()) {
                destFolder.mkdirs();
            }
            if(!destFolder.getName().equalsIgnoreCase(generateDialog.getProjectName())) {
                destFolder = new File(destFolder, generateDialog.getProjectName());
                destFolder.mkdirs();
            }

            File srcDir = new File(destFolder, "src");
            File generatedDir = new File(srcDir, "generated");
            File nbprojectDir = new File(destFolder, "nbproject");
            File nbprojectPrivateDir = new File(nbprojectDir, "private");

            srcDir.mkdirs();
            generatedDir.mkdirs();
            nbprojectDir.mkdirs();


            if(loadedFile == null) {
                loadedFile = new File(srcDir, ResourceEditorView.normalizeFormName(generateDialog.getProjectName()) + ".res");
            } else {
                loadedFile = new File(srcDir, loadedFile.getName());
            }
            v.setLoadedFile(loadedFile);
            OutputStream out = new FileOutputStream(loadedFile);
            loadedResources.save(out);
            out.close();
            v.addToRecentMenu(loadedFile);

            String packageName = v.generateStateMachineCode(generateDialog.getInitialForm(), new File(generatedDir, "StateMachineBase.java"), false);

            Properties projectGeneratorSettings = new Properties();
            projectGeneratorSettings.load(getClass().getResourceAsStream("/GeneratedProject/codenameone_settings.properties"));
            projectGeneratorSettings.put("mainForm", generateDialog.getInitialForm());
            projectGeneratorSettings.put("package", packageName);
            File codenameOnePropertiesFile = new File(destFolder, "codenameone_settings.properties");
            out = new FileOutputStream(codenameOnePropertiesFile);
            projectGeneratorSettings.store(out, "Generated by the Codename One Designer");
            projectGeneratorSettings.put("userClassAbs",
                    new File(destFolder, projectGeneratorSettings.getProperty("userClass")).getAbsolutePath());
            out.close();

            for(String folderName : GENERATED_PROJECT_FOLDER_NAMES) {
                new File(destFolder, folderName.replace("/GeneratedProject/", "")).mkdirs();
            }
            File generatePackageFolder = new File(srcDir, generateDialog.getPackageName().replace('.', File.separatorChar));
            generatePackageFolder.mkdirs();
            
            for(String fileName : GENERATED_PROJECT_FILE_NAMES) {
                if(fileName.endsWith(".j")) {
                    createFileInDir(fileName, new File(destFolder, fileName.replace("/GeneratedProject/", "") + "ava"));
                } else {
                    createFileInDir(fileName, new File(destFolder, fileName.replace("/GeneratedProject/", "")));
                }
            }

            File mainClassFile = new File(generatePackageFolder, generateDialog.getMainClassName() + ".java");
            createFileInDir("/GeneratedProject/src/MainClass.j", mainClassFile);
            replaceStringInFiles("MainClass", generateDialog.getMainClassName(), mainClassFile, 
                    new File(destFolder, "codenameone_settings.properties"), 
                    new File(nbprojectPrivateDir, "private.properties"));

            replaceStringInFiles("resourceFileName.res", loadedFile.getName(), mainClassFile, 
                    new File(destFolder, "codenameone_settings.properties"));
            
            replaceStringInFiles("MainFormName", generateDialog.getInitialForm(),
                    new File(destFolder, "codenameone_settings.properties"));
            
            replaceStringInFiles("mainPackageName", generateDialog.getPackageName(), mainClassFile, 
                    new File(destFolder, "codenameone_settings.properties"), 
                    new File(nbprojectPrivateDir, "private.properties"));

            replaceStringInFiles("GeneratedProject", generateDialog.getProjectName(), new File(destFolder, "build.xml"),
                    new File(nbprojectDir, "build-impl.xml"),
                    new File(nbprojectDir, "project.properties"),
                    new File(nbprojectDir, "project.xml"));
            
            ResourceEditorView.openInIDE(destFolder, -1);
            return projectGeneratorSettings;
        } catch(IOException err) {
            err.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel, "IO Error occured during creation: " + err, "IO Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    void createFileInDir(String resourceName, File destinationFile) throws IOException {
        InputStream i = getClass().getResourceAsStream(resourceName);
        OutputStream out = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[65536];
        int size = i.read(buffer);
        while(size > -1) {
            out.write(buffer, 0, size);
            size = i.read(buffer);
        }
        out.close();
        i.close();
    }

    private RunOnDevice generatePreviewActivity(JComponent mainPanel, EditableResources loadedResources, RunOnDevice rd, File selection, String androidDir) {
        try {
            final Process p = new ProcessBuilder(androidDir + "/tools/android.bat", "create", "project", "--target", "2", "--name", "LWUITPreview",
                    "--path", selection.getAbsolutePath(), "--activity", "PreviewActivity", "--package", "com.codenameone.preview").
                    redirectErrorStream(true).start();
            rd.waitForProcess(p, false, "Building Activity");
            File libsFolder = new File(selection, "libs");
            libsFolder.mkdirs();
            createFileInDir("/GeneratedProject/Android/libs/IO_Android.jar", new File(libsFolder, "IO_Android.jar"));
            createFileInDir("/GeneratedProject/Android/libs/UI_Android.jar", new File(libsFolder, "UI_Android.jar"));
            File srcFolder = new File(selection, "src/com/codenameone/preview");
            srcFolder.mkdirs();
            createFileInDir("/PreviewActivity.java", new File(srcFolder, "PreviewActivity.java"));
            createFileInDir("/AndroidManifest.xml", new File(selection, "AndroidManifest.xml"));
            File assetsFolder = new File(selection, "assets");
            assetsFolder.mkdirs();
            FileOutputStream fo = new FileOutputStream(new File(assetsFolder, "r.res"));
            loadedResources.save(fo);
            fo.close();
            return rd;
        } catch(Exception err) {
            err.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel, "Error when generating Activity " + err, "IO Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public void xDevicePreview(final JComponent mainPanel, final EditableResources loadedResources) {
        try {
            String node = Preferences.userNodeForPackage(ResourceEditorView.class).get("AndroidSDK", null);
            if (node == null || !new File(node).exists()) {
                node = pickAndroidSDK();
                if (node != null) {
                    Preferences.userNodeForPackage(ResourceEditorView.class).put("AndroidSDK", node);
                } else {
                    File[] result = ResourceEditorView.showOpenFileChooserWithTitle("Find Android SDK Install", true, "Directory");
                    if (result == null || result.length == 0) {
                        if(JOptionPane.showConfirmDialog(mainPanel, "Do you want to download the Android SDK?", "Download SDK", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) ==
                                JOptionPane.YES_OPTION) {
                            try {
                                Desktop.getDesktop().browse(new URI("http://developer.android.com/sdk/"));
                            } catch(Throwable ioErr) {
                                ioErr.printStackTrace();
                            }
                        }
                        return;
                    }
                    node = result[0].getAbsolutePath();
                    if (!new File(node, "platform-tools/dx.bat").exists()) {
                        JOptionPane.showMessageDialog(mainPanel, "No Android SDK Instance", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Preferences.userNodeForPackage(ResourceEditorView.class).put("AndroidSDK", node);
                }
            }
            String antNode = Preferences.userNodeForPackage(ResourceEditorView.class).get("antLocation", null);
            if (antNode == null || !new File(antNode).exists()) {
                File[] result = ResourceEditorView.showOpenFileChooserWithTitle("Find Apache Ant Install", true, "Directory");
                if (result == null || result.length == 0) {
                    if(JOptionPane.showConfirmDialog(mainPanel, "Do you want to download Apache Ant?", "Download Ant", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) ==
                            JOptionPane.YES_OPTION) {
                        try {
                            Desktop.getDesktop().browse(new URI("http://ant.apache.org/bindownload.cgi"));
                        } catch(Throwable ioErr) {
                            ioErr.printStackTrace();
                        }
                    }
                    return;
                }
                antNode = result[0].getAbsolutePath();
                if (!new File(antNode, "bin/ant.bat").exists()) {
                    JOptionPane.showMessageDialog(mainPanel, "No Apache Ant Installation Found", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Preferences.userNodeForPackage(ResourceEditorView.class).put("antLocation", antNode);
            }
            String javaHomeNode = System.getProperty("java.home");
            if(javaHomeNode == null || !new  File(javaHomeNode + "/bin/javac.exe").exists()) {
                javaHomeNode = Preferences.userNodeForPackage(ResourceEditorView.class).get("javaHome", null);
                if (javaHomeNode == null || !new File(javaHomeNode).exists()) {
                    File[] result = ResourceEditorView.showOpenFileChooserWithTitle("Find The JDK Installation", true, "Directory");
                    if (result == null || result.length == 0) {
                        return;
                    }
                    javaHomeNode = result[0].getAbsolutePath();
                    if (!new File(javaHomeNode, "bin/javac.exe").exists()) {
                        JOptionPane.showMessageDialog(mainPanel, "No JDK Installation Found", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Preferences.userNodeForPackage(ResourceEditorView.class).put("javaHome", javaHomeNode);
                }
            }
            File tmp = File.createTempFile("resourceedit", "tmp");
            tmp.deleteOnExit();
            tmp = new File(tmp.getParentFile(), "resourceeditor");
            if(tmp.exists()) {
                ResourceEditorView.delTree(tmp);
            }
            tmp.mkdirs();
            final File tmpFile = tmp;
            final String finalNode = node;
            final String finalAntNode = antNode;
            final String finalJavaHome = javaHomeNode;
            final RunOnDevice rd = RunOnDevice.showRunDialog(mainPanel, "/help/xDeviceRunOnDeviceURL.html");
            new Thread() {
                public void run() {
                    if(generatePreviewActivity(mainPanel, loadedResources, rd, tmpFile, finalNode) != null) {
                        // run build
                        compileAndUploadToAndroid(mainPanel, rd, finalJavaHome, finalAntNode, tmpFile, finalNode);
                    }
                }
            }.start();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel, "Error " + ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Try to find a JDE instance automatically
     */
    private String pickAndroidSDK() {
        File baseDir = new File("C:\\Program Files (x86)\\Android\\android-sdk-windows");
        if(!baseDir.exists()) {
            baseDir = new File("C:\\Program Files\\Android\\android-sdk-windows");
            if(!baseDir.exists()) {
                baseDir = new File("C:\\Program Files (x86)\\android\\android-sdk");
                if(!baseDir.exists()) {
                    baseDir = new File("C:\\Program Files\\android\\android-sdk");
                     if(!baseDir.exists()) {
                        return null;
                    }
                }
            }
        }

        return baseDir.getAbsolutePath();
    }

    private void compileAndUploadToAndroid(JComponent mainPanel, final RunOnDevice rd, final String jdkDir, final String antDir, final File previewMIDletDir, final String sdkDir) {
        try {
            ProcessBuilder builder = new ProcessBuilder(antDir + "\\bin\\ant.bat", "debug", "install").
                redirectErrorStream(true).
                directory(previewMIDletDir);
            builder.environment().put("JAVA_HOME", jdkDir);
            Process p = builder.start();
            rd.waitForProcess(p, false, "Compiling And Installing Activity");
            new ProcessBuilder(sdkDir + "\\platform-tools\\adb.exe", "-d", "shell",
                    "am start -n com.codenameone.preview/com.codenameone.preview.LWUITPreview").
                redirectErrorStream(true).start();
            rd.waitForProcess(p, true, "Running App");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel, "Error " + ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
