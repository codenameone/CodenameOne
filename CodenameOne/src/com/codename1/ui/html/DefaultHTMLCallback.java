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

import com.codename1.ui.Component;
import com.codename1.ui.List;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;

/**
 * This is a default implementation of HTMLCallback that basically doesn't do much but does keep the HTMLComponent work intact.
 * This class was created so developers will avoid pitfalls of HTMLCallback, as using the wrong return values.
 *
 * Note that in any case an HTMLComponent doesn't have to use an HTMLCallback.
 *
 * @author Ofir Leitner
 */
public class DefaultHTMLCallback implements HTMLCallback {

    /**
     * {@inheritDoc}
     */
    public void titleUpdated(HTMLComponent htmlC, String title) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public boolean parsingError(int errorId, String tag, String attribute, String value, String description) {
        System.out.println(description);
        return true; // Signals the parser to continue parsing despite of the error (if it is a recoverable error)
    }

    /**
     * {@inheritDoc}
     */
    public void pageStatusChanged(HTMLComponent htmlC, int status, String url) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public String fieldSubmitted(HTMLComponent htmlC, TextArea ta, String actionURL, String id, String value, int type, String errorMsg) {
        return value; // Returns the same value
    }

    /**
     * {@inheritDoc}
     */
    public String getAutoComplete(HTMLComponent htmlC, String actionURL, String id) {
        return null; // i.e. no auto complete value was found
    }

    /**
     * {@inheritDoc}
     */
    public int getLinkProperties(HTMLComponent htmlC, String url) {
        return LINK_REGULAR; // Regular link - not visited and not forbidden
    }

    /**
     * {@inheritDoc}
     */
    public boolean linkClicked(HTMLComponent htmlC, String url) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent evt, HTMLComponent htmlC, HTMLElement element) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void focusGained(Component cmp, HTMLComponent htmlC, HTMLElement element) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void focusLost(Component cmp, HTMLComponent htmlC, HTMLElement element) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void selectionChanged(int oldSelected, int newSelected, HTMLComponent htmlC, List list, HTMLElement element) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void dataChanged(int type, int index, HTMLComponent htmlC, TextField textField, HTMLElement element) {
        // do nothing
    }

}
