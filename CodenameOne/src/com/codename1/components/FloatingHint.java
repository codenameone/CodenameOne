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
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;

/**
 * A floating hint is similar to a text field with a hint. However, when the text field has text in it the hint appears
 * above the text field instead including an animation when focus hits the text field see:
 * http://www.google.com/design/spec/components/text-fields.html#text-fields-floating-labels
 *
 * @author Shai Almog
 */
public class FloatingHint extends Container {
    /**
     * Wraps a text field in a floating hint
     * @param tf the text field
     */
    public FloatingHint(final TextField tf) {
        super(new LayeredLayout());
        Container content = new Container(new BorderLayout());
        add(content);
        final Button hintButton = new Button(tf.getHint());
        final Label hintLabel = new Label(tf.getHint());
        tf.setHint("");
        hintButton.setFocusable(false);
        hintButton.setUIID("FloatingHint");
        hintLabel.setUIID("TextHint");
        tf.setLabelForComponent(hintButton);
        
        // we block user initiated editing to allow the animation time to complete
        tf.setEditable(false);
        
        add(BorderLayout.north(new Label(" ")).
                add(BorderLayout.CENTER, tf));
        
        add(BorderLayout.north(hintButton).
                add(BorderLayout.CENTER, hintLabel));
        
        hintButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tf.startEditingAsync();
            }
        });
        if(tf.getText() == null || tf.getText().length() == 0) {
            hintButton.setVisible(false);
        } else {
            hintLabel.setVisible(false);
        }
        FocusListener fl = new FocusListener() {
            public void focusGained(Component cmp) {
                if(isInitialized()) {
                    if(!hintButton.isVisible()) {
                        hintButton.setVisible(true);
                        if(getComponentForm().grabAnimationLock()) {
                            morphAndWait(hintLabel, hintButton, 150);
                            getComponentForm().releaseAnimationLock();
                        }
                        hintLabel.setVisible(false);
                        tf.getComponentForm().revalidate();
                        tf.startEditingAsync();
                    } else {
                        tf.startEditingAsync();
                    }
                } else {
                    boolean t = tf.getText() == null || tf.getText().length() == 0;
                    hintButton.setVisible(t);
                    hintLabel.setVisible(!t);
                }
            }

            public void focusLost(Component cmp) {
                if(isInitialized()) {
                    if(tf.getText().length() == 0) {
                        hintLabel.setVisible(true);
                        if(getComponentForm().grabAnimationLock()) {
                            morphAndWait(hintButton, hintLabel, 150);
                            getComponentForm().releaseAnimationLock();
                        }
                        hintButton.setVisible(false);
                        tf.getComponentForm().revalidate();
                        revalidate();
                    }
                } else {
                    boolean t = tf.getText() == null || tf.getText().length() == 0;
                    hintButton.setVisible(!t);
                    hintLabel.setVisible(t);
                }
            }
        };
        tf.addFocusListener(fl);
    }
}
