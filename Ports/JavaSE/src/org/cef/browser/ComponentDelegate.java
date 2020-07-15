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
public interface ComponentDelegate {
    public void boundsChanged(int x, int y, int width, int height);
    public void createBrowserIfRequired(boolean b);
    public void wasResized(int width, int height);
}
