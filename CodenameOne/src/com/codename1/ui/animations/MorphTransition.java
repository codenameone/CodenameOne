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

package com.codename1.ui.animations;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A transition inspired by the Android L release morph activity effect allowing
 * a set of components in one form/container to morph into another in a different
 * container/form.
 *
 * @author Shai Almog
 */
public class MorphTransition extends Transition {
    private int duration;
    private HashMap<String, String> fromTo = new HashMap<String, String>();
    private CC[] fromToComponents;
    private Motion animationMotion;
    private boolean finished;
    
    private MorphTransition() {}

    /**
     * @inheritDoc
     */
    public Transition copy(boolean reverse){
        MorphTransition m = create(duration);
        if(reverse) {
            Iterator<String> keyIterator = fromTo.keySet().iterator();
            while(keyIterator.hasNext()) {
                String k = keyIterator.next();
                String v = fromTo.get(k);
                m.fromTo.put(v, k);
            }
        } else {
            m.fromTo.putAll(fromTo);
        }
        return m;
    }
    
    /**
     * Creates a transition with the given duration, this transition should be modified with the 
     * builder methods such as morph
     * @param duration the duration of the transition
     * @return a new Morph transition instance
     */
    public static MorphTransition create(int duration) {
        MorphTransition mt = new MorphTransition();
        mt.duration = duration;
        return mt;
    }
    
    /**
     * Morphs the component with the given source name in the source container hierarchy 
     * to the component with the given name in the destination hierarchy
     * @param source
     * @param to
     * @return this so morph operations can be chained as MorphTransition t = MorphTransition.create(300).morph("a", "b").("c", "d");
     */
    public MorphTransition morph(String source, String to) {
        fromTo.put(source, to);
        return this;
    }

    /**
     * @inheritDoc
     */
    public final void initTransition() {
        animationMotion = Motion.createEaseInOutMotion(0, 255, duration);
        animationMotion.start();
        Container s = (Container)getSource();
        Container d = (Container)getDestination();

        Iterator<String> keyIterator = fromTo.keySet().iterator();
        int size = fromTo.size();
        fromToComponents = new CC[size];
        Form destForm = d.getComponentForm();
        Form sourceForm = s.getComponentForm();
        for(int iter = 0 ; iter < size ; iter++) {
            String k = keyIterator.next();
            String v = fromTo.get(k);
            CC cc = new CC(findByName(s, k), findByName(d, v), sourceForm, destForm);
            fromToComponents[iter] = cc;
            cc.placeholder = new Label();
            cc.placeholder.setVisible(false);
            Container destParent = cc.dest.getParent();
            Object constraint = destParent.getLayout().getComponentConstraint(cc.dest);
            destParent.removeComponent(cc.dest);
            cc.placeholder.setX(cc.dest.getX());
            cc.placeholder.setY(cc.dest.getY() - destForm.getTitleArea().getHeight());
            cc.placeholder.setWidth(cc.dest.getWidth());
            cc.placeholder.setHeight(cc.dest.getHeight());
            cc.placeholder.setPreferredSize(new Dimension(cc.dest.getWidth(), cc.dest.getHeight()));
            if(constraint != null) {
                destParent.addComponent(constraint, cc.placeholder);
            } else {
                destParent.addComponent(cc.placeholder);
            }
            destForm.getLayeredPane().addComponent(cc.dest);
            cc.originalContainer = cc.source.getParent();
            cc.originalConstraint = cc.originalContainer.getLayout().getComponentConstraint(cc.source);
            cc.originalContainer.removeComponent(cc.source);
            cc.originalContainer.getComponentForm().getLayeredPane().addComponent(cc.source);
        }
    }

    private static Component findByName(Container root, String componentName) {
        int count = root.getComponentCount();
        for(int iter = 0 ; iter < count ; iter++) {
            Component c = root.getComponentAt(iter);
            String n = c.getName();
            if(n != null && n.equals(componentName)) {
                return c;
            }
            if(c instanceof Container) {
                c = findByName((Container)c, componentName);
                if(c != null) {
                    return c;
                }
            }
        }
        return null;
    }
    
    /**
     * @inheritDoc
     */
    public boolean animate() {
        if(!finished) {
            // animate one last time
            if(animationMotion.isFinished()) {
                finished = true;
                
                // restore forms to orignial states
                for(CC c : fromToComponents) {
                    Container p = c.placeholder.getParent();
                    Object constraint = p.getLayout().getComponentConstraint(c.placeholder);
                    p.removeComponent(c.placeholder);
                    c.dest.getParent().removeComponent(c.dest);
                    if(constraint != null) {
                        p.addComponent(constraint, c.dest);
                    } else {
                        p.addComponent(c.dest);
                    }

                    c.source.getParent().removeComponent(c.source);
                    if(c.originalConstraint != null) {
                        c.originalContainer.addComponent(c.originalConstraint, c.source);
                    } else {
                        c.originalContainer.addComponent(c.source);
                    }
                }
                
                // remove potential memory leak
                fromToComponents = null;
                
                return true;
            }
            for(CC c : fromToComponents) {
                int x = c.xMotion.getValue();
                int y = c.yMotion.getValue();
                int w = c.wMotion.getValue();
                int h = c.hMotion.getValue();
                c.source.setX(x);
                c.source.setY(y);
                c.source.setWidth(w);
                c.source.setHeight(h);
                c.dest.setX(x);
                c.dest.setY(y);
                c.dest.setWidth(w);
                c.dest.setHeight(h);
            }

            return true;
        }

        return false;
    }

    /**
     * @inheritDoc
     */
    public void paint(Graphics g) {
        int oldAlpha = g.getAlpha();
        int alpha = animationMotion.getValue();
        if(alpha < 255) {
            g.setAlpha(255 - alpha);
            getSource().paintComponent(g);

            g.setAlpha(alpha);
            getDestination().paintComponent(g);
            g.setAlpha(oldAlpha);
        } else {
            getDestination().paintComponent(g);
        }
    }
    
    class CC {
        public CC(Component source, Component dest, Form sourceForm, Form destForm) {
            this.source = source;
            this.dest = dest;
            int titleHeightSource = sourceForm.getTitleArea().getHeight();
            int titleHeightDest = destForm.getTitleArea().getHeight();
            xMotion = Motion.createEaseInOutMotion(source.getAbsoluteX(), dest.getAbsoluteX(), duration);
            xMotion.start();
            yMotion = Motion.createEaseInOutMotion(source.getAbsoluteY() - titleHeightSource, dest.getAbsoluteY()  - titleHeightDest, duration);
            yMotion.start();
            hMotion = Motion.createEaseInOutMotion(source.getHeight(), dest.getHeight(), duration);
            hMotion.start();
            wMotion = Motion.createEaseInOutMotion(source.getWidth(), dest.getWidth(), duration);
            wMotion.start();
        }
        
        Component source;
        Component dest;
        Label placeholder;
        Motion xMotion;
        Motion yMotion;
        Motion wMotion;
        Motion hMotion;
        Object originalConstraint;
        Container originalContainer;
    }
}
