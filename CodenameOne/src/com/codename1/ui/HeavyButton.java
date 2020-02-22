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

import com.codename1.ui.events.ActionListener;

/**
 * A Button that is rendered by the native platform, over top of the light-weight components.
 * The action listener is also fired on the native main thread.
 * 
 * <p>Currently this is only used in the Javascript port because there are some functions
 * that can only be performed in direct response to user action.  Using a HeavyButton 
 * rather than a button to invoke the action will solve this.</p>
 * 
 * <p>WARNING: You can't use any concurrency (wait, sleep, etc..) in the action listener
 * or it will break Javascript.  Only use this class if you know what you're doing.</p>
 * @author shannah
 */
class HeavyButton extends Button {
        Object peer;
        HeavyButton(String label) {
            super(label);
            if (Display.impl.requiresHeavyButtonForCopyToClipboard()) {
                peer = Display.impl.createHeavyButton(this);
            }
            
        }

        @Override
        public void addActionListener(ActionListener l) {
            super.addActionListener(l);
            if (peer != null) {
                Display.impl.addHeavyActionListener(peer, l);
            }
        }

        @Override
        public void removeActionListener(ActionListener l) {
            super.removeActionListener(l);
            if (peer != null) {
                Display.impl.removeHeavyActionListener(peer, l);
            }
        }
        
        private void updateHeavyBounds() {
            if (peer != null) {
                Display.impl.updateHeavyButtonBounds(peer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
            }
        }
        
        public void setWidth(int w) {
            super.setWidth(w);
            updateHeavyBounds();
        }
        
        public void setHeight(int h) {
            super.setHeight(h);
            updateHeavyBounds();
        }
        
        public void setX(int x) {
            super.setX(x);
            updateHeavyBounds();
        }
        
        public void setY(int y) {
            super.setY(y);
            updateHeavyBounds();
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            
            if (peer != null) {
                Display.impl.initHeavyButton(peer);
            }
        }

        @Override
        protected void deinitialize() {
            if (peer != null) {
                Display.impl.deinitializeHeavyButton(peer);
            }
            super.deinitialize();
        }
        
        
        
        
        
        
    }
