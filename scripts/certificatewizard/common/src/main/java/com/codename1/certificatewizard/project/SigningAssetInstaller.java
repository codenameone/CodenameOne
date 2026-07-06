package com.codename1.certificatewizard.project;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public final class SigningAssetInstaller {
    private SigningAssetInstaller() {
    }

    public static void applyDebugCertificate(String settingsPath, String p12Path, String password,
                                             String profilePath) throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("codename1.ios.debug.certificate", p12Path == null ? "" : p12Path);
        updates.put("codename1.ios.debug.certificatePassword", password == null ? "" : password);
        updates.put("codename1.ios.debug.provision", profilePath == null ? "" : profilePath);
        update(settingsPath, updates);
    }

    public static void applyReleaseCertificate(String settingsPath, String p12Path, String password,
                                               String profilePath) throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("codename1.ios.release.certificate", p12Path == null ? "" : p12Path);
        updates.put("codename1.ios.release.certificatePassword", password == null ? "" : password);
        updates.put("codename1.ios.release.provision", profilePath == null ? "" : profilePath);
        updates.put("codename1.ios.certificate", p12Path == null ? "" : p12Path);
        updates.put("codename1.ios.certificatePassword", password == null ? "" : password);
        updates.put("codename1.ios.provision", profilePath == null ? "" : profilePath);
        update(settingsPath, updates);
    }

    public static void applyAndroidKeystore(String settingsPath, String keystorePath, String alias,
                                            String password) throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("codename1.android.keystore", keystorePath == null ? "" : keystorePath);
        updates.put("codename1.android.keystoreAlias", alias == null ? "" : alias);
        updates.put("codename1.android.keystorePassword", password == null ? "" : password);
        update(settingsPath, updates);
    }

    public static void applyMacCertificate(String settingsPath, String p12Path, String password,
                                           String profilePath, String distribution) throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("codename1.mac.certificate", p12Path == null ? "" : p12Path);
        updates.put("codename1.mac.certificatePassword", password == null ? "" : password);
        updates.put("codename1.mac.provision", profilePath == null ? "" : profilePath);
        updates.put("codename1.arg.macNative.enabled", "true");
        if (distribution != null && distribution.length() > 0) {
            updates.put("codename1.arg.macNative.distribution", distribution);
        }
        update(settingsPath, updates);
    }

    static void update(String settingsPath, Map<String, String> updates) throws IOException {
        String text = read(settingsPath);
        String[] lines = text.replace("\r\n", "\n").split("\n");
        StringBuilder out = new StringBuilder(text.length() + 160);
        Map<String, String> remaining = new HashMap<String, String>(updates);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && !trimmed.startsWith("#")) {
                String key = trimmed.substring(0, eq).trim();
                if (remaining.containsKey(key)) {
                    out.append(key).append('=').append(escape(remaining.get(key))).append('\n');
                    remaining.remove(key);
                    continue;
                }
            }
            out.append(line).append('\n');
        }
        for (String key : remaining.keySet()) {
            out.append(key).append('=').append(escape(remaining.get(key))).append('\n');
        }
        write(settingsPath, out.toString());
    }

    static String read(String settingsPath) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String url = ProjectIO.fsUrl(settingsPath);
        if (fs.exists(url)) {
            InputStream in = null;
            try {
                in = fs.openInputStream(url);
                return Util.readToString(in, "UTF-8");
            } finally {
                Util.cleanup(in);
            }
        }
        return "";
    }

    static void write(String settingsPath, String text) throws IOException {
        OutputStream out = null;
        try {
            out = FileSystemStorage.getInstance().openOutputStream(ProjectIO.fsUrl(settingsPath));
            out.write(text.getBytes("UTF-8"));
            out.flush();
        } finally {
            Util.cleanup(out);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "");
    }
}
