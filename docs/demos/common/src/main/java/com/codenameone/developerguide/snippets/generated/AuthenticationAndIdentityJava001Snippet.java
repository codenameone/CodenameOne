package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.util.SuccessCallback;

class AuthenticationAndIdentityJava001Snippet {

    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    void snippet() throws Exception {
        // tag::authentication-and-identity-java-001[]
        OidcClient.discover("https://accounts.google.com").ready(new SuccessCallback<OidcClient>() {
            public void onSucess(OidcClient client) {
                client.setClientId("YOUR_CLIENT_ID")
                      .setRedirectUri("com.example.app:/oauth2redirect")
                      .setScopes("openid", "email", "profile");
                client.authorize().ready(new SuccessCallback<OidcTokens>() {
                    public void onSucess(OidcTokens tokens) {
                        // tokens.getAccessToken() -- bearer for API calls
                        // tokens.getIdToken()      -- JWT with user identity claims
                        // tokens.getEmail()        -- convenience accessor
                    }
                }).except(new SuccessCallback<Throwable>() {
                    public void onSucess(Throwable err) {
                        System.out.println("Sign-in failed: " + err.getMessage());
                    }
                });
            }
        });
        // end::authentication-and-identity-java-001[]
    }
}
