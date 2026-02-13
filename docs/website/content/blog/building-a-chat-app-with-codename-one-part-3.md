---
title: Building A Chat App With Codename One Part 3
slug: building-a-chat-app-with-codename-one-part-3
url: /blog/building-a-chat-app-with-codename-one-part-3/
original_url: https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-3.html
aliases:
- /blog/building-a-chat-app-with-codename-one-part-3.html
date: '2015-07-28'
author: Shai Almog
---

![Header Image](/blog/building-a-chat-app-with-codename-one-part-3/facebook-login-blue.png)

In the previous section we went over the login with Google process, in this section we’ll go over the login with Facebook.  
At this time we’ll skip the “invite friends” option since that is blog post all on its own and we can just add that functionality  
to the completed application.

Facebook has a “get friends” API call that we can use, the downside of that is that it will only return our  
friends that have already joined the app so we won’t be able to contact anyone on an arbitrary basis.

### Getting Started – Configuration

Getting started with Facebook is pretty similar to the Google process and you can learn about it [here](http://www.codenameone.com/facebook-login.html).

You need to go to <https://developers.facebook.com/apps/> and signup to create an application:

![Create New App](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-1.png)

You need to repeat the process for web, Android & iOS (web is used by the simulator):

![Pick Platform](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-2.png)

For the first platform you need to enter the app name:

![Pick Name](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-3.png)

And provide some basic details:

![Details](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-4.png)

For iOS we need the bundle ID which is the exact same thing we used in the Google+ login to identify the iOS app  
its effectively your package name:

![Details](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-5.png)

You should end up with something that looks like this:

![Details](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-6.png)

The Android process is pretty similar but in this case we need the activity name too.

Important: notice there is a mistake in the screenshot, the activity name should match the main class name followed  
by the word Stub (uppercase s). In this case it should be SocialChatStub.

![Details](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-7.png)

Just like on Google+ we need to generate a hash to verify that the app is indeed ours to Facebook and we do this  
using the same keytool approach using the command line:
    
    
    keytool -exportcert -alias (your_keystore_alias) -keystore (path_to_your_keystore) | openssl sha1 -binary | openssl base64

![Hash](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-8.png)

Lastly you need to publish the Facebook app by flipping the switch in the apps “Status & Review” page as such:

![Enable The App](/blog/building-a-chat-app-with-codename-one-part-3/chat-app-tutorial-facebook-login-9.png)

### Project Configuration

We now need to set some important build hints in the project so it will work correctly. To set the build hints just right click the project  
select project properties and in the Codename One section pick the second tab. Add these entries into the table:
    
    
    facebook.appId=...

The app ID will be visible in your Facebook app page in the top left position.

### The Code

So now that all of that is in place we would want it to work with our app…​

Add the event handling for logging in with Facebook as such:
    
    
    loginWithFacebook.addActionListener((e) -> {
        tokenPrefix = "facebook";
        Login fb = FacebookConnect.getInstance();
        fb.setClientId("739727009469185");
        fb.setRedirectURI("http://www.codenameone.com/");
        fb.setClientSecret("-------");
        doLogin(fb, new FacebookData(), false);
    });

Notice that the client ID, redirect, secret etc. are all relevant to the simulator login and won’t be used on Android/iOS  
where native Facebook login will kick into place.

Similarly to the Google+ implementation we’ll create a class to abstract the Facebook connection based on the interface  
we defined the last time around:
    
    
    class FacebookData implements UserData {
            String name;
            String id;
    
            @Override
            public String getName() {
                return name;
            }
    
            @Override
            public String getId() {
                return id;
            }
    
            @Override
            public String getImage() {
                return "http://graph.facebook.com/v2.4/" + id + "/picture";
            }
    
            @Override
            public void fetchData(String token, Runnable callback) {
                ConnectionRequest req = new ConnectionRequest() {
                    @Override
                    protected void readResponse(InputStream input) throws IOException {
                        JSONParser parser = new JSONParser();
                        Map<String, Object> parsed = parser.parseJSON(new InputStreamReader(input, "UTF-8"));
                        name = (String) parsed.get("name");
                        id = (String) parsed.get("id");
                    }
    
                    @Override
                    protected void postResponse() {
                        callback.run();
                    }
    
                    @Override
                    protected void handleErrorResponseCode(int code, String message) {
                        //access token not valid anymore
                        if(code >= 400 && code <= 410){
                            doLogin(FacebookConnect.getInstance(), FacebookData.this, true);
                            return;
                        }
                        super.handleErrorResponseCode(code, message);
                    }
                };
                req.setPost(false);
                req.setUrl("https://graph.facebook.com/v2.4/me");
                req.addArgumentNoEncoding("access_token", token);
                NetworkManager.getInstance().addToQueue(req);
            }
    }

This is really trivial code, we just connect to Facebooks Graph API and provide the token. From here on its  
just a matter of parsing the returned data which contains only two keys for the user name and the unique id  
which we can use later on when setting up a chat.

That’s it for Facebook login, next time we’ll get into accessing the contacts and showing the contacts form UI.

__ |  the original post discussed using the email address as the unique user ID which was the initial direction  
we were going for. But we since decided to go in a different route with user ID’s (similar to the Facebook experience).  
We also made some minor changes to error handling logic making the code more robust.   
---|---  
  
### Other Posts In This Series

This is a multi-part series of posts including the following parts:

  * [Part 1 – Initial UI](/blog/building-a-chat-app-with-codename-one-part-1.html)

  * [Part 2 – Login With Google](/blog/building-a-chat-app-with-codename-one-part-2.html)

  * [Part 3 – Login With Facebook](/blog/building-a-chat-app-with-codename-one-part-3.html)

  * [Part 4 – The Contacts Form](/blog/building-a-chat-app-with-codename-one-part-4.html)

  * [Part 5 – The Chat Form](/blog/building-a-chat-app-with-codename-one-part-5.html)

  * [Part 6 – Native Push & Finishing Up](/blog/building-a-chat-app-with-codename-one-part-6.html)

You can check out the final source code of this tutorial [here](https://github.com/codenameone/codenameone-demos/tree/master/SocialChat).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
