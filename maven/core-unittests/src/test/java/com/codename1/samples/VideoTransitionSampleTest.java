package com.codename1.samples;

import com.codename1.components.MediaPlayer;
import com.codename1.ui.*;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.util.AsyncResource;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class VideoTransitionSampleTest extends UITestBase {

    private Image image;
    private boolean mediaLoaded = false;
    private Form hi;
    private Button swap;
    private Container theCnt;
    private Label imageLabel;
    private MediaPlayer mediaPlayer;

    @FormTest
    public void testVideoTransition() {
        hi = new Form("Hi World", new BorderLayout());
        swap = new Button("Swap");
        theCnt = new Container(new LayeredLayout());
        imageLabel = new Label();
        imageLabel.getAllStyles().setBgColor(0);
        imageLabel.getAllStyles().setBgTransparency(255);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.getAllStyles().setBgColor(0);
        mediaPlayer.getAllStyles().setBgTransparency(255);

        swap.addActionListener(e -> {
            if (image == null) {
                // Mock image download
                image = Image.createImage(100, 100, 0xff0000);
                imageLabel.setIcon(image.scaled(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight()));
            }
            if (!mediaLoaded) {
                // In a real app we would set data source here.
                // In test, we skip setting real media source to avoid network/codec issues.
                // try {
                //    mediaPlayer.setDataSource("...", null);
                // } catch (IOException ex) {}
                mediaLoaded = true;
            }

            if (theCnt.getComponentCount() == 0) {
                theCnt.add(mediaPlayer);
                if (mediaPlayer.getMedia() != null) {
                    mediaPlayer.getMedia().play();
                }
                hi.revalidate();
            } else if (theCnt.contains(mediaPlayer) && !theCnt.contains(imageLabel)) {
                imageLabel.getAllStyles().setOpacity(0);
                imageLabel.getAllStyles().setBgTransparency(0);
                theCnt.add(imageLabel);
                theCnt.revalidate();
                // Speed up animation for test
                fadeIn(imageLabel, 50).ready(res -> {
                    if (mediaPlayer.getMedia() != null) {
                        mediaPlayer.getMedia().pause();
                    }
                    mediaPlayer.remove();
                    hi.revalidate();
                });

            } else if (theCnt.contains(imageLabel) && !theCnt.contains(mediaPlayer)) {
                theCnt.addComponent(0, mediaPlayer);
                if (mediaPlayer.getMedia() != null) {
                    mediaPlayer.getMedia().play();
                }
                // Speed up animation for test
                fadeOut(imageLabel, 50).ready(res -> {
                    imageLabel.remove();
                    theCnt.revalidate();
                });
            }
        });

        hi.add(BorderLayout.NORTH, swap);
        hi.add(BorderLayout.CENTER, theCnt);
        hi.show();

        waitForForm(hi);

        // Test Step 1: Initial State - empty
        assertEquals(0, theCnt.getComponentCount(), "Container should be initially empty");

        // Test Step 2: First Click - Show MediaPlayer
        clickButton(swap);
        assertEquals(1, theCnt.getComponentCount(), "Container should have 1 component (MediaPlayer)");
        assertTrue(theCnt.contains(mediaPlayer), "Container should contain MediaPlayer");

        // Test Step 3: Second Click - Transition to Image
        clickButton(swap);

        // At this point, both might be present during animation or imageLabel added
        assertTrue(theCnt.contains(imageLabel), "Container should contain imageLabel during transition");

        // Wait for animation to finish
        waitForAnimation(300);

        assertTrue(theCnt.contains(imageLabel), "Container should contain imageLabel after transition");
        assertFalse(theCnt.contains(mediaPlayer), "Container should NOT contain MediaPlayer after transition");
        assertEquals(255, imageLabel.getStyle().getOpacity(), "ImageLabel should be fully opaque");

        // Test Step 4: Third Click - Transition back to MediaPlayer
        clickButton(swap);

        // Wait for animation
        waitForAnimation(300);

        assertTrue(theCnt.contains(mediaPlayer), "Container should contain MediaPlayer after switching back");
        assertFalse(theCnt.contains(imageLabel), "Container should NOT contain imageLabel after switching back");
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
                com.codename1.ui.DisplayTest.flushEdt();
            } catch (InterruptedException e) {
            }
        }
    }

    private void clickButton(Button b) {
        // Use public method from TestCodenameOneImplementation
        implementation.dispatchPointerPress(b.getAbsoluteX() + b.getWidth() / 2, b.getAbsoluteY() + b.getHeight() / 2);
        implementation.dispatchPointerRelease(b.getAbsoluteX() + b.getWidth() / 2, b.getAbsoluteY() + b.getHeight() / 2);
        com.codename1.ui.DisplayTest.flushEdt();
    }

    private void waitForAnimation(int timeout) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
             com.codename1.ui.DisplayTest.flushEdt();
             try {
                Thread.sleep(50);
             } catch (InterruptedException e) {}
        }
    }

    private AsyncResource<Component> fadeIn(Component cmp, int duration) {
        AsyncResource<Component> out = new AsyncResource<Component>();
        Motion m = Motion.createEaseInMotion(cmp.getStyle().getOpacity(), 255, duration);

        final java.util.Timer t = new java.util.Timer();
        t.schedule(new TimerTask() {
            public void run() {
                 Display.getInstance().callSerially(() -> {
                    int currOpacity = cmp.getStyle().getOpacity();
                    int newOpacity = m.getValue();
                    if (currOpacity != newOpacity) {
                        cmp.getStyle().setOpacity(newOpacity);
                        cmp.getStyle().setBgTransparency(newOpacity);
                        cmp.repaint();
                    }
                    if (m.isFinished()) {
                        t.cancel();
                        out.complete(cmp);
                    }
                 });
            }
        }, 0, 20);

        m.start();
        return out;
    }

    private AsyncResource<Component> fadeOut(Component cmp, int duration) {
        AsyncResource<Component> out = new AsyncResource<Component>();
        Motion m = Motion.createEaseInMotion(cmp.getStyle().getOpacity(), 0, duration);
        final java.util.Timer t = new java.util.Timer();
        t.schedule(new TimerTask() {
            public void run() {
                Display.getInstance().callSerially(() -> {
                    int currOpacity = cmp.getStyle().getOpacity();
                    int newOpacity = m.getValue();
                    if (currOpacity != newOpacity) {
                        cmp.getStyle().setBgTransparency(newOpacity);
                        cmp.getStyle().setOpacity(newOpacity);
                        cmp.repaint();
                    }
                    if (m.isFinished()) {
                        t.cancel();
                        out.complete(cmp);
                    }
                });
            }
        }, 0, 20);
        m.start();
        return out;
    }
}
