---
title: Why we Don't Import Android Native Code?
slug: why-we-dont-import-android-native-code
url: /blog/why-we-dont-import-android-native-code/
original_url: https://www.codenameone.com/blog/why-we-dont-import-android-native-code.html
aliases:
- /blog/why-we-dont-import-android-native-code.html
date: '2016-04-16'
author: Shai Almog
---

![Header Image](/blog/why-we-dont-import-android-native-code/android_studio.jpg)

When we first started to present Codename One to investors a very prominent local investor said he’d commit  
to a round of funding if we allow Android developers to import native Android apps to Codename One. We passed  
on that which in retrospect might have been a mistake but the technical challenges were the main reason for  
that decision.

### Should we Support Android Native Code?

Ignoring the technical issues involved this might seem like a “no brainer” for Android developers. But history teaches  
us otherwise…​

Quite often when a platform offers compatibility it serves to make it irrelevant rather than promote it. A famous  
example of such a platform is OS/2 which was marketed as: “Better Windows than Windows and better DOS than DOS”.

__ |  It is debatable whether OS/2 failed because or despite its choice to focus on compatibility.   
---|---  
  
The question becomes more muddled if we do a lousy job of importing. Compatibility to Android is not realistically  
feasible for elaborate apps. Android is remarkably complex and nuanced, activities have no equivalent in other  
OS’s and the UI paradigms of Android are “unique”.

#### Would an import that is “bad” be better than nothing?

We are currently at the “probably mindset”.

So while we can’t say that this is a “good thing” we’d rather have something  
in place. Even if that “something” is broken, partial and problematic.

We already support importing Android string bundles into the localization section in the Codename One designer,  
this is quite useful as you can send string bundles to localization services rather easily.

__ |  Most localization services also support Java properties files but do so badly as most of them didn’t really  
read the spec   
---|---  
  
We think the “right thing” to do in the case of Android is to import only the XML files and resources and ignore  
the Java code which you would need to migrate manually (as there are no activities etc.). Codename One has  
the theoretically perfect destinations, the resource files can serve as a great destination for the res files.

The XML’s can be imported directly into the XML file format used by the [new GUI builder](/blog/new-gui-builder/).

However, the technical aspects are challenging to say the least!

### This is a REMARKABLY Difficult Task

Codename One’s roots predated Android. We started working on the predecessor of Codename One at Sun in  
2007\. This was before the iPhone announcement and before the Android announcement.

While todays product looks completely different from its original version a lot of the API design decisions are  
different and hard to adapt. Android’s layout system has many parallels to our layout system as both are inspired  
by previous work in the same field, but Android’s layouts are far more complex with many small nuances.

Specifically we chose to keep the layout system as simple as possible telling the users to “just nest”. Android  
developers took the opposite approach of adding flags to customize layout behaviors deeply. This means that  
a lot of Android layouts have no direct Codename One equivalents…​ So we would need to translate a flat layout  
into a hierarchy which isn’t always trivial and might result in issues.

Because the Android implementation is so large supporting all of those nuances would be challenging.

The style system in Android is also quite different from our approach to theming and this also poses some challenges.  
While we do have some support for the Android style 9-patch borders (which differ from our 9-piece borders)  
there is no UI tooling to edit such borders in Codename One.

### Will we Ever do This?

We really want to do this. We think it will provide a lot of value to many developers and help us reach a larger  
market share even if it’s badly implemented.

Unfortunately, a lot needs to happen first for this to be viable so it’s relatively low in the list of priorities at this time.  
So before we see a stable version of Windows, GUI builder etc. this probably won’t happen.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Scott Turner** — April 22, 2017 at 6:21 pm ([permalink](/blog/why-we-dont-import-android-native-code/#comment-23262))

> Scott Turner says:
>
> I think that’s fair. Focus on the low hanging fruit first before tackling such a large and complex problem. This translation tool is probably something that would eat up all of your resources for the forseeable future, and isn’t a guaranteed return on investment. Good business sense to steer clear for now.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
