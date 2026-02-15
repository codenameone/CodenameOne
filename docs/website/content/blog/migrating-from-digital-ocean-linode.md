---
title: Migrating from Digital Ocean to Linode
slug: migrating-from-digital-ocean-linode
url: /blog/migrating-from-digital-ocean-linode/
original_url: https://www.codenameone.com/blog/migrating-from-digital-ocean-linode.html
aliases:
- /blog/migrating-from-digital-ocean-linode.html
date: '2017-01-11'
author: Shai Almog
---

![Header Image](/blog/migrating-from-digital-ocean-linode/under-the-hood.jpg)

We haven’t talked as much in recent years about what it takes to run Codename One. Our infrastructure and backend are pretty complex with multiple pieces working in cohort to make everything feel like a single product. As part of that we work with 5-10 different backend SaaS providers that sell us various services, this might seem like an “odd” statement since the number should be fixed but it isn’t…​

We change provider and some backend providers like Intercom/Cloudflare or AWS you might not think of as proper backend (like Digital Ocean or [Linode](https://www.linode.com/?r=57ffeef90ab49b35f5bdc2a8658a413515d8b3ca)) so the number will fluctuate based on our moving servers from one provider to another and based on what you would consider a provider.

A couple of years ago we moved all our Linux based infrastructure from AWS reserved instances to [digital ocean](https://www.digitalocean.com/). The price was better (even after factoring reserved instance discount) and simplicity/service were superior so there was really no question. As an added bonus digital ocean accepts payments via PayPal making it much cheaper for us as we get paid thru paypal too, this saves on currency conversion and PayPal fees.

We’ve recently become aware of [Linode](https://www.linode.com/?r=57ffeef90ab49b35f5bdc2a8658a413515d8b3ca) and were very skeptical. The pricing was literally half the price of the already cheap digital ocean. Digital ocean hasn’t made any price cuts in over 2 years which is odd in the competitive cloud server landscape. Since we don’t use any of the value added features of digital ocean we decided to give linode a chance.

They start by billing with credit card but they accept paypal once they have a card on record. Setting up the servers was as easy as doing it in digital ocean and once they were up we could literally move traffic to the new servers. Thanks to cloudflare we switched servers while people were working with no downtime and no one was the wiser…​

Once we validated everything worked we deleted the old digital ocean servers. The whole process took a couple of days and cut our Linux server expenses by half (we still pay a lot for App engine, Mac hosting & Windows hosting though). The real cool thing is that the servers seem **much** faster than the old digital ocean ones. This might be because we are on a shared machine that isn’t filled yet so it might change.

The web interface for linode doesn’t seem as polished as the one on digital ocean but it works well and we are very happy with the result as this literally maps to thousands of dollars saved over a couple of days work!

### IaaS vs. PaaS

Our biggest expense is still App Engine, we planned to dump it as soon as we can but it’s hard to remove it from our infrastructure as it is embedded so deep. The story above pretty much proves to me the value of IaaS over  
PaaS when building major apps, it allowed us to cut expenses with almost no effort!

I used to be a big PaaS fan as I can see the value of binding an entire infrastructure seamlessly but with the trouble with had with App Engine over the years I’m now convinced that I was wrong.

### Future Plans

We want to move additional resources from app engine to [Linode](https://www.linode.com/?r=57ffeef90ab49b35f5bdc2a8658a413515d8b3ca) and probably AWS webservices for some of the features. The timeline for this is flexible though as it’s really hard for us to allocate developers to rewrite stuff that already works even if it does save some money. That’s part of the difficulty in running a SaaS company you sometimes need to compromise on these sort of things to keep moving ahead.

We’ll try to find better hosts for our Windows/Mac servers which will allow us to add more servers there and shorten the iOS build times for everyone. So far these fields aren’t as competitive as the Linux hosting fields so we’re not as optimistic about that.

Another thing on our todo list for the past couple of years is packaging everything into containers. We started Codename One before Docker was “a thing” so by the time it got on our radar we had too much invested. Had we used it this move might have been even simpler than it already was.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Mark Korsak** — January 13, 2017 at 5:29 pm ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-22847))

> Mark Korsak says:
>
> This is great to see! Glad to hear the transition went so smooth. Let us know if there’s ever anything we can do for you! – Linode Community Advocate
>



### **João Bastos** — January 13, 2017 at 11:43 pm ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-23259))

> João Bastos says:
>
> Well, my doubts about linode are no longer here… Thank You 🙂
>



### **Ahmed Kamel Taha** — January 16, 2017 at 11:21 pm ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-23286))

> Ahmed Kamel Taha says:
>
> Have you considered jelastic? Mirhousting provide a really competitive prices for PaaS & CaaS only $1.15a month for the cloudlet and they only charge for the real usage with automatic vertical & horizontal scaling  
> [https://jelastic.cloud/deta…](<https://jelastic.cloud/details/mirhosting>)
>



### **Shai Almog** — January 17, 2017 at 5:21 am ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-23295))

> Shai Almog says:
>
> That’s a PaaS, check out what I said about PaaS…
>
> I tried them a few years ago mostly thinking about replacing App Engine with them. I like the idea in general but I’m not sure I want to go there and financially I’m not sure it makes sense. In our case the servers we use are pretty darn sophisticated (build servers) and PaaS might be over simplistic.
>



### **Nkansah Rexford** — January 17, 2017 at 9:57 am ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-23145))

> Nkansah Rexford says:
>
> And you know why I didn’t start with Linode?  
> Because you don’t have a 5$ option! Not everyone needs a 2 GB 1 Core 24 GB SSD2 TB 40 Gbps 125 Mbps on the first day of launch.
>
> I’m currently on a 10$ DO droplet, although I started with 5$. and with the 5$ I could run 3 WordPress instances, 2 Django apps, 1 NodeJS app and 2 static sites, with Nginx as the front-end server, all with an average of about 4 seconds response time with a combined average of 15,000 hits a month. Just 5$.
>
> Although the Linode 10$ offer beats that of DO, I think that 5$ on DO is what attracts, and although I’m yet to face any crippling funds reasons to want to move my entire system to Linode, DO works, and I’ve never had not even 1 second issue with DO since I started using over some 2 years ago.
>
> At just an extra 1$/month, I can top up my DO SSD to 40 Gig from 30.
>
> Fortunately for me, my applications haven’t scaled enough for the price difference to mean a lot. The 5$ got me into DO. Good to know Linode has great options too. It might come handy.
>



### **Shai Almog** — January 20, 2017 at 12:37 pm ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-24123))

> Shai Almog says:
>
> That’s a good point. Our needs require stronger servers so I wasn’t even aware of the 5USD tier.
>
> Just to be clear I really like Digital Ocean too, they were super nice when I asked them about the prices of Linode. They wouldn’t match the prices but after we moved they refunded the remainder balance back into our paypal which I totally didn’t expect…
>



### **noxiouz** — January 25, 2017 at 6:01 pm ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-23003))

> noxiouz says:
>
> I’m still a big fan of PaaS.  
> Could you please write some words about your negative experience?
>



### **Shai Almog** — January 26, 2017 at 6:58 am ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-23176))

> Shai Almog says:
>
> I wrote a bit about it here: [https://www.codenameone.com…](</blog/migrating-away-from-app-engine/>)  
> Generally Google just started overcharging us at a rate that would have bankrupted us very quickly.
>
> The problem is that to be effective PaaS sometimes hides details and in this case we had no way to track the cause of the HUGE expenses. I opened a ticket with Google as a gold customer (400USD per month for that “privilege”) and essentially got an “it’s a problem on your end, we checked in our logs”. Which is essentially a big F U.
>
> This went back and forth a lot but basically the gist of it is you have no way of “knowing” what you are paying for with PaaS as it’s way too big. After migrating away our scaling improved because we could write better code (without all the app engine restrictions) and because we could add affordable servers/CDN.
>



### **John Scarborough** — February 21, 2017 at 10:09 am ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-23090))

> John Scarborough says:
>
> Well this has just changed with Linode offering a $5 option now. Ive already moved some droplets over! I dont need snapshots/block storage for them … Good luck!
>



### **smaugstheswagger** — November 25, 2017 at 10:46 am ([permalink](/blog/migrating-from-digital-ocean-linode/#comment-23827))

> smaugstheswagger says:
>
> Linode is much better than any shared hosting. Shared hosting has lots of issues regarding performance and security, while Linode doesn’t because it is a VPS. Try Linode on Cloudways and you will see the difference in performance. This platform has its own custom stack optimized for performance. Together with Linode, you will experience a significant increase in performance.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
