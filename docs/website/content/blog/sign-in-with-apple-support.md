---
title: Sign-in with Apple Support
slug: sign-in-with-apple-support
url: /blog/sign-in-with-apple-support/
original_url: https://www.codenameone.com/blog/sign-in-with-apple-support.html
aliases:
- /blog/sign-in-with-apple-support.html
date: '2020-01-30'
author: Steve Hannah
---

![Header Image](/blog/sign-in-with-apple-support/sign-in-with-apple-header.jpg)

We have just finished the initial release of our [“Sign-in with Apple” cn1lib](https://github.com/shannah/cn1-applesignin), which adds “Sign-in with Apple” support to Codename One apps. On iOS 13 and higher, this will use Apple’s native Authentication framework. On other platforms (e.g. Android, Desktop, and Simulator), this will use Apple’s Oauth2 authentication service.

The main motivation for adding this functionality is that Apple would require apps that use “sign in with…​” to support its service too. If you won’t support sign in with Apple but include support signin with Facebook/Google your app might be rejected in the future.

__ |  If your app doesn’t require sign-in or uses custom login logic there’s no requirement to support “sign in with Apple”   
---|---  
  
## Getting Started

The hardest part of adding Apple sign-in support house-keeping you need to perform in Apple’s developer portal. If you only intend to support Apple sign-in with your iOS app, and not on other platforms, then the process is pretty simple – you just check a box next to “Sign-in with Apple” in the capabilites section of your App ID details page. Set-up for other platforms is a bit more involved. You need to create a “Services ID” (used for the Oauth2 client ID), and generate a private key so you will be able to generate the Oauth2 client secret on-demand.

For full instructions see the [setup documentation in the cn1lib’s wiki](https://github.com/shannah/cn1-applesignin/wiki/Getting-Started).

You can find the plugin in Codename One Settings. Once you’ve added the cn1lib to your Codename One project, you can begin using the `AppleLogin` class to provide Apple sign-in support.

## The Code

The following is an example of how to add Apple login support to your app.
    
    
    AppleLogin login = new AppleLogin();
    // If using on non-iOS platforms, set Oauth2 settings here:
    // login.setClientId(...);
    // login.setKeyId(...);
    // login.setTeamId(...);
    // login.setRedirectURI(...);
    // login.setPrivateKey(...);
    
    if (login.isUserLoggedIn()) {
        new MainForm().show();
    } else {
        new LoginForm().show();
    }
    
    
    ....
    
    
    class LoginForm extends Form {
        LoginForm() {
            super(BoxLayout.y());
            $(getContentPane()).setPaddingMillimeters(3f, 0, 0, 0);
            add(FlowLayout.encloseCenter(new Label(AppleLogin.createAppleLogo(0x0, 15f))));
    
    
            Button loginBtn = new Button("Sign in with Apple");
            AppleLogin.decorateLoginButton(loginBtn, 0x0, 0xffffff);
    
            loginBtn.addActionListener(evt->{
                login.doLogin(new LoginCallback() {
                    @Override
                    public void loginFailed(String errorMessage) {
                        System.out.println("Login failed");
                        ToastBar.showErrorMessage(errorMessage);
                    }
    
                    @Override
                    public void loginSuccessful() {
                        new MainForm().show();
                    }
                });
            });
    
            add(FlowLayout.encloseCenter(loginBtn));
    
    
        }
    }
    
    
    ....
    
    class MainForm extends Form {
        MainForm() {
            super(BoxLayout.y());
            add(new SpanLabel("You are now logged in as "+login.getEmail()));
            Button logout = new Button("Logout from Apple");
            logout.addActionListener(e->{
                login.doLogout();
                new LoginForm().show();
            });
            add(logout);
        }
    }

For full working demo, see the [Demo app](https://github.com/shannah/cn1-applesignin/tree/master/CN1AppleSignInDemo)

Some screenshots from the demo:

![Sign-in with apple demo](/blog/sign-in-with-apple-support/iOS-Screenshots.png)

## More information

  1. See the [Github project for the Apple Sign-in cn1lib](https://github.com/shannah/cn1-applesignin).

  2. See the [Demo project](https://github.com/shannah/cn1-applesignin/tree/master/CN1AppleSignInDemo) for an example.

  3. See the [set-up instructions](https://github.com/shannah/cn1-applesignin/wiki/Getting-Started) detailed instructions on adding support for Apple Sign-in.

  4. See [Apple’s documentation for Sign-in with Apple](https://developer.apple.com/sign-in-with-apple/).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Javier Anton** — April 1, 2020 at 12:30 pm ([permalink](https://www.codenameone.com/blog/sign-in-with-apple-support.html#comment-21373))

> This cn1 lib breaks the host app that imports it at compile time. The reason is that the native javase bit of the lib has incorrect package names and some imports aren’t available (in AppleSignInNativeImpl and WebViewBrowserWindow). The host app will compile when manually commenting these things out, but it might be worth updating the cn1lib so it doesn’t throw these errors as currently the native interface gets reset to faulty code each time the cn1lib is refreshed
>
> Also, even after doing these things the following error is thrown on the server when compiling AppleLogin:  
> com_codename1_social_AppleLogin.m:22:10: fatal error: ‘java_io_StringReader.h’ file not found  
> “#”include “java_io_StringReader.h”
>
> Could it be that java.io.StringReader was used instead of com.codename1.util .regex.StringReader?
>



### **Steve Hannah** — April 1, 2020 at 1:52 pm ([permalink](https://www.codenameone.com/blog/sign-in-with-apple-support.html#comment-21375))

> [Steve Hannah](https://lh3.googleusercontent.com/a-/AAuE7mBmUCgKSZtJ2cqeHgj6bdPY2AAQ10roHlMpgRWc) says:
>
> I have replied inside the issue you opened in the issue tracker. <https://github.com/codenameone/CodenameOne/issues/3068>
>
> The gist of it is that the build.xml file needs to be updated, and you need to run an update libs. This won’t be needed for long as the build.xml file will come updated with the next plugin update.
>



### **Javier Anton** — April 1, 2020 at 2:40 pm ([permalink](https://www.codenameone.com/blog/sign-in-with-apple-support.html#comment-21374))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> I’ll wait until the next update then as I don’t need this right now, was just toying with it in preparation of Apple making this a requirement (apparently that has been rolled back to June last I heard because of the coronavirus crisis)  
> Thanks for this
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
