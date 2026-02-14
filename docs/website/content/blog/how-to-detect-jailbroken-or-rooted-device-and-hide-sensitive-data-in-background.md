---
title: How to detect Jailbroken or Rooted device and hide sensitive data in background?
slug: how-to-detect-jailbroken-or-rooted-device-and-hide-sensitive-data-in-background
url: /blog/how-to-detect-jailbroken-or-rooted-device-and-hide-sensitive-data-in-background/
original_url: https://www.codenameone.com/blog/how-to-detect-jailbroken-or-rooted-device-and-hide-sensitive-data-in-background.html
aliases:
- /blog/how-to-detect-jailbroken-or-rooted-device-and-hide-sensitive-data-in-background.html
date: '2021-05-21'
author: Steve Hannah
description: The following recipes relate to security of Codename One apps. This includes
  detecting Jailbroken or Rooted device and hiding sensitive data when entering background.
---

The following recipes relate to security of Codename One apps. This includes detecting Jailbroken or Rooted device and hiding sensitive data when entering background.

The following recipes include tips on making your Codename One apps more secure.

### Detecting Jailbroken/Rooted Device

## Problem

You want to detect whether the device your app is running on is Jailbroken or Rooted.

## Solution

While there is no way to know whether the device is rooted with 100% certainty, you can use the [CN1JailbreakDetect](https://github.com/shannah/CN1JailbreakDetect) cn1lib to to make a good guess.

This cn1lib acts as a thin wrapper around the [RootBeer](https://github.com/scottyab/rootbeer) Android library, and [DTTJailbreakDetection](https://github.com/thii/DTTJailbreakDetection) iOS library, which employ heuristics to determine whether the device has likely been jailbroken.

## Example

```java
				
					package com.codename1.samples;

import com.codename1.ext.jailbreak.JailbreakDetect;
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
import com.codename1.ui.Button;
import com.codename1.ui.Command;

public class JailbreakDetectionSample {

    private Form current;
    private Resources theme;

    public void init(Object context) {
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
        Form hi = new Form("Jailbreak Detection", BoxLayout.y());
        Button detect = new Button("Detect Jailbreak");
        detect.addActionListener(evt->{
            if (JailbreakDetect.isJailbreakDetectionSupported()) {
                if (JailbreakDetect.isJailbroken()) {
                    Dialog.show("Jailbroken","This device is jailbroken", new Command("OK") );
                } else {
                    Dialog.show("Not Jailbroken", "Probably not jailbroken.  But can't be 100% sure.", new Command("OK"));
                }
            } else {
                Dialog.show("No Idea", "No support for jailbreak detection on this device.", new Command("OK"));
            }
        });
        hi.add(detect);
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
				
			
```

## Tip:

> This sample is part of the [Codename One samples](https://github.com/codenameone/CodenameOne/tree/master/Samples) project, and can be run directly from the Codename One SampleRunner.

## Discussion

The [CN1JailbreakDetect](https://github.com/shannah/CN1JailbreakDetect) provides two useful static methods for jailbreak detection:

1. isJailbreakDetectionSupported() – This checks if the jailbreak detection is even supported on this platform.
  
  
2. isJailBroken() – This checks if the device is jailbroken. If detection is not supported, then this will always return false.

Currently jailbreak detection is only supported on Android and iOS.

## Important:

> There is NO way to know with 100% certainty whether or not a device has been jailbroken.

## Further Reading

1. [CN1JailbreakDetect github project](https://github.com/shannah/CN1JailbreakDetect)
2. [RootBeer project](https://github.com/scottyab/rootbeer) (Used on Android)
3. [DTTJailbreakDetection project](https://github.com/thii/DTTJailbreakDetection) (Used on iOS)

### Hiding Sensitive Data When Entering Background

## Problem

iOS will take a screenshot of your app when it enters the background that it uses for various previews of the app state. You want to hide sensitive data in your app’s UI to prevent this information from leaking out via these screenshots.

## Solution

You can use the ios.blockScreenshotsOnEnterBackground=true build hint to prevent iOS from taking screenshots app goes into the background. This will cause the canvas on which the Codename One UI is drawn to be hidden in the didEnterBackground hook and unhidden in the willEnterForeground hook.

## Warning:

> This will cause your app to appear as a blank white rectangle when the user is browsing through opened apps.

![](https://www.codenameone.com/wp-content/uploads/2021/05/Image-270420-124718.733.png)

> Figure 1. Notice the app in the middle is blank white because it has been set to block iOS screenshots.

## Discussion

You might have been tempted to try to modify the UI inside the stop() lifecycle method of your app, since it is called itself by the didEnterBackground hook.
  
  
This strategy will work in some platforms, but not on iOS because the screenshot call is made immediately upon the didEnterBackground method returning – and the stop() method runs on the EDT (a different thread), so this is not possible.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Javier Anton** — May 24, 2021 at 11:06 am ([permalink](https://www.codenameone.com/blog/how-to-detect-jailbroken-or-rooted-device-and-hide-sensitive-data-in-background.html#comment-24454))

> Javier Anton says:
>
> Very useful. There are lots of rooted users that hack in-app purchases and get things for free. I wonder how effective RootBeer is, I know there are tools available to hide the root at the moment
>



### **Javier Anton** — May 24, 2021 at 12:30 pm ([permalink](https://www.codenameone.com/blog/how-to-detect-jailbroken-or-rooted-device-and-hide-sensitive-data-in-background.html#comment-24455))

> Javier Anton says:
>
> Just looked at the RootBeer code and it looks great. It even checks natively in cpp, which I read was the safest way to bypass root masks
>



### **Javier Anton** — May 24, 2021 at 12:45 pm ([permalink](https://www.codenameone.com/blog/how-to-detect-jailbroken-or-rooted-device-and-hide-sensitive-data-in-background.html#comment-24456))

> Javier Anton says:
>
> Is there any reason RootBeer version 0.0.8 is used and not 0.0.9?
>



### **Javier Anton** — May 24, 2021 at 2:49 pm ([permalink](https://www.codenameone.com/blog/how-to-detect-jailbroken-or-rooted-device-and-hide-sensitive-data-in-background.html#comment-24457))

> Javier Anton says:
>
> Nevermind, I just added the native bits myself. Wish I could delete/edit my posts on here, maybe that could be enabled at some point
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
