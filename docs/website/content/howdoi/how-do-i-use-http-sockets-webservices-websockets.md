---
title: USE HTTP, SOCKETS, WEBSERVICES AND WEBSOCKETS
slug: how-do-i-use-http-sockets-webservices-websockets
url: /how-do-i/how-do-i-use-http-sockets-webservices-websockets/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-http-sockets-webservices-websockets.html
tags:
- basic
- io
description: Networking options explained
youtube_id: -M957AAi-vk
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-17.jpg
---

{{< youtube "-M957AAi-vk" >}} 

#### Transcript

In this short video I’ll cover some core concepts of networking and webservices on mobile devices and Codename One First and most importantly for those of you coming from the desktop or server world, this might not be a shock that network on mobile devices is unreliable. However, the extent of this is sometimes surprising to developers who are new to mobile. That’s why many low level networking strategies are discouraged on mobile devices.

Another big surprise for developers is that Apple literally blocks HTTP on their devices unless you have a really good excuse. If you will try to connect to HTTP from an app that doesn’t explicitly enable that the connection will just fail. You need to use HTTPS or ask for a special permission, notice that if you ask for that permission and don’t have a good enough reason your app will be rejected from the appstore.

Sockets are usable on mobile devices but they are flaky and hard to use across NATs and devices. We only got server sockets working reliably on Android, they are possible on iOS but they are pretty different there. As a solution of sort to some of those issues websockets have risen in recent years and are shaping up to be a very interesting midrange option.

The most common networking API in Codename One is Connection request which is paired with the network manager. It’s inspired by JavaScripts asynchronous networking but tries to provide more low level control. The connection request API was developed with a goal of extreme portability and best practices built-in. It’s seamlessly threaded and can automatically work with the EDT as a synchronous or asynchronous API.

We also have some support for the URL class which helps port Java SE code to Codename One. It doesn’t use the network manager API and goes directly to the low level API’s. As a result you need to take care of threads yourself and might need to work thru some low level differences in behavior between platforms.

We have two socket implementations, one is builtin to Codename One and works asynchronously thru a callback. The other was implemented as a cn1lib and is a lower level API that works directly with the streams. Sockets are inherently very low level and are an advanced API to use.

Web sockets serve as a middle of the road approach. They are implemented as a cn1lib as well but use a simplified asynchronous callback API. Since common servers already support websockets, the server side should be a breeze. They are relatively easy to work with and allow sending messages back and forth from the server.

Before we go to the code notice that in order to use this code you will need to import the CN class statics.

Creating a hello world get request is as simple as adding a new connection request to the queue. Notice that the second argument indicates that we are making a GET request and not a POST request which is the default. Also notice that the request is asynchronous so it might not have completed after the addToQueue call. So how do we get the actual data from the URL?

There are 3 options…  
First we can override read response and read the stream directly. This is arguably the best approach as we will only read the data once. Read response is invoked on the network thread so make sure not to change the UI from that method! That is a good thing though as all your processing won’t block the event dispatch thread and won’t slow the app noticeably.

The second option uses a response listener to get the result from the connection request. Notice that a response listener occurs on the event dispatch thread so this will slow down execution slightly but you will be able to make changes to the UI directly so the code will be simpler.

The same is true about the last and arguably simplest option. When you use addToQueueAndWait instead of addToQueue the current thread is blocked using invokeAndBlock and we can then extract the data. This is pretty convenient for simple requests and we use that often for those cases.

The builtin sockets use an asynchronous API where the callback method is invoked once connection to the server is established. At that point you will have a thread with two streams and you can just read or write from the streams as you see fit.

Web sockets are easier, you just receive messages and can send them using the web socket API. Notice that you shouldn’t send any message before onOpen was invoked as you might fail badly… Web sockets are excellent for API’s like chat where a server might trigger a message to a device without the device making a request. This is far more efficient than approaches such as polling which can result in serious battery drain and low performance.

I’ve mentioned URL before and indeed you can use the Codename One URL to port existing code but this also begs the question: why not use URL instead of connection request?  
Threading is hard would be the first and obvious answer. This is especially true on devices where QA needs to go far and wide.  
Connection request has some builtin capabilities that bind directly to the EDT for instance addToQueueAndWait, progress indicator etc.  
URL is inherently less portable since it is low level and might expose platform behaviors that you don’t expect a common example is different redirection behavior on 302 for the various OS’s.

Webservices can be implemented in many ways in Codename One, a common approach is the webservice wizard detailed in the developer guide. It generates method calls that map to server code and allow us to generate a remote function invocation on the server.

You can use connection request to invoke rest API’s from the client  
You can use one of the 3rd party or builtin API’s to connect we have some great API’s such as the REST api that lets you invoke server code with almost no effort  
You can use the builtin JSON and XML parsers as well as the Result class for xpath expression style parsing. You can also use some of the 3rd party parsers for JSON and XML in the cn1lib section

Here is a sample rest request from the kitchen sink, you will notice that there isn’t much here, we just parse the data and pass it on based on the response

We’ve ported that older kitchen sink code to use the new Rest API and this code is even easier. It removes the need for additional classes and can be chained to generate a short expression.  
We just get a result as a parsed map we can work with, which is very convenient. This API also has an asynchronous version which is similarly convenient

Thanks for watching, I hope you found this helpful

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
