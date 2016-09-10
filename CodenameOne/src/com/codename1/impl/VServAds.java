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
package com.codename1.impl;

import com.codename1.components.WebBrowser;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.services.ImageDownloadService;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.BrowserNavigationCallback;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The v-serv ad service implements full screen ads
 * 
 * @author Shai Almog
 */
public class VServAds extends FullScreenAdService {
    boolean failed;
    private static final String URL = "https://a.vserv.mobi/delivery/adapi.php";
    private String zoneId = "6216";
    private String countryCode;
    private String networkCode;
    private String locale = "en";
    
    public static final int CAT_ID_ACTION_ADVENTURE = 18;
    public static final int CAT_ID_SPORTS = 19;
    public static final int CAT_ID_MOVIES = 20;
    public static final int CAT_ID_KIDS = 21;
    public static final int CAT_ID_RACING = 22;
    public static final int CAT_ID_ARCADE = 23;
    public static final int CAT_ID_ADULT = 24;
    public static final int CAT_ID_STRATEGY = 25;
    public static final int CAT_ID_TRAVEL = 27;
    public static final int CAT_ID_EDUCATION = 28;
    public static final int CAT_ID_PRODUCTIVITY = 29;
    public static final int CAT_ID_ENTERTAINMENT = 30;
    public static final int CAT_ID_MULTIMEDIA = 31;
    public static final int CAT_ID_SPIRITUAL = 32;
    public static final int CAT_ID_UTILITY = 33;
    public static final int CAT_ID_SOCIAL_NETWORKING = 34;
    public static final int CAT_ID_HEALTH = 35;
    public static final int CAT_ID_OTHERS = 36;

    private int category = CAT_ID_PRODUCTIVITY;
    
    private String destination;
    private String imageURL;
    private String contentType;
    private int backgroundColor;
    private String renderNotify;
    private String actionNotify;
    
    /**
     * {@inheritDoc}
     */
    protected ConnectionRequest createAdRequest() {
        ConnectionRequest con = new ConnectionRequest() {
            protected void handleErrorResponseCode(int code, String message) {
                failed = true;
            }

            protected void handleException(Exception err) {
                failed = true;
                Log.e(err);
            }

            private String getString(Hashtable h, String n) {
                Object v = h.get(n);
                if(v == null) {
                    return null;
                }
                if(v instanceof Vector) {
                    return (String)((Vector)v).elementAt(0);
                }
                return (String)v;
            }
            
            protected void readResponse(InputStream input) throws IOException {
                JSONParser parser = new JSONParser();
                Hashtable h = parser.parse(new InputStreamReader(input, "UTF-8"));
                if(h.size() == 0) {
                    return;
                }
                backgroundColor = Integer.parseInt( (String)((Hashtable)((Vector)h.get("style")).elementAt(0)).get("background-color"), 16);
                Hashtable actionHash = ((Hashtable)((Vector)h.get("action")).elementAt(0));
                actionNotify = getString(actionHash, "notify");
                if(actionNotify == null) {
                    actionNotify = getString(actionHash, "notify-once");
                }
                
                destination = (String)actionHash.get("data");
                
                Hashtable renderHash = ((Hashtable)((Vector)h.get("render")).elementAt(0));
                contentType = (String)renderHash.get("type");
                renderNotify = getString(renderHash, "notify");
                if(renderNotify == null) {
                    renderNotify = getString(renderHash, "notify-once");
                }
                imageURL = (String)renderHash.get("data");
            }
        };
        con.setUrl(URL);
        con.setPost(false);
        con.addArgument("zoneid", getZoneId());
        con.addArgument("ua", Display.getInstance().getProperty("User-Agent", ""));
        con.addArgument("app", "1");
        con.addArgument("aid", Display.getInstance().getProperty("androidId", ""));
        con.addArgument("uuid", Display.getInstance().getProperty("uuid", ""));
        con.addArgument("im", Display.getInstance().getProperty("imei", ""));
        con.addArgument("sw", "" + Display.getInstance().getDisplayWidth());
        con.addArgument("sh", "" + Display.getInstance().getDisplayHeight());
        con.addArgument("mn", Display.getInstance().getProperty("AppName", ""));
        con.addArgument("vs3", "1");
        con.addArgument("partnerid", "1");
        con.addArgument("zc", "" + category);
        if(countryCode != null) {
            con.addArgument("cc", countryCode);
        }
        if(networkCode != null) {
            con.addArgument("nc", networkCode);            
        }
        if(locale != null) {
            con.addArgument("lc", locale);            
        }
        return con;
    }

    protected boolean hasPendingAd() {
        return imageURL != null;
    }
    
    /**
     * {@inheritDoc}
     */
    protected void clearPendingAd() {
        imageURL = null;
        renderNotify = null;
        
    }
    
    /**
     * {@inheritDoc}
     */
    protected Component getPendingAd() {
        if(imageURL == null) {
            return null;
        }
        if(renderNotify != null && renderNotify.length() > 0) {
            ConnectionRequest c = new ConnectionRequest();
            c.setFailSilently(true);
            c.setUrl(renderNotify);
            c.setPost(false);
            NetworkManager.getInstance().addToQueue(c);
        }
        if("image".equalsIgnoreCase(contentType)) {
            Button adComponent = new Button(){

                public void setIcon(Image icon) {
                    if(icon != null && isScaleMode()){
                        icon = icon.scaledWidth(Display.getInstance().getDisplayWidth());
                    }
                    super.setIcon(icon);
                }
                
            };
            adComponent.setUIID("Container");
            adComponent.getStyle().setBgColor(backgroundColor);
            adComponent.getStyle().setOpacity(0xff);
            ImageDownloadService imd = new ImageDownloadService(imageURL, adComponent);
            NetworkManager.getInstance().addToQueueAndWait(imd);
            /*adComponent.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    Display.getInstance().execute(getAdDestination());
                }
            });*/
            return adComponent;
        } else {
            WebBrowser wb = new WebBrowser();
            if(wb.getInternal() instanceof BrowserComponent) {
                BrowserComponent bc = (BrowserComponent)wb.getInternal();
                bc.setBrowserNavigationCallback(new BrowserNavigationCallback() {
                    public boolean shouldNavigate(final String url) {
                        unlock(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                Display.getInstance().execute(url);
                            }
                        });
                        return false;
                    }
                });
            }
            wb.setURL(imageURL);
            return wb;
        }
    }

    /**
     * {@inheritDoc}
     */
    protected String getAdDestination() {
        if(actionNotify != null && actionNotify.length() > 0) {
            ConnectionRequest c = new ConnectionRequest();
            c.setFailSilently(true);
            c.setUrl(actionNotify);
            c.setPost(false);
            NetworkManager.getInstance().addToQueue(c);
        }
        return destination;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean failed() {
        return failed;
    }

    /**
     * @return the countryCode
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * @param countryCode the countryCode to set
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * @return the networkCode
     */
    public String getNetworkCode() {
        return networkCode;
    }

    /**
     * @param networkCode the networkCode to set
     */
    public void setNetworkCode(String networkCode) {
        this.networkCode = networkCode;
    }

    /**
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * @return the category
     */
    public int getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(int category) {
        this.category = category;
    }

    /**
     * @return the zoneId
     */
    public String getZoneId() {
        return zoneId;
    }

    /**
     * @param zoneId the zoneId to set
     */
    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAllowSkipping() {
        return true;
    }
    
    
}
