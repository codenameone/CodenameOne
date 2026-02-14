---
title: Cache, Sorted Properties and Preference Listener
slug: cache-sorted-properties-preferences-listener
url: /blog/cache-sorted-properties-preferences-listener/
original_url: https://www.codenameone.com/blog/cache-sorted-properties-preferences-listener.html
aliases:
- /blog/cache-sorted-properties-preferences-listener.html
date: '2016-12-12'
author: Shai Almog
---

![Header Image](/blog/cache-sorted-properties-preferences-listener/new-features-5.jpg)

We’ve had quite a few interesting features land last week and didn’t get a chance to cover them. First we have  
access to the OS’s caches directory where you can store files that you don’t really need as cache. Both iOS &  
Android have such a directory and files stored there might be purged without notice if the OS runs out of space.

This is a good place to store files you don’t really need such as images or downloads you just need for “right now”.

To use this we added two new API’s to `FileSystemStorage`:
    
    
    public boolean hasCachesDir();
    public String getCachesDir();

Normally we would use `hasCachesDir()` to indicate whether the caches dir should be used and if not we would just  
write fallback code that uses the home directory. Notice that the caches dir will only work on iOS & Android and  
will return false everywhere else at this time. We plan to integrate this into various features of Codename One  
to make usage easier e.g. `URLImage` and an upcoming feature I’ll discuss later in the week.

### Sorted Properties

Starting with this release properties are sorted and no longer add a timestamp comment every time they are saved.

The comment makes no sense as it never provided any value beyond the file date/time stamp. The sorting of the  
properties solves a major problem with the properties API. Saving is inconsistent.

We did this to fix a bug in our settings tool where `codenameone_settings.properties` gets jumbled whenever you  
save because the order of the hashmap is based on the `hashCode` method. This way the order is consistent and  
remains that way when checking the file into version control.

Fixing this for our application only would solve our problem but I’m guessing the jumbled properties that you need  
to sort thru every time doesn’t make sense for any application ever…​  
This way you can have a consistent human understandable order for machine saved properties. Notice this is a  
“write only” feature so you don’t need to sort your properties for parsing to work.

### Preferences Listener

We got a [pull request](https://github.com/codenameone/CodenameOne/pull/1980) that allows you to observe  
the `Preferences` API. This is useful if you have some generic code that relies on this API for settings.

This request lets you do something like:
    
    
    Preferences.addPreferenceListener("MySetting", (pref, oldValue, newValue) -> Log.p(pref + " changed to " + newValue));

This allows you to monitor changes to preferences individually and apply them instantly when they happen  
without the need to wrap all calls to preferences in a generic method. This observability allows different parts  
of your application to remain decoupled while supporting a single setting attribute.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — December 13, 2016 at 11:46 pm ([permalink](https://www.codenameone.com/blog/cache-sorted-properties-preferences-listener.html#comment-23066))

> bryan says:
>
> So should use something like:
>
> FileSystemStorage.getInstance().openOutputStream(FileSystemStorage.getInstance().getChacheDir() + “/ ” + fname);
>



### **Shai Almog** — December 14, 2016 at 5:12 am ([permalink](https://www.codenameone.com/blog/cache-sorted-properties-preferences-listener.html#comment-23232))

> Shai Almog says:
>
> Not quite. getCachesDir() will be null for cases where there is no such directory. E.g. on the simulator we don’t have a caches dir so this will fail. So you need a strategy that will no how to deal with a null cache directory. E.g. this is new code for a feature I’m blogging about later this week:
>
> private String getCacheFileName() {  
> String root;  
> if(FileSystemStorage.getInstance().hasCachesDir()) {  
> root = FileSystemStorage.getInstance().getCachesDir() + “cn1ConCache/”;  
> } else {  
> root = FileSystemStorage.getInstance().getAppHomePath()+ “cn1ConCache/”;  
> }  
> FileSystemStorage.getInstance().mkdir(root);  
> return root + Base64.encodeNoNewline(createRequestURL().getBytes()).replace(‘/’, ‘-‘).replace(‘+’, ‘_’);  
> }
>
> This will use the caches dir if it’s available but fallback to normal if not.
>
> Also notice that directories in Codename One should always end with the / character so the + “/” is redundant.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
