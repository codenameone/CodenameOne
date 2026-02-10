---
title: HELPBUTTON COMPONENT
slug: helpbutton-component
url: /blog/helpbutton-component/
original_url: https://www.codenameone.com/blog/helpbutton-component.html
aliases:
- /blog/helpbutton-component.html
date: '2020-10-31'
author: Steve Hannah
---

This is the second is a series of blog posts hightlighting some of the components available in the CodeRAD cn1lib. The [first post (or series of posts) introduced the RAD Chatroom component](https://www.codenameone.com/blog/rad-chatroom-part-1.html), a rich 2nd order UI component that encapsulates the user interface for a fully functional chat room.

__ |  A second-order UI component is a complex UI component, usually composed of multiple basic components, which is designed for a specific type of application. Some examples of second-order UI components are login forms, contacts lists, chat room components, news lists, etc..   
---|---  
  
In this post I share a much simpler, first-order component: The [HelpButton](https://shannah.github.io/CodeRAD/javadoc/ca/weblite/shared/components/HelpButton.html). The HelpButton is just a button that displays a “Help” or “Error” icon. When the user clicks on this button, it pops up with some “help” text.

![](/blog/helpbutton-component/help-button-animated.gif)

### Usage Example

The following is the snippet that was used to generate the above screen capture:
    
    
    Form hi = new Form("Hi World", BoxLayout.y());
    HelpButton btn = new HelpButton("This is some help text to give you some hints");
    hi.add(FlowLayout.encloseIn(new Label("Hi World"), btn));
    hi.show();

There really isn’t much to this component, but it is a handy addition to the toolbox nonetheless.

| To use the HelpButton component you’ll need to add the CodeRAD cn1lib to your project, which is available in through Codename One preferences. For instructions on adding cn1libs to your projects, see [this tutorial](https://www.beta.codenameone.com/blog/automatically-install-update-distribute-cn1libs-extensions.html).  
---|---  
  
For more information about CodeRAD, check out its [github repo](https://github.com/shannah/CodeRAD).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
