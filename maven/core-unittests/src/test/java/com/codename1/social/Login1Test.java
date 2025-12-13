package com.codename1.social;

import com.codename1.io.AccessToken;
import com.codename1.io.Oauth2;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

public class Login1Test extends UITestBase {

    @FormTest
    public void testLoginActionListener() {
        Login login = new Login() {
            @Override
            public boolean isNativeLoginSupported() {
                return false;
            }

            @Override
            protected boolean validateToken(String token) {
                return false;
            }

            @Override
            protected Oauth2 createOauth2() {
                return new Oauth2("url", "client", "redirect", "scope") {
                     @Override
                     public void showAuthentication(ActionListener al) {
                         // Test success with AccessToken
                         AccessToken token = new AccessToken("token", null);
                         al.actionPerformed(new ActionEvent(token));

                         // Test success with String
                         al.actionPerformed(new ActionEvent("tokenString"));

                         // Test failure with Exception
                         al.actionPerformed(new ActionEvent(new Exception("Fail")));
                     }
                };
            }
        };

        login.setClientId("id");
        login.setClientSecret("secret");
        login.setOauth2URL("url");
        login.setRedirectURI("uri");

        login.doLogin();
    }
}
