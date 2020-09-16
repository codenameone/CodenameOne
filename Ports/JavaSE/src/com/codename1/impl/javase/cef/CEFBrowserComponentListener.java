/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.ui.events.ActionEvent;

/**
 *
 * @author shannah
 */
public interface CEFBrowserComponentListener {
    public void onError(ActionEvent e);
    public void onStart(ActionEvent e);
    public void onLoad(ActionEvent e);
    public boolean shouldNavigate(String url);
}
