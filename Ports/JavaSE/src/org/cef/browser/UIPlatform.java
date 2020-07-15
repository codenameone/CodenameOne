/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cef.browser;

/**
 *
 * @author shannah
 */
public interface UIPlatform {
    public int convertToPixels(int dips, boolean horizontal);
    public void runLater(Runnable r);
    
}
