---
title: Should Oracle Spring Clean JavaFX?
slug: should-oracle-spring-clean-javafx
url: /blog/should-oracle-spring-clean-javafx/
original_url: https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html
aliases:
- /blog/should-oracle-spring-clean-javafx.html
date: '2015-11-18'
author: Shai Almog
---

![Header Image](/blog/should-oracle-spring-clean-javafx/should-oracle-spring-clean-javafx.jpg)

> If anything, they are rearranging the deck chairs on the Hindenburg!
> 
> Stephen Colbert

We really depend on JavaFX at Codename One, our simulator needs it. Our desktop build uses it and our  
designer tool is based on Swing. We want it to succeed, its important to our business!  
We are not alone, even if you are a Java EE developer and don’t care about desktop programming, keep in  
mind the fact that today’s desktop technology is tomorrow’s server technology.  
E.g.: C++ and Windows (a desktop technology) took the servers from Unix and C. Only to be replaced by  
Java (up until then a web based Applet language) and Linux. JavaScript might not look as a JavaEE contender  
today but as more developers come out of college liking JavaScript and not Java this will affect us all. 

**Note:** If you are a fan of JavaScript/NodeJS or any other such scripting language then  
you are not the intended audience for this post…  
  
This post is for people who love working with Java and want it to move forward. 

When we rented a booth at JavaOne 2014 we got the impression that 90% of the Java developers we talked  
to were enterprise developers. Most of the exhibitors and the highest attended talks were also enterprise oriented.  
As a mobile tool vendor this is a much harder divide to cross than the one between desktop development and  
mobile. It highlights the fact that we need JavaFX to work or make way for something better but we need a  
GUI solution now. 

This post isn’t about whether JavaFX sucks or not. Its not about whether its a good API and it isn’t about  
whether you can create nice looking apps with it (you can). Its about fixing JavaFX or moving away from it  
to something else, its about acknowledging the problems within it rather than presenting an aura of “everything’s  
fine” to fresh Java developers.  
Initially I wrote about some technical issues in JavaFX as well. I decided not to go into that discussion. I have the  
utmost admiration and respect to the guys who created JavaFX. Its impressive in many ways. But good technologies  
also fail and in the next few sections I’ll try to elaborate on this: 

  * Reasoning – Why we all need a Java desktop API strategy
  * Proof – read this if you don’t think there is a serious problem with JavaFX’s traction
  * Why Should We Care? – If you are a Java EE developer who thinks this isn’t your concern please read this.
  * What Are The Options? – how do we move Java forward.
  * How did we get here? – if you are new to Java and this discussion is missing historical context read this first.
  * Final Word – my personal take on the facts I listed here.

### Reasoning

The first step in solving a problem is admitting that we have one and right now JavaFX is a problem that the  
Java community tries hard to avoid. Swing was pretty stable and while it had its share of issues/pitfalls  
it had its own reasonable traction. JavaFX still hasn’t attained that status but  
just in case you aren’t on the same page as I am we’ll review the evidence in the next section. 

This isn’t an easy post to write and I’m sure it isn’t easy to read, but its a discussion that just isn’t happening  
in the Java community and should happen. New developers coming into Java every day are introduced  
to JavaFX without truly understanding its problems.  
The thing that made me write this post was this  
[blog post](https://dlemmermann.wordpress.com/2015/11/16/when-to-use-javafx-instead-of-html/#comment-516)  
that was mirrored by  
[Java Code Geeks here](http://www.javacodegeeks.com/2015/11/when-to-use-javafx-instead-of-html-3.html).  
I would argue that while the post is truthful (in a very subjective way) it also enforces a false impression  
of the status and perception of JavaFX. This is very troubling when we try to convince young students to  
pick up Java, we don’t want them to be disillusioned later on.  
The problems with JavaFX can’t be addressed if we don’t accept they exist.  
The current users of Java FX consist of 3 archetypes:  
corporations with huge Swing investment, students & hardcore diehard fans.  
While all of the above are totally fine, you can’t grow a community based on that. The corporations aren’t building  
new things, the students will graduate to do something else and the hardcore fans…  
They might be left with nothing as the platform declines. 

I’ll cover the “why should we care” down this post but first lest talk about the proof for the hardcore fans. 

### Proof JavaFX Doesn’t Have Traction

**Exhibit A: Oracle Doesn’t Use JavaFX**  
I can go on about this but the facts are pretty clear. Even Swing based Oracle products aren’t moving in the  
direction of JavaFX. I can go into the firing of the evangelists and some of the teams within Oracle working  
on JavaFX but that seems redundant.  
I would like to point out though that Oracle no longer distributes Scene Builder, yes I know its still available elsewhere  
but if you are looking for signs of what Oracle is thinking… The messaging is pretty loud and clear. 

**Exhibit B: JavaFX Hasn’t Gained The Traction Of Swing**  
Stack overflow was launched on “September 15th, 2008”, this is important since JavaFX was launched released  
on “December 4th, 2008”. Effectively StackOverflow was brand new when FX came out with all of its PR glory  
and Swing should have been on the decline. There was very little time where StackOverflow existed and JavaFX  
didn’t exist. 

The above essentially means Swing should have far less question tags on StackOverflow compared to FX,  
surprisingly the numbers here are pretty staggering and decisive…  
There are 11,565 questions tagged JavaFX, this makes sense for a 7 year old highly visible and widely  
promoted project. However, Swing which should have declined during this time has 56,434 questions which  
indicates to me that even the Swing developers who are the CORE demographic of developers for FX  
haven’t migrated. 

Now to be fair, JavaFX transitioned between JavaFX script to the Java based JavaFX. But that should have  
caused far more questions and confusion within the community. The “reboot” so to speak generated attention  
all over the place and should have mapped to usage numbers.  
This is really punctuated by this illuminating graph from Google trends: 

![javafx swing google trends](/blog/should-oracle-spring-clean-javafx/javafx-vs-swing-trends.png)

Notice that Swing (which had some traction) declined, JavaFX remained low and effectively competes for attention  
against Swing rather than growing. This chart might be read as “desktops lost interest to mobile and web”,  
which would be true and acceptable as an answer (see my discussion below) but the fact that FX can’t even  
beat Swing indicates a far larger problem at play.  
But lets compare it to another company in a similar situation that had a desktop oriented tool that was popular  
and got swept by web/mobile: 

![javafx swing flash google trends](/blog/should-oracle-spring-clean-javafx/javafx-vs-swing-vs-flash-trends.png)

As you can see, the much maligned Adobe Flash is more relevant than Swing/FX by orders of magnitude  
according to the (unscientific) Google trends. 

**Exhibit C: Dice.com Agrees**  
While I don’t think you should pick a technology because of the job market, it is an indication of the state of the  
market. Searching thru dice.com for JavaFX netted me 28 positions of which only 4 placed Java FX as a requirement  
to the job (I checked one by one which is possible when you only have 28!).  
“Java FX” only listed 12 options. But this is where it gets interesting… Swing had 198 positions!  
JavaEE had 16,752 positions and Android had 2,333 positions. 

To be fair there was a job as a NASA contractor that did look pretty sweet in the Java FX search but I think that  
**combining all of the above conclusively demonstrates that JavaFX lacks traction**. 

### Why Should We Care?

If you are a fan of JavaFX then this is a no-brainer. Skip ahead.  
But the rest of us should care deeply since desktop programming is important to the health of the Java ecosystem  
as a whole. One of the big benefits of Java was the skill transferal/tool portability between mobile,  
desktop and backend. The ability we had as developers to move between the datacenter and the front office  
was unparalleled in our industry!  
Java is now challenged on all fronts: NodeJS/Ruby etc. on the server side, iOS on mobile & HTML+JavaScript  
on both mobile and desktop. If the client team doesn’t write the app in Java then why use Java on the server?  
Won’t it be more convenient if the client team and server team speak the same language? 

Yes mobile plays a big role here and JavaFX (or desktop) wouldn’t take over from the web.  
However, in the enterprise Swing dominated well after the rise of the web and JavaFX was able to lose that  
advantage. Losing that ground might cost Oracle the very lucrative JavaEE market and it might cost us  
in skill decline as our specific set of skills experience less demand (yes we’d still make money just like the  
COBOL guys do, it just won’t be as much fun maintaining a legacy system over cutting edge stuff). 

We still need a desktop development API to build our IDE’s, our control consoles and do practically everything  
on our machine. Desktop development API’s are also tremendous teaching aids, getting a UI up and running  
is far more conductive to the teaching process than getting some webservice deployed.  
If you want a future generation of Java developers we need a decent UI option. Some of you  
JavaEE developers out there (or play framework fans) might jump on the HTML bandwagon for teaching…  
I think that’s a better solution than teaching Java FX but effectively its still harder than desktop programming  
and you are then in direct competition with JavaScript tools which have a “home court advantage” as  
students would probably rather learn 2 languages instead of 3 (HTML+JavaScript only).  
Todays students sometimes learn JavaFX or Swing in class and often find out that they learned yesterdays  
technologies as they leave the classroom!  
Even if you never intend to write such a UI the ability to do so in Java is crucial to all Java developers! 

### What Are The Options?

Hopefully you reached this point agreeing (at least partially) that there is a problem. I think one of the problems  
is unclear messaging from Oracle about its commitment (or lack thereof) to JavaFX. Their representatives  
say unofficially that Oracle never discontinues products. That’s pretty accurate.  
However, Swing has been pretty much abandoned and it feels that way. 

**Fix & Promote JavaFX**  
Only Oracle can do this. While Java is bigger than Oracle and would continue even if Oracle stops all activity  
the same cannot be said for JavaFX. The community has made some efforts for quite a while but something  
as ambitious as JavaFX requires serious backing and if Oracle can’t get behind JavaFX 100% then it will  
keep declining and drag Java down with it. 

**Acknowledge That JavaFX Will Never Pickup**  
This is what I’m advocating. JavaFX is here to stay in the same way that AWT was, but once we accept that  
its never going to amount to more than its current limited scope this opens up the possibilities for client  
side development in Java. It also means we should start focusing on new things and maybe something  
can emerge as a replacement.  
I think that the most important thing to do here is to move students off of JavaFX and into more sustainable  
areas in Java such as the newer server/HTML frameworks or to mobile, this will still provide some of the  
pleasurable “tingle” of seeing your UI run but would provide a more sustainable skill set.  
I’ve spent several days trying to come up with a potential replacement to JavaFX on the desktop and  
unfortunately there are no serious contenders at this point in time. Maybe one of the contenders I listed below  
will rise to the task: 

  * SWT – SWT hasn’t matured well. When it was designed modeling it to the Win32 API seemed like the  
right thing to do but with the move to mobile and Macs its now a problematic contender. It is mature though  
and well understood.
  * Swing – going back to Swing is probably not an option, too much time has passed. Because its integrated  
with the JDK anything needs to go into the JVM and thru Oracle.
  * QT – I used to really like QT back in my C++ days. It since added some things but ever since the Nokia  
purchase it was mostly stuck in place. Adding to that the fact that most of the code base is in C++ makes  
this a non-started for most Java developers.
  * Native – this is actually something we are considering for Codename Ones desktop port. Just calling  
the OS native API’s directly using a native to Java mapping API. For Codename One this is pretty simple since  
we can use Open GL and very few peers but I don’t think this will be useful for Java developers as a whole.
  * HTML5 – I think that JavaScript has a huge advantage when it comes to HTML. If HTML or browsers  
are the dominant players then why use Java? JavaScript already has the traction & toolkits in the  
HTML world and Java seems alien there.
  * DukeScript/TeaVM/GWT – I really like all of these and the ability to integrate with HTML is powerful,  
but I think focusing everything on these tools could ultimately relegate Java to a coffeescript substitute  
which seems like a demotion.
  * Android – like Codename One Android wasn’t designed for the desktop. But unlike us its being adapted  
to the desktop (replacing Chrome OS according to rumors). Its a huge, complex and pretty complete API  
missing a few key features but its still pretty powerful. The only problem is that this would require quite a bit of  
effort both in the porting effort and in adding desktop “concepts” to the API (Windows etc.) which was very  
much mapped to mobile.

### How did we get here?

This section is probably redundant for most readers but after writing everything above it occurred to me that  
a fresh Java developer reading my huge rant will have very little historical context. Luckily I can tell recite  
the history rather easily as I had a front row seat working at Sun Microsystem during the peek of Java FX  
and at Oracle as it failed to materialize. 

Java launched with AWT which was a pretty problematic “rushed to market” GUI API.  
  
Sun wanted to improve AWT and replace it with Swing unfortunately at that time Netscape (the leading browser  
vendor by a good margin) had standardized on Java 1.1 and Microsoft was also stuck there. 

So Swing was developed with compromises designed for it to work within the browsers who were the main  
users of Java at that time. This bit of history is important since it punctuates the problems in FX perfectly.  
10 years or so ago Chris Oliver (a Sun engineer) introduced a rather cool scripting language he wrote and it  
gained some traction within Sun. At that time Swing was popular with the enterprise but slowly losing ground  
to Flash in the consumer market. 

The managers at Sun decided to promote the idea and placed a lot of effort, resources into this new language  
which was eventually christened as JavaFX Script. A lot of developer resources were removed from Swing  
and placed into the JavaFX script project and a lot of promises were made to developers. I actually helped  
some of the related projects such as the mobile phone ports etc. 

There were many problems with JavaFX Script and they were further compounded by Sun’s troubles and notoriously  
loose management style. Swing was declining rapidly as Oracle purchased Sun. Oracle killed JavaFX Script but  
liked a lot of the API ideas behind it so they re-targeted the JavaFX effort as a Java based API. Generally a  
good idea, but in a typical corporate fashion everyone who used JavaFX Script had to port their apps at once  
to the new JavaFX or be stuck without the ability to download the VM (they later reversed that decision but  
its not the best way to treat early adapters). 

The new JavaFX API took years to materialize and for quite a while wasn’t even open or properly integrated in the  
JDK. Its integration is partial to this day and its still not a part of Open JDK (which is important on Linux). 

When the JavaFX team was assembled and grew they made an important decision that came back to  
haunt them: Don’t repeat the mistakes of Swing/AWT – build a clean API unburdened by legacy.  
Unfortunately, being a product of a major corporation in the developed world they needed to support  
a lot of things (e.g. accessability) and so a huge amount of code needed to be written from scratch. 

So the team created a well designed API but there was no decent migration path to Swing developers  
and to some degree the path from Swing problematic to this day (despite many improvements). The API is  
huge but still incomplete at some parts because the required breadth for such an API. In the meantime Swing  
developers who got no real updates for years mostly evaporated to other platforms and now we have Swing  
and FX one of which is outdated and the other brand spanking new but has no real traction. 

I think the biggest lesson from JavaFX is to always “think small” and release often. Trying to build a complete  
solution from day one rarely works even if you have the full set of resources that Sun/Oracle were able  
to wield. I would argue that all the problems in JavaFX are a result of mismanagement. 

### Final Word

One of the things I hated the most about Google under Larry Page was the Spring cleaning, since Android  
Google has failed to create anything that enjoyed that level of traction. That was not due to lack of trying,  
it was due to lack of commitment to anything. Most people don’t remember this but Android was a failure  
when it was initially released (G1) and the iPhone had a very small niche following (relatively to the mobile market  
as a whole).  
Both companies stayed the course and invested in the product/partnerships while slowly iterating on the product.  
This took money, time and commitment which is pretty hard to do. 

Unfortunately, looking at the current state of JavaFX and Oracles backing of it. Its pretty obvious that it was  
already moved to maintenence mode and won’t get the resource commitment it needs to grow. I think we  
might be better off moving it aside and allowing other technologies to rise to prominence. Even if you disagree  
with that opinion I hope we can all agree that there is a serious problem here. To me the problem is mostly  
with students picking up JavaFX either thru universities or online courses. We might as well teach them  
COBOL, there are jobs writing COBOL too. 

With the current state of JavaFX and the lack of any contender to occupy its space (which is currently not officially vacant)  
I get a sense that we might be better off with nothing. At least then we’d have a big “vacancy” sign in our  
virtual front yard where our desktop API should reside. This will let one of the options I listed above (or something  
brand new) occupy that spot…   
  
Maybe it will trigger someone at Oracle to finally give JavaFX the resources needed to turn it into a viable  
tool but knowing Oracle… I’m not holding my breath.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Andrea Liana** — November 19, 2015 at 11:18 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22561))

> Andrea Liana says:
>
> Hallo, I liked your post because I share many of your worries about Java future. I had great expectations from Java One 2015, but except for the module feature, I wanted to hear too a clear and resounding message from Oracle about desktop commitment. And that did not came.  
> I have a large codebase running on a custom fork of a very old AWT based framework and at the moment I am working on a little pure JavaFX framework because I expect soon or later to read an Oracle message like “JDK 1.xx will no more support AWT”.  
> What I still don’t understand on Oracle policy, like:  
> – letting RoboVM being acquired by Xamarin instead of buying it and use its codebase for the recent accounce of a project about JVM for iOS / Android;  
> – delegating SceneBuilder to GluOn instead of keeping it as an official Java branded tool;  
> – why is it so hard to extends JavaFX components considering all the customization required by current AWT/Swing project to meet our customer needs.
>
> I chose Java becuase I trusted that platform with the “Write once, run everywhere” key feature. I just only hope not to be forced to abandon it because of Oracle foggy decisions…
>



### **Shai Almog** — November 19, 2015 at 11:43 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22569))

> Shai Almog says:
>
> As a former Sun guy I can answer this pretty clearly.  
> If Oracle would have bought RoboVM then someone was clearly asleep at the wheel which is rare for Oracle who are normally very calculating. RoboVM relies on 3rd party projects with mixed licensing some of which potentially conflict with Oracles goals. For Oracle lawyers just evaluating RoboVM would be an IP nightmare.
>
> Furthermore, RoboVM doesn’t make anything that Sun/Oracle needs. We had VM’s running on iOS back before Oracle acquired Sun. We even demoed them and we had really brilliant compiler engineers some of which are still at Oracle. RoboVM’s traction is noticeably lower than Codename One and we both don’t have a fraction of the marketing muscle Oracle has. So that’s not a reason for an acquisition either.
>
> JavaFX has inherent technical problems it will never work on iOS properly and neither will the full JVM because mobile is pretty different to desktop. I didn’t want to get into the technical aspects there and didn’t want to get too deep into mobile as this might have shifted the discussion into me trying to promote Codename One.
>
> Oracle didn’t delegate anything to Gluon, they just abandoned scene builder and the Gluon folks couldn’t take a hint.
>
> As a guy who built VM’s and done mobile since the 90’s Gluon really gets under my skin. They are trying to sell something that isn’t technically viable as if its a real world tool that can actually work in production. There are basic things both in Java FX and Java’s network stack that can’t be implemented correctly/efficiently in iOS e.g. [java.net](<http://java.net>).* (posix sockets don’t turn on radio), sub pixel anti-aliasing etc.
>



### **Andrea Liana** — November 19, 2015 at 12:02 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22317))

> Andrea Liana says:
>
> Thank you four your prompt reply and clarifications.  
> One more question… am I wasting my time and efforts on JavaFX for desktop applications?
>



### **Shai Almog** — November 19, 2015 at 12:07 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22325))

> Shai Almog says:
>
> Hard to tell.  
> I don’t share your immediate concern about AWT. It wasn’t even officially deprecated so I can’t really see it going away “soon”. Since Java release cycles are around 18-24 months assuming Java 9 deprecates AWT you would still be clear for another 2 years after that…
>
> JavaFX is way better than Swing and AWT as you probably noticed. Its more modern and easier to use.
>
> It has its faults but if you need a desktop API at this point in time and like Java then its a reasonable enough choice. As you saw from the article above, we are in the same boat. Codename One currently uses JavaFX & Swing for our desktop builds and for our mobile device simulators.
>



### **Andrea Liana** — November 19, 2015 at 12:53 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22479))

> Andrea Liana says:
>
> Considering that Java 9 is more concerned about solving the package hell, I hope Java 10 will be more focused on giving us developers a clear and convinced path for forging our user interfaces. Desktop or mobile….
>



### **Michael Stover** — November 19, 2015 at 6:22 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-24163))

> Michael Stover says:
>
> The list of alternative options seems to include only one real choice: SWT.
>
> And I say that having despised the creation of SWT for my entire career.
>



### **bryan** — November 19, 2015 at 11:44 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22347))

> bryan says:
>
> I’m in the same position re developing desktop app in FX.
>
> I’m not quite as pessimistic as Shai. As far as I know Oracle will continue to ship FX as part of the standard JRE runtime. The OpenJDK mailing list seems to reasonably active on the FX front. IMO, FX with SceneBuilder is probably the best GUI development environment I’ve used – CN1 is close 🙂
>
> I was a big fan of GWT and the app I’m currently developing with FX was going to be GWT. Unfortunately, most of the stuff that (to me) makes GWT a value proposition is going to be deprecated in the 3.0 release.
>
> It will be interesting to see what the merging of ChromeOS/Android brings. I think some of benefits of web apps (i.e. zero footprint deployment) are disappearing with the App Store model of deployment, and given that it’s impossible to be sure any Javascript framework will be around next week let alone in 5 years.
>



### **critic** — November 20, 2015 at 7:19 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22332))

> critic says:
>
> JavaFX API was made as heavy as Swing API was. It looks like object oriented concepts were intentionally avoided. Maybe it’s a reason why JavaFX is pretty fast. If one needs fast dynamic imaging, forget about Swing, take side of JavaFX. Not sure? Compare CPU percentage in Task Manager. But what could break any business is a tolerance to failures. When something fails in Swing, one will likely recover the application. When such accident happens with JavaFX, the best choice would better kill the application. Sounds bad for business.
>



### **Jeff Martin** — November 23, 2015 at 4:48 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22289))

> Jeff Martin says:
>
> Interesting post – I would be curious to hear your suggestions for specific improvements to fix JavaFX (or what a replacement would look like).
>



### **Shai Almog** — November 23, 2015 at 8:03 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-21480))

> Shai Almog says:
>
> A replacement IMHO should be *simple* and not over ambitious as JavaFX.
>
> While you can outline a lot of mistakes made on the managerial, strategic levels with JavaFX the biggest technical mistakes I personally perceived are:
>
> a. Not providing easy interaction with Swing from the start (throwing away existing traction).  
> b. Building something huge that’s very hard to move and adapt
>
> The project is generally over ambitious and tries to be “all that”. Its admirable but really hard to get a battle-cruiser of this size into production quality. Then it takes a huge amount of maintenance. This is astounding to me as the original proof of concept for JavaFX Script was the work of one guy…
>



### **Jeff Martin** — November 23, 2015 at 8:52 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22181))

> Jeff Martin says:
>
> Very good points – particularly in light of recent staff reductions.
>



### **Pablo Rodriguez Pina** — November 25, 2015 at 1:27 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22513))

> Pablo Rodriguez Pina says:
>
> Hey Shai,
>
> Just a opinion from a hands on JavaFX software company. We have been building all our enterprise apps with a JavaFX frontend and a JavaEE backend, all traffic between server and client in pure java usign either native java serialization or java serialization frameworks such as hessian or kryo.
>
> From our experience, JavaFX is working great, there may not be many stackoverflow questions, or many developers in the world using JavaFX or the evangelists may have gone to hell, but it is just working great for us. The JavaFX support team as Oracle has been pretty good in looking at issues and communicating and we find building frontends in JavaFX far more practical than web based interfaces. We can use JMS easily to receive push notifications on the client, and it just feels way more relaxing using strongly typed Java 8 and moreover Java EE (CDI, JMS, etc) on the client with all its glory.
>
> The core JavaFX controls provide us pretty much all the functionality we need, we have had to resort to controlsfx or jfxtras for a coupl of things, but overall, the core javafx controls do the job.
>
> That’s all, happy to be the rare case in the javafx community, but we are as happy as chips with it.
>
> In 2000, during the dot com boom, everyhting had to be web based, it was either web based or good for nothing. Similarly in recent times, we have seen an insane Objective-C universe expansion and contraction that took Objective-C from nothing to rank #3 on the tiobe index and it has now disappeared from the top 10.
>
> Having done swing earlier and still mantaining legacy swing applications, I still don’t see how, regardless of how much growth energy Oracle puts into JavaFX, any other alternatives are going to be in the best interest of our clients over the next 10 years.
>
> If we are talking about traction, yes, javascript has traction even thought the fricking programming language hasn’t moved fro 20 years and even Google has stated that the limitations of the language make it almost impossible to build large applications and there is a universe of javascript producing frameworks and languages like Dart, GWT, vaadin, JSF, etc. But that traction the javascript language has is not so easy to find in the actual frameworks software vendors need to choose to develop an application UI.
>
> With all the glory of javascript and html, still one needs to choose JSF 1 or 2 and a JSF component library or a javascript framework and these js frameworks seem to appear and disappear in a shorter amount of time and don’t make it so easy for the developer to upgrade to the following version. I wrote a JSF about 8 years ago app using the Sun woodstock JSF library, got dephased before I finished, what do I migrate it to to keep it up to date without a massive UI rewrite ? then did another one on IceFaces 1.8, want to migrate it to IceFaces 3.0? not even that was straight forward.
>
> We have migrated apps from JavaFX 2 to JavaFX 8 with minor complications, and it was only a bit of an effort migrating the first application, once the regressions were located, the other apps took no time to migrate.
>
> In our case, as we mainly build enterprise apps, there is no much need for running apps on mobile phones. If we need to run on tablets, We can get decent windows tablets that can run JavaFX at decent performance (I’d probably prefer linux tablets but windows seems to have monopolized the intel bay trail tablet market as it has done with laptops). Hang on, we do have a JavaFX app running on a linux based touch device.
>
> We have built about half a docen enterprise apps using JavaFX as a front end and haven’t experienced hughe maintenance caused by JavaFX. In fact, we have probably only bumped into a handfull of bugs that got resolved by the JavaFX team in a mater of a jre update.
>
> We gave up on scenebuilder a long time ago, mainly due to its blindness of the maven classpath, but one way or another, I didn’t like the generated FXML code, today, we are coding the FXML and we have a shortcut in our apps that reloads the FXML by doing Ctrl+F so we can reload changes instantly and are able to test the UI with fully working controllers as the FXML reloads (takes 2 seconds). So personally, I don’t miss SceneBuilder at all.
>
> I have also felt the JavaFX team has at times trying to set a too wider scope, specially when it comes to hardware acceleration, mobility, accessibility, but they are doing a good job overall hey. And it does seem they have energy to look into mobile device builds.
>
> Sun supported swing, had traction and did a good job, we all loved Sun, as a whole, they always seems positive, creative, good hearted people. Went bankrupt though.
>
> Oracle picked up Java, they have been doing more good to the JDK than Sun had in the 5 years prior to the merge, yes, we know what they are like with dollars, licenses and courts, but who is financially contributing to the JavaFX growth?, I recently looked into the SE support options and I am still trying to gather the 10K USD for commercial JavaSE support.
>
> From my point of view, I think we have gone way past the turnaround point of ejecting from JavaFX into a new UI, Oracle supported, JDK bundled UI toolkit. If you feel JavaFX doesn’t have traction, I don’t see Oracle even engaging on a ‘let’s start all over again’ java se desktop framework conversation at a cofee break.
>
> I am sure it could have been done better, like anything on this world, but I’d say that as a community, we are better off contributing and supporting to what is there today and taking it all the way to mobile builds than aiming at a new UI toolkit. After all, if we are looking at traction levels, how would a new UI toolkit from scratch would get the traction we are talking about?
>
> Talking of traction, I still don’t get my head around the node.js thing and having a 20 year old programming language for backend processing. I do see that there are more js / html developers (or maybe more the graphic designer profile) that think ok, hang on, I’ve learned javascript as part of my web designing career, now i can do backend logic myself with my javascript skills, fantastic, let’s go node.js but that is not me. I am happy with my JPA, JMS, EJBs.
>
> Apple came up with this iPad tablet that had a super cool screen and an 8 hour battery life and everyone went insane with their iphone apps when the desktop environment was just deemed as out of fashion. I don’t think they planned for Objective-C’s traction. They may have planned for the iPad sales, but I doubt at the time of engineering the iPad people at apple could have predicted the boom in iOS apps that was about to happen.
>
> When it comes to ‘engineering’ the process of a framework’s traction, we can either approach it the software engineering way (maintainability, escalability, backwards compatibility, etc) or we can go left brain and do the rain dance to see if the way it implemented converts plumbers into develoeprs.
>
> Personally, I always prefer to stick to core Java / Java EE frameworks than going too much out of the box into things like spring, or plain hibernate or propietary gui toolkits, the main reason for my view is based on long term maintenance and upgrade. Yes, the JSRs are always running a few years behind the frameworks with the actual creative ideas, but I just find it more cost effective in the short and long term. From training materials for develoeprs, to java certifications, to release notes, upgrade manuals, etc. Loosing a bit on those edgie latest features pans out to me.
>
> Same thing with the Jvm languages, how many jvm have come up in the last 7 years: redhat has one, scala, jruby, and even IntelliJ knocked one up, what are they going to do abou the traction of their -in deed more pracical than java- cool jvm language?
>
> Swing got tracking because Java was the word on the street then, at the time, everyone looked into JSRs and the Java community process, the community was more focused into the core releases (java, java ee, jsrs, jcp), today are way more scattered. May be be bacause java lost momentum in the last years of Sun , maybe because of the JDK license, may be because Android did what they did. But if we do want to concentrate our energies into a more focalised point, the most democractic option is still the JCP. With all the Apache piss off and with all the who trusts oracle, it is still the JCP. Except for Google and Apache, most of the big players are still there. The problem may be us, java community members that we have grown an ego thing against the JCP. Look at any other platform outhere, go .NET, that’s it, microsoft, not much open source. Go Apple, go the most propietary platform in the world. Go google, go Google Ads and boicot anything other than a web browser or Android.
>
> If the entire java community started gravitating back to the JCP as it happened in the begining, we would be looking at lots of new vertical JSRs emerged from community driven proposals, a well organised, regulated, participative, collaborative community driven effort more estable, innovative, maintainable platform which would allow us to produce more with less effort while keeping the java programming language at it’s all time high.
>
> About the sacking of the evangelists, you probably know better Shai, but one thought that it came to my mind, is that java does seem to be picking up (i look at tiobe, not perfect, i know, but i look at it) and does have a self selling road map with modularity ahead of us. Not so sure how much evangelists were earning vs producing. But I think the features and evolution of java itself is the best form of evangelization. But yes, you probably know better what is happening over there.
>



### **Shai Almog** — November 25, 2015 at 3:25 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22256))

> Shai Almog says:
>
> Wow that’s practically a guest blog post 😉
>
> I get what you are saying entirely and I don’t think its in conflict with what I’m saying.
>



### **RuRu** — November 25, 2015 at 9:58 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22268))

> RuRu says:
>
> In the defence of students and recent graduates, not all are attracted by JavaScript and Html. My self personally, I despise the idea of using something like JS (Node.js) in the backend and in general. I will use it for the front end in the website if my job requires to do so (Part time student software developer), but never in the backend, I would quit my job.  
> That’s for trusted languages that are designed to be used for the backend, such as Java. I am personally a Java fan boy.
>
> As for JavaFX, it is a shame I never had a chance to really get into it, when I started programming I really wanted to be a Desktop developer, and it does sadden me the fact that everything is going web and cloud based. I only dealt with .NET such as WinForm for desktops. But even WinForms are decreasing in popularity and use.
>
> I hope Oracle makes fixes and supports JavaFX and desktop development a bit more.
>



### **RuRu** — November 25, 2015 at 10:03 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22388))

> RuRu says:
>
> That would be great. Since Java is making a native Java MVC framework, for the JAVA EE (of course for the web). It could also transition to desktop with an API that allows good integration with object oriented features and great looking ui components. At least something I hope for
>



### **Sid** — November 27, 2015 at 4:20 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22474))

> Sid says:
>
> Why does the Java community care so much about corporate backing? So what if Oracle has no interest in desktop Java. JavaFX is obviously better than Swing and open source too. Why can’t the community invest its efforts into making it a better product. Look at the Linux world. I don’t know of any billion dollar company that invests and promotes GNU utilities, KDE, GNOME, hundreds of distros and yet every such project has an active community around it no matter how small.
>



### **Shai Almog** — November 27, 2015 at 4:51 pm ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22416))

> Shai Almog says:
>
> That’s a great example of why corporate backing is essential. The Linux kernel is one of the rare true large scale open source projects that thrived before corporate backing. However, today saying that it doesn’t have corporate backing ignores the salaries top hackers get from major corporations. If you doubt the importance of that then look at BSD, Gnu Hurd etc.  
> Pretty much all of the major distros are backed by pretty big corporations. There are unaffiliated distros but they are pretty small by comparison.
>
> But Gnome/KDE are far better examples… KDE didn’t have corporate backing and was winning.  
> RedHat and most corporate Linux vendors standardized on Gnome and invested many millions into it. At Sun we specifically invested a huge amount of effort on many “unsexy” endeavors such as accessibility etc.
>
> This was all part of a proxy war against Microsoft that the open source community benefited from.
>
> We’re an open source company and I’ve been running open source projects for many years so I’m a big fan of open source. But the “oh open source it and let the community take over” is one of the things that crashed Sun.
>



### **Pablo Rodriguez Pina** — November 30, 2015 at 7:49 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22489))

> Pablo Rodriguez Pina says:
>
> 🙂
>



### **Pablo Rodriguez Pina** — November 30, 2015 at 7:53 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22307))

> Pablo Rodriguez Pina says:
>
> Just to provide grounded evidence to Shai’s statement on open source crash, here is some archive video [https://www.youtube.com/wat…](<https://www.youtube.com/watch?v=5r3JSciJf5M>)
>



### **First Last** — March 27, 2016 at 11:02 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22719))

> First Last says:
>
> Hahaha, the Flash to JavaFX/Swing interest chart is so dumb. Sorry but you are comparing widely known technology which is used as a runtime (and Googled for in that purpose) to a user-invisible technologies, only developers know. Hillariuos.
>



### **Shai Almog** — March 27, 2016 at 11:11 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22491))

> Shai Almog says:
>
> Fixating anonymously on one small pet peeve in an article that contains many solid arguments is what? Smart?
>
> Java was branded to users just like Flash. FX was branded as a user oriented technology for years with visual tools to compete with Adobe so no. Hell, I’ll even go further: [https://www.google.com/tren…](<https://www.google.com/trends/explore#q=java%2Cflash>) how dumb is that?
>
> Or maybe that: [https://www.google.com/tren…]([https://www.google.com/trends/explore#q=java%2C%20flash%2C%20Android&cmpt=q&tz=Etc%2FGMT-3](https://www.google.com/trends/explore#q=java%2C%20flash%2C%20Android&cmpt=q&tz=Etc%2FGMT-3))
>
> I suggest working on your research skills and maybe making statements you wouldn’t be ashamed to make with your actual name.
>



### **Shai Almog** — September 14, 2016 at 5:19 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-23072))

> Shai Almog says:
>
> Flash performed very badly on Android devices as well one of its core problems was that it expected very specific behaviors from the low level drawing primitives. This can be mitigated in desktop GPU’s but not so much on mobile GPU’s which are both flaky and varied.
>
> FX has issues with desktop GPU’s and I doubt it has resources to deal with the huge complexity that is mobile. It is over ambitious. Being over ambitious is a problem as it creates an “all or nothing” situation. HTML/JS/CSS shares that over ambitious nature and has some failings because of that.
>



### **Shai Almog** — September 15, 2016 at 4:15 am ([permalink](https://www.codenameone.com/blog/should-oracle-spring-clean-javafx.html#comment-22938))

> Shai Almog says:
>
> The goal of the post was to talk about the problems in FX not HTML. The fact that I don’t like HTML is a given…
>
> I detailed a lot of the problems in Gluon in a comment a while back: [http://www.codenameone.com/…](<http://www.codenameone.com/blog/comparing-qt-and-codename-one.html>)  
> This created an unpleasant exchange with a “colorfull” person who then proceeded to harass me personally going so far as to create fake accounts on dzone with word plays on my name and post insulting comments to every social network post we make. He is the first and only person we ever blocked from commenting…
>
> The problems I detailed with Gluon just got far worse since I wrote what I did, they are now building their own OpenJDK AoT compiler which is insane. Besides the amount of work/maintenance which is already impractical, JavaFX depends on a huge number of classes and including them will produce binaries unfit for mobile distribution as iOS requires 2-3 platforms in a fat binary…
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
