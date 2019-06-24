/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.ui;

import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BoxLayout;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A view to graph the performance of the EDT.  This will display a bar-chart showing 
 * the time taken to render the UI on the EDT.  To receive events, it must be registered
 * with the EDT via the {@link Display#setEDTProfiler(com.codename1.ui.EDTProfiler) } method.
 * @author shannah
 * @since 7.0
 */
public class EDTProfilerView extends Container implements EDTProfiler {

    private boolean paintSlices = true;
    private int frameBarWidth = Math.min(1, CN.convertToPixels(0.25f));
    private int framesPerSlice = 30;
    private int sliceBarWidth = CN.convertToPixels(1.5f);
    private int numSlicesToRetain = Math.max(CN.getDisplayHeight(), CN.getDisplayWidth()) / sliceBarWidth;
    private int numFramesToRetain = Math.max(CN.getDisplayHeight(), CN.getDisplayWidth()) / frameBarWidth;
    private int yAxisMax = 100;
    
    private FrameBar currentFrame;
    private LinkedList<FrameBar> bars = new LinkedList<FrameBar>();
    private LinkedList<SliceBar> slices = new LinkedList<SliceBar>();
    private Label frameLabel, paintDirtyLabel, repaintAnimationsLabel, processSerialCallsLabel;
    private Image buffer;
    private boolean useBuffer = false;
    private long lastRepaint;
    
    /**
     * Creates a new Profiler view.
     */
    public EDTProfilerView() {
        setGrabsPointerEvents(true);
        initUI();
    }
    
    
    
    private void initUI() {
        frameLabel = new Label("Frame");
        frameLabel.getStyle().setFgColor(0xffff00);
        paintDirtyLabel = new Label("paintDirty()");
        paintDirtyLabel.getStyle().setFgColor(0x0000ff);
        repaintAnimationsLabel = new Label("repaintAnimations()");
        repaintAnimationsLabel.getStyle().setFgColor(0x00ff00);
        processSerialCallsLabel = new Label("processSerialCalls()");
        processSerialCallsLabel.getStyle().setFgColor(0xff0000);
        
        $(frameLabel, paintDirtyLabel, repaintAnimationsLabel, processSerialCallsLabel)
                .selectAllStyles()
                .setFontSizeMillimeters(1.5f);
            
        setLayout(BoxLayout.y());
        addAll(frameLabel, paintDirtyLabel, repaintAnimationsLabel, processSerialCallsLabel);
        
    }

    @Override
    public boolean animate() {
        long now = System.currentTimeMillis();
        boolean out = now - lastRepaint > 1000;
        lastRepaint = now;
        return out;
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(CN.getDisplayWidth(), CN.getDisplayHeight()/3);
    }
    
    private class SliceBar {
        private int numFrames;
        private double avgFrame, avgPaintDirty, avgRepaintAnimations, avgProcessSerialCalls;

        void addFrame(long frame, long paintDirty, long repaintAnimations, long processSerialCalls) {
            avgFrame = (avgFrame * numFrames + frame) / (numFrames+1);
            avgPaintDirty = (avgPaintDirty * numFrames + paintDirty) / (numFrames+1);
            avgRepaintAnimations = (avgRepaintAnimations * numFrames + repaintAnimations) / (numFrames+1);
            avgProcessSerialCalls = (avgProcessSerialCalls * numFrames + processSerialCalls) / (numFrames+1);
            numFrames++;
        }

        private void drawBarInRect(Graphics g, int x, int y, int w, int h) {
            //g.setColor(0xffff00);
            //g.fillRect(x, y+h - frameBarHeight(h), w, frameBarHeight(h));
            int y0 = y + h;
            g.setColor(0x0000ff);
            g.fillRect(x, y0 - paintDirtyBarHeight(h), w, paintDirtyBarHeight(h));
            y0 -= paintDirtyBarHeight(h);
            g.setColor(0x00ff00);
            g.fillRect(x, y0 - repaintAnimationsBarHeight(h), w, repaintAnimationsBarHeight(h));
            y0 -= repaintAnimationsBarHeight(h);
            g.setColor(0xff0000);
            g.fillRect(x, y0 - processSerialCallsBarHeight(h), w, processSerialCallsBarHeight(h));
            y0 -= processSerialCallsBarHeight(h);
            g.setColor(0xffff00);

            int frameBarHeight = frameBarHeight(h) - processSerialCallsBarHeight(h) - paintDirtyBarHeight(h)
                    - repaintAnimationsBarHeight(h);

            g.fillRect(x, y0 - frameBarHeight, w, frameBarHeight);


        }

        private void appendRect(GeneralPath dest, Rectangle rect) {
            dest.moveTo(rect.getX(), rect.getY());
            dest.lineTo(rect.getX() + rect.getWidth(), rect.getY());
            dest.lineTo(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight());
            dest.lineTo(rect.getX(), rect.getY() + rect.getHeight());
            dest.closePath();
        }

        public void addBarToPath(GeneralPath paintDirtyPath, GeneralPath repaintAnimationsPath, GeneralPath processSerialCallsPath, GeneralPath framePath, int x, int y, int w, int h) {
            if (x < 0) {
                return;
            }
            int y0 = y + h;
            //g.setColor(0x0000ff);
            appendRect(paintDirtyPath, new Rectangle(x, y0 - paintDirtyBarHeight(h), w, paintDirtyBarHeight(h)));
            y0 -= paintDirtyBarHeight(h);
            //g.setColor(0x00ff00);
            appendRect(repaintAnimationsPath, new Rectangle(x, y0 - repaintAnimationsBarHeight(h), w, repaintAnimationsBarHeight(h)));
            y0 -= repaintAnimationsBarHeight(h);
            //g.setColor(0xff0000);
            appendRect(processSerialCallsPath, new Rectangle(x, y0 - processSerialCallsBarHeight(h), w, processSerialCallsBarHeight(h)));
            y0 -= processSerialCallsBarHeight(h);
            //g.setColor(0xffff00);

            int frameBarHeight = frameBarHeight(h) - processSerialCallsBarHeight(h) - paintDirtyBarHeight(h)
                    - repaintAnimationsBarHeight(h);

            appendRect(framePath, new Rectangle(x, y0 - frameBarHeight, w, frameBarHeight));
        }

        private int frameBarHeight(int yMaxHeight) {
            return (int)(((avgFrame) / (double)yAxisMax) * (double)yMaxHeight);
        }

        private int paintDirtyBarHeight(int yMaxHeight) {
            return (int)(((avgPaintDirty) / (double)yAxisMax) * (double)yMaxHeight);
        }

        private int repaintAnimationsBarHeight(int yMaxHeight) {
            return (int)(((avgRepaintAnimations) / (double)yAxisMax) * (double)yMaxHeight);
        }

        private int processSerialCallsBarHeight(int yMaxHeight) {
            return (int)(((avgProcessSerialCalls) / (double)yAxisMax) * (double)yMaxHeight);
        }


    }
    
    private class FrameBar {
        long frameStart, frameEnd, paintDirtyStart, paintDirtyEnd, repaintAnimationsStart, repaintAnimationsEnd, processSerialCallsStart, processSerialCallsEnd;


    
        private void drawBarInRect(Graphics g, int x, int y, int w, int h) {
            //g.setColor(0xffff00);
            //g.fillRect(x, y+h - frameBarHeight(h), w, frameBarHeight(h));
            int y0 = y + h;
            g.setColor(0x0000ff);
            g.fillRect(x, y0 - paintDirtyBarHeight(h), w, paintDirtyBarHeight(h));
            y0 -= paintDirtyBarHeight(h);
            g.setColor(0x00ff00);
            g.fillRect(x, y0 - repaintAnimationsBarHeight(h), w, repaintAnimationsBarHeight(h));
            y0 -= repaintAnimationsBarHeight(h);
            g.setColor(0xff0000);
            g.fillRect(x, y0 - processSerialCallsBarHeight(h), w, processSerialCallsBarHeight(h));
            y0 -= processSerialCallsBarHeight(h);
            g.setColor(0xffff00);
            
            int frameBarHeight = frameBarHeight(h) - processSerialCallsBarHeight(h) - paintDirtyBarHeight(h)
                    - repaintAnimationsBarHeight(h);
            
            g.fillRect(x, y0 - frameBarHeight, w, frameBarHeight);
            
            
        }
        
        private int frameBarHeight(int yMaxHeight) {
            return (int)(((frameEnd - frameStart) / (double)yAxisMax) * (double)yMaxHeight);
        }
        
        private int paintDirtyBarHeight(int yMaxHeight) {
            return (int)(((paintDirtyEnd - paintDirtyStart) / (double)yAxisMax) * (double)yMaxHeight);
        }
        
        private int repaintAnimationsBarHeight(int yMaxHeight) {
            return (int)(((repaintAnimationsEnd - repaintAnimationsStart) / (double)yAxisMax) * (double)yMaxHeight);
        }
        
        private int processSerialCallsBarHeight(int yMaxHeight) {
            return (int)(((processSerialCallsEnd - processSerialCallsStart) / (double)yAxisMax) * (double)yMaxHeight);
        }
        
        
    }
    

    
    @Override
    public void startFrame() {
        if (currentFrame != null) {
            bars.add(currentFrame);
            while (bars.size() > numFramesToRetain) {
                bars.remove(0);
            }
        }


        currentFrame = new FrameBar();
        currentFrame.frameStart = System.currentTimeMillis();




    }



    private void appendFrameToSlice(FrameBar frame) {
        SliceBar slice = null;
        if (slices.isEmpty()) {
            slice = new SliceBar();
            slices.add(slice);
        } else {
            slice = slices.getLast();
            if (slice.numFrames >= framesPerSlice) {
                slice = new SliceBar();
                slices.add(slice);
            }
        }
        slice.addFrame(frame.frameEnd-frame.frameStart,
                frame.paintDirtyEnd - frame.paintDirtyStart,
                frame.repaintAnimationsEnd - frame.repaintAnimationsStart,
                frame.processSerialCallsEnd - frame.processSerialCallsStart);

        while (slices.size() > numSlicesToRetain) {
            slices.remove(0);
        }
    }

    @Override
    public void endFrame() {
        if (currentFrame == null) return;
        currentFrame.frameEnd = System.currentTimeMillis();
        appendFrameToSlice(currentFrame);

        
    }

    @Override
    public void startPaintDirty() {
        if (currentFrame == null) return;
        currentFrame.paintDirtyStart = System.currentTimeMillis();
    }

    @Override
    public void endPaintDirty() {
        if (currentFrame == null) return;
        currentFrame.paintDirtyEnd = System.currentTimeMillis();
    }

    @Override
    public void startRepaintAnimations() {
        if (currentFrame == null) return;
        currentFrame.repaintAnimationsStart = System.currentTimeMillis();
    }

    @Override
    public void endRepaintAnimations() {
        if (currentFrame == null) return;
        currentFrame.repaintAnimationsEnd = System.currentTimeMillis();
    }

    @Override
    public void startProcessSerialCalls() {
        if (currentFrame == null) return;
        currentFrame.processSerialCallsStart = System.currentTimeMillis();
    }

    @Override
    public void endProcessSerialCalls() {
        if (currentFrame == null) return;
        currentFrame.processSerialCallsEnd = System.currentTimeMillis();
    }

    private GeneralPath paintDirtyPath=new GeneralPath(), repaintAnimationsPath = new GeneralPath(), processSerialCallsPath = new GeneralPath(), framePath = new GeneralPath();
    private boolean paintingBuffer;
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!paintSlices) {
            if (!useBuffer || paintingBuffer) {


                int len = bars.size();
                int x = getX() + getWidth();
                int alpha = g.getAlpha();
                g.setAlpha(64);
                for (int i = len - 1; i >= 0; i--) {
                    if (x < getX()) {
                        break;
                    }
                    FrameBar bar = bars.get(i);

                    bar.drawBarInRect(g, x - frameBarWidth, getY(), frameBarWidth, getHeight());
                    x -= frameBarWidth;
                }
                g.setColor(0x333333);
                int goodLine = (int) (16.7 / yAxisMax * getHeight());
                //g.drawLine(getX(), (int)(getY() + getHeight() - goodLine), getX() + getWidth(), (int)(getY() + getHeight() - goodLine));
                g.fillRect(getX(), (int) (getY() + getHeight() - goodLine), getWidth(), CN.convertToPixels(1f));
                g.setAlpha(alpha);
            } else {
                if (buffer != null) {
                    g.drawImage(buffer, getX(), getY());
                }
            }
        } else {
            paintDirtyPath.reset();
            processSerialCallsPath.reset();
            repaintAnimationsPath.reset();
            framePath.reset();
            int len = slices.size();
            //System.out.println("Slices size "+len);
            int x = getX() + getWidth();
            int alpha = g.getAlpha();
            g.setAlpha(64);
            for (int i = len - 1; i >= 0; i--) {
                if (x < getX()) {
                    break;
                }
                SliceBar bar = slices.get(i);

                //bar.drawBarInRect(g, x - sliceBarWidth, getY(), sliceBarWidth, getHeight());
                bar.addBarToPath(paintDirtyPath, repaintAnimationsPath, processSerialCallsPath, framePath,  x - sliceBarWidth, getY(), sliceBarWidth, getHeight());

                x -= sliceBarWidth;
            }
            g.setColor(0x0000ff);
            g.fillShape(paintDirtyPath);
            g.setColor(0x00ff00);
            g.fillShape(repaintAnimationsPath);
            g.setColor(0xff0000);
            g.fillShape(processSerialCallsPath);
            g.setColor(0xffff00);
            g.fillShape(framePath);
            g.setColor(0x00ff00);
            int goodLine = (int) (16.7 / yAxisMax * getHeight());
            //g.drawLine(getX(), (int)(getY() + getHeight() - goodLine), getX() + getWidth(), (int)(getY() + getHeight() - goodLine));
            g.fillRect(getX(), (int) (getY() + getHeight() - goodLine), getWidth(), CN.convertToPixels(1f));
            g.setAlpha(alpha);
        }
        
        
    }

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        updateBuffer();
        repaint();
    }

    private void updateBuffer() {
        if (useBuffer) {
            buffer = Image.createImage(getWidth(), getHeight(), 0x0);
            paintingBuffer = true;
            Graphics g = buffer.getGraphics();
            g.translate(-getX(), -getY());
            paint(g);

            paintingBuffer = false;
        }

    }

    
    /**
     * @return the useBuffer
     */
    private boolean isUseBuffer() {
        return useBuffer;
    }

    /**
     * 
     * @param useBuffer the useBuffer to set
     */
    private void setUseBuffer(boolean useBuffer) {
        this.useBuffer = useBuffer;
    }

    Timer timer;


    @Override
    protected void initComponent() {
        super.initComponent();
        if (useBuffer) {
            timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {

                    CN.callSerially(new Runnable() {
                        public void run() {
                            updateBuffer();
                            repaint();
                        }
                    });
                }
            }, 1000, 4000);
        }

    }

    @Override
    protected void deinitialize() {
        if (useBuffer) {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }

        super.deinitialize();
    }
}
