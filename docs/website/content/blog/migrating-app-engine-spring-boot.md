---
title: Migrating from App Engine to Spring Boot
slug: migrating-app-engine-spring-boot
url: /blog/migrating-app-engine-spring-boot/
original_url: https://www.codenameone.com/blog/migrating-app-engine-spring-boot.html
aliases:
- /blog/migrating-app-engine-spring-boot.html
date: '2018-07-08'
author: Shai Almog
---

![Header Image](/blog/migrating-app-engine-spring-boot/generic-java-2.jpg)

Over the weekend we migrated a huge amount of code to the new build servers. In this post I’ll try to cover three separate things. I’ll explain the architecture/history and process of the migration. What worked, what didn’t work and lessons learned. And finally how this will impact Codename One users moving forward.

I’ll start with a bit of history to explain the background and motivations. We formed Codename One in January 2012. Back then Chen and I worked in full startup mode in an accelerator. Chen never worked in a startup before and was amazed by the pace of work, he quipped that we did more work in 3 months than we did in 5 years at Sun Microsystems!

The build cloud was built during that time but was refined over the past 6 years to suit our growing needs. We made the mistake of picking App Engine for the build cloud as we wanted to shorten the time to market while keeping the implementation scalable. The fact that Google offered free hosting credits for startups was also a big incentive.

The build cloud was built as a monolith, this isn’t a bad thing. In fact Martin Fowler specifically advises that people [start with a monolith architecture first](https://martinfowler.com/bliki/MonolithFirst.html). The cloud server doesn’t actually do the builds, for that we need dedicated standalone servers (e.g. Macs, Windows machines etc). It essentially orchestrates all the disparate pieces so they will work together. In the past it handled a lot more but as the years went by we chopped out pieces one by one to move into a microservices architecture e.g.:

  * Push
  * Crash protection
  * Build file submission
  * Build server web UI

And more. We tried to remove everything since app engine is just SO bad but still quite a few things remained in the cloud servers:

  * User authorization/signup/activation
  * Build orchestration – submit, status, server assignment etc.
  * Billing – we don’t store billing information but PayPal notified the old servers on payment
  * Some of the transactional mail infrastructure

The first two are big pieces that include a lot of code. They are also deeply tied to everything else.

When we started off with App Engine we believed Google’s claims that it supports JPA. To keep our code portable we used JPA so we’d be able to migrate away. As we discovered JPA support on App Engine is a sad joke. Basic functionality didn’t work and failed painfully, ironically these failures occurred only when the business scaled so instead of smooth performance degradation we got downtime.

As we moved ahead we rewrote a lot of JPA code to use the App Engine entity API and memcached. Thankfully we saved a lot of the old JPA code.

Over the past year updating the App Engine deployment became impossible as Google blocked the old plugin it used to support App Engine. The approach of migrating to a completely new project structure is poorly documented and seems like a huge risk as things might fail badly. So we had no choice.

### How does it Work?

We’ve worked a lot with Spring Boot while developing [the courses](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) and [upcoming book](https://www.codenameone.com/blog/uber-clone-book.html). The speed of development and ease of use is at a different scale altogether.

With that in mind we decided to do a live migration to a Spring Boot server that would work as a drop in replacement to app engine. To keep scalability in place we decided to use cloudflare (as we do on the main website). It makes scaling remarkably easy.

Because we already extracted the build UI from app engine in the past and now host it as part of our website, we have a clear API to the backend server. With that we could change all the calls that go into the app engine server and just point the exact same calls into the new cloud server. The cloud server decides whether it should handle a call locally or ask App Engine to perform the call for it. In this way we added a new layer for existing users, but it should be 100% compatible and shouldn’t fail.

The reason we had to take this approach is due to the plugins, since they are installed on the end user machines some might still point at the old App Engine. We don’t want all builds to break suddenly. We’d like users to move away gently from the old App Engine deployment.

Normally rewriting from scratch would have been easier but because we wanted as little disruption as possible we tried to setup the new Spring Boot server to be 100% compatible so we had to import code and try to convert the old mishmash of servlets with no tiers to a proper architecture that separates tiers properly. It still looks a bit messy as the original wire protocol is messy. But now that this is behind us we’ll be able to revisit this and improve a lot of the missing pieces.

### What went Wrong?

A lot of things failed in unexpected ways.

The biggest problem we ran into in production was due to cloudflare. Cloudflare blocks DDoS attacks and one of the tricks it uses is blocking all traffic that doesn’t include the `User-Agent` header. A lot of our code uses the `URL` class and doesn’t add that header. So basic things worked great in the development environment but failed in production.

We migrated a lot of the old numeric keys as we moved from the datastore to SQL. So the new String based keys worked reasonably well but a couple of JavaScript client side functions used syntax such as: `doThis(id)`.

This seemed to work but just failed silently when the key was a string. We had to add quotes to those methods to get it through. Since there are so many nuanced features in the platform we just missed testing this.

Some OTA and install features failed in production. This relates to relatively hidden/obscure code in the app engine implementation that we completely missed. In fact we used JSP for a couple of features in the old app engine. While Spring Boot supports JSP it has some deployment limitations so we just translated that code to a standard webservice call. Not an ideal solution but since there was so little code to port it was something we could address.

After working a bit Spring Boot would fail with RAM errors. Turns out we need to explicitly set the `Xmx/Xms` flags in Spring Boot using a conf file. We didn’t run into it in previous deployments as we didn’t do some of the heavy IO that we do here.

### What Worked Great

So many things “just worked”!

The migration to SQL was mostly smooth and included only a few pitfalls/schema changes. Being able to use proper SQL instead of datastore is a huge step forward. It’s so much faster we don’t need memcached and get better performance to boot!

The truly amazing thing is the queries and 3rd party tools. This has boosted our ability to address issues tremendously.

Despite the `User-Agent` issue cloudflare is a huge asset. It makes caching repeated queries trivial. Better yet, since we now proxy some downloads through the servers we can get faster/more reliable downloads thanks to cloudflare.

I can’t sing the praises of Spring Boot enough, it makes this trivial. It has it’s pain points (unreadable huge stack traces) but the ease of development is amazing. We manage our own infrastructure now through IaaS. It’s easier, faster, cheaper and scales better than the previous PaaS deployment. Four out of four criteria.

While we didn’t test scaling to the full extent so far CPU utilization is flat/low. This architecture would probably scale much better than app engine ever did. Google sells App Engine as a “Google Scale” solution but anyone who worked with it will know that this only applies if you can spend “Google Sums” to pay for that. App Engine tries to scale by adding computing resources instead of just slowing down.

That means that if you have 10k active users you’d pay for a lot of servers to handle them. Our current solution can handle 10k concurrent users easily. It would slow down but wouldn’t crash. It would still be cheap thanks to [Linode](https://www.linode.com/?r=57ffeef90ab49b35f5bdc2a8658a413515d8b3ca).

We also took the opportunity to move most of our transactional emails to mailgun. So if you get an email from us you will notice it uses a different domain. One of the big problems developers had with signup in the past was due to corporate inboxes relegating us to spam. We made some bad technical choices assuming SendGrid can help us fix these issues. This probably isn’t SendGrid’s fault as much as it’s our lack of understanding in this field.

We decided to start a new leaf with a new domain for the emails. We didn’t move everything there and I’m not sure if we ever will as I’m concerned about deliverability. Regardless this is seems to be a good move as we have 100% deliverability so far.

### How could this Impact You

I already discussed some of these [here](https://www.codenameone.com/blog/new-build-cloud.html). But there is one additional entry in the pile: IPN.

We handle our subscriptions via PayPal. We’d love to add other options but there are logistical issues with global deployments for all of them. We don’t want any billing data on our servers as we don’t want to deal with that complexity.

PayPal billing can work via a solution called IPN which means paypal invokes us on every billing. That way we can update our user database based on payment received. So far so good.

Unfortunately you can’t change an IPN address after it was set. So existing subscribers still point to the old app engine URL. We have a workaround of polling app engine for subscription level for existing subscribers. This only impacts current paying users and might cause a situation where your subscription appears to have 2 days left even if it has more.

We plan to migrate the project in app engine so the IPN will be the only remaining piece and it will point back to our server. To do that we’ll need to bring app engine offline and set it up with a new project. That will take time. So for now this workaround is in place.

We plan to disable app engine builds by the end of the month. That means you would need to update your plugins to the latest in order to build. If you don’t you would get an error. We would still not remove app engine as a few other features are harder to migrate over.

### What’s Next?

Now that this is in place we can finally implement some cool features we’ve been craving…​ Higher build quotas for free users including features such as push notification etc.

All of these would be coming in the next few months…​

I don’t want to announce dates as I’m still working on the book and we need to push out Codename One 5.0 but it’s coming and hopefully before 5.0 which is now slated for September.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Steve Nganga** — July 12, 2018 at 11:26 am ([permalink](https://www.codenameone.com/blog/migrating-app-engine-spring-boot.html#comment-23807))

> Steve Nganga says:
>
> Good read… just a quick one. much has been said on vertx on the backend, is this something you ever thought of using as opposed to springboot….
>



### **Lukman Javalove Idealist Jaji** — July 12, 2018 at 1:18 pm ([permalink](https://www.codenameone.com/blog/migrating-app-engine-spring-boot.html#comment-23993))

> Lukman Javalove Idealist Jaji says:
>
> Fantastic read and kudos to you and the team.  
> “Higher build quotas for free users including features such as push notification etc”…  
> Does this mean we may get push features with basic subscription at least?
>



### **Shai Almog** — July 13, 2018 at 5:00 am ([permalink](https://www.codenameone.com/blog/migrating-app-engine-spring-boot.html#comment-23857))

> Shai Almog says:
>
> I know quite a few people that use Spring Boot and don’t personally know anyone that uses vertx. It looks interesting but there are a few things that make me think vertx isn’t the right solution for us:
>
> – JPA – I like JPA. It exposes SQL etc. but doesn’t seem to promote JPA. I’m sure I can integrate hibernate etc. but that would already mean more work than Spring Boot.
>
> – It “isn’t opinionated” – I think that’s conceptually wrong. I don’t think a framework can be everything to everyone and still be good. You need to have opinions and optimize to those opinions otherwise you provide a sub par experience to everyone
>
> – Legacy – one of the cool things I have with Spring Boot is support for legacy code and features. I could just stick an old servlet “as is” without rewriting the very sensitive code I had there. It’s really convenient
>
> – Deployment – deploying a Spring Boot app in linux as a service is amazing. It generates a special JAR that works as a Linux service… The whole story “just works”.
>
> Again, I’m writing this without spending one minute playing with vertx so I might be completely wrong.
>



### **Shai Almog** — July 13, 2018 at 5:04 am ([permalink](https://www.codenameone.com/blog/migrating-app-engine-spring-boot.html#comment-23865))

> Shai Almog says:
>
> This is still in tentative so keep that in mind. These plans can change as we refine things.
>
> Our goal is to offer 100 free push messages per month for everyone so you can test the push functionality for free. We will treat basic/free users in the same way when it comes to push.
>
> The goal is to provide an incentive based system for promoting Codename One and increasing the quotas accordingly. I think push will still cap at some level but we haven’t decided the full details yet.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
