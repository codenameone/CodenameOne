---
title: Spring Template and more about the Bootcamp
slug: spring-template-more-about-bootcamp
url: /blog/spring-template-more-about-bootcamp/
original_url: https://www.codenameone.com/blog/spring-template-more-about-bootcamp.html
aliases:
- /blog/spring-template-more-about-bootcamp.html
date: '2017-03-05'
author: Shai Almog
---

![Header Image](/blog/spring-template-more-about-bootcamp/full-stack-java-bootcamp.jpg)

__ |  It’s LIVE! Check out the bootcamp signup [here](https://codenameone.teachable.com/p/full-stack-java-mobile-app-bootcamp/).   
---|---  
  
Thanks for all the comments and interest on the [previous post](/blog/full-stack-java-bootcamp/). In that post I discussed the importance of great app design and showed some of the cool newish demos I built to highlight the general direction. I also talked about the importance of personal mentorship especially with new/elaborate mobile technologies where **everything** is different.

I also discussed the bootcamp with broad strokes, I won’t go into too many details about it today (I’ll write more later in the week). Just to clarify the general direction, the main focus will be on UI/UX. This means we will build a complete production grade application from the ground up with server connectivity and everything…​ I’m aiming this as an advanced bootcamp (deep into native interfaces etc.) but I have provisions for people who are new to Codename One.   
One of the things that really excites me is getting to know you (our community) better on a more personal level. I find it very hard to keep up with all the thousands of people that I’ve communicated with in the past 5 years. Putting faces, voices and some context is something I’m eagerly looking forward to.

It’s been over 10 years since Chen and I started what would later become Codename One and almost 9 years since we open sourced that to the world at Java One. Over these years our offering grew and shifted so quickly that we sometimes forget to mention or document some substantial pieces…​  
Case in point: Steve implemented an integration with Spring based on yeoman a while back and I don’t think we told anyone.

E.g. you can do this using npm/yo:
    
    
    sudo npm install -g generator-cn1-spring-app
    yo cn1-spring-app MySpringApp

__ |  The `sudo` command might not be needed on all platforms   
---|---  
  
This generates a [Codename One Spring template](https://github.com/shannah/codenameone-spring-template) that you can also install manually to build a client server application in minutes…​

I’m not sure if I’ll use this in the bootcamp though…​ Chen is pushing for Spring Boot while I have some thoughts about using JHipster or maybe something else?

If you have strong feelings/opinions on this please let me know in the comments.

I’m not sure how crucial this will be as the core focus is on the client side and the communication layers. On the server we’ll work a lot on the networking aspects, push etc. but most of that stuff is very simple. My goal is to create something that will be generic enough that you can adapt it almost seamlessly to your environment.

### From Zero to Millions of Downloads

One of the main things I want to focus on is “real life” and the transformational effect of this approach. One of my best examples for this is Ram Nathaniel who is probably one of the first developers using our platform.

![Ram Nathaniel](/blog/spring-template-more-about-bootcamp/Ram.jpg)

Ram picked up Codename One during the beta & built one of the [coolest apps](/featured-yhomework/) ever back in 2012. It’s a bit dated by now with its design but it works well and had gained a loyal following because it’s an amazing app.

Ram shipped his app on iOS & Android without a problem but it stagnated for a couple of years. Ram like a lot of us didn’t have the proper expertise in marketing and promotion to push the app to the prominent place it needs to be. A lot of apps die in this exact spot… Most of us think of a competitor as the worst thing that can happen to our app and a strong competitor is exactly what happened!

A highly visible YC funded company entered the field and got a lot of visibility/PR.  
This sounds like a disaster right?

In Rams case this was a blessing!  
Typically for that time the newcomer was available only in iOS. Ram seized the opportunity & as a result people searching for similar solutions on Android installed his app increasing his install base by leaps and bounds. These users shared the app socially boosting his iOS installs as well.

While the specific app Ram built is unique, the lessons learned are not. Getting into the market fast and supporting secondary markets such as Windows Mobile is often a powerful play for smaller companies. You still need to invest in design, marketing and the general alphabet soup as you don’t want to rely on luck alone.

This isn’t unique to the startup world. I worked with customers ranging from banks to operators and so forth. They all benefited from a simpler approach to mobile.

### Your Feedback

I got a lot of feedback both in the emails and comments in the various posts I made on the subject. The feedback has been overwhelmingly positive and raised a few interesting points. I won’t quote emails/comments directly as I didn’t ask for permission beforehand…​

So here are some of the things I want to address within the bootcamp:

  * Full application development process from certificate to final app in the store

  * Layouts

  * Multi-DPI and resolution independence

  * Building a gorgeous UI – adopting it from PSD. Adapting to platform native conventions or overriding them entirely

  * Using both designer and using CSS to build UI

  * Animations & Transitions – the different types of animations, where and how to use them

  * Threading, the EDT & network threads

  * Security

  * Custom components, composite components and lead components

  * Working with GUI builder and it’s underlying XML

  * Localization, Internationalization & RTL/Bidi

  * Working with sqlite DB

  * Application design – separation of concerns, best practices & Codename One’s interpretation of MVC

  * Tuning performance and memory usage for consistent cross device usability

  * Working with native code, integrating native library

  * On device debugging with a Mac

  * Client server communication – webservices & web sockets

  * Server initiated push

Time permitting I’d like to also cover things like parse, databinding with properties, debugging the Codename One sources etc.

Because of the way a bootcamp is structured this is pretty fluid and we’ll shift it around add/de-emphasize things based on the desires of the group.

I’m also still looking for feedback on that list so if you think something is missing please let me know.

### Up Next

Later in the week I want to outline the exactly how everything will work out as there is obvious logistic complexity with my time and how we are going to run this bootcamp.

I’m thinking about it as a startup experience to go thru together, it’s hard, fast and very effective. The intensity is a core part of a good bootcamp and it applies to everyone who takes part in a bootcamp including me. The trick that always works for me is to dive head first into the challenge.

I got a lot of personal contacts and a some comments asking how they can join. I’m sorry but we just haven’t scheduled everything yet so I don’t know for sure. I’ll try to get the basics up by the end of the week & by early next week I think I’ll have the final details. What I can tell is that we’d like to move fast and open/close signup quickly as the scheduling for this is turning out to be really difficult.

P.S. Thanks to all of you who shared my last post within your social circle, I really appreciate that as it brings more people into the discussion and helps us make a better product overall. I would appreciate your taking the time to share this article as well!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jérémy MARQUER** — March 6, 2017 at 4:39 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23313))

> Very interesting program !  
> As I did not take the time to reply by e-mail, I tell you here what I would like to see in the bootcamp : Unit and Integration Test ! 🙂  
> Since I had begin to work on my codenameone project in 2014, I’ve experimented many subject you have mentionned above (full application dev, security, sqlite database, nice UI, threads, webservices, third parties, animations etc…). My application is currently in beta testing and right before to release in AppStore, I would like to cover it with some unit test …  
> I will probably tell you more details on this application later, with screenshots.
>



### **Dalvik** — March 6, 2017 at 5:06 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23379))

> Dalvik says:
>
> Great story about Ram, I was impressed that despite looking dated his app has a 4.2 rating on play from 40k users which is pretty decent and beats a lot of apps out there.
>



### **Shai Almog** — March 6, 2017 at 5:09 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23191))

> Shai Almog says:
>
> Great!
>
> TDD is one of those things I’d love to improve in Codename One. It’s my personal second on my wish list after on-device-debugging.
>



### **Shai Almog** — March 6, 2017 at 5:11 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23289))

> Shai Almog says:
>
> One thing I didn’t highlight enough was the fact that when he wrote the app there were no tutorials, guidelines or anything. No developer guide just a blog and javadocs. So he did a pretty amazing job for the shape Codename One was back then. I wonder how the app would look if he had started working on it now.
>



### **Chad Elofson** — March 6, 2017 at 5:37 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23197))

> Chad Elofson says:
>
> JHipster is Spring Boot with Angular. I have been wanting to try it out.
>



### **Fabrício Cabeça** — March 6, 2017 at 5:45 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23407))

> Fabrício Cabeça says:
>
> Hi Shai, if you enjoy a dangerous approach please take a look at our crude swagger plugin for cn1 at github 😉  
> [https://github.com/Pmovil/s…](<https://github.com/Pmovil/swagger-codenameone-codegen>)
>



### **salah Alhaddabi** — March 6, 2017 at 6:11 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23425))

> salah Alhaddabi says:
>
> Dear Shai, since you are trying to focus more on the UI/UX I would really strongly advice that you just stick to the standard java ee 7 api without going into any specific free or commercial third party APIs. That way everyone can follow up without having to learn something new on the server side. If you have written specific plugins to work with spring then its better to discuss that on a separate blog please. I think most of us here are very familiar with the Oracle java ee APIs but we are not all using spring or jhipster plus I am sure there are very good tutorials out there that could explain more on those subjects. So please please please just use the standard java ee 7 API as you can achieve all server tasks by using it. Thanks and sorry for the long reply.
>



### **Shai Almog** — March 6, 2017 at 6:53 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23258))

> Shai Almog says:
>
> Yes, it’s spring boot with a few extras, angular being one of them. Mostly it’s the scaffolding process they use.
>



### **Shai Almog** — March 6, 2017 at 6:54 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23099))

> Shai Almog says:
>
> Interesting!
>
> I’ll post about it in the blog but probably too risky for a bootcamp 😉
>



### **Shai Almog** — March 6, 2017 at 7:00 pm ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23337))

> Shai Almog says:
>
> I mostly agree with what you said and want to keep the “hot stuff” down so most people can apply this instantly.
>
> Having said that, I’d like to move thru the material quickly and focus on the client side. If I end up writing low level webservice code or persistence code on the server I might end up spending too much time on the server tier and too little time in the client tier. I’m also trying to focus around what people actually use in the field today and spring boot is pretty common by now.
>
> Even if I will use spring boot most of the code I will write will be standard Java EE stuff and should work on any server out there so it shouldn’t detract from the applicability of the code. Most of what spring boot brings to the table is a ready made starting point which means I’ll spend less time on server code that most of you already have.
>



### **salah Alhaddabi** — March 7, 2017 at 4:38 am ([permalink](/blog/spring-template-more-about-bootcamp/#comment-23211))

> salah Alhaddabi says:
>
> Thanks Shai its ok if you insists on using spring boot but we expect that you will be going through the basics of spring boot as well please because some of us are only using standard java ee. Thanks again.
>



### **Shai Almog** — March 7, 2017 at 5:17 am ([permalink](/blog/spring-template-more-about-bootcamp/#comment-24128))

> Shai Almog says:
>
> Sure, I don’t assume developers know this. It’s relatively simple and localized otherwise I wouldn’t consider using it.
>
> Since the bootcamp is very interactive unlike a course I’d expect more “back & forth” in case I go to quickly or don’t cover something properly.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
