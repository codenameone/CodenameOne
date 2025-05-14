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

import com.codename1.io.Log;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/**
 * A collection of useful progress animations and utility methods.
 * @author shannah
 * @since 7.0
 */
public class CommonProgressAnimations {
    
    /**
     * Base class for ProgressAnimations
     * @since 7.0
     */
    public static abstract class ProgressAnimation extends Component {
        private static final String PROGRESS_KEY = "$$ProgressAnimation";
        protected Component cmp;
        
        /**
         * Marks a component as "loading", replacing it in its parent conatiner with
         * a progress animation.  When the component's content is "ready" or finished loading
         * you should call {@link #markComponentReady(com.codename1.ui.Component) } to swap 
         * the component back into its parent, and removing the progress animation.
         * 
         * <p>If the component has already been marked as "loading", this will simply return
         * the progress animation that was previously assigned to it.</p>
         * 
         * @param cmp The component to be replaced by a progress animation.
         * @param type The type of progress animation to use.
         * @return The ProgressAnimation that is currently occuppying the component's space.
         */
        public static ProgressAnimation markComponentLoading(Component cmp, Class<? extends ProgressAnimation> type) {
            if (type == null) {
                type = CircleProgress.class;
            }
            if (getProgressAnimation(cmp) != null) {
                return getProgressAnimation(cmp);
            }
            try {
                ProgressAnimation prg = (ProgressAnimation)type.newInstance();
                prg.setPreferredH(cmp.getPreferredH());
                prg.setPreferredW(cmp.getPreferredW());
                replaceUntilReady(cmp, prg);
                return prg;
            } catch (Throwable t) {
                Log.e(t);
                throw new RuntimeException("Failed to create progress component: "+t.getMessage());
            }
        }
        
        private static void replaceUntilReady(Component cmp, ProgressAnimation progress) {
            if (cmp.getClientProperty(PROGRESS_KEY) instanceof ProgressAnimation) {
                throw new IllegalStateException("Component already has ProgressAnimation assigned to it");
            }
            if (cmp.getParent() == null) {
                throw new IllegalStateException("Component has no parent so cannot be replaced by progress");
            }
            cmp.putClientProperty(PROGRESS_KEY, progress);
            cmp.getParent().replace(cmp, progress, null);
            progress.cmp = cmp;
           
        }
        
        /**
         * Marks a component as "ready", if it had previously been marked as "loading".  If the component
         * had been replaced in its parent container by a progress animation, then this will swap it back.
         * @param cmp The component to mark as "ready".
         * @param t The transition to use for replacing the progress animation with the component.  Null for immediate replacement.
         */
        public static void markComponentReady(Component cmp, Transition t) {
            ProgressAnimation progress = (ProgressAnimation)cmp.getClientProperty(PROGRESS_KEY);
            if (progress == null) {
                return;
            }
            cmp.putClientProperty(PROGRESS_KEY, null);
            Container parent = progress.getParent();
            if (parent != null) {
                parent.replace(progress, cmp, t);
                if (t == null) {
                    parent.revalidateLater();
                }
            }
        }
        
        /**
         * Marks a component as "ready", if it had previously been marked as "loading".  If the component
         * had been replaced in its parent container by a progress animation, then this will swap it back.
         * @param cmp The component to mark as "ready".
         */
        public static void markComponentReady(Component cmp) {
            markComponentReady(cmp, null);
        }
        
        /**
         * Gets the progress animation that is currently showing for a component.  The progress
         * animation would be "assigned" by {@link #markComponentLoading(com.codename1.ui.Component, java.lang.Class) },
         * and can be "deassigned" by by {@link #markComponentReady(com.codename1.ui.Component) }.
         * 
         * <p>This may return null, if the component isn't currently marked as loading.</p>
         * 
         * @param cmp The Component whose progress animation we're seeking.
         * @return The ProgressAnimation, or null if the component isn't "loading".
         */
        public static ProgressAnimation getProgressAnimation(Component cmp) {
            return (ProgressAnimation)cmp.getClientProperty(PROGRESS_KEY);
        }
        
        @Override
        protected void initComponent() {
            super.initComponent();
            getComponentForm().registerAnimated(this);
        }

        @Override
        protected void deinitialize() {
            getComponentForm().deregisterAnimated(this);
            super.deinitialize();
        }
    }
    
    /**
     * A progress animation that shows an animated circle.
     * @since 7.0
     */
    public static class CircleProgress extends ProgressAnimation {
        int stepSize = (int)Math.round(360 / Display.getInstance().getFrameRate() / 1.5);
        int step = 0;
        @Override
        public void paint(Graphics g) {
            super.paint(g); 
        }

        @Override
        public boolean animate() {
            step += stepSize;
            step = step % 720;
            return true;
        }

        @Override
        protected void paintBackground(Graphics g) {
            g.setColor(getStyle().getFgColor());
            int alpha = g.concatenateAlpha(getStyle().getFgAlpha());
            boolean anti = g.isAntiAliased();
            g.setAntiAliased(true);
            int d = Math.min(getWidth(), getHeight());
            int x = getX() + (getWidth()-d)/2;
            int y = getY() + (getHeight()-d)/2;
            int w = d;
            int h = d;
            if (step <= 360) {
                g.fillArc(x, y, w, h, 0, -step);
            } else {
                g.fillArc(x, y, w, h, 0, (720 - step));
            }
            g.setAntiAliased(anti);
            g.setAlpha(alpha);
        }

        @Override
        protected Dimension calcPreferredSize() {
            Dimension out = new Dimension(CN.convertToPixels(8f), CN.convertToPixels(8f));
            return out;
        }
        
        /**
         * Replaces the given component with a CircleProgress until its content is ready.
         * When the component's content is ready, then make sure to call {@link ProgressAnimation#markComponentReady(com.codename1.ui.Component) )
         * to swap the component back into its parent.
         * @param cmp
         * @return 
         */
        public static CircleProgress markComponentLoading(Component cmp) {
            return (CircleProgress)markComponentLoading(cmp, CircleProgress.class);
        }
        
        
    }
    
    /**
     * An empty progress animation.
     * @since 7.0
     */
    public static class EmptyAnimation extends ProgressAnimation {
        @Override
        protected Dimension calcPreferredSize() {
            Dimension out = new Dimension(CN.convertToPixels(8f), CN.convertToPixels(8f));
            return out;
        }
        
        /**
         * Replaces the given component with an EmptyAnimation until its content is ready.
         * When the component's content is ready, then make sure to call {@link ProgressAnimation#markComponentReady(com.codename1.ui.Component) )
         * to swap the component back into its parent.
         * @param cmp
         * @return 
         */
        public static EmptyAnimation markComponentLoading(Component cmp) {
            return (EmptyAnimation)markComponentLoading(cmp, EmptyAnimation.class);
        }
    }
    
    /**
     * A progress animation that shows a block of text being typed.  Except words
     * are rendered as filled rectangles instead of actual glyphs.  This is appropriate
     * where a paragraph of text is scheduled to appear, but is not ready to show yet.
     * @since 7.0
     */
    public static class LoadingTextAnimation extends ProgressAnimation {
        
        private static final String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        private int lettersPerChunk = 3;
        private int cyclesPerChunk = 3; // Number of cycles required to type a letter.
        private int pauseCounter = 0;
        private int pauseLength = Display.getInstance().getFrameRate();
        private int rows = 3;
        private int cols = 40;
        private int cycleCount;
        private int strlen = loremIpsum.length();
        private int strpos = 0;
        
        
        public LoadingTextAnimation() {
            getStyle().setFgColor(0x666666);
            getStyle().setOpacity(0x66);
            $(this).setPaddingMillimeters(2f);
        }

        @Override
        public boolean animate() {
            if (pauseCounter > 0) {
                pauseCounter--;
                if (pauseCounter == 0) {
                    strpos = 0;
                }
                return true;
            }
            cycleCount = (cycleCount + 1) % cyclesPerChunk;
            if (cycleCount == 1) {
                strpos = (strpos + lettersPerChunk);
                if (strpos >= strlen) {
                    pauseCounter = pauseLength;
                    
                } else {
                    strpos = strpos % strlen;
                }
            }
            return true;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g); 
        }

        @Override
        protected void paintBackground(Graphics g) {
            strlen = loremIpsum.length();
            g.setColor(getStyle().getFgColor());

            int alpha = g.getAlpha();
            float pauseRatio = 1f;
            if (pauseCounter > 0) {
                pauseRatio = pauseCounter / (float)pauseLength;
            }
            g.setAlpha((int)(getStyle().getOpacity()/255f * alpha * pauseRatio));
            g.concatenateAlpha(getStyle().getFgAlpha());
            Font f = cmp == null ? getStyle().getFont() : cmp.getStyle().getFont();
            if (f == null) {
                f = Font.getDefaultFont();
            }
            int h = f.getHeight();
            int w = f.charWidth('M');
            Style s = cmp == null ? getStyle() : cmp.getStyle();
            
            int paddingTop = s.getPaddingTop();
            int paddingLeft = s.getPaddingLeftNoRTL();
            int paddingRight = s.getPaddingRightNoRTL();
            int paddingBottom = s.getPaddingBottom();
            int leftBounds = getX() + paddingLeft;
            int rightBounds = getX() + getWidth() - paddingRight;
            int bottomBounds = getY() + getHeight() - paddingBottom;
            int topBounds = getY() + paddingBottom;
            int x =leftBounds;
            int y = topBounds;
            int wordStartX = x;
            int wordStartPos = 0;
            int leading = h / 2;
            int row = 0;
            for (int i=0; i<strpos; i++) {
                char c = loremIpsum.charAt(i);
                
                if (c == ' ') {
                    if (wordStartX < x) {
                        g.fillRect(wordStartX, y, x - wordStartX, h);
                    }
                    x += w;
                    wordStartX = x;
                    wordStartPos = i+1;
                } else {
                    x += w;
                }
                if (x > rightBounds) {
                    row++;
                    x = leftBounds;
                    i = wordStartPos-1;
                    wordStartX = x;
                    y += h + leading;
                }
                if (y + h > bottomBounds || row > rows-1) {
                    strlen = i+1;
                    break;
                }
                
            }
            g.setAlpha(alpha);
            
        }

        @Override
        protected Dimension calcPreferredSize() {
            Font f = getStyle().getFont();
            if (f == null) {
                f = Font.getDefaultFont();
            }
            int charWidth = f.charWidth('M');
            int charHeight = f.getHeight();
            int leading = charHeight/2;
            
            Dimension out = new Dimension(charWidth * cols + getStyle().getHorizontalPadding(), rows * (charHeight + leading) - leading + getStyle().getVerticalPadding());
            return out;
        }
        
        /**
         * Returns the number of rows of text to render.
         * @return 
         */
        public int rows() {
            return rows;
        }
        
        /**
         * Returns the number of columns to render.
         * @return 
         */
        public int cols() {
            return cols;
        }
        
        /**
         * Sets the number of rows to render.
         * @param rows The number of rows to render.
         * @return Self for chaining.
         */
        public LoadingTextAnimation rows(int rows) {
            this.rows = rows;
            return this;
        }
        
        /**
         * Returns the number of columns to render.  
         * @param cols The number of columns to render.
         * @return Self for chaining.
         */
        public LoadingTextAnimation cols(int cols) {
            this.cols = cols;
            return this;
        }
        
        /**
         * Replaces the given component with a LoadingTextAnimation until its content is ready.
         * When the component's content is ready, then make sure to call {@link ProgressAnimation#markComponentReady(com.codename1.ui.Component) )
         * to swap the component back into its parent.
         * @param cmp
         * @return 
         */
        public static LoadingTextAnimation markComponentLoading(Component cmp) {
            return (LoadingTextAnimation)markComponentLoading(cmp, LoadingTextAnimation.class);
        }
    }
}
