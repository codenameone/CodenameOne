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
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;

/**
 * <p>A floating hint is similar to a text field with a hint. However, when the text field has text in it the hint appears
 * above the text field instead including an animation when focus hits the text field see 
 * <a href="http://www.google.com/design/spec/components/text-fields.html#text-fields-floating-labels"
 * target="_blank">
 * Googles take on this</a>.
 * </p>
 * <script src="https://gist.github.com/codenameone/54e919b2be01561ac91c.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-floatinghint.png" alt="The FloatingHint component with one component that contains text and another that doesn't" />
 * 
 * <h4>The animation effect</h4>
 * <img src="http://www.codenameone.com/img/blog/floatinghint.gif" alt="Animation" />
 *
 * @author Shai Almog
 */
public class FloatingHint extends Container {
    private final TextArea tf;
    private final Button hintButton;
    private final Label hintLabel;
    
    /**
     * Wraps a text field in a floating hint
     * @param tf the text field
     */
    public FloatingHint(final TextArea tf) {
        super(new LayeredLayout());
        this.tf = tf;
        Container content = new Container(new BorderLayout());
        add(content);
        hintButton = new Button(tf.getHint()) {
            @Override
            protected boolean shouldRenderSelection() {
                return true;
            }
        };
        hintLabel = new Label(tf.getHint());
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
                focusGainedImpl();
            }

            public void focusLost(Component cmp) {
                focusLostImpl();
            }
        };
        tf.addFocusListener(fl);
    }

    private void focusGainedImpl() {
        if(isInitialized()) {
            hintButton.setFocus(true);
            if(!hintButton.isVisible()) {
                hintButton.setVisible(true);
                if(getComponentForm().grabAnimationLock()) {
                    morphAndWait(hintLabel, hintButton, 150);
                    getComponentForm().releaseAnimationLock();
                }
                hintLabel.setVisible(false);
                tf.getComponentForm().revalidate();
                tf.setEditable(true);
                tf.startEditingAsync();
            } else {
                tf.setEditable(true);
                tf.startEditingAsync();
            }
        } else {
            boolean t = tf.getText() == null || tf.getText().length() == 0;
            hintButton.setVisible(t);
            hintLabel.setVisible(!t);
            revalidate();
        }
    }
    
    private void focusLostImpl() {
        if(isInitialized()) {
            hintButton.setFocus(false);
            if(tf.getText().length() == 0) {
                hintLabel.setVisible(true);
                if(getComponentForm().grabAnimationLock()) {
                    morphAndWait(hintButton, hintLabel, 150);
                    getComponentForm().releaseAnimationLock();
                }
                hintButton.setVisible(false);
                tf.getComponentForm().revalidate();
                revalidate();
                tf.setEditable(false);
            } 
        } else {
            boolean t = tf.getText() == null || tf.getText().length() == 0;
            hintButton.setVisible(!t);
            hintLabel.setVisible(t);
            revalidate();
        }
    }
    
    @Override
    protected void initComponent() {
        super.initComponent(); 
        if (tf.hasFocus()) {
            focusGainedImpl();
        }
        
    }

    
    
    
}
