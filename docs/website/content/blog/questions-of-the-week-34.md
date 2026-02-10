---
title: Questions of the Week 34
slug: questions-of-the-week-34
url: /blog/questions-of-the-week-34/
original_url: https://www.codenameone.com/blog/questions-of-the-week-34.html
aliases:
- /blog/questions-of-the-week-34.html
date: '2016-12-01'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-34/qanda-friday2.jpg)

While I find these posts useful I think it’s time to re-think this post which is overly mechanical and only post  
interesting news from the week rather than “everything”. So this week we’ll try something new, I’ll discuss the  
news in general and the questions/answers I find valuable only. I’ll ignore the other questions and this should  
make the post more “digestible”.

[peopletookallthegoodnames](http://stackoverflow.com/users/1299498/peopletookallthegoodnames) asked  
about `Storage` to `FileSystemStorage` mapping [here](http://stackoverflow.com/questions/40897107/codename-one-cant-get-correct-storage-path-on-android).  
This is a problematic subject. `Storage` is an abstraction, the fact that the implementation can be seen sometimes  
within the `FileSystemStorage` is a “leak” within the abstraction.

We considered hardening the abstraction by making this impractical in the simulator but that would break running  
apps and we don’t want to get into that at this time.

[HelloWorld](http://stackoverflow.com/users/6351897/helloworld) showed why it’s important to ask on stackoverflow…​  
We don’t currently support Java 8 operations in native Android code but following  
[his question](http://stackoverflow.com/questions/40886260/android-native-code-in-codename-one-with-lambda-not-working)  
we added an experimental feature that might allow this.

[Stefan](http://stackoverflow.com/users/5695429/stefan-eder) literally took the sample code from our javascript  
JavaDocs and tried to run it and it seems  
[it didn’t work](http://stackoverflow.com/questions/40866747/call-of-java-method-via-javascript-not-working-in-simulator)  
apparently this code should be executed after the page loads which makes it a bit harder to bind properly.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
