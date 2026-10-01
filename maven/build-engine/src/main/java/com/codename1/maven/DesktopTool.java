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
package com.codename1.maven;

import com.codename1.build.BuildExecutionException;
import com.codename1.build.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/// One of the Codename One desktop tools -- Settings, the GUI Builder, the Game
/// Builder, the Certificate Wizard -- and how to start it bound to a project.
///
/// Each tool is a Codename One application run on the JavaSE port. It reads the
/// project it edits from a binding file named by a system property
/// (`settings.input`, ...), in the [com.codename1.project.ProjectDescriptor]
/// format. The Maven goals and the Gradle tasks both launch the tools through
/// this class; only how the tool's jars are resolved differs between them.
public final class DesktopTool {
    /// `mvn cn1:settings` / `./gradlew settings`.
    public static final DesktopTool SETTINGS = new DesktopTool("codenameone-settings",
            "com.codename1.settings.CodenameOneSettingsLauncher", "Codename One Settings", "CodenameOneSettings",
            "settings", ".codenameoneSettings", "settings-icon.png", true, true);
    /// `mvn cn1:guibuilder` / `./gradlew guibuilder`.
    public static final DesktopTool GUI_BUILDER = new DesktopTool("codenameone-guibuilder",
            "com.codename1.guibuilder.CodenameOneGUIBuilderLauncher", "Codename One GUI Builder",
            "CodenameOneGUIBuilder", "guibuilder", ".codenameoneGUIBuilder", null, false, true);
    /// `mvn cn1:gamebuilder` / `./gradlew gameBuilder`.
    public static final DesktopTool GAME_BUILDER = new DesktopTool("codenameone-gamebuilder",
            "com.codename1.gamebuilder.GameBuilderStub", "Codename One Game Builder", "CodenameOneGameBuilder",
            "gamebuilder", ".gameBuilder", null, false, false);
    /// `mvn cn1:certificatewizard` / `./gradlew certificateWizard`.
    public static final DesktopTool CERTIFICATE_WIZARD = new DesktopTool("codenameone-certificatewizard",
            "com.codename1.certificatewizard.CertificateWizardLauncher", "Certificate Wizard", "CertificateWizard",
            "certificatewizard", ".certificateWizard", "certificate-wizard-icon.png", true, true);

    private final String artifactId;
    private final String mainClass;
    private final String displayName;
    private final String windowClass;
    private final String propertyPrefix;
    private final String runtimeDirName;
    private final String iconFileName;
    private final boolean namedLauncher;
    private final boolean identity;

    private DesktopTool(String artifactId, String mainClass, String displayName, String windowClass,
                        String propertyPrefix, String runtimeDirName, String iconFileName, boolean namedLauncher,
                        boolean identity) {
        this.artifactId = artifactId;
        this.mainClass = mainClass;
        this.displayName = displayName;
        this.windowClass = windowClass;
        this.propertyPrefix = propertyPrefix;
        this.runtimeDirName = runtimeDirName;
        this.iconFileName = iconFileName;
        this.namedLauncher = namedLauncher;
        this.identity = identity;
    }

    /// The tool's artifact, `com.codenameone:<artifactId>`, published with the
    /// plugins at the same version.
    public String artifactId() {
        return artifactId;
    }

    /// The tool's main class.
    public String mainClass() {
        return mainClass;
    }

    /// The name shown in the dock, taskbar and window manager.
    public String displayName() {
        return displayName;
    }

    /// The system property the tool reads its binding file's path from.
    public String inputProperty() {
        return propertyPrefix + ".input";
    }

    /// `~/.<tool>`, where bindings, logs and the named launcher live.
    public File runtimeDir() {
        File dir = new File(System.getProperty("user.home"), runtimeDirName);
        dir.mkdirs();
        return dir;
    }

    /// Writes `content` to a fresh binding file in [runtimeDir()].
    public File writeBinding(String content) throws IOException {
        File input = new File(runtimeDir(), propertyPrefix + "-" + java.util.UUID.randomUUID() + ".input");
        Files.write(input.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return input;
    }

    /// The `<prefix>.*` system properties of this JVM, as `-D` arguments for the
    /// tool's (screenshot capture, a forced section, dark mode, an MCP port...),
    /// minus the binding and the spawn flag, which the launcher owns.
    public List<String> forwardedProperties() {
        List<String> args = new ArrayList<String>();
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith(propertyPrefix + ".")
                    && !key.equals(propertyPrefix + ".input")
                    && !key.equals(propertyPrefix + ".spawn")) {
                args.add("-D" + key + "=" + System.getProperty(key));
            }
        }
        return args;
    }

    /// Names the process for the dock, taskbar and window manager, and opens the
    /// JDK packages the JavaSE port needs on Java 9 and newer.
    ///
    /// @param primaryJar the tool's own jar, where a dock icon is read from; may be null
    public List<String> identityArgs(File primaryJar, File runtimeDir, Log log) {
        List<String> args = new ArrayList<String>();
        if (!identity) {
            return args;
        }
        args.add("-Dapple.awt.application.name=" + displayName);
        args.add("-Dcom.apple.mrj.application.apple.menu.about.name=" + displayName);
        args.add("-Dsun.awt.application.name=" + displayName);
        args.add("-Dsun.awt.X11.XWMClass=" + windowClass);
        if (isJava9OrNewer()) {
            args.add("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED");
            args.add("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED");
        }
        if (isMacOs()) {
            args.add("-Xdock:name=" + displayName);
            if (iconFileName != null && primaryJar != null) {
                File icon = extractIcon(primaryJar, runtimeDir, log);
                if (icon != null && icon.isFile()) {
                    args.add("-Xdock:icon=" + icon.getAbsolutePath());
                }
            }
        }
        return args;
    }

    /// The `icon.png` in the tool's jar, extracted for the dock, or null.
    public File extractIcon(File jar, File runtimeDir, Log log) {
        File iconFile = new File(runtimeDir, iconFileName == null ? artifactId + "-icon.png" : iconFileName);
        try (JarFile jf = new JarFile(jar)) {
            JarEntry entry = jf.getJarEntry("icon.png");
            if (entry == null) {
                return null;
            }
            try (InputStream in = jf.getInputStream(entry); OutputStream out = new FileOutputStream(iconFile)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            return iconFile;
        } catch (IOException ex) {
            log.debug("Unable to extract the " + displayName + " dock icon: " + ex.getMessage());
            return null;
        }
    }

    /// The Java launcher to start the tool with: a symbolic link named after the
    /// tool, so the process list shows its name, except on Windows.
    ///
    /// On Windows the real `javaw.exe` is used, never a copy: a copied launcher
    /// loses the JDK's bin directory as its DLL search anchor, so dependent
    /// native libraries can bind to wrong DLLs and break rendering (issue #5443).
    public File javaLauncher(File runtimeDir, Log log) {
        File java = new File(javaExecutable());
        if (!namedLauncher || isWindows()) {
            return java;
        }
        File launcher = new File(runtimeDir, displayName);
        try {
            Files.deleteIfExists(launcher.toPath());
            Files.createSymbolicLink(launcher.toPath(), java.toPath());
            return launcher;
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            log.debug("Unable to create the " + displayName + " launcher symlink: " + ex.getMessage());
            return java;
        }
    }

    /// The full command line that starts the tool bound to `input`.
    ///
    /// @param extraJvmArgs tool-specific `-D` options (a scene to open, ...)
    public List<String> command(List<File> classpath, File primaryJar, File input, List<String> extraJvmArgs,
                                Log log) {
        File runtimeDir = runtimeDir();
        List<String> command = new ArrayList<String>();
        command.add(javaLauncher(runtimeDir, log).getAbsolutePath());
        command.addAll(identityArgs(primaryJar, runtimeDir, log));
        command.add("-D" + inputProperty() + "=" + input.getAbsolutePath());
        command.addAll(forwardedProperties());
        if (extraJvmArgs != null) {
            command.addAll(extraJvmArgs);
        }
        command.add("-cp");
        command.add(join(classpath));
        command.add(mainClass);
        return command;
    }

    /// Starts the tool. Detached (the default) returns at once and logs to
    /// `<runtimeDir>/<prefix>.log`; attached waits for the window to close.
    public void launch(List<File> classpath, File primaryJar, File input, File workingDir, boolean detached,
                       List<String> extraJvmArgs, Log log) throws BuildExecutionException {
        List<String> command = command(classpath, primaryJar, input, extraJvmArgs, log);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        File logFile = new File(runtimeDir(), propertyPrefix + ".log");
        if (detached) {
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        } else {
            pb.inheritIO();
        }
        try {
            Process p = pb.start();
            if (detached) {
                log.info(displayName + " launched in the background. Log: " + logFile.getAbsolutePath());
                return;
            }
            int status = p.waitFor();
            if (status != 0) {
                throw new BuildExecutionException(displayName + " exited with status " + status);
            }
        } catch (IOException ex) {
            throw new BuildExecutionException("Failed to launch " + displayName, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BuildExecutionException("Interrupted while running " + displayName, ex);
        }
    }

    /// The jar of this tool among `classpath`, or null.
    public File primaryJar(Iterable<File> classpath) {
        for (File f : classpath) {
            if (f.getName().startsWith(artifactId + "-") && f.getName().endsWith(".jar")) {
                return f;
            }
        }
        return null;
    }

    private static String join(List<File> files) {
        StringBuilder out = new StringBuilder();
        for (File file : files) {
            if (out.length() > 0) {
                out.append(File.pathSeparator);
            }
            out.append(file.getAbsolutePath());
        }
        return out.toString();
    }

    private static String javaExecutable() {
        String executable = isWindows() ? "javaw.exe" : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath();
    }

    static boolean isMacOs() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac");
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    static boolean isJava9OrNewer() {
        String version = System.getProperty("java.specification.version", "");
        return version.length() > 0 && !version.startsWith("1.");
    }
}
