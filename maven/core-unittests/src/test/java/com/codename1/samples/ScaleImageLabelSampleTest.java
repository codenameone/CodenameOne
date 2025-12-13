package com.codename1.samples;

import com.codename1.components.ScaleImageLabel;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.*;

public class ScaleImageLabelSampleTest extends UITestBase {

    private int pressCount;

    @FormTest
    public void testScaleImageLabelUpdatesIcon() throws Exception {
        pressCount = 0;
        Form form = new Form("Hi World", BoxLayout.y());
        ScaleImageLabel scaleLabel = new ScaleImageLabel();
        scaleLabel.setPreferredSize(new Dimension(140, 60));

        Label title = new Label("Hi World");
        Button updateButton = new Button("Update Label");
        updateButton.addActionListener(evt -> {
            pressCount++;
            String message = "Pressed " + pressCount + " times";
            try {
                loadTextImage(scaleLabel, null, message, scaleLabel.getWidth(), scaleLabel.getHeight());
            } catch (Exception ex) {
                fail("Unexpected exception while updating icon: " + ex.getMessage());
            }
        });

        form.add(title);
        form.add(FlowLayout.encloseIn(scaleLabel));
        form.add(updateButton);
        form.show();
        form.revalidateWithAnimationSafety();
        flushSerialCalls();

        assertNull(scaleLabel.getIcon(), "Icon should start empty");

        TestCodenameOneImplementation impl = implementation;
        impl.tapComponent(updateButton);
        flushSerialCalls();

        Image firstIcon = (Image) scaleLabel.getIcon();
        assertNotNull(firstIcon, "Tapping the button should apply an icon");
        assertEquals(140, firstIcon.getWidth(), "Icon width should match preferred width");
        assertEquals(60, firstIcon.getHeight(), "Icon height should match preferred height");

        impl.tapComponent(updateButton);
        flushSerialCalls();

        Image secondIcon = (Image) scaleLabel.getIcon();
        assertNotSame(firstIcon, secondIcon, "Updating the label should replace the icon");
        assertEquals(2, pressCount, "Press count should track button taps");

        Component parent = scaleLabel.getParent();
        assertNotNull(parent, "ScaleImageLabel should have a parent after layout");
    }

    private void loadTextImage(ScaleImageLabel ddbox, Image image, String message, int width, int height) throws Exception {
        if (width <= 0) {
            width = ddbox.getPreferredW();
        }
        if (height <= 0) {
            height = ddbox.getPreferredH();
        }
        Font font = ddbox.getSelectedStyle().getFont();
        int w = font.stringWidth(message);
        int h = font.getHeight();
        Graphics graphics;
        if (width <= w) {
            if (height <= h) {
                if (image == null) {
                    image = Image.createImage(w, h);
                } else {
                    height = image.getHeight();
                    width = image.getWidth();
                }
                graphics = image.getGraphics();
                graphics.setFont(font);
                graphics.setColor(ddbox.getSelectedStyle().getFgColor());
                graphics.drawString(message, 0, 0);
            } else {
                if (image == null) {
                    image = Image.createImage(w, height);
                } else {
                    height = image.getHeight();
                    width = image.getWidth();
                }
                graphics = image.getGraphics();
                graphics.setFont(font);
                graphics.setColor(255);
                graphics.drawString(message, 0, (height / 2) - (h / 2));
            }
        } else if (height <= h) {
            if (image == null) {
                image = Image.createImage(width, h);
            } else {
                height = image.getHeight();
                width = image.getWidth();
            }
            graphics = image.getGraphics();
            graphics.setFont(font);
            graphics.setColor(ddbox.getSelectedStyle().getFgColor());
            graphics.drawString(message, (width / 2) - (w / 2), 0);
        } else {
            if (image == null) {
                image = Image.createImage(width, height);
            } else {
                height = image.getHeight();
                width = image.getWidth();
            }
            graphics = image.getGraphics();
            graphics.setFont(font);
            graphics.setColor(ddbox.getSelectedStyle().getFgColor());
            graphics.drawString(message, (width / 2) - (w / 2), (height / 2) - (h / 2));
        }
        ddbox.setIcon(image);
        Container parent = ddbox.getParent();
        if (parent != null) {
            parent.revalidateWithAnimationSafety();
        }
    }
}
