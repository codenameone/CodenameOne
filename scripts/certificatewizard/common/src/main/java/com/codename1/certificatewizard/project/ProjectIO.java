package com.codename1.certificatewizard.project;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.JSONParser;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public final class ProjectIO {
    public static final String INPUT_PROPERTY = "certificatewizard.input";

    private ProjectIO() {
    }

    /// Reads `<projectDir>/src/main/resources/surfaces.json` when present. Returns
    /// `null` when the project does not use External Surfaces (no manifest file);
    /// a non-null result means the manifest exists (the `appGroup` value may still
    /// be null when the manifest does not declare one).
    public static SurfacesManifest readSurfacesManifest(String projectDir) {
        if (projectDir == null || projectDir.trim().isEmpty()) {
            return null;
        }
        String path = projectDir + "/src/main/resources/surfaces.json";
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String url = fsUrl(path);
        if (!fs.exists(url)) {
            return null;
        }
        InputStream in = null;
        try {
            in = fs.openInputStream(url);
            Map<String, Object> json = new JSONParser().parseJSON(new InputStreamReader(in, "UTF-8"));
            Object appGroup = json == null ? null : json.get("appGroup");
            return new SurfacesManifest(appGroup == null ? null : appGroup.toString());
        } catch (IOException ex) {
            return new SurfacesManifest(null);
        } finally {
            Util.cleanup(in);
        }
    }

    public static final class SurfacesManifest {
        private final String appGroup;

        SurfacesManifest(String appGroup) {
            this.appGroup = appGroup == null || appGroup.trim().isEmpty() ? null : appGroup.trim();
        }

        public String appGroup() {
            return appGroup;
        }
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
