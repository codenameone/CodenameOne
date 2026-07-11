package com.codenameone.developerguide;

import com.codenameone.developerguide.screenshots.PreAdvancedThemingScreenshots;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * JavaSE entry point used by docs automation to regenerate guide screenshots.
 */
public final class GuideScreenshotGeneratorStub {
    private GuideScreenshotGeneratorStub() {
    }

    public static void main(String[] args) throws Exception {
        generateInto(new File(args.length > 0 ? args[0] : "target/generated-guide-screenshots"));
    }

    public static void generateInto(File outputDirectory) throws IOException {
        System.out.println("Generating guide screenshots into: " + outputDirectory.getAbsolutePath());
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IOException("Unable to create screenshot output directory: " + outputDirectory);
        }
        PreAdvancedThemingScreenshots.generate(new PreAdvancedThemingScreenshots.ScreenshotSink() {
            @Override
            public OutputStream open(String fileName) throws IOException {
                System.out.println("Generating guide screenshot: " + fileName);
                return new FileOutputStream(new File(outputDirectory, fileName));
            }
        });
    }
}
