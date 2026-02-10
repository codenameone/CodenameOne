---
title: New CSS Units in Codename One Apps
slug: new-css-units-in-codename-one-apps
url: /blog/new-css-units-in-codename-one-apps/
original_url: https://www.codenameone.com/blog/new-css-units-in-codename-one-apps.html
aliases:
- /blog/new-css-units-in-codename-one-apps.html
date: '2021-07-07'
author: Steve Hannah
description: We’ve added some new units that you can use for specifying things like
  margin, padding and font sizes. You can use each of these units directly in your
  CSS stylesheets.
---

We’ve added some new units that you can use for specifying things like margin, padding and font sizes. You can use each of these units directly in your CSS stylesheets.

![New CSS Units in Codename One Apps](/blog/new-css-units-in-codename-one-apps/New-CSS-Units-in-Codename-One-Apps-1024x576.jpg)

We’ve added some new units that you can use for specifying things like margin, padding, and font sizes.

These units are `rem`, `vh`, `vw`, `vmin`, and `vmax`.

You can use each of these units directly in your CSS stylesheets, and there are corresponding `style` constants for each of these so that you can use them directly in your Java code.

These units were inspired by their CSS counterparts, so if you are familiar with these units from CSS, they have the same meaning (except for `rem`, which is a little bit different).

### `rem`

`1rem` is the line height of the default font. i.e. `1rem == Font.getDefaultFont().getHeight()`. This unit is particularly useful if you would like to set your font size relative to the system’s default font size.

## `rem` examples in CSS

```css
				
					/* A label that 1.5x bigger than the line height of the default font. */
MyBigLabel {
    font-size: 1.5rem;
}

/* A label roughly size of default font. */
MyNormalLabel {
    font-size: 0.7rem;
}

/* 1 line height padding */
MyPaddedContainer {
    padding: 1rem;
}
				
			
```

## Converting `rem` to pixels in Java

```java
				
					// Padding 1.5x line height of default font
int padding = CN.convertToPixels(1.5, Style.UNIT_TYPE_REM);

// Create a Truetype Font with REM sizing
Font myFont = Font.createTruetypeFont(CN.NATIVE_MAIN_REGULAR, 1.5f, Style.UNIT_TYPE_REM);

// Deriving font to smaller size
Font mySmallFont = myFont.derive(0.5f, Style.UNIT_TYPE_REM);
				
			
```

## Note

> Since `1rem` is equal to the line height of the default font, it is slightly bigger than the actual default font size. Usually around `0.7rem` will give you a font with the same size as the default font.

### `vw` and `vh`

`1vw` and `1vh` are 1 percent of the viewport width and height respectively. These differ from the existing **percent** unit in Codename One because the **percent** unit is contextual. i.e. If it is applied to the **top** or **bottom** padding, it will be relative to the viewport height, and if it is applied to the **left** or **right** padding, it will be relative to the viewport width. `vw` and `vh`, by contrast, are explicitly relative to the viewport **width** (for `vw`) or **height** (for `vh`).

## Using `vw` unit from CSS

```css
				
					MyContainer {
    /* Make right margin 10% of screen width */
    margin-right: 10vw;
}
				
			
```

## Setting margin in Java using `UNIT_TYPE_VW` constant

```java
				
					// Set right margin of this Style object to 10% of screen width.
myStyle.setMarginUnitRight(Style.UNIT_TYPE_VW);
myStyle.setMarginRight(10);
				
			
```

### `vmin` and `vmax`

`1vmin` is 1 percent of the minimum of viewport width and viewport height. i.e. `1vmin=Math.min(CN.getDisplayWidth(), CN.getDisplayHeight())/100f`

`1vmax` is 1 percent of the maximum of viewport width and viewport height. i.e. `1vmax=Math.max(CN.getDisplayWidth(), CN.getDisplayHeight())/100f`

## Using `vw` unit from CSS

```css
				
					MyContainer {
    /* Make right margin 10% either screen width or height - whichever is smaller */
    margin-right: 10vmin;
}
				
			
```

## Setting margin in Java using `UNIT_TYPE_VW` constant.

```java
				
					// Set right margin of this Style object to 10% of smaller of screen width or height.
myStyle.setMarginUnitRight(Style.UNIT_TYPE_VMIN);
myStyle.setMarginRight(10);
				
			
```

### Overriding the Default Font Size

When trying to make a design look “good” across multiple platforms it can be difficult to deal with the differing default font sizes on different platforms. You may spend hours tweaking your UI to look perfect on iPhone X, only to find out that the fonts are too small when viewed on an android device. We have now added theme constants to explicitly set the the default font size in “screen-independent-pixels”.

## Note

> In this case, 1 screen-independent-pixel is defined as 1/160th of an inch on a device, and 1/96th of an inch on desktop. These values correspond to Android’s definition on device, and Windows' definition on the desktop.

If you add the following to your stylesheet, it will set the default font size to 18 screen-independent pixels (or 18/160th of an inch), which corresponds to the Android native default “medium” font size.

```css
				
					#Constants {
    defaultFontSizeInt: 18;
}
				
			
```

## Tip

> I have found that a value of 18 here gives optimum results across devices.

### Desktop and Tablet Default Font sizes

On the desktop, you may find that **18** is too big. You can additionally define a default font size for for tablet and desktop using `defaultDesktopFontSizeInt` and

`defaultFontSizeDesktopInt` respectively. I have found that a `defaultDesktopFontSizeInt` gives results that closely match the Mac OS default font size.

```css
				
					#Constants {
    defaultFontSizeInt: 18;
    defaultDesktopFontSizeInt: 14;
}
				
			
```

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
