---
title: Android Gradle Build Status & Minor Changes
slug: android-gradle-build-status-minor-changes
url: /blog/android-gradle-build-status-minor-changes/
original_url: https://www.codenameone.com/blog/android-gradle-build-status-minor-changes.html
aliases:
- /blog/android-gradle-build-status-minor-changes.html
date: '2016-02-08'
author: Shai Almog
---

![Header Image](/blog/android-gradle-build-status-minor-changes/gradle.png)

I’ve been remarkably busy working on issues and documentation so I neglected an important announcement I  
had to make. Over the weekend we flipped the default build from gradle back to ant. So effectively if you don’t set  
any build hint the behavior will be `android.gradle=false` which should work fine for most of you. This is temporary but  
we felt it was necessary as a stopgap measure.

In other news it seems that fixing the Codename One documentation is like diving into a bottomless pit.  
When we started this effort the developer guide was 300 pages it is now approaching 500 pages and we  
aren’t close to half way thru…​

This doesn’t even cover all the work we did with refining the JavaDocs and there is a lot of work that needs doing  
on that side of the fence.

During this time I’ve made a conscious effort not to do anything significant that isn’t documentation writing but  
some code had to go thru. Specifically things related to syntax that needed doing for the developer guide.

### CheckBox Toggle Syntax

Up until now we had terse syntax for creating a toggle button for a `RadioButton` but we didn’t have anything  
like that for the `CheckBox`. So we added a couple of methods:

  * [createToggle(Image icon)](https://www.codenameone.com/javadoc/com/codename1/ui/CheckBox.html#createToggle-com.codename1.ui.Image-)

  * [createToggle(String text)](https://www.codenameone.com/javadoc/com/codename1/ui/CheckBox.html#createToggle-java.lang.String-)

  * [createToggle(String text, Image icon)](https://www.codenameone.com/javadoc/com/codename1/ui/CheckBox.html#createToggle-java.lang.String-com.codename1.ui.Image-)

### ButtonGroup Shortcut

Up until now creating a `RadioButton` required adding it to a `ButtonGroup` which was tedious.

To solve this we added a varargs  
[addAll(Component…​)](https://www.codenameone.com/javadoc/com/codename1/ui/ButtonGroup.html#addAll-com.codename1.ui.RadioButton…​-)  
method as well as a [varargs constructor](https://www.codenameone.com/javadoc/com/codename1/ui/ButtonGroup.html#ButtonGroup-com.codename1.ui.RadioButton…​-).

### ComponentGroup enclose

`ComponentGroup` didn’t have an `enclose` method which is one of those things that beg for a fix since its **the**  
`Container` for that sort of API.

So we added two enclose methods:

  * [enclose(Component…​)](https://www.codenameone.com/javadoc/com/codename1/ui/ComponentGroup.html#enclose-com.codename1.ui.Component…​-)

  * [encloseHorizontal(Component…​)](https://www.codenameone.com/javadoc/com/codename1/ui/ComponentGroup.html#encloseHorizontal-com.codename1.ui.Component…​-)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
