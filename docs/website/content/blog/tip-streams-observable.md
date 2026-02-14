---
title: 'TIP: Streams are Observable in Codename One'
slug: tip-streams-observable
url: /blog/tip-streams-observable/
original_url: https://www.codenameone.com/blog/tip-streams-observable.html
aliases:
- /blog/tip-streams-observable.html
date: '2018-02-26'
author: Shai Almog
---

![Header Image](/blog/tip-streams-observable/tip.jpg)

We got a [pull request](https://github.com/codenameone/CodenameOne/pull/2336) the other day that reminded me of some hidden functionality in Codename One that most developers aren’t aware of: observable input streams. By default Codename One API’s try to return [BufferedInputStream](https://www.codenameone.com/javadoc/com/codename1/io/BufferedInputStream.html) and [BufferedOutputStream](https://www.codenameone.com/javadoc/com/codename1/io/BufferedOutputStream.html) instances from our internal API’s. Those classes aren’t the typical `java.io` versions but rather ones from the `com.codename1.io` package.

That API allows us to add functionality into the streams without breaking the Java compatibility or specs. One such feature is [setProgressListener(IOProgressListener)](https://www.codenameone.com/javadoc/com/codename1/io/BufferedInputStream.html#setProgressListener-com.codename1.io.IOProgressListener-). This is probably better explained with a simple sample:
    
    
    final Status status = ToastBar.getInstance().createStatus(); __**(1)**
    status.setMessage("Reading file");
    status.setShowProgressIndicator(true);
    status.showDelayed(300);
    BufferedInputStream bi; __**(2)**
    if(inputStream instanceof BufferedInputStream) {
       bi = (BufferedInputStream)inputStream;
    } else {
       bi = new BufferedInputStream(inputStream);
    }
    bi.setProgressListener((source, bytes) -> { __**(3)**
        callSerially(() -> status.setProgress(100 * bytes / streamLength)); __**(4)**
    });

__**1** | The toastbar has a special mode that lets us display progress from 0 to 100  
---|---  
__**2** | Since streams would often already be observable the `instanceof ` should be true for a few cases. In the exception this fallback would be reasonable  
__**3** | Notice that the progress listener carries an overhead and might slow your application. This might not be a big deal for some cases  
__**4** | The value is between 0 and 100 so we need to know the stream length in advance for the toastbar API  
  
### Finally

There are quite a few API’s in Codename One that are hidden under the surface. From our vantage point it’s often hard to remember everything that’s there even if we put it there. It’s worth asking us as some things can be hidden from view.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chibuike Mba** — March 1, 2018 at 8:48 pm ([permalink](https://www.codenameone.com/blog/tip-streams-observable.html#comment-21589))

> Chibuike Mba says:
>
> Hi Shai, this week I came across a Java library for bringing Reactive Programming into Java called RxJava [[https://github.com/Reactive…](<https://github.com/ReactiveX/RxJava>)] as I was researching on the topic because I needed the programming pattern in CodenameOne project am working on.  
> After seeing the library’s capabilities and benefits I became interested in it and have been wondering on how to port/extend it into CodenameOne, Android has an extension already called RxAndroid [[https://github.com/Reactive…](<https://github.com/ReactiveX/RxAndroid>)].
>
> Can you guide me on the best way about bringing the RxJava library into CodenameOne?
>



### **Shai Almog** — March 2, 2018 at 4:50 am ([permalink](https://www.codenameone.com/blog/tip-streams-observable.html#comment-21643))

> Shai Almog says:
>
> Hi,  
> I looked at this a while back and thought it would be interesting too. It should be pretty easy to port the standard project to Codename One and wrap it in a cn1lib. It uses a couple of features we don’t support such as atomic values etc. but those should be relatively simple to implement locally to get it working.
>
> The main reason I didn’t port it is that I consider it to be of relatively limited benefit in Codename One. Android has a lot of synchronous IO code that should be serialized. This makes a lot of sense there.
>
> Codename One already handles a lot of these things thanks to NetworkManager albeit not as “elegantly” in some cases. So I just didn’t see this as a personal priority but if you need help I’ll be happy to help with that.
>



### **Chibuike Mba** — March 2, 2018 at 6:53 am ([permalink](https://www.codenameone.com/blog/tip-streams-observable.html#comment-23707))

> Chibuike Mba says:
>
> Thanks for your response. I will really need your help. I want to combine reactive programming with MVVM pattern to mimic something like android fragment in codenameone where your app can behave differently based on device screen size and orientation. For example, on mobile phone you have a list of properties displayed on a form, clicking an item opens another form and displays the property details. But on desktop version of the same app or on tablet landscape orientation one form splits into vertical panels, one panel holds the properties listing while another panel displays selected property details. The app’s screen splits into panels on one device and multiple forms on another device. Using reactivity and mvvm patterns will make this use case easier to accomplish with much cleaner code.  
> What do you think, does codenameone have a way of doing this kind of thing already which i may not know of? Can this patterns benefit codenameone, being write once run anywhere platform?
>



### **Shai Almog** — March 3, 2018 at 9:39 am ([permalink](https://www.codenameone.com/blog/tip-streams-observable.html#comment-23715))

> Shai Almog says:
>
> Fragments are one of the most hated features in Android developer community so I’m not sure why you would want something like that?
>
> We did somethings like this in the kitchen sink which looks very different on phones/tablets and just did that with a simple isTablet() call. I think a lot of engineers overthink these problems trying to over generalize something that’s easily solvable with an if statement and a couple of methods.
>
> The orientation listener also allows you to create special cases for specific issues. In the latest version of the kitchen sink I removed that code but I didn’t commit it to git yet so you can check it out here: [https://github.com/codename…](<https://github.com/codenameone/KitchenSink/blob/master/src/com/codename1/demos/kitchen/Input.java>)
>
> I just re-arrange the test fields etc. based on orientation and based on whether this is a tablet or desktop.
>
> I think Android is a combination of low level hacks that expose way too much of the implementation details combined with nose bleed high level of abstraction. Both are horrible for developers but both are really hard to remove from Android due to the legacy and binary compatibility requirements.
>



### **Chibuike Mba** — March 5, 2018 at 2:48 pm ([permalink](https://www.codenameone.com/blog/tip-streams-observable.html#comment-23674))

> Chibuike Mba says:
>
> Hi Shai, my aim is not to implement Fragments in Codename One but to utilize device screen sizes as you have point me in the right direction with the Kitchen sink demo link.
>
> For the RxJava, I still need its reactivity and will start the porting process. When ever I encounter challenges I will seek for you assistance.
>
> Thanks.
>



### **Chibuike Mba** — March 7, 2018 at 5:55 pm ([permalink](https://www.codenameone.com/blog/tip-streams-observable.html#comment-23930))

> Chibuike Mba says:
>
> Hi Shai, am on the process of porting RxJava into Codename One by wrapping it into .cn1lib but there is an issue: RxJava has a dependency on this package java.util.concurrent which seem not available on Codename One.
>
> Questions:
>
> Is it legal to copy the missing package source code from openjdk-8 project?
>
> Will there be any conflict in Codename One if java.util.concurrent or other packages are copied into the .cn1lib?
>
> Can I have private conversations with you through email or is this place acceptable to you? as I work on the porting process.
>
> Looking forward for your reply.
>



### **Shai Almog** — March 8, 2018 at 6:26 am ([permalink](https://www.codenameone.com/blog/tip-streams-observable.html#comment-23937))

> Shai Almog says:
>
> Hi,  
> here would be good and so would the discussion forum etc. I like keeping these things in public forums so they are searchable by google if someone is interested in this subject in the future.
>
> Assuming you are setting up a public github fork you can also assign me to an issue and discuss it with me there.
>
> You can define java.util.concurrent classes that are missing although personally I would just rename the dependencies to the RxJava package space to avoid potential conflict.
>
> I can’t comment on the legal implications as this is a touchy subject with Oracle. I would generally prefer the Android source over OpenJDK but in this case I’m not sure if this will be a good idea to begin with.
>
> This specific package is implemented with very low VM specific behaviors that might make things even more complicated. Since there are very few API dependencies into the package I would just build a clean room implementation of that API in pure Java which should be relatively trivial.
>
> Such a clean room port might be a worthwhile contribution to Codename One’s core for people looking to port similar code. If you need some guidance or review to a specific class just point it to me in github and I’ll try helping with this.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
