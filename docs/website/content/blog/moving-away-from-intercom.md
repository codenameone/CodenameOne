---
title: Moving Away from Intercom
slug: moving-away-from-intercom
url: /blog/moving-away-from-intercom/
original_url: https://www.codenameone.com/blog/moving-away-from-intercom.html
aliases:
- /blog/moving-away-from-intercom.html
date: '2018-06-26'
author: Shai Almog
---

![Header Image](/blog/moving-away-from-intercom/looking-forward.jpg)

A few years ago a consultant convinced us to integrate Intercom into our website. In retrospect this was a mistake which I’ll discuss in more depth below. We are migrating away from Intercom right now…​ That means that if you have an email address or ongoing chat history with us in Intercom it might get lost!  
Worse. If you unsubscribe this might also get lost due to the migration process (sorry about that!). We’re moving to a new far better system ([crisp](https://crisp.chat/en/)).

### What’s Intercom

It’s the chat widget at the bottom right portion of the website.

It also handles the emails we send out offering help or pointing out information about Codename One. We use it to send out email announcements etc. It’s useful because when you reply to an email or chat it all goes to one place so we know who we’re talking with (sort of, it doesn’t do a great job). E.g. if you use the chat wizard then answer through email for us it’s one interface.

### Why does it Suck

Intercom is a **bad** product and to make things worse it’s expensive. It does the basic things reasonably well but it’s UX and UI are very badly designed for us and our audience.

Despite Intercom’s many shortcomings we were lazy on the issue of migration as there is always something more important to do. Thankfully Intercom decided to change their pricing model so we’d pay 5x the already inflated price which was the perfect incentive for us to discover [crisp](https://crisp.chat/en/) which is far cheaper and seems like a better product altogether.

With this you should already see the new chat widget below and should be able to interact with us there…​ You can also check out the new <https://help.codenameone.com/> website that was generated as apart of the process. Hopefully we’ll strengthen it with a bigger knowledge base.

### Where Next

Unfortunately this is just the tip of the iceberg. Intercoms code is embedded deep into the backend systems. The problem is we still have some app engine servers running and it’s really hard to update them as Google effectively killed off the system we were using. So we are finally doing what we procrastinated on for 3 years and removing app engine completely from our stack!

This is a **HUGE** move, I can’t over state it. All our user logs and everything is in app engine. That might mean that if you had an old “dormant” account that you haven’t used in years it might get deleted in the migration as we won’t be able to migrate it. That’s not a big deal since such accounts would typically be free accounts and you could just re-create that account. The bigger benefit is that we would be able to implement a lot of the features we always wanted to and couldn’t because of the problems in our backend!

You might notice some kinks in the migration let us know in the comments or the chat if things don’t work well. Setting up the automated emails from scratch will be a nightmare but it has to be done.

This might delay some things such as the book release, but we are making progress there…​

I’m sending out an email with some more details there if you are interested. It will be the first email to go out with the new [crisp](https://crisp.chat/en/) system so hopefully this works out nicely. If you don’t get it let us know in the chat and we’ll try to track that with you.

### Codename One 5.0

This isn’t directly related to that but with this overhaul the decision is even more important. We decided to bump Codename One 5.0 to September 2018 instead of the current July release date.

We think features such as JDK 9/10/11 support is crucial with the new JDK release cycles. I’d also like Codename One to work with OpenJDK which is now a more stable target. Since these things require a lot of testing over time we think this new release date is crucial.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — June 28, 2018 at 6:51 am ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-24140))

> Diamond says:
>
> I use [https://www.livechatinc.com](<https://www.livechatinc.com>). You might also want to check them out and compare features and cost with crisp.
>



### **Shai Almog** — June 29, 2018 at 6:20 am ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-23808))

> Shai Almog says:
>
> I wasn’t familiar with them. Looking at this it seems they price per-agent which is slightly more expensive for our use case. The problem in this field seems to be the HUGE number of companies doing roughly the same thing. I don’t understand how Intercom can raise their prices 5x when they were already FAR more expensive with fewer features…
>



### **Francesco Galgani** — July 23, 2018 at 5:19 pm ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-23891))

> Francesco Galgani says:
>
> About the OpenJDK support you mentioned in this article, have you read this news on the Oracle web site?  
> [https://www.java.com/en/dow…](<https://www.java.com/en/download/release_notice.jsp>)  
> I suppose that OpenJDK will be the best response.
>
> And now a difficult question, because there is few information: in the next years, do you think that the applications developed with Codename One will be able to run on Google Fuchsia, the replacement of Android?
>



### **Shai Almog** — July 24, 2018 at 4:11 am ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-23840))

> Shai Almog says:
>
> Yes that really sucks. That’s why we consider the move to OpenJDK/JDK 11 as very high priority…
>
> Fuchsia will never happen. There are millions of Android apps, hundreds of thousands adaptations and vendor customizations. Google knows it. Fuschia is a research project not an actual attempt to replace Android (despite some misleading nonsense from the tech press/PR).
>
> The only thing of interest in Fuschia is the kernel, you can replace Androids kernel and keep 90% of the compatibility for apps and vendors. Mac OS did it when they migrated from Power PC to Intel. It makes the most sense.
>
> Having said that, since we run on a lot of very different architectures if something that historically never happened would happen we could target that too.
>



### **Caterina Bassano** — July 24, 2018 at 11:12 am ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-23976))

> Caterina Bassano says:
>
> Interesting! I am curious: what makes Intercom a bad product and what made you choose Crisp over others?
>



### **Shai Almog** — July 25, 2018 at 4:27 am ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-23982))

> Shai Almog says:
>
> Lets start with a couple of things I liked in Intercom which are still unavailable in Crisp:  
> – Threaded discussions are/were handled better  
> – Analytics on email sending is better
>
> The pricing difference is amazing, AFAIK no one is remotely close to Crisps “ultimate” tier in terms of pricing. I was referring to Intercoms pricing without all its packages to make it equivalent to Crisp which would have put the price difference at x20/x30…
>
> The price made us move but after working with Crisp for a while now I think it’s also a technically superior product. This is mostly because these guys are a startup and they are “hungry” unlike Intercom which is so full of investor funding it can’t function. Crisp listens to users whereas Intercom doesn’t really care unless you pay 5-6 figures. We paid mid 4 figures (annually) and they didn’t care to implement anything we asked for. They changed pricing to existing customers with 1 month of notice… Who does that?
>
> Here are the features that Crisp does way better:
>
> – User account – their insight into users is much easier to read with color coded events placed in a sensible location. It’s far more readable, I can now look at a specific user and understand what’s going on where in Intercom this was a mess.
>
> – Mobile – Intercom was unusable on mobile. There was no way to disable it there. Crisp can be disabled on mobile but is usable there to begin with. It doesn’t annoy. People would complain about our site a lot mostly because of Intercom.
>
> – Gifs – Intercom added the most annoying feature in history: gif support. Users started sending us animated gifs. That’s stupid and redundant. We aren’t targeting children… Our chat is there to help not entertain. Crisp has a “while you wait play a game” feature, but it can be turned off. They let us adapt the way the widget looks/behaves to the type of business we have. BTW I commented about the gif issue in their public blog post and a lot of users chimed in complaining about it. Intercoms solution was swift and simple: they disabled commenting on future blog posts…
>
> – I love their visitor view, it has given me a level of insight into our site visitors that neither Google Analytics or Intercom have given. It might be because we integrated Google Analytics badly though.
>
> – The tools are very hacker friendly, markup and REST API’s. Developers are responsive and proactive in their attitude.
>
> The way I see it, Crisp is moving to answer customer needs. Intercom is moving to increase monetization. I don’t think anyone should use Intercom regardless of the price. It’s a company culture issue that’s probably unfixable.
>



### **Shai Almog** — July 25, 2018 at 5:47 am ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-23955))

> Shai Almog says:
>
> I forgot to mention one HUGE conceptual problem with Intercom… They separate leads from users and charge the same for both.
>
> A lead is a person who visited the site but you don’t know who he is. A user is a logged in user. If a lead enters his email address into intercom he’s still a separate lead and isn’t merged with his user account. Supposedly for security so a person can’t impersonate a user… The reality is that you end up with multiple redundant accounts and some of your user support goes into the lead while other goes into the user. That means conversations are lost and invisible. The only real purpose of this is to overcharge on leads, we literally took time every month to delete leads to keep our price manageable as leads are redundant… But we also deleted user data as a result.
>
> One Intercom user who was communicating with us was complaining about something. When our agent tried to understand what he was complaining about he asked her to look in his chat history. But it was hidden in a lead and was impossible to find in intercoms UI. He didn’t believe our agent who said she can’t see the history he was talking about…
>
> Again, this boils down to a company that over monetizes its users.
>



### **Francesco Galgani** — August 22, 2018 at 4:05 pm ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-23882))

> Francesco Galgani says:
>
> I have no experience of Crisp or other similar services. If you think that Crisp is a good service, my question is if it can be integrated in a Codename One app. I’m developing a complex app: it could be useful is there is something ready to be used to get feedbacks and support requests from users.
>



### **Shai Almog** — August 22, 2018 at 4:21 pm ([permalink](https://www.codenameone.com/blog/moving-away-from-intercom.html#comment-24068))

> Shai Almog says:
>
> I agree. We plan to integrate crisp as a cn1lib. This should be trivial to accomplish but I don’t have an ETA for it right now. It’s very much on our “TODO” list.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
