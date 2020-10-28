/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.events.ActionEvent;
import java.lang.ref.WeakReference;

/**
 *
 * @author shannah
 */
public class CEFBrowserComponentAdapter implements CEFBrowserComponentListener {
    private WeakReference<BrowserComponent> bcRef;
    
    public CEFBrowserComponentAdapter(BrowserComponent bc) {
        this.bcRef = new WeakReference<BrowserComponent>(bc);
    }
    
    
    @Override
    public void onError(ActionEvent e) {
        BrowserComponent bc = bcRef.get();
        if (bc != null) {
            bc.fireWebEvent(BrowserComponent.onError, e);
        }
    }

    @Override
    public void onStart(ActionEvent e) {
        BrowserComponent bc = bcRef.get();
        if (bc != null) {
            bc.fireWebEvent(BrowserComponent.onStart, e);
        }
    }

    @Override
    public void onLoad(ActionEvent e) {
        BrowserComponent bc = bcRef.get();
        if (bc != null) {
            bc.fireWebEvent(BrowserComponent.onLoad, e);
        }
    }

    @Override
    public boolean shouldNavigate(String url) {
        BrowserComponent bc = bcRef.get();
        if (bc != null) {
            return bc.fireBrowserNavigationCallbacks(url);
        }
        return true;
    }
    
}
