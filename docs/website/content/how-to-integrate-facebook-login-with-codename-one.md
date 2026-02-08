---
title: "How to Integrate Facebook Login with Codename One"
date: 2015-06-26
slug: "how-to-integrate-facebook-login-with-codename-one"
---

# Facebook Login

This tutorial covers how to add facebook connect to your CN1 app

1. [Home](https://beta.codenameone.com/getting-started.html)
2. [Developers](https://beta.codenameone.com/getting-started.html)
3. Facebook Login

Codename One supports facebook Oauth2 login and facebook single sign on for iOS and Android

To get started first you will need to create a facebook app on the facebook developer portal Here: [https://developers.facebook.com/apps/](https://developers.facebook.com/apps/)

Your facebook app should have 3 platforms added on the Settings tab:Website, iOS and Android

Android Settings:

- Enter app package name in the "Google Play Package Name".
- Enter the CN1 activity name in the class name, which is: full class name + "Stub".
- Enter your app key hash: use your app release certificate and do the following in your command tool: **keytool -exportcert -alias (your\_keystore\_alias) -keystore (path\_to\_your\_keystore) | openssl sha1 -binary | openssl base64** This will print out the key hash of your app, copy it and place it in the facebook settings for android.

iOS Settings:

- Enter app package name in the "Bundle ID".
- Enter the iPhone Store ID, once you know it.

The settings page should look like this:

![](/uploads/facebook_settings.png)

In your CodenameOne app do the following: Add facebook.appId build hint to your project properties and in your code do the following:

```

                //use your own facebook app identifiers here   
                //These are used for the Oauth2 web login process on the Simulator.
                String clientId = "1171134366245722";
                String redirectURI = "http://www.codenameone.com/";
                String clientSecret = "XXXXXXXXXXXXXXXXXXXXXXXXXX";
                Login fb = FacebookConnect.getInstance();
                fb.setClientId(clientId);
                fb.setRedirectURI(redirectURI);
                fb.setClientSecret(clientSecret);
                //Sets a LoginCallback listener
                fb.setCallback(...);
                //trigger the login if not already logged in
                if(!fb.isUserLoggedIn()){
                    fb.doLogin();
                }else{
                    //get the token and now you can query the facebook API
                    String token = fb.getAccessToken().getToken();
                    ...
                }
                
```
