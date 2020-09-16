/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.events.ActionEvent;

/**
 *
 * @author shannah
 */
public class CEFBrowserComponentAdapter implements CEFBrowserComponentListener {
    private BrowserComponent bc;
    
    public CEFBrowserComponentAdapter(BrowserComponent bc) {
        this.bc = bc;
    }
    
    
    @Override
    public void onError(ActionEvent e) {
        if (bc != null) {
            bc.fireWebEvent(BrowserComponent.onError, e);
        }
    }

    @Override
    public void onStart(ActionEvent e) {
        if (bc != null) {
            bc.fireWebEvent(BrowserComponent.onStart, e);
        }
    }

    @Override
    public void onLoad(ActionEvent e) {
        if (bc != null) {
            bc.fireWebEvent(BrowserComponent.onLoad, e);
        }
    }

    @Override
    public boolean shouldNavigate(String url) {
        if (bc != null) {
            return bc.fireBrowserNavigationCallbacks(url);
        }
        return true;
    }
    
}
