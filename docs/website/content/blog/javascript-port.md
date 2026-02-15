---
title: Codename One in the Browser
slug: javascript-port
url: /blog/javascript-port/
original_url: https://www.codenameone.com/blog/javascript-port.html
aliases:
- /blog/javascript-port.html
date: '2015-04-21'
author: Steve Hannah
---

![Header Image](/blog/javascript-port/html5-banner.jpg)

We are very excited to announce the alpha release of the Codename One Javascript port. This brings us one  
step closer to the coveted _write once run anywhere_ ideal. Starting with Codename One version 3.0, you will  
be able to deploy your projects as Javascript applications that run directly in the browser. The process is simple:

  1. Select the “JavaScript” build target from the right click menu.

  2. Log into the Codename One build server.

  3. Download the application as a .zip archive that will run on any web server – or try the app instantly in your  
browser using the “Preview” link.

## Limitations

I mention above that this is an alpha release because  
[not all features have yet been implemented](http://www.codenameone.com/js-port-status.html). The port is still  
under active development, and most/all of the Codename One platform features will be implemented over the  
next few months. If you build an app and it doesn’t work, it is likely due to a feature not being implemented yet.

I have created a [Project Status](http://www.codenameone.com/js-port-status.html) page for the project to record  
the status of each feature.

## Browser Support

The goal is for this port to work in all modern browsers, but development has primarily occurred on Chrome,  
Safari, and Firefox (on the desktop), with mobile testing on Android’s Chrome browser and iOS’s Safari browser.  
Once all of the features are complete, I will focus on ensuring support for the other major browsers (i.e. IE).

## Performance

When I first started working on the port, I didn’t know whether it would even be viable, performance-wise. I feared  
that we might finish the port and find that the interface was sluggish, unresponsive, and clunky. The single-threaded  
architecture of Javascript was viewed as a limitation, and it was thought that the EDT might cause the Javascript  
main thread to lock and block the UI, causing the dreaded “This page is unresponsive” alert in the browser.

I am happy to say that our fears were unfounded. Performance, on the desktop, seems comparable to the current  
JavaSE port – and in some cases it is even better. The true test of performance, however, is on mobile devices.  
On newer devices (e.g. iPhone 6, iPad Air 2) the port is quite responsive; Animations and transitions are smooth –  
though not quite as smooth as in the iOS port. On the previous generation of devices (e.g. Nexus 5, Nexus 7),  
I would describe the port as “usable”, but animations and transitions are a little jerky. As we move into even older  
devices, the apps remain usable, but the slow responsiveness, animations, and transitions become more  
noticeable.

So… at present, your apps should work **very** well in desktop browsers and quite well in new mobile devices.

## The Secret Ingredient: TeaVM

As recently as 4 months ago most of us believed that a Javascript port of Codename One was impossible due to  
Codename One’s extensive use of threads and concurrency, for which Javascript has no equivalent. Developers  
had been working for years on solutions to try to add threading to the browser, but no full solution existed. And  
Codename One needed a full solution. It needed all of the bells and whistles of Java’s threading features:  
wait/notify/synchronized, etc..

The turning point was when Alexey Andreev told me that he had a way to support threads in [TeaVM](http://teavm.org/)  
using a sophisticated transformation of the Java code into continuations. This break-through was nothing short of  
amazing, and it paved the way for Codename One in the browser. One of the most surprising things is how  
performant TeaVM is able to be despite these transformations. Alexey has taken great care, each step of the way,  
to ensure that the generated Javascript code is both concise (for a small file size), and performant. Benchmarks  
have shown the code to be as fast or faster than GWT on the same code – though we can’t really test that on  
Codename One since GWT doesn’t support threads – and thus can’t run Codename One.

## The Future

Shai always reminds me to just take this one step at a time, and not to get ahead of myself. But let’s cast that  
aside for a moment and ruminate on the future potential of Codename One now that it can run inside the browser.  
I believe that Codename One is currently the closest thing to an heir-apparent to Swing that exists; and indeed  
the API is quite similar to Swing. It was carefully crafted to be easily portable to other platforms. Many suggested  
features or APIs have been rejected by Codename One because they wouldn’t be portable, or wouldn’t fit into the  
WORA paradigm.

Being able to run inside the browser opens up a whole array of possibilities for us Codename One developers.  
Enterprise applications that must target the “web first”, can now be written entirely in Java. No more messing  
around with HTML and Javascript for the client side. Write the application once, deploy to the web, and also  
provide native application bundles for users that require the enhanced experience of a native app.

This port could also provide a gateway to some of the more niche platforms like Firefox OS and Chrome OS  
where the small user-base makes it hard to justify a full native port.

Ultimately, the future direction will depend on you, the developers who are using Codename One to realize your  
creative solutions. How do you see web deployment fitting into your development cycle? How can the port be  
improved to meet your needs? We want to hear from you.

## Same Origin

One of the limitations of JavaScript is the [same origin policy](http://en.wikipedia.org/wiki/Same-origin_policy)  
which means you can’t connect to a server other than the one the app was delivered from. Since resources  
(images, res files etc.) are stored in the server this makes the JavaScript impractical for local execution  
by just opening a file. So you can’t download the build result zip, extract it and run it in your browser, you  
need to place it in a webserver for it to work.

As part of that work we also included a special servlet that will allow you to open a connection request to  
any destination. All requests will be proxied thru that servlet and thus allow you to connect to any domain  
you choose seamlessly.

However, we wanted developers to be able to preview their build results easily without requiring a specific server  
setup. To that end we generate a monolithic HTML file build result that we provide as a “preview” that you can  
open using the QR code or preview link as a result of the build. Its not as optimized as the actual result but you  
can see it instantly without setting up a server.

## Availability

You will be able to send a JavaScript build with the next update of the plugin coming really soon!

During the technology preview phase we will allow everyone to build to the JavaScript target and experience the  
technology. Once the port enters beta status it will become an enterprise only feature since it was the funding  
from enterprise customers that allowed this work to happen.

Continued support from enterprise accounts is crucial for the growth of Codename One, it will allow us to build  
more offerings such as this and expand/bolster our existing offerings.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — April 22, 2015 at 10:33 pm ([permalink](/blog/javascript-port/#comment-22034))

> bryan says:
>
> Have you got any demos that can be downloaded ?
>



### **Shai Almog** — April 23, 2015 at 10:43 am ([permalink](/blog/javascript-port/#comment-22019))

> Shai Almog says:
>
> Good point. We will add some demos to the website.
>



### **glegris** — April 29, 2015 at 2:37 pm ([permalink](/blog/javascript-port/#comment-22362))

> glegris says:
>
> Great work !
>
> Did you also consider Mozilla’s J2ME VM in Javascript: [https://github.com/mozilla/…](<https://github.com/mozilla/j2me.js>) ?
>
> As it provides a J2ME layer, It could avoid to write a specific port for IO and graphic primitives.
>
> Nevertheless, not sure the project is as mature as TeaVM
>



### **Shai Almog** — April 29, 2015 at 5:27 pm ([permalink](/blog/javascript-port/#comment-24178))

> Shai Almog says:
>
> I wasn’t familiar with that project. Interesting.  
> Either way I think the TeaVM approach is probably superior in terms of compatibility to newer Java language features rather than CLDC limitations, good to know about it regardless…
>



### **Thomas Yuen** — May 11, 2015 at 1:42 pm ([permalink](/blog/javascript-port/#comment-22092))

> Thomas Yuen says:
>
> It is good to see js support but if it is only for the enterprise (the “rich”) customer, it is not so good (bad for biz)
>



### **Shai Almog** — May 11, 2015 at 2:37 pm ([permalink](/blog/javascript-port/#comment-22373))

> Shai Almog says:
>
> Enterprise subscribers paid for this port so it wouldn’t exist without them.  
> More enterprise subscribers == more developers == more features and capabilities for everyone.  
> So its great this is an enterprise only feature as it will make some developers upgrade and make us a stronger/larger company. Even if you can’t afford this particular feature I’m sure you can benefit from that.
>



### **Thomas Yuen** — May 11, 2015 at 5:02 pm ([permalink](/blog/javascript-port/#comment-22238))

> Thomas Yuen says:
>
> hope u hv a lot more enterprise cust than professional cust for i am abt to sub as prof. enterprise is bit too much for now.
>



### **KL** — May 14, 2015 at 2:28 pm ([permalink](/blog/javascript-port/#comment-22072))

> KL says:
>
> Hi, Because of “same origin policy”, if my js apps need to do http request to origin server, should I hardcode the server domain/ip inside the apps? If not, how do I open the http request?  
> `  
> ConnectionRequest r = new ConnectionRequest();  
> r.setPost(false);  
> r.setUrl(“http://www.abcdefg.com/hello.php”); // Possible not to hard code the domain name?  
> NetworkManager.getInstance().addToQueue(r);  
> `  
> Kindly advice.  
> * Can I ask here? Or where should I?
>



### **Shai Almog** — May 14, 2015 at 6:50 pm ([permalink](/blog/javascript-port/#comment-21689))

> Shai Almog says:
>
> It should work exactly like it does in the simulator with the full URL’s. Keep in mind that our goal here is WORA so we want the code that works for you on the mobile native app to be the exact same code that works for the JavaScript version. You can ask here (if its relevant to the post) or in the discussion forum or in stack overflow (with the codenameone tag).  
> You can also use pro support if you signup for pro.
>



### **KL** — June 7, 2015 at 12:25 pm ([permalink](/blog/javascript-port/#comment-22304))

> KL says:
>
> Can I use the Display.getInstance().getProperty(“[browser.window.location.host](<http://browser.window.location.host>)”, “DEFAULT”) to get the host/domain name? I tried, but it just return “DEFAULT” ..
>
> Still not implemented?
>



### **Shai Almog** — June 7, 2015 at 3:05 pm ([permalink](/blog/javascript-port/#comment-21542))

> Shai Almog says:
>
> Why do you you expect that to be in Display?  
> You are aware that Codename One isn’t a JavaScript/HTML5 API right?  
> Its a native platform that allows you to target JavaScript, not the other way around.
>



### **KL** — June 7, 2015 at 3:33 pm ([permalink](/blog/javascript-port/#comment-22314))

> KL says:
>
> Thanks, I am clearer now. Have to go through the developer guide again.
>



### **Gareth Murfin** — May 6, 2016 at 2:18 am ([permalink](/blog/javascript-port/#comment-22769))

> Gareth Murfin says:
>
> Just read this again, I can say that the Javascript port works unbelievably well, it’s astounding work.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
