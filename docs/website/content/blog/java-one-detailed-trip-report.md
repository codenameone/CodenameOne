---
title: Java One Detailed Trip Report
slug: java-one-detailed-trip-report
url: /blog/java-one-detailed-trip-report/
original_url: https://www.codenameone.com/blog/java-one-detailed-trip-report.html
aliases:
- /blog/java-one-detailed-trip-report.html
date: '2013-09-28'
author: Shai Almog
---

![Header Image](/blog/java-one-detailed-trip-report/java-one-detailed-trip-report-1.png)

  
  
  
[  
![Uh... I'm standing on stage next to James Gosling during rehearsal. Achievement unlocked.](/blog/java-one-detailed-trip-report/java-one-detailed-trip-report-1.png)  
](https://twitter.com/SeanMiPhillips)  
  
  

There is absolutely no way I can recall everything that went on in JavaOne but I will try to do my best. I arrived sick and on pretty strong medication for my sinuses so my recollection might be a bit hazy, I did meet great people and had loads of fun. 

Before JavaOne started I had a meeting with  
[  
Arno  
](http://www.puder.org/)  
of the  
[  
XMLVM  
](http://xmlvm.org/)  
project, I was hoping to get more facetime with him and to maybe actually code something together but he was on his way for the rest of the week due to other duties. We discussed some of the issues with XMLVM and the potential of forking vs. merging our changes with the main XMLVM code base. The way things look it seems that we will have to take over XMLVM or at least the iOS/Windows Phone ports.  
  
  
Later in the conference I had a meeting with  
  
[  
Niklas Therning  
](https://twitter.com/ntherning)  
of  
[  
RoboVM  
](http://robovm.org/)  
, who like Arno is a great and interesting fellow.  
  
  
I’ve been giving a lot of thoughts to XMLVM and RoboVM over the past few months, both have done a lot of work and each has its advantages/disadvantages. I’ve asked  
[  
Steve Hannah  
](https://twitter.com/shannah78)  
to run his benchmarks for me on devices with some settings I asked for. Initially it seemed that RoboVM smoked XMLVM but when running on the devices the differences became far less pronounced.  
  
  
  
Because of the way Codename One is built, we could potentially just flip a switch to our servers and builds will be performed with RoboVM instead of XMLVM (this would require a lot of work on our side but would be seamless to you). The main holdback for me is my conservative nature. XMLVM is a very safe choice:  
  

  1.   
We can debug it (although its inconvenient).  

  2.   
We can maintain it, I’m sure we can maintain RoboVM too if Niklas decides to quit but we already know we can maintain XMLVM.  

  3.   
If Apple changes something (e.g. new architecture like the 64 bit they just announced) we can support it instantly since we just use xcode.  

So currently we will stick with XMLVM but there are no final decisions here since we could potentially offer both backends as we move forward. 

  
  
  
  
  
JavaOne started with a great  
[  
NetBeans party  
](https://blogs.oracle.com/geertjan/entry/youtube_netbeans_party_2013)  
on Saturday night, I met a lot of great people at that party doing some interesting stuff (although its all hazy with the medication and the beer). I met  
[  
Joonas Lehtinen  
](https://twitter.com/joonaslehtinen)  
at that party he is the founder and CEO of  
[  
vaadin  
](http://www.vaadin.com)  
(who now have a great  
[  
new NetBeans plugin  
](https://blogs.oracle.com/geertjan/entry/official_netbeans_tools_for_vaadin)  
), which is a name that I saw popup every now and again. I’m a heavy GWT user and love it but it has its limitations, I guess I didn’t pay enough attention (too busy with mobile) to notice that  
[  
vaadin  
](http://www.vaadin.com)  
is really a hybrid solution that tries to merge the stuff I liked about Echo2 with the stuff I like about GWT. Very cool! One of the great things about their booth is that they literally had a pile of vaadin books as the giveaway, finally a show giveaway that is actually useful!  
  
  
  
Kudos on that one, we should definitely do something like that as well. It also gave me a chance to read something on takeoff and landing (3 connections on the way home) so I was able to finish that pretty hefty book (honestly I skimmed some parts such as installation).  
  
Another cool thing about vaadin is that they are an open source startup that has ±70 employees, really encouraging stuff on a personal level.

On NetBeans day itself I was on my way to Moscone (if you are a long time alumni this alone should provide you the reason to attend NB day, its at Moscone), when I ran into  
[  
Geertjan  
](https://blogs.oracle.com/geertjan/)  
. We both walked to the first session of the day entered the room and there sitting and working bright and early is Dr. James Gosling. Alone.  
  
People reading this blog can probably imagine my feeling at that moment, but I’m an entrepreneur first and human being second… I instantly descended on him like locust pitching Codename One and giving him the birds eye view pitch.  
  
He was great and expressed interest, at the end of the day I asked that he would join our advisory board and I have his email so I can continue begging thru that medium! So that alone was a huge thing for me on a personal and professional level. 

Gosling demoed his cool robots to the crowd, being a mobile/embedded guy (and having worked on robots as a kid) I really appreciate a lot of the stuff they are doing at  
[  
liquid robotics  
](http://liquidr.com/)  
. They have amazing visualization tools for controlling these robots which I’m sure will really benefit from Codename One… I promised him that if he accepts my invitation I will personally port everything to Codename One and give him a great tablet UI to play with the robots. 

Gosling told the story of the NetBeans acquisition, having worked at Sun I can pretty much attest to the mindset behind it. The gist of the story is that Sun at some point decided they want a “big play” on development tools and didn’t have anything good (because they kept killing internal projects so as not to compete with Borland et. al.). So they were looking for a billion dollar acquisition and came across Forte which was making mainframe tools (totally unrelated), Gosling tried to convince management to buy NetBeans but they decided that the price was too low so its not a “big play”. Eventually the compromise was to buy both (he put it as: the price for NetBeans was a rounding error on the Forte deal).  
  
Years later Forte is effectively nothing and NetBeans has 1.4m active users.

There were a lot of other great talks that day but frankly I was all jagged up on Gosling and I don’t remember anything. I vaguely remember my talk on that day too although from feedback I got it was apparently given. I think I also had a sugar rush from all the great cupcakes  
[  
Tinu  
](https://twitter.com/netbeans)  
ordered for the event celebrating 15 years of NetBeans (Google is also 15 years old this week, coincidence???).  
  
  
Nathan Howard wrote a  
[  
great blog post highlighting the NetBeans plugins  
](http://blog.idrsolutions.com/2013/09/6-must-have-ide-plugins-featured-at-javaone/)  
that were presented that day.  

In the middle of the NetBeans day we left for the keynote (in Moscone again which was cool), there were quite a few technical glitches in the keynote which is pretty shocking. I remember working on keynote presentations when at Sun, the level of work and rehearsal is astounding and having tech issues in that setting is really surprising.

The keynote talked about what a great year this was for Java, I understand the need to get people enthusiastic and excited in a keynote (and be positive) but ignoring the security issues and NSA warnings etc. (within the keynote) is a bit much.  
  
The thing is Oracle actually has a security track in JavaOne now and is doing a lot of things to fix the problems we saw this year, I’d much rather hear “we had a tough year on this front but here is how we are fixing it”. The keynote was mostly anemic talking a lot about the embedded effort which is very interesting (the internet of “things”), but there was very little content there that was interesting to me personally as a mobile developer that isn’t a mobile HTML developer or an embedded developer.

The day ended with a BoF about contribution to the JDK narrated by Daniel Sachse and Helio Frota, I came because of the description of the BoF which was pretty in line with Daniel’s presentation. However, it seems the committee that approved the BoF unified two separate and unrelated talks into one causing some frustration in the audience most of whom came for the description which was taken from Daniels talk.  
  
  
  
  
The next day started with a session of notify your mobile clients by  
[  
Jay Balunas  
](https://twitter.com/tech4j)  
and  
[  
Matthias Wessendorf  
](https://twitter.com/mwessendorf)  
of the JBoss team at RedHat, they talked about sending push messages from the server. The talk was good, geeky and coherent. I think they made the problem seem a bit easier than it really is in the real world, they have an open source project called  
[  
AeroGear  
](http://www.aerogear.org/)  
which I will be sure to check out the next time I look at our push code.

Next on  
[  
Richard  
](https://twitter.com/richardbair)  
and  
[  
Jasper  
](https://twitter.com/jasperpotts)  
gave their annual what’s new in JavaFX talk which was pretty interesting. To me the biggest announcement by far: one thread!  
  
Up until now if you wanted to touch JavaFX from Swing you had to mix the threads since both have their own EDT. This is pretty similar to Codename One only imagine having 2 EDT’s and keeping track of both while you have your own code running. That isn’t horrible for simple use cases but in the case of our webkit and graphics implementation this is something that is REALLY hard to get right. They added an experimental flag that effectively unifies both threads into one thread, this should make migrating code way easier. They announced a lot of other stuff from 3d to embedding Swing in FX (up until now you could only embed in the opposite direction).  
  
I grabbed Richard for a chat, we have known each other online for quite a while mostly thru the work I did for SwingX’s open source project and in my properties work later on (and now thru Codename One). We ended up going to lunch with Richard, Jasper and  
_  
Steve  
_  
Northover. Apparently they now work very closely with Daniel Blaukopf who is a great friend from the Sun Microsystems WTK team days and one of the smartest people I know. We had some lunch and chatted a bit about FX and Codename One. It was a very interesting conversation but there is no point of going into details at this point in time.

I missed a session and arrived at a session on Clojure, I didn’t like Clojure when it was called Lisp and that feeling remains the same. Trying to push a language like Clojure is like trying to get us all to use superior keyboard layout (e.g. Dvorak), probably won’t happen. These things are rooted too deep, but maybe I’m old (strike that, I am old who knows?).

I ran into a friend so I was late to the multi-device session by  
[  
David Campelo  
](https://twitter.com/davidcampelo)  
. I think I met David years ago when working for Sun, Chen and him know each other from our work on GingaJ which effectively put LWUIT on TV screens in Brazil. He gave an interesting demo about connecting tablets to the currently playing content on a TV, that is indeed an interesting direction to explore. I understand that the level of engagement in such experiences is tremendous and this is probably the future for such tools.  
  
Unfortunately his demo didn’t work, I can feel his pain. Its really hard to show network/device stuff especially with proprietary chains involved. There are so many points of failure which is why I always bring a ready made video of the demo in case of such a failure. This works like Murphy’s law, when you take that precaution the demo never fails…

I then went on the  
[  
Matthew  
](https://twitter.com/matthewmccull)  
‘s GIT on NetBeans Hands On Lab. I am not a fan of Hand On Labs and rarely take them (I never liked sitting in classrooms), I went simply because I was so impressed by Matthew following his JavaZone talk and wanted to see more. Matthew is indeed as great a teacher as he is a speaker, he was very interactive and I feel I understand GIT better now. If you happen to see his name at a conference you are attending I highly recommend you check it out, I think its always a good policy to just go to sessions with good speakers. Even if the subject isn’t interesting to you right now you can learn so much.   
  
In the past we had a lot of issues with GIT especially for Chen who is using Windows where the GIT GUI support isn’t as good as the SVN integration. I’m assuming some of this got fixed and it also seems we used GIT somewhat like Teamware (the gradfather of GIT) which I always loved and Chen really hates with a vengeance. Maybe its time to switch back to github, I love their UI and a lot of the things about it. I wonder how easy it would be to migrate a Google code project (probably should have asked Matthew).

  
  
I then rushed to the JCP party on the roof of the Hilton, it had nice food and some interesting people but I couldn’t stay and ran off to a BoF on Swing & JavaFX. On my way to the BoF room I ran into  
[  
Tinuola Awopetu  
](https://oracleus.activeevents.com/2013/connect/speakerDetail.ww?PERSON_ID=5B887663459DE248B1E37D23F1023BA5&tclass=popup)  
who does the marketing for the NetBeans team, we started chatting and it turns out she had her own panel BoF starting just then titled: “So You Want to Be a Published Technical Author?”.  
  
  
A friend from Sun and one of the best technical writers I know  
[  
Jonathan K  
](http://www.jonathanknudsen.com/)  
  
[  
nudsen  
](http://www.jonathanknudsen.com/)  
(who also wrote one of the first LWUIT tutorials) used to describe book authoring as torture. So I don’t want to write a book.  
  
  
However, I had fun talking with Tinu and  
  
thought I might as well check out the BoF. I’m glad that I did.  
  
  
It featured 5 panelists:  
  
  
[  
Erol Staveley  
](https://twitter.com/ErolStaveley)  
– publisher at Packt  
  
[  
Joel Murach  
](http://www.amazon.com/Joel-Murach/e/B001JP7JQI)  
– who is both an author and publisher at Mike Murach & Associates  
  
[  
  
  
Meghan Blanchette  
](https://twitter.com/MeghanORM)  
– editor at O’Reilly Media  
  
  
[  
Arun Gupta  
](https://twitter.com/arungupta)  
– needs no introductions but in this case he came as an  
[  
author  
](http://www.amazon.com/Arun-Gupta/e/B00DWBZ3NI/ref=sr_ntt_srch_lnk_1?qid=1380465054&sr=1-1)  
.  
  
  
[  
  
  
David Heffelfinger  
](https://twitter.com/ensode)  
– an established author of  
[  
several books  
](http://www.ensode.com/books.jsf)  
.  
  
  
  
  
  
This was a very interesting/fun panel, I hadn’t thought about the option of writing a Codename One book before this panel but I am now entertaining the thought.  
  
Just goes to show that you should always be open at Java One.

  
  
The next day started by touring the exhibition floor, I was on a tight schedule running around to very specific booths of interest. Last year I was so busy I didn’t get to see the pavilion even once, so I was very careful and methodological this year. There were many interesting booths from IBM, RedHat, ARM etc. but the startups were more bold. CloudBees literally dressed all their personnel as bees, including head of marketing and (male) CEO. Amusing, but I think I’m too dry to do something like that.

“Practical Pros and Cons of Replacing Swing with JavaFX in Existing Applications” was the title of a surprisingly interesting session. I added it to my list because it was lead by  
[  
Geertjan  
](http://twitter.com/GeertjanW)  
but pretty much everyone on the panel had an interesting story. It focused on developers using the NetBeans platform (platform not just the IDE) for their development who incorporated JavaFX in various ways to reap the benefits of improved UI visualizations. 

[  
Sean Phillips  
](https://twitter.com/SeanMiPhillips)  
presented the app he has been working on for NASA (later on he showed it in the  
[  
community keynote  
](https://blogs.oracle.com/javaone/entry/the_javaone_2013_java_community)  
, I think he got a dukes choice award for it), it is a NetBeans platform/FX based visualization of spacecraft motion in formation. This includes amazing visualizations for NASA scientists, I suggest checking out a small portion of his demo in the community keynote, very compelling. What doesn’t really show in the keynote is how excitable, enthusiastic and passionate he is about this technology.

[  
Timon Veenstra  
](https://twitter.com/TimonVeenstra)  
presented a pretty cool NetBeans platform app that enables  
[  
crop management for farmers  
](http://agrosense.eu)  
interesting stuff. They won a Duke’s choice award. Its pretty cool. 

[  
Rob Terpilowski  
](https://twitter.com/RobTerp)  
presented the work that he has been doing at  
[  
Lynden  
](http://www.lynden.com/)  
which is a major shipping company. They are using FX to make the UI’s of their rather elaborate tables more visually appealing and usable. 

I then went to watch “Play Framework Versus Grails Smackdown” which was delivered by two brilliant speakers:  
[  
James Ward  
](https://twitter.com/_JamesWard)  
&  
[  
Matt Raible  
](https://twitter.com/mraible)  
. The talk was great however they gave a member of the audience in the front seat a really loud bell (or gong) and whenever one of them “delivered a smackdown” he rang the bell really loudly. I was in the back and had a headache 20 minutes into the talk. Left to the speaker room next door where the bell was also heard only softly.  
  
  
  
There are two reasons I went to the BoF “Teaching Java with Minecraft, Greenfoot, and Scratch”,  
[  
Daniel Green  
](https://twitter.com/dangjavageek)  
&  
[  
Arun Gupta  
](https://twitter.com/arungupta)  
. My daughter is still a bit too young for programming I’ll probably have to start next year when she is 4. The session was fun and engaging, but the real kicker came later… All thru the session Arun dropped references to teaching his son to work on the Minecraft source code. In the general session his son (Aditya) got on stage and stole the entire JavaOne show, he launched Eclipse and went into a compelling demo (and anyone who ever demoed source code modification on stage knows this is HARD and BORING). The kid had the audience eating out of the palm of his keyboard, it was one of the best demos I saw on the JavaOne stage and the audience was totally with him as he complained about Eclipse or lamented on a method needing to be overriden “not sure why”. James Gosling followed him noting that now he wants to become a minecraft hacker!  
  
  
  
  
Wednesday started with a session by  
[  
Nuvos  
](http://nuvos.com/)  
who are launching a WORA mobile solution in Java using a build cloud (sounds familiar?). They had a booth at the pavilion so I got a chance to talk to them and figure them out a bit.  
[  
Kevin  
](https://twitter.com/nuvosKevin)  
is the founder/CEO of the company, they effectively started as a consulting project that grew into a solution for mobile.  
  
  
Kevin is a great guy, we spent quite a while talking before and after his session. It might surprise some readers but we do wish them the best of luck, a rising tide raises all boats and our shared success would mean the success of Java & WORA for mobile as a whole.  
  

* * *

  
  
  
[  
![Picture](/blog/java-one-detailed-trip-report/java-one-detailed-trip-report-2.jpeg)  
](/img/blog/old_posts/java-one-detailed-trip-report-large-3.jpeg)  
  
Jaroslav Tulach  
  

A bit later I attended a great session “The Chuck Norris Experiment: Running Java in Any Browser Without a Plug-in” which was given by  
[  
Jaroslav Tulach  
](https://twitter.com/JaroslavTulach)  
(co-founder of NetBeans or Xelfi for us old timers) and  
[  
Anton Epple  
](https://twitter.com/monacotoni)  
. The session presented the  
[  
Bck2Brwsr  
](http://wiki.apidesign.org/wiki/Bck2Brwsr)  
JVM that JIT’s bytecode to JavaScript (yep), this isn’t GWT… The JIT literally takes a JAR opens it and jits the content to JavaScript which is then executed pretty efficiently on the V8-JIT (JIT nesting at its best).  
  
The ridiculous part is that it works and works well including debugging and decent IDE environment pretty shocking stuff. Unfortunately it doesn’t support threading (and no WebWorkers aren’t threads). The session itself was great and surprisingly even the Chuck Norris jokes were funny, both Anton and Jaroslav have a great sense of humor coupled with the technical chops to make this session one of the better sessions I attended at Java One. 

  
I tried to talk to them after the session but it was a bit too busy and I started talking with  
  
[  
Yusuke Yamamoto  
](https://twitter.com/yusukey)  
who I thought I didn’t know. Turns out he wrote an  
[  
article about us  
](http://www.atmarkit.co.jp/ait/articles/1210/30/news020_3.html)  
last year and also apparently a  
[  
presentation  
](http://www.slideshare.net/yusukey/codename-one)  
. He is also a former Twitter employee and the author of  
[  
Twitter4J  
](http://twitter4j.org/)  
a very cool Twitter API allowing Java developers to work with the Twitter webservice easily. It would be great to port that to Codename One.

  
After the community general session I ran into  
  
Jaroslav & Anton hacking their way in the hall. I naturally descended on them and went into a long discussion with Jaroslav on thread support for Bck2Brwsr so we can add Codename One support for in browser apps. He is pretty adamant against this saying that it would impact performance considerably (he is right, this would require state transferal which is pretty tough), he pointed me at  
[  
Doppio  
](http://plasma-umass.github.io/doppio/about.html)  
which is doing something similar but I can’t see a community working on this so I’m unsure of where they stand.  
  
  
  
  
  
Regardless you should check out  
  
the  
[  
Bck2Brwsr  
](http://wiki.apidesign.org/wiki/Bck2Brwsr)  
JVM, its pretty amazing.

  
The last J1 session I attended was the  
[  
RoboVM  
](http://robovm.org/)  
session,  
  
Niklas was great although he needs to seriously simplify his message to the audience some of whom didn’t really understand what LLVM meant. He had great attendance considering the fact that this session was announced just before J1 started so most people wouldn’t have seen it in their schedule builder tool (even moreso considering the fact that some people already left as the conference was winding down they literally started folding up the room around us).

  
I would describe RoboVM as a Xamarin for Java based on LLVM. Its an LLVM front end for Java that allows using the toolchain to build native iOS apps with native iOS API’s. Unlike Codename One which aims for WORA, RoboVM is targeted at avoiding Objective-C and giving a Java facelift to iOS development. Niklas did an amazing job in terms of tooling and the underlying technology and we are keeping a keen eye on this project, we spent a great deal of time talking about making a living from open source projects. I do hope that Niklas will find the model that works for him with RoboVM and is able to keep the amazing rate of progress he has shown so far.  

  
JavaOne ended with the usual party at Moscone, however this year they merged it with the Open World party which was too big and sucked as a result. I miss the old garden party which was crowded yet intimate, where you actually knew some of the people in attendance.  

  
Regardless, it was a fun JavaOne for me on a personal note. Hopefully next year will be as good or even better.  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — September 30, 2013 at 7:15 pm ([permalink](https://www.codenameone.com/blog/java-one-detailed-trip-report.html#comment-21640))

> Anonymous says:
>
> I’m wondering whether RoboVM has better stack trace support than XMLVM.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-one-detailed-trip-report.html)


### **Anonymous** — October 1, 2013 at 3:53 am ([permalink](https://www.codenameone.com/blog/java-one-detailed-trip-report.html#comment-22035))

> Anonymous says:
>
> Good question. 
>
> Yes and no. You will still have issue on devices since symbols are stripped and since we won’t be using the native simulator you won’t see most of that anyway. 
>
> I think that if we will start maintaining XMLVM we will make radical changes to it which will improve performance, size and stack trace support. I have some ideas on the matter which I discussed with Arno to verify their feasibility. Naturally this all boils down to time/effort.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-one-detailed-trip-report.html)


### **Anonymous** — October 4, 2013 at 2:07 pm ([permalink](https://www.codenameone.com/blog/java-one-detailed-trip-report.html#comment-21858))

> Anonymous says:
>
> I’m afraid I’m going to sound like a broken record, but thank you so much for this detailed report, it’s almost like being there! Do you know if/when the session videos will be available for viewing, like they did for 2012? Keynotes can only tell you so much… Also, I’m very intrigued by your JavaFX comment “It was a very interesting conversation but there is no point of going into details at this point in time.” We’ll stay tuned, I guess 😉 Congrats on the 21 millions devices! Love the ticker!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-one-detailed-trip-report.html)


### **Anonymous** — October 4, 2013 at 4:20 pm ([permalink](https://www.codenameone.com/blog/java-one-detailed-trip-report.html#comment-21910))

> Anonymous says:
>
> Thanks! 
>
> As far as I know they should upload everything to [parleys.com](<http://parleys.com>) but its not there yet. I wanted to reference some things I saw and checked there today.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-one-detailed-trip-report.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
