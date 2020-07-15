/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.CN;
import org.cef.browser.UIPlatform;

/**
 *
 * @author shannah
 */
public class CEFUIPlatform implements UIPlatform {

    @Override
    public int convertToPixels(int dips, boolean horizontal) {
        return JavaSEPort.instance.convertToPixels(dips, horizontal);
    }

    @Override
    public void runLater(Runnable r) {
        CN.callSerially(r);
    }
    
    
    
}
