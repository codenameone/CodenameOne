/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cef.browser;

import javax.swing.JPanel;

/**
 *
 * @author shannah
 */
public interface ComponentFactory {
    public JPanel createComponent(ComponentDelegate delegate);
}
