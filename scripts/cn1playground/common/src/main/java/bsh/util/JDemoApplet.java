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

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JWindow;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;

/**
    Run bsh as an applet for demo purposes.
*/
public class JDemoApplet extends JFrame
{
    Interpreter interpreter;
    public JDemoApplet(String type)
    {
        super("Demo");

        // String type = getParameter("type");
        if ( type != null && type.equals("desktop") )
            // start the desktop
            try {
                interpreter = new Interpreter();
                interpreter.eval( "desktop()" );
            } catch ( TargetError te ) {
                te.printStackTrace();
                System.out.println( te.getTarget() );
                te.getTarget().printStackTrace();
            } catch ( EvalError evalError ) {
                System.out.println( evalError );
                evalError.printStackTrace();
            }
        else
        {
            getContentPane().setLayout(new BorderLayout());
            setSize(600,200);
            JConsole console = new JConsole();
            getContentPane().add("Center", console);
            interpreter = new Interpreter( console );
            new Thread(interpreter).start();
            setVisible(true);
        }
    }
    public static void main(String[] args) {
        new JWindow(new JDemoApplet(args[0]));
    }
}

