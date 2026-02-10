---
title: Lightweight Text Selection
slug: lightweight-text-selection
url: /blog/lightweight-text-selection/
original_url: https://www.codenameone.com/blog/lightweight-text-selection.html
aliases:
- /blog/lightweight-text-selection.html
date: '2019-05-06'
author: Shai Almog
---

![Header Image](/blog/lightweight-text-selection/new-features-5.jpg)

Text editing is implemented natively in Codename One. This provides a huge benefit as it allows us to ignore a lot of complex topics such as virtual keyboard localization etc. If we had to implement all that logic the overhead of Codename One would have been huge…​

One free benefit is seamless support for copy & paste. It “just works” thanks to the usage of the native widget we don’t need to worry about that UI. However, this breaks down when we want to provide the ability to select & copy without the ability to edit. E.g. in a web app we can usually select any bit of text and copy it…​ That’s convenient for some cases.

__ |  A good example would be listing user details within the app that the user might want to copy e.g. his user ID   
---|---  
  
As part of an RFE we added support for that through a lightweight copy mechanism, this works for uneditable TextAreas and TextFields. It will also work for Labels and SpanLabels. Notice that this is off by default you need to enable this using `parentForm.getTextSelection().setEnabled(true)`. Even then it will only work for TextAreas and TextFields by default. You will need to explicitly enable it on other components using `setTextSelectionEnabled(true)`.
    
    
    Form hi = new Form("Tabs", BoxLayout.y());
    hi.getTextSelection().setEnabled(true);
    
    SpanLabel s = new SpanLabel("Long label text that would still be selectable in this case");
    s.setTextSelectionEnabled(true);
    hi.add(s);
    
    hi.show();

![Lightweight Text Selection](/blog/lightweight-text-selection/lightweight-text-selection.png)

Figure 1. Lightweight Text Selection

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
