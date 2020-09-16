/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.impl.javase.JavaSEPort.CN1JPanel;
import com.codename1.ui.Component;
import com.codename1.ui.Container;

import com.codename1.xml.Element;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JPanel;
import org.cef.browser.ComponentDelegate;
import org.cef.browser.ComponentFactory;

/**
 *
 * @author shannah
 */
public class CEFComponentFactory implements ComponentFactory {

    @Override
    public JPanel createComponent(final ComponentDelegate delegate) {
        return new CN1JPanel() {
            @Override
            public void setBounds(int x, int y, int w, int h) {
                super.setBounds(x, y, w, h);
                delegate.boundsChanged(x, y, w, h);
                /*
                browser_rect_.setBounds(x, y, w, h);
                screenPoint_ = component_.getLocationOnScreen();
                wasResized(w, h);
                */
            }
            
            @Override
            public void setBounds(Rectangle r) {
                setBounds(r.x, r.y, r.width, r.height);
            }

            @Override
            public void setSize(int width, int height) {
                super.setSize(width, height);
                delegate.wasResized(width, height);
            }

            @Override
            public void setSize(Dimension d) {
                setSize(d.width, d.height);
            }
            
            public void paint(Graphics g) {
                delegate.createBrowserIfRequired(false);
                /*
                super.paint(g);
                g.setColor(Color.red);
                g.fillRect(100, 100, 100, 100);
                
                BufferedImage bimg = buffer_.getBufferedImage();
                if (bimg != null) {
                    ((Graphics2D)g).drawImage(bufferedImage_, 0, 0, this);
                }
                */
                
            }
            
        };
    }

   
    
}
