---
title: Validate, Owner, Badges, ImageViewer and Picker Range
slug: validate-owner-badges-imageviewer-picker-range
url: /blog/validate-owner-badges-imageviewer-picker-range/
original_url: https://www.codenameone.com/blog/validate-owner-badges-imageviewer-picker-range.html
aliases:
- /blog/validate-owner-badges-imageviewer-picker-range.html
date: '2018-11-14'
author: Shai Almog
---

![Header Image](/blog/validate-owner-badges-imageviewer-picker-range/new-features-1.jpg)

I’ve been pretty busy over the past few weeks and didn’t get a chance to write a “what’s new” post. This caused a huge backlog of new features which I’ll try to cut down in this post.

### UI Validation

This is a feature that’s coming in the Friday update. Component inspector has a new “Validate” button that checks the UI for common mistakes. Right now it only checks for nested scrollables on the Y axis but ideally we should include additional checks.

This could be a pretty useful tool to diagnose common programming errors in the UI.

### Material Commands

`Command` has a new factory method:
    
    
    public static Command createMaterial(String name, char icon, ActionListener ev);

Using this API you can create a `Command` object with the given material icon.

### Fixing Revalidate

This is a big one: `revalidateWithAnimationSafety()`.

Codename One was inspired by Swing which didn’t have animations. We added them but our model for animations was too simplistic when we launched. We later refined it with the animation manager which was a huge overhaul.

Here’s the problem. Let’s say you have an animation running and you invoke `component.remove()` during the animation. It’s very likely `remove()` will disrupt the animation. So we serialized all calls to methods that mutate the UI. This means that if an animation is in progress it will wait for it to complete before doing the operation.

While this broke some edge case behaviors this was relatively compatible and worked reasonably well.

The problem is we didn’t change `revalidate()`. We left it “as is” and didn’t think it was a problem. It’s a complex method which impacts a lot of core functionality so leaving it in place was probably a good call.

However, for most cases you might want to use the new `revalidateWithAnimationSafety()` which makes sure animations aren’t disrupted by `revalidate()`.

### Component Ownership

`Component.setOwner()`, `Component.isOwnedBy()` and `Component.containsOrOwns(x,y)` allow components to denote an ownership hierarchy. This allows you to more easily track relationships hierarchical relationships between components that don’t happen to descendents of each other – e.g. for popup dialogs that are actually contained in a layered pane but are logically owned by components in the content pane. This is used by `Dialog` and `InteractionDialog` to better test for pointer events that occur outside their bounds. Now `Dialog.setDisposeWhenPointerOutOfBounds()` regards an event to be **in** bounds if it occurs on a component that is owned by the `Dialog` (or by a component in the dialog). The `Picker`, `AutocompleteTextField` and `ComboBox` popup dialogs have been updated to track their owner so that they behave appropriately. If you develop your own popups that are placed in the layered pane, it is up to you to set their owner appropriately using `Component.setOwner()` so that dialogs can deal with their pointer events properly.

There is no special house-keeping for the owner hierarchy. E.g. If you remove an owner from the form, it doesn’t do anything like remove its owned components also. It is up to you (the developer) to manage these relationships.

### Badges on Android

We added limited support for badges in the Android port. Now Push type 101 is supported on Android as well. Also `LocalNotification.setBadgeNumber(int)` is supported. Notable omissions are push type 100 and `Display.setBadgeNumber(int)` which are still not supported. Android, while supporting badges on API 22 and above still can only set it in conjunction with a notification, so APIs that just set the badge number still can’t be implemented.

### rotateRadian

Rotate works with absolute coordinates which in retrospect might have been a poor choice. We can’t change that as due to backward compatibility. So we added a new method `rotateRadian()` that rotate graphics context about the context’s current origin, taking into account translation.

### Zoom Enhancements on ImageViewer

[Carlos Verdier](https://github.com/carlosverdier) submitted a [a pull request](https://github.com/codenameone/CodenameOne/pull/2592) that adds a method to zoom to a specific location panning the image. As part of that change zoom is now animated by default.

You can disable zoom animations on `ImageViewer` using `setAnimateZoom(false)`.

### FontImage Rotation

Multiple icon fonts include rotating glyphs to indicate progress. While it’s trivial to rotate a `FontImage` we wanted to make it even simpler. As a result we added the method `FontImage rotateAnimation()` to `FontImage`. It returns a new `FontImage` instance using the same font/icon as the current font image. This new image is an animation that rotates to the right constantly.

You can use that in any `Label` subclass and it will animate the rotation constantly.

### Efficient getRGB

`Image.getRGB()` isn’t very efficient as it might trigger the creation of a new array when it’s invoked. [Dave Dyer](https://github.com/ddyer0) submitted a new [pull request](https://github.com/codenameone/CodenameOne/pull/2593) that adds a new `getRGB` variant. This variant accepts an existing `int[]` array and fills it up instead of returning the array.

This reduces RAM allocation and is ultimately more efficient as a result.

### Picker Rage Limits

One of the biggest RFE’s for the old `Picker` was the ability to determine ranges. Now that we have a lightweight picker implementation this is possible in a cross platform way:
    
    
    public void setHourRange(int min, int max);
    public void setStartDate(Date start);
    public void setEndDate(Date end);

This lets you limit the range of hours or dates in a lightweight `Picker` so a user won’t be able to pick outside of that range.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
