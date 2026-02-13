---
title: Moving to API Level 27 and Facebook Clone is DONE!
slug: moving-to-27-facebook-clone-done
url: /blog/moving-to-27-facebook-clone-done/
original_url: https://www.codenameone.com/blog/moving-to-27-facebook-clone-done.html
aliases:
- /blog/moving-to-27-facebook-clone-done.html
date: '2018-05-30'
author: Shai Almog
---

![Header Image](/blog/moving-to-27-facebook-clone-done/android_studio.jpg)

This is important: We’ll update the build tools & build target to API level 27 with next weeks update (June 8th). We HIGHLY recommend you check your app before we flip the switch!  
I also just uploaded the final lesson in the Facebook Clone App which is now fully live in the [online course](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java).

I’ve discussed the [build target 27 migration before](/blog/android-build-target-27-migration.html). We wanted to do it in May but the work on the Facebook clone pushed that back so it will have to wait. You can test this right now by setting the build hints:
    
    
    android.buildToolsVersion=27
    android.targetSDKVersion=27

If you would like to go back to the previous level after the change you can set them to 23 although I’d strongly recommend against that. Google will require at least 26 starting in August and is already producing warnings if you try to submit updates to apps right now.

I answered a lot of FAQ’s about this [the last time I wrote about this](/blog/android-build-target-27-migration.html).

### Facebook Clone is GA

I was racing against the clock to get the Facebook clone out of the door and made it just now. In the past 48 hours I recorded the last 20+ lessons to finish the 46 lesson long module. I have a lot of thoughts about the results and insights about Facebook after doing this…​

I’ll try to write an article similar to the one [I wrote about Uber](https://medium.com/@Codename_One/what-ive-learned-from-cloning-the-uber-app-b0a7c743c1c1) detailing some of my experiences here.

I mentioned before when we launched the Uber app that the price of the course will go up by 100USD to $599 once the Facebook clone is up. Since it was late we’ll postpone this price change to June 12th so if you still didn’t signup for the course nows a good time.  
We’ll raise the price further in the future as we add things to the courses but probably not by the same amount.

I’ll send out a couple of reminders for this by email during the week.

### We Updated our Privacy Policy

Sorry about that. I know it’s a pain I think we all get enough emails without that one…​

I’m personally a big fan of the ideas behind GDPR & I think it will ultimately help in refining better business models that don’t rely on spyware tactics. For the record, we never tracked anything or used personal data in any way. It’s not a part of our business model.

We use a couple of 3rd party GDPR compliant services (Intercom & Google Analytics), that’s pretty much it.

Anyway it’s a minimal [privacy policy](/privacy.html) because we respect your privacy and don’t need to look at details. If you think we need to explicitly state something there that isn’t mentioned let us know.

There is a lot of additional news I’d like to cover but I’ll try to get to those next week.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
