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

package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.services.RSSService;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.GenericListCellRenderer;
import com.codename1.ui.html.HTMLComponent;
import com.codename1.ui.TextArea;
import com.codename1.io.html.AsyncDocumentRequestHandlerImpl;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A List implementing an RSS reader that automatically populates itself with content
 * from the RSS chanel. When clicking an article it displays the HTML content of said
 * article in a new Form.
 *
 * @author Shai Almog
 */
public class RSSReader extends List {
    private Vector existingData;
    private String url = "http://blogs.oracle.com/readingLists/oracleblogs.xml";
    private RSSService service;
    private int limit = 20;
    private boolean waitingForResponseLock;
    private boolean blockList = true;
    private String progressTitle = "Fetching RSS";
    private boolean displayProgressPercentage;
    private static final Hashtable MORE = new Hashtable();
    static {
        MORE.put("title", "More");
        MORE.put("details", "Fetch More Elements");
    }

    private boolean designMode;

    /**
     * Sets the form/container to which the RSS will navigate when clicking a RSS
     * entry
     */
    private Container targetContainer;

    /**
     * Indicates whether a back command should be added implicitly to the target container
     */
    private boolean addBackToTaget = true;

    /**
     * Creates an rss reader instance
     */
    public RSSReader() {
        setUIID("RSSReader");
        setRenderer(new GenericListCellRenderer(createRendererContainer(), createRendererContainer()));
        addActionListener(new EventHandler());
    }


    /**
     * Set the description for the "more" request
     * 
     * @param d the description
     */
    public static void setMoreDescription(String d) {
        MORE.put("description", d);
    }

    /**
     * Set the title for the "more" request
     * 
     * @param t new title
     */
    public static void setMoreTitle(String t) {
        MORE.put("title", t);
    }

    private Container createRendererContainer() {
        Container entries = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        entries.setUIID("RSSEntry");
        Label title = new Label();
        title.setName("title");
        title.setUIID("RSSTitle");
        entries.addComponent(title);
        TextArea description = new TextArea(2, 30);
        description.setGrowByContent(false);
        description.setName("details");
        description.setUIID("RSSDescription");
        description.setScrollVisible(false);
        entries.addComponent(description);
        return entries;
    }

    /**
     * The URL of the RSS stream
     *
     * @param url The URL of the RSS stream
     */
    public void setURL(String url) {
        this.url = url;
    }

    /**
     * Send the request to the server, will only work once. This is called implicitly
     * when the list is initialized
     */
    public void sendRequest() {
        if(service == null) {
            service = new RSSService(url, limit);
            service.addResponseListener(new EventHandler());
            if(blockList) {
                Progress p = new Progress(progressTitle, service, displayProgressPercentage);
                p.setAutoShow(true);
                p.setDisposeOnCompletion(true);
            }
            setHint(progressTitle);
            NetworkManager.getInstance().addToQueue(service);
        }
    }

    /**
     * @inheritDoc
     */
    protected void initComponent() {
        super.initComponent();
        if(designMode) {
            setHint("RSS Data Will Show Here");
        } else {
            sendRequest();
        }
    }

    /**
     * The URL of the RSS stream
     *
     * @return The URL of the RSS stream 
     */
    public String getURL() {
        return url;
    }

    private void updateComponentValues(Container root, Hashtable h) {
        int c = root.getComponentCount();
        for(int iter = 0 ; iter < c ; iter++) {
            Component current = root.getComponentAt(iter);
            
            // the comparison assumes that other container subclases are really just
            // custom components e.g. tree, table and HTMLComponent as well as third party
            // subclasses
            if(current.getClass() == com.codename1.ui.Container.class ||
                    current.getClass() == com.codename1.ui.Tabs.class) {
                updateComponentValues((Container)current, h);
                continue;
            }
            String n = current.getName();
            if(n != null) {
                String val = (String)h.get(n);
                if(val != null) {
                    if(current instanceof Button) {
                        final String url = (String)val;
                        ((Button)current).addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                Display.getInstance().execute(url);
                            }
                        });
                        continue;
                    }
                    if(current instanceof Label) {
                        ((Label)current).setText(val);
                        continue;
                    }
                    if(current instanceof TextArea) {
                        ((TextArea)current).setText(val);
                        continue;
                    }
                    if(current instanceof HTMLComponent) {
                        ((HTMLComponent)current).setHTML(val, "UTF-8", "", val.indexOf("<html") > -1);
                        continue;
                    }
                }
            }
        }
    }

    /**
     * Shows a form containing the RSS entry
     *
     * @param h the parsed entry
     */
    protected void showRSSEntry(Hashtable h) {
        Form newForm = null;
        if(targetContainer != null) {
            if(targetContainer instanceof Form) {
                newForm = (Form)targetContainer;
            } else {
                newForm = new Form((String)h.get("title"));
                newForm.setLayout(new BorderLayout());
                newForm.addComponent(BorderLayout.CENTER, targetContainer);
            }
            updateComponentValues(newForm, h);
        } else {
            newForm = new Form((String)h.get("title"));
            HTMLComponent c = new HTMLComponent(new AsyncDocumentRequestHandlerImpl());
            String s = (String)h.get("description");
            s = "<html><body>" + s + "</body></html>";
            c.setBodyText(s);
            newForm.setLayout(new BorderLayout());
            newForm.addComponent(BorderLayout.CENTER, c);
        }
        if(addBackToTaget) {
            final Form sourceForm = Display.getInstance().getCurrent();
            Command back = new Command("Back") {
                public void actionPerformed(ActionEvent ev) {
                    sourceForm.showBack();
                }
            };
            newForm.addCommand(back);
            newForm.setBackCommand(back);
        }
        newForm.show();
    }

    /**
     * Places a limit on the number of RSS entries requested from the server
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Places a limit on the number of RSS entries requested from the server
     * 
     * @param limit the limit to set
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"limit", "url", "blockList", "progressTitle", "displayProgressPercentage", "target"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {Integer.class, String.class, Boolean.class, String.class, Boolean.class, Container.class};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("limit")) {
            return new Integer(limit);
        }
        if(name.equals("url")) {
            return url;
        }
        if(name.equals("blockList")) {
            if(blockList) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("progressTitle")) {
            return progressTitle;
        }
        if(name.equals("displayProgressPercentage")) {
            if(displayProgressPercentage) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("target")) {
            return targetContainer;
        }
        if(name.equals("$designMode")) {
            if(designMode) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("limit")) {
            limit = ((Integer)value).intValue();
            return null;
        }
        if(name.equals("url")) {
            url = (String)value;
            return null;
        }
        if(name.equals("blockList")) {
            blockList = ((Boolean)value).booleanValue();
            return null;
        }
        if(name.equals("progressTitle")) {
            progressTitle = (String)value;
            return null;
        }
        if(name.equals("displayProgressPercentage")) {
            displayProgressPercentage = ((Boolean)value).booleanValue();
            return null;
        }
        if(name.equals("target")) {
            targetContainer = (Container)value;
            return null;
        }
        if(name.equals("$designMode")) {
            designMode = ((Boolean)value).booleanValue();
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * @return the blockList
     */
    public boolean isBlockList() {
        return blockList;
    }

    /**
     * @param blockList the blockList to set
     */
    public void setBlockList(boolean blockList) {
        this.blockList = blockList;
    }

    /**
     * @return the progressTitle
     */
    public String getProgressTitle() {
        return progressTitle;
    }

    /**
     * @param progressTitle the progressTitle to set
     */
    public void setProgressTitle(String progressTitle) {
        this.progressTitle = progressTitle;
    }

    /**
     * The form/container to which the RSS will navigate when clicking a RSS
     * entry
     * @return the targetContainer
     */
    public Container getTargetContainer() {
        return targetContainer;
    }

    /**
     * The form/container to which the RSS will navigate when clicking a RSS
     * entry
     * @param targetContainer the targetContainer to set
     */
    public void setTargetContainer(Container targetContainer) {
        this.targetContainer = targetContainer;
    }

    /**
     * Indicates whether a back command should be added implicitly to the target container
     * @return the addBackToTaget
     */
    public boolean isAddBackToTaget() {
        return addBackToTaget;
    }

    /**
     * Indicates whether a back command should be added implicitly to the target container
     * @param addBackToTaget the addBackToTaget to set
     */
    public void setAddBackToTaget(boolean addBackToTaget) {
        this.addBackToTaget = addBackToTaget;
    }


    class EventHandler implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            if(evt instanceof NetworkEvent) {
                waitingForResponseLock = false;
                NetworkEvent e = (NetworkEvent)evt;
                Vector v = (Vector)e.getMetaData();
                RSSService s = (RSSService)e.getConnectionRequest();

                if(existingData != null) {
                    existingData.removeElement(MORE);
                    for(int iter = 0 ; iter < v.size() ; iter++) {
                        existingData.addElement(v.elementAt(iter));
                    }
                } else {
                    existingData = v;
                }

                if(s.hasMore()) {
                    v.addElement(MORE);
                }

                setModel(new DefaultListModel(existingData));
                return;
            }

            Hashtable sel = (Hashtable)getSelectedItem();
            if(sel == MORE) {
                if(waitingForResponseLock) {
                    return;
                }
                waitingForResponseLock = true;
                service = new RSSService(url, limit, existingData.size() - 1);
                service.addResponseListener(new EventHandler());
                Progress p = new Progress("Fetching RSS", service);
                p.setAutoShow(true);
                p.setDisposeOnCompletion(true);
                NetworkManager.getInstance().addToQueue(service);
            } else {
                showRSSEntry(sel);
            }
        }
    }
}
