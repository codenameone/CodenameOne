---
title: New Property Sheet and JavaDocs
slug: new-property-sheet-and-javadocs
url: /blog/new-property-sheet-and-javadocs/
original_url: https://www.codenameone.com/blog/new-property-sheet-and-javadocs.html
aliases:
- /blog/new-property-sheet-and-javadocs.html
date: '2022-04-06'
author: Steve Hannah
description: We have added a new property sheet and a JavaDocs button to the toolbar
  of the Properties panel.
---

We have added a new property sheet and a JavaDocs button to the toolbar of the Properties panel.

![Property Sheet and JavaDocs - Codename One](/blog/new-property-sheet-and-javadocs/Property-Sheet-and-JavaDocs-Codename-One-1024x536.jpg)

To continue the theme of improving the development experience in the simulator, you will find a few new features landing on Friday.

Firstly, we have added a new property sheet that shows you the value of all of the properties of the selected component in the component tree. This is useful for checking whether a container is scrollable, or what text property of a button is set to, or pretty much anything else you’d like to know about a component’s state.

![](/blog/new-property-sheet-and-javadocs/property-sheet-in-simulator.png) 

Figure 1. The new property sheet is visible in the right panel of this screenshot.

The above screenshot shows the property sheet in the right panel, displaying properties of the selected label.

Currently the values are read-only. Editing support will be added soon.

### Filtering Properties

The property sheet includes all properties that have a public accessor. For some classes, there can be an awful lot of properties, which can be unwieldy to sift through, so we have included the ability to filter them, with a query.

![](/blog/new-property-sheet-and-javadocs/property-sheet-filter.png) 

In the above screenshot, I am filtering the properties so that only the ones containing the word "Width" are shown.

### JavaDocs

We’ve also added a `JavaDocs` button to the toolbar of the `Properties` panel that will open the JavaDocs for the class of the currently selected component.

![](/blog/new-property-sheet-and-javadocs/javadocs-button.png)

Currently this only works for Maven projects, as it uses the `-javadoc.jar` files that accompany Maven packages.

## TIP

> Depending on your IDE settings, you may need to explicitly download the JavaDocs for your project dependencies in order for this feature to work properly. In IntelliJ, you can do this by right-clicking on the `pom.xml` file, and selecting `Maven > Download Documentation`
>   
>   
> ![maven download documentation](/blog/new-property-sheet-and-javadocs/maven-download-documentation.png)

If JavaDocs are not available for the specific class of the selected component, it will traverse the superclass hierarchy until it finds a class that does have JavaDocs available.

### Force "Revalidate"

We have also added a smaller niche feature that you may never need, but can come in handy: `Revalidate`

If you’re debugging layout issues, you may not be sure if, perhaps the container just needed to be revalidated, or re-laid-out.
  
  
Now, you can trigger a revalidation directly in the simulator by right-clicking the container’s node in the component tree, and selecting `Revalidate` from the context menu.

![](/blog/new-property-sheet-and-javadocs/revalidate-option.png)

### More To Come

We’re not done yet. You’ll see more improvements trickling in over the next couple of weeks. If you have any pet requests, you can post them in the comments, or in the issue tracker.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Antonio Rios** — April 9, 2022 at 7:32 pm ([permalink](/blog/new-property-sheet-and-javadocs/#comment-24532))

> Antonio Rios says:
>
> Excellent work guys! This new feature will definitely help me troubleshoot a problem I’m having with the SpanLabel not taking the width I’m setting. For some reason it seems that I can’t resize the width through the css styling nor the setPreferredW() method on the spanlabel I made. I’m having a lot of fun learning the API, reading the developer guide, and learning from the kitchen sink example and the other samples. Every day I impressed by all the cool features. Thanks for all your hard work!
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
