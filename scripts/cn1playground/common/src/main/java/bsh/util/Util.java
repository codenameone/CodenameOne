/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/
package bsh.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;

import bsh.Interpreter;


/**
    Misc utilities for the bsh.util package.
    Nothing in the core language (bsh package) should depend on this.
    Note: that promise is currently broken... fix it.
*/
public class Util
{
    /*
    public static ConsoleInterface makeConsole() {
        if ( bsh.Capabilities.haveSwing() )
            return new JConsole();
        else
            return new AWTConsole();
    }
    */

    static Window splashScreen;
    /*
        This could live in the desktop script.
        However we'd like to get it on the screen as quickly as possible.
    */
    public static void startSplashScreen()
    {
        int width=275,height=148;
        Window win=new Window( new Frame() );
        win.pack();
        BshCanvas can=new BshCanvas();
        can.setSize( width, height ); // why is this necessary?
        Toolkit tk=Toolkit.getDefaultToolkit();
        Dimension dim=tk.getScreenSize();
        win.setBounds(
            dim.width/2-width/2, dim.height/2-height/2, width, height );
        win.add("Center", can);
        Image img=tk.getImage(
            Interpreter.class.getResource("/bsh/util/lib/splash.gif") );
        MediaTracker mt=new MediaTracker(can);
        mt.addImage(img,0);
        try { mt.waitForAll(); } catch ( Exception e ) { }
        Graphics gr=can.getBufferedGraphics();
        gr.drawImage(img, 0, 0, can);
        win.setVisible(true);
        win.toFront();
        splashScreen = win;
    }

    public static void endSplashScreen() {
        if ( splashScreen != null )
            splashScreen.dispose();
    }

}
