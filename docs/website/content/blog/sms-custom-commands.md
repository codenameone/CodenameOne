---
title: SMS & Custom Commands
slug: sms-custom-commands
url: /blog/sms-custom-commands/
original_url: https://www.codenameone.com/blog/sms-custom-commands.html
aliases:
- /blog/sms-custom-commands.html
date: '2014-11-25'
author: Shai Almog
---

![Header Image](/blog/sms-custom-commands/sms-custom-commands-1.png)

  
  
  
  
![Picture](/blog/sms-custom-commands/sms-custom-commands-1.png)  
  
  
  

  
  
  
  
  
One of the problems in cross platform development is availability of functionality on one platform that is missing on another. Case in point sending an SMS message.  
  
Android, Blackberry & J2ME support sending SMS’s in the background without showing the user anything. They even support a form of intercepting incoming SMS’s to one degree or another (but that’s rather problematic).  
  
iOS & Windows Phone just don’t have that ability, the best they can offer is to launch the native SMS app with your message already in that app. 

Unfortunately our sendSMS API ignored that difference and simply worked interactively on iOS/Windows Phone while sending in the background for the other platforms.

In the next update we will add the API getSMSSupport which will return one of the following options:  
  

  *   
SMS_NOT_SUPPORTED – for desktop, tablet etc.  

  *   
  
  
SMS_SEAMLESS – sendSMS will not show a UI and will just send in the background  
  

  *   
SMS_INTERACTIVE – sendSMS will show an SMS sending UI  
  

  *   
SMS_BOTH – sendSMS can support both seamless and interactive mode, this currently only works on Android  
  

And we updated the sendSMS method to: sendSMS(String phoneNumber, String message, boolean interactive)

  
  
  
  
  
  
  
The last argument will be ignored unless SMS_BOTH is returned from  
  
getSMSSupport  
  
at which point you would be able to choose one way or the other. The default behavior (when not using that flag) is the background sending which is the current behavior on Android.

**  
Customizing Commands In Runtime  
**  
  
Commands are abstracted so we can integrate deeply into the native platform. E.g. the default behavior on Android is to add commands to the action bar which is what we would assume in the case of Android.  
  
If commands are drawn by us and not native (e.g. if you use the side menu bar or running on iOS etc.) you can easily customize the style of the commands using the “TouchCommand” and “Command” UIID’s. However, what happens when you want one command to have a different style (e.g. for a delete command). There are many ways to solve this especially with the SideMenuBar where you can use a custom component or quite a few other features. However, none of them are trivial.

To solve that we added the ability to assign a UIID to a command which will be applied to the element if applicable, just use:  
  
form.getMenuBar().setCommandUIID(cmd, “MyUIID”);

This will work in runtime and should implicitly refresh the UI.  
  
  
  

  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — November 27, 2014 at 5:37 am ([permalink](https://www.codenameone.com/blog/sms-custom-commands.html#comment-21605))

> Anonymous says:
>
> Hey Shai, great features! I just want to know if its possible to have one form to present a component in a side menu, and another form to have the native action bar to present the commands?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsms-custom-commands.html)


### **Anonymous** — November 28, 2014 at 4:18 pm ([permalink](https://www.codenameone.com/blog/sms-custom-commands.html#comment-21504))

> Anonymous says:
>
> You can have a regular title bar without the side menu for a form if you don’t add too many commands. However, it won’t be an action bar on Android since that is overriden by the side menu and the override is global. 
>
> This isn’t trivial since commands need to be routed in a very different way to work with the action bar.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsms-custom-commands.html)


### **Anonymous** — November 29, 2014 at 12:28 am ([permalink](https://www.codenameone.com/blog/sms-custom-commands.html#comment-22107))

> Anonymous says:
>
> Nice one. 
>
> Is it possible to intercept sms in those possible platform using codenameone?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsms-custom-commands.html)


### **Anonymous** — November 29, 2014 at 3:31 am ([permalink](https://www.codenameone.com/blog/sms-custom-commands.html#comment-22272))

> Anonymous says:
>
> Only thru native code. Its too different. 
>
> Also Android is changing the way in which it intercepts SMS so its not really useful as it was before.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsms-custom-commands.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
