---
title: Sockets & MultiLine Trees
slug: sockets-multiline-trees
url: /blog/sockets-multiline-trees/
original_url: https://www.codenameone.com/blog/sockets-multiline-trees.html
aliases:
- /blog/sockets-multiline-trees.html
date: '2014-01-07'
author: Shai Almog
---

![Header Image](/blog/sockets-multiline-trees/sockets-multiline-trees-1.png)

  
  
  
  
![Picture](/blog/sockets-multiline-trees/sockets-multiline-trees-1.png)  
  
  
  

[  
Steve  
](http://sjhannah.com/)  
recently added an implementation of a  
[  
sockets library  
](https://github.com/shannah/CN1Sockets)  
which is pretty cool and very timely since we are just about to release our own socket library and unlike his work which supports all Codename One targets we currently only support iOS & Android.  
  
  
  
  
At the moment we only support TCP sockets, we support server socket (listen/accept) on Android but not on iOS. You can check if Sockets are supported using the Socket.isSupported() and whether server sockets are supported using Socket.  
  
  
isServerSocketSupported().  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
To use sockets you can use the Socket.connect(String host, int port, SocketConnection eventCallback) method. To listen on sockets you can use the Socket.listen(int port, Class scClass) method which will instantiate a SocketConnection instance (scClass is expected to be a subclass of SocketConnection) for every incoming connection.  
  
  
  
  
  
This simple example allows you to create a server and a client assuming the device supports both:  
  
  
  
  

* * *

Also in the coming update to Codename One we will include support for multiline entries in trees and more arbitrary tree node types. To support that we added a new flag to indicate if the tree is multiline in which case each node would be a SpanButton instead of a button. This is pretty simple for the basic use case just invoke setMultilineMode(true).  
  
  
  
  
  
  
Unfortunately for the complex use cases up until now when we customized a tree entry one had to override the method: protected Button createNodeComponent(Object node, int depth), which relies on the Tree’s usage of buttons. So that method is now deprecated (although it will still work for now).  
  
  
You should now override:  
  
protected Component createNode(Object node, int depth), which allows you to return any component type. However, the tree needs methods to receive a click event from the node and to set an icon both of which aren’t available in Component. So for that purpose you might need to override: protected void bindNodeListener(ActionListener l, Component node), and: protected void setNodeIcon(Image icon, Component node). Just override them to support your custom component type unless its a Button/SpanButton (or subclass) both of which are supported.  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 8, 2014 at 1:15 pm ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-21722))

> Anonymous says:
>
> I like the more event-driven approach to sockets. Reminds me a little of the Node.js model. It removes a lot of the complexities around socket programming.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — January 8, 2014 at 1:47 pm ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-22075))

> Anonymous says:
>
> Thanks. 
>
> We were mostly trying to imitate the concepts of NetworkManager/ConnectionRequest for consistency and simplicity. The main pain point was server sockets, we just couldn’t find the right abstraction that will work with GCD on iOS. Unfortunately there is very little samples on iOS socket programming and a lot of developers incorrectly use POSIX sockets on iOS. This is a huge mistake, they work on wifi but they don’t guarantee to activate the radio and so they might fail to connect simply because the phone is trying to save some battery life. 
>
> Unfortunately GCD doesn’t play well with the select() based behavior of Java sockets.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — January 12, 2014 at 10:16 am ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-21874))

> Anonymous says:
>
> Great to see that you are releasing a socket library. I have also checked out Steve’s CN1Socket library. I am working on home automation from mobile devices and need to use UDP sockets. I was unable to find UDP socket support in CN1Socket and from the text above, I see you that your library might also not have UDP support. Could you suggest any alternatives? I need to support iOS primarily right now, and android in a later release.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — January 12, 2014 at 3:58 pm ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-21620))

> Anonymous says:
>
> At this point in time we have no such plans since none of our enterprise customers asked for this functionality. I’m assuming that you are not an enterprise account holder, right? 
>
> You can probably extends Steve’s approach to support UDP or just use it as a reference to create your own cn1lib for UDP.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — March 21, 2014 at 5:32 pm ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-21621))

> Anonymous says:
>
> Shai, 
>
> I am using NetBeans with the current 1.0.70 and I do not see any Socket or SocketConnection class as is shown in the javadoc online: 
>
> [https://codenameone.googlec…](<https://codenameone.googlecode.com/svn/trunk/CodenameOne/javadoc/com/codename1/io/Socket.html>) 
>
> [https://codenameone.googlec…](<https://codenameone.googlecode.com/svn/trunk/CodenameOne/javadoc/com/codename1/io/SocketConnection.html>) 
>
> I will use Steve’s library, but I would rather stay within Codename One as much as possible. Am I missing something in the plugin update process?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — March 22, 2014 at 4:58 am ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-22026))

> Anonymous says:
>
> Go to project properties, Codename One settings and click update client libraries.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — March 24, 2014 at 3:11 pm ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-21968))

> Anonymous says:
>
> Unfortunately this path is not there. I do not see Codename One settings. I am currently using Netbeans 7.3. I am looking into this and i will update when I find the answer.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — March 24, 2014 at 3:35 pm ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-24162))

> Anonymous says:
>
> In case this helps someone else: It appears that my property window was not expanding all the way to reveal the “Update Project libs” button. The end to a frusterating hunt.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — April 27, 2014 at 6:42 pm ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-21436))

> Anonymous says:
>
> Hello Shai! 
>
> I’m developing an application for sending OSC messages and now I have the following questions. The Codename One supports the development of Sockets for the UDP protocol?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — April 28, 2014 at 2:21 am ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-21847))

> Anonymous says:
>
> Hi, 
>
> no we don’t. However, you can write this using native code just like Steve implemented TCP sockets in the library linked above.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — December 7, 2014 at 8:23 am ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-22080))

> Anonymous says:
>
> Hi Shai, 
>
> I’m looking for something like Android’s Expandable List in Codename One. Would a tree with an overridden createNode() now be the best choice? Or would you recommend using List and a Renderer containing another List?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — December 7, 2014 at 8:55 am ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-21937))

> Anonymous says:
>
> Oh, I forgot to mention: Unfortunately, I can’t just use the Tree as it is, since I need Buttons on the right hand side of each entry.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)


### **Anonymous** — December 7, 2014 at 2:54 pm ([permalink](https://www.codenameone.com/blog/sockets-multiline-trees.html#comment-22135))

> Anonymous says:
>
> You can have buttons on the right hand side without a problem with a Tree as explained in the above post. You can just create a container with anything you want in it (multiline being the common example).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsockets-multiline-trees.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
