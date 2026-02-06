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
package com.codename1.ads;

import com.codename1.components.Ads;
import com.codename1.io.ConnectionRequest;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;

import java.io.IOException;
import java.io.InputStream;

/// This is an Ad service implementation by InnerActive see:
/// http://console.inner-active.com/iamp/publisher/register?ref_id=affiliate_CodenameOne
///
/// @author Chen
public class InnerActive extends AdsService { // PMD Fix: UnusedPrivateField removed obsolete field

    private static final String protocolVersion = "Sm2m-1.5.3";
    private static boolean testAds = true;
    private static final String REQUEST_URL = "http://m2m1.inner-active.com/simpleM2M/clientRequestHtmlAd";
    //Distribution channel ID
    private int po = 559;
    private String os;
    //UDID/IMEI
    private String hid;
    private boolean banner = true;

    private static void addParam(ConnectionRequest req, String key, String val) {
        if (val != null && val.length() > 0) {
            req.addArgument(key, val);
        }
    }

    /// If true and no ads exists the network will return house holds ads
    ///
    /// #### Parameters
    ///
    /// - `test`
    public static void setTestAds(boolean test) {
        testAds = test;
    }

    /// Sets this ads type, by default this a banner type.
    ///
    /// #### Parameters
    ///
    /// - `banner`: sets the ads to banners or text ads
    public void setBanner(boolean banner) {
        this.banner = banner;
    }

    /// initialize the ads service
    @Override
    public void initService(Ads ads) {
        this.os = Display.getInstance().getPlatformName();
        if ("and".equals(os)) {
            if (banner) {
                po = 559;
            } else {
                po = 600;
            }
        } else if ("rim".equals(os)) {
            if (banner) {
                po = 635;
            } else {
                po = 634;
            }
        } else if ("ios".equals(os)) {
            if (banner) {
                if (Display.getInstance().isTablet()) {
                    po = 947;
                } else {
                    po = 642;
                }
            } else {
                if (Display.getInstance().isTablet()) {
                    po = 946;
                } else {
                    po = 632;
                }
            }
        } else if ("me".equals(os)) {
            if (banner) {
                po = 551;
            } else {
                po = 519;
            }
        }

        String url = REQUEST_URL;
        setPost(false);
        setUrl(url);
        addParam(this, "aid", ads.getAppID());
        addParam(this, "po", "" + po);
        //protocol version
        String version = protocolVersion;
        addParam(this, "v", version);
        if ("ios".equals(os)) {
            hid = Display.getInstance().getProperty("UDID", null);
        } else {
            hid = Display.getInstance().getProperty("IMEI", null);
        }
        addParam(this, "hid", hid);
        addParam(this, "w", "" + Display.getInstance().getDisplayWidth());
        addParam(this, "h", "" + Display.getInstance().getDisplayHeight());

        //add optional params
        addParam(this, "a", ads.getAge());
        addParam(this, "g", ads.getGender());
        addParam(this, "c", ads.getCategory());
        addParam(this, "l", ads.getLocation());

        addParam(this, "mn", Display.getInstance().getProperty("MSISDN", null));
        String[] keywords = ads.getKeywords();
        if (keywords != null && keywords.length > 0) {
            int klen = keywords.length;
            StringBuilder k = new StringBuilder();
            for (int i = 0; i < klen; i++) {
                k.append(",").append(keywords[i]);
            }
            addParam(this, "k", k.toString().substring(1));
        }
        if (testAds) {
            addParam(this, "test", "1");
        }
        setDuplicateSupported(true);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InnerActive)) {
            return false;
        }
        InnerActive that = (InnerActive) o;
        return super.equals(o) &&
                po == that.po &&
                banner == that.banner &&
                (os == null ? that.os == null : os.equals(that.os)) &&
                (hid == null ? that.hid == null : hid.equals(that.hid));
    }

    /// {@inheritDoc}
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + po;
        result = 31 * result + (os != null ? os.hashCode() : 0);
        result = 31 * result + (hid != null ? hid.hashCode() : 0);
        result = 31 * result + (banner ? 1 : 0);
        return result;
    }

    /// {@inheritDoc}
    @Override
    protected void readResponse(InputStream input) throws IOException {
        StringBuffer buf = new StringBuffer();
        byte[] buffer = new byte[256];
        int len;
        while ((len = input.read(buffer)) > 0) {
            String temp = new String(buffer, 0, len, "UTF-8");
            int endOfFile = temp.indexOf("/html>");
            if (endOfFile > 0) {
                buf.append(temp.toCharArray(), 0, endOfFile + 6);
                break;
            } else {
                buf.append(temp);
            }
        }
        String s = buf.toString();
//        if(s.indexOf("ci=") > -1){
//            String ci = s.substring(s.indexOf("ci=") + 3, s.length());
//            cid = ci.substring(0, ci.indexOf("&"));
//            Storage.getInstance().writeObject("cid", cid);
//            Storage.getInstance().flushStorageCache();            
//            addParam(this, "cid", cid);
//        }
        fireResponseListener(new ActionEvent(s, ActionEvent.Type.Response));
    }

}
