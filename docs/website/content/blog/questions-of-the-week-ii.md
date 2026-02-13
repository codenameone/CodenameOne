---
title: Questions Of The Week II
slug: questions-of-the-week-ii
url: /blog/questions-of-the-week-ii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-ii.html
aliases:
- /blog/questions-of-the-week-ii.html
date: '2016-04-21'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-ii/qanda-friday.jpg)

This week felt slow as we were going thru it but as I was preparing this installment I was reminded just how much  
activity we had in stackoverflow this week.

### Codenameone : Alternative to webBrowsers

This question was triggered by the desire for dynamic rich UI elements coupled with concern about  
an issue with the web browser component. It turns out that we just fixed that specific issue as we are  
moving into the final stretch before 3.4.

However, I did mention an interesting project which didn’t get much attention there,  
[Steves xml view](https://github.com/shannah/cn1-xmlview). You might want to check that out.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36708229/codenameone-alternative-to-webbrowsers)

### Alternative Methods in CodenameOne

This is a pretty common question: “Method X is missing what should I do”, in this case the methods that  
were missing included `Scanner` (to which we have no direct parallel), `File`, `Math.pow` & `String.format`.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36681724/alternative-methods-in-codenameone)

### Search TextField in TableLayout

We have several samples of searching within a Container hierarchy but most of them rely on the behavior of  
the `BoxLayout` what if we used the `TableLayout` and instead of narrowing only to a specific component we want  
to leave the entire row in place.

With a `Table` we can just modify the model dynamically but a `TableLayout` needs some creative code:

[Read on stackoverflow…​](http://stackoverflow.com/questions/36693541/search-textfield-in-tablelayout)

### IOS intercepting URLs issue

Intercepting URL callbacks is challenging and always painful regardless of the road you take. We tried  
to simplify it as much as possible but the Android/iOS ways differ too much.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36650285/codename-one-ios-intercepting-urls-issue)

### How to Create Background Service Through AlarmManager with Codename One Native Interface?

This question is a great example on why you don’t always need to reach out to the native interface wrench. You  
should usually check if there are other options and we do have such options today.

This shows the importance of asking as things also change a lot. Had he asked it 6 months ago the answer would  
have been completely different…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/36646173/how-to-create-background-service-through-alarmmanager-with-codename-one-native-i)

### How to fix CodenameOne build upload error?

This is a remarkably common forum question, I gave the generic answer which seemed to be incorrect in this  
particular case!

Turns out that this was one of those cases where caches in our servers got corrupted, so a note to developers  
who run into this, there is a chat button on the bottom right side of all pages. Try to contact us and one of our  
support engineers can help with issues like that. They don’t usually answer technical questions as these  
are interns who still don’t have that experience, but they can walk you thru resolving some of these issues.

[Read on stackoverflow…​](http://stackoverflow.com/questions/36731836/how-to-fix-codenameone-build-upload-erroe)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
