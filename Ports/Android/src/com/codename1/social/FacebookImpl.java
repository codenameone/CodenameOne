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
package com.codename1.social;

import android.content.Intent;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.impl.android.IntentResultListener;
import com.codename1.ui.Display;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Class implementing the facebook API
 *
 * @author Shai Almog
 */
public class FacebookImpl extends FacebookConnect {

    private static List<String> permissions;
    private boolean loginLock = false;
    private static final List<String> PUBLISH_PERMISSIONS = Arrays.asList("publish_actions");

    public static void init() {
        FacebookConnect.implClass = FacebookImpl.class;
        permissions = new ArrayList<String>();
        String permissionsStr = Display.getInstance().getProperty("facebook_permissions", "");
        permissionsStr = permissionsStr.trim();
        
        StringTokenizer token = new StringTokenizer(permissionsStr, ", ");
        if (token.countTokens() > 0) {
            try {
                while (token.hasMoreElements()) {
                    String permission = (String) token.nextToken();
                    permission = permission.trim();
                    permissions.add(permission);
                }
            } catch (Exception e) {
                //the pattern is not valid
            }

        }
        FacebookSdk.sdkInitialize(AndroidNativeUtil.getActivity().getApplicationContext());

    }

    @Override
    public boolean isFacebookSDKSupported() {
        return true;
    }

    @Override
    public void login() {
        login(callback);
    }

    private void login(final LoginCallback cb) {
        if (loginLock) {
            return;
        }
        loginLock = true;
        
        LoginManager login = LoginManager.getInstance();        
        final CallbackManager mCallbackManager = CallbackManager.Factory.create();
        final CodenameOneActivity activity = (CodenameOneActivity)AndroidNativeUtil.getActivity();
        activity.setIntentResultListener(new IntentResultListener() {

            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                mCallbackManager.onActivityResult(requestCode, resultCode, data);
                activity.restoreIntentResultListener();
            }
        });
        login.registerCallback(mCallbackManager, new FBCallback(cb));
        login.logInWithReadPermissions(activity, permissions);
    }

    @Override
    public boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && !accessToken.isExpired();
    }

    @Override
    public String getToken() {
        com.codename1.io.AccessToken t = getAccessToken();
        if(t != null){
            return t.getToken();
        }
        return null;
    }

    @Override
    public com.codename1.io.AccessToken getAccessToken() {
        AccessToken fbToken = AccessToken.getCurrentAccessToken();
        if(fbToken != null){
            String token = fbToken.getToken();
            Date ex = fbToken.getExpires();
            long diff = ex.getTime() - System.currentTimeMillis();
            diff = diff/1000;
            com.codename1.io.AccessToken cn1Token = new com.codename1.io.AccessToken(token, "" + diff);
            return cn1Token;
        }
        return null;
    }
    
    @Override
    public void logout() {
        LoginManager login = LoginManager.getInstance();
        login.logOut();
    }

    public void askPublishPermissions(final LoginCallback cb) {
        if (loginLock) {
            return;
        }
        loginLock = true;
        
        LoginManager login = LoginManager.getInstance();        
        final CallbackManager mCallbackManager = CallbackManager.Factory.create();
        final CodenameOneActivity activity = (CodenameOneActivity)AndroidNativeUtil.getActivity();
        activity.setIntentResultListener(new IntentResultListener() {

            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                mCallbackManager.onActivityResult(requestCode, resultCode, data);
                activity.restoreIntentResultListener();
            }
        });
        login.registerCallback(mCallbackManager, new FBCallback(cb));
        login.logInWithPublishPermissions(activity, PUBLISH_PERMISSIONS);
    }

    private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
        for (String string : subset) {
            if (!superset.contains(string)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the current session already has publish permissions
     *
     * @return
     */
    public boolean hasPublishPermissions() {
        AccessToken fbToken = AccessToken.getCurrentAccessToken();
        if(fbToken != null && !fbToken.isExpired()){
            return fbToken.getPermissions().contains(PUBLISH_PERMISSIONS);
        }
        return false;
    }
   
    @Override
    public void inviteFriends(String appLinkUrl, String previewImageUrl) {
        if (AppInviteDialog.canShow()) {
            AppInviteContent content = new AppInviteContent.Builder()
                    .setApplinkUrl(appLinkUrl)
                    .setPreviewImageUrl(previewImageUrl)
                    .build();
            AppInviteDialog.show(AndroidNativeUtil.getActivity(), content);
        }

    }

    @Override
    public boolean isInviteFriendsSupported(){
        return true;
    }
    
    class FBCallback implements FacebookCallback{
        
        private LoginCallback cb;
        
        public FBCallback(LoginCallback cb) {
            this.cb = cb;
        }

        @Override
        public void onSuccess(Object result) {
            cb.loginSuccessful();
            loginLock = false;
        }

        @Override
        public void onCancel() {
            cb.loginFailed("User cancelled");
            loginLock = false;
        }

        @Override
        public void onError(FacebookException fe) {
            cb.loginFailed(fe.getMessage());
            loginLock = false;
        }
    
    }

}
