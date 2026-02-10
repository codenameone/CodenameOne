---
title: Announcing CodeRAD 2.0 Preview
slug: announcing-coderad-2-0-preview
url: /blog/announcing-coderad-2-0-preview/
original_url: https://www.codenameone.com/blog/announcing-coderad-2-0-preview.html
aliases:
- /blog/announcing-coderad-2-0-preview.html
date: '2021-08-13'
author: Steve Hannah
description: We are proud to announce the immediate availability of the CodeRAD 2.0
  developer preview. CodeRAD is a modern MVC framework for building truly native,
  mobile-first, pixel-perfect applications in Java and Kotlin.
---

We are proud to announce the immediate availability of the CodeRAD 2.0 developer preview. CodeRAD is a modern MVC framework for building truly native, mobile-first, pixel-perfect applications in Java and Kotlin.

![CodeRAD 2.0](/blog/announcing-coderad-2-0-preview/CodeRAD-2.0-1024x576.jpg)

CodeRAD 2 builds upon the solid foundation of CodeRAD 1.0, and adds several new features aimed at increasing component reuse, and improving developer experience.

### Declarative View Syntax

First among the long list of new features included in CodeRAD 2 is the ability to write your **View** classes in XML.

For example, consider this simple login form component written in Java:

```java
				
					package com.example.mybareapp;

import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

public class LoginForm extends Container {
    public MyJavaForm() {
        super(BoxLayout.y());

        Label loginHeading = new Label("Login");
        loginHeading.setUIID("LoginHeading");

        TextField username = new TextField();
        username.setMaxSize(30);
        username.setConstraint(TextField.USERNAME);
        username.setUIID("LoginField");
        username.setHint("Enter username");
        username.getHintLabel().setUIID("LoginFieldHint");

        TextField password = new TextField();
        password.setConstraint(TextField.PASSWORD);
        password.setUIID("LoginField");
        password.setHint("Enter password");
        password.getHintLabel().setUIID("LoginFieldHint");

        Label usernameLabel = new Label("Username:");
        usernameLabel.setUIID("LoginFieldLabel");

        Label passwordLabel = new Label("Password:");
        passwordLabel.setUIID("LoginFieldLabel");

        Button login = new Button("Login");
        login.setUIID("LoginButton");

        Button reset = new Button("Reset");
        reset.setUIID("ResetButton");

        Container buttons = FlowLayout.encloseCenter(
                reset,
                login
        );

        addAll(loginHeading,
                usernameLabel,
                username,
                passwordLabel,
                password,
                buttons
        );
    }
}
				
			
```

You could write the same component as a CodeRAD view using the following:

```xml
				
					xml version=1.0" ?

    Login

    Username:
    

    Password:
    

    
        Reset
        Login
    

				
			
```

The XML view is automatically transformed into a Java class by the CodeRAD annotation processor so that performance is on par with the hand-coded **Java** version.
  
  
This example demonstrates the primary benefit of using XML as a language for writing view classes: It is **declarative**! The XML representation maps directly to way the view will be presented in the app.
  
  
This example is just the tip of the iceberg, however, as CodeRAD’s XML views also include many other features aimed at making your development experience as smooth as possible. They have built-in support for property binding, for example, making it easy to maintain a clean separation between your **View** and **Controller** logic.

### Hot Reload

When building user interfaces, the biggest pain point, by far, is the “test-edit-reload” cycle. e.g. Load app in simulator, add label to your form, reload app in simulator, navigate back to form, repeat…​

Having to wait while the app is recompiled, and the simulator is reloaded each time you make a change is an excruciating productivity killer.

CodeRAD 2 takes a giant step toward alleviating this problem with its introduction of **Hot Reload**. When this feature is enabled, the simulator will update in near real time (1-2 second delay) as you make changes to your application source.

There are two modes for Hot Reload:
  
  
1. ****Reload Simulator**** – This will restart your app at your “start form” when changes are detected.
  
  
2. ****Reload Current Form**** – This will restart your app, and automatically load the current Form, to save you from having to manually navigate there.

![codename-one-coderad-hot-reload-menu](/blog/announcing-coderad-2-0-preview/codename-one-coderad-hot-reload-menu.png)

## Note

> This is actually a pseudo "hot" reload, as it restarts your app rather than just patching code in place like the existing "Apply code changes" feature in most Java IDEs does. The "apply code changes" feature is very limited as it doesn’t apply to code that has already been run in your app, and it doesn’t support things like adding methods or classes. If you are trying to test changes to a UI form using "apply code changes", you would typically need to navigate away from your form, and navigate back after code changes are applied to see the changes. And this would still be insufficient in cases where the UI depends on "bootstrap" code in the app. A full app restart is necessary to reliably see the result of code changes, and this is what CodeRAD’s hot reload feature provides.

### CodeRAD 2 Intro Video

This introduction to CodeRAD 2 video is a good starting point if you want to learn more about the features and concepts of CodeRAD.

### Getting Started

We have added a “CodeRAD (MVC) Starter Project” option to the [Codename One initializr](https://start.codenameone.com/), which will give you a starting point for developing apps with CodeRAD 2.

![](/blog/announcing-coderad-2-0-preview/initializr-rad-mvc-dropdown.png) 

The "Tweet App" template is also a CodeRAD 2 project that provides a twitter-style app template, with login forms, and a tweet list view.

### Learn More

We have developed a wealth of resources to help you get started with CodeRAD. After watching the [intro Video](https://youtu.be/x7qaWBTjwMI), you can check out these other resources:

[The CodeRAD Developers Guide](https://shannah.github.io/CodeRAD/manual/)
:   This includes an introduction to the key concepts, as well as two tutorials:

    ****1. [Getting Started](https://shannah.github.io/CodeRAD/manual/#getting-started)**** and [companion screencast](https://youtu.be/QdyO4tpYOHs).

    ****2. [Building a Twitter Clone](https://shannah.github.io/CodeRAD/manual/#_app_example_1_a_twitter_clone)****

****[The CodeRAD Wiki](https://github.com/shannah/CodeRAD/wiki)****
:   The best source for reference documentation on CodeRAD components.

****[The CodeRAD Javadocs](https://shannah.github.io/CodeRAD/javadoc)****
:   JavaDocs for CodeRAD.

****[Github Repository](https://github.com/shannah/CodeRAD)****
:   All the source for CodeRAD.

## Sample Projects

****1. [CodeRAD2 Samples](https://github.com/shannah/coderad2-samples)**** – Includes a growing set of samples demonstrating the use of CodeRAD components. **Views are located in the [src/main/rad/views directory](https://github.com/shannah/coderad2-samples/tree/master/common/src/main/rad/views/com/codename1/rad/sampler).**

****2. [Tweet App](https://github.com/shannah/tweetapp)**** – A partial twitter clone.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
