---
title: New Android Peer Mode
slug: new-android-peer-mode
url: /blog/new-android-peer-mode/
original_url: https://www.codenameone.com/blog/new-android-peer-mode.html
aliases:
- /blog/new-android-peer-mode.html
date: '2016-07-10'
author: Shai Almog
---

![Header Image](/blog/new-android-peer-mode/native-peer-revisited.png)

As [we mentioned recently](/blog/peering-revisited.html) we have a new idea on how peering can be improved  
and we just deployed a this into our build servers in the weekend update. This is highly experimental and might  
crash instantly which is why we hope you give it a test drive and see how it feels before we switch it on by default.

To recap: Peers (or heavyweight components) are OS native widgets. E.g. when you use the `BrowserComponent`  
we effectively load the OS’s native webkit renderer and use that instead of showing HTML ourselves. This is good  
as it allows us to use OS native functionality, however it’s bad because peers have a lot of limitations  
which we covered in depth [here](/blog/understanding-peer-native-components-why-codename-one-is-so-portable.html).

One of the biggest limitations has been that peers are always drawn on top of the UI as they are drawn separately  
from Codename One. With the original implementation of Codename One which was double buffered this made  
a lot of sense, you can’t conceivably implement peers in any other way. However, newer implementations moved  
to a more dynamic architecture so we can take better advantage of hardware acceleration…​

With that we can also use this architecture to draw peers directly into the rendering graph which is exactly what  
we do in the new Android pipeline changes. Since this is such a huge change we left it off by default and you will  
need to set the build hint: `android.newPeer=true`.

__ |  We will remove that build hint in the future so it is undocumented in the developer guide, it’s hard to maintain  
that peer due to the complexity of the fork   
---|---  
  
Please try your apps with the new flag and let us know about crashes, memory leaks or problematic functionality  
that didn’t exist before.

This should allow a lot of very exciting functionality, e.g. features like `ToastBar`, glasspane, layered layout etc.  
will work with peer components and will allow you things like drawing on top of a map, video, browser etc.

Notice that this isn’t available on the simulator/iOS at this time so you can only see this working on Android devices.  
We plan to add something similar to iOS and the simulator as we move forward.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Fabrizio Grassi** — July 11, 2016 at 2:11 pm ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-21427))

> Fabrizio Grassi says:
>
> Could this affect also the text editing?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)


### **Diamond** — July 11, 2016 at 8:44 pm ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-22842))

> Diamond says:
>
> Hi Shai,
>
> I tested this feature on Samsung S5 mini. App crashes when I tried to open web browser. Some transparency on components overlaid on Google Maps and Google Maps requires touching to get refreshed before it shows, while the overlaid component is already shown. These are the issues I found so far, I will keep testing.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)


### **Shai Almog** — July 12, 2016 at 3:19 am ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-24224))

> Shai Almog says:
>
> Right now we didn’t touch the text editing code which is a bit of a special case. We might change it in the future.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)


### **Shai Almog** — July 12, 2016 at 3:21 am ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-22759))

> Shai Almog says:
>
> Thanks. Can you try and get a stack trace from the crash?  
> I’m not sure how well transparency will work in these situations. Transparency requires the underlying component to paint itself and we don’t always have a way to force that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)


### **Jonathan** — August 3, 2016 at 6:14 am ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-22767))

> Jonathan says:
>
> Can you use it on the camera live mode ( Capture.capturePhoto()) ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)


### **Shai Almog** — August 4, 2016 at 4:25 am ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-24204))

> Shai Almog says:
>
> Capture doesn’t use peer components. It’s a monolithic API.  
> One could use a peer component to map to low lever camera native API’s in a cn1lib in a similar way to the native maps implementation. We might do this ourselves at some point but right now our task list is so full I just don’t see this happening.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)


### **Lukman Javalove Idealist Jaji** — August 16, 2016 at 7:09 am ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-24225))

> Lukman Javalove Idealist Jaji says:
>
> Hi Diamond,
>
> Could you help with how you laid components on a Map? I’ve been wanting to do that
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)


### **Diamond** — August 16, 2016 at 10:35 am ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-22760))

> Diamond says:
>
> Hi Lukman, It’s as simple as placing the component in your LayeredPane after adding your map to the form center layout position. remember to add Build Hint “android.newPeer=true” until it’s true by default.
>
> It’s advisable to do this in postFormShow(), if hand-coded form, do it in the addShowListener() and inside CallSerially. Example 1 – Hand-Coded form:
>
> form.addShowListener((evt) -> {  
> removeAllShowListeners();  
> Display.getInstance().callSerially(new Runnable() {  
> @Override  
> public void run() {  
> MapContainer mc = new MapContainer();  
> form.add(BorderLayout.CENTER, mc);  
> form.getLayeredPane().add(FlowLayout.encloseCenterMiddle(myTestingLabel));  
> form.revalidate();  
> form.getLayeredPane().revalidate();  
> }  
> });  
> });  
> Example 2 – GUI form:
>
> @Override  
> protected void postMyForm(final Form f) {  
> Display.getInstance().callSerially(new Runnable() {  
> @Override  
> public void run() {  
> MapContainer mc = new MapContainer();  
> f.add(BorderLayout.CENTER, mc);  
> f.getLayeredPane().add(FlowLayout.encloseCenterMiddle(myTestingLabel));  
> f.revalidate();  
> f.getLayeredPane().revalidate();  
> }  
> });  
> }
>
> THIS code is written here and was not tested.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)


### **Lukman Javalove Idealist Jaji** — August 16, 2016 at 10:47 am ([permalink](https://www.codenameone.com/blog/new-android-peer-mode.html#comment-22529))

> Lukman Javalove Idealist Jaji says:
>
> Ngiyabonga Diamond ..it worked … 🙂 Thanks for your help once again….
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-android-peer-mode.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
