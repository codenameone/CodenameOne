/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.html;

import com.codename1.ui.Button;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class implements HTML Image maps (ones defined with the MAP and AREA tags) 
 * 
 * @author Ofir Leitner
 */
class HTMLImageMap extends Button implements ActionListener {

    ImageMapData mapData;
    HTMLComponent htmlC;

    HTMLImageMap(HTMLComponent htmlC) {
        this.htmlC=htmlC;
        setUIID("HTMLLink");
        addActionListener(this);
    }
    
    
    public void actionPerformed(ActionEvent evt) {
        if (mapData!=null) {
            int x=evt.getX();
            int y=evt.getY();
            if ((mapData.areas!=null) && (x!=-1)) {
                for(Enumeration e=mapData.areas.keys();e.hasMoreElements();) {
                    Rectangle rect = (Rectangle)e.nextElement();
                    if (rect.contains(x-getAbsoluteX(), y-getAbsoluteY())) {
                        String link=(String)mapData.areas.get(rect);
                        if (link!=null) {
                            HTMLLink.processLink(htmlC, link);
                        }
                        return;
                    }
                }
            }
            if (mapData.defaultLink!=null) {
                HTMLLink.processLink(htmlC, mapData.defaultLink);
            }
        }

    }


}
