---
title: Survey Results (2018) Upcoming Apps
slug: survey-results-2018
url: /blog/survey-results-2018/
original_url: https://www.codenameone.com/blog/survey-results-2018.html
aliases:
- /blog/survey-results-2018.html
date: '2018-03-13'
author: Shai Almog
---

![Header Image](/blog/survey-results-2018/codenameone-academy.jpg)

I already teased about the surprises I got from our annual survey results and there are still quite a few surprises but as more people filled out the survey some of the big surprises tamed and a lot of interesting results emerged. I also found the comments very interesting so I’ll go over the numbers/comments and provide my thoughts. If you think I misinterpreted the results please let me know…​

### Which would you prefer for the “Social App” clone?

In retrospect I think I made a mistake by not separating Snapchat & Instagram into the next question. But I’m not sure I’d want to create “another” social app too soon so placing them in the 2019 survey might be better:

![Which would you prefer for the ](/blog/survey-results-2018/which-would-you-prefer-for-the-social-app-clone.png)

Figure 1. Which would you prefer for the “Social App” clone?

Facebook is a clear winner here so the next app I’ll release would be a Facebook clone. Having said that there is clear interest in both ephemeral communication (snapchat) & in photo sharing/manipulation so I’ll try to discuss both of these to some degree. I’m not sure if they will make it into the current app though.

__ |  I stripped out a few “no opinion” responses to make the results easier to read   
---|---  
  
### The Next App

I divided the question of the next app into two, the #1 choice and the #2 choice. A lot of developers are focused on one thing so having two choices might help us get a better/wider sense of the things you are building.

#### What’s your #1 Choice for the Next App? (After Facebook)

Lets start with the data:

![app choice 1](/blog/survey-results-2018/app-choice-1.png)

Figure 2. What’s your #1 Choice for the next App?

Before I proceed one of the feedback comments brought my attention to the fact that I might have biased the poll by phrasing the question as “After the Facebook App”. I totally missed that. I’ll try to pay more attention next time!

Clearly whatsapp won. Initially this wasn’t the case. It was losing to AirBnB. After a while Netflix came from behind to grab the second place spot. Notice that there are a lot of people who specified other which means I missed a few important apps (I’ll get into that soon).

Before I go into the analysis I think it helps to see the second choice too…​

#### What’s your #2 Choice for the Next App?

![app choice 2](/blog/survey-results-2018/app-choice-2.png)

Figure 3. What’s your #2 Choice for the next App?

Whatsapp and Netflix pretty much tied here but AirBnb shrunk.

### Analysis

Before I go into analysis and comments I’d like to declare the winners!

  1. **Facebook** – The social app which won #2 place in last years survey will be a Facebook clone

  2. **Whatsapp** – The next app released in the summer should be a whatsapp clone

  3. **Netflix** – The app after that should be a Netflix clone

  4. **AirBnB** – Currently the AirBnB app is in the lead for 4th place but I might revisit that with a new survey

#### Unsurprising Results

There were quite a few things that seem obvious and made sense in the results. At least in retrospect.

Whatsapp has always been a popular app and our old chat app tutorial is pretty old by now.

AirBnB is one of the best looking apps out in the market, I’m sure people want to learn how to build something like that. It also rides the wave of the sharing economy which is a popular target.

Protosketch got low votes. People don’t know it. Most of the drawing apps don’t enjoy mass market appeal. I added it because people have asked for a drawing app repeatedly, it’s something I’d like to build but it clearly doesn’t have enough commercial appeal.

One thing that didn’t exactly surprise me was the showing for a Mario game. It came in 4th which makes a lot of sense. Most of our developers aren’t game developers but there are a few.

#### Surprising Results

I was surprised by a lot of the results…​

Netflix is big but I don’t see many Netflix clones or people trying to compete with Netflix. I think this mostly indicates interest in the video browsing/distribution process. I think it would have given similar results if I’d had youtube as an option. I think video streaming is an interesting app type so I’m excited to do something similar to Netflix.

Tinder probably surprised me the most but I was also relieved. To clone an app I need to use it and I doubt my spouse would accept an “I’m just cloning the app” excuse…​  
I honestly don’t know why this wasn’t a popular choice.

Square is also surprising. I’m personally familiar with at least two PoS vendors who use Codename One so I assumed there would be more interest in that. It’s possible that this is because square is so focused in the US market & unfamiliar outside of it. Or maybe it’s because of the hardware reader. I’m not sure.

I would have expected Spotify to have a better showing. I’m guessing that most developers feel the music field is too competitive to enter at this time.

#### Comments & Suggestions

I very much appreciate all the comments and suggestions that highlighted some omissions and also some things I should improve in our communication.

__ |  Notice I cleaned up the comments a bit and unified them to make them more readable, if I missed a comment it might be by mistake!   
---|---  
  
##### App Suggestions

A common request was for business oriented apps:

  * Appointment Setting App (e.g. for Doctors)

  * Business oriented app (e.g. CRM mobile client)

  * ERP client

I’d love to do one of those but these are harder to market and generalize. Each business is doing something different in their backend and it’s much harder to just make up something from scratch.

If there was a business app I can replicate or a backend ERP platform we can target that could be interesting but it’s very hard to teach/build something vague.

One of the things I did think about was an Intercom clone. It’s a CRM of sort. We use them for the website support and emails. I really don’t like that tool and would love to replace them. I just forgot to add it to the survey, although I don’t think it would have made an impact.

Other suggestions included:

  * FaceApp – Most of the app is in the face processing code, I think this would step outside of our comfort zone. Having said that there are some cool open source image recognition libraries that I’d love to integrate into Codename One given the time/user demand

  * Runtastic/Fitbit – I totally missed those…​ They should have been on the list!

  * Smart tv universal remote – I’m guessing these just use a communication protocol with the TV. I’m not sure what value we can bring here?

  * Android Pay – I’m assuming this means Google Pay? I can’t install it on my device but PayPal might be an interesting option here. My bank also has a nice app but I’m not sure if it’s something that’s marketable

  * Uber clone – Someone actually asked for this…​ I guess I don’t send out enough emails…​

#### Comments

I’ve removed a lot of the comments especially the positive ones. Thank you for them but I’m not sure those are interesting in this context…​

Of the other comments I tried to moderate as little as possible. If people took the time to write something I’m guessing it’s unclear.

##### Codenameone networking is not supporting real time updates. Would be nice if it did.

I’m not sure I understand this comment.

Our networking does support realtime updates via: WebSockets, Sockets, Long Polling & Push. Otherwise I wouldn’t have even suggested a whatsapp clone…​

Do you mean something like socket.io or pubnub?

We have somewhat out of date support for the latter but not the former. If there is specific demand we can help with that.

##### Why social application?

Because people voted on that last year and [picked it](/blog/launching-codename-one-academy.html).

We try to do what people ask for. I think that even if you build completely closed backend systems a lot of your UI and code would be influenced by the top apps in the market today (e.g. Uber, Facebook, whatsapp etc.) so even if you aren’t building a social app you can learn a lot by following a course on how it’s made.

##### Bluetooth & Hardware Integration

“**My proposal is Bluetooth BLe, Application for sensors and anything that can help normal people with real needs** “

“**Apps with high hardware integration e.g. AR, Bluetooth or development boards for Embedded devices.** “

Those are great but also vague. I agree we need better demos for bluetooth support but We also need mass appeal. We need concrete targets that we can implement.

One of the **huge** problems with bluetooth is that the standard is so big and varied. One person sees bluetooth as file sharing, while another sees it as hardware monitoring and another as a data transfer tool. Add to that the need to get physical hardware that works with this and you end up with something most people will find hard to pick up.

##### AI is becoming the in thing…​ any app that incorporates Artificial Intelligence

I started my professional career working in AI so I’d love to do some more. But moving out of my personal space, the question is what can I teach and how can I help?

I agree, I’d love to have builtin integration for things like tensorflow and other AI related API’s. I’m not sure if it’s something that should go into an app or just a small tutorial. I’d also love to integrate one of the image recognition libraries in the market.

##### ERP/CRM

“**Most of commercial software vendors work on business apps. Not social apps. So I would love to see something resembling CRM, ERP, HCM or anything similar** “

I tend to agree with that statement. The problem is that there is no real uniform standard we can target here in the app space. If you have more concrete suggestions for something we can build I’m open to it. The only thing that comes close to this is Intercom but I doubt it would have been popular in this survey.

To be fair I worked with a lot of businesses and they expect things to look like commercial apps. The app from my current bank looks like Facebook with a feed of “stories” that match my spending & stats from the bank. They even have a chat interface to a teller which should be close to whatsapp. It’s actually pretty nice.  
My point is that business app developers would probably find more benefit from a tutorial on building facebook than one that’s too business specific. E.g. one could take the Uber app in a business and use it to manage their vehicle fleet.

##### Offline Builds

**Let developers enjoy CN1 features! Offline Build server capabilities for free!**

That’s a bit unrelated but I’d like to address that as there are several common misconceptions here.

First is that offline building isn’t free. Offline building is free, the source for building Codename One is 100% open source and all the instructions are publically available. If you don’t want to make the effort to learn how to do it we have a [course module that explains the steps one by one](/blog/use-open-source-build-offline.html).  
There is an enterprise grade [offline build tool](/blog/offline-build.html) but the cost there is mostly support overhead.

Furthermore, the build servers are free to use commercially with reasonable limits. Notice that the Uber app and all our demos fall within the free quota.

The second misconception is that this would be enjoyable. It’s a pain to build offline. E.g. from our enterprise customers who can build offline (as it’s included in their subscription) a very small percentage makes use of that feature. I can build offline and I don’t use that feature.  
It’s painful and surprisingly slower than using the build servers even with our enterprise integration. Even when I want to debug in an IDE I generally prefer sending a build and downloading the source code than going through the offline builder. The only reason we added that feature was for government and other cloud averse entities.

Now back to our regularly scheduled suggestions…​

##### Any Online shopping App

We already have the restaurant app which is a shopping app of a sort in the [building course](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java).

##### Leading the Survey?

**“What’s your #1 Choice for the next App? (after the Facebook app)” is biased.. should be “What’s your #1 Choice for the next App? (after the social app)”**

Damn. You are right. I’ll try to be more careful with those subtle things next time. That was unintentional.

##### Kraken or some other crypto exchange

I don’t use crypto currencies but adding something around them seems appealing. This specific app might be problematic as it’s very poorly rated with a mediocre UI. I’m open to suggestions around this field.

### TL;DR

The winners of for the next apps we will build starting this summer are:

  1. **Facebook**

  2. **Whatsapp**

  3. **Netflix**

**AirBnB** is a strong runner up but we’ll probably do a new survey to determine whether it should be the leader.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
