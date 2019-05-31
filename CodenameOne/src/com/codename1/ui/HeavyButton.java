/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
