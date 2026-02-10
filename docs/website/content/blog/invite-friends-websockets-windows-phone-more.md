---
title: Invite Friends, WebSockets, Windows Phone & More
slug: invite-friends-websockets-windows-phone-more
url: /blog/invite-friends-websockets-windows-phone-more/
original_url: https://www.codenameone.com/blog/invite-friends-websockets-windows-phone-more.html
aliases:
- /blog/invite-friends-websockets-windows-phone-more.html
date: '2015-08-02'
author: Shai Almog
---

![Header Image](/blog/invite-friends-websockets-windows-phone-more/facebook-invite-friends.jpg)

We’ve released a burst of small new features that piled up during the code freeze and release cycle, we also have a couple of interesting  
3rd party efforts such as an independent Windows Phone port and websockets implementation. But first lets start with  
Facebooks “invite friends” feature. Historically with the Facebook API you could just use the Graph API to query  
Facebook for the list of friends. This will return an empty list now and will only expose friends who are already using the  
app. You can use the standard share button or Facebook share both of which are great options to promote your app.  
However, Facebook also has a special native API allowing the user to invite his friends into the app… 

The `FacebookConnection` class now has support for that feature, its a non-trivial feature and you will need to activate  
some options within the Facebook application in order to fully utilize it but its a pretty powerful feature if you use  
Facebook login and want to promote your app socially. 

#### Windows Phone Port

As you may recall, we are taking a [“wait and see”  
approach for the future of the Windows Phone port](/blog/login-tutorials-future-of-windows-phone.html) based on MS’s historical flakyness when committing to  
technologies. However, some developers need to release a version to Windows Phone now and Fabricio who is one such developer  
took it upon himself to improve the current port with some interesting results. You can check out his companies project  
[here](https://github.com/Pmovil/CN1WindowsPort). 

There are several inherent problems that would be harder still to fix, they derive from the C# based port that relies on  
XMLVM. Since XMLVM was effectively discontinued and its C# backend was experimental at best it contains a  
lot of bugs that you might need to workaround. The proper solution would be to port the new VM built for iOS  
to Windows Phone, it was designed to be very portable so this should be relatively easy. It should also provide a  
nice performance boost, C# is pretty fast but the XMLVM layer on top isn’t as efficient as it should be due to language  
inconsistencies between C# and Java. 

If you would like us to put more effort into Windows Phone support please signup as an enterprise account and  
make your voice heard. 

#### Websocket cn1lib & Chat

[Steve](http://sjhannah.com/) already released a  
[sockets cn1lib](https://github.com/shannah/CN1Sockets) a while back and  
he just introduced a [new websocket cn1lib](https://github.com/shannah/cn1-websockets)  
yesterday! 

The websockets standard is designed to provide the web generation of tools with the power/speed of socket  
communication without getting into some of the low level oddities of the TCP protocol. The really nice thing  
about websockets, is that you can just program to it using a webserver with URL based notations which simplifies  
a lot of the hassle you might have normally with socket programming. 

As part of the release [Steve](http://sjhannah.com/) also added a chat app that  
communicates between users. Its a really interesting approach and you might find it very useful if you want to  
integrate chat functionality in your application. 

#### URLImage.createMaskAdapter

Up until now the `URLImage` class only supported relatively simple adapters out of the box,  
specifically scaling of various types. We now have a new API called `URLImage.createMaskAdapter`  
that accepts a mask image. You can then apply this adapter to the `URLImage` creation and thus  
apply a mask to the stored image. E.g. we use it in the chat app demo to create a circular image of the speaker. 

#### Util Methods

One of the most annoying pieces of code I write all the time is: 
    
    
    try {
        Thread.sleep(ms);
    } catch(InterruptedException e) {}

I love checked exceptions but they are problematic when they are misused in cases where they make no sense at  
all (e.g. threads are never interrupted on iOS). So to reduce this pain we added two methods to `Util`:  
`sleep(int)` and `wait(Object, int)`.  
They do pretty much what you expect and don’t throw an exception with the added bonus in the case of wait where  
it implicitly declares the `synchronized` block for you removing the need for you to do that so you can  
convert this: 
    
    
    try {
        syncronized(obj) {
            obj.wait(ms);
        }
    } catch(InterruptedException e) {}

To: 
    
    
    Util.wait(obj, ms);

#### Simplified Theme Initialization

Up until now handcoded Codename One apps had a pretty standard set of boilerplate code: 
    
    
    try {
        theme = Resources.openLayered("/theme");
        UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
    } catch(IOException e){
        e.printStackTrace();
    }

Its redundant since its rarely changed by users and when it is changed its usually with one purpose (setting  
a specific theme). So rather than write all that code we now use: 
    
    
    theme = UIManager.initFirstTheme("/theme");

Which is exactly the same block of code wrapped in a method.  
If you want to explicitly specify the theme you can use 
    
    
    theme = UIManager.initNamedTheme("/theme", "MyTheme");

Which is still more concise and ignores the `IOException` which can’t be handled properly this  
early in the app anyway. 

#### CaseInsensitiveOrder

The `String` class in Java SE has a member comparator called `String.CASE_INSENSITIVE_ORDER`  
and that’s really useful when we want to do something like: 
    
    
    ContactData[] cnts = data.getContacts();
    Arrays.sort(cnts, (ContactData o1, ContactData o2) -> {
        return String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name);
    });

Which allows us to effectively sort an array or collection based on a string element very easily. However, having this  
in the `String` class is painful when we have to do it to multiple platforms. Furthermore this approach is  
inefficient since the object needs to be instantiated even when its unused so we’d rather not add something like  
that to a mobile device platform where every KB counts.  
As a result we decided to add the class `CaseInsensitiveOrder` which is effectively almost identical: 
    
    
    ContactData[] cnts = data.getContacts();
    CaseInsensitiveOrder co = new CaseInsensitiveOrder();
    Arrays.sort(cnts, (ContactData o1, ContactData o2) -> {
        return co.compare(o1.name, o2.name);
    });
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Fabrício Cabeça** — August 3, 2015 at 4:52 pm ([permalink](https://www.codenameone.com/blog/invite-friends-websockets-windows-phone-more.html#comment-22297))

> Fabrício Cabeça says:
>
> Hi Shai, thanks for mentioning our efforts with the Windows port, we are working hard to make it fast, stable and feature complete. We do have an internal schedule for features like in-app purchase and facebook native integration and we plan to release some of our apps using this port in the near future. Any pro/enterprise user willing to help would be most welcome.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Finvite-friends-websockets-windows-phone-more.html)


### **ftp27** — August 4, 2015 at 11:25 am ([permalink](https://www.codenameone.com/blog/invite-friends-websockets-windows-phone-more.html#comment-22375))

> ftp27 says:
>
> Thanks for WebSockets plugin!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Finvite-friends-websockets-windows-phone-more.html)


### **shannah78** — August 4, 2015 at 4:38 pm ([permalink](https://www.codenameone.com/blog/invite-friends-websockets-windows-phone-more.html#comment-22305))

> shannah78 says:
>
> Your welcome 🙂 Your original comment mentioned that you were having trouble on iOS. Did you solve this?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Finvite-friends-websockets-windows-phone-more.html)


### **ftp27** — August 5, 2015 at 8:17 am ([permalink](https://www.codenameone.com/blog/invite-friends-websockets-windows-phone-more.html#comment-21544))

> ftp27 says:
>
> Yes. This trouble has been associated with animate function in iOS. I replace this function to revalidate and all began worked fine. This was not associated with WebSockets.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Finvite-friends-websockets-windows-phone-more.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
