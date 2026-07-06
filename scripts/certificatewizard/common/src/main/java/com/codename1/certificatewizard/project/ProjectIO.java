package com.codename1.certificatewizard.project;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;

public final class ProjectIO {
    public static final String INPUT_PROPERTY = "certificatewizard.input";

    private ProjectIO() {
    }

    public static ProjectBinding loadBinding() {
        String path = System.getProperty(INPUT_PROPERTY);
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        InputStream in = null;
        try {
            String url = fsUrl(path);
            if (!fs.exists(url)) {
                return null;
            }
            in = fs.openInputStream(url);
            ProjectBinding b = ProjectBinding.parse(Util.readToString(in, "UTF-8"));
            return b.isValid() ? b : null;
        } catch (IOException ex) {
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    public static String fsUrl(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("file://") || path.indexOf("://") > 0) {
            return path;
        }
        return "file://" + path;
    }
}
