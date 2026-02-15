---
title: Increase your Build Quotas
slug: increase-your-build-quotas
url: /blog/increase-your-build-quotas/
original_url: https://www.codenameone.com/blog/increase-your-build-quotas.html
aliases:
- /blog/increase-your-build-quotas.html
date: '2018-07-24'
author: Shai Almog
---

![Header Image](/blog/increase-your-build-quotas/generic-java-2.jpg)

The most common question we get about Codename One is: “Is Codename One Free”. The direct answer is “Yes” but we don’t want to mislead. You can work with the open source code, which is just as free as any other project. But it’s not for the faint of heart…​  
The build servers have quotas so we won’t go out of business. This is perceived by developers as “not free” but since no one else offers build servers I have an issue with that perception. To battle that perception we’re increasing the build quotas.

Sort of…​

To keep us sustainable paying users essentially pay for the free users. Worse, it’s only the pro/enterprise accounts that cover these costs.

So in order to increase the quotas we need your help. We’ll give you increased quotas for friends of yours that join e.g. you bring your friends and we’ll increase your build quotas respectively.

For every friend we’ll add 512kb to your jar size limit and 50 build credits. This is a permanent addition that will come into effect during the quota reset (it isn’t immediate). We’ll also count that friend as if he referred one person already so he’ll have more than the default count!  
Currently this is capped off at 15 friends which should give you plenty of room to grow.  
These benefits are perpetual and aren’t dependent on your friends staying or paying. However, we reserve the right to revoke these credits for abuse of this system!

Here’s the kicker, we just updated all the users in the database currently to 1 referral by default. So all active Codename One users should already have a larger build credit by default. Due to technical reasons we can’t do it to old users that didn’t use Codename One since our migration from App Engine…​

### How Does it Work?

You can invite friends by sharing a special URL which you can see in your console [here](/build-server/) under the Account tab. E.g. mine is: `</index/?ref=baa9d923-b26a-430b-a814-02cf55605231>`.

__ |  You might need to logout and login again to see this entry. It should be below the token   
---|---  
  
The important aspect here is the `?ref=baa9d923-b26a-430b-a814-02cf55605231` part. You can attach it to any html URL in this site including this page e.g.: `</blog/increase-your-build-quotas/?ref=baa9d923-b26a-430b-a814-02cf55605231>` would be valid.

However `<https://www.codenameone.com/?ref=baa9d923-b26a-430b-a814-02cf55605231>` isn’t valid as it doesn’t contain an html file in the URL.

### Terms and Questions

#### Does this Apply to Paid Users?

Yes. If you ever cancel your account you’d revert to paid mode and still have your increased credits. If you signup for a paid program and downgrade you won’t lose any signups during the paid duration or after.

#### How does this Work?

The referral URL sets a cookie in the users browser that is active for 6 months. If during those 6 months the user signs up he is counted.  
You would need to promote Codename One to your friends/social network to get the additional credits.

#### What if a User Clicks two URL’s?

The last one is counted. This is the industry standard.

#### How can I Monitor This?

Right now you will only know when the build credits are reset. However, we plan to introduce a UI that will let you see how many users you referred.

#### How do you Check for Abuse?

We only count activated users. We check user behavioral patterns through an automated system that alerts us of potential abuse.

### Future Directions

We’d like to extend this to push notification as well. We have some ideas on how to provide push support for free/basic accounts but for that we would probably want to redesign our push servers.

That’s a bit of an undertaking and we’ll only start that off if this program proves successful.

I hope you all take advantage of this as much as possible!

A Word About the 1MB Jar Size

We’re doing this mostly to battle a perception issue. The JAR size limit should be enough for relatively demanding apps. It’s generally a good practice to stay within it.

The JAR size limit refers to the size of the JAR sent to the server. Not the one returned.

Native libs are essentially free since they are fetched from gradle/cocoapods. By default a clean hello world app is around 2-3kb and the Uber/Facebook clones fall well below the 1mb limit.

It’s a good limit to abide by. It means your builds will be faster and the end result application will be smaller. You shouldn’t need a larger app size.

Keep in mind that a 1mb jar app can grow up to 16 times on iOS and a bit less on Android.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Synapsido** — August 21, 2018 at 12:22 am ([permalink](/blog/increase-your-build-quotas/#comment-24006))

> Synapsido says:
>
> I’m free user for now, I have several developer fiends invited, where can I see my total Quotas in my account…?
>



### **Shai Almog** — August 21, 2018 at 8:23 am ([permalink](/blog/increase-your-build-quotas/#comment-21538))

> Shai Almog says:
>
> On the first of the month when your credits are reset you can see how many clicked and created an account based on the build credits you would have.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
