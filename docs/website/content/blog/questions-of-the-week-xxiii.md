---
title: Questions of the Week XXIII
slug: questions-of-the-week-xxiii
url: /blog/questions-of-the-week-xxiii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xxiii.html
aliases:
- /blog/questions-of-the-week-xxiii.html
date: '2016-09-15'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xxiii/qanda-friday.jpg)

This has been a busy week with new features and some interesting announcements. We are releasing a plugin  
update today with a lot of fixes especially to the GUI builder which should be far more stable.

NetBeans announced they will be moving to Apache and [we volunteered to help](https://wiki.apache.org/incubator/NetBeansProposal).  
We’ll post some thoughts about this next week.

Java One is coming back, again we chose not to go. I wanted to write a bit about it but didn’t find the time to  
put down my thoughts. Overall I love Java One, it’s loads of fun…​  
But flying there from another continent is probably not the best use of our time/budget.

On stackoverflow things were as usual:

### UWP SQLite CodenameOne with native interface

We do need to support a more consistent cross platform database. SQLite is pretty inconsistent between platforms

[Read on stackoverflow…​](http://stackoverflow.com/questions/39500153/uwp-sqlite-codenameone-with-native-interface)

### How do I access UI components in a .res file with Codename One?

This is so much easier in the new GUI builder, hopefully this weeks update will tip the scales for most developers

[Read on stackoverflow…​](http://stackoverflow.com/questions/39497111/how-do-i-access-ui-components-in-a-res-file-with-codename-one)

### List adding functionality

Lists are always a pain which is why we recommend [avoiding them](/blog/avoiding-lists/)

[Read on stackoverflow…​](http://stackoverflow.com/questions/39494963/codenameone-list-adding-functionality)

### How to compress project size in codenameone

This should actually be on by default for most scripts

[Read on stackoverflow…​](http://stackoverflow.com/questions/39489455/how-to-compress-project-size-in-codenameone)

### Wrong paths in Codename One preferences

Unfortunately we were unable to reproduce the exact issue, hopefully it was resolved. If not we could use a  
good way to reproduce it consistently

[Read on stackoverflow…​](http://stackoverflow.com/questions/39482677/wrong-paths-in-codename-one-preferences)

### UWP deployment failed with CodenameOne

We had some UWP deployment issues that should be fixed in todays update

[Read on stackoverflow…​](http://stackoverflow.com/questions/39477840/uwp-deployment-failed-with-codenameone)

### iOS certification generation with Codename One

The certificate generation process is by definition “flaky” as we rely on undocumented behavior. That’s why we  
hide it behind a webservice so we can patch it without asking everyone to update their install

[Read on stackoverflow…​](http://stackoverflow.com/questions/39472324/ios-certification-generation-with-codename-one)

### How to make enable https in codenameone

I’m really grateful when people answer these questions, it’s always hard to answer the obvious questions

[Read on stackoverflow…​](http://stackoverflow.com/questions/39466919/how-to-make-enable-https-in-codenameone)

### Issues with Mirah

I haven’t played with the Mirah integration that Steve built a while back, if you like languages like Ruby this  
might be interesting to you

[Read on stackoverflow…​](http://stackoverflow.com/questions/39461797/issues-with-mirah)

### Add Checkbox to CodenameOne TableModel in Table component

MVC is always challenging even when it’s a simplified version like the one in `Table`

[Read on stackoverflow…​](http://stackoverflow.com/questions/39456175/add-checkbox-to-codenameone-tablemodel-in-table-component)

### Clickable Component

We should probably block users from setting a lead component to another lead component

[Read on stackoverflow…​](http://stackoverflow.com/questions/39449138/codename-one-clickable-component)

### Is Self Signed Certificate work for IOS?

When first reading this I thought it was about signing but it seems to be a question about https server setup

[Read on stackoverflow…​](http://stackoverflow.com/questions/39447232/codenameoneis-self-signed-certificate-work-for-ios)

### Layered Layout not fill the screen

This question is probably not as important with the new support for the floating button

[Read on stackoverflow…​](http://stackoverflow.com/questions/39435913/codename-one-layered-layout-not-fill-the-screen)

### How to use Google Speech API from Codename One?

There are some hidden features in Codename One such as the ability to record audio without user interaction to  
a specific format type. This was one of those features that we added for an enterprise developer a while back  
and never really promoted

[Read on stackoverflow…​](http://stackoverflow.com/questions/39433368/how-to-use-google-speech-api-from-codename-one)

### How to use drawImage instead of scaled for performance

Some of the performance tips are specific for usage

[Read on stackoverflow…​](http://stackoverflow.com/questions/39429497/how-to-use-drawimage-instead-of-scaled-for-performance)

### Post request don’t send argument

These issues are much easier to debug when you look at the output on the server and describe the request that  
worked for you as a dump. Otherwise we are just guessing…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39429170/codenameone-post-request-dont-send-argument)

### How do I use the codename one migration tool on my Android Studio project?

People asked us a lot about building a tool like this, when we finally put the effort in and built it we expected  
more interest around it. This demonstrates perfectly why you can’t take feature requests from non-users seriously…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/39425633/how-do-i-use-the-codename-one-migration-tool-on-my-android-studio-project)

### Error on Build: “error: cannot find symbol method compare(int, int)”

This is one of those “Codename One doesn’t support Java feature X” which prompted us to write  
[this](/blog/why-we-dont-support-the-full-java-api.html).

[Read on stackoverflow…​](http://stackoverflow.com/questions/39422409/codenameone-error-on-build-error-cannot-find-symbol-method-compareint-int)

### Calling custom JavaScript function

The [javascript package](/javadoc/com/codename1/javascript/package-summary/)  
contains pretty extensive documentation on this

[Read on stackoverflow…​](http://stackoverflow.com/questions/39417360/calling-custom-javascript-function-from-codename-one)

### MediaPlayer doesn’t release video on form change

We should make media playback more intuitive than it is right now

[Read on stackoverflow…​](http://stackoverflow.com/questions/39404180/codename-one-mediaplayer-doesnt-release-video-on-form-change)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
