---
title: Dr. Sbaitso Revisited
slug: dr-sbaitso-revisited
url: /blog/dr-sbaitso-revisited/
original_url: https://www.codenameone.com/blog/dr-sbaitso-revisited.html
aliases:
- /blog/dr-sbaitso-revisited.html
date: '2016-06-06'
author: Shai Almog
---

![Header Image](/blog/dr-sbaitso-revisited/drsbaitso.jpg)

Dr. Sbaitso is one of our newer demos. We wrote it for a workshop at JavaZone a couple of years ago and it  
proved to be an excellent tutorial on many complex abilities of Codename One. It captures images from the  
camera, rounds them, does dynamic search with a chat like bubble interface…​

**Check the live version running on the right hand side thanks to the power of the Codename One JavaScript port!**

But the coolest part is the speech synthesis…​

It uses native interfaces to access the text-to-speech capabilities of the device and “says” what the “doctor” is  
saying. This both demonstrates native access and the rather cool TTS functionality.

### What’s to Improve?

The demo is already so good and relatively terse, there isn’t much to improve about it.

We naturally updated it to Java 8 and the newer terse syntax which cleaned up a bit of the UI. We also replaced  
the next command with an arrow material design icon which is more attractive…​

But the coolest change is that we added native interface support for JavaScript so Dr. Sbaitso now speaks in  
the Chrome browser!

This works really well and runs very smoothly on my desktop, it’s not ideal because speech synthesis isn’t  
widely supported by browsers and I wasn’t able to get it to work on firefox or any other browser. But it’s still  
remarkably cool.

__ |  This pretty much shows off the benefit of having a native framework vs. the pain of web fragmentation…​   
---|---
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ross Taylor** — June 16, 2016 at 7:44 pm ([permalink](https://www.codenameone.com/blog/dr-sbaitso-revisited.html#comment-21630))

> Ross Taylor says:
>
> Hi Shai, would it be possible to create text input that allows for paragraph to be created and emojis to be inserted? Also allow to create a bubble to format text appropriately when needed like bullet points (if pasted from a document that does have them), telephone numbers (tapping on numbers to trigger events like calling or saving to address book) , URL links (underline them and when tapped to open web browser), i.a.w, pretty much like the way bubbles displays the text richly like existing IMs you get now?
>



### **Shai Almog** — June 17, 2016 at 3:44 am ([permalink](https://www.codenameone.com/blog/dr-sbaitso-revisited.html#comment-22697))

> Shai Almog says:
>
> Hi,  
> You can insert emojiis on the device today and it should work fine for this and other apps.  
> However, what you are talking about is called a “rich edit component” we have a cn1lib that supports that somewhere (Steve did it) thru a web browser UI. The problem is that Android and iOS have rather bad support for that concept and it’s rather different between the OS’s.  
> It’s something we wanted to integrate a while back but we didn’t have any actionable user demand which is really needed for such a problematic task.
>



### **ssybesma** — December 30, 2016 at 3:43 am ([permalink](https://www.codenameone.com/blog/dr-sbaitso-revisited.html#comment-23137))

> ssybesma says:
>
> You have to be kidding…all it does is ask you questions about what you just said. Dr. Sbaitso actually is intelligent compared to this. He does more than just that.
>



### **Shai Almog** — December 30, 2016 at 5:20 am ([permalink](https://www.codenameone.com/blog/dr-sbaitso-revisited.html#comment-23256))

> Shai Almog says:
>
> 🙂
>
> That’s the nature of demos. I totally meant to write a decent AI chat and instead focused on the other features. By the time I was done I couldn’t justify writing more code for something that’s meant to teach UI programming and native interfaces…
>
> Feel free to submit a pull request though, all the logic is in [AI.java](<http://AI.java>) so it should be pretty easy.
>



### **ssybesma** — December 30, 2016 at 6:03 am ([permalink](https://www.codenameone.com/blog/dr-sbaitso-revisited.html#comment-23301))

> ssybesma says:
>
> I appreciate that you were not offended and I apologize for being a bit harsh. I do look forward to the eventual release of an application that does Dr. Sbaitso one better. The voice has to be the same, though…sorry. I’m sentimental.
>



### **Shai Almog** — December 31, 2016 at 12:16 pm ([permalink](https://www.codenameone.com/blog/dr-sbaitso-revisited.html#comment-23075))

> Shai Almog says:
>
> If I’d spend time on this I’d work on the AI but I doubt I’ll get any time to work on that. When I initially presented the demo no one knew about the original so the points were a bit lost on them… Plus it made me feel old 😉
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
