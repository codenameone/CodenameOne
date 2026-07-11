package com.codename1.settings.project;

public final class ProjectBinding {
    private String projectDir;
    private String settings;
    private String pom;
    private String multimoduleRoot;
    private String buildHintsDoc;

    public String projectDir() {
        return projectDir;
    }

    public String settings() {
        return settings;
    }

    public String pom() {
        return pom;
    }

    public String multimoduleRoot() {
        return multimoduleRoot;
    }

    public String buildHintsDoc() {
        return buildHintsDoc;
    }

    public boolean isValid() {
        return settings != null && settings.length() > 0;
    }

    public static ProjectBinding parse(String content) {
        ProjectBinding b = new ProjectBinding();
        if (content == null) {
            return b;
        }
        String[] lines = content.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String val = trimmed.substring(eq + 1).trim();
            switch (key) {
                case "projectDir" -> b.projectDir = val;
                case "settings" -> b.settings = val;
                case "pom" -> b.pom = val;
                case "multimoduleRoot" -> b.multimoduleRoot = val;
                case "buildHintsDoc" -> b.buildHintsDoc = val;
                default -> {
                }
            }
        }
        return b;
    }
}
