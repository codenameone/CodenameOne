---
title: How I Chose my Replacement for Parse.com
slug: how-i-chose-my-replacement-for-parse-com
url: /blog/how-i-chose-my-replacement-for-parse-com/
original_url: https://www.codenameone.com/blog/how-i-chose-my-replacement-for-parse-com.html
aliases:
- /blog/how-i-chose-my-replacement-for-parse-com.html
date: '2016-07-19'
author: Shai Almog
---

![Header Image](/blog/how-i-chose-my-replacement-for-parse-com/parse.com-post-header.jpg)

When I first read the announcement that Parse.com would shut down on January 28th 2017, I went from  
disbelief (it’s probably an early April fool’s joke) to rage (how could they do that?!) to sadness (oh no! it was a  
great service) and finally to utter confusion (where do I go from here and what do I do with my apps –  
[parse4cn1](https://github.com/sidiabale/parse4cn1), two [MVPs](https://www.smash-ict.com/#projects) for clients  
and an upcoming rewrite of  
[Medex](https://play.google.com/store/apps/details?id=com.mosync.app_Medex) for which I was planning to use  
Parse.com as backend?). A few months down the line, a lot has happened such as the release of the open source  
Parse Server (yay!) and subsequently the Parse Dashboard. However, I still had to decide what do with my apps  
and I guess you have to as well.

By now, the imminent shutdown of Parse.com must have hit home for you as well: You wish it was merely a bad dream but unfortunately it’s a stark reality. You’ve got just a few months left to migrate your existing apps and obviously do not want to face another shutdown. Nearly every major MBaaS vendor is brandishing a banner that directly or indirect says “Parse.com let you down but you’re safe with us”. However, whom can you trust? Or maybe you should just host your own Parse Server? How about migrating away from anything Parse-like and settling for a completely different MBaaS.

In this post, I share some important points to consider while deciding on your replacement for Parse.com. In Part 2, I’ll share how I applied these guidelines while migrating my own apps. This article is highly recommended for anyone who still has their apps on Parse.com or is contemplating using a Parse.com-like solution as backend for their (new) apps.

### There Are Basically 3 Options

Don’t be intimidated by the long lists of Parse.com alternatives that you may have seen (for example, this one). Broadly speaking, all Parse.com alternatives fit into one of the following three categories:

  1. Self-hosting

  2. Parse Server hosting provider

  3. Other MBaaS (unrelated to Parse)

Let’s briefly consider these categories and give some examples.

#### Self-hosting

Self-hosting comes in two flavors:

  * Running Parse Server on a PaaS infrastructure such as Heroku, AWS, Microsoft Azure or Google App Engine. The Parse Server wiki provides links to installation guides for these services and more.

  * Running Parse Server on your own infrastructure. This could be attractive if you already have your own platform and/or if you require on-premises hosting.

#### Parse Server hosting provider

These are BaaS providers who have leveraged the open-source Parse Server to create Parse.com-like services. Some of the providers in this category are back4app, ParseGround (now called SashiDo) and Oursky Parse Hosting to mention just a few.

__ |  A more comprehensive list can be found [here](https://github.com/relatedcode/ParseAlternatives#parse-server-hosting-providers).  
Note that I cannot ascertain how up-to-date it is…​   
---|---  
  
Undoubtedly, this category of Parse.com alternatives will be attractive to most of the users who chose Parse.com in the first place because of its ease of use, rich feature set and/or intuitive dashboard. Although Parse Server is not a clone of Parse.com, it is quite similar in terms of features and even has some new features like live queries which were not present in Parse.com. As such Parse Server is definitely an alternative worth considering!

As already mentioned above, there are several providers in this category. If you take this route, you’ll need to choose one of them. While there is no silver bullet or crystal ball to help you make that choice, I’ll highlight important aspect that you must take into consideration while choosing your Parse.com replacement. But first, let’s consider the third category of Parse.com alternatives.

#### Other MBaaS solutions

Shortly after Parse.com announced the imminent shutdown, various lists of alternatives appeared on the Internet.  
Consider, for example, [this list](https://github.com/relatedcode/ParseAlternatives) of 100 or more alternatives to Parse.com. (Note that the list also includes alternatives from the other two categories.)

While Parse.com offered a rich feature set, it was not ideal for all use cases. For example, Parse.com was (and by extension Parse Server is) not suited for real-time messaging. If you’re one of those who found Parse’s offering insufficient for your use case, then this is a good time to consider other MBaaS options that could better meet your needs including the possibility of an in-house custom solution. While you’re at it though, watch out for signs of vendor lock-in and do not be fooled by false assurances that a particular MBaaS is reliable simply because it is backed up by a big company. If Facebook pulled the plugs on Parse, any of those big-company-backed solutions can face the same fate! That’s the reality.

The rest of this article will focus on the first two categories (i.e. self-hosting and Parse Server hosting providers).

### 5 Things to Bear in Mind

Now that you have a better idea of what the options are, here are some things you must bear in mind while making your choice for your Parse.com replacement.

#### Self-hosting is more than merely clicking a “Deploy on X” button

Most self-hosting Parse migration guides display a “Deploy on <PaaS>” button. While this might help you through the initial migration, it is only the tip of the iceberg! Self-hosting requires non-trivial investment of time and resources; it requires a certain degree of technical competence and could be quite expensive in the long run.

On the other hand, self-hosting offers you maximum flexibility from choice of database to Parse server version (remember that Parse Server is still in active development so there are regular updates, bug fixes, etc.). Furthermore, you can add new functionality that is not (yet) available in Parse Server.

If you’re considering self-hosting, be sure to give yourself convincing answers to questions like:

  * Do I have the technical skills to maintain my own Parse Server? In addition to supporting your apps, you’re suddenly going to also be responsible for several quality aspects like scalability, security, redundancy and reliability. Can you cope with that especially if your app has a significant user base?

  * Is self-hosting financially viable for me? It might look cheap at the beginning but as your app’s audience grows, you’ll probably need to scale. While it is out of the scope of this article to go into detailed cost calculations, I’d like to point you to this article on how much self-hosting using AWS could cost as well as this discussion thread by other Parse Server enthusiasts on cost considerations. The main message here is: Do not underestimate the costs!

  * Do I have sufficient time to set up my Parse Server and migrate all my apps? You have till January 28, 2017 to get done with migration and that’s not so much time. In fact, according to the migration guide provided by Parse.com, you should already have finished setting up your Parse Server by July 28, 2016. While that’s not a firm deadline, if you’re reading this article now and still haven’t chosen your self-hosting PaaS, you’re kind of running late…​

If you cannot provide satisfactory answers to questions like the ones above, self-hosting is likely not for you. Instead, consider a Parse Server hosting provider that will handle the hosting for you, allowing you to focus on making great apps. That was the power of Parse.com – “focus on your apps and we’ll take care of the rest”. And it remains a strong value proposition.

#### Beware of vendor lock-in

This is particularly relevant if you choose for the Parse Server hosting category. Parse Server and Parse Dashboard are open source so you might be wondering why vendor lock-in is a potential issue. Let me explain.

Parse Server was open sourced with a BSD license model. While I’m not a software licensing expert, my understanding of this license is that it allows users to modify the source without releasing such modifications. Sooner or later, Parse Server hosting providers will begin to add their own features and I can bet you that not all of them will be willing to contribute back to the open source Parse Server. Every feature from a Parse Server hosting provider that is not present in the open source Parse Server is a potential lock-in! Think of what will happen if your new provider shuts down. Obviously, Parse Server will remain open source so there will be alternative providers. However, you’ll be stuck with those custom features that are no longer supported.

Other possible symptoms of potential vendor lock-in are unclear terms and conditions with regard to your data and migration in the event of a shutdown. Let’s be frank: Parse.com has provided a decent migration plan and, as far as I can tell, they’re doing their best to support us through the process. Your next provider should be able to commit upfront to something similar or better in case the undesirable happens.

Moreover, you’d be much better off with a Parse Server hosting provider that is actively involved in adding features and fixing bugs in the open source Parse Server repository over one that just promises to occasionally give back with no evidence whatsoever.

#### Beware of elaborate Freemium offers

As the adage goes “Once bitten, twice shy!” With the unprecedented shutdown of Parse.com, you definitely do  
not want to migrate your apps only to face another shutdown. So be careful with very attractive freemium offers.  
One of my college professors often reminded us that “there’s no such thing as a free lunch” and I think he was right.

Although the [announcement](http://blog.parse.com/announcements/moving-on/) of Parse.com’s shutdown didn’t  
provide details of why Facebook pulled the plugs, the main reason is apparently $$$ as explained in  
[this New York Times article](http://bits.blogs.nytimes.com/2016/01/28/facebook-to-shut-down-parse-its-platform-for-mobile-developers/?_r=1)  
which, remarkably, was termed by Parse.com’s CEO, as  
[“pretty accurate”](https://www.quora.com/Why-is-Parse-shutting-down/answer/Ilya-Sukhar?srid=uX6B5).  
You might also find  
[this article](https://medium.com/@sashidoio/dangers-and-benefits-of-the-freemium-model-what-did-we-learn-out-of-parses-shutdown-79becb215c84#.ggsb3gf6l)  
on the dangers and benefits of the freemium model interesting.

#### Parse Server is not a clone of Parse.com

With the release of the open source Parse Server, one would expect that the Parse.com code was cleaned up  
and open sourced. However, that is definitely not the case. While Parse Server has strong similarities to Parse.com,  
it is only a look-alike and not a clone as clearly indicated in  
[this discussion](https://github.com/ParsePlatform/parse-server/issues/765) involving Parse.com engineers.

So bear in mind that there are difference some of which are listed on the Parse Server  
[wiki](https://github.com/ParsePlatform/parse-server/wiki/Compatibility-with-Hosted-Parse). If your app relies  
heavily on any of those features (e.g. background jobs or push notification support for Windows Phone), then Parse Server as-is does not (yet) meet your needs. The good news is that, now that it’s open source, you can (and should) contribute to making Parse Server better. You don’t have to wait until someone else builds it; you can make Parse Server richer by contributing new features! Alternatively, you could look for other ways to realize the missing functionality. Going back to the background jobs example, you could find other means to schedule background jobs or use a Parse Server provider like back4app which already implements that feature. Similarly, you could consider a separate service for multi-platform push notifications which can later be integrated with Parse Server via the  
[PushAdapter](https://github.com/parse-server-modules/parse-server-push-adapter) mechanism.

Another implication of the fact that Parse Server is not a clone of Parse.com is that there could be bugs and other issues that make it unfit for production especially for more complex apps, at least for the time being. While it is difficult to assess how production-ready Parse Server currently is, this  
somewhat [outdated discussion](https://github.com/ParsePlatform/parse-server/issues/1106) might provide some insights.

#### Make your app as future-proof as possible

With the migration from Parse.com to Parse Server, you have to release a new version of your apps with at least the Parse endpoint changed from api.parse.com to whatever endpoint you’ll be using. While you’re at it, make sure to enrich your app with the intelligence of being able to dynamically switch to a new backend should the need ever arise in the future. In that way, you’ll have one thing less to worry about if the undesirable happens with your next provider. This is just one way you can make your apps future-proof.

### How I chose my replacement for Parse.com

In Part 2, I will explain how I applied the above guidelines in choosing a replacement for Parse.com. Watch out for the follow up post!

### Conclusion

In this article, I’ve given you some food for thought as you decide where to migrate your Parse.com-hosted apps to.  
I’ve deliberately not recommended any particular self-hosting service or Parse Server hosting provider as there is  
no one-size-fits-all solution. You’ll need to make that choice based on your app needs and your answers to the  
(difficult?) questions posed in the article. In a sequel blog post, I will explain how I decided on my replacement for  
Parse.com, highlighting strong and weak points of the Parse Server providers that I tested. If you still can’t make  
any headway, feel free to get in touch or leave a comment. Also do not hesitate to share your thoughts on the subject!

### References and interesting reads

  1. Radek Zaleski (February 2016). Parse Is Done. What Now? 5 Tips How to Proceed with Migration. Retrieved from <https://www.netguru.co/blog/parse-is-done.-what-now-5-tips-how-to-proceed-with-migration>

  2. Ron Palmeri (January 30, 2016). Why Facebook’s Parse shutdown is good news for all of us. Retrieved from <http://venturebeat.com/2016/01/30/why-facebooks-parse-shutdown-is-good-news-for-all-of-us/>

  3. Marian Ignev (April 28, 2016). Dangers and benefits of the freemium model — What did we learn out of Parse’s shutdown? Retrieved from <https://medium.com/@sashidoio/dangers-and-benefits-of-the-freemium-model-what-did-we-learn-out-of-parses-shutdown-79becb215c84#.ggsb3gf6l>

  4. Alysson Melo (May 3, 2016). Parse alternative: Self-hosting or Parse hosting provider? Retrieved from <http://blog.back4app.com/2016/05/03/parse-alternative/>

  5. Alysson Melo (June 15, 2016). Firebase vs. Parse Server. Retrieved from <http://blog.back4app.com/2016/06/15/firebase-parse>

  6. Alysson Melo (June 21, 2016). How much cost Parse self-hosting? Retrieved from <http://blog.back4app.com/2016/06/21/parse-aws>

  7. Mike Isaac and Quentin Hardy (January 28, 2016). Facebook to Shut Down Parse, Its Platform for Mobile Developers. Retrieved from <http://bits.blogs.nytimes.com/2016/01/28/facebook-to-shut-down-parse-its-platform-for-mobile-developers/?_r=1>

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
