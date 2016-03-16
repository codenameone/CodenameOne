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
package com.codename1.ui.list;

import com.codename1.ui.Component;
import com.codename1.ui.List;

/**
 * A "rubber stamp" tool that allows us to extract a component (often the same
 * component instance for all invocations) that is initialized to the value 
 * of the current item extracted from the model, this component is drawn on the
 * list and discarded. No state of the component is kept and the component is
 * essentially discarded.<br>
 * <b>This is a very advanced interface</b> it is recommended that you use either
 * the DefaultListCellRenderer or GenericListCellRenderer whenever possible
 * to avoid mistakes.<br>
 * <p>An instance of a renderer can be developed as such:
 * <script src="https://gist.github.com/codenameone/003d20de84f1a962b811.js"></script></pre>
 * 
 * @author Chen Fishbein
 */
public interface ListCellRenderer<T> { 
    /**
     * Returns a component instance that is already set to render "value". While it is not a requirement
     * many renderes often derive from a component (such as a label) and return "this".
     * Notice that a null value for the value argument might be sent when refreshing the theme of the
     * list.
     * 
     * @param list the list component
     * @param value the value to render
     * @param index the index in the list
     * @param isSelected whether the entry is selected
     * @return a component to paint within the list
     */
    public Component getListCellRendererComponent(List list, T value, int index, boolean isSelected);
    
    /**
     * Returns a component instance that is painted under the currently focused renderer
     * and is animated to provide smooth scrolling. 
     * When the selection moves, this component is drawn above/bellow the list items - 
     * it is recommended to give this component some level of transparency (see above code example). 
     * This method is optional an implementation 
     * can choose to return null.
     * 
     * @param list the parent list 
     * @return a component to use as focus
     * @see List#setSmoothScrolling
     */
    public Component getListFocusComponent(List list);
    
}
