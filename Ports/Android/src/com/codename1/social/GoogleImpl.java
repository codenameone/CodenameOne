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
import android.os.AsyncTask;
import android.os.Bundle;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.impl.android.IntentResultListener;
import com.codename1.impl.android.LifecycleListener;
import com.codename1.io.AccessToken;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import java.io.IOException;

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
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()){
            return getAccessToken() != null;
        }
        return false;
    }

    @Override
    public void nativelogin() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void nativeLogout() {
        if (mGoogleApiClient != null) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
    }

    public void onConnected(Bundle bundle) {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {

                    Context ctx = AndroidNativeUtil.getActivity();
                    token = GoogleAuthUtil.getToken(
                            ctx,
                            Plus.AccountApi.getAccountName(mGoogleApiClient),
                            "oauth2:"
                            + Scopes.PLUS_LOGIN + " "
                            + Scopes.PLUS_ME);
                    setAccessToken(new AccessToken(token, null));

                } catch (IOException transientEx) {
                    transientEx.printStackTrace();
                    // Network or server error, try later
                    //Log.e(TAG, transientEx.toString());
                } catch (UserRecoverableAuthException e) {
                    // Recover (with e.getIntent())
                    Intent recover = e.getIntent();
                    AndroidNativeUtil.startActivityForResult(recover, new IntentResultListener() {

                        public void onActivityResult(int requestCode, int resultCode, Intent data) {
                        }
                    });
                } catch (GoogleAuthException authEx) {
                    authEx.printStackTrace();
                    // The call is not ever expected to succeed
                    // assuming you have already verified that 
                    // Google Play services is installed.
                }

                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                if (callback != null) {
                    callback.loginSuccessful();
                }
            }

        };
        task.execute();

    }

    public void onConnectionSuspended(int i) {
    }

    public void onConnectionFailed(ConnectionResult cr) {

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
            callback.loginFailed(GooglePlayServicesUtil.getErrorString(cr.getErrorCode()));
        }
    }

    public void onCreate(Bundle savedInstanceState) {
    }

    public void onResume() {
        Context ctx = AndroidNativeUtil.getActivity();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Plus.API, Plus.PlusOptions.builder().build())
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .addScope(Plus.SCOPE_PLUS_PROFILE)
                    .build();
        }
    }

    public void onPause() {
    }

    public void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }        
    }

    public void onSaveInstanceState(Bundle b) {
    }

    public void onLowMemory() {
    }

}
