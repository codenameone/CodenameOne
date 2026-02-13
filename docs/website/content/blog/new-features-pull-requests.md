---
title: New Features and Pull Requests
slug: new-features-pull-requests
url: /blog/new-features-pull-requests/
original_url: https://www.codenameone.com/blog/new-features-pull-requests.html
aliases:
- /blog/new-features-pull-requests.html
date: '2017-08-30'
author: Shai Almog
---

![Header Image](/blog/new-features-pull-requests/new-features-6.jpg)

I haven’t blogged as much in the past month and as a result I have a big pile of updates from all over. This is going to be a big list so I’ll start with a few pull requests that were submitted by [Diamond](https://github.com/diamondobama) and [Durank](https://github.com/DurankGts). If you see something broken or something that could be better in Codename One just [fix it like they did](/blog/how-to-use-the-codename-one-sources.html)!

### New Material Icons & Multi Selection Calendar

[In this pull request](https://github.com/codenameone/CodenameOne/pull/2194) Diamond added two separate features:

#### Material Icons

The PR includes 49 new google material design icons, our material design font and constants were made a while back and we didn’t update them with changes from Google. Diamond updated both the font and the constants which now include new icons such as `MATERIAL_DELETE_FOREVER` & `MATERIAL_DO_NOT_DISTURB_ON`.

You can check out the full list of added icons in [the source of the PR](https://github.com/codenameone/CodenameOne/pull/2194/commits/827b713cefd956e53ae5eaea39bd120a2ee08547).

#### Calendar Multi Selection

If you haven’t used the calendar class it’s a pretty old class in Codename One. It was one of the original LWUIT classes, I think it even predated my time in LWUIT (it’s **THAT old**). The age of this specific API is pretty obvious when looking at it but people still find it useful for very specific use cases.

People have enhanced this class in the past but as far as I know Diamond is the first person to commit the code back which is pretty great and might make other developers follow suit.

The core of the change is a new method in `Calendar` called `setMultipleSelectionEnabled(boolean)` which toggles the calendar to multi selection mode. He also added a `CalendarMultipleDay` UIID to style this selection mode differently.

### Convenience methods for BorderLayout

[In this pull request](https://github.com/codenameone/CodenameOne/pull/2191) Diamond added a few new methods to the `BorderLayout` class. Specifically:
    
    
    public static BorderLayout center();
    public static BorderLayout absolute();
    public static BorderLayout totalBelow();

These are essentially synonyms to the equivalent constants e.g. instead of writing `new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER)` you can now write `BorderLayout.center()` which is both shorter and is better for compile time checks (since `CENTER_BEHAVIOR_CENTER` is an int).

He also added:
    
    
    public static Container centerCenterEastWest(Component center, Component east, Component west);
    public static Container centerTotalBelowEastWest(Component center, Component east, Component west);
    public static Container centerTotalBelow(Component center);

Which are shorthand variants of enclose that allow us to package two or three components together in a horizontal border layout or just enclose with a total below border layout.

__ |  In the total below mode the center component takes up the entire screens and the sides are automatically placed on top of it thus creating a layered effect   
---|---  
  
### Accordion Listener

There is currently no listener event bound to the accordion expansion. This might be useful for various UI’s and is addressed in [this pull request](https://github.com/codenameone/CodenameOne/pull/2193) from Durank.

We now have new methods in `Accordion` specifically:
    
    
    public void addOnClickItemListener(ActionListener a);
    public void removeOnClickItemListener(ActionListener a);

### QR Code Creation

[rwanghk](https://github.com/rwanghk) created a [new cn1lib that creates QR codes](https://github.com/rwanghk/QRMaker). We already have a couple of cn1libs for reading QR codes but this is the first one that allows you to generate a QR code from your data.

### Geofencing Improvements

Steve committed a big set of improvements to the geofencing functionality. First and foremost is the new `GeofenceManager` class that abstracts many of the pain points of geofencing. E.g. platform-specific limitations on the number of simultaneous geofence regions that can be monitored.

The geofencing documentation is now improved and mentions platform-specific limitations, and clarified the units of some parameters. There is also some support for `LocalNotifications` in the simulator which should make debugging these applications far easier. The simulator will also display warnings when geofences are added with radiuses that may be smaller than the minimum on device.

Now, when the app is paused, `LocalNotifications` will show up in the system tray.

For additional convenience in measuring distance Steve added `getDistanceTo()` methods to both `Geofence` and `Location`. He also added `createDistanceComparator()` methods to `Geofence` and `Location` to make it easier to sort locations and regions by distance from a reference point or region. This is used by `GeofenceManager` to return a list of current geofences sorted by distance from the current location.

### End of ThreadSafeDatabase

We introduced `ThreadSafeDatabase` last release and we’re deprecating it today. This sucks but since the API is mostly compatible changing back shouldn’t be hard.

We tried to get `ThreadSafeDatabase` but eventually decided that the approach it took just isn’t workable across platforms. We ran into too many oddities even in the simulator with no decent explanation for the failures we encountered. Eventually, this left us no choice as `ThreadSafeDatabase` left us in a position that was even less stable.

However, we did integrate an improvement to the SQLite implementation on iOS which should make it more robust for multi-thread access. This isn’t ideal and we can’t guarantee thread safety but hopefully at least some of the portability issues would be resolved due to that change.

### Build Timeouts

Over the past week one of our bug fixes in the iOS VM triggered longer builds that caused timeouts especially with release builds. We hope to resolve this issue over this weekend update and get builds working again.

However, during this process it has come to my attention that some developers have such timeout failures and haven’t complained about them or kept up with us. We had assumed the issue was resolved for most developers with the exception of one special case but it seems that this assumption might have been flawed.

So for the record: we don’t track your failed builds…​ We have too many builds going thru the servers to see who’s build failed and we don’t have direct access to the build logs. We just don’t know if you are getting errors so please report them.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
