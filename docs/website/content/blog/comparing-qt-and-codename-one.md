---
title: Comparing QT and Codename One
slug: comparing-qt-and-codename-one
url: /blog/comparing-qt-and-codename-one/
original_url: https://www.codenameone.com/blog/comparing-qt-and-codename-one.html
aliases:
- /blog/comparing-qt-and-codename-one.html
date: '2016-04-27'
author: Shai Almog
---

![Header Image](/blog/comparing-qt-and-codename-one/compare-to-qt.jpg)

We get a lot of requests to compare Codename One to other technologies and while we do have a comparison  
page it is somewhat static. Doing a comparison within a blog post does have the advantage of focusing on  
one technology and allowing comments. In this segment we’ll compare the venerable QT to Codename One.

When developers started asking for this comparison I was of the opinion that it made no sense. The technologies  
are so far apart from one another that they defy any sensible comparison. I still think the conceptual difference is  
a bit too great but I’ll try to bridge some of the gap.

__ |  We updated this comparison after the initial publication to include the additional Property Cross section   
---|---  
  
__ |  We updated this again to reflect pricing, terms & lightweight/heavyweight mixing   
---|---  
  
### Background

QT went thru a lot over it’s many years. It’s the basis of the excellent KDE Linux desktop environment which in  
my humble opinion was always superior to gnome. It’s a C++ library & toolchain developed by a company called  
Trolltech that was purchased by Nokia.

Nokia purchased Trolltech with the goal of taking QT into mobile, this went thru many iterations over the years  
but the product that’s available today is radically different to everything available since.

### At a Glance

Table 1. QT vs. Codename One Category | QT | Codename One  
---|---|---  
Language |  C++/QML-JavaScript |  Java  
Cloud Build |  No (requires Mac & Windows for full OS support) |  Yes (can work on Linux for iOS development)  
IDE |  QT Creator or Visual Studio |  NetBeans, Eclipse or IntelliJ IDEA  
Web Deployment |  No |  Yes  
Widgets |  Lightweight |  Lightweight  
Pricing (Monthly no discounts) |  350 USD |  19 USD – 399 USD + Free  
App Distribution |  Only with active subscription |  Perpetual  
  
### In Detail

#### Language

While QT supports QML (an XML flavor) with JavaScript bindings. QT at it’s base is C based. If you need to integrate a native widget like Googles native maps or similar capabilities you need to do this via C.

This might be challenging as most of the integration documentation for various libraries is in Java, Objective-C.  
Binding an iOS/Android SDK to QT might not be trivial. C++ isn’t garbage collected or “safe” and is a challenging  
language for novices.

Codename One uses Java for everything with all of the typical Java advantages (GC, safe memory etc.). When  
integrating with native code Codename One generates “stubs” for the various platforms allowing developers to  
write native Objective-C, Java (Android flavor), C# etc. code.

This makes integrating with native OS capabilities far simpler as developers can literally copy and paste sample  
code from the SDK vendor.

#### Pricing & Rights

QT is free for open source GPL projects. For every other case it’s priced at 295USD per developer.

Codename One has a free commercial license and a full license starts at 19USD.

Codename One sees the apps as owned by you, once built you are free to sell and redistribute them even after canceling a subscription. QT [requires](https://www.qt.io/faq/) licensing fees for distribution. If you stop paying the monthly licensing fee you need to stop distributing the applications you’ve already built.

#### Cloud Build

Codename One’s cloud build capability is unique. It allows developers to build a native application using the  
Codename One cloud servers (Macs for iOS, Windows machines for Windows etc.) This removes the need to  
own dedicated hardware and allows you to build native iOS apps from Windows and native Windows apps  
from your Mac or Linux machine.

This makes the installation of Codename One trivial, a single plugin to install. This isn’t true for development  
environments that don’t use that approach.

#### IDE

QT has its own custom IDE with GUI builder. It also has a plugin to support Visual Studio which is quite popular  
in the C++ developer community.

Codename One integrates with all major Java IDE’s thru a standard plugin. The plugin delivers the simulators  
and visual tools for Codename One as well as the device building capabilities.

#### Web Deployment

QT is very portable and so is Codename One. Both support many platforms but there is one that Codename One  
can support that QT might find unreachable: JavaScript.

Codename One supports the process of compiling an application (threads and all) into a JavaScript application  
that can be hosted on the web. This is done by statically translating the Java bytecode to JavaScript obfuscated  
code.

#### Widgets

The one point of similarity between QT and Codename One is that both frameworks take the lighweight widget  
approach for greater flexibility and portability. However, as far as I can tell only Codename One supports lightweight/heavyweight mixing.

Integrating a component like Goolge Maps into QT and drawing on top of it will require a lot of work. This is seamless in the current version of Codename One.

### Property Cross Comparison

The PropertyCross demo was built as a tool that allows us to compare two cross platform frameworks, as such  
there are versions of the demo for many such platforms. You can check out details of the Codename One  
implementation [here](/blog/property-cross-revisited.html). The github repository for this demo  
is [here](https://github.com/codenameone/PropertyCross/).

The [QT version of property cross](https://github.com/tastejs/PropertyCross/tree/master/qt) includes 3 projects:  
– The main application QML UI – this includes code that is mostly declarative but quite a lot of it. It defines the UI  
of the application and not much more  
– The lib directory includes the CPP files and header files for the project  
– A special test project that we’ll ignore for now as this isn’t covered in other discussions

It’s pretty great that QT doesn’t require separate projects for every OS. It does have some theme directories but  
most of the code including the UI is common. This is because QT and Codename One share a lightweight  
architecture that increases portability.

The code isn’t as verbose as some other implementations it is roughly 2x-3x larger than the Codename One  
project in lines of code. The project seems to be missing the application launcher screenshots and the icon files  
for the various supported DPI’s. While most of the resources would probably be adaptable in a portable way  
icons and splash screens probably won’t be.

This is something that Codename One can handle seamlessly thanks to the build servers but QT might have an  
issue with.

### Final Word

Our opinions are obviously biased but I think we did a reasonably fair comparison. I actually like and respect QT  
which makes it a great candidate for comparison.

If you think we misrepresented Codename One or QT in any way let us know in the comments.

Please use the comments section for suggestions of future comparison segments.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — April 30, 2016 at 8:37 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22630))

> bryan says:
>
> Gluon vs CN1
>



### **Shai Almog** — May 1, 2016 at 4:39 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22641))

> Shai Almog says:
>
> I won’t blog about Gluon right now. It’s  
> not a “real” product with shipping mobile apps. Comparing it to us would  
> present it in a light where real apps can be built with it where in  
> fact it is so inherently broken that I can’t find a single good thing to  
> say about it.
>
> I don’t want to write a comparison to a  
> product that I can’t say anything positive about. Gluon is so  
> problematic I can’t even call it a real product as much as a mish mash  
> salad of half baked technologies. It’s a proof of concept based on  
> hugely complex 3rd party code that can’t possibly be understood by the  
> guys shipping it.
>
> It is based on Java FX, the only reason  
> that still exists is because Oracle is a corporate hoarder incapable of  
> throwing anything away. FX is dead on the desktop and Oracle made the  
> smart business move of not releasing it to mobile as it is technically  
> unworkable in mobile. This was proven by Adobe who had a similar  
> architecture fail on mobile regardless of Apple (it failed on Android  
> too). 3rd party Scene Graph implementations are REMARKABLY hard to tune  
> in a performant way and integrate with mobile GPUs/native widgets.  
> Oracle who had some of the most amazing graphics developers ever  
> understood that this is impratical.
>
> Their VM used to be  
> RoboVM on which performance still sucked and it was a VERY FAST AOT VM.  
> Now that RoboVM is dead their alternative is OpenJDK which is  
> interpreter based… How can someone who even suggest an interpreter for  
> something as complex and slow as a graphics intensive mobile  
> scene-graph engine be taken seriously?
>
> While  
> they are focused on re-inventing this HUGE amount of code that they  
> can’t possibly test properly (e.g. [java.net](<http://java.net>) can’t be implemented  
> correctly on iOS, sqlite on Android/iOS is totally different etc.) they  
> are also picking up the scene builder, custom widgets and IDE plugins…  
> They will have to also pick up a VM as none of the existing options can  
> compete with RoboVM in terms of speed/breadth. RoboVM was too big for  
> one company to maintain and Gluon is already over stretched by a few  
> miles so what’s another insurmountable challenge to the pile…
>
> This  
> shows lack of focus and understanding of the scope of work required. I  
> can’t possibly take a company that takes up something clearly  
> impractical and unworkable as serious.
>
> To make matters  
> worse they have no viable business model. This means they rely on  
> consulting fees and nag screens which is a very problematic method of  
> building a sustainable business. RoboVM took a similar path of  
> unsustainable business practice and that ended in the only place it  
> could end. We like to think that those things don’t matter but if you  
> want your technology to exist tomorrow you need it to be open,  
> accessible, understandable and have a viable long term business model.
>



### **bryan** — May 1, 2016 at 12:49 pm ([permalink](/blog/comparing-qt-and-codename-one/#comment-22790))

> bryan says:
>
> That’s a pretty good comparison 🙂
>



### **Johan Vos** — May 2, 2016 at 9:14 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22519))

> Johan Vos says:
>
> If you want an answer without lies and FUD, feel free to contact us (Gluon) directly.
>



### **Shai Almog** — May 2, 2016 at 9:17 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22809))

> Shai Almog says:
>
> FUD is subjective but which part of my response is untruthful?
>



### **Felix Bembrick** — May 2, 2016 at 11:15 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22848))

> Felix Bembrick says:
>
> Which part is untruthful? Just the bit after “I won’t blog about Gluon right now” which even itself was a lie.
>
> Underestimating Gluon and the passionate people like me who support what they are doing will be your own downfall.
>
> Pride comes before a fall and given the humungous amount of pride that’s on display here I would anticipate your fall will be of nuclear proportions.
>
> JavaFX rules and Gluon is making it rule even better.
>



### **Shai Almog** — May 2, 2016 at 11:24 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22721))

> Shai Almog says:
>
> Not blog, comment. Big difference.
>
> I’ve worked in this industry and at Sun for quite a while (90’s). Seen these things come and go and I know the complexities involved in both JavaFX on mobile and in mobile Java in general (not to mention Sun/Oracle politics). You are conflicting pride and expertise.
>
> I know my stuff and I advocate it, that’s pride. You are looking at my expert opinion and discarding it as “just pride” well… That’s the kettle…
>
> It’s true that I am discounting you just like I did RoboVM which ended pretty much where I predicted which is the exact reason we chose not to go with them in 2013.
>
> When you say JavaFX “rules” you might as well spell that with a Z. That is not a fact and shows the level of thought you put into this. Please feel free to disprove a single factual point I made above…
>
> E.g. let me give you a nugget, FX fans constantly talk about FX going into VW and making a splash. Guess what, Codename One has shipped in millions of cars for years and we didn’t even mention that ONCE!
>
> If you think FX will pick up because of one deal or another I suggest you take your emotional attachment. Put it in a box and take a step backwards. You might like FX, it’s an elegant API designed by brilliant people for whom I have the utmost respect. But that doesn’t change one iota of the facts I wrote above. I’m waiting anxiously for an itemized list of “lies”.
>



### **Felix Bembrick** — May 2, 2016 at 11:25 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22857))

> Felix Bembrick says:
>
> BTW, just to fill those massive voids known as ignorance, are you even aware that there are already 2 forks of RoboVM so the technology is actually more alive than ever?
>
> And, gosh, of course Gluon never realised that a fully interpreted version of Java will never be viable on mobiles!
>
> You are basically calling Johan an idiot and the one thing that I am more certain about him than anything else is that he ain’t no idiot!
>



### **Shai Almog** — May 2, 2016 at 11:28 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-21484))

> Shai Almog says:
>
> One minor thing I forgot to mention. I actually worked with the JavaFX  
> Script team to get JavaFX onto mobile. I know a thing or two here. Also  
> consulted to the JavaFX embedded team lead on graphics and GPU usage…
>
> I’m well aware of these forks. I’m also well aware that the RoboVM team claimed that Java 9 compatibility which came after the forks was the hardest thing they worked on since building RoboVM itself. So you are saying they lie?
>
> I’m calling Johan ignorant and arrogant. A desktop developer walking into mobile expecting things to work in the same way and picking up a HUGE pile of code expecting it to “work” in production.
>



### **Felix Bembrick** — May 2, 2016 at 11:29 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22448))

> Felix Bembrick says:
>
> Oh well, all I can say to you is “keep digging”. That’s already a mighty big sink hole that your life’s investment is about to fall into…
>



### **Felix Bembrick** — May 2, 2016 at 11:31 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22610))

> Felix Bembrick says:
>
> Do you even know that Gluon products are already working and working very well on mobiles? I mean never let the truth get in your way of dissing a Java Champion or a vastly superior technology to whatever it is that you’re peddling…
>



### **Shai Almog** — May 2, 2016 at 11:34 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22467))

> Shai Almog says:
>
> Being a Java champion (I’m only a rockstar as I don’t suck up to Jim) I couldn’t care one iota.
>
> Running something on a device is miles away from shipping a product or passing a TCK.
>
> I’m invested but you still fail to show one thing that was a “lie” in my post.
>



### **Felix Bembrick** — May 2, 2016 at 11:35 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22577))

> Felix Bembrick says:
>
> And I am sorry, I admit that cannot disprove any factual comments you have made.
>
> But that’s because i cannot disprove something which is yet to exist.
>



### **Felix Bembrick** — May 2, 2016 at 11:39 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22631))

> Felix Bembrick says:
>
> Gosh, did I use the word “pride”? I should have used “arrogance”, “delusion” and/or “narcissism”.
>



### **Shai Almog** — May 2, 2016 at 11:44 am ([permalink](/blog/comparing-qt-and-codename-one/#comment-22642))

> Shai Almog says:
>
> So let me get this strait.  
> Johan comes on our blog. Calls me a liar for posting a factual true opinion made by myself with my bias clearly evident and displayed…
>
> And you then come off and claim that I somehow am unaware of “facts” that I am clearly aware of and I’m not OK?
>
> I’m biased true… I might have a cognitive dissonance. That is always true…
>
> Narcissism?
>
> You would laugh if you actually knew me.
>
> Arrogance?
>
> You betcha I’m arrogant. Comes with being an expert too.
>
> Delusion?
>
> I can’t possibly tell that about myself. However, I suggest you take a step backwards and evaluate if maybe the roles are reversed here.
>
> FX couldn’t succeed with all the resources of Sun/Oracle behind it, you can claim that I’m arrogant and delusional… But you are insane if you don’t take up these mantles yourself….
>
> Gluon is taking up not Just FX, but the whole ecosystem and is now also picking up the VM work, IDE integration and all the slack left by RoboVM. We have people who know every piece of Codename One inside out. We worked on it since 2007 and we know everything there is to know. You guys are dealing with a HUGE black hole of software some of which still doesn’t exist and you are trying to sell it as if it’s a sellable product. But I’m arrogant and delusional?
>



### **Felix Bembrick** — May 2, 2016 at 12:29 pm ([permalink](/blog/comparing-qt-and-codename-one/#comment-22868))

> Felix Bembrick says:
>
> Well, you have still failed to even concede that your very first statement “I am not going to blog about Gluon right now because the babbling that follows is commenting, not blogging as the two are radically different polar opposite concepts” was a lie.
>
> And that was the high point of your babbling.
>
> It was all downhill from there, just like your company’s future bottom line.
>
> And to say that I would laugh if I actually met you is a given as I have basically been laughing my head off since you started firing blanks at Johan.
>
> And, you might think you are the smartest and most experienced person with these technologies on the planet but that’s only true because you’re on some other planet than the rest of us.
>
> Let me just say you have no idea who you are feebly trying to trade blows with. I too worked at Sun (but that wasn’t challenging enough). I have also worked on what was recently voted the second best animated film of all time in terms of the rendering and animation quality. I have optimised scene graphs before. I know more about GPU technology than you do about how to behave like a tool. Johan and I could together take down your entire company all by ourselves if you continue to motivate us to do just that.
>



### **Shai Almog** — May 2, 2016 at 1:23 pm ([permalink](/blog/comparing-qt-and-codename-one/#comment-22791))

> Shai Almog says:
>
> Great to hear that you worked at Sun. Did you do mobile too?
>
> Good luck to you guys with gluon, I’m sure you will reach the same level of market success as FX.
>
> Notice that if we fail you will fail too but the opposite isn’t true. We are the established leader in the Java mobile WORA tools but to take that position you need to surpass in acquisitions not take business from us. I hope you have someone who understands how the business side of things works at your company.
>
> We are not your competitor just like you are no threat to us. At most you could in theory slow our growth but if we go down in sales it means the market is shrinking which is bad for us but horrible for you as you aren’t yet established.
>
> I suggest you focus first on understanding “what the market is”, it sounds like a “stupid business speak” but I suggest you understand these concepts if you want to succeed. Then take a step back and objectively compare the end user/developer proposition of our respective tools. You will notice that you guys target a very different demo/value proposition. If you carry that analysis to its full extent you will also see what I saw in my first meeting with Niklas at JavaOne 2013. There is no business model for your approach other than consulting.
>
> Consulting can be moderately profitable, but you can’t sustain long term platform development on consulting fees. Eventually you need to pick and choose if you are a consulting shop or a startup and at that point you need to sit down with someone who understands business and try to imagine the growth that’s achievable in realistic scenarios.
>



### **Felix Bembrick** — May 2, 2016 at 6:31 pm ([permalink](/blog/comparing-qt-and-codename-one/#comment-22875))

> Felix Bembrick says:
>
> Thanks for your advice. It’s mostly very accurate and worth reading. It’s just the bit after “Glad to hear that you worked at Sun” that has that “babbling” quality you do so well that I will wisely ignore.
>
> Would you appreciate me endorsing you for the skill of babbling? You are clearly at the elite level.
>
> You know, I am fairly sure Johan is a lot like me in one aspect. The more people tell us that something “can’t be done”, the more motivated we become to prove such people wrong (which seems a little superfluous in your case).
>
> So, let the Hunger Games begin. I’ll play the part of Katniss and you can continue in your role as Snow (or Coin).
>
> The very name of your company suggests you are so narrow minded that you think you are the only technology that anyone ever needs. One size fits all. You are so disrespectful and arrogant to your competitors that I am surprised if there’s room in your office space for anything more than just your ego.
>
> But I love to see this. You are such a classic example of the kind of CEO that thinks their business model is “Competitors? What competitors?”.
>
> You will only see them when you are broke, unemployed and living in a trailer park.
>



### **Shai Almog** — May 2, 2016 at 7:16 pm ([permalink](/blog/comparing-qt-and-codename-one/#comment-22812))

> Shai Almog says:
>
> Like I said before takes one to know one but I get that you don’t like me and I don’t mind one bit.
>
> Notice a big difference about our approaches, you are writing in our  
> blog deriding me with insults on a personal level. Yet I don’t kick you  
> out. That’s pretty evil of me. I’m just mean in that way….
>
> Codename One is about unifying everything to a single technology that hides the complexity not using a single technology for everything. But feel free to concentrate all your anger at me and use me as your motivation.
>
> You didn’t get what I said at all. You don’t compete with us. If you do then you are in big trouble.  
> We are profitable but we’re not the leader in cross platform by a long shot. We are not even the leader in mobile Java (that’s Android). If you are going after us then you are going after a small market share relatively and that’s “stupid”. I know business books are boring but I suggest picking a few up and learning about “go to market” strategies. But obviously you treat everything I say as evil and malicious so I’m just trying to waste your time with an understanding of how business works and how to define your competitors properly to confuse you.
>
> But if it helps you work at night with the dream of somehow putting the big bad me and my family into the streets because I’m such an awful human being who dares to think you are wrong and is wasting his time in his own blog post explaining that to you then feel free to concentrate your anger and contempt at me 😉
>



### **Felix Bembrick** — May 2, 2016 at 8:19 pm ([permalink](/blog/comparing-qt-and-codename-one/#comment-22849))

> Felix Bembrick says:
>
> If I offered you a tissue, would that make you feel any better?
>
> And please stop assuming you know stuff that neither I nor seemingly anyone else could possibly know.
>
> I don’t need lectures from you on business, strategic planning or on anything for that matter (except of course on babbling mastery where I admit you have me beaten).
>
> And insults? Have a look at where they started. It was only then that I even decided to get involved (regrettably).
>
> But thanks for showing such extraordinary tolerance and generosity for not booting me out of *your* forum. I mean, it’s not like that would appear as a concession of defeat or anything like that
>
> Everyone knows it’s just because you’re such a kind hearted, polite, respectful and open minded person who knows more about business than Warren Buffet and has an IQ of 250.
>
> Oh, and who doesn’t suck up to Jim 😉
>



### **Shai Almog** — May 2, 2016 at 8:38 pm ([permalink](/blog/comparing-qt-and-codename-one/#comment-22722))

> Shai Almog says:
>
> Despite your attitude towards me I wish you no harm, on the contrary I honestly do hope that you guys do well. I don’t think it will hurt our business just like RoboVM didn’t impact our business much neither when they launched nor when they left (just made CPM and SEO slightly more expensive).
>
> I think you need to re-evaluate your words and the line between a strong disagreement and animosity.
>
> I didn’t “start” I wrote a factual opinion in a comment response to a question posted to our blog. Your colleague came in, called me a liar without basis. You then come in with name calling and repeated assaults on what you perceive as my personality flaws.
>
> No I don’t suck up to Jim and never have which is why we never got free publicity from Oracle despite paying full price for a booth at J1 a couple of years ago and speaking for years.
>
> Unlike you I don’t think you guys are stupid, I think you are wrong but not stupid.
>
> You obviously ignore everything I say so lets cut it here.
>



### **Kyri Ioulianou** — June 15, 2016 at 5:36 pm ([permalink](/blog/comparing-qt-and-codename-one/#comment-24212))

> Kyri Ioulianou says:
>
> You destroyed this guy
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
