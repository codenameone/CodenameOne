package com.codenameone.developerguide;

import com.codename1.ui.Display;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * JavaSE desktop entry point used by docs automation to regenerate guide screenshots.
 */
public final class GuideScreenshotDesktopStub implements Runnable {
    private static final String APP_TITLE = "Guide Screenshots";
    private static final String APP_NAME = "DemoCodeScreenshots";
    private static final String APP_VERSION = "1.0";
    private static final int APP_WIDTH = 800;
    private static final int APP_HEIGHT = 600;
    private static final boolean APP_RESIZEABLE = false;
    private static final boolean APP_FULLSCREEN = false;

    private static JFrame frame;

    public static void main(String[] args) {
        System.out.println("Starting guide screenshot desktop stub");
        System.setProperty("java.awt.headless", "false");
        System.setProperty("guide.screenshot.output", args.length > 0 ? args[0] : "target/generated-guide-screenshots");

        frame = new JFrame(APP_TITLE);
        Display.init(frame.getContentPane());
        Display.getInstance().setProperty("AppName", APP_NAME);
        Display.getInstance().setProperty("AppVersion", APP_VERSION);
        Display.getInstance().setProperty("Platform", System.getProperty("os.name"));
        Display.getInstance().setProperty("OSVer", System.getProperty("os.version"));

        SwingUtilities.invokeLater(new GuideScreenshotDesktopStub());
    }

    @Override
    public void run() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (APP_FULLSCREEN && gd.isFullScreenSupported()) {
            frame.setResizable(false);
            frame.setUndecorated(true);
            gd.setFullScreenWindow(frame);
        } else {
            frame.setLocationByPlatform(true);
            frame.setResizable(APP_RESIZEABLE);
            frame.getContentPane().setPreferredSize(new java.awt.Dimension(APP_WIDTH, APP_HEIGHT));
            frame.getContentPane().setMinimumSize(new java.awt.Dimension(APP_WIDTH, APP_HEIGHT));
            frame.getContentPane().setMaximumSize(new java.awt.Dimension(APP_WIDTH, APP_HEIGHT));
            frame.pack();
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    GuideScreenshotGeneratorStub.generateInto(new File(System.getProperty("guide.screenshot.output")));
                } catch (Exception err) {
                    err.printStackTrace();
                    System.exit(1);
                }
                Display.getInstance().exitApplication();
            }
        });
    }
}
