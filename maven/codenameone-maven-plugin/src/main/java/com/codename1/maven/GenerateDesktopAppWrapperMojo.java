package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.codename1.maven.PathUtil.path;

/**
 * Generates The SEWrapper, or the main class for a JavaSE desktop app.  This is used by the javase module
 * of the cn1app-archetype.
 */
@Mojo(name="generate-desktop-app-wrapper")
public class GenerateDesktopAppWrapperMojo extends AbstractCN1Mojo {
    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        String iconPath = properties.getProperty("codename1.icon");
        File iconFile = new File(iconPath);
        if (!iconFile.isAbsolute()) {
            iconFile = new File(getCN1ProjectDir(), iconFile.getPath());
        }
        if (!iconFile.exists()) {
            getLog().warn("Icon file "+iconFile+" not found.  Skipping desktop appp icon generation.");
            return;
        }
        File outputDir = new File(project.getBuild().getOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        try (FileInputStream fis = new FileInputStream(iconFile)) {
            getLog().debug("Creating Application Icons");
            BufferedImage iconImage = ImageIO.read(fis);
            createIconFile(new File(outputDir, "applicationIconImage_16x16.png"), iconImage, 16, 16);
            createIconFile(new File(outputDir, "applicationIconImage_20x20.png"), iconImage, 20, 20);
            createIconFile(new File(outputDir, "applicationIconImage_32x32.png"), iconImage, 32, 32);
            createIconFile(new File(outputDir, "applicationIconImage_40x40.png"), iconImage, 40, 40);
            createIconFile(new File(outputDir, "applicationIconImage_64x64.png"), iconImage, 64, 64);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to generate icons", ex);
        }

        // Now get the wrapper
        File wrapperSources = new File(project.getBasedir(), path("src", "desktop", "java"));
        if (wrapperSources.exists()) {
            project.addCompileSourceRoot(wrapperSources.getAbsolutePath());
        }





    }

    private void createIconFile(File f, BufferedImage icon, int w, int h) throws IOException {
        ImageIO.write(getScaledInstance(icon, w, h), "png", f);
    }

    private BufferedImage getScaledInstance(BufferedImage img,
                                              int targetWidth,
                                              int targetHeight) {
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        w = img.getWidth();
        h = img.getHeight();

        if (w < targetWidth && h < targetHeight) {
            BufferedImage b = new BufferedImage(targetWidth, targetHeight, img.getType());
            Graphics2D g2d = b.createGraphics();
            g2d.drawImage(img, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();
            return b;
        }

        do {
            if (w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

}
