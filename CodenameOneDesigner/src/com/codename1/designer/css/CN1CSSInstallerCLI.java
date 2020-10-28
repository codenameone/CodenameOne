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
package com.codename1.designer.css;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import com.codename1.ui.util.EditableResources;
import java.awt.EventQueue;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author shannah
 */
public class CN1CSSInstallerCLI {
    private static void install(String[] args) throws Exception {
        File cssFile = new File(args[1]);
        File resFile = new File(args[2]);
        EventQueue.invokeLater(()->{
            try {
                JavaSEPort.setShowEDTViolationStacks(false);
                
                JavaSEPort.setShowEDTWarnings(false);
                JFrame frm = new JFrame("Placeholder");
                frm.setVisible(false);
                Display.init(frm.getContentPane());
                JavaSEPort.setBaseResourceDir(resFile.getParentFile());
                EditableResources res = new EditableResources();
                res.openFile(new FileInputStream(resFile));
                String mainTheme = res.getThemeResourceNames()[0];
                res.setThemeProperty(mainTheme, "@OverlayThemes", cssFile.getName());
                System.out.println("Setting @OverlayThemes constant in "+mainTheme+" theme of "+resFile.getPath()+" to "+cssFile.getName()+" so that the CSS styles will override styles in the default theme.");
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(resFile));
                res.save(dos);
                dos.close();
                Display.getInstance().exitApplication();
            } catch (IOException ex) {
                Logger.getLogger(CN1CSSInstallerCLI.class.getName()).log(Level.SEVERE, null, ex);
            }
        

        });
    }
        
    
    public static void main(String[] args) throws Exception {
        
        if (args.length != 3 || !"install".equals(args[0])) {
            System.err.println("Invalid parameters.  Expected install <cssfile> <resfile>");
            System.exit(1);
        }
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        //System.setProperty("java.awt.headless", "true");
        
        install(args);
    }
}
