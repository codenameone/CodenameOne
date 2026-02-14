---
title: What Sucks in Codename One & How Can We Fix It?
slug: what-sucks-in-codename-one-how-can-we-fix-it
url: /blog/what-sucks-in-codename-one-how-can-we-fix-it/
original_url: https://www.codenameone.com/blog/what-sucks-in-codename-one-how-can-we-fix-it.html
aliases:
- /blog/what-sucks-in-codename-one-how-can-we-fix-it.html
date: '2016-01-10'
author: Shai Almog
---

![Header Image](/blog/what-sucks-in-codename-one-how-can-we-fix-it/which-of-the-following-is-the-most-important-to-you-2.png)

I deeply care about what we do at Codename One and writing a negative post even when the words aren’t mine  
is difficult. I think one of the most valuable thing about an open project is honesty and open communications even when  
that’s unpleasant, **without open criticism we can’t get better**.  
Chen recently sent out a request to fill out our survey via direct mail and responses came streaming in. Added  
to the guys who filled it out after our previous blog post we got a lot of additional valuable feedback.  
We can’t satisfy everyone and we shouldn’t aim to, however developers who spent time answering our survey  
probably like the basic concept of Codename One and should be happy. Most of them by a good margin (over 90%)  
are happy, but its only a good margin not a great margin and in this post I’d like to hash up the problems. 

My goal here is to explain how we internalize your very valuable feedback and convert it into something we  
can work with. I’d also like to explain why some things happen, e.g. what tradeoffs we took that lead us  
to this position. I’ll try to focus on proactive responses and reduce the “excuses” to a minimum but  
I do make some excuses for Codename One below since… well I’m biased. 

One observation I had when reading the responses was that IntelliJ/IDEA developers were far more critical  
of Codename One than developers using other IDE’s. This makes a lot of sense as our current  
IntelliJ plugin isn’t as robust as the other plugins. This is something we really need to address moving forward. 

#### Survey Question: What Do you like/dislike about Codename One?

Stability and our fast update cycles are a problem as expressed in the comments below: 

> I dislike the instability. It happened to me more than once that when I returned to my project after a pause of a  
>  couple of months, my app -which previously worked fine- suddenly looks different and, worse, gives error  
>  messages or even crashes. I get the impression that things are released too soon, before being properly  
>  tested… 

> I like the simplicity and fast development cicle. I however stopped using it after my project simply stopped  
>  building and gave no errors or any hint of what was wrong (I was experimenting with different technologies  
>  for a project). 

> Good online support; great performance on devices / I feel you go too fast sometimes, need to  
>  ensure stability before releasing new version (regressions stop our development)

> When you release some changes and the plugin update it without ask to me. And suddenly my  
>  application starts to present some bugs that didn’t exist before.

We just made 3 HUGE compatibility related changes  
with our gradle migration, performance optimization & animation framework rewrite. This obviously impacted  
the results but the feedback is indeed a valid one and this isn’t the first time this happens.  
We have a standard reply for pro users: “[use the versioned builds]()“. Unfortunately that reply  
doesn’t work for basic/free users and even for pro users who might need the latest and greatest.   
As a side note I’d like to mention that we see a very clear migration path of free/basic subscribers up to pro/enterprise  
tiers. So we care deeply about platform stability even for basic/free subscribers! You are (quite literally)  
the Enterprise customers of tomorrow and we do care. 

Addressing stability is tough for a company of any size but its tougher for a small company like us especially  
with the scale of the undertaking at hand. The mobile world is moving quickly and we need to move faster to  
catch up, so how do we settle the conflict: move fast vs. break things?  
We had heated debates about this internally several times including after reading these posts.  
I can’t say we have concensus, I can at least say we all agree that there is a problem that needs fixing. 

One of the common suggestions we get when faced with this issue is that we need to provide an option to  
build against the last known stable version. Unfortunately, that won’t work because if we will offer such an option  
no one will use the latest and no one will report the issues as they happen. This will make bug fixing much harder  
as an issue will be reported after we already moved to a different task, so we will end up with a situation worse  
than we have today as it would take us longer to fix issues. 

We see the long term solution as a two pronged approach. We need better internal QA and a better process  
for handling regressions. In retrospect we should have reverted some of the performance changes sooner  
as a temporary measure to get code working. We will try to be more vigilant with issues like this as we move  
forward. It was also a mistake to do this change during Christmas, we incorrectly assumed there would be  
less damage with people on vacation. The actual result was that the issue took longer to surface and when  
it did we were too deep in to revert properly.  
I’d like to further emphasize that we need your bug reports and communications in  
[stackoverflow](http://stackoverflow.com/tags/codenameone) and the  
[support forum](/discussion-forum.html) as soon as you see a potential regression. If you see a  
sudden issue first check the  
[blog](/blog), we try to communicate all major changes thru the blog. This can give you a  
sense of the areas we are working on  
and if one of those changes could have triggered a regression you are seeing. 

> Out-dated tutorials or code examples.

> Some of the documentation is out of date, especially in regards to using it with Eclipse. 

Its painstaking going over everything. If you see something that’s out of date or plain wrong first  
write a comment there. Then file an issue in our [  
github issue tracker](http://github.com/codenameone/CodenameOne/issues/) so we can assign an action item to dealing  
with that. 

> Disliked lack of updated documentation/tutorials/support for CN1 workarounds for missing  
>  Java libraries

> Like: Java code / dislike: no medium and advanced tutorials/videos, layout not work on iphone. 

> Documentation should really be improved

> Lag of clear examples on how to do things

> The lack of information and tutorials

> will be good to have an small code section to show the classes ussage.

> The samples often do not use the “Visual” approach so a lot of time is spent translating the code.  
>  For Example: The Validator-Component.

> Dislike the learning curve.

> Some features are difficult to find documentation

> quality of documentation, details of docs

We had a sense that people weren’t using the developer guide as much as they are. With the results of this survey  
it dawned on us that this was a misconception. We need to work on the documentation  
more thoroughly and keep changing it while working on Codename One itself rather than at the code freeze  
week. However, the original chart we posted turned out to be based on a smaller sample than our final result,  
it seems the “How Do I” videos do occupy a large segment of the docs. 

![What form of documentation/help do you value the most?](/blog/what-sucks-in-codename-one-how-can-we-fix-it/what-form-of-documentation-help-do-you-value-the-most-2.png)

Keeping the documentation in sync for something as big as Codename One is a big task and doing it in parallel to moving  
everything forward is even harder. However, building features without documenting them is probably useless as  
no one will use said features…  
So we’ve decided to set documentation and reworking of the videos as one of our top short term priorities.  
We need to re-think a lot of things in regards to documentation, e.g. should we move to a more open platform  
like git with our docs?  
How should samples integrate into the documentation?  
Generally I think we need to improve the process to something that’s more open but still works into our  
general workflow of building everything together. Ideally samples should integrate directly into git so you  
guys can contribute/use them right away. 

One of our concerns here is tooling, right now we use asciidoc to generate the developer guide and this is  
a bit problematic as we can’t seem to get the right look we had when we just used a word processor.  
It also makes working with proofreaders much harder as they can’t read asciidoc and communicate with us  
via the PDF file.  
If you have experience with open source toolchains for documentation we’d appreciate some tips here…  
For videos I don’t see a way of scaling this by much, I’ve improved my video generation process but its still  
far behind the speed we would need to redo everything and update with new developments. Steve did some  
great videos but we have a lot of tasks and not enough time. The only solution here is for me to priorities videos  
over my other tasks which is something I will try to work on. 

> Dislike: performance on iOS and to some extent on WP

> Dislike sluggish performance

> I think that if the apps have more performance it would be great!

> Improve performance in android.

Performance has shifted to our top priority just before the survey launched so this is quite important to us and  
I think that we were lax on performance for a bit too long.  
Having said that, most of the performance problems we see every day have nothing to do with the performance  
improvements we made and are a result of misuse of Codename One’s API’s and disregard of our guidelines  
which we documented poorly… (totally our fault). 

Part of the fix for performance should be improvement of tooling that would detect performance issues in the same  
way we detect EDT violations. We should also redo the documentation and videos on performance, this is obviously  
an important issue that needs attention probably more than any other issue. 

> Improve performance and always introduce latest trending features in mobile development space (e.g. Google material design and 3D touch) 

We have been slow with adopting some latest trends such as material design patterns and other capabilities.  
These are things we are trying to do right now as we handle all of these other issues. Currently the material  
design level effects are on our top priority list.  
3d touch isn’t there yet, but we need to support that as well as pen input and various other capabilities. There  
are some complexities in doing that though, we’ll need to migrate to newer versions of xcode which might produce  
build regressions for some users and we are procrastinating on that. We will however, need to make  
a big leap to xcode 7 sooner or later and that might cause some issues similar to the Android gradle  
migration issues. However, because of the way Apple does things we won’t be able to provide you with  
a fallback flag like we do in the Android build… 

With the Android gradle migration we can easily fallback to using Ant if you explicitly ask to do so. Its our  
decision and Ant still works just like it did before. However, Apple decided to limit newer versions of xcode  
to newer versions of Mac OS X. In itself this shouldn’t be a problem, but Apple also broke the older version of  
xcode when updating the OS…  
So if we upgrade our servers to the latest OS X we won’t be able to run in compatibility mode for the older  
versions of xcode. 

> Limited native features, when your client says hey you GOT to use this SDK, and cn1 doesnt have a lib, you feel  
>  pretty backed into a corner because you then have to go write a native lib for 2 platforms. 

While the number of cn1libs is growing and we have the tutorial on wrapping a native SDK from Steve I do see  
the point here. Unfortunately I’m unsure what we can do here.  
For enterprise developers we sometimes help in wrapping cn1lib’s that they might require (we then place them in  
a public repository as part of the cn1lib section). Accessing native API’s/libraries will probably always require some  
manual work from the developer or from us. I’d love to have more automation on that but I doubt its technically  
feasible. 

> What worries the most my boss is the fact that we are “bound” to a build server to compile the apps.  
>  He would prefer that we could be indipendent in that phase. On the other hand programming in java for all the  
>  platforms and with the same set of APIs is wonderful! 

Since Codename One is open source the binding isn’t as bad as it could be. If worse comes to worse you  
could always build from code. When picking any platform (native or otherwise) you end up depending  
on the platform, e.g. Apple changed a lot of of things just these past 2 years (64bit, bitcode etc.) all of which  
were huge disruptions for native developers but not as big for Codename One. 

> To difficult to use some components. For example table use sucks.

We’d like to hear about those things. Table has its issues and we could improve the API for various use cases so  
just open a discussion in the forum with some suggestions and you can also fork the code to contribute  
enhancements.  
Personally, I never ran into a Table API I liked on any platform and ours isn’t much different. 

> Is the Bluetooth API finally available?

> Its java based, easy to use, very good support and fast response time. Pore support for Windows  
>  phones, lack of standard functions like bluetooth and NFC

Bluetooth is something we got asked quite a few times for.  
Then when the time came to implement it we contacted several developers and asked for specific use cases  
for bluetooth. E.g. which of the many bluetooth API’s do you actually need and for what purpose? 

It turns out that 3 out of 3 developers didn’t really know what they wanted but had a requirement to “support bluetooth”  
without any actual information. Normally when we are faced with a situation like this we look at similar products  
and try to create a similar API to something like Cordova. However, we couldn’t find any WORA tool  
that implemented bluetooth so we don’t have anything to go on.   
Notice that you can always use a cn1lib to build bluetooth support without our help. 

The lesson here though is: you need to be clear about what you are asking for. Asking for bluetooth is like  
asking for “internet”. You need to ask more specific things e.g. bluetooth discovery and serial API’s. You  
need to file an official request as a pro/enterprise developer and give proper details.  
Otherwise these things just get buried under the amount of work we have piled up. 

About Windows Phone support, this has entered as one of our highest priorities. I wrote a lot in the past about  
how we got to the state we are in, I generally blame Microsoft and their volatile offhand approach to the platform  
but we are also to blame here.  
Since this requires a ground up rewrite this might take some time as we are under staffed. The work Fabricio  
did in open source can help us a lot but ultimately the big problems are in the VM and the servers hosting  
the Windows builds. Both of these should be fixed. 

#### Survey Question: How Can we Improve Codename One?

> More functionality, like cron, need better api to handle pdf files I am running issues opening pdf in  
>  the App got very little help being a pro user.

We already have some basic cron like support see [this](/blog/local-notifications.html).  
The use case of viewing a PDF locally is something we need to improve, as a pro user you need to clarify  
that an issue wasn’t resolved. You also should ask for a specific enhancement if you need it otherwise  
this probably won’t make it into the queue of tasks.  
We should work better on communicating this. 

> Your very core and idea are cool. Your CN1 implementation has quality of “proof of concept”, i.e.  
>  alpha, i.e. minimalistic albeit working core. It’s ok for me, because I’m like you, but current spoilt generation of  
>  programmers needs more comfortable conditions, so they don’t shoot themselves into foot each time. Invest  
>  more money, hire more developers (or, if you think you have enough, hire quality manager, he will return you to  
>  the ground). Focusing on PR (recent months raised activity) alone is inadequate at current stage, it is not  
>  proportional to developer resources and development pace you have. You say IntelliJ idea plugin is not up to  
>  date, your previous UI designer (did not see next one yet) looks like 2-nd year student’s project, your demos  
>  (where they have a text, e.g. propertycross) all use one unattractive bold font and they mostly lack design, they  
>  are uninspiring, you put caret in the middle of second letter of the hint (see screenshot of search screen in  
>  propertycross app – this all because of broken padding/marging based on 4 styles concept, i had the same in  
>  my apps), you did not implement high-level ui concepts like those found in material design, and you don’t have  
>  StyledTextView control!! In other words, you didn’t advance much since J2ME means of expression, And at  
>  the same time you advertise it all, just to get more people raising their eyebrows, and causing “shai, relogin”  
>  after any positive words towards cn1 on forums.

Our designer is based on an official product from Sun Microsystems and is actually far improved over that but  
I actually agree. It needs a rewrite and is undergoing one (albeit a slow one).  
We are acting on a lot of the refinement issues mentioned quite a few of them are already in place in the  
NetBeans/Eclipse versions of the plugins. E.g. we rewrote the GUI builder and switched to new default  
themes with better fonts. I do agree we should do a lot more there and the hint text is a good example…  
I don’t think you offered a solution to the 4 style approach in the comment, I don’t think there is a generic  
solution to that. I do think we need to improve our demos and bring them to the refinement level we see  
in some of the finished apps from some developers. 

StyledTextView is one of the painful features of Swing so no we don’t have it although Steve created something  
a while back based on an HTML editor component (notice that unlike Swing/FX we have a proper webkit browser).  
This is actually something we looked into quite a lot and  
would love to have but you would be shocked at the state of native support for styled input…  
There is no unifying logic or even support for styled input in iOS/Android, its possible but remarkably hard on  
both platforms and integrating the native input into our logic of this would be “difficult” to say the least. 

> Lets start by creating a really nice theme that looks and works great on all platforns.

I agree, our themes need a lot of work. 

> Better support to 3rd party APIs, use Google’s GUI builder, offline local incremental building, no  
>  automatic updating of versions – even for basic users – I like to control when things change

Google’s GUI builder is for Android, our API is too radically different and this just won’t work. Its unrealistic to  
try and adapt it. Might as well adapt Matisse which we did in the past until we hit a brick wall… 

You don’t control when things change on mobile since Google/Apple deploy OS changes and restrictions meaning  
your appstore submission will suddenly fail because Apple made a change in requirements.  
Building incrementally is a problem with build servers as it makes us into application hosts which is something  
we don’t want to do. Building offline would be painful for everyone but most of all to the users. We can only  
test on a very limited set of configurations and we finely tune our servers to match. Now multiply that number  
of configurations by 10,000 and you get the drift.  
If you want to build offline and to a specific self controlled version you can use our source code, its there.  
To me that’s an unattractive option since it makes everything much harder ultimately. Even if we try to officially  
support that we won’t be able to simplify it by much since we would still need to send you off to install this  
and that for every single platform… 

> host online webinar or Q&A sessions

Steve did quite a few of these and we worked hard to promote them. The attendance wasn’t as high as we  
expected so we decided his time could be better spent doing other things.  
I think that if Steve spends the time he did working on the webinar by just improving the docs & demos this  
would probably be better. One of the problems with a webinar is its time sensitive nature, since our community  
is dispersed widely across the globe it makes the schedule unreasonable for half of the community regardless  
of the time slot chosen. 

> Making able to start the app using argumeents. Lets say… if you could fire a notification… an you  
>  press it…. then you could open an specific part of the app…. same way calling the codename one app from  
>  another app using an specific intent. Also.. is important the support of services and bluetooth

That’s already supported, the docs are sometimes complex to comb thru so I suggest using stackoverflow if you are  
looking for something specific. The app args option can be passed via URI to other apps on execution and a  
push notification can also include hidden payloads. 

> Improving your support, improving the gui designer and providing more animations and responsive  
>  design elements in the designer. I’m a CN1 evangelist and the only weak point that my developer friends  
>  always say about CN1 is the gui designer and that the generated UI isnt as nice and as easy to develop as  
>  another tools 

> 1) Better GUI editor, I really like the current one but it has some problems with reordering in the  
>  filetree, and some confusing usability issues. 2) Ability to somehow send off a build and get a notification on  
>  the device automatically that you tap to install, the email to yourself and click link approach slows down  
>  productivity imho. 3) “native simulators” would be good, ie it can launch an (existing) real android emulator  
>  and a real iphone emulator [or EVEN a real device plugged into usb!] (this is a pipe dream on windows since  
>  no one can emulate iphone which sucks, been using macincloud recently, would probably gladly have that  
>  open to see it launch in an iphone emulator rather than needing to send a build and install each time) – the  
>  main reason I mention this is that you need to view it natively to see the real changes, some of the tiny  
>  tweaks i had to do for iOS to get the top status bar thing to look perfect took days due to the cloud building,  
>  if your simulator could somehow replicate the appearance perfectly it would be good, but Im assuming that  
>  is somewhat impossible.

Did you try the new GUI builder?  
We pretty much agree that this is a very important direction to go. We do need a lot more feedback on the new  
GUI builder though. 

The second set of suggestions should be addressed by on-device-debugging which will use an on-device-agent  
approach for execution so it will be fast and instant. Notice iOS doesn’t have emulators only simulators so  
this will not be applicable for those. 

I’m not sure I follow the problem with support, we already put pretty much all of our available time into support.  
How did you try to get help from us and how did that not work out for you? 

> Better scrolling mechanism/algorithm/impl

Did you try the latest on-device versions? We just replaced it not so long ago. 

> I often find myself having a hard time finding the documentation I need. There is a lot out there,  
>  but it is very fragmented, so requires a lot of search time. For example, I can go to the basic java doc to get  
>  the full reference of all the components, but to get code examples I have to try to find a video of something  
>  close enough being done, or maybe if I’m lucky a most basic usage might be available in the developer guide  
>  to help me. Less common use cases leave me to using guesswork and searching for others work. Would be  
>  nice if java doc had more detailed information or even small snippets for all methods.

I very much agree with that feedback. Having a link to sample usage and more extensive docs seems like  
a great direction. Also links to usage samples on github would probably go a long way in helping the docs. 

> Write more unit tests

I tend to agree with that a lot but the problem isn’t “more”. The problem is “better”.  
The big hard to detect in advance issues are usually device compatibility issues and those won’t  
be detected by unit tests with our current feature set. 

We would love to have feature such as automated on-device testing within device farms, once we have  
that we can obviously use that internally for our own deployments. Currently this isn’t one of our top priorities  
since its a pretty big feature that doesn’t “sell” so its very hard to justify that effort in the short term. However,  
we would like to add that in the slightly longer term vision. 

> Some emulation devices don’t work properly. For example, I have bug on HTC where the bottom  
>  of my panel gets chopped off, but it looks fine in the emulator. 

This is indeed painful. We are looking at doing on-device-debugging and it seems that this actually carries more  
interest than our initial survey revealed. We think that on-device-debugging will be a game changer for many  
Codename One developers. 

In the interim did you contact us about such issues?  
Did you get help?  
We invest a lot of time on support both to improve Codename One (based on your feedback) and to help you  
guys get around the rough spots. 

> Resolve the windows 8 versions and ability to directly build BB10 app instead of android apk version.

Our decision to avoid BB 10 seems to have paid off as RIM themselves are discarding it in favor of Android.  
We probably won’t invest further in Windows 8 support and focus our efforts on getting a proper universal  
Windows 10 app build that will work on desktops, tablets and phones. See some of my comments above. 

> Create another subscription level between the basic and the pro for developers that dont need so many builds or notifications every month

We got asked this a lot and its hard for us to tell whether this would really make sense financially  
and technically. There are standard industry measurements for pricing distribution that show whether  
your pricing is sensible (ratio of users picking every tier).   
Based on those objective measures pro doesn’t seem to be too expensive although basic used to be too cheap. 

> Make it mvc 🙂

The list, table & tree classes are MVC and those are the hardest classes to deal with in all of Codename One.  
Swing over did MVC by applying it to components that made no sense for that e.g. Buttons. Our goal was to create  
something that’s simpler in a “good way” and avoid over abstracting some notions. 

I would even go further by saying that we are removing some of the Swing like MVC usages that are over the top.  
E.g. copying Swing’s approach to building List was a mistake in retrospect and we are now focusing on a  
more modern approach of infinite scroll. 

> I’d love to see the CSS tool being integrated into CN1

I’ve commented about this in the previous post about this survey and it seems there are several people who  
are interested in CSS… Make your voices heard!  
If we don’t hear you and don’t see activity we won’t work on this!  
Try Steve’s plugin and start filing issues on that, once we start seeing people use it we will integrate it into the  
platform but this has to be something we feel is stable and in demand. 

#### Where Do We Go From Here?

The feedback we got here is great! It matches a lot of our feelings yet contradicts other assumptions we held  
onto.  
I think this is a good time to show the final version  
of the answers to “Which of the following is the most important to you?” and “Which of the following is the second most important to you?”.  
Both of these changed significantly: 

![Which of the following is most important to you?](/blog/what-sucks-in-codename-one-how-can-we-fix-it/which-of-the-following-is-the-most-important-to-you-2.png)

![Which of the following is the second most important to you?](/blog/what-sucks-in-codename-one-how-can-we-fix-it/which-of-the-following-is_the-second-most-important-to-you-2.png)

Historically we stopped publishing our roadmap since in the past we got into trouble with  
developers when we couldn’t keep up (because of other developers pressing on another deadline). But in light  
of this I think we need to break the rules a bit and publish a set of priorities for us as stewards of Codename One.  
The order of these priorities is important: 

  1. Performance & Refinement – performance is a big ticket item but with recent changes I think we are 90%  
there. A lot of the remaining work is in giving you the tools to track why a Codename One application is sluggish both  
in terms of documentation/tutorials and in physical tools (e.g. performance monitor).  
E.g. Chen was recently reviewing a performance issue that was triggered by making a single solid color UIID  
opaque instead of transparent. These things can sometimes be very hard to track even for us.  
The refinement aspect is arguably more complex, themes that are more complete, material design capabilities  
and an easy/intuitive out of box feel.
  2. Windows Phone Port – while this didn’t get the top spot it is indeed painful to many developers. We are  
investigating a potential shortcut we can take that will hopefully save us the need of porting ParparVM  
to Windows. We would do the port anyway and probably base it on the excellent work from Fabricio. We see  
this is the second most important thing we have to do but with the volatile nature of this task we can’t really  
commit to a timeline.
  3. GUI Builder – we wrote a completely new GUI builder and we committed to having it at beta grade for 3.3.  
We intend to keep with that plan and enhance it based on your requirements/feedback.
  4. Documentation – we need to review everything about our documentation and redo as much  
as we can. Ideally also go to older sources and reference the newly available data immediately. This is painstaking  
work especially given the volume of documentation we have.  
In the past our docs were open but they weren’t easily accessible. We’ll try improving this and refining the tools  
to work with the docs. This work might grind our progress to a standstill but I think its crucial that we do it now.
  5. IntelliJ/IDEA Update – we need to put out a fix for this. This might be in a form of moving to maven/gradle to  
simplify the work that the plugin does today.
  6. On device debugging – we think we can leapfrog a lot of the solutions in the market with our approach to  
on-device-debugging. This is something we wanted to do for quite a while and its hard to get to with our  
current set of limited manpower. If we get thru the list above this is solid on my personal wish list…

You will notice some important tasks that are missing from the “top priorities list”.  
E.g. a complete rewrite of the theme designer or a CSS alternative are missing. This has been a part of our TODO  
list for quite a while but we left it out of the current priorities. We want to evaluate the reception of the new  
GUI builder as well as the reception to the CSS plugin from Steve before making any long term decisions here. 

Another thing that’s important and missing is new Java language features. Even though 25 percent of you indicated  
that they want this as their highest or second highest priority. The main problem here is in our phrasing of the  
question, Java language features mean very different things for different people and while we agree with the  
basic premise of extending the functionality of the VM’s we have doubts about the scope. We might conduct  
a separate survey dedicated for this exact purpose but right now we have enough on our plate. 

#### Summary

I hope this gave you some answers about why things are in this particular way. I left out a lot of comments because  
of many reasons but I think I got most of them and didn’t pull punches. I hope this did give you a better sense  
of how we plan to fix these issues and maybe some ideas on how to help us with that process.  
Feel free to use the comments section to refine or better articulate some of the points made, or maybe  
answer some of the questions I had about unclear comments above. 

I’d also like to thank all of you who answered the survey in great numbers, it has been very illuminating.  
We really enjoyed the kind words in it as well, which made the other comments go down easier. We really  
appreciated the rougher comments as they do help us improve.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Fabrício Cabeça** — January 11, 2016 at 10:49 pm ([permalink](https://www.codenameone.com/blog/what-sucks-in-codename-one-how-can-we-fix-it.html#comment-21491))

> Fabrício Cabeça says:
>
> Thanks for mentioning my work with the Windows port, you can count on me in case you want to take this leap of faith. To me this is one of the biggest problems in cn1, the other one is the fast lifecycle that delivers great features but great headaches too. Versioned builds helps but even there I had issues before.


### **Carlos** — January 17, 2016 at 4:11 pm ([permalink](https://www.codenameone.com/blog/what-sucks-in-codename-one-how-can-we-fix-it.html#comment-22689))

> Carlos says:
>
> I totally agree with the previous comment. Windows Phone support is important for me. On the other hand, regressions force us to write workarounds that can even be the cause of future problems when the issues are fixed.
>
> Apart form that, I can’t emphathize enough how good CN1 has been for us, and how it has been a game changer for our business model. Thanks for that.
>
> Oh, and please, whatever you do, don’t deprecate the list rendering approach. It may be a hassle to some developers, but I like the concept and actually make use of it on some projects.


### **Shai Almog** — January 18, 2016 at 3:43 am ([permalink](https://www.codenameone.com/blog/what-sucks-in-codename-one-how-can-we-fix-it.html#comment-22523))

> Shai Almog says:
>
> Thanks!
>
> We made a big mistake with the recent set of updates. We should have tiered them more slowly, added flags to disable them more easily and we shouldn’t have done them while everyone was on vacation (which seemed like a good idea when we started off).
>
> No worries, we are probably stuck with renderers for good. The main change we’d do is in de-emphasizing it in our docs and trying to gently nudge people to use components/containers which are pretty performant.


### **joaa** — April 16, 2016 at 4:29 pm ([permalink](https://www.codenameone.com/blog/what-sucks-in-codename-one-how-can-we-fix-it.html#comment-22381))

> joaa says:
>
> I love to be able to import my code from android studio and just continue working on it without the need to start from begining. Any comments?


### **Shai Almog** — April 17, 2016 at 12:10 pm ([permalink](https://www.codenameone.com/blog/what-sucks-in-codename-one-how-can-we-fix-it.html#comment-21556))

> Shai Almog says:
>
> Thanks for the feedback. This is a point that came out a few times before so I posted a blog entry covering this here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/why-we-dont-import-android-native-code.html>)  
> The gist of it is “we’d like to do it but it’s damn hard so we might get to it one day…”.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
