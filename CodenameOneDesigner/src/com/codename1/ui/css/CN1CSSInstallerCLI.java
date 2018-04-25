/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.css;

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
