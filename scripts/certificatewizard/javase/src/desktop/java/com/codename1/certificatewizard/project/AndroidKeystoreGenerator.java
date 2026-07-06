package com.codename1.certificatewizard.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AndroidKeystoreGenerator implements AndroidKeystoreProvider {
    public AndroidKeystoreGenerator() {
    }

    public String defaultKeystore(String projectDir) {
        return new File(new File(projectDir, "androidCerts"), "KeyChain.ks").getAbsolutePath();
    }

    public void generate(String keystorePath, String alias, String password, String distinguishedName)
            throws IOException, InterruptedException {
        File keystore = new File(keystorePath);
        if (keystore.exists()) {
            throw new IOException("Keystore already exists: " + keystore.getAbsolutePath());
        }
        if (alias == null || alias.trim().length() == 0) {
            throw new IOException("Missing key alias");
        }
        if (password == null || password.length() < 6) {
            throw new IOException("Android keystore password must be at least 6 characters");
        }
        File parent = keystore.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Failed to create " + parent.getAbsolutePath());
        }
        String dname = distinguishedName == null || distinguishedName.trim().length() == 0
                ? "CN=Codename One, OU=Development, O=Codename One, L=Unknown, ST=Unknown, C=US"
                : distinguishedName.trim();
        List<String> command = new ArrayList<String>();
        command.add(keytool().getAbsolutePath());
        command.add("-genkeypair");
        command.add("-v");
        command.add("-keystore");
        command.add(keystore.getAbsolutePath());
        command.add("-storepass");
        command.add(password);
        command.add("-keypass");
        command.add(password);
        command.add("-alias");
        command.add(alias.trim());
        command.add("-keyalg");
        command.add("RSA");
        command.add("-keysize");
        command.add("2048");
        command.add("-validity");
        command.add("10000");
        command.add("-dname");
        command.add(dname);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        int code = process.waitFor();
        if (code != 0) {
            throw new IOException("keytool failed with exit code " + code);
        }
    }

    static File keytool() throws IOException {
        String executable = File.separatorChar == '\\' ? "keytool.exe" : "keytool";
        File javaHomeTool = new File(new File(System.getProperty("java.home"), "bin"), executable);
        if (javaHomeTool.isFile()) {
            return javaHomeTool;
        }
        String path = System.getenv("PATH");
        if (path != null) {
            String[] entries = path.split(File.pathSeparator);
            for (int i = 0; i < entries.length; i++) {
                File candidate = new File(entries[i], executable);
                if (candidate.isFile()) {
                    return candidate;
                }
            }
        }
        throw new IOException("Unable to locate keytool. Install a JDK and run the wizard with that JDK.");
    }
}
