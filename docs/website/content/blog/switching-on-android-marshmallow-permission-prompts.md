---
title: Switching on Android Marshmallow Permission Prompts
slug: switching-on-android-marshmallow-permission-prompts
url: /blog/switching-on-android-marshmallow-permission-prompts/
original_url: https://www.codenameone.com/blog/switching-on-android-marshmallow-permission-prompts.html
aliases:
- /blog/switching-on-android-marshmallow-permission-prompts.html
date: '2016-05-07'
author: Shai Almog
---

![Header Image](/blog/switching-on-android-marshmallow-permission-prompts/marshmallow.png)

Up until Marshmallow (version 6) Android used a rather obtuse permission system that very few end users understood.  
With API level 23 (Marshmallow) Android finally shifted to a structure that makes more sense similarly to iOS.  
Marshmallow asks a users permission the first time an API is used e.g. when accessing contacts the user will  
receive a prompt whether to allow contacts access.

__ |  Permission can be denied and a user can later on revoke/grant a permission via external settings UI   
---|---  
  
This is really great as it allows apps to be installed with a single click and no permission prompt during install  
which can increase conversion rates!

### The Transition

Starting today all compilations will be done with SDK level 23 but not with target level 23!

This means that by default the new permission mode is still off and you won’t see any of the effects mentioned below.

We will probably change this to be the default in the future but at the moment the target SDK defaults to 21. To  
activate this functionality you will need to set the target SDK to level 23 by using the  
[build hint](/manual/advanced-topics.html)  
`android.targetSDKVersion=23`.

### How Does This Look?

To test this API I’ve created a simple contacts app:
    
    
    Form f = new Form("Contacts", BoxLayout.y());
    f.add(new InfiniteProgress());
    Display.getInstance().invokeAndBlock(() -> {
        Contact[] ct = Display.getInstance().getAllContacts(true, true, false, true, true, false);
        Display.getInstance().callSerially(() -> {
            f.removeAll();
            for(Contact c : ct) {
                MultiButton mb = new MultiButton(c.getDisplayName());
                mb.setTextLine2(c.getPrimaryPhoneNumber());
                f.add(mb);
            }
            f.revalidate();
        });
    });
    
    f.show();

When I try to install it without changing anything on my Android 6 OPO device I see this UI:

![Install UI when using the old permissions system](/blog/switching-on-android-marshmallow-permission-prompts/marshmallow-permissions-level21.png)

Figure 1. Install UI when using the old permissions system

When I set `android.targetSDKVersion=23` in the build hints and try to install again the UI looks like this:

![Install UI when using the new permissions system](/blog/switching-on-android-marshmallow-permission-prompts/marshmallow-permissions-level23.png)

Figure 2. Install UI when using the new permissions system

When I launch the UI under the old permissions system I got to the contacts instantly in the new system I’m presented  
with this UI:

![Native permission prompt first time](/blog/switching-on-android-marshmallow-permission-prompts/marshmallow-permissions-first-request.png)

Figure 3. Native permission prompt first time

If I accept and allow all is good and the app loads as usual but if I deny then Codename One gives the user another  
chance to request the permission. Notice that in this case you can customize the prompt string as explained  
below.

![Codename One permission prompt](/blog/switching-on-android-marshmallow-permission-prompts/marshmallow-permissions-codenameone-prompt.png)

Figure 4. Codename One permission prompt

If I would select don’t ask then I will get a blank screen since the contacts will return as a 0 length array. This makes  
sense as the user is aware he denied permission and the app will still function as expected on a device where  
no contacts are available. However, if the user realizes his mistake he can double back and ask to re-prompt for  
permission in which case he will see this native prompt:

![Native permission prompt second time](/blog/switching-on-android-marshmallow-permission-prompts/marshmallow-permissions-second-request.png)

Figure 5. Native permission prompt second time

Notice that denying this second request will not trigger another Codename One prompt.

### Code Changes

For native Android developers this transition was painful requiring developers to rewrite API access with  
callbacks to respect the cases where permission is denied or revoked. When adding the support to Codename  
One we chose to make this as seamless as we always strived to be. The respective API’s will work just like they  
always worked and will prompt the user seamlessly for permissions.

__ |  Some behaviors that never occurred on Android but were perfectly legal in the past might start occurring with  
the switch to the new API. E.g. the location manager might be null and your app must always be ready to deal  
with such a situation   
---|---  
  
When permission is requested a user will be seamlessly prompted/warned, we have builtin text to control such  
prompts but you might want to customize the text. You can customize permission text via the `Display` properties  
e.g. to customize the text of the contacts permission we can do something such as:
    
    
    Display.getInstance().setProperty("android.permission.READ_CONTACTS", "MyCoolChatApp needs access to your contacts so we can show you which of your friends already have MyCoolChatApp installed");

This is optional as there is a default value defined. You can define this once in the `init(Object)` method but for some  
extreme cases permission might be needed for different things e.g. you might ask for this permission with one reason  
at one point in the app and with a different reason at another point in the app.

The following permission keys are supported: “android.permission.READ_PHONE_STATE”  
`android.permission.WRITE_EXTERNAL_STORAGE`,  
`android.permission.ACCESS_FINE_LOCATION`,  
`android.permission.SEND_SMS`,  
`android.permission.READ_CONTACTS`,  
`android.permission.WRITE_CONTACTS`,  
`android.permission.RECORD_AUDIO`.

### Should you Switch?

We suggest you test target level 23 with your app and see how it performs. Verify that nothing breaks and that  
the app maintains usability.

__ |  You will need a Marshmallow level device to test this properly as the app will look the same on older devices   
---|---  
  
Testing this is crucial as we would flip the switch to make this the default in the near future. The timing when  
this is switched on might not be as convenient as trying this option now. Due to the way Android works it is very  
possible that Google will promote target level 23 apps more aggressively to encourage developers to update  
their apps so it is prudent to update now rather than later.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
