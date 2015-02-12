/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.animations;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Transition;

/**
 * A Transitions that flips between 2 views
 * 
 * @author Chen, Steve
 */
public class FlipTransition extends Transition {

    private Image sourceBuffer;
    private Image destBuffer;
    // 0 is front, 1.0 is back
    private float flipState = 0f;

    private Motion motion;
    private boolean firstFinished = false;
    private boolean started = false;

    private int bgColor = -1;

    /**
     * Creates  a Flip Transition
     */ 
    public FlipTransition() {
    }

    /**
     * Creates  a Flip Transition
     * 
     * @param bgColor the color to paint in the background when the transition 
     * paints
     */ 
    public FlipTransition(int bgColor) {
        this.bgColor = bgColor;
    }

    @Override
    public void initTransition() {
        Component source = getSource();
        Component destination = getDestination();
        int w = source.getWidth();
        int h = source.getHeight();

        // a transition might occur with illegal source or destination values (common with 
        // improper replace() calls, this may still be valid and shouldn't fail
        if (w <= 0 || h <= 0) {
            return;
        }
        sourceBuffer = createMutableImage(source.getWidth(), source.getHeight());
        paint(sourceBuffer.getGraphics(), source, -source.getAbsoluteX(), -source.getAbsoluteY());

        destBuffer = createMutableImage(destination.getWidth(), destination.getHeight());
        paint(destBuffer.getGraphics(), destination, -destination.getAbsoluteX(), -destination.getAbsoluteY());

        if (source instanceof Form) {
            bgColor = 0;
        }
        flipState = 0f;
        motion = Motion.createLinearMotion(0, 180, 500);
        motion.start();

    }

    @Override
    public boolean animate() {
        int val = motion.getValue();
        double valInRadians = Math.PI / 180f * (double) val;
        double projectedPos = Math.cos(valInRadians);

        flipState = (float) ((-projectedPos) / 2.0 + 0.5);
        if (motion.isFinished()) {
            return false;
        }
        return true;
    }

    @Override
    public void paint(Graphics g) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        int x = getSource().getAbsoluteX();
        int y = getSource().getAbsoluteY();
        int w = getSource().getWidth();
        int h = getSource().getHeight();
        g.setClip(x, y, w, h);
        if (bgColor >= 0) {
            int c = g.getColor();
            g.setColor(0);
            g.fillRect(x, y, w, h);
            g.setColor(c);
        } else {
            getSource().paintBackgrounds(g);
        }

        if (flipState < 0.5) {
            int frontX = x + (int) (flipState * (float) w);
            int frontWidth = (int) ((float) w * (1.0 - flipState * 2.0));
            g.drawImage(sourceBuffer, frontX, y, frontWidth, h);
        } else {
            double backState = 1.0 - flipState;
            int backX = x + (int) (backState * (float) w);
            int backWidth = (int) ((float) w * (1.0 - backState * 2.0));
            g.drawImage(destBuffer, backX, y, backWidth, h);
        }
        g.setClip(cx, cy, cw, ch);
    }

    private Image createMutableImage(int w, int h) {
        Display d = Display.getInstance();
        return Image.createImage(Math.min(d.getDisplayWidth(), w), Math.min(d.getDisplayHeight(), h));
    }

    private void paint(Graphics g, Component cmp, int x, int y) {
        paint(g, cmp, x, y, false);
    }

    private void paint(Graphics g, Component cmp, int x, int y, boolean background) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        g.translate(x, y);
        cmp.paintComponent(g, background);
        g.translate(-x, -y);

        g.setClip(cx, cy, cw, ch);
    }

    public void cleanup() {
        sourceBuffer = null;
        destBuffer = null;
    }

}
