---
title: Marshmallow Permissions in the Simulator and Native Code
slug: marshmallow-permissions-in-the-simulator-and-native-code
url: /blog/marshmallow-permissions-in-the-simulator-and-native-code/
original_url: https://www.codenameone.com/blog/marshmallow-permissions-in-the-simulator-and-native-code.html
aliases:
- /blog/marshmallow-permissions-in-the-simulator-and-native-code.html
date: '2016-05-14'
author: Shai Almog
---

![Header Image](/blog/marshmallow-permissions-in-the-simulator-and-native-code/marshmallow.png)

We talked about the  
[new Android 6 (Marshmallow) permissions in Codename One last week](/blog/switching-on-android-marshmallow-permission-prompts.html)  
and so far we’ve been pretty happy with the result. We had some build regressions on the older Ant based build  
path but those were fixed shortly after and it’s been smooth sailing since then. As part of the transition to the new  
permissions system we added two features to the simulator and the `AndroidNativeUtil` class.

### Simulate Permission Prompts

You can simulate permission prompts by checking that option in the simulator menu.

![Simulate permission prompts menu item in the simulator](/blog/marshmallow-permissions-in-the-simulator-and-native-code/simulate-permission-prompts.png)

Figure 1. Simulate permission prompts menu item in the simulator

This will produce a dialog to the user whenever this happens in Android and will try to act in a similar way to the  
device. Notice that you can test it in the iOS simulator too.

### AndroidNativeUtil’s checkForPermission

If you write Android native code using our native interfaces you are probably familiar with the `AndroidNativeUtil`  
class from the `com.codename1.impl.android` package.

This class provides access to many low level capabilities you would need as a developer writing native code.  
Since native code might need to request a permission we introduced the same underlying logic we used namely:  
`checkForPermission`.

To get a permission you can use this code as such:
    
    
    if(!com.codename1.impl.android.AndroidNativeUtil.checkForPermission(Manifest.permission.READ_PHONE_STATE, "This should be the description shown to the user...")){
        // you didn't get the permission, you might want to return here
    }
    // you have the permission, do what you need

This will prompt the user with the native UI and later on with our fallback option as described in  
[the previous blog post](/blog/switching-on-android-marshmallow-permission-prompts.html). Notice that  
the `checkForPermission` method is a blocking method and it will return when there is a final conclusion  
on the subject. It uses `invokeAndBlock` and can be safely invoked on the event dispatch thread without concern.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
