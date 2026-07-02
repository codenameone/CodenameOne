package com.codenameone.developerguide;

/**
 * Stable mapping between a runnable guide demo and the image it documents.
 */
public final class GuideScreenshot {
    private final String id;
    private final Demo demo;
    private final String fileName;

    public GuideScreenshot(String id, Demo demo, String fileName) {
        this.id = id;
        this.demo = demo;
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public Demo getDemo() {
        return demo;
    }

    public String getFileName() {
        return fileName;
    }
}
