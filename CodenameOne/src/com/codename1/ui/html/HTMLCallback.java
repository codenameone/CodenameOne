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
import com.codename1.xml.ParserCallback;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;

/**
 * HTMLCallback is used to dispatch document lifecycle events.
 * Most methods are called on the EDT thread, except parsingError, getAutoComplete and getLinkProperties
 *
 * @author Ofir Leitner
 */
public interface HTMLCallback extends ParserCallback,CSSParserCallback {


    //////////////////////////////////
    // Error constants              //
    //////////////////////////////////

    /**
     * Error code denoting that a connection to the resource provider/server could not be made
     */
    public static int ERROR_CONNECTING = 100;

    /**
     * Error code denoting that an image referenced from the HTML was not found
     */
    public static int ERROR_IMAGE_NOT_FOUND = 101;

    /**
     * Error code denoting that an image referenced from the HTML could not be loaded
     */
    public static int ERROR_IMAGE_BAD_FORMAT = 102;

    /**
     * Error code denoting that a relative URL was referenced from a document with no base URL (A document that was loaded via setBody/setHTML/setDOM and not via setPage)
     * In this case the return value of parsingError is not considered - parsing continues and the resource at the URL (CSS file/image) is ignored
     */
    public static int ERROR_NO_BASE_URL = 103;

    /**
     * Error code denoting that a tag contains tags it shouldn't, or that a tag doesn't have the parent tag it should have
     */
    public static int ERROR_INVALID_TAG_HIERARCHY = 104;

    //////////////////////////////////
    // Page status constants        //
    //////////////////////////////////

    /**
     * This is returned in the page status if no page has been set to the HTMLComponent
     */
    public static int STATUS_NONE = -3;

    /**
     * The page couldn't load completely because of parsing errors
     */
    public static int STATUS_ERROR = -2;

    /**
     * The page loading was cancelled before it could be completed
     */
    public static int STATUS_CANCELLED = -1;
    
    /**
     * The page was requested from the request handler
     */
    public static int STATUS_REQUESTED = 0;

    /**
     * The stream was received
     */
    public static int STATUS_CONNECTED = 1;

    /**
     * The page was parsed
     */
    public static int STATUS_PARSED = 2;

    /**
     *  The page was displayed on screen - but at this stage some images and CSS files may still be loading in the background
     */
    public static int STATUS_DISPLAYED = 3;

    /**
     * The page and all of its referenced images and CSS files were loaded completely
     */
    public static int STATUS_COMPLETED = 4;

    /**
     * The page was redirected to another URL
     */
    public static int STATUS_REDIRECTED = 5;



    //////////////////////////////////
    // Field type constants         //
    //////////////////////////////////

    /**
     * A text field
     */
    public static int FIELD_TEXT = 0;

    /**
     * A password field
     */
    public static int FIELD_PASSWORD = 1;


    //////////////////////////////////
    // Link property constants      //
    //////////////////////////////////

    /**
     * A regular link
     */
    public static int LINK_REGULAR = 0;

    /**
     * A link that was visited before
     */
    public static int LINK_VISTED = 1;

    /**
     * A forbidden link (not to be rendered as a link but as a regular label)
     */
    public static int LINK_FORBIDDEN = 2;


    //////////////////////////////////
    // Interface methods            //
    //////////////////////////////////

    /**
     * Called when the page's title is updated
     * 
     * @param htmlC The HTMLComponent that triggered the event
     * @param title The new title
     */
    public void titleUpdated(HTMLComponent htmlC, String title);

    /**
     *  Called when the page status has been changed
     * 
     * @param htmlC The HTMLComponent in which the status change occured
     * @param status The new status, one of the STATUS_* constants
     * @param url The URL of the page
     */
    public void pageStatusChanged(HTMLComponent htmlC, int status,String url);
    
    /**
     * Called whenever a field is submitted to a form. 
     * This can be used to perform sanity checks and/or to store values for auto complete.
     * 
     * @param htmlC The HTMLComponent in which this event occured
     * @param ta The TextArea/TextField of this field
     * @param actionURL The action URL of the form
     * @param id The ID of the field
     * @param value The value entered
     * @param type The type of the field, one of the FIELD_* constants
     * @param errorMsg The error message if any error occured (i.e. input validation error) or null if no error occured
     * @return The string to submit to the form (Should return value if nothing changed)
     */
    public String fieldSubmitted(HTMLComponent htmlC,TextArea ta,String actionURL,String id,String value,int type,String errorMsg);

    /**
     * Called on form creation and enabled implementations of this method to return a value to preset in a form field.
     * This can be used to auto complete previously entered  value
     * Note that this method is always called NOT on the EDT thread.
     *
     * @param htmlC The HTMLComponent in which this event occured
     * @param actionURL The action URL of the form
     * @param id The ID of the field
     * @return The string to place in the indicated field
     */
    public String getAutoComplete(HTMLComponent htmlC,String actionURL,String id);

    /**
     * Returns properties about the given link to indicate to HTMLComponent how to render it
     * Note that this method is always called NOT on the EDT thread.
     *
     * @param htmlC The HTMLComponent
     * @param url The Link URL
     * @return LINK_REGULAR or LINK_VISITED or LINK_FORBIDDEN or a mask of those
     */
    public int getLinkProperties(HTMLComponent htmlC, String url);

    /**
     * Called when a link is clicked. This can be used to process links that needs additional/alternate handling than fetching an HTML.
     *
     * @param htmlC The HTMLComponent
     * @param url The Link URL
     * @return true if regular link processing should continue, false otherwise
     */
    public boolean linkClicked(HTMLComponent htmlC, String url);


    //////////////////////
    /// Events methods
    //////////////////////

    /**
     * Called whenever an ActionEvent is triggered in one of the controls in the HTMLComponent
     * For example, button press, checkbox/radiobutton check etc.
     * This method will be called only if HTMLComponent.isEventsEnabled() is true
     * 
     * @param evt The event triggered, the component can be extracted using getSource
     * @param htmlC The HTMLComponent
     * @param element The element associated with the component that triggered the event
     */
    public void actionPerformed(ActionEvent evt,HTMLComponent htmlC, HTMLElement element);

    /**
     * Called when one of the controls in the HTMLComponent obtained focus
     * This method will be called only if HTMLComponent.isEventsEnabled() is true
     * 
     * @param cmp The component that triggered the event
     * @param htmlC The HTMLComponent
     * @param element The element associated with the component that triggered the event
     */
    public void focusGained(Component cmp, HTMLComponent htmlC, HTMLElement element);

    /**
     * Called when one of the controls in the HTMLComponent lost focus
     * This method will be called only if HTMLComponent.isEventsEnabled() is true
     *
     * @param cmp The component that triggered the event
     * @param htmlC The HTMLComponent
     * @param element The element associated with the component that triggered the event
     */
    public void focusLost(Component cmp, HTMLComponent htmlC, HTMLElement element);


    /**
     * Called when the user selects an item in a List inside the HTMLComponent
     * Note: Selection is traversing through the items - once the user has actually decided, an ActionEvent will be sent as well
     * This method will be called only if HTMLComponent.isEventsEnabled() is true
     *
     * @param oldSelected old selected index in list model
     * @param newSelected new selected index in list model
     * @param htmlC The HTMLComponent
     * @param list The list that triggered the event (Usually a ComboBox but not always)
     * @param element The element associated with the component that triggered the event (Should be TAG_SELECT)
     */
    public void selectionChanged(int oldSelected, int newSelected, HTMLComponent htmlC, List list, HTMLElement element);

    /**
     * Called when the user types in a TextField inside the HTMLComponent
     * This method will be called only if HTMLComponent.isEventsEnabled() is true
     *
     * @param type the type data change; REMOVED, ADDED or CHANGED
     * @param index item index in a list model
     * @param htmlC The HTMLComponent
     * @param textField The TextField that triggerd the event
     * @param element The element associated with the component that triggered the event (Should be TAG_INPUT with type text/password)
     */
    public void dataChanged(int type, int index, HTMLComponent htmlC, TextField textField, HTMLElement element);

}
