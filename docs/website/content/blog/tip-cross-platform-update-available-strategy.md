---
title: 'TIP: Cross Platform Update Available Strategy'
slug: tip-cross-platform-update-available-strategy
url: /blog/tip-cross-platform-update-available-strategy/
original_url: https://www.codenameone.com/blog/tip-cross-platform-update-available-strategy.html
aliases:
- /blog/tip-cross-platform-update-available-strategy.html
date: '2016-11-27'
author: Shai Almog
---

![Header Image](/blog/tip-cross-platform-update-available-strategy/just-the-tip.jpg)

One of the nice things in mobile development vs. desktop is the fact that updates are seamless. We supposedly  
don’t need to worry about them and most newer OS’s turn them on by default. This keeps are users with the latest  
version which is important, e.g. if we fixed a crucial bug or added a new monetization option…​

However, the reality rarely fits into this nice image. In practice an update can be delayed because of permission  
changes user settings and many other problems. As a result old and even discontinued apps can still exist on  
user devices and give you a hard time with user complaints.

__ |  We still get notifications from apps that haven’t been listed in the stores for over 2 years!   
---|---  
  
Over the years we developed a policy of update management that handles all potential edge cases:

  * Update recommended – we have a new update out and we want you to switch to it but it isn’t crucial

  * Update required – if you don’t update now we’d rather the app stops working

  * Switch App recommended – This app was discontinued, we **suggest** you switch to a new app. This can  
happen when a product line changes or even if you lose your certificate an can no longer update the app

  * Switch App required – This app was discontinued but we have this alternative app you can migrate to, the  
app will stop working and activating it will launch that URL after an error message

  * App was discontinued but you can keep using for now

  * App was discontinued and will no longer work

Those are all potential outcomes that we can forsee but there are a lot of things we can’t forsee so we need  
to make this process as generic as possible.

### The Solution

We store properties files in our servers as plain text, this is easy and requires almost no configuration. These  
properties files use the following naming convention: `AppName-OS-Status.properties`.

Inside the properties file we have the following settings:

  * deprecatedVersion – the version number we no longer support but still run

  * derprecatedMessage – a message to show users of the deprecated version

  * derprecatedAltURL – URL to send users of the deprecated version to

  * unsupportedVersion – the version we no longer support

  * unsupportedMessage – a message we show to users of the unsupported version

  * unsupportedAltURL – URL to send users of the unsupported version

We can leave most properties blank if they aren’t applicable and might not even have a file.

__ |  Host these files in your own domain under your control. A dropbox URL might change and might be blocked in  
some cases and the same is true for github   
---|---  
  
#### The Code

Notice that this code needs to execute after the first form was shown and not in the `init(Object)` method.  
It will fail gracefully and the app will keep working if there is no Internet connection.
    
    
    String url = "https://myurl.com/mydir/" + Display.getInstance().getProperty("AppName", "MyApp") +
            "-" + Display.getInstance().getPlatformName() + "-Status.properties";
    Log.p("Checking update URL: " + url);
    ConnectionRequest c = new ConnectionRequest(url, false) {
        private Properties props;
        @Override
        protected void readResponse(InputStream input) throws IOException {
            props = new Properties();
            props.load(input);
        }
    
        @Override
        protected void postResponse() {
            if(props != null) {
                float version = Float.parseFloat(Display.getInstance().getProperty("AppVersion", "1.0"));
                float unsupportedVersion = Float.parseFloat(props.getProperty("unsupportedVersion", "-1"));
                if(version <= unsupportedVersion) {
                    String message = props.getProperty("unsupportedMessage", "The current application version is no longer supported");
                    String url = props.getProperty("unsupportedAltURL", null);
                    if(url == null) {
                        Dialog.show("Unsupported Version", message, "OK", null);
                        Display.getInstance().exitApplication();
                    } else {
                        if(!Dialog.show("Unsupported Version", message, "OK", "Exit")) {
                            Display.getInstance().exitApplication();
                        }
                        Display.getInstance().execute(url);
                        Display.getInstance().exitApplication();
                    }
                }
                float deprecatedVersion = Float.parseFloat(props.getProperty("deprecatedVersion", "-1"));
                if(version <= deprecatedVersion) {
                    String message = props.getProperty("derprecatedMessage", "A new version of the app is available, please update");
                    String url = props.getProperty("derprecatedAltURL", null);
                    if(url == null) {
                        Dialog.show("New Version", message, "OK", null);
                    } else {
                        if(Dialog.show("New Version", message, "Download", "OK")) {
                            Display.getInstance().execute(url);
                        }
                    }
                }
            }
        }
    
    };
    c.setFailSilently(true);
    NetworkManager.getInstance().addToQueue(c);

Now you will need properties files matching these URL’s, notice that the name is OS specific to allow you to have  
different version deprecation policies for different OS’s (e.g. for the case of losing the Android certificate).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jérémy MARQUER** — November 28, 2016 at 2:31 pm ([permalink](/blog/tip-cross-platform-update-available-strategy/#comment-23065))

> Jérémy MARQUER says:
>
> Interesting post but what about offline app or online app that can work offline ? I mean, for example, exiting application in case of unsupported version will work only if device is connected …
>



### **Shai Almog** — November 30, 2016 at 4:05 am ([permalink](/blog/tip-cross-platform-update-available-strategy/#comment-23229))

> Shai Almog says:
>
> This fails silently when offline.
>
> I wanted to keep the code simple so I didn’t go into the more complex situation of a “user turns on airplane mode to use the app”. I think that if a user resorts to that rather than just update the app he’ll also download an illegal APK/IPA and jailbrake his device so this ventures into another domain.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
