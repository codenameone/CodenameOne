---
title: 'New Feature: Inspect Component'
slug: inspect-component
url: /blog/inspect-component/
original_url: https://www.codenameone.com/blog/inspect-component.html
aliases:
- /blog/inspect-component.html
date: '2022-03-28'
author: Steve Hannah
description: We’ve added support for "Inspect Component", which is similar to the
  "Inspect Element" feature available in Chrome.
---

We’ve added support for "Inspect Component", which is similar to the "Inspect Element" feature available in Chrome.

![Inspect Component - Codename One](/blog/inspect-component/Inspect-Component-Codename-One-1024x536.jpg)

Continuing in the direction of improving the development experience inside the Codename One simulator, we have made another batch of small improvements this week. Notably, we’ve added support for “Inspect Component”, which is similar to the “Inspect Element” feature available in Chrome. Now, if you right-click on a component in the simulator, it will provide you with a context menu.

![](/blog/inspect-component/inspect-component.png) 

Figure 1. The "Inspect Component" option in the context menu.

If you select the “Inspect Component” option in this menu, it will select the selected component in the “Components” panel, and in the “Component details”.

The “Component Inspector” has been available as part of the simulator for a long time, but selecting a particular component used to require you to expand the component tree nodes manually until you found the component you were looking for.

The simulator would help you a little bit by placing a translucent red highlight over the currently selected node’s corresponding component, but it could still be a little bit tedious to have to manually walk through the tree to find the component you wanted.

![](/blog/inspect-component/selected-component.png) 

Figure 2. After choosing "Inspect Component", the "Components" panel auto-expands and selects the corresponding node, and the "Component details" panel is populated with the selected component details.

You’ll notice also, that the “Component Details” will automatically populate with the details of your selected component. Most for the fields in the component details form are read only, but the **UIID** field can be edited, allowing you to experiment with different styles for your elements directly in the simulator.

### Disabling the Context Menu

If your application needs to handle “Right Mouse-click” events, then the context menu may interfere with your app. In such cases you can disable the context menu by toggling the ![arrow button](/blog/inspect-component/arrow-button.png) button found on the toolbar of the **Components** panel. Once toggled off, the button icon will change to ![arrow button disabled](/blog/inspect-component/arrow-button-disabled.png).

![](/blog/inspect-component/components-toolbar.png) 

Figure 3. The "Components" panel toolbar, which includes a toggle button to enable/disable the context menu.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **plumberg** — March 29, 2022 at 5:28 pm ([permalink](https://www.codenameone.com/blog/inspect-component.html#comment-24528))

> plumberg says:
>
> Tested it out for a bit yesterday. Looks great and very convenient!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Finspect-component.html)


### **ThomasH99** — April 5, 2022 at 11:21 am ([permalink](https://www.codenameone.com/blog/inspect-component.html#comment-24530))

> ThomasH99 says:
>
> It is really great with these improvements! While you’re at it, Steve, improving the testing (test automation) would also make a huge difference. The old support is promising but misses some small things to be really useful. Let me know if this migth make it to your todo list and I’d be happy to provide input and feedback 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Finspect-component.html)


### **Steve Hannah** — April 5, 2022 at 11:15 pm ([permalink](https://www.codenameone.com/blog/inspect-component.html#comment-24531))

> Steve Hannah says:
>
> We are making a push right now to improve the development experience in the simulator. No promises, filing an RFE in the issue tracker is where to start the ball rolling.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Finspect-component.html)


### **ThomasH99** — April 10, 2022 at 12:30 pm ([permalink](https://www.codenameone.com/blog/inspect-component.html#comment-24533))

> ThomasH99 says:
>
> Great, I hope for the best :-). The TestRecorder has a lot of potential and would make the Simulator UI even more powerful and impressive. I’ve filed an RFE here: <https://github.com/codenameone/CodenameOne/issues/3575>
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Finspect-component.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
