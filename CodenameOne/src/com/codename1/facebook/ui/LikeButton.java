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
package com.codename1.facebook.ui;

import com.codename1.facebook.FaceBookAccess;
import com.codename1.ui.Button;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.io.IOException;

/**
 * Generic "Like" button that enables us to submit a like to facebook, to get the post id
 * required for the like functionality you will need to use a tool such as: 
 * https://developers.facebook.com/tools/explorer/?method=GET&path=me%2Fposts
 * You can ask it to list your posts and then seek the correct id within the returned JSON
 * 
 * @author Chen Fishbein
 */
public class LikeButton extends Button implements ActionListener {
    private String appId = "132970916828080";
    private String redirectURI = "https://www.codenameone.com/";
    private String clientSecret = "6aaf4c8ea791f08ea15735eb647becfe";
    private String[] permissions;
    private String postId = "290052831046005_244933438934534";
    
    /**
     * Constructor accepting the post id
     * 
     * @param postId 
     */
    public LikeButton(String postId) {
        this.postId = postId;
    }

    /**
     * Default constructor
     */
    public LikeButton() {
        setUIID("LikeButton");
        setText("Like");
        addActionListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent evt) {
        if(!FaceBookAccess.getInstance().isAuthenticated()) {
            FaceBookAccess.setClientId(appId);
            FaceBookAccess.setRedirectURI(redirectURI);
            FaceBookAccess.setClientSecret(clientSecret);
            if(permissions != null) {
                FaceBookAccess.setPermissions(permissions);            
            }
            FaceBookAccess.getInstance().showAuthentication(this);
            return;
        }
        if(evt.getSource() instanceof Exception) {
            return;
        }
        try {
            FaceBookAccess.getInstance().postLike(getPostId());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @return the postId
     */
    public String getPostId() {
        return postId;
    }

    /**
     * @param postId the postId to set
     */
    public void setPostId(String postId) {
        this.postId = postId;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[]{"appId", "redirectURI", "clientSecret", "postId", "permissions"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, new String[0].getClass()};
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String", "String", "String", "String", "String[]"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if (name.equals("appId")) {
            return getAppId();
        }
        if (name.equals("redirectURI")) {
            return getRedirectURI();
        }
        if (name.equals("clientSecret")) {
            return getClientSecret();
        }
        if (name.equals("postId")) {
            return getPostId();
        }
        if (name.equals("permissions")) {
            return getPermissions();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if (name.equals("appId")) {
            setAppId((String) value);
            return null;
        }
        if (name.equals("redirectURI")) {
            setRedirectURI((String) value);
            return null;
        }
        if (name.equals("clientSecret")) {
            setClientSecret((String) value);
            return null;
        }
        if (name.equals("postId")) {
            setPostId((String) value);
            return null;
        }
        if (name.equals("permissions")) {
            setPermissions((String[]) value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * @return the appId
     */
    public String getAppId() {
        return appId;
    }

    /**
     * @param appId the appId to set
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * @return the redirectURI
     */
    public String getRedirectURI() {
        return redirectURI;
    }

    /**
     * @param redirectURI the redirectURI to set
     */
    public void setRedirectURI(String redirectURI) {
        this.redirectURI = redirectURI;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret the clientSecret to set
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * @return the permissions
     */
    public String[] getPermissions() {
        return permissions;
    }

    /**
     * @param permissions the permissions to set
     */
    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }
}
