---
title: StackOverflow, Cordova update, Validation, Text Input, Hints & Icon
slug: stackoverflow-cordova-update-validation-text-input-hints-icon
url: /blog/stackoverflow-cordova-update-validation-text-input-hints-icon/
original_url: https://www.codenameone.com/blog/stackoverflow-cordova-update-validation-text-input-hints-icon.html
aliases:
- /blog/stackoverflow-cordova-update-validation-text-input-hints-icon.html
date: '2015-11-29'
author: Shai Almog
---

![Header Image](/blog/stackoverflow-cordova-update-validation-text-input-hints-icon/floatinghint.gif)

After a discussion with some members of the community we decided to shift the weight of our support efforts to  
[StackOverflow](http://stackoverflow.com/tags/codenameone) from our existing  
google group [discussion forum](/discussion-forum.html). Notice that we will still answer questions  
in the discussion forum but we strongly prefer using StackOverflow (don’t forget to use the codenameone tag).   
There are issues with StackOverflow which is why we are keeping the existing group and will still answer  
questions/issues there but the benefits far outweigh the issues: 

  * Cleaner – stack overflow is far better structured than the discussion forum. This translates to better  
search functionality that can make finding answers much easier
  * Google spring cleaning – Many Google projects moved their support to StackOverflow and with Googles  
history of killing projects in use we have some concerns
  * Community Collaboration – one of the big issues with the existing discussion forum is that the community  
finds it harder to collaborate and incentives for community support aren’t clear. 

As part of this work we also try to answer questions thru StackOverflow even when we get them in other  
channels (e.g. email support). This can be helpful as a future reference to users who are looking at the given  
issue encountered by a pro user. 

#### Cordova Support Improvements

A lot has changed in the Cordova support since we  
[originally announced it](/blog/phonegap-cordova-compatibility-for-codename-one.html), Steve  
has been hard at work on the following changes: 

  1. Plugins are now automatically imported when projects are created. 
  2. Improved documentation, especially for plugin development
  3. Plugin development should now just involve implementation of the native portions in Java. The rest is automated.

All of the details are listed in the main port  
[site](https://github.com/codenameone/CN1Cordova).  
Documentation specific to plugin development can be found  
[here](https://github.com/codenameone/CN1Cordova/wiki/Plugin-Development).  
This effectively means it should be MUCH easier to port plugins to Cordova with these changes. 

#### Easier Text Component Editing

We’ve simplified text editing features in two significant ways, first we just added a set of methods to `TextArea`  
instead of the venerable yet unintuitive methods of `Display`.  
We now have `startEditing`, `startEditingAsync`, `isEditing` &  
`stopEditing` in `TextArea`. Most of these are pretty trivial with the exception  
of `startEditingAsync` which wraps the call to `startEditing` in a `callSerially`.  
That’s generally a good practice if you can follow that. 

We also added to `Form` the methods `setEditOnShow`/`getEditOnShow`  
which allow you to determine a `TextArea` that will enter editing mode immediately when the form  
is shown. This is a common and somewhat error prone use case since its pretty tricky to know when a form  
was actually shown (post transition). 

#### Floating Hint

Hints in text fields are really useful to convey information, however they can become really bothersome when  
working with details that aren’t yours. E.g. around here Shai and Almog can be both surnames and given names  
so a form showing a text field with Shai and Almog might not convey the right information to the user. 

So why have a hint at all if we should always use labels as is the case above?  
Hints are really convenient since they show a user exactly where to type and are a great convention for a blank  
field… `FloatingHint` offers the best of both worlds. When there is no input or focus in the field  
the floating hint appears like a regular text field hint with some extra space above. When there is input or a user  
enters the field the hint animates into a small label on top as such: 

![FloatingHint](/blog/stackoverflow-cordova-update-validation-text-input-hints-icon/floatinghint.gif)

You need to style `FloatingHint` in the designer to provide the style for the label on top (the hint  
uses the same `TextHint`). Other than that the code is trivial just wrap any text area/field with a  
`new FloatingHint(myTextField)`. 

#### Validation Error Messages

Up until now we provided great validation hints and constraints but didn’t provide any way to show the error  
messages related to validation…  
We just committed a fix for that where errors will appear as `InteractionDialog` with the popup  
pointing either at the error emblem or at the component. You can enable/disable this functionality on the validator  
by using: `val.setShowErrorMessageForFocusedComponent(true);`

![Validation Error Message](/blog/stackoverflow-cordova-update-validation-text-input-hints-icon/validation-message.png)

#### New Default Icon

![New Icon](/blog/stackoverflow-cordova-update-validation-text-input-hints-icon/new_icon.png)

Up until recently we used our logo as the default icon for Codename One applications, in the next plugin update  
we’ll replace it with this image. Obviously you would need to replace it as you would for every shipping application  
but as a person whose phone is filled to the brim with Codename One logo icons for all the testcases I need to  
run I think this is a huge improvement.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — December 9, 2015 at 10:46 am ([permalink](/blog/stackoverflow-cordova-update-validation-text-input-hints-icon/#comment-22589))

> Diamond says:
>
> Hi Shai,
>
> I was trying out the FloatingHint and discover few bugs that occurs on Sim, Android and iOS.
>
> When you initiate scrolling with your finger placed on the textfield, it receives focus and trigger editing.
>
> If a textfield contains text and you trigger scrolling from that textfield, the text is hanging on the screen while the textfield itself and the rest of the form scrolls.
>
> Can FloatingHint be modified to allow the empty space that holds the Label to be hidden when nothing is there and grow during the animation process? Right now the space between fields is too large due to that and doesn’t make forms look good.
>



### **Shai Almog** — December 9, 2015 at 11:58 am ([permalink](/blog/stackoverflow-cordova-update-validation-text-input-hints-icon/#comment-22597))

> Shai Almog says:
>
> Hi Diamond,  
> yes we know of these issues. Steve just committed improvements for this on iOS and is switching to Android for further improvements.  
> The issues on Android are more severe since the text input there is a bit antiquated and needs a serious overhaul.
>
> Feel free to file issues on these bugs for tracking them.
>
> I’d like to provide that option in floating hint but its non-trivial in the current animation architecture since it would require growing in two containers which means using animateHierarchy and that API is a bit flaky. I’m looking at rewriting our animation logic to address some core issues in Codename One and allow some seriously cool effects including this sort of effect.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
