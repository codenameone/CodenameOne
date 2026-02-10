---
title: Test Recorder and Toolbar
slug: test-recorder-toolbar
url: /blog/test-recorder-toolbar/
original_url: https://www.codenameone.com/blog/test-recorder-toolbar.html
aliases:
- /blog/test-recorder-toolbar.html
date: '2017-05-22'
author: Shai Almog
---

![Header Image](/blog/test-recorder-toolbar/quality.jpg)

One of the key takeaways I have from the bootcamp is the need to work on TDD in Codename One. We had a test framework and test recording framework for years, but it wasn’t picked up by many developers and as such it stagnated. As we launched the `Toolbar` API we didn’t even check that framework and some basic stuff stopped working.

This was something several attendants in the bootcamp mentioned as important as well as a couple of enterprise customers so I knew we had to revisit this feature. With this weeks update commands should be handled correctly even with a toolbar and features such as “wait for title” should work for a form with a `Toolbar`. I’d still recommend using `setName()` for any component you want to test as this will make the generated code far clearer to the human observer when you record a test.

But first lets take a step back and review the process to record a test again. If you have an app you wish to test you can just open the test recorder thru the Test Recorder menu option:

![Test recorder menu option](/blog/test-recorder-toolbar/test-recorder-launch.png)

Figure 1. Test recorder menu option

![The test recorder UI during recording](/blog/test-recorder-toolbar/test-recorder.png)

Figure 2. The test recorder UI during recording

When you press the record button in the test recorder UI you will instantly see the skeleton test class generated. You can then just use the app as normal and most of the features should instantly generate themselves into the test case.You can use the assert buttons to generate assertions of values and press the save button when you are done.

Once the file is saved you can edit it the tests directory and run it via the Run Tests option in the Codename One menu or the standard Test option (this is IDE dependent).

While we fixed a lot of things in the test recorder it might still generate problematic code especially in the “wait for X” logic which might cause a test to fail because we don’t wait enough for something to occur. If you run into such issues or inexplicable failures please let us know.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
