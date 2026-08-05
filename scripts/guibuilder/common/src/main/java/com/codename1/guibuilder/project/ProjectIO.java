package com.codename1.guibuilder.project;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProjectIO {
    public static final String INPUT_PROPERTY = "guibuilder.input";

    private ProjectIO() { }

    public static ProjectBinding loadBinding() {
        String path = System.getProperty(INPUT_PROPERTY);
        if (path == null || path.trim().length() == 0) return null;
        try {
            String content = read(path);
            ProjectBinding binding = ProjectBinding.parse(content);
            return binding.isValid() ? binding : null;
        } catch (IOException ex) {
            return null;
        }
    }

    public static List<String> findGuiFiles(String guiDir) {
        List<String> files = new ArrayList<>();
        collect(fsUrl(guiDir), files);
        Collections.sort(files);
        return files;
    }

    private static void collect(String dir, List<String> files) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if (dir == null || !fs.exists(dir) || !fs.isDirectory(dir)) return;
        try {
            for (String child : fs.listFiles(dir)) {
                String path = dir + (dir.endsWith("/") ? "" : "/") + child;
                if (fs.isDirectory(path)) collect(path, files);
                else if (path.endsWith(".gui")) files.add(path);
            }
        } catch (IOException ignored) { }
    }

    public static String read(String path) throws IOException {
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(fsUrl(path));
            return Util.readToString(in, "UTF-8");
        } finally {
            Util.cleanup(in);
        }
    }

    public static boolean exists(String path) {
        return path != null && FileSystemStorage.getInstance().exists(fsUrl(path));
    }

    public static void write(String path, String content) throws IOException {
        OutputStream out = null;
        try {
            String url = fsUrl(path);
            ensureParent(url);
            out = FileSystemStorage.getInstance().openOutputStream(url);
            out.write(content.getBytes("UTF-8"));
        } finally {
            Util.cleanup(out);
        }
    }

    private static void ensureParent(String path) {
        int slash = path.lastIndexOf('/');
        if (slash <= "file://".length()) return;
        String parent = path.substring(0, slash);
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if (fs.exists(parent)) return;
        ensureParent(parent);
        fs.mkdir(parent);
    }

    public static String fsUrl(String path) {
        if (path == null || path.startsWith("file://") || path.indexOf("://") > 0) return path;
        return "file://" + path;
    }
}
