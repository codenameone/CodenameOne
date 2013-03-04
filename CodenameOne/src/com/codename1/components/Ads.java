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
package com.codename1.components;

import com.codename1.ads.AdsService;
import com.codename1.io.ConnectionRequest;
import com.codename1.ui.html.AsyncDocumentRequestHandlerImpl;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.html.IOCallback;
import com.codename1.ui.html.DocumentInfo;
import com.codename1.ui.html.HTMLCallback;
import com.codename1.ui.html.HTMLComponent;
import com.codename1.ui.html.HTMLElement;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import java.io.IOException;
import java.util.Vector;

/**
 * This is an Ads Component, this Component can displays banner/text ads on a 
 * Form.
 * This is a generic Ads Component that can support different type of Ads Network
 * Services, at the Moment Codename One supports innerActive ads, to gain an appId
 * please refer to 
 * http://console.inner-active.com/iamp/publisher/register?ref_id=affiliate_CodenameOne
 * 
 * @author Chen
 */
public class Ads extends Container implements HTMLCallback {

    private long elapsed;
    private int updateDuration = 60;
    private String ad;
    private AdsService service;
    private boolean refreshAd;
    private String appId;
    /**
     * optional parameters
     */
    private String age;
    private String gender;
    private String category;
    private String location;
    private String[] keywords;

    /**
     * Default constructor for GUI builder
     */
    public Ads() {
        setUIID("Ads");
        setLayout(new BorderLayout());
        
        // special case for iOS. It seems the ad component can inadvertedly steal focus from 
        // the text field being edited thus blocking the hiding of the lightweight text.
        // I'm guessing this can affect Android too in some cases
        setFocusable(!Display.getInstance().isTouchScreenDevice());
        Label filler = new Label(" ");
        filler.setPreferredSize(new Dimension(400, 2));
        filler.getStyle().setBgTransparency(0);
        addComponent(BorderLayout.CENTER, filler);
    }
    
    /**
     * Simple constructor to create an Ad Component
     * @param appId unique identifier of the app, to gain an appId please refer to 
     * http://console.inner-active.com/iamp/publisher/register?ref_id=affiliate_CodenameOne
     */
    public Ads(String appId) {
        this(appId, true);
    }

    /**
     * 
     * @param appId unique identifier of the app, to gain an appId please refer to 
     * http://console.inner-active.com/iamp/publisher/register?ref_id=affiliate_CodenameOne
     * @param refreshAd if true this Component will refresh the Ad every 60 seconds,
     * if false no refresh will occur
     */
    public Ads(String appId, boolean refreshAd) {
        this();
        this.appId = appId;
        this.refreshAd = refreshAd;
        this.service = AdsService.createAdsService();
    }

    /**
     * @inheritDoc
     */
    public void initComponent() {
        if(service != null) {
            service.initialize(this);
            service.addResponseListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    String a = (String) evt.getSource();
                    setAd(a);
                }
            });
            
            if (refreshAd) {
                getComponentForm().registerAnimated(this);
            }else{
                requestAd();            
            }
        }
    }

    /**
     * @inheritDoc
     */
    protected void deinitialize() {
        if (refreshAd) {
            getComponentForm().deregisterAnimated(this);
        }
    }
    
    

    private void requestAd() {
        service.requestAd();
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        Form parent = getComponentForm();
        if (parent == null || !parent.isVisible()) {
            return false;
        }
        long t = System.currentTimeMillis();
        if (t - elapsed > getUpdateDuration() * 1000) {
            // we need to update the ad
            elapsed = t;
            requestAd();
        }
        return super.animate();
    }

    /**
     * @inheritDoc
     */
    public void keyReleased(int code) {
        if (Display.getInstance().getGameAction(code) == Display.GAME_FIRE) {
            launchAd();
            requestAd();
        }
    }

    /**
     * @inheritDoc
     */
    public void pointerReleased(int x, int y) {
        if (!isDragActivated()) {
            launchAd();
            requestAd();
        }
    }

    private void launchAd() {
        Component c = getComponentAt(0);
        if (c instanceof HTMLComponent) {
            HTMLComponent h = (HTMLComponent) c;
            HTMLElement dom = h.getDOM();
            Vector links = dom.getDescendantsByTagName("a");
            if (links.size() > 0) {
                HTMLElement e = (HTMLElement) links.elementAt(0);
                String link = e.getAttribute("href");
                if (link != null) {
                    Display.getInstance().execute(link);
                }
            }
        }
    }

    /**
     * HTML ad received from the server
     * @return the ad
     */
    public String getAd() {
        return ad;
    }

    /**
     * HTML ad received from the server
     * @param ad the ad to set
     */
    public void setAd(String ad) {
        HTMLComponent html = new HTMLComponent(new AsyncDocumentRequestHandlerImpl() {

            protected ConnectionRequest createConnectionRequest(DocumentInfo docInfo, IOCallback callback, Object[] response) {
                ConnectionRequest req = super.createConnectionRequest(docInfo, callback, response);
                req.addResponseCodeListener(new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        //do nothing, just make sure the html won't throw an error
                    }
                });
                return req;
            }
        });

        html.setHTMLCallback(this);
        html.setBodyText("<html><body><div align='center'>" + ad + "</div></body></html>");
        if (isInitialized()) {
            replaceAndWait(getComponentAt(0), html, CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, true, 200));
        } else {
            replace(getComponentAt(0), html, null);
        }
        revalidate();
        html.setPageUIID("Container");
        html.getStyle().setBgTransparency(0);
    }

    /**
     * The amount of time needed to update the ad
     *
     * @return the updateDuration
     */
    public int getUpdateDuration() {
        return updateDuration;
    }

    /**
     * The amount of time needed to update the ad
     *
     * @param updateDuration the updateDuration to set
     */
    public void setUpdateDuration(int updateDuration) {
        this.updateDuration = updateDuration;
    }

    /**
     * @inheritDoc
     */
    public void titleUpdated(HTMLComponent htmlC, String title) {
    }

    /**
     * @inheritDoc
     */
    public void pageStatusChanged(HTMLComponent htmlC, int status, String url) {
        if (status == STATUS_COMPLETED) {
            // loop the component and prevent entries within it from receiving focus 
            unfocus(htmlC);
            //notify the service the ad is being displayed
        } else if (status == STATUS_DISPLAYED) {
            service.onAdDisplay(htmlC);
        }
    }

    private void unfocus(Container c) {
        c.setFocusable(false);
        c.setFocus(false);
        int s = c.getComponentCount();
        for (int iter = 0; iter < s; iter++) {
            Component current = c.getComponentAt(iter);
            if (current instanceof Container) {
                unfocus((Container) current);
            } else {
                current.setFocusable(false);
                current.setFocus(false);
            }
        }
    }

    /**
     * @inheritDoc
     */
    public String fieldSubmitted(HTMLComponent htmlC, TextArea ta, String actionURL, String id, String value, int type, String errorMsg) {
        return value;
    }

    /**
     * @inheritDoc
     */
    public String getAutoComplete(HTMLComponent htmlC, String actionURL, String id) {
        return "";
    }

    /**
     * @inheritDoc
     */
    public int getLinkProperties(HTMLComponent htmlC, String url) {
        return LINK_REGULAR;
    }

    /**
     * @inheritDoc
     */
    public boolean linkClicked(HTMLComponent htmlC, String url) {
        //this is relevant when the Ad is in Full Screen mode
        launchAd();
        //reportClick();
        return false;
    }

    /**
     * @inheritDoc
     */
    public void actionPerformed(ActionEvent evt, HTMLComponent htmlC, HTMLElement element) {
    }

    /**
     * @inheritDoc
     */
    public void focusGained(Component cmp, HTMLComponent htmlC, HTMLElement element) {
    }

    /**
     * @inheritDoc
     */
    public void focusLost(Component cmp, HTMLComponent htmlC, HTMLElement element) {
    }

    /**
     * @inheritDoc
     */
    public void dataChanged(int type, int index, HTMLComponent htmlC, TextField textField, HTMLElement element) {
    }

    /**
     * @inheritDoc
     */
    public boolean parsingError(int errorId, String tag, String attribute, String value, String description) {
        return true;
    }

    /**
     * @inheritDoc
     */
    public void selectionChanged(int oldSelected, int newSelected, HTMLComponent htmlC, com.codename1.ui.List list, HTMLElement element) {
    }

    /**
     * Simple setter of the unique identifier of the app on the ads service 
     * network, no need to manually use this the createAdsService uses this.
     * 
     * @param appId unique identifier of the app, to gain an appId please refer to 
     * http://console.inner-active.com/iamp/publisher/register?ref_id=affiliate_CodenameOne
     */
    public void setAppID(String appId) {
        this.appId = appId;
        if(service == null) {
            service = AdsService.createAdsService();
            if(isInitialized()) {
                initComponent();
            }
        }
    }

    /**
     * Simple getter of the unique identifier of the app on the ads service 
     * network.
     * 
     * @return the app unique identifier.
     */
    public String getAppID() {
        return appId;
    }

    /**
     * Sets Gender if applicable can be one of the following:
     * 'F', 'f', 'M', 'm', 'Female', 'female', 'Male', 'male'
     * @param gender
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * Keywords relevant to this user specific session
     * @param keywords
     */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    /**
     * Users age
     * @return the user age
     */
    public String getAge() {
        return age;
    }

    /**
     * The user gender can be: M/m, F/f, Male, Female.
     * @return 
     */
    public String getGender() {
        return gender;
    }

    /**
     * Keywords relevant to this user specific session
     * @return 
     */
    public String[] getKeywords() {
        return keywords;
    }

    /**
     * Sets the users age
     * @param age 
     */
    public void setAge(String age) {
        this.age = age;
    }

    /**
     * Category is a single word description of the application.
     * @param category 
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Category is a single word description of the application.
     * @return a single word description of the application.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Location string is a comma separated list of country, state/province, city
     * For example: US, NY, NY
     * @param location 
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Location string is a comma separated list of country, state/province, city
     * For example: US, NY, NY
     * @return 
     */
    public String getLocation() {
        return location;
    }
    
    
    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[]{"appId", "updateDuration", "age", "gender", "category", "location", "keywords"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
        Class c = new String[0].getClass();
        return new Class[]{String.class, Integer.class, String.class, String.class, String.class, String.class, c};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if (name.equals("appId")) {
            return getAppID();
        }
        if (name.equals("updateDuration")) {
            return new Integer(getUpdateDuration());
        }
        if (name.equals("age")) {
            return getAge();
        }
        if (name.equals("gender")) {
            return getGender();
        }
        if (name.equals("category")) {
            return getCategory();
        }
        if (name.equals("location")) {
            return getLocation();
        }
        if (name.equals("keywords")) {
            return getKeywords();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if (name.equals("appId")) {
            setAppID((String) value);
            return null;
        }
        if (name.equals("updateDuration")) {
            setUpdateDuration(((Integer) value).intValue());
            return null;
        }
        if (name.equals("age")) {
            setAge((String) value);
            return null;
        }
        if (name.equals("gender")) {
            setGender((String) value);
            return null;
        }
        if (name.equals("category")) {
            setCategory((String) value);
            return null;
        }
        if (name.equals("location")) {
            setLocation((String) value);
            return null;
        }
        if (name.equals("keywords")) {
            setKeywords((String[]) value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}
