---
title: Async Debugging with IntelliJ IDEA
slug: async-debugging-with-intellij-idea
url: /blog/async-debugging-with-intellij-idea/
original_url: https://www.codenameone.com/blog/async-debugging-with-intellij-idea.html
aliases:
- /blog/async-debugging-with-intellij-idea.html
date: '2022-04-11'
author: Steve Hannah
description: We have added support for IntelliJ’s asynchronous code debugging feature,
  so that you can more easily debug your asynchronous code.
---

We have added support for IntelliJ’s asynchronous code debugging feature, so that you can more easily debug your asynchronous code.

![Async Debugging with IntelliJ IDEA - Codename One](/blog/async-debugging-with-intellij-idea/Async-Debugging-with-IntelliJ-IDEA-Codename-One-1024x536.jpg)

When debugging your apps in IntelliJ, stack-traces will include the “async” context’s stack frames so that you can see the stack trace of the code that scheduled your asynchronous code. For example, methods like `callSerially()` are notoriously prickly to debug because the “logical” stack trace includes the stack frame in which `callSerially(Runnable)` is called, and also the frame in which the **Runnable**‘s `run()` method is called. It is very difficult to walk up this “logical” stack from a break-point inside the `run()` method.

To demonstrate this point, consider the following code:

```java
				
					package com.codenameone.devmode;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;

import static com.codename1.ui.CN.callSerially;

public class TestAsyncDebugForm extends Form {

    public TestAsyncDebugForm() {
        super(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
        Button btn = new Button("Hello 1");
        btn.addActionListener(this::button1Clicked);
        Button btn2 = new Button("Hello 2");
        btn2.addActionListener(this::button2Clicked);
        add(BorderLayout.CENTER, btn);
        add(BorderLayout.SOUTH, btn2);
    }

    private void button1Clicked(ActionEvent evt) {
        callSerially(printHello);
    }

    private void button2Clicked(ActionEvent evt) {
        callSerially(printHello);
    }

    Runnable printHello = () -> {
        printHello();
    };

    private void printHello() {
        System.out.println("Hello");
    }
}
				
			
```

This creates a form with two buttons: “Hello 1” and “Hello 2”. Clicking on either button will ultimately trigger an async call to the `printHello()` method, but they follow different code paths to get there. Clicking “Hello 1” triggers the `button1Clicked()` method, and clicking “Hello 2” triggers the `button2Clicked()` method – both triggering an async call to `printHello()`.

Let’s set a break point inside the `printHello()` method:

![](/blog/async-debugging-with-intellij-idea/break-point-hello.png)

If we debug the app and press “Hello 1”, we will see a stack trace like the following:

![](/blog/async-debugging-with-intellij-idea/stack-trace-hello1-sync.png)

There is no way to tell from this stack trace which button was pressed to trigger it. If we walk up the stack we hit a dead end at **executeSerialCall()**. This is because the break-point occurs in the asynchronous callback of `callSerially()`, so the original stack frame for the call to `callSerially()` is already “gone” by the time we hit our break-point.

Now, let’s try this again with async debugging enabled. The stack trace this time will look like:

![](/blog/async-debugging-with-intellij-idea/stack-trace-hello1-async.png)

We can now trace this break-point back to “Button 1” definitively because it displays both the “execution” stack frame’s trace, and the scheduler’s stack frame’s trace.

### Enabling Async Debugging

Async debugging requires:
  
  
1. That you are using IntelliJ IDEA
  
  
2. That your project is using Maven
  
  
3. That your `cn1.version` property is set to 7.0.65 or higher.
  
  
4. That your project is configured to use the `com.codename1.annotations.Async` annotations for the async stack traces feature. All projects created using the [Codename One initializr](https://start.codenameone.com/) after April 18th will include this configuration “out of the box”, so it should “just work”.

### Configuring the Async Annotations

As mentioned above, new projects created with [Codename One initializr](https://start.codenameone.com/) after April 18th, should include async stack traces out of the box. If you have an existing project on which you want to enable async traces, you just need to tell IntelliJ to use the Codename One annotations for async stack traces. The easiest way is to simply copy the [debugger.xml](https://github.com/shannah/cn1-maven-archetypes/blob/master/cn1app-archetype/src/main/resources/archetype-resources/.idea/debugger.xml) file from the cn1app-archetype into the `.idea` directory of your project.

## Place the following into the .idea/debugger.xml file of your project to enable async stack-traces.

```xml
				
					xml version="1.0" encoding="UTF-8"?

    
        
            
        
        
            
        
    

				
			
```

Alternatively you can follow the IntelliJ documentation for configuring custom annotations [here](https://www.jetbrains.com/help/idea/debug-asynchronous-code.html#custom_async_annotations). You should add `com.codename1.annotations.Async.Schedule` to the list of Async Schedule annotations, and `com.codename1.annotations.Async.Execute` to the list of Async Execute annotations. The configuration dialog is shown below.

![](/blog/async-debugging-with-intellij-idea/configure-annotations.png) 

Figure 1. The Async Annotations configuration dialog in IntelliJ.

### For More Information

For more information about IntelliJ’s asynchronous debugging feature, see [the IntelliJ documentation on the subject](https://www.jetbrains.com/help/idea/debug-asynchronous-code.html).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Antonio Rios** — April 14, 2022 at 3:20 am ([permalink](https://www.codenameone.com/blog/async-debugging-with-intellij-idea.html#comment-24534))

> Antonio Rios says:
>
> Wonderful new features! Great job guys! I’m literally impress every time I visit the blog and see some new cool feature added. Keep those cool useful features coming please.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
