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
import java.awt.Graphics;
import java.awt.IllegalComponentStateException;
import java.awt.Image;

import javax.swing.JComponent;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.This;

/**
    Scriptable Canvas with buffered graphics.

    Provides a Component that:
    1) delegates calls to paint() to a bsh method called paint()
        in a specific NameSpace.
    2) provides a simple buffered image maintained by built in paint() that
        is useful for simple immediate procedural rendering from scripts...

*/
public class BshCanvas extends JComponent {
    This ths;
    Image imageBuffer;

    public BshCanvas () { }

    public BshCanvas ( This ths ) {
        this.ths = ths;
    }

    public void paintComponent( Graphics g ) {
        // copy buffered image
        if ( imageBuffer != null )
            g.drawImage(imageBuffer, 0,0, this);

        // Delegate call to scripted paint() method
        if ( ths != null ) {
            try {
                ths.invokeMethod( "paint", new Object[] { g } );
            } catch(EvalError e) {
                Interpreter.debug(
                    "BshCanvas: method invocation error:", e);
            }
        }
    }

    /**
        Get a buffered (persistent) image for drawing on this component
    */
    public Graphics getBufferedGraphics() {
        Dimension dim = getSize();
        imageBuffer = createImage( dim.width, dim.height );
        if ( null == imageBuffer )
            throw new IllegalComponentStateException("Cannot create Image");
        return imageBuffer.getGraphics();
    }

    public void setBounds( int x, int y, int width, int height ) {
        setPreferredSize( new Dimension(width, height) );
        setMinimumSize( new Dimension(width, height) );
        super.setBounds( x, y, width, height );
    }

}

