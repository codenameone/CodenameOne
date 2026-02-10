---
title: Survey Results, Kotlin, Updates and News
slug: survey-results-kotlin-updates-news
url: /blog/survey-results-kotlin-updates-news/
original_url: https://www.codenameone.com/blog/survey-results-kotlin-updates-news.html
aliases:
- /blog/survey-results-kotlin-updates-news.html
date: '2017-06-28'
author: Shai Almog
---

![Header Image](/blog/survey-results-kotlin-updates-news/codenameone-academy.jpg)

It’s been a hectic month getting the academy up and orchestrating the release. I have a mixed bag of updates and announcements we need to make. Specifically covering the surveys for the next academy app, the [Kotlin survey](/blog/kotlin-wora-ios-iphone-windows-android.html), the coming update to 3.7.2 and future milestones.

### Kotlin

As you recall I published a Kotlin survey a while back and we didn’t publish the results. Some of the result were a bit surprising…​ Lets review some of the answers.

![How do you use Kotlin today?](/blog/survey-results-kotlin-updates-news/survey-how-kotlin-today.png)

Figure 1. How do you use Kotlin today?

Most of the people who responded used Kotlin to some degree which is very good and indicates they have a sense of what to expect.

![How do you use Kotlin today?](/blog/survey-results-kotlin-updates-news/survey-do-you-use-kotlin-android.png)

Figure 2. Do you build Android applications with Kotlin?

So 58% said they use Kotlin today in some form but only 37.9% use it for Android?

My first thought was that it’s Codename One fans who use Kotlin on the server and there were those but not much. I’m not sure.

![Do You Use GUI Builders?](/blog/survey-results-kotlin-updates-news/survey-do-you-use-gui-builders.png)

Figure 3. Do You Use GUI Builders?

This number is a bit larger than I expected. I would have expected fewer people to use GUI builders but I guess that’s just me.

![Should the Codename One GUI builder generate Kotlin code instead of Java?](/blog/survey-results-kotlin-updates-news/survey-gui-builder-kotlin.png)

Figure 4. Should the Codename One GUI builder generate Kotlin code instead of Java?

I expected this to be easy, I would have expected overwhelmingly that people wouldn’t care. Maybe I should have opened with the disclaimer that it will divert work from other things…​

I don’t care much about the GUI builder language/code. I’m not sure how much Kotlin will benefit from that if we generate code in Kotlin instead of Java. Maybe I phrased the question badly…​

#### What’s next for Kotlin

I’m not sure if we’ll do the whole Kotlin code in the GUI builder. It shouldn’t impact development as you can still write Kotlin code everywhere but a GUI builder form will probably a java form for now. I’d like to push something out relatively quickly rather than spend time on things like the GUI builder support.

I’m not sure when this will be out as we have a lot on our plate for 3.8.

### Whatsapp vs. Social App

Continuing with the trend of surveys you have all helped tremendously with the decision to go with an Uber clone as the first app built for the [new course](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java).

As I mentioned before the results for the second place were ambiguous and within the margin of error. They favored the social app which went against my personal intuition…​ Turns out my intuition got it wrong…​

![Survey results for whatsapp vs. social app](/blog/survey-results-kotlin-updates-news/survey-whastapp-vs-socialapp.png)

Figure 5. Survey results for whatsapp vs. social app

Which means our second app developed in [the course](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) will be a social app.

It doesn’t mean the third app will be a whatsapp clone…​

I’ll do a new survey when we reach that point as a lot can change by then and we might have additional app suggestions.

### Update 3.7.2

We already have some changes and bug fixes for the GUI builder lined up. We didn’t include them in 3.7.1 which just included critical code fixes.

We won’t release an update this Friday because we still have some things we’d like to finish but the week after that we’ll push out a new update both to the library and the plugin.

### Summer Time & Course Updates

Next week Steve will be on a much deserved vacation and during the month of August some of us (myself included) have more spotty schedules due to the summer vacation. During August we might skip a Friday update in August if there aren’t any significant changes to the libraries.

Blog posts probably won’t have the same volume during August either.

### Future Milestones – 3.9 and beyond

One of the things we didn’t mention in the release is the dates of the releases after 3.8.

We decided to move all of that into the github project where it’s easier to manage everything in one place. Check out the [milestones section](https://github.com/codenameone/CodenameOne/milestones). We listed all the dates of the milestones for 2018 although these can change in some situations but we’ll try to abide by them as 3 releases per year is pretty good.

### Academy

I’ve posted some additional modules covering things such as push, websockets etc. since publishing the academy but haven’t kept the same pace as before. I still have a lot of material that needs publishing just to get to the starting line.

I’m working hard on finishing this and would hopefully have more time to do this during July

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
