---
title: Preferences, Location, Popup & Order
slug: preferences-location-popup-order
url: /blog/preferences-location-popup-order/
original_url: https://www.codenameone.com/blog/preferences-location-popup-order.html
aliases:
- /blog/preferences-location-popup-order.html
date: '2016-12-20'
author: Shai Almog
---

![Header Image](/blog/preferences-location-popup-order/new-features-5.jpg)

One of the fallouts from the new encrypted storage API we added last week is the fact that it encrypts things  
like preferences making them unusable if you expected them to work before/after encryption was applied.  
To workaround this we added a new API to the `Preferences` class:
    
    
    public static void setPreferencesLocation(String storageFileName)
    public static String getPreferencesLocation()

These API’s allow us to determine the storage file in which the preferences are stored (not to be confused  
with `FileSystemStorage` file). By default preferences are stored in `CN1Preferences` but you can use any file  
name you want. This is useful if your app allows several logins and you might want to use preferences  
differently for every login.

### Location Popup

A while back a user contributed a change the triggered the simulator location dialog popup automatically when  
working with the location API. This seemed like a good idea at the time but proved to be one of the most  
annoying features if you have things like background location tracking.

So until we have a configuration to enable/disable it by default we decided to turn this off. You can still open the  
location popup from the simulator menu if you want it.

### Argument Order

We recently ran into a weird misbehavior in the Amazon AWS API where they relied on the order of the post  
values and would fail if they weren’t delivered in a specific order within the body of the request.

Unfortunately, we used a `Map` to store the elements and the elements in Java maps are ordered based on their  
`hashCode()` method value which doesn’t comply to the key/value order. With the next update we’ll replace the  
internal `Map` with `LinkedHashMap` which preserves the addition order and that way `addArgument` will determine  
the order in which the element appears in the URL/body of a request.

This actually has some merit. With an upload Amazon can evaluate values of small fields before the full file  
is uploaded to S3. That way it can reject something quickly before the server/network or device do the actual  
upload.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jérémy MARQUER** — December 21, 2016 at 1:00 pm ([permalink](https://www.codenameone.com/blog/preferences-location-popup-order.html#comment-21558))

> Jérémy MARQUER says:
>
> Happy to read that we can now manage (without any implementation) multi account preference ! Thanks.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreferences-location-popup-order.html)


### **M Usman Nu** — February 1, 2017 at 7:27 pm ([permalink](https://www.codenameone.com/blog/preferences-location-popup-order.html#comment-24121))

> M Usman Nu says:
>
> If I close the application then preferences are cleared. Is there any way to make it permanent ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreferences-location-popup-order.html)


### **Shai Almog** — February 2, 2017 at 6:19 am ([permalink](https://www.codenameone.com/blog/preferences-location-popup-order.html#comment-23257))

> Shai Almog says:
>
> Use the store() method just like standard Java
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreferences-location-popup-order.html)


### **Peng** — April 13, 2017 at 9:46 am ([permalink](https://www.codenameone.com/blog/preferences-location-popup-order.html#comment-23300))

> Peng says:
>
> Hi, I’m struggling with writing unencrypted Preferences after EncryptedStorage is installed. My use case is that I need to read certain Preferences before the user logs in. During login the EncryptedStorage is installed. After that some Preferences need to be written which are needed for the next launch of the app (before the login).
>
> Is there a way to accomplish this? According to what I’ve read in this article, changing the preferences location should have the desired effect, but I cannot figure out how. Putting the following snippet into the init method of a generated project always (even after restarting the app) results in NULL being returned by Preferences.get():
>
> // before login  
> Preferences.setPreferencesLocation(“SomeOtherLocation”);  
> Preferences.get(“Key”, null); // This always returns null
>
> // login  
> EncryptedStorage.install(“Secret”);
>
> // after login  
> Preferences.set(“Key”, “Value”);
>
> The invocation of the Preferences.get() method during the first launch just returns NULL, during the second launch also an EOFException is logged to the console before the NULL value is returned.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreferences-location-popup-order.html)


### **Shai Almog** — April 14, 2017 at 4:36 am ([permalink](https://www.codenameone.com/blog/preferences-location-popup-order.html#comment-23154))

> Shai Almog says:
>
> Once encryption is in place a file read/written without/with encryption won’t work. What you are doing here is:
>
> setting location for preferences  
> Getting/setting a key (creating unencrypted file)
>
> Encrypting
>
> Getting key from unencrypted file which seems corrupt to the system now.
>
> The idea is to keep two preferences files, one encrypted and one unencrypted and call set preferences location AFTER encrypting so one doesn’t break the other.
>
> You obviously can’t share a key between those two so if you need a key to exist in both you will need to transfer it thru a variable in memory as once encryption is on everything is “gone”.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreferences-location-popup-order.html)


### **Peng** — April 19, 2017 at 8:11 am ([permalink](https://www.codenameone.com/blog/preferences-location-popup-order.html#comment-23474))

> Peng says:
>
> Thank you for your answer. Unfortunately, transfer throught a variable in memory is not an option for me. I need to store a piece of information AFTER encrypting that would be available during the next launch of the app BEFORE the EncryptedStorage.install call.
>
> To make my question more comprehensive, here’s my situation: I need to store the password hash of the user somehow unencrypted to check during login. The same password is used during login to decrypt the storage. Now, while still logged in, the user may change his password. The hash of this new password then needs to be stored for the next login.
>
> If I got your answer right, there is no built-in way to accomplish this, using only the Storage/EncryptedStorage and Preferences APIs. As there is no “EncryptedStorage.uninstall” method, one cannot write plaintext data to storage after EncryptedStorage.install was called.
>
> Based on your experience, what solution would you recommend, especially in terms of portability? Would using e.g. Properties and FileSystemStorage directly be a good idea or are there better alternatives?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreferences-location-popup-order.html)


### **Shai Almog** — April 20, 2017 at 4:45 am ([permalink](https://www.codenameone.com/blog/preferences-location-popup-order.html#comment-23512))

> Shai Almog says:
>
> You can store things in FileSystemStorage under the home directory of the user and they won’t be encrypted. Only Storage is encrypted.
>
> Re-encrypting is a bit tedious as this wasn’t designed for that and might be error prone. I would suggest this:
>
> Use a random generated value to encrypt the data
>
> Then encrypt the random key using your password and save that in FileSystemStorage. That way you just need to update one file with every change of the password…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpreferences-location-popup-order.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
