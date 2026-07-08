/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.taskdefs.Java;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;

/**
 * Launches the standalone Certificate Wizard for a Codename One project.
 *
 * <pre>mvn cn1:certificatewizard</pre>
 */
@Mojo(name = "certificatewizard")
public class OpenCertificateWizardMojo extends AbstractCN1Mojo {
    private static final String LAUNCHED_PROPERTY =
            "com.codename1.maven.OpenCertificateWizardMojo.launched";

    @Parameter(property = "token", required = false)
    private String token;

    @Parameter(property = "user", required = false)
    private String user;

    @Parameter(property = "baseUrl", required = false, defaultValue = "https://cloud.codenameone.com")
    private String baseUrl;

    @Parameter(property = "outputDir", required = false)
    private File outputDir;

    @Parameter(property = "certificatewizard.login", required = false, defaultValue = "true")
    private boolean login;

    @Parameter(property = "certificatewizard.loginTimeoutSeconds", required = false, defaultValue = "180")
    private int loginTimeoutSeconds;

    @Parameter(property = "certificatewizard.spawn", required = false, defaultValue = "true")
    private boolean spawn;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (Boolean.getBoolean(LAUNCHED_PROPERTY)) {
            getLog().debug("Skipping certificatewizard: already launched in this Maven invocation");
            return;
        }
        if (!isCN1ProjectDir()) {
            getLog().debug("Skipping certificatewizard: not a CN1 project dir");
            return;
        }
        System.setProperty(LAUNCHED_PROPERTY, "true");

        File projectDir = getCN1ProjectDir();
        if (outputDir == null) {
            outputDir = new File(projectDir, "iosCerts");
        }
        outputDir.mkdirs();

        Preferences prefs = Preferences.userRoot().node("/com/codename1/ui");
        String effectiveToken = firstNonEmpty(token, "");
        String effectiveUser = firstNonEmpty(user, prefs.get("user", null), "");
        String cachedToken = firstNonEmpty(prefs.get("token", null), "");
        if (effectiveToken.length() == 0 && isUsableJwt(cachedToken)) {
            effectiveToken = cachedToken;
            getLog().debug("Using cached Codename One signing API JWT");
        }
        if (login && effectiveToken.length() == 0) {
            LoginResult result = interactiveLogin();
            if (result != null) {
                effectiveToken = result.token;
                effectiveUser = firstNonEmpty(user, result.user, effectiveUser);
                prefs.put("token", effectiveToken);
                if (effectiveUser.length() > 0) {
                    prefs.put("user", effectiveUser);
                }
            }
        }

        File runtimeDir = new File(System.getProperty("user.home"), ".certificateWizard");
        runtimeDir.mkdirs();
        File inputFile = new File(runtimeDir, "certificatewizard.input");
        File outputFile = new File(runtimeDir, UUID.randomUUID().toString() + ".output");
        writeBinding(inputFile, projectDir, outputDir, outputFile, effectiveUser, effectiveToken);

        ToolClasspath toolClasspath = getCertificateWizardClasspath();
        getLog().info("Launching certificate wizard bound to " + projectDir);
        if (effectiveToken.length() == 0) {
            getLog().warn("No Codename One bearer token was found. The wizard will open in offline mode unless "
                    + "you pass -Dtoken=<keycloak-jwt> or allow -Dcertificatewizard.login=true.");
        }

        if (shouldSpawn()) {
            launchDetached(toolClasspath, runtimeDir, inputFile, projectDir);
            return;
        }

        Java java = createJava();
        java.setFork(true);
        java.setJvm(namedJavaLauncher(runtimeDir).getAbsolutePath());
        java.setClassname("com.codename1.certificatewizard.CertificateWizardLauncher");
        java.createClasspath().setPath(joinClasspath(toolClasspath.files));
        configureDesktopIdentity(java, toolClasspath.primaryJar, runtimeDir);
        java.createJvmarg().setValue("-Dcertificatewizard.input=" + inputFile.getAbsolutePath());
        java.executeJava();
    }

    @Override
    protected boolean isCN1ProjectDir() {
        File cn1ProjectDir = getCN1ProjectDir();
        if (cn1ProjectDir == null || project == null || project.getBasedir() == null) {
            getLog().debug("Skipping certificatewizard: not a CN1 project dir");
            return false;
        }
        try {
            File current = project.getBasedir().getCanonicalFile();
            File cn1 = cn1ProjectDir.getCanonicalFile();
            if (cn1.equals(current)) {
                return true;
            }
            File rootCommon = new File(current, "common").getCanonicalFile();
            if (cn1.equals(rootCommon)) {
                return true;
            }
        } catch (IOException ex) {
            getLog().error("Failed to get canonical paths for project dir", ex);
        }
        getLog().debug("Skipping certificatewizard: not a CN1 project dir");
        return false;
    }

    private void configureDesktopIdentity(Java java, File jar, File runtimeDir) {
        for (String arg : desktopIdentityArgs(jar, runtimeDir)) {
            java.createJvmarg().setValue(arg);
        }
    }

    private boolean shouldSpawn() {
        String legacySpawn = System.getProperty("spawn");
        if (legacySpawn != null) {
            return Boolean.parseBoolean(legacySpawn);
        }
        return spawn;
    }

    private void launchDetached(ToolClasspath toolClasspath, File runtimeDir, File inputFile, File projectDir)
            throws MojoExecutionException {
        File log = new File(runtimeDir, "certificatewizard.log");
        List<String> command = new ArrayList<String>();
        command.add(namedJavaLauncher(runtimeDir).getAbsolutePath());
        command.addAll(desktopIdentityArgs(toolClasspath.primaryJar, runtimeDir));
        command.add("-Dcertificatewizard.input=" + inputFile.getAbsolutePath());
        command.add("-cp");
        command.add(joinClasspath(toolClasspath.files));
        command.add("com.codename1.certificatewizard.CertificateWizardLauncher");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        configureLauncherEnvironment(pb);
        try {
            pb.start();
            getLog().info("Certificate Wizard launched in the background. Log: " + log.getAbsolutePath());
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to launch Certificate Wizard", ex);
        }
    }

    private String javaExecutable() {
        String executable = isWindows() ? "javaw.exe" : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath();
    }

    File namedJavaLauncher(File runtimeDir) {
        File java = new File(javaExecutable());
        if (isWindows()) {
            File launcher = new File(runtimeDir, "CertificateWizard.exe");
            try {
                FileUtils.copyFile(java, launcher);
                return launcher;
            } catch (IOException ex) {
                getLog().debug("Unable to create Certificate Wizard launcher executable: " + ex.getMessage());
                return java;
            }
        }
        File launcher = new File(runtimeDir, "Certificate Wizard");
        try {
            Files.deleteIfExists(launcher.toPath());
            Files.createSymbolicLink(launcher.toPath(), java.toPath());
            return launcher;
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            getLog().debug("Unable to create Certificate Wizard launcher symlink: " + ex.getMessage());
            return java;
        }
    }

    private void configureLauncherEnvironment(ProcessBuilder pb) {
        if (!isWindows()) {
            return;
        }
        String javaBin = new File(System.getProperty("java.home"), "bin").getAbsolutePath();
        String path = pb.environment().get("PATH");
        pb.environment().put("PATH", path == null || path.length() == 0
                ? javaBin : javaBin + File.pathSeparator + path);
    }

    List<String> desktopIdentityArgs(File jar, File runtimeDir) {
        List<String> args = new ArrayList<String>();
        args.add("-Dapple.awt.application.name=Certificate Wizard");
        args.add("-Dcom.apple.mrj.application.apple.menu.about.name=Certificate Wizard");
        args.add("-Dsun.awt.application.name=Certificate Wizard");
        args.add("-Dsun.awt.X11.XWMClass=CertificateWizard");
        if (isJava9OrNewer()) {
            args.add("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED");
            args.add("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED");
        }
        if (!isMacOs()) {
            return args;
        }
        args.add("-Xdock:name=Certificate Wizard");
        File icon = extractWizardIcon(jar, runtimeDir);
        if (icon != null && icon.isFile()) {
            args.add("-Xdock:icon=" + icon.getAbsolutePath());
        }
        return args;
    }

    static boolean isMacOs() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    static boolean isJava9OrNewer() {
        String version = System.getProperty("java.specification.version", "");
        return version.length() > 0 && !version.startsWith("1.");
    }

    File extractWizardIcon(File jar, File runtimeDir) {
        File iconFile = new File(runtimeDir, "certificate-wizard-icon.png");
        try (JarFile jf = new JarFile(jar)) {
            JarEntry entry = jf.getJarEntry("icon.png");
            if (entry == null) {
                getLog().debug("Certificate wizard jar does not contain icon.png");
                return null;
            }
            try (InputStream in = jf.getInputStream(entry)) {
                FileUtils.copyInputStreamToFile(in, iconFile);
            }
            return iconFile;
        } catch (IOException ex) {
            getLog().debug("Unable to extract certificate wizard dock icon: " + ex.getMessage());
            return null;
        }
    }

    private LoginResult interactiveLogin() {
        if (loginTimeoutSeconds <= 0) {
            return null;
        }
        String key = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + loginTimeoutSeconds * 1000L;
        try {
            String root = normalizeBaseUrl(baseUrl);
            String redirect = root + "/loggedIn.html";
            String loginUrl = root + "/appsec/7.0/set-user?redirect=" + enc(redirect)
                    + "&loginKey=" + enc(key);
            getLog().info("Opening Codename One sign-in for certificate wizard authentication");
            getLog().info(loginUrl);
            openBrowser(loginUrl);
            while (System.currentTimeMillis() < deadline) {
                LoginResult result = pollLogin(root, key);
                if (result != null) {
                    getLog().info("Received Codename One signing API token for " + result.user);
                    return result;
                }
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            getLog().warn("Timed out waiting for Codename One browser login");
        } catch (Exception ex) {
            getLog().warn("Unable to complete browser login: " + ex.getMessage());
        }
        return null;
    }

    private LoginResult pollLogin(String root, String key) throws IOException {
        URL url = new URL(root + "/poll-user?ver=2&loginKey=" + enc(key));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestMethod("GET");
            int code = con.getResponseCode();
            if (code == 404) {
                return null;
            }
            if (code != 200) {
                getLog().debug("Codename One login poll returned HTTP " + code);
                return null;
            }
            String body = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8).trim();
            if (body.length() == 0) {
                return null;
            }
            String[] lines = body.split("\\r?\\n", 2);
            String receivedToken = lines[0].trim();
            if (receivedToken.length() == 0) {
                return null;
            }
            String receivedUser = lines.length > 1 ? lines[1].trim() : "";
            return new LoginResult(receivedToken, receivedUser);
        } finally {
            con.disconnect();
        }
    }

    private void openBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            getLog().warn("Desktop browsing is not available. Open the URL above in a browser to continue.");
        }
    }

    private void writeBinding(File inputFile, File projectDir, File outDir, File outputFile,
                              String effectiveUser, String effectiveToken) throws MojoExecutionException {
        String content = "# Codename One certificate wizard project binding\n"
                + "projectDir=" + projectDir.getAbsolutePath() + "\n"
                + "settings=" + new File(projectDir, "codenameone_settings.properties").getAbsolutePath() + "\n"
                + "outputDir=" + outDir.getAbsolutePath() + "\n"
                + "output=" + outputFile.getAbsolutePath() + "\n"
                + "user=" + effectiveUser + "\n"
                + "token=" + effectiveToken + "\n"
                + "baseUrl=" + baseUrl + "\n";
        try {
            FileUtils.write(inputFile, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write certificatewizard binding", ex);
        }
    }

    private ToolClasspath getCertificateWizardClasspath() throws MojoExecutionException, MojoFailureException {
        Artifact artifact = getArtifact("com.codenameone", "codenameone-certificatewizard");
        if (artifact == null) {
            artifact = repositorySystem.createArtifact(
                    "com.codenameone", "codenameone-certificatewizard", pluginVersion(), "jar");
        }
        ToolClasspath classpath = resolveToolClasspath(artifact);
        if (classpath.primaryJar == null || classpath.files.isEmpty()) {
            throw new MojoFailureException(
                    "Could not resolve the certificate wizard "
                    + "(com.codenameone:codenameone-certificatewizard:" + pluginVersion()
                    + ").\n"
                    + "It is distributed through Maven Central alongside the Codename One plugin.\n"
                    + "To work on the wizard itself, run:\n"
                    + "    cd scripts/certificatewizard && mvn -Pexecutable-jar -pl javase -am package -Dcodename1.platform=javase\n"
                    + "    java -cp \"javase/target/codenameone-certificatewizard-*.jar:javase/target/libs/*\" "
                    + "com.codename1.certificatewizard.CertificateWizardLauncher");
        }
        return classpath;
    }

    private ToolClasspath resolveToolClasspath(Artifact artifact) {
        List<File> files = new ArrayList<File>();
        ArtifactResolutionResult result = repositorySystem.resolve(new ArtifactResolutionRequest()
                .setLocalRepository(localRepository)
                .setRemoteRepositories(new ArrayList<ArtifactRepository>(remoteRepositories))
                .setResolveTransitively(true)
                .setArtifact(artifact));
        File primary = addArtifactFile(files, artifact);
        if (result != null && result.getArtifacts() != null) {
            for (Artifact resolved : result.getArtifacts()) {
                File file = addArtifactFile(files, resolved);
                if (primary == null && resolved != null
                        && "com.codenameone".equals(resolved.getGroupId())
                        && "codenameone-certificatewizard".equals(resolved.getArtifactId())) {
                    primary = file;
                }
            }
        }
        return new ToolClasspath(primary, files);
    }

    private static File addArtifactFile(List<File> files, Artifact artifact) {
        if (artifact == null || artifact.getFile() == null || !"jar".equals(artifact.getType())) {
            return null;
        }
        File file = artifact.getFile().getAbsoluteFile();
        if (!file.exists()) {
            return null;
        }
        if (!files.contains(file)) {
            files.add(file);
        }
        return file;
    }

    private static String joinClasspath(List<File> files) {
        StringBuilder out = new StringBuilder();
        for (File file : files) {
            if (out.length() > 0) {
                out.append(File.pathSeparator);
            }
            out.append(file.getAbsolutePath());
        }
        return out.toString();
    }

    private static final class ToolClasspath {
        final File primaryJar;
        final List<File> files;

        ToolClasspath(File primaryJar, List<File> files) {
            this.primaryJar = primaryJar;
            this.files = files;
        }
    }

    private String pluginVersion() {
        if (pluginArtifacts != null) {
            for (Artifact a : pluginArtifacts) {
                if ("codenameone-maven-plugin".equals(a.getArtifactId())
                        && "com.codenameone".equals(a.getGroupId())) {
                    return a.getVersion();
                }
            }
        }
        return project.getProperties().getProperty("cn1.plugin.version",
                project.getProperties().getProperty("cn1.version", "8.0-SNAPSHOT"));
    }

    private static String firstNonEmpty(String a, String b, String c) {
        if (a != null && a.trim().length() > 0) {
            return a.trim();
        }
        if (b != null && b.trim().length() > 0) {
            return b.trim();
        }
        return c == null ? "" : c;
    }

    private static String firstNonEmpty(String a, String b) {
        return firstNonEmpty(a, b, "");
    }

    private static String normalizeBaseUrl(String url) {
        String out = url == null || url.trim().length() == 0
                ? "https://cloud.codenameone.com" : url.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8");
    }

    static boolean isUsableJwt(String token) {
        long expiresAt = jwtExpiresAt(token);
        return expiresAt > System.currentTimeMillis() + 120000L;
    }

    static long jwtExpiresAt(String token) {
        if (token == null) {
            return -1L;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length < 2) {
            return -1L;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            String payload = new String(decoded, StandardCharsets.UTF_8);
            long exp = numericJsonClaim(payload, "exp");
            return exp <= 0L ? -1L : exp * 1000L;
        } catch (RuntimeException ex) {
            return -1L;
        }
    }

    private static long numericJsonClaim(String json, String name) {
        String quoted = "\"" + name + "\"";
        int idx = json.indexOf(quoted);
        if (idx < 0) {
            return -1L;
        }
        int colon = json.indexOf(':', idx + quoted.length());
        if (colon < 0) {
            return -1L;
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        if (end == start) {
            return -1L;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private static String padBase64(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value);
        for (int i = remainder; i < 4; i++) {
            out.append('=');
        }
        return out.toString();
    }

    private static final class LoginResult {
        final String token;
        final String user;

        LoginResult(String token, String user) {
            this.token = token;
            this.user = user == null ? "" : user;
        }
    }
}
