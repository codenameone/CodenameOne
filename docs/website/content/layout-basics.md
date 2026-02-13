---
title: "Layout Basics"
date: 2019-05-18
slug: "layout-basics"
---

## Transcript

In this short video I'll try to address one of the most challenging basic features of Codename One: Layouts

Before we go into the actual code let's explain the basics. Codename One Components are arranged within Containers. A Container can contain an arbitrary number of components

Since it derives from Component a Container can also Contain other Containers and so forth. By nesting these Containers one into another we can build any type of user interface

This begs the question: Why? Why not place the components where we want them to be on the screen?

Devices can have different resolution, pixel density, font sizes and form factors. So a specific position that looks good on one device can look awful in another.

With device orientation changes, tablets, desktop/web versions of the app etc. You would expect the UI to adapt "automatically". It gets worse with localization for instance languages like German have very long words and you would expect components to resize accordingly

Layout managers convey the logic of component placement in a way that Codename One understands and can adapt e.g. Let's say I want to say put a label on the left and put a text area next to it. I also want to give that text area the rest of the available space

We can do this in many ways but a common way would be to use a Border Layout. We can place the label in the WEST & the text field in the center

This can be abbreviated using this shorthand syntax so the initial container will have the east component

And can be further abbreviated using this one line of code, this also brings us to one of the more important aspects of layout managers. Constraints.

There are two types of layout managers in Codename One, those that have component constraints and those that don’t. Border layout requires a constraint for every addition (like the center or east constraint we used), layouts like table layout can work without one but add it implicitly. Layouts like Box Layout don’t need a constraint at all.

Let's go over some of the layout managers at a high level using the kitchen sink demo to show them off

First we have the flow layout which is the default layout manager in Codename One, it arranges components from left to right and gives each component its preferred size.

Flow layout can be aligned to the left, center or right. Vertically it can be aligned to the top middle or the bottom of the available space. It implicitly breaks a line when it reaches the end of the line. This is a bit of a long discussion to get into but if you add complex/large components into flow layout it might fail as Codename One doesn’t reflow for performance reasons. You should be careful when using it for anything non-trivial.

Next we have border layout that we discussed briefly, it can place components in the north (top), south (bottom), east (right), west (left) and center. Components placed in the north & south have their preferred height but are stretched to fill up available width. Likewise components placed in the east & west have their preferred width but stretch to fill up the available height.

The center component is a special case, it’s stretched to fill the available space. There is a special absolute center mode that allows it to be in the actual center based on the preferred size of the component. Border layout doesn’t make sense as a scrollable component since the behavior heavily depends on the fixed size of the available space. Hence it will implicitly disable scrolling when set to a Container or Form. We often use Border Layout for components that need to be sized explicitly such as Map or BrowserComponent as the center location makes sure to give them the available space.

Box layout places components in a row or a column. When we place elements in Box X the row has the same height but the elements are given their preferred width. When we use Box Y elements occupy the available width but have their preferred height. This helps components align property on the given axis as they have the same size on that axis.

Grid layout gives all the components the same size based on their preferred size and places them in rows/columns. This is useful for the icon grid style of user interface and also useful for button bars where you want all the elements to have the exact same size regardless of their text

Table layout is more flexible, it’s very useful for input and forms. Table layout allows spanning columns or rows between table cells. It uses constraints to define behaviors of table elements.

And finally Layered Layout places elements one on top of the other so we can do things like overlays. Here we set a bit of margin on each entry so the difference between the layers are clearer

Now here is a neat trick to learn about components, open the component inspector on a UI and look within the hierarchy to see what we did for that specific hierarchy. You can also look at the code but that isn’t always clear… In the contacts section of the kitchen sink demo we can inspect the various elements and understand which layout was used for what purpose.

We have a layered layout for the root, this allows the floating action button on the bottom.

A bit deeper we can see the Box Layout Y that contains all of the “Swipeable Container” classes,

Swipeable container is a layered layout that we can swipe to show the buttons below

These buttons under the swipeable container use a grid layout so they can occupy the same size

The top component in the swipeable container is the multi button, this button is really just a border layout Container with text in the center and an icon label in the west!

I could talk about layouts for another hour (and I will at a later date) but for now we’ll call this a day. Thanks for watching and I hope it was helpful
