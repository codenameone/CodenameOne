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
package com.codename1.ui.list;

import com.codename1.components.MultiButton;
import com.codename1.ui.List;
import java.util.Hashtable;

/**
 * A list with a multi-button renderer by default
 *
 * @author Shai Almog
 */
public class MultiList extends List {
    /**
     * Constructor for the GUI builder
     */
    public MultiList() {
        super(new DefaultListModel(new Object[] {
            h("Entry 1", "more..."),
            h("Entry 2", "more..."),
            h("Entry 3", "more..."),
        }));
        MultiButton sel = new MultiButton();
        MultiButton unsel = new MultiButton();
        GenericListCellRenderer gn = new GenericListCellRenderer(sel, unsel);
        setRenderer(gn);
    }
    
    private static Hashtable h(String fline, String sline) {
        Hashtable h = new Hashtable();
        h.put("Line1", fline);
        h.put("Line2", sline);
        return h;
    }
}
