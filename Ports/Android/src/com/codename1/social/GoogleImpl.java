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

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.impl.android.IntentResultListener;
import com.codename1.impl.android.LifecycleListener;
import com.codename1.io.AccessToken;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.plus.Plus;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
//import com.google.android.gms.plus.Plus;
//import com.google.android.gms.plus.model.people.Person;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is an implementation to the google sign in.
 * https://developers.google.com/+/mobile/android/getting-started
 *
 * @author Chen
 */
public class GoogleImpl extends GoogleConnect implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LifecycleListener {

    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient mGoogleApiClient;

    private boolean mIntentInProgress;

    public GoogleImpl() {
    }

    public static void init() {
        GoogleConnect.implClass = GoogleImpl.class;
    }

    @Override
    public boolean isNativeLoginSupported() {
        return true;
    }

    @Override
    public boolean nativeIsLoggedIn() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            return getAccessToken() != null;
        }
        return false;
    }

    
    private void nativeLoginImpl(final GoogleApiClient client) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(client);
        AndroidNativeUtil.startActivityForResult(signInIntent, RC_SIGN_IN, new IntentResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {

                // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
                if (requestCode == RC_SIGN_IN) {
                    final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (result.isSuccess()) {
                        // Signed in successfully, show authenticated UI.
                        GoogleSignInAccount acct = result.getSignInAccount();
                        String displayName = acct.getDisplayName();
                        String acctId = acct.getId();
                        String email = acct.getEmail();
                        String requestIdToken = acct.getIdToken();
                        Set<Scope> grantedScopes = acct.getGrantedScopes();
                        String code = acct.getServerAuthCode();
                        String scopeStr = scope;
                        System.out.println("Token is "+acct.getIdToken());
                        if (acct.getIdToken() == null && clientId != null && clientSecret != null) {
                            Log.p("Received null ID token even though clientId and clientSecret are set.");
                        }
                        // In order to use Google's REST APIs, we'll need to request a token
                        // that can be used for OAuth2 requests.  The requestId token we received
                        // doesn't do this, but we can use it to request a token that does.
                        // We'll only request this token if the clientId and clientSecret were
                        // supplied (these should be the client ID and client secret for web clients)
                        // otherwise we'll set the token to null.
                        if (clientId != null && clientSecret != null && requestIdToken != null && code != null) {
                            ConnectionRequest req = new ConnectionRequest() {
                                @Override
                                protected void readResponse(InputStream input) throws IOException {
                                    Map<String, Object> json = new JSONParser().parseJSON(new InputStreamReader(input, "UTF-8"));
                                    if (json.containsKey("access_token")) {
                                        setAccessToken(new AccessToken((String) json.get("access_token"), null));
                                        Display.getInstance().callSerially(new Runnable() {

                                            @Override
                                            public void run() {
                                                callback.loginSuccessful();
                                            }
                                        });
                                    } else {
                                        setAccessToken(new AccessToken(null, null));
                                        Log.p("Failed to retrieve the access token from the google auth server.  Login succeeded, but access token is null, so you won't be able to use it to retrieve additional information.");
                                        Log.p("Response was " + json);
                                        Display.getInstance().callSerially(new Runnable() {

                                            @Override
                                            public void run() {
                                                callback.loginSuccessful();
                                            }
                                        });
                                    }
                                }
                            };
                            req.setUrl("https://www.googleapis.com/oauth2/v4/token");
                            req.addArgument("grant_type", "authorization_code");
                            //req.addArgument("client_id", "555462747934-iujpd5saj4pjpibo7c6r9tbjfef22rh1.apps.googleusercontent.com");
                            req.addArgument("client_id", clientId);
                            //req.addArgument("client_secret", "650YqplrnAI0KXb9LMUnVNnx");
                            req.addArgument("client_secret", clientSecret);
                            req.addArgument("redirect_uri", "");
                            req.addArgument("code", code);
                            req.addArgument("id_token", requestIdToken);
                            req.setPost(true);
                            req.setReadResponseForErrors(true);
                            NetworkManager.getInstance().addToQueue(req);
                        } else {
                            setAccessToken(new AccessToken(null, null));
                            Log.p("The access token was set to null because one of clientId, clientSecret, requestIdToken, or auth were null");
                            Log.p("The login succeeded, but you won't be able to make any requests to Google's REST apis using the login token.");
                            Log.p("In order to obtain a token that can be used with Google's REST APIs, you need to set the clientId, and clientSecret of" +
                                    "the GoogleConnect instance to valid OAuth2.0 Client IDs for Web Clients.");
                            Log.p("See https://console.developers.google.com/apis/credentials");
                            Log.p("You can get the OAuth2.0 client ID for this project in your google-services.json file in the oauth_client section");

                            Display.getInstance().callSerially(new Runnable() {

                                @Override
                                public void run() {
                                    callback.loginSuccessful();
                                }
                            });
                        }

                    } else {
                        if (callback != null) {
                            if (callback != null) {
                                Display.getInstance().callSerially(new Runnable() {

                                    @Override
                                    public void run() {
                                        callback.loginFailed(GooglePlayServicesUtil.getErrorString(result.getStatus().getStatusCode()));
                                    }
                                });
                            }
                        }
                    }

                }
            }
        });
        
    }
    
    @Override
    public void nativelogin() {
        //if(!checkForPermission(Manifest.permission.GET_ACCOUNTS, "This is required to login with Google+")) {
        //    return;
        //}
        getClient(new SuccessCallback<GoogleApiClient>() {

            @Override
            public void onSucess(GoogleApiClient client) {
                nativeLoginImpl(client);
            }
            
        });
        
    }
    
    private List<SuccessCallback<GoogleApiClient>> onConnectedCallbacks = new ArrayList<SuccessCallback<GoogleApiClient>>();
    
    private GoogleApiClient getClient(SuccessCallback<GoogleApiClient> onConnected) {
        if (mGoogleApiClient == null) {
            Context ctx = AndroidNativeUtil.getContext();
            if (mGoogleApiClient == null) {
                GoogleSignInOptions gso;

                if (clientId != null && clientSecret != null) {
                    System.out.println("Generating GoogleSignIn for clientID="+clientId);
                    gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            //.requestIdToken("555462747934-iujpd5saj4pjpibo7c6r9tbjfef22rh1.apps.googleusercontent.com")
                            .requestIdToken(clientId)
                            //.requestScopes(Plus.SCOPE_PLUS_PROFILE)
                            //.requestServerAuthCode("555462747934-iujpd5saj4pjpibo7c6r9tbjfef22rh1.apps.googleusercontent.com")
                            .requestServerAuthCode(clientId)
                            .build();
                } else {
                    System.out.println("Generating GoogleSignIn without ID token");
                    gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
                }
                mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                        .build();
            }
        }
        if (mGoogleApiClient.isConnected()) {
            if (onConnected != null) {
                onConnected.onSucess(mGoogleApiClient);
            }
        } else {
            synchronized(onConnectedCallbacks) {
                if (onConnected != null) {
                    onConnectedCallbacks.add(onConnected);
                }
                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
                }
            }
        }
        return mGoogleApiClient;
    }
    
    
    @Override
    public void nativeLogout() {
        
        getClient(new SuccessCallback<GoogleApiClient>() {

            @Override
            public void onSucess(final GoogleApiClient client) {
                Auth.GoogleSignInApi.signOut(client).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            Log.p("Finished signing out "+status);
                            setAccessToken(null);
                            mGoogleApiClient = null;
                            client.disconnect();
                        }
                    });
            }
            
        });
    }

    public void onConnected(Bundle bundle) {
        List<SuccessCallback<GoogleApiClient>> callbacks;
    
        synchronized(onConnectedCallbacks) {
            if (!onConnectedCallbacks.isEmpty()) {
                callbacks = new ArrayList<SuccessCallback<GoogleApiClient>>(onConnectedCallbacks);
                onConnectedCallbacks.clear();
            } else {
                callbacks = new ArrayList<SuccessCallback<GoogleApiClient>>();
            }
        }
        GoogleApiClient client = mGoogleApiClient;
        for (SuccessCallback<GoogleApiClient> cb : callbacks) {
            cb.onSucess(client);
        }

    }

    public void onConnectionSuspended(int i) {
    }

    public void onConnectionFailed(final ConnectionResult cr) {
        if (AndroidNativeUtil.getActivity() == null) {
            return;
        }
        final CodenameOneActivity main = (CodenameOneActivity) AndroidNativeUtil.getActivity();

        if (!mIntentInProgress && cr.hasResolution()) {
            try {
                mIntentInProgress = true;
                main.startIntentSenderForResult(cr.getResolution().getIntentSender(),
                        0, null, 0, 0, 0);
                main.setIntentResultListener(new com.codename1.impl.android.IntentResultListener() {

                    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
                        mIntentInProgress = false;
                        if (!mGoogleApiClient.isConnecting()) {
                            mGoogleApiClient.connect();
                        }
                        main.restoreIntentResultListener();
                    }
                });

            } catch (SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
            return;
        }
        if (callback != null) {
            Display.getInstance().callSerially(new Runnable() {

                @Override
                public void run() {
                    callback.loginFailed(GooglePlayServicesUtil.getErrorString(cr.getErrorCode()));
                }
            });
        }
    }

    private boolean wasConnected;
    public void onCreate(Bundle savedInstanceState) {
        clientId = savedInstanceState.getString("clientId");
        clientSecret = savedInstanceState.getString("clientSecret");
        wasConnected = savedInstanceState.getBoolean("isConnected", false);
    }

    public void onResume() {
        if (wasConnected) {
            wasConnected = false;
            getClient(null);
        }
    }

    public void onPause() {
    }

    public void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
    }

    public void onSaveInstanceState(Bundle b) {
        b.putString("clientId", clientId);
        b.putString("clientSecret", clientSecret);
        b.putBoolean("isConnected", mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    public void onLowMemory() {
    }


}
