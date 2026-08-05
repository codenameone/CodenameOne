package com.codename1.guibuilder.project;

public final class ProjectBinding {
    private String projectDir;
    private String guiDir;
    private String sourceDir;
    private String cssFile;
    private String initialForm;

    public String projectDir() { return projectDir; }
    public String guiDir() { return guiDir; }
    public String sourceDir() { return sourceDir; }
    public String cssFile() { return cssFile; }
    public String initialForm() { return initialForm; }
    public boolean isValid() { return projectDir != null && guiDir != null; }

    public static ProjectBinding parse(String content) {
        ProjectBinding binding = new ProjectBinding();
        if (content == null) return binding;
        for (String line : content.replace("\r\n", "\n").split("\n")) {
            String value = line.trim();
            if (value.length() == 0 || value.startsWith("#")) continue;
            int split = value.indexOf('=');
            if (split < 1) continue;
            String key = value.substring(0, split).trim();
            String field = value.substring(split + 1).trim();
            switch (key) {
                case "projectDir" -> binding.projectDir = field;
                case "guiDir" -> binding.guiDir = field;
                case "sourceDir" -> binding.sourceDir = field;
                case "cssFile" -> binding.cssFile = field;
                case "initialForm" -> binding.initialForm = field;
                default -> { }
            }
        }
        return binding;
    }
}
