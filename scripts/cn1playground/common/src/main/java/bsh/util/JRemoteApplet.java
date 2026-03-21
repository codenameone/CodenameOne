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
import java.awt.Component;
import java.awt.Label;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JWindow;


/**
    A lightweight console applet for remote display of a Beanshell session.
*/

public class JRemoteApplet extends JFrame
{
    OutputStream out;
    InputStream in;
    Socket s;

    public JRemoteApplet(int port) {

        getContentPane().setLayout(new BorderLayout());
        setSize(600, 200);
        try {
            // URL base = getDocumentBase();

            // connect to session server on port (httpd + 1)
            s = new Socket("localhost", port);// base.getHost(), base.getPort() + 1);
            out = s.getOutputStream();
            in = s.getInputStream();
        } catch(IOException e) {
            getContentPane().add("Center",
                new Label("Remote Connection Failed", Label.CENTER));
            setVisible(true);
            return;
        }

        Component console = new JConsole(in, out);
        getContentPane().add("Center", console);
        setVisible(true);
    }
    public static void main(String[] args) {
        new JWindow(new JRemoteApplet(Integer.parseInt(args[0])));
    }

}

