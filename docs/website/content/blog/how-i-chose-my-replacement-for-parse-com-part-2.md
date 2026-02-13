---
title: How I Chose my Replacement for Parse.com Part 2
slug: how-i-chose-my-replacement-for-parse-com-part-2
url: /blog/how-i-chose-my-replacement-for-parse-com-part-2/
original_url: https://www.codenameone.com/blog/how-i-chose-my-replacement-for-parse-com-part-2.html
aliases:
- /blog/how-i-chose-my-replacement-for-parse-com-part-2.html
date: '2016-08-08'
author: Shai Almog
---

![Header Image](/blog/how-i-chose-my-replacement-for-parse-com-part-2/parse.com-post-header.jpg)

You probably recently received the “Next Steps from the Parse Team” newsletter in your inbox in which you were urged to take immediate action as it pertains to migrating your Parse.com-hosted apps. Or at least, you’re aware of the ultimate January 28, 2017 deadline for migrating your apps. While you should take such reminders seriously, there’s no need to panic. In this article, I share my experience with different Parse Server hosting backends and my choice after applying the principles outlined in [Part 1](/blog/how-i-chose-my-replacement-for-parse-com.html) of this series. Read on!

### Applying the principles

As I mentioned in Part 1, at the time that the imminent shutdown of Parse.com was announced, I had a few apps on Parse.com:

  * Parse4cn1 which is actually a Codename One wrapper for the Parse REST API but was hosted on Parse.com in order to test the library.

  * Two prototypes both in development at the time Parse.com’s shutdown was announced. (These apps still need to be migrated and this post is an extended version of my input to my clients as I help them choose a new backend.)

Let’s begin by applying the principles outlined in Part 1 to parse4cn1. (Feel free to jump to the next section if you’re itching to see the backends I investigated.)

  1. Self-hosting vs. Parse Server hosting providers: For parse4cn1, I considered both self-hosting and Parse Server hosting providers. In fact, I’m actually using both options, given the fact that I need parse4cn1 to work properly with various Parse Server-based backends. More on the backends shortly.

  2. Vendor lock-in: This was not an issue for parse4cn1. The goal of parse4cn1 is to provide a wrapper for Parse Server so vendor-specific features are not quite interesting in this context. And like I suggested in Part 1, the chance of vendor lock-in is directly proportional to the amount of vendor-specific features used (none in the case of parse4cn1 so no risk of vendor lock-in).

  3. Freemium: Contrary to the general principle about being wary of overly generous freemium packages, I want to be able to maintain parse4cn1 for free. So in this case, freemium is a plus. However, that does not detract from the fact that you should beware of overly generous freemium models.

  4. Parse Server is not a clone of Parse.com: The main implication of this for me was the need to have a stable test environment in which I could easily switch Parse Server versions and debug issues. OpenShift turned out to be a great solution for this as I explain later in the article.

  5. Making parse4cn1 future proof and migration-friendly: For parse4cn1, this means that it needs to keep working with Parse.com (until January 28, 2017) and also work with as many Parse Server providers as possible. To achieve that, I migrated parse4n1 to and tested it on multiple Parse Server backends. Let’s now look at those backends.

### Test driving various Parse Server backends

#### OpenShift: Great for development but not yet production ready

OpenShift is a PaaS on which you can run various applications. Building up the work of Ionut-Cristian Florescu (alias [“icflorescu”](https://github.com/icflorescu)), OpenShift expert (at least in my eyes) [Anatoly Tokalov](https://plus.google.com/101404724521695639107/about) (alias [“antt”](https://github.com/antt)) has created a one-click solution to setup a Parse Server on OpenShift. I collaborated with him to integrate Parse Dashboard into this solution. So, with a few easy steps, it is now possible to set your own Parse Server backend for free on OpenShift! You can read more about the steps on [Anatoly’s blog](http://www.anttdev.com/2016/02/how-to-install-your-own-parse-server-on-openshift/).

Having your own Parse Server sandbox can be tremendously useful as you migrate. You can easily switch between different Parse Server versions e.g. to track down bugs (that’s how I found [this one](https://github.com/ParsePlatform/parse-server/issues/2103), for instance). You can try different configurations out and get an initial feeling as to whether Parse Server is mature enough for your app. Regardless of what Parse Server backend you choose, I highly recommend an OpenShift sandbox.

Be careful though! As pointed out by Anatoly in the same [blog post](http://www.anttdev.com/2016/02/how-to-install-your-own-parse-server-on-openshift/) (see Update 1 and Update 2 as well as the comments on the post), **Parse Server on OpenShift is not yet production ready!**  
Moreover, at the time of writing, it is not (yet) possible to use the migration tool provided by Parse.com to migrate your apps to OpenShift because the mongodb instance in OpenShift is not accessible externally.

So to summarize: OpenShift is a good place to test things out and arguably faster to set up than on your local machine. And it is free. However, at the time of writing, it is not production ready. I’m currently using OpenShift for parse4cn1 maintenance. The [automated regression tests](https://travis-ci.org/sidiabale/parse4cn1) currently run against OpenShift and against Parse.com (for compatibility checks). I also manually tested parse4cn1 against two Parse Server hosting providers – back4app and sashido.io. Let’s have a look at them.

#### back{4}app: An attractive solution

[Back4app](https://www.back4app.com/) is one of the Parse Server hosting providers looking to fill the void created by Parse.com. They use the Open Source Parse Server and Parse Dashboard as core and provide free and paid services around it with a pricing model quite comparable to Parse.com. The last part of the previous sentence caught your attention, didn’t’ it? Perhaps you’re thinking, “Parse.com shut down probably in part due to financial reasons and back4app has a similar freemium-based model yet you say it could be interesting?” Hang on, let me explain. And by the way, I have no affiliation with back4app, sashido.io, OpenShift or any other backend provider; all I’m going to say next is completely my personal opinion.

Yes, I think back4app is promising and here’s why:

  * They seem to understand very well that former Parse.com users are disgruntled and sceptical. They are open about this; see, for example, these back4app blog posts: ([i](http://blog.back4app.com/2016/05/03/parse-alternative/))([ii](http://blog.back4app.com/2016/07/07/baas-market/)). Moreover, their comparisons of different Parse alternatives (e.g. this one on [Parse Server](http://blog.back4app.com/2016/06/15/firebase-parse/) vs. Firebase) seem quite balanced which again is a sign to me that they understand the situation and are not just reacting to the opportunity offered by Parse.com’s shutdown without thinking things through.

  * They also are clear about your data: It’s yours and you can have it anytime according to back4app this FAQ answer. I’ve not tried exporting my data yet though so I’m only working with what is said. Check it out for yourself!

  * They stick quite closely to the Parse Server offering – you can see the Parse Server and Dashboard version your app is running on and they clearly state what services they’ve added that are not yet supported (e.g. background jobs). This helps reduce the chance of vendor-lock in.

  * They’ve made some improvements that could make life easier, for example, the option to upload cloud code via a Web interface. I really hated that CLI tool from Parse.com and the fact that I had to push all my changes to test them. With back4app, you still have to “push” your changes to test them but you can simply upload your .js file via a web interface. I find that a good step in the right direction.

  * Their customer service is friendly and supportive. At least that was my experience when I contacted them about this [bug](https://github.com/ParsePlatform/parse-server/issues/2103). I encountered it for the first time when testing parse4cn1 against back4app and I thought it was in their system. But they confirmed that they use the Parse Server code as-is so I looked further and found that the bug was actually in Parse Server. During the process of debugging, they were supportive and very responsive via live chat and email.

The big question mark for me is back4app’s freemium offering. Not as generous as Parse.com’s but in my opinion still somewhat too generous for comfort. As at the time of writing back4app offers 10 Requests/s, 50 K Requests/mo, 5 GB File Storage, 1 GB Database Storage, 1 cloud code job (read: background job) for free. Note though that the pricing page gets updated (read: tightened) from time to time so it could be that things have changed by the time you read this.

I still find the current freemium package too generous as a lot of apps can run comfortably without ever needing to upgrade to a paid subscription which is not a good foundation for continuity in my opinion. Of course, I don’t know their business model. It could be that they want to use a decent freemium offer to attract as many users as possible and then “raise the heat”. That won’t be such a bad idea. Recall that there’s no free lunch. It’s better you pay a little and have a service that stays alive than to get a lot for free and face another shutdown!

#### Sashido.io: An interesting alternative

In many ways, [Sashido.io](http://www.sashido.io/) (previously Parseground) is similar to back4app. However, there are a few significant differences:

  * Unlike back4app, Sashido touts the “freemium is bad” slogan. At the time of writing, their home page has the following (emphasis added): “No limit of monthly requests & req. per second, storage, database and file transfer. And the best part? It starts from $4.95/mo. Better than free.”  
They even wrote [an article](https://medium.com/@sashidoio/dangers-and-benefits-of-the-freemium-model-what-did-we-learn-out-of-parses-shutdown-79becb215c84#.ck7dsersi) on why freemium is bad. While that article makes sense, their approach of simply a 14-day trial and no freemium package is somewhat extreme in my opinion. By the way, I got a 2-month free trial due to early subscription. Often mobile projects start off as modest ideas and I don’t know many people who would be willing to incur monthly costs for a backend when they are not sure if their MVP would see the light of day. In that sense, I’m as opposed to overly generous freemiums as I am to no freemiums.

  * Sashido uses a custom dashboard instead of Parse Dashboard. While their dashboard is as intuitive as the Parse Dashboard and is designed very much to look like the Parse.com dashboard, this could be an issue in the future. As the open source Parse Dashboard gets enriched with new functionality, there is no guarantee that sashido will keep up or make the same choices with their dashboard. Something to definitely consider carefully if you choose sashido.

  * Sashido does use the open source Parse Server though. At least that’s [what they told us](https://twitter.com/sashidoio/status/758586000717279232). However, it’s useful to note that their dashboard does not (yet) mention what version of Parse Server they are running. I find that useful information and I hope they’ll add it soon. (Note that the Parse Server and dashboard version are present in the open source Parse Dashboard and by extension in back4app as well.)

  * As at the time of writing, there is no terms of service link on sashido.io’s homepage or any mention of what happens with your data if you opt-out. They don’t seem to be very open about the whole vendor lock-in fear. It might be a small omission on the website or it could be that the information is hidden somewhere and I didn’t look properly. But in any case, I expect this information to be prominent and easily accessible because it is at the center of the discussion and should not be relegated to the background.

Sashido does offer some features that might be interesting to you such as [deploying cloud code](https://blog.sashido.io/how-to-set-up-cloud-code-on-sashido/) via a private Github repo and a [file migration tool](https://blog.sashido.io/parse-migration-in-a-click/) from Parse.com to AWS (available to all Parse.com users but only useful if you want to store your files in Amazon S3).

### Moving forward

There are a bunch of other services like back4app and sashido each having their pros and cons. I’ve not investigated all of them and honestly have no immediate plans to. parse4cn1 has successfully been tested against back4app and sashido as stated in this article. so they are both potential options. If I have to choose though, based on the current state of affairs, I’d go for back4app because I find their service overall more appealing and the risk of lock-in less than with sashido. I think this holds in general for small and medium-sized apps. In any case, I’ll always maintain an OpenShift sandbox in parallel for development and debugging.

One thing that both back4app and sashido are missing at the moment is the option to switch versions of Parse Server per app. The way it works now is that they decide at some point to upgrade to a particular Parse Server version and developers have no say in that. It would be super cool if one could choose the Parse Server version to run a specific app on and/or decide when to upgrade…​ And this is not an unrealistic dream! In a sense, the fall of Parse.com has opened the door to many new opportunities…​

### The future is brighter with Parse Server

Beyond the shock and fury at the shutdown of Parse.com, there is a silver lining and a bright future. The decision by Facebook to open source Parse Server and Dashboard might prove to be a game changer in the MBaaS space. It offers developers a decent backend out of the box and endless possibilities to customize and improve it. MBaaS is still somewhat of a gamble and skeptics suggest that an in-house solution is always the best. With Parse Server, you can get the best of both worlds! For instance, you can start off out with a Parse Server hosting provider and if your app becomes a hit and your needs necessitate an in-house solution, you can then smoothly migrate your Parse Server. If you app is one of the thousands that doesn’t see the light of day, you won’t have lost much in backend investment (especially if you chose a provider with a freemium model).

Of course, the main thrust of this two part series has been to help you make a wise choice now and avoid another shutdown. But the truth is, even if you face another shutdown, the impact will be way less than Parse.com’s shutdown because you’ll have good alternatives and if you followed my advice, your app users won’t even notice that you switched backends again. That is the power of the open source solution offered by Parse Server!

### Final word

In this article, I’ve presented three Parse Server solutions, outlining their main pros and cons and clearly indicating my preferences. Note that I’ve deliberately not addressed “how to migrate” as there is a lot of useful information on that subject in the Parse.com [migration guide](https://www.parse.com/migration) as well as on the website of each Parse Server hosting provider. As such, there is no point in repeating what you can readily find yourself

Of course, your app might differ in scope and purpose from the example that I’ve used in this article. Nevertheless, I’m pretty sure that if you follow my line of thought and apply the guidelines I’ve outlined in this series thoughtfully, you’d find a good replacement for your apps as well. I’ve shared the facts as well as my opinions with you; it’s now up to you to double-check and make your own choice; in that sense you have the final word!

If you know of other attractive solutions, have success stories/useful tips to share and/or disagree with my reasoning/opinion, do not hesitate to leave a comment! Should the information in these articles and complementary ones on the Internet not suffice, feel free to [get in touch](/cdn-cgi/l/email-protection#21484f474e61524c4052490c4842550f424e4c) with us at SMash ICT for personalised consultation or contact me personally at chidi [dot] okwudire [at] smash-ict [dot] com. I wish you the very best with choosing your own replacement for Parse.com and would be glad to assist you in any way possible!

### References and interesting reads

[1] Anatoly Tokalov (February 19, 2016). How to install your own Parse Server on OpenShift. Retrieved from <http://www.anttdev.com/2016/02/how-to-install-your-own-parse-server-on-openshift/>  
[2] Marian Ignev (April 28, 2016). Dangers and benefits of the freemium model — What did we learn out of Parse’s shutdown? Retrieved from <https://medium.com/@sashidoio/dangers-and-benefits-of-the-freemium-model-what-did-we-learn-out-of-parses-shutdown-79becb215c84#.ggsb3gf6l>  
[3] Alysson Melo (July 7, 2016). Challenges and Opportunities in the BaaS Market. Retrieved from <http://blog.back4app.com/2016/07/07/baas-market/>
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Marian Ignev** — September 25, 2016 at 4:31 am ([permalink](https://www.codenameone.com/blog/how-i-chose-my-replacement-for-parse-com-part-2.html#comment-22973))

> Marian Ignev says:
>
> Hi Chidiebere Okwudire, very interesting article, great job, But … let me introduce myself. I’m part of the SashiDo team, so I want to share some thoughts with you … if you allow me of course 🙂
>
> The things about SashiDo are wrong! Sorry … but they just are not true.
>
> – SashiDo Dashboard is 100% based on the OpenSource Parse Dashboard (I mean fork) and it’s improved by SashiDo. You had only to say that you write an article about SashiDo and we would love to tell you all the details 😉
>
> – About my article for the Freemium business model please correct me but the title I think is “Dangers and benefits of the freemium model ” … not Freemium is bad thing … is that correct? 🙂 The only place that I used the word BAD was the there I told that …. Shutting down of good services like Parse is a bad thing … and yes I’m sure that a lot of Parse customers will agree with me now 🙂
>
> In other hand as an entrepreneur I think that danger is a good thing … it’s exciting and keeps you focused, all the time.
>
> The last thing I want to share with you is that the free trial is not freemium 🙂 Just an example … You can do a Test Drive before you buy a car, right? To understand what’s the difference between the freemium and the free trial I have a challenge for you: Please thing about implementing a freemium model for the auto motor industry 🙂
>
> I just wanted to share the true — no hard feelings here.
>
> All best,  
> Marian
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fhow-i-chose-my-replacement-for-parse-com-part-2.html)


### **Chidiebere Okwudire** — September 29, 2016 at 11:00 am ([permalink](https://www.codenameone.com/blog/how-i-chose-my-replacement-for-parse-com-part-2.html#comment-22965))

> Chidiebere Okwudire says:
>
> Hi Marian,
>
> Thanks for your response. Great to see that you’ve taken the time to point out some things that you consider incorrect in my analysis! Let me respond:
>
> You suggest that what I say about Sashido is wrong and you go ahead to point out a few issues like the dashboard and freemium model. I’ll respond to those shortly but just to be clear, I mentioned other things about Sashido and I assume that since you didn’t say anything about those, they are correct. If not, please feel free to refute anything else that you feel is wrong in my article. My goal with these articles is to help developers make a right choice so I’m 100% in favor of accurate and correct information and input from folks like you is very much appreciated.
>
> Now to your remarks:
>
> 1\. In the article, I state that “Sashido uses a custom dashboard instead of Parse Dashboard”. This is based on an inspection of the Sashido dashboard when I still had a free trial account a few months ago. I don’t know how it *looks* now but at the time I wrote the article, the differences from the default Parse Dashboard were impossible to miss (I might even have a screenshot somewhere) – Parse Server and dashboard versions were not reported, the menu items were somewhat different, etc.
>
> You’ve clarified that your version is forked from the open source Parse Dashboard and that’s nice to know. However, my point was (and remains) that with such a highly customized dashboard (albeit based on Parse Dashboard), there’s still a risk of lock in. Would you disagree? For example, a user wanting to switch from Sashido to say back4app would have to (at least) deal with a different looking dashboard with different options, etc. And I have a strong feeling there’s more that’s different than just the look-and-feel. Maybe you’d like to comment on that so readers can have a more balanced picture. Like I said, my free trial of Sashido has ended and I don’t plan to make another trial account to point out all the differences. The interested reader can do this for themselves 🙂
>
> 2\. About the Freemium model, you’re right that my wording in the article might incorrectly give the impression that the author is completely against Freemium whereas the article does *try* to compare both sides. I emphasize “try” here because in my opinion, the article is baised in favor of the dangers of freemium than the benefits. Again, let the interested reader be the judge. Let’s not get too distracted by the article though because I think the more important point in this context is: Is Sashido a freemium service or not? What’s your take on that? Note that [http://www.sashido.io/#Pricing](<http://www.sashido.io/#Pricing>) still include the line that I quoted in my article: “It starts from $4.95/mo. Better than free.”
>
> I look forward to your response and once again thanks for taking your time to engage in this conversation.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fhow-i-chose-my-replacement-for-parse-com-part-2.html)


### **Marian Ignev** — September 30, 2016 at 2:38 am ([permalink](https://www.codenameone.com/blog/how-i-chose-my-replacement-for-parse-com-part-2.html#comment-22997))

> Marian Ignev says:
>
> Hi Chidiebere,
>
> Thank you for your detailed answer. I appreciate it and I’m totally agree with you … let the interested reader be the judge I think that’s fair 🙂
>
> And yes … SashiDo is not a free service, but everybody can try it 14 days for free 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fhow-i-chose-my-replacement-for-parse-com-part-2.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
