---
title: Always on Top and Style Parser
slug: always-on-top-style-parser
url: /blog/always-on-top-style-parser/
original_url: https://www.codenameone.com/blog/always-on-top-style-parser.html
aliases:
- /blog/always-on-top-style-parser.html
date: '2017-09-24'
author: Shai Almog
---

![Header Image](/blog/always-on-top-style-parser/new-features-2.jpg)

It’s been a busy week with 3.7.3 released and a lot of new things. [Diamond](https://github.com/diamondobama) made several [PR’s](https://github.com/codenameone/CodenameOne/pulls) over the past couple of weeks but one interesting PR is an “always on top” feature for the simulator which is exactly what it sounds…​

This is very useful for me personally as it will allow me to film coding while showing the simulator floating on top (thanks Diamond!) but it should be super useful for everyone. You can inspect the code/debugging values while the simulator floats on top. You can activate it using the simulator menu option.

### Style Parser

Styles are a pretty complex and deeply ingrained subject in Codename One. It’s really hard to extend or modify this without breaking everything…​

Steve recently ventured on the surface and offered a new way in thru a String based style syntax that is a hybrid of CSS and Codename One logic. Don’t confuse this with the CSS plugin. The CSS plugin works statically during compile time and Codename One is “unaware” of its existence. The resulting file in the CSS file is a regular CSS file.

This new style mode is builtin to Codename One and overrides styles defined in the theme. This is useful for the GUI builder where you might want to customize the appearance of a component without venturing into the theme and creating a new UIID.

You can use this from code and might find some pretty cool hacks for it but this was built mostly to facilitate styling directly from the GUI builder. This can be used in the new GUI builder UI in the 3rd tab where you can now customize styles for components directly:

![Style customization from within the GUI builder](/blog/always-on-top-style-parser/gui-builder-style-editor.png)

Figure 1. Style customization from within the GUI builder

This is a new feature so some things might now work for the 3.7.3 release. Please let us know in the [issue tracker](http://github.com/codenameone/CodenameOne/issues/)

__ |  In the current version there is a bug that might require you to close and reopen the UI for the styling to work   
---|---  
  
As a sidenote the new GUI builder and autolayout mode have improved significantly since 3.7.2 and are far more pleasurable to use!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
