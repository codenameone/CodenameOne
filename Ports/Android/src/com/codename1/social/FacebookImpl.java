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

import static android.R.attr.description;
import static android.R.attr.name;
import static android.R.id.message;
import android.content.Intent;
import android.os.Bundle;
import com.codename1.facebook.FaceBookAccess;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.impl.android.IntentResultListener;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class implementing the facebook API
 *
 * @author Shai Almog
 */
public class FacebookImpl extends FacebookConnect {

    private SessionDefaultAudience defaultAudience = SessionDefaultAudience.FRIENDS;
    private static List<String> permissions;
    private SessionLoginBehavior loginBehavior = SessionLoginBehavior.SSO_WITH_FALLBACK;
    private boolean publish = false;
    private boolean loginLock = false;
    private static final List<String> PUBLISH_PERMISSIONS = Arrays.asList("publish_actions");
    private boolean pendingPublishReauthorization = false;

    public static void init() {
        FacebookConnect.implClass = FacebookImpl.class;
        permissions = new ArrayList<String>();
        String permissionsStr = Display.getInstance().getProperty("facebook_permissions", "");
        permissionsStr = permissionsStr.trim();
        
        StringTokenizer token = new StringTokenizer(permissionsStr, ",");
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
        AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Session s = Session.getActiveSession();
                if (s == null) {
                    Log.p("CN1 Creating new session");
                    s = new Session.Builder(AndroidNativeUtil.getActivity()).setApplicationId(Display.getInstance().getProperty("facebook_app_id", "")).build();
                    Session.setActiveSession(s);
                }
                if (s.isOpened()) {
                    Log.p("CN1 Login session already open");
                    loginLock = false;
                    return;
                }
                final CodenameOneActivity cn = (CodenameOneActivity) AndroidNativeUtil.getActivity();
                cn.setIntentResultListener(new IntentResultListener() {
                    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                        Session s = Session.getActiveSession();
                        if (s != null) {
                            s.onActivityResult(AndroidNativeUtil.getActivity(), requestCode, resultCode, data);
                        }
                        cn.restoreIntentResultListener();
                    }
                });
                Session.OpenRequest openRequest = new Session.OpenRequest(cn);
                openRequest.setDefaultAudience(defaultAudience);
                openRequest.setPermissions(permissions);
                openRequest.setLoginBehavior(loginBehavior);
                openRequest.setCallback(new Session.StatusCallback() {

                    @Override
                    public void call(Session session, SessionState state, final Exception exception) {
                        loginLock = false;
                        if (exception != null) {
                            Log.p("Login calback exception");
                            Log.e(exception);
                            if (cb != null) {
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        Session.setActiveSession(null);
                                        cb.loginFailed(exception.toString());
                                    }
                                });
                            }
                            cn.restoreIntentResultListener();
                            return;
                        }
                        Log.p("CN1 Facebook session status callback " + state);
                        if (state == SessionState.OPENED) {
                            FaceBookAccess.setToken(session.getAccessToken());
                            if (cb != null) {
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        Log.p("CN1 Facebook loginSuccessful");
                                        cb.loginSuccessful();
                                    }
                                });
                            }
                            cn.restoreIntentResultListener();
                        }
                    }
                });

                if (publish) {
                    s.openForPublish(openRequest);
                } else {
                    s.openForRead(openRequest);
                }
            }
        });

    }

    @Override
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    @Override
    public String getToken() {
        final String[] arr = new String[1];
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            @Override
            public void run() {
                Session s = Session.getActiveSession();
                if (s != null && s.isOpened()) {
                    arr[0] = s.getAccessToken();
                }
            }
        });
        return arr[0];
    }

    @Override
    public void logout() {
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            @Override
            public void run() {
                Session s = Session.getActiveSession();
                if (s.isOpened()) {
                    s.closeAndClearTokenInformation();
                    Session.setActiveSession(null);
                }
            }
        });
    }

    public void askPublishPermissions(final LoginCallback lc) {
        AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {

            public void run() {

                //try to login if not already logged in
                Session session = Session.getActiveSession();
                if (session == null) {
                    login(new LoginCallback() {

                        @Override
                        public void loginSuccessful() {
                            Log.p("CN1 askPublishPermissions");
                            askPublishPermissions(lc);
                        }

                    });
                    return;
                }
                if (session != null) {
                    //Session.openActiveSessionFromCache(AndroidNativeUtil.getActivity());
                    // Check for publish permissions    
                    List<String> permissions = session.getPermissions();
                    if (!isSubsetOf(PUBLISH_PERMISSIONS, permissions)) {
                        pendingPublishReauthorization = true;
                        final CodenameOneActivity cn = (CodenameOneActivity) AndroidNativeUtil.getActivity();
                        cn.setIntentResultListener(new IntentResultListener() {
                            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                                Session s = Session.getActiveSession();
                                if (s != null) {
                                    s.onActivityResult(AndroidNativeUtil.getActivity(), requestCode, resultCode, data);
                                }
                                cn.restoreIntentResultListener();
                            }
                        });
                        //Session.OpenRequest openRequest = new Session.OpenRequest(cn);
                        Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(cn, PUBLISH_PERMISSIONS);
                        newPermissionsRequest.setCallback(new Session.StatusCallback() {

                            public void call(Session session, SessionState state, final Exception exception) {

                                if (exception != null) {
                                    Log.p("CN1 Write Permissions calback exception");
                                    Log.e(exception);
                                    Display.getInstance().callSerially(new Runnable() {
                                        public void run() {
                                            lc.loginFailed(exception.getMessage());
                                        }
                                    });

                                    cn.restoreIntentResultListener();
                                    return;
                                }
                                Log.p("CN1 askPublishPermissions");
                                //if permissions granted call the method again to 
                                //post the message on the wall.
                                if (state == SessionState.OPENED) {
                                    Display.getInstance().callSerially(new Runnable() {
                                        public void run() {
                                            lc.loginSuccessful();
                                        }
                                    });
                                    cn.restoreIntentResultListener();
                                }
                            }
                        });
                        session.requestNewPublishPermissions(newPermissionsRequest);
                        return;
                    }

                }
            }

        });

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

        Session session = Session.getActiveSession();

        if (session != null) {
            // Check for publish permissions    
            List<String> permissions = session.getPermissions();
            return isSubsetOf(PUBLISH_PERMISSIONS, permissions);

        }
        return false;

    }

}
