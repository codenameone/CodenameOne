package com.codename1.samples;

import com.codename1.io.AccessToken;
import com.codename1.io.Log;
import com.codename1.io.Oauth2;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import static com.codename1.ui.CN.*;

public class Oauth2LoginSampleTest extends UITestBase {
    private static  String REDIRECT_URI = "https://weblite.ca/a/auth4/loggedin.html";
    private AccessToken accessToken;

    @FormTest
    public void testOauth2Login() {
        if (Oauth2.handleRedirect(e->{


            if(e.getSource() instanceof AccessToken) {
                Log.p("Logged in: " + e);
                AccessToken token = (AccessToken)e.getSource();
                setAccessToken(token);
                showLoggedIn();
            } else {
                Log.p("Failed to login " + e.getSource());
                showLogin();

            }
            return;

        })) {
            return;
        }

        showLogin();
        // Smoke test to ensure no exceptions
    }

    private void doLogin() {

        Oauth2.setBackToParent(false);
        Oauth2 login = new Oauth2("https://auth.codenameone.com/auth/realms/Realm/protocol/openid-connect/auth?client_id=cn1cloudapp&scope=openid%20address&response_type=code&state=a2V5Y2xvYWs=", "cn1cloudapp", REDIRECT_URI, "openid address",
                "https://auth.codenameone.com/auth/realms/Realm/protocol/openid-connect/token", null);
        login.setUseRedirectForWeb(true);
        login.showAuthentication(e -> {
            Log.p("Logged in: " + e);
            if(e.getSource() instanceof AccessToken) {
                AccessToken token = (AccessToken)e.getSource();
                setAccessToken(token);
                showLoggedIn();
            }
        });
    }

    private void showLoggedIn() {
        Form f = new Form("Logged In", BoxLayout.y());
        f.add(new Label("You are logged in"));
        f.show();
    }

    private void showLogin() {
        Form f = new Form("Login", BoxLayout.y());
        Button b = new Button("Login");
        b.addActionListener(evt->{
            doLogin();
        });
        f.add(b);
        f.show();
    }

    private void setAccessToken(AccessToken tok) {
        accessToken = tok;
    }
}
