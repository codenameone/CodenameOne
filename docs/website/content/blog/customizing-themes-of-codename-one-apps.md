---
title: Customizing Themes Of Codename One Apps
slug: customizing-themes-of-codename-one-apps
url: /blog/customizing-themes-of-codename-one-apps/
original_url: https://www.codenameone.com/blog/customizing-themes-of-codename-one-apps.html
aliases:
- /blog/customizing-themes-of-codename-one-apps.html
date: '2021-03-15'
author: Steve Hannah
description: Recipes to customize the look and feel of Codename One apps using Themes,
  CSS, Styles, etc.
---

Recipes to customize the look and feel of Codename One apps using Themes, CSS, Styles, etc.

The following recipes include tips on customizing the look and feel of Codename one apps using themes, CSS, styles, etc.

### Platform-Specific Styling

## Problem

You have used CSS to style your app, and it looks great on some devices but not on others. You want to change the font size of some styles, but only on specific devices.

## Solution

Use CSS media queries to target styles at a specific device (e.g. desktop, tablet, or phone), platform (e.g. Android, iOS, Mac, Windows, etc…​), or device densities (e.g. low, medium, high, very high, etc..).

## Example: A media query to override Label color on iOS only

```css
				
					@media platform-ios {
    Label {
        color: red;
    }
}
				
			
```

Media queries will allow you to target devices based on three axes: Platform, Device, and Density

## Table 1. Platform Queries

| Value | Description |
| --- | --- |
| platform-ios | Apply only on iOS |
| platform-and | Apply only on Android |
| platform-mac | Apply only on Mac desktop |
| platform-win | Apply only on Windows desktop |

## Table 2. Device Queries

| Value | Description |
| --- | --- |
| device-desktop | Apply only on desktop |
| device-tablet | Apply only on tablet |
| device-phone | Apply only on phone |

## Table 3. Density Queries

| Value | Description |
| --- | --- |
| density-very-low | Very low density 176x220 and smaller |
| density-low | Low density up to 240x320 |
| density-medium | Medium density up to 360x480 |
| density-high | High density up to 480x854 |
| density-very-high | Very high density up to 1440x720 |
| density-hd | HD up to 1920x1080 |
| density-560 | Intermediate density for screens between HD to 2HD |
| density-2hd | Double the HD level density |
| density-4k | 4K level density |

You can combine media queries to increase the specificity.

## Example: Targeting only 4k Android tablets

```css
				
					@media platform-and, device-tablet, density-4k {
    Label {
        font-size: 5mm;
    }
}
				
			
```

You can also combine more than one query of the same type to broaden the range of the query.

For example, targeting only HD, 2HD, and 4K Android tablets:

```css
				
					@media platform-and, device-tablet, density-4k, density-2hd, density-hd {
    Label {
        font-size: 5mm;
    }
}
				
			
```

## Further Reading:

[Media Queries Section of Codename One Wiki](https://github.com/codenameone/CodenameOne/wiki/css#media-queries)

### Platform-Specific Font Scaling

## Problem

Your app looks great except that on desktop, the fonts are a little too small. If you could only scale the fonts to be 25% larger on the desktop, your app would be perfect.

## Solution

You can use font-scaling constants to scale all of the fonts in your stylesheet by a constant factor. You can use a “media-query-like” syntax to apply this scaling only on particular platforms, devices, or densities.

## Example: Scaling Fonts to be 25% larger on desktop

```css
				
					#Constants {
    device-desktop-font-scale: "1.25";
}
				
			
```

## Tip:

> In most cases it is better to use standard media queries to apply styles which target specific platforms in a more fine-grained manner.

## Further Reading:

[Font-Scaling constants section of the Codename One Wiki](https://github.com/codenameone/CodenameOne/wiki/css#font-scaling-constants)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **ThomasH99** — March 16, 2021 at 8:07 am ([permalink](https://www.codenameone.com/blog/customizing-themes-of-codename-one-apps.html#comment-24414))

> ThomasH99 says:
>
> Great addition, really useful. Thanks!
>



### **Javier Anton** — March 24, 2021 at 9:06 am ([permalink](https://www.codenameone.com/blog/customizing-themes-of-codename-one-apps.html#comment-24417))

> Javier Anton says:
>
> Thanks, I rely on themes at the moment and kind of dread migrating to CSS. I know the clock is ticking and you will switch off support for themes at some point, so resources like these are great
>



### **ThomasH99** — March 28, 2021 at 9:33 am ([permalink](https://www.codenameone.com/blog/customizing-themes-of-codename-one-apps.html#comment-24418))

> ThomasH99 says:
>
> Javier, I’m using CSS since quite some time, I found it easy to get started, and it’s a really nice way of working, especially with the live update (the simulator is updated as soon as you save the CSS file). This has made the workflow a LOT faster. The only slight concern I’ve come across is that since the CSS conversion creates every possible variation of the UIIDs (pressed ect), my generated .res becomes very big (280 uiids gives a .res of 400kb, no pictures). I define UIIDs for most of the individual elements based on their semantics and that might not be the best solution, but it makes it easy to tune individual styles whenever needed.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
