package com.codename1.samples;


import com.codename1.io.AccessToken;
import com.codename1.io.ConnectionRequest;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.ui.Toolbar;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.NetworkEvent;
import com.codename1.social.GoogleConnect;
import com.codename1.social.LoginCallback;
import com.codename1.ui.Button;
import java.util.Map;
import com.codename1.io.JSONParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.social.Login;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.events.ActionEvent;

/**
 * <p>This sample shows the use of the Google SignIn API to login into an app using OAuth2.  Currently this sample only sets the ClientID
 * of GoogleConnect which is all that is required for the Javascript port.  For other ports you will need to set additional information.</p>
 * 
 * <p>Much of this sample is extracted from the Contact <a href="https://github.com/codenameone/codenameone-demos/tree/master/SocialChat">sample application </a>
 * which is described in <a href="https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html">this tutorial.</a></p>
 * 
 * <p>This was developed specifically for testing the Javascript port support for Google login.  You need to set up an OAuth2 client
 * key for a "web browser" app, in the <a href="https://console.developers.google.com/apis">Google API console.</a></p>
 * 
 * <p>Some helpful  background information is also available in <a href="https://developers.google.com/identity/sign-in/web/sign-in">this guide</a>, although
 * this includes lots of information not relevant to Codename One.</p>
 * 
 * 
 */
public class GoogleLoginSample {
    
    // You need to copy the Client ID value of your OAuth2.0 API key here for the sample to work in the Javascript port.
    private static final String CLIENT_ID = "XXXXXXX";
    
    
    
    private Form current;
    private Resources theme;
    private static final String tokenPrefix = "google";

    
    public void init(Object context) {
        //Display.getInstance().setProperty("javascript.google.clientId", CLIENT_ID);
        GoogleConnect.getInstance().setClientId(CLIENT_ID);
        //GoogleConnect.getInstance().setClientSecret(CLIENT_SECRET);
        
        fullName = Preferences.get("fullName", null);
        uniqueId = Preferences.get("uniqueId", null);
        imageURL = Preferences.get("imageURL", null);
        
        //Display.getInstance().setProperty("javascript.google.clientSecret", CLIENT_SECRET);
        // use two network threads instead of one
        updateNetworkThreadCount(2);

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });        
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        
        Login lg = GoogleConnect.getInstance();
        if (!lg.isUserLoggedIn()) {
            showLoginForm();
        } else {
            showContactsForm(new GoogleData());
        }
       
    }
    
    static interface UserData {

        public String getName();

        public String getId();

        public String getImage();

        public void fetchData(String token, Runnable callback);
    }
    
    class GoogleData extends ConnectionRequest implements UserData {
        private Runnable callback;
        private Map<String, Object> parsedData;

        @Override
        public String getName() {
            return (String) parsedData.get("displayName");
        }

        @Override
        public String getId() {
                return parsedData.get("id").toString();
        }

        @Override
        public String getImage() {
            Map<String, Object> imageMeta = ((Map<String, Object>) parsedData.get("image"));
            return (String)imageMeta.get("url");
        }

        @Override
        public void fetchData(String token, Runnable callback) {
            this.callback = callback;
            addRequestHeader("Authorization", "Bearer " + token);
            setUrl("https://www.googleapis.com/plus/v1/people/me");
            setPost(false);
            NetworkManager.getInstance().addToQueue(this);
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            //access token not valid anymore
            if(code >= 400 && code <= 410){
                doLogin(GoogleConnect.getInstance(), this, true);
                return;
            }
            super.handleErrorResponseCode(code, message);
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            JSONParser parser = new JSONParser();
            parsedData = parser.parseJSON(new InputStreamReader(input, "UTF-8"));
        }

        @Override
        protected void postResponse() {
            callback.run();
        }
        
        
    }
    
    private String fullName;
    private String uniqueId;
    private String imageURL;
    void doLogin(Login lg, UserData data, boolean forceLogin) {
        if(!forceLogin) {
            if(lg.isUserLoggedIn()) {
                showContactsForm(data);
                return;
            }

            // if the user already logged in previously and we have a token
            String t = Preferences.get(tokenPrefix + "token", (String)null);
            if(t != null) {
                // we check the expiration of the token which we previously stored as System time
                long tokenExpires = Preferences.get(tokenPrefix + "tokenExpires", (long)-1);
                if(tokenExpires < 0 || tokenExpires > System.currentTimeMillis()) {
                    // we are still logged in
                    showContactsForm(data);
                    return;
                }
            }
        }

        lg.setCallback(new LoginCallback() {
            @Override
            public void loginFailed(String errorMessage) {
                Dialog.show("Error Logging In", "There was an error logging in: " + errorMessage, "OK", null);
            }

            @Override
            public void loginSuccessful() {
                // when login is successful we fetch the full data
                data.fetchData(lg.getAccessToken().getToken(), ()-> {
                    // we store the values of result into local variables
                    uniqueId = data.getId();
                    fullName = data.getName();
                    imageURL = data.getImage();

                    // we then store the data into local cached storage so they will be around when we run the app next time
                    Preferences.set("fullName", fullName);
                    Preferences.set("uniqueId", uniqueId);
                    Preferences.set("imageURL", imageURL);
                    Preferences.set(tokenPrefix + "token", lg.getAccessToken().getToken());

                    // token expiration is in seconds from the current time, we convert it to a System.currentTimeMillis value so we can
                    // reference it in the future to check expiration
                    Preferences.set(tokenPrefix + "tokenExpires", tokenExpirationInMillis(lg.getAccessToken()));
                    showContactsForm(data);
                });
            }
        });
        lg.doLogin();
    }

    long tokenExpirationInMillis(AccessToken token) {
        String expires = token.getExpires();
        if(expires != null && expires.length() > 0) {
            try {
                // when it will expire in seconds
                long l = (long)(Float.parseFloat(expires) * 1000);
                return System.currentTimeMillis() + l;
            } catch(NumberFormatException err) {
                // ignore invalid input
            }
        }
        return -1;
    }
    
    
    public void showContactsForm(UserData data) {
        Form f = new Form("Contact Info");
        f.setToolbar(new Toolbar());
        f.getToolbar().setTitle("Contact Info");
        Form prev = CN.getCurrentForm();
        if (prev != null) {
            f.getToolbar().setBackCommand(new Command("Back") {
                public void actionPerformed(ActionEvent evt) {
                    prev.show();
                }
                
            });
        }
        f.add(new Label("You are logged in as "+fullName));
        
        Button logout = new Button("Log out");
        logout.addActionListener(e->{
            GoogleConnect.getInstance().doLogout();
            Preferences.set(tokenPrefix + "tokenExpires", -1);
            Preferences.set(tokenPrefix + "token", (String)null);
            showLoginForm();
        });
        f.add(logout);
        f.show();
        
        
    }

    
    
    public void showLoginForm() {
        Login lg = GoogleConnect.getInstance();
        Form hi = new Form("Hi World", BoxLayout.y());
        Button login = new Button("Login");
        login.addActionListener(e->{
            doLogin(lg, new GoogleData(), false);
        });
        hi.add(login);
        hi.show();
    }

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }
    
    public void destroy() {
    }

}
