---
title: New Default Code
slug: new-default-code
url: /blog/new-default-code/
original_url: https://www.codenameone.com/blog/new-default-code.html
aliases:
- /blog/new-default-code.html
date: '2018-03-06'
author: Shai Almog
---

![Header Image](/blog/new-default-code/new-features-5.jpg)

This is new behavior that went in without fanfare. If you created a new hello world app you might have noticed this. We changed the default boilerplate for Codename One and made it more representative of what you’d want to see in a hello world app.

The entire change to the default generated app is within the init method:
    
    
    public void init(Object context) {
        // use two network threads instead of one
        updateNetworkThreadCount(2); __**(1)**
    
        theme = UIManager.initFirstTheme("/theme");
    
        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);
    
        // Pro only feature
        Log.bindCrashProtection(true);
    
        addNetworkErrorListener(err -> { __**(2)**
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });
    }

__**1** | By default networking in Codename One runs on one network thread for consistency. Two threads make more sense from a performance standpoint. We wanted to make this easily configurable and it should be easy in the `init()` method  
---|---  
__**2** | Handling network errors in a generic way is probably one of the hardest things to grasp. So many developers are still stuck with the default error handling code…​ With that in mind we added a bit of boilerplate to handle network errors in a way that makes sense and should be easily customizable  
  
I had some thoughts about removing this boilerplate and packaging it in a class. But then I thought again.

### I Like Boilerplate

  * It’s simple

  * It’s obvious

  * You can instantly find what you want

This isn’t code for the sake of code or declaration. The code is still concise and does what we expect. This is one of those cases where boilerplate makes more sense.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
