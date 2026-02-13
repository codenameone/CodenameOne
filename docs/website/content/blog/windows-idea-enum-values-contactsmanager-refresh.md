---
title: Windows, IDEA, Enum Values & ContactsManager Refresh
slug: windows-idea-enum-values-contactsmanager-refresh
url: /blog/windows-idea-enum-values-contactsmanager-refresh/
original_url: https://www.codenameone.com/blog/windows-idea-enum-values-contactsmanager-refresh.html
aliases:
- /blog/windows-idea-enum-values-contactsmanager-refresh.html
date: '2016-04-10'
author: Shai Almog
---

![Header Image](/blog/windows-idea-enum-values-contactsmanager-refresh/generic-java-1.jpg)

Steve committed the [upcoming Windows port to out github repository](https://github.com/codenameone/CodenameOne/tree/master/Ports/UWP/VSProjectTemplate)  
it’s still work in progress so you can’t physically target it as part of the build system although we hope to move  
this port out relatively quickly. This should be more doable thanks to the [iKVM route](https://www.codenameone.com/blog/new-windows-port.html)  
& the [work from Fabricio](https://github.com/Pmovil/CN1WindowsPort) both of which saved a lot of the effort in this port.

You can take a look at this port if you are Windows inclined and let us know what you think.

### IntelliJ IDEA Update

So far the plugin rewrite went more smoothly than our worst fear. We did have to issue an update to a regression  
that caused the wrong UI to load sometime for the designer/settings panel/gui builder. If you are using version  
3.3.1 at this time you are up to date.

One thing we neglected to mention in the announcement is that you need Java 8 or the plugin won’t even show up.  
We only tested on IntelliJ 16 so please try to use that version with Java 8.

### Enum Values

One of the long time [ParparVM](https://github.com/codenameone/CodenameOne/tree/master/vm) issues has been  
the usage of enums in iOS. Enum compilation triggers some reflection code under the hood to support capabilities  
such as `values()` and those just didn’t translate into C code properly.

Steve took on that issue and now code such as this:
    
    
    for(MyEnum e : MyEnum.values()) {
        ...
    }

Should work just fine for iOS compilation too.

### ContactsManager Refresh

We added a bit of a hack to solve an iOS issue with contacts based on a request from an enterprise developer.  
It seems that in iOS when you have a list of contacts and you minimize the application to add a new contact,  
the list won’t be refreshed even if you re-invoke `getAllContacts()`.

iOS caches the data for the contacts and we need to explicitly refresh the list using  
[ContactsManager.refresh()](https://www.codenameone.com/javadoc/com/codename1/contacts/ContactsManager.html#refresh--)  
which you can call in the `start()` method in case the app is being restored. This method won’t do anything  
in other platforms but if you need it then it’s pretty valuable.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
