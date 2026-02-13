---
title: Fingerprint/TouchID Support
slug: fingerprint-touchid-support
url: /blog/fingerprint-touchid-support/
original_url: https://www.codenameone.com/blog/fingerprint-touchid-support.html
aliases:
- /blog/fingerprint-touchid-support.html
date: '2017-05-02'
author: Shai Almog
---

![Header Image](/blog/fingerprint-touchid-support/fingerprint-scanner.jpg)

Fingerprint scanners are pretty common in modern hardware both from Apple and some Android vendors. The problem is that the iOS and Android API’s for accessing them are a world apart. However, it’s possible to find some low level common ground which is exactly what our [cn1lib for fingerprint scanning](https://github.com/codenameone/FingerprintScanner) accomplished.

This is a very basic API that just validates the user as the owner of the device, it’s useful to lock off portions of the application from a 3rd party using code such as:
    
    
    Fingerprint.scanFingerprint("Use your finger print to unlock AppName.", value -> {
        Log.p("Scan successful!");
    }, (sender, err, errorCode, errorMessage) -> {
        Log.p("Scan Failed!");
    });

Since the cn1lib is pretty simple it can probably be enhanced to support more elaborate functionality in the future.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Lukman Javalove Idealist Jaji** — May 4, 2017 at 5:43 am ([permalink](https://www.codenameone.com/blog/fingerprint-touchid-support.html#comment-24130))

> Lukman Javalove Idealist Jaji says:
>
> Fantastic. To think this is what I have been researching all week. But I do want something not too complex but maybe complex for cn1 at this time. I am ordering external finger print scanners to use with my apps (for data collection). The only way to achieve that I guess is to delve into native code. I will like to see this in the future, take advantage of the inbuilt fingerprint hardware on devices to collect fingerprint data and stored. More like
>
> if(Display.getInstance().isFPSupported())  
> {  
> //Collect and store)  
> }
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffingerprint-touchid-support.html)


### **Shai Almog** — May 5, 2017 at 4:46 am ([permalink](https://www.codenameone.com/blog/fingerprint-touchid-support.html#comment-23559))

> Shai Almog says:
>
> Great. I don’t think that’s allowed on iOS but you can probably extend the Android code in the library to support some Android specific features and expose them in the API.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffingerprint-touchid-support.html)


### **Gareth Murfin** — July 6, 2017 at 11:36 pm ([permalink](https://www.codenameone.com/blog/fingerprint-touchid-support.html#comment-24135))

> Gareth Murfin says:
>
> Awesome, but how does it know your finger print already? from the OS?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffingerprint-touchid-support.html)


### **Shai Almog** — July 7, 2017 at 4:17 am ([permalink](https://www.codenameone.com/blog/fingerprint-touchid-support.html#comment-22614))

> Shai Almog says:
>
> The devices already have scanned fingerprints within. You use the OS interface to scan.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffingerprint-touchid-support.html)


### **Yishai Steinhart** — July 6, 2018 at 7:37 pm ([permalink](https://www.codenameone.com/blog/fingerprint-touchid-support.html#comment-23791))

> Yishai Steinhart says:
>
> NM
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffingerprint-touchid-support.html)


### **Shai Almog** — July 7, 2018 at 4:50 am ([permalink](https://www.codenameone.com/blog/fingerprint-touchid-support.html#comment-23961))

> Shai Almog says:
>
> FYI to your original question I suggest installing the cn1lib via the extension manager as you would get the latest version…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffingerprint-touchid-support.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
